package work.liziyun.comp;




import work.liziyun.code.LzyScope;
import work.liziyun.code.LzySymtab;
import work.liziyun.code.symbol.*;
import work.liziyun.code.type.LzyMethodType;
import work.liziyun.code.type.LzyType;
import work.liziyun.code.type.LzyTypeTags;
import work.liziyun.code.LzyClassReader;
import work.liziyun.tag.LzyFlags;
import work.liziyun.tag.LzyKinds;
import work.liziyun.tree.LzyTreeInfo;
import work.liziyun.util.LzyContext;
import work.liziyun.util.LzyList;
import work.liziyun.world.LzyName;
import work.liziyun.world.LzyTable;
import static work.liziyun.tag.LzyFlags.*;
import static work.liziyun.tag.LzyKinds.*;
import static work.liziyun.code.type.LzyTypeTags.*;



public class LzyResolve {
    private static final LzyContext.Key resolveKey = new LzyContext.Key();
    private LzyTable names;
    private LzySymtab syms;
    private LzyCheck check;
    private LzyClassReader reader;
    private LzyTreeInfo treeInfo;

    // 错误代码
    static final int AMBIGUOUS = 256; // 模棱两可
    static final int HIDDEN = 257; // 访问权限错误
    static final int ABSENT_VAR = 258; // 没有找到变量
    static final int WRONG_MTHS = 259; // 找到多个
    static final int WRONG_MTH = 260; // 找到一个错误的方法
    static final int ABSENT_MTH = 261; // 没有找到方法
    static final int ABSENT_TYP = 262; // 没有找到类型
    // 错误
    final LzyResolve.ResolveError varNotFound;
    final LzyResolve.ResolveError wrongMethod;
    final LzyResolve.ResolveError wrongMethods;
    final LzyResolve.ResolveError methodNotFound;
    final LzyResolve.ResolveError typeNotFound;


    public LzyResolve(LzyContext LzyContext) {
        LzyContext.put(resolveKey,this);
        this.syms = LzySymtab.instance(LzyContext);
        this.names = LzyTable.instance(LzyContext);
        this.check = LzyCheck.instance(LzyContext);
        this.reader = LzyClassReader.instance(LzyContext);
        this.treeInfo = LzyTreeInfo.instance(LzyContext);
        // 初始化错误
        this.varNotFound = new LzyResolve.ResolveError(ABSENT_VAR,this.syms.errSymbol,"没有找到变量!");
        this.wrongMethod = new LzyResolve.ResolveError(WRONG_MTH,this.syms.errSymbol,"发现一个错误方法!");
        this.wrongMethods = new LzyResolve.ResolveError(WRONG_MTHS,this.syms.errSymbol,"发现多个错误方法!");
        this.methodNotFound = new LzyResolve.ResolveError(ABSENT_MTH,this.syms.errSymbol,"方法没有找到!");
        this.typeNotFound = new LzyResolve.ResolveError(ABSENT_TYP,this.syms.errSymbol,"类型没有找到!");

    }
    // 环境创建者
    public static LzyResolve instance(LzyContext var0) {
        LzyResolve var1 = (LzyResolve)var0.get(resolveKey);
        if (var1 == null) {
            var1 = new LzyResolve(var0);
        }
        return var1;
    }
    // 通过Name查找到Symbol
    // 可能是 1.变量 2.类型 3.包
    LzySymbol findIdent(LzyEnv env,LzyName name,int kind)  {
        LzySymbol bestSoFar = this.typeNotFound;
        LzySymbol symbolRs = null;
        // 如果是变量
        if ((kind & LzyKinds.VAR) != 0 ){
            symbolRs = this.findVar(env,name);
            if (symbolRs.kind <= 256){
                return symbolRs;
            }
            // 择优
            if (symbolRs.kind < bestSoFar.kind){
                bestSoFar = symbolRs;
            }
        }
        // 如果是类型
        if ( (kind & LzyKinds.TYP) != 0 ){
            symbolRs = findType(env,name);
            if (symbolRs.kind <= 256) {
                return symbolRs;
            }
            if ( symbolRs.kind < bestSoFar.kind ){
                bestSoFar = symbolRs;
            }
        }
        // 如果是包
        if ( (kind & LzyKinds.PCK) != 0 ){
            return reader.enterPackage(name);
        }
        return bestSoFar;
    }


    LzySymbol findVar(LzyEnv env, LzyName name) {
        LzyEnv env2 = env;
        LzySymbol bestSoFar = this.varNotFound;
        LzySymbol symbol = null;
        // 注意: 我们不考虑内部类的情况！所以不需要用循环，向外部环境中进行查找！
        // 标志: 当前环境是否静态
        boolean isStatic = false;

        // 注意: 由于不考虑内部类的情况，所以不需要使用循环到外部环境中查找

        if ( isStatic(env2) ){
            isStatic = true;
        }
        // 当前环境中查找
        // 当前作用域下所有符合Name的。可能是变量，也可能是其他的。
        LzyScope.Entry entry = ((LzyAttrContext)env2.info).scope.lookup(name);
        // 我们需要的是名字叫Name的变量
        while ( entry.scope!=null && entry.sym.kind!=LzyKinds.VAR ){
            entry = entry.next();
        }
        // 如果在当前作用域中找到 ---> 这不是一个局部变量，而是一个成员变量
        if (entry.scope != null){
            symbol = entry.sym;
        }else{
            // 尝试从类中开始查找
            symbol = this.findField(env2, env2.enclClass.sym.type, name, env2.enclClass.sym);
        }


        // 当是 正确结果 或 模棱两可错误时
        if (symbol.kind <= AMBIGUOUS){
            // 防止: 静态环境引用非静态的变量
            if (isStatic
                    && symbol.kind == LzyKinds.VAR
                    && symbol.owner.kind == LzyKinds.TYP
                    && (symbol.flags() & LzyFlags.STATIC) == 0
            ){
                System.out.println("编译错误: 静态引用非静态!");
                return new LzyResolve.StaticError(symbol);
            }

            return symbol;
        }

        // 注意: 尝试从预定义类型中查找 1. 方法运算符 2.null类型
        symbol = this.findField(env,this.syms.predefClass.type,name,this.syms.predefClass);
        if (symbol.kind <= 256){
            return symbol;
        }else {
            return bestSoFar;
        }
    }

    LzySymbol findType(LzyEnv env,LzyName name){
        // 择优
        LzySymbol bestSoFar = this.typeNotFound;
        // 环境
        LzyEnv env2 = env;
        LzySymbol symbol;
        // 注意: 省略代码，由于我们不支持内部类的语法，所以我们不需要在类，父类，接口中查找类型的定义。
        // 1. 从普通导包
        symbol = this.findGlobalType(env,env.toplevel.namedImportScope,name);
        if (symbol.kind <=  256){
            return symbol;
        }
        if ( symbol.kind < bestSoFar.kind ){
            bestSoFar = symbol;
        }

        // 2. 同包中寻找
        symbol = this.findGlobalType(env,env.toplevel.packageSymbol.members(),name);
        if (symbol.kind <=  256){
            return symbol;
        }
        if ( symbol.kind < bestSoFar.kind ){
            bestSoFar = symbol;
        }
        // 3. 从'*'号导入中查找
        symbol = this.findGlobalType(env,env.toplevel.starImportScope,name);
        if (symbol.kind <=  256){
            return symbol;
        }
        if ( symbol.kind < bestSoFar.kind ){
            bestSoFar = symbol;
        }
        return bestSoFar;
    }
    LzySymbol findGlobalType(LzyEnv env,LzyScope scope,LzyName name){
        // 择优
        LzySymbol bestSoFar = this.typeNotFound;
        // 查找
        LzyScope.Entry entry = scope.lookup(name);
        LzyClassSymbol classSymbol = null;
        // 由于根据名称查找: 可能找到多个结果
        // 例如: a包下Teacher 和 b包下Teacher
        while (entry.scope != null){
            //
            if (entry.scope.owner == entry.sym.owner){
                // 通过全限定类名加载类符号
               classSymbol  = this.reader.loadClass(entry.sym.flatName());
               // 拥有两个符合的答案: 我们产生一个模棱两可错误
               if ( bestSoFar.kind == TYP && classSymbol.kind == TYP && classSymbol != bestSoFar ){
                   return new LzyResolve.AmbiguityError(bestSoFar,classSymbol);
               }
               if ( classSymbol.kind < bestSoFar.kind ){
                   bestSoFar = classSymbol;
               }
            }
            // 下一个
            entry = entry.next();
        }
        return bestSoFar;
    }

    LzySymbol findField(LzyEnv env, LzyType type, LzyName name, LzyTypeSymbol typeSymbol){
        // 默认: 没有找到
        LzySymbol bestSoFar = this.varNotFound;
        // 当前类中进行查找
        // 注意: 为什么是循环呢？ 因为名称为name的，可以是一个变量,也可以是一个方法! 可能存在多个名称叫name的!
        for (LzyScope.Entry entry = typeSymbol.members().lookup(name) ; entry.scope != null ; entry = entry.next()){
            //1. 当前符号是一个变量符号
            //2. 变量的修饰符中没有合成
            if (entry.sym.kind == LzyKinds.VAR  ){
                // 是否可以访问
                if (this.isAccessible(env,type,entry.sym)){
                    return entry.sym;
                }
                System.out.println("编译错误:没有访问权限!");
                return new LzyResolve.AccessError(entry.sym);
            }
        }
        // 父类中进行查找
        LzyType extendType = typeSymbol.type.supertype();
        // 父类中查找的结果
        LzySymbol extendSymbolRs  = null;
        // 如果存在，并且是一个类
        if (extendType != null && extendType.tag == LzyTypeTags.CLASS){
            extendSymbolRs = this.findField(env,type,name,extendType.tsym);
            // 择优
            if (extendSymbolRs.kind < bestSoFar.kind){
                bestSoFar = extendSymbolRs;
            }
        }
        // 接口中进行查找
        for (LzyList interList = typeSymbol.type.interfaces(); interList.nonEmpty() ; interList = interList.tail ){
            extendSymbolRs = this.findField(env,type,name,((LzyType)interList.head).tsym);
            // 产生模棱两可错误: 1. 当前查找结果是一个合法数据 2. bestSoFar结果是一个合法数据
            // 注意: 防止查找相同的数据 bestSoFar.owner != symbol.owner
            // 案例: S extends A implement B ; A implement B ;
            // 假如变量在接口B中。那么我们从两条路线，查询到同一个答案。但是这并不是模棱两可错误！
            if ( bestSoFar.kind < 256 && extendSymbolRs.kind < 256 && bestSoFar.owner != extendSymbolRs.owner ){
                bestSoFar = new LzyResolve.AmbiguityError(bestSoFar,extendSymbolRs);
            }else if ( extendSymbolRs.kind < bestSoFar.kind ){
                // 当前查找到的结果: 比模棱两可还要好!
                bestSoFar = extendSymbolRs;
            }
        }
        return bestSoFar;
    }


    boolean isAccessible(LzyEnv env, LzyTypeSymbol typeSymbol){
        switch( (int)( typeSymbol.flags() & AccessFlags )  ) {
            case 0: // 没有权限修饰符: 本类,同包
                return env.toplevel.packageSymbol == typeSymbol.owner  ;
            case LzyFlags.PUBLIC: // public修饰符: 任何人都能访问
                return true;
            default:
                return true;
        }
    }



    // 当前环境 和 变量/方法 的权限关系
    // 注意: 我们不考虑内部类的情况!
    boolean isAccessible(LzyEnv env, LzyType type , LzySymbol symbol){
        // 禁止对init初始化方法的调用
        if (symbol.name == this.names.init && symbol.owner != type.tsym ){
            return false;
        }else{
            switch ( (int)(symbol.flags() & (LzyFlags.AccessFlags) ) ){
                // 默认本类同包
                case 0:
                    // 注意: 由于本类一定是同包的,所以只需要对同包进验证!
                    return env.toplevel.packageSymbol == symbol.owner.owner && symbol.isInheritedIn(type.tsym) ;
                case LzyFlags.PUBLIC:
                    // 对类的访问修饰符检验!
                    switch( (int)( type.tsym.flags() & LzyFlags.PUBLIC )  ) {
                        case 0: // 没有权限修饰符: 本类,同包
                            return env.toplevel.packageSymbol == type.tsym.owner  ;
                        case LzyFlags.PUBLIC: // public修饰符: 任何人都能访问
                            return true;
                        default:
                            return true;
                    }
                case LzyFlags.PRIVATE:
                    return env.enclClass.sym == symbol.owner && symbol.isInheritedIn(type.tsym);
                case LzyFlags.PROTECTED:
                    if (env.toplevel.packageSymbol == symbol.owner.owner){
                        return true;
                    }else if ( env.enclClass.sym.isSubClass(type.tsym) && (type.tsym.flags() & LzyFlags.PUBLIC) != 0  ){
                        return true;
                    }else{
                        return false;
                    }
                default:
                    System.out.println("权限修饰符检查: 遇到问题!");
                    return false;
            }
        }
    }


    LzySymbol findFun(LzyEnv env,LzyName name,LzyList argList){
        LzyEnv env1 = env;
        LzySymbol symbol2 ;
        // 内部环境向外部环境查找: 内部类 ---> 引用外部类的方法
        for (boolean isStatic = false  ; env1.outer != null ; env1 = env1.outer ) {
            // 当前方法是一个静态方法: 发起调用方
            if ( isStatic(env1) ){
                isStatic = true;
            }
            // 被调用的一方
            symbol2 = this.findMethod(env1,env1.enclClass.sym.type,name,argList);
            // 如果静态内容: 引用非静态内容。这里将会报错！
            // 条件一: 发起调用的方法是一个静态方法
            // 条件二: 被调用的是一个方法
            // 条件三: 被调用的方法在一个类中
            // 条件四: 被调用的方法不是静态方法
            if (isStatic
                    && symbol2.kind == LzyKinds.MTH
                    && symbol2.owner.kind == LzyKinds.TYP
                    && (symbol2.flags() & LzyFlags.STATIC) == 0 ){
                System.out.println("编译错误: 静态方法不能调用非静态方法!");
                return null;
            }else{
                return symbol2;
            }
        }
        System.out.println("编译错误: 没有找到合适的方法! "+name.toString());
        return null;
    }


    static boolean isStatic(LzyEnv env){
        // 当前环境的staticLevel 大于 外部环境的staticLevel： 即是静态
        return ((LzyAttrContext)env.info).staticLevel > ((LzyAttrContext)env.outer.info).staticLevel;
    }

    // 通过简单名称Name名称查找 --->调用findFun() --->调用findMethod() --->
    // 例如: md()
    LzySymbol resolveMethod(int pos, LzyEnv env, LzyName name, LzyList argList){
        return this.findFun(env,name,argList);
    }

    LzySymbol findMethod(LzyEnv env,LzyType type,LzyName name,LzyList argList){
        return  this.findMethod(env,type,name,argList,type.tsym.type,true,this.methodNotFound);
    }

    public LzyMethodSymbol resolveInternalMethod(int pos,LzyEnv env,LzyType site,LzyName name,LzyList<LzyType> argtypes){
        // 基于类型的方法调用解析
        LzySymbol methodSymbol = resolveQualifiedMethod(pos, env, site, name, argtypes);
        if ( methodSymbol.kind == MTH ){
            return (LzyMethodSymbol) methodSymbol;
        }else {
            // 抛出一个致命的错误
            throw new Error("fatal.err.cant.locate.meth: " + name);
        }

    }



    /**
     * 解析复合赋值运算
     * @param pos       位置
     * @param opTag     操作码
     * @param env       环境
     * @param argList   参数列表
     * @return
     */
    LzySymbol resolveOperator(int pos,int opTag,LzyEnv env,LzyList argList){
        // 操作符的名称
        LzyName name = this.treeInfo.operatorName(opTag);
        return this.findMethod(env,this.syms.predefClass.type,name,argList);
    }

    LzySymbol resolveUnaryOperator(int pos,int optag,LzyEnv env,LzyType arg){
        // 参数列表
        LzyList<LzyType> argList = LzyList.of(arg);
        return this.resolveOperator(pos,optag,env,argList);
    }


    /**
     * 解析二元运算
     * @param opTag     操作码
     * @param leftType  左边的运算类型
     * @param env       环境
     * @param rightType 右边的运算类型
     * @return
     */
    LzySymbol resolveBinaryOperator(int opTag, LzyType leftType, LzyEnv env, LzyType rightType){
        // 参数列表
        LzyList argType = LzyList.nil().prepend(leftType).prepend(rightType);
        // 方法名
        LzyName methodName =  this.treeInfo.operatorName(opTag);
        // 参数三:方法名       参数四:参数列表
        return this.findMethod(env,this.syms.predefClass.type,methodName,argType);
    }

    private LzySymbol findMethod(LzyEnv env,LzyType site,LzyName name,LzyList argList,LzyType intype,boolean absOpen,LzySymbol bestSoFar ){
        // 遍历查找父类
        for (LzyType t = intype;t.tag == LzyTypeTags.CLASS;t = t.supertype()){
            // 类符号
            LzyClassSymbol classSymbol = (LzyClassSymbol) t.tsym;
            // 如果不是抽象类或者接口
            if ( (classSymbol.flags()& (ABSTRACT|INTERFACE) ) == 0L ){
                absOpen = false;
            }
            // 在当前类
            for (LzyScope.Entry entry = classSymbol.members().lookup(name); entry.scope != null ; entry = entry.next()) {
                // 当前方法的修饰: 不是合成的
                if ( entry.sym.kind == MTH  ){
                    // 尝试找到更好的
                    bestSoFar = this.selectBest(env,site,argList,entry.sym,bestSoFar);
                }
            }
            // 如果当前类是抽象类 或者是接口
            if (absOpen){
                // 遍历接口
                for (LzyList interList = classSymbol.type.interfaces();interList.nonEmpty();interList = interList.tail){
                    // 接口中寻找
                    bestSoFar = this.findMethod(env, site,name,argList,(LzyType) interList.head,absOpen,bestSoFar);
                }
            }
        }
        return bestSoFar;
    }

    LzySymbol selectBest(LzyEnv env,LzyType site,LzyList argList,LzySymbol currentSym,LzySymbol bestSoFar){
        // 当前查找到的是一个错误的
        if (currentSym.kind == ERR){
            return bestSoFar;
        }else if (this.instantiate(env,site,currentSym,argList) == null){// 找打了不符合规则的方法
            // 规则: argList实际参数 是 找到的方法参数的子类

            // 不符合继承关系的方法: 参数列表不合格
            switch (bestSoFar.kind){
                case WRONG_MTH: //第一次找到错误方法
                    return this.wrongMethods; // 多次找到错误方法
                case ABSENT_MTH: //没有找到方
                    return this.wrongMethod.setWrongSym(currentSym);
                default:
                    return bestSoFar;
            }
        }else if (!this.isAccessible(env,site,currentSym)){
            // 如果一个符合规则的都没有找到
            if (bestSoFar.kind == ABSENT_MTH){
                // 返回一个访问权限错误!
                return new LzyResolve.AccessError(currentSym);
            }else{
                return bestSoFar;
            }
        }else{ // 找到一个符合的方法
            // 如果bestSoFar是错误方法
            if (bestSoFar.kind > AMBIGUOUS){
                return currentSym;
            }else{
                return this.mostSpecific(currentSym,bestSoFar,env,site);
            }
        }
    }

    LzySymbol mostSpecific(LzySymbol currentSymbol,LzySymbol bestSoFar,LzyEnv env,LzyType site){

        switch (bestSoFar.kind){
            case MTH:
                if (currentSymbol == bestSoFar){
                    return currentSymbol;
                }else{
                    // 类中获取方法符号
                    LzyMethodType currentType = (LzyMethodType)site.memberType(currentSymbol) ;
                    // bestSoFar的参数 是 currentType方法的父类
                    boolean bestSoFarIsPub = this.instantiate(env,site,bestSoFar,currentType.argtypes ) != null;
                    // 类中获取方法符号
                    LzyMethodType bestSoFarType = (LzyMethodType)site.memberType(bestSoFar);
                    // currentSymbl 的参数是 bestSoFar参数的父类
                    boolean currentIsPub = this.instantiate(env, site, currentSymbol, bestSoFarType.argtypes ) != null;
                    if (currentIsPub && bestSoFarIsPub){
                        // current是子类
                        if (this.methodsInherited(currentSymbol.owner,bestSoFar.owner)){ // 为了支持方法的重写,返回子类中方法
                            return currentSymbol;
                        }else if (this.methodsInherited(bestSoFar.owner,currentSymbol.owner)){ // 为了支持方法的重写,返回子类中方法
                            return bestSoFar;
                        }else{
                            // currentSymbol是抽象的
                            boolean currentIsAbstract =(currentSymbol.flags()&ABSTRACT) != 0;
                            // 当前方法不是抽象
                            if (!currentIsAbstract){
                                return currentSymbol; // 抽象方法 和 非抽象方法 同时胜出时,返回非抽象方法
                            }else{
                                // bestSoFar是抽象的
                                boolean bestSoFarIsAbstract  = (bestSoFar.flags()&ABSTRACT)!=0;
                                // bestSoFar不是抽象
                                if (!bestSoFarIsAbstract){
                                    return bestSoFar; // 抽象方法 和 非抽象方法 同时胜出时,返回非抽象方法
                                }
                                // 如果两个方法都是抽象的，并且同时胜出。
                                System.out.println("编译错误: 暂时不处理两个抽象方法同时胜出的情况!");
                                return null;
                            }
                        }
                    }else if (bestSoFarIsPub){ // bestSoFar是父类的情况
                        return currentSymbol;
                    }else{
                        if (currentIsPub){
                            return bestSoFar;
                        }
                        // 由于两次寻找打平手,产生一个模棱两可的错误
                        return new LzyResolve.AmbiguityError(currentSymbol,bestSoFar);
                    }
                }
            case AMBIGUOUS:
                // bestSoFar是一个模棱两可错误
                LzyResolve.AmbiguityError ambiguityError = (LzyResolve.AmbiguityError)bestSoFar;
                // 当前方法 与 模棱两可中第一个方法比较: 选择出更优秀的结果
                LzySymbol sym1 = this.mostSpecific(currentSymbol, ambiguityError.sym1, env, site);
                // 当前方法 与 模棱两可中第二个方法比较: 选择出更优秀的结果
                LzySymbol sym2 = this.mostSpecific(currentSymbol, ambiguityError.sym2, env, site);
                // 当前方法 比 模棱两可中的结果都要好
                if (sym1 == sym2){
                    return sym1;
                }else{
                    // 当前方法 比 模棱两可中的结果都要差
                    if(sym1 == ambiguityError.sym1 && sym2 == ambiguityError.sym2){
                        return bestSoFar;
                    }
                    // 当前方法 只比 模棱两可中的一个结果好
                    return new LzyResolve.AmbiguityError(sym1,sym2);
                }
            default:
                throw new AssertionError();
        }
    }

    // 期望结果: sub是子类,pub是父类
    private boolean methodsInherited(LzySymbol sub,LzySymbol pub){
        // 继承存在的形式: 1. 普通类中   2. 接口中
        return sub.type.isSubType(pub.type) &&((sub.flags()&INTERFACE)==0 || pub.type != this.syms.objectType) ;
    }



    // 期望结果: argList是子类，symbol是父类
    LzyType instantiate(LzyEnv env,LzyType type,LzySymbol symbol,LzyList argList){
        LzyMethodType methodType = (LzyMethodType)type.memberType( symbol);
        // 两个参数数组: 是否继承关系
        // 参数一: 子类      参数二: 父类
        return LzyType.isSubTypes(argList,methodType.argtypes)?methodType:null;
    }

    // 通过类型查找 --->调用findMethod()
    // 例如: Test.md()
    LzySymbol resolveQualifiedMethod(int pos,LzyEnv env,LzyType type,LzyName name,LzyList argList){
        return this.findMethod(env,type,name,argList);
    }

    // 处理构造方法
    LzySymbol resolveConstructor(int pos,LzyEnv env,LzyType classType,LzyList argList){
        // 解析成一个init负责初始化的方法
        return this.resolveQualifiedMethod(pos,env,classType,this.names.init,argList);
    }

    LzySymbol findMemberType(LzyEnv env, LzyType site, LzyName name,LzyTypeSymbol c){
        LzySymbol bestSoFar = typeNotFound;
        LzySymbol sym;
        LzyScope.Entry e = c.members().lookup(name);
        // 遍历
        while (e.scope != null){
            if (e.sym.kind == TYP){
                return isAccessible(env,site,e.sym)?e.sym:null;
            }
            e = e.next();
        }
        // 父类
        LzyType st = c.type.supertype();
        if (st != null
            && st.tag==CLASS
        ){
            sym = this.findMemberType(env,site,name,st.tsym);
            // 更新bestSoFar迄今为止最好的
            if (sym.kind < bestSoFar.kind){
                bestSoFar = sym;
            }
        }
        // 接口
        for (
                LzyList l = c.type.interfaces()
                ;
                bestSoFar.kind != AMBIGUOUS && l.nonEmpty()
                ;
                l = l.tail
            )
        {
                // 查找
                sym = findMemberType(env,site,name,((LzyType)l.head).tsym);
                // 产生模棱两可错误
                if (bestSoFar.kind < AMBIGUOUS && sym.kind < AMBIGUOUS && sym.owner != bestSoFar.owner ){
                    // 这里创建一个模棱两可的错误
                    bestSoFar = new LzyResolve.AmbiguityError(bestSoFar,sym);
                }else if (sym.kind < bestSoFar.kind){// 更新迄今为止最好的结果
                    bestSoFar = sym;
                }
        }
        return bestSoFar;
    }

    LzySymbol findIdentInType(LzyEnv env,LzyType site,LzyName name,int kind){
        LzySymbol bestSoFar = typeNotFound;
        LzySymbol sym ;
        // 如果期望的是变量
        if ( (kind&VAR) != 0 ){
            sym = findField(env,site,name,site.tsym);
            if (sym.exists()){
                return sym;
            }else if ( sym.kind < bestSoFar.kind ){
                // 更新更高的精度更加准确的数据
                bestSoFar = sym;
            }
        }
        // 如果期望的是类型
        if ( (kind& LzyKinds.TYP) != 0 ){
            sym = findMemberType(env,site,name,site.tsym);
            if (sym.exists()){
                return sym;
            }else if (sym.kind < bestSoFar.kind){
                bestSoFar = sym;
            }
        }
        return bestSoFar;
    }


    LzySymbol resolveSelf(int pos, LzyEnv env, LzyTypeSymbol typeSymbol, LzyName name){
        LzyEnv env2 = env;
        boolean staticOnly = false;
        // 向内部环境查找
        while (env2.outer != null){
            if (isStatic(env2)){
                staticOnly = true;
            }
            if (env2.enclClass.sym == typeSymbol){
                LzySymbol sym = ((LzyAttrContext) env2.info).scope.lookup(name).sym;
                if (sym != null){
                    if (staticOnly){
                        System.out.println("编译错误: 静态引用!");
                    }
                    return sym;
                }
            }
            if ( (env2.enclClass.sym.flags()&STATIC) != 0 ){
                staticOnly = true;
            }
            env2  = env2.outer;
        }
        return this.syms.errSymbol;
    }

    LzySymbol findIdentInPackage(LzyEnv env, LzyTypeSymbol typeSymbol,LzyName name,int kind){
        LzyName name2 = LzyTypeSymbol.formFullName(name, typeSymbol);
        // 初始化没有找到类型
        LzySymbol symbol = this.typeNotFound;
        // 包符号
        LzyPackageSymbol packageSymbol = null;
        // 期望是包
        if ( ( kind & PCK ) != 0 ){
            packageSymbol = this.reader.enterPackage(name2);
            // 包符号已经填充过,那么拥有exists标记。什么确定！可以直接返回!
            if ( packageSymbol.exists() ){
                return packageSymbol;
            }
        }
        // 期待是类
        if ( ( kind & TYP ) != 0 ){
            LzySymbol classSymbol = this.loadClass(env,name2);
            // 类符号： 正确 或者 模棱两可
            if (classSymbol.kind <= 256){
                if (name == classSymbol.name){
                    return classSymbol;
                }
            }else if (classSymbol.kind < symbol.kind ){ // 是一个错误
                // 提高精度
                symbol = classSymbol;
            }
        }
        // 如果包符号和类符号同时找到，那么我们返回包符号
        return packageSymbol!=null?packageSymbol:symbol;
    }


    LzySymbol loadClass(LzyEnv env,LzyName name){
        try{
            // 读取
            LzyClassSymbol classSymbol = this.reader.loadClass(name);
            // 判断权限修饰符
            if ( this.isAccessible(env,(LzyTypeSymbol)classSymbol) ){
                return classSymbol;
            }else{
                return new LzyResolve.AccessError(classSymbol);
            }
        }catch (LzyClassReader.BadClassFile badClassFile){
            // 文件不存在: 报错！！！！
            throw badClassFile;
        }catch (LzySymbol.CompletionFailure completionFailure){
            // 错误填充
            return this.typeNotFound;
        }
    }

    /**
     * 内部类: 错误
     */

    // 访问权限错误
    static class AccessError extends LzyResolve.ResolveError {
        AccessError(LzySymbol var1) {
            super(257, var1, "access error");
        }
        public boolean exists() {
            return false;
        }
    }

    // 模棱两可错误
    static class AmbiguityError extends LzyResolve.ResolveError {
        LzySymbol sym1;
        LzySymbol sym2;

        AmbiguityError(LzySymbol var1, LzySymbol var2) {
            super(256, var1, "ambiguity error");
            this.sym1 = var1;
            this.sym2 = var2;
        }
    }

    // 解析错误
    private static class ResolveError extends LzySymbol  {
        final String debugName;
        final LzySymbol sym;
        LzySymbol wrongSym;

        public String toString() {
            return this.debugName + " wrongSym=" + this.wrongSym;
        }

        LzyResolve.ResolveError setWrongSym(LzySymbol var1) {
            this.wrongSym = var1;
            return this;
        }

        ResolveError(int var1, LzySymbol var2, String var3) {
            super(var1, 0L, (LzyName)null, (LzyType)null, (LzySymbol)null);
            this.debugName = var3;
            this.sym = var2;
        }

        /**
         * 不存在
         * @return
         */
        public boolean exists() {
            return false;
        }

    }

    // 静态引用的错误
    static class StaticError extends LzyResolve.ResolveError {
        StaticError(LzySymbol var1) {
            super(257, var1, "static error");
        }
    }

}

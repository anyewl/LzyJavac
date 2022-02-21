package work.liziyun.comp;




import work.liziyun.code.symbol.*;
import work.liziyun.code.type.LzyClassType;
import work.liziyun.code.type.LzyMethodType;
import work.liziyun.code.type.LzyTypeTags;
import work.liziyun.tag.LzyFlags;
import work.liziyun.tree.*;
import work.liziyun.code.LzyScope;
import work.liziyun.code.LzySymtab;
import work.liziyun.code.type.LzyType;
import work.liziyun.code.LzyClassReader;
import work.liziyun.tree.express.LzyJCSelect;
import work.liziyun.tree.state.LzyJCClassDef;
import work.liziyun.tree.state.LzyJCExpressionStatement;
import work.liziyun.tree.state.LzyJCVarDef;
import work.liziyun.util.LzyContext;
import work.liziyun.util.LzyList;
import work.liziyun.util.LzyListBuffer;
import work.liziyun.world.LzyName;
import work.liziyun.world.LzyTable;




public class LzyMemberEnter extends LzyVisitor implements LzySymbol.Completer,LzyFlags {
    private static final LzyContext.Key key = new LzyContext.Key();
    private LzyEnter lzyEnter;
    protected LzyEnv env;
    LzyTodo todo;
    LzyTable names;
    LzySymtab syms;
    LzyClassReader reader;
    LzyAttr lzyAttr;
    LzyListBuffer halfcompleted = new LzyListBuffer();
    LzyTreeMaker treeMaker;

    protected LzyMemberEnter(LzyContext LzyContext) {
        LzyContext.put(key,this);
        this.lzyEnter = LzyEnter.instance(LzyContext);
        this.names = LzyTable.instance(LzyContext);
        this.syms = LzySymtab.instance(LzyContext);
        this.reader = LzyClassReader.instance(LzyContext);
        this.todo = LzyTodo.instance(LzyContext);
        this.lzyAttr = LzyAttr.instance(LzyContext);
        this.treeMaker = LzyTreeMaker.instance(LzyContext);
    }

    public static LzyMemberEnter instance(LzyContext LzyContext) {
        LzyMemberEnter attr = (LzyMemberEnter)LzyContext.get(key);
        if (attr == null) {
            attr = new LzyMemberEnter(LzyContext);
        }
        return attr;
    }

    @Override
    public void visitJCTree(LzyJCTree tree) {

    }

    /**
     * 作用: 创建方法的作用域
     *      问题: 如何理解不共享作用域?
     *          深拷贝table，浅拷贝table中内容
     *          1. 浅拷贝table中内容:方法作用域中,拥有类作用域中的内容。即,可以达到方法访问成员属性的效果。
     *          2. 深拷贝table:方法作用域中,新增一个变量。并不会影响到类的成员作用域Scope。
     * 经典案例:
     *      1. 方法内部 和 方法外部 间的作用域关系: 非共享式作用域
     *      2. 成员变量 和 成员变量 间的作用域关系: 共享式作用域
     * @param methodDecl
     * @param classEnv
     * @return
     */
    LzyEnv methodEnv(LzyJCMethodDef methodDecl, LzyEnv classEnv){
        // 创建一个不共享的作用域
        // 深拷贝: Scope 和 table
        // 浅拷贝: Scope中内容 和 table中内容
        // 注意: elems并没有拷贝
        LzyScope unSharedScope = ((LzyAttrContext) classEnv.info).scope.dupUnshared();
        // 创建一个附加信息AttrContext
        // 深拷贝: AttrContext 浅拷贝: AttrContext中内容
        LzyAttrContext attrContext = ((LzyAttrContext) classEnv.info).dup(unSharedScope);
        // 创建一个方法环境: next是类环境，outer是编译单元环境
        LzyEnv methodEnv = classEnv.dup(
                methodDecl
                ,attrContext
        );
        // 环境的附加方法
        methodEnv.enclMethod = methodDecl;
        // 设置变量作用域的所有者: 方法符号
        ((LzyAttrContext)methodEnv.info).scope.owner = methodDecl.sym;
        // 如果是一个静态方法
        if ( (methodDecl.flags & STATIC) != 0 ){
            // 新增一个static等级
            ++((LzyAttrContext)methodEnv.info).staticLevel;
        }
        return methodEnv;
    }

    /**
     * 什么是共享作用域 和 非共享式作用域?
     *      共享式作用域: 会相互影响
     *          例如: 如果A和B是共享作用域
     *              那么A中添加一个数据，B中也能看见这个数据
     *      非共享式作用域: 不会相互影响
     *          例如: 如果A和B是非共享式作用域
     *              那么A中添加一个数据，B中不能看见这个数据
     * 相似案例: 深拷贝 和 浅拷贝
     */


    /**
     * 作用: 为变量的初始值,创建环境
     * 细节:
     *      1. 若变量是成员变量，那么变量的所有者是变量本身
     *      2. 若变量是局部变量，那么变量的所有者是方法
     * 思考问题: 为什么这里采用共享式作用域?
     *      答案一: 可以采用共享式作用域，也可以采用非共享式作用域
     *          因为我们不可能写出这样的代码"int a = int b  = 2;"
     *          即，不会影响到其他的作用域!
     *      可以告诉你，答案一并站不住脚!
     *
     *      答案二: 只能采用共享式作用域
     *          class A{
     *              final S s ;
     *              final A a = b;
     *              final B b ;
     *          }
     *          如果采用了非共享式的作用域，我们在处理"final A a = b",我们将找不到b,只能找到s。
     *          因为此时作用域中还没有"final B b"
     *
     * 触发条件:
     *      1. final修饰的成员变量: MemberEnter.visitVarDef()
     *          此时的外部环境是类环境
     *      2. 局部变量: Attr.visitVarDef()
     *          此时的外部环境是代码块环境
     * @param variableDecl
     * @param classOrBlockEnv
     * @return
     */
    LzyEnv initEnv(LzyJCVarDef variableDecl, LzyEnv classOrBlockEnv){
        // 创建变量初始化的环境: variableInitEnv
        // 细节: 1. next是classEnv 2. outer是CompilationEnv
        // 思考: 为什么next是classEnv,outer是CompilationEnv?
        // 观点: 因为next的设计思想是从层级结构的角度，outer始终指向当前的编译单元
        // 注意: info是深拷贝，info中内容都是浅拷贝 --> Scope是浅拷贝
        LzyEnv variableInitEnv = classOrBlockEnv.dupto( new LzyEnv(variableDecl,((LzyAttrContext)classOrBlockEnv.info).dup() )  );
        // 变量在类中: 这里是成员变量的情况，静态变量的owner是编译单元!
        if (variableDecl.sym.owner.kind == LzyClassReader.TYP){
            // 相同的成员作用域: 变量初始化的成员作用域Scope 等于 类的成员作用域Scope
            // 思考: Java编译器为什么要这么设计？
            // 逆推: 如果每个变量初始化代码，创建一个全新的作用域。那是不合理的，因为会产生作用域的隔离！
            // 例如: 作用域的隔离，变量a 无法 访问 变量b
            // 思考: 什么时候我们需要作用域的隔离？
            // 例如: 方法间的作用域.a方法是不能够访问到b方法中的内容，既a方法不可能调用b方法中的局部变量!
            // 变量的成员作用域: 使用类的成员作用域! 注意: 这里是作用域的共t 既a方法不可能调用b方法中的局部变量享!
            // 创建细节: Scope是深拷贝，Scope中内容是浅拷贝!
            // 为什么这么做? 为下一行代码作铺垫！更改作用域的所有者owner
            // 注意: 这里浅拷贝的只有table和owner,并没有拷贝elements
            ((LzyAttrContext)variableInitEnv.info).scope = ((LzyAttrContext)classOrBlockEnv.info).scope.dup();
            // 变量的成员作用域的属于: 变量
            ((LzyAttrContext)variableInitEnv.info).scope.owner = variableDecl.sym;
        }
        // 1.变量是静态的 2.变量在接口中
        if ( (variableDecl.flags & STATIC )!= 0 || ((classOrBlockEnv.enclClass.sym.flags() & 512) != 0) ){
            // 增加静态等级: staticLevel
            ++((LzyAttrContext)variableInitEnv.info).staticLevel;
        }
        return variableInitEnv;
    }

    @Override
    public void visitJCImport(LzyJCImport jcImport) {
        LzyJCTree tree = jcImport.qualid;
        LzyName name = LzyTreeInfo.name(tree);
        // 创建新的环境
        LzyEnv env = this.env.dup(jcImport);
        if (!this.lzyEnter.completionEnabled){
            System.out.println("开关异常: completionEnabled");
        }
        // 关闭第二阶段填充开关
        this.lzyEnter.completionEnabled = false;
        if (tree.getTag() == LzyJCTree.SELECT){
            LzyJCSelect select = (LzyJCSelect)tree;
            // 我们期望的是:1. 类型 2.包
            LzyTypeSymbol typeSymbol = this.lzyAttr.attribTree(select.selected, env, 3, LzyType.noType).tsym;
            // 如果是'*'
            if (name == this.names.asterisk){
                // 注意: 这里并不会触发引用消除，因为Attr处理是包。只会创建包和包下面的类符号。
                this.importAll(jcImport.pos,typeSymbol,this.env);
            }else{
                // 注意: 这里会进行引用的消除,因为Attr处理是类。findType中会触发loadClass()
                LzyTypeSymbol typeSymbol2 = this.lzyAttr.attribType(tree,env).tsym;
                this.importNamed(jcImport.pos,typeSymbol2,this.env);
            }
        }
        // 开启第二阶段填充开关
        this.lzyEnter.completionEnabled = true;
    }

    @Override
    public void visitJCMethodDef(LzyJCMethodDef methodDecl) {
        // this.env是类的环境,enterScope()方法返回类的成员作用域Scope!
        LzyScope scope = this.lzyEnter.enterScope(this.env);
        // 创建方法符号: 方法的所有者是类
        LzyMethodSymbol methodSymbol = new LzyMethodSymbol(0, methodDecl.name, null, scope.owner);
        methodSymbol.flags_field = this.lzyEnter.check.checkFlags(methodDecl.pos,methodDecl.flags,methodSymbol);
        // 设置方法符号到方法树节点上
        methodDecl.sym = methodSymbol;
        // 创建Env
        LzyEnv env = this.methodEnv(methodDecl, this.env);
        // 方法签名
        methodSymbol.type = this.signature(methodDecl.typarams,methodDecl.params,methodDecl.restype,methodDecl.thrown,env);
        // 作用: 清空elems  注意: 这里什么也没干，应该leave()方法针对的是共享式的作用域!
        //
        ((LzyAttrContext)env.info).scope.leave();
        // 省略符号的唯一性检查
        scope.enter(methodSymbol);
    }

    @Override
    public void visitJCCompilationUnit(LzyJCCompilationUnit var1) {
        // 如果'*'导入的内容目前为空
        if (var1.starImportScope.elems == null) {
            // 确保父包符号已经完成填充: 创建父包下所有的类符号
            if (var1.pid != null) {
                for(Object var2 = var1.packageSymbol; ((LzySymbol)var2).owner != this.syms.rootPackage; var2 = ((LzySymbol)var2).owner) {
                    ((LzySymbol)var2).owner.complete();
                }
            }
            // 导入java.lang
            this.importAll(var1.pos, this.reader.enterPackage(this.names.java_lang), this.env);
            // 第二阶段: 填充成员类 ---> 触发import的填充 ---> 同时也会触发Class符号的填充，但是是空实现!
            this.memberEnter(var1.defs, this.env);// 编译单元的成员: 1. 导包 2.类(空)
        }
    }


    //
    @Override
    public void visitJCVarDef(LzyJCVarDef variableDecl) {
        // 类环境: next 和 outer 都有编译单元
        LzyEnv env = this.env;
        // 静态修饰的变量
        // 注意: 为什么静态需要创建新的环境?
        // 静态变量应该使用编译单元的环境，普通成员变量应该使用类环境
        // 因为，非静态的变量应该属于类的作用域中，静态的变量应该属于编译单元的作用域中。
        // 相关细节: Scope scope = this.lzyEnter.enterScope(this.env);
        if ( (variableDecl.flags & STATIC) != 0L ){
            // 更新环境 ---> 使得静态变量最终放在编译单元的作用域下，而不是类的作用域下。
            env = this.env.dup(variableDecl,((LzyAttrContext)this.env.info).dup());
            // 更新静态等级  ---> 作用： 区分静态 和 非静态 的标志
            ++((LzyAttrContext)env.info).staticLevel;
        }
        // 类型的引用消除:  1.基本数据类型JCTree.JCPrimitiveTypeTree  2.引用数据类型JCTree.JCIdent
        this.lzyAttr.attribType(variableDecl.vartype,env);
        // 返回变量应该存放的作用域:
        // 1. 静态变量，应该放在编译单元的成员作用域下
        // 2. 成员变量，应该放在类的成员作用域下
        LzyScope scope = this.lzyEnter.enterScope(this.env);
        // 创建变量
        // 注意: 成员变量的owner是类，静态变量的owner是编译单元
        LzyVarSymbol varSymbol = new LzyVarSymbol(0L, variableDecl.name, variableDecl.vartype.type, scope.owner);
        // 修饰符检查处理
        varSymbol.flags_field = this.lzyEnter.check.checkFlags(variableDecl.pos,variableDecl.flags,varSymbol);
        // 设置变量
        variableDecl.sym = varSymbol;
        // 初始化进行处理
        if (variableDecl.init != null){
            // 如果有初始化的内容: 标记已经初始化hasinit
            varSymbol.flags_field |= HASINIT;
            // 如果是final修饰
            if ( (varSymbol.flags_field & FINAL) != 0L ){
                // Data中存储变量的环境
                // 注意: MemberEnter.visitDefVar()处理的是成员变量，并不会处理局部变量!
                // 意义: 变量的初始化的成员作用域 和 类的作用域一样，那么变量的初始化代码有权利访问类的成中内容!
                varSymbol.constValue = this.initEnv(variableDecl,this.env) ;
            }
        }
        //注意: 省略符号的唯一性检查
        // 符号填充到作用域下面： 1. 编译单元的成员作用域 2. 类的成员作用域
        scope.enter(varSymbol);
        varSymbol.pos = variableDecl.pos;
    }





    private void importNamed(int var1, LzySymbol symbol, LzyEnv env) {
        if ( (symbol.kind == LzyClassReader.TYP)  ){
            env.toplevel.namedImportScope.enter(symbol,symbol.owner.members());
        }
    }

    private void importAll(int vabr1, LzyTypeSymbol  typeSymbol, LzyEnv env) {
        // 包符号下所有类符号的创建: 并没有触发类符号的填充
        LzyScope memberScope = typeSymbol.members();
        LzyScope starImportScope = env.toplevel.starImportScope;
        // 遍历成员
        for (LzyScope.Entry entry = memberScope.elems; entry != null ; entry=entry.sibling) {
            if (entry.sym.kind == LzyClassReader.TYP && !this.lzyEnter.isIncluded(entry.sym,starImportScope)){
                starImportScope.enter(entry.sym,memberScope);
            }
        }
    }

    protected void memberEnter(LzyJCTree var1, LzyEnv var2) {
        LzyEnv var3 = this.env;
        try {
            this.env = var2;
            var1.accept(this);
        } catch (Exception var8) {
            //Enter.this.chk.completionError(var1.pos, var8);
            throw var8;
        } finally {
            this.env = var3;
        }

    }

    void memberEnter(LzyList var1, LzyEnv var2) {
        for(LzyList var3 = var1; var3.nonEmpty(); var3 = var3.tail) {
            this.memberEnter((LzyJCTree) var3.head, var2);
        }
    }


    // 方法签名
    LzyType signature(LzyList typarams, LzyList params, LzyJCTree restree, LzyList thrown, LzyEnv env) {
        LzyListBuffer LzyListBuffer = new LzyListBuffer();
        // 处理参数列表
        for ( LzyList LzyList = params;  LzyList.nonEmpty(); LzyList = LzyList.tail) {
            LzyListBuffer.append(this.lzyAttr.attribType( ((LzyJCVarDef)LzyList.head).vartype,env ));
        }
        // 返回值
        LzyType resType;
        if ( restree == null){
            resType = this.syms.voidType;
        }else{
            resType = this.lzyAttr.attribType(restree,env);
        }
        // 对于异常不做处理 thrown
        return new LzyMethodType(LzyListBuffer.toList(),resType,LzyList.nil(),this.syms.methodClass);
    }


    // 父类构造方法
    LzyJCExpressionStatement SuperCall(LzyTreeMaker make,
                                       LzyList<LzyType> typarams,
                                       LzyList<LzyJCVarDef> params,
                                       boolean based) {
        LzyJCTree.JCExpression meth;
        if (based) {
            meth = make.Select(make.Ident(params.head), names._super);
            params = params.tail;
        } else {
            meth = make.Ident(names._super);
        }
        return make.Exec(make.Apply(meth, make.Idents(params)));
    }


    // 符号填充器第二阶段的核心流程:
    @Override
    public void complete(LzySymbol symbol)  {
        if (!this.lzyEnter.completionEnabled){
            symbol.completer = this;
        }else{
            LzyClassSymbol classSymbol = (LzyClassSymbol)symbol;
            LzyClassType classType = (LzyClassType)symbol.type;
            LzyEnv env = this.lzyEnter.classEnvs.get(classSymbol);
            LzyJCClassDef jcClassDecl = (LzyJCClassDef) env.tree;
            boolean isEmpty = this.halfcompleted.isEmpty();
            // 最后的成员处理作准备!
            this.halfcompleted.append(env);
            // 上一级
            if (classSymbol.owner.kind == LzyClassReader.PCK){
                // 访问者设计模式分发    ---> 处理导包(其他不处理)
                this.memberEnter((LzyJCTree) env.toplevel,env.enclosing(LzyJCTree.TOPLEVEL));// 触发对编译单元的访问处理 ---> 简介触发对import导包处理
                // 保存环境
                this.todo.append(env);
            }
            // 对类符号进行标记: 已处理，但是没有attribute.即，引用消除
            classSymbol.flags_field |= LzyFlags.UNATTRIBUTED;
            // 如果是内部类
            if (classSymbol.owner.kind == LzyClassReader.TYP){
                // 上一级类符号填充
                classSymbol.owner.complete();
            }
            // 处理继承: 默认继承java.lang.Object
            LzyType extendsType;
            LzyType implementType;
            if (jcClassDecl.extending == null){
                // java.lang.Object
                // 到这里只是完成了符号填充
                extendsType = this.syms.objectType;
            }else{
                // 用户自定义的继承
                // 这里会进行引用消除
                extendsType = this.lzyAttr.attribBase(jcClassDecl.extending,env,true,false);
            }
            // 此处省略: 泛型
            LzyListBuffer impleList = new LzyListBuffer();
            for (LzyJCTree.JCExpression expression : jcClassDecl.implementing) {
                // 这里会进行引用消除
                implementType = this.lzyAttr.attribBase(expression,env,false,true);
                // 如果是for循环
                if (implementType.tag == 10){
                    impleList.append(implementType);
                }
            }
            // 设置
            classType.supertype_field = extendsType;
            classType.interfaces_field = impleList.toList();
            // 如果当前类是java.lang.Object
            if (classSymbol.fullname == this.names.java_lang_Object){
                if (jcClassDecl.extending != null){
                    classType.supertype_field = LzyType.noType;
                }else if (jcClassDecl.implementing.nonEmpty()){
                    classType.interfaces_field = LzyList.nil();
                }
            }
            // 1.不是一个接口 2.没有构造方法
            // 我们将添加默认的无参构造方法
            if (  (classSymbol.flags()&LzyFlags.INTERFACE) == 0L && !LzyTreeInfo.hasConstructors(jcClassDecl.defs)){
                LzyList argTypes = LzyList.nil();
                LzyList thrown = LzyList.nil();
                // 是否开启解析: xx.super()
                boolean based = false;
                // 注意: 针对匿名类的情况，我们忽略处理!
                if (classSymbol.name.length == 0){
                    // ...
                }
                LzyJCMethodDef jcMethodDecl = this.DefaultConstructor(
                        treeMaker.at(jcClassDecl.pos)
                        , classSymbol
                        , argTypes
                        , thrown
                        , based
                );
                // 类的成员: 添加一个构造方法
                jcClassDecl.defs  = jcClassDecl.defs.prepend(jcMethodDecl);
            }
            // 1.不是一个接口
            // 我们将添加this 和 super
            // 疑惑: 为什么this和super占用同一个位置 ? 1025
            if ((classSymbol.flags()&LzyFlags.INTERFACE) == 0L){
                LzyVarSymbol varSymbol = new LzyVarSymbol(LzyFlags.FINAL | HASINIT, this.names._this, classSymbol.type, classSymbol);
                varSymbol.pos = 1025;
                // 变量添加到类的成员作用域中
                ((LzyAttrContext)env.info).scope.enter(varSymbol);
                // 添加super
                if (classType.supertype_field.tag == LzyTypeTags.CLASS){
                    LzyVarSymbol varSymbol2 = new LzyVarSymbol(LzyFlags.FINAL | HASINIT, this.names._super, classType.supertype_field, classSymbol);
                    varSymbol2.pos = 1025;
                    // 变量添加到类的成员作用域中
                    ((LzyAttrContext)env.info).scope.enter(varSymbol2);
                }
            }



            // 条件: halfcompleted在之前是空的
            if (isEmpty){
                // 条件: halfcompleted在之后添加数据
                while (this.halfcompleted.nonEmpty()){
                    // 处理类成员: 1. 变量 2.方法
                    this.finish((LzyEnv)this.halfcompleted.next());
                }
            }
        }
    }

    // 父类的无参调用
    LzyJCTree.JCStatement SuperCall(LzyTreeMaker treeMaker, LzyList<LzyJCVarDef> params,boolean based){
        // 调用父类的构造方法 ---> 表达式
        LzyJCTree.JCExpression meth ;
        // 调用: xx.super();
        if (based){
            meth = treeMaker.Select(treeMaker.Ident(params.head),names._super);
        }else{ // 调用: super();
            meth = treeMaker.Ident(this.names._super);
        }
        // 创建语句: 调用父类无参构造方法
        return treeMaker.Exec(treeMaker.Apply(meth,treeMaker.Idents(params)));
    }



    // 添加默认的构造方法
    LzyJCMethodDef DefaultConstructor(LzyTreeMaker treeMaker, LzyClassSymbol classSymbol,LzyList argList,LzyList thrown,boolean based){
        // 创建一组参数列表
        LzyList<LzyJCVarDef> params = treeMaker.Params(argList,this.syms.noSymbol);
        // 语句
        LzyList<LzyJCTree.JCStatement> stats = LzyList.nil();
        // 添加调用父类无参构造方法super()
        if (classSymbol.type != this.syms.objectType){
            stats = stats.prepend(this.SuperCall(treeMaker,params,based));
        }
        // 清空标注: 只含权限修饰符
        long accessFlags = classSymbol.flags()& AccessFlags;
        // 创建初始化方法
        LzyJCMethodDef methodDecl = treeMaker.MethodDef(accessFlags
                , this.names.init
                , null
                , LzyList.nil()
                , params
                , treeMaker.Types(thrown)
                , treeMaker.Block(0, stats)
                );
        return methodDecl;
    }

    private void finish(LzyEnv env){
        // 获取环境中的顶层节点
        LzyJCClassDef classDecl = (LzyJCClassDef) env.tree;
        // 第二阶段的填充
        this.memberEnter(classDecl.defs,env); // 这里的环境env是一个类环境
    }
}


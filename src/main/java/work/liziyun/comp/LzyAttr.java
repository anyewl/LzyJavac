package work.liziyun.comp;





import work.liziyun.code.LzyScope;
import work.liziyun.code.LzySymtab;
import work.liziyun.code.symbol.LzyClassSymbol;
import work.liziyun.code.symbol.LzyMethodSymbol;
import work.liziyun.code.symbol.LzySymbol;
import work.liziyun.code.symbol.LzyVarSymbol;
import work.liziyun.code.type.LzyArrayType;
import work.liziyun.code.type.LzyMethodType;
import work.liziyun.code.type.LzyType;
import work.liziyun.code.type.LzyTypeTags;
import work.liziyun.tag.LzyFlags;
import work.liziyun.tag.LzyKinds;
import work.liziyun.tree.*;
import work.liziyun.tree.express.*;
import work.liziyun.tree.state.*;
import work.liziyun.util.LzyContext;
import work.liziyun.util.LzyList;
import work.liziyun.util.LzyListBuffer;
import work.liziyun.world.LzyName;
import work.liziyun.world.LzyTable;

import java.util.HashSet;
import java.util.Set;

/**
 * 作者: 李滋芸
 * 微信公众号: 小豆的奇思妙想
 *
 * 标注语法树阶段: 在抽象语法树的基础上,完成符号和类型的填充。即Symbol 和 Type
 *      语法树节点分两类: 1. 直接创建符号   2. 需要引用别的节点中已经创建好的符号
 *      第一类:
 *          1. JCCompilation  --->PackageSymbol
 *          2. JCClassDecl    --->ClassSymbol
 *          3. JCMethodDecl   --->MethodSymbol
 *          4. JCVariableDecl --->VarSymbol
 *      第二类:
 *          1. JCNewClass     --->引用已经创建的Symbol
 *          2. JCAssignOp     --->引用已经创建的Symbol
 *          3. JCUnary        --->引用已经创建的Symbol
 *
 *          4. JCBinary       --->引用已经创建的Symbol
 *          5. JCFieldAccess  --->引用已经创建的Symbol
 *          6. JCIdent        --->引用已经创建的Symbol
 *
 *注意: 第一类情况，一节在符号填充的第一阶段和第二阶段完成。 ---> LzyEnter 和 LzyMemberEnter
 *     当前类,主要处理第二阶段!
 */
public class LzyAttr extends LzyVisitor {
    private static final LzyContext.Key attrKey = new LzyContext.Key();
    //
    LzyEnv env;
    // 目前预期的原型
    int pkind;
    // 当前预期的原型
    LzyType pt;
    //
    LzyType result;
    //
    LzySymtab syms;
    // 类型的引用解析
    LzyResolve resolve;
    // 常量折叠
    final LzyConstFold constFold;
    // 检查
    final  LzyCheck check;
    //
    final LzyTable names;
    // 符号填充第一阶段
    final LzyEnter enter;
    // 符号填充第二阶段
    final LzyMemberEnter memberEnter;
    // 创建者
    final LzyTreeMaker treeMaker;

    //
    LzyListBuffer methTemplateSupply = new LzyListBuffer();



    public LzyAttr(LzyContext LzyContext) {
        LzyContext.put(attrKey,this);
        this.syms = LzySymtab.instance(LzyContext);
        this.constFold = LzyConstFold.instance(LzyContext);
        this.check = LzyCheck.instance(LzyContext);
        this.names = LzyTable.instance(LzyContext);
        this.memberEnter = LzyMemberEnter.instance(LzyContext);
        this.treeMaker = LzyTreeMaker.instance(LzyContext);
        this.resolve = LzyResolve.instance(LzyContext);
        this.enter = LzyEnter.instance(LzyContext);
    }


    public static LzyAttr instance(LzyContext LzyContext) {
        LzyAttr attr = (LzyAttr)LzyContext.get(attrKey);
        if (attr == null) {
            attr = new LzyAttr(LzyContext);
        }
        return attr;
    }



    public void attribClass(int i,LzyClassSymbol classSymbol){
        this.attribClass(classSymbol);
    }

    void attribClass(LzyClassSymbol classSymbol){
        // 不是错误的类型
        if ( classSymbol.type.tag != LzyTypeTags.ERROR ){
            // 检查: 会触发父类的填充
            this.check.checkNonCyclic(0,classSymbol.type);
            // 父类
            LzyType supertype = classSymbol.type.supertype();
            // 如果类型正确
            if (supertype.tag == LzyTypeTags.CLASS){
                this.attribClass( (LzyClassSymbol)supertype.tsym );
            }
            // 内部类情况: 处理外部类
            if ( classSymbol.owner.kind == LzyKinds.TYP && classSymbol.owner.type.tag == LzyTypeTags.CLASS ){

            }
            // 如果是没有进行引用消除
            if ( (classSymbol.flags_field&LzyFlags.UNATTRIBUTED) != 0 ){
                // 清楚标记
                classSymbol.flags_field &= (-(LzyFlags.UNATTRIBUTED+1));
                // 符号填充的第一阶段中移除
                LzyEnv classEnv = this.enter.classEnvs.remove(classSymbol);
                // 处理类体
                this.attribClassBody(classEnv,classSymbol);
            }
        }
    }

    private void attribClassBody(LzyEnv env,LzyClassSymbol classSymbol){
        LzyJCClassDef classDef = (LzyJCClassDef)env.tree;
        //设置类型
        classDef.type = classSymbol.type;
        // 所有的类成员
        LzyList<LzyJCTree> defs = classDef.defs;
        while ( defs.nonEmpty() ){
            // 引用消除: 类的成员
            this.attribStat( (LzyJCTree)defs.head , env );
            // 忽略内部类
            defs = defs.tail;
        }
    }


    LzyType attribType(LzyJCTree var1, LzyEnv var2) {
        return this.attribTree(var1, var2, LzyKinds.TYP, LzyType.noType);
    }



    /**
     *
     * @param tree
     * @param env   当前的环境
     * @param pkind 期望的种类 --> Kinds ---> 变量，方法
     * @param type  期望的类型 --> 例: 期望是一个boolean
     * @return
     */
    LzyType attribTree(LzyJCTree tree, LzyEnv env, int pkind, LzyType type){
        LzyEnv prevEnv = this.env;
        int prevPkind = this.pkind;
        LzyType prevPt = this.pt;
        // 出现错误的返回
        LzyType errorType ;
        try{
            this.env = env;
            this.pkind = pkind;
            this.pt = type;
            tree.accept(this);
            return  this.result;
        }catch (Exception completionFailure){
            tree.type = this.syms.errorType;
            errorType = null;
            throw completionFailure;
        }finally {
            this.env = prevEnv;
            this.pkind = prevPkind;
            this.pt = prevPt;
        }


    }

    LzyType attribStat(LzyJCTree tree,LzyEnv env){
        return this.attribTree(tree,env,LzyKinds.NIL,LzyType.noType);
    }

    LzyList<LzyType> attribExprs(LzyList<LzyJCTree.JCExpression> tress, LzyEnv env, LzyType pt){
        LzyListBuffer LzyListBuffer = new LzyListBuffer();
        for (LzyList<LzyJCTree.JCExpression> l = tress; l.nonEmpty(); l=l.tail) {
            LzyListBuffer.append(attribExpr(l.head,env,pt));
        }
        return LzyListBuffer.toList();
    }

    LzyType attribExpr(LzyJCTree tree,LzyEnv env,LzyType type){
        return this.attribTree(tree,env,LzyKinds.VAL,type);
    }

    LzyType  attribBase(LzyJCTree tree,LzyEnv env,boolean classExpected,boolean interfaceExpected){
        // Flags.INTERFACE;
        LzyType type = this.attribType(tree,env);
        // 检查Type是否符合Class --> 我们期望是一个类或者接口
        type = this.check.checkClassType(tree.pos,type);// TypeTags.Class
        // 期望一个接口
        if (interfaceExpected && (type.tsym.flags()& LzyFlags.INTERFACE) == 0 ){
            // 实际上不是一个接口
            System.out.println("编译错误: 我们期望一个接口!");
            return this.syms.errType;
        }else if (classExpected && (type.tsym.flags()&LzyFlags.INTERFACE) != 0 ){ // 期望一个类
            // 实际上不是一个类
            System.out.println("编译错误: 我们期望一个类!");
            return this.syms.errType;
        }else{
            // 如果这是一个final修饰的类，那么不允许有继承!
            if ( (type.tsym.flags()&LzyFlags.FINAL) != 0 ){
                System.out.println("编译错误: final修饰的不能继承!");
            }
        }
        return type;
    }

    LzyType attribExpr(LzyJCTree tree,LzyEnv env){
        return this.attribTree(tree,env,LzyKinds.VAL,LzyType.noType);
    }

    void attribStats(LzyList LzyList, LzyEnv env){
        while (LzyList.nonEmpty()){
            this.attribStat( (LzyJCTree) LzyList.head , env );
            LzyList = LzyList.tail;
        }
    }

    LzyList attribArgs(LzyList LzyList,LzyEnv env){
        LzyListBuffer LzyListBuffer = new LzyListBuffer();
        while (LzyList.nonEmpty()){
            LzyListBuffer.append(this.attribTree((LzyJCTree) LzyList.head,env,LzyKinds.VAL, new LzyType(LzyTypeTags.ERROR,null)));
            LzyList = LzyList.tail;
        }
        return LzyListBuffer.toList();
    }


    LzyType litType(int tag) {
        // 如果是引用数据类型，那么当做String处理
        return tag == LzyTypeTags.CLASS ? this.syms.stringType : this.syms.typeOfTag[tag];
    }


    boolean checkFirstConstructorStat(LzyJCApply methodInvocation, LzyEnv env){
        // 附加方法
        LzyJCMethodDef enclMethod = env.enclMethod;
        if ( enclMethod != null && enclMethod.name == names.init ){
            // 方法体
            LzyJCBlock body = enclMethod.block;
            // 方法的数据域
            if ( body.stats.head.getTag() == LzyJCTree.EXEC
                && ((LzyJCExpressionStatement)body.stats.head).expr == methodInvocation
            ){
                return true;
            }
        }
        return false;
    }

    LzyType newMethTemplate(LzyList LzyList){
        // 上一个
        if (this.methTemplateSupply.elems == this.methTemplateSupply.last){
            LzyMethodType methodType = new LzyMethodType(null,null,null,this.syms.methodClass);
            this.methTemplateSupply.append(methodType);
        }
        // 数据域
        LzyMethodType methodType = (LzyMethodType)this.methTemplateSupply.elems.head;
        // 移动到下一个
        this.methTemplateSupply.elems = this.methTemplateSupply.elems.tail;
        // 方法的参数列表
        methodType.argtypes = LzyList;
        return methodType;
    }

    // 方法的调用
    @Override
    public void visitJCApply(LzyJCApply methodInvocation){
        LzyEnv env2 = this.env;
        LzyName name = LzyTreeInfo.name(methodInvocation.meth);
        boolean isThisOrSuper = (name == this.names._this || name == this.names._super);
        LzyList methodArgList;
        // 处理this或super
        if (isThisOrSuper){
            // 是否方法体中的第一句
            if ( this.checkFirstConstructorStat(methodInvocation,this.env) ){
                // 新环境
                env2 = this.env.dup(this.env.tree,((LzyAttrContext)this.env.info).dup());
                // 对this() 或 super() 调用
                ((LzyAttrContext)env2.info).isSelfCall = true;
                // 方法调用
                methodArgList = this.attribArgs(methodInvocation.args,env2);
                // 附加类的类型
                LzyType classType = this.env.enclClass.sym.type;
                // 父类
                if (name == this.names._super){
                    classType = classType.supertype();
                }
                // 确定类型是一个类
                if (classType.tag == LzyTypeTags.CLASS){
                    // 注意: 省略内部类向外部类查找的细节
/*                    boolean selectSuperPrev = ((LzyAttrContext) env2.info).selectSuper;*/
       /*             ((LzyAttrContext)env2.info).selectSuper = true;*/
                    // 解析构造方法
                    LzySymbol constructSymbol = this.resolve.resolveConstructor(methodInvocation.meth.pos,env2,classType,methodArgList);
                    // 还原
/*                    ((LzyAttrContext)env2.info).selectSuper = selectSuperPrev;*/
                    // 设置符号
                    LzyTreeInfo.setSymbol(methodInvocation.meth,constructSymbol);
                    // 检查上下文合法 并 设置树类型
                    LzyList elems = this.methTemplateSupply.elems;
                    LzyType methodType = this.newMethTemplate(methodArgList);
                    // 确保ident即super标识符的type
                    methodInvocation.meth.type = methodType;
                }
            }
            this.result = methodInvocation.type = this.syms.voidType;
        }else{
            // 方法的参数列表
            methodArgList = this.attribArgs(methodInvocation.args,env2);
            // 方法模板
            LzyList methTemplateList = this.methTemplateSupply.elems;
            // 创建新方法模板
            LzyType methodType = this.newMethTemplate(methodArgList);
            LzyType m = this.attribExpr(methodInvocation.meth, env2, methodType);
            this.methTemplateSupply.elems = methTemplateList;
            this.result = methodInvocation.type =  m.restype();
        }
    }


    // 二元运算
    @Override
    public void visitJCBinary(LzyJCBinary binary){
        // 左边表达式
        LzyType leftType = this.attribExpr(binary.lhs, this.env);
        // 右边表达式
        LzyType rightType = this.attribExpr(binary.rhs, this.env);
        // 运算符
        LzySymbol operatorSymbol = binary.operator = this.resolve.resolveBinaryOperator(binary.opcode,leftType,env,rightType);
        // 方法返回值
        LzyType rType = null;
        if (operatorSymbol.kind == LzyKinds.MTH
                && leftType != null
                && rightType != null
            ){
            rType = ((LzyMethodType)operatorSymbol.type).restype;
            // 运算符的编号
            int opc = ((LzyMethodSymbol.OperatorSymbol)operatorSymbol).opcode;
            // 常量折叠
            if (leftType.constValue != null && rightType.constValue != null  ){
                LzyType cType = constFold.fold2(opc,leftType,rightType);
                // String类型的检验
                if (binary.lhs.type.tsym == syms.stringType.tsym){
                    binary.lhs.type = syms.stringType;
                }
                if (binary.rhs.type.tsym == syms.stringType.tsym){
                    binary.rhs.type = syms.stringType;
                }
                // 除0检查
                check.checkDivZero(operatorSymbol,rightType);
            }
        }
        binary.type = rType;
        this.result = rType;
    }


    // 代码块
    @Override
    public void visitJCBlock(LzyJCBlock block){
        if ( ((LzyAttrContext)this.env.info).scope.owner.kind == LzyKinds.TYP ){
            // 创建出新的环境
            LzyEnv localEnv = this.env.dup(block, ((LzyAttrContext) this.env.info).dup(((LzyAttrContext) this.env.info).scope.dupUnshared()));
            // 设置当前环境的父
            ((LzyAttrContext)localEnv.info).scope.owner = new LzyMethodSymbol(block.flags|LzyFlags.BLOCK,names.empty,null,((LzyAttrContext) this.env.info).scope.owner );
            // 静态
            if (  (block.flags&LzyFlags.STATIC) != 0 ){
                ((LzyAttrContext) localEnv.info).staticLevel++;
            }
            attribStats(block.stats,localEnv);
        }else{
            // 局部作用域
            LzyEnv localEnv = this.env.dup(block, ((LzyAttrContext) this.env.info).dup(((LzyAttrContext) this.env.info).scope.dup()));
            attribStats(block.stats,localEnv);
            ((LzyAttrContext) localEnv.info).scope.leave();
        }
        result = null;
    }

    @Override
    public void visitJCBreak(LzyJCBreak jcBreak){
        jcBreak.target = findJumpTarget(jcBreak.getTag(),jcBreak.label,env);
        result = null;
    }
    @Override
    public void visitJCContinue(LzyJCContinue jcContinue){
        jcContinue.target = findJumpTarget(jcContinue.getTag(),jcContinue.label,env);
        this.result = null;
    }

    @Override
    public void visitJCDoloop(LzyJCDoloop doWhileLoop){
        this.attribStat(doWhileLoop.body,this.env.dup(doWhileLoop));
        this.attribExpr(doWhileLoop.cond,this.env,syms.booleanType);
        this.result = null;
    }


    @Override
    public void visitJCExec(LzyJCExpressionStatement expressionStatement){
        this.attribExpr(expressionStatement.expr,this.env);
        this.result = null;
    }


    /**
     * 例如: 循环中的if作用域，需要向外部查找到循环节点。并返回。
     * @param tag
     * @param label
     * @param env
     * @return
     */
    private LzyJCTree findJumpTarget(int tag,LzyName label,LzyEnv env){
        LzyEnv env2 = env;
        LOOP:
        while (env2 != null){
            switch (env2.tree.getTag()){
                case LzyJCTree.DOLOOP:
                case LzyJCTree.WHILELOOP:
                case LzyJCTree.FORLOOP:
                case LzyJCTree.FOREACHLOOP: // 增强for循环
                    if (label == null) return env2.tree;
                    break;
                case LzyJCTree.SWITCH:
                    if (label == null && tag == LzyJCTree.BREAK)return env2.tree;
                    break;
                case LzyJCTree.METHODDEF:
                case LzyJCTree.CLASSDEF:
                    break LOOP; // 直接完全终止迭代
            }
            // 向外部环境中寻找
            env2 = env2.next;
        }
        return null;
    }

    // 赋值语句
    @Override
    public void visitJCAssign(LzyJCAssign assign){
        // 赋值语句左边
        LzyType leftType = this.attribTree(assign.lhs,this.env.dup(assign),LzyKinds.VAR,this.pt);
        // 赋值语句右边
        this.attribExpr(assign.rhs,this.env,leftType);
        assign.type  = leftType;
        this.result = leftType;
    }

    // 复合赋值运算
    @Override
    public void visitJCAssignop(LzyJCAssignop assignOp){
        // 期望: 变量
        LzyType leftType = this.attribTree(assignOp.lhs, this.env, LzyKinds.VAR, LzyType.noType);
        // 期望: 变量 或 变量表达式
        LzyType rightType = this.attribExpr(assignOp.rhs, env);
        // 方法的参数列表: 赋值运算前后
        LzyList<LzyType> argList = LzyList.of(leftType, rightType);
        // 运算符 ---> 方法名
        LzySymbol methodSymbol = assignOp.operator = this.resolve.resolveOperator(assignOp.pos,assignOp.getTag()-LzyJCTree.ASGOffset,this.env,argList);
        //
        this.result = assignOp.type = argList.head;
    }



    // 获取基本数据类型对应的Type
    @Override
    public void visitJCTypeIdent(LzyJCTypeIdent tree){
        this.result = tree.type = syms.typeOfTag[tree.typetag];
    }

    // 标识符
    @Override
    public void visitJCIdent(LzyJCIdent ident)  {
        LzySymbol symbol;
        // 如果是方法Type
        if (this.pt.tag == LzyTypeTags.METHOD){
            symbol = this.resolve.resolveMethod(ident.pos,this.env,ident.name,((LzyMethodType)this.pt).argtypes);
        }else{
            // 尝试解析: 变量，类型，包
            symbol = this.resolve.findIdent(this.env,ident.name,this.pkind);
        }
        ident.symbol = symbol;
        ident.type = symbol.type;
        this.result  = symbol.type;
    }

    // For循环
    @Override
    public void visitJCForLoop(LzyJCForLoop forLoop){
        // For的环境是在方法中，所以需要从方法中的环境中创建
        LzyEnv loopEnv = this.env.dup(this.env.tree, ((LzyAttrContext)this.env.info).dup(((LzyAttrContext)this.env.info).scope.dup())  );
        // 初始化语句
        this.attribStats(forLoop.init,loopEnv);
        // 循环条件
        if (forLoop.cond != null){
            this.attribExpr(forLoop.cond,loopEnv,syms.booleanType);
        }
        // 环境设置树节点
        loopEnv.tree = forLoop;
        // 循环增量
        this.attribStats(forLoop.step,loopEnv);
        this.attribStat(forLoop.body,loopEnv);
        // 清空
        ((LzyAttrContext)loopEnv.info).scope.leave();
        this.result = null;
    }


    @Override
    public void visitJCIf(LzyJCIf jcIf){
        // 判断条件
        this.attribExpr(jcIf.cond,this.env,this.syms.booleanType);
        // 成立的语句
        this.attribExpr(jcIf.thenpart,this.env,this.pt);
        // 不成立的语句
        if (jcIf.elsepart != null){
            this.attribExpr(jcIf.elsepart,this.env,this.pt);
        }
        this.result = null;
    }

    @Override
    public void visitJCIndexed(LzyJCIndexed arrayAccess){
        LzyType eleType = null;
        LzyType arrType = this.attribExpr(arrayAccess.indexed, this.env);
        this.attribExpr(arrayAccess.index,this.env,this.syms.intType);
        if (arrType.tag == LzyTypeTags.ARRAY){
            eleType = ((LzyArrayType)arrType).elemtype;
        }
        arrayAccess.type = eleType;
        this.result = eleType;
    }

    @Override
    public void visitJCLiteral(LzyJCLiteral literal){
        // 创建常量类型 ---> 数据valiue
        LzyType type = this.litType(literal.typetag).constType(literal.value);
        literal.type = type;
        this.result = type;
    }

    @Override
    public void visitJCMethodDef(LzyJCMethodDef methodDecl){
        LzyMethodSymbol methodSymbol = methodDecl.sym;
        // 创建方法的环境
        LzyEnv methodEnv = this.memberEnter.methodEnv(methodDecl,this.env);
        // 参数
        LzyList argList = methodDecl.params;
        while ( argList.nonEmpty() ){
            this.attribStat((LzyJCTree) argList.head,methodEnv);
            argList = argList.tail;
        }
        // 类符号
        LzyClassSymbol classSym = this.env.enclClass.sym;
        // 初始化方法
        if(methodDecl.name == this.names.init && classSym.type != this.syms.objectType ){
            LzyJCBlock block = methodDecl.block;
            // 代码块
            if(block.stats.isEmpty() || !LzyTreeInfo.isSelfCall(block.stats.head)){
                // 追加一条语句: 调用父类无参构造方法
                block.stats = block.stats.prepend(this.memberEnter.SuperCall(treeMaker.at(block.pos),LzyList.nil(),LzyList.nil(),false));
            }
        }
        if(methodDecl.block != null){
            if (  (classSym.flags() & LzyFlags.INTERFACE) != 0){
                System.out.println("编译错误: 接口中方法不能有方法体!");
            }
            // 方法体中内容进行引用消除
            this.attribStat(methodDecl.block,methodEnv);
        }

        // 环境清空
        ((LzyAttrContext)methodEnv.info).scope.leave();
        this.result = methodDecl.type = methodSymbol.type;

    }

    @Override
    public void visitJCNewArray(LzyJCNewArray newArray){
        LzyType rsType = null;
        LzyType elemtype = null;
        if (newArray.elemtype != null){
            elemtype = attribType(newArray.elemtype,this.env);
            rsType = elemtype;
            for ( LzyList<LzyJCTree.JCExpression> l =  newArray.dims; l.nonEmpty()  ; l = l.tail ) {
                attribExpr(l.head,env,syms.intType);
                rsType = new LzyArrayType(rsType,syms.arrayClass);
            }
        }else{
            if (this.pt.tag == LzyTypeTags.ARRAY){
                elemtype = pt.elemtype();
            }
        }

        if (newArray.elems != null){
            attribExprs(newArray.elems,env,elemtype);
            rsType = new LzyArrayType(elemtype,this.syms.arrayClass);
        }
        this.result = newArray.type = rsType;
    }

    @Override
    public void visitJCNewClass(LzyJCNewClass newClass){
        LzyType rsType = null;
        // 这里在处理匿名内部类
        LzyJCClassDef classDecl = newClass.def;
        // 实例化的类型
        LzyType type = this.attribType(newClass.clazz,this.env);
        // 构造方法的参数列表
        LzyList argList = this.attribArgs(newClass.args,this.env);
        // 实例化的是一个类： 普通类,接口，抽象类
        if (type.tag == LzyTypeTags.CLASS){
            // 如果在实例接口或抽象方法： 产生编译错误！
            if (classDecl == null && (type.tsym.flags() & (LzyFlags.INTERFACE|LzyFlags.ABSTRACT)) !=0   ){
                System.out.println("编译错误: 抽象类不能被实例化!");
            }else{
                // 注意: 匿名内部类的情况不讨论！那么ClassDecl无效!
/*                boolean selectSuper = ((LzyAttrContext)this.env.info).selectSuper;*/
                if (classDecl != null){
                    System.out.println("我们不讨论匿名内部类的情况!");
                }
                // 解析构造方法: 形成初始化init方法
                newClass.constructor = this.resolve.resolveConstructor(newClass.pos,this.env,type,argList) ;
   /*             ((LzyAttrContext)this.env.info).selectSuper = selectSuper;*/
            }
            // 注意: 我们不考虑匿名内部类的情和处理
            if (newClass.constructor != null && newClass.constructor.kind == LzyKinds.MTH){
                rsType = type;
            }
        }
        this.result = newClass.type = rsType;
    }


    // 这里应该是大括号节点
    @Override
    public void visitJCParens(LzyJCParens parens){
        LzyType type = this.attribTree(parens.expr,this.env,this.pkind,this.pt);
        this.result = parens.type = type;
    }



    @Override
    public void  visitJCReturn(LzyJCReturn jcReturn){
        // 如果在方法内部
        if ( this.env.enclMethod != null && this.env.enclMethod.sym.owner == this.env.enclClass.sym){
            LzyMethodSymbol methodSymbol = this.env.enclMethod.sym;
            // 如果方法的返回值是void
            if (methodSymbol.type.restype().tag == LzyTypeTags.VOID){
                if (jcReturn.expr != null){
                    System.out.println("编译错误: void方法,不能拥有返回值!");
                }
            }else if (jcReturn.expr == null){
                System.out.println("编译错误: 缺少返回值!");
            }else{
                this.attribExpr(jcReturn.expr,this.env,((LzyMethodType)methodSymbol.type).restype());
            }
        }else{ // 如果在方法外部
            System.out.println("return出现在方法外部!");
        }
        this.result = null;
    }


    @Override
    public void visitJCSelect(LzyJCSelect fieldAccess){
        // 确定限定符表达式 预期的类型
        int skind = 0;
        // 确定预期的类型
        if ( fieldAccess.name == this.names._this
            || fieldAccess.name == this.names._super
                || fieldAccess.name == this.names._class
        ){
            // 期望的是类型
            skind = LzyKinds.TYP;
        }else{
            // 期望包
            if ( (pkind&LzyKinds.PCK) != 0 ){
                skind = skind | LzyKinds.PCK;
            }
            // 期望类型
            if ( (pkind&LzyKinds.TYP) != 0 ){
                skind = skind| LzyKinds.TYP | LzyKinds.PCK;
            }
            // 期望变量表达式 或 方法
            if ( (pkind&(LzyKinds.VAL|LzyKinds.MTH)) != 0 ){
                skind = skind | LzyKinds.VAL | LzyKinds.TYP;
            }
        }

        LzyType site = this.attribTree(fieldAccess.selected,this.env,skind,LzyInfer.anyPoly);
        // 获取符号
        LzySymbol siteSym = LzyTreeInfo.symbol(fieldAccess.selected);

        //
/*        boolean selectSuperPrev = ((LzyAttrContext)this.env.info).selectSuper;
        if (siteSym != null && (siteSym.name==this.names._super||siteSym.kind == LzyKinds.TYP)   ){
            ((LzyAttrContext)this.env.info).selectSuper = true;
        }else{
            ((LzyAttrContext)this.env.info).selectSuper = false;
        }*/

        LzySymbol selectSym = this.selectSym(fieldAccess, site, this.env, this.pt, this.pkind);
        fieldAccess.sym = selectSym;
        // 期望: 一个变量
        if (selectSym.kind == LzyKinds.VAR){
            // 当做一个变量符号
            LzyVarSymbol varSymbol = (LzyVarSymbol)selectSym;
            // 变量的初始化
            this.evalInit(varSymbol);// ensure initializer is evaluated
        }

        //
/*        if ( isType(selectSym)
                && (siteSym == null || (siteSym.kind&(LzyKinds.TYP|LzyKinds.PCK)) == 0 )
        ){
            fieldAccess.type = fieldAccess.selected.type = this.pt;
        }
        // 如果是super实例 或 类名开头
        if (  ((LzyAttrContext)this.env.info).selectSuper  ){
            // 不是静态,不是this,不是super
            if ( (selectSym.flags()&LzyFlags.STATIC)==0
                && selectSym.name != this.names._this
                    && selectSym.name != this.names._super
            ){
                *//*
                    类型擦除
                    if ( site.isRaw() ){
                        LzyType site2 = types.asSuper(this.env.enclClass.sym.type,site.tsym);
                        if (site2!=null) site = site2;
                     }
                *//*
            }
        }
        //
        ((LzyAttrContext)this.env.info).selectSuper = selectSuperPrev;*/
        // 强行触发包符号
        selectSym.flags();
        // 这里调用flags()方法间接触发包符号的填充 ----> 类符号的创建
        if (selectSym instanceof LzyVarSymbol && ((LzyVarSymbol)selectSym).constValue != null ){
               this.result = fieldAccess.type = selectSym.type.constType( ((LzyVarSymbol)selectSym).constValue );
        }else {
            this.result = fieldAccess.type = selectSym.type;
        }
        ((LzyAttrContext)this.env.info).tvars = LzyList.nil();
    }

    static boolean isType(LzySymbol symbol){
        return symbol != null && symbol.kind == 2;
    }



    private LzySymbol selectSym(LzyJCSelect fieldAccess,LzyType type,LzyEnv env,LzyType pt,int pkind){
        int pos = fieldAccess.pos;
        LzyName name = fieldAccess.name;
        switch (type.tag){
            case LzyTypeTags.CLASS:
            case LzyTypeTags.ARRAY:
                // 期望的是一个方法
                if (pt.tag == LzyTypeTags.METHOD){
                    return this.resolve.resolveQualifiedMethod(pos,env,type,name,((LzyMethodType)pt).argtypes);
                }else{
                    // 不是this也不是super
                    if (name != this.names._this && name != this.names._super ){
                        if (name == this.names._class){
                            return new LzyVarSymbol( (LzyFlags.STATIC|LzyFlags.FINAL|LzyFlags.PUBLIC),this.names._class,this.syms.classType,type.tsym);
                        }
                        return this.resolve.findIdentInType(env,type,name,pkind);
                    }
                    return this.resolve.resolveSelf(pos,env,type.tsym,name);
                }
            case LzyTypeTags.METHOD:
            case LzyTypeTags.BOT:
            default:
                if (name == this.names._class){
                    return new LzyVarSymbol(LzyFlags.FINAL|LzyFlags.STATIC|LzyFlags.PUBLIC,this.names._class,this.syms.classType,type.tsym);
                }
                return this.syms.errSymbol;
            case LzyTypeTags.PACKAGE:
                return this.resolve.findIdentInPackage(env,type.tsym,name,pkind);
            case LzyTypeTags.NONE:
                System.out.println("编译错误: LzyAttr.selectSym()! ");
                return null;
        }
    }


    @Override
    public void visitJCSkip(LzyJCSkip jcSkip){
        this.result = null;
    }

    @Override
    public void visitJCSwitch(LzyJCSwitch jcSwitch){
        // 条件的类型
        LzyType selectType = this.attribExpr(jcSwitch.selector, this.env, this.syms.intType);
        // 拷贝一个新环境
        LzyEnv switchEnv = this.env.dup(jcSwitch, ((LzyAttrContext) this.env.info).dup(((LzyAttrContext) this.env.info).scope.dup()));
        // 默认
        boolean hasDefault = false;
        // Set容器中存储常量值
        Set set = new HashSet();
        // 处理各个分支
        for ( LzyList caseList = jcSwitch.cases ; caseList.nonEmpty() ; caseList = caseList.tail ){
            // case
            LzyJCCase jcCase = (LzyJCCase)caseList.head;
            // 创建case的环境
            LzyEnv caseEnv = switchEnv.dup(jcCase, ((LzyAttrContext) this.env.info).dup(((LzyAttrContext) switchEnv.info).scope.dup()));
            // case比较的值
            if (jcCase.pat != null ){
                //
                LzyType caseType = this.attribExpr(jcCase.pat, switchEnv, this.syms.intType);
                if (caseType.tag != LzyTypeTags.NONE){
                    // 将常量值存储刀到set容器中
                    set.add(caseType.constValue);
                }
            }else if (hasDefault){
                System.out.println("编译错误: 发现重复的default在swich中!");
            }else{
                // 默认分支default
                hasDefault = true;
            }
            this.attribStats(jcCase.stats,caseEnv);
            ((LzyAttrContext)caseEnv.info).scope.leave();
            // 所有的变量节点添加到scope中
            addVars(jcCase.stats,((LzyAttrContext)switchEnv.info).scope);
        }
        ((LzyAttrContext)switchEnv.info).scope.leave();
        this.result = null;
    }

    private static void addVars(LzyList LzyList, LzyScope scope){
        while (LzyList.nonEmpty()){
            // 数据域
            LzyJCTree jcTree = (LzyJCTree)LzyList.head;
            // 当前节点是一个JCVariableDecl节点
            if (jcTree.getTag() == LzyJCTree.VARDEF){
                scope.enter( ((LzyJCVarDef)jcTree).sym );
            }
            LzyList = LzyList.tail;
        }
    }

    @Override
    public void visitJCThrow(LzyJCThrow jcThrow){
        this.attribExpr(jcThrow.expr,this.env,this.syms.throwableType);
        this.result = null;
    }

    @Override
    public void visitJCTree(LzyJCTree tree){
        throw new AssertionError();
    }


    @Override
    public void visitJCTry(LzyJCTry jcTry){
        // try体
        this.attribStat(jcTry.body,this.env.dup(jcTry, ((LzyAttrContext)this.env.info).dup() ));
        // 所有的catch分支
        LzyList catchList = jcTry.catchers;
        while (catchList.nonEmpty()){
            // 数据域
            LzyJCCatch jcCatch = (LzyJCCatch)catchList.head;
            // 创建出catch的环境
            LzyEnv catchEnv = this.env.dup(jcCatch,((LzyAttrContext)this.env.info).dup( ((LzyAttrContext)this.env.info).scope.dup() )  );
            // catch中的参数列表
            LzyType catchArg = this.attribStat(jcCatch.param,catchEnv);
            // catch中代码块
            this.attribStat(jcCatch.body,catchEnv);
            // 清空
            ((LzyAttrContext)catchEnv.info).scope.leave();
            catchList= catchList.tail; // 移动指针
        }
        // finally
        if (jcTry.finalizer != null){
            this.attribStat(jcTry.finalizer,this.env  );
        }


    }

    @Override
    public void visitJCTypeArray(LzyJCTypeArray jcArrayTypeTree){
        LzyType element = this.attribType(jcArrayTypeTree.elemtype, this.env);
        this.result = jcArrayTypeTree.type = new LzyArrayType(element,this.syms.arrayClass);
    }


    @Override
    public void visitJCTypeCast(LzyJCTypeCast jcTypeCast){
        LzyType clazzType = this.attribType(jcTypeCast.clazz, this.env);
        LzyType exprType = this.attribExpr(jcTypeCast.expr, this.env, LzyType.noType);
        LzyType clazzType2 = clazzType;
        if (exprType.constValue != null){
            clazzType2 = this.constFold.coerce(exprType,clazzType2);
        }
        this.result = jcTypeCast.type = clazzType2;
    }


    @Override
    public void visitJCTypeTest(LzyJCTypeTest jcInstanceOf){
        LzyType exprType = this.attribExpr(jcInstanceOf.expr, this.env);
        LzyType clazzType = this.attribType(jcInstanceOf.clazz, this.env);
        this.result = jcInstanceOf.type = this.syms.booleanType;
    }


    // 一元运算
    @Override
    public void visitJCUnary(LzyJCUnary jcUnary){
        LzyType type = null;
        // 当前节点的范围: 一元运算
        if ( jcUnary.getTag() >= LzyJCTree.POS && jcUnary.getTag() <= LzyJCTree.POSTDEC ){
            type = this.attribTree(jcUnary.arg,this.env,LzyKinds.VAR,LzyType.noType);
        }else{
            type = this.attribExpr(jcUnary.arg,this.env);
        }
        // 尝试解析一元运算符
        LzySymbol unaryOperator = jcUnary.operator = this.resolve.resolveUnaryOperator(jcUnary.pos,jcUnary.getTag(),this.env,type);
        //
        LzyType returnType = this.syms.errType;
        // 如果操作符是方法
        if (unaryOperator.kind == LzyKinds.MTH){
            returnType = ((LzyMethodType)unaryOperator.type).restype();
            //
            int opcode = ((LzyMethodSymbol.OperatorSymbol) unaryOperator).opcode;
            // 进行常量折叠
            if (type.constValue != null){
                LzyType cfcolder = this.constFold.fold1(opcode,type);
                // 常量折叠结果
                if (cfcolder != null){
                    returnType = this.constFold.coerce(cfcolder,returnType);
                    // 如果常量是字符串类型
                    if (jcUnary.arg.type.tsym == this.syms.stringType.tsym){
                        jcUnary.arg.type = this.syms.stringType;
                    }
                }
            }
        }
        //
        this.result = jcUnary.type = returnType;
    }



    @Override
    public void visitJCVarDef(LzyJCVarDef jcVariableDecl){
        // 如果是方法中的局部变量
        if (  ((LzyAttrContext)this.env.info).scope.owner.kind == LzyKinds.MTH  ){
            // 变量的引用消除
            this.memberEnter.memberEnter(jcVariableDecl,this.env);
        }
        // 变量的符号
        LzyVarSymbol varSymbol = jcVariableDecl.sym;
        // 变量的初始化
        if (jcVariableDecl.init != null){
            varSymbol.pos = 2147483647;
            // final修饰那么必须进行初始化
            if ( ( varSymbol.flags_field & LzyFlags.FINAL) != 0 ){
                evalInit(varSymbol);
            }else{
                this.attribExpr(jcVariableDecl.init,this.memberEnter.initEnv(jcVariableDecl,this.env),varSymbol.type);
            }
            varSymbol.pos = jcVariableDecl.pos;
        }
        this.result = jcVariableDecl.type = varSymbol.type;
    }

    public void evalInit(LzyVarSymbol varSymbol){
        if ( varSymbol.constValue instanceof LzyEnv ){
            LzyEnv env = (LzyEnv)varSymbol.constValue;
            // 设置null
            varSymbol.constValue = null;
            // 类型的引用消除
            LzyType type = this.attribExpr(((LzyJCVarDef)env.tree).init , env , varSymbol.type);
            if ( type.constValue != null ){
                // 常量折叠
                varSymbol.constValue = this.constFold.coerce(type,varSymbol.type).constValue;
            }
        }
    }

    @Override
    public void visitJCWhileLoop(LzyJCWhileLoop jcWhileLoop){
        // 循环条件
        this.attribExpr(jcWhileLoop.cond,this.env,this.syms.booleanType);
        // 循环体
        this.attribStat(jcWhileLoop.body,this.env.dup(jcWhileLoop));
        this.result = null;
    }



    @Override
    public void visitJCClassDef(LzyJCClassDef classDef){
        System.out.println("编译错误: Attr不支持内部类");
    }

    @Override
    public void visitJCConditional(LzyJCConditional conditional){
        this.attribExpr(conditional.cond,this.env,this.syms.booleanType);
        this.attribExpr(conditional.truepart,this.env,this.pt);
        this.attribExpr(conditional.falsepart,this.env,this.pt);
        this.result = conditional.type = this.condType(conditional.pos,conditional.cond.type,conditional.truepart.type,conditional.falsepart.type);
    }

    private LzyType condType(int i,LzyType condType,LzyType trueType,LzyType falseType){
        LzyType type = this.condType1(i, condType, trueType, falseType);
        if ( condType.constValue != null && trueType.constValue != null && falseType.constValue != null ){
            LzyType rsType = null;
            if ( ((Number)condType.constValue).intValue() != 0 ){
                rsType = trueType;
            }else {
                rsType = falseType;
            }
            return this.constFold.coerce(rsType,type);
        }else {
            return type;
        }
    }

    private LzyType condType1(int i,LzyType condType,LzyType trueType,LzyType falseType){
        // trueType可以转换成falseType: 他们都是byte,char,short,int中一种
        if (trueType.tag < LzyTypeTags.INT && falseType.tag == LzyTypeTags.INT &&  falseType.isAssignable(trueType)){
            // 最终可以转成trueType的类型
            return trueType.baseType();
        }else if (falseType.tag < LzyTypeTags.INT && trueType.tag == LzyTypeTags.INT && trueType.isAssignable(falseType) ){
            // 最终可以转成falseType的类型
            return falseType.baseType();
        }else {
            // 找到他们都属于的父类
            if ( trueType.tag <= LzyTypeTags.DOUBLE && falseType.tag <= LzyTypeTags.DOUBLE ){
                for (LzyType superType  : this.syms.typeOfTag) {
                    if ( trueType.isSubType(superType) && falseType.isSubType(superType) ){
                        return superType;
                    }
                }

            }
            // 都是String那么返回String类型
            if ( trueType.tsym == this.syms.stringType.tsym && falseType.tsym == this.syms.stringType.tsym ){
                return this.syms.stringType;
            }else if ( trueType.isSubType(falseType) ){ // 返回父类的类型
                return falseType.baseType();
            }else if ( falseType.isSubType(trueType) ){
                return trueType.baseType();
            }else {
                return trueType.baseType();
            }


        }
    }





}

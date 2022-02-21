package work.liziyun.comp;



import work.liziyun.code.LzyScope;
import work.liziyun.code.LzySymtab;
import work.liziyun.code.symbol.LzyClassSymbol;
import work.liziyun.code.symbol.LzyPackageSymbol;
import work.liziyun.code.symbol.LzySymbol;
import work.liziyun.code.symbol.LzyTypeSymbol;
import work.liziyun.code.type.LzyClassType;
import work.liziyun.code.type.LzyType;
import work.liziyun.code.LzyClassReader;
import work.liziyun.tag.LzyFlags;
import work.liziyun.tag.LzyKinds;
import work.liziyun.tree.LzyJCCompilationUnit;
import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyTreeInfo;
import work.liziyun.tree.LzyVisitor;
import work.liziyun.tree.state.LzyJCClassDef;
import work.liziyun.util.LzyContext;
import work.liziyun.util.LzyList;
import work.liziyun.util.LzyListBuffer;
import work.liziyun.world.LzyName;
import work.liziyun.world.LzyTable;

import java.util.HashMap;


/**
 * 符号填充核心类
 */
public class LzyEnter extends LzyVisitor {

    private static final LzyContext.Key key = new LzyContext.Key();
    // 读取器
    protected LzyClassReader reader ;

    // 符号表
    LzySymtab syms;
    protected LzyEnv env;

    LzyType result;

    // 已编译
    HashMap<LzyName, LzyClassSymbol> compiled = new HashMap();

    // 顶级环境的虚拟类
    private LzyJCClassDef predefClassDef;

    // 符号 --> 环境变量
    HashMap<LzyClassSymbol, LzyEnv> classEnvs = new HashMap();

    // 符号填充第二阶段核心类:
     LzyMemberEnter phase2;

    // 第一阶阶段和第二阶段的桥梁: 存储ClassSymbol
    LzyListBuffer uncompleted;

     boolean completionEnabled = true;

    LzyTable names;

     // 检查类
    LzyCheck check;

    public LzyEnter(LzyContext LzyContext) {
        LzyContext.put(key,this);
        reader = LzyClassReader.instance(LzyContext);
        syms = LzySymtab.instance(LzyContext);
        names = LzyTable.instance(LzyContext);
        check = LzyCheck.instance(LzyContext);
        phase2 = LzyMemberEnter.instance(LzyContext);
    }

    public static LzyEnter instance(LzyContext LzyContext) {
        LzyEnter attr = (LzyEnter)LzyContext.get(key);
        if (attr == null) {
            attr = new LzyEnter(LzyContext);
        }
        return attr;
    }

    public LzyEnv getEnv(LzyClassSymbol classSymbol){
        return this.classEnvs.get(classSymbol);
    }


    public void main(LzyList<LzyJCCompilationUnit> var1) {
        this.complete(var1, (LzyClassSymbol)null);
    }

    /**
     * 符号填充流程: 第一阶段 和 第二阶段
     * @param LzyList
     * @param classSymbol
     */
    public void complete(LzyList<LzyJCCompilationUnit> LzyList, LzyClassSymbol classSymbol){
        LzyListBuffer oldBuffer = this.uncompleted;
        // 开关: 第二阶段的符号填充
        if (this.completionEnabled){
            this.uncompleted = new LzyListBuffer();
        }
        // 第一阶段
        try {
            this.classEnter(LzyList,null);
            // 开关: 第二阶段的符号填充
            if (this.completionEnabled){
                while (true){
                    // 第二阶段的符号填充
                    while (this.uncompleted.nonEmpty()){
                        // 第一阶段中处理好的ClassSymbol
                        LzyClassSymbol firstClassSymbol = (LzyClassSymbol)this.uncompleted.next();
                        if (classSymbol==null || classSymbol == firstClassSymbol || oldBuffer == null ){
                            // 调用填充器进行处理!
                            firstClassSymbol.complete();
                        }else{ // 当oldBuffer!=null && classSymbol != null && classSmbol != firsrClassSymbol
                            oldBuffer.append(firstClassSymbol);
                        }
                    }

                    // 第二阶段: ---> visitTopLevel(TopLevel var1) --> 处理导包
                    // 针对特殊情况的第二阶段导包处理
                    for (LzyList list2 = LzyList;list2.nonEmpty();list2 = list2.tail){
                        LzyJCCompilationUnit compilationUnit = (LzyJCCompilationUnit)list2.head;
                        if (compilationUnit.starImportScope.elems == null){
                            phase2.memberEnter(compilationUnit,this.jCCompilationUnitEnv(compilationUnit));
                        }
                    }
                    return;
                }
            }
        }finally {
            // 结束: 第一阶段 和 第二阶段 --> 还原缓存
            this.uncompleted = oldBuffer;
        }

    }

    /**
     * 环境创建
     */
    LzyEnv jCCompilationUnitEnv(LzyJCCompilationUnit jcCompilationUnit){
        LzyEnv env = new LzyEnv(jcCompilationUnit,new LzyAttrContext());
        // 当前环境的编译单元节点
        env.toplevel = jcCompilationUnit;
        // 下一个封装类的定义: ClassDef
        env.enclClass = this.predefClassDef;
        // 所有命名导入的作用域
        jcCompilationUnit.namedImportScope = new LzyScope(jcCompilationUnit.packageSymbol);
        // 所有按需导入的范围
        jcCompilationUnit.starImportScope = new LzyScope(jcCompilationUnit.packageSymbol);
        // 共享导入
        ((LzyAttrContext)env.info).scope = jcCompilationUnit.namedImportScope;
        return env;
    }

    // 返回当前节点的作用域
    // 1.编译单元节点 2.类节点
    LzyScope enterScope(LzyEnv env) {
       // return var1.tree.tag == 3 ? ((ClassDef)var1.tree).sym.members_field : ((AttrContext)var1.info).scope;
        // 如果上下文环境Env的管理节点是JCClassDecl,即它不是一个顶层的编译类JCCompilationUnit
        if (env.tree.getTag() == LzyJCTree.CLASSDEF){
            return ((LzyJCClassDef)env.tree).sym.members_field; // ---> 类下成员作用域
        }else{
            return ((LzyAttrContext)env.info).scope; // ---> JcCompilationUnit.namedImportScope ----> 包下成员作用域
        }
    }




    /**
     * 第一编译阶段:
     *      遍历集合: 对每个元素，进行填充
     * @param LzyList
     * @param env
     * @param <T>
     * @return
     */
    <T extends LzyJCTree> LzyList<LzyJCTree> classEnter(LzyList<T> LzyList, LzyEnv env) {
        LzyListBuffer resultList = new LzyListBuffer();

        for(LzyList list2 = LzyList; list2.nonEmpty(); list2 = list2.tail) {
            LzyType rs = this.classEnter((LzyJCTree) list2.head, env);
            if (rs != null)
            resultList.append(rs);
        }

        return resultList.toList();
    }

    /**
     * 访问者设计模式
     * @param tree
     * @param e
     * @return
     */
    LzyType classEnter(LzyJCTree tree, LzyEnv e) {
        LzyEnv var3 = this.env;
        LzyType var5;
        try {
            this.env = e;
            // 传递参数: 访问者
            tree.accept(this);
            return this.result;
        } catch (Exception var9) {
           // var5 = this.chk.completionError(var1.pos, var9);
            var5 = null;
            throw var9;

        } finally {
            this.env = var3;
        }

    }

    // 第一阶段会触发: 填充ClassDecl的成员，但是什么都不会干。因为真正的逻辑在第二件阶段。
    @Override
    public void visitJCTree(LzyJCTree var1) {
        this.result = null;
    }

    /**
     * 编译单元的访问处理:complete
     */
    @Override
    public void visitJCCompilationUnit(LzyJCCompilationUnit compilationUnit){
        // 获取包符号
        if (compilationUnit.pid != null){
            compilationUnit.packageSymbol = this.reader.enterPackage(LzyTreeInfo.fullName(compilationUnit.pid));
        }else{
            compilationUnit.packageSymbol = this.syms.emptyPackage;
        }
        // 包符号填充器 ---> 包符号创建时，指定填充器ClassReader ----> 同包类符号的创建!
        compilationUnit.packageSymbol.complete(); // ---> 调用ClassReader填充逻辑!!!
        // 填充所有的成员 --->JCClassDecl创建类符号 ---> 调用方法visitClassDecl()
        this.classEnter(compilationUnit.defs,this.jCCompilationUnitEnv(compilationUnit));//填充成员时，使用的环境。即this.env ---> 即编译单元的环境
        this.result = null;
    }
    /**
     * 类的访问处理:complete
     */
    @Override
    public void visitJCClassDef(LzyJCClassDef classDecl){
        // 查找父符号
        LzySymbol owner = ((LzyAttrContext)env.info).scope.owner;
        // 当前节点的成员作用域
        LzyScope enclScope = enterScope(env);
        // 当前ClassDecl的符号
        LzyClassSymbol c = null;
        // 如果父是一个包
        if (owner.kind == LzyClassReader.PCK){
            LzyPackageSymbol packageSymbol = (LzyPackageSymbol)owner;
            // 参数一: 当前类Name 参数二: 包符号
            c = reader.enterClass(classDecl.name,packageSymbol);// 通过包符号,创建类符号
            // 1.调用填充器 2.存储到包符号下的成员符号中
            packageSymbol.members().enterIfAbsent(c);
        }else{
            if (owner.kind == LzyClassReader.TYP){// 成员类(类中类)
                // 通过父类符号，创建类符号
                c = reader.enterClass(classDecl.name,(LzyTypeSymbol)owner);

            }else {// 处理本地类: 这部分代码第二阶段才会执行！
                /*c = reader.defineClass(classDecl.name,owner);
                c.flatname = ;*/
                // 这部分代码不做实现！
            }
        }

        // 设置类符号
        classDecl.sym = c;
        // 从编译缓存中获取
        if (this.compiled.get(c.flatname) != null){
            System.out.println("编译错误!ClassSymbol重复!");
        }else{
            // 放入缓存
            this.compiled.put(c.flatname,c);
            // 成员作用域中放入符号
            enclScope.enter(c);
            // 新环境的next是旧环境
            LzyEnv env2 = this.classEnv(classDecl, this.env);
            // 类符号 <---> 环境变量
            this.classEnvs.put(c,env2);
            // 类符号的填充器 ---> 第二阶段的符号填充: 处理类成员
            c.completer = phase2; // 创建ClassSymbol默认填充器ClassReader，这里进行重新指定！！！
            // 修饰符: 1.检查 2.隐式还原
            c.flags_field = check.checkFlags(classDecl.pos,classDecl.flags,c);
            // 指定文件的位置
            c.sourcefile = this.env.toplevel.sourcefile;
            // 当前ClassSymbol的作用域
            c.members_field = new LzyScope(c);
            //
            LzyClassType classType = (LzyClassType)c.type;
            // 处理非静态内部类的情况: 1. 外面不是包 2.不是static修饰
            if (owner.kind != LzyClassReader.PCK && (c.flags_field & LzyFlags.STATIC) == 0){
                // 外部类符号
                LzySymbol owner2 = owner;
                // 向外寻找: 如果父是静态的变量或方法，那么向外寻找
                while ((owner2.kind & (LzyKinds.VAR | LzyKinds.MTH)) != 0 &&
                        (owner2.flags_field & LzyFlags.STATIC) == 0){
                    // 查找外部
                    owner2 = owner2.owner;
                }
                // 设置外部
/*
                注意: 我们不处理内部类的情况!
                if (owner2.kind == Kinds.TYP){
                    classType.setEnclosingType(owner.type);
                }*/
            }
            // 前提: ClassSymbol不是本地类 !c.isLocal() &&
            // uncompleted: 第一阶段 和 第二阶段 的桥梁!
            if ( this.uncompleted != null) {
                this.uncompleted.append(c);
            }
            // 填充所有的成员: 变量，方法，内部类
            // 这个地方实际上: 空实现。真正执行在第二阶段!
            this.classEnter(classDecl.defs,env2); // ---->最终调用visitJCTree() ----> 空实现!
            this.result = c.type;
        }
    }

    public LzyEnv classEnv(LzyJCClassDef classDecl, LzyEnv env) {
        // 创建新环境 env2的下一个next环境是env
        LzyEnv env2 = env.dup(classDecl, ((LzyAttrContext)env.info).dup(new LzyScope(classDecl.sym)));
        // 新环境的JCClassDecl节点
        env2.enclClass = classDecl;
        // 新环境的外部是旧环境
        env2.outer = env;
        // 是否this和super相关
        ((LzyAttrContext)env2.info).isSelfCall = false;
        return env2;
    }


    // 作用域下是否包含这个符号
    public boolean isIncluded(LzySymbol symbol, LzyScope scope) {
        // 遍历作用域: 查找是否有这个符号
        for(LzyScope.Entry entry = scope.lookup(symbol.name); entry.scope == scope; entry = entry.next()) {
            if (entry.sym.kind == symbol.kind ) {
                if (symbol instanceof LzyClassSymbol && entry.sym instanceof LzyClassSymbol){
                    LzyClassSymbol s1 = (LzyClassSymbol) symbol;
                    LzyClassSymbol s2 = (LzyClassSymbol) entry.sym;
                    if (s1.fullname == s2.fullname){
                        return true;
                    }
                }
                if (symbol instanceof LzyPackageSymbol && entry.sym instanceof LzyPackageSymbol){
                    LzyPackageSymbol p1 = (LzyPackageSymbol)symbol;
                    LzyPackageSymbol p2 = (LzyPackageSymbol)entry.sym;
                    if (p1.fullname == p2.fullname){
                        return true;
                    }
                }
                return true;
            }
        }
        return false;
    }


}

package work.liziyun.tree;


import work.liziyun.code.symbol.LzyClassSymbol;
import work.liziyun.code.LzyScope;
import work.liziyun.code.symbol.*;
import work.liziyun.code.type.LzyType;
import work.liziyun.code.type.LzyTypeTags;
import work.liziyun.tag.LzyFlags;
import work.liziyun.tag.LzyKinds;
import work.liziyun.tree.state.*;
import work.liziyun.tree.express.*;
import work.liziyun.util.LzyContext;
import work.liziyun.util.LzyList;
import work.liziyun.util.LzyListBuffer;
import work.liziyun.world.LzyName;
import work.liziyun.world.LzyTable;

public class LzyTreeMaker {
    private static final LzyContext.Key treeMarkerKey = new LzyContext.Key();
    // 树节点的位置
    public int pos = 0;
    // 编译单元
    public LzyJCCompilationUnit topLevel;
    // 表
    private LzyTable table;

    public LzyTreeMaker(LzyJCCompilationUnit toplevel) {
        this.pos = 1025;
        this.topLevel = toplevel;
        this.table = toplevel.sourcefile.lzyTable;
    }

    public LzyTreeMaker at(int p) {
        this.pos = p;
        return this;
    }


    public LzyJCModifiers Modifiers(long var1) {
        return new LzyJCModifiers(var1);
    }

    // 是否是不合格的
    boolean isUnqualifiable(LzySymbol symbol){
        // 条件： 符号有所属，并且不属于方法，也不属于变量
        if ( symbol.owner != null && symbol.owner.kind!= LzyKinds.MTH && symbol.owner.kind != LzyKinds.VAR && symbol.owner.name != this.table.empty ){
            // 当前符号是: 1. 方法    2. 变量
            if (symbol.kind == LzyKinds.TYP && this.topLevel != null){
                // name作用域
                LzyScope.Entry entry = this.topLevel.namedImportScope.lookup(symbol.name);
                //
                if (entry.scope != null){
                    return entry.scope.owner == entry.sym.owner && entry.sym == symbol && entry.next().scope == null;
                }
                //
                entry = this.topLevel.packageSymbol.members().lookup(symbol.name);
                if (entry.scope != null){
                    return entry.scope.owner == entry.sym.owner && entry.sym==symbol && entry.next().scope == null;
                }
                entry = this.topLevel.starImportScope.lookup(symbol.name);
                if (entry.scope != null){
                    return entry.scope.owner == entry.sym.owner && entry.sym == symbol && entry.next().scope == null;
                }
            }
            // 合格
            return false;
        }else{
            // 不合格
            return true;
        }
    }



    public LzyJCApply Apply(LzyJCTree jcTree,LzyList arg){
        LzyJCApply lzyJCApply = new LzyJCApply(jcTree, arg);
        lzyJCApply.pos  = this.pos;
        return lzyJCApply;
    }

    public LzyName paramName(int i){
        return this.table.fromString("x"+i);
    }

    public static LzyTreeMaker instance(LzyContext var0) {
        LzyTreeMaker var1 = (LzyTreeMaker)var0.get(treeMarkerKey);
        if (var1 == null) {
            var1 = new LzyTreeMaker(var0);
        }

        return var1;
    }


    private LzyTreeMaker(LzyContext LzyContext){
        LzyContext.put(treeMarkerKey,this);
        this.pos = 0;
        this.topLevel = null;
        this.table = LzyTable.instance(LzyContext);
    }



    // LzyJC
    public LzyJCErroneous Erroneous() {
        LzyJCErroneous erroneous = new LzyJCErroneous();
        erroneous.pos = this.pos;
        return erroneous;
    }
    //
    public LzyJCSkip Skip() {
        LzyJCSkip skip = new LzyJCSkip();
        skip.pos = this.pos;
        return skip;
    }

    public LzyJCTypeIdent TypeIdent(int var1) {
        LzyJCTypeIdent var2 = new LzyJCTypeIdent(var1);
        var2.pos = this.pos;
        return var2;
    }



    public LzyJCTree.JCExpression Ident(LzyName var1) {
        LzyJCIdent var2 = new LzyJCIdent(var1, (LzySymbol)null);
        var2.pos = this.pos;
        return var2;
    }

    public LzyJCTree.JCExpression Ident(LzySymbol var1) {
        return (new LzyJCIdent(var1.name, var1)).setPos(this.pos).setType(var1.type);
    }

    public LzyJCTree.JCExpression Ident(LzyJCVarDef var1) {
        return this.Ident((LzySymbol)var1.sym);
    }

    public LzyJCBlock Block(long var1, LzyList var3) {
        LzyJCBlock var4 = new LzyJCBlock(var1, var3);
        var4.pos = this.pos;
        return var4;
    }


    public LzyJCBreak Break(LzyName var1) {
        LzyJCBreak var2 = new LzyJCBreak(var1, (LzyJCTree)null);
        var2.pos = this.pos;
        return var2;
    }

    public LzyJCContinue Continue(LzyName var1) {
        LzyJCContinue var2 = new LzyJCContinue(var1, (LzyJCTree)null);
        var2.pos = this.pos;
        return var2;
    }


    public LzyJCExpressionStatement Exec(LzyJCTree.JCExpression var1) {
        LzyJCExpressionStatement var2 = new LzyJCExpressionStatement(var1);
        var2.pos = this.pos;
        return var2;
    }

    public LzyJCExec Exec(LzyJCTree var1) {
        LzyJCExec var2 = new LzyJCExec(var1);
        var2.pos = this.pos;
        return var2;
    }

    public LzyJCImport Import(LzyJCTree var1) {
        LzyJCImport var2 = new LzyJCImport(var1);
        var2.pos = this.pos;
        return var2;
    }

    public LzyJCLiteral Literal(int var1, Object var2) {
        LzyJCLiteral var3 = new LzyJCLiteral(var1, var2);
        var3.pos = this.pos;
        return var3;
    }

    public LzyJCParens Parens(LzyJCTree var1) {
        LzyJCParens var2 = new LzyJCParens(var1);
        var2.pos = this.pos;
        return var2;
    }

    public LzyJCReturn Return(LzyJCTree var1) {
        LzyJCReturn var2 = new LzyJCReturn(var1);
        var2.pos = this.pos;
        return var2;
    }

    public LzyJCThrow Throw(LzyJCTree var1) {
        LzyJCThrow var2 = new LzyJCThrow(var1);
        var2.pos = this.pos;
        return var2;
    }

    public LzyJCTypeArray TypeArray(LzyJCTree var1) {
        LzyJCTypeArray var2 = new LzyJCTypeArray(var1);
        var2.pos = this.pos;
        return var2;
    }

    public LzyJCUnary Unary(int var1, LzyJCTree var2) {
        LzyJCUnary var3 = new LzyJCUnary(var1, var2, (LzySymbol)null);
        var3.pos = this.pos;
        return var3;
    }

    public LzyList Idents(LzyList var1) {
        LzyListBuffer var2 = new LzyListBuffer();
        for(LzyList var3 = var1; var3.nonEmpty(); var3 = var3.tail) {
            var2.append(this.Ident((LzyJCVarDef)var3.head));
        }
        return var2.toList();
    }

    public LzyJCTree QualIdent(LzySymbol var1) {
        return this.isUnqualifiable(var1) ? this.Ident(var1) : this.Select(this.QualIdent(var1.owner), var1);
    }


    public LzyJCTree Type(LzyType type){
        if (type == null){
            return null;
        }else{
            LzyJCTree lzyJCTree;
            switch (type.tag){
                // 基本数据类型
                case LzyTypeTags.BYTE:
                case LzyTypeTags.CHAR:
                case LzyTypeTags.SHORT:
                case LzyTypeTags.INT:
                case LzyTypeTags.LONG:
                case LzyTypeTags.FLOAT:
                case LzyTypeTags.DOUBLE:
                case LzyTypeTags.BOOLEAN:
                case LzyTypeTags.VOID:
                    lzyJCTree = this.TypeIdent(type.tag);
                    break;
                // 类
                case LzyTypeTags.CLASS:
                    // 不处理外部类outer
                    lzyJCTree = this.QualIdent(type.tsym);
                    break;
                // 数组
                case LzyTypeTags.ARRAY:
                    lzyJCTree = this.TypeArray(this.Type(type.elemtype()));
                    break;
                case LzyTypeTags.METHOD:
                case LzyTypeTags.PACKAGE:
                case LzyTypeTags.BOT:
                case LzyTypeTags.NONE:
                default:
                    System.out.println("编译错误: 不是我们期望的符号");
                case LzyTypeTags.ERROR:
                    lzyJCTree = this.TypeIdent(LzyTypeTags.ERROR);
            }
            // 设置树节点： 类型
            return lzyJCTree.setType(type);
        }
    }

    public LzyList Types(LzyList LzyList) {
        LzyListBuffer LzyListBuffer = new LzyListBuffer();
        for(LzyList l = LzyList; l.nonEmpty(); l = l.tail) {
            LzyListBuffer .append(this.Type((LzyType)l.head));
        }
        return LzyListBuffer .toList();
    }

    public LzyJCSelect Select(LzyJCTree var1,LzyName var2) {
        LzyJCSelect var3 = new LzyJCSelect(var1, var2, (LzySymbol)null);
        var3.pos = this.pos;
        return var3;
    }


    public LzyJCTree Select(LzyJCTree lzyJCTree, LzySymbol lzySymbol) {
        return (new LzyJCSelect(lzyJCTree, lzySymbol.name,lzySymbol)).setPos(this.pos).setType(lzySymbol.type);
    }

    public LzyJCSwitch Switch(LzyJCTree var1, LzyList var2) {
        LzyJCSwitch var3 = new LzyJCSwitch(var1, var2);
        var3.pos = this.pos;
        return var3;
    }

    public LzyJCCompilationUnit CompilationUnit(LzyJCTree var1, LzyList var2) {
        LzyJCCompilationUnit var3 = new LzyJCCompilationUnit(var1, var2, (LzyName)null, (LzyPackageSymbol)null, (LzyScope)null, (LzyScope)null);
        var3.pos = this.pos;
        return var3;
    }


    public LzyJCTree Super(LzyType var1, LzyTypeSymbol var2) {
        return this.Ident((LzySymbol)(new LzyVarSymbol(16L, this.table._super, var1, var2)));
    }

    // 赋值运算
    public LzyJCTree.JCStatement Assignment(LzySymbol symbol, LzyJCTree jcTree) {
        return this.Exec(this.Assign(this.Ident(symbol), jcTree).setType(symbol.type));
    }

    // 赋值运算
    public LzyJCAssign Assign(LzyJCTree left, LzyJCTree right) {
        LzyJCAssign var3 = new LzyJCAssign(left, right);
        var3.pos = this.pos;
        return var3;
    }
    // 复合赋值运算
    public LzyJCAssignop Assignop(int var1, LzyJCTree var2, LzyJCTree var3) {
        LzyJCAssignop var4 = new LzyJCAssignop(var1, var2, var3, (LzySymbol)null);
        var4.pos = this.pos;
        return var4;
    }

    // 运算符
    public LzyJCBinary Binary(int opcode, LzyJCTree var2, LzyJCTree var3) {
        LzyJCBinary var4 = new LzyJCBinary(opcode, var2, var3, (LzySymbol)null);
        var4.pos = this.pos;
        return var4;
    }

    // 情况
    public LzyJCCase Case(LzyJCTree var1, LzyList var2) {
        LzyJCCase var3 = new LzyJCCase(var1, var2);
        var3.pos = this.pos;
        return var3;
    }

    //
    public LzyJCCatch Catch(LzyJCVarDef var1, LzyJCTree var2) {
        LzyJCCatch var3 = new LzyJCCatch(var1, var2);
        var3.pos = this.pos;
        return var3;
    }


    public LzyJCDoloop DoLoop(LzyJCTree var1, LzyJCTree var2) {
        LzyJCDoloop var3 = new LzyJCDoloop(var1, var2);
        var3.pos = this.pos;
        return var3;
    }



    public LzyJCIndexed Indexed(LzyJCTree var1, LzyJCTree var2) {
        LzyJCIndexed var3 = new LzyJCIndexed(var1, var2);
        var3.pos = this.pos;
        return var3;
    }


    // 方法
    public LzyJCMethodDef MethodDef(LzyMethodSymbol var1, LzyJCBlock var2) {
        return this.MethodDef(var1, var1.type, var2);
    }
    // 变量
    public LzyJCVarDef VarDef(LzyVarSymbol varSymbol,LzyJCTree lzyJCTree){
        LzyJCVarDef lzyJCVarDef = new LzyJCVarDef(varSymbol.flags(), varSymbol.name, this.Type(varSymbol.type), lzyJCTree, varSymbol);
        return (LzyJCVarDef)(lzyJCVarDef).setPos(this.pos).setType(varSymbol.type);
    }
    public LzyJCVarDef VarDef(long flags, LzyName name, LzyJCTree left, LzyJCTree right) {
        LzyJCVarDef varDef = new LzyJCVarDef(flags, name, left, right, (LzyVarSymbol)null);
        varDef.pos = this.pos;
        return varDef;
    }

    public LzyJCVarDef Param(LzyName name,LzyType lzyType,LzySymbol symbol){
        LzyVarSymbol varSymbol = new LzyVarSymbol(LzyFlags.EMPTY,name,lzyType,symbol);
        return this.VarDef(varSymbol,null);
    }

    public LzyList Params(LzyList LzyList,LzySymbol symbol){
        LzyListBuffer LzyListBuffer = new LzyListBuffer();
        int i = 0 ;
        LzyList l = LzyList;
        while (l.nonEmpty()){
            //
            LzyListBuffer.append(this.Param(this.paramName(i++),(LzyType) l.head,symbol));
            l = l.tail;
        }
        return LzyListBuffer.toList();
    }


    public LzyJCMethodDef MethodDef(LzyMethodSymbol methodSymbol, LzyType type, LzyJCBlock block) {
        return (LzyJCMethodDef)(new LzyJCMethodDef(methodSymbol.flags(), methodSymbol.name, this.Type(type.restype()), LzyJCTypeParameter.emptyList, this.Params(type.argtypes(), methodSymbol), this.Types(type.thrown()), block, methodSymbol)).setPos(this.pos).setType(type);
    }

    //
    public LzyJCTypeCast TypeCast(LzyJCTree var1, LzyJCTree var2) {
        LzyJCTypeCast var3 = new LzyJCTypeCast(var1, var2);
        var3.pos = this.pos;
        return var3;
    }

    public LzyJCTypeTest TypeTest(LzyJCTree var1, LzyJCTree var2) {
        LzyJCTypeTest var3 = new LzyJCTypeTest(var1, var2);
        var3.pos = this.pos;
        return var3;
    }

    public LzyJCWhileLoop WhileLoop(LzyJCTree cond, LzyJCTree body) {
        LzyJCWhileLoop whileLoop = new LzyJCWhileLoop(cond, body);
        whileLoop.pos = this.pos;
        return whileLoop;
    }

    public LzyJCConditional Conditional(LzyJCTree cond, LzyJCTree truePart, LzyJCTree falsePart) {
        LzyJCConditional conditional = new LzyJCConditional(cond,truePart, falsePart);
        conditional.pos = this.pos;
        return conditional;
    }

    public LzyJCIf If(LzyJCTree cond, LzyJCTree thenPart, LzyJCTree elsePart) {
        LzyJCIf lzyJCIf = new LzyJCIf(cond,thenPart,elsePart);
        lzyJCIf.pos = this.pos;
        return lzyJCIf;
    }
/*    public LzyJCMethodDef MethodDef(LzyMethodSymbol symbol, LzyType type, LzyJCBlock block) {
        return (LzyJCMethodDef)(new LzyJCMethodDef(symbol.flags(), symbol.name, this.Type(type.restype()), LzyTypeParameter.emptyList, this.Params(type.argtypes(), symbol), this.Types(type.thrown()), block, symbol)).setPos(this.pos).setType(type);
    }*/

    public LzyJCNewArray NewArray(LzyJCTree var1, LzyList var2, LzyList var3) {
        LzyJCNewArray var4 = new LzyJCNewArray(var1, var2, var3);
        var4.pos = this.pos;
        return var4;
    }


    public LzyJCTry Try(LzyJCTree body, LzyList<LzyJCCatch> catches, LzyJCBlock finalizer) {
        LzyJCTry var4 = new LzyJCTry( body, catches, finalizer);
        var4.pos = this.pos;
        return var4;
    }
    public LzyJCForLoop ForLoop(LzyList init, LzyJCTree.JCExpression cond, LzyList<LzyJCExpressionStatement> step, LzyJCTree.JCStatement body) {
        LzyJCForLoop forLoop = new LzyJCForLoop(init, cond, step,body);
        forLoop.pos = this.pos;
        return forLoop;
    }

    public LzyJCNewClass NewClass(LzyJCTree encl, LzyJCTree clazz, LzyList args, LzyJCClassDef def) {
        LzyJCNewClass newClass = new LzyJCNewClass(encl, clazz ,args, def, (LzySymbol)null);
        newClass.pos = this.pos;
        return newClass;
    }


    public LzyJCClassDef ClassDef(long flags, LzyName name, LzyList typarams, LzyJCTree extending, LzyList implementing, LzyList defs) {
        LzyJCClassDef var8 = new LzyJCClassDef(flags, name, typarams, extending, implementing, defs, (LzyClassSymbol)null);
        var8.pos = this.pos;
        return var8;
    }


    public LzyJCMethodDef MethodDef(long flags, LzyName name, LzyJCTree resType, LzyList typarams, LzyList params, LzyList thrown, LzyJCBlock block) {
        LzyJCMethodDef methodDef = new LzyJCMethodDef(flags, name, resType, typarams, params, thrown, block, (LzyMethodSymbol)null);
        methodDef.pos = this.pos;
        return methodDef;
    }

}

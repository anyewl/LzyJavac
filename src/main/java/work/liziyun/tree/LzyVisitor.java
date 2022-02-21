package work.liziyun.tree;


import work.liziyun.tree.express.*;
import work.liziyun.tree.state.*;


public class LzyVisitor {
    public LzyVisitor(){}

    public void visitModifiers(LzyJCModifiers modifers){
        this.visitModifiers(modifers);
    }

    public void visitJCCompilationUnit(LzyJCCompilationUnit lzyJCTopLevel){
        this.visitJCTree(lzyJCTopLevel);
    }


    public void visitJCWhileLoop(LzyJCWhileLoop jcWhileLoop){
        this.visitJCTree(jcWhileLoop);
    }


    public void visitJCUnary(LzyJCUnary jcUnary){
        this.visitJCTree(jcUnary);
    }

    public void visitJCTypeIdent(LzyJCTypeIdent jcTypeIdent){
        this.visitJCTree(jcTypeIdent);
    }

    public void visitJCTypeTest(LzyJCTypeTest jcTypeTest){
        this.visitJCTree(jcTypeTest);
    }

    public void visitJCTypeCast(LzyJCTypeCast jcTypeCast){
        this.visitJCTree(jcTypeCast);
    }

    public void visitJCTypeArray(LzyJCTypeArray jcTypeArray){
        this.visitJCTree(jcTypeArray);
    }

    public void visitJCTry(LzyJCTry jctry){
        this.visitJCTree(jctry);
    }



    public void visitJCThrow(LzyJCThrow jcThrow){
        this.visitJCTree(jcThrow);
    }
    public void visitJCSwitch(LzyJCSwitch jcSwitch){
        this.visitJCTree(jcSwitch);
    }

    public void visitJCSkip(LzyJCSkip jcSkip){
        this.visitJCTree(jcSkip);
    }

    public void visitJCSelect(LzyJCSelect jcSelect){
        this.visitJCTree(jcSelect);
    }
    public void visitJCReturn(LzyJCReturn jcReturn){
        this.visitJCTree(jcReturn);
    }

    public void visitJCParens(LzyJCParens jcParens){
        this.visitJCTree(jcParens);
    }


    public void visitJCNewArray(LzyJCNewArray jcNewArray){
        this.visitJCTree(jcNewArray);
    }


    public void visitJCNewClass(LzyJCNewClass jcNewClass){
        this.visitJCTree(jcNewClass);
    }
    public void visitJCMethodDef(LzyJCMethodDef jcMethodDef){
        this.visitJCTree(jcMethodDef);
    }

    public void visitJCLiteral(LzyJCLiteral jcLiteral){
        this.visitJCTree(jcLiteral);
    }
    public void visitJCIndexed(LzyJCIndexed jcIndexed){
        this.visitJCTree(jcIndexed);
    }

    public void visitJCImport(LzyJCImport jcImport){
        this.visitJCTree(jcImport);
    }

    public void visitJCIf(LzyJCIf jcIf){
        this.visitJCTree(jcIf);
    }

    public void visitJCForLoop(LzyJCForLoop jcForLoop){
        this.visitJCTree(jcForLoop);
    }

    public void visitJCIdent(LzyJCIdent jcIdent) {
        this.visitJCTree(jcIdent);
    }

    public void visitJCExec(LzyJCExec jcExec){
        this.visitJCTree(jcExec);
    }
    public void visitJCExec(LzyJCExpressionStatement jcExpressionStatement){
        this.visitJCTree(jcExpressionStatement);
    }

    public void visitJCErroneous(LzyJCErroneous jcErroneous){
        this.visitJCTree(jcErroneous);
    }

    public void visitJCDoloop(LzyJCDoloop jcDoloop){
        this.visitJCTree(jcDoloop);
    }

    public void visitJCContinue(LzyJCContinue jcContinue){
        this.visitJCTree(jcContinue);
    }
    public void visitJCConditional(LzyJCConditional jcConditional){
        this.visitJCTree(jcConditional);
    }
    public void visitJCClassDef(LzyJCClassDef jcClassDef){
        this.visitJCTree(jcClassDef);
    }

    public void visitJCVarDef(LzyJCVarDef jcVarDef){
        this.visitJCTree(jcVarDef);
    }

    public void visitJCCatch(LzyJCCatch jcCatch){
        this.visitJCTree(jcCatch);
    }

    public void visitJCCase(LzyJCCase jcCase){
        this.visitJCTree(jcCase);
    }

    public void visitJCBreak(LzyJCBreak jcBreak){
        this.visitJCTree(jcBreak);
    }

    public void visitJCBlock(LzyJCBlock jcBlock){
        this.visitJCTree(jcBlock);
    }

    public void visitJCBinary(LzyJCBinary jcBinary){
        this.visitJCTree(jcBinary);
    }

    public void visitJCAssignop(LzyJCAssignop lzyAssignop){
        this.visitJCTree(lzyAssignop);
    }

    public void visitJCApply(LzyJCApply apply){
        this.visitJCTree(apply);
    }

    public void visitJCTree(LzyJCTree jcTree){
        System.out.println("不能访问visitTree");
    }
    public void visitJCAssign(LzyJCAssign lzyAssign){
        this.visitJCTree(lzyAssign);
    }

}

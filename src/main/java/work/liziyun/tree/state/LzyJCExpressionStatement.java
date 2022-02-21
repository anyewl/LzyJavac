package work.liziyun.tree.state;

import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;

public class LzyJCExpressionStatement extends LzyJCTree.JCStatement {

    public LzyJCTree.JCExpression expr;

    public LzyJCExpressionStatement(LzyJCTree.JCExpression expr) {
        super(LzyJCTree.EXEC);
        this.expr  = expr;
    }

    @Override
    public void accept(LzyVisitor var1) {
        var1.visitJCExec(this);
    }

    public int getTag(){
        return tag;
    }



}

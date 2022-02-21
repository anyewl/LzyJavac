package work.liziyun.tree.express;

import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;

public class LzyJCParens extends LzyJCTree.JCExpression {
    public LzyJCTree expr;
    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCParens(this);
    }

    public LzyJCParens(LzyJCTree expr) {
        super(PARENS);
        this.expr = expr;
    }

    public int getTag(){
        return tag;
    }
}

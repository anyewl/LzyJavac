package work.liziyun.tree.express;

import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;

public class LzyJCConditional extends LzyJCTree.JCExpression {
    public LzyJCTree cond;
    public LzyJCTree truepart;
    public LzyJCTree falsepart;

    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCConditional(this);
    }

    public LzyJCConditional(LzyJCTree cond, LzyJCTree truepart, LzyJCTree falsepart) {
        super(CONDEXPR);
        this.cond = cond;
        this.truepart = truepart;
        this.falsepart = falsepart;
    }

    public int getTag(){
        return tag;
    }
}

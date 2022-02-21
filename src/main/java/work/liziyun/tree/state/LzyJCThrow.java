package work.liziyun.tree.state;

import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;

public class LzyJCThrow extends LzyJCTree.JCStatement {
    public LzyJCTree expr;
    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCThrow(this);
    }

    public LzyJCThrow(LzyJCTree expr) {
        super(THROW);
        this.expr = expr;
    }
    public int getTag(){
        return tag;
    }
}

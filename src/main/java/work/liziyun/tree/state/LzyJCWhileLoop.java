package work.liziyun.tree.state;

import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;

public class LzyJCWhileLoop extends LzyJCTree.JCStatement {
    public LzyJCTree cond;
    public LzyJCTree body;

    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCWhileLoop(this);
    }

    public LzyJCWhileLoop(LzyJCTree cond, LzyJCTree body) {
        super(WHILELOOP);
        this.cond = cond;
        this.body = body;
    }

    public int getTag(){
        return tag;
    }
}

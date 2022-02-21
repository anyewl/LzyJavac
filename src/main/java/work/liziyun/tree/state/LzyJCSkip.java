package work.liziyun.tree.state;

import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;

public class LzyJCSkip extends LzyJCTree.JCStatement {
    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCSkip(this);
    }

    public LzyJCSkip() {
        super(SKIP);
    }

    public int getTag(){
        return tag;
    }
}

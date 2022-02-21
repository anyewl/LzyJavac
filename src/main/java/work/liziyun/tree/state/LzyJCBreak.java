package work.liziyun.tree.state;

import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;
import work.liziyun.world.LzyName;

public class LzyJCBreak extends LzyJCTree.JCStatement {

    public LzyName label;
    public LzyJCTree target;

    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCBreak(this);
    }

    public LzyJCBreak(LzyName label, LzyJCTree target) {
        super(BREAK);
        this.label = label;
        this.target = target;
    }

    public int getTag(){
        return tag;
    }
}

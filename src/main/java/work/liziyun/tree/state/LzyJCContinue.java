package work.liziyun.tree.state;

import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;
import work.liziyun.world.LzyName;

public class LzyJCContinue extends LzyJCTree.JCStatement {
    public LzyName label;
    public LzyJCTree target;

    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCContinue(this);
    }

    public LzyJCContinue(LzyName label, LzyJCTree target) {
        super(CONTINUE);
        this.label = label;
        this.target = target;
    }

    public int getTag(){
        return tag;
    }
}

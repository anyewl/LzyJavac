package work.liziyun.tree.state;


import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;
import work.liziyun.util.LzyList;

public class LzyJCBlock extends LzyJCTree.JCStatement {
    public long flags;
    public LzyList<JCStatement> stats;
    public int endpos = 0;
    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCBlock(this);
    }

    public LzyJCBlock(long flags, LzyList stats) {
        super(BLOCK);
        this.flags = flags;
        this.stats = stats;
    }

    public int getTag(){
        return tag;
    }
}

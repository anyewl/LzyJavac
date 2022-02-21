package work.liziyun.tree.state;



import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;
import work.liziyun.util.LzyList;

public class LzyJCCase extends LzyJCTree.JCStatement {
    public LzyJCTree pat;
    public LzyList stats;

    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCCase(this);
    }

    public LzyJCCase(LzyJCTree pat, LzyList stats) {
        super(CASE);
        this.pat = pat;
        this.stats = stats;
    }
    public int getTag(){
        return tag;
    }
}

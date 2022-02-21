package work.liziyun.tree.express;



import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;
import work.liziyun.util.LzyList;

public class LzyJCApply extends LzyJCTree.JCExpression {

    public LzyJCTree meth;
    public LzyList args;

    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCApply(this);
    }

    public LzyJCApply(LzyJCTree meth, LzyList args) {
        super(APPLY);
        this.meth = meth;
        this.args = args;
    }

    public int getTag(){
        return tag;
    }
}

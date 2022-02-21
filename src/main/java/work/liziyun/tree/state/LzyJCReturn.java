package work.liziyun.tree.state;


import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;

public class LzyJCReturn extends LzyJCTree.JCStatement {
    public LzyJCTree expr;
    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCReturn(this);
    }

    public LzyJCReturn(LzyJCTree expr) {
        super(RETURN);
        this.expr = expr;
    }
    public int getTag(){
        return tag;
    }
}

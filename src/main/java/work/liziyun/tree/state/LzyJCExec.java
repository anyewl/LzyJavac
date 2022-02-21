package work.liziyun.tree.state;

import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;

public class LzyJCExec extends LzyJCTree.JCStatement {
    public LzyJCTree expr;
    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCExec(this);
    }

    public LzyJCExec(LzyJCTree expr) {
        super(EXEC);
        this.expr = expr;
    }

    public int getTag(){
        return tag;
    }
}

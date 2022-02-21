package work.liziyun.tree.state;


import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;
import work.liziyun.util.LzyList;

public class LzyJCForLoop extends LzyJCTree.JCStatement {

    public LzyList init;
    public LzyJCTree.JCExpression cond;
    public LzyList<LzyJCExpressionStatement> step;
    public LzyJCTree.JCStatement body;

    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCForLoop(this);
    }

    public LzyJCForLoop(LzyList init, LzyJCTree.JCExpression cond, LzyList<LzyJCExpressionStatement> step, LzyJCTree.JCStatement body) {
        super(FORLOOP);
        this.init = init;
        this.cond = cond;
        this.step = step;
        this.body = body;
    }

    public int getTag(){
        return tag;
    }
}

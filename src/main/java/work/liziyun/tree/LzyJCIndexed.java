package work.liziyun.tree;

public class LzyJCIndexed extends LzyJCTree.JCExpression {
    public LzyJCTree indexed;
    public LzyJCTree index;
    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCIndexed(this);
    }

    public LzyJCIndexed(LzyJCTree indexed, LzyJCTree index) {
        super(INDEXED);
        this.indexed = indexed;
        this.index = index;
    }


    public int getTag(){
        return tag;
    }
}

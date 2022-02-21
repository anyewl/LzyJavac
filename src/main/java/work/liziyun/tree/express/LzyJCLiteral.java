package work.liziyun.tree.express;


import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;

public class LzyJCLiteral extends LzyJCTree.JCExpression {

    public int typetag;

    public Object value;

    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCLiteral(this);
    }

    public LzyJCLiteral(int typetag, Object value) {
        super(LITERAL);
        this.typetag = typetag;
        this.value = value;
    }

    public int getTag(){
        return tag;
    }
}

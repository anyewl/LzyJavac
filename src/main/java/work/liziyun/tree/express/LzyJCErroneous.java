package work.liziyun.tree.express;


import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;

public class LzyJCErroneous extends LzyJCTree.JCExpression {

    public LzyJCErroneous() {
        super(ERRONEOUS);
    }

    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCErroneous(this);
    }


    public int getTag(){
        return tag;
    }
}

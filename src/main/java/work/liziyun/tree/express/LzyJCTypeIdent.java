package work.liziyun.tree.express;

import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;

public class LzyJCTypeIdent extends LzyJCTree.JCExpression {
    public int typetag;

    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCTypeIdent(this);
    }

    public LzyJCTypeIdent(int typetag) {
        super(TYPEIDENT);
        this.typetag = typetag;
    }

    public int getTag(){
        return tag;
    }
}

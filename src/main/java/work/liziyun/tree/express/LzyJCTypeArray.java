package work.liziyun.tree.express;

import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;

public class LzyJCTypeArray extends LzyJCTree.JCExpression {
    public LzyJCTree elemtype;
    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCTypeArray(this);
    }


    public LzyJCTypeArray(LzyJCTree elemtype) {
        super(TYPEARRAY);
        this.elemtype = elemtype;
    }

    public int getTag(){
        return tag;
    }
}

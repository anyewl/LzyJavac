package work.liziyun.tree.express;


import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;
import work.liziyun.util.LzyList;

public class LzyJCNewArray extends LzyJCTree.JCExpression {

    public LzyJCTree elemtype;
    public LzyList dims;
    public LzyList elems;

    public void accept(LzyVisitor visitor) {
        visitor.visitJCNewArray(this); }

    public LzyJCNewArray(LzyJCTree elemtype, LzyList dims, LzyList elems) {
        super(NEWARRAY);
        this.elemtype = elemtype;
        this.dims = dims;
        this.elems = elems;
    }

    public int getTag(){
        return tag;
    }
}

package work.liziyun.tree.state;


import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;
import work.liziyun.util.LzyList;

public class LzyJCTry extends LzyJCTree.JCStatement {

    public LzyJCTree body;
    public LzyList catchers;
    public LzyJCBlock finalizer;

    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCTry(this);
    }

    public LzyJCTry(LzyJCTree body,LzyList catchers, LzyJCBlock finalizer) {
        super(TRY);
        this.body = body;
        this.catchers = catchers;
        this.finalizer = finalizer;
    }
    public int getTag(){
        return tag;
    }
}

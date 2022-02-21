package work.liziyun.tree.state;

import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;

public class LzyJCDoloop extends LzyJCTree.JCStatement {
    public LzyJCTree body;
    public LzyJCTree cond;
    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCDoloop(this);
    }

    public LzyJCDoloop(LzyJCTree body, LzyJCTree cond) {
        super(DOLOOP);
        this.body = body;
        this.cond = cond;
    }

    public int getTag(){
        return tag;
    }
}

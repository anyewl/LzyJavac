package work.liziyun.tree.state;

import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;

public class LzyJCIf extends LzyJCTree.JCStatement {
    public LzyJCTree cond;
    public LzyJCTree thenpart;
    public LzyJCTree elsepart;
    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCIf(this);
    }

    public LzyJCIf(LzyJCTree cond, LzyJCTree thenpart, LzyJCTree elsepart) {
        super(IF);
        this.cond = cond;
        this.thenpart = thenpart;
        this.elsepart = elsepart;
    }

    public int getTag(){
        return tag;
    }
}

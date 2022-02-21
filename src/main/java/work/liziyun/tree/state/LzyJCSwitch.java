package work.liziyun.tree.state;


import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;
import work.liziyun.util.LzyList;

public class LzyJCSwitch extends LzyJCTree.JCStatement {

    public LzyJCTree selector;
    public LzyList cases;

    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCSwitch(this);
    }

    public LzyJCSwitch(LzyJCTree selector, LzyList cases) {
        super(SWITCH);
        this.selector = selector;
        this.cases = cases;
    }

    public int getTag(){
        return tag;
    }
}

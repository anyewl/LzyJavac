package work.liziyun.tree.state;


import work.liziyun.tree.LzyJCTree;

import work.liziyun.tree.LzyVisitor;
import work.liziyun.util.LzyList;

public class LzyJCCatch extends LzyJCTree.JCStatement {
    public LzyJCVarDef param;
    public LzyJCTree body;
    public static LzyList emptyList = LzyList.nil();
    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCCatch(this);
    }

    public LzyJCCatch(LzyJCVarDef param, LzyJCTree body) {
        super(CATCH);
        this.param = param;
        this.body = body;
    }

    public int getTag(){
        return tag;
    }
}

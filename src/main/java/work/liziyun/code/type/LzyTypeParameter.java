package work.liziyun.code.type;


import work.liziyun.tree.LzyJCTree;
import work.liziyun.util.LzyList;

public class LzyTypeParameter extends LzyJCTree {
    public static final LzyList emptyList = LzyList.nil();

    public LzyTypeParameter() {
        super(TYPEPARAMETER);
    }
}

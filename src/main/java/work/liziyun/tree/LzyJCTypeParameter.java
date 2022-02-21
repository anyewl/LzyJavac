package work.liziyun.tree;


import work.liziyun.util.LzyList;

// 泛型
public class LzyJCTypeParameter extends LzyJCTree{
    public static final LzyList emptyList = LzyList.nil();

    public LzyJCTypeParameter() {
        // 疑问这里为什么是0，而不是TYPEPARAMETER
        super(TYPEPARAMETER);
    }

    public int getTag(){
        return tag;
    }
}

package work.liziyun.tree;



public class LzyJCModifiers extends LzyJCTree{
    public long flags;

    public LzyJCModifiers(long flags) {
        super(LzyJCTree.MODIFIERS);
        this.flags = flags;
    }
    @Override
    public void accept(LzyVisitor var1) {
        var1.visitModifiers(this);
    }

    public int getTag(){
        return tag;
    }


}

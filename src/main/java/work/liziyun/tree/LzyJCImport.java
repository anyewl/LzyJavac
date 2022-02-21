package work.liziyun.tree;


public class LzyJCImport extends LzyJCTree {
    public LzyJCTree qualid;
    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCImport(this);
    }

    public LzyJCImport(LzyJCTree qualid) {
        super(IMPORT);
        this.qualid = qualid;
    }

    public int getTag(){
        return tag;
    }
}

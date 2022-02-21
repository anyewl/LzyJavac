package work.liziyun.tree;


import work.liziyun.code.symbol.LzyMethodSymbol;
import work.liziyun.tree.state.LzyJCBlock;
import work.liziyun.util.LzyList;
import work.liziyun.world.LzyName;

public class LzyJCMethodDef extends LzyJCTree {
    public long flags;
    public LzyName name;
    public LzyJCTree restype;
    // 内置泛型
    public LzyList typarams;
    public LzyList params;
    public LzyList thrown;
    public LzyJCBlock block;
    public LzyMethodSymbol sym;
    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCMethodDef(this);
    }

    public LzyJCMethodDef(long flags, LzyName name, LzyJCTree restype, LzyList typarams, LzyList params, LzyList thrown, LzyJCBlock block, LzyMethodSymbol sym) {
        super(METHODDEF);
        this.flags = flags;
        this.name = name;
        this.restype = restype;
        this.typarams = typarams;
        this.params = params;
        this.thrown = thrown;
        this.block = block;
        this.sym = sym;
    }

    public int getTag(){
        return tag;
    }
}

package work.liziyun.tree.state;


import work.liziyun.code.symbol.LzyVarSymbol;
import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;
import work.liziyun.util.LzyList;
import work.liziyun.world.LzyName;

public class LzyJCVarDef extends LzyJCTree.JCStatement {
    public long flags;
    public LzyName name;
    public LzyJCTree vartype;
    public LzyJCTree init;
    public LzyVarSymbol sym;
    public static final LzyList emptyList = LzyList.nil();
    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCVarDef(this);
    }

    public LzyJCVarDef(long flags, LzyName name, LzyJCTree vartype, LzyJCTree init, LzyVarSymbol sym) {
        super(VARDEF);
        this.flags = flags;
        this.name = name;
        this.vartype = vartype;
        this.init = init;
        this.sym = sym;
    }

    public int getTag(){
        return tag;
    }
}

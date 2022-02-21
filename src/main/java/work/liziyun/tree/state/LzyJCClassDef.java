package work.liziyun.tree.state;

import work.liziyun.code.symbol.LzyClassSymbol;
import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;
import work.liziyun.util.LzyList;
import work.liziyun.world.LzyName;

public class LzyJCClassDef extends LzyJCTree.JCStatement {
    public long flags;
    public LzyName name;
    // 内置泛型
    public LzyList typarams;
    public LzyJCTree extending;
    public LzyList<LzyJCTree.JCExpression> implementing;
    public LzyList<LzyJCTree> defs;
    public LzyClassSymbol sym;
    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCClassDef(this);
    }

    public LzyJCClassDef(long flags, LzyName name, LzyList typarams, LzyJCTree extending, LzyList implementing, LzyList defs, LzyClassSymbol sym) {
        super(CLASSDEF);
        this.flags = flags;
        this.name = name;
        this.typarams = typarams;
        this.extending = extending;
        this.implementing = implementing;
        this.defs = defs;
        this.sym = sym;
    }

    public int getTag(){
        return tag;
    }
}

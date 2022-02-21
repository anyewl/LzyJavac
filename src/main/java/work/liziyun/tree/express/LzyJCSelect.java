package work.liziyun.tree.express;


import work.liziyun.code.symbol.LzySymbol;
import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;
import work.liziyun.world.LzyName;

public class LzyJCSelect extends LzyJCTree.JCExpression {
    public LzyJCTree selected;
    public LzyName name;
    public LzySymbol sym;
    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCSelect(this);
    }

    public LzyJCSelect(LzyJCTree selected, LzyName name, LzySymbol sym) {
        super(SELECT);
        this.selected = selected;
        this.name = name;
        this.sym = sym;
    }

    public int getTag(){
        return tag;
    }
}

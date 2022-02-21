package work.liziyun.tree.express;

import work.liziyun.code.symbol.LzySymbol;
import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;
import work.liziyun.world.LzyName;

public class LzyJCIdent  extends LzyJCTree.JCExpression {
    public LzyName name;
    public LzySymbol symbol;

    @Override
    public void accept(LzyVisitor visitor) {
            visitor.visitJCIdent(this);
    }

    public LzyJCIdent(LzyName name, LzySymbol symbol) {
        super(IDENT);
        this.name = name;
        this.symbol = symbol;
    }

    public int getTag(){
        return tag;
    }
}

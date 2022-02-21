package work.liziyun.tree.express;

import work.liziyun.code.symbol.LzySymbol;
import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;

public class LzyJCUnary extends LzyJCTree.JCExpression{
    public LzyJCTree arg;
    public LzySymbol operator;

    public void accept(LzyVisitor var1) {
        var1.visitJCUnary(this);
    }


    public LzyJCUnary(int tag,LzyJCTree arg, LzySymbol operator) {
        super(tag);
        this.arg = arg;
        this.operator = operator;
    }
    public int getTag(){
        return tag;
    }
}

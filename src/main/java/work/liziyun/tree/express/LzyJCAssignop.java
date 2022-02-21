package work.liziyun.tree.express;

import work.liziyun.code.symbol.LzySymbol;
import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;

/**
 * 作者: 李滋芸
 * 语法树节点: 复合赋值节点
 *
 */
public class LzyJCAssignop extends LzyJCTree.JCExpression {
    public LzyJCTree lhs;
    public LzyJCTree rhs;
    public LzySymbol operator;
    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCAssignop(this);
    }

    public LzyJCAssignop(int tag, LzyJCTree lhs, LzyJCTree rhs, LzySymbol operator) {
        super(tag);
        this.lhs = lhs;
        this.rhs = rhs;
        this.operator = operator;
    }

    public int getTag(){
        return tag;
    }
}

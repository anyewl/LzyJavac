package work.liziyun.tree.express;



import work.liziyun.code.symbol.LzySymbol;
import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;

/**
 * 作者: 李滋芸
 * 语法树节点: 算术运算
 *
 */
public class LzyJCBinary extends LzyJCTree.JCExpression {
    public int opcode;
    public LzyJCTree lhs;
    public LzyJCTree rhs;
    public LzySymbol operator;
    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCBinary(this);
    }

    public LzyJCBinary(int opcode, LzyJCTree lhs, LzyJCTree rhs, LzySymbol operator) {
        this.opcode = opcode;
        this.lhs = lhs;
        this.rhs = rhs;
        this.operator = operator;
    }

    public int getTag(){
        return tag;
    }
}

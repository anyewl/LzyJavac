package work.liziyun.tree.express;

import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;

/**
 * 作者: 李滋芸
 * 语法树节点: 普通赋值节点
 *
 */
public class LzyJCAssign extends LzyJCTree.JCExpression{

    public LzyJCTree lhs;
    public LzyJCTree rhs;

    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCAssign(this);
    }

    public LzyJCAssign( LzyJCTree lhs, LzyJCTree rhs) {
        super(ASSIGN);
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public int getTag(){
        return tag;
    }

}

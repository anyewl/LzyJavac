package work.liziyun.tree.express;

import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;
// instanceOf
public class LzyJCTypeTest extends LzyJCTree.JCExpression {
    public LzyJCTree expr;
    public LzyJCTree clazz;


    public void accept(LzyVisitor var1) {
        var1.visitJCTypeTest(this);
    }

    public LzyJCTypeTest(LzyJCTree expr, LzyJCTree clazz) {
        super(TYPETEST);
        this.expr = expr;
        this.clazz = clazz;
    }

    public int getTag(){
        return tag;
    }
}

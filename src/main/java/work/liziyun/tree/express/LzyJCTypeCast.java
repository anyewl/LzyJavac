package work.liziyun.tree.express;


import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;

public class LzyJCTypeCast extends LzyJCTree.JCExpression {

    public LzyJCTree clazz;
    public LzyJCTree expr;


    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCTypeCast(this);
    }

    public LzyJCTypeCast(LzyJCTree clazz, LzyJCTree expr) {
        super(TYPECAST);
        this.clazz = clazz;
        this.expr = expr;
    }

    public int getTag(){
        return tag;
    }
}

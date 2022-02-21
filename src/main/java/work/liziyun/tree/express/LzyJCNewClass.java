package work.liziyun.tree.express;


import work.liziyun.code.symbol.LzySymbol;
import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyVisitor;
import work.liziyun.tree.state.LzyJCClassDef;
import work.liziyun.util.LzyList;

public class LzyJCNewClass extends LzyJCTree.JCExpression {
    public LzyJCTree encl;
    public LzyJCTree clazz;
    public LzyList args;
    public LzyJCClassDef def;
    public LzySymbol constructor;
    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCNewClass(this);
    }

    public LzyJCNewClass(LzyJCTree encl, LzyJCTree clazz, LzyList args, LzyJCClassDef def, LzySymbol constructor) {
        super(NEWCLASS);
        this.encl = encl;
        this.clazz = clazz;
        this.args = args;
        this.def = def;
        this.constructor = constructor;
    }

    public int getTag(){
        return tag;
    }
}

package work.liziyun.tree;


import work.liziyun.code.LzyScope;
import work.liziyun.code.symbol.LzyPackageSymbol;
import work.liziyun.util.LzyList;
import work.liziyun.world.LzyName;

public class LzyJCCompilationUnit extends LzyJCTree {
    public LzyJCTree pid;
    public LzyList defs;
    public LzyName sourcefile;
    public LzyPackageSymbol packageSymbol;
    // 普通导入的作用域
    public LzyScope namedImportScope;
    // 星号导入的作用域
    public LzyScope starImportScope;


    public LzyJCCompilationUnit(LzyJCTree pid, LzyList defs, LzyName name, LzyPackageSymbol lzyPackageSymbol, LzyScope namedImportScope, LzyScope starImportScope) {
        super(TOPLEVEL);
        this.pid = pid;
        this.defs = defs;
        this.sourcefile = name;
        this.packageSymbol = lzyPackageSymbol;
        this.namedImportScope = namedImportScope;
        this.starImportScope = starImportScope;
    }

    @Override
    public void accept(LzyVisitor visitor) {
        visitor.visitJCCompilationUnit(this);
    }


    public int getTag(){
        return tag;
    }
}

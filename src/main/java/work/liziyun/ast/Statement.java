package work.liziyun.ast;

import work.liziyun.tree.LzyJCTree;
import work.liziyun.util.LzyList;

public interface Statement {
    LzyList<LzyJCTree.JCStatement> blockStatements();
    LzyJCTree.JCStatement statement();
}

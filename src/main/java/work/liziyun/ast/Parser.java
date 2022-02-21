package work.liziyun.ast;


import work.liziyun.tree.LzyJCCompilationUnit;
import work.liziyun.tree.LzyJCTree;

public interface Parser {
    LzyJCCompilationUnit parseCompilationUnit();

    LzyJCTree.JCExpression parseExpression();

    LzyJCTree.JCStatement parseStatement();

    LzyJCTree.JCExpression parseType();
}

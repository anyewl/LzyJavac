package work.liziyun.ast;


import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyTreeInfo;
import work.liziyun.world.LzyToken;

public interface Express {
    /**
     * 赋值表达式
     * @return
     */
   public LzyJCTree.JCExpression term();
   public LzyJCTree.JCExpression term(int mode);
   public LzyJCTree.JCExpression termRest(LzyJCTree.JCExpression var1);


    /**
     * 三元
     * @return
     */
    public LzyJCTree.JCExpression term1();
   public LzyJCTree.JCExpression term1Rest(LzyJCTree.JCExpression tree);


    /**
     * 二元
     * @return
     */
    public LzyJCTree.JCExpression term2();
    public LzyJCTree.JCExpression term2Rest(LzyJCTree.JCExpression tree,int op);


    /**
     * 一元 与 基本表达式
     * @return
     */
    public LzyJCTree.JCExpression term3();

    static int prec(LzyToken var0) {
        int var1 = optag(var0);
        return var1 >= 0 ? LzyTreeInfo.opPrec(var1) : -1;
    }

    /**
     * 基本数据类型的映射值
     * @param var0
     * @return
     */
    static int typetag(LzyToken var0) {
        switch(var0) {
            case BYTE:
                return 1;
            case SHORT:
                return 3;
            case CHAR:
                return 2;
            case INT:
                return 4;
            case LONG:
                return 5;
            case FLOAT:
                return 6;
            case DOUBLE:
                return 7;
            case BOOLEAN:
                return 8;
            default:
                return -1;
        }
    }



    /**
     * 辅助TreeMark的创建: 一元运算符 映射值
     * @param var0
     * @return
     */
    static int unoptag(LzyToken var0) {
        switch(var0) {
            case BANG:
                return 50;
            case TILDE:
                return 51;
            case LPAREN:
            case THIS:
            case SUPER:
            case NEW:
            case LBRACKET:
            case DOT:
            case QUES:
            default:
                return -1;
            case PLUSPLUS:
                return 52;
            case SUBSUB:
                return 53;
            case PLUS:
                return 48;
            case SUB:
                return 49;
        }
    }

    static int optag(LzyToken var0) {
        switch(var0) {
            case LT:
                return 64;
            case BYTE:
            case SHORT:
            case CHAR:
            case INT:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case BOOLEAN:
            case VOID:
            case IDENTIFIER:
            case CASE:
            case DEFAULT:
            case IF:
            case FOR:
            case WHILE:
            case DO:
            case TRY:
            case SWITCH:
            case RETURN:
            case THROW:
            case BREAK:
            case CONTINUE:
            case ELSE:
            case FINALLY:
            case CATCH:
            case INTLITERAL:
            case LONGLITERAL:
            case FLOATLITERAL:
            case DOUBLELITERAL:
            case CHARLITERAL:
            case STRINGLITERAL:
            case TRUE:
            case FALSE:
            case NULL:
            case EQ:
            case BANG:
            case TILDE:
            case LPAREN:
            case THIS:
            case SUPER:
            case NEW:
            case LBRACKET:
            case DOT:
            case QUES:
            case PLUSPLUS:
            case SUBSUB:
            case ERROR:
            default:
                return -1;
            case PLUSEQ:
                return 88;
            case SUBEQ:
                return 89;
            case STAREQ:
                return 90;
            case SLASHEQ:
                return 91;
            case PERCENTEQ:
                return 92;
            case AMPEQ:
                return 78;
            case BAREQ:
                return 76;
            case CARETEQ:
                return 77;
            case LTLTEQ:
                return 85;
            case GTGTEQ:
                return 86;
            case GTGTGTEQ:
                return 87;
            case PLUS:
                return 71;
            case SUB:
                return 72;
            case GTEQ:
                return 67;
            case GTGTGT:
                return 70;
            case GTGT:
                return 69;
            case GT:
                return 65;
            case BARBAR:
                return 57;
            case AMPAMP:
                return 58;
            case BAR:
                return 59;
            case CARET:
                return 60;
            case AMP:
                return 61;
            case EQEQ:
                return 62;
            case BANGEQ:
                return 63;
            case LTEQ:
                return 66;
            case LTLT:
                return 68;
            case STAR:
                return 73;
            case SLASH:
                return 74;
            case PERCENT:
                return 75;
            case INSTANCEOF:
                return 32;
        }
    }

}

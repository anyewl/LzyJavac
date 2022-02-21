package work.liziyun.tree;


import work.liziyun.code.symbol.LzySymbol;
import work.liziyun.code.type.LzyTypeTags;
import work.liziyun.tag.LzyFlags;
import work.liziyun.tree.express.*;
import work.liziyun.tree.state.LzyJCExec;
import work.liziyun.tree.state.LzyJCExpressionStatement;
import work.liziyun.util.LzyContext;
import work.liziyun.util.LzyList;
import work.liziyun.world.LzyName;
import work.liziyun.world.LzyTable;

public class LzyTreeInfo {
    protected static final LzyContext.Key<LzyTreeInfo> treeInfoKey = new LzyContext.Key();
    // 运算符Name
    private LzyName[] opname = new LzyName[28];
    // 修饰符的名称数组
    private static final String[] flagName =  new String[]{"public", "private", "protected", "static", "final", "synchronized", "volatile", "transient", "native", "interface", "abstract", "strictfp"};;
    public static final int notExpression = -1;
    public static final int noPrec = 0;
    public static final int assignPrec = 1;
    public static final int assignopPrec = 2;
    public static final int condPrec = 3;
    public static final int orPrec = 4;
    public static final int andPrec = 5;
    public static final int bitorPrec = 6;
    public static final int bitxorPrec = 7;
    public static final int bitandPrec = 8;
    public static final int eqPrec = 9;
    public static final int ordPrec = 10;
    public static final int shiftPrec = 11;
    public static final int addPrec = 12;
    public static final int mulPrec = 13;
    public static final int prefixPrec = 14;
    public static final int postfixPrec = 15;
    public static final int precCount = 16;

    public static LzyTreeInfo instance(LzyContext LzyContext){
        LzyTreeInfo lzyTreeInfo = LzyContext.get(treeInfoKey);
        if(lzyTreeInfo == null){
            lzyTreeInfo = new LzyTreeInfo(LzyContext);
        }
        return lzyTreeInfo;
    }


    public LzyTreeInfo(LzyContext LzyContext) {
        LzyContext.put(treeInfoKey,this);
        LzyTable lzyTable = LzyTable.instance(LzyContext);
        this.opname[0] = lzyTable.fromString("+");
        this.opname[1] = lzyTable.hyphen;
        this.opname[2] = lzyTable.fromString("!");
        this.opname[3] = lzyTable.fromString("~");
        this.opname[4] = lzyTable.fromString("++");
        this.opname[5] = lzyTable.fromString("--");
        this.opname[6] = lzyTable.fromString("++");
        this.opname[7] = lzyTable.fromString("--");
        this.opname[8] = lzyTable.fromString("<*nullchk*>");
        this.opname[9] = lzyTable.fromString("||");
        this.opname[10] = lzyTable.fromString("&&");
        this.opname[14] = lzyTable.fromString("==");
        this.opname[15] = lzyTable.fromString("!=");
        this.opname[16] = lzyTable.fromString("<");
        this.opname[17] = lzyTable.fromString(">");
        this.opname[18] = lzyTable.fromString("<=");
        this.opname[19] = lzyTable.fromString(">=");
        this.opname[11] = lzyTable.fromString("|");
        this.opname[12] = lzyTable.fromString("^");
        this.opname[13] = lzyTable.fromString("&");
        this.opname[20] = lzyTable.fromString("<<");
        this.opname[21] = lzyTable.fromString(">>");
        this.opname[22] = lzyTable.fromString(">>>");
        this.opname[23] = lzyTable.fromString("+");
        this.opname[24] = lzyTable.hyphen;
        this.opname[25] = lzyTable.asterisk;
        this.opname[26] = lzyTable.slash;
        this.opname[27] = lzyTable.fromString("%");
    }

    public static int opPrec(int opCode){
        switch ( opCode ){
            case LzyJCTree.ASSIGN:
                return assignPrec;
            case LzyJCTree.TYPECAST:
            case LzyJCTree.INDEXED:
            case LzyJCTree.SELECT:
            case LzyJCTree.IDENT:
            case LzyJCTree.LITERAL:
            case LzyJCTree.TYPEIDENT:
            case LzyJCTree.TYPEARRAY:
            case LzyJCTree.TYPEAPPLY:
            case LzyJCTree.TYPEUNION:
            case LzyJCTree.TYPEPARAMETER:
            case LzyJCTree.WILDCARD:
            case LzyJCTree.TYPEBOUNDKIND:
            case LzyJCTree.ANNOTATION:
            case LzyJCTree.MODIFIERS:
            case LzyJCTree.ANNOTATED_TYPE:
            case LzyJCTree.ERRONEOUS:
            default:
                throw new AssertionError();
            case LzyJCTree.TYPETEST:
                return ordPrec;
            case LzyJCTree.POS:
            case LzyJCTree.NEG:
            case LzyJCTree.NOT:
            case LzyJCTree.COMPL:
            case LzyJCTree.PREINC:
            case LzyJCTree.PREDEC:
                return prefixPrec;
            case LzyJCTree.POSTINC:
            case LzyJCTree.POSTDEC:
            case LzyJCTree.NULLCHK:
                return postfixPrec;
            case LzyJCTree.OR:
                return orPrec;
            case LzyJCTree.AND:
                return andPrec;
            case LzyJCTree.BITOR:
                return bitorPrec;
            case LzyJCTree.BITXOR:
                return bitxorPrec;
            case LzyJCTree.BITAND:
                return bitandPrec;
            case LzyJCTree.EQ:
            case LzyJCTree.NE:
                return eqPrec;
            case LzyJCTree.LT:
            case LzyJCTree.GT:
            case LzyJCTree.LE:
            case LzyJCTree.GE:
                return ordPrec;
            case LzyJCTree.SL:
            case LzyJCTree.SR:
            case LzyJCTree.USR:
                return shiftPrec;
            case LzyJCTree.PLUS:
            case LzyJCTree.MINUS:
                return addPrec;
            case LzyJCTree.MUL:
            case LzyJCTree.DIV:
            case LzyJCTree.MOD:
                return mulPrec;
            case LzyJCTree.BITOR_ASG:
            case LzyJCTree.BITXOR_ASG:
            case LzyJCTree.BITAND_ASG:
            case LzyJCTree.SL_ASG:
            case LzyJCTree.SR_ASG:
            case LzyJCTree.USR_ASG:
            case LzyJCTree.PLUS_ASG:
            case LzyJCTree.MUL_ASG:
            case LzyJCTree.DIV_ASG:
            case LzyJCTree.MOD_ASG:
                return assignopPrec;

        }
    }


    /**
     * 检查是否是空指针标签: null
     * @param jcTree
     * @return
     */
    public static boolean isNull(LzyJCTree jcTree){
        if ( jcTree.getTag() != LzyJCTree.LITERAL ){
            return false;
        }
        LzyJCLiteral lit = (LzyJCLiteral)jcTree;
        // 判断空指针类型
        return  (lit.typetag == LzyTypeTags.BOT);
    }


    public LzyName operatorName(int tokenIndex){
        return this.opname[tokenIndex-48];
    }

    public static LzyName name(LzyJCTree tree){
        switch (tree.tag){
            case LzyJCTree.SELECT:
                return ((LzyJCSelect)tree).name;
            case LzyJCTree.IDENT:
                return ((LzyJCIdent)tree).name;
            default:
                return null;
        }
    }


    public static LzyJCTree skipParens(LzyJCTree jcTree){
        // 获取括号中的表达式
        while (jcTree.tag == LzyJCTree.PARENS){
            jcTree = ((LzyJCParens)jcTree).expr;
        }
        return jcTree;
    }

    public static void setSymbol(LzyJCTree tree, LzySymbol symbol){
        tree = skipParens(tree);
        switch (tree.tag){
            case LzyJCTree.SELECT:
                ((LzyJCSelect)tree).sym = symbol;
                break;
            case LzyJCTree.IDENT:
                ((LzyJCIdent)tree).symbol = symbol;
                break;
        }
    }

    public static LzySymbol symbol(LzyJCTree tree){
        tree = skipParens(tree);
        switch (tree.tag){
            case LzyJCTree.SELECT:
                return ((LzyJCSelect)tree).sym;
            case LzyJCTree.IDENT:
                return ((LzyJCIdent)tree).symbol;
            default:
                return null;
        }
    }


    public static LzyName calledMethodName(LzyJCTree tree){
        if (tree.tag == LzyJCTree.EXEC){
            LzyJCExpressionStatement exec = (LzyJCExpressionStatement)tree;
            if (exec.expr.tag == LzyJCTree.APPLY){
                LzyName name = name(((LzyJCApply) exec.expr).meth);
                return name;
            }
        }
        return null;
    }




    // 是否是this方法或super方法
    public static boolean isSelfCall(LzyJCTree tree){
        // 调用节点的方法名称
        LzyName name = calledMethodName(tree);
        if (name == null){
            return false;
        }else{
            LzyTable table = name.lzyTable;
            return name == table._this || name == table._super;
        }
    }


    public static LzyName fullName(LzyJCTree lzyJCTree){
        lzyJCTree = skipParens(lzyJCTree);
        switch (lzyJCTree.tag){
            case LzyJCTree.SELECT:
                LzyName name = fullName(((LzyJCSelect) lzyJCTree).selected);
                return name == null ? null : name.append('.',name(lzyJCTree));
            case LzyJCTree.IDENT:
                return ((LzyJCIdent)lzyJCTree).name;
            default:
                return null;
        }
    }

    public static boolean isConstructor(LzyJCTree tree){
        // 方法节点
        if (tree.tag == LzyJCTree.METHODDEF){
            LzyName name = ((LzyJCMethodDef)tree).name;
            // 是否初始化方法
            return name == name.lzyTable.init;
        }else{
            return false;
        }
    }


    /**
     * 调用表达式: 如果是对this()调用，那么返回true
     * @param jcTree
     * @return
     */
    public static boolean isSyntheticInit(LzyJCTree jcTree){
        if ( jcTree.tag == LzyJCTree.EXEC ){
            LzyJCTree.JCExpression expr= ((LzyJCExpressionStatement)jcTree).expr;
            if ( expr.tag == LzyJCTree.ASSIGN ){
                // 赋值语句
                LzyJCAssign assign  = (LzyJCAssign)expr;
                if (assign.lhs.getTag() == LzyJCTree.SELECT){
                   LzyJCSelect jcSelect =  (LzyJCSelect)assign.lhs;
                   if (
                           jcSelect.sym != null && ( jcSelect.sym.flags() & LzyFlags.SYNTHETIC) != 0
                   ){
                       LzyName selected = name(jcSelect);
                       if ( selected != null && selected == selected.lzyTable._this ){
                           return true;
                       }
                   }
                }
            }
        }
        return false;
    }


    public static LzyJCApply firstConstructorCall(LzyJCTree jcTree){
        // 如果不是一个方法
        if ( jcTree.tag != LzyJCTree.METHODDEF){
            return null;
        }else{
            LzyJCMethodDef methodDef = (LzyJCMethodDef)jcTree;
            LzyTable lzyTable = methodDef.name.lzyTable;
            // 方法名不是init初始化方法
            if ( methodDef.name != lzyTable.init ){
                return null;
            }else if ( methodDef.block == null ){
                return null;
            }else{
                // 语句块
                LzyList<LzyJCTree.JCStatement> staments;
                for(staments = ((LzyJCMethodDef) jcTree).block.stats; staments.nonEmpty() && isSyntheticInit((LzyJCTree)staments.head); staments = staments.tail) {
                }
                if (staments.isEmpty()) {
                    return null;
                } else if (((LzyJCTree)staments.head).tag != LzyJCTree.EXEC) {
                    return null;
                } else {
                    LzyJCExpressionStatement var4 = (LzyJCExpressionStatement)staments.head;
                    return var4.expr.tag != LzyJCTree.APPLY ? null : (LzyJCApply)var4.expr;
                }
            }
        }
    }


    /**
     * 对于this()构造方法调用的判断
     * @param jcTree
     * @return
     */
    public static boolean isInitialConstructor(LzyJCTree jcTree){
        LzyJCApply lzyJCApply = firstConstructorCall(jcTree);
        if ( lzyJCApply == null ){
            return false;
        }else {
            LzyName name = name(lzyJCApply.meth);
            return name == null || name != name.lzyTable._this;
        }
    }

    public static boolean hasConstructors(LzyList<LzyJCTree> trees){
        LzyList<LzyJCTree> LzyList = trees;
        while ( LzyList.nonEmpty() ){
            if ( isConstructor(LzyList.head) ){
                return true;
            }
            LzyList = LzyList.tail;
        }
        return false;
    }





}

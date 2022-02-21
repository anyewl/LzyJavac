package work.liziyun.tree;


import work.liziyun.code.type.LzyType;

public abstract class LzyJCTree {
    public static final int TOPLEVEL = 1;
    public static final int IMPORT = 2;
    public static final int CLASSDEF = 3;
    public static final int METHODDEF = 4;
    public static final int VARDEF = 5;
    public static final int SKIP = 6;
    public static final int BLOCK = 7;
    public static final int DOLOOP = 8;
    public static final int WHILELOOP = 9;
    public static final int FORLOOP = 10;
    public static final int FOREACHLOOP = 11;
    public static final int LABELLED = 12;
    public static final int SWITCH = 13;
    public static final int CASE = 14;
    public static final int SYNCHRONIZED = 15;
    public static final int TRY = 16;
    public static final int CATCH = 17;
    public static final int CONDEXPR = 18;
    public static final int IF = 19;
    public static final int EXEC = 20;
    public static final int BREAK = 21;
    public static final int CONTINUE = 22;
    public static final int RETURN = 23;
    public static final int THROW = 24;
    public static final int ASSERT = 25;
    public static final int APPLY = 26;
    public static final int NEWCLASS = 27;
    public static final int NEWARRAY = 28;
    public static final int PARENS = 29;
    public static final int ASSIGN = 30;
    public static final int TYPECAST = 31;
    public static final int TYPETEST = 32;
    public static final int INDEXED = 33;
    public static final int SELECT = 34;
    public static final int IDENT = 35;
    public static final int LITERAL = 36;
    public static final int TYPEIDENT = 37;
    public static final int TYPEARRAY = 38;
    public static final int TYPEAPPLY = 39;
    public static final int TYPEUNION = 40;
    public static final int TYPEPARAMETER = 41;
    public static final int WILDCARD = 42;
    public static final int TYPEBOUNDKIND = 43;
    public static final int ANNOTATION = 44;
    public static final int MODIFIERS = 45;
    public static final int ANNOTATED_TYPE = 46;
    public static final int ERRONEOUS = 47;
    public static final int POS = 48;       //  +
    public static final int NEG = 49;       //  -
    public static final int NOT = 50;       //  !
    public static final int COMPL = 51;     //  ~
    public static final int PREINC = 52;    //  ++ _
    public static final int PREDEC = 53;    //  -- _
    public static final int POSTINC = 54;   //  _ ++
    public static final int POSTDEC = 55;   //  _ --
    public static final int NULLCHK = 56;
    public static final int OR = 57;        // ||
    public static final int AND = 58;       // &&
    public static final int BITOR = 59;     // |
    public static final int BITXOR = 60;    // ^
    public static final int BITAND = 61;    // &
    public static final int EQ = 62;        // ==
    public static final int NE = 63;        // !=
    public static final int LT = 64;        // <
    public static final int GT = 65;        // >
    public static final int LE = 66;        // <=
    public static final int GE = 67;        // >=
    public static final int SL = 68;        // <<
    public static final int SR = 69;        // >>
    public static final int USR = 70;       // >>>
    public static final int PLUS = 71;      // +
    public static final int MINUS = 72;     // -
    public static final int MUL = 73;       // *
    public static final int DIV = 74;       // /
    public static final int MOD = 75;       // %
    public static final int BITOR_ASG = 76;  // |=
    public static final int BITXOR_ASG = 77; // ^=
    public static final int BITAND_ASG = 78; // &=
    public static final int SL_ASG = 85;     // <<=
    public static final int SR_ASG = 86;     // >>=
    public static final int USR_ASG = 87;    // >>>=
    public static final int PLUS_ASG = 88;   // +=
    public static final int MINUS_ASG = 89;  // -=
    public static final int MUL_ASG = 90;    // *=
    public static final int DIV_ASG = 91;    // /=
    public static final int MOD_ASG = 92;    // %=
    public static final int LETEXPR = 93;
    public static final int ASGOffset = 17;
    public int pos;
    public LzyType type;
    public int tag;

    public LzyJCTree(int tag) {
        this.tag = tag;
    }

    public LzyJCTree() {
    }

    public int getTag(){
        return tag;
    }
    //
    public LzyJCTree setPos(int pos){
        this.pos = pos;
        return this;
    }
    //
    public LzyJCTree setType(LzyType type){
        this.type = type;
        return this;
    }
    // 访问者
    public void accept(LzyVisitor visitor){
        visitor.visitJCTree(this);
    }


    public static class JCExpression extends LzyJCTree {

        public JCExpression () {
            super();
        }

        public JCExpression (int tag) {
            super(tag);
        }

        public JCExpression setType(LzyType var1) {
            super.setType(var1);
            return this;
        }

        public JCExpression setPos(int var1) {
            super.setPos(var1);
            return this;
        }
    }

    public static class JCStatement extends LzyJCTree{
        public JCStatement(int tag) {
            super(tag);
        }


        public JCStatement setType(LzyType var1) {
            super.setType(var1);
            return this;
        }

        public JCStatement setPos(int var1) {
            super.setPos(var1);
            return this;
        }
    }

}

package work.liziyun.world;

/**
 * 作者: 李滋芸
 *      Java敏感词
 */
public enum LzyToken {
    EOF,    // 结束
    ERROR,  // 错误
    IDENTIFIER, // 标识符
    ABSTRACT("abstract"),   // 抽象
    BOOLEAN("boolean"),     // 布尔
    BREAK("break"),
    BYTE("byte"),
    CASE("case"),
    CHAR("char"),
    CLASS("class"),
    CONTINUE("continue"),
    CATCH("catch"),
    DEFAULT("default"),
    DO("do"),
    DOUBLE("double"),
    ELSE("else"),
    EXTENDS("extends"),
    SYNCHRONIZED("synchronized"),
    FINAL("final"),
    FLOAT("float"),
    FINALLY("finally"),
    FOR("for"),
    IF("if"),
    IMPLEMENTS("implements"),
    IMPORT("import"),
    INSTANCEOF("instanceof"),
    INT("int"),
    INTERFACE("interface"),
    LONG("long"),
    NEW("new"),
    PACKAGE("package"),
    PRIVATE("private"),
    PROTECTED("protected"),
    PUBLIC("public"),
    RETURN("return"),
    SHORT("short"),
    STATIC("static"),
    SUPER("super"),
    SWITCH("switch"),
    THIS("this"),
    TRY("try"),
    THROW("throw"),
    VOID("void"),
    WHILE("while"),
    INTLITERAL,
    LONGLITERAL,
    FLOATLITERAL,
    DOUBLELITERAL,
    CHARLITERAL,
    STRINGLITERAL,
    TRUE("true"),
    FALSE("false"),
    NULL("null"),
    LPAREN("("),
    RPAREN(")"),
    LBRACE("{"),
    RBRACE("}"),
    LBRACKET("["),
    RBRACKET("]"),
    SEMI(";"),
    COMMA(","),
    DOT("."),
    EQ("="),
    GT(">"),
    LT("<"),
    BANG("!"),
    TILDE("~"),
    QUES("?"),
    COLON(":"),
    EQEQ("=="),
    LTEQ("<="),
    GTEQ(">="),
    BANGEQ("!="),
    AMPAMP("&&"),
    BARBAR("||"),
    PLUSPLUS("++"),
    SUBSUB("--"),
    PLUS("+"),
    SUB("-"),
    STAR("*"),
    SLASH("/"),
    AMP("&"),
    BAR("|"),
    CARET("^"),
    PERCENT("%"),
    LTLT("<<"),
    GTGT(">>"),
    GTGTGT(">>>"),
    PLUSEQ("+="),
    SUBEQ("-="),
    STAREQ("*="),
    SLASHEQ("/="),
    AMPEQ("&="),
    BAREQ("|="),
    CARETEQ("^="),
    PERCENTEQ("%="),
    LTLTEQ("<<="),
    GTGTEQ(">>="),
    GTGTGTEQ(">>>="),
    CUSTOM;
    // 关键字的名字
    public final String name;

    LzyToken() {
        this(null);
    }

    LzyToken(String name) {
        this.name = name;
    }
    // 词的类型: Java敏感词
    public String getKind(){
        return "Token(Java敏感词)";
    }
    // Java敏感词转字符串
    public String toString(){
        switch (this){
            // 标识符
            case IDENTIFIER:
                return "token.identifier";
            case CHARLITERAL: // char字面量
                return "token.character";
            case STRINGLITERAL: // 字符串
                return "token.string";
            case INTLITERAL: // int字面量
                return "token.integer";
            case LONGLITERAL: // long字面量
                return "token.long-integer";
            case FLOATLITERAL: // float字面量
                return "token.float";
            case DOUBLELITERAL: // double字面量
                return "token.double";
            case ERROR: // 错误
                return "token.bad-symbol";
            case EOF: // 结束
                return "token.end-of-input";
            case DOT: // 点
            case COMMA: // 逗号
            case SEMI: // 分号
            case LPAREN: // 左小括号
            case RPAREN: // 右小括号
            case LBRACKET: // 左中括号
            case RBRACKET: // 右中括号
            case LBRACE: // 左花括号
            case RBRACE: // 右花括号
                return "'" + this.name + "'";
            default:
                return this.name;
        }
    }
}

package work.liziyun.ast;




import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.express.LzyJCTypeIdent;
import work.liziyun.tree.LzyTreeMaker;
import work.liziyun.tree.state.LzyJCClassDef;
import work.liziyun.util.LzyConvert;
import work.liziyun.util.LzyList;
import work.liziyun.util.LzyListBuffer;
import work.liziyun.world.LzyName;
import work.liziyun.world.LzyTable;
import work.liziyun.world.LzyToken;

import static work.liziyun.world.LzyToken.RBRACKET;


public class LzyExpressParser  implements Express {
    protected LexerBuffer S;
    protected LzyTreeMaker F;
    protected LzyTable names;
    protected static LzyJCTree.JCExpression errorTree ;

    int mode ;
    int lastmode;
    // 表达式
    static final int EXPR = 1;
    // 类型
    static final int TYPE = 2;

    public LzyExpressParser(LexerBuffer s, LzyTreeMaker f, LzyTable names) {
        S = s;
        F = f;
        this.names = names;
        // 错误节点
        errorTree = F.Erroneous();
    }

    // 运算符
    LzyListBuffer<LzyToken[]> opStackSupply = new LzyListBuffer<LzyToken[]>();
    // 表达式
    LzyListBuffer<LzyJCTree.JCExpression[]> odStackSupply = new LzyListBuffer<LzyJCTree.JCExpression[]>();

    public LzyJCTree.JCExpression term() {
        LzyJCTree.JCExpression tree = this.term1();
        /*
            不是赋值运算:
                1. mode不是表达式
                2. 不是'='
                3. 不是其他赋值符号
         */
        if ( (this.mode & 1) == 0 || this.S.token() != LzyToken.EQ && (LzyToken.PLUSEQ.compareTo(this.S.token()) > 0 || this.S.token().compareTo(LzyToken.GTGTGTEQ) > 0) ){
            return tree;
        }else{
            return this.termRest(tree);
        }

    }

    public LzyJCTree.JCExpression term(int mode) {
        // 备份当前mode
        int lastmode = this.mode;
        // 更新mode
        this.mode = mode;
        // 调用
        LzyJCTree.JCExpression expr = this.term();
        // 设置上一个mode
        this.lastmode = mode;
        // 还原mode
        this.mode = lastmode;
        return expr;
    }

    public LzyJCTree.JCExpression termRest(LzyJCTree.JCExpression var1) {
        int var2;
        switch(this.S.token()) {
            case EQ:
                var2 = this.S.pos();
                this.S.nextToken();
                this.mode = 1;
                LzyJCTree.JCExpression var5 = this.term();
                return this.F.at(var2).Assign(var1, var5);
            case PLUSEQ:
            case SUBEQ:
            case STAREQ:
            case SLASHEQ:
            case PERCENTEQ:
            case AMPEQ:
            case BAREQ:
            case CARETEQ:
            case LTLTEQ:
            case GTGTEQ:
            case GTGTGTEQ:
                var2 = this.S.pos();
                LzyToken var3 = this.S.token();
                this.S.nextToken();
                this.mode = 1;
                LzyJCTree.JCExpression var4 = this.term();
                return this.F.at(var2).Assignop(Express.optag(var3), var1, var4);
            default:
                return var1;
        }
    }

    public LzyJCTree.JCExpression term1() {
        LzyJCTree.JCExpression tree = this.term2();
        /**
         * 三元运算
         *      1. mode是表达式
         *      2. 当前词是'?'
         */
        if ((this.mode & 1) != 0 & this.S.token() == LzyToken.QUES) {
            this.mode = 1;
            return this.term1Rest(tree);
        } else {
            return tree;
        }
    }

    public LzyJCTree.JCExpression term1Rest(LzyJCTree.JCExpression tree) {
        // 安全操作: 再次判断'?'
        if (this.S.token() == LzyToken.QUES) {
            int quesPos = this.S.pos();
            this.S.nextToken();
            LzyJCTree.JCExpression leftTree = this.term();
            this.S.accept(LzyToken.COLON);
            LzyJCTree.JCExpression rightTree = this.term1();
            return this.F.at(quesPos).Conditional(tree, leftTree, rightTree);
        } else {
            return tree;
        }
    }

    public LzyJCTree.JCExpression term2() {
        LzyJCTree.JCExpression expression = this.term3();
        /**
         * 1. 如果期待是表达式
         * 2. 运算符优先级大于4(2元运算)
         */
        if ( (this.mode & EXPR) != 0 && Express.prec(this.S.token()) >= 4 ){
            this.mode = 1;
            return this.term2Rest(expression,4);
        }
        return expression;
    }

    /**
     *
     * @param express 运算符前面的表达式
     * @param op 需要处理的优先级
     * @return
     * 案例: boolean flag =  false || true && true;
     * 参数:
     *      express = false; op=4;
     *          odStack[false]
     * 循环： || 大于 4 成立
     *   外层第一次循环:
     *          opStack[ERROR]
     *          odStack[false,true]
     *   外层第二次循环:
     *          opStack[ERROR,||]
     *          odStack[false,true,false]
     *   内存第一次循环:
     *          合并: true && false
     *          合并: false || (true && false)
     *
     * 为什么要使用引用传递的内存空间？
     *      因为一个完整表达式，term2Rest()并不能完全解析。 只能解析括号内的。
     *
     *      下一次解析时，需要使用上一次的解析。
     *
     */
    public LzyJCTree.JCExpression term2Rest(LzyJCTree.JCExpression express,int op) {
        // 上一次的表达式
        LzyList<LzyJCTree.JCExpression[]> saveOd = this.odStackSupply.elems;
        // 当前的表达式
        LzyJCTree.JCExpression[] odStack = this.newOdStack(); // 引用传递: odStack 和 saveOd.head 是一块内存地址
        // 上一次操作符
        LzyList<LzyToken[]> saveOp = this.opStackSupply.elems;
        // 当前操作符
        LzyToken[] opStack = this.newOpStack(); // 引用传递: opStack 和 opStackSupply.elems 是一块内存地址
        // 当前操作符的位置
        int index = 0;
        // 存入表达式
        odStack[0] = express;
        // 当前词的位置
        int pos = this.S.pos();
        // 上一次操作符: 由于第一次，没有上一次。所以用ERROR代表。
        LzyToken preOpToken = LzyToken.ERROR;
        // 当前词的优先级大于方法传递的优先级(一般是4,二元运算)
        while ( Express.prec(this.S.token()) >= op ){
            // 操作符存储，上一个操作符
            opStack[index] = preOpToken;
            // 新增操作符位置
            index++;
            // 上一个操作符：二元运算符
            preOpToken = this.S.token();
            // 移词： 表达式
            this.S.nextToken();
            // 解析二元运算符后面的表达式
            odStack[index] = (preOpToken==LzyToken.INSTANCEOF ? this.term(TYPE) : this.term3() );
            // 上一个词的优先级 > 当前词的优先级
            while ( index > 0 && Express.prec(preOpToken) >= Express.prec(this.S.token()) ){
                // 判断两个操作符优先级: 合并表达式
                odStack[index-1] = this.makeOp(pos,preOpToken,odStack[index-1],odStack[index]);
                // 两个表达式变成一个,所以减一
                index--;
                // 错误opToken
                preOpToken = opStack[index];
            }
        }
        // 注意: 这段代码并不矛盾！！！因为newOdStack()方法会替换掉this.odStackSupply.element的空间！！
        this.odStackSupply.elems = saveOd;
        this.opStackSupply.elems = saveOp;
        // 返回第一个表达式
        return odStack[0];
    }

    public LzyJCTree.JCExpression term3() {
        // 第一个词的位置
        int pos = this.S.pos();
        LzyJCTree.JCExpression expr = null;
        // 泛型
        LzyList<LzyJCTree.JCExpression> typeArgs = null;
        // pos
        int pos2;
        // 括号内部 -->表达式
        LzyJCTree.JCExpression exprBrackets;
        // 括号内部 -->类型(被强制转换的对象)
        LzyJCTree.JCExpression exprBracketsType;
        // 开头
        switch ( this.S.token()){
            case IDENTIFIER:
                expr = this.F.at(this.S.pos()).Ident(this.S.ident());
                laber253:
                    while (true){
                        pos = this.S.pos();
                        // 标识符后下一个词
                        switch (this.S.token()){
                            case LPAREN: // '(' -->调用方法
                                    if ( (this.mode & EXPR) != 0 ){
                                        this.mode = EXPR;
                                        expr = this.arguments( (LzyJCTree.JCExpression) expr);
                                    }
                                    break laber253;
                            case LBRACKET: // '[' -->数组
                                // 移词
                                this.S.nextToken();
                                // ']'
                                if (this.S.token() == RBRACKET ){
                                    this.S.nextToken();
                                    expr = this.bracketsSuffix( this.bracketsOpt( this.F.at(pos).TypeArray(expr)  ) );
                                }else {
                                    if ( (this.mode & EXPR) != 0  ){
                                           this.mode = 1;
                                           expr = this.F.Indexed(expr, this.term()  );
                                    }
                                    this.S.accept(RBRACKET);
                                }
                                break laber253;
                            case DOT: // '.' --> 点
                                this.S.nextToken();
                                // mode是表达式
                                if ( (this.mode&EXPR) != 0 ){
                                    switch (this.S.token()){
                                        case CLASS:
                                            this.mode = EXPR;
                                            expr = this.F.at(pos).Select(expr,this.names._class);
                                            this.S.nextToken();
                                            break laber253;
                                        case THIS:
                                            this.mode = EXPR;
                                            expr = this.F.at(pos).Select(expr,this.names._class);
                                            this.S.nextToken();
                                            break laber253;
                                        case SUPER:
                                            this.mode = EXPR;
                                            expr = this.superSuffix(typeArgs,this.F.at(pos).Select((LzyJCTree.JCExpression)expr, this.names._super));
                                            break laber253;
                                        case NEW: // 内部类语法: new Outer().new Inner();
                                            this.mode = EXPR;
                                            pos2 = this.S.pos();
                                            this.S.nextToken();
                                            expr = this.innerCreator(pos2,typeArgs,expr);
                                            break laber253;
                                    }
                                }
                                // 继续循环: 1. 上一个词是‘.' 2. 当前词不是特殊词(this,super,new)
                                expr = this.F.at(pos).Select((LzyJCTree.JCExpression)expr,this.S.ident());
                                break ;

                            default:
                                break laber253;
                        }
                    } // 退出死循环

                    break;//退出当前case
            case LPAREN: // 第一个词'('
                // 移词
                this.S.nextToken();
                // 括号内部: 1. 表达式 2.类型
                this.mode = 7;
                // 递归括号内部
                exprBrackets = this.termRest(this.term1Rest(this.term2Rest(this.term3(),4)));
                // 词: ')'
                this.S.accept(LzyToken.RPAREN);
                this.lastmode = this.mode;
                this.mode = EXPR;
                // 如果上一个mode: 不是表达式 ---> 即exprBrackets是类型，表示强制转换
                // 基本数据类型转换 ---> lastmode = 2 ---> 例： (int)
                if ( (this.lastmode & 1) == 0 ){
                    // 被强制转换的对象
                    exprBracketsType = this.term3();
                    // 参数一: 括号内部  参数二: 括号右边
                    return this.F.at(pos).TypeCast(exprBrackets,exprBracketsType);
                }
                // 如果上一个mode: 是类型
                // 引用数据类型转换 ---> lastmode = 7  例: (Integer) , (Outer.Inner)
                // 这个判断并不严禁，内部switch更加严密的细节控制！
                if ( (this.lastmode & TYPE) != 0 ){
                    // 引用数据类型的情况比较复杂: 这段代码防止误判！！！
                    switch ( this.S.token() ){
                        // 被转换对象: 允许的开头
                        case LPAREN:
                        case INTLITERAL:
                        case LONGLITERAL:
                        case FLOATLITERAL:
                        case DOUBLELITERAL:
                        case CHARLITERAL:
                        case STRINGLITERAL:
                        case TRUE:
                        case FALSE:
                        case NULL:
                        case BANG:
                        case TILDE:
                        case THIS:
                        case SUPER:
                        case NEW:
                        case IDENTIFIER:
                        case BYTE:
                        case SHORT:
                        case CHAR:
                        case INT:
                        case LONG:
                        case FLOAT:
                        case DOUBLE:
                        case BOOLEAN:
                        case VOID:
                            // 被强制转换的对象
                            exprBracketsType = this.term3();
                            return this.F.at(pos).TypeCast(exprBrackets,exprBracketsType);
                        case SEMI:
                        case RBRACE:
                        case RPAREN:
                        case LBRACE:
                        case EQ:
                        case PLUSEQ:
                        case SUBEQ:
                        case STAREQ:
                        case SLASHEQ:
                        case PERCENTEQ:
                        case AMPEQ:
                        case BAREQ:
                        case CARETEQ:
                        case LTLTEQ:
                        case GTGTEQ:
                        case GTGTGTEQ:
                            // 已经证明是误判！不能当做类型转换来处理！
                    }
                }
                // 二次封装括号内表达式
                expr = this.F.at(pos).Parens(exprBrackets);
                break;
            case INTLITERAL:
            case LONGLITERAL:
            case FLOATLITERAL:
            case DOUBLELITERAL:
            case CHARLITERAL:
            case STRINGLITERAL:
            case TRUE:
            case FALSE:
            case NULL:
                // 常量字面量
                this.mode = EXPR;
                expr = this.literal(this.names.empty);
                break;
            case BANG:
            case TILDE:
            case PLUSPLUS:
            case SUBSUB:
            case PLUS: // 注意: 这里'+'含义，并不是算数运算。而是充当连字符。
            case SUB:// 注意: 这里'-'含义,并不是算数运算。而是充当取反!
                // 一元运算: ++i
                // 注意: i++形式的自增运算，不在这里处理！
                // 当前mode是表达式
                if ( (this.mode & EXPR ) != 0  ){
                    // 一元运算符
                    LzyToken t = this.S.token();
                    // 移词
                    this.S.nextToken();
                    // 表达式
                    this.mode = EXPR;
                    // 1. 不是'-'
                    // 2. 不是'int' 和 不是'long' 的字面量
                    // 3. 不是10进制数
                    // 一元运算符操作的数据
                    if ( t != LzyToken.SUB || this.S.token() != LzyToken.INTLITERAL && this.S.token() != LzyToken.LONGLITERAL || this.S.radix() != 10   ){
                        exprBrackets = this.term3();
                        return this.F.at(pos).Unary(Express.unoptag(t),exprBrackets);
                    }
                    this.mode = EXPR;
                    //
                    expr = this.literal(this.names.hyphen);
                    break;
                }
                return null;
            case THIS:
                this.mode = EXPR;
                // this节点
                LzyJCTree.JCExpression jcIdent = this.F.at(pos).Ident(this.names._this);
                this.S.nextToken();
                // 处理this()构造方法
                // this.的情况这里不处理，交给最后面的死循环中
                expr = this.argumentsOpt(null,jcIdent);
                break;
            case SUPER:
                this.mode = EXPR;
                expr = this.argumentsOpt(null,this.F.at(pos).Ident(this.names._super));
                this.S.nextToken();
                // expr = this.superSuffix(null,this.F.at(pos).Ident(this.names._super));

                break;
            case NEW:
                this.mode = EXPR;
                this.S.nextToken();
                // 构造方法
                expr = this.creator(pos,typeArgs);
                break;
                // 处理基本数据类型
            case BYTE:
            case SHORT:
            case CHAR:
            case INT:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case BOOLEAN:
                // basicType： 建立基本数据类型节点
                // bracketsOpt: 可能是数组
                // bracketsSuffix: 可能.class
                expr = this.bracketsSuffix(this.bracketsOpt(this.basicType()));
                break;
            case VOID:
                this.S.nextToken();
                expr = this.bracketsSuffix(this.F.at(pos).TypeIdent(9));
                break;
        }

        // 死循环
        while (true){
            while (true){
                pos2 = this.S.pos();
                // 当处理的符号'['
                if ( this.S.token() == LzyToken.LBRACKET ) {
                    this.S.nextToken();
                    // if
                    if ( this.S.token() == RBRACKET && (this.mode & TYPE) != 0 ){
                        this.mode = 2;
                        this.S.nextToken();
                        return this.bracketsOpt( this.F.at(pos).TypeArray(expr) );
                    }
                    // if
                    if ( (this.mode&EXPR) != 0 ){
                        this.mode = 1;
                        expr = this.F.at( pos ).Indexed( expr , this.term() );
                    }
                    this.S.accept(RBRACKET);
                }else{
                    // 当符号不是'.'
                    if ( this.S.token() != LzyToken.DOT ){
                        while ( this.S.token() == LzyToken.PLUSPLUS || this.S.token() == LzyToken.SUBSUB && (this.mode & 1) != 0){
                            this.mode = 1;
                            expr = this.F.at(this.S.pos()).Unary(this.S.token() == LzyToken.PLUSPLUS?52:53,expr);
                            this.S.nextToken();
                        }
                        return  expr;
                    }
                    this.S.nextToken();
                    // A a1 = new Teacher().new A();
                    if ( this.S.token() == LzyToken.NEW && (this.mode & EXPR) != 0  ){
                        this.mode = EXPR;
                        pos2 = this.S.pos();
                        this.S.nextToken();
                        expr = this.innerCreator(pos2,typeArgs,expr);
                    }else {
                        expr = this.argumentsOpt(typeArgs,this.F.at(pos).Select(expr,this.S.ident()));
                    }
                }

            }
        }
        // 死循环结束
    }


    LzyJCTree.JCExpression bracketsSuffix(LzyJCTree.JCExpression var1) {
        if ((this.mode & 1) != 0 && this.S.token() == LzyToken.DOT) {
            this.mode = 1;
            int var2 = this.S.pos();
            this.S.nextToken();
            this.S.accept(LzyToken.CLASS);
            var1 = this.F.at(var2).Select((LzyJCTree.JCExpression)var1, this.names._class);
        } else if ((this.mode & 2) != 0) {
            this.mode = 2;
        } else {
            // this.syntaxError(this.S.pos(), "dot.class.expected");
        }

        return (LzyJCTree.JCExpression)var1;
    }


    private LzyJCTree.JCExpression[] newOdStack() {
        // 第一次： elems 和 last 都是null，相等！！
        // 创建全新大小空间
        if (this.odStackSupply.elems == this.odStackSupply.last) {
            // head开辟11个大小空间！
            this.odStackSupply.append(new LzyJCTree.JCExpression[11]);
        }
        // 1.返回刚刚开辟的11个大小空间  2.返回上一次保留下来的
        LzyJCTree.JCExpression[] var1 = (LzyJCTree.JCExpression[])this.odStackSupply.elems.head;
        // 链表下一个
        this.odStackSupply.elems = this.odStackSupply.elems.tail;
        return var1;
    }

    private LzyToken[] newOpStack() {
        if (this.opStackSupply.elems == this.opStackSupply.last) {
            this.opStackSupply.append(new LzyToken[11]);
        }

        LzyToken[] var1 = (LzyToken[])this.opStackSupply.elems.head;
        this.opStackSupply.elems = this.opStackSupply.elems.tail;
        return var1;
    }

    private LzyJCTree.JCExpression makeOp(int var1, LzyToken var2, LzyJCTree.JCExpression var3, LzyJCTree.JCExpression var4) {
        return (LzyJCTree.JCExpression)(var2 == LzyToken.INSTANCEOF ? this.F.at(var1).TypeTest(var3, var4) : this.F.at(var1).Binary(Express.optag(var2), var3, var4));
    }

    LzyJCTree.JCExpression arguments( LzyJCTree.JCExpression methodName) {
        int var3 = this.S.pos();
        LzyList args = this.arguments();
        return this.F.at(var3).Apply( methodName, args);
    }

    LzyList<LzyJCTree.JCExpression> arguments() {
        int var1 = this.S.pos();
        LzyListBuffer var2 = new LzyListBuffer();
        if (this.S.token() == LzyToken.LPAREN) {
            this.S.nextToken();
            if (this.S.token() != LzyToken.RPAREN) {
                var2.append(this.term(EXPR));

                while(this.S.token() == LzyToken.COMMA) {
                    this.S.nextToken();
                    var2.append(this.term(EXPR));
                }
            }

            this.S.accept(LzyToken.RPAREN);
        } else {
            // 1.记录错误位置 2.尝试跳词
            // this.syntaxError(this.S.pos(), "expected", this.keywords.token2string(Tokens.LPAREN));
            System.out.println(this.S.pos() + " expected " + LzyToken.LPAREN.toString() );
        }

        return var2.toList();
    }



    LzyJCTree.JCExpression innerCreator(int pos, LzyList<LzyJCTree.JCExpression> typpeArg, LzyJCTree.JCExpression expr) {
        Object className = this.F.at(this.S.pos()).Ident(this.S.ident());
        return this.classCreatorRest(pos, expr, typpeArg, (LzyJCTree.JCExpression)className);
    }

    LzyJCTree.JCExpression classCreatorRest(int pos, LzyJCTree.JCExpression expr, LzyList<LzyJCTree.JCExpression> typeArg, LzyJCTree.JCExpression className) {
        LzyList arguments = this.arguments();
        return this.F.at(pos).NewClass(expr, className, arguments, null);
    }


    LzyJCTree.JCExpression superSuffix(LzyList<LzyJCTree.JCExpression> typeArg, LzyJCTree.JCExpression expr) {
        this.S.nextToken();
        // 处理‘.’
        if (this.S.token() == LzyToken.LPAREN ) {
            expr = this.arguments( expr);
        } else {// 处理构造方法
            int var3 = this.S.pos();
            this.S.accept(LzyToken.DOT);
            // 由于不处理泛型: 直接传递null
            expr = this.argumentsOpt( null, this.F.at(var3).Select(expr, this.S.ident()));
        }
        return expr;
    }

    LzyJCTree.JCExpression argumentsOpt(LzyList<LzyJCTree.JCExpression> typeArg, LzyJCTree.JCExpression expr) {
        // 1. 期待不是表达式 2.不是'('
        if (((this.mode & 1) == 0 || this.S.token() != LzyToken.LPAREN)) {
            return expr;
        } else {
            this.mode = 1;
            // 处理'(' 方法
            return this.arguments( expr);
        }
    }


    /**
     * 字面量转换: 可能不是十进制，需要进行转换！
     * @param var1
     * @return
     */
    LzyJCTree.JCExpression literal(LzyName var1) {
        int var2 = this.S.pos();
        Object var3 = errorTree;
        String var4;
        switch(this.S.token()) {
            case INTLITERAL:
                try {
                    var3 = this.F.at(var2).Literal(4, LzyConvert.string2int(this.strval(var1), this.S.radix()));
                } catch (NumberFormatException var10) {
                    // this.log.error(this.S.pos(), "int.number.too.large", new Object[]{this.strval(var1)});
                }
                break;
            case LONGLITERAL:
                try {
                    var3 = this.F.at(var2).Literal(5, new Long(LzyConvert.string2long(this.strval(var1), this.S.radix())));
                } catch (NumberFormatException var9) {
                    // this.log.error(this.S.pos(), "int.number.too.large", new Object[]{this.strval(var1)});
                }
                break;
            case FLOATLITERAL:
                var4 = this.S.radix() == 16 ? "0x" + this.S.stringVal() : this.S.stringVal();

                Float var11;
                try {
                    var11 = Float.valueOf(var4);
                } catch (NumberFormatException var8) {
                    // var11 = 0.0F / 0.0;
                    var11 = 0.0F;
                }

                if (var11 == 0.0F && !this.isZero(var4)) {
                    // this.log.error(this.S.pos(), "fp.number.too.small", new Object[0]);
                } else if (var11 == 1.0F / 0.0) {
                    // this.log.error(this.S.pos(), "fp.number.too.large", new Object[0]);
                } else {
                    var3 = this.F.at(var2).Literal(6, var11);
                }
                break;
            case DOUBLELITERAL:
                var4 = this.S.radix() == 16 ? "0x" + this.S.stringVal() : this.S.stringVal();

                Double var5;
                try {
                    var5 = Double.valueOf(var4);
                } catch (NumberFormatException var7) {
                    var5 = 0.0D / 0.0;
                }

                if (var5 == 0.0D && !this.isZero(var4)) {
                    // this.log.error(this.S.pos(), "fp.number.too.small", new Object[0]);
                } else if (var5 == 1.0D / 0.0) {
                    // this.log.error(this.S.pos(), "fp.number.too.large", new Object[0]);
                } else {
                    var3 = this.F.at(var2).Literal(7, var5);
                }
                break;
            case CHARLITERAL:
                var3 = this.F.at(var2).Literal(2, this.S.stringVal().charAt(0) + 0);
                break;
            case STRINGLITERAL:
                var3 = this.F.at(var2).Literal(10, this.S.stringVal());
                break;
            case TRUE:
            case FALSE:
            case NULL:
                var3 = this.F.at(var2).Ident(this.S.name());
                break;
            default:
                assert false;
        }

        this.S.nextToken();
        return (LzyJCTree.JCExpression)var3;
    }

    boolean isZero(String var1) {
        char[] var2 = var1.toCharArray();
        int var3 = Character.toLowerCase(var1.charAt(1)) == 'x' ? 16 : 10;

        int var4;
        for(var4 = var3 == 16 ? 2 : 0; var4 < var2.length && (var2[var4] == '0' || var2[var4] == '.'); ++var4) {
        }

        return var4 >= var2.length || Character.digit(var2[var4], var3) <= 0;
    }

    String strval(LzyName var1) {
        String var2 = this.S.stringVal();
        return var1.length == 0 ? var2 : var1 + var2;
    }


    LzyJCTree.JCExpression basicType() {
        LzyJCTypeIdent var1 = this.F.at(this.S.pos()).TypeIdent(Express.typetag(this.S.token()));
        this.S.nextToken();
        return var1;
    }

    /**
     * 处理new创建对象
     * @param pos
     * @param typeArg
     * @return
     */
    LzyJCTree.JCExpression creator(int pos, LzyList<LzyJCTree.JCExpression> typeArg) {
        switch(this.S.token()) {
            // 创建基本数据类型的数组
            case BYTE:
            case SHORT:
            case CHAR:
            case INT:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case BOOLEAN:
                if (typeArg == null) {
                    return this.arrayCreatorRest(pos, this.basicType());
                }
            default:
                LzyJCTree.JCExpression var3 = this.qualident();
                int var4 = this.mode;
                this.mode = 2;
                this.mode = var4;
                if (this.S.token() == LzyToken.LBRACKET) {
                    return this.arrayCreatorRest(pos, var3);
                } else {
                   // return this.S.token() == Token.LPAREN ? this.classCreatorRest(pos, (LzyJCTree.JCExpression)null, typeArg, var3) : this.syntaxError("left-paren.or.left-square-bracket.expected");
                    return  this.classCreatorRest(pos, (LzyJCTree.JCExpression)null, typeArg, var3);
                }
        }
    }



    LzyJCTree.JCExpression arrayCreatorRest(int var1, LzyJCTree.JCExpression var2) {
        this.S.accept(LzyToken.LBRACKET);
        // 无下标大小的情况[]
        if (this.S.token() == RBRACKET) {
            this.S.accept(RBRACKET);
            var2 = this.bracketsOpt(var2);
            if (this.S.token() == LzyToken.LBRACE) {
                return this.arrayInitializer(var2);
            } else {
                // this.syntaxError(this.S.pos(), "array.dimension.missing");
                return errorTree;
            }
        } else {// 有下标大小的情况[2]
            LzyListBuffer var3 = new LzyListBuffer();
            var3.append(this.term(EXPR));
            this.S.accept(RBRACKET);
            // 处理多维数组
            while(this.S.token() == LzyToken.LBRACKET) {
                int var4 = this.S.pos();
                this.S.nextToken();
                if (this.S.token() == RBRACKET) {
                    var2 = this.bracketsOptCont(var2, var4);
                } else {
                    var3.append(this.term(EXPR));
                    this.S.accept(RBRACKET);
                }
            }
            return this.F.at(var1).NewArray(var2, var3.toList(), (LzyList)null);
        }
    }

    LzyJCTree.JCExpression arrayInitializer(LzyJCTree.JCExpression var1) {
        int var2 = this.S.pos();
        this.S.accept(LzyToken.LBRACE);
        LzyListBuffer var3 = new LzyListBuffer();
        if (this.S.token() == LzyToken.COMMA) {
            this.S.nextToken();
        } else if (this.S.token() != LzyToken.RBRACE) {
            var3.append(this.variableInitializer());
            while(this.S.token() == LzyToken.COMMA) {
                this.S.nextToken();
                if (this.S.token() == LzyToken.RBRACE) {
                    break;
                }
                var3.append(this.variableInitializer());
            }
        }

        this.S.accept(LzyToken.RBRACE);
        return this.F.at(var2).NewArray(var1, LzyList.nil(), var3.toList());
    }

    LzyJCTree.JCExpression variableInitializer() {
        return this.S.token() == LzyToken.LBRACE ? this.arrayInitializer((LzyJCTree.JCExpression)null) : this.term(EXPR);
    }

    /**
     * 辅助处理'['
     * @param var1
     * @return
     */
    public LzyJCTree.JCExpression bracketsOpt(LzyJCTree.JCExpression var1) {
        if (this.S.token() == LzyToken.LBRACKET) {
            int var2 = this.S.pos();
            this.S.nextToken();
            var1 = this.bracketsOptCont(var1, var2);
        }
        return var1;
    }

    /**
     * 辅助处理'['
     * @param var1
     * @param var2
     * @return
     */
    private LzyJCTree.JCExpression bracketsOptCont(LzyJCTree.JCExpression var1, int var2) {
        this.S.accept(RBRACKET);
        var1 = this.bracketsOpt(var1);
        return this.F.at(var2).TypeArray(var1);
    }


    public LzyJCTree.JCExpression qualident() {
        // 解析的抽象语法树节点: 包名
        // LzyJCTree.JCExpression exp1 = this.lzyPos.toP(  this.F.at( this.S.pos() ).Ident( this.S.ident() ) );
        LzyJCTree.JCExpression exp1 =   this.F.at( this.S.pos() ).Ident( this.S.ident() ) ;
        // 下一词是'.'
        while(this.S.token() == LzyToken.DOT) {
            int var3 = this.S.pos();
            this.S.nextToken();
            // 表达式嵌套
            // exp1 = (LzyJCTree.JCExpression)this.lzyPos.toP( this.F.at( var3 ).Select( exp1, this.S.ident() ) );
            exp1 =  this.F.at( var3 ).Select( exp1, this.S.ident() ) ;
        }
        return exp1;
    }


}

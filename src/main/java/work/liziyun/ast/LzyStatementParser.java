package work.liziyun.ast;



import work.liziyun.tree.LzyJCModifiers;
import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyTreeMaker;
import work.liziyun.tree.state.*;
import work.liziyun.util.LzyList;
import work.liziyun.util.LzyListBuffer;
import work.liziyun.world.LzyName;
import work.liziyun.world.LzyTable;
import work.liziyun.world.LzyToken;


public class LzyStatementParser extends LzyExpressParser implements Statement {

    public LzyStatementParser(LexerBuffer s, LzyTreeMaker f, LzyTable names) {
        super(s, f, names);
    }

    @Override
    public LzyList<LzyJCTree.JCStatement> blockStatements() {
        LzyListBuffer LzyListBuffer = new LzyListBuffer();
        while (true){
            int pos = this.S.pos();
            LzyJCTree.JCStatement statement;
            // 一个语句可能出现的开头
            switch (this.S.token()){
                // 处理局部内部类
                case CLASS:
                case INTERFACE:
                    LzyListBuffer.append(this.classOrInterfaceOrEnumDeclaration(this.modifiersOpt(null),null));
                    break;
                    // 常规语句开头:
                    // 1. 变量的声明 --> 开头变量类型  int a = 1;
                    // 2. 变量的赋值运算 --> 开头变量的名称 a = 2;
                    //
                case RPAREN:
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
                case LBRACKET:
                case DOT:
                case QUES:
                case PLUSPLUS:
                case SUBSUB:
                case PLUS:
                case SUB:
                case GTEQ:
                case GTGTGT:
                case GTGT:
                default:
                    LzyName name = this.S.name();
                    // 表达式解析
                    LzyJCTree.JCExpression expr = this.term(EXPR + TYPE);
                    // 条件一: ':'
                    // 条件二: JCTree.JCIdent --> 标识符节点，可能变量，类型，关键字。
                    // 作用: 这里在处理Java中的标签。这个语法并不常用，可以忽略！
                    if ( this.S.token() == LzyToken.COLON && expr.getTag() == LzyJCTree.IDENT ){
                        this.S.nextToken();
                        statement = this.statement();
                        // 创建一个标签Label: 省略对标签的实现!
                       // LzyListBuffer.append(this.F.at(pos).Labelled(name,statement));
                        System.out.println("编译错误: 省略对标签的实现!");
                    }else{
                        // 当做语句处理
                        // 1. 不是类型TYPE
                        // 2. 当前词不是标识符
                        if ( (this.lastmode & 2) == 0 || this.S.token() != LzyToken.IDENTIFIER){
                            LzyListBuffer.append(this.F.at(pos).Exec(expr));
                        }else {
                            // 变量
                            LzyListBuffer.appendList(this.variableDeclarators(this.F.at(this.S.pos()).Modifiers(0L),expr ));
                        }

                        this.S.accept(LzyToken.SEMI);
                    }
                    break;//继续循环
                case SEMI:
                case LBRACE:
                case IF:
                case FOR:
                case WHILE:
                case DO:
                case TRY:
                case SWITCH:
                case SYNCHRONIZED:
                case RETURN:
                case THROW:
                case BREAK:
                case CONTINUE:
                case ELSE:
                case FINALLY:
                case CATCH:
                    LzyListBuffer.append(this.statement());
                    break;// 继续下一次循环
                case EOF:
                case RBRACE:
                case CASE:
                case DEFAULT:
                    return  LzyListBuffer.toList();//退出循环
            }
        }

    }

    protected LzyJCCatch catchClause(){
        int pos = this.S.pos();
        this.S.accept(LzyToken.CATCH);
        this.S.accept(LzyToken.LPAREN);
        // 异常类
        LzyJCVarDef varDef = this.variableDeclarator(this.F.at(this.S.pos()).Modifiers(0L), this.term(TYPE), false, null);
        this.S.accept(LzyToken.RPAREN);
        LzyJCBlock catchBody = block();
        return this.F.at(pos).Catch( varDef  , catchBody  );
    }

    @Override
    public LzyJCTree.JCStatement statement() {
        int pos = this.S.pos();
        LzyName name;
        switch ( this.S.token() ){
            case SEMI:
                this.S.nextToken();
                return this.F.at(pos).Skip();
            case LBRACE:
                return this.block();
            case RBRACE:
            case RPAREN:
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
            case LBRACKET:
            case DOT:
            case QUES:
            case PLUSPLUS:
            case SUBSUB:
            case PLUS:
            case SUB:
            case GTEQ:
            case GTGTGT:
            case GTGT:
            case CASE:
            case DEFAULT:
            default:
                name = this.S.name();
                LzyJCTree.JCExpression expr = this.term(EXPR);
                // 处理标签
                if ( this.S.token() == LzyToken.COLON && expr.getTag() == 35 ){
                    this.S.nextToken();
                    // 标签: 此处省略实现
                    // return this.F.at(pos).Labelled( name,this.statement() );
                    return null;
                }
                // 普通语句
                LzyJCExpressionStatement statement = this.F.at(pos).Exec(  (LzyJCTree.JCExpression)expr );
                this.S.accept(LzyToken.SEMI);
                return statement;
            case IF:
                this.S.nextToken();
                // 括号内表达式
                LzyJCTree.JCExpression exprCondition = this.parExpression();
                // if内语句
                LzyJCTree.JCStatement stateIf = this.statement();
                // else内语句
                LzyJCTree.JCStatement stateElse = null;
                if ( this.S.token() == LzyToken.ELSE ){
                    this.S.nextToken();
                    stateElse = this.statement();
                }
                return  this.F.at(pos).If(exprCondition,(LzyJCTree.JCStatement) stateIf,(LzyJCTree.JCStatement) stateElse);
            case FOR:
                this.S.nextToken();
                this.S.accept(LzyToken.LPAREN);
                LzyList initList = (this.S.token()==LzyToken.SEMI?LzyList.nil():this.forInit());
                // 判断是否是增强for循环
                // 1. 初始化大小为1
                // 2. 当前节点是JCVariableDecl
                // 3. 符号':'
                if ( initList.length() == 1 && ((LzyJCTree)initList.head).getTag() == 5 && this.S.token() == LzyToken.COLON  ){
                    // 声明的变量
                    LzyJCVarDef variableDecl = (LzyJCVarDef) initList.head;
                    // 符号':'
                    this.S.accept(LzyToken.COLON);
                    // 数据集合表达式
                    LzyJCTree.JCExpression exprList = this.term(EXPR);
                    // 符号')'
                    this.S.accept(LzyToken.RPAREN);
                    // 增强for循环: 暂时不做实现，因为需要解开语法糖!
                    /*return  this.F.at(pos).ForeachLoop(variableDecl,exprList,this.statement());*/
                    return null;
                }
                // 符号';'
                this.S.accept(LzyToken.SEMI);
                // 循环条件
                LzyJCTree.JCExpression exprCondition2 = (this.S.token()==LzyToken.SEMI?null:this.term(EXPR));
                // 符号';'
                this.S.accept(LzyToken.SEMI);
                // 循环增量
                LzyList addList = (this.S.token() == LzyToken.RPAREN?LzyList.nil():this.forUpdate());
                // 符号')'
                this.S.accept(LzyToken.RPAREN);
                // 循环体
                return  this.F.at(pos).ForLoop(initList,exprCondition2,addList,this.statement());
            case  WHILE:
                this.S.nextToken();
                return this.F.at(pos).WhileLoop(this.parExpression(),this.statement());
            case DO:
                this.S.nextToken();
                // 循环体
                LzyJCTree.JCStatement statement1 = this.statement();
                this.S.accept(LzyToken.WHILE);
                // 循环条件
                LzyJCTree.JCExpression exprCondition3 = this.parExpression();
                LzyJCDoloop whileLoop= this.F.at(pos).DoLoop(statement1, exprCondition3);
                this.S.accept(LzyToken.SEMI);
                return whileLoop;
            case SWITCH:
                this.S.nextToken();
                // 条件
                LzyJCTree.JCExpression expr2 = this.parExpression();
                // 符号'{'
                this.S.accept(LzyToken.LBRACE);
                // switch体中内容
                LzyList LzyList = this.switchBlockStatementGroups();
                LzyJCSwitch jcSwitch = this.F.at(pos).Switch(expr2, LzyList);
                // 符号‘}’
                this.S.accept(LzyToken.RBRACE);
                return jcSwitch;
            case RETURN:
                this.S.nextToken();
                LzyJCTree.JCExpression expression = this.S.token() == LzyToken.SEMI ? null : this.term(EXPR);
                LzyJCReturn aReturn = this.F.at(pos).Return(expression);
                this.S.accept(LzyToken.SEMI);
                return  aReturn;
            case BREAK:
                this.S.nextToken();
                LzyName name2 = this.S.token() != LzyToken.IDENTIFIER ? null : this.S.ident();
                LzyJCBreak aBreak = this.F.at(pos).Break(name2);
                this.S.accept(LzyToken.SEMI);
                return  aBreak;
            case CONTINUE:
                this.S.nextToken();
                LzyName name3 = this.S.token() != LzyToken.IDENTIFIER ? null : this.S.ident();
                LzyJCContinue aContinue = this.F.at(pos).Continue(name3);
                this.S.accept(LzyToken.SEMI);
                return aContinue;
            case TRY:
                this.S.nextToken();
                // 代码块
                LzyJCBlock tryBody = block();
                // catch列表
                LzyListBuffer<LzyJCCatch> catchers = new LzyListBuffer();
                // 最终
                LzyJCBlock finalizer = null;
                if ( this.S.token() == LzyToken.CATCH || this.S.token() == LzyToken.FINALLY  ){
                    // 处理catch
                    while ( this.S.token() == LzyToken.CATCH ) catchers.append(catchClause());
                    if ( this.S.token() == LzyToken.FINALLY ){
                        this.S.nextToken();
                        finalizer = block();
                    }
                }
                return this.F.at(pos).Try( tryBody  , catchers.toList() , finalizer );
        }
    }

    public LzyJCTree.JCStatement classOrInterfaceOrEnumDeclaration(LzyJCModifiers mods, String dc){
        // 空实现: 期待子类重写！
        return null;
    }

    /**
     * 修饰符
     * @param modifiers
     * @return
     */
    public LzyJCModifiers modifiersOpt(LzyJCModifiers modifiers) {
        LzyListBuffer listAnnoBuf = new LzyListBuffer();
        long flag ;
        int pos;
        // 尝试整个两个修饰符
        if ( modifiers == null ){
            flag = 0L;
            pos = this.S.pos();
        }else{
            flag = modifiers.flags;
            pos = modifiers.pos;
        }

        while (true){
            long newFlag;
            switch (this.S.token()){
                case PUBLIC:
                    newFlag  = 1L;
                    break;
                case FINAL:
                    newFlag = 16L;
                    break;
                case ABSTRACT:
                    newFlag = 1024L;
                    break;
                case PRIVATE:
                    newFlag = 2L;
                    break;
                case PROTECTED:
                    newFlag = 4L;
                    break;
                case STATIC:
                    newFlag = 8L;
                    break;
                case SYNCHRONIZED:
                    newFlag = 32L;
                    break;
                case ERROR:
                    newFlag = 0L;
                    this.S.nextToken();
                    break;
                default:
                    switch(this.S.token()) {
                        case INTERFACE:
                            flag |= 512L;
                            break;
                    }
                    return this.mods(pos,flag);
            }
            // 新修饰符 和 旧修饰符，有相同情况冲突！
            if ( (flag & newFlag) != 0L ){
                System.out.println("两个修饰符冲突:" + newFlag + " 和 " + flag + ". 位置:" + this.S.pos());
            }
            this.S.nextToken();
            flag |= newFlag;
        }
    }

    private LzyJCModifiers mods(int pos , long flag){
        // 创建修饰符
        LzyJCModifiers modifiers = this.F.at(pos).Modifiers(flag);
        return modifiers;
    }

    /**
     * 变量的解析: 可能一次声明多个。
     * 例如: int a,b;
     * @param pos
     * @param modifiers
     * @param expr
     * @param name
     * @param mustInit
     * @param doc
     * @return
     *
     */
    LzyList<LzyJCTree.JCExpression> variableDeclaratorsRest(int pos, LzyJCModifiers modifiers, LzyJCTree.JCExpression expr, LzyName name, boolean mustInit, String doc) {
        LzyListBuffer LzyListBuffer = new LzyListBuffer();
        LzyListBuffer.append(this.variableDeclaratorRest(pos, modifiers, expr, name, mustInit, doc));
        // 符号','
        while(this.S.token() == LzyToken.COMMA) {
            this.S.nextToken();
            LzyListBuffer.append(this.variableDeclarator(modifiers, expr, mustInit, doc));
        }
        return LzyListBuffer.toList();
    }

    LzyList<LzyJCTree.JCExpression> variableDeclarators(LzyJCModifiers modifiers, LzyJCTree.JCExpression expr) {
        return this.variableDeclaratorsRest(this.S.pos(), modifiers, expr, this.S.ident(), false, (String)null);
    }

    LzyJCVarDef variableDeclarator(LzyJCModifiers modifiers, LzyJCTree.JCExpression expr, boolean mustInit, String doc) {
        return this.variableDeclaratorRest(this.S.pos(), modifiers, expr, this.S.ident(), mustInit, doc);
    }

    LzyJCVarDef variableDeclaratorRest(int pos, LzyJCModifiers modifiers, LzyJCTree.JCExpression expr, LzyName name, boolean mustInit, String doc) {
        // 尝试处理数组
        expr = this.bracketsOpt(expr);
        LzyJCTree.JCExpression initExpr = null;
        // 符号'='
        if (this.S.token() == LzyToken.EQ) {
            this.S.nextToken();
            // 变量的初始化
            initExpr = this.variableInitializer();
        } else if (mustInit) {// 是否必须初始化
            System.out.println("必须初始化!");
           // this.syntaxError(this.S.pos(), "expected", this.keywords.token2string(Tokens.EQ));
        }
        LzyJCVarDef variableDecl = this.F.at(pos).VarDef(modifiers.flags, name, expr, initExpr);
        return variableDecl;
    }


    LzyJCBlock block(long flags){
        return  block(this.S.pos(),flags);
    }
    /**
     * 代码块
     * @param flags
     * @return
     */
    LzyJCBlock block(int pos,long flags) {
        this.S.accept(LzyToken.LBRACE);
        LzyList LzyList = this.blockStatements();
        LzyJCBlock jcBlock = this.F.at(pos).Block(flags, LzyList);
        while(this.S.token() == LzyToken.CASE || this.S.token() == LzyToken.DEFAULT) {
            // this.syntaxError("orphaned", this.keywords.token2string(this.S.token()));
            this.blockStatements();
        }
        jcBlock.endpos = this.S.pos();
        this.S.accept(LzyToken.RBRACE);

        return jcBlock;
    }

    LzyJCBlock block() {
        return this.block(0L);
    }

    LzyList<LzyJCTree.JCExpression> forInit() {
        int pos = this.S.pos();
        if (this.S.token() != LzyToken.FINAL ) {
            LzyJCTree.JCExpression expr = this.term(3);
            // 上一个表达式： 不是类型
            return (this.lastmode & 2) == 0 || this.S.token() != LzyToken.IDENTIFIER   ? this.moreStatementExpressions(pos, expr) : this.variableDeclarators(this.modifiersOpt(null), expr);
        } else {
            return this.variableDeclarators( this.optFinal(0L), this.term(TYPE) );
        }
    }

    LzyList<LzyJCTree.JCExpression> forUpdate() {
        return this.moreStatementExpressions(this.S.pos(), this.term(EXPR));
    }


    LzyJCTree.JCExpression parExpression() {
        int var1 = this.S.pos();
        this.S.accept(LzyToken.LPAREN);
        LzyJCTree.JCExpression expr = this.term(EXPR);
        this.S.accept(LzyToken.RPAREN);
       // return (JCTree.JCExpression)(this.genEndPos ? this.F.at(var1).Parens(var2) : var2);
        return expr;
    }

    LzyJCModifiers optFinal(long f) {
        LzyJCModifiers modifiers = this.modifiersOpt(null);
        // this.checkNoMods(var3.flags & -131089L);
        modifiers.flags |= f;
        return modifiers;
    }


    /**
     * 处理逗号分割开多个表达式的情况
     * @param pos
     * @param expr
     * @return
     */
    LzyList<LzyJCTree.JCExpression> moreStatementExpressions(int pos, LzyJCTree.JCExpression expr) {
        LzyListBuffer LzyListBuffer = new LzyListBuffer();
        LzyListBuffer.append(this.F.at(pos).Exec(expr));
        // 逗号分隔开多个表达式
        while(this.S.token() == LzyToken.COMMA) {
            this.S.nextToken();
            pos = this.S.pos();
            LzyJCTree.JCExpression expr2 = this.term(EXPR);
            LzyListBuffer.append(this.F.at(pos).Exec(expr2));
        }

        return LzyListBuffer.toList();
    }

    /**
     * 处理switch中内容: 创建节点
     * @return
     */
    LzyList<LzyJCCase> switchBlockStatementGroups() {
        LzyListBuffer var1 = new LzyListBuffer();

        while(true) {
            int var2 = this.S.pos();
            switch(this.S.token()) {
                case EOF:
                case RBRACE:
                    return var1.toList();
                case CASE:
                    this.S.nextToken();
                    LzyJCTree.JCExpression expr = this.term(EXPR);
                    this.S.accept(LzyToken.COLON);
                    LzyList LzyList = this.blockStatements();
                    var1.append(this.F.at(var2).Case(expr, LzyList));
                    break;
                case DEFAULT:
                    this.S.nextToken();
                    this.S.accept(LzyToken.COLON);
                    LzyList list2 = this.blockStatements();
                    var1.append(this.F.at(var2).Case((LzyJCTree.JCExpression) null, list2));
                    break;
                default:
                    this.S.nextToken();
                    //this.syntaxError(var2, "case.default.or.right-brace.expected");
            }
        }
    }


    /**
     * 所有实现类: A,B,C
     * @return
     */
    LzyList<LzyJCTree.JCExpression> typeList() {
        LzyListBuffer var1 = new LzyListBuffer();
        var1.append(this.term(TYPE));
        while(this.S.token() == LzyToken.COMMA) {
            this.S.nextToken();
            var1.append(this.term(TYPE));
        }
        return var1.toList();
    }

}

package work.liziyun.ast;


import work.liziyun.util.LzyContext;
import work.liziyun.world.LzyName;
import work.liziyun.world.LzyScanner;
import work.liziyun.world.LzyTable;
import work.liziyun.world.LzyToken;


public class LexerBuffer extends LzyScanner {

    private int errorEndPos;
    private LzyTable names;

    public LexerBuffer(LzyContext LzyContext, char[] chars,String fileName) {
        super( LzyContext,chars,fileName);

    }



    /**
     * 返回当前词Token的Name
     * 注意:
     *      1. 不处理assert断言关键字
     *      2. 不处理枚举
     *      3. 不处理this
     *      4. 不处理UNDERSCORE下划线
     * @return
     */
    public LzyName ident(){
        LzyName name;
        // 当前Token是标识符
        if (this.token() == LzyToken.IDENTIFIER){
            name = this.name();
            // 移词
            this.nextToken();
            return name;
        }else{
            // 记录错误: 我们一直期望得到一个标识符的词
            this.accept(LzyToken.IDENTIFIER);
            return this.names.error;
        }
    }

    public void accept(LzyToken expectToken){
        if (this.token() == expectToken ){
            this.nextToken();

        } else {
            // 设置错误位置
            setErrorEndPos(this.pos());
            // 记录报错信息
            System.out.println(this.prevEndPos()+" expected: " + expectToken.toString() );
        }
    }

    public void tryEndOf(){
        if (this.bp == this.buf.length-1){
            this.token = LzyToken.EOF;
            System.out.println("词法分析结束:"+fileName);
        }
    }

    public void setErrorEndPos(int errPos) {
        if (errPos > errorEndPos)
            errorEndPos = errPos;
    }

    public int getErrorEndPos(){
        return errorEndPos;
    }

    /** Skip forward until a suitable stop token is found.
     */
    public void skip(boolean stopAtImport, boolean stopAtMemberDecl, boolean stopAtIdentifier, boolean stopAtStatement) {
        while (true) {
            switch (token()) {
                case SEMI:
                    nextToken();
                    return;
                case PUBLIC:
                case FINAL:
                case ABSTRACT:
                case EOF:
                case CLASS:
                case INTERFACE:
                    return;
                case IMPORT:
                    if (stopAtImport)
                        return;
                    break;
                case LBRACE:
                case RBRACE:
                case PRIVATE:
                case PROTECTED:
                case STATIC:

                case LT:
                case BYTE:
                case SHORT:
                case CHAR:
                case INT:
                case LONG:
                case FLOAT:
                case DOUBLE:
                case BOOLEAN:
                case VOID:
                    if (stopAtMemberDecl)
                        return;
                    break;
                case IDENTIFIER:
                    if (stopAtIdentifier)
                        return;
                    break;
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
                    if (stopAtStatement)
                        return;
                    break;
            }
            nextToken();
        }
    }






}

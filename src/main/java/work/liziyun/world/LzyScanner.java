package work.liziyun.world;


import work.liziyun.util.LzyContext;

public class LzyScanner {
    // 当前位置
    int pos;
    // 当前结束位置
    int endPos;
    // 上一个结束位置
    int prevEndPos;
    // 开始
    char[] sbuf = new char[128];
    int sp;    // 单个词的缓存下标
    LzyName name;
    protected LzyToken token;
    // 结束
    protected char[] buf;
    protected int bp;     // 上一次的位置
    int buflen;
    int eofPos;
    // 当前字符
    private char ch;
    // Table
    private final LzyTable table;
    // KeyWords
    private final LzyKeywords lzyKeywords;
    // 进制: 默认10进制
    private int radix = 10;
    protected String fileName;


    public LzyScanner(LzyContext LzyContext, char[] chars,String fileName){
        this.table = LzyTable.instance(LzyContext);
        this.lzyKeywords = LzyKeywords.instance(LzyContext);
        buf = chars;
        this.ch = buf[0];
        this.buflen = chars.length;
        this.fileName = fileName;
    }


    public int radix(){
        return radix;
    }

    // 单个词缓存
    private void putChar(char c){
        // sbuf为单个词的缓存: 扩容操作
        if (this.sp == this.sbuf.length){
            char[] newChars = new char[this.sbuf.length*2];
            System.arraycopy(this.sbuf,0,newChars,0,this.sbuf.length);
            this.sbuf = newChars;
        }
        this.sbuf[this.sp++] = c;
    }

    public int prevEndPos() {
        return this.prevEndPos;
    }

    public int pos() {
        return this.pos;
    }

    public LzyToken token(){
        return this.token;
    }

    public LzyName name() {
        return this.name;
    }

    // 处理运算符
    private void scanOperator(){
        while (true){
            // 放入缓存
            this.putChar(this.ch);
            // names
            LzyName lzyName = this.table.fromChars(this.sbuf, 0, this.sp);
            // 如果不是敏感词
            if (this.lzyKeywords.key(lzyName) == LzyToken.IDENTIFIER){
                // 缓存位置回退
                --this.sp;
            }else{
                this.name = lzyName;
                // 如果是敏感词，那么获取。如果非敏感词，那么返回Token.IDENTIFIER
                this.token = this.lzyKeywords.key(lzyName);
                // 移动
                this.scanChar();
                // 运算符符号: 大于1，那么继续进入缓存
                if ( this.isSpecial(this.ch) ){
                    continue;
                }
            }
            // 结束循环和方法
            return;
        }
    }

    // 特殊词
    private boolean isSpecial(char c){
        switch (c){
            case '!': case '%': case '&': case '*': case '+': case '-':
            case ':': case '<': case '=': case '>': case '?': case '@': case '^': case '|': case '~': case '/':
                return true;
            default:
                return false;
        }
    }

    public void nextToken(){

        try{
            this.prevEndPos = this.endPos;
            // 单个词的缓存下标
            this.sp = 0;
            // 移动到首字符
            while (true){
                // 上一次的位置，作为当前的开始
                this.pos = this.bp;
                // 当前字符
                switch (this.ch){
                    // 空格
                    case ' ': case '\t': case '\f':
                        do {
                            // 移动下标
                            this.scanChar();
                        }while ( this.ch == ' ' || this.ch == '\t' || this.ch == '\f');
                        this.endPos = this.bp;
                        // 没有发现到词，进入下一轮循环!
                        break;
                    // 换行
                    case '\n':
                        // 移动下标
                        this.scanChar();
                        this.endPos = this.bp;
                        // 没有发现到词，进入下一轮循环!
                        break;
                    // 运算符
                    case '!': case '#': case '%': case '&': case '*': case '+': case '-': case ':': case '<': case '=':
                    case '>': case '?': case '@': case '\\': case '^': case '|': case '/':
                        this.scanOperator();
                        // 已经找到词
                        return;
                    default:
                        System.out.println("编译错误: 分词器未识别！");
                    case '\r':
                        this.scanChar();
                        this.endPos = this.bp;
                        break;
                    // 双引号
                    case '"':
                        // 移动
                        this.scanChar();
                        // 循环
                        while (true){
                            if (this.ch != '"' && this.ch != '\r' && this.ch != '\n' && this.bp < this.buflen){
                                this.scanLitChar();
                            }else{
                                break;
                            }
                        }
                        // 建立Token
                        if (this.ch == '"'){
                            this.token = LzyToken.STRINGLITERAL;
                            this.scanChar();
                        }else{
                            System.out.println("编译错误: 期望\"结尾");
                        }
                        return;
                        // 标识符名称
                    case '$':
                    case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': case 'G': case 'H':
                    case 'I': case 'J': case 'K': case 'L': case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R': case 'S': case 'T': case 'U': case 'V': case 'W': case 'X': case 'Y': case 'Z':
                    case '_':
                    case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'g': case 'h': case 'i': case 'j': case 'k': case 'l': case 'm': case 'n': case 'o': case 'p': case 'q': case 'r': case 's': case 't': case 'u': case 'v': case 'w': case 'x': case 'y': case 'z':
                        this.scanIdent();
                        return;
                    // char变量存储的字符
                    case '\'':
                        this.scanChar();
                        scanLitChar();
                 
                        if (this.ch == '\''){
                            this.token = LzyToken.CHARLITERAL;
                        }
                        this.scanChar();
                        return;
                    case '(':
                        this.scanChar();
                        this.token = LzyToken.LPAREN;
                        return;
                    case ')':
                        this.scanChar();
                        this.token = LzyToken.RPAREN;
                        return;
                    case ',':
                        this.scanChar();
                        this.token = LzyToken.COMMA;
                        return;
                    case '.':
                        //
                        this.scanChar();
                        this.token = LzyToken.DOT;
                        return;
                        // 数字
                    case '1':case '2':case '3':case '4':case '5':case '6':case '7':case '8':case '9': case '0':
                        scanNumber(10);
                        return;
                    case ';':
                        this.scanChar();
                        this.token = LzyToken.SEMI;
                        return;
                    case '[':
                        this.scanChar();
                        this.token = LzyToken.LBRACKET;
                        return;
                    case ']':
                        this.scanChar();
                        this.token = LzyToken.RBRACKET;
                        return;
                    case '{':
                        this.scanChar();
                        this.token = LzyToken.LBRACE;
                        return;
                    case '}':
                        this.scanChar();
                        this.token = LzyToken.RBRACE;
                        return;
                }
            }
        }catch (Exception e){
            System.out.println(e);
            this.endPos = this.bp;
        }

    }


    private boolean isNumber(){
        if (this.ch >= '0' && this.ch <= '9'){
            return true;
        }else {
            return false;
        }
    }

    // 解析小数点后的内容
    private void scanFractionAndSuffix(){
        while (isNumber()){
            this.putChar(this.ch);
            this.scanChar();
        }
        // 解析结尾
        if (ch == 'f' || ch == 'F'){
            putChar(ch);
            scanChar();
            this.token = LzyToken.FLOATLITERAL;
        }else{
            if (ch == 'd' || ch == 'D'){
                putChar(ch);
                scanChar();
            }
            // 默认浮点型的字面量： double
            this.token = LzyToken.DOUBLELITERAL;
        }
    }

    private void scanNumber(int radix){
        // 数字开头
        while (isNumber()){
            this.putChar(this.ch);
            this.scanChar();
        }
        // 数字结尾 1. '.'  2. 'l' 'L' 3. 'f' 'F' 'd' 'D'
        // 小数处理
        if (this.ch == '.'){
            // 小数点存入缓存
            this.putChar(this.ch);
            // 移
            this.scanChar();
            // 处理小数的结尾
            scanFractionAndSuffix();
        }else {
            if (this.ch == 'l' || this.ch == 'L'){
                scanChar();
                this.token = LzyToken.LONGLITERAL;
            }else{
                // 整型的字面量: int
                this.token = LzyToken.INTLITERAL;
            }
        }
    }


    private void scanIdent(){
        // 缓存
        this.putChar(this.ch);
        // 移动
        this.scanChar();
        while (true){
            while (true){
                switch (this.ch){
                    case '$':
                    case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
                    case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': case 'G': case 'H':
                    case 'I': case 'J': case 'K': case 'L': case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R': case 'S': case 'T': case 'U': case 'V': case 'W': case 'X': case 'Y': case 'Z':
                    case '_':
                    case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'g': case 'h': case 'i': case 'j': case 'k': case 'l': case 'm': case 'n': case 'o': case 'p': case 'q': case 'r': case 's': case 't': case 'u': case 'v': case 'w': case 'x': case 'y': case 'z':
                        // 缓存
                        this.putChar(this.ch);
                        // 移动
                        this.scanChar();
                        break;
                    default:
                        if (this.ch < 128){
                            this.name = this.table.fromChars(this.sbuf,0,this.sp);
                            this.token = this.lzyKeywords.key(this.name);
                        }else{
                            System.out.println("编译错误: 分词器解析标识符结尾失败!");
                        }
                        return;
                }
            }
        }
    }

    private void scanLitChar(){
        // 我们将处理字符串中包含转义字符情况
        if (this.ch == '\\'){
            // 移动
            this.scanChar();
            switch (this.ch){
                case '"':
                    // 缓存
                    this.putChar('"');
                    this.scanChar();
                    break;
                case '\'':
                    this.putChar('\'');
                    this.scanChar();
                    break;
                case '\\':
                    this.putChar('\\');
                    this.scanChar();
                    break;
                case 'b':
                    this.putChar('\b');
                    this.scanChar();
                    break;
                case 'f':
                    this.putChar('\f');
                    this.scanChar();
                    break;
                case 'n':
                    this.putChar('\n');
                    this.scanChar();
                    break;
                case 'r':
                    this.putChar('\r');
                    this.scanChar();
                    break;
                case 't':
                    this.putChar('\t');
                    this.scanChar();
                    break;
            }
        }else if (this.bp != this.buflen){
            // 放入缓存
            this.putChar(this.ch);
            // 移动
            this.scanChar();
        }
    }

    private void scanChar(){
        if (this.bp != this.buf.length-1){
            this.ch = this.buf[++this.bp];
        }
        // 注意: 我们不处理unicode编码.即开头
    }


    // 作用: 当缓存中是一个字面量时将调用，转换成一个String字符串
    public String stringVal(){
        return new String(sbuf,0,this.sp);
    }
}

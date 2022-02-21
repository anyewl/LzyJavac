package work.liziyun.code;





import work.liziyun.code.symbol.LzyMethodSymbol;
import work.liziyun.code.symbol.LzyVarSymbol;
import work.liziyun.code.type.LzyType;
import work.liziyun.code.type.LzyTypeTags;
import work.liziyun.tag.LzyByteCodes;
import work.liziyun.util.LzyList;
import work.liziyun.util.LzyListBuffer;


/**
 * 作用: 每个方法对应着一个Code对象。 1. 局部变狼表 2.操作数栈
 */
public class LzyCode implements LzyByteCodes, LzyTypeTags {

    // 最大栈深度
    public int max_stack = 0;
    // 局部变量表大小
    public int max_locals = 0;

    // 已经合成好的回填信息链
    LzyCode.Chain pendingJumps = null;

    // 当前字节码指令的位置
    public int cp;

    // 异常表
    public LzyListBuffer<char[]> catchInfo = new LzyListBuffer<>();

    // 字节码指令
    public byte[] code = new byte[64];

    // 禁用压缩代码
    // false表示开启代码压缩
    // true表示关闭代码压缩
    private boolean fixedPc = false;

    // 下一个可用的寄存器
    public int nextreg = 0;

    // 当前栈大小
    public int stacksize = 0;

    // 执行字节码指令后: 操作数栈的大小变化表
    public static final int[] stackdiff;
    // 可达性
    private boolean alive = true;
    // 开启地址扩容模式
    public boolean fatcode;
    //
    final LzyMethodSymbol meth;

    final LzySymtab syms;
    //
    boolean pendingStackMap = false;

    public int varBufferSize;

    // 局部变量表
    public LocalVar[] lvar;

    public LzyCode(boolean fatcode,LzySymtab syms,LzyMethodSymbol meth){
        this.fatcode = fatcode;
        this.syms = syms;
        this.meth = meth;
    }


    static {
        stackdiff = new int[203];
        stackdiff[0] = 0;
        stackdiff[1] = 1;
        stackdiff[2] = 1;
        stackdiff[3] = 1;
        stackdiff[4] = 1;
        stackdiff[5] = 1;
        stackdiff[6] = 1;
        stackdiff[7] = 1;
        stackdiff[8] = 1;
        stackdiff[9] = 2;
        stackdiff[10] = 2;
        stackdiff[11] = 1;
        stackdiff[12] = 1;
        stackdiff[13] = 1;
        stackdiff[14] = 2;
        stackdiff[15] = 2;
        stackdiff[16] = 1;
        stackdiff[17] = 1;
        stackdiff[18] = -999;
        stackdiff[19] = -999;
        stackdiff[20] = -999;
        stackdiff[21] = 1;
        stackdiff[22] = 2;
        stackdiff[23] = 1;
        stackdiff[24] = 2;
        stackdiff[25] = 1;
        stackdiff[26] = 1;
        stackdiff[30] = 2;
        stackdiff[34] = 1;
        stackdiff[38] = 2;
        stackdiff[42] = 1;
        stackdiff[27] = 1;
        stackdiff[31] = 2;
        stackdiff[35] = 1;
        stackdiff[39] = 2;
        stackdiff[43] = 1;
        stackdiff[28] = 1;
        stackdiff[32] = 2;
        stackdiff[36] = 1;
        stackdiff[40] = 2;
        stackdiff[44] = 1;
        stackdiff[29] = 1;
        stackdiff[33] = 2;
        stackdiff[37] = 1;
        stackdiff[41] = 2;
        stackdiff[45] = 1;
        stackdiff[46] = -1;
        stackdiff[47] = 0;
        stackdiff[48] = -1;
        stackdiff[49] = 0;
        stackdiff[50] = -1;
        stackdiff[51] = -1;
        stackdiff[52] = -1;
        stackdiff[53] = -1;
        stackdiff[54] = -1;
        stackdiff[55] = -2;
        stackdiff[56] = -1;
        stackdiff[57] = -2;
        stackdiff[58] = -1;
        stackdiff[59] = -1;
        stackdiff[63] = -2;
        stackdiff[67] = -1;
        stackdiff[71] = -2;
        stackdiff[75] = -1;
        stackdiff[60] = -1;
        stackdiff[64] = -2;
        stackdiff[68] = -1;
        stackdiff[72] = -2;
        stackdiff[76] = -1;
        stackdiff[61] = -1;
        stackdiff[65] = -2;
        stackdiff[69] = -1;
        stackdiff[73] = -2;
        stackdiff[77] = -1;
        stackdiff[62] = -1;
        stackdiff[66] = -2;
        stackdiff[70] = -1;
        stackdiff[74] = -2;
        stackdiff[78] = -1;
        stackdiff[79] = -3;
        stackdiff[80] = -4;
        stackdiff[81] = -3;
        stackdiff[82] = -4;
        stackdiff[83] = -3;
        stackdiff[84] = -3;
        stackdiff[85] = -3;
        stackdiff[86] = -3;
        stackdiff[87] = -1;
        stackdiff[88] = -2;
        stackdiff[89] = 1;
        stackdiff[90] = 1;
        stackdiff[91] = 1;
        stackdiff[92] = 2;
        stackdiff[93] = 2;
        stackdiff[94] = 2;
        stackdiff[95] = 0;
        stackdiff[96] = -1;
        stackdiff[97] = -2;
        stackdiff[98] = -1;
        stackdiff[99] = -2;
        stackdiff[100] = -1;
        stackdiff[101] = -2;
        stackdiff[102] = -1;
        stackdiff[103] = -2;
        stackdiff[104] = -1;
        stackdiff[105] = -2;
        stackdiff[106] = -1;
        stackdiff[107] = -2;
        stackdiff[108] = -1;
        stackdiff[109] = -2;
        stackdiff[110] = -1;
        stackdiff[111] = -2;
        stackdiff[112] = -1;
        stackdiff[113] = -2;
        stackdiff[114] = -1;
        stackdiff[115] = -2;
        stackdiff[116] = 0;
        stackdiff[117] = 0;
        stackdiff[118] = 0;
        stackdiff[119] = 0;
        stackdiff[120] = -1;
        stackdiff[121] = -1;
        stackdiff[122] = -1;
        stackdiff[123] = -1;
        stackdiff[124] = -1;
        stackdiff[125] = -1;
        stackdiff[126] = -1;
        stackdiff[127] = -2;
        stackdiff[128] = -1;
        stackdiff[129] = -2;
        stackdiff[130] = -1;
        stackdiff[131] = -2;
        stackdiff[132] = 0;
        stackdiff[133] = 1;
        stackdiff[134] = 0;
        stackdiff[135] = 1;
        stackdiff[136] = -1;
        stackdiff[137] = -1;
        stackdiff[138] = 0;
        stackdiff[139] = 0;
        stackdiff[140] = 1;
        stackdiff[141] = 1;
        stackdiff[142] = -1;
        stackdiff[143] = 0;
        stackdiff[144] = -1;
        stackdiff[145] = 0;
        stackdiff[146] = 0;
        stackdiff[147] = 0;
        stackdiff[148] = -3;
        stackdiff[149] = -1;
        stackdiff[150] = -1;
        stackdiff[151] = -3;
        stackdiff[152] = -3;
        stackdiff[153] = -1;
        stackdiff[154] = -1;
        stackdiff[155] = -1;
        stackdiff[156] = -1;
        stackdiff[157] = -1;
        stackdiff[158] = -1;
        stackdiff[159] = -2;
        stackdiff[160] = -2;
        stackdiff[161] = -2;
        stackdiff[162] = -2;
        stackdiff[163] = -2;
        stackdiff[164] = -2;
        stackdiff[165] = -2;
        stackdiff[166] = -2;
        stackdiff[167] = 0;
        stackdiff[168] = 0;
        stackdiff[169] = 0;
        stackdiff[170] = -1;
        stackdiff[171] = -1;
        stackdiff[172] = -1001;
        stackdiff[173] = -1002;
        stackdiff[174] = -1001;
        stackdiff[175] = -1002;
        stackdiff[176] = -1001;
        stackdiff[177] = -1000;
        stackdiff[178] = -999;
        stackdiff[179] = -999;
        stackdiff[180] = -999;
        stackdiff[181] = -999;
        stackdiff[182] = -999;
        stackdiff[183] = -999;
        stackdiff[184] = -999;
        stackdiff[185] = -999;
        stackdiff[186] = 0;
        stackdiff[187] = 1;
        stackdiff[188] = 0;
        stackdiff[189] = 0;
        stackdiff[190] = 0;
        stackdiff[191] = -1001;
        stackdiff[192] = 0;
        stackdiff[193] = 0;
        stackdiff[194] = -1;
        stackdiff[195] = -1;
        stackdiff[196] = 0;
        stackdiff[197] = -999;
        stackdiff[198] = -1;
        stackdiff[199] = -1;
        stackdiff[200] = 0;
        stackdiff[201] = 0;
        stackdiff[202] = 0;
    }


    /**
     * 向异常表中插入一条数据
     * @param startPc
     * @param endPc
     * @param handlerPc
     * @param catchType
     */
    public void addCatch(char startPc , char endPc , char handlerPc , char catchType){
        this.catchInfo.append(new char[]{startPc,endPc,handlerPc,catchType });
    }

    public void newRegSegment() {
        this.nextreg = this.max_locals;
    }

    public void markDead(){
        this.alive = false;
    }


    /**
     * 带寄存器的所有变量的结束作用域 大于等于 第一个
     * @param first
     */
    public void endScopes(int first) {
        this.nextreg = first;
    }

    /**
     * 申报入境点: 返回当前字节码的位置
     * @return
     */
    public int entryPoint(){
        this.alive = true;
        return this.curPc();
    }


    /**
     * 申报入境点: 返回当前字节码的位置
     * @param size 栈大小
     * @return
     */
    public int entryPoint(int size){
        this.alive = true;
        this.stacksize = size;
        if ( this.stacksize > this.max_stack ){
            this.max_stack = this.stacksize;
        }
        return this.curPc();
    }






    public int newLocal(LzyVarSymbol varSymbol){
        return  varSymbol.adr = this.newLocal(varSymbol.type);
    }



    /**
     * 局部变量表
     * @param type
     * @return
     */
    public int newLocal(LzyType type) {
        return this.newLocal(typecode(type));
    }


    /**
     * 局部变量表
     * @param opcode
     * @return
     */
    public int newLocal(int opcode) {
        // 局部变量表中下一个位置
        int nextreg = this.nextreg;
        // 数据宽度
        int width = width(opcode);
        // 更新局部变量表中下一个位置
        this.nextreg = nextreg + width;
        // 如果超出最大值: 扩容
        if (this.nextreg > this.max_locals) {
            this.max_locals = this.nextreg;
        }
        // 返回局部变量表下一个可用位置
        return nextreg;
    }


    /**
     * 计算形式参数所有的宽度
     * @param argList
     * @return
     */
    public static int width(LzyList<LzyType> argList){
        int sumWidth = 0;
        LzyList<LzyType> argList2 = argList;
        while ( argList2.nonEmpty() ){
            sumWidth += width( argList2.head );
            // 下一个
            argList2 = argList2.tail;
        }
        return sumWidth;
    }


    /**
     * 计算类型宽度
     * @param type
     * @return
     */
    public static int width(LzyType type){
        return type == null? 1: width(typecode(type));
    }


    /**
     * 局部变量表宽度占用大小
     *      例如: 4字节只占用1个位置 ， 8字节占用两个位置
     * @param typecode
     * @return
     */
    public static int width(int typecode){
        switch (typecode){
            case LONGcode:
            case DOUBLEcode:
                return 2;
            case VOIDcode:
                return 0;
            default:
                return 1;
        }
    }


    /**
     * 存储1字节
     * @param index 位置
     * @param data 数据
     */
    public void put1(int index , int data){
        this.code[index] = (byte) data;
    }

    /**
     * 存储2字节
     * @param index 位置
     * @param data 数据
     */
    public void put2(int index , int data){
        // 存储第二个字节
        this.put1(index,data>>8);
        // 存储第一个字节
        this.put1(index+1,data);
    }

    /**
     * 存储4字节
     * @param index 位置
     * @param data 数据
     */
    public void put4(int index , int data){
        // 存储第四个字节
        this.put1(index,data>>24);
        // 存储第三个字节
        this.put1(index+1,data>>16);
        // 存储第二个字节
        this.put1(index+2,data>>8);
        // 存储第一个字节
        this.put1(index+3,data);
    }

    public static int arraycode(LzyType type){
        switch (type.tag){
            case BYTE:
                return 8;
            case CHAR:
                return 5;
            case SHORT:
                return 9;
            case INT:
                return 10;
            case LONG:
                return 11;
            case FLOAT:
                return 6;
            case DOUBLE:
                return 7;
            case BOOLEAN:
                return 4;
            case VOID:
            default:
                throw new AssertionError("arraycode " + type );
            case CLASS:
                return 0;
            case ARRAY:
                return 1;
        }
    }




    /**
     * 获取1字节
     * 注意: 结果大小超过255将无效,因为有效值在一个字节内。
     * @param index
     * @return
     */
    public int get1(int index){
        return this.code[index]&255;
    }

    /**
     * 获取2字节
     * @param index
     * @return
     */
    public int get2(int index){
        return this.get1(index) << 8 | this.get1(index+1);
    }

    /**
     * 获取4字节
     * @param index
     * @return
     */
    public int get4(int index){
        return this.get1(index) << 24 | this.get1(index+1) << 16 | this.get1(index+2) << 8 | this.get1(index+3);
    }

    public void resolvePending(){
        LzyCode.Chain chain = this.pendingJumps;
        this.pendingJumps = null;
        this.resolve(chain,this.cp);
    }

    public int emitJump(int typecode){
        // 未开启地址扩容模式
        if (!this.fatcode){
            // 1. typecode是跳转指令1字节
            // 2. 0是跳转的字节码行号. 目前无法知道具体的值,先用0占位.后续需要调用resoleve(Chain,int)方法进行地址回填
            this.emitop2(typecode,0);
            // 找到跳转指令前面的字节码行号: 两部分组成1字节的跳转指令2字节的字节码行号
            return this.cp - 3;
        }else { // 开启地址扩容模式
            if (typecode != LzyByteCodes.goto_ && typecode != LzyByteCodes.jsr){
                // 强制跳过两条指令： 8个字节
                // 第一条指令: 比较指令+操作数 ---> 1字节是字节码指令 2字节是操作数
                // 第二跳指令: goto_w跳转指令+操作数 ---> 1字节是字节码指令 4字节是操作数
                this.emitop2(negate(typecode),8);
                this.emitop4(LzyByteCodes.goto_w,0);
            }else {
                this.emitop4(typecode+LzyByteCodes.goto_w-LzyByteCodes.goto_,0);
            }
        }
        return this.cp - 5;
    }

    public void emit1(int typecode){
        // 可达性判断
        if (this.alive){
            // 扩容操作
            if (this.cp == this.code.length){
                byte[] bytes = new byte[this.cp * 2];
                System.arraycopy(this.code,0,bytes,0,this.cp);
                this.code = bytes;
            }
            // 字节码指令
            this.code[this.cp++] = (byte) typecode;
        }
    }


    /**
     * 添加一个2字节的操作数
     * @param od 操作数
     */
    public void emit2(int od){
        // 可达性判断
        if (this.alive){
            // 超出缓存大小
            if ( this.cp + 2 > this.code.length ){
                // 添加一层安全校验
                this.emit1(od>>8);
                // 添加一层安全校验
                this.emit1(od);
            }else{
                // 存储第二个字节
                this.code[this.cp++] = (byte) (od>>8);
                // 存储第一个字节
                this.code[this.cp++] = (byte) od;
            }
        }
    }

    public void emit4(int typecode){
        if (this.alive){
            if (this.cp + 4 > this.code.length){
                // 第四个字节
                this.emit1(typecode>>24);
                // 第三个字节
                this.emit1( typecode>>16 );
                // 第二个字节
                this.emit1(typecode>>8);
                // 第一个字节
                this.emit1(typecode);
            }else {
                this.code[this.cp++] = (byte) (typecode>>24);
                this.code[this.cp++] = (byte) (typecode>>16);
                this.code[this.cp++] = (byte) (typecode>>8);
                this.code[this.cp++] = (byte)typecode;
            }
        }
    }

    /**
     * 作用: 生成无操作数的指令
     * @param typeCode 指令编码
     */
    public void emitop(int typeCode){
        this.emitop(typeCode,stackdiff[typeCode]);
    }

    /**
     * 添加一个字节码指令 和 操作数(2字节的)
     * @param op
     * @param od
     */
    public void emitop2(int op,int od){
        // 添加一个指令
        this.emitop(op);
        // 添加一个操作数2字节的
        this.emit2(od);
    }

    /**
     * 操作数od的大小是4字节
     * @param op
     * @param od
     */
    public void emitop4(int op,int od){
        this.emitop(op);
        this.emit4(od);
    }

    /**
     * 作用: 生成无操作数的指令
     * @param typeCode 指令编码
     * @param stackChangeSize 操作数栈的改变大小
     */
    public void emitop(int typeCode,int stackChangeSize){
        // 如果有回填信息: 跳转信息 ---> 可能将alive由false变true
        if ( this.pendingJumps != null) {
            // 开始回填
            this.resolvePending();
        }
        // 可达性判断
        if (this.alive){
            // 将指令压入缓存
            this.emit1(typeCode);
            // 栈深度判断
            if ( stackChangeSize <= -1000){
                this.stacksize = this.stacksize + stackChangeSize + 1000;
                this.alive = false;
            }else{
                // 更新当前栈深度
                this.stacksize += stackChangeSize;
                // 当前栈的深度 大于 最大栈深度
                if (this.stacksize > this.max_stack){
                    // 更新最大栈深度
                    this.max_stack = this.stacksize;
                }
            }
        }
    }


    /**
     * 操作数od的大小是1字节
     * @param op
     * @param od
     */
    public void emitop1(int op,int od){
        this.emitop(op);
        this.emit1(od);
    }


    /**
     * 单字节的操作数和操作码
     * @param op 操作码
     * @param od 操作数
     */
    public void emitop1w(int op,int od){
        // 操作数大于255: 这里操作数含义,局部变量表的下标。
        if ( od > 255 ){
            // 生成一个扩容指令
            this.emitop(LzyByteCodes.wide);
            //
            this.emitop2(op,od);
        }else {
            this.emitop1(op,od);
        }
    }

    /**
     * byet，char，short都将按照int类型来处理
     * @param targetcode
     * @return
     */
    public static int truncate(int targetcode){
        switch (targetcode){
            case BYTEcode:
            case CHARcode:
            case SHORTcode:
                return INTcode;
            default:
                return targetcode;
        }
    }


    public static String mnem(int opcode){
        return LzyCode.Mneumonics.mnem[opcode];
    }



    public static int typecode(LzyType type){
        switch (type.tag){
            case LzyTypeTags.BYTE: return LzyByteCodes.BYTEcode;

            case LzyTypeTags.SHORT: return LzyByteCodes.SHORTcode;

            case LzyTypeTags.CHAR: return LzyByteCodes.CHARcode;

            case LzyTypeTags.INT: return LzyByteCodes.INTcode;

            case LzyTypeTags.LONG: return LzyByteCodes.LONGcode;

            case LzyTypeTags.FLOAT: return LzyByteCodes.FLOATcode;

            case LzyTypeTags.DOUBLE: return LzyByteCodes.DOUBLEcode;

            case LzyTypeTags.BOOLEAN: return LzyByteCodes.BYTEcode;

            case LzyTypeTags.VOID: return LzyByteCodes.VOIDcode;

            case LzyTypeTags.CLASS: case LzyTypeTags.ARRAY:
            case LzyTypeTags.METHOD: case LzyTypeTags.BOT:
                return LzyByteCodes.OBJECTcode;
            default:
                throw new AssertionError("typecode " + type.tag);
        }
    }


    /**
     * 作用: 地址回填
     *      当生成跳转指令时,需要指定跳转地址。
     *      某些情况下,跳转地址可能是个未知数。
     *      Chain临时保存跳转相关信息,生成目标指令时,再来回填这个地址。
     */
    public static class Chain{
        // 指令的位置: 需要回填的指令
        // 细节: 是跳转指令前面的位置
        public final int pc;
        // 相同跳转地址: 形成链表
        public final Chain next;
        // 栈大小
        public final int stacksize;

        public Chain(int pc, int stacksize, Chain next) {
            this.pc = pc;
            this.next = next;
            this.stacksize = stacksize;
        }
    }

    /**
     * 作用: 跳转指令回填
     *      当我们生成目标跳转的字节码行号那条指令时，我们需要对前面的指令进行回填。
     * @param chain 回填的信息
     */
    public void resolve(LzyCode.Chain chain){
        // 将回填信息合并到一个链上
        this.pendingJumps = mergeChains(chain,this.pendingJumps);
    }


    /**
     * 作用: 获取比较指令,相反的指令。
     * 例如:
     *      大于0     相反     小于等于0
     * @param typecode
     * @return
     */
    public static int negate(int typecode){
        if ( typecode == LzyByteCodes.if_acmp_null ){
            return LzyByteCodes.if_acmp_nonnull;
        }else if ( typecode == LzyByteCodes.if_acmp_nonnull ){
            return LzyByteCodes.if_acmp_null;
        }else{
            // 相反指令的偏移量公式: (typecode + 1^1)-1u
            return (typecode+1^1)-1;
        }
    }

    public int curPc(){
        // 开始地址回填
        if (this.pendingJumps != null){
            this.resolvePending();
        }
        // 关闭代码压缩
        this.fixedPc = true;
        return this.cp;
    }



    /**
     *
     * @param chain 回填信息: 需要
     * @param target 跳转地址
     */
    public void resolve(LzyCode.Chain chain,int target){

        while (chain != null){
            // 目标跳转地址一定大于需要回填的地方
            if (target <= chain.pc && this.stacksize != 0 ){
                throw new AssertionError();
            }
            // 跳转地址已经超过当前字节码指令的末尾
            if (target >= this.cp){
                // 确保跳转地址不超过当前字节码指令的末尾
                target = this.cp;
            }else if (this.get1(target) == goto_ ){// 如果跳转的地方是一个: 跳转指令
                // 更新跳转地址: goto指向的地址(goto指令后面的1字节数据,既第二个跳转位置)
                // 这是一个神奇的套后操作!这里跳转了两次
                // 例如:
                // 1. goto 4
                // 4  goto 7
                // 7  iadd
                target += this.get2(target+1);
            }

            // 当无条件跳转指令目标位置为下一条指令时,且下一条指令既末尾。不需要goto这条指令!
            // 既无条件跳转到下一行: 这是一个多余的操作
            if ( this.get1(chain.pc) == goto_ && chain.pc + 3 == target &&  target == this.cp && !this.fixedPc  ){
                // 去掉末尾3字节的大小
                // 为什么是三字节? 1.跳转指令占用1字节 2.字节码行号占用2字节 --> 既去掉一个goto x
                // 如果不去掉是什么效果?
                // 例: goto 25
                // 注意: goto字节码指令行号是23 ,  25字节码指令行号是24和25
                // 多么荒唐!
                // 注意： 这段代码有兼容性问题！一旦开启fatcode代码膨胀，那么减去3字节是错误的。应该减去5字节。
                this.cp -= 3;
                target -= 3;
            }else {
                if ( this.fatcode ){
                    // 回填跳转地址: 4字节的差值
                    // 注意: 真实跳转地址,需要二次计算 ---> this.cp + (target-chain.pc)
                    this.put4( chain.pc + 1 , target - chain.pc);
                }else if ( target - chain.pc >= Short.MIN_VALUE && target - chain.pc <= Short.MAX_VALUE   ){
                    // 回填跳转地址: 2字节的差值
                    // 注意: 真实跳转地址,需要二次计算 ---> this.cp + (target-chain.pc)
                    this.put2(chain.pc+1,target-chain.pc);
                }else {
                    // 开启地址扩容模式
                    this.fatcode = true;
                }
            }

            // 当前字节码指令位置就是跳转位置
            if ( cp == target && !this.alive ){
                // 修复栈大小
                this.stacksize = chain.stacksize;
                // 修复代码可达
                this.alive = true;
            }

            chain = chain.next;
        }

    }

    /**
     * 可达性判断
     * @return
     */
    public boolean isAive(){
        //
        return this.alive || this.pendingJumps != null;
    }

    public LzyCode.Chain branch(int typecode){
        LzyCode.Chain chain = null;
        if (typecode == LzyByteCodes.goto_){
            // 回填链表
            chain = this.pendingJumps;
            // 清空回填链表
            this.pendingJumps = null;
        }

        if ( typecode != LzyByteCodes.dontgoto && this.isAive() ){
            // 创建回填信息: 1.跳转指令前面的字节码行号位置 2.当前栈大小 3.链表
            chain = new LzyCode.Chain( this.emitJump(typecode) , this.stacksize , chain  );
            // 一旦开启地址扩容模式，那么代码压缩将关闭。因为代码兼容性问题！
            this.fixedPc = this.fatcode;
            // 生成字节码跳转指令goto,那么后续字节码不可达!
            if (typecode == LzyByteCodes.goto_){
                // 不可达
                this.alive = false;
            }
        }
        return chain;
    }


    /**
     * 作用: 尝试合并两个回填信息,形成链表。从大到小排序
     * @param chain1
     * @param chain2
     * @return
     */
    public static LzyCode.Chain mergeChains(LzyCode.Chain chain1,LzyCode.Chain chain2){
        if ( chain2 == null ){
            return chain1;
        }else if (chain1 == null){
            return chain2;
        }else {
            // 指令pc大的的放第一个
            if (chain1.pc < chain2.pc){
                // 递归: 竞争失败者 和 优胜者的下一个
                return new LzyCode.Chain(chain2.pc,chain2.stacksize,mergeChains(chain1,chain2.next));
            }else {
                // 递归: 优胜者的下一个 和 竞争失败者
                return new LzyCode.Chain(chain1.pc,chain1.stacksize,mergeChains(chain1.next,chain2));
            }
        }
    }

    public static class LocalVar{
        // 变量符号
        final LzyVarSymbol varSymbol;

        final char reg;


        char start_pc = '\uffff';

        char length = '\uffff';

        public LzyCode.LocalVar dup(){
            return new LzyCode.LocalVar(this.varSymbol);
        }

        public LocalVar(LzyVarSymbol varSymbol){
            this.varSymbol = varSymbol;
            this.reg = (char) varSymbol.adr;
        }
    }



    private static class Mneumonics{
        private static final String[] mnem = new String[203];
        static {
            mnem[0] = "nop";
            mnem[1] = "aconst_null";
            mnem[2] = "iconst_m1";
            mnem[3] = "iconst_0";
            mnem[4] = "iconst_1";
            mnem[5] = "iconst_2";
            mnem[6] = "iconst_3";
            mnem[7] = "iconst_4";
            mnem[8] = "iconst_5";
            mnem[9] = "lconst_0";
            mnem[10] = "lconst_1";
            mnem[11] = "fconst_0";
            mnem[12] = "fconst_1";
            mnem[13] = "fconst_2";
            mnem[14] = "dconst_0";
            mnem[15] = "dconst_1";
            mnem[16] = "bipush";
            mnem[17] = "sipush";
            mnem[18] = "ldc1";
            mnem[19] = "ldc2";
            mnem[20] = "ldc2w";
            mnem[21] = "iload";
            mnem[22] = "lload";
            mnem[23] = "fload";
            mnem[24] = "dload";
            mnem[25] = "aload";
            mnem[26] = "iload_0";
            mnem[30] = "lload_0";
            mnem[34] = "fload_0";
            mnem[38] = "dload_0";
            mnem[42] = "aload_0";
            mnem[27] = "iload_1";
            mnem[31] = "lload_1";
            mnem[35] = "fload_1";
            mnem[39] = "dload_1";
            mnem[43] = "aload_1";
            mnem[28] = "iload_2";
            mnem[32] = "lload_2";
            mnem[36] = "fload_2";
            mnem[40] = "dload_2";
            mnem[44] = "aload_2";
            mnem[29] = "iload_3";
            mnem[33] = "lload_3";
            mnem[37] = "fload_3";
            mnem[41] = "dload_3";
            mnem[45] = "aload_3";
            mnem[46] = "iaload";
            mnem[47] = "laload";
            mnem[48] = "faload";
            mnem[49] = "daload";
            mnem[50] = "aaload";
            mnem[51] = "baload";
            mnem[52] = "caload";
            mnem[53] = "saload";
            mnem[54] = "istore";
            mnem[55] = "lstore";
            mnem[56] = "fstore";
            mnem[57] = "dstore";
            mnem[58] = "astore";
            mnem[59] = "istore_0";
            mnem[63] = "lstore_0";
            mnem[67] = "fstore_0";
            mnem[71] = "dstore_0";
            mnem[75] = "astore_0";
            mnem[60] = "istore_1";
            mnem[64] = "lstore_1";
            mnem[68] = "fstore_1";
            mnem[72] = "dstore_1";
            mnem[76] = "astore_1";
            mnem[61] = "istore_2";
            mnem[65] = "lstore_2";
            mnem[69] = "fstore_2";
            mnem[73] = "dstore_2";
            mnem[77] = "astore_2";
            mnem[62] = "istore_3";
            mnem[66] = "lstore_3";
            mnem[70] = "fstore_3";
            mnem[74] = "dstore_3";
            mnem[78] = "astore_3";
            mnem[79] = "iastore";
            mnem[80] = "lastore";
            mnem[81] = "fastore";
            mnem[82] = "dastore";
            mnem[83] = "aastore";
            mnem[84] = "bastore";
            mnem[85] = "castore";
            mnem[86] = "sastore";
            mnem[87] = "pop";
            mnem[88] = "pop2";
            mnem[89] = "dup";
            mnem[90] = "dup_x1";
            mnem[91] = "dup_x2";
            mnem[92] = "dup2";
            mnem[93] = "dup2_x1";
            mnem[94] = "dup2_x2";
            mnem[95] = "swap";
            mnem[96] = "iadd";
            mnem[97] = "ladd";
            mnem[98] = "fadd";
            mnem[99] = "dadd";
            mnem[100] = "isub";
            mnem[101] = "lsub";
            mnem[102] = "fsub";
            mnem[103] = "dsub";
            mnem[104] = "imul";
            mnem[105] = "lmul";
            mnem[106] = "fmul";
            mnem[107] = "dmul";
            mnem[108] = "idiv";
            mnem[109] = "ldiv";
            mnem[110] = "fdiv";
            mnem[111] = "ddiv";
            mnem[112] = "imod";
            mnem[113] = "lmod";
            mnem[114] = "fmod";
            mnem[115] = "dmod";
            mnem[116] = "ineg";
            mnem[117] = "lneg";
            mnem[118] = "fneg";
            mnem[119] = "dneg";
            mnem[120] = "ishl";
            mnem[121] = "lshl";
            mnem[122] = "ishr";
            mnem[123] = "lshr";
            mnem[124] = "iushr";
            mnem[125] = "lushr";
            mnem[126] = "iand";
            mnem[127] = "land";
            mnem[128] = "ior";
            mnem[129] = "lor";
            mnem[130] = "ixor";
            mnem[131] = "lxor";
            mnem[132] = "iinc";
            mnem[133] = "i2l";
            mnem[134] = "i2f";
            mnem[135] = "i2d";
            mnem[136] = "l2i";
            mnem[137] = "l2f";
            mnem[138] = "l2d";
            mnem[139] = "f2i";
            mnem[140] = "f2l";
            mnem[141] = "f2d";
            mnem[142] = "d2i";
            mnem[143] = "d2l";
            mnem[144] = "d2f";
            mnem[145] = "int2byte";
            mnem[146] = "int2char";
            mnem[147] = "int2short";
            mnem[148] = "lcmp";
            mnem[149] = "fcmpl";
            mnem[150] = "fcmpg";
            mnem[151] = "dcmpl";
            mnem[152] = "dcmpg";
            mnem[153] = "ifeq";
            mnem[154] = "ifne";
            mnem[155] = "iflt";
            mnem[156] = "ifge";
            mnem[157] = "ifgt";
            mnem[158] = "ifle";
            mnem[159] = "if_icmpeq";
            mnem[160] = "if_icmpne";
            mnem[161] = "if_icmplt";
            mnem[162] = "if_icmpge";
            mnem[163] = "if_icmpgt";
            mnem[164] = "if_icmple";
            mnem[165] = "if_acmpeq";
            mnem[166] = "if_acmpne";
            mnem[167] = "goto_";
            mnem[168] = "jsr";
            mnem[169] = "ret";
            mnem[170] = "tableswitch";
            mnem[171] = "lookupswitch";
            mnem[172] = "ireturn";
            mnem[173] = "lreturn";
            mnem[174] = "freturn";
            mnem[175] = "dreturn";
            mnem[176] = "areturn";
            mnem[177] = "return_";
            mnem[178] = "getstatic";
            mnem[179] = "putstatic";
            mnem[180] = "getfield";
            mnem[181] = "putfield";
            mnem[182] = "invokevirtual";
            mnem[183] = "invokespecial";
            mnem[184] = "invokestatic";
            mnem[185] = "invokeinterface";
            mnem[186] = "newfromname";
            mnem[187] = "new_";
            mnem[188] = "newarray";
            mnem[189] = "anewarray";
            mnem[190] = "arraylength";
            mnem[191] = "athrow";
            mnem[192] = "checkcast";
            mnem[193] = "instanceof_";
            mnem[194] = "monitorenter";
            mnem[195] = "monitorexit";
            mnem[196] = "wide";
            mnem[197] = "multianewarray";
            mnem[198] = "if_acmp_null";
            mnem[199] = "if_acmp_nonnull";
            mnem[200] = "goto_w";
            mnem[201] = "jsr_w";
            mnem[202] = "breakpoint";
        }
        private Mneumonics() {
        }
    }
}

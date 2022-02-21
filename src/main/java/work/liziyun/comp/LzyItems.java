package work.liziyun.comp;

import work.liziyun.code.*;
import work.liziyun.code.symbol.LzySymbol;
import work.liziyun.code.symbol.LzyVarSymbol;
import work.liziyun.code.type.LzyMethodType;
import work.liziyun.code.type.LzyType;
import work.liziyun.code.type.LzyTypeTags;
import work.liziyun.tag.LzyByteCodes;
import work.liziyun.tag.LzyFlags;

/**
 * 作者: 李滋芸
 *      所有的可寻址实体
 */
public class LzyItems implements LzyByteCodes, LzyTypeTags {
    // 常量池
    LzyPool pool;
    // 字节码
    LzyCode code;
    // 符号表
    LzySymtab symtab;
    // Void可寻址实体
    private final LzyItems.Item voidItem;
    // this可寻址实体
    private final LzyItems.Item thisItem;
    // super可寻址实体
    private final LzyItems.Item superItem;
    // 可寻址实体
    private final LzyItems.Item[] stackItem = new LzyItems.Item[9];

    LzyItems.CondItem makeCondItem(){
        return this.makeCondItem();
    }

    LzyItems.Item makeStackItem(LzyType type) {
        return this.stackItem[LzyCode.typecode(type)];
    }


    public LzyItems(LzyPool pool, LzyCode code, LzySymtab symtab) {
        this.pool = pool;
        this.code = code;
        this.symtab = symtab;
        // void可寻址实体
        this.voidItem = new Item(LzyByteCodes.VOIDcode) {
            @Override
            public String toString() {
                return "void";
            }
        };
        // this可寻址实体
        this.thisItem = new LzyItems.SelfItem(false);
        this.superItem = new LzyItems.SelfItem(true);
        // 初始化栈可寻址实体: int , long , float , double , object , byte , char , short , void
        // 总共9个基本数据类型的栈可寻址实体
        for (int i = 0; i < 8; i++) {
            this.stackItem[i] = new LzyItems.StackItem(i);
        }
        this.stackItem[8] = this.voidItem;
    }

    /**
     * 可寻址实体
     */
    abstract class Item{
        // 字节码指令
        int typecode;
        // 宽度
        int width(){return 0;}
        // 丢弃当前实体
        void drop(){}
        // 复制栈顶项
        void duplicate(){}
        // 复制栈顶项,插入到当前实体的下面
        void stash(int toscode){
            LzyItems.this.stackItem[toscode].duplicate();
        }

        public Item(int typecode) {
            this.typecode = typecode;
        }

        LzyItems.CondItem mkCond(){
            this.load();
            return LzyItems.this.makeCondItem(ifne);
        }

        LzyItems.Item load(){
            throw new AssertionError();
        }

        LzyItems.Item store() {
            throw new AssertionError("store unsupported: " + this);
        }

        LzyItems.Item invoke() {
            throw new AssertionError(this.toString());
        }


        /**
         * 生成强制转换
         * @param targetTypeCode
         * @return
         */
        LzyItems.Item coerce(int targetTypeCode) {
            // 当前类型 和 目标类型: 一样
            if (this.typecode == targetTypeCode) {
                return this;
            } else {
                this.load();
                // 如果是byte,char,short。那么都将转换成int
                int toTypecode  = LzyCode.truncate(this.typecode); // 真实类型 ---> 来自语法树节点
                // 如果byte,char,short.那么都将转化成int
                int toTargetTypeCode = LzyCode.truncate(targetTypeCode); // 匹配到类型 ---> 来自方法中符号
                //
                if (toTypecode  != toTargetTypeCode) {
                    int var4 = toTargetTypeCode > toTypecode ? toTargetTypeCode - 1 : toTargetTypeCode;

                    LzyItems.this.code.emitop(i2l + toTypecode * 3 + var4);
                }
                // int转byte:
                if (targetTypeCode != toTargetTypeCode) {
                    LzyItems.this.code.emitop(int2byte + targetTypeCode - 5);
                }

                return LzyItems.this.stackItem[targetTypeCode];
            }
        }


        public abstract String toString();

        LzyItems.Item coerce(LzyType var1) {
            return this.coerce(LzyCode.typecode(var1));
        }
    }

    /**
     * 赋值语句可寻址实体
     */
    class AssignItem extends LzyItems.Item {
        LzyItems.Item lhs;

        public AssignItem(LzyItems.Item item) {
            super(item.typecode);
            this.lhs = item;
        }

        int width(){return this.lhs.width()+LzyCode.width(this.typecode);}

        void drop(){ this.lhs.store(); }

        LzyItems.Item load(){
            // 复制栈顶数据: 基本数据类型
            this.lhs.stash(this.typecode);
            // 存储到局部变量表中
            this.lhs.store();
            // 复制栈顶数据
            return LzyItems.this.stackItem[this.typecode];
        }

        void duplicate(){
            this.load().duplicate();
        }

        void stash(int i){
            assert  false;
        }

        @Override
        public String toString() {
            return "assign(lhs = " + this.lhs + ")";
        }
    }

    LzyItems.Item makeAssignItem(LzyItems.Item item) {
        return new LzyItems.AssignItem(item);
    }


    /**
     * 常量可寻址实体
     */
    class ImmediateItem extends LzyItems.Item{

        // 常量值
        Object value;

        public ImmediateItem(LzyType type,Object value) {
            super(LzyCode.typecode(type));
            this.value = value;
        }

        /**
         * ldc指令: 操作较大的常量
         * 作用:
         *      1. 将常量存储到常量池中
         *      2. 常量加载到操作数栈上
         * 分情况讨论:
         *      1. 常量是long或double
         *          将占用两个局部变量表中的位置,使用ldc2_w指令.加载两个项到操作数栈上
         *      2. 常量非long和double
         *          a. 局部变量表下标在255范围内,操作数1字节可以表示
         *              ldc1指令
         *          b. 局部变量表下标大于255范围内,操作数2字节才可以表示
         *              ldc2指令
         */
        private void  ldc(){
            // 放入常量池，返回下标
            int idx = pool.put(value);

            if (typecode != LzyByteCodes.LONGcode && typecode != LzyByteCodes.DOUBLEcode ){
                // 指令+操作数
                // 操作数是1字节地址，最大只能表示到255
                if (idx <= 255){
                    code.emitop(ldc1, 1);
                    code.emit1(idx);
                }else{// 操作数是2字节地址
                    code.emitop(ldc2, 1);
                    code.emit2(idx);
                }
            }else{
                // 操作数是long或double的专用指令
                // 由于占用两个局部变量表常量项的位置： 所以栈大小改变2
                code.emitop(ldc2w, 2);
                code.emit2(idx);
            }
        }


        /**
         * 作用: 操作较小的常量,加载到操作数栈上
         *
         * 什么是较小的常量?
         *      1. int byte short char
         *              a. -1到5 ---> iconst_x
         *              b. byte范围 ---> bipush
         *              c. short范围 ---> sipush
         *      2. long
         *              0 或 1
         *      3. float
         *              0.0 或 1.0 或 2.0
         *      4. double
         *              0.0 或 1.0 或 2.0
         *
         * @return
         */
        Item load(){
            switch (typecode){
                // int类型的指令
                case INTcode:case BYTEcode:case SHORTcode:case CHARcode:
                    // int类型的常量值: 可以充当偏移量
                    int ival = ((Number)value).intValue();
                    // 常量值的范围： [-1,5]
                    if ( -1 <= ival && ival <= 5 ){
                        code.emitop(iconst_0+ival);
                    }else if (Byte.MIN_VALUE <= ival && ival <= Byte.MAX_VALUE){ // 常量值的范围： [-128,127]
                        code.emitop1(bipush,ival);
                    }else if (Short.MIN_VALUE <= ival && ival <= Short.MAX_VALUE){
                        code.emitop2(sipush,ival);
                    }else {
                        // 数字较大将使用常量池存储
                        ldc();
                    }
                    break;
                case LONGcode:
                    long lval = ((Number)value).longValue();
                    // 只有0和1才使用指令
                    if (lval == 0 || lval == 1){
                        code.emitop(lconst_0+(int)lval);
                    }else {
                        // 基与常量池
                        ldc();
                    }
                    break;
                case FLOATcode:
                    float fval = ((Number)value).floatValue();
                    if ( isPosZero(fval) || fval == 1.0f || fval == 2.0f ){
                        code.emitop(fconst_0+(int)fval);
                    }else {
                        ldc();
                    }
                case DOUBLEcode:
                    double dval = ((Number)value).doubleValue();
                    if ( isPosZero(dval) || dval == 1.0 ){
                        code.emitop(dconst_0+(int)dval);
                    }else {
                        ldc();
                    }
                    break;
                case OBJECTcode:
                    ldc();
                    break;
            }
            return stackItem[typecode];
        }

        /**
         * 作用: 将常量可寻址中的类型进行强制转换
         * 思想: 对ImmediateItem常量可寻址实体的二次封装
         *
         * 例如: targetcode强转目标类型
         *      1. 类型转换
         *      2. 常量值强制转换
         * @param targetcode 强制的目标类型
         * @return
         */
        LzyItems.Item coerce(int targetcode){
            // 当前类型和强制类型一致: 无需处理
            if (typecode == targetcode){
                return this;
            }else {// 当前类型和强转类型不一致: 转换封装
                switch (targetcode){
                    case INTcode:
                        // byte,char,short都将按照int类型处理
                        if ( LzyCode.truncate(this.typecode) == INTcode ){
                            return this;
                        }
                        return new ImmediateItem( LzyItems.this.symtab.intType,new Integer( ((Number)this.value).intValue() ) );
                    case LONGcode:
                        return new ImmediateItem( LzyItems.this.symtab.longType, new Long( ((Number)this.value).longValue() ) );
                    case FLOATcode:
                        return new ImmediateItem( LzyItems.this.symtab.floatType, new Float( ((Number)this.value).floatValue() ) );
                    case DOUBLEcode:
                        return new ImmediateItem( LzyItems.this.symtab.doubleType, new Double( ((Number)this.value).doubleValue() ) );
                    case BYTEcode:
                        return new ImmediateItem( LzyItems.this.symtab.byteType, new Integer( (byte)((Number)this.value).intValue() ) );
                    case CHARcode:
                        return new ImmediateItem( LzyItems.this.symtab.charType, new Integer( (char)((Number)this.value).intValue() ) );
                    case SHORTcode:
                        return new ImmediateItem( LzyItems.this.symtab.shortType, new Integer( (short)((Number)this.value).intValue() ) );
                    default:
                        return super.coerce(targetcode);
                }
            }
        }

        @Override
        public String toString() {
            return "immediate(" + this.value + ")";
        }

        /** Return true iff float number is positive 0.
         */
        private boolean isPosZero(float x) {
            return x == 0.0f && 1.0f / x > 0.0f;
        }
        /** Return true iff double number is positive 0.
         */
        private boolean isPosZero(double x) {
            return x == 0.0d && 1.0d / x > 0.0d;
        }

        /**
         * 创建条件可寻址实体
         *      对常量值判断
         *      1. 0表示不跳转
         *      2. 非0表示跳转
         * @return
         */
        CondItem mkCond(){
            int ival = ((Number)value).intValue();
            // 0表示不跳转,1表示跳转
            return LzyItems.this.makeCondItem(ival!=0?goto_:dontgoto);
        }
    }

    CondItem makeCondItem(int opcode){
        return makeCondItem(opcode,null,null);
    }

    /**
     * 局部变量可寻址实体
     */
    class LocalItem extends LzyItems.Item{
        // 变量在局部变量中的位置
        int reg;
        // 变量的类型
        LzyType type;

        /**
         *
         * @param type 局部变量的类型
         * @param index 在局部变量表的位置
         */
        public LocalItem(LzyType type,int index) {
            super(LzyCode.typecode(type));
            assert index >= 0;
            this.type = type;
            this.reg = index;
        }

        public String toString(){
            return "localItem(type="+this.type+";reg="+this.reg+")";
        }

        /**
         * 作用: 加法 和 减法
         *
         * 分情况讨论:
         *      1. int类型： 局部变量表上直接加减
         *      2. 非int类型： 加载到操作数栈上加减，结果存储到局部变量表中
         * @param num
         */
        void incr(int num){
            // 操作的是int类型加减
            if (this.typecode == LzyByteCodes.INTcode){
                // 创建字节码指令: 基于局部变量表完成加减
                LzyItems.this.code.emitop1w(LzyByteCodes.iinc,this.reg);
                // 局部变量表下标是否大于255
                if (this.reg > 255){
                    // 两字节操作数
                    LzyItems.this.code.emit2(num);
                }else {
                    // 一字节操作数
                    LzyItems.this.code.emit1(num);
                }
            }else {
                // 第一个数据进入操作数栈
                load();
                // 加法操作
                if ( num >= 0 ){
                    // 第二个数据进入操作数栈
                    LzyItems.this.makeImmediateItem(  LzyItems.this.symtab.intType,new Integer(num)  ).load();
                    // 栈顶两个数据相加
                    LzyItems.this.code.emitop(iadd);
                }else {// 减法操作
                    // 第二个数据进入操作数栈
                    LzyItems.this.makeImmediateItem(  LzyItems.this.symtab.intType,new Integer(-num)  ).load();
                    // 栈顶两个数据相减
                    LzyItems.this.code.emitop(isub);
                }
                // 类型转换： int类型 和 目标类型，尝试生成类型转换的字节码指令
                LzyItems.this.makeStackItem(LzyItems.this.symtab.intType).coerce(this.typecode);
                // 存储到局部变量表中
                this.store();
            }
        }

        /**
         * 作用: 局部变量表中数据加载到操作数栈上
         *      1. 局部变量表索引1到3间: iload_0 + 偏移量 ---> 无操作数指令
         *      2. 局部变量表索引大于3: iload + 操作数索引
         * @return
         */
        LzyItems.Item load(){
            // 如果变量在局部变量表中的位置没有超过3： 使用优化指令
            if ( this.reg <= 3 ){
                // 偏移量计算: truncate()方法是在将byte,char,short的偏移量都当int处理
                LzyItems.this.code.emitop(iload_0+LzyCode.truncate(this.typecode)*4+this.reg );
            }else {
                LzyItems.this.code.emitop1w(iload+LzyCode.truncate(this.typecode),this.reg);
            }
            // 返回基本数据类型的栈可寻址实体
            return LzyItems.this.stackItem[this.typecode];
        }


        /**
         * 作用: 操作数栈上数据存储到局部变量表中
         *      1. 局部变量表索引1到3间: istore_0 + 偏移量 ---> 无操作数指令
         *      2. 局部变量表索引大于3: istore + 操作数索引
         * @return
         */
        LzyItems.Item store(){
            // 如果变量在局部变量表中的位置没有超过3： 使用优化指令
            if (this.reg <= 3){
                // 偏移量计算: truncate()方法是在将byte,char,short的偏移量都当int处理
                LzyItems.this.code.emitop(istore_0+LzyCode.truncate(typecode)*4+reg);
            }else {
                // 偏移量计算: truncate()方法是在将byte,char,short的偏移量都当int处理
                LzyItems.this.code.emitop1w(istore+LzyCode.truncate(typecode),reg);
            }
            return LzyItems.this.voidItem;
        }
    }


    /**
     * 创建一个局部变量的可寻址实体
     * @param type 变量类型
     * @param reg 局部变量在局部变量表中位置
     * @return
     */
    LzyItems.Item makeLocalItem(LzyType type,int reg){
        return new LzyItems.LocalItem(type,reg);
    }

    LzyItems.Item makeLocalItem(LzyVarSymbol varSymbol){
        return new LzyItems.LocalItem( varSymbol.type , varSymbol.adr );
    }

    LzyItems.Item makeThisItem(){
        return this.thisItem;
    }

    LzyItems.Item makeSuperItem(){
        return this.superItem;
    }

    /**
     * 条件可寻址实体
     */
    class CondItem extends LzyItems.Item{

        LzyCode.Chain trueJumps;
        LzyCode.Chain falseJumps;
        int opcode;


        public CondItem(int opcode, LzyCode.Chain trueJumps, LzyCode.Chain falseJumps) {
            super(LzyByteCodes.BYTEcode);
            this.opcode = opcode;
            this.trueJumps = trueJumps;
            this.falseJumps = falseJumps;
        }

        int width(){
            return -LzyCode.stackdiff[this.opcode];
        }


        void drop(){
            this.load().drop();;
        }

        void duplicate(){
            this.load().duplicate();
        }



        boolean isFalse() {
            return this.trueJumps == null && this.opcode == LzyByteCodes.dontgoto;
        }

        boolean isTrue() {
            return this.falseJumps == null && this.opcode == LzyByteCodes.goto_;
        }

        LzyCode.Chain jumpFalse(){
            return LzyCode.mergeChains(this.falseJumps,LzyItems.this.code.branch( LzyCode.negate(this.opcode) ));
        }

        LzyCode.Chain jumpTrue(){
            return LzyCode.mergeChains(this.trueJumps,LzyItems.this.code.branch(this.opcode));
        }

        LzyItems.CondItem mkCond(){
            return this;
        }

        /**
         * 条件可寻址实体取反:
         *      1. 比较条件取反
         *      2. true变false
         *      3. false变true
         * @return
         */
        LzyItems.CondItem negate(){
            LzyCode code = LzyItems.this.code;
            CondItem condItem = new CondItem(LzyCode.negate(this.opcode), this.falseJumps, this.trueJumps);

            return condItem;
        }

        LzyItems.Item load(){
            LzyCode.Chain trueChain = null;
            LzyCode.Chain falseChain = this.jumpFalse();
            if ( this.trueJumps != null || this.opcode != dontgoto  ){
                // 回填成功跳转
                LzyItems.this.code.resolve(this.trueJumps);
                //
                LzyItems.this.code.emitop(iconst_1);
                // 成功后强制跳转
                trueChain = LzyItems.this.code.branch(goto_);
            }
            if ( falseChain != null){
                // 回填失败跳转
                LzyItems.this.code.resolve(falseChain);
                LzyItems.this.code.emitop(iconst_0);
            }
            LzyItems.this.code.resolve(trueChain);
            return LzyItems.this.stackItem[typecode];
        }


        @Override
        public String toString() {
            return "cond("+LzyCode.mnem(this.opcode)+")";
        }
    }

    LzyItems.CondItem makeCondItem(int opcode, LzyCode.Chain chain1 , LzyCode.Chain chain2){
        return new LzyItems.CondItem(opcode,chain1,chain2);
    }

    LzyItems.Item makeImmediateItem(LzyType type,Object value){
        return new LzyItems.ImmediateItem(type,value);
    }

    /**
     * 下标可寻址实体
     */
    class IndexItem extends LzyItems.Item {

        public IndexItem(LzyType type) {
            super(LzyCode.typecode(type));
        }

        int width(){
            return 2;
        }

        /**
         * 弹出栈顶两个数据
         */
        void drop(){
            LzyItems.this.code.emitop(pop2);
        }

        /**
         * 复制栈顶两个数据
         */
        void duplicate() {
            LzyItems.this.code.emitop(dup2);
        }

        /**
         * 参数toscode目的确定类型宽度
         *      int的宽度1
         *      long的宽度2
         *      float的宽度1
         *      double的宽度2
         *
         * 宽度1时: dup_x2
         *      复制栈顶一个数据,插入到栈顶第三个元素下面
         * 宽度2时: dup2_x2
         *      复制栈顶两个数据,插入栈顶第三个元素下面
         *
         * 例如: dup_x2
         *   栈(右为顶部): this , 43
         *   执行dup_x2
         *   栈(右为顶部): 43 this 43
         *
         * @param toscode
         */
        void stash(int toscode){
            LzyItems.this.code.emitop(dup_x2 + 3 *  ( LzyCode.width(toscode)-1 ) );
        }

        /**
         * 参考: LzyByteCode.java
         *
         * 加载相关的字节码指令: 将局部变量表中数据加载到操作数栈上
         * 注意: this.typecode类型标记,可以当作偏移量使用
         *      int     --> 0
         *      long    --> 1
         *      float   --> 2
         *      double  --> 3
         *      object  --> 4
         * 基址: iaload
         * 基址+偏移:
         *      iaload + int    = 46 + 0 = 46 结果iload
         *      iaload + long   = 46 + 1 = 47 结果laload
         *      iaload + float  = 46 + 2 = 48 结果faload
         *      iaload + double = 46 + 3 = 49 结果daload
         *      iaload + object = 46 + 4 = 50 结果aaload
         * @return
         */
        LzyItems.Item load(){
            LzyItems.this.code.emitop(iaload+this.typecode);
            return LzyItems.this.voidItem;
        }
        /**
         * 参考: LzyByteCode.java
         *
         * 加载相关的字节码指令: 将局部变量表中数据加载到操作数栈上
         * 注意: this.typecode类型标记,可以当作偏移量使用
         *      int     --> 0
         *      long    --> 1
         *      float   --> 2
         *      double  --> 3
         *      object  --> 4
         * 基址: iastore
         * 基址+偏移:
         *      iastore + int    = 79 + 0 = 79 结果iastore
         *      iastore + long   = 79 + 1 = 80 结果lastore
         *      iastore + float  = 79 + 2 = 81 结果fastore
         *      iastore + double = 79 + 3 = 82 结果dastore
         *      iastore + object = 79 + 4 = 83 结果aastore
         * @return
         */
        LzyItems.Item store(){
            LzyItems.this.code.emitop(iastore+this.typecode);
            return LzyItems.this.voidItem;
        }

        @Override
        public String toString() {
            return "indexed("+LzyByteCodes.typecodeNames[this.typecode]+")";
        }

    }

    LzyItems.Item makeIndexedItem(LzyType type){
        return new LzyItems.IndexItem(type);
    }


    /**
     * 栈可寻址实体
     *
     * 作用:
     *      1. 存储类型作强制转换
     *      2. 存储类型作弹栈操作
     *      3. 存储类型作复制栈顶中数据操作
     */
    class StackItem extends LzyItems.Item {
        int width(){ return LzyCode.width(this.typecode); }

        void drop(){ LzyItems.this.code.emitop( this.width()==2?LzyByteCodes.pop2:LzyByteCodes.pop ); }

        void duplicate(){ LzyItems.this.code.emitop( this.width() == 2 ? LzyByteCodes.dup2:LzyByteCodes.dup ); }

        void stash(int typecode){ LzyItems.this.code.emitop( (this.width()==2?LzyByteCodes.dup_x2:LzyByteCodes.dup2_x1)+3*( LzyCode.width(typecode)-1 )  ); }

        public StackItem(int typecode) {
            super(typecode);
        }

        LzyItems.Item load(){return this;}

        public String toString(){
            return "stack(" +LzyByteCodes.typecodeNames[this.typecode] +")";
        }
    }


    /**
     * 静态可寻址实体
     */
    class StaticItem extends LzyItems.Item {

        LzySymbol member;

        public StaticItem(LzySymbol symbol) {
            super(LzyCode.typecode(symbol.type));
            this.member = symbol;
        }


        /**
         * 作用: 调用静态方法
         * 操作数栈大小改变公式: 返回值宽度 - 形式参数宽度
         *
         * 公式原理:
         *      调用方法,消耗形式参数,生成返回值.
         *      最终的宽度差: 返回值宽度 - 形式参数宽度
         *      即,调用方法的栈改变大小.
         * 例如:
         *      public static int max(int a,int b){
         *          return a+b;
         *      }
         *
         *      计算栈改变大小: max(1,2)
         *          1-2 = -1
         *
         * @return
         */
        LzyItems.Item invoke(){
            // 方法类型
            LzyType methodType = member.type;
            // 方法所有参数的宽度总和
            int argWidth = LzyCode.width(methodType.argtypes());
            // 方法的返回值的宽度
            int returnOpcode = LzyCode.typecode(methodType.restype());
            // 参数宽度 和 返回值宽度 的差值
            int stackChangeSize = LzyCode.width(returnOpcode) - argWidth;
            // 静态方法的调用指令invokestatic,操作数: 参数宽度和返回值宽度的差值
            LzyItems.this.code.emitop(invokestatic,stackChangeSize);
            // 方法存入常量池
            int index = LzyItems.this.pool.put(this.member);
            // 常量池中的下标放入操作数栈中
            LzyItems.this.code.emit2( index );
            return LzyItems.this.stackItem[returnOpcode];
        }

        /**
         * 作用: 获取静态字段. 将静态字段加载到操作数栈上
         * @return
         */
        LzyItems.Item load(){
            // 获取静态的内容: getstatic
            // 第二个参数: 操作数栈的大小变化量
            LzyItems.this.code.emitop(  getstatic , LzyCode.width(this.typecode)  );
            // 存储到常量池中,返回在常量池中的索引
            int index = LzyItems.this.pool.put(this.member);
            // 将常量池中索引加载到操作数栈中
            LzyItems.this.code.emit2(index);
            // 返回操作数栈的可寻址实体: void类型
            return LzyItems.this.stackItem[this.typecode];
        }

        /**
         * 作用: 设置静态字段的值
         * @return
         */
        LzyItems.Item store(){
            // 存储静态的内容: putstatic
            // 第二个参数: 操作数栈的大小变化量
            LzyItems.this.code.emitop( putstatic , -LzyCode.width(this.typecode)  );
            // 存储到常量池中,返回值中的索引
            int index = LzyItems.this.pool.put(this.member);
            // 将常量池中索引加载到操作数栈中
            LzyItems.this.code.emit2( index );
            // 返回操作数栈的可寻址实体: void类型
            return LzyItems.this.voidItem;
        }

        @Override
        public String toString() {
            return "static("+this.member+")";
        }
    }

    LzyItems.Item makeStaticItem(LzySymbol symbol) {
        return new LzyItems.StaticItem(symbol);
    }


    /**
     * 自身可寻址实体
     */
    class SelfItem extends LzyItems.Item {
        boolean isSuper;

        SelfItem(boolean isSuper){
            super(LzyByteCodes.OBJECTcode);
            this.isSuper = isSuper;
        }


        /**
         * 作用: 将thihs加载到操作数栈中,既局部变量表下标为0的位置
         * @return
         */
        LzyItems.Item load(){
            // 加载局部变量表0位置: 将指令压入操作数栈中
            LzyItems.this.code.emitop(LzyByteCodes.aload_0);
            // 这里即LzyByteCodes.OBJECTcode，既4
            return LzyItems.this.stackItem[this.typecode];
        }

        @Override
        public String toString() {
            return this.isSuper?"super":"this";
        }
    }


    /**
     * 成员可寻址实体
     */
    class MemberItem extends LzyItems.Item {
        LzySymbol member;
        // 是否非虚拟的: private
        boolean nonvirtual;

        public MemberItem(LzySymbol symbol,boolean nonvirtual) {
            super(LzyCode.typecode(symbol.type));
            this.member = symbol;
            // 是否非虚拟机的
            this.nonvirtual = nonvirtual;
        }

        // 对成员方法的调用
        LzyItems.Item invoke(){
            // 方法类型
            LzyMethodType methodType = (LzyMethodType) this.member.type;
            // 参数宽度
            int argWidth = LzyCode.width(methodType.argtypes);
            // 返回值的类型的字节码指令
            int retuenByteCodes = LzyCode.typecode(methodType.restype);
            // 返回值的宽度 和 参数列表宽度 的差值
            int changeStackSize = LzyCode.width(retuenByteCodes) - argWidth;
            // 如果方法是一个接口
            if ( (this.member.owner.flags() & LzyFlags.INTERFACE) != 0   ){
                // 调用接口指令
                LzyItems.this.code.emitop(invokeinterface,changeStackSize-1);
                // 方法存储到常量池中
                int index = LzyItems.this.pool.put(this.member);
                // 下标加载到操作数栈上
                LzyItems.this.code.emit2(index);
                // 参数宽度+1加载到操作数栈上 ---> 这个1是this的宽度. 方法形式参数所有的类型宽度
                LzyItems.this.code.emit1(argWidth+1);
                // 0加载到操作数栈上 ---> 历史遗留,提供扩充机制
                LzyItems.this.code.emit1(0);
            }else if (this.nonvirtual){// 1. 构造方法<init>  2.private的方法 3.super父类方法
                // 调用invokespecial字节码指令
                LzyItems.this.code.emitop(invokespecial,changeStackSize-1);
                // 方法加载到常量池中
                int index = LzyItems.this.pool.put(this.member);
                // 常量池下标加载到操作数栈上
                LzyItems.this.code.emit2(index);
            }else{
                // 调用invokevirtual字节码指令
                LzyItems.this.code.emitop(invokevirtual,changeStackSize-1);
                // 方法加载到常量池中
                int index = LzyItems.this.pool.put(this.member);
                // 常量池中下标加载到操作数栈
                LzyItems.this.code.emit2(index);
            }
            return LzyItems.this.stackItem[retuenByteCodes];
        }

        LzyItems.Item load(){
            // 调用getfeld字节码指令
            LzyItems.this.code.emitop(getfield,LzyCode.width(this.typecode)-1 );
            // 方法加入到常量池中
            int index = LzyItems.this.pool.put(this.member);
            // 下标加载到操作数栈中
            LzyItems.this.code.emit2( index );
            return LzyItems.this.stackItem[this.typecode];
        }


        LzyItems.Item store(){
            // 调用putfield字节码指令
            // 栈深度变化: 减小
            LzyItems.this.code.emitop( putfield , -LzyCode.width(this.typecode)  );
            // 方法加入到常量中
            int index = LzyItems.this.pool.put(this.member);
            // 下标加载到操作数栈中
            LzyItems.this.code.emit2(index);
            return LzyItems.this.voidItem;
        }

        // 宽度
        int width() {
            return 1;
        }
        // 销毁 --> 生成pop指令,可能是pop或pop2.根据类型宽度决定
        void drop() {
            LzyItems.this.stackItem[4].drop();
        }
        // 复制一份 --> 生成dup指令,可能是dup或dup2.根据类型宽度决定
        void duplicate() {
            LzyItems.this.stackItem[4].duplicate();
        }

        // 复制一份插入到下面 --> 生成dup_x2相关指令
        void stash(int var1) {
            LzyItems.this.stackItem[4].stash(var1);
        }


        @Override
        public String toString() {
            return "member(" + this.member + (this.nonvirtual ? " nonvirtual)" : ")");
        }

    }

    LzyItems.Item makeMemberItem(LzySymbol symbol, boolean isInit) {
        return new LzyItems.MemberItem( symbol,isInit );
    }



}

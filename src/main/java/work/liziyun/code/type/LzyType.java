package work.liziyun.code.type;


import work.liziyun.code.symbol.LzySymbol;
import work.liziyun.code.symbol.LzyTypeSymbol;
import work.liziyun.tag.LzyFlags;
import work.liziyun.tag.LzyKinds;
import work.liziyun.util.LzyList;

public class LzyType implements LzyFlags, LzyKinds,LzyTypeTags {

    // 常量的类型: 没有类型
    public static final LzyType noType ;
    // Type的标识
    public int tag;
    // Type的符号: 1. 包符号 2.类符号
    public LzyTypeSymbol tsym;
    // 空数组
    public static final LzyList emptyList;
    // 常量值
    public Object constValue = null;
    static {
        noType = new LzyType(LzyTypeTags.NONE,null);
        emptyList = LzyList.nil();

    }
    // 构造方法
    public LzyType(int tag, LzyTypeSymbol tsym) {
        this.tag = tag;
        this.tsym = tsym;
    }

    public LzyType outer(){
        return null;
    }

    public int dimensions() {
        return 0;
    }


    /**
     * byte char short int 的自动转换检查
     * @param
     * @return
     */
    public boolean isAssignable(LzyType targetType){
        // 当前类型是整形，并且有初始化值. --> 范围校验!
        if ( this.tag <= LzyTypeTags.INT && this.constValue != null ){
            int value = ((Number)this.constValue).intValue();
            // 传递的类型
            switch (targetType.tag){
                // 传递的类型是Byte
                case LzyTypeTags.BYTE:
                    if ( Byte.MIN_VALUE <=  value && value <= Byte.MAX_VALUE ){
                        return true;
                    }
                    break;
                // 传递的类型是Char
                case LzyTypeTags.CHAR:
                    if ( Character.MIN_VALUE <= value && value <= Character.MAX_VALUE ){
                        return true;
                    }
                    break;
                // 传递的类型是Short
                case LzyTypeTags.SHORT:
                    if ( Short.MIN_VALUE <= value && value <= Short.MAX_VALUE ){
                        return true;
                    }
                    break;
                // 传递的类型是Int
                case LzyTypeTags.INT:
                    return true;
            }
        }
        // 类型转换校验
        return this.isSubType(targetType);
    }


    public LzyType baseType(){
        return this.constValue == null ? this:this.tsym.type;
    }


    // 接口
    public LzyList interfaces(){
        return emptyList;
    }

    // 填充: 空实现
    public void complete(){

    }


    public LzyType constType(Object constValue){
        // 基本数据类型
        if ( this.tag <= LzyTypeTags.BOOLEAN ){
            LzyType lzyType = new LzyType(this.tag, this.tsym);
            lzyType.constValue = constValue;
            return lzyType;
        }
        throw new AssertionError();
    }

    public LzyMethodType asMethodType() {
        throw new AssertionError();
    }

    public String stringValue(){
        if (this.tag == LzyTypeTags.BOOLEAN){
            return ((Integer)this.constValue) == 0?"false":"true";
        }else{
            return this.tag == LzyTypeTags.CHAR?String.valueOf(this.constValue):this.constValue.toString();
        }
    }


    //
    public LzyType asSuper(LzySymbol sym){
        return null;
    }

    // 原始类型判断： 泛型的擦除相关
    public  boolean isRaw(){
        return false;
    }

    public static boolean isRaw(LzyList LzyList){
        return false;
    }

    public static boolean isDerivedRaw(LzyType type){
        if (type.isRaw()){
            return true;
        }else if ( type.supertype()!=null && isDerivedRaw(type.supertype()) ){
            return true;
        }else if ( isDerivedRaw(type.interfaces())  ){
            return true;
        }
        return false;
    }


    /**
     * 如果所有接口中有一个true，那么整个方法返回true
     * @param LzyList
     * @return
     */
    public static boolean isDerivedRaw(LzyList LzyList){
        LzyList interfaceList = LzyList;
        while ( interfaceList.nonEmpty() && !isDerivedRaw((LzyType) LzyList.head) ){
            interfaceList = interfaceList.tail;
        }
        return interfaceList.nonEmpty();
    }


    // 常量类型的值: 是否为true
    public boolean isTrue(){
        if ( this.tag == BOOLEAN && this.constValue != null && (int)this.constValue != 0 ){
            return true;
        }
        return false;
    }

    public boolean isSuperType(LzyType lzyType) {
        return lzyType.isSubType(this);
    }
    // 继承
    public boolean isSubType(LzyType superType){
        if (this == superType){
            return true;
        }else if (superType.tag >= 18){
            // 错误类型
            return superType.isSuperType(this);
        }else if (this.tsym == superType.tsym){
            // 由于标识符的引用消除，会创建出一个新的类型Type。所以无法利用this==superType判断成功!
            return true;
        }else{
            // 判断当前符号类型
            switch (this.tag){
                case LzyTypeTags.BYTE:
                case LzyTypeTags.CHAR:
                    // 父类成立条件第一种: 相等
                    // 父类成立条件第二种: 相差至少为2，且不超过double ---> 排除short的情况
                    return this.tag == superType.tag || this.tag+2 <= superType.tag && superType.tag <= LzyTypeTags.DOUBLE;
                case LzyTypeTags.SHORT:
                case LzyTypeTags.INT:
                case LzyTypeTags.LONG:
                case LzyTypeTags.FLOAT:
                case LzyTypeTags.DOUBLE:
                    // 基本数据类型的父类
                    return this.tag <= superType.tag && superType.tag <= 7;
                case LzyTypeTags.BOOLEAN:
                case LzyTypeTags.VOID:
                    return  this.tag == superType.tag;
                // null空指针的类型
                case LzyTypeTags.BOT:
                    return superType.tag == LzyTypeTags.BOT || superType.tag == LzyTypeTags.CLASS || superType.tag == LzyTypeTags.ARRAY;
                // 引用数据类型的比较: 在LzyClassType 或者 LzyArrayType中
                default:
                    System.out.println("编译错误: isSubType应该在LzyClassType或LzyArrayType中!");
                    return false;
            }
        }
    }

    public boolean isErroneous(){
        return false;
    }

    public LzyType elemtype() {
        return null;
    }

    public LzyType restype() { return null; }

    public LzyList<LzyType> argtypes() {
        return emptyList;
    }

    public LzyList thrown() {
        return emptyList;
    }


    public LzyType supertype(){
        return null;
    }


    public LzyType memberType(LzySymbol sym){
        // 这里尝试擦除泛型
        return  sym.type;
    }

    //
    public boolean isSameType(LzyType type){
        if (this == type){
            return true;
        }else if (type.tag >= LzyTypeTags.ERROR){
            // 错误类型的处理
            return type.isSameType(this);
        }else {
            switch (this.tag) {
                case LzyTypeTags.BYTE:
                case LzyTypeTags.CHAR:
                case LzyTypeTags.SHORT:
                case LzyTypeTags.INT:
                case LzyTypeTags.LONG:
                case LzyTypeTags.FLOAT:
                case LzyTypeTags.DOUBLE:
                case LzyTypeTags.VOID:
                case LzyTypeTags.BOT:
                case LzyTypeTags.NONE:
                    return this.tag == type.tag;
                default:
                    throw new AssertionError();
            }
        }
    }


    // 方法的重写: 相同参数
    public boolean hasSameArgs(LzyType type){
        throw new AssertionError();
    }

    public static boolean isSubTypes(LzyList<LzyType> subTypeList,LzyList<LzyType> pubTypeList){
        while (subTypeList.tail != null && pubTypeList.tail != null && subTypeList.head.isSubType(pubTypeList.head) ) {
            subTypeList = subTypeList.tail;
            pubTypeList = pubTypeList.tail;
        }
        return subTypeList.tail == null && pubTypeList.tail == null;
    }

    public String toString(){
        String rs = null;
        if ( this.tsym!=null && this.tsym.name != null ){
            rs  = this.tsym.name.toString();
        }else{
            rs = "null";
        }
        return rs;
    }

}

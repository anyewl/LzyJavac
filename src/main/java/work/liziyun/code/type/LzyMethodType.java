package work.liziyun.code.type;


import work.liziyun.code.symbol.LzyTypeSymbol;
import work.liziyun.util.LzyList;

public class LzyMethodType extends LzyType implements Cloneable {
    public LzyList<LzyType> argtypes;
    public LzyType restype;
    public LzyList thrown;
    public LzyMethodType(LzyList argtypes,LzyType restype,LzyList thrown, LzyTypeSymbol tsym) {
        super(LzyTypeTags.METHOD, tsym);
        this.argtypes = argtypes;
        this.restype = restype;
        this.thrown = thrown;
    }


    public LzyType restype() {
        return this.restype;
    }

    public LzyList<LzyType> argtypes() { return this.argtypes; }


    public LzyList thrown() { return this.thrown; }
    public void setThrown(LzyList thrown) {
        this.thrown = thrown;
    }

    public LzyMethodType asMethodType(){
        return this;
    }

    public int hashCode(){
        int code = 12;
        LzyList argList = this.argtypes;
        while (argList.tail != null){
            // 疑问: 逻辑障碍，计算当前hashcode的前提是下一个节点的存在!
            code = (code<<5) + ((LzyType)argList.head).hashCode();
            // 下一个
            argList = argList.tail;
        }
        return (code << 5) + this.restype.hashCode();
    }



    // 调用出: LzyPool.Method.equals()
    public boolean equals(Object obj){
        // 一样直接返回true
        if (this == obj){
            return true;
        }else if ( !(obj instanceof LzyMethodType) ){ // 不是方法类型，直接返回false
            return false;
        }else{
            // 方法类型
            LzyMethodType methodType = (LzyMethodType) obj;
            // 目标: 方法的参数列表
            LzyList targetArgList = this.argtypes;
            // 当前: 方法的参数列表
            LzyList currentArgList = methodType.argtypes;
            // 存在的问题: 如果下一个为null,那个当前这个将不会调用equals方法
            // 例: 最后一个会错过比较的机会,因为下一个为null,那么equals方法将会短路掉
            // 辩证: 也许存在一个默认的结尾!
            while (targetArgList.tail != null && currentArgList.tail != null && ((LzyType)targetArgList.head).equals(currentArgList.head)){
                // 下一个
                targetArgList = targetArgList.tail;
                currentArgList = currentArgList.tail;
            }
            // 注意: 如果方法的参数全部都相等，那么最后一定会移动到null
            // 接着比较: 方法的返回值
            if (targetArgList.tail == null && currentArgList.tail == null){
                return this.restype.equals(methodType.restype);
            }else {
                return false;
            }
        }
    }


    // 符号填充
    public void complete(){
        // 形参列表的填充
        LzyList argTypeList = this.argtypes;
        while ( argTypeList.nonEmpty() ){
            ((LzyType)argTypeList.head).complete();
            argTypeList = argtypes.tail;
        }
        // 返回类型
        this.restype.complete();
        // 异常
        LzyList thrownList = this.thrown;
        while ( thrownList.nonEmpty() ){
            ((LzyType)thrownList.head).complete();
            thrownList = thrownList.tail;
        }
    }


    /**
     * 1. 形式参数
     * 2. 返回值类型
     * @param type
     * @return
     */
    @Override
    public boolean isSameType(LzyType type){
        return  this.hasSameArgs(type) && this.restype.isSameType(type.restype());
    }


    /**
     * 方法的形式参数相同判断
     * @param type
     * @return
     */
    @Override
    public boolean hasSameArgs(LzyType type){
        return type.tag == LzyTypeTags.METHOD && isSameTypes(this.argtypes,type.argtypes());
    }


    /**
     * 两个方法的形式参数: 相同判断
     * @param methodArgs01
     * @param methodArgs02
     * @return
     */
    public static boolean isSameTypes(LzyList methodArgs01, LzyList methodArgs02) {
        while (methodArgs01.tail != null && methodArgs02.tail !=null  ){
            ((LzyType)methodArgs01.head).isSameType((LzyType)methodArgs02.head);
            methodArgs01 = methodArgs01.tail;
            methodArgs02 = methodArgs02.tail;
        }
        return methodArgs01.tail == null && methodArgs02.tail == null;
    }


    public String toString() {
        return "(" + this.argtypes.toString() + ")" + this.restype;
    }


}

package work.liziyun.code;


import work.liziyun.code.symbol.LzyMethodSymbol;
import work.liziyun.code.symbol.LzyVarSymbol;
import java.util.HashMap;

/**
 * 作者: 李滋芸
 *      常量池
 */

interface LzyPoolType{
    int CONSTANT_Utf8_info	= 1;
    int CONSTANT_Integer_info	= 3;
    int CONSTANT_Float_info	= 4;
    int CONSTANT_Long_info =	5;
    int CONSTANT_Double_info	= 6;
    int CONSTANT_Class_info	= 7;
    int CONSTANT_String_info	= 8;
    int CONSTANT_Filedref_info	= 9;
    int CONSTANT_Methodref_info	= 10;
    int CONSTANT_InterfaceMethodref_info	= 11;
    int CONSTANT_NameAndType_info	=12;
    int CONSTANT_MethodHandle_info	=15;
    int CONSTANT_MethodType_info	=16;
    int CONSTANT_InvokeDynamic_info	=18;
}


public class LzyPool {
    public static final int MAX_ENTRIES = 65535;
    public static final int MAX_STRING_LENGTH= 65535;
    int pp;
    Object[] pool;
    HashMap<Object,Integer> indices;


    // 所有的实体个数
    public int numEntries(){
        return this.pp;
    }

    public LzyPool(){
        this(1,new Object[64]);
    }

    private void doublePool(){
        Object[] objects = new Object[this.pool.length * 2];
        System.arraycopy(this.pool,0,objects,0,this.pool.length);
        this.pool = objects;
    }


    // 重置操作
    public void reset(){
        this.pp = 1;
        this.indices.clear();
    }

    public LzyPool(int size,Object[] datas){
        this.pp = size;
        this.pool = datas;
        indices = new HashMap(datas.length);
        for (int i = 0; i < size; i++) {
            // 存在数据
            if ( datas[i] != null ){
                // key: 值 value: 下标
                this.indices.put(datas[i],new Integer(i));
            }
        }
    }


    // 获取value值: 即下标
    public int get(Object key){
        Integer index = this.indices.get(key);
        // 没有找到返回-1
        if (index == null){
            return  -1;
        }else{
            return index;
        }
    }

    // 放入数据
    public int put(Object key){
        // 方法符号
        if (key instanceof LzyMethodSymbol){
            // 封装成方法实体
            key = new LzyPool.Method((LzyMethodSymbol) key);
        }else if (key instanceof LzyVarSymbol){ // 变量符号
            key = new LzyPool.Variable((LzyVarSymbol) key);
        }
        // 获取下标
        Integer index = this.indices.get(key);
        if (index == null){
            index = new Integer(this.pp);
            this.indices.put(key,index);
            // 扩容
            if (this.pp == this.pool.length){
                this.doublePool();
            }
            // 存储
            this.pool[this.pp++] = key;
            // 如果是数字，那么存储空
            if ( key instanceof Long || key instanceof Double  ){
                if (this.pp == this.pool.length){
                    this.doublePool();
                }
                this.pool[this.pp++] = null;
            }
        }
        return index;
    }

    // 常量池中的常量项: 方法
    static class Method{
        LzyMethodSymbol m;
        public int hashCode(){
            int nameCode = this.m.name.hashCode()*33;
            int ownerCode = this.m.owner.hashCode()*9;
            int typeCode = this.m.type.hashCode();
            return nameCode+ownerCode+typeCode;
        }

        public Method(LzyMethodSymbol m) {
            this.m = m;
        }
        public boolean equals(Object obj){
            // 类型不对，直接返回false
            if ( !(obj instanceof LzyPool.Method) ){
                return false;
            }else{
                // 获取常量项中的方法符号
                LzyMethodSymbol methodSymbol = ((Method) obj).m;
                // 比较三部分: 1.方法名称  2.方法的所有者  3.方法的类型Type
                if (methodSymbol.name == this.m.name && methodSymbol.owner == this.m.owner && methodSymbol.type.equals(this.m.type)){
                    return true;
                }else {
                    return false;
                }
            }
        }
    }
    // 常量池中的常量项: 变量
    static class Variable{
        LzyVarSymbol v;

        public Variable(LzyVarSymbol v) {
            this.v = v;
        }

        public int hashCode(){
            int name = this.v.hashCode()*33;
            int owner = this.v.owner.hashCode()*9;
            int type = this.v.type.hashCode();
            return name+owner+type;
        }

        public boolean equals(Object obj){
            if (!(obj instanceof LzyPool.Variable)){
                return false;
            }else{
                LzyVarSymbol varSymbol = ((Variable)obj).v;
                //比较内容: 1. 名称 2.所有者 3.类型
                return varSymbol.name == this.v.name && varSymbol.owner == this.v.owner && varSymbol.type.equals(this.v.type);
            }
        }

    }
}

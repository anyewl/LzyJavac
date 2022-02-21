package work.liziyun.world;



import work.liziyun.util.LzyContext;
import work.liziyun.util.LzyConvert;

public class LzyTable   {
    public static final LzyContext.Key tableKey = new LzyContext.Key();
    // 字节数组bytes的大小
    private int nc;
    private int hashMask;
    // 所有的Name
    private LzyName[] hashes ;
    // 字节数组
    public byte[] bytes ;

    public static LzyTable instance(LzyContext LzyContext){
        LzyTable lzyTable = (LzyTable)LzyContext.get(tableKey);
        if (lzyTable == null){
            lzyTable = new LzyTable();
            LzyContext.put(tableKey,lzyTable);
        }
        return lzyTable;
    }

    public LzyTable() {
        // Name个数: 32768
        // 字节大小: 131072
        this( 32768, 131072);
    }

    protected LzyTable(int hashSize,int nameSize) {
        this.nc = 0;
        this.hashMask = hashSize-1;
        // 词的个数
        this.hashes = new LzyName[32768];
        // 所有词的字节数组
        this.bytes = new byte[nameSize];
        // 初始化所有的词
        _null = this.fromString("null");
        slash = LzyTable.this.fromString("/");
        hyphen = LzyTable.this.fromString("-");
        T = LzyTable.this.fromString("T");
        slashequals = LzyTable.this.fromString("/=");
        deprecated = LzyTable.this.fromString("deprecated");
        init = LzyTable.this.fromString("<init>");
        clinit = LzyTable.this.fromString("<clinit>");
        error = LzyTable.this.fromString("<error>");
        any = LzyTable.this.fromString("<any>");
        emptyPackage = LzyTable.this.fromString("unnamed package");
        empty = LzyTable.this.fromString("");
        one = LzyTable.this.fromString("1");
        period =  LzyTable.this.fromString(".");
        comma = LzyTable.this.fromString(",");
        semicolon = LzyTable.this.fromString(";");
        asterisk = LzyTable.this.fromString("*");
        _this = LzyTable.this.fromString("this");
        _super = LzyTable.this.fromString("super");
        _default = LzyTable.this.fromString("default");
        _class = LzyTable.this.fromString("class");
        java_lang = LzyTable.this.fromString("java.lang");
        java_lang_Object = LzyTable.this.fromString("java.lang.Object");
        java_lang_Cloneable = LzyTable.this.fromString("java.lang.Cloneable");
        java_lang_Class = LzyTable.this.fromString("java.lang.Class");
        java_io_Serializable = LzyTable.this.fromString("java.io.Serializable");
        java_lang_Enum = LzyTable.this.fromString("java.lang.Enum");
        java_lang_invoke_MethodHandle = LzyTable.this.fromString("java.lang.invoke.MethodHandle") ;
        package_info = LzyTable.this.fromString("package-info");
        serialVersionUID = LzyTable.this.fromString("serialVersionUID");
        ConstantValue = LzyTable.this.fromString("ConstantValue");
        LineNumberTable = LzyTable.this.fromString("LineNumberTable");
        LocalVariableTable = LzyTable.this.fromString("LocalVariableTable");
        LocalVariableTypeTable = LzyTable.this.fromString("LocalVariableTypeTable");
        CharacterRangeTable = LzyTable.this.fromString("CharacterRangeTable");
        StackMap = LzyTable.this.fromString("StackMap");
        StackMapTable = LzyTable.this.fromString("StackMapTable");
        SourceID = LzyTable.this.fromString("SourceID");
        CompilationID = LzyTable.this.fromString("CompilationID");
        Code = LzyTable.this.fromString("Code");
        Exceptions = LzyTable.this.fromString("Exceptions");
        SourceFile = LzyTable.this.fromString("SourceFile");
        InnerClasses = LzyTable.this.fromString("InnerClasses");
        Synthetic = LzyTable.this.fromString("Synthetic");
        Bridge = LzyTable.this.fromString("Bridge");
        Deprecated = LzyTable.this.fromString("Deprecated");
        Enum = LzyTable.this.fromString("Enum");
        _name = LzyTable.this.fromString("name");
        Signature = LzyTable.this.fromString("Signature");
        Varargs = LzyTable.this.fromString("Varargs");
        Annotation = LzyTable.this.fromString("Annotation");
        RuntimeVisibleAnnotations = LzyTable.this.fromString("RuntimeVisibleAnnotations");
        RuntimeInvisibleAnnotations = LzyTable.this.fromString("RuntimeInvisibleAnnotations");
        RuntimeVisibleTypeAnnotations = LzyTable.this.fromString("RuntimeVisibleTypeAnnotations");
        RuntimeInvisibleTypeAnnotations = LzyTable.this.fromString("RuntimeInvisibleTypeAnnotations");
        RuntimeVisibleParameterAnnotations= LzyTable.this.fromString("RuntimeVisibleParameterAnnotations");
        RuntimeInvisibleParameterAnnotations= LzyTable.this.fromString("RuntimeInvisibleParameterAnnotations");
        Value = LzyTable.this.fromString("Value");
        EnclosingMethod= LzyTable.this.fromString("EnclosingMethod");
        desiredAssertionStatus= LzyTable.this.fromString("desiredAssertionStatus");
        append = LzyTable.this.fromString("append");
        family= LzyTable.this.fromString("family");
        forName= LzyTable.this.fromString("forName");
        toString= LzyTable.this.fromString("toString");
        length= LzyTable.this.fromString("length");
        valueOf= LzyTable.this.fromString("valueOf");
        value= LzyTable.this.fromString("value");
        getMessage= LzyTable.this.fromString("getMessage");
        getClass= LzyTable.this.fromString("getClass");
        TYPE= LzyTable.this.fromString("getClass");
        TYPE_USE= LzyTable.this.fromString("TYPE_USE");
        TYPE_PARAMETER= LzyTable.this.fromString("TYPE_PARAMETER");
        FIELD= LzyTable.this.fromString("FIELD");
        METHOD= LzyTable.this.fromString("METHOD");
        PARAMETER= LzyTable.this.fromString("PARAMETER");
        CONSTRUCTOR= LzyTable.this.fromString("CONSTRUCTOR");
        LOCAL_VARIABLE= LzyTable.this.fromString("LOCAL_VARIABLE");
        ANNOTATION_TYPE= LzyTable.this.fromString("ANNOTATION_TYPE");
        PACKAGE= LzyTable.this.fromString("PACKAGE");
        SOURCE= LzyTable.this.fromString("SOURCE");
        CLASS= LzyTable.this.fromString("CLASS");
        RUNTIME= LzyTable.this.fromString("RUNTIME");
        Array= LzyTable.this.fromString("Array");
        Method= LzyTable.this.fromString("Method");
        Bound= LzyTable.this.fromString("Bound");
        clone= LzyTable.this.fromString("clone");
        getComponentType = LzyTable.this.fromString("getComponentType");
        getClassLoader= LzyTable.this.fromString("getClassLoader");
        initCause= LzyTable.this.fromString("initCause");
        values= LzyTable.this.fromString("values");
        iterator= LzyTable.this.fromString("iterator");
        hasNext= LzyTable.this.fromString("hasNext");
        next= LzyTable.this.fromString("next");
        AnnotationDefault= LzyTable.this.fromString("AnnotationDefault");
        ordinal= LzyTable.this.fromString("ordinal");
        equals= LzyTable.this.fromString("equals");
        hashCode= LzyTable.this.fromString("hashCode");
        compareTo= LzyTable.this.fromString("compareTo");
        getDeclaringClass= LzyTable.this.fromString("getDeclaringClass");
        ex= LzyTable.this.fromString("ex");
        finalize= LzyTable.this.fromString("finalize");
        java_lang_AutoCloseable= LzyTable.this.fromString("java.lang.AutoCloseable");
        close = LzyTable.this.fromString("close");
        addSuppressed = LzyTable.this.fromString("addSuppressed");

    }


    public String getString(int start,int length){
        byte b ;
        String rs = "";
        for (int i = start; i < start+length; i++) {
               b = bytes[i];
               rs += (char)b;
        }
        return rs;
    }


    private static int hashValue(byte[] bytes,int start,int length){
        int hash = 0;
        // 如果词的长度大于0
        if (length>0){
            // hash算法: 长度*68921 +  开头 + 末尾 + 中间
            int a = length * 68921;
            int b = bytes[start] * 1681;
            int c = bytes[start+length-1]*41;
            int d = bytes[start + (length>>1)];
            return  a + b + c +d;
        }else {
            return hash;
        }
    }

    private static boolean equals(byte[] targetBytes,int targetIndex,byte[] sourceBytes ,int sourceIndex,int length){
        for (int i = 0; i < length; i++) {
            // 目标字节
            byte t = targetBytes[targetIndex+i];
            // 源文件字节
            byte s = sourceBytes[sourceIndex+i];
            if (t != s)return false;
        }
        return true;
    }


    public LzyName fromUtf(byte[] sourceBytes ){
        return this.fromUtf(sourceBytes,0,sourceBytes.length);
    }

    /**
     * 我们并不会提前放入bytes数组中
     *      根本原因: 没有调用LzyConvert的char转byte
     * 注意: 这个方法，不支持转码
     * @param sourceBytes
     * @param start
     * @param len
     * @return
     */
    public LzyName fromUtf(byte[] sourceBytes , int start ,int len ){
        // 计算hash值
        int h = hashValue(sourceBytes,start,len) & hashMask;
        // 获取Name
        LzyName  n = hashes[h];
        byte[] bytes2= this.bytes;
        // 注意: 我们并没有将比较的字节提前放入bytes中
        while ( n != null && ( n.getByteLength() != len || !equals(bytes2,n.index,sourceBytes,start,len)  ) ){
            n = n.next;
        }
        // 如果没有找到
        if ( n == null ){
            // 有效数据大小
            int nc = this.nc;
            // 超过真实大小
            while ( nc + len > bytes2.length ){
                // 扩容到原来的2倍
                byte[] newBytes = new byte[bytes2.length*2];
                // 拷贝: 数据方向newBytes
                System.arraycopy(bytes2,0,newBytes,0,bytes2.length);
                // 更新引用
                bytes2 = this.bytes = newBytes;
            }
            // sourceBytes 拷贝到 targetBytes
            System.arraycopy(sourceBytes,start,bytes2,nc,len);
            // 创建Name
            n = new LzyName(this,nc,len);
            // 连接到冲突链上
            n.next = hashes[h];
            // 放入冲突链
            hashes[h] = n;
            // 更新有效位置
            this.nc = nc + len;
            // 无效位置
            if ( len == 0  ){
                this.nc++;
            }
        }
        return n;
    }


    public LzyName fromString(String str) {
        return fromChars(str.toCharArray(),0,str.toCharArray().length);
    }


    /**
     * 我们先将chars转bytes，提前放入bytes数组中
     *      注意: 这个方法支持转码!
     * @param chars
     * @param start
     * @param length
     * @return
     */
    public LzyName fromChars(char[] chars, int start, int length) {
        // Name的个数
        int bytesSize = this.nc;
        // 扩容操作
        byte[] newBytes ;
        byte[] oldBytes ;
        oldBytes = this.bytes;
        // 尝试扩容
        while (bytesSize+length*2 >= oldBytes.length){
            // 扩大2倍
            newBytes = new byte[oldBytes.length*2];
            System.arraycopy(oldBytes,0,newBytes,0,oldBytes.length);
            // 更新状态
            oldBytes = this.bytes = newBytes;
        }
        // 字符占用的byte个数： 将char转byte存储到this.bytes中
        // 注意: 这里没有判断存储到bytes,不会造成内存空间太多浪费！因为无效数据，并不会移动nc
        int worldBytesSize = LzyConvert.chars2utf(chars, start, oldBytes, bytesSize, length)-bytesSize;
        // 计算hash
        int hashcode = hashValue(bytes, bytesSize, worldBytesSize)&hashMask;
        // 如果有值，那么尝试在链表上解决hash冲突
        LzyName lzyName = hashes[hashcode];

        // 1. 长度判断 2.内容比较
        // 注意: 这个地方一定要加小括号，防止右边产生空指针异常
        while ( lzyName!=null && ( lzyName.length != worldBytesSize || !equals(bytes,lzyName.index,bytes,bytesSize,worldBytesSize)  )  ){
            // 冲突链表上移动
            lzyName = lzyName.next;
        }

        if (lzyName == null){
            lzyName = new LzyName(this,bytesSize,worldBytesSize);
            lzyName.lzyTable = this;
            this.nc = bytesSize + worldBytesSize;
            // 放入冲突链表中
            lzyName.next = hashes[hashcode];
            hashes[hashcode] = lzyName;
            // 空词也占用一个位置
            if (worldBytesSize == 0){
                worldBytesSize++;
            }
        }
        return lzyName;
    }

    public final LzyName _null;
    public final LzyName slash;
    public final LzyName hyphen ;
    public final LzyName T ;
    public final LzyName slashequals;
    public final LzyName deprecated ;
    public final LzyName init ;
    public final LzyName clinit ;
    public final LzyName error ;
    public final LzyName any ;
    public final LzyName emptyPackage;
    public final LzyName empty;
    public final LzyName one;
    public final LzyName period ;
    public final LzyName comma;
    public final LzyName semicolon ;
    public final LzyName asterisk ;
    public final LzyName _this ;
    public final LzyName _super ;
    public final LzyName _default;
    public final LzyName _class ;
    public final LzyName java_lang ;
    public final LzyName java_lang_Object ;
    public final LzyName java_lang_Class ;
    public final LzyName java_lang_Cloneable ;
    public final LzyName java_io_Serializable ;
    public final LzyName java_lang_Enum ;
    public final LzyName java_lang_invoke_MethodHandle  ;
    public final LzyName package_info;
    public final LzyName serialVersionUID ;
    public final LzyName ConstantValue;
    public final LzyName LineNumberTable ;
    public final LzyName LocalVariableTable ;
    public final LzyName LocalVariableTypeTable;
    public final LzyName CharacterRangeTable;
    public final LzyName StackMap ;
    public final LzyName StackMapTable;
    public final LzyName SourceID ;
    public final LzyName CompilationID ;
    public final LzyName Code ;
    public final LzyName Exceptions ;
    public final LzyName SourceFile ;
    public final LzyName InnerClasses ;
    public final LzyName Synthetic ;
    public final LzyName Bridge ;
    public final LzyName Deprecated ;
    public final LzyName Enum;
    public final LzyName _name ;
    public final LzyName Signature;
    public final LzyName Varargs;
    public final LzyName Annotation;
    public final LzyName RuntimeVisibleAnnotations;
    public final LzyName RuntimeInvisibleAnnotations ;
    public final LzyName RuntimeVisibleTypeAnnotations ;
    public final LzyName RuntimeInvisibleTypeAnnotations ;
    public final LzyName RuntimeVisibleParameterAnnotations;
    public final LzyName RuntimeInvisibleParameterAnnotations;
    public final LzyName Value ;
    public final LzyName EnclosingMethod;
    public final LzyName desiredAssertionStatus;
    public final LzyName append ;
    public final LzyName family;
    public final LzyName forName;
    public final LzyName toString;
    public final LzyName length;
    public final LzyName valueOf;
    public final LzyName value;
    public final LzyName getMessage;
    public final LzyName getClass;
    public final LzyName TYPE;
    public final LzyName TYPE_USE;
    public final LzyName TYPE_PARAMETER;
    public final LzyName FIELD;
    public final LzyName METHOD;
    public final LzyName PARAMETER;
    public final LzyName CONSTRUCTOR;
    public final LzyName LOCAL_VARIABLE;
    public final LzyName ANNOTATION_TYPE;
    public final LzyName PACKAGE;
    public final LzyName SOURCE;
    public final LzyName CLASS;
    public final LzyName RUNTIME;
    public final LzyName Array;
    public final LzyName Method;
    public final LzyName Bound;
    public final LzyName clone;
    public final LzyName getComponentType ;
    public final LzyName getClassLoader;
    public final LzyName initCause;
    public final LzyName values;
    public final LzyName iterator;
    public final LzyName hasNext;
    public final LzyName next;
    public final LzyName AnnotationDefault;
    public final LzyName ordinal;
    public final LzyName equals;
    public final LzyName hashCode;
    public final LzyName compareTo;
    public final LzyName getDeclaringClass;
    public final LzyName ex;
    public final LzyName finalize;
    public final LzyName java_lang_AutoCloseable;
    public final LzyName close ;
    public final LzyName addSuppressed;

}

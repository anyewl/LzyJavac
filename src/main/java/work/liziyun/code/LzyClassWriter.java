package work.liziyun.code;


import work.liziyun.code.symbol.LzyClassSymbol;
import work.liziyun.code.symbol.LzyMethodSymbol;
import work.liziyun.code.symbol.LzySymbol;
import work.liziyun.code.symbol.LzyVarSymbol;
import work.liziyun.code.type.*;
import work.liziyun.jvm.LzyTarget;
import work.liziyun.tag.LzyFlags;
import work.liziyun.tag.LzyKinds;
import work.liziyun.util.*;
import work.liziyun.world.LzyName;
import work.liziyun.world.LzyTable;
import java.io.*;
import static work.liziyun.tag.LzyFlags.*;

class LzyWriterBuffer {
    LzyByteBuffer databuf = new LzyByteBuffer(65520);
    LzyByteBuffer poolbuf = new LzyByteBuffer(131056);
    LzyByteBuffer sigbuf = new LzyByteBuffer();
    /**
     *
     * @param byteBuffer
     * @param op 位置
     * @param x 元素
     */
    void putChar(LzyByteBuffer byteBuffer ,int op , int x ){
        // 第二个字节 --> 0xFF: 只要前8位
        byteBuffer.elems[op] = (byte)( (x >> 8) & 0xFF );
        // 第一个字节 --> 0xFF: 只要前8位
        byteBuffer.elems[op+1] = (byte) ((x)&0xFF);
    }
    /**
     *
     * @param buffer
     * @param op 位置
     * @param x 元素
     */
    void putInt(LzyByteBuffer buffer,int op , int x){
        // 第四个字节 --> 0xFF: 只要前8位
        buffer.elems[op] = (byte) ((x>>24)&0xFF);
        // 第三个字节 --> 0xFF: 只要前8位
        buffer.elems[op + 1] = (byte)( (x>>16) &0xFF  );
        // 第二个字节 --> 0xFF: 只要前8位
        buffer.elems[op + 2] = (byte)( (x>>8) & 0xFF );
        // 第一个字节 --> 0xFF: 只要前8位
        buffer.elems[op + 3] = (byte)( x & 0xFF );
    }
}

public class LzyClassWriter extends LzyWriterConstanPool {

    public static final LzyContext.Key key = new LzyContext.Key();

    public File outDir = null;

    boolean emitSourceFile = true;

    private LzyTarget target;

    private LzyClassWriter(LzyContext LzyContext){
        LzyContext.put(key,this);
        this.names = LzyTable.instance(LzyContext);
        this.target = LzyTarget.instance(LzyContext);
    }
    public static LzyClassWriter instance(LzyContext LzyContext){
        LzyClassWriter classWriter = (LzyClassWriter)LzyContext.get(key);
        if ( classWriter == null ){
            classWriter = new LzyClassWriter(LzyContext);
        }
        return classWriter;
    }

    public File outputFile( LzyClassSymbol classSymbol , String postfix ){
        // 不存在字节码文件输出路径,那么我们跟源码放在一起
        if ( this.outDir == null ){
            // 全限定名称
            String name = LzyConvert.shortName(classSymbol.flatname)+postfix;
            // 编译器调用位置生成字节码
            if ( classSymbol.sourcefile == null ){
                return new File(name);
            }else {
                // 源码路径所在的目录
                String path = (new File(classSymbol.sourcefile.toString())).getParent();
                //
                return path == null? new File(name):new File(path,name);
            }
        }else {
            // 向字节码输出路径下输出
            return this.outputFile( this.outDir  , classSymbol.flatname.toString()  , postfix );
        }
    }

    File outputFile( File path , String name , String postfix  ){
        int i = 0;
        // 创建全限定类名对应的目录
        for (int index = name.indexOf(".")   ; index >= i   ; index = name.indexOf(".",i)  ){
            path = new File( path , name.substring(i,index)  );
            if ( !path.exists() ){
                path.mkdir();
            }
            i = index + 1;
        }
        return new File(path, name.substring(i)+postfix);
    }


    public void writeClass(LzyClassSymbol classSymbol) throws IOException, StringOverflow {
        File file = this.outputFile(classSymbol, ".class");
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        try{
            this.writeClassFile(fileOutputStream,classSymbol);
        } catch (PoolOverflow poolOverflow) {
            poolOverflow.printStackTrace();
        } finally {
            if (fileOutputStream != null){
                fileOutputStream.close();;
                //file.delete();
                fileOutputStream = null;
            }
        }
    }

    public void writeClassFile(OutputStream outputStream , LzyClassSymbol classSymbol) throws StringOverflow, IOException, PoolOverflow {
        if (  (classSymbol.flags() & COMPOUND) != 0 ){
            throw new AssertionError();
        }else {
            // 重置位置
            this.databuf.reset();
            this.poolbuf.reset();
            this.sigbuf.reset();
            this.pool = classSymbol.pool;
            // 父类
            LzyType supertype = classSymbol.type.supertype();
            // 接口
            LzyList<LzyType> interfaces = classSymbol.type.interfaces();
            // 类修饰符
            long flags = classSymbol.flags();
            // 类使用protected修饰
            if ( ( flags & LzyFlags.PROTECTED ) != 0){
                // 添加修饰符public
                flags |= 1L;
            }
            // 修饰符只要: public final interface abstract strictfp 然后去掉strictfp
            flags = flags & (PUBLIC|FINAL|INTERFACE|ABSTRACT|STRICTFP) & -(STRICTFP+1) ;
            // 如果不是接口
            if ( (flags & INTERFACE) == 0 ){
                // 类的修饰符加锁
                flags |= SYNCHRONIZED ;
            }
            // 添加类修饰符
            this.databuf.appendChar( (int) flags );
            this.databuf.appendChar( this.pool.put(classSymbol) );
            // 父类
            this.databuf.appendChar( supertype.tag == LzyTypeTags.CLASS ? this.pool.put(supertype.tsym):0 );
            // 接口个数
            this.databuf.appendChar( interfaces.length() );
            // 所有的接口
            for ( LzyList<LzyType> l = interfaces ; l.nonEmpty() ;  l = l.tail ){
                this.databuf.appendChar( this.pool.put( l.head.tsym ) );
            }
            // 方法 和 字段
            int fieldsCount = 0;
            int methodsCount = 0;
            for (LzyScope.Entry e = classSymbol.members().elems ; e != null ; e = e.sibling ){
                switch (e.sym.kind){
                    case LzyKinds.TYP: // 类型
                        // 因为不存在内部类,所以不处理
                        break;
                    case LzyKinds.VAR: // 变量
                        ++fieldsCount;
                        break;
                    case LzyKinds.MTH: // 方法
                        ++methodsCount;
                        break;
                    default: throw new AssertionError();
                }
            }
            // 字段个数            }
            this.databuf.appendChar(fieldsCount);
            this.writeFields(classSymbol.members().elems);
            // 方法个数
            this.databuf.appendChar(methodsCount);
            this.writeMethods(classSymbol.members().elems);

            int acountIdx = this.beginAttrs();
            int acount = 0;
            int alenIdx;
            if ( classSymbol.sourcefile != null && this.emitSourceFile ){
                alenIdx = this.writeAttr(this.names.SourceFile);
                // 源文件
                String name = classSymbol.sourcefile.toString();
                // 分隔符位置
                int index1 = name.lastIndexOf(File.separatorChar);
                int index2 = name.lastIndexOf("/");
                // 我们期望更大的值
                if (index2 > index1){
                    index1 = index2;
                }
                // 截取
                if ( index1 >= 0 ){
                    name = name.substring(index1+1);
                }
                this.databuf.appendChar( classSymbol.pool.put( this.names.fromString(name) ) );
                this.endAttr(alenIdx);
                ++acount;
            }

            acount += this.writeFlagAttrs( classSymbol.flags() );
            // CAFEBABY
            this.poolbuf.appendInt(-889275714 );
            this.poolbuf.appendChar(this.target.minorVersion);
            this.poolbuf.appendChar(this.target.majorVersion);
            // 常量池
            this.writePool(classSymbol.pool);
            this.endAttrs( acountIdx , acount );
            this.poolbuf.appendBytes(  this.databuf.elems ,  0 ,  this.databuf.length );
            outputStream.write( this.poolbuf.elems , 0 , this.poolbuf.length );
            this.pool = classSymbol.pool = null;
        }



    }






    public static class PoolOverflow extends Exception {
        public PoolOverflow() {
        }
    }

    public static class StringOverflow extends Exception {
        public final String value;

        public StringOverflow(String var1) {
            this.value = var1;
        }
    }


}

/**
 * 生成类型签名
 */
class LzyWriterType extends LzyWriterBuffer implements LzyTypeTags {

    boolean scramble;
    boolean scrambleAll;
    LzyTable names;

    LzyName fieldName(LzySymbol var1) {
        return (!this.scramble || (var1.flags() & 2L) == 0L) && (!this.scrambleAll || (var1.flags() & 5L) != 0L) ? var1.name : this.names.fromString("_$" + var1.name.index);
    }


    LzyClassFile.NameAndType nameType(LzySymbol symbol){
        return new LzyClassFile.NameAndType( this.fieldName(symbol) , symbol.type );
    }

    LzyName typeSig(LzyType var1) {
        if (  this.sigbuf.length != 0) {
            throw new AssertionError();
        } else {
            this.assembleSig(var1);
            LzyName var2 = this.sigbuf.toName(this.names);
            this.sigbuf.reset();
            return var2;
        }
    }

    void assembleSig(LzyList<LzyType> typeList){
        for (LzyList<LzyType> l = typeList; l.nonEmpty()  ; l = l.tail  ){
            this.assembleSig(l.head);
        }
    }


    void assembleSig(LzyType type) {
        switch(type.tag) {
            case BYTE:
                this.sigbuf.appendByte('B');
                break;
            case CHAR:
                this.sigbuf.appendByte('C');
                break;
            case SHORT:
                this.sigbuf.appendByte('S');
                break;
            case INT:
                this.sigbuf.appendByte('I');
                break;
            case LONG:
                this.sigbuf.appendByte('J');
                break;
            case FLOAT:
                this.sigbuf.appendByte('F');
                break;
            case DOUBLE:
                this.sigbuf.appendByte('D');
                break;
            case BOOLEAN:
                this.sigbuf.appendByte('Z');
                break;
            case VOID:
                this.sigbuf.appendByte('V');
                break;
            case CLASS:
                LzyClassType var2 = (LzyClassType)type;
                LzyClassSymbol var3 = (LzyClassSymbol)var2.tsym;
                this.sigbuf.appendByte('L');
                this.sigbuf.appendBytes(externalize(var3.flatname));
                this.sigbuf.appendByte(';');
                break;
            case ARRAY:
                LzyArrayType var4 = (LzyArrayType)type;
                this.sigbuf.appendByte('[');
                this.assembleSig(var4.elemtype);
                break;
            case METHOD:
                LzyMethodType var5 = (LzyMethodType)type;
                this.sigbuf.appendByte('(');
                this.assembleSig(var5.argtypes);
                this.sigbuf.appendByte(')');
                this.assembleSig(var5.restype);
                break;
            default:
                throw new AssertionError("typeSig" + type.tag);
        }

    }


    public static byte[] externalize(LzyName name) {
        return externalize(name.lzyTable.bytes, name.index, name.length);
    }

    /**
     * 将全限定类名中'.'换成'/'
     * @param bytes
     * @param index
     * @param length
     * @return
     */
    public static byte[] externalize(byte[] bytes, int index, int length) {
        byte[] newBytes = new byte[length];
        for(int i = 0; i < length; ++i) {
            byte b = bytes[index + i];
            if (b == '.') {
                newBytes[i] = '/';
            } else {
                newBytes[i] = b;
            }
        }
        return newBytes;
    }
}

/**
 * 生成属性
 */
class LzyWriterArrtibutes extends LzyWriterType {
    // 常量池
    LzyPool pool;

    /**
     * 开始标志
     * @return
     */
    int beginAttrs(){
        this.databuf.appendChar(0);
        return this.databuf.length;
    }

    /**
     * 结束标志
     * @param index
     */
    void endAttr(int index){
        this.putInt(this.databuf,index-4,this.databuf.length - index);
    }


    /**
     * 结束标志
     * @param index
     * @param count
     */
    void endAttrs(int index , int count){
        this.putChar(this.databuf , index-2  , count );
    }

    /**
     * 写入一个属性,最终存储的是局部变量表的下标
     * @param name
     * @return
     */
    int writeAttr(LzyName name){
        // 两字节
        this.databuf.appendChar( this.pool.put(name) );
        // 四字节
        this.databuf.appendInt(0);
        return this.databuf.length;
    }


    /**
     * 修饰符属性: 只处理synthetic 和 deprecated
     * @param flags
     * @return
     */
    int writeFlagAttrs(long flags){
        int acount = 0;
        int alenIdx  = 0;
        // Deprecate
        if ( (flags & LzyFlags.DEPRECATED) != 0  ){
            alenIdx = this.writeAttr( this.names.Deprecated );
            this.endAttr(alenIdx);
            ++acount;
        }
        // Synthetic
        if ( (flags & LzyFlags.SYNTHETIC ) != 0  ){
            alenIdx = this.writeAttr(this.names.Synthetic);
            this.endAttr(alenIdx);
            ++acount;
        }
        return acount;
    }

    int writeMemberAttrs(LzySymbol symbol){
        int acount = this.writeFlagAttrs( symbol.flags() );
        return acount;
    }

    void writeCode(LzyCode code){
        // 最大栈深度
        this.databuf.appendChar(code.max_stack);
        // 局部变量表大小
        this.databuf.appendChar(code.max_locals);
        // 当前字节码指令的位置
        this.databuf.appendInt(code.cp);
        // Code属性内容
        this.databuf.appendBytes(code.code,0,code.cp);
        // 异常表大小
        this.databuf.appendChar( code.catchInfo.length() );
        // 异常表
        for(LzyList l = code.catchInfo.toList() ; l.nonEmpty() ; l = l.tail  ){
            // 两字节添加
            for (char c : ((char[]) l.head)) {
                this.databuf.appendChar(c);
            }
        }
        // 开始位置
        int acountIdx = this.beginAttrs();
        int acount = 0;
        int alenIdx;
        int nGenericVars;
        // 局部变量表
        if ( code.varBufferSize > 0  ){
            // 局部变量表插入常量池
            alenIdx = this.writeAttr(this.names.LocalVariableTable);
            nGenericVars = code.varBufferSize;
            // 局部变量表大小
            this.databuf.appendChar(nGenericVars);
            //
            for (int i = 0; i < nGenericVars; i++) {
                // 局部变量
                LzyCode.LocalVar localVar = code.lvar[i];
                if ( localVar.start_pc < 0 || localVar.start_pc >  code.cp ){
                    throw new AssertionError();
                }
                // 开始位置
                this.databuf.appendChar( localVar.start_pc );
                if (localVar.length <0 || localVar.start_pc+localVar.length > code.cp){
                    throw new AssertionError();
                }
                // 长度
                this.databuf.appendChar(localVar.length);
                LzyVarSymbol varSymbol = localVar.varSymbol;
                // 变量名
                this.databuf.appendChar( this.pool.put( varSymbol.name ) );
                // 变量类型
                this.databuf.appendChar( this.pool.put( this.typeSig(varSymbol.type) ) );
                // 变量的位置
                this.databuf.appendChar( localVar.reg );
            }

            this.endAttr(alenIdx);
            ++acount;
        }
        this.endAttrs(acountIdx,acount);
    }
}


/**
 * 常量池相关操作
 */
class LzyWriterConstanPool extends LzyWriterArrtibutes implements LzyPoolType {


    /**
     *
     * @param entry
     */
    void writeMethods(LzyScope.Entry entry){
        // 所有的方法
        LzyList<LzyMethodSymbol> methods = LzyList.nil();
        for (LzyScope.Entry e = entry;  e != null ; e = e.sibling ){
            if ( e.sym.kind == LzyKinds.MTH){
                methods = methods.prepend( (LzyMethodSymbol)e.sym );
            }
        }
        // 不为空
        while ( methods.nonEmpty() ){
            this.writeMethod( methods.head );
            methods = methods.tail;
        }

    }

    void writeMethod(LzyMethodSymbol methodSymbol){
        // 修饰符
        this.databuf.appendChar( (int)methodSymbol.flags() );
        // 方法名
        this.databuf.appendChar( this.pool.put( this.fieldName(methodSymbol) ) );
        // 方法签名
        this.databuf.appendChar( this.pool.put( this.typeSig(methodSymbol.externalType()) )  );
        // 开始下标
        int acountIndex = this.beginAttrs();
        int acount = 0;
        // 方法的code属性
        if ( methodSymbol.code != null ){
            // Code存储到局部变量表
            int alenIdx = this.writeAttr(this.names.Code);
            // 写入字节码Code
            this.writeCode(methodSymbol.code);
            methodSymbol.code = null;
            this.endAttr(alenIdx);
            ++acount;
        }

        LzyList thrown = methodSymbol.type.thrown();
        if ( thrown.nonEmpty() ){
            // 异常字符加入常量池
            int alenIdx  = this.writeAttr(this.names.Exceptions);
            // 异常大小
            this.databuf.appendChar( thrown.length() );
            // 遍历异常
            for ( LzyList<LzyType> l = thrown;  l.nonEmpty()  ; l = l.tail ) {
                this.databuf.appendChar( this.pool.put( l.head.tsym ) );
            }
            this.endAttr(alenIdx);
            ++acount;
        }
        acount += this.writeMemberAttrs(methodSymbol);
        this.endAttrs(acountIndex,acount);
    }

    void writeFields(LzyScope.Entry entry){
        // 作用域下查找
         LzyList<LzyVarSymbol> vars = LzyList.nil();
         for ( LzyScope.Entry e = entry ; e != null  ;  e = e.sibling ){
            if ( e.sym.kind == LzyKinds.VAR ){
                vars = vars.prepend( (LzyVarSymbol)e.sym );
            }
         }
         // 循环遍历
         while ( vars.nonEmpty() ){
             this.writeField( (LzyVarSymbol) vars.head  );
             vars = vars.tail;
         }
    }

    void writeField(LzyVarSymbol varSymbol){
        // 权限修饰符
        this.databuf.appendChar( (int) varSymbol.flags()  );
        // 字段名
        this.databuf.appendChar( this.pool.put( this.fieldName(varSymbol)  ) );
        // 签名
        this.databuf.appendChar( this.pool.put( this.typeSig(varSymbol.type) ) );

        int acountIdx = this.beginAttrs();

        int acount = 0;

        // 常量
        if ( varSymbol.constValue != null ){
            // ConstantValue常量标记
            int alenIdx = this.writeAttr( this.names.ConstantValue ) ;
            this.databuf.appendChar( this.pool.put( varSymbol.constValue ) );
            this.endAttr(alenIdx);
            ++acount;
        }
        acount += this.writeMemberAttrs(varSymbol);
        this.endAttrs(acountIdx,acount);
    }

    void writePool(LzyPool pool) throws LzyClassWriter.StringOverflow, LzyClassWriter.PoolOverflow {
        // 常量池缓存的末尾下标
        int poolCountIdx = this.poolbuf.length;
        this.poolbuf.appendChar(0);
        //
        for (int i = 1; i < pool.pp; i++) {
            // 常量项
            Object value = pool.pool[i];
            LzyAssert.checkNonNull(value);
            if ( value instanceof LzyPool.Method ){
                // 方法符号
                value = ((LzyPool.Method)value).m;
            }else if ( value instanceof  LzyPool.Variable ){
                // 变量符号
                value = ((LzyPool.Variable)value ).v;
            }

            // 方法符号
            if ( value instanceof  LzyMethodSymbol ){
                LzyMethodSymbol methodSymbol  = (LzyMethodSymbol)value;
                // 接口中方法标记: 11
                // 普通方法标记: 10
                this.poolbuf.appendByte( (methodSymbol.owner.flags() & LzyFlags.INTERFACE) != 0 ?CONSTANT_InterfaceMethodref_info :CONSTANT_Methodref_info );
                // 方法所属的类: 添加到常量池中
                this.poolbuf.appendChar( pool.put(methodSymbol.owner) );
                //名字和类型
                this.poolbuf.appendChar( pool.put(this.nameType(methodSymbol)) );
            }else if ( value instanceof LzyVarSymbol ){ //变量符号
                LzyVarSymbol varSymbol = (LzyVarSymbol)value;
                this.poolbuf.appendByte(CONSTANT_Filedref_info);
                // 变量所属的类: 添加到常量池中
                this.poolbuf.appendChar( pool.put(varSymbol.owner) );
                // 名字和类型
                this.poolbuf.appendChar( pool.put(this.nameType(varSymbol)) );
            }else if ( value instanceof  LzyName  ){ // Name
                this.poolbuf.appendByte( CONSTANT_Utf8_info );
                // 字节
                byte[] bytes  = ((LzyName)value).toUtf();
                // 字符串大小
                this.poolbuf.appendChar(bytes.length);
                // 字符串
                this.poolbuf.appendBytes( bytes , 0 , bytes.length );
                if ( bytes.length > 65535  ){
                    throw new LzyClassWriter.StringOverflow( value.toString() );
                }
            }else if ( value instanceof  LzyClassSymbol  ){ // 类符号
                LzyClassSymbol classSymbol  =  (LzyClassSymbol)value;
                this.poolbuf.appendByte( CONSTANT_Class_info );
                // 数组
                if ( classSymbol.type.tag == LzyTypeTags.ARRAY ){
                    this.poolbuf.appendChar( pool.put( this.typeSig(classSymbol.type) )  );
                }else{
                    this.poolbuf.appendChar( pool.put( this.names.fromUtf( externalize( classSymbol.flatname ) ) ) );
                }
            }else if ( value instanceof LzyClassFile.NameAndType){
                // 名称 + 签名
                LzyClassFile.NameAndType nameAndType  = (LzyClassFile.NameAndType)value;
                this.poolbuf.appendByte(CONSTANT_NameAndType_info);
                // 名称: 可能是方法名称 或者 字段名称
                this.poolbuf.appendChar( pool.put(nameAndType.name) );
                // 签名
                this.poolbuf.appendChar( pool.put( this.typeSig(nameAndType.type) ) );
            }else if ( value instanceof Integer ){
                this.poolbuf.appendByte( CONSTANT_Integer_info );
                this.poolbuf.appendInt( (Integer)value );
            }else if ( value instanceof Long ){
                this.poolbuf.appendByte( CONSTANT_Long_info );
                this.poolbuf.appendLong( (Long) value );
                // 占用两个常量项
                ++i;
            }else if ( value instanceof Float ){
                this.poolbuf.appendByte( CONSTANT_Float_info );
                this.poolbuf.appendFloat((Float)value);
            }else if ( value instanceof Double ){
                this.poolbuf.appendByte( CONSTANT_Double_info );
                this.poolbuf.appendDouble((Double)value);
                // 占用两个常量项
                ++i;
            }else if ( value instanceof String ){
                this.poolbuf.appendByte( CONSTANT_String_info );
                this.poolbuf.appendChar( pool.put( this.names.fromString( (String) value)  ) );
            }else if ( value instanceof LzyType ){
                this.poolbuf.appendByte( CONSTANT_Class_info );
                this.poolbuf.appendChar( pool.put( this.xClassName((LzyType)value) ) );
            }
        }
        if (pool.pp > 65535) {
            throw new LzyClassWriter.PoolOverflow();
        } else {
            this.putChar(this.poolbuf,poolCountIdx, pool.pp);
        }

    }

    public LzyName xClassName(LzyType type){
        if ( type.tag == LzyTypeTags.CLASS ){
             return this.names.fromUtf( externalize( type.tsym.flatName() ) );
        }else if ( type.tag == LzyTypeTags.ARRAY ){
            return this.typeSig( type );
        }else {
            throw new AssertionError("xClassName");
        }
    }

}


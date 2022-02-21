package work.liziyun.code;


import work.liziyun.code.symbol.LzyClassSymbol;
import work.liziyun.code.symbol.*;
import work.liziyun.code.type.LzyArrayType;
import work.liziyun.code.type.LzyClassType;
import work.liziyun.code.type.LzyMethodType;
import work.liziyun.code.type.LzyType;
import work.liziyun.io.LzyJavaFile;
import work.liziyun.jvm.LzyTarget;
import work.liziyun.tag.LzyFlags;
import work.liziyun.util.LzyContext;
import work.liziyun.util.LzyConvert;
import work.liziyun.util.LzyList;
import work.liziyun.world.LzyName;
import work.liziyun.world.LzyTable;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static work.liziyun.code.LzyClassFile.JAVA_MAGIC;
import static work.liziyun.code.LzyClassFile.Version.V45_3;


class LzyBufferAccess {

     byte[] buf = new byte[INITIAL_BUFFER_SIZE];
    public static final int INITIAL_BUFFER_SIZE = 0x0fff0;
    int bp;

    char nextChar() {
        return (char)(((buf[bp++] & 0xFF) << 8) + (buf[bp++] & 0xFF));
    }

    int nextInt() {
        return
                ((buf[bp++] & 0xFF) << 24) +
                        ((buf[bp++] & 0xFF) << 16) +
                        ((buf[bp++] & 0xFF) << 8) +
                        (buf[bp++] & 0xFF);
    }

    byte nextByte() {
        return buf[bp++];
    }

    char getChar(int bp) {
        return
                (char)(((buf[bp] & 0xFF) << 8) + (buf[bp+1] & 0xFF));
    }

    float getFloat(int bp) {
        DataInputStream bufin = new DataInputStream(new ByteArrayInputStream(buf, bp, 4));
        try {
            return bufin.readFloat();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }


    double getDouble(int bp) {
        DataInputStream bufin = new DataInputStream(new ByteArrayInputStream(buf, bp, 8));
        try {
            return bufin.readDouble();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    int getInt(int bp) {
        return ((buf[bp] & 0xFF) << 24) +
                ((buf[bp+1] & 0xFF) << 16) +
                ((buf[bp+2] & 0xFF) << 8) +
                (buf[bp+3] & 0xFF);
    }

    long getLong(int bp) {
        DataInputStream bufin = new DataInputStream(new ByteArrayInputStream(buf, bp, 8));
        try {
            return bufin.readLong();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }


}


/**
 * 填充接口: Symbol.Completer
 * 作用:
 *      1. 包符号的创建
 *      2. 包符号的填充
 */

public class LzyClassReader extends LzyReadAttributes implements LzySymbol.Completer {

    private static final LzyContext.Key classReaderKey = new LzyContext.Key();

    protected LzySymbol currentOwner = null;
    protected LzyJavaFile currentClassFile = null;
    public LzyClassReader.SourceCompleter sourceCompleter = null;
    // 包符号缓存
    private Map<LzyName, LzyPackageSymbol> packages ;
    // 类符号缓存
    private Map<LzyName, LzyClassSymbol> classes ;
    // ClassSymbol类型标识
    public final static int TYP = 1 << 1;
    // PackageSymbol类型标识
    public final static int PCK = 1 << 0;
    // 一次只能读取一个文件
    private boolean filling = false;
    // 开关: 是否检查版本
    boolean checkClassFile;
    // 签名缓存
    byte[] signatureBuffer = new byte[0];
    // 开关: 两个文件同时存在。可能是另一种,".java"和".class"。
    public boolean preferSource;
    // 分隔符
    public static final String pathSep = System.getProperty("path.separator");
    // .class后缀
    static final EnumSet classOnly = EnumSet.of(LzyJavaFile.Kind.CLASS);
    // .java后缀
    static final EnumSet javaOnly= EnumSet.of(LzyJavaFile.Kind.SOURCE);
    // .class 和 .java
    static final EnumSet classOrJava= EnumSet.of(LzyJavaFile.Kind.SOURCE,LzyJavaFile.Kind.CLASS);
    // 启动类路径
    public String bootclassPath ;
    // 源码路径
    public String sourceClassPath ;
    // 字节码路径
    public String classPath ;
    // 打开压缩包
    HashMap<String, ZipFile> zipCache = new HashMap();


    public interface SourceCompleter {
        void complete(LzyClassSymbol sym,String classFileName,InputStream inputStream) throws IOException;
    }

    public static void main(String[] args) {
        LzyContext LzyContext = new LzyContext();
        LzyClassReader cr = LzyClassReader.instance(LzyContext);
    }

    public LzyClassReader(LzyContext LzyContext) {

        super(LzyContext);
        LzyContext.put(classReaderKey,this);
        syms = LzySymtab.instance(LzyContext);
        init(syms);
        // 填充器
        typevars = new LzyScope(syms.noSymbol);
        bootclassPath = System.getProperty("sun.boot.class.path");
        classPath = System.getProperty("env.class.path");
    }

    public void init(LzySymtab symtab){
        if (this.classes == null) {
            this.packages = symtab.packages;
            this.classes = symtab.classes;
            // 填充器
            this.packages.put(symtab.rootPackage.fullname, symtab.rootPackage);
            symtab.rootPackage.completer = this;
            this.packages.put(symtab.emptyPackage.fullname, symtab.emptyPackage);
            symtab.emptyPackage.completer = this;
        }
    }

    public static class BadClassFile extends LzySymbol.CompletionFailure{

        public BadClassFile(LzySymbol sym, String errmsg) {
            super(sym, errmsg);
        }
    }


    public static LzyClassReader instance(LzyContext LzyContext) {
        LzyClassReader classReader = (LzyClassReader)LzyContext.get(classReaderKey);
        if (classReader == null) {
            classReader = new LzyClassReader(LzyContext);
        }
        return classReader;
    }

    @Override
    public void complete(LzySymbol symbol)  {
        // 当前符号的类型: ClassSymbol
        if ( symbol.kind == TYP){
            // 创建符号类符号
            LzyClassSymbol c = (LzyClassSymbol)symbol;
            // 初始化符号的作用域
            c.members_field = new LzyScope.ErrorScope(c);
            // 先填充owner的包符号，再填充类符号
            completeOwners(c.owner);
            completeEnclosing(c);
            fillIn(c);
        }else if(symbol.kind == PCK){
            LzyPackageSymbol p = (LzyPackageSymbol)symbol;
            try {
                fillIn(p);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    void readInnerClasses(LzyClassSymbol c) {
        // 大小
        char size = this.nextChar();
        for (int i = 0; i < size; i++) {
            this.nextChar();
            // 获取符号
            LzyClassSymbol outerClassSymbol = readClassSymbol(this.nextChar());
            // 名称
            LzyName name = this.readName(this.nextChar());
            if (name == null){
                name = this.names.empty;
            }
            // 修饰符
            char flags = this.nextChar();
            if (outerClassSymbol != null){
                if (name == this.names.empty){
                    name = this.names.one;
                }
                // 创建类符号: 通过外部类
                LzyClassSymbol innnerClassSymbol = this.enterClass(name, outerClassSymbol);
                // 内部类没有static修饰符: 内部类 和 外部类，拥有关联关系
                if ( (flags& LzyFlags.STATIC) == 0 ){
                    ((LzyClassType)innnerClassSymbol.type).outer_field = outerClassSymbol.type;
                }
                // 内部类填充到外部类的作用域中
                if (c == outerClassSymbol){
                    innnerClassSymbol.flags_field = flags;
                    this.enterMember(c,innnerClassSymbol);
                }
            }
        }
    }

    /** Add member to class unless it is synthetic.
     */
    private void enterMember(LzyClassSymbol c, LzySymbol sym) {
        if ((sym.flags_field & (LzyFlags.SYNTHETIC)) == 0)
            c.members_field.enter(sym);
    }



    @Override
    public LzyClassSymbol enterClass(LzyName flatname) {
        // 常识从缓存中获取
        LzyClassSymbol classSymbol = this.classes.get(flatname);
        if (classSymbol == null){
            LzyName pName = LzyConvert.packagePart(flatname);
            // 创建类符号 ---> 指定填充器(this)
            classSymbol = this.defineClass( LzyConvert.shortName(flatname) , this.enterPackage(pName) );
            // 存储到缓存中
            this.classes.put(flatname,classSymbol);
        }
        // 返回类符号
        return classSymbol;
    }



    // 通过包名，创建类符号
    @Override
    public LzyClassSymbol enterClass(LzyName name, LzyTypeSymbol owner) {
        LzyName flatName = LzyTypeSymbol.formFlatName(name, owner);
        // 从缓存中获取类符号
        LzyClassSymbol classSymbol = this.classes.get(flatName);
        if (classSymbol == null){
            // 创建类符号 ---> 指定填充器(this)
            classSymbol = this.defineClass(name,owner);
            this.classes.put(flatName,classSymbol);
        }else if ( (classSymbol.name != name||classSymbol.owner!=owner) && owner.kind == LzyClassReader.TYP  ){
            // 内部类的情况
            classSymbol.name = name;
            classSymbol.owner = owner;
            classSymbol.fullname = LzyClassSymbol.formFlatName(name,owner);
        }
        return classSymbol;
    }

    // 创建包符号。指定包符号的填充器!
    public LzyPackageSymbol enterPackage(LzyName fullname) {
        // 尝试从缓存中获取
        LzyPackageSymbol p = packages.get(fullname);
        if (p == null) {
            // Assert.check(!fullname.isEmpty(), "rootPackage missing!");
            // 创建新包符号
            p = new LzyPackageSymbol(
                    LzyConvert.shortName(fullname),
                    enterPackage(LzyConvert.packagePart(fullname)));//递归创建父包符号
            // 指定符号的填充器: 本类
            p.completer = this;
            // 放入缓存中
            packages.put(fullname, p);
        }
        return p;
    }

    public LzyClassSymbol defineClass(LzyName name,LzySymbol symbol){
        LzyClassSymbol classSymbol = new LzyClassSymbol(0L, name, symbol);
        classSymbol.completer = this;
        return classSymbol;
    }


    /**
     * 先找到包符号，再填充类符号
     * @param o
     */
    private void completeOwners(LzySymbol o) {
        if (o.kind != PCK) completeOwners(o.owner);
        o.complete();
    }

    private void completeEnclosing(LzyClassSymbol c) {
        // 当前类属于包: 不是内部类的情况
        if (c.owner.kind == PCK) {
            // 包符号
            LzySymbol owner = c.owner;
            // 简单类名: 1.去掉包名   2.如果有$符号,那么获取$前面的类名
            for (LzyName name : LzyConvert.enclosingCandidates(LzyConvert.shortName(c.name))) {
                // 包符号作用域下查找符号
                LzySymbol encl = owner.members().lookup(name).sym;
                if (encl == null)
                    // 缓存中获取
                    encl = classes.get(LzyTypeSymbol.formFlatName(name, owner));
                if (encl != null)
                    // 类符号的填充
                    encl.complete();
            }
        }
    }


    // 类符号的填充
    private void fillIn(LzyClassSymbol c) {
        currentOwner = c;
        LzyJavaFile classfile = c.classfile;
        if ( classfile != null ){
            // 上一个文件路径
            LzyJavaFile previousClassFile = currentClassFile;
            // 当前文件路径
            currentClassFile = classfile;
            // 如果是字节码文件
            if (classfile.getKind() == LzyJavaFile.Kind.CLASS){
                filling = true;
                try {
                    bp = 0;
                    buf = readInputStream(buf, classfile.open());
                    readClassFile(c);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else{// 源码文件需要指定填充器
                if (this.sourceCompleter != null){
                    // .java文件的ClassSymbol符号填充
                    try {
                        this.sourceCompleter.complete(c,classfile.getPath(),classfile.open());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    System.out.println("sourceCompleter不能为空!");
                }
            }
        }else{
            // 这是一个非法的类符号:
            throw new LzySymbol.CompletionFailure(c,"file.doesnt.contain.class "+c.fullName());
        }
    }

    private void listAll(String allRootPath, String fullName, EnumSet kinds, LzyPackageSymbol packageSymbol) throws IOException {
        int i = 0;
        // 系统分隔符的位置
        int sepIndex;
        for (int length = allRootPath.length(); i < length; i = sepIndex+1) {
            // 寻找分隔符的位置
            sepIndex = allRootPath.indexOf(pathSep,i);
            String rootPath = allRootPath.substring(i,sepIndex);
            // 处理
            this.list(rootPath,fullName,kinds,packageSymbol);
        }
    }



    // 指定路径下面查找
    // 注意: 我们并不处理压缩包的情况
    private void list(String rootPath,String fullPath,EnumSet kinds,LzyPackageSymbol packageSymbol) throws IOException {
        // 根是一个.jar
        if (  (new File(rootPath)).isFile()  ){
            // 压缩包文件
            ZipFile zipFile = this.openZip(rootPath);
            // 全限定类名
            if (fullPath.length() != 0){
                fullPath = fullPath.replace('\\','/');
                if (!fullPath.endsWith("/")){
                    fullPath = fullPath + "/";
                }
            }
            // 全限定类名的长度
            int length = fullPath.length();
            Iterator<ZipEntry> entries = (Iterator<ZipEntry>)zipFile.entries();
            ZipEntry zipEntry = null;
            while (entries.hasNext()){
                zipEntry = entries.next();
                // 压缩包中类的全限定类名
                String zipFullName = zipEntry.getName();
                // 是我们要找的
                if (zipFullName.startsWith(fullPath)){
                    if (this.isValidFile(zipFullName,kinds)){
                        String classFileName = zipFullName.substring(length);
                        // 1.确保长度有限 2.确保不包含字符'/'
                        if (classFileName.length() > 0 && classFileName.indexOf('/')<0){
                            // 创建一个IO
                            LzyJavaFile.JarFile jarFile = new LzyJavaFile.JarFile(classFileName, zipFile, zipEntry);
                            this.includeClassFile(packageSymbol,jarFile);
                        }
                    }else{
                        // 扩展机制: 对其他格式的解析
                        extraZipFileActions(packageSymbol,zipFullName,fullPath,rootPath);
                    }
                }
            }
        }else{// 根是一个目录(我们开发的项目目录)
            File file = fullPath.length()!=0?new File(rootPath,fullPath):new File(rootPath);

            // 这里有个重要的检查
            if ( file.exists() ){
                // 所有下级
                String[] LzyList = file.list();
                for (String fileName : LzyList) {
                    // 检查后缀
                    if (isValidFile(fileName,kinds)){
                        LzyJavaFile.CommonFile commonFile = new LzyJavaFile.CommonFile(fileName,new File(file,fileName) );
                        this.includeClassFile(packageSymbol,commonFile);
                    }else{
                        System.out.println("非法后缀尝试扩展机制: "+fileName );
                        // 扩展机制: 对其他格式的解析
                        extraZipFileActions(packageSymbol,fileName,fullPath,rootPath);
                    }
                }

            }
        }
    }


    // 扩展机制: 其他格式解析
    protected void extraZipFileActions(LzyPackageSymbol packageSymbol, String name, String fullPath, String rootPath) {

    }

    private void includeClassFile(LzyPackageSymbol packageSymbol ,LzyJavaFile lzyJavaFile){
        // 没有存在标识
        if ( (packageSymbol.flags_field & LzyFlags.EXISTS) == 0L ){
            // 向上寻找并且添加标识
            for (Object tem = packageSymbol;tem!=null&& ((LzySymbol)tem).kind ==LzyClassReader.PCK ; tem = ((LzySymbol)tem).owner){
                ((LzySymbol)tem).flags_field |= LzyFlags.EXISTS;
            }
        }
        // 标识: java , class
        int seen;
        if (lzyJavaFile.getKind() == LzyJavaFile.Kind.CLASS){
            seen = LzyFlags.CLASS_SEEN;
        }else{
            seen = LzyFlags.SOURCE_SEEN;
        }
        // 获取Name,去掉结尾
        LzyName name = this.names.fromString(lzyJavaFile.getSimpleName());
        // 获取符号: 在包符号的作用域下查找
        LzyClassSymbol classSymbol = (LzyClassSymbol) packageSymbol.members_field.lookup(name).sym;
        if (classSymbol == null){
            // 通过包名，创建类符号
            classSymbol = this.enterClass(name,packageSymbol);
            classSymbol.classfile = lzyJavaFile; // 这里多实现了无用的接口
            // 类符号 属于 包符号
            if (classSymbol.owner == packageSymbol){
                packageSymbol.members_field.enter(classSymbol);
            }
        }else if (classSymbol.classfile != null && (classSymbol.flags_field & seen) == 0){ // 不是其中的一种后缀，但是另外一种后缀

            // 但是另一种后缀
            // 原则: 最新的,而不是.java或.class优先!
            if ( (classSymbol.flags_field & (LzyFlags.CLASS_SEEN|LzyFlags.SOURCE_SEEN)) != 0){
                // 推翻策略: 新的将覆盖旧的
                long date1 = lzyJavaFile.lastMod();
                long date2 = classSymbol.classfile.lastMod();
                if (date1 >= 0 && date2 >= 0 && date1 > date2){
                    classSymbol.classfile = lzyJavaFile;
                }
            }
        }
        // 后缀标识: 表明.java，还是.class
        classSymbol.flags_field |= (long)seen;
    }



    private boolean isValidFile(String fileName,EnumSet<LzyJavaFile.Kind> enumSet){
        for (LzyJavaFile.Kind kind : enumSet) {
            if (fileName.endsWith(kind.extension))return true;
        }
        return false;
    }

    // 包符号的填充complete
    private void fillIn(LzyPackageSymbol p) throws IOException {
        //初始化作用域
        if (p.members_field == null){
            p.members_field = new LzyScope(p);
        }


        String fullName ;
        if (p.fullname == this.names.emptyPackage){
            fullName = this.names.empty.toString();
        }else{
            // ‘.’环分隔符
            fullName = p.fullname.toString().replace('.', File.separatorChar);
        }


        // 系统中的启动类
        this.listAll(this.bootclassPath,fullName,classOnly,p);
        // 有填充器，但是没有源码路径
        // 1. sourceCompleter ---> JavaCompiler
        // 2. sourceClassPath ---> 项目路径
        // 注意: 我们通常使用这种情况!
        if (this.sourceCompleter != null && this.sourceClassPath == null ){
            // 没有指定源码路径，那么我们源码和字节码文件。都在classPath中查找!
            this.listAll(this.classPath,fullName,classOrJava,p);
        }else{ // 这种情况: 出现可能性很低，我们不做考虑!
            // 指定了源码路径。那我们现充字节码路径中开始查找，字节码路径的优先级更高
            this.listAll(this.classPath,fullName,classOnly,p);
            // 拥有源码路径，并且拥有源码填充器
            if (this.sourceCompleter != null){
                // 最后我们从源文件路径下查找`1
                this.listAll(this.sourceClassPath,fullName,javaOnly,p);
            }
        }

    }



    /**
     * 字节码文件的格式校验
     * @param c
     * @throws IOException
     */
    private void readClassFile(LzyClassSymbol c) throws IOException {
        // 4个字节
        int magic = nextInt();
        if (magic != JAVA_MAGIC){
            System.out.println("非法的Magic!");
        }
        // throw badClassFile("illegal.start.of.class.file");
        // 2字节大版本号
        minorVersion = nextChar();
        // 2字节小版本号
        majorVersion = nextChar();
        // 大版本号
        int maxMajor = LzyTarget.MAX().majorVersion;
        // 小版本号
        int maxMinor = LzyTarget.MAX().minorVersion;
        // 字节码文件的版本号 不能大于 编译器的版本号
        if (majorVersion > maxMajor ||
                majorVersion * 1000 + minorVersion <
                        LzyTarget.MIN().majorVersion * 1000 + LzyTarget.MIN().minorVersion)
        {

            // 版本号校验: 报错！
            if (majorVersion == (maxMajor + 1)){
                // log.warning("big.major.version", currentClassFile, majorVersion, maxMajor);
            }else{
                //  throw badClassFile("wrong.version",Integer.toString(majorVersion),Integer.toString(minorVersion),Integer.toString(maxMajor),Integer.toString(maxMinor));
            }

        }
        else if (checkClassFile &&
                majorVersion == maxMajor &&
                minorVersion > maxMinor)
        {
            //  printCCF("found.later.version",Integer.toString(minorVersion));
        }
        // 读取常量池
        indexPool(); // 跳过常量池
        if (signatureBuffer.length < bp) {
            int ns = Integer.highestOneBit(bp) << 1;
            signatureBuffer = new byte[ns];
        }
        readClass(c);
    }

    void readClass(LzyClassSymbol c) {
        LzyClassType ct = (LzyClassType)c.type;

        // allocate scope for members
        c.members_field = new LzyScope(c);

        // prepare type variable table
        typevars = this.typevars.dup();

        // read flags, or skip if this is an inner class
        // 读取访问标记: 修饰符
        long flags = adjustClassFlags(nextChar());
       if ( c != null && c.owner.kind == PCK) c.flags_field = flags;

        // read own class name and check that it matches
        // 读取this关键字
        LzyClassSymbol self = readClassSymbol(nextChar());
        if (c != self) {
            // throw badClassFile("class.file.wrong.class", self.flatname);
        }
        // class attributes must be read before class
        // skip ahead to read class attributes
        int startbp = bp;
        // 直接跳过父类索引
        nextChar();
        // 获取接口个数
        char interfaceCount = nextChar();
        // 直接跳过接口的
        bp += interfaceCount * 2;
        // 字段个数
        char fieldCount = nextChar();
        // 跳过字段
        for (int i = 0; i < fieldCount; i++) skipMember();
        // 方法个数
        char methodCount = nextChar();
        // 跳过方法
        for (int i = 0; i < methodCount; i++) skipMember();
        // 读取属性表
        readClassAttrs(c);


        // 对常量池进行处理
        if (readAllOfClassFile) {
            for (int i = 1; i < poolObj.length; i++) readPool(i);
            c.pool = new LzyPool(poolObj.length, poolObj);
        }

        // reset and read rest of classinfo
        bp = startbp;
        // 继承
        int n = nextChar();
        if (ct.supertype_field == null){
            if (n == 0){
                ct.supertype_field = LzyType.noType;
            }else{
                ct.supertype_field = this.readClassSymbol(n).type;
            }
        }

        // 接口个数
        n = nextChar();
        LzyList<LzyType> interfaceList = LzyList.nil();
        if (ct.interfaces_field == null){
            for (int i = 0; i < n; i++) {
                interfaceList.append(this.readClassSymbol(this.nextChar()).type);
            }
            ct.interfaces_field = interfaceList;
        }

        // Assert.check(fieldCount == nextChar());
        int fieldSize = this.nextChar();
        for (int i = 0; i < fieldSize; i++) enterMember(c, readField());
        // Assert.check(methodCount == nextChar());
        int methodSize = this.nextChar();
        for (int i = 0; i < methodSize; i++) enterMember(c, readMethod());
        //
        typevars = typevars.leave();
    }

    /** Read a field.
     */
    LzyVarSymbol readField() {
        long flags = adjustFieldFlags(nextChar());
        LzyName name = readName(nextChar());
        LzyType type = readType(nextChar());
        LzyVarSymbol v = new LzyVarSymbol(flags, name, type, currentOwner);
        readMemberAttrs(v);
        return v;
    }
    /** Read a method.
     */
    LzyMethodSymbol readMethod() {
        long flags = adjustMethodFlags(nextChar());
        LzyName name = readName(nextChar());
        LzyType type = readType(nextChar());
        LzyMethodSymbol m = new LzyMethodSymbol(flags, name, type, currentOwner);
        LzySymbol prevOwner = currentOwner;
        currentOwner = m;
        try {
            readMemberAttrs(m);
        } finally {
            currentOwner = prevOwner;
        }
        return m;
    }




    private static byte[] readInputStream(byte[] buf, InputStream s) throws IOException {
        try {
            buf = ensureCapacity(buf, s.available());
            int r = s.read(buf);
            int bp = 0;
            while (r != -1) {
                bp += r;
                buf = ensureCapacity(buf, bp);
                r = s.read(buf, bp, buf.length - bp);
            }
            return buf;
        } finally {
            try {
                s.close();
            } catch (IOException e) {
                /* Ignore any errors, as this stream may have already
                 * thrown a related exception which is the one that
                 * should be reported.
                 */
            }
        }
    }

    private static byte[] ensureCapacity(byte[] buf, int needed) {
        if (buf.length <= needed) {
            byte[] old = buf;
            buf = new byte[Integer.highestOneBit(needed) << 1];
            System.arraycopy(old, 0, buf, 0, old.length);
        }
        return buf;
    }


    long adjustFieldFlags(long flags) {
        return flags;
    }

    long adjustClassFlags(long flags) {
        return flags & ~LzyFlags.ACC_SUPER; // SUPER and SYNCHRONIZED bits overloaded
    }
    long adjustMethodFlags(long flags) {
        if ((flags & LzyFlags.ACC_BRIDGE) != 0) {
            flags &= ~LzyFlags.ACC_BRIDGE;
            flags |= LzyFlags.BRIDGE;
            if (!allowGenerics)
                flags &= ~LzyFlags.SYNTHETIC;
        }
        if ((flags & LzyFlags.ACC_VARARGS) != 0) {
            flags &= ~LzyFlags.ACC_VARARGS;
            flags |= LzyFlags.VARARGS;
        }
        return flags;
    }


    void skipMember() {
        // 跳过6个字节
        bp = bp + 6;
        // 读取2字节: 个数
        char ac = nextChar();
        for (int i = 0; i < ac; i++) {
            //
            bp = bp + 2;
            int attrLen = nextInt();
            bp = bp + attrLen;
        }
    }




    ZipFile openZip(String filePath) throws IOException {
        // 尝试从缓存中获取
        ZipFile zipFile = zipCache.get(filePath);
        if (zipFile == null){
            zipFile = new ZipFile(filePath);
            this.zipCache.put(filePath,zipFile);
        }
        return zipFile;
    }


    public LzyClassSymbol loadClass(LzyName name) throws LzySymbol.CompletionFailure {
        // 获取缓存中最开始到底有没有ClassSymbol
        boolean hasClassSymbol = this.classes.get(name)==null;
        LzyClassSymbol classSymbol = this.enterClass(name);
        // 没有进行填充，并且拥有填充器
        if ( classSymbol.members_field == null && classSymbol.completer != null ){
            try{
                classSymbol.complete();
            }catch (LzySymbol.CompletionFailure e){
                // 如果本来就不存在，被迫创建的。并且无法填充符号，那么我们进行删除。
                if (hasClassSymbol){
                    this.classes.remove(name);
                }
                throw e;
            }
        }
        return classSymbol;
    }
}

/**
 * 属性
 */
enum AttributeKind { CLASS, MEMBER };
abstract class LzyReadAttributes extends LzyReadTypes {
    // 大版本号
    int majorVersion;
    // 小版本号
    int minorVersion;

    // 用于保存方法参数的常量池索引的表
    int[] parameterNameIndices;

    // 开关: 读取常量池代码部分
    public boolean readAllOfClassFile = false;

    // 开关: 保存变量表中参数名称
    public boolean saveParameterNames;

    // 是否找到任何参数的名称
    boolean haveParameterNameIndices;

    // 开关: 读取GJ签名信息
    boolean allowGenerics;

    public LzyReadAttributes(LzyContext LzyContext) {
        super(LzyContext);

        initAttributeReaders();
    }

    /**
     * Character.isDigit answers <tt>true</tt> to some non-ascii
     * digits.  This one does not.  <b>copied from java.lang.Class</b>
     */
    private static boolean isAsciiDigit(char c) {
        return '0' <= c && c <= '9';
    }

    /** Read member attributes.
     */
    void readMemberAttrs(LzySymbol sym) {
        readAttrs(sym, AttributeKind.MEMBER);
    }

    void readAttrs(LzySymbol sym, AttributeKind kind) {
        // 读取2字节
        char ac = nextChar();
        //
        for (int i = 0; i < ac; i++) {
            // 读取2字节的Name
            LzyName attrName = readName(nextChar());
            // 读取属性长度
            int attrLen = nextInt();
            // 获取属性读取器
            AttributeReader r = attributeReaders.get(attrName);

            if (r != null && r.accepts(kind))
                // 利用属性读取器读取
                r.read(sym, attrLen);
            else  {
                // 没有找到将跳过这个属性
                bp = bp + attrLen;
            }
        }
    }

    /** Read class attributes.
     */
    void readClassAttrs(LzyClassSymbol c) {
        readAttrs(c, AttributeKind.CLASS);
    }

    /** Read code block.
     */
    LzyCode readCode(LzySymbol owner) {
        // 最大栈深度
        nextChar(); // max_stack
        // 局部变量表大小
        nextChar(); // max_locals
        // code长度
        final int  code_length = nextInt();
        // 跳过这么长
        bp += code_length;
        // 异常表大小
        final char exception_table_length = nextChar();
        // 跳过异常表
        bp += exception_table_length * 8;
        //
        readMemberAttrs(owner);
        return null;
    }


    abstract void readInnerClasses(LzyClassSymbol c);


    // 内部类: 抽象读取
    abstract class AttributeReader {

        AttributeReader(LzyName name, LzyClassFile.Version version, Set<AttributeKind> kinds) {
            this.name = name;
            this.version = version;
            this.kinds = kinds;
        }

        boolean accepts(AttributeKind kind) {
            if (kinds.contains(kind)) {
                if (majorVersion > version.major || (majorVersion == version.major && minorVersion >= version.minor))
                    return true;
            }
            return false;
        }

        abstract void read(LzySymbol sym, int attrLen);

        final LzyName name;
        final LzyClassFile.Version version;
        final Set<AttributeKind> kinds;
    }

    protected Set<AttributeKind> CLASS_ATTRIBUTE =
            EnumSet.of(AttributeKind.CLASS);
    protected Set<AttributeKind> MEMBER_ATTRIBUTE =
            EnumSet.of(AttributeKind.MEMBER);
    protected Set<AttributeKind> CLASS_OR_MEMBER_ATTRIBUTE =
            EnumSet.of(AttributeKind.CLASS, AttributeKind.MEMBER);

    // 所有属性读取器
    protected Map<LzyName, AttributeReader> attributeReaders = new HashMap<LzyName, AttributeReader>();
    // 初始化所有为属性读取器
    private void initAttributeReaders() {

        // v45.3 attributes
        // Code属性读取器
        AttributeReader codeAttributeReader = new AttributeReader(names.Code, V45_3, MEMBER_ATTRIBUTE) {
            void read(LzySymbol sym, int attrLen) {
                if (readAllOfClassFile || saveParameterNames)
                    ((LzyMethodSymbol) sym).code = readCode(sym);
                else
                    bp = bp + attrLen;
            }
        };
        attributeReaders.put(codeAttributeReader.name, codeAttributeReader);
        // 常量池值读取器
        AttributeReader constantValueAttributeReader = new AttributeReader(names.ConstantValue, V45_3, MEMBER_ATTRIBUTE) {
            void read(LzySymbol sym, int attrLen) {
                Object v = readPool(nextChar());
                // Ignore ConstantValue attribute if field not final.
                // 只有final修饰的才会被处理
                if ((sym.flags() & LzyFlags.FINAL) != 0)
                    ((LzyVarSymbol) sym).constValue = v;
            }
        };
        attributeReaders.put(constantValueAttributeReader.name, constantValueAttributeReader);
        // 忽略读取器
        AttributeReader deprecatedAttributeReader = new AttributeReader(names.Deprecated, V45_3, CLASS_OR_MEMBER_ATTRIBUTE) {
            void read(LzySymbol sym, int attrLen) {
                sym.flags_field |= LzyFlags.DEPRECATED;
            }
        };
        attributeReaders.put(deprecatedAttributeReader.name,deprecatedAttributeReader);
        // 异常读取器
        AttributeReader execeptionAttributeReader = new AttributeReader(names.Exceptions, V45_3, CLASS_OR_MEMBER_ATTRIBUTE) {
            void read(LzySymbol sym, int attrLen) {
                int nexceptions = nextChar();
                LzyList<LzyType> thrown = LzyList.nil();
                for (int j = 0; j < nexceptions; j++)
                    thrown = thrown.prepend(readClassSymbol(nextChar()).type);
                if (sym.type.thrown().isEmpty())
                    sym.type.asMethodType().thrown = thrown.reverse();
            }
        };
        attributeReaders.put(execeptionAttributeReader.name,execeptionAttributeReader);
        // 内部类读取器
        AttributeReader innerClassesAttributeReader = new AttributeReader(names.InnerClasses, V45_3, CLASS_ATTRIBUTE) {
            void read(LzySymbol sym, int attrLen) {
                LzyClassSymbol c = (LzyClassSymbol) sym;
                readInnerClasses(c);
            }
        };
        attributeReaders.put(innerClassesAttributeReader.name,innerClassesAttributeReader);
        // 局部变量表读取器
        AttributeReader localVariableTableAttributeReader = new AttributeReader(names.LocalVariableTable, V45_3, CLASS_OR_MEMBER_ATTRIBUTE) {
            void read(LzySymbol sym, int attrLen) {
                int newbp = bp + attrLen;
                if (saveParameterNames) {
                    // Pick up parameter names from the variable table.
                    // Parameter names are not explicitly identified as such,
                    // but all parameter name entries in the LocalVariableTable
                    // have a start_pc of 0.  Therefore, we record the name
                    // indicies of all slots with a start_pc of zero in the
                    // parameterNameIndicies array.
                    // Note that this implicitly honors the JVMS spec that
                    // there may be more than one LocalVariableTable, and that
                    // there is no specified ordering for the entries.
                    int numEntries = nextChar();
                    for (int i = 0; i < numEntries; i++) {
                        int start_pc = nextChar();
                        int length = nextChar();
                        int nameIndex = nextChar();
                        int sigIndex = nextChar();
                        int register = nextChar();
                        if (start_pc == 0) {
                            // ensure array large enough
                            if (register >= parameterNameIndices.length) {
                                int newSize = Math.max(register, parameterNameIndices.length + 8);
                                parameterNameIndices =
                                        Arrays.copyOf(parameterNameIndices, newSize);
                            }
                            parameterNameIndices[register] = nameIndex;
                            haveParameterNameIndices = true;
                        }
                    }
                }
                bp = newbp;
            }
        };
        attributeReaders.put(localVariableTableAttributeReader.name,localVariableTableAttributeReader);
        // 源文件读取器
        AttributeReader sourceFileAttributeReader = new AttributeReader(names.SourceFile, V45_3, CLASS_ATTRIBUTE) {
            void read(LzySymbol sym, int attrLen) {
                LzyClassSymbol c = (LzyClassSymbol) sym;
                LzyName n = readName(nextChar());
                c.sourcefile = n;
            }
        };
        attributeReaders.put(sourceFileAttributeReader.name,sourceFileAttributeReader);
        // 合成读取器
        AttributeReader syntheticAttributeReader = new AttributeReader(names.Synthetic, V45_3, CLASS_OR_MEMBER_ATTRIBUTE) {
            void read(LzySymbol sym, int attrLen) {
                // bridge methods are visible when generics not enabled
                if (allowGenerics || (sym.flags_field & LzyFlags.BRIDGE) == 0)
                    sym.flags_field |= LzyFlags.SYNTHETIC;
            }
        };
        attributeReaders.put(syntheticAttributeReader.name,syntheticAttributeReader);
    }

}

/**
 * 类型与签名
 */
 abstract class LzyReadTypes extends LzyConstantPoolAccess {

    protected LzySymbol currentOwner = null;

    // 作用域
    protected LzyScope typevars = new LzyScope(null);
    // 符号表: 存储所有的符号
    LzySymtab syms;


    // 开关
    protected boolean readingClassAttr = false;

    // 缓存
    byte[] signature;
    // 开始位置
    int sigp;
    // 结束位置
    int siglimit;
    boolean sigEnterPhase = false;

    private LzyList<LzyType> missingTypeVariables = LzyList.nil();

    public LzyReadTypes(LzyContext LzyContext) {
        super(LzyContext);

    }

    LzyType sigToType(byte[] sig, int offset, int len) {

        signature = sig;
        sigp = offset;
        siglimit = offset + len;
        return sigToType();
    }


    // 类型匹配: B --> Byte , C --> Char , D --> Double , V --> Void
    LzyType sigToType() {
        // 读取第一个字符: 判断类型
        switch ((char) signature[sigp]) {
            case 'T':
                // 位置自增
                sigp++;
                // 从下一个位置开始读
                int start = sigp;
                // 一直到';'
                while (signature[sigp] != ';') sigp++;
                sigp++;
                // 通过开始位置 和 结束位置，创建Name
                // 通过Name，在Scope中获取Type符号
                return sigEnterPhase
                        ? LzyType.noType
                        : findTypeVar(names.fromUtf(signature, start, sigp - 1 - start));
            case '+': {
                // 实现泛型: 可以忽略
          /*      sigp++;
                LzyType t = sigToType();
                return new Type.WildcardType(t, BoundKind.EXTENDS,
                        syms.boundClass);*/
                return null;
            }
            case '*':
                // 实现泛型: 可以忽略
/*                sigp++;
                return new Type.WildcardType(syms.objectType, BoundKind.UNBOUND,
                        syms.boundClass);*/
                return null;
            case '-': {
                // 实现泛型: 可以忽略
/*                sigp++;
                LzyType t = sigToType();
                return new Type.WildcardType(t, BoundKind.SUPER,
                        syms.boundClass);*/
                return null;
            }
            case 'B':
                // 这是一个Byte符号
                sigp++;
                return syms.byteType;
            case 'C':
                // 这是一个Char符号
                sigp++;
                return syms.charType;
            case 'D':
                // 这是一个Double符号
                sigp++;
                return syms.doubleType;
            case 'F':
                // 这是一个Float符号
                sigp++;
                return syms.floatType;
            case 'I':
                // 这是一个Int符号
                sigp++;
                return syms.intType;
            case 'J':
                // 这是一个Ｌｏｎｇ符号
                sigp++;
                return syms.longType;
            case 'L':
            {
                // int oldsigp = sigp;
                // 这是的引用数据类型的符号
                LzyType t = classSigToType();
                if (sigp < siglimit && signature[sigp] == '.'){
                    // throw badClassFile("deprecated inner class signature syntax " + "(please recompile from source)");
                    System.out.println("deprecated inner class signature syntax");
                }
                return t;
            }
            case 'S':
                // 这是一个Short符号
                sigp++;
                return syms.shortType;
            case 'V':
                // 这是一个Void符号
                sigp++;
                return syms.voidType;
            case 'Z':
                // 这是一个Boolean符号
                sigp++;
                return syms.booleanType;
            case '[':
                // 这是一个数组符号
                sigp++;
                // 递归进一步解析: 什么数组?
                return new LzyArrayType(sigToType(), syms.arrayClass);
            case '(':
                // 解析方法符号
                sigp++;
                // 方法参数符号列表
                LzyList<LzyType> argtypes = sigToTypes(')');
                // 返回值的符号
                LzyType restype = sigToType();
                // 解析抛出异常
                LzyList<LzyType> thrown = LzyList.nil();
                while (signature[sigp] == '^') {
                    sigp++;
                    thrown = thrown.prepend(sigToType());
                }
                // 创建方法符号
                return new LzyMethodType(argtypes,
                        restype,
                        thrown.reverse(),
                        syms.methodClass);
            default:
                // throw badClassFile("bad.signature", Convert.utf2string(signature, sigp, 10));
        }
        return null;
    }


    byte[] signatureBuffer = new byte[0];
    int sbp = 0;
    /** Convert class signature to type, where signature is implicit.
     */
    LzyType classSigToType() {
        if (this.signature[this.sigp] != 'L'){
            System.out.println("bad.class.signature");
            return null;
        }else{
            // 移动到下一个位置
            ++this.sigp;
            // 开始位置
            int startSigp = this.sigp;
            // 查找开始位置
            while (this.signature[this.sigp] != ';'&& this.signature[this.sigp] != '<' ){
                ++this.sigp;
            }
            // 创建符号
            LzyClassType classType = (LzyClassType)this.enterClass(this.names.fromUtf(internalize(this.signature, startSigp, this.sigp - startSigp))).type;
            // 如果结束符号是'<'
/*            if (this.signature[this.sigp] == '<'){
                // 当前类是内部类,获取外部类变量getEnclosingType();
                classType = new Type.ClassType(classType.getEnclosingType(),this.sigToTypes('>'),classType.tsym);
            }else if (classType.getTypeArguments().nonEmpty()){
                // 如果类型参数不为空: 存在泛型的情况! 忽略处理！
                // classType = (Type.ClassType)classType.tsym.erasure();
            }
            // 处理泛型的缓存: 可以忽略！
            if (classType.isParameterized()){

            }*/
            ++this.sigp;
            return classType;
        }
    }

    // 在typevars的范围内中查找,给定的类型变量
    LzyType findTypeVar(LzyName name) {
        // 通过Name在Scope中获取Type
        LzyScope.Entry e = typevars.lookup(name);
        if (e.scope != null) {
            return e.sym.type;
        } else {
            return null;
        }

    }



    // 将隐式签名 转换 签名列表 ---> 解析方法参数列表　---> 结束符号')'
    LzyList<LzyType> sigToTypes(char terminator) {
        LzyList<LzyType> head = LzyList.of(null);
        LzyList<LzyType> tail = head;
        // 指定分隔符进行解析 --> 形成链表
        while (signature[sigp] != terminator){
            // 将数据封装到List的head中
            tail = tail.setTail(LzyList.of(sigToType()));
        }

        sigp++;
        // 返回头节点
        return head.tail;
    }



    // 字符串的转义。所有的'/'符号变成'.'符号。
    public static byte[] internalize(byte[] buf, int offset, int len) {
        // 创建指定大小的字节数组
        byte[] translated = new byte[len];
        for (int j = 0; j < len; j++) {
            byte b = buf[offset + j];
            if (b == '/') translated[j] = (byte) '.';
            else translated[j] = b;
        }
        return translated;
    }


    @Override
    LzyType readType(int i) {
        // 获取常量项的开头
        int index = poolIdx[i];
        return sigToType(buf, index + 3, getChar(index + 1));
    }



    @Override
    Object readClassOrType(int i) {
        // 获取常量项的开始位置
        int index =  poolIdx[i];
        // 获取长度
        int len = getChar(index + 1);
        //
        int start = index + 3;
        // Assert.check(buf[start] == '[' || buf[start + len - 1] != ';');
        // by the above assertion, the following test can be
        // simplified to (buf[start] == '[')
        return (buf[start] == '[' || buf[start + len - 1] == ';')
                ? (Object)sigToType(buf, start, len)
                : (Object)enterClass(names.fromUtf(internalize(buf, start,
                len)));
    }

    public abstract LzyClassSymbol enterClass(LzyName flatname);
    public abstract LzyClassSymbol enterClass(LzyName name, LzyTypeSymbol owner);

}


/**
 * 常量池
 */

abstract class LzyConstantPoolAccess extends LzyBufferAccess {
    // 存储常量项的位置 --> 在buf中的下标
    int[] poolIdx;
    // 常量池
    Object[] poolObj;
    // Names
    LzyTable names;


    public LzyConstantPoolAccess(LzyContext LzyContext){
        this.names = LzyTable.instance(LzyContext);
    }

    /**
     * 跳过常量池
     */
    void indexPool() {
        // 初始化常量池
        poolIdx = new int[nextChar()]; // 存储索引下标
        poolObj = new Object[poolIdx.length]; // 存储常量项
        // 注意： 常量池是从1开始的,不是从0开始的。
        int i = 1;
        while (i < poolIdx.length) {
            // 存储在文件中的位置
            poolIdx[i++] = bp;
            // 第一个字节: 存储常量项的类型
            byte tag = buf[bp++];
            switch (tag) {
                case LzyClassFile.CONSTANT_Utf8: case LzyClassFile.CONSTANT_Unicode: {
                    // 大小
                    int len = nextChar();
                    // 跳过
                    bp = bp + len;
                    break;
                }
                case LzyClassFile.CONSTANT_Class:
                case LzyClassFile.CONSTANT_String:
                case LzyClassFile.CONSTANT_MethodType:
                    // 跳过
                    bp = bp + 2;
                    break;
                case LzyClassFile.CONSTANT_MethodHandle:
                    // 跳过
                    bp = bp + 3;
                    break;
                case LzyClassFile.CONSTANT_Fieldref:
                case LzyClassFile.CONSTANT_Methodref:
                case LzyClassFile.CONSTANT_InterfaceMethodref:
                case LzyClassFile.CONSTANT_NameandType:
                case LzyClassFile.CONSTANT_Integer:
                case LzyClassFile.CONSTANT_Float:
                case LzyClassFile.CONSTANT_InvokeDynamic:
                    // 跳过
                    bp = bp + 4;
                    break;
                case LzyClassFile.CONSTANT_Long:
                case LzyClassFile.CONSTANT_Double:
                    // 跳过
                    bp = bp + 8;
                    i++;
                    break;
                default:
                    // throw badClassFile("bad.const.pool.tag.at", Byte.toString(tag), Integer.toString(bp -1));
            }
        }
    }


    /**
     * 从常量池中读取数据
     * @param i
     * @return
     */
    Object readPool(int i) {
        // 根据下标获取常量项
        Object result = poolObj[i];
        if (result != null) return result;
        // 获取常量项的位置
        int index = poolIdx[i];
        if (index == 0) return null;

        // 常量项类型
        byte tag = buf[index];
        // 根据类型读取数据
        switch (tag) {
            case LzyClassFile.CONSTANT_Utf8:
                // 存储Name
                poolObj[i] = names.fromUtf(buf, index + 3, getChar(index + 1));
                break;
            case LzyClassFile.CONSTANT_Unicode:
                // throw badClassFile("unicode.str.not.supported");
                System.out.println("unicode.str.not.supported");
                return null;
            case LzyClassFile.CONSTANT_Class:
                poolObj[i] = readClassOrType(getChar(index + 1));
                break;
            case LzyClassFile.CONSTANT_String:
                // FIXME: (footprint) do not use toString here
                // 存储Name
                poolObj[i] = readName(getChar(index + 1)).toString();
                break;
            case LzyClassFile.CONSTANT_Fieldref: {
                LzyClassSymbol owner = readClassSymbol(getChar(index + 1));
                LzyClassFile.NameAndType nt = (LzyClassFile.NameAndType)readPool(getChar(index + 3));
                poolObj[i] = new LzyVarSymbol(0, nt.name, nt.type, owner);
                break;
            }
            case LzyClassFile.CONSTANT_Methodref:
            case LzyClassFile.CONSTANT_InterfaceMethodref: {
                LzyClassSymbol owner = readClassSymbol(getChar(index + 1));
                LzyClassFile.NameAndType nt = (LzyClassFile.NameAndType)readPool(getChar(index + 3));
                poolObj[i] = new LzyMethodSymbol(0, nt.name, nt.type, owner);
                break;
            }
            case LzyClassFile.CONSTANT_NameandType:
                poolObj[i] = new LzyClassFile.NameAndType(
                        readName(getChar(index + 1)),
                        readType(getChar(index + 3)));
                break;
            case LzyClassFile.CONSTANT_Integer:
                poolObj[i] = getInt(index + 1);
                break;
            case LzyClassFile.CONSTANT_Float:
                poolObj[i] = new Float(getFloat(index + 1));
                break;
            case LzyClassFile.CONSTANT_Long:
                poolObj[i] = new Long(getLong(index + 1));
                break;
            case LzyClassFile.CONSTANT_Double:
                poolObj[i] = new Double(getDouble(index + 1));
                break;
            case LzyClassFile.CONSTANT_MethodHandle:
                // 向后跳4字节
                skipBytes(4);
                break;
            case LzyClassFile.CONSTANT_MethodType:
                // 向后跳3字节
                skipBytes(3);
                break;
            case LzyClassFile.CONSTANT_InvokeDynamic:
                // 向后跳5字节
                skipBytes(5);
                break;
            default:
                // throw badClassFile("bad.const.pool.tag", Byte.toString(tag));
        }
        // 返回常量池的常量项
        return poolObj[i];
    }

    void skipBytes(int n) {
        bp = bp + n;
    }

    LzyName readName(int i) {
        return (LzyName) (readPool(i));
    }

    LzyClassSymbol readClassSymbol(int i) {
        return (LzyClassSymbol) (readPool(i));
    }

    abstract LzyType readType(int i) ;
    abstract Object readClassOrType(int i) ;

}

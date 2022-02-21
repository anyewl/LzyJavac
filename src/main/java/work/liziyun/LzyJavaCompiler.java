package work.liziyun;



import work.liziyun.ast.LexerBuffer;
import work.liziyun.ast.LzyJavacParser;
import work.liziyun.code.LzyClassWriter;
import work.liziyun.code.symbol.LzyClassSymbol;
import work.liziyun.comp.*;
import work.liziyun.code.LzyClassReader;
import work.liziyun.tree.LzyJCCompilationUnit;

import work.liziyun.tree.LzyTreeMaker;
import work.liziyun.tree.state.LzyJCClassDef;
import work.liziyun.util.LzyContext;
import work.liziyun.util.LzyList;
import work.liziyun.util.LzyListBuffer;
import work.liziyun.world.LzyTable;
import java.io.*;
import java.util.Scanner;

public class LzyJavaCompiler implements LzyClassReader.SourceCompleter {


    private work.liziyun.util.LzyContext LzyContext;

    private LzyTreeMaker treeMaker;

    private LzyTable names;

    private LzyEnter enter;

    private LzyClassReader classReader;

    private LzyAttr attr;

    private LzyTodo todo;

    private LzyGen gen;

    private LzyClassWriter classWriter;

    public LzyJavaCompiler(LzyContext LzyContext){
        this.LzyContext = LzyContext;
        this.treeMaker = LzyTreeMaker.instance(LzyContext);
        this.names = LzyTable.instance(LzyContext);
        this.enter = LzyEnter.instance(LzyContext);
        this.classReader = LzyClassReader.instance(LzyContext);
        // 设置路径: 注意结束符号 classReader.bootclassPath
        // 源码路径: 注意结束符号 classReader.sourceClassPath
        // 设置填充器
        classReader.sourceCompleter = this;
        // 引用消除
        this.attr = LzyAttr.instance(LzyContext);
        // 承上启下的组件
        this.todo = LzyTodo.instance(LzyContext);
        //
        this.gen = LzyGen.instance(LzyContext);

        this.classWriter = LzyClassWriter.instance(LzyContext);
    }

    public LzyList compile(LzyList LzyList) throws Exception {
        // 抽象语法树建立
        LzyList<String> allPath = LzyList;
        LzyListBuffer<LzyJCCompilationUnit> compilationUnitListBuffer = new LzyListBuffer();
        while ( allPath.nonEmpty() ){
            try {
                compilationUnitListBuffer.append( this.parse(allPath.head) );
            } catch (IOException e) {
                e.printStackTrace();
            }
            // 下一个
            allPath = allPath.tail;
        }
        // 符号填充
        enter.complete(compilationUnitListBuffer.toList(),null);
        // 引用消除
        while (todo.nonEmpty()){
            LzyEnv env = (LzyEnv)todo.remove();
            attr.attribClass(env.tree.pos,env.enclClass.sym);
            // Gen生成可寻址实体 + 写入字节码IO流
            LzyJCClassDef classDef = (LzyJCClassDef) env.tree;
            genCode(env,classDef);
        }
        return null ;
    }

    void genCode(LzyEnv env, LzyJCClassDef classDef) throws Exception {
        try{
            // 可寻址指令处理
            if (this.gen.genClass(env,classDef)){
                // 建立IO流生成字节码
                this.classWriter.writeClass( classDef.sym );
            }
        }catch (Exception e){
            throw e;
        }
    }



    public static String read(InputStream inputStream) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String temp;
        StringBuilder sb = new StringBuilder();
        while ( (temp = in.readLine()) != null ){
            sb.append(temp);
        }
        in.close();
        return sb.toString();
    }


    public LzyJCCompilationUnit parse(String path) throws IOException {
        return parse(path,new FileInputStream(new File(path)));
    }


    public LzyJCCompilationUnit parse(String path,InputStream inputStream) {
        // 默认: 空语法树
        LzyJCCompilationUnit compilationUnit = this.treeMaker.CompilationUnit(null, LzyList.nil());
        if ( inputStream != null ){
            // 词法解析器
            LexerBuffer lexerBuffer = null;
            try {
                lexerBuffer = new LexerBuffer(LzyContext, read(inputStream).toCharArray(),path);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // 关闭流
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // 移动到第一个词
            lexerBuffer.nextToken();
            // 解析抽象语法树
            LzyJavacParser lzyJavacParser = new LzyJavacParser(lexerBuffer, LzyTreeMaker.instance(LzyContext), LzyTable.instance(LzyContext));
            compilationUnit = lzyJavacParser.parseCompilationUnit();
        }
        // 设置文件路径
        compilationUnit.sourcefile = this.names.fromString(path);
        return compilationUnit;
    }


    /**
     * 被动触发的Java源文件: 1. 抽象语法树的建立  2. 符号的填充
     * @param classSymbol
     * @param sourcePath
     * @param inputStream
     *
     *  am
     */
    @Override
    public void complete(LzyClassSymbol classSymbol , String sourcePath, InputStream inputStream){
        // 抽象语法树的建立
        System.out.println("LzyJavaCompiler.complete(): 成功触发其他源文件的编译!");
        LzyJCCompilationUnit compilationUnit = this.parse(sourcePath, inputStream);
        // 符号的填充
        this.enter.complete(LzyList.of(compilationUnit),classSymbol);
        // 填充失败
        if ( this.enter.getEnv(classSymbol) == null){
            throw new LzyClassReader.BadClassFile(classSymbol,"file.doesnt.contain.class "+classSymbol.fullName());
        }
    }
    // 系统下级符号
    private static String separator = File.separator;
    // 系统分割符号
    private static String pathSeparator = File.pathSeparator;

    public static void main(String[] args) throws Exception {

        Scanner sc = new Scanner(System.in);
        // 设置编译器路径
        System.setProperty("env.class.path",System.getProperty("user.dir")+separator+"test_javac"+separator+pathSeparator );
        // 系统类路径
        System.setProperty("sun.boot.class.path",System.getProperty("env.class.path").substring(0,System.getProperty("env.class.path").length()-1)+"rt.jar"+pathSeparator);
        System.out.println( "请检查jdk类库路径:" + System.getProperty("env.class.path") );
        System.out.println( "请检查项目路径: " + System.getProperty("sun.boot.class.path") );
        LzyJavaCompiler lzyJavaCompiler = new LzyJavaCompiler(new LzyContext());
        System.out.println("========================================================");
        System.out.println("位置：./LzyJavaCompile1.5/test_javac");
        System.out.println("有包格式：pojo.Student.java");
        System.out.println("无包格式：Student.java");
        System.out.println("========================================================");
        LzyList<String> list = LzyList.nil();
        while (true){
            System.out.println("请输入编译的文件:");
            String javaFileName = sc.next();
            javaFileName = javaFileName.substring(0,javaFileName.lastIndexOf('.')).replace('.',File.separatorChar)+".java";
            list = list.append(System.getProperty("user.dir")+separator+"test_javac"+separator+javaFileName);
            System.out.println("是否，继续？(y/n)");
            String yes = sc.next();
            if (!yes.equals("y")){
                break;
            }
        }
        System.out.println("=======================开始编译===========================");
        lzyJavaCompiler.compile( list );
    }
}

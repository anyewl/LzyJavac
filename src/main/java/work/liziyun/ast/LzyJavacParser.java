package work.liziyun.ast;

import work.liziyun.code.type.LzyTypeTags;
import work.liziyun.tag.LzyFlags;
import work.liziyun.tree.*;
import work.liziyun.tree.state.LzyJCBlock;
import work.liziyun.tree.state.LzyJCClassDef;
import work.liziyun.tree.state.LzyJCExpressionStatement;
import work.liziyun.tree.state.LzyJCVarDef;
import work.liziyun.util.LzyList;
import work.liziyun.util.LzyListBuffer;
import work.liziyun.world.LzyName;
import work.liziyun.world.LzyTable;
import work.liziyun.world.LzyToken;
import static work.liziyun.world.LzyToken.*;


public class LzyJavacParser extends LzyStatementParser implements Parser  {

    public LzyJavacParser(LexerBuffer s, LzyTreeMaker f, LzyTable names) {
        super(s, f, names);
    }


    @Override
    public LzyJCCompilationUnit parseCompilationUnit() {
        // 第一个词的位置
        int pos = this.S.pos();
        // 包节点
        LzyJCTree.JCExpression pid = null;
        // 修饰符节点
        LzyJCModifiers mods = null;
        // 包
        if (this.S.token() == LzyToken.PACKAGE){
            this.S.nextToken();
            pid = this.qualident();
            this.S.accept(SEMI);
        }
        // 树节点
        LzyListBuffer<LzyJCTree> defs = new LzyListBuffer();
        // 是否检查导包语法: 检查
        boolean checkForImports = true;
        while ( this.S.token() != EOF ){
            // 当发生错误时，跳过这个错词！
            if ( this.S.pos() <= this.S.getErrorEndPos() ){
                // error recovery 将跳过一些词: 类外，import词将跳过！
                this.S.skip(checkForImports, false, false, false);
                if (S.token() == EOF)
                    break;
            }
            // 核心
            if ( checkForImports && mods == null && this.S.token() == IMPORT ){
                defs.append(importDeclaration());
            }else{
                // 处理class
                LzyJCTree def = typeDeclaration(mods);
                // 如果是一条语句: 拿出其中表达式
                if ( def instanceof LzyJCExpressionStatement){
                    def = ((LzyJCExpressionStatement)def).expr;
                }
                defs.append(def);
                if (def instanceof  LzyJCClassDef ){
                    checkForImports = false;
                }
                mods = null;
            }
        }
        // 1.
        LzyJCCompilationUnit jcCompilationUnit = this.F.at(pos).CompilationUnit(pid, defs.toList());
        return jcCompilationUnit;
    }

    @Override
    public LzyJCTree.JCExpression parseExpression() {
        return null;
    }

    @Override
    public LzyJCTree.JCStatement parseStatement() {

        return null;
    }

    @Override
    public LzyJCTree.JCExpression parseType() {
        return term(TYPE);
    }

    /**
     * 导包
     * @return
     */
    protected LzyJCTree importDeclaration() {
        int pos = this.S.pos();
        this.S.nextToken();
        boolean isStatic = false;
        // 尝试创建
        LzyJCTree.JCExpression exp1 = (LzyJCTree.JCExpression) this.F.at(this.S.pos()).Ident(this.S.ident());
        do {
            int pos2 = S.pos();
            this.S.accept(LzyToken.DOT);
            if ( this.S.token() == LzyToken.STAR ){
                exp1 = (LzyJCTree.JCExpression)  this.F.at(pos2).Select(exp1,this.names.asterisk) ;
                this.S.nextToken();
                break;
            }
            exp1 = (LzyJCTree.JCExpression) this.F.at(pos2).Select(exp1,this.S.ident()) ;
        }while ( this.S.token() == LzyToken.DOT );
        this.S.accept(SEMI);
        return this.F.at(pos).Import(exp1);
    }


    /** TypeDeclaration = ClassOrInterfaceOrEnumDeclaration
     *                  | ";"
     */
    LzyJCTree typeDeclaration(LzyJCModifiers mods) {
        int pos = S.pos();
        if (mods == null && S.token() == SEMI) {
            S.nextToken();
            return this.F.at(pos).Skip();
        } else {
            return classOrInterfaceOrEnumDeclaration(this.modifiersOpt(mods), null);
        }
    }

    public LzyJCTree.JCStatement classOrInterfaceOrEnumDeclaration(LzyJCModifiers mods, String dc){
        if ( this.S.token() == CLASS ){
            return this.classDeclaration(mods,dc);
        }else if ( this.S.token() == INTERFACE ) {
            return this.interfaceDeclaration(mods,dc);
        }else{
            return null;
        }
    }

    /**
     * 类
     * @param mods
     * @param dc
     * @return
     */
    LzyJCClassDef classDeclaration(LzyJCModifiers mods, String dc){
        int pos = this.S.pos();
        this.S.accept(CLASS);
        LzyName name = this.S.ident();
        // 不处理泛型: typeParameterOpt()
        LzyList<LzyJCTypeParameter> typarams = LzyList.nil();
        // 继承
        LzyJCTree.JCExpression extending = null;
        if (S.token() == EXTENDS) {
            S.nextToken();
            extending = parseType();
        }
        // 接口实现
        LzyList<LzyJCTree.JCExpression> implementing = LzyList.nil();
        if (S.token() == IMPLEMENTS) {
            S.nextToken();
            implementing = this.typeList();
        }
        // 类体
        LzyList<LzyJCTree> defs = classOrInterfaceBody(name, false);
        // 创建
        LzyJCClassDef result = this.F.at(pos).ClassDef(
                mods.flags, name, typarams, extending, implementing, defs);
        return result;
    }


    LzyJCClassDef interfaceDeclaration(LzyJCModifiers mods, String dc) {
        int pos = this.S.pos();
        this.S.accept(INTERFACE);
        LzyName name = this.S.ident();
        // 泛型 typeParametersOpt();
        LzyList<LzyJCTypeParameter> typarams = LzyList.nil();
        // 继承
        LzyList<LzyJCTree.JCExpression> extending = LzyList.nil();
        if (S.token() == EXTENDS) {
            S.nextToken();
            extending = this.typeList();
        }
        // 接口体
        LzyList<LzyJCTree> defs = classOrInterfaceBody(name, true);
        LzyJCClassDef result = this.F.at(pos).ClassDef(
                mods.flags|LzyFlags.INTERFACE, name, typarams, null, extending, defs);
        return  result;
    }


    LzyList<LzyJCTree> classOrInterfaceBody(LzyName var1, boolean var2) {
        int var3 = this.S.pos();
        // 符号'{'
        this.S.accept(LzyToken.LBRACE);
        LzyListBuffer var4 = new LzyListBuffer();

        while(this.S.token() != LzyToken.RBRACE && this.S.token() != LzyToken.EOF) {
            var4.appendList(this.classOrInterfaceBodyDeclaration(var1, var2));
        }
        //符号'}'
        this.S.accept(LzyToken.RBRACE);
        // 尝试结束
        this.S.tryEndOf();
        return var4.toList();
    }

    LzyList<LzyJCTree> classOrInterfaceBodyDeclaration(LzyName className, boolean isInterface){
        if ( S.token() == SEMI ){
            S.nextToken();
            return LzyList.nil();
        }else{
            int pos = this.S.pos();
            LzyJCModifiers mods = this.modifiersOpt(null);
            // 类中有类的情况: 递归
            if ( S.token() == CLASS || S.token() == INTERFACE ) {
                return LzyList.<LzyJCTree>of( classOrInterfaceOrEnumDeclaration(mods,null) );
            } else if (S.token() == LBRACE && !isInterface &&
                    (mods.flags & LzyFlags.StandardFlags & ~LzyFlags.STATIC) == 0 ) {// 静态代码块
                return LzyList.of(this.block(pos,mods.flags));
            } else {
                pos = this.S.pos();
                // 泛型: 不处理
                LzyList<LzyJCTypeParameter> typarams = LzyList.nil();
                // 名称
                LzyName name = this.S.name();
                // 位置
                pos = this.S.pos();
                // 类型
                LzyJCTree.JCExpression type;
                boolean isVoid = this.S.token() == VOID;
                // 如果类型: void
                if ( isVoid ){
                    type = this.F.at(pos).TypeIdent(LzyTypeTags.VOID);
                    this.S.nextToken();
                } else {
                    type = this.parseType();
                }
                // 可能是构造方法: 类型(
                // 1. '('
                // 2. 不是接口
                // 3. 上一个词是标识符
                if ( this.S.token() == LPAREN && !isInterface && type.getTag() == LzyJCTree.IDENT){
                    // 构造方法
                    return LzyList.of(this.methodDeclaratorRest(pos,mods,null,this.names.init,typarams,isInterface,true,null));
                } else {
                    // 名称 --> 可能是变量名,也可能是方法名
                    pos = this.S.pos();
                    name = this.S.ident();
                    // 方法
                    if ( this.S.token() == LPAREN ){
                        return LzyList.of(this.methodDeclaratorRest(pos,mods,type,name,typarams,isInterface,isVoid,null));
                    }else if (!isVoid){// 变量
                        LzyList LzyList = this.variableDeclaratorsRest(pos, mods, type, name, isInterface, null);
                        this.S.accept(SEMI);
                        return LzyList;
                    }else {
                        // 错误情况
                        return LzyList.nil();
                    }
                }
            }
        }
    }


    /** MethodDeclaratorRest =
     *      FormalParameters BracketsOpt [Throws TypeList] ( MethodBody | [DEFAULT AnnotationValue] ";")
     *  VoidMethodDeclaratorRest =
     *      FormalParameters [Throws TypeList] ( MethodBody | ";")
     *  InterfaceMethodDeclaratorRest =
     *      FormalParameters BracketsOpt [THROWS TypeList] ";"
     *  VoidInterfaceMethodDeclaratorRest =
     *      FormalParameters [THROWS TypeList] ";"
     *  ConstructorDeclaratorRest =
     *      "(" FormalParameterListOpt ")" [THROWS TypeList] MethodBody
     */
    LzyJCTree methodDeclaratorRest(int pos,
                                LzyJCModifiers mods,
                                LzyJCTree.JCExpression rstype,
                                LzyName name,
                                LzyList<LzyJCTypeParameter> typarams,
                                boolean isInterface, boolean isVoid,
                                String dc) {
        // 可能处理数组
        if (!isVoid) rstype = bracketsOpt(rstype);
        // 方法的参数列表: (A a , B b)
        LzyList<LzyJCVarDef> params = formalParameters();

        // 抛出异常
        LzyList<LzyJCTree.JCExpression> thrown = LzyList.nil();
/*        if (S.token() == THROWS) {
            S.nextToken();
            thrown = qualidentList();
        }*/
        LzyJCBlock body = null;
        LzyJCTree.JCExpression defaultValue = null; // 接口中的默认方法
        // 方法体
        if (S.token() == LBRACE) {
            body = this.block(S.pos(), 0);
            defaultValue = null;
        } else {
            // 处理接口中的默认方法
            if (S.token() == DEFAULT) {
                this.S.accept(DEFAULT);
            }
            this.S.accept(SEMI);
            body = null;
        }

        LzyJCMethodDef result = this.F.at(pos).MethodDef(mods.flags, name, rstype, typarams,
                        params, thrown,
                        body);
        return result;
    }


    /** FormalParameters = "(" [ FormalParameterList ] ")"
     *  FormalParameterList = [ FormalParameterListNovarargs , ] LastFormalParameter
     *  FormalParameterListNovarargs = [ FormalParameterListNovarargs , ] FormalParameter
     */
    LzyList<LzyJCVarDef> formalParameters() {
        LzyListBuffer<LzyJCVarDef> params = new LzyListBuffer<LzyJCVarDef>();
        LzyJCVarDef lastParam = null;
        this.S.accept(LPAREN);
        if (S.token() != RPAREN) {
            params.append(lastParam = formalParameter());
            // 注意: VARARGS这里在处理可变参数
            while ((lastParam.flags & LzyFlags.VARARGS) == 0 && S.token() == COMMA) {
                S.nextToken();
                params.append(lastParam = formalParameter());
            }
        }
        this.S.accept(RPAREN);
        return params.toList();
    }

    /** FormalParameter = { FINAL | '@' Annotation } Type VariableDeclaratorId
     *  LastFormalParameter = { FINAL | '@' Annotation } Type '...' Ident | FormalParameter
     */
    LzyJCVarDef formalParameter() {
        // 方法参数中: 变量的修饰符
        LzyJCModifiers mods = optFinal(LzyFlags.PARAMETER);
        // 变量类型
        LzyJCTree.JCExpression type = parseType();
        // 符号'...',是否可变参数
/*        if (S.token() == ELLIPSIS) {
            // checkVarargs();
            mods.flags |= Flags.VARARGS;
            type = this.F.at(S.pos()).TypeArray(type);
            S.nextToken();
        }*/
        // 变量的创建
        return variableDeclaratorId(mods, type);
    }


    LzyJCVarDef variableDeclaratorId(LzyJCModifiers var1, LzyJCTree.JCExpression var2) {
        int var3 = this.S.pos();
        LzyName var4 = this.S.ident();
        // 存在数组的情况
        if ((var1.flags & 17179869184L) == 0L) {
            var2 = this.bracketsOpt(var2);
        }
        // 直接创建变量
        return this.F.at(var3).VarDef(var1.flags, var4, var2, (LzyJCTree.JCExpression) null);
    }

}

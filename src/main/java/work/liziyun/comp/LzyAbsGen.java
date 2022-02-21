package work.liziyun.comp;



import work.liziyun.code.LzyCode;
import work.liziyun.code.LzyPool;
import work.liziyun.code.LzySymtab;
import work.liziyun.code.symbol.LzyClassSymbol;
import work.liziyun.code.symbol.LzyMethodSymbol;
import work.liziyun.code.symbol.LzySymbol;
import work.liziyun.code.symbol.LzyVarSymbol;
import work.liziyun.code.type.LzyMethodType;
import work.liziyun.code.type.LzyType;
import work.liziyun.code.type.LzyTypeTags;
import work.liziyun.jvm.LzyTarget;
import work.liziyun.code.LzyClassReader;
import work.liziyun.tag.LzyByteCodes;
import work.liziyun.tag.LzyFlags;
import work.liziyun.tag.LzyKinds;
import work.liziyun.tree.*;
import work.liziyun.tree.express.LzyJCBinary;
import work.liziyun.tree.express.LzyJCConditional;
import work.liziyun.tree.state.*;
import work.liziyun.util.LzyAssert;
import work.liziyun.util.LzyContext;
import work.liziyun.util.LzyList;
import work.liziyun.util.LzyListBuffer;
import work.liziyun.world.LzyName;
import work.liziyun.world.LzyTable;
import java.util.Map;


public abstract class LzyAbsGen extends LzyVisitor implements LzyFlags, LzyKinds, LzyTypeTags, LzyByteCodes {
    private static final LzyContext.Key genkey = new LzyContext.Key();
    final LzyTable names;
    final LzySymtab syms;
    final LzyCheck chk;
    final LzyResolve rs;
    final LzyTreeMaker make;
    final LzyTarget target;
    final LzyMethodType methodType;
    // 编译单元
    LzyJCCompilationUnit compilationUnit;
    // 常量池
    LzyPool pool = new LzyPool();

    LzyEnv attrEnv;
    //
    int nerrs = 0;
    LzyEnv env;
    //
    LzyType pt;
    // 所有的可寻址节点
    LzyItems items;
    // 可寻址节点
    LzyItems.Item result;
    // Code属性
    LzyCode code;


     LzyAbsGen(LzyContext LzyContext){
        LzyContext.put(genkey,this);
        this.names = LzyTable.instance(LzyContext);
        this.syms = LzySymtab.instance(LzyContext);
        this.chk = LzyCheck.instance(LzyContext);
        this.rs = LzyResolve.instance(LzyContext);
        this.make = LzyTreeMaker.instance(LzyContext);
        this.target = LzyTarget.instance(LzyContext);
        this.methodType = new LzyMethodType(null,null,null,this.syms.methodClass);

    }





    public void genStats(LzyList< ? extends LzyJCTree> LzyList , LzyEnv env){
        LzyList<? extends LzyJCTree> list1 = LzyList;
        while (list1.nonEmpty()){
            this.genStat(list1.head,env);
            list1 = list1.tail;
        }
    }


    /**
     *
     * @param argJCTreeList 参数列表所有的树节点
     * @param argTypeList   参数列表所有的类型
     */
    public void genArgs(LzyList<LzyJCTree> argJCTreeList,LzyList<LzyType> argTypeList){
        LzyList<LzyJCTree> argJCList = argJCTreeList;
        while ( argJCList.nonEmpty() ){
            // 表达式
            this.genExpr( argJCList.head , argTypeList.head ).load();
            // 下一个
            argJCList = argJCList.tail;
            argTypeList = argTypeList.tail;
        }

        // 说明参数列表的 节点个数 和 类型个数 不一致！ 出现错误!
        if ( !argTypeList.isEmpty() ){
            throw new AssertionError();
        }

    }


    public static int one(int var0) {
        return zero(var0) + 1;
    }

    public static int zero(int var0) {
        switch(var0) {
            case 0:
            case 5:
            case 6:
            case 7:
                return 3;
            case 1:
                return 9;
            case 2:
                return 11;
            case 3:
                return 14;
            case 4:
            default:
                throw new AssertionError("zero");
        }
    }

     void genNullCheck(int pos) {
        this.callMethod(pos, this.syms.objectType, this.names.getClass, LzyType.emptyList, false);
        this.code.emitop(pop);
    }


    public void genStat(LzyJCTree jcTree , LzyEnv<GenContext> env){
        // 代码可达
        if ( this.code.isAive()  ){
            this.genDef( jcTree , env  );
        }else if (  ((GenContext)env.info).isSwitch && jcTree.tag == LzyJCTree.VARDEF ){
            // 变量符号
            this.code.newLocal( ((LzyJCVarDef)jcTree).sym );
        }
    }




    /**
     *
     * @param loop
     * @param body
     * @param cond
     * @param step
     * @param testFirst 当while和for时为true,当do...while时为false。 循环体和判断条件的先后。
     *
     */
    void genLoop(LzyJCTree.JCStatement loop , LzyJCTree.JCStatement body, LzyJCTree.JCExpression cond , LzyList<LzyJCExpressionStatement> step , boolean testFirst){
        LzyEnv<GenContext> loopEnv = this.env.dup(loop, new GenContext());
        // 当前字节码指令的位置: 开始循环前的字节码位置
        int startPc = this.code.entryPoint();
        // 条件可寻址实体
        LzyItems.CondItem condItem;
        if (testFirst){
            // 有循环条件的情况
            if ( cond != null ){
                // 生成条件的可寻址实体
                condItem = this.genCond(LzyTreeInfo.skipParens(cond));
            }else {// 死循环生成一个循环条件: for语句的循环条件为空的情况
                // 生成条件的可寻址实体
                condItem = items.makeCondItem(goto_);
            }
            // 循环结束的回填地址: 应该在循环字节码后面
            LzyCode.Chain loopDone = condItem.jumpFalse();
            // 回填位置已经确定: 开始回填
            code.resolve(condItem.trueJumps);
            // 生成语句
            this.genStat( body , loopEnv  );
            // 开始回填: contine地方,这个地方回填应该跳到startPc处!
            code.resolve( loopEnv.info.cont );
            genStats(step,loopEnv);
            // 开始回填： 无条件跳转到循环开始处，既startPc
            code.resolve( code.branch(goto_) , startPc  );
            // 循环的字节码已经生成: 开始回填循环结束
            code.resolve(loopDone);
        }else {
            // 生成语句
            this.genStat( body , loopEnv  );
            //
            this.code.resolve(loopEnv.info.cont);
            //
            genStats(step,loopEnv);
            //
            LzyItems.CondItem c = null;
            if ( cond != null  ){

                c = genCond( LzyTreeInfo.skipParens(cond) );
            }else {
                c = items.makeCondItem(goto_);
            }
            // 开始回填: 成功的跳转
            code.resolve( c.jumpTrue(),startPc );
            // 开始回填: 失败的跳转
            code.resolve( c.falseJumps );
        }
        // 开始回填: 跳出循环的地方 ---> 这个回填与loopDone循环完成的回填相同!
        this.code.resolve(loopEnv.info.exit);
    }


    /**
     * 生成赋值语句的字节码,左边的赋值节点已经生成可寻址实体并且在操作数栈中
     * @param lhs 赋值语句左边
     * @param rhs 赋值语句右边
     * @param operator 运算符号
     * @return
     */
    LzyItems.Item completeBinop( LzyJCTree lhs , LzyJCTree rhs , LzyMethodSymbol.OperatorSymbol operator ){
        // 运算类型
        LzyMethodType methodType = (LzyMethodType)operator.type;
        // 运算码
        int opcode = operator.opcode;
        // 如果跟0进行关系运算
        if (opcode >= if_icmpeq && opcode <= if_icmple &&
            rhs.type.constValue instanceof Number &&
                ((Number) rhs.type.constValue).intValue() == 0
        ){
            // 操作码偏移到与0运算的区域中
            opcode = opcode + (ifeq - if_icmpeq);
        // 如果跟null进行关系运算
        }else if (opcode >= if_acmpeq && opcode <= if_acmpne && LzyTreeInfo.isNull(rhs) ){
            opcode = opcode + (if_acmp_null - if_acmpeq);
        }else {
            // 赋值语句右节点的预期操作数类型: 方法的形式参数的第二个
            LzyType rType = ((LzyMethodType)operator.type).argtypes.tail.head;
            // 如果是移位操作
            if ( opcode >= ishll && opcode <= lushrl ){
                // 偏移操作
                opcode = opcode + ( ishl - ishll );
                // 期望右边是int类型
                rType = syms.intType;
            }
            // 生成赋值语句右边的字节码指令: 核心!!!!!
            genExpr(rhs,rType).load();
            // 如果有两条连续的字节码指令,发射第一个.
            if ( opcode >= (1 << preShift) ){
                this.code.emitop(opcode >> preShift);
                opcode = opcode & 0xFF;
            }
        }
        // 如果比较运算 和 null判断
        if ( opcode >= ifeq && opcode <= if_acmpne ||
            opcode == if_acmp_null || opcode == if_acmp_nonnull
        ){
            // 生成一个条件可寻址实体
            return this.items.makeCondItem(opcode);
        }else {
            this.code.emitop(opcode);
            // 生成一个基本数据类型的可寻址实体
            return this.items.makeStackItem(methodType.restype);
        }
    }


    /**
     * -1加载到操作数栈上
     * @param tc
     */
    void emitMinusOne(int tc){
        if ( tc == 1 ){
            // 常量-1加载到操作数栈上
            this.items.makeImmediateItem(this.syms.longType , new Long(-1) ).load();
        } else {
            // -1压入栈顶
            this.code.emitop(iconst_m1);
        }
    }




    public LzyItems.CondItem genCond(LzyJCTree jcTree  ){
        // 去除外部括号
        LzyJCTree inner_tree = LzyTreeInfo.skipParens(jcTree);
        // 条件表达式
        if ( inner_tree.tag == LzyJCTree.CONDEXPR ){
            // 条件
            LzyJCConditional conditional = (LzyJCConditional)inner_tree;
            // 可寻址实体
            LzyItems.CondItem condItem = this.genCond(conditional.cond);
            LzyItems.CondItem result ;
            if ( condItem.isTrue() ){
                // 回填
                this.code.resolve(condItem.trueJumps);
                // 条件
                result = this.genCond(conditional.cond);
                return result;
            } else if ( condItem.isFalse() ){
                // 回填
                this.code.resolve(condItem.falseJumps);
                // 条件
                result  = this.genCond(conditional.falsepart);
                return result;
            }else {
                LzyCode.Chain secondJumps = condItem.jumpFalse();
                // 回填成功
                this.code.resolve( condItem.trueJumps );
                LzyItems.CondItem first = this.genCond(conditional.truepart);
                // 回填成功
                LzyCode.Chain falseJumps = first.jumpFalse();
                this.code.resolve(first.trueJumps);

                LzyCode.Chain trueJumps = this.code.branch(goto_);
                this.code.resolve( secondJumps );
                // 第二个条件可寻址实体
                LzyItems.CondItem second  = this.genCond(conditional.falsepart);
                return this.items.makeCondItem( second.opcode  , LzyCode.mergeChains(trueJumps,second.trueJumps) ,  LzyCode.mergeChains( falseJumps , second.falseJumps ) );
            }
        }else {
            return this.genExpr(jcTree,this.syms.booleanType).mkCond();
        }
    }

    /**
     * 创建常量的可寻址实体
     * @param n
     */
    void  loadIntConst(int n){
        this.items.makeImmediateItem( this.syms.intType , new Integer(n) );
    }


    /**
     * 创建数组生成字节码的可寻址实体
     * @param type
     * @param ndims
     * @return
     */
    LzyItems.Item makeNewArray(  LzyType type ,  int ndims  ){
        // 元素类型
        LzyType elementType = type.elemtype();
        // 类型编码
        int elemcode = LzyCode.arraycode(elementType);
        // elemcode不是类也不是数组:
        if ( elemcode != 0 && (elemcode != 1 || ndims != 1) ){

            // 一维度数组
            if ( elemcode == 1 ){
                // 多维数组
                this.code.emitop( multianewarray , 1-ndims  );
                //
                this.code.emit2( this.makeRef( type   ) );
                //
                this.code.emit1( ndims );
            }else {
                // 创建一个基本类型的数组
                this.code.emitop1(newarray,elemcode);
            }
        }else {
            // 创建一个引用数据类型的数组
            this.code.emitop2( anewarray ,  this.makeRef(elementType)  );
        }
        // 创建栈可寻址实体
        return this.items.makeStackItem(type);
    }



    void genTry(LzyJCTree body , LzyList<LzyJCCatch> catches , LzyEnv<GenContext> env ){
        // 开始的局部变量表下标: 用于出作用域，清空作用域中声明的变量
        int limit = this.code.nextreg;
        // try中代码开始位置
        int startpc = code.curPc();
        // try状态: 删除掉
        // LzyCode.State stateTry = code.state.dup();
        // 生成try中代码
        genStat( body , env  );
        // try中代码结束位置
        int endpc = code.curPc();
        // 是否有finally代码块
        boolean hasFializer =  env.info.finalizer != null && env.info.finalizer.hasFinalizer();
        // 冗余位置记录表
        LzyList<Integer> gaps = env.info.gaps.toList();
        // 生成finally代码块: 记录开始位置
        genFinalizer(env);
        // 强制跳转的回填
        LzyCode.Chain exitChain = code.branch(goto_);
        // 记录结束位置
        endFinalizerGap(env);
        // 处理所有的catch
        if ( startpc != endpc ) for ( LzyList<LzyJCCatch> l = catches ; l.nonEmpty()  ; l = l.tail ){
            // 设置入境点: 1. alive可达 2. 栈大小为1
            code.entryPoint(1);
            // 生成catch中字节码
            genCatch( l.head , env  ,  startpc , endpc, gaps );
            // 每个catch字节码后面: 添加finally代码块. 记录冗余字节码的开始位置
            genFinalizer(env);
            // 创建新回填
            if ( hasFializer || l.tail.nonEmpty() ){
                exitChain = LzyCode.mergeChains(exitChain,code.branch(goto_));
            }
            // 记录冗余字节码的结束位置
            endFinalizerGap(env);
        }
        // 如果存在finnally代码块
        if ( hasFializer ){
            // 将局部变量表可用位置,移动到末尾.防止出错
            code.newRegSegment();
            // 从堆栈上的异常开始: 返回字节码位置
            int catchallpc = code.entryPoint( 1  );
            //
            int startseg = startpc;
            // 冗余字节码的所有位置: 位置记录到异常表中
            while ( env.info.gaps.nonEmpty() ){
                // 冗余代码的开始位置
                int endseg = env.info.gaps.next().intValue();
                // 注册到异常表中
                // 注意: 冗余代码的开始位置,是异常catch代码的结束位置
                registerCatch(startpc,endseg,catchallpc,0);
                // 冗余代码的结束位置
                startseg = env.info.gaps.next().intValue();
            }
            // 创建一个异常类型
            LzyItems.LocalItem exeVar = makeTemp(syms.throwableType);
            // 异常类型存储到局部变量表
            exeVar.store();
            // 生成finnally代码块
            genFinalizer(env);
            // 异常类型加载到操作数栈上
            exeVar.load();
            // 注册到异常表中
            registerCatch( startseg , env.info.gaps.next().intValue() , catchallpc , 0 );
            // 强制抛出异常
            code.emitop(athrow);
            // 设置不可达
            code.markDead();
        }
        // 回填跳出try
        code.resolve(exitChain);
        // 清空作用域
        code.endScopes(limit);
    }

    /**
     * 向异常表中注册一条数据
     * @param startpc
     * @param endpc
     * @param handler_pc
     * @param catch_type
     */
    void registerCatch(int startpc , int endpc , int handler_pc , int catch_type){
        // 范围检查: 不能超过char的最大值
        char startpc1 = (char)startpc;
        char endpc1 = (char)endpc;
        char handler_pc1 = (char)handler_pc;
        if ( startpc == startpc1 && endpc == endpc1 && handler_pc == handler_pc1  ){
            // 添加catch
            code.addCatch( startpc1 , endpc1 , handler_pc1 , (char) catch_type );
        } else {
            // 范围不合法: 即catch中代码不能过多
            System.out.println("编译错误: catch中代码过长!");
            // 错误个数+1
            ++nerrs;
        }
    }


    void genCatch( LzyJCCatch tree , LzyEnv<GenContext> env , int startpc , int endpc , LzyList<Integer> gaps  ){
        // catch中有内容
        if ( startpc != endpc ){

        }
    }


    /**
     * 标记终结器的“全部捕捉”范围中的间隙结束。
     * @param env
     */
    void endFinalizerGap( LzyEnv<GenContext> env ){
        if (env.info.gaps != null && env.info.gaps.length() % 2 == 1)
            env.info.gaps.append(code.curPc());
    }

    /**
     * 为环境终结器标记所有捕获范围中的所有间隙结束
     * 位于两个环境之间，包括两个环境。
     * @param from
     * @param to
     */
    void endFinalizerGaps( LzyEnv<GenContext> from  , LzyEnv<GenContext> to ){
        LzyEnv<GenContext> last = null;
        while (last != to) {
            endFinalizerGap(from);
            last = from;
            from = from.next;
        }
    }

    /**
     * 对终结器的任何调用都会附加到环境“cont”链中
     * @param env
     */
    void genFinalizer(LzyEnv<GenContext> env){
        if ( this.code.isAive() && env.info.finalizer != null  ){
            env.info.finalizer.gen();
        }
    }

    /**
     * 调用由中止的结构的所有终结器通过非本地的
     * 返回非本地出口的目标环境
     * @param target
     * @param env
     * @return
     */
    LzyEnv<GenContext> unwind(LzyJCTree target,LzyEnv<GenContext> env){
        LzyEnv<GenContext> env1 = env;
        while (true) {
            genFinalizer(env1);
            if (env1.tree == target) break;
            env1 = env1.next;
        }
        return env1;
    }

    /**
     * 创建一个临时变量
     * @param type
     * @return
     */
    LzyItems.LocalItem makeTemp(LzyType type){
        // 创建一个合成变量
        return (LzyItems.LocalItem)this.items.makeLocalItem(type, this.code.newLocal(type));
    }



    public LzyItems.Item genExpr(LzyJCTree jcTree,LzyType type){
         // 上一个pt
         LzyType preType = this.pt;
         // 可寻址实体
         LzyItems.Item item;
         try {
             // 如果拥有常量值
             if ( jcTree.type.constValue != null ){
                this.checkStringConstant( jcTree.pos , jcTree.type.constValue );
                // 常量可寻址实体
                this.result = this.items.makeImmediateItem( jcTree.type, jcTree.type.constValue );
             } else {
                // 设置期望的类型
                this.pt  = type;
                // 访问者分发
                 jcTree.accept( this );
             }
             return this.result.coerce(type);
         }catch (LzySymbol.CompletionFailure completionFailure){
            if (  completionFailure instanceof LzyClassReader.BadClassFile){
                throw new Error();
            }
            item = this.items.makeStackItem(type);
         }finally {
             this.pt = preType;
         }
         return item;
    }

    public void  genDef( LzyJCTree jcTree , LzyEnv env ){
         LzyEnv preEnv = this.env;
         try {
             // 当前访问者需要的环境
             this.env = env;
             jcTree.accept(this);
         }catch (LzySymbol.CompletionFailure completionFailure){
            if ( completionFailure instanceof LzyClassReader.BadClassFile){
                throw new Error();
            }
         } finally {
             // 还原环境
             this.env = preEnv;
         }
    }


    void genMethod(LzyJCMethodDef methodDef , LzyEnv env , boolean fatcode  ){
        // 方法符号
        LzyMethodSymbol methodSymbol  = methodDef.sym;
        if ( methodDef.block != null  ){
            // 方法的Code属性
            methodSymbol.code = this.code = new LzyCode(fatcode ,syms, methodSymbol );
            // 可寻址实体
            this.items = new LzyItems( this.pool , this.code , this.syms  );
            // 非静态
            if ( (methodDef.flags & STATIC) == 0 ){
                // 创建局部变量: 更新局部变量表大小 ---> final this
                this.code.newLocal(new LzyVarSymbol( FINAL , this.names._this , methodSymbol.owner.type , methodSymbol.owner ));
            }
            // 方法的形式参数
            for ( LzyList l = methodDef.params  ; l.nonEmpty() ; l = l.tail  ){
                // 创建局部变量: 更新局部变量表大小
                this.code.newLocal( ((LzyJCVarDef)l.head).sym );
            }
            // 关闭代码压缩
            this.code.curPc();
            // 方法体
            this.genStat( methodDef.block , env );
            // 可达性判断
            if ( this.code.isAive()  ){
                // 方法返回值不是void
                if ( env.enclMethod != null && env.enclMethod.sym.type.restype().tag != LzyTypeTags.VOID   ){
                    // 生成一个自旋: 卡着不退出方法!
                    // 自己跳自己
                    int startpc = this.code.entryPoint();
                    LzyItems.CondItem condItem = this.items.makeCondItem(goto_);
                    // 回填
                    this.code.resolve( condItem.jumpTrue() , startpc );
                }else {
                    // 没有返回值的方法添加return结束方法的调用
                    this.code.emitop(return_);
                }
            }


            this.code.endScopes(0);

            // 最开始false,不需要扩容.后来变成true,那么我们需要重新生成Code
            if ( !fatcode && this.code.fatcode ){
                this.genMethod(methodDef,env,true);
            }


        }

    }

    public boolean genClass(LzyEnv env, LzyJCClassDef classDef){
        boolean success;
        try {
            this.attrEnv = env;
            // 类符号
            LzyClassSymbol classSym = classDef.sym;
            // 编译单元
            this.compilationUnit = env.toplevel;
            // 省略生成代理的代码

            // 对合成的进行处理
            classDef.defs = this.normalizeDefs( classDef.defs,classSym );
            // 常量池
            classSym.pool = this.pool;
            // 常量池重置到1的位置
            this.pool.reset();
            // 环境创建
            LzyEnv genEnv = new LzyEnv(classDef, new GenContext());
            // 环境的编译单元
            genEnv.toplevel = env.toplevel;
            // 附加类
            genEnv.enclClass = classDef;

            //
            LzyList<LzyJCTree> defsList = classDef.defs;
            while ( defsList.nonEmpty() ){
                // 访问者设计模式分发
                this.genDef(defsList.head,genEnv);
                // 下一个
                defsList  = defsList.tail;
            }


            if ( this.nerrs != 0 ){
                defsList = classDef.defs;
                while ( defsList.nonEmpty() ){
                    // 方法节点
                    if ( ((LzyJCTree)defsList.head).tag == LzyJCTree.METHODDEF ){
                        // 将方法的Code属性置空
                        ((LzyJCMethodDef)defsList.head).sym.code = null;
                    }
                    // 下一个
                    defsList = defsList.tail;
                }
            }
            success = (this.nerrs == 0);
        }finally {
            this.attrEnv = null;
            this.env = null;
            this.compilationUnit = null;
            this.nerrs = 0;
        }
        return success;
    }

    void normalizeMethod(LzyJCMethodDef methodDef,LzyList<LzyJCTree.JCStatement> initCode){
        if ( methodDef.name == this.names.init && LzyTreeInfo.isInitialConstructor(methodDef)){
            LzyList<LzyJCTree.JCStatement> stats = methodDef.block.stats;
            LzyListBuffer listBuffer = new LzyListBuffer();
            // 方法中存在内容
            if ( stats.nonEmpty() ){
                // 找到合成的那么停止寻找
                while ( LzyTreeInfo.isSyntheticInit( (LzyJCTree)stats.head  ) ){
                    listBuffer.append( stats.head );
                    // 移动到下一个
                    stats = stats.tail;
                }
                listBuffer.append(stats.head);
                //
                stats = stats.tail;
                while ( stats.nonEmpty() && LzyTreeInfo.isSyntheticInit((LzyJCTree)stats.head)  ){
                    listBuffer.append( stats.head );
                }
                listBuffer.appendList(initCode);
                while ( stats.nonEmpty() ){
                    listBuffer.append( stats.head );
                    stats = stats.tail;
                }
            }
            methodDef.block.stats = listBuffer.toList();
        }
    }





    /**
     * 是否有任何被非本地出口中止的结构,需要空堆栈的终结器
     * @param target
     * @param env
     * @return
     */
    boolean hasFinally(LzyJCTree target , LzyEnv<GenContext> env){
        while ( env.tree != target ){
            if ( env.tree.getTag() == LzyJCTree.TRY && env.info.finalizer.hasFinalizer() ){
                return true;
            }
            env = env.next;
        }
        return false;
    }

    /**
     * 检查常量值String的字符串长度，不能超过65535
     * @param pos 节点的位置pos
     * @param data 常量数据值
     */
    private void checkStringConstant(int pos,Object data){
         if (  this.nerrs == 0 && data != null && data instanceof String && ((String)data).length() >= 65535 ){
            ++this.nerrs;
         }
    }

    /**
     * 类型存储到常量池中，并返回常量池中的下标
     * @param type
     * @return
     */
    int makeRef(LzyType type){
        if ( type.tag == CLASS  ){
            return pool.put(type.tsym);
        }else{
            return pool.put(type);
        }
    }




    void callMethod(int pos, LzyType site, LzyName name,LzyList<LzyType> argTypes,boolean isStatic ){
        LzyMethodSymbol methodSymbol = this.rs.resolveInternalMethod(pos, this.attrEnv, site, name, argTypes);
        // 静态调用
        if ( isStatic ){
            this.items.makeStaticItem(methodSymbol).invoke();
        }else {
            // 实例调用
            this.items.makeMemberItem( methodSymbol , name == this.names.init ).invoke();
        }
    }

    /**
     * 将StringBuffer转变String
     * @param pos
     */
    void bufferToString(int pos){
        this.callMethod( pos , this.syms.stringBufferType , this.names.toString , LzyType.emptyList , false );
    }

    /**
     * String字符串实例创建，相关的字节码指令!
     * @param pos
     */
    void makeStringBuffer(int pos){
        // 创建一个new字节码指令
        this.code.emitop2( new_ , this.makeRef( this.syms.stringBufferType )  );
        // 赋值栈顶的数据
        this.code.emitop(dup);
        // 方法回调: 非静态方法
        callMethod( pos , this.syms.stringBufferType , this.names.init , LzyType.emptyList , false );
    }



    /**
     * 拼接所有的字符串 在buffer中
     * @param jcTree
     */
    void appendStrings(LzyJCTree jcTree){
        // 去掉括号: 获取括号中表达式
        jcTree = LzyTreeInfo.skipParens(jcTree);
        // '+'号: 连接两个String
        if ( jcTree.tag == LzyJCTree.PLUS && jcTree.type.constValue == null ){
            // 运算符节点
            LzyJCBinary binary = (LzyJCBinary)jcTree;
            // 1. 操作的类型是方法  2. string_add拼接`字符串
            if ( binary.operator.kind == MTH && ((LzyMethodSymbol.OperatorSymbol)binary.operator ).opcode == string_add  ){
                // 递归拼接: 左边表达式
                appendStrings(binary.lhs);
                // 递归拼接: 右边表达式
                appendStrings(binary.rhs);
                return ;
            }
        }
        //
        this.genExpr( jcTree , jcTree.type ).load();
        this.appendString(jcTree);
    }


    /**
     * 字符串拼接
     * @param jcTree
     */
    void appendString(LzyJCTree jcTree){
        LzyType type = jcTree.type;
        // 大于8即表明非基本数据类型。
        if ( type.tag > BOOLEAN && type.tsym != this.syms.stringType.tsym  ){
            type = this.syms.objectType;
        }
        // 调用StringBufferType
        this.callMethod( jcTree.pos , this.syms.stringBufferType , this.names.append , LzyType.emptyList.append(type) ,false );
    }





    /**
     * 将成员属性的初始化代码，添加到构造方法和静态代码块
     * @return
     */
    LzyList normalizeDefs(LzyList<LzyJCTree> defs , LzyClassSymbol classSymbol){
        // 实例初始化代码
        LzyListBuffer<LzyJCTree.JCStatement> initCode = new LzyListBuffer();
        // 静态初始化代码
        LzyListBuffer<LzyJCTree.JCStatement> clinitCode = new LzyListBuffer();
        // 所有方法
        LzyListBuffer<LzyJCTree> methodDefs = new LzyListBuffer();
        // 防止游标被移动到末尾
        for (LzyJCTree def : defs) {
            switch ( def.getTag() ){
                case LzyJCTree.BLOCK:
                    LzyJCBlock lzyJCBlock = (LzyJCBlock)def;
                    // 如果static修饰的代码块
                    if ( (lzyJCBlock.flags & STATIC) != 0 ){
                        clinitCode.append(lzyJCBlock);
                    }else {
                        initCode.append(lzyJCBlock);
                    }
                    break;
                case LzyJCTree.METHODDEF:
                    methodDefs.append(def);
                    break;
                case LzyJCTree.VARDEF:
                    // 变量节点
                    LzyJCVarDef jcVarDef = (LzyJCVarDef)def;
                    // 变量符号
                    LzyVarSymbol varSymbol = jcVarDef.sym;
                    // 如果变量有初始化
                    if ( jcVarDef.init != null ){
                        LzyJCTree.JCStatement init;
                        // 实例变量
                        if ( ( varSymbol.flags() & STATIC ) == 0 ){
                            init = this.make.at(jcVarDef.pos).Assignment(jcVarDef.sym, jcVarDef.init);
                            initCode.append(init);
                        }else if ( varSymbol.constValue == null ){
                            // 静态变量
                            init = this.make.at( jcVarDef.pos ).Assignment(jcVarDef.sym,jcVarDef.init);
                            clinitCode.append(init);
                        }
                    }
                    break;
                default:
                    LzyAssert.error();
            }
        }

        // 将<init>中代码插入到所有的构造器中
        if ( initCode.length() != 0 ){
            LzyList<LzyJCTree.JCStatement> inits = initCode.toList();
            for (LzyJCTree methodDef : methodDefs) {
                this.normalizeMethod( (LzyJCMethodDef) methodDef,inits );
            }
        }
        // <clint>静态代码块
        if ( clinitCode.length() != 0 ){
            // 创建一个方法符号
            LzyMethodSymbol methodSymbol = new LzyMethodSymbol(STATIC, this.names.clinit, new LzyMethodType(LzyType.emptyList, this.syms.voidType, LzyType.emptyList, this.syms.methodClass), classSymbol);
            // 方法符号填充到类符号中
            classSymbol.members().enter(methodSymbol);
            // 静态代码块
            LzyList<LzyJCTree.JCStatement> clinitList = clinitCode.toList();
            LzyJCBlock block = this.make.at( clinitList.head.pos ).Block(0,clinitList);
            //
            methodDefs.append( this.make.MethodDef( methodSymbol , block ) );
        }
        // 所有的方法
        return methodDefs.toList();
    }



    class GenContext{
        // 用于退出当前环境的所有未解析跳转的链: 处理所有break的回填
        LzyCode.Chain exit = null;
        // 在当前环境中继续的所有未解析跳转的链: 处理所有continue的回填
        LzyCode.Chain cont = null;
        /**
         * 生成当前环境终结器的闭包。
         * 仅为同步和重试上下文设置。
         */
        LzyGen.GenFinalizer finalizer = null;
        /**
         * 这是switch语句吗?如果是，分配寄存器
         * 即使变量声明不可访问。
         */
        boolean isSwitch = false;
        /**
         * 包含终结器范围内所有间隙的列表缓冲区，
         * 不应应用“捕获所有”异常的情况。
         */
        LzyListBuffer<Integer> gaps = null;

        void  addCont(LzyCode.Chain chain){
            this.cont = LzyCode.mergeChains(chain,this.cont);
        }

        void addExit(LzyCode.Chain chain){
            this.exit = LzyCode.mergeChains(chain,this.exit);
        }

    }

    abstract class GenFinalizer{
        // 生成代码以在展开时进行清理
        abstract void gen();
        // 最后生成代码进行清理
        abstract void genLast();
        // 此终结器是否要执行一些非平凡的清理？
        boolean hasFinalizer(){
            return true;
        }
        GenFinalizer(){

        }
    }

}

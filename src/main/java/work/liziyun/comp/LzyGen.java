package work.liziyun.comp;

import work.liziyun.code.LzyCode;
import work.liziyun.code.symbol.LzyMethodSymbol;
import work.liziyun.code.symbol.LzySymbol;
import work.liziyun.code.symbol.LzyVarSymbol;
import work.liziyun.code.type.LzyMethodType;
import work.liziyun.code.type.LzyType;
import work.liziyun.code.type.LzyTypeTags;
import work.liziyun.tag.LzyByteCodes;
import work.liziyun.tag.LzyFlags;
import work.liziyun.tag.LzyKinds;
import work.liziyun.tree.LzyJCIndexed;
import work.liziyun.tree.LzyJCMethodDef;
import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.LzyTreeInfo;
import work.liziyun.tree.express.*;
import work.liziyun.tree.state.*;
import work.liziyun.util.LzyAssert;
import work.liziyun.util.LzyContext;
import work.liziyun.util.LzyList;
import work.liziyun.util.LzyListBuffer;

import static work.liziyun.tree.LzyJCTree.*;

public class LzyGen extends LzyAbsGen {

    public static final LzyContext.Key key = new LzyContext.Key();

    private LzyGen(LzyContext LzyContext) {
        super(LzyContext);
        LzyContext.put(key,this);
    }

    public static LzyGen instance(LzyContext LzyContext){
        LzyGen gen = (LzyGen)LzyContext.get(key);
        if ( gen == null ){
            gen = new LzyGen(LzyContext);
        }
        return gen;
    }


    @Override
    public void visitJCApply(LzyJCApply apply){
        // 生成方法调用的可寻址实体
        LzyItems.Item methodItem = this.genExpr(apply.meth, this.methodType);
        this.genArgs( apply.args , LzyTreeInfo.symbol( apply.meth ).type.argtypes() );
        // 生成invoke指令
        this.result = methodItem.invoke();
    }


    @Override
    public void visitJCReturn(LzyJCReturn jcReturn){
        // 下一个可用的寄存器： 记录进入作用域前的初始值
        int limit = this.code.nextreg;
        // 环境
        final  LzyEnv<GenContext> targetEnv;
        if ( jcReturn.expr != null ){
            LzyItems.Item item = genExpr(jcReturn.expr, pt).load();
            if ( hasFinally(env.enclMethod, env) ) {
                // 创建局部变量可寻址实体
                item = makeTemp(pt);
                // 加载到操作数栈中
                item.store();
            }
            targetEnv = unwind(env.enclMethod,env);
            // 加载到操作数栈上
            item.load();
            // 返回值指令 + 偏移量
            code.emitop(ireturn + LzyCode.truncate(LzyCode.typecode(pt)) );
        }else {
            targetEnv = unwind(env.enclMethod,env);
            // 没有返回值，直接返回
            code.emitop(return_);
        }
        this.endFinalizerGaps(env,targetEnv);
        // 清空作用域
        this.code.endScopes(limit);
    }

    @Override
    public void visitJCSelect(LzyJCSelect jcSelect){
        LzySymbol sym = jcSelect.sym;
        // 严格禁止: .class语法,因为A.class将语法糖处理成A.forName("A");
        if (jcSelect.name == this.names._class){
            throw new AssertionError();
        }else {
            LzyItems.Item base = this.genExpr(jcSelect.selected,jcSelect.selected.type);
            // 静态内容
            if ( (sym.flags()&STATIC) != 0 ){
                // 注意: base创建的静态可寻址实体是无效的.
                // 重新创建有效的可寻址实体
                this.result = this.items.makeStaticItem(sym);
            }else {
                // 上一级的可寻址实体
                base.load();
                if (sym == this.syms.lengthVar){
                    // 生成字节码指令: 数组长度
                    this.code.emitop(arraylength);
                    // 返回栈可寻址实体
                    this.result = this.items.makeStackItem(this.syms.intType);
                }else {
                    // 返回结果: 当前级的可寻址实体,并没有触发加载到操作数栈上
                    LzySymbol preSym = LzyTreeInfo.symbol(jcSelect.selected);
                    this.result = this.items.makeMemberItem(sym,(sym.flags()&LzyFlags.PRIVATE)!=0 || ( preSym != null && (preSym.kind == TYP || preSym.name == this.names._super ) )  );
                }
            }

        }
    }

    @Override
    public void visitJCSkip(LzyJCSkip jcSkip){

    }


    @Override
    public void visitJCSwitch(LzyJCSwitch lzyJCSwitch){
        int limit = this.code.nextreg;
        int startpc = 0;
        // 生成条件的可寻址实体
        LzyItems.Item sel = genExpr(lzyJCSwitch.selector, this.syms.intType);
        // 所有的cases
        LzyList<LzyJCCase> cases = lzyJCSwitch.cases;
        // 如果没有cases
        if ( cases.isEmpty() ){
            System.out.println("编译错误: 不存在case");
        }else {
            // 加载到操作数栈中
            sel.load();
            //
        }
    }






    @Override
    public void visitJCForLoop(LzyJCForLoop jcForLoop){
        // 当前局部变量表下标
        int nextreg = this.code.nextreg;
        this.genStats(jcForLoop.init,this.env);
        this.genLoop( jcForLoop , jcForLoop.body , jcForLoop.cond , jcForLoop.step , true);
        // 还原局部变量表下标: 相当于清空局部作用域,重复利用局部变量表空间.
        this.code.endScopes(nextreg);
    }


    /**
     *  Java语言层面if 和 字节码指令层面if
     *          if( age > 18 ){
     *              System.out.println("成年");
     *          }else{
     *              System.out.println("未成年");
     *          }
     *  Java语言层面含义: 满足if跳转 --> 跳到需要执行代码
     *      age大于18进入代码块
     *  字节码层面含义: 满足if跳转 --> 跳到不需要执行的代码
     *      1. 先对指令取反 age < 18
     *      2. 由于指令是顺序紧紧挨着的
     *  为什么字节码层面的含义不设计成Java语言层面一样的含义?
     *      因为一个跳转相关字节码指令只能跳转到一个地方.
     *      无法表达出Java这种含义: 指令判断成功跳转成年,指令判断不成功跳转未成年.
     *
     *      字节码层面思想的精巧:
     *          1. age < 18 跳转 4
     *          2. System.out.println("成年");
     *          3. 跳转 5
     *          4. System.out.println("未成年");
     *          5. return
     *      注意: age < 18这里只能指定判断成功的跳转,无法指定判断失败的跳转.如果失败,将继续执行!
     *
     * @param lzyJCIf
     */
    @Override
    public void visitJCIf(LzyJCIf lzyJCIf){
        // 下一个可用的局部变量的位置: 进入if前的局部变量表的大小
        int limit = this.code.nextreg;
        // 出if的跳转
        LzyCode.Chain thenExit = null;
        // 条件可寻址实体
        LzyItems.CondItem condItem = genCond(LzyTreeInfo.skipParens(lzyJCIf.cond));
        // 添加跳转链： false
        // 添加跳转链： if
        // 生成跳转： 等待回填
        LzyCode.Chain elseChain = condItem.jumpFalse();
        // 确保有意义: 防止静态false
        if ( ! condItem.isFalse() ){
            // 填充可寻址
            this.code.resolve(condItem.trueJumps);
            // 生成内容: 这里存在递归
            // 案例: if(){ }else if(){  }else{ }
            // thenpart中内容: 是一个LzyJCIf语法书节点
            genStat(lzyJCIf.thenpart,env);
            // 生成跳转： 等待回填
            // 注意: 这里是goto_,那么将链接到pendingResolve链表上
            thenExit = code.branch(goto_);
        }

        if (elseChain != null){
            // 添加跳转链： elesChain
            code.resolve(elseChain);
            if ( lzyJCIf.elsepart != null ){
                // 开始回填
                genStat( lzyJCIf.elsepart , env );
            }
        }
        // 回填出if的跳转: 因为else部分，已经生成
        code.resolve(thenExit);
        // 情况if作用域
        code.endScopes(limit);
    }


    /**
     * 普通赋值语句
     * @param assign
     */
    @Override
    public void visitJCAssign(LzyJCAssign assign){
        // 赋值语句的可寻址实体  参数一: 赋值语句左边的节点        参数二: 赋值语句右边的类型
        LzyItems.Item assignLeftItem = this.genExpr(assign.lhs, assign.rhs.type);
        // 表达式
        this.genExpr( assign.rhs , assign.lhs.type ).load();
        // 结果
        this.result = this.items.makeAssignItem(assignLeftItem);
    }

    /**
     * 复合赋值语句
     *
     * 注意:
     *     语法树节点中的是真实类型,运算符号中的是预定义类型.
     *     1. 语法树中类型是真实类型
     *     2. 匹配到的方法符号
     *     例如: 基本数据类型
     *          char a = 1;
     *          a += 1;
     *          语法树节点:
     *              leftType:char
     *              rightType:char
     *          运算符号:
     *              int +(int,int)
     * @param assignop
     */
    @Override
    public void visitJCAssignop(LzyJCAssignop assignop){
        // 运算符
        LzyMethodSymbol.OperatorSymbol operatorSymbol  = (LzyMethodSymbol.OperatorSymbol)assignop.operator;
        // 可寻址实体
        LzyItems.Item item ;
        // 字符串的拼接
        if ( operatorSymbol.opcode == LzyByteCodes.string_add){
            // 注意: 我们使用的String类型是StringBuilder
            // 当jdk大于1.5时，编译器应该指定为StringBuffer
            // StringBuffer是线程安全的,StringBuilder是线程不安全的
            // 解析: 生成第一个字符串的代码，在缓存区下复制
            this.makeStringBuffer(assignop.pos);
            // 解析赋值语句的左边的表达式
            item = this.genExpr( assignop.lhs , assignop.lhs.type );
            // 无符号字节码指令
            if ( item.width() > 0 ){
                this.code.emitop( dup_x1 + 3 * (item.width()-1)  );
            }
            // 加载第一个字符串 并 拼接缓存
            item.load();
            // 拼接左边
            this.appendString(assignop.lhs);
            // 拼接右边
            this.appendStrings(assignop.rhs);
            // 将StringBuffer转成String
            this.bufferToString(assignop.pos);
        } else {
            // 生成字节码的第一个表达式
            item = genExpr( assignop.lhs , assignop.lhs.type );
            // 复合赋值运算: += 和 -= ， 我们可以考虑用incc指令代替
            // 注意: 操作的是局部变量的可寻址实体
            if (
                    (assignop.getTag() == LzyJCTree.PLUS_ASG || assignop.getTag() == LzyJCTree.MINUS_ASG )
                    &&
                       item instanceof  LzyItems.LocalItem
                    &&
                       assignop.lhs.type.tag <= INT
                    &&
                       assignop.rhs.type.tag <= INT
                    &&
                       assignop.rhs.type.constValue != null
            ){
                    int num = ((Number)assignop.rhs.type.constValue).intValue();
                    // 正数变负数
                    if ( assignop.tag == LzyJCTree.MINUS_ASG  ){
                        num = -num;
                    }

                    if ( -128 <= num && num <= 127  ){
                        ((LzyItems.LocalItem)item).incr(num);
                        this.result = item;
                        return;
                    }
            }
            // 复制栈顶数据: 存储一份到局部变量表
            item.duplicate();
            // 强制类型转换指令的生成: 匹配类型
            item.coerce((LzyType) operatorSymbol.type.argtypes().head).load();
            // 强制类型转换: 真实类型
            completeBinop( assignop.lhs , assignop.rhs , operatorSymbol ).coerce(assignop.lhs.type);
        }
        // 强制封装成赋值语句
        this.result = this.items.makeAssignItem(item);
    }


    @Override
    public void visitJCBlock(LzyJCBlock jcBlock){
        int limit = this.code.nextreg;
        // 生成局部环境
        LzyEnv<GenContext> localEnv = this.env.dup(jcBlock,new LzyAbsGen.GenContext());
        // 生成代码块
        this.genStats( jcBlock.stats , localEnv);
        // 如果不是方法块,那么清空作用域
        if ( this.env.tree.tag != METHODDEF ){
            this.code.endScopes(limit);
        }
    }


    @Override
    public void visitJCBreak(LzyJCBreak jcBreak){
        LzyEnv env = this.unwind(jcBreak.target, this.env);
        ((LzyAbsGen.GenContext)env.info).addExit( this.code.branch(goto_) );
        this.endFinalizerGaps(this.env,env);
    }

    @Override
    public void visitJCConditional(LzyJCConditional tree){
        LzyCode.Chain thenExit = null;
        LzyItems.CondItem c = genCond(tree.cond);
        // else的
        LzyCode.Chain elseChain = c.jumpFalse();
        int startpc;
        if ( !c.isFalse() ){
            this.code.resolve(c.trueJumps);
            startpc = 0;
            this.genExpr( tree.truepart , this.pt ).load();
            thenExit = this.code.branch(goto_);
        }
        if ( elseChain != null ){
            this.code.resolve(elseChain);
            startpc = 0;
            this.genExpr( tree.falsepart , this.pt ).load();
        }
        // 回填
        this.code.resolve(thenExit);
        // 创建基本数据类型可寻址实体
        this.result = this.items.makeStackItem(this.pt);
    }

    @Override
    public void visitJCContinue(LzyJCContinue jcContinue){
        LzyEnv<GenContext> targetEnv = this.unwind(jcContinue.target, this.env);
        targetEnv.info.addExit(code.branch(goto_));
        endFinalizerGaps(env,targetEnv);
    }

    @Override
    public void visitJCDoloop(LzyJCDoloop jcDoloop){
        this.genLoop(jcDoloop,(LzyJCTree.JCStatement)jcDoloop.body,(LzyJCTree.JCExpression)jcDoloop.cond, LzyList.nil(),false);
    }


    /**
     * 我们将后缀变前缀
     *  例如: i++ 变 ++i , i-- 变 --i
     * @param statement
     */
    @Override
    public void visitJCExec(LzyJCExpressionStatement statement){
        if ( statement.expr.tag ==  POSTINC){
            statement.expr.tag = PREINC;
        }else if ( statement.expr.tag == POSTDEC ){
            statement.expr.tag = PREDEC;
        }
        this.genExpr(statement.expr, statement.expr.type).drop();
    }



    @Override
    public void visitJCIdent(LzyJCIdent ident){
        LzySymbol symbol = ident.symbol;
        if ( ident.name != this.names._this && ident.name != this.names._super ){
            if ( ident.name == this.names._null ){
                // null指令
                this.code.emitop(aconst_null);
                // 如果是数组
                if ( this.pt.dimensions() > 1 ){
                    // 类型检查
                    this.code.emitop2( checkcast , this.makeRef(this.pt) );
                    this.result = this.items.makeStackItem(this.pt);
                }else {
                    this.result  = this.items.makeStackItem(ident.type);
                }
            } else if ( symbol.kind == LzyKinds.VAR && symbol.owner.kind == MTH ){// 局部变量
                this.result = this.items.makeLocalItem( (LzyVarSymbol) symbol );
            } else if ( (symbol.flags() & STATIC) != 0 ){ // 是static修饰
                this.result = this.items.makeStaticItem(symbol);
            }else if ( symbol.kind == LzyKinds.TYP ){
                // 因为静态可寻址实体: 不需要加载
                this.result = this.items.makeStaticItem(symbol);
            }else {// 成员变量
                // 创建this可寻址实体,加载到操作数栈中
                this.items.makeThisItem().load();
                this.result = this.items.makeMemberItem( symbol, (symbol.flags()& LzyFlags.PRIVATE) != 0 );
            }
        }else{
            // 创建this或super可寻址实体
            LzyItems.Item item =  ident.name == this.names._this ? this.items.makeThisItem():this.items.makeSuperItem();
            if (symbol.kind == MTH){
                item.load();
                item = this.items.makeMemberItem(symbol,true);
            }
            this.result = item;
        }
    }


    @Override
    public void visitJCIndexed(LzyJCIndexed indexed){
        // 数组加载到操作数栈
        this.genExpr(indexed.indexed, indexed.indexed.type).load();
        // 下标加载到操作数栈
        this.genExpr(indexed.index, this.syms.intType).load();
        // 下标可寻址实体
        this.result = this.items.makeIndexedItem(indexed.type);
    }

    @Override
    public void visitJCMethodDef(LzyJCMethodDef methodDef){
        LzyEnv env = this.env.dup(methodDef);
        env.enclMethod  = methodDef;
        // 期望类型
        this.pt = ((LzyMethodType)methodDef.sym.type).restype();
        // 生成方法
        this.genMethod( methodDef , env ,false  );
    }


    @Override
    public void visitJCNewArray(LzyJCNewArray newArray){
        if ( newArray.elems != null ){
            //
            LzyType elemtype = newArray.type.elemtype();
            // 数组长度: 创建常量可寻址
            this.loadIntConst(newArray.elems.length());
            // 创建可寻址实体
            LzyItems.Item arr = this.makeNewArray(newArray.type, 1);
            int i = 0;
            for ( LzyList<LzyJCTree.JCExpression> l = newArray.elems   ; l.nonEmpty() ;  l = l.tail  ){
                // 复制
                arr.duplicate();
                // 创建常量可寻址实体
                this.loadIntConst(i);
                ++i;
                //
                this.genExpr( l.head , elemtype ).load();
                // 创建下标可寻址实体
                this.items.makeIndexedItem( elemtype ).store();
            }
            this.result = arr;
        }else {
            for ( LzyList<LzyJCTree.JCExpression> l = newArray.dims ; l.nonEmpty()  ; l = l.tail  ){
                this.genExpr( l.head , this.syms.intType ).load();
            }
            this.result = this.makeNewArray(  newArray.type , newArray.dims.length() );
        }
    }

    @Override
    public void visitJCNewClass(LzyJCNewClass newClass){
        if ( newClass.encl == null && newClass.def == null){
            // 生成指令: new_
            this.code.emitop2( new_ , this.makeRef(newClass.type) );
            // 生成指令: dup
            this.code.emitop(dup);
            // 为所有参数生成代码
            this.genArgs( newClass.args , newClass.constructor.externalType().argtypes() );
            // 创建实体可寻址
            this.items.makeMemberItem(newClass.constructor , true).invoke();
            this.result = items.makeStackItem(newClass.type);
        }else {
            throw new AssertionError();
        }
    }

    @Override
    public void visitJCParens(LzyJCParens jcParens){
        this.result = this.genExpr( jcParens.expr , jcParens.expr.type );
    }




    /**
     * 常量
     * @param literal
     */
    @Override
    public void visitJCLiteral(LzyJCLiteral literal){
        this.result = this.items.makeImmediateItem(literal.type,literal.value);
    }



    @Override
    public void visitJCBinary(LzyJCBinary jcBinary){
        LzyMethodSymbol.OperatorSymbol operatorSymbol = (LzyMethodSymbol.OperatorSymbol)jcBinary.operator;
        if ( operatorSymbol.opcode == string_add ){
            // 创建一个StringBuffer
            makeStringBuffer(jcBinary.pos);
            // 添加一个String
            appendStrings(jcBinary);
            // StringBuffer转String
            bufferToString(jcBinary.pos);
            result = items.makeStackItem(this.syms.stringType);
        }else {
            LzyItems.CondItem leftCond;
            LzyCode.Chain chain;
            LzyItems.CondItem rightCond;
            // 逻辑与
            if ( jcBinary.opcode == LzyJCTree.AND ){
                leftCond = this.genCond( jcBinary.lhs );
                // 不是false,那么成立
                if ( !leftCond.isFalse() ){
                    chain = leftCond.jumpFalse();
                    // 确定回填
                    this.code.resolve(leftCond.trueJumps);
                    // 生成赋值语句右边
                    rightCond = this.genCond( jcBinary.rhs );
                    // 创建条件可寻址实体
                    this.result = this.items.makeCondItem( rightCond.opcode , rightCond.trueJumps , LzyCode.mergeChains(chain,rightCond.falseJumps)  );
                }else {// 编译时期已经可以确定短路
                    this.result = leftCond;
                }
            // 逻辑或
            }else if ( jcBinary.opcode == LzyJCTree.OR ){
                leftCond = this.genCond( jcBinary.lhs  );
                // 不是false,那么成立
                if (!leftCond.isTrue()){
                    chain = leftCond.jumpTrue();
                    // 开始回填
                    this.code.resolve(leftCond.falseJumps);
                    // 生成右边的字节码指令
                    rightCond = this.genCond( jcBinary.rhs );
                    this.result = this.items.makeCondItem( rightCond.opcode ,LzyCode.mergeChains(chain,rightCond.trueJumps) , rightCond.falseJumps   );
                }else{
                    this.result = leftCond;
                }
            }else{// 其他运算符
                // 运算符左边的数据可寻址操作：
                LzyItems.Item item = this.genExpr(jcBinary.lhs, operatorSymbol.type.argtypes().head);
                // 加载到操作数栈中
                item.load();
                // 关系运算
                this.result = this.completeBinop(jcBinary.lhs,jcBinary.rhs,operatorSymbol);
            }
        }
    }


    /**
     * 一元运算
     * @param unary
     */
    @Override
    public void visitJCUnary(LzyJCUnary unary){
         LzyMethodSymbol.OperatorSymbol operatorSymbol = (LzyMethodSymbol.OperatorSymbol)unary.operator;
         // 一元运算符: !
         if ( unary.tag == NOT ){
             LzyItems.CondItem condItem = this.genCond(unary.arg);
             // 条件可寻址实体取反
             this.result = condItem.negate();
         }else {
            // 指令可寻址实体
             LzyItems.Item od = genExpr( unary.arg , (LzyType) operatorSymbol.type.argtypes().head  );
             //
             switch (unary.tag){
                 // +
                 case POS:
                     this.result = od.load();
                     break;
                 // -
                 case NEG:
                     this.result = od.load();
                     this.code.emitop(operatorSymbol.opcode);
                     break;
                 // !
                 case NOT:
                 default:
                     break;
                 // ~
                 case COMPL:
                    this.result = od.load();
                    this.emitMinusOne(od.typecode);
                    this.code.emitop(operatorSymbol.opcode);
                    break;
                 // ++i
                 case PREINC:
                 // --i
                 case PREDEC:
                     od.duplicate();
                     // 局部变量可寻址
                     if ( od instanceof LzyItems.LocalItem && (operatorSymbol.opcode == iadd || operatorSymbol.opcode == isub  ) ){
                         // 创建incr指令： 自增长
                         ((LzyItems.LocalItem)od).incr( unary.tag == PREINC ? 1:-1 );
                         this.result = od;
                     }else {
                         // 成员变量
                         od.load();
                         this.code.emitop(one(od.typecode));
                         this.code.emitop(operatorSymbol.opcode);
                         if ( od.typecode != 0 && LzyCode.truncate(od.typecode) == 0 ){
                             this.code.emitop( int2byte + od.typecode - BYTEcode  );
                         }
                         // 运算可寻址实体
                         this.result = this.items.makeAssignItem(od);
                     }
                     break;
                 // i++
                 case POSTINC:
                 // i--
                 case POSTDEC:
                     od.duplicate();
                     LzyItems.Item item;
                     if (od instanceof LzyItems.LocalItem && operatorSymbol.opcode == iadd ){
                         item = od.load();
                         ((LzyItems.LocalItem)od).incr( unary.tag == POSTINC ? 1:-1    );
                         this.result = item;
                     }else {
                         item = od.load();
                         // 复制栈顶，插入到当前实体下面
                         od.stash(od.typecode);
                         //
                         this.code.emitop(one(od.typecode));
                         this.code.emitop(operatorSymbol.opcode);
                         if ( od.typecode != 0 && LzyCode.truncate(od.typecode) == 0 ){
                             this.code.emitop( int2byte + od.typecode - BYTEcode  );
                         }
                         od.store();
                         this.result = item;
                     }
                     break;
                 case NULLCHK:
                     this.result = od.load();
                     this.code.emitop(dup);
                     this.genNullCheck(unary.pos);
             }
         }
    }

    @Override
    public void visitJCVarDef(LzyJCVarDef varDef){
        LzyVarSymbol varSymbol = varDef.sym;
        // 更新局部变量表大小,下标存储到变量VarSymbol的adr中
        this.code.newLocal(varSymbol);
        // 变量值的初始化
        if ( varDef.init != null ){
            if ( varSymbol.constValue == null  ){
                // 参数一: 变量初始化表达式   参数二: 变量类型
                // 存在多种情况:
                //      1. 成员字段调用表达式 ---> 成员可寻址实体 ---> 加载到操作数栈
                //              第一种情况： 是一个变量
                //                  第一阶段LzyGen.genExpr() ---> LzyGen.visitJCSelect() ---> LzyGen.visitJCIdent()
                //                      a.
                //                  第二阶段LzyMemberItem.load()
                //                      a.
                //              第二种情况： 是一个常量
                //                  第一阶段：LzyGen.genExpr()
                //                      a. 创建常量可寻址实体
                //                  第二阶段:LzyImmediateItem.load()
                //                      a. 将常量池中数据加载到操作数栈上
                //      2. 静态字段调用表达式 ---> 静态可寻址实体 ---> 加载到操作数栈
                //              第一阶段LzyGen.visitJCSelect() ---> LzyGen.visitJCIdent()
                //              第二阶段LzyStaticItem.load()
                //      3. 变量表达式 ---> 变量可寻址实体 ---> 加载到操作数栈
                //              第一阶段LzyGen.visitJCIdent():
                //                  a. 创建局部变量可寻址实体. 根据变量符号(存储局部变量表中下标)
                //              第二阶段LzyLocalItem.load()
                //                  a. 将局部变量表中数据加载到操作数栈中.
                //      4. 下标表达式 ---> 下标可寻址实体 ---> 加载到操作数栈
                //              第一阶段LzyGen.visitJCVarDef():
                //                  a. 数组加载到操作数栈
                //                  b. 下标加载到操作数栈
                //              第二阶段LzyIndexItem.load()
                //                  a. 生成xaload相关指令,将数组中数据加载到操作数栈中
                //      5. 成员方法与静态方法表达式 ---> 栈可寻址实体 ---> 空实现
                //              第一阶段LzyGen.visitJCApply()
                //                  a. 将方法调用所需的参数加载到操作数栈上
                //                  b. 生成静态可寻址实体LzyStaticItem和成员可寻址实体LzyMemberItem
                //                  c. 生成invoke相关方法调用指令
                //                  d. 返回一个栈可寻址实体LzyStaticItem
                //              第二阶段LzyStaticItem.load()
                //                  a. 空实现
                this.genExpr( varDef.init , varSymbol.type ).load();
                // 操作数栈中栈顶的数据,存储到局部变量表中指定位置
                this.items.makeLocalItem(varSymbol).store();
            }
        }
    }


    @Override
    public void visitJCTypeCast(LzyJCTypeCast typeCast){
        // checkcast指令
        this.result =  this.genExpr(typeCast.expr, typeCast.clazz.type).load();
        if (typeCast.clazz.type.tag > LzyTypeTags.BOOLEAN && typeCast.expr.type.asSuper(typeCast.clazz.type.tsym)==null  ){
            this.code.emitop2(checkcast,this.makeRef(typeCast.clazz.type));
        }
    }

    @Override
    public void visitJCTypeTest(LzyJCTypeTest typeTest){
        // instanceof指令
        this.genExpr( typeTest.expr , typeTest.expr.type ).load();
        this.code.emitop2(instanceof_ , this.makeRef(typeTest.clazz.type));
        this.result = this.items.makeStackItem( this.syms.booleanType );
    }

    @Override
    public void visitJCThrow(LzyJCThrow jcThrow){
        // 创建一个局部变量可寻址实体LocalItem： 加载到操作数栈中
        this.genExpr(jcThrow.expr, jcThrow.expr.type ).load();
        // 调用athrow指令抛出异常
        this.code.emitop(athrow);
    }

    @Override
    public void visitJCTry(LzyJCTry jcTry){
        // 创建出一个新环境
        final LzyEnv<GenContext> tryEnv = this.env.dup(jcTry, new GenContext());
        // 上一个环境
        final LzyEnv<GenContext> oldEnv = env;
        // 匿名实现类
        tryEnv.info.finalizer = new GenFinalizer() {
            // 记录开始位置 + 生成冗余的字节码指令
            @Override
            void gen() {
                // 确保gaps是空的
                LzyAssert.check(tryEnv.info.gaps.length()%2==0);
                // 记录冗余代码的开始位置
                tryEnv.info.gaps.append( code.curPc() );
                // 生成冗余指令
                genLast();
            }

            // 生成冗余的字节码指令
            @Override
            void genLast() {
                // 如果拥有finally代码块
                if ( jcTry.finalizer != null )
                    genStat( jcTry.finalizer , oldEnv  );
            }

            // 是否有finally代码块
            @Override
            boolean hasFinalizer() {
                return jcTry.finalizer != null;
            }
        };
        // 冗余代码的字节码行号记录表
        tryEnv.info.gaps = new LzyListBuffer();
        //
        genTry( jcTry.body , jcTry.catchers , tryEnv );
    }


}

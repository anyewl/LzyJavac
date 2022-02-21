package work.liziyun.comp;


import work.liziyun.code.LzyScope;
import work.liziyun.util.LzyList;

public class LzyAttrContext {
    // 局部变量符号的作用域
    LzyScope scope = null;

    int staticLevel = 0;
    // 是否this或super调用的环境
    boolean isSelfCall = false;
    // 当前函数应用程序的参数是否已装箱到varargs的数组中
    boolean rawArgs = false;
    // 上下文环境中全部的类型变量列表
    LzyList tvars;

    public LzyAttrContext() {
        this.tvars = LzyList.nil();
    }

    LzyAttrContext dup() {
        return this.dup(this.scope);
    }

    LzyAttrContext dup(LzyScope var1) {
        LzyAttrContext var2 = new LzyAttrContext();
        // 设置新的Scope作用域
        var2.scope = var1;
        // 其他属性全部拷贝
        var2.staticLevel = this.staticLevel;
        var2.isSelfCall = this.isSelfCall;

        var2.rawArgs = this.rawArgs;
        var2.tvars = this.tvars;
        return var2;
    }
}

package work.liziyun.comp;

import work.liziyun.tree.LzyJCCompilationUnit;
import work.liziyun.tree.LzyJCMethodDef;
import work.liziyun.tree.LzyJCTree;
import work.liziyun.tree.state.LzyJCClassDef;

public class LzyEnv<A> {

    // 下一个封闭环境
    public LzyEnv<A> next = null;
    // 包含当前类的环境
    public LzyEnv<A> outer = null;
    // 环境相关的树
    public LzyJCTree tree;
    // 封闭的编译单元
    public LzyJCCompilationUnit toplevel;
    // 下一个封闭类的定义
    public LzyJCClassDef enclClass;
    // 下一孤封闭的方法定义
    public LzyJCMethodDef enclMethod;
    // 用于进一步信息的通用字段
    public A info;

    public LzyEnv enclosing(int var1) {
        LzyEnv var2;
        for(var2 = this; var2 != null && var2.tree.getTag() != var1; var2 = var2.next) {
        }
        return var2;
    }

    public LzyEnv dupto(LzyEnv var1) {
        // 新环境的下一个封闭环境是自己
        var1.next = this;   // 新环境的下一个: 当前环境
        var1.outer = this.outer; // 新环境的外部: 当前环境的外部(很有可能是编译单元!)
        var1.toplevel = this.toplevel;
        var1.enclClass = this.enclClass;
        var1.enclMethod = this.enclMethod;
        return var1;
    }

    public LzyEnv dup(LzyJCTree var1) {
        return this.dup(var1, this.info);
    }

    public LzyEnv(LzyJCTree var1, A var2) {
        this.tree = var1;
        this.toplevel = null;
        this.enclClass = null;
        this.enclMethod = null;
        this.info = var2;
    }

    public LzyEnv dup(LzyJCTree var1, Object var2) {
        return this.dupto(new LzyEnv(var1, var2));
    }
}

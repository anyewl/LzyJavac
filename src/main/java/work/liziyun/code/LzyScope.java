package work.liziyun.code;

import work.liziyun.code.symbol.LzySymbol;
import work.liziyun.world.LzyName;

public class LzyScope {
    // 外部环境
    public LzyScope next;
    // 符号所属
    public LzySymbol owner;
    // 能够访问的其他成员
    public LzyScope.Entry[] table;
    //
    int hashMask;
    // 当前作用域下的成员
    public LzyScope.Entry elems;
    // 大小
    public int nelems;
    // 初始化大小
    private static final int INITIAL_SIZE=128;
    // 无效标记成员
    private static final LzyScope.Entry sentinel;
    // 空作用域
    private static final LzyScope emptyScope;

    static {
        sentinel = new LzyScope.Entry(null,null,null,null);
        emptyScope = new LzyScope(null);
    }

    public LzyScope(LzyScope outerScope,LzySymbol ownerSymbol,LzyScope.Entry[] table) {
        this.nelems = 0;
        this.next = outerScope;
        this.owner = ownerSymbol;
        this.table = table;
        this.hashMask = table.length-1;
        this.elems = null;
    }

    private void dble(){
        // 能够访问的所有成员: 内部 + 外部
        LzyScope.Entry[] ts = this.table;
        // 扩容2倍
        LzyScope.Entry[] newts = new LzyScope.Entry[ts.length*2];
        // 当前作用域
        LzyScope sc = this;
        do {
            sc.table = newts;
            sc.hashMask = newts.length - 1;
            // 外部作用域
            sc = sc.next;
        }while (sc != null);
        // 初始化新的数组
        for (int i = 0; i < newts.length; i++) {
            newts[i] = sentinel;
        }
        // 旧的数据 复制到 新的中
        for (int i = 0; i < ts.length; i++) {
            this.copy(ts[i]);
        }

    }

    private void copy(LzyScope.Entry oldEntry){
        if (oldEntry.sym != null){
            // 递归处理: hash冲突的链表
            this.copy(oldEntry.shadowed);
            // 确定下标
            int index = oldEntry.sym.name.getIndex() & this.hashMask;
            // 连接到冲突的链表上
            oldEntry.shadowed = this.table[index];
            // 放入第一个位置
            this.table[index] = oldEntry;
        }
    }


    // 共享式： table
    public LzyScope dup(){
        return new LzyScope(this,this.owner,this.table);
    }

    // 非共享式: table
    public LzyScope dupUnshared(){
        return new LzyScope(this,this.owner,this.table.clone());
    }

    public LzyScope(LzySymbol owner){
        this(null,owner,new LzyScope.Entry[128]);
        for (int i = 0; i < 128; i++) {
            this.table[i] = sentinel;
        }
    }


    // 情况当前作用域的成员
    public LzyScope leave(){
        while (this.elems != null){
            int hash = this.elems.sym.name.getIndex() & this.hashMask;
            // 删除: 直接指向hash冲突的下一个！
            // 思考: 这么做合理？会不会产生BUG？
            // 因为产生冲突的节点，前面也可能有节点。
            // 导致误删前一个节点。
            // 误删的讨论: 共享式的Table，将会产生混乱!
            this.table[hash] = this.elems.shadowed;
            // 下一个
            this.elems = this.elems.sibling;
        }
        // 返回外部环境
        return this.next;
    }

    public LzyScope.Entry lookup(LzyName name){
        LzyScope.Entry e = this.table[name.getIndex()&this.hashMask];
        // 在冲突链表上查找： 解决名称不同产生的hash冲突
        while ( e.scope != null && e.sym.name != name ){
            e = e.shadowed;
        }
        return e;
    }

    public void enterIfAbsent(LzySymbol symbol){
        // 复合名称的Entry
        LzyScope.Entry e = this.lookup(symbol.name);
        // 条件一: 复合当前作用域  条件二: 名称相同，但是类型不同
        // 例如: abc的变量，abc的方法
        while (e.scope == this && e.sym.kind != symbol.kind){
            // 解决名称相同产生的hash冲突
            e.next(); // 如果e找不到,那么返回最后的存储空的Entry
        }

        // 作用域不是当前。
        // 错误的查找结果: 1. 外部作用域  2.e是空的sentinel
        // 正确的查找结果: 1. 作用域一定是当前作用域
        if (e.scope != this){
            // 当前作用域下建立符号
            this.enter(symbol);
        }

    }

    public void enter(LzySymbol symbol){
        this.enter(symbol,this);
    }

    // 填充符号到作用域中
    public void enter(LzySymbol symbol,LzyScope scope){
        // 扩容
        if (this.nelems*3 >= this.hashMask*2){
            this.dble();
        }
        // hash
        int hash = symbol.name.getIndex() & this.hashMask;
        // 建立： 有hash冲突，那么会形成链表
        LzyScope.Entry e = new LzyScope.Entry(symbol,this.table[hash],this.elems,scope);
        // 放入
        this.table[hash] = e;
        this.elems = e;
        ++this.nelems;
    }

    public boolean includes(LzySymbol symbol) {
        for(LzyScope.Entry entry = this.lookup(symbol.name); entry.scope == this; entry = entry.next()) {
            if (entry.sym == symbol) {
                return true;
            }
        }
        return false;
    }



    public static class Entry{
        public LzySymbol sym;
        // 名称相同： 冲突时
        public LzyScope.Entry shadowed;
        // 下一个
        public LzyScope.Entry sibling;
        // 作用域
        public LzyScope scope;



        // 作用: 解析name相同产生的hash冲突
        public LzyScope.Entry next(){
            // 冲突链表上寻找: 直到找到名称相同的!
            LzyScope.Entry entry = this.shadowed;
            while ( entry.scope != null && entry.sym.name != this.sym.name){
                entry = entry.shadowed;
            }
            return entry;
        }

        public Entry(LzySymbol sym, Entry shadowed, Entry sibling, LzyScope scope) {
            this.sym = sym;
            this.shadowed = shadowed;
            this.sibling = sibling;
            this.scope = scope;
        }
    }



    // 两个特殊的内部类
    public static class ErrorScope extends LzyScope {
        public LzyScope dup() {
            return new LzyScope.ErrorScope(this, this.owner, this.table);
        }

        public LzyScope dupUnshared() {
            return new LzyScope.ErrorScope(this, this.owner, (LzyScope.Entry[])this.table.clone());
        }

        public ErrorScope(LzySymbol var1) {
            super(var1);
        }

        public LzyScope.Entry lookup(LzyName var1) {
            LzyScope.Entry var2 = super.lookup(var1);
            return var2.scope == null ? new LzyScope.Entry(this.owner, (LzyScope.Entry)null, (LzyScope.Entry)null, (LzyScope)null) : var2;
        }

        ErrorScope(LzyScope var1, LzySymbol var2, LzyScope.Entry[] var3) {
            super(var1, var2, var3);
        }
    }



}

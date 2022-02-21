package work.liziyun.code.symbol;

import work.liziyun.code.LzyScope;
import work.liziyun.code.type.LzyMethodType;
import work.liziyun.code.type.LzyType;
import work.liziyun.code.type.LzyTypeTags;
import work.liziyun.tag.LzyFlags;
import work.liziyun.tag.LzyKinds;
import work.liziyun.world.LzyName;

public class LzySymbol implements LzyFlags, LzyKinds, LzyTypeTags {
    // 期望的类型
    public int kind;
    // 标记
    public long flags_field;
    // 名称
    public LzyName name;
    // 所属于
    public LzySymbol owner;
    // 填充器
    public LzySymbol.Completer completer;
    // 类型
    public LzyType type;

    public LzySymbol(int kind,long flags_field,LzyName name,LzyType type,LzySymbol symbol){
        this.kind = kind;
        this.flags_field = flags_field;
        this.type = type;
        this.owner = symbol;
        this.completer = null;
        this.name = name;
    }

    /**
     * 外部的类型
     *      注意: 我们并不处理泛型，也不处理内部类外部类
     * @return
     */
    public LzyType externalType(){
        return type;
    }


    public boolean isInheritedIn(LzySymbol symbol){
        // 当前符号的访问修饰符
        switch ( (int)(this.flags_field & LzyFlags.AccessFlags )  ){
            // 缺省
            case 0:
                // 包符号
                LzyPackageSymbol packageSymbol = this.packge();
                Object obj = symbol;
                while (obj != null && obj != this.owner){
                    if (  ((LzySymbol)obj).packge() != packageSymbol ){
                        return false;
                    }
                    obj = ((LzySymbol)obj).type.supertype().tsym;
                }
                // 不是接口
                return (symbol.flags()&LzyFlags.INTERFACE) == 0 ;
            case LzyFlags.PUBLIC:
                return true;
            case LzyFlags.PRIVATE:
                return this.owner == symbol;
            default:
                throw new AssertionError();
            case LzyFlags.PROTECTED:
                return (symbol.flags() & LzyFlags.INTERFACE) == 0;

        }
    }

    /**
     * 满足成员关系
     *      情况一: 方法属于这个类
     *      情况二: 方法所在的类，是这个类的子类
     * @param typeSymbol
     * @return
     */
    public boolean isMemberOf(LzyTypeSymbol typeSymbol){
        // 满足所属关系
        if ( this.owner == typeSymbol   ){
            return true;
        }else if ( typeSymbol.isSubClass(this.owner) && this.isInheritedIn(typeSymbol) && (this.kind == LzyKinds.MTH || !this.hiddenIn((LzyClassSymbol) typeSymbol))  ){//
            return true;
        }
        return false;
    }


    private boolean hiddenIn(LzyClassSymbol classSymbol){
        while (this.owner != classSymbol){
            // 作用域下查找
            LzyScope.Entry entry = classSymbol.members().lookup(this.name);
            while (entry.scope != null){
                if ( entry.sym.kind == this.kind ){
                    return entry.sym != this;
                }
                entry = entry.next();
            }
            // 父类
            LzyType supertype = classSymbol.type.supertype();
            if (supertype.tag != LzyTypeTags.CLASS){
                return false;
            }
            classSymbol = (LzyClassSymbol) supertype.tsym;
        }
        return false;
    }


    // 调用填充
    public void complete(){
        if (this.completer != null){
            LzySymbol.Completer c = this.completer;
            // 符号填充只进行一次
            this.completer = null;
            c.complete(this);
        }
    }

    public long flags(){
        return this.flags_field;
    }

    public LzySymbol clone(LzySymbol var1) {
        throw new AssertionError();
    }


    public interface Completer {
        void complete(LzySymbol var1);
    }

    public LzyName flatName(){
        return fullName();
    }

    public LzyName fullName() {
        return this.name;
    }


    public boolean isConstructor() {
        return this.name == this.name.lzyTable.init;
    }


    public boolean isSubClass(LzySymbol superSymbol){
        throw new AssertionError();
    }


    public LzyMethodType asMethodType() {
        throw new AssertionError();
    }


    public LzySymbol asMemberOf(LzyType type){
        throw new AssertionError();
    }

    /**
     * 是否存在: 防止非法强行创建出无效的包
     * @return
     */
    public boolean exists() {
        return (this.flags_field&LzyFlags.EXISTS) != 0;
    }


    // 返回作用域
    public LzyScope members(){
        return null;
    }
    // 包符号
    public LzyPackageSymbol packge(){
        LzySymbol symbol = this;
        while (symbol.kind != LzyKinds.PCK){
            symbol = symbol.owner;
        }
        return (LzyPackageSymbol) symbol;
    }

    public static class CompletionFailure extends RuntimeException{
        public LzySymbol sym;
        public String errmsg;

        public String getMessage(){
            return this.errmsg;
        }

        public CompletionFailure(LzySymbol sym, String errmsg) {
            this.sym = sym;
            this.errmsg = errmsg;
        }
    }

}

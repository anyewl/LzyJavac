package work.liziyun.code.symbol;


import work.liziyun.code.LzyCode;
import work.liziyun.code.LzyScope;
import work.liziyun.code.type.LzyType;
import work.liziyun.code.type.LzyTypeTags;
import work.liziyun.tag.LzyFlags;
import work.liziyun.tag.LzyKinds;
import work.liziyun.util.LzyList;
import work.liziyun.world.LzyName;



public class LzyMethodSymbol extends LzySymbol {

    public LzyCode code = null;
    public static final LzyList emptyList = LzyList.nil();


    // 克隆出一个新对象
    @Override
    public LzySymbol clone(LzySymbol sym){
        LzyMethodSymbol lzyMethodSymbol = new LzyMethodSymbol(this.flags_field, this.name, this.type, sym);
        lzyMethodSymbol.code = this.code;
        return lzyMethodSymbol;
    }

    // 实现
    public LzySymbol implemented(LzyTypeSymbol typeSymbol){
        // 获取所有的接口
        LzyList interfaceList = typeSymbol.type.interfaces();
        LzySymbol sym = null;
        while ( sym == null && interfaceList.nonEmpty() ){
            LzyTypeSymbol interfaceTypeSymbol = ((LzyType) interfaceList.head).tsym;
            // 接口作用域下寻找
            LzyScope.Entry entry = interfaceTypeSymbol.members().lookup(this.name);
            while (sym == null && entry.scope != null){
                // 重写
                boolean overFlag = this.overrides(entry.sym,(LzyTypeSymbol) this.owner);
                // 返回值相同
                boolean sameReturnFlag = this.type.restype().isSameType(entry.sym.type.restype());
                if (overFlag && sameReturnFlag){
                    sym = entry.sym;
                }
                if (sym == null){
                    // 递归:
                    sym = this.implemented(interfaceTypeSymbol);
                }
                // 下一个
                entry = entry.next();
            }
            // 下一个
            interfaceList = interfaceList.tail;
        }
        return sym;
    }

    public LzyMethodSymbol implementation(LzyTypeSymbol typeSymbol){
        LzyType type = typeSymbol.type;
        while (type.tag == LzyTypeTags.CLASS){

            LzyTypeSymbol tsym = type.tsym;

            LzyScope.Entry entry = tsym.members().lookup(this.name);
            while (entry.scope != null){
                if (entry.sym.kind == LzyKinds.MTH){
                    LzyMethodSymbol methodSymbol = (LzyMethodSymbol)entry.sym;
                    if ( methodSymbol.overrides(this,typeSymbol) ){
                        return  methodSymbol;
                    }
                }
                entry = entry.next();
            }

            // 继承
            type = type.supertype();
        }
        // 分析: 不可能返回true
        if (LzyType.isDerivedRaw(typeSymbol.type)){
            return this.implementation(typeSymbol.type.supertype().tsym);
        }else {
            return null;
        }
    }

    @Override
    public LzySymbol asMemberOf(LzyType type){
        return new LzyMethodSymbol(this.flags_field,this.name,type.memberType(this),this.owner);
    }

    public LzyMethodSymbol binaryImplementation(LzyClassSymbol classSymbol){
        LzyTypeSymbol typeSymbol = classSymbol;
        while ( typeSymbol != null){
            LzyScope.Entry entry = typeSymbol.members().lookup(this.name);
            while (entry.scope != null){
                if ( entry.sym.kind == LzyKinds.MTH && ((LzyMethodSymbol)entry.sym).binaryOverrides(this,classSymbol) ){
                    return (LzyMethodSymbol) entry.sym;
                }
                // 下一个
                entry = entry.next();
            }
            // 父类
            typeSymbol = typeSymbol.type.supertype().tsym;
        }
        return null;
    }


    public boolean binaryOverrides(LzySymbol symbol,LzyTypeSymbol typeSymbol){
        // 1. 不是构造方法 2.是一个方法
        if ( !this.isConstructor() && symbol.kind == LzyKinds.MTH ){
            if (this == symbol){
                return true;
            }else {
                LzyMethodSymbol methodSymbol = (LzyMethodSymbol)symbol;
                if (methodSymbol.isOverridableIn((LzyTypeSymbol) this.owner)){
                    return true;
                }else{
                    return ((this.flags() & LzyFlags.ABSTRACT)==0) && methodSymbol.isOverridableIn(typeSymbol) && this.isMemberOf(typeSymbol) ;
                }
            }
        }else{
            return false;
        }
    }


    /**
     * 1. 当前类: 接口中方法
     * @param subClassSymbol 实现类
     * @return
     */
    private boolean isOverridableIn(LzyTypeSymbol subClassSymbol) {
        // 接口中方法的修饰符
        switch ( (int)(this.flags_field & LzyFlags.AccessFlags) ){
            case 0:// 默认: 本类同包
                return this.packge()==subClassSymbol.packge() && ( (subClassSymbol.flags() & LzyFlags.INTERFACE) == 0L ) ;
            case LzyFlags.PUBLIC:
                return true;
            case LzyFlags.PRIVATE:
                return false;
            default:
                throw  new AssertionError();
            case LzyFlags.PROTECTED:
                // 实现类不是接口
                return (subClassSymbol.flags()&LzyFlags.INTERFACE) == 0;
        }
    }

    /**
     * 重写
     *      遗留的问题: 返回值的校验，在哪里触发?
     * @param symbol
     * @param typeSymbol
     * @return
     */
    public boolean overrides(LzySymbol symbol,LzyTypeSymbol typeSymbol){
        if (!this.isConstructor() && symbol.kind == LzyKinds.MTH ){
            LzyMethodSymbol methodSymbol = (LzyMethodSymbol)symbol;
            if (
            // 权限是支持的
            methodSymbol.isOverridableIn( (LzyTypeSymbol) this.owner  )
            &&
            // 方法实现所属于类 和 接口中方法所属于类，是继承关系
            this.owner.type.asSuper(methodSymbol.owner) != null
            &&
            // 相同的参数: 1. 方法的形式参数 2. 返回值类型
            this.type.hasSameArgs(methodSymbol.type)
            ){
                return true;
            }else{
                // 条件一: 不是抽象
                // 条件二: 有权限
                // 条件三: 所属关系
                // 条件四: 相同的方法参数列表
                return  ((this.flags() & LzyFlags.ABSTRACT)==0) && methodSymbol.isOverridableIn(typeSymbol) && this.isMemberOf(typeSymbol) && this.type.hasSameArgs(methodSymbol.type) ;
            }
        }else{
            return false;
        }
    }


    public LzyMethodSymbol(long flags_field, LzyName name, LzyType type, LzySymbol symbol) {
        super(LzyKinds.MTH, flags_field, name, type, symbol);
    }

    public static class OperatorSymbol extends LzyMethodSymbol {

        public int opcode;

        public OperatorSymbol( LzyName name, LzyType type,int opcode, LzySymbol symbol) {
            super( (LzyFlags.STATIC|LzyFlags.PUBLIC), name, type, symbol);
            this.opcode = opcode;
        }
    }
}

package work.liziyun.code.symbol;



import work.liziyun.code.type.LzyType;
import work.liziyun.tag.LzyKinds;
import work.liziyun.util.LzyList;
import work.liziyun.world.LzyName;

public  class LzyVarSymbol extends LzySymbol  {

    public int pos ;
    public int adr = -1;
    public Object constValue;
    public static final LzyList emptyList = LzyList.nil();

    public LzySymbol asMemberOf(LzyType type){
        return new LzyVarSymbol(this.flags_field,this.name,type.memberType(this),this.owner);
    }

    public LzyVarSymbol(long flags_field, LzyName name, LzyType type, LzySymbol symbol) {
        super(LzyKinds.VAR,flags_field,name,type,symbol);
    }


    // 克隆
    @Override
    public LzySymbol clone(LzySymbol var1) {
        LzyVarSymbol varSymbol = new LzyVarSymbol(this.flags_field, this.name, this.type, var1);
        varSymbol.pos = this.pos;
        varSymbol.adr = this.adr;
        varSymbol.constValue = this.constValue;
        return varSymbol;
    }
}

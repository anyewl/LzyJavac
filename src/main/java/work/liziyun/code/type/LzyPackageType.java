package work.liziyun.code.type;

import work.liziyun.code.symbol.LzyTypeSymbol;

public class LzyPackageType extends LzyType {

    public LzyPackageType(LzyTypeSymbol typeSymbol) {
        super(LzyTypeTags.PACKAGE, typeSymbol);
    }

    public boolean isSameType(LzyType type){
        return type==this;
    }

    public String toString(){
        return this.tsym.fullName().toString();
    }
}

package work.liziyun.code.type;

import work.liziyun.code.symbol.LzyClassSymbol;
import work.liziyun.tag.LzyKinds;


public class LzyErrorType extends  LzyClassType{

    public LzyErrorType(){
        super(null,null);
        this.tag = LzyTypeTags.firstPartialTag;

    }

    public LzyErrorType(LzyClassSymbol tsym) {
        super( null,tsym);
        tsym.type = this;
        tsym.kind = LzyKinds.ERR;

    }

    public LzyType elemtype() {
        return this;
    }


    public boolean isSameType(LzyType var1) {
        return true;
    }
}

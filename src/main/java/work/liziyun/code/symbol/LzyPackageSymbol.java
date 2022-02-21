package work.liziyun.code.symbol;

import work.liziyun.code.LzyScope;
import work.liziyun.code.type.LzyPackageType;
import work.liziyun.code.type.LzyType;
import work.liziyun.tag.LzyFlags;
import work.liziyun.tag.LzyKinds;
import work.liziyun.world.LzyName;

public class LzyPackageSymbol extends LzyTypeSymbol {

    public LzyScope members_field;
    public LzyName fullname;

    public LzyPackageSymbol(LzyName name,LzySymbol symbol){
        this(name,null,symbol);
        this.type = new LzyPackageType(this);
    }

    public LzyPackageSymbol( LzyName name, LzyType type, LzySymbol symbol) {
        super(LzyFlags.EMPTY, name, type, symbol);
        // 设置类型
        this.kind = LzyKinds.PCK;
        // 成员
        this.members_field = null;
        // 全限定类名
        this.fullname = formFullName(name,symbol);
    }

    public long flags(){
        if (this.completer != null){
            this.complete();
        }
        return  this.flags_field;
    }


    public LzyScope members(){
        if (this.completer != null){
            this.complete();
        }
        return this.members_field;
    }

    @Override
    public LzyName fullName(){
        return this.fullname;
    }

    public String toString(){
        return "package "+this.fullname;
    }


}

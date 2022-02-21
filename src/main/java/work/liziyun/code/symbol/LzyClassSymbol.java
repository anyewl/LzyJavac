package work.liziyun.code.symbol;


import work.liziyun.code.LzyPool;
import work.liziyun.code.LzyScope;
import work.liziyun.code.type.LzyClassType;
import work.liziyun.code.type.LzyErrorType;
import work.liziyun.code.type.LzyType;
import work.liziyun.code.type.LzyTypeTags;
import work.liziyun.io.LzyJavaFile;
import work.liziyun.tag.LzyFlags;
import work.liziyun.util.LzyList;
import work.liziyun.world.LzyName;

public class LzyClassSymbol extends LzyTypeSymbol {
    public LzyScope members_field;
    public LzyName fullname;
    public LzyName flatname;
    public LzyName sourcefile;
    // 两种情况: 1. 压缩包 2.普通文件
    public LzyJavaFile classfile;
    public LzyPool pool;
    private int rank_field;
    public static final LzyList emptyList = LzyList.nil();

    public LzyClassSymbol(long flags_field, LzyName name, LzySymbol symbol){
        this(flags_field,name,new LzyClassType(LzyType.noType,null),symbol);
        // 设置Type中Symbol
        this.type.tsym = this;
    }

    public LzyClassSymbol(long flags_field, LzyName name, LzyType type, LzySymbol symbol) {
        super(flags_field, name, type, symbol);
        this.rank_field = -1;
        this.members_field = null;
        this.fullname = formFullName(name,symbol);
        this.flatname = formFlatName(name,symbol);
        this.sourcefile = null;
        this.classfile = null;
        this.pool = null;
    }

    public void complete(){
        try{
            super.complete();
        }catch(Exception e) {
            this.flags_field |= 9;
            // 异常时创建错误类型: ErrorType
            this.type = new LzyErrorType(this);
            throw  e;
        }
    }

    public long flags(){
        if (this.completer != null){
            this.complete();
        }
        return this.flags_field;
    }

    public LzyScope members(){
        if ( this.completer != null ){
            this.complete();
        }
        return this.members_field;
    }


    public LzyName flatName(){
        return this.flatname;
    }

    public LzyName fullName(){
        return this.fullname;
    }

    @Override
    public boolean isSubClass(LzySymbol superSymbol){
        if (this == superSymbol){
            return true;
        }else {
            LzyType lzyType;
            // 接口: 接口间的继承关系
            if ( (superSymbol.flags() & LzyFlags.INTERFACE) != 0  ){
                lzyType = this.type;
                while (lzyType.tag == LzyTypeTags.CLASS){
                    // 当前类的接口
                    LzyList interfaceList = lzyType.interfaces();
                    while (interfaceList.nonEmpty()){
                        // 递归寻找
                        if ( ((LzyType)interfaceList.head).tsym.isSubClass(superSymbol) ){
                            return true;
                        }
                        // 下一个接口
                        interfaceList = interfaceList.tail;
                    }
                    // 父类
                    lzyType = lzyType.supertype();
                }
            }else{ // 类中的继承关系
                lzyType = this.type;
                while (lzyType.tag == LzyTypeTags.CLASS){
                    if (lzyType.tsym == superSymbol){
                        return true;
                    }
                    // 父类
                    lzyType = lzyType.supertype();
                }
            }
            return false;
        }
    }



}

package work.liziyun.code.type;

import  work.liziyun.code.symbol.LzyTypeSymbol;
import work.liziyun.world.LzyName;
import work.liziyun.world.LzyTable;

public class LzyArrayType extends LzyType {
    // 数组中元素类型
    public LzyType elemtype;

    public LzyArrayType(LzyType elemType, LzyTypeSymbol tsym) {
        super(LzyTypeTags.ARRAY, tsym);
        this.elemtype = elemType;
    }

    public LzyType elemtype() {
        return this.elemtype;
    }

    public void complete(){
        this.elemtype.complete();
    }

    public int hashCode(){
        return 352+this.elemtype.hashCode();
    }


    public int dimensions() {
        int var1 = 0;

        for(Object var2 = this; ((LzyType)var2).tag == 11; var2 = ((LzyType)var2).elemtype()) {
            ++var1;
        }

        return var1;
    }


    @Override
    public boolean equals(Object obj){
        if (this == obj){
            return true;
        }else if ( obj instanceof LzyArrayType && this.elemtype.equals( ((LzyArrayType)obj).elemtype  ) ){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean isSameType(LzyType type){
        if (this == type) {
            return true;
        } else if (type.tag >= LzyTypeTags.ERROR) {
            // 错误类型
            return type.isSameType(this);
        } else {
            return type.tag == LzyTypeTags.ARRAY && this.elemtype.isSameType(type.elemtype());
        }
    }



    @Override
    public boolean isSubType(LzyType type){
        // 递归结束: 相等
        if (this == type){
            return true;
        }else if(type.tag >= LzyTypeTags.ERROR){
            // 错误
            return type.isSubType(this);
        }else if (type.tag == LzyTypeTags.ARRAY){
            // 基本数据类型
            if (this.elemtype.tag <= LzyTypeTags.BOOLEAN){
                // 直接相等判断
                return this.elemtype.isSameType(type.elemtype());
            }else{// 引用数据类型
                // 继承体系中查找: 递归
               return this.elemtype.isSubType(type.elemtype());
            }
        }else if (type.tag != LzyTypeTags.CLASS){
            return false;
        }else {
            // 全限定类名
            LzyName name = type.tsym.fullName();
            LzyTable lzyTable = name.lzyTable;
            // 1.Object 2.Cloneable 3.Serializable
            return name == lzyTable.java_lang_Object || name == lzyTable.java_lang_Cloneable || name == lzyTable.java_io_Serializable;
        }
    }



}

package work.liziyun.code.type;


import work.liziyun.code.symbol.LzySymbol;
import work.liziyun.code.symbol.LzyTypeSymbol;
import work.liziyun.tag.LzyFlags;
import work.liziyun.tag.LzyKinds;
import work.liziyun.util.LzyList;

public class LzyClassType extends LzyType {

    // 外部类型
    public LzyType outer_field;
    // 继承
    public LzyType supertype_field;
    // 接口
    public LzyList interfaces_field;

    public LzyClassType(LzyType outer_field,LzyTypeSymbol tsym) {
        super(CLASS, tsym);
        this.outer_field = outer_field;
    }

    // 类符号的填充
    public void complete(){
        if(this.tsym.completer != null){
            this.tsym.complete();
        }
    }

    public LzyType asSuper(LzySymbol superSymbol){
        // 递归结束: 相等时
        if (this.tsym == superSymbol){
            return this;
        }else{
            // 当前类的父类
            LzyType supertype = this.supertype();
            //
            if (supertype.tag == LzyTypeTags.CLASS || supertype.tag == LzyTypeTags.ERROR ){
                // 当前类的父类: 递归
                LzyType rs = supertype.asSuper(superSymbol);
                if (rs != null){
                    return rs;
                }
            }
            // 1. 父类是一个接口 2. 当前类也是接口
            if ( (superSymbol.flags()& LzyFlags.INTERFACE) != 0){
                LzyList interfaceList = this.interfaces();
                while (interfaceList.nonEmpty()){
                    LzyType rs = ((LzyType) interfaceList.head).asSuper(superSymbol);
                    if (rs != null){
                        return rs;
                    }
                    interfaceList = interfaceList.tail;
                }
            }
            return null;
        }
    }


    public boolean isSuperType(LzyType superType) {
        return superType.isSubType(this);
    }

    // 继承判断
    public boolean isSubType(LzyType superType){
        // 结束递归的地方: 两个类相等!
        if (this == superType){
            return true;
        }else if (superType.tag >= LzyTypeTags.ERROR){
            return superType.isSubType(this);
        }else if (this.tsym == superType.tsym){
            // 字面量类型的引用消除，会创建全新的Type。利用this == superType比较无效!
            return true;
        }else {
            // 比较的类是一个接口
            if ( (superType.tsym.flags()&INTERFACE) != 0 ){
                LzyList interfaces = this.interfaces();
                while ( interfaces!=null && interfaces.nonEmpty()  ){
                    if ( ((LzyType)interfaces.head).isSubType(superType) ){
                        return true;
                    }
                    // 移动指针
                    interfaces = interfaces.tail;
                }
            }
            // 当前类的父类
            LzyType t = this.supertype();
            // 递归比较: 拿自己的父类。直到两个类相等，那么符合继承关系。
            return t.tag == 10 && t.isSubType(superType)?true:t.isErroneous();
        }
    }


    @Override
    public LzyType supertype(){
        // 继承为空
        if (this.supertype_field == null){
            this.complete();
            // 从符号中获取继承的类型
            LzyType type = ((LzyClassType)this.tsym.type).supertype_field;
            if (type == null){
                this.supertype_field = noType;
            }else if (this == this.tsym.type){
                // 外部封装类型 和 内部封装类型 一致
                this.supertype_field = type;
            }else{
                this.supertype_field = type;
            }
        }
        return this.supertype_field;
    }



    public LzyType constType(Object value){
        LzyClassType lzyClassType = new LzyClassType(this.outer_field, this.tsym);
        lzyClassType.constValue = value;
        return lzyClassType;
    }



    public LzyList interfaces(){
        if (this.interfaces_field == null){
            this.complete();
            LzyList interfaces_field = ((LzyClassType)this.tsym.type).interfaces_field;
            if (interfaces_field == null){
                this.interfaces_field = LzyType.emptyList;
            }else if (this == this.tsym.type){
                this.interfaces_field = interfaces_field;
            }else{
                this.interfaces_field = interfaces_field;
            }
        }
        return this.interfaces_field;
    }


    @Override
    public LzyType memberType(LzySymbol symbol){
        // 这里在尝试擦除泛型
        return symbol.type;
    }

    @Override
    public boolean isSameType(LzyType type){
        if (this == type){
            return true;
        }else if (type.tag >= LzyTypeTags.ERROR){
            // 错误类型
            return type.isSameType(this);
        }else{
            return this.tsym == type.tsym;
        }
    }

    public LzyType outer(){
        if (this.outer_field == null){
            this.complete();
            this.outer_field = this.tsym.type.outer();
        }
        return this.outer_field;
    }

    private String className(LzySymbol symbol,boolean bool){
        // 名称长度为0: 匿名实现类
        if ( symbol.name.length == 0 ){
            return "<anonymous " + (symbol.type.interfaces().nonEmpty() ? (LzyType)symbol.type.interfaces().head : symbol.type.supertype()) + ">";
        }else {
            return bool?symbol.fullName().toString():symbol.name.toString();
        }
    }

    public String toString(){
        StringBuffer stringBuffer = new StringBuffer();
        // 内部类情况
        if (this.outer().tag == LzyTypeTags.CLASS && this.tsym.owner.kind == LzyKinds.TYP){
            stringBuffer.append(this.outer()).toString();
            stringBuffer.append(".");
            stringBuffer.append(this.className(this.tsym,false));
        }else {
            stringBuffer.append(this.className(this.tsym,true));
        }
        return stringBuffer.toString();
    }

}

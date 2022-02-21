package work.liziyun.code.symbol;

import work.liziyun.code.type.LzyType;
import work.liziyun.tag.LzyKinds;
import work.liziyun.world.LzyName;

public class LzyTypeSymbol extends LzySymbol{

    public LzyTypeSymbol(long flags_field, LzyName name, LzyType type, LzySymbol symbol) {
        super(LzyKinds.TYP, flags_field, name, type, symbol);
    }

    // 尝试返回全限定类名
    public static LzyName formFullName(LzyName name, LzySymbol symbol) {
        if ( symbol == null) {
            return name;
        } else if ( symbol.kind != LzyKinds.AllKinds && ( symbol.kind & (LzyKinds.MTH|LzyKinds.VAR)) != 0) {
            return name;
        } else {
            LzyName n =  symbol.fullName();
            // 拼接全限定类名
            if ( n != null && n != n.lzyTable.empty && n != n.lzyTable.emptyPackage ){
                return n.append('.',name);
            }else{
                return name;
            }
        }
    }
    //
    public static LzyName formFlatName(LzyName className,LzySymbol packSymbol){
        if (packSymbol != null && (packSymbol.kind & (LzyKinds.MTH|LzyKinds.VAR)) == 0){
            char c ;
            // 如果是一个类型
            if ( packSymbol.kind == LzyKinds.TYP ){
                // 考虑内部类的情况
                c = '$';
            }else{
                c = '.';
            }
            LzyName fullname = packSymbol.flatName();
            // 有包名
            if (fullname != null && fullname != fullname.lzyTable.empty && fullname != fullname.lzyTable.emptyPackage ){
                return fullname.append(c,className);
            }else{
                // 无包名
                return className;
            }
        }else{
            // 无包名
            return className;
        }
    }
}

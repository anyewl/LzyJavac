package work.liziyun.comp;


import work.liziyun.code.symbol.LzyMethodSymbol;
import work.liziyun.code.symbol.LzySymbol;
import work.liziyun.code.symbol.LzyTypeSymbol;
import work.liziyun.code.type.LzyClassType;
import work.liziyun.code.type.LzyType;
import work.liziyun.code.type.LzyTypeTags;
import work.liziyun.tag.LzyByteCodes;
import work.liziyun.tag.LzyFlags;
import work.liziyun.util.LzyContext;
import work.liziyun.util.LzyList;
import work.liziyun.world.LzyTable;

public class LzyCheck {
    private static final LzyContext.Key key = new LzyContext.Key();

    private LzyTable names;

    public static LzyCheck instance(LzyContext LzyContext) {
        LzyCheck check = (LzyCheck)LzyContext.get(key);
        if (check == null) {
            check = new LzyCheck(LzyContext);
        }
        return check;
    }

    public LzyCheck(LzyContext LzyContext) {
        this.names = LzyTable.instance(LzyContext);
    }


    /**
     * 检查类型是 类 还是 接口
     * @param pos
     * @param type
     * @return
     */
    LzyType checkClassType(int pos, LzyType type){
        // 检查是否Class
        if ( type.tag != LzyTypeTags.CLASS && type.tag != LzyTypeTags.NONE){
            System.out.println("编译错误: 期望Type是一个TypeTag.Class,可能是一个接口或类");
            return null;
        }else{
            return type;
        }
    }

    /**
     * 检查修饰符: 1. 是否合法 2. 缺省的修饰符添加进去
     * @param var1
     * @param var2
     * @param var4
     * @return
     */
    long checkFlags(int var1, long var2, LzySymbol var4) {
        long var7 = 0L;
        long var5;
        switch(var4.kind) {
            case 2:
                 if (var4.owner.kind != 2) {
                    var5 = 3601L;
                } else {
                    var5 = 3607L;
                    if (var4.owner.owner.kind == 1 || (var4.owner.flags_field & 8L) != 0L) {
                        var5 |= 8L;
                    }

                    if ((var2 & 512L) != 0L) {
                        var7 = 8L;
                    }
                }

                if ((var2 & 512L) != 0L) {
                    var7 |= 1024L;
                }

                var7 |= var4.owner.flags_field & 2048L;
                break;
            case 4:
                if (var4.owner.kind != 2) {
                    var5 = 8589934608L;
                } else if ((var4.owner.flags_field & 512L) != 0L) {
                    var7 = 25L;
                    var5 = 25L;
                } else {
                    var5 = 223L;
                }
                break;
            case 16:
                if (var4.name == this.names.init) {
                    var5 = 7L;
                } else if ((var4.owner.flags_field & 512L) != 0L) {
                    var7 = 1025L;
                    var5 = 1025L;
                } else {
                    var5 = 3391L;
                }

                if (((var2 | var7) & 1024L) == 0L) {
                    var7 |= var4.owner.flags_field & 2048L;
                }
                break;
            default:
                throw new AssertionError();
        }

        long var9 = var2 & 4095L & ~var5;

        return var2 & (var5 | -4096L) | var7;
    }

    /**
     * 除0异常的检查
     * @param operator
     * @param operand
     */
    void checkDivZero( LzySymbol operator, LzyType operand) {
        if (operand.constValue != null
                && ((Number) (operand.constValue)).longValue() == 0) {
            int opc = ((LzyMethodSymbol.OperatorSymbol)operator).opcode;
            if (opc == LzyByteCodes.idiv || opc == LzyByteCodes.imod
                    || opc == LzyByteCodes.ldiv || opc == LzyByteCodes.lmod) {
                System.out.println("编译错误: 除数不能是0");
            }
        }
    }

    LzyType checkNonCyclic(int i,LzyType type){
        // 类符号
        LzyTypeSymbol typeSymbol = ((LzyType)type).tsym;

        if ( (typeSymbol.flags_field& LzyFlags.LOCKED) != 0 ){
            System.out.println("编译错误: 上锁LOCK"+type.tsym.name.toString());
        }else{
            // 如果有ACYCLIC修饰: 即无环
            if ( (typeSymbol.flags_field&LzyFlags.ACYCLIC) != 0 ){
                return type;
            }
            // 非错误类型
            if ( !typeSymbol.type.isErroneous() ){
                try {
                    // 上锁操作
                    typeSymbol.flags_field |= LzyFlags.LOCKED;
                    // 接口
                    // 注意: interfaces() ---> 如果没有接口，那么会触发类的加载!
                    LzyList interList = typeSymbol.type.interfaces();
                    while ( interList.nonEmpty() ){
                        interList.head = checkNonCyclic(i  , (LzyType) interList.head );
                        //下一个
                        interList = interList.tail;
                    }
                    // 继承
                    LzyType supertype = typeSymbol.type.supertype();
                    // 如果父类
                    if (supertype != null && supertype.tag == LzyTypeTags.CLASS ){
                        // 递归
                        ((LzyClassType)typeSymbol.type).supertype_field = this.checkNonCyclic(i,supertype);
                    }
                }finally {
                    // 清除锁标记
                    typeSymbol.flags_field &= (-(LzyFlags.LOCKED+1));
                }
            }
        }
        // 标记无环
        typeSymbol.flags_field |= LzyFlags.ACYCLIC;
        return type;
    }


}

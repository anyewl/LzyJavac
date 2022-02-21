package work.liziyun.comp;






import work.liziyun.code.LzySymtab;
import work.liziyun.code.type.LzyType;
import work.liziyun.tag.LzyByteCodes;
import work.liziyun.util.LzyContext;
import work.liziyun.util.LzyList;

import static work.liziyun.code.type.LzyTypeTags.*;
import static work.liziyun.tag.LzyByteCodes.*;

public class LzyConstFold {
    protected static final LzyContext.Key constFoldKey =
            new LzyContext.Key();

    private LzySymtab syms;

    public static LzyConstFold instance(LzyContext LzyContext) {
        LzyConstFold instance = (LzyConstFold)LzyContext.get(constFoldKey);
        if (instance == null)
            instance = new LzyConstFold(LzyContext);
        return instance;
    }

    private LzyConstFold(LzyContext LzyContext) {
        LzyContext.put(constFoldKey, this);
        syms = LzySymtab.instance(LzyContext);
    }

    static Integer minusOne = -1;
    static Integer zero     = 0;
    static Integer one      = 1;

    /** Convert boolean to integer (true = 1, false = 0).
     */
    private static Integer b2i(boolean b) {
        return b ? one : zero;
    }
    private static int intValue(Object x) { return ((Number)x).intValue(); }
    private static long longValue(Object x) { return ((Number)x).longValue(); }
    private static float floatValue(Object x) { return ((Number)x).floatValue(); }
    private static double doubleValue(Object x) { return ((Number)x).doubleValue(); }

    /** Fold binary or unary operation, returning constant type reflecting the
     *  operations result. Return null if fold failed due to an
     *  arithmetic exception.
     *  @param opcode    The operation's opcode instruction (usually a byte code),
     *                   as entered by class Symtab.
     *  @param argtypes  The operation's argument types (a LzyList of length 1 or 2).
     *                   Argument types are assumed to have non-null constValue's.
     */
    LzyType fold(int opcode, LzyList<LzyType> argtypes) {
        int argCount = argtypes.length();
        if (argCount == 1)
            return fold1(opcode, argtypes.head);
        else if (argCount == 2)
            return fold2(opcode, argtypes.head, argtypes.tail.head);
        else
            throw new AssertionError();
    }

    /** Fold unary operation.
     *  @param opcode    The operation's opcode instruction (usually a byte code),
     *                   as entered by class Symtab.
     *                   opcode's ifeq to ifge are for postprocessing
     *                   xcmp; ifxx pairs of instructions.
     *  @param operand   The operation's operand type.
     *                   Argument types are assumed to have non-null constValue's.
     */
    LzyType fold1(int opcode, LzyType operand) {
        try {
            Object od = operand.constValue;
            switch (opcode) {
                case nop:
                    return operand;
                case LzyByteCodes.ineg: // unary -
                    return syms.intType.constType(-intValue(od));
                case ixor: // ~
                    return syms.intType.constType(~intValue(od));
                case bool_not: // !
                    return syms.booleanType.constType(b2i(intValue(od) == 0));
                case ifeq:
                    return syms.booleanType.constType(b2i(intValue(od) == 0));
                case ifne:
                    return syms.booleanType.constType(b2i(intValue(od) != 0));
                case iflt:
                    return syms.booleanType.constType(b2i(intValue(od) < 0));
                case ifgt:
                    return syms.booleanType.constType(b2i(intValue(od) > 0));
                case ifle:
                    return syms.booleanType.constType(b2i(intValue(od) <= 0));
                case ifge:
                    return syms.booleanType.constType(b2i(intValue(od) >= 0));

                case lneg: // unary -
                    return syms.longType.constType(new Long(-longValue(od)));
                case lxor: // ~
                    return syms.longType.constType(new Long(~longValue(od)));

                case fneg: // unary -
                    return syms.floatType.constType(new Float(-floatValue(od)));

                case dneg: // ~
                    return syms.doubleType.constType(new Double(-doubleValue(od)));

                default:
                    return null;
            }
        } catch (ArithmeticException e) {
            return null;
        }
    }

    /** Fold binary operation.
     *  @param opcode    The operation's opcode instruction (usually a byte code),
     *                   as entered by class Symtab.
     *                   opcode's ifeq to ifge are for postprocessing
     *                   xcmp; ifxx pairs of instructions.
     *  @param left      The type of the operation's left operand.
     *  @param right     The type of the operation's right operand.
     */
    LzyType fold2(int opcode, LzyType left, LzyType right) {
        try {
            if (opcode > LzyByteCodes.preMask) {
                // we are seeing a composite instruction of the form xcmp; ifxx.
                // In this case fold both instructions separately.
                LzyType t1 = fold2(opcode >> LzyByteCodes.preShift, left, right);
                return (t1.constValue == null) ? t1
                        : fold1(opcode & LzyByteCodes.preMask, t1);
            } else {
                Object l = left.constValue;
                Object r = right.constValue;
                switch (opcode) {
                    case iadd:
                        return syms.intType.constType(intValue(l) + intValue(r));
                    case isub:
                        return syms.intType.constType(intValue(l) - intValue(r));
                    case imul:
                        return syms.intType.constType(intValue(l) * intValue(r));
                    case idiv:
                        return syms.intType.constType(intValue(l) / intValue(r));
                    case imod:
                        return syms.intType.constType(intValue(l) % intValue(r));
                    case iand:
                        return (left.tag == BOOLEAN
                                ? syms.booleanType : syms.intType)
                                .constType(intValue(l) & intValue(r));
                    case bool_and:
                        return syms.booleanType.constType(b2i((intValue(l) & intValue(r)) != 0));
                    case ior:
                        return (left.tag == BOOLEAN
                                ? syms.booleanType : syms.intType)
                                .constType(intValue(l) | intValue(r));
                    case bool_or:
                        return syms.booleanType.constType(b2i((intValue(l) | intValue(r)) != 0));
                    case ixor:
                        return (left.tag == BOOLEAN
                                ? syms.booleanType : syms.intType)
                                .constType(intValue(l) ^ intValue(r));
                    case ishl: case ishll:
                        return syms.intType.constType(intValue(l) << intValue(r));
                    case ishr: case ishrl:
                        return syms.intType.constType(intValue(l) >> intValue(r));
                    case iushr: case iushrl:
                        return syms.intType.constType(intValue(l) >>> intValue(r));
                    case if_icmpeq:
                        return syms.booleanType.constType(
                                b2i(intValue(l) == intValue(r)));
                    case if_icmpne:
                        return syms.booleanType.constType(
                                b2i(intValue(l) != intValue(r)));
                    case if_icmplt:
                        return syms.booleanType.constType(
                                b2i(intValue(l) < intValue(r)));
                    case if_icmpgt:
                        return syms.booleanType.constType(
                                b2i(intValue(l) > intValue(r)));
                    case if_icmple:
                        return syms.booleanType.constType(
                                b2i(intValue(l) <= intValue(r)));
                    case if_icmpge:
                        return syms.booleanType.constType(
                                b2i(intValue(l) >= intValue(r)));

                    case ladd:
                        return syms.longType.constType(
                                new Long(longValue(l) + longValue(r)));
                    case lsub:
                        return syms.longType.constType(
                                new Long(longValue(l) - longValue(r)));
                    case lmul:
                        return syms.longType.constType(
                                new Long(longValue(l) * longValue(r)));
                    case ldiv:
                        return syms.longType.constType(
                                new Long(longValue(l) / longValue(r)));
                    case lmod:
                        return syms.longType.constType(
                                new Long(longValue(l) % longValue(r)));
                    case land:
                        return syms.longType.constType(
                                new Long(longValue(l) & longValue(r)));
                    case lor:
                        return syms.longType.constType(
                                new Long(longValue(l) | longValue(r)));
                    case lxor:
                        return syms.longType.constType(
                                new Long(longValue(l) ^ longValue(r)));
                    case lshl: case lshll:
                        return syms.longType.constType(
                                new Long(longValue(l) << intValue(r)));
                    case lshr: case lshrl:
                        return syms.longType.constType(
                                new Long(longValue(l) >> intValue(r)));
                    case lushr:
                        return syms.longType.constType(
                                new Long(longValue(l) >>> intValue(r)));
                    case lcmp:
                        if (longValue(l) < longValue(r))
                            return syms.intType.constType(minusOne);
                        else if (longValue(l) > longValue(r))
                            return syms.intType.constType(one);
                        else
                            return syms.intType.constType(zero);
                    case fadd:
                        return syms.floatType.constType(
                                new Float(floatValue(l) + floatValue(r)));
                    case fsub:
                        return syms.floatType.constType(
                                new Float(floatValue(l) - floatValue(r)));
                    case fmul:
                        return syms.floatType.constType(
                                new Float(floatValue(l) * floatValue(r)));
                    case fdiv:
                        return syms.floatType.constType(
                                new Float(floatValue(l) / floatValue(r)));
                    case fmod:
                        return syms.floatType.constType(
                                new Float(floatValue(l) % floatValue(r)));
                    case fcmpg: case fcmpl:
                        if (floatValue(l) < floatValue(r))
                            return syms.intType.constType(minusOne);
                        else if (floatValue(l) > floatValue(r))
                            return syms.intType.constType(one);
                        else if (floatValue(l) == floatValue(r))
                            return syms.intType.constType(zero);
                        else if (opcode == fcmpg)
                            return syms.intType.constType(one);
                        else
                            return syms.intType.constType(minusOne);
                    case dadd:
                        return syms.doubleType.constType(
                                new Double(doubleValue(l) + doubleValue(r)));
                    case dsub:
                        return syms.doubleType.constType(
                                new Double(doubleValue(l) - doubleValue(r)));
                    case dmul:
                        return syms.doubleType.constType(
                                new Double(doubleValue(l) * doubleValue(r)));
                    case ddiv:
                        return syms.doubleType.constType(
                                new Double(doubleValue(l) / doubleValue(r)));
                    case dmod:
                        return syms.doubleType.constType(
                                new Double(doubleValue(l) % doubleValue(r)));
                    case dcmpg: case dcmpl:
                        if (doubleValue(l) < doubleValue(r))
                            return syms.intType.constType(minusOne);
                        else if (doubleValue(l) > doubleValue(r))
                            return syms.intType.constType(one);
                        else if (doubleValue(l) == doubleValue(r))
                            return syms.intType.constType(zero);
                        else if (opcode == dcmpg)
                            return syms.intType.constType(one);
                        else
                            return syms.intType.constType(minusOne);
                    case if_acmpeq:
                        return syms.booleanType.constType(b2i(l.equals(r)));
                    case if_acmpne:
                        return syms.booleanType.constType(b2i(!l.equals(r)));
                    case string_add:
                        return syms.stringType.constType(
                                left.stringValue() + right.stringValue());
                    default:
                        return null;
                }
            }
        } catch (ArithmeticException e) {
            return null;
        }
    }

    /** Coerce constant type to target type.
     *  @param etype      The source type of the coercion,
     *                    which is assumed to be a constant type compatble with
     *                    ttype.
     *  @param ttype      The target type of the coercion.
     */
    LzyType coerce(LzyType etype, LzyType ttype) {
        // WAS if (etype.baseType() == ttype.baseType())
        if (etype.tsym.type == ttype.tsym.type)
            return etype;
        if (etype.tag <= DOUBLE) {
            Object n = etype.constValue;
            switch (ttype.tag) {
                case BYTE:
                    return syms.byteType.constType(0 + (byte)intValue(n));
                case CHAR:
                    return syms.charType.constType(0 + (char)intValue(n));
                case SHORT:
                    return syms.shortType.constType(0 + (short)intValue(n));
                case INT:
                    return syms.intType.constType(intValue(n));
                case LONG:
                    return syms.longType.constType(longValue(n));
                case FLOAT:
                    return syms.floatType.constType(floatValue(n));
                case DOUBLE:
                    return syms.doubleType.constType(doubleValue(n));
            }
        }
        return ttype;
    }
}

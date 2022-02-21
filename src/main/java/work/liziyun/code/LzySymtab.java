package work.liziyun.code;


import work.liziyun.code.symbol.*;
import work.liziyun.code.type.*;
import work.liziyun.tag.LzyFlags;
import work.liziyun.util.LzyContext;
import work.liziyun.util.LzyList;
import work.liziyun.world.LzyName;
import work.liziyun.world.LzyTable;

import java.util.HashMap;

// 默认的类型符号
public class LzySymtab {
    private static final LzyContext.Key symtabKey = new LzyContext.Key();
    private final LzyTable names;
    private final LzyClassReader reader;
    public final LzyPackageSymbol rootPackage;
    public final LzyPackageSymbol emptyPackage;
    public final LzyTypeSymbol noSymbol;
    public final LzyClassSymbol errSymbol;
    public final LzyType byteType;
    public final LzyType charType;
    public final LzyType shortType;
    public final LzyType intType;
    public final LzyType longType;
    public final LzyType floatType;
    public final LzyType doubleType;
    public final LzyType booleanType;
    public final LzyType voidType;
    public final LzyType botType;
    public final LzyType errType;
    public final LzyType unknownType;
    public final LzyClassSymbol arrayClass;
    public final LzyClassSymbol methodClass;
    public final LzyType objectType;
    public final LzyType classType;
    public final LzyType classLoaderType;
    public final LzyType stringType;
    public final LzyType stringBufferType;
    public final LzyType cloneableType;
    public final LzyType serializableType;
    public final LzyType throwableType;
    public final LzyType errorType;
    public final LzyType exceptionType;
    public final LzyType runtimeExceptionType;
    public final LzyType classNotFoundExceptionType;
    public final LzyType noClassDefFoundErrorType;
    public final LzyType assertionErrorType;
    public final LzyVarSymbol lengthVar;
    public final LzyVarSymbol nullConst;
    public final LzyVarSymbol trueConst;
    public final LzyVarSymbol falseConst;
    public final LzyMethodSymbol.OperatorSymbol nullcheck;
    public final LzyType[] typeOfTag = new LzyType[21];
    public final LzyName[] boxedName = new LzyName[21];
    public final HashMap classes = new HashMap();
    public final HashMap packages = new HashMap();
    public final LzyClassSymbol predefClass;


    // 环境
    public static LzySymtab instance(LzyContext LzyContext){
        LzySymtab symtab =(LzySymtab) LzyContext.get(symtabKey);
        if (symtab == null){
            symtab = new LzySymtab(LzyContext);
        }
        return symtab;
    }

    private LzySymtab(LzyContext LzyContext){
        LzyContext.put(symtabKey,this);
        this.names = LzyTable.instance(LzyContext);
        this.byteType = new LzyType(LzyTypeTags.BYTE, (LzyTypeSymbol)null);
        this.charType = new LzyType(LzyTypeTags.CHAR, (LzyTypeSymbol)null);
        this.shortType = new LzyType(LzyTypeTags.SHORT, (LzyTypeSymbol)null);
        this.intType = new LzyType(LzyTypeTags.INT, (LzyTypeSymbol)null);
        this.longType = new LzyType(LzyTypeTags.LONG, (LzyTypeSymbol)null);
        this.floatType = new LzyType(LzyTypeTags.FLOAT, (LzyTypeSymbol)null);
        this.doubleType = new LzyType(LzyTypeTags.DOUBLE, (LzyTypeSymbol)null);
        this.booleanType = new LzyType(LzyTypeTags.BOOLEAN, (LzyTypeSymbol)null);
        this.voidType = new LzyType(LzyTypeTags.VOID, (LzyTypeSymbol)null);
        this.botType = new LzyType(LzyTypeTags.BOT, (LzyTypeSymbol)null);
        this.unknownType = new LzyType(LzyTypeTags.UNKNOWN, (LzyTypeSymbol)null) {
            public boolean isSameType(LzyType var1) {
                return true;
            }

            public boolean isSubType(LzyType var1) {
                return false;
            }

            public boolean isSuperType(LzyType var1) {
                return true;
            }
        };
        this.rootPackage = new LzyPackageSymbol(this.names.empty, (LzySymbol)null);
        this.emptyPackage = new LzyPackageSymbol(this.names.emptyPackage, this.rootPackage);
        this.noSymbol = new LzyTypeSymbol(0L, this.names.empty, LzyType.noType, this.rootPackage);
        this.noSymbol.kind = 0;
        this.errSymbol = new LzyClassSymbol(9L, this.names.any, (LzyType)null, this.rootPackage);
        this.errType = new LzyErrorType(this.errSymbol);
        this.initType(this.byteType, "byte", "Byte");
        this.initType(this.shortType, "short", "Short");
        this.initType(this.charType, "char", "Character");
        this.initType(this.intType, "int", "Integer");
        this.initType(this.longType, "long", "Long");
        this.initType(this.floatType, "float", "Float");
        this.initType(this.doubleType, "double", "Double");
        this.initType(this.booleanType, "boolean", "Boolean");
        this.initType(this.voidType, "void", "Void");
        this.initType(this.botType, "<nulltype>");
        this.initType(this.errType, this.errSymbol);
        this.initType(this.unknownType, "<any?>");
        this.arrayClass = new LzyClassSymbol(LzyFlags.PUBLIC, this.names.Array, this.noSymbol);
        this.methodClass = new LzyClassSymbol(LzyFlags.PUBLIC, this.names.Method, this.noSymbol);
        this.predefClass = new LzyClassSymbol(LzyFlags.PUBLIC, this.names.empty, this.rootPackage);
        LzyScope var2 = new LzyScope(this.predefClass);
        this.predefClass.members_field = var2;
        var2.enter(this.byteType.tsym);
        var2.enter(this.shortType.tsym);
        var2.enter(this.charType.tsym);
        var2.enter(this.intType.tsym);
        var2.enter(this.longType.tsym);
        var2.enter(this.floatType.tsym);
        var2.enter(this.doubleType.tsym);
        var2.enter(this.booleanType.tsym);
        var2.enter(this.errType.tsym);
        this.classes.put(this.predefClass.fullname, this.predefClass);
        this.reader = LzyClassReader.instance(LzyContext);
        this.reader.init(this);
        this.objectType = this.enterClass("java.lang.Object");
        this.classType = this.enterClass("java.lang.Class");
        this.stringType = this.enterClass("java.lang.String");
        this.stringBufferType = this.enterClass("java.lang.StringBuffer");
        this.cloneableType = this.enterClass("java.lang.Cloneable");
        this.throwableType = this.enterClass("java.lang.Throwable");
        this.serializableType = this.enterClass("java.io.Serializable");
        this.errorType = this.enterClass("java.lang.Error");
        this.exceptionType = this.enterClass("java.lang.Exception");
        this.runtimeExceptionType = this.enterClass("java.lang.RuntimeException");
        this.classNotFoundExceptionType = this.enterClass("java.lang.ClassNotFoundException");
        this.noClassDefFoundErrorType = this.enterClass("java.lang.NoClassDefFoundError");
        this.assertionErrorType = this.enterClass("java.lang.AssertionError");
        this.classLoaderType = this.enterClass("java.lang.ClassLoader");
        LzyClassType var3 = (LzyClassType)this.arrayClass.type;
        var3.supertype_field = this.objectType;
        var3.interfaces_field = LzyList.of(this.cloneableType, this.serializableType);
        this.arrayClass.members_field = new LzyScope(this.arrayClass);
        this.lengthVar = new LzyVarSymbol(17L, this.names.length, this.intType, this.arrayClass);
        this.arrayClass.members().enter(this.lengthVar);
        LzyMethodSymbol var4 = new LzyMethodSymbol(1L, this.names.clone, new LzyMethodType(LzyType.emptyList, this.objectType, LzyType.emptyList, this.methodClass), this.arrayClass);
        this.arrayClass.members().enter(var4);
        this.nullConst = this.enterConstant("null", this.botType);
        this.trueConst = this.enterConstant("true", this.booleanType.constType(new Integer(1)));
        this.falseConst = this.enterConstant("false", this.booleanType.constType(new Integer(0)));
        this.enterUnop("+", this.intType, this.intType, 0);
        this.enterUnop("+", this.longType, this.longType, 0);
        this.enterUnop("+", this.floatType, this.floatType, 0);
        this.enterUnop("+", this.doubleType, this.doubleType, 0);
        this.enterUnop("-", this.intType, this.intType, 116);
        this.enterUnop("-", this.longType, this.longType, 117);
        this.enterUnop("-", this.floatType, this.floatType, 118);
        this.enterUnop("-", this.doubleType, this.doubleType, 119);
        this.enterUnop("~", this.intType, this.intType, 130);
        this.enterUnop("~", this.longType, this.longType, 131);
        this.enterUnop("++", this.byteType, this.byteType, 96);
        this.enterUnop("++", this.shortType, this.shortType, 96);
        this.enterUnop("++", this.charType, this.charType, 96);
        this.enterUnop("++", this.intType, this.intType, 96);
        this.enterUnop("++", this.longType, this.longType, 97);
        this.enterUnop("++", this.floatType, this.floatType, 98);
        this.enterUnop("++", this.doubleType, this.doubleType, 99);
        this.enterUnop("--", this.byteType, this.byteType, 100);
        this.enterUnop("--", this.shortType, this.shortType, 100);
        this.enterUnop("--", this.charType, this.charType, 100);
        this.enterUnop("--", this.intType, this.intType, 100);
        this.enterUnop("--", this.longType, this.longType, 101);
        this.enterUnop("--", this.floatType, this.floatType, 102);
        this.enterUnop("--", this.doubleType, this.doubleType, 103);
        this.enterUnop("!", this.booleanType, this.booleanType, 257);
        this.nullcheck = this.enterUnop("<*nullchk*>", this.objectType, this.objectType, 276);
        this.enterBinop("+", this.stringType, this.stringType, this.stringType, 256);
        this.enterBinop("+", this.stringType, this.intType, this.stringType, 256);
        this.enterBinop("+", this.stringType, this.longType, this.stringType, 256);
        this.enterBinop("+", this.stringType, this.floatType, this.stringType, 256);
        this.enterBinop("+", this.stringType, this.doubleType, this.stringType, 256);
        this.enterBinop("+", this.stringType, this.booleanType, this.stringType, 256);
        this.enterBinop("+", this.stringType, this.objectType, this.stringType, 256);
        this.enterBinop("+", this.stringType, this.botType, this.stringType, 256);
        this.enterBinop("+", this.intType, this.stringType, this.stringType, 256);
        this.enterBinop("+", this.longType, this.stringType, this.stringType, 256);
        this.enterBinop("+", this.floatType, this.stringType, this.stringType, 256);
        this.enterBinop("+", this.doubleType, this.stringType, this.stringType, 256);
        this.enterBinop("+", this.booleanType, this.stringType, this.stringType, 256);
        this.enterBinop("+", this.objectType, this.stringType, this.stringType, 256);
        this.enterBinop("+", this.botType, this.stringType, this.stringType, 256);
        this.enterBinop("+", this.intType, this.intType, this.intType, 96);
        this.enterBinop("+", this.longType, this.longType, this.longType, 97);
        this.enterBinop("+", this.floatType, this.floatType, this.floatType, 98);
        this.enterBinop("+", this.doubleType, this.doubleType, this.doubleType, 99);
        this.enterBinop("+", this.botType, this.botType, this.botType, 277);
        this.enterBinop("+", this.botType, this.intType, this.botType, 277);
        this.enterBinop("+", this.botType, this.longType, this.botType, 277);
        this.enterBinop("+", this.botType, this.floatType, this.botType, 277);
        this.enterBinop("+", this.botType, this.doubleType, this.botType, 277);
        this.enterBinop("+", this.botType, this.booleanType, this.botType, 277);
        this.enterBinop("+", this.botType, this.objectType, this.botType, 277);
        this.enterBinop("+", this.intType, this.botType, this.botType, 277);
        this.enterBinop("+", this.longType, this.botType, this.botType, 277);
        this.enterBinop("+", this.floatType, this.botType, this.botType, 277);
        this.enterBinop("+", this.doubleType, this.botType, this.botType, 277);
        this.enterBinop("+", this.booleanType, this.botType, this.botType, 277);
        this.enterBinop("+", this.objectType, this.botType, this.botType, 277);
        this.enterBinop("-", this.intType, this.intType, this.intType, 100);
        this.enterBinop("-", this.longType, this.longType, this.longType, 101);
        this.enterBinop("-", this.floatType, this.floatType, this.floatType, 102);
        this.enterBinop("-", this.doubleType, this.doubleType, this.doubleType, 103);
        this.enterBinop("*", this.intType, this.intType, this.intType, 104);
        this.enterBinop("*", this.longType, this.longType, this.longType, 105);
        this.enterBinop("*", this.floatType, this.floatType, this.floatType, 106);
        this.enterBinop("*", this.doubleType, this.doubleType, this.doubleType, 107);
        this.enterBinop("/", this.intType, this.intType, this.intType, 108);
        this.enterBinop("/", this.longType, this.longType, this.longType, 109);
        this.enterBinop("/", this.floatType, this.floatType, this.floatType, 110);
        this.enterBinop("/", this.doubleType, this.doubleType, this.doubleType, 111);
        this.enterBinop("%", this.intType, this.intType, this.intType, 112);
        this.enterBinop("%", this.longType, this.longType, this.longType, 113);
        this.enterBinop("%", this.floatType, this.floatType, this.floatType, 114);
        this.enterBinop("%", this.doubleType, this.doubleType, this.doubleType, 115);
        this.enterBinop("&", this.booleanType, this.booleanType, this.booleanType, 126);
        this.enterBinop("&", this.intType, this.intType, this.intType, 126);
        this.enterBinop("&", this.longType, this.longType, this.longType, 127);
        this.enterBinop("|", this.booleanType, this.booleanType, this.booleanType, 128);
        this.enterBinop("|", this.intType, this.intType, this.intType, 128);
        this.enterBinop("|", this.longType, this.longType, this.longType, 129);
        this.enterBinop("^", this.booleanType, this.booleanType, this.booleanType, 130);
        this.enterBinop("^", this.intType, this.intType, this.intType, 130);
        this.enterBinop("^", this.longType, this.longType, this.longType, 131);
        this.enterBinop("<<", this.intType, this.intType, this.intType, 120);
        this.enterBinop("<<", this.longType, this.intType, this.longType, 121);
        this.enterBinop("<<", this.intType, this.longType, this.intType, 270);
        this.enterBinop("<<", this.longType, this.longType, this.longType, 271);
        this.enterBinop(">>", this.intType, this.intType, this.intType, 122);
        this.enterBinop(">>", this.longType, this.intType, this.longType, 123);
        this.enterBinop(">>", this.intType, this.longType, this.intType, 272);
        this.enterBinop(">>", this.longType, this.longType, this.longType, 273);
        this.enterBinop(">>>", this.intType, this.intType, this.intType, 124);
        this.enterBinop(">>>", this.longType, this.intType, this.longType, 125);
        this.enterBinop(">>>", this.intType, this.longType, this.intType, 274);
        this.enterBinop(">>>", this.longType, this.longType, this.longType, 275);
        this.enterBinop("<", this.intType, this.intType, this.booleanType, 161);
        this.enterBinop("<", this.longType, this.longType, this.booleanType, 148, 155);
        this.enterBinop("<", this.floatType, this.floatType, this.booleanType, 150, 155);
        this.enterBinop("<", this.doubleType, this.doubleType, this.booleanType, 152, 155);
        this.enterBinop(">", this.intType, this.intType, this.booleanType, 163);
        this.enterBinop(">", this.longType, this.longType, this.booleanType, 148, 157);
        this.enterBinop(">", this.floatType, this.floatType, this.booleanType, 149, 157);
        this.enterBinop(">", this.doubleType, this.doubleType, this.booleanType, 151, 157);
        this.enterBinop("<=", this.intType, this.intType, this.booleanType, 164);
        this.enterBinop("<=", this.longType, this.longType, this.booleanType, 148, 158);
        this.enterBinop("<=", this.floatType, this.floatType, this.booleanType, 150, 158);
        this.enterBinop("<=", this.doubleType, this.doubleType, this.booleanType, 152, 158);
        this.enterBinop(">=", this.intType, this.intType, this.booleanType, 162);
        this.enterBinop(">=", this.longType, this.longType, this.booleanType, 148, 156);
        this.enterBinop(">=", this.floatType, this.floatType, this.booleanType, 149, 156);
        this.enterBinop(">=", this.doubleType, this.doubleType, this.booleanType, 151, 156);
        this.enterBinop("==", this.intType, this.intType, this.booleanType, 159);
        this.enterBinop("==", this.longType, this.longType, this.booleanType, 148, 153);
        this.enterBinop("==", this.floatType, this.floatType, this.booleanType, 149, 153);
        this.enterBinop("==", this.doubleType, this.doubleType, this.booleanType, 151, 153);
        this.enterBinop("==", this.booleanType, this.booleanType, this.booleanType, 159);
        this.enterBinop("==", this.objectType, this.objectType, this.booleanType, 165);
        this.enterBinop("!=", this.intType, this.intType, this.booleanType, 160);
        this.enterBinop("!=", this.longType, this.longType, this.booleanType, 148, 154);
        this.enterBinop("!=", this.floatType, this.floatType, this.booleanType, 149, 154);
        this.enterBinop("!=", this.doubleType, this.doubleType, this.booleanType, 151, 154);
        this.enterBinop("!=", this.booleanType, this.booleanType, this.booleanType, 160);
        this.enterBinop("!=", this.objectType, this.objectType, this.booleanType, 166);
        this.enterBinop("&&", this.booleanType, this.booleanType, this.booleanType, 258);
        this.enterBinop("||", this.booleanType, this.booleanType, this.booleanType, 259);
    }

    public void initType(LzyType basicType,LzyClassSymbol classSymbol){
        basicType.tsym = classSymbol;
        this.typeOfTag[basicType.tag] = basicType;
    }

    public void initType(LzyType type,String s){
        this.initType(type,new LzyClassSymbol(LzyFlags.PUBLIC,this.names.fromString(s),type,this.rootPackage));
    }


    public void initType(LzyType type,String s1,String s2){
        this.initType(type,s1);
        this.boxedName[type.tag] = this.names.fromString("java.lang"+s2);
    }

    private LzyMethodSymbol.OperatorSymbol enterUnop(String s1,LzyType t1,LzyType t2,int i){
        LzyMethodSymbol.OperatorSymbol operatorSymbol = new LzyMethodSymbol.OperatorSymbol(this.names.fromString(s1), new LzyMethodType(LzyList.of(t1), t2, LzyType.emptyList, this.methodClass), i, this.predefClass);
        this.predefClass.members().enter(operatorSymbol);
        return operatorSymbol;
    }

    private void enterBinop(String var1, LzyType var2, LzyType var3, LzyType var4, int var5) {
        this.predefClass.members().enter(new LzyMethodSymbol.OperatorSymbol(this.names.fromString(var1), new LzyMethodType(LzyList.of(var2, var3), var4, LzyType.emptyList, this.methodClass), var5, this.predefClass));
    }

    private void enterBinop(String var1, LzyType var2, LzyType var3, LzyType var4, int var5, int var6) {
        this.enterBinop(var1, var2, var3, var4, var5 << 9 | var6);
    }

    private LzyVarSymbol enterConstant(String var1, LzyType var2) {
        LzyVarSymbol var3 = new LzyVarSymbol(25L, this.names.fromString(var1), var2, this.predefClass);
        var3.constValue = var2.constValue;
        this.predefClass.members().enter(var3);
        return var3;
    }



    private LzyType enterClass(String var1) {
        return this.reader.enterClass(this.names.fromString(var1)).type;
    }



}

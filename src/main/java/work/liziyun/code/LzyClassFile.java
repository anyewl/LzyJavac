package work.liziyun.code;


import work.liziyun.code.type.LzyType;
import work.liziyun.world.LzyName;
import java.io.File;

public class LzyClassFile {
    public static final int JAVA_MAGIC = -889275714;
    public static final int CONSTANT_Utf8 = 1;
    public static final int CONSTANT_Unicode = 2;
    public static final int CONSTANT_Integer = 3;
    public static final int CONSTANT_Float = 4;
    public static final int CONSTANT_Long = 5;
    public static final int CONSTANT_Double = 6;
    public static final int CONSTANT_Class = 7;
    public static final int CONSTANT_String = 8;
    public static final int CONSTANT_Fieldref = 9;
    public static final int CONSTANT_Methodref = 10;
    public static final int CONSTANT_InterfaceMethodref = 11;
    public static final int CONSTANT_NameandType = 12;
    public static final int CONSTANT_MethodHandle = 15;
    public static final int CONSTANT_MethodType = 16;
    public static final int CONSTANT_InvokeDynamic = 18;
    public static final int MAX_PARAMETERS = 255;
    public static final int MAX_DIMENSIONS = 255;
    public static final int MAX_CODE = 65535;
    public static final int MAX_LOCALS = 65535;
    public static final int MAX_STACK = 65535;

    public LzyClassFile() {
    }

    public static byte[] internalize(byte[] var0, int var1, int var2) {
        byte[] var3 = new byte[var2];

        for(int var4 = 0; var4 < var2; ++var4) {
            byte var5 = var0[var1 + var4];
            if (var5 == 47) {
                var3[var4] = 46;
            } else {
                var3[var4] = var5;
            }
        }

        return var3;
    }

    public static byte[] internalize(LzyName var0) {
        return internalize(var0.getByteArray(), var0.getIndex(), var0.getByteLength());
    }

    public static byte[] externalize(byte[] var0, int var1, int var2) {
        byte[] var3 = new byte[var2];

        for(int var4 = 0; var4 < var2; ++var4) {
            byte var5 = var0[var1 + var4];
            if (var5 == 46) {
                var3[var4] = 47;
            } else {
                var3[var4] = var5;
            }
        }

        return var3;
    }

    public static byte[] externalize(LzyName var0) {
        return externalize(var0.getByteArray(), var0.getIndex(), var0.getByteLength());
    }

    public static String externalizeFileName(LzyName var0) {
        return var0.toString().replace('.', File.separatorChar);
    }
    public static class NameAndType {
        public LzyName name;
        public LzyType type;

        public NameAndType(LzyName var1, LzyType var2) {
            this.name = var1;
            this.type = var2;
        }

        public boolean equals(Object var1) {
            return var1 instanceof LzyClassFile.NameAndType && this.name == ((LzyClassFile.NameAndType)var1).name && this.type.equals(((LzyClassFile.NameAndType)var1).type);
        }

        public int hashCode() {
            return this.name.hashCode() * this.type.hashCode();
        }
    }

    public static enum Version {
        V45_3(45, 3),
        V49(49, 0),
        V50(50, 0),
        V51(51, 0);

        public final int major;
        public final int minor;

        private Version(int var3, int var4) {
            this.major = var3;
            this.minor = var4;
        }
    }
}

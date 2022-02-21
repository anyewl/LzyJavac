package work.liziyun.util;

public class LzyPosition {
    public static final int LINESHIFT = 10;
    public static final int COLUMNMASK = 1023;
    public static final int NOPOS = 0;
    public static final int FIRSTPOS = 1025;
    public static final int MAXPOS = 2147483647;

    public LzyPosition() {
    }


    public static int column(int var0) {
        return var0 & 1023;
    }

    public static int line(int var0) {
        return var0 >>> 10;
    }

    public static int make(int var0, int var1) {
        return (var0 << 10) + var1;
    }

}

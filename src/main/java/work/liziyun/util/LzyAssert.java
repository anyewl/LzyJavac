package work.liziyun.util;

public class LzyAssert {

    public static void check(boolean var0) {
        if (!var0) {
            error();
        }

    }

    public static void checkNull(Object var0) {
        if (var0 != null) {
            error();
        }

    }

    public static <T> T checkNonNull(T var0) {
        if (var0 == null) {
            error();
        }

        return var0;
    }

    public static void check(boolean var0, int var1) {
        if (!var0) {
            error(String.valueOf(var1));
        }

    }

    public static void check(boolean var0, long var1) {
        if (!var0) {
            error(String.valueOf(var1));
        }

    }

    public static void check(boolean var0, Object var1) {
        if (!var0) {
            error(String.valueOf(var1));
        }

    }

    public static void check(boolean var0, String var1) {
        if (!var0) {
            error(var1);
        }

    }

    public static void checkNull(Object var0, Object var1) {
        if (var0 != null) {
            error(String.valueOf(var1));
        }

    }

    public static void checkNull(Object var0, String var1) {
        if (var0 != null) {
            error(var1);
        }

    }

    public static <T> T checkNonNull(T var0, String var1) {
        if (var0 == null) {
            error(var1);
        }

        return var0;
    }

    public static void error() {
        throw new AssertionError();
    }

    public static void error(String var0) {
        throw new AssertionError(var0);
    }

    private LzyAssert() {
    }
}

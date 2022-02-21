package work.liziyun.util;




import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class LzyContext {

    private Map<LzyContext.Key<?>, Object> ht = new HashMap();
    private Map<LzyContext.Key<?>, LzyContext.Factory<?>> ft = new HashMap();
    private Map<Class<?>, LzyContext.Key<?>> kt = new HashMap();

    public <T> void put(LzyContext.Key<T> var1, LzyContext.Factory<T> var2) {
        checkState(this.ht);
        Object var3 = this.ht.put(var1, var2);
        if (var3 != null) {
            throw new AssertionError("duplicate LzyContext value");
        } else {
            checkState(this.ft);
            this.ft.put(var1, var2);
        }
    }

    public <T> void put(LzyContext.Key<T> var1, T var2) {
        if (var2 instanceof LzyContext.Factory) {
            throw new AssertionError("T extends LzyContext.Factory");
        } else {
            checkState(this.ht);
            Object var3 = this.ht.put(var1, var2);
            if (var3 != null && !(var3 instanceof LzyContext.Factory) && var3 != var2 && var2 != null) {
                throw new AssertionError("duplicate LzyContext value");
            }
        }
    }

    public <T> T get(LzyContext.Key<T> var1) {
        checkState(this.ht);
        Object var2 = this.ht.get(var1);
        if (var2 instanceof LzyContext.Factory) {
            LzyContext.Factory var3 = (LzyContext.Factory)var2;
            var2 = var3.make(this);
            if (var2 instanceof LzyContext.Factory) {
                throw new AssertionError("T extends LzyContext.Factory");
            }

            LzyAssert.check(this.ht.get(var1) == var2);
        }

        return (T) uncheckedCast(var2);
    }

    public LzyContext() {
    }

    public LzyContext(LzyContext var1) {
        this.kt.putAll(var1.kt);
        this.ft.putAll(var1.ft);
        this.ht.putAll(var1.ft);
    }

    private <T> LzyContext.Key<T> key(Class<T> var1) {
        checkState(this.kt);
        LzyContext.Key var2 = (LzyContext.Key)uncheckedCast(this.kt.get(var1));
        if (var2 == null) {
            var2 = new LzyContext.Key();
            this.kt.put(var1, var2);
        }

        return var2;
    }

    public <T> T get(Class<T> var1) {
        return this.get(this.key(var1));
    }

    public <T> void put(Class<T> var1, T var2) {
        this.put(this.key(var1), var2);
    }

    public <T> void put(Class<T> var1, LzyContext.Factory<T> var2) {
        this.put(this.key(var1), var2);
    }

    private static <T> T uncheckedCast(T var0) {
        return var0;
    }

    public void dump() {
        Iterator var1 = this.ht.values().iterator();

        while(var1.hasNext()) {
            Object var2 = var1.next();
            System.err.println(var2 == null ? null : var2.getClass());
        }

    }

    public void clear() {
        this.ht = null;
        this.kt = null;
        this.ft = null;
    }

    private static void checkState(Map<?, ?> var0) {
        if (var0 == null) {
            throw new IllegalStateException();
        }
    }

    public interface Factory<T> {
        T make(LzyContext var1);
    }

    public static class Key<T> {
        public Key() {
        }
    }
}

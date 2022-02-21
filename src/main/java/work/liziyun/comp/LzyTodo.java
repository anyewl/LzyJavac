package work.liziyun.comp;


import work.liziyun.util.LzyContext;
import work.liziyun.util.LzyListBuffer;

public class LzyTodo extends LzyListBuffer {
    private static final LzyContext.Key todoKey = new LzyContext.Key();

    private LzyTodo(LzyContext var1) {
        var1.put(todoKey, this);
    }

    public Object first() {
        return super.first();
    }

    public Object next() {
        return super.next();
    }

    public boolean contains(Object var1) {
        return super.contains((LzyEnv)var1);
    }

    public static LzyTodo instance(LzyContext var0) {
        LzyTodo var1 = (LzyTodo)var0.get(todoKey);
        if (var1 == null) {
            var1 = new LzyTodo(var0);
        }

        return var1;
    }

    public LzyListBuffer append(Object var1) {
        return super.append((LzyEnv)var1);
    }

    public LzyListBuffer prepend(Object var1) {
        return super.prepend((LzyEnv)var1);
    }

    public LzyListBuffer appendArray(Object[] var1) {
        return super.appendArray((LzyEnv[])var1);
    }

    public Object[] toArray(Object[] var1) {
        return super.toArray((LzyEnv[])var1);
    }
}

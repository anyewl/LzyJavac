package work.liziyun.jvm;


import work.liziyun.util.LzyContext;

import java.util.HashMap;
import java.util.Map;

public enum  LzyTarget {
    JDK1_1("1.1", 45, 3),
    JDK1_2("1.2", 46, 0),
    JDK1_3("1.3", 47, 0),
    JDK1_4("1.4", 48, 0),
    JSR14("jsr14", 48, 0),
    JDK1_4_1("1.4.1", 48, 0),
    JDK1_4_2("1.4.2", 48, 0);


    private static final LzyContext.Key<LzyTarget> targetKey = new LzyContext.Key();
    private static LzyTarget MIN;
    private static LzyTarget MAX;
    private static Map<String,LzyTarget> tab = new HashMap();
    public final String name;
    public final int majorVersion;

    public final int minorVersion;

    public static final LzyTarget DEFAULT;

    static {
        LzyTarget[] values = values();
        // 最小值和最大值: jdk1.7
        for (LzyTarget value : values) {
            if (MIN == null){
                MIN = value;
            }
            MAX = value;
            tab.put(value.name,value);
        }
        // 默认值: jdk1.4
        DEFAULT = JDK1_4;
    }

    LzyTarget(String name, int majorVersion, int minorVersion) {
        this.name = name;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }




    public static LzyTarget instance(LzyContext LzyContext){
        LzyTarget lzyTarget = LzyContext.get(targetKey);
        if (lzyTarget == null){
            // 省略: 尝试获取指定的编译版本
            lzyTarget = DEFAULT;
            LzyContext.put(targetKey,lzyTarget);
        }
        return lzyTarget;
    }

    public static LzyTarget MAX() {
        return MAX;
    }

    public static LzyTarget MIN() {
        return MIN;
    }
}

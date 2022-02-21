package work.liziyun.util;


public class LzyHashtable {
    private int hashSize;
    private int hashMask;
    private int limit;
    private int size;
    private LzyHashtable.Entry[] table;

    public int size(){
        return this.size;
    }


    public LzyHashtable(){
        this(32);
    }

    public LzyHashtable(int maxSize){
        this(maxSize,0.75f);
    }


    /**
     *
     * @param maxSize 大小的参考值
     * @param rate 增长因子
     */
    public LzyHashtable(int maxSize,float rate){
        int capacity = 1;
        while (size < maxSize){
            // 2倍增长
            capacity <<= 1;
        }
        this.hashSize = capacity;
        this.hashMask = capacity - 1;
        // 阀值
        this.limit = (int)(size * rate);
        // 实际大小
        this.size = 0;
        // 初始化
        this.table = new LzyHashtable.Entry[capacity];
    }



    // 工厂方法
    public static LzyHashtable make(){
        return new LzyHashtable();
    }
    // 扩容
    private void dble(){
        // 大小增长一倍
        this.hashSize <<= 1;
        // 掩码
        this.hashMask = this.hashSize - 1;
        // 阀值: 增长一倍
        this.limit <<= 1;
        // 旧数据
        LzyHashtable.Entry[] oldTables = this.table;
        // 新
        this.table = new LzyHashtable.Entry[this.hashSize];
        // 复制数据
        for (Entry oldEntry : oldTables) {
            // 冲突链
            LzyHashtable.Entry entry = null;
            while (oldEntry != null){
                entry = oldEntry.next;
                // hash
                int hash = oldEntry.hash & this.hashMask;
                // 指向旧链
                oldEntry.next = this.table[hash];
                // 放入第一个位置
                this.table[hash] = oldEntry;
                // 下一个
                oldEntry = entry;
            }
        }
    }
    
    
    // 置空
    public void reset(){
        for (int i = 0; i < this.table.length; i++) {
            this.table[i] = null;
        }
        this.size = 0;
    }
/*
    public List keys(){
        
    }
    
    public Object get(Object key){
        
    }
    
    public Object remove(Object key){
        
    }*/

    // 元素
    private static class Entry{
        Object key;
        Object value;
        int hash;
        LzyHashtable.Entry next;

        public Entry(Object key, Object value, int hash, Entry next) {
            this.key = key;
            this.value = value;
            this.hash = hash;
            this.next = next;
        }
    }
}

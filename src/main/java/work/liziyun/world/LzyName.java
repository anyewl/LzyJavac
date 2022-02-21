package work.liziyun.world;

public class LzyName {
    public LzyTable lzyTable;
    public int index;
    public int length;
    LzyName next;

    public LzyName(LzyTable lzyTable, int index, int length) {
        this.lzyTable = lzyTable;
        this.index = index;
        this.length = length;
    }


    public int getIndex(){
        return index;
    }
    public int getByteLength(){
        return length;
    }
    public int hashCode(){
        return this.index;
    }
    public String toString(){
        return lzyTable.getString(index,length);
    }

    // 追加Name
    public LzyName append(char c,LzyName name){
        // 新词的大小
        byte[] newBytes = new byte[this.length + name.length + 1];
        // 将旧数据 拷贝到 新数据中
        this.getBytes(newBytes,0);
        // 新数组添加一个字符
        newBytes[this.length] = (byte) c;
        // 将name添加到新数据中
        name.getBytes(newBytes,this.length+1);
        // 不转码的方式创建
        return lzyTable.fromUtf(newBytes,0,newBytes.length);
    }
    // 追加Name: 两个Name相加
    public LzyName append(LzyName name){
        byte[] newBytes = new byte[this.length + name.length];
        // 拷贝数据到newBytes中: 目标0开始
        this.getBytes(newBytes,0);
        // 拷贝数据: 目标length开w
        name.getBytes(newBytes,this.length);
        return lzyTable.fromUtf(newBytes,0,newBytes.length);
    }

    public void getBytes(byte[] bytes,int start){
        System.arraycopy(this.lzyTable.bytes,this.index,bytes,start,this.length);
    }

    public byte[] getByteArray(){
        return this.lzyTable.bytes;
    }


    /**
     * 如果byte在Name存在，那么返回值一定大于等于0。
     * 如果bye不在Name中存在,那么返回值一定等于-1
     * @param b
     * @return
     */
    public int lastIndexOf(byte b){
        // Table中byte数组
        byte[] byteArray = this.getByteArray();
        // Name的开始下标
        int startIndex = this.index;
        // Name的长度-1
        int length  = this.length-1;
        // 在Name中查找指定的byte
        while (length >= 0 && byteArray[startIndex+length] != b){
            length--;
        }
        return length;
    }


    /**
     * Name截取
     * @param start 相对于Name的开始位置
     * @param end 相对于Name的结束位置
     * @return
     */
    public LzyName subName(int start,int end){
        // 我们要更大的int
        if (end < start){
            end = start;
        }
        // 参数二: 绝对位置  参数三: 长度
        return this.lzyTable.fromUtf(this.getByteArray(),this.index+start,end-start);
    }

    public byte[] toUtf() {
        byte[] newBytes = new byte[this.length];
        System.arraycopy(this.lzyTable.bytes, this.index, newBytes, 0, this.length);
        return newBytes;
    }

}


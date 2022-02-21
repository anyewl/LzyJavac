package work.liziyun.util;



import work.liziyun.world.LzyName;

public class LzyConvert {

    public static int string2int(String data,int raidx){
        if ( raidx == 10 ){
            return Integer.parseInt(data,raidx);
        }else{
            System.out.println("编译错误: 只能处理10进制字面量!");
            return -1;
        }
    }

    public static long string2long(String data,int radix){
        if ( radix == 10 ){
            return Long.parseLong(data,radix);
        }else{
            System.out.println("编译错误: 只能处理10进制字面量!");
            return -1;
        }
    }


    public static int chars2utf(char[] src,int sIndex,byte[] dst,int dIndex,int len){
        int j = dIndex;
        int sEnd = sIndex + len;
        for ( int i = sIndex ;  i < sEnd ; i++ ){
            // 1100000 + 后5位
            char c = src[i];

            if ( c >= 1 && c <= 127){
                dst[j++] = (byte) c;
            }else{
                System.out.println("编译错误: 暂不支持复杂的字符编码");
            }
        }
        return j;
    }


    /**
     * 截取$符号前的Name
     * @param name
     * @return
     */
    public static LzyList<LzyName> enclosingCandidates(LzyName name){
        LzyList list = LzyList.nil();
        int pos;
        // 在Name中查找'$'的位置.
        // 注意: 如果$的位置是第一个，那么不处理。因为我们要截取$符号前面的内容
        while ( ( pos = name.lastIndexOf((byte)'$') )  > 0){
            name = name.subName(0,pos);
            list  = list.prepend(name);
        }
        return list;
    }

    /**
     * 截取简单类名: 去掉前面的包名
     * @param name
     * @return
     */
    public static LzyName shortName(LzyName name){
        return name.subName(name.lastIndexOf((byte)'.')+1,name.getByteLength());
    }


    public static LzyName packagePart(LzyName name){
        return name.subName(0,name.lastIndexOf((byte)'.'));
    }

}

package work.liziyun.code.type;

/**
 * Type的标识符
 *
 * 参考标准: jdk1.4
 */
public interface LzyTypeTags {
    // 基本数据类型
    int BYTE = 1;
    int CHAR = 2;
    int SHORT = 3;
    int INT = 4;
    int LONG = 5;
    int FLOAT = 6;
    int DOUBLE = 7;
    int BOOLEAN = 8;
    // 引用数据类型
    int VOID = 9;  // 无返回值
    int CLASS = 10; // 1. 类 2.接口
    int ARRAY = 11; // 数组
    int METHOD = 12; // 方法
    int PACKAGE = 13; // 包
    int BOT = 16; // null空指针类型
    int NONE = 17; // 无类型
    int ERROR = 18; // 错误类型
    int UNKNOWN = 19; // 没有找到
    int UNDETVAR = 20; // 实例化类型变量的标记
    int TypeTagCount = 21; // 数字类型
    int lastBaseTag = 8; // 基本数据类型最大值标记
    int firstPartialTag = 18; // 基本数据类型最小标记
}

# LzyJavac
JavaCompile 仿制的Java前端编译器 基础正版javac的精简版本，添加大量注释说明

# LzyJavac和正版Javac有什么区别？
  LzyJavac是从正版Javac大量抄袭修改而来的，为了帮助大家方便学习理解。
  删减大量复杂非必要的过程，语法检查 和 插件化注解 和 解语法糖并没有实现！
  但是这不影响，我们对Java前端编译器的学习和理解！
  这份源码中，写了大量注释。参考多个jdk版本，选择的算法是简单为主！而不是最好的，因为更容易帮助大家理解！

# 两个路径
  jdk类库路径：存放jdk自带的类
    String List 等等
  编译路径：存放Java源代码
    将对这个位置的源代码进行编译处理
# 不支持的功能
  1. 内部类
  2. 枚举
  3. 注解
  4. 泛型
  5. Lambda
  6. switch
  为什么不支持1-5？
    因为要实现他们，过于繁琐难度很大，也不利于大家初步的学习
  为什么不支持6？
    因为懒，大家可以参考我关于switch字节码的分析去实现！
# switch字节码的原理分析
  https://www.bilibili.com/video/BV1qv411g7tj/

# 作者信息
  李滋芸 鄂州职业大学
  
  微信：liziyun_2000
  
  公众号：小豆的奇思妙想
  
  B站up讲课：李滋芸
# 使用教程
  https://www.bilibili.com/video/BV1La411C7Xe
  
  

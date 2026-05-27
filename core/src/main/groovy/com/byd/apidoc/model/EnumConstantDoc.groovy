package com.byd.apidoc.model

/**
 * 枚举常量文档模型
 * @author qiao.zhi2
 */
class EnumConstantDoc {

    /** 枚举常量名 */
    String name

    /** 枚举常量描述 */
    String description

    /** 核心业务值 */
    String value;

    /** 枚举常量的构造函数参数 */
    List<String> constructorParams

    /** 枚举常量上的注解 */
    List<String> annotations
}
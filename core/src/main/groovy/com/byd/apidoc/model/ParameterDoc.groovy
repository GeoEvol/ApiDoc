package com.byd.apidoc.model

/**
 * 参数文档模型
 * 用于存储方法参数的详细信息
 * @author qiao.zhi2
 */
class ParameterDoc {

    /** 参数名 */
    String name

    /** 参数类型 */
    String type

    /** 参数描述 */
    String description

    /** 参数上的注解 */
    List<String> annotations
}
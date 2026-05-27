package com.byd.apidoc.utils

/**
 * 字符串处理工具类
 * @author qiao.zhi2
 */
class StringUtils {

    private StringUtils() {
        // 工具类不允许实例化
    }

    /**
     * 将字符串的首字母转换为小写
     * 用于将驼峰命名的字符串首字母转为小写
     *
     * @param str 输入字符串
     * @return 首字母小写的字符串
     */
    static String firstToLowerCase(String str) {
        if (str == null || str.isEmpty()) {
            return str
        }
        if (str.length() == 1) {
            return str.toLowerCase()
        }
        return str[0].toLowerCase() + str.substring(1)
    }

    /**
     * 获取类的简单名称（不含包名）
     * 从完整类名中提取出类的简单名称
     *
     * @param fullyQualifiedName 完整类名
     * @return 类的简单名称
     */
    static String getClassSimpleName(String fullyQualifiedName) {
        if (fullyQualifiedName == null) {
            return null
        }
        int lastDotIndex = fullyQualifiedName.lastIndexOf('.')
        if (lastDotIndex >= 0 && lastDotIndex < fullyQualifiedName.length() - 1) {
            return fullyQualifiedName.substring(lastDotIndex + 1)
        }
        return fullyQualifiedName
    }

    /**
     * 判断字符串是否为有效类名
     * 验证字符串是否包含点号且不以点号开头或结尾
     *
     * @param className 类名字符串
     * @return 如果是有效类名返回true，否则返回false
     */
    static boolean isClassName(String className) {
        if (className == null || className.isEmpty()) {
            return false
        }
        return className.contains(".") && !className.startsWith(".") && !className.endsWith(".")
    }
}
package com.byd.apidoc.model

/**
 * API配置
 * 包含输出格式和文件扩展名常量
 * @author qiao.zhi2
 */
class ApiConfig {
    // 输出格式常量
    static final String FORMAT_MARKDOWN = "markdown"
    static final String FORMAT_HTML = "html"

    // 文件扩展名常量
    static final String EXTENSION_MARKDOWN = ".md"
    static final String EXTENSION_HTML = ".html"

    // 模板路径常量
    static final String TEMPLATE_ROOT_MARKDOWN = "/markdown"
    static final String TEMPLATE_ROOT_HTML = "/html"
    static final String TEMPLATE_INDEX = "index.ftl"
    static final String TEMPLATE_CLASS = "class.ftl"

    String projectName = "API Documentation"

    /**
     * 输出格式（如：markdown、html）。
     */
    String outputFormat = FORMAT_MARKDOWN

    // 类型过滤开关
    boolean includeInterfaces = true
    boolean includeClasses = true
    boolean includeEnums = true
    boolean includeAnnotations = true
    boolean includeExceptions = true
    boolean includeInnerClasses = false
    boolean includeInheritedMembers = true
    boolean includeHidden = false
    boolean includeRemoved = false

    /**
     * Optional Java source version for javadoc parsing. When empty, the parser
     * uses the current runtime JDK major version.
     */
    String sourceVersion = ""

    /**
     * All dependencies needed to parse source signatures and comments, such as
     * android.jar and project/library jars.
     */
    List<String> dependencyClasspath = []

    // 是否按@tag分组
    boolean groupByTag = false

    // 只包含指定标签
    Set<String> includeTags = []

    // 排除这些包下的类
    Set<String> excludePackages = []

    private TemplateConfig templateConfig = new TemplateConfig();

    TemplateConfig getTemplateConfig() { return templateConfig; }

    void setTemplateConfig(TemplateConfig templateConfig) { this.templateConfig = templateConfig; }
}

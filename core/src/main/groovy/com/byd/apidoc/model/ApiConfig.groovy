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
    boolean externalLinksEnabled = false

    /**
     * HTML 静态站点的部署根路径，例如 /api-docs-html/。
     *
     * 仅在 stableAssetLinks=true 时参与 CSS/JS/search-index 路径生成。
     * 默认留空，保持原有相对路径输出，兼容本地离线浏览。
     */
    String siteBasePath = ""

    /**
     * HTML 静态资源版本号。非空时输出 apidoc-${assetVersion}.css、
     * apidoc-${assetVersion}.js、search-${assetVersion}.js。
     */
    String assetVersion = ""

    /**
     * 是否对 CSS/JS/search-index 使用稳定站点根路径。
     * false 时继续使用原有 ${prefix}assets/... 相对路径。
     */
    boolean stableAssetLinks = false

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

    /**
     * Root directory of the Gradle root project. Used to resolve relative generated
     * javadoc stub paths before invoking the JDK DocumentationTool.
     */
    String projectRootDir = ""

    /**
     * Whether ApiDoc should generate temporary javadoc stubs for known missing
     * compile-time dependencies. Generated stubs are added to javadoc -sourcepath
     * only and are not collected as documentation source files.
     */
    boolean generatedStubsEnabled = false

    /**
     * Directory where temporary javadoc stub sources are generated.
     */
    String generatedStubDir = "build/apidoc/generated-stubs"

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

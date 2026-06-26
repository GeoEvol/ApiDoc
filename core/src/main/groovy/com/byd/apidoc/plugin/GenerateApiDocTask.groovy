package com.byd.apidoc.plugin

import com.byd.apidoc.generator.DocGenerator
import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.TemplateConfig
import com.byd.apidoc.utils.PathUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault

/**
 * 生成API文档任务
 *
 * 启用增量构建和构建缓存，当源文件或模板没有变化时，任务会被跳过。
 */
@DisableCachingByDefault(because = "Not worth caching as inputs are usually local")
@CacheableTask
abstract class GenerateApiDocTask extends DefaultTask {

    @Input
    abstract ListProperty<String> getSourcePaths()

    /**
     * 源码目录 - 自动从 sourcePaths 派生
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    ConfigurableFileCollection getSourceDirs() {
        if (_sourceDirs == null) {
            _sourceDirs = project.files(sourcePaths.map { paths ->
                List<File> dirs = []
                paths.each { path ->
                    File dir = new File(path).isAbsolute() ? new File(path) : project.rootProject.file(path)
                    if (dir.exists() && dir.isDirectory()) {
                        dirs.add(dir)
                    }
                }
                dirs
            })
        }
        return _sourceDirs
    }

    private ConfigurableFileCollection _sourceDirs

    @OutputDirectory
    abstract DirectoryProperty getOutputDir()

    @Input
    abstract Property<String> getProjectName()
    @Input
    abstract Property<String> getOutputFormat()
    @Input
    abstract Property<Boolean> getIncludeInterfaces()
    @Input
    abstract Property<Boolean> getIncludeClasses()
    @Input
    abstract Property<Boolean> getIncludeEnums()
    @Input
    abstract Property<Boolean> getIncludeAnnotations()
    @Input
    abstract Property<Boolean> getIncludeExceptions()
    @Input
    abstract Property<Boolean> getIncludeInnerClasses()
    @Input
    abstract Property<Boolean> getIncludeInheritedMembers()
    @Input
    abstract Property<Boolean> getIncludeHidden()
    @Input
    abstract Property<Boolean> getIncludeRemoved()
    @Input
    abstract Property<Boolean> getExternalLinksEnabled()
    @Input
    @Optional
    abstract Property<String> getSourceVersion()
    @Input
    @Optional
    abstract Property<String> getSiteBasePath()
    @Input
    @Optional
    abstract Property<String> getAssetVersion()
    @Input
    abstract Property<Boolean> getStableAssetLinks()
    @Classpath
    abstract ConfigurableFileCollection getDependencyClasspath()
    @Input
    abstract Property<Boolean> getGeneratedStubsEnabled()
    @Input
    @Optional
    abstract Property<String> getGeneratedStubDir()
    @Input
    abstract Property<Boolean> getGroupByTag()
    @Input
    abstract ListProperty<String> getIncludeTags()
    @Input
    abstract ListProperty<String> getExcludePackages()
    @Nested
    abstract Property<TemplateConfig> getTemplateConfig()

    @TaskAction
    void generate() {
        List<String> sourcePaths = sourcePaths.get()
        File outputDirFile = outputDir.get().asFile
        String projectNameValue = projectName.get()

        // 验证并规范化源码路径
        List<String> validSourcePaths = validateAndResolvePaths(sourcePaths)

        if (validSourcePaths.isEmpty()) {
            logger.lifecycle("没有找到有效的源码路径，跳过API文档生成。")
            return
        }

        ApiConfig config = buildConfig()

        logger.lifecycle("========================================")
        logger.lifecycle("开始生成API文档...")
        logger.lifecycle("项目名称: ${projectNameValue}")
        logger.lifecycle("发现 ${validSourcePaths.size()} 个源码路径:")
        validSourcePaths.each { path ->
            logger.lifecycle("  - ${path}")
        }
        logger.lifecycle("输出路径: ${outputDirFile.absolutePath}")
        if (!config.includeTags.isEmpty()) {
            logger.lifecycle("包含标签: ${config.includeTags}")
        }
        if (!config.excludePackages.isEmpty()) {
            logger.lifecycle("排除包: ${config.excludePackages}")
        }
        if (config.generatedStubsEnabled) {
            logger.lifecycle("启用Javadoc解析Stub: ${config.generatedStubDir}")
        }
        logger.lifecycle("========================================")

        try {
            DocGenerator generator = new DocGenerator()
            generator.generateApiDocs(validSourcePaths, projectNameValue, outputDirFile, config)
            logger.lifecycle("API文档生成成功! 位置: ${outputDirFile}")
        } catch (Exception e) {
            logger.error("API文档生成失败", e)
            throw new RuntimeException("API文档生成失败", e)
        }
    }

    /**
     * 验证并解析源码路径
     */
    private List<String> validateAndResolvePaths(List<String> paths) {
        return PathUtils.resolvePaths(paths, project.rootProject.projectDir)
    }

    /**
     * 构建配置对象
     */
    private ApiConfig buildConfig() {
        ApiConfig config = new ApiConfig()
        config.projectName = projectName.get()
        config.outputFormat = outputFormat.get()
        config.groupByTag = groupByTag.get()
        config.includeTags = new LinkedHashSet<>(includeTags.get())
        config.excludePackages = new LinkedHashSet<>(excludePackages.get())
        config.templateConfig = templateConfig.orNull ?: new TemplateConfig()
        config.includeInterfaces = includeInterfaces.get()
        config.includeClasses = includeClasses.get()
        config.includeEnums = includeEnums.get()
        config.includeAnnotations = includeAnnotations.get()
        config.includeExceptions = includeExceptions.get()
        config.includeInnerClasses = includeInnerClasses.get()
        config.includeInheritedMembers = includeInheritedMembers.get()
        config.includeHidden = includeHidden.get()
        config.includeRemoved = includeRemoved.get()
        config.externalLinksEnabled = externalLinksEnabled.get()
        config.sourceVersion = sourceVersion.orNull ?: ""
        config.siteBasePath = siteBasePath.orNull ?: ""
        config.assetVersion = assetVersion.orNull ?: ""
        config.stableAssetLinks = stableAssetLinks.getOrElse(false)
        config.dependencyClasspath = dependencyClasspath.files.collect { it.absolutePath }
        config.projectRootDir = project.rootProject.projectDir.absolutePath
        config.generatedStubsEnabled = generatedStubsEnabled.getOrElse(false)
        config.generatedStubDir = generatedStubDir.orNull ?: "build/apidoc/generated-stubs"
        return config
    }
}

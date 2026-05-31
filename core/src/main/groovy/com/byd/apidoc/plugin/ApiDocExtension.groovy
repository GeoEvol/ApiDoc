package com.byd.apidoc.plugin

import com.byd.apidoc.model.TemplateConfig
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

import javax.inject.Inject

/**
 * ApiDoc生成任务所需参数
 * @author qiao.zhi2
 */
abstract class ApiDocExtension {

    // 默认路径常量
    private static final String DEFAULT_SOURCE_PATH = "src/main/java"
    private static final String DEFAULT_MARKDOWN_OUTPUT_DIR = "build/api-docs"
    private static final String DEFAULT_HTML_OUTPUT_DIR = "build/api-docs-html"
    private static final String EMPTY_STRING = ""

    // --- 支持单个源码路径 ---
    abstract Property<String> getSourceDir()

    // --- 支持多个源码路径 ---
    abstract ListProperty<String> getSourcePaths()

    // --- 自动发现子模块 ---
    abstract Property<Boolean> getAutoDiscoverSubprojects()

    // --- 排除子模块 ---
    abstract SetProperty<String> getExcludeSubprojects()

    // --- 子模块源码相对路径 ---
    abstract Property<String> getSubprojectSourceRelativePath()

    // --- 输出目录列表：第一个是 Markdown，第二个是 HTML ---
    abstract ListProperty<String> getOutputDir()
    abstract Property<String> getProjectName()
    abstract Property<Boolean> getIncludeInterfaces()
    abstract Property<Boolean> getIncludeClasses()
    abstract Property<Boolean> getIncludeEnums()
    abstract Property<Boolean> getIncludeAnnotations()
    abstract Property<Boolean> getIncludeExceptions()
    abstract Property<Boolean> getIncludeInnerClasses()
    abstract Property<Boolean> getIncludeInheritedMembers()
    abstract Property<Boolean> getIncludeHidden()
    abstract Property<Boolean> getIncludeRemoved()
    abstract Property<Boolean> getExternalLinksEnabled()
    abstract Property<String> getSourceVersion()
    abstract ConfigurableFileCollection getDependencyClasspath()
    abstract Property<Boolean> getGroupByTag()
    abstract ListProperty<String> getIncludeTags()
    abstract ListProperty<String> getExcludePackages()
    abstract Property<TemplateConfig> getTemplateConfig()

    @Inject
    ApiDocExtension(ObjectFactory objects) {
        // 默认值
        getSourceDir().convention(DEFAULT_SOURCE_PATH)
        getSourcePaths().convention(objects.listProperty(String.class))
        getAutoDiscoverSubprojects().convention(true)
        getExcludeSubprojects().convention(objects.setProperty(String.class))
        getSubprojectSourceRelativePath().convention(DEFAULT_SOURCE_PATH)
        // 默认输出目录列表：[markdown路径, html路径]
        getOutputDir().convention(objects.listProperty(String.class).value([DEFAULT_MARKDOWN_OUTPUT_DIR, DEFAULT_HTML_OUTPUT_DIR]))
        getIncludeInterfaces().convention(true)
        getIncludeClasses().convention(true)
        getIncludeEnums().convention(true)
        getIncludeAnnotations().convention(true)
        getIncludeExceptions().convention(true)
        getIncludeInnerClasses().convention(false)
        getIncludeInheritedMembers().convention(true)
        getIncludeHidden().convention(false)
        getIncludeRemoved().convention(false)
        getExternalLinksEnabled().convention(false)
        getSourceVersion().convention(EMPTY_STRING)
        getProjectName().convention(EMPTY_STRING)
        getGroupByTag().convention(false)
        getIncludeTags().convention(objects.listProperty(String.class))
        getExcludePackages().convention(objects.listProperty(String.class))
        getTemplateConfig().convention(objects.newInstance(TemplateConfig.class))
    }
}

package com.byd.apidoc.plugin

import com.byd.apidoc.utils.PathUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

/**
 * API 文档生成插件
 */
class ApiDocGeneratorPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def extension = project.extensions.create('apiDoc', ApiDocExtension)

        def generateMarkdownProvider = createGenerateMarkdownTask(project, extension)
        def generateHtmlProvider = createGenerateHtmlTask(project, extension)
    }

    private static TaskProvider<GenerateApiDocTask> createGenerateMarkdownTask(Project project, ApiDocExtension extension) {
        return project.tasks.register('generateMarkdown', GenerateApiDocTask) { task ->
            task.description = "Generate Java API documentation in Markdown format"
            task.group = "documentation"

            task.projectName = extension.projectName.getOrElse(project.getName())
            // 使用 outputDir 列表的第一个元素作为 Markdown 输出路径
            List<String> outputDirs = extension.outputDir.get()
            String markdownOutputPath = outputDirs.size() > 0 ? outputDirs.get(0) : "build/api-docs"
            task.outputDir = project.layout.projectDirectory.dir(markdownOutputPath)
            task.outputFormat.set("markdown")
            task.includeInterfaces = extension.includeInterfaces
            task.includeClasses = extension.includeClasses
            task.includeEnums = extension.includeEnums
            task.includeAnnotations = extension.includeAnnotations
            task.includeExceptions = extension.includeExceptions
            task.includeInnerClasses = extension.includeInnerClasses
            task.includeInheritedMembers = extension.includeInheritedMembers
            task.includeHidden = extension.includeHidden
            task.includeRemoved = extension.includeRemoved
            task.sourceVersion = extension.sourceVersion
            task.dependencyClasspath.from(extension.dependencyClasspath)

            task.groupByTag = extension.groupByTag
            task.includeTags = extension.includeTags
            task.excludePackages = extension.excludePackages
            task.templateConfig = extension.templateConfig

            // 解析源码路径
            List<String> resolvedSourcePaths = resolveSourcePaths(project, extension)
            task.sourcePaths.set(resolvedSourcePaths)
            task.projectName.set(extension.getProjectName().getOrElse(project.getName()))

            // 仅在项目是 Java/Groovy/Kotlin 项目时添加编译依赖
            if (project.plugins.findPlugin('java') || project.plugins.findPlugin('groovy') || project.plugins.findPlugin('org.jetbrains.kotlin.jvm')) {
                def compileJavaTask = project.tasks.findByName("compileJava")
                if (compileJavaTask != null) {
                    task.dependsOn(compileJavaTask)
                }
            }
        }
    }

    private static TaskProvider<GenerateApiDocTask> createGenerateHtmlTask(Project project, ApiDocExtension extension) {
        return project.tasks.register('generateHtml', GenerateApiDocTask) { task ->
            task.description = "Generate Java API documentation in HTML format"
            task.group = "documentation"

            task.projectName = extension.projectName.getOrElse(project.getName())
            // 使用 outputDir 列表的第二个元素作为 HTML 输出路径
            List<String> outputDirs = extension.outputDir.get()
            String htmlOutputPath = outputDirs.size() > 1 ? outputDirs.get(1) : "build/api-docs-html"
            task.outputDir = project.layout.projectDirectory.dir(htmlOutputPath)
            task.outputFormat.set("html")
            task.includeInterfaces = extension.includeInterfaces
            task.includeClasses = extension.includeClasses
            task.includeEnums = extension.includeEnums
            task.includeAnnotations = extension.includeAnnotations
            task.includeExceptions = extension.includeExceptions
            task.includeInnerClasses = extension.includeInnerClasses
            task.includeInheritedMembers = extension.includeInheritedMembers
            task.includeHidden = extension.includeHidden
            task.includeRemoved = extension.includeRemoved
            task.sourceVersion = extension.sourceVersion
            task.dependencyClasspath.from(extension.dependencyClasspath)

            task.groupByTag = extension.groupByTag
            task.includeTags = extension.includeTags
            task.excludePackages = extension.excludePackages
            task.templateConfig = extension.templateConfig

            // 解析源码路径
            List<String> resolvedSourcePaths = resolveSourcePaths(project, extension)
            task.sourcePaths.set(resolvedSourcePaths)
            task.projectName.set(extension.getProjectName().getOrElse(project.getName()))

            // 仅在项目是 Java/Groovy/Kotlin 项目时添加编译依赖
            if (project.plugins.findPlugin('java') || project.plugins.findPlugin('groovy') || project.plugins.findPlugin('org.jetbrains.kotlin.jvm')) {
                def compileJavaTask = project.tasks.findByName("compileJava")
                if (compileJavaTask != null) {
                    task.dependsOn(compileJavaTask)
                }
            }
        }
    }

    /**
     * 解析源码路径，支持自动发现子模块
     */
    private static List<String> resolveSourcePaths(Project project, ApiDocExtension extension) {
        List<String> sourcePaths = new ArrayList<>()

        // 1. 如果用户显式配置了 sourcePaths，优先使用
        if (!extension.getSourcePaths().get().isEmpty()) {
            extension.getSourcePaths().get().each { path ->
                File dir = new File(path).isAbsolute() ? new File(path) : project.rootProject.file(path)
                if (dir.exists() && dir.isDirectory()) {
                    String normalized = PathUtils.normalizePath(dir)
                    if (normalized != null) {
                        sourcePaths.add(normalized)
                    }
                }
            }
            return sourcePaths
        }

        // 2. 如果用户显式配置了 sourceDir，使用它
        String explicitSourceDir = extension.getSourceDir().get()
        if (explicitSourceDir != null && !explicitSourceDir.isEmpty() && explicitSourceDir != "src/main/java") {
            File dir = project.rootProject.file(explicitSourceDir)
            if (dir.exists() && dir.isDirectory()) {
                String normalized = PathUtils.normalizePath(dir)
                if (normalized != null) {
                    sourcePaths.add(normalized)
                }
            }
            return sourcePaths
        }

        // 3. 自动发现子模块源码路径
        boolean autoDiscover = extension.getAutoDiscoverSubprojects().get()
        if (autoDiscover && project.rootProject != null) {
            Set<String> excludedSubprojects = extension.getExcludeSubprojects().get()
            String relativePath = extension.getSubprojectSourceRelativePath().get()

            // 收集根项目本身的源码目录
            File rootSourceDir = new File(project.rootProject.projectDir, relativePath)
            if (rootSourceDir.exists() && rootSourceDir.isDirectory()) {
                String normalized = PathUtils.normalizePath(rootSourceDir)
                if (normalized != null) {
                    sourcePaths.add(normalized)
                }
            }

            // 遍历所有子项目
            project.rootProject.subprojects.each { subproject ->
                if (excludedSubprojects.contains(subproject.name)) {
                    return
                }

                File subprojectSourceDir = new File(subproject.projectDir, relativePath)
                if (subprojectSourceDir.exists() && subprojectSourceDir.isDirectory()) {
                    String normalized = PathUtils.normalizePath(subprojectSourceDir)
                    if (normalized != null) {
                        sourcePaths.add(normalized)
                    }
                }
            }
        }

        // 4. 如果没有发现任何路径，使用默认值
        if (sourcePaths.isEmpty()) {
            File defaultDir = new File(project.projectDir, "src/main/java")
            if (defaultDir.exists() && defaultDir.isDirectory()) {
                String normalized = PathUtils.normalizePath(defaultDir)
                if (normalized != null) {
                    sourcePaths.add(normalized)
                }
            }
        }

        return sourcePaths
    }
}

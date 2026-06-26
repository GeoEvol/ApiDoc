package com.byd.apidoc.parser.javadoc

import com.byd.apidoc.doclet.ApiDocDoclet
import com.byd.apidoc.doclet.BuildContext
import com.byd.apidoc.doclet.BuildContextRegistry
import com.byd.apidoc.doclet.DocletConfig
import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.ApiDoc
import com.byd.apidoc.model.DocCorpus
import com.byd.apidoc.parser.javadoc.stub.JavadocStubGenerator

import javax.tools.DiagnosticCollector
import javax.tools.DocumentationTool
import javax.tools.JavaFileObject
import javax.tools.StandardJavaFileManager
import javax.tools.ToolProvider
import java.nio.charset.Charset

class JavadocApiParser {

    List<ApiDoc> parse(List<String> sourcePaths, ApiConfig config) {
        return parseResult(sourcePaths, config).apiDocs
    }

    DocCorpus parseCorpus(List<String> sourcePaths, ApiConfig config) {
        return parseResult(sourcePaths, config).docCorpus
    }

    JavadocParseResult parseResult(List<String> sourcePaths, ApiConfig config) {
        BuildContext context = parseContext(sourcePaths, config)
        return new JavadocParseResult(
                apiDocs: context.apiDocs ?: [],
                docCorpus: context.docCorpus ?: new DocCorpus()
        )
    }

    private BuildContext parseContext(List<String> sourcePaths, ApiConfig config) {
        List<File> sourceFiles = collectJavaFiles(sourcePaths)
        if (sourceFiles.isEmpty()) {
            return new BuildContext(config: config, docCorpus: new DocCorpus())
        }

        List<String> resolveSourcePaths = resolveSourcePaths(sourcePaths, config)

        DocumentationTool tool = ToolProvider.systemDocumentationTool
        if (tool == null) {
            throw new IllegalStateException("JDK DocumentationTool is unavailable. Run ApiDoc with a JDK, not a JRE.")
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>()
        BuildContext context = BuildContextRegistry.register(new BuildContext(config: config))
        StandardJavaFileManager fileManager = null

        try {
            fileManager = tool.getStandardFileManager(diagnostics, Locale.getDefault(), Charset.forName("UTF-8"))
            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromFiles(sourceFiles)
            List<String> options = buildOptions(resolveSourcePaths, context.id, config)

            DocumentationTool.DocumentationTask task = tool.getTask(
                    new StringWriter(),
                    fileManager,
                    diagnostics,
                    ApiDocDoclet.class,
                    options,
                    units
            )
            Boolean ok = task.call()
            if (!ok) {
                String message = diagnostics.diagnostics.collect { diagnostic ->
                    "${diagnostic.kind}: ${diagnostic.getMessage(Locale.getDefault())}"
                }.join(System.lineSeparator())
                if (context.failure != null) {
                    throw context.failure
                }
                throw new RuntimeException("Javadoc parsing failed.${message ? System.lineSeparator() + message : ''}")
            }
            if (context.failure != null) {
                throw context.failure
            }
            return context
        } finally {
            BuildContextRegistry.remove(context.id)
            fileManager?.close()
        }
    }

    private static List<String> resolveSourcePaths(List<String> sourcePaths, ApiConfig config) {
        List<String> paths = new ArrayList<>(sourcePaths?.findAll { it != null } ?: [])
        if (config?.generatedStubsEnabled) {
            File stubRoot = new JavadocStubGenerator().generate(config)
            paths.add(stubRoot.absolutePath)
        }
        return paths
    }

    private static List<File> collectJavaFiles(List<String> sourcePaths) {
        List<File> files = []
        sourcePaths?.each { path ->
            File source = new File(path)
            if (source.isFile() && source.name.endsWith(".java")) {
                files.add(source)
            } else if (source.isDirectory()) {
                source.eachFileRecurse { File file ->
                    if (file.isFile() && file.name.endsWith(".java")) {
                        files.add(file)
                    }
                }
            }
        }
        return files
    }

    private static List<String> buildOptions(List<String> sourcePaths, String contextId, ApiConfig config) {
        List<String> sourceRoots = sourcePaths?.findAll { it != null } ?: []
        String sourceVersion = effectiveSourceVersion(config)
        List<String> options = [
                "-quiet",
                "-encoding", "UTF-8",
                "-source", sourceVersion,
                "-private",
                DocletConfig.CONTEXT_ID_OPTION, contextId
        ]
        if (!sourceRoots.isEmpty()) {
            options.add("-sourcepath")
            options.add(sourceRoots.unique().join(File.pathSeparator))
        }
        List<String> classPaths = []
        classPaths.addAll(config?.dependencyClasspath?.findAll { it } ?: [])
        String runtimeClasspath = System.getProperty("java.class.path")
        if (runtimeClasspath) {
            classPaths.add(runtimeClasspath)
        }
        if (!classPaths.isEmpty()) {
            options.add("-classpath")
            options.add(classPaths.unique().join(File.pathSeparator))
        }
        return options
    }

    private static String effectiveSourceVersion(ApiConfig config) {
        int runtimeVersion = detectJavaVersion()
        String configured = config?.sourceVersion?.trim()
        if (!configured) {
            return String.valueOf(runtimeVersion)
        }

        int configuredVersion = parseJavaMajor(configured)
        if (configuredVersion > runtimeVersion) {
            throw new IllegalArgumentException(
                    "Configured sourceVersion ${configuredVersion} is higher than current JDK ${runtimeVersion}. " +
                            "Run ApiDoc with JDK ${configuredVersion}+ or lower apiDoc.sourceVersion."
            )
        }
        return String.valueOf(configuredVersion)
    }

    private static int detectJavaVersion() {
        return parseJavaMajor(System.getProperty("java.version"))
    }

    private static int parseJavaMajor(String version) {
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("Java version must not be empty")
        }
        String normalized = version.trim()
        if (normalized.startsWith("1.")) {
            int end = normalized.indexOf('.', 2)
            return Integer.parseInt(end == -1 ? normalized.substring(2) : normalized.substring(2, end))
        }
        int dot = normalized.indexOf('.')
        return Integer.parseInt(dot == -1 ? normalized : normalized.substring(0, dot))
    }
}

package com.byd.apidoc.plugin

import groovy.json.JsonSlurper
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

class ApiDocPluginV2EndToEndTest {

    @Test
    void generateMarkdownTaskWritesV2AcceptanceOutputsFromSampleSdk() {
        File projectDir = new File("build/testkit/generate-markdown-v2")
        recreateDir(projectDir)
        writeSampleSdkProject(projectDir)

        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("generateMarkdown", "--stacktrace")
                .withPluginClasspath()
                .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateMarkdown").outcome)

        assertMarkdownOnlyV2AcceptanceOutputs(new File(projectDir, "build/apidoc-out"))
    }

    @Test
    void generateHtmlTaskWritesV2AcceptanceOutputsFromSampleSdk() {
        File projectDir = new File("build/testkit/generate-html-v2")
        recreateDir(projectDir)
        writeSampleSdkProject(projectDir)

        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("generateHtml", "--stacktrace")
                .withPluginClasspath()
                .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateHtml").outcome)

        assertHtmlOnlyV2AcceptanceOutputs(new File(projectDir, "build/apidoc-html-out"))
    }

    @Test
    void includeHiddenAndRemovedConfigExpandsProjectionFromGradlePlugin() {
        File projectDir = new File("build/testkit/generate-markdown-v2-internal")
        recreateDir(projectDir)
        writeSampleSdkProject(projectDir, true, true)

        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("generateMarkdown", "--stacktrace")
                .withPluginClasspath()
                .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateMarkdown").outcome)

        File outputDir = new File(projectDir, "build/apidoc-out")
        def corpus = new JsonSlurper().parse(new File(outputDir, "doc-corpus.json"))
        def pages = new JsonSlurper().parse(new File(outputDir, "page-index.json"))
        def search = new JsonSlurper().parse(new File(outputDir, "search-index.json"))

        assertTrue(corpus.types.any { it.qualifiedName == "com.example.sdk.HiddenApi" })
        assertTrue(corpus.types.any { it.qualifiedName == "com.example.sdk.RemovedApi" })
        assertTrue(pages.any { it.targetId?.qualifiedName == "com.example.sdk.HiddenApi" })
        assertTrue(pages.any { it.targetId?.qualifiedName == "com.example.sdk.RemovedApi" })
        assertTrue(search.any { it.qualifiedName == "com.example.sdk.HiddenApi" })
        assertTrue(search.any { it.qualifiedName == "com.example.sdk.RemovedApi" })
    }

    private static void assertCommonV2AcceptanceOutputs(File outputDir) {
        assertTrue(new File(outputDir, "doc-corpus.json").exists())
        assertTrue(new File(outputDir, "page-index.json").exists())
        assertTrue(new File(outputDir, "nav-index.json").exists())
        assertTrue(new File(outputDir, "search-index.json").exists())
        assertTrue(new File(outputDir, "output-manifest.json").exists())

        def manifest = new JsonSlurper().parse(new File(outputDir, "output-manifest.json"))
        assertEquals("1.0", manifest.schemaVersion)
        assertEquals("doc-corpus.json", manifest.outputs.corpus)
        assertEquals("page-index.json", manifest.outputs.pages)
        assertEquals("nav-index.json", manifest.outputs.nav)
        assertEquals("search-index.json", manifest.outputs.search)
        assertEquals("output-manifest.json", manifest.outputs.manifest)
        assertTrue(manifest.generatedAt != null && !manifest.generatedAt.toString().isEmpty())

        def corpus = new JsonSlurper().parse(new File(outputDir, "doc-corpus.json"))
        def pages = new JsonSlurper().parse(new File(outputDir, "page-index.json"))
        def rootSearch = new JsonSlurper().parse(new File(outputDir, "search-index.json"))

        assertTrue(corpus.types.any { it.qualifiedName == "com.example.sdk.HiddenApi" })
        assertTrue(corpus.types.any { it.qualifiedName == "com.example.sdk.RemovedApi" })
        assertFalse(pages.any { it.targetId?.qualifiedName == "com.example.sdk.HiddenApi" })
        assertFalse(pages.any { it.targetId?.qualifiedName == "com.example.sdk.RemovedApi" })
        assertFalse(rootSearch.any { it.qualifiedName == "com.example.sdk.HiddenApi" })
        assertFalse(rootSearch.any { it.qualifiedName == "com.example.sdk.RemovedApi" })
    }

    private static void assertMarkdownOnlyV2AcceptanceOutputs(File outputDir) {
        assertCommonV2AcceptanceOutputs(outputDir)

        File htmlRoot = new File(outputDir, "api-docs-html")
        File markdownRoot = new File(outputDir, "api-docs-md")
        assertFalse("generateMarkdown must not write HTML output", htmlRoot.exists())
        assertTrue(new File(markdownRoot, "reference/com.example.sdk.Foo.md").exists())
        assertTrue(new File(markdownRoot, "reference/com.example.sdk.inheritance.DerivedService.md").exists())

        def manifest = new JsonSlurper().parse(new File(outputDir, "output-manifest.json"))
        assertEquals("api-docs-md/", manifest.outputs.markdown)
        assertFalse(manifest.outputs.containsKey("html"))

        String inheritedMarkdown = new File(markdownRoot, "reference/com.example.sdk.inheritance.DerivedService.md").text
        assertFalse(inheritedMarkdown.contains("## Inherited Members"))
        assertFalse(inheritedMarkdown.contains("### Inherited from BaseService"))
        assertFalse(inheritedMarkdown.contains("[start()](com.example.sdk.inheritance.BaseService.md#start()) - Starts the base service."))

        String fooMarkdown = new File(markdownRoot, "reference/com.example.sdk.Foo.md").text
        assertTrue(fooMarkdown.contains("## API Status"))
        assertTrue(fooMarkdown.contains("- Since 1.0"))
        assertTrue(fooMarkdown.contains("- API 3"))
        assertTrue(fooMarkdown.contains("**@permission:** android.permission.INTERNET"))
        assertTrue(fooMarkdown.contains("sample.permission.RUN"))

        String deprecatedMarkdown = new File(markdownRoot, "reference/com.example.sdk.DeprecatedApi.md").text
        assertTrue(deprecatedMarkdown.contains("## API Status"))
        assertTrue(deprecatedMarkdown.contains("- Deprecated: use {@link Foo} instead"))
        assertTrue(deprecatedMarkdown.contains("- Deprecated since 2.0"))
        assertTrue(deprecatedMarkdown.contains("**Deprecated.** use [Foo](com.example.sdk.Foo.md#com.example.sdk.Foo) instead"))
        assertFalse(new File(markdownRoot, "reference/com.example.sdk.AndroidMetadataApi.md").exists())
    }

    private static void assertHtmlOnlyV2AcceptanceOutputs(File outputDir) {
        assertCommonV2AcceptanceOutputs(outputDir)

        File htmlRoot = new File(outputDir, "api-docs-html")
        File markdownRoot = new File(outputDir, "api-docs-md")
        assertFalse("generateHtml must not write Markdown output", markdownRoot.exists())
        assertTrue(new File(htmlRoot, "index.html").exists())
        assertTrue(new File(htmlRoot, "reference/com.example.sdk.Foo.html").exists())
        assertTrue(new File(htmlRoot, "reference/com.example.sdk.inheritance.DerivedService.html").exists())
        assertTrue(new File(htmlRoot, "assets/apidoc-devsite.css").exists())
        assertTrue(new File(htmlRoot, "assets/apidoc-devsite.js").exists())
        assertTrue(new File(htmlRoot, "assets/search.js").exists())
        assertTrue(new File(htmlRoot, "search-index.json").exists())

        def manifest = new JsonSlurper().parse(new File(outputDir, "output-manifest.json"))
        assertEquals("api-docs-html/", manifest.outputs.html)
        assertFalse(manifest.outputs.containsKey("markdown"))

        def rootSearch = new JsonSlurper().parse(new File(outputDir, "search-index.json"))
        def htmlSearch = new JsonSlurper().parse(new File(htmlRoot, "search-index.json"))
        assertEquals(rootSearch*.url, htmlSearch*.url)
        List<String> htmlDerivedFields = ["simpleName", "ownerSimpleName", "ownerQualifiedName", "displayTitle", "searchText"]
        assertTrue(rootSearch.every { Map item -> htmlDerivedFields.every { String field -> !item.containsKey(field) } })
        assertTrue(htmlSearch.every { Map item -> htmlDerivedFields.every { String field -> item.containsKey(field) } })
        assertTrue(htmlSearch.every { Map item -> item.containsKey("metadata") })

        def htmlFoo = htmlSearch.find { it.qualifiedName == "com.example.sdk.Foo" }
        assertNotNull(htmlFoo)
        assertEquals("Foo", htmlFoo.simpleName)
        assertEquals("Foo", htmlFoo.displayTitle)

        def htmlRun = htmlSearch.find { it.ownerQualifiedName == "com.example.sdk.Foo" && it.label == "run" }
        assertNotNull(htmlRun)
        assertEquals("Foo", htmlRun.ownerSimpleName)
        assertTrue(htmlRun.searchText.contains("Runs the sample operation") || htmlRun.searchText.contains("Runs a varargs overload"))

        String indexHtml = new File(htmlRoot, "index.html").text
        assertTrue(indexHtml.contains("class=\"ad-devsite-topbar\""))
        assertTrue(indexHtml.contains("class=\"ad-devsite-shell\""))
        assertTrue(indexHtml.contains("class=\"ad-devsite-book-nav\""))
        assertFalse(indexHtml.contains("ad-nav-filter"))
        assertTrue(indexHtml.contains("class=\"ad-devsite-content\""))
        assertTrue(indexHtml.contains("class=\"ad-devsite-toc\""))
        assertTrue(indexHtml.contains("src=\"assets/search.js\" data-root-prefix=\"\""))

        String fooHtml = new File(htmlRoot, "reference/com.example.sdk.Foo.html").text
        assertTrue(fooHtml.contains("class=\"ad-devsite-topbar\""))
        assertTrue(fooHtml.contains("class=\"ad-devsite-shell\""))
        assertTrue(fooHtml.contains("class=\"ad-devsite-toc\""))
        assertTrue(fooHtml.contains("<div class=\"ad-toc-title\">On this page</div>"))
        assertTrue(fooHtml.contains("href=\"#constants\""))
        assertTrue(fooHtml.contains("href=\"#details\""))
        assertTrue(fooHtml.contains("src=\"../assets/search.js\" data-root-prefix=\"../\""))
        assertFalse(fooHtml.contains("<th>Description</th>"))
        assertTrue(fooHtml.contains("class=\"ad-member-description\""))
        assertTrue(fooHtml.contains("class=\"ad-section-kind-icon"))
        assertFalse(fooHtml.contains("class=\"ad-member-icon\""))
        assertTrue(fooHtml.contains("class=\"ad-detail-table\""))
        assertFalse(fooHtml.contains("devsite.google"))
        assertFalse(fooHtml.contains("cdn.jsdelivr.net"))

        String inheritedHtml = new File(htmlRoot, "reference/com.example.sdk.inheritance.DerivedService.html").text
        assertFalse(inheritedHtml.contains("id=\"inherited-members\""))
        assertFalse(inheritedHtml.contains("Inherited from BaseService"))

        assertSearchLinksResolveFromRootAndNestedPages(htmlRoot, rootSearch)
    }

    private static void assertSearchLinksResolveFromRootAndNestedPages(File htmlRoot, List search) {
        def foo = search.find { it.qualifiedName == "com.example.sdk.Foo" }
        def run = search.find { it.ownerName == "com.example.sdk.Foo" && it.label == "run" && it.url?.contains("#") }
        assertNotNull(foo)
        assertNotNull(run)

        [foo, run].each { entry ->
            assertFalse(entry.url.toString().startsWith("http://"))
            assertFalse(entry.url.toString().startsWith("https://"))
            assertFalse(entry.url.toString().startsWith("/"))
            String pathOnly = entry.url.toString().split("#", 2)[0]
            assertTrue(new File(htmlRoot, pathOnly).exists())
            assertTrue(new File(htmlRoot, pathOnly).text.contains(entry.anchor.toString()))
            assertTrue(new File(new File(htmlRoot, "reference"), "../${pathOnly}").canonicalFile == new File(htmlRoot, pathOnly).canonicalFile)
        }
    }

    private static void writeSampleSdkProject(File projectDir, boolean includeHidden = false, boolean includeRemoved = false) {
        new File(projectDir, "settings.gradle").text = "pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }\nrootProject.name = 'apidoc-v2-fixture'\n"
        new File(projectDir, "build.gradle").text = """
plugins {
    id 'java'
    id 'com.byd.apidoc'
}

repositories {
    mavenCentral()
}

apiDoc {
    projectName = 'Sample SDK'
    sourcePaths = ['src/main/java']
    outputDir = ['build/apidoc-out', 'build/apidoc-html-out']
    sourceVersion = '${runtimeJavaMajor()}'
    dependencyClasspath.from()
    includeHidden = ${includeHidden}
    includeRemoved = ${includeRemoved}
}
"""
        copyDir(new File("src/test/resources/sample-sdk"), projectDir)
    }

    private static void copyDir(File source, File target) {
        source.eachFileRecurse { File file ->
            if (file.isFile()) {
                File destination = new File(target, source.toPath().relativize(file.toPath()).toString())
                destination.parentFile.mkdirs()
                destination.bytes = file.bytes
            }
        }
    }

    private static void recreateDir(File dir) {
        if (dir.exists()) {
            dir.deleteDir()
        }
        dir.mkdirs()
    }

    private static int runtimeJavaMajor() {
        String version = System.getProperty("java.version")
        if (version.startsWith("1.")) {
            return Integer.parseInt(version.substring(2, version.indexOf('.', 2)))
        }
        int dot = version.indexOf('.')
        return Integer.parseInt(dot == -1 ? version : version.substring(0, dot))
    }
}

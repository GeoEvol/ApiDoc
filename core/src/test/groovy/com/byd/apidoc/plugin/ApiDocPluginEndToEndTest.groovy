package com.byd.apidoc.plugin

import groovy.json.JsonSlurper
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class ApiDocPluginEndToEndTest {

    @Test
    void generateMarkdownTaskWritesV1StaticSiteContract() {
        File projectDir = new File("build/testkit/generate-markdown-v1")
        recreateDir(projectDir)
        writeFixtureProject(projectDir, false, false)

        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("generateMarkdown", "--stacktrace")
                .withPluginClasspath()
                .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateMarkdown").outcome)

        File outputDir = new File(projectDir, "build/apidoc-out")
        assertTrue(new File(outputDir, "doc-corpus.json").exists())
        assertTrue(new File(outputDir, "page-index.json").exists())
        assertTrue(new File(outputDir, "nav-index.json").exists())
        assertTrue(new File(outputDir, "search-index.json").exists())
        assertTrue(new File(outputDir, "output-manifest.json").exists())
        assertTrue(new File(outputDir, "api-docs-md/index.md").exists())
        assertTrue(new File(outputDir, "api-docs-md/reference/com.example.fixture.PublicApi.md").exists())
        assertFalse(new File(outputDir, "api-docs-html").exists())
        assertFalse(new File(outputDir, "index.md").exists())
        assertFalse(new File(outputDir, "index.html").exists())
        assertFalse(new File(outputDir, "com/example/fixture/PublicApi.md").exists())
        assertFalse(new File(outputDir, "com/example/fixture/PublicApi.html").exists())

        def manifest = new JsonSlurper().parse(new File(outputDir, "output-manifest.json"))
        assertEquals("1.0", manifest.schemaVersion)
        assertEquals("doc-corpus.json", manifest.outputs.corpus)
        assertEquals("nav-index.json", manifest.outputs.nav)
        assertEquals("search-index.json", manifest.outputs.search)
        assertEquals("api-docs-md/", manifest.outputs.markdown)
        assertFalse(manifest.outputs.containsKey("html"))

        def corpus = new JsonSlurper().parse(new File(outputDir, "doc-corpus.json"))
        def pages = new JsonSlurper().parse(new File(outputDir, "page-index.json"))
        def search = new JsonSlurper().parse(new File(outputDir, "search-index.json"))

        assertTrue(corpus.types.any { it.qualifiedName == "com.example.fixture.HiddenApi" })
        assertTrue(corpus.types.any { it.qualifiedName == "com.example.fixture.RemovedApi" })
        assertFalse(pages.any { it.targetId?.qualifiedName == "com.example.fixture.HiddenApi" })
        assertFalse(pages.any { it.targetId?.qualifiedName == "com.example.fixture.RemovedApi" })
        assertFalse(search.any { it.qualifiedName == "com.example.fixture.HiddenApi" })
        assertFalse(search.any { it.qualifiedName == "com.example.fixture.RemovedApi" })
        assertTrue(search.any { it.qualifiedName == "com.example.fixture.PublicApi" })

        String markdown = new File(outputDir, "api-docs-md/reference/com.example.fixture.PublicApi.md").text
        assertTrue(markdown.contains("PublicApi"))
        assertTrue(markdown.contains("helper"))
    }

    @Test
    void generateHtmlTaskWritesV1StaticSiteContract() {
        File projectDir = new File("build/testkit/generate-html-v1")
        recreateDir(projectDir)
        writeFixtureProject(projectDir, false, false)

        def result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("generateHtml", "--stacktrace")
                .withPluginClasspath()
                .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateHtml").outcome)

        File outputDir = new File(projectDir, "build/apidoc-html-out")
        assertTrue(new File(outputDir, "doc-corpus.json").exists())
        assertTrue(new File(outputDir, "page-index.json").exists())
        assertTrue(new File(outputDir, "nav-index.json").exists())
        assertTrue(new File(outputDir, "search-index.json").exists())
        assertTrue(new File(outputDir, "output-manifest.json").exists())
        assertFalse(new File(outputDir, "api-docs-md").exists())
        assertTrue(new File(outputDir, "api-docs-html/index.html").exists())
        assertTrue(new File(outputDir, "api-docs-html/reference/com.example.fixture.PublicApi.html").exists())
        assertTrue(new File(outputDir, "api-docs-html/assets/search.js").exists())
        assertFalse(new File(outputDir, "index.html").exists())
        assertFalse(new File(outputDir, "index-all.html").exists())
        assertFalse(new File(outputDir, "member-search-index.js").exists())
        assertFalse(new File(outputDir, "com/example/fixture/PublicApi.html").exists())

        def manifest = new JsonSlurper().parse(new File(outputDir, "output-manifest.json"))
        assertEquals("api-docs-html/", manifest.outputs.html)
        assertFalse(manifest.outputs.containsKey("markdown"))

        String html = new File(outputDir, "api-docs-html/reference/com.example.fixture.PublicApi.html").text
        assertTrue(html.contains("PublicApi"))
        assertTrue(html.contains("data-root-prefix=\"../\""))
    }

    @Test
    void includeHiddenAndRemovedKeepsCorpusStableAndExpandsProjection() {
        File projectDir = new File("build/testkit/generate-markdown-v1-internal")
        recreateDir(projectDir)
        writeFixtureProject(projectDir, true, true)

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

        assertTrue(corpus.types.any { it.qualifiedName == "com.example.fixture.HiddenApi" })
        assertTrue(corpus.types.any { it.qualifiedName == "com.example.fixture.RemovedApi" })
        assertTrue(pages.any { it.targetId?.qualifiedName == "com.example.fixture.HiddenApi" })
        assertTrue(pages.any { it.targetId?.qualifiedName == "com.example.fixture.RemovedApi" })
        assertTrue(search.any { it.qualifiedName == "com.example.fixture.HiddenApi" })
        assertTrue(search.any { it.qualifiedName == "com.example.fixture.RemovedApi" })
    }

    private static void writeFixtureProject(File projectDir, boolean includeHidden, boolean includeRemoved) {
        new File(projectDir, "settings.gradle").text = "pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }\nrootProject.name = 'apidoc-fixture'\n"
        new File(projectDir, "build.gradle").text = """
plugins {
    id 'java'
    id 'com.byd.apidoc'
}

repositories {
    mavenCentral()
}

apiDoc {
    projectName = 'Fixture SDK'
    sourcePaths = ['src/main/java']
    outputDir = ['build/apidoc-out', 'build/apidoc-html-out']
    sourceVersion = '${runtimeJavaMajor()}'
    dependencyClasspath.from()
    includeHidden = ${includeHidden}
    includeRemoved = ${includeRemoved}
}
"""
        File sourceDir = new File(projectDir, "src/main/java/com/example/fixture")
        sourceDir.mkdirs()
        new File(sourceDir, "PublicApi.java").text = """
package com.example.fixture;

/**
 * Public API summary.
 *
 * Public API body with {@link HelperApi helper type}.
 *
 * @since 1.0
 */
public class PublicApi {
    /**
     * Performs work.
     *
     * @param value input value
     * @return work result
     * @throws IllegalArgumentException when value is empty
     */
    public String work(String value) {
        return value;
    }
}
"""
        new File(sourceDir, "HelperApi.java").text = """
package com.example.fixture;

/** Helper API summary. */
public class HelperApi {
}
"""
        new File(sourceDir, "HiddenApi.java").text = """
package com.example.fixture;

/**
 * Hidden API summary.
 *
 * @hide
 */
public class HiddenApi {
}
"""
        new File(sourceDir, "RemovedApi.java").text = """
package com.example.fixture;

/**
 * Removed API summary.
 *
 * @removed
 */
public class RemovedApi {
}
"""
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

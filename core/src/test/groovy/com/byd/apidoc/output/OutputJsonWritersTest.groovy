package com.byd.apidoc.output

import com.byd.apidoc.metadata.VisibilityPolicy
import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.DocCorpus
import com.byd.apidoc.parser.javadoc.JavadocApiParser
import com.byd.apidoc.projection.DocProjection
import com.byd.apidoc.projection.ProjectionBuilder
import groovy.json.JsonSlurper
import org.junit.Test

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class OutputJsonWritersTest {

    @Test
    void writesCorpusProjectionAndManifestJsonContracts() {
        File outputDir = new File("build/test-v1-output-writers")
        recreateDir(outputDir)

        DocCorpus corpus = sampleCorpus()
        DocProjection projection = new ProjectionBuilder().build(corpus, new VisibilityPolicy())

        File corpusFile = new DocCorpusWriter().write(corpus, outputDir)
        Map<String, File> projectionFiles = new ProjectionWriter().write(projection, outputDir)
        File manifestFile = new OutputManifestWriter(
                new JsonWriter(),
                Clock.fixed(Instant.parse("2026-05-26T00:00:00Z"), ZoneOffset.UTC)
        ).write(outputDir)

        def corpusJson = new JsonSlurper().parse(corpusFile)
        assertEquals("v1", corpusJson.schemaVersion)
        assertTrue(corpusJson.types.any { it.qualifiedName == "com.example.sdk.Foo" })
        assertTrue(corpusJson.types.any { it.qualifiedName == "com.example.sdk.HiddenApi" && it.metadata.visibility == "HIDDEN" })
        assertTrue(corpusJson.types.any { it.qualifiedName == "com.example.sdk.RemovedApi" && it.metadata.availability == "REMOVED" })
        assertTrue(corpusJson.members.any { it.ownerId.qualifiedName == "com.example.sdk.Foo" && it.name == "run" })

        def pages = new JsonSlurper().parse(projectionFiles.pages)
        def nav = new JsonSlurper().parse(projectionFiles.nav)
        def search = new JsonSlurper().parse(projectionFiles.search)
        assertTrue(pages.any { it.title == "Foo" && it.url == "reference/com.example.sdk.Foo.html" })
        assertTrue(nav.any { it.label == "com.example.sdk" })
        assertTrue(search.any { it.label == "Foo" && it.kind == "CLASS" })
        assertEquals("nav-index.json", projectionFiles.nav.name)
        assertEquals("search-index.json", projectionFiles.search.name)

        def manifest = new JsonSlurper().parse(manifestFile)
        assertEquals("output-manifest.json", manifestFile.name)
        assertEquals("1.0", manifest.schemaVersion)
        assertEquals("1.0.0", manifest.generatorVersion)
        assertEquals("2026-05-26T00:00:00Z", manifest.generatedAt)
        assertEquals("doc-corpus.json", manifest.outputs.corpus)
        assertEquals("page-index.json", manifest.outputs.pages)
        assertEquals("nav-index.json", manifest.outputs.nav)
        assertEquals("search-index.json", manifest.outputs.search)
        assertEquals("output-manifest.json", manifest.outputs.manifest)
        assertEquals("api-docs-html/", manifest.outputs.html)
        assertEquals("api-docs-md/", manifest.outputs.markdown)
    }

    @Test
    void manifestCanBeLimitedToOneRenderedFormat() {
        File markdownDir = new File("build/test-output-manifest-markdown")
        File htmlDir = new File("build/test-output-manifest-html")
        recreateDir(markdownDir)
        recreateDir(htmlDir)

        def markdownManifest = new JsonSlurper().parse(new OutputManifestWriter().write(markdownDir, ApiConfig.FORMAT_MARKDOWN))
        def htmlManifest = new JsonSlurper().parse(new OutputManifestWriter().write(htmlDir, ApiConfig.FORMAT_HTML))

        assertEquals("api-docs-md/", markdownManifest.outputs.markdown)
        assertTrue(!markdownManifest.outputs.containsKey("html"))
        assertEquals("api-docs-html/", htmlManifest.outputs.html)
        assertTrue(!htmlManifest.outputs.containsKey("markdown"))
    }

    private static DocCorpus sampleCorpus() {
        File sourceRoot = new File("src/test/resources/sample-sdk/src/main/java")
        return new JavadocApiParser().parseCorpus([sourceRoot.absolutePath], new ApiConfig())
    }

    private static void recreateDir(File dir) {
        if (dir.exists()) {
            dir.deleteDir()
        }
        dir.mkdirs()
    }
}

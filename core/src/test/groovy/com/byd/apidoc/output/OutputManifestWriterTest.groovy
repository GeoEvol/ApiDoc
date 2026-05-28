package com.byd.apidoc.output

import com.byd.apidoc.model.ApiConfig
import groovy.json.JsonSlurper
import org.junit.Test

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class OutputManifestWriterTest {

    @Test
    void writesStableManifestContract() {
        File outputDir = new File("build/test-output-manifest")
        if (outputDir.exists()) {
            outputDir.deleteDir()
        }
        Clock clock = Clock.fixed(Instant.parse("2026-05-26T00:00:00Z"), ZoneOffset.UTC)

        File outputFile = new OutputManifestWriter(new JsonWriter(), clock).write(outputDir)
        def json = new JsonSlurper().parse(outputFile)

        assertTrue(outputFile.exists())
        assertEquals("output-manifest.json", outputFile.name)
        assertEquals("1.0", json.schemaVersion)
        assertEquals("1.0.0", json.generatorVersion)
        assertEquals("2026-05-26T00:00:00Z", json.generatedAt)
        assertEquals("doc-corpus.json", json.outputs.corpus)
        assertEquals("page-index.json", json.outputs.pages)
        assertEquals("nav-index.json", json.outputs.nav)
        assertEquals("search-index.json", json.outputs.search)
        assertEquals("output-manifest.json", json.outputs.manifest)
        assertEquals("api-docs-html/", json.outputs.html)
        assertEquals("api-docs-md/", json.outputs.markdown)
    }

    @Test
    void writesOnlyMarkdownEntryForMarkdownOutput() {
        File outputDir = new File("build/test-output-manifest-markdown")
        if (outputDir.exists()) {
            outputDir.deleteDir()
        }

        File outputFile = new OutputManifestWriter().write(outputDir, ApiConfig.FORMAT_MARKDOWN)
        def json = new JsonSlurper().parse(outputFile)

        assertEquals("api-docs-md/", json.outputs.markdown)
        assertTrue(!json.outputs.containsKey("html"))
    }

    @Test
    void writesOnlyHtmlEntryForHtmlOutput() {
        File outputDir = new File("build/test-output-manifest-html")
        if (outputDir.exists()) {
            outputDir.deleteDir()
        }

        File outputFile = new OutputManifestWriter().write(outputDir, ApiConfig.FORMAT_HTML)
        def json = new JsonSlurper().parse(outputFile)

        assertEquals("api-docs-html/", json.outputs.html)
        assertTrue(!json.outputs.containsKey("markdown"))
    }
}

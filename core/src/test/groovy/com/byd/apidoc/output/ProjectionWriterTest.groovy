package com.byd.apidoc.output

import com.byd.apidoc.metadata.VisibilityPolicy
import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.DocCorpus
import com.byd.apidoc.parser.javadoc.JavadocApiParser
import com.byd.apidoc.projection.DocProjection
import com.byd.apidoc.projection.ProjectionBuilder
import groovy.json.JsonSlurper
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class ProjectionWriterTest {

    @Test
    void writesPageNavAndSearchIndexes() {
        File sourceRoot = new File("src/test/resources/sample-sdk/src/main/java")
        DocCorpus corpus = new JavadocApiParser().parseCorpus([sourceRoot.absolutePath], new ApiConfig())
        DocProjection projection = new ProjectionBuilder().build(corpus, new VisibilityPolicy())
        File outputDir = new File("build/test-v1-projection-output")
        if (outputDir.exists()) {
            outputDir.deleteDir()
        }

        Map<String, File> files = new ProjectionWriter().write(projection, outputDir)

        assertTrue(files.pages.exists())
        assertTrue(files.nav.exists())
        assertTrue(files.search.exists())

        def pages = new JsonSlurper().parse(files.pages)
        def nav = new JsonSlurper().parse(files.nav)
        def search = new JsonSlurper().parse(files.search)

        assertTrue(pages.any { it.title == "Foo" && it.url == "reference/com.example.sdk.Foo.html" })
        assertTrue(nav.any { it.label == "com.example.sdk" })
        assertTrue(search.any { it.label == "Foo" && it.kind == "CLASS" })
        assertEquals("nav-index.json", files.nav.name)
        assertEquals("search-index.json", files.search.name)
    }
}

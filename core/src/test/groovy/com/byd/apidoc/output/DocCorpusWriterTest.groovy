package com.byd.apidoc.output

import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.DocCorpus
import com.byd.apidoc.parser.javadoc.JavadocApiParser
import groovy.json.JsonSlurper
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class DocCorpusWriterTest {

    @Test
    void writesDocCorpusJsonFromSampleSdk() {
        File sourceRoot = new File("src/test/resources/sample-sdk/src/main/java")
        DocCorpus corpus = new JavadocApiParser().parseCorpus([sourceRoot.absolutePath], new ApiConfig())
        File outputDir = new File("build/test-v1-output")
        if (outputDir.exists()) {
            outputDir.deleteDir()
        }

        File outputFile = new DocCorpusWriter().write(corpus, outputDir)
        def json = new JsonSlurper().parse(outputFile)

        assertTrue(outputFile.exists())
        assertEquals("v1", json.schemaVersion)
        assertTrue(json.types.any { it.qualifiedName == "com.example.sdk.Foo" })
        assertTrue(json.types.any { it.qualifiedName == "com.example.sdk.HiddenApi" && it.metadata.visibility == "HIDDEN" })
        assertTrue(json.types.any { it.qualifiedName == "com.example.sdk.RemovedApi" && it.metadata.availability == "REMOVED" })
        assertTrue(json.members.any { it.ownerId.qualifiedName == "com.example.sdk.Foo" && it.name == "run" })
    }
}

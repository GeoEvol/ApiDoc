package com.byd.apidoc.pipeline

import com.byd.apidoc.model.ApiConfig
import groovy.json.JsonSlurper
import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class RenderPipelineCoordinatorV1JsonTest {

    @Test
    void writesV1JsonOutputsDuringNormalGeneration() {
        File sourceRoot = new File("src/test/resources/sample-sdk/src/main/java")
        File outputDir = new File("build/test-v1-pipeline-output/public")
        recreateDir(outputDir)

        ApiConfig config = new ApiConfig(outputFormat: ApiConfig.FORMAT_MARKDOWN)

        new RenderPipelineCoordinator().generate([sourceRoot.absolutePath], "Sample SDK", outputDir, config)

        assertTrue(new File(outputDir, "doc-corpus.json").exists())
        assertTrue(new File(outputDir, "page-index.json").exists())
        assertTrue(new File(outputDir, "nav-index.json").exists())
        assertTrue(new File(outputDir, "search-index.json").exists())
        assertTrue(new File(outputDir, "output-manifest.json").exists())
        assertTrue(new File(outputDir, "api-docs-md/index.md").exists())
        assertFalse(new File(outputDir, "api-docs-html").exists())
        assertFalse(new File(outputDir, "index.md").exists())
        assertFalse(new File(outputDir, "index.html").exists())
        assertFalse(new File(outputDir, "index-all.html").exists())
        assertFalse(new File(outputDir, "member-search-index.js").exists())
        assertFalse(new File(outputDir, "com/example/sdk/Foo.md").exists())
        assertFalse(new File(outputDir, "com/example/sdk/Foo.html").exists())

        def corpus = new JsonSlurper().parse(new File(outputDir, "doc-corpus.json"))
        def pages = new JsonSlurper().parse(new File(outputDir, "page-index.json"))
        def search = new JsonSlurper().parse(new File(outputDir, "search-index.json"))
        def manifest = new JsonSlurper().parse(new File(outputDir, "output-manifest.json"))

        assertTrue(corpus.types.any { it.qualifiedName == "com.example.sdk.HiddenApi" })
        assertTrue(corpus.types.any { it.qualifiedName == "com.example.sdk.RemovedApi" })
        assertFalse(pages.any { it.targetId?.qualifiedName == "com.example.sdk.HiddenApi" })
        assertFalse(search.any { it.qualifiedName == "com.example.sdk.RemovedApi" })
        assertTrue(search.any { it.qualifiedName == "com.example.sdk.Foo" })
        assertTrue(manifest.generatedAt != null && !manifest.generatedAt.toString().isEmpty())
        assertTrue(manifest.outputs.corpus == "doc-corpus.json")
        assertTrue(manifest.outputs.nav == "nav-index.json")
        assertTrue(manifest.outputs.search == "search-index.json")
        assertTrue(manifest.outputs.markdown == "api-docs-md/")
        assertFalse(manifest.outputs.containsKey("html"))
    }

    @Test
    void includeHiddenAndRemovedConfigAffectsProjectionOnly() {
        File sourceRoot = new File("src/test/resources/sample-sdk/src/main/java")
        File outputDir = new File("build/test-v1-pipeline-output/internal")
        recreateDir(outputDir)

        ApiConfig config = new ApiConfig(
                outputFormat: ApiConfig.FORMAT_MARKDOWN,
                includeHidden: true,
                includeRemoved: true
        )

        new RenderPipelineCoordinator().generate([sourceRoot.absolutePath], "Sample SDK", outputDir, config)

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

    private static void recreateDir(File dir) {
        if (dir.exists()) {
            dir.deleteDir()
        }
        dir.mkdirs()
    }
}

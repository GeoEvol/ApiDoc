package com.byd.apidoc.render

import com.byd.apidoc.metadata.VisibilityPolicy
import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.DocCorpus
import com.byd.apidoc.parser.javadoc.JavadocApiParser
import com.byd.apidoc.projection.DocProjection
import com.byd.apidoc.projection.ProjectionBuilder
import com.byd.apidoc.reference.ReferenceResolver
import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class MarkdownRendererTest {

    @Test
    void rendersMinimalMarkdownReferenceFromProjection() {
        File outputDir = new File("build/test-v1-markdown-renderer")
        if (outputDir.exists()) {
            outputDir.deleteDir()
        }
        DocCorpus corpus = sampleCorpus()
        new ReferenceResolver().resolve(corpus)
        DocProjection projection = new ProjectionBuilder().build(corpus, new VisibilityPolicy())

        new MarkdownRenderer().render(new RenderContext(
                corpus: corpus,
                projection: projection,
                outputDir: outputDir,
                projectName: "Sample SDK"
        ))

        File root = new File(outputDir, "api-docs-md")
        assertTrue(new File(root, "index.md").text.contains("# Sample SDK"))
        assertTrue(new File(root, "packages.md").text.contains("com.example.sdk"))
        assertTrue(new File(root, "classes.md").text.contains("com.example.sdk.Foo"))

        File foo = new File(root, "reference/com.example.sdk.Foo.md")
        assertTrue(foo.exists())
        String text = foo.text
        assertTrue(text.contains("# Foo"))
        assertTrue(text.contains("**Package:** `com.example.sdk`"))
        assertTrue(text.contains("class Foo<T>"))
        assertTrue(text.contains("**Implements:** [ServiceContract](com.example.sdk.ServiceContract.md#com.example.sdk.ServiceContract)"))
        assertTrue(text.contains("[helper type](com.example.sdk.Bar.md#com.example.sdk.Bar)"))
        assertTrue(text.contains("[plain list](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/List.html)"))
        assertTrue(text.contains("`code literal`"))
        assertTrue(text.contains("**@permission:** android.permission.INTERNET"))
        assertTrue(text.contains("## Constants"))
        assertTrue(text.contains("## Constructors"))
        assertTrue(text.contains("## Methods"))
        assertTrue(text.contains("## Inherited Members"))
        assertTrue(text.contains("run("))
        assertTrue(text.contains("**Parameters**"))
        assertTrue(text.contains("`value`: value description"))
        assertTrue(text.contains("**Returns:** mapped result"))
        assertTrue(text.contains("**Throws**"))
        assertTrue(text.contains("`IllegalArgumentException`: if value is invalid"))

        assertFalse(new File(root, "reference/com.example.sdk.HiddenApi.md").exists())
        assertFalse(new File(root, "reference/com.example.sdk.RemovedApi.md").exists())
    }

    private static DocCorpus sampleCorpus() {
        File sourceRoot = new File("src/test/resources/sample-sdk/src/main/java")
        return new JavadocApiParser().parseCorpus([sourceRoot.absolutePath], new ApiConfig())
    }
}

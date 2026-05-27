package com.byd.apidoc.render

import com.byd.apidoc.metadata.VisibilityPolicy
import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.DocCorpus
import com.byd.apidoc.parser.javadoc.JavadocApiParser
import com.byd.apidoc.projection.DocProjection
import com.byd.apidoc.projection.ProjectionBuilder
import com.byd.apidoc.reference.ReferenceResolver
import groovy.json.JsonSlurper
import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class BuiltinHtmlRendererTest {

    @Test
    void rendersMinimalStaticHtmlReferenceFromProjection() {
        File outputDir = new File("build/test-v1-html-renderer")
        if (outputDir.exists()) {
            outputDir.deleteDir()
        }
        DocCorpus corpus = sampleCorpus()
        new ReferenceResolver().resolve(corpus)
        DocProjection projection = new ProjectionBuilder().build(corpus, new VisibilityPolicy())

        new BuiltinHtmlRenderer().render(new RenderContext(
                corpus: corpus,
                projection: projection,
                outputDir: outputDir,
                projectName: "Sample SDK"
        ))

        File root = new File(outputDir, "api-docs-html")
        assertTrue(new File(root, "index.html").text.contains("API Reference"))
        assertTrue(new File(root, "packages.html").text.contains("com.example.sdk"))
        assertTrue(new File(root, "classes.html").text.contains("com.example.sdk.Foo"))
        assertTrue(new File(root, "assets/apidoc.css").exists())
        assertTrue(new File(root, "assets/apidoc.js").exists())
        assertTrue(new File(root, "assets/search.js").exists())
        assertTrue(new File(root, "assets/search.js").text.contains("ad-search-results"))
        assertTrue(new File(root, "assets/search.js").text.contains("fetch(url)"))
        assertTrue(new File(root, "assets/search.js").text.contains("getAttribute('data-root-prefix')"))
        assertFalse(new File(root, "assets/search.js").text.contains("href=\"'+esc('../'+"))
        assertTrue(new File(root, "nav-index.json").exists())
        assertTrue(new File(root, "search-index.json").exists())

        String indexHtml = new File(root, "index.html").text
        assertTrue(indexHtml.contains("<script src=\"assets/search.js\" data-root-prefix=\"\"></script>"))

        File foo = new File(root, "reference/com.example.sdk.Foo.html")
        assertTrue(foo.exists())
        String text = foo.text
        assertTrue(text.contains("<h1>Foo</h1>"))
        assertTrue(text.contains("class Foo&lt;T&gt;"))
        assertTrue(text.contains("<a href=\"com.example.sdk.ServiceContract.html#com.example.sdk.ServiceContract\">ServiceContract</a>"))
        assertTrue(text.contains("<a href=\"com.example.sdk.Bar.html#com.example.sdk.Bar\">helper type</a>"))
        assertTrue(text.contains("<a href=\"https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/List.html\">plain list</a>"))
        assertTrue(text.contains("<code>code literal</code>"))
        assertTrue(text.contains("<strong>@permission:</strong> android.permission.INTERNET"))
        assertTrue(text.contains("Constants"))
        assertTrue(text.contains("Constructors"))
        assertTrue(text.contains("Methods"))
        assertTrue(text.contains("Inherited Members"))
        assertTrue(text.contains("<h4>Parameters</h4>"))
        assertTrue(text.contains("<dt><code>value</code></dt><dd>value description</dd>"))
        assertTrue(text.contains("<strong>Returns:</strong> mapped result"))
        assertTrue(text.contains("<h4>Throws</h4>"))
        assertTrue(text.contains("../assets/apidoc.css"))
        assertTrue(text.contains("<script src=\"../assets/search.js\" data-root-prefix=\"../\"></script>"))

        def search = new JsonSlurper().parse(new File(root, "search-index.json"))
        assertTrue(search.any { it.qualifiedName == "com.example.sdk.Foo" })

        assertFalse(new File(root, "reference/com.example.sdk.HiddenApi.html").exists())
        assertFalse(new File(root, "reference/com.example.sdk.RemovedApi.html").exists())
    }

    private static DocCorpus sampleCorpus() {
        File sourceRoot = new File("src/test/resources/sample-sdk/src/main/java")
        return new JavadocApiParser().parseCorpus([sourceRoot.absolutePath], new ApiConfig())
    }
}

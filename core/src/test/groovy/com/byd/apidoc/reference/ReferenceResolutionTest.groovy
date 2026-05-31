package com.byd.apidoc.reference

import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.DocCorpus
import com.byd.apidoc.model.LinkRefKind
import com.byd.apidoc.parser.javadoc.JavadocApiParser
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class ReferenceResolutionTest {

    @Test
    void resolvesExternalInternalUnresolvedAndRelativeLinks() {
        ExternalLinkResolver externalResolver = new ExternalLinkResolver()
        def string = externalResolver.resolveType("java.lang.String")
        def context = externalResolver.resolveType("android.content.Context")
        def nonNull = externalResolver.resolveType("androidx.annotation.NonNull")
        def unknown = externalResolver.resolveType("com.example.LocalType")

        assertEquals(LinkRefKind.EXTERNAL, string.kind)
        assertTrue(string.externalUrl.endsWith("java/lang/String.html"))
        assertEquals("String", string.label)
        assertEquals(LinkRefKind.EXTERNAL, context.kind)
        assertEquals("https://developer.android.com/reference/android/content/Context.html", context.externalUrl)
        assertEquals(LinkRefKind.EXTERNAL, nonNull.kind)
        assertEquals("https://developer.android.com/reference/androidx/annotation/NonNull.html", nonNull.externalUrl)
        assertEquals(LinkRefKind.UNRESOLVED, unknown.kind)
        assertEquals("com.example.LocalType", unknown.rawTarget)
        assertEquals("LocalType", unknown.label)

        DocCorpus corpus = sampleCorpus()
        new ReferenceResolver().resolve(corpus)
        def foo = corpus.types.find { it.qualifiedName == "com.example.sdk.Foo" }
        def convert = corpus.members.find { it.ownerId.qualifiedName == "com.example.sdk.Foo" && it.name == "convert" }

        assertEquals(LinkRefKind.INTERNAL, foo.interfaces[0].linkRef.kind)
        assertEquals("com.example.sdk.ServiceContract", foo.interfaces[0].linkRef.targetId.qualifiedName)
        assertEquals(LinkRefKind.UNRESOLVED, convert.parameters[0].type.linkRef.kind)
        assertTrue(foo.comment.inlineNodes.any {
            it.name == "link" && it.reference.kind == LinkRefKind.INTERNAL && it.reference.targetId.qualifiedName == "com.example.sdk.Bar"
        })
        assertTrue(foo.comment.inlineNodes.any {
            it.name == "linkplain" && it.reference.kind == LinkRefKind.UNRESOLVED && it.reference.fallbackText == "plain list"
        })

        DocCorpus externalCorpus = sampleCorpus()
        new ReferenceResolver(new ExternalLinkResolver()).resolve(externalCorpus)
        def externalConvert = externalCorpus.members.find { it.ownerId.qualifiedName == "com.example.sdk.Foo" && it.name == "convert" }
        assertEquals(LinkRefKind.EXTERNAL, externalConvert.parameters[0].type.linkRef.kind)
        assertTrue(externalConvert.parameters[0].type.linkRef.externalUrl.endsWith("java/time/Instant.html"))

        LinkPathResolver pathResolver = new LinkPathResolver()
        assertEquals("../reference/com.example.Bar.html#run()", pathResolver.htmlUrl(
                "package/com.example.html",
                "reference/com.example.Bar.html",
                "run()"
        ))
        assertEquals("com.example.Bar.md#run-string-", pathResolver.markdownUrl(
                "reference/com.example.Foo.md",
                "reference/com.example.Bar.md",
                "run-string-"
        ))
        assertEquals("classes.html", pathResolver.relativeUrl("index.html", "classes.html"))
        assertEquals("#section", pathResolver.relativeUrl("", "", "section"))
    }

    private static DocCorpus sampleCorpus() {
        File sourceRoot = new File("src/test/resources/sample-sdk/src/main/java")
        return new JavadocApiParser().parseCorpus([sourceRoot.absolutePath], new ApiConfig())
    }
}

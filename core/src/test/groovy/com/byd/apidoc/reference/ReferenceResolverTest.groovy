package com.byd.apidoc.reference

import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.DocCorpus
import com.byd.apidoc.model.LinkRefKind
import com.byd.apidoc.parser.javadoc.JavadocApiParser
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class ReferenceResolverTest {

    @Test
    void resolvesInternalTypesExternalTypesAndInlineLinks() {
        DocCorpus corpus = sampleCorpus()

        new ReferenceResolver().resolve(corpus)

        def foo = corpus.types.find { it.qualifiedName == "com.example.sdk.Foo" }
        assertEquals(LinkRefKind.INTERNAL, foo.interfaces[0].linkRef.kind)
        assertEquals("com.example.sdk.ServiceContract", foo.interfaces[0].linkRef.targetId.qualifiedName)

        def convert = corpus.members.find { it.ownerId.qualifiedName == "com.example.sdk.Foo" && it.name == "convert" }
        assertEquals(LinkRefKind.EXTERNAL, convert.parameters[0].type.linkRef.kind)
        assertTrue(convert.parameters[0].type.linkRef.externalUrl.endsWith("java/time/Instant.html"))

        assertTrue(foo.comment.inlineNodes.any {
            it.name == "link" && it.reference.kind == LinkRefKind.INTERNAL && it.reference.targetId.qualifiedName == "com.example.sdk.Bar"
        })
        assertTrue(foo.comment.inlineNodes.any {
            it.name == "linkplain" && it.reference.kind == LinkRefKind.EXTERNAL && it.reference.externalUrl.endsWith("java/util/List.html")
        })
    }

    private static DocCorpus sampleCorpus() {
        File sourceRoot = new File("src/test/resources/sample-sdk/src/main/java")
        return new JavadocApiParser().parseCorpus([sourceRoot.absolutePath], new ApiConfig())
    }
}

package com.byd.apidoc.reference

import com.byd.apidoc.model.LinkRefKind
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class ExternalLinkResolverTest {

    @Test
    void resolvesBuiltInJdkAndroidAndAndroidxLinks() {
        ExternalLinkResolver resolver = new ExternalLinkResolver()

        def string = resolver.resolveType("java.lang.String")
        def context = resolver.resolveType("android.content.Context")
        def nonNull = resolver.resolveType("androidx.annotation.NonNull")

        assertEquals(LinkRefKind.EXTERNAL, string.kind)
        assertTrue(string.externalUrl.endsWith("java/lang/String.html"))
        assertEquals("String", string.label)

        assertEquals(LinkRefKind.EXTERNAL, context.kind)
        assertEquals("https://developer.android.com/reference/android/content/Context.html", context.externalUrl)

        assertEquals(LinkRefKind.EXTERNAL, nonNull.kind)
        assertEquals("https://developer.android.com/reference/androidx/annotation/NonNull.html", nonNull.externalUrl)
    }

    @Test
    void preservesUnresolvedFallbackForUnknownTypes() {
        def link = new ExternalLinkResolver().resolveType("com.example.LocalType")

        assertEquals(LinkRefKind.UNRESOLVED, link.kind)
        assertEquals("com.example.LocalType", link.rawTarget)
        assertEquals("LocalType", link.label)
        assertEquals("com.example.LocalType", link.fallbackText)
    }
}

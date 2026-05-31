package com.byd.apidoc.projection

import com.byd.apidoc.metadata.VisibilityPolicy
import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.DocCorpus
import com.byd.apidoc.parser.javadoc.JavadocApiParser
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

class SearchAndNavV2Test {

    @Test
    void navTreeGroupsPackagesAndTypesWithActivePathAndGroupMetadata() {
        DocProjection projection = projection()

        NavNode packageNode = projection.nav.find { it.label == "com.example.sdk" }
        assertNotNull(packageNode)
        assertEquals(NavNodeKind.PACKAGE, packageNode.kind)
        assertEquals("package", packageNode.group)
        assertEquals(["com.example.sdk"], packageNode.activePath)
        assertEquals(NavNodeKind.OVERVIEW, packageNode.children[0].kind)
        assertEquals("Overview", packageNode.children[0].label)

        NavNode classes = packageNode.children.find { it.label == "Classes" }
        assertNotNull(classes)
        assertEquals(NavNodeKind.GROUP, classes.kind)
        assertEquals("class", classes.group)
        assertEquals(["com.example.sdk", "Classes"], classes.activePath)

        NavNode foo = classes.children.find { it.label == "Foo" }
        assertNotNull(foo)
        assertEquals(NavNodeKind.TYPE, foo.kind)
        assertEquals("class", foo.group)
        assertEquals(["com.example.sdk", "Classes", "Foo"], foo.activePath)
        assertEquals("reference/com.example.sdk.Foo.html", foo.url)
    }

    @Test
    void searchEntriesDistinguishPackageTypeConstructorMethodFieldAndConstant() {
        DocProjection projection = projection()

        assertEquals(SearchEntryKind.PACKAGE, search(projection, "com.example.sdk").kind)
        assertEquals(SearchEntryKind.CLASS, search(projection, "Foo").kind)
        assertEquals(SearchEntryKind.INTERFACE, search(projection, "ServiceContract").kind)
        assertEquals(SearchEntryKind.ENUM, search(projection, "Mode").kind)
        assertEquals(SearchEntryKind.ANNOTATION, search(projection, "SdkAnnotation").kind)
        assertEquals(SearchEntryKind.RECORD, search(projection, "RecordType").kind)
        assertEquals(SearchEntryKind.EXCEPTION, search(projection, "SampleException").kind)
        assertEquals(SearchEntryKind.ERROR, search(projection, "SampleError").kind)
        assertEquals(SearchEntryKind.EXCEPTION, search(projection, "SampleThrowable").kind)
        assertEquals(SearchEntryKind.CONSTRUCTOR, memberSearch(projection, "Foo", "Foo").kind)
        assertEquals(SearchEntryKind.METHOD, memberSearch(projection, "Foo", "run").kind)
        assertEquals(SearchEntryKind.CONSTANT, memberSearch(projection, "Foo", "DEFAULT_NAME").kind)

        SearchEntry field = memberSearch(projection, "BaseService", "baseField")
        assertEquals(SearchEntryKind.FIELD, field.kind)
    }

    @Test
    void searchEntriesExposeOwnerPackageStatusSummaryUrlAnchorAndTokens() {
        DocProjection projection = projection()

        SearchEntry packageEntry = search(projection, "com.example.sdk")
        assertEquals("com.example.sdk", packageEntry.packageName)
        assertEquals("package/com.example.sdk.html", packageEntry.url)
        assertEquals("com.example.sdk", packageEntry.anchor)
        assertNotNull(packageEntry.status)
        assertTrue(packageEntry.summary.contains("Sample SDK package"))
        assertTrue(packageEntry.tokens.contains("sample"))
        assertTrue(packageEntry.tokens.contains("sdk"))

        SearchEntry run = memberSearch(projection, "Foo", "run")
        assertEquals("com.example.sdk.Foo", run.ownerName)
        assertEquals("com.example.sdk", run.packageName)
        assertNotNull(run.status)
        assertTrue(run.summary.contains("Runs the sample operation") || run.summary.contains("Runs a varargs overload"))
        assertTrue(run.url.startsWith("reference/com.example.sdk.Foo.html#"))
        assertEquals(run.url.substring(run.url.indexOf("#") + 1), run.anchor)
        assertTrue(run.displaySignature.contains("run("))
        assertTrue(run.tokens.contains("foo"))
        assertTrue(run.tokens.contains("method"))
        assertTrue(run.tokens.size() <= 32)
    }

    @Test
    void htmlSearchAssetKeepsSearchSelfContainedAndGraceful() {
        String script = getClass().classLoader.getResource("apidoc-v2/assets/apidoc-search.js").text

        assertFalse((script =~ /https?:\/\//).find())
        assertFalse(script.contains("jQuery"))
        assertFalse(script.contains("React"))
        assertFalse(script.contains("Vue"))
        assertTrue(script.contains("ad-search-kind"))
        assertTrue(script.contains("EXCEPTION"))
        assertTrue(script.contains("ERROR"))
        assertTrue(script.contains("apidoc.platform"))
        assertTrue(script.contains("matchesPlatform"))
        assertTrue(script.contains("catch(function"))
        assertTrue(script.contains("response.ok"))
        assertTrue(script.contains("aria-expanded"))
        assertTrue(script.contains("ArrowDown"))
        assertTrue(script.contains("panel.addEventListener(\"keydown\""))
        assertTrue(script.contains("input.addEventListener(\"keydown\""))
        assertTrue(script.contains("new URL(\"../search-index.json\""))
        assertTrue(script.contains("itemUrl.indexOf(\"#\") < 0"))
    }

    private static SearchEntry search(DocProjection projection, String label) {
        SearchEntry entry = projection.search.find { it.label == label }
        assertNotNull("Missing search entry for ${label}", entry)
        return entry
    }

    private static SearchEntry memberSearch(DocProjection projection, String ownerSimpleName, String label) {
        SearchEntry entry = projection.search.find { it.ownerName?.endsWith(".${ownerSimpleName}") && it.label == label }
        assertNotNull("Missing member search entry for ${ownerSimpleName}.${label}", entry)
        return entry
    }

    private static DocProjection projection() {
        new ProjectionBuilder().build(sampleCorpus(), new VisibilityPolicy())
    }

    private static DocCorpus sampleCorpus() {
        File sourceRoot = new File("src/test/resources/sample-sdk/src/main/java")
        return new JavadocApiParser().parseCorpus([sourceRoot.absolutePath], new ApiConfig())
    }
}

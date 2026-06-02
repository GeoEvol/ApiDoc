package com.byd.apidoc.render.html

import com.byd.apidoc.metadata.ApiMetadata
import com.byd.apidoc.projection.ApiStatusModel
import com.byd.apidoc.projection.SearchEntry
import com.byd.apidoc.projection.SearchEntryKind
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

class HtmlSearchIndexViewBuilderTest {

    @Test
    void preservesCanonicalSearchEntryFieldsAndAddsHtmlSearchViewFields() {
        ApiMetadata metadata = new ApiMetadata(since: "1.0")
        ApiStatusModel status = new ApiStatusModel(deprecated: true, since: "1.0")
        SearchEntry member = new SearchEntry(
                kind: SearchEntryKind.METHOD,
                label: "run",
                qualifiedName: "com.example.sdk.Foo.run",
                packageName: "com.example.sdk",
                ownerName: "com.example.sdk.Foo",
                url: "reference/com.example.sdk.Foo.html#run(java.lang.String)",
                anchor: "run(java.lang.String)",
                summary: "Runs the sample operation.",
                metadata: metadata,
                status: status,
                displaySignature: "public void run(String name)",
                platforms: ["DiLink300"],
                tokens: ["run", "foo", "method"]
        )

        Map view = new HtmlSearchIndexViewBuilder().build([member]).first()

        assertEquals(SearchEntryKind.METHOD, view.kind)
        assertEquals("run", view.label)
        assertEquals("com.example.sdk.Foo.run", view.qualifiedName)
        assertEquals("com.example.sdk", view.packageName)
        assertEquals("com.example.sdk.Foo", view.ownerName)
        assertEquals("reference/com.example.sdk.Foo.html#run(java.lang.String)", view.url)
        assertEquals("run(java.lang.String)", view.anchor)
        assertEquals("Runs the sample operation.", view.summary)
        assertEquals(metadata, view.metadata)
        assertEquals(status, view.status)
        assertEquals("public void run(String name)", view.displaySignature)
        assertEquals(["DiLink300"], view.platforms)
        assertEquals(["run", "foo", "method"], view.tokens)

        assertEquals("run", view.simpleName)
        assertEquals("com.example.sdk.Foo", view.ownerQualifiedName)
        assertEquals("Foo", view.ownerSimpleName)
        assertEquals("run", view.displayTitle)
        assertTrue(view.searchText.contains("run"))
        assertTrue(view.searchText.contains("com.example.sdk.Foo"))
        assertTrue(view.searchText.contains("Runs the sample operation."))
        assertTrue(view.searchText.contains("public void run(String name)"))
    }

    @Test
    void preservesEveryExistingSearchEntryKind() {
        List<SearchEntry> entries = SearchEntryKind.values().collect { SearchEntryKind kind ->
            new SearchEntry(
                    kind: kind,
                    label: kind.name(),
                    qualifiedName: "com.example.${kind.name()}",
                    packageName: "com.example",
                    url: "reference/${kind.name()}.html",
                    tokens: [kind.name().toLowerCase()]
            )
        }

        List<Map<String, Object>> view = new HtmlSearchIndexViewBuilder().build(entries)

        assertEquals(SearchEntryKind.values() as List, view*.kind)
        view.each { Map item ->
            assertNotNull(item.simpleName)
            assertTrue(item.containsKey("metadata"))
            assertTrue(item.containsKey("status"))
        }
    }
}

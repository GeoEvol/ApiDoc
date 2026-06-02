package com.byd.apidoc.render

import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class ApiDocSearchAssetTest {

    @Test
    void searchAssetDefinesPureScorerAndStableSortContract() {
        String script = getClass().classLoader.getResource("apidoc-v2/assets/apidoc-search.js").text

        assertTrue(script.contains("var KIND_WEIGHTS = {"))
        assertTrue(script.contains("PACKAGE: 120"))
        assertTrue(script.contains("CLASS: 110"))
        assertTrue(script.contains("INTERFACE: 110"))
        assertTrue(script.contains("ENUM: 108"))
        assertTrue(script.contains("ANNOTATION: 106"))
        assertTrue(script.contains("RECORD: 104"))
        assertTrue(script.contains("EXCEPTION: 102"))
        assertTrue(script.contains("ERROR: 102"))
        assertTrue(script.contains("CONSTRUCTOR: 90"))
        assertTrue(script.contains("METHOD: 80"))
        assertTrue(script.contains("CONSTANT: 70"))
        assertTrue(script.contains("FIELD: 60"))

        assertTrue(script.contains("function scoreItem(item, query)"))
        assertTrue(script.contains("function sortSearchResults(items, query)"))
        assertTrue(script.contains("function bestFieldScore"))
        assertTrue(script.contains("function isSignatureQuery(query)"))
        assertTrue(script.contains("if (isSignatureQuery(query)) {"))
        assertTrue(script.contains("window.__APIDOC_SEARCH_TESTING__"))

        [1200, 1100, 1050, 900, 850, 780, 740, 650, 620, 540, 520, 460, 420, 360, 300, 260, 220, 160].each { int score ->
            assertTrue("Missing scorer constant ${score}", script.contains("score: ${score}") || script.contains(" ${score}") || script.contains(", ${score}"))
        }
        assertTrue(script.contains("compareText(left.item.label, right.item.label)"))
        assertTrue(script.contains("compareText(left.item.ownerQualifiedName, right.item.ownerQualifiedName)"))
        assertTrue(script.contains("compareText(left.item.qualifiedName, right.item.qualifiedName)"))
        assertTrue(script.contains("compareText(left.item.displaySignature, right.item.displaySignature)"))
        assertTrue(script.contains("compareText(left.item.url, right.item.url)"))
        assertTrue(script.contains("}).join(\" "))
        assertTrue(script.contains("u00b7"))

        ["Algolia", "Pagefind", "MiniSearch", "Fuse", "Lunr", "FlexSearch", "jQuery", "React", "Vue"].each { String forbidden ->
            assertFalse("Search asset must not depend on ${forbidden}", script.contains(forbidden))
        }

        int platformFilter = script.indexOf("return matchesPlatform(item);")
        int sortCall = script.indexOf("sortSearchResults(items.filter")
        int sliceCall = script.indexOf(".slice(0, 30)")
        assertTrue("Search must pass platform-filtered items into sorting", sortCall >= 0 && platformFilter > sortCall)
        assertTrue("Search must slice after sorting", sliceCall > sortCall)
    }

    @Test
    void searchAssetKeepsResultsSimpleWithoutHighlightSnippetOrGrouping() {
        String script = getClass().classLoader.getResource("apidoc-v2/assets/apidoc-search.js").text

        assertFalse(script.contains("function matchRanges(value, query)"))
        assertFalse(script.contains("function renderHighlightedText(value, query)"))
        assertFalse(script.contains("function snippetFor(item, query)"))
        assertFalse(script.contains("function renderSnippet(item, query)"))
        assertFalse(script.contains("ad-search-highlight"))
        assertFalse(script.contains("ad-search-snippet"))
        assertFalse(script.contains("Best match"))
        assertFalse(script.contains("Fields & Constants"))
    }
}

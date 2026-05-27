package com.byd.apidoc.reference

import org.junit.Test

import static org.junit.Assert.assertEquals

class LinkPathResolverTest {

    @Test
    void resolvesRelativeHtmlAndMarkdownUrlsWithAnchors() {
        LinkPathResolver resolver = new LinkPathResolver()

        assertEquals("../reference/com.example.Bar.html#run()", resolver.htmlUrl(
                "package/com.example.html",
                "reference/com.example.Bar.html",
                "run()"
        ))
        assertEquals("com.example.Bar.md#run-string-", resolver.markdownUrl(
                "reference/com.example.Foo.md",
                "reference/com.example.Bar.md",
                "run-string-"
        ))
        assertEquals("classes.html", resolver.relativeUrl("index.html", "classes.html"))
        assertEquals("#section", resolver.relativeUrl("", "", "section"))
    }
}

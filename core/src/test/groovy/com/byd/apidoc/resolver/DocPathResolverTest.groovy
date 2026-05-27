package com.byd.apidoc.resolver

import com.byd.apidoc.model.ApiDoc
import org.junit.Test

import static org.junit.Assert.assertEquals

class DocPathResolverTest {

    @Test
    void testJavadocCompatiblePaths() {
        def resolver = new DocPathResolver()
        def doc = new ApiDoc(name: "UserService", packageName: "com.example")

        assertEquals("index.html", resolver.overviewPath())
        assertEquals("index-all.html", resolver.indexAllPath())
        assertEquals("search.html", resolver.searchPath())
        assertEquals("com/example/package-summary.html", resolver.packagePath("com.example"))
        assertEquals("com/example/UserService.html", resolver.classPath(doc))
    }

    @Test
    void testDefaultPackagePaths() {
        def resolver = new DocPathResolver()

        assertEquals("default/package-summary.html", resolver.packagePath(null))
        assertEquals("default/RootType.html", resolver.classPath("", "RootType"))
    }
}

package com.byd.apidoc.utils

import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.ApiDoc
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class TypeLinkerTest {

    @Test
    void rendersLinkAndLinkplainWithMemberReferences() {
        def current = new ApiDoc(name: "CurrentType", packageName: "com.example")
        def other = new ApiDoc(name: "OtherType", packageName: "com.example")
        def docPathMap = [
                (current): "com/example/CurrentType.html",
                (other)  : "com/example/OtherType.html"
        ]
        def linker = new TypeLinker([current, other], [] as Set, docPathMap, ApiConfig.FORMAT_HTML)

        String local = linker.linkifyDescription("See {@link #run(java.lang.String)}.", "com/example/CurrentType.html")
        String remote = linker.linkifyDescription("See {@link OtherType#run(java.lang.String, int)}.", "com/example/CurrentType.html")
        String plain = linker.linkifyDescription("See {@linkplain OtherType helper type}.", "com/example/CurrentType.html")

        assertTrue(local.contains('href="CurrentType.html#run(java.lang.String)"') || local.contains('href="#run(java.lang.String)"'))
        assertTrue(local.contains("<code>#run(java.lang.String)</code>"))
        assertTrue(remote.contains('href="OtherType.html#run(java.lang.String, int)"'))
        assertTrue(remote.contains("<code>OtherType#run(java.lang.String, int)</code>"))
        assertTrue(plain.contains('href="OtherType.html"'))
        assertTrue(plain.contains('>helper type</a>'))
    }

    @Test
    void preservesCodeAndLiteralSemantics() {
        def linker = new TypeLinker([], [] as Set, [:], ApiConfig.FORMAT_HTML)

        assertEquals('Value <code>List<String></code> text',
                linker.linkifyDescription('Value {@code List<String>} text', 'com/example/CurrentType.html'))
        assertEquals('Value List<String> text',
                linker.linkifyDescription('Value {@literal List<String>} text', 'com/example/CurrentType.html'))
    }
}

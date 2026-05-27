package com.byd.apidoc.parser.javadoc

import com.byd.apidoc.doclet.BuildContextRegistry
import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.ApiDoc
import com.byd.apidoc.model.DocCorpus
import org.junit.Test

import javax.tools.ToolProvider

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

class JavadocApiParserTest {

    @Test
    void parsesJavadocAnchorsForConstructorsOverloadsArraysAndVarargs() {
        File root = new File("build/test-javadoc-parser/anchors")
        recreateDir(root)
        File sourceDir = new File(root, "src/main/java/com/example")
        sourceDir.mkdirs()
        new File(sourceDir, "AnchorFixture.java").text = '''
package com.example;

public class AnchorFixture {
    public AnchorFixture() {}

    public String run(String id) { return id; }

    public String run(String[] ids) { return ids[0]; }

    public String join(String... parts) { return String.join(",", parts); }
}
'''

        int before = BuildContextRegistry.size()
        List<ApiDoc> docs = new JavadocApiParser().parse([new File(root, "src/main/java").absolutePath], new ApiConfig())

        assertEquals(before, BuildContextRegistry.size())
        assertEquals(1, docs.size())

        ApiDoc doc = docs[0]
        assertNotNull(doc)
        assertTrue(doc.constructors.any { it.anchorId == "AnchorFixture()" })
        assertTrue(doc.list.any { it.anchorId == "run(java.lang.String)" })
        assertTrue(doc.list.any { it.anchorId == "run(java.lang.String[])" })
        assertTrue(doc.list.any { it.anchorId == "join(java.lang.String[])" })
    }

    @Test
    void removesBuildContextAfterParseFailure() {
        File root = new File("build/test-javadoc-parser/failure")
        recreateDir(root)
        File sourceDir = new File(root, "src/main/java/com/example")
        sourceDir.mkdirs()
        new File(sourceDir, "Broken.java").text = '''
package com.example;

public class Broken {
    public void run( {
    }
}
'''

        int before = BuildContextRegistry.size()
        try {
            new JavadocApiParser().parse([new File(root, "src/main/java").absolutePath], new ApiConfig())
            fail("Expected Javadoc parsing to fail for invalid Java source")
        } catch (RuntimeException ignored) {
            // expected
        }
        assertEquals(before, BuildContextRegistry.size())
    }

    @Test
    void preservesNormalizedInlineTagsAndSeeReferences() {
        File root = new File("build/test-javadoc-parser/docs")
        recreateDir(root)
        File sourceDir = new File(root, "src/main/java/com/example")
        sourceDir.mkdirs()
        new File(sourceDir, "OtherType.java").text = '''
package com.example;

public class OtherType {
}
'''
        new File(sourceDir, "DocFixture.java").text = '''
package com.example;

/**
 * Uses {@code List<String>} with {@link OtherType helper type}.
 *
 * @since 1.2
 * @deprecated prefer {@link OtherType}
 * @see OtherType
 */
public class DocFixture {
    /**
     * Calls {@link OtherType}.
     *
     * @param value use {@code raw} input
     * @return {@code done}
     * @throws IllegalArgumentException if bad
     * @see OtherType#toString()
     */
    public String run(String value) {
        return value;
    }
}
'''

        List<ApiDoc> docs = new JavadocApiParser().parse([new File(root, "src/main/java").absolutePath], new ApiConfig())
        ApiDoc doc = docs.find { it.name == "DocFixture" }

        assertNotNull(doc)
        assertTrue(doc.description.contains("{@code List<String>}"))
        assertTrue(doc.description.contains("{@link OtherType helper type}"))
        assertTrue(doc.tagRefs*.tag.contains("since:1.2"))
        assertTrue(doc.tagRefs*.tag.contains("deprecated:prefer {@link OtherType}"))
        assertTrue(doc.tagRefs*.tag.contains("see:OtherType"))

        def method = doc.list.find { it.name == "run" }
        assertNotNull(method)
        assertTrue(method.description.contains("{@link OtherType}"))
        assertEquals("use {@code raw} input", method.parameters.find { it.name == "value" }.description)
        assertEquals("{@code done}", method.returnComment)
        assertEquals("if bad", method.exceptionComments["IllegalArgumentException"])
        assertTrue(method.tagRefs*.tag.contains("see:OtherType#toString()"))
    }

    @Test
    void resolvesExternalTypesFromDependencyClasspath() {
        File root = new File("build/test-javadoc-parser/dependency-classpath")
        recreateDir(root)

        File dependencySourceDir = new File(root, "dependency-src/com/example/dependency")
        dependencySourceDir.mkdirs()
        File dependencySource = new File(dependencySourceDir, "ExternalType.java")
        dependencySource.text = '''
package com.example.dependency;

public class ExternalType {
}
'''
        File dependencyClasses = new File(root, "dependency-classes")
        dependencyClasses.mkdirs()
        int compileResult = ToolProvider.systemJavaCompiler.run(
                null,
                null,
                null,
                "-d", dependencyClasses.absolutePath,
                dependencySource.absolutePath
        )
        assertEquals(0, compileResult)

        File sourceDir = new File(root, "src/main/java/com/example")
        sourceDir.mkdirs()
        new File(sourceDir, "UsesExternal.java").text = '''
package com.example;

import com.example.dependency.ExternalType;

/** Uses an external dependency type. */
public class UsesExternal {
    public ExternalType external() {
        return null;
    }
}
'''

        ApiConfig config = new ApiConfig()
        config.dependencyClasspath = [dependencyClasses.absolutePath]

        DocCorpus corpus = new JavadocApiParser().parseCorpus([new File(root, "src/main/java").absolutePath], config)

        assertTrue(corpus.types.any { it.qualifiedName == "com.example.UsesExternal" })
        assertTrue(corpus.members.any { it.returnType?.qualifiedName == "com.example.dependency.ExternalType" })
    }

    @Test
    void rejectsSourceVersionHigherThanRuntimeJdk() {
        File root = new File("build/test-javadoc-parser/source-version")
        recreateDir(root)
        File sourceDir = new File(root, "src/main/java/com/example")
        sourceDir.mkdirs()
        new File(sourceDir, "SourceVersionFixture.java").text = '''
package com.example;

public class SourceVersionFixture {
}
'''

        ApiConfig config = new ApiConfig()
        config.sourceVersion = String.valueOf(runtimeJavaMajor() + 1)

        try {
            new JavadocApiParser().parseCorpus([new File(root, "src/main/java").absolutePath], config)
            fail("Expected sourceVersion higher than runtime JDK to fail fast")
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.message.contains("sourceVersion"))
            assertTrue(expected.message.contains("higher than current JDK"))
        }
    }

    private static void recreateDir(File dir) {
        if (dir.exists()) {
            dir.deleteDir()
        }
        dir.mkdirs()
    }

    private static int runtimeJavaMajor() {
        String version = System.getProperty("java.version")
        if (version.startsWith("1.")) {
            return Integer.parseInt(version.substring(2, version.indexOf('.', 2)))
        }
        int dot = version.indexOf('.')
        return Integer.parseInt(dot == -1 ? version : version.substring(0, dot))
    }
}

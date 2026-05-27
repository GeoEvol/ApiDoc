package com.byd.apidoc.fixture

import org.junit.Test

import javax.tools.ToolProvider

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

class SampleSdkFixtureTest {

    @Test
    void sampleSdkCoversV1ModelEdgeCases() {
        File root = new File("src/test/resources/sample-sdk/src/main/java")
        assertTrue(root.exists())

        List<String> paths = []
        root.eachFileRecurse { File file ->
            if (file.name.endsWith(".java")) {
                paths.add(root.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/' as char))
            }
        }

        assertTrue(paths.contains("com/example/sdk/Foo.java"))
        assertTrue(paths.contains("com/example/sdk/ServiceContract.java"))
        assertTrue(paths.contains("com/example/sdk/Mode.java"))
        assertTrue(paths.contains("com/example/sdk/SdkAnnotation.java"))
        assertTrue(paths.contains("com/example/sdk/RecordType.java"))
        assertTrue(paths.contains("com/example/sdk/DeprecatedApi.java"))
        assertTrue(paths.contains("com/example/sdk/HiddenApi.java"))
        assertTrue(paths.contains("com/example/sdk/RemovedApi.java"))

        String foo = new File(root, "com/example/sdk/Foo.java").text
        assertTrue(foo.contains("@param <T>"))
        assertTrue(foo.contains("@apiSince 3"))
        assertTrue(foo.contains("@permission android.permission.INTERNET"))
        assertTrue(foo.contains("{@link Bar helper type}"))
        assertTrue(foo.contains("{@linkplain java.util.List plain list}"))
        assertTrue(foo.contains("{@code code literal}"))
        assertTrue(foo.contains("{@customInline custom value}"))
        assertTrue(foo.contains("List<? extends T>"))
        assertTrue(foo.contains("String... values"))

        assertTrue(new File(root, "com/example/sdk/DeprecatedApi.java").text.contains("@Deprecated"))
        assertTrue(new File(root, "com/example/sdk/DeprecatedApi.java").text.contains("@deprecated use {@link Foo} instead"))
        assertTrue(new File(root, "com/example/sdk/HiddenApi.java").text.contains("@hide internal only"))
        assertTrue(new File(root, "com/example/sdk/RemovedApi.java").text.contains("@removed no longer available"))
    }

    @Test
    void sampleSdkSourcesCompileAsJava17() {
        File root = new File("src/test/resources/sample-sdk/src/main/java")
        File output = new File("build/test-sample-sdk-classes")
        if (output.exists()) {
            output.deleteDir()
        }
        output.mkdirs()

        def compiler = ToolProvider.systemJavaCompiler
        assertNotNull("Run tests with a JDK, not a JRE", compiler)

        List<String> args = ["-d", output.absolutePath, "-source", "17", "-target", "17"]
        root.eachFileRecurse { File file ->
            if (file.name.endsWith(".java")) {
                args.add(file.absolutePath)
            }
        }

        int result = compiler.run(null, null, null, args as String[])

        assertEquals(0, result)
    }
}

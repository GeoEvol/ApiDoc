package com.byd.apidoc.parser.javadoc

import com.byd.apidoc.comment.BlockTagKind
import com.byd.apidoc.comment.InlineTagKind
import com.byd.apidoc.metadata.ApiAvailability
import com.byd.apidoc.metadata.ApiVisibility
import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.DocCorpus
import com.byd.apidoc.model.DocMemberKind
import com.byd.apidoc.model.DocType
import com.byd.apidoc.model.DocTypeKind
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

class V1DocCorpusParserTest {

    @Test
    void parsesSampleSdkIntoV1DocCorpus() {
        File sourceRoot = new File("src/test/resources/sample-sdk/src/main/java")

        DocCorpus corpus = new JavadocApiParser().parseCorpus([sourceRoot.absolutePath], new ApiConfig())

        assertEquals("v1", corpus.schemaVersion)
        assertTrue(corpus.packages*.name.contains("com.example.sdk"))
        assertTrue(corpus.packages*.name.contains("com.example.sdk.annotations"))

        DocType foo = corpus.types.find { it.qualifiedName == "com.example.sdk.Foo" }
        DocType contract = corpus.types.find { it.qualifiedName == "com.example.sdk.ServiceContract" }
        DocType mode = corpus.types.find { it.qualifiedName == "com.example.sdk.Mode" }
        DocType recordType = corpus.types.find { it.qualifiedName == "com.example.sdk.RecordType" }
        DocType annotationType = corpus.types.find { it.qualifiedName == "com.example.sdk.SdkAnnotation" }
        DocType sampleException = corpus.types.find { it.qualifiedName == "com.example.sdk.SampleException" }
        DocType sampleError = corpus.types.find { it.qualifiedName == "com.example.sdk.SampleError" }
        DocType sampleThrowable = corpus.types.find { it.qualifiedName == "com.example.sdk.SampleThrowable" }
        DocType hidden = corpus.types.find { it.qualifiedName == "com.example.sdk.HiddenApi" }
        DocType removed = corpus.types.find { it.qualifiedName == "com.example.sdk.RemovedApi" }
        DocType deprecated = corpus.types.find { it.qualifiedName == "com.example.sdk.DeprecatedApi" }

        assertNotNull(foo)
        assertEquals("type:com.example.sdk.Foo", foo.id.canonicalId)
        assertEquals("Foo", foo.id.displayId)
        assertEquals("com.example.sdk.Foo", foo.id.anchorId)
        assertNotNull(contract)
        assertNotNull(mode)
        assertNotNull(recordType)
        assertNotNull(annotationType)
        assertNotNull(sampleException)
        assertNotNull(sampleError)
        assertNotNull(sampleThrowable)
        assertNotNull(hidden)
        assertNotNull(removed)
        assertNotNull(deprecated)

        assertEquals(DocTypeKind.CLASS, foo.kind)
        assertEquals(DocTypeKind.INTERFACE, contract.kind)
        assertEquals(DocTypeKind.ENUM, mode.kind)
        assertEquals(DocTypeKind.RECORD, recordType.kind)
        assertEquals(DocTypeKind.ANNOTATION, annotationType.kind)
        assertEquals(DocTypeKind.EXCEPTION, sampleException.kind)
        assertEquals(DocTypeKind.ERROR, sampleError.kind)
        assertEquals(DocTypeKind.EXCEPTION, sampleThrowable.kind)

        assertEquals("T", foo.typeParameters[0].name)
        assertEquals("com.example.sdk.Bar", foo.typeParameters[0].bounds[0].qualifiedName)
        assertEquals("com.example.sdk.ServiceContract", foo.interfaces[0].qualifiedName)

        assertTrue(foo.comment.summaryNodes[0].text.contains("Main sample API"))
        assertTrue(foo.comment.blockTags.any { it.kind == BlockTagKind.PARAM && it.key == "T" })
        assertTrue(foo.comment.blockTags.any { it.name == "apiSince" && it.rawText == "3" })
        assertTrue(foo.comment.blockTags.any { it.name == "permission" })
        assertTrue(foo.comment.inlineNodes.any { it.kind == InlineTagKind.LINK && it.reference.fallbackText == "Bar" })
        assertTrue(foo.comment.inlineNodes.any { it.kind == InlineTagKind.LINKPLAIN && it.reference.fallbackText == "java.util.List" })
        assertTrue(foo.comment.inlineNodes.any { it.kind == InlineTagKind.CODE && it.body == "code literal" })
        assertTrue(foo.comment.inlineNodes.any { it.kind == InlineTagKind.CUSTOM && it.name == "customInline" })

        assertEquals("1.0", foo.metadata.since)
        assertEquals(3, foo.metadata.apiLevel.intValue())
        assertTrue(foo.metadata.sourceTags.contains("apiSince"))
        assertTrue(foo.metadata.sourceTags.contains("permission"))

        assertEquals(ApiVisibility.HIDDEN, hidden.metadata.visibility)
        assertTrue(hidden.metadata.sourceTags.contains("hide"))
        assertEquals(ApiAvailability.REMOVED, removed.metadata.availability)
        assertEquals("no longer available", removed.metadata.removed.message)
        assertEquals(ApiAvailability.DEPRECATED, deprecated.metadata.availability)
        assertTrue(deprecated.metadata.deprecated.fromJavadocTag)
        assertTrue(deprecated.metadata.deprecated.fromAnnotation)
        assertEquals("use {@link Foo} instead", deprecated.metadata.deprecated.message)
        assertTrue(deprecated.metadata.sourceAnnotations.contains("java.lang.Deprecated"))

        def runMethods = corpus.members.findAll {
            it.ownerId.qualifiedName == "com.example.sdk.Foo" && it.name == "run" && it.kind == DocMemberKind.METHOD
        }
        assertEquals(2, runMethods.size())
        assertTrue(runMethods.any { it.parameters*.name == ["value", "items"] && it.throwsTypes[0].qualifiedName == "java.lang.IllegalArgumentException" })
        assertTrue(runMethods.any { it.parameters[0].varargs })
        assertTrue(runMethods.every { it.id.canonicalId?.startsWith("method:com.example.sdk.Foo#run(") })
        assertTrue(runMethods.every { it.id.displayId?.startsWith("run(") })
        assertTrue(runMethods.every { it.id.anchorId == it.id.fragment })
        assertTrue(runMethods.any { method ->
            method.annotations.any { annotation ->
                annotation.qualifiedName == "com.example.sdk.annotations.RequiresPermission" &&
                        annotation.values.keySet().containsAll(["value", "allOf", "anyOf"]) &&
                        annotation.values.allOf == [] &&
                        annotation.values.anyOf == []
            }
        })

        def constructor = corpus.members.find {
            it.ownerId.qualifiedName == "com.example.sdk.Foo" && it.name == "Foo" && it.kind == DocMemberKind.CONSTRUCTOR
        }
        assertNotNull(constructor)
        assertTrue(constructor.annotations.any { annotation ->
            annotation.qualifiedName == "com.byd.dilink.anotation.Supported" &&
                    annotation.values.containsKey("platforms") &&
                    annotation.values.platforms == []
        })
        assertTrue(constructor.metadata.supportedPlatforms.isEmpty())
        assertFalse(constructor.metadata.metadataSources.any { it.name == "Supported" && it.property == "platforms" })

        def convert = corpus.members.find {
            it.ownerId.qualifiedName == "com.example.sdk.Foo" && it.name == "convert"
        }
        assertNotNull(convert)
        assertEquals("R", convert.typeParameters[0].name)
        assertEquals("java.time.Instant", convert.parameters[0].type.qualifiedName)

        assertFalse(corpus.members.findAll { it.kind == DocMemberKind.ENUM_CONSTANT && it.ownerId.qualifiedName == "com.example.sdk.Mode" }.isEmpty())
        assertFalse(corpus.members.findAll { it.kind == DocMemberKind.RECORD_COMPONENT && it.ownerId.qualifiedName == "com.example.sdk.RecordType" }.isEmpty())
        assertFalse(corpus.members.findAll { it.kind == DocMemberKind.ANNOTATION_ELEMENT && it.ownerId.qualifiedName == "com.example.sdk.SdkAnnotation" }.isEmpty())
    }
}

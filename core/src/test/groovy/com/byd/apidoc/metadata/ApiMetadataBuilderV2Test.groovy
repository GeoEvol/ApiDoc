package com.byd.apidoc.metadata

import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.DocCorpus
import com.byd.apidoc.model.DocMember
import com.byd.apidoc.model.DocType
import com.byd.apidoc.parser.javadoc.JavadocApiParser
import com.byd.apidoc.projection.DocProjection
import com.byd.apidoc.projection.ProjectionBuilder
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

class ApiMetadataBuilderV2Test {

    @Test
    void extractsAndroidStyleJavadocMetadataAndPreservesSourceTags() {
        DocType type = androidMetadataType()

        assertTrue(type.metadata.pending)
        assertEquals("1.5", type.metadata.since)
        assertEquals(12, type.metadata.apiLevel)
        assertEquals("R Extensions 4", type.metadata.sdkExtensionSince)
        assertEquals(ApiAvailability.REMOVED, type.metadata.availability)
        assertEquals("14", type.metadata.removedSince)
        assertEquals("removed after replacement shipped", type.metadata.removed.message)
        assertEquals("13", type.metadata.deprecatedSince)
        assertEquals("use Foo instead", type.metadata.deprecated.message)
        assertTrue(type.metadata.deprecated.fromJavadocTag)
        assertTrue(type.metadata.deprecated.fromAnnotation)
        assertTrue(type.comment.blockTags.any { it.name == "pending" })
        assertTrue(type.comment.blockTags.any { it.name == "apiSince" && it.rawText == "12" })
        assertTrue(type.comment.blockTags.any { it.name == "sdkExtSince" && it.rawText == "R Extensions 4" })
        assertTrue(type.comment.blockTags.any { it.name == "deprecated" && it.rawText == "use Foo instead" })
        assertTrue(type.comment.blockTags.any { it.name == "removed" && it.rawText == "removed after replacement shipped" })
        assertTrue(type.metadata.sourceTags.containsAll([
                "pending",
                "apiSince",
                "sdkExtSince",
                "removed",
                "removedSince",
                "deprecated",
                "deprecatedSince"
        ]))
        assertTrue(type.metadata.sourceAnnotations.contains("java.lang.Deprecated"))
    }

    @Test
    void extractsAndroidStyleAnnotationMetadataAndPreservesSourceAnnotations() {
        DocMember method = androidMetadataMember("guardedValue")

        assertEquals(["sample.permission.READ", "sample.permission.WRITE"] as LinkedHashSet, method.metadata.permissions)
        assertEquals("NONNULL", method.metadata.nullability)
        assertEquals(1, method.metadata.valueRanges.size())
        assertEquals("IntRange", method.metadata.valueRanges[0].kind)
        assertEquals(1L, method.metadata.valueRanges[0].from)
        assertEquals(10L, method.metadata.valueRanges[0].to)
        assertTrue(method.metadata.sourceAnnotations.contains("com.example.sdk.annotations.RequiresPermission"))
        assertTrue(method.metadata.sourceAnnotations.contains("com.example.sdk.annotations.IntRange"))
        assertTrue(method.metadata.sourceAnnotations.contains("com.example.sdk.annotations.NonNull"))

        DocMember nullableMethod = androidMetadataMember("nullableRatio")
        assertEquals("NULLABLE", nullableMethod.metadata.nullability)
        assertEquals(1, nullableMethod.metadata.valueRanges.size())
        assertEquals("FloatRange", nullableMethod.metadata.valueRanges[0].kind)
        assertEquals(0.0d, (Double) nullableMethod.metadata.valueRanges[0].from, 0.0001d)
        assertEquals(1.0d, (Double) nullableMethod.metadata.valueRanges[0].to, 0.0001d)
        assertTrue(nullableMethod.metadata.sourceAnnotations.contains("com.example.sdk.annotations.Nullable"))
        assertTrue(nullableMethod.metadata.sourceAnnotations.contains("com.example.sdk.annotations.FloatRange"))

        DocMember allRequired = androidMetadataMember("allRequired")
        assertEquals(["sample.permission.CAMERA", "sample.permission.LOCATION"] as LinkedHashSet, allRequired.metadata.permissions)

        DocMember anyRequired = androidMetadataMember("anyRequired")
        assertEquals(["sample.permission.BLUETOOTH", "sample.permission.NFC"] as LinkedHashSet, anyRequired.metadata.permissions)
    }

    @Test
    void projectionStatusExposesExpandedMetadataWithoutRendererCoupling() {
        DocCorpus corpus = sampleCorpus()

        DocProjection projection = new ProjectionBuilder().build(corpus, new VisibilityPolicy(includeRemoved: true))

        def page = projection.typePages.find { it.id.qualifiedName == "com.example.sdk.AndroidMetadataApi" }
        assertNotNull(page)
        assertTrue(page.apiStatus.pending)
        assertEquals("R Extensions 4", page.apiStatus.sdkExtensionSince)
        assertEquals("13", page.apiStatus.deprecatedSince)
        assertEquals("14", page.apiStatus.removedSince)

        def searchEntry = projection.search.find { it.ownerName == "com.example.sdk.AndroidMetadataApi" && it.label == "guardedValue" }
        assertNotNull(searchEntry)
        assertEquals(["sample.permission.READ", "sample.permission.WRITE"] as LinkedHashSet, searchEntry.status.permissions)
        assertEquals("NONNULL", searchEntry.status.nullability)
        assertFalse(searchEntry.status.valueRanges.isEmpty())

        searchEntry.status.permissions.add("renderer.mutation")
        searchEntry.status.valueRanges[0].from = -1L

        DocMember sourceMember = androidMetadataMember("guardedValue")
        assertEquals(["sample.permission.READ", "sample.permission.WRITE"] as LinkedHashSet, sourceMember.metadata.permissions)
        assertEquals(1L, sourceMember.metadata.valueRanges[0].from)
    }

    private static DocType androidMetadataType() {
        DocType type = sampleCorpus().types.find { it.qualifiedName == "com.example.sdk.AndroidMetadataApi" }
        assertNotNull(type)
        return type
    }

    private static DocMember androidMetadataMember(String name) {
        DocMember member = sampleCorpus().members.find {
            it.ownerId.qualifiedName == "com.example.sdk.AndroidMetadataApi" && it.name == name
        }
        assertNotNull(member)
        return member
    }

    private static DocCorpus sampleCorpus() {
        File sourceRoot = new File("src/test/resources/sample-sdk/src/main/java")
        return new JavadocApiParser().parseCorpus([sourceRoot.absolutePath], new ApiConfig())
    }
}

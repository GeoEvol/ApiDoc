package com.byd.apidoc.projection

import com.byd.apidoc.metadata.VisibilityPolicy
import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.DocCorpus
import com.byd.apidoc.parser.javadoc.JavadocApiParser
import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

class ProjectionBuilderTest {

    @Test
    void buildsAndroidStyleProjectionFromSampleSdkAndFiltersHiddenRemovedByPolicy() {
        DocCorpus corpus = sampleCorpus()

        DocProjection projection = new ProjectionBuilder().build(corpus, new VisibilityPolicy())

        assertTrue(corpus.types.any { it.qualifiedName == "com.example.sdk.HiddenApi" })
        assertTrue(corpus.types.any { it.qualifiedName == "com.example.sdk.RemovedApi" })

        assertTrue(projection.pages.any { it.kind == PageKind.INDEX && it.url == "index.html" })
        assertTrue(projection.pages.any { it.kind == PageKind.PACKAGES && it.url == "packages.html" })
        assertTrue(projection.pages.any { it.kind == PageKind.CLASSES && it.url == "classes.html" })
        assertTrue(projection.pages.any { it.kind == PageKind.PACKAGE && it.title == "com.example.sdk" })
        assertTrue(projection.pages.any { it.kind == PageKind.TYPE && it.targetId.qualifiedName == "com.example.sdk.Foo" })
        assertTrue(projection.typePages.any { it.id.qualifiedName == "com.example.sdk.Foo" && it.declaration.contains("class Foo<T>") })

        assertFalse(projection.pages.any { it.targetId?.qualifiedName == "com.example.sdk.HiddenApi" })
        assertFalse(projection.pages.any { it.targetId?.qualifiedName == "com.example.sdk.RemovedApi" })
        assertFalse(projection.search.any { it.qualifiedName == "com.example.sdk.HiddenApi" })
        assertFalse(projection.search.any { it.qualifiedName == "com.example.sdk.RemovedApi" })

        def sdkPackage = projection.nav.find { it.label == "com.example.sdk" }
        assertNotNull(sdkPackage)
        assertTrue(sdkPackage.children.any { it.label == "Classes" })
        assertTrue(sdkPackage.children.any { it.label == "Interfaces" })
        assertTrue(sdkPackage.children.any { it.label == "Enums" })
        assertTrue(sdkPackage.children.any { it.label == "Annotations" })
        assertTrue(sdkPackage.children.any { it.label == "Records" })

        assertTrue(projection.search.any { it.kind == SearchEntryKind.CLASS && it.qualifiedName == "com.example.sdk.Foo" })
        assertTrue(projection.search.any { it.kind == SearchEntryKind.INTERFACE && it.qualifiedName == "com.example.sdk.ServiceContract" })
        assertTrue(projection.search.any { it.kind == SearchEntryKind.ENUM && it.qualifiedName == "com.example.sdk.Mode" })
        assertTrue(projection.search.any { it.kind == SearchEntryKind.ANNOTATION && it.qualifiedName == "com.example.sdk.SdkAnnotation" })
        assertTrue(projection.search.any { it.kind == SearchEntryKind.RECORD && it.qualifiedName == "com.example.sdk.RecordType" })
        assertTrue(projection.search.any { it.kind == SearchEntryKind.METHOD && it.ownerName == "com.example.sdk.Foo" && it.label == "run" })
        assertTrue(projection.search.any { it.kind == SearchEntryKind.CONSTANT && it.ownerName == "com.example.sdk.Foo" && it.label == "DEFAULT_NAME" })

        def fooPage = projection.typePages.find { it.id.qualifiedName == "com.example.sdk.Foo" }
        assertTrue(fooPage.memberGroups.any { it.title == "Constants" })
        assertTrue(fooPage.memberGroups.any { it.title == "Constructors" })
        assertTrue(fooPage.memberGroups.any { it.title == "Methods" })
        assertTrue(fooPage.memberDetails.any { it.displayName.startsWith("run(") })
        assertTrue(fooPage.inheritedMemberGroups.isEmpty())
    }

    @Test
    void internalPolicyCanProjectHiddenAndRemovedTypesWithoutChangingCorpus() {
        DocCorpus corpus = sampleCorpus()
        VisibilityPolicy policy = new VisibilityPolicy(includeHidden: true, includeRemoved: true)

        DocProjection projection = new ProjectionBuilder().build(corpus, policy)

        assertTrue(projection.pages.any { it.targetId?.qualifiedName == "com.example.sdk.HiddenApi" })
        assertTrue(projection.pages.any { it.targetId?.qualifiedName == "com.example.sdk.RemovedApi" })
        assertTrue(projection.search.any { it.qualifiedName == "com.example.sdk.HiddenApi" })
        assertTrue(projection.search.any { it.qualifiedName == "com.example.sdk.RemovedApi" })
        assertTrue(corpus.types.any { it.qualifiedName == "com.example.sdk.HiddenApi" })
        assertTrue(corpus.types.any { it.qualifiedName == "com.example.sdk.RemovedApi" })
    }

    private static DocCorpus sampleCorpus() {
        File sourceRoot = new File("src/test/resources/sample-sdk/src/main/java")
        return new JavadocApiParser().parseCorpus([sourceRoot.absolutePath], new ApiConfig())
    }
}

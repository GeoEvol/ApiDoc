package com.byd.apidoc.projection

import com.byd.apidoc.metadata.ApiMetadata
import com.byd.apidoc.metadata.VisibilityPolicy
import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.DocCorpus
import com.byd.apidoc.model.DocId
import com.byd.apidoc.model.DocIdKind
import com.byd.apidoc.model.DocMember
import com.byd.apidoc.model.DocMemberKind
import com.byd.apidoc.model.DocPackage
import com.byd.apidoc.model.DocType
import com.byd.apidoc.model.DocTypeKind
import com.byd.apidoc.parser.javadoc.JavadocApiParser
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

class ProjectionBuilderV2Test {

    @Test
    void typePageModelHasBreadcrumbs() {
        TypePageModel fooPage = fooPage()

        assertEquals(["Packages", "com.example.sdk", "Foo"], fooPage.breadcrumbs*.label)
        assertEquals(["packages.html", "package/com.example.sdk.html", "reference/com.example.sdk.Foo.html"], fooPage.breadcrumbs*.url)
        assertEquals(fooPage.id, fooPage.breadcrumbs.last().targetId)
    }

    @Test
    void typePageModelHasRightTocEntries() {
        TypePageModel fooPage = fooPage()

        assertEquals(["Summary", "Constants", "Constructors", "Methods", "Details"], fooPage.rightToc*.label)
        assertEquals(["summary", "constants", "constructors", "methods", "details"], fooPage.rightToc*.anchor)
    }

    @Test
    void typePageModelHasAndroidStyleMemberGroupsInStableOrder() {
        TypePageModel fooPage = fooPage()

        assertEquals(["Constants", "Constructors", "Methods"], fooPage.memberGroups*.title)
        assertEquals(["CONSTANTS", "CONSTRUCTORS", "METHODS"], fooPage.memberGroups*.kind)
        assertTrue(fooPage.memberGroups.find { it.title == "Constants" }.members*.name.contains("DEFAULT_NAME"))
        assertTrue(fooPage.memberGroups.find { it.title == "Methods" }.members*.name.contains("run"))
    }

    @Test
    void typePageModelExposesTypeKindForRendererAndProjectionJson() {
        DocProjection projection = new ProjectionBuilder().build(sampleCorpus(), new VisibilityPolicy())

        assertEquals(DocTypeKind.CLASS, projection.typePages.find { it.id.qualifiedName == "com.example.sdk.Foo" }.typeKind)
        assertEquals(DocTypeKind.EXCEPTION, projection.typePages.find { it.id.qualifiedName == "com.example.sdk.SampleException" }.typeKind)
        assertEquals(DocTypeKind.ERROR, projection.typePages.find { it.id.qualifiedName == "com.example.sdk.SampleError" }.typeKind)
        assertEquals(DocTypeKind.EXCEPTION, projection.typePages.find { it.id.qualifiedName == "com.example.sdk.SampleThrowable" }.typeKind)
    }

    @Test
    void projectionPropagatesSupportedPlatformsAndMemberInheritance() {
        DocProjection projection = new ProjectionBuilder().build(sampleCorpus(), new VisibilityPolicy())
        TypePageModel fooPage = projection.typePages.find { it.id.qualifiedName == "com.example.sdk.Foo" }
        assertNotNull(fooPage)

        assertEquals(["DiLink300", "DiLink300F"], fooPage.platforms)
        assertEquals(["all", "DiLink300", "DiLink300F", "DiLink300VCP", "DiLinkF_300VCP"], [projection.platformFilter.defaultValue] + projection.platformFilter.platforms)

        MemberDetailModel constructor = fooPage.memberDetails.find { it.name == "Foo" }
        assertNotNull(constructor)
        assertEquals(["DiLink300", "DiLink300F"], constructor.platforms)

        MemberDetailModel constant = fooPage.memberDetails.find { it.name == "DEFAULT_NAME" }
        assertNotNull(constant)
        assertEquals(["DiLink300VCP"], constant.platforms)

        MemberDetailModel method = fooPage.memberDetails.find { it.name == "run" && it.parameters?.size() == 2 }
        assertNotNull(method)
        assertEquals(["DiLinkF_300VCP"], method.platforms)

        MemberSummaryModel methodSummary = fooPage.memberGroups.find { it.title == "Methods" }.members.find {
            it.name == "run" && it.displayName?.contains("String")
        }
        assertNotNull(methodSummary)
        assertEquals(["DiLinkF_300VCP"], methodSummary.platforms)

        SearchEntry typeSearch = projection.search.find { it.qualifiedName == "com.example.sdk.Foo" }
        assertNotNull(typeSearch)
        assertEquals(["DiLink300", "DiLink300F"], typeSearch.platforms)

        SearchEntry memberSearch = projection.search.find {
            it.ownerName == "com.example.sdk.Foo" && it.label == "DEFAULT_NAME"
        }
        assertNotNull(memberSearch)
        assertEquals(["DiLink300VCP"], memberSearch.platforms)
    }

    @Test
    void packagePagesAndNavUseOverviewAndSpecificTypeGroups() {
        DocProjection projection = new ProjectionBuilder().build(sampleCorpus(), new VisibilityPolicy())

        PackagePageModel packagePage = projection.packagePages.find { it.packageName == "com.example.sdk" }
        assertNotNull(packagePage)
        assertEquals("package/com.example.sdk.html", packagePage.url)
        assertTrue(packagePage.platforms.containsAll(["DiLink300", "DiLink300F", "DiLink300VCP", "DiLinkF_300VCP"]))
        assertTrue(packagePage.typeGroups*.label.containsAll(["Interfaces", "Classes", "Enums", "Annotations", "Exceptions", "Errors", "Records"]))
        assertTrue(packagePage.typeGroups.find { it.label == "Exceptions" }.types*.title.contains("SampleException"))
        assertTrue(packagePage.typeGroups.find { it.label == "Errors" }.types*.title.contains("SampleError"))
        assertFalse(packagePage.typeGroups.find { it.label == "Classes" }.types*.title.contains("SampleException"))
        assertFalse(packagePage.typeGroups.find { it.label == "Classes" }.types*.title.contains("SampleError"))

        NavNode packageNode = projection.nav.find { it.label == "com.example.sdk" }
        assertNotNull(packageNode)
        assertEquals(NavNodeKind.OVERVIEW, packageNode.children[0].kind)
        assertEquals("Overview", packageNode.children[0].label)
        assertEquals("package/com.example.sdk.html", packageNode.children[0].url)
        assertTrue(packageNode.platforms.contains("DiLink300"))

        NavNode classes = packageNode.children.find { it.label == "Classes" }
        NavNode exceptions = packageNode.children.find { it.label == "Exceptions" }
        NavNode errors = packageNode.children.find { it.label == "Errors" }
        assertNotNull(classes)
        assertNotNull(exceptions)
        assertNotNull(errors)
        assertFalse(classes.children*.label.contains("SampleException"))
        assertFalse(classes.children*.label.contains("SampleError"))
        assertTrue(exceptions.children*.label.contains("SampleException"))
        assertTrue(errors.children*.label.contains("SampleError"))
    }

    @Test
    void typePageModelExposesInheritedMemberGroupsAsRealList() {
        TypePageModel fooPage = fooPage()

        assertNotNull(fooPage.inheritedMemberGroups)
        assertTrue(fooPage.inheritedMemberGroups instanceof List)
        assertTrue(fooPage.inheritedMemberGroups.isEmpty())
        assertEquals(InheritedMemberGroupModel, TypePageModel.declaredFields.find { it.name == "inheritedMemberGroups" }.genericType.actualTypeArguments[0])
    }

    @Test
    void typePageModelHasApiStatusAndRenderNeutralTypeHeader() {
        TypePageModel fooPage = fooPage()

        assertNotNull(fooPage.apiStatus)
        assertEquals(false, fooPage.apiStatus.hidden)
        assertEquals(false, fooPage.apiStatus.removed)
        assertEquals(false, fooPage.apiStatus.deprecated)
        assertEquals("1.0", fooPage.apiStatus.since)
        assertEquals(3, fooPage.apiStatus.apiSince)

        assertNotNull(fooPage.typeHeader)
        assertEquals("Foo", fooPage.typeHeader.title)
        assertEquals("com.example.sdk", fooPage.typeHeader.packageName)
        assertTrue(fooPage.typeHeader.declaration.contains("class Foo<T>"))
        assertEquals("java.lang.Object", fooPage.typeHeader.inheritance.displayName)
        assertEquals(["com.example.sdk.ServiceContract"], fooPage.typeHeader.interfaces*.displayName)
    }

    @Test
    void searchEntryIncludesStatusMetadataAndAnchor() {
        DocProjection projection = new ProjectionBuilder().build(sampleCorpus(), new VisibilityPolicy(includeHidden: true, includeRemoved: true))

        SearchEntry hiddenType = projection.search.find { it.qualifiedName == "com.example.sdk.HiddenApi" }
        assertNotNull(hiddenType)
        assertNotNull(hiddenType.status)
        assertEquals(true, hiddenType.status.hidden)
        assertEquals(false, hiddenType.status.removed)
        assertEquals("com.example.sdk.HiddenApi", hiddenType.anchor)

        SearchEntry removedType = projection.search.find { it.qualifiedName == "com.example.sdk.RemovedApi" }
        assertNotNull(removedType)
        assertEquals(true, removedType.status.removed)

        SearchEntry runMethod = projection.search.find { it.ownerName == "com.example.sdk.Foo" && it.label == "run" }
        assertNotNull(runMethod)
        assertNotNull(runMethod.status)
        assertTrue(runMethod.anchor != null && !runMethod.anchor.isEmpty())
        assertTrue(runMethod.url.endsWith("#${runMethod.anchor}"))
        assertTrue(runMethod.displaySignature.contains("run("))
        assertTrue(runMethod.tokens.contains("run"))
    }

    @Test
    void sparseCorpusDefaultsNullCollectionsToEmptyProjectionInputs() {
        DocCorpus sparse = new DocCorpus(packages: null, types: null, members: null)

        DocProjection projection = new ProjectionBuilder().build(sparse, new VisibilityPolicy())

        assertEquals([PageKind.INDEX, PageKind.PACKAGES, PageKind.CLASSES], projection.pages*.kind)
        assertTrue(projection.typePages.isEmpty())
        assertTrue(projection.nav.isEmpty())
        assertTrue(projection.search.isEmpty())
    }

    @Test
    void visibleMembersAreComputedOncePerTypeForTypePagesAndSearch() {
        ApiMetadata typeMetadata = new ApiMetadata()
        ApiMetadata memberMetadata = new ApiMetadata()
        DocId typeId = new DocId(kind: DocIdKind.TYPE, qualifiedName: "com.example.Sparse")
        DocId memberId = new DocId(kind: DocIdKind.METHOD, qualifiedName: "com.example.Sparse.run", signature: "run()", fragment: "run()")
        DocCorpus corpus = new DocCorpus(
                packages: [new DocPackage(id: new DocId(kind: DocIdKind.PACKAGE, qualifiedName: "com.example"), name: "com.example", typeIds: [typeId])],
                types: [new DocType(id: typeId, name: "Sparse", qualifiedName: "com.example.Sparse", packageName: "com.example", kind: DocTypeKind.CLASS, metadata: typeMetadata, memberIds: [memberId])],
                members: [new DocMember(id: memberId, ownerId: typeId, name: "run", qualifiedName: "com.example.Sparse.run", kind: DocMemberKind.METHOD, metadata: memberMetadata)]
        )
        CountingVisibilityPolicy policy = new CountingVisibilityPolicy(memberMetadata)

        DocProjection projection = new ProjectionBuilder().build(corpus, policy)

        assertEquals(1, projection.typePages[0].memberDetails.size())
        assertEquals(1, projection.search.count { it.ownerName == "com.example.Sparse" && it.label == "run" })
        assertEquals(1, policy.memberChecks)
    }

    private static TypePageModel fooPage() {
        DocProjection projection = new ProjectionBuilder().build(sampleCorpus(), new VisibilityPolicy())
        TypePageModel page = projection.typePages.find { it.id.qualifiedName == "com.example.sdk.Foo" }
        assertNotNull(page)
        return page
    }

    private static DocCorpus sampleCorpus() {
        File sourceRoot = new File("src/test/resources/sample-sdk/src/main/java")
        return new JavadocApiParser().parseCorpus([sourceRoot.absolutePath], new ApiConfig())
    }

    private static class CountingVisibilityPolicy extends VisibilityPolicy {
        private final ApiMetadata countedMetadata
        int memberChecks = 0

        CountingVisibilityPolicy(ApiMetadata countedMetadata) {
            this.countedMetadata = countedMetadata
        }

        @Override
        boolean includes(ApiMetadata metadata) {
            if (metadata.is(countedMetadata)) {
                memberChecks++
            }
            return super.includes(metadata)
        }
    }
}

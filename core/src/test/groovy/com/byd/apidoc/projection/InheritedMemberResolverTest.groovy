package com.byd.apidoc.projection

import com.byd.apidoc.metadata.ApiMetadata
import com.byd.apidoc.metadata.ApiVisibility
import com.byd.apidoc.metadata.VisibilityPolicy
import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.DocCorpus
import com.byd.apidoc.model.DocId
import com.byd.apidoc.model.DocIdKind
import com.byd.apidoc.model.DocMember
import com.byd.apidoc.model.DocMemberKind
import com.byd.apidoc.model.DocType
import com.byd.apidoc.model.TypeRef
import com.byd.apidoc.parser.javadoc.JavadocApiParser
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

class InheritedMemberResolverTest {

    @Test
    void publicAndProtectedMethodsAndFieldsAppearFromBaseType() {
        List<InheritedMemberGroupModel> groups = resolve("com.example.sdk.inheritance.DerivedService")

        InheritedMemberGroupModel base = group(groups, "com.example.sdk.inheritance.BaseService")
        assertEquals("BaseService", base.ownerName)
        assertEquals("Inherited from BaseService", base.title)
        assertTrue(base.members*.name.contains("start"))
        assertTrue(base.members*.name.contains("protectedHook"))
        assertTrue(base.members*.name.contains("baseField"))
        assertEquals("Starts the base service.", member(base, "start").summary)
    }

    @Test
    void overriddenMethodsAreNotDuplicatedAsInheritedMembers() {
        List<InheritedMemberGroupModel> groups = resolve("com.example.sdk.inheritance.OverrideService")

        InheritedMemberGroupModel base = group(groups, "com.example.sdk.inheritance.BaseService")
        assertFalse(base.members*.name.contains("start"))
        assertTrue(base.members*.name.contains("protectedHook"))
        assertTrue(base.members*.name.contains("baseField"))
    }

    @Test
    void hiddenInheritedMembersObeyVisibilityPolicy() {
        List<InheritedMemberGroupModel> publicGroups = resolve("com.example.sdk.inheritance.HiddenMemberChild")
        assertTrue(publicGroups.isEmpty())

        List<InheritedMemberGroupModel> internalGroups = resolve("com.example.sdk.inheritance.HiddenMemberChild", new VisibilityPolicy(includeHidden: true))
        InheritedMemberGroupModel hidden = group(internalGroups, "com.example.sdk.inheritance.HiddenBaseMember")
        assertTrue(hidden.members*.name.contains("hiddenOperation"))
    }

    @Test
    void removedInheritedMembersObeyVisibilityPolicy() {
        List<InheritedMemberGroupModel> publicGroups = resolve("com.example.sdk.inheritance.RemovedMemberChild")
        assertTrue(publicGroups.isEmpty())

        List<InheritedMemberGroupModel> internalGroups = resolve("com.example.sdk.inheritance.RemovedMemberChild", new VisibilityPolicy(includeRemoved: true))
        InheritedMemberGroupModel removed = group(internalGroups, "com.example.sdk.inheritance.RemovedBaseMember")
        assertTrue(removed.members*.name.contains("removedOperation"))
    }

    @Test
    void interfaceMembersAreGroupedByOriginalOwnerAndLinkToOriginalAnchor() {
        List<InheritedMemberGroupModel> groups = resolve("com.example.sdk.inheritance.DerivedWithInterface")

        assertEquals(["com.example.sdk.inheritance.ServiceInterface"], groups*.ownerQualifiedName)
        InheritedMemberGroupModel serviceInterface = groups[0]
        MemberSummaryModel serviceName = member(serviceInterface, "serviceName")
        assertEquals("Names the interface service.", serviceName.summary)
        assertEquals("reference/com.example.sdk.inheritance.ServiceInterface.html#serviceName()", serviceName.url)
    }

    @Test
    void projectionBuilderExposesInheritedGroupsOnTypePageModel() {
        DocProjection projection = new ProjectionBuilder().build(sampleCorpus(), new VisibilityPolicy())
        TypePageModel derived = projection.typePages.find { it.id.qualifiedName == "com.example.sdk.inheritance.DerivedService" }

        assertNotNull(derived)
        assertFalse(derived.inheritedMemberGroups.isEmpty())
        assertEquals("com.example.sdk.inheritance.BaseService", derived.inheritedMemberGroups[0].ownerQualifiedName)
        assertTrue(derived.inheritedMemberGroups[0].members*.url.contains("reference/com.example.sdk.inheritance.BaseService.html#start()"))
    }

    @Test
    void hiddenOwnerTypeDoesNotSuppressVisibleGrandparentMembers() {
        DocId childId = typeId("com.example.Child")
        DocId hiddenParentId = typeId("com.example.HiddenParent")
        DocId visibleGrandparentId = typeId("com.example.VisibleGrandparent")
        DocId hiddenRunId = methodId("com.example.HiddenParent", "run()")
        DocId visibleRunId = methodId("com.example.VisibleGrandparent", "run()")
        DocCorpus corpus = new DocCorpus(
                types: [
                        new DocType(id: childId, name: "Child", qualifiedName: "com.example.Child", superType: typeRef("com.example.HiddenParent"), metadata: new ApiMetadata()),
                        new DocType(id: hiddenParentId, name: "HiddenParent", qualifiedName: "com.example.HiddenParent", superType: typeRef("com.example.VisibleGrandparent"), metadata: new ApiMetadata(visibility: ApiVisibility.HIDDEN), memberIds: [hiddenRunId]),
                        new DocType(id: visibleGrandparentId, name: "VisibleGrandparent", qualifiedName: "com.example.VisibleGrandparent", metadata: new ApiMetadata(), memberIds: [visibleRunId])
                ],
                members: [
                        new DocMember(id: hiddenRunId, ownerId: hiddenParentId, name: "run", kind: DocMemberKind.METHOD, metadata: new ApiMetadata()),
                        new DocMember(id: visibleRunId, ownerId: visibleGrandparentId, name: "run", kind: DocMemberKind.METHOD, metadata: new ApiMetadata())
                ]
        )

        List<InheritedMemberGroupModel> groups = new InheritedMemberResolver().resolve(corpus, corpus.types[0], new VisibilityPolicy())

        assertEquals(["com.example.VisibleGrandparent"], groups*.ownerQualifiedName)
        assertEquals(["run"], groups[0].members*.name)
    }

    @Test
    void cyclicTypeRefsDoNotRecurseForever() {
        DocId firstId = typeId("com.example.First")
        DocId secondId = typeId("com.example.Second")
        DocId memberId = methodId("com.example.Second", "fromSecond()")
        DocCorpus corpus = new DocCorpus(
                types: [
                        new DocType(id: firstId, name: "First", qualifiedName: "com.example.First", superType: typeRef("com.example.Second"), metadata: new ApiMetadata()),
                        new DocType(id: secondId, name: "Second", qualifiedName: "com.example.Second", superType: typeRef("com.example.First"), metadata: new ApiMetadata(), memberIds: [memberId])
                ],
                members: [
                        new DocMember(id: memberId, ownerId: secondId, name: "fromSecond", kind: DocMemberKind.METHOD, metadata: new ApiMetadata())
                ]
        )

        List<InheritedMemberGroupModel> groups = new InheritedMemberResolver().resolve(corpus, corpus.types[0], new VisibilityPolicy())

        assertEquals(["com.example.Second"], groups*.ownerQualifiedName)
    }

    private static List<InheritedMemberGroupModel> resolve(String qualifiedName, VisibilityPolicy policy = new VisibilityPolicy()) {
        DocCorpus corpus = sampleCorpus()
        DocType type = corpus.types.find { it.qualifiedName == qualifiedName }
        assertNotNull(type)
        return new InheritedMemberResolver().resolve(corpus, type, policy)
    }

    private static InheritedMemberGroupModel group(List<InheritedMemberGroupModel> groups, String ownerQualifiedName) {
        InheritedMemberGroupModel group = groups.find { it.ownerQualifiedName == ownerQualifiedName }
        assertNotNull(group)
        return group
    }

    private static MemberSummaryModel member(InheritedMemberGroupModel group, String name) {
        MemberSummaryModel member = group.members.find { it.name == name }
        assertNotNull(member)
        return member
    }

    private static DocCorpus sampleCorpus() {
        File sourceRoot = new File("src/test/resources/sample-sdk/src/main/java")
        return new JavadocApiParser().parseCorpus([sourceRoot.absolutePath], new ApiConfig())
    }

    private static DocId typeId(String qualifiedName) {
        new DocId(kind: DocIdKind.TYPE, qualifiedName: qualifiedName, canonicalId: "type:${qualifiedName}")
    }

    private static DocId methodId(String ownerQualifiedName, String signature) {
        String name = signature.substring(0, signature.indexOf('('))
        new DocId(
                kind: DocIdKind.METHOD,
                qualifiedName: "${ownerQualifiedName}.${name}",
                canonicalId: "method:${ownerQualifiedName}#${signature}",
                displayId: signature,
                anchorId: signature,
                signature: signature,
                fragment: signature
        )
    }

    private static TypeRef typeRef(String qualifiedName) {
        new TypeRef(qualifiedName: qualifiedName, displayName: qualifiedName, simpleName: qualifiedName.tokenize('.').last())
    }
}

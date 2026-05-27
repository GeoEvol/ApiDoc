package com.byd.apidoc.model

import com.byd.apidoc.comment.BlockTag
import com.byd.apidoc.comment.BlockTagKind
import com.byd.apidoc.comment.CommentDoc
import com.byd.apidoc.comment.CommentNode
import com.byd.apidoc.comment.CommentNodeKind
import com.byd.apidoc.comment.InlineTag
import com.byd.apidoc.comment.InlineTagKind
import com.byd.apidoc.metadata.ApiAvailability
import com.byd.apidoc.metadata.ApiMetadata
import com.byd.apidoc.metadata.ApiVisibility
import com.byd.apidoc.metadata.DeprecatedMetadata
import com.byd.apidoc.metadata.VisibilityPolicy
import com.byd.apidoc.projection.DocProjection
import com.byd.apidoc.projection.NavNode
import com.byd.apidoc.projection.NavNodeKind
import com.byd.apidoc.projection.PageKind
import com.byd.apidoc.projection.PageModel
import com.byd.apidoc.projection.SearchEntry
import com.byd.apidoc.projection.SearchEntryKind
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

class V1ModelSchemaTest {

    @Test
    void serializesCorpusMetadataCommentsRefsAndProjectionShape() {
        DocId typeId = new DocId(
                kind: DocIdKind.TYPE,
                qualifiedName: "com.example.sdk.Foo",
                fragment: "com.example.sdk.Foo"
        )
        DocId methodId = new DocId(
                kind: DocIdKind.METHOD,
                qualifiedName: "com.example.sdk.Foo.run",
                signature: "run(java.lang.String)",
                fragment: "run(java.lang.String)"
        )
        CommentDoc comment = new CommentDoc(
                rawText: "Summary.\n\n@param value value description\n@apiSince 3",
                summaryNodes: [
                        new CommentNode(kind: CommentNodeKind.TEXT, text: "Summary.")
                ],
                bodyNodes: [
                        new CommentNode(
                                kind: CommentNodeKind.INLINE_TAG,
                                inlineTag: new InlineTag(
                                        kind: InlineTagKind.LINK,
                                        name: "link",
                                        rawName: "{@link}",
                                        reference: new LinkRef(
                                                kind: LinkRefKind.INTERNAL,
                                                label: "Bar",
                                                targetId: new DocId(kind: DocIdKind.TYPE, qualifiedName: "com.example.sdk.Bar")
                                        ),
                                        known: true
                                )
                        )
                ],
                blockTags: [
                        new BlockTag(kind: BlockTagKind.PARAM, name: "param", rawName: "@param", key: "value", known: true),
                        new BlockTag(kind: BlockTagKind.CUSTOM, name: "apiSince", rawName: "@apiSince", rawText: "3", known: false)
                ]
        )
        ApiMetadata metadata = new ApiMetadata(
                visibility: ApiVisibility.PUBLIC,
                availability: ApiAvailability.DEPRECATED,
                since: "1.0",
                apiLevel: 3,
                deprecated: new DeprecatedMetadata(
                        fromJavadocTag: true,
                        fromAnnotation: true,
                        message: "use Bar instead"
                ),
                sourceTags: ["since", "apiSince", "deprecated"] as LinkedHashSet,
                sourceAnnotations: ["java.lang.Deprecated"] as LinkedHashSet
        )
        DocCorpus corpus = new DocCorpus(
                packages: [
                        new DocPackage(id: new DocId(kind: DocIdKind.PACKAGE, qualifiedName: "com.example.sdk"), name: "com.example.sdk", typeIds: [typeId])
                ],
                types: [
                        new DocType(
                                id: typeId,
                                name: "Foo",
                                qualifiedName: "com.example.sdk.Foo",
                                packageName: "com.example.sdk",
                                kind: DocTypeKind.CLASS,
                                comment: comment,
                                metadata: metadata,
                                memberIds: [methodId]
                        )
                ],
                members: [
                        new DocMember(
                                id: methodId,
                                ownerId: typeId,
                                name: "run",
                                qualifiedName: "com.example.sdk.Foo.run",
                                kind: DocMemberKind.METHOD,
                                returnType: new TypeRef(kind: TypeRefKind.DECLARED, displayName: "String", qualifiedName: "java.lang.String"),
                                parameters: [
                                        new DocParameter(name: "value", type: new TypeRef(kind: TypeRefKind.DECLARED, displayName: "String", qualifiedName: "java.lang.String"))
                                ],
                                comment: comment,
                                metadata: metadata
                        )
                ],
                comments: [(typeId.stableKey()): comment],
                refs: [(typeId.stableKey()): [
                        new LinkRef(kind: LinkRefKind.EXTERNAL, label: "List", externalUrl: "https://docs.oracle.com/javase/17/docs/api/java.base/java/util/List.html")
                ]],
                metadata: [(typeId.stableKey()): metadata]
        )
        DocProjection projection = new DocProjection(
                pages: [
                        new PageModel(kind: PageKind.TYPE, title: "Foo", url: "reference/com.example.sdk.Foo.html", targetId: typeId, metadata: metadata)
                ],
                nav: [
                        new NavNode(label: "com.example.sdk", kind: NavNodeKind.PACKAGE, url: "package/com.example.sdk.html", children: [
                                new NavNode(label: "Foo", kind: NavNodeKind.TYPE, url: "reference/com.example.sdk.Foo.html", targetId: typeId)
                        ])
                ],
                search: [
                        new SearchEntry(kind: SearchEntryKind.CLASS, label: "Foo", qualifiedName: "com.example.sdk.Foo", packageName: "com.example.sdk", url: "reference/com.example.sdk.Foo.html", metadata: metadata)
                ]
        )

        def parsedCorpus = new JsonSlurper().parseText(JsonOutput.toJson(corpus))
        def parsedProjection = new JsonSlurper().parseText(JsonOutput.toJson(projection))

        assertEquals("v1", parsedCorpus.schemaVersion)
        assertEquals("com.example.sdk.Foo", parsedCorpus.types[0].qualifiedName)
        assertEquals("run", parsedCorpus.members[0].name)
        assertEquals("Summary.", parsedCorpus.types[0].comment.summaryNodes[0].text)
        assertEquals("apiSince", parsedCorpus.types[0].comment.blockTags[1].name)
        assertEquals("DEPRECATED", parsedCorpus.types[0].metadata.availability)
        assertTrue(parsedCorpus.types[0].metadata.deprecated.fromJavadocTag)
        assertTrue(parsedCorpus.types[0].metadata.deprecated.fromAnnotation)
        assertEquals("java.lang.Deprecated", parsedCorpus.types[0].metadata.sourceAnnotations[0])
        assertEquals("EXTERNAL", parsedCorpus.refs[typeId.stableKey()][0].kind)

        assertEquals("Foo", parsedProjection.pages[0].title)
        assertEquals("com.example.sdk", parsedProjection.nav[0].label)
        assertEquals("CLASS", parsedProjection.search[0].kind)
        assertNotNull(parsedProjection.search[0].metadata)
    }

    @Test
    void visibilityPolicyFiltersProjectionWithoutDestroyingCorpusEligibility() {
        VisibilityPolicy publicPolicy = new VisibilityPolicy()
        ApiMetadata hidden = new ApiMetadata(visibility: ApiVisibility.HIDDEN)
        ApiMetadata removed = new ApiMetadata(visibility: ApiVisibility.PUBLIC, availability: ApiAvailability.REMOVED)
        ApiMetadata deprecated = new ApiMetadata(visibility: ApiVisibility.PUBLIC, availability: ApiAvailability.DEPRECATED)

        assertFalse(publicPolicy.includes(hidden))
        assertFalse(publicPolicy.includes(removed))
        assertTrue(publicPolicy.includes(deprecated))

        VisibilityPolicy internalPolicy = new VisibilityPolicy(includeHidden: true, includeRemoved: true)

        assertTrue(internalPolicy.includes(hidden))
        assertTrue(internalPolicy.includes(removed))
    }
}

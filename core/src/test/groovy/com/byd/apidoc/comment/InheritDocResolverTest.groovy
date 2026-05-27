package com.byd.apidoc.comment

import com.byd.apidoc.model.DiagnosticSeverity
import com.byd.apidoc.model.DocCorpus
import com.byd.apidoc.model.DocId
import com.byd.apidoc.model.DocIdKind
import com.byd.apidoc.model.DocMember
import com.byd.apidoc.model.DocMemberKind
import com.byd.apidoc.model.DocParameter
import com.byd.apidoc.model.DocType
import com.byd.apidoc.model.LinkRef
import com.byd.apidoc.model.LinkRefKind
import com.byd.apidoc.model.TypeRef
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotSame
import static org.junit.Assert.assertTrue

class InheritDocResolverTest {

    @Test
    void methodBodyContainingOnlyInheritDocUsesInheritedBody() {
        DocCorpus corpus = corpusWithOverride(
                comment("Base summary.", "Inherited body."),
                comment("Child summary.", null, [inheritNode()])
        )

        new InheritDocResolver().resolve(corpus)

        assertEquals("Inherited body.", text(childMember(corpus).comment.bodyNodes))
    }

    @Test
    void methodCommentContainingOnlyInheritDocKindUsesInheritedComment() {
        DocCorpus corpus = corpusWithOverride(
                comment("Base summary.", "Inherited body."),
                commentNodes([inheritKindNode()], [inheritKindNode()])
        )

        new InheritDocResolver().resolve(corpus)

        assertEquals("Base summary.", text(childMember(corpus).comment.summaryNodes))
        assertEquals("Inherited body.", text(childMember(corpus).comment.bodyNodes))
    }

    @Test
    void methodSummaryCanSurroundInheritedSummaryWithLocalText() {
        DocCorpus corpus = corpusWithOverride(
                comment("base operation", "Base body."),
                commentNodes([textNode("Before"), inheritNode(), textNode("after.")], [])
        )

        new InheritDocResolver().resolve(corpus)

        assertEquals("Before base operation after.", text(childMember(corpus).comment.summaryNodes))
    }

    @Test
    void paramTagInheritDocUsesInheritedParamWithMatchingName() {
        DocCorpus corpus = corpusWithOverride(
                comment("Base summary.", "Base body.", [], [paramTag("value", "base parameter")]),
                comment("Child summary.", "Child body.", [], [paramTag("value", null, [inheritNode()])])
        )

        new InheritDocResolver().resolve(corpus)

        assertEquals("base parameter", text(block(childMember(corpus), BlockTagKind.PARAM, "value").body))
    }

    @Test
    void returnTagInheritDocUsesInheritedReturnTag() {
        DocCorpus corpus = corpusWithOverride(
                comment("Base summary.", "Base body.", [], [returnTag("base return")]),
                comment("Child summary.", "Child body.", [], [returnTag(null, [inheritNode()])])
        )

        new InheritDocResolver().resolve(corpus)

        assertEquals("base return", text(block(childMember(corpus), BlockTagKind.RETURN, null).body))
    }

    @Test
    void throwsTagInheritDocUsesInheritedThrowsTagWithMatchingException() {
        DocCorpus corpus = corpusWithOverride(
                comment("Base summary.", "Base body.", [], [throwsTag("IllegalArgumentException", "base throws")]),
                comment("Child summary.", "Child body.", [], [throwsTag("IllegalArgumentException", null, [inheritNode()])])
        )

        new InheritDocResolver().resolve(corpus)

        assertEquals("base throws", text(block(childMember(corpus), BlockTagKind.THROWS, "IllegalArgumentException").body))
    }

    @Test
    void implementedInterfaceMethodCanProvideInheritedDoc() {
        DocCorpus corpus = corpusWithInterface(
                comment("Interface summary.", "Interface body."),
                commentNodes([inheritNode()], [inheritNode()])
        )

        new InheritDocResolver().resolve(corpus)

        assertEquals("Interface summary.", text(childMember(corpus).comment.summaryNodes))
        assertEquals("Interface body.", text(childMember(corpus).comment.bodyNodes))
    }

    @Test
    void transitiveSuperclassMethodCanProvideInheritedDoc() {
        DocCorpus corpus = corpusWithOverride(
                comment("Base summary.", "Base body."),
                commentNodes([inheritNode()], [inheritNode()])
        )
        DocId middleTypeId = typeId("com.example.Middle")
        corpus.types.add(new DocType(
                id: middleTypeId,
                name: "Middle",
                qualifiedName: "com.example.Middle",
                superType: typeRef("com.example.Base")
        ))
        corpus.types.find { it.qualifiedName == "com.example.Child" }.superType = typeRef("com.example.Middle")

        new InheritDocResolver().resolve(corpus)

        assertEquals("Base summary.", text(childMember(corpus).comment.summaryNodes))
        assertEquals("Base body.", text(childMember(corpus).comment.bodyNodes))
    }

    @Test
    void inheritedMethodContainingInheritDocIsResolvedBeforeCopyingToChild() {
        DocCorpus corpus = corpusWithTwoLevelOverride(
                comment("Base summary.", "Base body."),
                commentNodes([inheritNode()], [inheritNode()]),
                commentNodes([inheritNode()], [inheritNode()])
        )
        corpus.members = [corpus.members[0], corpus.members[2], corpus.members[1]]

        new InheritDocResolver().resolve(corpus)

        assertEquals("Base summary.", text(childMember(corpus).comment.summaryNodes))
        assertEquals("Base body.", text(childMember(corpus).comment.bodyNodes))
    }

    @Test
    void inheritedInlineReferencesAreCopiedWithoutSharingSourceReferenceObjects() {
        CommentNode inheritedLink = linkNode("Base link", "com.example.Target")
        DocCorpus corpus = corpusWithOverride(
                commentNodes([inheritedLink], []),
                commentNodes([inheritNode()], [])
        )

        new InheritDocResolver().resolve(corpus)

        InlineTag inheritedTag = inheritedLink.inlineTag
        InlineTag copiedTag = childMember(corpus).comment.summaryNodes.first().inlineTag
        assertEquals("com.example.Target", copiedTag.reference.rawTarget)
        assertNotSame(inheritedTag, copiedTag)
        assertNotSame(inheritedTag.reference, copiedTag.reference)
    }

    @Test
    void missingInheritedDocRecordsDiagnosticWithoutCrashing() {
        DocCorpus corpus = corpusWithOverride(
                comment("Base summary.", "Base body."),
                comment("Child summary.", null, [inheritNode()])
        )
        corpus.types.find { it.qualifiedName == "com.example.Child" }.superType = null

        new InheritDocResolver().resolve(corpus)

        assertEquals("{@inheritDoc}", text(childMember(corpus).comment.bodyNodes))
        assertTrue(corpus.diagnostics.any {
            it.severity == DiagnosticSeverity.WARNING &&
                    it.code == "inheritDoc.unresolved" &&
                    it.targetId?.canonicalId == "method:com.example.Child#run(java.lang.String)"
        })
    }

    @Test
    void sparseCorpusWithNullMapsAndDiagnosticListsDoesNotCrash() {
        DocCorpus corpus = corpusWithOverride(
                comment("Base summary.", "Base body."),
                comment("Child summary.", null, [inheritNode()])
        )
        corpus.types.find { it.qualifiedName == "com.example.Child" }.superType = null
        corpus.comments = null
        corpus.diagnostics = null
        childMember(corpus).comment.diagnostics = null

        new InheritDocResolver().resolve(corpus)

        assertEquals("{@inheritDoc}", text(childMember(corpus).comment.bodyNodes))
    }

    private static DocCorpus corpusWithOverride(CommentDoc baseComment, CommentDoc childComment) {
        DocId baseTypeId = typeId("com.example.Base")
        DocId childTypeId = typeId("com.example.Child")
        DocId baseRunId = methodId("com.example.Base", "run(java.lang.String)")
        DocId childRunId = methodId("com.example.Child", "run(java.lang.String)")
        DocMember baseRun = new DocMember(
                id: baseRunId,
                ownerId: baseTypeId,
                name: "run",
                kind: DocMemberKind.METHOD,
                parameters: [new DocParameter(name: "value", type: typeRef("java.lang.String"))],
                comment: baseComment
        )
        DocMember childRun = new DocMember(
                id: childRunId,
                ownerId: childTypeId,
                name: "run",
                kind: DocMemberKind.METHOD,
                parameters: [new DocParameter(name: "value", type: typeRef("java.lang.String"))],
                comment: childComment
        )
        new DocCorpus(
                types: [
                        new DocType(id: baseTypeId, name: "Base", qualifiedName: "com.example.Base", memberIds: [baseRunId]),
                        new DocType(id: childTypeId, name: "Child", qualifiedName: "com.example.Child", superType: typeRef("com.example.Base"), memberIds: [childRunId])
                ],
                members: [baseRun, childRun]
        )
    }

    private static DocCorpus corpusWithInterface(CommentDoc interfaceComment, CommentDoc childComment) {
        DocId interfaceTypeId = typeId("com.example.Service")
        DocId childTypeId = typeId("com.example.Child")
        DocId interfaceRunId = methodId("com.example.Service", "run(java.lang.String)")
        DocId childRunId = methodId("com.example.Child", "run(java.lang.String)")
        DocMember interfaceRun = new DocMember(
                id: interfaceRunId,
                ownerId: interfaceTypeId,
                name: "run",
                kind: DocMemberKind.METHOD,
                parameters: [new DocParameter(name: "value", type: typeRef("java.lang.String"))],
                comment: interfaceComment
        )
        DocMember childRun = new DocMember(
                id: childRunId,
                ownerId: childTypeId,
                name: "run",
                kind: DocMemberKind.METHOD,
                parameters: [new DocParameter(name: "value", type: typeRef("java.lang.String"))],
                comment: childComment
        )
        new DocCorpus(
                types: [
                        new DocType(id: interfaceTypeId, name: "Service", qualifiedName: "com.example.Service", memberIds: [interfaceRunId]),
                        new DocType(id: childTypeId, name: "Child", qualifiedName: "com.example.Child", interfaces: [typeRef("com.example.Service")], memberIds: [childRunId])
                ],
                members: [interfaceRun, childRun]
        )
    }

    private static DocCorpus corpusWithTwoLevelOverride(CommentDoc baseComment, CommentDoc middleComment, CommentDoc childComment) {
        DocId baseTypeId = typeId("com.example.Base")
        DocId middleTypeId = typeId("com.example.Middle")
        DocId childTypeId = typeId("com.example.Child")
        DocId baseRunId = methodId("com.example.Base", "run(java.lang.String)")
        DocId middleRunId = methodId("com.example.Middle", "run(java.lang.String)")
        DocId childRunId = methodId("com.example.Child", "run(java.lang.String)")
        DocMember baseRun = method(baseTypeId, baseRunId, "run", baseComment)
        DocMember middleRun = method(middleTypeId, middleRunId, "run", middleComment)
        DocMember childRun = method(childTypeId, childRunId, "run", childComment)
        new DocCorpus(
                types: [
                        new DocType(id: baseTypeId, name: "Base", qualifiedName: "com.example.Base", memberIds: [baseRunId]),
                        new DocType(id: middleTypeId, name: "Middle", qualifiedName: "com.example.Middle", superType: typeRef("com.example.Base"), memberIds: [middleRunId]),
                        new DocType(id: childTypeId, name: "Child", qualifiedName: "com.example.Child", superType: typeRef("com.example.Middle"), memberIds: [childRunId])
                ],
                members: [baseRun, middleRun, childRun]
        )
    }

    private static DocMember method(DocId ownerId, DocId id, String name, CommentDoc comment) {
        new DocMember(
                id: id,
                ownerId: ownerId,
                name: name,
                kind: DocMemberKind.METHOD,
                parameters: [new DocParameter(name: "value", type: typeRef("java.lang.String"))],
                comment: comment
        )
    }

    private static DocMember childMember(DocCorpus corpus) {
        corpus.members.find { it.ownerId?.qualifiedName == "com.example.Child" && it.name == "run" }
    }

    private static BlockTag block(DocMember member, BlockTagKind kind, String key) {
        member.comment.blockTags.find { it.kind == kind && (key == null || it.key == key) }
    }

    private static CommentDoc comment(String summary, String body, List<CommentNode> bodyNodes = null, List<BlockTag> blockTags = []) {
        commentNodes(summary ? [textNode(summary)] : [], bodyNodes != null ? bodyNodes : (body ? [textNode(body)] : []), blockTags)
    }

    private static CommentDoc commentNodes(List<CommentNode> summaryNodes, List<CommentNode> bodyNodes, List<BlockTag> blockTags = []) {
        new CommentDoc(summaryNodes: summaryNodes, bodyNodes: bodyNodes, blockTags: blockTags)
    }

    private static ParamTag paramTag(String name, String body, List<CommentNode> nodes = null) {
        new ParamTag(kind: BlockTagKind.PARAM, name: "param", key: name, parameterName: name, body: nodes ?: [textNode(body)], rawText: body, known: true)
    }

    private static ReturnTag returnTag(String body, List<CommentNode> nodes = null) {
        new ReturnTag(kind: BlockTagKind.RETURN, name: "return", body: nodes ?: [textNode(body)], rawText: body, known: true)
    }

    private static ThrowsTag throwsTag(String exceptionName, String body, List<CommentNode> nodes = null) {
        new ThrowsTag(kind: BlockTagKind.THROWS, name: "throws", key: exceptionName, exceptionName: exceptionName, body: nodes ?: [textNode(body)], rawText: body, known: true)
    }

    private static CommentNode textNode(String text) {
        new CommentNode(kind: CommentNodeKind.TEXT, text: text)
    }

    private static CommentNode inheritNode() {
        new CommentNode(
                kind: CommentNodeKind.INLINE_TAG,
                text: "{@inheritDoc}",
                inlineTag: new InlineTag(kind: InlineTagKind.CUSTOM, name: "inheritDoc", rawName: "{@inheritDoc}", rawText: "{@inheritDoc}", known: false)
        )
    }

    private static CommentNode inheritKindNode() {
        new CommentNode(
                kind: CommentNodeKind.INLINE_TAG,
                text: "{@inheritDoc}",
                inlineTag: new InlineTag(kind: InlineTagKind.INHERIT_DOC, name: "inheritDoc", rawName: "{@inheritDoc}", rawText: "{@inheritDoc}", known: true)
        )
    }

    private static CommentNode linkNode(String label, String target) {
        new CommentNode(
                kind: CommentNodeKind.INLINE_TAG,
                text: label,
                inlineTag: new InlineTag(
                        kind: InlineTagKind.LINK,
                        name: "link",
                        rawName: "{@link}",
                        rawText: label,
                        reference: new LinkRef(kind: LinkRefKind.UNRESOLVED, rawTarget: target, label: label, fallbackText: label),
                        known: true
                )
        )
    }

    private static String text(List<CommentNode> nodes) {
        (nodes ?: []).collect { it.text ?: it.inlineTag?.rawText ?: "" }.join(" ").replaceAll(/\s+/, " ").trim()
    }

    private static DocId typeId(String qualifiedName) {
        new DocId(kind: DocIdKind.TYPE, qualifiedName: qualifiedName, canonicalId: "type:${qualifiedName}")
    }

    private static DocId methodId(String ownerQualifiedName, String signature) {
        String name = signature.substring(0, signature.indexOf("("))
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
        new TypeRef(qualifiedName: qualifiedName, displayName: qualifiedName, simpleName: qualifiedName.tokenize(".").last())
    }
}

package com.byd.apidoc.comment

import com.byd.apidoc.model.DiagnosticSeverity
import com.byd.apidoc.model.DocCorpus
import com.byd.apidoc.model.DocDiagnostic
import com.byd.apidoc.model.DocId
import com.byd.apidoc.model.DocMember
import com.byd.apidoc.model.DocMemberKind
import com.byd.apidoc.model.DocType
import com.byd.apidoc.model.LinkRef
import com.byd.apidoc.model.TypeRef

class InheritDocResolver {
    private Map<String, DocType> typesByName = [:]
    private Map<String, DocMember> membersByKey = [:]

    DocCorpus resolve(DocCorpus corpus) {
        if (corpus == null) {
            return corpus
        }
        typesByName = (corpus.types ?: []).findAll { DocType type -> type?.qualifiedName }
                .collectEntries { DocType type -> [(type.qualifiedName): type] }
        membersByKey = (corpus.members ?: []).findAll { DocMember member -> member?.id?.stableKey() }
                .collectEntries { DocMember member -> [(member.id.stableKey()): member] }

        (corpus.members ?: []).findAll { DocMember member -> isMethodLike(member) && hasInheritDoc(member.comment) }.each { DocMember member ->
            resolveMemberComment(corpus, member, new LinkedHashSet<String>())
        }
        return corpus
    }

    private CommentDoc resolveMemberComment(DocCorpus corpus, DocMember member, Set<String> resolving) {
        CommentDoc comment = member.comment
        if (comment == null || !hasInheritDoc(comment)) {
            return comment
        }
        String memberKey = member?.id?.stableKey()
        if (memberKey && !resolving.add(memberKey)) {
            recordDiagnostic(corpus, member, "cyclic inherited comment")
            return comment
        }

        DocMember inherited = findInheritedMethod(member)
        if (inherited != null) {
            resolveMemberComment(corpus, inherited, resolving)
        }
        CommentDoc inheritedComment = inherited?.comment

        comment.summaryNodes = resolveNodes(corpus, member, comment.summaryNodes, inheritedComment?.summaryNodes, "summary")
        comment.bodyNodes = resolveNodes(corpus, member, comment.bodyNodes, inheritedComment?.bodyNodes, "body")
        (comment.blockTags ?: []).each { BlockTag tag ->
            if (!hasInheritDoc(tag.body)) {
                return
            }
            BlockTag inheritedTag = findInheritedBlockTag(tag, inheritedComment)
            tag.body = resolveNodes(corpus, member, tag.body, inheritedTag?.body, blockContext(tag))
            if (inheritedTag?.rawText) {
                tag.rawText = inheritedTag.rawText
            }
        }

        rebuildInlineNodes(comment)
        if (memberKey) {
            if (corpus.comments == null) {
                corpus.comments = [:]
            }
            corpus.comments[memberKey] = comment
        }
        if (memberKey) {
            resolving.remove(memberKey)
        }
        return comment
    }

    private List<CommentNode> resolveNodes(DocCorpus corpus,
                                           DocMember target,
                                           List<CommentNode> nodes,
                                           List<CommentNode> inheritedNodes,
                                           String context) {
        if (!hasInheritDoc(nodes)) {
            return nodes ?: []
        }
        if (!inheritedNodes) {
            recordDiagnostic(corpus, target, context)
            return nodes ?: []
        }
        List<CommentNode> resolved = []
        (nodes ?: []).each { CommentNode node ->
            if (isInheritDoc(node)) {
                resolved.addAll(copyNodes(inheritedNodes))
            } else {
                resolved.add(copyNode(node))
            }
        }
        return resolved
    }

    private DocMember findInheritedMethod(DocMember member) {
        DocType owner = ownerOf(member)
        if (owner == null) {
            return null
        }
        Set<String> visited = new LinkedHashSet<>()
        DocMember fromSuper = findInheritedMethod(owner.superType, member, visited)
        if (fromSuper != null) {
            return fromSuper
        }
        for (TypeRef interfaceRef : (owner.interfaces ?: [])) {
            DocMember candidate = findInheritedMethod(interfaceRef, member, visited)
            if (candidate != null) {
                return candidate
            }
        }
        return null
    }

    private DocMember findInheritedMethod(TypeRef ownerRef, DocMember target, Set<String> visited) {
        DocType owner = ownerRef?.qualifiedName ? typesByName[ownerRef.qualifiedName] : null
        if (owner == null || !visited.add(owner.qualifiedName)) {
            return null
        }

        DocMember direct = declaredMembers(owner).find { DocMember candidate -> sameMethod(candidate, target) }
        if (direct != null) {
            return direct
        }

        DocMember fromSuper = findInheritedMethod(owner.superType, target, visited)
        if (fromSuper != null) {
            return fromSuper
        }
        for (TypeRef interfaceRef : (owner.interfaces ?: [])) {
            DocMember candidate = findInheritedMethod(interfaceRef, target, visited)
            if (candidate != null) {
                return candidate
            }
        }
        return null
    }

    private DocType ownerOf(DocMember member) {
        String ownerKey = member?.ownerId?.stableKey()
        if (ownerKey) {
            return (typesByName.values().find { DocType type -> type.id?.stableKey() == ownerKey })
        }
        String ownerName = member?.id?.canonicalId?.contains("#") ? member.id.canonicalId.substring(member.id.canonicalId.indexOf(":") + 1, member.id.canonicalId.indexOf("#")) : null
        return ownerName ? typesByName[ownerName] : null
    }

    private List<DocMember> declaredMembers(DocType owner) {
        (owner.memberIds ?: []).collect { DocId id ->
            id?.stableKey() ? membersByKey[id.stableKey()] : null
        }.findAll { it != null }
    }

    private static boolean sameMethod(DocMember candidate, DocMember target) {
        if (!isMethodLike(candidate) || !isMethodLike(target)) {
            return false
        }
        if (candidate.id?.signature && target.id?.signature) {
            return candidate.id.signature == target.id.signature
        }
        return methodKey(candidate) == methodKey(target)
    }

    private static String methodKey(DocMember member) {
        String params = (member.parameters ?: []).collect { parameter ->
            parameter.type?.qualifiedName ?: parameter.type?.rawText ?: parameter.type?.displayName ?: ""
        }.join(",")
        return "${member.name}(${params})"
    }

    private static boolean isMethodLike(DocMember member) {
        member?.kind in [DocMemberKind.METHOD, DocMemberKind.ANNOTATION_ELEMENT]
    }

    private static BlockTag findInheritedBlockTag(BlockTag tag, CommentDoc inheritedComment) {
        if (tag == null || inheritedComment == null) {
            return null
        }
        (inheritedComment.blockTags ?: []).find { BlockTag candidate ->
            candidate.kind == tag.kind && blockKey(candidate) == blockKey(tag)
        }
    }

    private static String blockKey(BlockTag tag) {
        if (tag instanceof ParamTag) {
            return tag.parameterName ?: tag.key
        }
        if (tag instanceof ThrowsTag) {
            return simpleName(tag.exceptionName ?: tag.key)
        }
        return tag.key ?: ""
    }

    private static String blockContext(BlockTag tag) {
        if (tag instanceof ParamTag) {
            return "param ${tag.parameterName ?: tag.key ?: ''}".trim()
        }
        if (tag instanceof ThrowsTag) {
            return "throws ${tag.exceptionName ?: tag.key ?: ''}".trim()
        }
        return tag.name ?: tag.kind?.name()?.toLowerCase(Locale.ROOT) ?: "block"
    }

    private static String simpleName(String name) {
        name?.tokenize(".")?.last() ?: ""
    }

    private static boolean hasInheritDoc(CommentDoc comment) {
        if (comment == null) {
            return false
        }
        hasInheritDoc(comment.summaryNodes) ||
                hasInheritDoc(comment.bodyNodes) ||
                (comment.blockTags ?: []).any { BlockTag tag -> hasInheritDoc(tag.body) }
    }

    private static boolean hasInheritDoc(List<CommentNode> nodes) {
        (nodes ?: []).any { CommentNode node -> isInheritDoc(node) || hasInheritDoc(node.children) }
    }

    private static boolean isInheritDoc(CommentNode node) {
        if (node == null) {
            return false
        }
        InlineTag tag = node.inlineTag
        if (tag == null) {
            return false
        }
        tag.kind == InlineTagKind.INHERIT_DOC ||
                tag.name == "inheritDoc" ||
                tag.rawName == "{@inheritDoc}" ||
                tag.rawText == "{@inheritDoc}"
    }

    private static List<CommentNode> copyNodes(List<CommentNode> nodes) {
        (nodes ?: []).collect { CommentNode node -> copyNode(node) }
    }

    private static CommentNode copyNode(CommentNode node) {
        if (node == null) {
            return null
        }
        new CommentNode(
                kind: node.kind,
                text: node.text,
                htmlName: node.htmlName,
                htmlAttributes: new LinkedHashMap<String, String>(node.htmlAttributes ?: [:]),
                htmlStart: node.htmlStart,
                htmlEnd: node.htmlEnd,
                htmlSelfClosing: node.htmlSelfClosing,
                inlineTag: copyInlineTag(node.inlineTag),
                children: copyNodes(node.children)
        )
    }

    private static InlineTag copyInlineTag(InlineTag tag) {
        if (tag == null) {
            return null
        }
        new InlineTag(
                kind: tag.kind,
                name: tag.name,
                rawName: tag.rawName,
                reference: copyLinkRef(tag.reference),
                label: copyNodes(tag.label),
                body: tag.body,
                rawText: tag.rawText,
                known: tag.known
        )
    }

    private static LinkRef copyLinkRef(LinkRef reference) {
        if (reference == null) {
            return null
        }
        new LinkRef(
                kind: reference.kind,
                rawTarget: reference.rawTarget,
                label: reference.label,
                targetId: reference.targetId,
                externalUrl: reference.externalUrl,
                fallbackText: reference.fallbackText,
                diagnostics: new ArrayList<DocDiagnostic>(reference.diagnostics ?: [])
        )
    }

    private static void rebuildInlineNodes(CommentDoc comment) {
        comment.inlineNodes = []
        collectInlineNodes(comment.inlineNodes, comment.summaryNodes)
        collectInlineNodes(comment.inlineNodes, comment.bodyNodes)
        (comment.blockTags ?: []).each { BlockTag tag -> collectInlineNodes(comment.inlineNodes, tag.body) }
    }

    private static void collectInlineNodes(List<InlineTag> tags, List<CommentNode> nodes) {
        (nodes ?: []).each { CommentNode node ->
            if (node?.inlineTag != null) {
                tags.add(node.inlineTag)
            }
            collectInlineNodes(tags, node?.children)
        }
    }

    private static void recordDiagnostic(DocCorpus corpus, DocMember target, String context) {
        DocDiagnostic diagnostic = new DocDiagnostic(
                severity: DiagnosticSeverity.WARNING,
                code: "inheritDoc.unresolved",
                message: "Unable to resolve {@inheritDoc} for ${context}.",
                targetId: target?.id
        )
        if (corpus != null) {
            corpus.diagnostics = corpus.diagnostics ?: []
            corpus.diagnostics.add(diagnostic)
        }
        if (target?.comment != null) {
            target.comment.diagnostics = target.comment.diagnostics ?: []
            target.comment.diagnostics.add(diagnostic)
        }
    }
}

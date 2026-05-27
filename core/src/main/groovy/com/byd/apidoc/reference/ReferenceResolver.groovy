package com.byd.apidoc.reference

import com.byd.apidoc.comment.CommentDoc
import com.byd.apidoc.comment.CommentNode
import com.byd.apidoc.comment.InlineTag
import com.byd.apidoc.model.DocCorpus
import com.byd.apidoc.model.DocId
import com.byd.apidoc.model.DocMember
import com.byd.apidoc.model.DocType
import com.byd.apidoc.model.LinkRef
import com.byd.apidoc.model.LinkRefKind
import com.byd.apidoc.model.TypeRef

class ReferenceResolver {
    private final ExternalLinkResolver externalLinkResolver

    ReferenceResolver(ExternalLinkResolver externalLinkResolver = new ExternalLinkResolver()) {
        this.externalLinkResolver = externalLinkResolver
    }

    DocCorpus resolve(DocCorpus corpus) {
        if (corpus == null) {
            return corpus
        }
        ReferenceIndex index = new ReferenceIndex(corpus)
        corpus.types.each { DocType type ->
            resolveTypeRef(type.superType, index)
            type.interfaces?.each { resolveTypeRef(it, index) }
            resolveComment(type.comment, index)
        }
        corpus.members.each { DocMember member ->
            resolveTypeRef(member.type, index)
            resolveTypeRef(member.returnType, index)
            member.parameters?.each { resolveTypeRef(it.type, index) }
            member.throwsTypes?.each { resolveTypeRef(it, index) }
            resolveComment(member.comment, index)
        }
        return corpus
    }

    private void resolveComment(CommentDoc comment, ReferenceIndex index) {
        if (comment == null) {
            return
        }
        comment.inlineNodes?.each { InlineTag tag ->
            if (tag.reference != null) {
                tag.reference = resolveLink(tag.reference.rawTarget ?: tag.reference.fallbackText ?: tag.reference.label, tag.reference.label, index)
            }
        }
        comment.summaryNodes?.each { resolveNode(it, index) }
        comment.bodyNodes?.each { resolveNode(it, index) }
        comment.blockTags?.each { block ->
            block.body?.each { resolveNode(it, index) }
        }
    }

    private void resolveNode(CommentNode node, ReferenceIndex index) {
        if (node == null) return
        if (node.inlineTag?.reference != null) {
            node.inlineTag.reference = resolveLink(node.inlineTag.reference.rawTarget ?: node.inlineTag.reference.fallbackText ?: node.inlineTag.reference.label, node.inlineTag.reference.label, index)
        }
        node.children?.each { resolveNode(it, index) }
    }

    private void resolveTypeRef(TypeRef typeRef, ReferenceIndex index) {
        if (typeRef == null) {
            return
        }
        if (typeRef.qualifiedName) {
            typeRef.linkRef = resolveLink(typeRef.qualifiedName, typeRef.simpleName ?: typeRef.displayName, index)
        }
        typeRef.typeArguments?.each { resolveTypeRef(it, index) }
        resolveTypeRef(typeRef.componentType, index)
        resolveTypeRef(typeRef.bound, index)
        typeRef.bounds?.each { resolveTypeRef(it, index) }
        typeRef.upperBounds?.each { resolveTypeRef(it, index) }
        typeRef.lowerBounds?.each { resolveTypeRef(it, index) }
    }

    private LinkRef resolveLink(String rawTarget, String label, ReferenceIndex index) {
        String target = normalizeTarget(rawTarget)
        if (!target) {
            return unresolved(rawTarget, label)
        }
        DocId internal = index.byQualifiedName[target] ?: index.bySimpleName[target]
        if (internal != null) {
            return new LinkRef(
                    kind: LinkRefKind.INTERNAL,
                    rawTarget: rawTarget,
                    label: label ?: internal.displayId ?: target,
                    targetId: internal,
                    fallbackText: label ?: target
            )
        }
        LinkRef external = externalLinkResolver.resolveType(target, label)
        if (external.kind == LinkRefKind.EXTERNAL) {
            external.rawTarget = rawTarget ?: target
            return external
        }
        return unresolved(rawTarget ?: target, label ?: target)
    }

    private static LinkRef unresolved(String rawTarget, String label) {
        new LinkRef(
                kind: LinkRefKind.UNRESOLVED,
                rawTarget: rawTarget,
                label: label,
                fallbackText: label ?: rawTarget
        )
    }

    private static String normalizeTarget(String rawTarget) {
        if (!rawTarget) return rawTarget
        String value = rawTarget.trim()
        int hash = value.indexOf('#')
        if (hash >= 0) {
            value = value.substring(0, hash)
        }
        int space = value.indexOf(' ')
        if (space >= 0) {
            value = value.substring(0, space)
        }
        return value
    }

    private static class ReferenceIndex {
        Map<String, DocId> byQualifiedName = [:]
        Map<String, DocId> bySimpleName = [:]

        ReferenceIndex(DocCorpus corpus) {
            Map<String, List<DocId>> simple = [:].withDefault { [] }
            corpus.types.each { DocType type ->
                if (type.qualifiedName && type.id) {
                    byQualifiedName[type.qualifiedName] = type.id
                    simple[type.name ?: type.qualifiedName.tokenize('.').last()].add(type.id)
                }
            }
            simple.each { String name, List<DocId> ids ->
                if (ids.size() == 1) {
                    bySimpleName[name] = ids[0]
                }
            }
        }
    }
}

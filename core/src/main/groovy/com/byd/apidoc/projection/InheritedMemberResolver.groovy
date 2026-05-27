package com.byd.apidoc.projection

import com.byd.apidoc.comment.CommentDoc
import com.byd.apidoc.metadata.VisibilityPolicy
import com.byd.apidoc.model.DocCorpus
import com.byd.apidoc.model.DocId
import com.byd.apidoc.model.DocMember
import com.byd.apidoc.model.DocMemberKind
import com.byd.apidoc.model.DocType
import com.byd.apidoc.model.TypeRef

class InheritedMemberResolver {

    List<InheritedMemberGroupModel> resolve(DocCorpus corpus, DocType type, VisibilityPolicy policy = new VisibilityPolicy()) {
        if (corpus == null || type == null) {
            return []
        }

        VisibilityPolicy effectivePolicy = policy ?: new VisibilityPolicy()
        Map<String, DocType> typesByName = (corpus.types ?: []).findAll { DocType docType ->
            docType?.qualifiedName
        }.collectEntries { DocType docType ->
            [(docType.qualifiedName): docType]
        }
        Map<String, DocMember> membersByKey = (corpus.members ?: []).findAll { DocMember member ->
            member?.id?.stableKey()
        }.collectEntries { DocMember member ->
            [(member.id.stableKey()): member]
        }

        Set<String> inheritedMethodKeys = directMethodKeys(type, membersByKey)
        Set<String> inheritedFieldKeys = directFieldKeys(type, membersByKey)
        LinkedHashSet<String> visitedTypes = new LinkedHashSet<>()
        List<InheritedMemberGroupModel> groups = []

        collectFrom(type.superType, typesByName, membersByKey, effectivePolicy, inheritedMethodKeys, inheritedFieldKeys, visitedTypes, groups)
        (type.interfaces ?: []).each { TypeRef interfaceRef ->
            collectFrom(interfaceRef, typesByName, membersByKey, effectivePolicy, inheritedMethodKeys, inheritedFieldKeys, visitedTypes, groups)
        }

        return groups
    }

    private void collectFrom(TypeRef ownerRef,
                             Map<String, DocType> typesByName,
                             Map<String, DocMember> membersByKey,
                             VisibilityPolicy policy,
                             Set<String> inheritedMethodKeys,
                             Set<String> inheritedFieldKeys,
                             Set<String> visitedTypes,
                             List<InheritedMemberGroupModel> groups) {
        DocType owner = ownerRef?.qualifiedName ? typesByName[ownerRef.qualifiedName] : null
        if (owner == null || !visitedTypes.add(owner.qualifiedName)) {
            return
        }

        boolean includeOwner = policy.includes(owner.metadata)
        List<DocMember> ownerMembers = declaredMembers(owner, membersByKey).findAll { DocMember member ->
            isInheritable(member) && policy.includes(member.metadata)
        }
        List<MemberSummaryModel> summaries = []
        if (includeOwner) {
            ownerMembers.sort { DocMember member -> "${member.kind}:${member.name}:${member.id?.signature ?: ''}" }.each { DocMember member ->
                if (member.kind == DocMemberKind.FIELD) {
                    String fieldKey = member.name ?: ""
                    if (!fieldKey || inheritedFieldKeys.contains(fieldKey)) {
                        return
                    }
                    inheritedFieldKeys.add(fieldKey)
                } else {
                    String methodKey = methodKey(member)
                    if (!methodKey || inheritedMethodKeys.contains(methodKey)) {
                        return
                    }
                    inheritedMethodKeys.add(methodKey)
                }
                summaries.add(memberSummary(owner, member))
            }
        }

        if (!summaries.isEmpty()) {
            groups.add(new InheritedMemberGroupModel(
                    title: "Inherited from ${owner.name}",
                    ownerName: owner.name,
                    ownerQualifiedName: owner.qualifiedName,
                    members: summaries
            ))
        }

        collectFrom(owner.superType, typesByName, membersByKey, policy, inheritedMethodKeys, inheritedFieldKeys, visitedTypes, groups)
        (owner.interfaces ?: []).each { TypeRef interfaceRef ->
            collectFrom(interfaceRef, typesByName, membersByKey, policy, inheritedMethodKeys, inheritedFieldKeys, visitedTypes, groups)
        }
    }

    private static List<DocMember> declaredMembers(DocType owner, Map<String, DocMember> membersByKey) {
        (owner.memberIds ?: []).collect { DocId id ->
            id?.stableKey() ? membersByKey[id.stableKey()] : null
        }.findAll { it != null }
    }

    private static boolean isInheritable(DocMember member) {
        member.kind in [DocMemberKind.FIELD, DocMemberKind.METHOD, DocMemberKind.ANNOTATION_ELEMENT]
    }

    private static Set<String> directMethodKeys(DocType type, Map<String, DocMember> membersByKey) {
        declaredMembers(type, membersByKey).findAll { DocMember member ->
            member.kind in [DocMemberKind.METHOD, DocMemberKind.ANNOTATION_ELEMENT]
        }.collect { DocMember member ->
            methodKey(member)
        }.findAll { it } as LinkedHashSet
    }

    private static Set<String> directFieldKeys(DocType type, Map<String, DocMember> membersByKey) {
        declaredMembers(type, membersByKey).findAll { DocMember member ->
            member.kind == DocMemberKind.FIELD
        }.collect { DocMember member ->
            member.name
        }.findAll { it } as LinkedHashSet
    }

    private static String methodKey(DocMember member) {
        if (member.id?.signature) {
            return member.id.signature
        }
        String params = (member.parameters ?: []).collect { parameter ->
            parameter.type?.qualifiedName ?: parameter.type?.rawText ?: parameter.type?.displayName ?: ""
        }.join(",")
        return "${member.name}(${params})"
    }

    private static MemberSummaryModel memberSummary(DocType owner, DocMember member) {
        String anchor = member.id?.effectiveAnchorId() ?: member.name
        new MemberSummaryModel(
                id: member.id,
                name: member.name,
                displayName: member.id?.displayId ?: member.name,
                url: "${typeUrl(owner)}#${anchor}",
                summary: summary(member.comment),
                metadata: member.metadata
        )
    }

    private static String summary(CommentDoc comment) {
        if (comment == null || !comment.summaryNodes) {
            return ""
        }
        return comment.summaryNodes.collect { node ->
            node.text ?: node.inlineTag?.rawText ?: ""
        }.join(" ").replaceAll(/\s+/, " ").trim()
    }

    private static String typeUrl(DocType type) {
        return "reference/${type.qualifiedName}.html"
    }
}

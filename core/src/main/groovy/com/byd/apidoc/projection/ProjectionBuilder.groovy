package com.byd.apidoc.projection

import com.byd.apidoc.comment.CommentDoc
import com.byd.apidoc.metadata.ApiAvailability
import com.byd.apidoc.metadata.ApiMetadata
import com.byd.apidoc.metadata.ApiValueRange
import com.byd.apidoc.metadata.ApiVisibility
import com.byd.apidoc.metadata.VisibilityPolicy
import com.byd.apidoc.model.DocCorpus
import com.byd.apidoc.model.DocId
import com.byd.apidoc.model.DocIdKind
import com.byd.apidoc.model.DocMember
import com.byd.apidoc.model.DocMemberKind
import com.byd.apidoc.model.DocPackage
import com.byd.apidoc.model.DocType
import com.byd.apidoc.model.DocTypeKind

class ProjectionBuilder {
    private static final int MAX_SEARCH_TOKENS = 32

    DocProjection build(DocCorpus corpus, VisibilityPolicy policy = new VisibilityPolicy()) {
        DocProjection projection = new DocProjection()
        if (corpus == null) {
            return projection
        }
        VisibilityPolicy effectivePolicy = policy ?: new VisibilityPolicy()
        List<DocPackage> packages = corpus.packages ?: []
        List<DocType> types = corpus.types ?: []
        List<DocMember> members = corpus.members ?: []
        Map<String, DocPackage> packageByName = packages.collectEntries { DocPackage pkg -> [(pkg.name ?: ""): pkg] }
        InheritedMemberResolver inheritedMemberResolver = new InheritedMemberResolver()

        List<DocType> visibleTypes = types.findAll { DocType type ->
            effectivePolicy.includes(type.metadata)
        }.sort { DocType type -> type.qualifiedName ?: type.name ?: "" }

        Map<String, List<DocType>> typesByPackage = visibleTypes.groupBy { DocType type ->
            type.packageName ?: ""
        }
        Map<String, List<DocMember>> visibleMembersByTypeKey = visibleTypes.collectEntries { DocType type ->
            [(typeKey(type)): visibleMembers(members, type, effectivePolicy)]
        }
        Map<String, List<String>> typePlatformsByKey = visibleTypes.collectEntries { DocType type ->
            [(typeKey(type)): typePlatforms(type, packageByName[type.packageName ?: ""])]
        }
        Map<String, List<String>> aggregatePlatformsByTypeKey = visibleTypes.collectEntries { DocType type ->
            List<String> typePlatforms = typePlatformsByKey[typeKey(type)] ?: []
            DocPackage ownerPackage = packageByName[type.packageName ?: ""]
            List<String> memberPlatforms = (visibleMembersByTypeKey[typeKey(type)] ?: []).collectMany { DocMember member ->
                memberPlatforms(member, typePlatforms, ownerPackage)
            }
            [(typeKey(type)): orderedPlatforms(typePlatforms + memberPlatforms)]
        }

        projection.pages.add(indexPage())
        projection.pages.add(packagesPage())
        projection.pages.add(classesPage())

        packages.sort { DocPackage pkg -> pkg.name ?: "" }.each { DocPackage pkg ->
            List<DocType> packageTypes = typesByPackage[pkg.name ?: ""] ?: []
            if (!packageTypes.isEmpty()) {
                List<String> packagePlatforms = packagePlatforms(pkg, packageTypes, aggregatePlatformsByTypeKey)
                PageModel page = packagePage(pkg)
                page.platforms = packagePlatforms
                projection.pages.add(page)
                projection.packagePages.add(packagePageModel(pkg, packageTypes, packagePlatforms, typePlatformsByKey, aggregatePlatformsByTypeKey))
                projection.search.add(packageSearchEntry(pkg, packagePlatforms))
            }
        }

        visibleTypes.each { DocType type ->
            List<DocMember> typeMembers = visibleMembersByTypeKey[typeKey(type)] ?: []
            List<InheritedMemberGroupModel> inheritedMemberGroups = inheritedMemberResolver.resolve(corpus, type, effectivePolicy)
            List<String> typePlatforms = typePlatformsByKey[typeKey(type)] ?: []
            PageModel page = typePage(type)
            page.platforms = typePlatforms
            projection.pages.add(page)
            projection.typePages.add(typePageModel(type, typeMembers, inheritedMemberGroups, typePlatforms, packageByName[type.packageName ?: ""]))
            projection.search.add(typeSearchEntry(type, typePlatforms))
            typeMembers.each { DocMember member ->
                projection.search.add(memberSearchEntry(type, member, memberPlatforms(member, typePlatforms, packageByName[type.packageName ?: ""])))
            }
        }

        projection.nav.addAll(navNodes(packages, typesByPackage, aggregatePlatformsByTypeKey))
        projection.platformFilter.platforms = orderedPlatforms(projection.search.collectMany { SearchEntry entry -> entry.platforms ?: [] })
        return projection
    }

    private static PageModel indexPage() {
        new PageModel(
                id: new DocId(kind: DocIdKind.PACKAGE, qualifiedName: "__index__"),
                kind: PageKind.INDEX,
                title: "API Reference",
                url: "index.html"
        )
    }

    private static PageModel packagesPage() {
        new PageModel(
                id: new DocId(kind: DocIdKind.PACKAGE, qualifiedName: "__packages__"),
                kind: PageKind.PACKAGES,
                title: "Packages",
                url: "packages.html"
        )
    }

    private static PageModel classesPage() {
        new PageModel(
                id: new DocId(kind: DocIdKind.TYPE, qualifiedName: "__classes__"),
                kind: PageKind.CLASSES,
                title: "Classes",
                url: "classes.html"
        )
    }

    private static PageModel packagePage(DocPackage pkg) {
        new PageModel(
                id: pkg.id,
                kind: PageKind.PACKAGE,
                title: pkg.name ?: "default",
                url: packageUrl(pkg.name),
                targetId: pkg.id,
                summary: summary(pkg.comment),
                metadata: pkg.metadata
        )
    }

    private static PageModel typePage(DocType type) {
        new PageModel(
                id: type.id,
                kind: PageKind.TYPE,
                title: type.name,
                url: typeUrl(type),
                targetId: type.id,
                summary: summary(type.comment),
                metadata: type.metadata
        )
    }

    private static List<NavNode> navNodes(List<DocPackage> packages, Map<String, List<DocType>> typesByPackage, Map<String, List<String>> typePlatformsByKey) {
        List<NavNode> roots = []
        packages.sort { DocPackage pkg -> pkg.name ?: "" }.each { DocPackage pkg ->
            List<DocType> types = typesByPackage[pkg.name ?: ""] ?: []
            if (types.isEmpty()) {
                return
            }
            NavNode packageNode = new NavNode(
                    label: pkg.name ?: "default",
                    kind: NavNodeKind.PACKAGE,
                    url: packageUrl(pkg.name),
                    targetId: pkg.id,
                    activePath: [pkg.name ?: "default"],
                    group: "package",
                    platforms: packagePlatforms(pkg, types, typePlatformsByKey)
            )
            packageNode.children.add(new NavNode(
                    label: "Overview",
                    kind: NavNodeKind.OVERVIEW,
                    url: packageUrl(pkg.name),
                    targetId: pkg.id,
                    activePath: [pkg.name ?: "default", "Overview"],
                    group: "overview",
                    platforms: packageNode.platforms
            ))
            groupedTypes(types).each { String groupName, List<DocType> grouped ->
                List<String> groupPlatforms = orderedPlatforms(grouped.collectMany { DocType type -> platformForTypeKey(type, typePlatformsByKey) })
                packageNode.children.add(new NavNode(
                        label: groupName,
                        kind: NavNodeKind.GROUP,
                        activePath: [pkg.name ?: "default", groupName],
                        group: groupKey(groupName),
                        platforms: groupPlatforms,
                        children: grouped.collect { DocType type ->
                            new NavNode(
                                    label: type.name,
                                    kind: NavNodeKind.TYPE,
                                    url: typeUrl(type),
                                    targetId: type.id,
                                    activePath: [pkg.name ?: "default", groupName, type.name ?: ""],
                                    group: groupKey(groupName),
                                    platforms: platformForTypeKey(type, typePlatformsByKey)
                            )
                        }
                ))
            }
            roots.add(packageNode)
        }
        return roots
    }

    private static Map<String, List<DocType>> groupedTypes(List<DocType> types) {
        LinkedHashMap<String, List<DocType>> groups = new LinkedHashMap<>()
        [
                "Interfaces" : { DocType type -> type.kind == DocTypeKind.INTERFACE },
                "Classes"    : { DocType type -> type.kind == DocTypeKind.CLASS },
                "Enums"      : { DocType type -> type.kind == DocTypeKind.ENUM },
                "Annotations": { DocType type -> type.kind == DocTypeKind.ANNOTATION },
                "Exceptions" : { DocType type -> type.kind == DocTypeKind.EXCEPTION },
                "Errors"     : { DocType type -> type.kind == DocTypeKind.ERROR },
                "Records"    : { DocType type -> type.kind == DocTypeKind.RECORD }
        ].each { String label, Closure<Boolean> matcher ->
            List<DocType> matches = types.findAll(matcher).sort { DocType type -> type.name ?: "" }
            if (!matches.isEmpty()) {
                groups[label] = matches
            }
        }
        return groups
    }

    private static SearchEntry packageSearchEntry(DocPackage pkg, List<String> platforms = []) {
        new SearchEntry(
                kind: SearchEntryKind.PACKAGE,
                label: pkg.name ?: "default",
                qualifiedName: pkg.name,
                packageName: pkg.name ?: "default",
                anchor: pkg.id?.effectiveAnchorId() ?: pkg.name ?: "default",
                url: packageUrl(pkg.name),
                summary: summary(pkg.comment),
                metadata: pkg.metadata,
                status: apiStatus(pkg.metadata),
                displaySignature: "package ${pkg.name ?: 'default'}",
                platforms: copyPlatforms(platforms),
                tokens: searchTokens("package", pkg.name, summary(pkg.comment))
        )
    }

    private static SearchEntry typeSearchEntry(DocType type, List<String> platforms = []) {
        new SearchEntry(
                kind: searchKind(type.kind),
                label: type.name,
                qualifiedName: type.qualifiedName,
                packageName: type.packageName,
                anchor: type.id?.effectiveAnchorId() ?: type.name,
                url: typeUrl(type),
                summary: summary(type.comment),
                metadata: type.metadata,
                status: apiStatus(type.metadata),
                displaySignature: typeDeclaration(type),
                platforms: copyPlatforms(platforms),
                tokens: searchTokens(type.name, type.qualifiedName, type.packageName, searchKind(type.kind).name(), summary(type.comment))
        )
    }

    private static SearchEntry memberSearchEntry(DocType owner, DocMember member, List<String> platforms = []) {
        String anchor = member.id?.effectiveAnchorId() ?: member.name
        new SearchEntry(
                kind: searchKind(member),
                label: member.name,
                qualifiedName: member.qualifiedName,
                packageName: owner.packageName,
                ownerName: owner.qualifiedName,
                anchor: anchor,
                url: "${typeUrl(owner)}#${anchor}",
                summary: summary(member.comment),
                metadata: member.metadata,
                status: apiStatus(member.metadata),
                displaySignature: memberDeclaration(member),
                platforms: copyPlatforms(platforms),
                tokens: searchTokens(member.name, member.qualifiedName, owner.name, owner.qualifiedName, owner.packageName, searchKind(member).name(), summary(member.comment))
        )
    }

    private static TypePageModel typePageModel(DocType type, List<DocMember> members, List<InheritedMemberGroupModel> inheritedMemberGroups = [], List<String> typePlatforms = [], DocPackage ownerPackage = null) {
        List<DocMember> effectiveMembers = members ?: []
        List<MemberDetailModel> details = effectiveMembers.collect { DocMember member ->
            List<String> platforms = memberPlatforms(member, typePlatforms, ownerPackage)
            new MemberDetailModel(
                    id: member.id,
                    name: member.name,
                    displayName: member.id?.displayId ?: member.name,
                    declaration: memberDeclaration(member),
                    kind: member.kind,
                    groupTitle: memberGroupTitle(member),
                    modifiers: new LinkedHashSet<String>(member.modifiers ?: []),
                    type: member.type,
                    returnType: member.returnType,
                    parameters: member.parameters ?: [],
                    throwsTypes: member.throwsTypes ?: [],
                    summary: summary(member.comment),
                    comment: member.comment,
                    metadata: member.metadata,
                    status: apiStatus(member.metadata),
                    platforms: platforms
            )
        }
        return new TypePageModel(
                id: type.id,
                typeKind: type.kind ?: DocTypeKind.CLASS,
                title: type.name,
                packageName: type.packageName,
                declaration: typeDeclaration(type),
                summary: summary(type.comment),
                comment: type.comment,
                metadata: type.metadata,
                platforms: copyPlatforms(typePlatforms),
                breadcrumbs: breadcrumbs(type, "class"),
                rightToc: rightToc(effectiveMembers),
                apiStatus: apiStatus(type.metadata),
                typeHeader: new AndroidTypeHeaderModel(
                        title: type.name,
                        packageName: type.packageName,
                        declaration: typeDeclaration(type),
                        inheritance: type.superType,
                        interfaces: type.interfaces ?: []
                ),
                inheritance: type.superType,
                interfaces: type.interfaces ?: [],
                memberGroups: memberGroups(type, effectiveMembers, typePlatforms, ownerPackage),
                memberDetails: details,
                inheritedMemberGroups: inheritedMemberGroups ?: []
        )
    }

    private static List<BreadcrumbModel> breadcrumbs(DocType type, String pageType = "class") {
        if (pageType == "class") {
            // 类页面: Packages Index → 包名 → 类名
            return [
                    new BreadcrumbModel(label: "Packages Index", url: "index.html", isHome: true),
                    new BreadcrumbModel(label: type.packageName ?: "default", url: packageUrl(type.packageName)),
                    new BreadcrumbModel(label: type.name, url: typeUrl(type), targetId: type.id)
            ]
        }
        return []
    }

    private static List<BreadcrumbModel> breadcrumbsForPackage(String packageName) {
        // 包页面: Packages Index → 包名
        return [
                new BreadcrumbModel(label: "Packages Index", url: "index.html", isHome: true),
                new BreadcrumbModel(label: packageName ?: "default", url: packageUrl(packageName))
        ]
    }

    private static List<TocEntryModel> rightToc(List<DocMember> members) {
        List<TocEntryModel> entries = []
        // Summary as 1级标题 (level 2)
        entries.add(new TocEntryModel(label: "Summary", anchor: "summary"))
        // Summary 下的各 group 作为 2级标题 (level 3)
        memberGroups(null, members).each { MemberGroupModel group ->
            entries.add(new TocEntryModel(label: group.title, anchor: anchorName(group.title), level: 3))
        }
        // 各 group 作为 1级标题 (level 2)，其下成员作为 2级标题 (level 3)
        memberGroups(null, members).each { MemberGroupModel group ->
            String detailAnchor = anchorName(group.title) + "-details"
            entries.add(new TocEntryModel(label: group.title, anchor: detailAnchor))
            group.members.each { MemberSummaryModel member ->
                String anchor = member.id?.effectiveAnchorId() ?: member.name
                entries.add(new TocEntryModel(label: member.name, anchor: anchor, level: 3))
            }
        }
        return entries
    }

    private static ApiStatusModel apiStatus(ApiMetadata metadata) {
        ApiMetadata effective = metadata ?: new ApiMetadata()
        new ApiStatusModel(
                hidden: effective.visibility in [ApiVisibility.HIDDEN, ApiVisibility.INTERNAL],
                removed: effective.availability == ApiAvailability.REMOVED,
                deprecated: effective.availability == ApiAvailability.DEPRECATED,
                since: effective.since,
                apiSince: effective.apiLevel,
                deprecatedSince: effective.deprecatedSince,
                removedSince: effective.removedSince,
                deprecatedMessage: effective.deprecated?.message,
                removedMessage: effective.removed?.message,
                pending: effective.pending,
                sdkExtensionSince: effective.sdkExtensionSince,
                permissions: new LinkedHashSet<String>(effective.permissions ?: []),
                nullability: effective.nullability,
                valueRanges: copyValueRanges(effective.valueRanges)
        )
    }

    private static List<ApiValueRange> copyValueRanges(List<ApiValueRange> ranges) {
        (ranges ?: []).collect { ApiValueRange range ->
            new ApiValueRange(kind: range.kind, from: range.from, to: range.to)
        }
    }

    private static List<MemberGroupModel> memberGroups(DocType type, List<DocMember> members, List<String> typePlatforms = [], DocPackage ownerPackage = null) {
        List<DocMember> effectiveMembers = members ?: []
        LinkedHashMap<String, Closure<Boolean>> groups = [
                "Nested Types" : { DocMember member -> false },
                "Fields"       : { DocMember member -> member.kind in [DocMemberKind.FIELD, DocMemberKind.ENUM_CONSTANT] },
                "Public Methods"     : { DocMember member -> member.kind in [DocMemberKind.CONSTRUCTOR, DocMemberKind.METHOD, DocMemberKind.ANNOTATION_ELEMENT] },
                "Record Components": { DocMember member -> member.kind == DocMemberKind.RECORD_COMPONENT }
        ]
        List<MemberGroupModel> result = []
        groups.each { String title, Closure<Boolean> matcher ->
            List<DocMember> grouped = effectiveMembers.findAll(matcher)
            if (!grouped.isEmpty()) {
                List<MemberSummaryModel> summaries = grouped.collect { DocMember member ->
                    List<String> platforms = memberPlatforms(member, typePlatforms, ownerPackage)
                    new MemberSummaryModel(
                            id: member.id,
                            name: member.name,
                            displayName: member.id?.displayId ?: member.name,
                            url: type == null ? null : "${typeUrl(type)}#${member.id?.effectiveAnchorId() ?: member.name}",
                            modifierAndType: memberModifierAndType(member),
                            kind: searchKind(member).name().toLowerCase(Locale.ROOT),
                            summary: summary(member.comment),
                            metadata: member.metadata,
                            status: apiStatus(member.metadata),
                            platforms: platforms
                    )
                }
                if (title == "Fields") {
                    summaries = summaries.sort { MemberSummaryModel a, MemberSummaryModel b ->
                        boolean aConst = a.kind == "constant"
                        boolean bConst = b.kind == "constant"
                        if (aConst != bConst) return aConst ? -1 : 1
                        return (a.name ?: "").compareTo(b.name ?: "")
                    }
                }
                result.add(new MemberGroupModel(
                        title: title,
                        kind: title.toUpperCase(Locale.ROOT).replaceAll(/\s+/, "_"),
                        platforms: orderedPlatforms(summaries.collectMany { MemberSummaryModel member -> member.platforms ?: [] }),
                        members: summaries
                ))
            }
        }
        return result
    }

    private static PackagePageModel packagePageModel(DocPackage pkg, List<DocType> types, List<String> platforms, Map<String, List<String>> typePlatformsByKey, Map<String, List<String>> aggregatePlatformsByTypeKey) {
        new PackagePageModel(
                id: pkg.id,
                packageName: pkg.name ?: "default",
                url: packageUrl(pkg.name),
                summary: summary(pkg.comment),
                metadata: pkg.metadata,
                platforms: copyPlatforms(platforms),
                breadcrumbs: breadcrumbsForPackage(pkg.name),
                typeGroups: groupedTypes(types).collect { String label, List<DocType> grouped ->
                    List<TypePageModel> typeModels = grouped.collect { DocType type ->
                        typePageModel(type, [], [], platformForTypeKey(type, typePlatformsByKey), pkg)
                    }
                    new PackageTypeGroupModel(
                            label: label,
                            kind: groupKey(label),
                            platforms: orderedPlatforms(grouped.collectMany { DocType type -> platformForTypeKey(type, aggregatePlatformsByTypeKey) }),
                            types: typeModels
                    )
                } as List<PackageTypeGroupModel>
        )
    }

    private static String typeDeclaration(DocType type) {
        String modifiers = type.modifiers?.join(" ") ?: ""
        String keyword = [
                (DocTypeKind.INTERFACE) : "interface",
                (DocTypeKind.ENUM)      : "enum",
                (DocTypeKind.ANNOTATION): "@interface",
                (DocTypeKind.RECORD)    : "record"
        ][type.kind] ?: "class"
        List typeParameters = type.typeParameters ?: []
        List interfaces = type.interfaces ?: []
        String typeParams = typeParameters ? "<${typeParameters*.name.join(', ')}>" : ""
        String extendsPart = type.superType?.qualifiedName && type.superType.qualifiedName != "java.lang.Object" ? " extends ${type.superType.displayName}" : ""
        String implementsPart = interfaces ? " implements ${interfaces*.displayName.join(', ')}" : ""
        return [modifiers, keyword, "${type.name}${typeParams}${extendsPart}${implementsPart}"]
                .findAll { it != null && !it.toString().isEmpty() }
                .join(" ")
    }

    private static String memberGroupTitle(DocMember member) {
        if (member.kind in [DocMemberKind.FIELD, DocMemberKind.ENUM_CONSTANT]) return "Fields"
        if (member.kind in [DocMemberKind.CONSTRUCTOR, DocMemberKind.METHOD, DocMemberKind.ANNOTATION_ELEMENT]) return "Public Methods"
        if (member.kind == DocMemberKind.RECORD_COMPONENT) return "Record Components"
        return ""
    }

    private static String memberDeclaration(DocMember member) {
        String modifiers = member.modifiers?.join(" ") ?: ""
        if (member.kind == DocMemberKind.CONSTRUCTOR || member.kind == DocMemberKind.METHOD || member.kind == DocMemberKind.ANNOTATION_ELEMENT) {
            String returnType = member.kind == DocMemberKind.CONSTRUCTOR ? "" : member.returnType?.displayName
            List parameters = member.parameters ?: []
            List throwsTypes = member.throwsTypes ?: []
            String params = parameters.collect { parameter -> "${parameter.type?.displayName ?: ''} ${parameter.name}".trim() }.join(", ")
            String throwsPart = throwsTypes ? " throws ${throwsTypes*.displayName.join(', ')}" : ""
            return [modifiers, returnType, "${member.name}(${params})${throwsPart}"]
                    .findAll { it != null && !it.toString().isEmpty() }
                    .join(" ")
        }
        return [modifiers, member.type?.displayName, member.name]
                .findAll { it != null && !it.toString().isEmpty() }
                .join(" ")
    }

    private static String memberModifierAndType(DocMember member) {
        String modifiers = (member.modifiers ?: []).findAll { it != "public" }.join(" ")
        String type = ""
        if (member.kind == DocMemberKind.CONSTRUCTOR) {
            type = "constructor"
        } else if (member.kind in [DocMemberKind.METHOD, DocMemberKind.ANNOTATION_ELEMENT]) {
            type = member.returnType?.displayName ?: ""
        } else {
            type = member.type?.displayName ?: ""
        }
        return [modifiers, type].findAll { it != null && !it.toString().isEmpty() }.join(" ")
    }

    private static List<DocMember> visibleMembers(List<DocMember> members, DocType owner, VisibilityPolicy policy) {
        Set<String> memberKeys = (owner.memberIds ?: []).collect { DocId id -> id?.stableKey() }.findAll { it } as Set
        (members ?: []).findAll { DocMember member ->
            memberKeys.contains(member.id?.stableKey()) && policy.includes(member.metadata)
        }.sort { DocMember member -> "${member.kind}:${member.name}:${member.id?.signature ?: ''}" }
    }

    private static SearchEntryKind searchKind(DocTypeKind kind) {
        switch (kind) {
            case DocTypeKind.INTERFACE:
                return SearchEntryKind.INTERFACE
            case DocTypeKind.ENUM:
                return SearchEntryKind.ENUM
            case DocTypeKind.ANNOTATION:
                return SearchEntryKind.ANNOTATION
            case DocTypeKind.RECORD:
                return SearchEntryKind.RECORD
            case DocTypeKind.EXCEPTION:
                return SearchEntryKind.EXCEPTION
            case DocTypeKind.ERROR:
                return SearchEntryKind.ERROR
            default:
                return SearchEntryKind.CLASS
        }
    }

    private static SearchEntryKind searchKind(DocMember member) {
        if (member.kind == DocMemberKind.CONSTRUCTOR) return SearchEntryKind.CONSTRUCTOR
        if (member.kind == DocMemberKind.METHOD || member.kind == DocMemberKind.ANNOTATION_ELEMENT) return SearchEntryKind.METHOD
        if (member.kind == DocMemberKind.ENUM_CONSTANT) return SearchEntryKind.CONSTANT
        if (isConstant(member)) return SearchEntryKind.CONSTANT
        return SearchEntryKind.FIELD
    }

    private static boolean isConstant(DocMember member) {
        Set<String> modifiers = member.modifiers ?: [] as Set
        return member.kind == DocMemberKind.ENUM_CONSTANT ||
                (member.kind == DocMemberKind.FIELD && modifiers.contains("static") && modifiers.contains("final"))
    }

    private static String summary(CommentDoc comment) {
        if (comment == null || !comment.summaryNodes) {
            return ""
        }
        return comment.summaryNodes.collect { node ->
            node.text ?: node.inlineTag?.rawText ?: ""
        }.join(" ").replaceAll(/\s+/, " ").trim()
    }

    private static String anchorName(String label) {
        label.toLowerCase(Locale.ROOT).replaceAll(/[^a-z0-9]+/, "-").replaceAll(/^-|-$/, "")
    }

    private static String groupKey(String label) {
        String normalized = anchorName(label ?: "")
        [
                "annotations": "annotation",
                "interfaces" : "interface",
                "classes"    : "class",
                "enums"      : "enum",
                "exceptions" : "exception",
                "errors"     : "error",
                "records"    : "record"
        ][normalized] ?: normalized
    }

    private static List<String> typePlatforms(DocType type, DocPackage ownerPackage) {
        List<String> own = copyPlatforms(type?.metadata?.supportedPlatforms)
        if (own) return own
        return copyPlatforms(ownerPackage?.metadata?.supportedPlatforms)
    }

    private static List<String> memberPlatforms(DocMember member, List<String> ownerTypePlatforms, DocPackage ownerPackage) {
        List<String> own = copyPlatforms(member?.metadata?.supportedPlatforms)
        if (own) return own
        if (ownerTypePlatforms) return copyPlatforms(ownerTypePlatforms)
        return copyPlatforms(ownerPackage?.metadata?.supportedPlatforms)
    }

    private static List<String> packagePlatforms(DocPackage pkg, List<DocType> types, Map<String, List<String>> typePlatformsByKey) {
        List<String> explicit = copyPlatforms(pkg?.metadata?.supportedPlatforms)
        if (explicit) return explicit
        return orderedPlatforms((types ?: []).collectMany { DocType type -> platformForTypeKey(type, typePlatformsByKey) })
    }

    private static List<String> platformForTypeKey(DocType type, Map<String, List<String>> typePlatformsByKey) {
        copyPlatforms(typePlatformsByKey[typeKey(type)])
    }

    private static String typeKey(DocType type) {
        type?.id?.stableKey() ?: type?.qualifiedName ?: type?.name ?: ""
    }

    private static List<String> copyPlatforms(Collection platforms) {
        orderedPlatforms(platforms)
    }

    private static List<String> orderedPlatforms(Collection platforms) {
        LinkedHashSet<String> values = new LinkedHashSet<>()
        (platforms ?: []).each { Object value ->
            if (value instanceof Collection) {
                values.addAll(orderedPlatforms((Collection) value))
            } else if (value != null && value.toString().trim()) {
                values.add(value.toString())
            }
        }
        return values as List
    }

    private static List<String> searchTokens(String... values) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>()
        values.findAll { it }.each { String value ->
            value.split(/[^A-Za-z0-9_]+/).findAll { it }.each { String token ->
                addSearchToken(tokens, token)
                splitCamelCase(token).each { String part ->
                    addSearchToken(tokens, part)
                }
            }
        }
        return tokens.take(MAX_SEARCH_TOKENS) as List
    }

    private static void addSearchToken(LinkedHashSet<String> tokens, String token) {
        if (!token || tokens.size() >= MAX_SEARCH_TOKENS) {
            return
        }
        tokens.add(token)
        String lower = token.toLowerCase(Locale.ROOT)
        if (tokens.size() < MAX_SEARCH_TOKENS) {
            tokens.add(lower)
        }
    }

    private static List<String> splitCamelCase(String token) {
        token.replaceAll(/([a-z0-9])([A-Z])/, '$1 $2')
                .split(/\s+/)
                .findAll { String part -> part && part != token }
    }

    private static String packageUrl(String packageName) {
        return "package/${packageName ?: 'default'}.html"
    }

    private static String typeUrl(DocType type) {
        return "reference/${type.qualifiedName}.html"
    }
}
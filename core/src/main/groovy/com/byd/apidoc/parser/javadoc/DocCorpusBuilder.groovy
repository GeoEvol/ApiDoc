package com.byd.apidoc.parser.javadoc

import com.byd.apidoc.comment.BlockTag
import com.byd.apidoc.comment.BlockTagKind
import com.byd.apidoc.comment.CommentDoc
import com.byd.apidoc.comment.CommentNode
import com.byd.apidoc.comment.CommentNodeKind
import com.byd.apidoc.comment.DeprecatedTag
import com.byd.apidoc.comment.InlineTag
import com.byd.apidoc.comment.InlineTagKind
import com.byd.apidoc.comment.ParamTag
import com.byd.apidoc.comment.ReturnTag
import com.byd.apidoc.comment.SeeTag
import com.byd.apidoc.comment.SinceTag
import com.byd.apidoc.comment.ThrowsTag
import com.byd.apidoc.doclet.BuildContext
import com.byd.apidoc.metadata.ApiAvailability
import com.byd.apidoc.metadata.ApiMetadata
import com.byd.apidoc.metadata.ApiValueRange
import com.byd.apidoc.metadata.ApiVisibility
import com.byd.apidoc.metadata.DeprecatedMetadata
import com.byd.apidoc.metadata.MetadataSource
import com.byd.apidoc.metadata.RemovedMetadata
import com.byd.apidoc.model.DocAnnotation
import com.byd.apidoc.model.DocCorpus
import com.byd.apidoc.model.DocId
import com.byd.apidoc.model.DocIdKind
import com.byd.apidoc.model.DocMember
import com.byd.apidoc.model.DocMemberKind
import com.byd.apidoc.model.DocPackage
import com.byd.apidoc.model.DocParameter
import com.byd.apidoc.model.DocType
import com.byd.apidoc.model.DocTypeKind
import com.byd.apidoc.model.LinkRef
import com.byd.apidoc.model.LinkRefKind
import com.byd.apidoc.model.TypeParameterRef
import com.byd.apidoc.model.TypeRef
import com.byd.apidoc.model.TypeRefKind
import com.byd.apidoc.parser.javadoc.converter.DocTreeExtractor
import com.sun.source.doctree.AttributeTree
import com.sun.source.doctree.DeprecatedTree
import com.sun.source.doctree.DocCommentTree
import com.sun.source.doctree.DocTree
import com.sun.source.doctree.EndElementTree
import com.sun.source.doctree.EntityTree
import com.sun.source.doctree.InheritDocTree
import com.sun.source.doctree.LinkTree
import com.sun.source.doctree.LiteralTree
import com.sun.source.doctree.ParamTree
import com.sun.source.doctree.ReturnTree
import com.sun.source.doctree.SeeTree
import com.sun.source.doctree.SinceTree
import com.sun.source.doctree.StartElementTree
import com.sun.source.doctree.TextTree
import com.sun.source.doctree.ThrowsTree
import com.sun.source.doctree.UnknownBlockTagTree
import com.sun.source.doctree.UnknownInlineTagTree

import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.TypeParameterElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVariable
import javax.lang.model.type.WildcardType

class DocCorpusBuilder {
    private final BuildContext context

    DocCorpusBuilder(BuildContext context) {
        this.context = context
    }

    DocCorpus build() {
        DocCorpus corpus = new DocCorpus()
        List<TypeElement> types = collectTypeElements()
        Map<String, DocPackage> packages = new TreeMap<>()

        types.each { TypeElement typeElement ->
            PackageElement packageElement = context.elements.getPackageOf(typeElement)
            String packageName = packageElement?.qualifiedName?.toString() ?: ""
            if (context.config?.isClassExcluded(typeElement.qualifiedName.toString(), packageName)) {
                return
            }
            DocPackage docPackage = packages.computeIfAbsent(packageName) {
                DocId packageId = packageId(packageName)
                new DocPackage(
                        id: packageId,
                        name: packageName,
                        comment: comment(packageElement),
                        metadata: metadata(packageElement)
                )
            }

            DocType type = type(typeElement, corpus)
            corpus.types.add(type)
            corpus.comments[type.id.stableKey()] = type.comment
            corpus.annotations[type.id.stableKey()] = type.annotations
            corpus.metadata[type.id.stableKey()] = type.metadata
            docPackage.typeIds.add(type.id)
        }

        corpus.packages.addAll(packages.values())
        return corpus
    }

    private DocType type(TypeElement element, DocCorpus corpus) {
        String qualifiedName = element.qualifiedName.toString()
        DocId id = typeId(qualifiedName)
        DocType type = new DocType(
                id: id,
                name: element.simpleName.toString(),
                qualifiedName: qualifiedName,
                packageName: context.elements.getPackageOf(element)?.qualifiedName?.toString() ?: "",
                kind: typeKind(element),
                modifiers: element.modifiers.collect { it.toString() } as LinkedHashSet,
                typeParameters: typeParameters(element.typeParameters),
                superType: typeRef(element.superclass),
                interfaces: element.interfaces.collect { TypeMirror mirror -> typeRef(mirror) },
                annotations: annotations(element),
                comment: comment(element),
                metadata: metadata(element)
        )
        element.enclosedElements.each { Element child ->
            if (child instanceof TypeElement) {
                type.nestedTypeIds.add(typeId(((TypeElement) child).qualifiedName.toString()))
            } else if (isDocumentableMember(child)) {
                DocMember member = member(element, child)
                corpus.members.add(member)
                corpus.comments[member.id.stableKey()] = member.comment
                corpus.annotations[member.id.stableKey()] = member.annotations
                corpus.metadata[member.id.stableKey()] = member.metadata
                type.memberIds.add(member.id)
            }
        }
        return type
    }

    private DocMember member(TypeElement owner, Element element) {
        DocMemberKind kind = memberKind(element)
        DocId id = memberId(owner, element, kind)
        DocMember member = new DocMember(
                id: id,
                ownerId: typeId(owner.qualifiedName.toString()),
                name: memberName(owner, element, kind),
                qualifiedName: "${owner.qualifiedName}.${element.simpleName}",
                kind: kind,
                modifiers: element.modifiers.collect { it.toString() } as LinkedHashSet,
                annotations: annotations(element),
                comment: comment(element),
                metadata: metadata(element)
        )
        if (element instanceof VariableElement) {
            member.type = typeRef(element.asType())
            member.constantValue = element.constantValue
        }
        if (element instanceof ExecutableElement) {
            ExecutableElement executable = (ExecutableElement) element
            List<? extends VariableElement> parameters = new ArrayList<>(executable.parameters)
            member.returnType = kind == DocMemberKind.CONSTRUCTOR ? new TypeRef(kind: TypeRefKind.VOID, displayName: "void") : typeRef(executable.returnType)
            member.typeParameters = typeParameters(executable.typeParameters)
            member.parameters = parameters.collect { VariableElement parameter ->
                new DocParameter(
                        name: parameter.simpleName.toString(),
                        type: typeRef(parameter.asType()),
                        varargs: executable.varArgs && parameter == parameters.last(),
                        annotations: annotations(parameter)
                )
            }
            member.throwsTypes = executable.thrownTypes.collect { TypeMirror mirror -> typeRef(mirror) }
            member.defaultValue = executable.defaultValue?.toString()
        }
        return member
    }

    private CommentDoc comment(Element element) {
        if (element == null) {
            return null
        }
        DocCommentTree tree = context.docTrees.getDocCommentTree(element)
        if (tree == null) {
            return new CommentDoc()
        }
        CommentDoc comment = new CommentDoc(
                rawText: tree.toString(),
                summaryNodes: nodes(tree.firstSentence),
                bodyNodes: nodes(tree.fullBody),
                blockTags: tree.blockTags.collect { DocTree tag -> blockTag(tag) }
        )
        comment.inlineNodes.addAll(inlineTags(tree.firstSentence))
        comment.inlineNodes.addAll(inlineTags(tree.fullBody))
        tree.blockTags.each { DocTree tag ->
            comment.inlineNodes.addAll(inlineTags(blockTagBody(tag)))
        }
        return comment
    }

    private BlockTag blockTag(DocTree tag) {
        if (tag instanceof ParamTree) {
            ParamTree tree = (ParamTree) tag
            return new ParamTag(
                    kind: BlockTagKind.PARAM,
                    name: "param",
                    rawName: "@param",
                    key: tree.name?.toString(),
                    parameterName: tree.name?.toString(),
                    typeParameter: tree.typeParameter,
                    body: nodes(tree.description),
                    rawText: DocTreeExtractor.render(tree.description),
                    known: true
            )
        }
        if (tag instanceof ReturnTree) {
            ReturnTree tree = (ReturnTree) tag
            return new ReturnTag(kind: BlockTagKind.RETURN, name: "return", rawName: "@return", body: nodes(tree.description), rawText: DocTreeExtractor.render(tree.description), known: true)
        }
        if (tag instanceof ThrowsTree) {
            ThrowsTree tree = (ThrowsTree) tag
            String exceptionName = tree.exceptionName?.toString()
            return new ThrowsTag(kind: BlockTagKind.THROWS, name: "throws", rawName: "@throws", key: exceptionName, exceptionName: exceptionName, exceptionRef: unresolved(exceptionName), body: nodes(tree.description), rawText: DocTreeExtractor.render(tree.description), known: true)
        }
        if (tag instanceof SeeTree) {
            SeeTree tree = (SeeTree) tag
            String text = DocTreeExtractor.render(tree.reference)
            return new SeeTag(kind: BlockTagKind.SEE, name: "see", rawName: "@see", reference: unresolved(text), body: nodes(tree.reference), rawText: text, known: true)
        }
        if (tag instanceof SinceTree) {
            SinceTree tree = (SinceTree) tag
            String version = DocTreeExtractor.render(tree.body)
            return new SinceTag(kind: BlockTagKind.SINCE, name: "since", rawName: "@since", version: version, body: nodes(tree.body), rawText: version, known: true)
        }
        if (tag instanceof DeprecatedTree) {
            DeprecatedTree tree = (DeprecatedTree) tag
            String message = DocTreeExtractor.render(tree.body)
            return new DeprecatedTag(kind: BlockTagKind.DEPRECATED, name: "deprecated", rawName: "@deprecated", message: message, body: nodes(tree.body), rawText: message, known: true)
        }
        if (tag instanceof UnknownBlockTagTree) {
            UnknownBlockTagTree tree = (UnknownBlockTagTree) tag
            String name = tree.tagName
            return new BlockTag(
                    kind: BlockTagKind.CUSTOM,
                    name: name,
                    rawName: "@${name}",
                    body: nodes(tree.content),
                    rawText: DocTreeExtractor.render(tree.content),
                    known: false
            )
        }
        return new BlockTag(kind: BlockTagKind.UNKNOWN, name: tag.kind?.tagName, rawName: "@${tag.kind?.tagName}", rawText: tag.toString(), known: false)
    }

    private List<CommentNode> nodes(List<? extends DocTree> trees) {
        return (trees ?: []).collect { DocTree tree ->
            if (tree instanceof TextTree) {
                return new CommentNode(kind: CommentNodeKind.TEXT, text: ((TextTree) tree).body)
            }
            if (tree instanceof EntityTree) {
                return new CommentNode(kind: CommentNodeKind.ENTITY, text: ((EntityTree) tree).name?.toString())
            }
            CommentNode htmlNode = htmlNode(tree)
            if (htmlNode != null) {
                return htmlNode
            }
            InlineTag inline = inlineTag(tree)
            if (inline != null) {
                return new CommentNode(kind: CommentNodeKind.INLINE_TAG, inlineTag: inline, text: inline.rawText)
            }
            return new CommentNode(kind: CommentNodeKind.TEXT, text: tree.toString())
        }
    }

    private static CommentNode htmlNode(DocTree tree) {
        if (tree instanceof StartElementTree) {
            StartElementTree start = (StartElementTree) tree
            String name = normalizeHtmlName(start.name?.toString())
            if (!name) {
                return null
            }
            return new CommentNode(
                    kind: CommentNodeKind.HTML,
                    text: tree.toString(),
                    htmlName: name,
                    htmlAttributes: htmlAttributes(start.attributes),
                    htmlStart: true,
                    htmlSelfClosing: start.selfClosing
            )
        }
        if (tree instanceof EndElementTree) {
            EndElementTree end = (EndElementTree) tree
            String name = normalizeHtmlName(end.name?.toString())
            if (!name) {
                return null
            }
            return new CommentNode(
                    kind: CommentNodeKind.HTML,
                    text: tree.toString(),
                    htmlName: name,
                    htmlEnd: true
            )
        }
        return null
    }

    private static Map<String, String> htmlAttributes(List<? extends DocTree> attributes) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>()
        (attributes ?: []).each { DocTree attributeTree ->
            if (!(attributeTree instanceof AttributeTree)) {
                return
            }
            AttributeTree attribute = (AttributeTree) attributeTree
            String name = attribute.name?.toString()
            if (!name) {
                return
            }
            String value = attribute.valueKind == AttributeTree.ValueKind.EMPTY ? "" : htmlAttributeValue(attribute.value)
            result[name] = value
        }
        return result
    }

    private static String htmlAttributeValue(List<? extends DocTree> valueTrees) {
        StringBuilder out = new StringBuilder()
        (valueTrees ?: []).each { DocTree valueTree ->
            if (valueTree instanceof TextTree) {
                out << ((TextTree) valueTree).body
            } else if (valueTree instanceof EntityTree) {
                out << htmlAttributeEntityValue(((EntityTree) valueTree).name?.toString())
            } else {
                out << valueTree.toString()
            }
        }
        return out.toString()
    }

    private static String htmlAttributeEntityValue(String value) {
        String name = value?.trim()
        if (!name) {
            return ""
        }
        switch (name) {
            case "amp":
                return "&"
            case "lt":
                return "<"
            case "gt":
                return ">"
            case "quot":
                return "\""
            case "apos":
                return "'"
        }
        def decimal = name =~ /^#([0-9]+)$/
        if (decimal.matches()) {
            return codePointToString(decimal.group(1) as int, value)
        }
        def hex = name =~ /^#x([0-9A-Fa-f]+)$/
        if (hex.matches()) {
            return codePointToString(Integer.parseInt(hex.group(1), 16), value)
        }
        return "&${name};"
    }

    private static String codePointToString(int codePoint, String fallback) {
        try {
            if (Character.isValidCodePoint(codePoint)) {
                return new String(Character.toChars(codePoint))
            }
        } catch (Exception ignored) {
        }
        return "&${fallback};"
    }

    private static String normalizeHtmlName(String value) {
        String name = value?.trim()
        if (!name || !(name ==~ /[A-Za-z][A-Za-z0-9:-]*/)) {
            return ""
        }
        return name.toLowerCase(Locale.ROOT)
    }

    private List<InlineTag> inlineTags(List<? extends DocTree> trees) {
        List<InlineTag> tags = []
        (trees ?: []).each { DocTree tree ->
            InlineTag tag = inlineTag(tree)
            if (tag != null) {
                tags.add(tag)
            }
            if (tree instanceof LinkTree) {
                tags.addAll(inlineTags(((LinkTree) tree).label))
            } else if (tree instanceof UnknownInlineTagTree) {
                tags.addAll(inlineTags(((UnknownInlineTagTree) tree).content))
            }
        }
        return tags
    }

    private InlineTag inlineTag(DocTree tree) {
        if (tree instanceof LinkTree) {
            LinkTree linkTree = (LinkTree) tree
            String reference = linkTree.reference?.signature
            return new InlineTag(
                    kind: linkTree.kind == DocTree.Kind.LINK_PLAIN ? InlineTagKind.LINKPLAIN : InlineTagKind.LINK,
                    name: linkTree.kind == DocTree.Kind.LINK_PLAIN ? "linkplain" : "link",
                    rawName: linkTree.kind == DocTree.Kind.LINK_PLAIN ? "{@linkplain}" : "{@link}",
                    reference: unresolved(reference),
                    label: nodes(linkTree.label),
                    rawText: DocTreeExtractor.render(tree),
                    known: true
            )
        }
        if (tree instanceof LiteralTree) {
            LiteralTree literal = (LiteralTree) tree
            boolean code = literal.kind == DocTree.Kind.CODE
            return new InlineTag(
                    kind: code ? InlineTagKind.CODE : InlineTagKind.LITERAL,
                    name: code ? "code" : "literal",
                    rawName: code ? "{@code}" : "{@literal}",
                    body: literal.body?.body,
                    rawText: DocTreeExtractor.render(tree),
                    known: true
            )
        }
        if (tree instanceof InheritDocTree) {
            return new InlineTag(
                    kind: InlineTagKind.INHERIT_DOC,
                    name: "inheritDoc",
                    rawName: "{@inheritDoc}",
                    rawText: DocTreeExtractor.render(tree),
                    known: true
            )
        }
        if (tree instanceof UnknownInlineTagTree) {
            UnknownInlineTagTree unknown = (UnknownInlineTagTree) tree
            return new InlineTag(
                    kind: InlineTagKind.CUSTOM,
                    name: unknown.tagName,
                    rawName: "{@${unknown.tagName}}",
                    body: DocTreeExtractor.render(unknown.content),
                    rawText: tree.toString(),
                    known: false
            )
        }
        return null
    }

    private List<? extends DocTree> blockTagBody(DocTree tag) {
        if (tag instanceof ParamTree) return ((ParamTree) tag).description
        if (tag instanceof ReturnTree) return ((ReturnTree) tag).description
        if (tag instanceof ThrowsTree) return ((ThrowsTree) tag).description
        if (tag instanceof SeeTree) return ((SeeTree) tag).reference
        if (tag instanceof SinceTree) return ((SinceTree) tag).body
        if (tag instanceof DeprecatedTree) return ((DeprecatedTree) tag).body
        if (tag instanceof UnknownBlockTagTree) return ((UnknownBlockTagTree) tag).content
        return []
    }

    private ApiMetadata metadata(Element element) {
        ApiMetadata metadata = new ApiMetadata(visibility: visibility(element))
        CommentDoc comment = comment(element)
        comment?.blockTags?.each { BlockTag tag ->
            if (!tag.name) return
            metadata.sourceTags.add(tag.name)
            switch (tag.name) {
                case "hide":
                    metadata.visibility = ApiVisibility.HIDDEN
                    break
                case "removed":
                    metadata.availability = ApiAvailability.REMOVED
                    metadata.removed = new RemovedMetadata(fromJavadocTag: true, message: tag.rawText)
                    break
                case "pending":
                    metadata.pending = true
                    break
                case "deprecated":
                    if (metadata.availability != ApiAvailability.REMOVED) {
                        metadata.availability = ApiAvailability.DEPRECATED
                    }
                    metadata.deprecated = metadata.deprecated ?: new DeprecatedMetadata()
                    metadata.deprecated.fromJavadocTag = true
                    metadata.deprecated.message = tag.rawText
                    break
                case "since":
                    metadata.since = tag.rawText
                    break
                case "apiSince":
                    metadata.since = metadata.since ?: tag.rawText
                    if (tag.rawText?.isInteger()) {
                        metadata.apiLevel = tag.rawText as Integer
                    }
                    break
                case "sdkExtSince":
                    metadata.sdkExtensionSince = tag.rawText
                    break
                case "deprecatedSince":
                    metadata.deprecatedSince = tag.rawText
                    break
                case "removedSince":
                    metadata.removedSince = tag.rawText
                    break
            }
        }
        annotations(element).each { DocAnnotation annotation ->
            String annotationName = annotation.qualifiedName ?: annotation.name
            if (annotationName) {
                metadata.sourceAnnotations.add(annotationName)
            }
            if (annotation.qualifiedName == "java.lang.Deprecated" || annotation.name == "Deprecated") {
                if (metadata.availability != ApiAvailability.REMOVED) {
                    metadata.availability = ApiAvailability.DEPRECATED
                }
                metadata.deprecated = metadata.deprecated ?: new DeprecatedMetadata()
                metadata.deprecated.fromAnnotation = true
            }
            if (matchesAnnotation(annotation, "RequiresPermission")) {
                metadata.permissions.addAll(permissionValues(annotation.values))
            }
            if (matchesAnnotation(annotation, "Supported")) {
                List<String> platforms = supportedPlatformValues(annotation.values)
                if (platforms) {
                    metadata.supportedPlatforms = platforms
                    metadata.metadataSources.add(new MetadataSource(
                            kind: "ANNOTATION",
                            name: annotation.name ?: "Supported",
                            property: "platforms",
                            rawValue: platforms
                    ))
                }
            }
            if (matchesAnnotation(annotation, "IntRange") || matchesAnnotation(annotation, "FloatRange")) {
                ApiValueRange range = valueRange(annotation)
                if (range != null) {
                    metadata.valueRanges.add(range)
                }
            }
            if (matchesAnnotation(annotation, "NonNull") || matchesAnnotation(annotation, "Nonnull")) {
                metadata.nullability = "NONNULL"
            }
            if (matchesAnnotation(annotation, "Nullable")) {
                metadata.nullability = "NULLABLE"
            }
        }
        return metadata
    }

    private List<DocAnnotation> annotations(Element element) {
        return element?.annotationMirrors?.collect { AnnotationMirror annotation ->
            Map valuesWithDefaults = context?.elements
                    ? context.elements.getElementValuesWithDefaults(annotation)
                    : annotation.elementValues
            new DocAnnotation(
                    name: annotation.annotationType.asElement().simpleName.toString(),
                    qualifiedName: annotation.annotationType.toString(),
                    values: valuesWithDefaults.collectEntries { entry ->
                        [(entry.key.simpleName.toString()): scalarAnnotationValue(entry.value)]
                    },
                    link: unresolved(annotation.annotationType.toString())
            )
        } ?: []
    }

    private static Object scalarAnnotationValue(Object value) {
        if (value instanceof AnnotationValue) {
            return scalarAnnotationValue(((AnnotationValue) value).value)
        }
        if (value == null || value instanceof Number || value instanceof Boolean || value instanceof CharSequence) {
            return value
        }
        if (value instanceof Collection) {
            return value.collect { scalarAnnotationValue(it) }
        }
        return value.toString()
    }

    private static boolean matchesAnnotation(DocAnnotation annotation, String simpleName) {
        annotation?.name == simpleName || annotation?.qualifiedName?.endsWith(".${simpleName}") || annotation?.qualifiedName == simpleName
    }

    private static Set<String> permissionValues(Map<String, Object> values) {
        LinkedHashSet<String> permissions = new LinkedHashSet<>()
        ["value", "allOf", "anyOf"].each { String key ->
            Object value = values?.get(key)
            collectStrings(value, permissions)
        }
        return permissions
    }

    private static List<String> supportedPlatformValues(Map<String, Object> values) {
        LinkedHashSet<String> platforms = new LinkedHashSet<>()
        collectStrings(values?.get("platforms"), platforms)
        return platforms as List
    }

    private static void collectStrings(Object value, Set<String> result) {
        if (value == null) {
            return
        }
        if (value instanceof Collection) {
            value.each { collectStrings(it, result) }
            return
        }
        String text = value.toString()
        if (!text.isEmpty()) {
            result.add(text)
        }
    }

    private static ApiValueRange valueRange(DocAnnotation annotation) {
        Object from = firstPresent(annotation.values, ["from", "min"])
        Object to = firstPresent(annotation.values, ["to", "max"])
        if (from == null && to == null) {
            return null
        }
        return new ApiValueRange(kind: annotation.name, from: from, to: to)
    }

    private static Object firstPresent(Map<String, Object> values, List<String> keys) {
        for (String key : keys) {
            if (values?.containsKey(key)) {
                return values[key]
            }
        }
        return null
    }

    private TypeRef typeRef(TypeMirror mirror) {
        if (mirror == null || mirror.kind == TypeKind.NONE) {
            return null
        }
        if (mirror.kind == TypeKind.VOID) {
            return new TypeRef(kind: TypeRefKind.VOID, rawText: "void", displayName: "void", simpleName: "void")
        }
        if (mirror.kind.isPrimitive()) {
            return new TypeRef(kind: TypeRefKind.PRIMITIVE, rawText: mirror.toString(), displayName: mirror.toString(), simpleName: mirror.toString(), qualifiedName: mirror.toString())
        }
        if (mirror instanceof ArrayType) {
            TypeRef component = typeRef(((ArrayType) mirror).componentType)
            return new TypeRef(kind: TypeRefKind.ARRAY, rawText: mirror.toString(), displayName: mirror.toString(), simpleName: mirror.toString(), componentType: component, arrayDepth: (component?.arrayDepth ?: 0) + 1)
        }
        if (mirror instanceof TypeVariable) {
            TypeVariable variable = (TypeVariable) mirror
            String name = variable.asElement().simpleName.toString()
            return new TypeRef(kind: TypeRefKind.TYPE_VARIABLE, rawText: mirror.toString(), displayName: name, simpleName: name, qualifiedName: name)
        }
        if (mirror instanceof WildcardType) {
            WildcardType wildcard = (WildcardType) mirror
            TypeRef extendsBound = typeRef(wildcard.extendsBound)
            TypeRef superBound = typeRef(wildcard.superBound)
            return new TypeRef(kind: TypeRefKind.WILDCARD, rawText: mirror.toString(), displayName: mirror.toString(), simpleName: mirror.toString(), bound: extendsBound ?: superBound, upperBounds: extendsBound ? [extendsBound] : [], lowerBounds: superBound ? [superBound] : [])
        }
        if (mirror instanceof DeclaredType) {
            DeclaredType declared = (DeclaredType) mirror
            Element element = declared.asElement()
            String qualified = element instanceof TypeElement ? ((TypeElement) element).qualifiedName.toString() : mirror.toString()
            return new TypeRef(
                    kind: TypeRefKind.DECLARED,
                    rawText: mirror.toString(),
                    displayName: mirror.toString(),
                    simpleName: element.simpleName.toString(),
                    qualifiedName: qualified,
                    targetId: typeId(qualified),
                    linkRef: unresolved(qualified),
                    typeArguments: declared.typeArguments.collect { TypeMirror arg -> typeRef(arg) },
                    annotations: annotations(element)
            )
        }
        return new TypeRef(kind: TypeRefKind.UNKNOWN, rawText: mirror.toString(), displayName: mirror.toString(), simpleName: mirror.toString(), qualifiedName: mirror.toString())
    }

    private List<TypeParameterRef> typeParameters(List<? extends TypeParameterElement> parameters) {
        return (parameters ?: []).collect { TypeParameterElement parameter ->
            new TypeParameterRef(
                    name: parameter.simpleName.toString(),
                    bounds: parameter.bounds.findAll { it.toString() != "java.lang.Object" }.collect { TypeMirror mirror -> typeRef(mirror) }
            )
        }
    }

    private LinkRef unresolved(String text) {
        return new LinkRef(kind: LinkRefKind.UNRESOLVED, rawTarget: text, label: text, fallbackText: text)
    }

    private DocId memberId(TypeElement owner, Element element, DocMemberKind kind) {
        String signature = null
        if (element instanceof ExecutableElement) {
            ExecutableElement executable = (ExecutableElement) element
            String name = kind == DocMemberKind.CONSTRUCTOR ? owner.simpleName.toString() : element.simpleName.toString()
            signature = "${name}(${executable.parameters.collect { VariableElement parameter -> context.types.erasure(parameter.asType()).toString() }.join(',')})"
        }
        DocIdKind idKind = [
                (DocMemberKind.FIELD)             : DocIdKind.FIELD,
                (DocMemberKind.METHOD)            : DocIdKind.METHOD,
                (DocMemberKind.CONSTRUCTOR)       : DocIdKind.CONSTRUCTOR,
                (DocMemberKind.ENUM_CONSTANT)     : DocIdKind.ENUM_CONSTANT,
                (DocMemberKind.RECORD_COMPONENT)  : DocIdKind.RECORD_COMPONENT,
                (DocMemberKind.ANNOTATION_ELEMENT): DocIdKind.ANNOTATION_ELEMENT
        ][kind]
        String memberName = memberName(owner, element, kind)
        String qualifiedName = "${owner.qualifiedName}.${element.simpleName}"
        String anchor = signature ?: element.simpleName.toString()
        return new DocId(
                kind: idKind,
                qualifiedName: qualifiedName,
                canonicalId: canonicalId(idKind, owner.qualifiedName.toString(), memberName, signature),
                displayId: displayId(owner, element, kind),
                anchorId: anchor,
                signature: signature,
                fragment: anchor
        )
    }

    private static DocId packageId(String packageName) {
        String label = packageName ?: "default"
        return new DocId(
                kind: DocIdKind.PACKAGE,
                qualifiedName: packageName ?: "",
                canonicalId: "package:${label}",
                displayId: label,
                anchorId: label,
                fragment: label
        )
    }

    private static DocId typeId(String qualifiedName) {
        String simpleName = qualifiedName?.tokenize('.')?.last() ?: qualifiedName
        return new DocId(
                kind: DocIdKind.TYPE,
                qualifiedName: qualifiedName,
                canonicalId: "type:${qualifiedName}",
                displayId: simpleName,
                anchorId: qualifiedName,
                fragment: qualifiedName
        )
    }

    private static String canonicalId(DocIdKind kind, String ownerQualifiedName, String name, String signature) {
        String prefix = kind?.name()?.toLowerCase()
        return signature ? "${prefix}:${ownerQualifiedName}#${signature}" : "${prefix}:${ownerQualifiedName}#${name}"
    }

    private static String displayId(TypeElement owner, Element element, DocMemberKind kind) {
        if (element instanceof ExecutableElement) {
            ExecutableElement executable = (ExecutableElement) element
            String name = kind == DocMemberKind.CONSTRUCTOR ? owner.simpleName.toString() : element.simpleName.toString()
            String params = executable.parameters.collect { VariableElement parameter ->
                "${simpleType(parameter.asType().toString())} ${parameter.simpleName}"
            }.join(", ")
            return "${name}(${params})"
        }
        return element.simpleName.toString()
    }

    private static String simpleType(String type) {
        if (!type) return ""
        return type.replaceAll(/([a-zA-Z_$][\w$]*\.)+([A-Z_$][\w$]*)/, '$2')
    }

    private String memberName(TypeElement owner, Element element, DocMemberKind kind) {
        return kind == DocMemberKind.CONSTRUCTOR ? owner.simpleName.toString() : element.simpleName.toString()
    }

    private static boolean isDocumentableMember(Element element) {
        return element.kind in [
                ElementKind.FIELD,
                ElementKind.METHOD,
                ElementKind.CONSTRUCTOR,
                ElementKind.ENUM_CONSTANT,
                ElementKind.RECORD_COMPONENT
        ]
    }

    private static DocMemberKind memberKind(Element element) {
        if (element.kind == ElementKind.CONSTRUCTOR) return DocMemberKind.CONSTRUCTOR
        if (element.kind == ElementKind.ENUM_CONSTANT) return DocMemberKind.ENUM_CONSTANT
        if (element.kind == ElementKind.RECORD_COMPONENT) return DocMemberKind.RECORD_COMPONENT
        if (element.kind == ElementKind.METHOD && element.enclosingElement?.kind == ElementKind.ANNOTATION_TYPE) return DocMemberKind.ANNOTATION_ELEMENT
        if (element.kind == ElementKind.METHOD) return DocMemberKind.METHOD
        return DocMemberKind.FIELD
    }

    private DocTypeKind typeKind(TypeElement element) {
        if (element.kind == ElementKind.INTERFACE) return DocTypeKind.INTERFACE
        if (element.kind == ElementKind.ENUM) return DocTypeKind.ENUM
        if (element.kind == ElementKind.ANNOTATION_TYPE) return DocTypeKind.ANNOTATION
        if (element.kind.name() == "RECORD") return DocTypeKind.RECORD
        if (element.kind != ElementKind.CLASS) return DocTypeKind.CLASS
        TypeMirror erasedType = context.types.erasure(element.asType())
        TypeElement error = context.elements.getTypeElement("java.lang.Error")
        if (error != null && context.types.isSubtype(erasedType, context.types.erasure(error.asType()))) {
            return DocTypeKind.ERROR
        }
        TypeElement throwable = context.elements.getTypeElement("java.lang.Throwable")
        if (throwable != null && context.types.isSubtype(erasedType, context.types.erasure(throwable.asType()))) {
            return DocTypeKind.EXCEPTION
        }
        return DocTypeKind.CLASS
    }

    private static ApiVisibility visibility(Element element) {
        if (element == null) return ApiVisibility.PUBLIC
        if (element.modifiers.contains(Modifier.PUBLIC)) return ApiVisibility.PUBLIC
        if (element.modifiers.contains(Modifier.PROTECTED)) return ApiVisibility.PROTECTED
        if (element.modifiers.contains(Modifier.PRIVATE)) return ApiVisibility.PRIVATE
        return ApiVisibility.PACKAGE_PRIVATE
    }

    private List<TypeElement> collectTypeElements() {
        LinkedHashSet<TypeElement> result = new LinkedHashSet<>()
        context.environment.includedElements.each { Element element ->
            collectTypeElements(element, result)
        }
        return new ArrayList<>(result)
    }

    private static void collectTypeElements(Element element, Set<TypeElement> result) {
        if (element instanceof TypeElement) {
            result.add((TypeElement) element)
        }
        element.enclosedElements.each { Element child ->
            if (child instanceof TypeElement) {
                collectTypeElements(child, result)
            }
        }
    }
}

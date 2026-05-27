package com.byd.apidoc.parser.javadoc.converter

import com.byd.apidoc.doclet.BuildContext
import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.ApiDoc
import com.byd.apidoc.model.DRI
import com.byd.apidoc.model.EnumConstantDoc
import com.byd.apidoc.model.FieldDoc
import com.byd.apidoc.model.TagDoc
import com.byd.apidoc.resolver.AnchorResolver
import com.byd.apidoc.resolver.DocPathResolver
import com.byd.apidoc.resolver.VisibleMemberResolver

import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.TypeParameterElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror

class ElementClassConverter {
    private final BuildContext context
    private final ApiConfig config
    private final TypeMirrorFormatter formatter
    private final DocTreeExtractor docTreeExtractor
    private final DocPathResolver pathResolver
    private final AnchorResolver anchorResolver
    private final ElementFieldConverter fieldConverter
    private final ElementEnumConverter enumConverter
    private final ElementMethodConverter methodConverter
    private final VisibleMemberResolver visibleMemberResolver

    ElementClassConverter(BuildContext context, ApiConfig config) {
        this.context = context
        this.config = config
        this.formatter = new TypeMirrorFormatter()
        this.docTreeExtractor = new DocTreeExtractor(context.docTrees)
        this.pathResolver = new DocPathResolver()
        this.anchorResolver = new AnchorResolver()
        this.fieldConverter = new ElementFieldConverter(formatter, docTreeExtractor, anchorResolver)
        this.enumConverter = new ElementEnumConverter(formatter, docTreeExtractor)
        this.methodConverter = new ElementMethodConverter(context.elements, context.types, formatter, docTreeExtractor, anchorResolver)
        this.visibleMemberResolver = new VisibleMemberResolver(context.elements, context.types)
    }

    ApiDoc convert(TypeElement typeElement) {
        if (!isValidType(typeElement)) {
            return null
        }

        String simpleName = typeElement.simpleName.toString()
        String pkg = packageName(typeElement)
        ApiDoc doc = new ApiDoc()
        doc.name = simpleName
        doc.packageName = pkg
        doc.qualifiedName = typeElement.qualifiedName.toString()
        doc.path = pathResolver.classPath(pkg, simpleName)
        doc.dri = DRI.classDri(pkg, simpleName)
        doc.desc = docTreeExtractor.description(typeElement)
        doc.description = doc.desc
        doc.isInterface = typeElement.kind == ElementKind.INTERFACE
        doc.isAnnotation = typeElement.kind == ElementKind.ANNOTATION_TYPE
        doc.enumType = typeElement.kind == ElementKind.ENUM
        doc.exceptionType = isExceptionType(typeElement)
        doc.superClass = superClassName(typeElement)
        doc.interfaces = typeElement.interfaces.collect { TypeMirror type -> formatter.typeName(type) }
        doc.typeParameters = typeElement.typeParameters.collect { TypeParameterElement it -> it.toString() }
        doc.genericSignature = genericSignature(typeElement)
        doc.annotations = formatter.annotations(typeElement)

        docTreeExtractor.tagValues(typeElement).each { String tag ->
            doc.tagRefs.add(new TagDoc(tag))
        }
        docTreeExtractor.searchableTags(typeElement).each { String tag, String value ->
            doc.tagRefs.add(new TagDoc(value ? "${tag}:${value}" : tag))
        }

        if (doc.enumType) {
            doc.enumConstants = enumConstants(typeElement)
        } else {
            doc.fields = fields(typeElement)
        }
        doc.constructors = constructors(typeElement)
        doc.list = methods(typeElement)

        if (!config.includeTags.isEmpty() && !hasIncludedTag(doc)) {
            return null
        }
        return hasContent(doc) ? doc : null
    }

    private boolean isValidType(TypeElement typeElement) {
        if (typeElement == null || isSystemElement(typeElement)) {
            return false
        }
        String pkg = packageName(typeElement)
        if (pkg && config.excludePackages.any { excluded -> pkg.startsWith(excluded) }) {
            return false
        }

        boolean annotation = typeElement.kind == ElementKind.ANNOTATION_TYPE
        boolean iface = typeElement.kind == ElementKind.INTERFACE
        boolean enumType = typeElement.kind == ElementKind.ENUM
        boolean exception = isExceptionType(typeElement)
        boolean normalClass = typeElement.kind == ElementKind.CLASS && !exception
        boolean nested = typeElement.enclosingElement instanceof TypeElement

        if (iface && !config.includeInterfaces) return false
        if (enumType && !config.includeEnums) return false
        if (annotation && !config.includeAnnotations) return false
        if (exception && !config.includeExceptions) return false
        if (normalClass && !config.includeClasses) return false
        if (nested && !config.includeInnerClasses) return false
        return true
    }

    private List<FieldDoc> fields(TypeElement typeElement) {
        memberElements(typeElement).findAll { Element element ->
            element.kind == ElementKind.FIELD && !element.modifiers.contains(Modifier.PRIVATE)
        }.collect { Element element ->
            fieldConverter.convert((VariableElement) element)
        }
    }

    private List<EnumConstantDoc> enumConstants(TypeElement typeElement) {
        typeElement.enclosedElements.findAll { it.kind == ElementKind.ENUM_CONSTANT }.collect { Element element ->
            enumConverter.convert((VariableElement) element)
        }
    }

    private List constructors(TypeElement typeElement) {
        memberElements(typeElement).findAll { Element element ->
            element.kind == ElementKind.CONSTRUCTOR && element.modifiers.contains(Modifier.PUBLIC)
        }.collect { Element element ->
            methodConverter.convert(typeElement, (ExecutableElement) element, true)
        }
    }

    private List methods(TypeElement typeElement) {
        boolean utilityClass = isUtilityClass(typeElement)
        boolean enumClass = typeElement.kind == ElementKind.ENUM
        memberElements(typeElement).findAll { Element element ->
            if (element.kind != ElementKind.METHOD) return false
            ExecutableElement method = (ExecutableElement) element
            if (!method.modifiers.contains(Modifier.PUBLIC)) return false
            return !method.modifiers.contains(Modifier.STATIC) || utilityClass || enumClass
        }.collect { Element element ->
            methodConverter.convert(typeElement, (ExecutableElement) element, false)
        }.unique { method ->
            "${method.name}#${method.anchorId}"
        }
    }

    private List<Element> memberElements(TypeElement typeElement) {
        return visibleMemberResolver.resolve(typeElement, config.includeInheritedMembers).findAll { !isSystemElement(it) }
    }

    private boolean hasIncludedTag(ApiDoc doc) {
        doc.tagRefs.any { TagDoc tag -> config.includeTags.contains(tag.tag) } ||
                doc.list.any { method -> method.tagRefs.any { TagDoc tag -> config.includeTags.contains(tag.tag) } } ||
                doc.constructors.any { method -> method.tagRefs.any { TagDoc tag -> config.includeTags.contains(tag.tag) } }
    }

    private static boolean hasContent(ApiDoc doc) {
        return !doc.list.isEmpty() || !doc.constructors.isEmpty() || !doc.fields.isEmpty() || doc.isEnum()
    }

    private String packageName(TypeElement typeElement) {
        PackageElement pkg = context.elements.getPackageOf(typeElement)
        return pkg?.qualifiedName?.toString() ?: ""
    }

    private boolean isExceptionType(TypeElement typeElement) {
        TypeElement throwable = context.elements.getTypeElement("java.lang.Exception")
        return throwable != null && context.types.isSubtype(context.types.erasure(typeElement.asType()), context.types.erasure(throwable.asType()))
    }

    private String superClassName(TypeElement typeElement) {
        TypeMirror superType = typeElement.superclass
        if (superType == null || superType.toString() == "none" || superType.toString() == "java.lang.Object") {
            return null
        }
        return formatter.typeName(superType)
    }

    private static boolean isSystemElement(Element element) {
        Element cursor = element
        while (cursor != null && !(cursor instanceof TypeElement)) {
            cursor = cursor.enclosingElement
        }
        if (!(cursor instanceof TypeElement)) {
            return false
        }
        String qualified = ((TypeElement) cursor).qualifiedName.toString()
        return ["java.", "javax.", "sun.", "com.sun.", "org.omg.", "org.w3c.dom.", "org.xml.sax."].any { qualified.startsWith(it) }
    }

    private boolean isUtilityClass(TypeElement typeElement) {
        String className = typeElement.simpleName.toString().toLowerCase()
        if (className.contains("util") || className.contains("tools")) {
            return true
        }
        return !typeElement.enclosedElements.any { Element element ->
            element.kind == ElementKind.METHOD &&
                    element.modifiers.contains(Modifier.PUBLIC) &&
                    !element.modifiers.contains(Modifier.STATIC)
        }
    }

    private static String genericSignature(TypeElement typeElement) {
        if (!typeElement.typeParameters) {
            return typeElement.simpleName.toString()
        }
        return "${typeElement.simpleName}<${typeElement.typeParameters.collect { it.toString() }.join(', ')}>"
    }
}

package com.byd.apidoc.parser.javadoc.converter

import com.byd.apidoc.model.ApiMethodDoc
import com.byd.apidoc.model.DRI
import com.byd.apidoc.model.ParameterDoc
import com.byd.apidoc.model.TagDoc
import com.byd.apidoc.resolver.AnchorResolver

import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class ElementMethodConverter {
    private final Elements elements
    private final Types types
    private final TypeMirrorFormatter formatter
    private final DocTreeExtractor docTreeExtractor
    private final AnchorResolver anchorResolver

    ElementMethodConverter(Elements elements,
                           Types types,
                           TypeMirrorFormatter formatter,
                           DocTreeExtractor docTreeExtractor,
                           AnchorResolver anchorResolver) {
        this.elements = elements
        this.types = types
        this.formatter = formatter
        this.docTreeExtractor = docTreeExtractor
        this.anchorResolver = anchorResolver
    }

    ApiMethodDoc convert(TypeElement owner, ExecutableElement executable, boolean constructor) {
        String name = constructor ? owner.simpleName.toString() : executable.simpleName.toString()
        Map<String, String> paramComments = docTreeExtractor.parameterComments(executable)
        Map<String, String> throwsComments = docTreeExtractor.throwsComments(executable)
        List<String> erasedParams = executable.parameters.collect { VariableElement p ->
            types.erasure(p.asType()).toString()
        }
        String signature = signature(owner, executable, constructor)

        ApiMethodDoc methodDoc = new ApiMethodDoc()
        methodDoc.name = name
        methodDoc.qualifiedName = "${owner.qualifiedName}.${constructor ? '<init>' : name}"
        methodDoc.desc = docTreeExtractor.description(executable) ?: name
        methodDoc.description = methodDoc.desc
        methodDoc.url = signature
        methodDoc.signature = signature
        methodDoc.modifiers = formatter.modifiers(executable)
        methodDoc.returnType = constructor ? "" : formatter.typeName(executable.returnType)
        methodDoc.returnComment = docTreeExtractor.returnComment(executable)
        methodDoc.exceptions = executable.thrownTypes.collect { TypeMirror type -> formatter.typeName(type) }
        methodDoc.exceptionComments = throwsComments
        methodDoc.annotations = formatter.annotations(executable)
        methodDoc.parameters = executable.parameters.collect { VariableElement parameter ->
            new ParameterDoc(
                    name: parameter.simpleName.toString(),
                    type: formatter.typeName(parameter.asType()),
                    description: paramComments.get(parameter.simpleName.toString()) ?: "",
                    annotations: formatter.annotations(parameter)
            )
        }
        methodDoc.anchorId = anchorResolver.forExecutable(name, executable, types)
        methodDoc.dri = DRI.memberDri(packageName(owner), owner.simpleName.toString(), constructor ? "<init>" : name, erasedParams)
        methodDoc.isStatic = executable.modifiers.contains(Modifier.STATIC)
        methodDoc.isConstructor = constructor
        methodDoc.genericSignature = genericSignature(executable)
        docTreeExtractor.tagValues(executable).each { String tag ->
            methodDoc.tagRefs.add(new TagDoc(tag))
        }
        docTreeExtractor.searchableTags(executable).each { String tag, String value ->
            methodDoc.tagRefs.add(new TagDoc(value ? "${tag}:${value}" : tag))
        }
        return methodDoc
    }

    private String signature(TypeElement owner, ExecutableElement executable, boolean constructor) {
        String methodName = constructor ? owner.simpleName.toString() : executable.simpleName.toString()
        String params = executable.parameters.collect { VariableElement parameter ->
            "${formatter.typeName(parameter.asType())} ${parameter.simpleName}"
        }.join(", ")
        return "${methodName}(${params})"
    }

    private String packageName(TypeElement typeElement) {
        return elements.getPackageOf(typeElement)?.qualifiedName?.toString() ?: ""
    }

    private static String genericSignature(ExecutableElement executable) {
        if (!executable.typeParameters) {
            return executable.toString()
        }
        return "<${executable.typeParameters.collect { it.toString() }.join(', ')}> ${executable}"
    }
}

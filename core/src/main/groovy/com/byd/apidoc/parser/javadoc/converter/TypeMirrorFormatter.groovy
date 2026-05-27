package com.byd.apidoc.parser.javadoc.converter

import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.type.TypeMirror

class TypeMirrorFormatter {
    String typeName(TypeMirror type) {
        return type == null ? "" : type.toString()
    }

    String modifiers(Element element) {
        List<Modifier> order = [
                Modifier.PUBLIC, Modifier.PROTECTED, Modifier.PRIVATE, Modifier.STATIC,
                Modifier.FINAL, Modifier.ABSTRACT, Modifier.SYNCHRONIZED, Modifier.NATIVE
        ]
        return order.findAll { element.modifiers.contains(it) }.collect { it.toString() }.join(" ")
    }

    String visibility(Element element) {
        if (element.modifiers.contains(Modifier.PUBLIC)) return "public"
        if (element.modifiers.contains(Modifier.PROTECTED)) return "protected"
        if (element.modifiers.contains(Modifier.PRIVATE)) return "private"
        return "package"
    }

    List<String> annotations(Element element) {
        return element.annotationMirrors.collect { AnnotationMirror annotation -> annotation.toString() }
    }
}

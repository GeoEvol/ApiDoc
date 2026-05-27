package com.byd.apidoc.parser.javadoc.converter

import com.byd.apidoc.model.EnumConstantDoc

import javax.lang.model.element.VariableElement

class ElementEnumConverter {
    private final TypeMirrorFormatter formatter
    private final DocTreeExtractor docTreeExtractor

    ElementEnumConverter(TypeMirrorFormatter formatter, DocTreeExtractor docTreeExtractor) {
        this.formatter = formatter
        this.docTreeExtractor = docTreeExtractor
    }

    EnumConstantDoc convert(VariableElement constant) {
        String comment = docTreeExtractor.description(constant)
        return new EnumConstantDoc(
                name: constant.simpleName.toString(),
                description: comment,
                value: enumValue(comment),
                constructorParams: [],
                annotations: formatter.annotations(constant)
        )
    }

    private static String enumValue(String comment) {
        if (!comment) return null
        def matcher = comment =~ /\{@code\s+([^}]+)\}/
        return matcher.find() ? matcher.group(1).trim() : null
    }
}

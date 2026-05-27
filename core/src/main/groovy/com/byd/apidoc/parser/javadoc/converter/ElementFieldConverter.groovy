package com.byd.apidoc.parser.javadoc.converter

import com.byd.apidoc.model.FieldDoc
import com.byd.apidoc.resolver.AnchorResolver

import javax.lang.model.element.VariableElement

class ElementFieldConverter {
    private final TypeMirrorFormatter formatter
    private final DocTreeExtractor docTreeExtractor
    private final AnchorResolver anchorResolver

    ElementFieldConverter(TypeMirrorFormatter formatter, DocTreeExtractor docTreeExtractor, AnchorResolver anchorResolver) {
        this.formatter = formatter
        this.docTreeExtractor = docTreeExtractor
        this.anchorResolver = anchorResolver
    }

    FieldDoc convert(VariableElement field) {
        String name = field.simpleName.toString()
        return new FieldDoc(
                name: name,
                anchorId: anchorResolver.forField(name),
                type: formatter.typeName(field.asType()),
                description: docTreeExtractor.description(field),
                isStatic: field.modifiers.any { it.name() == "STATIC" },
                modifier: formatter.visibility(field),
                annotations: formatter.annotations(field)
        )
    }
}

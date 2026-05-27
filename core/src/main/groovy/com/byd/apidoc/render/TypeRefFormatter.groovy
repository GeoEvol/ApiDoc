package com.byd.apidoc.render

import com.byd.apidoc.model.TypeRef
import com.byd.apidoc.model.TypeRefKind

class TypeRefFormatter {
    String format(TypeRef typeRef) {
        if (typeRef == null) {
            return ""
        }
        if (typeRef.kind == TypeRefKind.ARRAY) {
            return "${format(typeRef.componentType)}[]"
        }
        if (typeRef.kind == TypeRefKind.WILDCARD) {
            if (typeRef.upperBounds) {
                return "? extends ${format(typeRef.upperBounds[0])}"
            }
            if (typeRef.lowerBounds) {
                return "? super ${format(typeRef.lowerBounds[0])}"
            }
            return "?"
        }
        if (typeRef.typeArguments) {
            return "${typeRef.simpleName ?: typeRef.displayName}<${typeRef.typeArguments.collect { format(it) }.join(', ')}>"
        }
        return typeRef.simpleName ?: typeRef.displayName ?: typeRef.qualifiedName ?: typeRef.rawText ?: ""
    }
}

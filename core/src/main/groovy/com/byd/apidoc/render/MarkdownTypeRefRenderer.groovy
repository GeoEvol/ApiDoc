package com.byd.apidoc.render

import com.byd.apidoc.model.TypeRef
import com.byd.apidoc.model.TypeRefKind
import com.byd.apidoc.projection.DocProjection

class MarkdownTypeRefRenderer {
    private final MarkdownLinkRenderer linkRenderer

    MarkdownTypeRefRenderer(MarkdownLinkRenderer linkRenderer = new MarkdownLinkRenderer()) {
        this.linkRenderer = linkRenderer
    }

    String render(TypeRef typeRef, String fromPage, DocProjection projection) {
        if (typeRef == null) return ""
        if (typeRef.kind == TypeRefKind.ARRAY) return "${render(typeRef.componentType, fromPage, projection)}[]"
        if (typeRef.kind == TypeRefKind.WILDCARD) {
            if (typeRef.upperBounds) return "? extends ${render(typeRef.upperBounds[0], fromPage, projection)}"
            if (typeRef.lowerBounds) return "? super ${render(typeRef.lowerBounds[0], fromPage, projection)}"
            return "?"
        }
        String base = typeRef.linkRef ? linkRenderer.render(typeRef.linkRef, fromPage, projection) : (typeRef.simpleName ?: typeRef.displayName ?: typeRef.qualifiedName ?: "")
        if (typeRef.typeArguments) {
            return "${base}<${typeRef.typeArguments.collect { render(it, fromPage, projection) }.join(', ')}>"
        }
        return base
    }
}

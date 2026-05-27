package com.byd.apidoc.render

import com.byd.apidoc.model.TypeRef
import com.byd.apidoc.model.TypeRefKind
import com.byd.apidoc.projection.DocProjection

class HtmlTypeRefRenderer {
    private final HtmlLinkRenderer linkRenderer

    HtmlTypeRefRenderer(HtmlLinkRenderer linkRenderer = new HtmlLinkRenderer()) {
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
        String base = typeRef.linkRef ? linkRenderer.render(typeRef.linkRef, fromPage, projection) : escape(typeRef.simpleName ?: typeRef.displayName ?: typeRef.qualifiedName ?: "")
        if (typeRef.typeArguments) {
            return "${base}&lt;${typeRef.typeArguments.collect { render(it, fromPage, projection) }.join(', ')}&gt;"
        }
        return base
    }

    private static String escape(String text) {
        return (text ?: "")
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
    }
}

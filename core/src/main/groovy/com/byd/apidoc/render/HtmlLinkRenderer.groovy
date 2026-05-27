package com.byd.apidoc.render

import com.byd.apidoc.model.LinkRef
import com.byd.apidoc.model.LinkRefKind
import com.byd.apidoc.projection.DocProjection
import com.byd.apidoc.reference.LinkPathResolver

class HtmlLinkRenderer {
    private final LinkPathResolver pathResolver

    HtmlLinkRenderer(LinkPathResolver pathResolver = new LinkPathResolver()) {
        this.pathResolver = pathResolver
    }

    String render(LinkRef linkRef, String fromPage, DocProjection projection) {
        String label = escape(linkRef?.label ?: linkRef?.fallbackText ?: linkRef?.rawTarget)
        if (!linkRef) {
            return label
        }
        if (linkRef.kind == LinkRefKind.EXTERNAL && linkRef.externalUrl) {
            return "<a href=\"${escapeAttr(linkRef.externalUrl)}\">${label}</a>"
        }
        if (linkRef.kind == LinkRefKind.INTERNAL && linkRef.targetId != null) {
            String pageUrl = projection.pages.find { it.targetId?.stableKey() == linkRef.targetId.stableKey() }?.url
            if (pageUrl) {
                return "<a href=\"${escapeAttr(pathResolver.htmlUrl(fromPage, pageUrl, linkRef.targetId.effectiveAnchorId()))}\">${label}</a>"
            }
        }
        return label
    }

    private static String escape(String text) {
        return (text ?: "")
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
    }

    private static String escapeAttr(String text) {
        return escape(text).replace('"', '&quot;')
    }
}

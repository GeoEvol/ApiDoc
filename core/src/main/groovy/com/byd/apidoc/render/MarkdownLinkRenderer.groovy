package com.byd.apidoc.render

import com.byd.apidoc.model.LinkRef
import com.byd.apidoc.model.LinkRefKind
import com.byd.apidoc.projection.DocProjection
import com.byd.apidoc.reference.LinkPathResolver

class MarkdownLinkRenderer {
    private final LinkPathResolver pathResolver

    MarkdownLinkRenderer(LinkPathResolver pathResolver = new LinkPathResolver()) {
        this.pathResolver = pathResolver
    }

    String render(LinkRef linkRef, String fromPage, DocProjection projection) {
        String label = escape(linkRef?.label ?: linkRef?.fallbackText ?: linkRef?.rawTarget)
        if (!linkRef) {
            return label
        }
        if (linkRef.kind == LinkRefKind.EXTERNAL) {
            return label
        }
        if (linkRef.kind == LinkRefKind.INTERNAL && linkRef.targetId != null) {
            String pageUrl = projection.pages.find { it.targetId?.stableKey() == linkRef.targetId.stableKey() }?.url
            if (pageUrl) {
                return "[${label}](${pathResolver.markdownUrl(fromPage, pageUrl.replaceAll(/\.html$/, '.md'), linkRef.targetId.effectiveAnchorId())})"
            }
        }
        return label
    }

    private static String escape(String text) {
        return (text ?: "").replace("|", "\\|")
    }
}

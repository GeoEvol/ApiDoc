package com.byd.apidoc.render

import com.byd.apidoc.comment.BlockTag
import com.byd.apidoc.comment.BlockTagKind
import com.byd.apidoc.comment.CommentDoc
import com.byd.apidoc.comment.CommentNode
import com.byd.apidoc.comment.InlineTag
import com.byd.apidoc.comment.InlineTagKind
import com.byd.apidoc.projection.DocProjection

class HtmlCommentRenderer {
    private final HtmlLinkRenderer linkRenderer

    HtmlCommentRenderer(HtmlLinkRenderer linkRenderer = new HtmlLinkRenderer()) {
        this.linkRenderer = linkRenderer
    }

    String renderSummary(CommentDoc comment, String fromPage, DocProjection projection) {
        return renderNodes(comment?.summaryNodes, fromPage, projection).trim()
    }

    String renderBody(CommentDoc comment, String fromPage, DocProjection projection) {
        String body = renderNodes(comment?.bodyNodes, fromPage, projection).trim()
        return body ? "<p>${body}</p>" : ""
    }

    String renderBlockTags(CommentDoc comment, String fromPage, DocProjection projection) {
        if (comment == null || !comment.blockTags) {
            return ""
        }
        StringBuilder out = new StringBuilder()
        List<BlockTag> deprecated = comment.blockTags.findAll { it.kind == BlockTagKind.DEPRECATED }
        if (deprecated) {
            out << "<p class=\"ad-deprecated\"><strong>Deprecated.</strong> ${deprecated.collect { renderNodes(it.body, fromPage, projection) ?: escape(it.rawText) }.findAll { it }.join(' ')}</p>\n"
        }
        List<BlockTag> params = comment.blockTags.findAll { it.kind == BlockTagKind.PARAM }
        if (params) {
            out << "<h4>Parameters</h4><dl>\n"
            params.each { BlockTag tag ->
                out << "<dt><code>${escape(tag.key)}</code></dt><dd>${renderNodes(tag.body, fromPage, projection) ?: escape(tag.rawText)}</dd>\n"
            }
            out << "</dl>\n"
        }
        List<BlockTag> returns = comment.blockTags.findAll { it.kind == BlockTagKind.RETURN }
        if (returns) {
            out << "<p><strong>Returns:</strong> ${returns.collect { renderNodes(it.body, fromPage, projection) ?: escape(it.rawText) }.findAll { it }.join(' ')}</p>\n"
        }
        List<BlockTag> throwsTags = comment.blockTags.findAll { it.kind == BlockTagKind.THROWS }
        if (throwsTags) {
            out << "<h4>Throws</h4><dl>\n"
            throwsTags.each { BlockTag tag ->
                out << "<dt><code>${escape(tag.key)}</code></dt><dd>${renderNodes(tag.body, fromPage, projection) ?: escape(tag.rawText)}</dd>\n"
            }
            out << "</dl>\n"
        }
        List<BlockTag> seeTags = comment.blockTags.findAll { it.kind == BlockTagKind.SEE }
        if (seeTags) {
            out << "<p><strong>See also:</strong> ${seeTags.collect { renderNodes(it.body, fromPage, projection) ?: escape(it.rawText) }.findAll { it }.join(', ')}</p>\n"
        }
        List<BlockTag> custom = comment.blockTags.findAll { it.kind == BlockTagKind.CUSTOM && !(it.name in ["hide", "removed", "apiSince", "deprecatedSince", "removedSince"]) }
        custom.each { BlockTag tag ->
            out << "<p><strong>@${escape(tag.name)}:</strong> ${renderNodes(tag.body, fromPage, projection) ?: escape(tag.rawText)}</p>\n"
        }
        return out.toString()
    }

    String renderNodes(List<CommentNode> nodes, String fromPage, DocProjection projection) {
        return (nodes ?: []).collect { renderNode(it, fromPage, projection) }
                .findAll { it != null && !it.isEmpty() }
                .join(" ")
                .replaceAll(/\s+/, " ")
                .trim()
    }

    private String renderNode(CommentNode node, String fromPage, DocProjection projection) {
        if (node == null) return ""
        if (node.inlineTag != null) {
            return renderInline(node.inlineTag, fromPage, projection)
        }
        return escape(node.text)
    }

    private String renderInline(InlineTag tag, String fromPage, DocProjection projection) {
        if (tag == null) return ""
        if (tag.kind in [InlineTagKind.LINK, InlineTagKind.LINKPLAIN]) {
            String label = renderNodes(tag.label, fromPage, projection)
            if (label && tag.reference != null) {
                tag.reference.label = label
            }
            return linkRenderer.render(tag.reference, fromPage, projection)
        }
        if (tag.kind == InlineTagKind.CODE) {
            return "<code>${escape(tag.body)}</code>"
        }
        if (tag.kind == InlineTagKind.LITERAL) {
            return escape(tag.body)
        }
        return escape(tag.body ?: tag.rawText ?: tag.name)
    }

    private static String escape(String text) {
        return (text ?: "")
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
    }
}

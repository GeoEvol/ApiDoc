package com.byd.apidoc.render

import com.byd.apidoc.comment.BlockTag
import com.byd.apidoc.comment.BlockTagKind
import com.byd.apidoc.comment.CommentDoc
import com.byd.apidoc.comment.CommentNode
import com.byd.apidoc.comment.CommentNodeKind
import com.byd.apidoc.comment.InlineTag
import com.byd.apidoc.comment.InlineTagKind
import com.byd.apidoc.projection.DocProjection

class MarkdownCommentRenderer {
    private final MarkdownLinkRenderer linkRenderer

    MarkdownCommentRenderer(MarkdownLinkRenderer linkRenderer = new MarkdownLinkRenderer()) {
        this.linkRenderer = linkRenderer
    }

    String renderSummary(CommentDoc comment, String fromPage, DocProjection projection) {
        return renderNodes(comment?.summaryNodes, fromPage, projection).trim()
    }

    String renderBody(CommentDoc comment, String fromPage, DocProjection projection) {
        return renderNodes(comment?.bodyNodes, fromPage, projection).trim()
    }

    String renderBlockTags(CommentDoc comment, String fromPage, DocProjection projection) {
        if (comment == null || !comment.blockTags) {
            return ""
        }
        StringBuilder out = new StringBuilder()
        List<BlockTag> deprecated = comment.blockTags.findAll { it.kind == BlockTagKind.DEPRECATED }
        if (deprecated) {
            out << "**Deprecated.** ${deprecated.collect { renderNodes(it.body, fromPage, projection) ?: it.rawText }.findAll { it }.join(' ')}\n\n"
        }
        List<BlockTag> params = comment.blockTags.findAll { it.kind == BlockTagKind.PARAM }
        if (params) {
            out << "**Parameters**\n\n"
            params.each { BlockTag tag ->
                out << "- `${tag.key}`: ${renderNodes(tag.body, fromPage, projection) ?: tag.rawText}\n"
            }
            out << "\n"
        }
        List<BlockTag> returns = comment.blockTags.findAll { it.kind == BlockTagKind.RETURN }
        if (returns) {
            out << "**Returns:** ${returns.collect { renderNodes(it.body, fromPage, projection) ?: it.rawText }.findAll { it }.join(' ')}\n\n"
        }
        List<BlockTag> throwsTags = comment.blockTags.findAll { it.kind == BlockTagKind.THROWS }
        if (throwsTags) {
            out << "**Throws**\n\n"
            throwsTags.each { BlockTag tag ->
                out << "- `${tag.key}`: ${renderNodes(tag.body, fromPage, projection) ?: tag.rawText}\n"
            }
            out << "\n"
        }
        List<BlockTag> seeTags = comment.blockTags.findAll { it.kind == BlockTagKind.SEE }
        if (seeTags) {
            out << "**See also:** ${seeTags.collect { renderNodes(it.body, fromPage, projection) ?: it.rawText }.findAll { it }.join(', ')}\n\n"
        }
        List<BlockTag> custom = comment.blockTags.findAll { it.kind == BlockTagKind.CUSTOM && !(it.name in ["hide", "removed", "apiSince", "deprecatedSince", "removedSince"]) }
        custom.each { BlockTag tag ->
            out << "**@${tag.name}:** ${renderNodes(tag.body, fromPage, projection) ?: tag.rawText}\n\n"
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
        if (node.kind == CommentNodeKind.HTML) {
            return ""
        }
        if (node.kind == CommentNodeKind.ENTITY) {
            return renderEntity(node.text)
        }
        if (node.inlineTag != null) {
            return renderInline(node.inlineTag, fromPage, projection)
        }
        return escape(node.text)
    }

    private static String renderEntity(String text) {
        String value = (text ?: "").trim()
        if (value.startsWith("&")) {
            value = value.substring(1)
        }
        if (value.endsWith(";")) {
            value = value.substring(0, value.length() - 1)
        }
        if (value ==~ /[A-Za-z][A-Za-z0-9]+|#[0-9]+|#x[0-9A-Fa-f]+/) {
            return "&${value};"
        }
        return escape("&${value};")
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
            return "`${tag.body ?: ''}`"
        }
        if (tag.kind == InlineTagKind.LITERAL) {
            return escape(tag.body)
        }
        return escape(tag.body ?: tag.rawText ?: tag.name)
    }

    private static String escape(String text) {
        return (text ?: "").replace("|", "\\|")
    }
}

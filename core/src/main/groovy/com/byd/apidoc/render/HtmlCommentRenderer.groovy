package com.byd.apidoc.render

import com.byd.apidoc.comment.BlockTag
import com.byd.apidoc.comment.BlockTagKind
import com.byd.apidoc.comment.CommentDoc
import com.byd.apidoc.comment.CommentNode
import com.byd.apidoc.comment.InlineTag
import com.byd.apidoc.comment.InlineTagKind
import com.byd.apidoc.comment.ThrowsTag
import com.byd.apidoc.model.LinkRef
import com.byd.apidoc.model.LinkRefKind
import com.byd.apidoc.model.TypeRef
import com.byd.apidoc.projection.DocProjection
import com.byd.apidoc.projection.PageModel

class HtmlCommentRenderer {
    private final HtmlLinkRenderer linkRenderer
    private final HtmlTypeRefRenderer typeRefRenderer

    HtmlCommentRenderer(HtmlLinkRenderer linkRenderer = new HtmlLinkRenderer()) {
        this.linkRenderer = linkRenderer
        this.typeRefRenderer = new HtmlTypeRefRenderer(linkRenderer)
    }

    String renderSummary(CommentDoc comment, String fromPage, DocProjection projection) {
        return renderNodes(comment?.summaryNodes, fromPage, projection).trim()
    }

    String renderBody(CommentDoc comment, String fromPage, DocProjection projection) {
        return renderBodyBlocks(comment?.bodyNodes, fromPage, projection)
    }

    String renderBlockTags(CommentDoc comment, String fromPage, DocProjection projection, List<TypeRef> throwsTypes = [], TypeRef returnType = null) {
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
            out << "<section class=\"ad-detail-section\"><table class=\"ad-detail-table\"><thead><tr class=\"ad-detail-heading-row\"><th colspan=\"2\" class=\"ad-detail-heading\"><h3 class=\"ad-sub-section-heading ad-member-section-title\"><span>Parameters</span></h3></th></tr></thead><tbody>\n"
            params.each { BlockTag tag ->
                out << "<tr><td><code>${escape(tag.key)}</code></td><td>${renderNodes(tag.body, fromPage, projection) ?: escape(tag.rawText)}</td></tr>\n"
            }
            out << "</tbody></table></section>\n"
        }
        List<BlockTag> returns = comment.blockTags.findAll { it.kind == BlockTagKind.RETURN }
        if (returns) {
            String renderedReturnType = returnType != null ? typeRefRenderer.render(returnType, fromPage, projection) : "Return value"
            out << "<section class=\"ad-detail-section\"><table class=\"ad-detail-table\"><thead><tr class=\"ad-detail-heading-row\"><th colspan=\"2\" class=\"ad-detail-heading\"><h3 class=\"ad-sub-section-heading ad-member-section-title\"><span>Returns</span></h3></th></tr></thead><tbody>\n"
            returns.each { BlockTag tag ->
                out << "<tr><td><code>${renderedReturnType}</code></td><td>${renderNodes(tag.body, fromPage, projection) ?: escape(tag.rawText)}</td></tr>\n"
            }
            out << "</tbody></table></section>\n"
        }
        List<BlockTag> throwsTags = comment.blockTags.findAll { it.kind == BlockTagKind.THROWS }
        if (throwsTags) {
            out << "<section class=\"ad-detail-section\"><table class=\"ad-detail-table\"><thead><tr class=\"ad-detail-heading-row\"><th colspan=\"2\" class=\"ad-detail-heading\"><h3 class=\"ad-sub-section-heading ad-member-section-title\"><span>Throws</span></h3></th></tr></thead><tbody>\n"
            throwsTags.each { BlockTag tag ->
                String key = renderThrowsKey(tag, fromPage, projection, throwsTypes)
                out << "<tr><td><code>${key}</code></td><td>${renderNodes(tag.body, fromPage, projection) ?: escape(tag.rawText)}</td></tr>\n"
            }
            out << "</tbody></table></section>\n"
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

    String renderBodyBlocks(List<CommentNode> nodes, String fromPage, DocProjection projection) {
        List<List<CommentNode>> blocks = [[]]
        (nodes ?: []).each { CommentNode node ->
            if (isParagraphBoundary(node)) {
                if (blocks.last()) {
                    blocks.add([])
                }
            } else {
                blocks.last().add(node)
            }
        }
        List<String> paragraphs = blocks.collect { List<CommentNode> block ->
            renderNodes(block, fromPage, projection)
        }.findAll { it }
        if (!paragraphs) return ""
        return "<div class=\"ad-detail-description\">${paragraphs.collect { "<p>${it}</p>" }.join("")}</div>"
    }

    String renderNodes(List<CommentNode> nodes, String fromPage, DocProjection projection) {
        return (nodes ?: []).collect { renderNode(it, fromPage, projection) }
                .findAll { it != null && !it.isEmpty() }
                .join(" ")
                .replaceAll(/\s+/, " ")
                .replaceAll(/\s+([,.;:!?])/, '$1')
                .trim()
    }

    private String renderNode(CommentNode node, String fromPage, DocProjection projection) {
        if (node == null) return ""
        if (node.kind == com.byd.apidoc.comment.CommentNodeKind.HTML) {
            return renderHtmlNode(node.text)
        }
        if (node.inlineTag != null) {
            return renderInline(node.inlineTag, fromPage, projection)
        }
        if ((node.text ?: "") ==~ /(?i)<\/?p\s*\/?>|<br\s*\/?>/) {
            return renderHtmlNode(node.text)
        }
        return escape(node.text)
    }

    private static String renderHtmlNode(String text) {
        String value = text ?: ""
        if (value ==~ /(?i)<\/?p\s*\/?>/) return ""
        if (value ==~ /(?i)<br\s*\/?>/) return "<br>"
        return escape(value)
    }

    private static boolean isParagraphBoundary(CommentNode node) {
        if (node?.kind != com.byd.apidoc.comment.CommentNodeKind.HTML) return false
        String value = node.text ?: ""
        return value ==~ /(?i)<\/?p\s*\/?>|<br\s*\/?>/
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

    private String renderThrowsKey(BlockTag tag, String fromPage, DocProjection projection, List<TypeRef> throwsTypes) {
        TypeRef matchedType = (throwsTypes ?: []).find { TypeRef type ->
            tag.key in [type.simpleName, type.displayName, type.qualifiedName]
        }
        if (matchedType != null) {
            return typeRefRenderer.render(matchedType, fromPage, projection)
        }
        if (tag instanceof ThrowsTag && tag.exceptionRef != null) {
            String rendered = linkRenderer.render(((ThrowsTag) tag).exceptionRef, fromPage, projection)
            if (rendered != escape(tag.key)) return rendered
        }
        PageModel target = projection?.pages?.find { PageModel page ->
            String qualifiedName = page.targetId?.qualifiedName
            qualifiedName == tag.key || qualifiedName?.endsWith(".${tag.key}")
        }
        if (target?.targetId) {
            return linkRenderer.render(new LinkRef(
                    kind: LinkRefKind.INTERNAL,
                    targetId: target.targetId,
                    label: tag.key,
                    fallbackText: tag.key
            ), fromPage, projection)
        }
        return escape(tag.key)
    }

    private static String escape(String text) {
        return (text ?: "")
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
    }
}
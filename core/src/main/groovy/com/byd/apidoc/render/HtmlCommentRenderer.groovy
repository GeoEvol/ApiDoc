package com.byd.apidoc.render

import com.byd.apidoc.comment.BlockTag
import com.byd.apidoc.comment.BlockTagKind
import com.byd.apidoc.comment.CommentDoc
import com.byd.apidoc.comment.CommentNode
import com.byd.apidoc.comment.InlineTag
import com.byd.apidoc.comment.InlineTagKind
import com.byd.apidoc.comment.SinceTag
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
        String attribution = renderAttribution(comment)
        return renderBodyBlocks(comment?.bodyNodes, fromPage, projection, attribution)
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
        List<BlockTag> custom = comment.blockTags.findAll { it.kind == BlockTagKind.CUSTOM && !(it.name in ["hide", "removed", "apiSince", "deprecatedSince", "removedSince", "author", "date"]) }
        custom.each { BlockTag tag ->
            out << "<p><strong>@${escape(tag.name)}:</strong> ${renderNodes(tag.body, fromPage, projection) ?: escape(tag.rawText)}</p>\n"
        }
        return out.toString()
    }

    String renderBodyBlocks(List<CommentNode> nodes, String fromPage, DocProjection projection, String attribution = "") {
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
        if (!paragraphs && !attribution) return ""
        return "<div class=\"ad-detail-description\">${paragraphs.collect { "<p>${it}</p>" }.join("")}${attribution}</div>"
    }

    private static String renderAttribution(CommentDoc comment) {
        if (comment == null) {
            return ""
        }
        String authorName = extractAuthorName(comment)
        AttributionSince since = extractSinceAttribution(comment)
        String date = since.date ?: extractDateTag(comment)
        if (!authorName && !date && !since.note) {
            return ""
        }
        List<String> parts = []
        if (authorName) parts.add(authorName)
        if (date) parts.add(date)
        StringBuilder out = new StringBuilder("<blockquote class=\"ad-attribution\">")
        if (parts) {
            out << "<p>${escape("Add by ${parts.join(' | ')}")}</p>"
        }
        if (since.note) {
            out << "<p>${escape(since.note)}</p>"
        }
        out << "</blockquote>"
        return out.toString()
    }

    private static String extractAuthorName(CommentDoc comment) {
        if (comment.blockTags) {
            for (BlockTag tag : comment.blockTags) {
                if (tag.kind == BlockTagKind.CUSTOM && tag.name == "author") {
                    return normalizeInline(tag.rawText ?: "")
                }
            }
        }
        if (comment.rawText) {
            def matcher = comment.rawText =~ /(?m)^\s*@author\s+(.+)$/
            if (matcher) {
                return normalizeInline(matcher[0][1] ?: "")
            }
        }
        return ""
    }

    private static AttributionSince extractSinceAttribution(CommentDoc comment) {
        if (comment.blockTags) {
            for (BlockTag tag : comment.blockTags) {
                if (tag.kind == BlockTagKind.SINCE && tag instanceof SinceTag) {
                    AttributionSince parsed = parseSinceAttribution(((SinceTag) tag).version ?: tag.rawText)
                    if (parsed.date) {
                        return parsed
                    }
                }
            }
        }
        if (comment.rawText) {
            AttributionSince parsed = parseSinceAttribution(extractRawBlock(comment.rawText, "since"))
            if (parsed.date) {
                return parsed
            }
        }
        return new AttributionSince()
    }

    private static String extractDateTag(CommentDoc comment) {
        if (comment.blockTags) {
            for (BlockTag tag : comment.blockTags) {
                if (tag.kind == BlockTagKind.CUSTOM && tag.name == "date") {
                    return formatSinceDate(tag.rawText ?: "")
                }
            }
        }
        if (comment.rawText) {
            def matcher = comment.rawText =~ /(?m)^\s*@date\s+(.+)$/
            if (matcher) {
                return formatSinceDate(matcher[0][1]?.trim() ?: "")
            }
        }
        return ""
    }

    private static AttributionSince parseSinceAttribution(String text) {
        String cleaned = normalizeMultiline(text)
        if (!cleaned) {
            return new AttributionSince()
        }
        List<String> lines = cleaned.readLines().collect { String line -> line.trim() }
        int firstLineIndex = lines.findIndexOf { String line -> line }
        if (firstLineIndex < 0) {
            return new AttributionSince()
        }
        def matcher = lines[firstLineIndex] =~ /^(\S+)(?:\s+(.*))?$/
        if (!matcher.matches()) {
            return new AttributionSince()
        }
        String candidate = matcher.group(1)
        if (!isDateLike(candidate)) {
            return new AttributionSince()
        }

        List<String> noteLines = []
        String remainder = matcher.group(2)?.trim()
        if (remainder) {
            noteLines.add(remainder)
        }
        for (int i = firstLineIndex + 1; i < lines.size(); i++) {
            String line = lines[i]
            if (!line) {
                continue
            }
            if (line ==~ /^@\w+.*/) {
                break
            }
            noteLines.add(line)
        }
        return new AttributionSince(
                date: formatSinceDate(candidate),
                note: normalizeInline(noteLines.join(" "))
        )
    }

    private static String extractRawBlock(String rawText, String tagName) {
        if (!rawText) {
            return ""
        }
        def matcher = rawText =~ /(?ms)^\s*@${tagName}\s+(.+?)(?=^\s*@\w+\b|\z)/
        if (matcher.find()) {
            return matcher.group(1)?.trim() ?: ""
        }
        return ""
    }

    private static boolean isDateLike(String value) {
        String cleaned = value?.trim()
        if (!cleaned) {
            return false
        }
        def separated = cleaned =~ /^(\d{4})[-\/](\d{1,2})[-\/](\d{1,2})$/
        if (separated.matches()) {
            return validMonthDay(separated.group(2) as int, separated.group(3) as int)
        }
        def compactYear = cleaned =~ /^(\d{4})(\d{2})(\d{2})$/
        if (compactYear.matches()) {
            return validMonthDay(compactYear.group(2) as int, compactYear.group(3) as int)
        }
        def compactShort = cleaned =~ /^(\d{2})(\d{2})(\d{2})$/
        if (compactShort.matches()) {
            return validMonthDay(compactShort.group(2) as int, compactShort.group(3) as int)
        }
        return false
    }

    private static boolean validMonthDay(int month, int day) {
        return month >= 1 && month <= 12 && day >= 1 && day <= 31
    }

    private static String formatSinceDate(String version) {
        if (version == null || version.trim().isEmpty()) {
            return ""
        }
        String cleaned = version.trim()
        try {
            def matcher = cleaned =~ /^(\d{4})[-\/](\d{1,2})[-\/](\d{1,2})$/
            if (matcher.matches()) {
                int year = matcher.group(1) as int
                int month = matcher.group(2) as int
                int day = matcher.group(3) as int
                return String.format("%04d-%02d-%02d", year, month, day)
            }
        } catch (Exception ignored) {
        }
        return cleaned
    }

    private static String normalizeMultiline(String text) {
        return (text ?: "")
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim()
    }

    private static String normalizeInline(String text) {
        return (text ?: "")
                .replaceAll(/\s+/, " ")
                .trim()
    }

    private static class AttributionSince {
        String date = ""
        String note = ""
    }

    String renderNodes(List<CommentNode> nodes, String fromPage, DocProjection projection) {
        return (nodes ?: []).collect { renderNode(it, fromPage, projection) }
                .findAll { it != null && !it.isEmpty() }
                .join(" ")
                .replaceAll(/\s*<br>\s*/, '<br>')
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
        return value ==~ /(?i)<\/?p\s*\/?>/
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

package com.byd.apidoc.render

import com.byd.apidoc.comment.BlockTag
import com.byd.apidoc.comment.BlockTagKind
import com.byd.apidoc.comment.CommentDoc
import com.byd.apidoc.comment.CommentNode
import com.byd.apidoc.comment.CommentNodeKind
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

    private static final Set<String> BLOCK_HTML_TAGS = [
            "address", "article", "aside", "blockquote", "caption", "dd", "details", "div", "dl", "dt",
            "figcaption", "figure", "h1", "h2", "h3", "h4", "h5", "h6", "hr", "li", "ol", "p",
            "pre", "section", "summary", "table", "tbody", "td", "tfoot", "th", "thead", "tr", "ul"
    ] as Set
    private static final Set<String> INLINE_HTML_TAGS = [
            "a", "abbr", "b", "br", "cite", "code", "data", "del", "dfn", "em", "i", "ins", "kbd",
            "mark", "q", "s", "samp", "small", "span", "strong", "sub", "sup", "time", "u", "var", "wbr"
    ] as Set
    private static final Set<String> VOID_HTML_TAGS = ["br", "hr", "wbr"] as Set
    private static final Set<String> URL_ATTRIBUTES = ["href", "src", "cite"] as Set
    private static final Set<String> ALLOWED_TARGETS = ["_blank", "_self", "_parent", "_top"] as Set

    HtmlCommentRenderer(HtmlLinkRenderer linkRenderer = new HtmlLinkRenderer()) {
        this.linkRenderer = linkRenderer
        this.typeRefRenderer = new HtmlTypeRefRenderer(linkRenderer)
    }

    String renderSummary(CommentDoc comment, String fromPage, DocProjection projection) {
        return renderNodes(comment?.summaryNodes, fromPage, projection).trim()
    }

    String renderBody(CommentDoc comment, String fromPage, DocProjection projection, CommentDoc parentComment = null) {
        String attributionBlock = renderAttributionBlock(comment, parentComment)
        return renderBodyBlocks(comment?.bodyNodes, fromPage, projection, attributionBlock)
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
        List<BlockTag> custom = comment.blockTags.findAll { it.kind == BlockTagKind.CUSTOM && !(it.name in ["hide", "removed", "apiSince", "deprecatedSince", "removedSince", "author", "date", "apiNote"]) }
        custom.each { BlockTag tag ->
            out << "<p><strong>@${escape(tag.name)}:</strong> ${renderNodes(tag.body, fromPage, projection) ?: escape(tag.rawText)}</p>\n"
        }
        return out.toString()
    }

    String renderBodyBlocks(List<CommentNode> nodes, String fromPage, DocProjection projection, String attributionBlock = "") {
        String content = renderBodyFlow(nodes, fromPage, projection)
        if (!content && !attributionBlock) return ""
        return "<div class=\"ad-detail-description\">${content}${attributionBlock}</div>"
    }

    private String renderBodyFlow(List<CommentNode> nodes, String fromPage, DocProjection projection) {
        StringBuilder out = new StringBuilder()
        StringBuilder inline = new StringBuilder()
        int blockDepth = 0
        (nodes ?: []).each { CommentNode node ->
            if (isAllowedBlockHtml(node)) {
                flushInlineParagraph(out, inline)
                out << renderNode(node, fromPage, projection)
                if (node.htmlEnd) {
                    blockDepth = Math.max(0, blockDepth - 1)
                } else if (node.htmlStart && !node.htmlSelfClosing && !isVoidHtmlNode(node)) {
                    blockDepth++
                }
                return
            }
            String rendered = renderNode(node, fromPage, projection)
            if (!rendered) {
                return
            }
            if (blockDepth > 0) {
                out << rendered
            } else {
                inline << rendered
            }
        }
        flushInlineParagraph(out, inline)
        return out.toString().trim()
    }

    private static void flushInlineParagraph(StringBuilder out, StringBuilder inline) {
        String content = inline.toString().trim()
        if (content) {
            out << "<p>${content}</p>"
        }
        inline.setLength(0)
    }

    private String renderAttributionBlock(CommentDoc comment, CommentDoc parentComment = null) {
        if (comment == null) {
            return ""
        }
        String authorName = extractAuthorName(comment)
        boolean hasSince = hasSinceTag(comment)
        String sinceDate = extractSinceDate(comment)
        String apiNoteText = extractApiNoteText(comment)
        // 方法注释：只有当方法与类注释的 author/since 不同时才显示
        if (parentComment != null) {
            String parentAuthor = extractAuthorName(parentComment)
            boolean parentHasSince = hasSinceTag(parentComment)
            String parentSince = parentHasSince ? extractSinceDate(parentComment) : ""
            // 如果方法没有 @author 或 @since，不显示
            if (!authorName || !hasSince || !sinceDate) {
                return ""
            }
            // 如果方法的 author 和 since 与类完全相同，不显示
            if (parentHasSince && authorName == parentAuthor && sinceDate == parentSince) {
                return ""
            }
        } else {
            // 类页面：没有 author 或 since 则不显示
            if (!authorName || !hasSince || !sinceDate) {
                return ""
            }
        }
        List<String> parts = []
        if (authorName) parts.add(authorName)
        if (sinceDate) parts.add(sinceDate)
        String attributionText = "Add by ${parts.join(' | ')}"
        StringBuilder block = new StringBuilder("<blockquote class=\"ad-attribution\">")
        block << "<p>${escape(attributionText)}</p>"
        if (apiNoteText) {
            block << "<p>${apiNoteText}</p>"
        }
        block << "</blockquote>"
        return block.toString()
    }

    private String extractApiNoteText(CommentDoc comment) {
        if (!comment.blockTags) return ""
        BlockTag apiNoteTag = comment.blockTags.find { it.kind == BlockTagKind.CUSTOM && it.name == "apiNote" }
        if (apiNoteTag) {
            String rendered = renderNodes(apiNoteTag.body, "", null)
            if (rendered) {
                // 剥离可能的 @apiNote 前缀（body 渲染结果可能包含标签名）
                return rendered.replaceAll(/^@apiNote\s+/, "")
            }
            String raw = apiNoteTag.rawText?.trim() ?: ""
            if (raw) {
                // rawText 通常包含 "@apiNote xxx"，需要去除前缀
                return escape(raw.replaceAll(/^@apiNote\s+/, ""))
            }
            return ""
        }
        // 回退：从 rawText 中解析 @apiNote（正则已只取正文）
        if (comment.rawText) {
            def matcher = comment.rawText =~ /(?m)^@apiNote\s+(.+)$/
            if (matcher) {
                return escape(unescapeJavaUnicode(matcher[0][1]?.trim() ?: ""))
            }
        }
        return ""
    }

    private static String extractAuthorName(CommentDoc comment) {
        // 首先从 blockTags 中查找
        if (comment.blockTags) {
            for (BlockTag tag : comment.blockTags) {
                if (tag.kind == BlockTagKind.CUSTOM && tag.name == "author") {
                    return tag.rawText?.trim()?.split(/\n/)[0]?.trim() ?: ""
                }
            }
        }
        // 回退：从 rawText 中解析 @author 标签
        // DocumentationTool API 不支持 -author 标志，JDK 默认会过滤掉 @author，
        // 但 rawText (tree.toString()) 保留了完整的注释文本
        if (comment.rawText) {
            def matcher = comment.rawText =~ /(?m)^@author\s+(.+)$/
            if (matcher) {
                return unescapeJavaUnicode(matcher[0][1]?.trim()?.split(/\n/)[0]?.trim() ?: "")
            }
        }
        return ""
    }

    private static boolean hasSinceTag(CommentDoc comment) {
        if (comment?.blockTags?.any { BlockTag tag -> tag.kind == BlockTagKind.SINCE }) {
            return true
        }
        if (comment?.rawText) {
            def matcher = comment.rawText =~ /(?m)^@since(?:\s+.*)?$/
            return matcher.find()
        }
        return false
    }

    private static String extractSinceDate(CommentDoc comment) {
        String sinceValue = extractSinceValue(comment)
        if (sinceValue) {
            return formatSinceDate(sinceValue)
        }
        String dateValue = extractDateValue(comment)
        return formatSinceDate(dateValue)
    }

    private static String extractSinceValue(CommentDoc comment) {
        if (comment.blockTags) {
            for (BlockTag tag : comment.blockTags) {
                if (tag.kind == BlockTagKind.SINCE) {
                    if (tag instanceof SinceTag) {
                        return ((SinceTag) tag).version ?: tag.rawText ?: ""
                    }
                    return tag.rawText ?: ""
                }
            }
        }
        if (comment.rawText) {
            def matcher = comment.rawText =~ /(?m)^@since\s+(.+)$/
            if (matcher.find()) {
                return unescapeJavaUnicode(matcher.group(1)?.trim() ?: "")
            }
        }
        return ""
    }

    private static String extractDateValue(CommentDoc comment) {
        if (comment.blockTags) {
            for (BlockTag tag : comment.blockTags) {
                if (tag.kind == BlockTagKind.CUSTOM && tag.name == "date") {
                    return tag.rawText ?: ""
                }
            }
        }
        if (comment.rawText) {
            def matcher = comment.rawText =~ /(?m)^@date\s+(.+)$/
            if (matcher.find()) {
                return unescapeJavaUnicode(matcher.group(1)?.trim() ?: "")
            }
        }
        return ""
    }
    /**
     * 从 rawText 中提取最后一个 @author/@since/@date 标签之后出现的正文内容。
     * JDK DocCommentTree API 中，bodyNodes 只包含 block tags 之前的正文，
     * blockTags 存储了 @author/@since 等标签但不含后续正文段落，
     * 因此需要从 rawText 中提取 @author/@since 之后的正文。
     */
    private static String extractPostAttributionText(CommentDoc comment) {
        if (!comment.rawText) return ""
        String raw = comment.rawText
        // 找到最后一个 @author/@since/@date 标签的行
        def matcher = raw =~ /(?m)^@(?:author|since|date)\s+.+$/
        if (!matcher) return ""
        // 获取最后一个匹配的结束位置
        int lastMatchEnd = -1
        while (matcher.find()) {
            lastMatchEnd = matcher.end()
        }
        if (lastMatchEnd < 0) return ""
        // 取最后一个 @author/@since/@date 行之后的所有内容
        String afterAttribution = raw.substring(lastMatchEnd)
        // 按段落分割（空行分隔），遇到其他 block tags 后停止收集
        List<String> paragraphs = []
        StringBuilder currentPara = new StringBuilder()
        for (String line : afterAttribution.readLines()) {
            String trimmed = line.trim()
            // 跳过空行（段落分隔符）
            if (trimmed.isEmpty()) {
                if (currentPara.length() > 0) {
                    paragraphs.add(currentPara.toString().trim())
                    currentPara = new StringBuilder()
                }
                continue
            }
            // 后续 block tags 不属于署名说明，停止收集，避免吞掉参数/返回值文档
            if (trimmed.matches(/^@\w+\b.*/)) {
                if (currentPara.length() > 0) {
                    paragraphs.add(currentPara.toString().trim())
                    currentPara = new StringBuilder()
                }
                break
            }
            // 跳过 /** 和 */ 等注释标记
            if (trimmed.matches(/^[\/\*]+\s*$/) || trimmed.matches(/^\*\s*[\/]/)) {
                continue
            }
            // 移除行首的 * 和空格
            String content = line.replaceAll(/^\s*\*\s?/, "")
            if (content.trim()) {
                currentPara << content.trim()
                if (!content.trim().endsWith('.')) {
                    currentPara << " "
                }
            }
        }
        if (currentPara.length() > 0) {
            paragraphs.add(currentPara.toString().trim())
        }
        return unescapeJavaUnicode(paragraphs.findAll { it && !it.isEmpty() }.join("\n"))
    }

    /**
     * 将 JDK DocCommentTree.toString() 输出的 \\uXXXX Unicode 转义还原为原始字符。
     * JDK 对非 ASCII 字符（如中文）会输出 \\u6ce8 而非 "注"，此方法还原为可读文本。
     */
    private static String unescapeJavaUnicode(String text) {
        if (!text) return ""
        return text.replaceAll(/\\u([0-9a-fA-F]{4})/, { match ->
            Character.toString((char) Integer.parseInt(match[1], 16))
        })
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
        return cleaned.split(/\s+/)[0]
    }

    String renderNodes(List<CommentNode> nodes, String fromPage, DocProjection projection) {
        return (nodes ?: []).collect { renderNode(it, fromPage, projection) }
                .findAll { it != null && !it.isEmpty() }
                .join("")
                .replaceAll(/\s+([,.;:!?])/, '$1')
                .trim()
    }

    private String renderNode(CommentNode node, String fromPage, DocProjection projection) {
        if (node == null) return ""
        if (node.kind == CommentNodeKind.HTML) {
            return renderHtmlNode(node)
        }
        if (node.kind == CommentNodeKind.ENTITY) {
            return renderEntity(node.text)
        }
        if (node.inlineTag != null) {
            return renderInline(node.inlineTag, fromPage, projection)
        }
        if ((node.text ?: "") ==~ /(?i)<\/?p\s*\/?>|<br\s*\/?>/) {
            return renderLegacyHtmlNode(node.text)
        }
        return escape(node.text)
    }

    private static String renderHtmlNode(CommentNode node) {
        String name = normalizeHtmlName(node.htmlName)
        if (!name) {
            return renderLegacyHtmlNode(node.text)
        }
        if (!isAllowedHtmlTag(name)) {
            return escape(node.text ?: "<${node.htmlEnd ? '/' : ''}${name}>")
        }
        if (node.htmlEnd) {
            return VOID_HTML_TAGS.contains(name) ? "" : "</${name}>"
        }
        if (!node.htmlStart) {
            return escape(node.text)
        }
        String attributes = renderHtmlAttributes(name, node.htmlAttributes ?: [:])
        if (VOID_HTML_TAGS.contains(name)) {
            return "<${name}${attributes}>"
        }
        if (node.htmlSelfClosing) {
            return "<${name}${attributes}></${name}>"
        }
        return "<${name}${attributes}>"
    }

    private static String renderLegacyHtmlNode(String text) {
        String value = text ?: ""
        if (value ==~ /(?i)<\/?p\s*\/?>/) return ""
        if (value ==~ /(?i)<br\s*\/?>/) return "<br>"
        return escape(value)
    }

    private static boolean isAllowedBlockHtml(CommentNode node) {
        if (node?.kind != CommentNodeKind.HTML) return false
        String name = normalizeHtmlName(node.htmlName)
        return name && BLOCK_HTML_TAGS.contains(name) && isAllowedHtmlTag(name)
    }

    private static boolean isVoidHtmlNode(CommentNode node) {
        String name = normalizeHtmlName(node?.htmlName)
        return name && VOID_HTML_TAGS.contains(name)
    }

    private static boolean isAllowedHtmlTag(String name) {
        return BLOCK_HTML_TAGS.contains(name) || INLINE_HTML_TAGS.contains(name)
    }

    private static String renderHtmlAttributes(String tagName, Map<String, String> attributes) {
        LinkedHashMap<String, String> safe = new LinkedHashMap<>()
        (attributes ?: [:]).each { String rawName, String rawValue ->
            String name = normalizeAttributeName(rawName)
            if (!isAllowedAttribute(tagName, name)) {
                return
            }
            String value = rawValue ?: ""
            if (URL_ATTRIBUTES.contains(name) && !isSafeUrl(value)) {
                return
            }
            if (name == "target" && !ALLOWED_TARGETS.contains(value)) {
                return
            }
            safe[name] = value
        }
        if (tagName == "a" && safe["target"] == "_blank" && !safe.containsKey("rel")) {
            safe["rel"] = "noopener noreferrer"
        }
        return safe.collect { String name, String value ->
            value == "" ? " ${name}" : " ${name}=\"${escapeAttribute(value)}\""
        }.join("")
    }

    private static boolean isAllowedAttribute(String tagName, String name) {
        if (!name || name.startsWith("on")) {
            return false
        }
        if (name in ["id", "class", "title", "role", "dir", "lang"] || name.startsWith("aria-") || name.startsWith("data-")) {
            return true
        }
        switch (tagName) {
            case "a":
                return name in ["href", "name", "target", "rel"]
            case "blockquote":
            case "q":
            case "del":
            case "ins":
                return name == "cite"
            case "time":
                return name == "datetime"
            case "td":
            case "th":
                return name in ["colspan", "rowspan", "scope", "headers"]
            default:
                return false
        }
    }

    private static String normalizeHtmlName(String value) {
        String name = value?.trim()
        if (!name || !(name ==~ /[A-Za-z][A-Za-z0-9:-]*/)) {
            return ""
        }
        return name.toLowerCase(Locale.ROOT)
    }

    private static String normalizeAttributeName(String value) {
        String name = value?.trim()
        if (!name || !(name ==~ /[A-Za-z_:][A-Za-z0-9_:.-]*/)) {
            return ""
        }
        return name.toLowerCase(Locale.ROOT)
    }

    private static boolean isSafeUrl(String value) {
        String trimmed = value?.trim()
        if (!trimmed) {
            return false
        }
        String lower = trimmed.toLowerCase(Locale.ROOT)
        if (lower.startsWith("#") || lower.startsWith("/") || lower.startsWith("./") || lower.startsWith("../")) {
            return true
        }
        def scheme = lower =~ /^([a-z][a-z0-9+.-]*):.*/
        if (scheme.matches()) {
            return scheme.group(1) in ["http", "https", "mailto", "tel"]
        }
        return !lower.startsWith("javascript:") && !lower.startsWith("data:")
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

    private static String escapeAttribute(String text) {
        return escape(text)
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
    }

    private static String escape(String text) {
        return (text ?: "")
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
    }
}

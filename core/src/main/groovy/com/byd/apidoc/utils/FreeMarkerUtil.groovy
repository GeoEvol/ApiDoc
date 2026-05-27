package com.byd.apidoc.utils


import com.byd.apidoc.model.TemplateData

import java.util.regex.Matcher
import java.util.regex.Pattern

class FreeMarkerUtil {

    private static final Pattern LINK_PATTERN = Pattern.compile("\\{@link\\s+([^}]+)}")

    static String relativePath(String fromPath, String toPath) {
        if (!toPath) return ""
        if (!fromPath) return toPath
        try {
            java.nio.file.Path target = java.nio.file.Paths.get(toPath)
            java.nio.file.Path from = java.nio.file.Paths.get(fromPath).getParent()
            java.nio.file.Path rel = from != null ? from.relativize(target) : target
            return rel.toString().replace('\\', '/')
        } catch (Exception ignored) {
            return toPath
        }
    }

    /**
     * 将 Javadoc 中的 {@link ...} 转换为 Markdown 或 HTML 链接
     * @param text 原始文本
     * @param data 模板数据（用于获取 TypeLinker 和当前文档路径）
     * @param currentDocPath 当前文档相对路径（如 "com/example/MyClass.md"）
     * @return 转换后的文本
     */
    static String convertJavadocLinks(String text, TemplateData data, String currentDocPath) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        Matcher matcher = LINK_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String className = matcher.group(1).trim();
            // 移除可能的 #methodName 或 (params) 部分，只保留类名
            int hashIndex = className.indexOf('#');
            int parenIndex = className.indexOf('(');
            if (hashIndex != -1) {
                className = className.substring(0, hashIndex);
            } else if (parenIndex != -1) {
                className = className.substring(0, parenIndex);
            }

            // 使用 TypeLinker 生成链接
            String linkedText = data.getTypeLinker().linkify(className, currentDocPath);
            // 如果 TypeLinker 无法链接，回退到纯文本
            String replacement = linkedText != className ? linkedText : formatAsCode(className);
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 格式化为代码样式
     */
    private static String formatAsCode(String text) {
        return "<code>${text}</code>"
    }

    /**
     * 转义 Markdown 特殊字符
     * 转义反引号、星号、下划线
     * @param text 原始文本
     * @return 转义后的文本
     */
    static String escapeMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.replace("`", "\\`")
                .replace("*", "\\*")
                .replace("_", "\\_");
    }

    /**
     * 转义 Markdown 表格单元格中的特殊字符
     * 额外转义管道符 |
     * @param text 原始文本
     * @return 转义后的文本
     */
    static String escapeMarkdownForTableCell(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.replace("|", "\\|")
                .replace("`", "\\`")
                .replace("*", "\\*")
                .replace("_", "\\_");
    }

    /**
     * 转义标识符中的 Markdown 特殊字符
     * 用于字段名、方法名、类名等标识符
     * 只转义真正会影响 Markdown 语法的字符，不转义下划线
     * @param text 原始文本
     * @return 转义后的文本
     */
    static String escapeMarkdownIdentifier(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        // 只转义反引号和星号，保留下划线（标识符中常见）
        return text.replace("`", "\\`")
                .replace("*", "\\*");
    }

    /**
     * 转义 HTML 特殊字符
     * @param text 原始文本
     * @return 转义后的文本
     */
    static String escapeHtml(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * 转义 HTML 表格单元格中的特殊字符
     * @param text 原始文本
     * @return 转义后的文本
     */
    static String escapeHtmlForTableCell(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return escapeHtml(text);
    }
}

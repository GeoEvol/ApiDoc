package com.byd.apidoc.utils

import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.ApiDoc

import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern

class TypeLinker {

    private final Map<String, String> simpleNameToPath = new HashMap<>()
    private final Map<String, String> fqcnToPath = new HashMap<>()
    private final Map<String, ApiDoc> pathToDoc = new HashMap<>()
    private final Set<String> duplicates
    private final Map<ApiDoc, String> docPathMap
    private final boolean isHtmlFormat

    private static final Pattern TYPE_PATTERN =
            Pattern.compile(/[A-Za-z_][A-Za-z0-9_\.\\$]*/)

    private static final String HTML_LINE_BREAK = "<br>"
    private static final String MD_LINE_BREAK = "\n"

    private static final Set<String> PRIMITIVES = [
            "byte", "short", "int", "long", "float", "double", "boolean", "char", "void"
    ] as Set

    TypeLinker(List<ApiDoc> apiDocs,
               Set<String> duplicateSimpleNames,
               Map<ApiDoc, String> docPathMap,
               String outputFormat = ApiConfig.FORMAT_MARKDOWN) {
        this.duplicates = duplicateSimpleNames ?: [] as Set
        this.docPathMap = docPathMap ?: [:]
        this.isHtmlFormat = ApiConfig.FORMAT_HTML.equalsIgnoreCase(outputFormat ?: ApiConfig.FORMAT_MARKDOWN)
        buildTypeMap(apiDocs)
    }

    private void buildTypeMap(List<ApiDoc> apiDocs) {
        apiDocs?.each { ApiDoc doc ->
            String path = docPathMap?.get(doc)
            if (!path) {
                return
            }

            String simple = doc.name
            String fqcn = doc.packageName ? "${doc.packageName}.${doc.name}" : doc.name

            if (!duplicates.contains(simple)) {
                simpleNameToPath.put(simple, path)
            }
            fqcnToPath.put(fqcn, path)
            pathToDoc.put(path, doc)
        }
    }

    String linkify(String type, String currentDocPath) {
        if (!type) {
            return ""
        }

        StringBuilder sb = new StringBuilder()
        int idx = 0
        def matcher = TYPE_PATTERN.matcher(type)
        while (matcher.find()) {
            String token = matcher.group()
            sb.append(type, idx, matcher.start())
            sb.append(linkForToken(token, currentDocPath))
            idx = matcher.end()
        }
        sb.append(type, idx, type.length())
        return sb.toString()
    }

    String linkifyGeneric(String type, String currentDocPath, int maxDepth = 2) {
        if (!type) {
            return ""
        }
        return parseType(type, 0, maxDepth, currentDocPath)
    }

    String linkifyWithNewlines(String type, String currentDocPath) {
        if (!type) {
            return ""
        }
        return linkify(type.replaceAll("\\n", newlineReplacement()), currentDocPath)
    }

    String linkifyGenericWithNewlines(String type, String currentDocPath, int maxDepth = 2) {
        if (!type) {
            return ""
        }
        return linkifyGeneric(type.replaceAll("\\n", newlineReplacement()), currentDocPath, maxDepth)
    }

    String linkifyForTableCell(String type, String currentDocPath) {
        return linkify(normalizeForTableCell(type), currentDocPath)
    }

    String linkifyGenericForTableCell(String type, String currentDocPath, int maxDepth = 2) {
        return linkifyGeneric(normalizeForTableCell(type), currentDocPath, maxDepth)
    }

    String processJavadocTags(String comment, String currentDocPath) {
        if (!comment) {
            return ""
        }

        StringBuilder out = new StringBuilder()
        int index = 0
        while (index < comment.length()) {
            int start = comment.indexOf("{@", index)
            if (start < 0) {
                out.append(comment.substring(index))
                break
            }

            out.append(comment, index, start)
            int end = findInlineTagEnd(comment, start)
            if (end < 0) {
                out.append(comment.substring(start))
                break
            }

            String body = comment.substring(start + 2, end).trim()
            out.append(renderInlineTag(body, currentDocPath))
            index = end + 1
        }
        return out.toString()
    }

    String linkifyDescription(String text, String currentDocPath) {
        if (!text) {
            return ""
        }
        return processJavadocTags(text.replaceAll("\\n", newlineReplacement()), currentDocPath)
    }

    String linkifyDescriptionForTableCell(String text, String currentDocPath) {
        return processJavadocTags(normalizeForTableCell(text), currentDocPath)
    }

    private String renderInlineTag(String body, String currentDocPath) {
        int split = firstWhitespace(body)
        String tagName = split >= 0 ? body.substring(0, split) : body
        String content = split >= 0 ? body.substring(split).trim() : ""

        switch (tagName) {
            case "link":
                return renderLinkTag(content, currentDocPath, true)
            case "linkplain":
                return renderLinkTag(content, currentDocPath, false)
            case "code":
                return formatCode(content)
            case "literal":
                return content
            default:
                return "{@${body}}"
        }
    }

    private String renderLinkTag(String content, String currentDocPath, boolean codeStyle) {
        LinkParts parts = parseLinkParts(content)
        if (!parts.reference) {
            return codeStyle ? formatCode(content) : content
        }

        String label = parts.label ? processJavadocTags(parts.label, currentDocPath) : escapeLinkLabel(parts.reference)
        LinkTarget target = resolveReference(parts.reference, currentDocPath)
        if (target?.relativeUrl) {
            return createJavadocLink(target.relativeUrl, label, codeStyle)
        }

        return codeStyle ? formatCode(label) : label
    }

    private LinkParts parseLinkParts(String content) {
        int parenDepth = 0
        int angleDepth = 0
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i)
            switch (ch) {
                case '(':
                    parenDepth++
                    break
                case ')':
                    parenDepth = Math.max(0, parenDepth - 1)
                    break
                case '<':
                    angleDepth++
                    break
                case '>':
                    angleDepth = Math.max(0, angleDepth - 1)
                    break
                default:
                    if (Character.isWhitespace(ch) && parenDepth == 0 && angleDepth == 0) {
                        String reference = content.substring(0, i).trim()
                        String label = content.substring(i).trim()
                        return new LinkParts(reference, label)
                    }
            }
        }
        return new LinkParts(content.trim(), "")
    }

    private LinkTarget resolveReference(String reference, String currentDocPath) {
        if (!reference) {
            return null
        }

        if (reference.startsWith("#")) {
            String fragment = reference.substring(1)
            return currentDocPath ? new LinkTarget(currentDocPath, fragment, "#${fragment}") : null
        }

        int hashIndex = reference.indexOf('#')
        String typeRef = hashIndex >= 0 ? reference.substring(0, hashIndex) : reference
        String memberRef = hashIndex >= 0 ? reference.substring(hashIndex + 1) : null
        String targetPath = findTypePath(typeRef)
        if (!targetPath) {
            return null
        }

        return new LinkTarget(targetPath, memberRef, toRelativePath(currentDocPath, targetPath) + (memberRef ? "#${memberRef}" : ""))
    }

    private String escapeLinkLabel(String label) {
        return label ?: ""
    }

    private String createJavadocLink(String url, String display, boolean codeStyle) {
        if (isHtmlFormat) {
            String inner = codeStyle ? "<code>${display}</code>" : display
            return "<a href=\"${url}\" class=\"type-link\">${inner}</a>"
        }
        return codeStyle ? "[`${display}`](${url})" : "[${display}](${url})"
    }

    private String formatCode(String value) {
        if (isHtmlFormat) {
            return "<code>${value}</code>"
        }
        return "`${value}`"
    }

    private String linkForToken(String token, String currentDocPath) {
        if (PRIMITIVES.contains(token)) {
            return token
        }
        String targetPath = findTypePath(token)
        if (!targetPath) {
            return token
        }
        return createTypeLink(toRelativePath(currentDocPath, targetPath), token)
    }

    private String createTypeLink(String url, String display) {
        if (isHtmlFormat) {
            return "<a href=\"${url}\" class=\"type-link\">${display}</a>"
        }
        return "[${display}](${url})"
    }

    private String findTypePath(String typeName) {
        if (!typeName) {
            return null
        }

        String path = fqcnToPath.get(typeName)
        if (path) {
            return path
        }

        if (!duplicates.contains(typeName)) {
            path = simpleNameToPath.get(typeName)
            if (path) {
                return path
            }
        }

        if (typeName.contains('.')) {
            String parentTypeName = extractParentTypeName(typeName)
            if (parentTypeName) {
                path = fqcnToPath.get(parentTypeName)
                if (path) {
                    return path
                }
                if (!duplicates.contains(parentTypeName)) {
                    path = simpleNameToPath.get(parentTypeName)
                    if (path) {
                        return path
                    }
                }
            }

            String[] parts = typeName.split(/\./)
            String simpleName = parts[parts.length - 1]
            if (!duplicates.contains(simpleName)) {
                path = simpleNameToPath.get(simpleName)
                if (path) {
                    return path
                }
            }
        }

        return null
    }

    private String parseType(String type, int depth, int maxDepth, String currentDocPath) {
        if (depth > maxDepth) {
            return type
        }
        StringBuilder out = new StringBuilder()
        int i = 0
        while (i < type.length()) {
            char ch = type.charAt(i)
            if (Character.isLetter(ch) || ch == '_') {
                int start = i
                i++
                while (i < type.length()) {
                    char c = type.charAt(i)
                    if (Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '$') {
                        i++
                    } else {
                        break
                    }
                }
                String token = type.substring(start, i)
                out.append(linkForToken(token, currentDocPath))
                continue
            } else if (ch == '<') {
                int end = findMatching(type, i, (char) '<', (char) '>')
                if (end > i) {
                    String inner = type.substring(i + 1, end)
                    out.append('<').append(parseType(inner, depth + 1, maxDepth, currentDocPath)).append('>')
                    i = end + 1
                    continue
                }
            }
            out.append(ch)
            i++
        }
        return out.toString()
    }

    private static int findMatching(String text, int startIdx, char open, char close) {
        int depth = 0
        for (int i = startIdx; i < text.length(); i++) {
            char ch = text.charAt(i)
            if (ch == open) {
                depth++
            } else if (ch == close) {
                depth--
                if (depth == 0) {
                    return i
                }
            }
        }
        return -1
    }

    private static int findInlineTagEnd(String text, int start) {
        int depth = 0
        for (int i = start; i < text.length(); i++) {
            if (text.charAt(i) == '{' && i + 1 < text.length() && text.charAt(i + 1) == '@') {
                depth++
                i++
                continue
            }
            if (text.charAt(i) == '}') {
                depth--
                if (depth == 0) {
                    return i
                }
            }
        }
        return -1
    }

    private static int firstWhitespace(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i
            }
        }
        return -1
    }

    private static String extractParentTypeName(String nestedTypeName) {
        if (!nestedTypeName || !nestedTypeName.contains('.')) {
            return null
        }
        return nestedTypeName.substring(0, nestedTypeName.lastIndexOf('.'))
    }

    private static String normalizeForTableCell(String text) {
        if (!text) {
            return ""
        }
        return text.replaceAll(/\s+/, " ").trim()
    }

    private String newlineReplacement() {
        return isHtmlFormat ? HTML_LINE_BREAK : MD_LINE_BREAK
    }

    private static String toRelativePath(String fromPath, String toPath) {
        try {
            Path target = Paths.get(toPath)
            Path from = fromPath ? Paths.get(fromPath).getParent() : null
            Path rel = from != null ? from.relativize(target) : target
            return rel.toString().replace('\\', '/')
        } catch (Exception ignored) {
            return toPath
        }
    }

    private static class LinkParts {
        final String reference
        final String label

        LinkParts(String reference, String label) {
            this.reference = reference
            this.label = label
        }
    }

    private static class LinkTarget {
        final String targetPath
        final String fragment
        final String relativeUrl

        LinkTarget(String targetPath, String fragment, String relativeUrl) {
            this.targetPath = targetPath
            this.fragment = fragment
            this.relativeUrl = relativeUrl
        }
    }
}

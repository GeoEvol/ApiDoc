package com.byd.apidoc.render

import com.byd.apidoc.output.OutputManifestWriter
import com.byd.apidoc.metadata.ApiValueRange
import com.byd.apidoc.projection.ApiStatusModel
import com.byd.apidoc.projection.BreadcrumbModel
import com.byd.apidoc.projection.InheritedMemberGroupModel
import com.byd.apidoc.projection.MemberDetailModel
import com.byd.apidoc.projection.MemberGroupModel
import com.byd.apidoc.projection.MemberSummaryModel
import com.byd.apidoc.projection.PageKind
import com.byd.apidoc.projection.PageModel
import com.byd.apidoc.projection.PackagePageModel
import com.byd.apidoc.projection.PackageTypeGroupModel
import com.byd.apidoc.projection.TocEntryModel
import com.byd.apidoc.projection.TypePageModel
import com.byd.apidoc.reference.LinkPathResolver

class MarkdownRenderer {
    static final String OUTPUT_DIR = OutputManifestWriter.MARKDOWN_OUTPUT_DIR.replaceAll('/$', '')
    private final MarkdownTypeRefRenderer typeRefRenderer = new MarkdownTypeRefRenderer()
    private final MarkdownCommentRenderer commentRenderer = new MarkdownCommentRenderer()
    private final LinkPathResolver pathResolver = new LinkPathResolver()

    void render(RenderContext context) {
        File root = new File(context.outputDir, OUTPUT_DIR)
        root.mkdirs()
        write(new File(root, "index.md"), index(context))
        write(new File(root, "packages.md"), packages(context))
        write(new File(root, "classes.md"), classes(context))
        context.projection.pages.findAll { it.kind == PageKind.PACKAGE }.each { PageModel page ->
            write(new File(root, markdownPath(page.url)), packagePage(context, page))
        }
        context.projection.typePages.each { TypePageModel page ->
            PageModel pageModel = context.projection.pages.find { it.targetId?.stableKey() == page.id?.stableKey() }
            String pageUrl = markdownPath(pageModel?.url ?: "reference/${page.id?.qualifiedName}.html")
            write(new File(root, pageUrl), typePage(context, page, pageUrl))
        }
    }

    private static String index(RenderContext context) {
        StringBuilder out = new StringBuilder("# ${escape(context.projectName ?: 'API Reference')}\n\n")
        out << "- [Packages](packages.md)\n"
        out << "- [Classes](classes.md)\n"
        return out.toString()
    }

    private static String packages(RenderContext context) {
        StringBuilder out = new StringBuilder("# Packages\n\n")
        context.projection.pages.findAll { it.kind == PageKind.PACKAGE }.sort { it.title }.each { PageModel page ->
            out << "- [${escape(page.title)}](${markdownPath(page.url)})"
            if (page.summary) out << " - ${escape(page.summary)}"
            out << "\n"
        }
        return out.toString()
    }

    private static String classes(RenderContext context) {
        StringBuilder out = new StringBuilder("# Classes\n\n")
        context.projection.typePages.sort { it.id?.qualifiedName ?: it.title }.each { TypePageModel page ->
            String url = markdownPath(context.projection.pages.find { it.targetId?.stableKey() == page.id?.stableKey() }?.url ?: "reference/${page.id?.qualifiedName}.html")
            out << "- [${escape(page.id?.qualifiedName ?: page.title)}](${url})"
            if (page.summary) out << " - ${escape(page.summary)}"
            out << "\n"
        }
        return out.toString()
    }

    private static String packagePage(RenderContext context, PageModel page) {
        StringBuilder out = new StringBuilder("# Package ${escape(page.title)}\n\n")
        out << "Overview\n\n"
        if (page.summary) out << "${escape(page.summary)}\n\n"
        PackagePageModel packagePage = context.projection.packagePages.find { it.packageName == page.title }
        (packagePage?.typeGroups ?: []).each { PackageTypeGroupModel group ->
            out << "## ${escape(group.label)}\n\n"
            group.types.each { TypePageModel type ->
                String url = markdownPath(context.projection.pages.find { it.targetId?.stableKey() == type.id?.stableKey() }?.url ?: "reference/${type.id?.qualifiedName}.html")
                out << "- [${escape(type.title)}](${new LinkPathResolver().markdownUrl(markdownPath(page.url), url)})"
                if (type.summary) out << " - ${escape(type.summary)}"
                out << "\n"
            }
            out << "\n"
        }
        return out.toString()
    }

    private String typePage(RenderContext context, TypePageModel page, String pageUrl) {
        StringBuilder out = new StringBuilder("# ${escape(page.title)}\n\n")
        out << breadcrumbs(page, pageUrl)
        out << "<a id=\"summary\"></a>\n\n"
        out << "**Package:** `${page.typeHeader?.packageName ?: page.packageName ?: 'default'}`\n\n"
        out << supportedPlatforms(page.platforms)
        out << apiStatus(page.apiStatus)
        out << contents(page)
        if (page.declaration) out << "```java\n${page.declaration}\n```\n\n"
        String body = commentRenderer.renderBody(page.comment, pageUrl, context.projection)
        if (body) {
            out << "${body}\n\n"
        } else if (page.summary) {
            out << "${escape(page.summary)}\n\n"
        }
        String typeTags = commentRenderer.renderBlockTags(page.comment, pageUrl, context.projection)
        if (typeTags) out << "${typeTags}\n"
        if (page.inheritance?.displayName) out << "**Extends:** ${typeRefRenderer.render(page.inheritance, pageUrl, context.projection)}\n\n"
        if (page.interfaces) out << "**Implements:** ${page.interfaces.collect { typeRefRenderer.render(it, pageUrl, context.projection) }.join(', ')}\n\n"
        page.memberGroups.each { MemberGroupModel group ->
            out << "<a id=\"${anchorName(group.title)}\"></a>\n\n"
            out << "## ${escape(group.title)}\n\n"
            group.members.each { member ->
                out << "- [${escape(member.displayName ?: member.name)}](#${escapeAnchorHref(member.id?.effectiveAnchorId() ?: member.name)})"
                String memberStatus = inlineStatus(member.status)
                if (memberStatus) out << " - ${memberStatus}"
                if (member.summary) out << " - ${escape(member.summary)}"
                out << "\n"
            }
            out << "\n"
        }
        if (page.memberDetails) {
            out << "<a id=\"details\"></a>\n\n"
            out << "## Details\n\n"
            page.memberDetails.each { MemberDetailModel detail ->
                out << "### ${escape(detail.displayName ?: detail.name)}\n\n"
                out << "<a id=\"${escapeHtmlAttr(detail.id?.effectiveAnchorId() ?: detail.name)}\"></a>\n\n"
                out << supportedPlatforms(detail.platforms)
                out << apiStatus(detail.status)
                if (detail.declaration) out << "```java\n${detail.declaration}\n```\n\n"
                String detailBody = commentRenderer.renderBody(detail.comment, pageUrl, context.projection)
                if (detailBody) {
                    out << "${detailBody}\n\n"
                } else if (detail.summary) {
                    out << "${escape(detail.summary)}\n\n"
                }
                String tags = commentRenderer.renderBlockTags(detail.comment, pageUrl, context.projection)
                if (tags) out << "${tags}\n"
            }
        }
        out << inheritedMembers(page, pageUrl)
        return out.toString()
    }

    private String breadcrumbs(TypePageModel page, String pageUrl) {
        if (!page.breadcrumbs) return ""
        String links = page.breadcrumbs.collect { BreadcrumbModel crumb ->
            String href = crumb.url ? relativeMarkdownUrl(pageUrl, crumb.url) : "#"
            "[${escape(crumb.label)}](${href})"
        }.join(" / ")
        return "${links}\n\n"
    }

    private static String contents(TypePageModel page) {
        if (!page.rightToc) return ""
        StringBuilder out = new StringBuilder("## Contents\n\n")
        page.rightToc.each { TocEntryModel entry ->
            out << "- [${escape(entry.label)}](#${entry.anchor})\n"
        }
        out << "\n"
        return out.toString()
    }

    private String inheritedMembers(TypePageModel page, String pageUrl) {
        StringBuilder out = new StringBuilder()
        out << "<a id=\"inherited-members\"></a>\n\n"
        out << "## Inherited Members\n\n"
        if (!page.inheritedMemberGroups) {
            out << "_No inherited members._\n"
            return out.toString()
        }
        page.inheritedMemberGroups.each { InheritedMemberGroupModel group ->
            out << "### ${escape(group.title ?: "Inherited from ${group.ownerName ?: group.ownerQualifiedName}")}\n\n"
            group.members.each { MemberSummaryModel member ->
                String href = member.url ? relativeMarkdownUrl(pageUrl, member.url) : "#"
                out << "- [${escape(member.displayName ?: member.name)}](${href})"
                String memberStatus = inlineStatus(member.status)
                if (memberStatus) out << " - ${memberStatus}"
                if (member.summary) out << " - ${escape(member.summary)}"
                out << "\n"
            }
            out << "\n"
        }
        return out.toString()
    }

    private String apiStatus(ApiStatusModel status) {
        List<String> chips = statusChips(status)
        if (!chips) return ""
        StringBuilder out = new StringBuilder("## API Status\n\n")
        chips.each { String chip -> out << "- ${escape(chip)}\n" }
        out << "\n"
        return out.toString()
    }

    private static String supportedPlatforms(Collection<String> platforms) {
        List<String> values = (platforms ?: []).findAll { it }.collect { "`" + escape(it.toString()) + "`" }
        return values ? "**Supported platforms:** ${values.join(', ')}\n\n" : ""
    }

    private static String inlineStatus(ApiStatusModel status) {
        List<String> chips = statusChips(status)
        return chips ? chips.collect { escape(it) }.join(", ") : ""
    }

    private static List<String> statusChips(ApiStatusModel status) {
        if (status == null) return []
        List<String> chips = []
        if (status.hidden) chips.add("Hidden")
        if (status.deprecated && !status.deprecatedMessage) chips.add("Deprecated")
        if (status.deprecatedMessage) chips.add("Deprecated: ${status.deprecatedMessage}")
        if (status.removed && !status.removedMessage) chips.add("Removed")
        if (status.removedMessage) chips.add("Removed: ${status.removedMessage}")
        if (status.pending) chips.add("Pending")
        if (status.since) chips.add("Since ${status.since}")
        if (status.apiSince != null) chips.add("API ${status.apiSince}")
        if (status.sdkExtensionSince) chips.add(status.sdkExtensionSince)
        if (status.deprecatedSince) chips.add("Deprecated since ${status.deprecatedSince}")
        if (status.removedSince) chips.add("Removed since ${status.removedSince}")
        if (status.nullability) chips.add(status.nullability)
        (status.permissions ?: []).each { chips.add(it) }
        (status.valueRanges ?: []).each { ApiValueRange range ->
            String value = [range.from, range.to].findAll { it != null }.join("..")
            chips.add("${range.kind ?: 'Range'} ${value}".trim())
        }
        return chips
    }

    private static String markdownPath(String url) {
        return (url ?: "").replaceAll(/\.html$/, ".md")
    }

    private String relativeMarkdownUrl(String fromPage, String targetUrl) {
        String[] parts = (targetUrl ?: "").split("#", 2)
        return pathResolver.markdownUrl(fromPage, markdownPath(parts[0]), parts.length > 1 ? escapeAnchorHref(parts[1]) : null)
    }

    private static String anchorName(String label) {
        (label ?: "").toLowerCase(Locale.ROOT).replaceAll(/[^a-z0-9]+/, "-").replaceAll(/^-|-$/, "")
    }

    private static void write(File file, String text) {
        file.parentFile?.mkdirs()
        file.text = text
    }

    private static String escape(String text) {
        return (text ?: "").replace("|", "\\|")
    }

    private static String escapeHtmlAttr(String text) {
        return (text ?: "")
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
    }

    private static String escapeAnchorHref(String anchor) {
        return (anchor ?: "")
                .replace("%", "%25")
                .replace("\"", "%22")
                .replace("<", "%3C")
                .replace(">", "%3E")
                .replace(" ", "%20")
    }
}

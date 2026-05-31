package com.byd.apidoc.render.html

import com.byd.apidoc.metadata.ApiValueRange
import com.byd.apidoc.model.DocMemberKind
import com.byd.apidoc.model.DocParameter
import com.byd.apidoc.model.DocTypeKind
import com.byd.apidoc.model.TypeRef
import com.byd.apidoc.output.JsonWriter
import com.byd.apidoc.output.OutputManifestWriter
import com.byd.apidoc.projection.ApiStatusModel
import com.byd.apidoc.projection.BreadcrumbModel
import com.byd.apidoc.projection.DocProjection
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
import com.byd.apidoc.render.HtmlCommentRenderer
import com.byd.apidoc.render.HtmlTypeRefRenderer
import com.byd.apidoc.render.RenderContext
import com.byd.apidoc.reference.LinkPathResolver

class HtmlSiteRenderer {
    static final String OUTPUT_DIR = OutputManifestWriter.HTML_OUTPUT_DIR.replaceAll('/$', '')

    private final JsonWriter jsonWriter = new JsonWriter()
    private final HtmlTypeRefRenderer typeRefRenderer = new HtmlTypeRefRenderer()
    private final HtmlCommentRenderer commentRenderer = new HtmlCommentRenderer()
    private final HtmlAssetWriter assetWriter = new HtmlAssetWriter()
    private final HtmlPageShellRenderer shellRenderer = new HtmlPageShellRenderer()
    private final LinkPathResolver pathResolver = new LinkPathResolver()

    void render(RenderContext context) {
        currentProjection = context.projection
        File root = new File(context.outputDir, OUTPUT_DIR)
        root.mkdirs()
        write(new File(root, "index.html"), shellRenderer.render(context, "API Reference", index(context), "", "index.html"))
        write(new File(root, "packages.html"), shellRenderer.render(context, "Packages", packages(context), "", "packages.html"))
        write(new File(root, "classes.html"), shellRenderer.render(context, "Classes", classes(context), "", "classes.html"))
        context.projection.pages.findAll { it.kind == PageKind.PACKAGE }.each { PageModel page ->
            write(new File(root, page.url), shellRenderer.render(context, page.title, packagePage(context, page), rootPrefix(page.url), page.url))
        }
        context.projection.typePages.each { TypePageModel page ->
            PageModel pageModel = context.projection.pages.find { it.targetId?.stableKey() == page.id?.stableKey() }
            String pageUrl = pageModel?.url ?: "reference/${page.id?.qualifiedName}.html"
            write(new File(root, pageUrl), shellRenderer.render(context, page.title, typePage(context, page, pageUrl), rootPrefix(pageUrl), pageUrl, rightToc(page)))
        }
        assetWriter.write(root)
        jsonWriter.write(context.projection.nav, new File(root, "nav-index.json"))
        jsonWriter.write(context.projection.search, new File(root, "search-index.json"))
    }

    private static String index(RenderContext context) {
        return """      <article class="ad-api-index">
        <h1>API Reference</h1>
        <ul>
          <li><a href="packages.html">Packages</a></li>
          <li><a href="classes.html">Classes</a></li>
        </ul>
      </article>
"""
    }

    private static String packages(RenderContext context) {
        String items = context.projection.pages.findAll { it.kind == PageKind.PACKAGE }.sort { it.title }.collect { PageModel page ->
            """          <li class="ad-index-row"${platformData(page.platforms)}><img class="ad-index-icon" src="assets/icon/package.svg" alt="" aria-hidden="true"><div><a href="${escapeAttr(page.url)}">${escape(page.title)}</a>${page.summary ? "<p>${escape(page.summary)}</p>" : ""}</div></li>"""
        }.join("\n")
        return """      <article class="ad-api-index">
        <h1>Packages</h1>
        <ul>
${items}
        </ul>
      </article>
"""
    }

    private static String classes(RenderContext context) {
        String items = context.projection.typePages.sort { it.id?.qualifiedName ?: it.title }.collect { TypePageModel page ->
            PageModel pageModel = context.projection.pages.find { it.targetId?.stableKey() == page.id?.stableKey() }
            String url = pageModel?.url ?: "reference/${page.id?.qualifiedName}.html"
            """          <li class="ad-index-row"${platformData(page.platforms)}><img class="ad-index-icon" src="assets/icon/${typeIcon(page)}.svg" alt="" aria-hidden="true"><div><a href="${escapeAttr(url)}">${escape(page.id?.qualifiedName ?: page.title)}</a>${page.summary ? "<p>${escape(page.summary)}</p>" : ""}</div></li>"""
        }.join("\n")
        return """      <article class="ad-api-index">
        <h1>Classes</h1>
        <ul>
${items}
        </ul>
      </article>
"""
    }

    private String packagePage(RenderContext context, PageModel page) {
        PackagePageModel packagePage = context.projection.packagePages.find { it.packageName == page.title }
        List<PackageTypeGroupModel> groups = packagePage?.typeGroups ?: []
        String items = groups.collect { PackageTypeGroupModel group ->
            String rows = group.types.collect { TypePageModel type ->
                PageModel model = context.projection.pages.find { it.targetId?.stableKey() == type.id?.stableKey() }
                String url = relativeUrl(page.url, model?.url ?: "reference/${type.id?.qualifiedName}.html")
                """            <li class="ad-index-row"${platformData(type.platforms)}><img class="ad-index-icon" src="${rootPrefix(page.url)}assets/icon/${typeIconForGroup(group.kind)}.svg" alt="" aria-hidden="true"><div><a href="${escapeAttr(url)}">${escape(type.title)}</a>${platformBadges(type.platforms)}${type.summary ? "<p>${escape(type.summary)}</p>" : ""}</div></li>"""
            }.join("\n")
            """        <section class="ad-package-type-group ad-package-group"${platformData(group.platforms)}>
          <h2>${escape(group.label)}</h2>
          <ul>
${rows}
          </ul>
        </section>"""
        }.join("\n")
        return """      <article class="ad-api-index">
        <h1>Package ${escape(page.title)}</h1>
        <p class="ad-package-overview-label">Overview</p>
        ${platformBadges(page.platforms)}
        ${page.summary ? "<p>${escape(page.summary)}</p>" : ""}
${items}
      </article>
"""
    }

    private String typePage(RenderContext context, TypePageModel page, String pageUrl) {
        StringBuilder out = new StringBuilder()
        out << "      <article class=\"ad-api-article\">\n"
        out << breadcrumbs(page, pageUrl)
        out << "        <header class=\"ad-api-header\" id=\"summary\">\n"
        out << "          <p class=\"ad-api-package\">${escape(page.typeHeader?.packageName ?: page.packageName ?: 'default')}</p>\n"
        out << "          <h1>${escape(page.typeHeader?.title ?: page.title)}</h1>\n"
        out << platformBadges(page.platforms)
        out << apiStatus(page.apiStatus)
        if (page.typeHeader?.declaration ?: page.declaration) {
            out << "          <pre class=\"ad-api-declaration ad-signature-card\"><code>${typeDeclaration(page, pageUrl)}</code><button class=\"ad-copy-code\" type=\"button\" aria-label=\"Copy declaration\"><img src=\"${rootPrefix(pageUrl)}assets/icon/copy.svg\" alt=\"\" aria-hidden=\"true\"></button></pre>\n"
        }
        out << "        </header>\n"
        String body = commentRenderer.renderBody(page.comment, pageUrl, context.projection)
        if (body) {
            out << "        ${body}\n"
        } else if (page.summary) {
            out << "        <p>${escape(page.summary)}</p>\n"
        }
        String typeTags = commentRenderer.renderBlockTags(page.comment, pageUrl, context.projection)
        if (typeTags) out << "        ${typeTags}\n"
        if (page.inheritance?.displayName) {
            out << "        <p><strong>Extends:</strong> ${typeRefRenderer.render(page.inheritance, pageUrl, context.projection)}</p>\n"
        }
        if (page.interfaces) {
            out << "        <p><strong>Implements:</strong> ${page.interfaces.collect { typeRefRenderer.render(it, pageUrl, context.projection) }.join(', ')}</p>\n"
        }
        page.memberGroups.each { MemberGroupModel group ->
            out << memberSummary(group, pageUrl)
        }
        if (page.memberDetails) {
            out << "        <section class=\"ad-member-details\" id=\"details\">\n"
            out << "          <h2>Details</h2>\n"
            page.memberDetails.each { MemberDetailModel detail ->
                out << memberDetail(context, pageUrl, detail)
            }
            out << "        </section>\n"
        }
        out << "      </article>\n"
        return out.toString()
    }

    private String breadcrumbs(TypePageModel page, String pageUrl) {
        if (!page.breadcrumbs) return ""
        String links = page.breadcrumbs.collect { BreadcrumbModel crumb ->
            String href = crumb.url ? relativeUrl(pageUrl, crumb.url) : "#"
            "<a href=\"${escapeAttr(href)}\">${escape(crumb.label)}</a>"
        }.join("<span>/</span>")
        return "        <nav class=\"ad-breadcrumbs\" aria-label=\"Breadcrumbs\">${links}</nav>\n"
    }

    private String memberSummary(MemberGroupModel group, String pageUrl) {
        String anchor = anchorName(group.title)
        String rows = group.members.collect { MemberSummaryModel member ->
            String anchorId = member.id?.effectiveAnchorId() ?: member.name
            """            <tr${platformData(member.platforms)}>
              <td class="ad-summary-modifier">${escape(member.modifierAndType ?: '')}</td>
              <td class="ad-member-main"><a class="ad-member-name" href="#${escapeAttr(anchorId)}">${escape(member.displayName ?: member.name)}</a>${apiStatus(member.status, "ad-api-status ad-api-status-inline")}${member.summary ? "<div class=\"ad-member-description\">${escape(member.summary)}</div>" : ""}</td>
            </tr>"""
        }.join("\n")
        return """        <section class="ad-member-summary" id="${escapeAttr(anchor)}">
          <h2 class="ad-member-section-title"><span class="ad-section-kind-icon ${escapeAttr(sectionIconClass(group.title))}" aria-hidden="true">${escape(sectionIcon(group.title))}</span><span>${escape(group.title)}</span></h2>
          <table class="ad-member-summary-table">
            <thead>
              <tr><th>Modifier and Type</th><th>Member</th></tr>
            </thead>
            <tbody>
${rows}
            </tbody>
          </table>
        </section>
"""
    }

    private String memberDetail(RenderContext context, String pageUrl, MemberDetailModel detail) {
        StringBuilder out = new StringBuilder()
        out << "          <section id=\"${escapeAttr(detail.id?.effectiveAnchorId() ?: detail.name)}\" class=\"ad-member-detail\"${platformData(detail.platforms)}>\n"
        out << "            <h3>${escape(detail.displayName ?: detail.name)} <button class=\"ad-copy-anchor\" type=\"button\" data-anchor=\"${escapeAttr(detail.id?.effectiveAnchorId() ?: detail.name)}\" aria-label=\"Copy anchor link\">#</button></h3>\n"
        out << platformBadges(detail.platforms)
        out << apiStatus(detail.status)
        if (detail.declaration) out << "            <pre class=\"ad-member-signature ad-signature-card\"><code>${memberDeclaration(detail, pageUrl)}</code><button class=\"ad-copy-code\" type=\"button\" aria-label=\"Copy signature\"><img src=\"${rootPrefix(pageUrl)}assets/icon/copy.svg\" alt=\"\" aria-hidden=\"true\"></button></pre>\n"
        String detailBody = commentRenderer.renderBody(detail.comment, pageUrl, context.projection)
        if (detailBody) {
            out << "            ${detailBody}\n"
        } else if (detail.summary) {
            out << "            <p>${escape(detail.summary)}</p>\n"
        }
        String tags = commentRenderer.renderBlockTags(detail.comment, pageUrl, context.projection, detail.throwsTypes, detail.returnType)
        if (tags) out << "            ${tags}\n"
        out << "          </section>\n"
        return out.toString()
    }

    private String inheritedMembers(TypePageModel page, String pageUrl) {
        StringBuilder out = new StringBuilder()
        out << "        <section class=\"ad-inherited-members\" id=\"inherited-members\">\n"
        out << "          <h2>Inherited Members</h2>\n"
        if (page.inheritedMemberGroups?.any()) {
            out << "          <div class=\"ad-inheritance-tree\" aria-label=\"Inheritance tree\">\n"
            out << "            <div>${escape(page.title)}</div>\n"
            page.inheritedMemberGroups.each { InheritedMemberGroupModel group ->
                out << "            <div><span>&rdsh;</span> ${escape(group.ownerQualifiedName ?: group.ownerName ?: '')}</div>\n"
            }
            out << "          </div>\n"
        }
        if (!page.inheritedMemberGroups) {
            out << "          <p><em>No inherited members.</em></p>\n"
        }
        page.inheritedMemberGroups.each { InheritedMemberGroupModel group ->
            out << "          <section class=\"ad-member-summary ad-inherited-member-group\">\n"
            out << "            <h3>${escape(group.title ?: "Inherited from ${group.ownerName ?: group.ownerQualifiedName}")}</h3>\n"
            out << "            <ul>\n"
            group.members.each { MemberSummaryModel member ->
                String href = member.url ? relativeUrl(pageUrl, member.url) : "#"
                out << "              <li><a href=\"${escapeAttr(href)}\">${escape(member.displayName ?: member.name)}</a>${member.summary ? " - ${escape(member.summary)}" : ""}</li>\n"
            }
            out << "            </ul>\n"
            out << "          </section>\n"
        }
        out << "        </section>\n"
        return out.toString()
    }

    private String typeDeclaration(TypePageModel page, String pageUrl) {
        String raw = page.typeHeader?.declaration ?: page.declaration ?: ""
        String base = raw
                .replaceFirst(/\s+extends\s+.+$/, "")
                .replaceFirst(/\s+implements\s+.+$/, "")
        List<String> parts = [escape(base)]
        TypeRef superType = page.typeHeader?.inheritance ?: page.inheritance
        if (superType?.qualifiedName && superType.qualifiedName != "java.lang.Object") {
            parts.add("extends ${typeRefRenderer.render(superType, pageUrl, nullSafeProjection(page))}")
        }
        List<TypeRef> interfaces = page.typeHeader?.interfaces ?: page.interfaces ?: []
        if (interfaces) {
            parts.add("implements ${interfaces.collect { typeRefRenderer.render(it, pageUrl, nullSafeProjection(page)) }.join(', ')}")
        }
        return parts.findAll { it }.join(" ")
    }

    private String memberDeclaration(MemberDetailModel detail, String pageUrl) {
        if (!detail.kind && !detail.returnType && !detail.type && !detail.parameters && !detail.throwsTypes) {
            return escape(detail.declaration)
        }
        String modifiers = detail.modifiers?.join(" ") ?: ""
        String name = escape(detail.name ?: detail.displayName ?: "")
        if (detail.kind in [DocMemberKind.METHOD, DocMemberKind.CONSTRUCTOR, DocMemberKind.ANNOTATION_ELEMENT]) {
            String returnType = detail.kind == DocMemberKind.CONSTRUCTOR ? "" : typeRefRenderer.render(detail.returnType, pageUrl, nullSafeProjection(null))
            String params = (detail.parameters ?: []).collect { DocParameter parameter -> renderParameter(parameter, pageUrl) }.join(", ")
            String throwsPart = detail.throwsTypes ? " throws ${detail.throwsTypes.collect { typeRefRenderer.render(it, pageUrl, nullSafeProjection(null)) }.join(', ')}" : ""
            return [escape(modifiers), returnType, "${name}(${params})${throwsPart}"]
                    .findAll { it != null && !it.toString().isEmpty() }
                    .join(" ")
        }
        String type = typeRefRenderer.render(detail.type, pageUrl, nullSafeProjection(null))
        return [escape(modifiers), type, name].findAll { it != null && !it.toString().isEmpty() }.join(" ")
    }

    private String renderParameter(DocParameter parameter, String pageUrl) {
        String type = typeRefRenderer.render(parameter?.type, pageUrl, nullSafeProjection(null))
        if (parameter?.varargs && type.endsWith("[]")) {
            type = "${type.substring(0, type.length() - 2)}..."
        }
        return [type, escape(parameter?.name ?: "")].findAll { it }.join(" ")
    }

    private DocProjection nullSafeProjection(TypePageModel ignored) {
        return currentProjection
    }

    private DocProjection currentProjection

    private static String rightToc(TypePageModel page) {
        String items = (page.rightToc ?: []).collect { TocEntryModel entry ->
            "        <a class=\"ad-toc-level-${entry.level ?: 2}\" href=\"#${escapeAttr(entry.anchor)}\">${escape(entry.label)}</a>"
        }.join("\n")
        return """<nav class="ad-devsite-toc" aria-label="On this page">
      <div class="ad-toc-title">On this page</div>
${items}
    </nav>"""
    }

    private static String typeIcon(TypePageModel page) {
        switch (page?.typeKind) {
            case DocTypeKind.INTERFACE:
                return "interface"
            case DocTypeKind.ENUM:
                return "enum"
            case DocTypeKind.ANNOTATION:
                return "annotation"
            case DocTypeKind.RECORD:
                return "record"
            case DocTypeKind.EXCEPTION:
            case DocTypeKind.ERROR:
                return "exception"
            default:
                return "class"
        }
    }

    private static String typeIconForGroup(String group) {
        String key = (group ?: "").toLowerCase(Locale.ROOT)
        if (key.contains("interface")) return "interface"
        if (key.contains("annotation")) return "annotation"
        if (key.contains("enum")) return "enum"
        if (key.contains("record")) return "record"
        if (key.contains("exception")) return "exception"
        if (key.contains("error")) return "exception"
        return "class"
    }

    private static String sectionIcon(String title) {
        String key = (title ?: "").toLowerCase(Locale.ROOT)
        if (key.contains("constant")) return "C"
        if (key.contains("field")) return "f"
        if (key.contains("constructor")) return "c"
        if (key.contains("annotation")) return "@"
        if (key.contains("method")) return "m"
        return "m"
    }

    private static String sectionIconClass(String title) {
        String key = (title ?: "").toLowerCase(Locale.ROOT)
        if (key.contains("constant")) return "ad-kind-constant"
        if (key.contains("field")) return "ad-kind-field"
        if (key.contains("constructor")) return "ad-kind-constructor"
        if (key.contains("annotation")) return "ad-kind-annotation"
        if (key.contains("method")) return "ad-kind-method"
        return "ad-kind-method"
    }

    private static String apiStatus(ApiStatusModel status, String cssClass = "ad-api-status") {
        List<Map<String, String>> chips = statusChips(status)
        if (!chips) return ""
        return "          <div class=\"${cssClass}\">${chips.collect { Map<String, String> chip -> "<span class=\"ad-status-chip ad-status-${escapeAttr(chip.kind)}\">${escape(chip.label)}</span>" }.join("")}</div>\n"
    }

    private static List<Map<String, String>> statusChips(ApiStatusModel status) {
        if (status == null) return []
        List<Map<String, String>> chips = []
        if (status.hidden) chips.add(statusChip("hidden", "Hidden"))
        if (status.deprecated) chips.add(statusChip("deprecated", "Deprecated"))
        if (status.deprecatedMessage) chips.add(statusChip("deprecated", "Deprecated: ${status.deprecatedMessage}"))
        if (status.removed) chips.add(statusChip("removed", "Removed"))
        if (status.removedMessage) chips.add(statusChip("removed", "Removed: ${status.removedMessage}"))
        if (status.pending) chips.add(statusChip("pending", "Pending"))
        if (status.since) chips.add(statusChip("since", "Since ${status.since}"))
        if (status.apiSince != null) chips.add(statusChip("since", "API ${status.apiSince}"))
        if (status.sdkExtensionSince) chips.add(statusChip("since", status.sdkExtensionSince))
        if (status.deprecatedSince) chips.add(statusChip("deprecated", "Deprecated since ${status.deprecatedSince}"))
        if (status.removedSince) chips.add(statusChip("removed", "Removed since ${status.removedSince}"))
        if (status.nullability) chips.add(statusChip("nullability", status.nullability))
        (status.permissions ?: []).each { chips.add(statusChip("permission", it)) }
        (status.valueRanges ?: []).each { ApiValueRange range ->
            String value = [range.from, range.to].findAll { it != null }.join("..")
            chips.add(statusChip("range", "${range.kind ?: 'Range'} ${value}".trim()))
        }
        return chips
    }

    private static Map<String, String> statusChip(String kind, String label) {
        [kind: kind ?: "info", label: label ?: ""]
    }

    private static String platformBadges(Collection<String> platforms) {
        List<String> values = (platforms ?: []).findAll { it }.collect { it.toString() }
        if (!values) return ""
        return "          <div class=\"ad-platform-badges\">${values.collect { String platform -> "<span class=\"ad-platform-badge\">${escape(platform)}</span>" }.join("")}</div>\n"
    }

    private static String platformData(Collection<String> platforms) {
        List<String> values = (platforms ?: []).findAll { it }.collect { it.toString() }
        return values ? " data-platforms=\"${escapeAttr(values.join(' '))}\"" : ""
    }

    private static void write(File file, String text) {
        file.parentFile?.mkdirs()
        file.text = text
    }

    private static String anchorName(String label) {
        label.toLowerCase(Locale.ROOT).replaceAll(/[^a-z0-9]+/, "-").replaceAll(/^-|-$/, "")
    }

    private static String rootPrefix(String pageUrl) {
        int depth = pageUrl?.tokenize('/')?.size() ?: 0
        return depth > 1 ? ("../" * (depth - 1)) : ""
    }

    private String relativeUrl(String fromPage, String targetUrl) {
        String[] parts = (targetUrl ?: "").split("#", 2)
        return pathResolver.htmlUrl(fromPage, parts[0], parts.length > 1 ? parts[1] : null)
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

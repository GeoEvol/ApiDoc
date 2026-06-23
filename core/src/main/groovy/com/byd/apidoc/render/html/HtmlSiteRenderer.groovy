package com.byd.apidoc.render.html

import com.byd.apidoc.comment.CommentDoc
import com.byd.apidoc.metadata.ApiValueRange
import com.byd.apidoc.model.DocMemberKind
import com.byd.apidoc.model.DocParameter
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
    private final HtmlSearchIndexViewBuilder htmlSearchIndexViewBuilder = new HtmlSearchIndexViewBuilder()

    void render(RenderContext context) {
        currentProjection = context.projection
        String timestamp = new Date().format("yyyy-MM-dd")
        File root = new File(context.outputDir, OUTPUT_DIR)
        root.mkdirs()
        write(new File(root, "index.html"), shellRenderer.render(context, "Package Index", packages(context), "", "index.html", packagesIndexToc(), timestamp))
        write(new File(root, "packages.html"), shellRenderer.render(context, "Package Index", packages(context), "", "packages.html", packagesIndexToc(), timestamp))
        write(new File(root, "classes.html"), shellRenderer.render(context, "Class Index", classes(context), "", "classes.html", classesIndexToc(context), timestamp))
        context.projection.pages.findAll { it.kind == PageKind.PACKAGE }.each { PageModel page ->
            PackagePageModel packageModel = context.projection.packagePages.find { it.packageName == page.title }
            List<PackageTypeGroupModel> groups = packageModel?.typeGroups ?: []
            write(new File(root, page.url), shellRenderer.render(context, page.title, packagePage(context, page, packageModel, groups), rootPrefix(page.url), page.url, packageToc(groups), timestamp))
        }
        context.projection.typePages.each { TypePageModel page ->
            PageModel pageModel = context.projection.pages.find { it.targetId?.stableKey() == page.id?.stableKey() }
            String pageUrl = pageModel?.url ?: "reference/${page.id?.qualifiedName}.html"
            write(new File(root, pageUrl), shellRenderer.render(context, page.title, typePage(context, page, pageUrl), rootPrefix(pageUrl), pageUrl, rightToc(page), timestamp))
        }
        assetWriter.write(root, context.assetVersion)
        jsonWriter.write(context.projection.nav, new File(root, "nav-index.json"))
        jsonWriter.write(htmlSearchIndexViewBuilder.build(context.projection.search), new File(root, "search-index.json"))
    }

    private static String packages(RenderContext context) {
        String rows = context.projection.pages.findAll { it.kind == PageKind.PACKAGE }.sort { it.title }.collect { PageModel page ->
            String description = page.summary ?: "Package ${page.title} contains related API types for this SDK reference."
            """            <tr${platformData(page.platforms)}>
              <td><a href="${escapeAttr(page.url)}">${escape(page.title)}</a></td>
              <td>${escape(description)}</td>
            </tr>"""
        }.join("\n")
        return """      <article class="ad-api-index">
        <header class="ad-index-header" id="packages-index">
          <h1>Package Index</h1>
          <p class="ad-index-intro">Browse all API packages in this reference.</p>
        </header>
        <section id="packages-list">
          <h2>Package list</h2>
          <table class="ad-index-table ad-packages-index-table">
            <thead>
              <tr><th>Package</th><th>Description</th></tr>
            </thead>
            <tbody>
${rows}
            </tbody>
          </table>
        </section>
      </article>
"""
    }

    private static String classes(RenderContext context) {
        List<TypePageModel> pages = (context.projection.typePages ?: []).sort { TypePageModel page ->
            simpleTypeName(page).toLowerCase(Locale.ROOT)
        }
        Map<String, List<TypePageModel>> pagesByLetter = pages.groupBy { TypePageModel page -> indexLetter(simpleTypeName(page)) }
        List<String> letters = pagesByLetter.keySet().toList().sort()
        String alphaIndex = letters.collect { String letter ->
            "          <a href=\"#class-${escapeAttr(letter)}\">${escape(letter)}</a>"
        }.join("\n")
        String sections = letters.collect { String letter ->
            String rows = pagesByLetter[letter].collect { TypePageModel page ->
                PageModel pageModel = context.projection.pages.find { it.targetId?.stableKey() == page.id?.stableKey() }
                String url = pageModel?.url ?: "reference/${page.id?.qualifiedName}.html"
                String name = simpleTypeName(page)
                String description = page.summary ?: ""
                """              <tr${platformData(page.platforms)}>
                <td><a href="${escapeAttr(url)}">${escape(name)}</a></td>
                <td>${escape(description)}</td>
              </tr>"""
            }.join("\n")
            """        <section class="ad-index-letter-section" id="class-${escapeAttr(letter)}">
          <h2>${escape(letter)}</h2>
          <table class="ad-index-table ad-classes-index-table">
            <thead>
              <tr><th>Class</th><th>Description</th></tr>
            </thead>
            <tbody>
${rows}
            </tbody>
          </table>
        </section>"""
        }.join("\n")
        return """      <article class="ad-api-index">
        <header class="ad-index-header" id="classes-index">
          <h1>Class Index</h1>
        </header>
        <nav class="ad-alpha-index" aria-label="Class index letters">
${alphaIndex}
        </nav>
${sections}
      </article>
"""
    }

    private String packagePage(RenderContext context, PageModel page, PackagePageModel packagePage, List<PackageTypeGroupModel> groups) {
        String items = groups.collect { PackageTypeGroupModel group ->
            String rows = group.types.collect { TypePageModel type ->
                PageModel model = context.projection.pages.find { it.targetId?.stableKey() == type.id?.stableKey() }
                String url = relativeUrl(page.url, model?.url ?: "reference/${type.id?.qualifiedName}.html")
                String name = type.title
                String description = type.summary ?: ""
                """              <tr${platformData(type.platforms)}>
                <td><a href="${escapeAttr(url)}">${escape(name)}</a></td>
                <td>${escape(description)}</td>
              </tr>"""
            }.join("\n")
            String label = titleCaseLabel(group.label)
            """        <section class="ad-package-type-group" id="${escapeAttr(anchorName(label))}"${platformData(group.platforms)}>
          <h2>${escape(label)}</h2>
          <table class="ad-index-table">
            <thead>
              <tr><th>Class</th><th>Description</th></tr>
            </thead>
            <tbody>
${rows}
            </tbody>
          </table>
        </section>"""
        }.join("\n")
        String overview = page.summary ?: "Package ${page.title} contains related API types for this SDK reference."
        return """      <article class="ad-api-index">
${breadcrumbsForPackage(packagePage, page.url)}
        <header class="ad-api-header" id="overview">
          <h1>${escape(page.title)}${platformBadgesInline(page.platforms)}</h1>
          <p>${escape(overview)}</p>
        </header>
${items}
      </article>
"""
    }

    private String typePage(RenderContext context, TypePageModel page, String pageUrl) {
        StringBuilder out = new StringBuilder()
        out << "      <article class=\"ad-api-article\">\n"
        out << breadcrumbs(page, pageUrl)
        out << "        <header class=\"ad-api-header\" id=\"overview\">\n"
        out << "          <h1>${escape(page.typeHeader?.title ?: page.title)}${platformBadgesInline(page.platforms)}</h1>\n"
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
        if (page.inheritance?.displayName && page.inheritance.qualifiedName != "java.lang.Object") {
            out << "        <p><strong>Extends:</strong> ${typeRefRenderer.render(page.inheritance, pageUrl, context.projection)}</p>\n"
        }
        if (page.interfaces) {
            out << "        <p><strong>Implements:</strong> ${page.interfaces.collect { typeRefRenderer.render(it, pageUrl, context.projection) }.join(', ')}</p>\n"
        }
        // Summary section wrapping all member summary groups
        if (page.memberGroups) {
            out << "        <section class=\"ad-api-summary\" id=\"summary\">\n"
            out << "          <h2 class=\"ad-section-heading\"><span>Summary</span><button class=\"ad-copy-anchor\" type=\"button\" data-anchor=\"summary\" aria-label=\"Copy anchor link\"><img src=\"${rootPrefix(pageUrl)}assets/icon/link.svg\" alt=\"\" aria-hidden=\"true\"></button></h2>\n"
            page.memberGroups.each { MemberGroupModel group ->
                out << memberSummary(group, pageUrl)
            }
            out << "        </section>\n"
        }
        // Detail sections grouped by groupTitle
        Map<String, List<MemberDetailModel>> detailsByGroup = page.memberDetails.groupBy { MemberDetailModel d -> d.groupTitle ?: "Other" }
        page.memberGroups.each { MemberGroupModel group ->
            String detailAnchor = anchorName(group.title) + "-details"
            List<MemberDetailModel> groupDetails = detailsByGroup[group.title] ?: []
            if (groupDetails) {
                out << "        <section class=\"ad-member-details\" id=\"${escapeAttr(detailAnchor)}\">\n"
                out << "          <h2 class=\"ad-section-heading\"><span>${escape(group.title)}</span><button class=\"ad-copy-anchor\" type=\"button\" data-anchor=\"${escapeAttr(detailAnchor)}\" aria-label=\"Copy anchor link\"><img src=\"${rootPrefix(pageUrl)}assets/icon/link.svg\" alt=\"\" aria-hidden=\"true\"></button></h2>\n"
                groupDetails.each { MemberDetailModel detail ->
                    out << memberDetail(context, pageUrl, detail, page.comment)
                }
                out << "        </section>\n"
            }
        }
        out << "      </article>\n"
        return out.toString()
    }

    private String breadcrumbs(TypePageModel page, String pageUrl) {
        if (!page.breadcrumbs) return ""
        String links = page.breadcrumbs.withIndex().collect { BreadcrumbModel crumb, int index ->
            String href = crumb.url ? relativeUrl(pageUrl, crumb.url) : "#"

            if (crumb.isHome) {
                // 首页：只显示图标
                "<a class=\"ad-breadcrumb-home\" href=\"${escapeAttr(href)}\" aria-label=\"Home\"></a>"
            } else {
                // 其他层级：显示文字
                "<a href=\"${escapeAttr(href)}\">${escape(crumb.label)}</a>"
            }
        }.join("<span class=\"ad-breadcrumb-separator\">&gt;</span>")
        return "        <nav class=\"ad-breadcrumbs\" aria-label=\"Breadcrumbs\">${links}</nav>\n"
    }

    private String breadcrumbsForPackage(PackagePageModel page, String pageUrl) {
        if (!page.breadcrumbs) return ""
        String links = page.breadcrumbs.withIndex().collect { BreadcrumbModel crumb, int index ->
            String href = crumb.url ? relativeUrl(pageUrl, crumb.url) : "#"

            if (crumb.isHome) {
                // 首页：只显示图标
                "<a class=\"ad-breadcrumb-home\" href=\"${escapeAttr(href)}\" aria-label=\"Home\"></a>"
            } else {
                // 其他层级：显示文字
                "<a href=\"${escapeAttr(href)}\">${escape(crumb.label)}</a>"
            }
        }.join("<span class=\"ad-breadcrumb-separator\">&gt;</span>")
        return "        <nav class=\"ad-breadcrumbs\" aria-label=\"Breadcrumbs\">${links}</nav>\n"
    }

    private String memberSummary(MemberGroupModel group, String pageUrl) {
        String anchor = anchorName(group.title)
        String rows = group.members.collect { MemberSummaryModel member ->
            String anchorId = member.id?.effectiveAnchorId() ?: member.name
            String rowClass = member.kind == "constant" ? " class=\"ad-summary-constant\"" : ""
            """            <tr${rowClass}${platformData(member.platforms)}>
              <td class="ad-summary-modifier"><code>${escape(member.modifierAndType ?: '')}</code></td>
              <td class="ad-member-main"><a class="ad-member-name" href="#${escapeAttr(anchorId)}">${escape(member.displayName ?: member.name)}</a>${apiStatus(member.status, "ad-api-status ad-api-status-inline")}${member.summary ? "<div class=\"ad-member-description\">${escape(member.summary)}</div>" : ""}</td>
            </tr>"""
        }.join("\n")
        return """        <section class="ad-member-summary" id="${escapeAttr(anchor)}">
          <table class="ad-member-summary-table">
            <thead>
              <tr class="ad-member-summary-heading-row">
                <th colspan="2" class="ad-member-summary-heading">
                  <h3 class="ad-sub-section-heading ad-member-section-title"><span>${escape(group.title)}</span></h3>
                </th>
              </tr>
            </thead>
            <tbody>
${rows}
            </tbody>
          </table>
        </section>
"""
    }

    private String memberDetail(RenderContext context, String pageUrl, MemberDetailModel detail, CommentDoc classComment = null) {
        StringBuilder out = new StringBuilder()
        out << "          <section id=\"${escapeAttr(detail.id?.effectiveAnchorId() ?: detail.name)}\" class=\"ad-member-detail\"${platformData(detail.platforms)}>\n"
        out << "            <h3>${escape(detail.name)} <button class=\"ad-copy-anchor\" type=\"button\" data-anchor=\"${escapeAttr(detail.id?.effectiveAnchorId() ?: detail.name)}\" aria-label=\"Copy anchor link\"><img src=\"${rootPrefix(pageUrl)}assets/icon/link.svg\" alt=\"\" aria-hidden=\"true\"></button>${platformBadges(detail.platforms)}</h3>\n"
        out << apiStatus(detail.status)
        if (detail.declaration) out << "            <pre class=\"ad-member-signature ad-signature-card\"><code>${memberDeclaration(detail, pageUrl)}</code><button class=\"ad-copy-code\" type=\"button\" aria-label=\"Copy signature\"><img src=\"${rootPrefix(pageUrl)}assets/icon/copy.svg\" alt=\"\" aria-hidden=\"true\"></button></pre>\n"
        String detailBody = commentRenderer.renderBody(detail.comment, pageUrl, context.projection, classComment)
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
            "        <a class=\"ad-toc-level-${entry.level ?: 2}\" href=\"#${escapeAttr(entry.anchor)}\">${HtmlPageShellRenderer.insertSemanticBreaks(entry.label)}</a>"
        }.join("\n")
        return """<nav class="ad-devsite-toc" aria-label="On this page">
      <div class="ad-toc-title">On this page</div>
${items}
${tocJumpButton()}
    </nav>"""
    }

    private static String packagesIndexToc() {
        return """<nav class="ad-devsite-toc" aria-label="On this page">
      <div class="ad-toc-title">On this page</div>
      <a class="ad-toc-level-2" href="#packages-index">Package Index</a>
      <a class="ad-toc-level-2" href="#packages-list">Package list</a>
${tocJumpButton()}
    </nav>"""
    }

    private static String classesIndexToc(RenderContext context) {
        List<String> letters = (context.projection.typePages ?: [])
                .collect { TypePageModel page -> indexLetter(simpleTypeName(page)) }
                .unique()
                .sort()
        String items = letters.collect { String letter ->
            "      <a class=\"ad-toc-level-2\" href=\"#class-${escapeAttr(letter)}\">${escape(letter)}</a>"
        }.join("\n")
        return """<nav class="ad-devsite-toc" aria-label="On this page">
      <div class="ad-toc-title">On this page</div>
      <a class="ad-toc-level-2" href="#classes-index">Class Index</a>
${items}
${tocJumpButton()}
    </nav>"""
    }

    private static String packageToc(List<PackageTypeGroupModel> groups) {
        String items = (groups ?: []).collect { PackageTypeGroupModel group ->
            String label = titleCaseLabel(group.label)
            "      <a class=\"ad-toc-level-2\" href=\"#${escapeAttr(anchorName(label))}\">${escape(label)}</a>"
        }.join("\n")
        return """<nav class="ad-devsite-toc" aria-label="On this page">
      <div class="ad-toc-title">On this page</div>
      <a class="ad-toc-level-2" href="#overview">Overview</a>
${items}
${tocJumpButton()}
    </nav>"""
    }

    private static String tocJumpButton() {
        return """      <button class="ad-toc-jump-toggle" type="button" aria-controls="main-content" aria-label="Back to top" data-title="Back to top" hidden>
        <svg class="ad-toc-jump-icon" width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true" focusable="false"><path d="M4.5 9.75 8 6.25l3.5 3.5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>
      </button>"""
    }

    private static String simpleTypeName(TypePageModel page) {
        String name = page?.title ?: page?.id?.qualifiedName ?: ""
        if (name.contains(".")) {
            return name.substring(name.lastIndexOf(".") + 1)
        }
        return name
    }

    private static String indexLetter(String name) {
        if (!name) return "#"
        String first = name.substring(0, 1).toUpperCase(Locale.ROOT)
        return first ==~ /[A-Z]/ ? first : "#"
    }

    private static String titleCaseLabel(String label) {
        String text = (label ?: "").trim()
        if (!text) return ""
        String lower = text.toLowerCase(Locale.ROOT)
        return lower.substring(0, 1).toUpperCase(Locale.ROOT) + lower.substring(1)
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
        // if (status.deprecated) chips.add(statusChip("deprecated", "Deprecated"))
        // if (status.deprecatedMessage) chips.add(statusChip("deprecated", "Deprecated: ${status.deprecatedMessage}"))
        if (status.removed) chips.add(statusChip("removed", "Removed"))
        if (status.removedMessage) chips.add(statusChip("removed", "Removed: ${status.removedMessage}"))
        if (status.pending) chips.add(statusChip("pending", "Pending"))
        // if (status.since) chips.add(statusChip("since", "Since ${status.since}"))
        // if (status.apiSince != null) chips.add(statusChip("since", "API ${status.apiSince}"))
        // if (status.sdkExtensionSince) chips.add(statusChip("since", status.sdkExtensionSince))
        // if (status.deprecatedSince) chips.add(statusChip("deprecated", "Deprecated since ${status.deprecatedSince}"))
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

    private static final Map<String, String> PLATFORM_LABELS = [
            "DiLink150VCP"    : "舱驾B",
            "DiLink300VCP"    : "舱驾B+",
            "DiLinkF_300VCP"  : "舱驾B+F",
            "DiLink300"       : "D300",
            "DiLink300F"      : "D300F"
    ]

    private static String platformBadgeTag(String platform) {
        String label = PLATFORM_LABELS[platform]
        if (label) {
            return "<span class=\"ad-platform-badge\" data-tooltip=\"${escapeAttr(platform)}\">${escape(label)}</span>"
        }
        return "<span class=\"ad-platform-badge\">${escape(platform)}</span>"
    }

    private static String platformBadges(Collection<String> platforms) {
        List<String> values = (platforms ?: []).findAll { it }.collect { it.toString() }
        if (!values) return ""
        String badges = values.collect { String platform -> platformBadgeTag(platform) }.join("")
        return "<span class=\"ad-platform-badges-inline\">${badges}</span>"
    }

    private static String platformBadgesInline(Collection<String> platforms) {
        List<String> values = (platforms ?: []).findAll { it }.collect { it.toString() }
        if (!values) return ""
        String badges = values.collect { String platform -> platformBadgeTag(platform) }.join("")
        return "<span class=\"ad-platform-badges-inline\">${badges}</span>"
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

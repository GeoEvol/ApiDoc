package com.byd.apidoc.render.html

import com.byd.apidoc.projection.NavNode
import com.byd.apidoc.render.RenderContext

class HtmlPageShellRenderer {

    String render(RenderContext context, String title, String body, String prefix, String currentUrl = "", String tocHtml = null, String timestamp = null) {
        String v = context.assetVersion ?: ""
        String cssFile = v ? "apidoc-${v}.css" : "apidoc.css"
        String jsFile  = v ? "apidoc-${v}.js"  : "apidoc.js"
        String searchFile = v ? "search-${v}.js" : "search.js"
        String assetBase = assetPrefix(context, prefix)
        String searchIndex = searchIndexUrl(context, prefix)
        return """<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${escape(title)} - ${escape(context.projectName ?: 'API Reference')}</title>
  <link rel="stylesheet" href="${assetBase}assets/${cssFile}">
</head>
<body>
  <!--
  <header class="ad-devsite-topbar" role="banner">
    <button class="ad-devsite-nav-toggle" type="button" aria-controls="ad-book-nav" aria-expanded="false"><img src="${prefix}assets/icon/menu.svg" alt="" aria-hidden="true">Menu</button>
    <a class="ad-brand" href="${prefix}index.html">${escape(context.projectName ?: 'API Reference')}</a>
  </header>
  -->
  <div class="ad-devsite-shell">
    <nav class="ad-devsite-book-nav" id="ad-book-nav" aria-label="API navigation">
      ${searchBox()}
      <div class="ad-book-nav-scroll">
${bookNav(context, prefix, currentUrl)}
      </div>
    </nav>
    <button class="ad-book-nav-toggle" type="button" aria-controls="ad-book-nav" aria-expanded="true" aria-label="Hide navigation" data-title="Hide navigation">
      <svg class="ad-book-nav-toggle-icon" width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true" focusable="false"><path d="M6 11.5 9.5 8 6 4.5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>
    </button>
    <main class="ad-devsite-content" id="main-content">
      <div class="ad-devsite-content-inner">
${body}
      </div>
      <footer class="ad-page-footer">Powered by DiCore Team.${timestamp ? " | Last updated ${timestamp}." : ""}</footer>
    </main>
    ${tocHtml ?: '<nav class="ad-devsite-toc" aria-label="On this page"></nav>'}
    ${contactWidget()}
  </div>
  <script src="${assetBase}assets/${searchFile}" data-root-prefix="${escapeAttr(prefix)}" data-search-index="${escapeAttr(searchIndex)}"></script>
  <script src="${assetBase}assets/${jsFile}"></script>
</body>
</html>
"""
    }

    private static String assetPrefix(RenderContext context, String prefix) {
        if (context?.stableAssetLinks && context?.siteBasePath?.trim()) {
            return normalizeBasePath(context.siteBasePath)
        }
        return prefix ?: ""
    }

    private static String searchIndexUrl(RenderContext context, String prefix) {
        if (context?.stableAssetLinks && context?.siteBasePath?.trim()) {
            return "${normalizeBasePath(context.siteBasePath)}search-index.json"
        }
        return "${prefix ?: ""}search-index.json"
    }

    private static String normalizeBasePath(String value) {
        String base = (value ?: "").trim()
        if (!base) return ""
        if (!base.startsWith("/")) base = "/" + base
        if (!base.endsWith("/")) base += "/"
        return base
    }

    private static String bookNav(RenderContext context, String prefix, String currentUrl) {
        StringBuilder out = new StringBuilder()
        boolean packageIndexCurrent = currentUrl == 'index.html' || currentUrl == 'packages.html'
        out << "      <details class=\"ad-packages-root\" id=\"ad-packages-root\" open>\n"
        out << "        <summary class=\"ad-packages-root-summary ad-nav-item\" data-filter-text=\"Packages\"><span class=\"ad-package-disclosure\"><img src=\"${prefix}assets/icon/chevron.svg\" alt=\"\" aria-hidden=\"true\"></span><span class=\"ad-packages-root-label\" data-nav-label=\"Packages\">Packages</span></summary>\n"
        out << "        <div class=\"ad-packages-root-content\">\n"
        out << platformSelector(context)
        out << "\n"
        boolean classIndexCurrent = currentUrl == 'classes.html'
        out << "          <a class=\"ad-book-link ad-book-index-link${currentClass(classIndexCurrent)}\"${ariaCurrent(classIndexCurrent)} data-filter-text=\"Class Index\" href=\"${prefix}classes.html\"><span data-nav-label=\"Class Index\">Class Index</span></a>\n"
        out << "          <a class=\"ad-book-link ad-book-index-link${currentClass(packageIndexCurrent)}\"${ariaCurrent(packageIndexCurrent)} data-filter-text=\"Package Index\" href=\"${prefix}packages.html\"><span data-nav-label=\"Package Index\">Package Index</span></a>\n"
        (context.projection?.nav ?: []).each { NavNode node ->
            boolean packageOpen = isOnDescendantUrl(node, currentUrl)
            out << "          <details class=\"ad-book-section ad-package\"${platformData(node.platforms)}${packageOpen ? ' open' : ''}>\n"
            boolean packageCurrent = currentUrl == node.url
            out << "            <summary class=\"ad-book-package ad-nav-item${currentClass(packageCurrent)}\"${ariaCurrent(packageCurrent)}${platformData(node.platforms)} data-filter-text=\"${escapeAttr(node.label)}\"><span class=\"ad-package-disclosure\"><img src=\"${prefix}assets/icon/chevron.svg\" alt=\"\" aria-hidden=\"true\"></span><span class=\"ad-package-name\" data-nav-label=\"${escapeAttr(node.label)}\">${insertSemanticBreaks(node.label)}</span></summary>\n"
            node.children.each { NavNode group ->
                if (group.kind == com.byd.apidoc.projection.NavNodeKind.OVERVIEW) {
                    boolean current = currentUrl == group.url
                    out << "            <a class=\"ad-book-overview ad-nav-item${currentClass(current)}\"${ariaCurrent(current)}${platformData(group.platforms)} data-filter-text=\"${escapeAttr("${node.label} Overview")}\" href=\"${prefix}${escapeAttr(group.url ?: '')}\"><span data-nav-label=\"${escapeAttr(group.label)}\">${escape(group.label)}</span></a>\n"
                } else {
                    boolean groupOpen = (group.children ?: []).any { NavNode child -> currentUrl == child.url }
                    String groupLabel = titleCaseGroupLabel(group.label ?: group.group?.toString())
                    out << "            <details class=\"ad-package-group\"${platformData(group.platforms)}${groupOpen ? ' open' : ''}>\n"
                    out << "              <summary class=\"ad-book-group ad-nav-item\"${platformData(group.platforms)} data-filter-text=\"${escapeAttr("${node.label} ${groupLabel}")}\"><span class=\"ad-package-disclosure\"><img src=\"${prefix}assets/icon/chevron.svg\" alt=\"\" aria-hidden=\"true\"></span><span class=\"ad-group-label\" data-nav-label=\"${escapeAttr(groupLabel)}\">${escape(groupLabel)}</span></summary>\n"
                    group.children.each { NavNode child ->
                        boolean current = currentUrl == child.url
                        out << "              <a class=\"ad-book-type ad-nav-item${currentClass(current)}\"${ariaCurrent(current)}${platformData(child.platforms)} data-filter-text=\"${escapeAttr("${node.label} ${group.label} ${child.label}")}\" href=\"${prefix}${escapeAttr(child.url ?: '')}\"><span data-nav-label=\"${escapeAttr(child.label)}\">${insertSemanticBreaks(child.label)}</span></a>\n"
                    }
                    out << "            </details>\n"
                }
            }
            out << "          </details>\n"
        }
        out << "        </div>\n"
        out << "      </details>\n"
        return out.toString()
    }

    private static String currentClass(boolean current) {
        return current ? " is-current" : ""
    }

    private static String ariaCurrent(boolean current) {
        return current ? ' aria-current="page"' : ""
    }

    private static String searchBox() {
        return """<div class="ad-search-wrap ad-nav-search-wrap">
          <input class="ad-search" id="ad-search" type="search" placeholder="Search API" autocomplete="off">
          <div class="ad-search-results" id="ad-search-results"></div>
        </div>"""
    }

    private static String contactWidget() {
        return """    <div class="ad-contact-widget">
      <button class="ad-contact-toggle" type="button" aria-controls="ad-contact-popover" aria-expanded="false" aria-label="Contact us" data-title="Contact us">
        <svg class="ad-contact-icon" width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden="true" focusable="false">
          <path d="M5.25 14.25 3 16.5V5.75A2.25 2.25 0 0 1 5.25 3.5h9.5A2.25 2.25 0 0 1 17 5.75v6.25a2.25 2.25 0 0 1-2.25 2.25h-9.5Z" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M6.75 7.25h6.5M6.75 10h4.5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
      <section class="ad-contact-popover" id="ad-contact-popover" role="dialog" aria-modal="false" aria-labelledby="ad-contact-title" tabindex="-1" hidden>
        <div class="ad-contact-header">
          <h2 id="ad-contact-title">Contact us</h2>
          <button class="ad-contact-close" type="button" aria-label="Close contact panel">&times;</button>
        </div>
        <p class="ad-contact-lead">Need help with this API reference?</p>
        <p>Contact the SDK documentation owner or DiCore Team.</p>
        <div class="ad-contact-note">
          <strong>When contacting us, please include:</strong>
          <ul>
            <li>API name or page title</li>
            <li>Current page URL</li>
            <li>Platform and SDK version</li>
            <li>Problem description or feedback</li>
          </ul>
        </div>
      </section>
    </div>"""
    }

    private static boolean isOnDescendantUrl(NavNode pkg, String currentUrl) {
        if (currentUrl == pkg.url) return true
        for (NavNode child : (pkg.children ?: [])) {
            if (currentUrl == child.url) return true
            for (NavNode grand : (child.children ?: [])) {
                if (currentUrl == grand.url) return true
            }
        }
        return false
    }

    private static final Map<String, String> PLATFORM_LABELS = [
            "DiLink150VCP"    : "舱驾B",
            "DiLink300VCP"    : "舱驾B+",
            "DiLinkF_300VCP"  : "舱驾B+F",
            "DiLink300"       : "D300",
            "DiLink300F"      : "D300F"
    ]

    private static String platformSelector(RenderContext context) {
        List<String> platforms = context.projection?.platformFilter?.platforms ?: []
        String options = (["all"] + platforms).collect { String platform ->
            String label = platform == "all" ? "All Platforms" : (PLATFORM_LABELS[platform] ?: platform)
            "          <option value=\"${escapeAttr(platform)}\">${escape(label)}</option>"
        }.join("\n")
        return """          <div class="ad-platform-selector">
        <label class="ad-platform-selector-label" for="ad-platform-select">Platform</label>
        <select id="ad-platform-select" class="ad-platform-selector-select" aria-label="Filter API by supported platform">
${options}
        </select>
      </div>"""
    }

    private static String titleCaseGroupLabel(String label) {
        String text = (label ?: "").trim()
        if (!text) return ""
        String lower = text.toLowerCase(Locale.ROOT)
        return lower.substring(0, 1).toUpperCase(Locale.ROOT) + lower.substring(1)
    }

    private static String platformData(Collection<String> platforms) {
        List<String> values = (platforms ?: []).findAll { it }.collect { it.toString() }
        return values ? " data-platforms=\"${escapeAttr(values.join(' '))}\"" : ""
    }

    static String insertSemanticBreaks(String text) {
        if (!text || text.length() < 8) return escape(text)
        String escaped = escape(text)
        String result

        // 包名（含点号）：点号保留在前一行末尾 → 在点号后插入 <wbr>
        if (text.contains('.')) {
            return escaped.replace('.', '.<wbr>')
        } else if (text.contains('_')) {
            // 下划线名称：下划线保留在前一行末尾 → 在下划线后插入 <wbr>
            return escaped.replace('_', '_<wbr>')
        } else if (text.contains('-')) {
            // 连字符名称：连字符保留在前一行末尾 → 在连字符后插入 <wbr>
            return escaped.replace('-', '-<wbr>')
        } else {
            // 驼峰类名：按大写字母拆分 → 在大写字母前插入 <wbr>（首字符除外）
            result = escaped.replaceAll(/(.)([A-Z])/, '$1<wbr>$2')
        }

        return result
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

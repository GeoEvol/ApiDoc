package com.byd.apidoc.render.html

import com.byd.apidoc.projection.NavNode
import com.byd.apidoc.render.RenderContext

class HtmlPageShellRenderer {

    String render(RenderContext context, String title, String body, String prefix, String currentUrl = "", String tocHtml = null) {
        return """<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${escape(title)} - ${escape(context.projectName ?: 'API Reference')}</title>
  <link rel="stylesheet" href="${prefix}assets/apidoc-devsite.css">
  <link rel="stylesheet" href="${prefix}assets/apidoc.css">
</head>
<body>
  <header class="ad-devsite-topbar" role="banner">
    <button class="ad-devsite-nav-toggle" type="button" aria-controls="ad-book-nav" aria-expanded="false"><img src="${prefix}assets/icon/menu.svg" alt="" aria-hidden="true">Menu</button>
    <a class="ad-brand" href="${prefix}index.html">${escape(context.projectName ?: 'API Reference')}</a>
  </header>
  <div class="ad-devsite-shell">
    <nav class="ad-devsite-book-nav" id="ad-book-nav" aria-label="API navigation">
      <div class="ad-reference-title">API REFERENCE</div>
      <div class="ad-book-nav-scroll">
        ${searchBox()}
${bookNav(context, prefix, currentUrl)}
      </div>
    </nav>
    <button class="ad-book-nav-toggle" type="button" aria-controls="ad-book-nav" aria-expanded="true" aria-label="Hide navigation" data-title="Hide navigation">
      <img class="ad-book-nav-toggle-icon" src="${prefix}assets/icon/chevron.svg" alt="" aria-hidden="true">
    </button>
    <main class="ad-devsite-content" id="main-content">
${body}
    </main>
    ${tocHtml ?: '<nav class="ad-devsite-toc" aria-label="On this page"></nav>'}
  </div>
  <script src="${prefix}assets/search.js" data-root-prefix="${escapeAttr(prefix)}"></script>
  <script src="${prefix}assets/apidoc-devsite.js"></script>
  <script src="${prefix}assets/apidoc.js"></script>
</body>
</html>
"""
    }

    private static String bookNav(RenderContext context, String prefix, String currentUrl) {
        StringBuilder out = new StringBuilder()
        boolean packageIndexCurrent = currentUrl == 'index.html' || currentUrl == 'packages.html'
        out << "      <details class=\"ad-packages-root\" id=\"ad-packages-root\">\n"
        out << "        <summary class=\"ad-packages-root-summary ad-nav-item\" data-filter-text=\"Packages\"><span class=\"ad-package-disclosure\"><img src=\"${prefix}assets/icon/chevron.svg\" alt=\"\" aria-hidden=\"true\"></span><span class=\"ad-packages-root-label\" data-nav-label=\"Packages\">Packages</span></summary>\n"
        out << "        <div class=\"ad-packages-root-content\">\n"
        out << platformSelector(context)
        out << "\n"
        out << "          <a class=\"ad-book-link ad-book-index-link${currentUrl == 'classes.html' ? ' is-current' : ''}\" data-filter-text=\"Class Index\" href=\"${prefix}classes.html\"><span data-nav-label=\"Class Index\">Class Index</span></a>\n"
        out << "          <a class=\"ad-book-link ad-book-index-link${packageIndexCurrent ? ' is-current' : ''}\" data-filter-text=\"Package Index\" href=\"${prefix}packages.html\"><span data-nav-label=\"Package Index\">Package Index</span></a>\n"
        (context.projection?.nav ?: []).each { NavNode node ->
            boolean packageOpen = isOnDescendantUrl(node, currentUrl)
            out << "          <details class=\"ad-book-section ad-package\"${platformData(node.platforms)}${packageOpen ? ' open' : ''}>\n"
            out << "            <summary class=\"ad-book-package ad-nav-item${currentUrl == node.url ? ' is-current' : ''}\"${platformData(node.platforms)} data-filter-text=\"${escapeAttr(node.label)}\"><span class=\"ad-package-disclosure\"><img src=\"${prefix}assets/icon/chevron.svg\" alt=\"\" aria-hidden=\"true\"></span><span class=\"ad-package-name\" data-nav-label=\"${escapeAttr(node.label)}\">${escape(node.label)}</span></summary>\n"
            node.children.each { NavNode group ->
                if (group.kind == com.byd.apidoc.projection.NavNodeKind.OVERVIEW) {
                    String current = currentUrl == group.url ? " is-current" : ""
                    out << "            <a class=\"ad-book-overview ad-nav-item${current}\"${platformData(group.platforms)} data-filter-text=\"${escapeAttr("${node.label} Overview")}\" href=\"${prefix}${escapeAttr(group.url ?: '')}\"><span data-nav-label=\"${escapeAttr(group.label)}\">${escape(group.label)}</span></a>\n"
                } else {
                    boolean groupOpen = (group.children ?: []).any { NavNode child -> currentUrl == child.url }
                    String groupKind = kindKey(group.group ?: group.label)
                    String icon = iconForGroup(group.group ?: group.label)
                    String groupLabel = titleCaseGroupLabel(group.label ?: group.group?.toString())
                    out << "            <details class=\"ad-package-group\" data-group-kind=\"${escapeAttr(groupKind)}\"${platformData(group.platforms)}${groupOpen ? ' open' : ''}>\n"
                    out << "              <summary class=\"ad-book-group ad-nav-item\"${platformData(group.platforms)} data-filter-text=\"${escapeAttr("${node.label} ${groupLabel}")}\"><span class=\"ad-package-disclosure\"><img src=\"${prefix}assets/icon/chevron.svg\" alt=\"\" aria-hidden=\"true\"></span><span class=\"ad-group-label\" data-nav-label=\"${escapeAttr(groupLabel)}\">${escape(groupLabel)}</span></summary>\n"
                    group.children.each { NavNode child ->
                        String current = currentUrl == child.url ? " is-current" : ""
                        out << "              <a class=\"ad-book-type ad-nav-item${current}\"${platformData(child.platforms)} data-filter-text=\"${escapeAttr("${node.label} ${group.label} ${child.label}")}\" href=\"${prefix}${escapeAttr(child.url ?: '')}\"><img class=\"ad-kind-icon\" src=\"${prefix}assets/icon/${icon}.svg\" alt=\"\" aria-hidden=\"true\"><span data-nav-label=\"${escapeAttr(child.label)}\">${escape(child.label)}</span></a>\n"
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

    private static String searchBox() {
        return """<div class="ad-search-wrap ad-nav-search-wrap">
          <input class="ad-search" id="ad-search" type="search" placeholder="Search API" autocomplete="off">
          <div class="ad-search-results" id="ad-search-results"></div>
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

    private static String kindKey(String label) {
        String key = (label ?: "").toLowerCase(Locale.ROOT).trim()
        if (key.contains("interface")) return "interfaces"
        if (key.contains("annotation")) return "annotations"
        if (key.contains("enum")) return "enums"
        if (key.contains("record")) return "records"
        if (key.contains("exception")) return "exceptions"
        if (key.contains("error")) return "errors"
        return "classes"
    }

    private static String platformSelector(RenderContext context) {
        List<String> platforms = context.projection?.platformFilter?.platforms ?: []
        String options = (["all"] + platforms).collect { String platform ->
            String label = platform == "all" ? "All Platforms" : platform
            "          <option value=\"${escapeAttr(platform)}\">${escape(label)}</option>"
        }.join("\n")
        return """          <div class="ad-platform-selector">
        <label class="ad-platform-selector-label" for="ad-platform-select">Platform</label>
        <select id="ad-platform-select" class="ad-platform-selector-select" aria-label="Filter API by supported platform">
${options}
        </select>
      </div>"""
    }

    private static String iconForGroup(String group) {
        String key = (group ?: "").toLowerCase(Locale.ROOT)
        if (key.contains("interface")) return "interface"
        if (key.contains("annotation")) return "annotation"
        if (key.contains("enum")) return "enum"
        if (key.contains("record")) return "record"
        if (key.contains("exception")) return "exception"
        if (key.contains("error")) return "exception"
        return "class"
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

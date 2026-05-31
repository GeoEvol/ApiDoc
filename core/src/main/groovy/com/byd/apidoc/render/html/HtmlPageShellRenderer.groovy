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
    <div class="ad-search-wrap">
      <input class="ad-search" id="ad-search" type="search" placeholder="Search API" autocomplete="off">
      <div class="ad-search-results" id="ad-search-results"></div>
    </div>
  </header>
  <div class="ad-devsite-shell">
    <nav class="ad-devsite-book-nav" id="ad-book-nav" aria-label="API navigation">
      ${platformSelector(context)}
      <div class="ad-book-filter">
        <input class="ad-nav-filter" id="ad-nav-filter" type="search" placeholder="Filter packages and types" autocomplete="off" aria-label="Filter packages and types">
      </div>
${bookNav(context, prefix, currentUrl)}
      <button class="ad-book-nav-toggle" type="button" aria-label="Toggle navigation" aria-controls="ad-book-nav">‹</button>
    </nav>
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
        out << "      <a class=\"ad-book-link${currentUrl == 'packages.html' ? ' is-current' : ''}\" href=\"${prefix}packages.html\"><img class=\"ad-nav-icon\" src=\"${prefix}assets/icon/package.svg\" alt=\"\" aria-hidden=\"true\">Packages</a>\n"
        out << "      <a class=\"ad-book-link${currentUrl == 'classes.html' ? ' is-current' : ''}\" href=\"${prefix}classes.html\"><img class=\"ad-nav-icon\" src=\"${prefix}assets/icon/type.svg\" alt=\"\" aria-hidden=\"true\">Classes</a>\n"
        (context.projection?.nav ?: []).each { NavNode node ->
            int typeCount = (node.children ?: []).sum { NavNode group -> group.kind == com.byd.apidoc.projection.NavNodeKind.GROUP ? (group.children?.size() ?: 0) : 0 } as int
            boolean open = currentUrl == node.url || (node.children ?: []).any { NavNode group -> currentUrl == group.url || (group.children ?: []).any { NavNode child -> currentUrl == child.url } }
            out << "      <details class=\"ad-book-section ad-package\"${platformData(node.platforms)}${open ? ' open' : ''}>\n"
            out << "        <summary class=\"ad-book-package ad-nav-item${currentUrl == node.url ? ' is-current' : ''}\"${platformData(node.platforms)}><span class=\"ad-package-disclosure\"><img src=\"${prefix}assets/icon/chevron.svg\" alt=\"\" aria-hidden=\"true\"></span><span class=\"ad-package-name\">${escape(node.label)}</span><a class=\"ad-package-link\" href=\"${prefix}${escapeAttr(node.url ?: '')}\" aria-label=\"Open package ${escapeAttr(node.label)}\"><img src=\"${prefix}assets/icon/package.svg\" alt=\"\" aria-hidden=\"true\"></a><span class=\"ad-package-count\">${typeCount}</span></summary>\n"
            node.children.each { NavNode group ->
                if (group.kind == com.byd.apidoc.projection.NavNodeKind.OVERVIEW) {
                    String current = currentUrl == group.url ? " is-current" : ""
                    out << "        <a class=\"ad-book-overview ad-nav-item${current}\"${platformData(group.platforms)} data-filter-text=\"${escapeAttr("${node.label} Overview")}\" href=\"${prefix}${escapeAttr(group.url ?: '')}\"><img class=\"ad-kind-icon\" src=\"${prefix}assets/icon/package.svg\" alt=\"\" aria-hidden=\"true\"><span>${escape(group.label)}</span></a>\n"
                } else {
                    out << "        <div class=\"ad-book-group ad-package-group\"${platformData(group.platforms)}>${escape(group.label)}</div>\n"
                    group.children.each { NavNode child ->
                        String current = currentUrl == child.url ? " is-current" : ""
                        String icon = iconForGroup(group.group ?: group.label)
                        out << "        <a class=\"ad-book-type ad-nav-item${current}\"${platformData(child.platforms)} data-filter-text=\"${escapeAttr("${node.label} ${group.label} ${child.label}")}\" href=\"${prefix}${escapeAttr(child.url ?: '')}\"><img class=\"ad-kind-icon\" src=\"${prefix}assets/icon/${icon}.svg\" alt=\"\" aria-hidden=\"true\"><span>${escape(child.label)}</span></a>\n"
                    }
                }
            }
            out << "      </details>\n"
        }
        return out.toString()
    }

    private static String platformSelector(RenderContext context) {
        List<String> platforms = context.projection?.platformFilter?.platforms ?: []
        String options = (["all"] + platforms).collect { String platform ->
            String label = platform == "all" ? "All platforms" : platform
            "          <option value=\"${escapeAttr(platform)}\">${escape(label)}</option>"
        }.join("\n")
        return """<div class="ad-platform-selector">
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

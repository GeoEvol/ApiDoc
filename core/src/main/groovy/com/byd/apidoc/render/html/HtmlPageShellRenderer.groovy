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
      <div class="ad-book-filter">
        <input class="ad-nav-filter" id="ad-nav-filter" type="search" placeholder="Filter packages and types" autocomplete="off" aria-label="Filter packages and types">
      </div>
${bookNav(context, prefix, currentUrl)}
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
            int typeCount = (node.children ?: []).sum { NavNode group -> group.children?.size() ?: 0 } as int
            boolean open = currentUrl == node.url || (node.children ?: []).any { NavNode group -> (group.children ?: []).any { NavNode child -> currentUrl == child.url } }
            out << "      <details class=\"ad-book-section ad-package\"${open ? ' open' : ''}>\n"
            out << "        <summary class=\"ad-book-package${currentUrl == node.url ? ' is-current' : ''}\"><span class=\"ad-package-disclosure\"><img src=\"${prefix}assets/icon/chevron.svg\" alt=\"\" aria-hidden=\"true\"></span><span class=\"ad-package-name\">${escape(node.label)}</span><a class=\"ad-package-link\" href=\"${prefix}${escapeAttr(node.url ?: '')}\" aria-label=\"Open package ${escapeAttr(node.label)}\"><img src=\"${prefix}assets/icon/package.svg\" alt=\"\" aria-hidden=\"true\"></a><span class=\"ad-package-count\">${typeCount}</span></summary>\n"
            node.children.each { NavNode group ->
                out << "        <div class=\"ad-book-group\">${escape(group.label)}</div>\n"
                group.children.each { NavNode child ->
                    String current = currentUrl == child.url ? " is-current" : ""
                    String icon = iconForGroup(group.group ?: group.label)
                    out << "        <a class=\"ad-book-type${current}\" data-filter-text=\"${escapeAttr("${node.label} ${group.label} ${child.label}")}\" href=\"${prefix}${escapeAttr(child.url ?: '')}\"><img class=\"ad-kind-icon\" src=\"${prefix}assets/icon/${icon}.svg\" alt=\"\" aria-hidden=\"true\"><span>${escape(child.label)}</span></a>\n"
                }
            }
            out << "      </details>\n"
        }
        return out.toString()
    }

    private static String iconForGroup(String group) {
        String key = (group ?: "").toLowerCase(Locale.ROOT)
        if (key.contains("interface")) return "interface"
        if (key.contains("annotation")) return "annotation"
        if (key.contains("enum")) return "enum"
        if (key.contains("record")) return "record"
        return "class"
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

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
    <button class="ad-devsite-nav-toggle" type="button" aria-controls="ad-book-nav" aria-expanded="false">Menu</button>
    <a class="ad-brand" href="${prefix}index.html">${escape(context.projectName ?: 'API Reference')}</a>
    <div class="ad-search-wrap">
      <input class="ad-search" id="ad-search" type="search" placeholder="Search API" autocomplete="off">
      <div class="ad-search-results" id="ad-search-results"></div>
    </div>
  </header>
  <div class="ad-devsite-shell">
    <nav class="ad-devsite-book-nav" id="ad-book-nav" aria-label="API navigation">
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
        out << "      <a class=\"ad-book-link${currentUrl == 'packages.html' ? ' is-current' : ''}\" href=\"${prefix}packages.html\">Packages</a>\n"
        out << "      <a class=\"ad-book-link${currentUrl == 'classes.html' ? ' is-current' : ''}\" href=\"${prefix}classes.html\">Classes</a>\n"
        (context.projection?.nav ?: []).each { NavNode node ->
            out << "      <section class=\"ad-book-section\">\n"
            out << "        <a class=\"ad-book-package${currentUrl == node.url ? ' is-current' : ''}\" href=\"${prefix}${escapeAttr(node.url ?: '')}\">${escape(node.label)}</a>\n"
            node.children.each { NavNode group ->
                out << "        <div class=\"ad-book-group\">${escape(group.label)}</div>\n"
                group.children.each { NavNode child ->
                    String current = currentUrl == child.url ? " is-current" : ""
                    out << "        <a class=\"ad-book-type${current}\" href=\"${prefix}${escapeAttr(child.url ?: '')}\">${escape(child.label)}</a>\n"
                }
            }
            out << "      </section>\n"
        }
        return out.toString()
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

package com.byd.apidoc.render

import com.byd.apidoc.render.html.HtmlSiteRenderer

class BuiltinHtmlRenderer {
    static final String OUTPUT_DIR = HtmlSiteRenderer.OUTPUT_DIR

    private final HtmlSiteRenderer siteRenderer = new HtmlSiteRenderer()

    void render(RenderContext context) {
        siteRenderer.render(context)
    }
}

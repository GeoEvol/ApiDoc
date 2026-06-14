package com.byd.apidoc.render

import com.byd.apidoc.render.html.HtmlSiteRenderer

class BuiltinHtmlRenderer {
    static final String OUTPUT_DIR = HtmlSiteRenderer.OUTPUT_DIR
    static final String VERSION_RESOURCE = "apidoc-v2/assets/apidoc-version.properties"

    private final HtmlSiteRenderer siteRenderer = new HtmlSiteRenderer()

    void render(RenderContext context) {
        context.assetVersion = readAssetVersion()
        siteRenderer.render(context)
    }

    private static String readAssetVersion() {
        InputStream input = BuiltinHtmlRenderer.class.classLoader.getResourceAsStream(VERSION_RESOURCE)
        if (input == null) return ""
        try {
            Properties props = new Properties()
            props.load(input)
            return props.getProperty("version", "")
        } finally {
            input.close()
        }
    }
}
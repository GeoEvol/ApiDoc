package com.byd.apidoc.render.html

class HtmlAssetWriter {
    static final String ASSET_RESOURCE_ROOT = "apidoc-v2/assets"

    void write(File root) {
        File assets = new File(root, "assets")
        assets.mkdirs()
        copyResource("apidoc-devsite.css", new File(assets, "apidoc-devsite.css"))
        copyResource("apidoc-devsite.js", new File(assets, "apidoc-devsite.js"))

        copyResource("apidoc-devsite.css", new File(assets, "apidoc.css"))
        copyResource("apidoc-devsite.js", new File(assets, "apidoc.js"))
        copyResource("apidoc-search.js", new File(assets, "search.js"))
    }

    private void copyResource(String name, File target) {
        String resourcePath = "${ASSET_RESOURCE_ROOT}/${name}"
        InputStream input = getClass().classLoader.getResourceAsStream(resourcePath)
        if (input == null) {
            throw new IllegalStateException("Missing HTML asset resource: ${resourcePath}")
        }
        target.parentFile.mkdirs()
        try {
            target.withOutputStream { OutputStream output ->
                output << input
            }
        } finally {
            input.close()
        }
    }
}

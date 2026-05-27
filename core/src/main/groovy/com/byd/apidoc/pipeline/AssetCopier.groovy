package com.byd.apidoc.pipeline

class AssetCopier {
    private static final List<String> THEME_RESOURCES = [
            "theme/css/docs.css",
            "theme/css/javadoc-layout.css",
            "theme/css/javadoc-content.css",
            "theme/css/javadoc-search.css",
            "theme/css/overrides.css",
            "theme/js/app.js",
            "theme/js/javadoc-navigation-adapter.js",
            "theme/js/javadoc-search-adapter.js",
            "theme/js/theme-switcher.js",
            "theme/js/search.js",
            "theme/vendor/github-markdown-css/github-markdown.css",
            "theme/vendor/prism/prism.css",
            "theme/vendor/prism/prism.js",
            "theme/vendor/lucide/icons/search.svg",
            "theme/vendor/lucide/icons/copy.svg",
            "theme/vendor/lucide/icons/chevron-down.svg",
            "theme/vendor/lucide/icons/moon.svg",
            "theme/vendor/lucide/icons/sun.svg"
    ]

    void copyThemeAssets(File outputDir) {
        THEME_RESOURCES.each { String resourcePath ->
            String relativePath = resourcePath.startsWith("theme/") ? resourcePath.substring("theme/".length()) : resourcePath
            copyKnownResource(resourcePath, new File(outputDir, relativePath))
        }
        copyResourceDirectory("theme", outputDir)
    }

    private void copyKnownResource(String resourcePath, File target) {
        InputStream input = getClass().classLoader.getResourceAsStream(resourcePath)
        if (input == null) {
            return
        }
        target.parentFile.mkdirs()
        target.withOutputStream { output ->
            output << input
        }
        input.close()
    }

    private void copyResourceDirectory(String resourceRoot, File outputDir) {
        URL root = getClass().classLoader.getResource(resourceRoot)
        if (root == null || root.protocol != "file") {
            return
        }
        File sourceRoot = new File(root.toURI())
        sourceRoot.eachFileRecurse { File source ->
            if (!source.isFile()) return
            String relative = sourceRoot.toPath().relativize(source.toPath()).toString()
            File target = new File(outputDir, relative)
            target.parentFile.mkdirs()
            target.bytes = source.bytes
        }
    }
}

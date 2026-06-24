package com.byd.apidoc.render.html

import java.nio.file.Files
import java.util.jar.JarEntry
import java.util.jar.JarFile

class HtmlAssetWriter {
    static final String ASSET_RESOURCE_ROOT = "apidoc-v2/assets"

    private static final List<String> CSS_PARTIALS = [
            "css/00-tokens.css",
            "css/01-base.css",
            "css/02-shell-and-search.css",
            "css/03-left-nav.css",
            "css/04-platform-filter.css",
            "css/05-right-toc.css",
            "css/06-index-and-api-page.css",
            "css/07-member-summary.css",
            "css/08-member-detail.css",
            "css/09-responsive.css",
            "css/10-print.css",
            "css/99-late-overrides.css"
    ]

    private static final List<String> JS_PARTIALS = [
            "js/00-iife-open-and-state.js",
            "js/01-nav-toggle-and-collapse.js",
            "js/02-current-nav-sync.js",
            "js/03-copy-actions.js",
            "js/04-platform-filter.js",
            "js/05-nav-label-and-search.js",
            "js/06-keyboard-shortcuts.js",
            "js/07-toc-back-to-top.js",
            "js/08-hash-scroll.js",
            "js/09-contact-popover.js",
            "js/99-iife-close.js"
    ]

    void write(File root, String version = "") {
        File assets = new File(root, "assets")
        assets.mkdirs()
        Files.deleteIfExists(new File(assets, "icon/chevron-double.svg").toPath())
        String cssName    = version ? "apidoc-${version}.css"    : "apidoc.css"
        String jsName     = version ? "apidoc-${version}.js"     : "apidoc.js"
        String searchName = version ? "search-${version}.js"     : "search.js"
        concatResources(CSS_PARTIALS, new File(assets, cssName))
        concatResources(JS_PARTIALS, new File(assets, jsName))
        copyResource("apidoc-search.js",   new File(assets, searchName))
        copyResourceDirectory("icon", new File(assets, "icon"))
    }

    private void concatResources(List<String> names, File target) {
        target.parentFile.mkdirs()
        target.withWriter("UTF-8") { Writer writer ->
            names.each { String name ->
                writer << readResourceText(name)
            }
        }
    }

    private String readResourceText(String name) {
        String resourcePath = "${ASSET_RESOURCE_ROOT}/${name}"
        InputStream input = getClass().classLoader.getResourceAsStream(resourcePath)
        if (input == null) {
            throw new IllegalStateException("Missing HTML asset resource: ${resourcePath}")
        }
        try {
            return input.getText("UTF-8")
        } finally {
            input.close()
        }
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

    private void copyResourceDirectory(String resourceName, File targetDir) {
        URL resource = getClass().classLoader.getResource("${ASSET_RESOURCE_ROOT}/${resourceName}")
        if (resource == null) {
            throw new IllegalStateException("Missing HTML asset resource directory: ${ASSET_RESOURCE_ROOT}/${resourceName}")
        }
        if (resource.protocol == "jar") {
            copyJarResourceDirectory(resource, "${ASSET_RESOURCE_ROOT}/${resourceName}", targetDir)
            return
        }
        if (resource.protocol != "file") return
        File sourceDir = new File(resource.toURI())
        sourceDir.eachFile { File source ->
            if (source.isFile()) {
                File target = new File(targetDir, source.name)
                target.parentFile.mkdirs()
                target.bytes = source.bytes
            }
        }
    }

    private void copyJarResourceDirectory(URL resource, String resourcePath, File targetDir) {
        String external = resource.toExternalForm()
        String jarPath = external.substring("jar:file:".length(), external.indexOf("!"))
        JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"))
        try {
            Enumeration<JarEntry> entries = jar.entries()
            String prefix = resourcePath.endsWith("/") ? resourcePath : "${resourcePath}/"
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement()
                if (entry.directory || !entry.name.startsWith(prefix)) continue
                String relativeName = entry.name.substring(prefix.length())
                if (!relativeName || relativeName.contains("/")) continue
                File target = new File(targetDir, relativeName)
                target.parentFile.mkdirs()
                InputStream input = jar.getInputStream(entry)
                try {
                    target.withOutputStream { OutputStream output -> output << input }
                } finally {
                    input.close()
                }
            }
        } finally {
            jar.close()
        }
    }
}
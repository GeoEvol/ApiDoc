package com.byd.apidoc.render.html

import java.util.jar.JarEntry
import java.util.jar.JarFile

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
        copyResourceDirectory("icon", new File(assets, "icon"))
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

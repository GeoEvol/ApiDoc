package com.byd.apidoc.reference

import com.byd.apidoc.model.LinkRef
import com.byd.apidoc.model.LinkRefKind

class ExternalLinkResolver {
    private final List<ExternalLinkTemplate> templates = []

    ExternalLinkResolver(Collection<ExternalLinkTemplate> templates = defaultTemplates()) {
        this.templates.addAll(templates ?: [])
    }

    LinkRef resolveType(String qualifiedName, String label = null) {
        if (!qualifiedName) {
            return unresolved(qualifiedName, label)
        }
        ExternalLinkTemplate template = templates.find { ExternalLinkTemplate it ->
            qualifiedName == it.packagePrefix || qualifiedName.startsWith("${it.packagePrefix}.")
        }
        if (template == null) {
            return unresolved(qualifiedName, label)
        }
        return new LinkRef(
                kind: LinkRefKind.EXTERNAL,
                rawTarget: qualifiedName,
                label: label ?: simpleName(qualifiedName),
                externalUrl: template.urlFor(qualifiedName),
                fallbackText: label ?: qualifiedName
        )
    }

    static List<ExternalLinkTemplate> defaultTemplates() {
        return [
                new ExternalLinkTemplate(packagePrefix: "java", baseUrl: "https://docs.oracle.com/en/java/javase/17/docs/api/java.base/", pathStyle: ExternalPathStyle.JAVA_PACKAGE),
                new ExternalLinkTemplate(packagePrefix: "javax", baseUrl: "https://docs.oracle.com/en/java/javase/17/docs/api/", pathStyle: ExternalPathStyle.JAVA_PACKAGE),
                new ExternalLinkTemplate(packagePrefix: "android", baseUrl: "https://developer.android.com/reference/", pathStyle: ExternalPathStyle.ANDROID_PACKAGE),
                new ExternalLinkTemplate(packagePrefix: "androidx", baseUrl: "https://developer.android.com/reference/", pathStyle: ExternalPathStyle.ANDROID_PACKAGE)
        ]
    }

    private static LinkRef unresolved(String qualifiedName, String label) {
        new LinkRef(
                kind: LinkRefKind.UNRESOLVED,
                rawTarget: qualifiedName,
                label: label ?: simpleName(qualifiedName),
                fallbackText: label ?: qualifiedName
        )
    }

    private static String simpleName(String qualifiedName) {
        return qualifiedName?.tokenize('.')?.last() ?: qualifiedName
    }
}

class ExternalLinkTemplate {
    String packagePrefix
    String baseUrl
    ExternalPathStyle pathStyle = ExternalPathStyle.JAVA_PACKAGE

    String urlFor(String qualifiedName) {
        String path = qualifiedName.replace('.', '/')
        return "${baseUrl}${path}.html"
    }
}

enum ExternalPathStyle {
    JAVA_PACKAGE,
    ANDROID_PACKAGE
}

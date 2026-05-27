package com.byd.apidoc.resolver

import com.byd.apidoc.model.ApiDoc

class DocPathResolver {
    static final String INDEX = "index.html"
    static final String INDEX_ALL = "index-all.html"
    static final String SEARCH = "search.html"

    String overviewPath() {
        return INDEX
    }

    String indexAllPath() {
        return INDEX_ALL
    }

    String searchPath() {
        return SEARCH
    }

    String packagePath(String packageName) {
        String pkgPath = packageName ? packageName.replace('.', '/') : "default"
        return "${pkgPath}/package-summary.html"
    }

    String classPath(ApiDoc doc) {
        return classPath(doc?.packageName, doc?.name)
    }

    String classPath(String packageName, String className) {
        String pkgPath = packageName ? packageName.replace('.', '/') : "default"
        return "${pkgPath}/${className}.html"
    }
}

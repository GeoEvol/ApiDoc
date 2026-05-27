package com.byd.apidoc.indexer

class SearchIndexItem {
    String category
    String label
    String packageName
    String containingClass
    String url
    Integer kind
    String description

    Map<String, Object> toShortMap() {
        Map<String, Object> map = new LinkedHashMap<>()
        if (packageName) map.p = packageName
        if (containingClass) map.c = containingClass
        if (label) map.l = label
        if (url) map.u = url
        if (kind != null) map.k = kind
        if (description) map.d = description
        return map
    }
}

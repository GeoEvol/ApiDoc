package com.byd.apidoc.model

class DocAnnotation {
    String name
    String qualifiedName
    Map<String, Object> values = [:]
    LinkRef link
}

package com.byd.apidoc.model

class DocId {
    DocIdKind kind
    String qualifiedName
    String canonicalId
    String displayId
    String anchorId
    String signature
    String fragment
    String jvmDescriptor

    String stableKey() {
        if (canonicalId) {
            return canonicalId
        }
        return [kind?.name(), qualifiedName, signature, fragment]
                .findAll { it != null && !it.toString().isEmpty() }
                .join(":")
    }

    String effectiveAnchorId() {
        return anchorId ?: fragment
    }
}

enum DocIdKind {
    MODULE,
    PACKAGE,
    TYPE,
    FIELD,
    METHOD,
    CONSTRUCTOR,
    ENUM_CONSTANT,
    RECORD_COMPONENT,
    ANNOTATION_ELEMENT
}

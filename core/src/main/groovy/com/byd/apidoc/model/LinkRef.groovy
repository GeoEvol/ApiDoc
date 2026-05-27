package com.byd.apidoc.model

class LinkRef {
    LinkRefKind kind = LinkRefKind.UNRESOLVED
    String rawTarget
    String label
    DocId targetId
    String externalUrl
    String fallbackText
    List<DocDiagnostic> diagnostics = []
}

enum LinkRefKind {
    INTERNAL,
    EXTERNAL,
    UNRESOLVED
}

package com.byd.apidoc.model

class DocDiagnostic {
    DiagnosticSeverity severity = DiagnosticSeverity.INFO
    String code
    String message
    DocId targetId
    String sourcePath
    Long line
    Long column
}

enum DiagnosticSeverity {
    INFO,
    WARNING,
    ERROR
}

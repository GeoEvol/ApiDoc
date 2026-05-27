package com.byd.apidoc.model

class TypeRef {
    TypeRefKind kind = TypeRefKind.DECLARED
    String rawText
    String displayName
    String simpleName
    String qualifiedName
    TypeRef owner
    DocId targetId
    LinkRef linkRef
    List<TypeRef> typeArguments = []
    TypeRef componentType
    int arrayDepth = 0
    TypeRef bound
    List<TypeRef> bounds = []
    List<TypeRef> upperBounds = []
    List<TypeRef> lowerBounds = []
    boolean nullable = false
    boolean varargs = false
    List<DocAnnotation> annotations = []
}

enum TypeRefKind {
    PRIMITIVE,
    DECLARED,
    ARRAY,
    TYPE_VARIABLE,
    WILDCARD,
    VOID,
    UNKNOWN
}

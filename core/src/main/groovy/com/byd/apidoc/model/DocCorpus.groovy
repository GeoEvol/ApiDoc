package com.byd.apidoc.model

import com.byd.apidoc.comment.CommentDoc
import com.byd.apidoc.metadata.ApiMetadata

class DocCorpus {
    String schemaVersion = "v1"
    List<DocPackage> packages = []
    List<DocType> types = []
    List<DocMember> members = []
    Map<String, CommentDoc> comments = [:]
    Map<String, List<DocAnnotation>> annotations = [:]
    Map<String, List<LinkRef>> refs = [:]
    Map<String, ApiMetadata> metadata = [:]
    List<DocDiagnostic> diagnostics = []
}

class DocPackage {
    DocId id
    String name
    CommentDoc comment
    ApiMetadata metadata
    List<DocId> typeIds = []
}

class DocType {
    DocId id
    String name
    String qualifiedName
    String packageName
    DocTypeKind kind = DocTypeKind.CLASS
    Set<String> modifiers = new LinkedHashSet<>()
    List<TypeParameterRef> typeParameters = []
    TypeRef superType
    List<TypeRef> interfaces = []
    List<DocAnnotation> annotations = []
    CommentDoc comment
    ApiMetadata metadata
    List<DocId> memberIds = []
    List<DocId> nestedTypeIds = []
}

class DocMember {
    DocId id
    DocId ownerId
    String name
    String qualifiedName
    DocMemberKind kind = DocMemberKind.METHOD
    Set<String> modifiers = new LinkedHashSet<>()
    TypeRef type
    TypeRef returnType
    List<TypeParameterRef> typeParameters = []
    List<DocParameter> parameters = []
    List<TypeRef> throwsTypes = []
    Object constantValue
    Object defaultValue
    List<DocAnnotation> annotations = []
    CommentDoc comment
    ApiMetadata metadata
}

class DocParameter {
    String name
    TypeRef type
    boolean varargs = false
    List<DocAnnotation> annotations = []
}

class TypeParameterRef {
    String name
    List<TypeRef> bounds = []
}

enum DocTypeKind {
    CLASS,
    INTERFACE,
    ENUM,
    ANNOTATION,
    RECORD,
    EXCEPTION,
    ERROR
}

enum DocMemberKind {
    FIELD,
    METHOD,
    CONSTRUCTOR,
    ENUM_CONSTANT,
    RECORD_COMPONENT,
    ANNOTATION_ELEMENT
}

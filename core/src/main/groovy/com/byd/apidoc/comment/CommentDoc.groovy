package com.byd.apidoc.comment

import com.byd.apidoc.model.DocDiagnostic
import com.byd.apidoc.model.LinkRef

class CommentDoc {
    String rawText
    List<CommentNode> summaryNodes = []
    List<CommentNode> bodyNodes = []
    List<BlockTag> blockTags = []
    List<InlineTag> inlineNodes = []
    List<DocDiagnostic> diagnostics = []
}

class CommentNode {
    CommentNodeKind kind = CommentNodeKind.TEXT
    String text
    InlineTag inlineTag
    List<CommentNode> children = []
}

class BlockTag {
    BlockTagKind kind = BlockTagKind.UNKNOWN
    String name
    String rawName
    String key
    List<CommentNode> body = []
    String rawText
    boolean known = false
}

class ParamTag extends BlockTag {
    String parameterName
    boolean typeParameter = false
}

class ReturnTag extends BlockTag {
}

class ThrowsTag extends BlockTag {
    String exceptionName
    LinkRef exceptionRef
}

class SeeTag extends BlockTag {
    LinkRef reference
}

class SinceTag extends BlockTag {
    String version
}

class DeprecatedTag extends BlockTag {
    String message
}

class InlineTag {
    InlineTagKind kind = InlineTagKind.UNKNOWN
    String name
    String rawName
    LinkRef reference
    List<CommentNode> label = []
    String body
    String rawText
    boolean known = false
}

enum CommentNodeKind {
    TEXT,
    INLINE_TAG,
    HTML,
    ENTITY,
    ERROR
}

enum BlockTagKind {
    PARAM,
    RETURN,
    THROWS,
    SEE,
    SINCE,
    DEPRECATED,
    CUSTOM,
    UNKNOWN
}

enum InlineTagKind {
    LINK,
    LINKPLAIN,
    CODE,
    LITERAL,
    INHERIT_DOC,
    VALUE,
    CUSTOM,
    UNKNOWN
}

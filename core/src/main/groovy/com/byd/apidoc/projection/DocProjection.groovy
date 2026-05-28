package com.byd.apidoc.projection

import com.byd.apidoc.metadata.ApiMetadata
import com.byd.apidoc.metadata.ApiValueRange
import com.byd.apidoc.comment.CommentDoc
import com.byd.apidoc.model.DocId
import com.byd.apidoc.model.DocMemberKind
import com.byd.apidoc.model.DocParameter
import com.byd.apidoc.model.TypeRef

class DocProjection {
    List<PageModel> pages = []
    List<TypePageModel> typePages = []
    List<NavNode> nav = []
    List<SearchEntry> search = []
}

class PageModel {
    DocId id
    PageKind kind = PageKind.TYPE
    String title
    String url
    DocId targetId
    String summary
    ApiMetadata metadata
}

class NavNode {
    String label
    NavNodeKind kind = NavNodeKind.TYPE
    String url
    DocId targetId
    List<String> activePath = []
    String group
    List<NavNode> children = []
}

class SearchEntry {
    SearchEntryKind kind = SearchEntryKind.CLASS
    String label
    String qualifiedName
    String packageName
    String ownerName
    String url
    String anchor
    String summary
    ApiMetadata metadata
    ApiStatusModel status
    String displaySignature
    List<String> tokens = []
}

class TypePageModel {
    DocId id
    String title
    String packageName
    String declaration
    String summary
    CommentDoc comment
    ApiMetadata metadata
    List<BreadcrumbModel> breadcrumbs = []
    List<TocEntryModel> rightToc = []
    ApiStatusModel apiStatus
    AndroidTypeHeaderModel typeHeader
    TypeRef inheritance
    List<TypeRef> interfaces = []
    List<MemberGroupModel> memberGroups = []
    List<MemberDetailModel> memberDetails = []
    List<InheritedMemberGroupModel> inheritedMemberGroups = []
}

class BreadcrumbModel {
    String label
    String url
    DocId targetId
}

class TocEntryModel {
    String label
    String anchor
    int level = 2
}

class ApiStatusModel {
    boolean hidden = false
    boolean removed = false
    boolean deprecated = false
    String since
    Integer apiSince
    String deprecatedSince
    String removedSince
    String deprecatedMessage
    String removedMessage
    boolean pending = false
    String sdkExtensionSince
    Set<String> permissions = new LinkedHashSet<>()
    String nullability
    List<ApiValueRange> valueRanges = []
}

class AndroidTypeHeaderModel {
    String title
    String packageName
    String declaration
    TypeRef inheritance
    List<TypeRef> interfaces = []
}

class InheritedMemberGroupModel {
    String title
    String ownerName
    String ownerQualifiedName
    List<MemberSummaryModel> members = []
}

class MemberGroupModel {
    String title
    String kind
    List<MemberSummaryModel> members = []
}

class MemberSummaryModel {
    DocId id
    String name
    String displayName
    String url
    String modifierAndType
    String kind
    String summary
    ApiMetadata metadata
    ApiStatusModel status
}

class MemberDetailModel {
    DocId id
    String name
    String displayName
    String declaration
    DocMemberKind kind
    Set<String> modifiers = new LinkedHashSet<>()
    TypeRef type
    TypeRef returnType
    List<DocParameter> parameters = []
    List<TypeRef> throwsTypes = []
    String summary
    CommentDoc comment
    ApiMetadata metadata
    ApiStatusModel status
}

enum PageKind {
    INDEX,
    PACKAGES,
    CLASSES,
    PACKAGE,
    TYPE,
    SEARCH
}

enum NavNodeKind {
    ROOT,
    PACKAGE,
    GROUP,
    TYPE,
    MEMBER
}

enum SearchEntryKind {
    PACKAGE,
    CLASS,
    INTERFACE,
    ENUM,
    ANNOTATION,
    RECORD,
    CONSTRUCTOR,
    METHOD,
    FIELD,
    CONSTANT
}

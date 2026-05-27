package com.byd.apidoc.model

class ApiMethodDoc {
    String name
    String desc
    String description
    String qualifiedName
    DRI dri

    String url
    String signature
    String modifiers
    String returnType
    String returnComment
    List<String> exceptions = []
    Map<String, String> exceptionComments = [:]
    List<String> annotations = []
    List<ParameterDoc> parameters = []

    String anchorId
    boolean isStatic = false
    boolean isConstructor = false
    String genericSignature

    Set<TagDoc> tagRefs = new LinkedHashSet<>()

    Set<TagDoc> getTagRefs() {
        return tagRefs
    }

    @Override
    boolean equals(Object o) {
        if (this.is(o)) return true
        if (o == null || getClass() != o.getClass()) return false
        ApiMethodDoc that = (ApiMethodDoc) o
        return Objects.equals(name, that.name) && Objects.equals(url, that.url)
    }

    @Override
    int hashCode() {
        return Objects.hash(name, url)
    }

    String toString() {
        return "ApiMethodDoc{name='${name}', url='${url}'}"
    }
}

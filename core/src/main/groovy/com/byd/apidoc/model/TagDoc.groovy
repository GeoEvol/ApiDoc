package com.byd.apidoc.model

class TagDoc {

    String tag

    // Keep insertion order stable for rendering and search index generation.
    Set<ApiDoc> clazzDocs = new LinkedHashSet<>()
    Set<ApiMethodDoc> methodDocs = new LinkedHashSet<>()

    TagDoc() {
    }

    TagDoc(String tag) {
        this.tag = tag
    }

    String getTag() {
        return tag
    }

    void setTag(String tag) {
        this.tag = tag
    }

    Set<ApiDoc> getClazzDocs() {
        return clazzDocs
    }

    Set<ApiMethodDoc> getMethodDocs() {
        return methodDocs
    }

    @Override
    boolean equals(Object o) {
        if (this.is(o)) return true
        if (o == null || getClass() != o.getClass()) return false
        TagDoc tagDoc = (TagDoc) o
        // Compare by tag name only to avoid recursive graph equality.
        return Objects.equals(tag, tagDoc.tag)
    }

    @Override
    int hashCode() {
        return Objects.hash(tag)
    }

    String toString() {
        return "TagDoc{tag='${tag}'}"
    }
}

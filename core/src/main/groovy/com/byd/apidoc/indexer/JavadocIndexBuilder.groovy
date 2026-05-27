package com.byd.apidoc.indexer

import com.byd.apidoc.model.ApiDoc
import com.byd.apidoc.model.ApiMethodDoc
import com.byd.apidoc.model.FieldDoc
import com.byd.apidoc.model.TagDoc
import com.byd.apidoc.model.TemplateData
import groovy.json.JsonOutput

class JavadocIndexBuilder {
    static final int KIND_ENUM_CONSTANT = 0
    static final int KIND_FIELD = 1
    static final int KIND_STATIC_FIELD = 2
    static final int KIND_CONSTRUCTOR = 3
    static final int KIND_METHOD = 5
    static final int KIND_STATIC_METHOD = 6
    static final int KIND_ANNOTATION_TYPE = 8
    static final int KIND_ENUM = 9
    static final int KIND_INTERFACE = 10
    static final int KIND_CLASS = 12
    static final int KIND_EXCEPTION_CLASS = 13
    static final int KIND_SEARCH_TAG = 14

    void writeSearchIndexFiles(TemplateData data, File outputDir) {
        writeIndexFile(outputDir, "module-search-index.js", "moduleSearchIndex", [])
        writeIndexFile(outputDir, "package-search-index.js", "packageSearchIndex", packageItems(data))
        writeIndexFile(outputDir, "type-search-index.js", "typeSearchIndex", typeItems(data))
        writeIndexFile(outputDir, "member-search-index.js", "memberSearchIndex", memberItems(data))
        writeIndexFile(outputDir, "tag-search-index.js", "tagSearchIndex", tagItems(data))
    }

    List<SearchIndexItem> allItems(TemplateData data) {
        return packageItems(data) + typeItems(data) + memberItems(data) + tagItems(data)
    }

    private List<SearchIndexItem> packageItems(TemplateData data) {
        data.packageIndex.keySet().sort().collect { String pkg ->
            new SearchIndexItem(
                    category: "packages",
                    label: pkg,
                    url: data.packagePathMap[pkg]
            )
        }
    }

    private List<SearchIndexItem> typeItems(TemplateData data) {
        data.apiDocs.collect { ApiDoc doc ->
            new SearchIndexItem(
                    category: "types",
                    label: doc.name,
                    packageName: doc.packageName,
                    url: data.docPathMap[doc],
                    kind: typeKind(doc),
                    description: doc.description
            )
        }.sort { a, b -> (a.label <=> b.label) ?: (a.packageName <=> b.packageName) }
    }

    private List<SearchIndexItem> memberItems(TemplateData data) {
        List<SearchIndexItem> items = []
        data.apiDocs.each { ApiDoc doc ->
            doc.fields?.each { FieldDoc field ->
                items.add(new SearchIndexItem(
                        category: "members",
                        label: field.name,
                        packageName: doc.packageName,
                        containingClass: doc.name,
                        url: field.anchorId,
                        kind: field.isStatic ? KIND_STATIC_FIELD : KIND_FIELD,
                        description: field.description
                ))
            }
            doc.constructors?.each { ApiMethodDoc method ->
                items.add(memberItem(doc, method, KIND_CONSTRUCTOR))
            }
            doc.list?.each { ApiMethodDoc method ->
                items.add(memberItem(doc, method, method.isStatic ? KIND_STATIC_METHOD : KIND_METHOD))
            }
            doc.enumConstants?.each { enumConstant ->
                items.add(new SearchIndexItem(
                        category: "members",
                        label: enumConstant.name,
                        packageName: doc.packageName,
                        containingClass: doc.name,
                        url: enumConstant.name,
                        kind: KIND_ENUM_CONSTANT,
                        description: enumConstant.description
                ))
            }
        }
        return items.sort { a, b -> (a.label <=> b.label) ?: (a.containingClass <=> b.containingClass) }
    }

    private SearchIndexItem memberItem(ApiDoc doc, ApiMethodDoc method, int kind) {
        return new SearchIndexItem(
                category: "members",
                label: method.anchorId ?: method.name,
                packageName: doc.packageName,
                containingClass: doc.name,
                url: method.anchorId,
                kind: kind,
                description: method.description
        )
    }

    private List<SearchIndexItem> tagItems(TemplateData data) {
        List<SearchIndexItem> items = []
        data.apiDocs.each { ApiDoc doc ->
            doc.tagRefs?.each { TagDoc tag ->
                items.add(new SearchIndexItem(
                        category: "searchTags",
                        label: tag.tag,
                        url: "${data.docPathMap[doc]}",
                        kind: KIND_SEARCH_TAG,
                        description: doc.name
                ))
            }
            (doc.list + doc.constructors).each { ApiMethodDoc method ->
                method.tagRefs?.each { TagDoc tag ->
                    items.add(new SearchIndexItem(
                            category: "searchTags",
                            label: tag.tag,
                            url: "${data.docPathMap[doc]}#${method.anchorId}",
                            kind: KIND_SEARCH_TAG,
                            description: "${doc.name}.${method.name}"
                    ))
                }
            }
        }
        return items.unique { "${it.label}|${it.url}" }.sort { a, b -> a.label <=> b.label }
    }

    private static int typeKind(ApiDoc doc) {
        if (doc.isAnnotation) return KIND_ANNOTATION_TYPE
        if (doc.isEnum()) return KIND_ENUM
        if (doc.isInterface) return KIND_INTERFACE
        if (doc.isException()) return KIND_EXCEPTION_CLASS
        return KIND_CLASS
    }

    private static void writeIndexFile(File outputDir, String fileName, String varName, List<SearchIndexItem> items) {
        outputDir.mkdirs()
        String json = JsonOutput.toJson(items.collect { it.toShortMap() })
        new File(outputDir, fileName).withWriter("UTF-8") { writer ->
            writer.write("${varName} = ${json};\n")
            writer.write("updateSearchResults();\n")
        }
    }
}

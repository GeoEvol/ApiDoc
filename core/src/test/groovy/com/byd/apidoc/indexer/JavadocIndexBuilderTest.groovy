package com.byd.apidoc.indexer

import com.byd.apidoc.model.ApiDoc
import com.byd.apidoc.model.ApiMethodDoc
import com.byd.apidoc.model.FieldDoc
import com.byd.apidoc.model.TagDoc
import com.byd.apidoc.model.TemplateData
import groovy.json.JsonSlurper
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class JavadocIndexBuilderTest {

    @Test
    void testWritesJdkStyleShortFieldIndexes() {
        def doc = new ApiDoc(
                name: "UserService",
                packageName: "com.example",
                description: "Service entry point",
                fields: [new FieldDoc(name: "name", anchorId: "name", description: "Display name")],
                constructors: [new ApiMethodDoc(name: "UserService", anchorId: "UserService()", description: "Ctor")],
                list: [new ApiMethodDoc(name: "execute", anchorId: "execute(java.lang.String)", description: "Runs the service")],
                tagRefs: [new TagDoc("since:1.0")] as Set
        )
        doc.list[0].tagRefs << new TagDoc("service")

        def data = new TemplateData(
                projectName: "Fixture",
                apiDocs: [doc],
                packageIndex: ["com.example": [doc]],
                packagePathMap: ["com.example": "com/example/package-summary.html"],
                docPathMap: [(doc): "com/example/UserService.html"]
        )

        def outputDir = new File("build/test-index-output")
        deleteDir(outputDir)

        new JavadocIndexBuilder().writeSearchIndexFiles(data, outputDir)

        def typeItems = parseIndex(outputDir, "type-search-index.js", "typeSearchIndex")
        def memberItems = parseIndex(outputDir, "member-search-index.js", "memberSearchIndex")
        def tagItems = parseIndex(outputDir, "tag-search-index.js", "tagSearchIndex")

        assertEquals([[p: "com.example", l: "UserService", u: "com/example/UserService.html", k: 12, d: "Service entry point"]], typeItems)
        assertTrue(memberItems.any { it.l == "execute(java.lang.String)" && it.c == "UserService" && it.u == "execute(java.lang.String)" })
        assertTrue(memberItems.any { it.l == "UserService()" && it.k == 3 })
        assertTrue(memberItems.any { it.l == "name" && it.k == 1 })
        assertTrue(tagItems.any { it.l == "since:1.0" && it.u == "com/example/UserService.html" })
        assertTrue(tagItems.any { it.l == "service" && it.u == "com/example/UserService.html#execute(java.lang.String)" })
    }

    private static List<Map<String, Object>> parseIndex(File dir, String fileName, String varName) {
        String text = new File(dir, fileName).getText("UTF-8")
        String json = text.substring("${varName} = ".length(), text.indexOf(";\nupdateSearchResults();"))
        return (List<Map<String, Object>>) new JsonSlurper().parseText(json)
    }

    private static void deleteDir(File dir) {
        if (dir.exists()) {
            dir.deleteDir()
        }
    }
}

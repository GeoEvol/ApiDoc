package com.byd.apidoc.output

import com.byd.apidoc.projection.DocProjection

class ProjectionWriter {
    static final String PAGE_INDEX_FILE = "page-index.json"
    static final String NAV_INDEX_FILE = "nav-index.json"
    static final String SEARCH_INDEX_FILE = "search-index.json"

    private final JsonWriter jsonWriter

    ProjectionWriter(JsonWriter jsonWriter = new JsonWriter()) {
        this.jsonWriter = jsonWriter
    }

    Map<String, File> write(DocProjection projection, File outputDir) {
        DocProjection effective = projection ?: new DocProjection()
        Map<String, File> files = [
                pages : new File(outputDir, PAGE_INDEX_FILE),
                nav   : new File(outputDir, NAV_INDEX_FILE),
                search: new File(outputDir, SEARCH_INDEX_FILE)
        ]
        jsonWriter.write(effective.pages, files.pages)
        jsonWriter.write(effective.nav, files.nav)
        jsonWriter.write(effective.search, files.search)
        return files
    }
}

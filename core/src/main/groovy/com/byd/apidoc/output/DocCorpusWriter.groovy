package com.byd.apidoc.output

import com.byd.apidoc.model.DocCorpus

class DocCorpusWriter {
    static final String FILE_NAME = "doc-corpus.json"

    private final JsonWriter jsonWriter

    DocCorpusWriter(JsonWriter jsonWriter = new JsonWriter()) {
        this.jsonWriter = jsonWriter
    }

    File write(DocCorpus corpus, File outputDir) {
        File outputFile = new File(outputDir, FILE_NAME)
        jsonWriter.write(corpus ?: new DocCorpus(), outputFile)
        return outputFile
    }
}

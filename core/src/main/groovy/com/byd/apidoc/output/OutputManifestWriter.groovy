package com.byd.apidoc.output

import com.byd.apidoc.model.ApiConfig

import java.time.Clock
import java.time.Instant

class OutputManifestWriter {
    static final String FILE_NAME = "output-manifest.json"
    static final String SCHEMA_VERSION = "1.0"
    static final String GENERATOR_VERSION = "1.0.0"
    static final String HTML_OUTPUT_DIR = "api-docs-html/"
    static final String MARKDOWN_OUTPUT_DIR = "api-docs-md/"

    private final JsonWriter jsonWriter
    private final Clock clock

    OutputManifestWriter(JsonWriter jsonWriter = new JsonWriter(), Clock clock = Clock.systemUTC()) {
        this.jsonWriter = jsonWriter
        this.clock = clock
    }

    File write(File outputDir) {
        return write(outputDir, null)
    }

    File write(File outputDir, String outputFormat) {
        LinkedHashMap<String, String> outputs = [
                corpus  : DocCorpusWriter.FILE_NAME,
                pages   : ProjectionWriter.PAGE_INDEX_FILE,
                nav     : ProjectionWriter.NAV_INDEX_FILE,
                search  : ProjectionWriter.SEARCH_INDEX_FILE,
                manifest: FILE_NAME
        ] as LinkedHashMap<String, String>
        if (outputFormat == null || outputFormat.trim().isEmpty()) {
            outputs.html = HTML_OUTPUT_DIR
            outputs.markdown = MARKDOWN_OUTPUT_DIR
        } else if (ApiConfig.FORMAT_HTML.equalsIgnoreCase(outputFormat)) {
            outputs.html = HTML_OUTPUT_DIR
        } else if (ApiConfig.FORMAT_MARKDOWN.equalsIgnoreCase(outputFormat)) {
            outputs.markdown = MARKDOWN_OUTPUT_DIR
        } else {
            throw new IllegalArgumentException("Unsupported output format: ${outputFormat}")
        }

        OutputManifest manifest = new OutputManifest(
                schemaVersion: SCHEMA_VERSION,
                generatorVersion: GENERATOR_VERSION,
                generatedAt: Instant.now(clock).toString(),
                outputs: outputs
        )
        File outputFile = new File(outputDir, FILE_NAME)
        jsonWriter.write(manifest, outputFile)
        return outputFile
    }
}

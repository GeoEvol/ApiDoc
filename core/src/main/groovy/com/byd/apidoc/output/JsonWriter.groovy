package com.byd.apidoc.output

import groovy.json.JsonOutput

class JsonWriter {
    void write(Object value, File outputFile) {
        if (outputFile.parentFile != null) {
            outputFile.parentFile.mkdirs()
        }
        outputFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(value))
    }
}

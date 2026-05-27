package com.byd.apidoc.output

class OutputManifest {
    String schemaVersion = "1.0"
    String generatorVersion = "1.0.0"
    String generatedAt
    Map<String, String> outputs = new LinkedHashMap<>()
}

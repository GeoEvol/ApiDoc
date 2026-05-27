package com.byd.apidoc.model

import com.byd.apidoc.utils.TypeLinker

class TemplateData {
    String projectName
    List<ApiDoc> apiDocs = []
    ApiConfig config
    String outputFormat

    Map<String, List<ApiDoc>> packageIndex = [:]
    Map<String, List<ApiDoc>> tagIndex = [:]
    List<ApiDoc> interfaces = []
    List<ApiDoc> classes = []
    List<ApiDoc> enums = []
    List<ApiDoc> annotations = []
    List<ApiDoc> exceptions = []

    Map<String, List<ApiMethodDoc>> methodOverloads = [:]
    Set<String> duplicateSimpleNames = []
    Map<String, Object> docStats = [:]
    Map<ApiDoc, String> docPathMap = [:]
    Map<String, String> packagePathMap = [:]
    Map<String, String> docPathByName = [:]
    TypeLinker typeLinker

    String overviewPath = "index.html"
    String indexAllPath = "index-all.html"
    String searchPath = "search.html"

    Date generatedAt
    String pluginVersion

    boolean isHtml() {
        return ApiConfig.FORMAT_HTML.equalsIgnoreCase(outputFormat)
    }

    boolean isMarkdown() {
        return ApiConfig.FORMAT_MARKDOWN.equalsIgnoreCase(outputFormat)
    }
}

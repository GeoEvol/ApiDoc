package com.byd.apidoc.render.html

import com.byd.apidoc.projection.SearchEntry
import com.byd.apidoc.projection.SearchEntryKind

class HtmlSearchIndexViewBuilder {

    List<Map<String, Object>> build(List<SearchEntry> entries) {
        (entries ?: []).collect { SearchEntry entry -> view(entry) }
    }

    private static Map<String, Object> view(SearchEntry entry) {
        String simple = simpleNameFor(entry)
        String ownerQualified = entry.ownerName
        String ownerSimple = simpleName(ownerQualified)
        [
                kind              : entry.kind,
                label             : entry.label,
                qualifiedName     : entry.qualifiedName,
                packageName       : entry.packageName,
                ownerName         : entry.ownerName,
                url               : entry.url,
                anchor            : entry.anchor,
                summary           : entry.summary,
                metadata          : entry.metadata,
                status            : entry.status,
                displaySignature  : entry.displaySignature,
                platforms         : copyList(entry.platforms),
                tokens            : copyList(entry.tokens),
                simpleName        : simple,
                ownerSimpleName   : ownerSimple,
                ownerQualifiedName: ownerQualified,
                displayTitle      : displayTitle(entry, simple),
                searchText        : searchText(entry, simple, ownerSimple, ownerQualified)
        ]
    }

    private static String simpleNameFor(SearchEntry entry) {
        if (entry.kind == SearchEntryKind.PACKAGE) {
            return entry.label ?: entry.qualifiedName
        }
        simpleName(entry.qualifiedName) ?: entry.label
    }

    private static String displayTitle(SearchEntry entry, String simple) {
        if (entry.kind == SearchEntryKind.PACKAGE) {
            return entry.label ?: entry.qualifiedName ?: simple
        }
        if (entry.kind in [SearchEntryKind.CONSTRUCTOR, SearchEntryKind.METHOD, SearchEntryKind.FIELD, SearchEntryKind.CONSTANT]) {
            return entry.label ?: simple ?: entry.qualifiedName
        }
        return simple ?: entry.label ?: entry.qualifiedName
    }

    private static String searchText(SearchEntry entry, String simple, String ownerSimple, String ownerQualified) {
        LinkedHashSet<String> values = new LinkedHashSet<>()
        add(values, entry.kind?.name())
        add(values, entry.label)
        add(values, simple)
        add(values, entry.qualifiedName)
        add(values, entry.packageName)
        add(values, entry.ownerName)
        add(values, ownerQualified)
        add(values, ownerSimple)
        add(values, entry.displaySignature)
        add(values, entry.summary)
        (entry.tokens ?: []).each { Object token -> add(values, token?.toString()) }
        values.join(" ")
    }

    private static void add(Set<String> values, String value) {
        if (value != null && !value.trim().isEmpty()) {
            values.add(value)
        }
    }

    private static String simpleName(String value) {
        if (value == null || value.isEmpty()) return value
        List<String> parts = value.tokenize(".")
        parts ? parts.last() : value
    }

    private static List<String> copyList(Collection<String> values) {
        (values ?: []).collect { it?.toString() }.findAll { it != null }
    }
}

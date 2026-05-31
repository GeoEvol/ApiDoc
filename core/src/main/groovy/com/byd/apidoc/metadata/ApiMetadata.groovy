package com.byd.apidoc.metadata

class ApiMetadata {
    ApiVisibility visibility = ApiVisibility.PUBLIC
    ApiAvailability availability = ApiAvailability.NORMAL
    String since
    String deprecatedSince
    String removedSince
    Integer apiLevel
    boolean pending = false
    String sdkExtensionSince
    DeprecatedMetadata deprecated
    RemovedMetadata removed
    Set<String> permissions = new LinkedHashSet<>()
    String nullability
    List<ApiValueRange> valueRanges = []
    List<String> supportedPlatforms = []
    List<MetadataSource> metadataSources = []
    Set<String> sourceTags = new LinkedHashSet<>()
    Set<String> sourceAnnotations = new LinkedHashSet<>()
    Map<String, Object> attributes = [:]
}

class MetadataSource {
    String kind
    String name
    String property
    Object rawValue
}

class ApiValueRange {
    String kind
    Object from
    Object to
}

class DeprecatedMetadata {
    boolean fromJavadocTag = false
    boolean fromAnnotation = false
    String message
}

class RemovedMetadata {
    boolean fromJavadocTag = false
    String message
}

enum ApiVisibility {
    PUBLIC,
    PROTECTED,
    PACKAGE_PRIVATE,
    PRIVATE,
    HIDDEN,
    INTERNAL
}

enum ApiAvailability {
    NORMAL,
    DEPRECATED,
    REMOVED
}

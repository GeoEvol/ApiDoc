package com.byd.apidoc.metadata

class VisibilityPolicy {
    boolean includePublic = true
    boolean includeProtected = true
    boolean includePackagePrivate = false
    boolean includePrivate = false
    boolean includeHidden = false
    boolean includeRemoved = false

    boolean includes(ApiMetadata metadata) {
        ApiMetadata effective = metadata ?: new ApiMetadata()
        if (effective.availability == ApiAvailability.REMOVED && !includeRemoved) {
            return false
        }
        switch (effective.visibility) {
            case ApiVisibility.PUBLIC:
                return includePublic
            case ApiVisibility.PROTECTED:
                return includeProtected
            case ApiVisibility.PACKAGE_PRIVATE:
                return includePackagePrivate
            case ApiVisibility.PRIVATE:
                return includePrivate
            case ApiVisibility.HIDDEN:
            case ApiVisibility.INTERNAL:
                return includeHidden
            default:
                return false
        }
    }
}

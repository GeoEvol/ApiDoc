package com.byd.apidoc.model

/**
 * Stable documentation resource identifier.
 */
class DRI {
    String packageName
    String className
    String memberName
    List<String> parameterTypes = []

    static DRI classDri(String packageName, String className) {
        return new DRI(packageName: packageName, className: className)
    }

    static DRI memberDri(String packageName, String className, String memberName, List<String> parameterTypes) {
        return new DRI(
                packageName: packageName,
                className: className,
                memberName: memberName,
                parameterTypes: parameterTypes ?: []
        )
    }

    String asString() {
        String base = packageName ? "${packageName}.${className}" : className
        if (!memberName) {
            return base
        }
        return "${base}#${memberName}(${parameterTypes.join(',')})"
    }

    String toString() {
        return asString()
    }
}

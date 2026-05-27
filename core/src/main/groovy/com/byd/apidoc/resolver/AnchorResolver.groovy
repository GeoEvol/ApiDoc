package com.byd.apidoc.resolver

import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types

class AnchorResolver {

    String forExecutable(String displayName, ExecutableElement executable, Types types) {
        String name = displayName ?: executable.simpleName.toString()
        String params = executable.parameters.collect { VariableElement parameter ->
            anchorType(parameter.asType(), types)
        }.join(",")
        return "${name}(${params})".replaceAll(/\s+/, "")
    }

    String forField(String name) {
        return name
    }

    private static String anchorType(TypeMirror type, Types types) {
        TypeMirror erased = types != null ? types.erasure(type) : type
        String text = erased?.toString() ?: ""
        if (text.endsWith("[]")) {
            return text
        }
        return text
    }
}

package com.byd.apidoc.doclet

import java.util.concurrent.ConcurrentHashMap

class BuildContextRegistry {
    private static final Map<String, BuildContext> CONTEXTS = new ConcurrentHashMap<>()

    static BuildContext register(BuildContext context) {
        if (context == null) {
            throw new IllegalArgumentException("BuildContext must not be null")
        }
        context.id = context.id ?: UUID.randomUUID().toString()
        CONTEXTS.put(context.id, context)
        return context
    }

    static BuildContext get(String id) {
        return id ? CONTEXTS.get(id) : null
    }

    static void remove(String id) {
        if (id) {
            CONTEXTS.remove(id)
        }
    }

    static int size() {
        return CONTEXTS.size()
    }
}

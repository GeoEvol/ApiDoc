package com.byd.apidoc.model

class FieldDoc {
    String name
    String anchorId
    String type
    String description
    boolean isStatic
    String modifier
    List<String> annotations = []
}

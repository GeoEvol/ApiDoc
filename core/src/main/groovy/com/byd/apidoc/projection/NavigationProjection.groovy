package com.byd.apidoc.projection

class NavigationProjection {
    List<PackageNode> packages = []
}

class PackageNode {
    String name
    String path
    List<ClassNode> classes = []
}

class ClassNode {
    String name
    String path
    String kind
}

package com.byd.apidoc.projection

class BreadcrumbProjection {
    List<BreadcrumbItem> items = []
}

class BreadcrumbItem {
    String label
    String path
}

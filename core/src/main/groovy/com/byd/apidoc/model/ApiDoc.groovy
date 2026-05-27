package com.byd.apidoc.model

class ApiDoc {
    String name
    String desc
    String description
    String packageName
    String qualifiedName
    String path
    DRI dri

    String superClass
    List<String> interfaces = []
    List<String> typeParameters = []
    String genericSignature
    List<String> annotations = []

    List<FieldDoc> fields = []
    List<ApiMethodDoc> constructors = []
    List<EnumConstantDoc> enumConstants = []
    List<ApiMethodDoc> list = []

    Set<TagDoc> tagRefs = new LinkedHashSet<>()

    boolean isInterface = false
    boolean isAnnotation = false
    boolean enumType = false
    boolean exceptionType = false

    boolean isEnum() {
        return enumType || (enumConstants != null && !enumConstants.isEmpty())
    }

    boolean isException() {
        return exceptionType || (superClass != null && superClass.endsWith("Exception"))
    }

    Set<TagDoc> getTagRefs() {
        return tagRefs
    }

    String getFullyQualifiedName() {
        if (qualifiedName) {
            return qualifiedName
        }
        if (packageName) {
            return packageName + "." + name
        }
        return name
    }

    @Override
    boolean equals(Object o) {
        if (this.is(o)) return true
        if (o == null || getClass() != o.getClass()) return false
        ApiDoc apiDoc = (ApiDoc) o
        return Objects.equals(getFullyQualifiedName(), apiDoc.getFullyQualifiedName())
    }

    @Override
    int hashCode() {
        return Objects.hash(getFullyQualifiedName())
    }
}

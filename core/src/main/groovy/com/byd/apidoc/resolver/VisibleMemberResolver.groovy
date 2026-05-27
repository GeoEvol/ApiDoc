package com.byd.apidoc.resolver

import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class VisibleMemberResolver {
    private final Elements elements
    private final Types types

    VisibleMemberResolver(Elements elements, Types types) {
        this.elements = elements
        this.types = types
    }

    List<Element> resolve(TypeElement typeElement, boolean includeInheritedMembers) {
        List<Element> declared = typeElement.enclosedElements
        if (!includeInheritedMembers) {
            return declared
        }

        List<Element> result = []
        result.addAll(declared)
        collectInherited(typeElement, typeElement, result, new LinkedHashSet<String>())
        return result
    }

    private void collectInherited(TypeElement root, TypeElement current, List<Element> result, Set<String> visited) {
        inheritedParents(current).each { TypeElement parent ->
            String key = parent.qualifiedName.toString()
            if (!visited.add(key)) {
                return
            }
            parent.enclosedElements.each { Element member ->
                if (isInheritable(root, member) && !isHiddenOrOverridden(root, result, member)) {
                    result.add(member)
                }
            }
            collectInherited(root, parent, result, visited)
        }
    }

    private List<TypeElement> inheritedParents(TypeElement typeElement) {
        List<TypeElement> parents = []
        TypeMirror superclass = typeElement.superclass
        if (superclass instanceof DeclaredType) {
            Element parent = ((DeclaredType) superclass).asElement()
            if (parent instanceof TypeElement && ((TypeElement) parent).qualifiedName.toString() != "java.lang.Object") {
                parents.add((TypeElement) parent)
            }
        }
        typeElement.interfaces.each { TypeMirror iface ->
            if (iface instanceof DeclaredType) {
                Element parent = ((DeclaredType) iface).asElement()
                if (parent instanceof TypeElement) {
                    parents.add((TypeElement) parent)
                }
            }
        }
        return parents
    }

    private boolean isInheritable(TypeElement root, Element member) {
        if (member.kind == ElementKind.CONSTRUCTOR) {
            return false
        }
        if (!(member.kind in [ElementKind.FIELD, ElementKind.METHOD])) {
            return false
        }
        if (member.modifiers.contains(Modifier.PRIVATE)) {
            return false
        }
        if (member.kind == ElementKind.METHOD && member.modifiers.contains(Modifier.STATIC)) {
            return false
        }
        if (member.modifiers.contains(Modifier.PUBLIC) || member.modifiers.contains(Modifier.PROTECTED)) {
            return true
        }
        return samePackage(root, member)
    }

    private boolean isHiddenOrOverridden(TypeElement root, List<Element> existingMembers, Element inheritedMember) {
        if (inheritedMember.kind == ElementKind.FIELD) {
            return existingMembers.any { Element existing ->
                existing.kind == ElementKind.FIELD && existing.simpleName.toString() == inheritedMember.simpleName.toString()
            }
        }
        if (inheritedMember.kind == ElementKind.METHOD) {
            ExecutableElement inheritedMethod = (ExecutableElement) inheritedMember
            return existingMembers.any { Element existing ->
                if (existing.kind != ElementKind.METHOD) {
                    return false
                }
                ExecutableElement existingMethod = (ExecutableElement) existing
                if (existingMethod.simpleName.toString() != inheritedMethod.simpleName.toString()) {
                    return false
                }
                if (elements.overrides(existingMethod, inheritedMethod, root)) {
                    return true
                }
                return erasedSignature(existingMethod) == erasedSignature(inheritedMethod)
            }
        }
        return false
    }

    private boolean samePackage(TypeElement root, Element member) {
        TypeElement owner = enclosingType(member)
        return owner != null && elements.getPackageOf(root) == elements.getPackageOf(owner)
    }

    private static TypeElement enclosingType(Element element) {
        Element cursor = element
        while (cursor != null && !(cursor instanceof TypeElement)) {
            cursor = cursor.enclosingElement
        }
        return cursor instanceof TypeElement ? (TypeElement) cursor : null
    }

    private String erasedSignature(ExecutableElement executable) {
        String params = executable.parameters.collect { parameter ->
            types.erasure(parameter.asType()).toString()
        }.join(",")
        return "${executable.simpleName}(${params})"
    }
}

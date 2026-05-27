package com.byd.apidoc.parser.javadoc.converter

import com.byd.apidoc.constants.DocTags
import com.sun.source.doctree.DeprecatedTree
import com.sun.source.doctree.DocCommentTree
import com.sun.source.doctree.DocTree
import com.sun.source.doctree.LinkTree
import com.sun.source.doctree.LiteralTree
import com.sun.source.doctree.ParamTree
import com.sun.source.doctree.ReferenceTree
import com.sun.source.doctree.ReturnTree
import com.sun.source.doctree.SeeTree
import com.sun.source.doctree.SinceTree
import com.sun.source.doctree.TextTree
import com.sun.source.doctree.ThrowsTree
import com.sun.source.doctree.UnknownBlockTagTree
import com.sun.source.util.DocTrees
import com.sun.source.util.SimpleDocTreeVisitor

import javax.lang.model.element.Element

class DocTreeExtractor {
    private final DocTrees docTrees

    DocTreeExtractor(DocTrees docTrees) {
        this.docTrees = docTrees
    }

    String description(Element element) {
        DocCommentTree tree = docTrees.getDocCommentTree(element)
        return tree ? render(tree.fullBody).trim() : ""
    }

    Map<String, String> parameterComments(Element element) {
        Map<String, String> comments = [:]
        DocCommentTree tree = docTrees.getDocCommentTree(element)
        tree?.blockTags?.each { DocTree tag ->
            if (tag instanceof ParamTree) {
                ParamTree param = (ParamTree) tag
                comments[param.name.toString()] = render(param.description).trim()
            }
        }
        return comments
    }

    String returnComment(Element element) {
        DocCommentTree tree = docTrees.getDocCommentTree(element)
        DocTree tag = tree?.blockTags?.find { it instanceof ReturnTree }
        return tag ? render(((ReturnTree) tag).description).trim() : ""
    }

    Map<String, String> throwsComments(Element element) {
        Map<String, String> comments = [:]
        DocCommentTree tree = docTrees.getDocCommentTree(element)
        tree?.blockTags?.each { DocTree tag ->
            if (tag instanceof ThrowsTree) {
                ThrowsTree throwsTree = (ThrowsTree) tag
                comments[throwsTree.exceptionName.toString()] = render(throwsTree.description).trim()
            }
        }
        return comments
    }

    List<String> tagValues(Element element) {
        List<String> values = []
        DocCommentTree tree = docTrees.getDocCommentTree(element)
        tree?.blockTags?.each { DocTree tag ->
            if (tag instanceof UnknownBlockTagTree && ((UnknownBlockTagTree) tag).tagName == DocTags.TAG) {
                String value = render(((UnknownBlockTagTree) tag).content).trim()
                if (value) {
                    values.add(value)
                }
            }
        }
        return values
    }

    Map<String, String> searchableTags(Element element) {
        Map<String, String> values = [:]
        DocCommentTree tree = docTrees.getDocCommentTree(element)
        tree?.blockTags?.each { DocTree tag ->
            if (tag instanceof SinceTree) {
                values[DocTags.SINCE] = render(((SinceTree) tag).body).trim()
            } else if (tag instanceof DeprecatedTree) {
                values[DocTags.DEPRECATED] = render(((DeprecatedTree) tag).body).trim()
            } else if (tag instanceof SeeTree) {
                values["see"] = renderSeeReference((SeeTree) tag).trim()
            }
        }
        return values
    }

    static String treeText(List<? extends DocTree> trees) {
        return render(trees)
    }

    private String renderSeeReference(SeeTree tree) {
        if (tree == null || !tree.reference) {
            return ""
        }

        DocTree first = tree.reference[0]
        if (first instanceof ReferenceTree) {
            return normalizeSignature(((ReferenceTree) first).signature)
        }
        return render(tree.reference)
    }

    static String render(List<? extends DocTree> trees) {
        if (!trees) {
            return ""
        }
        return trees.collect { DocTree tree -> render(tree) }
                .findAll { it != null && !it.isEmpty() }
                .join(" ")
                .replaceAll(/\s+/, " ")
                .trim()
    }

    static String render(DocTree tree) {
        if (tree == null) {
            return ""
        }
        return new RenderingVisitor().visit(tree, null) ?: ""
    }

    static String normalizeSignature(String sig) {
        if (sig == null
                || (!sig.contains(" ") && !sig.contains("\n")
                && !sig.contains("\r") && !sig.endsWith("/"))) {
            return sig
        }

        StringBuilder sb = new StringBuilder()
        char lastChar = 0
        for (int i = 0; i < sig.length(); i++) {
            char ch = sig.charAt(i)
            switch (ch) {
                case '\n':
                case '\r':
                case '\f':
                case '\t':
                case ' ':
                    switch (lastChar) {
                        case 0:
                        case '(':
                        case '<':
                        case ' ':
                        case '.':
                            break
                        default:
                            sb.append(' ')
                            lastChar = ' '
                            break
                    }
                    break
                case ',':
                case '>':
                case ')':
                case '.':
                    if (lastChar == ' ') {
                        sb.setLength(sb.length() - 1)
                    }
                    sb.append(ch)
                    lastChar = ch
                    break
                default:
                    sb.append(ch)
                    lastChar = ch
                    break
            }
        }
        if (lastChar == '/') {
            sb.setLength(sb.length() - 1)
        }
        return sb.toString()
    }

    private static class RenderingVisitor extends SimpleDocTreeVisitor<String, Void> {
        @Override
        String visitText(TextTree node, Void unused) {
            return node.body
        }

        @Override
        String visitLiteral(LiteralTree node, Void unused) {
            String body = node.body?.body ?: ""
            return node.kind == DocTree.Kind.CODE ? "{@code ${body}}" : body
        }

        @Override
        String visitLink(LinkTree node, Void unused) {
            String reference = normalizeSignature(node.reference?.signature)
            String label = render(node.label)
            if (!reference) {
                return label ?: ""
            }
            String tagName = node.kind == DocTree.Kind.LINK_PLAIN ? "linkplain" : "link"
            return label ? "{@${tagName} ${reference} ${label}}" : "{@${tagName} ${reference}}"
        }

        @Override
        String visitSee(SeeTree node, Void unused) {
            return render(node.reference)
        }

        @Override
        protected String defaultAction(DocTree node, Void unused) {
            return node.toString()
        }
    }
}

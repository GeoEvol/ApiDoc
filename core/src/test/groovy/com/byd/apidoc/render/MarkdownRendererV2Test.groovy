package com.byd.apidoc.render

import com.byd.apidoc.comment.CommentDoc
import com.byd.apidoc.comment.CommentNode
import com.byd.apidoc.comment.CommentNodeKind
import com.byd.apidoc.comment.InheritDocResolver
import com.byd.apidoc.comment.InlineTag
import com.byd.apidoc.comment.InlineTagKind
import com.byd.apidoc.metadata.VisibilityPolicy
import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.DocCorpus
import com.byd.apidoc.model.DocId
import com.byd.apidoc.model.DocIdKind
import com.byd.apidoc.model.DocMember
import com.byd.apidoc.model.DocMemberKind
import com.byd.apidoc.model.DocPackage
import com.byd.apidoc.model.DocParameter
import com.byd.apidoc.model.DocType
import com.byd.apidoc.model.TypeRef
import com.byd.apidoc.parser.javadoc.JavadocApiParser
import com.byd.apidoc.projection.ApiStatusModel
import com.byd.apidoc.projection.BreadcrumbModel
import com.byd.apidoc.projection.DocProjection
import com.byd.apidoc.projection.MemberDetailModel
import com.byd.apidoc.projection.MemberGroupModel
import com.byd.apidoc.projection.MemberSummaryModel
import com.byd.apidoc.projection.PageKind
import com.byd.apidoc.projection.PageModel
import com.byd.apidoc.projection.ProjectionBuilder
import com.byd.apidoc.projection.TocEntryModel
import com.byd.apidoc.projection.TypePageModel
import com.byd.apidoc.reference.ReferenceResolver
import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class MarkdownRendererV2Test {

    @Test
    void rendersV2ProjectionFieldsInOfflineMarkdown() {
        File outputDir = new File("build/test-v2-markdown-renderer")
        if (outputDir.exists()) {
            outputDir.deleteDir()
        }
        DocCorpus corpus = sampleCorpus()
        new InheritDocResolver().resolve(corpus)
        new ReferenceResolver().resolve(corpus)
        DocProjection projection = new ProjectionBuilder().build(corpus, new VisibilityPolicy(includeRemoved: true))

        new MarkdownRenderer().render(new RenderContext(
                corpus: corpus,
                projection: projection,
                outputDir: outputDir,
                projectName: "Sample SDK"
        ))

        File root = new File(outputDir, "api-docs-md")
        String foo = new File(root, "reference/com.example.sdk.Foo.md").text
        assertTrue(foo.contains("[Packages](../packages.md) / [com.example.sdk](../package/com.example.sdk.md) / [Foo](com.example.sdk.Foo.md)"))
        assertTrue(foo.contains("## Contents"))
        assertTrue(foo.contains("- [Constants](#constants)"))
        assertTrue(foo.contains("## Constants"))
        assertTrue(foo.contains("### run(String value, List<? extends T> items)"))
        assertTrue(foo.contains("<a id=\"run(java.lang.String,java.util.List)\"></a>"))
        assertTrue(foo.contains("[DEFAULT_NAME](#DEFAULT_NAME)"))
        assertTrue(foo.contains("Public constant value."))
        assertTrue(foo.contains("[run(String value, List<? extends T> items)](#run(java.lang.String,java.util.List))"))
        assertTrue(foo.contains("[helper type](com.example.sdk.Bar.md#com.example.sdk.Bar)"))
        assertFalse(foo.contains("devsite.google"))
        assertFalse(foo.contains("react"))
        assertFalse(foo.contains("vue"))

        String inherited = new File(root, "reference/com.example.sdk.inheritance.DerivedService.md").text
        assertTrue(inherited.contains("## Inherited Members"))
        assertTrue(inherited.contains("### Inherited from BaseService"))
        assertTrue(inherited.contains("[start()](com.example.sdk.inheritance.BaseService.md#start()) - Starts the base service."))
        assertFalse(inherited.contains("reserved for a later version"))

        String metadata = new File(root, "reference/com.example.sdk.AndroidMetadataApi.md").text
        assertTrue(metadata.contains("## API Status"))
        assertTrue(metadata.contains("- Deprecated: use Foo instead"))
        assertTrue(metadata.contains("- Removed: removed after replacement shipped"))
        assertFalse(metadata.contains("- Deprecated\n"))
        assertFalse(metadata.contains("- Removed\n"))
        assertTrue(metadata.contains("- Pending"))
        assertTrue(metadata.contains("- Since 1.5"))
        assertTrue(metadata.contains("- API 12"))
        assertTrue(metadata.contains("- Deprecated since 13"))
        assertTrue(metadata.contains("- Removed since 14"))
        assertTrue(metadata.contains("- R Extensions 4"))
        assertTrue(metadata.contains("- sample.permission.READ"))
        assertTrue(metadata.contains("- NONNULL"))
        assertTrue(metadata.contains("- IntRange 1..10"))
    }

    @Test
    void rendersResolvedInheritDocAndRelativeMarkdownLinksFromProjectionUrls() {
        File outputDir = new File("build/test-v2-markdown-renderer-relative-links")
        if (outputDir.exists()) {
            outputDir.deleteDir()
        }
        DocCorpus corpus = inheritDocCorpus()
        new InheritDocResolver().resolve(corpus)
        DocProjection projection = new ProjectionBuilder().build(corpus, new VisibilityPolicy())
        TypePageModel child = projection.typePages.find { it.id.qualifiedName == "com.example.deep.Child" }
        child.breadcrumbs = [
                new BreadcrumbModel(label: "Packages", url: "packages.html"),
                new BreadcrumbModel(label: "com.example.deep", url: "package/com/example/deep.html"),
                new BreadcrumbModel(label: "Child", url: "reference/com/example/deep/Child.html", targetId: child.id)
        ]
        child.rightToc = [new TocEntryModel(label: "Summary", anchor: "summary")]
        child.apiStatus = new ApiStatusModel(since: "2.0")
        projection.pages.find { it.targetId?.stableKey() == child.id.stableKey() }.url = "reference/com/example/deep/Child.html"
        projection.pages.find { it.kind == PageKind.PACKAGE && it.title == "com.example.deep" }.url = "package/com/example/deep.html"

        new MarkdownRenderer().render(new RenderContext(
                corpus: corpus,
                projection: projection,
                outputDir: outputDir,
                projectName: "Nested SDK"
        ))

        File root = new File(outputDir, "api-docs-md")
        String childMarkdown = new File(root, "reference/com/example/deep/Child.md").text
        assertTrue(childMarkdown.contains("[Packages](../../../../packages.md) / [com.example.deep](../../../../package/com/example/deep.md) / [Child](Child.md)"))
        assertTrue(childMarkdown.contains("## API Status"))
        assertTrue(childMarkdown.contains("- Since 2.0"))
        assertTrue(childMarkdown.contains("Inherited body from base."))

        String packageMarkdown = new File(root, "package/com/example/deep.md").text
        assertTrue(packageMarkdown.contains("[Child](../../../reference/com/example/deep/Child.md)"))
    }

    @Test
    void escapesMemberAnchorsInHtmlAttributesAndMarkdownLinks() {
        File outputDir = new File("build/test-v2-markdown-renderer-anchors")
        if (outputDir.exists()) {
            outputDir.deleteDir()
        }
        DocId typeId = typeId("com.example.AnchorFixture")
        DocId memberId = new DocId(
                kind: DocIdKind.METHOD,
                qualifiedName: "com.example.AnchorFixture.danger",
                canonicalId: "method:com.example.AnchorFixture#danger",
                displayId: "danger()",
                anchorId: "danger\"<tag>",
                fragment: "danger\"<tag>"
        )
        TypePageModel typePage = new TypePageModel(
                id: typeId,
                title: "AnchorFixture",
                packageName: "com.example",
                memberGroups: [new MemberGroupModel(title: "Methods", members: [
                        new MemberSummaryModel(id: memberId, name: "danger", displayName: "danger()")
                ])],
                memberDetails: [new MemberDetailModel(id: memberId, name: "danger", displayName: "danger()")]
        )
        DocProjection projection = new DocProjection(
                pages: [new PageModel(kind: PageKind.TYPE, title: "AnchorFixture", url: "reference/com.example.AnchorFixture.html", targetId: typeId)],
                typePages: [typePage]
        )

        new MarkdownRenderer().render(new RenderContext(
                projection: projection,
                outputDir: outputDir,
                projectName: "Anchor SDK"
        ))

        String markdown = new File(outputDir, "api-docs-md/reference/com.example.AnchorFixture.md").text
        assertTrue(markdown.contains("[danger()](#danger%22%3Ctag%3E)"))
        assertTrue(markdown.contains("<a id=\"danger&quot;&lt;tag&gt;\"></a>"))
        assertFalse(markdown.contains("<a id=\"danger\"<tag>\"></a>"))
    }

    private static DocCorpus sampleCorpus() {
        File sourceRoot = new File("src/test/resources/sample-sdk/src/main/java")
        return new JavadocApiParser().parseCorpus([sourceRoot.absolutePath], new ApiConfig())
    }

    private static DocCorpus inheritDocCorpus() {
        DocId packageId = new DocId(kind: DocIdKind.PACKAGE, qualifiedName: "com.example.deep", canonicalId: "package:com.example.deep")
        DocId baseTypeId = typeId("com.example.deep.Base")
        DocId childTypeId = typeId("com.example.deep.Child")
        DocId baseRunId = methodId("com.example.deep.Base", "run(java.lang.String)")
        DocId childRunId = methodId("com.example.deep.Child", "run(java.lang.String)")
        new DocCorpus(
                packages: [new DocPackage(id: packageId, name: "com.example.deep")],
                types: [
                        new DocType(id: baseTypeId, name: "Base", qualifiedName: "com.example.deep.Base", packageName: "com.example.deep", memberIds: [baseRunId]),
                        new DocType(id: childTypeId, name: "Child", qualifiedName: "com.example.deep.Child", packageName: "com.example.deep", superType: typeRef("com.example.deep.Base"), memberIds: [childRunId])
                ],
                members: [
                        new DocMember(id: baseRunId, ownerId: baseTypeId, name: "run", kind: DocMemberKind.METHOD, parameters: [parameter("value")], comment: comment("Base summary.", "Inherited body from base.")),
                        new DocMember(id: childRunId, ownerId: childTypeId, name: "run", kind: DocMemberKind.METHOD, parameters: [parameter("value")], comment: inheritComment())
                ]
        )
    }

    private static CommentDoc comment(String summary, String body) {
        new CommentDoc(
                summaryNodes: summary ? [new CommentNode(kind: CommentNodeKind.TEXT, text: summary)] : [],
                bodyNodes: body ? [new CommentNode(kind: CommentNodeKind.TEXT, text: body)] : []
        )
    }

    private static CommentDoc inheritComment() {
        new CommentDoc(
                summaryNodes: [new CommentNode(kind: CommentNodeKind.TEXT, text: "Child summary.")],
                bodyNodes: [new CommentNode(
                        kind: CommentNodeKind.INLINE_TAG,
                        text: "{@inheritDoc}",
                        inlineTag: new InlineTag(kind: InlineTagKind.INHERIT_DOC, name: "inheritDoc", rawName: "{@inheritDoc}", rawText: "{@inheritDoc}", known: true)
                )]
        )
    }

    private static DocParameter parameter(String name) {
        new DocParameter(name: name, type: typeRef("java.lang.String"))
    }

    private static DocId typeId(String qualifiedName) {
        new DocId(kind: DocIdKind.TYPE, qualifiedName: qualifiedName, canonicalId: "type:${qualifiedName}")
    }

    private static DocId methodId(String ownerQualifiedName, String signature) {
        String name = signature.substring(0, signature.indexOf("("))
        new DocId(
                kind: DocIdKind.METHOD,
                qualifiedName: "${ownerQualifiedName}.${name}",
                canonicalId: "method:${ownerQualifiedName}#${signature}",
                displayId: signature,
                anchorId: signature,
                signature: signature,
                fragment: signature
        )
    }

    private static TypeRef typeRef(String qualifiedName) {
        new TypeRef(qualifiedName: qualifiedName, displayName: qualifiedName, simpleName: qualifiedName.tokenize(".").last())
    }
}

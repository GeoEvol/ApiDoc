package com.byd.apidoc.render

import com.byd.apidoc.metadata.VisibilityPolicy
import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.DocId
import com.byd.apidoc.model.DocIdKind
import com.byd.apidoc.model.DocCorpus
import com.byd.apidoc.projection.ApiStatusModel
import com.byd.apidoc.projection.BreadcrumbModel
import com.byd.apidoc.parser.javadoc.JavadocApiParser
import com.byd.apidoc.projection.DocProjection
import com.byd.apidoc.projection.MemberDetailModel
import com.byd.apidoc.projection.MemberGroupModel
import com.byd.apidoc.projection.MemberSummaryModel
import com.byd.apidoc.projection.NavNode
import com.byd.apidoc.projection.NavNodeKind
import com.byd.apidoc.projection.PageKind
import com.byd.apidoc.projection.PageModel
import com.byd.apidoc.projection.ProjectionBuilder
import com.byd.apidoc.projection.TocEntryModel
import com.byd.apidoc.projection.TypePageModel
import com.byd.apidoc.reference.ReferenceResolver
import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class HtmlDevsiteRendererTest {

    @Test
    void rendersDevsiteInspiredStaticTypePageShellFromProjection() {
        File outputDir = new File("build/test-devsite-html-renderer")
        if (outputDir.exists()) {
            outputDir.deleteDir()
        }
        DocCorpus corpus = sampleCorpus()
        new ReferenceResolver().resolve(corpus)
        DocProjection projection = new ProjectionBuilder().build(
                corpus,
                new VisibilityPolicy(includeRemoved: true)
        )

        new BuiltinHtmlRenderer().render(new RenderContext(
                corpus: corpus,
                projection: projection,
                outputDir: outputDir,
                projectName: "Sample SDK"
        ))

        File root = new File(outputDir, "api-docs-html")
        File foo = new File(root, "reference/com.example.sdk.Foo.html")
        assertTrue(foo.exists())
        String text = foo.text

        assertTrue(text.contains("class=\"ad-devsite-topbar\""))
        assertTrue(text.contains("class=\"ad-devsite-shell\""))
        assertTrue(text.contains("class=\"ad-devsite-book-nav\""))
        assertTrue(text.contains("class=\"ad-nav-filter\""))
        assertTrue(text.contains("class=\"ad-devsite-content\""))
        assertTrue(text.contains("class=\"ad-devsite-toc\""))
        assertTrue(text.contains("<article class=\"ad-api-article\">"))
        assertTrue(text.contains("class=\"ad-breadcrumbs\""))
        assertTrue(text.contains("class=\"ad-api-header\""))
        assertTrue(text.contains("class=\"ad-api-status\""))
        assertTrue(text.contains("class=\"ad-member-summary\""))
        assertTrue(text.contains("class=\"ad-member-detail\""))
        assertTrue(text.contains("id=\"inherited-members\""))

        assertTrue(text.contains("Packages"))
        assertTrue(text.contains("com.example.sdk"))
        assertTrue(text.contains("<h1>Foo</h1>"))
        assertTrue(text.contains("Since 1.0"))
        assertTrue(text.contains("API 3"))
        assertTrue(text.contains("Constants"))
        assertTrue(text.contains("Constructors"))
        assertTrue(text.contains("Methods"))
        assertTrue(text.contains("href=\"#constants\""))
        assertTrue(text.contains("href=\"#details\""))
        assertTrue(text.contains("DEFAULT_NAME"))
        assertTrue(text.contains("run(String value, List&lt;? extends T&gt; items)"))
        assertTrue(text.contains("Modifier and Type"))
        assertTrue(text.contains("ad-signature-card"))
        assertTrue(text.contains("ad-copy-code"))
        assertTrue(text.contains("../assets/icon/package.svg"))
        assertTrue(text.contains("../assets/icon/class.svg"))
        assertTrue(text.contains("public static final String DEFAULT_NAME"))
        assertTrue(text.contains("throws IllegalArgumentException"))
        assertFalse(text.contains("docs.oracle.com"))
        assertFalse(text.contains("java/lang/String.html"))
        assertFalse(text.contains("java/lang/IllegalArgumentException.html"))

        File derived = new File(root, "reference/com.example.sdk.inheritance.DerivedService.html")
        assertTrue(derived.exists())
        String inheritedText = derived.text
        assertTrue(inheritedText.contains("Inherited from BaseService"))
        assertTrue(inheritedText.contains("start"))

        File metadata = new File(root, "reference/com.example.sdk.AndroidMetadataApi.html")
        assertTrue(metadata.exists())
        String metadataText = metadata.text
        assertTrue(metadataText.contains("Deprecated"))
        assertTrue(metadataText.contains("Deprecated: use Foo instead"))
        assertTrue(metadataText.contains("Removed"))
        assertTrue(metadataText.contains("Removed: removed after replacement shipped"))
        assertTrue(metadataText.contains("Pending"))
        assertTrue(metadataText.contains("Since 1.5"))
        assertTrue(metadataText.contains("API 12"))
        assertTrue(metadataText.contains("Deprecated since 13"))
        assertTrue(metadataText.contains("Removed since 14"))
        assertTrue(metadataText.contains("R Extensions 4"))
        assertTrue(metadataText.contains("sample.permission.READ"))
        assertTrue(metadataText.contains("NonNull"))
        assertTrue(metadataText.contains("IntRange 1..10"))

        File css = new File(root, "assets/apidoc-devsite.css")
        File js = new File(root, "assets/apidoc-devsite.js")
        assertTrue(css.exists())
        assertTrue(js.exists())
        assertTrue(new File(root, "assets/icon/package.svg").exists())
        assertTrue(new File(root, "assets/icon/copy.svg").exists())
        assertTrue(new File(root, "assets/icon/checked.svg").exists())
        assertTrue(css.text.contains("@media"))
        assertTrue(css.text.contains(".ad-devsite-shell"))
        assertTrue(css.text.contains(".ad-signature-card"))
        assertTrue(js.text.contains("ad-devsite-nav-toggle"))
        assertTrue(js.text.contains("ad-nav-filter"))
        assertTrue(js.text.contains("ad-copy-code"))
        assertTrue(text.contains("../assets/apidoc-devsite.css"))
        assertTrue(text.contains("../assets/apidoc-devsite.js"))
        assertFalse(text.contains("devsite.google"))
        assertFalse(text.contains("unpkg.com"))
        assertFalse(text.contains("cdn.jsdelivr.net"))
        assertFalse(text.contains("react"))
        assertFalse(text.contains("vue"))
        assertFalse(css.text.contains("https://"))
        assertFalse(js.text.contains("https://"))
    }

    @Test
    void rendersRelativeLinksForNestedProjectionUrls() {
        File outputDir = new File("build/test-devsite-html-renderer-nested")
        if (outputDir.exists()) {
            outputDir.deleteDir()
        }
        DocId packageId = new DocId(kind: DocIdKind.PACKAGE, qualifiedName: "com.example.sdk")
        DocId typeId = new DocId(kind: DocIdKind.TYPE, qualifiedName: "com.example.sdk.Foo")
        DocProjection projection = new DocProjection(
                pages: [
                        new PageModel(kind: PageKind.PACKAGE, title: "com.example.sdk", url: "package/com/example/sdk.html", targetId: packageId),
                        new PageModel(kind: PageKind.TYPE, title: "Foo", url: "reference/com/example/sdk/Foo.html", targetId: typeId)
                ],
                nav: [
                        new NavNode(label: "com.example.sdk", kind: NavNodeKind.PACKAGE, url: "package/com/example/sdk.html", targetId: packageId, children: [
                                new NavNode(label: "Classes", kind: NavNodeKind.GROUP, children: [
                                        new NavNode(label: "Foo", kind: NavNodeKind.TYPE, url: "reference/com/example/sdk/Foo.html", targetId: typeId)
                                ])
                        ])
                ],
                typePages: [
                        new TypePageModel(
                                id: typeId,
                                title: "Foo",
                                packageName: "com.example.sdk",
                                summary: "Nested URL sample",
                                breadcrumbs: [
                                        new BreadcrumbModel(label: "Packages", url: "packages.html"),
                                        new BreadcrumbModel(label: "com.example.sdk", url: "package/com/example/sdk.html"),
                                        new BreadcrumbModel(label: "Foo", url: "reference/com/example/sdk/Foo.html", targetId: typeId)
                                ],
                                rightToc: [new TocEntryModel(label: "Summary", anchor: "summary")],
                                apiStatus: new ApiStatusModel(since: "2.0")
                        )
                ]
        )

        new BuiltinHtmlRenderer().render(new RenderContext(
                projection: projection,
                outputDir: outputDir,
                projectName: "Nested SDK"
        ))

        File root = new File(outputDir, "api-docs-html")
        String typeHtml = new File(root, "reference/com/example/sdk/Foo.html").text
        assertTrue(typeHtml.contains("href=\"../../../../assets/apidoc-devsite.css\""))
        assertTrue(typeHtml.contains("src=\"../../../../assets/search.js\" data-root-prefix=\"../../../../\""))
        assertTrue(typeHtml.contains("href=\"../../../../package/com/example/sdk.html\""))
        assertTrue(typeHtml.contains("href=\"Foo.html\""))

        String packageHtml = new File(root, "package/com/example/sdk.html").text
        assertTrue(packageHtml.contains("href=\"../../../reference/com/example/sdk/Foo.html\""))
        assertTrue(packageHtml.contains("href=\"../../../assets/apidoc-devsite.css\""))
    }

    @Test
    void rendersStatusOnlyFromProjectionStatusModels() {
        File outputDir = new File("build/test-devsite-html-renderer-status-source")
        if (outputDir.exists()) {
            outputDir.deleteDir()
        }
        DocId typeId = new DocId(kind: DocIdKind.TYPE, qualifiedName: "com.example.sdk.StatusOnly")
        DocId memberId = new DocId(kind: DocIdKind.METHOD, qualifiedName: "com.example.sdk.StatusOnly.run", signature: "run()")
        DocProjection projection = new DocProjection(
                pages: [
                        new PageModel(kind: PageKind.TYPE, title: "StatusOnly", url: "reference/com.example.sdk.StatusOnly.html", targetId: typeId)
                ],
                typePages: [
                        new TypePageModel(
                                id: typeId,
                                title: "StatusOnly",
                                packageName: "com.example.sdk",
                                metadata: new com.byd.apidoc.metadata.ApiMetadata(since: "metadata-only"),
                                memberGroups: [
                                        new MemberGroupModel(title: "Methods", members: [
                                                new MemberSummaryModel(id: memberId, name: "run", displayName: "run()", metadata: new com.byd.apidoc.metadata.ApiMetadata(since: "member-summary-metadata-only"))
                                        ])
                                ],
                                memberDetails: [
                                        new MemberDetailModel(id: memberId, name: "run", displayName: "run()", metadata: new com.byd.apidoc.metadata.ApiMetadata(since: "member-detail-metadata-only"))
                                ]
                        )
                ]
        )

        new BuiltinHtmlRenderer().render(new RenderContext(
                projection: projection,
                outputDir: outputDir,
                projectName: "Status SDK"
        ))

        String html = new File(outputDir, "api-docs-html/reference/com.example.sdk.StatusOnly.html").text
        assertFalse(html.contains("metadata-only"))
        assertFalse(html.contains("member-summary-metadata-only"))
        assertFalse(html.contains("member-detail-metadata-only"))
    }

    private static DocCorpus sampleCorpus() {
        File sourceRoot = new File("src/test/resources/sample-sdk/src/main/java")
        return new JavadocApiParser().parseCorpus([sourceRoot.absolutePath], new ApiConfig())
    }
}

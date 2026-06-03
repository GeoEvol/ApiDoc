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
        String indexText = new File(root, "index.html").text
        assertTrue(indexText.contains("<h1>Package Index</h1>"))
        assertTrue(indexText.contains("class=\"ad-index-table ad-packages-index-table\""))
        assertTrue(indexText.contains("href=\"#packages-index\""))
        assertTrue(indexText.contains("href=\"#packages-list\""))
        assertTrue(indexText.contains("ad-book-index-link is-current"))
        assertTrue(new File(root, "packages.html").text.contains("com.example.sdk"))
        assertTrue(new File(root, "classes.html").text.contains(">Foo</a>"))
        assertFalse(new File(root, "classes.html").text.contains(">com.example.sdk.Foo</a>"))
        assertTrue(new File(root, "assets/apidoc.css").exists())
        assertTrue(new File(root, "assets/apidoc.js").exists())
        assertTrue(new File(root, "nav-index.json").exists())
        assertTrue(new File(root, "search-index.json").text.contains("com.example.sdk.Foo"))

        File searchJs = new File(root, "assets/search.js")
        assertTrue(searchJs.exists())
        assertTrue(searchJs.text.contains("ad-search-results"))
        assertTrue(searchJs.text.contains("fetch(url)"))
        assertTrue(searchJs.text.contains("getAttribute('data-root-prefix')"))
        assertFalse(searchJs.text.contains("ad-search-kind"))
        assertFalse(searchJs.text.contains("href=\"'+esc('../'+"))
        assertTrue(new File(root, "index.html").text.contains("<script src=\"assets/search.js\" data-root-prefix=\"\"></script>"))

        File foo = new File(root, "reference/com.example.sdk.Foo.html")
        assertTrue(foo.exists())
        String text = foo.text

        assertTrue(text.contains("class=\"ad-devsite-topbar\""))
        assertTrue(text.contains("class=\"ad-devsite-shell\""))
        assertTrue(text.contains("class=\"ad-devsite-book-nav\""))
        assertFalse(text.contains("ad-nav-filter"))
        assertFalse(text.contains("ad-search-kind"))
        assertTrue(text.contains("class=\"ad-search-wrap ad-nav-search-wrap\""))
        assertTrue(text.indexOf("ad-search-wrap ad-nav-search-wrap") > text.indexOf("ad-book-nav-scroll"))
        assertTrue(text.indexOf("ad-search-wrap ad-nav-search-wrap") < text.indexOf("id=\"ad-packages-root\""))
        assertTrue(text.indexOf("id=\"ad-packages-root\"") < text.indexOf("ad-platform-selector"))
        assertTrue(text.contains("class=\"ad-platform-selector\""))
        assertTrue(text.contains("class=\"ad-platform-selector-select\""))
        assertTrue(text.contains("<option value=\"all\">All Platforms</option>"))
        assertFalse(text.contains("AllPlatforms"))
        assertFalse(text.contains("Allplatforms"))
        assertTrue(text.contains("DiLink300"))
        assertTrue(text.contains("class=\"ad-book-nav-toggle\""))
        assertTrue(text.contains("class=\"ad-reference-title\">API REFERENCE"))
        assertTrue(text.contains("id=\"ad-packages-root\""))
        assertTrue(text.contains("class=\"ad-packages-root-summary ad-nav-item\""))
        assertFalse(text.contains("id=\"ad-packages-root\" open"))
        assertFalse(text.contains("class=\"ad-book-nav-restore-handle\""))
        assertFalse(text.contains("class=\"ad-book-nav-footer\""))
        assertFalse(text.contains("class=\"ad-book-nav-toggle-label\""))
        assertTrue(text.contains("aria-label=\"Hide navigation\""))
        assertTrue(text.contains("class=\"ad-devsite-content\""))
        assertTrue(text.contains("class=\"ad-devsite-toc\""))
        assertTrue(text.contains("<article class=\"ad-api-article\">"))
        assertTrue(text.contains("class=\"ad-breadcrumbs\""))
        assertTrue(text.contains("class=\"ad-api-header\""))
        assertTrue(text.contains("class=\"ad-api-status\""))
        assertTrue(text.contains("class=\"ad-member-summary\""))
        assertTrue(text.contains("class=\"ad-member-detail\""))
        assertFalse(text.contains("id=\"inherited-members\""))

        assertTrue(text.contains("Package Index"))
        assertTrue(text.contains("Class Index"))
        assertFalse(text.contains("Packages Index"))
        assertFalse(text.contains("Classes Index"))
        assertTrue(text.contains("com.example.sdk"))
        assertTrue(text.contains("<h1>Foo</h1>"))
        assertTrue(text.contains("class Foo&lt;T&gt;"))
        assertTrue(text.contains("<a href=\"com.example.sdk.ServiceContract.html#com.example.sdk.ServiceContract\">ServiceContract</a>"))
        assertTrue(text.contains("<a href=\"com.example.sdk.Bar.html#com.example.sdk.Bar\">helper type</a>"))
        assertTrue(text.contains("plain list"))
        assertTrue(text.contains("<code>code literal</code>"))
        assertTrue(text.contains("<strong>@permission:</strong> android.permission.INTERNET"))
        assertTrue(text.contains("class=\"ad-platform-badge\""))
        assertTrue(text.contains("data-platforms=\"DiLink300 DiLink300F\""))
        assertTrue(text.contains("data-platforms=\"DiLink300VCP\""))
        assertTrue(text.contains("data-platforms=\"DiLinkF_300VCP\""))
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
        assertFalse(text.contains("<th>Description</th>"))
        assertTrue(text.contains("class=\"ad-member-description\""))
        assertTrue(text.contains("class=\"ad-member-section-title\""))
        assertTrue(text.contains("class=\"ad-section-kind-icon"))
        assertFalse(text.contains("class=\"ad-member-icon\""))
        assertSummarySectionHasNoPlatformBadges(text, "Constants")
        assertSummarySectionHasNoPlatformBadges(text, "Methods")
        assertTrue(text.contains("ad-signature-card"))
        assertTrue(text.contains("ad-copy-code"))
        assertTrue(text.contains("class=\"ad-detail-section\""))
        assertTrue(text.contains("class=\"ad-detail-table\""))
        assertTrue(text.contains("<h4>Parameters</h4>"))
        assertTrue(text.contains("<td><code>value</code></td>"))
        assertTrue(text.contains("<td>value description</td>"))
        assertTrue(text.contains("<h4>Returns</h4>"))
        assertTrue(text.contains("<td><code>Map&lt;String, T&gt;</code></td>"))
        assertTrue(text.contains("<td>mapped result</td>"))
        assertTrue(text.contains("<h4>Throws</h4>"))
        assertTrue(text.contains("<td><code>IllegalArgumentException</code></td>"))
        assertTrue(text.contains("../assets/icon/class.svg"))
        assertTrue(text.contains("public static final String DEFAULT_NAME"))
        assertTrue(text.contains("throws IllegalArgumentException"))
        assertFalse(text.contains("docs.oracle.com"))
        assertFalse(text.contains("java/lang/String.html"))
        assertFalse(text.contains("java/lang/IllegalArgumentException.html"))
        assertFalse(new File(root, "reference/com.example.sdk.HiddenApi.html").exists())

        File derived = new File(root, "reference/com.example.sdk.inheritance.DerivedService.html")
        assertTrue(derived.exists())
        String inheritedText = derived.text
        assertFalse(inheritedText.contains("Inherited Members"))
        assertFalse(inheritedText.contains("Inherited from BaseService"))

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
        assertTrue(css.text.contains("--ad-link-hover"))
        assertTrue(css.text.contains(".ad-platform-badge"))
        assertTrue(css.text.contains(".is-platform-disabled"))
        assertFalse(css.text.contains(".ad-platform-hidden {\n  display: none !important;\n}"))
        assertTrue(css.text.contains(".ad-nav-collapsed"))
        assertTrue(css.text.contains(".ad-devsite-shell"))
        assertTrue(css.text.contains(".ad-signature-card"))
        assertTrue(css.text.contains(".ad-reference-title"))
        assertTrue(css.text.contains(".ad-packages-root"))
        assertFalse(css.text.contains(".ad-book-nav-restore-handle"))
        assertFalse(css.text.contains(".ad-book-nav-footer"))
        assertTrue(css.text.contains("--ad-left-nav-width: 0px"))
        assertTrue(css.text.contains("translateX(calc(-1 * var(--ad-left-nav-expanded-width)))"))
        assertTrue(css.text.contains(".ad-book-nav-toggle[aria-expanded=\"false\"]"))
        assertFalse(css.text.contains("text-overflow: ellipsis"))
        assertFalse(css.text.contains(".ad-search-kind"))
        assertFalse(css.text.contains(".ad-package-count"))
        assertFalse(css.text.contains(".ad-package-link"))
        assertTrue(js.text.contains("ad-devsite-nav-toggle"))
        assertFalse(js.text.contains("restoreHandle"))
        assertFalse(js.text.contains("ad-book-nav-restore-handle"))
        assertTrue(js.text.contains("state ? \"Show navigation\" : \"Hide navigation\""))
        assertTrue(js.text.contains("bookToggle.setAttribute(\"data-title\", label)"))
        assertTrue(js.text.contains("document.documentElement.scrollHeight"))
        assertTrue(js.text.contains("document.documentElement.scrollTop"))
        assertTrue(js.text.contains("apidoc.platform"))
        assertTrue(js.text.contains("apidoc.packagesExpanded"))
        assertFalse(js.text.contains("apidoc.navCollapsed"))
        assertTrue(js.text.contains("apidoc-search-query-change"))
        assertTrue(js.text.contains("applyNavSearch"))
        assertFalse(js.text.contains("setPlatformHidden"))
        assertFalse(js.text.contains("ad-platform-hidden"))
        assertTrue(js.text.contains("pkg.hidden ="))
        assertTrue(js.text.contains("group.hidden ="))
        assertTrue(js.text.contains("link.hidden ="))
        assertTrue(js.text.contains("is-platform-disabled"))
        assertTrue(js.text.contains("aria-disabled"))
        assertTrue(js.text.contains("tabindex"))
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

        assertTrue(text.contains("class=\"ad-book-nav-scroll\""))
        assertTrue(text.contains("class=\"ad-book-nav-toggle\""))
        assertTrue(text.contains("class=\"ad-book-nav-toggle-icon\""))
        assertTrue(text.contains("assets/icon/chevron.svg"))
        assertFalse(text.contains("assets/icon/chevron-double.svg"))
        assertFalse(text.contains("class=\"ad-package-link\""))
        assertFalse(text.contains("class=\"ad-package-count\""))
        assertTrue(text.contains("<details class=\"ad-package-group\""))
        assertTrue(text.contains("data-group-kind=\"interfaces\""))
        assertTrue(text.contains("data-group-kind=\"classes\""))
        assertTrue(text.contains("class=\"ad-group-label\" data-nav-label=\"Interfaces\">Interfaces</span>"))
        assertTrue(text.contains("class=\"ad-group-label\" data-nav-label=\"Classes\">Classes</span>"))
        assertFalse(text.contains(">INTERFACES<"))
        assertFalse(text.contains(">CLASSES<"))
        assertFalse(new File(root, "assets/icon/chevron-double.svg").exists())
        assertTrue(css.text.contains("--ad-link-hover-soft"))
        assertTrue(css.text.contains(".ad-package-group"))
        assertFalse(css.text.contains(".ad-devsite-toc a:hover,\n.ad-devsite-toc a:focus {\n  border-left-color: var(--ad-link-hover);"))

        File packagePage = new File(root, "package/com.example.sdk.html")
        assertTrue(packagePage.exists())
        String packageText = packagePage.text
        assertTrue(packageText.contains("<h1>Package com.example.sdk</h1>"))
        assertTrue(packageText.contains("class=\"ad-devsite-toc\""))
        assertTrue(packageText.contains("href=\"#overview\""))
        assertTrue(packageText.contains("href=\"#classes\""))
        assertTrue(packageText.contains("href=\"#exceptions\""))
        assertTrue(packageText.contains("Overview"))
        assertTrue(packageText.contains("Interfaces"))
        assertTrue(packageText.contains("Classes"))
        assertTrue(packageText.contains("Exceptions"))
        assertTrue(packageText.contains("SampleException"))
        assertTrue(packageText.contains("Errors"))
        assertTrue(packageText.contains("SampleError"))

        File classesPage = new File(root, "classes.html")
        assertTrue(classesPage.exists())
        String classesText = classesPage.text
        assertTrue(classesText.contains("<h1>Class Index</h1>"))
        assertTrue(classesText.contains("class=\"ad-alpha-index\""))
        assertTrue(classesText.contains("class=\"ad-index-table ad-classes-index-table\""))
        assertTrue(classesText.contains("<th>Class</th>"))
        assertTrue(classesText.contains("<th>Description</th>"))
        assertTrue(classesText.contains(">Foo</a>"))
        assertFalse(classesText.contains(">com.example.sdk.Foo</a>"))
        assertTrue(classesText.indexOf(">Bar</a>") < classesText.indexOf(">Foo</a>"))

        String packagesText = new File(root, "packages.html").text
        assertTrue(packagesText.contains("<h1>Package Index</h1>"))
        assertTrue(packagesText.contains("Browse all API packages"))
        assertTrue(packagesText.contains("class=\"ad-index-table ad-packages-index-table\""))
        assertTrue(packagesText.contains("<th>Package</th>"))
        assertTrue(packagesText.contains("<th>Description</th>"))
        assertTrue(packagesText.contains("class=\"ad-devsite-toc\""))
        assertTrue(packagesText.contains("href=\"#packages-index\""))
        assertTrue(packagesText.contains("href=\"#packages-list\""))
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
                                new NavNode(label: "Overview", kind: NavNodeKind.OVERVIEW, url: "package/com/example/sdk.html", targetId: packageId),
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

    private static void assertSummarySectionHasNoPlatformBadges(String html, String title) {
        String id = title.toLowerCase(Locale.ROOT).replaceAll(/[^a-z0-9]+/, "-").replaceAll(/^-|-$/, "")
        int start = html.indexOf("class=\"ad-member-summary\" id=\"${id}\"")
        assertTrue("Missing summary section ${title}", start >= 0)
        int end = html.indexOf("</section>", start)
        assertTrue("Missing summary section end ${title}", end > start)
        String section = html.substring(start, end)
        assertFalse("Summary section ${title} should not render platform badges", section.contains("ad-platform-badge"))
        assertTrue("Summary section ${title} should keep platform data attributes", section.contains("data-platforms="))
    }

    private static DocCorpus sampleCorpus() {
        File sourceRoot = new File("src/test/resources/sample-sdk/src/main/java")
        return new JavadocApiParser().parseCorpus([sourceRoot.absolutePath], new ApiConfig())
    }
}

# ApiDoc v2 Progress Checkpoint

**Saved At:** 2026-05-28

**Current Execution Mode:** Subagent-Driven

**Current Scope:** `docs/superpowers/plans/2026-05-27-apidoc-v2-android-devsite.md`

---

## Current Status

```text
Task 1: Resource Inventory and Design Notes        DONE
Task 2: Projection Model Enrichment                DONE
Task 3: Basic Inherited Members                    DONE
Task 4: Basic {@inheritDoc} Resolution             DONE
Task 5: Android Metadata Expansion                 DONE
Task 6: DevSite-Inspired Static HTML Renderer      DONE
Task 6A: Optional Build Web Apps Visual Pass       OPTIONAL
Task 7: Search and Navigation v2                   DONE
Task 8: Markdown Parity                            DONE
Task 9: End-to-End v2 Acceptance                   IMPLEMENTED, REVIEW PENDING
Task 10: v2 Documentation and Handoff              PENDING
```

`D:/workspace/ApiDoc` is not a git repository in this environment, so progress is tracked by changed files, tests, and reviewer results instead of commits.

---

## Completed Work

### Task 1: Resource Inventory and Design Notes

Created:

```text
docs/research/javadoc-v2-notes.md
docs/research/doclava-v2-notes.md
docs/research/devsite-v2-notes.md
docs/research/v2-decisions.md
```

Result:

```text
Spec review: approved
Quality review: approved
```

Captured:

```text
OpenJDK Javadoc member grouping, inherited filtering, inheritDoc behavior, navigation, search-index adjacent files
Doclava hide/removed/pending, apiSince/sdkExtSince, member grouping, federation, NavTree
DevSite shell, left nav, right TOC, search, tables/code, CSS variables, breakpoints
Build Web Apps optional visual pass constraints and missing-resource stop condition
```

### Task 2: Projection Model Enrichment

Changed:

```text
core/src/main/groovy/com/byd/apidoc/projection/DocProjection.groovy
core/src/main/groovy/com/byd/apidoc/projection/ProjectionBuilder.groovy
core/src/test/groovy/com/byd/apidoc/projection/ProjectionBuilderV2Test.groovy
```

Implemented:

```text
BreadcrumbModel
TocEntryModel
ApiStatusModel
AndroidTypeHeaderModel
InheritedMemberGroupModel
TypePageModel breadcrumbs/rightToc/apiStatus/typeHeader
SearchEntry status/displaySignature/tokens
ProjectionBuilder population for v2 render-neutral fields
```

Reviewer fixes:

```text
TypePageModel.inheritedMemberGroups is now List<InheritedMemberGroupModel>
ProjectionBuilder handles sparse/null collections more defensively
ProjectionBuilder avoids duplicate visible-member scans
```

Verification reported:

```text
.\gradlew.bat :core:test --tests com.byd.apidoc.projection.ProjectionBuilderV2Test
.\gradlew.bat :core:test --tests com.byd.apidoc.projection.ProjectionBuilderTest
.\gradlew.bat :core:test
```

All passed.

### Task 3: Basic Inherited Members

Changed:

```text
core/src/main/groovy/com/byd/apidoc/projection/InheritedMemberResolver.groovy
core/src/main/groovy/com/byd/apidoc/projection/ProjectionBuilder.groovy
core/src/test/groovy/com/byd/apidoc/projection/InheritedMemberResolverTest.groovy
core/src/test/resources/sample-sdk/src/main/java/com/example/sdk/inheritance/*.java
```

Implemented:

```text
DocCorpus-based inherited member resolver
Superclass and interface traversal by TypeRef.qualifiedName
Inherited fields and methods grouped by original owner
VisibilityPolicy filtering
Override suppression by erased method signature
Inherited links targeting original owner page and member anchor
ProjectionBuilder populates TypePageModel.inheritedMemberGroups
```

Reviewer fixes:

```text
Hidden/removed owner types no longer incorrectly suppress visible ancestor members
Added hidden owner traversal test
Added cyclic type reference test
```

Verification reported:

```text
.\gradlew.bat :core:test --tests com.byd.apidoc.projection.InheritedMemberResolverTest
.\gradlew.bat :core:test --tests com.byd.apidoc.projection.ProjectionBuilderTest --tests com.byd.apidoc.projection.ProjectionBuilderV2Test
.\gradlew.bat :core:test
```

All passed.

### Task 4: Basic `{@inheritDoc}` Resolution

Changed:

```text
core/src/main/groovy/com/byd/apidoc/comment/CommentDoc.groovy
core/src/main/groovy/com/byd/apidoc/comment/InheritDocResolver.groovy
core/src/main/groovy/com/byd/apidoc/parser/javadoc/v1/DocCorpusBuilder.groovy
core/src/main/groovy/com/byd/apidoc/pipeline/RenderPipelineCoordinator.groovy
core/src/test/groovy/com/byd/apidoc/comment/InheritDocResolverTest.groovy
```

Implemented:

```text
InlineTagKind.INHERIT_DOC
Public Javadoc InheritDocTree parsing
Method-level inheritDoc resolution across superclass/interface methods
Summary/body inheritDoc replacement
@param/@return/@throws inheritDoc replacement by block key
inheritDoc.unresolved diagnostics without crashing
Pipeline integration after ReferenceResolver and before JSON/projection/rendering
```

Reviewer fixes:

```text
Added test coverage for actual InlineTagKind.INHERIT_DOC
Added transitive superclass traversal coverage
Resolved inherited comments recursively before copying from them
Added active-member tracking to avoid cycles
Deep-copied LinkRef inside inherited inline tags
Hardened sparse/null diagnostics and comment maps
```

Verification reported:

```text
.\gradlew.bat :core:test --tests com.byd.apidoc.comment.InheritDocResolverTest
.\gradlew.bat :core:test --tests com.byd.apidoc.render.MarkdownRendererTest --tests com.byd.apidoc.render.BuiltinHtmlRendererTest
.\gradlew.bat :core:test --tests com.byd.apidoc.pipeline.RenderPipelineCoordinatorV1JsonTest --tests com.byd.apidoc.output.DocCorpusWriterTest --tests com.byd.apidoc.output.ProjectionWriterTest
.\gradlew.bat :core:test
```

All passed.

### Task 5: Android Metadata Expansion

Changed:

```text
core/src/main/groovy/com/byd/apidoc/metadata/ApiMetadata.groovy
core/src/main/groovy/com/byd/apidoc/parser/javadoc/v1/DocCorpusBuilder.groovy
core/src/main/groovy/com/byd/apidoc/projection/DocProjection.groovy
core/src/main/groovy/com/byd/apidoc/projection/ProjectionBuilder.groovy
core/src/test/groovy/com/byd/apidoc/metadata/ApiMetadataBuilderV2Test.groovy
core/src/test/resources/sample-sdk/src/main/java/com/example/sdk/AndroidMetadataApi.java
core/src/test/resources/sample-sdk/src/main/java/com/example/sdk/annotations/RequiresPermission.java
core/src/test/resources/sample-sdk/src/main/java/com/example/sdk/annotations/IntRange.java
core/src/test/resources/sample-sdk/src/main/java/com/example/sdk/annotations/FloatRange.java
core/src/test/resources/sample-sdk/src/main/java/com/example/sdk/annotations/NonNull.java
core/src/test/resources/sample-sdk/src/main/java/com/example/sdk/annotations/Nullable.java
```

Implemented:

```text
ApiMetadata.pending
ApiMetadata.sdkExtensionSince
ApiMetadata.permissions
ApiMetadata.nullability
ApiMetadata.valueRanges
ApiValueRange
Android-style tag extraction for @pending, @apiSince, @sdkExtSince, @removed, @deprecated
Android-style annotation extraction for RequiresPermission, IntRange, FloatRange, NonNull, Nullable
ApiStatusModel expanded metadata projection
```

Reviewer fixes:

```text
Tightened raw block tag preservation tests for @apiSince and @deprecated
Asserted type-level @Deprecated annotation preservation
Fixed projection status collection aliasing for permissions/valueRanges
Added allOf/anyOf permission array fixtures and renderer-mutation isolation coverage
```

Verification reported:

```text
.\gradlew.bat :core:test --tests com.byd.apidoc.metadata.ApiMetadataBuilderV2Test
.\gradlew.bat :core:test --tests com.byd.apidoc.projection.ProjectionBuilderV2Test
.\gradlew.bat :core:test --tests com.byd.apidoc.parser.javadoc.V1DocCorpusParserTest --tests com.byd.apidoc.fixture.SampleSdkFixtureTest
.\gradlew.bat :core:test
```

All passed.

### Task 6: DevSite-Inspired Static HTML Renderer

Changed:

```text
core/src/main/groovy/com/byd/apidoc/render/BuiltinHtmlRenderer.groovy
core/src/main/groovy/com/byd/apidoc/render/html/HtmlSiteRenderer.groovy
core/src/main/groovy/com/byd/apidoc/render/html/HtmlAssetWriter.groovy
core/src/main/groovy/com/byd/apidoc/render/html/HtmlPageShellRenderer.groovy
core/src/main/resources/apidoc-v2/assets/apidoc-devsite.css
core/src/main/resources/apidoc-v2/assets/apidoc-devsite.js
core/src/main/resources/apidoc-v2/assets/apidoc-search.js
core/src/test/groovy/com/byd/apidoc/render/HtmlDevsiteRendererTest.groovy
```

Implemented:

```text
BuiltinHtmlRenderer delegates to HtmlSiteRenderer
DevSite-inspired static topbar/book-nav/content/right-TOC shell
Type pages render breadcrumbs, right TOC, API status, type header, member summaries/details, inherited groups
CSS/JS moved into resources and copied into api-docs-html/assets/
Search JS moved into apidoc-search.js static resource
```

Reviewer fixes:

```text
Renderer now consumes projection status models instead of recalculating API status from raw metadata
Nested page relative links are computed by page depth with LinkPathResolver
INTERNAL visibility is treated as hidden in status display
Expanded status messages are rendered from ApiStatusModel
No CDN/SPA/framework import assertions added
```

Verification reported:

```text
.\gradlew.bat :core:test --tests com.byd.apidoc.render.HtmlDevsiteRendererTest
.\gradlew.bat :core:test --tests com.byd.apidoc.render.BuiltinHtmlRendererTest --tests com.byd.apidoc.pipeline.RenderPipelineCoordinatorV1JsonTest
.\gradlew.bat :core:test
```

All passed.

### Task 7: Search and Navigation v2

Changed:

```text
core/src/main/groovy/com/byd/apidoc/projection/DocProjection.groovy
core/src/main/groovy/com/byd/apidoc/projection/ProjectionBuilder.groovy
core/src/main/resources/apidoc-v2/assets/apidoc-search.js
core/src/test/groovy/com/byd/apidoc/projection/SearchAndNavV2Test.groovy
```

Implemented:

```text
NavNode.activePath
NavNode.group
package search entries
improved tokens/display signatures for package/type/member search
local static search query filtering
kind filtering
keyboard navigation
result links with url + anchor
```

Reviewer fixes:

```text
Search URL prefixing now preserves absolute/protocol/root/hash URLs
Keyboard navigation works from input and results panel
aria-expanded state added
Non-OK search-index fetch degrades to empty index
Search token generation includes camel-case split and is capped at 32 tokens
```

Verification reported:

```text
.\gradlew.bat :core:test --tests com.byd.apidoc.projection.SearchAndNavV2Test
.\gradlew.bat :core:test --tests com.byd.apidoc.projection.ProjectionBuilderV2Test --tests com.byd.apidoc.render.HtmlDevsiteRendererTest
.\gradlew.bat :core:test
```

All passed.

### Task 8: Markdown Parity

Changed:

```text
core/src/main/groovy/com/byd/apidoc/render/MarkdownRenderer.groovy
core/src/test/groovy/com/byd/apidoc/render/MarkdownRendererV2Test.groovy
```

Implemented:

```text
Markdown renders breadcrumbs
Markdown renders API status blocks
Markdown renders inherited members
Markdown renders resolved inheritDoc output
Markdown renders Android metadata text
Markdown uses stable anchors and relative Markdown links
```

Reviewer fixes:

```text
Removed duplicate Deprecated/Removed status bullets
Escaped generated member anchor hrefs and HTML id attributes
Added focused tests for status de-duplication and safe anchors
```

Verification reported:

```text
.\gradlew.bat :core:test --tests com.byd.apidoc.render.MarkdownRendererV2Test
.\gradlew.bat :core:test --tests com.byd.apidoc.render.MarkdownRendererTest
.\gradlew.bat :core:test --tests com.byd.apidoc.comment.InheritDocResolverTest
.\gradlew.bat :core:test
```

All passed.

### Task 9: End-to-End v2 Acceptance

Current status:

```text
Implementation done by worker.
Spec review pending.
Quality review pending.
```

Changed:

```text
core/src/test/groovy/com/byd/apidoc/plugin/ApiDocPluginV2EndToEndTest.groovy
```

Implemented:

```text
Gradle TestKit v2 end-to-end acceptance test over sample-sdk
Runs generateMarkdown and generateHtml
Asserts DocCorpus retains hidden/removed APIs
Asserts projection/search/page outputs filter hidden/removed APIs under public policy
Asserts DevSite-inspired HTML shell, right TOC, inherited member groups, and offline search assets
Asserts root and nested search structure has correct data-root-prefix, copied assets, search JSON, and resolvable generated links
Asserts Markdown includes inherited docs and public/deprecated metadata
Asserts output-manifest.json keeps compatible schema/paths
```

Worker verification reported:

```text
.\gradlew.bat :core:test --tests com.byd.apidoc.plugin.ApiDocPluginV2EndToEndTest --rerun-tasks
.\gradlew.bat :core:test --rerun-tasks
```

Full suite reported:

```text
74 tests, 0 failures, 0 errors, 0 skipped
```

---

## Next Task

Detailed execution plan saved:

```text
docs/superpowers/plans/2026-05-28-apidoc-v2-task9-10-finalization-and-frontend-design.md
```

Resume with:

```text
Task 9: End-to-End v2 Acceptance spec review, then code quality review
```

Primary file:

```text
core/src/test/groovy/com/byd/apidoc/plugin/ApiDocPluginV2EndToEndTest.groovy
```

After Task 9 reviews pass, continue with:

```text
Build Web Apps visual design pass for the offline HTML API reference frontend
Task 10: v2 Documentation and Handoff
```

Important cautions:

```text
Task 9 is not considered fully complete until spec review and code quality review are approved.
Do not mark Task 10 started until Task 9 reviews pass and frontend design pass is either completed or explicitly blocked with missing resources.
Workspace is not a git repository in this environment.
```

---

## 2026-05-28 Build Web Apps Frontend Pass Progress

Current status:

```text
Task 9: End-to-End v2 Acceptance implementation verified locally, review still needs formal closeout
Build Web Apps visual design pass: IMPLEMENTED, SCREENSHOT QA COMPLETED
Task 10: Documentation draft implemented
```

Resources confirmed:

```text
C:/Users/Evol/Desktop/devsite/google/AdIdManager.html
C:/Users/Evol/Desktop/devsite/google/app.css
C:/Users/Evol/Desktop/devsite/google/devsite_devsite_search_module__zh_cn.js
C:/Users/Evol/Desktop/devsite/google/devsite_devsite_book_nav_module__zh_cn.js
C:/Users/Evol/Desktop/devsite/google/devsite_devsite_toc_module__zh_cn.js
D:/workspace/test/build/modern-javadoc.css
D:/workspace/test/build/modern-javadoc.js
D:/workspace/JDK/jdk/modern-javadoc.css
D:/workspace/JDK/jdk/modern-javadoc.js
```

Changed:

```text
docs/v2-frontend-design.md
docs/v2-output-contract.md
docs/v2-android-devsite-target.md
docs/v2-visual-qa.md
docs/portable-usage.md
core/src/main/resources/apidoc-v2/assets/apidoc-devsite.css
core/src/main/resources/apidoc-v2/assets/apidoc-devsite.js
core/src/main/groovy/com/byd/apidoc/render/html/HtmlSiteRenderer.groovy
```

Implemented:

```text
DevSite-inspired CSS token system
desktop three-column API reference layout refinement
mobile nav/search/content responsive behavior
semantic API status chip classes
mobile status chip vertical flow to avoid clipping
search/topbar styling refinement
right TOC active-section highlighting
Ctrl/Cmd+K search focus
mobile nav closes after link click and Escape
frontend design and output contract documentation
visual QA documentation with screenshot paths
```

Screenshots:

```text
D:/workspace/ApiDoc/build/visual-qa/apidoc-v2-desktop-2.png
D:/workspace/ApiDoc/build/visual-qa/apidoc-v2-mobile-5.png
D:/workspace/ApiDoc/build/visual-qa/devsite-reference-desktop.png
D:/workspace/ApiDoc/build/visual-qa/apidoc-v2-search-open-http.png
```

Verification run:

```text
.\gradlew.bat :core:test --tests com.byd.apidoc.plugin.ApiDocPluginV2EndToEndTest --rerun-tasks
.\gradlew.bat :core:test --tests com.byd.apidoc.render.HtmlDevsiteRendererTest --rerun-tasks
.\gradlew.bat :core:test --tests com.byd.apidoc.render.MarkdownRendererV2Test --rerun-tasks --stacktrace
.\gradlew.bat :core:test --rerun-tasks
```

Notes:

```text
Do not run multiple .\gradlew.bat :core:test invocations in parallel. It causes test-results output directory contention and misleading NoClassDefFoundError/delete failures.
Search open-state was captured through a temporary local HTTP server because file:// cannot reliably fetch search-index.json.
```

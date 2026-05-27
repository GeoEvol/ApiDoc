# ApiDoc v2 Android Devsite Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade ApiDoc from a v1 data-to-file API reference generator into an Android Developers-style offline API reference generator.

**Architecture:** Keep the v1 core pipeline intact: `Javadoc Public API -> DocCorpus + ApiMetadata -> DocProjection -> renderer outputs`. v2 enriches the projection layer, renderer view models, inherited documentation, Android metadata, search/navigation, and static HTML assets without depending on `jdk.javadoc.internal.*` at runtime.

**Tech Stack:** Groovy Gradle plugin, Javadoc Public Doclet API, local OpenJDK javadoc source as reference only, AOSP Doclava source as reference only, static HTML/CSS/JS, JUnit/TestKit.

---

## Current Functional Positioning

ApiDoc is a Gradle plugin for Java SDK/API reference generation. It is not a generic documentation portal, not a StandardDoclet skin, and not a VitePress/Docusaurus generator.

v2 remains focused on:

```text
Java source parsing
  -> stable API model
  -> Android-style API metadata
  -> Android Developers-style API reference pages
  -> offline HTML and Markdown outputs
```

The final UX target is the Android Developers API reference information structure:

```text
package index
class/type index
left navigation
right page table of contents
type header and declaration
API status and level metadata
class hierarchy
implements/extends
nested types
constants
fields
constructors
methods
inherited members
member details
search across package/type/member/tag
```

The visual target is Android Developers / DevSite density and hierarchy, not a pixel-perfect clone.

Visual implementation is allowed to use a two-track workflow:

```text
Default track:
  ApiDoc implementer builds a restrained DevSite-inspired offline shell directly.

Escalation track:
  If the page looks structurally correct but visually weak, hand the visual redesign to Build Web Apps with the prompt in Task 6A. The result must return as static CSS/JS/template guidance that preserves the ApiDoc renderer architecture.
```

Build Web Apps may improve visual design, spacing, typography, responsive behavior, and interaction polish. It must not change the core product scope, introduce a SPA framework, require network/CDN dependencies, or make renderers depend on Javadoc APIs.

When Build Web Apps is used, it must explicitly benchmark Android Developers / Google Developers API reference pages. The local DevSite reference resources are under `C:/Users/Evol/Desktop/devsite/google`, and additional frontend resources are under `D:/workspace`. If these local resources are insufficient, the design pass must stop and report the exact missing repositories, assets, screenshots, CSS/JS files, fonts, icons, or sample pages needed before continuing.

---

## Current Progress

v1 is complete and verified:

```text
DocCorpus + ApiMetadata
  -> DocProjection
  -> doc-corpus.json
  -> page-index.json
  -> nav-index.json
  -> search-index.json
  -> output-manifest.json
  -> api-docs-md/
  -> api-docs-html/
```

Important v1 files:

```text
core/src/main/groovy/com/byd/apidoc/model/
core/src/main/groovy/com/byd/apidoc/comment/
core/src/main/groovy/com/byd/apidoc/metadata/
core/src/main/groovy/com/byd/apidoc/parser/javadoc/
core/src/main/groovy/com/byd/apidoc/projection/
core/src/main/groovy/com/byd/apidoc/reference/
core/src/main/groovy/com/byd/apidoc/render/
core/src/main/groovy/com/byd/apidoc/output/
core/src/main/groovy/com/byd/apidoc/pipeline/
core/src/main/groovy/com/byd/apidoc/plugin/
```

Current v1 limitations that become v2 work:

```text
minimal HTML layout
minimal local search UI
inheritedMemberGroups reserved but empty
no full {@inheritDoc} resolver
limited Android metadata beyond hide/removed/deprecated/since/apiSince
no devsite-inspired asset pipeline
no real page table of contents model
no Android-style API reference shell
```

---

## Reference Inputs

Use these local resources:

```text
D:/workspace/JDK/jdk/src/jdk.javadoc
D:/workspace/doclava/doclava-refs-tags-android-15.0.0_r10
D:/workspace/dokka
D:/workspace/pagefind
D:/workspace/prism
D:/workspace/lucide
D:/workspace/github-markdown-css
C:/Users/Evol/Desktop/devsite/google
```

Reference rules:

```text
OpenJDK javadoc source: study behavior and page structure only; do not depend on internal JDK packages.
Doclava source: study Android metadata, hidden/removed rules, inherited tags, navigation, federation, and page grouping.
DevSite resources: study layout, density, variables, components, responsive behavior, and search UX.
Dokka/VitePress-like tools: reference output architecture only, not main renderer.
```

---

## Task 1: Resource Inventory and Design Notes

**Files:**
- Create: `docs/research/javadoc-v2-notes.md`
- Create: `docs/research/doclava-v2-notes.md`
- Create: `docs/research/devsite-v2-notes.md`
- Create: `docs/research/v2-decisions.md`

- [ ] **Step 1: Inventory Javadoc source behavior**

Read these files and record only behavior we need:

```text
D:/workspace/JDK/jdk/src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/toolkit/util/VisibleMemberTable.java
D:/workspace/JDK/jdk/src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/formats/html/ClassWriter.java
D:/workspace/JDK/jdk/src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/formats/html/AbstractMemberWriter.java
D:/workspace/JDK/jdk/src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/formats/html/taglets/InheritDocTaglet.java
D:/workspace/JDK/jdk/src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/formats/html/taglets/ThrowsTaglet.java
D:/workspace/JDK/jdk/src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/formats/html/Navigation.java
```

Record:

```text
member grouping
inherited member filtering
{@inheritDoc} lookup behavior
summary/detail split
TOC and heading shape
search index shape
```

- [ ] **Step 2: Inventory Doclava behavior**

Read these files:

```text
D:/workspace/doclava/doclava-refs-tags-android-15.0.0_r10/src/com/google/doclava/ClassInfo.java
D:/workspace/doclava/doclava-refs-tags-android-15.0.0_r10/src/com/google/doclava/Comment.java
D:/workspace/doclava/doclava-refs-tags-android-15.0.0_r10/src/com/google/doclava/TagInfo.java
D:/workspace/doclava/doclava-refs-tags-android-15.0.0_r10/src/com/google/doclava/NavTree.java
D:/workspace/doclava/doclava-refs-tags-android-15.0.0_r10/src/com/google/doclava/FederationTagger.java
D:/workspace/doclava/doclava-refs-tags-android-15.0.0_r10/src/com/google/doclava/SinceTagger.java
```

Record:

```text
@hide/@removed/@pending handling
@apiSince/@sdkExtSince handling
InheritedTags behavior
ClassInfo member grouping
external/federated link strategy
NavTree grouping strategy
```

- [ ] **Step 3: Inventory DevSite shell and assets**

Read:

```text
C:/Users/Evol/Desktop/devsite/google/AdIdManager.html
C:/Users/Evol/Desktop/devsite/google/app.css
C:/Users/Evol/Desktop/devsite/google/devsite_devsite_search_module__zh_cn.js
C:/Users/Evol/Desktop/devsite/google/devsite_devsite_book_nav_module__zh_cn.js
C:/Users/Evol/Desktop/devsite/google/devsite_devsite_toc_module__zh_cn.js
```

Record:

```text
page shell regions
left nav behavior
right TOC behavior
search affordances
table and code styling
mobile breakpoints
CSS variables worth adapting
```

- [ ] **Step 4: Write v2 decisions**

Write `docs/research/v2-decisions.md` with these decisions:

```text
no runtime dependency on jdk.javadoc.internal.*
no full DevSite JS import
use DevSite-inspired static shell
preserve v1 JSON contract
extend DocProjection rather than renderer reading raw DocCorpus
implement inherited members before full {@inheritDoc}
keep Markdown renderer compatible with new projection fields
```

- [ ] **Step 5: Verify research files exist**

Run:

```bat
dir docs\research
```

Expected:

```text
javadoc-v2-notes.md
doclava-v2-notes.md
devsite-v2-notes.md
v2-decisions.md
```

---

## Task 2: Projection Model Enrichment

**Files:**
- Modify: `core/src/main/groovy/com/byd/apidoc/projection/DocProjection.groovy`
- Modify: `core/src/main/groovy/com/byd/apidoc/projection/ProjectionBuilder.groovy`
- Test: `core/src/test/groovy/com/byd/apidoc/projection/ProjectionBuilderV2Test.groovy`

- [ ] **Step 1: Add failing projection test**

Create `ProjectionBuilderV2Test.groovy` with assertions for:

```text
TypePageModel has breadcrumbs
TypePageModel has rightToc entries
TypePageModel has Android-style member groups in stable order
TypePageModel exposes inheritedMemberGroups as a real list
SearchEntry includes status metadata and anchor
```

Run:

```bat
.\gradlew.bat :core:test --tests com.byd.apidoc.projection.ProjectionBuilderV2Test --rerun-tasks
```

Expected: FAIL because v2 fields are missing or empty.

- [ ] **Step 2: Add render-neutral projection fields**

Extend projection model with render-neutral types:

```text
BreadcrumbModel
TocEntryModel
ApiStatusModel
AndroidTypeHeaderModel
InheritedMemberGroupModel
```

Do not add HTML strings to these models.

- [ ] **Step 3: Populate basic v2 fields**

Update `ProjectionBuilder` so type pages get:

```text
breadcrumbs: Packages -> package -> type
rightToc: Summary, Constants, Fields, Constructors, Methods, Details, Inherited Members
apiStatus: hidden/removed/deprecated/since/apiSince/sdkExtSince fields
memberGroups: Nested Types, Constants, Fields, Constructors, Methods
inheritedMemberGroups: empty list until Task 3
```

- [ ] **Step 4: Run projection tests**

Run:

```bat
.\gradlew.bat :core:test --tests com.byd.apidoc.projection.ProjectionBuilderV2Test --rerun-tasks
```

Expected: PASS.

---

## Task 3: Basic Inherited Members

**Files:**
- Create: `core/src/main/groovy/com/byd/apidoc/projection/InheritedMemberResolver.groovy`
- Modify: `core/src/main/groovy/com/byd/apidoc/projection/ProjectionBuilder.groovy`
- Test: `core/src/test/groovy/com/byd/apidoc/projection/InheritedMemberResolverTest.groovy`
- Add fixture source under: `core/src/test/resources/sample-sdk/src/main/java/com/example/sdk/inheritance/`

- [ ] **Step 1: Add inheritance fixture**

Add fixture types:

```text
BaseService
DerivedService extends BaseService
ServiceInterface
DerivedWithInterface implements ServiceInterface
OverrideService overrides inherited method
HiddenBaseMember fixture with @hide
RemovedBaseMember fixture with @removed
```

- [ ] **Step 2: Write failing inherited-member tests**

Assert:

```text
public/protected inherited methods appear
overridden methods are not duplicated
hidden/removed inherited members obey VisibilityPolicy
inherited groups are grouped by owner type
links target the original owner page and member anchor
```

Run:

```bat
.\gradlew.bat :core:test --tests com.byd.apidoc.projection.InheritedMemberResolverTest --rerun-tasks
```

Expected: FAIL because resolver is not implemented.

- [ ] **Step 3: Implement resolver**

Implement using existing `DocCorpus` data only:

```text
build type map by qualifiedName
walk superclass and interface TypeRef links
collect visible fields/methods/nested types
remove overridden methods by erased name + parameter signature
respect VisibilityPolicy
return InheritedMemberGroupModel per owner
```

Do not use `jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable`.

- [ ] **Step 4: Wire resolver into ProjectionBuilder**

Populate `TypePageModel.inheritedMemberGroups` from the resolver.

- [ ] **Step 5: Verify**

Run:

```bat
.\gradlew.bat :core:test --tests com.byd.apidoc.projection.InheritedMemberResolverTest --rerun-tasks
.\gradlew.bat :core:test --rerun-tasks
```

Expected: PASS.

---

## Task 4: Basic `{@inheritDoc}` Resolution

**Files:**
- Create: `core/src/main/groovy/com/byd/apidoc/comment/InheritDocResolver.groovy`
- Modify: `core/src/main/groovy/com/byd/apidoc/parser/javadoc/v1/DocCorpusBuilder.groovy`
- Modify: `core/src/main/groovy/com/byd/apidoc/render/HtmlCommentRenderer.groovy`
- Modify: `core/src/main/groovy/com/byd/apidoc/render/MarkdownCommentRenderer.groovy`
- Test: `core/src/test/groovy/com/byd/apidoc/comment/InheritDocResolverTest.groovy`

- [ ] **Step 1: Add failing tests**

Cover:

```text
method body containing only {@inheritDoc}
method summary with text before and after {@inheritDoc}
@param with {@inheritDoc}
@return with {@inheritDoc}
@throws with {@inheritDoc}
no inherited doc produces diagnostic, not crash
```

Run:

```bat
.\gradlew.bat :core:test --tests com.byd.apidoc.comment.InheritDocResolverTest --rerun-tasks
```

Expected: FAIL.

- [ ] **Step 2: Implement resolver**

Resolver behavior:

```text
find overridden or implemented method using DocCorpus ids/signatures
replace InlineTag kind INHERIT_DOC with inherited nodes
resolve block tags by name and param/throws key
record diagnostic when no source is found
```

- [ ] **Step 3: Apply before ProjectionBuilder**

In the pipeline, after `ReferenceResolver.resolve(docCorpus)` and before `ProjectionBuilder.build(...)`, apply `InheritDocResolver`.

- [ ] **Step 4: Verify renderers**

Run:

```bat
.\gradlew.bat :core:test --tests com.byd.apidoc.render.MarkdownRendererTest --tests com.byd.apidoc.render.BuiltinHtmlRendererTest --rerun-tasks
```

Expected: PASS and inherited docs render as normal comments.

---

## Task 5: Android Metadata Expansion

**Files:**
- Modify: `core/src/main/groovy/com/byd/apidoc/metadata/ApiMetadata.groovy`
- Modify: `core/src/main/groovy/com/byd/apidoc/parser/javadoc/v1/DocCorpusBuilder.groovy`
- Modify: `core/src/main/groovy/com/byd/apidoc/projection/ProjectionBuilder.groovy`
- Test: `core/src/test/groovy/com/byd/apidoc/metadata/ApiMetadataBuilderV2Test.groovy`

- [ ] **Step 1: Add failing metadata tests**

Assert extraction for:

```text
@pending
@apiSince
@sdkExtSince
@removed
@deprecated
@RequiresPermission-like annotations
@IntRange-like annotations
@FloatRange-like annotations
@NonNull/@Nullable annotations
```

Run:

```bat
.\gradlew.bat :core:test --tests com.byd.apidoc.metadata.ApiMetadataBuilderV2Test --rerun-tasks
```

Expected: FAIL.

- [ ] **Step 2: Extend ApiMetadata**

Add structured fields:

```text
pending
sdkExtensionSince
deprecatedSince
removedSince
permissions
nullability
valueRanges
sourceTags
sourceAnnotations
```

- [ ] **Step 3: Extract from tags and annotations**

Keep original tags in `CommentDoc.blockTags`. Metadata extraction must not remove raw tag data.

- [ ] **Step 4: Verify**

Run:

```bat
.\gradlew.bat :core:test --tests com.byd.apidoc.metadata.ApiMetadataBuilderV2Test --rerun-tasks
.\gradlew.bat :core:test --rerun-tasks
```

Expected: PASS.

---

## Task 6: DevSite-Inspired Static HTML Renderer

**Files:**
- Create: `core/src/main/groovy/com/byd/apidoc/render/html/HtmlSiteRenderer.groovy`
- Create: `core/src/main/groovy/com/byd/apidoc/render/html/HtmlAssetWriter.groovy`
- Create: `core/src/main/groovy/com/byd/apidoc/render/html/HtmlPageShellRenderer.groovy`
- Modify: `core/src/main/groovy/com/byd/apidoc/render/BuiltinHtmlRenderer.groovy`
- Create: `core/src/main/resources/apidoc-v2/assets/apidoc-devsite.css`
- Create: `core/src/main/resources/apidoc-v2/assets/apidoc-devsite.js`
- Test: `core/src/test/groovy/com/byd/apidoc/render/HtmlDevsiteRendererTest.groovy`

- [ ] **Step 1: Add failing HTML structure test**

Assert generated type page contains:

```text
header region
left navigation region
main article region
right toc region
type title area
API status chips
member summary tables
member detail sections
inherited member sections
responsive CSS asset
offline JS asset
```

Run:

```bat
.\gradlew.bat :core:test --tests com.byd.apidoc.render.HtmlDevsiteRendererTest --rerun-tasks
```

Expected: FAIL.

- [ ] **Step 2: Extract HTML assets out of Groovy strings**

Move v1 inline CSS/JS out of `BuiltinHtmlRenderer` into resources:

```text
core/src/main/resources/apidoc-v2/assets/apidoc-devsite.css
core/src/main/resources/apidoc-v2/assets/apidoc-devsite.js
```

The asset writer copies these files to:

```text
api-docs-html/assets/
```

- [ ] **Step 3: Implement static shell**

Render page shell with these semantic classes:

```text
ad-devsite-topbar
ad-devsite-shell
ad-devsite-book-nav
ad-devsite-content
ad-devsite-toc
ad-api-header
ad-api-status
ad-member-summary
ad-member-detail
```

Do not import full Google DevSite JS. Implement only local, offline behavior.

- [ ] **Step 4: Verify**

Run:

```bat
.\gradlew.bat :core:test --tests com.byd.apidoc.render.HtmlDevsiteRendererTest --rerun-tasks
.\gradlew.bat :core:test --rerun-tasks
```

Expected: PASS.

---

## Task 6A: Optional Build Web Apps Visual Design Pass

Use this task only if Task 6 produces functionally correct pages but the visual quality is not good enough. This is an optional design escalation, not a core architecture change.

**Files:**
- May modify: `core/src/main/resources/apidoc-v2/assets/apidoc-devsite.css`
- May modify: `core/src/main/resources/apidoc-v2/assets/apidoc-devsite.js`
- May modify: `core/src/main/groovy/com/byd/apidoc/render/html/HtmlPageShellRenderer.groovy`
- May modify: `core/src/main/groovy/com/byd/apidoc/render/BuiltinHtmlRenderer.groovy`
- Must not modify core parsing/model contracts unless explicitly approved.

- [ ] **Step 1: Prepare visual handoff inputs**

Collect these files and outputs for the Build Web Apps pass:

```text
D:/workspace/ApiDoc/build/.../api-docs-html/index.html
D:/workspace/ApiDoc/build/.../api-docs-html/classes.html
D:/workspace/ApiDoc/build/.../api-docs-html/package/<sample-package>.html
D:/workspace/ApiDoc/build/.../api-docs-html/reference/<sample-type>.html
D:/workspace/ApiDoc/build/.../api-docs-html/assets/apidoc-devsite.css
D:/workspace/ApiDoc/build/.../api-docs-html/assets/apidoc-devsite.js
C:/Users/Evol/Desktop/devsite/google/AdIdManager.html
C:/Users/Evol/Desktop/devsite/google/app.css
C:/Users/Evol/Desktop/devsite/google/devsite_devsite_search_module__zh_cn.js
C:/Users/Evol/Desktop/devsite/google/devsite_devsite_book_nav_module__zh_cn.js
C:/Users/Evol/Desktop/devsite/google/devsite_devsite_toc_module__zh_cn.js
```

Also provide screenshots of at least:

```text
desktop type page
desktop package page
mobile type page
search open state
left navigation open state
```

- [ ] **Step 2: Use this exact Build Web Apps prompt**

```text
You are redesigning the visual system for ApiDoc, an offline Java API Reference generator.

Product positioning:
- ApiDoc is a Gradle plugin that parses Java SDK/API source through Javadoc Public API.
- It generates DocCorpus + ApiMetadata + DocProjection, then renders offline Markdown and offline static HTML.
- The target output is an Android Developers-style API reference, not a marketing site and not a documentation portal.
- The HTML must be usable after zip extraction and local static hosting. No backend, no CDN, no Node runtime, no SPA framework.

Visual target:
- Use Android Developers / Google DevSite API Reference as the reference for information density, hierarchy, navigation, table of contents, search placement, type header, API status labels, method/member tables, and responsive behavior.
- Do not make a pixel-perfect clone.
- Preserve a professional API reference feel: dense, scannable, restrained, technical, and readable.
- Avoid landing-page styling, hero sections, decorative gradients, oversized cards, bento grids, marketing copy, and illustration-heavy layouts.

Reference resources available locally:
- DevSite sample HTML: C:/Users/Evol/Desktop/devsite/google/AdIdManager.html
- DevSite CSS: C:/Users/Evol/Desktop/devsite/google/app.css
- DevSite search JS: C:/Users/Evol/Desktop/devsite/google/devsite_devsite_search_module__zh_cn.js
- DevSite book nav JS: C:/Users/Evol/Desktop/devsite/google/devsite_devsite_book_nav_module__zh_cn.js
- DevSite TOC JS: C:/Users/Evol/Desktop/devsite/google/devsite_devsite_toc_module__zh_cn.js
- Additional frontend resources: D:/workspace
- ApiDoc generated sample pages and assets will be provided with this prompt.

Mandatory reference policy:
- First benchmark Android Developers / Google Developers API Reference structure and visual density.
- Use C:/Users/Evol/Desktop/devsite/google as the primary local DevSite reference.
- Use D:/workspace as the secondary local frontend resource pool.
- If the available local resources are not enough to make a defensible design decision, stop and list exactly what is missing.
- Do not silently replace the target with a generic documentation theme, SaaS dashboard, landing page, VitePress theme, Docusaurus theme, or GitHub-style Markdown page.

Architecture constraints:
- Keep ApiDoc as a static multi-page site.
- Do not introduce React, Vue, Svelte, Vite, Docusaurus, VitePress, or a SPA runtime.
- Do not import full Google DevSite JavaScript.
- Do not require network access, fonts from CDN, icons from CDN, analytics, or backend APIs.
- Keep all behavior in local static CSS and small vanilla JavaScript.
- Renderer data comes from DocProjection. Do not move API grouping, visibility filtering, link resolution, inherited member calculation, or metadata extraction into CSS/JS.
- CSS/JS can improve layout, interaction, responsive behavior, navigation highlighting, search UI presentation, copy-anchor controls, and collapsible regions.

Pages that must be designed:
- index.html
- packages.html
- classes.html
- package/<package>.html
- reference/<qualified-type>.html

Required page regions:
- top app bar with project name and search
- left package/type navigation
- main article content
- right table of contents on desktop
- mobile navigation drawer/toggle
- type header with package, declaration, inheritance, implements/extends
- API status area for deprecated/removed/hidden/since/apiSince/sdkExtSince
- member summary groups: nested types, constants, fields, constructors, methods
- inherited member groups
- member detail sections
- code/type signature blocks
- search results panel

Design requirements:
- Use a clean light theme by default.
- Use CSS variables for color, spacing, typography, borders, radius, shadows, and semantic status colors.
- Typography should be compact and highly readable for reference docs.
- Tables must be scannable and responsive.
- Code signatures must wrap cleanly without breaking layout.
- Left navigation should support long package/type names.
- Right TOC should remain useful without occupying too much width.
- Mobile layout must not overlap content, nav, search, or TOC.
- Avoid nested cards and heavy shadows. Prefer separators, subtle surfaces, and clear hierarchy.
- API status labels should be visually distinct but not loud.
- Search UI should feel integrated with the header and should support keyboard-friendly scanning.

Deliverables:
1. A concise visual design specification:
   - layout grid
   - breakpoints
   - spacing scale
   - typography scale
   - color tokens
   - component rules
   - interaction states
2. Revised static CSS for apidoc-devsite.css.
3. Revised vanilla JS for apidoc-devsite.js, only if needed.
4. HTML class/structure adjustment recommendations, if current markup blocks the design.
5. A verification checklist for desktop and mobile screenshots.

Important:
- Do not rewrite the ApiDoc generator architecture.
- Do not invent new product features.
- Do not convert the output into a dashboard or marketing page.
- Do not remove offline compatibility.
- Do not continue with guesswork if required Android/Google Developers reference resources are missing. Stop and request the missing resource names or repositories.
- If a requested visual detail conflicts with static offline constraints, choose the simpler offline-safe option and document the tradeoff.
```

- [ ] **Step 3: Apply only architecture-safe output**

Accept Build Web Apps output only if it satisfies all of these:

```text
CSS remains static and local.
JS remains vanilla and local.
No SPA framework is introduced.
No CDN or remote resource is required.
No renderer logic is moved into JS.
No core JSON/Projection schema is changed for visual convenience.
HTML class changes remain semantic and renderer-owned.
```

- [ ] **Step 4: Verify visual pass**

Run:

```bat
.\gradlew.bat :core:test --tests com.byd.apidoc.render.HtmlDevsiteRendererTest --rerun-tasks
.\gradlew.bat :core:test --rerun-tasks
```

Expected:

```text
BUILD SUCCESSFUL
```

Then manually inspect:

```text
desktop type page
desktop package page
mobile type page
search result panel
long type names
long method signatures
deprecated/removed/since status labels
```

---

## Task 7: Search and Navigation v2

**Files:**
- Modify: `core/src/main/groovy/com/byd/apidoc/projection/DocProjection.groovy`
- Modify: `core/src/main/groovy/com/byd/apidoc/projection/ProjectionBuilder.groovy`
- Modify: `core/src/main/resources/apidoc-v2/assets/apidoc-devsite.js`
- Test: `core/src/test/groovy/com/byd/apidoc/projection/SearchAndNavV2Test.groovy`

- [ ] **Step 1: Add failing tests**

Assert:

```text
nav tree groups packages and types
search entries distinguish package/type/constructor/method/field/constant
search entry includes ownerName, packageName, status, summary, url, anchor
HTML search UI filters by kind and label
```

- [ ] **Step 2: Extend search and nav model**

Add fields without breaking v1 JSON names:

```text
SearchEntry.status
SearchEntry.tokens
SearchEntry.displaySignature
NavNode.activePath
NavNode.group
```

- [ ] **Step 3: Implement local search filtering**

Add offline search behavior:

```text
query text
kind filter
keyboard focus
result click target url + anchor
no backend dependency
```

- [ ] **Step 4: Verify**

Run:

```bat
.\gradlew.bat :core:test --tests com.byd.apidoc.projection.SearchAndNavV2Test --rerun-tasks
.\gradlew.bat :core:test --rerun-tasks
```

Expected: PASS.

---

## Task 8: Markdown Parity

**Files:**
- Modify: `core/src/main/groovy/com/byd/apidoc/render/MarkdownRenderer.groovy`
- Modify: `core/src/main/groovy/com/byd/apidoc/render/MarkdownCommentRenderer.groovy`
- Test: `core/src/test/groovy/com/byd/apidoc/render/MarkdownRendererV2Test.groovy`

- [ ] **Step 1: Add failing Markdown v2 tests**

Assert Markdown includes:

```text
breadcrumbs
API status block
inherited members
expanded {@inheritDoc}
Android metadata text
stable anchors
relative links
```

- [ ] **Step 2: Update Markdown renderer**

Render all v2 projection fields without HTML-only dependencies.

- [ ] **Step 3: Verify**

Run:

```bat
.\gradlew.bat :core:test --tests com.byd.apidoc.render.MarkdownRendererV2Test --rerun-tasks
.\gradlew.bat :core:test --rerun-tasks
```

Expected: PASS.

---

## Task 9: End-to-End v2 Acceptance

**Files:**
- Modify: `core/src/test/groovy/com/byd/apidoc/plugin/ApiDocPluginEndToEndTest.groovy`
- Create: `core/src/test/groovy/com/byd/apidoc/plugin/ApiDocPluginV2EndToEndTest.groovy`
- Create or extend fixtures under: `core/src/test/resources/sample-sdk/`

- [ ] **Step 1: Add v2 end-to-end fixture assertions**

Assert `generateHtml` and `generateMarkdown` produce:

```text
DocCorpus still includes hidden/removed APIs
Projection filters hidden/removed by policy
HTML contains DevSite-inspired shell
HTML contains inherited member groups
HTML contains right TOC
HTML search works from root and nested pages
Markdown contains inherited docs and metadata
output-manifest.json remains compatible
```

- [ ] **Step 2: Run v2 E2E tests**

Run:

```bat
.\gradlew.bat :core:test --tests com.byd.apidoc.plugin.ApiDocPluginV2EndToEndTest --rerun-tasks
```

Expected: PASS.

- [ ] **Step 3: Run full suite**

Run:

```bat
.\gradlew.bat :core:test --rerun-tasks
```

Expected:

```text
BUILD SUCCESSFUL
```

---

## Task 10: v2 Documentation and Handoff

**Files:**
- Create: `docs/v2-output-contract.md`
- Modify: `docs/portable-usage.md`
- Create: `docs/v2-android-devsite-target.md`

- [ ] **Step 1: Document v2 contract**

Document:

```text
new projection fields
new HTML shell regions
search/nav schema additions
inherited member support level
{@inheritDoc} support level
Android metadata support level
```

- [ ] **Step 2: Document target comparison**

Create `docs/v2-android-devsite-target.md` with a table:

```text
Android Developers feature
ApiDoc v2 support
Not supported
Reason
```

- [ ] **Step 3: Verify docs and tests**

Run:

```bat
.\gradlew.bat :core:test --rerun-tasks
```

Expected: PASS.

---

## Explicit v2 Non-Goals

Do not implement these in v2:

```text
pixel-perfect Google DevSite clone
runtime import of full DevSite JS module system
Kotlin/Dokka support
full API diff/lint
multi-version docs
server-side search
external portal integration
complete StandardDoclet behavior parity
dependency on jdk.javadoc.internal.*
```

---

## Acceptance Definition

v2 is complete when:

```text
1. v1 output contract remains valid.
2. HTML output has Android Developers-style information density and page shell.
3. Type pages include right TOC, enriched API status, member summaries, details, and inherited members.
4. Basic {@inheritDoc} is resolved for body, params, returns, and throws.
5. Android metadata is extracted and rendered without dropping raw tags.
6. Search and nav are usable offline.
7. Markdown output remains structurally complete.
8. .\gradlew.bat :core:test --rerun-tasks passes.
```

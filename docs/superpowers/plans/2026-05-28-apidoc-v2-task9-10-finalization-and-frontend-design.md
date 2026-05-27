# ApiDoc v2 Task 9-10 Finalization and Frontend Design Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish ApiDoc v2 acceptance and documentation handoff, and run a Build Web Apps-driven visual design pass for the offline HTML API reference frontend.

**Architecture:** ApiDoc remains a static offline API reference generator. The pipeline stays `DocCorpus + ApiMetadata -> DocProjection -> JSON / Markdown / HTML`; visual design can only adjust renderer-owned markup, static CSS, and small vanilla JavaScript, not parsing, metadata extraction, projection semantics, visibility filtering, or link resolution.

**Tech Stack:** Groovy, Gradle TestKit, Spock, Javadoc Public API, static HTML, static CSS, vanilla JavaScript, Build Web Apps frontend design workflow, Browser/screenshot visual QA.

---

## Current State

This plan refines the final part of:

```text
D:/workspace/ApiDoc/docs/superpowers/plans/2026-05-27-apidoc-v2-android-devsite.md
```

Current checkpoint:

```text
Task 1: Resource Inventory and Design Notes        DONE
Task 2: Projection Model Enrichment                DONE
Task 3: Basic Inherited Members                    DONE
Task 4: Basic {@inheritDoc} Resolution             DONE
Task 5: Android Metadata Expansion                 DONE
Task 6: DevSite-Inspired Static HTML Renderer      DONE
Task 6A: Build Web Apps Visual Pass                NOW ENABLED
Task 7: Search and Navigation v2                   DONE
Task 8: Markdown Parity                            DONE
Task 9: End-to-End v2 Acceptance                   IMPLEMENTED, REVIEW PENDING
Task 10: v2 Documentation and Handoff              PENDING
```

Important constraints:

```text
D:/workspace/ApiDoc is not a git repository in this environment.
Track progress by changed files, test commands, reviewer output, screenshots, and checkpoint updates.
Do not rely on commits as checkpoints.
```

Task 9 worker-reported verification:

```bat
.\gradlew.bat :core:test --tests com.byd.apidoc.plugin.ApiDocPluginV2EndToEndTest --rerun-tasks
.\gradlew.bat :core:test --rerun-tasks
```

Reported result:

```text
74 tests, 0 failures, 0 errors, 0 skipped
```

This result is not enough to mark Task 9 complete. It still needs:

```text
spec review
code quality review
local verification by current executing agent
```

---

## Scope Lock

The frontend target is:

```text
Android Developers / Google Developers API Reference information density,
navigation hierarchy,
technical readability,
offline static behavior.
```

The frontend target is not:

```text
pixel-perfect Google clone
generic documentation theme
marketing site
SaaS dashboard
landing page
VitePress theme
Docusaurus theme
GitHub Markdown page
SPA application
```

Build Web Apps may improve:

```text
visual hierarchy
spacing
typography
component density
responsive layout
left navigation behavior
right TOC behavior
search overlay/panel
status chip styling
member summary/detail readability
long signature wrapping
accessibility states
```

Build Web Apps must not introduce:

```text
React
Vue
Svelte
Vite
VitePress
Docusaurus
Astro
backend API
CDN dependency
remote fonts
remote icons
full Google DevSite JavaScript
renderer logic inside frontend JS
schema changes for visual convenience
```

---

## Reference Resources

Primary DevSite reference resources:

```text
C:/Users/Evol/Desktop/devsite/google/AdIdManager.html
C:/Users/Evol/Desktop/devsite/google/app.css
C:/Users/Evol/Desktop/devsite/google/devsite_devsite_search_module__zh_cn.js
C:/Users/Evol/Desktop/devsite/google/devsite_devsite_book_nav_module__zh_cn.js
C:/Users/Evol/Desktop/devsite/google/devsite_devsite_toc_module__zh_cn.js
```

Secondary frontend resource pool:

```text
D:/workspace
```

If those resources are not enough to make a defensible design decision, stop and report exactly what is missing, using this format:

```text
Missing resources:
- screenshots: <exact page/state/viewport needed>
- CSS/JS: <exact repository or file type needed>
- icons/fonts/assets: <exact asset family needed>
- sample generated pages: <exact ApiDoc output page needed>
Reason blocked:
- <why current resources are insufficient>
```

Do not continue by guessing a generic style.

---

## Files and Ownership

### Task 9 review files

```text
D:/workspace/ApiDoc/core/src/test/groovy/com/byd/apidoc/plugin/ApiDocPluginV2EndToEndTest.groovy
D:/workspace/ApiDoc/core/src/test/groovy/com/byd/apidoc/plugin/ApiDocPluginEndToEndTest.groovy
D:/workspace/ApiDoc/core/src/test/resources/sample-sdk/
```

### Frontend files allowed for Build Web Apps implementation

```text
D:/workspace/ApiDoc/core/src/main/resources/apidoc-v2/assets/apidoc-devsite.css
D:/workspace/ApiDoc/core/src/main/resources/apidoc-v2/assets/apidoc-devsite.js
D:/workspace/ApiDoc/core/src/main/resources/apidoc-v2/assets/apidoc-search.js
```

Renderer-owned markup files allowed if current HTML structure blocks the design:

```text
D:/workspace/ApiDoc/core/src/main/groovy/com/byd/apidoc/render/html/HtmlSiteRenderer.groovy
D:/workspace/ApiDoc/core/src/main/groovy/com/byd/apidoc/render/html/HtmlPageShellRenderer.groovy
D:/workspace/ApiDoc/core/src/main/groovy/com/byd/apidoc/render/html/HtmlAssetWriter.groovy
```

If the actual renderer file names differ, inspect:

```bat
Get-ChildItem -LiteralPath D:\workspace\ApiDoc\core\src\main\groovy\com\byd\apidoc\render -Recurse -File
```

Then update this plan's checkpoint with the actual file names before editing.

### Documentation files for Task 10

```text
D:/workspace/ApiDoc/docs/v2-output-contract.md
D:/workspace/ApiDoc/docs/v2-android-devsite-target.md
D:/workspace/ApiDoc/docs/portable-usage.md
D:/workspace/ApiDoc/docs/v2-frontend-design.md
D:/workspace/ApiDoc/docs/v2-visual-qa.md
D:/workspace/ApiDoc/docs/superpowers/plans/2026-05-27-apidoc-v2-progress-checkpoint.md
```

`docs/v2-frontend-design.md` and `docs/v2-visual-qa.md` are optional only if their content is folded cleanly into `docs/v2-android-devsite-target.md`. Prefer creating them if the visual pass produces meaningful design tokens, screenshots, or QA notes.

---

## Execution Order

Do not skip review just because Task 9 tests reportedly passed.

```text
Task A: Task 9 spec review
Task B: Task 9 code quality review
Task C: prepare generated HTML and reference inputs for Build Web Apps
Task D: run Build Web Apps design/spec pass
Task E: apply approved architecture-safe frontend changes
Task F: browser and screenshot visual QA
Task G: Task 10 documentation and handoff
Task H: final verification and checkpoint update
```

---

## Task A: Task 9 Spec Review

**Files:**
- Review: `D:/workspace/ApiDoc/docs/superpowers/plans/2026-05-27-apidoc-v2-android-devsite.md`
- Review: `D:/workspace/ApiDoc/core/src/test/groovy/com/byd/apidoc/plugin/ApiDocPluginV2EndToEndTest.groovy`
- Review: `D:/workspace/ApiDoc/core/src/test/resources/sample-sdk/`

- [ ] **Step A1: Inspect the Task 9 implementation**

Run:

```bat
Get-Content -LiteralPath D:\workspace\ApiDoc\core\src\test\groovy\com\byd\apidoc\plugin\ApiDocPluginV2EndToEndTest.groovy
```

Expected:

```text
The test creates or uses a sample Gradle project.
The test runs generateHtml and generateMarkdown or equivalent plugin tasks.
The test asserts JSON, Markdown, and HTML outputs.
The test covers v2 projection/rendering behavior rather than only file existence.
```

- [ ] **Step A2: Compare test assertions against Task 9 spec**

Open the Task 9 section:

```bat
Select-String -LiteralPath D:\workspace\ApiDoc\docs\superpowers\plans\2026-05-27-apidoc-v2-android-devsite.md -Pattern "## Task 9" -Context 0,70
```

Confirm there are assertions for:

```text
DocCorpus includes hidden APIs
DocCorpus includes removed APIs
Projection filters hidden/removed by VisibilityPolicy
Generated HTML includes DevSite shell elements
Generated HTML includes inherited members
Generated HTML includes right TOC
Generated HTML includes search assets or search UI entry point
Generated Markdown includes inherited documentation
Generated Markdown includes metadata/status output
Output manifest remains compatible
Search/nav/page JSON remain valid
```

- [ ] **Step A3: Record spec review result**

If anything is missing, write a precise fix list in the progress checkpoint under a new section:

```text
## Task 9 Review Findings

Spec review:
- [BLOCKER] <missing required assertion or behavior>
- [MAJOR] <coverage gap that could hide a regression>
- [MINOR] <clarity or naming issue>
```

If everything is covered, record:

```text
Task 9 spec review: approved.
Evidence:
- <test class name>
- <assertion groups observed>
```

Use `apply_patch` for checkpoint edits.

---

## Task B: Task 9 Code Quality Review

**Files:**
- Review: `D:/workspace/ApiDoc/core/src/test/groovy/com/byd/apidoc/plugin/ApiDocPluginV2EndToEndTest.groovy`
- Review: any fixture files changed by Task 9

- [ ] **Step B1: Check for brittle absolute paths**

Search:

```bat
Select-String -Path D:\workspace\ApiDoc\core\src\test\groovy\com\byd\apidoc\plugin\*.groovy -Pattern "D:\\|C:\\|Users\\|workspace\\|tmp\\|build\\\\testkit"
```

Expected:

```text
No hardcoded user machine paths.
Temporary folders use Spock @TempDir, Gradle TestKit temp projects, or buildDir-controlled paths.
```

- [ ] **Step B2: Check for weak file-existence-only acceptance**

Search:

```bat
Select-String -LiteralPath D:\workspace\ApiDoc\core\src\test\groovy\com\byd\apidoc\plugin\ApiDocPluginV2EndToEndTest.groovy -Pattern "exists\\(\\)|isFile\\(\\)|contains\\("
```

Expected:

```text
File existence checks are paired with content checks.
JSON files are parsed or content-asserted for stable keys.
HTML/Markdown checks assert meaningful v2 content.
```

- [ ] **Step B3: Run focused Task 9 verification**

Run:

```bat
.\gradlew.bat :core:test --tests com.byd.apidoc.plugin.ApiDocPluginV2EndToEndTest --rerun-tasks
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step B4: Run full core verification**

Run:

```bat
.\gradlew.bat :core:test --rerun-tasks
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step B5: Record code quality review result**

If approved, checkpoint text:

```text
Task 9 code quality review: approved.
Verification:
- .\gradlew.bat :core:test --tests com.byd.apidoc.plugin.ApiDocPluginV2EndToEndTest --rerun-tasks
- .\gradlew.bat :core:test --rerun-tasks
```

If blocked, record each issue with file path, line number, impact, and exact expected fix.

---

## Task C: Prepare Build Web Apps Inputs

**Files:**
- Read: generated HTML output directory from Task 9 TestKit fixture
- Read: `C:/Users/Evol/Desktop/devsite/google/*`
- Read: `D:/workspace/ApiDoc/core/src/main/resources/apidoc-v2/assets/*`

- [ ] **Step C1: Locate Task 9 generated output directory**

Inspect the E2E test for output configuration:

```bat
Select-String -LiteralPath D:\workspace\ApiDoc\core\src\test\groovy\com\byd\apidoc\plugin\ApiDocPluginV2EndToEndTest.groovy -Pattern "output|api-docs-html|api-docs-md|doc-corpus|nav-index|search-index|manifest" -Context 2,4
```

Expected:

```text
The command identifies the TestKit project output path or enough build script text to infer it.
```

If output is generated only during tests and deleted with temp folders, create a non-test fixture under `C:/tmp/apidoc-v2-fixture` using the same build script and sample sources. Use this command shape after creating the fixture:

```bat
D:\workspace\ApiDoc\gradlew.bat -p C:\tmp\apidoc-v2-fixture generateHtml generateMarkdown --stacktrace
```

- [ ] **Step C2: Confirm generated pages exist**

Locate and list:

```text
api-docs-html/index.html
api-docs-html/classes.html
api-docs-html/packages.html
api-docs-html/package/<sample-package>.html
api-docs-html/reference/<sample-type>.html
api-docs-html/assets/apidoc-devsite.css
api-docs-html/assets/apidoc-devsite.js
api-docs-html/assets/apidoc-search.js
api-docs-html/nav-index.json
api-docs-html/search-index.json
api-docs-html/output-manifest.json
api-docs-md/index.md
api-docs-md/classes.md
api-docs-md/packages.md
api-docs-md/reference/<sample-type>.md
```

PowerShell helper once the output root is known:

```bat
Get-ChildItem -LiteralPath <OUTPUT_ROOT> -Recurse -File | Select-Object FullName,Length,LastWriteTime
```

- [ ] **Step C3: Confirm local DevSite resources exist**

Run:

```bat
Get-Item -LiteralPath C:\Users\Evol\Desktop\devsite\google\AdIdManager.html
Get-Item -LiteralPath C:\Users\Evol\Desktop\devsite\google\app.css
Get-Item -LiteralPath C:\Users\Evol\Desktop\devsite\google\devsite_devsite_search_module__zh_cn.js
Get-Item -LiteralPath C:\Users\Evol\Desktop\devsite\google\devsite_devsite_book_nav_module__zh_cn.js
Get-Item -LiteralPath C:\Users\Evol\Desktop\devsite\google\devsite_devsite_toc_module__zh_cn.js
```

Expected:

```text
All five files exist.
```

If any file is missing, stop and report the missing file list to the user before running the design pass.

- [ ] **Step C4: Capture baseline HTML screenshots**

Use Browser plugin first when executing inside Codex. Open the generated `reference/<sample-type>.html` page and capture:

```text
desktop width around 1440px
mobile width around 390px
search open state
left nav state
right TOC state
long method/member signature area
metadata/status chip area
```

If Browser is unavailable, use an approved local Chrome headless command only after confirming the output file path:

```bat
C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe -Command "& 'C:\Program Files\Google\Chrome\Application\chrome.exe' --headless=new --no-sandbox --disable-gpu --hide-scrollbars --window-size=1440,1200 --screenshot=<SCREENSHOT_DESKTOP>.png file:///<HTML_FILE>"
```

Do not claim visual baseline completion without screenshots.

---

## Task D: Build Web Apps Design Pass

**Files:**
- May create: `D:/workspace/ApiDoc/docs/v2-frontend-design.md`
- May create: `D:/workspace/ApiDoc/docs/v2-visual-qa.md`
- May create temporary screenshots under: `D:/workspace/ApiDoc/build/visual-qa/`

- [ ] **Step D1: Use Build Web Apps as a formal design pass**

Use this exact prompt as the design brief:

```text
You are designing the frontend visual system for ApiDoc, an offline Java API Reference generator.

Product positioning:
- ApiDoc is a Gradle plugin that parses Java SDK/API source through Javadoc Public API.
- It generates DocCorpus + ApiMetadata + DocProjection, then renders offline Markdown and offline static HTML.
- The HTML output is a static multi-page API reference that can be zipped, copied to another computer, and viewed through local static hosting.
- This is an API reference product, not a documentation portal, not a marketing site, and not a SPA.

Visual target:
- Benchmark Android Developers / Google Developers API Reference.
- Use it for information density, navigation hierarchy, right-side table of contents, search placement, class/type header structure, API status labels, member summaries, member details, inherited member sections, and responsive behavior.
- Do not make a pixel-perfect clone.
- Preserve a restrained technical feel: dense, scannable, precise, readable, and quiet.
- Do not use landing-page heroes, marketing copy, illustration-heavy sections, bento grids, oversized rounded cards, decorative gradient backgrounds, or SaaS dashboard aesthetics.

Primary local reference resources:
- C:/Users/Evol/Desktop/devsite/google/AdIdManager.html
- C:/Users/Evol/Desktop/devsite/google/app.css
- C:/Users/Evol/Desktop/devsite/google/devsite_devsite_search_module__zh_cn.js
- C:/Users/Evol/Desktop/devsite/google/devsite_devsite_book_nav_module__zh_cn.js
- C:/Users/Evol/Desktop/devsite/google/devsite_devsite_toc_module__zh_cn.js

Secondary local reference resources:
- D:/workspace

Generated ApiDoc pages to inspect:
- api-docs-html/index.html
- api-docs-html/packages.html
- api-docs-html/classes.html
- api-docs-html/package/<sample-package>.html
- api-docs-html/reference/<sample-type>.html
- api-docs-html/assets/apidoc-devsite.css
- api-docs-html/assets/apidoc-devsite.js
- api-docs-html/assets/apidoc-search.js
- api-docs-html/nav-index.json
- api-docs-html/search-index.json

Architecture constraints:
- Keep ApiDoc as a static multi-page site.
- Do not introduce React, Vue, Svelte, Vite, VitePress, Docusaurus, Astro, or a SPA runtime.
- Do not import full Google DevSite JavaScript.
- Do not require network access, CDN fonts, CDN icons, analytics, or backend APIs.
- Keep all behavior in local static CSS and small vanilla JavaScript.
- Renderer data comes from DocProjection. Do not move API grouping, visibility filtering, link resolution, inherited member calculation, metadata extraction, or search-index generation into CSS/JS.
- If the current markup blocks a necessary visual improvement, recommend semantic renderer-owned class/structure changes only.

Required page/state coverage:
- desktop index page
- desktop package index page
- desktop class/type index page
- desktop package detail page
- desktop type/reference page
- mobile type/reference page
- search closed state
- search open state with results
- search empty state
- left nav open/collapsed behavior
- right TOC active state
- inherited members section
- deprecated/removed/pending/API-level status labels
- long package names
- long type names
- long method signatures
- overloaded methods
- parameter/return/throws documentation blocks

Design constraints:
- Text must not overlap or clip at desktop or mobile widths.
- Long signatures must wrap predictably without widening the page.
- Left navigation, main content, and right TOC must not fight for space.
- Status chips must be distinct but not visually loud.
- Tables and member summaries must be easy to scan repeatedly.
- Search must feel integrated into the top app bar and support keyboard-friendly scanning.
- Use separators, subtle surfaces, compact rows, and clear hierarchy instead of nested cards and heavy shadows.
- Use local CSS variables for color, spacing, typography, radii, borders, z-index, and motion.

Deliverables:
1. A concise visual design specification:
   - layout grid
   - breakpoints
   - spacing scale
   - typography scale
   - color tokens
   - component rules
   - search states
   - nav states
   - status chip rules
   - member table/detail rules
   - responsive behavior
2. A CSS patch plan for:
   - core/src/main/resources/apidoc-v2/assets/apidoc-devsite.css
3. A vanilla JS patch plan only if behavior changes are needed for:
   - core/src/main/resources/apidoc-v2/assets/apidoc-devsite.js
   - core/src/main/resources/apidoc-v2/assets/apidoc-search.js
4. A renderer markup adjustment plan only if current HTML structure blocks the design.
5. A screenshot QA checklist for desktop and mobile.
6. A missing-resource report if current local resources are insufficient.

Hard stops:
- Do not continue with a generic documentation theme.
- Do not continue if Android/Google Developers reference resources are insufficient. Stop and list exact missing resources.
- Do not change core model, parser, metadata, projection, link resolution, inherited members, or JSON schema for visual convenience.
- Do not remove offline compatibility.
- Do not claim completion without browser screenshots.
```

- [ ] **Step D2: Produce design system notes before editing**

Create or update:

```text
D:/workspace/ApiDoc/docs/v2-frontend-design.md
```

Required sections:

```markdown
# ApiDoc v2 Frontend Design

## Product Positioning

## Reference Inputs

## Layout Grid

## Breakpoints

## Typography

## Color Tokens

## Spacing and Radius

## App Bar

## Left Navigation

## Right TOC

## Search

## Type Header

## API Status Labels

## Member Summary

## Member Detail

## Inherited Members

## Responsive Rules

## Allowed Implementation Files

## Explicit Non-Goals
```

- [ ] **Step D3: Decide whether Image Gen is necessary**

Build Web Apps usually requires concept images before coding. For this task:

```text
Use Image Gen if the existing DevSite resources and current ApiDoc pages are not enough to define layout, spacing, typography, and responsive behavior.
Do not use Image Gen to invent a new brand or marketing look.
If Image Gen is used, request API-reference UI screenshots only: desktop type page, mobile type page, search state, and member table/detail state.
If the local DevSite references are already concrete enough, document that this is an existing-design-system adaptation and proceed without Image Gen.
```

Record the decision in `docs/v2-frontend-design.md` under `Reference Inputs`.

---

## Task E: Apply Approved Frontend Design Safely

**Files:**
- Modify: `D:/workspace/ApiDoc/core/src/main/resources/apidoc-v2/assets/apidoc-devsite.css`
- Modify only if needed: `D:/workspace/ApiDoc/core/src/main/resources/apidoc-v2/assets/apidoc-devsite.js`
- Modify only if needed: `D:/workspace/ApiDoc/core/src/main/resources/apidoc-v2/assets/apidoc-search.js`
- Modify only if needed: renderer-owned HTML shell files under `D:/workspace/ApiDoc/core/src/main/groovy/com/byd/apidoc/render/`

- [ ] **Step E1: Add or normalize CSS tokens**

Ensure `apidoc-devsite.css` has tokens for:

```css
:root {
  --ad-bg: #ffffff;
  --ad-surface: #ffffff;
  --ad-surface-alt: #f8fafd;
  --ad-text: #202124;
  --ad-muted: #5f6368;
  --ad-border: #dadce0;
  --ad-primary: #1a73e8;
  --ad-primary-hover: #185abc;
  --ad-code-bg: #f1f3f4;
  --ad-deprecated: #b06000;
  --ad-removed: #b3261e;
  --ad-pending: #5f6368;
  --ad-radius-sm: 4px;
  --ad-radius-md: 8px;
  --ad-header-height: 64px;
  --ad-left-nav-width: 280px;
  --ad-right-toc-width: 240px;
  --ad-content-max: 960px;
}
```

Keep actual values aligned with the approved design notes. If current CSS already defines equivalent variables, consolidate rather than duplicate.

- [ ] **Step E2: Implement layout without changing renderer semantics**

Use CSS and renderer-owned class names to preserve this layout:

```text
top app bar
left nav
main API content
right TOC
footer
```

Desktop target:

```text
left nav fixed or sticky within viewport
right TOC visible for type pages
main content centered between side rails
member summaries and details fit without horizontal page scroll
```

Mobile target:

```text
left nav collapses behind button or drawer behavior
right TOC collapses or moves below type header
main content uses full width
long names and signatures wrap
search remains reachable
```

- [ ] **Step E3: Keep JavaScript behavior small and local**

Only implement vanilla JS for:

```text
mobile nav toggle
right TOC active highlight
copy anchor link
search panel open/close
search input/result navigation
```

Do not implement:

```text
client-side routing
runtime API model transformation
visibility filtering
inherited member computation
link resolution
metadata parsing
remote search requests
```

- [ ] **Step E4: Regenerate outputs**

Run the focused renderer tests first:

```bat
.\gradlew.bat :core:test --tests com.byd.apidoc.render.HtmlDevsiteRendererTest --rerun-tasks
```

Expected:

```text
BUILD SUCCESSFUL
```

Then run the v2 E2E:

```bat
.\gradlew.bat :core:test --tests com.byd.apidoc.plugin.ApiDocPluginV2EndToEndTest --rerun-tasks
```

Expected:

```text
BUILD SUCCESSFUL
```

---

## Task F: Browser and Screenshot Visual QA

**Files:**
- Create or update: `D:/workspace/ApiDoc/docs/v2-visual-qa.md`
- May create temporary screenshots under: `D:/workspace/ApiDoc/build/visual-qa/`

- [ ] **Step F1: Open generated HTML through Browser**

Use Browser plugin first. Verify:

```text
desktop type page
mobile type page
index page
packages page
classes page
package detail page
search open state
search result state
search empty state
```

If Browser is unavailable or unreliable, record fallback reason and use Chrome headless screenshot commands.

- [ ] **Step F2: Capture desktop screenshot**

Target viewport:

```text
1440 x 1200
```

Page:

```text
api-docs-html/reference/<sample-type>.html
```

Required visible checks:

```text
app bar visible
left nav visible
right TOC visible
type header visible
metadata/status chips visible when applicable
member summary visible
long signature wraps
no horizontal page scroll
```

- [ ] **Step F3: Capture mobile screenshot**

Target viewport:

```text
390 x 1200
```

Page:

```text
api-docs-html/reference/<sample-type>.html
```

Required visible checks:

```text
no text overlap
no clipped method signatures
nav/search controls reachable
right TOC does not cover content
member details remain readable
status chips wrap safely
```

- [ ] **Step F4: Capture search screenshots**

States:

```text
search closed
search open with query and results
search open with empty result
```

Required checks:

```text
search panel does not hide primary navigation permanently
results show kind, label, owner/package, and URL target
keyboard focus is visible
empty state is clear and compact
```

- [ ] **Step F5: Write visual QA ledger**

Create or update:

```text
D:/workspace/ApiDoc/docs/v2-visual-qa.md
```

Required structure:

```markdown
# ApiDoc v2 Visual QA

## Build Under Test

## Reference Inputs

## Screenshots

| State | Viewport | Path | Screenshot | Result |
| --- | --- | --- | --- | --- |

## Desktop Type Page Checks

## Mobile Type Page Checks

## Search Checks

## Navigation Checks

## Long Content Checks

## Known Deviations

## Final Result
```

Do not mark final result as pass if there is:

```text
overlapping text
clipped primary content
horizontal page scroll on mobile
unreachable search
nav covering content
right TOC covering content
long method signature breaking layout
generic/marketing visual drift
```

---

## Task G: Task 10 Documentation and Handoff

**Files:**
- Create: `D:/workspace/ApiDoc/docs/v2-output-contract.md`
- Create: `D:/workspace/ApiDoc/docs/v2-android-devsite-target.md`
- Modify: `D:/workspace/ApiDoc/docs/portable-usage.md`
- Create or update: `D:/workspace/ApiDoc/docs/v2-frontend-design.md`
- Create or update: `D:/workspace/ApiDoc/docs/v2-visual-qa.md`

- [ ] **Step G1: Write output contract documentation**

Create:

```text
D:/workspace/ApiDoc/docs/v2-output-contract.md
```

Required sections:

```markdown
# ApiDoc v2 Output Contract

## Scope

## Directory Layout

## Manifest

## doc-corpus.json

## page-index.json

## nav-index.json

## search-index.json

## HTML Output

## Markdown Output

## DocProjection Fields

## TypePageModel Fields

## Inherited Member Fields

## ApiMetadata Fields

## Search Entry Fields

## Compatibility Rules

## Non-Goals
```

Include this compatibility rule exactly:

```text
Renderer implementations must not require Javadoc API objects. Renderers consume DocProjection and may read DocCorpus only as a read-only supplemental source.
```

- [ ] **Step G2: Write Android DevSite target documentation**

Create:

```text
D:/workspace/ApiDoc/docs/v2-android-devsite-target.md
```

Required sections:

```markdown
# ApiDoc v2 Android DevSite Target

## Positioning

## Reference Sources

## What We Match

## What We Do Not Clone

## Page Types

## Type Page Anatomy

## Navigation Model

## Search Model

## API Status Model

## Inherited Members

## Responsive Behavior

## Offline Constraints

## Build Web Apps Design Pass

## Future Work
```

State clearly:

```text
ApiDoc targets Android Developers / Google Developers API Reference information structure and density. It does not import DevSite runtime or attempt a pixel-perfect clone.
```

- [ ] **Step G3: Update portable usage documentation**

Modify:

```text
D:/workspace/ApiDoc/docs/portable-usage.md
```

Add sections:

```markdown
## v2 HTML Output

## v2 Markdown Output

## Viewing Offline HTML

## Copying to Another Computer

## Required JDK and Gradle Inputs

## Dependency Classpath

## Troubleshooting
```

Include command examples:

```bat
.\gradlew.bat generateHtml
.\gradlew.bat generateMarkdown
```

Include offline viewing example:

```bat
cd <output-dir>\api-docs-html
python -m http.server 8080
```

Then:

```text
http://localhost:8080/
```

- [ ] **Step G4: Document frontend design outcome**

If `docs/v2-frontend-design.md` exists from Task D, confirm it includes:

```text
reference resources used
design tokens
layout grid
component rules
responsive rules
accepted deviations
allowed implementation files
```

If it does not exist, create it with the structure from Task D2.

- [ ] **Step G5: Document visual QA outcome**

If screenshots were captured, `docs/v2-visual-qa.md` must include:

```text
paths to screenshots
desktop result
mobile result
search result
known deviations
final pass/fail
```

If screenshots could not be captured, state the blocker and do not claim visual completion.

---

## Task H: Final Verification and Checkpoint Update

**Files:**
- Modify: `D:/workspace/ApiDoc/docs/superpowers/plans/2026-05-27-apidoc-v2-progress-checkpoint.md`

- [ ] **Step H1: Run focused verification suite**

Run:

```bat
.\gradlew.bat :core:test --tests com.byd.apidoc.plugin.ApiDocPluginV2EndToEndTest --rerun-tasks
```

Expected:

```text
BUILD SUCCESSFUL
```

Run:

```bat
.\gradlew.bat :core:test --tests com.byd.apidoc.render.HtmlDevsiteRendererTest --tests com.byd.apidoc.projection.SearchAndNavV2Test --rerun-tasks
```

Expected:

```text
BUILD SUCCESSFUL
```

Run:

```bat
.\gradlew.bat :core:test --tests com.byd.apidoc.render.MarkdownRendererV2Test --rerun-tasks
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step H2: Run full verification suite**

Run:

```bat
.\gradlew.bat :core:test --rerun-tasks
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step H3: Update checkpoint**

Modify:

```text
D:/workspace/ApiDoc/docs/superpowers/plans/2026-05-27-apidoc-v2-progress-checkpoint.md
```

Set statuses:

```text
Task 9: End-to-End v2 Acceptance                   DONE
Task 10: v2 Documentation and Handoff              DONE
Build Web Apps Visual Design Pass                  DONE or BLOCKED WITH MISSING RESOURCES
```

Add evidence:

```text
Task 9 spec review result
Task 9 code quality review result
Build Web Apps design resource decision
files changed
tests run
screenshot paths
documentation files created/updated
known deviations
remaining work
```

- [ ] **Step H4: Final acceptance checklist**

Before reporting completion, verify each item:

```text
[ ] Task 9 spec review completed
[ ] Task 9 code quality review completed
[ ] focused Task 9 E2E test passes
[ ] full core test suite passes
[ ] Build Web Apps design pass completed or blocked with exact missing resources
[ ] frontend implementation keeps offline static architecture
[ ] generated HTML inspected in browser or screenshot fallback
[ ] desktop type page visually checked
[ ] mobile type page visually checked
[ ] search state visually checked
[ ] v2 output contract documented
[ ] Android DevSite target documented
[ ] portable usage updated
[ ] checkpoint updated
```

---

## Completion Report Format

When this plan is executed, report progress in this format:

```text
Current progress:
- Task 9: DONE / BLOCKED / PARTIAL
- Build Web Apps visual pass: DONE / BLOCKED / PARTIAL
- Task 10: DONE / BLOCKED / PARTIAL

Changed files:
- <file>

Verification:
- <command>: PASS / FAIL / NOT RUN

Visual QA:
- desktop screenshot: <path or not captured with reason>
- mobile screenshot: <path or not captured with reason>
- search screenshot: <path or not captured with reason>

Remaining:
- <exact remaining work or "none">
```

Do not say "v2 complete" unless Task H checklist is satisfied.

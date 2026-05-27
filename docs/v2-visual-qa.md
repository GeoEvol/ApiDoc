# ApiDoc v2 Visual QA

## Build Under Test

Current build resources:

```text
D:/workspace/ApiDoc/core/src/main/resources/apidoc-v2/assets/apidoc-devsite.css
D:/workspace/ApiDoc/core/src/main/resources/apidoc-v2/assets/apidoc-devsite.js
D:/workspace/ApiDoc/core/src/main/resources/apidoc-v2/assets/apidoc-search.js
```

Generated sample output observed under:

```text
D:/workspace/ApiDoc/core/build/test-devsite-html-renderer/api-docs-html/
```

## Reference Inputs

```text
C:/Users/Evol/Desktop/devsite/google/AdIdManager.html
C:/Users/Evol/Desktop/devsite/google/app.css
D:/workspace/test/build/modern-javadoc.css
D:/workspace/test/build/modern-javadoc.js
```

## Screenshots

Screenshot capture was completed with local Chrome headless after the initial approval timeout. Node Playwright was not available in this environment.

Required screenshots before final visual sign-off:

| State | Viewport | Path | Screenshot | Result |
| --- | --- | --- | --- | --- |
| ApiDoc type page | 1440x1200 | `api-docs-html/reference/com.example.sdk.AndroidMetadataApi.html` | `D:/workspace/ApiDoc/build/visual-qa/apidoc-v2-desktop-2.png` | pass |
| ApiDoc type page | 390x1200 | `api-docs-html/reference/com.example.sdk.AndroidMetadataApi.html` | `D:/workspace/ApiDoc/build/visual-qa/apidoc-v2-mobile-5.png` | pass |
| DevSite reference | 1440x1200 | `C:/Users/Evol/Desktop/devsite/google/AdIdManager.html` | `D:/workspace/ApiDoc/build/visual-qa/devsite-reference-desktop.png` | reference only |
| Search open state | 1440x1200 | generated type page served from `http://127.0.0.1:8765/` | `D:/workspace/ApiDoc/build/visual-qa/apidoc-v2-search-open-http.png` | pass |

## Desktop Type Page Checks

Desktop screenshot result:

```text
topbar remains sticky
left nav remains sticky and scrollable
right TOC remains sticky
main content does not exceed available width
long status chips wrap
member tables scan cleanly
code blocks scroll internally
```

Result: pass for the captured first viewport.

## Mobile Type Page Checks

Mobile screenshot result:

```text
topbar wraps without overlap
search remains reachable
nav toggle opens and closes the book nav
right TOC is hidden
member table rows stack
no page-level horizontal scroll
```

Result: pass for the captured first viewport after changing mobile status chips to vertical flow.

## Search Checks

Implemented behavior to verify visually:

```text
Ctrl/Cmd+K focuses search
search kind filter appears beside input
result panel aligns to topbar search
active result is visible
empty state is compact
```

Result: pass for search results under local HTTP serving. `file://` mode cannot be used to validate search because browser JSON fetch restrictions can prevent `search-index.json` from loading.

## Navigation Checks

Implemented behavior to verify visually:

```text
current nav item is highlighted
mobile nav closes after link click
Escape closes mobile nav
```

## Long Content Checks

Implemented CSS safeguards:

```text
overflow-wrap on nav links, breadcrumbs, headings, search rows, status chips
table-layout fixed on member summaries
mobile table row stacking
pre/code overflow contained inside code blocks
```

## Known Deviations

```text
No full DevSite runtime import.
No Google account/header widgets.
No remote search backend.
Local DevSite reference screenshot has degraded custom-element/icon text because it is opened as a standalone local file outside the full DevSite runtime.
Search open-state screenshot uses a temporary generated QA HTML file in `core/build/...` to prefill the search query. Source renderer files were not modified for that screenshot.
```

## Final Result

```text
Functional frontend implementation: pass.
Desktop type page visual QA: pass.
Mobile type page visual QA: pass.
Search open-state visual QA: pass under local HTTP server.
```

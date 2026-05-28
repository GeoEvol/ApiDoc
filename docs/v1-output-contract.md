# ApiDoc v1 Output Contract

This document defines the v1 filesystem contract produced by the Gradle plugin.
The contract is intentionally narrow: v1 proves a stable data-to-file pipeline,
not a polished documentation portal.

## Pipeline

```text
Javadoc Public API
  -> ApiDocDoclet
  -> DocCorpus + ApiMetadata
  -> DocProjection
  -> JSON outputs
  -> Markdown API Reference
  -> HTML API Reference
```

Renderers must consume `DocProjection` as the primary input. They may read
`DocCorpus` only for supplemental display data. Renderers must not re-scan
source files, re-run visibility filtering, or re-resolve links.

## Root Outputs

Each generation output directory contains the shared JSON files and the renderer
directory selected by the task. `generateMarkdown` writes `api-docs-md/`;
`generateHtml` writes `api-docs-html/`. The combined list below shows the paths
across both task types:

```text
doc-corpus.json
page-index.json
nav-index.json
search-index.json
output-manifest.json
api-docs-md/
api-docs-html/
```

`DocCorpus` preserves all elements visible to the current javadoc task. Public
versus internal documentation is controlled by `VisibilityPolicy` when building
`DocProjection`; parser code must not drop hidden or removed APIs from the
corpus.

## Manifest

`output-manifest.json` contains the shared JSON entries and only the renderer
entry that was actually generated:

```json
{
  "schemaVersion": "1.0",
  "generatorVersion": "1.0.0",
  "generatedAt": "ISO-8601 timestamp",
  "outputs": {
    "corpus": "doc-corpus.json",
    "pages": "page-index.json",
    "nav": "nav-index.json",
    "search": "search-index.json",
    "manifest": "output-manifest.json",
    "markdown": "api-docs-md/"
  }
}
```

An HTML run uses `"html": "api-docs-html/"` instead of the `markdown` entry.
The manifest is a file contract, not a complete build audit record.

The root output directory is reserved for v1 data files and the two v1 renderer
directories. Legacy root-level renderer files such as `index.md`, `index.html`,
`index-all.html`, `member-search-index.js`, or package/class pages outside
`api-docs-md/` and `api-docs-html/` are not part of the v1 contract.

## Markdown Layout

```text
api-docs-md/
  index.md
  packages.md
  classes.md
  package/{packageName}.md
  reference/{qualifiedName}.md
```

The Markdown renderer must preserve core API reference structure, member
anchors, internal links, external links, API metadata text, and block tags.
Pretty formatting and advanced table layout are outside v1.

## HTML Layout

```text
api-docs-html/
  index.html
  packages.html
  classes.html
  package/{packageName}.html
  reference/{qualifiedName}.html
  assets/
    apidoc.css
    apidoc.js
    search.js
  nav-index.json
  search-index.json
```

The HTML renderer is a static multi-page renderer. It does not provide SPA
routing, server-side APIs, CDN dependencies, or a full Android Developers visual
clone.

Search uses `api-docs-html/search-index.json` and must generate result links
relative to the current page.

## v1 Explicit Non-Goals

The following are intentionally outside v1:

```text
complete inherited member tables
complete {@inheritDoc}
API diff
API lint
Kotlin/Dokka support
Vue/React/Svelte renderer
full Doclava/Android Developers visual parity
full external-link federation
perfect file:// mode
```

# ApiDoc v2 Output Contract

## Scope

ApiDoc v2 emits a stable API reference data and rendering package:

```text
DocCorpus + ApiMetadata
  -> DocProjection
  -> JSON indexes
  -> Markdown API reference
  -> offline static HTML API reference
```

Renderer implementations must not require Javadoc API objects. Renderers consume `DocProjection` and may read `DocCorpus` only as a read-only supplemental source.

## Directory Layout

`generateMarkdown` and `generateHtml` share the JSON outputs, but each task
writes only its selected renderer directory. The combined layout below shows the
paths across both task types; one task run should not create both
`api-docs-md/` and `api-docs-html/`.

```text
<output>/
├── doc-corpus.json
├── page-index.json
├── nav-index.json
├── search-index.json
├── output-manifest.json
├── api-docs-html/
│   ├── index.html
│   ├── packages.html
│   ├── classes.html
│   ├── package/
│   ├── reference/
│   ├── assets/
│   ├── nav-index.json
│   └── search-index.json
└── api-docs-md/
    ├── index.md
    ├── packages.md
    ├── classes.md
    ├── package/
    └── reference/
```

## Manifest

`output-manifest.json` records the stable output entry points. The renderer
entry is format-specific: Markdown runs include `markdown`, HTML runs include
`html`.

```json
{
  "schemaVersion": "1.0",
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
`generatedAt` may vary by run and must not be used as a stable cache key.

## doc-corpus.json

`doc-corpus.json` is the normalized API fact model. It preserves elements scanned by the Javadoc task and is not filtered by the HTML renderer.

Expected top-level groups:

```text
packages
types
members
comments
annotations
refs
metadata
diagnostics
```

Visibility filtering belongs to `DocProjection`, not Corpus parsing.

## page-index.json

`page-index.json` is the render-oriented page list. It is filtered by the configured visibility policy.

Page entries include:

```text
id
kind
title
url
targetId
summary
metadata
```

## nav-index.json

`nav-index.json` contains the left API navigation tree:

```text
package node
  type-kind group
    type node
```

The HTML renderer also writes a copy into `api-docs-html/nav-index.json` for offline inspection and possible future client-side enhancements.

## search-index.json

`search-index.json` contains searchable packages, types, and members.

Search entries include:

```text
id
kind
label
qualifiedName
packageName
ownerName
url
anchor
summary
metadata
displaySignature
tokens
```

URLs are relative and must remain portable after copying the output directory.

## HTML Output

The built-in v2 HTML renderer emits a static multi-page site:

```text
api-docs-html/index.html
api-docs-html/packages.html
api-docs-html/classes.html
api-docs-html/package/<package>.html
api-docs-html/reference/<qualified-type>.html
api-docs-html/assets/apidoc-devsite.css
api-docs-html/assets/apidoc-devsite.js
api-docs-html/assets/search.js
```

HTML must remain offline safe:

```text
no CDN
no backend
no SPA runtime
no remote search service
no dependency on Google DevSite runtime JavaScript
```

## Markdown Output

The built-in Markdown renderer emits:

```text
api-docs-md/index.md
api-docs-md/packages.md
api-docs-md/classes.md
api-docs-md/package/<package>.md
api-docs-md/reference/<qualified-type>.md
```

Markdown uses relative links and stable anchors derived from `DocId`.

## DocProjection Fields

`DocProjection` provides:

```text
pages
typePages
nav
search
```

Renderers consume Projection first. They must not recompute visibility filtering, inherited members, link resolution, or API metadata.

## TypePageModel Fields

Type pages include:

```text
id
title
packageName
declaration
summary
comment
metadata
breadcrumbs
rightToc
apiStatus
typeHeader
inheritance
interfaces
memberGroups
memberDetails
inheritedMemberGroups
```

`inheritedMemberGroups` may be empty but should exist as a stable rendering surface.

## Inherited Member Fields

Inherited member groups include:

```text
ownerName
ownerQualifiedName
title
members
```

Member entries link to the original owner page and member anchor.

## ApiMetadata Fields

v2 metadata may include:

```text
hidden
removed
pending
deprecated
since
apiSince
sdkExtensionSince
deprecatedSince
removedSince
permissions
nullability
valueRanges
sourceTags
sourceAnnotations
```

Metadata must preserve source information where available.

## Search Entry Fields

Search entries should carry enough information for Android-style scanning:

```text
kind
label
qualifiedName
packageName
ownerName
url
anchor
summary
displaySignature
tokens
metadata/status
```

## Compatibility Rules

```text
Keep schemaVersion stable until a breaking schema change is intentional.
Keep output paths relative.
Keep HTML assets local.
Keep renderers independent from Javadoc API objects.
Keep VisibilityPolicy in Projection.
Keep unknown/custom Javadoc tags in CommentDoc.
```

## Non-Goals

```text
No full StandardDoclet clone.
No full Doclava clone.
No API diff/lint in this output contract.
No Kotlin/Dokka output in v2.
No VitePress/Docusaurus as the primary renderer.
```

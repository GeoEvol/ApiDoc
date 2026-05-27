# ApiDoc v2 Frontend Design

## Product Positioning

ApiDoc v2 renders an offline Java API reference from `DocProjection`. The HTML output is a static multi-page site intended for local hosting or zip distribution. The visual target is Android Developers / Google Developers API Reference density and hierarchy, not a pixel-perfect clone.

## Reference Inputs

Primary local DevSite resources:

```text
C:/Users/Evol/Desktop/devsite/google/AdIdManager.html
C:/Users/Evol/Desktop/devsite/google/app.css
C:/Users/Evol/Desktop/devsite/google/devsite_devsite_search_module__zh_cn.js
C:/Users/Evol/Desktop/devsite/google/devsite_devsite_book_nav_module__zh_cn.js
C:/Users/Evol/Desktop/devsite/google/devsite_devsite_toc_module__zh_cn.js
```

User-designed frontend resources found in `D:/workspace`:

```text
D:/workspace/test/build/modern-javadoc.css
D:/workspace/test/build/modern-javadoc.js
D:/workspace/test/docs/modern-javadoc.css
D:/workspace/test/docs/modern-javadoc.js
D:/workspace/JDK/jdk/modern-javadoc.css
D:/workspace/JDK/jdk/modern-javadoc.js
```

The `modern-javadoc` assets are useful as design references for compact API layout, sidebar behavior, right TOC behavior, search ergonomics, copy-anchor interaction, and long-signature handling. They are not copied directly because they target the JDK StandardDoclet DOM and some copies contain encoding artifacts in comments.

Build Web Apps image generation is not required for this pass because the user supplied an existing target design system and local DevSite frontend resources. The accepted design source is the local DevSite reference plus the existing ApiDoc v2 renderer structure.

## Layout Grid

Desktop:

```text
top app bar
left API navigation rail
main API content
right on-this-page TOC
```

The main shell uses fixed navigation rails and a fluid content column:

```text
left nav: 280px
content: minmax(0, 1fr)
right TOC: 240px
```

The content column keeps long API signatures within the available width through `min-width: 0`, wrapping, and horizontal scrolling only inside code blocks.

## Breakpoints

```text
1280px: reduce side rail widths and gaps
1120px: hide right TOC, keep left nav
760px: collapse left nav below the top bar, make search a full-width second row
```

## Typography

Use system fonts only to preserve offline portability:

```text
body/UI: system-ui, Segoe UI, Roboto, Arial, sans-serif
code/API signatures: SFMono-Regular, Consolas, Liberation Mono, Menlo, monospace
```

The type scale stays compact:

```text
body: 14px / 1.58
metadata/nav/search: 12px to 13px
type h1: 32px desktop, 26px mobile
section h2: 20px
member h3: 17px
```

## Color Tokens

The palette follows Google/Android neutral surfaces and blue action color:

```text
background: #ffffff
surface: #ffffff
surface alt: #f8fafd
surface strong: #f1f3f4
text: #202124
muted: #5f6368
border: #dadce0
primary: #1a73e8
deprecated: #b06000
removed: #b3261e
pending: #5f6368
```

## Spacing and Radius

Spacing is compact and table-oriented:

```text
topbar horizontal padding: 24px desktop, 14px mobile
content padding: 32px 40px desktop, 22px 18px mobile
member row padding: 12px 14px
small radius: 4px
medium radius: 8px
```

No decorative gradients, no nested cards, no heavy shadows.

## App Bar

The app bar is sticky, white, and separated by a one-pixel border. It contains:

```text
mobile nav toggle
project brand
search input
search kind select
search results panel
```

The search panel is local and reads `search-index.json`. It must not call any remote service.

## Left Navigation

The left navigation mirrors DevSite book navigation:

```text
Packages
Classes
package sections
type kind groups
type links
current link state
```

Desktop behavior is sticky and scrollable. Mobile behavior collapses under the app bar and is toggled by the `Menu` button.

## Right TOC

The right TOC is sticky on desktop and hidden below 1120px. Active section highlighting is progressive enhancement in local vanilla JavaScript.

## Search

Search behavior:

```text
Ctrl/Cmd+K focuses search
typing opens results
kind select filters results
ArrowUp/ArrowDown moves active result
Enter follows active result
Escape closes results
outside click closes results
```

Search result rows show:

```text
label
kind/signature
owner or package
```

## Type Header

Type pages show:

```text
breadcrumbs
package name
type name
API status chips
type declaration
summary/body
extends/implements
member summaries
member details
inherited members
```

The declaration block uses the same code surface as method declarations.

## API Status Labels

Status chips are semantically styled:

```text
deprecated: amber
removed: red
hidden: neutral
pending: neutral
since/api/sdk extension: blue
permissions: green
nullability/ranges: neutral/cyan
```

Long status text may wrap and should not force horizontal page scroll.

## Member Summary

Member summary sections use full-width tables on desktop. The first column is the member name/signature entry; the second column is summary text. On mobile, rows collapse into stacked blocks.

## Member Detail

Member detail sections are separated by borders. Each detail has:

```text
member heading
copy-anchor button
status chips
declaration code block
comment body
block tags
```

## Inherited Members

Inherited members stay visibly separate from declared member summaries. They should scan like compact reference rows, not as a marketing card list.

## Responsive Rules

Required outcomes:

```text
No text overlap on mobile.
No page-level horizontal scrolling.
Long package/type names wrap.
Long method declarations scroll inside code blocks only.
Search stays reachable.
Left nav does not cover content after link activation.
Right TOC never overlays content.
```

## Allowed Implementation Files

```text
D:/workspace/ApiDoc/core/src/main/resources/apidoc-v2/assets/apidoc-devsite.css
D:/workspace/ApiDoc/core/src/main/resources/apidoc-v2/assets/apidoc-devsite.js
D:/workspace/ApiDoc/core/src/main/resources/apidoc-v2/assets/apidoc-search.js
D:/workspace/ApiDoc/core/src/main/groovy/com/byd/apidoc/render/html/HtmlSiteRenderer.groovy
D:/workspace/ApiDoc/core/src/main/groovy/com/byd/apidoc/render/html/HtmlPageShellRenderer.groovy
```

## Explicit Non-Goals

```text
No React/Vue/Svelte/Vite runtime.
No VitePress/Docusaurus theme.
No CDN fonts or icons.
No full DevSite JavaScript import.
No parser/model/projection changes for visual convenience.
No generic docs portal redesign.
No landing-page hero.
```

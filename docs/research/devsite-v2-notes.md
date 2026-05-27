# DevSite v2 Reference Notes

Source files inspected:

- `C:/Users/Evol/Desktop/devsite/google/AdIdManager.html`
- `C:/Users/Evol/Desktop/devsite/google/app.css`
- `C:/Users/Evol/Desktop/devsite/google/devsite_devsite_search_module__zh_cn.js`
- `C:/Users/Evol/Desktop/devsite/google/devsite_devsite_book_nav_module__zh_cn.js`
- `C:/Users/Evol/Desktop/devsite/google/devsite_devsite_toc_module__zh_cn.js`

DevSite assets are visual/interaction references only. ApiDoc v2 should not import the full DevSite JS runtime.

## Shell Inventory

| Region | Observed behavior | ApiDoc v2 implication |
| --- | --- | --- |
| Header | `AdIdManager.html` uses `devsite-header` with upper tabs, logo row, hamburger button, search, appearance selector, language selector, and account controls. | ApiDoc offline shell only needs a compact header with product name, search box, and mobile nav toggle. Avoid sign-in, analytics, language, and dynamic app loader features. |
| Left nav | `devsite-book-nav` contains a filter input, mobile header, nested nav list, active-link tracking, expand/collapse behavior, mobile sitemask close, and scroll-to-active behavior. Items can carry `data-version-added`. | Generate static left nav from `nav-index.json`. Add optional client-side filtering and active-link highlighting with small local JS. Preserve `data-version-added` or equivalent metadata for future API-level filtering. |
| Main content | `devsite-content` and `.devsite-main-content` wrap article body, breadcrumbs/page title, and reference content. The page uses dense tables and many anchors. | Renderer should emit a three-column docs layout: left nav, article, right TOC. Content should be readable offline with no JS. |
| Right TOC | `devsite-toc` scans `h2` and `h3` headings with IDs and `data-text`, builds nested links, highlights active heading via `IntersectionObserver`, hides when empty, and supports embedded/mobile mode with "more" expansion. | Projection should provide TOC entries; renderer should also set heading IDs. Local JS can only handle active-state/highlight and mobile expansion. |
| Footer/utility | DevSite includes a large footer, language selector, analytics, snackbar, tooltips, heading-link custom element, and app loader. | Skip in v2 unless needed. A small footer with generator/version metadata is enough for offline output. |

## Search Affordances

Observed in `devsite_devsite_search_module__zh_cn.js` and `AdIdManager.html`:

- Search form posts to `/s/results` with query parameter `q`.
- Search input has open/close buttons, `aria-controls`, `aria-expanded`, `aria-activedescendant`, and a visible `/` keyboard shortcut.
- Suggestions are grouped into query suggestions, pages, reference pages, and products.
- Keyboard navigation uses arrow keys and `aria-selected`.
- Results include title fragments, product/project context, reference context, and highlighted matched text.

ApiDoc v2 implication: keep a local static search affordance with accessible input, slash shortcut, grouped results, and keyboard navigation. The backing data should be ApiDoc `search-index.json`, not DevSite services.

## Left Nav Behavior

Observed in `devsite_devsite_book_nav_module__zh_cn.js`:

- A hamburger/toggle controls collapsed state and updates `aria-expanded`.
- Expandable nav sections toggle on click/Enter unless the target is a direct link.
- Current page gets `.devsite-nav-active` and `aria-current=page`.
- Filter input hides non-matching items, highlights text with `mark`, shows descendant match counts, and has a clear button.
- Mobile navigation closes when a leaf link is selected.

ApiDoc v2 implication: implement a small `apidoc-nav.js` with active link, expandable groups, filter, and mobile close. Do not import the DevSite module.

## Right TOC Behavior

Observed in `devsite_devsite_toc_module__zh_cn.js`:

- TOC source headings are `h2` and `h3`, excluding `.hide-from-toc`, `#contents`, and `#table-of-contents`.
- Items require heading IDs and `data-text`.
- Active item tracks visible headings with `IntersectionObserver`.
- Embedded/mobile mode initially shows fewer items and supports expand/collapse when there are more than eight entries.

ApiDoc v2 implication: projection should emit right TOC entries; the renderer should output `data-text` on headings. JS should progressively enhance active heading and compact/mobile expansion only.

## Tables, Code, and Density

Observed in `app.css`:

- Theme variables include `--devsite-primary-text-color`, `--devsite-secondary-text-color`, `--devsite-link-color`, `--devsite-background-1` through `--devsite-background-5`, `--devsite-primary-font-family`, `--devsite-heading-font-family`, and `--devsite-code-font-family`.
- Code variables include `--devsite-code-background`, `--devsite-code-color`, `--devsite-code-comments-color`, `--devsite-code-keywords-color`, `--devsite-code-numbers-color`, `--devsite-code-strings-color`, and `--devsite-code-types-color`.
- Code blocks use a monospace font and approximately `14px/20px`; inline code is smaller/dense.
- Tables are wrapped with `.devsite-table-wrapper` patterns and reset first/last child margins inside table cells.
- Main content variables include max width and padding, with docs layout using generous desktop side padding and smaller mobile layouts.

ApiDoc v2 implication: adapt a small token set rather than copy the full CSS. Use neutral Google-like text/background/link/code tokens and simple table wrappers with overflow on mobile.

## Mobile Breakpoints

Observed breakpoints in `app.css` and modules:

- `max-width: 840px` is repeatedly used for tablet/mobile layout changes.
- `max-width: 600px` is used for phone-specific tightening.
- Some content-specific breakpoints also appear around `800px`, `1000px`, and `1200px`.

ApiDoc v2 implication: use `840px` as the primary nav/sidebar breakpoint and `600px` for tighter article/table/code adjustments. Wider desktop can use a fixed left nav and right TOC.

## CSS Variables Worth Adapting

| Variable | Suggested ApiDoc role |
| --- | --- |
| `--devsite-primary-text-color` | `--apidoc-text-primary` |
| `--devsite-secondary-text-color` | `--apidoc-text-secondary` |
| `--devsite-link-color` | `--apidoc-link` |
| `--devsite-background-1` | `--apidoc-surface` |
| `--devsite-background-2` | `--apidoc-surface-subtle` |
| `--devsite-background-3` | `--apidoc-code-bg` / hover bg |
| `--devsite-background-5` | `--apidoc-border` |
| `--devsite-primary-font-family` | `--apidoc-font-body` |
| `--devsite-heading-font-family` | `--apidoc-font-heading` |
| `--devsite-code-font-family` | `--apidoc-font-code` |
| `--devsite-search-height` | `--apidoc-search-height` |

## Practical v2 Notes

- Build a DevSite-inspired shell: fixed header, collapsible/filterable left nav, central article, right TOC, and local search.
- Keep everything offline and static. No remote fonts, app loader, analytics, sign-in, language selector, or DevSite custom elements are required.
- Benchmark visual density against `C:/Users/Evol/Desktop/devsite/google/AdIdManager.html` before any Build Web Apps visual pass.

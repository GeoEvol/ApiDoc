# ApiDoc v2 Android DevSite Target

## Positioning

ApiDoc v2 targets the information structure and density of Android Developers / Google Developers API Reference. It does not import DevSite runtime code and does not attempt a pixel-perfect clone.

The goal is an offline API reference that feels familiar to Android SDK users:

```text
left package/type navigation
central API reference content
right on-this-page navigation
integrated search
API status labels
member summaries
member details
inherited members
```

## Reference Sources

Primary local references:

```text
C:/Users/Evol/Desktop/devsite/google/AdIdManager.html
C:/Users/Evol/Desktop/devsite/google/app.css
C:/Users/Evol/Desktop/devsite/google/devsite_devsite_search_module__zh_cn.js
C:/Users/Evol/Desktop/devsite/google/devsite_devsite_book_nav_module__zh_cn.js
C:/Users/Evol/Desktop/devsite/google/devsite_devsite_toc_module__zh_cn.js
```

Additional references:

```text
D:/workspace/test/build/modern-javadoc.css
D:/workspace/test/build/modern-javadoc.js
D:/workspace/JDK/jdk/modern-javadoc.css
D:/workspace/JDK/jdk/modern-javadoc.js
D:/workspace/doclava/doclava-refs-tags-android-15.0.0_r10/res/assets/templates-sdk/assets/android-developer-docs.css
D:/workspace/JDK/jdk/src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/formats/html/resources/stylesheet.css
```

## What We Match

```text
API reference density
top search entry point
left book navigation
right TOC
compact member tables
type header anatomy
status labels
offline-friendly MPA behavior
responsive collapse behavior
```

## What We Do Not Clone

```text
Google account/user widgets
analytics
feedback widgets
service workers
remote search backend
full DevSite custom elements
DevSite build/runtime pipeline
pixel-perfect branding
```

## Page Types

v2 HTML includes:

```text
index.html
packages.html
classes.html
package/<package>.html
reference/<qualified-type>.html
```

## Type Page Anatomy

Type pages render:

```text
breadcrumbs
package name
type name
API status
declaration
summary/body
extends
implements
member summaries
member details
inherited members
right TOC
```

## Navigation Model

The left navigation is built from `DocProjection.nav`, grouped by package and type kind. It is not generated from the DOM at runtime.

## Search Model

Search is local and uses `search-index.json`. It supports:

```text
query text
kind filter
keyboard navigation
relative URLs
anchors
empty result state
```

## API Status Model

Status chips are derived from `ApiStatusModel`:

```text
hidden
deprecated
removed
pending
since/apiSince/sdkExtensionSince
deprecatedSince/removedSince
permissions
nullability
valueRanges
```

CSS styles these chips by semantic class. Rendering still comes from Projection data.

## Inherited Members

Inherited members are resolved by the projection layer and rendered as a separate section. The HTML renderer does not compute inheritance.

## Responsive Behavior

Desktop:

```text
sticky topbar
sticky left nav
main content
sticky right TOC
```

Tablet:

```text
left nav + main content
right TOC hidden
```

Mobile:

```text
topbar wraps
search is full width
left nav toggles under header
right TOC hidden
member tables collapse into stacked rows
```

## Offline Constraints

The HTML output must work after copying to another machine and serving locally:

```text
cd api-docs-html
python -m http.server 8080
```

No remote resources are required.

## Build Web Apps Design Pass

Build Web Apps is used as a visual design discipline, not as permission to change architecture. It may improve:

```text
CSS tokens
layout density
responsive behavior
search/nav/TOC interaction polish
status chip styling
member table readability
```

It may not introduce:

```text
SPA framework
CDN dependency
backend service
DevSite runtime import
schema changes for visual convenience
```

## Future Work

```text
visual screenshot QA once local Chrome/Browser screenshot execution is approved
more DevSite-like package/type index density
member filters
file:// search-data.js mode
dark mode, if explicitly required
```

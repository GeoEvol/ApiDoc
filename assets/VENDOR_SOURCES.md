# Vendor Sources

This file records frontend repositories used as runtime assets or design references for the Javadoc-compatible documentation theme.

## Runtime vs Reference

- `assets/`: source reading, reference notes, and offline provenance.
- `core/src/main/resources/theme/vendor/`: files copied into generated documentation output.

Do not copy full source repositories into `theme/vendor/`.

## Prism.js

- Source: https://github.com/PrismJS/prism
- Local path: `D:\workspace\prism`
- Commit: `ded4a65b75a246b4dbc6c5a84e584db1078529aa`
- Package version: `1.29.0`
- Usage: Java/Groovy/Kotlin code highlighting.
- Runtime in Phase 1.5: yes.
- Runtime target:
  - `core/src/main/resources/theme/vendor/prism/prism.js`
  - `core/src/main/resources/theme/vendor/prism/prism.css`
- Runtime artifact source:
  - `D:\workspace\prism.js`
  - `D:\workspace\prism.css`
- Notes: runtime files were supplied as pinned browser bundle artifacts outside the source checkout and copied into `theme/vendor/prism/`.

## Lucide

- Source: https://github.com/lucide-icons/lucide
- Local path: `D:\workspace\lucide`
- Commit: `5b40f2c5a76a27eeb81c8f1b1c311121dee45495`
- Package: `packages/lucide`
- Usage: icons for search, copy, navigation, and theme controls.
- Runtime in Phase 1.5: yes, selected static SVG icons.
- Runtime path:
  - `core/src/main/resources/theme/vendor/lucide/icons/search.svg`
  - `core/src/main/resources/theme/vendor/lucide/icons/copy.svg`
  - `core/src/main/resources/theme/vendor/lucide/icons/chevron-down.svg`
  - `core/src/main/resources/theme/vendor/lucide/icons/sun.svg`
  - `core/src/main/resources/theme/vendor/lucide/icons/moon.svg`
- Notes: local checkout does not contain built `packages/lucide/dist/umd/lucide.js`, so Phase 1.5 uses pinned SVG assets instead of the JS package.

## github-markdown-css

- Source: https://github.com/sindresorhus/github-markdown-css
- Local path provided: `D:\workspace\github-markdown-css`
- Commit: `41ae7d13c1f762b43e2cb67b0b166bd8f81517c0`
- Usage: scoped doc comment typography under `.markdown-body`.
- Runtime in Phase 1.5: yes.
- Runtime target:
  - `core/src/main/resources/theme/vendor/github-markdown-css/github-markdown.css`
- Runtime artifact source:
  - `D:\workspace\github-markdown.css`
- Notes: runtime CSS was supplied as a pinned generated artifact outside the source checkout and copied into `theme/vendor/github-markdown-css/`.

## Just the Docs

- Source: https://github.com/just-the-docs/just-the-docs
- Local path: `D:\workspace\just-the-docs`
- Commit: `f43d7cfc4b1e18a97e6d6d20b88f1aaeda0e7196`
- Usage: layout, sidebar, navigation, spacing, and responsive behavior reference.
- Runtime in Phase 1.5: no.
- Read focus:
  - `_sass/layout.scss`
  - `_sass/navigation.scss`
  - `_sass/search.scss`

## Pagefind

- Source: https://github.com/Pagefind/pagefind
- Local path: `D:\workspace\pagefind`
- Commit: `a7e26d53da78d4b89454d7b31bc8c7024e94b165`
- Usage: future full-text search reference.
- Runtime in Phase 1.5: no.
- Read focus:
  - `pagefind_ui/component/README.md`
  - `pagefind_ui/component/css/pagefind-component-ui.css`

## OpenJDK Javadoc

- Local reference path: `D:\workspace\JDK\jdk`
- Usage: Javadoc path, anchor, search index, navigation, and page behavior reference.
- Runtime in Phase 1.5: no.
- Constraint: use public Java 17 Doclet APIs in this project; do not depend on `jdk.javadoc.internal.*`.

## External Design References

- Android API reference: https://developer.android.com/reference
- Android package index: https://developer.android.com/reference/packages
- Android class page example: https://developer.android.com/reference/android/view/View
- Oracle Java 17 API overview: https://docs.oracle.com/en/java/javase/17/docs/api/
- Oracle Java 17 class page example: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/String.html

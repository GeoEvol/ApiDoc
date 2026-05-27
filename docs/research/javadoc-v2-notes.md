# Javadoc v2 Reference Notes

Source files inspected:

- `D:/workspace/JDK/jdk/src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/toolkit/util/VisibleMemberTable.java`
- `D:/workspace/JDK/jdk/src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/formats/html/ClassWriter.java`
- `D:/workspace/JDK/jdk/src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/formats/html/AbstractMemberWriter.java`
- `D:/workspace/JDK/jdk/src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/formats/html/taglets/InheritDocTaglet.java`
- `D:/workspace/JDK/jdk/src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/formats/html/taglets/ThrowsTaglet.java`
- `D:/workspace/JDK/jdk/src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/formats/html/Navigation.java`

These are reference-only notes. ApiDoc v2 must not depend on `jdk.javadoc.internal.*`.

## Behaviors to Preserve or Adapt

| Area | Observed behavior | ApiDoc v2 implication |
| --- | --- | --- |
| Member grouping | `VisibleMemberTable.Kind` separates `NESTED_CLASSES`, `ENUM_CONSTANTS`, `FIELDS`, `CONSTRUCTORS`, `METHODS`, annotation required/optional members, and `PROPERTIES`. Summary sets differ for normal types, enums, and annotation types. Detail sets exclude nested classes and constructors for enums. | Keep ApiDoc projection member groups explicit and stable. Use Android labels, but model enum constants, fields/constants, constructors, methods, nested types, and annotation members separately enough for renderers to choose labels/order. |
| Summary order | `AbstractMemberWriter.summaryKinds` orders nested classes, enum constants, properties, fields, constructors, annotation members, then methods. | Do not rely on raw parse order in the renderer. Projection should emit ordered groups. Android-style pages should likely use: nested types, constants, fields, constructors, methods, inherited groups, then details. |
| Detail order | `AbstractMemberWriter.detailKinds` orders enum constants, properties, fields, constructors, annotation members, methods. `ClassWriter` loops summary writers and detail writers separately. | Preserve a summary/detail split in `DocProjection`. Details should be anchor-addressable and renderer-neutral so HTML and Markdown can share the same projection fields. |
| Inherited fields/nested classes | Visible members are gathered from parents, de-duplicated, then filtered for accessibility and hidden-by-local-member checks. Local members appear before inherited members. | `InheritedMemberResolver` should filter by visibility and local hiding before rendering. It should group inherited entries by declaring type, not append them into local summary groups. |
| Inherited methods | Inheritance filtering follows JLS visibility, excludes static interface methods, removes lower-priority interface definitions when an override exists, handles local hides/overrides, and tracks "simple override" metadata. | Task 3 can start with visible inherited methods grouped by owner and filtered by local override signature. Full simple-override semantics can be deferred, but the projection should leave room for `overriddenDocId` and `simpleOverride`. |
| Inherited summary rendering | `AbstractMemberWriter` shows inherited summaries only for fields, methods, nested classes, and properties. It emits inherited TOC entries only when own member count is substantial. It skips a special Object-only TOC case for class methods. | ApiDoc v2 should implement inherited member sections after local summaries, grouped by owner type. Right TOC can include a single "Inherited members" entry first; per-owner nested TOC can come later. |
| `{@inheritDoc}` lookup | `InheritDocTaglet` uses `DocFinder` over overridden methods. If `{@inheritDoc Type}` names a supertype, it validates that the current method overrides a method in that type. It can replace main descriptions or delegate to inheritable taglets depending on containing tag. Missing docs warn. | Implement inherited members before full inline `{@inheritDoc}` expansion. Later inherit-doc support needs a resolver over override/interface chains and should preserve warnings when no source doc exists. |
| `@throws` inheritance | `ThrowsTaglet` handles `{@inheritDoc}` inside `@throws` specially. It documents explicit tags first, then method throws-clause exceptions with inherited tags, then fallback exceptions with no inherited docs. Constructors do not participate in inherited throws lookup. | Do not treat throws inheritance as generic inline text replacement. Store throws tags as structured block tags in `DocCorpus`, and implement throws inheritance as a later resolver step. |
| Heading/TOC shape | `ClassWriter` builds a type heading, description, member summaries, and member details. It adds table-of-contents links for description, summary groups, and details. `Navigation` builds breadcrumb links for modules/packages/nested classes, with undocumented enclosing classes rendered as text. | Projection should expose breadcrumbs and right TOC entries independent of HTML. Use stable anchors for description, summary groups, detail groups, and individual members. |

## Search Index

Search index behavior was not in the requested file list. Adjacent files found via `rg`:

- `D:/workspace/JDK/jdk/src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/toolkit/util/IndexBuilder.java`
- `D:/workspace/JDK/jdk/src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/toolkit/util/IndexItem.java`
- `D:/workspace/JDK/jdk/src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/toolkit/util/DocPaths.java`
- `D:/workspace/JDK/jdk/src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/formats/html/resources/search.js.template`
- `D:/workspace/JDK/jdk/src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/formats/html/resources/script.js.template`
- `D:/workspace/JDK/jdk/src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/formats/html/SearchWriter.java`

Observed adjacent shape: javadoc emits separate JS search indexes for modules, packages, types, members, and tags, with `IndexItem` carrying label, kind, URL, and containing module/package/class metadata.

## Practical v2 Notes

- Keep the v1 public Doclet API pipeline. Use these files only as behavioral reference.
- Add projection-level groups and TOC models before changing renderer templates.
- For inherited members, implement owner-grouped inherited summaries before attempting exact javadoc simple-override and `{@inheritDoc}` semantics.
- Search should stay a generated ApiDoc JSON index, but it should contain enough metadata for Android-style scoped results: package, type, member kind, status, anchor, and owning type.

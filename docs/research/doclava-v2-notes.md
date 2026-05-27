# Doclava v2 Reference Notes

Source files inspected:

- `D:/workspace/doclava/doclava-refs-tags-android-15.0.0_r10/src/com/google/doclava/ClassInfo.java`
- `D:/workspace/doclava/doclava-refs-tags-android-15.0.0_r10/src/com/google/doclava/Comment.java`
- `D:/workspace/doclava/doclava-refs-tags-android-15.0.0_r10/src/com/google/doclava/TagInfo.java`
- `D:/workspace/doclava/doclava-refs-tags-android-15.0.0_r10/src/com/google/doclava/NavTree.java`
- `D:/workspace/doclava/doclava-refs-tags-android-15.0.0_r10/src/com/google/doclava/FederationTagger.java`
- `D:/workspace/doclava/doclava-refs-tags-android-15.0.0_r10/src/com/google/doclava/SinceTagger.java`

Doclava is a reference only. ApiDoc v2 must not depend on Doclava.

## Behaviors to Preserve or Adapt

| Area | Observed behavior | ApiDoc v2 implication |
| --- | --- | --- |
| `@hide`, `@removed`, `@pending` | `Comment` treats `@hide`, `@removed`, `@pending`, and `@doconly` as recognized tags. `isHidden` checks `@hide` or `@pending`; `isRemoved` checks `@removed`. `ClassInfo` propagates hidden/removed status through containing packages, ancestors, and hide annotations. | Preserve raw tags in `DocCorpus`; apply visibility in `DocProjection`. `@pending` should map to hidden unless the visibility policy says otherwise. Removed should be distinct from hidden so v2 can support removed API output when configured. |
| `@apiSince`, `@sdkExtSince` | `Comment` recognizes `@apiSince` and `@sdkExtSince`, trims values, and reports multiple `@sdkExtSince` tags as an error. `@since` is noted as not used by Android docs in favor of `@apiSince`. | `ApiMetadata` should keep `since`, `apiSince`, and `sdkExtSince` separately. Android API badges should prefer `apiSince`; `sdkExtSince` should be rendered as extension metadata, not merged into `since`. |
| Since tagging | `SinceTagger` reads apicheck XML versions in order and fills missing version data on packages, classes, constructors, fields, and methods. It warns/errors for documented symbols missing version data. It checks ancestor specs for methods before applying method versions. | ApiDoc v2 does not need apicheck XML in Task 1, but projection fields should be able to hold externally supplied API-level data later. Do not bake the source of version data into the renderer. |
| InheritedTags | `TagInfo.makeHDF` accepts `InheritedTags`. If local tags are empty, it recursively emits inherited tags. If a local tag is `@inheritDoc`, it replaces that tag with inherited tags. `InheritedTags` itself is defined in `InheritedTags.java`; implementations are in `MethodInfo.java` for inline, first sentence, and return tags. | The requested files reveal use sites, but the next files to inspect for full behavior are `D:/workspace/doclava/doclava-refs-tags-android-15.0.0_r10/src/com/google/doclava/InheritedTags.java` and `D:/workspace/doclava/doclava-refs-tags-android-15.0.0_r10/src/com/google/doclava/MethodInfo.java`. ApiDoc should defer full inherit-doc expansion until a structured comment resolver exists. |
| ClassInfo local member groups | `ClassInfo` stores constructors, self methods, annotation elements, fields, enum constants, inner classes, interfaces, superclass, and all/self caches separately. `makeHDF` emits enum constants, constants, fields, constructors by visibility, methods by visibility, and inherited groups. | Projection should model local groups separately from inherited groups. Constants can be a filtered view of fields, but the projection should emit a distinct constants group for Android-style pages. |
| ClassInfo inherited member groups | `ClassInfo.makeHDF` emits `class.inherited.N` sections for superclasses and interfaces, with methods, fields, and constants. It skips the current class and groups by the inherited owner. | ApiDoc inherited member groups should be owner-grouped and include owner type link, owner kind, and lists for methods/fields/constants. This can be implemented without rendering raw corpus directly. |
| Hidden superclass/interface handling | `ClassInfo.interfaces()` gathers interfaces from hidden superclasses so visible APIs do not lose interface information. Superclass methods/fields are cloned for the current owner when gathered. | Visibility filtering must not make type hierarchy information disappear. Projection should preserve direct visible hierarchy plus enough hidden-ancestor flattening to keep inherited API useful. |
| External/federated links | `FederationTagger` registers named external sites with base URL and optional API XML. It matches local package/class/member symbols against the federated API and tags packages, classes, constructors, methods, and fields with federation metadata. | ApiDoc v2 can later support an external link map keyed by package/type/member identity. Keep link metadata in projection/search entries, not hard-coded in templates. |
| NavTree grouping | `NavTree` creates a Reference root, package nodes, and per-package collapsible groups: Annotations, Interfaces, Classes, Enums, Exceptions, Errors. It can output YAML v2 with grouped classes and version-added metadata. | ApiDoc nav should group package children by type kind, with package/type `since` metadata available for badges or filtering. Left nav should be generated from projection/nav JSON. |

## Practical v2 Notes

- Treat `@hide`, `@removed`, `@pending`, `@apiSince`, and `@sdkExtSince` as first-class metadata in the public model.
- Preserve unknown/custom tags in `DocCorpus`; do not drop tags just because v2 does not render them yet.
- Implement owner-grouped inherited members before `InheritedTags`/`@inheritDoc` expansion.
- Do not copy Doclava HDF concepts into ApiDoc. Use Doclava to inform projection fields and static renderer behavior.

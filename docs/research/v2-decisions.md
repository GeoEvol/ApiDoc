# ApiDoc v2 Decisions

These decisions apply to the Android Developers / Google Developers-inspired v2 work.

| Decision | Rationale | Implementation implication |
| --- | --- | --- |
| No runtime dependency on `jdk.javadoc.internal.*`. | OpenJDK javadoc internals are useful behavioral references, but they are not stable public API. | Keep ApiDoc on the public Javadoc Doclet API. Any javadoc behavior copied into ApiDoc must be reimplemented through project-owned model/resolver code. |
| No runtime dependency on Doclava. | Doclava is a legacy Android documentation implementation and a reference source only. | Use Doclava to inform tags, metadata, inherited members, federation, and nav grouping. Do not link or shade Doclava. |
| No full DevSite JS import. | DevSite JS depends on large app infrastructure, remote services, custom elements, analytics, and dynamic navigation. | Write small local JS for offline search, nav filtering, active nav state, mobile drawer, and right TOC active-heading enhancement. |
| Use a DevSite-inspired static shell. | The product target is Android/Google Developers API reference density, not a pixel clone or hosted DevSite app. | Generate static HTML/CSS/JS assets that work from disk or a static host, with no CDN/runtime service dependency. |
| Preserve the v1 JSON contract. | Existing v1 outputs are the stable pipeline contract. v2 should enrich rather than break it. | Keep `doc-corpus.json`, `page-index.json`, `nav-index.json`, `search-index.json`, and `output-manifest.json` compatible. Add fields in a backward-compatible way. |
| Extend `DocProjection` rather than renderer reading raw `DocCorpus`. | Projection is where visibility, navigation, grouping, and page structure belong. Renderer templates should stay dumb and deterministic. | Add breadcrumbs, right TOC, API status, Android-style member groups, and inherited member groups to projection models. Renderers consume projection plus targeted corpus lookups only where the existing architecture already requires content detail. |
| Implement inherited members before full `{@inheritDoc}`. | Inherited member lists are structural and needed for Android-style pages. Full comment inheritance is more complex, especially for inline tags, first sentences, return tags, and throws tags. | Build owner-grouped inherited member summaries first. Add placeholders/metadata for later inherited docs resolution without blocking v2 shell work. |
| Keep Markdown renderer compatible with new projection fields. | v2 enriches shared projection data, so non-HTML outputs must not break when new fields appear. | New projection fields should be render-neutral and optional/defaulted. Markdown can ignore or lightly render them until parity work is scheduled. |
| Build Web Apps visual pass is optional. | The primary work can proceed with a restrained static shell; visual escalation is only needed if structure is correct but polish is weak. | If used, benchmark `C:/Users/Evol/Desktop/devsite/google` first and `D:/workspace` resources second. If local resources are insufficient, stop and list missing screenshots, CSS/JS, fonts, icons, sample pages, or repositories before continuing. |

## Current Sequencing

1. Keep v1 pipeline: `Javadoc Public API -> DocCorpus + ApiMetadata -> DocProjection -> renderers`.
2. Add projection fields for v2 structure before rewriting HTML.
3. Add basic inherited member groups before full `{@inheritDoc}`.
4. Update HTML renderer and assets to use DevSite-inspired static layout.
5. Keep Markdown working with the enriched projection.

## Non-goals

- Do not clone OpenJDK StandardDoclet internals.
- Do not clone Doclava HDF rendering.
- Do not import DevSite application bundles.
- Do not make renderers infer navigation/search directly from raw `DocCorpus`.

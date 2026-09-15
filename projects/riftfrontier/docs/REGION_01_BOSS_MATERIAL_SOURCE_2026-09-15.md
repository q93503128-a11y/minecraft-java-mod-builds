# Region 01 boss material source — 2026-09-15

## Purpose

Pin the exact upstream material source for the already-selected Dragon Evolved geometry without promoting unverified pixels to final art. This closes source discovery; it does **not** close Minecraft visual acceptance.

## Selected source direction

- Asset: **Dark Rock**
- Author: **Amal Kumar**
- Publisher/source: **Poly Haven**
- Canonical asset page: https://polyhaven.com/a/dark_rock
- License: **CC0** (Poly Haven asset page/license presentation)
- Intended use: source material for a derived Dragon Evolved albedo/material treatment; not a wholesale texture-pack import.
- Geometry source remains Quaternius `Dragon Evolved`; its original `Atlas` art remains excluded.

## Exact upstream file pinned for intake

Poly Haven's own download link exposed from the canonical asset page on 2026-09-15:

`https://dl.polyhaven.org/file/ph-assets/Textures/png/4k/dark_rock/dark_rock_diff_4k.png`

The link is the 4K PNG diffuse map served by Poly Haven's download host. Do not substitute a search-engine mirror, screenshot, preview thumbnail, or re-encoded copy.

## Acceptance state

**SOURCE PINNED / BYTES NOT YET ACCEPTED.**

The repository must not claim this file as bundled/production material until one run has all of the following from the exact downloaded bytes:

1. successful image decode;
2. exact byte size and SHA-256 receipt;
3. deterministic derivation into the Dragon Evolved UV/material pipeline;
4. provenance record from source hash to derived runtime resource hash;
5. executable-JAR inspection proving only the intended derived resource is bundled;
6. Minecraft render review for UV seams, head/neck readability, wing membranes, belly/limb separation and distance silhouette;
7. human visual acceptance.

A source/license page alone is not visual approval. The existing `Region01BossMaterialPreparation` decode/integrity gate remains the runtime boundary; do not add another speculative gate around it.

## Next implementation batch

Fetch the exact pinned PNG, record its receipt, derive the first legal Dragon Evolved runtime material candidate, wire it through the existing material preparation/render path, run the normal Riftfrontier verification stack, and produce a field-review JAR. If the source host cannot be fetched in the execution environment, stop at the source pin rather than fabricating bytes or hashes.

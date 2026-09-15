# Region 01 boss material source — 2026-09-15

## Purpose

Pin and authenticate the exact upstream material source for the already-selected Dragon Evolved geometry without promoting unreviewed derived pixels to final art. Source discovery and source-byte acquisition are now closed; Minecraft visual acceptance is not.

## Selected source direction

- Asset: **Dark Rock**
- Author: **Amal Kumar**
- Publisher/source: **Poly Haven**
- Canonical asset page: https://polyhaven.com/a/dark_rock
- License: **CC0**
- Intended use: source material for a derived Dragon Evolved albedo/material treatment; not a wholesale texture-pack import.
- Geometry source remains Quaternius `Dragon Evolved`; its original `Atlas` art remains excluded.

## Exact upstream file

`https://dl.polyhaven.org/file/ph-assets/Textures/png/4k/dark_rock/dark_rock_diff_4k.png`

Do not substitute a search-engine mirror, screenshot, preview thumbnail, re-encoded copy, or lower-resolution file merely to bypass an execution-environment transfer limit.

Machine-readable provenance/intake receipt:

`assets/sources/region_01_boss_dark_rock.source.json`

## Accepted source-byte receipt — 2026-09-15

Repository-scoped GitHub Actions run **34934969947** successfully acquired the exact pinned 4K PNG and published artifact **10383212118** (`riftfrontier-region01-boss-material-source`). The artifact was then downloaded independently and inspected against its `receipt.json`.

Accepted exact source facts:

- byte size: **94,658,158 bytes**
- SHA-256: **`cf323f68f6a784bf160d5394524ca2a606b6909b37c5a906601668afeb45bde4`**
- PNG dimensions: **4096 × 4096**
- parsed PNG chunks: **11,549**
- PNG chunk CRC verification: **passed**
- terminal IEND verification: **passed**
- independent local SHA-256 after artifact download: **matched receipt**

This closes the earlier transfer uncertainty. The exact source body is authenticated for deterministic downstream derivation. It does **not** approve any Dragon texture treatment by itself.

## Acceptance state

**SOURCE BYTES ACCEPTED / DRAGON UV DERIVATION PENDING.**

Completed:

1. exact pinned source acquired;
2. actual byte size recorded;
3. source SHA-256 recorded and independently rechecked;
4. PNG structure/chunk CRC/IEND verified;
5. 4096×4096 image accepted as the pinned diffuse source.

Still required before production material approval:

1. deterministic derivation into the Dragon Evolved UV/material pipeline;
2. provenance record from accepted source hash to derived runtime resource hash;
3. executable-JAR inspection proving only the intended derived resource is bundled;
4. Minecraft render review for UV seams, head/neck readability, wing membranes, belly/limb separation and distance silhouette;
5. human visual acceptance.

A source/license page or a valid PNG is not visual approval. The existing `Region01BossMaterialPreparation` decode/integrity gate remains the runtime boundary; do not add another speculative gate around it.

## Next implementation batch

Do **not** repeat source discovery or download work. Continue directly from accepted source SHA-256 `cf323f68f6a784bf160d5394524ca2a606b6909b37c5a906601668afeb45bde4`: derive the first legal Dragon Evolved runtime material candidate deterministically, record source-to-runtime provenance, wire it through the existing material preparation/render path, run the normal Riftfrontier verification stack, inspect the executable JAR, and produce a Minecraft field-review JAR with explicit human reject criteria. If derivation cannot be completed in the current environment, move to an independent production-visible Region 01 task rather than adding more material gates.

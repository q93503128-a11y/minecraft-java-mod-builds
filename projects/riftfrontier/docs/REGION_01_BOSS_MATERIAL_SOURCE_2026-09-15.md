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

The link is the 4K PNG diffuse map served by Poly Haven's download host. Do not substitute a search-engine mirror, screenshot, preview thumbnail, re-encoded copy, or lower-resolution file merely to bypass an execution-environment transfer limit.

The machine-readable provenance/intake receipt is:

`assets/sources/region_01_boss_dark_rock.source.json`

That receipt is intentionally non-accepting: until exact source bytes are acquired it carries no source SHA-256 and cannot authorize a runtime material.

## Transfer receipt — 2026-09-15

A fresh request through the available web retrieval path reached the exact pinned Poly Haven download URL. The host reported a payload length of **94,658,158 bytes**, but the retrieval layer refused to transfer the body because it exceeded that layer's content-size limit. A second attempt from the local execution container could not resolve the external host and therefore also did not obtain bytes.

This is useful provenance evidence only: it proves that the pinned endpoint currently resolves to a large payload through the web retrieval path, but **does not authenticate the file body**. No SHA-256, dimensions, decoded pixels, or derived texture may be claimed from this receipt. The 94,658,158-byte value must be rechecked against the actual downloaded file before it is promoted to an accepted source receipt.

Do not repeat source discovery or replace the pinned 4K file merely to work around an execution-environment transfer limit. A later environment that can retrieve the exact URL should continue directly from byte acquisition and hashing.

## Acceptance state

**SOURCE PINNED / ENDPOINT REACHED / BYTES NOT YET ACCEPTED.**

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

Fetch the exact pinned PNG in an environment that can transfer the 94 MB-class source body, record the actual byte size and SHA-256 in `region_01_boss_dark_rock.source.json`, decode it, derive the first legal Dragon Evolved runtime material candidate, wire it through the existing material preparation/render path, run the normal Riftfrontier verification stack, and produce a field-review JAR. If the source host still cannot be fetched, do not fabricate bytes/hashes and do not restart material-source research; move to another production-visible Region 01 task that is independent of this transfer gate.

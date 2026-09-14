# M3 Region 01 Material Intake Rejection — 2026-09-14

This note records a closed technical rejection so later work does **not** reintroduce the same broken field-review asset or mistake a green packaging build for visual-resource validity.

## Rejected candidate

The temporary file `assets/riftfrontier/textures/entity/region_01/boss_dark_rock_candidate.png` was introduced only as a non-production field-review material candidate. It was never approved final art and never satisfied `Region01BossMaterialPreparation`.

Relevant history:

- `3646826c201134fcae5e39583c40cdfd42f0faca` — introduced the field-review candidate;
- `027f51a2d6fc935dd0c91c6b29e28cfccffa0823` — attempted to repair the PNG; `Build Riftfrontier` run `34832719483` passed because the existing gates packaged the resource but did not decode it;
- `73cfb969dbe01941107d4012d8701b5b23b0370d` — added an explicit `ImageIO` decode regression check; run `34836277831` failed in `Region01BossMaterialPreparationTest.fieldReviewMaterialCandidateRemainsARealDecodablePng()` with `javax.imageio.IIOException` caused by `java.util.zip.ZipException` while decoding the IDAT stream.

This is direct evidence that the candidate itself remained corrupt. Do not restore either binary revision and do not treat run `34832719483` as proof that the texture was render-valid.

## Verified recovery

Commit `ea9474c3622bfe6e2cfcba0cf1b6e48b628b5b91` removed the invalid candidate and the now-inapplicable candidate-review test/doc, then restored `Region01BossFieldReviewRenderPreview` to the previously verified Minecraft stone field-review fallback. No gameplay authority, boss motion mapping, geometry/rig selection, damage, hit geometry, targeting, save state or network contract changed.

`Build Riftfrontier` run `34836650498` completed successfully through:

- asset-intake tests;
- `clean test build`;
- required GameTest gate;
- dedicated-server smoke;
- Xvfb client smoke;
- executable-JAR inspection;
- report and artifact upload.

Recovery deliverable:

- artifact: `riftfrontier-0.1.0-alpha.1-deliverables`
- artifact id: `10345016609`
- digest: `sha256:b0d07d70cadc20c579faf74a79613e00238a9fb529eb859b9b3ccee12e3ab43f`

Verification vocabulary for the recovery SHA:

- `CODE REVIEWED`: YES
- `TESTED`: YES
- `BUILD VERIFIED`: YES
- `JAR PRODUCED`: YES
- `PLAYTESTED`: NO
- `MULTIPLAYER TESTED`: NO
- `MATERIAL VISUALLY ACCEPTED`: NO

## Direction lock

The canonical material boundary is therefore unchanged from `AUTOMATION_HANDOFF.md`: the accepted Dragon Evolved geometry/rig remains selected, the source Atlas remains forbidden as a final-art shortcut, and the stone texture remains a temporary field-review fallback only.

The next material candidate must be a **real decodable physical resource before it is wired into the renderer**. Record provenance/license when an external redistributable asset is involved, and run a decode/format validation as part of intake before human UV/silhouette review. This requirement is based on this demonstrated corruption regression; it is not a request to add more speculative authority/lifecycle fences.

Do not reopen model search, animation roles, expedition authority or boss gameplay contracts because of this rejection. Replace only the failed material direction when a valid, provenance-safe candidate is actually available.

# Profile and Skill Tree Visual Review

Date: 2026-11-09

## Reference and scope

The approved profile and skill-tree concepts supplied with this task are the visual reference. The changes improve those three screens while retaining the existing selection screen, network packets, server-side profile data, skill-point rules, class-specific catalogs, and class management's intentionally empty center. No combat or progression behavior was changed.

The available attachments for this review contain the approved profile and tree concepts. Actual Minecraft captures of the current profile, tree, and management screens were not available in the workspace, so this is a source-and-asset review rather than a before/after in-game screenshot comparison.

## Visual changes

- Replaced the broad flat panel treatment with a shared, textured frame atlas, a blank centered title plaque, and a plum surface tile. Profile, skill tree, and management use the same frame language; the title text remains live Korean/English UI text.
- Profile keeps the large class artwork as the left visual anchor and groups class identity, stored level/experience/SP, and server-reported attributes on the right. The two navigation controls are compact and centered. No experience progress bar is drawn because the project has no approved XP threshold curve.
- Skill tree uses six original 4×4 class-specific skill icon atlases in canonical node order. It has separate frame states for uninvested, invested, selected, and hovered nodes; textured connection, button, and rank-meter assets; current/max rank; next-rank cost; next-breakpoint target and cost; and the existing effect description.
- Skill nodes remain freely selectable; lines communicate existing skill relationships only. No prerequisites, locks, or new costs were added. The player-facing passive-effect implementation banner was removed; effect descriptions remain descriptions and do not claim that combat effects are active.
- Class management retains the requested title, small class identity, return/change controls, and empty central area, now placed over the shared textured surface. No placeholder management feature was added.
- Artwork and controls remain separate runtime textures and text. The approved concept PNGs are not embedded as screen backgrounds.

## Fidelity and remaining differences

The implementation follows the approved compositions, dark plum/purple palette, gold frame language, title plaque, class artwork, node-centered tree, and right-side detail hierarchy. It is not a pixel-perfect reproduction: the concepts contain denser bespoke ornament and micro-detail than these reusable atlases; the 4×4 icons are newly authored interpretations rather than exact copies of concept symbols. Runtime scale, actual glyph rasterization, and texture sampling still need the user's in-game visual review.

## Static checks

`tools/verify_profile_skilltree.py` passed for nine logical viewport combinations:

| Aspect ratio | GUI 2 | GUI 3 | GUI 4 |
|---|---:|---:|---:|
| 16:9 | 960×540 | 640×360 | 480×270 |
| 16:10 | 960×600 | 640×400 | 480×300 |
| 4:3 | 800×600 | 534×400 | 400×300 |

The check covers screen and control bounds, graph/node bounds, the canonical six trees and SP totals, translated labels, and source-level preservation of investment/reset transactions. Its label-width check uses conservative Korean/English estimates; actual `Font.width()` and rendered glyphs were not measured in Minecraft. Click rectangles match the registered widget/node boxes by source construction, but live click testing was not performed.

The `clean build` and JAR verifier passed. The JAR check validates packaged class artwork, all six transparent skill atlases, the shared UI atlas dimensions/transparency, loader metadata, compiled classes, locale files, data files, and duplicate entries.

## Runtime review boundary

- Minecraft client: `runClient` launched after the final build and remains running. The startup log confirms the mod resource reload; no in-world UI was opened.
- CUA and live Minecraft interaction: not used.
- Client-rendered text, texture sampling, screenshot comparison, and live click behavior: not verified.
- User review: enter a world, press `M`, inspect Profile → Skill Tree and Profile → Class Management at GUI Scale 2, 3, and 4, then repeat at 16:9, 16:10, and 4:3 if available. Verify node selection, rank details, investment/reset responses, and Korean/English text.

# Profile and skill-tree UI visual review — 2026-11-11

## Scope and baseline

- Repository: `q93503128-a11y/minecraft-java-mod-builds`
- Project: `projects/anomalous-classes/`, branch `main`
- Baseline checked against `origin/main`: `c71dd56b0bf9b77634546480b3fa551ff81b8d84`
- Minecraft 1.21.1, NeoForge 21.1.244, Java 21
- This pass only repairs profile/frame/tooltip presentation. The six skill maps, 581 purchasable nodes, their costs and prerequisites, server checks, saves, migrations, reset, and pan/zoom are retained.

## Frame atlas findings

`frame_atlas.png` is a 768×768 RGBA sheet. The visible frame is split across corner artwork and narrow rail/jewel regions; it is not a regular 3×3 set of equal cells.

The previous `MenuUi.drawPanel()` path used guessed atlas coordinates. In particular, its horizontal rail helper selected a 24-pixel-wide strip around source x=84, then expanded that unrelated strip across the full panel width. The top and bottom source Y coordinates also sampled the wrong portions of the sheet. The four old corner crops started at the atlas cell bounds (0 or 624) instead of the actual corner art bounds, clipping the outer curve/gem and including transparent margin. Side rails and ornaments likewise used independently guessed coordinates. That combination produced stretched bands, cut corner details, and stray transparent/low-alpha pixels from the wrong source areas. The existing screen capture shows the resulting streaks and edge contamination.

The runtime now samples isolated images cropped from the original atlas, keeping the source atlas unchanged:

| Piece | Atlas source `(x, y, width, height)` | Runtime file |
|---|---:|---|
| Four corners | `(32,32)`, `(576,32)`, `(32,576)`, `(576,576)`; each `160×160` | `frame_parts/corner_*.png` |
| Top rails | `(318,64)`, `(426,64)`; each `24×64` | `frame_parts/rail_top_*.png` |
| Bottom rails | `(319,640)`, `(426,640)`; each `24×64` | `frame_parts/rail_bottom_*.png` |
| Side rails | `(64,434)`, `(640,428)`; each `64×24` | `frame_parts/rail_side_*.png` |
| Side jewels | `(72,344)`, `(648,344)`; each `48×64` | `frame_parts/jewel_side_*.png` |
| Top/bottom jewels | `(352,48)`, `(352,628)`; each `64×88` | `frame_parts/jewel_*.png` |

Corners retain fixed proportions. Rails tile along their long axis using the same `corner / 160` scale, with partial final tiles cropped proportionally. Jewels are centered on the rail axes. Small HUD and tooltip panels omit large ornaments while keeping the same frame source art. This avoids sampling neighboring atlas cells and avoids stretching a narrow vertical sample into a long horizontal rail.

## Profile and tooltip review

- Profile rows now measure the localized label and value widths and place them in one centered two-column group. Values are right-aligned to the group’s value column instead of the far screen edge. The existing 16:9 art is still cover-cropped and scissored to its assigned rectangle.
- The tooltip contains only the node name, actual effect (or stat gain), SP cost, and one status. Repeated prerequisite names, stage counts, the “Passive description” prefix, and extra adjacent-path prose were removed. A blocked path is summarized as `선행 경로 미해금` / `Path not yet unlocked`; server checks are unchanged.
- The maximum tooltip width is 160 GUI units. Height is derived from the wrapped lines and clamped to the screen. Both locales use `%s` formatting; source locale validation found no literal `{0}`-style indexed arguments.

## Generated and visually inspected images

These images are generated from the shipped textures and current source geometry by `tools/render_ui_static_review.java`. The before frame uses the previous atlas UV/stretched-rail formulas; the after frame uses the isolated source parts and corrected scale. Profile and tooltip images use a 512×288 logical viewport rendered at 4×. Text is Java2D-rendered for static comparison; its glyph rasterization is not Minecraft’s font renderer.

- `frame_before_after.png` — old and corrected frame composition side by side
- `profile_layout_ko.png`, `profile_layout_en.png` — profile composition and grouped metric rows
- `tooltip_ko.png`, `tooltip_en.png` — localized compact hover card over the original Swarm Caster map art

Visual review found continuous corner-to-rail joins, no unrelated atlas cell in the new rails, aligned profile label/value columns, an unclipped profile illustration, and a shorter tooltip without prerequisite lists. The old Java2D reproduction shows the wrong stretched band and clipped crop, but does **not** reproduce Minecraft/OpenGL mipmap or texture-filter behavior pixel-for-pixel.

## Static matrix and limits

`tools/verify_graph_skilltree.py` checks Korean and English, all 581 nodes and 900-SP totals, all 61 skill icons, migration fixtures, panel resource dimensions, profile row bounds, and these 18 combinations:

- 16:9, 16:10, and 4:3
- GUI Scale 2, 3, and 4
- profile and skill-tree layout bounds, HUD, camera, and tooltip limits

This is a **STATIC REVIEW**, not a Minecraft screenshot or test. No client screen was opened or manipulated. Actual GPU sampling, Minecraft font glyph appearance, and the user’s exact frame corruption must be checked in the client by the user.

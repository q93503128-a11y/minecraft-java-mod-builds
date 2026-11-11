# Frame artifact root cause review — 2026-11-11

## Scope and baseline

- Repository: `q93503128-a11y/minecraft-java-mod-builds`
- Project: `projects/anomalous-classes/`, branch `main`
- Fetched `origin/main` before editing; baseline: `93709c133b288fe046cf73f31be103ee78b8bc69`
- Minecraft 1.21.1, NeoForge 21.1.244, Java 21
- Scope: shared `MenuUi.drawPanel()` frame parts and their static review. Profile content, class art, the 581-node graph, skill costs/prerequisites, server state, saves, migrations, and pan/zoom are unchanged.

## Root cause found

The 768×768 source `frame_atlas.png` contains frame parts in authored regions; it is not a uniform nine-slice grid. The previous extracted files had exact alpha values, but `Graphics2D.drawImage()` copied them through `SrcOver` into a new transparent image. A per-pixel comparison against the atlas found RGB changes in the old crops (including 3,688–4,713 changed pixels per corner, with alpha unchanged and maximum RGB delta 5). It also zeroed hidden RGB on 24–45 fully transparent corner pixels. This was a secondary edge-sampling risk, not the cause of the large rectangular fragments.

The visible artifacts came primarily from two geometry errors:

1. A 24×64 rail crop, which contains non-seamless ornamental detail along its width, was compressed to roughly four GUI pixels at the screenshot's GUI Scale 4 and repeated across the panel. The same approach was used on vertical rails. This multiplied the decoration into dense seams and repeated edge fragments.
2. Side jewels were cropped as 48×64 instead of the isolated 24×64 center, and top/bottom jewels as 64×88 instead of 32×88. Those crops included adjacent rail/background pixels, then were drawn over the independently rendered rails. The unwanted pixels therefore reappeared as gray blocks around the corner jewels and as gray/color fragments below the top and bottom center ornaments.

`drawPanel()` did not draw the whole frame twice. It draws the surface once, then rails, corners, and center/side ornaments. The overlapping pixels were in the oversized ornament crops and the tightly repeated rail geometry. No global blend or blur setting was changed.

## Fix

- Kept `frame_atlas.png` as the source of record and extracted every part with raw `getRGB()` / `setRGB()` copies. The updated audit reports **zero RGBA pixel differences** between each runtime part and its specified atlas crop.
- Isolated side jewels at `(84,344)` and `(660,344)`, each 24×64; isolated top/bottom jewels at `(368,48)` and `(368,628)`, each 32×88. The original corner and rail crops remain at their atlas bounds and retain their authored shadows, metallic bevels, purple curves, and gold details.
- Replaced repeated compressed rail chunks with one-pixel cross-sections from the rail center (`x=12` for horizontal rails, `y=12` for vertical rails), stretched only along the long axis. This preserves the layered rail profile without repeating its non-seamless ornaments. Existing render order remains surface → rails → corners → isolated ornaments.
- Added source/resource regression checks for the isolated crop sizes and the new rail rendering path.

The gray bevels and dark outline pixels still visible when zooming a corner are attached to the original authored corner art and are retained intentionally; they are not detached squares or newly introduced pixels. The checker-backed static composites show those pieces joining the rail and surface without separate rectangular leftovers.

## Pixel audit and visual evidence

`tools/frame_asset_diagnostics.java` reads the atlas and all 14 runtime PNGs, compares raw RGBA per pixel, inspects alpha ranges, reconstructs the previous committed extraction from `HEAD`, and renders before/after compositions at the same 512×276 logical viewport, GUI Scale 4, and screen coordinates. The patterned panel interior uses the shipped `surface_tile.png`; a checker is used outside the panel to expose stray opaque/low-alpha pixels. Crop enlargements use nearest-neighbor for inspection.

Evidence generated and visually inspected:

- [Before/after full-frame comparison](frame-root-cause/profile_frame_before_after.png)
- [Patched full-frame static composition](frame-root-cause/profile_frame_after_scale4_static.png)
- [Top-left corner, before and after](frame-root-cause/comparison/corner_tl_before_after.png)
- [Top-right corner, before and after](frame-root-cause/comparison/corner_tr_before_after.png)
- [Bottom-left corner, before and after](frame-root-cause/comparison/corner_bl_before_after.png)
- [Bottom-right corner, before and after](frame-root-cause/comparison/corner_br_before_after.png)
- [Top center ornament, before and after](frame-root-cause/comparison/jewel_top_before_after.png)
- [Bottom center ornament, before and after](frame-root-cause/comparison/jewel_bottom_before_after.png)
- [After profile layout with Korean text](profile_layout_ko.png)
- [After skill-tree tooltip with Korean text](tooltip_ko.png)
- [After skill-tree tooltip with English text](tooltip_en.png)
- [Pixel and alpha audit log](frame-root-cause/pixel_rgba_audit.txt)

The `after/` crops are 3× nearest-neighbor close-ups of the rendered panel joins. The full profile and tooltip references come from `tools/render_ui_static_review.java` and use the current `MenuUi` geometry plus shipped textures. Java2D static output is not Minecraft's GPU renderer.

## Shared-screen regression review

`ProfileScreen` and `ClassManagementScreen` use the large panel; the skill-tree SP HUD and hover card use the same renderer at smaller dimensions. The small-panel path continues to omit center ornaments under its existing size threshold. Static profile and tooltip compositions were refreshed, and the project's 16:9 / 16:10 / 4:3 × GUI Scale 2 / 3 / 4 static bounds verifier is part of the build.

## Verification boundary

- Source atlas dimensions, crop bounds, extracted RGBA, and PNG alpha values: **CHECKED**.
- Old and updated source-based frame compositions, four corners, and both center ornaments: **STATIC REVIEWED**.
- Shared profile/skill-tree frame layouts and 9 screen geometry combinations: **STATIC REVIEWED**.
- Actual Minecraft GPU sampling, framebuffer behavior, and on-screen appearance after loading this build: **NOT TESTED**. No CUA was used. The user should inspect Profile, Skill Tree SP HUD/tooltip, and Class Management in the running client.

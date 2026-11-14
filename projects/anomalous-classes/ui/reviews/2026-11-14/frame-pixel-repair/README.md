# Frame alpha cleanup review

These PNGs are offline Java2D composites generated from the project frame textures and `MenuUi.drawPanel()` geometry by `tools/frame_asset_diagnostics.java`.

- `profile_frame_before_after.png` compares the committed corner alpha with the cleaned corner assets across a full static panel.
- `comparison/` contains enlarged corner and central jewel comparisons.
- `after/` contains enlarged post-cleanup joins.
- The files named `screenshot_*.png` are derived crops of user-provided pre-change screenshots. They are intentionally not included in the repository artifact.

This is static image review only. It is not a Minecraft renderer capture and does not establish `CLIENT TESTED` or `PLAYTESTED` status.

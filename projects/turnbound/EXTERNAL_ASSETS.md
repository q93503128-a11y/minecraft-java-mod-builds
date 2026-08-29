# TURNBOUND external asset lock

## Active
- UI Lib 21.1.1 — Apache-2.0, API/reference dependency retained for later interface work.
- GeckoLib 5.5.3 — animation/runtime dependency reserved for authored character/enemy models.

## Retired from active battle HUD
- Kenney UI Pack: RPG Expansion — CC0. Original: https://kenney.nl/assets/ui-pack-rpg-expansion
  - alpha.2~alpha.4 P0 used `buttonLong_blue.png`, `buttonLong_blue_pressed.png`, `panel_blue.png`, `panelInset_blue.png`.
  - alpha.6 removes those vendored sprites and the old framed battle-button/panel implementation from the active project because real playtests showed that stretched framed assets dominated the 3D scene.
  - License history remains recorded here for provenance; removal is a design decision, not a licensing issue.

R_PG/R_PG X and proprietary games are reference-only for information hierarchy, spatial composition and pacing. Do not copy their UI textures, icons, fonts, code or exact layout values.

Temporary stand-in 3D actors are explicitly non-final and must be replaced by authored character models/animations in the presentation pass.

# TURNBOUND external asset / UI reference lock

- Kenney UI Pack: RPG Expansion — CC0. The alpha.4 large framed battle skin was retired in alpha.6. Kenney remains a low-level frame/button skin source only; TURNBOUND's house style is defined by its own color tokens, spacing, information hierarchy, selection/marker shapes and world-first layout. Do not mix random Kenney families per-screen.
- BetterQuesting (`Funwayguy/BetterQuesting`, MIT) — alpha.15+ UI design reference for compact nested frames, bounded quest surfaces, tab/header hierarchy, dense readable information and clearly shaped selected states. No BetterQuesting pixels or source code are vendored.
- Roughly Enough Items / REI — alpha.15+ design reference for compact framed controls, repeated grid rhythm, dense layouts and tooltip hierarchy. No REI pixels or source code are vendored.
- UI Lib 21.1.1 — Apache-2.0, API/reference only.
- GeckoLib 5.5.3 — animation/runtime dependency reserved for authored character models.

The user-supplied reference-game screenshots are used only for spatial hierarchy: world-dominant field/battle view, party on the lower edge, small contextual actions, world-space HP, and a clear arrow over the selected 3D target. Do not copy proprietary textures, icons, fonts or exact UI assets.

`UI_DESIGN_SYSTEM.md` is the TURNBOUND project-level design lock. Shared frame/button renderers should preserve source texture corner/border proportions rather than stretching complete textures across arbitrary rectangles. Map marker type and feedback state should use shape/text as well as color.

Temporary ArmorStand actors remain non-final and must later be replaced by authored character models/animations.

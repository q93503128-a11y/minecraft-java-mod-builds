# Frontier Settlement Alpha.82 — companion-safe key profile

Version: `0.1.0-alpha.82`

Alpha.82 is a client UX hotfix driven by the first real Alpha.81 graphical-client test. Alpha.81 booted and loaded Frontier, but `B` opened Xaero's Minimap new-waypoint screen because the candidate stack had three useful actions competing for the same default key.

## Canonical pack key map

- `M` — Frontier Settlement main menu. Before founding it opens the settlement-start screen; afterwards it opens the construction/infrastructure palette.
- `B` — Sophisticated Backpacks open-backpack action. Frontier no longer claims B.
- `U` — Xaero's Minimap waypoint screen; this remains the normal waypoint-management route.
- Xaero's redundant default `B` quick/new-waypoint action is unbound by Frontier only while that Xaero mapping is still on its own shipped default.
- `Y`, `I`, `O` — Xaero minimap/settings/zoom controls remain untouched.
- `V` — Weapons Expanded weapon/grip action remains untouched.
- Better Combat's optional extra actions remain on their upstream unbound defaults.
- `R` — Frontier building rotation while ordinary building placement is active.
- `Enter` — Frontier confirmation while a Frontier building/road/outpost/civil-work placement step is active.
- `Backspace` — Frontier road/civil start reset while that Frontier placement mode is active.

## User-control rule

Frontier must never reset the whole Minecraft control table. `CompanionKeyProfile` scans only after the client is in a world and changes only a known redundant Xaero mapping that is still `isDefault()` and still bound to `B`. A player-customized mapping is therefore left alone. The adjustment is saved through Minecraft `Options`, so it does not repeat every tick or require replacing the user's `options.txt`.

This is intentionally narrow. Unknown duplicate mappings from future companion updates are reported/tested before adding another automatic remap; Frontier does not guess and disable arbitrary third-party controls.

## Companion audit basis

The pinned candidate stack was checked rather than treating every installed mod as equivalent:

- Sophisticated Backpacks 26.2 defaults its primary backpack opening action to `B`, so B is reserved for the backpack.
- Xaero's Minimap 26.4.2 exposes the full waypoint screen on `U`, making its separate default-B quick/new-waypoint action redundant for this pack.
- Better Combat 26.2 keeps its optional feint/toggle extras unbound by default.
- Weapons Expanded 1.9.3 remains pinned and its documented `V` action does not collide with the Frontier profile.
- worldgen/library companions do not receive invented key overrides.

No companion Java class, code, texture or UI asset is copied into Frontier. The normalizer works only through Minecraft's public registered `KeyMapping` / `Options` state and remains optional when Xaero is absent.

## Acceptance boundary

Canonical source/docs/build/JAR verification can prove compile/package integrity. It cannot prove the user's full controls menu is visually conflict-free after arbitrary personal remaps. Real graphical-client acceptance requires launching the Alpha.82 test pack and confirming at minimum: `M` opens Frontier, `B` opens the backpack without a waypoint popup, `U` still opens Xaero waypoints, and Frontier `R`/`Enter`/`Backspace` placement controls still behave correctly.

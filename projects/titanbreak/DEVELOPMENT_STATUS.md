# TITANBREAK Development Status

Last normalized at: **0.1.0-alpha.54 elite visual completion line**

This is the current live status index. Historical `ALPHA*_IMPLEMENTATION_NOTES.md` files are archaeology/regression records; current source + the v0.3 content bible + this status index win when old notes disagree.

## Current production state

- Core augmentation/HUD/resource systems: implemented baseline.
- Reflex Drive time-acceleration line: implemented; suppression interaction integrated.
- Main boss progression through B10: implemented baseline.
- B08 Ash Titan, B09 Null Seraph and B10 Worldbreaker: dedicated multipart encounters implemented.
- Canonical elite roster: 10/10 implemented and connected to hunt/reward systems.
- Visual governance: `VISUAL_BIBLE.md` + `CHARACTER_DESIGN_PIPELINE.md`.
- Integrated asset validation: `tools/verify_visual_assets.py`.
- Runtime target: Minecraft **26.2**, NeoForge **26.2.0.38-beta**, GeckoLib **5.5.3**, Java **25**.

## Presentation completion lines

### Alpha.52
Established the non-generic boss silhouette standard for B01 The Pursuer, B04 The Regnant Flesh, B05 Hundred-Eyed Watcher, B06 Chronophage, B09 Null Seraph and B10 Worldbreaker.

### Alpha.53
Closed the remaining first-pass boss/normal-threat presentation debt for B02 Gravemarch Colossus, B03 Bastion Walker, B07 Storm Leviathan, B08 Ash Titan, Bulwark and Howler. It also replaced the obsolete alpha.52-only visual CI with the reusable `titanbreak-visual-regression-ci.yml` gate and introduced `tools/verify_visual_assets.py`.

### Alpha.54
The post-alpha.53 repository audit identified the ten elites as the largest remaining presentation-quality gap. Alpha.54 remasters all ten while preserving gameplay, AI, stats, drops and animation/runtime bone contracts:

- **Chrono Hound** — low temporal pursuit quadruped; sensor head, chrono core/ring, dorsal fins, phase rails and tail mass communicate high relative speed inside temporal fields.
- **Null Eye** — floating optic-jammer organism; central eye, jammer rings/coils, relays and tendrils replace any humanoid read.
- **Iron Maw** — dense impact/grab brute dominated by jaw, clamp forearms, hooks, chest impact mass and bracing.
- **Revenant** — asymmetric regenerative colony with three simultaneously readable canonical regeneration cores and replacement tissue.
- **Apex Stalker** — lean optical pursuit predator with cloak masses, blades, sensor crest, optic nodes and route-control fins.
- **Shock Choir** — electrical conductor organism built around chest coil, spires, conductor/link structures, overload ring and rear capacitor.
- **Siegeback** — low moving bunker with four supports, hard frontal wall, dorsal armored cannon, side armor and recoil bracing.
- **Phase Lurker** — spatially discontinuous predator with offset shells, phase rings, anchors, veil fins and distortion core.
- **Warden Node** — battlefield command hub dominated by command node, mast, halo, relays and formation emitters.
- **Harvester** — salvage recycler/brood carrier dominated by harvest vat, intake, feeder, claws, brood pod, spawn cradle and repair spine.

No new elite mechanic or weakpoint is introduced by the geometry.

## Automated visual gate

The reusable verifier protects **16 remastered targets**: the six alpha.53 targets plus all ten elites. It:
1. parses all GeckoLib entity geometry;
2. rejects duplicate bones, broken parents, parent cycles and malformed cube vectors;
3. checks animation bone references against matching geometry;
4. checks ordinary model/texture stem mapping, with documented renderer exceptions only;
5. enforces signature/runtime bones and minimum-density floors for protected remasters;
6. runs through the version-tolerant Visual Regression CI;
7. requires Java 25 clean build, runtime JAR-name verification and artifact upload.

`Elite Catalog CI` remains the separate source-of-truth gate for the 10-entry elite roster and its gameplay/catalog wiring.

## Next phase after alpha.54

Do **not** jump directly to broad content expansion. Finish the remaining normal-enemy art/resource audit first so the first integrated playtest is not polluted by obvious placeholder presentation debt.

Audit the 16 normal enemies for generic humanoid/blockout silhouettes, duplicated geometry/animation patterns, renderer-scale vs hitbox mismatch, obsolete/unreachable assets, role readability, animation pivots and asset-registry consistency. Then move to integrated in-game presentation testing covering bosses, elites and normal threats together. A clean CI build means structurally ready for playtest, not visually approved in-game.

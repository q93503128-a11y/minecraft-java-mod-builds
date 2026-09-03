# TITANBREAK Development Status

Last normalized at: **0.1.0-alpha.53 visual completion line**

This is the current status index. Historical `ALPHA*_IMPLEMENTATION_NOTES.md` files remain decision/regression records, but current source + the v0.3 content bible + this status index are authoritative when old notes disagree.

## Current production state

- Core augmentation/HUD/resource systems: implemented baseline.
- Reflex Drive time-acceleration line: implemented; suppression interaction is integrated.
- Main boss progression through B10: implemented baseline.
- B08 Ash Titan, B09 Null Seraph and B10 Worldbreaker: dedicated multipart encounters implemented.
- Visual governance: `VISUAL_BIBLE.md` + `CHARACTER_DESIGN_PIPELINE.md`.
- Integrated asset validation: `tools/verify_visual_assets.py`.
- Runtime target from current build files: Minecraft **26.2**, NeoForge **26.2.0.38-beta**, GeckoLib **5.5.3**, Java **25**.

## Alpha.52 completed presentation line

Alpha.52 established the non-generic silhouette standard for:
- B01 The Pursuer
- B04 The Regnant Flesh
- B05 Hundred-Eyed Watcher
- B06 Chronophage
- B09 Null Seraph
- B10 Worldbreaker

Those assets remain part of the visual regression surface.

## Alpha.53 scope

Alpha.53 closes the remaining first-pass silhouette debt before integrated real-play review.

Remodeled in this line:
- **B02 Gravemarch Colossus** — power/berserker giant with heavier upper body, reinforced elbows/knees/ankles, dorsal impact mass and a clearly exposed shock-heart.
- **B03 Bastion Walker** — low mobile fortress with layered closure plates, layered closure armor, readable upper defense node/power-core route, asymmetric turret masses, buttresses and frontal ram.
- **B07 Storm Leviathan** — horizontal wandering storm organism with expanded wing membranes, six electric sacs, a stronger sensor crest, dorsal charge spine, storm organ and tail control surfaces.
- **B08 Ash Titan** — thermal/radiant guardian with six cooling plates preserved, heavier radiation-arm masses, exposed radiant-heart framing, heat vents and a protected head sensor.
- **Bulwark** — compact moving rampart whose shield/front wall dominates the silhouette.
- **Howler** — top-heavy resonator organism whose horn-mouth, throat bellows and acoustic rings dominate the body.

Gameplay, balance, encounter sequencing and reward contracts are intentionally unchanged by this visual batch.

## Alpha.53 automated gate

The old alpha.52-only visual workflow is retired. `titanbreak-visual-regression-ci.yml` now:
1. parses all GeckoLib entity geometry;
2. rejects duplicate bones, broken parents, parent cycles and malformed cubes;
3. verifies animation bone references against matching geometry;
4. verifies ordinary model/texture stem mapping, with only documented renderer exceptions;
5. enforces persistent alpha.53 silhouette/signature contracts for the six remodeled targets;
6. runs a Java 25 clean build;
7. verifies and uploads the runtime JAR.

The regression gate is deliberately version-tolerant so it does not become a stale exact-alpha blocker during later development.

## Next phase

Do **integrated in-game presentation testing**, not another blind geometry pass. Check:
- spawn/culling distance and scale;
- ground contact and movement;
- attack animation pivots;
- multipart/weakpoint alignment;
- phase visibility;
- terrain clipping/traversal;
- texture stretching introduced by the new blockout;
- B02/B03/B07/B08 mechanic readability;
- Bulwark/Howler combat readability at ordinary encounter distance.

A clean CI build means structurally ready for playtest, not visually approved in-game.

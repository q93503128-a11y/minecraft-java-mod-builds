# TITANBREAK Visual Bible

Status: canonical presentation contract for the current implementation line.  
Design authority: `TITANBREAK_통합_기획서_콘텐츠바이블_v0.3` remains the gameplay/content source of truth. This file translates that content into visual production rules; it does not change boss mechanics, rewards, progression, or balance.

## 1. Visual thesis

TITANBREAK is body-augmentation survival against organisms and machines whose anatomy communicates gameplay. A creature must not read as “a scaled vanilla humanoid with parts attached” unless the content bible explicitly requires a humanoid identity.

Every major silhouette must communicate at first glance:
1. what kind of threat it is,
2. how it moves,
3. which structures matter in combat,
4. how large it is relative to the player,
5. which boss it is without relying on nameplates.

Generic head/torso/two-arm/two-leg construction is a fallback, not a house style.

## 2. Shape language

- **Pursuit / kinetic threats:** forward-biased mass, narrow front sensor profile, long contact limbs, rearward reactor/keel shapes. Motion should look committed.
- **Regeneration / flesh:** asymmetry, overlapping masses, visible circulation and replacement tissue. Mirrored clean anatomy is discouraged.
- **Analysis / optical:** radial or orbital composition, repeated sensors, nested rings, intentionally ordered geometry.
- **Temporal:** offset rings, segmented joints, discontinuous/arthropod-like body plan, visibly separate time organs.
- **Suppression / null:** thin monolithic mass, blade arrays, void gaps, suspended stabilizers. Avoid conventional angel-person anatomy.
- **Catastrophe / fortress:** broad load-bearing hull, architecture-like superstructure, redundant support legs, weapon pylons. The body is terrain.

## 3. Weakpoint readability

Destructible parts are gameplay UI rendered in world space.

- A required weakpoint gets its own named GeckoLib bone.
- Renderer-controlled weakpoints must never be merged into decorative parent bones that disappear for unrelated reasons.
- Exposed-core transitions use shell/core separation.
- A destroyed part must visibly reduce the silhouette or remove a recognisable organ.
- Decorative detail may surround a weakpoint but must not hide its approximate location.
- Phase-only field geometry must remain visually subordinate to the physical weakpoints.

## 4. Scale hierarchy

The renderer scale and encounter dimensions are part of the design contract, not cosmetic multipliers. The player should read:
normal enemy < elite < early boss < regional giant < world-scale boss.

B10 Worldbreaker is an encounter space as much as an entity. Its four legs, hull, ramparts, auxiliary organs, weapon pylons, and central core must form climbable/legible vertical layers rather than a single enlarged humanoid torso.

## 5. Materials and palette

Textures may reuse project-owned palette baselines, but silhouettes must remain unique. Color should reinforce function:
- hot/energy organs: concentrated emissive-looking accents against darker housing,
- neural/optical organs: precise high-contrast points,
- regenerative tissue: uneven warm biological values,
- null/suppression structures: low-saturation body with sharp high-value signal accents,
- fortress/catastrophe armor: large dark industrial masses with localized core accents.

Do not compensate for weak geometry by adding random glow everywhere.

## 6. Boss identity contracts

### B01 The Pursuer
Forward-hunched pursuit engine. Long forelimbs, digitigrade rear legs, wedge sensor head, chest core, exposed dorsal reactor and pursuit keel. It should look built to close distance, not stand upright.

### B04 The Regnant Flesh
Asymmetric mobile flesh colony. Tumor masses, ribs, circulation nodes, multiple regeneration cores, brain sac/stalk and mismatched limbs. Its body should look replaceable and rearrangeable.

### B05 Hundred-Eyed Watcher
Floating ocular observatory. The body is a nested sensor cluster with three orbital eye bands, 24 independently destructible eyes, three predictive brains, false cores, central visual core and prediction field. No humanoid silhouette.

### B06 Chronophage
Temporal arthropod/engine. Horizontal carapace, forward mandibles, scythe-like forelimbs, rear pylons, four phase joints, three time organs and concentric temporal structures. The center ring is the visual anchor.

### B09 Null Seraph
Floating suppression monolith. Coffin-like central body, four blade wings, dual null cores, head resonator, dangling stabilizers, suppression halo and lance crown. “Seraph” is expressed by ordered arrays and ritual geometry rather than a person with wings.

### B10 Worldbreaker
Mobile fortress quadruped. Four cathedral-scale legs support a broad siege hull, belly keel, upper citadel, twin ramparts, weapon pylons, six outer cores, temporal/energy auxiliary organs and central core. It must read as architecture in motion.

## 7. Anti-regression rules

A boss presentation change fails review if:
- all major masses can be reduced to a vanilla humanoid skeleton without losing identity,
- a renderer-required weakpoint bone is removed or renamed,
- an animation references a bone absent from geometry,
- multiple unrelated bosses converge on the same silhouette,
- a phase mechanic becomes harder to understand because geometry hides its weakpoint,
- internal developer labels appear in player-facing text.

## 8. Ownership and sourcing

Current boss geometry in the alpha.52 remaster is project-owned original work. External references may inform quality targets, but copied geometry/textures are not a production shortcut. Any future external asset included at runtime must be recorded in `ASSET_REGISTRY.md` with source, author, license, modification, and usage before merge.

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

- **Pursuit / kinetic threats:** forward-biased mass, narrow front sensor profile, long contact limbs, rearward reactor/keel shapes.
- **Regeneration / flesh:** asymmetry, overlapping masses, visible circulation and replacement tissue.
- **Analysis / optical:** radial or orbital composition, repeated sensors, nested rings.
- **Temporal:** offset rings, segmented joints, discontinuous/arthropod-like body plan.
- **Suppression / null:** thin monolithic mass, blade arrays, void gaps, suspended stabilizers.
- **Catastrophe / fortress:** broad load-bearing hull, architecture-like superstructure, redundant supports and weapon pylons.
- **Impact / power:** oversized joint armor, dense upper-body mass, short load paths and visibly protected shock organs; weight should read before decoration.
- **Storm / aerial:** horizontal movement axis, fins and dorsal charge organs; avoid standing-animal posture.
- **Thermal / ash:** kiln, furnace, vent, cooling plate and slag masses; heat source must have structural housing.
- **Resonance / sonic:** horn, throat, bellows and ring forms; sound-producing anatomy must dominate.

## 3. Weakpoint readability

Destructible parts are gameplay UI rendered in world space.

- A required weakpoint gets its own named GeckoLib bone when renderer/part logic depends on it.
- Renderer-controlled weakpoints must never be merged into decorative parent bones that disappear for unrelated reasons.
- Exposed-core transitions use shell/core separation.
- A destroyed part must visibly reduce the silhouette or remove a recognisable organ.
- Decorative detail may surround a weakpoint but must not hide its approximate location.
- Phase-only field geometry must remain visually subordinate to physical weakpoints.

## 4. Scale hierarchy

The renderer scale and encounter dimensions are part of the design contract, not cosmetic multipliers. The player should read:
normal enemy < elite < early boss < regional giant < world-scale boss.

B10 Worldbreaker is an encounter space as much as an entity. Its legs, hull, ramparts, auxiliary organs, weapon pylons and central core must form legible vertical layers rather than a single enlarged humanoid torso.

## 5. Materials and palette

Textures may reuse project-owned palette baselines, but silhouettes must remain unique. Color should reinforce function:
- hot/energy organs: concentrated accents against darker housing,
- neural/optical organs: precise high-contrast points,
- regenerative tissue: uneven warm biological values,
- null/suppression structures: low-saturation body with sharp signal accents,
- fortress armor: large dark industrial masses with localized core accents.

Do not compensate for weak geometry by adding random glow everywhere.

## 6. Boss identity contracts

### B01 The Pursuer
Forward-hunched pursuit engine. Long forelimbs, digitigrade rear legs, wedge sensor head, chest core, exposed dorsal reactor and pursuit keel.

### B02 Gravemarch Colossus
A power/berserker giant whose weight is communicated by a broad upper body, oversized forearms, reinforced elbow/knee/ankle masses and dorsal impact armor. The shock-heart and skull armor remain readable, and the silhouette must not collapse back into a merely scaled humanoid.

### B03 Bastion Walker
A squat mobile fortress. Four load-bearing legs and eight external armor plates support a readable outside-climb route toward the upper defense node and internal power core. Asymmetric turret masses, side buttresses and a frontal ram make it read as fortification before creature.

### B04 The Regnant Flesh
Asymmetric mobile flesh colony. Tumor masses, ribs, circulation nodes, multiple regeneration cores, brain sac/stalk and mismatched limbs.

### B05 Hundred-Eyed Watcher
Floating ocular observatory. Nested sensor clusters, orbital eye bands, predictive brains, false cores and a central visual core; no humanoid silhouette.

### B06 Chronophage
Temporal arthropod/engine. Horizontal carapace, forward mandibles, scythe-like forelimbs, rear pylons, phase joints and concentric temporal structures.

### B07 Storm Leviathan
Long horizontal wandering organism. Four wing membranes, six electric sacs, a head sensor, deep storm organ, organic dorsal charge spine and tail control surfaces keep it unmistakably alive rather than a flying machine.

### B08 Ash Titan
Thermal/radiant guardian. Six back cooling plates, both radiation-arm organs, the chest radiant heart and head sensor remain the combat anchors. Added heart framing, heat vents and dense thermal body mass communicate heat management without inventing new weakpoints.

### B09 Null Seraph
Floating suppression monolith. Coffin-like central body, blade wings, null cores, head resonator, stabilizers, halo and lance crown. “Seraph” is ordered ritual geometry rather than a person with wings.

### B10 Worldbreaker
Mobile fortress quadruped. Cathedral-scale supports carry a broad siege hull, belly keel, upper citadel, ramparts, weapon pylons, outer cores, auxiliary organs and central core.

## 7. Normal enemy identity contracts

### Bulwark
A moving section of defensive wall. Front shield/rampart mass must be wider and visually heavier than the body; head and limbs are secondary support structures. The shield boss and asymmetrical side cover prevent a plain “armored zombie” read.

### Howler
A resonance organism. The head/jaw/horn assembly, throat bellows and acoustic rings are intentionally oversized; torso and rear mass stay subordinate. It should read as a living sound weapon even when idle.

## 8. Anti-regression rules

A presentation change fails review if:
- all major masses can be reduced to a vanilla humanoid skeleton without losing identity,
- a renderer-required weakpoint bone is removed or renamed,
- an animation references a bone absent from geometry,
- multiple unrelated bosses converge on the same silhouette,
- a phase mechanic becomes harder to understand because geometry hides its weakpoint,
- internal developer labels appear in player-facing text.

## 9. Ownership and sourcing

Current remastered geometry is project-owned original work. External references may inform quality targets, but copied geometry/textures are not a production shortcut. Any future external runtime asset must be recorded in `ASSET_REGISTRY.md` with source, author, license, modification and usage before merge.

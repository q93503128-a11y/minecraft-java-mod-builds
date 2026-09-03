# TITANBREAK Visual Bible

Status: canonical presentation contract for the current implementation line.  
Design authority: `TITANBREAK_통합_기획서_콘텐츠바이블_v0.3` remains the gameplay/content source of truth. This file translates that content into visual production rules; it does not change mechanics, rewards, progression or balance.

## 1. Visual thesis

TITANBREAK is body-augmentation survival against organisms and machines whose anatomy communicates gameplay. A creature must not read as “a scaled vanilla humanoid with parts attached” unless the content bible explicitly requires a humanoid identity.

Every major silhouette must communicate at first glance: threat role, movement style, important combat structures, scale relative to the player and roster identity without relying on nameplates. Generic head/torso/two-arm/two-leg construction is a fallback, not a house style.

## 2. Shape language

- **Pursuit / kinetic:** forward-biased mass, narrow sensor profile, long contact limbs, rear reactor/keel shapes.
- **Regeneration / flesh:** asymmetry, overlapping masses, circulation and replacement tissue.
- **Analysis / optical:** radial/orbital composition, repeated sensors, nested rings.
- **Temporal:** offset rings, segmented joints, discontinuous/arthropod-like body plan.
- **Suppression / null:** thin monolithic mass, blade arrays, void gaps, suspended stabilizers.
- **Catastrophe / fortress:** broad load-bearing hull, architecture-like superstructure, redundant supports and weapon pylons.
- **Impact / power:** oversized joint armor, dense upper-body mass, short load paths and protected shock organs.
- **Storm / aerial:** horizontal movement axis, fins and dorsal charge organs; avoid standing-animal posture.
- **Thermal / ash:** kiln, furnace, vent, cooling plate and slag masses; heat source requires structural housing.
- **Resonance / sonic:** horn, throat, bellows and ring forms; sound-producing anatomy dominates.

## 3. Weakpoint readability

Destructible parts are gameplay UI rendered in world space.

- A renderer/gameplay weakpoint keeps its own stable GeckoLib bone.
- Renderer-controlled weakpoints must never be merged into decorative parents whose visibility can hide unrelated mechanics.
- Exposed-core transitions use shell/core separation.
- Destroying a part should visibly reduce a recognisable organ or silhouette mass.
- Decoration may frame a weakpoint but must not obscure its approximate location.
- Phase-only field geometry stays subordinate to physical weakpoints.

## 4. Scale hierarchy

Renderer scale and encounter dimensions are design contracts: normal enemy < elite < early boss < regional giant < world-scale boss. B10 Worldbreaker is encounter space as much as entity; its supports, hull, ramparts, organs, pylons and core must form readable vertical layers rather than one enlarged humanoid torso.

## 5. Materials and palette

Textures may reuse project-owned palette baselines, but silhouettes remain unique. Hot/energy organs use concentrated accents against darker housing; neural/optical organs use precise contrast; regenerative tissue uses uneven warm biological values; null/suppression uses low-saturation mass with sharp signal accents; fortress armor uses large dark industrial masses with localized core accents. Do not compensate for weak geometry by adding random glow.

## 6. Boss identity contracts

### B01 The Pursuer
Forward-hunched pursuit engine with long forelimbs, digitigrade rear legs, wedge sensor head, chest core, dorsal reactor and pursuit keel.

### B02 Gravemarch Colossus
Power/berserker giant with broad upper body, oversized forearms, reinforced elbow/knee/ankle masses and dorsal impact armor. Shock-heart and skull armor stay readable.

### B03 Bastion Walker
Squat mobile fortress. Four load-bearing legs and eight armor plates support an outside-climb route toward upper defense node and internal power core. Asymmetric turrets, buttresses and frontal ram reinforce fortification identity.

### B04 The Regnant Flesh
Asymmetric mobile flesh colony with tumor masses, ribs, circulation nodes, regeneration cores, brain sac/stalk and mismatched limbs.

### B05 Hundred-Eyed Watcher
Floating ocular observatory with nested sensor clusters, orbital eye bands, predictive brains, false cores and central visual core. No humanoid silhouette.

### B06 Chronophage
Temporal arthropod/engine with horizontal carapace, mandibles, scythe-like forelimbs, rear pylons, phase joints and concentric temporal structures.

### B07 Storm Leviathan
Long horizontal wandering organism. Four wing membranes, six electric sacs, head sensor, storm organ, organic dorsal charge spine and tail control surfaces keep it alive rather than mechanical.

### B08 Ash Titan
Thermal/radiant guardian. Six cooling plates, both radiation-arm organs, chest radiant heart and head sensor remain combat anchors. Heart framing and heat vents communicate heat management without new weakpoints.

### B09 Null Seraph
Floating suppression monolith with coffin-like body, blade wings, null cores, head resonator, stabilizers, halo and lance crown. “Seraph” is ordered ritual geometry rather than a person with wings.

### B10 Worldbreaker
Mobile fortress quadruped. Cathedral-scale supports carry broad siege hull, belly keel, upper citadel, ramparts, weapon pylons, outer cores, auxiliary organs and central core.

## 7. Normal enemy identity contracts already remastered

### Bulwark
A moving section of defensive wall. Front shield/rampart mass is wider and visually heavier than the body; head and limbs are secondary supports.

### Howler
A resonance organism. Oversized head/jaw/horn assembly, throat bellows and acoustic rings dominate; torso and rear mass remain subordinate.

## 8. Elite identity contracts

### Chrono Hound
Low four-legged temporal pursuit body. Elongated sensor head, chrono core/ring, dorsal fins, phase rails and tail mass communicate dangerous mobility inside temporal fields.

### Null Eye
Floating optic-jammer organism. Central eye, nested jammer structures, relay masses, antennae and trailing tendrils dominate; ordinary humanoid limbs are absent.

### Iron Maw
Grab-and-impact brute. Jaw, clamp forearms, hooks, shoulder bracing and chest impact mass are larger visual ideas than the base skeleton.

### Revenant
Asymmetric regenerative colony. Three canonical regeneration cores remain simultaneously readable as separate organs connected by replacement tissue. Geometry must not imply extra gameplay weakpoints.

### Apex Stalker
Lean optical pursuit predator with forward sensor mass, cloak/fins, twin blades, optic nodes and route-control structures. It should read as a purpose-built hunter rather than a hooded humanoid assassin.

### Shock Choir
Electrical conductor organism. Chest coil, tall spires, conductor ribs/link structures, overload ring and capacitor dominate the body and imply networked overload behavior.

### Siegeback
Moving bunker, not an upright heavy soldier. Four load-bearing supports carry an extremely hard frontal wall, bunker shell, dorsal armored cannon, side armor, ammunition masses and recoil bracing.

### Phase Lurker
Spatially discontinuous temporal predator. Offset shell fragments, phase rings, anchors, veil fins and distortion core make the body look only partially coincident with normal space.

### Warden Node
Battlefield command hub. Command node, mast, halo, relays and formation emitters dominate over arms and legs; it reads as the organizing center of nearby enemies.

### Harvester
Salvage recycler and brood carrier. Oversized harvest vat, intake anatomy, feeder tube, claws, brood pod, spawn cradle and repair structures communicate corpse/debris consumption, self-repair and small-creature production.

## 9. Anti-regression rules

A presentation change fails review if:
- the major masses can be reduced to a vanilla humanoid skeleton without losing identity;
- a renderer-required weakpoint bone is removed or renamed;
- an animation references a bone absent from geometry;
- unrelated roster entries converge on the same silhouette;
- phase/weakpoint readability gets worse;
- internal developer labels appear in player-facing text.

## 10. Ownership and sourcing

Current remastered geometry is project-owned original work. External references may inform quality targets, but copied geometry/textures are not a production shortcut. Any future external runtime asset must be recorded in `ASSET_REGISTRY.md` with source, author, license, modification and usage before merge.

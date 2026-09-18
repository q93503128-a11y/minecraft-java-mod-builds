# Open-World RPG — R01 Asset Phase D Fish / VFX / Audio Source Review

> Date: 2026-09-18  
> Status: **FISH SOURCE LOCATORS + FIRST DIRECT VISUAL TRIAGE + KENNEY VFX/AUDIO SOURCE REVALIDATION COMPLETE — BINARY ACCEPTANCE STILL OPEN**  
> Parent intake: `R01_ASSET_INTAKE.md`  
> Rule: this pass narrows source identity and visible suitability. It does not claim local ZIP acquisition, SHA-256, Blockbench review, Minecraft render acceptance or audition acceptance unless explicitly stated.

---

# 1. R01 four-role fish queue — exact current source evidence

Gameplay remains unchanged:

- Common A — small/streamlined;
- Common B — ordinary-bodied;
- Uncommon — bottom/deeper-water silhouette;
- Rare — elongated predatory silhouette.

Broad fish discovery remains stopped. This pass inspects only the existing four-role queue and immediately adjacent source evidence.

## 1.1 CDmir Esox — Rare role

Authoritative creator page:

`https://opengameart.org/content/esox-animated-fish`

Verified public metadata:

```text
author = CDmir
license = CC0
file = esox.zip
published size = 16.9 MB
direct file URL = https://opengameart.org/sites/default/files/esox.zip
faces = 826
triangles = 1,632
textures = albedo / normal / roughness / specularity
animations @ 60 fps:
  Idle = 480 frames
  Swimming-Slow = 120 frames
  Swimming-Fast = 60 frames
```

The direct ZIP locator is now exact. The current workspace could reach the source page and direct file locator, but its container/network layer could not materialize the ZIP bytes. Therefore:

```text
exact creator file URL: YES
creator-source CC0: YES
binary locally acquired: NO
SHA-256: NO
mesh/animation playback: NO
Minecraft acceptance: NO
```

Role verdict remains **DIRECT_REVIEW_PRIORITY / Rare**. Nothing found in this pass invalidates its freshwater-predator role.

## 1.2 CDmir Fish (Animated) — ordinary Common role

Authoritative creator page:

`https://opengameart.org/content/fish-animated`

Verified public metadata:

```text
author = CDmir
collaborator = TinyWorlds
license = CC0
file = fish.zip
published size = 2.9 MB
direct file URL = https://opengameart.org/sites/default/files/fish.zip
rigged = yes
animated = yes
diffuse texture = yes
creator description = ready to use ingame
```

The exact direct ZIP locator is now pinned. Local binary materialization failed for the same workspace/network reason as Esox.

Role verdict remains **DIRECT_REVIEW_PRIORITY / ordinary Common**, pending actual visual/animation review.

## 1.3 Quaternius Armored Catfish — Uncommon role

Authoritative model page:

`https://poly.pizza/m/mtd9QK5yCe`

Verified public metadata:

```text
author = Quaternius
license = Public Domain / CC0
format = FBX / GLTF
tags = Low Poly / Animated
model id = mtd9QK5yCe
```

Direct current preview review shows:

- very broad, blunt armored-catfish head;
- compact/deep body;
- pronounced fins and strong bottom-feeder silhouette;
- low-poly form is visually compatible with Minecraft-scale readability;
- current preview material is extremely dark, so underwater readability must be tested before acceptance.

Decision:

```text
freshwater role fit = PASS
silhouette distinction from Common fish = PASS
low-poly style compatibility direction = PASS
dark-water readability = NEEDS_MINECRAFT_REVIEW
binary/animation acceptance = NOT DONE
status = DIRECT_REVIEW_PRIORITY retained for Uncommon
```

Do not replace it merely because the preview is dark. First test lighting/material adaptation within the allowed external-asset adaptation boundary.

## 1.4 Small Fish — Common Minnow

Authoritative collection pages:

`https://sbox.game/fish/~assets`  
`https://sbox.game/fish/fishes/`

Verified current source evidence:

```text
publisher/team = Small Fish
Common Minnow = Model / Released / CC0 / 5 MB
collection = Fish Models
collection statement = fish ragdoll models
collection statement = each model made from the real-world look of the named fish
artist credited by collection = Grodbert
```

The same source currently exposes multiple other CC0 freshwater identities — Common Pike, Chain Pickerel, Brown Trout, Yellow Perch, Chinook Salmon, Golden Trout and others — but **they are not promoted into the R01 queue in this pass**. The existing four-role queue stays authoritative unless one selected role fails direct review.

Common Minnow remains the correct small-stream morphology direction. Exact individual package/file locator is still not surfaced by the currently indexed public page.

Decision:

```text
CC0 source evidence = PASS
freshwater identity = PASS
small-stream role = PASS
exact package locator = OPEN
binary/texture/rig review = OPEN
status = DIRECT_REVIEW_PRIORITY retained
```

---

# 2. Fish queue conclusion

Current R01 queue after this pass:

| R01 role | Candidate | Phase-D result |
|---|---|---|
| small streamlined Common | Small Fish Common Minnow | retain; exact package locator + binary review still required |
| ordinary-bodied Common | CDmir Fish (Animated) | exact creator page + direct ZIP locator pinned; binary review blocked by workspace |
| bottom/deeper Uncommon | Quaternius Armored Catfish | **first direct visible suitability pass completed; role/silhouette pass**; Minecraft-water readability pending |
| elongated predatory Rare | CDmir Esox | exact creator page + direct ZIP locator pinned; binary review blocked by workspace |

No candidate is promoted to `FINAL_ACCEPTED` yet.

The remaining fish blocker is now specifically:

```text
1. materialize Common Minnow exact individual package;
2. materialize CDmir fish.zip / esox.zip;
3. acquire exact Poly Pizza FBX/GLTF for Armored Catfish;
4. hash source bytes;
5. inspect mesh/texture/rig/animation;
6. compare all four together at the same Minecraft render scale;
7. only then lock final player-facing species names and Fish Codex icons.
```

---

# 3. Kenney VFX / audio source revalidation

The project already selected Kenney as the reusable baseline family. This pass rechecked the current official pages so implementation does not rely on stale pack descriptions.

## VFX

### Particle Pack

`https://kenney.nl/assets/particle-pack`

Current verified metadata:

```text
category = 2D / VFX
tile size = 512 × 512
files = 80
license = CC0
release line = 1.0 / 2018
```

### Smoke Particles

`https://kenney.nl/assets/smoke-particles`

Current verified metadata:

```text
category = 2D / VFX
files = 70
license = CC0
release line = 1.0 / 2014
```

These remain baseline component pools only. Boss/class signature VFX still require authored composition and cannot ship as one untouched generic sprite.

## Audio

### RPG Audio

`https://kenney.nl/assets/rpg-audio`

```text
files = 50
license = CC0
tags = foley / rpg / footstep / weapon
```

### Impact Sounds

`https://kenney.nl/assets/impact-sounds`

```text
files = 130
license = CC0
tags = impact / foley
```

### UI Audio

`https://kenney.nl/assets/ui-audio`

```text
files = 50
license = CC0
tags = button / switch / click
```

### Interface Sounds

`https://kenney.nl/assets/interface-sounds`

```text
files = 100
license = CC0
tags = interface / click / button
```

Decision:

- current family/license/count evidence remains valid;
- no new audio family search is warranted for ordinary baseline UI/foley/impact work;
- exact clips are **not** accepted until auditioned against the real R01 mix;
- Earthloong, Regalhart, class Ultimates and Mythic feedback may still require stronger signature layers on top of these baselines.

---

# 4. Workspace limitation recorded

This pass attempted to move from browser-visible ZIP URLs into local binary inspection.

Observed limitation:

```text
web layer can resolve OpenGameArt direct ZIP URL
container download/materialization from that external host = failed
container DNS/network access to OpenGameArt = unavailable
```

Therefore the project must not repeatedly spend future passes rediscovering those URLs.

Exact CDmir direct URLs are now canonically recorded. A future environment with ordinary file download access should start directly from them.

---

# 5. Verification

```text
R01 fish broad search reopened: NO
CDmir exact creator ZIP locators pinned: YES
Armored Catfish direct visual role review: YES
Common Minnow current source/license/collection evidence: YES
four fish binaries acquired: NO
four fish SHA-256 recorded: NO
four fish compared in 3D/Minecraft: NO
final R01 fish roster: NO
final species names: NO

Kenney current VFX source pages revalidated: YES
Kenney current baseline audio source pages revalidated: YES
exact VFX sprites selected: NO
exact audio clips auditioned/selected: NO

BUILD VERIFIED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

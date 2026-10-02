# Open-World RPG — Azari R01 Client Review

> Status: **DIRECT NBT SURFACE REVIEW COMPLETE FOR CURRENT CANDIDATES / FINAL CLIENT VISUAL ACCEPTANCE DEFERRED TO INTEGRATED R01 FIRST-COMPLETE TEST**
>
> Date: 2026-09-28
>
> Spatial evidence: `AZARI_R01_SPATIAL_PASS2.md`, `AZARI_R01_QUARRY_INTERIOR_PASS3.md`, `AZARI_R01_SURFACE_SPATIAL_PASS4.md`, `AZARI_R01_FIELD_SPATIAL_PASS5.md`, `AZARI_R01_ALDERFORD_LANDMARK_PASS6.md`, `AZARI_R01_GATHERING_SPATIAL_PASS7.md`
>
> Rule: this is developer-only inspection workflow. Nothing in this file promotes a candidate coordinate to gameplay authority.

## 1. Purpose

The creator-acquired Azari archive remains local-only. Do not commit, upload, redistribute or modify the original archive.

**This is not a current user test task.** Pass 4 reopened the four persisted R01 slice archives and resolved the current surface Y/topography questions directly from Anvil data. Keep this workflow for the later integrated visual/feel acceptance after the authored R01 world content exists.

The review workflow creates a fresh Minecraft save containing only the already-inspected R01 overworld slice:

```text
region X = -8 .. 4
region Z =  0 .. 12
```

That copy contains:

- `level.dat`;
- all 169 required overworld terrain region files in the R01 rectangle;
- matching entity-region files that exist in the source archive;
- matching POI-region files that exist in the source archive;
- world datapack files;
- optional `level.dat_old`, `icon.png` and `resources.zip` when present.

It intentionally does not copy creator `playerdata`, `stats`, `advancements`, unrelated regions or Nether/End region data.

The original ZIP is read-only from this workflow's point of view.

## 2. Build the local review save

Canonical local source location:

```text
projects/openworld-rpg/.local/azari/source/azari.zip
```

From `projects/openworld-rpg/`, run:

```powershell
py tools\azari_world_intake.py `
  ".local\azari\source\azari.zip" `
  --output ".local\azari\intake\azari-world-report.json" `
  --extract-r01-review "<YOUR_OPENWORLD_RPG_INSTANCE>\saves\Azari_R01_Review"
```

Use the actual `saves` directory of the Minecraft instance that runs the Openworld RPG gameplay stack. Do not assume the default launcher path when using Modrinth or another isolated instance.

The destination must not already exist. The tool refuses to overwrite an existing world save.

Expected R01 terrain result:

```text
copied_region_files = 169
missing_terrain_region_files = []
```

The actual creator slice previously supplied for analysis contained:

```text
terrain region files = 169
entity region files  = 87
POI region files     = 102
```

The extraction command should reproduce those counts when run against the same creator archive. If the terrain count is not 169, stop review and inspect the JSON report instead of filling the missing area with generated terrain.

## 3. First client launch

Open `Azari_R01_Review` in the Openworld RPG test instance.

This review save is disposable. The source ZIP remains untouched.

If Minecraft rejects the save because of a world-version/datapack error, record the exact message and stop. Do not force-convert the creator archive itself and do not remove datapacks merely to make the world load.

For spatial inspection:

```mcfunction
/gamemode spectator
```

Keep normal gameplay code disconnected from all candidate coordinates during this pass.

## 4. Surface review order

Pass 4 already resolved raw terrain Y/topography from the actual Anvil bytes. The later client pass is for **visual composition and gameplay feel**, not for discovering coordinates that tooling can read directly.

Surface teleports may still use `Y=160` as an inspection altitude.

### Alderford

```mcfunction
/tp @s -2208 160 4000
/tp @s -2240 160 4048
```

Direct-NBT candidate facts:

```text
Alderford center:                (-2208, 67, 4000)
gate / first-shrine candidate:   (-2240, 67, 4048)
horizontal separation:           ~57.7 blocks
```

Pass 6 also binds terrain-level shell centers for the locked Alderford service roster and all five launch housing shells. Those centers are implementation candidates only; exact prefab footprint/orientation still follows accepted asset bounds.

Later integrated review checks:

- authored gate → shrine reveal;
- shrine visibility immediately after the gate reveal;
- central square and service silhouettes at gameplay FOV;
- stable visibility toward the outward road;
- first/third-person circulation after the real structures exist.

### Old Quarry Road

Use the refined semantic route:

```mcfunction
/tp @s -2208 160 4000
/tp @s -2240 160 4048
/tp @s -2404 160 4312
/tp @s -2472 160 4388
/tp @s -2520 160 4480
/tp @s -2560 160 4660
/tp @s -2600 160 4700
```

Direct-NBT surface candidates are already known:

```text
Broken Road Marker  ≈ (-2404, 71, 4312)
Roadside Trouble    ≈ (-2472, 71, 4388)
Lost Cargo          ≈ (-2520, 73, 4480)
```

The later client pass judges whether authored evidence, wagon, props and road dressing are visible/readable in the intended sequence. It does not need to rediscover the terrain Y values.

### Quarry exterior

```mcfunction
/tp @s -2560 160 4660
/tp @s -2628 160 4764
/tp @s -2600 160 4700
```

Direct-NBT candidates remain:

```text
Quarry Waystone:       (-2560, 67, 4660)
Quarry overlook:       (-2628, 96, 4764)
lower entrance:        (-2600, 64, 4700)
```

The Waystone ↔ lower-entrance horizontal separation is about 56.6 blocks, inside the canonical 35–70 block rule.

The integrated visual pass still checks entrance facing, overlook composition, supports/carts/hoist and the authored transition into the dungeon.

## 5. Travel-time review

Pass 4 verifies distance and dry-terrain plausibility, **not real travel time**.

Current refined semantic route:

```text
Alderford center → lower entrance ≈ 815.2 horizontal blocks
Alderford center → Roadside Trouble ≈ 470.5 horizontal blocks
Quarry Waystone → lower entrance ≈ 56.6 horizontal blocks
```

Do not interrupt R01 implementation for a partial timing test. Measure normal on-foot and Trail Stag timings during the integrated R01 first-complete client acceptance, after the authored road, props, encounters and movement presentation exist.

Current spatial targets remain:

```text
Alderford gate → square: 20–35 s
square → Greenwater Ford activity edge: 45–75 s
square → first Alder Meadow meaningful interaction: 45–90 s
square → Old Quarry Road disturbance belt: 90–150 s
nearest practical R01 checkpoint → quarry entrance after discovery: 60–120 s
```

The separate Quarry Waystone placement rule remains 35–70 blocks of legal travel from the lower entrance.

## 6. Quarry interior review order

Remain in spectator mode for the Pass-3 review volumes. These teleports go to **review-volume centers**, not final room anchors.

### Upper Mining Gallery

```mcfunction
/tp @s -2558.5 39.5 4737.5
```

Review shell:

```text
x -2576 .. -2541
y    32 ..    47
z  4722 ..  4753
```

### Collapsed Hoist

```mcfunction
/tp @s -2592.5 35.5 4789.5
```

Review shell:

```text
x -2612 .. -2573
y    24 ..    47
z  4770 ..  4809
```

### Root-Breached Workings

```mcfunction
/tp @s -2616.5 31.5 4821.5
```

Review shell:

```text
x -2636 .. -2597
y    22 ..    41
z  4802 ..  4841
```

### Relay Gallery

```mcfunction
/tp @s -2585.5 27.5 4829.5
```

Review shell:

```text
x -2596 .. -2575
y    22 ..    33
z  4818 ..  4841
```

### Earthloong carve probe

```mcfunction
/tp @s -2570.5 19.5 4833.5
```

Review shell:

```text
x -2586 .. -2555
y    14 ..    25
z  4818 ..  4849
```

The Earthloong volume is only a verified solid-rock carve pocket. It is not the final arena size.

Do not switch to survival inside a solid review volume.

## 7. Interior acceptance questions

For Upper Gallery → Hoist → Root-Breached → Relay → Earthloong, record:

- usable natural cave seam versus rock that must be authored/excavated;
- entrance and exit facing;
- readable player route at normal gameplay FOV;
- ceiling/wall clearance for Cave Centipede and Nature Spirit spawn anchors;
- Hoist vertical navigation readability in first and third person;
- choke widths and co-op revive space;
- entrance → boss travel time;
- whether Relay reads as a short story transition rather than another combat room.

Earthloong final arena sizing remains open until the actual donor model can be reviewed in this space. Final sizing must account for:

- donor body and camera distance;
- 12-block Lightning Furrow;
- four-lane lateral spacing;
- 4.5-block Root Breaker radius;
- dodge corridors;
- co-op/revive spacing;
- breakable props;
- entrance/aftermath sightline.

Do not use a temporary vanilla boss or generic proxy model to close that decision.

## 8. Review record

For each surface/interior candidate, capture at least:

```text
candidate id:
client loaded: yes/no
observed surface Y: value / n/a
first-person readable: yes/no + note
third-person readable: yes/no + note
route/entrance facing:
measured travel time:
collision/choke issue:
camera issue:
terrain/authored-structure conflict:
recommended status: keep_candidate / reject_or_move / ready_for_client_verified
screenshots:
notes:
```

`ready_for_client_verified` is a review recommendation only. The JSON status is changed separately after the evidence is checked.

## 9. Promotion rule

The spatial states remain:

```text
candidate
→ client_verified
→ production
```

No candidate is promoted simply because the save loaded.

Surface evidence must close the real Y/sightline/travel questions. Interior evidence must close shell/orientation/traversal questions. Earthloong additionally requires accepted donor-model/camera sizing before its final production arena bounds can exist.

Until that happens:

```text
R01 SPATIAL_BINDING COMPLETE: NO
R01 IMPLEMENTED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```


## 10. Production-source promotion contract — 2026-10-02

The runtime promotion path is now mechanically reachable without weakening the current candidate
gate.

Spatial authority uses two independent conditions:

```text
dataset source status
+ individual binding status
```

Current bundled review data remains:

```text
source_status = actual_r01_slice_candidate
```

Under that source status, an accidentally edited individual `production` entry is rejected by
validation and production accessors remain closed.

After actual client/world acceptance, the dataset may be deliberately promoted to:

```text
source_status = actual_r01_slice_production
```

Only entries explicitly marked `production` then become visible to runtime gameplay. Historical
review volumes may remain `candidate` provenance in the same file. They do not need to be
misrepresented as final room geometry.

Final production volumes use:

```text
review_mode = runtime_authored
```

A review probe (`natural_seam`, `transition_probe`, `solid_carve_probe`) cannot itself be
flagged as a production runtime volume.

Alderford structure binding follows the same source-level rule. Exact accepted compositions may
only become live after the structure dataset is explicitly promoted from
`family_bound_composition_gated` to `production_composition_bound`; the promoted source requires
all 15 shells, 9 services and 5 properties to be production-ready with accepted exact composition
IDs.

This work removes an implementation dead-end. It does **not** promote any current coordinate,
structure, service, property, Quarry room or Earthloong arena.

Verification:

- code state: `02c73c66f656bfee4aa8c77565bdeafe50c02c82`;
- Build Openworld RPG run `36961511251`: **SUCCESS**;
- unit tests / build: PASS;
- pinned R01 creature surface inspection: PASS;
- core server smoke: PASS;
- gameplay dependency server smoke: PASS;
- gameplay client startup smoke: PASS;
- verification JARs / mrpack / artifact upload: PASS;
- artifact ID: `11208028054`.

The state therefore remains:

```text
R01 SPATIAL_BINDING COMPLETE: NO
R01 IMPLEMENTED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

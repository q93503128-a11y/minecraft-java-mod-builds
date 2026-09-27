# Open-World RPG — Azari R01 Client Review

> Status: **LOCAL REVIEW SAVE PREPARED BY TOOLING / REAL MINECRAFT CLIENT REVIEW STILL REQUIRED**
>
> Date: 2026-09-27
>
> Spatial evidence: `AZARI_R01_SPATIAL_PASS2.md`, `AZARI_R01_QUARRY_INTERIOR_PASS3.md`
>
> Rule: this is developer-only inspection workflow. Nothing in this file promotes a candidate coordinate to gameplay authority.

## 1. Purpose

The creator-acquired Azari archive remains local-only. Do not commit, upload, redistribute or modify the original archive.

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

Surface teleports deliberately use `Y=160` as an **inspection altitude**, not as a spatial binding. Descend in spectator mode, stand at the real terrain surface, and record the observed ground/standing Y from the client.

### Alderford

```mcfunction
/tp @s -2208 160 4000
/tp @s -2280 160 4080
```

Inspect:

- whether the candidate core can support the locked settlement topology without flattening signature terrain;
- whether a gate → first shrine reveal is readable;
- whether the shrine can be visible immediately after the gate reveal;
- whether a roughly 25–35 block central square and 45–60 block gate-to-market relationship remain plausible;
- service circulation, stable visibility and future-route sightlines.

Do not lock exact service/building coordinates yet.

### Old Quarry Road

Review the route in order:

```mcfunction
/tp @s -2208 160 4000
/tp @s -2260 160 4100
/tp @s -2320 160 4200
/tp @s -2380 160 4300
/tp @s -2440 160 4400
/tp @s -2500 160 4500
/tp @s -2560 160 4600
/tp @s -2560 160 4660
/tp @s -2600 160 4700
```

The three evidence probes intentionally have no accepted Y yet:

```text
Broken Road Marker  ≈ (-2380, ?, 4300)
Roadside Trouble    ≈ (-2440, ?, 4400)
Lost Cargo          ≈ (-2500, ?, 4500)
```

Record the real terrain Y in-client. Do not copy a guessed `67` or another nearby surface value into spatial data.

Review whether each evidence beat is physically legible from the intended road and whether the sequence feels like a route rather than three disconnected pins.

### Quarry exterior

```mcfunction
/tp @s -2560 160 4660
/tp @s -2628 160 4764
/tp @s -2600 160 4700
```

Inspect:

- Waystone placement space;
- overlook reveal and gameplay-FOV sightline;
- lower-entrance face/orientation;
- support/cart/hoist structure footprint;
- whether the exterior can lead into the authored interior without a long dead tunnel.

## 5. Travel-time review

The Pass-2 polyline is approximately 812 horizontal blocks, but that number is not accepted travel time.

After identifying a plausible walkable line:

1. return to the intended Alderford outward-road start;
2. use the normal intended player movement state, not spectator/creative flight;
3. follow the candidate route to the Quarry entrance;
4. record elapsed time and any forced detours;
5. repeat later with the accepted Trail Stag implementation when that presentation/runtime gate is available.

Current spatial targets from the project canon remain:

```text
major POI → next meaningful route decision/POI: roughly 1–3.5 min
first-visit dungeon approach from practical service/shrine: roughly 2–5 min
repeat route after shortcut/shrine: materially shorter
```

A raw distance matching a target is not sufficient if the route contains dead travel or unreadable turns.

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

# Region 01 Threat-Staging Field Review

Status: **AUTOMATED IMPLEMENTATION CANDIDATE — HUMAN FIELD ACCEPTANCE OPEN**

Implementation checkpoint: `f49357355899d3efb0e21f0101c837c5e28d0353`.

## Why this batch exists

The preceding Region 01 combat-space checkpoint intentionally assigns z=-5..2 to the combat field and leaves z=3..5 to the existing extraction-relay presentation. The encounter runtime still used older technical spawn coordinates: Hunters began at z=3 in relay-owned space and Scouts began at z=-3 immediately beside the arrival side. That meant the new cover geometry and the role staging were not actually designed as one playable encounter.

This batch changes only initial technical staging. It does not add a new encounter state, alter pressure math, change mob stats, add UI, or claim final creature/environment art.

## Reviewed staging contract

Current maximum pressure composition remains unchanged: up to three Hunters, three Scouts and one Elite.

Local cells relative to the Region 01 technical center:

- Hunter: `(-4,-1)`, `(-4,1)`, `(-4,2)`
- Scout: `(4,2)`, `(4,0)`, `(4,-2)`
- Elite anchor: `(0,2)`

Contract properties:

- all cells are inside the combat-owned z=-5..2 space;
- no cell overlaps the arrival lane `(0,-4)`, central salvage `(0,0)`, relay `(0,5)`, or the four corner salvage positions;
- no cell overlaps the current staggered cover pillars;
- Hunter/Scout spawn cells are unique and placed on opposite flanks;
- the existing pressure-driven role counts select the first N reviewed cells rather than inventing coordinates at runtime;
- if later content asks for more than three Hunters or Scouts, runtime fails closed until more staging cells are deliberately reviewed.

The same Minecraft Trial Chambers reference boundary already recorded by `M2B_REGION01_COMBAT_SPACE_REVIEW.md` applies here: mixed melee/ranged pressure should benefit from readable, varied combat space. No Trial Chamber layout or external asset bytes are copied.

## Automated verification

`Build Riftfrontier` workflow `34890699183` completed **SUCCESS** for `f49357355899d3efb0e21f0101c837c5e28d0353`.

Passed:

- asset-intake tests;
- `clean test build`;
- pure `Region01FieldArenaPlanTest` staging assertions;
- required native GameTest gate including the real `region_01_encounter_runtime`;
- dedicated-server smoke;
- Xvfb client smoke;
- executable-JAR inspection;
- deliverable/report upload.

Deliverable:

- artifact id `10366927173`
- archive digest `sha256:37e476566e8fe5e6096a2cd60d89503a3c2f69a9d3fbee48f5112ed2c13a3c0b`
- executable JAR SHA-256 `9cd41f6b080113e5fa79b4e461e917b73cc70e212d726a27fc10554d54968686`

This proves build/runtime smoke integrity, not human encounter quality.

## Exact human review

1. Use Minecraft 26.2, NeoForge 26.2.0.38-beta and the verified JAR.
2. Start a fresh/disposable world and deploy through the normal hub -> lodestone path.
3. On the first low-pressure Region 01 run, observe the encounter before deliberately pulling mobs around.
4. Hunter should begin from the west flank, Scout from the east flank, and Elite from the open back-center. No starting patrol actor should occupy the far extraction-relay approach.
5. Walk forward from `(0,-4)`. The opening must not be an unavoidable body-pin or instant crossfire.
6. Use the staggered cover to break/reopen Scout sightlines. The Scout must still become threatening when sightline is exposed.
7. Pull Hunter and Elite around both cover sides. Repeated path stalls or permanent traps reject the layout.
8. Recover central/corner salvage. Spawn staging must not prevent interaction with any objective.
9. Extract normally after 3/3. Relay readiness and extraction behavior must remain unchanged.
10. Repeat at higher pressure if possible. Additional Hunters/Scouts must use the extra flank cells without spawning inside each other or creating an unavoidable surround.
11. For multiplayer acceptance, repeat with two actual clients; automated server/client smoke does not count.

## Reject conditions

Reject or revise staging if any of these are observed:

- player is pinned before meaningful movement from arrival;
- opening Scout crossfire is effectively unavoidable;
- a Hunter/Elite repeatedly fails to route around current cover;
- a technical threat starts in z=3..5 relay-owned space;
- a spawn obstructs salvage or relay interaction;
- higher-pressure cells create persistent body-blocking or overlapping actors;
- staging makes one role irrelevant or trivially farmable from safe cover.

Do not auto-tune these coordinates from automation-only evidence. Human field play is the acceptance gate.

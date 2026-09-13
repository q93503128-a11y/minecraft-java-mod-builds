# Region 01 Boss Presentation Gate

Status: **GEOMETRY/RIG + CUSTOM RENDER PATH SELECTED — FINAL MATERIAL / ATTACK SOURCE BINDINGS / VFX / SOUND STILL GATED**

This document is the current design gate between the verified M3 presentation runtime and the remaining real Region 01 boss presentation work. It must not be read as a reason to restart model selection or renderer research that has already been closed.

## 1. Current selected direction

The first Region 01 boss geometry/rig source is the Quaternius CC0 **Dragon Evolved** derivation recorded in `docs/THIRD_PARTY_ASSETS.md` and `assets/sources/region_01_boss_dragon_evolved.acceptance.json`.

The production rendering direction is Riftfrontier's existing native custom skinned-mesh importer/renderer. GeckoLib is **not** on the current first-boss critical path and must not be added merely to duplicate this working path.

The accepted art-neutral runtime resource is:

```text
riftfrontier:boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf
```

The original source `Atlas` material/texture was deliberately stripped and is **not approved as final Region 01 art**.

What remains unresolved is not another model search. The open production gates are:

- explicit gameplay-semantic source-animation binding;
- final material/texture treatment;
- VFX;
- sound;
- Minecraft scale/hit-geometry evidence;
- human combat readability and field-play.

## 2. Combat identity requirements

The first Region 01 boss must be readable as a **position-control / commitment-punish** encounter rather than a high-HP version of the M2 proxies.

Required combat language:

- at least one broad committed melee/impact action with an unmistakable pre-hit pose;
- at least one displacement or line-pressure action that forces lateral or range-management movement;
- at least one arena-pressure action whose danger area is visible before activation;
- recovery poses long enough for the player to recognize a counterattack window;
- phase change must alter attack composition or arena pressure, not merely damage/health multipliers;
- every damaging action must remain driven by the authoritative `AttackPattern` timing source;
- animation, VFX and sound may emphasize timing but may not create a second independent hit clock.

The production semantic content already implements the three required roles and two-phase composition. This gate now governs how those roles become readable physical presentation.

## 3. Silhouette and Minecraft-fit requirements

The selected Dragon derivation must still pass these conditions in actual Minecraft field use before encounter promotion:

1. **Readable facing** — front/back and attack-bearing side remain obvious at typical third-person combat distance.
2. **Attack-bearing mass** — the limb/head/body motion performing a hit visually supports the real hit volume.
3. **Telegraph headroom** — anticipation, ACTIVE motion and recovery are materially distinguishable rather than only whole-body translation.
4. **Minecraft scale fit** — the boss is neither a tiny low-detail toy nor a screen-filling shape whose feet/weak points disappear.
5. **Texture/style fit** — final material treatment coheres with Region 01 and Minecraft rather than importing an unrelated low-poly palette unchanged.
6. **Variant stability** — phase/presentation variants retain the accepted skeleton and deterministic resolver contract.
7. **Performance fit** — final material/VFX/animation complexity stays reasonable alongside encounter actors.

Geometry selection is closed unless actual Minecraft evidence proves that this accepted direction cannot satisfy these requirements.

## 4. Animation acceptance matrix

Minimum production clips or equivalent state-driven poses:

| Semantic role | Minimum visual requirement |
|---|---|
| idle / locomotion | facing and movement direction remain readable |
| telegraph: committed strike | unmistakable anticipation pose before ACTIVE |
| active: committed strike | visible strike path aligned to hit volume |
| recovery: committed strike | explicit vulnerable/reset pose |
| telegraph: line/displacement | direction and travel lane readable before movement |
| active: line/displacement | movement/action weight communicates current danger |
| telegraph: area pressure | future danger area is understandable before activation |
| phase transition | cannot be mistaken for ordinary hit-stun or idle |
| hurt / stagger where used | does not cancel authoritative attack state unless server combat state says so |
| death | terminal, non-looping presentation |

One generic `attack` animation copied across mechanically different attacks is not production-complete.

The source-motion review has directly observed `Punch` and `Headbutt` and exact source phase windows exist for those reviewed clips. That evidence does **not** authorize `Punch -> committed_strike`, `Headbutt -> line_displacement`, or source ACTION -> server ACTIVE. A gameplay-role binding must explicitly review the observed motion against the intended authoritative attack.

Arena pressure currently has no approved source-motion mapping and must remain unresolved until a compatible, legally usable/authored motion is reviewed.

## 5. VFX and sound contract

VFX and sound are timing channels, not decoration.

- Telegraph VFX must appear early enough to communicate the same danger that `AttackPattern.telegraph` represents.
- ACTIVE VFX must not visually claim damage outside the authoritative hit window.
- Recovery VFX/sound must decay instead of continuing to signal an active threat.
- High-priority boss cues need frequency separation from ambient Region 01 audio.
- Camera shake, flash, bloom, particles and screen effects are optional and must not become the only source of readability.
- Color-only telegraphs are insufficient; shape, motion, placement or sound must provide redundant information.

No final VFX or sound asset is selected merely because a logical presentation key now exists.

## 6. Provenance and physical asset status

### Quaternius — Dragon Evolved

- Status: **SELECTED** for Region 01 first-boss geometry/rig derivation.
- Author: Quaternius.
- Family source: https://quaternius.com/packs/ultimatemonsters.html
- License: CC0 1.0 / public-domain dedication, with exact source/hash/provenance recorded in `docs/THIRD_PARTY_ASSETS.md`.
- Runtime use: art-neutral sanitized glTF consumed by Riftfrontier's custom skinned-mesh pipeline.
- Source `Atlas`: intentionally excluded from accepted production art.
- Source motion inventory: retained, but gameplay-semantic mappings remain separately review-gated.

Rejected/alternative Quaternius candidates remain historical evidence in `REGION_01_BOSS_CANDIDATE_AUDIT.md`; do not reopen that search without evidence that Dragon Evolved fails the Minecraft field gate.

### GeckoLib

GeckoLib was previously researched as a technology candidate. That research is now superseded for the first-boss path by the verified custom Riftfrontier skinned-mesh renderer. Do not add GeckoLib solely to re-express the accepted Dragon geometry/rig path.

## 7. Logical profile versus selected physical assets

The packaged logical profile `region_01_first_apex.json` now exists and covers the production three attacks across TELEGRAPH / ACTIVE / RECOVERY.

Those model/animation/VFX/sound IDs are logical contract keys. They are **not** evidence that final physical resources exist.

`ContentServerReloadListener` intentionally stages the logical boss presentation instead of publishing it into the authoritative runtime while no real `presentation_assets` manifest exists. Do not create placeholder physical paths or a fake manifest to bypass this boundary.

A real `presentation_assets` manifest becomes appropriate only after a coherent set of actual production resources has been selected, provenance-reviewed and present in the repository.

## 8. Field-test boundary

A development-only boss combat harness now exists so the authored server semantics can be exercised before production encounter promotion:

```text
/riftfrontier boss fieldtest spawn
/riftfrontier boss fieldtest phase1
/riftfrontier boss fieldtest phase2
```

Its neutral AABB and `1.0F` damage are diagnostic only. It does not prove final attack geometry or balance, and it does not authorize adding the boss to Region 01 production encounter composition.

The exact human procedure is `docs/M3_REGION01_BOSS_FIELD_PLAY.md`.

## 9. Exact next implementation boundary

1. Run/record human field evidence for the development boss harness when a person is available; do not invent `PLAYTESTED` or `MULTIPLAYER TESTED` results.
2. Explicitly review whether observed `Punch` and/or `Headbutt` motion truly fits a production gameplay role before authoring source bindings.
3. Resolve arena-pressure motion with a reviewed legal/authored source compatible with the selected rig direction; fail closed otherwise.
4. Select a real final material/texture direction with documented commercial-game/major-mod visual references plus legal provenance for any external bytes. Do not invent an arbitrary palette and do not restore stripped Atlas by default.
5. Select or author VFX/sound under the same readability/provenance constraints.
6. Author the first real `presentation_assets` manifest only after all referenced physical resources actually exist and pass review.
7. Publish through the existing validated reload/render pipeline and verify resource reload, animation timing, material rendering, VFX/sound timing and Minecraft readability.
8. Only then consider production Region 01 encounter insertion and final attack geometry/damage tuning.

## 10. Explicitly still NOT TESTED

- final Region 01 boss material/texture in Minecraft;
- final boss scale/hitbox alignment;
- approved production gameplay-source animation playback;
- arena-pressure motion;
- VFX and sound timing;
- full published presentation manifest reload;
- human combat readability;
- human boss combat field play;
- multiplayer boss combat field play.

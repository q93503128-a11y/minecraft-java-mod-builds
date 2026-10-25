# Augmentation Survival — Implementation Status

Date: 2026-10-25
Supersedes: `IMPLEMENTATION_STATUS_2026-10-24.md`
Status: M1C FUNCTIONAL BASELINE / 135-OF-146 GAMEPLAY RUNTIME / CLONE AVATAR-SYNC CHECKPOINT VERIFIED

## 146-roster implementation count

This count measures whether the canonical gameplay effect has a runtime implementation. It does **not** mean final VFX/UI/audio polish or live gameplay testing is complete.

- Fully implemented gameplay runtime: **135 / 146**
- Partially implemented: **1 / 146** — `분신`
- Not fully implemented: **11 / 146**
- No gameplay runtime yet: **10 / 146**
- `풍요` is FULL and `휩쓸기 강화` remains FULL from its earlier verified runtime.

## Verified FULL runtime notes

### `전리품 챙기기`

`LivingEntity.dropFromLootTable` wraps only the killed entity's final entity-loot-table `Consumer<ItemStack>`. One canonical **5% / 10% / 15%** roll duplicates that exact loot-table bundle. Nearby drops, another mob's drops, equipment drops and unrelated custom death drops cannot be captured accidentally. Player victims, dominated companions, configured bosses and augment cores remain excluded.

### `풍요`

`풍요` duplicates the exact final compatible ordinary-drop stack at the producer boundary rather than scanning nearby `ItemEntity` objects.

- Direct mining / direct harvest: final stacks emitted through `Block.popResource` are duplicated for the server player owning the active direct `destroyBlock` call. Derived ResourceHarvestService breaks are excluded.
- Fishing: the actual final fishing loot list is duplicated without re-rolling the fishing loot table.
- Normal-mob direct kill: the killed entity's exact final loot-table consumer is wrapped and root-direct player provenance is required.
- Cores, explicit `augmentation_survival:abundance_excluded` entries, non-stackable/damageable equipment and custom-data/name/lore stacks remain excluded.

### `휩쓸기 강화`

This augment is not in the remaining no-runtime set. Vanilla sweep expansion and the optional Better Combat 3.2.2 adapter were already implemented and full-pack smoke-verified in Build #81. Live combat feel and multiplayer behavior remain untested.

## `분신` presentation/runtime checkpoint

The clone continues to use the registered custom `CombatCloneEntity`, server-owned autonomous melee AI, 20-second lifetime, activation-time 50% max-health snapshot, 60% attack-damage snapshot, 60-second base cooldown, ally checks and non-player-root damage provenance.

The player-avatar presentation path is now materially stronger than the previous fixed-wide baseline:

- On activation, the clone snapshots the owner's main hand, off hand, head, chest, legs and feet equipment as copied stacks. The copied presentation gear has zero drop chance and the clone still cannot pick up loot, so it cannot become an item-duplication path.
- Owner UUID is now carried through `SynchedEntityData`, so the client clone no longer depends on a server-only plain field when resolving the owner's skin.
- The owner's enabled player-model-part bitmask is snapshotted and synchronized. Hat, jacket, left/right sleeves and left/right pants layers follow the activation-time owner settings.
- The renderer resolves the owner's `PlayerSkin` from the client connection/player-info skin cache, with deterministic UUID-derived default-skin fallback if unavailable.
- Both wide and slim `PlayerModel` bodies are baked. The clone selects the body model from the resolved `PlayerSkin.model()` value rather than forcing every owner onto the wide model.
- Humanoid armor and held-item render layers remain installed, and owner handedness is snapshotted.
- Cape presentation is intentionally not claimed as reproduced yet.

`분신` remains **PARTIAL**, not FULL. The largest remaining correctness gate is now the combat snapshot contract: the current 60% damage snapshot still derives from the owner's `Attributes.ATTACK_DAMAGE` value rather than a proven full player combat-power package across the project's combat integrations. In addition, no live visual/combat playtest has yet verified the owner skin, slim/wide body, outer layers, equipment pose and melee feel in an actual client session. Smoke testing proves startup/runtime compatibility, not visual completion.

## Verified gameplay checkpoint

- Gameplay-code checkpoint: `6d05cd589d25be3d8fe4953946e2c1831581af0d`
- Commit: `fix(augmentation-survival): serialize clone owner id`
- Avatar-sync/render commits in this batch: `1f9f72998411a49e87a7c66ae2a2dc1190390fb2`, `15dc778a0b7e6ce268b55950cbcc383b881a1035`, `6d05cd589d25be3d8fe4953946e2c1831581af0d`
- GitHub Actions: `40164608397` / Build Augmentation Survival Pack #102
- Build and pinned-pack inspection: **PASS**
- Dedicated server smoke: **PASS**
- Client boot smoke: **PASS**
- Verification artifact upload: **PASS**
- Artifact: `augmentation-survival-pack-smoke-102`
- Artifact ID: `12587886602`
- Artifact ZIP size: `979209` bytes
- Artifact ZIP SHA-256: `b64dcb4d814627f94c0f0e57305bacc983429c308871b950a822f600f93afb4d`

Build #101 failed at compile before runtime smoke because Minecraft 26.2 does not expose `EntityDataSerializers.OPTIONAL_UUID`. This was a code/API regression, not a workflow/environment issue. The first real failure was read from the job log and fixed by synchronizing the UUID as a validated string through the available `STRING` serializer. Build #102 then passed build, dedicated-server smoke and client-boot smoke; the failed run was not blindly repeated.

## Remaining implementation set

### Fixed-semantics no-runtime — 2

- `공간 연결`
- `권능 탈취`

These remain large systems and must not be reduced to temporary commands or generic fake boss powers.

### Partial — 1

- `분신`

The major owner-avatar presentation gap is now closed at runtime: owner identity is synchronized, actual skin lookup is available client-side, wide/slim player bodies are selected, player outer-layer toggles are snapshotted, and equipment/handedness presentation is retained. Remaining FULL gates are the full combat-power snapshot contract and actual visual/combat playtest; cape/slim-armor edge fidelity can be reviewed during that visual pass rather than being assumed from startup smoke.

### Explicitly underspecified / blocked — 8

Do not invent values for these until canon fixes the missing boundaries:

- `손재주`
- `물약 확산`
- `탄성 저장`
- `탄도 장막`
- `임시 발판`
- `생명 연결`
- `작아지기`
- `벽치기`

## Validation labels

- CODE REVIEWED: **YES**
- TESTED: **NO — workflow test task is `NO-SOURCE`; no dedicated automated gameplay test was executed**
- BUILD VERIFIED: **YES — `6d05cd589d25be3d8fe4953946e2c1831581af0d`**
- JAR PRODUCED: **YES**
- DEDICATED SERVER SMOKE: **PASS**
- CLIENT BOOT SMOKE: **PASS**
- LIVE GAMEPLAY TESTED: **NO**
- STATISTICAL PROBABILITY PLAYTESTED: **NO**
- MULTIPLAYER TESTED: **NO**

Documentation commits after the gameplay checkpoint are docs-only and must not be treated as a separately build-verified gameplay SHA.

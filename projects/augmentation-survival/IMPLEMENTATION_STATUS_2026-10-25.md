# Augmentation Survival — Implementation Status

Date: 2026-10-25
Supersedes: `IMPLEMENTATION_STATUS_2026-10-24.md`
Status: M1C FUNCTIONAL BASELINE / 135-OF-146 GAMEPLAY RUNTIME / CLONE PRESENTATION CHECKPOINT VERIFIED

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

This pass removes the fixed-Steve-only presentation limitation in two concrete ways:

- On activation, the clone snapshots the owner's main hand, off hand, head, chest, legs and feet equipment as copied stacks. The copied presentation gear has zero drop chance and the clone still cannot pick up loot, so it cannot become an item-duplication path.
- On the client, the renderer resolves the owner's current `PlayerSkin` from the connection/player-info skin cache and uses that body texture. Offline/unavailable profile resolution falls back deterministically to the UUID-derived default player skin rather than a single fixed Steve texture.
- Humanoid armor and held-item render layers are installed, so the snapshotted armor and hand items are visible on the clone.
- Owner handedness is snapshotted for the clone body.

`분신` remains **PARTIAL**, not FULL. The current body renderer still uses the wide player humanoid model and does not yet reproduce the full player-avatar presentation contract such as slim-arm model selection and all player outer-layer/customisation toggles. Its combat read also still needs actual gameplay inspection. No claim of visual completion is made from smoke testing alone.

## Verified gameplay checkpoint

- Gameplay-code checkpoint: `3dbfbf3076eb13e246b5c5554c7a32c50299a163`
- Commit: `fix(augmentation-survival): use explicit clone equipment slots`
- Presentation commits in this batch: `b80926f3c1c6e34501e6f0bbb021a4561bc99a00`, `edc477ba70a6d6680c59c2b66e9201e850d2f48e`, `3dbfbf3076eb13e246b5c5554c7a32c50299a163`
- GitHub Actions: `40163466675` / Build Augmentation Survival Pack #100
- Build and pinned-pack inspection: **PASS**
- Dedicated server smoke: **PASS**
- Client boot smoke: **PASS**
- Verification artifact upload: **PASS**
- Artifact: `augmentation-survival-pack-smoke-100`
- Artifact ID: `12587765316`
- Artifact ZIP size: `977422` bytes
- Artifact ZIP SHA-256: `9cd0309571560bdd52c4049997fd019cddfef841327c0be33fd883bf3a51179c`

Build #99 failed before runtime smoke because the first equipment-snapshot implementation called a nonexistent Minecraft 26.2 `EquipmentSlot.isForHumanoidArmor()` method. This was a code/API regression, not a workflow/environment issue. The fix replaced that assumption with an explicit six-slot presentation list. Build #100 then passed build, server smoke and client smoke; the failed run was not blindly rerun.

## Remaining implementation set

### Fixed-semantics no-runtime — 2

- `공간 연결`
- `권능 탈취`

These remain large systems and must not be reduced to temporary commands or generic fake boss powers.

### Partial — 1

- `분신`

Remaining presentation work is principally player-model fidelity (wide/slim and player-specific outer-layer state) plus actual combat/visual playtest. The functional server entity, AI, lifetime, health/damage snapshots, cooldown and reward-provenance boundaries already exist.

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
- BUILD VERIFIED: **YES — `3dbfbf3076eb13e246b5c5554c7a32c50299a163`**
- JAR PRODUCED: **YES**
- DEDICATED SERVER SMOKE: **PASS**
- CLIENT BOOT SMOKE: **PASS**
- LIVE GAMEPLAY TESTED: **NO**
- STATISTICAL PROBABILITY PLAYTESTED: **NO**
- MULTIPLAYER TESTED: **NO**

Documentation commits after the gameplay checkpoint are docs-only and must not be treated as a separately build-verified gameplay SHA.

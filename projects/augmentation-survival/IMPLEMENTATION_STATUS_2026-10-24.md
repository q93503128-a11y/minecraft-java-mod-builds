# Augmentation Survival — Implementation Status

Date: 2026-10-24
Status: M1C FUNCTIONAL BASELINE / 100-OF-146 GAMEPLAY RUNTIME / REACTIVE + MOBILITY + UTILITY BULK PASS VERIFIED

## 146-roster implementation count

The count below measures whether the canonical gameplay effect has a runtime implementation. It does **not** mean final VFX/UI polish or live playtesting is complete.

- Fully implemented gameplay runtime: **100 / 146**
- Partially implemented: **4 / 146** — `회복 증폭`, `전리품 챙기기`, `보물 낚시`, `풍요`
- Not fully implemented: **46 / 146**
- No gameplay runtime yet: **42 / 146**
- **This verified bulk pass adds 18 new gameplay runtime implementations.**
- Reactive/status/protection/death additions: `돌려보내기`, `상태 반사`, `강인한 정신`, `보호막 폭발`, `되밟기`, `무적`, `소지품 보존`
- Mobility additions: `물살 타기`, `기수`, `물 위 달리기`
- Utility/passive/active additions: `집중 조준`, `줍는 손`, `방패잡이`, `연료 절약`, `빠른 양조`, `경험 보존`, `위협 억제`, `순간 채굴`

`전리품 챙기기` and `풍요` remain PARTIAL because their remaining limitation is item-drop provenance / unique-progression exclusion. `회복 증폭` and `보물 낚시` retain their previously documented partial limitations.

## Verified code checkpoint

- Verified gameplay-code checkpoint: `898f0a595fbdb0ac7adeb344056d794cca5152af`
- Primary feature commits:
  - `6e948fe7f73ef36d6685d0577b8865dda2784654` — `feat(augmentation-survival): implement reactive combat and mobility augment batch`
  - `910990bb223f35ddbd1cae731fb2d85aa107f3da` — `feat(augmentation-survival): implement large passive and utility augment batch`
- Final runtime-fix checkpoint: `898f0a595fbdb0ac7adeb344056d794cca5152af` — canonical `무적` id binding after 26.2 mixin target corrections
- GitHub Actions: `40095699643` / Build Augmentation Survival Pack #62
- Build and pinned-pack inspection: PASS
- Pack dependency inspection: PASS — 23 pinned external artifacts
- Runtime classpath verification: PASS — 150 files
- Dedicated server smoke: PASS — 92 mods
- Client boot smoke: PASS — 108 mods
- Verification artifacts upload: PASS
- Artifact: `augmentation-survival-pack-smoke-62`
- Artifact ID: `12560243897`
- Artifact ZIP size: 629526 bytes
- Artifact ZIP SHA-256: `be1b882c5d5e4ec18a2d0505605ff1b1c3028fe1c8a12c9296fd8cb8d7c557a7`
- Canonical augment catalog initialization: PASS — 146 definitions
- Entity compatibility contract initialization: PASS — **8 exact selectors / 11 token selectors / 1 dynamic adapter**
- Enemy scaling contract initialization: PASS — 9 stages / 4 native exact profiles / 0 namespace profiles
- Stat contract initialization: PASS — 7 definitions / 7 modifier groups
- Survival contract initialization: PASS
- Resource-harvest contract initialization: PASS
- Consumption/fishing contract initialization: PASS
- Combat / expansion / movement / projectile / core contracts initialization: PASS
- Reactive combat / mobility utility / utility runtime contracts: BUILD + FULL-PACK BOOT VERIFIED

Full-pack server startup verified the new mixin/runtime boundaries together with the existing external-boss runtime. The final correction sequence fixed 26.2 method targets without deleting functionality: shield disable is intercepted at `Player.blockUsingItem(ServerLevel, LivingEntity, DamageSource, float)` and canonical `무적` state binds to augment id `invincibility`.

Known external/harness warnings remain non-fatal: optional Naturalist client-side mixin warnings on dedicated server, missing Linux narrator/OpenAL capabilities in CI, World Bosses asset warnings, tag/translation warnings and the pre-existing `ProjectileAugmentService` deprecated-API warning. None prevented #62 from succeeding.

Documentation commits after this checkpoint do not change Java gameplay code and must not be treated as new gameplay verification targets.

## Newly verified bulk runtime pass

### Reactive / status / protection / death

- `돌려보내기`: shield-blocked projectiles are re-owned by the player and redirected along look direction with level-scaled retained speed; reflected provenance is separated from ROOT projectile provenance.
- `상태 반사`: finite removable harmful effects directly applied by enemies are reflected with the canonical 50% duration without recursively re-reflecting.
- `강인한 정신`: removable harmful-effect duration reduction is applied at the shared status-application boundary; boss-origin effects remain conservatively excluded from this generic reduction path.
- `보호막 폭발`: source-owned augment shield/absorption pools report one terminal event on full depletion or expiry and emit capped derived AoE damage once per pool.
- `되밟기`: a finite two-second server position history is replayed backwards after a qualifying full-strength ROOT melee hit; each crossed hostile is damaged once per activation with a five-second cooldown.
- `무적`: canonical `invincibility` runtime cycles a three-second invulnerability window every ten seconds, blocks ordinary damage/new removable harmful effects and suppresses knockback while active; explicit invulnerability-bypass damage remains allowed.
- `소지품 보존`: actual-death equipment/inventory drop is cancelled and the old inventory is copied into the respawned player; XP remains a separate system.

### Mobility

- `물살 타기`: flowing-water direction and player movement are evaluated server-side and a temporary movement modifier is maintained only while the condition remains valid.
- `기수`: directly controlled ground animals receive source-owned movement-speed and jump-strength modifiers while ridden by the augment holder; modifiers are removed when ownership/control conditions end.
- `물 위 달리기`: a sprinting holder is allowed to stand on water through the vanilla fluid-standing boundary while the condition remains valid.

### Utility / passive / active

- `집중 조준`: one-second stationary full-draw state is tracked server-side and snapshotted onto the next ROOT bow arrow; its damage bonus joins the existing projectile direct-damage path.
- `줍는 손`: nearby item entities and XP orbs inside the level-scaled expanded radius reuse normal `playerTouch` pickup behavior.
- `방패잡이`: the vanilla 26.2 shield-disable duration argument is reduced at the actual `BlocksAttacks.disable(...)` call inside `Player.blockUsingItem(...)`.
- `연료 절약`: furnace burn duration is extended from the vanilla burn-duration boundary using the highest eligible nearby holder level.
- `빠른 양조`: a newly started 400-tick brew snapshots the highest eligible nearby holder level and replaces its duration once.
- `경험 보존`: death-orb reward and respawn XP restoration split the original XP so retained XP and dropped XP are not duplicated.
- `위협 억제`: the toggle blocks NATURAL non-boss hostile entity load within the canonical 24-block radius while leaving disk loads, event/spawner-style paths and bosses untouched.
- `순간 채굴`: the toggle only upgrades directly targeted, normally harvestable correct-tool root blocks to effectively one-tick destroy speed; normal drop/tool/enchantment paths remain vanilla-owned and derived extra blocks do not become new ROOT actions.

## Implemented foundations retained

### Runtime / progression / device

- persistent server-owned player augment state
- optional `persistent_ints` map in schema v2 for counters/toggles; older saves decode with an empty map
- persistent per-augment cooldown-ready ticks
- save-wide threat high-water state
- strict 146-entry canonical runtime catalog
- six core tiers and data-driven core acquisition/source profiles
- structure, hostile, raid, fishing and vanilla-boss core reward baselines
- basic/advanced augment workstations
- three-candidate server transaction
- `두 번째 선택` atomic two-pick
- 3:1 one/bulk core upgrade
- functional client card-selection UI baseline

### Stat / combat / protection / healing / lethal

- unconditional stat contract: 7 definitions / 7 modifier groups
- combat provenance and ROOT/derived recursion boundaries
- post-mitigation protection/delayed-damage pipeline
- augment-generated primary-healing pipeline
- lethal order: `한 번 버티기` -> vanilla held Totem -> `비상 토템` -> `재림`
- broad combat expansion includes `연쇄 폭발`, `벼락`, `메아리 타격`, `약점 표식`, `속도 전환`, `육중한 일격`, `생명 파동`, `난전`, `유리 대포`, `우두머리 사냥`

### Movement / projectile / active

- shared movement/aerial runtime including `숨 고르기`, `야행성`, `햇빛 체질`, `성큼걸음`, `멀리뛰기`, `관성 전환`, `두 번 점프`, `공중 연계`, `물갈퀴`, `발 안 묶이기`, `물살 타기`, `기수`, `물 위 달리기`
- shared projectile shot-action / flight-segment runtime for `화살 아끼기`, `관통 화살`, `연쇄 화살`, `분산 사격`, `추적 화살`, `도탄`, plus `집중 조준` snapshot integration
- common 10-slot active runtime with server-authoritative ownership/cooldown/charge/target/result validation
- supported cooldown actives: `정화`, `끌어당기기`, `자리 바꾸기`, `보호막`, `광폭화`, `치유 파동`
- supported toggle actives: `위협 억제`, `순간 채굴`
- active meta runtime: `능력 가속`, `이중 충전`, `재연`, `전투 순환`

### Survival / resource / harvest / consumption / utility

- `광부의 손`, `벌목 요령`, `야행성` mining facet, `연속 채굴`
- `물갈퀴`, `긴 팔`, `물속 작업`, `발 안 묶이기`
- `적게 먹기`, `숨 오래 참기`, `추위 적응`, `밤눈`
- `도구 관리`, `방어구 관리`, `자동 보충`
- `회복력`, `햇빛 체질` natural-regeneration facet
- `풍작`, `씨앗 챙기기`, `한 조각 더`, `다시 심기`, `싹쓸이 수확`, `나무 베기`, `굴파기`
- `든든한 한 끼`, `낚시 감각`, `빨리 먹기`, `쌍둥이`, `먹으면서 걷기`, `비축 식사`
- `줍는 손`, `방패잡이`, `연료 절약`, `빠른 양조`, `경험 보존`, `소지품 보존`
- `전리품 챙기기`, `보물 낚시`, `풍요` retain functional partial baselines

Automatic crop/tree/tunnel actions reuse the server block-destruction path rather than raw block deletion. Mining-speed bonuses sharing one stat axis are additive before one final scale. Core items are excluded from ordinary duplication effects.

## Save-wide hostile threat scaling runtime

`enemy_scaling_contract.json` remains the runtime-active data source for the 0–8 save-wide curve.

### Save-wide application

- player augment-level mutations recompute canonical power score and feed the existing high-water state;
- threat is one shared save value across dimensions and players and never automatically decreases;
- a stage rise reconciles currently loaded scalable entities once; no global per-tick hostile scan exists;
- newly loaded/spawned scalable entities reconcile on server entity load;
- max-health changes preserve current health ratio.

### Normal / elite / boss curves

Stage 8 normal hostile baseline remains:

- max health x12.00
- outgoing damage x2.70
- movement speed x1.175
- armor +12
- knockback resistance +0.20

Elite scaling uses 65% of the **extra** normal scaling. Native exact elite profiles are `minecraft:warden` and `minecraft:elder_guardian`.

Boss scaling uses 35% of normal extra health and the 30% midpoint of the canonical 25–35% extra damage band. Generic boss speed/armor/knockback scaling is disabled. Bosses persist an encounter-stage snapshot so a later save-wide stage increase does not suddenly rescale an active encounter. Native exact boss profiles are Wither and Ender Dragon; compatible external bosses enter the same boss group through the shared compatibility service.

Hostile outgoing damage is amplified once at the shared server pre-mitigation boundary. `DELAYED_DAMAGE` explicitly bypasses threat amplification because delayed installments already contain reserved damage.

## External boss compatibility runtime

`entity_compatibility_profiles.json` is the shared identity and control-policy contract. Boss identity is deliberately separate from core reward assignment: identifying an entity as a boss does **not** automatically award a boss core.

### Exact identities

Native / exact selectors:

- `minecraft:wither`
- `minecraft:ender_dragon`
- `chronodawn:time_guardian`
- `chronodawn:chronos_warden`
- `chronodawn:clockwork_colossus`
- `chronodawn:entropy_keeper`
- `chronodawn:temporal_phantom`
- `chronodawn:time_tyrant`

The six Chrono Dawn IDs were verified against Chrono Dawn's published integration/source material rather than inferred from display names.

### World Bosses runtime registry resolution

The pinned World Bosses 2.0.2 JAR is loaded as mod id `worldbosses`. Its 11 boss profiles use namespace + path-token selectors at contract parse time, then bind to one exact live registry ID at server start. The full-pack checkpoint resolved all of them successfully:

- `worldbosses:sylvaron`
- `worldbosses:aureon`
- `worldbosses:nekhraz`
- `worldbosses:varkhul`
- `worldbosses:morvath`
- `worldbosses:elyndra`
- `worldbosses:astrael`
- `worldbosses:shadow_king`
- `worldbosses:dragon_mother`
- `worldbosses:storm_griffin`
- `worldbosses:shadow_weaver`

A loaded required mod with a missing or ambiguous token selector fails the compatibility binding instead of silently guessing another entity.

### MCEndgame dynamic dungeon bosses

MCEndgame dungeon bosses are not a fixed entity-type list. The compatibility runtime binds the mod's own persisted boss marker through reflection:

`de.fuballer.mcendgame.main.accessor.MobEntityDungeonBossAccessor#mcendgame$isDungeonBoss`

The full-pack checkpoint confirmed that this accessor exists in the pinned MCEndgame runtime and bound it successfully. This lets a mob be classified as a boss only when MCEndgame itself marks that concrete instance as a dungeon boss.

### Shared consumers

`CombatExpansionService.isConfiguredBoss(...)` delegates to the compatibility service. Existing systems share the same boss identity for:

- `우두머리 사냥` direct boss damage bonus;
- `전투 순환` boss-kill cooldown reduction;
- `전리품 챙기기` normal-mob-only exclusion;
- `풍요` normal-mob ordinary-output exclusion;
- `끌어당기기` existing reduced boss pull scalar;
- save-wide threat boss curve + encounter-stage snapshot;
- `위협 억제` boss spawn exclusion;
- conservative `강인한 정신` generic boss-origin status exclusion.

The current compatibility contract also exposes future per-boss control fields (`allow_pull`, `allow_position_swap`, `allow_time_stop`, `allow_stun`, `allow_percent_hp_damage`, `allow_wall_impact`, `allow_dominate`). Current boss profiles preserve the existing pull/swap behavior, so this checkpoint does **not** claim that all future per-boss control restrictions are already enforced by every ability implementation.

## Partial augments still open

- `회복 증폭`: only routed augment-generated non-natural healing is guaranteed to pass amplification; every external non-natural heal is not yet intercepted.
- `전리품 챙기기`: external boss exclusion is materially stronger, but crowded simultaneous nearby-item provenance remains spatial rather than exact source-owned capture.
- `보물 낚시`: vanilla luck-equivalent adjustment is not mathematically identical to canonical +2 / +4 / +6 percentage points.
- `풍요`: external boss exclusion is improved, but exact final-output provenance and comprehensive unique/progression-item exclusions remain incomplete.

`손재주` remains unimplemented because its non-combat gathering whitelist is not fixed. `한 마리 더` remains a held/unconfirmed proposal and is not counted in the locked 146 roster.

## Deliberately not claimed complete

- Full roster gameplay completion is **100 / 146**, not 146 / 146.
- the 18-new-augment bulk pass is build/full-pack boot verified but **not live-player playtested**;
- reactive combat timing/feel, reflected projectile behavior after vanilla block resolution, water-running edge cases, mount feel, machine radius ownership, instant-mining tool/enchantment matrix, XP preservation and death inventory behavior still need live gameplay validation;
- external boss identity/binding is build/full-pack boot verified but actual boss encounters have not been live-playtested;
- World Bosses 11 IDs were proven by registry binding, but their threat-scaled HP/damage and active interactions have not been measured in a running player fight;
- Chrono Dawn six boss profiles have not been live encounter-tested with phase transitions;
- MCEndgame dynamic dungeon-boss classification has not been live-tested on an actually spawned dungeon boss;
- scripted damage with no living dealer may still require mod-specific damage-source adapters before threat damage scaling is guaranteed;
- threat stage advancement with actual loaded mobs has not been live-tested;
- Wither/Ender Dragon snapshot behavior has not been live-tested;
- no threat-stage HUD/presentation is implemented;
- Farmer's Delight harvest/food compatibility still needs live gameplay validation;
- active input remains a functional baseline; `Left Alt + 1..0` may also switch the vanilla hotbar until final input UX is implemented;
- `약점 표식` multiplayer ownership still needs a multi-owner-safe representation;
- projectile and movement shared runtimes remain build/boot verified, not live-playtested for feel and matrix interactions;
- final device/active/combat/resource/threat VFX, sound, models, HUD and UI visual polish are not done;
- compilation still reports the pre-existing `ProjectileAugmentService` deprecated-API warning; it is not a #62 failure and is not claimed resolved;
- multiplayer is not tested.

## Batching rule

Do not return to one-augment-at-a-time implementation except for isolated bugs or genuinely unique mechanics.

For every next batch:

1. recompute the 146-roster implementation inventory;
2. report `fully implemented / partial / not fully implemented / this batch` counts;
3. build or reuse one shared runtime boundary;
4. connect every canonical easy/medium augment that safely fits it;
5. validate once after the meaningful batch;
6. keep only genuinely unique physics/world/companion/UI mechanics for specialized systems.

## Next implementation direction

The reactive/status/mobility/utility bulk pass is no longer the next blocker. The next large roster pass must recompute the remaining **42 no-runtime augments** from the locked 146 roster and choose the densest shared subsystem. Prefer already-fixed semantics that can reuse current server-authoritative state, active routing, entity compatibility, combat provenance or world interaction boundaries. Do not implement still-underspecified mechanics by inventing missing values or behavior.

## Validation labels

- CODE REVIEWED: YES
- BUILD VERIFIED: YES — `898f0a595fbdb0ac7adeb344056d794cca5152af`
- JAR PRODUCED: YES — Actions run `40095699643`
- PACK DEPENDENCY INSPECTION: PASS — 23 pinned external artifacts
- PACK RUNTIME CLASSPATH: PASS — 150 files
- PACK SERVER SMOKE: PASS — 92 mods
- PACK CLIENT SMOKE: PASS — 108 mods
- VERIFICATION ARTIFACT: `augmentation-survival-pack-smoke-62` / ID `12560243897`
- VERIFICATION ARTIFACT SHA-256: `be1b882c5d5e4ec18a2d0505605ff1b1c3028fe1c8a12c9296fd8cb8d7c557a7`
- CANONICAL AUGMENT CATALOG INITIALIZATION: PASS — 146 definitions
- ENTITY COMPATIBILITY CONTRACT INITIALIZATION: PASS — 8 exact / 11 token / 1 dynamic
- WORLD BOSSES LIVE REGISTRY PROFILE BINDING: PASS — 11 / 11
- MCENDGAME DYNAMIC BOSS ADAPTER BINDING: PASS — 1 / 1
- CHRONO DAWN EXACT BOSS PROFILE DEFINITIONS: PASS — 6
- ENEMY SCALING CONTRACT INITIALIZATION: PASS — 9 stages / 4 native exact profiles / 0 namespace profiles
- SAVE-WIDE THREAT HOSTILE RUNTIME: BUILD/BOOT VERIFIED
- STAT CONTRACT INITIALIZATION: PASS — 7 definitions / 7 modifier groups
- SURVIVAL CONTRACT INITIALIZATION: PASS
- RESOURCE-HARVEST CONTRACT INITIALIZATION: PASS
- CONSUMPTION/FISHING CONTRACT INITIALIZATION: PASS
- COMBAT/PROTECTION/HEAL/DEATH CONTRACT INITIALIZATION: PASS
- EXPANSION CONTRACT INITIALIZATION: PASS
- MOVEMENT CONTRACT INITIALIZATION: PASS
- PROJECTILE CONTRACT INITIALIZATION: PASS
- REACTIVE COMBAT RUNTIME: BUILD/BOOT VERIFIED
- MOBILITY UTILITY RUNTIME: BUILD/BOOT VERIFIED
- UTILITY/PASSIVE/TOGGLE RUNTIME: BUILD/BOOT VERIFIED
- EXTERNAL BOSS LIVE ENCOUNTER TESTED: NO
- THREAT LIVE-PLAYER TESTED: NO
- THREAT STAGE-RISE LOADED-MOB REFRESH LIVE TESTED: NO
- BOSS THREAT SNAPSHOT LIVE TESTED: NO
- NEW 18-AUGMENT BULK PASS LIVE-PLAYER TESTED: NO
- CONSUMPTION/FISHING/BREEDING LIVE-PLAYER TESTED: NO
- ABUNDANCE DROP MATRIX LIVE TESTED: NO
- RESOURCE-HARVEST LIVE-PLAYER TESTED: NO
- ACTIVE ENGINE LIVE-PLAYER TESTED: NO
- BROAD COMBAT EXPANSION LIVE TESTED: NO
- PROJECTILE LIVE-PLAYER TESTED: NO
- MOVEMENT LIVE-PLAYER TESTED: NO
- PROTECTION PIPELINE LIVE-PLAYER TESTED: NO
- HEALING PIPELINE LIVE-PLAYER TESTED: NO
- LETHAL CHAIN LIVE-PLAYER TESTED: NO
- STAT AUGMENT LIVE-PLAYER TESTED: NO
- DEVICE FLOW PLAYTESTED: NO
- UI VISUAL REVIEW: NO
- BETTER COMBAT GAMEPLAY TESTED: NO
- MULTIPLAYER TESTED: NO

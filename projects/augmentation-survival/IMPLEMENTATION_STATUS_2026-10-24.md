# Augmentation Survival — Implementation Status

Date: 2026-10-24
Status: M1C FUNCTIONAL BASELINE / SHARED GAMEPLAY RUNTIMES + SAVE-WIDE THREAT + EXTERNAL BOSS COMPATIBILITY IMPLEMENTED / REMAINING ROSTER BULK NEXT

## 146-roster implementation count

The count below measures whether the canonical gameplay effect has a runtime implementation. It does **not** mean final VFX/UI polish or live playtesting is complete.

- Fully implemented gameplay runtime: **82 / 146**
- Partially implemented: **4 / 146** — `회복 증폭`, `전리품 챙기기`, `보물 낚시`, `풍요`
- Not fully implemented: **64 / 146**
- No gameplay runtime yet: **60 / 146**
- **This external-boss compatibility batch adds 0 new augment implementations.**
- Existing augments with materially improved boss integration in this batch: **5** — `우두머리 사냥`, `전투 순환`, `전리품 챙기기`, `풍요`, `끌어당기기`
- System integration improved in this batch: save-wide threat boss classification/snapshot.

`전리품 챙기기` and `풍요` remain PARTIAL because their remaining limitation is item-drop provenance / unique-progression exclusion, not external boss identity alone.

## Verified code checkpoint

- Verified gameplay-code checkpoint: `448fa7413871b55b7642f6bdfa53db77af482be1`
- Feature commit: `448fa7413871b55b7642f6bdfa53db77af482be1` — `feat(augmentation-survival): unify external boss compatibility profiles`
- GitHub Actions: `40086538371` / Build Augmentation Survival Pack #53
- Build and pinned-pack inspection: PASS
- Pack dependency inspection: PASS — 23 pinned external artifacts
- Runtime classpath verification: PASS — 150 files
- Dedicated server smoke: PASS — 92 mods
- Client boot smoke: PASS — 108 mods
- Verification artifacts upload: PASS
- Artifact: `augmentation-survival-pack-smoke-53`
- Artifact ID: `12557656344`
- Artifact ZIP size: 549104 bytes
- Artifact ZIP SHA-256: `8f9be0219e68f36739573f87eb9db8f3716fbca4f974a87785c7b0709d6ba3f9`
- Canonical augment catalog initialization: PASS — 146 definitions
- Entity compatibility contract initialization: PASS — **8 exact selectors / 11 token selectors / 1 dynamic adapter**
- Enemy scaling contract initialization: PASS — 9 stages / 4 native exact profiles / 0 namespace profiles
- Stat contract initialization: PASS — 7 definitions / 7 modifier groups
- Survival contract initialization: PASS
- Resource-harvest contract initialization: PASS
- Consumption/fishing contract initialization: PASS
- Combat / expansion / movement / projectile / core contracts initialization: PASS

Full-pack server startup additionally verified the runtime binding phase rather than only parsing the JSON contract:

- all 11 pinned World Bosses 2.0.2 entity IDs resolved uniquely from the live registry;
- the MCEndgame 1.7.0+26.2 dungeon-boss accessor was found and bound;
- Chrono Dawn 0.12.0, World Bosses 2.0.2 and MCEndgame 1.7.0+26.2 co-loaded with the compatibility runtime;
- server reached ready state and client stayed healthy through the smoke timeout.

Known external/harness warnings remain non-fatal: optional Naturalist FieldGuide mixins, missing Linux narrator `flite`, no CI OpenAL device, World Bosses asset warnings, initial missing smoke `server.properties`, tag/translation warnings and the pre-existing `ProjectileAugmentService` deprecated-API warning. None prevented #53 from succeeding.

Documentation commits after this checkpoint do not change Java gameplay code and must not be treated as new gameplay verification targets.

## Implemented foundations retained

### Runtime / progression / device

- persistent server-owned player augment state
- optional `persistent_ints` map in schema v2 for small counters such as `비축 식사`; older saves decode with an empty map
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

- shared movement/aerial runtime including `숨 고르기`, `야행성`, `햇빛 체질`, `성큼걸음`, `멀리뛰기`, `관성 전환`, `두 번 점프`, `공중 연계`, `물갈퀴`, `발 안 묶이기`
- shared projectile shot-action / flight-segment runtime for `화살 아끼기`, `관통 화살`, `연쇄 화살`, `분산 사격`, `추적 화살`, `도탄`
- common 10-slot active runtime with server-authoritative ownership/cooldown/charge/target/result validation
- supported actives: `정화`, `끌어당기기`, `자리 바꾸기`, `보호막`, `광폭화`, `치유 파동`
- active meta runtime: `능력 가속`, `이중 충전`, `재연`, `전투 순환`

### Survival / resource / harvest / consumption

- `광부의 손`, `벌목 요령`, `야행성` mining facet, `연속 채굴`
- `물갈퀴`, `긴 팔`, `물속 작업`, `발 안 묶이기`
- `적게 먹기`, `숨 오래 참기`, `추위 적응`, `밤눈`
- `도구 관리`, `방어구 관리`, `자동 보충`
- `회복력`, `햇빛 체질` natural-regeneration facet
- `풍작`, `씨앗 챙기기`, `한 조각 더`, `다시 심기`, `싹쓸이 수확`, `나무 베기`, `굴파기`
- `든든한 한 끼`, `낚시 감각`, `빨리 먹기`, `쌍둥이`, `먹으면서 걷기`, `비축 식사`
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

Boss scaling uses 35% of normal extra health and the 30% midpoint of the canonical 25–35% extra damage band. Generic boss speed/armor/knockback scaling is disabled. Bosses persist an encounter-stage snapshot so a later save-wide stage increase does not suddenly rescale an active encounter. Native exact boss profiles are Wither and Ender Dragon; compatible external bosses now enter the same boss group through the shared compatibility service.

Hostile outgoing damage is amplified once at the shared server pre-mitigation boundary. `DELAYED_DAMAGE` explicitly bypasses threat amplification because delayed installments already contain reserved damage.

## External boss compatibility runtime — new #53 checkpoint

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

The pinned World Bosses 2.0.2 JAR is loaded as mod id `worldbosses`. Its 11 boss profiles use namespace + path-token selectors at contract parse time, then bind to one exact live registry ID at server start. #53 resolved all of them successfully:

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

MCEndgame dungeon bosses are not a fixed entity-type list. The compatibility runtime therefore binds the mod's own persisted boss marker through reflection:

`de.fuballer.mcendgame.main.accessor.MobEntityDungeonBossAccessor#mcendgame$isDungeonBoss`

#53 confirmed that this accessor exists in the pinned MCEndgame runtime and bound it successfully. This lets a mob be classified as a boss only when MCEndgame itself marks that concrete instance as a dungeon boss.

### Shared consumers

`CombatExpansionService.isConfiguredBoss(...)` now delegates to the compatibility service. Existing systems therefore share the same boss identity for:

- `우두머리 사냥` direct boss damage bonus;
- `전투 순환` boss-kill cooldown reduction;
- `전리품 챙기기` normal-mob-only exclusion;
- `풍요` normal-mob ordinary-output exclusion;
- `끌어당기기` existing reduced boss pull scalar;
- save-wide threat boss curve + encounter-stage snapshot.

The current compatibility contract also exposes future per-boss control fields (`allow_pull`, `allow_position_swap`, `allow_time_stop`, `allow_stun`, `allow_percent_hp_damage`, `allow_wall_impact`, `allow_dominate`). Current boss profiles preserve the existing pull/swap behavior, so #53 does **not** claim that all future per-boss control restrictions are already enforced by every ability implementation. In particular, the contract's `pull_multiplier` is available at the compatibility boundary while the current active implementation still uses the existing shared boss pull scalar.

## Partial augments still open

- `회복 증폭`: only routed augment-generated non-natural healing is guaranteed to pass amplification; every external non-natural heal is not yet intercepted.
- `전리품 챙기기`: external boss exclusion is now materially stronger, but crowded simultaneous nearby-item provenance remains spatial rather than exact source-owned capture.
- `보물 낚시`: vanilla luck-equivalent adjustment is not mathematically identical to canonical +2 / +4 / +6 percentage points.
- `풍요`: external boss exclusion is improved, but exact final-output provenance and comprehensive unique/progression-item exclusions remain incomplete.

`손재주` remains unimplemented because its non-combat gathering whitelist is not fixed. `한 마리 더` remains a held/unconfirmed proposal and is not counted in the locked 146 roster.

## Deliberately not claimed complete

- Full roster gameplay completion is **82 / 146**, not 146 / 146.
- external boss identity/binding is build/full-pack boot verified but **actual boss encounters have not been live-playtested**;
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
- compilation still reports the pre-existing `ProjectileAugmentService` deprecated-API warning; it is not a #53 failure and is not claimed resolved;
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

The external-boss identity gate is no longer the next blocker. The next large roster pass should return to the remaining 60 augments with no gameplay runtime and choose the densest shared subsystem, prioritizing **reactive/status/protection combat effects that can reuse the existing combat/protection/healing/provenance boundaries** before creating bespoke entity or collision-heavy engines.

Candidate selection must be recomputed from the locked 146 roster at the start of the next batch. Do not implement underspecified mechanics by inventing missing semantics.

## Validation labels

- CODE REVIEWED: YES
- BUILD VERIFIED: YES — `448fa7413871b55b7642f6bdfa53db77af482be1`
- JAR PRODUCED: YES — Actions run `40086538371`
- PACK DEPENDENCY INSPECTION: PASS — 23 pinned external artifacts
- PACK RUNTIME CLASSPATH: PASS — 150 files
- PACK SERVER SMOKE: PASS — 92 mods
- PACK CLIENT SMOKE: PASS — 108 mods
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
- EXTERNAL BOSS LIVE ENCOUNTER TESTED: NO
- THREAT LIVE-PLAYER TESTED: NO
- THREAT STAGE-RISE LOADED-MOB REFRESH LIVE TESTED: NO
- BOSS THREAT SNAPSHOT LIVE TESTED: NO
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

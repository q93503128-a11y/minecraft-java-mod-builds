# Augmentation Survival — Implementation Status

Date: 2026-10-24
Status: M1C FUNCTIONAL BASELINE / 110-OF-146 GAMEPLAY RUNTIME / SPECIAL TEMPORAL + MYTHIC ACTIVE PASS VERIFIED

## 146-roster implementation count

The count below measures whether the canonical gameplay effect has a runtime implementation. It does **not** mean final VFX/UI polish or live playtesting is complete.

- Fully implemented gameplay runtime: **110 / 146**
- Partially implemented: **4 / 146** — `회복 증폭`, `전리품 챙기기`, `보물 낚시`, `풍요`
- Not fully implemented: **36 / 146**
- No gameplay runtime yet: **32 / 146**
- **This verified batch adds 3 new fully implemented gameplay runtimes.**
- Newly implemented: `귀환`, `시간 정지`, `천벌`
- Partially implemented this batch: **0**
- Existing implementation reclassified complete after source-consumer audit: **1** — `피해 적응`
- Shared systems materially extended: special active routing, concentration cancellation, hostile control state, projectile freeze state, status-timer freeze, boss control-policy enforcement, and `SPECIAL_PERCENT_HP` damage.

The previous 106-runtime checkpoint understated the implementation count by one: `피해 적응` was already fully consumed by `ProtectionAugmentService` and `CombatTuning` before this batch. It records the actual non-delayed incoming damage family, keeps the canonical six-second adaptation window and applies the level-scaled 10% / 20% / 30% reduction before delayed-damage/protection-pool handling. It is therefore moved from the stale no-runtime inventory into the fully implemented count without claiming new code for it.

`회복 증폭`, `전리품 챙기기`, `보물 낚시`, `풍요` retain their previously documented partial limitations.

## Verified code checkpoint

- Verified gameplay-code checkpoint: `37050798f139981d10f38dc8932f911a4a3db385`
- Primary feature commit: `37050798f139981d10f38dc8932f911a4a3db385` — `feat(augmentation-survival): implement special temporal and mythic actives`
- GitHub Actions: `40101411961` / Build Augmentation Survival Pack #66
- Build and pinned-pack inspection: PASS
- Pack dependency inspection: PASS — 23 pinned external artifacts
- Runtime classpath verification: PASS — 150 files
- Dedicated server smoke: PASS — 92 mods
- Client boot smoke: PASS — 108 mods
- Verification artifacts upload: PASS
- Artifact: `augmentation-survival-pack-smoke-66`
- Artifact ID: `12563216792`
- Artifact ZIP size: 681410 bytes
- Artifact ZIP SHA-256: `c8ea2ab5dbbc7bf139db8fa3798fdb40439c3302dfedc4787fd64c6f1c3427d9`
- Canonical augment catalog initialization: PASS — 146 definitions
- Entity compatibility contract initialization: PASS — 8 exact selectors / 11 token selectors / 1 dynamic adapter
- Enemy scaling contract initialization: PASS — 9 stages / 4 native exact profiles / 0 namespace profiles
- Stat / survival / harvest / consumption / combat / expansion / movement / projectile / reactive / utility contracts: BUILD + FULL-PACK BOOT VERIFIED
- Advanced utility contract: BUILD + FULL-PACK BOOT VERIFIED
- Special active contract and custom damage type: BUILD + FULL-PACK BOOT VERIFIED

Build #66 compiled the new server APIs, loaded both new Mixin targets, loaded the custom `augmentation_survival:divine_punishment` damage type and its armor-bypass tag, reached dedicated-server ready state with the full pack and completed the client smoke test. No corrective gameplay commit was required after `37050798f139981d10f38dc8932f911a4a3db385`.

Documentation commits after this checkpoint do not change Java gameplay code and must not be treated as new gameplay verification targets.

## Newly verified special temporal / mythic active pass

### `귀환`

The server starts a five-second concentration channel only when the player owns a valid personal respawn point. The channel records the starting dimension and exact position. Movement, direct attack, block-breaking input, item use, block-item use or actual unblocked damage cancels concentration. At completion the respawn target is revalidated and the server performs the teleport. The ability has no cooldown, so it remains outside `능력 가속` / `이중 충전` / `전투 순환` cooldown handling and is not replayed.

### `시간 정지`

Activation uses the canonical 32-block spherical range, nine-second duration and sixty-second cooldown. Only hostile living entities/configured bosses and hostile owned projectiles are selected; the player and allies continue normally. Living targets have movement suppressed and Mob AI temporarily disabled while their original NoAI state is preserved. Common `Projectile` instances preserve their pre-stop velocity/gravity and resume them after the stop. Active `MobEffectInstance` server ticks are paused while the target is time-stopped, so status/stun timers do not elapse during the stop. External boss targets obey `EntityCompatibilityProfileService.allowTimeStop(...)` rather than per-boss hardcoding. As a mythic active it deliberately does not use ability haste, double charge or replay.

### `천벌`

Activation uses the canonical 32-block radius and sixty-second cooldown. It applies maximum-health damage through the normal `hurtServer` path under `SPECIAL_PERCENT_HP` provenance rather than directly setting HP. Normal enemies take 60% / 90% max-HP damage and bosses take 10% / 25%; level 2 adds a two-second stun where the compatibility profile allows it. The authored damage type is tagged to bypass armor while still respecting normal hurt rejection such as invulnerability/phase handling. Boss percent-HP damage and stun independently obey `allowPercentHpDamage` and `allowStun`. The effect does not inherit ordinary attack-damage amplification or life-link sharing and does not modify blocks or apply fire.

## Prior verified advanced utility / permanent progression pass retained

- `방어 무시`: ROOT direct attacks reduce the armor/armor-toughness inputs used by vanilla armor absorption by 10% / 20% / 30%.
- `사냥 기록`: first qualifying ROOT-direct kill per hostile registry type is stored permanently, capped at 30 stacks, each giving +1 max HP and +0.5% ROOT direct damage.
- `나무타기`: pressing into adjacent log/leaf blocks enables level-scaled climbing based on ladder ascent speed.
- `진주 숙련`: ender-pearl self-damage -50% / -75% / -100%; pearl cooldown -15% / -30% / -45% at the 26.2 `UseCooldown` boundary.
- `휴대 수류`: a dry Riptide use may consume an offhand water bucket only after a successful portable release; natural wet use and creative use do not consume it.
- `사망 지점 귀환`: an actual death grants one use that teleports to the validated last-death location; lethal-prevention paths do not grant a charge.

## Implemented foundations retained

### Runtime / progression / device

- persistent server-owned player augment state with schema-v2 optional `persistent_ints`
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

### Combat / protection / healing / lethal

- unconditional stat contract: 7 definitions / 7 modifier groups
- combat provenance with ROOT/derived/reflected/path/special-percent-HP boundaries
- post-mitigation `피해 적응` / delayed-damage / protection-pool pipeline
- augment-generated primary-healing pipeline
- lethal order: `한 번 버티기` -> vanilla held Totem -> `비상 토템` -> `재림`
- broad combat expansion including `연쇄 폭발`, `벼락`, `메아리 타격`, `약점 표식`, `속도 전환`, `육중한 일격`, `생명 파동`, `난전`, `유리 대포`, `우두머리 사냥`
- reactive/status runtime including `돌려보내기`, `상태 반사`, `강인한 정신`, `보호막 폭발`, `되밟기`, `무적`
- advanced combat bridge includes `방어 무시` and `사냥 기록`

### Movement / projectile / active

- shared movement/aerial runtime including `숨 고르기`, `야행성`, `햇빛 체질`, `성큼걸음`, `멀리뛰기`, `관성 전환`, `두 번 점프`, `공중 연계`, `물갈퀴`, `발 안 묶이기`, `물살 타기`, `기수`, `물 위 달리기`, `나무타기`
- shared projectile shot-action / flight-segment runtime for `화살 아끼기`, `관통 화살`, `연쇄 화살`, `분산 사격`, `추적 화살`, `도탄`, plus `집중 조준`
- common 10-slot active runtime with server-authoritative ownership/cooldown/charge/target/result validation
- supported ordinary cooldown actives: `정화`, `끌어당기기`, `자리 바꾸기`, `보호막`, `광폭화`, `치유 파동`
- supported toggles: `위협 억제`, `순간 채굴`
- one-use death active: `사망 지점 귀환`
- no-cooldown concentration active: `귀환`
- mythic standalone actives: `시간 정지`, `천벌`
- active meta runtime: `능력 가속`, `이중 충전`, `재연`, `전투 순환`

### Survival / resource / harvest / consumption / utility

- `광부의 손`, `벌목 요령`, `연속 채굴`, `도구 관리`, `방어구 관리`, `자동 보충`
- `적게 먹기`, `숨 오래 참기`, `추위 적응`, `밤눈`, `회복력`
- `풍작`, `씨앗 챙기기`, `한 조각 더`, `다시 심기`, `싹쓸이 수확`, `나무 베기`, `굴파기`
- `든든한 한 끼`, `낚시 감각`, `빨리 먹기`, `쌍둥이`, `먹으면서 걷기`, `비축 식사`
- `줍는 손`, `방패잡이`, `연료 절약`, `빠른 양조`, `경험 보존`, `소지품 보존`
- item/enchantment utility includes `진주 숙련`, `휴대 수류`
- `전리품 챙기기`, `보물 낚시`, `풍요` retain functional partial baselines

## Save-wide hostile threat scaling and external boss compatibility

The existing save-wide 0–8 threat runtime remains active. Stage rises reconcile loaded scalable entities once; newly loaded/spawned hostiles receive the current stage; max-health changes preserve health ratio. Bosses use encounter-stage snapshots. Hostile outgoing damage is amplified once at the shared server boundary and delayed-damage installments do not receive a second multiplier.

`entity_compatibility_profiles.json` remains the shared boss identity/control-policy contract. Native exact identities include Wither, Ender Dragon and six Chrono Dawn bosses. Pinned World Bosses resolves 11 token profiles to exact live registry IDs. MCEndgame uses its dynamic dungeon-boss accessor. Boss identity remains separate from boss-core reward assignment.

Existing consumers now include boss threat scaling, `우두머리 사냥`, `전투 순환`, normal-mob-only loot exclusions, boss pull handling, `위협 억제`, generic boss-origin status handling, `시간 정지` and `천벌` control/percent-HP policy.

## Partial augments still open

- `회복 증폭`: every external non-natural heal is not yet intercepted.
- `전리품 챙기기`: crowded simultaneous nearby-item provenance remains spatial rather than exact source-owned capture.
- `보물 낚시`: vanilla luck-equivalent adjustment is not mathematically identical to canonical +2 / +4 / +6 percentage points.
- `풍요`: exact final-output provenance and comprehensive unique/progression-item exclusions remain incomplete.

`손재주` remains unimplemented because its non-combat gathering whitelist is not fixed. `물약 확산` remains blocked because the canonical text says “주변” without a fixed radius. `탄성 저장` still lacks a canonical block-distance-to-propulsion mapping. Other still-underspecified mechanics such as `탄도 장막`, `임시 발판`, `생명 연결`, `작아지기`, `벽치기` must not receive invented values.

`되감기` is also deliberately not claimed complete: its own 60-second active cooldown is specified, but as an eligible non-mythic cooldown active it must participate correctly in `능력 가속`, sequential `이중 충전` and `전투 순환`. Implementing it as an isolated one-cooldown button would violate the interaction contract.

## Deliberately not claimed complete

- Full roster gameplay completion is **110 / 146**, not 146 / 146.
- The new three-augment pass is build/full-pack boot verified but **not live-player playtested**.
- `귀환` needs actual movement/attack/use/damage interruption and cross-dimension respawn-point validation in a player session.
- `시간 정지` needs live validation of AI restoration, effect-timer pausing, overlapping stun interactions and hostile projectile resume behavior. Exotic external projectiles that do not subclass the common Minecraft `Projectile` path are not yet proven compatible.
- `천벌` needs live boss-phase/invulnerability validation and real external-boss encounters for percent-HP/stun policy.
- External boss encounters, threat stage progression, Farmer's Delight compatibility and broad combat/resource matrices remain live-test pending.
- Final device/active/combat/resource/threat VFX, sound, models, HUD and UI visual polish are not complete.
- The pre-existing `ProjectileAugmentService` deprecated-API warning remains non-fatal.
- Multiplayer is not tested.

## Batching rule

Do not return to one-augment-at-a-time implementation except for isolated bugs or genuinely unique mechanics.

For every next batch:

1. recompute the locked 146-roster implementation inventory from actual runtime consumers, not only stale docs;
2. report full / partial / not-fully / no-runtime counts;
3. reuse or create one meaningful shared runtime boundary;
4. connect every canonically fixed easy/medium augment that safely fits it;
5. validate once after the meaningful batch;
6. leave only genuinely unique or underspecified mechanics for specialized systems.

## Next implementation direction

The corrected remaining no-runtime set is now **32**. Recompute those 32 from the locked roster and actual runtime consumers before coding. Prefer the densest fixed-semantics subsystem that can remain server-authoritative. A strong next target is the non-mythic active-duration/cooldown family (`연장전`, `되감기` and any other eligible active that can correctly share the existing charge/cooldown engine), or a world/interact subsystem with several fixed entries. Do not fill missing parameters by inference.

## Validation labels

- CODE REVIEWED: YES
- BUILD VERIFIED: YES — `37050798f139981d10f38dc8932f911a4a3db385`
- JAR PRODUCED: YES — Actions run `40101411961`
- PACK DEPENDENCY INSPECTION: PASS — 23 pinned external artifacts
- PACK RUNTIME CLASSPATH: PASS — 150 files
- PACK SERVER SMOKE: PASS — 92 mods
- PACK CLIENT SMOKE: PASS — 108 mods
- VERIFICATION ARTIFACT: `augmentation-survival-pack-smoke-66` / ID `12563216792`
- VERIFICATION ARTIFACT SHA-256: `c8ea2ab5dbbc7bf139db8fa3798fdb40439c3302dfedc4787fd64c6f1c3427d9`
- CANONICAL AUGMENT CATALOG INITIALIZATION: PASS — 146 definitions
- ENTITY COMPATIBILITY CONTRACT INITIALIZATION: PASS — 8 exact / 11 token / 1 dynamic
- WORLD BOSSES LIVE REGISTRY PROFILE BINDING: PASS — 11 / 11
- MCENDGAME DYNAMIC BOSS ADAPTER BINDING: PASS — 1 / 1
- CHRONO DAWN EXACT BOSS PROFILE DEFINITIONS: PASS — 6
- ENEMY SCALING CONTRACT INITIALIZATION: PASS — 9 stages / 4 native exact profiles / 0 namespace profiles
- SPECIAL TEMPORAL / MYTHIC ACTIVE RUNTIME: BUILD/BOOT VERIFIED
- NEW 3-AUGMENT BATCH LIVE-PLAYER TESTED: NO
- RETURN-HOME LIVE-PLAYER TESTED: NO
- TIME-STOP LIVE-PLAYER TESTED: NO
- DIVINE-PUNISHMENT LIVE-PLAYER TESTED: NO
- EXTERNAL BOSS LIVE ENCOUNTER TESTED: NO
- THREAT LIVE-PLAYER TESTED: NO
- ACTIVE ENGINE LIVE-PLAYER TESTED: NO
- PROJECTILE LIVE-PLAYER TESTED: NO
- MOVEMENT LIVE-PLAYER TESTED: NO
- PROTECTION PIPELINE LIVE-PLAYER TESTED: NO
- HEALING PIPELINE LIVE-PLAYER TESTED: NO
- DEVICE FLOW PLAYTESTED: NO
- UI VISUAL REVIEW: NO
- BETTER COMBAT GAMEPLAY TESTED: NO
- MULTIPLAYER TESTED: NO

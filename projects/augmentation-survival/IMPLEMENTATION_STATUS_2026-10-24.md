# Augmentation Survival — Implementation Status

Date: 2026-10-24
Status: M1C FUNCTIONAL BASELINE / 119-OF-146 GAMEPLAY RUNTIME / ADVANCED WORLD CONTROL PASS VERIFIED

## 146-roster implementation count

The count below measures whether the canonical gameplay effect has a runtime implementation. It does **not** mean final VFX/UI polish or live playtesting is complete.

- Fully implemented gameplay runtime: **119 / 146**
- Partially implemented: **5 / 146** — `회복 증폭`, `전리품 챙기기`, `보물 낚시`, `풍요`, `지배`
- Not fully implemented: **27 / 146**
- No gameplay runtime yet: **22 / 146**
- **This verified batch adds 3 new fully implemented gameplay runtimes and 1 new partial runtime.**
- Newly fully implemented: `위상 이동`, `거대화`, `금단의 마법`
- Newly partial: `지배`
- Shared systems materially extended: advanced world-active tuning, server/client phase-state prediction, collision/action-lock handling, player-scale/health/reach/damage transforms, mythic fear navigation, persistent dominated-entity ownership, save-wide domination occupancy, allied/friendly-fire handling, and anvil exclusivity/prior-work/Too-Expensive overrides.

The earlier 106-runtime checkpoint understated the implementation count by one: `피해 적응` was already fully consumed by `ProtectionAugmentService` and `CombatTuning`. That correction remains part of the total; it was a source-consumer reclassification, not new gameplay code.

## Verified code checkpoint

- Verified gameplay-code checkpoint: `3ebeb3000a618fec998c8bd2cad4a1c05fbbcdd7`
- Primary feature commit: `58eec9edc8ebb5084db113a30a682c2c1cce6157` — `feat(augmentation-survival): implement advanced world control augments`
- Minecraft 26.2 interaction-signature fix: `2d34bfdcb25fe1f7b39ddeeb1e2e3ad67809a2de`
- Server-smoke false-negative workflow fix: `1a83e935aecf68a330d97c380c3f8873045c594d`
- Final semantic/performance fix: `3ebeb3000a618fec998c8bd2cad4a1c05fbbcdd7` — `fix(augmentation-survival): complete phase prediction and domination tracking`
- GitHub Actions: `40145505210` / Build Augmentation Survival Pack #72
- Build and pinned-pack inspection: PASS
- Pack dependency inspection: PASS — 23 pinned external artifacts
- Runtime classpath verification: PASS — 150 files
- Dedicated server smoke: PASS — 92 mods
- Client boot smoke: PASS — 108 mods
- Verification artifacts upload: PASS
- Artifact: `augmentation-survival-pack-smoke-72`
- Artifact ID: `12580571756`
- Artifact ZIP size: 798249 bytes
- Artifact ZIP SHA-256: `fe3e2ed9e9120d7cf2bfc2b822f0d2ddaac0d1b9b04f905d4aae34daad164b58`
- Canonical augment catalog initialization: PASS — 146 definitions
- Entity compatibility contract initialization: PASS — 8 exact selectors / 11 token selectors / 1 dynamic adapter
- Enemy scaling contract initialization: PASS — 9 stages / 4 native exact profiles / 0 namespace profiles
- Stat / survival / harvest / consumption / combat / expansion / movement / projectile / reactive / utility contracts: BUILD + FULL-PACK BOOT VERIFIED
- Advanced utility contract: BUILD + FULL-PACK BOOT VERIFIED
- Special active contract and custom damage type: BUILD + FULL-PACK BOOT VERIFIED
- Advanced active contract: BUILD + FULL-PACK BOOT VERIFIED
- Advanced world/control contract: BUILD + FULL-PACK BOOT VERIFIED

Build #69 found the first real 26.2 runtime error: `Player.interactOn` now includes a `Vec3` hit-location argument, so the phase action-lock mixin descriptor was corrected. Build #70 then booted the 92-mod server successfully and logged the ready marker, but the workflow falsely failed because the post-timeout check searched only `latest.log`; the ready marker remained in the captured console log. The workflow was corrected to inspect both logs. Build #71 fully passed the corrected workflow. The final semantic pass then added local phase collision prediction and event-tracked dominated companions, and Build #72 fully passed build, server and client smoke.

Documentation commits after this checkpoint do not change Java gameplay code and must not be treated as new gameplay verification targets.

## Newly verified advanced world / control pass

### `위상 이동` — FULL runtime

Activation creates a five-second phase window. Horizontal and upward solid collision are bypassed while downward collision remains active, so the player cannot fall through floors. Attack, mining, placement, item use and entity/block interaction are locked while phased. The server remains authoritative, and an S2C phase-state payload mirrors the state to the local client so movement prediction uses the same collision rule instead of stopping at the wall first. Ending the effect searches for a nearby non-colliding position and falls back to the recorded start position if necessary.

The twenty-second base cooldown is a non-mythic cooldown active and therefore participates in `능력 가속`, sequential `이중 충전` and `전투 순환`. ROOT-direct hostile kills can also extend the currently running manual phase instance through `연장전`, with the normal per-instance +50% base-duration cap. `재연` is excluded because this is a mobility/state active rather than a replay-safe combat action.

### `거대화` — FULL runtime

The mythic active lasts twenty seconds with a sixty-second cooldown. Player scale, maximum/current health, interaction/melee reach and attack damage use transient attribute modifiers at the canonical level values; health ratio is preserved both when entering and leaving the state. Outgoing player melee knockback is doubled at the vanilla knockback boundary. Level 2 applies the canonical two-second fear to nearby normal nonboss hostiles in an eight-block radius by clearing their target and driving navigation away from the player. Dominated companions and configured bosses are excluded. End-of-state collision is corrected to a safe position when necessary.

As a mythic active, `거대화` deliberately does not participate in ability haste, double charge or replay.

### `금단의 마법` — FULL runtime

The anvil keeps normal item/enchantment applicability rules but relaxes mutual enchantment exclusivity for a player who owns the augment. The vanilla Too Expensive result cap is removed, pickup is allowed when the player actually has the required levels, and both input items' prior-work repair-cost contribution is removed from the calculated level cost. The produced item does not inherit a new prior-work penalty. Base repair, enchantment and naming costs remain; this does not turn arbitrary invalid enchantment/item pairs into valid combinations.

### `지배` — PARTIAL runtime

The implemented baseline permanently attaches an owner UUID to a dominated nonboss hostile, marks the mob persistent, stores save-wide occupied companion counts per owner UUID, and releases the slot on actual companion death even when the owner is offline. Caps are 3 / 5 / 10. Dominated entities are treated as allied with the owner and same-owner companions, normal friendly fire is rejected, and loaded dominated mobs are tracked through `ServerEntityEvents` rather than an every-tick full-world entity scan. Their server AI automatically seeks nearby hostile targets. `시간 정지` and `천벌` exclude dominated companions.

This augment remains **partial** because older combat/projectile/world-control code still contains several direct `instanceof Enemy` selectors. Friendly-fire rejection prevents owner damage, but selectors such as pull, projectile tracking/chain targeting and some nearby-enemy quota/counting paths can still select a dominated mob or waste a target slot. Comprehensive script-owned/special-entity exclusion is also not yet normalized through one shared hostility predicate. `지배` must not be counted FULL until that selector/refactor boundary is completed.

## Prior verified advanced active / interact pass retained

- `되감기`: restores the canonical historical player snapshot without rewinding inventory/world/cooldowns; 60-second cooldown participates in haste/double-charge/combat-cycle; replay excluded.
- `시간 가속`: three-second concentration, twenty-second sustained acceleration, client use-duration prediction and owned-projectile launch-speed scaling; haste/double-charge/combat-cycle integrated.
- `연장전`: ROOT-direct kill extends eligible manual non-mythic sustained actives by +1 second, configured bosses +4 seconds, capped at +50% base duration per use instance.
- `방패 던지기`: weapon-side raw melee basis, canonical target/range/damage scaling, haste/double-charge/combat-cycle and replay integration.
- `흥정`: post-vanilla final emerald price discount 10% / 20% / 30%, minimum one emerald.
- `매달리기`: server collision-validated one-block ledge mantle.

## Prior verified special temporal / mythic pass retained

- `귀환`: five-second cancellable concentration then validated personal respawn teleport, no cooldown.
- `시간 정지`: 32-block radius, nine-second hostile/control freeze, projectile/status-timer pause, boss profile gates.
- `천벌`: 32-block special percent-HP damage, normal 60% / 90%, boss 10% / 25%, Lv.2 two-second stun with boss policy gates.

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

- shared movement/aerial runtime includes `숨 고르기`, `야행성`, `햇빛 체질`, `성큼걸음`, `멀리뛰기`, `관성 전환`, `두 번 점프`, `공중 연계`, `물갈퀴`, `발 안 묶이기`, `물살 타기`, `기수`, `물 위 달리기`, `나무타기`, `매달리기`
- projectile shot-action / flight-segment runtime includes `화살 아끼기`, `관통 화살`, `연쇄 화살`, `분산 사격`, `추적 화살`, `도탄`, `집중 조준`
- common 10-slot active runtime with server-authoritative ownership/cooldown/charge/target/result validation
- ordinary cooldown actives include `정화`, `끌어당기기`, `자리 바꾸기`, `보호막`, `광폭화`, `치유 파동`, `방패 던지기`, `되감기`, `시간 가속`, `위상 이동`
- toggles: `위협 억제`, `순간 채굴`
- one-use death active: `사망 지점 귀환`
- no-cooldown concentration active: `귀환`
- mythic standalone actives include `시간 정지`, `천벌`, `지배`, `거대화`
- active meta runtime: `능력 가속`, `이중 충전`, `재연`, `전투 순환`, `연장전`

### Survival / resource / harvest / consumption / utility

- `광부의 손`, `벌목 요령`, `연속 채굴`, `도구 관리`, `방어구 관리`, `자동 보충`
- `적게 먹기`, `숨 오래 참기`, `추위 적응`, `밤눈`, `회복력`
- `풍작`, `씨앗 챙기기`, `한 조각 더`, `다시 심기`, `싹쓸이 수확`, `나무 베기`, `굴파기`
- `든든한 한 끼`, `낚시 감각`, `빨리 먹기`, `쌍둥이`, `먹으면서 걷기`, `비축 식사`
- `줍는 손`, `방패잡이`, `연료 절약`, `빠른 양조`, `경험 보존`, `소지품 보존`, `흥정`
- item/enchantment utility includes `진주 숙련`, `휴대 수류`, `금단의 마법`
- `전리품 챙기기`, `보물 낚시`, `풍요` retain functional partial baselines

## Partial augments still open

- `회복 증폭`: every external non-natural heal is not yet intercepted.
- `전리품 챙기기`: crowded simultaneous nearby-item provenance remains spatial rather than exact source-owned capture.
- `보물 낚시`: vanilla luck-equivalent adjustment is not mathematically identical to canonical +2 / +4 / +6 percentage points.
- `풍요`: exact final-output provenance and comprehensive unique/progression-item exclusions remain incomplete.
- `지배`: shared hostile-selector normalization and comprehensive script-owned/special-entity exclusion remain incomplete.

`손재주` remains unimplemented because its non-combat gathering whitelist is not fixed. `물약 확산` remains blocked because the canonical text says “주변” without a fixed radius. `탄성 저장` still lacks a canonical block-distance-to-propulsion mapping. Other still-underspecified mechanics such as `탄도 장막`, `임시 발판`, `생명 연결`, `작아지기`, `벽치기` must not receive invented values.

## Deliberately not claimed complete

- Full roster gameplay completion is **119 / 146**, not 146 / 146.
- This advanced world/control pass is build/full-pack boot verified but **not live-player playtested**.
- `위상 이동` needs live validation of wall traversal, floor retention, interaction lock, safe exit and latency/prediction behavior.
- `거대화` needs live validation of camera/scale feel, Better Combat reach/damage, fear navigation, knockback and collision-safe exit.
- `금단의 마법` needs live anvil matrix validation across mutually exclusive enchantments, repairs, names, high costs and prior-work histories.
- `지배` needs shared hostile-selector normalization, broader owned/script-critical exclusion review, AI/owner-offline/death/save-reload testing and multiplayer authority testing.
- Prior active, projectile, movement, threat and external-boss live-test gaps remain.
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

The remaining no-runtime set is now **22**. The next pass should first normalize the shared player-hostility selector so `지배` can move from PARTIAL to FULL without leaving old projectile/AoE/control paths inconsistent. After that, prefer the densest fixed-semantics subsystem among the remaining scanner/inventory/enchantment/companion/world entries. Do not fill missing parameters by inference, and keep the explicitly blocked parameter-contract entries untouched until canon changes.

## Validation labels

- CODE REVIEWED: YES
- BUILD VERIFIED: YES — `3ebeb3000a618fec998c8bd2cad4a1c05fbbcdd7`
- JAR PRODUCED: YES — Actions run `40145505210`
- PACK DEPENDENCY INSPECTION: PASS — 23 pinned external artifacts
- PACK RUNTIME CLASSPATH: PASS — 150 files
- PACK SERVER SMOKE: PASS — 92 mods
- PACK CLIENT SMOKE: PASS — 108 mods
- VERIFICATION ARTIFACT: `augmentation-survival-pack-smoke-72` / ID `12580571756`
- VERIFICATION ARTIFACT SHA-256: `fe3e2ed9e9120d7cf2bfc2b822f0d2ddaac0d1b9b04f905d4aae34daad164b58`
- CANONICAL AUGMENT CATALOG INITIALIZATION: PASS — 146 definitions
- ENTITY COMPATIBILITY CONTRACT INITIALIZATION: PASS — 8 exact / 11 token / 1 dynamic
- WORLD BOSSES LIVE REGISTRY PROFILE BINDING: PASS — 11 / 11
- MCENDGAME DYNAMIC BOSS ADAPTER BINDING: PASS — 1 / 1
- CHRONO DAWN EXACT BOSS PROFILE DEFINITIONS: PASS — 6
- ENEMY SCALING CONTRACT INITIALIZATION: PASS — 9 stages / 4 native exact profiles / 0 namespace profiles
- ADVANCED WORLD / PHASE / GIANT / DOMINATION / FORBIDDEN-MAGIC RUNTIME: BUILD/BOOT VERIFIED
- NEW 3-FULL + 1-PARTIAL BATCH LIVE-PLAYER TESTED: NO
- PHASE-SHIFT LIVE-PLAYER TESTED: NO
- GIGANTIFICATION LIVE-PLAYER TESTED: NO
- FORBIDDEN-MAGIC LIVE-PLAYER TESTED: NO
- DOMINATION LIVE-PLAYER TESTED: NO
- EXTERNAL BOSS LIVE ENCOUNTER TESTED: NO
- ACTIVE ENGINE LIVE-PLAYER TESTED: NO
- PROJECTILE LIVE-PLAYER TESTED: NO
- MOVEMENT LIVE-PLAYER TESTED: NO
- DEVICE FLOW PLAYTESTED: NO
- UI VISUAL REVIEW: NO
- BETTER COMBAT GAMEPLAY TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

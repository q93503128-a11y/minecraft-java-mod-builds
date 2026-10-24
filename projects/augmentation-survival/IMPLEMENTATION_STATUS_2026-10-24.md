# Augmentation Survival — Implementation Status

Date: 2026-10-24
Status: M1C FUNCTIONAL BASELINE / 106-OF-146 GAMEPLAY RUNTIME / ADVANCED UTILITY + PERMANENT PROGRESSION PASS VERIFIED

## 146-roster implementation count

The count below measures whether the canonical gameplay effect has a runtime implementation. It does **not** mean final VFX/UI polish or live playtesting is complete.

- Fully implemented gameplay runtime: **106 / 146**
- Partially implemented: **4 / 146** — `회복 증폭`, `전리품 챙기기`, `보물 낚시`, `풍요`
- Not fully implemented: **40 / 146**
- No gameplay runtime yet: **36 / 146**
- **This verified batch adds 6 new fully implemented gameplay runtimes.**
- Newly implemented: `방어 무시`, `사냥 기록`, `나무타기`, `진주 숙련`, `휴대 수류`, `사망 지점 귀환`
- Partially implemented this batch: **0**
- Existing augment implementations completed/improved this batch: **0**
- Shared systems materially extended: combat armor-effectiveness boundary, persistent per-player augment state, movement utility runtime, active routing, item-use cooldown/Riptide integration.

`회복 증폭`, `전리품 챙기기`, `보물 낚시`, `풍요` retain their previously documented partial limitations.

## Verified code checkpoint

- Verified gameplay-code checkpoint: `c131c2121f9418a23db29f9701348972ec8b96bd`
- Primary feature commit: `783eb0bde5c09c1c1fe0f8867060814aa06d9c10` — `feat(augmentation-survival): implement advanced utility augment batch`
- Runtime-fix commits:
  - `2a41c210eb96c28c087d40c8278ca9690e6588a7` — bind ender-pearl cooldown at the 26.2 `UseCooldown` component boundary
  - `c131c2121f9418a23db29f9701348972ec8b96bd` — target the actual 26.2 `Player.isInWaterOrRain()` Riptide checks
- GitHub Actions: `40099460009` / Build Augmentation Survival Pack #65
- Build and pinned-pack inspection: PASS
- Pack dependency inspection: PASS — 23 pinned external artifacts
- Runtime classpath verification: PASS — 150 files
- Dedicated server smoke: PASS — 92 mods
- Client boot smoke: PASS — 108 mods
- Verification artifacts upload: PASS
- Artifact: `augmentation-survival-pack-smoke-65`
- Artifact ID: `12562406429`
- Artifact ZIP size: 657736 bytes
- Artifact ZIP SHA-256: `a0a35296563cf902d532e597ee41e54027fe7300ea36825faf04d68464f1ddc7`
- Canonical augment catalog initialization: PASS — 146 definitions
- Entity compatibility contract initialization: PASS — 8 exact selectors / 11 token selectors / 1 dynamic adapter
- Enemy scaling contract initialization: PASS — 9 stages / 4 native exact profiles / 0 namespace profiles
- Stat / survival / harvest / consumption / combat / expansion / movement / projectile / reactive / utility contracts: BUILD + FULL-PACK BOOT VERIFIED
- Advanced utility contract: BUILD + FULL-PACK BOOT VERIFIED

Build #63 first exposed the old ender-pearl item-level cooldown target. Build #64 then exposed the wrong owner type on the Trident water check. Both were corrected at the actual 26.2 runtime boundaries without deleting or weakening the features. Build #65 passed the complete pack matrix.

Documentation commits after this checkpoint do not change Java gameplay code and must not be treated as new gameplay verification targets.

## Newly verified advanced utility / permanent progression pass

### `방어 무시`

ROOT direct player attacks now reduce the target armor and armor-toughness inputs used by vanilla armor absorption by 10% / 20% / 30%. Derived damage does not recursively inherit the reduction. The implementation wraps the existing armor calculation rather than replacing the full damage pipeline.

### `사냥 기록`

The first qualifying ROOT-direct kill of each hostile entity registry type is recorded permanently in the server-owned player state. External hostile entity types count independently. The stack cap is 30. Each stack grants +1 max HP and +0.5% ROOT direct damage, for the canonical maximum of +30 HP and +15% direct damage. Companion/derived kills do not create records. The max-health modifier is reconstructed from persistent stacks on join/respawn/state reconciliation.

### `나무타기`

Players with the augment can climb while pressing into adjacent vanilla-tagged logs/leaves. The server runtime uses ladder ascent speed as the baseline and applies the canonical 40% / 60% / 80% scale. It does not run a world-wide entity scan and removes the holder from the tracked set when the augment is absent.

### `진주 숙련`

Ender-pearl self-damage is reduced by 50% / 75% / 100% before augment protection pools consume the hit. Ender-pearl use cooldown is reduced by 15% / 30% / 45% at the vanilla 26.2 `UseCooldown` data-component application boundary. Other item cooldowns are unaffected.

### `휴대 수류`

A dry player with Riptide and a water bucket in the offhand can satisfy the water/rain requirement. The implementation modifies the actual Trident Riptide wet-condition checks. The offhand water bucket is consumed into an empty bucket only after a successful dry portable-Riptide release; normal water/rain Riptide does not consume the bucket. Creative-mode use does not consume water.

### `사망 지점 귀환`

An actual death grants one server-owned use token when a valid vanilla last-death location exists. Activating the augment teleports across dimensions to that recorded death position and consumes the token only after successful teleport. Because the token is granted from the actual respawn boundary, lethal-prevention paths such as `재림` do not create a new death-return charge.

## Implemented foundations retained

### Runtime / progression / device

- persistent server-owned player augment state with optional schema-v2 `persistent_ints`
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
- combat provenance with ROOT/derived/reflected/path boundaries
- post-mitigation protection/delayed-damage pipeline
- augment-generated primary-healing pipeline
- lethal order: `한 번 버티기` -> vanilla held Totem -> `비상 토템` -> `재림`
- broad combat expansion including `연쇄 폭발`, `벼락`, `메아리 타격`, `약점 표식`, `속도 전환`, `육중한 일격`, `생명 파동`, `난전`, `유리 대포`, `우두머리 사냥`
- reactive/status runtime including `돌려보내기`, `상태 반사`, `강인한 정신`, `보호막 폭발`, `되밟기`, `무적`
- advanced combat bridge now includes `방어 무시` and `사냥 기록`

### Movement / projectile / active

- shared movement/aerial runtime including `숨 고르기`, `야행성`, `햇빛 체질`, `성큼걸음`, `멀리뛰기`, `관성 전환`, `두 번 점프`, `공중 연계`, `물갈퀴`, `발 안 묶이기`, `물살 타기`, `기수`, `물 위 달리기`, `나무타기`
- shared projectile shot-action / flight-segment runtime for `화살 아끼기`, `관통 화살`, `연쇄 화살`, `분산 사격`, `추적 화살`, `도탄`, plus `집중 조준`
- common 10-slot active runtime with server-authoritative ownership/cooldown/charge/target/result validation
- supported cooldown actives: `정화`, `끌어당기기`, `자리 바꾸기`, `보호막`, `광폭화`, `치유 파동`
- supported toggles: `위협 억제`, `순간 채굴`
- one-use death active: `사망 지점 귀환`
- active meta runtime: `능력 가속`, `이중 충전`, `재연`, `전투 순환`

### Survival / resource / harvest / consumption / utility

- `광부의 손`, `벌목 요령`, `연속 채굴`, `도구 관리`, `방어구 관리`, `자동 보충`
- `적게 먹기`, `숨 오래 참기`, `추위 적응`, `밤눈`, `회복력`
- `풍작`, `씨앗 챙기기`, `한 조각 더`, `다시 심기`, `싹쓸이 수확`, `나무 베기`, `굴파기`
- `든든한 한 끼`, `낚시 감각`, `빨리 먹기`, `쌍둥이`, `먹으면서 걷기`, `비축 식사`
- `줍는 손`, `방패잡이`, `연료 절약`, `빠른 양조`, `경험 보존`, `소지품 보존`
- item/enchantment utility now includes `진주 숙련`, `휴대 수류`
- `전리품 챙기기`, `보물 낚시`, `풍요` retain functional partial baselines

## Save-wide hostile threat scaling and external boss compatibility

The existing save-wide 0–8 threat runtime remains active. Stage rises reconcile loaded scalable entities once; newly loaded/spawned hostiles receive the current stage; max-health changes preserve health ratio. Bosses use encounter-stage snapshots. Hostile outgoing damage is amplified once at the shared server boundary and delayed-damage installments do not receive a second multiplier.

`entity_compatibility_profiles.json` remains the shared boss identity/control-policy contract. Native exact identities include Wither, Ender Dragon and six Chrono Dawn bosses. Pinned World Bosses resolves 11 token profiles to exact live registry IDs. MCEndgame uses its dynamic dungeon-boss accessor. Boss identity remains separate from boss-core reward assignment.

Existing consumers continue to share that identity for boss threat scaling, `우두머리 사냥`, `전투 순환`, normal-mob-only loot exclusions, boss pull handling, `위협 억제`, and conservative generic boss-origin status handling.

## Partial augments still open

- `회복 증폭`: every external non-natural heal is not yet intercepted.
- `전리품 챙기기`: crowded simultaneous nearby-item provenance remains spatial rather than exact source-owned capture.
- `보물 낚시`: vanilla luck-equivalent adjustment is not mathematically identical to canonical +2 / +4 / +6 percentage points.
- `풍요`: exact final-output provenance and comprehensive unique/progression-item exclusions remain incomplete.

`손재주` remains unimplemented because its non-combat gathering whitelist is not fixed. `물약 확산` was deliberately not implemented in this batch because its canonical text still says “주변” without a fixed radius. Other still-underspecified mechanics such as `탄도 장막`, `임시 발판`, `생명 연결`, `작아지기`, `벽치기` must not receive invented values.

## Deliberately not claimed complete

- Full roster gameplay completion is **106 / 146**, not 146 / 146.
- The new six-augment pass is build/full-pack boot verified but **not live-player playtested**.
- `휴대 수류` still needs live client/server input-feel validation for the dry Riptide start/release cycle and bucket synchronization.
- `진주 숙련` still needs live validation of self-damage and client cooldown presentation.
- `사냥 기록` needs live save/rejoin and multi-mod hostile-type validation.
- `나무타기` needs feel/collision validation on irregular modded trees.
- `사망 지점 귀환` needs actual cross-dimension death/respawn/use validation.
- External boss encounters, threat stage progression, Farmer's Delight compatibility and broad combat/resource matrices remain live-test pending.
- Final device/active/combat/resource/threat VFX, sound, models, HUD and UI visual polish are not complete.
- The pre-existing `ProjectileAugmentService` deprecated-API warning remains non-fatal.
- Multiplayer is not tested.

## Batching rule

Do not return to one-augment-at-a-time implementation except for isolated bugs or genuinely unique mechanics.

For every next batch:

1. recompute the locked 146-roster implementation inventory;
2. report full / partial / not-fully / no-runtime counts;
3. reuse or create one meaningful shared runtime boundary;
4. connect every canonically fixed easy/medium augment that safely fits it;
5. validate once after the meaningful batch;
6. leave only genuinely unique or underspecified mechanics for specialized systems.

## Next implementation direction

The remaining no-runtime set is now **36**. Recompute those 36 from the locked roster before coding. Prefer the next densest group whose semantics are already fixed and whose authority can stay server-side. Good candidates are world/interact/entity/active groups that can reuse existing state and compatibility services. Do not fill missing parameters by inference.

## Validation labels

- CODE REVIEWED: YES
- BUILD VERIFIED: YES — `c131c2121f9418a23db29f9701348972ec8b96bd`
- JAR PRODUCED: YES — Actions run `40099460009`
- PACK DEPENDENCY INSPECTION: PASS — 23 pinned external artifacts
- PACK RUNTIME CLASSPATH: PASS — 150 files
- PACK SERVER SMOKE: PASS — 92 mods
- PACK CLIENT SMOKE: PASS — 108 mods
- VERIFICATION ARTIFACT: `augmentation-survival-pack-smoke-65` / ID `12562406429`
- VERIFICATION ARTIFACT SHA-256: `a0a35296563cf902d532e597ee41e54027fe7300ea36825faf04d68464f1ddc7`
- CANONICAL AUGMENT CATALOG INITIALIZATION: PASS — 146 definitions
- ENTITY COMPATIBILITY CONTRACT INITIALIZATION: PASS — 8 exact / 11 token / 1 dynamic
- WORLD BOSSES LIVE REGISTRY PROFILE BINDING: PASS — 11 / 11
- MCENDGAME DYNAMIC BOSS ADAPTER BINDING: PASS — 1 / 1
- CHRONO DAWN EXACT BOSS PROFILE DEFINITIONS: PASS — 6
- ENEMY SCALING CONTRACT INITIALIZATION: PASS — 9 stages / 4 native exact profiles / 0 namespace profiles
- ADVANCED UTILITY RUNTIME: BUILD/BOOT VERIFIED
- NEW 6-AUGMENT BATCH LIVE-PLAYER TESTED: NO
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

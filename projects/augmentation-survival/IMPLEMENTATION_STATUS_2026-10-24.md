# Augmentation Survival — Implementation Status

Date: 2026-10-24
Status: M1C FUNCTIONAL BASELINE / PROJECTILE + ACTIVE/COMBAT + SURVIVAL/RESOURCE SHARED RUNTIMES IMPLEMENTED / LOOT+FARMING BULK NEXT

## 146-roster implementation count

The count below measures whether the canonical gameplay effect has a runtime implementation. It does **not** mean final VFX/UI polish or live playtesting is complete.

- Fully implemented gameplay runtime: **69 / 146**
- Partially implemented: **1 / 146** — `회복 증폭`
- Not fully implemented: **77 / 146**
- No gameplay runtime yet: **76 / 146**
- **This batch completed: 17 augments**

The 17-count includes 15 previously unimplemented augments plus completion of the missing facets of `야행성` and `햇빛 체질`.

## Verified code checkpoint

- Verified gameplay-code checkpoint: `1c9cae521727e9280b4ba0e42159916284e32205`
- Feature checkpoint: `feat(augmentation-survival): build shared survival resource runtime`
- GitHub Actions: `40078301895` / Build Augmentation Survival Pack #47
- Build and pinned-pack inspection: PASS
- Pack dependency inspection: PASS — 23 pinned external artifacts
- Runtime classpath verification: PASS — 150 files
- Dedicated server smoke: PASS
- Client boot smoke: PASS
- Verification artifacts upload: PASS
- Artifact: `augmentation-survival-pack-smoke-47`
- Artifact ID: `12554605978`
- Artifact ZIP SHA-256: `39a861e050b7b2054eb3469f1d97cf2b454ad48790466de2176ceb461a35781d`
- Stat contract initialization: PASS — 7 definitions / 7 modifier groups
- Runtime initialization marker: `Loaded survival augment contract: shared resource, hunger, environment and utility runtime`

Documentation commits after this checkpoint do not change Java gameplay code and must not be treated as new gameplay verification targets.

## Implemented foundations retained

### Runtime / progression / device

- persistent server-owned player augment state
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

- unconditional stat contract now has 7 definitions / 7 modifier groups
- combat provenance and ROOT/derived recursion boundaries
- post-mitigation protection/delayed-damage pipeline
- augment-generated primary-healing pipeline
- lethal order: `한 번 버티기` -> vanilla held Totem -> `비상 토템` -> `재림`
- previous broad combat expansion retained: `연쇄 폭발`, `벼락`, `메아리 타격`, `약점 표식`, `속도 전환`, `육중한 일격`, `생명 파동`, `난전`, `유리 대포`, `우두머리 사냥`

### Movement / projectile / active

- shared movement/aerial runtime including `두 번 점프`, `공중 연계`, `관성 전환`, `멀리뛰기`, `성큼걸음`
- shared projectile shot-action / flight-segment runtime for `화살 아끼기`, `관통 화살`, `연쇄 화살`, `분산 사격`, `추적 화살`, `도탄`
- common 10-slot active runtime with server-authoritative ownership/cooldown/charge/target/result validation
- supported actives: `정화`, `끌어당기기`, `자리 바꾸기`, `보호막`, `광폭화`, `치유 파동`
- active meta runtime: `능력 가속`, `이중 충전`, `재연`, `전투 순환`

## New survival / resource bulk batch — 17 augments

`survival_modifier_contract.json` is the shared tuning source for resource, hunger, environment and utility behavior. The batch reuses vanilla attributes and narrow gameplay boundaries instead of adding one private tick loop per augment.

### Mining / gathering speed

- `광부의 손` — block break speed +4% per level
- `벌목 요령` — log block break speed +10% per level using `BlockTags.LOGS`
- `야행성` — previous night movement facet retained; missing nighttime mining-speed facet is now implemented at +5% per level
- `연속 채굴` — successful block breaks within a 40-tick window build up to 5 stacks; each stack grants +3% block-break speed per augment level
- mining-speed bonuses on this shared boundary are added together before one final scale rather than multiplied independently

### Movement / interaction utility

- `물갈퀴` — while in water, +10% movement speed per level through the existing shared conditional movement modifier
- `긴 팔` — +0.5 block interaction range per level through vanilla `BLOCK_INTERACTION_RANGE`
- `물속 작업` — +0.16 `SUBMERGED_MINING_SPEED` per level, giving the canonical underwater mining-penalty reduction without replacing the mining loop
- `발 안 묶이기` — +0.15 `MOVEMENT_EFFICIENCY` per level so vanilla difficult-terrain slowdown is reduced through the native attribute path

### Hunger / oxygen / cold / vision

- `적게 먹기` — food exhaustion reduced by 4% per level at the vanilla exhaustion boundary
- `숨 오래 참기` — server-derived vanilla oxygen-bonus attribute values implement 10% / 20% / 30% reduced oxygen consumption
- `추위 적응` — freezing buildup is reduced 15% / 30% / 45% using fractional compensation on actual frozen-tick increases
- `밤눈` — refreshes a hidden vanilla Night Vision effect; no particles/icon are shown by this baseline

### Durability / inventory utility

- `도구 관리` — 5% per level chance to cancel the whole durability-loss event for vanilla-component tools before the normal Unbreaking path
- `방어구 관리` — 5% per level chance to cancel the whole durability-loss event for currently equipped armor
- `자동 보충` — when the selected hotbar stack becomes empty, an identical item+components stack is moved from the main inventory into that selected slot

### Natural regeneration

- `회복력` — accelerates the vanilla natural-regeneration timer by 10% per level instead of creating a separate fake healing tick
- `햇빛 체질` — previous daytime movement facet retained; missing daytime natural-regeneration interval reduction is now implemented at 5% per level
- `회복력` and `햇빛 체질` combine on the same timer boundary and still require vanilla natural-regeneration conditions

## Deliberately not claimed complete

- Full roster gameplay completion is **69 / 146**, not 146 / 146.
- `회복 증폭` remains partial because the amplification path still covers routed augment-generated non-natural healing rather than every external non-natural heal.
- New survival/resource effects are build/boot verified but **not live-player tested**.
- `밤눈` currently uses hidden vanilla Night Vision as the gameplay baseline; final presentation is not done.
- `물갈퀴` needs live swim-feel testing and external movement-mod coexistence testing.
- `추위 적응` needs powder-snow/freezing live testing.
- `도구 관리` / `방어구 관리` need live Unbreaking and external-item interaction testing; external tools without the vanilla `TOOL` component may require adapters.
- `자동 보충` currently refills only the selected hotbar slot from main inventory slots and needs live inventory-sync testing.
- `회복력` / `햇빛 체질` need live saturation/hunger/day-night natural-regeneration testing.
- `긴 팔`, `물속 작업`, `발 안 묶이기` intentionally use vanilla attributes and still need live feel/compatibility validation.
- Drop-producing/farming augments were intentionally not mixed into this batch. They need one shared block-output / loot boundary so Fortune, Silk Touch, Farmer's Delight and external drops are not accidentally duplicated or bypassed.
- Active input remains a functional baseline; `Left Alt + 1..0` may also switch the vanilla hotbar until final input UX is implemented.
- `약점 표식` multiplayer ownership still needs a multi-owner-safe representation.
- full Chrono Dawn / World Bosses / MCEndgame boss identity adapters remain incomplete for boss-targeted control/damage effects.
- projectile and movement shared runtimes remain build/boot verified, not live-playtested for feel and matrix interactions.
- save-wide threat high-water state exists but final live hostile stat application remains incomplete.
- final device/active/combat/resource VFX, sound, models, HUD and UI visual polish are not done.
- compilation still reports the pre-existing `ProjectileAugmentService` deprecated-API warning; it is not a #47 failure and is not claimed resolved.
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

## Next implementation unit

Next large batch: **shared block-output / loot / farming runtime**.

Primary goals:

- inventory all remaining crop, seed, ore/drop, harvest, replant, food-production and compatible collection augments before coding;
- preserve vanilla Fortune/Silk Touch and Farmer's Delight behavior rather than replacing loot logic wholesale;
- distinguish original loot from augment-added output so extra-output effects cannot recursively multiply each other unless canon explicitly permits it;
- avoid global block/entity scans; operate from actual harvest/break/drop events;
- connect every compatible canonical augment in the same batch instead of stopping after 2–3 effects.

After that, choose the next bulk boundary from remaining status/reactive combat, fishing/consumption, collision-heavy movement physics, companion/entity systems, save-wide threat scaling or higher-tier unique mechanics.

## Validation labels

- CODE REVIEWED: YES
- BUILD VERIFIED: YES — `1c9cae521727e9280b4ba0e42159916284e32205`
- JAR PRODUCED: YES — Actions run `40078301895`
- PACK DEPENDENCY INSPECTION: PASS — 23 pinned external artifacts
- PACK RUNTIME CLASSPATH: PASS — 150 files
- PACK SERVER SMOKE: PASS
- PACK CLIENT SMOKE: PASS
- STAT CONTRACT INITIALIZATION: PASS — 7 definitions / 7 modifier groups
- SURVIVAL CONTRACT INITIALIZATION: PASS
- COMBAT/PROTECTION/HEAL/DEATH CONTRACT INITIALIZATION: PASS
- EXPANSION CONTRACT INITIALIZATION: PASS
- MOVEMENT CONTRACT INITIALIZATION: PASS
- PROJECTILE CONTRACT INITIALIZATION: PASS
- SURVIVAL/RESOURCE LIVE-PLAYER TESTED: NO
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
- EXTERNAL BOSS GAMEPLAY TESTED: NO
- MULTIPLAYER TESTED: NO

# Augmentation Survival — Implementation Status

Date: 2026-10-24
Status: M1C FUNCTIONAL BASELINE / COMBAT+MOVEMENT+PROJECTILE+ACTIVE+SURVIVAL+HARVEST SHARED RUNTIMES IMPLEMENTED / NEXT BULK SYSTEM BATCH

## 146-roster implementation count

The count below measures whether the canonical gameplay effect has a runtime implementation. It does **not** mean final VFX/UI polish or live playtesting is complete.

- Fully implemented gameplay runtime: **76 / 146**
- Partially implemented: **2 / 146** — `회복 증폭`, `전리품 챙기기`
- Not fully implemented: **70 / 146**
- No gameplay runtime yet: **68 / 146**
- **This batch touched: 8 augments**
  - newly fully implemented: **7**
  - newly partially implemented: **1**

This batch completes the canonical runtime baseline for `풍작`, `씨앗 챙기기`, `한 조각 더`, `다시 심기`, `싹쓸이 수확`, `나무 베기`, `굴파기`. `전리품 챙기기` has a functional normal-mob item-bundle duplication baseline but remains partial until external-boss and crowded-drop provenance coverage is complete.

## Verified code checkpoint

- Verified gameplay-code checkpoint: `8b8b2974726a6a60ea05fb3a898ac705ba4c913f`
- Main feature commit: `df9dea84098d37bffb3bf309176dbd9de08eda6a` — `feat(augmentation-survival): add farming and derived harvest runtime`
- 26.2 API adaptation: `8b8b2974726a6a60ea05fb3a898ac705ba4c913f` — `fix(augmentation-survival): align harvest runtime with 26.2 API`
- GitHub Actions: `40079795087` / Build Augmentation Survival Pack #49
- Build and pinned-pack inspection: PASS
- Pack dependency inspection: PASS — 23 pinned external artifacts
- Runtime classpath verification: PASS — 150 files
- Dedicated server smoke: PASS
- Client boot smoke: PASS
- Verification artifacts upload: PASS
- Artifact: `augmentation-survival-pack-smoke-49`
- Artifact ID: `12553959283`
- Artifact ZIP SHA-256: `e75a797d5102cf3eb68ead72d7c5279860079d46223330f16f167714979d73a2`
- Stat contract initialization: PASS — 7 definitions / 7 modifier groups
- Survival contract initialization: PASS
- Resource-harvest contract initialization: PASS — `Loaded resource harvest contract: crop, block-drop and derived-harvest runtime`
- Combat / expansion / movement / projectile contract initialization: PASS

The first feature run, Build Augmentation Survival Pack #48 (`40079556176`), failed during compilation because the Fabric block-break callback exposed `Level` rather than `ServerLevel` and Minecraft 26.2 does not expose `BlockTags.ORES`. The follow-up commit changed only those API boundaries: the callback now narrows to `ServerLevel` and ore checks use Fabric's conventional ore tag. #49 then passed build plus both full-pack smoke tests. The failed run was not blindly rerun.

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

- unconditional stat contract: 7 definitions / 7 modifier groups
- combat provenance and ROOT/derived recursion boundaries
- post-mitigation protection/delayed-damage pipeline
- augment-generated primary-healing pipeline
- lethal order: `한 번 버티기` -> vanilla held Totem -> `비상 토템` -> `재림`
- broad combat expansion retained: `연쇄 폭발`, `벼락`, `메아리 타격`, `약점 표식`, `속도 전환`, `육중한 일격`, `생명 파동`, `난전`, `유리 대포`, `우두머리 사냥`

### Movement / projectile / active

- shared movement/aerial runtime including `숨 고르기`, `야행성`, `햇빛 체질`, `성큼걸음`, `멀리뛰기`, `관성 전환`, `두 번 점프`, `공중 연계`, `물갈퀴`, `발 안 묶이기`
- shared projectile shot-action / flight-segment runtime for `화살 아끼기`, `관통 화살`, `연쇄 화살`, `분산 사격`, `추적 화살`, `도탄`
- common 10-slot active runtime with server-authoritative ownership/cooldown/charge/target/result validation
- supported actives: `정화`, `끌어당기기`, `자리 바꾸기`, `보호막`, `광폭화`, `치유 파동`
- active meta runtime: `능력 가속`, `이중 충전`, `재연`, `전투 순환`

### Existing survival / utility runtime

- `광부의 손`, `벌목 요령`, `야행성` mining facet, `연속 채굴`
- `물갈퀴`, `긴 팔`, `물속 작업`, `발 안 묶이기`
- `적게 먹기`, `숨 오래 참기`, `추위 적응`, `밤눈`
- `도구 관리`, `방어구 관리`, `자동 보충`
- `회복력`, `햇빛 체질` natural-regeneration facet
- mining-speed bonuses on the shared boundary are additive before one final scale rather than independently multiplied
- survival/static utility effects use native vanilla attributes or narrow vanilla boundaries where possible

## New resource-harvest bulk batch — 8 augments touched

`resource_harvest_contract.json` is the data-driven tuning source for crop output, ore output, normal-mob bundle duplication and derived crop/tree/tunnel actions.

### Root crop / ore output

- `풍작` (`bumper_crop`) — mature crop harvest has +8% per level chance to create one additional primary crop output
- `씨앗 챙기기` (`seed_saver`) — mature crop harvest has +15% per level chance to create one additional planting item
- `한 조각 더` (`extra_piece`) — conventional ore break has +5% per level chance to create one additional ordinary primary drop; augment cores are explicitly excluded
- drop inspection recomputes the vanilla block drop result from the actual pre-break tool snapshot so Fortune/Silk Touch-compatible output identity is preserved rather than guessed from block IDs

### Replant / sweeping harvest

- `다시 심기` (`replant`) — after mature crop destruction, waits one server tick for the normal crop drops, consumes one nearby matching planting item, and restores the age-0 crop only if the original block position is still empty
- `싹쓸이 수확` (`sweeping_harvest`) — on root mature-crop harvest, selects nearest same-crop mature blocks in a bounded local search and destroys up to 2 / 4 / 8 through the normal server block-destruction path
- sweep-derived crops force the canonical replant behavior even when the player does not separately own `다시 심기`
- current sweep search tuning is 2 blocks horizontally and 1 block vertically; this is an implementation tuning value rather than a canonically fixed radius

### Tree felling / tunneling

- `나무 베기` (`tree_felling`) — breaking a log starts a bounded connected-log breadth-first traversal and destroys up to 3 / 6 / 10 additional connected log-tag blocks
- `굴파기` (`tunneling`) — after an ordinary non-ore/non-log/non-crop/non-block-entity root break, destroys up to 1 / 2 / 3 matching blocks behind the root mining direction
- all automatic secondary block breaks re-enter `ServerPlayerGameMode.destroyBlock` instead of using raw `setBlock(AIR)`, preserving normal tool eligibility, block-break hooks, Fortune/Silk Touch, item drops, stats and existing durability-saving hooks
- a thread-local derived-break mode prevents sweeping harvest, tree felling and tunneling from geometrically retriggering themselves
- tunneling measures the actual durability delta after each derived vanilla break and refunds fractional durability so the additional-block effective durability cost is 50%; this composes with Unbreaking and `도구 관리` instead of bypassing them

### Normal-mob loot duplication — PARTIAL

- `전리품 챙기기` (`loot_scoop`) — direct player normal-mob kills roll once at 5% / 10% / 15%; on success, fresh nearby ordinary item drops are copied as one death-bundle baseline
- augment cores are explicitly excluded
- copies generated by the augment are tagged in transient runtime state so they cannot be recopied
- currently configured boss identities are excluded
- remains **PARTIAL** because external boss identification is not yet comprehensive and the one-tick nearby-item capture still needs provenance hardening for crowded simultaneous drop situations

## Deliberately not claimed complete

- Full roster gameplay completion is **76 / 146**, not 146 / 146.
- `회복 증폭` remains partial because amplification still covers routed augment-generated non-natural healing rather than every external non-natural heal.
- `전리품 챙기기` remains partial for the provenance/boss-coverage reasons above.
- `손재주` is not implemented because the canon specifies a whitelist but the exact whitelist entries are not currently fixed; no arbitrary whitelist was invented.
- `풍요` is not implemented yet because its final x2 rule spans compatible mining, harvesting, fishing and general-mob output after all other compatible drop calculation and needs a broader final-output boundary.
- `쌍둥이` breeding output is not part of this batch and remains pending.
- new resource-harvest effects are build/boot verified but **not live-player tested**.
- Farmer's Delight crop inheritance/planting-item discovery still needs live gameplay validation.
- `다시 심기` needs live validation that normal item entities are available on the chosen one-tick boundary for all supported crop families.
- `싹쓸이 수확` search radius and visual/feel behavior are not playtested.
- `나무 베기` connected-log traversal needs real tree/modded-tree testing.
- `굴파기` direction feel, same-family behavior and the edge case where a tool breaks during a derived block remain live-test items.
- normal-mob loot duplication needs crowded-drop and external-mob testing.
- previous survival/resource effects remain build/boot verified but not live-player tested.
- active input remains a functional baseline; `Left Alt + 1..0` may also switch the vanilla hotbar until final input UX is implemented.
- `약점 표식` multiplayer ownership still needs a multi-owner-safe representation.
- full Chrono Dawn / World Bosses / MCEndgame boss identity adapters remain incomplete for boss-targeted control/damage and loot-exclusion effects.
- projectile and movement shared runtimes remain build/boot verified, not live-playtested for feel and matrix interactions.
- save-wide threat high-water state exists but final live hostile stat application remains incomplete.
- final device/active/combat/resource VFX, sound, models, HUD and UI visual polish are not done.
- compilation still reports the pre-existing `ProjectileAugmentService` deprecated-API warning; it is not a #49 failure and is not claimed resolved.
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

Choose the next bulk boundary from the remaining roster rather than spending a full pass on one easy augment. High-density candidates now include:

- fishing / food-consumption / breeding output (`낚시 감각`, `보물 낚시`, food effects, `쌍둥이`) and a final compatible-output pipeline needed by `풍요`;
- remaining protection/reactive/status combat effects that can reuse the damage/heal/provenance state;
- collision/medium-heavy movement mechanics that still require a dedicated physics boundary;
- companion/entity systems;
- save-wide threat live hostile scaling and external boss adapters;
- higher-tier unique mechanics whose parameters are already canonically fixed.

Do not implement `손재주` until its non-combat gathering whitelist is actually fixed in canon.

## Validation labels

- CODE REVIEWED: YES
- BUILD VERIFIED: YES — `8b8b2974726a6a60ea05fb3a898ac705ba4c913f`
- JAR PRODUCED: YES — Actions run `40079795087`
- PACK DEPENDENCY INSPECTION: PASS — 23 pinned external artifacts
- PACK RUNTIME CLASSPATH: PASS — 150 files
- PACK SERVER SMOKE: PASS
- PACK CLIENT SMOKE: PASS
- STAT CONTRACT INITIALIZATION: PASS — 7 definitions / 7 modifier groups
- SURVIVAL CONTRACT INITIALIZATION: PASS
- RESOURCE-HARVEST CONTRACT INITIALIZATION: PASS
- COMBAT/PROTECTION/HEAL/DEATH CONTRACT INITIALIZATION: PASS
- EXPANSION CONTRACT INITIALIZATION: PASS
- MOVEMENT CONTRACT INITIALIZATION: PASS
- PROJECTILE CONTRACT INITIALIZATION: PASS
- RESOURCE-HARVEST LIVE-PLAYER TESTED: NO
- FARMER'S DELIGHT HARVEST COMPATIBILITY TESTED: NO
- LOOT-SCOOP LIVE TESTED: NO
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
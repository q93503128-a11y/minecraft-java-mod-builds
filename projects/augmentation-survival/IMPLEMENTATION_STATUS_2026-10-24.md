# Augmentation Survival — Implementation Status

Date: 2026-10-24
Status: M1C FUNCTIONAL BASELINE / COMBAT+MOVEMENT+PROJECTILE+ACTIVE+SURVIVAL+HARVEST+CONSUMPTION SHARED RUNTIMES IMPLEMENTED / THREAT APPLICATION BULK NEXT

## 146-roster implementation count

The count below measures whether the canonical gameplay effect has a runtime implementation. It does **not** mean final VFX/UI polish or live playtesting is complete.

- Fully implemented gameplay runtime: **82 / 146**
- Partially implemented: **4 / 146** — `회복 증폭`, `전리품 챙기기`, `보물 낚시`, `풍요`
- Not fully implemented: **64 / 146**
- No gameplay runtime yet: **60 / 146**
- **This batch touched: 8 augments**
  - newly fully implemented: **6**
  - newly partially implemented: **2**

This batch completes the canonical runtime baseline for `든든한 한 끼`, `낚시 감각`, `빨리 먹기`, `쌍둥이`, `먹으면서 걷기`, `비축 식사`. `보물 낚시` and `풍요` now have functional baselines but remain partial for the exactness/provenance reasons documented below.

## Verified code checkpoint

- Verified gameplay-code checkpoint: `fb7c963a93ef721810512d3d4510195f1ae5fd1f`
- Feature commit: `fb7c963a93ef721810512d3d4510195f1ae5fd1f` — `feat(augmentation-survival): add consumption fishing and abundance runtime`
- GitHub Actions: `40081679446` / Build Augmentation Survival Pack #50
- Build and pinned-pack inspection: PASS
- Pack dependency inspection: PASS — 23 pinned external artifacts
- Runtime classpath verification: PASS — 150 files
- Dedicated server smoke: PASS — 92 mods
- Client boot smoke: PASS — 108 mods
- Verification artifacts upload: PASS
- Artifact: `augmentation-survival-pack-smoke-50`
- Artifact ID: `12555731050`
- Artifact ZIP SHA-256: `8908a66e3f83e3fc7746883d8abb7379f72f61ec7f7e9570a6888e0a2639d30c`
- Canonical augment catalog initialization: PASS — 146 definitions
- Stat contract initialization: PASS — 7 definitions / 7 modifier groups
- Survival contract initialization: PASS
- Resource-harvest contract initialization: PASS
- Consumption/fishing contract initialization: PASS — `Loaded consumption/fishing contract: food, reserve, fishing and breeding runtime`
- Combat / expansion / movement / projectile / core contracts initialization: PASS

The full-pack smoke still reports the known external/harness warnings: optional Naturalist FieldGuide mixins, missing Linux narrator `flite`, no CI OpenAL device, World Bosses texture-reference warnings, and the smoke server's initially absent `server.properties`. None prevented the server from reaching ready state or the client from remaining healthy through the smoke timeout. Compilation also still reports the pre-existing `ProjectileAugmentService` deprecated-API warning.

Documentation commits after this checkpoint do not change Java gameplay code and must not be treated as new gameplay verification targets.

## Implemented foundations retained

### Runtime / progression / device

- persistent server-owned player augment state
- generic optional persistent integer map added in schema v2 for small server-owned counters such as `비축 식사`; existing saves remain decodable because the new field defaults to an empty map
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

## Resource-harvest bulk batch retained — 8 augments touched

`resource_harvest_contract.json` remains the data-driven tuning source for crop output, ore output, normal-mob bundle duplication and derived crop/tree/tunnel actions.

### Root crop / ore output

- `풍작` — mature crop harvest has +8% per level chance to create one additional primary crop output
- `씨앗 챙기기` — mature crop harvest has +15% per level chance to create one additional planting item
- `한 조각 더` — conventional ore break has +5% per level chance to create one additional ordinary primary drop; augment cores are excluded
- drop inspection recomputes the vanilla block drop result from the actual pre-break tool snapshot so Fortune/Silk Touch-compatible output identity is preserved rather than guessed from block IDs

### Replant / sweeping harvest

- `다시 심기` — after mature crop destruction, waits one server tick for normal crop drops, consumes one nearby matching planting item, and restores the age-0 crop only if the original block position is still empty
- `싹쓸이 수확` — on root mature-crop harvest, selects nearest same-crop mature blocks in a bounded local search and destroys up to 2 / 4 / 8 through the normal server block-destruction path
- sweep-derived crops force canonical replant behavior even without separate `다시 심기`

### Tree felling / tunneling

- `나무 베기` — breaking a log starts bounded connected-log traversal and destroys up to 3 / 6 / 10 additional log-tag blocks
- `굴파기` — after an ordinary non-ore/non-log/non-crop/non-block-entity root break, destroys up to 1 / 2 / 3 matching blocks behind the root mining direction
- automatic secondary block breaks re-enter `ServerPlayerGameMode.destroyBlock`, preserving normal tool eligibility, hooks, Fortune/Silk Touch, drops, stats and existing durability-saving hooks
- a thread-local derived-break mode prevents geometric retriggering
- tunneling measures actual derived durability loss and refunds fractional durability so the effective additional-block durability cost is 50%, composing with Unbreaking and `도구 관리`

### Normal-mob loot duplication — PARTIAL

- `전리품 챙기기` — direct player normal-mob kills roll once at 5% / 10% / 15%; on success, fresh nearby ordinary item drops are copied as one death-bundle baseline
- augment cores and configured bosses are excluded; generated copies cannot recursively copy themselves
- remains **PARTIAL** because external boss identification is not comprehensive and the nearby-item capture needs stronger provenance for crowded simultaneous drops

The harvest feature was first pushed as `df9dea84098d37bffb3bf309176dbd9de08eda6a`. Build #48 (`40079556176`) exposed only two 26.2 API differences: the Fabric block-break callback provided `Level`, and `BlockTags.ORES` was unavailable. `8b8b2974726a6a60ea05fb3a898ac705ba4c913f` narrowed the level safely and moved ore checks to the Fabric conventional ore tag. Build #49 then passed build plus both full-pack smoke tests; the failed run was not blindly rerun.

## New consumption / fishing / breeding bulk batch — 8 augments touched

`consumption_fishing_contract.json` is the shared tuning source. Authority-sensitive results stay on the server. A small clientbound prediction snapshot carries only the values required for local food-use timing and use-input feel.

### Food / saturation

- `든든한 한 끼` — food consumption adds +8% / +16% / +24% extra saturation relative to the normal food saturation gain baseline
- `빨리 먹기` — food use duration is reduced 5% per level, up to 25%; server and local client use the same synced augment level rather than allowing server-only early completion
- `먹으면서 걷기` — while using a consumable, the client input path reads the actual vanilla `UseEffects.speedMultiplier` and eases that slowdown 33% / 66% / 100% toward normal movement

### Reserve meal

- `비축 식사` — can intentionally consume food while the visible hunger bar is full if reserve capacity is available
- overflow nutrition is stored at 4 / 8 / 12 hunger points in server-owned persistent state
- when vanilla exhaustion would reduce visible food level, reserve is spent first and restores that loss after the vanilla food tick; saturation/exhaustion/natural-regeneration rules are not replaced with a parallel fake hunger system
- `persistent_ints` was added as an optional schema-v2 field and is also synced only as a small client prediction/display value; the server remains authoritative

### Fishing

- `낚시 감각` — observes the actual vanilla decrease of the fishing hook's lure wait timer and adds fractional extra progress to produce the canonical 10% / 20% / 30% wait reduction without replacing the fishing state machine
- `보물 낚시` — adds a +1 / +2 / +3 vanilla fishing luck-equivalent baseline at hook construction; this is useful and functional but remains **PARTIAL** because the canonical +2 / +4 / +6 percentage-point treasure chance is not mathematically identical to vanilla luck-weight changes

### Breeding

- `쌍둥이` — after vanilla breeding finalization, rolls 10% / 20% / 30% and, on success, creates one additional offspring through the parents' normal `getBreedOffspring` path
- breeding actor attribution uses the parents' love-cause players and prefers the higher `쌍둥이` level when both are available
- the normal child, parent breeding cooldown/state and vanilla breeding transaction are retained

### Final ordinary output — `풍요` PARTIAL

- `풍요` now has a shared final ordinary-drop duplication baseline for block breaks, normal player-attributed mob kills and successful fishing retrieval
- capture is delayed until vanilla/mod drops and compatible augmentation-added output are present, then the final eligible fresh stack is copied once instead of rerunning its loot table
- block capture waits 1 tick, normal-mob capture 2 ticks, fishing capture 1 tick; the normal-mob delay allows `전리품 챙기기` output to exist before final x2 duplication
- exact original and generated item UUIDs are tracked transiently so overlapping capture windows cannot geometrically multiply the same item
- augment cores, non-stackables, damageable gear and current custom-data stacks are excluded; configured bosses are excluded from the normal-mob path
- remains **PARTIAL** because this is still spatial fresh-item capture rather than a complete source-provenance output pipeline; external boss/progression/unique-component coverage is not complete

## Deliberately not claimed complete

- Full roster gameplay completion is **82 / 146**, not 146 / 146.
- `회복 증폭` remains partial because amplification still covers routed augment-generated non-natural healing rather than every external non-natural heal.
- `전리품 챙기기` remains partial for provenance/external-boss coverage.
- `보물 낚시` remains partial because vanilla luck-equivalent adjustment is not the exact fixed percentage-point contract.
- `풍요` remains partial because exact final-output provenance and comprehensive unique/progression exclusions are incomplete.
- `손재주` is not implemented because the canon specifies a whitelist but the exact whitelist entries are not currently fixed; no arbitrary whitelist was invented.
- `한 마리 더` remains a held/unconfirmed proposal and is **not** counted in the locked 146 roster or implementation totals.
- consumption/fishing/breeding/abundance effects are build/boot verified but **not live-player tested**.
- Farmer's Delight crop, food and consumable compatibility needs live gameplay validation.
- `비축 식사` needs live persistence, hunger/exhaustion and inventory-use testing.
- `낚시 감각` needs real fishing wait-distribution testing; `보물 낚시` needs actual loot-table outcome sampling before any balance claim.
- `쌍둥이` needs live tests across vanilla and modded breedable animals.
- `먹으면서 걷기` needs live input feel and multiplayer movement-authority validation.
- `풍요` needs a full block / Fortune / Silk Touch / harvest / mob / fishing / external-mod drop matrix test.
- previous resource-harvest effects remain build/boot verified but not live-player tested.
- active input remains a functional baseline; `Left Alt + 1..0` may also switch the vanilla hotbar until final input UX is implemented.
- `약점 표식` multiplayer ownership still needs a multi-owner-safe representation.
- full Chrono Dawn / World Bosses / MCEndgame boss identity adapters remain incomplete for boss-targeted control/damage and loot-exclusion effects.
- projectile and movement shared runtimes remain build/boot verified, not live-playtested for feel and matrix interactions.
- save-wide threat high-water state exists but final live hostile stat application remains incomplete.
- final device/active/combat/resource VFX, sound, models, HUD and UI visual polish are not done.
- compilation still reports the pre-existing `ProjectileAugmentService` deprecated-API warning; it is not a #50 failure and is not claimed resolved.
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

Next large system batch: **save-wide threat live hostile scaling + elite/boss application boundary**.

Primary goals:

- wire the already-persistent save-wide threat high-water state into real hostile spawn/stat application instead of leaving it as stored progression only;
- apply the canonical stage table consistently to ordinary hostiles and the reduced elite/boss coefficients without multiplying external boss stats repeatedly on reload/rejoin;
- establish stable baseline-vs-applied modifiers so entities are not re-scaled geometrically when chunks reload or threat rises;
- keep damage, health, movement, armor and knockback resistance server-authoritative;
- add/extend data-driven external profile adapters for Chrono Dawn / World Bosses / MCEndgame where exact boss/elite identity can be resolved safely;
- avoid global per-tick entity scans: apply on spawn/load and on actual threat-stage advancement using bounded tracked entities or one-time reconciliation;
- preserve external mod AI, skills, loot and boss phases rather than replacing their entity logic.

After threat application, choose the next roster bulk boundary from remaining protection/reactive/status combat, collision-heavy movement physics, companion/entity systems or higher-tier unique mechanics whose contracts are sufficiently fixed.

Do not implement `손재주` until its non-combat gathering whitelist is actually fixed in canon.

## Validation labels

- CODE REVIEWED: YES
- BUILD VERIFIED: YES — `fb7c963a93ef721810512d3d4510195f1ae5fd1f`
- JAR PRODUCED: YES — Actions run `40081679446`
- PACK DEPENDENCY INSPECTION: PASS — 23 pinned external artifacts
- PACK RUNTIME CLASSPATH: PASS — 150 files
- PACK SERVER SMOKE: PASS
- PACK CLIENT SMOKE: PASS
- CANONICAL AUGMENT CATALOG INITIALIZATION: PASS — 146 definitions
- STAT CONTRACT INITIALIZATION: PASS — 7 definitions / 7 modifier groups
- SURVIVAL CONTRACT INITIALIZATION: PASS
- RESOURCE-HARVEST CONTRACT INITIALIZATION: PASS
- CONSUMPTION/FISHING CONTRACT INITIALIZATION: PASS
- COMBAT/PROTECTION/HEAL/DEATH CONTRACT INITIALIZATION: PASS
- EXPANSION CONTRACT INITIALIZATION: PASS
- MOVEMENT CONTRACT INITIALIZATION: PASS
- PROJECTILE CONTRACT INITIALIZATION: PASS
- CONSUMPTION/FISHING/BREEDING LIVE-PLAYER TESTED: NO
- FOOD USE LIVE-PLAYER TESTED: NO
- RESERVE MEAL LIVE TESTED: NO
- FISHING TIMING LIVE TESTED: NO
- TREASURE CHANCE LIVE TESTED: NO
- TWIN BREEDING LIVE TESTED: NO
- EAT-WHILE-WALKING FEEL LIVE TESTED: NO
- ABUNDANCE DROP MATRIX LIVE TESTED: NO
- RESOURCE-HARVEST LIVE-PLAYER TESTED: NO
- FARMER'S DELIGHT HARVEST/FOOD COMPATIBILITY TESTED: NO
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

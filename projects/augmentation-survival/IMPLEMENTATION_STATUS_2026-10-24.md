# Augmentation Survival — Implementation Status

Date: 2026-10-24
Status: M1C FUNCTIONAL BASELINE / SHARED GAMEPLAY RUNTIMES + SAVE-WIDE THREAT HOSTILE SCALING IMPLEMENTED / EXTERNAL BOSS ADAPTER BULK NEXT

## 146-roster implementation count

The count below measures whether the canonical gameplay effect has a runtime implementation. It does **not** mean final VFX/UI polish or live playtesting is complete.

- Fully implemented gameplay runtime: **82 / 146**
- Partially implemented: **4 / 146** — `회복 증폭`, `전리품 챙기기`, `보물 낚시`, `풍요`
- Not fully implemented: **64 / 146**
- No gameplay runtime yet: **60 / 146**
- **This threat-system batch changes no augment roster count.**

The previous consumption/fishing batch completed the canonical runtime baseline for `든든한 한 끼`, `낚시 감각`, `빨리 먹기`, `쌍둥이`, `먹으면서 걷기`, `비축 식사`. `보물 낚시` and `풍요` retain functional partial baselines. The current batch activates the already-designed save-wide threat curve against actual hostile entities rather than adding a new augment.

## Verified code checkpoint

- Verified gameplay-code checkpoint: `913224aa2b3f57b57398b18b9984dccc51b40478`
- Main threat feature commit: `4131ebc19a51bbfaa5599451d22efaff080c103c` — `feat(augmentation-survival): apply save-wide hostile threat scaling`
- 26.2 API/correctness correction: `913224aa2b3f57b57398b18b9984dccc51b40478` — `fix(augmentation-survival): align threat runtime with 26.2 server API`
- GitHub Actions: `40084458272` / Build Augmentation Survival Pack #52
- Build and pinned-pack inspection: PASS
- Pack dependency inspection: PASS — 23 pinned external artifacts
- Runtime classpath verification: PASS — 150 files
- Dedicated server smoke: PASS — 92 mods
- Client boot smoke: PASS — 108 mods
- Verification artifacts upload: PASS
- Artifact: `augmentation-survival-pack-smoke-52`
- Artifact ID: `12556463231`
- Artifact ZIP SHA-256: `36eeff6a26deab0adf5dacef64d60b6da2301747764f2e950287d2bd06bfe680`
- Canonical augment catalog initialization: PASS — 146 definitions
- Enemy scaling contract initialization: PASS — `Loaded enemy scaling contract: 9 stages, 4 exact profiles, 0 namespace profiles`
- Stat contract initialization: PASS — 7 definitions / 7 modifier groups
- Survival contract initialization: PASS
- Resource-harvest contract initialization: PASS
- Consumption/fishing contract initialization: PASS
- Combat / expansion / movement / projectile / core contracts initialization: PASS

Build #51 (`40084209926`) exposed one real compile failure: Minecraft 26.2 does not provide `ServerPlayer.getServer()`. The follow-up commit obtains the server through the player's `ServerLevel`, and also incorporated two code-review corrections before revalidation: delayed installments bypass threat damage amplification because they already contain reserved post-mitigation damage, and ordinary/elite hostiles no longer receive persistent threat attachments. Build #52 then passed compile/build and both full-pack smoke tests. The failed run was not blindly rerun.

The full-pack smoke still reports known external/harness warnings: optional Naturalist FieldGuide mixins, missing Linux narrator `flite`, no CI OpenAL device, World Bosses texture-reference warnings, and the smoke server's initially absent `server.properties`. None prevented the server from reaching ready state or the client from remaining healthy through the smoke timeout. Compilation also still reports the pre-existing `ProjectileAugmentService` deprecated-API warning.

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

- `풍작` — mature crop harvest has +8% per level chance to create one additional primary crop output
- `씨앗 챙기기` — mature crop harvest has +15% per level chance to create one additional planting item
- `한 조각 더` — conventional ore break has +5% per level chance to create one additional ordinary primary drop; augment cores are excluded
- `다시 심기` — after mature crop destruction, waits one server tick for normal crop drops, consumes one nearby matching planting item, and restores the age-0 crop if the position is still empty
- `싹쓸이 수확` — destroys up to 2 / 4 / 8 additional nearby mature same-crop blocks through the normal server block-destruction path and forces canonical replanting on derived crops
- `나무 베기` — bounded connected-log traversal destroys up to 3 / 6 / 10 additional log-tag blocks
- `굴파기` — destroys up to 1 / 2 / 3 matching blocks behind a valid root break; derived durability is reconciled to 50% effective cost
- `전리품 챙기기` — direct player normal-mob kills have a 5% / 10% / 15% ordinary bundle-duplication baseline; remains **PARTIAL** for external-boss and crowded-drop provenance

The harvest feature was first pushed as `df9dea84098d37bffb3bf309176dbd9de08eda6a`. Build #48 (`40079556176`) exposed only two 26.2 API differences. `8b8b2974726a6a60ea05fb3a898ac705ba4c913f` corrected those boundaries and Build #49 passed both full-pack smoke tests.

## Consumption / fishing / breeding bulk batch retained — 8 augments touched

`consumption_fishing_contract.json` is the shared tuning source. Authority-sensitive results stay on the server. A small clientbound prediction snapshot carries only values required for local food-use timing and input feel.

- `든든한 한 끼` — +8% / +16% / +24% extra saturation relative to the normal food saturation gain baseline
- `빨리 먹기` — food-use duration -5% per level, up to -25%, synchronized for server and local prediction
- `먹으면서 걷기` — eases the actual vanilla consumable slowdown 33% / 66% / 100% toward normal movement
- `비축 식사` — stores overflow nutrition at 4 / 8 / 12 hunger points and consumes reserve before visible hunger falls; no parallel fake hunger model
- `낚시 감각` — accelerates the actual vanilla lure-wait countdown by the canonical 10% / 20% / 30%
- `보물 낚시` — +1 / +2 / +3 vanilla luck-equivalent baseline; **PARTIAL** because this is not mathematically identical to canonical +2 / +4 / +6 percentage points
- `쌍둥이` — after normal breeding finalization, 10% / 20% / 30% chance for one additional offspring through the parents' normal offspring factory
- `풍요` — final ordinary-drop duplication baseline for block breaks, normal player-attributed mob kills and fishing; **PARTIAL** because final source provenance and unique/progression exclusions are not yet comprehensive

## New save-wide hostile threat scaling runtime

`enemy_scaling_contract.json` is now runtime-active and is parsed into a dedicated threat profile catalog that is deliberately separate from core-reward boss classification.

### Save-wide progression application

- player augment-level mutations recompute canonical `AugmentCatalog.powerScore(...)` and feed the existing save-wide high-water state
- threat remains one shared save value across dimensions and players and never automatically decreases
- when a stage rises, loaded scalable entities are reconciled once across loaded server levels; there is no per-tick global entity scan
- newly loaded/spawned scalable entities reconcile when they enter a `ServerLevel`
- max-health changes preserve the entity's current health ratio instead of healing or injuring it as a side effect of stage advancement

### Normal hostile scaling

Ordinary entities implementing vanilla `Enemy` default to the normal threat group unless an exact/namespace profile overrides them.

Stage 8 remains canonical:
- max health x12.00
- outgoing damage x2.70
- movement speed x1.175
- armor +12
- knockback resistance +0.20

Health and movement use stable transient multiplicative attribute modifiers; armor and knockback resistance use stable transient additive modifiers. Outgoing hostile damage is applied once at the shared server pre-mitigation `DamageSource` boundary before player armor/protection. `DELAYED_DAMAGE` explicitly bypasses this multiplier so reserved delayed installments are not amplified twice.

### Elite scaling

- generic elite formula uses 65% of the **extra** normal scaling, not 65% of the final total multiplier
- initial exact elite profiles: `minecraft:warden`, `minecraft:elder_guardian`
- elite entities use current save stage like normal hostiles and refresh on actual stage increases

### Boss scaling and encounter snapshot

- generic boss health uses 35% of the normal extra health scaling
- generic boss damage currently uses the canonical midpoint, 30% of normal extra damage scaling, inside the allowed 25–35% band
- generic boss speed/armor/knockback scaling is disabled; boss-specific profiles/adapters own those decisions
- initial exact boss profiles: `minecraft:wither`, `minecraft:ender_dragon`
- a recognized boss persists only its encounter threat-stage snapshot; later save-wide stage increases do not rescale that active boss encounter
- ordinary/elite hostiles do **not** receive persistent threat attachments, avoiding save churn across every mob

### Classification boundary

Threat difficulty classification does not reuse `BossCoreRewardCatalog` / reward-band assignment. This prevents a scripted external boss that lacks a reward assignment from silently receiving the ordinary hostile x12 health curve. External scripted bosses must gain explicit threat profiles/adapters before their boss scaling is claimed complete.

Current threat profile catalog at the verified checkpoint:
- 9 stages
- 4 exact profiles
- 0 namespace profiles
- default vanilla `Enemy` -> NORMAL
- non-`Enemy` without explicit profile -> EXCLUDED

## Deliberately not claimed complete

- Full roster gameplay completion is **82 / 146**, not 146 / 146.
- `회복 증폭` remains partial because amplification still covers routed augment-generated non-natural healing rather than every external non-natural heal.
- `전리품 챙기기` remains partial for provenance/external-boss coverage.
- `보물 낚시` remains partial because vanilla luck-equivalent adjustment is not the exact fixed percentage-point contract.
- `풍요` remains partial because exact final-output provenance and comprehensive unique/progression exclusions are incomplete.
- `손재주` is not implemented because its non-combat gathering whitelist is not fixed; no arbitrary whitelist was invented.
- `한 마리 더` remains a held/unconfirmed proposal and is not counted in the locked 146 roster.
- threat scaling is build/full-pack boot verified but **not live-player gameplay tested**.
- threat stage advancement with actual loaded mobs has not been live-tested.
- normal/elite stage numeric values have not been measured in a running player session.
- Wither/Ender Dragon boss snapshot behavior has not been live-tested.
- external Chrono Dawn / World Bosses / MCEndgame scripted bosses are not yet explicit threat profiles and must not be claimed boss-scaled.
- custom/scripted damage without a living dealer can require an external adapter before threat damage scaling is guaranteed.
- no threat-stage HUD/presentation is implemented.
- consumption/fishing/breeding/abundance effects remain build/boot verified but not live-player tested.
- Farmer's Delight crop/food/consumable compatibility needs live gameplay validation.
- active input remains a functional baseline; `Left Alt + 1..0` may also switch the vanilla hotbar until final input UX is implemented.
- `약점 표식` multiplayer ownership still needs a multi-owner-safe representation.
- projectile and movement shared runtimes remain build/boot verified, not live-playtested for feel and matrix interactions.
- final device/active/combat/resource/threat VFX, sound, models, HUD and UI visual polish are not done.
- compilation still reports the pre-existing `ProjectileAugmentService` deprecated-API warning; it is not a #52 failure and is not claimed resolved.
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

Next large system batch: **external boss threat/control/damage identity adapters + boss-profile mapping**.

Primary goals:
- inspect the actual entity IDs and boss mechanics shipped by Chrono Dawn, World Bosses and MCEndgame instead of guessing namespace-wide boss status;
- bind every safely identifiable scripted boss to explicit BOSS threat profiles and preserve each mod's phases, AI and custom health logic;
- unify boss identity use for threat scaling, boss-targeted control restrictions, boss-hunt damage and ordinary-loot duplication exclusions where the semantics match, without coupling reward probability to difficulty classification;
- add adapters for scripted damage sources that bypass a living dealer where needed;
- keep external ordinary enemies on normal scaling only when they satisfy the normal hostile contract;
- do not use a broad namespace=BOSS shortcut that would classify ordinary mod mobs as bosses.

After that adapter pass, choose the next large remaining roster subsystem from reactive/status combat, collision-heavy movement physics, companion/entity systems, or higher-tier unique mechanics whose contracts are fixed.

## Validation labels

- CODE REVIEWED: YES
- BUILD VERIFIED: YES — `913224aa2b3f57b57398b18b9984dccc51b40478`
- JAR PRODUCED: YES — Actions run `40084458272`
- PACK DEPENDENCY INSPECTION: PASS — 23 pinned external artifacts
- PACK RUNTIME CLASSPATH: PASS — 150 files
- PACK SERVER SMOKE: PASS — 92 mods
- PACK CLIENT SMOKE: PASS — 108 mods
- CANONICAL AUGMENT CATALOG INITIALIZATION: PASS — 146 definitions
- ENEMY SCALING CONTRACT INITIALIZATION: PASS — 9 stages / 4 exact profiles / 0 namespace profiles
- SAVE-WIDE THREAT HOSTILE RUNTIME: BUILD/BOOT VERIFIED
- THREAT LIVE-PLAYER TESTED: NO
- THREAT STAGE-RISE LOADED-MOB REFRESH LIVE TESTED: NO
- BOSS THREAT SNAPSHOT LIVE TESTED: NO
- EXTERNAL BOSS THREAT ADAPTERS: INCOMPLETE
- STAT CONTRACT INITIALIZATION: PASS — 7 definitions / 7 modifier groups
- SURVIVAL CONTRACT INITIALIZATION: PASS
- RESOURCE-HARVEST CONTRACT INITIALIZATION: PASS
- CONSUMPTION/FISHING CONTRACT INITIALIZATION: PASS
- COMBAT/PROTECTION/HEAL/DEATH CONTRACT INITIALIZATION: PASS
- EXPANSION CONTRACT INITIALIZATION: PASS
- MOVEMENT CONTRACT INITIALIZATION: PASS
- PROJECTILE CONTRACT INITIALIZATION: PASS
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
- EXTERNAL BOSS GAMEPLAY TESTED: NO
- MULTIPLAYER TESTED: NO

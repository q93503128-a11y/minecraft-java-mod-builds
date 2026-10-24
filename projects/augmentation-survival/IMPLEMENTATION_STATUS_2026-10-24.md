# Augmentation Survival — Implementation Status

Date: 2026-10-24
Status: M1C FUNCTIONAL BASELINE / PROJECTILE + ACTIVE/COMBAT EXPANSION RUNTIMES IMPLEMENTED / NEXT BULK SYSTEM BATCH

## Verified code checkpoint

- Verified gameplay-code checkpoint: `960bc9739ac92989499f6d177438aec730503ae4`
- Main active/combat expansion feature commit: `6f29bb5a40c7dc1073a32653eaa6b11a4d8e9c90` — `feat(augmentation-survival): expand active and combat runtime`
- 26.2 API adaptation: `960bc9739ac92989499f6d177438aec730503ae4` — `fix(augmentation-survival): align expansion runtime with 26.2 API`
- GitHub Actions: `40076587240` / Build Augmentation Survival Pack #46
- Build and pinned-pack inspection: PASS
- Pack dependency inspection: PASS — 23 pinned external artifacts
- Runtime classpath verification: PASS — 150 files
- Dedicated server smoke: PASS
- Client boot smoke: PASS
- Verification artifacts upload: PASS
- Artifact: `augmentation-survival-pack-smoke-46`
- Artifact ID: `12552963409`
- Artifact ZIP SHA-256: `940bf01dca404c463dd54a6f1a88caeb0535152f6b5d4743bb471cf910ff8d04`
- Runtime initialization marker: `Loaded expansion augment contract: active engine and broad combat batch`

The documentation checkpoint after this code commit does not change Java gameplay code and must not be treated as a new gameplay verification target.

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

### Existing stat / combat / protection / healing / lethal runtime

- unconditional stat contract: `강건함`, `속공`, `빠른 발`, `버티기`
- combat provenance and ROOT/derived recursion boundaries
- `날 선 감각`, `정확한 조준`, `질긴 피부`, `가벼운 착지`, `튼튼한 폐`, `마지막 힘`
- timed `연속 처치`, `다급한 발걸음`
- `흡혈`, partial `회복 증폭`, `넘치는 회복`, `회복 나눔`
- `피해 적응`, `피해 지연`
- source-owned shield and augment-absorption pools
- lethal order: `한 번 버티기` -> vanilla held Totem -> `비상 토템` -> `재림`

### Existing movement / aerial runtime

- `숨 고르기`
- `야행성` movement facet
- `햇빛 체질` movement facet
- `성큼걸음`
- `멀리뛰기`
- `관성 전환`
- `두 번 점프`
- `공중 연계`

### Existing projectile shot-action / flight-segment runtime

- `화살 아끼기`
- `관통 화살`
- `연쇄 화살`
- `분산 사격`
- `추적 화살`
- `도탄`
- per-arrow identity / ownership / provenance / segment state
- ROOT physical arrow, derived scatter projectile, derived chain and reflected-arrow provenance separation
- vanilla Piercing + augment piercing combination
- ricochet opens a new flight segment and resets segment-only hit identity
- no global projectile tick map

## New broad active/combat expansion batch

This batch intentionally implements many compatible augments at once instead of returning to one-augment-at-a-time work. `expansion_contract.json` is the data-driven tuning source for the shared active and broad combat batch.

### Common active-ability engine

- client requests only a slot index; ownership, cooldown/charge, targeting and gameplay results are server-authoritative
- default input baseline: hold `증강 스킬 모드` (`Left Alt`) and press `1..0` for active slots 1..10
- existing persistent `activeSlots` are honored when configured
- until the final active-slot management UI exists, owned supported actives are auto-filled as a functional fallback
- only active timers/replays that actually exist are tracked; no global world/entity scan
- current input baseline does not suppress ordinary hotbar switching while the modifier key is held; final input UX is pending

### Six supported active abilities

- `정화` (`cleanse`) — removes currently active harmful effects; 45-second cooldown; no effect means no cooldown consumption
- `끌어당기기` (`pull`) — pulls hostile entities within 6 / 8 / 10 blocks; 10 / 8 / 6-second cooldown; exact configured bosses receive 30% pull strength
- `자리 바꾸기` (`swap`) — swaps position with a visible aimed non-player living target within 24 blocks; 8-second cooldown; fall distance reset
- `보호막` (`shield`) — existing source-owned shield pool is now player-activatable; 8 / 12 / 16 HP for 8 seconds; 24 / 20 / 16-second cooldown
- `광폭화` (`berserk`) — 8 seconds; ROOT direct damage +30% / +45% / +60%, attack speed +20% / +30% / +40%, incoming damage +25%; 30 / 27 / 24-second cooldown
- `치유 파동` (`healing_wave`) — 8-block self/ally/tamed-animal heal for 20% / 30% / 40% max HP; 30 / 25 / 20-second cooldown; self-heal uses the common augment-healing route

### Active meta augments

- `능력 가속` (`ability_haste`) — current supported non-mythic active cooldowns are reduced by 25%
- `이중 충전` (`double_charge`) — supported cooldown actives receive two stored charges with sequential persistent recharge queues
- `재연` (`replay`) — compatible supported actives repeat after 30 ticks at 50% efficiency without consuming another charge/cooldown; current compatible set is pull/shield/berserk/healing-wave
- replay does not recursively schedule another replay
- cleanse and swap are deliberately excluded from replay because a canonical 50%-efficiency interpretation is not meaningful/safe

### Broad combat passives

- `전투 순환` (`combat_cycle`) — ROOT direct hostile kills reduce supported active cooldowns by 1 / 2 / 3 seconds; exact configured boss ROOT kills use 5x reduction
- `연쇄 폭발` (`chain_explosion`) — hostile kills deal derived AoE damage around the killed hostile; 2.5 / 3 / 3.5-block radius; 20% / 30% / 40% of dead target max HP, capped at 12 / 18 / 24 damage
- `벼락` (`lightning`) — every 4 / 3 / 2 ROOT direct hits chains to up to 2 / 3 / 4 additional hostiles within 7 blocks for 50% / 65% / 80% of the triggering actual damage
- `메아리 타격` (`echo_strike`) — same-target fully charged ROOT melee counter; every 3 / 3 / 2 hits repeats 50% / 75% / 100% of the baseline event damage using ECHO provenance
- `약점 표식` (`weakness_mark`) — 3 ROOT direct hits within 5 seconds mark the target for 6 seconds; owner/allied damage receives final +8% / +12% / +16%
- `속도 전환` (`speed_conversion`) — positive movement-speed bonus converts into ROOT direct damage, +0.4% / +0.6% / +0.8% damage per +1% movement, capped at +20% / +30% / +40%; basic sprint contribution is excluded
- `육중한 일격` (`heavy_strike`) — fully charged ROOT melee, 4-second internal cooldown, adds normal derived damage equal to 5% / 7.5% / 10% of player max HP
- `생명 파동` (`life_wave`) — primary non-natural actual HP healing damages nearby hostiles within 4 blocks for healed HP x1 / x1.5 / x2, capped per target at 10 / 15 / 20
- `난전` (`brawl`) — counts nearby hostiles within 8 blocks, capped at five; per hostile ROOT direct damage +3% / +5% / +7% and incoming damage reduction 2% / 3% / 4%
- `유리 대포` (`glass_cannon`) — max-health multiplier 60% / 80% / 100% with ROOT direct damage +60% / +80% / +100%; transient max-health modifier is reconciled with persistent augment state
- `우두머리 사냥` (`boss_hunter`) — current exact configured boss identities receive ROOT direct damage +8% / +16% / +24%

### Recursion / authority rules in this batch

- `벼락`, `메아리 타격`, `육중한 일격`, `생명 파동`, `연쇄 폭발` use derived provenance and do not increment ROOT-only hit/counter systems
- `전투 순환` requires ROOT direct kill provenance
- `생명 파동` is triggered from primary actual healing only; shared heals do not recursively create another life wave
- `치유 파동` intentionally synergizes with the existing primary-heal pipeline
- all active execution/cooldown/charge decisions are server-owned

## Deliberately not claimed complete

- The full 146-roster gameplay runtime is not complete.
- Active input is functional/boot-verified but not live-tested; `Left Alt + 1..0` may also change the vanilla hotbar slot until final input UX is implemented.
- final active-slot assignment/management UI is not implemented; current auto-fill is a functional fallback only.
- `능력 가속`, `이중 충전`, `재연`, `전투 순환` currently cover the six actives registered in this engine. Future active augments still need registration.
- acquiring `이중 충전` while a legacy single-charge cooldown is already running has no separately validated migration behavior yet.
- `자리 바꾸기` does not yet have a scripted-external-boss teleport exclusion adapter.
- `끌어당기기` boss scaling and `우두머리 사냥` currently recognize only exact boss identities already configured by `BossCoreRewardCatalog`; full Chrono Dawn / World Bosses / MCEndgame boss coverage remains an adapter task.
- `정화` uses harmful-effect category removal as the baseline; external/system effects that should be explicitly unremovable need adapters.
- `치유 파동` / `회복 나눔` do not yet include future `지배` companions.
- `연쇄 폭발` and `벼락` currently implement gameplay damage behavior, not final explosion/lightning VFX/sound/readability.
- `약점 표식` currently stores one active owner mark per target. A second marker can overwrite the previous owner's mark; multiplayer mark ownership must be corrected before multiplayer-ready status.
- `속도 전환` is based on the current movement attribute ratio and subtracts the ordinary sprint contribution; unusual external movement modifiers need live compatibility tuning.
- Better Combat fully charged melee cadence for `메아리 타격` and `육중한 일격` is not live-tested.
- `유리 대포` interaction with external max-health modifiers is not live-tested.
- `회복 증폭` remains limited to healing routed through the augment-healing service, not every external non-natural heal.
- `야행성` mining-speed facet and `햇빛 체질` natural-regeneration facet remain pending.
- collision/medium-heavy mechanics such as `발 안 묶이기`, `물살 타기`, `매달리기`, `탄성 저장`, `물 위 달리기` remain separate physics work.
- `탄성 저장` still lacks a canonically specified stored-distance-to-propulsion mapping; no arbitrary conversion has been invented.
- projectile runtime remains build/boot verified but not live-playtested for bounce geometry, tracking feel, chain/scatter behavior, ammo saving, Piercing/Multishot combinations or segment re-hit rules.
- modded projectile classes that do not inherit `AbstractArrow` are not automatically covered; `ThrownTrident` remains intentionally excluded from arrow augmentation behavior.
- compilation still reports the pre-existing deprecation warning in `ProjectileAugmentService`; it is not a build/smoke failure and is not claimed resolved.
- save-wide threat high-water state exists but final live hostile stat application is incomplete.
- final device model/texture/VFX/sound, final active/combat VFX and final UI/HUD pass are not done.
- CI proves compile/build, contract initialization and full-pack server/client boot. It does not prove live combat numbers, input feel, cooldown timing or multiplayer correctness.

## Implementation batching rule from this checkpoint

The user explicitly requested faster, larger batches. Do not return to one-augment-at-a-time implementation except for isolated bugs or genuinely unique mechanics.

For each next unit:

1. inventory the whole remaining 146-roster against the runtime boundaries that already exist;
2. implement every easy or medium augment that safely reuses those boundaries in the same batch;
3. add a shared subsystem only when the remaining mechanics genuinely require one;
4. validate once after the meaningful bulk unit;
5. leave only physics/world-generation/companion/UI mechanics whose semantics really require dedicated systems.

## Next implementation direction

Next work should again be a **bulk system batch**, chosen by how many remaining canonical augments can safely reuse one boundary. High-density candidates include:

- remaining protection/reactive/status/combat effects that can reuse current damage/heal/provenance state;
- resource/mining/farming/survival augments on shared block-break/loot/food/environment boundaries;
- save-wide threat live hostile scaling;
- external boss adapters needed by boss-targeted combat/control augments;
- collision/medium-heavy movement mechanics as their own physics unit.

Do not spend a full batch on one easy augment when the current framework can safely connect many.

## Validation labels

- CODE REVIEWED: YES
- BUILD VERIFIED: YES — `960bc9739ac92989499f6d177438aec730503ae4`
- JAR PRODUCED: YES — Actions run `40076587240`
- PACK DEPENDENCY INSPECTION: PASS — 23 pinned external artifacts
- PACK RUNTIME CLASSPATH: PASS — 150 files
- PACK SERVER SMOKE: PASS
- PACK CLIENT SMOKE: PASS
- STAT CONTRACT INITIALIZATION: PASS — 4 definitions / 4 modifier groups
- COMBAT/PROTECTION/HEAL/DEATH CONTRACT INITIALIZATION: PASS
- MOVEMENT CONTRACT INITIALIZATION: PASS
- PROJECTILE CONTRACT INITIALIZATION: PASS
- EXPANSION CONTRACT INITIALIZATION: PASS
- ACTIVE ENGINE LIVE-PLAYER TESTED: NO
- ACTIVE INPUT `Left Alt + 1..0` LIVE TESTED: NO
- ACTIVE COOLDOWN/DOUBLE-CHARGE LIVE TESTED: NO
- REPLAY LIVE TESTED: NO
- BROAD COMBAT EXPANSION LIVE TESTED: NO
- PROJECTILE LIVE-PLAYER TESTED: NO
- MOVEMENT LIVE-PLAYER TESTED: NO
- DOUBLE-JUMP INPUT/SYNC LIVE TESTED: NO
- AERIAL-COMBO LIVE TESTED: NO
- PROTECTION PIPELINE LIVE-PLAYER TESTED: NO
- HEALING PIPELINE LIVE-PLAYER TESTED: NO
- LETHAL CHAIN LIVE-PLAYER TESTED: NO
- STAT AUGMENT LIVE-PLAYER TESTED: NO
- STAT RECONNECT/DEATH TESTED: NO
- DEVICE FLOW PLAYTESTED: NO
- UI VISUAL REVIEW: NO
- BETTER COMBAT GAMEPLAY TESTED: NO
- EXTERNAL BOSS GAMEPLAY TESTED: NO
- MULTIPLAYER TESTED: NO

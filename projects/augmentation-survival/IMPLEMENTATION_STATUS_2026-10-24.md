# Augmentation Survival — Implementation Status

Date: 2026-10-24
Status: M1C FUNCTIONAL BASELINE COMPLETE / PROJECTILE SHOT-ACTION+FLIGHT-SEGMENT RUNTIME IMPLEMENTED / COMMON ACTIVE-ABILITY ENGINE NEXT

## Verified code checkpoint

- Verified gameplay-code checkpoint: `316f92a67ba7b7a5ecd5651e2fe122d5ac54f05b`
- Main projectile feature commit: `7ed812cbc3fd4f73dfe1e2b3bd58790db68b37cf` — `feat(augmentation-survival): build projectile flight segment runtime`
- 26.2 API adaptation: `316f92a67ba7b7a5ecd5651e2fe122d5ac54f05b` — `fix(augmentation-survival): use 26.2 direction vector API`
- GitHub Actions: `40074027578` / Build Augmentation Survival Pack #44
- Build and pinned-pack inspection: PASS
- Pack dependency inspection: PASS — 23 pinned external artifacts
- Runtime classpath verification: PASS — 150 files
- Dedicated server smoke: PASS
- Client boot smoke: PASS
- Verification artifacts upload: PASS
- Artifact: `augmentation-survival-pack-smoke-44`
- Artifact ID: `12552780881`
- Artifact ZIP SHA-256: `0463311e3211f75e27ba2c99479d3a399cfcc54c01536a4dc5cca437bfce6ba3`
- Runtime initialization marker: `Loaded projectile augment contract: shared shot-action and flight-segment runtime`

The documentation checkpoint after this code commit does not change Java gameplay code and must not be treated as a new gameplay verification target.

## Implemented now

### Runtime/state foundation

- persistent server-owned player augment state
- augment levels stored by stable namespaced ID
- state survives death through persistent `copyOnDeath` attachment semantics
- persistent per-augment cooldown-ready ticks reuse the existing player-state save schema
- save-wide threat high-water state
- 146-entry augment catalog loader and strict count/max-level validation
- canonical tier power weights feed threat high-water observation after augment acquisition

### Core economy

- six augment core items
- common core crafting recipe
- data-driven core distribution contract loader
- data-driven source profile resolver
- hostile/special/elite player-attributed kill rolls
- Warden / Elder Guardian event-profile rewards
- raid victory reward path
- treasure-fishing core roll
- natural structure/container core reward path
- vanilla boss reward bands
- persistent per-player boss first-kill bonus state
- original rewards remain separate from progression-core rewards
- Looting/Fortune/general drop multipliers do not multiply progression cores

### Augment device loop

- physical `증강 장치` workstation
- physical `강화 증강 장치` workstation
- server creates up to three valid same-tier candidates
- max-level augments are removed from the candidate pool
- if a tier is fully maxed, a draw cannot start and no core is consumed
- opening/cancelling an offer does not consume a core
- opaque server offer token validates the final transaction
- one core is consumed only after final selection validates
- `두 번째 선택` commits two distinct offered augments in one transaction while consuming one core
- successful selection persists the new level and updates save-wide threat high-water
- advanced workstation supports 3:1 upward conversion, one or bulk

### Functional client baseline

- custom card-selection screen instead of a vanilla button list
- six tier selectors and three candidate cards
- cards show canonical Korean name, tier, active tag, current-to-next level and max level
- advanced device has separate `증강 선택` / `코어 합성` modes
- current UI is functional baseline, not final visual-quality pass

### Unconditional stat augment runtime

- `stat_modifier_contract.json` is the data-driven source for unconditional player attributes
- persistent augment levels are source of truth; vanilla attribute modifiers are transient derived state
- shared attribute/operation definitions are summed into deterministic modifiers
- reconciliation occurs on state replacement, join and respawn; no per-tick global scan
- `강건함` — +2 maximum health per level, max +10
- `속공` — +4% attack speed per level, max +20%
- `빠른 발` — +3% movement speed per level, max +15%
- `버티기` — +10% knockback resistance per level, max +50%
- verified initialization: 4 definitions / 4 modifier groups

### Shared combat provenance / damage runtime

- canonical provenance covers ROOT melee/projectile and derived projectile/chain/AOE/echo/path-echo/active/replay/authority/companion/reflected/special-percent-HP/delayed damage
- scoped server-thread provenance overrides block accidental ROOT recursion
- ROOT melee/projectile outgoing bonuses occur before vanilla armor/Protection
- augment incoming reductions occur after armor/Protection and before protection pools/absorption/HP
- `날 선 감각` — ROOT melee damage +4% per level, max +20%
- `정확한 조준` — ROOT projectile damage +4% per level, max +20%
- `질긴 피부` — general augment damage reduction 2% per level, max 10%
- `가벼운 착지` — fall reduction 12% per level, max 60%
- `튼튼한 폐` — drowning reduction 15% per level, max 45%
- `마지막 힘` — at 30% HP or lower, ROOT outgoing damage +10% per level, max +30%
- `연속 처치` — hostile kill grants up to 3 refreshed 4-second movement/attack-speed stacks
- `다급한 발걸음` — real incoming damage grants 3-second movement-speed window

### Protection / delayed-damage runtime

- `ProtectionAugmentService` owns post-mitigation protection state without global entity scans
- current order: ordinary augment reduction -> damage adaptation -> delayed split -> augment shield -> augment absorption -> vanilla absorption -> HP -> lethal protection
- source-owned `SHIELD` and augment `ABSORPTION` remain separate identities
- `피해 적응` — same exact damage-type family reduction 10% / 20% / 30% for 6 seconds
- `피해 지연` — reserves 20% / 30% / 40% and replays five installments every 2 seconds over 10 seconds
- delayed installments use explicit `DELAYED_DAMAGE` and do not re-run armor/Protection/delay splitting
- `보호막` pool/tuning/API exists; active trigger waits for the common active-ability engine

### Healing runtime

- augment-generated primary healing has one server-owned entry point
- routed order: `회복 증폭` -> actual HP gain -> `넘치는 회복` -> `회복 나눔`
- `흡혈` — direct server-player hostile kill heals 1 / 2 / 3 HP through common heal path
- `회복 증폭` — +10% / +20% / +30% on routed augment-generated non-natural healing
- `넘치는 회복` — excess routed healing becomes augment absorption capped at 4 / 8 / 12 HP
- `회복 나눔` — shares 15% / 30% / 45% of actual restored HP within 8 blocks to allies/player-owned tamed animals
- share recursion and overflow recursion are blocked

### Lethal-protection chain

- common order: `한 번 버티기` -> vanilla held Totem -> `비상 토템` inventory totem -> `재림`
- `한 번 버티기` — lethal damage becomes 1 HP, 1 second normal-damage protection, 180-second persisted cooldown
- `비상 토템` — after held-totem protection fails, consumes one inventory Totem and reproduces vanilla death-protection component/effects/stat/criterion path
- `재림` — 3-second recovery stasis, 180-second cooldown, normal-damage/action suppression, then full maximum health

### Shared movement / aerial runtime

- `movement_modifier_contract.json` owns shared locomotion tuning
- only relevant augment owners are tracked; no global player/entity scan
- conditional movement bonuses share one deterministic movement-speed modifier
- `숨 고르기` — after 60 ticks without sprinting, movement speed +10% / +20% / +30%
- `야행성` — nighttime movement +5% / +10% / +15%; mining facet pending
- `햇빛 체질` — daytime movement +4% / +8% / +12%; natural-regeneration interval facet pending
- `성큼걸음` — normal player step height raised from 0.6 to canonical 1 block
- `멀리뛰기` — sprinting vanilla ground jump multiplies horizontal motion by 1.08 / 1.16 / 1.24
- `관성 전환` — actual fall distance >=4 blocks grants +10% / +20% / +30% movement for 60 ticks on landing
- `두 번 점프` — server-owned one-charge airborne extra jump using vanilla jump-power calculation
- ground/swimmable water/climbable contact recharges the air jump
- client sends only jump intent; server decides eligibility/charge
- `공중 연계` — successful airborne hostile ROOT melee hit restores one air-jump charge

### Projectile shot-action / flight-segment runtime

- new `projectile_modifier_contract.json` owns shared projectile tuning
- physical arrow runtime state is attached directly to each `AbstractArrow`; no global projectile tick map or entity scan exists
- `ThrownTrident` is intentionally excluded from arrow augmentation behavior
- projectile identity records original owner, provenance, vanilla pierce level, augment snapshot, bounce count, current segment hits, tracking target and once-per-projectile chain state
- ROOT physical arrows remain `ROOT_PROJECTILE`; `분산 사격` auxiliaries are `DERIVED_PROJECTILE`; an arrow whose owner changes away from the original player becomes `REFLECTED`
- reflected arrows restore their vanilla pierce budget and stop using player-owned tracking/ricochet/chain behavior
- a solid-block ricochet ends the current flight segment, clears segment hit identity and vanilla pierced-entity memory, restores the combined pierce budget, and begins a new segment
- the same entity is blocked from repeated direct hits within one segment but may be hit again after a ricochet starts a new segment
- vanilla block projectile callbacks and weapon block-hit enchantment effects are preserved when a ricochet replaces normal sticking
- vanilla Piercing level and `관통 화살` levels combine into one physical pierce budget
- `관통 화살` — +1 / +2 / +3 extra target budget; same-segment follow-up damage reduction 10% / 5% / 0% per prior successful direct hit
- `추적 화살` — hostile target acquisition within 12 blocks and front 30° cone; steering capped at 2° / 3° / 4° per tick while preserving projectile speed; a new segment can reacquire targets
- `도탄` — up to 1 / 2 / 3 solid-block ricochets; direct projectile damage gains +20% / +35% / +50% per bounce relative to projectile baseline
- `연쇄 화살` — first valid physical hostile hit starts one lifetime chain sequence of up to 1 / 2 / 3 hops; each hop searches within 5 blocks; first chain uses the triggering hit amount and later hops multiply by 0.75; chain damage is `DERIVED_CHAIN`, not another physical projectile
- `분산 사격` — fully charged bow or arrow-loaded crossbow adds 1 / 2 / 3 intangible auxiliary arrows at 60% projectile baseline damage; spread reaches up to 6° / 12° / 18°; no extra ammo or weapon-durability payment is created by the auxiliaries
- `화살 아끼기` — 10% / 20% / 30% root-ammo non-consume roll for bow/crossbow arrows; Crossbow Multishot still receives one root ammo roll rather than one roll per Multishot projectile
- auxiliary projectiles are spawned directly rather than replaying the shot action, preventing scatter recursion
- each auxiliary physical projectile independently receives its own flight-segment state and can use compatible piercing/tracking/ricochet/chain effects
- full-pack server and client smoke both initialize the projectile contract without our Mixin/Invoker fatal signatures

## Deliberately not claimed complete

- The full 146-roster gameplay runtime is not complete.
- `회복 증폭` is still limited to healing routed through the augment-generated heal service, not every external non-natural heal.
- `흡혈` currently proves direct player hostile-kill attribution; broader owned-derived attribution remains future work.
- `회복 나눔` still needs a future `지배` companion ally adapter.
- `보호막` remains non-activatable until the common active engine exists.
- custom augment protection pools do not yet have final HUD/presentation.
- `재림` strict positional immobility has not been live-playtested.
- `야행성` mining-speed facet and `햇빛 체질` natural-regeneration facet are pending.
- collision/medium-heavy movement mechanics such as `발 안 묶이기`, `물살 타기`, `매달리기`, `탄성 저장`, `물 위 달리기`, `속도 전환` remain separate physics work.
- `탄성 저장` still lacks a canonically specified stored-distance-to-propulsion mapping, so no arbitrary conversion has been invented.
- movement/aerial runtime is build/boot verified but not live-playtested for feel/input/synchronization.
- projectile runtime is build/boot verified but **not** live-playtested for actual bounce geometry, homing feel, target counts, chain targets/damage, ammo consumption, Multishot interaction or segment re-hit behavior.
- modded projectile classes that do not inherit `AbstractArrow` are not automatically covered by this physical-arrow engine.
- `ThrownTrident` is intentionally not treated as an arrow augment projectile.
- projectile VFX/sound/readability polish is not implemented.
- compilation currently reports a deprecation warning in `ProjectileAugmentService`; it does not fail build or smoke, but the deprecated call should be identified during a later API-cleanup pass rather than being described as resolved.
- Common active-ability engine is not implemented yet.
- save-wide threat scaling has state/high-water observation but final live hostile stat application is not complete.
- final device model/texture/VFX/sound and final UI visual pass are not done.
- CI proves compile/build, contract initialization, full-pack dedicated-server boot and full-pack client boot. It does **not** prove live gameplay values or feel.
- Better Combat 3.2.2 co-loads and boots, but actual combat coexistence is not playtested.
- external boss reward gameplay is not playtested.
- multiplayer is not tested.

## Implementation batching rule from this checkpoint

Do not return to one-augment-at-a-time implementation except for isolated bugs or genuinely unique mechanics.

Use shared-system batches:

1. build/reuse a common runtime boundary;
2. connect every already-canonical augment that safely fits that boundary;
3. validate once at the end of the meaningful batch;
4. keep unique/complex mechanics for the next appropriate shared engine rather than hardcoding them independently.

## Next implementation unit

Next large batch: **common active-ability engine**.

Primary anchors:

1. establish one server-authoritative active input / selected-slot / cooldown / charge / validation path instead of giving each active augment a private key handler;
2. wire the already-prepared `보호막` trigger through that engine and connect every other canonical active augment that safely shares the same activation boundary in one batch;
3. keep damage, resource consumption, cooldown completion and world/entity mutation server-authoritative;
4. reserve the normal 10-slot active quick-selection contract and keep `권능 탈취` on its separate dedicated input as already specified by project canon;
5. avoid global per-tick scans; only track active instances/timers that actually exist;
6. do not fake unique mechanics whose targeting/world contracts are not yet sufficiently specified; leave those for the next shared subsystem rather than inventing behavior.

After the active engine, choose the next bulk unit from the remaining canonical mechanics by shared runtime boundary—resource/mining/farming/world interaction, collision-heavy movement physics, save-wide threat application, or other high-density system—rather than returning to isolated augments.

Before treating the existing work as PLAYTESTED, verify a real player across stat application/reconnect/death, Better Combat cadence/damage, protection/healing/lethal order, conditional movement, double jump/aerial recharge, and the full projectile matrix including Piercing, Multishot, ricochet, tracking, chain, scatter and ammo saving.

## Validation labels

- CODE REVIEWED: YES
- BUILD VERIFIED: YES — `316f92a67ba7b7a5ecd5651e2fe122d5ac54f05b`
- JAR PRODUCED: YES — Actions run `40074027578`
- PACK DEPENDENCY INSPECTION: PASS — 23 pinned external artifacts
- PACK RUNTIME CLASSPATH: PASS — 150 files
- PACK SERVER SMOKE: PASS
- PACK CLIENT SMOKE: PASS
- STAT CONTRACT INITIALIZATION: PASS — 4 definitions / 4 modifier groups
- COMBAT/PROTECTION/HEAL/DEATH CONTRACT INITIALIZATION: PASS
- MOVEMENT CONTRACT INITIALIZATION: PASS
- PROJECTILE CONTRACT INITIALIZATION: PASS
- PROJECTILE LIVE-PLAYER TESTED: NO
- PIERCING LIVE VALUES TESTED: NO
- CHAIN TARGETING/DAMAGE LIVE TESTED: NO
- SCATTER SHOT LIVE TESTED: NO
- ARROW SAVER LIVE AMMO TESTED: NO
- TRACKING TURN/REACQUIRE LIVE TESTED: NO
- RICOCHET REFLECTION/SEGMENT LIVE TESTED: NO
- VANILLA PIERCING + AUGMENT COMBINATION LIVE TESTED: NO
- CROSSBOW MULTISHOT + SCATTER LIVE TESTED: NO
- MOVEMENT LIVE-PLAYER TESTED: NO
- DOUBLE-JUMP INPUT/SYNC LIVE TESTED: NO
- AERIAL-COMBO LIVE TESTED: NO
- PROTECTION PIPELINE LIVE-PLAYER TESTED: NO
- HEALING PIPELINE LIVE-PLAYER TESTED: NO
- LETHAL CHAIN LIVE-PLAYER TESTED: NO
- COMBAT AUGMENT LIVE-PLAYER TESTED: NO
- STAT AUGMENT LIVE-PLAYER TESTED: NO
- STAT RECONNECT/DEATH TESTED: NO
- DEVICE FLOW PLAYTESTED: NO
- UI VISUAL REVIEW: NO
- BETTER COMBAT GAMEPLAY TESTED: NO
- BETTER COMBAT MOVEMENT/PROJECTILE COEXISTENCE PLAYTESTED: NO
- EXTERNAL BOSS GAMEPLAY TESTED: NO
- MULTIPLAYER TESTED: NO

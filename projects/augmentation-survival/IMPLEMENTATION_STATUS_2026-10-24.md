# Augmentation Survival — Implementation Status

Date: 2026-10-24
Status: M1C FUNCTIONAL BASELINE COMPLETE / SHARED MOVEMENT+AERIAL RUNTIME IMPLEMENTED / PROJECTILE FLIGHT-SEGMENT NEXT

## Verified code checkpoint

- Verified gameplay-code checkpoint: `446bf22d1de6517c70b71d36f864d2a31509eefa`
- Main movement feature commit: `4b612d4799bbbd6bf91e5699a85f132a6eb6b6af` — `feat(augmentation-survival): build shared movement runtime`
- 26.2 API adaptation: `9a72e89e5979a3fb5280edf7f9556ee48ed284e4` — `fix(augmentation-survival): align movement runtime with 26.2 API`
- 26.2 world-clock adaptation: `2dd35f71d22a132737609ab9dab998805d97b825` — `fix(augmentation-survival): use 26.2 world clock API`
- Final client input-edge guard: `446bf22d1de6517c70b71d36f864d2a31509eefa` — `fix(augmentation-survival): guard initial jump input edge`
- GitHub Actions: `40072354505` / Build Augmentation Survival Pack #42
- Build and pinned-pack inspection: PASS
- Pack dependency inspection: PASS — 23 pinned external artifacts
- Runtime classpath verification: PASS — 150 files
- Dedicated server smoke: PASS
- Client boot smoke: PASS
- Verification artifacts upload: PASS
- Artifact: `augmentation-survival-pack-smoke-42`
- Artifact ID: `12552215797`
- Artifact ZIP SHA-256: `4bc0b680aa58beb4394dcdc36d5176e2c6d8a11998411836dd0f2d31b2a314ba`
- Runtime initialization marker: `Loaded movement augment contract: shared conditional movement and aerial runtime`

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
- recipes match `GAME_DESIGN.md`
- server creates up to three valid same-tier candidates
- max-level augments are removed from the candidate pool
- if a tier is fully maxed, a draw cannot start and no core is consumed
- opening/cancelling an offer does not consume a core
- opaque server offer token validates the final transaction
- one core is consumed only after the final selection validates
- `두 번째 선택` commits two distinct offered augments in one transaction while consuming one core
- successful selection persists the new augment level and updates the save-wide threat high-water score
- advanced workstation supports 3:1 upward conversion
- advanced workstation supports one conversion or bulk conversion
- closing the workstation cancels an uncommitted offer without consuming a core

### Functional client baseline

- custom card-selection screen instead of a vanilla button list
- six tier selectors
- three candidate cards
- card shows canonical Korean name, tier, active tag when applicable, current-to-next level, and max level
- selected-card state is visible
- advanced device has separate `증강 선택` / `코어 합성` modes
- Korean and English item/device names exist

### Unconditional stat augment runtime

- bundled `stat_modifier_contract.json` is the data-driven source for unconditional player attribute augments
- persistent augment levels remain the source of truth; vanilla attribute modifiers are transient derived state
- stat definitions sharing the same attribute and operation are summed before one deterministic modifier is applied
- modifier identity is stable and `addOrUpdateTransientModifier` is used, preventing reconnect/state-update duplication by design
- runtime reconciliation occurs on server-owned player-state replacement, player join, and respawn; no per-tick player scan is used
- `강건함` (`sturdiness`) — +2 maximum health per level, max +10 at Lv.5
- `속공` (`rapid_strikes`) — +4% attack speed per level, max +20% at Lv.5
- `빠른 발` (`quick_feet`) — +3% movement speed per level, max +15% at Lv.5
- `버티기` (`brace`) — +10% knockback resistance per level, max +50% at Lv.5
- verified initialization: 4 definitions / 4 modifier groups

### Shared combat provenance / damage runtime

- canonical provenance enum covers ROOT melee/projectile and derived projectile/chain/AOE/echo/path-echo/active/replay/authority/companion/reflected/special-percent-HP damage
- `DELAYED_DAMAGE` provenance is explicit and carries the reserved post-mitigation installment amount
- scoped server-thread provenance override prevents derived/replayed damage from accidentally becoming a new ROOT attack
- ROOT melee and ROOT projectile outgoing damage are modified before vanilla armor/protection mitigation
- post-armor/Protection augment reduction is applied before protection pools, vanilla absorption and HP
- `날 선 감각` (`keen_edge`) — ROOT melee damage +4% per level, max +20%
- `정확한 조준` (`precise_aim`) — ROOT projectile damage +4% per level, max +20%
- `질긴 피부` (`tough_skin`) — general augment damage reduction 2% per level, max 10%
- `가벼운 착지` (`light_landing`) — fall damage reduction 12% per level, max 60%
- `튼튼한 폐` (`strong_lungs`) — drowning damage reduction 15% per level, max 45%
- `마지막 힘` (`last_strength`) — at 30% HP or lower, outgoing ROOT damage +10% per level, max +30%

### Timed combat / kill runtime

- timed runtime state tracks only players with active combat effects instead of globally scanning all players/entities each tick
- dynamic movement bonuses sharing the same stat axis are combined into one deterministic transient modifier
- timed runtime is cleared on death/join/respawn and modifiers are removed with it
- `연속 처치` (`kill_streak`) — hostile kill grants up to 3 refreshed 4-second stacks; each stack grants level-scaled movement and attack-speed bonuses
- `다급한 발걸음` (`urgent_steps`) — successful incoming damage grants the level-scaled 3-second movement-speed window

### Protection / delayed-damage runtime

- `ProtectionAugmentService` owns post-mitigation protection state without a global entity scan
- implemented order at this stage is: ordinary augment reduction -> damage adaptation -> delayed split -> augment shield pool -> augment absorption pool -> vanilla absorption -> HP -> lethal protection
- source-owned protection pools keep `SHIELD` and augment `ABSORPTION` identities separate instead of merging them into vanilla absorption hearts
- this pool identity is preserved for later mechanics such as `보호막 폭발`
- `피해 적응` (`damage_adaptation`) — after a real non-delayed hit, the same exact damage-type family is reduced by 10% / 20% / 30% for 6 seconds
- `피해 지연` (`delayed_damage`) — 20% / 30% / 40% of post-mitigation damage is reserved and replayed as five installments every 2 seconds over 10 seconds
- delayed installments carry explicit `DELAYED_DAMAGE` provenance
- delayed installments skip armor defense, armor durability loss, Protection/resistance recalculation and delay splitting
- delayed installments still enter current invulnerability/protection pools/vanilla absorption/HP/lethal protection instead of directly editing HP
- if the player dies, pending delayed installments and transient protection pools are cleared
- shield pool grant/replace API and data tuning for `보호막` exist, but the active trigger is deliberately not faked before the common active-ability engine exists

### Healing runtime

- augment-generated primary healing has one server-owned entry point
- routed order is: `회복 증폭` -> actual HP gain -> `넘치는 회복` -> `회복 나눔`
- natural hunger/saturation regeneration does not use this path
- `흡혈` (`lifesteal`) — direct server-player hostile kill heals 1 / 2 / 3 HP through the common augment-heal path
- `회복 증폭` (`healing_amplification`) — +10% / +20% / +30% on healing routed through the augment-generated non-natural heal service
- `넘치는 회복` (`overflow_healing`) — excess routed healing becomes a source-owned augment absorption pool capped at 4 / 8 / 12 HP
- `회복 나눔` (`healing_share`) — 15% / 30% / 45% of actual HP restored is shared within 8 blocks to allied entities and tamed animals owned by the player
- shared healing may use the recipient player's own healing amplification
- shared healing cannot create another overflow-healing pool or another healing-share recursion
- dominated-companion ally integration remains pending because the domination runtime does not exist yet

### Lethal-protection chain

- lethal behavior stays on the common vanilla/Fabric death path instead of creating independent fake deaths
- implemented order is `한 번 버티기` -> vanilla held Totem of Undying -> `비상 토템` inventory totem -> `재림`
- `한 번 버티기` (`last_stand`) — lethal damage becomes 1 HP, 1 second normal-damage protection, persisted 180-second cooldown
- `비상 토템` (`emergency_totem`) — after vanilla hand-held death protection fails, one Totem of Undying in non-equipment inventory is consumed and the vanilla death-protection component/effects/stat/criterion path is reproduced
- `재림` (`reincarnation`) — after earlier death protection fails, lethal damage enters a 3-second recovery stasis, persists a 180-second cooldown, blocks normal damage, disables attack/mining/item/block-use paths, damps velocity each server tick, then restores full maximum health
- damage tagged to bypass invulnerability is not suppressed by `한 번 버티기` or `재림` normal-damage protection

### Shared movement / aerial runtime

- bundled `movement_modifier_contract.json` owns the tunable values for the shared locomotion batch
- only players who own movement augments requiring live state are tracked; there is no global player/entity scan
- movement runtime is reconciled on augment-state replacement, join and respawn, and transient state is cleared on death
- conditional movement bonuses in this runtime share one deterministic movement-speed modifier instead of stacking separate transient modifiers
- `숨 고르기` (`catch_breath`) — after 60 ticks without sprinting, movement speed +10% / +20% / +30%
- `야행성` (`nocturnal`) — nighttime movement-speed facet +5% / +10% / +15%; mining-speed facet is still pending
- `햇빛 체질` (`sun_affinity`) — daytime movement-speed facet +4% / +8% / +12%; natural-regeneration interval facet is still pending
- `성큼걸음` (`step_up`) — derived step-height modifier raises the normal player 0.6-block step height by +0.4 to the canonical 1-block step
- `멀리뛰기` (`long_jump`) — a sprinting vanilla ground jump multiplies horizontal X/Z motion by 1.08 / 1.16 / 1.24 while preserving the vanilla Y jump
- `관성 전환` (`momentum_shift`) — actual fall distance is tracked independently of fall-damage reduction; landing after at least 4 blocks grants +10% / +20% / +30% movement speed for 60 ticks
- `두 번 점프` (`double_jump`) — one airborne extra jump uses the exact vanilla jump-power calculation, preserves horizontal velocity, resets fall distance and consumes one server-owned air-jump charge
- the air-jump charge is restored after returning to ground, swimmable water or a climbable surface
- the client only sends a jump-key request; spectator/flying/passenger/action-lock/contact/augment/charge eligibility is decided by the server
- the client input edge remembers the previous ground/water/climb contact state so the initial ordinary jump press is not reinterpreted as an air jump when contact clears
- `공중 연계` (`aerial_combo`) — a successful airborne hostile ROOT melee hit restores one air-jump charge; repeat hits and the same target are allowed because there is no internal cooldown
- long jump is attached narrowly to the vanilla `jumpFromGround` boundary instead of replacing the player movement loop
- the movement contract and new Mixins/network registration load in both full-pack dedicated-server and client smoke environments

## Deliberately not claimed complete

- The full 146-roster gameplay runtime is not complete.
- `회복 증폭` currently applies to healing routed through the augment-generated heal service. It is not yet a global interceptor for every vanilla/external non-natural healing source.
- `흡혈` currently proves direct player hostile-kill attribution. Owned derived/active damage attribution expands when those damage producers are implemented.
- `회복 나눔` currently supports vanilla team/allied checks and player-owned tamed animals; future `지배` companions need an ally adapter when that runtime is implemented.
- `보호막` has pool/tuning infrastructure but is not player-activatable yet because the common active-ability input/cooldown engine is not implemented.
- custom augment shield/absorption pools do not yet have final HUD/presentation.
- `재림` attack/mining/use actions are server-locked and velocity is suppressed during stasis, but strict positional packet immobility has not been gameplay-tested; do not claim a perfect freeze until real play confirms it.
- `야행성` mining-speed facet is not implemented yet.
- `햇빛 체질` natural-regeneration interval facet is not implemented yet.
- `발 안 묶이기`, `물살 타기`, `매달리기`, `탄성 저장`, `물 위 달리기`, `속도 전환` and other collision/medium/physics-heavy movement mechanics are not part of this shared locomotion baseline yet.
- `탄성 저장` has an actual-fall-distance trigger in canon, but the exact stored-distance-to-propulsion velocity mapping is not sufficiently specified; no arbitrary conversion was invented in this batch.
- movement/aerial runtime is build/boot verified but has not been live-playtested for feel, input timing, packet synchronization, jump count or recharge behavior.
- Projectile flight-segment runtime for penetration/homing/ricochet is not implemented yet.
- Common active-ability engine is not implemented yet.
- Save-wide threat scaling has state/high-water observation but final live hostile stat application is not complete.
- The current device block models inherit vanilla workstation models as a functional placeholder. Final model/texture/VFX/sound are not done.
- The current card screen is a functional interaction baseline, not a final visual-quality pass.
- CI proves compile/build, contract initialization, full-pack dedicated-server boot and full-pack client boot. It does **not** prove real combat numbers, movement feel, jump input correctness, installment timing, healing recipients or lethal ordering in a real player session.
- Better Combat 3.2.2 co-loads and boots with the movement runtime, but movement/combat coexistence has not been playtested.
- External boss reward gameplay is not playtested.
- Multiplayer is not tested.

## Implementation batching rule from this checkpoint

Do not return to one-augment-at-a-time implementation except for isolated bugs or genuinely unique mechanics.

Use shared-system batches:

1. build/reuse a common runtime boundary;
2. connect every already-canonical augment that safely fits that boundary;
3. validate once at the end of the meaningful batch;
4. keep unique/complex mechanics for the next appropriate shared engine rather than hardcoding them independently.

## Next implementation unit

Next large batch: **projectile flight-segment runtime**.

Primary anchors:

1. establish one server-authoritative projectile identity / ownership / flight-segment runtime that preserves canonical combat provenance;
2. connect `관통 화살`, `추적 화살`, `도탄` and every other already-canonical projectile augment that safely reuses that runtime in one batch;
3. keep ROOT projectile hits distinct from derived ricochet/chain/secondary projectile effects so ROOT-only counters cannot recurse;
4. preserve vanilla projectile behavior and external-mod coexistence; do not replace the entire projectile loop when narrow hooks are sufficient;
5. collision-heavy movement mechanics omitted from the current movement batch may return later as their own physics unit rather than being hacked into the projectile work;
6. after the projectile batch, build the common active-ability input/cooldown/charge engine and wire the already-prepared `보호막` plus compatible active augments through it.

Before treating the existing stat/combat/protection/healing/movement work as PLAYTESTED, verify a real player across stat application/reconnect/death, Better Combat cadence/damage, damage adaptation, delayed installments, overflow/share healing, lethal protection order, conditional movement, long jump, momentum landing, double-jump input/recharge and aerial-combo recharge.

## Validation labels

- CODE REVIEWED: YES
- BUILD VERIFIED: YES — `446bf22d1de6517c70b71d36f864d2a31509eefa`
- JAR PRODUCED: YES — Actions run `40072354505`
- PACK DEPENDENCY INSPECTION: PASS — 23 pinned external artifacts
- PACK RUNTIME CLASSPATH: PASS — 150 files
- PACK SERVER SMOKE: PASS
- PACK CLIENT SMOKE: PASS
- STAT CONTRACT INITIALIZATION: PASS — 4 definitions / 4 modifier groups
- COMBAT/PROTECTION/HEAL/DEATH CONTRACT INITIALIZATION: PASS
- MOVEMENT CONTRACT INITIALIZATION: PASS
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
- BETTER COMBAT MOVEMENT COEXISTENCE PLAYTESTED: NO
- EXTERNAL BOSS GAMEPLAY TESTED: NO
- MULTIPLAYER TESTED: NO

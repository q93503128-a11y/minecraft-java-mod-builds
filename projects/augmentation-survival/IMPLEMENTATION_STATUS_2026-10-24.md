# Augmentation Survival — Implementation Status

Date: 2026-10-24
Status: M1C FUNCTIONAL BASELINE COMPLETE / PROTECTION+HEALING ENGINE IMPLEMENTED / MOVEMENT BATCH NEXT

## Verified code checkpoint

- Verified code checkpoint: `59f45a9a4ce1f2febf09404b295f3c8b816c18f8`
- Main feature commit: `942708946ca0c00a51f4a38a002e314823a5cd43` — `feat(augmentation-survival): add protection and healing pipeline`
- 26.2 API fix commit: `59f45a9a4ce1f2febf09404b295f3c8b816c18f8` — `fix(augmentation-survival): update 26.2 criterion trigger package`
- GitHub Actions: `40070335710` / Build Augmentation Survival Pack #38
- Build and pinned-pack inspection: PASS
- Pack dependency inspection: PASS — 23 pinned external artifacts
- Runtime classpath verification: PASS — 150 files
- Dedicated server smoke: PASS
- Client boot smoke: PASS
- Verification artifacts upload: PASS
- Artifact: `augmentation-survival-pack-smoke-38`
- Artifact ID: `12550967850`
- Artifact ZIP SHA-256: `a9ceeb08ae35ac27696715ffe98a6aed3df7f48de087bb0955ac37ce7bc6f16a`
- Runtime initialization marker: `Loaded combat augment contract: shared combat/protection/heal/death baseline`

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

- canonical provenance enum covers ROOT melee/projectile and future derived projectile/chain/AOE/echo/path-echo/active/replay/authority/companion/reflected/special-percent-HP damage
- `DELAYED_DAMAGE` provenance is now explicit and carries the reserved post-mitigation installment amount
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

- new `ProtectionAugmentService` owns post-mitigation protection state without a global entity scan
- canonical implemented order at this stage is: ordinary augment reduction -> damage adaptation -> delayed split -> augment shield pool -> augment absorption pool -> vanilla absorption -> HP -> lethal protection
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
- canonical routed order is: `회복 증폭` -> actual HP gain -> `넘치는 회복` -> `회복 나눔`
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

## Deliberately not claimed complete

- The full 146-roster gameplay runtime is not complete.
- `회복 증폭` currently applies to healing routed through the augment-generated heal service. It is not yet a global interceptor for every vanilla/external non-natural healing source.
- `흡혈` currently proves direct player hostile-kill attribution. Owned derived/active damage attribution expands when those damage producers are implemented.
- `회복 나눔` currently supports vanilla team/allied checks and player-owned tamed animals; future `지배` companions need an ally adapter when that runtime is implemented.
- `보호막` has pool/tuning infrastructure but is not player-activatable yet because the common active-ability input/cooldown engine is not implemented.
- custom augment shield/absorption pools do not yet have final HUD/presentation.
- `재림` attack/mining/use actions are server-locked and velocity is suppressed during stasis, but strict positional packet immobility has not been gameplay-tested; do not claim a perfect freeze until real play confirms it.
- Double-jump/aerial-combo movement runtime is not implemented yet.
- Projectile flight-segment runtime for penetration/homing/ricochet is not implemented yet.
- Common active-ability engine is not implemented yet.
- Save-wide threat scaling has state/high-water observation but final live hostile stat application is not complete.
- The current device block models inherit vanilla workstation models as a functional placeholder. Final model/texture/VFX/sound are not done.
- The current card screen is a functional interaction baseline, not a final visual-quality pass.
- CI proves compile/build, contract initialization, full-pack dedicated-server boot, and full-pack client boot. It does **not** prove real combat damage numbers, installment timing, healing recipients, lethal ordering or game feel.
- Direct player verification of stat acquisition/reconnect/death has not been performed yet.
- `한 번 버티기` / held totem / emergency totem / reincarnation ordering has not been verified in live gameplay yet.
- delayed-damage timing and no-double-mitigation behavior have not been live-playtested yet.
- overflow absorption and healing-share values/targeting have not been live-playtested yet.
- Better Combat gameplay coexistence is not playtested; CI only proves Better Combat 3.2.2 co-loads and boots with the runtime.
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

Next large batch: **movement / aerial runtime**.

Primary anchors:

1. `두 번 점프` + `공중 연계` — establish one server-authoritative air-jump budget and recharge contract.
2. Connect other already-canonical movement augments that can safely reuse the same locomotion state/boundaries instead of creating separate tick systems.
3. Preserve vanilla movement and external-mod coexistence; do not replace the whole player movement loop unless a narrower hook cannot satisfy the contract.
4. After the movement batch, build the projectile flight-segment engine and connect `관통 화살` + `추적 화살` + `도탄` plus other compatible projectile augments together.
5. Then build the common active-ability engine and wire already-prepared `보호막` plus other compatible active augments through one input/cooldown/charge path.

Before treating the existing stat/combat/protection/healing work as PLAYTESTED, verify a real player across immediate stat application, reconnect, death/respawn, Better Combat cadence/damage, damage-adaptation family matching, delayed installments, overflow/share healing, and the full lethal-protection order.

## Validation labels

- CODE REVIEWED: YES
- BUILD VERIFIED: YES — `59f45a9a4ce1f2febf09404b295f3c8b816c18f8`
- JAR PRODUCED: YES — Actions run `40070335710`
- PACK DEPENDENCY INSPECTION: PASS — 23 pinned external artifacts
- PACK RUNTIME CLASSPATH: PASS — 150 files
- PACK SERVER SMOKE: PASS
- PACK CLIENT SMOKE: PASS
- STAT CONTRACT INITIALIZATION: PASS — 4 definitions / 4 modifier groups
- COMBAT/PROTECTION/HEAL/DEATH CONTRACT INITIALIZATION: PASS
- PROTECTION PIPELINE LIVE-PLAYER TESTED: NO
- HEALING PIPELINE LIVE-PLAYER TESTED: NO
- LETHAL CHAIN LIVE-PLAYER TESTED: NO
- COMBAT AUGMENT LIVE-PLAYER TESTED: NO
- STAT AUGMENT LIVE-PLAYER TESTED: NO
- STAT RECONNECT/DEATH TESTED: NO
- DEVICE FLOW PLAYTESTED: NO
- UI VISUAL REVIEW: NO
- BETTER COMBAT GAMEPLAY TESTED: NO
- EXTERNAL BOSS GAMEPLAY TESTED: NO
- MULTIPLAYER TESTED: NO

# Augmentation Survival — Implementation Status

Date: 2026-10-24
Status: M1C FUNCTIONAL BASELINE COMPLETE / SHARED COMBAT-HEAL-DEATH BASELINE IMPLEMENTED / PROTECTION+MOVEMENT BATCH NEXT

## Verified code checkpoint

- Code checkpoint: `d523acc93aa2727ac3b608b01d43ba0a22995a25`
- Commit: `feat(augmentation-survival): build shared combat runtime`
- GitHub Actions: `40067817151` / Build Augmentation Survival Pack #35
- Build and pinned-pack inspection: PASS
- Pack dependency inspection: PASS — 23 pinned external artifacts
- Runtime classpath verification: PASS — 150 files
- Dedicated server smoke: PASS
- Client boot smoke: PASS
- Verification artifacts upload: PASS
- Artifact: `augmentation-survival-pack-smoke-35`
- Artifact ID: `12550201468`
- Artifact ZIP SHA-256: `851b0d11ffe23f4c89e6ffd3fcd6d0744412ed86f5f42a6cd9cf089a2ff65ac9`

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
- the stat contract validates augment IDs against the canonical 146-entry catalog during initialization
- verified initialization: 4 definitions / 4 modifier groups

### Shared combat provenance / damage runtime

- canonical provenance enum now exists for ROOT melee/projectile and future derived projectile/chain/AOE/echo/path-echo/active/replay/authority/companion/reflected/special-percent-HP damage
- scoped server-thread provenance override exists so future derived damage can explicitly avoid ROOT-only recursion
- ROOT melee and ROOT projectile outgoing damage are modified before vanilla defense processing
- post-armor/Protection augment damage reduction is applied at the shared post-mitigation boundary before absorption/HP
- same event is not replaced wholesale; the runtime modifies the damage value at narrow shared boundaries
- `날 선 감각` (`keen_edge`) — ROOT melee damage +4% per level, max +20%
- `정확한 조준` (`precise_aim`) — ROOT projectile damage +4% per level, max +20%
- `질긴 피부` (`tough_skin`) — general augment damage reduction 2% per level, max 10%
- `가벼운 착지` (`light_landing`) — fall damage reduction 12% per level, max 60%
- `튼튼한 폐` (`strong_lungs`) — drowning damage reduction 15% per level, max 45%
- `마지막 힘` (`last_strength`) — at 30% HP or lower, outgoing ROOT damage +10% per level, max +30%

### Timed combat / kill runtime

- timed runtime state tracks only players with active combat effects instead of scanning every player/entity each tick
- dynamic movement bonuses that share the same stat axis are combined into one deterministic transient modifier
- timed runtime is cleared on death/join/respawn and modifiers are removed with it
- `연속 처치` (`kill_streak`) — hostile kill grants up to 3 refreshed 4-second stacks; each stack grants level-scaled movement and attack-speed bonuses
- `다급한 발걸음` (`urgent_steps`) — successful incoming damage grants the level-scaled 3-second movement-speed window

### Healing / lethal-damage baseline

- augment-generated primary healing has a shared server-owned entry point
- the shared heal path applies `회복 증폭` to routed non-natural augment heals before actual HP gain is measured
- natural hunger/saturation regeneration does not use this path
- `흡혈` (`lifesteal`) — direct server-player hostile kill heals 1/2/3 HP and uses the common augment-heal path
- companion entities do not satisfy the current direct player-killer gate
- `한 번 버티기` (`last_stand`) — lethal damage is intercepted at the common pre-death boundary, health becomes 1, cooldown is persisted for 180 seconds, and normal damage is rejected for 20 ticks
- damage explicitly tagged to bypass invulnerability is not suppressed by the one-second protection window
- the lethal interception sits before vanilla death completion rather than implementing a separate fake resurrection path

## Deliberately not claimed complete

- The shared combat/heal/death baseline is implemented, but the full 146-roster gameplay runtime is not complete.
- `회복 증폭` is only wired into healing that enters the new augment-generated heal service. It is not yet a global interceptor for every possible external non-natural healing source.
- `흡혈` currently proves the direct player hostile-kill path. Owned derived/active damage attribution will expand when those damage producers are implemented.
- `피해 적응`, `피해 지연`, shield/absorption augments, overflow healing, healing share, and the remaining lethal-chain augments are not implemented yet.
- Double-jump/aerial-combo movement runtime is not implemented yet.
- Projectile flight-segment runtime for penetration/homing/ricochet is not implemented yet.
- Common active-ability engine is not implemented yet.
- Save-wide threat scaling has state/high-water observation but final live hostile stat application is not complete.
- The current device block models inherit vanilla workstation models as a functional placeholder. Final model/texture/VFX/sound are not done.
- The current card screen is a functional interaction baseline, not a final visual-quality pass.
- CI proves compile/build, contract initialization, full-pack dedicated-server boot, and full-pack client boot. It does **not** prove combat numbers or game feel in a real player session.
- Direct player verification of stat acquisition/reconnect/death has not been performed yet.
- `한 번 버티기` vs vanilla Totem ordering has not been verified in live gameplay yet.
- `연속 처치`, `다급한 발걸음`, `흡혈`, damage-reduction values, and low-HP damage bonus have not been live-playtested yet.
- Better Combat gameplay coexistence is not playtested; CI only proves that Better Combat 3.2.2 co-loads and boots with the new runtime.
- External boss reward gameplay is not playtested.
- Multiplayer is not tested.

## Implementation batching rule from this checkpoint

Do not return to one-augment-at-a-time implementation except for isolated bugs or genuinely unique mechanics.

Use shared-system batches:

1. build/reuse a common runtime boundary;
2. connect every already-canonical augment that safely fits that boundary;
3. validate once at the end of the meaningful batch;
4. keep unique/complex mechanics for the next appropriate shared engine rather than hardcoding them independently.

The current combat batch is the reference for this implementation cadence.

## Next implementation unit

Next large batch: **protection + healing continuation**, not a three-augment micro-slice.

Target one shared unit containing as many canonical effects as the common pipelines safely support:

1. `피해 적응` and one finite `피해 지연` runtime using explicit provenance and post-mitigation ordering.
2. shield/absorption consumers on the same protection ordering path.
3. `넘치는 회복` + `회복 나눔` on the common healing pipeline, with actual-HP-heal accounting and recursion exclusion.
4. remaining compatible lethal-chain behavior around `한 번 버티기` / vanilla Totem / later emergency resurrection semantics without bypassing the common death path.
5. After that batch, implement `두 번 점프` + `공중 연계` and related movement effects as one movement runtime batch.
6. Then build the projectile flight-segment engine and connect `관통 화살` + `추적 화살` + `도탄` plus other compatible projectile augments together.

Before treating the current combat/stat batch as PLAYTESTED, verify a real player across immediate stat application, reconnect, death/respawn, repeated level changes, Better Combat attack cadence/damage, hostile kills, lethal interception, and timed-buff expiry.

## Validation labels

- CODE REVIEWED: YES
- BUILD VERIFIED: YES — `d523acc93aa2727ac3b608b01d43ba0a22995a25`
- JAR PRODUCED: YES — Actions run `40067817151`
- PACK DEPENDENCY INSPECTION: PASS — 23 pinned external artifacts
- PACK RUNTIME CLASSPATH: PASS — 150 files
- PACK SERVER SMOKE: PASS
- PACK CLIENT SMOKE: PASS
- STAT CONTRACT INITIALIZATION: PASS — 4 definitions / 4 modifier groups
- COMBAT CONTRACT INITIALIZATION: PASS
- COMBAT AUGMENT LIVE-PLAYER TESTED: NO
- STAT AUGMENT LIVE-PLAYER TESTED: NO
- STAT RECONNECT/DEATH TESTED: NO
- DEVICE FLOW PLAYTESTED: NO
- UI VISUAL REVIEW: NO
- BETTER COMBAT GAMEPLAY TESTED: NO
- EXTERNAL BOSS GAMEPLAY TESTED: NO
- MULTIPLAYER TESTED: NO

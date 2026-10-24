# Augmentation Survival — Implementation Status

Date: 2026-10-24
Status: M1C FUNCTIONAL BASELINE COMPLETE / REFERENCE STAT SLICE IMPLEMENTED / COMBAT PIPELINE NEXT

## Verified code checkpoint

- Code checkpoint: `3911b238d234d1bf335726befaaa2d1e39e7fd11`
- Commit: `feat(augmentation-survival): apply reference stat augments`
- GitHub Actions: `40065641689` / Build Augmentation Survival Pack #34
- Build and pinned-pack inspection: PASS
- Dedicated server smoke: PASS
- Client boot smoke: PASS
- Verification artifacts upload: PASS

The documentation checkpoint after this code commit does not change Java gameplay code and must not be treated as a new gameplay verification target.

## Implemented now

### Runtime/state foundation

- persistent server-owned player augment state
- augment levels stored by stable namespaced ID
- state survives death through persistent `copyOnDeath` attachment semantics
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

### Reference stat augment runtime

- bundled `stat_modifier_contract.json` is the data-driven source for unconditional player attribute augments
- persistent augment levels remain the source of truth; vanilla attribute modifiers are transient derived state
- stat definitions sharing the same attribute and operation are summed before one deterministic modifier is applied
- modifier identity is stable and `addOrUpdateTransientModifier` is used, preventing reconnect/state-update duplication
- runtime reconciliation occurs on server-owned player-state replacement, player join, and respawn; no per-tick player scan is used
- `강건함` (`sturdiness`) applies +2 maximum health per level, max +10 at Lv.5
- `속공` (`rapid_strikes`) applies +4% attack speed per level through the normal attack-speed attribute axis, max +20% at Lv.5
- the stat contract validates augment IDs against the canonical 146-entry catalog during initialization

## Deliberately not claimed complete

- Only the first two reference gameplay behaviors, `강건함` and `속공`, are implemented. The rest of the 146-roster gameplay behaviors are not implemented yet.
- Shared combat provenance, healing/death pipelines, projectile flight-segment runtime, and the common active-ability engine are not yet complete.
- The current device block models inherit vanilla workstation models as a functional placeholder. Final model/texture/VFX/sound are not done.
- The current card screen is a functional interaction baseline, not a final visual-quality pass.
- CI proves compile/build, contract initialization, dedicated-server boot, and client boot. It does **not** prove live-player stat behavior.
- Direct player verification of `강건함` across acquisition/reconnect/death has not been performed yet.
- Direct duplicate-modifier verification of `속공` across repeated acquisition/reconnect/death has not been performed yet.
- Better Combat gameplay coexistence is not playtested.
- External boss reward gameplay is not playtested.
- Multiplayer is not tested.

## Next implementation unit

Follow `DEVELOPMENT_IMPLEMENTATION_PLAN_2026-10-24.md` reference vertical slice instead of bulk-implementing all 146 at once.

Next:

1. `흡혈` — establish server-owned damage attribution -> healing and explicit recursion exclusion.
2. `한 번 버티기` — establish lethal-damage interception and per-life/cooldown state semantics without bypassing the common death pipeline.
3. One shield/damage-delay path — prove post-mitigation protection ordering and finite delayed-damage provenance.
4. Then `두 번 점프` + `공중 연계`.
5. Then `관통 화살` + `추적 화살` + `도탄` flight-segment runtime.

Before treating the stat slice as PLAYTESTED, verify a real player with acquired levels across immediate application, reconnect, death/respawn, repeated level changes, and Better Combat attack cadence.

## Validation labels

- CODE REVIEWED: YES
- BUILD VERIFIED: YES — `3911b238d234d1bf335726befaaa2d1e39e7fd11`
- JAR PRODUCED: YES — Actions run `40065641689`
- PACK SERVER SMOKE: PASS
- PACK CLIENT SMOKE: PASS
- STAT CONTRACT INITIALIZATION: PASS — 2 definitions / 2 modifier groups
- STAT AUGMENT LIVE-PLAYER TESTED: NO
- STAT RECONNECT/DEATH TESTED: NO
- DEVICE FLOW PLAYTESTED: NO
- UI VISUAL REVIEW: NO
- BETTER COMBAT GAMEPLAY TESTED: NO
- EXTERNAL BOSS GAMEPLAY TESTED: NO
- MULTIPLAYER TESTED: NO

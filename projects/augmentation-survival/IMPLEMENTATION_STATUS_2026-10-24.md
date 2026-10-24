# Augmentation Survival — Implementation Status

Date: 2026-10-24
Status: M1C FUNCTIONAL BASELINE COMPLETE / REFERENCE AUGMENT SLICE NEXT

## Verified code checkpoint

- Code checkpoint: `168cb49c37435ade9207017e25a428adf9209dff`
- Commit: `feat(augmentation-survival): activate core device selection loop`
- GitHub Actions: `40064257071` / Build Augmentation Survival Pack #33
- Build and pinned-pack inspection: PASS
- Dedicated server smoke: PASS
- Client boot smoke: PASS
- Verification artifacts upload: PASS

The docs/metadata commit after this checkpoint does not change Java gameplay code. It only records the verified state and removes player-facing bootstrap wording.

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

## Deliberately not claimed complete

- The 146 augment gameplay effects are **not** implemented yet. Catalog/state support is implemented; effect behaviors come next.
- Shared combat provenance, healing/death pipelines, projectile flight-segment runtime, and the common active-ability engine are not yet complete.
- The current device block models inherit vanilla workstation models as a functional placeholder. Final model/texture/VFX/sound are not done.
- The current card screen is a functional interaction baseline, not a final visual-quality pass.
- No direct in-game human playtest of the device selection flow has been performed yet.
- Better Combat gameplay coexistence is not playtested.
- External boss reward gameplay is not playtested.
- Multiplayer is not tested.

## Next implementation unit

Follow `DEVELOPMENT_IMPLEMENTATION_PLAN_2026-10-24.md` reference vertical slice instead of bulk-implementing all 146 at once.

Start with:

1. `강건함` — prove persistent level -> attribute application -> reload/death stability.
2. `속공` — prove attack-speed modifier application and duplicate-modifier prevention.
3. Then `흡혈`, `한 번 버티기`, and one shield/damage-delay path to establish combat/heal/death provenance.
4. Then `두 번 점프` + `공중 연계`.
5. Then `관통 화살` + `추적 화살` + `도탄` flight-segment runtime.

## Validation labels

- CODE REVIEWED: YES
- BUILD VERIFIED: YES — `168cb49c37435ade9207017e25a428adf9209dff`
- JAR PRODUCED: YES — Actions run `40064257071`
- PACK SERVER SMOKE: PASS
- PACK CLIENT SMOKE: PASS
- DEVICE FLOW PLAYTESTED: NO
- UI VISUAL REVIEW: NO
- BETTER COMBAT GAMEPLAY TESTED: NO
- EXTERNAL BOSS GAMEPLAY TESTED: NO
- MULTIPLAYER TESTED: NO

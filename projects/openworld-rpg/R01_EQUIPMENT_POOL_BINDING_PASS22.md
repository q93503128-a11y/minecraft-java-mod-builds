# Open-World RPG — R01 Equipment Pool Binding Pass 22

> Status: **R01 ORDINARY BASE ROSTER + GENERIC AFFIX ELIGIBILITY + WEAPON-FAMILY POWER TEMPLATE BOUND / NESSA LIVE PURCHASE STILL FAIL-CLOSED**
>
> Date: 2026-09-28

## External-asset boundary

This pass does not introduce temporary player-facing equipment models.

R01 visuals remain bound to the external-first production direction already recorded in canon:

- Quaternius Modular Weapons for grounded martial weapons/shields;
- KayKit Fantasy Weapons Bits / approved Quaternius bases for magical weapons/focus;
- Quaternius Modular Character Outfits - Fantasy for armor.

A base may exist in server data before its final visual asset is accepted, but no placeholder/vanilla model is promoted as the finished player-facing identity.

## Closed in this pass

The exact R01 ordinary equipment base roster is now data-bound:

- 7 weapons;
- Watch Buckler;
- Apprentice Focus;
- 3 armor families;
- 4 accessories.

Each base resolves to its actual combat family/archetype/slot contract.

The shared pool resolver follows the already-closed global rules:

- categories with zero canonical weight for that equipment family are excluded;
- all static generic affixes in positive-weight categories remain eligible;
- a weapon additionally receives exactly one matching parameterized
  `specific weapon-family power` affix;
- no hidden per-affix rarity weight is invented;
- no unsupported affix is silently deleted to make a purchase succeed.

## Parameterized weapon-family power

The canonical ordinary affix:

```text
specific weapon-family power: +4% to +10%
```

is now represented as a data-owned parameterized template.

For a Riverwood Bow this becomes a stable Bow-family power identity; for a sword it becomes Sword-family power, etc.

The runtime adapter publishes it through the existing
`WEAPON_FAMILY_POWER` combat authority and preserves the global +60% family-power gear cap.

## Nessa bridge

A persisted Nessa stock slot can now be translated all the way through:

```text
Nessa stock
→ exact R01 base profile
→ exact generic eligible pool
→ matching weapon-family affix where applicable
→ shared ordinary affix materializer
```

If the eligible pool still contains an affix without a real gameplay runtime, materialization returns a blocker and the purchase remains closed.

Current shared blockers are intentionally visible rather than removed from the pool:

- Attack Speed;
- weak-point damage;
- dodge/sprint Stamina-cost reduction;
- Ultimate charge gain;
- Movement Speed;
- Healing Done;
- Healing Received;
- negative-status duration reduction;
- potion/food effect strength.

Therefore Nessa still does not debit Gold or deliver incomplete gear.

```text
R01 ORDINARY BASE ROSTER: BOUND
WEAPON-FAMILY POWER TEMPLATE: IMPLEMENTED
WEAPON-FAMILY POWER RUNTIME: IMPLEMENTED
R01 GENERIC AFFIX ELIGIBILITY: BOUND
NESSA → SHARED MATERIALIZER BRIDGE: IMPLEMENTED
UNSUPPORTED-AFFIX SILENT FILTERING: BLOCKED

NESSA LIVE PURCHASE: NO
TEMPORARY PLAYER-FACING MODELS: NO
FINAL EXTERNAL EQUIPMENT ASSET BINDING: INCOMPLETE
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```


## Verification

GitHub Actions run `36384564089`, code state
`125fe9c68421eb462051fb8d00fa9975030af4ee`:

```text
UNIT TESTS: PASS
CLEAN BUILD / JAR: PASS
CORE DEDICATED SERVER SMOKE: PASS
GAMEPLAY DEPENDENCY SERVER SMOKE: PASS
GAMEPLAY CLIENT STARTUP SMOKE: PASS
R01 INTEGRATION PLAYTEST JAR BUILD: PASS

PLAYER-FACING EQUIPMENT ASSET REVIEW: NOT COMPLETE
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

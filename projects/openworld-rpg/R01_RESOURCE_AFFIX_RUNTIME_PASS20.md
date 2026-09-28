# Open-World RPG — Resource Affix Runtime Pass 20

> Status: **MAX HP / MAX MANA / MAX STAMINA / MANA RECOVERY / STAMINA RECOVERY / SKILL MANA-COST REDUCTION LIVE**
>
> Date: 2026-09-28

## Closed in this pass

Six previously data-known ordinary affixes now have real project-owned gameplay consumers:

- Max HP
- Max Mana
- Max Stamina
- Mana Recovery
- Stamina Recovery
- skill Mana-cost reduction

They are no longer `stored_only`.

## Server authority

Equipped items are aggregated server-side and published into the existing project resource/vital authority.

### Max HP

`Max HP` gear percentage is now passed into the canonical:

```text
BaseHP × VITMultiplier × (1 + summed MaxHP bonus)
```

The Minecraft MAX_HEALTH projection still preserves current HP percentage when the build changes.

The transient `PlayerCombatBuildState.maxHealth()` uses the same MaxHP gear bonus, so internal combat snapshots and the live player projection no longer diverge.

### Mana / Stamina maximum

Max Mana and Max Stamina percentage bonuses are applied after their existing WIL / END base formulas.

Changing:

- WIL / END;
- Max Mana gear;
- Max Stamina gear;

preserves the player's current resource percentage rather than providing a free refill.

### Recovery

Mana Recovery multiplies the canonical natural Mana regeneration rate.

It does not bypass:

- the 20-tick Mana-spend regeneration lock;
- the 5-second out-of-combat rule;
- the existing 2x out-of-combat Mana regeneration behavior.

Stamina Recovery multiplies the canonical Stamina regeneration rate without bypassing authored regeneration delays.

### Skill Mana-cost reduction

The equipped contribution is additive and hard-capped at the canonical **20%** gear cap.

Project Mana preflight and committed spend both use the same effective cost, so a cast cannot pass admission at one cost and be charged a different cost later.

## Fail-closed boundary

The ordinary-affix catalog currently contains 29 static canonical affixes.

```text
STATIC CANONICAL AFFIXES: 29
LIVE RUNTIME-ADAPTED STATIC AFFIXES: 18
RESOURCE AFFIX AUTHORITY READY: YES
```

The six affixes promoted in this pass are the only new entries marked `implemented`.

The following remain intentionally gated until their real consumer exists:

- Critical Chance
- Critical Damage
- Attack Speed
- weak-point damage
- dodge/sprint Stamina-cost reduction
- Ultimate charge gain
- Movement Speed
- Healing Done
- Healing Received
- negative-status duration reduction
- potion/food effect strength

Parameterized weapon-family and element/status-output affixes also remain separate from the static catalog until their actual family/status payload is bound.

No unsupported affix is silently removed from an item's valid pool.

## Nessa impact

Pass 18 stock seeds can now materialize any R01 merchant item whose eventual valid-affix pool contains only runtime-complete affixes.

The remaining blocker for live Nessa purchase is still the exact **R01 base-specific valid-affix pool binding** plus the still-gated affix consumers that those pools require.

```text
RESOURCE/VITAL AFFIX RUNTIME: IMPLEMENTED
RESOURCE PERCENT PRESERVATION: IMPLEMENTED
MANA COST REDUCTION CAP: IMPLEMENTED
CATALOG STARTUP VALIDATION: IMPLEMENTED

R01 BASE-SPECIFIC VALID AFFIX POOLS: NO
ALL CANONICAL AFFIX RUNTIME PUBLISHERS: NO
NESSA LIVE PURCHASE: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

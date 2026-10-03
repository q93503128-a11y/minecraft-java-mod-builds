# Open-World RPG — R01 Backend Integrity Pass 13

> Date: 2026-10-03
>
> Scope: backend correctness only; no new gameplay values or presentation assets

## Closed in this pass

### Hartcrown target scope

Hart's Momentum is a weapon effect, not an external-actor effect.

Before this pass, its only runtime call lived after
`ExternalActorBindingRuntime.ownsDamageAuthority(...)`, so the effect never triggered on ordinary
Better Combat targets such as vanilla mobs.

The melee hook now has two explicit paths:

```text
Better Combat selected LivingEntity
├─ project-owned target
│  └─ project target/damage/cadence authority accepts
│     └─ Hartcrown lunge
│        └─ project HP damage
│           └─ project poise damage (+35% when triggered)
└─ compatibility target
   └─ Hartcrown lunge
      └─ existing Better Combat/vanilla damage call
```

The target was already selected by Better Combat before the lunge in both cases. Hartcrown does not
search for another target and does not extend candidate reach.

### Hartcrown movement/contact ordering

The project-owned path previously applied HP damage before moving the player. That could read as a
hit followed by a pull-forward.

The lunge now resolves after project acceptance/cadence but before HP contact, so the server
transaction matches the authored “lunge toward the valid target, then land the accepted melee hit”
behavior more closely.

The existing collision-resolved 1.5-block `MoverType.SELF` cap is unchanged.

### Random donor ecology containment

The manual audit's spawn-containment concern was rechecked through the full bootstrap chain.

`data/openworld_rpg/integration/dependencies.json` requires:

```text
mobfilter 0.28.0+26.2
profiles: gameplay, essential
```

`ExternalRuntimeContainment` owns the managed MobFilter configuration and disallows non-authored
random ecology spawn reasons including natural, chunk-generation, spawner, structure, breeding,
summoned/event/conversion/reinforcement/patrol/trial-spawner paths.

Therefore no duplicate project spawn suppressor is added here.

## Still deliberately open

This pass does not choose a new rule for Hart's Momentum after its 2.0-second opportunity expires
during one uninterrupted sprint. The current implementation's sprint-break re-arm behavior was
identified by the manual audit as not explicitly authored. That is a gameplay-canon question, not a
backend plumbing bug, and remains open until canon decides it.

Regalhart production spawn also remains closed by its known attack/weak-point/presentation gates.

## Validation boundary

A full Openworld build is required because the melee mixin changed.

```text
CODE REVIEWED: YES
BUILD VERIFIED: PENDING
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

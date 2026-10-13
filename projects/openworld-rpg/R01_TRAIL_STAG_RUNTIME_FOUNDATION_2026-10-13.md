# Open-World RPG — R01 Trail Stag Runtime Foundation

> Date: 2026-10-13
> Scope: persistent Trail Stag unlock/cooldown authority and temporary Resolve rules. This pass does not expose an unaccepted visible mount.

## Closed in this pass

- persistent per-player Trail Stag unlock state;
- first registration selects Trail Stag as the active mount and is idempotent;
- exact 4.0 s hostile-combat quiet requirement represented as 80 ticks;
- exact 1.0 s summon-channel contract represented as 20 ticks;
- exact 12 s Resolve-break re-summon cooldown represented as 240 ticks;
- fresh Trail Stag Resolve = rider MaxHP × 1.00;
- Resolve accounting reports only real overflow and deliberately does not invent a second damage-mitigation formula;
- exact initial movement/collision tuning seeds are centralized: 6.4 b/s cruise, 1.0 s to 90%, 0.75 s coast-to-stop, 1.25-block authored step-up, 120/180 deg/s turn anchors, 1.65-block jump apex, 85% horizontal retention and 0.15 s landing recovery;
- a server-side idempotent completion seam now exists for `A Stag at the Ford` to grant the permanent mount unlock.

## Deliberately still gated

The Trail Stag is not player-facing production-ready yet.

The preferred visual source remains the Quaternius Ultimate Animated Animal Pack Stag specified by `MOUNTS.md`. Current repository research located a transported `Stag.glb` in the public Quaternius showcase mirror, but the raw binary is not admitted here solely from that mirror. Current `R01_ASSET_INTAKE.md` requires source-specific acquisition/license evidence because central Quaternius licensing changed over time.

Therefore this pass does not:

- substitute a vanilla horse;
- expose an invisible/static mount;
- copy an unverified binary asset;
- fabricate mount collision dimensions from a guessed model scale;
- promote any R01 candidate coordinate;
- fabricate a dungeon/no-mount volume;
- invent a second player-damage formula for Resolve overflow.

The next Trail Stag pass should bind the exact accepted Stag model/animation artifact, then implement the custom rideable entity, server-selected summon placement, safe dismount search, fixed hop and U-key request transport against the accepted visible collision scale.

## Validation state before CI

```text
CODE REVIEWED: YES
TESTED: PENDING CI
BUILD VERIFIED: NO
JAR PRODUCED: NO
TRAIL STAG VISIBLE RUNTIME: GATED
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

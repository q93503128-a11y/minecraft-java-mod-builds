# Open-World RPG — R01 Canon Hygiene Pass 12

> Date: 2026-10-03
>
> Basis: manual full audit + Regalhart reward-authority repair
>
> Previous code checkpoint: `66df0af2b2837bf7473159abde169b0035ca4c56`
>
> Build Openworld RPG #323 / run `37119716754`: **SUCCESS**

## Purpose

This pass changes no gameplay code. It removes two pieces of stale/overstated live canon exposed by the manual full audit.

## Regalhart reward normalization

The effective current first-defeat Regalhart progression contract is:

```text
Combat EXP: 20% current next-Lv requirement
Class XP: 15% current Class Rank requirement
Gold: 70
Regalhart Antler: x2
guaranteed Superior+ normal gear
Hartcrown direct roll: 15%
```

Repeat remains:

```text
Combat EXP: 7%
Class XP: 6%
Gold: 35
Regalhart Antler: x1
guaranteed normal gear
second normal gear: 25%
Hartcrown direct roll: 15%
```

The obsolete Regalhart 40% Combat EXP / 30% Class XP wording in `STATUS_AND_R01_ENCOUNTERS.md` is removed. The 20% value also matches the global field/world-boss first-defeat target in `GAME_DESIGN.md`; the 15% Class XP value is the later explicit Regalhart reward contract already used by `R01RegalhartRewardRules`.

## R01 design-closure correction

R01 has extensive authored content and most gameplay contracts are fixed, but it cannot honestly claim “zero implementation-time gameplay choices” while these Regalhart values remain unresolved:

- exact 7.0-block weighted-selector endpoint ownership;
- exact 12.0-block weighted-selector endpoint ownership;
- Crown Charge ordinary guardability;
- Rear Kick guard-pressure;
- Rear Kick player-poise pressure;
- mirrored Antler Sweep second-hit exact timing.

Therefore current status is:

```text
R01 BROAD CONTENT STRUCTURE CLOSED: YES
R01 GAMEPLAY / CONTENT DESIGN FULLY CLOSED: NO
R01 IMPLEMENTATION-TIME GAMEPLAY CHOICES REMAIN: YES — NARROW REGALHART COMBAT CONTRACTS ONLY
R01 ASSET_BINDING COMPLETE: NO
R01 SPATIAL_BINDING COMPLETE: NO
R01 SOURCE READY: NO
R01 IMPLEMENTED: NO
R01 PLAYTESTED: NO
R01 MULTIPLAYER TESTED: NO
```

This does not reopen already-closed quests, economy, merchant rules, fishing, Camp/housing, mount rules, fast travel, UI flow or ordinary encounter canon. Only the explicitly listed gaps are open.

## Validation

This is documentation/canon hygiene only. No build is required for this pass.

```text
CODE CHANGED: NO
GAMEPLAY VALUES INVENTED: NO
STALE REWARD CONFLICT REMOVED: YES
FALSE FULL-DESIGN-CLOSURE CLAIM REMOVED: YES
BUILD RE-RUN FOR DOC-ONLY PASS: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

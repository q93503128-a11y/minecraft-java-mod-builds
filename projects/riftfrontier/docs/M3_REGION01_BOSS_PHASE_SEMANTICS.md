# M3 Region 01 Boss Phase Semantics

Status: **PRODUCTION PHASE COMPOSITION AUTHORED / PRESENTATION SOURCE COVERAGE INCOMPLETE**

This document records the production phase-composition boundary for `riftfrontier:boss/region_01_first_apex`. It does not approve final damage, timing, hit geometry, material, VFX, sound, encounter attachment, or field balance.

## Authored production policy

The canonical resource is:

`data/riftfrontier/riftfrontier/combat/region_01_first_apex_semantics.json`

It is loaded through `Region01BossProductionSemantics` and decoded by the existing strict `BossCombatSemanticProfile` codec.

Phase composition:

```text
phase 1
- region_01_committed_strike
- region_01_line_displacement

phase 2
- region_01_committed_strike
- region_01_line_displacement
- region_01_arena_pressure
```

This uses only the three attack roles already authored in the production Region 01 content pack. The phase transition changes attack composition by adding the already-approved arena-pressure role; it does not introduce a hidden damage/health multiplier as the phase identity.

## Why this boundary exists

`BossCombatSemanticProfile` and `ValidatedBossCombatSemantics` already provide the fail-closed architecture for phase attack pools. The production gap was that Region 01 still had no packaged phase policy, leaving the first boss profile with three attacks but no authored runtime composition.

This batch fills that data gap instead of adding another selection framework.

## Verification contract

`ProductionRegion01BossPhaseSemanticsTest` requires:

- the packaged semantic resource to target exactly `riftfrontier:boss/region_01_first_apex`;
- phase 1 to contain committed strike + line displacement;
- phase 2 to contain those two plus arena pressure;
- the two phase pools to differ;
- every phase attack to belong to the actual packaged production boss profile;
- phase 2 to equal the complete current production boss attack set.

## Presentation boundary

The selected Dragon Evolved source currently has exact reviewed source-motion windows for `Headbutt` and `Punch`. Those windows are evidence only and are not automatically equivalent to server ACTIVE windows.

Do not fabricate source-animation mappings for line displacement or arena pressure merely to make `BossAnimationSemanticBinding` pass. The next animation work must explicitly review whether an existing source clip can represent each logical phase or author a derived animation on the selected rig, then record that evidence before publication.

Likewise, do not attach the boss to the Region 01 encounter until Minecraft scale/hit geometry, presentation readability and authoritative damage policy have evidence-backed values.

## Next implementation boundary

1. Keep the current phase composition stable unless field evidence demonstrates a problem.
2. Build the production logical `BossPresentationProfile` for the three existing attacks without pretending unresolved physical assets exist.
3. Bind only source-animation windows that have explicit motion/phase evidence. Missing coverage must remain a visible fail-closed gap.
4. Continue the selected Dragon Evolved material/animation/VFX/sound path using documented external references and license-safe assets rather than an AI-invented final art language.
5. Human player-combat field play remains independently required and is not replaced by this boss-side work.

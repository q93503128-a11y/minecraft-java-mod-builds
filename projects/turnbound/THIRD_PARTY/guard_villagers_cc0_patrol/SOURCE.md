# GuardVillagersFabric patrol reference

- Upstream: https://github.com/ffggyyuufamily-tech/GuardVillagersFabric
- Inspected commit: `46f6893a7c97f3b1f49b5068bbd8ee12be4760c3`
- License: CC0-1.0
- License blob: `1625c1793607996fcfc46420e8aa2f3d2b7efd1e`
- Classification: code_library / editable algorithm base

Inspected source:
- `src/main/java/com/guardvillagers/entity/goal/PerimeterPatrolGoal.java`
  - blob `2866c61d86a6c8e0113b6bceab88e21764a21dfe`
- `src/main/java/com/guardvillagers/navigation/GuardNavigation.java`
  - blob `227a78f9428be177c5328340c884b0164b3e4954`
- `src/main/java/com/guardvillagers/entity/ai/GuardMovementSlotResolver.java`
  - blob `6a98b582b446193e9dd3909aeb09100b8b583387`

TURNBOUND adaptation:
- varied next-point selection instead of a rigid conveyor loop;
- reject tiny next hops when a readable farther move exists;
- authored idle dwell between roaming moves;
- deterministic server-side choice because TURNBOUND uses a shared multiplayer encounter representation.

Not imported:
- guard ownership/economy;
- squad tactics;
- formation commands;
- custom guard navigation stack.

TURNBOUND's implementation is intentionally smaller because field enemies are encounter presentation, while battle state remains owned by BattleSession.

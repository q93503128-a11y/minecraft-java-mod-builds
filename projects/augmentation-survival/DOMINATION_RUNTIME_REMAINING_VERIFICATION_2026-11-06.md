# Augmentation Survival — Domination combat: outstanding live verification

Recorded: 2026-11-06. Current checked branch: `main`.
Status: shared native AI/faction wiring CODE-REVIEWED + COMPILED, targeted Creeper removal/Illager retaliation GameTests PASS, actual species-by-species native combat NOT PLAYTESTED.

## User symptoms observed in gameplay (prior to these code changes)
- Guardian attacks without native charge/cadence.
- Magma Cube jumps once every several seconds rather than normal combat hopping.
- Blaze pursues in melee instead of ranged volleys.
- Vex only flies, never attacks.
- Piglin Brute trembles without hitting.
- Ordinary Illagers do not respond to attacks by dominated Illagers.
- Other species may share similar issues; do not assume all are fixed without testing.
- Dominated Creeper that self-detonates remains in the saved companion roster.

## Source changes already in main
- Native control: target is server-authoritative; avoid per-tick forced generic navigation and do not constantly reset active attack target. Brain-oriented piglin-family target memory adapter, Vex/Blaze native navigation handling, allied faction override, damage-on-hit retaliation target assignment.
- Creeper permanent `DISCARDED` removal now releases the saved slot once. Chunk unloading is not counted as a death; revive can reclaim.
- Existing tests: Creeper discarded removal and opposing Evokers retaliation pass in CI.
- Existing two combat regression tests still fail in full CI. Compiled code is not proof that vanilla Guardian/Blaze/Vex/Piglin attack Goals work against all hostile entity types.

## Next actual gameplay verification matrix
- Guardian and Elder Guardian: water environment; verify native laser charging and damage against hostile target, target switch, line-of-sight, and cancellation on lost target.
- Blaze: verify flying height, several native fireball volleys, and ranged-distance maintenance rather than generic melee running.
- Vex: verify pursuit plus repeated sword hits, target switching, phase-through-blocks behavior, and retargeting after owner damage.
- Magma Cube/Slime: verify normal hopping rhythm, contact damage, no artificial attack for smallest slime, and no attacks on allied owners.
- Piglin Brute/Piglin/Hoglin: verify native Brain ATTACK_TARGET persists through AI behaviors, target clearance, and not attacking the owner.
- Illager/Evoker/Vindicator and ordinary Zombie/Skeleton: verify attack on enemy with same vanilla species and enemy's retaliation; preserve original spell/ranged patterns.
- Creeper: explosion/discard releases capacity; chunk unload, dimension transfer, and revive do not permanently consume or duplicate the slot.
- Repeat with two real players; verify same-owner ally immunity, enemy aggro, save/reload, and no client-side authority changes.
- Distinguish whether a failure is caused by target-finding, navigation, Goal/Brain attack condition, projectile, or actual damage pipeline.

## Failing regressions and diagnostics
- `DominationBehaviorGameTests.dominationConvertsEnemyIntoPersistentCombatAlly` refuses Zombie->Zombie `mobAttack` damage even though: victim hostile=true, allied=false, policy=true, iFrames=0; vanilla generic damage on that victim succeeds.
- `ReturnToSenderGameTests.realShieldDurabilityBlocksEmitConeAtLevelThreeThreshold`: three shield durability-consumption actions clear the block accumulator; eligible entities exist within the 7-block range. Cone special damage remains zero, but an independent vanilla generic damage call succeeds.
- Earlier `LivePlaytestRegressionGameTests.wallSlamAddsDamageToRealMeleeAtNearbyWall` failed intermittently in one run, but was not a consistent failing case.
- Do not delete or no-op either required test. Differentiate damage-source filtering, source tags and entity-specific friendly-fire gates from actual attack-Goal behavior.
- Test scripts and regression assertions were extended with diagnostics; final reported CI run `42227291659`: 166/168 required tests passed.

## Accurate verification
- Build Java and GameTest compile: PASS.
- Complete required GameTests: FAIL (2/168).
- New World Bosses raid cache GameTest: PASS (11 exact cache types).
- JAR from source commit `bd17f042292f93e5c9f460c9a9a2b8f9c383902f` created and bundled into a *playtest-only* .mrpack; subsequent git commits only changed GameTest diagnostics and docs.
- Client in-game playtest: NOT RUN. Multiplayer: NOT RUN. No claim of release readiness.

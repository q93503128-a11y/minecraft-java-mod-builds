# Survival Ascension 0.61 — Final Boundary Focused Test Matrix

Use the 0.61 JAR with `0.61.0-alpha.1-content-preview.1`. Back up any long-lived world before the first run.

## 1. Regression before Final Ascension
- Boot one fresh world and one existing 0.60 world.
- PASS: no save migration crash, missing registry, codec error, repeated login exception or external-mod linkage error.
- Verify the existing Final Ascension readiness gate still requires exactly Dragon stage2 + Expedition 9/9 + Apex first-clear 9/9 + Ascension Nexus.
- Run the 0.60 acts: mining wall, four construction cells, three guards, movement checkpoints, three regional echo sets, and three Shift seals. No GUI-only numeric submission may advance them.
- Snapshot Apex progress before/after acts 1-3. Apex unique first-clear count and rewards must not change.

## 2. Final boss admission
- Complete the third collapse seal in an open loaded arena.
- PASS: the old placeholder ending is replaced by the unique Final Ascension boss `세계의 경계자`.
- The boss must appear only after acts 1-3; selecting the old repeatable Ascension Trial must remain a separate action.
- During all boss phases, the Warden shell must remain actively hostile to the run owner. It must not lose the owner target, enter its idle burrow/dig-away behavior, or become a passive arena prop while the owner is valid and inside the arena.
- Attempt a second large activity for the same owner while the final boss is active. It must not stack another Final Ascension/Apex/Trial state machine.

## 3. Phase gate at 65%
- Damage the boss from full health toward 65%.
- PASS: one hit may land exactly on the 65% boundary but must not skip below it.
- At 65%, further incoming damage must be suppressed while the anchor phase is active.
- Exactly 고정점 3개, implemented as physical Crying Obsidian world anchors, must appear in loaded open positions.
- Mine the anchors normally and with Shift precision. They must respond to the same real Mining rules as ordinary blocks.
- Destroying all three anchors must remove protection and enter the breakthrough phase.

## 4. Phase gate at 30%
- Continue damage from 65% toward 30%.
- PASS: damage cannot skip below the 30% gate before the final phase transition.
- After the final phase begins, the health floor is gone and the boss can be killed normally.

## 5. Telegraph readability
Test every Survival-owned attack pattern multiple times from different camera angles and ranges.

- `LINE`: a visible straight telegraph must precede the hit lane. Moving sideways outside the lane before execution must avoid damage.
- `RING`: a visible circular warning around the boss must precede the annular hit zone. Staying close inside the inner safe zone or moving beyond the outer ring must avoid damage.
- `MARKED`: a visible warning must appear around the player's sampled position. Leaving that location before execution must avoid damage.
- PASS: telegraph appears before damage, damage occurs only in the authored zone, and no pattern silently tracks the player after its target position/direction has been locked.
- Report any unavoidable hit, invisible warning, stale particle ring, or attack that executes twice from one telegraph.

## 6. Failure and cleanup
For each case below, verify no boss/marker/bossbar remains after failure:
- owner death;
- owner leaves the 72-block arena long enough to fail;
- logout;
- encounter timeout;
- normal server Stop followed by restart while acts 1-3 are active;
- normal server Stop followed by restart while the final boss/anchors are active.

PASS: orderly `ServerStoppingEvent` cleanup removes Survival-owned active marker/anchor blocks, encounter mobs, the final boss, and boss bars before save. Cleanup removes a temporary block only when it is still the exact expected Survival marker. Player repair blocks and unrelated world blocks must remain.

A forced process kill, power loss, or JVM crash may bypass the orderly stopping event and is not claimed as a 0.61 cleanup guarantee. Report such a case separately instead of treating it as equivalent to a normal Stop/restart.

## 7. Permanent SavedData
- Before the first boss kill, inspect world data: no completed final state should be reported.
- Kill the final boss in the FINAL phase.
- PASS: `final_ascension_v1` becomes the one world-scoped Final Ascension completion authority.
- Reload the world and relog. Completion must remain true.
- Re-select Final Ascension after completion. It must report that the world is already conquered instead of granting another first-clear reward.
- Existing 0.60 worlds with no `final_ascension_v1` data must load with `complete=false` through codec defaults.

## 8. Reward isolation
On the first valid world completion:
- owner receives one named Nether Star `승천의 증표`;
- owner receives one awakened Mythic III Ascension equipment roll;
- owner receives 500 XP.

Before/after snapshots must show:
- Apex first-clear count unchanged by the final boss;
- no Apex mastery reward duplication;
- no repeatable Ascension Trial reward duplication;
- no random normal Elite conversion stacked onto `세계의 경계자`;
- the final boss is not absorbed into a normal Warband/tactical squad.

## 9. Final Mobility authority
Use Mobility Lv.100 after world completion.
- Without Field Mastery: maximum 공중 돌진(air-dash) count must be 4.
- With Field Mastery: maximum 공중 돌진 count must be 5.
- Dash power must be observably higher than pre-final Lv.100 by the authored +0.15 addition.
- Dash cooldown must be 4 ticks shorter than the ordinary Lv.100 value but never below 12 ticks.
- Ground reset, logout cleanup, water/passenger/fall-flying restrictions and existing Shift-independent controls must remain intact.

## 10. Final Construction authority
Use Construction Lv.100 after world completion.
- Without Field Mastery: selected line/causeway maximum remains bounded at 65.
- With Field Mastery: length cycling adds 81 and must not allow a value above 81.
- Wall/Floor final mastery size must reach 15×15.
- Shift during actual placement must still force the ordinary single placement behavior.
- Every generated target must still require loaded chunk, interaction permission, valid placement and real material from the existing physical supply path.
- Pending work must remain bounded by the existing per-player queue and global tick budget.

## 11. Multiplayer-later structural checks
Current practical validation is single-player first. Before future multiplayer sign-off, retain these acceptance points:
- completion is world-scoped and should unlock the same conquered-world authority for players in that world;
- bossbar viewers may observe nearby combat but only the run owner owns failure/admission state;
- no second owner may inherit or claim an orphaned active boss;
- reward ownership and helper behavior need a dedicated multiplayer pass when real multiplayer testing becomes available.

## Stop-and-report signals
Immediately report crash/save corruption, duplicate first-clear reward, Apex state mutation, force-loaded/generated chunks, boss health skipping the 65% or 30% boundary, anchors becoming remotely completable, Warden target loss or idle burrow during the active fight, unavoidable invisible attacks, orderly Stop/restart leaving temporary Final Ascension markers or the boss behind, marker cleanup deleting player blocks, orphan boss persistence, final SavedData disappearing after reload, air-dash count exceeding its bound, construction exceeding 81 or 15×15, or any external content classloading error.

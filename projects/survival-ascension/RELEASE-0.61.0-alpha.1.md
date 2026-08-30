# Survival Ascension 0.61.0-alpha.1 — Final Boundary Closure

## Release scope
0.61 closes the path that 0.60 deliberately left unfinished. The existing canonical Final Ascension admission gate is unchanged: Ender Dragon world stage 2, Expedition 9/9, Apex first-clear 9/9, and completed Ascension Nexus are still the only prerequisites.

Acts 1-3 from 0.60 remain the same real-world sequence: World Test, three compressed regional-echo sets, and three collapse seals. Completing the third seal now opens the unique final boundary encounter instead of ending at a placeholder message.

## Unique final boss — 세계의 경계자
- Uses a vanilla Warden only as the visible/AI shell; Survival Ascension owns the boss state, phase gates, arena objectives, telegraphs, rewards, cleanup and persistence.
- Opening phase is damage-gated at 65% health.
- At 65%, the boss becomes protected and three physical Crying Obsidian world anchors appear. All three must actually be mined before damage can continue.
- Breakthrough phase is damage-gated at 30% health.
- Final phase removes the gate and accelerates the encounter.
- Three readable Survival-owned attack patterns are used: `LINE`, `RING`, and `MARKED`. Each has a visible particle telegraph before execution instead of unavoidable hidden damage.
- The Warden shell periodically refreshes owner anger/attack-target authority so the active final boss does not drift into an idle/burrow state while its owner remains valid in the arena.
- Boss/anchor placement is bounded to loaded chunks. No force-load, region ticket, teleport-to-unloaded-area, optional-mod implementation dependency or Apex persistence mutation is used.

## Permanent completion
The unique boss is the first point at which Final Ascension completion becomes persistent.

New world-scoped SavedData authority: `survivalascension:final_ascension_v1`.

Codec fields all have backwards-safe defaults:
- `complete=false`
- `first_conqueror=""`
- `completed_game_time=0`

Acts 1-3 still own no persistence and still cannot write Expedition/Apex/Infrastructure completion.

## Orderly shutdown cleanup
- `FinalAscensionSystem` and `FinalAscensionBossSystem` listen for NeoForge `ServerStoppingEvent`.
- During a normal server stop/restart they remove their still-active Survival-owned temporary mobs, marker/anchor blocks, and boss bars before the world is saved.
- Cleanup still checks the exact expected temporary block before replacing it with air, so player-built repair blocks are not deleted.
- This is intentionally not described as crash recovery: a forced process kill, power loss, or JVM crash can bypass an orderly stopping event. No extra persistent encounter schema or force-loaded cleanup pass is introduced solely to cover those abnormal terminations.

## Final authority rewards
The reward is not a new universal raw-stat tier. It expands the physical action scale of existing Lv.100 systems.

### Mobility
- Final Ascension complete + Mobility Lv.100: one additional endgame 공중 돌진(air dash) tier.
- Without Field Mastery: up to 4 공중 돌진.
- With Field Mastery: up to 5 공중 돌진.
- Final mastery adds +0.15 dash power and reduces the Lv.100 dash cooldown by 4 ticks, with a 12-tick floor.

### Construction
- Final Ascension complete + Construction Lv.100 opens the next bounded line/causeway authority.
- Without Field Mastery: maximum selectable length 65.
- With Field Mastery: maximum selectable length 81.
- Final mastery expands wall/floor work to 15×15.
- Existing loaded-chunk, interaction, material-consumption, job-budget and Shift precision rules remain authoritative.

## Solo balance and logistics closure
The 0.61 source was re-audited for actual single-player pacing after the final content path was complete. The goal is to keep physical-world logistics and progression meaningful without turning repeated play into bulk-resource chores.

- Large one-time infrastructure requirements and repeated production/frontline costs were reduced to single-player scale; rare ingredients remain concentrated in genuinely late/final upgrades instead of routine rerolls.
- Ascension equipment options were made materially stronger and their actual effects are shown directly in player-facing equipment UI/messages.
- Ambiguous material labels were replaced with exact item/category names such as `철 주괴`, `자수정 조각`, `메아리 조각`, `석재 벽돌`, `아무 종류의 통나무`, `연료(석탄 또는 숯)`, and `식량(밀/당근/감자/비트)`.
- The frontline freight manifest now matches one expedition + one normal defense + one bastion defense at the retuned costs: food 60, iron ingot 16, fuel 3, logs 12, stone bricks 32.
- Registered logistics storage is a same-dimension network while loaded: player distance is no longer an artificial material-access tax, but unloaded chunks are still skipped and never force-loaded.
- Player-owned registered Barrel storage can be packed and moved without spilling its 27-slot contents. A moved ordinary depot restores its registration without consuming another field-supply charge; linked storage can auto-relink when replaced within six blocks of the owner's depot.
- Outpost-promoted depot anchors remain non-portable, linked barrels must be moved/unlinked before their anchor, another player's packed Barrel cannot auto-register, and packed logistics Barrels cannot be nested inside another packed logistics Barrel.
- Portable Barrel block-entity contents use Minecraft 26.2 `TypedEntityData`/`BLOCK_ENTITY_DATA`; the implementation was compile-validated with Java 25 rather than relying on pre-26.2 `CustomData` block-entity assumptions.

## Rewards and isolation
The first world completion grants the owner one named Nether Star `승천의 증표`, one awakened Mythic III Ascension equipment roll, and 500 XP. The final encounter does not call `ApexHuntData.recordVictory`, does not mutate Apex first-clear bits, and does not duplicate Apex or Ascension Trial rewards.

The internal Warden spawn is explicitly excluded from normal random Elite conversion so the final boss cannot accidentally inherit a second unrelated rank/trait ruleset. The existing Warband exclusion marker also keeps it out of normal tactical-squad formation.

## Compatibility
- Minecraft 26.2
- NeoForge 26.2.0.38-beta
- Java 25
- Network protocol 9
- Content pack identity: `0.61.0-alpha.1-content-preview.1`
- The seven external project/version IDs remain unchanged from 0.60.
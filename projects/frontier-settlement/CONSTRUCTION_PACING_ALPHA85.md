# Frontier Settlement 0.1.0-alpha.85 — construction pacing

Alpha.85 is a graphical-playtest pacing correction. Alpha.84 fixed the worker authority and the construction deadlock, but ordinary building remained much slower than the intended survival-game tempo.

## Root pacing defect

`SettlementConstructionService.tick` was called only inside a five-tick scheduler while grading also required `serverTick % 8 == 0`. Sampling the eight-tick gate only on five-tick boundaries meant grading could advance only every 40 ticks (LCM 5, 8), or roughly two seconds per grading cell. A modest house footprint could therefore spend well over a minute on grading alone.

## Alpha.85 pacing

- Ordinary building construction is ticked every server tick while active.
- Roads, outposts and selected-area civil works keep their historical five-tick scheduler.
- Grading advances at most once every 3 ticks when the physical builder is in range.
- Blueprint placement advances at most once every 4 ticks when the physical builder is in range.
- Ground-level work reach is 3.5 blocks instead of requiring the builder to walk within 1.5 blocks of nearly every blueprint cell.
- The same builder can physically haul 32 items per trip and stage up to 32 wood plus 32 stone at the active site, reducing repetitive storage-site-storage trips.

No block is placed remotely across unloaded terrain. The builder still walks, material still comes from real shared-storage ItemStacks, construction still consumes the exact configured cost transactionally, and there is no teleport, force-load, virtual material balance or second construction authority.

## Playtest target

A first house should visibly grade, fetch materials and build rather than feel instant, but the player should no longer wait minutes for a small structure. Large landmarks remain proportionally longer because they contain more blueprint cells and require more real materials.

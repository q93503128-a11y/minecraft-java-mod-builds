# Frontier Settlement 0.1.0-alpha.86 — construction pacing II

Alpha.85 removed the 5-tick/8-tick scheduler aliasing defect, but a real graphical playtest still measured more than ten minutes for one 9x9 house. Alpha.86 targets the remaining logistics and movement bottlenecks rather than making construction virtual or instant.

## Root cause found after Alpha.85

A house contains 367 blueprint placements and 121 grading cells. More importantly, `stageRemainingMaterials` tried to keep the active-site crate exactly at its reserve target before every blueprint step. After one material was consumed, a full 32-item reserve became 31, which immediately blocked construction and sent the single physical builder all the way back to town to fetch exactly one replacement item. Blueprint insertion order also alternates between distant wall/roof positions, so the old 3.5-block local work radius caused repeated short pathfinding moves even after materials arrived.

## Alpha.86 pacing

- Site reserve target: 64 items per material category.
- Site refill low-water mark: 8 items.
- Initial staging still uses real shared-storage ItemStacks and the builder MAINHAND.
- After staging, construction does not refill merely because 64 became 63. It keeps building locally until the reserve is genuinely low or the next transactional placement cannot be funded.
- House cost is 48 wood + 20 stone, so a normal house can stage each category in one physical trip when storage contains the required stack.
- Grading cadence: 1 tick per eligible cell.
- Blueprint cadence: 2 ticks per eligible placement.
- Ordinary/grade local work envelope: 10.5 blocks; this is a construction-zone reach, not a teleport. The builder still physically reaches the site before working.
- High-work validation envelope: 14 blocks around the selected work/scaffold position.
- Remaining construction navigation requests use faster 1.05–1.10 speed modifiers while retaining the same PathfinderMob base movement attribute.

## Expected house pacing

The hard cadence floor for 367 blueprint placements is about 36.7 seconds at 20 TPS, plus roughly 6 seconds for 121 grading cells. Initial wood/stone hauling, pathfinding, terrain shape, storage distance, scaffolding and final return-home time add real runtime. The practical target for an ordinary nearby house is roughly 45–90 seconds, not an automated guarantee. A distant/obstructed site may take longer.

## Authority and safety unchanged

No material is virtualized or prepaid into a second ledger. No builder teleports, no chunk is force-loaded, and no additional worker authority is created. Block placement still follows the existing world-success-before-ItemStack-consume transaction and rollback rules. Roads, outposts and civil works keep their separate pacing/authority.

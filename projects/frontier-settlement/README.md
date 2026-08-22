# Frontier Settlement

Minecraft Java 26.2 / NeoForge 26.2 cooperative settlement-growth mod.

## Alpha.3 vertical slice

Alpha.3 keeps the full Alpha.2 house/lumber construction slice and adds the first physical expansion infrastructure.

- one server-authoritative shared settlement per world
- one physical shared barrel stockpile with live wood / stone / metal / food ledger
- shared compact HUD synced to every connected player
- persistent construction villager that walks to active construction work positions
- persistent building and road construction progress across server saves/restarts
- starter house construction: 9x9 timber-frame home, framed four-side windows, gabled spruce roof, four-point lantern lighting
- starter lumber camp construction: 11x9 semi-open timber workshop, rear windows, structural front beam/posts, gabled roof, four-point lantern lighting
- real warehouse material consumption on groundbreaking
- building cost transactions pre-count the live stockpile before mutating any slot, so a failed start cannot partially eat resources
- automatic safe building-site search within 40 blocks
- terrain acceptance gate for buildings: footprint height variance must be at most 2 blocks
- water, block entities, tree trunks, valuable/non-natural obstructions and existing structures reject building sites instead of being destroyed
- top-down no-drop building-site preparation followed by a continuous cobblestone foundation and low-spot support fill
- hard building order: floor -> frame/walls/gables -> roof -> doors/fixtures/lighting
- the worker never breaks a newly obstructing building block; construction pauses instead of producing item drops or overwriting player work
- completion validation requires every planned building block, lantern, window and roof block to exist before registration
- housing capacity is persisted; each completed starter house adds 4 housing

### New in Alpha.3 — short road

- first road unlocks after at least one completed house and one completed lumber camp
- temporary placement seam: stand 5-32 blocks from the settlement center, face outward, then use `/frontier road`
- road is 16 blocks long and 3 blocks wide
- road palette is gravel center lane with cobblestone edge lanes
- route is flattened to one walkable road level; accepted footprint may vary by at most one block before preparation
- water, fluids, block entities, non-natural obstructions, existing structures and existing road footprints reject the route
- route preparation is top-down and uses no drop-producing block destruction path
- after preparation, each planned road block is left empty for the construction villager to place; a newly introduced obstruction pauses work rather than being broken
- every road placement requires solid, dry support below it, preventing gravel falls or visually floating lanes
- road progress survives save/reload
- completed road start, direction, length and endpoint are stored as persistent infrastructure for the next outpost slice
- road cost is 24 physical stone-category items from the shared stockpile, pre-counted transactionally before any stack is changed

### Alpha.3 acceptance

A construction pass is not considered successful if any of the following occurs:

1. a building roof phase begins before supporting walls, gables and beams are complete;
2. building or road terrain preparation creates dropped item entities through block-breaking logic;
3. a site silently destroys containers, fluids, tree trunks, ores/non-natural blocks, existing player construction or an existing road;
4. building foundation/floor blocks visibly float above accepted terrain;
5. planned building windows are absent or disconnected during the finished state;
6. a finished enclosed building interior lacks its planned lantern light grid;
7. an obstruction introduced during building or road construction is automatically broken or overwritten;
8. save/reload loses active building type/origin/progress or active road start/direction/length/progress;
9. a failed building or road start consumes only part of its required stockpile cost;
10. a road accepts more than one block of pre-build height variance, contains unsupported gravel, or finishes with a missing lane block;
11. the first road can be started before both the starter house and lumber camp exist;
12. a completed road endpoint is not persisted for the following outpost expansion slice.

Temporary test seams:
- `/frontier found`
- `/frontier status`
- `/frontier rescan`
- `/frontier build house`
- `/frontier build lumber_camp`
- `/frontier road`

The final interaction model remains world-space building/road placement rather than commands. Debug commands are temporary testing seams while worker, blueprint, terrain-safety, shared-resource and spatial-expansion loops are stabilized.

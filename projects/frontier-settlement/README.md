# Frontier Settlement

Minecraft Java 26.2 / NeoForge 26.2 cooperative settlement-growth mod.

## Alpha.2 vertical slice

- one server-authoritative shared settlement per world
- one physical shared barrel stockpile with live wood / stone / metal / food ledger
- shared compact HUD synced to every connected player
- persistent construction villager that walks to active construction work positions
- persistent construction progress across server saves/restarts
- starter house construction: 9x9 timber-frame home, framed four-side windows, gabled spruce roof, four-point lantern lighting
- starter lumber camp construction: 11x9 semi-open timber workshop, rear windows, structural front beam/posts, gabled roof, four-point lantern lighting
- real warehouse material consumption on groundbreaking
- automatic build-site search within 40 blocks
- terrain acceptance gate: footprint height variance must be at most 2 blocks
- water, block entities, tree trunks, valuable/non-natural obstructions and existing structures reject the site instead of being destroyed
- top-down no-drop site preparation followed by a continuous cobblestone foundation and low-spot support fill
- hard construction order: floor -> frame/walls/gables -> roof -> doors/fixtures/lighting
- the worker never breaks a newly obstructing block; construction pauses instead of producing item drops or overwriting player work
- completion validation requires every planned block (including all lanterns/windows/roof blocks) to exist before the building is registered
- housing capacity is persisted; each completed starter house adds 4 housing
- temporary test seams: `/frontier found`, `/frontier status`, `/frontier rescan`, `/frontier build house`, `/frontier build lumber_camp`

### Alpha.2 construction acceptance

A construction pass is not considered successful if any of the following occurs:

1. a roof phase begins before its supporting walls, gables and beams are complete;
2. terrain preparation creates dropped item entities through block-breaking logic;
3. a site silently destroys containers, fluids, tree trunks, ores/non-natural blocks or existing player construction;
4. foundation/floor blocks visibly float above accepted terrain;
5. planned windows are absent or disconnected during the finished state;
6. a finished enclosed interior lacks its planned lantern light grid;
7. an obstruction introduced during construction is automatically broken or overwritten;
8. save/reload loses the active construction type, origin or progress step.

The final interaction model remains world-space building placement rather than commands. Debug commands are temporary testing seams while the worker, blueprint, terrain-safety and shared-resource loop is stabilized.

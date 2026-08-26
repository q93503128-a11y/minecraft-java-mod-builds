# Frontier Settlement — canonical direction

The complete repository-side design source of truth is `CANONICAL_PLAN.md`. Read that file, this current-direction summary and the current `main` source before continuing development. Where an older backlog still treats lifelike NPC simulation or rare-NPC breadth as a priority, the bounded-NPC direction below supersedes that priority without deleting the rest of the original design.

Current implementation delta: **0.1.0-alpha.77**. The large historical canonical/gap documents remain the original scope ledger; this file records the newer bounded-NPC and exploration/outpost gameplay direction until the next consolidated documentation pass.

Non-negotiable summary:

- The player should keep playing Minecraft, not a spreadsheet.
- One server/world uses one shared settlement and territory state.
- Internal simulation may be deep; player micromanagement stays shallow.
- Resources remain physical items in settlement storage, with a cached HUD ledger.
- Building flow is palette -> world preview -> position/rotation -> validation -> physical hauling -> worker construction.
- Roads/outposts are the spatial expansion layer instead of endlessly enlarging one flat town.
- Vanilla villager jobs/trading are not settlement progression; custom worker roles replace them.
- No player-authored functional-building registration in the planned scope.
- No early teleport network that invalidates roads/logistics.
- UI information architecture follows proven references such as Against the Storm, Manor Lords and MineColonies instead of generic invented panels.
- Normal controls stay compact and avoid important vanilla key conflicts.
- World/combat/dungeon/item depth remains compatible with selected external mods rather than being reimplemented here.
- NPC event/dialogue/social breadth is intentionally bounded. Frontier does not require lifelike daily-life simulation; add NPC interactions only when they create a clear gameplay decision, reward, service or progression effect.
- Content work should prefer deeper use of existing settlement systems and companion content over adding decorative NPC behavior or another management layer.
- External structures should not all collapse into one generic survey reward: broad soft archetypes may feed different existing systems while unknown companions still fall back to generic survey progression.

Alpha.76 deepens the exploration -> territory loop without a new menu, building or NPC interaction layer. Previously discovered trade-settlement knowledge adds at most +16 field evidence and industrial-site knowledge adds at most +6 exposed-stone evidence when a new road-end outpost is evaluated. Those bonuses stack with the existing bounded biome/survey bias but are deliberately too small to create agriculture or quarry specialization without real local terrain evidence. Mining remains untouched so metadata cannot fabricate an ore specialization where no physical ore exists.

Alpha.77 makes the DOMAIN stage care about a diversified physical territory instead of raw outpost count alone. Distinct productive outpost roles among lumber, agriculture, quarry and mining create a capped territory-network level: two different roles -> level 1, three -> level 2, four -> level 3. Repeating the same specialization never raises the level. The level strengthens only existing town services: market relic payout gains +1 emerald per level, one-metal workshop repair gains +8 durability per level, and advanced forge/reforge selection power gains +1 per level. The underlying market goods, metal, relics, weapons, outpost production and transport remain real ItemStacks; no research currency, virtual cargo, new menu, worker family or second logistics authority is created.

Always validate actual Minecraft presentation: no floating roofs/foundations, no unsafe block destruction or loose-drop preparation, proper windows/lighting, readable worker movement, and no magical instant-building feel.

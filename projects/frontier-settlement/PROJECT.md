# Frontier Settlement — canonical direction

The complete repository-side design source of truth is `CANONICAL_PLAN.md`. Read that file and the current `main` source before continuing development.

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

Always validate actual Minecraft presentation: no floating roofs/foundations, no unsafe block destruction or loose-drop preparation, proper windows/lighting, readable worker movement, and no magical instant-building feel.

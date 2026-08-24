# Frontier Settlement

Minecraft Java 26.2 / NeoForge 26.2 cooperative survival settlement-growth mod.

Canonical direction: `ORIGINAL_DESIGN_v0.2.md` + `CANONICAL_PLAN.md`. Remaining original-scope gaps are tracked in `COMPLETION_GAP_AUDIT.md`.

## Current version: 0.1.0-alpha.49

Frontier Settlement owns the shared settlement, physical construction, residents, production, roads, outposts, logistics, defense infrastructure, bounded civil works and territory progression. Companion mods remain the preferred source of biome, dungeon, structure, combat, weapon and loot breadth.

This is a broad playable alpha, not original-v0.2 completion. Do not call it complete while `COMPLETION_GAP_AUDIT.md` still contains meaningful `부분/미구현` items.

## Core loop

`survival -> settlement growth -> better exploration -> external exploration / conquest -> NPCs / resources / technology -> settlement growth`

Hard rules:

- one shared settlement per world/server;
- server-authoritative state;
- real Minecraft ItemStacks are the resource authority;
- player-made buildings are not scanned into Frontier functional buildings;
- repetitive production/hauling/job assignment is automated;
- loaded areas visibly work and move;
- do not force-load chunks merely to continue simulation;
- do not silently destroy player containers, fluids, valuable blocks or unrelated builds;
- companion mods provide adventure/content breadth while Frontier remains settlement/progression glue.

## Controls

No new Alpha.49 key was added.

- `B` — settlement/infrastructure palette;
- `R` — rotate an ordinary building placement;
- `Enter` — confirm the current building/road/outpost/civil-work selection step;
- `Backspace` — reset the road start or Alpha.49 civil-work first corner.

Alpha.49 uses the existing B palette entry `토목 평탄화`: `Enter first corner -> Enter opposite corner/approve`. The first corner's Y is the target grade plane.

## Functional building families

The functional family count remains exactly **15**:

1. house;
2. lumber camp;
3. farm;
4. quarry;
5. mine;
6. warehouse;
7. construction office;
8. blacksmith;
9. workshop;
10. advanced workshop;
11. guard post;
12. watchtower;
13. barracks;
14. market;
15. cart station.

Alpha.40–49 deepen existing systems rather than inventing meaningless 16th–20th buildings.

## Physical construction and logistics

Ordinary functional construction remains:

`preview -> terrain/overlap/cost validation -> physical grading -> material hauling -> phased construction -> completion`

The construction presentation invariant remains: **builder walks from actual settlement storage carrying real wood/stone stacks**.

- Alpha.38 construction office physically stages wood/stone with one loaded supply runner; `SettlementConstructionService` remains the building/grading authority.
- Alpha.44 ordinary building footprints support bounded medium terrain: span0–2 normal grading, span3–4 `지형 공사 포함`, >4 rejected. Deep exposed foundations use real hauled/staged retaining stone, not free cobblestone.
- Alpha.27 road logistics remains the **single authority for outpost transport**. Transport workers belong to a specific outpost and pause at unloaded route boundaries instead of teleporting or force-loading.
- Alpha.34 cart station raises physical freight capacity without creating another logistics controller.
- Alpha.35 adds one-block road stairs and bounded short-water bridges using real stone.
- Alpha.46 waterfront wood reverse supply and Alpha.41 military food/metal reverse supply reuse that same transporter; military supply takes precedence.

## Alpha.49 — bounded selected-area balanced earthworks

Alpha.49 implements the original v0.2 late-game selected-area flatten/cut/fill concept as a deliberately bounded first pass.

- unlock: shared settlement at `DOMAIN` + at least one completed construction office;
- opened from the existing `B` construction palette; no new management screen or key;
- first selected corner fixes the target grade Y, second corner defines the X/Z rectangle;
- maximum footprint **9×9**;
- every selected column may require at most **4 blocks of cut** or **4 blocks of fill**;
- both selected corners must remain within 28 blocks of the player and the project center within 80 blocks of the settlement center;
- the complete area must already be loaded;
- the area may not overlap the authoritative stockpile, completed buildings, roads or outposts;
- block entities, fluids, ores, structures/player blocks and other non-natural terrain cause rejection;
- the server computes the full cut/fill volume before approval;
- first-pass balanced-earth rule: **initial fill volume may not exceed initial cut volume**;
- real natural blocks removed by the shared construction worker produce **no item drops**. Each successful cut adds one project-local earth unit;
- each coarse-dirt fill consumes one project-local earth unit;
- the project-local earth bank is persisted only for save/reload correctness. It is not an ItemStack, settlement resource, currency, cargo or reusable balance and disappears with project completion;
- the existing shared `건설 주민` physically walks to work cells; Alpha.49 does not spawn a second builder authority;
- building, road and outpost starts are blocked while the civil-work project is active;
- active project cells around grade Y are break-protected from ordinary player mining;
- compact HUD/context reports `선택영역 절토` / `선택영역 성토`, percentage, initial cut/fill and current project-local earth;
- no `destroyBlock`, loose-drop excavation, teleport inventory, chunk force-load or virtual soil generation path is used.

Alpha.49 therefore moves `선택영역 절토/성토` from unimplemented to **completed/partial**. It does **not** claim imported-fill projects, retaining-heavy terraces, large ravine works, long bridges, tunnels, monumental terraforming or mountain-scale deletion.

## Alpha.48 — supplied humanoid military presentation

Alpha.48 replaces the visible town/remote military golem presentation without changing the proven supply/combat economy.

- `FrontierSoldierEntity` is a Frontier-owned entity type that **extends `IronGolem`** and inherits its server combat body/attributes;
- the client renders that entity as a humanoid player-shaped soldier;
- the visible service sword is a **client render-state-only ItemStack** and is never inserted into server equipment, settlement storage or loot;
- barracks still own exactly **3 supplied slots**, each replacement costing **8 real food + 2 real metal**;
- a qualifying dangerous-region outpost still owns at most one sentry, replacement costing **6 real food + 2 real metal** from its local stockpile;
- older loaded tagged Iron Golem soldiers/sentries migrate **1:1** to `FrontierSoldierEntity`, preserving tags/name/health and charging no recruitment cost again;
- drop protection remains active;
- no Better Combat or Weapons Expanded Java class becomes a hard dependency.

Alpha.48 does **not** claim that soldiers physically consume/equip Weapons Expanded items yet. A real physical armory/loadout loop remains unfinished and must not become per-soldier micromanagement.

## Alpha.47 — domain relic reforging

The existing advanced workshop gains a DOMAIN-only second high-tier use rather than a new building/currency.

- original Alpha.39 path remains: unenchanted recognized external weapon + relic1 + metal4 -> validated power30 forge;
- DOMAIN reforge: already-enchanted recognized external weapon + relic2 + metal8 -> compatible power40 additions;
- the same specialist physically fetches real metal;
- every pre-existing enchantment must remain at the same or higher level;
- if no compatible improvement exists, no relic/metal is consumed;
- no hard Weapons Expanded dependency or virtual reforge points.

## Alpha.45–46 exploration and waterfront bridges

Alpha.45 connects normal exploration/conquest back to shared progression without creating a currency:

- only online players' current already-loaded positions are examined;
- unique external structure type counts once;
- direct player Dragon/Wither kills and qualifying external strong-enemy types count once;
- score is capped at8 and is non-spendable metadata;
- legacy tier routes remain accepted; exploration only provides alternate accelerator routes;
- no global locate scan, chunk generation or loot minting.

Alpha.46 deepens Alpha.40 fishing:

- qualifying fishing outpost builds a small persisted landing from real local wood;
- shortage can be reverse-supplied by the same road transporter;
- dedicated waterfront barrel only: **16 real cod/salmon -> 1 real emerald**;
- ordinary outpost stockpile is never auto-sold;
- no boat logistics or second transport authority.

## Alpha.42–44 simulation and compact information

- Alpha.42 stores bounded unloaded **work-time debt**, not virtual resources/cargo, and redeems it only through later real loaded work;
- Alpha.43 provides active-project HUD, bounded right-side notices and optional Jade context without new gameplay authority;
- Xaero Minimap only changes HUD positioning. Exact 26.4.2 investigation found the historical public `WaypointsManager` API absent, so marker synchronization is still not claimed;
- **tier-visible public works** remain allowed only where they are safe, loaded and non-farmable.

## External-content contract

`COMPANION_LOCK.json` remains `candidate_runtime_lock`. The current candidate stack includes Terralith/Lithostitched, Dungeons and Taverns, Repurposed Structures, Better Combat and libraries, Weapons Expanded, Lootr, Sophisticated Backpacks/Core, Jade and Xaero's Minimap.

Frontier must still boot without optional companions. Alpha.49 only reads already-loaded terrain/block state and does not add a Terralith/worldgen Java dependency.

## Validation

Canonical CI runs:

1. cumulative Alpha.23–49 source/runtime audit;
2. canonical README/plan/gap audit after docs are bound;
3. Java 25 clean Gradle build against Minecraft26.2 / NeoForge26.2.0.38-beta;
4. runtime JAR verification;
5. canonical source SHA/result recording.

Automated validation does not replace final real Minecraft acceptance. Important final play checks include civil-work pathing/save-reload/exploit resistance, Alpha.48 humanoid render/attack presentation, external weapon breadth, waterfront pathing/trade balance, dangerous-outpost combat, deferred-work pacing, Jade/Xaero visual coexistence, two-player shared-state behavior and full candidate companion-stack fresh-world launch.

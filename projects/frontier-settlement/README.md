# Frontier Settlement

Minecraft Java 26.2 / NeoForge 26.2 cooperative survival settlement-growth mod.

Canonical direction: `ORIGINAL_DESIGN_v0.2.md` + `CANONICAL_PLAN.md`. Remaining original-scope gaps are tracked in `COMPLETION_GAP_AUDIT.md`.

## Current version: 0.1.0-alpha.50

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

No new Alpha.50 key was added.

- `B` — settlement/infrastructure palette;
- `R` — rotate an ordinary building placement;
- `Enter` — confirm the current building/road/outpost/civil-work selection step;
- `Backspace` — reset the road start or civil-work first corner.

Alpha.50 keeps the existing B palette entry `토목 평탄화`: `Enter first corner -> Enter opposite corner/approve`. The first corner's Y is the target grade plane.

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

Alpha.40–50 deepen existing systems rather than inventing meaningless 16th–20th buildings.

## Physical construction and logistics

Ordinary functional construction remains:

`preview -> terrain/overlap/cost validation -> physical grading -> material hauling -> phased construction -> completion`

The construction presentation invariant remains: **builder walks from actual settlement storage carrying real wood/stone stacks**.

- Alpha.38 construction office physically stages wood/stone with one loaded supply runner; `SettlementConstructionService` remains the building/grading authority.
- Alpha.44 ordinary building footprints support bounded medium terrain: span0–2 normal grading, span3–4 `지형 공사 포함`, >4 rejected. Deep exposed foundations use real hauled/staged retaining stone, not free cobblestone.
- Alpha.27 road logistics remains the **single authority for outpost transport**. **Transport workers belong to a specific outpost** and **pause at unloaded route boundaries** instead of teleporting or force-loading.
- Alpha.34 cart station raises physical freight capacity without creating another logistics controller.
- Alpha.35 adds one-block road stairs and bounded short-water bridges using real stone.
- Alpha.46 waterfront wood reverse supply and Alpha.41 military food/metal reverse supply reuse that same transporter; **군사 전초도 같은 도로 운송자가 역방향 보급**하고 **위험지역 군사 역할이 우선**이다.

## Alpha.50 — 13×13 civil work with physical imported fill

Alpha.50 is the first expansion of Alpha.49's bounded selected-area earthwork. It is still settlement-scale civil work, not unrestricted WorldEdit.

- unlock remains `DOMAIN` + at least one completed construction office;
- existing `B / Enter / Backspace` interaction is reused, with no new management screen, key, currency or BuildingType;
- first selected corner fixes target grade Y; second corner defines X/Z bounds;
- maximum footprint **13×13**;
- every selected column may require at most **5 blocks of cut** or **5 blocks of fill**;
- both selected corners must remain within 36 blocks of the player and the project center within 96 blocks of settlement center;
- complete selected area must already be loaded;
- authoritative stockpile, completed buildings, roads and outposts may not overlap the project;
- block entities, fluids, ores, structures/player blocks and other non-natural terrain cause rejection;
- each successful real cut removes one natural world block without item drops, then adds one project-local `earthBank` unit;
- fill consumes on-site `earthBank` first;
- when fill exceeds on-site cut, the missing volume is `max(0, fill - cut)` and approval requires enough real `DIRT` / `COARSE_DIRT` in loaded shared settlement storage;
- the existing shared construction worker walks to the concrete storage container, extracts a bounded batch of at most 16 real dirt/coarse-dirt ItemStacks into MAINHAND, walks back to the selected work cell, places the physical block, and only after successful placement shrinks one carried item;
- if players remove required dirt from storage after approval, work pauses until real supply exists again; no free substitute is minted;
- remaining imported demand is re-evaluated from the current physical site, so an externally changed/pre-filled cell cannot create an unnecessary final haul;
- if a project finishes while the worker still carries material, the worker physically returns the remaining ItemStack to a concrete loaded settlement container before the shared construction authority is released;
- save/reload preserves project phase, progress and project-local earth; carried ItemStacks remain physical entity inventory rather than a virtual balance;
- project-local `earthBank` is not an ItemStack, settlement resource, currency, cargo or reusable balance and disappears with project completion;
- the same `SettlementConstructionService.ensureBuilder` authority is reused; there is no second builder or civil economy;
- building, road and outpost starts are blocked while civil work is active;
- active project volume remains break-protected;
- no `destroyBlock`, `dropResources`, teleport inventory, chunk force-load or virtual soil generation path is used;
- compact project context/status exposes only current phase, on-site earth, remaining imported fill and actual storage supply state. **가상 토사 0**.

Alpha.50 therefore completes the **first physical imported-fill expansion**. It still does **not** claim retaining-heavy large terraces, ravine-scale works, long bridges, tunnels or monumental civil engineering. Mountain deletion and unrestricted WorldEdit-style terraforming remain outside scope.

## Alpha.49 — historical balanced-earth first pass

Alpha.49 is retained as historical implementation context, not the current limit. It established `DOMAIN + construction office`, B/Enter/Backspace selection, first-corner grade Y, loaded-area/protection checks, the shared builder, and project-local non-economic earth accounting with a **9×9 / ±4** envelope. Its first-pass rule rejected `fill > cut`. Alpha.50 intentionally supersedes only those civil capacity/import rules while preserving the authority and safety contracts.

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

Frontier must still boot without optional companions. Alpha.50 reads already-loaded ordinary terrain and physical storage only; it adds no Terralith/worldgen Java dependency.

## Validation

Canonical Alpha.50 CI order:

1. cumulative Alpha.23–50 source/runtime audit, preserving historical Alpha.23–49 files while superseding only the intended civil limits;
2. Alpha.50 canonical README/plan/gap docs audit;
3. `git diff --check` + clean-worktree check;
4. Java 25 `clean build` against Minecraft 26.2 / NeoForge 26.2.0.38-beta;
5. runtime JAR verification and SHA-256;
6. exact source/docs SHA + CI result commit/run recording.

Automated validation does not replace final real Minecraft acceptance. Important final play checks include Alpha.50 imported-fill depletion/resupply/save-reload/cargo-return behavior, civil pathing/exploit resistance, Alpha.48 humanoid render/attack presentation, external weapon breadth, waterfront pathing/trade balance, dangerous-outpost combat, deferred-work pacing, Jade/Xaero visual coexistence, two-player shared-state behavior and full candidate companion-stack fresh-world launch.

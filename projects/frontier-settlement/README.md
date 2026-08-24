# Frontier Settlement

Minecraft Java 26.2 / NeoForge 26.2 cooperative survival settlement-growth mod.

Canonical direction: `ORIGINAL_DESIGN_v0.2.md` + `CANONICAL_PLAN.md`. Remaining original-scope gaps are tracked in `COMPLETION_GAP_AUDIT.md`.

## Current version: 0.1.0-alpha.48

Frontier Settlement owns the shared settlement, physical construction, residents, production, roads, outposts, logistics, defense infrastructure and territory progression. Companion mods remain the preferred source of biome, dungeon, structure, combat, weapon and loot breadth.

This is a broad playable alpha, not original-v0.2 completion. Do not call it complete while `COMPLETION_GAP_AUDIT.md` still contains meaningful `부분/미구현` items.

## Core loop

`survival -> settlement growth -> better exploration -> external exploration / conquest -> NPCs / resources / technology -> settlement growth`

Hard rules:

- one shared settlement per world/server;
- server-authoritative state;
- actual Minecraft ItemStacks remain resource authority;
- player builds are never scanned into functional Frontier buildings;
- repeated hauling/production/job assignment is automated;
- loaded areas visibly work; unloading never justifies force-loading or teleport cargo;
- companion integrations stay soft where practical;
- no new management screen/key/currency merely to expose backend depth.

## Controls

- `B` — settlement palette;
- `R` — rotate current building placement;
- `Enter` — confirm active building/road/outpost placement;
- `Backspace` — road reset/cancel.

Alpha.43–48 add no new gameplay key or giant management dashboard.

## Functional building families

Current functional families: **15**.

1. house
2. lumber camp
3. farm
4. quarry
5. mine
6. warehouse
7. construction office
8. blacksmith
9. workshop
10. advanced workshop
11. guard post
12. watchtower
13. barracks
14. market
15. cart station

The original target is roughly 15–20 meaningful families. Alpha.40–48 deepen existing systems rather than inventing meaningless extra buildings.

## Physical construction and territory

Construction remains:

`approval -> physical grading -> real material hauling -> foundation/frame/walls/roof/finish -> completion`

- small terrain span 0–2 uses the established grading path;
- Alpha.44 accepts bounded span 3–4 as `지형 공사 포함`;
- larger/unsafe/fluid/block-entity sites are rejected;
- deep exposed support uses cobblestone retaining/foundation only after real settlement stone is hauled, staged and consumed;
- retaining surcharge is bounded to 96/project;
- no `destroyBlock` free-drop excavation or hidden resource minting;
- arbitrary selected-area cut/fill, large ravine engineering and tunnels remain unfinished.

Roads/outposts also remain physical. Alpha.27 road logistics is the **single authority for outpost transport**.

## Outposts and loaded/offline behavior

- lumber/quarry/mining/agriculture specialization;
- Alpha.40 loaded shoreline fishing overlay produces real cod/salmon into the physical outpost stockpile;
- Alpha.41 loaded dangerous-region military overlay maintains one supplied sentry and overrides fishing while danger is active;
- Alpha.42 persists bounded work-time debt only, never virtual wood/stone/ore/fish/food/cargo;
- Alpha.46 qualifying fishing outposts build a bounded real-wood landing, with the existing road transporter able to reverse-supply wood after military food/metal supply priority;
- dedicated waterfront trade barrel converts **16 real cod/salmon -> 1 real emerald**; ordinary stockpile fish are never auto-sold;
- no boat logistics, force-load, teleport inventory, virtual trade points or second transport authority.

## External exploration and high-tier crafting

Alpha.45 observes already-loaded external structure pieces and directly attributed conquest milestones without `/locate`, global radar or chunk generation. Progress is unique-by-type and non-spendable; legacy tier routes remain valid while exploration offers alternate accelerator routes.

High-tier role split:

- market = relic -> physical trade value;
- normal workshop = metal -> external weapon repair;
- Alpha.39 first advanced forge = unenchanted recognized external weapon + **1 real expedition relic + 4 real metal** -> validated compatible power30 forge;
- Alpha.47 domain reforge = already-enchanted Frontier-recognized external weapon + **2 real expedition relics + 8 real metal** -> compatible new enchantment additions only.

Alpha.47 validates the copied result before spending resources. Existing enchantments are never removed or downgraded; when there is no compatible new improvement, **no relic or metal is consumed**.

## Alpha.48 — supplied humanoid military presentation

Alpha.48 changes how the supplied barracks garrison and dangerous-region sentry are represented without inventing free equipment or rewriting the proven military economy.

- new `FrontierSoldierEntity` is a distinct Frontier entity type that **extends IronGolem**;
- its server combat goals/attributes preserve the proven Iron Golem combat body instead of switching to a naturally hostile humanoid mob AI;
- entity dimensions are humanoid-sized and the client renders the Frontier soldier with a standard humanoid/player model;
- the visible service sword is a **client render-state-only ItemStack**;
- the visual sword is never inserted into the server entity, settlement storage, equipment inventory or loot table;
- therefore Alpha.48 does not mint a free weapon, alter settlement resources or create weapon drops;
- barracks still own exactly **3 supplied slots** and each new slot still costs **8 real food + 2 real metal**;
- dangerous-region outposts still own at most one sentry and replacement still costs **6 real food + 2 real metal** from the local physical stockpile;
- tagged military drops are still cleared;
- existing loaded tagged Iron Golem soldiers/sentries from older saves migrate **1:1** to `FrontierSoldierEntity`, preserving assignment tags/name/health and charging no recruitment cost again;
- no civilian population/housing is added by the migration;
- no Better Combat or Weapons Expanded Java class becomes a hard dependency;
- Better Combat / Weapons Expanded remain companion candidates rather than Frontier boot requirements.

Alpha.48 completes the **humanoid visible military body** first. It does **not** claim that soldiers physically consume/equip Weapons Expanded items yet. A future physical armory/loadout loop must use actual ItemStacks and must not create free weapons or a second military-capacity path.

## Compact information layer

Alpha.43 keeps presentation separate from gameplay authority:

- compact resource/project HUD;
- right-side bounded notices;
- optional Jade context under isolated compat code;
- Xaero-aware HUD offset only.

True Xaero settlement/outpost marker synchronization is still not claimed because the locked Xaero 26.4.2 artifact does not expose the historical public `WaypointsManager` seam used by older integrations.

## Companion candidate lock

`COMPANION_LOCK.json` remains `candidate_runtime_lock` until the full client/server pack is launched together.

Current candidate set includes Terralith, Lithostitched, Dungeons and Taverns, Repurposed Structures, Better Combat, Cloth Config API, Player Animation Library, Weapons Expanded, Lootr, Sophisticated Backpacks/Core, Jade and Xaero's Minimap.

Frontier must still boot without optional companions unless a later explicit hard dependency is justified.

## Validation

Canonical CI performs:

1. cumulative established source invariants plus the current Alpha.48 military-presentation audit;
2. current canonical document audit once synchronized;
3. Java 25 clean Gradle build;
4. runtime JAR verification;
5. artifact upload and exact-source CI result recording.

The source/API Alpha.48 slice has already compiled successfully in Java 25. Final canonical acceptance additionally requires README/CANONICAL_PLAN/COMPLETION_GAP_AUDIT to match the same source SHA.

Automated validation does not replace final Minecraft acceptance for humanoid render quality/attack animation, legacy military migration, Better Combat/Weapons Expanded coexistence, waterfront pathing, unloaded-work pacing, large terrain cases, Jade/Xaero visuals, multiplayer sharing or the full companion fresh-world pack.

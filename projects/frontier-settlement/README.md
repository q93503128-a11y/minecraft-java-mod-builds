# Frontier Settlement

Minecraft Java 26.2 / NeoForge 26.2 cooperative survival settlement-growth mod.

Canonical direction: `ORIGINAL_DESIGN_v0.2.md` + `CANONICAL_PLAN.md`. Remaining original-scope gaps are tracked in `COMPLETION_GAP_AUDIT.md`.

## Current version: 0.1.0-alpha.65

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

No new Alpha.65 key was added.

- `B` — settlement/infrastructure palette;
- `R` — rotate an ordinary building placement;
- `Enter` — confirm the current building/road/outpost/civil-work selection step;
- `Backspace` — reset the road start or civil-work first corner.

Alpha.51 keeps the existing B palette entry `토목 평탄화`: `Enter first corner -> Enter opposite corner/approve`. The first corner's Y is the target grade plane.

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

Alpha.40–65 deepen existing systems rather than inventing meaningless 16th–20th buildings.

## Physical construction and logistics

Ordinary functional construction remains:

`preview -> terrain/overlap/cost validation -> physical grading -> material hauling -> phased construction -> completion`

The construction presentation invariant remains: **builder walks from actual settlement storage carrying real wood/stone stacks**.

- Alpha.38 construction office physically stages wood/stone with one loaded supply runner; `SettlementConstructionService` remains the building/grading authority.
- Alpha.44 ordinary building footprints support bounded medium terrain: span0–2 normal grading, span3–4 `지형 공사 포함`, >4 rejected. Deep exposed foundations use real hauled/staged retaining stone, not free cobblestone.
- Alpha.27 road logistics remains the **single authority for outpost transport**. **Transport workers belong to a specific outpost** and **pause at unloaded route boundaries** instead of teleporting or force-loading.
- Alpha.34 cart station raises physical freight capacity without creating another logistics controller.
- Alpha.35 adds one-block road stairs and bounded short-water bridges using real stone. Alpha.52 extends that same road authority to bounded 24-cell long-water/dry-ravine bridge runs with persisted physical stone piers.
- Alpha.46 waterfront wood reverse supply and Alpha.41 military food/metal reverse supply reuse that same transporter; **군사 전초도 같은 도로 운송자가 역방향 보급**하고 **위험지역 군사 역할이 우선**이다.

## Alpha.65 — exact local civilian cargo death recovery

Alpha.65 closes the remaining deterministic death-loss gap for Frontier civilians that physically carry settlement resources. It adds no new job, inventory or recovery ledger.

- lumber, farm, quarry and mine workers now receive a persistent Frontier resource-worker entity tag when newly recruited; pre-Alpha.65 saves remain recognized by their existing exact Frontier worker names;
- workshop artisans are recognized through their existing workshop assignment tag;
- when one of those managed civilians dies, vanilla equipment-drop randomness is cleared and its current MAINHAND ItemStack is emitted exactly once as a physical world drop;
- an empty MAINHAND creates no item; death never refunds the worker's food recruitment cost or mints the resources it had already deposited;
- the rule covers physically harvested logs/wheat/stone/ore and workshop metal that has actually left shared storage;
- road-bound outpost transporters are explicitly excluded from this handler and retain the dedicated Alpha.63 exact-cargo recovery path, preventing double recovery;
- active building/road public-works builders remain governed by their existing invulnerable active-project lifecycle rather than a second cargo-death system;
- no new SavedData field, virtual cargo, refund currency, force-load, teleport, worker-management UI or logistics authority is introduced.

This closes the statically reproducible local civilian MAINHAND loss boundary. Repeated death/replacement, save/reload, route unload and two-client runtime acceptance remain real-play work rather than being claimed complete.

## Alpha.64 — atomic food-funded worker arrivals

Alpha.64 hardens resident replacement/recruitment before long two-player acceptance; it adds no new job or management UI.

- ordinary production workers, workshop artisans and outpost-assigned transporters now expose real `addFreshEntity` success before arrival food/population can commit;
- each path first requires loaded shared storage with at least the existing 4-food arrival cost, then creates the candidate entity, then consumes the same real food, and only then increments shared population;
- failed entity insertion consumes no food and adds no population;
- if physical food consumption unexpectedly fails after a successful entity insertion, that just-created worker is discarded and population remains unchanged;
- workshop/outpost assigned-worker spawn also rechecks the current loaded assignment immediately before entity insertion, so a stale missing-worker observation cannot knowingly create a second assignment;
- outpost transport replacement remains the same resident-attraction path and the same road-bound logistics authority; no instant cargo restoration or virtual replacement inventory exists;
- Alpha.63 exact MAINHAND cargo recovery on transporter death remains unchanged;
- **Transport workers belong to a specific outpost**, **pause at unloaded route boundaries**, Alpha.27 remains the **single authority for outpost transport**, and **there is still only one authority for long-distance outpost transport**;
- no new save field, worker type, route controller, key, UI, currency, force-load or teleport.

This closes deterministic spawn/food/population transaction gaps, but repeated death/replacement, unload/reload and two-client runtime acceptance remain real-play items.

## Alpha.63 — transport transaction hardening

Alpha.63 hardens the existing Alpha.27/41/62 physical outpost transporter for long-session failure edges without adding another logistics system.

- military weapon demand is rechecked at the **actual outpost delivery point**, not trusted from the town departure decision;
- if the sentry became armed or another recognized weapon reached the outpost while one was in flight, the transporter keeps that exact weapon in MAINHAND, clears only the military-supply trip state, and returns it through the existing road/town-deposit path;
- the stale weapon is never inserted as a second reserve copy, deleted, converted to a number or teleported;
- a tagged outpost transporter death clears vanilla equipment/drop-chance ambiguity and re-adds its exact carried MAINHAND ItemStack once as a recoverable world drop;
- this death rule applies equally to normal outpost cargo and food/metal/wood/weapon reverse-supply cargo, preventing silent physical cargo loss;
- worker tags, MAINHAND equipment and the persisted road remain the authority across normal entity save/reload; no new SavedData field or weapon-specific trip tag is introduced;
- **Transport workers belong to a specific outpost**, **pause at unloaded route boundaries**, and **군사 전초도 같은 도로 운송자가 역방향 보급** remains true;
- Alpha.27 remains the **single authority for outpost transport** and **there is still only one authority for long-distance outpost transport**;
- no new worker, route controller, building, key, UI, currency, force-load, teleport or hard companion dependency is added.

This closes two statically reproducible no-loss/no-dup edges. Long save/reload, route-unload and two-player runtime acceptance is still not claimed.

## Alpha.62 — road-bound remote sentry physical armament

Alpha.62 closes the remote half of the physical military armory without creating a second logistics system.

- only an active dangerous **general** outpost with an existing unarmed sentry can request a weapon;
- the existing military reverse-supply order remains **food reserve first -> metal reserve second -> weapon third**;
- weapon demand is exactly one and becomes zero if the sentry is already armed or an external weapon is already waiting in the outpost stockpile;
- the same outpost-assigned transporter walks the existing persisted road, extracts one real Frontier-recognized external weapon from loaded shared settlement storage, carries that exact MAINHAND stack, and inserts it into the exact outpost stockpile;
- no direct town-storage -> sentry transfer exists. After combat ends, the sentry walks locally to its own stockpile and extracts exactly one real weapon into MAINHAND;
- **위험지역 군사 역할이 우선**: an active threat always wins over an equipment walk, and food/metal survival supply wins over the weapon cargo;
- sentry death still clears body/iron drops but now restores the exact physically equipped external weapon once for recovery instead of deleting it;
- if the military overlay ends during a supply trip, existing transporter behavior keeps the carried ItemStack physical and returns it through the same route rather than teleporting or minting a replacement;
- **군사 전초도 같은 도로 운송자가 역방향 보급**, **Transport workers belong to a specific outpost**, and they **pause at unloaded route boundaries**;
- Alpha.27 remains the **single authority for outpost transport** and **there is still only one authority for long-distance outpost transport**;
- no new trip tag, save field, worker, route controller, building, key, UI, currency, force-load, teleport or hard Weapons Expanded dependency is introduced.

This implements the planned remote physical-weapon supply slice, but save/reload, route-unload and no-dup behavior still require real-play acceptance.

## Alpha.61 — rollback-safe outpost grading

Alpha.61 closes the remaining old-style terrain mutation in physical outpost construction before save/reload acceptance.

- outpost grading keeps the existing bounded 9×9 footprint and `MAX_FILL_DEPTH=2`; no larger terrain envelope is introduced;
- one grade cell snapshots every world block it actually changes;
- every clear/fill/final-grade `setBlock` result is checked;
- if any placement fails or a required cell becomes unloaded, every earlier successful mutation in that grade cell is restored in reverse order;
- `advanceOutpostConstruction()` runs only after the complete grade cell succeeds;
- no resource is minted or consumed by grading, so rollback never creates a refund currency or loose block drops;
- physical outpost blueprint construction remains the Alpha.55 `setBlock -> carried ItemStack consume -> step advance` authority;
- no new save field, worker, key, UI, building family, force-load, teleport, `destroyBlock` or `dropResources` path is added.

This is long-play/save hardening, not a claim that two-player runtime acceptance is finished.

## Alpha.60 — rollback-safe ordinary construction transactions

Alpha.60 closes a physical-authority gap found during long-play audit. Roads, outposts and civil work already commit resources after successful world mutation; ordinary buildings now follow the same rule.

- a build step first verifies that the physical site crate contains its exact wood/stone delta;
- when the blueprint target is empty, `setBlock` must succeed **before** the crate ItemStacks are consumed and the construction step advances;
- an unexpected consume failure after successful placement rolls that newly placed block back to its previous state and leaves the step unchanged;
- if the correct building block is already present, the step still consumes its normal material delta before advancing, so player pre-fill cannot bypass construction cost;
- Alpha.44 grade cells now record the original block states they change; every clear/foundation/support placement checks `setBlock` success;
- a failed grade mutation restores all successful partial changes and consumes no retaining stone;
- retaining/foundation stone is consumed only after the full grade-cell mutation succeeds; an unexpected consume failure rolls the complete grade cell back and does not advance the project;
- already-paid `finishIfValid` replacement remains a repair of a step whose material was previously committed, so Alpha.60 does not double-charge historical/current prepaid repair;
- no `destroyBlock`, loose drops, virtual refund, new worker, save field, key, UI or currency is introduced.

This improves deterministic long-play/save safety but still does **not** replace the required real two-player runtime acceptance.

## Alpha.59 — centralized single-project authority hardening

Alpha.59 fixes a service-layer exclusivity gap found while auditing the required two-player acceptance path. It does **not** claim that long two-player runtime testing is complete.

- new `SettlementProjectAuthority.anyActive(server, data)` is the single server-side gate for building, road, outpost and selected-area civil projects;
- the gate reads all four shared project states, including `SettlementCivilWorkData`, so civil work cannot be accidentally omitted from another service's internal guard;
- building `checkPlacement` and `startAt`, road `checkRoute`, outpost `checkPlacement` and `startAt`, and civil `check` and `start` all reuse the same authority;
- therefore a stale client preview, `/frontier` command path, or future direct service caller cannot create a second project merely because an outer UI/network pre-check was bypassed;
- NeoForge MAIN-thread serialization from Alpha.58 remains in place, so simultaneous confirms are processed sequentially and the later request sees the first request's newly active shared project;
- the change adds no project save format, no project queue, no second builder, no resource reservation ledger, no key/UI and no new companion dependency;
- existing physical ItemStack hauling and one shared construction worker remain unchanged.

This is another **pre-acceptance hardening** slice. The actual two-client long-survival, reconnect/save-reload and simultaneous-confirm play session is still required.

## Alpha.58 — multiplayer snapshot/session pre-acceptance hardening

Alpha.58 does **not** claim that the required long two-player survival acceptance has been completed. It closes deterministic multiplayer-state holes found before that real-play pass.

- the settlement is still one server-owned `SettlementData`; no per-player settlement copy/save authority is introduced;
- NeoForge 26.2 payload registration is now explicitly `.executesOn(HandlerThread.MAIN)`, making building/road/outpost/civil requests visibly serialized on the server main thread before each service revalidates current shared state;
- if a player logs into an already-founded world, their presence may make common storage loaded again. The server refreshes the physical storage ledger once and **broadcasts the same authoritative snapshot to every connected player**, not only the joiner;
- this closes a stale-HUD edge where an existing player could otherwise keep the pre-login resource snapshot after the joiner caused a ledger refresh;
- `ClientPlayerNetworkEvent.LoggingOut` now clears the client settlement snapshot/context initialization flags, all placement modes/previews, and queued settlement notices;
- moving from one server/world to another therefore cannot compare the previous world's tier/context against the new world and emit fake growth/completion notices;
- no payload schema change, new key, building, currency, player-specific progression or async mutation authority is added;
- existing server confirmation still wins if two players preview the same opportunity: the later MAIN-thread request rechecks the now-current shared project state instead of trusting its old client preview.

This is **pre-acceptance hardening** only. Long survival + two-player gameplay, reconnect/save-reload, simultaneous placement attempts and full companion-stack runtime remain explicit real-play acceptance work.

## Alpha.57 — automated physical barracks armament

Alpha.57 closes the first physical military armory/loadout slice without adding soldier-by-soldier menus or a 16th building.

- only loaded **town barracks** soldiers participate in this first slice; dangerous-region remote sentries remain unchanged until weapons can travel through the existing road transporter authority;
- an idle barracks soldier with an empty MAINHAND checks real shared settlement storage only when the ordinary storage authority is fully loaded;
- if a Frontier-recognized external weapon exists, the soldier walks to the **nearest concrete storage container within 160 blocks**; no teleport, force-load, virtual armory inventory or instant remote transfer;
- after reaching normal 3-block interaction range, the soldier extracts **exactly one real external weapon ItemStack** and equips it in vanilla `EquipmentSlot.MAINHAND`; damage/enchantments/components stay on that exact stack and vanilla Mob equipment persistence/sync owns save/reload/client state;
- hostile defense has priority over an armament trip, so soldiers do not abandon an active barracks threat to fetch gear;
- the humanoid renderer shows the real synced MAINHAND weapon when present and keeps Alpha.48's client-only iron service sword only as the un-upgraded fallback;
- soldier death still clears ordinary body/iron drops, but if a real external weapon was assigned, exactly that one stack is re-added as the sole recoverable military drop. The source stack previously left settlement storage, so this is recovery rather than minting;
- `/frontier status` adds only the loaded physically armed garrison count; no new screen/key/manual assignment list;
- `SettlementExternalContentService.isExternalWeapon` remains the soft registry recognizer, so no Weapons Expanded Java class or Better Combat class becomes a hard dependency;
- remote sentry weapon supply is **not** faked. When/if implemented, **군사 전초도 같은 도로 운송자가 역방향 보급** and **위험지역 군사 역할이 우선** must still hold;
- `Transport workers belong to a specific outpost`, `pause at unloaded route boundaries`, Alpha.27 remains the **single authority for outpost transport**, and **there is still only one authority for long-distance outpost transport**.

This is automated physical loadout rather than per-soldier micromanagement: players only put useful weapons into the same shared storage they already use.

## Alpha.56 — soft biome-aware outpost specialization

Alpha.56 uses the stable soft seam that was left open after Alpha.55: common NeoForge biome tags at the **already-loaded outpost center**. It never calls Terralith or another worldgen mod directly.

- the existing 12-block physical ore/log/field/exposed-stone survey remains the primary specialization authority;
- `IS_FOREST` / `IS_DENSE_VEGETATION` adds only **+8 log evidence**;
- `IS_PLAINS` / `IS_SAVANNA` adds only **+24 field evidence**;
- `IS_MOUNTAIN` / `IS_HILL` adds **+8 exposed-stone +1 ore evidence**;
- `IS_BADLANDS` / `IS_SANDY` adds only **+6 exposed-stone evidence** when the stronger mountain rule does not apply;
- these bonuses are deliberately below the existing specialization thresholds (ore4, logs24, field120, stone24), so a biome tag alone does not magically create a mine/farm/quarry/lumber outpost;
- datapacks/worldgen companions that correctly participate in common biome tags can influence the same survey automatically; missing Terralith or any other companion still boots and behaves normally;
- no biome id string allowlist, reflection, class reference, chunk generation, locator, force-load, virtual resource, new specialization family or new saved field is added;
- placement preview exposes only a compact environment label (`삼림/개활지/산악/건조 암지/중립`) beside the existing specialization candidate; no new dashboard/key;
- Alpha.55 survey/conquest knowledge stacks only as bounded evidence/cost context; actual outpost materials and construction remain physical ItemStacks;
- `Transport workers belong to a specific outpost`, `pause at unloaded route boundaries`, and Alpha.27 remains the **single authority for outpost transport**; **there is still only one authority for long-distance outpost transport**.

This closes the generic companion-biome-aware specialization gap through a common tag seam. Rare-NPC-specific value remains optional only if a similarly stable soft data seam appears.

## Alpha.55 — non-farmable exploration knowledge feeds existing outposts

Alpha.55 makes the already-persisted Alpha.45 discovery/conquest milestones matter after tier acceleration without creating another progression tree.

- unique external structure IDs yield a capped **survey level 0–3**; repeated copies of the same structure type still add nothing;
- survey knowledge does not create ores/logs/food. It only adds a small bounded evidence bias to the existing loaded 12-block outpost-specialization survey, making mining/lumber/agriculture/quarry roles slightly easier to recognize after real exploration;
- unique conquest target IDs yield a capped **conquest level 0–2**; repeated kills of the same boss type add nothing;
- each conquest level reduces a **new** outpost's physical construction total by only 4 wood + 2 stone, capped at 8 wood + 4 stone; the builder still walks from real loaded settlement storage and consumes actual ItemStacks through the existing outpost construction authority;
- Alpha.55 also closes a physical-authority hole in that existing outpost builder: world placement must succeed before carried wood/stone is consumed and the step advances, with rollback on an unexpected consume failure; final repair of missing priced blocks uses real material for Alpha.26+ physical projects, while historical prepaid saves keep their already-paid repair semantics;
- base outpost costs remain 72 wood + 48 stone and the minimum Alpha.55 explored cost is 64 wood + 44 stone;
- the benefit is computed directly from existing unique milestone lists, so old saves gain the correct value automatically and there is no new saved currency/claim flag;
- no structure locator, force-load, companion class reference, generated reward chest, free item, population grant, second economy or second logistics authority is added;
- compact `/frontier status` shows survey/conquest levels and the current physical new-outpost cost; no new dashboard or key is introduced;
- `Transport workers belong to a specific outpost`, `pause at unloaded route boundaries`, and Alpha.27 remains the **single authority for outpost transport**; **there is still only one authority for long-distance outpost transport**.

This is intentionally a small settlement-value bridge, not a generic RPG skill tree. Companion-specific biome/NPC seams remain a later optional depth pass only where a stable API/data seam exists.

## Alpha.54 — bounded one-bend tunnels and physical portals

Alpha.54 deepens the same automatic road/tunnel authority instead of making tunnels longer or adding another tool.

- the total automatic tunnel ceiling remains **24 centerline cells**;
- one tunnel run may contain at most **one 90-degree bend**, and both legs around that bend must contain at least **3 tunnel centers** so the corner is not a tiny accidental notch;
- the persisted centerline path + existing `PROFILE_TUNNEL=2` fully determines the bend after save/reload; there is no new save authority or route controller;
- the same width-3 / clear-height-3 conservative excavation remains in force, including loaded-only, non-ore, non-fluid, non-container, non-player-block validation;
- each tunnel run receives exactly **two deterministic stone-brick portal frames**, one at each end. The frame envelope is **5 blocks wide × 4 blocks high** and is validated before approval;
- portal frame cells are included in the no-drop physical excavation phase, then the same road builder carries real settlement stone and places `STONE_BRICKS` during the established paving phase;
- the two frames add **22 real-stone units per tunnel run**; no visual-only/free portal block, virtual stone or excavated-stone refund exists;
- active tunnel interior/floor/portal cells remain project-protected, and unsafe edits pause rather than being overwritten;
- completed bent tunnels are still ordinary `RoadSegment`s. Alpha.27 remains the **single authority for outpost transport**, **Transport workers belong to a specific outpost**, and workers **pause at unloaded route boundaries**;
- no new key, building family, currency, dashboard, second builder, second logistics authority, force-load or teleport is introduced.

Alpha.54 closes the first qualitative curved/monumental tunnel slice without raising the destruction ceiling. Very-long bores, underground stations and unrestricted mountain deletion remain outside the intended product.

## Alpha.53 — bounded straight road tunnels

Alpha.53 fills the next civil-engineering gap without adding a tunnel dashboard, new key or second construction authority.

- the existing road endpoint flow can automatically choose a **straight tunnel up to 24 centerline cells** when a loaded cliff/ridge rises at least **4 blocks** above nearly level entry/exit shoulders;
- tunnel road profile is persisted in the existing road state as `PROFILE_TUNNEL=2`; old saves decode exactly as before;
- grading transitions through a persisted `TUNNEL_STEP_OFFSET=1_500_000` phase before the established 2M+ physical paving phase;
- the tunnel clear section is 3 blocks wide and **3 blocks high above the road floor**;
- all affected floor/headspace cells must already be loaded and consist only of conservative natural non-ore blocks; block entities, fluids, ores, caves/air pockets and player/non-natural blocks reject the automatic tunnel;
- the same shared road builder works from the previous open tunnel floor and removes one validated natural block at a time with `setBlock(AIR)` and **no drops**; there is no virtual excavated-stone credit or farmable quarry path;
- active tunnel cells are break-protected while the road project owns them, and external unsafe changes pause work instead of being buried;
- after excavation, the same physical paving phase consumes real settlement stone; tunnel centers add a bounded stone surcharge rather than a virtual tunnel currency;
- tunnel works require the existing **frontier-town + construction office** stage;
- no force-load, teleport, `destroyBlock`, `dropResources`, second builder, second road authority or companion hard dependency is introduced.

This is the first bounded road-tunnel slice. Alpha.54 adds the bounded one-bend/portal pass; very long bores, underground stations and unrestricted mountain deletion remain outside the intended product.

## Alpha.52 — bounded long bridges and ravine crossings

Alpha.52 advances the first item left after Alpha.51 without inventing a new road system. The existing road endpoint flow, shared builder and real settlement stone authority are reused.

- ordinary Alpha.35 short-water bridges up to 6 centerline cells remain supported;
- a straight water or abrupt dry-ravine bridge run may now span at most **24 centerline cells**;
- dry ravine detection requires a bounded depression at least **4 blocks** below compatible shoulders;
- bridge approaches still require bank/shoulder height difference of at most 1 block;
- bridge runs needing structural support receive two stone pier columns at bounded stations rather than becoming floating decks;
- every planned pier cell is persisted in `RoadConstructionState.bridge_supports`, so save/reload keeps the exact same support plan;
- each pier must reach natural support ground within **12 blocks** below the deck. Unloaded cells, containers, lava/other fluids, player structures and non-natural support reject the route;
- pier-required long bridges unlock at the existing village stage; no new key, building family, currency or dashboard is added;
- the same road builder walks to real settlement storage, extracts real stone ItemStacks, and physically builds deck/support placements;
- paving now treats world placement and ItemStack consumption atomically: successful `setBlock` precedes carried-stone shrink and state advance, with rollback if consumption unexpectedly fails;
- final repair for Alpha.25+ physical roads no longer places missing road/bridge blocks for free; each such repair fetches and consumes a real stone ItemStack, while historical prepaid road saves keep their already-paid repair semantics to avoid double charging;
- no force-load, teleport logistics, second builder, second road authority or second outpost transport authority is introduced.

Alpha.52 completes the **first bounded long-bridge/ravine-crossing slice**. Tunnels, more complex curved/deeper monumental crossings and final real-play acceptance remain unfinished.

## Alpha.51 — 17×17 retaining-heavy terraces

Alpha.51 is the next bounded civil-engineering pass. It expands the same late-game tool instead of adding a new key, building family, currency or dashboard.

- unlock remains `DOMAIN` + completed construction office and the existing `B / Enter / Backspace` flow;
- maximum selected footprint is **17×17**, with at most **7 blocks of cut** or **7 blocks of fill** per column;
- selected corners must stay within 44 blocks of the player and project center within 112 blocks of settlement center;
- the selected area plus its one-block retaining protection ring must already be loaded and clear of Frontier infrastructure, containers, fluids and non-natural/player blocks;
- Alpha.50 project-local earth-first fill and physical imported `DIRT` / `COARSE_DIRT` hauling remain unchanged;
- after cutting and before filling, a fill-facing outer edge that would stand at least **3 blocks** above natural exterior ground receives a one-block-wide retaining wall;
- retaining height is capped at **7 blocks**. Deeper ravines remain rejected rather than silently bridged or filled;
- the exact retaining material is real `COBBLESTONE`: approval checks actual loaded shared storage, the shared construction worker walks to the exact container, extracts at most 16 cobblestone into MAINHAND, walks to the wall cell, and only after successful `setBlock` shrinks one ItemStack;
- a mid-project cobblestone shortage pauses work until real supply returns; no generic stone number becomes free cobblestone;
- civil state persists the retaining phase and initial retaining block count for save/reload correctness;
- cut -> retaining wall -> fill -> carried-material return all reuse the one shared construction worker and one construction authority;
- the active selected area plus retaining ring is break-protected while work is active;
- no force-load, teleport inventory, `destroyBlock`, `dropResources`, virtual stone, second builder or second economy is introduced.

Alpha.51 completed the first **retaining-heavy large-terrace** slice. Alpha.52 now adds a bounded first long-bridge/ravine-crossing pass; tunnels and more complex monumental civil engineering remain unfinished. Unrestricted WorldEdit and mountain deletion remain outside scope.

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

Alpha.50 completed the **first physical imported-fill expansion** but did not yet claim retaining-heavy large terraces. Alpha.51 supersedes the current capacity with bounded physical cobblestone retaining terraces; ravine-scale works, long bridges, tunnels and monumental civil engineering remain unfinished.

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

Frontier must still boot without optional companions. Alpha.52 reads already-loaded ordinary terrain and physical storage only; it adds no Terralith/worldgen Java dependency.

## Validation

Canonical Alpha.53 CI order:

1. cumulative Alpha.23–53 source/runtime audit, preserving historical Alpha.23–50 files while superseding only the intended civil limits/retaining expansion;
2. Alpha.53 canonical README/plan/gap docs audit;
3. `git diff --check` + clean-worktree check;
4. Java 25 `clean build` against Minecraft 26.2 / NeoForge 26.2.0.38-beta;
5. runtime JAR verification and SHA-256;
6. exact source/docs SHA + CI result commit/run recording.

Automated validation does not replace final real Minecraft acceptance. Important final play checks include Alpha.51 retaining-plan boundary/cobblestone depletion/save-reload behavior, Alpha.50 imported-fill depletion/resupply/cargo-return behavior, civil pathing/exploit resistance, Alpha.48 humanoid render/attack presentation, external weapon breadth, waterfront pathing/trade balance, dangerous-outpost combat, deferred-work pacing, Jade/Xaero visual coexistence, two-player shared-state behavior and full candidate companion-stack fresh-world launch.

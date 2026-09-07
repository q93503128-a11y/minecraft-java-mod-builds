# Frontier Settlement

Minecraft Java 26.2 / NeoForge 26.2 cooperative survival settlement-growth mod.

## Current version: 0.1.0-alpha.121

Frontier Settlement owns the shared settlement, physical construction, residents, production, roads, outposts, logistics, defense infrastructure, bounded civil works and territory progression. Companion mods remain the preferred source of biome, dungeon, structure, combat, weapon and loot breadth.

This is a **broad playable alpha**, not original-v0.2 completion. Do not call it complete while `COMPLETION_GAP_AUDIT.md` still contains meaningful `부분/미구현` items, and do not treat automated server smoke as graphical-client play acceptance.

## Canonical direction

Read these before changing product direction:

1. `ORIGINAL_DESIGN_v0.2.md` — original product intent;
2. `CANONICAL_PLAN.md` — current implementation direction and authority rules;
3. `COMPLETION_GAP_AUDIT.md` — remaining original-scope gaps;
4. `UI_UX_REDESIGN_ALPHA117.md` — current UI hierarchy and quality-standard application.

Repository-wide implementation work should also follow the shared `docs/QUALITY_STANDARD.md`.

## Core loop

`survival -> settlement growth -> better exploration -> external exploration / conquest -> NPCs / resources / technology -> settlement growth`

Hard rules:

- one shared settlement per world/server;
- server-authoritative settlement and project state;
- real Minecraft ItemStacks are the resource authority;
- player-made buildings are not scanned into Frontier functional buildings;
- repetitive production, hauling and ordinary job assignment are automated;
- loaded areas visibly work and move;
- never force-load chunks merely to continue simulation;
- never silently destroy player containers, fluids, valuable blocks or unrelated builds;
- companion mods provide adventure/content breadth while Frontier remains settlement/progression glue;
- server/build automation is not a substitute for real client visual/play acceptance.

## Controls

Current default construction controls:

- `M` — settlement / infrastructure palette;
- `R` — rotate an ordinary building placement;
- `Enter` — confirm the current building / road / outpost / civil-work selection step;
- `Backspace` — reset the road start or civil-work first corner.

Civil work is entered from the current `M` infrastructure flow. The first selected corner's Y is the target grade plane; the opposite corner defines the bounded work area.

## Alpha.117 UI contract

The settlement command UI follows one information hierarchy:

`settlement tier -> population/resources -> next growth goal -> construction family -> building choice -> detail -> world placement`

Current UI requirements:

- settlement tier and six-stage progress are explicit at the top of the M screen;
- the server-authored next-growth goal is visible in the M screen rather than permanently occupying the HUD;
- construction uses the horizontal families `기반 / 생산 / 제작·서비스 / 방어 / 랜드마크 / 인프라`;
- each building exposes an explicit `건설 가능 / 자원 부족 / 잠김` state instead of relying on color alone;
- wide layouts use a choice list plus one detail pane, while narrow layouts may collapse detail rather than crush text;
- founding, guide, location, palette and HUD surfaces share `FrontierUiTheme` tokens instead of inventing local colors and spacing;
- the idle HUD stays compact and expands only for active project information;
- current Korean instructions must describe the real M/R/Enter/Backspace interaction model.

Actual graphical-client screenshots across practical GUI scales remain required before calling the visual redesign final.

## Alpha.117 runtime hardening

Alpha.117 also reduces repeated work without changing physical gameplay authority:

- shared-storage resource accounting classifies each physical inventory slot once per scan instead of making four independent wood/stone/metal/food passes;
- stationary building, road, outpost and civil-work previews revalidate once per second, while a target or rotation change still invalidates immediately and requests a fresh result;
- fishing shoreline evidence is reused within the same server tick only, so duplicate callers cannot rescan the same shoreline while world changes remain visible on the next tick;
- specialized-production and fishing outpost workers use cached physical entity UUID lookup on the ordinary hot path;
- full assignment-AABB scans remain as conservative 10-second maintenance for duplicate cleanup, old-save migration and missing-worker recovery;
- no cache becomes population, cargo, resource or spawn authority;
- blacksmith equipment repair is explicit player-directed interaction and consumes real settlement metal rather than silently repairing nearby equipment.

## Alpha.118 specialized outpost scan budget

Alpha.118 reduces repeated physical-world search cost without reducing authored work range or production throughput:

- lumber and quarry outposts cache the selected physical block coordinate while it remains a valid loaded target;
- ordinary 10-tick worker updates validate that one cached coordinate instead of rescanning the full work radius;
- a destroyed, replaced, protected or unloaded target invalidates immediately and may be reacquired on the next work update;
- a full search that finds no eligible resource backs off for 100 ticks (five seconds) rather than repeating every half-second;
- natural-leaf evidence for a cached lumber target is reconfirmed only when the 100-tick harvest cadence is actually due;
- quarry overburden may continue to clear layer-by-layer against the same cached stone face;
- tree radius 18, quarry radius 16, work periods, harvest batch limits, real ItemStack cargo, protection checks and no-force-load rules are unchanged.

## Alpha.119 city administration and production buffers

- A completed civic hall reduces ordinary civilian vacancy checks from 600 to 400 ticks (30s -> 20s).
- The civic hall contributes two additional bounded construction workers; the hard builder cap becomes 14.
- Production grade I uses one physical profession barrel, grade II may create a second, and grades III-IV may create a third vertical barrel.
- Extra barrels are created only in safe replaceable cells; existing containers, fluids and unrelated/player blocks are never overwritten.
- Producers fill real local profession barrels before using the existing central/shared overflow path.
- The M palette exposes both civic administration and current production-buffer capacity.

Central warehouses remain the long-duration storage backbone; extra profession barrels are bounded local buffers rather than a replacement warehouse network.

## Alpha.120 deterministic blacksmith reinforcement

- Sneak-right-clicking the completed settlement blacksmith anvil still repairs a damaged held item first.
- A fully repaired single equipment stack with a normal attack-damage or armor attribute can instead be reinforced deterministically.
- Reinforcement never rolls failure, destruction, downgrade or enchantment replacement.
- The stack stores only a bounded reinforcement level in custom data; NeoForge's item-attribute event adds the bonus without replacing the equipment's original attribute component.
- Attack-damage equipment gains +0.5 attack damage per reinforcement level; armor equipment gains +0.25 armor per level.
- Settlement-tier caps are +1 through Village, +2 at Frontier Town, +4 at Domain and +5 at Frontier Capital.
- Costs for +1/+2/+3/+4/+5 are 4/8/12/18/26 physical copper-or-iron items.
- Blacksmith repair and reinforcement accept only copper/raw copper/iron/raw iron; gold, diamonds and arbitrary high-value modded metals are never implicit blacksmith payment.
- A failed eligibility, tier-cap, storage-load or material check consumes nothing.

Existing external item components, enchantments, names and lore remain authoritative. Unsupported items fall through to ordinary anvil behavior.

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

Late-game civic hall, trade hall and citadel are progression landmarks rather than an excuse to multiply routine production-management families.

## Physical construction and logistics

Ordinary functional construction remains:

`preview -> terrain/overlap/cost validation -> physical grading -> material hauling -> phased construction -> completion`

The presentation and authority invariant is: **builders walk from actual settlement storage carrying real wood/stone stacks**.

Important current behavior:

- building validation uses the real rotated footprint rather than a hidden exterior veto ring;
- clearable grass, flowers and verified natural tree vegetation do not make an otherwise valid site fail merely because they occupy the footprint;
- medium terrain work remains bounded; water, protected blocks, containers and unsafe/deep terrain reject placement rather than being silently overwritten;
- construction offices and completed outposts expand a bounded builder workforce;
- building, road and outpost project lanes can run in parallel only within the centralized project-capacity/separation rules;
- Alpha.27 road logistics remains the single authority for long-distance outpost transport;
- transporters belong to a specific outpost and pause at unloaded route boundaries instead of teleporting or force-loading;
- profession worksite buffers and shared depots remain real physical containers;
- same-profession town workers use deterministic global minimum-total-distance workplace matching rather than UUID/list-index assignment.

## Production and ecology

- staffed farms actively tend crop growth and use bounded harvest batches;
- lumber workers harvest real natural trunks and attempt bounded physical replanting;
- quarry workers may open a shallow face through safe natural overburden rather than requiring pre-exposed stone;
- mines consume finite real ore blocks;
- specialized outposts remain loaded-only physical producers;
- deferred outpost state represents bounded work-time debt, never virtual resources or cargo.

## Settlement resources

Shared resource totals are derived from physical storage:

- wood;
- stone;
- metal value;
- food value.

An ItemStack may fund only one settlement resource category. Ambiguous multi-category companion/datapack tags fail closed, and expedition relics / recognized external weapons are not ordinary construction material.

## Validation

Authoritative Frontier CI runs, in order:

1. cumulative current-source audit;
2. canonical/current-doc audit;
3. Java 25 toolchain and committed-tree verification;
4. Java 25 clean build against Minecraft 26.2 / NeoForge 26.2.0.38-beta;
5. runtime JAR verification + SHA-256;
6. companion dedicated-server preflight;
7. fresh-world companion server creation/save/clean-shutdown smoke where applicable.

For Alpha.117 the source audit additionally locks:

- shared UI hierarchy/theme invariants;
- compact HUD / current M-key guidance;
- manual physical-metal blacksmith repair;
- single-pass shared-storage resource aggregation;
- one-second stationary placement preview throttling with immediate target-change invalidation;
- same-tick shoreline evidence reuse;
- specialized/fishing worker UUID hot paths and 10-second maintenance scans.

## Real-play acceptance still required

Automated validation does **not** prove the following:

- actual M-screen visual quality at common GUI scales and aspect ratios;
- Korean text clipping or awkward wrapping on small windows;
- long-session worker/workplace feel and pathing;
- construction placement feel on real uneven survival terrain;
- multi-project construction readability;
- farm/lumber/quarry production pacing over a long survival session;
- outpost worker recovery through practical chunk unload/reload;
- two-player shared-state behavior;
- companion-mod UI/key conflicts;
- long-play save/reload behavior.

Treat these as playtest evidence, not assumptions.


## Alpha.121 production state and citadel response

- Existing production AI publishes presentation-only runtime states; UI/Jade does not launch a second entity/resource scan.
- Loaded lumber/farm/quarry/mine buildings expose staffing, travel, depletion and storage-blocked states; unloaded sites report `청크 미로드`.
- A citadel raises the barracks patrol leash from 24 to 32 blocks and adds 12 blocks to loaded garrison threat detection.
- One threat lookup is shared by all three soldiers of a barracks per patrol pass instead of repeating the same monster AABB scan three times.
- Citadel-expanded patrols use the matching 32-block loaded-area evidence gate, preserving no-force-load behavior.
- Soldier capacity, real-weapon authority, recruitment payment and physical cargo rules are unchanged.

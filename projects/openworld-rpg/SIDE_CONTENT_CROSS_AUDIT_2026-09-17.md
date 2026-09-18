# Open-World RPG — Side / Contract Content Cross-Audit — 2026-09-17

> Scope: R01–R12 optional quests, contracts, hunts, optional traversal and non-main regional content.  
> Authority: `PROJECT.md`, `GAME_DESIGN.md`, each region's current `*_CONTENT_BIBLE.md`, and `R01_VERTICAL_SLICE.md`.  
> Result: **SIDE-CONTENT COVERAGE PRESENT ACROSS R01–R12 — DO NOT ADD QUESTS ONLY TO INCREASE COUNT**

This audit was triggered by a production question: whether the current game is effectively only a main-quest RPG or already contains meaningful work such as local requests/contracts outside the main story.

The answer from current canon is clear: the project already has a second layer of regional activity in every region. The remaining problem is implementation, spatial placement and presentation quality, not inventing more quest categories.

This file does not create new gameplay. It records the cross-region result so implementation does not accidentally collapse the game into the main route or, in the opposite direction, inflate every settlement into an MMO chore board.

---

# 1. Global side-content contract

The intended open-world structure is:

```text
main / regional story
+ a small foreground set of authored local requests
+ discovery-led hunts / optional bosses
+ dynamic world events
+ gathering / fishing / traversal / service activities that have real regional use
```

These layers are not interchangeable.

- **Main / regional story** carries the Anchor-network investigation and major world-state changes.
- **Regional side quests** tell local stories, reopen useful routes, rescue people or expose a region-specific problem without being required for main progression.
- **Contracts / requests** are shorter, concrete pieces of work tied to gathering, fishing, route safety, repair, survey or local services.
- **Hunts / optional bosses** are discovery-led combat goals and must not become mandatory story gates merely because they are expensive content.
- **Dynamic events** occur from world state rather than requiring every activity to begin from an NPC dialogue click.

The player should be able to ignore most optional content and still follow the main game, while a player who wanders should repeatedly find useful things to do that feed progression, world familiarity, collections, services or traversal.

---

# 2. Region-by-region evidence

| Region | Confirmed non-main authored content examples | Regional function |
|---|---|---|
| **R01 Alderford** | `Riverbank Remedies`, `Signs in the Meadow`, `Steel in the Grass`, repeatable `Roadside Trouble`, optional Regalhart hunt | gathering/healing, ecology/tracking, spontaneous road assistance, optional hunt discovery, early open-world choice |
| **R02 Rillcross** | `The Last Survey`, `River Ledger`, `Under the Eaves`, optional Grovebound Warden route/hunt | rescue without escort drag, fishing introduction, local supplies, forest disturbance side branch |
| **R03 Cairnwatch** | `The Long Way Holds`, `Silver Cut`, Whitecrest Griffin hunt | alternate conventional route, mining/tool payoff, optional apex hunt |
| **R04 Hearthspring** | `Shelter in the Storm`, `Dark Water, Bright Scale`, Ferox Iceworm hunt | authored whiteout navigation, cold-water fishing, optional field boss |
| **R05 Tanglewater** | `A Faster Trail`, `Rainleaf Stock`, `A Clear Crossing`, `River Table`, mature Earthloong hunt | mount unlock, alchemy/gathering, route safety, fishing, optional mature boss |
| **R06 Siltwake** | `Clean Water, Bitter Leaves`, `Clay for the Walkway`, `Reedwater Catch`, Hydra hunt | medicine/ecology, repair material, fishing, optional wetland boss |
| **R07 Amberwell** | `Salt and Shade`, `Armor on the Pass`, conditional `Oasis Catch`, Ferox Deathworm hunt | caravan supplies, route threat, ecology-dependent fishing, optional desert boss |
| **R08 Lumenroot** | `Colors That Live`, `The Quiet Shelf`, `A Spring Worth Keeping`, Titan Rabbit ritual hunt | cosmetic/gathering, archive exploration, magical ecology, deliberate optional summon |
| **R09 Stoneway** | `Parts by Measure`, `Herdstone Passage`, `The Third Signal`, optional major-hunt slot | logistics materials, non-kill road resolution, optional infrastructure completion, field-boss slot pending asset gate |
| **R10 Cinderhold** | `Blackglass Work`, `Safe Shift`, `Ashline Survey`, Basalt Wyvern hunt | high-tier gathering, hazard response, exploration/survey, optional aerial boss |
| **R11 Tidecross** | `Reef Ledger`, `Lights Out`, `Red Wake at Night` / Riptooth hunt, optional `The Breakwater Keep`, Laviathan / Deep-Dive Harness side progression | reef economy, beacon repair, hunt, optional sea-fort dungeon, traversal specialization |
| **R12 Riftwatch** | `Lost Signal`, `Glass Garden Survey`, `The Fourth Line`, optional Sky Drake unlock, Terradragon world boss | rescue/survey, anomaly ecology, optional evidence completion, late traversal, world-boss spectacle |

R01 explicitly distinguishes the main board entry from optional contracts at first arrival. Later regional bibles follow the same principle with fewer foreground optional tasks rather than filling the HUD with every available activity.

---

# 3. Why the current density is enough

The project should **not** respond to open-world scale by adding ten generic requests per settlement.

Current canon already avoids several low-quality MMO patterns:

- no repeated `kill 10 wolves` quota as the default contract grammar;
- no new regional token/currency merely to support one board;
- no rare-fish requirement for basic fishing introductions;
- no slow mandatory escort merely because an NPC must be rescued;
- no compulsory service-tour quest through every shop;
- no requirement to clear every optional hunt before the regional dungeon;
- no objective count inflation when the real content is a route, environmental trail, mechanism or world-state change.

Several region documents explicitly limit foreground optional contracts to roughly two relevant entries beside the active main objective. That rule should be preserved in implementation.

The target is **content density**, not quest-log density.

---

# 4. Implementation rules for side content

When gameplay source/data is eventually bootstrapped, optional content should preserve these properties:

1. **Server-owned completion and rewards.** Quest credit, consumed materials, Gold, mount registration and persistent world changes are authoritative server transactions.
2. **Data-driven definitions.** IDs, giver/trigger, prerequisites, objective stages, reward tables, repeatability, shared/personal state ownership and failure/rejoin behavior should live in reusable data/state structures rather than one-off hardcoded quest classes where practical.
3. **Spatial truth.** A contract should point at a real authored place/activity. Do not spawn arbitrary enemies beside the board because a quest needs a target.
4. **Useful aftermath.** Route, service, NPC, stock, codex, housing, traversal or world presentation should change when the authored contract says it changes.
5. **No marker dependency.** GPS-style exact pins should not replace readable trails, landmarks, sightings and authored search areas where the regional canon deliberately uses them.
6. **Optional means optional.** Side content may improve readiness, gear, travel, services or context, but it must not silently become a required grind wall for the next main region.
7. **Repeat only where repetition is actually fun.** Normal authored contracts are often once-per-player; repeat activity belongs mainly to world events, fishing/gathering loops, dungeons and eligible hunts under their existing global rules.

---

# 5. Playtest expansion rule

Do not pre-emptively add more contracts now.

Add new optional content later only if actual spatial/playtest evidence shows a concrete dead zone, for example:

```text
major travel corridor has 8–12 minutes with no meaningful decision or interaction
specific service/system is introduced but never used naturally
an otherwise strong POI has no reason to revisit or engage
one region's optional layer collapses into only combat while its identity depends on another system
co-op repeatedly leaves one player without meaningful contribution
```

If such a gap appears, fix the gap with the smallest regional activity that connects to an existing system. Do not create a new menu, currency or progression tree just to solve one empty stretch.

---

# 6. Current production handoff after this audit

This audit closes the question **“does the game have meaningful requests outside the main quest?”** at the design level.

It does **not** make the project source-ready.

Current earliest production blockers remain the authoritative `PROJECT.md` pre-code gates, especially:

1. exact external presentation binding/provenance;
2. actual Azari spatial closure;
3. exact model-dependent final encounter sheets;
4. final stale-document / hidden-choice cleanup.

For R01 specifically, Phase-B Pass 5 has already locked creator-controlled acquisition locators and narrowed remaining motion-source gaps, but the current execution environment still cannot retrieve the required binary ZIP bytes for project-local SHA-256, archive-member inspection, 3D review or Minecraft visual acceptance. That limitation must not be papered over with guessed hashes or placeholder art.

Therefore the next binary-capable intake session should continue the recorded R01 acquisition batch rather than restart broad research.

---

# 7. Verification state

```text
R01–R12 SIDE-CONTENT CANON REVIEWED: YES
MAIN-ONLY RPG RISK AT DESIGN LEVEL: NO
NEW SIDE-QUEST DESIGN ADDED BY THIS AUDIT: NO
CODE REVIEWED: N/A
TESTED: NO
BUILD VERIFIED: NO
JAR PRODUCED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

No build/CI is appropriate for this documentation-only cross-audit.

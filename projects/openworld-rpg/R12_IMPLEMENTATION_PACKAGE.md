# Open-World RPG — R12 Obsidian Rift Implementation Package

> Status: **DESIGN CANON — R12 world/story/traversal/service/reward/finale flow is implementation-ready; exact final-guardian model/anatomy and selected anomaly-creature bindings remain explicit external-asset gates**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Story spine: `WORLD_STORY_CANON.md`  
> Region graph: `REGIONS.md`  
> Combat: `COMBAT_BALANCE.md`, `STATUS_AND_R01_ENCOUNTERS.md`  
> Loot/equipment: `LOOT_ECONOMY.md`, `EQUIPMENT_BALANCE.md`  
> Mounts: `MOUNTS.md`  
> Field systems: `GATHERING_FISHING_CAMP_HOUSING.md`, `FISHING_COLLECTION_HOUSING_MARKET.md`  
> Quest/state: `QUEST_WORLD_STATE.md`  
> Quality contract: `DESIGN_COMPLETENESS_AUDIT.md`  
> External provenance: `EXTERNAL_SOURCES.md`  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins.

R12 is the highest suggested-entry open-world land region at launch, but it is **not a corridor called `the final biome`**.

Its play sentence is:

```text
see impossible obsidian silhouettes long before reaching them
→ enter a sparse region where ordinary geography still exists but selected places obey damaged Anchor logic
→ use landmarks, echo-signals and earlier regional knowledge to distinguish safe routes from unstable ones
→ establish contact with a small edge refuge instead of discovering another full city
→ explore large optional anomaly complexes and hunt rare endgame creatures
→ unlock the Sky Drake only after the player has first learned R12 at ground scale
→ challenge Terradragon as an optional altar-summoned world spectacle
→ descend through the Obsidian Cathedral / rift excavation into the Central Anchor complex
→ stop a cascading network failure through a final systemic guardian encounter
→ make the personal Restore / Release / Partition choice only after the immediate crisis is contained
→ continue into postgame with the world stabilized enough to explore rather than being rolled back or permanently locked
```

R12 should make the player feel:

> `The strange things I saw across the whole continent were parts of one system — but this place is where those relationships finally become physically visible.`

The region is allowed to be strange. It is not allowed to be visually incoherent or mechanically annoying merely because it is an anomaly zone.

---

# 1. Locked regional identity

Preserved from `REGIONS.md` and `WORLD_STORY_CANON.md`:

- terrain: western-central obsidian spikes / dark stone, overgrown caves, rift pockets and exposed ancient infrastructure;
- suggested entry Lv: **72**;
- the region is physically discoverable from multiple approaches and is not sealed behind the main story;
- ordinary R12 exploration, edge POIs and optional world content can be entered early by a reckless under-level player;
- the **deep Central Anchor finale**, not the whole region, requires the main investigation/evidence state;
- ecology/encounters are sparse rather than continuously hostile;
- fewer creatures have larger encounter footprints and stronger environmental context;
- no large ordinary city exists inside the anomaly;
- one edge refuge / research camp provides the practical service anchor;
- resource direction remains rift crystal / obsidian-glass-like material / late arcane mineral roles only after exact external visual intake;
- reward identity emphasizes build-defining relics, class-skill variants and unusual high-end mechanics rather than only the highest numbers;
- Terradragon remains a high-end world-boss candidate;
- the main finale uses a separate Anchor-system guardian/crisis-form role rather than forcing Terradragon into the story boss slot.

Production clarifications:

- R12 has **no global anomaly/sanity/corruption meter**;
- simply standing in R12 does not constantly drain HP/Mana/Stamina;
- no random teleport system repeatedly destroys player orientation;
- no blanket purple/red screen shader or permanent camera distortion;
- no generic `all previous enemies but stronger` population;
- no giant ordinary settlement is added just to match other region templates;
- no launch prestige level or R12-only currency is introduced;
- no Nether/End trip becomes mandatory merely because some donor creature originally belonged there.

---

# 2. External game-content precedents

These are structural references. No proprietary maps, scripts, assets or exact encounter content are copied.

## 2.1 Control — grounded paranormal space and meaningful optional exploration

Useful precedent:

- the Oldest House remains built from understandable architecture/material/function even while its spatial rules become supernatural;
- distinct sectors have recognizable purposes rather than every room being generic anomaly scenery;
- optional exploration carries meaningful secrets/lore/content instead of existing only to pad completion percentage;
- visual weirdness has stronger impact because normal, grounded spaces remain present as contrast;
- environmental reaction/destruction is constrained by visual and performance requirements rather than being unlimited spectacle.

Project adoption:

- the Edge Refuge, old excavation works, archive/maintenance spaces and Central Anchor infrastructure remain materially understandable;
- anomalies appear at specific seams/nodes/complexes instead of covering every block with effects;
- each major R12 POI has a former practical function the player can infer;
- important optional content gives relics, techniques, world understanding, shortcuts, bosses or collection value — not repeated generic chests;
- anomaly VFX are budgeted so combat telegraphs remain readable.

Explicitly not adopted:

- constant screen-space distortion;
- a fully shifting labyrinth where the route becomes unknowable;
- modern-government aesthetic or Control-specific fiction.

References:

- `https://blog.playstation.com/2019/08/08/everything-we-learned-playing-the-first-2-hours-of-control/`
- `https://www.gdcvault.com/play/1030643/Destructible-Environments-in-Control-Lessons`

## 2.2 The Legend of Zelda: Tears of the Kingdom — vertical layers and valuable clues

Useful precedent from Nintendo developer interviews:

- adding caves/vertical layers changes how players read even familiar terrain when one discovery suggests other discoveries;
- surface, caves and higher spaces are strongest when connected rather than isolated map modes;
- the development team explicitly found that adding too many sky islands made the space cluttered and reduced them;
- a new layer is valuable when it changes how the player looks at the whole world, not simply because it adds more square meters.

Project adoption:

- R12 uses vertical sightlines, caverns and elevated obsidian shelves that remain spatially related to surface landmarks;
- one anomaly clue should make the player re-interpret similar structures elsewhere in the region;
- fewer substantial anomaly sites are favored over dozens of micro-islands/mini-rifts;
- entrances to deeper complexes are visually associated with surface structures so the player can build a mental map.

Not adopted:

- a universal construction/physics sandbox;
- sky-island quantity as content;
- new traversal powers that invalidate the project's existing mount/class systems.

Reference:

- `https://www.nintendo.com/us/whatsnew/ask-the-developer-vol-9-the-legend-of-zelda-tears-of-the-kingdom-part-3/`

## 2.3 Environmental-storytelling production lessons

Useful precedent:

- strong environmental storytelling lets the player infer what happened from layout, props, lighting, damage and system behavior rather than receiving every fact through exposition;
- player/system reactions can communicate history and consequence as strongly as text.

Project adoption:

- old maintenance routes, severed network trunks, emergency partitions and failed relay chambers physically demonstrate how the Anchor network became over-coupled;
- the player can understand `this system was once useful` and `this coupling became dangerous` from the place before the finale dialogue states it;
- readable environmental evidence is always paired with concise journal/dialogue support for players who do not inspect every prop.

Reference:

- `https://www.gdcvault.com/play/1012696/What-Happened-Here-Environmental`

---

# 3. External-first source stack

## 3.1 Threateningly Mobs Continued — dependency candidates

Current public project metadata exposes a Fabric 26.2 release and currently lists the project under MIT.

Strong R12 candidate:

### Terradragon

Current/original source lineage establishes that:

- Terradragon is an ultra-tier / very heavy creature;
- its modern encounter identity uses an **altar summon** rather than ordinary world spawn;
- older public descriptions present it as a moving active-volcano-like dragon;
- later donor combat work moved Terradragon toward authored procedure-controlled actions rather than ordinary basic attacks;
- some donor versions allow Terradragon skills to break blocks.

Project adoption:

- preserve the altar-summoned spectacle identity;
- project data owns Lv, HP, damage, rewards, summon eligibility, reset and multiplayer scaling;
- arbitrary terrain destruction is forbidden; only tagged authored breakable arena props may break;
- Terradragon is **optional R12 world-boss content**, not the main story's Central Anchor guardian;
- donor Ultra Summon Stone grind is not inherited.

Current references:

- `https://modrinth.com/mod/threateningly-mobs-continued`
- `https://modrinth.com/mod/threateningly-mobs/version/1.0.9.2`
- `https://www.curseforge.com/minecraft/mc-mods/threateningly-mobs`

Other candidate names present in existing project direction such as `Farseer`, `Murmur` and `Reaper` are **not production-locked by this file** unless exact current 26.2 identity/model/behavior is inspected. R12 does not need to fill every low-density encounter slot before source verification.

## 3.2 Final Central Guardian external gate

The final main-story combat role requires a visual identity that can express a constructed/systemic Anchor guardian or crisis form.

Primary visual-comparison pool:

- Quaternius `Ultimate Monsters` — 50 fully animated monster models, older source page explicitly published under CC0;
- other package-specific redistributable Quaternius/KayKit/independent animated guardian candidates after exact license evidence is recorded;
- a current mod dependency creature only if its model/animation language fits the system role without forcing unrelated donor lore.

Reference:

- `https://quaternius.com/packs/ultimatemonsters.html`

Status:

```text
FINAL GUARDIAN ROLE / ENCOUNTER STRUCTURE: LOCKED
FINAL PLAYER-FACING MODEL / NAME / ANATOMY-SPECIFIC ATTACKS: OPEN EXTERNAL-ASSET GATE
```

The implementation may not replace a failed asset search with a scaled vanilla mob, armor stand stack or generic particle cube.

## 3.3 Anchor machinery / structures

R12 requires stronger bespoke-looking ancient infrastructure than earlier incidental ruins.

External-first direction:

- large terrain silhouettes remain authored Minecraft terrain/blocks for scale/performance;
- accepted modular ruins/fantasy architecture can supply bridges, masonry, excavation and cathedral shell language;
- machinery, lenses, rings, conduits, control surfaces and central devices require coherent external model/reference direction;
- exact Anchor machinery cannot default to `glowing cube + rotating particles`;
- old maintenance/excavation props must look physically usable even when the system they service is magical.

Source-specific Quaternius license handling remains mandatory: do not assume all newer Quaternius packages are CC0 merely because older packs are.

## 3.4 Sky Drake

`MOUNTS.md` already locks the late permanent flying mount direction:

- primary visual source: Quaternius `Animated Monster Pack` Dragon;
- project-owned persistent mount behavior;
- cruise / climb / dive tuning remains exactly in `MOUNTS.md`;
- R12 owns the authored unlock quest and environment context.

R12 does not redesign its speed or stamina rules.

---

# 4. Story role — where the competing truths become one physical system

R12 is the Act-IV region from `WORLD_STORY_CANON.md`.

Regional truth:

- older facilities visible across R01–R11 were not independent magical shrines;
- the Central Anchor coordinated measurement, routing, containment and selected environmental infrastructure across large regional clusters;
- later administrators increased coupling between previously more independent branches to improve efficiency and control;
- this created cascade paths where one failing subsystem could stress distant branches;
- modern instability is dangerous precisely because the network still partially works;
- full restoration, full controlled release and regional partition each solve different real problems seen during earlier regional play.

R12 must not reveal that every prior region was secretly artificial.

The player should instead discover:

```text
natural regions + cultures existed on their own
→ Anchors intervened in selected large-scale processes
→ societies adapted differently as that infrastructure weakened
→ later centralization made the intervention network itself fragile
→ the launch finale is about future stewardship, not restoring a lost golden age by default
```

---

# 5. Player access / main-story boundary

R12 obeys the master open-world rule: **recommended Lv is guidance, not an invisible wall**.

## 5.1 Physically open content

A player who reaches R12 early can:

- enter the region;
- discover the Edge Refuge if they survive;
- gather accessible approved resources;
- encounter normal/elite R12 threats;
- find several POIs;
- discover the Terradragon altar/hunt chain;
- discover the sealed Central Anchor exterior;
- retreat.

They are not teleported away for lacking a quest flag.

## 5.2 Main-finale gate

The deepest Central Anchor interface requires the main investigation's accumulated evidence / authorization state.

This is allowed because it gates one **specific authored interior/finale**, not the region.

Presentation rules:

- the physical facility exists and can be found before the story is ready;
- the locked deep interface has an understandable in-world reason: multiple regional authentication/coordination records are required to safely enter/operate it;
- no arbitrary `Come back at Lv72` door;
- a player who finds it early receives a concise journal lead, not a fake invisible barrier.

---

# 6. Edge Refuge / people

R12 has no normal major city.

Primary hub: **Edge Refuge / research-and-rescue camp** outside the strongest anomaly band.

Target daytime functional population:

```text
6–10 researchers / engineers / scouts / guards / recovery personnel
2–4 travelers / recovered expedition members / merchants
```

No vanilla villagers.

Normal visible work:

- surveying rift boundaries;
- maintaining route beacons and rope/bridge access;
- analyzing recovered physical records;
- treating injured scouts;
- repairing gear and instruments;
- recording changes in obsidian growth / network signals;
- cooking / sleeping / ordinary camp routines so the refuge feels inhabited rather than a quest kiosk.

Services:

- shrine / fast travel;
- inn/rest equivalent;
- bank/Material Vault access or secure supply handoff;
- high-tier smith/repair/forge service where justified;
- merchant with fixed recovery essentials + high-tier rotating stock;
- research/journal interaction for main evidence;
- contract board limited to meaningful hunts/recovery/investigation work;
- Sky Drake handler/research lead after its quest becomes available.

R12 does **not** baseline include:

- purchasable housing inside the anomaly;
- a giant market district;
- every class trainer;
- a separate anomaly currency vendor;
- dozens of NPCs standing idle.

Housing remains available in safer existing settlements; the player does not need a mansion in every zone.

---

# 7. Spatial structure / pressure

Suggested entry Lv remains **72**.

| Sub-area | Local pressure | Role |
|---|---:|---|
| approach / fractured border | Lv 69–72 | transition / first anomaly language |
| Edge Refuge | Lv 72 | services / orientation |
| obsidian shelves / glass garden | Lv 72–74 | exploration / gathering / first anomaly encounters |
| split spires / echo quarry | Lv 73–76 | high-end POIs / elites |
| overgrown fault caves | Lv 74–77 | vertical/deep exploration |
| Terradragon altar basin | **Lv 78** | optional world boss |
| Obsidian Cathedral / rift excavation | Lv 76–79 | major dungeon/finale approach |
| Central Anchor deep complex | **Lv 79–80** | Act-IV finale |
| final systemic guardian role | **Lv 80** | launch main-story combat climax |

R12 should not fill every route with Lv72 trash mobs.

Between major sites, the player gets real breathing room, distant sound/visual cues and occasional low-density threats.

---

# 8. Anomaly language — strange but readable

R12 uses a small number of repeatable anomaly rules. Each has consistent visual/audio language.

## 8.1 Phase Seam

Role:

- local route-space instability;
- two authored geometry states or one temporarily stabilized connection;
- never a fully random teleport maze.

Rules:

- seam boundaries are visible through external VFX/geometry language before interaction;
- the player can understand which path is currently stable;
- route state changes only from explicit local triggers / authored event state;
- no change traps a player permanently in unloaded geometry;
- server owns state.

Use for:

- opening a bridge;
- exposing a side chamber;
- changing one encounter angle;
- revealing environmental evidence.

Do not use every 30 seconds.

## 8.2 Resonance Pulse

Role:

- short telegraphed network discharge affecting a bounded area.

Possible gameplay:

- temporarily energizes/de-energizes a floor lane;
- moves a visible environmental barrier;
- activates an old mechanism;
- changes one enemy behavior/event condition.

Rules:

- obvious pre-pulse tell;
- no unavoidable damage for simply standing in the region;
- combat use must show the exact danger zone;
- pulses do not scramble the player's controls or camera.

## 8.3 Overgrowth Fault

Role:

- natural/magical life reclaiming damaged infrastructure.

Use:

- roots/vines/crystal growth showing long-term system failure;
- alternate route around machinery;
- rare gathering/collection pocket;
- visual contrast against black glass/obsidian.

It is not generic `corruption` and is not automatically hostile.

## 8.4 Echo Signal

Role:

- navigation/story clue connecting one distant structure to another.

Presentation can combine:

- visible alignment/light pulse;
- directional sound;
- instrument/marker response;
- physical architecture pointing toward another node.

One discovered echo should teach the player how to notice the next without filling the map with GPS markers.

---

# 9. Navigation / density

R12 uses the normal `WORLD_STORY_CANON.md` POI principles but deliberately sits at the sparse end of the density range.

Target:

```text
orientation / major silhouette: almost continuously available in exposed areas
meaningful route decision / anomaly / encounter / resource pocket: roughly every 2–5 active minutes on content-bearing routes
substantial authored POI: normally within ~5–8 minutes of active exploration on intended routes
```

These are playtest targets, not placement quotas.

Primary macro landmarks:

1. the **Central Split Spire / Anchor silhouette** visible from multiple approaches;
2. the **Obsidian Cathedral / excavation scar**;
3. the **Terradragon altar/caldera-like basin**;
4. Edge Refuge beacon / warm-light cluster;
5. one major overgrown glass/crystal valley contrasting the dark terrain.

Rules:

- important entrances remain associated with surface landmarks;
- vertical layers reconnect rather than becoming stacked icon soup;
- Sky Drake flight later adds alternative sightlines but should not be required for first understanding of the region;
- no minimap display of every anomaly or hidden relic.

---

# 10. POI package

## Major POI A — Edge Refuge

Functions:

- safe social/service anchor;
- readable contrast against unstable territory;
- recurring-character convergence point;
- first explanation of how expeditions currently survive R12.

## Major POI B — Split Spire

Functions:

- primary region landmark;
- demonstrates multiple old network trunks physically converging;
- panorama / orientation point;
- contains a vertical optional exploration route rather than only a chest.

## Major POI C — Glass Garden

Functions:

- dark obsidian + living overgrowth contrast;
- rare material / collection / optional fishing-pool possibility only if exact accepted assets support it;
- shows that anomaly exposure can produce stable new ecology rather than only destruction;
- one optional Hidden Technique / relic clue candidate.

## Major POI D — Echo Quarry

Functions:

- former extraction/maintenance site;
- repeatable resonance route mechanic;
- physically demonstrates that old infrastructure moved more than magical energy;
- high-tier gathering without strip-mining.

## Major POI E — Terradragon Altar Basin

Functions:

- optional ultra-tier world-boss arena;
- clear long-range spectacle landmark;
- preserves donor altar-summon identity;
- no main-story requirement.

## Major complex F — Obsidian Cathedral / Rift Excavation

Functions:

- major exploration/dungeon complex;
- current-era excavation transitions into ancient maintenance strata;
- contains the route toward the Central Anchor interface;
- enough optional branches to reward exploration without becoming a 90-minute maze.

## Deep complex G — Central Anchor

Functions:

- Act-IV finale;
- environmental explanation of network coupling;
- final systemic guardian encounter;
- personal ending decision after immediate crisis containment.

Smaller discoveries may include:

- abandoned survey shelters;
- emergency partition gates;
- severed conduit trenches;
- fossilized/overgrown old service tunnels;
- failed relay lenses;
- lost expedition signals;
- one-off anomaly creature nests;
- vantage points that align distant regions/landmarks.

Do not convert these into identical `rift cache` markers.

---

# 11. Ecology / encounter roster philosophy

R12 has the lowest ordinary-creature density of the twelve major land regions.

Reason:

- high-level danger comes from encounter quality and space control, not aggro volume;
- sparse life makes each strange creature more memorable;
- performance budget is preserved for large actors/VFX/landmarks;
- the anomaly should not feel like a theme park queue of monsters.

## Candidate roles

### Low-density anomaly creature

Exact species/model remains external-gated.

Target:

- 1–2 visually distinct common/sturdy roles maximum;
- movement or perception mechanic tied to a specific anomaly space;
- 3–6 s ordinary TTK, not miniature boss health bars.

### Farseer / Murmur candidate names

These remain **candidate source identities only** from older project research until exact current 26.2 models/behavior are verified.

Do not write finished lore or attacks around unverified anatomy.

### Reaper-tier elite

A Reaper-like high-tier role is acceptable if the current dependency model/animation passes intake.

Target:

- rare authored elite, not natural spam;
- short dangerous duel / pursuit inside a specific complex;
- visible attack commitments;
- reward comparable to other late elites, not a separate currency.

### Earlier enemy reuse

Earlier Lich/Moonpriest/Executioner models may appear **only** where a specific authored expedition/ruin context justifies an individual.

Do not populate R12 with a greatest-hits roster simply because the assets already exist.

---

# 12. Terradragon optional world boss

Production role:

```text
encounter Lv: 78
role: optional ultra-tier world boss
active solo TTK target: ~250–290 s
HP authoring target: ~48,000–58,000 before final model/movement adjustment
Defense: high but not blanket physical immunity
MR: high-moderate
PoiseMax target: ~270–290
```

Final HP should be recalculated from `COMBAT_BALANCE.md` BenchmarkDPS after the accepted current entity movement/airtime is profiled.

## 12.1 Summon / repeat structure

First encounter:

- discover altar basin through exploration / local clues;
- complete one authored altar-access discovery sequence;
- no stack of random `Ultra Summon Stones` is required;
- once unlocked, a deliberate altar interaction begins the encounter.

Repeat:

- personal first-clear reward never repeats;
- world encounter uses a substantial active-world cooldown / reset condition, target **~45 active minutes** before re-summon eligibility;
- relog/restart does not reset cooldown;
- no rapid altar farm loop.

## 12.2 Arena

- very broad fractured basin / caldera-like shelf;
- multiple safe approach angles;
- large actor camera readability tested at Minecraft scale;
- no lethal-cliff cheese around the entire perimeter;
- selected tagged breakable props may sell impact;
- arbitrary terrain/player structures cannot be destroyed.

## 12.3 Combat identity

Preserve the moving-volcano / ultra-dragon impression, not donor progression rules.

Final attack list is gated by current model/animation inspection, but the encounter must support these **functional** roles:

1. close committed body/limb pressure that gives melee players real punish windows;
2. one clearly telegraphed forward/line eruption or breath-like attack;
3. one large ground/terrain-control attack whose visible area equals server hit area;
4. one reposition / short airborne or elevated sequence if the model supports it;
5. one ultra-tier signature at low HP that changes space/pattern rather than only adding damage.

Constraints:

- routine unreachable airtime <= ~6 s before a melee punishable window;
- no repeated 20 s circles;
- no permanent lava floor covering the arena;
- no ordinary attack >65% benchmark HP;
- catastrophic attack must use the exceptional telegraph rules from `COMBAT_BALANCE.md`;
- phase transition has no long invulnerability cutscene.

## 12.4 Rewards

First eligible defeat:

- guaranteed high-grade / endgame-appropriate personal gear roll;
- model-linked signature material after exact asset intake;
- high Mythic/signature chance within existing loot canon;
- EXP around the normal first world-boss percentage for the receiving Lv;
- Class XP around existing boss contribution rules;
- one housing trophy / visual collection unlock based on the accepted model.

Repeat:

- normal personal boss material/gear rolls;
- no unique currency;
- no mandatory Terradragon farm for the main ending.

---

# 13. Sky Drake unlock in R12

The Sky Drake is a major traversal reward and must not be handed out on region entry.

## Unlock timing

Target:

- available after the player has learned at least the Edge Refuge + one major R12 ground route;
- approximately Lv72+ progression context as already canonical;
- does not require Central Anchor final-story completion;
- can reasonably be unlocked before the finale and then used for optional R12/high-region exploration.

Quest shape:

```text
notice high-altitude drake signs / inaccessible perches
→ learn a safe approach through one ground-route / rift-shelter sequence
→ complete an authored registration/bonding/rescue interaction
→ return to the R12 handler/research lead
→ pay canonical 6,000 Gold harness/registration fee
→ unlock persistent Sky Drake summon
```

The actual creature interaction waits for final external model/animation intake.

Rules:

- no random taming percentage spam;
- no breeding/stat genetics;
- no mount XP grind;
- no `feed 50 rare crystals` gate;
- no invisible anti-flight wall across the open region after unlock;
- authored dungeon/interior/no-fly volumes remain legitimate.

Post-unlock optional content can use high spires and long aerial lines, but the core first R12 route remains understandable on foot/ground mount.

---

# 14. Gathering / collection / fishing

R12 introduces few new ordinary materials.

Principle:

- late-game regions should deepen earlier crafting relationships rather than making every old material obsolete;
- R12 adds only materials with strong visual/source and recipe roles;
- no `R12 Token` or `Anomaly Dust` filler currency.

Candidate categories pending exact asset intake:

- obsidian-glass-like high-temperature / arcane material;
- rift crystal with a distinct accepted model;
- late arcane ore / conductor material if it has non-overlapping use;
- rare overgrowth component from the Glass Garden.

Uses:

- Mythic/high-tier forge choices;
- class-skill variant / Hidden Technique crafting where already supported;
- late accessory/relic recipes;
- selected housing trophy/decor;
- endgame consumable recipes only where they do not create mandatory farming.

Earlier Iron/Silver/Hardwood/herbs/etc. remain part of recipes where logical.

## Fishing

R12 is not a fishing-first region.

One or two anomaly-influenced catches may exist only if strong external fish models support them.

Potential loop:

- stable Glass Garden pool / sheltered rift spring;
- rare Fish Codex discovery;
- sale/cooking/trophy use;
- not required for progression or final story.

Do not invent a transparent glowing fish just because a collection slot is empty.

---

# 15. Obsidian Cathedral / Rift Excavation dungeon

Target first-clear wall-clock:

```text
~30–45 minutes including exploration and final approach
```

This is longer than early dungeons but must not become an hour-plus corridor.

## Stage 1 — Current expedition layer

- readable scaffolds, survey stations and failed entry attempts;
- establishes what modern people currently understand;
- one optional rescue/recovery branch;
- no combat spam before the ancient layer.

## Stage 2 — Partition Galleries

- old physical bulkheads / route-separation architecture;
- one or two Phase Seam route changes;
- environment shows that parts of the network were once deliberately isolated;
- first evidence that later administrators bypassed/linked some safeguards.

## Stage 3 — Conduit Descent

- strong vertical sightline into deeper Anchor machinery;
- high-end elite encounter / optional material branch;
- shortcut elevator/route unlocks after reaching the lower level.

## Stage 4 — Network Archive

- visual maps / models / records show regional clusters and coupling history;
- main truth is understandable without reading pages of text;
- earlier R04/R05/R06/R07/R10 evidence gains context rather than being invalidated.

## Stage 5 — Central Anchor approach

- controlled set-piece showing the current cascade beginning/accelerating;
- player uses previously understood interaction language, not a brand-new minigame at the finale;
- recurring NPCs may appear physically or through safe communication depending on implementation/performance.

## Repeat route

After first clear:

- story-only archive gates remain resolved;
- central shortcut reduces repeated traversal;
- replay content focuses on combat/rare branches/guardian variant where appropriate;
- ending choice is never repeatedly re-earned from dungeon farming.

---

# 16. Final systemic guardian encounter

Internal production role: **Central Guardian / Cascade Guardian**.

This is not a final player-facing name.

Working target:

```text
encounter Lv: 80
role: launch main-story major/endgame boss
active solo TTK target: ~285–320 s
HP authoring target: ~58,000–66,000 before final model/phase adjustment
PoiseMax target: ~285–300
```

The encounter expresses Anchor-network logic through space and timing.

## 16.1 Arena

- central machine chamber / open vertical core rather than a generic flat circle;
- three visible network-sector interfaces around the arena;
- safe readable floor remains available at all times outside explicitly telegraphed mechanics;
- background machinery shows distant network connections without spawning hundreds of live entities;
- camera remains viable for both melee and ranged builds.

## 16.2 Functional mechanics locked before model intake

The exact animation/anatomy binding waits for the selected external guardian.

### Network Link

One visible sector interface links to the guardian / arena system.

Gameplay role:

- modifies one attack family / arena lane;
- link is visible before the effect;
- player can move/position around it;
- not a hidden stat buff.

### Cascade Line

- strongly telegraphed line/lane propagation through the arena;
- one wave cannot multi-hit a player through overlapping server hitboxes;
- encourages leaving the path, not iframe-tanking a persistent lane.

### Partition Window

At selected HP/state thresholds:

- one or more interfaces can be temporarily isolated through a concise world interaction after the player earns a combat opening;
- successful partition changes the next pattern / prevents one cascade branch;
- interaction is not a long lever animation while enemies freely hit the player;
- multiplayer requires only one authoritative successful interaction, while all eligible players keep personal quest/reward credit.

### Reconfiguration

Phase change around the middle of the fight:

- changes which network paths are active;
- visually reconfigures arena machinery / VFX;
- no >2.5 s repeated invulnerability sequence;
- learned tells persist in transformed form rather than becoming unrelated attacks.

### Catastrophic Cascade

Late signature sequence:

- long unmistakable telegraph;
- uses previously taught network lanes/interfaces;
- failure can deal exceptional 55–65% benchmark HP but is not a routine one-shot;
- correct response is readable from mechanics learned earlier in the encounter/dungeon;
- no quick-time event.

## 16.3 Class fairness

- every major cycle includes melee access / punish time;
- ranged builds cannot ignore all arena mechanics from a safe platform;
- support/healing/guard contributions count normally for multiplayer eligibility;
- no required elemental damage type;
- no hard requirement for one class/weapon family;
- poise play provides meaningful openings but cannot permanently stun-lock the final boss.

## 16.4 Restoration Director role

The Restoration Director may push for immediate reactivation during the crisis and can be a source of conflict, but the finale does **not** require converting them into a generic humanoid HP sponge.

Preferred structure:

```text
Director initiates / refuses to halt a risky system action
→ physical network crisis escalates
→ player stops the immediate systemic failure / guardian
→ surviving people confront the consequences and available operating models
→ player makes the personal ending choice
```

If a direct confrontation occurs, it is dialogue/interaction/set-piece context unless later external-first humanoid combat work proves a genuinely strong reason for a duel.

---

# 17. Final choice / ending execution

The three ending philosophies remain exactly `WORLD_STORY_CANON.md`:

- **Restore** — rebuild coordinated continental operation under modern stewardship;
- **Release** — controlled severance/decommissioning of the old network;
- **Partition** — split it into independently governed regional clusters.

Rules:

- the immediate catastrophic cascade is stopped **before** the choice;
- the choice is deliberate, explained and not timed;
- no hidden `correct` option;
- each ending acknowledges both benefits and unresolved costs established by actual regional content;
- prior optional regions/NPCs/evidence can change epilogue detail, not invalidate the player's selected philosophy;
- one player cannot select another player's ending in multiplayer.

## Shared-world multiplayer consequence

Because ending choice is personal, the ordinary shared server world cannot permanently become three mutually exclusive physical maps.

Launch rule:

- shared physical R12 remains in the **post-crisis stabilized interim state** after eligible players clear the finale;
- personal dialogue, journal, epilogue, faction responses and selected postgame event/quest weights reflect each player's ending;
- any ending-specific major geometry that would conflict between players must use a deliberate instance/personal presentation rather than whole-world per-player phasing;
- players who have not completed the finale can still progress normally in the same shared world.

This follows `QUEST_WORLD_STATE.md` personal/shared-state ownership.

---

# 18. Postgame / endgame role

R12 completion does not end the world or reset the save.

Postgame is the existing game at its most connected, not a new prestige treadmill.

Useful postgame loops:

- Terradragon and other world-boss hunting;
- R11 abyssal content;
- R12 anomaly events / optional complexes;
- Mythic / named equipment target farming;
- Hidden Techniques / class-skill variants;
- difficult dungeon repeats;
- Fish Codex / rare collection completion;
- housing upgrades / trophies / furnishing;
- mount exploration with Sky Drake;
- optional regional quests/POIs not completed during the main route;
- ending-specific personal epilogue content where authored.

No launch baseline:

- infinite paragon levels;
- weekly reset chores;
- daily login currency;
- mandatory seasonal gear treadmill;
- endless escalating world tier that invalidates the authored region map.

---

# 19. Rewards / endgame item identity

R12 rewards should create new decisions, not only larger stat numbers.

Regional emphasis:

- Relics/Charms with build-changing conditional effects;
- class-skill variants / Hidden Technique unlocks;
- arcane/status conversion;
- risk/reward effects tied to guard, poise, resource state or positioning;
- high-end mixed-stat accessories;
- selected Mythic weapon/armor identities.

Rules:

- Item Lv still matters; R12 does not bypass the equipment system;
- a good R12 effect may compete with an earlier signature item instead of always replacing it;
- no `Anomaly Power` secondary item-level system;
- no separate R12 enhancement currency;
- deterministic main-story reward choice prevents finale RNG from being the only meaningful reward;
- optional world-boss loot remains farmable through existing signature/bad-luck rules.

Final exact item names/models/effects require external visual intake and compatibility with `LOOT_ECONOMY.md` / `EQUIPMENT_BALANCE.md`.

---

# 20. Dynamic events

R12 uses fewer but larger dynamic events.

Candidate authored event roles:

## Rift Survey Failure

- expedition signal appears / route becomes dangerous;
- rescue / stabilize / elite encounter;
- changes one local event state, not the whole region.

## Echo Convergence

- two or three nearby network signals align;
- opens a temporary optional route / rare encounter / resource pocket;
- predictable local warning;
- no random player teleport.

## Overgrowth Surge

- natural growth temporarily exposes/blocks a route and creates a gathering/creature event;
- demonstrates R12 is not only dead obsidian.

## High-tier roaming encounter

- rare elite/large creature crosses a known route;
- event has clear audio/visual warning and bounded persistence;
- does not spawn directly on top of a fast-travel arrival.

Participation/reward rules remain `QUEST_WORLD_STATE.md`.

---

# 21. Audio / presentation

R12 needs strong restraint.

Outside combat:

- broad wind / distant stone/metal resonance;
- sparse wildlife rather than continuous monster noises;
- Edge Refuge has warm human/work sounds;
- deep facilities use low mechanical/rhythmic cues that help orientation;
- anomaly sites can add directionally useful tones/pulses.

Music:

- do not run constant `final boss area` orchestration during ordinary exploration;
- use sparse regional bed + stronger layers at major anomaly/complex thresholds;
- final dungeon gradually introduces network motifs heard subtly in earlier region/Anchor content where possible;
- finale receives its own high-impact track family.

VFX:

- bounded and shape-readable;
- visible danger = server danger;
- no particle fog hiding weak points/telegraphs;
- no full-screen chromatic distortion as baseline region ambience;
- accessibility option should reduce camera/screen-distortion intensity if any such effects survive final presentation.

---

# 22. Multiplayer / authority

Server owns:

- R12 anomaly state;
- Phase Seam state;
- route/shortcut state;
- Terradragon summon/cooldown/encounter state;
- Sky Drake unlock/Resolve/summon validity;
- Central Anchor encounter state;
- objective/evidence state;
- ending eligibility and reward transactions;
- personal ending choice;
- personal loot;
- shared safe world-state changes.

Required later multiplayer tests:

- early-arriving under-level player can enter/retreat without corrupting main state;
- one player's finale evidence does not unlock another player's personal finale eligibility;
- players on different story steps can share ordinary R12 encounters safely;
- Terradragon participation uses support/damage/revive/objective eligibility without last-hit ownership;
- Terradragon cooldown cannot be reset through relog/chunk unload;
- Sky Drake mount position/flight/seat state remains server-correct;
- one player's Restore/Release/Partition selection never overwrites another's;
- a late-join player after shared post-crisis stabilization can still complete the full personal finale;
- final reward transaction cannot duplicate on disconnect/reconnect;
- dungeon shortcut state persists independently from personal ending state.

`MULTIPLAYER TESTED` remains NO until actual clients verify it.

---

# 23. Performance constraints

R12 is visually ambitious but does not receive permission to ignore the project performance rules.

- no global anomaly scan every tick;
- anomaly controllers exist only for loaded/local authored zones;
- Phase Seam state uses explicit saved/event state rather than searching geometry continuously;
- Terradragon AI/path/effects are bounded to the active arena/encounter;
- large effects use a small number of coherent VFX objects/decals/sounds rather than thousands of particles;
- background Central Anchor machinery should rely on static terrain/models plus bounded animated components;
- large Display Entity / BDEngine use must be profiled under the actual expected view distance;
- low-density ecology reduces persistent AI/pathfinding load;
- Sky Drake flight does not scan the whole world for POIs each tick;
- dungeon/anomaly logic sleeps when its area is unloaded.

Profiler validation is required before choosing final budgets.

---

# 24. Data contract

R12 should be content/data-driven, not a pile of boss-specific constants in event handlers.

Suggested data groups:

```text
regions/r12.json
r12/poi/*.json
r12/anomalies/*.json
r12/events/*.json
r12/encounters/*.json
r12/terradragon.json
r12/central_guardian.json
r12/finale_state.json
r12/ending_presentation/*.json
```

Minimum anomaly definition:

```text
id
zone_bounds_or_locator
kind
visual_binding
sound_binding
server_state_key
activation_condition
transition_timing
hazard_profile
route_effect
reset_rule
multiplayer_visibility_rule
```

Minimum finale state:

```text
player_evidence_requirements
world_shared_crisis_state
player_finale_eligibility
player_pending_reward
player_ending_choice
player_epilogue_flags
shared_post_crisis_state
```

Do not store `ending = host_choice` as a shared global shortcut.

---

# 25. R12 first-play acceptance targets

R12 is not accepted because the map looks dramatic in a screenshot.

Verify later:

1. approaching R12 exposes at least one unmistakable landmark before the player enters the highest danger band;
2. a player can navigate Edge Refuge → first major POI → return without constant map checking;
3. anomaly rules are learned through repeated visual language rather than text popups;
4. no baseline anomaly/cold/heat/sanity maintenance meter appears;
5. ordinary encounter density leaves meaningful breathing room;
6. substantial POIs feel different in function, not only silhouette;
7. under-level early entry is possible and dangerous without invisible rejection;
8. final facility exterior is discoverable before eligibility, while the deep finale remains legitimately gated;
9. Sky Drake unlock happens after ground-scale understanding and makes return exploration more enjoyable rather than mandatory first-pass navigation obsolete;
10. Terradragon remains reachable and fair for melee/ranged/support builds and does not spend routine long periods untargetable;
11. Terradragon arbitrary block grief is impossible;
12. Central Guardian active solo TTK falls inside ~285–320 s at intended build;
13. final boss phases alter space/decision-making instead of only stats;
14. Restore/Release/Partition choice is understandable without a hidden morality score;
15. multiplayer ending choices remain personal;
16. late join after another player's ending can still complete all personal main-story content;
17. postgame world remains playable and exploration content remains available;
18. R12 VFX never hides combat telegraphs or tanks performance below the project's acceptable client/server target;
19. optional anomaly exploration produces real relic/technique/story/collection value rather than repeated cache filler;
20. a 20–30 minute free-exploration session produces several memorable discoveries/decisions without feeling overcrowded.

---

# 26. Explicit non-goals

R12 does not add:

- sanity/corruption survival meter;
- random-control inversion;
- random teleport labyrinth;
- permanent screen distortion;
- mandatory environmental consumable upkeep;
- another player Lv system;
- anomaly reputation grind;
- anomaly token currency;
- prestige/paragon treadmill;
- automatic upgrade invalidation of all earlier gear;
- a full city inside the rift;
- default flying requirement before Sky Drake unlock;
- vanilla End/Nether mob population;
- vanilla Warden integration;
- giant unavoidable particle storms;
- humanoid Restoration Director HP-sponge fight merely because the story needs opposition.

---

# 27. What is locked vs still asset-gated

## Locked for implementation

- R12 suggested-entry Lv72 / local Lv72–80 pressure;
- physical early access and deep-finale-only story gate;
- sparse encounter philosophy;
- Edge Refuge service role;
- anomaly vocabulary: Phase Seam / Resonance Pulse / Overgrowth Fault / Echo Signal;
- major POI functions;
- Terradragon optional altar-summoned world-boss role;
- Terradragon no-summon-stone-grind rule and repeat cooldown direction;
- Sky Drake authored R12 unlock timing/role while preserving `MOUNTS.md` numbers;
- Obsidian Cathedral → Central Anchor dungeon/finale structure;
- separate systemic final guardian role;
- final encounter functional mechanics;
- personal three-ending execution / shared post-crisis state;
- postgame role and no prestige treadmill;
- multiplayer authority/performance/data contracts.

## Asset/source gates still open

- exact final guardian model, final name and anatomy-specific attacks;
- exact Farseer/Murmur/Reaper candidate acceptance;
- exact Anchor machinery models;
- exact rift crystal / obsidian-glass / arcane-material models and therefore final production names;
- exact optional anomaly fish identities;
- exact Sky Drake animation conversion/rig acceptance;
- exact R12 relic/weapon/armor visual bindings;
- exact final dungeon architectural modules where existing accepted packs do not meet the quality bar.

These gates must be closed through external-first intake and Minecraft-scale visual review. They are not permission to improvise placeholders during implementation.

---

# 28. Verification state

As of this design pass:

```text
DESIGN REVIEWED: YES
EXTERNAL REFERENCE REVIEWED: YES
EXTERNAL ASSET INTAKE: PARTIAL / GATED AS MARKED
CODE REVIEWED: N/A — design-only pass
TESTED: NO
BUILD VERIFIED: NO
JAR PRODUCED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

No build/CI is required for this docs-only design batch.

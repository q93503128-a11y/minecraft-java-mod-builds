# Open-World RPG — R05 Jungle Greenbelt Implementation Package

> Status: **DESIGN CANON — R05 world/story/traversal/service/reward flow is content/mechanics-closed; exact dungeon-boss visual/attack binding remains an explicit external-asset gate**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Story spine: `WORLD_STORY_CANON.md`  
> Region graph: `REGIONS.md`  
> Combat: `COMBAT_BALANCE.md`, `STATUS_AND_R01_ENCOUNTERS.md`  
> Loot/equipment: `LOOT_ECONOMY.md`, `EQUIPMENT_BALANCE.md`  
> Mounts: `MOUNTS.md`  
> Field systems: `GATHERING_FISHING_CAMP_HOUSING.md`, `FISHING_COLLECTION_HOUSING_MARKET.md`  
> Quest/state: `QUEST_WORLD_STATE.md`, `PARTY_MULTIPLAYER.md`  
> Quality contract: `DESIGN_COMPLETENESS_AUDIT.md`  
> External provenance: `EXTERNAL_SOURCES.md`  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins.

R05 is the peer Lv20 route to R04. It must prove that a dense jungle can be rich, layered and surprising **without becoming a navigation punishment**.

Its play sentence is:

```text
follow a great river and giant-canopy silhouettes into a living jungle
→ choose between ground trails, short canopy routes and river crossings without losing orientation
→ discover a river market whose people already adapted to floods, roots and seasonal change
→ hunt, fish, gather and unlock the agile Jungle Komodo while learning the local ecology
→ investigate an old regulator that intermittently forces the jungle back toward an obsolete controlled state
→ track a mature Earthloong as a native apex threat, not a corrupted quest monster
→ descend through an overgrown stepped temple into a readable root vault
→ isolate the failing regulator and leave with proof that a region can thrive by adapting locally instead of reconnecting every ancient system
```

R05 should make the player think:

> `Some places do not need the old network restored. They learned to live without it.`

That is evidence for autonomy, not a declaration that the `Release` ending is universally correct.

---

# 1. Locked regional identity

Preserved from `REGIONS.md`:

- terrain: northeastern jungle, bamboo-rich green belt, rivers and dense vegetation;
- suggested entry Lv: **20**;
- R04 and R05 are peer expedition routes;
- navigation is shaped by vegetation, rivers and landmarks rather than invisible walls;
- canopy landmarks keep orientation readable;
- ecology includes Gorilla, Capuchin, Toucan, Tiger, Komodo Dragon, Anaconda and selected insects;
- Leafcutter Ants remain ecology rather than combat spam;
- mature Earthloong is the native high-tier field-boss direction;
- settlement direction: river market / canopy-edge settlement;
- resources: rare herbs, resin, tropical food, venom materials and flexible wood/bamboo roles;
- dungeon: overgrown stepped temple / flooded root vault with exterior canopy + stone + roots;
- reward identity: poison/status builds, mobility/rapid attack, herbal/alchemy utility and nature/earth spell variants.

Production clarifications:

- R05 has **no baseline permanent poison-zone meter**, jungle sickness meter or universal movement slowdown;
- bamboo is not a new currency;
- every dense forest pocket does not need a hostile creature;
- mature Earthloong is native ecology and not revealed to be an Anchor creation;
- the dungeon boss is not locked until an external model/animation family passes intake.

---

# 2. External game-content precedents

These are structural references only. No proprietary art, maps, quest scripts or numbers are copied.

## 2.1 Monster Hunter: World — Ancient Forest

Capcom's own development material is especially relevant because the Ancient Forest intentionally pursued a dense, seamless, three-dimensional ecosystem.

Useful lessons:

- the environment and monster ecology were designed together;
- the giant ancient tree became a powerful macro-landmark;
- weak/safer ecology sits toward more open outer spaces while stronger predators occupy deeper, denser territory;
- environmental objects and vertical spaces can materially affect hunting;
- the first dense prototype became **too complicated and developers themselves got lost**, leading the team to add stronger guidance/navigation support.

Project adoption:

- one or two enormous canopy/river/temple silhouettes anchor the whole region;
- pressure rises as the player goes deeper instead of every square meter being equally dangerous;
- wildlife roles are designed around the terrain rather than dropped randomly into a jungle biome;
- short canopy/high routes create alternate approaches and sightlines;
- dense vegetation is repeatedly broken by readable light, water, clearings and landmark views.

Explicitly not adopted:

- maze-like connective corridors that require a permanent glowing trail just to traverse normally;
- long monster chases through confusing vertical tunnels;
- using density as an excuse for weak orientation.

References:

- `https://www.capcom.co.jp/ir/english/feature/2017_mh_crvoice.html`
- `https://www.famitsu.com/news/201808/24162857.html`

## 2.2 The Legend of Zelda — Breath of the Wild / Tears of the Kingdom

Useful precedent:

- the world supports different routes and player-chosen approaches rather than one prescribed path;
- landmarks, paths, edges and terrain help form a mental map;
- adding useful caves/vertical layers can make familiar space interesting again when each layer has a clear reason to exist;
- optional exploration works best when players can plausibly discover things from environmental clues instead of marker saturation.

Project adoption:

- R05 normally offers at least two viable approaches to substantial POIs;
- the river is a persistent `path/edge`, giant trees and temple spires are landmarks, and the market is a strong node;
- root caves and canopy paths exist because they connect real content, not because verticality itself is a feature;
- discoveries can be found through sightlines, sound, wildlife traces and terrain rather than automatic GPS.

Not adopted:

- a universal climb-anything system;
- physics-sandbox expectations that would require rebuilding the whole combat/world runtime;
- hundreds of disposable micro-shrines/checklist icons.

References:

- `https://www.nintendo.com/en-gb/News/2016/June/Nintendo-gives-players-unprecedented-freedom-of-adventure-in-The-Legend-of-Zelda-Breath-of-the-Wild-1113442.html`
- `https://www.nintendo.com/us/whatsnew/ask-the-developer-vol-9-the-legend-of-zelda-tears-of-the-kingdom-part-3/`

## 2.3 Shadow of the Tomb Raider — jungle readability / layered hubs

Useful precedent:

- jungle readability can be improved through lighting, foliage density changes and elevated overview points;
- canopy and underwater/low layers are useful only when each has actual gameplay value;
- large jungle hubs need vertical depth without becoming empty volume;
- sound can shift from grounded natural ambience to more psychological/unnatural treatment inside tombs.

Project adoption:

- main routes use controlled foliage density and deliberate light gaps;
- canopy overlooks reveal route relationships rather than serving only as collectibles;
- dungeon/tomb sound becomes more enclosed and strange while outdoor jungle remains grounded;
- different vertical layers reconnect frequently so the player does not need to memorize a three-dimensional maze.

Not adopted:

- lethal trap density as the main dungeon identity;
- long cinematic climbing sequences replacing RPG traversal/combat.

Reference direction:

- `https://www.gamedeveloper.com/audio/building-an-immersive-soundscape-in-shadow-of-the-tomb-raider---full-q-a`

## 2.4 Horizon Forbidden West — settlement authenticity / quest-space iteration

Useful precedent:

- settlements feel credible when people, props, architecture and services reflect how local people actually live;
- progression systems work best when built in dialogue with the whole world;
- strong quests combine story beats, map/flow planning, traversal, combat, interaction and visible world changes;
- overly long traversal routes should be cut or shortened rather than defended merely because they are realistic.

Project adoption:

- R05 market props/jobs/services reflect fishing, herbs, bamboo/wood, river travel and wildlife;
- the regional temple route mixes exploration, combat and environmental evidence instead of becoming only a boss corridor;
- after the regional outcome, river/market activity changes visibly;
- first-clear dungeon routing creates a repeat shortcut rather than replaying story friction forever.

References:

- `https://blog.playstation.com/2021/11/22/horizon-forbidden-west-an-authentic-world/`
- `https://blog.playstation.com/2022/08/10/how-guerrilla-created-vegas-in-horizon-forbidden-west/`

---

# 3. External-first source stack

## 3.1 Alex's Mobs Continued — jungle ecology / Komodo mount

Current Fabric 26.2 continuation is already in the pinned dependency direction and preserves the original creature roster, models, animations and behaviors.

R05 candidate ecology from that dependency includes the already-canonical:

- Gorilla;
- Capuchin Monkey;
- Toucan;
- Tiger;
- Komodo Dragon;
- Anaconda;
- Leafcutter Ant.

Project rules:

- project data owns Lv, stats, spawn density, loot and encounter placement;
- passive/neutral creatures keep ecological roles instead of all becoming XP targets;
- dependency-wide global spawn rules are filtered into the authored R05 region;
- creature behaviors are inspected in the current 26.2 build before final numerical combat tuning;
- current storefront license metadata remains inconsistent across surfaced Modrinth/CurseForge pages, so ordinary dependency use is distinct from copying source/asset bytes into the public repository.

`MOUNTS.md` already locks the **Jungle Komodo** as R05's first clearly fast ground mount role. R05 must therefore supply the authored ecology/handler unlock instead of leaving the mount progression disconnected from the region.

Current dependency reference:

- `https://modrinth.com/mod/alexs-mobs-continued`

## 3.2 Threateningly Mobs Continued — mature Earthloong

Current 26.2 Fabric continuation remains available and its source lineage explicitly places **Earthloong** in forest/jungle environments.

Project use:

- R01 Earthloong remains the first dungeon boss / early individual;
- R05 uses a **mature native Earthloong** as an optional high-tier field/world-boss hunt;
- the mature fight must expand the source identity instead of copy-pasting the R01 Lv8 moves with bigger numbers;
- project spawn/reward/stat/arena rules override donor defaults.

License/public-repository caution remains exactly as already documented for Threateningly Mobs: use as dependency while exact continuation/source asset reuse terms remain source-specific.

Reference:

- `https://modrinth.com/mod/threateningly-mobs-continued`

## 3.3 River-market / tropical architecture

Primary redistributable support direction:

- **Kenney `Pirate Kit`** — CC0, 3D, 70 assets, useful for docks, raised platforms, tropical vegetation, water-adjacent structures and props after removing pirate-specific visual language that does not fit;
- **Kenney `Watercraft Kit`** — CC0 candidate for small boats/rowboats/fishing craft if actual model intake fits the project;
- KayKit / accepted CC0 nature resources for rocks, plants and utility props;
- existing Medieval/Fantasy prop families only where materials/forms can be adapted coherently into the settlement rather than copying an R01 village into a jungle.

The R05 market should read as a **river settlement first**, not a pirate town.

No skull flags, cannon-lined docks or unrelated pirate kits are retained just because the base pack is useful.

## 3.4 Jungle / bamboo vegetation

Macro canopy remains terrain/block composition for performance.

External assets are used for:

- gathering nodes;
- distinctive plants;
- bamboo/wood bundles and market props;
- small landmark dressing;
- temple/root detail;
- rare oversized flora when performance allows.

Accepted Quaternius/KayKit nature families remain candidates under their exact package/source licenses. New Quaternius assets obtained after the 2026-08-28 license update must follow QAL rather than being blanket-labelled CC0.

## 3.5 Dungeon-boss external gate

R05's dungeon requires a visually distinct **old-regulator / root-vault guardian** that is not another Earthloong, scaled Nature Spirit or vanilla Iron Golem.

Current source pools worth direct visual comparison:

1. Quaternius `Ultimate Monsters` — older package page publishes 50 fully animated monsters under CC0;
2. Quaternius `Bestiary - Dungeon Monsters Kit` — 7 newer rigged/retargetable monster designs under current QAL, compatible with Universal Animation Library but not bundled with animations;
3. another legally redistributable animated temple/guardian model found during intake if it fits better.

Status:

```text
DUNGEON BOSS VISUAL IDENTITY: OPEN ASSET GATE
REGIONAL DUNGEON ROLE/FLOW: LOCKED
```

Do **not** lock a player-facing boss name, anatomy-specific weak point or animation-specific attack list until the actual model is chosen.

This is deliberate compliance with the project's external-first rule, not permission for implementation to invent a placeholder boss.

---

# 4. Story role — the strongest early case for local autonomy

R05 is one of the optional R04–R07 Act-II evidence regions.

Its evidence package argues:

> A region can become resilient by adapting to natural/local cycles after ancient centralized control disappears. Reconnecting an obsolete regulator can damage a society that no longer needs it.

Regional history:

- an Anchor-era regulator once constrained flood pulses, groundwater, root growth and selected river channels around a large temple/management complex;
- the system gradually failed generations ago;
- the jungle did **not** collapse;
- rivers migrated locally, giant roots reclaimed stone, wildlife spread and people adapted settlements/trade to seasonal routes;
- modern residents rely on flexible docks, raised walkways, ferries, known flood shelves and local ecological knowledge rather than the old regulator;
- recent continental instability causes the dormant regulator to pulse intermittently again;
- these pulses close side channels, dry some nursery pools, push water into obsolete routes and create abnormal root pressure around the old temple;
- fully reactivating the old settings would make some old roads predictable again but would damage the modern river-market ecology/economy.

The player ultimately isolates/stabilizes the dangerous regulator interface without restoring its obsolete continental control pattern.

This is a practical regional success for autonomy, not proof that every other region should dismantle its Anchor infrastructure.

---

# 5. Local people / regional conflict

Primary hub: **river market / canopy-edge settlement** built where several navigable channels meet firm high ground.

Normal life should visibly include:

- fish unloading / smoking / cooking;
- herb sorting and alchemy ingredients;
- bamboo/wood bundles and light construction;
- small watercraft / docks;
- produce/fruit stalls;
- wildlife wardens / route scouts;
- raised walkways and rain cover;
- residents using different paths as water levels/weather change.

Key local roles:

## 5.1 River Factor / Market Coordinator

- cares about trade routes and reliable access;
- initially sees any old system capable of stabilizing channels as potentially useful;
- changes position as evidence shows the old regulator is optimized for a world that no longer exists;
- keeps the debate practical rather than ideological.

## 5.2 Forest Warden / Ecologist

- understands predator territory, root growth, herbs and seasonal river behavior;
- argues that the jungle has developed a functioning new equilibrium;
- is not anti-technology and supports local bridges, medicine, tools and safe route engineering;
- provides a grounded autonomy perspective.

## 5.3 Boatwright / Handler

- connects docks, route maintenance and the Jungle Komodo unlock;
- knows how people move when ordinary roads are not the best answer;
- gives the mount system a believable regional owner instead of making Komodo registration a menu unlock.

Recurring `Anchor Scholar`, `Cartographer / Ranger`, `Engineer / Smith` or `Rival Wanderer` can appear when the main story routes through R05.

---

# 6. Spatial progression / local pressure

Suggested entry Lv remains **20**.

| Sub-area | Local pressure | Role |
|---|---:|---|
| western/northern approach / river fringe | Lv 19–20 | readable transition / first landmark views |
| river market / cultivated edge | Lv 20 | hub / services / Komodo lead |
| broad river forks / bamboo groves | Lv 20–22 | fishing / gathering / common ecology |
| mid-jungle root roads / giant-tree belt | Lv 21–23 | multiple routes / predator pressure |
| canopy overlooks / ancient causeways | Lv 22–24 | route mastery / discoveries |
| temple approach / regulator disturbance | Lv 23–25 | elite pressure / main regional evidence |
| mature Earthloong basin | **Lv 26** | optional field/world boss |
| stepped temple / root vault dungeon | Lv 24–27 | regional dungeon |
| final guardian role | **Lv 27** target | dungeon climax |

The player may leave for R04/R06/other reachable territory without finishing R05.

---

# 7. Navigation / layer contract

R05 should be dense but mentally map-able.

## 7.1 Macro landmarks

Use at least four persistent orientation anchors when the actual Azari terrain supports them:

1. **great river** — primary path/edge;
2. **one giant canopy tree / crown silhouette** — region-scale landmark;
3. **stepped temple crown / broken spire** — distant dungeon landmark;
4. **river market smoke, banners, roofs or elevated tower** — settlement landmark.

Secondary anchors:

- distinctive waterfall;
- bamboo ridge;
- pale cliff/stone shelf;
- giant root arch;
- suspension crossing.

## 7.2 Three traversal layers, not a 3D maze

R05 uses:

### Ground/root layer

- most combat and ordinary travel;
- clear foot trails, root arches and clearings;
- Komodo's best handling environment.

### River/low-water layer

- fishing, market access, alternate routes and some POIs;
- bridges/shallows/short boats where external asset/implementation fits;
- no prolonged forced swimming combat.

### Short canopy/elevated layer

- overlooks, shortcuts, rare nodes and selected encounter approaches;
- built from obvious ramps, roots, stairs, platforms or short authored climbs;
- repeatedly reconnects to ground;
- not a permanent tree-top parkour network.

Rules:

- a player should rarely spend >2–3 minutes on an elevated route without a clear view/reconnection choice;
- cave/root-vault entrances are visibly associated with surface landmarks;
- dungeon/local maps must communicate vertical layer/depth cleanly rather than stacking unreadable icons;
- no blanket vine-climbing system is added solely for one region.

## 7.3 Foliage readability

Main routes deliberately use:

- lower foliage density;
- brighter sky/light gaps;
- stronger ground material/path contrast;
- repeated river/root/cairn/prop signposting;
- vegetation walls only where they serve a readable boundary.

Do not require the player to follow floating arrows through leaves.

---

# 8. Weather / rain

Tropical rain is an atmosphere and content condition, not a punishment meter.

Heavy rain can:

- alter ambient sound and visibility modestly;
- increase selected herb/fish/event weights;
- make waterfalls/side channels more visually active;
- trigger a few authored regional events;
- change NPC shelter/market presentation.

Heavy rain does **not** baseline:

- apply permanent movement slow;
- add a Wet debuff to every fight;
- drain Stamina faster;
- make all climbing impossible;
- flood/erase the player's only route without warning.

Time/weather-specific rare catches or herbs remain optional collection advantages, never mandatory main-progression waits.

---

# 9. POI package

## Major POI A — River Market

Functions:

- main social/service node;
- navigation anchor;
- visible regional economy;
- mount/boat/fishing/herb loops intersect here.

## Major POI B — Crownroot Overlook

Functions:

- giant-tree / high-root panorama;
- teaches region layout;
- exposes temple, river branches and at least one hidden route clue;
- rare plant/wildlife observation opportunity;
- no chest required.

## Major POI C — Splitwater Gardens

Functions:

- shallow channels / nursery pools / herbs/fishing;
- concrete evidence of intermittent regulator pulses changing water;
- one regional event location;
- demonstrates why old control can harm current ecology.

## Major POI D — Bamboo Causeway

Functions:

- fast ground/Komodo route;
- compact ambush/territorial predator space;
- bamboo/flexible wood gathering;
- bridge/route worldbuilding.

## Major POI E — Earthloong Basin

Functions:

- optional mature Earthloong field-boss arena;
- visible territorial marks / disturbed earth / ancient roots;
- high-value hunt and trophy source;
- native ecology, no Anchor altar required.

## Major complex — Stepped Temple / Root Vault

Handled in §17.

Smaller discoveries may include:

- old flood markers above current river level;
- abandoned ferry steps;
- Toucan nesting overlook;
- Capuchin foraging grove;
- Tiger scratch/kill site;
- medicine-gatherer shelter;
- root bridge destroyed by regulator pulse;
- rare fish pool;
- Leafcutter Ant path crossing.

Do not convert each into an icon/checklist chest.

---

# 10. Settlement / services / housing

Target daytime physical population:

```text
12–17 functional / trader / fisher / warden / service NPCs
6–10 ambient residents / travelers
```

No vanilla villagers.

Baseline services:

- shrine / fast travel;
- inn / food / rest;
- river-market general merchant;
- bank / Material Vault;
- healer / alchemy service;
- herb/fish/material buyers;
- regional contract / warden board;
- smith capable of ordinary regional equipment work;
- Komodo handler / stable registration;
- furnishing/property interaction;
- small watercraft/ferry role only where actual implementation/asset quality supports it.

Architecture direction:

- raised decks/docks and tropical water-edge props can use/adapt Kenney CC0 Pirate Kit primitives where they fit;
- remove pirate-specific symbols/weapons;
- integrate with accepted fantasy/market props;
- houses and services must look built for rain/water access, not like R01 buildings dropped into palm trees.

## Housing

R05 offers regional architectural alternatives rather than mandatory tier escalation.

Target:

- 1–2 Town House-class stilt/river homes around the existing ~9,000 Gold band;
- optionally 1 Large House-class river/canopy home around ~25,000 Gold **only if R04 does not already provide a strong first Large House and the actual shell is excellent**.

No gameplay advantage beyond normal tier storage/furnishing/display rules.

---

# 11. Jungle Komodo mount integration

The R05 mount progression from `MOUNTS.md` is now explicitly part of the regional package.

Working quest role: **handler / route ecology quest**, not random taming spam.

Flow:

```text
meet handler/boatwright near market
→ learn Komodo routes/territorial behavior through a real field objective
→ help recover/register one suitable creature without repeatedly feeding RNG items
→ unlock permanent Jungle Komodo identity
→ pay existing 600 Gold registration/tack fee
```

Existing canonical movement remains:

```text
cruise: 8.4 b/s
dash: 10.5 b/s for 2.5 s
very high turning response
Resolve: 1.20x player MaxHP
```

Regional payoff:

- ground/root paths that were readable on foot now become satisfyingly fast to traverse;
- tight jungle steering differentiates Komodo from Trail Stag;
- it does not climb vertical trees or bypass temple progression;
- mount remains useful after R05 because high handling is a permanent traversal niche.

Combat bite/venom ability remains optional exactly as `MOUNTS.md` states: only ship it if the current dependency animation/hitbox integration is clean.

---

# 12. Gathering / regional materials

Do not add a new profession or huge material tier.

## 12.1 Bamboo / flexible wood role

Use authored bamboo/flexible timber nodes rather than breaking thousands of ordinary decorative blocks for progression.

Baseline role:

```text
yield: 2–3
personal respawn: 5–7 active min
tool: Field or Refined Axe depending node density
uses: furniture, selected bows/polearms, regional construction/service recipes, ordinary sale
```

Final item/model naming waits for exact accepted external node visuals; ordinary `Bamboo` is acceptable if the visual is genuinely bamboo.

## 12.2 Resin continuity

R02 tree-resin role remains useful in R05 instead of inventing `Jungle Resin II`.

R05 may provide:

- alternate richer source nodes;
- different recipe uses;
- no separate currency/tier item unless a distinct accepted visual/material function justifies it.

## 12.3 Rare jungle herb role

One higher-tier tropical herb supports:

- poison/cleanse alchemy;
- selected food/tonic;
- regional equipment recipe;
- contracts.

Working behavior:

```text
yield: 1
personal respawn: 10–14 active min
tool: Harvest Knife / Sickle
locations: humid shade, river islands, temple fringe
```

Final name/model waits external intake.

## 12.4 Venom material role

Tiger/Gorilla are **not** venom sources.

A small number of appropriate reptile/invertebrate enemies may drop a normalized venom ingredient used in poison/status recipes.

Rules:

- one readable shared venom material is preferred over one venom currency per species;
- loot source must visually make sense;
- no mandatory farming of rare predators for routine alchemy.

---

# 13. Fishing / collection

R05 is one of the stronger freshwater Fish Codex regions.

Use:

- main-river bends;
- shaded tributaries;
- floodplain pools;
- deep root-shadow water;
- market docks.

R05 Codex target:

```text
5–7 regional/shared fish identities
```

Composition target:

- 2 common warm-river species;
- 1 shared species overlapping R02/downstream waters;
- 1 uncommon shaded/deep-pool species;
- 1 rain-favored species;
- 1 rare/trophy jungle species;
- optional seventh species tied to a temple/root pool only if ecology/model quality supports it.

No catch required for main-story progression.

Fish continue to feed:

- cooking;
- sale;
- personal best/Trophy records;
- housing display;
- selected regional contracts.

Final species names await external animated fish intake.

---

# 14. Ecology / encounter roles

R05 should feel biologically dense without becoming hostile-entity dense.

## 14.1 Capuchin / Toucan

Ambient/foraging ecology.

- not ordinary combat targets;
- may interact with fruit/props where donor behavior supports it;
- provide movement/sound/life in canopy edges;
- do not constantly steal important quest items or grief inventory.

## 14.2 Leafcutter Ant

Ecology/system flavor.

- colonies/trails can visually connect vegetation to nests;
- no swarm-combat XP farm;
- entity count must remain aggressively bounded for performance;
- use local/short trail behavior rather than region-wide ant simulation.

## 14.3 Gorilla

Large neutral/territorial social wildlife.

Project direction:

- group displays/warnings before aggression where donor animation allows;
- bounded group-response cap;
- dangerous when provoked, but not a normal hostile pack;
- useful ecological/resource identity without turning Gorilla hunting into routine gear farming.

Current 26.2 continuation recently fixed pathfinding for Gorilla/Bison/Rhino/Elephant/Leafcutter Ant, so direct current-build play inspection remains required before stat tuning.

## 14.4 Tiger

Primary stalking predator.

Project direction:

- sparse territorial placement;
- uses vegetation/height/cover where pathing supports it;
- clear visible/audio pre-pounce cue;
- disengages after territory escape;
- does not spawn every 40 blocks just because jungle = tiger.

Working band:

```text
Lv 22–24
role: dangerous predator / occasional elite-grade encounter depending authored variant
```

## 14.5 Komodo Dragon

Natural regional reptile and mount source.

- ordinary wild Komodo behavior remains distinct from permanently registered mount ownership;
- authored handler quest creates mount unlock;
- normal individuals do not all become attack-on-sight trash;
- venom/bite behavior is normalized only after direct donor review.

## 14.6 Anaconda

Ambush/water-edge heavy predator where current 26.2 behavior is stable.

Rules:

- low density;
- readable coil/strike/grab warning;
- grab mechanics must support server-authoritative escape/teammate interaction if retained;
- if donor grab behavior is unstable or unfair in multiplayer, simplify the attack rather than preserve it for novelty.

---

# 15. Mature Earthloong field/world boss

R05 returns Earthloong at its native high-tier stage. This must feel like **species progression**, not R01 boss reuse.

Working role:

```text
Lv: 26
role: optional field/world boss
HP target: ~14,000–15,500
Defense: ~70
MR: ~55
Poise: 225
solo active TTK target: ~200–225 s
```

The HP band is aligned to the canonical `BenchmarkDPS(L) × target active TTK` rule. Final HP may move after donor mobility/uptime review, but any excessive burrow/reposition downtime is fixed first rather than converted into more health.

## 15.1 Arena

- broad root basin / eroded river shelf with enough open combat floor;
- several giant roots/stone shelves create meaningful line/height changes without camera traps;
- authored breakable props only;
- no random jungle terrain grief.

## 15.2 Difference from R01 Earthloong

R01 teaches:

- basic sweeps/charge;
- Lightning Furrow lanes;
- Root Breaker;
- simple phase-2 lightning escalation.

R05 mature encounter emphasizes:

- longer-body repositioning and arena control;
- visible earth/root displacement;
- more deliberate weak-point windows;
- one advanced lightning/earth interaction;
- less reliance on repeating the R01 three/four-lane pattern.

The existing species identity remains recognizable, but the fight cannot be `Earthloong Lv8 x2.5 stats`.

## 15.3 Mature attack direction

Exact animation bindings remain donor-review dependent, but the accepted kit must satisfy these functional roles:

### Close-body control

- one readable sweep/body-turn tool;
- benchmark damage ~14–18%;
- prevents permanent rear-side camping.

### Committed earth surge

- strong visible ground/root path;
- benchmark damage ~25–30%;
- real miss/punish window;
- no invisible shockwave beyond visible terrain treatment.

### Lightning-root convergence

- mature signature replacing routine R01 lane repetition;
- several visible root/ground anchors charge first, then lightning resolves between/through them;
- positioning puzzle is readable before damage;
- same-cast multi-hit cap prevents overlapping instant deletion.

### Short subterranean/reposition action only if supported

- no long invulnerability;
- visible ground trace;
- <=4–5 s routine untargetable duration;
- omit entirely if source animation cannot support it honestly.

## 15.4 Weak point

The R01 luminous head/crest precedent may remain, but mature weak-point timing must be tied to actual R05 animation review.

Do not invent a new belly/core weak point without visible anatomy.

## 15.5 Rewards

First eligible defeat:

- guaranteed Superior+ R05 equipment;
- 2 mature model-linked signature materials;
- 15% direct Mythic/signature roll;
- field-boss EXP/Class XP under existing canon;
- one housing trophy form only if the accepted model/anatomy yields a visually sensible display.

No generic `Earthloong Token`.

---

# 16. Dynamic events

R05 event pool remains restrained.

## Flooded Crossing

- short local route/event after heavy rain or regulator pulse;
- player finds alternate root/bridge route or helps restore a crossing;
- no unavoidable region-wide flood state.

## Predator on the Trade Path

- Tiger/Anaconda/other authored predator pressure around one route;
- can be avoided or handled;
- does not permanently turn every travel session into escort duty.

## Stranded Skiff / River Cargo

- short interaction/combat/recovery event near water;
- no long NPC escort AI through dense jungle;
- rewards regional goods/Gold rather than event currency.

## Ant Trail Discovery

- ecology/discovery event, not combat;
- can reveal plant/resource clue or journal note;
- keeps world activity from meaning `something attacks you` every time.

---

# 17. Regional dungeon — Stepped Temple → Flooded Root Vault → Regulator Chamber

Target first-clear wall-clock length:

```text
~22–32 minutes
```

The dungeon must showcase **R05 traversal language** rather than becoming a generic stone tunnel.

## Stage 1 — Exterior stepped temple

- visible from multiple jungle sightlines;
- exterior stair/root/canopy approach;
- player reaches it through at least two routes;
- first strong evidence that temple geometry once controlled/observed water flow.

## Stage 2 — Root court

- giant roots split old plazas/chambers;
- combat uses pillars/roots and height variation;
- one optional herb/material/lore branch;
- no tiny doorway maze.

## Stage 3 — Flooded archive / water-control level

- shallow/waist-deep or platformed water only where controls remain good;
- route reading through visible sluice/channel architecture;
- one main environmental interaction changes water/route state;
- no repeated valve puzzle in every room.

## Stage 4 — Deep root vault

- natural root/stone space visibly merged with ancient infrastructure;
- evidence shows the forest/river adapted after old control declined;
- local modern practices are visible in notes/markers/objects where believable;
- the main-story evidence is understandable through space, not a ten-page text dump.

## Stage 5 — Regulator chamber / guardian

- final boss arena around old machinery and living roots;
- exact guardian model/attack identity remains asset-gated;
- fight must visually express the conflict between old rigid infrastructure and the reclaimed jungle without generic green particle spam.

## First-clear resolution

After the boss:

- player isolates the dangerous continental coupling/control path;
- local flow returns to a stable **modern regional state**, not to the ancient map;
- one visibly blocked/altered side channel reopens or equal late-join-safe route change occurs;
- evidence package records a successful bounded decommission/local adaptation case.

## Shortcut / repeat

- unlock a direct root/temple service route back toward the exterior;
- repeats skip first-clear story interactions;
- repeat dungeon remains valuable for gear/materials without re-solving exposition mechanisms.

---

# 18. Dungeon-boss role contract — exact identity pending asset intake

Because the final model is intentionally not locked, implementation must **not** build a throwaway boss and promise to replace it later.

Target encounter role after model selection:

```text
Lv: ~27
role: dungeon boss / ancient regulator guardian
solo active TTK target: ~170–200 s
Poise band: ~200–230 depending accepted body/weapon mass
```

Required design properties:

- visually distinct from Earthloong and Nature Spirit;
- readable high-quality external model with animation coverage sufficient for at least 4 meaningful actions/reactions;
- one core mechanic tied to regulator/root-space control;
- no constant invulnerability;
- no generic `adds every 30 seconds` unless the selected model/source naturally supports a summon identity;
- melee/ranged builds both receive real punish opportunities;
- phase change modifies behavior/space, not only damage/speed;
- visible attack range = server hit range.

Allowed final directions after asset comparison include:

- ancient humanoid/stone/wood regulator guardian;
- distinct beast/statue guardian with enough animation coverage;
- another coherent model that strongly fits the temple's old infrastructure.

Rejected shortcuts:

- scaled R02 Goleling-Evolved candidate;
- second Earthloong;
- recolored Iron Golem;
- static statue + particle beams;
- random Quaternius model chosen only because it is free.

Final attack table and signature-material name are written **after** accepted model intake.

---

# 19. Regional quest / discovery flow

Working quest-name candidates may change during narrative polish; functions are canonical.

## `Where the River Forks`

- enter market;
- investigate strange route/water behavior;
- meet market/warden perspectives;
- establish that seasonal variation is normal and current pulses are not.

## `Paths Above the Water`

- explore two of several route/observation points in flexible order;
- ground/canopy/river approaches demonstrate regional navigation;
- unlocks deeper regulator evidence without a checklist wall.

## `A Map That No Longer Fits`

- compare old temple/regulator evidence with current settlement/ecology;
- shows ancient infrastructure was built for different channels/conditions;
- makes the autonomy argument experiential rather than ideological exposition.

## `Roots Beneath the Steps`

- regional dungeon / regulator resolution;
- valid R04–R07 Act-II evidence package;
- does not require killing mature Earthloong.

## `A Faster Trail`

- authored Jungle Komodo ecology/handler quest;
- may begin independently of main chain after market discovery;
- permanent mount unlock after existing 600 Gold registration.

## Mature Earthloong hunt discovery

- optional;
- begins from terrain/body marks, witness report or distant sighting;
- never becomes a main-story gate.

---

# 20. Regional outcome / memory

After the regional climax:

Shared late-join-safe consequences may include:

- one side channel / crossing returns to a stable usable route;
- selected raised market/dock activity expands;
- one previously awkward route gains a permanent footbridge/ferry/clear passage;
- merchant/alchemy/fish stock reflects improved access;
- regulator pulse event frequency changes/reduces.

Personal consequences:

- R05 autonomy evidence package;
- local NPC dialogue flags;
- Komodo unlock/registration;
- Earthloong discovery/clear;
- temple first-clear/reward state;
- Fish Codex/discovery records.

The jungle remains wild. The player does **not** tame or flatten the entire ecosystem.

---

# 21. Reward identity

No R05 token currency.

Regional gear/affix emphasis:

- Poison buildup / Poisoned payoff;
- cleanse/status-handling utility;
- DEX/attack-speed/finesse support within existing caps;
- movement and dodge utility;
- WIL/INT nature/earth/status support;
- selected healing/herbal bonuses;
- anti-grab/territorial-predator utility where mechanically justified;
- jungle traversal/mobility benefits that remain useful elsewhere.

Avoid:

- every item simply giving Poison Resistance;
- region-only bonuses like `+20% damage in R05`;
- forcing poison build to use only R05 equipment forever.

---

# 22. Audio / presentation contract

Required regional audio states:

1. river-market daytime life;
2. ordinary jungle canopy / insects / birds / water;
3. heavy-rain state;
4. deeper predator territory / reduced social sound;
5. stepped-temple exterior;
6. root-vault / water-control interior;
7. mature Earthloong encounter;
8. final temple-guardian/boss state after asset selection.

Rules:

- constant insect loops must not become high-frequency fatigue;
- Toucan/Capuchin/ambient wildlife calls are spatial/bounded, not every-second spam;
- market sound must communicate trade/work without hundreds of ticking ambient entities;
- rain lowers/changes some ambient layers rather than simply adding louder noise on top of everything;
- dungeon sound shifts from grounded jungle reality toward stone/water/root resonance;
- external-first BGM/SFX sourcing remains required.

---

# 23. Multiplayer / authority

Server owns:

- regulator/route shared state;
- quest/evidence progression;
- Komodo unlock/registration;
- boss encounter state;
- Earthloong path/hit validation;
- dungeon route/water-state changes;
- gathering/fishing personal state;
- personal loot/first-clear rewards;
- dynamic-event eligibility.

Later multiplayer tests must include:

- players taking different R05 navigation layers and regrouping without quest loss;
- one player's shared channel/bridge restoration does not erase another player's personal evidence interaction;
- late join sees regional route outcome but can still complete personal story;
- two players on different quest steps can share Earthloong/dungeon encounters safely;
- **one valid hit or one valid heal/protection/support/control action is sufficient for combat reward/kill-objective eligibility under `PARTY_MULTIPLAYER.md`**;
- Komodo registration remains personal;
- any Anaconda/grab behavior is server-authoritative and escapable/consistent;
- dungeon water/route state remains coherent after disconnect/chunk unload;
- no duplicate first-clear/reward on reconnect.

`MULTIPLAYER TESTED` remains NO until real clients are used.

---

# 24. Performance constraints

- no every-tick full-jungle wildlife/ecology simulation;
- dense visual jungle comes primarily from terrain/block composition and bounded props, not massive Display Entity populations;
- Leafcutter Ant colonies have strict loaded/local caps;
- ambient Capuchin/Toucan counts remain bounded;
- predator territory logic uses local entity/encounter state;
- heavy-rain effects are region/weather driven, not one emitter per leaf;
- fish schools remain presentation-lightweight;
- Earthloong boss logic only runs in active encounter scope;
- dungeon regulator/root effects stop when unloaded;
- use profiler/playtest for final vegetation/VFX/entity density.

---

# 25. R05 implementation / intake gates

Before player-facing implementation is accepted:

- inspect current 26.2 Gorilla / Capuchin / Toucan / Tiger / Komodo / Anaconda / Leafcutter Ant models, behavior and animation;
- inspect current Jungle Komodo riding integration against `MOUNTS.md`;
- inspect mature Earthloong model/animation and compare directly with R01 presentation so the second encounter genuinely escalates;
- select an exact external dungeon-boss model before writing its final attack table/name;
- select exact bamboo/flexible-wood, rare-herb and venom icons/models;
- select R05 fish models;
- bind river-market architecture/props with coherent CC0/QAL provenance;
- bind NPC outfits for traders/fishers/wardens/handler;
- bind canopy/river/temple VFX and audio;
- verify every important model at Minecraft scale;
- record source-specific license and SHA-256 for committed external bytes where applicable.

No placeholder vanilla Panda, generic Villager market, recolored Spider, Iron Golem temple boss or green-particle-only jungle magic.

---

# 26. Acceptance targets

A later playable R05 is not accepted merely because navigation and quests technically work.

## Navigation

- can a new player repeatedly orient using river / canopy / temple / market landmarks?
- does dense foliage create discovery without frequent `where am I?` frustration?
- do elevated routes reconnect before becoming a 3D maze?
- does the Komodo make revisiting familiar routes more fun rather than merely mandatory?

## Ecology

- does the jungle feel alive even when nothing attacks?
- are Gorilla/Capuchin/Toucan/ants doing believable regional roles?
- are Tiger/Anaconda encounters rare/readable enough to feel like predators rather than trash mobs?
- does mature Earthloong feel like a real species escalation from R01?

## Regional story

- can the player understand that modern residents adapted successfully without ancient control before being told the ending philosophies?
- does the old regulator visibly conflict with modern river/root patterns?
- does the final local isolation/decommission solve a real regional problem without implying all Anchors are bad?
- does the market/route state remember completion?

## Side loops

- do fishing/herbs/bamboo/resin/venom connect to cooking, alchemy, furnishing, gear or sale?
- are there enough reasons to leave the main trail without turning the map into icons?
- is mount acquisition a meaningful region event instead of a menu reward?

## Dungeon

- is surface → temple → root vault layering always readable?
- is water traversal brief/controlled?
- does the final guardian model/kit feel purpose-built after intake?
- can repeat clears use the shortcut and skip story friction?

## Presentation

- does the market look adapted to rain/river access rather than reused R01 architecture?
- are jungle ambience and rain rich without audio fatigue?
- are external assets visually coherent rather than a collage?

## Technical

- no route/world-state loss on save/load;
- no reward duplication;
- no ant/ambient-mob performance spike;
- stable current-build donor AI/pathfinding;
- multiplayer personal/shared states remain separated;
- profiler targets met in the densest jungle hub/encounter.

Verification state at design authoring time:

```text
DESIGN REVIEWED: YES
EXTERNAL REFERENCE REVIEWED: YES
CODE REVIEWED: N/A
TESTED: NO
BUILD VERIFIED: NO
JAR PRODUCED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

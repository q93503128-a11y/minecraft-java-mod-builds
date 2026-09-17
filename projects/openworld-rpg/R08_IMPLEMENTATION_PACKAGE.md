# Open-World RPG — R08 Bloomveil Implementation Package

> Status: **DESIGN CANON — R08 world/story/traversal/service/combat/reward flow is content/mechanics-closed; exact dungeon-boss and several magical-flora bindings remain external-intake gates**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Story spine: `WORLD_STORY_CANON.md`  
> Region graph: `REGIONS.md`  
> Combat: `COMBAT_BALANCE.md`, `STATUS_AND_R01_ENCOUNTERS.md`  
> Loot/equipment: `LOOT_ECONOMY.md`, `EQUIPMENT_BALANCE.md`  
> Field systems: `GATHERING_FISHING_CAMP_HOUSING.md`, `FISHING_COLLECTION_HOUSING_MARKET.md`  
> Quest/state: `QUEST_WORLD_STATE.md`  
> Quality contract: `DESIGN_COMPLETENESS_AUDIT.md`  
> External provenance: `EXTERNAL_SOURCES.md`  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins.

R08 is the first high-tier region where the world should feel openly **magical rather than merely fantastical**. It must not be R05 with brighter flowers.

Its play sentence is:

```text
cross from grounded midgame regions into cliffs and forests where pollination, light, roots and ambient magic visibly interact
→ orient by giant blossoms, luminous canopy breaks, scholar beacons and glass-root structures rather than a wall of quest markers
→ discover a secluded sanctuary whose residents deliberately study and coexist with a magical ecology that did not exist in this form under full Anchor regulation
→ gather pigments, pollen and mana-active flora that feed real alchemy/crafting/spellcraft loops
→ navigate brief floating-root / flower-bridge / cliff routes without gaining a new permanent parkour subsystem
→ investigate modern Restoration tests that stabilize some anomalies but also begin suppressing valuable magical cycles
→ optionally invoke the Titan Rabbit through a discovered ritual site instead of finding it randomly wandering the forest
→ descend into a fae archive / glass-root observatory where records show the old network intentionally damped local magical emergence
→ stop an over-stabilization event and preserve a bounded local safety system without erasing the region's evolved ecology
→ leave with high-tier evidence that stability itself can have a real cost
```

R08 should make the player think:

> `The world did not merely survive after the old system weakened. In some places, something new and valuable was able to exist.`

This makes R08 one of the strongest high-tier arguments against blindly restoring the historical network, without declaring all regulation bad.

---

# 1. Locked regional identity

Preserved from `REGIONS.md`:

- terrain: flower forest, fairy/whimsical forest, pollinating cliffs and strongly magical woodland;
- suggested entry Lv: **44**;
- R08 and R09 are peer high-tier routes;
- deliberately more magical than R01/R02/R05 woodland;
- ecology direction includes Hummingbird, Flutter, Bunfungus and selected non-hostile fantastical life;
- Knowledge Fairy belongs only in authored library/ruin/knowledge spaces;
- Nature Spirit variants and Moonpriest can provide authored high-tier magical pressure;
- Titan Rabbit remains the strange optional field/world-boss direction;
- settlement direction: secluded sanctuary / scholar enclave;
- resources: mana flora, pigments, magical pollen, light materials and spellcraft ingredients;
- dungeon direction: fae archive / glass-and-root observatory / overgrown library;
- reward identity: Mana sustain, spell shaping, support/healing interactions, status conversion and magical accessories.

Production clarifications:

- **R08 is not another jungle navigation region**;
- no universal levitation/floating-island platforming system is introduced merely because the region is magical;
- ambient magic changes presentation and authored mechanics but does not make ordinary movement random;
- magical wildlife is not automatically hostile;
- every tree does not glow;
- every attack does not use the same purple/blue particle cloud;
- the final dungeon boss is not locked until an external animated model and readable magic-combat silhouette pass intake.

---

# 2. External game-content precedents

These are structural and art-direction references only. No proprietary maps, story scripts, models or exact mechanics are copied.

## 2.1 Kingdoms of Amalur — Faelands / Fae environment design

Useful lessons:

- a strongly stylized magical forest can still feel like one coherent world rather than a collage of fantasy props;
- the Fae architecture visually grows out of organic forms, flowers, roots, curves and stained-glass-like surfaces instead of looking like ordinary human buildings with glowing vines pasted on;
- the broader forest region varies internally between magical, dark, open and spider-webbed sub-areas rather than using one visual note everywhere;
- environment designers explicitly considered combat, narrative, RPG resources and performance alongside beauty;
- environmental storytelling gives ruins, buildings and decorative motifs actual history instead of treating them as arbitrary scenery.

Project adoption:

- R08 sanctuary and archive architecture must look **native to the region's magical ecology**;
- one coherent external visual family governs arches, glass/root motifs, luminous plants and scholar spaces;
- subareas vary between bright bloom fields, quiet scholar groves, shadowed archive approaches and magical cliff zones while remaining one region;
- pigments/pollen/flora/resource nodes are integrated into the same art language as quests and settlement economy;
- ruins carry visual evidence of what old regulation did to the region.

Explicitly not adopted:

- a generic high-fantasy art dump where every prop glows;
- treating beauty as sufficient gameplay;
- filling the region with arbitrary lore objects that do not affect exploration or systems.

References:

- `https://www.gamedeveloper.com/design/building-a-fantasy-world---the-art-direction-of-i-kingdoms-of-amalur-reckoning-i-`
- `https://www.ausgamers.com/features/read/3146278`
- Jason Johnson's published Fae environment art is a **visual reference only**, not a redistributable asset source.

## 2.2 Guild Wars 2 — Heart of Maguuma / open-world storytelling

ArenaNet's own design material is useful beyond the jungle theme because it explicitly combines terrain, gameplay, story, characters and map-wide features instead of designing them separately.

Useful lessons:

- gameplay, story and terrain can be conceived together around one regional theme;
- vertical/horizontal layers are valuable when each level contains distinct gameplay opportunities;
- an outpost/POI can contextualize a family of local events instead of scattering unrelated tasks;
- open-world actions become more memorable when the world and NPC behavior visibly reflect them;
- ambient dialogue and local activity can carry story without forcing the player into constant conversations.

Project adoption:

- R08's sanctuary and two field research sites act as **content anchors**, each with related ecology/events/resources rather than standalone icons;
- magical cliff routes are short and content-bearing, not empty vertical space;
- regional state changes alter NPC activity, event availability and visible flora at selected safe locations;
- settlement ambient dialogue reflects the Restoration experiments and ecological consequences;
- optional world events reinforce the region's main theme instead of being generic combat waves.

Not adopted:

- MMO-scale map-wide event chains covering every minute of play;
- mastery-grind gates for basic traversal;
- high player-count requirements for ordinary regional content.

References:

- `https://www.guildwars2.com/en/news/gameplay-features-making-the-most-of-maguuma/`
- `https://www.guildwars2.com/en/news/storytelling-in-guild-wars-2s-open-world/`
- `https://www.guildwars2.com/en-gb/news/journey-into-the-heart-of-maguuma-in-guild-wars-2-heart-of-thorns/`

## 2.3 Bright fantasy as contrast, not visual noise

R08 needs a deliberately brighter and more magical palette than grounded regions, but the quality target is **contrast and hierarchy**.

Adopt:

- bright bloom fields against darker root/stone anchors;
- limited bioluminescent accents at night;
- one or two signature magical colors per subarea;
- normal earth/wood/stone retained so magical objects remain special;
- readable attack telegraphs that do not disappear into ambient VFX.

Reject:

- permanent full-screen bloom;
- rainbow particle fog;
- every plant pulsing/emitting light;
- background effects that hide combat tells;
- magical UI overlays merely because the biome is magical.

---

# 3. External-first source stack

## 3.1 Current dependency ecology

### Alex's Mobs Continued

Current Fabric 26.2 release exists.

Potential R08 ecology roles from the preserved creature roster include:

- **Hummingbird** — ambient pollinator;
- **Flutter** — passive plant-like magical creature / bloom interaction;
- **Bunfungus** — whimsical non-hostile or neutral magical wildlife if current 26.2 behavior/model fits the region;
- **Sunbird** — rare sky spectacle only if use here does not dilute its R03/high-altitude identity.

Project rules:

- project spawn tables own regional density;
- ambient/pollinating creatures are not turned into XP fodder;
- Hummingbird/Flutter interactions should support the visual ecology before they support loot;
- Bunfungus is admitted only after current in-game model/behavior inspection confirms it improves R08 rather than making the region feel like a donor-mod showcase;
- source/asset copying remains separate from normal dependency use because continuation license metadata must be handled exactly.

Reference:

- `https://www.curseforge.com/minecraft/mc-mods/alexs-mobs-continued`

### Threateningly Mobs Continued / original lineage

Useful preserved identities:

- **Nature Spirit** — high-tier forest identity, but R08 variants must be authored and visually distinct rather than recolored R01 copies;
- **Moonpriest** — original lineage added the high-tier caster with curse and explosive-moon attacks; it is suitable for authored ruin/library pressure if current 26.2 model/animation review passes;
- **Titan Rabbit** — original/current lineage treats it as an Ultra-tier **summoned altar boss**, not a normal spawn;
- **Knowledge Fairy** — original wiki identity is passive, bookshelf/library-linked knowledge energy; this fits authored archive ecology but not field combat spam.

Project use:

- Moonpriest appears in authored archive/ritual spaces rather than normal meadow density;
- Titan Rabbit remains an optional ritual-triggered boss and is never a random natural trash spawn;
- Knowledge Fairy remains non-hostile and library-specific;
- donor stats, loot, altar economy and global spawn rules are replaced by project-authored equivalents.

References:

- `https://modrinth.com/mod/threateningly-mobs/version/1.0.9`
- `https://www.curseforge.com/minecraft/mc-mods/threateninglly-mobs-continued`

## 3.2 Redistributable environment/monster source pools

Strong public-safe candidate families:

- **Quaternius Ultimate Monsters** — legacy pack page still explicitly identifies 50 animated monsters and CC0; exact artifact/license evidence must be preserved per package;
- **Quaternius Ultimate Nature** legacy package — 150-model CC0 nature family where exact old-package provenance is verified;
- **Kenney Nature Kit** — 330 CC0 nature props;
- **Kenney Fantasy Town Kit** — 160 CC0 modular fantasy-town pieces for neutral structural primitives;
- accepted KayKit/Kenney CC0 props for jars, scholar workspaces, books, signs and gathering objects.

Important Quaternius rule:

- Quaternius central license changed to QAL v1.0 on 2026-08-28;
- therefore never write `all Quaternius = CC0`;
- only exact legacy/package pages or creator-uploaded artifacts that explicitly preserve CC0 may be committed under CC0;
- newer/current downloads without exact legacy evidence follow QAL and its redistribution boundary.

References:

- `https://quaternius.com/packs/ultimatemonsters.html`
- `https://kenney.nl/assets/nature-kit`
- `https://kenney.nl/assets/fantasy-town-kit`
- `https://quaternius.com/license.html`

## 3.3 Magical flora / pigment intake

The region needs actual external visual candidates for:

- giant bloom landmark;
- pollinating flower clusters;
- mana-active flora;
- pigment flowers/fungi;
- luminous root/glass accents;
- pollen-gathering workstation props.

Do not lock six invented flower names first and then search for models later.

The final catalog is produced by asset intake:

```text
accepted external model
→ readable silhouette/color
→ ecological location
→ crafting/alchemy/spellcraft role
→ final player-facing name
```

---

# 4. Story role — valuable emergence after regulation

R08 is one of the R08/R09 high-tier main-story routes in Act III.

Its evidence package argues:

> Some local magical systems emerged only after the old network stopped suppressing them. Restoring historical stability can erase useful modern ecology and culture even if the restoration works exactly as intended.

Regional history:

- the old Anchor network damped large fluctuations in ambient magical pressure around the cliffs/forest;
- this made ancient transport/observation safer but reduced spontaneous magical growth and pollination cycles;
- after the regional regulator weakened generations ago, local flora/fauna gradually adapted to higher variable magic;
- scholars, healers, pigment workers and local crafts grew around that ecology;
- this is not a recent mutation disaster; it is now a stable living regional system with occasional dangerous surges;
- modern Restoration tests reconnecting a nearby branch successfully reduce some anomalous spikes;
- however the same tests visibly reduce bloom cycles, suppress selected magical pollinators and destabilize Knowledge Fairy/archive phenomena that depend on local variation;
- the region therefore proves that `stable according to the old specification` is not equivalent to `best for the present world`.

Regional resolution:

- stop the immediate over-stabilization cascade;
- retain a **bounded local safety regulator** for dangerous spikes around settlement/archive infrastructure;
- reject the old constant suppression target;
- preserve evidence showing the Restoration system performed technically correctly while producing unacceptable ecological/cultural loss.

This is stronger evidence than a simple broken machine.

---

# 5. Local people / culture

Primary hub: **secluded sanctuary / scholar enclave** built around a stable cliff shelf and large bloom/root landmark.

Normal life should visibly include:

- herb/pollen sorting;
- pigment grinding and dye work;
- spellcraft research using ordinary props as well as magical instruments;
- healers/support practitioners studying flora;
- gardeners/pollination wardens;
- book/archive handling;
- glass/root architecture maintenance;
- travelers or apprentices arriving for study;
- market/service areas small enough that the enclave still feels secluded.

Key local roles:

## 5.1 Bloom Warden / Ecologist

- tracks pollination and magical-flora cycles;
- knows the region became more biologically/magically diverse after old regulation faded;
- does not oppose safety measures; opposes forcing living cycles back to an obsolete baseline;
- grounds the autonomy argument in observation rather than mysticism.

## 5.2 Archivist / Lens Keeper

- maintains old and modern records;
- understands the archive responds to magical pressure;
- initially values the Restoration test because record corruption/anomalies are genuinely dangerous;
- becomes the clearest witness that perfect stabilization also kills some useful archive phenomena.

## 5.3 Pigmenter / Spellcraft Artisan

- turns pollen/pigment/light materials into real equipment/appearance/spellcraft economy;
- gives magical resources practical value outside exposition;
- sells/commissions selected cosmetics, dyes, catalyst/staff parts or support accessories without adding another profession tree.

Recurring Anchor Scholar, Cartographer/Ranger, Restoration Director representatives or Rival Wanderer may appear depending on main-story routing.

---

# 6. Spatial progression / local pressure

Suggested entry Lv remains **44**.

| Sub-area | Local pressure | Role |
|---|---:|---|
| outer flowerwood / approach cliffs | Lv 43–44 | visual contrast / first magical ecology |
| sanctuary / scholar shelf | Lv 44 | hub / services / story context |
| bloom terraces / pollen paths | Lv 44–46 | gathering / ambient ecology / events |
| luminous root belt / shallow caves | Lv 45–47 | magical threats / discoveries |
| pollinating cliffs / glass-root walks | Lv 46–48 | traversal / high-value nodes / main evidence |
| ritual meadow / Titan Rabbit site | **Lv 49** | optional ritual field/world boss |
| fae archive / observatory dungeon | Lv 47–50 | high-tier dungeon |
| final guardian/caster target | **Lv 50** | dungeon climax |

R09 is a peer Lv44 route and can be visited first or skipped until later.

---

# 7. Traversal language — magical without movement chaos

R08 uses **authored magical route elements**, not a new universal movement system.

Ordinary travel still uses:

- ground trails;
- bridges/root arches;
- short stairs/ramps;
- Trail Stag / Komodo where collision permits;
- discovered shrine fast travel.

Magical route elements may include:

### Bloom bridges

- large flowers/roots open a short connection after local event/state resolution;
- visible physical path appears;
- not random disappearing platforms.

### Pollen updrafts / lift currents

- only at selected cliff POIs;
- short server-authored lift/glide-like movement if implementation can provide a clean animation/trajectory;
- otherwise replace with root ramps/lifts rather than shipping bad floating motion;
- no permanent glider mastery tree.

### Glass-root walkways

- ancient/modern constructed paths around observatory areas;
- strong silhouette and transparent/luminous accents only where readability remains good;
- collision always matches visible geometry.

Rules:

- no route requires guessing which decorative flower is a platform;
- no long chain of precision jumps during normal progression;
- failure returns player to nearby safe ground rather than a two-minute climb;
- magic shortcuts become permanent/readable where world-state appropriate.

---

# 8. Navigation / landmark language

R08 can become visually noisy, so orientation must be stronger than the effects.

Use at least:

1. **one colossal flower/tree/bloom crown** visible across the central region;
2. **sanctuary lens/spire** with distinct silhouette;
3. **glass-root observatory/archive crown**;
4. **pollinating cliff wall** with color banding visible at distance;
5. **one dark root/grotto landmark** for contrast;
6. a distant R09 rocky horizon or other grounded landmass when geography permits.

Rules:

- local VFX never erase macro silhouettes;
- fog/glow is spatially bounded;
- night bioluminescence reinforces routes instead of changing the whole map into equal-brightness neon;
- the player should distinguish settlement, dungeon and field-boss directions from scenery alone after familiarity.

---

# 9. POI package

## Major POI A — Bloomveil Sanctuary

Functions:

- social/service hub;
- architecture/culture statement;
- spellcraft/pigment/herbal economy;
- main-story tension made visible through local experiments.

## Major POI B — Pollinator Terraces

Functions:

- Hummingbird/Flutter/Bunfungus ecology;
- pollen/pigment gathering;
- one event family around disturbed bloom cycles;
- visual evidence that Restoration test zones lose some activity.

## Major POI C — Prismroot Cliffs

Functions:

- high-view navigation;
- short authored magical lift/bridge traversal;
- rare resource pockets;
- first strong visual evidence of changed magical pressure.

## Major POI D — Quiet Library Grove

Functions:

- Knowledge Fairy / books / scholars;
- discovery and lore without combat requirement;
- archive phenomena visibly change with regional state;
- source of one optional spell/Hidden Technique clue where class canon permits.

## Major POI E — Titan Ritual Meadow

Functions:

- optional boss ritual site;
- discovered invocation, not random field spawn;
- large open arena with distinct floral/stone ritual geometry;
- trophy/signature reward source.

## Major complex — Fae Archive / Glass-Root Observatory

Handled in §17.

Smaller discoveries may include:

- dormant pollination shrine;
- pigment cave;
- Flutter nursery;
- shadow-butterfly grove;
- abandoned old regulator marker;
- luminous spring;
- scholar camp;
- rare flower/fish pool where ecology fits.

No `glowing chest behind every flower` checklist.

---

# 10. Settlement / services / housing

Target daytime physical population:

```text
8–12 functional scholars/artisans/healers/wardens
5–8 ambient residents/apprentices/travelers
```

Baseline services:

- shrine / fast travel;
- inn/rest/food equivalent appropriate to sanctuary culture;
- Material Vault/bank;
- strong alchemy/healing service;
- magical accessory/catalyst merchant;
- pigment/appearance artisan;
- regional contract/discovery board or archive desk;
- normal general merchant essentials;
- limited smith access rather than copying every prior hub.

No new `magic reputation currency`.

## Housing

If actual map/architecture supports it, R08 may introduce:

- 1–2 high-quality **Large House-class** sanctuary homes near the ~25,000 Gold band;
- decorative magical furnishing set / display pieces;
- no stat buffs from magical furniture;
- no requirement to buy housing to access spellcraft services.

Prestige 65,000+ housing remains optional later and does not need to appear just because R08 is magical.

---

# 11. Gathering / production package

R08 resources must connect existing systems rather than become a pile of fantasy nouns.

## Magical Pollen role

Purpose:

- alchemy/support consumables;
- pigment/dye/cosmetic crafting;
- selected magical accessory/catalyst recipes.

Gathering:

```text
yield: 1–2
personal respawn: ~7 active min
tool: Harvest Knife/Sickle
source: authored pollination clusters / event-safe zones
```

Final model/name may remain `Magical Pollen` only if the accepted presentation reads clearly; otherwise rename after asset intake.

## Pigment flower role

Use a small number of ingredient identities, not every color as its own material.

Baseline target:

- one common pigment plant;
- one rare high-tier luminous pigment/material if external model supports it.

Uses:

- Wardrobe/appearance unlocks or dyes where technically supported;
- furnishing;
- selected spellcraft/alchemy.

## Mana-active flora role

R02's earlier mana-flora role can evolve here rather than creating a completely unrelated second magic herb system.

R08 contains denser/rarer high-tier variants used in:

- Focus/support recipes;
- catalysts/staff accessories;
- magical equipment crafting.

## Light/glass-root material role

One high-tier structural/magical material may exist for observatory gear if exact external visual source supports it.

Do not lock a generic `Fairy Crystal` without a real visual candidate.

---

# 12. Fishing / collection

R08 fishing is optional but can support visually magical species where actual animated models exist.

Use:

- clear forest pools;
- luminous spring;
- cliff-fed stream;
- no floating-space fish unless a strong external model and ecology justify it.

Fish Codex target:

```text
3–5 regional/shared fish identities
```

At most one or two should be overtly magical/trophy-like. The rest can be ordinary ecological continuity so the world does not reset into fantasy species at every border.

No equipment drops from fishing.

---

# 13. Ecology / encounter roles

R08 is magical but not permanently hostile.

## Hummingbird

- ambient pollinator;
- visual route/ecology cue around active blooms;
- not a combat target.

## Flutter

- passive plant-like magical creature;
- can cluster around healthy bloom zones;
- actual current behavior may inspire bounded pollination interactions;
- no pet-management grind added by default.

## Bunfungus

- candidate neutral/whimsical ecology;
- use only if current model/animation style fits R08 after direct review;
- avoid high density so the region does not become comedic.

## Knowledge Fairy

- passive archive/library phenomenon;
- no normal field spawn;
- no farming bookshelves for energy currency;
- used to communicate that magical knowledge-storage phenomena depend on local pressure variation.

## Nature Spirit variants

R08 may use **authored visual/mechanical variants only if actual external model/skin/animation differences exist**.

Do not recolor the R01 Nature Spirit purple and call it high-tier.

If no convincing variant source exists, use a different accepted magical guardian model.

## Moonpriest

High-tier authored caster around specific ritual/archive spaces.

Original identity supports:

- curse skill;
- explosive moon projectile.

Project rules:

- no random broad-world spawn;
- attack telegraphs must be external-animation/VFX-readable;
- curses use project status rules, not unbounded donor debuffs;
- groups are small and authored because several explosive casters can become unreadable.

---

# 14. Titan Rabbit ritual field boss

The source identity explicitly treats Titan Rabbit as an Ultra-tier **summoned altar boss**.

R08 preserves that structure.

Working role:

```text
Lv: 49
role: optional ritual field/world boss
HP target: ~25,500–29,500
Defense: ~120
MR: ~82
Poise: 255
solo active TTK target: ~210–245 s
```

This HP band is aligned to the canonical `BenchmarkDPS(L) × target active TTK` authoring rule. Final values may still move after current 26.2 dependency inspection and real model uptime review, but do not restore the older 33,000–38,000 band merely to make the boss feel larger.

## Invocation

The player discovers the ritual meadow and learns the local invocation through exploration/records.

Project rule:

- invocation uses a bounded authored regional requirement/reusable encounter state;
- it does **not** inherit a new grindy UltraSummonStone economy;
- first invocation cannot consume a progression-critical rare item irreversibly if the boss immediately despawns/crashes;
- encounter reset is server-authoritative and repeatable on a sensible cooldown/trigger.

Possible project requirement:

```text
one discovered ritual condition
+ modest regional material offering
+ no random ultra-rare key drop
```

Exact material waits for asset intake.

## Arena

- broad flower/stone meadow with stable floor;
- scenery low enough not to hide foot/claw tells;
- authored breakable props only;
- no world grief despite donor stomp/leap block-breaking identity.

## Source-derived attack identity

The original Titan Rabbit wiki documents:

- claw swipe;
- stomp;
- leap/crash.

Project adaptation keeps those readable physical identities rather than making the rabbit a generic magic laser boss.

### Claw Sweep

```text
wind-up: ~0.45 s
wide frontal pressure
guardable/perfect_guardable: true
benchmark damage: ~12–14%
```

### Titan Stomp

```text
telegraph: >=0.90 s
radius: model/arena tuned, target ~4.5–5.5 blocks
guardable: false
perfect_guardable: false
benchmark damage: ~28–32%
recovery: >=0.9 s
```

Only tagged arena props may break.

### Great Leap

```text
initial tell: >=1.10 s
visible airborne arc / landing target
benchmark damage: ~35–40%
recovery/punish: 1.2–1.6 s
```

No teleporting landing.

### High-pressure sequencing

Below ~45% HP:

- stomp → short reposition → leap or swipe sequences may become more varied;
- every major attack retains its own readable cue;
- no permanent hidden speed/damage steroid;
- melee punish windows remain.

## Rewards

First eligible defeat:

- guaranteed Superior+ R08 equipment;
- 2 model-linked signature materials after exact source intake;
- 15% direct Mythic/signature roll;
- EXP ~20% current next-Lv requirement;
- Class XP ~15%;
- trophy furnishing unlock if accepted model supports one.

No `Titan Rabbit Token`.

---

# 15. Dynamic event package

## Pollination Collapse

- one bloom terrace loses pollinator activity during Restoration test pulses;
- player may stabilize local flora, clear hostile magical pressure or operate a local regulator;
- reward = useful pollen/pigment/alchemy material, not a new event currency.

## Archive Echo

- Knowledge Fairy / record projections reveal a short historical scene or route clue;
- may involve light puzzle/interaction and one authored magical threat;
- not a lore-text wall.

## Bloom Surge

- local magical spike causes plants/routes to overgrow or open temporary paths;
- creates optional rare gathering / exploration opportunity;
- does not block the main road.

## Moonpriest Incursion

- small authored high-tier caster event around one ruin/observation site;
- support/cleanse/positioning matter;
- no endless wave spam.

---

# 16. Main-story / regional quest flow

## `A Forest Too Quiet` role

- arrive during a successful-looking Restoration stabilization test;
- sanctuary routes are safer, but a pollination terrace is unnaturally inactive;
- establishes that the problem is not a machine exploding.

## `What the Bloom Remembers` role

- investigate two or three ecology/archive sites in flexible order;
- compare modern bloom records, Knowledge Fairy behavior and old regulator targets;
- proves the historical baseline intentionally suppressed variation.

## `Safe According to Whom` role

- local scholars and Restoration technicians both have valid evidence;
- one dangerous magical surge must actually be contained, proving total laissez-faire is not automatically safe;
- player helps build/set a bounded local threshold rather than choosing ideology through dialogue only.

## `The Glass-Root Archive` role

- dungeon / Act-III high-tier evidence;
- reveals direct historical documentation of suppression targets and why central operators preferred them;
- current over-stabilization is stopped.

## Titan Rabbit ritual discovery

- optional;
- can be found through old ritual symbols / animal signs / local stories;
- never required for the Anchor evidence package.

---

# 17. Regional dungeon — Fae Archive / Glass-Root Observatory

Target first-clear wall-clock:

```text
~25–35 minutes
```

The dungeon is not a generic stone library with vines.

## Stage 1 — Living approach

- exterior roots/flowers visibly change around the stabilization field;
- one route is safer but ecologically dormant; another is wilder but richer;
- both reconnect, demonstrating theme through navigation.

## Stage 2 — Glass-root galleries

- organic architecture + transparent/luminous surfaces;
- readable book/archive/workstation spaces;
- small Moonpriest or guardian encounters use long sightlines and cover.

## Stage 3 — Pollination lens / regulator chamber

- old instruments show pressure suppression targets;
- one light spatial interaction lets the player compare old stable output with modern variable output;
- not a color-match puzzle repeated five times.

## Stage 4 — Memory archive

- Knowledge Fairy/record phenomena reveal what disappeared under historical regulation and what dangers were prevented;
- visual/environmental evidence carries most of the information;
- Act-III evidence is recorded here.

## Stage 5 — Final guardian / caster chamber

- broad arena with limited magical environmental states;
- final external boss must visibly fit archive/regulator logic;
- no recycled Nature Spirit or giant vanilla Evoker.

## Repeat shortcut

After first clear:

- one root/glass bridge or archive lift opens near the entrance;
- story-comparison interactions are skipped on repeat;
- boss/reward route remains ~10–15 min target depending on encounter density.

---

# 18. Dungeon-boss asset/mechanical contract

Final identity remains open pending external intake.

Target:

```text
Lv: 50
role: dungeon boss
solo active TTK: ~175–215 s
Poise: ~210–250 depending on anatomy
```

Required encounter properties:

- high-tier magical pressure without projectile spam filling the screen;
- at least one mechanic that converts or redirects a visible magical field/zone rather than only dodging circles;
- one cleanse/support opportunity valuable to Cleric/Guardian without making those classes mandatory;
- one phase change visibly alters environmental state or attack geometry;
- melee uptime remains reasonable;
- final weak points/status relations derive from the accepted model, not invented invisible rules.

Possible source directions:

- visually strong Quaternius Ultimate Monsters caster/guardian model after direct inspection;
- a distinct current Threateningly Moonpriest-derived boss only if its model/animation can support boss-level readability without cloning the normal Moonpriest encounter;
- another legally redistributable magical guardian with better visual fit.

Stop and revise exact attack design after model intake; do not ship a placeholder boss.

---

# 19. Regional outcome / memory

After R08 resolution:

Shared late-join-safe consequences may include:

- selected stabilization pylons/lenses visibly retuned rather than destroyed;
- one bloom terrace becomes active again;
- pollinator ambience returns to a specific safe area;
- sanctuary artisans expand pigment/spellcraft stock;
- one glass-root shortcut remains open;
- ambient dialogue changes from `the experiment is working` to debate about what `working` means.

Personal consequences:

- R08 high-tier evidence package;
- local NPC/ideology dialogue flags;
- Titan Rabbit ritual/hunt state;
- archive discoveries;
- Wardrobe/pigment/codex progress;
- first-clear/reward state.

The region should not visually revert to a pre-story state where all effects vanish.

---

# 20. Reward identity

Regional equipment/affix emphasis:

- Mana sustain / resource conversion;
- support/healing amplification with bounded caps;
- status conversion/cleanse interactions;
- spell-shape/radius/duration utility where skill framework supports it cleanly;
- WIL/INT accessories;
- catalyst/staff/support off-hand options;
- movement/cast utility that does not erase commitment timing;
- selected pigments/appearance unlocks.

Rules:

- R08 must still drop useful Warrior/Hunter/Guardian equipment; magical region does not mean three classes get no rewards;
- avoid percentage stacking that breaks `COMBAT_BALANCE.md`;
- no `Fairy Dust` currency just because the region is magical.

---

# 21. Audio / presentation

R08 must sound magical without becoming constant high-frequency sparkle noise.

Outdoor baseline:

- ordinary forest wind/birds/water retained;
- Hummingbird/Flutter/pollinator layers near active terraces;
- soft glass/root resonance only near magical structures;
- strong quiet zones around over-stabilized areas so ecological absence is audible;
- bloom-surge states add layered harmonic ambience sparingly.

Dungeon:

- library/observatory has dry book/footstep/wood/glass detail;
- magical resonance increases near active regulator chambers;
- record/memory phenomena use distinct spatial audio but never mask combat telegraphs.

Titan Rabbit:

- footsteps/stomp/landing need strong low-frequency physical weight;
- do not make the joke-like silhouette produce cartoon sound effects unless the accepted source style genuinely supports it.

All audio remains external-first/provenance-tracked.

---

# 22. Multiplayer / authority

Server owns:

- regional stabilization world states;
- quest/evidence state;
- ritual invocation/boss encounter state;
- Titan Rabbit hit/landing/breakable validation;
- dungeon field state;
- personal loot/reward claims;
- gathering/fishing state;
- event contribution.

Required multiplayer tests later:

- personal evidence remains independent while shared stabilization visuals are consistent;
- late join sees retuned sanctuary state but can still complete personal investigation;
- Titan Rabbit ritual cannot consume offerings twice or duplicate rewards;
- support roles qualify in Moonpriest/Titan/events;
- dungeon magical-field mechanics remain synchronized across clients;
- disconnect during field transition returns to one valid deterministic state;
- no reward reroll/duplication on reconnect.

`MULTIPLAYER TESTED` remains NO until real-client verification.

---

# 23. Performance constraints

- no every-tick scan of all magical flora;
- pollinator ambience uses bounded spawn density;
- magical bloom/lighting uses static/block/model composition where possible, not hundreds of persistent particles;
- local VFX controllers run only in loaded/relevant POIs;
- archive memory effects stop when unloaded;
- Titan Rabbit heavy AI/physics logic only runs during active encounter;
- transparent/glowing surfaces require actual client profiling to avoid overdraw problems;
- no huge number of dynamic lights if the chosen rendering stack makes them expensive.

---

# 24. Asset-intake blockers

R08 is not asset-ready until:

1. current 26.2 Hummingbird/Flutter/Bunfungus behavior/model review;
2. current Moonpriest/Titan Rabbit model, animations and hitbox review;
3. exact Knowledge Fairy current dependency presentation review;
4. exact magical-flora/pigment/pollen models and source/license/hash;
5. coherent sanctuary / glass-root archive architecture family;
6. final dungeon-boss external model/animation source;
7. exact R08 equipment/catalyst/accessory visual families;
8. magical regulator/Anchor machinery visual language;
9. bloom/pollen/status VFX sources;
10. R08 ambience/music/UI icon sources;
11. pigment/Wardrobe integration visuals;
12. any magical fish model assignments.

No vanilla Allay, Evoker, Iron Golem or particle cloud is accepted as the finished replacement for these gates.

---

# 25. R08 quality acceptance

Real play must prove:

- R08 reads instantly as more magical than R01/R02/R05 without becoming visual noise;
- the region can be navigated from macro landmarks despite bright VFX;
- magical traversal elements are readable and short rather than precision-platforming chores;
- ordinary ecology and settlement life remain believable beneath the magic;
- the player visibly understands what Restoration stabilizes **and what it suppresses**;
- the story is learned through ecology, archive behavior and changed world state, not a lecture;
- Titan Rabbit feels intentionally bizarre and mechanically serious rather than a random meme boss;
- Moonpriest/magical encounters remain readable in multiplayer;
- magical resources feed existing systems instead of another currency/profession tree;
- dungeon repeat flow removes first-clear exposition friction;
- post-clear ecological/settlement changes are noticeable;
- audio/VFX preserve combat readability;
- performance survives magical props/effects at target multiplayer density.

Verification state:

```text
DESIGN REVIEWED: YES
EXTERNAL REFERENCE REVIEWED: YES
EXACT ASSET INTAKE: PARTIAL / REQUIRED
CODE REVIEWED: N/A
TESTED: NO
BUILD VERIFIED: NO
JAR PRODUCED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

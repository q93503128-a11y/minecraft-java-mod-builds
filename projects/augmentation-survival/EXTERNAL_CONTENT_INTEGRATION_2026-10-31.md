# Augmentation Survival — External Content Integration Audit

Date: 2026-10-31

Status: **CORE ACQUISITION INTEGRATION IMPLEMENTED / EXTERNAL CONTENT INVENTORIED / LIVE BALANCE PLAYTEST PENDING**

This pass audits the exact pinned external pack and closes the largest reward-loop gaps without deleting third-party content or inventing a second progression system on top of it.

## 1. Product rule

Augmentation Survival remains the primary progression loop.

External mods are kept when they add one or more of the following:

- places worth exploring;
- enemies worth fighting;
- bosses worth preparing for;
- useful survival/production options;
- equipment that creates a distinct playstyle rather than a mandatory parallel grind.

External content does **not** automatically receive its own augmentation-only currency, menu, or upgrade tree. Existing third-party rewards are preserved. Augmentation cores are added as a cross-mod reward layer so that playing the whole pack still advances the central augmentation system.

No external content mod is removed in this pass.

## 2. Core acquisition — before this pass

### Ordinary and special hostile mobs

Already functional:

- vanilla ordinary hostile fallback: `normal_hostile` — 0.2% core roll;
- selected vanilla/Friends&Foes elites: `special_hostile` or `elite_hostile`;
- Illager Invasion hostile namespace: `special_hostile` — 1.5%;
- Naturalist wildlife: deliberately excluded.

Gap:

- Chrono Dawn and World Bosses progression-zone hostiles mostly fell through to the ordinary 0.2% fallback;
- MCEndgame dungeon enemies frequently use vanilla entity types plus runtime flags, so namespace matching could not tell an ordinary zombie from an MCE dungeon enemy, elite, loot goblin, or dungeon boss.

### Structure chests

Already functional through natural generated-structure resolution:

- vanilla structures;
- Repurposed Structures;
- Dungeons and Taverns, including its verified `nova_structures` data namespace;
- Illager Invasion namespace fallback.

Gap:

- Chrono Dawn's authored progression structures were not mapped;
- World Bosses realm villages/caches and MCEndgame procedural dungeon rewards were not safe candidates for a blanket structure-namespace rule because their reward density/semantics differ substantially.

### Bosses

Major gap found:

- `boss_core_reward_contract.json` documented Chrono Dawn, World Bosses, and MCEndgame as intended boss sources;
- the actual `BossCoreRewardService` only used exact vanilla assignments, so only Wither and Ender Dragon received the direct authored boss-core path;
- recognized external bosses could therefore be treated as bosses by threat/control systems while receiving no guaranteed boss core.

This mismatch is closed by this pass.

## 3. Core acquisition — new canonical behavior

### Hostile profiles

| Source | Core behavior |
|---|---|
| Ordinary hostile fallback | 0.2%; Common 90% / Uncommon 10% |
| Chrono Dawn hostile | `progression_hostile`: 0.8%; Common 75% / Uncommon 22% / Rare 3% |
| World Bosses hostile | `progression_hostile`: 0.8%; Common 75% / Uncommon 22% / Rare 3% |
| Illager Invasion hostile | `special_hostile`: 1.5%; Common 65% / Uncommon 30% / Rare 5% |
| MCEndgame dungeon enemy | runtime flag -> `special_hostile`: 1.5% |
| MCEndgame elite | runtime flag -> `elite_hostile`: 4%; Uncommon 65% / Rare 30% / Epic 5% |
| MCEndgame loot goblin | runtime flag -> `elite_hostile`: 4% |
| Recognized external boss | generic hostile core roll suppressed; direct boss reward only |

MCEndgame runtime classification is read through its public mixin accessor interfaces rather than guessed from the underlying vanilla entity ID:

- `LivingEntityDungeonEnemyAccessor#mcendgame$isDungeonEnemy`
- `LivingEntityEliteAccessor#mcendgame$isElite`
- `LivingEntityLootGoblinAccessor#mcendgame$isLootGoblin`
- boss identity continues through `MobEntityDungeonBossAccessor#mcendgame$isDungeonBoss`

Runtime precedence is boss/event redirect -> dynamic state -> exact/static source -> namespace -> global fallback. Therefore an MCE elite zombie is an elite source, not an ordinary zombie source.

### Chrono Dawn structure chests

The eight managed structures were verified from Chrono Dawn's authored `ManagedStructure` source and are mapped explicitly:

| Structure | Profile | Chance after density multiplier |
|---|---:|---:|
| Ancient Ruins | routine_exploration ×1.00 | 8.0% |
| Forgotten Library | dangerous_mid ×0.75 | 9.0% |
| Desert Clock Tower | dangerous_high ×0.55 | 9.9% |
| Guardian Vault | dangerous_high ×0.55 | 9.9% |
| Clockwork Depths | dangerous_high ×0.55 | 9.9% |
| Phantom Catacombs | dangerous_high ×0.55 | 9.9% |
| Entropy Crypt | dangerous_high ×0.55 | 9.9% |
| Master Clock | dangerous_high ×0.40 | 7.2% |

The multiplier is applied to the chance of one additional core, not to item count.

Boss structures deliberately do not use `pinnacle_cache`. Their boss already gives guaranteed personal cores, so the chests are a supplementary exploration reward rather than a second mythic farm.

Chrono Dawn microstructures and renewable/refillable Watchmaker loot are not blanket-mapped. Renewable containers must not become renewable augmentation-core printers.

### External bosses

The following shared compatibility profiles now have an explicit `boss_major` reward assignment:

- Chrono Dawn: all six verified bosses;
- World Bosses 2.0.2: all eleven runtime-resolved boss profiles;
- MCEndgame: dynamically flagged dungeon bosses.

`boss_major` behavior:

- every eligible player receives **1 guaranteed personal core**;
- Epic 80% / Legendary 20%;
- first authored-boss-profile kill for that player grants **1 additional core**;
- original boss loot remains untouched;
- general loot multipliers do not duplicate boss cores;
- the same boss does not also roll the ordinary hostile core table.

For Chrono Dawn and World Bosses, each authored boss profile has its own first-kill key. MCEndgame exposes one generic dungeon-boss compatibility profile, so the first-kill bonus is intentionally paid once for that generic boss profile rather than once per random underlying vanilla mob type.

Time Tyrant, Shadow Weaver, and Beastweaver remain pinnacle candidates, but they are **not** promoted to direct mythic reward weights merely because they are described as late/final bosses. Promotion requires measured difficulty, repeatability, and time-to-clear data from the pinned pack.

## 4. External content inventory and decision

This inventory focuses on progression-relevant content. Decorative block variants are not treated as separate progression systems.

### Terralith 2.6.4 — KEEP

Role:

- large overworld terrain/biome expansion.

Integration:

- no direct core source from the Terralith namespace itself;
- structures, enemies, and events inside the world remain the reward sources.

Reason:

- adds exploration variety without competing equipment/progression currency.

### Repurposed Structures 7.7.6 — KEEP

Role:

- large family of vanilla-like structure variants.

Integration:

- natural structure chests already map into routine/mid/high distributions using semantic structure families and density correction;
- villages remain excluded from easy storage farming.

### Dungeons and Taverns 5.3.2 — KEEP

Role:

- dense exploration structures, dungeons, taverns, forts, ruins.

Integration:

- structure chests already map through the verified `nova_structures` namespace;
- dense structures use below-1.0 chance multipliers so chest count does not multiply core income out of control.

### Friends&Foes 4.0.27 — KEEP

Role:

- vanilla-style creatures and combat encounters.

Integration:

- verified stronger hostiles such as Iceologer, Illusioner, Mauler, and Wildfire receive authored special/elite profiles;
- other genuinely hostile Friends&Foes mobs can use the normal hostile fallback;
- peaceful mobs do not become core farms.

### Illager Invasion 26.2.0 — KEEP_AND_INTEGRATE

Progression-relevant content includes:

- new Illagers and raid pressure;
- Illager Fort, Illusioner Tower, Labyrinth, Firecaller Hut, Sorcerer Hut;
- Hallowed Gem;
- Imbuing Table for beyond-normal enchantment upgrades;
- Platinum Infused Hatchet;
- Lost Candle ore-finding utility;
- Horn of Sight hostile-finding utility;
- special dust/material drops.

Core integration:

- hostile namespace already receives the 1.5% special-hostile profile;
- structure namespace already receives dangerous-mid structure rewards.

Decision:

- keep. Its gear/utilities are distinct enough to add exploration and combat options instead of replacing the augmentation loop.

### Naturalist 2.0.6 — KEEP

Role:

- wildlife and ecosystem density.

Integration:

- ordinary animals intentionally do not drop cores.

Reason:

- slaughtering passive wildlife must not become an optimal progression route.

### Farmer's Delight Refabricated 26.2-3.6.22 — KEEP

Role:

- crops, cooking, food, kitchen/production content.

Integration:

- no direct core printing from normal production;
- it provides targets for food/harvest/production-oriented augments.

### Better Combat 3.2.2 — KEEP, LIVE INTERACTION GATE REMAINS

Role:

- melee animation, attack timing, combat feel.

Integration:

- not a reward source;
- attack-boundary compatibility is handled by Augmentation Survival.

Remaining gate:

- human playtest of hit timing, reach, multihit, Echo Strike, Lightning and other contact augments.

### LambDynamicLights 4.12.4 — KEEP

Role:

- client-side visual lighting.

Integration:

- no reward/progression authority;
- does not alter actual server light or spawn rules.

### Chrono Dawn 0.12.0 — KEEP_AND_INTEGRATE

Verified progression content includes:

**Resources / ores**

- Chronite — overworld reagent ore;
- Clockstone;
- Enhanced Clockstone;
- Time Crystal;
- Entropy Crystal;
- Temporal Amber / Raw Temporal Amber / Temporal Amber Dust.

**Combat / utility equipment**

- Clockstone and Enhanced Clockstone equipment families;
- Time Clock;
- Time Arrow;
- Entropy Crystal Sword / shield path;
- Spatially Linked Pickaxe;
- Temporal Amber armor.

**Boss/progression materials**

- Guardian Stone;
- Colossus Gear;
- Phantom Essence;
- Entropy Core;
- Eye of Chronos;
- Fragments of Stasis Core;
- Ancient Gear / Clockwork Mechanism.

**Major artifacts / late rewards**

- Chrono Aegis and the Time Tyrant/ultimate-artifact progression family;
- other time-themed artifact/equipment rewards remain intact.

**World content**

- dedicated Chrono Dawn dimension;
- eight authored major structures;
- six boss encounters;
- dedicated hostile roster and traders.

Decision:

- keep the complete progression path;
- make playing it advance augmentation progression through its hostiles, chests, and bosses;
- do not replace its materials with augmentation cores;
- measure ultimate artifact power before any nerf or recipe rewrite.

### World Bosses 2.0.2 — KEEP_AND_INTEGRATE

Verified progression content includes:

- eleven major boss profiles in the pinned pack;
- shrines/rituals and raid-cache reward flows;
- Worldbreaker Dust and Worldbreaker portal progression;
- Primordial Spark / Worldbreaker Igniter progression;
- Worldbreaker Realm;
- Umbrite, Terranite, Caelestine -> Primordium Ingot;
- realm creatures and villages;
- Worldbreaker equipment progression;
- Shadowbreaker late equipment progression;
- boss/realm codices and trophies/encounter rewards.

Core integration:

- recognized bosses now give guaranteed personal boss-major cores;
- hostile realm creatures can use `progression_hostile` when server-classified hostile.

Deliberate non-integration:

- no blanket core injection into every World Bosses structure/village chest;
- no extra automatic core in raid caches in this pass.

Reason:

- the boss itself now pays the central progression reward, and 2.0.x already has rich participant/raid-cache rewards. Blindly rewarding every realm chest or cache would double-count encounters and make village storage a progression exploit.

### MCEndgame 1.7.0+26.2 — KEEP_WITH_BALANCE_GUARD

Progression-relevant systems include:

- repeatable procedural dungeons;
- dynamically generated dungeon enemies and bosses;
- elites;
- loot goblins with guaranteed equipment;
- Bandits with custom equipment and Synthesized Crystals;
- custom weapon/armor itemization and attributes;
- Unique equipment;
- Crystals and Crystal Forge equipment modification;
- Totem slots and dungeon-local power progression;
- Aspects that alter dungeon risk/reward, enemy strength, boss count and special-enemy rates.

Core integration now follows MCE's runtime state rather than the underlying entity ID:

- dungeon enemy -> 1.5% special-hostile core source;
- elite -> 4% elite-hostile source;
- loot goblin -> 4% elite-hostile source;
- dungeon boss -> guaranteed boss-major personal reward, with generic mob roll suppressed.

Decision:

- keep the dungeon system because it supplies a strong repeatable endgame loop;
- do not make Crystals, equipment dismantling, or ordinary crafting directly produce augmentation cores;
- do not promote repeatable dungeon bosses to mythic-heavy rewards;
- this mod has the highest progression-overlap risk and is the first candidate for measured gear normalization if live testing shows that its equipment makes augmentation choices secondary.

The pack remains pinned to 1.7.0+26.2 for this integration checkpoint. A newer version is not adopted during reward integration because the current exact pack has already passed compatibility verification and a version migration would confound the audit.

## 5. Why the new reward rates are structured this way

The intended hierarchy is:

1. ordinary mobs: background chance;
2. dedicated progression-zone mobs: noticeable but not farm-dominant chance;
3. special/elite dungeon enemies: meaningful repeated-combat reward;
4. natural structure chests: visible exploration reward;
5. authored bosses: guaranteed personal progression reward;
6. pinnacle/mythic weighting: reserved for measured top-end encounters.

This keeps the core loop connected to the whole pack without making the optimal strategy "farm the fastest external mob" or "reset one renewable chest".

## 6. Technical corrections in this pass

- added `progression_hostile` distribution;
- added exact Chrono Dawn structure mappings;
- added Chrono Dawn and World Bosses hostile namespace mappings;
- added dynamic MCEndgame dungeon-enemy/elite/loot-goblin adapters;
- authored explicit compatibility-profile boss reward assignments for Chrono Dawn, World Bosses, and MCEndgame;
- extended boss participant/reward routing beyond exact vanilla boss IDs;
- suppressed generic hostile core rolls for every recognized external boss;
- corrected dynamic boss compatibility caching so a `false` query before MCEndgame calls `setDungeonBoss()` cannot permanently hide later boss promotion;
- preserved all original third-party loot and progression items.

## 7. Remaining live balance gates

The following are intentionally **not** claimed complete by code/build verification:

1. Compare vanilla/augmentation gear against MCEndgame custom gear at several dungeon levels.
2. Compare Worldbreaker/Shadowbreaker sets with strong vanilla enchanted gear plus augmentation builds.
3. Compare Chrono Dawn late artifacts/armor/tools against equivalent progression timing.
4. Play at least one full Chrono Dawn boss path, multiple World Bosses encounters, and multiple MCE dungeons.
5. Measure core income per hour from:
   - ordinary overworld combat;
   - Chrono Dawn exploration;
   - Illager structures/raids;
   - Worldbreaker Realm;
   - MCE dungeons;
   - boss rushing.
6. Only after those measurements decide whether a reward rate, equipment stat, recipe, or repeatability rule needs adjustment.

A build pass proves reward wiring and compatibility contracts. It does not prove that the external equipment meta or core income feels balanced in a real play session.

## 8. Source / license boundary

- Chrono Dawn: public LGPL source used for exact structure/entity/progression verification.
- MCEndgame: public LGPL source used for dynamic accessor and dungeon-system verification.
- Illager Invasion: MIT/public source and project documentation available.
- World Bosses: All Rights Reserved; integration is performed from Augmentation Survival through public/runtime interfaces and exact pinned metadata. Its JAR is not modified or redistributed.

Third-party content remains externally fetched through the verified modpack dependency path.

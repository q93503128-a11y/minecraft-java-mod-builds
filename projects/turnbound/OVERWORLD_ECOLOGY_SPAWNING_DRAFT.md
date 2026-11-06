# TURNBOUND — Overworld Ecology and Encounter Spawn Architecture

Date: 2026-11-06
Branch: `turnbound-content-rebuild`
Status: DESIGN DRAFT; **no code, spawn config, entity roster or specific spawn/balance numbers implemented or approved**
Scope: Minecraft Java 26.2 / NeoForge 26.2.0.62
Project: TURNBOUND, not TURNBOUND: RE.

## 1. Authorized direction / player-experience contract

Explicit user decisions:
- Preserve normal, generated **Minecraft survival world** (mining, crafting, exploration and building), supplemented by authored RPG settlements/ruins/dungeons.
- **Replace every vanilla living creature** with TURNBOUND-authored fauna, hostile mobs and service NPC equivalents. Do not equate "living creature" with all Entities; players, items, projectiles, minecarts, boats, item/display entities and other nonliving world functions are not to be suppressed.
- Solo turn battles use **up to four collectible playable characters**, not the Minecraft avatar. At new-game start the player chooses **one of three ordinary native ★3 characters**. Native summon rarity ★1–★5.
- Standard hostile encounter style is **R_PG**: a visible enemy notices the player, displays **!**, reacts briefly, chases, and **contact triggers a turn battle in the same actual world terrain**. Defeat, disengage and return-to-world behavior need dedicated rules.
- User's principal UX concern: dense monster spawning would force a turn battle every time the player travels. **Avoiding repetitive, unavoidable fights is a hard design requirement**.
- No mandatory story; optional contracts/jobs, survival resources and rewards must interconnect.

The following categories and numbers are **proposals, not finalized approvals**. They must be reviewed in real gameplay and revised from the user's feedback.

## 2. World ecosystem: visible life does not mean constant combat

| Ecosystem role | Behavior and gameplay | Intended density | Battle entry |
| --- | --- | --- | --- |
| Peaceful wildlife / food animals | Wander, react to surroundings, serve survival food/husbandry/resource loops | Can be relatively frequent without clogging paths | Do not auto-aggro; whether player hunting requires turn combat or ordinary interaction is undecided |
| Neutral territorial wildlife | Warn or avoid; only some locations/conditions become dangerous | Infrequent near common travel routes | If hostile, use the same visible alert/chase/contact flow |
| Roaming hostile encounter representative | One visible world actor/group stands for a full tactical enemy composition | Sparse, readable, avoidable | ! → telegraphed chase → contact battle |
| Dungeon / ruin encounter | Designed pressure near visible landmarks and entrances; controlled pacing through interior | Medium-to-high *encounter* pressure, not an entity swarm | ! → chase/contact where feasible; authored terrain remains safe for battle camera |
| Elite / boss | Clearly marked danger, rare/territorial or authored landmark | Deliberate and exceptional | Visible warning; no random road boss spam |
| Settlement NPCs / service creatures | Shops, crafting/service/contract hooks, safe place identity | As needed for real gameplay function | Not hostile, no forced battle |

**Do not fill an empty world by using aggressive mob numbers.** Ambient life, landmarks, visual movement and useful resource interactions should provide density without constant hostile contacts.

## 3. Geography-driven threat, with noncombat corridors

| Zone | Proposed hostile pressure | World feel / rationale |
| --- | --- | --- |
| Starting area / player spawn | Very low; allow the player to choose the ★3 starter and learn survival controls | Readable, noninterruptive onboarding |
| Player builds, regular farm/base surroundings, civilian village | Very low; distinguish safe built-up regions from random wilderness | The player can manage survival and return home without forced turn battles |
| Open plains, meadows, typical roads | Low and intermittent; hostile encounters can be bypassed | Long-distance travel is not a repeated battle loading screen |
| Typical forests and riversides | Low; a few neutral or roaming enemies with visible cues | Exploratory variety without a monster corridor |
| Night outdoors | Some increase in threat, subject to avoiding unavoidable chase chains | Night matters, but never erase travel choice |
| Natural caves and underground resource zones | Moderate threat, separated into pockets rather than every ore vein | Mining remains usable; deeper/rarer resources may carry real risk |
| Ruins, hostile camps, dungeon approaches | Higher but deliberately bounded encounter groups with breathing pockets | Purposeful combat destination with material, contract and progression rewards |
| Dungeon interiors and lairs | Designed encounter sequences, elites and bosses | Tactical challenge chosen by the player; not infinite spawn clutter |

Use biomes, surface and local terrain conditions, dimension, nearby structures/POIs, time/weather, player-built settlement/base claims (if a robust criterion is eventually defined) and encounter lifecycle. **Do not simply tie difficulty to distance from world origin**: Minecraft's infinite procedural exploration and arbitrary spawn positions would make it unreliable.

### Special-case handling to design, not silently omit
- Nether/End and ocean ecosystems, including swimming, flying and lava creatures, must eventually replace vanilla living mobs too. Their new content and vanilla progression-item substitutes require separate approval.
- A village without villagers, an ocean without fish or a Nether without blaze materials can break vanilla survival loops. The ecology plan must not make the world functionally empty.
- 'Safe' cannot mean erasing any hostile encounter whenever a player places a torch; treat player-created settlements and torches with an exploit-resistant rule and clear explanation. Exact safe-area mechanism is undecided.

## 4. R_PG encounter lifecycle / preventing nuisance combat

Proposed state model:
1. `IDLE/PATROL`: visible creature/group stays within designated home territory, occasionally moves.
2. `NOTICE/ALERT`: verifies vision/engagement condition; shows **!** and a readable reaction/pose/sound, not instant unavoidable battle.
3. `CHASE`: pursues for a bounded distance/time while respecting terrain, cliffs, water, player path and settlements.
4. `CONTACT/CLAIM`: server atomically reserves the encounter and eligible player party before opening the on-site four-character battle.
5. `DISENGAGE/RETURN`: loss of target, excess pursuit distance or safe boundary makes it abandon chase and return. Include a short re-aggro grace to avoid stutter.
6. `BATTLE RESOLVE`: release/restore actor and player ownership; apply win/lose/escape consequences and rewards once; guard immediate recontact.
7. `COOLDOWN/RESPAWN`: defeating a field representative prevents it immediately appearing at the same place. Existing nearby mobs must not chain-lock the player as battle UI closes.

Rules:
- Only a few nearby hostile groups should be allowed to actively claim/chase the *same player* at once. A visible nearby mob can exist without being allowed to initiate a second immediate battle.
- Do not rely solely on reduced raw mob spawns: group separation, line-of-sight, routes, chase leash, post-battle relief and safe transit are each necessary.
- If several field representatives overlap, the implementation needs an explicit group composition/encounter merge rule, not two simultaneous battles.
- Rare/strong enemies should have visibly different silhouettes or ! signaling. Avoid surprise invulnerability or invisible proximity trigger.
- World simulation for uninvolved players continues. The avatar, battle area, projectiles, other mobs, cross-player hits, spectator collision and drop/loot duplication need a consistent server-authoritative boundary; implementation is NOT approved yet.

## 5. Spawn budgets: candidate prototype targets, not numerical canon

Initial telemetry and gameplay targets proposed for review:
- Measure **unwanted battle starts per 5 minutes of ordinary traversal**, **voluntary engaged battles per 5 minutes in a hostile area**, average successful avoidance, travel time, time spent in combat versus exploring, and repeat-aggro loops.
- *First draft experience target*: a player deliberately walking along ordinary safe routes should generally have **zero or at most one unavoidable encounter in a 5-minute segment**. In dangerous destinations, additional **chosen** encounters are fine.
- Start by testing only **one or two aggro-capable groups** inside the player's immediate *ordinary-travel* vicinity, then adjust from profiling and real movement. The "group" is a field encounter representative, **not necessarily one in-battle enemy**.
- Avoid several visible chase groups stacking at a tunnel or unavoidable bridge unless the location is clearly an intentional challenge.
- After combat, test a short anti-chain-contact protection interval, along with suppression of already claimed nearby groups. Determine exact duration from actual gameplay, not a blind copy of old 20–35-second common respawn.
- Progression changes risk by biome/POI/content, not by linearly increasing random enemy density indefinitely.
- Track group cap and spawn rate **per player/per active area and server globally** so multiplayer does not multiply expensive entity AI/VFX and uncontrollable group swarms.

These are **test targets and starting experiments**; exact spawn weights, alert ranges, detection timing, respawn intervals and caps are unapproved.

## 6. Vanilla survival dependency replacement matrix

Before suppressing vanilla spawns globally, validate that important vanilla loops still work with the new ecology:

| Vanilla living-mob dependency | Examples | Replacement needs |
| --- | --- | --- |
| Passive animal food and farm loops | Pork, beef, chicken, mutton, milk, eggs | Equivalent food/husbandry resources or explicit alternate recipes/systems; how farm interactions work still undecided |
| Raw materials | Leather, wool, feathers, ink, honey, slime | Drop sources through authored fauna, recipes and/or trading; avoid forcing tactical battle for every trivial material |
| Fishing/water life | Fish, squid, aquatic drops | Non-hostile aquatic life, useful fishing and any needed resource conversion |
| Villager economy / job sites | Trading, village services, golems, raids | New non-hostile NPC services and credible village economy; decide what happens to vanilla villages/structures |
| Hostile drop progression | Bones, string, gunpowder, rotten flesh, arrows, ender pearls | Replacement loot tables and recipes so normal crafting/research remains possible |
| Nether progression | Blaze rods/powder, ghast tears, magma cream, wither skeleton drops | Author equivalent dangerous encounters or crafting/trading sources without forcing an unrelated mandatory story |
| End progression / boss | End crystals, dragon-related mechanics, shulker drops | Decide how End portal and End items remain attainable with vanilla living bosses removed |
| Transport and utility mob mechanics | Horses, livestock leads, boats unchanged, mounts | Proposed replacement mounts/utility fauna if these gameplay capabilities remain desirable |

Do not assume all vanilla mob drops must remain identical; design alternatives explicitly. **Do not remove every passive mob until replacements for food and core resources are available and tested.**

## 7. Proposed technical architecture (implementation deferred)

- Define `ecology zones` + `spawn profiles` **data-first**: biome tags, dimension, altitude/surface/fluid conditions, time, structure/POI proximity, spawn weights, hostile/neutral/passive role, group caps, patrol/leash, cooldown and resource loot references.
- Prefer NeoForge documented **biome modifiers / remove_spawns** for natural vanilla population removal, alongside verified spawn-position rules for new custom mobs. Structure spawn overrides, commands/spawn eggs, conversions, spawners, breeding and entities loaded from existing chunk saves are separate gates; audit them rather than assume biome removal catches all.
- Source-based version check before implementation: current runtime is **NeoForge 26.2.0.62**. External docs on main or newer (26.3) may differ; verify exact APIs and behavior with local jar and dedicated-server tests.
- Existing legacy `AsterMarchVanillaSpawnGuard` and `FieldVanillaMobSanitizer` demonstrate spawn prevention and periodic region cleanup, but they are hardcoded to retired map regions and blanket-scan `Mob`; **do not enable or copy them wholesale**. The latter would delete custom peaceful mobs as well.
- Server validates world spawn, encounter ownership, resource drops and party reward exactly once; client only renders reactions, actual animation and VFX.
- **Never solve global replacement by scanning every entity in a large radius every tick.** Prefer spawn/registration gates, bounded event-driven bookkeeping and infrequent cached reconciliation for already-loaded mobs, if justified.
- Ambient passive fauna should remain inexpensive: no large-radius persistent chase/pathfinding on every entity.
- World generation and structure integration should be based on actual terrain validation and avoid flattening terrain merely to fit a battle camera.
- New custom entity IDs, data schema and tests need review before registration. Avoid save compatibility surprises and third-party-mod entity deletion.

External behavior/reference sources (implementation study, not imported code):
- NeoForge biome spawn modifiers and documented `remove_spawns`: https://docs.neoforged.net/docs/1.21.10/worldgen/biomemodifier/ . The 26.2 runtime specifics still require checking.
- NeoForge living-entity spawn placement rules: https://docs.neoforged.net/docs/entities/livingentity/ .
- Cobblemon's condition/preset-driven spawn files as a data-design reference: https://wiki.cobblemon.com/index.php?title=Spawn_Pool_World .
- Cobblemon's reusable environment spawn presets: https://wiki.cobblemon.com/index.php/Spawn_Detail_Presets .
- Original TURNBOUND encounter principles retained only as **reference**, not current world canon: `turnbound-alpha17-full-v04:projects/turnbound/ENCOUNTER_ENEMY_PLACEMENT_v1.md` and `world/FieldEncounterRules.java`.

## 8. One vertical slice before mass removal

Do **not** populate the full 26.2 overworld with placeholder mobs or empty it permanently first.

Recommended implementation sequence:
1. Inventory vanilla survival dependencies and decide replacement loot for the first surface biome and first cave environment.
2. Agree on one believable peaceful/neutral authored resource creature and one distinct hostile enemy family; ensure the hostile has a properly animated `!` -> chase -> contact behavior, a four-character battlefield placement and readable reaction/camera.
3. Confirm safe travel from new spawn and first starter selection to a visible optional field encounter, one short tactical battle, reward, return-to-overworld without immediate repeated battle, and a functional contract/gacha reward link.
4. Playtest walking, running, mining, avoiding an enemy, chases that give up, victory/defeat/retreat/reconnect, and two-player simultaneous movement/claim. Profile entity counts and pathfinding.
5. Expand by biome/POI and verify resource drop parity; only then apply broad vanilla living-mob removal across the agreed world scope.

Exit gate: player can roam standard overworld regions without repetitive compulsory battle, see meaningful peaceful activity, voluntarily enter dangerous zones, defeat a monster group and obtain useful survival/progression rewards, with no broken save/session or resource bottleneck.

## 9. Still requires explicit user decisions

- How passive/neutral fauna are gathered, farmed, protected and hunted in normal survival; whether every attack on an animal triggers a turn encounter or ordinary survival processing is allowed.
- How to represent the user's player body in a field battle and defend it from unrelated world hazards.
- Which character/monster identities, themed region(s) and art sources constitute the first vertical slice.
- Gacha resources, rates, duplicates, party growth and user-facing UI.
- Nether/End replacing rules and how original vanilla structures/milestones are transformed.
- Exact biome-specific spawn density, limits, night difficulty, chase distances and cooldown durations.

No source changes, Java compile/build, JAR, client playtest or multiplayer test were run for this **documentation-only** design checkpoint.

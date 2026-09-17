# Open-World RPG — Mount / Traversal Reference

> Status: subordinate design reference  
> Master canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Regional context: `REGIONS.md`  
> Scope: mount visual sourcing, progression, speed/handling, combat boundaries, multiplayer behavior and summon/defeat rules.  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins.

This file closes the mount/traversal decisions that must not be invented during implementation. Mounts are designed from actual external visual/behavior sources and benchmarked against proven mount systems rather than by arbitrary percentage escalation.

---

# 1. External precedents and what is adopted

## Guild Wars 2

Useful precedent:

- mounts are differentiated primarily by movement role rather than cosmetic speed tiers;
- Raptor, Skimmer, Griffon and Skyscale solve different traversal problems;
- mount speed can be meaningfully above on-foot speed without making every mount identical;
- flying mounts separate horizontal cruising, climbing and dive/burst behavior;
- mount health can force a dismount rather than turning the mount into permanent invulnerability.

Observed reference ratios from the current GW2 wiki:

- on-foot baseline: about 294 units/s;
- Raptor run: about 600, roughly 2.0x the on-foot baseline;
- Jackal: about 625, roughly 2.1x;
- Skimmer water-surface travel: about 850, roughly 2.9x;
- Skyscale airborne: about 600, roughly 2.0x, while ascent is slower;
- Griffon normal airborne travel is much lower than its extreme dive speed; the fully developed dive can become several times faster than on-foot movement.

Project adoption:

- role-based mounts;
- late flight with separate cruise/climb/dive tuning;
- temporary mount durability/Resolve that forces dismount instead of permanent death;
- high-speed burst is situational, not the permanent default cruise speed.

The project does not copy GW2 mastery tracks, exact unlock grind, currency structure or numbers.

## Minecraft horse range

Vanilla Minecraft is useful only as a familiar movement-feel benchmark, not as a visible mount source.

A player sprint is roughly 5.6 blocks/s. Ordinary vanilla horses cover a very wide speed range, with average horses around the high single-digit blocks/s range and exceptional horses around the mid-teens.

Project adoption:

- first mount stays close to sprint speed because the early world must still be read on foot-scale;
- midgame fast ground travel moves into the ~8–10 blocks/s range;
- late burst traversal may briefly enter the mid-teens without making every road/POI unreadable.

Random horse-stat genetics are explicitly not adopted.

## World of Warcraft

WoW demonstrates a clear progression from ordinary ground travel to substantially faster mounted/flying travel.

Project adoption:

- late traversal should feel materially faster than early traversal;
- flight is a major progression reward.

The project does not adopt a permanent multi-hundred-percent flight cruise that trivializes a 30k authored terrain map immediately after unlock.

## Current Minecraft 26.2 references

### Alex's Mobs Continued

- current 26.2 Fabric release observed;
- carries the original creature models, animations and riding behaviors forward rather than redesigning them;
- useful rideable identities include Komodo Dragon, Elephant and Laviathan;
- Laviathan supports a distinctive multi-passenger water/lava traversal role in the original design;
- project status: dependency/reference only; project progression, spawn rules, stats and ownership behavior remain authoritative.

Important license note: current public metadata is inconsistent between surfaced Modrinth and CurseForge pages. Therefore the project treats the mod as a normal external dependency and does not copy its code/assets into this public repository until the exact current source/license boundary is re-audited at M0.

### Jasmine Dragons

- current 26.2 Fabric release observed;
- strong reference for aim-directed dragon flight, climb/dive handling and breath/ranged mounted attacks;
- its own design emphasizes wild dragons/temporary trust rather than the project's persistent stable-owned mount model.

Project status: **REFERENCE / optional dependency behavior study**, not the canonical permanent Sky Drake implementation.

### Icy's Better Horses / Vehicle Upgrade / Horse Combat Controls

These remain code/UX references for ownership, handling, riding controls and command/roster concepts. Vanilla horse visuals are not used.

---

# 2. Locked external visual sources

## First permanent mount — Trail Stag

Primary visual source:

- Quaternius `Ultimate Animated Animal Pack`;
- CC0;
- contains 12 animated animals with 12+ animations each, including movement/attack/death/kick/gallop/walk/jump sets;
- current project choice: use the pack's **Stag** as the first-mount visual/animation base.

Why:

- it already has a strong readable quadruped silhouette and movement animation family;
- it fits the R01 meadow/forest-fringe identity better than a generic horse;
- it is non-vanilla in implementation/presentation even though it remains visually understandable;
- CC0 allows the model/animation source to be converted and adapted for the public repo.

The implementation is a project-owned custom rideable entity using the selected Stag visual/animation base, not a reskinned vanilla horse entity exposed to the player.

## Midgame agile mount — Jungle Komodo

Primary visual/behavior source:

- `Alex's Mobs Continued` Komodo Dragon dependency.

The original creature is tameable/rideable when saddled. The project uses the existing creature/model/animation if M0 confirms the current Continued build's taming/riding behavior is stable enough to integrate cleanly.

Fallback if current dependency behavior is broken or impossible to decouple from unwanted progression:

- keep the **Komodo / large jungle-lizard role**, but source another external animated lizard model before implementation;
- do not replace it with an invented AI creature merely to keep the schedule.

## Heavy utility mount — Caravan Elephant

Primary visual/behavior source:

- `Alex's Mobs Continued` Elephant dependency.

The original creature supports riding and heavy charge behavior. The project uses that visual/animation identity as a later utility/combat-capable mount, but does not inherit random breeding/stat progression or chest-storage inflation automatically.

## Amphibious / hazardous-terrain mount — Laviathan

Primary visual/behavior source:

- `Alex's Mobs Continued` Laviathan dependency.

The original design is a large rideable creature capable of carrying up to four passengers and operating in lava/water contexts. This is strong enough to preserve as a specialized multiplayer traversal identity rather than redesigning it.

The project removes its dependency on vanilla Nether progression: **launch acquisition belongs to R11 Inner Sea only**, with project stable/tack rules owning access.

## Late permanent flying mount — Sky Drake

Primary visual source:

- Quaternius `Animated Monster Pack` Dragon;
- CC0;
- fully animated source pack with FBX / OBJ / Blend formats.

Fallback visual pool:

- Quaternius `Ultimate Monsters` dragon/winged-monster candidates, also CC0, if asset intake proves their silhouette/animations superior at Minecraft scale.

Behavior references:

- Guild Wars 2 Skyscale for controlled climb/hover-style terrain solving;
- Guild Wars 2 Griffon for rewarding dive-speed behavior;
- Jasmine Dragons for Minecraft-scale aim-directed flight and mounted attack reference.

The final Sky Drake is project-owned/persistent and does **not** inherit Jasmine Dragons' temporary-trust/no-permanent-pet rule.

---

# 3. Mount-system architecture

Mounts are **persistent unlocks that summon traversal entities**, not livestock the player must continuously babysit in the world.

Rules:

- acquiring/registering a mount permanently unlocks that mount for the player;
- unlock state, summon validity, current Resolve and special ability state are server-authoritative;
- mounts are summoned through one rebindable mount action/whistle behavior rather than carrying physical saddled creatures everywhere;
- dismounted project mounts return/despawn cleanly instead of following the player through every interior and creating pathfinding/teleport problems;
- mounts cannot permanently die;
- mounts do not use hunger, breeding RNG, random speed genes, bonding grind, durability, mount levels or a separate mount-XP treadmill;
- cosmetic tack/color variants do not alter stats;
- no mount can be nested into an inventory item to bypass gameplay restrictions unless implementation explicitly stores only the unlock identity rather than a live entity state.

This architecture is chosen for open-world RPG usability and multiplayer reliability rather than simulating vanilla horse ownership chores.

---

# 4. Summon / dismount / combat rules

## 4.1 Summon

Baseline mount summon conditions:

- player has the mount unlocked;
- enough collision/space exists for the selected mount;
- player is not in a dungeon/interior/no-mount authored volume;
- player has neither dealt nor received hostile combat damage for **4.0 seconds**;
- summon channel: **1.0 second**;
- taking hostile damage during the channel cancels summon.

This prevents repeatedly mounting during a boss attack to absorb one hit.

## 4.2 Normal dismount

Normal manual dismount has no cooldown.
The mount returns/despawns after the rider has fully dismounted unless the specific authored interaction requires it to remain temporarily visible.

## 4.3 Mount Resolve

Instead of permanent mount death, each summoned mount has a temporary `Resolve` buffer.

```text
ResolveMax = PlayerMaxHP * MountResolveMultiplier
```

Damage while mounted first damages Resolve. If an incoming hit exceeds remaining Resolve, overflow damage transfers to the rider.

When Resolve reaches zero:

- force dismount;
- remaining overflow damage still applies to player;
- mount cannot be summoned for **12 seconds**;
- ordinary combat lock rules also apply, so the player cannot simply whistle again during the same attack sequence.

Resolve fully resets on a fresh legal summon outside combat. It is not a second potion/repair economy.

Baseline multipliers:

| Mount | Resolve multiplier |
|---|---:|
| Trail Stag | 1.00x player max HP |
| Jungle Komodo | 1.20x |
| Caravan Elephant | 2.00x |
| Laviathan | 2.50x |
| Sky Drake | 1.50x |

## 4.4 Class combat while mounted

Baseline rule:

- traversal mounts do **not** allow the player's ordinary 4-skill class bar while moving mounted;
- attempting a normal weapon/basic attack on a pure traversal mount dismounts the rider cleanly before the attack proceeds;
- this prevents dozens of weapon/skill animations from needing broken mounted variants;
- dedicated combat-capable mounts may expose at most **1–2 authored mount abilities** with their own real animations/hitboxes;
- mounted abilities never inherit the full class skill system automatically.

Combat mounts are exceptions built deliberately, not a blanket promise that every class spell works from every creature.

---

# 5. Baseline movement benchmark

Technical on-foot reference for balance:

- vanilla-style player sprint benchmark: approximately **5.61 blocks/s**.

The project uses absolute blocks/s internally for mount tuning so numbers are understandable and testable.

Launch travel-speed targets:

| Mount | Primary terrain | Cruise | Burst / special speed | Relative to player sprint |
|---|---|---:|---:|---:|
| Trail Stag | ordinary ground / roads / meadow | **6.4 b/s** | no speed burst | 1.14x cruise |
| Jungle Komodo | agile ground / jungle / uneven paths | **8.4 b/s** | **10.5 b/s** dash | 1.50x / 1.87x |
| Caravan Elephant | road / dryland / heavy traversal | **6.3 b/s** | **9.0 b/s** charge | 1.12x / 1.60x |
| Laviathan | water / lava | **8.8 b/s** in water/lava | no generic burst | 1.57x |
| Laviathan | ordinary land | **4.6 b/s** | — | slower than sprint |
| Sky Drake | airborne cruise | **10.5 b/s** | **16.5 b/s** dive | 1.87x / 2.94x |

Interpretation:

- the starter Stag is convenience, not a map skip;
- Komodo is the first clearly fast ground mount;
- Elephant remains valuable through toughness/charge, not because every later mount must be faster;
- Laviathan is dominant only in its water/lava niche;
- Sky Drake is a late major traversal reward, but ordinary cruise is still slow enough for landmarks/terrain to remain readable;
- the extreme late-game speed lives in a controllable dive state rather than permanent 5x cruise.

No launch ground mount has a normal cruise speed above **10.5 b/s** unless later actual Azari traversal playtesting demonstrates that distances are meaninglessly long at these values.

---

# 6. Handling / acceleration targets

Speed alone is not the identity.

| Mount | 0→90% cruise | Turning identity | Terrain identity |
|---|---:|---|---|
| Trail Stag | ~1.0 s | high / forgiving | roads, meadow, forest-edge, low obstacles |
| Jungle Komodo | ~0.8 s | very high | dense paths, quick direction changes, uneven jungle ground |
| Caravan Elephant | ~1.8 s | low / weighty | stable road travel, deliberate charge lines |
| Laviathan | ~1.5 s in water/lava | medium | broad aquatic/lava navigation, large collision footprint |
| Sky Drake | ~1.2 s cruise; ~1.5 s into full dive | medium, banked | open-air long traversal, controlled vertical solving |

Exact acceleration interpolation is an implementation/data value, but the target time-to-speed above is canonical.

No mount should snap instantly to full speed if its animation/body mass visually implies acceleration.

---

# 7. Core launch mount progression

## 7.1 Trail Stag — first R01 mount

Role:

- first permanent mount;
- sustained-travel convenience;
- teaches summon/stable system without trivializing exploration.

Unlock:

- stable visible from first settlement visit;
- unlocked through an early R01 stable/road event around the first **25–40 minutes** of ordinary play, approximately local Lv 3–4;
- must occur before the player has exhausted most R01 exploration;
- first registration is **free** after the event/quest. No early Gold grind blocks basic mount access.

Movement:

- cruise 6.4 b/s;
- high steering response;
- no endurance drain for ordinary travel;
- target step/terrain assistance: approximately 1.25-block step handling where the Minecraft movement backend allows it cleanly;
- charged/intentional jump target: enough to clear roughly a 2-block obstacle/gap without becoming a vertical-cliff solver.

Combat:

- no mount attack;
- attempting player basic attack dismounts first.

Why it remains relevant:

- easiest handling;
- tight paths/settlement approaches;
- zero dash/endurance management;
- immediate summon-size compatibility compared with later huge mounts.

Visual source: Quaternius Ultimate Animated Animal Pack Stag.

## 7.2 Jungle Komodo — R05 agile ground mount

Role:

- first clearly fast ground mount;
- dense-jungle and route-changing traversal;
- short tactical burst rather than permanent near-horse-max speed.

Unlock:

- R05 authored handler/hunt/ecology quest;
- suggested-entry context: around Lv 20;
- project stable registration fee after quest: **600 Gold**;
- fee is intentionally well below one hour of same-tier normal gross income and is not the main gate.

Movement:

- cruise 8.4 b/s;
- dash 10.5 b/s for **2.5 seconds**;
- dash uses one mount-endurance bar;
- full endurance supports one complete 2.5-second dash;
- endurance refills from empty in **5 seconds** while not dashing;
- very high turn responsiveness.

Combat:

- baseline launch implementation remains traversal-first;
- if current dependency animation/hitbox integration passes M0, one authored bite/venom attack may be enabled;
- if that attack is not technically clean, remove the attack rather than ship a visually mismatched hitbox. Travel role remains complete without it.

Visual/behavior source: Alex's Mobs Continued Komodo Dragon dependency.

## 7.3 Caravan Elephant — R09 heavy utility mount

Role:

- high Resolve;
- charge-based route/encounter utility;
- deliberate heavy-mount feel;
- visually appropriate to R09 dry grassland/caravan routes.

Unlock:

- R09 caravan protection/animal-handler quest;
- stable harness/registration: **1,500 Gold**;
- recommended around the R09 Lv44 progression band.

Movement:

- cruise 6.3 b/s;
- slow acceleration and low steering responsiveness;
- charge reaches 9.0 b/s for **2.0 seconds**;
- charge cooldown: **8 seconds**;
- charge does not phase through solid terrain and must respect the creature's visible width.

Combat ability — Charge:

- dedicated mount ability 1;
- contact hitbox follows the Elephant's actual forward collision silhouette;
- normal enemy impact: `0.70 * normalized current weapon power` equivalent physical/impact damage + strong stagger;
- elites/bosses: `0.35 * weapon power` + meaningful poise damage, no launch/knockback cheese;
- one target cannot be hit repeatedly by overlapping ticks from the same charge.

Storage:

- do **not** inherit a large extra chest inventory at baseline;
- the player already has a large backpack + Material Pouch and another mobile inventory would add management complexity.

Visual/behavior source: Alex's Mobs Continued Elephant dependency.

## 7.4 Laviathan — R11 Inner Sea hazard travel / multiplayer ferry

Role:

- late specialist for large bodies of water and lava/volcanic-route crossings where relevant;
- multiplayer group transport;
- not a universal land-speed replacement.

Unlock:

- **R11 Tidecross Freeport / Inner Sea authored handler and route trial only**;
- launch acquisition is not available from R10;
- progression context: open-sea/deep-route play in the R11 Lv44–64 layers;
- project tack/registration fee after qualification: **3,000 Gold**;
- no Nether visit or vanilla dimension progression required.

Passengers:

- target maximum: **4 riders**, preserving the creature's proven source identity and matching the launch formal-party cap;
- one rider is the controller;
- remaining seats are passengers;
- server owns seat/control assignment;
- passengers cannot steal controller authority by reconnecting or changing seat client-side.

Movement:

- water/lava cruise: 8.8 b/s;
- land cruise: 4.6 b/s;
- medium turning, deliberately broad turn radius;
- no generic dash.

Combat:

- no baseline mounted attack;
- role is traversal/ferry utility.

Visual/behavior source: Alex's Mobs Continued Laviathan dependency.

## 7.5 Sky Drake — R12 permanent flying mount

Role:

- final launch-tier traversal unlock;
- long-distance airborne travel;
- controlled vertical solving;
- rewarding dive-speed mastery without erasing map scale.

Unlock:

- major R12 dragon-sanctuary/rift traversal quest;
- progression context around Lv72+;
- stable harness/registration fee: **6,000 Gold** after quest;
- quest/discovery is the real gate; Gold fee is a modest late-game sink, not RNG purchase gating.

Movement:

- airborne cruise: 10.5 b/s;
- horizontal cruise can be maintained indefinitely;
- normal climb speed: **5.0 b/s**;
- descent can be faster than climb and restores Wing Stamina;
- dive max: **16.5 b/s**;
- dive requires downward pitch/altitude loss; it cannot be toggled as a flat-ground turbo button.

### Wing Stamina

Wing Stamina exists only to limit *vertical gain*, not normal horizontal flight.

- full Wing Stamina permits approximately **48 blocks of total active vertical climb** from one full bar;
- hovering/level cruise does not drain it materially;
- descending regenerates it rapidly;
- grounded/perched state regenerates from empty to full in about **5 seconds**;
- ordinary horizontal travel never forces the player to land every minute.

This intentionally sits between GW2's Skyscale terrain-solving model and Griffon dive-speed model.

### Flight restrictions

- cannot summon in normal combat;
- auto-dismount or reject entry in authored no-fly dungeon/interior volumes;
- flight is disabled where it would bypass encounter geometry or progression-critical interior traversal;
- no invisible global ceiling lower than normal world safety limits merely to punish flight;
- major authored world landmarks should remain approachable from the air rather than covered in arbitrary anti-flight walls.

### Mounted combat

Sky Drake may ship with **one** dedicated breath/projectile attack only if the final Quaternius animation/VFX integration passes the project's visible-range = actual-hitbox standard.

Provisional canonical behavior if accepted:

- usable only below **12 b/s** current speed;
- cannot fire during full-speed dive;
- cooldown: **3.0 seconds**;
- projectile/stream damage is utility-level rather than replacing the player's full class rotation;
- no full class skill bar while mounted.

If final animation/VFX quality does not pass, ship the Sky Drake as a pure traversal mount rather than adding a fake particle breath.

Primary visual source: Quaternius Animated Monster Pack Dragon.  
Fallback visual source: Quaternius Ultimate Monsters winged/dragon candidate after actual asset comparison.

---

# 8. Optional regional mounts that do not define progression

Optional mounts can exist only when they provide a distinct role and have strong external visuals.

## R02 Grizzly-style heavy companion mount

Alex's Mobs Continued Grizzly Bear is a potential optional combat/tough mount because the source creature can be tamed/ridden in the original ecosystem.

If adopted:

- cruise around 6.0 b/s;
- Resolve around 1.6x player max HP;
- no higher top-speed role than Stag/Komodo;
- acquisition through an authored R02 ecology/hunt relationship, not random low-probability feeding spam.

Status: optional candidate pending direct current 26.2 behavior verification.

## R04 Tusklin regional ride

Tusklin is more interesting as a **temporary/regional difficult-animal ride** than a permanent core stable tier because its source identity involves eventually bucking the rider and extending ride time through special handling.

If used, preserve that distinctive behavior in an R04 event/route instead of flattening it into another permanent speed mount.

Status: optional regional traversal content, not required for launch mount progression.

---

# 9. Stable / economy rules

The stable is a world service, not a mount gacha menu.

Core rules:

- core mounts are unlocked through quests/discovery/creature interaction first;
- Gold pays modest registration/tack/service costs on later mounts but never replaces the world-content unlock;
- first Trail Stag is free;
- later baseline fees are calibrated to stay around or below roughly **30–50 minutes of same-tier gross Gold income**;
- mounts are never sold through 10-minute random merchant rotation;
- no random mount-quality roll;
- no breeding for better stats;
- cosmetic tack/color may be purchased separately without stat changes;
- stable UI shows role, speed, handling, Resolve and special traversal ability before summon/select.

Current baseline registration:

| Mount | Unlock source | Registration/tack |
|---|---|---:|
| Trail Stag | early R01 stable/road quest | free |
| Jungle Komodo | R05 ecology/handler quest | 600 Gold |
| Caravan Elephant | R09 caravan/handler quest | 1,500 Gold |
| Laviathan | **R11 Inner Sea handler/route trial** | 3,000 Gold |
| Sky Drake | R12 dragon/rift traversal quest | 6,000 Gold |

These fees are balance data and can move only after playtest evidence; implementation must not invent separate prices.

---

# 10. Fast travel relationship

Mounts and fast travel solve different problems.

- mounts handle local/world traversal, route discovery and travel between undiscovered destinations;
- discovered shrines/major hubs remain the long-range teleport network;
- ordinary camps remain non-fast-travel locations;
- late Sky Drake does not delete shrine travel value because 30k-scale cross-continent travel is still faster through discovered hubs;
- travel content should not artificially disable mounts just to force shrine use.

---

# 11. Multiplayer rules

Mount ownership is personal per player.

- unlocks are stored per player;
- one player's registration does not unlock a mount for everyone;
- each player may summon their own mount when valid;
- Laviathan is intentionally the main multi-passenger mount at baseline;
- passengers do not consume ownership or permanently unlock the mount merely by riding another player's creature;
- server validates mount speed, Resolve, ability cooldowns, seat/controller state, summon restrictions and flight state;
- clients may predict smooth movement but do not authoritatively decide teleport/speed/ability success;
- mount summoning/despawning must not duplicate entities/items through disconnect/reconnect.

Essential connection convenience does not alter these authority rules.

---

# 12. Fall / collision behavior

Mount traversal must not become either free invulnerability or constant random damage.

- normal small drops within the mount's intended movement animation are tolerated;
- larger fall damage applies to Resolve first, then overflow transfers to the player;
- forced dismount from depleted Resolve uses the same 12-second lock;
- Elephant/Laviathan large bodies cannot squeeze through tiny collision gaps merely because the rider camera fits;
- high-speed charge/dive never phases through solid blocks;
- mount step height and collision must match the visible legs/body closely enough to avoid floating through terrain.

Sky Drake flight landing uses a dedicated landing state/animation if the source animation supports it; do not simply freeze the flying pose on the ground.

---

# 13. Data contract

Mount content is data-driven where practical.

Suggested split:

```text
mounts/
  definitions/*.json
  abilities/*.json
  unlocks/*.json
  region_rules/*.json
assets/
  source_bindings/*.json
```

Mount definition should resolve at least:

```text
id
player_facing_name
visual_source_id
animation_profile
unlock_id
registration_gold
cruise_speed
acceleration_to_90_ms
turn_profile
resolve_multiplier
terrain_profile
passenger_count
summon_space_profile
mount_ability_ids[]
flight_profile_or_null
server_authority_flags
```

Flight profile should resolve:

```text
cruise_speed
climb_speed
dive_speed
wing_stamina_vertical_budget
wing_stamina_ground_recovery_ms
dive_regen_rule
no_fly_volume_behavior
```

Code owns validation and state transitions. Data owns mount-specific tuning.

---

# 14. Pre-code verification checkpoint

Before mount source implementation begins:

1. download Quaternius Ultimate Animated Animal Pack and Animated Monster Pack from canonical pages;
2. record exact Stag/Dragon source filenames and SHA-256 hashes;
3. inspect rig/animation names and Minecraft-scale silhouette in Blender/Blockbench;
4. verify Stag walk/gallop/jump/idle/death or fallback animation states required by runtime;
5. verify the selected Dragon has acceptable flight/turn/landing visual states or choose the predeclared Quaternius fallback before coding it;
6. install current Alex's Mobs Continued 26.2 Fabric build in the M0 dependency sandbox and directly test Komodo riding, Elephant riding/charge and Laviathan four-seat/control behavior;
7. re-audit Alex's Mobs Continued's exact current license/source metadata because public project pages currently disagree;
8. verify Essential-host multiplayer seat ownership/synchronization architecture before claiming multiplayer-tested;
9. create a 1 km straight/curved Azari route test and measure actual travel times for 6.4 / 8.4 / 10.5 b/s ground and 10.5 / 16.5 b/s air targets;
10. only after real client movement review adjust numbers if traversal feels too slow/fast; document changes first.

No generic vanilla horse placeholder is used while waiting for this checkpoint.

---

# 15. What this pass closes

This file locks:

- summonable persistent mount ownership model;
- no mount hunger/breeding/random-stat/level grind;
- summon/combat/dismount rules;
- Resolve and forced-dismount behavior;
- player-class combat boundary while mounted;
- launch speed/acceleration/handling targets;
- first R01 Trail Stag source, unlock timing and balance;
- R05 Jungle Komodo source and movement role;
- R09 Caravan Elephant source, charge and toughness role;
- **R11-only Laviathan** multi-passenger hazard-travel role;
- R12 Sky Drake CC0 visual source, flight stamina, cruise/climb/dive behavior;
- stable prices and acquisition philosophy;
- multiplayer seat/authority rules;
- external visual/behavior source boundaries.

Implementation is not allowed to replace these with a generic horse tier ladder or invent new traversal numbers in code. Future changes come from actual playtest evidence and update canon first.
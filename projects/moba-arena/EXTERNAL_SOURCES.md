# MOBA Arena — External Source & Donor Audit

This file records the exact external foundations considered for the project and how each one may be used.

Candidate status is not proof that third-party bytes have already entered the repository. Actual intake is recorded in `THIRD_PARTY_ASSETS.md`.

## Status vocabulary

- `ADOPTED DONOR` — selected implementation provenance for a subsystem; port still requires notice/modification tracking
- `CONDITIONAL RUNTIME LOCK` — selected if the M0 compatibility/smoke test passes
- `PRIMARY LOCAL-ONLY CANDIDATE` — intended owner-local content, not approved to commit
- `FALLBACK` — retained only if the primary path fails
- `REFERENCE ONLY` — study behavior; do not copy code/assets
- `REJECTED FOR INITIAL STACK` — not used in the first slice
- `BLOCKER` — unresolved source/version/terms that must be closed before source bootstrap

---

## 1. Primary player-facing runtime

### Anime Assembly 1.1.4

- Status: `CONDITIONAL RUNTIME LOCK`
- Author: Haxsa_1545
- Source: https://www.curseforge.com/minecraft/mc-mods/anime-assembly
- Exact file: `AnimeAssembly+1.1.4.jar`
- CurseForge project ID: `1169249`
- File ID: `7514535`
- Game/loader: Minecraft 1.19.2 / Forge
- Curse Maven: `implementation fg.deobf("curse.maven:anime-assembly-1169249:7514535")`
- Project-listed license: Academic Free License v3.0
- Environment: client + server
- Advertised player-facing scope:
  - 22 playable characters;
  - character selection UI (`Ctrl+U`) and cycling;
  - four character abilities plus additional skill;
  - health-bar display;
  - blue/red team assignment helper;
  - MOBA Info/Start flow where all non-spectators must start/ready;
  - M-key equipment shop;
  - NPC hero variants and `/team` integration;
  - integrated MOBA mode and improved MOBA minimap;
  - modified MOBA map download linked from the project page.
- Required external dependencies listed by the author:
  - GeckoLib;
  - Kleider Custom Renderer;
  - Pehkui;
  - Player Animator.
- Planned use: direct dependency; it owns the character/combat/presentation layer.
- Public-repo boundary: do not blindly commit the JAR or extracted character/franchise assets. AFL-3.0 on the project page does not by itself establish rights to every underlying anime/comic character, trademark or third-party work represented inside it.
- M0 gate: must prove dependency compatibility, actual server-visible state and bridgeable shop/team/Ready signals before project bootstrap.

### Why this beats the initial 26.2 combat-framework plan

A generic combat framework such as Spell Engine would still require this project to choose/build champion kits, character art, animations, VFX, icons, character select, health bars and other presentation. Anime Assembly already provides these as one coherent runtime. The project therefore prefers the older Minecraft version with the stronger finished donor package.

---

## 2. Required Anime Assembly dependency profile

### GeckoLib

- Status: `CONDITIONAL RUNTIME LOCK`
- Source: https://www.curseforge.com/minecraft/mc-mods/geckolib
- Exact first target: `geckolib-forge-1.19-3.1.40.jar`
- CurseForge file ID: `4407241`
- Game/loader: 1.19.2 / Forge
- License: MIT
- Use: direct required animation/runtime dependency.

### Pehkui

- Status: `CONDITIONAL RUNTIME LOCK`
- Source: https://www.curseforge.com/minecraft/mc-mods/pehkui
- Exact first target: `Pehkui-3.8.2+1.19.2-forge.jar`
- CurseForge file ID: `5393090`
- Game/loader: 1.19.2 / Forge
- License: MIT
- Source page reports last successful test on Forge `1.19.2-43.3.13`.
- Use: direct required scaling dependency.

### playerAnimator

- Status: `CONDITIONAL RUNTIME LOCK`
- Author/project: KosmX `playerAnimator`
- Source: https://www.curseforge.com/minecraft/mc-mods/playeranimator
- CurseForge project ID: `658587`
- Exact first target: `player-animation-lib-forge-1.0.2.jar`
- CurseForge file ID: `4418149`
- Game/loader: 1.19.2 / Forge
- License: MIT
- Curse Maven: `implementation fg.deobf("curse.maven:playeranimator-658587:4418149")`
- Use: direct required player-animation dependency.
- Note: the Anime Assembly dependency link points to this KosmX project. Do not substitute a similarly named Player Animator fork/API without a new compatibility audit.
- SHA-256 remains M0 intake work because the actual JAR bytes have not yet been acquired in the current environment.

### Kleiders Custom Renderer API

- Status: `CONDITIONAL RUNTIME LOCK`
- Author: kleiders3010
- Source: https://www.curseforge.com/minecraft/mc-mods/kleiders-custom-renderer-api
- CurseForge project ID: `682065`
- Exact first target: `Kleiders Custom Renderer API 6.0.0 1.19.2.jar`
- CurseForge file ID: `5083496`
- Game/loader: 1.19.2 / Forge
- License: All Rights Reserved
- Curse Maven metadata: `implementation fg.deobf("curse.maven:kleiders-custom-renderer-api-682065:5083496")`
- Declared project relations: Dependencies (0); no required dependency is listed by CurseForge for this renderer itself.
- Use: local/direct required Anime Assembly renderer dependency.
- Public-repo bytes: **NO** unless explicit redistribution permission is later established. Reference the original project/file identity instead of rehosting the JAR.
- SHA-256 remains M0 intake work because the actual JAR bytes have not yet been acquired in the current environment.

The former Kleider identification `BLOCKER` is closed. Runtime compatibility is still unproven until the complete donor profile boots successfully.

---

## 3. Minion code and AI

### SimpleLaneWars / `c0mbit/mc-dota`

- Status: `ADOPTED DONOR` for narrow wave/tag/reward logic
- Source: https://github.com/c0mbit/mc-dota
- Audited commit: `cacd3625b8a0066d6085bbaa0c81a18ac58254fc`
- License: MIT
- Platform: Paper/Bukkit-style implementation, therefore source concepts must be ported to Forge 1.19.2.
- Exact useful provenance:
  - `src/main/java/com/simplelanewars/Main.java`
    - wave scheduling;
    - team/minion identity;
    - per-team spawn lifecycle;
    - nearby enemy lookup concepts.
  - `MinionListener.java`
    - no vanilla drops/XP;
    - killer/last-hit reward flow;
    - friendly-target filtering.
- Explicitly rejected source behavior:
  - generated 35x35/flat arena;
  - delete-all-minions before each wave;
  - raw `setVelocity` movement toward enemy spawn;
  - asymmetric/hard-coded player team handling;
  - scoreboard as production HUD.
- Modification rule: retain MIT notice/provenance in the port.

### SmartBrainLib 1.9

- Status: `CONDITIONAL RUNTIME LOCK`
- Source: https://github.com/Tslat/SmartBrainLib
- Branch: `1.19.2`
- Audited commit: `3d1263fe39bc96c84fe920632208e8958d24b13f`
- Project version on branch: 1.9
- Forge version used by that branch: 43.2.8
- License: MPL-2.0
- Use mode: direct dependency, not copied source.
- Useful audited primitives include:
  - `NearbyLivingEntitySensor`;
  - `GenericAttackTargetSensor`;
  - `SetAttackTarget`;
  - `SetWalkTargetToAttackTarget`;
  - `MoveToWalkTarget` / `WalkOrRunToWalkTarget`;
  - `StayWithinDistanceOfAttackTarget`;
  - `AnimatableMeleeAttack` / ranged behavior;
  - `ReactToUnreachableTarget`;
  - `LookAtTarget`.
- Planned use: actual pathfinding/behavior execution. Project glue provides lane waypoint memory and MOBA team/structure predicates.
- Compatibility note: branch Forge 43.2.8 vs provisional runtime target 43.3.13 must be smoke-tested, not assumed.

---

## 4. Structures, game state and fallback shop

### `cadox8/LoM`

- Status: `ADOPTED DONOR`
- Source: https://github.com/cadox8/LoM
- Audited commit: `5ae2b4b747989dc74ebe1af17869a11879cceecb`
- License: Apache-2.0
- Old platform: Bukkit/Spigot-era project; use requires porting rather than binary dependency.

Selected source provenance:

- `structures/Structure.java`
- `structures/TowerType.java`
- `structures/InhibType.java`
- `task/InhibTask.java`
- `managers/GameManager.java`
- `managers/Teams.java`
- `utils/TeamData.java`
- fallback-only: `shop/Shop.java`, `ShopManager.java`, `shop/item/ShopItem.java`, `ItemEffects.java`, `ShopItemType.java`

Adopted concepts:

- common TOWER / INHIB / final-core structure state;
- team ownership;
- health/reward lifecycle;
- tower attack parameters;
- inhibitor delayed regeneration;
- game/team state shapes;
- shop item price/effect/parts data model only if needed.

Known limitations requiring deliberate repair:

- old Bukkit integration is obsolete for this mod;
- tower `Structure.attack()` contains incomplete minion-target branches;
- old particle/reflection utilities are not desired production presentation;
- champion/skill code overlaps Anime Assembly and must not be ported.

Apache-2.0 attribution and modified-file notices are mandatory for actual ports.

---

## 5. Arena maps

### Anime Assembly modified MOBA map

- Status: `PRIMARY LOCAL-ONLY CANDIDATE`
- Modified-copy source: Anime Assembly's `Moba Mode Map Download` Google Drive link, file ID `1tL4A1RIjUULe7tJy2tRBjI1AFwGsqipW`.
- Parent map: Shinkiroo, `League of Legends Summoner's Rift (Pre-Season 10) [DOWNLOAD]`.
- Parent source: https://www.planetminecraft.com/project/re-league-of-legend-summoner-s-rift-download/
- Parent-map terms visible on the source page: editing and distributing are not allowed; the author asks to be informed for YouTube/public-server use.
- Anime Assembly identifies its map as a modified MOBA map, but no separate redistribution/edit permission for that modified copy has been verified.
- Planned use: first **local-only** arena because the adopted MOBA runtime was authored around it.
- Public Git/package: **DO NOT COMMIT OR REDISTRIBUTE MAP BYTES** under the evidence currently available.
- Git-safe records: source URLs/IDs, local checksum, expected world-folder identity, and project-created coordinate/metadata bindings.
- Remaining intake work:
  - obtain the modified copy locally from the original Anime Assembly link without bypassing access controls;
  - record filename/world folder;
  - record SHA-256;
  - inspect geometry and derive metadata coordinates without editing/rebuilding the third-party terrain;
  - re-audit permission if the project ever moves beyond private/local play.

### Matter Overdrive — MOBA Map

- Status: `FALLBACK`
- Source: https://www.curseforge.com/minecraft/worlds/matter-overdrive-moba-map
- Version: 1.7.10
- Listed map license: Public Domain
- Value: legally cleaner backup arena and historical lane-spawner reference.
- Risk: very old world/runtime; conversion needs proof.

### League of Legends 1.12.2 playable map

- Status: `FALLBACK / TERMS AUDIT REQUIRED`
- Source: https://www.planetminecraft.com/project/league-of-legends-in-minecraft---playable-3479252/
- Advertised scope includes turrets, inhibitors, minions, shop, respawn, jungle and objective systems.
- Do not import files/code until actual terms are verified.

### Other Summoner's Rift replicas

- Status: `REFERENCE ONLY / LOCAL-ONLY UNTIL TERMS PROVE MORE`
- Never infer redistribution rights from download availability.

---

## 6. Missing-screen UI assets

Anime Assembly already owns much of the visible in-match/player layer. The project still needs external final visuals for map select, asymmetric team setup and result/reset if the donor lacks suitable screens.

### Kenney UI Pack family

- Status: `PRIMARY FREE VISUAL FALLBACK`, not yet final-accepted
- Sources:
  - https://kenney.nl/assets/ui-pack — 430 files, CC0
  - https://kenney.nl/assets/ui-pack-rpg-expansion — 85 files, CC0
  - https://kenney.nl/assets/pixel-ui-pack — 750 files, CC0
- Use mode: directly usable/editable external UI assets.
- Rule: create one real Minecraft screen and compare its visual language with Anime Assembly before locking. If stylistically incompatible, reject and research another usable family rather than inventing project art.

### UI Lib / RPG-HUD / other UI frameworks

- Status: `RESERVE ONLY`
- A framework is not automatically needed because Anime Assembly already provides several visible screens.
- Add one only after a concrete missing-screen implementation demonstrates that Vanilla Forge Screen + adopted assets is insufficient or wasteful.

---

## 7. Alternatives deliberately not selected for the first slice

### Anime Limitless 1.1.0

- Status: `REJECTED FOR INITIAL STACK / OPTIONAL FUTURE LOCAL ALTERNATIVE`
- Source: https://www.curseforge.com/minecraft/mc-mods/anime-limitless
- File: `gojo-limitless-1.1.0.jar`, CurseForge file 8688244
- Platform: Minecraft 26.2 / Fabric
- Requirements documented by project: Fabric Loader >= 0.19.3, Fabric API, Java >= 25
- License: All Rights Reserved
- Advertised content: 128 characters, 1,786 techniques, bundled skins, transformation visuals, character list, broad combat features.
- Reason not selected: excellent character quantity and modern platform, but no equivalently documented integrated MOBA team/Ready/shop/minimap ruleset; ARR also prevents treating its implementation/assets as a permissive code donor. Using it first would require more project-owned MOBA architecture.

### Spell Engine

- Status: `REJECTED FOR INITIAL STACK / 26.2 FALLBACK`
- Source: https://github.com/ZsoltMolnarrr/SpellEngine
- License: GPL-3.0
- Reason: strong generic framework, but redundant when Anime Assembly is the selected full character/combat runtime. Do not install both by default.

### Matter Overdrive source

- Status: `FALLBACK`
- Source: https://github.com/simeonradivoev/MatterOverdrive
- License: GPL-3.0
- Reason: historically relevant minion/team behavior but much older and more expensive to port than SimpleLaneWars + SmartBrainLib.

### `lol-minecraft`

- Status: `REFERENCE ONLY — NO LICENSE FOUND`
- Useful scope: modern Minecraft MOBA architecture reference.
- Rule: no copying source/assets/substantial implementation without an actual license grant.

---

## 8. Loader/version decision

The audit now conditionally chooses **Forge 1.19.2 / Java 17** for the first playable slice.

This is not permanent loyalty to Forge or to an old Minecraft version. It is a direct consequence of external reuse value:

- Anime Assembly provides the broadest coherent ready-made MOBA player layer found so far;
- every dependency explicitly listed by Anime Assembly now has an identified Forge 1.19.2 build target;
- SmartBrainLib has a dedicated 1.19.2 Forge module;
- LoM and SimpleLaneWars are source ports and therefore do not force a loader.

The version/file identification phase is no longer the blocker. The next blocker is empirical: the pinned donor profile must actually boot and expose enough stable integration state.

Re-evaluate platform only if M0 proves this stack unusable or a stronger legally reusable complete donor appears.

---

## 9. Source-admission checklist

For every source that actually enters development, record:

- exact source URL/repository;
- author;
- exact license/usage terms;
- exact version/commit/file/file ID;
- dependency vs port vs local asset vs reference;
- modification permission;
- redistribution permission;
- attribution/notice requirement;
- public-Git eligibility;
- game/loader/Java version;
- dependencies;
- exact subsystem ownership;
- exact adapter/port scope;
- conflicts with other adopted systems;
- SHA-256 for downloaded runtime bytes where practical.

## 10. Next action — no gameplay coding before this

1. obtain the exact pinned donor JARs from their original sources and record SHA-256 fingerprints;
2. build and boot the donor-only Anime Assembly runtime profile on Forge 1.19.2;
3. run the full donor feature checklist, including character select, four skills/additional skill, health bars, team behavior, all-player Start, M shop and minimap;
4. inventory real Anime Assembly symbols/resources and write the bridge symbol map;
5. check client plus dedicated-server/multiplayer viability to the extent supported;
6. obtain the local-only modified MOBA map from its original Anime Assembly link, record folder/checksum, and derive metadata without redistributing the map;
7. only then bootstrap `moba-arena` source following `IMPLEMENTATION_BLUEPRINT.md`.
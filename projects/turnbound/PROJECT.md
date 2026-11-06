# TURNBOUND — Rebuild project contract

## Identity
- Project: `projects/turnbound/` (NOT `projects/turnbound-re/`).
- Mod ID / namespace: `turnbound`; Java package `io.github.q93503128.turnbound`.
- Minecraft Java 26.2; NeoForge 26.2.0.62; Java 25; Gradle 9.2.1.
- This is the *existing TURNBOUND technical project with a new player-facing game*, not a migration from TURNBOUND: RE.

## Current explicit design decisions (2026-11-05)
- **All previously authored player characters (P01–P08, F01–F04), character kits, quests, story, enemy/boss content, item lists, map/NPC placements and Drehmal-centered gameplay are retired.** No old content has approval as the new baseline.
- The new goal is a playable exploration-oriented turn-based RPG in Minecraft, integrating **exploration/story + tactical turn battles + collection/progression**. Neither narrative rush nor prolonged repetitive grinding should dominate.
- New characters, equipment, quests, regions, story and combat content are co-designed with the user. The assistant must not independently choose identities, abilities, rarity assignments or quests as settled canon.
- Codex is primarily assigned visual asset production, UI/UX implementation, animation, VFX, audio, world building and actual Minecraft screen verification. Gameplay logic is designed together and implemented/reviewed separately.
- Do not start creating story, roster, NPCs or equipment while the content-free technical foundation is not fully ready.

## Engineering foundation to preserve (subject to actual code/CI verification)
- Fixed-point 1000-Gauge turn scheduler and player-decision pause.
- Generic combat damage/healing/target/effect/status/cooldown mechanics; cleanly separate remaining hardcoded legacy character rules.
- Server-authoritative battle session ownership, network validation, shared-party/reconnect and idempotent reward settlement.
- Versioned campaign persistence and explicit legacy-save safety; old saves must never be silently rewritten into an unrelated new roster.
- Data-driven authoring, safe empty-content behavior and runtime/renderer checks before new content is approved.
- Existing gameplay details and numeric balances are **not** binding merely because corresponding Java or test fixtures exist.

## Frozen reference and workbranch
- Last verified legacy commit: `1877b9ef59c86d8e8271e43e0b1f6b8657dc174f` on `turnbound-alpha17-full-v04`. Treat this as the recoverable legacy implementation, not a live design source.
- Content-rebuild branch: `turnbound-content-rebuild`.
- Recovery reference includes legacy source, artwork, existing saves' schema and old design docs in Git history. Do not force-push or delete the reference branch.
- The working dependency/removal audit is `CONTENT_REBUILD_AUDIT_2026-11-05.md`.
- `MULTIPLAYER_DESIGN_v1.md` is historical technical guidance requiring revalidation. `EXTERNAL_ASSETS.md` and `AUDIO_ASSETS.md` remain as prior-asset provenance; they are not new design authority.

## Current implementation checkpoint
- Legacy world/quest/gameplay lifecycle, Drehmal auto-installer, old HUD/camera boot path and initial content activation have been detached from the active client/server entrypoints.
- All 27 old authored `data/turnbound` JSON files have been moved from `src/main/resources` to `src/test/resources`; historical tests can use them, **released JARs cannot**.
- The production catalog `data/turnbound/rebuild/foundation_v1.json` deliberately contains zero characters, enemies, quests, equipment, encounters and regions. Runtime initialization validates this.
- All 223 former `assets/turnbound` files (models, textures, animations, audio and legacy UI) have moved to test resources. The rebuild JAR excludes them. Registrations for retired actor entities, mounts, held items and old sounds were removed from the active mod entrypoint; associated legacy renderer subscribers and presentation isolation subscriber have been detached. The legacy JourneyMap marker plugin and old automatic audio playback tick were also disconnected.
- Legacy Java classes **still compile into the JAR**. Their dependency-safe physical removal remains unfinished; do not claim complete source cleanup.
- Current blank foundation is not a playable RPG. Minecraft client gameplay and multiplayer were not verified for this rebuild.

## Validation
- Build TURNBOUND #1049 (rollback verified): Gradle build/tests PASS; dedicated-server smoke PASS; JAR inspection PASS; Modrinth pack validation PASS. This validates the restored runtime code plus production-data/asset removal.
- CI run: https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/41980378906
- JAR from this run is a foundation artifact, **not a released playable game**. Never claim it was client playtested.
- The one-click rebuild pack no longer downloads or repairs Drehmal, and should be installed separately from the old production instance.

## Next executable cleanup gates
1. Remove remaining authored-content Java classes and stale dependency chains, preserving the generic battle, network and persistence engines.
2. Verify no unwanted static/event registry callbacks reactivate old content; retain required third-party asset license records.
3. Confirm new-game empty state and block accidental use of old player save state; prove no content/world mutation at client and server bootstrap.
4. Validate meaningful code units with relevant tests, one integrated build per checkpoint, dedicated-server smoke, and eventual client/multiplayer playtest.
5. After foundation verification, jointly agree on new game systems, region flow, characters, equipment and quests.

## Quality
- Gameplay completeness over feature count. Systems must tie into a meaningful loop.
- Commercial references for important UI and animations, with licensing documented.
- No no-op compatibility hacks, deletion of still-relevant tests just to pass CI, fake APIs, or hidden save loss.
- Distinguish CODE REVIEWED, TESTED, BUILD VERIFIED, PLAYTESTED, MULTIPLAYER TESTED honestly.

## Additional cleanup checkpoint (2026-11-05)
- Detached the last identified legacy world-level auto-subscriber `FieldEncounterPresentationBridge` to prevent obsolete encounter-marker polling and actor replacement.
- Removed old summon source portraits and presentation images (18 PNGs, approx. 37.4 MB) and their unused generation script from the active workbranch. Original bytes remain recoverable from Git history; asset provenance was moved to `legacy/ASSET_PROVENANCE_summon.txt`.
- Historical NeoForge/Minecraft Java classes are still retained in the main compile source set until their shared runtime dependencies can be properly extracted. Do NOT move these classes into the ordinary JUnit `test` source set: Build #1048 failed because that source set lacked the transformed game API. The attempted move was completely rolled back in #1049.

## Empty rebuild campaign boundary — 2026-11-05
- A fresh empty rebuild runtime is now explicitly opt-in through `CampaignProgressStore.startEmptyRebuildGame`; it never replaces an already loaded player runtime. The old `ensureNewGame` and legacy v1–v6 reader remain historical paths for existing tests and recovery, not rebuild game entrypoints.
- The new save codec uses schema **7** with `saveKind=turnbound-empty-foundation`, rejects authored content and reads no legacy save as a rebuild. Storage is reserved under the distinct NeoForge `rebuild_campaign_profile` attachment; the legacy `campaign_profile` remains untouched.
- Until server login/logout and migration policy are separately implemented and playtested, the new rebuild attachment has **no automatic read/write lifecycle**. No current work claims end-to-end persistence in a launched client.
- New rebuild profiles contain no characters, party, rewards or currencies. Legacy gacha calls are rejected for those runtime instances; legacy engine/test fixtures remain available for historical regression tests.

## Legacy encounter boundary — 2026-11-05
- Explicit empty rebuild runtime refuses retired solo/training/encounter battle creation before UI/world transitions. Shared battle creation refuses if any participant has an empty rebuild profile. The historical combat engine and its existing tests remain untouched for future generic extraction.
- The current battle engine still has dozens of hardcoded old ability/character/boss rules. This guard is safety isolation, **not** proof that the generic engine has been extracted or that a playable new battle exists.

## Protected server session and arithmetic extraction — 2026-11-05
- The only active server gameplay hooks are now the rebuild profile login, 100-tick dirty autosave, logout, and server-stop lifecycle; no world builder or old quest/encounter entrypoint is re-enabled.
- The login admission gate refuses any existing `campaign_profile` attachment or legacy JSON/.bak file. A conflicting legacy + rebuild save also fails closed; no automatic old save migration, quarantine, reset or overwrite is attempted. New profiles are allocated only with no old data and persist to `rebuild_campaign_profile` (NeoForge attachment `copyOnDeath`).
- Schema-7 malformed rebuild saves cause disconnect rather than destructive reset. Client restart/respawn, actual multiplayer, and old-world visual safety remain NOT PLAYTESTED. Legacy worlds must not be used for rebuild playtests.
- Damage arithmetic is factored into content-independent `CombatMath` and the old `BattleEngine.calculateDamage` delegates to it for compatibility. The remaining legacy character/boss rules in BattleEngine are still present and must be addressed as an entire combat-behavior extraction, not by deleting tests.
- Old `CampaignPersistence` is preserved for archival tests but not registered for this rebuild. A future authored-content schema needs a separate reviewed storage migration from the strict empty schema 7.

## Verified implementation checkpoint — Build TURNBOUND #1059 (2026-11-05)
- Source HEAD `f19710f2469519c6c53a01b36c31aedd365e3cf6` passed Gradle test/build, dedicated NeoForge server smoke, JAR contents and Modrinth one-click pack verification: https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/41987155431
- The previously disconnected legacy world/bootstrap, authored catalogs and art remain inactive. The rebuild now automatically manages its **own separate empty-profile attachment** on server login, dirty autosave, logout and server stop, while refusing legacy/conflicted data; NeoForge `copyOnDeath` and identity-bound owner transfer cover the intended respawn path.
- The original battle resolver now delegates generic target selection (`BattleTargetResolver`) and damage arithmetic (`CombatMath`); legacy boss/character-specific logic remains in historical classes and is not approved new-game design.
- CODE REVIEWED: yes. TESTED (JUnit): yes. BUILD VERIFIED: yes. JAR PRODUCED: yes. DEDICATED SERVER SMOKE: yes. CLIENT PLAYTESTED: **no**. MULTIPLAYER TESTED: **no**.
- **Next code-removal unit:** analyze and break source/test references to old world builders, quest/NPC classes and character-specific battle handlers before physical Java removal. Do not move Minecraft classes into the ordinary JUnit test source set (Build #1048 failure). Preserve old-save snapshots, historical tests, and generic networking/settlement semantics. Client login/restart/death and multi-owner server sessions require real playtests in a separate NEW world before release.
- Do not co-design new roster/map/story or commission Codex assets until this boundary is confirmed through client verification and the remaining legacy code separation has been deliberately reviewed.

## Blank-production gate — 2026-11-05 / Build TURNBOUND #1062
- Verification source commit: `1a901f9237a84828f113b3ba0a89d7bdec42b0b0` with [Build TURNBOUND #1062](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42014459244) PASS (Gradle tests/build, NeoForge dedicated-server smoke, JAR inspection, MRPACK inspection).
- `tools/verify_rebuild_boundary.py` now checks the empty authored-content production manifest and detects any auto-registered legacy Java subscribers. It also verifies the runtime JAR has no old authored asset/data files or retired Drehmal installer/world-repair class bytecode.
- The retired world downloader, world-loading overlay and resource/compatibility repair implementation are excluded from **the shipped JAR** through Gradle packaging. These historical Java source files remain compiled for existing tests; do not mistake packaging exclusion for complete physical code removal.
- Content-first milestone: **BLANK BUILD/SERVER BASELINE VERIFIED**. This is suitable for agreeing on the new player-facing game's direction, but NOT certified as PLAYTESTED, client-load-safe or multiplayer-safe.
- Remaining technical cleanup: break historical compile/test references to old world, NPC/quest UI and character-specific battle Java before physically retiring source files. Do not move NeoForge Java classes into the standard JUnit test sourceSet; the failed #1048 already demonstrated an invalid classpath.
- Next real-world acceptance: install the one-click artifact in a completely separate Minecraft 26.2 test instance, create a fresh world, observe no legacy onboarding/download or terrain writes, log in/reconnect/respawn and confirm the empty rebuild attachment safely persists. Test multiple connected players separately. NEVER use the old world/save as the first playtest.

## Java source cleanup checkpoint — 2026-11-05
- removed 49 disconnected, untested-and-unreferenced retired Java classes, including the dormant Drehmal auto-installer and obsolete user-facing field/facility/summon UI, without removing any historical JUnit test source. Source recovery is guaranteed by the frozen legacy branch.
- Exact retired source names are tracked under `tools/retired_legacy_java_classes.txt`, with both Java-source and packaged-bytecode CI regression checks.
- Remaining old Java modules have compile-time dependencies on preserved combat/session/world protocols or historical tests, and must be isolated deliberately before deletion.
- CI success on this checkpoint verifies a blank-data/dedicated-server foundation, NOT Minecraft client launch, player-login persistence, or multiplayer gameplay. The production-ready-for-co-design decision must distinguish this from client-playtest readiness.

- Build #1064 revealed a compile-time reference from retained unused `BattlePlayerRenderPolicy` to retired `SummonCameraController`. Both were disconnected from the live and test roots. Removed `BattlePlayerRenderPolicy` too rather than retaining a broken reference or replacing behavior with a stub. Total isolated Java files removed: **49**.

## Blank-world manual acceptance gate — 2026-11-05
- Build TURNBOUND #1065 PASS after physical removal of 49 isolated old Java classes, retaining all existing tests and generic core systems. Source SHA: `3fc091ca58b2f6d8e2bc0c50333379e69e2ae20e`.
- Fresh Minecraft client acceptance procedure and evidence checklist: `REBUILD_FRESH_WORLD_ACCEPTANCE.md`. Actual fresh-world login, restart, death and multiplayer remain **NOT TESTED**, regardless of any headless client-boot result.

## Verified client window and remaining gameplay gates — 2026-11-05
- [Client Boot Smoke #2](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42018332635) PASS: headless Linux client created a Minecraft window after the TURNBOUND mod registered. Client-only JourneyMap from the modpack was staged for the dev-run environment. This is CLIENT BOOT SMOKE VERIFIED, not PLAYTESTED.
- **Blank technical base now verified through:** Build TURNBOUND #1065 (JUnit/build/dedicated server/JAR/mrpack), source and runtime legacy-data guard, physically retired 49 unreferenced legacy Java classes, and headless client window creation.
- **Still gated before full ready-for-play claim:** real client fresh-world entry, logout/rejoin, restart, /kill & respawn and actual two-person multiplayer; source-reference cleanup for the remaining old world and character-specific BattleEngine branches. Do not claim legacy code fully removed.

## Approved party size and gacha direction — 2026-11-06
- **Confirmed:** Player-avatar-centric hybrid survival/turn combat. Starting play is player-driven, with companion acquisition via **gacha**, not mandatory difficult exploration or recruitment quests.
- **Solo active-party capacity: 1 player + up to 4 companions = 5 combat participants.** The four slots EXCLUDE the player.
- The prior 'very difficult/rare companion recruitment' proposal is superseded.
- Gacha draw acquisition sources, currencies, odds, rarities, duplicate handling, guarantees, character roles and co-op limits are undecided. No paid draws were requested.
- This is design documentation only, **not an implemented five-participant party or working gacha**. Existing historical battle capacity and the empty save schema require later implementation and testing.
- The details and open choices are in NEW_GAME_CONTENT_DIRECTION_DRAFT.md.

## Updated confirmed solo party and starter choice — 2026-11-06
**OVERRIDES the earlier 2026-11-06 player-plus-four-companion entry above.**
- User shifted combat back to the original party-only format: **up to 4 gacha character actors**, **player avatar excluded from turn combat**. Player remains responsible for vanilla-world exploration, mining, building, crafting and survival.
- **First-start choice: exactly one of three normal native ★3 characters.** They are normal characters, NOT exclusive starter classes, not stat-boosted, and not permanently inaccessible to gacha. The two unselected choices remain available from regular character acquisition.
- Characters' names, role kits, new star rarity ceiling, gacha probabilities, starter UI and saved first-choice/one-time claim mechanics remain unimplemented and require design/verification. No old retired characters automatically reused.
- HISTORICAL evidence (legacy `turnbound-alpha17-full-v04`): `PROJECT.md`, `02_BALANCE_RULES_v1.md`, `03_CHARACTER_DESIGN_v1.md` defined **native ★1–★5**. The proposed ★6 promotion/Awakening relationship remained unresolved, and old `CanonicalData.levelCap` supporting an integer 6 is **not proof of approved ★6 native rarity**.
- This is only a planning/documentation checkpoint; do not claim any starter selection or populated four-character roster currently functions in the released empty foundation.
- Refer to `NEW_GAME_CONTENT_DIRECTION_DRAFT.md` latest selected combat direction section for the current design; superseded historical sections remain for traceability, not authority.

## Approved native rarity range — 2026-11-06
- **Native / born rarity is ★1, ★2, ★3, ★4 or ★5 only.** There is no native ★6 gacha rank.
- Native rarity and any future upgrade/promotion/awakening tiers are distinct concepts. A ★6 promotion, sixth display star or Awakening path is **not approved** by today's native rarity decision; decide separately if ever needed.
- The player avatar is outside turn-based combat. The solo combat party holds up to **four playable characters**, obtained by gacha except for the first choice.
- On first start, choose **one of three ordinary ★3 characters**. No special starter-only power, extra rarity or exclusivity; all three are ordinary characters available through the normal pool.
- No rarity probabilities, duplicate conversion, star growth curves, new character identities or starter-specific role kits have been approved.
- These decisions are currently **DESIGN ONLY**. The blank production catalog remains empty and the game does not yet include the new roster or selection UI.
- **Next recommended design unit:** field-to-turn encounter entry/exit, avatar vulnerability/protection while directing the character party, party defeat and survival consequences, including multiplayer/exploit boundaries. Then first starter trio roles and the gacha economy. No code change required for native rarity documentation.

## Confirmed encounter presentation — overworld field battle (2026-11-06)
- **CONFIRMED: Option A**. Encounters transition into turn-based battles **in the same actual vanilla-generated overworld terrain where the enemy was met**, with authored TURNBOUND character/enemy models and a dedicated battle camera/UI. This is NOT a separate instanced arena or teleport to a dedicated battle dimension.
- Up to **four playable gacha/starter characters** fight; the player's Minecraft avatar is an overworld explorer/director, **not a turn-based combatant**.
- Preserve the surrounding survival world and other players' simulation. A battle session must not pause the entire world. Camera placement, slopes/cliffs, obstacles, entity positioning and post-battle state restoration require testing on actual terrain.
- The exact **encounter trigger** (attack, enemy aggression, interaction or proximity), participant/world-external damage handling, avatar vulnerability/immobility, retreat, enemy interference and multiplayer participation rules are **not yet approved**.
- This is a design choice only. The current content-empty build has **not implemented or playtested** this new field encounter flow. Legacy world-combat source must not be treated as approved behavior without review.

- Next co-design decision: specify the **encounter trigger** without making ordinary Minecraft movement or resource gathering produce unavoidable repetitive battles. Then specify avatar protection/retreat and multiplayer authority before implementing a complete field battle.

## Confirmed R_PG-style visual contact and low-friction traversal — 2026-11-06

### User-confirmed encounter trigger (CANON DIRECTION)
- Reuse the older TURNBOUND **R_PG-style overworld enemy reaction**, not the recently offered attack/interact/proximity-selection alternatives.
- A visible hostile field monster **notices the player**, displays a readable **`!` alert**, briefly reacts, and **chases**. **Physical contact triggers a 4-character-party turn-based battle on the current terrain**. No separate preparation menu and no special battle-interact key.
- Escape should remain meaningful: a chasing monster disengages if the player gets far enough / breaks pursuit conditions and returns to its patrol or territory.
- **Critical user concern:** if the overworld becomes thick with mobs, ordinary travel must not force constant, frustrating turn battles. Traversal comfort is a design acceptance requirement, not an optional polish item.

### Anti-frustration measures — recommended for review, NOT numeric canon
1. **Spawn policy by area:** Roads, starting points, settlements, bases and ordinary routes should have safe travel/breathing stretches; dangerous ruins, dungeons and optional hostile territories can have more pressure. Do not populate every section of every road with a forced battle.
2. **Fewer representative field groups:** One visible field creature/group can represent a multi-enemy turn encounter, so battle complexity does not require equivalent counts of chasing field entities. Avoid overlapping aggro groups that chain player into several battles.
3. **Avoidance readability:** Enemies should be visible before contact when plausible. Alert is a brief warning, not instant teleport into combat; line-of-sight and bounded chasing make circumventing monsters legitimate.
4. **Retreat and re-aggro:** Chases end when leaving territory or losing pursuit. Returning creatures have a brief re-alert grace; avoid permanent endless chase, including across base/settlement boundaries.
5. **After-battle breathing room:** A defeated field group must not instantly respawn/retrigger on the same spot. Other nearby enemies should not ambush a player in the immediate transition back from the battle UI. Exact respawn/cooldown behavior must be validated for fairness and farming balance.
6. **Ambient versus hostile wildlife:** Although user wants all vanilla living mobs replaced, **not every authored animal should auto-aggro**. Distinguish peaceful/neutral wildlife from aggressive opponents so gathering and traveling remain possible.
7. **Multiplayer and authority:** One player/group cannot claim multiple overlapping battle sessions; server adjudicates contacts, parties, return-to-field positions and collision with spectators. The rest of the server world must not pause.

### Historical implementation reference (NOT approved new numeric balance)
- Legacy branch `turnbound-alpha17-full-v04`: `world/DrehmalFieldEncounterPolicy.java` records the older R_PG reference sequence: see in field → alert beat → chase/contact → battle, with no pre-battle menu.
- `world/FieldEncounterRules.java` used PATROL/ALERT/RETURN and, historically, 10-block alert, 15-block disengage, 18-block home leash, 2.6-block contact and 60-tick return grace. Those are historical code constants **not automatically the new game settings**.
- The old `ENCOUNTER_ENEMY_PLACEMENT_v1.md` explicitly forbade monster corridors, repeated roadblocks, invisible battle triggers and rapid nearby respawns, and permitted field representatives to stand for a multi-opponent battle. **Do not transfer old Drehmal-specific map, mobs, routes or coordinates.**

### Still open before coding
- Where/how new hostile ecology spawns in the vanilla-generated overworld, including light levels and player-built safe areas; tuning maximum simultaneous nearby hostile groups and aggro frequency.
- What counts as escape when climbing, swimming, flying, mining or crossing a region boundary.
- Field battle protection, third-party interference, post-combat positioning and how a player avatar (not a combat actor) is represented during battle.
- Whether several visible monsters join a single battle when clustered and what happens when multiple players contact a group.
- Exact detection, chase, respawn and grace numbers are **UNDECIDED**. Validate against real survival traversal rather than copying legacy 20–35-second common respawns.
- This is a **documented design decision only**; no hostile monster spawning or field contact battle has been re-enabled in the blank-build runtime.

- Gate for the first playable slice: a player must be able to travel normal routes without being repeatedly forced into consecutive turn battles; playtest for both avoiding enemies and intentionally engaging them. This is an experience requirement, not a promise that any actual density tuning has been tested.

## Monster ecology/spawning planning checkpoint — 2026-11-06
- User agreed to the next design step: preserve ordinary survival traversal with mostly peaceful/neutral custom wildlife, place sparse avoidable hostile field groups, and increase intentional combat pressure in caves/ruins/dungeons. All vanilla living creatures are intended to be replaced eventually.
- Design source: `OVERWORLD_ECOLOGY_SPAWNING_DRAFT.md`. Contains proposed ecology-zone roles, explicit anti-spam combat acceptance, vanilla-material replacement matrix, NeoForge spawn-gate approach and small vertical-slice milestones.
- Spawn density numbers/weights, actual new entity kinds, loot tables and dimensions remain **NOT APPROVED/NOT IMPLEMENTED**. No Java source/resources were altered, and no build/CI was necessary for this docs-only checkpoint.

## Confirmed peaceful-wildlife response and dungeon direction — 2026-11-06
- **Peaceful authored wildlife becomes a turn-based opponent if the player deliberately attacks it**, while normal unprovoked survival travel leaves it peaceful. Routine nonhostile breeding/feeding/resource interactions must not accidentally force combat.
- **Dungeons and ruins are planned as discoverable authored structures in ordinary vanilla-generated survival terrain**. They should be varied, contain meaningful encounters/rewards, preserve user builds and support same-world turn battle cameras and multiplayer authority.
- R_PG visual ! reaction, pursuit/contact remains the normal *naturally hostile* enemy trigger; deliberate hits on passive fauna cause the field battle without requiring a nonsensical chase before contact.
- `OVERWORLD_ECOLOGY_SPAWNING_DRAFT.md` records ecosystem, dungeon generation options, compatibility considerations and remaining design questions. This is a documentation decision, **not a new entity/dungeon implementation or playtest**.

## Confirmed preservation of TURNBOUND turn-combat system — 2026-11-06
**User decision: preserve the established TURNBOUND battle system; do not redesign the entire combat engine for the new survival-world game.**

Retain the proven generic combat contract from legacy TURNBOUND as a system, not old character content:
- **Four playable character actors maximum**, player avatar not a combat actor (latest party choice).
- **Deterministic fixed-point 1000 Turn Gauge scheduler.** When Gauge reaches 1000, action is available; each normal action subtracts 1000; overflow survives. SPEED/SPD influences future Gauge fill and thus action frequency, not merely initial turn order. Preserve natural successive turns when legitimately earned.
- **Battle logic pauses during manual player action/target selection**. Runtime actors, turn-order preview, and AUTO must use the same authoritative scheduler, not separate approximate systems.
- Keep **Basic (CD 0), active skill cooldowns, passive/reaction and other general effect mechanisms**, target validation, buffs/debuffs, healing, status/Gauge manipulation, and server-authoritative damage/settlement, as long as the generic implementation is still appropriate. Keep player decisions meaningful and avoid reaction recursion.
- Preserve the player-facing convenience design of **AUTO and 1x/2x presentation speed** without altering the actual underlying action economics. Actual initial AUTO policies and exact controls may need UI reimplementation/revalidation after the blank-content reset.
- Retain old generic battle tests and network/save safety boundaries; do not restore old cast-specific skill definitions, boss hardcoding, retired story/quests, old landscapes, retired 3D assets, or previous numerical balance by default.
- The user requested preservation of the **battle system**, not automatic reapproval of prior native character kits, duplicate progression bonuses, equipment stats, gacha probabilities, enemy lineup, playtime targets or world geometry.
- Implementation boundary: the current shipped rebuild is still an **empty playable-content foundation**. Most generic combat types survive but the live authoring/battle entrypoint is intentionally disabled and historical character-specific branches are present in source. Preserving a design is **NOT** proof of a working new-game field battle; an appropriate integration, test pass and actual Minecraft playtest remain required.
- Historical supporting reference: `turnbound-alpha17-full-v04:projects/turnbound/02_BALANCE_RULES_v1.md` sections 1–2, 21. Old 1000-Gauge and fixed-point `TurnScheduler`, plus action cooldowns and AUTO/1x/2x semantics, are reference behavior; old numerical tuning not new canon.
- Follow-up co-design: clarify how combat rewards/party progression integrate with vanilla survival and how a player avatar's world-state is protected during field battle. Do not move straight to building a detailed first region or entire roster before core integration decisions.


## Growth pillars retained for the new survival game — 2026-11-06
**User decision:** Keep **all four existing TURNBOUND growth pillars** in the redesigned Minecraft survival + party-turn-RPG: (1) character XP/levels, (2) gear/equipment, (3) awakening, (4) duplicate character rewards. Do NOT discard one for convenience or blindly reinvent the system.

### Preserved system shape (legacy baseline to carry forward)
- **Character levels:** retain battle/contract XP progression. Historical baseline: base Lv1–60, independent duplicate bonus +0–+10, effective maximum Lv70. The precise numeric curve and pacing are balance review items for the new survival loop.
- **Equipment:** retain the three ordinary character equipment slots: weapon, armor and accessory. Retain the concept of character-specific signature gear as a distinct optional slot/mechanic; it is not permission to automatically restore old signature items or character kits. Historical enhancement target: +0–+10 with no random failures or destruction. Specific stat values, Gold costs and recipes require adaptation to survival.
- **Awakening:** retain the once-per-character mechanical evolution concept; make it a meaningful kit option rather than only a flat stat multiplier. The old activation path depending on mandatory main-story or character-story quests is INCOMPATIBLE with the now-confirmed job-board/nonlinear game direction and must be redesigned (e.g. gameplay accomplishments/optional challenge), not silently copied.
- **Duplicate gacha characters:** preserve meaningful choice between **one additional bonus level** (up to the cap) **OR native-rarity Star Essence** per duplicate; not both. At the +level cap, Essence-only is the prior baseline. Preserve unclaimed selection state safely across save/reconnect. Whether old Essence conversion shop/costs are retained is a separate economy review; no mandatory duplicate copies to unlock a unit's core skills.
- **Native rarity remains ★1–★5 only.** A possible ★6 promotion/awakening relationship is separate and still undecided.

### Survival integration requirements (proposals to design, not approved numbers)
- Mine/forage/craft resources should help produce, enhance or trade useful **party** equipment. The Minecraft player avatar remains an explorer/crafter, not a combat actor, so do not accidentally transfer vanilla weapon damage into the turn battle as avatar damage.
- Turn battles and optional contracts award character XP and useful equipment resources; ruins/dungeons/bosses can provide signature/awakening-related objectives and rarer upgrade materials.
- Vanilla character experience, raw ores, survival resources and RPG progression must not spawn several duplicated, unnecessary currencies or repetitive processing steps.
- Existing fixed-point Gauge / party-only four-character combat is retained. A complete new-game save system for characters, equipment, duplicate pending choices and awakening will need an explicit migration from the current intentionally empty rebuild profile.

### Boundaries / verification
- The user confirmed retention of the **systems**. Old character names, kits, specific gear items, story unlocks, rarities per character, numerical XP/stat balance, drop rates, previous map and quests remain retired unless explicitly newly approved.
- Source references: frozen TURNBOUND `turnbound-alpha17-full-v04`, `02_BALANCE_RULES_v1.md` sections 9–11 and 16–17 plus prior `PROJECT.md` duplicate-choice policy. These are historical detailed baselines, not active new content.
- This commit updates **planning documents only**; there is no newly implemented party progression, awakening UI, equipment integration, or duplicate selection, and no build or playtest performed for this doc-only change.

### Next design dependency
- Before first-region production: decide the **resource/reward bridge** from ordinary survival mining, crafting, hunting, ruins, dungeons and NPC contracts into the four retained growth systems, ensuring neither survival nor tactical combat is made optional/pointless.

## Updated star-based level caps and forging direction — 2026-11-06

### User decisions: DO NOT silently revert
- **Current star rank sets the normal character level cap:** ★1 Lv10, ★2 Lv20, ★3 Lv30, ★4 Lv40, ★5 Lv50, ★6 Lv60. Formula: `baseLevelCap(currentStars) = 10 * clamp(currentStars, 1, 6)`.
- **Native rarity is ★1–★5 only** (gacha identity); the sixth star is an achievable *growth/promotion* rank, NOT a native ★6 gacha rarity. This resolves the earlier uncertainty about whether a ★6 growth rank is in scope, but does **not** determine promotion costs, eligibility, number of ascensions, star-stat modifiers or resource recipes.
- Prior legacy v1 active growth applied a flat base Lv60 and effective Lv70 when duplicate +level was counted; `CanonicalData.levelCap(stars)` still contained 10/20/30/40/50/60 as a largely unused legacy helper. **The user's latest decision supersedes that flat-cap interpretation** for the new game. Do not treat the presence of the old helper as proof that the new rank-based cap is already in live runtime.
- Keep previously approved duplicate acquisition bonus level OR Star Essence option as a growth pillar; **the stacking and UI semantics of duplicate bonus +0–+10 versus the new star-specific base-level limits remain to be reconciled before code and balancing**. Do not silently force a level-70 base cap, auto-award +levels or let duplicate bonuses bypass a promotion gate without an explicit rule.

### User-requested equipment direction
- **Change the primary normal gear acquisition loop from direct finished-equipment drops to material-based crafting at a forge/blacksmith.**
- Players obtain ore/metal and other useful materials through vanilla survival mining and exploration; authored TURNBOUND creatures drop distinctive components, while dungeon/elite/boss rewards offer rare special materials or recipe discoveries.
- **Better ingredients yield better or meaningfully more specialized equipment**, but the effect should be inspectable (visible recipe/preview), balanced and relevant to actual four-character turn combat rather than a random hidden power roll.
- Preserve existing character equipment identity (weapon/armor/accessory ordinary slots, optionally character-specific signature gear, and the already selected enhancement system). Do not automatically copy retired equipment identities, recipes, numerical stats or old direct-drop economy.
- Exact forge station type, whether it is NPC-operated/buildable, crafting formulas, ore-compatibility, item tiers, traits, required materials and upgrade costs are **proposed, not yet approved**.
- Full equipment design proposal and integration constraints: `SURVIVAL_FORGE_AND_STAR_GROWTH_DRAFT.md`.
- **This update is documentation only:** NO equipment recipes, forge UI, new materials, star-cap runtime, promotion or test/JAR is implemented or verified.

### Next design dependency
Decide forge workflow and how the basic survival materials / monster trophies produce weapon, armor and accessory options without excessive crafting steps; then reconcile level caps with duplicate bonuses and star promotion. Avoid switching to first-region production before these relationships are coherent.


## User-confirmed forge separation from vanilla Smithing Table — 2026-11-06
- **TURNBOUND gear forging is an independently registered custom forge/blacksmith workstation and recipe interface, separate from Minecraft's vanilla Smithing Table.** No repurposing or replacement of vanilla Smithing Table recipes, templates, armor-trim/smithing functions or player-survival inventory item behavior.
- The TURNBOUND forge creates/enhances **party character equipment** from survival ores and custom monster materials. Vanilla crafting/smithing remains useful for the Minecraft explorer's ordinary survival tools and armor.
- Allowing an NPC blacksmith in towns to use the same shared forge recipes is an architectural **proposal**; the exact station location, GUI, extra modules and whether the player can craft/place a forge remain unfinalized in detail. Keep the player's ability to access normal Minecraft crafting independent of TURNBOUND's equipment loops.
- Existing ★1–★6 rank level caps and duplicate +level cap-interaction are separate decisions. This separation does not resolve those mechanics.
- Documentation checkpoint only: there is no actual custom forge block, recipe serializer, Forge GUI or registered structure yet.


## Confirmed duplicate-level option A — 2026-11-06
- Latest user choice: a character's current star rank limits only base XP level: star 1=Lv10, 2=Lv20, 3=Lv30, 4=Lv40, 5=Lv50, 6=Lv60. Native rarity remains 1–5 stars; rank 6 is progression-only.
- Existing duplicate reward: choose either +1 bonus level or native-rarity Star Essence per copy. Bonus levels cap at +10 and accumulate separately from base XP.
- Effective combat level = base XP level + duplicate bonus level, even if that exceeds the current-star base XP cap. Example: star 1, base Lv10 + bonus 3 gives effective Lv13; star 6, base Lv60 + bonus 10 gives effective Lv70.
- Bonus levels do not independently promote star rank, increase the XP cap or substitute for star-promotion requirements. Promotion prerequisites, costs, exact stat curve and final UI display remain undecided.
- This supersedes earlier statements marking bonus-level/cap interaction unresolved, but does not approve any new runtime feature or assert testing. Documentation only.

## User-approved sequential star promotion — 2026-11-06
- **Promotion is available when a character reaches the CURRENT STAR RANK'S BASE XP LEVEL CAP** and the player spends the required **promotion materials obtained through monster encounters and dungeons**. This is an AND condition, not either/or.
- Rank grows **one star at a time** (★1→★2→★3→★4→★5→★6). All native ★1–★5 characters are eligible to grow eventually to ★6. No native ★6 draws.
- Each current star rank has an independent normal base XP ceiling: ★1 Lv10, ★2 Lv20, ★3 Lv30, ★4 Lv40, ★5 Lv50, ★6 Lv60. Example: ★3 Lv30 (+5 duplicate levels) may ascend to ★4 after paying the appropriate materials; its normal base XP ceiling then becomes Lv40.
- **Duplicate bonus levels do NOT satisfy the base XP cap requirement:** ★3 Lv25(+5) has effective combat level 30 but still cannot promote until its actual base XP level reaches 30.
- Promotion raises `currentStars` and the **base XP cap** but must not change immutable `nativeStars`, remove already-earned +bonus levels, or automatically restore old character assets/old balance.
- **No specific costs are approved yet**: promotion-material names, quantities, the split between ordinary monsters, elites and dungeons, any optional Gold component, future star stat changes, level/XP carry behavior and relation to once-only Awakening need separate design.
- Promotion should create meaningful mining/monster/dungeon gameplay without overwhelming inventory with one new currency per star or requiring repetitive mandatory dungeon grinding. Proposed UX/recipe implementation is not an approved final design.
- **DESIGN ONLY:** no actual rank-promotion screen, save schema or verified playable progression exists in the current empty-content build; no code/build/CI work was requested here.

## Confirmed summon guarantee and reward sources — 2026-11-06
- **Every 10-pull batch guarantees at least one native ★4-or-★5 character.** The batch may contain more high-rarity results naturally. A ★5 counts toward this guaranteed ★4+ slot; do not silently promise an additional ★4 on top of a ★5.
- **Every 50th individual draw is guaranteed native ★5**: 50, 100, 150, 200, ...; this is a **fixed milestone** guarantee, not a conventional pity counter that resets after obtaining ★5.
- **An early ★5 never resets or advances the fixed 50-draw schedule**. Example: ★5 on draw 27 still guarantees draw 50; ★5 on draw 49 still guarantees draw 50. Within a 10-pull, a scheduled guaranteed ★5 also satisfies the ★4+ batch guarantee.
- Both single and batch pulls contribute their actual individual draws to the same cumulative milestone count. The batch-specific ★4+ guarantee is explicitly associated with the **10-pull action**; no separate ten-consecutive-single-pulls ★4+ promise has been authorized.
- Gacha's possible native ranks remain **★1–★5**; rank ★6 exists only through character promotion. Initial starter choice is one of three ordinary native ★3 characters in the normal obtainable roster.
- **Summon currency comes principally from NPC/job-board contracts and dungeons.** Elite/boss first-clears and substantial discoveries are viable proposed secondary sources; exact payout amounts and whether repeat-clears award summon currency remain undecided.
- Prior frozen legacy 65-soft/80-hard pity and any ★5-on-early-draw reset logic **DO NOT apply** to this new approved scheme. Do not copy old numerical summon odds or 300-crystal costs without a separate explicit balance decision.
- Retain earlier duplicate character handling: reward choice of a +1 duplicate bonus level (up to +10) OR native-star essence, unless later explicitly changed.
- Still unapproved: native-rarity probability weights, exact summons cost, currency/item names, how/where the UI opens, cross-pool/banner account rules if multiple pools ever exist, any additional ★4 guarantee on singles, and precise repeatable reward anti-grind policy.
- Save the per-player cumulative draw count and enforce reward settlement server-side when implementing. Never award guaranteed draws twice on interrupted transactions. These are design/implementation criteria, **not evidence that summons are live**.
- This is **documentation only**, not new summon code, resource drops, tests, server runtime, JAR or playtest. Full draft: `SUMMON_GACHA_AND_REWARD_ECONOMY_DRAFT.md`.


## Confirmed native draw probabilities and flexible initial roster size — 2026-11-06
**Latest user decision, overriding earlier unresolved odds and provisional market comparisons:**

| Native rarity | Base chance per non-guaranteed draw |
| --- | ---: |
| ★1 | 15% |
| ★2 | 25% |
| ★3 | 40% |
| ★4 | 15% |
| ★5 | 5% |
| **Total** | **100%** |

- These are the **base-rarity odds before applying guarantees**. The 10-pull guarantees at least one ★4+ in that batch; cumulative individual draws #50, #100, #150, ... guarantee a ★5 regardless of earlier lucky ★5 outcomes. The exact server draw/guarantee algorithm must preserve the promised minimum and not downgrade a naturally or milestone rolled ★5.
- The initial **overall character roster size and the number per rarity are intentionally NOT fixed**. Character identities, designs, role balance, assets, skills, and available pool counts should evolve with content production and playtest. Do not manufacture filler units merely to fill an artificial quota.
- The first-start selection remains exactly one from **three ordinary ★3 candidates**, all available from the normal summon pool. This fixed starter-choice count does NOT set the total number of ★3 characters.
- Low rarity must have purposeful roles and an attainable promotion path to growth rank ★6; native ★5 should offer appealing identities without making low-rarity strategic choices irrelevant. Exact individual kits and raw stat/power budgets remain to be designed.
- Main summon resources principally come from optional contracts and dungeons. **Exact summon cost and currency pacing are STILL undecided.**
- This is a **planning-only decision**. GachaService is not connected to the rebuilt empty-runtime playable content; no code/build or gameplay tests were run.


## Field battle failure, possible flee, contracts and dungeons — 2026-11-06
### User-confirmed gameplay outcomes
- **Party defeat:** return the Minecraft explorer avatar to the most recent applicable checkpoint. There is **no defeat penalty**: no inventory loss, dropped tools, Gold/gacha-gem deduction, XP/character level loss or equipment destruction. Defeat is a battle outcome, **not a vanilla player-death/drop event**.
- A failed battle grants no victory loot, contract kill credit, dungeon-clear status or one-time clear bonus. This distinction is not a punishment or preexisting resource deduction; it simply does not award a victory.
- Checkpoint activation, initial fallback, boss-specific checkpoint placement, recovery of the defeated party's HP and the non-teleporting behavior in multiplayer require design before implementation. No loss of progress or overworld building is intended.
- **Contracts:** the player wants contract difficulty to influence **Gold and gem payouts**. Gems are principally for character summoning, and Gold is principally for other RPG services/equipment/transactions, but the exact currency uses and award rules remain **flexible, not fully fixed**. No amount or difficulty rating has been chosen.
- **Dungeons:** discovery should be intrinsically rewarding, with worthwhile loot; they are one of the major gem-earning content sources. Dungeon layout, duration, repeatability, difficulty, resource drops and payout numbers have not been fixed.

### Recommendations under discussion — NOT yet user-approved details
- **Probabilistic flee:** the user raised escape chance because free, guaranteed fleeing would trivialize world navigation. Recommend a chance-based escape attempt: on success, exit at the encounter location; on failure, the attempt costs an action/opportunity and the enemy can pressure the party. Do not set a numeric percentage until tested. Whether bosses disallow fleeing or modify odds remains open.
- **Retry-loop protection:** a small escape probability alone cannot prevent unlimited escape attempts, so failed attempts should create actual time/turn pressure; escaping should not award clear rewards or allow a bypass of required boss/encounter victories.
- **Combined NPC + quest-board workflow:** local boards list repeatable regular jobs, while villagers/other named NPCs can offer memorable or specialist tasks. Both use the same underlying server-owned contract records, journal and payouts to avoid duplicated systems. This is preferred over making the player browse several disconnected interfaces.
- **Reward directions:** first dungeon clears should feel valuable; later repeats can still provide materials and useful progress, but do not lock all gems behind one endlessly repeated easy dungeon. Exact first-clear and repeat payouts remain to be designed, not an approved economy.
- **Protect the overworld avatar during battle:** since the Minecraft world does not globally pause and the player is directing four characters rather than fighting as an avatar, define the avatar's physical vulnerability to mobs, hazards, other players, world changes, disconnect and death; server authority must prevent exploits and double settlements.
- **Checkpoint exploit consideration:** loss-free defeat returning to a checkpoint might be used as free fast travel. Assess its impact in real play; do not introduce an unrequested item or currency death penalty as a workaround.

### Existing technical reference and scope
- On current source, `BattleSessionManager.command` treats `FLEE` as an unconditional session end when `fleeAllowed()`, not a probabilistic retreat action. `BattleOutcome` currently has `RUNNING`, `ALLY_VICTORY`, `ENEMY_VICTORY`; fleeing is not a modeled outcome. Adding chance/failure costs requires real gameplay changes, not merely a value tweak.
- Old `RegionalContractService` demonstrates NPC-delivered repeatable region tasks with Gold/party XP, but it is legacy authored gameplay and is NOT active new-game content. Its architectural ideas may be reused after review, without importing its old map, NPCs, payout values or forced story requirements.
- This checkpoint records **content direction only**. No battle defeat checkpoint teleport, flee probability, new NPC or noticeboard, dungeon spawn/rewards, or gem settlement is implemented/tested in the rebuilt empty content.

### Next co-design unit
- Pick a **contract acquisition UX** (proposed combined noticeboard + NPC), then detail dungeon exploration decisions and checkpoint placements before setting reward amounts or summon-currency pacing.


## Confirmed unified contract acquisition — 2026-11-06
- **Both village noticeboards and resident NPCs issue contracts.** Noticeboards are the principal access point for routine/open contracts; residents/NPCs can offer personal, place-specific or special contracts.
- They share **one contract system**: common objective tracking, acceptance/completion state, player journal and authoritative reward settlement. Do not create parallel unrelated quest economies or duplicate credit just because two sources expose a contract.
- Main story is not required; contracts remain optional activities in the Minecraft survival + turn-battle loop.
- Reward direction already chosen: difficulty-sensitive Gold and summon gems; exact payout amounts, difficulty brackets and available job count remain open. Gold primarily serves other progression/services, gems primarily serve character summons, without permanently freezing every possible use.
- **Unresolved content design:** job categories and objectives, contract generation/availability, delivery and hand-in rules, limits on concurrent/recurring jobs, cancellation, dungeon integration, NPC interaction details and any exceptional reward cases. Do not manufacture a fixed initial contract count.
- **Design documentation only**: NPC/board contract UI, world registrations, new rewards and multiplayer state are not implemented or playtested.

## Graded stochastic contracts and dungeon quest discovery — 2026-11-06

### User-chosen direction and user proposals
- **Replace reward stereotypes by objective type with an independent quest grade/difficulty axis.** Gold and summoning gems are awarded according to contract difficulty/grade and actual challenge; killing, collection, exploration and dungeon tasks are not permanently Gold-only or gem-only categories. Exact grade labels, bracket counts, drop tables and pay amounts are not fixed.
- **Ordinary contracts** may originate from either village noticeboards **or suitable villager NPCs**; **special contracts** originate only from NPCs or special world interactions/events, not the ordinary noticeboard listing.
- **Both ordinary and special contracts should have probabilistic occurrence/availability.** Villagers that offer a contract should vary across world-generated villages; no universal permanently fixed quest-giver list, no requirement that every villager offers work.
- User prefers **not** to create a separate category of identically repeatable contracts. Distinguish each completed/consumed individual contract from future newly generated contracts. Infinite copies of a single trivial request should not become the primary gems economy.
- **Dungeons are worthwhile independently of quests**, and they are a primary source of summoning gems. Whether a dungeon-related quest points toward its location from a resident or begins via an NPC/event at the entrance is a **question still under co-design**, not a resolved user requirement.
- The player's previous acceptance of unified quest tracking and NPC/noticeboard acquisition still applies; the newer special-contract exception supersedes earlier blanket statements implying every quest may appear on either surface.

### Design recommendations (NOT user-approved binding specifics)
- Represent grade independently from mission kind; calculate grade using actual threat, travel/risk, dungeon difficulty and required effort. Higher grade should improve the expected total value of Gold and gems, not necessarily require identical rewards for every mission. Do not substitute a fixed drop value based solely on objective type.
- For grade naming, E/D/C/B/A/S is only an illustrative UI scheme; **no letters or tier counts are approved**.
- Create ordinary contracts as **persisted instances** with objective, region, giver/source, grade, designated targets, completion and claim state. Generate new instances from region-appropriate event/templates when world conditions allow; never re-roll an existing offer simply by clicking an NPC repeatedly, rejoining or breaking/replacing a board.
- A special contract may appear with a low but nonzero chance after an eligible world occurrence (expedition clue, rare encounter, dungeon lead, NPC incident), rather than on every random villager conversation. Keep sufficient authored variety and meaningful stages so mere random parameter swaps do not masquerade as content.
- Avoid a dedicated daily/weekly contract reset or requiring the exact same completed job over and over. **Economic caveat:** if one-off jobs are finite, a primary summoning currency runs out. Continually generated *new distinct contract instances* are a candidate solution, but their quality and supply need playtesting. No specific respawn timer, generation rate, cap or guaranteed minimum is approved.
- **Dungeon-related lead distribution:** recommended majority mode is a villager/contract informant describing a rough region, landmarks or route to a **real existing world-generated dungeon**, allowing exploration and discovery. A rarer entrance-site clue or distressed NPC can initiate a **special event/quest** at the dungeon itself. Avoid one generic NPC standing in front of every dungeon. A dungeon remains enterable and rewarding if found without a quest. These are suggestions, **not yet agreed user decisions**.
- Quest-to-dungeon pointers MUST resolve to a real generated structure rather than pointing to a nonexistent or already exhausted dungeon; per-player multiplayer clear semantics and worldgen chunk availability need implementation design.
- Reward stability: no gem duplication from multiple quest-giver surfaces, overlapping objectives, board refreshing, villager multiplication, reconnection or repeatedly completing the same persisted instance. Per-player completion and multiplayer target ownership require explicit rules.
- Design the objective verbs and actual player choices before finalizing numerical gem/hour expectations or assigning fixed counts to quest types, NPCs or dungeons.

### Supporting game comparisons, not implementation copies
- *Monster Hunter: World* investigation quests can be generated from monster traces/activities, relate to a field area and provide better rewards with difficulty. Japanese/Korean game tutorial transcription: https://mhw.poedb.tw/kor/panel_tutorial ; community investigation overview: https://monsterhunterwiki.org/wiki/MHWorld/Quests .
- *The Witcher 3* links noticeboard contracts to clients and discoveries; notices can direct players to locations. Reference catalog: https://witcher-games.fandom.com/wiki/Notice_board and https://witcher-games.fandom.com/wiki/The_Witcher_3_contracts .
- These are **design references**, not approvals to copy private code/content or assets.

### Implementation state and next gate
This is a **design document change only**. No procedural NPC offers, board generation, special events, quest grade/reward engine, dungeon lead system, new state persistence or gem settlement exists in the new-game empty runtime yet.
Before implementing, co-design **the contract lifecycle**, **frequency/eligibility by region** and **dungeon discovery** through actual in-game choices; keep all numerical payouts and probabilities open.


## Dungeon exploration design reference checkpoint — 2026-11-06
- Deeper dungeon design research and **unapproved proposed gameplay structure** recorded in `DUNGEON_GAMEPLAY_STRUCTURE_REFERENCE_AND_OPTIONS_DRAFT.md`.
- External inspirations: YUNG's Better Dungeons (recognizable layered themed structures), When Dungeons Arise (discoverable spectacle with authored modular pieces), Vault Hunters (varied dungeon objectives); do not copy code/assets or claim current TURNBOUND 26.2 compatibility.
- **No new dungeon mechanic has yet been user-approved by this research alone.** Next co-design pivot: whether every dungeon needs a boss or only the major ones; preserve dungeon discoverability without mandatory quests, meaningful gems and reward ownership, loss-free defeat, and same-world party turn battles.
- Design/documentation work only, no Java changes or tests/build/playtest.

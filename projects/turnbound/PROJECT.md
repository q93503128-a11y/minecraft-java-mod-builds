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

# TURNBOUND content-rebuild dependency audit — 2026-11-05

## Freeze, identity and status
- Source: q93503128-a11y/minecraft-java-mod-builds, original branch turnbound-alpha17-full-v04.
- Recovery baseline SHA: 1877b9ef59c86d8e8271e43e0b1f6b8657dc174f. The last prior #1043 build, dedicated server smoke, JAR and MRPACK passed; Minecraft client and multiplayer not tested for that SHA.
- Working branch: turnbound-content-rebuild (new branch, never TURNBOUND: RE). Target: Minecraft Java 26.2, NeoForge 26.2.0.62, Java 25, mod id turnbound.
- This audit is a targeted SOURCE REVIEW. No old content has yet been deleted, no rebuild runtime has been verified, no new JAR has been produced.

## Actual source inventory at frozen SHA
- Java main classes: 372. client 88; combat 29; command 1; content 12; network 14; presentation 23; progression 9; session 20; root 3; world 173.
- Java test files: 177. Historical assertions tied to obsolete content must not be deleted merely to make CI green.
- Resources: 27 files under src/main/resources/data/turnbound and 224 files under src/main/resources/assets.
- Main data: 12 playable characters, 12 matching character-menu profiles, 28 enemies, 59 encounters, 3 tutorial bridge encounters, 30 Rift floors, 24 equipment entries, 8 signature items.
- Old quest sources: quests/v04.json 18 main + 8 character + 12 region + 20 challenge; avsal_v1.json 18 quests; drehmal_first_route_v1.json 9 quests; regional_contracts_v1.json 15 contracts. These sources have separate loader paths and may overlap logically.
- Other catalogs: 6 regions, 25 passives, 95 skill IDs, 12 status entries; additional authored world placement, travel, NPC and terrain JSON.

## Blocking dependencies before any deletion
1. Turnbound.java / TurnboundClient.java: server/client registration, tick and login invoke old world and UI services; client still invokes Drehmal installation/repair. Never leave dangling callbacks or force a world download.
2. content/CanonicalData.java: static definitions read /data/turnbound/characters/v04.json and enemies/v04.json. Missing files throw; hardcoded runtime lookups assume those IDs exist.
3. content/V04Catalogs.java: eagerly loads encounters, equipment, Rift, regions, quests, loot, statuses, passives, skills; the loaders expect schemaVersion 4, some also require nonempty/known entries.
4. progression/GachaCatalog.java: old 12-character rarity pools and Starter guarantee baked into static maps. Empty roster requires an explicit unavailable-state policy; do not call RNG on empty pools.
5. world/CampaignProgressStore.java: old starter party P01/P03/P04/P08 seeded directly; quest reward, encounter and progression authority share the same player store. A fresh development profile needs a genuinely empty collection, without fake old heroes.
6. progression/PlayerProfile.java and world/CampaignSaveCodec.java: known character validation, growth, equipment, party and orphaned IDs are coupled. Legacy saves must not be silently overwritten, lost or remapped to invented new characters.
7. world/MetaMenuService.java + client menus: read old roster/encounters, quest and signature data at snapshot time. Empty catalogs and unavailable features need deliberate, informative client states.
8. world/ExternalWorldBootstrap.java: production behavior activates in Drehmal-bound world, runs field enemies, NPCs and path hooks; WorldSessionRouter / StarterSliceBootstrap still contain old Aster/Radia building paths. World entry must be migrated as a coherent lifecycle unit.
9. session/BattleSessionManager.java + RewardGrantService: battles and rewards use encounter IDs, field return flows and durable transactions. Preserve generic server authority and reconnect guarantees while replacing authored encounter routing.
10. Tests: 177 source files cover both pure engine invariants and old content. Change their contracts only when the underlying feature is intentionally retired; do not mass-delete tests or use no-op implementations.

## Retire content, preserve mechanics
- RETIRE: P01–P08 / F01–F04 identities and abilities, all existing quests/storylines/dialogue, equipment specs and signature effects, old mob/boss definitions, encounter/Rift maps, region NPCs and legacy UI art.
- PRESERVE WHEN VALID: TurnScheduler and deterministic gauge/turn rules; generic damage, target, effect and cooldown engine; battle/session ownership; multiplayer and server validation; durable save, idempotent settlement and return safety; transport codecs and tested reusable rendering/input machinery.
- REFACTOR: data catalogs to support a legitimate empty authored content set; new-game profile/party without old IDs; UI empty states; safe world entry that requires no Drehmal download; old-save quarantine/migration rules.
- WAIT FOR USER CO-DESIGN: new characters, skills, enemies, bosses, equipment, quests, progression and world-specific authored choices. Do not generate substitute NPCs or quests as placeholders.

## Executable removal sequence (checkpoint-gated)
A. Recovery checkpoint DONE: original branch and frozen SHA preserved; new branch created.
B. Audit DONE to initial dependency-blocker level. Do not claim the code is decoupled yet.
C. Add honest tests for vanilla-safe game boot (no auto world download or terrain rewrite), empty profile, empty data, old-save preservation and server-authoritative action failures. Keep existing core tests.
D. Replace world/bootstrap registration, combat encounter adapter and profile/catalog assumptions in coherent functional changes; no comment-out/no-op or swallowed exceptions as a shortcut.
E. Remove now-unreachable authored world/NPC/quest and character/equipment/monster modules and associated resources, while preserving provenance in original Git commit. Update tests to the new functional contract.
F. Run targeted tests and one integrated Gradle/NeoForge server build at meaningful checkpoint; verify JAR internals. Client playtest and multiplayer are separate required labels.
G. Only after the clean foundation is technically verified, decide new area and character/quest/equipment plans together with the user and ask Codex for approved visual production.

## Current blocking risks
- Old roster IDs are in default save/party and multiple battle/visual rules.
- World startup runs external Drehmal bootstrap and client-side asset installer.
- Hardcoded sample encounters/rift floors and old quest unlocks are referenced from GUI, reward and services.
- Removing old assets without matching registry/render changes can break client load even if the dedicated server starts.
- Save identities are persistent; missing catalog entries must fail safely without destroying old player progress.

Validation: AUDITED STATICALLY; CONTENT REMOVAL NOT STARTED; BUILD NOT RUN FOR THIS BRANCH; CLIENT / MULTIPLAYER NOT TESTED.

## Foundation registration pass — 2026-11-05
- The mod entrypoint no longer runs old quest / world / player-shell tick/login handlers, field or meta commands, or old world building. Stable entity IDs, attachments, battle protocol and party protocol remain registered pending migration.
- The client entrypoint no longer invokes the Drehmal installer or repair path, old field/quest UI, loading screen, camera overrides, or HUD suppression. Generic battle/party/audio network registrations remain.
- The generated Modrinth pack no longer writes the Drehmal opt-in marker or advertises a world download; obsolete Elysia texture regeneration was removed from build tasks.
- Inactive legacy classes/JSON/resources remain in source for staged removal. This is not yet a physically empty content catalog, and no existing save is migrated or overwritten by the new entrypoints.
- Build/CI and actual Minecraft client confirmation must be recorded separately; an inactive entrypoint by itself is not proof of successful gameplay.

## First physical content removal — 2026-11-05
- 27 legacy content JSON files under `src/main/resources/data/turnbound/` have been relocated byte-for-byte under `src/test/resources/data/turnbound/` so historic tests can validate legacy logic without shipping those datasets to players. Source original is preserved in prior branch.
- `data/turnbound/rebuild/foundation_v1.json` is the new production catalog with **zero** authored characters, enemies, quests, equipment, encounters and regions.
- `RebuildContentCatalog` validates that all six production content arrays remain empty at mod initialization; this prevents accidentally loading unapproved auto-generated replacement content.
- Legacy Elysia .b64 input is test-only; `prepareLegacyHeroFixture` produces the historically expected PNG only in test resources, not in the final JAR.
- CI checks the shipped JAR does not contain any of the retired JSON namespaces. Legacy Java code and legacy art remain as staged cleanup tasks, not active gameplay.
- This phase does not wipe player saves. The old campaign loader is not invoked at login on the rebuild branch.

## Asset and legacy registry retirement checkpoint — 2026-11-05
- All 223 original `src/main/resources/assets` files relocated unchanged (same Git blob hashes) to `src/test/resources/assets`; previous commit and legacy branch remain recovery sources. Includes models, animations, textures, old UI graphics and music/SFX. Existing resource-level tests keep their historical fixture inputs.
- Active `Turnbound` entrypoint no longer registers P/F/enemy/boss entity types, signature actor types, Drabyel service actors, Roadhorn mount, legacy held weapons, or any retired sound event IDs.
- Legacy GeckoLib renderer auto-subscribers were disconnected in `TurnboundBattleActors`, `SignatureBattleActors`, `DrabyelServiceActors`, `TurnboundMounts`. `PersonalPresentationIsolation` global world event subscriber was disconnected too, to avoid affecting ordinary vanilla worlds.
- Classes remain for dependency-backed test contracts until the generic modules can be separated without compile-time or runtime breakage. These inert source classes are not considered new production content.
- JAR verification now refuses any `assets/turnbound/` entry, in addition to retired data namespaces.
- Preserved `META-INF/licenses` and `THIRD_PARTY` provenance. Existing character and world definitions still exist only as test fixtures.
- Client and multiplayer playtests remain NOT TESTED until explicitly performed; CI cannot establish appearance or world safety on its own.

## Client residual-hook retirement — 2026-11-05
- Removed `@JourneyMapPlugin` from the old world marker plugin so JourneyMap cannot auto-initialize the obsolete Drehmal/Aster marker layer. Future custom-map integration requires an explicitly reviewed new plugin.
- Disconnected `ClientAudioPlayback::onTick` from the client event bus. No retired soundtrack will replace vanilla Minecraft music in an empty development world.
- The client audio payload codec/handler remains registered for future server-authoritative battle audio, but there is no active authored battle/music producer in the blank-content runtime.

## Historical Java source-set migration failure and rollback — 2026-11-05
- An exploratory move of 79 now-disconnected Minecraft/NeoForge Java sources to the default Gradle `test` source-set failed in Build #1048. `compileTestJava` lacked the NeoForge/Minecraft transformed API classpath (first error: `com.mojang.brigadier` and `net.minecraft.commands` missing while compiling `TurnboundCommands`). Compilation of old main sources had succeeded before test compilation failed.
- All 79 source paths were restored unchanged (same Git blobs) into the NeoForge main source-set. Test source duplicates were removed. Historical tests and engine behavior remain intact.
- Moving client/runtime mod code directly into Gradle's ordinary test source-set is NOT a valid long-term archive strategy here without correct NeoForge test-source compilation support. For future physical removal, first decouple generic runtime dependencies and retain historical source via the frozen legacy branch or a separately configured archival source-set.
- Never represent this experiment as successful Java removal. The confirmed milestone is old authored data/assets removal from the production resource set, detached runtime registrations, and preserved core code.

## Raw summon art and field-event retirement — 2026-11-05
- After verifying the 79-class source-set migration was fully rolled back and Build #1049 passed, remove the retired raw 18 summon PNG sources (total 37,444,555 bytes) and the old `tools/generate_summon_assets.py` script. These art sources were not referenced by current Gradle build or CI because its generation step had already been removed.
- Preserve source/provenance attribution at `legacy/ASSET_PROVENANCE_summon.txt`; the previous code/image blobs remain in the frozen legacy Git commit and previous rebuild history. The new game must not reuse them without user review.
- `FieldEncounterPresentationBridge` still existed in Java main and declared a `@EventBusSubscriber` despite prior entrypoint removal. Detaching this annotation prevents automatic player-tick scanning or modifying obsolete encounter armor stands in ordinary worlds. Generic source retained because other current Java compilation units reference it.
- Verified prior rollback checkpoint: Build TURNBOUND #1049 PASS (tests/build, server smoke, JAR and pack inspection). The new raw-art/subscriber change requires a separate CI confirmation and has not yet been built as of writing this line.

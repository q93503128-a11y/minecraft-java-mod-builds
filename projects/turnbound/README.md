# TURNBOUND — Fabric 1.20.1 migration

**New approved game:** Drehmal v2.2.2f map-backed independent party turn RPG. Survival, mining, block placing and player health/hunger are excluded from gameplay. Existing generic TURNBOUND battle contract is preserved.

**Latest work status:** Fabric 1.20.1 battle foundation (including server enemy-turn handling) **BUILD VERIFIED #1077**. Official Drehmal 2.2.2f file analysis and copied-world automatic-entrypoint isolation/restoration **verified offline**, but full original game-script removal, real map interaction and multiplayer are **not tested**. This is not a playable game or an install-ready Drehmal package. Read [current technical contract](PROJECT.md), [Fabric battle session bridge](BATTLE_SESSION_FABRIC_BRIDGE.md), [Drehmal analysis](DREHMAL_DATA_PACK_SEPARATION.md), and [game design canon](GAME_DESIGN_CANON_INDEX.md).

The archived NeoForge sources are not built into the Fabric JAR; old world/save files are never converted automatically. Do not activate the marker on a real Drehmal save.

## Previous rebuild notes (historical)

# TURNBOUND — Content Rebuild

This branch is a **technical foundation** for a completely redesigned Minecraft Java turn-based RPG.

The old characters, quests, story, bosses, equipment and Drehmal-based gameplay are not part of the new game's design. They are preserved at the legacy commit `1877b9ef59c86d8e8271e43e0b1f6b8657dc174f` on `turnbound-alpha17-full-v04`, not silently reused here.

**Current state:** Old content no longer activates on startup. All 27 old authored gameplay JSON resources have been moved into test-only fixtures; the production manifest has an empty character/quest/equipment/enemy/region/encounter set. Legacy Java code and artwork still need dependency-safe retirement. This is **not yet a playable RPG**.

- Build stack: Minecraft Java 26.2, NeoForge 26.2.0.62, Java 25, Gradle 9.2.1.
- **Current game-design decisions / open questions:** [GAME_DESIGN_CANON_INDEX.md](GAME_DESIGN_CANON_INDEX.md) (read first for new-game planning).
- Technical project contract, baseline and engineering gates: [PROJECT.md](PROJECT.md).
- Dependency audit and removal gates: [CONTENT_REBUILD_AUDIT_2026-11-05.md](CONTENT_REBUILD_AUDIT_2026-11-05.md).
- Last documented blank-foundation build verification: [GitHub Actions Build TURNBOUND #1065](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42016753073); [Xvfb graphical boot smoke #2](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42018332635) passed (not a gameplay playtest).
- Validation: Tests/build, dedicated-server smoke, JAR and pack checks PASS; client gameplay and multiplayer NOT TESTED.
- The rebuild pack intentionally does not install or modify Drehmal. Do not replace the legacy JAR in an existing world without a separate save-migration decision.

High-level new-game world, combat, growth, gacha, contracts and dungeon design are now partially approved and cataloged in the current index; character identities, content lists, balance and implementation are not complete. Further design is collaborative. Codex handles approved visual/world work, not autonomous game lore or roster decisions.

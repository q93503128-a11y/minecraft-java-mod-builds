# TURNBOUND — Content Rebuild

This branch is a **technical foundation** for a completely redesigned Minecraft Java turn-based RPG.

The old characters, quests, story, bosses, equipment and Drehmal-based gameplay are not part of the new game's design. They are preserved at the legacy commit `1877b9ef59c86d8e8271e43e0b1f6b8657dc174f` on `turnbound-alpha17-full-v04`, not silently reused here.

**Current state:** Old content no longer activates on startup. All 27 old authored gameplay JSON resources have been moved into test-only fixtures; the production manifest has an empty character/quest/equipment/enemy/region/encounter set. Legacy artwork is absent from the production JAR, and 79 disconnected Java classes are test-only. Remaining legacy Java dependencies still need extraction and removal. This is **not yet a playable RPG**.

- Build stack: Minecraft Java 26.2, NeoForge 26.2.0.62, Java 25, Gradle 9.2.1.
- Canon: [PROJECT.md](PROJECT.md).
- Dependency audit and removal gates: [CONTENT_REBUILD_AUDIT_2026-11-05.md](CONTENT_REBUILD_AUDIT_2026-11-05.md).
- Last validated foundation: [GitHub Actions Build TURNBOUND #1045](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/41977185198).
- Validation: Tests/build, dedicated-server smoke, JAR and pack checks PASS; client gameplay and multiplayer NOT TESTED.
- The rebuild pack intentionally does not install or modify Drehmal. Do not replace the legacy JAR in an existing world without a separate save-migration decision.

The new characters, equipment, quest content, and world will be planned jointly with the user after the generic foundation has been checked. Codex handles approved visual/world work, not autonomous game lore or roster decisions.

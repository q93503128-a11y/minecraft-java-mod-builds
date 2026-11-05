# TURNBOUND — blank-foundation fresh-world acceptance

## Scope
This document verifies that the content-rebuild distribution starts as a **normal blank Minecraft 26.2 world** without retired TURNBOUND characters, quests, Drehmal world installation, forced adventure mode or old field/HUD onboarding. It does **not** approve a new game design or new authored content.

Current verified technical baseline: `3fc091ca58b2f6d8e2bc0c50333379e69e2ae20e`, Build TURNBOUND [#1065](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42016753073) PASS for Gradle/JUnit, NeoForge dedicated-server smoke, JAR and Modrinth pack. 49 disconnected old Java classes were physically removed; remaining legacy combat/world classes are not approved game content.

## Isolated client setup
1. Download the **turnbound-content-rebuild-foundation** artifact from Build #1065. Use `TURNBOUND-oneclick-0.1.0-alpha.17.mrpack` inside it to create an entirely **NEW instance** with Minecraft Java **26.2**, NeoForge **26.2.0.62**, Java **25**. GeckoLib is bundled and JourneyMap is referenced by the pack.
2. Do not merge into an old profile or reuse any TURNBOUND/Drehmal save directory. Existing saves are intentionally not migrated and may trigger a protective disconnect.
3. Start the client. **PASS:** title screen reaches normal Minecraft UI; no TURNBOUND world installation/repair window, download, world selection hijack or crash. **FAIL:** frozen/black screen, forced installer, exception or missing dependency.

## Fresh world and save lifecycle
4. Create a new disposable world in Survival mode with commands enabled. **PASS:** vanilla terrain, vanilla inventory and movement, no TURNBOUND character/quest/field UI, no teleport, forced Adventure mode or spawn edits.
5. Walk, collect one vanilla item, note position. Save and Quit, reopen this same world. **PASS:** same world/player state, no protective disconnect or exception.
6. Close the game completely, launch it again, reopen this world. **PASS:** normal login, no reset-to-new-game behavior, no malformed rebuild-profile error.
7. In this disposable world use `/kill @s` and respawn. **PASS:** normal vanilla death/respawn; the rebuild has no extra roster/quest content, and no duplicate runtime / attachment error. Vanilla item loss under standard gamerules is expected; do not mislabel it as a save regression.

## Multiplayer acceptance
8. With the exact same modpack, join a fresh disposable server with **two clients**. Confirm independent join, survival movement, logout, reconnect and one death/respawn for each player. **PASS:** neither player's session or world state resets, no duplicate UUID ownership error, no old content/world activation, no unintended battle start.
9. A legacy TURNBOUND world is **not** a valid test world for this branch. Preserve it separately; the rebuild should refuse old campaign data instead of overwriting it.

## Evidence and acceptance boundary
- Save `logs/latest.log` from the newly created instance and a screenshot of the normal spawn. For multiplayer, also retain server `logs/latest.log`. A crash report or `latest.log` with the first error is needed for any failure.
- A separate Linux/Xvfb development-client window check runs at [TURNBOUND Client Boot Smoke](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/workflows/turnbound-client-boot-smoke.yml). It confirms only graphical client boot, not fresh-world playability.
- Do **not** label the client PLAYTESTED until items 3–7 have been observed in a real client. Do **not** label MULTIPLAYER TESTED until item 8 has been observed with two real clients.
- Content design discussion can begin on the verified **blank build/server baseline**, but game-play readiness requires these real-client checks.

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

# Augmentation Survival — Final Audit

Date: 2026-10-29 (Asia/Seoul)
Audit baseline: `main` at `588943b26b024e95a3a49e6da048723a73798862`
Most recent gameplay-code checkpoint: `3ecf999d36a3c067a77d6a3ff87aa5fe7657577e`
Status: STATIC FINAL AUDIT; MANUAL PLAYTEST AND MULTIPLAYER VALIDATION REMAIN OPEN

## Scope and source of truth

Fetched `origin/main` before review and fast-forwarded the local `main` from `8c2946ca6` to the fetched head. Preserved the pre-existing untracked root file `TURNBOUND_CANON_REGRESSION_AUDIT_2026-09-28.md`.

Read the repository `README.md`, `AGENTS.md`, `docs/BUILD_STANDARD.md`, `docs/QUALITY_STANDARD.md`, `docs/QUALITY_STANDARD_GAME_DESIGN.md`; project `PROJECT.md`, `AUGMENTS.md`, `AUGMENT_INTERACTION_CONTRACT_2026-10-24.md`, `AUGMENT_CANON_CORRECTION_2026-10-27.md`, `AUGMENT_CANON_DELTA_2026-10-26.md`, `IMPLEMENTATION_STATUS_2026-10-24.md`, `IMPLEMENTATION_STATUS_2026-10-25.md`, `GAME_DESIGN.md`, `HUD_AND_PLAYER_TEXT_STANDARD.md`, `MODPACK_BASELINE.md`, Gradle configuration, selected runtime data contracts and source entry points, plus the GameTest source inventory and selected high-risk test files.

The named `MOONSEUNGJUN_MINECRAFT_HIGH_QUALITY_PLAYBOOK.md` and a separate shared dev-playbook were not present in the repository or the searched local `.codex` and `Documents` trees. The repository quality standards and the project-specific contracts were applied. No connected shared dev-playbook source was available in this workspace.

Checked the pinned toolchain coordinates against the official Fabric Maven: Loader `0.19.5`, Fabric API `0.160.0+26.2`, and Loom `1.17.20` exist; Fabric's 26.2 metadata specifies Java 25 or later. Newer compatible patch releases were also published (`0.161.0+26.2` API and Loom `1.17.21`). The currently pinned versions were retained because the project has a recent successful checkpoint and no dependency regression was found. Sources: [Fabric Loader Maven](https://maven.fabricmc.net/net/fabricmc/fabric-loader/0.19.5/), [Fabric API Maven](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.160.0%2B26.2/), [Loom Maven](https://maven.fabricmc.net/net/fabricmc/fabric-loom/1.17.20/), [Fabric 26.2 metadata documentation](https://docs.fabricmc.net/develop/loader/fabric-mod-json).

## Findings and changes

### P1 — Current project status and acquisition metadata were stale

`PROJECT.md` still described the project as 129/146 complete, listed 14 augments as having no runtime, directed the next implementation pass to work on those entries, and listed now-fixed augment parameters as undecided. Its modpack section also said core loot integration was absent. The active acquisition contracts had `runtime_active:false`, although their registered services load and use them; that field is descriptive metadata, not a runtime gate in their parsers. Set `core_distribution_contract.json`, `boss_core_reward_contract.json`, and `core_source_profiles.json` to `runtime_active:true`. The current canonical Markdown and bundled copy specify 149 entries; the JSON catalog and `AugmentCatalog` enforce 149 entries with tier counts 20/27/31/41/20/10. The two canonical Markdown files are byte-identical. The implementation status files dated October 24 and 25 and `GAME_DESIGN.md` retain their earlier 146-entry snapshots.

Updated `PROJECT.md` and the current status section of `MODPACK_BASELINE.md` to reflect the 149-entry runtime baseline, identify the dated 146-entry status/design files as historical snapshots, remove resolved mechanics from the undecided list, and record the supplied latest #431 verification checkpoint. Set the three active contract status flags to `true` to match their runtime consumers. Kept historical checkpoints intact. Added this report and linked it from `PROJECT.md`.

### Prior CI finding corrected — the workflow does run GameTests

The prior audit incorrectly inferred that the checked-in workflow did not execute GameTests. Build #431 (run ID `40795138452`) logs `:compileGametestJava`, `:processGametestResources`, `:gametestClasses`, and `:runGameTest`; all 74 required tests passed. The workflow finding is closed. This follow-up records the supplied run evidence and did not rerun the workflow or GameTests.

No gameplay defect was confirmed in the source paths inspected for the high-risk damage, healing, movement, projectile, active, rescue, and reroll interactions. Static inspection cannot establish that all 149 effects feel correct in a live game.

## 149-augment catalog and runtime alignment

- Catalog: 149 IDs; tier totals match the Java catalog constants and `AUGMENTS.md`.
- Runtime wiring inventory: 142 augment IDs have direct Java source references; the remaining seven unconditional stat augments are declared in `stat_modifier_contract.json` and consumed through the generic stat runtime. This is a source-wiring check, not proof that every effect has been exhaustively validated in play.
- Presentation: `CanonicalCoverageGameTests` checks 149 distinct icons. The roster source and bundled copy match byte-for-byte.
- Direct GameTest ID inventory: 72 distinct roster IDs occur literally in GameTest sources. Other tests exercise generic loops/services, so this is not a count of untested augments. It does show that the suite does not provide an obvious individually named effect assertion for every entry.
- Tuning: the interaction contract and current data-driven contracts were checked for the reviewed cases, including stat modifiers, final augment parameters, shield/delayed damage, Last Stand, Shield Burst, Life Wave, double jump, aerial combo, and tracking arrow. This audit did not independently compare every numeric field in every tuning JSON against every player-facing tooltip.

## Interaction and quality review

| Area | Static review result | Live validation |
|---|---|---|
| Projectile graph | Recent contract and projectile regression tests cover shot/action behavior, tracking, return-to-sender and physical shield throw. No confirmed defect in reviewed paths. | NOT RUN in this audit |
| Healing / protection / lethal | Reviewed actual `hurtServer` assertions for delayed damage, shield pools, invulnerability, Last Stand ordering, healing share recursion and Life Wave radius. Helper-only assertions are also present, but key cases include real hurt/heal paths. | NOT RUN in this audit |
| Movement | Double jump preserves/reboosts momentum; aerial combo is tested without Double Jump; temporary platform, water run, wall slam, mantle, flight and traversal have focused tests. | NOT RUN in this audit |
| Server authority / state | Catalog/acquisition and dedicated-input tests exist; the gameplay contract assigns final results to the server. Full packet-by-packet, save/reload, disconnect and reconnect review was not completed here. | NOT RUN in this audit |
| External compatibility | Reviewed profile data and project compatibility contracts. Full live encounters with every listed external mod were not run. | NOT RUN in this audit |
| UI / VFX / audio | Text rules and presentation/source wiring reviewed; no game screenshot or sound audition was available. | NOT RUN; visual/audio completion not claimed |
| Performance | Searched tick registrations, broad entity queries and UUID-keyed runtime maps. This was a structural scan only; no profiler evidence was collected and map cleanup was not exhaustively traced across all services. | NOT RUN |
| Player-facing text | Targeted search found debug strings only in the bundled World Bosses namespace language files; no confirmed internal audit labels were found in the inspected augmentation text. | Localization audit was reported PASS in #431 handoff |

## Build and verification record

Build Augmentation Survival Pack #431 (run ID `40795138452`) identifies `3ecf999d36a3c067a77d6a3ff87aa5fe7657577e` as the latest gameplay-code checkpoint. Its job log shows `:runGameTest` completed with 74 required tests passed. Build, localization audit, dedicated-server smoke, client boot smoke, and verified Modrinth pack production also passed. The current `588943b26b024e95a3a49e6da048723a73798862` head differs from that gameplay checkpoint by documentation synchronization, according to the handoff and commit history.

This follow-up did not rerun build, CI, or GameTests; it corrected audit documentation and synchronized three descriptive `runtime_active` values, which are not runtime gates in the parsers. All three edited JSON resources were parsed successfully. No local JAR, SHA-256 file, or raw log archive was produced during this follow-up. The current verified pack was not downloaded into the workspace.

| Check | Result |
|---|---|
| Java / Gradle version check this turn | NOT RUN |
| `clean build` this turn | NOT RUN |
| Datagen | NOT RUN; project says not configured |
| GameTests this turn | NOT RUN; Build #431 run `40795138452` executed `:runGameTest`; 74 required tests PASS |
| Dedicated server smoke this turn | NOT RUN; #431 PASS per handoff |
| Client boot smoke this turn | NOT RUN; #431 PASS per handoff |
| JAR inspection / SHA-256 this turn | NOT RUN; #431 pack reported verified |
| Manual playtest | NO |
| Multiplayer test | NO |

## Remaining review and player validation

1. Run the packaged client in Minecraft and review the device/card flows, passive readability, proc cues, boss HUD overlap, long Korean/English strings, several GUI scales, and the current VFX/audio in actual play.
2. Exercise the full 149-augment roster and interaction-heavy builds in survival play; validate balance, feedback clarity, and edge cases not represented by GameTests.
3. Test two-player ownership, shared storage, companion/rescue behavior, projectile ownership, cooldown isolation, and reconnect/save-reload cases on a dedicated server.
4. Review persistent UUID-map lifecycle cleanup and performance with a profiler under representative multiplayer load.

## Final labels

- CODE REVIEWED: YES — static review at the listed baseline; remaining scope is stated above.
- TESTED: YES — prior GameTests PASS (74 required tests, #431 run `40795138452`); NOT RUN during this follow-up.
- BUILD VERIFIED: YES — prior #431 PASS; build NOT RUN during this follow-up.
- PACK PRODUCED: YES — prior verified Modrinth pack per supplied handoff; not downloaded here.
- CLIENT BOOT TESTED: YES — prior #431 PASS; client boot NOT RUN during this follow-up.
- PLAYTESTED: NO.
- MULTIPLAYER TESTED: NO.

## Current source-level follow-up — 2026-10-29

This section supersedes the earlier head and verification labels for the follow-up work below; the preceding audit remains as a historical record.

- Fetched `origin/main` before continuing. Current starting `HEAD` and `origin/main`: `47f1319253ef4512c333c33762e820b307eff638`; branch: `main`. The supplied older SHA `588943b26b024e95a3a49e6da048723a73798862` was not the fetched remote head.
- Rechecked repository instructions, README, Build Standard, Quality Standard, project `PROJECT.md`, `AUGMENTS.md`, and interaction contract. The named Minecraft playbook was not found in the repository; previous local-tree search also found no shared dev-playbook source.
- Current project source/config inventory: 447 files (364 Java, 75 JSON, 2 Gradle, 2 properties, 1 replant function, 1 Python tool, 2 wrappers). The path-level record is [`CODE_AUDIT_2026-10-29_INVENTORY.csv`](CODE_AUDIT_2026-10-29_INVENTORY.csv). At this checkpoint, 249 files are reviewed, 198 remain unread, and 5 reviewed entries require runtime/path validation. All 137 mixin sources have been read. Therefore the full file-by-file audit is **not complete**.
- Confirmed and fixed seven source defects: projectile flight-state save/reload, stale acquisition-offer cleanup, client card-presentation mixin registration, unused boss pull/swap profile controls, cross-dimension domination command-target UUID reuse, stale replant fallback ownership, and local powered-flight pose false positives when no-gravity comes from another source. The client pose change still needs in-game visual verification.
- Verified the three active runtime metadata contracts (`core_distribution_contract.json`, `boss_core_reward_contract.json`, `core_source_profiles.json`) are all `runtime_active: true`. Their Java parsers do not use that field as a runtime gate.
- Test-source review found direct `AugmentHealingService` calls instead of the primary game heal boundary, event-invoker tests for some root-kill behaviors, and policy-helper-only coverage for the boss pull/swap control. Marked these tests for runtime/path-strength validation; did not weaken or remove them. Other source and asset entries remain unread, so catalog-to-runtime coverage and the broader UI/resource review are incomplete.
- `git diff --check`: PASS. JSON parse check: 74 project main-resource JSON files, 0 invalid (PowerShell parsed with `-AsHashtable` to support valid empty-string blockstate keys).
- Local Java 25 / Gradle 9.5.1 `clean build` is BLOCKED before compilation: Fabric Loom `1.17.20` resolution from Fabric Maven ended with `Unsupported or unrecognized SSL message`. Current changes' GameTests, server/client boot, manual play, JAR inspection, SHA-256, and pack production have not been run. Historical Build #431 remains 74/74 GameTests PASS with build, localization audit, dedicated-server smoke, client boot smoke, and verified Modrinth pack PASS at gameplay checkpoint `3ecf999d36a3c067a77d6a3ff87aa5fe7657577e`; those results do not validate this follow-up's code.
- After the first push, Build Augmentation Survival Pack #436 (`40808323457`) compiled main Java successfully but failed `compileGametestJava` on the missing `ServerLevel` import at `DominationPlaytestGameTests.java:56`. The import has been added locally; a following CI run is required before claiming test/build success. The failed run skipped GameTest execution, dedicated server/client smoke, and pack production.
- Current follow-up labels: `CODE REVIEWED: NO` (198 unread); `TESTED: NO` for these changes; `BUILD VERIFIED: NO` for these changes; `PACK PRODUCED: NO` this follow-up; `CLIENT BOOT TESTED: NO` this follow-up; `PLAYTESTED: NO`; `MULTIPLAYER TESTED: NO`.
- Commit/push and parent-folder rename have not yet been performed. The unrelated untracked root audit file remains outside this project-only change.

### CI follow-up — Build Augmentation Survival Pack #437

- Commit `50cb291255164bd88ac5275d14f32b9b489a5898` compiled main and GameTest Java successfully on GitHub Actions (Java 25.0.5+8, Gradle 9.5.1).
- `:runGameTest` then failed one required test: `domination_playtest_game_tests_command_target_cannot_cross_dimension_damage_boundary`. The first actionable failure was `AssertionError: owner command target leaked across dimensions to an unrelated companion` at `DominationPlaytestGameTests.java:69`.
- This exposed an incomplete guard in the prior fix: target lookup checked the target dimension but omitted the attacking companion's dimension. `mayDominatedMobDamage` now requires the stored command dimension to match both attacker and target. The existing cross-dimension regression test exercises this actual damage authorization boundary.
- Build #437 skipped localization capture/audit, dedicated server smoke, client boot, and verified Modrinth pack because GameTest failed. It uploaded only a partial diagnostic artifact, ID `12869815005`; this is not a verified pack.
- Follow-up fix is not yet built or tested. Build verification, GameTests, dedicated server/client smoke, JAR inspection, SHA-256, and pack production remain NO for the current code until the next CI run succeeds.

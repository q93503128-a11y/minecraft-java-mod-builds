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

### Successful verification — Build Augmentation Survival Pack #438

- Commit `09852ced15db96cb1fd157a5853d5f47b80449b7` was built by GitHub Actions run `40808710842` / run #438: PASS.
- Java 25.0.5+8 and Gradle 9.5.1; `clean build`, `inspectPackDependencies`, and `verifyPackRuntimeClasspath`: PASS.
- All 77 required GameTests passed, including the cross-dimension command-target damage boundary regression.
- World Bosses localization contract (789 keys), external Korean localization audit (24 namespaces, 4,165 English entries, 0 missing), dedicated server smoke, client boot smoke, and verified Modrinth pack generation: PASS.
- Modrinth pack SHA-256: `c0f1fdcbd06b4aa296d4630815d1abc6f116f7f3aadc5577625a43fcff7cd3ea`.
- Workflow artifact `augmentation-survival-pack-smoke-438`, ID `12869825395`, uploaded successfully (3,330,134 bytes; archive SHA-256 `833c07f890faaf33f885a82db47aeabf62c3f864bac694f8f1651d4668e2e463`). Download: https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/40808710842/artifacts/12869825395 . The artifact contains the workflow outputs; no JAR or pack was downloaded into the local workspace.
- CI now verifies build, tests, server/client boot, and pack for commit `09852ced`. Manual play, multiplayer play, and profiling remain NOT RUN. The broader file-by-file source audit remains incomplete: 249 reviewed, 198 unread.

### Final labels for commit `09852ced`

```text
SOURCE FILES FOUND: 447
SOURCE FILES REVIEWED: 249
SOURCE FILES UNREAD: 198
FULL SOURCE AUDIT COMPLETE: NO
CODE REVIEWED: NO (file-by-file audit incomplete)
TESTED: YES (77/77 required GameTests, workflow #438)
BUILD VERIFIED: YES (workflow #438)
PACK PRODUCED: YES (verified Modrinth pack; artifact 12869825395)
DEDICATED SERVER SMOKE: YES (workflow #438)
CLIENT BOOT TESTED: YES (workflow #438; boot smoke only, no visual review)
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
PROFILER: NOT RUN
```

## Full file-by-file source audit and defect fixes — 2026-10-29

This section records the continuation after Build #438. It supersedes the preceding “FULL SOURCE AUDIT COMPLETE: NO” label for **static file review only**; build and live validation remain separate.

- Completed the tracked project source/config/resource inventory and included the two new Java persistence files: 467 distinct entries, 0 duplicate paths, 0 unread files. Status counts: 421 reviewed with no issue, 25 reviewed with issue fixes (including prior fixes), 18 reviewed but still requiring runtime/path validation, and 3 historical/test-only files. The production JAR verifier added for final packaging is included in this inventory.
- Reviewed the project Java, Gradle/wrapper configuration, game-test sources and metadata, resource contracts, recipes, damage types, tags, models, item/block definitions, internal and external language assets, and authoritative `AUGMENTS.md`. The 18 runtime-validation entries are the GameTest/runtime-sensitive cases, Better Combat reflection and flight behavior, and screens that still need actual Minecraft rendering and interaction checks.
- Confirmed 8 additional defects and applied fixes: temporary platform expiry records were not persisted across restart; reactive combat runtime data could survive player/session transitions; revive markers mixed dimension-local clocks with the Overworld clock; interrupted food-use snapshots could survive player departure; gigantification cleanup could raise a dying player's health; time-stop boundary state could outlive its player/server; passive-proc debounce timestamps could suppress effects after reconnect/world restart; and spatial-link search/page/snapshot selection changes could leave the rename field bound to an earlier gate name.
- Temporary platform expiry/owner records now use a persistent Overworld attachment. A codec round-trip regression was added; an actual world save/reload and delayed dimension reload scenario remains NOT RUN.
- Added cleanup for player/server lifecycle maps and unified revive marker comparisons on the Overworld game clock. Spatial-link selection changes now refresh the selected-name field.
- Resource checks: all 74 main-resource JSON files parsed; internal Korean and English locale files have identical 39-key sets; the ten World Bosses Korean fragments contain 789 unique nonblank keys; eight encoded external-language chunks decode as a ZIP with 11 namespaces and 1,913 unique nonblank Korean entries; all eight mod item textures are 32×32 PNGs and every referenced internal texture exists.
- `git diff --check`: PASS. Toolchain query reports Temurin Java 25.0.4.1 and Gradle 9.5.1. A first local clean-build attempt inherited a Java 8 path and stopped before Gradle work. Retried with the installed Java 25 runtime; `clean build --stacktrace` then stopped before project configuration completed because Gradle received `Unsupported or unrecognized SSL message` fetching Fabric Loom `1.17.20` from Fabric Maven. No source compilation occurred locally.
- Build #441 / run `40811651532` passed against audit commit `477872f3967994bbcfc833f60bfb1aed27c0867b`: Java 25 / Gradle 9.5.1 `clean build`, pack dependency checks, 77 required GameTests, localization audit, dedicated-server smoke, client boot smoke, verified Modrinth pack generation and production JAR inspection/checksum.
- The production JAR is `augmentation-survival-0.1.0-alpha.1.jar`, 1,415,465 bytes, SHA-256 `d0f7f182e2943167c7af7f3f58cdd0b2329b938cd395885f0d20eb4b096437f5`. Inspection passed: 538 compiled classes, 45 internal asset entries, 41 internal data entries, loader metadata/manifest consistent with project pins, no duplicate entries, and no source/docs payload.
- Verified Modrinth pack SHA-256: `561e0b17fd460c0e33085e0084c3ed3228f461ac60d8e1d4f1ac681737f41c2e`. Artifact `augmentation-survival-pack-smoke-441`, ID `12870627324`, size 3,402,587 bytes, includes the JAR sidecar, pack, `BUILD_AND_RUNTIME_REPORT.md`, `raw-logs.zip`, client/server logs, and localization reports.
- Actual Minecraft screen review, full gameplay playtest, multiplayer save/reconnect checks, external-mod encounter testing, and profiler measurements remain NOT RUN. UI screens are reviewed at source level; visual completion is not claimed.

### Final verification labels

```text
INVENTORY FILES: 467
INVENTORY UNREAD: 0
FULL STATIC FILE REVIEW: YES
RUNTIME-SENSITIVE REVIEW ITEMS: 18 (listed in CODE_AUDIT_2026-10-29_INVENTORY.csv)
LOCAL CLEAN BUILD: BLOCKED before compilation (Fabric Maven TLS response)
CURRENT CODE CI / GAMETEST / SERVER / CLIENT / JAR / PACK: PASS (Build #441, commit 477872f)
PRODUCTION JAR: PASS (538 classes; SHA-256 recorded above)
RAW LOG ARCHIVE / BUILD REPORT: PASS (included in artifact 12870627324)
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
PROFILER: NOT RUN
```

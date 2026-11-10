# Build and Runtime Report

Date: 2026-11-08

Project: `anomalous-classes` · `0.1.0-alpha.1`

Minecraft: 1.21.1 · NeoForge 21.1.244 · Java 21

## Source baseline

- Fetched GitHub `main` at the start of this follow-up. Local `main` and `origin/main` matched at `fc1350833b67229c5f6d98633660cb59f07f459e`.
- Only `projects/anomalous-classes/` was changed. The pre-existing untracked `projects/turnbound/` was left untouched.
- Scope was limited to side-preview names, held-class text placement, static text/layout review, build artifacts, and this report. Class artwork, icons, colors, card proportions, selection behavior, server state, and click handling were preserved.

## Build result

**BUILD PASS.** Java 21.0.10 and Gradle 8.8 completed `./gradlew.bat clean build` successfully. The first attempt placed its log inside `build/`, so Gradle's `clean` could not delete that open log; the same build then passed with its log outside the build directory.

| Check | Result |
|---|---|
| `java -version` | OpenJDK 21.0.10 |
| `./gradlew.bat --version` | Gradle 8.8, Java 21.0.10 |
| `./gradlew.bat clean build` | **PASS** — 5 tasks executed; no test sources configured |
| Static layout verifier | **PASS** — all 9 aspect-ratio / GUI-scale combinations |
| JAR verifier | **PASS** — 2,120,557 bytes; 27 compiled classes; required loader metadata/resources present; no duplicate ZIP entries |
| SHA-256 | `e4f457c22e2927903ba2af5ff5c7d1aba0aef79586751bc625c13c0e92581fe9` |

Deliverables:

- `artifacts/anomalous-classes-0.1.0-alpha.1.jar`
- `artifacts/anomalous-classes-0.1.0-alpha.1.jar.sha256`
- `artifacts/raw-logs.zip` (earlier sanitized logs plus this build, layout matrix, JAR verification, and client startup logs)
- `ui/reviews/2026-11-08/STATIC_LAYOUT_REVIEW.md` and refreshed Korean/English static previews

The build retains two existing removal/deprecation warnings for `EventBusSubscriber.bus()` and `EventBusSubscriber.Bus` in `client/ClientModEvents.java:12`. They are unrelated to this UI change.

## UI and runtime checks

- **TEXT PLACEMENT — STATIC REVIEW PASS:** Side-preview names are omitted while their class thumbnails remain. Compact layouts no longer draw the held-class sentence beneath the tabs. The existing bottom clearance remains reserved for the hotbar area. The wide-layout held panel stays in its existing lower-right position; each held name is measured with the active Minecraft `font.width()` and appears as one whole line only when its full width and line height fit its box. Otherwise that name is omitted.
- **STATIC LAYOUT — PASS:** Checked 16:9, 16:10, and 4:3 at GUI Scale 2, 3, and 4. The verifier checks screen/card/button/tab bounds, tab spacing, click/render action geometry, transition offsets, held-name text rows, and the compact footer's 20px bottom safe area. Korean and English static preview images were refreshed. They use Java2D and are not Minecraft screenshots.
- **Minecraft client — RUNNING:** No Java/Minecraft client was active before launch. Started this project's `runClient` after the successful build; one Minecraft client process remains running. Its log records mod discovery, the `anomalousclasses` resource reload, and OpenAL initialization.
- **CLASS SELECTION SCREEN — NOT OPENED / CLIENT TESTED — NOT RUN:** No CUA or in-game interaction was used. The client is left at its startup screen; the user should open a world and inspect the initial selection screen or press `K` / run `/class`.
- Datagen: **NOT CONFIGURED / NOT RUN**.
- GameTest: **NOT CONFIGURED / NOT RUN**.
- Dedicated server smoke boot: **NOT RUN**; outside this UI-only change.
- Gameplay, combat, balance, progression, and AI: **NOT RUN** and outside scope.

## GitHub main

Per repository policy, this change is committed directly to `main` without creating a branch or PR.

## 2026-11-09 — Profile, skill tree, and class-management frame

### Baseline and scope

- Fetched `origin/main` before editing. Local `main` and fetched `origin/main` both pointed to `b7699d39522979fb44176eca47cf9ec9081ede85` (`Fix class selection text placement`).
- Changes are limited to `projects/anomalous-classes/`; the pre-existing untracked `projects/turnbound/` was left untouched.
- The existing class-selection screen, class selection/change flow, server-owned profile, and prior UI artwork were retained.

### Implemented

- `M` opens the server-backed profile. The key mapping is rebindable in Minecraft Controls.
- Profile shows current class art/icon, class level, stored experience, current SP, and the server player's current max health, attack damage, movement speed, and armor. It has only the two requested navigation buttons.
- The skill tree presents the canonical nodes, ranks, groupings, breakpoints, icons, and cost arrays for all six classes. One-rank, next-breakpoint, max-affordable investment, insufficient-SP/max-rank responses, and free respec are processed by the server. Respec is rejected during combat; ranks and SP are stored per class in the existing SavedData.
- The class-management screen contains the common frame, current class identity, profile return, empty central area, and a small class-change action that reuses the existing selection screen.
- Added an OP level-2-only `/class test grant-levels <amount>` command to exercise level/SP screens. No ordinary XP reward path was invented.
- Reused existing class card art and icons plus vanilla item icons. No class art was replaced and no third-party assets or new libraries were added.

### Build and static verification

| Check | Result |
|---|---|
| Java / Gradle | Java 21.0.10 / Gradle 8.8 |
| `./gradlew.bat clean build` | **PASS**; `test` is `NO-SOURCE` |
| Profile/tree static verifier | **PASS**; canonical node names/order/max ranks, bilingual keys, purchase boundaries, and 9 layout cases |
| JAR verifier | **PASS**; 2,187,404 bytes, 48 classes, loader metadata/assets/data present, cost model packaged, no duplicate ZIP entries |
| SHA-256 | `2f23427468032bf1016c2ff904c90606e2b64c85d873205d7a10ba9bf364bb11` |
| Datagen / GameTest / dedicated server | **NOT RUN**; no project datagen or GameTest source is configured |
| Minecraft UI interaction | **NOT RUN**; CUA was not used |

The package is at `build/libs/anomalous-classes-0.1.0-alpha.1.jar`; the checksum is beside it as `.jar.sha256`. Static layout notes are in `ui/reviews/2026-11-09/PROFILE_SKILLTREE_STATIC_REVIEW.md`.

### Runtime and remaining boundaries

- **CLIENT RUNNING:** `runClient` launched after the successful build; mod discovery, `mod/anomalousclasses` resource reload, and OpenAL initialization appeared in the launch output. The client remains running; no in-world screen was opened or interacted with.
- **CLIENT TESTED: NOT RUN.** The user should enter a world, press `M`, inspect Profile → Skill Tree and Profile → Class Management, and verify input/readability. For SP checks, an operator can use `/class test grant-levels <amount>`.
- The project has no RPG XP acquisition path or approved XP threshold curve. The profile shows stored XP only and does not draw a made-up progress bar.
- Skill investment and storage are implemented; passive combat effects are not. No gameplay, combat, balance, or AI review was performed.
- Existing `EventBusSubscriber.bus()` removal warnings remain in `ClientModEvents.java:14`.
- The existing GitHub Actions workflow uploads the built JAR, checksum, report, and raw CI log bundle on a successful `main` build.

## 2026-11-09 — Profile and skill-tree visual quality follow-up

### Baseline and scope

- Fetched `origin/main` before continuing. Local `main` and fetched `origin/main` both matched `f1c1fb8d5e7230d790f137ab103ebeaf1b836cce` (`anomalous-classes: remove K class selection shortcut`). That change remains intact; no K class-selection mapping or behavior was restored.
- Only `projects/anomalous-classes/` was changed. The pre-existing untracked `projects/turnbound/` remains untouched.
- The existing server-owned profile, class-specific skill data, SP transactions, reset restriction, class selection/change flow, and M key profile action were preserved. No gameplay, combat, or balance code was changed.
- The baseline build exposed a missing `org.lwjgl.glfw.GLFW` import in the existing M key mapping. Adding that import preserves the M mapping while allowing the existing key registration to compile.

### Visual work

- Profile, skill tree, and class management now share authored frame, blank title-plaque, and surface textures with button, node-state, connection, and rank-progress atlases. The six class-specific 4×4 skill icon atlases are separate runtime PNG assets, keyed by canonical node order. Sources and processing are documented in `THIRD_PARTY_ASSETS.md`; no external art, fonts, paid assets, or library dependency was introduced.
- Profile retains the large class illustration, server-stored level/experience/SP, and the four server-reported attributes. Navigation controls are compact. No XP progress bar was invented.
- The tree uses the canonical six skill catalogs, current/max rank, next-rank cost, next breakpoint/cost, per-class icons, and distinct uninvested/invested/selected/hover states. Lines express the existing tree relationships without adding prerequisites. The internal passive-effect status banner was removed; skill investment remains stored but passive combat effects remain unimplemented.
- Class management keeps its intentionally empty central area and only the common frame, current class identity, profile return, and class-change action.
- A comparison and limitation record is in `ui/reviews/2026-11-09/PROFILE_SKILLTREE_VISUAL_REVIEW.md`. Actual Minecraft profile/tree screenshots were not available for this pass; no in-game screenshot comparison is claimed.

### Build and static verification

| Check | Result |
|---|---|
| Java / Gradle | Java 21.0.10 / Gradle 8.8 |
| `./gradlew.bat --console=plain clean build` | **PASS** — 6 tasks executed; `test` is `NO-SOURCE` |
| Profile/skill-tree static verifier | **PASS** — canonical trees, bilingual labels, and all 9 aspect-ratio / GUI-scale cases |
| JAR verifier | **PASS** — 9,378,884 bytes; 48 classes; metadata, assets, data, all new UI texture atlases, and duplicate-entry checks passed |
| SHA-256 | `e1015912b916304ccf88615fa731e9d7bf7cc33035a3e73e55d078d46744eac8` |
| Datagen / GameTest / dedicated server | **NOT RUN** — no configured datagen or GameTest source; outside this UI-only change |
| Minecraft screen interaction | **NOT RUN** — CUA was not used |

Deliverables:

- `artifacts/anomalous-classes-0.1.0-alpha.1.jar`
- `artifacts/anomalous-classes-0.1.0-alpha.1.jar.sha256`
- `artifacts/raw-logs.zip` (sanitized build, static-layout, and JAR-verifier logs)

The existing `ClientModEvents.java:14` `EventBusSubscriber.bus()` removal warnings remain. `tools/verify_profile_skilltree.py` uses conservative Korean/English width estimates for group labels; actual Minecraft glyph widths and texture sampling require in-game review.

### Runtime review boundary

- No matching Minecraft client was running before launch. The updated `runClient` now remains active in a responsive window titled `Minecraft NeoForge* 1.21.1`; its log confirms `anomalousclasses` resources were loaded and OpenAL initialized.
- **CLIENT TESTED: NOT RUN.** The client has not been used to open or interact with these screens. After launch, the user should enter a world, press `M`, then inspect Profile → Skill Tree and Profile → Class Management at the requested GUI scales and aspect ratios.
- **CLIENT RUNNING:** yes. The client remains open at the startup/title screen. No world or UI screen was opened because live client interaction is outside this review and CUA was not used.
- The approved concept images guided the composition and visual language, but the resulting textures are not a pixel-perfect copy. Further ornament and glyph treatment cannot be confirmed without the user's actual Minecraft screen review.

---

# Build and Runtime Report — 2026-11-10

## Build target

- Project: `projects/anomalous-classes/`
- Base GitHub `main`: `5732427fea2ae0fd295618c0ffc84b1e292c53e5`
- Minecraft: 1.21.1
- NeoForge: 21.1.244
- Java: OpenJDK 21.0.10
- Gradle wrapper: 8.8
- Mod version: 0.1.0-alpha.1

## Implementation summary

- Replaced the old small category grid with the six full graph maps. All 581 purchasable nodes are loaded from the graph data: 94 mental dominator, 101 puppeteer, 108 parasite morpher, 102 barrier caster, 86 pact caster, and 90 swarm caster. Each class totals 900 SP.
- Added separate class-map background art and 61 runtime icon files (57 class-specific passive icons plus four shared stat icons). Live graph lines, node states, text, tooltips, and buttons render separately from the artwork.
- Added pan, pointer-centered wheel zoom, bounded camera movement, transformed hit testing, drag/click separation, viewport culling, and edge-clamped hover cards.
- The server validates class, revision, node, purchase path, prerequisites, ownership, and SP. Noncombat reset is free and returns the purchased-node cost.
- Legacy per-class rank arrays migrate to fully purchased stages. Incomplete-stage rank spend is refunded. All six class conversions are validated before a batch save; SP conservation and saved-data round trips are covered by runtime fixtures.
- Minor vanilla attribute nodes use stable modifiers for the active class. Existing major passive descriptions and stage purchases are preserved as data; major combat effects are not claimed active where this project has no combat integration.
- M profile entry and the existing class-selection flow remain. The removed K class-change shortcut was not restored.

## Verification

| Check | Result |
|---|---|
| `.\gradlew.bat --no-daemon clean build --stacktrace` | PASS — `BUILD SUCCESSFUL` |
| Java runtime catalog and graph purchase fixture | PASS — all six graphs can be purchased through the real catalog path at 900 SP |
| Migration fixtures | PASS — new, partial, complete, invalid/atomic-failure, retry, reset refund, and NBT save/load cases |
| Static tree/UI verifier | PASS — 581 nodes, six 900-SP totals, original skill/rank/milestone mapping, Korean/English keys, PNG resources |
| Viewport calculations | STATIC REVIEW PASS — 16:9, 16:10, 4:3 × GUI Scale 2, 3, 4 |
| JAR archive inspection | PASS — readable ZIP, no duplicate entries, 64 compiled classes, NeoForge metadata, graph JSON, languages, backgrounds and icons present; source/tools/docs excluded |
| Datagen / JUnit / GameTest | NOT RUN — no project datagen or JUnit tests are registered; GameTest was not launched |
| Dedicated-server smoke test | NOT RUN — `run/server/eula.txt` is absent; EULA was not accepted on the user's behalf |
| Minecraft client process | RUNNING — development client loaded NeoForge 21.1.244 and the mod resource pack; left open |
| UI screen interaction / visual review | NOT TESTED — no Minecraft UI was opened or manipulated; perform the user test below |
| Multiplayer / gameplay / balance | NOT TESTED — outside this verification run |

## JAR

- File: `anomalous-classes-0.1.0-alpha.1.jar`
- Size: 31,372,781 bytes
- SHA-256: `1a985b65ed7d433be0fc13eaab92bf21b1b9394bfd735f60b8c8edc68ebd943e`
- Checksum file: `anomalous-classes-0.1.0-alpha.1.jar.sha256`

The current JAR is delivered as a local artifact. The repository's previously tracked artifact files were left untouched.

## Warnings and limits

- Compilation reports the existing deprecated NeoForge `EventBusSubscriber.Bus` API use in the client event subscriber.
- The client log includes nonfatal vanilla goat-horn sound and emissive-shader sampler warnings.
- Static font and viewport checks do not substitute for opening the screen in Minecraft. Final visual quality, real font shaping, zoom feel, and click accuracy still require user review.
- Major passive stage purchase state is persistent, but a stage whose combat implementation does not exist remains unimplemented in combat.

## User UI check

1. Enter a test world and open the profile with `M`.
2. Select **Skill Tree** and verify the active class map, all three branch directions, labels, node hover card, and SP counts.
3. Drag from empty space, zoom around the pointer, and check that releasing a drag does not purchase a node.
4. Purchase an available minor/major node, reopen the screen, then confirm SP and ownership persisted. Try a blocked node and an insufficient-SP node.
5. Reset while outside combat; confirm the full node cost is returned. Try again in combat and confirm reset is rejected.
6. Change class through the existing class-selection screen and confirm the other class has its own tree. Repeat at GUI Scale 2, 3, and 4 in 16:9, 16:10, and 4:3 windows; review Korean and English.
7. For authorized progression testing, use the existing admin command `/class test grant-levels 10`.

Do not interpret this UI checklist as a gameplay balance or combat-effect evaluation.

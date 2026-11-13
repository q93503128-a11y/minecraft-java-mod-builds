# Build and Runtime Report

## 2026-11-12 — Common focus items, recipes, and slot foundation

### Baseline and scope

- Fetched `origin/main` at the start. Baseline: `51e0de6ef8dbb9ee1fe1de5260849d0ab6199355`; work remains on `main`.
- Scope: the first functional backend slice after the overview hub. This does **not** complete the six class-specific management systems.
- Existing profile, graph tree, class change flow, M/J/U mappings, combat checks, and outer menu frame were preserved.

### Implemented

- Registered 30 stable focus item IDs (six classes × five grades), with item models/textures derived from existing class glyphs and grade markers. No outside artwork was added.
- Added one server-owned focus slot to the existing player profile `SavedData` (schema 4). Old records without the field load with an empty slot; the item identity is stable across restarts. Death follows `keepInventory` and drops the slot item when that rule is off.
- Added server-validated equip/unequip requests. Equipping requires the matching current-class item in the player's inventory; replacement returns the old item into the consumed item's inventory slot; unequip requires a free slot. Revision checks reject stale/duplicate requests.
- Added 30 actual `minecraft:crafting_shaped` recipes from `data/focus_recipes.json`. The recipe viewer reads that same catalog, including each 3×3 pattern, repeated ingredient counts, the previous grade in the center, and the Pact incomprehensible-grade totem/heart alternative.
- Added a compact focus recipe/equipment screen reachable from the class-management overview. Korean and English labels are included. The grade selector, ingredient/result hover tooltips, inventory count, equip, unequip, and server result states are wired.
- Kept the two-column recipe/details body centered and height-limited on larger screens. On the compact 320×180 logical viewport, the alternative-material control, equipped-slot summary, inventory count, and response line occupy separate measured rows without running into the footer buttons.
- Added a 30-icon contact sheet from the shipped 16×16 textures: [class_focus_item_icons_contact_sheet.png](ui/reviews/2026-11-12/class_focus_item_icons_contact_sheet.png). It is an asset review image, not an in-game screenshot.
- Visually inspected the contact sheet: all six class glyphs remain distinct and grade pips form a clear 1–5 progression when enlarged. Native 16px legibility still needs the user’s Minecraft review.

### Explicit remaining work

- Focus items currently have **no class-specific effects**. Existing design effects require missing class backends (dominated mobs, puppets, hosts, barriers, contracts, or swarm entities); the UI does not claim that equipping a focus applies those bonuses.
- Mental dominator, puppeteer, parasite, barrier, pact, and swarm management actions and persistent domain state are still unimplemented. In particular, user-created swarm tactics remain unimplemented; no default presets were added.
- No focus effect tooltip/stat rows or fake management actions were added. The current `main` Java source contains no controlled-mob, puppet, host, barrier, pact-contract, or swarm entity/domain runtime to connect. The next units therefore need to implement those authoritative systems from their class plans before their management views can show live state; do not extend this screen with empty controls or sample data.

### Verification

| Check | Result |
|---|---|
| `java -version` / Gradle | OpenJDK 21.0.10 / Gradle 8.8 |
| `.\gradlew.bat clean build --console plain --stacktrace` | **PASS** — all 13 tasks; `check` included |
| Focus recipe/assets parity | **PASS** — 30 registered textures/models and 30 shaped recipe outputs/patterns; Pact alternative and previous-grade-center checks |
| Focus screen layout | **PASS** — 12 logical viewport cases across 16:9, 16:10, 4:3, compact 16:9 and GUI Scale 2–4; recipe grid, arrow/result, panels and buttons remain in bounds and non-overlapping |
| Management layout | **PASS** — 9 viewport/scale cases |
| Existing graph/runtime regression | **PASS** — 581 nodes, six 900-SP graphs, legacy migration and SP conservation, focus-slot NBT round trip, old-save default, all 30 item identities |
| Existing profile/tree static verifier | **PASS** — Korean/English and 18 prior viewport cases |
| JAR verification | **PASS** — 31,745,646 bytes, 83 compiled classes; required class assets, 30 focus item models/textures and all 30 shaped recipes verified; no duplicate entries |
| `git diff --check` | **PASS** — Git emitted only existing LF/CRLF normalization notices |
| Compiler warnings | Two existing `EventBusSubscriber.bus()`/`Bus` removal warnings in `ClientModEvents.java`; Gradle 9 compatibility deprecation notice |
| Development client | **RUNNING** — checked for an existing project `runClient` first, found none, then started `.\gradlew.bat runClient`. The log shows NeoForge 21.1.244, this mod discovered, resource reload, and graphics/audio initialization; the process remains open. |
| Datagen / GameTest / dedicated server / multiplayer | **NOT RUN** |
| Actual screen open, click, or Minecraft visual inspection | **NOT TESTED** — no screen was opened and no CUA was used. The user should open the focus screen for final visual/click review. |

### Artifact

- Build JAR: `build/libs/anomalous-classes-0.1.0-alpha.1.jar`
- User deliverable: `artifacts/anomalous-classes-0.1.0-alpha.1.jar`
- SHA-256: `91450e4277d267019e0e9a428f3f93c4849fb4899f8f960a097741e54bd2b732`
- Sanitized logs: `artifacts/raw-logs.zip`
- GitHub push result and final `main` SHA are in the completion summary.

---

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

---

# Build and Runtime Report — 2026-11-10 UI Playtest Fixes

## Baseline

- Repository: `q93503128-a11y/minecraft-java-mod-builds`
- Project: `projects/anomalous-classes/`
- Branch: `main`
- Base HEAD checked before editing: `04ef95c168baf6778f2751abc26452ac5ffe4f89`
- Minecraft 1.21.1 / NeoForge 21.1.244 / Java 21.0.10 / Gradle 8.8

## Root causes and changes

- **Missing node icons:** `SkillTreeScreen.iconTextures` was read while drawing nodes, but never populated. The screen now builds the map from the active tree's `purchaseNodes()` and each node's existing `iconPath()`. It reuses all 57 class skill textures and four shared stat textures already in the project.
- **Stage fractions:** node rendering drew `stage/maxStages` below major nodes and hover cards showed the legacy rank fraction. Those text draws and the redundant minor `0/1` rank line were removed. Stage, prerequisite, migration, and milestone fields remain intact in graph data and server code.
- **Literal translation arguments:** the new skill-tree labels used `{0}` / `{1}`, but Minecraft's `Component.translatable()` uses Java format arguments. Both locales now use `%s` (and `%%` for a literal percent); the removed stage/rank strings were also deleted. Static checks compare KO/EN argument counts and reject indexed braces.
- **Broken panel and button frames:** `drawPanel()` compressed complete 256×256 atlas cells into 6–12 px strips, while buttons compressed an entire 1024×256 state row into each control. Panel rails and center ornaments now use cropped atlas regions with adaptive corner geometry; button skins keep fixed-size end caps and stretch only the center.
- **Profile alignment and artwork:** profile content now respects a size-aware inset matching the frame, navigation buttons sit inside that inset, and the class title is vertically aligned with its icon. Existing 16:9 artwork keeps its aspect-preserving cover crop and is explicitly clipped to its assigned rectangle.
- **Background contrast:** a restrained plum tint is applied only over the skill-map viewport. The six class backdrops and all node data remain unchanged.

No icons or class illustrations were replaced. The 581 purchase nodes, six 900-SP totals, server-authoritative investment/reset, saved data, migration, pan/zoom, M profile entry, existing class-selection route, and removed K shortcut were not modified. RPG XP behavior was not added.

## Verification

| Check | Result |
|---|---|
| `.\gradlew.bat --no-daemon --console=plain clean build --stacktrace` | **PASS** — 18s, 7 tasks executed; `test` is `NO-SOURCE` |
| Runtime graph/migration fixture | **PASS** — graph catalog, legacy conversion, SP conservation, atomic failure, reset, and NBT round trip |
| Static profile/tree verifier | **PASS** — 581 nodes, six 900-SP trees, 61 icon paths, KO/EN format arguments, profile layout and tree HUD in 18 combinations |
| JAR archive verifier | **PASS** — 31,373,485 bytes, 64 classes, metadata, graph data, six backgrounds, 61 icon textures, and duplicate-entry check |
| Off-screen frame sampling preview | **STATIC REVIEW PASS** — large and compact panels plus two button states reconstructed from the same atlas source crops; this is not a Minecraft screenshot |
| Datagen / GameTest | **NOT RUN** — no configured datagen or GameTest source |
| Dedicated server | **NOT RUN** — no server EULA was accepted for this UI-only change |
| Minecraft client process | **RUNNING** — no matching client existed before launch; updated development client loaded NeoForge 21.1.244 and the mod resources |
| Minecraft UI interaction / visual inspection | **NOT TESTED** — CUA was not used; no profile or skill-tree screen was opened |
| Playtest / multiplayer | **NOT TESTED** |

The off-screen preview checks frame texture geometry only. The nine layout cases per screen are static coordinate checks, not actual Minecraft GUI-scale or font-rendering tests. The existing NeoForge `EventBusSubscriber.Bus` deprecation warnings remain; startup also emitted existing nonfatal shader and vanilla sound warnings.

## JAR

- File: `anomalous-classes-0.1.0-alpha.1.jar`
- Size: 31,373,485 bytes
- SHA-256: `2a65a53bf4e86ee153934fbeda94fe770439e8b08f2eea7ec3410e67cff005c5`
- Local deliverable folder: `Downloads/anomalous-classes-ui-playtest-fixes-2026-11-10/`
- Checksum file: `anomalous-classes-0.1.0-alpha.1.jar.sha256`

## User review

The development client is left running. Open a test world in that window, press `M`, and inspect the profile frame, title/class alignment, artwork crop, stat rows, and navigation buttons. Open **스킬 트리 / Skill Tree** and verify that node icons appear, major nodes have no stage fractions, SP strings and hover-card arguments are formatted, and the tree remains legible over each class backdrop. Check Korean and English, then repeat at GUI Scale 2, 3, and 4 in 16:9, 16:10, and 4:3 layouts. This checklist is UI review only; no combat, balance, or progression test is requested.

---

# Build and Runtime Report — Class-management overview hub — 2026-11-12

## Baseline and authorized scope

- Repository: `q93503128-a11y/minecraft-java-mod-builds`, project `projects/anomalous-classes/`, branch `main`.
- Fetched and checked `origin/main` before editing; local and remote baseline: `b3849bb671ad08f7e4d3b63da6580b9edd86ac87`.
- Checked the shared development playbook at `e022e9e4ff3947fcbd7f5b0237c5905d7c0409c7` and read the handoff ZIP's `CODEX_PROMPT.md`, `DESIGN_NOTES.md`, and all seven PNGs.
- The user selected a current-functionality management hub. Latest project code has no class-specific management backends, focus-medium item, or recipe registry/recipe data. Those features were not fabricated; the existing class-management screen is an overview and route hub only.
- No class selection, skill-tree, server persistence, save migration, outer frame, or unrelated project implementation was changed.

## Implementation

- Reworked `ClassManagementScreen` to use the six existing class illustrations/icons and the server profile snapshot: class, level, stored experience, available SP, and the actual current vanilla attributes.
- Profile, Skill Tree, and Change Class controls route through the existing request payloads and server checks. The profile and tree buttons show the existing M/J shortcuts; the smaller Change Class route reuses the server class-selection flow.
- Added `ClassManagementLayout`, with measured responsive bounds, and a verifier for 16:9, 16:10, and 4:3 at GUI Scale 2, 3, and 4.
- Preserved the shared ornamental frame and its assets. Existing profile and skill-tree screens and all six class-specific backdrops remain intact.
- Added Korean and English labels. No debug/internal text, mock management actions, default swarm tactics, or pretend focus recipe are shown.
- Added Java2D static previews based on the existing runtime class art, icon, and UI frame textures:
  - `ui/reviews/2026-11-12/class_management_mental_ko_static.png`
  - `ui/reviews/2026-11-12/class_management_mental_en_static.png`
  - `ui/reviews/2026-11-12/class_management_swarm_ko_static.png`

The previews were visually reviewed for layout balance, alignment, and clipping after an initial overly empty information panel was compacted. They are static Java2D renders, not Minecraft GPU screenshots.

## Verification

| Check | Result |
|---|---|
| Java / Gradle | OpenJDK 21.0.10 / Gradle 8.8 |
| `verifyClassManagementLayout` | **PASS** — nine screen-aspect × GUI-scale combinations |
| Existing graph/profile/resource/migration verifiers | **PASS** — 581 nodes, 900 SP per class, 61 icons, Korean/English, migration fixtures, and existing 18-viewport checks |
| Java2D static review | **RENDERED AND VISUALLY REVIEWED** — Korean/English mental-control and Korean swarm-control previews; not an in-game capture |
| `git diff --check` / resource JSON validation | **PASS** (run before commit) |
| `clean build` | **PASS** — 10 actionable tasks; final build completed successfully |
| First build attempt | **FAILED, then fixed** — the temporary verifier used the source-launcher's isolated classloader and could not load the package-private layout class. The verifier was moved onto the normal test runtime classpath; final clean build and check passed. |
| Existing compile warnings | Existing NeoForge `EventBusSubscriber.bus()` / `Bus` deprecation/removal warnings in `ClientModEvents.java`; unrelated to this change |
| JAR verification | **PASS** — 31,662,846 bytes, 69 compiled classes, loader metadata and required mod assets/data; no duplicate entries |
| Datagen / GameTest / dedicated-server / multiplayer | **NOT RUN** — none was needed for this client UI scope |
| Minecraft development client | **RUNNING** — after checking no runClient existed, started the project's normal `runClient`; Windows reports title `Minecraft NeoForge* 1.21.1` at last check. The process was left open. |
| Minecraft UI interaction / client visual test | **NOT TESTED** — no screen was opened or manipulated, and no CUA was used. |

## JAR and checksum

- JAR: `build/libs/anomalous-classes-0.1.0-alpha.1.jar`
- Size: `31,662,846` bytes
- SHA-256: `d0a279f541186a92c374cefa56111f7436faf7ac3f109027b083a407084a3716`
- Checksum sidecar: `build/libs/anomalous-classes-0.1.0-alpha.1.jar.sha256`

## User review

In the running development client, enter a world and press `U` to open the class-management overview. Verify the class art, profile values, compact button row, Korean/English labels, then open Profile, Skill Tree, and Change Class. Inspect GUI Scale 2, 3, and 4 and 16:9, 16:10, and 4:3 layouts. These checks are for screen layout and navigation only; no gameplay, combat, balance, or multiplayer review is requested. Class-specific management actions and the focus-medium recipe screen remain future work until their actual game systems are designed and implemented.

---

# Build and Runtime Report — 2026-11-11 Profile/Skill-Tree Visual Repair

## Baseline and scope

- Repository: `q93503128-a11y/minecraft-java-mod-builds`
- Project: `projects/anomalous-classes/`, branch `main`
- Fetched `origin/main` before editing; local and remote baseline: `c71dd56b0bf9b77634546480b3fa551ff81b8d84`
- Minecraft 1.21.1 / NeoForge 21.1.244 / OpenJDK 21.0.10 / Gradle 8.8
- No gameplay, graph data, server purchase/reset, save/migration, class selection, K-key behavior, or pan/zoom code was changed.

## Changes

- **Frame root cause:** the previous horizontal helper sampled a guessed 24-pixel-wide strip around atlas x=84 and stretched it across each full-width rail. Its top/bottom source Y values were also guessed. The old corner crops began at 0/624 rather than the visible corner bounds at 32/576 and clipped the actual outer curve/gem. Rails, corners, and ornaments therefore included wrong source pixels and distorted artwork. The captured frame streaks match that source/scale error. The original 768×768 RGBA atlas is unchanged; 14 verified pieces are extracted to separate PNGs and rails repeat at the corner’s source scale. Static and runtime resource paths use these pieces.
- **Profile:** labels and values are measured with the active Minecraft `Font.width()` and grouped into one centered two-column area; values are right-aligned within the value column. Existing art remains aspect-preserved and scissored to its area.
- **Skill-tree cards:** only node name, actual effect/stat gain, SP cost, and one state line remain. Repeated prerequisite names, stage counts, the passive-description prefix, and duplicate path instructions are removed. Width is capped at 160 GUI units; height follows wrapped text. The actual purchase checks remain unchanged. Locale format arguments use `%s`, with no indexed `{0}` values found by validation.
- Added `tools/render_ui_static_review.java` and five review images from the source textures/current layout. The old frame uses the pre-change UV/scale formula; the new frame uses separated crops and proportional tile scaling.

## Verification

| Check | Result |
|---|---|
| Official NeoForge Maven 21.1.244 | HTTP 200 for the official POM |
| `java -version` / `gradlew --version` | OpenJDK 21.0.10 / Gradle 8.8 |
| `gradlew --no-daemon --console=plain clean build --stacktrace` | **PASS** — 28s; 7 tasks; `test` is `NO-SOURCE` |
| Graph/runtime fixture | **PASS** — catalog, legacy migration, SP conservation, atomic failure, reset, NBT round trip |
| Static graph/UI verifier | **PASS** — 581 nodes, six 900-SP totals, 61 skill icons, KO/EN, 18 screen-ratio × GUI-scale cases |
| Static visual images | **RENDERED AND VISUALLY REVIEWED** — frame comparison, Korean/English profile, Korean/English tooltip. These are Java2D source/layout previews, not Minecraft screenshots. |
| JAR verifier | **PASS** — 31,674,627 bytes, 65 classes, loader metadata, data/assets, 14 frame pieces, 61 tree icons, no duplicate entries |
| Datagen / GameTest / dedicated server | **NOT RUN** — no UI-relevant configured datagen/GameTest; no server test was requested |
| Minecraft development client | **RUNNING** — started after confirming no client was active. Log shows mod discovery, resource-manager reload including `mod/anomalousclasses`, and OpenAL initialization. No screen was opened or manipulated. |
| Minecraft UI visual test / playtest | **NOT TESTED** — no CUA; actual GUI scale rendering, GPU filtering, and click behavior remain for user review |

The static matrix covers 16:9, 16:10, and 4:3 at GUI Scale 2, 3, and 4 for both profile and skill-tree bounds. Preview text glyphs are rendered by Java2D; actual Minecraft glyph appearance was not inspected. The old frame reproduction illustrates the bad atlas sample/stretch but does not emulate Minecraft texture filtering or mipmaps pixel-for-pixel.

## JAR and checksum

- File: `artifacts/anomalous-classes-0.1.0-alpha.1.jar`
- Size: `31,674,627` bytes
- SHA-256: `fc9f8437295ce13eac2b87482ba1ffde58f7b9c4f60c35b3f7f83ea8a05e5aad`
- Checksum: `artifacts/anomalous-classes-0.1.0-alpha.1.jar.sha256`
- Sanitized logs: `artifacts/raw-logs.zip`
- Visual review: `ui/reviews/2026-11-11/`

The JDK compiler reports the existing `EventBusSubscriber.bus()` / `.Bus` removal warnings in `ClientModEvents.java:14`. Client startup also reported the existing vanilla goat-horn and emissive-shader warnings. No new UI startup error was observed.

## User verification

The development client remains running without GUI input. Open a test world, press `M`, and inspect all four profile frame corners, the top/bottom rails and ornaments, class artwork crop, stat columns, and buttons. Open the skill tree and hover a locked and purchasable major node; verify the concise status, wrapped effect, card bounds, SP HUD, and node icons. Repeat in Korean and English at GUI Scale 2/3/4 and 16:9/16:10/4:3. This is UI-only review; no gameplay, combat, balance, or multiplayer test was run.

---

# Build and Runtime Report — 2026-11-11 frame artifact repair

## Baseline and scope

- Fetched latest `origin/main` before editing: `93709c133b288fe046cf73f31be103ee78b8bc69`.
- Source and resource changes are scoped to `projects/anomalous-classes/`; unrelated project files were not included.
- Corrected shared profile, class-management, skill-tree HUD, and tooltip frame rendering. The 581-node graph, 900-SP totals, skill icons/backgrounds, purchases, resets, prerequisites, saves, migrations, profile contents, and class flow were preserved.

## Cause and fix

The frame art was drawn once; the bad pieces came from the source geometry. The previous panel code compressed non-seamless 24px-wide rail artwork to about 4 GUI pixels at GUI Scale 4 and repeated it along the whole edge. The previous side-jewel crops were 48×64 rather than the isolated 24×64 art, and the top/bottom crops were 64×88 rather than 32×88. Those wider crops included adjacent rail/background pixels and were overdrawn on top of the rails. The old `Graphics2D` extractor also changed RGB values on partially transparent pixels (alpha remained unchanged), adding a smaller edge-sampling risk.

Rails now stretch a single-pixel cross-section along their long axis instead of repeating the non-seamless crop. Side and center ornaments use exact isolated atlas regions. The extractor copies each RGBA pixel without compositing. The original atlas and authored frame shapes remain unchanged. Details and atlas coordinates are in [FRAME_ROOT_CAUSE_REVIEW.md](ui/reviews/2026-11-11/FRAME_ROOT_CAUSE_REVIEW.md).

## Verification

| Check | Result |
|---|---|
| Java / Gradle | OpenJDK 21.0.10 / Gradle 8.8 |
| RGBA crop comparison | **PASS** — all 14 runtime PNGs match the selected `frame_atlas.png` source pixels exactly; alpha and color deltas are zero |
| Previous extractor comparison | **REVIEWED** — alpha was unchanged; prior corner RGB differed by up to 5, and 24–45 transparent corner RGB samples per part had been reset |
| Before/after visual composites | **STATIC REVIEWED** — all four corners and both center ornaments, plus whole frame; original `surface_tile.png` under the panel and a checker outside it expose edge leakage |
| Shared UI static review | **PASS** — refreshed profile/tooltip compositions; profile and tree layout checks across 16:9, 16:10, 4:3 × GUI Scale 2, 3, 4 (18 cases) |
| `python tools/verify_graph_skilltree.py` | **PASS** — 581 nodes, six 900-SP trees, 61 icons, Korean/English, migration fixtures, and 18 viewport cases |
| `.\gradlew.bat --no-daemon --console=plain clean build --stacktrace` | **PASS** — 7 tasks; graph/migration runtime fixtures passed; `test` has no sources |
| JAR verifier | **PASS** — 31,652,487 bytes, 65 compiled classes, metadata and required resources present, no duplicate entries |
| SHA-256 | `50c21d20acea1e3f913a576a337d1e02904da1132272510187411f3200e38607` |
| Existing deprecation warnings | Two in `client/ClientModEvents.java:14` (`EventBusSubscriber.bus()` and `Bus`); unrelated and unchanged |

The static render is Java2D using the runtime PNGs and current layout math. It does not emulate Minecraft's GPU sampling or prove the in-game result.

## Runtime and user review

- **CLIENT RUNNING: YES.** No matching client process was present before launch. Started one `runClient` after the patched build; its log records NeoForge 21.1.244, `anomalousclasses`, resource reload, and OpenAL. It remains open for the user's review.
- **CLIENT TESTED: NOT RUN.** No Minecraft screen was opened or manipulated and no CUA was used. The user should enter a world and inspect Profile corners and center title trim, Skill Tree SP panel and hover card, and Class Management corners. Repeat at preferred GUI scales/aspect ratios.
- Datagen: **NOT CONFIGURED / NOT RUN**. GameTest: **NOT RUN**. Dedicated-server smoke boot: **NOT RUN**.
- Gameplay, balance, combat, saves/migration behavior, and multiplayer were not retested by this frame-only task.

## Artifact

- JAR: `artifacts/anomalous-classes-0.1.0-alpha.1.jar`
- Checksum: `artifacts/anomalous-classes-0.1.0-alpha.1.jar.sha256`
- Pixel audit and side-by-side render: `ui/reviews/2026-11-11/frame-root-cause/`
- GitHub `main` implementation commit: `66e6e3a27dd694531ba781f6415a8c818c8cbf9b`.
- Checksum file: `anomalous-classes-0.1.0-alpha.1.jar.sha256`

The verified JAR and checksum are included under the project artifacts directory. The sanitized build, JAR verifier, pixel audit, and client-startup logs are preserved in the artifacts raw-logs ZIP.

## Warnings and limits

- Compilation reports the existing deprecated NeoForge `EventBusSubscriber.Bus` API use in the client event subscriber.
- The client log includes nonfatal vanilla goat-horn sound and emissive-shader sampler warnings.
- Static font and viewport checks do not substitute for opening the screen in Minecraft. Final visual quality, real font shaping, zoom feel, and click accuracy still require user review.
- Major passive stage purchase state is persistent, but a stage whose combat implementation does not exist remains unimplemented in combat.

## User UI check

1. In the running patched client, enter a test world and open Profile with M. Inspect all four corners, top/bottom rails and center ornaments for stray squares, dark flecks, clipping, gaps, or overlaps.
2. Open Skill Tree and inspect the SP HUD frame and a hovered node card. Confirm the frame joins remain clean at the smaller panel size.
3. Open Class Management and inspect its panel corners and rails.
4. Repeat the visual check in Korean and English at GUI Scale 2, 3, and 4, using 16:9, 16:10, and 4:3 window shapes where available.

This is visual UI review only. No node purchase, reset, combat, balance, save, migration, or multiplayer test is part of this frame repair.

---

# Build and Runtime Report — Mental Dominator core loop — 2026-11-13

## Baseline and scope

- Repository: `q93503128-a11y/minecraft-java-mod-builds`, project `projects/anomalous-classes/`, branch `main`.
- Fetched `origin/main` before final integration. Local baseline and remote `main` matched at `939ba3af7b35d39942cad03c3fb962f7a1bc7113`.
- Minecraft 1.21.1 / NeoForge 21.1.244 / Java 21.0.10 / Gradle 8.8.
- Added the Mental Dominator server loop and class-management screen. Existing profile, skill tree, other class screens, 30 focus items and shaped recipes, focus slot, save data, and class-change checks were retained.

## Implemented

- Server-side R-key channeling resolves the target from the player's actual aim, checks a clear raycast, class, target family, entity ownership, current slot capacity, and psyche, then revalidates the same target each tick. Start cost, per-second cost, interruption, progress decay, cooldown, and success are decided on the server.
- Successful control keeps the original `Mob` and UUID. It adds a persistent owner marker and a world `SavedData` index. Capture clears the current combat target; release removes the marker and temporary control goals, stops navigation, clears the active target, and restores the saved `NoAI` state. The original entity, health, equipment, tame owner, villager data, native goal selectors, and other entity NBT remain in place. A persisted release tombstone protects the entity from regaining an old control link if entity NBT and the world index are saved on different ticks.
- Vanilla `PathfinderMob` entities can be targeted, including passive creatures and villagers as well as attack-capable mobs. This supports their original native AI; it does not grant passive entities attacks. Controlled combat-capable mobs receive a target-selection goal that points their existing combat AI at valid targets. Follow uses native navigation; stay suppresses follow movement while still allowing the mob's native response to a valid target. Cross-dimension follow does not take over the mob's movement selector. No teleport, clone, forced chunk load, or resurrection was added.
- Owner/team/controlled-entity checks guard target selection and damage. Player damage is denied unless the per-mob setting is enabled, server PvP is enabled, and the victim is not the owner or an ally. Loaded controlled mobs pause AI while their owner is offline, dead, or no longer the Mental Dominator; unloaded entities are never force-loaded. Loaded maintenance drains psyche, and shortage releases the lowest-threat entity first (newest first for ties). Death removes the entity record and returns its slot.
- `U` opens the server-backed Mental Dominator manager for this class, while the other classes retain the common management hub. The screen shows current/max psyche, loaded upkeep, slots, a scrollable list, only-live health for loaded entities, command/location/status, per-entity upkeep, player-damage state, individual and all follow/stay, two-step release, and the captured-species codex. Results are applied from server responses; actions include an owner revision check. M/J/U mappings and the removed K shortcut remain unchanged. `R` is a rebindable default for mental intrusion.
- Matching Mental Dominator focus equipment modifies the actual resource rules. Only the equipped matching class tier applies. The user-confirmed Incomprehensible values are max psyche +100%, upkeep −40%, and intrusion +28%; the lower tier values follow the existing class design table and remain unplaytested.

## Balance source and limits

The numeric runtime defaults are explicitly marked as **adjustable candidates**, not playtested constants. They follow `DOMINATION_BALANCE.md`, `MENTAL_DOMINATOR.md`, and the skill-tree slot/upkeep proposal: max psyche `100 + 5 × (level − 1)`, regeneration `2.5 + 0.06 × (level − 1)` per second, control factor `1 + 0.035 × (level − 1)`, 12 psyche to start, 5 per channeling second, 40 base progress per second, 20 interruption decay per second, 80 ticks cooldown, and 16-block unobstructed range. The configurable threat estimate uses base 4, max-health weight 4, attack-damage weight 1.4, and armor weight 0.3; resistance piercing affects intrusion only. Individual upkeep follows `max(0, 0.018 × T^1.4 − 0.006 × (level − 1))`, while group upkeep follows `0.05 × max(0, N − 1)^2`, with matching focus and passive reductions. Maximum control follows `3 + floor(sqrt((level − 1)/8)) + floor(multipleBondRanks/5)`.

The proposal's special-ability threat term `E` is not inferred. The current capture scope is the `minecraft` namespace plus `PathfinderMob`; modded entity adapters and non-Pathfinder families such as the Ender Dragon are not supported. This is an AI compatibility boundary, not a blanket boss immunity. Vanilla bosses that satisfy the implemented family and state checks are not rejected by a boss flag. Target threat currently uses the available vanilla health, attack-damage, and armor attributes, so unmodeled projectile/special abilities are not represented. Candidate values need actual survival playtesting before balance claims.

## Verification

| Check | Result |
|---|---|
| `java -version` / Gradle | OpenJDK 21.0.10 / Gradle 8.8 |
| `gradlew --no-daemon --console=plain clean build --stacktrace` | **PASS** — 15 actionable tasks; `check` completed |
| Mental rules verifier | **PASS** — psyche/regen, slots, 5 focus tiers and isolation, passive mapping/reductions, release-pending and tombstone NBT round-trip, and no cross-dimension follow movement |
| Management layout verifier | **PASS** — 9 cases across 16:9, 16:10, 4:3 and GUI Scale 2–4 |
| Existing regression checks | **PASS** — 581 tree nodes, six 900-SP trees, legacy migration/SP conservation, 61 skill icons, bilingual strings, existing profile/tree layouts, focus-slot persistence, and 30 shaped focus recipes |
| `tools/verify_jar.py` | **PASS** — 31,818,553 bytes; 104 compiled classes; loader metadata/assets/data and all focus item/recipe parity checks; no duplicate ZIP entries |
| Locale JSON / `git diff --check` | **PASS** |
| Development client | **RUNNING** — checked for an existing project Java client first (none found), then started the normal Gradle `runClient`. Startup logs show NeoForge 21.1.244, this mod discovered, resource reload, and audio initialization. The Minecraft process remains open; no screen was inspected or interacted with. |
| Client UI / gameplay / multiplayer | **NOT TESTED** — no world or screen was opened, no interaction or CUA was used, and no dedicated server or multiplayer session was run. |
| Datagen / GameTest | **NOT RUN** — this project has no configured datagen or GameTest task/source. |

Two existing NeoForge `EventBusSubscriber.bus()` / `Bus` removal warnings remain in `ClientModEvents.java`; Gradle also reports the project's existing Gradle 9 deprecation notice.

## Artifact

- JAR: `artifacts/anomalous-classes-0.1.0-alpha.1.jar`
- Size: `31,818,553` bytes
- SHA-256: `cb6851fc22705dd7779fc4b2a2db030628c83bbd0e3d305ce801278c7bcdac9b`
- Checksum: `artifacts/anomalous-classes-0.1.0-alpha.1.jar.sha256`
- Sanitized build and JAR-verification logs: `artifacts/raw-logs.zip`

## User playtest procedure

1. In the running dev client, create or enter a test world and select Mental Dominator. Use the existing OP-only `/class test grant-levels 10` command if level/SP test progression is needed.
2. Summon a vanilla zombie with `/summon minecraft:zombie ~ ~ ~`, aim at it unobstructed within 16 blocks, and hold the default `R` key (rebindable) until the server reports success. Verify that the original zombie remains and begins following.
3. Open `U`; select the controlled zombie, switch Follow/Stay, use All Follow/All Stay, toggle player damage, inspect live health/maintenance/location, and release after the second confirmation. Verify the released mob is the same entity and regains its normal AI.
4. Try a villager and iron golem. Check that passive mobs do not gain attack behavior. Let a controlled mob die and verify its slot disappears. Then leave/re-enter the world and inspect the remaining list and codex.
5. Use the same steps with each equipped Mental Dominator focus tier, especially the user-confirmed Incomprehensible tier. Repeat in a local multiplayer test only if needed; multiplayer has not been tested by this build task.

These are player-run gameplay checks. The running client is **not** evidence that the capture, AI, UI interactions, balance, or multiplayer have been playtested.

---

# Build and Runtime Report — class-management UI/UX — 2026-11-13

## Scope and baseline

- Repository: `q93503128-a11y/minecraft-java-mod-builds`, project `projects/anomalous-classes/`, branch `main`.
- Fetched `origin/main` before editing. Starting local/remote HEAD: `06fba9183e6f4630dfa4e60fe3f326f3031db1e4`.
- Minecraft 1.21.1 / NeoForge 21.1.244 / OpenJDK 21.0.10 / Gradle 8.8.
- This change is limited to the Mental Dominator manager presentation, focus recipe screen spacing/labels, layout regression checks, and offline design-review material. No gameplay, AI, combat, balance, persistence, or server payload code changed. Existing frame PNGs, 581-node trees, 30 focus items/recipes, and M/J/U mappings were left intact.

## Implemented

- Mental Dominator: grouped psyche/slot/upkeep summary with a server-backed psyche meter; larger creature list rows; real vanilla spawn-egg icons only when a matching item is registered; selected-row hierarchy and live health bars; type/status/health/command/upkeep/location/damage state in the detail surface; a pending server response notice; and compact navigation that clears the title area at small logical widths. The existing ownership, revision, server actions, two-step release, and codex flows are unchanged.
- Focus recipe screen: kept the five grade selectors legible as compact numeric tabs; the selected grade remains named in the detail surface and hover tooltip. Reflowed the information panel so the alternative recipe selector no longer collides with equipment/inventory status; added class-accent surface rules. Recipe catalog, displayed 3×3 patterns, equip/unequip actions, and server behavior are unchanged.
- Other five classes: created ten bilingual, offline-only layout review images plus a contact sheet, reusing existing class art/icons and menu textures. They carry a visible “DESIGN REVIEW · NOT IN GAME” badge and are not registered as screens. Swarm starts with no saved tactics and no built-in presets. The separate UI data-contract document lists what server responses are still required before these views can be connected.
- The existing outer ornamental frame assets were not edited.

## Verification

| Check | Result |
|---|---|
| `.\gradlew.bat --no-daemon --console=plain verifyMentalDominatorLayout verifyClassFocusLayout` | **PASS** — 12 viewport/GUI-scale combinations for each screen. |
| `.\gradlew.bat --no-daemon --console=plain clean build --stacktrace` | **PASS** — 15 tasks; focus item/recipe parity, class management layouts, 581-node graph/migration, profile/skill-tree, focus, and Mental Dominator checks completed. |
| Profile/skill-tree existing viewport checks | **PASS** — 16:9, 16:10, 4:3 × GUI Scale 2–4. |
| Korean/English language JSON | **PASS** — both parsed successfully. |
| Java2D review renderer | **PASS** — ten 1600×900 Korean/English images and contact sheet regenerated from current project art/texture files; representative full-size images and the contact sheet were visually inspected. |
| `git diff --check` | **PASS** — no whitespace errors. Git only reports the repository's LF-to-CRLF conversion notices. |
| `python tools/verify_jar.py artifacts/anomalous-classes-0.1.0-alpha.1.jar` | **PASS** — 31,820,748 bytes; 104 compiled classes; loader metadata and required resources present. |
| Static render versus Minecraft | Static review images are **not** Minecraft screenshots and do not validate `GuiGraphics`, native font shaping, GPU filtering, or actual click feel. |

The review renderer outputs only the five screens that do not yet have server systems; those images are design studies, not game states. Actual responsive behavior was checked for the two connected screens (Mental Dominator and Focus) and existing profile/skill-tree screens. The five review-only designs are single-canvas 1600×900 compositions, not runtime screens or viewport tests.

## Runtime status and user review

- **CLIENT RUNNING: YES.** No project `runClient` process existed before launch. Started the normal Gradle `runClient` after the clean build. Startup log shows NeoForge 21.1.244, `anomalousclasses`, resource reload, LWJGL, and audio initialization. The process remains open for the user.
- **CLIENT TESTED: NOT RUN.** No world or class-management screen was opened or manipulated, and no CUA was used. Do not treat the startup log as visual QA.
- **PLAYTESTED: NOT RUN. MULTIPLAYER TESTED: NOT RUN.** The five other class layouts are review-only images and their game backends remain absent.
- Datagen: **NOT CONFIGURED / NOT RUN**. GameTest: **NOT RUN**. Dedicated-server boot: **NOT RUN**.

To review the connected screens, enter a world and press `U` on Mental Dominator, then inspect the real server list/detail, pending response, row selection, follow/stay, damage toggle, codex, and two-step release states. Open the focus screen from its existing navigation and inspect each grade, the actual 3×3 recipe, equipment, and inventory status. Repeat in Korean/English at GUI Scale 2, 3, and 4. The five other class screens are not available in-game; inspect their bilingual review assets instead.

## Artifacts

- JAR: `artifacts/anomalous-classes-0.1.0-alpha.1.jar` — 31,820,748 bytes.
- SHA-256: `a21eed743cb5ba892583abd15b65e1a2ceeb413a46ff47152fec559f85d18feb`.
- Checksum: `artifacts/anomalous-classes-0.1.0-alpha.1.jar.sha256`.
- Five-class bilingual review set: `ui/reviews/2026-11-13/class-management-design-review/`.
- UI/server contract gaps: `ui/CLASS_MANAGEMENT_UI_DATA_CONTRACT_2026-11-13.md`.
- Sanitized build, verification, and startup logs: `artifacts/raw-logs.zip`.
- GitHub implementation commit: pending final push.

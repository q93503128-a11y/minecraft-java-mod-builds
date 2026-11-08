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

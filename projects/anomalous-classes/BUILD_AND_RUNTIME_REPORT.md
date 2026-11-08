# Build and Runtime Report

Date: 2026-11-08

Project: `anomalous-classes` · `0.1.0-alpha.1`

Minecraft: 1.21.1 · NeoForge 21.1.244 · Java 21

## Source baseline

- Local `main` and fetched `origin/main` matched at start: `62c80be94d5ce913dc7f055bbe123dcf3ed20fce`.
- The completed UI follow-up commit and GitHub Actions result are recorded below after push.
- No other project directory was changed. The pre-existing untracked `projects/turnbound/` was left untouched.

## Build result

**BUILD PASS.** Java 21.0.10 and Gradle 8.8 ran the project clean build successfully.

| Check | Result |
|---|---|
| `java -version` | OpenJDK 21.0.10 |
| `./gradlew.bat --version` | Gradle 8.8, Java 21.0.10 |
| `./gradlew.bat --no-daemon clean build --console=plain` | **PASS** — 5 tasks executed; no test sources configured |
| Static UI layout verifier | **PASS** — 9 aspect-ratio / GUI-scale combinations; button and transition geometry checked |
| JAR verifier | **PASS** — 2,122,060 bytes; 27 compiled classes; required loader metadata/resources present; no duplicate ZIP entries |
| SHA-256 | `b66bc432c4f10ab6cab187ae85a51ac7ef0f76efa09cfa1dd1082e1005f8b151` |

Deliverables:

- `artifacts/anomalous-classes-0.1.0-alpha.1.jar`
- `artifacts/anomalous-classes-0.1.0-alpha.1.jar.sha256`
- `artifacts/raw-logs.zip` (prior sanitized logs plus this clean build, layout matrix, and JAR check)
- `ui/reviews/2026-11-08/STATIC_LAYOUT_REVIEW.md`

The build retains two existing removal/deprecation warnings for `EventBusSubscriber.bus()` and `EventBusSubscriber.Bus` in `client/ClientModEvents.java:12`. They are unrelated to this UI change.

## UI and runtime checks

- **STATIC REVIEW — PASS:** Verified the exact Minecraft 1.21.1 `Screen.render()` and `Screen.renderBackground()` bytecode from the configured NeoForge runtime artifact. `Screen.render()` calls `renderBackground()` before registered widgets; `renderBackground()` processes the blur. The former custom-render + `super.render()` sequence duplicated that background pass. The fixed screen draws the background once, then its custom content, then the existing registered widgets.
- **STATIC LAYOUT — PASS:** Tested 16:9, 16:10, and 4:3 at GUI Scale 2, 3, and 4. Korean and English text appear in static previews. The runtime UI uses Minecraft font measurement and wrapping; static previews are not Minecraft captures.
- **STATIC RESOURCE — PASS:** Six unchanged icons are 96×96 transparent PNGs; six unchanged card images are 512×288. UI textures have no `.mcmeta`; Minecraft's 1.21.1 texture metadata defaults `blur` to false, and `SimpleTexture` passes that to nearest filtering.
- **Minecraft client — RUNNING:** Started this project's `runClient` after the successful build. Its log shows the `anomalousclasses` resource pack reload and client render-thread initialization. The development window remains open. The class-selection screen was not opened by the agent.
- **CLIENT TESTED — NOT RUN:** No CUA or in-game interaction was used. Actual GUI rendering, click/focus behavior, transition motion, and Korean glyph appearance remain for user review.
- Datagen: **NOT CONFIGURED / NOT RUN**.
- GameTest: **NOT CONFIGURED / NOT RUN**.
- Dedicated server smoke boot: **NOT RUN** — did not create or accept a server EULA.
- Gameplay, combat, balance, progression, and AI: **NOT RUN** and outside the visual task.

The client log also contains non-fatal NeoForge/Minecraft warnings about `union:` asset URL schemas, built-in goat-horn sound entries, and an unrelated entity shader sampler. No UI-specific crash was observed before resource loading completed.

## GitHub main

The UI follow-up is committed directly to `main` in accordance with the repository policy. The source baseline above is the fetched `origin/main` before this change. The task completion report identifies the pushed commit and the workflow status associated with it.

# Build and runtime report

Date: 2026-11-08

Project: `anomalous-classes`

Minecraft: 1.21.1 · NeoForge 21.1.244 · Java 21

## Result

**BUILD PASS.** The UI follow-up compiled and produced a non-empty JAR. The packaging verifier confirmed loader metadata, manifest version, compiled classes, language files, six card images, six transparent icons, required data, and absence of archived artwork or development files from the JAR.

- JAR: `artifacts/anomalous-classes-0.1.0-alpha.1.jar`
- SHA-256: `artifacts/anomalous-classes-0.1.0-alpha.1.jar.sha256`
- Sanitized build logs: `artifacts/raw-logs.zip`
- Static layout record: `ui/reviews/2026-11-08/STATIC_LAYOUT_REVIEW.md`

## Commands and checks

| Command/check | Result |
|---|---|
| `java -version` | OpenJDK 21.0.10 |
| `./gradlew --version` | Gradle 8.8 on Java 21.0.10 |
| `./gradlew clean build --console=plain` | **PASS** — 5 tasks executed; Gradle reports no test sources |
| Static layout verifier | **PASS** — 9 aspect-ratio / GUI-scale combinations; action geometry also checked with 1–4 description lines and 0–2 status lines |
| `python tools/verify_jar.py build/libs/anomalous-classes-0.1.0-alpha.1.jar` | **PASS** — 2,119,576 bytes; 27 compiled classes; no duplicate ZIP entries |
| SHA-256 generation | **PASS** — checksum file included with artifact |

The first clean-build attempt could not remove `build/` because its console log was being written inside the directory Gradle was cleaning. The complete failed-attempt log is preserved in the log archive. The log was moved outside `build/`, and the exact build then passed.

The successful build retains two existing deprecation warnings in `client/ClientModEvents.java:12` for `EventBusSubscriber.bus()` and `EventBusSubscriber.Bus`; neither is part of this UI change.

## Runtime and other checks

- Datagen: **NOT CONFIGURED / NOT RUN**.
- GameTest: **NOT CONFIGURED / NOT RUN**.
- Dedicated server smoke boot: **NOT RUN** — `run/server/eula.txt` is absent; EULA acceptance was not created automatically.
- Minecraft client load, in-game screenshot, pointer interaction, GUI-scale visual check, and card-transition interaction: **NOT RUN**. This task used static code, layout geometry, and image resources only. The actual-client review remains for the user.
- Gameplay, combat, balance, progression, and AI: **NOT RUN** and outside this task.

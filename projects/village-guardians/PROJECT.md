# Village Guardians

공용 Minecraft Java 모드 빌드 저장소에서 **Village Guardians(마을 지키기)**만 담당하는 프로젝트 명세다.

## 프로젝트 식별자

- Project slug: `village-guardians`
- Mod ID / namespace: `villageguardians`
- Java package root: `kr.moonseungjun.villageguardians`
- Source version: `0.9.4-alpha.1`
- Expected runtime JAR: `villageguardians-0.9.4-alpha.1.jar`

## 빌드 환경

- Minecraft Java: `26.2`
- NeoForge: `26.2.0.38-beta`
- Java toolchain: `25`
- Gradle used by CI: `9.2.1`
- ModDevGradle: `2.0.143`
- Runtime dependency: NeoForge
- `e4mc` is optional and is not bundled in this project.

## Source snapshot

- Snapshot date: `2026-07-31`
- Source archive: `source/source.tar.xz.b64.part00` through `part13`
- Reconstructed `source.tar.xz` SHA-256: `4fc0c1f550daae0f2e618e7119ef1a02c698b34bf0c2bb57ade6ef6481aaeb6f`
- Original source ZIP SHA-256: `67b619dec3bb9125ed4ad167d8e3f712088cf13cc08d51c3ab041944ba0436b2`

The baseline source did not include a complete Gradle Wrapper (`gradlew`, `gradlew.bat`, and `gradle-wrapper.jar` were absent). The dedicated CI workflow therefore pins and installs Gradle `9.2.1` directly instead of pretending the incomplete wrapper is usable.

## Compatibility invariants

Do not change or replace the following merely to make compilation easier:

- mod ID and namespace
- Java package root
- registrations and registry names
- saved-data keys and existing world compatibility
- networking action identifiers and packet schema
- implemented gameplay systems or assets

Compilation errors must be fixed against the real Java 25 / Minecraft 26.2 / NeoForge APIs. Do not stub, delete, bypass, or silently reduce features.

## CI scope

The dedicated workflow performs:

1. source-part concatenation and Base64 decoding
2. source archive SHA-256 verification and XZ integrity verification
3. clean Java 25 / NeoForge Gradle build
4. runtime JAR selection and structural validation
5. JAR SHA-256 and build report generation
6. GitHub Actions artifact and version-specific release asset publication

The following are intentionally **not run** until separately authorized:

- gameplay or manual client testing
- dedicated server launch
- GameTest
- datagen

Runtime JARs and build caches must not be committed to this repository. They are published only as verified workflow artifacts or release assets.

## Ownership boundary

Changes for this mod belong only in:

- `projects/village-guardians/**`
- `.github/workflows/build-village-guardians.yml`

Do not modify another mod's project directory, workflow, release tag, build result, or artifact name while working on Village Guardians.

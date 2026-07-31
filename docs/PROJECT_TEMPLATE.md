# Project Template

새 모드를 `projects/<slug>/`에 추가할 때 이 내용을 복사해 `PROJECT.md`로 사용한다.

```md
# <정식 모드명>

## Identity

- Slug: `<folder-name>`
- Mod ID: `<modid>`
- Namespace: `<namespace>`
- Current version: `<version>`
- Final JAR: `<filename>.jar`

## Toolchain

- Minecraft: `<version>`
- Java: `<version>`
- Loader: `<NeoForge/Fabric/etc.>`
- Loader version: `<exact-version>`
- Gradle: `<version>`
- Build plugin: `<name and exact version>`

## Compatibility Contract

- Existing-world compatibility: `<requirements>`
- IDs that must never change: `<list>`
- Saved-data keys that must never change: `<list>`
- Required dependencies: `<list or none>`
- Optional external mods: `<list or none>`
- Forbidden bundled dependencies: `<list or none>`

## Build Tasks

- Clean build: `<task>`
- Datagen: `<task or NOT AVAILABLE>`
- Unit tests: `<task or NOT AVAILABLE>`
- GameTest: `<task or NOT AVAILABLE>`
- Dedicated server smoke test: `<task or procedure>`
- Client smoke test: `<task or procedure>`
- Local modpack task: `<task or NOT AVAILABLE>`

## Final Validation

- Metadata path: `<path inside JAR>`
- Main class package: `<package path>`
- Assets path: `assets/<modid>`
- Data path: `data/<modid>`
- Extra required entries: `<list>`

## Current Source Baseline

- Source archive or commit: `<name/SHA>`
- Baseline date: `<YYYY-MM-DD>`
- Notes: `<important handoff details>`
```

## 추가 체크리스트

- [ ] 최신 기준 소스를 직접 확인했다.
- [ ] 버전 문자열이 모든 설정과 문서에서 일치한다.
- [ ] mod id와 저장 키를 변경하지 않았다.
- [ ] 프로젝트 전용 workflow가 있다.
- [ ] 빌드 결과와 로그 artifact 이름이 다른 프로젝트와 충돌하지 않는다.
- [ ] 공개 저장소에 올릴 수 없는 비밀·리소스가 없다.

# Riftfrontier — 균열 개척기

- Slug: `riftfrontier`
- Mod ID: `riftfrontier`
- Namespace: `riftfrontier`
- Mod version: `0.1.0-alpha.1`
- Minecraft: `26.2`
- Java: `25`
- Loader: `NeoForge`
- Loader version: `26.2.0.38-beta`
- Gradle: `9.2.1`
- Build plugin: `ModDevGradle 2.0.143`
- Final JAR: `riftfrontier-0.1.0-alpha.1.jar`
- Existing-world compatibility: 첫 플레이어블 알파 이전에는 세이브 스키마를 실험할 수 있다. 첫 플레이어블 알파 이후부터 registry ID, 저장 키, content ID를 고정하고 migration을 우선한다.
- Required dependencies: Minecraft, NeoForge
- Optional external mods/libraries: 현재 26.2 호환성과 유지보수 상태를 다시 검증한 뒤 목적별로 단일 선택한다. 애니메이션은 GeckoLib 계열을 우선 검토하고, 복잡한 AI/UI 라이브러리는 실제 필요가 생긴 뒤 추가한다.
- Forbidden bundled dependencies: Minecraft 원본 파일, NeoForge 배포 파일, 외부 모드 JAR, 재배포 권한이 불명확한 모델·텍스처·음원·UI 자산
- Datagen task: `runData`
- GameTest task: `runGameTestServer` / native 26.2 test-function registry + data-driven `test_instance`
- Server smoke-test task: CI `Dedicated server smoke` → `runServer`, ready/content/authoritative-world marker 검증
- Client smoke-test task: CI `Client smoke under virtual display` → Xvfb `runClient`, init/fatal-crash marker 검증

## 프로젝트 정체성

Riftfrontier는 단순한 RPG 콘텐츠 팩이나 차원 추가 모드가 아니다.

# Arcane Circle Lab

- Slug: arcane-circle
- Mod ID: arcanecircle
- Namespace: arcanecircle
- Mod version: 0.1.0-alpha.1
- Minecraft: 26.2
- Java: 25
- Loader: NeoForge
- Loader version: 26.2.0.38-beta
- Gradle: 9.2.1 wrapper
- Build plugin: net.neoforged.moddev 2.0.143
- Final JAR: arcanecircle-0.1.0-alpha.1.jar
- Existing-world compatibility: 새 저장 데이터 키만 추가하며 기존 바닐라 월드를 변경하지 않음
- Required dependencies: Minecraft 26.2, NeoForge 26.2.0.38-beta 이상 호환 빌드
- Optional external mods: e4mc
- Forbidden bundled dependencies: Minecraft, NeoForge, 외부 쉐이더·렌더러
- Datagen task: runData 설정됨, 첫 테스트에서는 수동 리소스를 사용
- GameTest task: 없음
- Server smoke-test task: runServer
- Client smoke-test task: runClient

## 테스트 범위

조합 마법진 블록에 세 촉매를 순서대로 넣는다. 유효한 세 조합은 불꽃 신성, 서리 봉인, 비전 파동이다.
조합 상태는 월드 저장 데이터에 보존된다. 빈손 우클릭은 발동 또는 상태 확인, 웅크리기+빈손 우클릭은 촉매 환불과 초기화다.

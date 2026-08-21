# Survival Ascension

- Slug: `survival-ascension`
- Mod ID: `survivalascension`
- Namespace: `survivalascension`
- Mod version: `0.1.0-alpha.1`
- Minecraft: `26.2`
- Java: `25`
- Loader: `NeoForge`
- Loader version: `26.2.0.38-beta`
- Gradle: `9.2.1`
- Build plugin: `net.neoforged.moddev 2.0.143`
- Final JAR: `survivalascension-0.1.0-alpha.1.jar`
- Existing-world compatibility: 기존 26.2 월드에 추가 가능. alpha.1은 월드 생성 요소를 추가하지 않고 플레이어 성장 SavedData만 저장한다.
- Required dependencies: Minecraft 26.2, NeoForge 26.2.0.38-beta 이상
- Optional external mods: 없음
- Forbidden bundled dependencies: Project MMO 및 타 모드 코드·리소스·에셋 일체
- Datagen task: `NOT IMPLEMENTED`
- GameTest task: `NOT IMPLEMENTED`
- Server smoke-test task: Gradle `runServer` 수동 실행 가능, canonical CI에서는 alpha.1 기준 `NOT RUN`
- Client smoke-test task: Gradle `runClient` 수동 실행 가능, canonical CI에서는 alpha.1 기준 `NOT RUN`

## 정체성

Survival Ascension은 바닐라 서바이벌의 성장 체급을 크게 확장한다. 단순히 공격력·채굴속도 숫자만 올리는 것이 아니라, 숙련이 쌓일수록 플레이어가 한 번에 처리할 수 있는 작업의 규모 자체가 커지는 것을 핵심으로 한다.

외부 성장형 모드에서 검증된 **행동 기반 숙련 → 단계별 능력 해금 → 후반 작업 규모 확대**라는 설계 원리만 참고하며, 타 모드의 소스 코드·에셋·데이터를 복제하거나 번들하지 않는다.

## alpha.1 채굴 계약

- 채굴 레벨: 0~100
- 곡괭이로 정상 채굴 가능한 블록을 파괴하면 채굴 XP 획득
- 귀중 광석 태그는 일반 암석보다 많은 XP 지급
- 채굴속도 배율: `1 + 0.03L + 0.0004L²`, L은 채굴 레벨
- 레벨 10: 3×3 채굴
- 레벨 30: 5×5 채굴
- 레벨 60: 7×7 채굴
- 웅크리기: 항상 1×1 정밀 채굴
- 광역 채굴은 `ServerPlayerGameMode.destroyBlock`을 사용해 각 추가 블록도 정상 플레이어 파괴 이벤트·드랍·도구 내구도 경로를 통과시킨다.
- 블록 엔티티는 alpha.1 광역 채굴에서 제외한다.
- 중심 블록보다 지나치게 단단한 블록은 광역 채굴에서 제외해 값싼 블록 하나로 고급 블록을 즉시 캐는 우회를 막는다.

## 다음 확장축

동일한 성장 엔진 위에 벌목, 농사, 전투, 건축, 이동을 순차적으로 확장한다. 각 분야는 후반으로 갈수록 단순 수치 증가가 아니라 작업 단위와 플레이 방식이 달라져야 한다.

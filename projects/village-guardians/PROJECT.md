# Village Guardians — 마을지키기

- Slug: `village-guardians`
- Mod ID: `villageguardians`
- Namespace: `villageguardians`
- Mod version: `0.2.0-alpha.1`
- Minecraft: `26.2`
- Java: `25`
- Loader: `NeoForge`
- Loader version: `26.2.0.37-beta`
- Gradle: `9.2.1`
- Build plugin: `ModDevGradle 2.0.143`
- Final JAR: `villageguardians-0.2.0-alpha.1.jar`
- Existing-world compatibility: `village_council` 저장 키와 기존 0.1 통치 데이터를 유지하며 RPG 필드는 선택값으로 추가한다.
- Required dependencies: Minecraft, NeoForge
- Optional external mods: e4mc 등 서버 연결 보조 모드
- Forbidden bundled dependencies: Minecraft 원본 파일, NeoForge 배포 파일, 외부 모드 JAR
- Datagen task: `runData`
- GameTest task: 미구현
- Server smoke-test task: `runServer`
- Client smoke-test task: `runClient`

## 절대 핵심 규칙

1. 한 마을에는 촌장 한 명만 존재한다.
2. 촌장 직책과 실무 역할은 별개다. 촌장도 실무 역할 하나를 가질 수 있다.
3. 멀티플레이에서는 플레이어가 경비대장, 건축가, 보급관, 정찰병, 농업관, 의무관 중 하나를 맡을 수 있다.
4. 마을 전체에 영향을 주는 결정은 투표로 처리한다.
5. 바닐라 자연 일주기는 항상 멈춘다.
6. 시간은 실제 틱 경과가 아니라 통과된 마을 결정에 의해서만 다음 단계로 넘어간다.
7. RPG 성장은 수치만 표시하지 않고 실제 공격, 생존력, 추가 체력과 역할 스킬에 반영한다.
8. UI는 기능을 가리는 임시 회색 화면으로 만들지 않는다. 제대로 디자인할 시점 전에는 명령 기반으로 유지한다.
9. 최종 배포물은 소스 ZIP이 아닌 실제 컴파일된 실행용 JAR이다.

## 0.2 RPG 알파 목표

- 플레이어별 레벨과 경험치의 월드 영구 저장
- 적대 몬스터 처치 경험치
- 최고 레벨 30
- 레벨별 공격 피해 증가와 받는 피해 감소
- 3레벨 단위 추가 체력 증가
- 5레벨 단위 전투력 급상승 구간
- 역할별 첫 액티브 스킬과 레벨 비례 강화
- 스킬 재사용 대기시간
- 인게임 테스트용 RPG 상태와 경험치 지급 명령

30레벨 기준 목표치는 기본 공격 약 6.23배, 받는 피해 28%, 추가 체력 36포인트다. 이후 습격 난도도 이 성장 폭을 기준으로 함께 강화한다.

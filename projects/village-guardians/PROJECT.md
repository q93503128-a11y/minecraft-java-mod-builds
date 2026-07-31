# Village Guardians — 마을지키기

- Slug: `village-guardians`
- Mod ID: `villageguardians`
- Namespace: `villageguardians`
- Mod version: `0.1.0-alpha.1`
- Minecraft: `26.2`
- Java: `25`
- Loader: `NeoForge`
- Loader version: `26.2.0.37-beta`
- Gradle: `9.2.1`
- Build plugin: `ModDevGradle 2.0.143`
- Final JAR: `villageguardians-0.1.0-alpha.1.jar`
- Existing-world compatibility: 새 프로젝트. 이후 같은 mod ID와 저장 키를 유지한다.
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
7. UI는 기능을 가리는 임시 회색 화면으로 만들지 않는다. 제대로 디자인할 시점 전에는 명령 기반으로 유지한다.
8. 최종 배포물은 소스 ZIP이 아닌 실제 컴파일된 실행용 JAR이다.

## 0.1 목표

- 서버 시작 시 `doDaylightCycle=false` 적용
- 첫 접속자를 임시 초대 촌장으로 지정
- 플레이어별 실무 역할 선택
- 촌장의 시간 진행 안건 발의
- 온라인 플레이어 과반수 투표
- 안건 통과 시에만 아침 → 낮 → 저녁 → 밤 → 다음 날 아침 순으로 진행
- 현재 촌장, 역할, 날짜, 시간 단계, 투표 현황 조회

이 단계는 전투·습격·경비병·건설 시스템을 얹기 위한 통치 코어다.

# Countryside Days — 시골식당

- Slug: `countryside-days`
- Mod ID: `countrysidedays`
- Namespace: `countrysidedays`
- Mod version: `0.1.0-alpha.3`
- Minecraft: `26.2`
- Java: `25`
- Loader: `NeoForge`
- Loader version: `26.2.0.38-beta`
- Gradle: `9.2.1`
- Build plugin: `ModDevGradle 2.0.143`
- Final JAR: `countrysidedays-0.1.0-alpha.3.jar`
- Existing-world compatibility: 신규 프로젝트이므로 폐기된 옛 Countryside Days 월드와 호환을 약속하지 않는다. 공개 알파부터 등록 ID와 저장 키를 고정한다.
- Required dependencies: Minecraft, NeoForge
- Optional external mods: e4mc 등 서버 연결 보조 모드
- Forbidden bundled dependencies: Minecraft 원본 파일, NeoForge 배포 파일, 외부 모드 JAR, 재배포 권한이 없는 셰이더·텍스처·음원
- Datagen task: `runData`
- GameTest task: `runGameTestServer`
- Server smoke-test task: `runServer`
- Client smoke-test task: `runClient` with a virtual display in CI

## 프로젝트 선언

이 프로젝트는 2026-07-31 이전에 폐기된 Countryside Days 소스와 설계를 복구하거나 재사용하지 않고 처음부터 새로 만든다.

게임의 정체성은 식당 경영 시뮬레이션에 한정되지 않는다. 플레이어가 매우 넓고 현실감 있는 시골에서 생활하고, 사람들과 관계를 맺고, 농사·채집·낚시·배달·요리·꾸미기·탐험을 즐기는 힐링 생활 게임이다. 식당은 그 생활과 마을 경제, 주민 관계가 만나는 중심 거점이다.

## 절대 핵심 규칙

1. 최종 배포물은 소스 ZIP이 아니라 실제 컴파일된 NeoForge 실행용 JAR이다.
2. 시골 지역은 작고 반복적인 장식 마을이 아니라, 이동과 생활 동선이 느껴지는 넓은 생활권으로 만든다.
3. 시골 밖의 도시·대륙·대형 세계관은 개발 우선순위에서 제외한다.
4. 식당은 핵심 축이지만 플레이어의 하루를 강제로 식당 업무에만 묶지 않는다.
5. 농사, 채집, 낚시, 동물 돌보기, 주민 부탁, 배달, 주거 꾸미기, 산책과 탐험이 서로 연결되어야 한다.
6. 콘텐츠는 수치 메뉴만 늘리지 않고 실제 월드 안에서 보이고 만지고 이동하며 경험하게 만든다.
7. 비주얼은 따뜻한 자연광, 안개, 계절색, 물과 바람, 풀과 나뭇잎 움직임, 실내 조명으로 힐링 분위기를 만든다.
8. UI는 기본 회색 화면을 그대로 쓰지 않고, 목재·종이·천·식물 모티프를 사용한 전용 디자인을 적용한다.
9. 다른 프로젝트 폴더를 수정하거나 삭제하지 않는다.
10. 빌드·서버·클라이언트 검증을 하지 않은 상태를 완성이라고 부르지 않는다.

## 0.1.0-alpha.3 구현 범위

- 공식 NeoForge 26.2 + Java 25 + ModDevGradle 프로젝트 골격
- 시골 주방 작업대와 들나물 → 민물고기 → 시골 전골 조리 루프
- 풀·고사리 채집 및 낚시를 통한 실제 재료 획득
- 안전한 평탄 지형 탐색과 첫 시골 거점 자동 생성
- 식당, 주방, 밭, 관개 수로, 우물, 과수목, 마당, 진입로
- 월드 공용 식당·조리·손님·마을 동전 진행 저장
- 주민 `복순 할머니`와 하루 손님 `나들이 손님 민수`
- 전골 제공 → 마을 동전과 경험치 보상
- 플레이어별 최초 준비물 지급과 멀티 공용 식당 진행
- 인벤토리 상태에 따라 다음 행동을 안내하는 첫 전용 HUD
- 한국어·영어 번역, 블록·아이템 모델, 조합법
- 필수 GameTest 3개, 데이터 로드 검사, 전용 서버 및 가상 디스플레이 클라이언트 부팅 검사
- 실행 JAR과 SHA-256을 생성하는 GitHub Actions

## 아직 개발할 핵심 범위

- 2048×2048 이상으로 확장되는 실제 시골 생활권과 여러 지역
- 주민 이동 일정, 호감도, 다양한 손님과 부탁
- 작물·축산·배달·주거 꾸미기·탐험의 장기 루프
- 마을 동전 상점과 식당 확장
- 전용 텍스처·입체 모델·환경음
- 낮·노을·밤·안개·물·바람을 묶는 본격 환경 렌더링
- 사람의 눈으로 확인한 UI·거점·조명·동선 품질 개선

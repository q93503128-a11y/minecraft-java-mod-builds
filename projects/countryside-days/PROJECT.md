# Countryside Days — 시골식당

- Slug: `countryside-days`
- Mod ID: `countrysidedays`
- Namespace: `countrysidedays`
- Mod version: `0.1.0-alpha.2`
- Minecraft: `26.2`
- Java: `25`
- Loader: `NeoForge`
- Loader version: `26.2.0.38-beta`
- Gradle: `9.2.1`
- Build plugin: `ModDevGradle 2.0.143`
- Final JAR: `countrysidedays-0.1.0-alpha.2.jar`
- Existing-world compatibility: 신규 프로젝트이므로 폐기된 옛 Countryside Days 월드와 호환을 약속하지 않는다. 공개 알파부터 등록 ID와 저장 키를 고정한다.
- Required dependencies: Minecraft, NeoForge
- Optional external mods: e4mc 등 서버 연결 보조 모드
- Forbidden bundled dependencies: Minecraft 원본 파일, NeoForge 배포 파일, 외부 모드 JAR, 재배포 권한이 없는 셰이더·텍스처·음원
- Datagen task: `runData`
- GameTest task: 코어 월드 시스템 구현 후 추가
- Server smoke-test task: `runServer`
- Client smoke-test task: `runClient`

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

## 0.1.0-alpha.2 구현 범위

- 공식 NeoForge 26.2 ModDevGradle 프로젝트 골격
- 모드 진입점과 프로젝트별 레지스트리 분리
- 시골 주방 작업대 블록
- 들나물, 민물고기, 시골 전골, 요리 수첩 아이템
- 전용 크리에이티브 탭과 한국어·영어 번역
- 현대 아이템 모델 정의와 기본 블록 모델
- 조리 재료 아이템 태그
- JAR 내부 검증 스크립트와 GitHub Actions 빌드

## 첫 플레이어블 알파 목표

- 새 월드 시작 시 시골 생활권의 기준 지점과 식당 거점 생성
- 식당 내부와 주방의 실제 상호작용 기반
- 재료 수집 → 손질 → 조리 → 주민 제공의 한 사이클
- 농장, 숲길, 냇가, 마을 중심, 외딴 주택을 잇는 기본 시골 동선
- 주민 일정과 최소 관계도
- 낮·노을·밤의 분위기 변화와 시골 환경음
- 전용 HUD 또는 메뉴의 첫 디자인 패스
- 멀티플레이에서 함께 재료를 모으고 식당을 운영할 수 있는 기본 동기화

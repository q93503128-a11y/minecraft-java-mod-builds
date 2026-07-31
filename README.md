# Minecraft Java Mod Builds

문승준의 여러 **Minecraft Java Edition 모드**를 프로젝트별로 개발·빌드·검증하기 위한 공용 저장소다.

## 현재 상태

- 기본 브랜치: `main`
- 모든 모드는 `projects/<project-slug>/` 아래에서 분리한다.
- 2026-07-31 이전에 저장되어 있던 Countryside Days, Wildbound, 기존 Village Guardians 소스·로그·워크플로는 폐기했다.
- 폐기된 프로젝트의 코드나 설계를 새 작업의 기준으로 사용하지 않는다.
- 현재 활성 프로젝트는 **Village Guardians(마을지키기)**, **Countryside Days(시골식당)**, **Living Kingdoms(살아있는 왕국)**다.

## 활성 프로젝트

### Village Guardians

- 경로: [`projects/village-guardians/`](./projects/village-guardians/)
- 현재 버전: `0.2.0-alpha.1`
- 촌장, 역할, 투표, 정지형 시간, 강한 RPG 성장을 결합한 마을 방어 모드
- 아직 실제 Java 25 + NeoForge 26.2 빌드와 게임 실행 검증 전이므로 배포 완료 상태는 아니다.

### Countryside Days

- 경로: [`projects/countryside-days/`](./projects/countryside-days/)
- 현재 버전: `0.1.0-alpha.2`
- 넓고 현실적인 시골 생활권에서 식당을 중심 거점으로 농사, 채집, 낚시, 주민 관계, 배달, 꾸미기, 탐험을 즐기는 힐링 생활 모드
- 폐기된 옛 Countryside Days 소스와 설계를 복구하지 않고 2026-07-31부터 새로 시작했다.
- Java 25 + NeoForge 26.2 기반 골격, 첫 주방 블록·재료·요리·전용 탭·리소스가 구현되었다.
- 월드 공용 식당 기준점, 작업대별 조리 상태, 누적 요리 수와 들나물→민물고기→시골 전골의 첫 조리 루프가 구현되었다.
- Java 25 clean build, datagen, JAR 구조 검사는 성공했다. GameTest와 전용 서버·클라이언트 실행 검증 전이므로 정식 테스트 배포 상태는 아니다.

### Living Kingdoms

- 경로: [`projects/living-kingdoms/`](./projects/living-kingdoms/)
- 현재 버전: `0.1.0-alpha.1`
- 설계된 대륙과 왕국에서 주민, 낚시꾼, 장인, 학자, 상인, 방랑자, 부족민, 전사, 영웅과 통치자 등 무엇이든 되어 살아가는 대형 판타지 세계 시뮬레이션 RPG
- 종족·출신 세력·사회 배경·시작 거주지를 분리한 초기 출신 카탈로그와 세계 설계 문서가 구현되어 있다.
- 아직 Gradle wrapper, 영구 저장, 캐릭터 생성 UI와 실제 빌드 검증이 남아 있으므로 실행 가능한 JAR은 없다.

## 작업자가 먼저 읽을 문서

1. [`AGENTS.md`](./AGENTS.md)
2. [`docs/BUILD_STANDARD.md`](./docs/BUILD_STANDARD.md)
3. 대상 프로젝트의 `PROJECT.md`

## 공용 저장소 원칙

- 특정 채팅방 하나의 전용 저장소가 아니다.
- 문승준의 다른 Minecraft Java 모드 채팅에서도 같은 저장소를 공용으로 사용한다.
- 한 프로젝트를 작업할 때 다른 프로젝트 폴더를 수정하거나 삭제하지 않는다.
- 사용자가 별도로 요청하지 않으면 `main`에 직접 반영하고 임의 브랜치나 PR을 만들지 않는다.
- 실제 빌드·실행 검증을 하지 않은 결과를 완료라고 주장하지 않는다.

## 기본 구조

```text
minecraft-java-mod-builds/
├─ projects/
│  ├─ village-guardians/
│  ├─ countryside-days/
│  └─ living-kingdoms/
├─ docs/
│  └─ BUILD_STANDARD.md
├─ AGENTS.md
└─ README.md
```

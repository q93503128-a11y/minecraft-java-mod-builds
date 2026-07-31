# Minecraft Java Mod Builds

문승준의 여러 **Minecraft Java Edition 모드**를 프로젝트별로 개발·빌드·검증하기 위한 공용 저장소다.

## 현재 상태

- 기본 브랜치: `main`
- 모든 모드는 `projects/<project-slug>/` 아래에서 분리한다.
- 2026-07-31 이전에 저장되어 있던 Countryside Days, Wildbound, 기존 Village Guardians 소스·로그·워크플로는 폐기했다.
- 폐기된 프로젝트의 코드나 설계를 새 작업의 기준으로 사용하지 않는다.
- 현재 활성 프로젝트는 **Village Guardians(마을지키기)**와 새로 시작한 **Countryside Days(시골식당)**다.

## 활성 프로젝트

### Village Guardians

- 경로: [`projects/village-guardians/`](./projects/village-guardians/)
- 현재 버전: `0.2.0-alpha.1`
- 촌장, 역할, 투표, 정지형 시간, 강한 RPG 성장을 결합한 마을 방어 모드
- 아직 실제 Java 25 + NeoForge 26.2 빌드와 게임 실행 검증 전이므로 배포 완료 상태는 아니다.

### Countryside Days

- 경로: [`projects/countryside-days/`](./projects/countryside-days/)
- 현재 버전: `0.1.0-alpha.1`
- 넓고 현실적인 시골 생활권에서 식당을 중심 거점으로 농사, 채집, 낚시, 주민 관계, 배달, 꾸미기, 탐험을 즐기는 힐링 생활 모드
- 폐기된 옛 Countryside Days 소스와 설계를 복구하지 않고 2026-07-31부터 새로 시작했다.
- 현재는 프로젝트 계약과 게임 비전만 고정된 단계이며 실행 가능한 JAR은 아직 없다.

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
│  └─ countryside-days/
├─ docs/
│  └─ BUILD_STANDARD.md
├─ AGENTS.md
└─ README.md
```

# Minecraft Java Mod Builds

문승준의 여러 **Minecraft Java Edition 모드**를 프로젝트별로 개발·빌드·검증하기 위한 공용 저장소다.

## 현재 상태

- 기본 브랜치: `main`
- 모든 모드는 `projects/<project-slug>/` 아래에서 분리한다.
- 2026-07-31 이전에 저장되어 있던 Countryside Days, Wildbound, 기존 Village Guardians 소스·로그·워크플로는 폐기했다.
- 폐기된 프로젝트의 코드나 설계를 새 작업의 기준으로 사용하지 않는다.
- 현재 새로 시작할 대상은 **Village Guardians(마을지키기)**이며, 기존 구현을 복원하지 않고 처음부터 설계·구현한다.

## 작업자가 먼저 읽을 문서

1. [`AGENTS.md`](./AGENTS.md)
2. [`docs/BUILD_STANDARD.md`](./docs/BUILD_STANDARD.md)
3. 작업 대상 프로젝트가 생긴 뒤 해당 폴더의 `PROJECT.md`

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
│  └─ <project-slug>/
├─ docs/
│  └─ BUILD_STANDARD.md
├─ AGENTS.md
└─ README.md
```

현재 `projects/`에는 유지할 기존 프로젝트가 없다. 새 프로젝트는 과거 소스를 복원하거나 복제하지 말고 새로운 명세와 소스에서 시작한다.

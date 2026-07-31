# Minecraft Java Mod Builds

이 저장소는 여러 **Minecraft Java Edition 모드의 실제 빌드·검사·실행용 JAR 산출**을 위한 공개 빌드 허브다.

저장소 주소만 전달받은 작업자는 먼저 이 문서와 [`AGENTS.md`](./AGENTS.md), [`docs/BUILD_STANDARD.md`](./docs/BUILD_STANDARD.md)를 읽고 그대로 작업해야 한다.

> **중요:** 이 저장소는 단순한 소스 보관소가 아니다. 실제 Java/Gradle/NeoForge 환경에서 컴파일하고, 생성된 JAR의 내부 구조와 실행 가능성을 검증하는 것이 목적이다.

## 핵심 목적

- 여러 Minecraft Java 모드를 한 공개 저장소에서 폴더별로 관리한다.
- GitHub Actions의 표준 공개 저장소 러너를 이용해 실제 빌드를 수행한다.
- 컴파일 오류를 로그의 가장 첫 원인부터 수정한다.
- 소스 ZIP이나 이름만 `.jar`인 파일이 아닌 **컴파일된 `.class`가 포함된 실행용 JAR**을 산출한다.
- 빌드 결과, SHA-256, 검사 보고서와 원본 로그를 Actions artifact로 남긴다.

## 저장소 구조

각 모드는 반드시 서로 독립된 폴더에 둔다.

```text
minecraft-java-mod-builds/
├─ projects/
│  ├─ countryside-days/
│  ├─ living-kingdoms/
│  ├─ village-guardians/
│  └─ <other-mod>/
├─ .github/
│  └─ workflows/
│     ├─ build-countryside-days.yml
│     └─ build-<other-mod>.yml
├─ docs/
│  └─ BUILD_STANDARD.md
├─ AGENTS.md
└─ README.md
```

한 모드를 수정할 때는 **해당 프로젝트 폴더와 그 프로젝트 전용 workflow만 수정**한다. 다른 모드의 코드, 리소스, 등록 ID, 버전, 저장 데이터 키를 건드리지 않는다.

## 다른 채팅 또는 에이전트가 이 주소만 받았을 때 해야 할 일

1. 저장소가 공개 상태인지, 기본 브랜치가 `main`인지 확인한다.
2. `README.md`, `AGENTS.md`, `docs/BUILD_STANDARD.md`를 전부 읽는다.
3. 사용자가 지정한 모드 폴더와 최신 기준 파일을 확인한다.
4. 기존 구현을 전부 읽고, 축소판이나 새 모드로 재구현하지 않는다.
5. 요구된 Java·Minecraft·로더·Gradle 버전을 공식 자료와 실제 Maven 존재 여부로 검증한다.
6. 프로젝트 전용 GitHub Actions workflow를 만들거나 수정한다.
7. 실제 `clean build`를 실행하고 실패 시 첫 원인 오류부터 수정한다.
8. 가능한 범위에서 datagen, GameTest, 서버 부팅, 클라이언트 로딩을 검사한다.
9. 최종 JAR을 ZIP으로 열어 메타데이터, `.class`, assets, data를 확인한다.
10. JAR, SHA-256, 보고서, 전체 로그를 artifact로 업로드하고 사용자에게 직접 전달한다.

## 기본 작업 원칙

- 기본 작업 브랜치는 `main`이다. 사용자가 별도로 요청하지 않으면 임의 브랜치나 PR을 만들지 않는다.
- 최신 기준 파일이 제공되면 반드시 직접 압축 해제하고 그 내용을 절대 기준으로 사용한다.
- 기존 mod id, 네임스페이스, 등록 ID, 저장 키와 기존 월드 호환성을 유지한다.
- 기능 삭제, 대량 주석 처리, 임시 스텁, 가짜 Minecraft/NeoForge API로 컴파일 오류를 숨기지 않는다.
- 실행하지 않은 빌드·테스트·부팅을 성공했다고 주장하지 않는다.
- 최종 JAR에 소스 코드, 개발 도구, 로그, `.github`, 보고서가 섞이지 않도록 검사한다.
- 빌드 산출물은 Git에 계속 커밋하지 않고 Actions artifact로 제공한다.

## 공개 저장소 주의사항

이 저장소의 소스, 커밋 기록, Actions 설정과 공개 로그는 누구나 볼 수 있다.

절대 올리면 안 되는 것:

- API 키, 토큰, 비밀번호, 쿠키, 계정 정보
- 개인 정보나 로컬 경로가 포함된 민감한 로그
- 재배포 권한이 없는 유료·비공개 리소스
- Minecraft 원본 게임 파일 또는 배포가 금지된 라이브러리
- `.env`, 인증서, SSH 키, 서비스 계정 파일

비밀이 한 번 커밋되면 파일을 삭제해도 Git 기록이나 복제본에 남을 수 있다. 민감 정보가 발견되면 단순 삭제로 끝내지 말고 즉시 키를 폐기·재발급하고 기록 정리 여부를 사용자에게 알린다.

## GitHub Actions 사용 원칙

공개 저장소의 표준 GitHub-hosted runner는 정상적인 개발·테스트·패키징 용도로 사용한다. 정책과 한도는 바뀔 수 있으므로 작업 시작 전에 GitHub 공식 문서를 확인한다.

허용되는 일반 용도:

- Java/Gradle/NeoForge 빌드
- datagen, 테스트, GameTest
- 서버 또는 클라이언트 최소 로딩 검사
- JAR 패키징과 SHA-256 생성
- 빌드 로그와 결과 artifact 업로드

금지되는 용도:

- 암호화폐 채굴
- 무한 재실행 루프
- 저장소 개발과 무관한 대규모 계산
- 상시 웹 서버, 프록시, CDN처럼 사용하는 행위
- 코드 변경 없이 짧은 간격으로 무의미하게 반복 실행하는 행위

## 프로젝트 추가 규칙

새 모드는 다음 폴더로 추가한다.

```text
projects/<project-slug>/
```

각 프로젝트 폴더에는 최소한 다음이 있어야 한다.

```text
projects/<project-slug>/
├─ PROJECT.md
├─ build.gradle 또는 build.gradle.kts
├─ gradle.properties
├─ settings.gradle 또는 settings.gradle.kts
├─ gradlew
├─ gradlew.bat
├─ gradle/wrapper/
└─ src/
```

`PROJECT.md`에는 반드시 다음을 기록한다.

- 정식 모드명과 slug
- mod id와 네임스페이스
- 현재 버전
- Java 버전
- Minecraft 버전
- 로더 및 정확한 로더 버전
- Gradle/ModDevGradle 버전
- 최종 JAR 이름
- 실행해야 할 datagen/GameTest/서버 작업
- 기존 월드 호환성 주의사항
- 외부 선택 모드와 포함 금지 의존성

## 결과물 기준

성공한 빌드는 최소한 다음을 제공해야 한다.

```text
<mod>-<version>.jar
<mod>-<version>.jar.sha256
BUILD_AND_RUNTIME_REPORT.md
raw-logs.zip
```

프로젝트가 지원하면 `.mrpack`도 추가한다.

JAR 검증 최소 항목:

- 로더 메타데이터 파일 존재
- 모드 패키지의 컴파일된 `.class` 존재
- `assets/<modid>` 존재
- `data/<modid>` 존재
- `.java`, `tools`, `.github`, 개발 보고서 미포함
- 중복 ZIP entry 없음
- 파일 크기가 0이 아니며 정상 ZIP/JAR로 열림

## 현재 첫 작업

첫 대상은 `Countryside Days`의 Java 25 + Minecraft 26.2 + NeoForge 26.2 실제 빌드다. 소스가 추가되면 `projects/countryside-days/`에서 작업하며, 다른 모드가 추가되더라도 동일한 원칙으로 독립 관리한다.

---

이 저장소를 받은 작업자는 설명을 다시 요청하기 전에 문서와 대상 프로젝트를 먼저 읽고, 가능한 작업을 직접 진행한 뒤 실제 결과와 검증 상태를 보고해야 한다.

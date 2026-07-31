# Build Standard

이 문서는 이 저장소에 들어오는 모든 Minecraft Java 모드의 공통 빌드·검증 기준이다.

## 1. 프로젝트 등록 전 확인

각 프로젝트는 `projects/<slug>/` 아래에 둔다.

추가 전에 다음을 확인한다.

- 소스와 리소스의 재배포 권한
- 비밀 정보 포함 여부
- mod id와 네임스페이스
- 현재 모드 버전
- Minecraft 버전
- Java 버전
- 로더 종류와 정확한 버전
- Gradle과 플러그인 버전
- 기존 월드 호환성 조건
- 선택 의존성과 필수 의존성 구분

## 2. PROJECT.md 필수 형식

각 프로젝트 루트에는 `PROJECT.md`를 둔다.

```md
# <정식 모드명>

- Slug:
- Mod ID:
- Namespace:
- Mod version:
- Minecraft:
- Java:
- Loader:
- Loader version:
- Gradle:
- Build plugin:
- Final JAR:
- Existing-world compatibility:
- Required dependencies:
- Optional external mods:
- Forbidden bundled dependencies:
- Datagen task:
- GameTest task:
- Server smoke-test task:
- Client smoke-test task:
```

버전이 바뀌면 `PROJECT.md`, Gradle 속성, 모드 메타데이터, 생성 카탈로그, 테스트 계약, 문서와 workflow를 함께 갱신한다.

## 3. Workflow 기준

프로젝트별 workflow를 `.github/workflows/build-<slug>.yml`에 둔다.

최소 요구사항:

- `workflow_dispatch` 지원
- 대상 프로젝트 경로 변경 시에만 자동 실행되도록 `paths` 사용
- `permissions: contents: read`
- 명시적인 `working-directory`
- 공식 Java 배포판과 요구 Java 버전 설정
- Gradle 캐시 사용
- 시간 제한 설정
- 성공·실패 여부와 관계없이 로그 업로드
- 성공 시 JAR, SHA-256, 보고서 업로드
- artifact 보존 기간을 필요 이상 길게 잡지 않기

예시 골격:

```yaml
name: Build <project>

on:
  workflow_dispatch:
  push:
    branches: [main]
    paths:
      - 'projects/<slug>/**'
      - '.github/workflows/build-<slug>.yml'

permissions:
  contents: read

defaults:
  run:
    working-directory: projects/<slug>

jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 120
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '<required-version>'
          cache: gradle
      - name: Verify toolchain
        run: |
          java -version
          ./gradlew --version
      - name: Clean build
        run: ./gradlew --no-daemon clean build --stacktrace
```

예시는 그대로 복사하지 말고 프로젝트의 실제 wrapper와 API에 맞춘다.

## 4. 빌드 순서

### 필수

1. wrapper 무결성 확인
2. Java 버전 확인
3. Gradle 버전 확인
4. dependency resolution
5. `clean build`
6. 최종 JAR 생성
7. JAR 내부 검사
8. SHA-256 생성

### 프로젝트가 지원할 때 추가

1. datagen
2. unit test
3. GameTest
4. 전용 서버 최소 부팅
5. 클라이언트 최소 로딩
6. `.mrpack` 생성

한 단계가 실패하면 로그의 마지막 오류만 보지 말고 **가장 먼저 발생한 실질 원인**을 찾아 수정한다.

## 5. 컴파일 오류 수정 기준

허용:

- 공식 최신 API로의 정확한 마이그레이션
- 제네릭 타입, Codec, SavedData, payload, event API 수정
- 등록 순서와 client/server 분리 수정
- datagen과 GameTest 계약 갱신
- 잘못된 리소스 경로나 JSON 수정

금지:

- 기능 전체 삭제
- 오류 코드를 통째로 주석 처리
- 빈 메서드 반환으로 동작 무력화
- 테스트 삭제로 통과 처리
- 가짜 API 클래스 추가
- 로더 검사 우회
- 소스 ZIP을 JAR로 재포장

## 6. 서버·클라이언트 검사

### 전용 서버

최소 목표:

- 모드 탐색과 로딩 성공
- 레지스트리 동결 성공
- 데이터팩 로딩 성공
- 레시피·태그·구조의 치명 오류 없음
- 일정 시간 후 정상 종료

EULA 또는 CI 제약으로 자동 부팅이 불가능하면 이유와 미실행 상태를 보고서에 기록한다.

### 클라이언트

헤드리스 환경에서는 실제 화면 확인이 제한될 수 있다.

가능하면 가상 디스플레이로 다음을 확인한다.

- 모드 초기화 성공
- 클라이언트 전용 클래스 로딩 성공
- 메인 메뉴 또는 초기 창 진입
- 치명 크래시 없음

불가능하면 `NOT RUN`으로 기록하며 성공으로 간주하지 않는다.

## 7. JAR 내부 검사

검사 항목:

- 정상 ZIP/JAR로 열림
- 로더 메타데이터 파일 존재
- 프로젝트의 Java 패키지 아래 `.class` 존재
- `assets/<modid>/` 존재
- `data/<modid>/` 존재
- 버전 정보 일치
- `.java` 미포함
- `.github/`, `tools/`, 개발 로그와 보고서 미포함
- 중복 entry 없음
- 예상치 못한 타 모드 클래스 미포함

NeoForge 프로젝트의 경우 일반적으로 `META-INF/neoforge.mods.toml` 또는 해당 버전이 요구하는 공식 메타데이터를 확인한다. 버전에 따라 형식이 달라질 수 있으므로 실제 공식 문서와 프로젝트 설정을 기준으로 한다.

## 8. 결과 보고서

`BUILD_AND_RUNTIME_REPORT.md`에는 다음을 기록한다.

- 기준 커밋 SHA
- 프로젝트와 버전
- Java/Gradle/Minecraft/로더 버전
- 실행한 명령
- 빌드 결과
- datagen 결과
- GameTest 결과
- 서버 검사 결과
- 클라이언트 검사 결과
- JAR 검사 결과
- SHA-256
- 남은 경고
- 미실행 또는 차단된 단계와 이유

## 9. Artifact 정책

성공 artifact:

```text
<slug>-<version>-deliverables
```

포함 항목:

- 실행용 JAR
- SHA-256
- 보고서
- 필요 시 `.mrpack`

로그 artifact:

```text
<slug>-<version>-logs
```

빌드가 실패해도 로그 artifact는 업로드한다.

JAR, ZIP, 로그를 저장소에 반복 커밋하지 않는다. 릴리스가 필요하다고 사용자가 명시한 경우에만 GitHub Release 사용을 검토한다.

## 10. 완료 판정

다음 조건을 모두 만족해야 `완료`다.

- 실제 Gradle 빌드 성공
- 실행용 JAR 존재
- JAR 안에 컴파일된 클래스 존재
- 필수 메타데이터와 리소스 존재
- SHA-256 생성
- 실행한 검증과 미실행 검증이 정직하게 구분됨
- 사용자에게 다운로드 가능한 결과가 전달됨

정적 검사만 통과했거나 소스만 수정된 상태는 완료가 아니다.

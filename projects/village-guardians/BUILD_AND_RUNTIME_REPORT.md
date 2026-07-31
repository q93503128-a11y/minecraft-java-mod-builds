# Build and Runtime Report

- Project: Village Guardians — 마을지키기
- Mod ID: `villageguardians`
- Version: `0.1.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.37-beta`
- Java target: `25`
- Gradle: `9.2.1`
- ModDevGradle: `2.0.143`

## 구현 상태

### 구현됨

- 촌장 한 명 자동 지정
- 촌장직 이전
- 플레이어별 실무 역할 선택
- 촌장의 시간 진행 안건 발의
- 온라인 플레이어 과반수 찬반 투표
- 접속을 끊은 플레이어의 표를 현재 집계에서 제외
- 자연 일주기 비활성화
- 서버가 1초마다 정해진 시간 단계를 다시 적용
- 투표 통과 시에만 아침, 낮, 저녁, 밤 순환
- 촌장, 역할, 마을 날짜와 시간 단계의 월드 저장 영속화
- 손상된 개별 역할 저장값을 건너뛰는 안전한 복구
- 명령 기반 상태 조회
- NeoForge 모드 메타데이터와 한국어 리소스
- Windows 및 POSIX Gradle 부트스트랩 빌드 스크립트
- JAR 내부 구조 및 SHA-256 검증 스크립트

### 아직 미구현

- 촌장 NPC 또는 촌장 선출 절차
- 역할별 실제 능력과 업무
- 습격, 경비병, 방어 시설, 마을 성장
- 디자인이 완성된 전용 UI
- 진행 중인 투표 안건의 재시작 복구

## 검증 결과

| 단계 | 상태 | 비고 |
|---|---|---|
| 정적 소스 구성 | 완료 | 프로젝트 구조, Java 소스, 메타데이터, 리소스 구성 |
| 26.2 공식 MDK 대조 | 완료 | Java 25, Gradle 9.2.1, ModDevGradle 2.0.143, NeoForge 26.2.0.37-beta 반영 |
| 26.2 저장 API 대조 | 완료 | `SavedDataType`, `Codec`, `computeIfAbsent` 구조를 공식 NeoForge 26.2 소스와 대조 |
| 공식 이벤트 API 대조 | 부분 완료 | 명령 등록과 서버 틱 이벤트 패턴을 NeoForge 공식 소스와 대조 |
| Java 25 실행 확인 | BLOCKED | 현재 실행 환경에는 Java 21만 존재 |
| Gradle `clean build` | NOT RUN | 현재 실행 환경에 Gradle이 없고 외부 다운로드가 차단됨 |
| Datagen | NOT RUN | 빌드 선행 필요 |
| GameTest | NOT RUN | 테스트 미구현 |
| 전용 서버 부팅 | NOT RUN | 실행용 JAR 미생성 |
| 클라이언트 로딩 | NOT RUN | 실행용 JAR 미생성 |
| JAR 내부 검사 | NOT RUN | 실행용 JAR 미생성 |
| SHA-256 | NOT RUN | 실행용 JAR 미생성 |

## 빌드 명령

Windows:

```bat
build.bat
```

Linux/macOS:

```bash
chmod +x build.sh
./build.sh
```

빌드 성공 후:

```bash
python tools/verify_jar.py build/libs/villageguardians-0.1.0-alpha.1.jar
```

## 완료 판정

현재 상태는 **소스 기반 1차 구현**이며 배포 완료가 아니다. 실제 Java 25 환경에서 Gradle 빌드, JAR 검사, 서버와 클라이언트 로딩 검증을 통과한 뒤에만 실행용 JAR 완료로 판정한다.

# Build and Runtime Report

- Project: Village Guardians — 마을지키기
- Mod ID: `villageguardians`
- Version: `0.2.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.37-beta`
- Java target: `25`
- Gradle: `9.2.1`
- ModDevGradle: `2.0.143`

## 구현 상태

### 통치 코어

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

### RPG 알파

- 플레이어별 레벨과 경험치의 월드 저장 영속화
- 기존 0.1 저장 데이터와 호환되는 선택형 RPG 저장 필드
- 적대 몬스터 처치 경험치
- 최고 레벨 30
- 레벨 비례 가하는 피해 증가
- 레벨 비례 받는 피해 감소
- 3레벨 단위 추가 최대 체력
- 5레벨 단위 전투력 급상승
- 접속, 리스폰, 서버 유지 틱에서 성장 체력 패시브 복구
- 역할별 첫 액티브 스킬
- 주변 12블록 같은 차원의 아군 대상 역할 버프
- 레벨 11과 21에서 역할 스킬 단계 상승
- 레벨에 따라 감소하는 스킬 재사용 대기시간
- `/vg rpg status`, `/vg rpg test_xp`, `/vg skill` 명령

### 30레벨 성장 목표

- 가하는 피해: 약 `6.23배`
- 받는 피해: 원래 피해의 `28%`
- 추가 최대 체력: `36포인트` (`18하트`)
- 레벨 1부터 30까지 필요한 누적 경험치: `19,140 XP`

### 아직 미구현

- 촌장 NPC 또는 정식 촌장 선출 절차
- 습격, 경비병, 방어 시설, 마을 성장
- 역할별 고유 작업과 생산 시스템
- 디자인이 완성된 전용 UI
- 진행 중인 투표 안건의 재시작 복구
- RPG 전용 적과 보스, 장비, 다단계 스킬 트리

## 검증 결과

| 단계 | 상태 | 비고 |
|---|---|---|
| 정적 소스 구성 | 완료 | 프로젝트 구조, Java 소스, 메타데이터, 리소스 구성 |
| 26.2 공식 MDK 대조 | 완료 | Java 25, Gradle 9.2.1, ModDevGradle 2.0.143, NeoForge 26.2.0.37-beta 반영 |
| 26.2 저장 API 대조 | 완료 | `SavedDataType`, `Codec`, `computeIfAbsent` 구조를 공식 NeoForge 26.2 소스와 대조 |
| 전투 이벤트 API 대조 | 완료 | `LivingIncomingDamageEvent#setAmount`, `LivingDeathEvent#getSource` 공식 API 대조 |
| RPG 밸런스 계약 테스트 | PASS | 성장 단조성, 레벨 30 수치, 누적 XP, 3000 XP 시뮬레이션 검사 |
| Java 25 실행 확인 | BLOCKED | 기본 실행 환경에는 Java 21만 존재 |
| Gradle `clean build` | NOT RUN | Java 25와 Gradle 실행 환경 확보 필요 |
| Datagen | NOT RUN | 빌드 선행 필요 |
| GameTest | NOT RUN | 테스트 미구현 |
| 전용 서버 부팅 | NOT RUN | 실행용 JAR 미생성 |
| 클라이언트 로딩 | NOT RUN | 실행용 JAR 미생성 |
| JAR 내부 검사 | NOT RUN | 실행용 JAR 미생성 |
| SHA-256 | NOT RUN | 실행용 JAR 미생성 |

## 실행한 로컬 계약 테스트

```bash
python tools/test_rpg_balance.py
```

확인 항목:

- 레벨 상승에 따라 공격 배율이 감소하지 않음
- 레벨 상승에 따라 받는 피해 비율이 증가하지 않음
- 추가 체력이 감소하지 않음
- 레벨 30 공격 배율 `6.23`
- 레벨 30 받는 피해 비율 `0.28`
- 레벨 30 추가 체력 `36`
- 테스트 XP 3000 지급 시 레벨 11, 잔여 200 XP
- 레벨 30까지 누적 필요 경험치 `19,140`

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
python tools/verify_jar.py build/libs/villageguardians-0.2.0-alpha.1.jar
```

## 첫 인게임 테스트

```text
/vg status
/vg role guard_captain
/vg skill
/vg rpg status
/vg rpg test_xp 3000
```

이후 적대 몬스터 처치, 서버 재시작, 멀티 아군 스킬 적용, 투표 과반수 처리를 확인한다.

## 완료 판정

현재 상태는 **RPG 기능이 포함된 소스 기반 플레이 테스트 후보**다. 밸런스 계산 계약은 통과했지만, 실제 Java 25 Gradle 빌드와 서버·클라이언트 로딩 검증 전에는 실행용 JAR 완료로 판정하지 않는다.

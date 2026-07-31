# Build and Runtime Report

- Project: Village Guardians — 마을지키기
- Mod ID: `villageguardians`
- Current source version: `0.7.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.37-beta`
- Java target: `25`
- Gradle: `9.2.1`
- ModDevGradle: `2.0.143`
- Target JAR: `villageguardians-0.7.0-alpha.1.jar`

## 0.7 변경 범위

### 외부 공개 자산

- Default Dark Mode 고정 커밋에서 26.2 인벤토리와 버튼 GUI 3개를 빌드 시 포함
- Towns and Towers 고정 Modrinth 버전에서 마을 구조 NBT만 탐색
- 좀비 마을, 전초기지, 선박, 폐허와 월드젠 설정 제외
- 같은 건축 스타일에서 회관, 병영, 대장간, 연구소, 상점, 의무소 6개를 선별
- 최종 선택 경로와 제3자 라이선스 고지를 JAR 내부에 기록
- 외부 구조가 없거나 지정 부지보다 크면 바닐라 구조, 이후 자체 구조로 안전하게 대체

### 건물 배치

- 구조 NBT 안의 모든 문 블록을 네 회전 방향으로 검사
- 문이 중앙 광장 쪽 외벽에 오는 회전을 자동 선택
- 실제 월드에 배치된 문 위치를 다시 탐색
- 실제 문 바로 밖에 진입로, 기능 단말과 벽면 간판 배치
- 임의 중앙 좌표에 책장, 양조기, 가마솥 등을 덧씌우던 코드 제거
- 강화 표시 블록을 입구 옆 지면 받침대로 이동

### 북문과 성벽

- 기존 석재 절단기 임시 조작 블록 제거
- 17×8 크기의 틈 없는 목제 성문
- 통나무 외곽틀, 중앙 보강대와 상하 보강대
- 내부 바닥 부착 레버로 개폐
- 성문 파괴 시 개폐 거부
- 레버 상태와 실제 성문 상태 동기화
- 북문 양쪽 계단을 성벽 상단 발판까지 직접 연결

### UI

- 인벤토리 GUI를 공개 어두운 자산으로 교체
- 인벤토리 보조 패널을 어두운 RPG 대시보드로 재설계
- 레벨 배지, 실제 XP 진행바, 역할·자산·마을 정보 행
- 전체 운영 UI를 전술형 어두운 패널로 재설계
- 무지개 버튼과 두꺼운 금색 외곽선 제거
- 행동 종류에 따른 제한된 청록·금색·위험색 사용
- 작은 화면에서 1열 버튼과 본문 스크롤 유지

## 검증 결과

| 단계 | 상태 | 비고 |
|---|---|---|
| 0.6 Java 25 `clean build` | PASS | 이전 기준 소스 컴파일 성공 |
| 0.6 계약 테스트 | PASS | RPG, 영역, 시설 성장 |
| 0.6 JAR 내부 검사 | PASS | 실행 JAR, 클래스, 리소스, SHA-256 |
| 0.7 요새 배치 계약 | PASS | 대로 무건물, 중앙 방향, 성문 무틈, 계단 연결 |
| 0.7 Direction API 대조 | PASS | 26.2 `getClockWise`, 방향 벡터 확인 |
| 0.7 간판 API 대조 | PASS | 26.2 `WallSignBlock.FACING` 확인 |
| 0.7 계단·레버 API 대조 | PASS | `StairBlock.FACING`, 바닥 부착 레버 속성 확인 |
| 0.7 구조 경계 API 대조 | PASS | 회전 경계와 `StructureTemplate#getBoundingBox` 확인 |
| 0.7 문 태그·구조 블록 조회 | PASS | `BlockTags.DOORS`, 구조 블록 정보 접근 확인 |
| 0.7 Python 전체 계약 실행 | PARTIAL PASS | 신규 요새 계약은 현재 환경에서 직접 실행해 통과 |
| 0.7 Java 25 컴파일 | NOT RUN | 현재 실행 환경은 Java 21이며 외부 도구 다운로드 차단 |
| 0.7 외부 자산 다운로드 | NOT RUN | 빌드 단계에서만 고정 URL·Maven 좌표로 실행 예정 |
| 0.7 JAR 내부 외부 자산 검사 | NOT RUN | 0.7 JAR 미생성 |
| 0.7 전용 서버 부팅 | NOT RUN | 0.7 JAR 미생성 |
| 0.7 클라이언트 로딩 | NOT RUN | 0.7 JAR 미생성 |
| 0.7 SHA-256 | NOT RUN | 0.7 JAR 미생성 |

## 신규 요새 계약 테스트

`tools/test_fortress_layout.py`가 다음을 확인한다.

- 모든 기능 건물과 중앙–북문 전투로의 비충돌
- 서쪽 건물은 동쪽, 동쪽 건물은 서쪽, 남쪽 회관은 북쪽을 향함
- 닫힌 성문 136칸이 모두 채워짐
- 계단 마지막 발판과 성벽 상단 발판이 같은 Z 좌표에서 연결됨

실행 결과:

```text
[PASS] Buildings do not block the north-gate avenue
[PASS] Every functional building faces the central plaza
[PASS] Closed gate has no missing columns or slit
[PASS] Wall stairs connect directly to the wall-top platform
```

## 최종 JAR 검증 강화

`tools/verify_jar.py`는 기존 검사에 더해 아래 파일을 필수로 요구한다.

```text
assets/minecraft/textures/gui/container/inventory.png
assets/minecraft/textures/gui/sprites/widget/button.png
assets/minecraft/textures/gui/sprites/widget/button_highlighted.png
data/villageguardians/structure/external/town_hall.nbt
data/villageguardians/structure/external/barracks.nbt
data/villageguardians/structure/external/smithy.nbt
data/villageguardians/structure/external/skill_hall.nbt
data/villageguardians/structure/external/storehouse.nbt
data/villageguardians/structure/external/infirmary.nbt
META-INF/villageguardians/THIRD_PARTY_NOTICES.txt
META-INF/villageguardians/towns-and-towers-selection.txt
```

## 자동 빌드 정책

GitHub Actions는 소스 커밋마다 실행되지 않는다. 워크플로는 다음 경우에만 실행된다.

- 수동 `workflow_dispatch`
- `projects/village-guardians/.build-trigger` 파일 변경

이번 0.7 수정에서는 `.build-trigger`를 변경하지 않았으므로 Actions 실행과 사용량 소비가 발생하지 않았다.

## 완료 판정

0.7 소스 수정과 수치 계약은 완료됐지만 Java 25 컴파일, 외부 자산 포함 JAR 검사와 실제 클라이언트 로딩은 아직 실행하지 않았다. 따라서 현재 상태를 **0.7 소스 구현 완료 / 실행 JAR 미검증**으로 판정한다. 이전 0.6 JAR을 0.7 결과물로 재사용하거나 최신 빌드라고 표시하지 않는다.

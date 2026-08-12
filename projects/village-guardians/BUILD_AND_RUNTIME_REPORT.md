# Build and Runtime Report

- Project: Village Guardians — 마을지키기
- Mod ID: `villageguardians`
- Current source version: `0.18.9-alpha.1`
- Minecraft: `26.2`
- NeoForge build dependency: `26.2.0.37-beta`
- Java target: `25`
- Gradle: `9.2.1`
- ModDevGradle: `2.0.143`
- Target JAR: `villageguardians-0.18.9-alpha.1.jar`
- Final acceptance Actions run: `31561823343`
- Final acceptance head: `d3a45e1358e181aa51656c28128148edfaf441cf`
- Final JAR SHA-256: `46cae2f08d801bf5599052fcc5335dcaea8e31b0312c223a070efb701b6cc385`
- Final JAR size: `898210` bytes

## 기준점과 회귀 기준

이번 작업은 사용자 실제 테스트 기준 JAR `villageguardians-0.18.8-alpha.1`과 당시 GitHub `main`을 다시 확인한 뒤 진행했다.

- 0.18.8 기준 JAR SHA-256: `688a63f2e46f2d52738955eadf566e178bf160fde2435612915605fa2f7cdd78`
- 0.18.8 마지막 확인 성공 Actions run: `31551899143`
- 0.18.8의 초반 솔로 난이도 완화 유지
- 플레이어 추가 1명마다 적 수 약 `+30%` 유지
- 전원 부활 대기 중 시설 피해 30% 보호는 제거된 상태 유지
- 기존 상점 카테고리, 잡템 판매 보호, 중요 행동 확인창, 다음 웨이브 도감, UI Safe Area, 구조물 파괴 드롭 억제 계약 유지

## 0.18.9 방어전 시스템 2차 확장

### 성벽 Segment / 국소 돌파

성벽 전체를 하나의 HP로 처리하지 않고 다음 7개 방어 구역으로 분리했다.

1. 북서 성벽
2. 북문
3. 북동 성벽
4. 서쪽 방벽
5. 동쪽 방벽
6. 후방 서측 방벽
7. 후방 동측 방벽

북문은 기존 `Building.WALLS` 저장 구조를 어댑터로 사용해 이전 세이브와 호환한다. 나머지 구역은 별도 현재 HP, 최대 HP, 방어 등급, 강화 단계, 손상 상태를 저장한다.

HP 비율에 따라 정상 → 균열 → 대파 → 돌파 상태로 월드 외형이 변하며 0이 되면 마지막 공격 축을 중심으로 폭 5블록의 국소 돌파구만 생성한다. `destroyBlock`이나 블록 드롭을 사용하지 않고 직접 블록 상태를 갱신한다. 수리 시 동일 위치를 재구축한다.

### 공격 방향과 전장 상황

날짜에 따라 전선 구조가 단계적으로 확장된다.

- 1~4일: 북문 정면 전용
- 5~7일: 북서/북동 소규모 별동대
- 8~11일: 서/동 측면 공세
- 12~15일: 정면 + 양측 복합 전선
- 16일 이후: 정찰에 공개된 후방 서측/후방 동측 별동대까지 포함 가능

측·후방 스폰은 자연 지형에 묻히지 않도록 목표 Y 기준 ±24블록 범위에서 `단단한 바닥 + 발/머리 2블록 공기` 위치를 찾는다. 돌파 전에는 대응 Segment를 공격하고, 돌파 후에는 해당 구멍 안쪽 접근점을 새 경로 목표로 사용한다.

전장 상황은 맑은 전장, 공성 북소리, 검은 안개, 불탄 진입로를 deterministic하게 선택하고 실제 강화/기동/화염 저항/구조물 압박 효과와 연결했다.

다음 밤 정찰에는 주공, 별동대, 예상 총병력, 병과, 공성 병과, 정예, 보스 전투 구조, 전장 상황을 표시하며 야간 시작 직전 위험 방향에 연기/화염 신호를 제공한다.

### 신규 정예 교리 5종

- 갈고리병: 측면 성벽 접근 후 내부 침투
- 화염 투척병: 근거리 범위 화상·마법 피해
- 침투 암살자: 플레이어 직접 추격
- 역병술사: 독·약화 범위 방해
- 돌파 기병: 고속 돌격·순간 강화

기존 적 병과를 색상/스탯만 바꾼 것이 아니라 각 교리가 실제 이동·목표·공격 방식을 변경한다.

### 신규 보스 전투 구조 3종

- 파성 거신: Segment를 직접 파쇄하고 HP 50% 이하에서 파쇄 주기 강화
- 사령 결속자: 주변 침공 병력에 반복 회복·보호막·저항 의식
- 검은 결투원수: 시설보다 살아 있는 플레이어를 우선 추격하는 결투형 지휘관

기존 보스 종류/변이 시스템 위에 별도 전투 구조로 적용된다.

### 직접 배치 포탑 10계열

기존 고정 성루는 요새 건축/관측 구조물로 남기고 실전 화력은 플레이어가 직접 배치하는 포탑이 담당한다.

- 중쇠뇌
- 연사 포탑
- 철갑 관통포
- 화염 투사기
- 서리 억제기
- 연쇄 전격탑
- 광역 투석포
- 마법 억제탑
- 대공 발사대
- 지원 봉화

설치 과정은 계열 선택 → 바닥 우클릭 유효성 미리보기 → 같은 위치 재클릭 확정이다.

서버 설치 검증은 방어구역, 주 통행로, 북문 전면 도배, 건물 출입구/운영 공간, 포탑간 최소 8블록, 단단한 바닥, 2블록 높이 공간, 전체 설치 한도를 검사한다.

각 포탑은 영구 ID, 위치, 레벨, 현재/최대 HP, 공격력, 공격 주기, 사거리, 계열 역할, 가동 상태를 가진다. 포탑 사냥꾼/폭파병/보스가 포탑을 공격하며 HP 0은 아이템 드롭 없는 잔해 상태가 된다. 회관에서 개별 수리/강화/철거와 손상 포탑 일괄 수리를 제공한다.

기존 `VillageTowerResearchBonusSystem` 생명주기 훅은 안전 계약 때문에 유지하되 고정 좌표 성루 보너스 사격 대신 직접 배치 포탑 연구 보너스로 재구현했다. `VillageDefenseSystem.tick`의 고정 성루 실전 사격은 production 전투 루프에서 제거했다.

### 용병 배치 3거점

전투 전 병과별로 성문 전방, 성 내부, 성벽 중 허용된 거점을 선택한다. 전투 중에는 RTS식 세밀 조작 없이 병과별 AI가 자동 전투하고, 역할별 leash 범위를 지나치게 벗어나면 지정 전선으로 복귀한다.

### 장비/무기/세트

기존 장비 등급·강화의 범용 성능을 주력으로 유지한 채 다음 무기 계열 차이를 실제 피해 경로에 연결했다.

- 장검
- 대형 도끼
- 장창/투척창
- 전투 망치
- 장궁
- 석궁

2/3세트는 다음 두 계열을 추가했다.

- 성벽 수호자: 범용 피해 증가 + 받는 피해 감소
- 밤사냥꾼: 원거리 운용 강화 + 범용 생존 보정

세트는 장비 상태에서 매번 계산하므로 해제/재장착 시 중복 누적되지 않는다. 동일 계산 결과를 장비 툴팁과 인벤토리 세트 상태 UI가 표시한다.

### UI 회귀 방지

- 포탑 신규 배치 10종 화면과 설치 포탑 관리 화면을 분리했다.
- 인벤토리 보조 패널은 실제 좌/우 안전공간을 기준으로 폭을 정한다.
- 좁은 GUI에서는 96px compact 패널로 줄이고 버튼을 제거한다.
- 읽을 수 있는 측면 공간이 없으면 바닐라 인벤토리와 겹치지 않도록 패널을 숨기며 장비 툴팁 정보는 유지한다.
- 테스트 모델에서 GUI 폭 `256`, `320`, `360`, `426`, `854`, `1280`, `1920`을 검사했다.
- `open_tower_control`, `tower_status`, `tower_open:*`, `tower_branch:*`, `tower_upgrade:*` 등 구식 고정 성루 action은 production에서 새 공성 방어 지휘 화면으로 강제 리다이렉트한다.

## 재도전 / 새 게임 상태 복원

0.18.9의 Segment/포탑 SavedData가 패배한 밤 이후 누적되는 문제를 방지하기 위해 낮→밤 전환 시 별도 공성 스냅샷을 저장한다.

- `segment_hp_*`
- `segment_breach_*`
- `turret_*`

같은 날 재도전은 야간 시작 스냅샷을 복원한다. 처음부터 재시작은 0.18.9 공성 SavedData를 초기화한다. 이후 `VillageWorldSystem.forceRebuild`가 기본 요새를 먼저 재건한 뒤 복원된 Segment 손상 외형과 포탑 상태를 다시 월드에 투영한다.

## 검증 결과

| 단계 | 상태 | 비고 |
|---|---|---|
| 0.18.8 기준 JAR 검사 | PASS | 버전/Manifest/NeoForge/MC 범위/SHA-256 확인 |
| 기존 RPG/영역/성장/요새/런타임 안전 계약 | PASS | 최종 run에서 재실행 |
| 0.18.8 위험/상점/UI 회귀 계약 | PASS | 초반 난이도, +30% 멀티, 사망 리스크, 생산 라우팅 유지 |
| 7개 Segment 계약 | PASS | 독립 HP, 5블록 국소 돌파, no-drop 복구 |
| 경로/다전선 변수 검사 | PASS | 날짜 단계, 측·후방 전선, safe terrain-height spawn, breach route |
| 1/2/3/4인 스케일 | PASS | 기준 10명 모델에서 10/13/16/19 |
| 직접 배치 포탑 | PASS | 10계열, 위치검증, HP/파괴/수리/강화/철거 |
| 재도전/새게임 스냅샷 | PASS | 저장/복원/초기화 + forceRebuild 재투영 계약 |
| 용병 배치 | PASS | 3거점 + 병과 제한 + 자동 전투 leash |
| 정예/보스 전투 구조 | PASS | 정예 5교리 + 보스 3구조 |
| 장비 실성능/설명 일치 | PASS | 동일 runtime multiplier와 tooltip/set UI 연결 |
| 세트 해제/재장착 | PASS | 매번 장착 상태 계산, 중복 누적 없음 |
| 인벤토리 Safe Area | PASS | 256~1920 모델, compact/hide 정책 검증 |
| 구조물/성벽 파괴 드롭 회귀 | PASS | no-drop 블록 투영 + 기존 debris guard 유지 |
| Java 25 NeoForge clean build | PASS | Actions run `31561823343` |
| JAR verifier | PASS | `tools/verify_jar.py` |
| Actions artifact upload | PASS | artifact `villageguardians-0.18.9-alpha.1` |
| 다운로드 후 ZIP/JAR 재검사 | PASS | `unzip -t`, TOML/Manifest 버전, artifact SHA 대조 |
| Windows + Modrinth 실제 플레이 | NOT RUN HERE | 이 실행 환경에는 Windows Modrinth 클라이언트가 없어 사용자 인스턴스에서 인게임 연출/AI 최종 체감 확인 필요 |

## 빌드 중 발견 및 수정한 문제

1. 기존 런타임 안전 계약이 `VillageTowerResearchBonusSystem.tick` 생명주기 훅을 요구함 → 훅은 유지하고 직접 배치 포탑 연구 보너스로 재구현.
2. Minecraft 26.2의 `Blocks.LIGHTNING_ROD`가 단일 `Block`이 아닌 구리 weathering collection으로 변경되어 컴파일 오류 발생 → 연쇄 전격탑 외형을 26.2 안전 단일 블록 `Blocks.END_ROD`로 변경.
3. 좁은 GUI에서 신규 세트 패널이 바닐라 인벤토리와 겹칠 가능성 발견 → 실측 side-space 기반 compact/hide Safe Area 로직으로 교체.
4. 측/후방 스폰 원점이 요새 평탄화 반경 밖이라 자연 지형에 묻힐 가능성 발견 → ±24Y safe spawn 탐색 추가.
5. 실패 재도전에서 신규 Segment/포탑 파손 상태가 누적될 가능성 발견 → 야간 시작 공성 스냅샷 및 forceRebuild 재투영 추가.
6. 오래된 고정 성루 UI action이 남아 있을 수 있음 → 새 공성 지휘 UI로 production redirect 추가.

## 최종 산출물

GitHub Actions run `31561823343`은 Java 25 / NeoForge 26.2 환경에서 deterministic contract tests → clean build → JAR verifier → artifact upload까지 모두 성공했다.

다운로드한 최종 JAR 내부는 다음을 다시 확인했다.

```text
META-INF/neoforge.mods.toml: version="0.18.9-alpha.1"
META-INF/MANIFEST.MF: Specification-Version: 0.18.9-alpha.1
META-INF/MANIFEST.MF: Implementation-Version: 0.18.9-alpha.1
JAR compressed-data test: no errors
size: 898210 bytes
SHA-256: 46cae2f08d801bf5599052fcc5335dcaea8e31b0312c223a070efb701b6cc385
```

## 남은 실제 플레이 테스트 포인트

코드/CI/JAR 수준 검증은 완료됐다. Windows + Modrinth App에서 다음을 우선 확인한다.

- 1~4일 솔로 정면전 체감 난이도
- 5~7일 측면 별동대가 정찰 정보/월드 신호와 같은 방향에서 등장하는지
- 16일 이후 후방 별동대가 사전 예고 없이 생성되지 않는지
- Segment HP 감소 → 균열 → 5블록 국소 돌파 → 적 진입 → 수리 복원 흐름
- 포탑 미리보기 유효/무효 위치, 파괴 잔해, 회관 개별/일괄 수리
- 패배 후 같은 날 재도전에서 야간 시작 전 Segment/포탑 상태가 정확히 복원되는지
- 처음부터 재시작에서 공성 SavedData가 초기화되는지
- GUI 배율 변경 시 인벤토리 세트 패널과 회관/포탑 화면의 실제 글꼴 렌더링
- 정예 5종과 보스 3구조의 체감 가독성/난이도

이 단계에서 발견되는 문제는 다음 alpha에서 실제 플레이 피드백 기준으로 보정한다.

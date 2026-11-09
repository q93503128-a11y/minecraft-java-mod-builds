# Drehmal 2.2.2f — 업적 보상 콜백·예약 함수 선별 조사

**결론: 원작 코드 재사용 0건, 원본/복사본 월드 수정 0건.** 정적 호출 근거로 분류한 것이며 안전한 제거 승인은 아님.

검증 출처: [원본 데이터팩 그래프](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42568764777) · [명령·업적·예약함수 감사](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42591982023).

## 전체 현황

| 검사 항목 | 개수 |
|---|---:|
| 원작 업적 정의 | 501 |
| 보상 필드가 있는 업적 | 292 |
| 무기 업적 39개 외의 함수 보상 기록 | 181 |
| 해당 기록에서 서로 다른 보상 함수 ID | 171 |
| 정적 함수 직접 호출자가 있는 업적 콜백 기록 | 7 |
| 원작 자동 root 그래프에서 도달하는 콜백 기록 | 1 |
| 월드 변경 명령 가능성이 있는 콜백 정적 그래프 | 42 |
| 예약 실행을 포함하는 콜백 정적 그래프 | 45 |
| 원작 예약 함수를 포함하는 함수 수 | 672 |
| 예약 함수 명시 호출 건수 | 854 |
| 서로 다른 예약 대상 함수 ID | 753 |
| 원작 자동 root 밖에서 존재하는 예약 실행 함수 | 273 |
| 자기 자신을 예약하는 함수 수 | 27 |
| 예약 함수 그래프 순환 컴포넌트 | 29 |

## 업적 콜백 분류 (제거 허가 아님)

| 검토 분류 | 기록 수 |
|---|---:|
| `ENTITY_COMBAT_REVIEW` | 5 |
| `MISSING_FUNCTION_REVIEW` | 2 |
| `PLAYER_REWARD_OR_STATE_REVIEW` | 11 |
| `STORY_OR_ADVANCEMENT_REVIEW` | 21 |
| `STRUCTURE_OR_PUZZLE_PRESERVE_REVIEW` | 65 |
| `WORLD_STATE_OR_TRAVEL_REVIEW` | 77 |

업적 소속 네임스페이스:

| Namespace | 콜백 기록 |
|---|---:|
| `advancements` | 22 |
| `core` | 50 |
| `dialogue` | 2 |
| `drop` | 1 |
| `entities` | 14 |
| `foundry` | 5 |
| `minecraft` | 4 |
| `players` | 82 |
| `terminus` | 1 |

정적 그래프에 존재하지 않는 원작 콜백 ID: `players:items/frenzy/kill`, `players:null`

## 장치 기능 보존이 필요한 사례

- `data/foundry/advancements/2.json` → `foundry:levers/2_check`: `STRUCTURE_OR_PUZZLE_PRESERVE_REVIEW`, 정적 함수 `3`개 연결. 게임 내 실제 실행/시설 의존성은 미검증.
- `data/players/advancements/terms/avsal.json` → `core:scene/terminals/avsal_check`: `STRUCTURE_OR_PUZZLE_PRESERVE_REVIEW`, 정적 함수 `6`개 연결. 게임 내 실제 실행/시설 의존성은 미검증.
- `data/core/advancements/godmachine/godbutton.json` → `core:scene/godmachine/enable`: `STRUCTURE_OR_PUZZLE_PRESERVE_REVIEW`, 정적 함수 `1`개 연결. 게임 내 실제 실행/시설 의존성은 미검증.
- `data/players/advancements/teth_spawn.json` → `players:null`: `MISSING_FUNCTION_REVIEW`, 정적 함수 `0`개 연결. 게임 내 실제 실행/시설 의존성은 미검증.
- `data/players/advancements/frenzy_kill_alt.json` → `players:items/frenzy/kill`: `MISSING_FUNCTION_REVIEW`, 정적 함수 `0`개 연결. 게임 내 실제 실행/시설 의존성은 미검증.

## 향후 분리 결정의 선행 조건

1. 업적 콜백 181건 중 맵의 문·레버·장면·포탈에 연결된 이벤트를 보존 대상으로 먼저 표시한다.
2. 원작 게임 내 무기·성장·보상만 제어하는 것으로 검증된 콜백에 한해 별도 사본에서 가역적 분리 실험을 한다.
3. 예약 함수 672곳은 원작 자동 root와 무관하게 재실행될 수 있으므로 world saved schedule 데이터 및 호출 조건을 추가 분석한다.
4. 어떤 함수도 호출자가 없다는 이유만으로 삭제하지 않는다. 업적 엔진이 직접 호출하기 때문이다.
5. 원작 구조물 코드가 유용해 보여도 TURNBOUND에 이식하려면 사용자 별도 승인 필요.

## 증거 한계

- Static callback ids and schedule edges do not prove live runtime activity.
- Advancement criteria may be referenced by other game logic even when reward functions have no direct callers.
- Saved scheduled functions were not read from level.dat or command storage.
- Original command-block geometry and mechanisms must not be removed from classification alone.
- No Drehmal code was copied, adapted or reused in TURNBOUND.
## 2026-11-09 실제 검증 상태

- [Terminus · 업적 보상 · 예약 함수 분류 검증 #1](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42605435141): **SUCCESS**. 자체 회귀 테스트 6개 + 공식 기존 감사 자료 SHA-256 검사 + 업적 콜백 181건, 예약 함수 672건, 예약 순환 컴포넌트 29개 재계산 통과. 새 원본 월드 다운로드/수정 없음.
- 원작 예약 함수 정적 순환 컴포넌트는 크기 **9인 장면 퍼즐 회로** `core:scene/bpalace/puzzle*` 1개, 크기 **2인** `core:c_commands/error/loop*` 1개, 크기 **1인** 자기 예약 27개로 구분됨. 스케줄러에 등록된 실제 진행 중 작업이 아니라 **원작 함수 내용의 정적 예약 관계**다. 퍼즐은 삭제 승인 없음.
- 공식 그래프에 정의되지 않은 원작 업적 보상 함수: `players:null`, `players:items/frenzy/kill`. 오타, 제거된 이전 기능 또는 동적 의존성 여부를 단정하지 않고 재확인 목록에 남김.
- 원작 빠른 이동/역참, 보상, 퍼즐, 엔티티 AI 구현을 TURNBOUND에 이식하지 않음. 새로운 원작 기능 재사용 승인은 없음.

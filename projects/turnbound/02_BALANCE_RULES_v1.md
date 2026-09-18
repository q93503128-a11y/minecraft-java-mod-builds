# TURNBOUND Balance / Rules v1

> 역할: 대격변 이후 전투 수치, 행동 경제, 성장, 재화, 소환의 수치 설계 정본.
> 아직 실제 simulator/playtest가 끝나지 않은 값은 “초기 목표 범위”로 표시하며, 옛 v0.4 수치를 자동 계승하지 않는다.

## 1. 전투 전역 규칙

| 항목 | v1 |
|---|---:|
| 아군 최대 | 4 |
| 일반 적 권장 | 1~4 |
| 일반 적 상한 | 5 |
| Turn Gauge 기준 | 1000 |
| 전투 속도 | 1.0x / 2.0x presentation only |
| 플레이어 선택 중 논리시간 | 정지 |
| 공통 MP/AP | 없음 |
| Basic CD | 0 |
| 속성 상성표 | 사용하지 않음 |
| 전투 중 파티 교체 | 기본 불가 |
| 정식 플레이어블 희귀도 | ★3~★5 |
| 레벨 상한 | 60 |

★1~2 “재료 캐릭터”는 v1 수집 체계에서 폐기한다.

## 2. SPD / Turn Gauge — 가장 먼저 고칠 기반 규칙

### 2.1 의미

SPD는 단순 선공 스탯이 아니라 **시간당 정규 행동 횟수**를 결정한다.

Gauge는 행동까지 남은 거리/진행을 표현한다.

- Gauge >= 1000: 행동 가능
- 행동 완료: Gauge -= 1000
- 초과 Gauge: 보존
- 자연 SPD 차이로 생기는 연속 행동: 허용
- 플레이어가 생각하는 동안 Gauge: 진행하지 않음

### 2.2 현재 integer pulse 방식 폐기

현재 구현처럼:

`ceil((1000 - gauge) / SPD)`

을 정수 pulse로 만든 뒤 `pulse × SPD`를 더하는 방식은 SPD 차이를 같은 pulse 구간으로 양자화한다.

v1에서는 사용하지 않는다.

### 2.3 deterministic fixed-point scheduler

구현 목표:

- `GAUGE_SCALE = 1,000,000`
- 내부 Gauge: `gaugeMicro = displayedGauge × GAUGE_SCALE`
- threshold: `1,000 × GAUGE_SCALE`
- 논리시간 최소 단위: 1 micro-time
- 1 micro-time 진행 시 각 전투원의 `gaugeMicro += FinalSPD`

Ready가 없을 때 전투원 i:

`timeToReadyMicro_i = ceil((thresholdMicro - gaugeMicro_i) / FinalSPD_i)`

`delta = min(timeToReadyMicro_i)`

모든 살아있는 전투원:

`gaugeMicro += delta × FinalSPD`

이 방식은 부동소수점에 의존하지 않고 현재 whole-pulse 방식보다 1,000,000배 미세한 시간 해상도를 가진다.

Skill Gauge +180은 내부적으로 `180 × GAUGE_SCALE`을 더한다.

행동 후 `1000 × GAUGE_SCALE`만 뺀다.

### 2.4 하나의 scheduler

다음은 반드시 동일한 `TurnScheduler` 계산 경로를 사용한다.

- 실제 next actor
- HUD Turn Order preview
- SPD buff/debuff 반영
- Gauge push/pull
- revive Gauge
- extra turn / action advance
- AUTO 판단에서 사용하는 미래 순서

UI용 별도 근사 공식을 만들지 않는다.

## 3. SPD 밸런스

외부 턴제 RPG에서도 SPD/Action Value는 행동 횟수와 직접 연결되므로 매우 높은 가치의 스탯이다.

TURNBOUND v1 초기 설계 범위:

- 대부분의 영웅: 90~110
- 명확한 느린 캐릭터: 80대 가능
- 명확한 빠른 캐릭터: 110대 가능
- 75 이하 / 120 이상은 강한 역할 비용 또는 전용 기믹 없이 사용하지 않는다

SPD +10%를 ATK +10%와 같은 가격의 단순 옵션으로 취급하지 않는다.

장비에서 SPD가 등장한다면:
- flat 소량
- 슬롯/티어별 상한
- 무작위 고SPD 부옵으로 행동 횟수를 폭발시키지 않음

## 4. Gauge 조작의 power budget

Gauge 조작은 곧 행동 경제 조작이다.

따라서:
- 단일 대상 소폭 당김/밀림: 일반 utility
- 아군 전체 당김: 고가치
- 적 전체 지연: 고가치
- Gauge를 즉시 1000 이상으로 만드는 효과: premium effect
- 즉시행동 + 쿨감 + 큰 피해를 한 스킬에 동시에 넣지 않는다

초기 튜닝 범위:
- 소형 단일 push: +100~180
- 소형 단일 delay: -80~-150
- 광역 delay: 개별 수치가 단일보다 낮아야 함
- “즉시 Ready”: CD/조건/자기 행동 기회비용 중 최소 하나가 커야 함

보스는 단순 면역 대신 감소 효율 cap이나 phase-specific resistance를 쓴다.

## 5. 캐릭터 power budget

캐릭터 강함은 다음 다섯 축으로 본다.

1. 직접 피해
2. 생존 기여
3. 행동 경제
4. 제어/디버프
5. 파티 시너지/유틸리티

한 캐릭터가 다섯 축 모두 상위권이면 안 된다.

높은 희귀도는:
- 더 많은 스탯
이 아니라
- 더 넓은 선택지
- 더 안정적인 signature loop
- 높은 ceiling
을 줄 수 있다.

★3도 좁은 상황에서는 ★5보다 좋은 선택이 될 수 있어야 한다.

## 6. Skill potency 초기 범위

실제 simulator로 조정하기 전의 출발 범위다.

### Basic
- 단일 damage: 약 0.85x~1.05x ATK
- utility Basic은 피해를 포기하는 만큼 실제 action economy/회복/방어 가치가 있어야 함

### CD2 공격 Active
- 약 1.55x~2.10x
- 조건부 setup/consume가 있으면 상단을 넘을 수 있음

### CD3~4 고가치 Active
- 강한 AoE / revive / premium Gauge / 팀 보호 같은 전투 흐름 변화 효과
- 단순 “Basic보다 수치만 큼”으로 쓰지 않음

### Reaction / Follow-up
- 정규 행동을 소비하지 않는 만큼 potency를 낮게 시작
- trigger frequency를 포함한 기대 피해로 평가
- 같은 Reaction이 다른 Reaction을 무한 재귀시키지 않음

## 7. 전투 길이 목표

난이도를 HP 스펀지로 만들지 않는다.

초기 목표:
- 약한 필드전: 파티 정규 행동 3~6회 안에서 승부 윤곽
- 표준 필드전: 파티 정규 행동 6~12회
- Elite: 10~18회
- Boss phase 포함: 18~30회 수준에서 mechanic을 여러 번 경험

실제 시간보다 “의미 있는 선택 횟수”를 먼저 본다. 2x 사용 시 animation만 빨라지고 행동 수는 동일해야 한다.

## 8. 성장 구조 단순화

v0.4의 태생 별과 현재 별을 동시에 올리는 이중 성급 구조는 폐기한다.

v1:
- **Native Rarity ★3~★5**: 수집 희귀도, 변하지 않음
- **Level 1~60**: 기본 수치 성장
- **Equipment 3 slots**: 빌드/파밍
- **Signature 1 slot**: 캐릭터별 선택적 특화, 캐릭터 본체의 필수 기능은 아님
- **Awakening 1회**: 후반 mechanical expansion

캐릭터 승급용 별 개수 반복은 없다.

모든 희귀도는 Lv60까지 성장 가능하다.

## 9. 레벨 성장

기존 `1 + 0.045 × (L-1)`을 자동 계승하지 않는다.

새 목표:
- Lv1→60 총 스탯 성장폭이 장비/캐릭터 기믹을 압도하지 않을 것
- 레벨이 부족하면 어렵지만 캐릭터 조합이 무의미해질 정도의 격차는 만들지 않을 것
- SPD는 레벨로 자동 성장하지 않음

새 growth curve는 전투 simulator로:
- 표준 필드 TTK
- boss action count
- heal/guard 가치
- 장비 강화 가치
를 동시에 맞춘 뒤 고정한다.

## 10. 장비

장비 가챠는 없다.

기본 슬롯:
- Weapon
- Armor
- Accessory

원칙:
- 옵션은 예측 가능
- 강화는 Gold
- 실패/파괴 없음
- 무작위 부옵 리롤 노동 없음
- 장비가 캐릭터 signature mechanic을 대체하지 않음

장비는 “ATK 높은 것 하나”가 아니라 공격/생존/tempo 중 선택을 만들 수 있어야 한다.

## 11. 재화 — core 3개 원칙

재화 수를 먼저 늘리지 않는다.

### Gold
용도:
- 장비 강화
- 상점 구매
- 일부 고정 성장 비용

주요 획득:
- 전투
- 퀘스트
- 탐험 보상
- 던전

### Summon Crystal
용도:
- 캐릭터 소환만

주요 획득:
- 메인/사이드 퀘스트
- 첫 클리어
- 발견/탐험 milestone
- 보스/도전

### Star Essence
용도:
- 중복 캐릭터 변환
- 장기 수집 보상/선택권 교환
- 제한된 late-game 성장 보조

“지역별 토큰”, “강화석”, “승급석”, “스킬책”을 이유 없이 추가하지 않는다. 필요하면 어떤 선택을 만드는지 먼저 증명한다.

## 12. 소환 경제 v1

현금 결제 없음, 기간 한정 FOMO 없음.

기존 v0.4의 300/3000 비용과 ★1~5 확률표는 정본에서 폐기한다.

초기 v1 tuning target:
- 1회: 300 Crystal
- 10회: 3000 Crystal
- ★5 base: 2%
- ★4: 15%
- ★3: 83%
- 10회 안에 최소 ★4 1명
- ★5 soft pity: 45부터 점진 상승
- ★5 hard pity: 60
- pity는 banner/풀 변경으로 사라지지 않음
- 정식 limited 50/50 구조는 초기 버전에서 사용하지 않음

이 수치는 “완료 수치”가 아니라 첫 economy simulation baseline이다.

### 획득 속도 목표

오프라인/개인 플레이 RPG이므로 모바일 과금게임보다 훨씬 관대하게 설계한다.

- 초반 30~45분 내 첫 10회 소환
- 새 콘텐츠를 정상적으로 탐험하는 플레이에서 대략 2~3시간마다 추가 10회 수준을 첫 목표로 측정
- 메인 진행에 특정 가챠 캐릭터를 요구하지 않음
- 천장 이전에도 파티 선택지가 충분히 늘어날 수 있게 ★3/★4 모두 실전 가치 보유

## 13. 중복 캐릭터

중복은 캐릭터의 핵심 기능을 잠그는 열쇠가 아니다.

- 첫 획득: 캐릭터 unlock
- 중복: Star Essence 중심 변환
- 중복으로 skill 자체를 해금하지 않음
- 중복 횟수가 없으면 캐릭터가 미완성인 구조 금지

필요하다면 소량의 multiplier bonus를 별도 실험할 수 있지만, 기본 방향은 **중복 = 다른 선택권으로 환원**이다.

## 14. 보상 설계

필드전:
- Gold + XP 기본
- 장비/재료는 적 테마에 맞게

첫 클리어/발견:
- Crystal 비중 증가
- 지도/빠른 이동/상점/새 의뢰 같은 access reward 가능

Boss:
- 큰 XP/Gold
- 보장 장비/선택 보상
- Crystal
- 새 지역/캐릭터/시스템 access

반복 사냥이 메인 퀘스트보다 항상 더 효율적이지 않게 한다.

## 15. 밸런스 검증 방식

캐릭터를 “DPS 1위” 한 줄로 평가하지 않는다.

각 캐릭터마다:
- 10-action damage
- 20-action damage
- 받은 피해 감소/회복량
- 생성/소모한 Gauge 가치
- Reaction 기대 횟수
- setup에 필요한 own turns
- Auto 손실
- 단일/다수/보스/짧은전/긴전
을 비교한다.

SPD/Gauge 캐릭터는 raw damage가 아니라 **추가로 만든 팀 행동의 가치**까지 계산한다.

수치는 외부 게임에서 비율/설계 원리를 참고하되 TURNBOUND simulator와 실제 플레이 결과로 최종 고정한다.

## 16. 구현 불변조건

1. Presentation 시간이 전투 결과를 바꾸지 않는다.
2. 플레이어 선택 중 논리시간 정지.
3. Gauge overflow 보존.
4. 자연 SPD 연속 행동을 임의 cap으로 자르지 않음.
5. Reaction과 정규 행동 구분.
6. Reaction은 기본적으로 자신의 regular-action cooldown을 줄이지 않음.
7. 1x/2x 동일 입력 → 동일 논리 결과.
8. 서버 권위.
9. UI timeline = 실제 scheduler의 미래 시뮬레이션.
10. 동일 seed/state/input에서 결과가 deterministic.

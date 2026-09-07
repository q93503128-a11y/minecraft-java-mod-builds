# Riftfrontier Canonical Direction Lock

이 문서는 Riftfrontier의 최상위 프로젝트 정본이다.

새 기능을 제안하거나 구현할 때 이 문서와 충돌하면 **새 아이디어가 아니라 이 문서가 우선**한다. 방향을 바꾸려면 이유와 영향 범위를 먼저 기록한 뒤 정본을 명시적으로 갱신한다.

---

## 1. 한 문장 정체성

**Riftfrontier는 Minecraft 안에 차원 탐사, 액션 RPG, 개척 물류, 산업·연구, 세력 시뮬레이션과 동적 사건이 서로 원인을 주고받는 하나의 독립 게임을 구축하는 프로젝트다.**

---

## 2. 우리가 만들지 않는 것

다음은 규모가 커 보여도 정본 방향이 아니다.

- 차원 수만 많은 포털 모음집
- 숫자만 다른 몹 수백 종
- 공격력만 커지는 장비 계단
- 체력과 공격력만 큰 보스
- 서로 연결되지 않은 미니게임 모음
- 퀘스트 마커를 따라가는 일방향 RPG
- 카드/반투명 사각형을 반복한 임시 UI
- 매 tick 전수 스캔으로 흉내 낸 시뮬레이션
- 기능이 존재한다는 이유만으로 완료된 알파 품질 콘텐츠
- 특정 상용 게임이나 모드의 코드·아트·UI를 그대로 복사한 결과물

---

## 3. 규모의 정의

Riftfrontier에서 규모는 다음 순서로 평가한다.

1. **시스템 간 상호작용 수**
2. **플레이어 선택이 만들어내는 결과의 다양성**
3. **지역 하나의 콘텐츠 밀도와 완결성**
4. **재사용 가능한 production pipeline의 확장성**
5. 단순 콘텐츠 개수

즉 `300 mobs`보다 `30 reusable behaviours × data definitions × world interactions`를 우선한다.

---

## 4. 핵심 기둥

### 4.1 Expedition

플레이어는 안전한 거점과 위험한 원정지를 오가며 준비, 탐사, 전투, 회수, 철수 결정을 반복한다. 원정은 자원 채굴만이 아니라 구조, 추적, 조사, 사냥, 호송, 방어, 유물 회수, 세력 의뢰, 보스 공략을 포함한다.

### 4.2 Action RPG

전투는 바닐라 수치 증가만으로 확장하지 않는다. 무기 계열, 공격 동작, 방어/회피, 상태, 스킬/모듈 조합, 적 행동 읽기와 telegraph를 통해 깊이를 만든다. 직업은 고정 클래스보다 장비·숙련·스킬·전문성의 조합을 우선한다.

### 4.3 Frontier & Logistics

원정에서 얻은 성과는 거점, 전초기지, 생산, 저장, 운송, 수리, 보급과 연결된다. 개척은 장식 건축이 아니라 다음 원정을 더 멀리·더 안전하게·더 효율적으로 가능하게 하는 시스템이다.

### 4.4 Industry & Research

기술과 제작은 단순 recipe 목록이 아니라 생산 계열과 연구 선택으로 구성한다. 자동화는 편의 기능이 아니라 장기 성장과 물류의 핵심 축이 된다.

### 4.5 Factions & World Response

세력은 상점 UI가 아니다. 자원 수요, 관계, 통제 지역, 의뢰, 경쟁, 교역, 위험 대응을 가진다. 플레이어의 원정과 거래가 일부 세계 상태를 바꾸고, 세계 사건이 다시 플레이어의 선택지를 바꾼다.

### 4.6 Dense Region Packs

각 지역/균열권은 별도 DLC처럼 완성한다. 숫자를 위해 20개 지역을 동시에 얕게 만들지 않는다.

---

## 5. 지역 패키지 완료 조건

지역 하나는 가능한 범위에서 아래 묶음을 갖춘 뒤에만 production-complete 후보가 된다.

```text
Region Identity
├─ terrain/worldgen
├─ environmental rule or hazard
├─ resource loop
├─ ambient sound/presentation
│
Ecology
├─ passive/neutral life where appropriate
├─ hostile archetypes
├─ elite/apex encounter
│
Exploration
├─ landmarks
├─ structures
├─ secrets/discoveries
├─ dynamic encounters
│
Progression
├─ unique materials
├─ crafting/research use
├─ loot identity
│
Challenge
├─ dungeon or equivalent major site
├─ boss/major encounter when appropriate
│
World Integration
├─ faction/economy effects
├─ contracts/quests/events
├─ bestiary/guide/map integration
│
Quality Gate
├─ data validation
├─ GameTest where automatable
├─ actual playtest
├─ presentation review
└─ worst-case performance check
```

모든 지역이 똑같은 체크박스를 기계적으로 채울 필요는 없지만, **지역 고유의 플레이 이유와 세계 연결**은 반드시 있어야 한다.

---

## 6. 첫 Vertical Slice

첫 수직 구간은 시각 테마를 지금 확정하지 않는다. 디자인은 별도 reference study 이후 잠근다.

기능적으로는 다음을 한 번에 검증한다.

- 중앙 거점 1곳
- 진입 가능한 원정 지역 1곳
- 지역 고유 환경 규칙 1개 이상
- 공통 behaviour를 공유하지만 역할이 다른 일반 적 archetype 여러 개
- elite 1종 이상
- 읽고 피할 수 있는 주요 보스 1종
- 무기/전투 계열 최소 2종
- 조합 가능한 전투 옵션 최소 1계열
- 회수 가능한 지역 자원과 거점 투자 루프
- 저장/보급/제작의 최소 물류 루프
- 세력 또는 계약 시스템의 최소 실제 연결
- 사건 1종 이상
- bestiary/guide 또는 동급 인게임 설명
- 서버 권위 상태와 저장
- 실제 GameTest/플레이/성능 검증

이 수직 구간이 품질 기준을 통과하기 전에는 지역 수를 폭발적으로 늘리지 않는다.

---

## 7. 디자인·아트 강제 규칙

- 프로젝트 디자인을 AI가 즉석에서 발명하지 않는다.
- UI는 실제 상용 게임 UI와 검증된 대형 모드의 문제 해결 방식을 먼저 조사한다.
- 모델/애니메이션은 실제 Minecraft 스케일과 silhouette, hitbox, animation weight를 검수한다.
- 재배포 가능한 외부 자산은 적극 활용할 수 있으나 출처와 라이선스를 기록한다.
- 허용되지 않는 외부 자산은 공개 저장소에 넣지 않는다.
- 핵심 화면은 목업 또는 충분한 reference breakdown 없이 바로 Java Screen으로 만들지 않는다.
- 최종 판단은 코드가 아니라 실제 Minecraft 화면과 플레이로 한다.

---

## 8. 기술 강제 규칙

- content ID와 저장 키는 첫 플레이어블 이후 안정성을 최우선한다.
- 서버 권위 상태를 기본으로 한다.
- data-driven을 기본값으로 하되 복잡성을 숨기는 JSON 만능주의는 피한다.
- 공통 behaviour와 composition을 우선하고 클래스 복제를 피한다.
- 새로운 콘텐츠 유형에는 schema와 validator를 함께 설계한다.
- 공식 NeoForge API/Event/Hook을 우선하고 내부 구현/Mixin 의존은 마지막 선택이다.
- 성능은 추측하지 않고 profiling한다.
- 전역 tick scan을 기본 구조로 사용하지 않는다.
- 라이브러리는 목적별 단일 선택을 우선한다.

---

## 9. 완료의 정의

`implemented`, `compiles`, `registered`, `button exists`는 완료를 뜻하지 않는다.

완료 후보는 최소한:

```text
정본 요구 충족
→ 자동 검증
→ clean build
→ 실제 게임 동작
→ 실제 화면/연출 검수
→ worst-case 성능 확인
→ 회귀 없음
→ JAR 검증
```

을 거친다.

---

## 10. 작업자가 매번 자문할 질문

1. 이 기능이 다른 시스템과 연결되는가, 아니면 고립된 체크박스인가?
2. 같은 결과를 더 재사용 가능한 규칙으로 만들 수 있는가?
3. 이 콘텐츠가 하나 늘어날 때 코드 비용도 거의 같은 만큼 늘어나는 구조인가?
4. 플레이어에게 실제 새로운 선택을 주는가?
5. 외형·사운드·UI가 기능 수준보다 뒤처지고 있지 않은가?
6. 실제 최악 조건에서 버틸 수 있는가?
7. 첫 vertical slice를 더 완성하기 전에 범위만 늘리고 있지 않은가?

하나라도 위험 신호라면 먼저 구조 또는 품질을 고친다.

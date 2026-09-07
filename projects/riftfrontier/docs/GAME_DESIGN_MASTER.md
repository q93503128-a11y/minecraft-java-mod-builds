# Riftfrontier — Master Game Design

이 문서는 시스템 수준 게임 디자인의 정본이다. 시각 스타일, 최종 명칭, 개별 몬스터 외형, UI 장식은 별도 reference/design gate에서 결정한다.

## 1. 장르

**Open-world Expedition Action RPG + Frontier Logistics + Industry/Research + Faction Simulation + Dynamic Events**

핵심은 서로 다른 장르 요소를 많이 넣는 것이 아니라, 한 시스템의 결과가 다른 시스템의 입력이 되게 만드는 것이다.

---

## 2. 플레이어 판타지

플레이어는 정해진 영웅이 아니라 미지의 영역을 개척하는 사람이다.

가능한 장기 역할 예:

- 전투 중심 탐험가
- 보스 사냥꾼
- 희귀 자원 전문 채집가
- 장비 제작/개조 전문가
- 물류 운영자
- 상인
- 기술 연구자
- 위험 지역 전초기지 운영자
- 특정 세력의 용병/협력자
- 여러 역할을 섞은 자유 플레이

어떤 역할도 다른 역할의 단순 하위 호환이 되어서는 안 된다.

---

## 3. 메타 루프

```text
PREPARE
장비 · 소모품 · 보급 · 계약 · 목표 선택

DEPLOY
균열/원정 지역 진입

EXPLORE
탐사 · 전투 · 수집 · 사건 · 구조물 · 비밀

DECIDE
더 깊이 갈지, 목표를 바꿀지, 철수할지 판단

EXTRACT
성과와 손실을 안고 귀환

INVEST
장비 · 시설 · 연구 · 생산 · 물류 · 관계에 투자

WORLD RESPONSE
가격 · 세력 · 사건 · 위험도 · 접근 가능 지역이 반응

REPEAT AT HIGHER COMPLEXITY
```

원정은 매번 보스 처치로 끝날 필요가 없다. 짧은 자원 회수, 구조, 호송, 방어, 조사, 사냥, 정찰, 긴 던전 원정이 같은 기반 위에서 공존한다.

---

## 4. 전투

### 4.1 기본 원칙

- 바닐라 좌클릭 DPS만으로 성장하지 않는다.
- 적의 사전 신호와 공격 판정이 외형적으로 일치해야 한다.
- 강한 행동은 회피·방어·중단·거리 관리 등 최소 한 가지 대응법을 가진다.
- 보스는 `telegraph → attack → recovery` 리듬을 기본으로 한다.
- 플레이어 빌드는 고정 클래스보다 구성 요소의 조합을 중심으로 한다.

### 4.2 플레이어 전투 구성

```text
Weapon Family
+ Weapon-specific moves
+ Active Skills / Modules
+ Passive Traits
+ Equipment properties
+ Utility tools
```

첫 vertical slice에서는 최소 2개의 충분히 다른 무기 계열만 완성한다. 숫자를 빠르게 늘리지 않는다.

### 4.3 적 구성

적은 종 이름보다 전투 역할과 behaviour composition을 먼저 정의한다.

예:

- pursuer
- skirmisher
- ranged pressure
- area denial
- support
- ambusher
- tank/controller

지역 몬스터는 공통 behaviour를 재조합하되, silhouette·속도·공격 리듬·환경 사용 방식으로 구분한다.

---

## 5. 성장

### 5.1 장비 성장

장비는 공격력 숫자만 높은 tier로 교체하는 구조를 피한다.

성장은 다음 축을 조합한다.

- 기반 성능
- 행동 변화
- 특성/모듈
- 재료 계열
- 제작 품질 또는 개조
- 특정 환경 대응
- 세트/시너지보다 개별 선택의 의미 우선

### 5.2 숙련/전문성

행동을 통해 관련 전문성이 성장할 수 있으나 단순 반복 노가다를 강제하지 않는다. 전문성은 새로운 제작법, 효율, 선택지, 특수 행동을 여는 데 사용한다.

### 5.3 연구

연구는 선형 기술 트리 하나가 아니라 플레이 스타일에 따라 우선순위를 고르는 구조를 목표로 한다.

- expedition technology
- logistics
- processing/industry
- equipment engineering
- rift science
- settlement infrastructure

---

## 6. 거점·전초기지·물류

거점은 메뉴 허브가 아니라 플레이의 실제 생산 기반이다.

핵심 기능:

- 저장
- 보급
- 제작/가공
- 수리/개조
- 연구
- 계약/세력 접점
- 운송
- 원정 준비

전초기지는 장식 건축이 아니라:

- 원정 거리 단축
- 안전한 보급
- 자원 처리
- 위험 지역 통제
- 세력 영향

에 실제 영향을 준다.

물류 시스템은 처음부터 완전한 공장 시뮬레이션으로 만들지 않는다. vertical slice에서 `입수 → 저장 → 가공 → 소비/운송` 최소 연결을 완성한 뒤 확장한다.

---

## 7. 세력과 경제

세력은 최소한 다음 상태 중 일부를 실제 데이터로 가진다.

- relationship
- territory/influence
- resource demand
- available contracts
- threat response
- trade preferences
- strategic goals

경제는 완전 현실 시뮬레이션이 목적이 아니다. 플레이어가 알아챌 수 있는 의미 있는 변화만 시뮬레이션한다.

예:

```text
희귀 광물 대량 공급
→ 해당 세력의 관련 생산 활성화
→ 특정 장비/연구 접근성 변화
→ 경쟁 세력의 수요/계약 변화 가능
```

---

## 8. 동적 사건

사건은 독립 랜덤 팝업이 아니라 세계 상태와 연결한다.

유형 예:

- 지역 생태 변화
- 균열 불안정
- 세력 충돌
- 자원 발견
- 수송 위기
- 거점 공격
- 보스/대형 개체 이동
- 탐사대 실종

사건은 가능한 경우 `조건 → 발생 → 선택/행동 → 세계 상태 변화` 구조를 가진다.

---

## 9. 균열과 지역

균열은 차원 개수를 늘리는 장치가 아니다. **서로 다른 게임 규칙을 가진 고밀도 탐사권을 연결하는 장치**다.

한 지역은 다음 중 몇 가지를 고유하게 가져야 한다.

- traversal rule
- environmental hazard
- resource ecology
- combat pressure
- visibility/navigation rule
- local event structure
- faction interest
- progression gate

지역 시각 테마와 아트 방향은 reference study 전에 확정하지 않는다.

---

## 10. 던전과 보스

### 던전

`Handcrafted landmark + modular room/encounter composition`의 혼합을 기본 후보로 한다.

절대 금지:

- 의미 없이 복도/방만 무작위 연결
- 동일 적만 숫자로 늘린 전투방 반복

### 보스

보스는 다음을 최소 설계 단위로 본다.

```text
combat identity
phase/state
attack selection
telegraph
hit volume
counterplay
recovery
arena interaction
animation
sound
reward/progression meaning
```

---

## 11. 생활/비전투 시스템

생활 시스템은 별도 미니게임 컬렉션이 아니라 핵심 루프에 연결되는 것만 우선한다.

후보:

- 채굴/채집
- 제작/개조
- 가공
- 거래
- 운송
- 연구
- 전초기지 관리

낚시·농업·요리 등은 전체 방향에 실제 의미가 생길 때 추가한다. “대형 모드니까 있어야 한다”는 이유로 넣지 않는다.

---

## 12. 멀티플레이 원칙

현재 구현은 싱글플레이에서도 완전해야 하지만 구조는 멀티플레이를 막지 않는다.

- 세계 상태: 서버 권위 공유
- 세력/경제/사건: 서버 권위 공유
- 플레이어 장비/숙련/계약: 플레이어별 상태
- 원정 파티: 후속 단계에서 공동 목표/보상 규칙 설계

싱글플레이 구현 편의를 위해 클라이언트 전용 상태를 정본 게임 상태로 사용하지 않는다.

---

## 13. 엔드게임 철학

엔드게임은 단일 최종 보스 후 종료가 아니라 여러 축을 지원한다.

- 최고 위험 원정
- 세계 사건 대응
- 고난도 보스
- 고급 제작/개조
- 깊은 연구
- 세력 관계/영향력
- 광역 물류/전초망 완성
- 희귀 발견 수집

다만 모든 축을 동시에 강제하지 않는다.

---

## 14. 첫 vertical slice의 성공 판정

플레이어가 실제 게임에서 다음 문장을 느낄 수 있어야 한다.

> 준비를 다르게 하면 원정 방식이 달라지고, 원정에서 무엇을 얻고 잃었는지가 다음 준비와 세계 상태를 바꾼다.

이 감각이 없다면 시스템 수가 많아도 vertical slice는 실패다.

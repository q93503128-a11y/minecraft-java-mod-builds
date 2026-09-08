# EARTH TO STARS — Master Game Design

이 문서는 EARTH TO STARS의 게임 자체에 대한 최상위 정본이다.

공용 `QUALITY_STANDARD.md`, `QUALITY_STANDARD_GAME_DESIGN.md`, 저장소 `AGENTS.md`, 문승준 Minecraft High-Quality Development Playbook의 원칙을 이 프로젝트에 구체화한다.

---

# 1. 제품 비전

EARTH TO STARS의 핵심 판타지는 단순하다.

> **평범한 Minecraft 오버월드에서 살아남던 플레이어가 산업과 우주비행 기술을 만들고, 직접 하늘을 뚫고 올라가 우주를 개척하며, 하나의 함선을 계속 키워 이동수단·집·공장·채굴선·전함으로 변화시킨다.**

이 프로젝트의 규모는 메뉴 수나 행성 수가 아니라 **플레이 공간이 실제로 확장되고, 함선이 실제 플레이 방식을 바꾸며, 새 자원이 다음 원정을 가능하게 하는 연결된 루프**에서 나온다.

---

# 2. 핵심 기둥

## 2.1 Minecraft Survival

초반은 Minecraft다.

- 채집
- 채굴
- 제작
- 건축
- 농사/식량
- 지상 탐험
- 몹 대응

기존 Minecraft를 삭제하고 별도 게임으로 갈아끼우는 것이 아니다. 우주 시스템이 들어와도 Overworld의 자원·거점·교통·생산 가치는 남는다.

## 2.2 Physical Expansion

성장은 메뉴에서 다음 지역을 누르는 것이 아니라 플레이 공간의 확장이다.

```text
지상
→ 고고도
→ 상층 대기
→ 저궤도
→ 달
→ 소행성권
→ 행성권
→ 외행성/심우주
```

플레이어는 최초 우주 진입을 명확한 사건으로 기억해야 한다.

## 2.3 Ship Growth

함선은 장비 슬롯 하나가 아니라 두 번째 플레이어 캐릭터다.

성장은:

- 더 빠름
- 더 튼튼함

만이 아니라:

- 더 멀리 감
- 더 오래 생존함
- 새로운 환경에 진입함
- 새로운 광물을 채굴함
- 더 큰 화물을 옮김
- 생산을 함선 안에서 처리함
- 소형기를 운용함
- 자동 방어가 가능함
- 새로운 무기 방식이 생김

으로 표현한다.

## 2.4 Cooperative Operation

멀티에서는 한 함선을 여러 플레이어가 자연스럽게 함께 운용할 수 있다.

그러나 역할을 직업처럼 강제하지 않는다.

- 혼자: automation/assistance가 빈 자리를 채운다.
- 둘: 조종 + 전투/탐사 분담이 자연스럽다.
- 셋 이상: 주포, 조종, 전력/정비, 탐사 등 선택적 협력이 가능하다.

누구도 반복 게이지 관리만 하는 노동자가 되어서는 안 된다.

---

# 3. 핵심 게임 루프

```text
지구에서 준비
→ 자원/연료/탄약/장비 확보
→ 함선에 모듈 구성
→ 직접 출항
→ 궤도/천체/소행성/구조물 탐사
→ 채굴·전투·구조·회수·발견
→ 가치 있는 자원/기술 확보
→ 함선 또는 지상 거점으로 귀환
→ 정제/연구/제작/함선 개수조
→ 새로운 이동거리/환경/전투 방식 개방
→ 더 위험하고 먼 원정
```

좋은 원정은 최소 두 가지 압력을 동시에 가진다.

예:

- 더 오래 남으면 자원은 늘지만 연료/산소/적 위협이 커짐
- 전투를 피하면 안전하지만 희귀 salvage를 놓침
- 화물칸을 늘리면 채굴 효율은 좋아지지만 전투력/기동성이 줄어듦
- 강한 무장을 달면 전력·질량·탄약 요구가 늘어남

---

# 4. 초반: 지구 시대

## 4.1 출발점

새 월드는 기본 Overworld에서 시작한다.

초반 30~60분 동안 SF 시스템이 바닐라 생존을 압도해서는 안 된다.

플레이어는 먼저:

- 철
- 구리 계열 재료
- 레드스톤
- 연료
- 기본 작업 공간

을 통해 초기 전기/가공 기술에 진입한다.

## 4.2 초기 산업

초기 산업은 거대한 기계 메뉴 게임이 아니라 우주 진입에 필요한 현실적 목적을 가진다.

핵심 설비 후보:

- 소형 발전기
- 배터리
- 금속 가공기
- 정밀 부품 제작기
- 연료 정제기
- 산소/압력 장비
- 기본 센서 제작대

각 기계는 명확한 역할이 있어야 하며, 같은 기능의 상위 기계를 여러 개 늘리지 않는다.

## 4.3 첫 비행체

첫 비행체는 거대 함선이 아니다.

권장 경험:

```text
작은 cockpit
+ 소형 fuel tank
+ chemical engine
+ 작은 cargo
```

이 단계의 목적은 우주전이 아니라 **하늘이 더 이상 세계의 끝이 아니라는 것**을 보여주는 것이다.

---

# 5. 지상 → 우주 진입

우주 진입은 이 게임의 대표 장면이다.

## 5.1 플레이어가 느끼는 흐름

1. 지상 이륙
2. 구름층 통과
3. 대기음/바람 변화
4. 하늘이 점점 어두워짐
5. 대기압/산소 경고 등장
6. 별이 보이기 시작
7. 지구 지평선 표현 변화
8. 엔진/선체 진동 변화
9. 우주층 진입
10. 지구가 아래에 남음

## 5.2 내부 구현 원칙

실제 Minecraft 기술 구조에서는 차원 또는 공간 레이어 전환을 사용할 수 있다.

하지만 다음은 금지한다.

```text
고도 도달
→ 화면 정지
→ 행성 선택 메뉴
→ 버튼 클릭
→ 갑자기 다른 차원
```

전환은 가능한 한 위치/방향/속도/함선 상태를 연속적으로 이어야 한다.

## 5.3 재진입

귀환도 콘텐츠다.

후기에는:

- 재진입 각도
- 열
- 감속
- 대기 항력
- 선체 보호

가 의미 있는 요소가 될 수 있다.

단, Kerbal 수준의 계산 노동을 강요하지 않는다. 보이는 정보와 선택은 직관적으로 유지한다.

---

# 6. B형 모듈식 함선

함선은 arbitrary moving blocks가 아니다.

서버에는 하나의 `ShipState`가 존재하며 모듈과 시스템을 소유한다.

## 6.1 함선 구성 축

### Core
- command core / cockpit
- flight computer
- ship identity

### Propulsion
- atmospheric engine
- chemical rocket
- maneuver thruster
- advanced drive

### Power
- generator
- battery
- reactor
- power distribution

### Survival
- oxygen
- pressure
- thermal control
- emergency reserve

### Logistics
- cargo
- fuel tank
- ammo storage
- resource buffer

### Industry
- refinery
- fabricator
- repair module
- research module

### Sensors
- short-range radar
- long-range scanner
- mining scanner
- targeting computer

### Combat
- turret hardpoint
- fixed weapon hardpoint
- missile rack
- point defense
- shield/defensive module if later approved

### Utility
- mining beam
- tractor/salvage system
- docking interface
- drone bay
- hangar

## 6.2 성장 단계

### Stage A — Launch Craft
- 1~2명 규모
- 짧은 우주 체류
- 수동 조종
- 화물 소량
- 방어 거의 없음

### Stage B — Orbital Workhorse
- 달/궤도 반복 운용
- basic life support
- cargo 확장
- mining/salvage 도구
- 소형 자동 방어 가능

### Stage C — Expedition Ship
- 장거리 체류
- onboard refining
- 여러 weapon hardpoint
- sensor network
- crew interior 의미 증가

### Stage D — Mobile Base
- 생산/정비/저장
- drone/hangar
- 고급 reactor
- 여러 플레이어가 역할을 나눌 수 있음

### Stage E — Capital-Class Vessel
- 심우주 원정
- 대형 무장
- 함재기/드론
- 높은 유지비
- 큰 장점과 명확한 기동/비용 페널티

대형함이 무조건 정답이 되지 않게 한다.

---

# 7. 함선 내부

## 7.1 원칙

함선 외부 이동 객체와 내부 플레이 공간은 논리적으로 연결하되 물리적으로 같은 좌표계일 필요는 없다.

권장:

```text
Ship Exterior Object
       │ shipId
       ▼
Ship Interior Instance
```

## 7.2 이유

이 방식은:

- 멀티 위치 동기화
- 함선 회전 중 플레이어 충돌
- 블록엔티티 이동
- 청크 이동
- 저장
- 렌더링

리스크를 크게 줄인다.

## 7.3 내부의 의미

내부를 단순 장식 공간으로 두지 않는다.

- cockpit/bridge: 직접 조종 및 상황 판단
- engineering: 긴급 수리/출력 조절
- cargo: 원정 결과 확인
- workshop: 제작/정비
- hangar: 소형기/드론
- airlock: EVA 진입

하지만 모든 방을 관리 미니게임으로 만들지 않는다.

---

# 8. 함선 전투

전투는 단순 DPS 교환이 아니다.

## 8.1 전투 축

- 위치
- 방향
- 사거리
- 발사각
- 함체 차폐
- 열
- 전력
- 탄약
- 센서
- 무기 회전속도
- 목표 우선순위

## 8.2 무기군

최종 개수는 콘텐츠 제작 단계에서 조정하지만 역할은 명확히 나눈다.

### Autocannon
- 탄약 소비
- 근/중거리
- 빠른 표적 대응
- 초반 범용

### Heavy Cannon / Coilgun
- 느린 조준
- 큰 충격
- 장갑/대형 표적

### Laser
- 탄약 대신 전력/열
- 정확도 우수
- 지속 사용 시 과열

### Missile
- 유도
- 제한 탄약
- 방어에 대응 가능

### Point Defense
- 미사일/소형 위협 대응
- 자동 운용 가치가 큼

### Mining / Utility Beam
- 전투용과 경제용 역할 구분

## 8.3 manual / auto

무기 자체와 제어 방식을 분리한다.

```text
Weapon
+ Mount
+ Sensor access
+ Control mode
```

Control mode 후보:

- OFF
- MANUAL
- ASSISTED
- AUTO DEFENSE
- AUTO TARGET

### Manual
- 더 나은 weak-point 조준
- ammo conservation
- 상황 판단
- 재미있는 직접 플레이

### Auto
- 솔로 지원
- 다중 포탑 방어
- point defense
- 후반 자동화 보상

자동 AI가 항상 인간보다 완벽해서 직접 조작이 무의미해지지 않게 한다.

---

# 9. 센서와 중앙 전투 시스템

포탑마다 독립적으로 세상을 검색하지 않는다.

```text
SensorGrid
→ Track List
→ Threat Evaluation
→ WeaponController
→ Turret Assignment
```

센서 성장도 gameplay growth다.

초기:
- 시야에 보이는 적 위주

중기:
- radar contact
- 거리/속도

후기:
- missile track
- resource signature
- subsystem targeting support

---

# 10. 전력 / 탄약 / 열

## 10.1 전력

함선 전력은 중앙 grid에서 계산한다.

소비:
- propulsion
- life support
- sensors
- weapons
- refinery
- shields if adopted

전력 부족 시 모든 것이 동시에 꺼지는 것보다 priority policy를 둔다.

예:

```text
Life Support > Control > Defense > Propulsion reserve > Industry
```

플레이어가 필요하면 우선순위를 바꿀 수 있지만, 복잡한 회로 편집을 필수로 하지 않는다.

## 10.2 탄약

함선 저장고와 weapon system을 논리 네트워크로 연결한다.

실제 아이템이 파이프 내부를 매 tick 이동할 필요는 없다.

## 10.3 열

열은 모든 장비에 붙이는 귀찮은 게이지가 아니라 강력한 시스템의 trade-off에만 사용한다.

예:
- laser
- reactor overdrive
- atmospheric re-entry
- high-output engine

---

# 11. 자원 설계

행성마다 광석 10개씩 만들지 않는다.

## 11.1 자원 역할

새 자원은 적어도 하나를 바꿔야 한다.

- 이동 범위
- 함선 크기
- 생존 가능 환경
- 무기 방식
- 자동화
- 생산 효율
- 탐지 능력
- 새로운 지역 접근

## 11.2 자원 분류

핵심 기능 분류 예:

- structural materials
- conductive/electronic materials
- propulsion materials
- thermal materials
- high-energy materials
- exotic late-game materials

같은 역할의 자원을 이름만 바꾸어 늘리지 않는다.

---

# 12. 지역 진행

정확한 수치와 광물 목록은 `02_WORLD_PROGRESSION_CONTENT.md`가 담당한다.

큰 흐름:

```text
Earth
→ Earth Orbit
→ Moon
→ Near-Earth Asteroids
→ Mars / inner-system region
→ Belt / advanced asteroid region
→ Outer-system region
→ Deep Space
```

모든 천체가 동일한 구조를 가지면 안 된다.

차이는 최소 다음 중 여러 축에서 생긴다.

- gravity
- atmosphere
- temperature
- radiation
- terrain
- local resource
- enemy/ecology
- navigation
- mission type
- base viability

---

# 13. 탐사 콘텐츠

우주는 빈 검은 공간 + 광물 블록만으로 끝내지 않는다.

탐사 대상 후보:

- 폐기 위성
- 파손된 정거장
- 구조 신호
- 소행성 내부 동굴
- 자동 방어 플랫폼
- 연구 잔해
- 화물선 wreck
- 희귀 현상
- 적대 세력 전초기지
- 자연 천체 위험

콘텐츠 하나는 가능하면 자원/전투/이야기/함선 선택 중 두 개 이상을 연결한다.

---

# 14. 적과 전투 콘텐츠

SF라고 모든 적을 로봇으로 만들지 않는다.

가능한 범주:

- autonomous machines
- pirate/scavenger craft
- defense drones
- hostile fauna on selected bodies
- environmental hazards
- large mechanical threats

대표 적/보스는:

- 전용 모델
- 애니메이션
- 사운드
- 읽을 수 있는 공격
- 실제 hitbox/판정 정합

이 필요하다.

바닐라 엔티티는 prototype proxy로 사용할 수 있지만 production 대표 적의 최종 외형으로 남기지 않는다.

---

# 15. 지상 거점과 우주정거장

함선이 성장해도 지상 건축 가치를 없애지 않는다.

## Earth Base
- 대량 생산
- 저장
- launch infrastructure
- 초기 연구
- 농업/자원 공급

## Orbital Station
- docking
- zero-g/space production advantages
- refueling
- logistics hub

## Planetary Outpost
- 현지 자원 확보
- 장거리 원정 중간 거점
- 위험 환경 대응

함선은 최고의 이동성, 거점은 최고의 안정성과 대량 처리라는 차이를 둔다.

---

# 16. 자동화 성장

반복 작업은 progression을 통해 자동화한다.

```text
직접 채굴
→ ship mining tool
→ scanner-assisted mining
→ drone mining
→ automated resource expedition
```

```text
직접 포탑 조준
→ aim assist
→ auto defense
→ multi-turret fire control
```

자동화는 처음부터 재미를 삭제하는 기능이 아니라 **이미 배운 반복 작업을 후반 기술로 압축하는 보상**이다.

---

# 17. 멀티플레이 게임성

## 17.1 공동 함선

함선 권한 기본 프리셋:

- Owner
- Crew
- Guest

세부 권한은 실제 필요가 생긴 경우만 추가한다.

## 17.2 자연스러운 협업

전투 중 예:

- 한 명: 조종 + 자동방어
- 두 명: 조종 / 포수
- 세 명: 조종 / 주포 / utility-defense
- 네 명 이상: 탐사/EVA/정비가 추가

그러나 UI가 플레이어 수에 따라 붕괴하지 않아야 한다.

## 17.3 개인 진행과 공동 진행

개인 progression과 ship progression을 분리한다.

- 개인: 장비, EVA 능력, 도구 숙련/접근
- 함선: 모듈, drive, reactor, storage, weapons, sensor

공동 함선에서 누가 접속하지 않았다고 전체 진행이 멈추지 않게 한다.

---

# 18. Nether / End 정책

## 18.1 메인 루트

Nether와 End를 한 번도 방문하지 않아도 심우주까지 갈 수 있어야 한다.

## 18.2 Nether

선택적 가치:

- 고온 재료 조기 획득
- 특수 열처리
- 위험한 연료 변형
- 장식/전문화 부품

그러나 동일 기능의 우주 메인루트 재료가 존재해야 한다.

## 18.3 End

선택적 가치:

- exotic physics 계열 재료
- 특수 이동/센서/공간 관련 sidegrade
- 고급 장식/희귀 기술

Ender Pearl/Chorus 같은 바닐라 자원을 메인 우주 기술 필수 재료로 고정하지 않는다.

## 18.4 원칙

Nether/End는 **지름길 또는 다른 빌드**이지 숙제 체크리스트가 아니다.

---

# 19. 실패와 사망

우주 원정 실패가 모든 진행을 날리는 극단적 구조일 필요는 없다.

그러나 위험이 무의미해서도 안 된다.

손실 후보:

- 현장 미회수 cargo
- damaged modules
- repair cost
- fuel/ammo loss
- temporary rescue requirement

멀티에서 다른 플레이어가 구조할 수 있는 여지도 가치가 있다.

정확한 death/ship destruction 정책은 실제 vertical slice 플레이테스트 후 고정한다.

---

# 20. UI 원칙

HUD는 상황에 따라 필요한 정보만 보여준다.

## 지상
- Minecraft 기본 경험을 과도하게 덮지 않음

## flight
- 속도/고도 또는 space-relative movement
- fuel/propellant
- power warning
- target/contact

## combat
- target
- weapon state
- ammo/heat
- damage alert

## engineering
- power budget
- damaged systems
- critical alerts

화면 모든 모서리에 계기판을 상시 띄우지 않는다.

최종 아트 방향은 `03_UI_ART_REFERENCE_GATE.md`가 승인되기 전 임의 제작하지 않는다.

---

# 21. 사운드와 연출

SF 경험에서 사운드는 중요하다.

대표 상태는 소리로도 구분한다.

- atmospheric engine
- rocket ignition
- upper-atmosphere transition
- vacuum/ship interior contrast
- radar/contact
- weapon family
- hull impact
- low power
- decompression/emergency
- docking

우주 진공 외부 사운드는 현실성보다 게임 가독성과 내부 전달음을 고려하되 일관된 규칙을 세운다.

---

# 22. 난이도와 복잡성

하드 SF 시뮬레이터를 그대로 만드는 것이 아니다.

복잡한 계산은 시스템 내부에 두고 플레이어에게는 의미 있는 선택으로 변환한다.

나쁜 예:
- 수십 개 수치를 매번 계산

좋은 예:
- `추력 부족`, `연료 여유`, `열 한계`, `안전한 재진입 범위`를 읽기 쉬운 피드백으로 제공

숙련자는 세부 정보를 더 볼 수 있어도 초보자가 필수로 계산기를 켜게 만들지 않는다.

---

# 23. 콘텐츠 밀도 규칙

새 행성/지역을 추가하기 전에 기존 지역이 다음을 갖췄는지 확인한다.

- 고유 환경 압력
- 고유 보상
- 새 함선/플레이 선택
- 대표 탐사 대상
- 시각/사운드 정체성
- 최소 하나의 기억할 장면
- 기존 루프로 돌아가는 출력

이 조건을 못 채운 행성 5개보다 완성된 달 1개가 우선이다.

---

# 24. 첫 완성 목표

전체 태양계를 먼저 만들지 않는다.

첫 production-quality vertical slice는:

```text
Earth base
→ launch craft
→ seamless-feeling orbital transition
→ orbital salvage encounter
→ manual/auto turret interaction
→ Moon landing
→ lunar resource collection
→ return to Earth
→ one meaningful ship upgrade
```

까지다.

이 한 사이클이 재미있고 안정적이며 멀티 구조를 막지 않는다는 것을 검증한 뒤 확장한다.

---

# 25. Definition of Good

EARTH TO STARS가 좋은 게임으로 느껴지는 상태는 다음과 같다.

- 처음 우주에 나갈 때 장면이 기억에 남는다.
- 함선을 업그레이드하면 단순 숫자가 아니라 할 수 있는 일이 달라진다.
- 혼자 해도 답답하지 않고, 친구와 하면 자연스럽게 역할이 생긴다.
- 자동화가 귀찮음을 줄이지만 직접 플레이의 재미를 죽이지 않는다.
- 새로운 천체에 가는 이유가 단순히 다음 광물 등급 때문만은 아니다.
- Overworld가 버려진 튜토리얼 지역이 되지 않는다.
- Nether/End는 선택지이지 의무가 아니다.
- UI와 함선 외형이 일반적인 AI SF 임시 디자인처럼 보이지 않는다.
- 대형함/자동포탑이 많아져도 서버가 무너지지 않는다.
- 코드, 저장, UI, VFX, 사운드, 실제 플레이가 하나의 게임 경험으로 연결된다.

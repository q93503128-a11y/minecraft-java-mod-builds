# EARTH TO STARS — World Progression & Content

이 문서는 지구에서 심우주까지의 진행 구조, 각 구간의 역할, 자원 설계 규칙, 반복 루프와 확장 순서를 정의한다.

정확한 수치·드롭률·광물 분포는 플레이테스트 전 하드코딩하지 않는다. 이 문서는 **무엇이 왜 존재하는가**를 고정하고, 실제 수치는 data-driven 값으로 조정한다.

---

# 1. 진행 철학

진행은 다음 원칙을 따른다.

1. 새 지역은 단순히 더 높은 숫자의 광물을 주는 곳이 아니다.
2. 새 자원은 새 행동·새 선택·새 이동 범위·새 생존 환경·새 자동화 중 하나 이상을 연다.
3. Earth는 튜토리얼 지역이 아니라 장기 거점으로 남는다.
4. Nether/End는 선택지이며 메인 진행 게이트가 아니다.
5. 한 천체를 얕게 여러 개 만드는 것보다, 한 지역이 작은 DLC처럼 완결되게 한다.
6. 이동 시간이 지루함이 되지 않게 한다. 장거리 이동은 기술 성장으로 압축된다.
7. 플레이어가 직접 하던 일을 후반 자동화가 대신하는 구조를 선호한다.

---

# 2. 메인 진행 지도

```text
EARTH
  ↓
HIGH ATMOSPHERE
  ↓
EARTH ORBIT
  ↓
MOON
  ↓
NEAR-EARTH ASTEROIDS
  ↓
MARS / INNER SYSTEM
  ↓
MAIN BELT
  ↓
OUTER SYSTEM
  ↓
DEEP SPACE
```

각 단계는 이전 단계의 기술을 버리지 않고 새로운 요구를 추가한다.

---

# 3. Earth

## 역할

- Minecraft 생존의 기반
- 초기 산업
- 첫 launch craft 제작
- 장기 대량 생산 거점
- 귀환/보급의 안정적 허브

## 핵심 자원

기존 Minecraft 자원을 최대한 재사용한다.

- Iron — 기본 구조재
- Copper — 전력/전도
- Redstone — 제어/센서/전자계통
- Coal/Charcoal — 초기 탄소/연료
- Sand/Glass — 광학/실리콘 계열 가공 베이스
- Gold — 고급 전자부품에 제한적으로 사용

신규 지구 자원은 꼭 필요할 때만 추가한다.

### 후보: Bauxite / Aluminum

목적:
- 경량 구조재
- 초기 항공/로켓 프레임
- 단순 철 기반 구조와 차별화

도입 여부는 P0 이후 실제 제작 루프를 보고 확정한다. 철/구리/기존 자원만으로 초기 비행이 충분히 재미있으면 신규 광석을 억지로 추가하지 않는다.

## 해금

- basic generator
- battery
- metal processing
- electronics
- fuel processing
- oxygen equipment
- launch craft

## 대표 콘텐츠

- 초기 지상 테스트장
- 발사대/격납고
- 기상/고도 변화
- 첫 고고도 시험비행

---

# 4. High Atmosphere

독립 광물 지역이 아니라 **지구와 우주의 경계 콘텐츠**다.

## 압력

- 산소 감소
- 대기압 감소
- 열/항력 변화
- 추진 방식 변화

## 플레이 의미

- 지상 엔진만으로는 한계가 생김
- 산소/압력 장비의 필요성 체감
- 첫 우주 진입의 연출 구간

## 보상

여기서 새로운 광석을 캐게 하지 않는다.

보상은:
- 우주 접근 자체
- orbital navigation unlock
- 첫 궤도 contact

이다.

---

# 5. Earth Orbit

첫 번째 진짜 SF 플레이 공간.

## 환경

- vacuum
- microgravity/low-gravity representation
- Earth visual anchor
- orbital debris
- satellites
- abandoned hardware

## 핵심 루프

```text
launch
→ orbital approach
→ scan contacts
→ salvage / repair / fight
→ cargo 확보
→ Earth 귀환
→ 첫 함선 개수조
```

## 자원

새 광석보다 salvage 중심.

- Salvaged Electronics
- Precision Components
- Orbital Alloy Scrap
- Data Core / Research Data

### 역할

**Salvaged Electronics**
- 센서 개선
- aim assist
- 자동 포탑의 초기 제어계

**Precision Components**
- 정밀 servo
- docking
- 고급 flight control

**Orbital Alloy Scrap**
- 재활용 구조재
- 첫 orbital-grade module 제작

**Research Data**
- 단순 재화가 아니라 특정 기술 family unlock에 사용
- 별도 화폐처럼 남발하지 않음

## 대표 콘텐츠

- 고장 난 위성
- 작은 debris field
- 폐기된 작업선
- 자동 방어 드론
- distress signal

## 첫 성장 변화

Earth Orbit을 반복하면:

- 수동 포탑 → assisted targeting
- 짧은 우주 체류 → 안정적 life support
- 소형 cargo → 반복 원정 가능

으로 바뀐다.

---

# 6. Moon

첫 외계 천체.

## 디자인 목표

Moon은 “회색 Overworld + 새 광석”이 아니다.

플레이어가 처음으로:

- 저중력
- 진공
- 장거리 지상 이동
- 지구가 하늘에 보이는 환경
- 함선이 유일한 생명선인 느낌

을 경험해야 한다.

## 핵심 자원

### Lunar Ilmenite

가공 결과:
- Titanium 계열 구조재

역할:
- 가벼운 함체
- 고온 부품
- 더 나은 thruster
- 장거리 expedition frame

### Lunar Volatiles

극지/특정 지역에서 획득.

역할:
- oxygen/fuel production 보조
- 현지 보급 거점의 첫 의미

### Helium-3 Candidate

초기에는 잠금.

역할 후보:
- 후반 fusion 관련 고급 연료

초반부터 He-3를 캐서 바로 최종 발전기를 만드는 식으로 쓰지 않는다.

## 대표 콘텐츠

- crater field
- shadowed polar region
- abandoned probe
- buried impact material
- lunar cave/void candidate
- first permanent outpost

## 해금

- titanium structural family
- better maneuvering thrusters
- improved EVA equipment
- basic lunar outpost
- longer-range sensor

---

# 7. Near-Earth Asteroids

함선 채굴과 위험/보상 루프를 처음 본격적으로 검증하는 지역.

## 특징

- 작은 목표물
- 이동하는 채굴 위치
- 함선 접근/정렬 중요
- 일부는 불안정하거나 적대 세력이 선점

## 핵심 자원

### Nickel-Iron Mass

역할:
- 고강도 합금
- 대형 frame
- heavy weapon 구조

### Platinum-Group Metals

역할:
- 촉매
- 고효율 전력/전자 장비
- 고급 센서

### Rare Inclusion

희귀 소행성 내부에만 존재하는 고가 자원 family.

정확한 물질명은 실제 후반 tech tree가 결정된 뒤 확정한다. “우주 보석” 같은 의미 없는 판타지 자원을 먼저 만들지 않는다.

## 대표 선택

- 작은 안전한 asteroid
- 큰 자원량 + hostile defense
- 위험한 회전/파편
- rare signature 추적

## 해금

- ship mining beam
- heavy cargo
- automated mining assistance
- first heavy weapon
- improved reactor components

---

# 8. Mars / Inner System

Mars는 두 번째 주요 행성형 지역으로, Moon과 다른 문제를 요구한다.

## 환경

- 얇은 대기
- dust
- 넓은 이동 거리
- temperature swing
- surface weather/hazard

## 핵심 재미

Moon이 “첫 외계 생존”이라면 Mars는 “원정 기지 운영”을 강화한다.

## 자원 역할

신규 자원 개수는 2~3개 수준으로 제한한다.

후보 역할:

- advanced thermal ceramic
- chemical processing catalyst
- high-capacity storage material

정확한 광물 이름은 실제 지형/과학 reference 조사와 함께 확정한다.

## 대표 콘텐츠

- dust storm navigation
- buried structure/wreck
- canyon exploration
- local resource extraction
- medium-term outpost

## 해금

- better thermal management
- longer independent operation
- advanced refinery
- medium expedition ship

---

# 9. Main Asteroid Belt

Near-Earth Asteroids보다 규모와 위험이 큰 경제/전투 지역.

## 차이

- 더 긴 원정
- 더 많은 contacts
- pirate/scavenger pressure
- 대형 resource body
- mining automation 가치 상승

## 보상

- 대형함 구조재
- 고효율 reactor material
- advanced weapon material
- industrial-scale resource throughput

## 해금

- drone mining
- multi-turret fire control
- large refinery
- larger cargo/ship frame
- independent mobile base gameplay

---

# 10. Outer System

후반부에는 단순히 적 HP와 자원 tier만 올리지 않는다.

## 새로운 압력 후보

- 극저온
- 약한 태양광
- 긴 보급 거리
- radiation belts
- gas giant atmosphere approach
- icy moon hazards

## 자원 역할

- advanced coolant/superconductive materials
- high-energy fuel sources
- late-game drive components

정확한 재료는 외행성 콘텐츠를 실제 제작할 때 별도 research gate를 통과한다.

## 해금

- long-range drive
- capital-class power
- deep-space sensor
- high-end automation

---

# 11. Deep Space

게임의 “숫자 끝”이 아니라 자유로운 최고 난도 탐사 영역.

## 요구

- 완성도 높은 expedition ship
- 장기 자립
- 높은 sensor capability
- advanced repair
- 강력한 전투 대응

## 콘텐츠 방향

- rare anomalies
- unknown structures
- extreme resource sites
- hostile high-end craft
- long-range discoveries

무한 반복 procedural soup가 아니라, 제작된 주요 discovery와 재사용 가능한 사건 시스템을 섞는다.

---

# 12. Earth가 버려지지 않게 하는 법

우주 기술이 발전한 뒤에도 Earth는 다음 장점을 가진다.

- 가장 값싼 대량 생산
- 농업/식량
- 안정적 산소
- 대규모 저장
- 초기 자원 대량 공급
- 핵심 fabrication infrastructure
- 공동 서버의 사회적 거점

후반 함선이 모든 것을 완벽하게 대체하지 않는다.

함선 = 이동성/현장 대응

거점 = 대량 처리/안정성/저비용

으로 역할을 구분한다.

---

# 13. Nether Optional Route

Nether 방문은 다음과 같은 **선택적 이익**을 줄 수 있다.

- 고온 재료를 더 빨리 확보
- 특정 thermal recipe 우회
- 위험하지만 높은 에너지 연료 변형
- 특수 장식/engine cosmetic

하지만 메인 루트에는 항상 Earth/space 대체 경로가 있어야 한다.

Validator는 progression graph에서 Nether-only 필수 노드를 감지해야 한다.

---

# 14. End Optional Route

End는 후반 side route.

가능한 이익:

- exotic navigation sidegrade
- unusual sensor technology
- mobility specialty
- cosmetic/rare module family

End 방문이 없으면 최종 drive를 만들 수 없는 구조는 금지한다.

---

# 15. Progression Gates

진행 게이트는 단순 숫자 레벨이 아니라 실제 능력 요구를 우선한다.

예:

```text
Moon 접근
= vacuum survival + orbital-capable drive + navigation

Asteroid Belt
= range + fuel reserve + sensor + cargo

Outer System
= power + thermal + long-range drive + repair capacity
```

플레이어가 “왜 못 가는지” 이해할 수 있어야 한다.

---

# 16. 기술 연구

Research는 별도 포인트 농장보다 **발견과 제작을 연결하는 unlock system**으로 사용한다.

입력 후보:

- recovered data
- first-time discovery
- sample analysis
- boss/structure technology recovery

출력:

- module family
- recipe family
- sensor capability
- travel capability

Research point를 무의미한 만능 화폐로 만들지 않는다.

---

# 17. 반복 플레이 압축

플레이어가 같은 작업을 충분히 배웠다면 후반 기술이 반복을 줄인다.

```text
manual mining
→ ship mining beam
→ scan filtering
→ drone mining
→ automated expedition
```

```text
manual targeting
→ aim assist
→ auto defense
→ fleet fire-control assistance
```

```text
Earth return every trip
→ orbital depot
→ lunar outpost
→ mobile base
```

이것이 성장의 체감이 된다.

---

# 18. 지역별 콘텐츠 패키지 Definition

새 주요 지역은 출시 가능한 상태가 되려면 최소한 다음을 갖는다.

1. 고유 환경 규칙
2. 고유한 탐사 동선
3. 2~4개의 의미 있는 자원/보상 축
4. 대표 구조물 또는 discovery
5. 최소 하나의 전투/위험 유형
6. 새 ship/build 선택
7. 시각/사운드 정체성
8. 기존 루프로 돌아가는 명확한 보상
9. multiplayer에서 협력이 자연스러운 순간
10. 실제 플레이 검수

이 기준을 통과하지 못하면 새 행성을 추가하지 않는다.

---

# 19. 첫 콘텐츠 완성 순서

```text
Earth
→ Earth Orbit
→ Moon
```

이 세 구간을 먼저 production-quality vertical slice로 완성한다.

그다음:

```text
Near-Earth Asteroids
→ Mars
→ Main Belt
```

으로 확장한다.

Outer System과 Deep Space는 앞 구간의 생산 체계와 성능 구조가 검증된 후 진행한다.

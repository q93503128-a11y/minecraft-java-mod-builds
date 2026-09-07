# Riftfrontier — Content Architecture

이 문서는 대형 콘텐츠를 유지 가능한 구조로 만드는 기술·데이터 설계 정본이다.

목표는 `콘텐츠 10배 = 코드 10배`가 되는 구조를 피하는 것이다.

---

## 1. 기본 원칙

대량 콘텐츠를 Java 클래스 복제로 추가하지 않는다.

가능한 구조:

```text
Content Type
→ Schema
→ Registry/Builder
→ Datagen
→ Runtime Resolver
→ Validator
→ Test Fixture
```

새로운 콘텐츠 종류를 도입할 때 최소한 `등록 방법`, `데이터 스키마`, `실패 검증`, `런타임 조회 방식`을 함께 정의한다.

---

## 2. 기능 중심 패키지

권장 상위 구조 초안:

```text
riftfrontier/
├─ content/
│  ├─ creature/
│  ├─ boss/
│  ├─ item/
│  ├─ skill/
│  ├─ region/
│  ├─ encounter/
│  ├─ faction/
│  ├─ contract/
│  └─ research/
├─ combat/
├─ expedition/
├─ rift/
├─ logistics/
├─ industry/
├─ faction/
├─ worldevent/
├─ persistence/
├─ networking/
└─ client/
```

단순 `entity/`, `item/`, `screen/`, `util/` 거대 폴더로 모든 기능을 흩뜨리지 않는다.

---

## 3. Content Builder / DSL

Create에서 얻은 교훈처럼 반복되는 등록 패턴은 프로젝트 전용 builder로 압축한다.

목표 형태 예시이며 실제 API는 M1에서 NeoForge 26.2에 맞춰 설계한다.

```java
creatures.register("rift_stalker", builder -> builder
    .archetype(SKIRMISHER)
    .region("region_01")
    .stats("rift_stalker")
    .behaviours("stalk", "flank", "melee_combo")
    .loot("rift_stalker")
    .presentation("rift_stalker")
);
```

한 선언에서 파생 가능한 항목은 datagen/validator가 책임진다.

예:

- registry linkage
- tags
- localization key checks
- spawn data linkage
- loot linkage
- bestiary entry presence
- asset reference validation
- test fixture registration

Builder가 모든 게임 로직을 숨기는 거대 마법 API가 되지 않게 한다. 반복 규칙만 추상화한다.

---

## 4. 정식 데이터 타입 후보

초기 core schema 후보:

```text
creature_profile
combat_archetype
boss_profile
attack_pattern
skill_part
item_profile
material_family
loot_profile
region_profile
encounter_profile
faction_profile
contract_profile
research_node
world_event
```

모든 후보를 M1에서 한 번에 구현하지 않는다. vertical slice에 필요한 순서로 추가한다.

---

## 5. Composition 우선

완성 콘텐츠를 복제하기보다 재사용 가능한 부품을 조립한다.

### Creature

```text
Creature Profile
+ Stat Profile
+ Movement Behaviour
+ Combat Behaviours
+ Sensors/Conditions
+ Loot Profile
+ Presentation Resolver
```

### Skill

```text
Delivery/Form
+ Effect
+ Modifier/Augment
+ Cost/Cooldown Rule
+ Presentation
```

### Encounter

```text
Trigger
+ Location Rule
+ Participant Set
+ Objective
+ Escalation Rule
+ Reward
+ World Consequence
```

조합 수를 늘리는 것이 목적이 아니라, 새로운 조합이 **실제로 다른 플레이 선택**을 만들게 해야 한다.

---

## 6. Asset Resolver

게임 데이터와 시각 자산을 직접 1:1 하드코딩하지 않는다.

목표 구조:

```text
Content ID
→ Presentation Profile
   ├─ model
   ├─ texture/variant
   ├─ animation set
   ├─ sound set
   ├─ VFX profile
   └─ icon/UI representation
```

변형은 가능한 경우 base profile + variant/aspect 방식으로 처리한다.

예:

```text
base creature
├─ biome/aspect variant
├─ elite variant
├─ equipment variant
└─ event-corrupted variant
```

단순 색상 변경만으로 콘텐츠 수를 부풀리지 않는다.

---

## 7. Addition/Patch Layer

기본 정의를 여러 기능이 직접 덮어쓰지 않게 한다.

향후 data pack/addon을 고려해 다음 개념을 검토한다.

```text
Base Definition
+ Ordered Additions/Patches
→ Resolved Definition
```

용도:

- 특정 지역 변형 추가
- 특정 세력과의 관계 추가
- compatibility 확장
- 이벤트 기간 modifier

충돌 검출과 적용 순서가 명확해야 하며, 무제한 임의 JSON merge는 허용하지 않는다.

---

## 8. Behaviour Library

적과 NPC의 규모는 behaviour 재사용으로 확보한다.

초기 라이브러리 후보:

### Movement
- pursue
- retreat
- orbit
- flank
- hold_position
- leap

### Combat
- melee_combo
- charge
- ranged_burst
- area_denial
- interrupt
- defensive_guard
- summon_support

### Boss
- telegraphed_slam
- line_charge
- radial_pattern
- phase_transition
- enraged_variant

실제 SmartBrainLib 등 외부 AI 라이브러리는 현재 26.2 지원을 확인하고 필요성이 충분할 때만 도입한다.

---

## 9. Request / Event 구조

MineColonies에서 얻은 핵심 교훈처럼 시스템이 서로 직접 강결합하지 않게 한다.

예:

```text
Expedition consumes supplies
→ LogisticsRequest emitted
→ Storage/production tries to satisfy
→ shortage state generated
→ contract/economy may react
```

또는:

```text
Region threat rises
→ WorldEvent
→ Faction response
→ contract pool changes
→ trade demand changes
```

모든 것을 범용 event bus에 던지지 않는다. 상태 변경 의미가 명확한 domain event와 request 타입을 정의한다.

---

## 10. Datagen

대량 JSON은 손으로 복사하지 않는다.

Datagen 우선 대상:

- tags
- recipes
- loot tables
- model/blockstate where practical
- generated validation indexes
- language stubs where useful
- content catalogs
- GameTest fixture data

생성 파일과 사람이 작성한 정본 데이터를 명확히 구분한다.

---

## 11. Validator

TFC식 교훈을 적용해 데이터 오류를 게임 실행 전 잡는다.

초기 검증 예:

```text
ERROR: missing referenced content ID
ERROR: boss profile has no valid attack pattern
ERROR: attack references missing presentation profile
ERROR: creature has spawn rule but no region link
ERROR: circular research dependency
ERROR: contract objective cannot resolve target
ERROR: duplicate stable content ID
ERROR: asset path missing
ERROR: server-only data references client class
WARN: content has no guide/bestiary integration
WARN: region pack has no unique gameplay rule
```

검증 실패를 로그 경고만 남기고 계속 진행할지, 빌드를 실패시킬지는 오류 등급별로 고정한다.

---

## 12. 저장과 네트워크

- 중요한 게임 상태는 서버가 소유한다.
- 저장 데이터에 schema version을 둔다.
- 첫 플레이어블 이후 ID rename을 매우 제한한다.
- 클라이언트에 필요한 최소 snapshot만 동기화한다.
- 대규모 리스트를 매 tick 전체 전송하지 않는다.
- content definition과 runtime state를 분리한다.

---

## 13. 성능 원칙

대규모 세계 시뮬레이션은 다음 우선순위를 따른다.

```text
Event on state change
→ cached/indexed query
→ scheduled low-frequency work
→ bounded local scan
→ 최후에만 broader scan
```

금지에 가까운 패턴:

- 매 tick 모든 loaded entity 전수 검사
- 매 tick 대형 반경 block scan
- UI가 매 frame 전체 데이터 재구축
- 동일 AI 조건을 여러 subsystem에서 중복 계산
- Display Entity를 개수 제한 없이 상시 생성

실제 budget 수치는 vertical slice에서 spark/JFR로 측정한 뒤 `PERFORMANCE_BASELINE.md`로 잠근다.

---

## 14. 테스트 전략

### Unit Test
- formula
- content resolution
- patch/addition merge
- research dependency
- loot weighting
- state transition

### GameTest
- content registration/runtime linkage
- entity combat state
- region hazard
- interaction
- logistics request chain
- persistence-critical scenario

### Manual
- combat feel
- animation/telegraph alignment
- UI hierarchy
- sound fatigue
- exploration readability
- progression pacing

---

## 15. 콘텐츠 추가 PR/커밋 체크

새 콘텐츠가 들어올 때 최소 확인:

1. 기존 schema/behaviour로 표현 가능한가?
2. 새 Java 클래스가 정말 필요한가?
3. 중복 데이터/자산을 만들고 있지 않은가?
4. reference ID가 validator를 통과하는가?
5. region/world loop와 연결되는가?
6. presentation 자산 또는 명시적 placeholder gate가 있는가?
7. 자동화 가능한 회귀 테스트가 있는가?
8. 최악 규모에서 추가 비용이 예측 가능한가?

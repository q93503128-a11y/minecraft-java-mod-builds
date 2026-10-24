# Augmentation Survival — Development Implementation Plan

Date: 2026-10-24
Status: IMPLEMENTATION-READY PLAN

목표는 146개 증강을 146개의 독립 하드코딩으로 만드는 것이 아니다. 먼저 공통 런타임 spine을 만든 뒤 단순 증강은 데이터/공통 handler로, 복잡한 증강만 전용 behavior로 구현한다.

정본:
- `AUGMENTS.md`
- `AUGMENT_INTERACTION_CONTRACT_2026-10-24.md`
- `AUGMENT_FINAL_COMPATIBILITY_AUDIT_2026-10-24.md`
- `HUD_AND_PLAYER_TEXT_STANDARD.md`
- `ENEMY_POWER_SCALING_DESIGN.md`

## 1. 구현 원칙

1. 서버가 gameplay authority를 가진다.
2. 이벤트/provenance 구조를 증강 구현보다 먼저 만든다.
3. 저장 schema version을 첫 구현부터 둔다.
4. 단순 수치는 data-driven, 복잡 행동은 Java behavior registry.
5. 외부 모드는 adapter/profile 경계 밖으로 새지 않게 한다.
6. 클라이언트는 HUD/UI/VFX/입력 요청을 맡고 피해·드롭·성장 결과를 결정하지 않는다.
7. build를 작은 변경마다 반복하지 않고 의미 있는 구현 단위가 끝날 때 검증한다.

## 2. 권장 패키지 구조

```text
dev.moonseungjun.augmentationsurvival
├─ AugmentationSurvival
├─ augment/
│  ├─ AugmentId
│  ├─ AugmentTier
│  ├─ AugmentDefinition
│  ├─ AugmentCatalog
│  ├─ AugmentBehavior
│  ├─ AugmentBehaviorRegistry
│  └─ behavior/
│     ├─ stat/
│     ├─ combat/
│     ├─ projectile/
│     ├─ movement/
│     ├─ resource/
│     ├─ active/
│     └─ mythic/
├─ state/
│  ├─ AugmentAttachments
│  ├─ PlayerAugmentState
│  ├─ PlayerAugmentStateCodec
│  ├─ StateMigration
│  └─ WorldThreatState
├─ event/
│  ├─ CombatEventRouter
│  ├─ DamagePipeline
│  ├─ HealingPipeline
│  ├─ DeathPipeline
│  ├─ ResourcePipeline
│  └─ MovementPipeline
├─ context/
│  ├─ AttackContext
│  ├─ DamageContext
│  ├─ HealContext
│  ├─ KillContext
│  ├─ ProjectileContext
│  ├─ MiningContext
│  ├─ LootContext
│  └─ AbilityContext
├─ projectile/
│  ├─ ProjectileAugmentState
│  ├─ FlightSegmentState
│  ├─ RicochetController
│  ├─ TrackingController
│  └─ PierceController
├─ ability/
│  ├─ AbilityManager
│  ├─ AbilityInvocationCause
│  ├─ CooldownState
│  ├─ ChargeState
│  ├─ ActiveInstance
│  ├─ ReplayPolicy
│  └─ ActiveSlotManager
├─ acquisition/
│  ├─ AugmentDeviceMenu
│  ├─ AugmentOfferService
│  ├─ AugmentSelectionTransaction
│  └─ CoreUpgradeService
├─ compat/
│  ├─ BossProfile
│  ├─ BossCompatRegistry
│  ├─ BetterCombatCompat
│  ├─ CropCompatRegistry
│  ├─ EnchantmentCompatRegistry
│  └─ external/
├─ world/
│  ├─ ThreatManager
│  ├─ RemoteWarehouseSavedData
│  └─ PortalNetworkSavedData
├─ network/
│  ├─ AugmentPayloads
│  ├─ ServerPayloadHandlers
│  └─ ClientPayloadHandlers
└─ client/
   ├─ hud/
   ├─ screen/
   ├─ tooltip/
   └─ vfx/
```

구현 중 실제 API/매핑에 맞춰 이름은 조정할 수 있지만 책임 경계는 유지한다.

## 3. 증강 정의 데이터

각 증강은 Java enum 하나에 모든 수치를 박지 않는다.

권장 정의 필드:

```text
id
tier
maxLevel
behavior
parameters
playerTextKey
shiftDetailKeys
tags
activeProfile
replayProfile
compatFlags
powerWeightOverride(optional)
```

예:

```json
{
  "id": "augmentation_survival:fast_feet",
  "tier": "common",
  "max_level": 5,
  "behavior": "stat_modifier",
  "parameters": {
    "stat": "movement_speed",
    "percent_per_level": 0.03
  }
}
```

복잡한 `도탄`, `되감기`, `권능 탈취` 같은 증강은 behavior key가 전용 Java handler를 가리킨다.

## 4. 영구 상태 설계

### PlayerAugmentState

최소 상태:

```text
schemaVersion
Map<AugmentId, level>
activeSlots[10]
abilityCooldowns / charges
persistentCounters
huntedEntityTypes
stolenAuthorityBossIds
lastActualDeathPoint
perDeathResources
```

증강 소유/레벨은 사망으로 사라지지 않는다.

상태 객체는 외부에서 임의로 mutate하지 않고 service를 통해 교체/갱신한다.

### 월드 전역 상태

세이브 전체 하나:

```text
worldThreatHighWaterScore
worldThreatStage
```

모든 차원이 같은 값을 읽는다.

### 별도 대형 영구 데이터

- 원격 창고: UUID segment + shared group
- 공간 연결: portal graph

대형 가변 데이터를 플레이어 attachment 하나에 몰아 넣지 않는다.

## 5. 네트워크 설계

### serverbound 요청

```text
UseActiveAbilityPayload(slot/ability, target hint)
EditActiveSlotsPayload(layout)
ReleaseAuthorityPayload()
ConfirmTargetPayload(...)
ConfirmAugmentSelectionPayload(offer token, selections)
Open/ManageRemoteWarehousePayload(...)
```

서버는 요청을 받은 뒤 모든 조건을 재검증한다.

### clientbound

```text
PlayerAugmentSnapshotPayload
PlayerAugmentDeltaPayload
AbilityStateDeltaPayload
WorldThreatPayload
AugmentOfferPayload
TargetPreviewResultPayload
PresentationEventPayload
```

HUD를 위해 전체 상태를 매 tick 보내지 않는다.

## 6. 내부 context / provenance

증강 handler가 vanilla event를 직접 서로 호출하지 않게 한다.

예:

```text
AttackContext
- actor
- target
- provenance
- rootActionId
- referenceDamage
- weaponSnapshot
- projectileId(optional)

ProjectileContext
- physicalProjectileId
- shotActionId
- provenance
- flightSegment
- hitSet
- pierceBudget
- ricochetCount
- chainStarted
- trackingTarget
```

이 context가 무한루프 방지와 Better Combat 호환의 중심이다.

## 7. 액티브 엔진

모든 액티브가 공통 AbilityManager를 사용한다.

필요 기능:
- cooldown
- sequential multi-charge
- duration instance
- target validation
- channel/cancel
- failure = no consumption
- 능력 가속
- 전투 순환
- 이중 충전
- 연장전
- 재연

`재연`은 각 능력의 `ReplayPolicy`를 읽어 50% 효과가 무엇을 의미하는지 명시적으로 처리한다.

## 8. 첫 수직 슬라이스

146개 전부를 시작하지 않는다. 다음 reference augments로 공통 구조를 검증한다.

### 상태 / 스탯
- `강건함`
- `속공`

검증:
- player state 저장/로드
- attribute modifier 중복 없음
- 레벨 변경 즉시 반영

### 전투 provenance / 회복
- `흡혈`
- `한 번 버티기`
- `보호막` 또는 `피해 지연`

검증:
- ROOT vs derived kill
- heal pipeline
- lethal ordering
- separate shield state

### 이동
- `두 번 점프`
- `공중 연계`

검증:
- server-authoritative air-jump charge
- 같은 적 반복 적중으로 무한 체공 가능
- packet spam/invalid jump 거부

### 투사체
- `관통 화살`
- `추적 화살`
- `도탄`

검증:
- flight segment
- 같은 대상 재관통
- segment hit-set
- pierce budget reset
- tracking reacquisition

가능하면 이후 `분산 사격`을 붙여 fan-out이 finite인지 확인한다.

### 액티브
- `치유 파동`

검증:
- active slot
- cooldown
- server validation
- client HUD delta

### 자원
- `풍요`

검증:
- 정상 loot pipeline 후 마지막 ×2
- 진행/코어/고유 NBT 제외

이 수직 슬라이스가 통과하기 전 고등급 100개를 대량 구현하지 않는다.

## 9. 구현 마일스톤

### M1A — Runtime Spine

- augment id/tier/definition loader
- behavior registry
- player persistent state + schema version
- world threat persistent state shell
- networking registration
- common context/provenance
- ability cooldown/charge engine shell

완료 게이트:
- 새 월드/재접속 후 상태 보존
- dedicated server에서 client request validation 구조 동작
- 한 가지 stat augment가 실제 적용

### M1B — Reference Vertical Slice

8절 reference augment 구현.

완료 게이트:
- melee/projectile/heal/death/movement/resource/active 각 최소 한 경로 실제 동작
- 관통→도탄→추적→동일 대상 재관통 확인
- invalid client request가 서버 상태를 바꾸지 않음

### M2 — Core / Device / Acquisition

- 6등급 core item
- 기본/강화 증강 장치
- 3-choice offer
- maxed pool 제거
- 두 번째 선택 transaction
- 3:1 core upgrade
- inventory/device UI

### M3 — Common / Advanced Bulk

공통 handler로 구현 가능한 단순 stat/resource/survival 증강을 먼저 대량 연결한다.

새 handler를 만들기 전에 기존 generic behavior로 표현 가능한지 확인한다.

### M4 — Rare / Heroic Systems

- projectile graph
- movement transforms
- healing/shield/death
- mining/harvest chain
- status interaction
- active skills

### M5 — Legendary / Mythic

- remote warehouse
- portal network
- time stop
- domination
- second choice
- forbidden magic/anvil
- replay
- authority theft

고비용/외부 의존 기능은 여기서 adapter와 함께 구현한다.

### M6 — World Threat / Enemy Scaling

- save-wide threat high-water
- hostile scaling profiles
- normal/elite/boss groups
- HUD `위협 0~5`
- boss encounter snapshot

초기 테이블은 플레이테스트에서 조정한다.

### M7 — External Pack Integration

- loot/core distribution
- Farmer's Delight crop/processing
- Better Combat authority adapter
- Chrono Dawn / World Bosses / MCEndgame boss profiles
- authority packages

### M8 — Presentation / UX

기능이 실제로 동작한 뒤 플레이 화면 기준으로 다듬는다.

- health/shield liquid HUD
- active HUD
- Shift detail tooltip
- augment card visuals
- VFX / sound / animation
- boss authority barrage presentation

### M9 — Playtest / Performance / Multiplayer

- TTK/threat tuning
- worst-case projectile fan-out
- authority barrage scheduler
- shared storage concurrency
- save migration
- actual multiplayer sync

## 10. 테스트 전략

### 순수 로직 테스트 우선 대상

- stat stacking
- damage reduction order
- heal pipeline
- death-prevention order
- cooldown/charge/replay
- selection transaction
- flight segment state machine
- threat stage thresholds

### GameTest / integration 대상

- actual projectile collision/ricochet
- movement/size/collision safety
- block harvest/drop inheritance
- temporary platform
- entity domination
- anvil/enchantment behavior

### 실제 client playtest 필수

- Better Combat feel
- health HUD readability
- active-slot UX
- Shift tooltip density
- projectile VFX readability
- giant/shrink collision feel
- world threat TTK

## 11. 성능 예산

첫 구현부터 profiler 대상 후보:

- 분산+도탄+추적+관통 최대 조합
- 위험 감지/상자 찾기/광석 눈썰미의 탐색 주기
- 되밟기 path buffer
- 지배 10체 AI
- 권능 탈취 전체 barrage
- 원격 창고 동시 접근
- 영역 가속 범위 처리
- threat stage 상승 시 loaded hostile 일괄 갱신

성능 문제가 의심될 때 기능을 임의 축소하지 않고 재현 조건과 profiler로 확인한다.

## 12. 구현 전 파라미터 정책

아직 미정인 탄도 장막/임시 발판/생명 연결/작아지기/벽치기 수치는 handler를 막지 않는다.

- definition JSON에 parameter field를 미리 둔다.
- 테스트용 안전 기본값을 정식 정본처럼 숨겨 고정하지 않는다.
- 해당 증강 구현 차례가 오면 플레이테스트 가능한 초깃값을 별도 결정한다.

## 13. 검증 강도

- 현재 문서 패스: build/CI 없음.
- M1A 코드 단위 완료: 좁은 정적 확인 + 필요 시 compile/build 1회.
- save/network/serialization 변경: 관련 테스트 + build.
- 외부 mod adapter 완료: pack server/client + 대상 gameplay 검증.
- release candidate: `docs/BUILD_STANDARD.md` 전체 검증.

## 14. 현재 개발 준비 상태

- 146 ROSTER LOCKED: YES
- INTERACTION V2: YES
- FINAL COMPAT AUDIT: YES
- IMPLEMENTATION ARCHITECTURE: YES
- FIRST VERTICAL SLICE DEFINED: YES
- RUNTIME SPINE IMPLEMENTED: NO
- CORE/DEVICE IMPLEMENTED: NO
- AUGMENTS IMPLEMENTED: NO
- BUILD/CI THIS PASS: NOT RUN — docs-only
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

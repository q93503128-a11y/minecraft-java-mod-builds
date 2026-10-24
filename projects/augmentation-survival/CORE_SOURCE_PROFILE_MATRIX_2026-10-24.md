# Augmentation Survival — Core Source Profile Matrix

Date: 2026-10-24  
Status: CANONICAL SOURCE-RESOLUTION BASELINE / RUNTIME NOT IMPLEMENTED

이 문서는 `CORE_ACQUISITION_DESIGN_2026-10-24.md`의 코어 경제를 실제 구조물·몹·이벤트·보스 출처에 연결하는 선택자 기준을 정의한다.

실제 확률과 등급 가중치는 이 문서에 복제하지 않는다.

- 일반 구조물/몹/이벤트 수치: `src/main/resources/data/augmentation_survival/augmentation/core_distribution_contract.json`
- 보스 보상 수치: `src/main/resources/data/augmentation_survival/augmentation/boss_core_reward_contract.json`
- 출처 선택자: `src/main/resources/data/augmentation_survival/augmentation/core_source_profiles.json`

## 1. 선택자 우선순위

출처는 다음 순서로 딱 한 번 해석한다.

1. exact override
2. path prefix / family
3. namespace default
4. global fallback

먼저 맞은 규칙이 승리한다.

목적은 두 가지다.

- 특수 보상/고밀도 구조물처럼 반드시 예외 처리가 필요한 곳은 정확하게 제어한다.
- 외부 구조물 모드의 수십~수백 biome 변형 ID를 코드나 데이터에 무식하게 전부 복사하지 않는다.

확인하지 못한 exact registry ID는 추측해서 추가하지 않는다.

## 2. 구조물 매핑

### Vanilla

| 계열 | 기본 프로필 | 보정 |
|---|---|---:|
| mineshaft / shipwreck / ruined portal / ocean ruin / trail ruins / igloo | `routine_exploration` | x1.0 |
| buried treasure | `routine_exploration` | x2.0 |
| desert/jungle pyramid / pillager outpost / fortress / stronghold / normal trial chambers | `dangerous_mid` | x1.0 |
| bastion remnant | `dangerous_high` | x0.8 |
| ancient city | `dangerous_high` | x0.65 |
| woodland mansion | `dangerous_high` | x0.75 |
| end city | `dangerous_high` | x0.8 |
| village 계열 | 제외 | - |

`coreRollMultiplier`는 코어 수량을 곱하는 값이 아니라 **해당 컨테이너의 추가 core roll 확률을 보정하는 값**이다.

마을 상자는 안전하고 밀집도가 높으므로 기본 공급처에서 제외한다.

### Repurposed Structures

정책은 가능한 한 대응되는 vanilla 구조물의 위험도를 상속하는 것이다.

- mineshaft / shipwreck / ruined portal / ocean ruin 계열 -> `routine_exploration`
- stronghold / fortress / pyramid / outpost / temple 계열 -> `dangerous_mid`
- mansion / bastion / ancient-city급 / end-city급 계열 -> `dangerous_high`
- village 계열 -> 제외
- 그 외 미분류 구조물 -> namespace default `dangerous_mid`

외부 모드가 biome별로 같은 구조물의 변형을 많이 생성하므로 exact ID 전체 나열은 하지 않는다. path family가 기본이고, 실제 플레이에서 특정 구조물만 과하거나 약하면 exact override를 추가한다.

### Dungeons and Taverns

상자 밀도가 높은 모드이므로 같은 위험도라도 기본 multiplier를 낮춘다.

- tavern / small / camp 계열 -> `routine_exploration`, x0.8
- outpost / dungeon 계열 -> `dangerous_mid`, x0.7
- fort / large / deep 계열 -> `dangerous_high`, x0.6
- 미분류 -> `dangerous_mid`, x0.65

이 값은 구조물 규모를 실제 플레이한 뒤 조정할 수 있다. 핵심은 **상자 수가 많다는 이유로 코어 기대값이 선형으로 폭증하지 않게 하는 것**이다.

### Illager Invasion 구조물

정확한 구조물별 난이도를 아직 플레이 측정하지 않았으므로 namespace 기본값은 `dangerous_mid`, x0.8이다.

특정 구조물이 실제로 대형 던전/최종 보상 구조라면 exact override로 `dangerous_high` 이상을 준다. 반대로 단순 보조 캠프라면 routine으로 내린다.

### 직접 구조물 공급에서 제외되는 namespace

- Terralith: 바이옴/월드젠 문맥. 코어는 그 안의 구조물·전투 콘텐츠에서 획득한다.
- Naturalist: 야생동물/환경 문맥.
- Farmer's Delight Refabricated: 일반 농사·요리 생산은 progression core를 만들지 않는다.

## 3. 몹 매핑

모든 적대몹을 exact list로 관리하지 않는다.

서버가 일반 적대 개체로 판정한 미지정 몹은 `normal_hostile` fallback을 사용하고, 특별히 강한 몹만 승격한다.

### Vanilla 승격

- `witch`, `vindicator`, `pillager`, `vex`, `piglin_brute`, `shulker` -> `special_hostile`
- `evoker`, `ravager` -> `elite_hostile`
- `elder_guardian` -> 일반 몹 roll을 억제하고 `elder_guardian` 이벤트 프로필
- `warden` -> 일반 몹 roll을 억제하고 `warden` 이벤트 프로필
- `wither`, `ender_dragon` -> 일반 몹 roll을 억제하고 보스 계약으로 전달

### Friends&Foes

2026-10-24 소스 감사에서 실제 entity registration ID를 확인한 전투 개체만 exact override로 넣었다.

- `friendsandfoes:iceologer` -> `elite_hostile`
- `friendsandfoes:illusioner` -> `elite_hostile`
- `friendsandfoes:mauler` -> `special_hostile`
- `friendsandfoes:wildfire` -> `elite_hostile`

그 외 Friends&Foes 개체는 **서버가 적대 개체로 판정한 경우에만** `normal_hostile` fallback을 쓴다. Copper Golem, Moobloom 같은 비적대 개체가 코어 공급처가 되지 않는다.

### Illager Invasion

26.2.x branch의 전투 entity resource는 확인했지만, 이번 패스에서는 개별 registry ID를 registration source까지 확정하지 못했다.

따라서 exact 이름을 추측해 넣지 않고:

- namespace `illagerinvasion`
- 서버 적대 판정 필수
- 기본 `special_hostile`

로 처리한다.

실제 플레이에서 강한 raid unit을 확인하면 그 개체만 exact `elite_hostile` override로 승격한다.

### Naturalist

평범한 야생동물은 코어 드랍 대상에서 제외한다.

증강 성장 때문에 동물 대량 사냥이 최적 루프가 되는 것을 막는다.

## 4. 이벤트 매핑

이벤트는 일반 mob roll과 중복하지 않는다.

- Raid Victory -> `raid_victory`
- Elder Guardian 직접 처치 -> `elder_guardian`
- Warden 플레이어 귀속 처치 -> `warden`

Elder Guardian/Warden은 이벤트 보상이 결정되면 같은 사망에서 일반 몹 코어 roll을 추가로 굴리지 않는다.

## 5. 보스 매핑

보스 source resolver는 **recognized boss encounter**에만 적용한다. namespace에 속한다는 이유만으로 모든 엔티티가 보스 보상을 주는 구조가 아니다.

### Vanilla

- `minecraft:wither` -> `boss_major`
- `minecraft:ender_dragon` -> `boss_pinnacle`
- Ender Dragon 반복 처치 -> `pinnacle_repeat_weights`

### Chrono Dawn

인식된 보스 encounter 기본값은 `boss_major`.

6개 작성 보스의 실제 난이도와 진행 순서를 플레이 측정한 뒤 개별 `boss_standard`/`boss_pinnacle` override를 추가한다.

### World Bosses

인식된 보스 encounter 기본값은 `boss_major`.

최종급으로 확인된 encounter만 `boss_pinnacle`로 승격한다. 일반 raid cache는 보스 kill 보상이 아니라 구조물/cache 프로필을 사용한다.

### MCEndgame

인식된 반복 던전 보스 기본값은 `boss_major`.

Beastweaver는 현재 설계상 `boss_pinnacle` 후보지만 이번 source audit에서 exact registry ID를 검증하지 못했으므로 **비활성 pending override**로만 기록한다. 정확 ID를 추측해서 활성화하지 않는다.

## 6. 밀도와 중복 방지

구조물 코어 보상은 상자마다 무제한 누적되는 별도 loot item이 아니다.

- 자연 loot container당 추가 core roll 최대 1회
- 이미 소비된 자연 loot container는 다시 seed하지 않음
- 플레이어 설치/복제 컨테이너는 자연 구조물 코어 roll을 얻지 않음
- 고밀도 구조물은 multiplier < 1 허용
- 사실상 단일 핵심 상자가 콘텐츠 전부인 경우 multiplier > 1 허용
- multiplier는 수량이 아니라 roll 확률에 적용

보스/이벤트 개인 보상은 ground drop ownership에 의존하지 않고 서버 권한 지급을 사용한다.

## 7. 버전 변화 대응

외부 모드 업데이트 때 우선 확인할 것은 전체 ID 재작성 여부가 아니다.

1. namespace가 유지되는지
2. family/path naming이 크게 바뀌었는지
3. exact override 대상 ID가 사라졌는지
4. 새 pinnacle/elite 콘텐츠가 생겼는지
5. 구조물당 상자 밀도가 크게 달라졌는지

대부분의 biome variant 추가는 namespace/path-family fallback으로 자동 흡수한다.

## 8. 데이터 소유권

역할을 분리한다.

- `core_distribution_contract.json`: 구조물/몹/이벤트의 확률과 등급 분포
- `boss_core_reward_contract.json`: 보스 reward band와 첫 처치/반복 정책
- `core_source_profiles.json`: 실제 source가 어느 profile/band를 쓰는지

즉 확률 밸런스를 조정할 때 source mapping 파일에 수치를 중복 복사하지 않는다.

## 9. 현재 검증 상태

- SOURCE MAPPING DESIGNED: YES
- VERIFIED EXACT FRIENDS&FOES HOSTILE IDS: YES
- ILLAGER INVASION EXACT PER-ENTITY REGISTRY IDS: NOT LOCKED
- EXTERNAL BOSS EXACT PER-BOSS IDS: NOT LOCKED
- DATA CONTRACT WRITTEN: YES
- RUNTIME CONSUMER IMPLEMENTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

현재 단계는 구현 직전 source-resolution 정본이다. 이후 런타임은 이 파일을 읽어 구조물 loot 이벤트, 사망 이벤트, raid/event 완료, boss adapter에서 서버 권한으로 한 번만 보상을 결정해야 한다.

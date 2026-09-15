# 34 — DREHMAL: APOTHEOSIS 26.2 MIGRATION AUDIT

Date: 2026-09-15

## 1. 목적

이 문서는 TURNBOUND: RE의 production world base인 **Drehmal: APOTHEOSIS v2.2.2f**를 Minecraft Java 26.2 + NeoForge 환경에서 실제로 열기 전에 필요한 준비와, 실제 load 이후의 판정 절차를 고정한다.

이 문서는 호환성을 가정하지 않는다. 실제 copied world가 26.2에서 열리고 아래 검사를 통과하기 전까지:

- `WORLD MIGRATION TESTED: NO`
- `PLAYTESTED: NO`
- disabled external-world candidate는 enable하지 않는다.

원본 Drehmal 배포본은 수정하지 않는다. 항상 별도 test copy에서 migration한다.

## 2. 공식 배포 정본

공식 release:
- `Drehmal: APOTHEOSIS v2.2.2f`
- release tag: `v2.2.2f`
- public target: Minecraft Java 1.20.1

공식 installer manifest가 기록한 map payload:
- compressed size: `3,987,641,579` bytes
- uncompressed size: `5,139,218,299` bytes
- map manifest hash: `2e6232dc3e97c77eaa006b09e3ee09b246c46e3df68359dcc1495ce19d1e8053`

공식 release shard:

| File | Size | GitHub release SHA-256 |
|---|---:|---|
| `shard_1.zip` | 1,484,178,230 bytes | `378f8bea9c88371c44d3a6fd6c6d91a886f14b6dfce97e6e6e994789b979b358` |
| `shard_2.zip` | 1,641,289,974 bytes | `2d250f04259404d81a3e9bd83a5592f1c26545f45f9c8a22a2277c9e523c1331` |
| `shard_3.zip` | 861,863,079 bytes | `8c892c77c7ab9ef03aac7b517e5448490f55690163085b38c380523dd2d29b67` |
| `resources.zip` | 190,255,507 bytes | release metadata does not publish a digest |

공식 installer의 shard 처리 방식은 세 ZIP을 **동일 output world directory에 순서대로 extract**하여 하나의 save를 조립하는 방식이다. TURNBOUND는 이 world/resource pack을 저장소에 vendoring하거나 재배포하지 않는다.

공식 Drehmal 안내의 mod set은 Fabric 1.20.1 기반이지만, 해당 안내는 사용되는 mods를 client-side로 설명한다. 따라서 TURNBOUND migration audit의 핵심은 Fabric server dependency 가정이 아니라 다음이다.

- 1.20.1 save의 26.2 DataFix/migration 성공 여부
- embedded datapack/function/command 호환성
- resource-pack 표현 손실 또는 포맷 경고
- 기존 landmark/terrain/entity/block-entity 보존
- TURNBOUND NeoForge runtime과의 충돌 여부

## 3. test copy 조립

1. 공식 release의 `shard_1.zip`, `shard_2.zip`, `shard_3.zip`을 받는다.
2. 가능하면 위 SHA-256을 대조한다.
3. 빈 test output directory를 만든다.
4. shard 1 → 2 → 3 순서로 **같은 output directory**에 extract한다.
5. 완성된 world를 별도 이름으로 Minecraft 26.2 test saves에 복사한다.
6. 원본 1.20.1 배포본은 보존한다.
7. `resources.zip`도 별도 보존하고 26.2에서 pack compatibility/시각 손실을 확인한다.

migration 실패 시 원본을 다시 변환하지 말고 새 test copy에서 재현한다.

## 4. 첫 26.2 load 판정

### 즉시 FAIL

다음 중 하나면 candidate 좌표 검사를 진행하지 않고 migration blocker로 기록한다.

- world open 자체가 crash/soft-lock.
- level/datapack load가 fatal error로 중단.
- dimension/registry 오류로 Overworld가 정상 로드되지 않음.
- New Drabyel 또는 Stasis Facility chunk가 실질적으로 유실/초기화됨.
- 대규모 chunk regeneration/terrain corruption이 관찰됨.

### 경고로 기록 후 계속 검사 가능

- resource-pack format warning.
- 사라진 client-only Fabric 시각 기능.
- 비핵심 command/function warning.
- 일부 custom presentation 손실.

단, 경고가 landmark 식별, 이동, 전투 공간, 채집 또는 TURNBOUND UI 가독성을 깨면 최종 migration PASS가 아니다.

## 5. landmark / candidate 직접 검사

아래 좌표는 `external_world_profiles.json`의 현재 integration seed다. 안전한 도착 블록을 확정한 좌표가 아니다.

| 순서 | 의미 | 좌표 | 현재 상태 |
|---:|---|---|---|
| 1 | New Drabyel Hub | `502 67 1801` | enabled seed |
| 2 | Stasis Facility gateway | `778 31 668` | enabled seed |
| 3 | riverside plot / Drabyel farmland candidate | `516 67 1851` | disabled |
| 4 | ore outcrop / Primal Caverns candidate | `855 65 553` | disabled |
| 5 | river pool / Solvei stream candidate | `1559 75 800` | disabled |
| 6 | patrol / Hunter's Crypt candidate | `325 71 290` | disabled |
| 7 | rift elite / Ruins of Ihted candidate | `1062 65 1097` | disabled |

검사용 기본 명령은 operator 환경에서 다음처럼 사용한다.

```text
/gamemode spectator
/tp @s 502 67 1801
/tp @s 778 31 668
/tp @s 516 67 1851
/tp @s 855 65 553
/tp @s 1559 75 800
/tp @s 325 71 290
/tp @s 1062 65 1097
```

각 지점에서 확인한다.

- chunk/terrain/building이 원본 의도대로 존재하는가.
- arrival 위치가 벽/지하/공중/위험 블록 안이 아닌가.
- Hub/Region landmark가 처음 보는 플레이어에게 구분되는가.
- 실제 이동 동선이 너무 길거나 길찾기가 불명확하지 않은가.
- mining candidate는 실제 채광 동선과 ore 접근성이 있는가.
- farming candidate는 실제 농사 활동으로 읽히는가.
- fishing candidate는 실제 vanilla fishing이 가능한 물과 접근 공간을 갖는가.
- patrol/elite candidate는 4인 파티 + 적 모델 + 카메라 + HUD를 수용할 시야/공간이 있는가.
- TURNBOUND anchor를 놓아도 Drehmal 원본 geometry를 가리거나 훼손하지 않는가.

좌표가 나쁘면 semantic locator는 유지하고 **external profile 좌표만 수정**한다.

## 6. TURNBOUND binding 검사

New Drabyel / Stasis Facility의 migration 상태가 정상일 때만 다음을 수행한다.

```text
/turnbound_re_world_slice validate
```

계약 PASS 후 New Drabyel integration seed 근처에서:

```text
/turnbound_re_world_slice bind_drehmal
```

정상 결과:
- Hub/Region fast-travel anchor가 등록됨.
- invisible semantic Interaction entity만 추가됨.
- Drehmal block/structure/terrain은 수정하지 않음.
- TURNBOUND respawn metadata가 Hub arrival로 설정됨.
- operator 개인 discovery를 setup 단계에서 임의 해금하지 않음.

`bind_drehmal`은 configured Hub에서 192 blocks 이상 떨어진 위치에서는 거부되는 것이 정상이다.

## 7. disabled candidate enable 기준

다섯 candidate는 migration load만 성공했다고 자동 enable하지 않는다.

각 candidate가 다음을 만족할 때만 `enabled=true` 또는 좌표 조정을 검토한다.

- 실제 geography가 semantic role과 맞음.
- 이동/가독성/접근성이 첫 loop에 적절함.
- 기존 Drehmal 진행을 불필요하게 훼손하거나 막지 않음.
- TURNBOUND 생활/Encounter loop에 메뉴 우회 없이 연결 가능.
- patrol/elite는 카메라와 전투 HUD가 실제 공간에서 읽힘.

적합하지 않은 candidate는 억지로 사용하지 않고 다른 기존 Drehmal location을 찾는다.

## 8. 첫 통합 playtest 진입 조건

다음 둘을 만족하면 M7 콘텐츠 확장보다 먼저 첫 통합 playtest로 들어간다.

1. 최근 M5/M6 visual/network 변경을 포함한 build + JUnit + production JAR verify checkpoint 1회 PASS.
2. copied APOTHEOSIS v2.2.2f world가 Java 26.2 + NeoForge에서 실제 load되고 New Drabyel / Stasis Facility가 보존됨.

그 뒤 실제 geography에 맞는 candidate만 enable/조정하고 다음 한 사이클을 플레이한다.

`Hub → party/equipment → Region discovery → life activity → patrol → growth/gear choice → rift elite → reward/completion → fast travel return → save/reload`

## 9. 현재 검증 상태

2026-09-15 현재:
- OFFICIAL SOURCE / MANIFEST REVIEWED: YES.
- SHARD ASSEMBLY METHOD REVIEWED: YES — official installer source.
- TURNBOUND PROFILE / BINDING CODE REVIEWED: YES.
- RECENT LOCAL BUILD CHECKPOINT: NOT RUN in the current assistant execution environment; repository/runtime is not locally mounted and outbound shell network is unavailable.
- CI SUBSTITUTE: NOT RUN. CI로 대체하지 않는다.
- WORLD MIGRATION TESTED: NO.
- DREHMAL LANDMARK INSPECTED UNDER 26.2: NO.
- PLAYTESTED: NO.
- MULTIPLAYER TESTED: NO.

실제 실행하지 않은 항목은 PASS로 승격하지 않는다.

# 18B — IRON GOLEM VANGUARD / BREAKER PRESENTATION GATE

최종 갱신: 2026-09-15  
상태: **EXTERNAL-ONLY BASE APPLIED / CURRENT BUILD NOT RUN / SCREENSHOT PASS PENDING**

이 문서는 `18_CHARACTER_VFX_REFERENCE_GATE.md`를 대체하지 않는다. Iron Golem의 역할/행동 구분은 유지하되, 외형 자체는 TURNBOUND가 재디자인하지 않는다는 production gate다.

## 1. Canonical gameplay identity

현재 production data의 Iron Golem은:

- character: `turnbound_re:iron_golem`
- gameplay source: `minecraft:iron_golem`
- origin: ★5
- roles: `VANGUARD / BREAKER`
- SPD 12 / HP 240 / DEF 52 / POISE 150
- `iron_golem_iron_fist`: enemy single MELEE
- `iron_golem_guardian_plate`: ally single Ward
- `iron_golem_ground_slam`: enemy multi MELEE + high Poise + Intent Delay
- `iron_golem_village_judgment`: enemy single Burst

수호 / 단일 압박 / 광역 붕괴 / Burst의 차이는 authoritative action과 기존 모델의 pose/timing으로 구분한다. 별도 어깨·흉곽·전완 geometry를 새로 만들지 않는다.

## 2. External production base

### GOLEM-P01 — Minecraft Java 26.2 runtime Iron Golem

- source: Minecraft Java 26.2 runtime `ModelLayers.IRON_GOLEM`
- texture: `minecraft:textures/entity/iron_golem/iron_golem.png`
- classification: **DIRECT RUNTIME PRODUCTION BASE / Mojang first-party**
- 사용:
  - Mojang Iron Golem geometry와 proportions를 그대로 사용한다.
  - TURNBOUND는 `head`, `right_arm`, `left_arm`, `right_leg`, `left_leg`의 existing part pose만 exact action에 연결한다.
  - gameplay `minecraft:iron_golem`은 교체하지 않는다.

### GOLEM-R01 / R02 / R03

Mojang Iron Golem 소개, Fresh Animations, Minecraft Dungeons golem 계열은 행동 무게/수호 역할을 이해하기 위한 reference로만 남긴다.

- reference-only 자료를 보고 넓은 흉곽, 큰 분절 전완, Redstone Golem식 mass를 TURNBOUND geometry로 재구성하는 것은 금지한다.
- 직접 사용할 수 있는 external custom asset을 나중에 채택한다면 파일/라이선스/26.2 호환을 먼저 고정한다.

## 3. Presentation contract

### Iron Fist — OFFENSIVE

- exact action id에서만 사용.
- Mojang 모델의 한 팔을 크게 싣고 반대 팔을 보조 자세로 둔다.
- 새 forearm/shoulder geometry를 만들지 않는다.

### Guardian Plate — DEFENSIVE

- ally-target exact action id에서만 사용.
- 기존 두 팔을 전방 방어 자세로 조정한다.
- aggressive flag를 사용하지 않는다.
- 기존 support impact accent + repair 계열 sound 계약을 유지한다.

### Ground Slam — SLAM

- exact action id + multi enemy target에서만 사용.
- 기존 두 팔과 다리 pose로 아래 방향의 준비 동작을 만든다.
- damage/Intent Delay/target은 server event 그대로다.

### Village Judgment — EXECUTE

- exact Burst action id에서만 사용.
- 기존 팔 pose를 비대칭 overhead로 조정한다.
- 별도 glow/geometry로 Burst를 꾸미지 않는다.

### Fail-closed

- RECOVERY / 다른 actor / unknown MELEE는 neutral.
- source가 `minecraft:iron_golem`이 아니면 override 금지.

## 4. Removed legacy design

external-only 규칙 이전의 아래 TURNBOUND 자체 외형은 production에서 제거한다.

- stock보다 넓힌 chest/shoulder geometry
- segmented upper arm / forearm
- segmented thigh / shin
- 128×128 custom UV layout

행동 pose family와 server-authoritative presentation contract만 유지한다.

## 5. Validation history vs current state

과거 custom-model 구현 commit:

- `8da2676a6867d25c3d6f78cfe20af1e26ab2ff83`
- GitHub Actions `Build turnbound-re` #261 / run `34810001527`: SUCCESS

이 성공 기록은 **삭제된 legacy custom geometry 구현만 검증한 과거 기록**이다. 이번 Mojang runtime model 직접 사용 전환의 build 검증으로 재사용하지 않는다.

현재 상태:

- CODE REVIEWED: **commit diff 확인 후 판정**
- TESTED: **NO (current conversion)**
- BUILD VERIFIED: **NO (current conversion)**
- JAR PRODUCED: **NO (current conversion)**
- PLAYTESTED: **NO**
- MULTIPLAYER TESTED: **NO**
- PRODUCTION VISUAL PASS: **NO**

## 6. Later screenshot gate

1. Mojang Iron Golem 실루엣이 Character Detail/Battle Stage에서 동일하게 읽히는가.
2. `Iron Fist`, `Guardian Plate`, `Ground Slam`, `Village Judgment` pose가 서로 혼동되지 않는가.
3. Guardian Plate가 공격처럼 보이지 않는가.
4. target/impact accent가 authoritative target과 일치하는가.
5. TURNBOUND 자체 widened chest/segmented limb가 남아 있지 않은가.
6. 작은 viewport에서 기존 Mojang 모델이 UI에 과도하게 잘리지 않는가.

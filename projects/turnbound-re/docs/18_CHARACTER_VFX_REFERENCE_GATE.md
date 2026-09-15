# 18 — REPRESENTATIVE CHARACTER / SKILL VFX REFERENCE GATE

이 문서는 전체 roster의 최종 미술 정본이 아니다. M5 production presentation에서 대표 캐릭터의 행동 가독성을 확보하면서, 외형 자체는 승인된 외부 production asset/base를 직접 사용하도록 강제하는 gate다.

## 1. 공통 목표

대표 캐릭터의 차이는 이름표가 아니라 전투 중 model/weapon/action timing만 봐도 읽혀야 한다.

- Skeleton: 정밀 원거리 공격자. 실제 3D Bow와 조준/연속 사격.
- Enderman: 공간/위상 공격자. 실제 모델 위치/시선 변화 + Rift travel.
- Blaze: FIRE striker/controller. Mojang Blaze runtime model을 직접 사용하고 enemy FIRE action만 공격 state로 구분.
- Witch: support/controller. Mojang Witch runtime model을 직접 사용하고 공격과 지원 target presentation을 분리.
- Iron Golem: vanguard/breaker. Mojang Iron Golem runtime model을 직접 사용하고 수호/단일/광역/Burst pose를 구분.
- Creeper / Spider / Zombie도 동일하게 Mojang runtime model/texture를 직접 production base로 사용한다.
- server-authoritative damage / target / turn 결과는 presentation이 변경하지 않는다.

## 2. 외부 reference와 직접 사용의 구분

### R-CVFX-001 — Fresh Animations
- official project: https://modrinth.com/resourcepack/fresh-animations
- 사용 상태: **REFERENCE ONLY**.
- 원본 CEM/model/animation 자산을 허용 범위 확인 없이 repository에 포함하지 않는다.
- 더 중요하게, 공개 이미지를 보고 TURNBOUND가 비슷한 replacement geometry를 새로 만드는 것도 금지한다.

### R-CVFX-002 — Fresh Animations: Quivers
- project: https://www.curseforge.com/minecraft/texture-packs/fresh-animations-quivers
- 사용 상태: **REFERENCE ONLY**.
- Skeleton의 원거리 role readability 원칙만 참고한다.

### R-CVFX-003 — Minecraft Dungeons / End family
- official reference: https://www.minecraft.net/en-us/article/meet-enderlings
- 사용 상태: **REFERENCE ONLY / proprietary**.
- Enderman의 phase/rift 행동 문법을 이해하기 위한 reference이며 고유 모델/텍스처/VFX를 복제하지 않는다.

### P-CVFX-001 — Minecraft Java 26.2 runtime entity models
- classification: **DIRECT RUNTIME PRODUCTION BASE / Mojang first-party**.
- current direct bases include `ModelLayers.ZOMBIE`, `CREEPER`, `SPIDER`, `BLAZE`, `WITCH`, `IRON_GOLEM` 및 대응 runtime entity textures.
- TURNBOUND가 새 creature silhouette/UV/skin을 만드는 대신 기존 model part pose와 stage presentation만 authoritative action에 연결한다.

## 3. Skeleton — Marksman signature

Burst `turnbound_re:skeleton_arrow_storm` 및 projectile action 기준:

1. virtual Skeleton은 실제 3D Bow를 장착한다.
2. projectile action의 현재 actor일 때 기존 Skeleton model의 공격/조준 상태를 사용한다.
3. stage render angle을 조정해 Bow와 팔이 정면에 묻히지 않게 한다.
4. Arrow Storm의 추가 화살은 기존 authoritative impact timing에 맞춰 시간차로 이동한다.
5. 추가 화살은 presentation-only이며 damage event 수를 늘리지 않는다.
6. 캐릭터 옆 2D Bow/Arrow 장식으로 정체성을 대신하지 않는다.

## 4. Enderman — Rift signature

Burst `turnbound_re:enderman_horizon_break` 및 VOID action 기준:

1. VOID action actor의 실제 3D model이 짧게 horizontal phase 이동한다.
2. view angle shift와 curved Rift travel을 결합한다.
3. target impact는 authoritative target의 feedback만 사용한다.
4. 2D pearl/frame 장식이나 particle fog로 정체성을 대신하지 않는다.
5. damage/target legality는 server event/snapshot만 따른다.

## 5. Blaze — external-only FIRE signature

현재 production data:
- roles: `STRIKER / CONTROLLER`.
- enemy FIRE: `Ember Bolt`, `Searing Volley`, `Inferno Burst`.
- self FIRE buff: `Heat Up`.

production contract:
1. gameplay/source identity는 `minecraft:blaze` 유지.
2. Character Detail/Battle Stage에서 `turnbound_re:blaze_visual` presentation entity를 사용할 수 있으나 renderer base는 **Minecraft Java 26.2 `ModelLayers.BLAZE`**다.
3. texture는 runtime `minecraft:textures/entity/blaze.png`를 직접 사용한다.
4. TURNBOUND 전용 core, rod size/tier, replacement silhouette, custom UV는 만들지 않는다.
5. canonical enemy-target FIRE action에서만 기존 Blaze model 위에 짧은 action-readable head/pose state를 추가한다.
6. `Heat Up`은 self-target이므로 공격 state를 사용하지 않는다.
7. unknown FIRE / other actor / RECOVERY는 neutral fail-closed.
8. FIRE/VOLLEY travel/impact는 existing authoritative event를 그대로 읽으며 damage event/target을 늘리지 않는다.

### Removed Blaze legacy design

external-only 규칙 이전의 TURNBOUND 자체 3-tier rod/core geometry와 furnace-cage silhouette은 production에서 제거한다. 그 디자인은 외부 production asset을 직접 사용한 것이 아니므로 최종 외형 근거로 인정하지 않는다.

## 6. Witch / Iron Golem / Creeper / Spider / Zombie

세부 gate:
- Witch: `18A_M5_WITCH_SUPPORT_PRESENTATION_GATE.md`
- Iron Golem: `18B_M5_IRON_GOLEM_PRESENTATION_GATE.md`
- Creeper: `18C_M5_CREEPER_PRESENTATION_GATE.md`
- Spider: `18D_M5_SPIDER_PRESENTATION_GATE.md`
- Zombie: `27_STARTER_CHARACTER_VISUAL_PIPELINE.md`

공통 규칙은 동일하다.

- 외형은 Mojang runtime base 또는 실제 반입 가능한 외부 asset을 직접 사용.
- reference-only 자료를 보고 비슷한 geometry를 재구성하지 않음.
- TURNBOUND는 exact action에 필요한 existing part pose, stage motion, target transfer, projectile/impact timing만 연결.
- gameplay source/save/drop/progression authority는 바꾸지 않음.

## 7. 실제 화면 검수용 showcase

기존 operator showcase:

`/turnbound_re_showcase`

정리:

`/turnbound_re_showcase cleanup`

검수 포인트:
- Skeleton: Bow/aim silhouette와 Arrow Storm 연속성이 읽히는가.
- Enderman: phase 이동 + curved Rift가 한 행동으로 읽히는가.
- 외부 runtime model이 reserved viewport 밖으로 잘리지 않는가.
- 이름표/HP/Intent/target marker 가독성을 침범하지 않는가.
- visual target과 server-authored target이 일치하는가.

## 8. 완료 판정

자동 검증 통과만으로 production visual PASS를 선언하지 않는다.

- CODE REVIEWED / TESTED / BUILD VERIFIED는 자동 계약 상태다.
- 실제 Minecraft screenshot에서 silhouette, timing, readability, visual intrusion을 확인하기 전에는 PLAYTESTED가 아니다.
- external-only 전환 뒤의 코드가 과거 custom-model build 성공 기록을 상속하지 않는다.
- 현재 외형 전환 batch는 사용자 요청에 따라 build/CI를 실행하지 않고 묶어 둔 뒤, 의미 있는 checkpoint에서 한 번만 검증한다.

# 18 — REPRESENTATIVE CHARACTER / SKILL VFX REFERENCE GATE

이 문서는 전체 roster의 최종 미술 정본이 아니다.
M5 production presentation 단계에서 **대표 캐릭터 2명(Skeleton, Enderman)** 을 먼저 상용/외부 사례에 맞춰 차별화하기 위한 좁은 vertical-quality gate다.

## 1. 목표

대표 캐릭터의 차이는 이름표가 아니라 전투 중 실루엣과 행동 준비만 봐도 읽혀야 한다.

- Skeleton: 정밀 원거리 공격자. 실제 3D 모델의 활/조준 자세와 다발 사격이 이어지는 방향.
- Enderman: 공간/위상 공격자. 직선 투사체 마법사가 아니라 실제 3D 모델의 위치/시선이 흔들리고 Rift가 이어지는 방향.
- 두 캐릭터 모두 server-authoritative damage / target / turn 결과는 변경하지 않는다.
- presentation은 이미 확정된 authoritative action event를 읽어 짧은 시각 beat만 추가한다.

## 2. 외부 reference

### R-CVFX-001 — Fresh Animations
- official project: https://modrinth.com/resourcepack/fresh-animations
- creator: FreshLX
- 관찰:
  - Minecraft 기본 외형을 버리지 않고 mob의 움직임을 더 dynamic하고 believable하게 만든다.
  - Skeleton과 Enderman 모두 지원 대상이다.
- TURNBOUND: RE 채택:
  - 바닐라 silhouette를 유지한 상태에서 행동 준비 자세와 움직임을 더 명확히 읽게 한다.
  - 캐릭터 고유성은 과도한 새 장식보다 pose / weapon-read / timing에서 먼저 만든다.
- 사용 상태: **REFERENCE ONLY**.
- 이유:
  - 현재 Terms & Conditions는 custom entity model/animation 자산에 별도 조건을 두고 있으며, 공개 배포에서 원본 자산의 무단 재배포를 허용하지 않는다.
  - TURNBOUND: RE 공개 저장소에 Fresh Animations 원본 `.jem/.jpm` 또는 애니메이션 자산을 그대로 포함하지 않는다.

### R-CVFX-002 — Fresh Animations: Quivers
- project: https://www.curseforge.com/minecraft/texture-packs/fresh-animations-quivers
- 관찰:
  - Skeleton의 원거리 정체성을 활만이 아니라 화살/사격 준비 실루엣까지 확장한다.
- TURNBOUND: RE 채택:
  - virtual Skeleton 모델 자체가 3D Bow를 들고, projectile action 동안 Skeleton의 공격/조준 상태를 사용한다.
  - Arrow Storm의 추가 화살은 동시에 한 덩어리로 날리지 않고 기존 authoritative impact timing에 맞춰 시간차를 둔다.
- 사용 상태: **REFERENCE ONLY**.

### R-CVFX-003 — Minecraft Dungeons / End family
- official reference: https://www.minecraft.net/en-us/article/meet-enderlings
- 관찰:
  - End 계열 적은 단순히 보라색 효과를 많이 쓰는 것이 아니라, 각 개체의 능력과 행동 방식 자체로 역할이 갈린다.
- TURNBOUND: RE 채택:
  - Enderman은 generic purple particle caster가 아니라 **phase / rift / 공간 이동** 문법으로 고유성을 만든다.
  - Horizon Break는 actual entity model의 horizontal phase movement + view-angle shift → curved Rift travel → impact 순서로 읽히게 한다.
- 금지:
  - Minecraft Dungeons 고유 모델/텍스처/VFX를 복제하지 않는다.

## 3. 대표 구현 규칙

### Skeleton — Marksman signature

Burst `turnbound_re:skeleton_arrow_storm` 및 projectile action 기준:

1. virtual Skeleton 생성 시 main hand에 실제 3D Bow를 장착한다.
2. Skeleton이 projectile action의 현재 actor일 때 wind-up 및 초기 impact 동안 aggressive/aim 상태를 사용한다.
3. 조준 중에는 stage render angle을 조금 틀어 활과 팔 실루엣이 정면에 묻히지 않게 한다.
4. Arrow Storm은 기존 base projectile 뒤로 추가 화살 2발이 시간차를 두고 따라간다.
5. target impact는 기존 authoritative target + impact feedback을 그대로 사용한다.
6. 추가 화살은 presentation-only이며 damage event 수를 늘리지 않는다.
7. 이전의 actor 옆 2D Bow/Arrow 아이콘은 제거한다. 캐릭터 정체성을 UI 아이콘으로 대신하지 않는다.

의도:
- Skeleton이 단순히 'PROJECTILE 태그를 가진 캐릭터'가 아니라 실제 3D 모델만 봐도 원거리 specialist로 읽힌다.

### Enderman — Rift signature

Burst `turnbound_re:enderman_horizon_break` 및 VOID action 기준:

1. Enderman이 VOID action의 현재 actor일 때 actual 3D model이 짧게 좌우 phase 이동한다.
2. 같은 beat에서 entity render view angle을 흔들어 단순 직선 이동보다 공간이 어긋나는 느낌을 준다.
3. actor→target Rift travel은 직선이 아니라 곡선 offset을 사용한다.
4. impact 시 target에는 기존 WARNING + FOCUS 계열 feedback을 짧게 표시한다.
5. actor 주위 2D Ender Pearl/이중 frame 장식은 제거한다. 실제 모델 움직임을 우선한다.
6. particle fog/glow spam으로 화면을 덮지 않는다.
7. 실제 damage/target legality는 기존 server event/snapshot만 따른다.

의도:
- Enderman의 핵심은 색이 아니라 **실제 모델과 공격 경로의 공간 어긋남**이다.

## 4. 현재 구현 범위와 한계

현재 pass에서 하는 것:
- Skeleton virtual entity에 3D Bow 장착.
- Skeleton projectile action에 실제 model aim/aggressive pose 적용.
- Skeleton aim 시 stage render angle 조정.
- Enderman VOID action에 실제 3D model horizontal phase motion + view-angle shift 적용.
- 기존 Volley/Rift travel/impact timing과 결합.
- character-specific actor 2D weapon/pearl/frame cue 제거.
- 480×270을 포함한 기존 reserved world viewport 안에서만 렌더.

현재 pass에서 아직 하지 않는 것:
- Fresh Animations 자산 복사.
- GeckoLib 기반 전용 skeleton/enderman 모델.
- 전용 texture override.
- 전용 bone animation clip.
- 카메라 연출 확대.
- roster 8명 전체의 최종 외형 확정.

## 5. 다음 gate

대표 2명 실제 Minecraft screenshot audit 후 다음을 결정한다.

1. Skeleton의 바닐라 aim pose가 충분히 강하게 읽히지 않으면 자체 model/GeckoLib 장비·팔 animation으로 승격한다.
2. Enderman phase motion이 단순 흔들림처럼 보이면 bone pose/afterimage 또는 별도 model animation을 검토한다.
3. 대표 2명 방향이 통과하면 Blaze/Witch/Iron Golem 등 다음 3명을 같은 방식으로 reference gate 후 확장한다.
4. 외부 자산을 실제 repository에 포함할 경우 `THIRD_PARTY_ASSETS.md`에 원본 URL/제작자/라이선스/수정 여부를 기록한다.

## 6. 완료 판정

자동 검증 통과만으로 production visual PASS를 선언하지 않는다.

- CODE REVIEWED / TESTED / BUILD VERIFIED는 자동 계약 상태다.
- 실제 Minecraft screenshot에서 silhouette, timing, readability, visual intrusion을 확인하기 전에는 **PLAYTESTED가 아니다**.
- 대표 2명의 screenshot 품질이 통과해야 roster production pass로 넘어간다.

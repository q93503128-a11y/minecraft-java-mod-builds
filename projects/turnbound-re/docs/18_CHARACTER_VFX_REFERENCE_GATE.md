# 18 — REPRESENTATIVE CHARACTER / SKILL VFX REFERENCE GATE

이 문서는 전체 roster의 최종 미술 정본이 아니다.
M5 production presentation 단계에서 **대표 캐릭터 2명(Skeleton, Enderman)** 을 먼저 상용/외부 사례에 맞춰 차별화하기 위한 좁은 vertical-quality gate다.

## 1. 목표

대표 캐릭터의 차이는 이름표가 아니라 전투 중 실루엣과 행동 준비만 봐도 읽혀야 한다.

- Skeleton: 정밀 원거리 공격자. 활을 준비하고 다발 사격이 이어지는 방향.
- Enderman: 공간/위상 공격자. 직선 투사체 마법사가 아니라 위치와 공간이 흔들리는 방향.
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
  - Skeleton Burst wind-up에서 **활 + 장전된 화살**이 model region 안에서 바로 읽히게 한다.
  - Arrow Storm의 추가 화살은 동시에 한 덩어리로 날리지 않고 기존 authoritative impact timing에 맞춰 시간차를 둔다.
- 사용 상태: **REFERENCE ONLY**.

### R-CVFX-003 — Minecraft Dungeons / End family
- official reference: https://www.minecraft.net/en-us/article/meet-enderlings
- 관찰:
  - End 계열 적은 단순히 보라색 효과를 많이 쓰는 것이 아니라, 각 개체의 능력과 행동 방식 자체로 역할이 갈린다.
- TURNBOUND: RE 채택:
  - Enderman은 generic purple particle caster가 아니라 **phase / rift / 공간 이동** 문법으로 고유성을 만든다.
  - Horizon Break는 actor 주변 공간 프레임이 확장되고, Rift travel이 곡선으로 이동하며, impact에서 target frame이 닫히는 흐름을 쓴다.
- 금지:
  - Minecraft Dungeons 고유 모델/텍스처/VFX를 복제하지 않는다.

## 3. 대표 구현 규칙

### Skeleton — Marksman signature

Burst `turnbound_re:skeleton_arrow_storm` 기준:

1. wind-up 중 actor model region에 Focus frame.
2. 활이 actor 손 쪽에서 보이도록 weapon cue를 표시.
3. wind-up 후반에는 장전된 화살 cue 추가.
4. 기존 base projectile 뒤로 추가 화살 2발이 시간차를 두고 따라간다.
5. target impact는 기존 authoritative target + impact frame을 그대로 사용한다.
6. 추가 화살은 presentation-only이며 damage event 수를 늘리지 않는다.

의도:
- Skeleton이 단순히 'PROJECTILE 태그를 가진 캐릭터'가 아니라 화면에서 즉시 원거리 specialist로 읽힌다.

### Enderman — Rift signature

Burst `turnbound_re:enderman_horizon_break` 기준:

1. wind-up 중 actor model region 안쪽에서 시작한 이중 semantic frame이 impact 직전 바깥으로 확장된다.
2. actor 주변 Ender projectile cue가 짧게 움직인다.
3. actor→target Rift travel은 직선이 아니라 곡선 offset을 사용한다.
4. impact 시 target에는 WARNING + FOCUS 계열 중첩 frame을 짧게 표시한다.
5. particle fog/glow spam으로 화면을 덮지 않는다.
6. 실제 damage/target legality는 기존 server event/snapshot만 따른다.

의도:
- Enderman의 핵심은 색이 아니라 **공간이 어긋나는 느낌**이다.

## 4. 현재 구현 범위와 한계

이번 pass에서 하는 것:
- Skeleton Arrow Storm actor weapon-read 강화.
- Enderman Horizon Break actor phase-read 강화.
- 기존 Volley/Rift travel/impact timing과 결합.
- 480×270을 포함한 기존 reserved world viewport 안에서만 렌더.
- 기존 `UiVisualLanguage` semantic frame 재사용.

이번 pass에서 아직 하지 않는 것:
- Fresh Animations 자산 복사.
- GeckoLib 기반 전용 skeleton/enderman 모델.
- 전용 texture override.
- 전용 bone animation clip.
- 카메라 연출 확대.
- roster 8명 전체의 최종 외형 확정.

## 5. 다음 gate

대표 2명 실제 Minecraft screenshot audit 후 다음을 결정한다.

1. weapon cue가 2D item icon처럼 튀어 보이면 자체 model/GeckoLib 장비 표현으로 교체.
2. Enderman phase frame이 UI 장식처럼 보이면 entity motion/pose 쪽으로 비중 이동.
3. 대표 2명 방향이 통과하면 Blaze/Witch/Iron Golem 등 다음 3명을 같은 방식으로 reference gate 후 확장.
4. 외부 자산을 실제 repository에 포함할 경우 `THIRD_PARTY_ASSETS.md`에 원본 URL/제작자/라이선스/수정 여부를 기록.

## 6. 완료 판정

자동 검증 통과만으로 production visual PASS를 선언하지 않는다.

- CODE REVIEWED / TESTED / BUILD VERIFIED는 자동 계약 상태다.
- 실제 Minecraft screenshot에서 silhouette, timing, readability, visual intrusion을 확인하기 전에는 **PLAYTESTED가 아니다**.
- 대표 2명의 screenshot 품질이 통과해야 roster production pass로 넘어간다.

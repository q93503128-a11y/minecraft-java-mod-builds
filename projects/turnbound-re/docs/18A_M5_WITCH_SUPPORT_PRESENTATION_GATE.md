# 18A — WITCH SUPPORT / CONTROLLER PRESENTATION GATE

최종 갱신: 2026-09-15  
상태: **EXTERNAL-ONLY BASE APPLIED / CODE REVIEW PENDING COMMIT / BUILD NOT RUN / SCREENSHOT PASS PENDING**

이 문서는 `18_CHARACTER_VFX_REFERENCE_GATE.md`를 대체하지 않는다. Witch의 gameplay identity와 지원 가독성은 유지하되, 외형 자체는 TURNBOUND가 새로 디자인하지 않는다는 production gate다.

## 1. Canonical gameplay identity

현재 production data의 Witch는:

- character: `turnbound_re:witch`
- gameplay source: `minecraft:witch`
- origin: ★4
- roles: `SUPPORT / CONTROLLER`
- `witch_splash_hex`: enemy single ARCANE damage
- `witch_weakening_brew`: enemy multi ARCANE damage + DEF Down
- `witch_restorative_draught`: ally single heal + DEF Down cleanse
- `witch_cauldron_overflow`: ally multi heal + ATK Up

공격 / 약화 / 회복 / 다중 강화의 차이는 authoritative action/target과 stage presentation으로 읽혀야 하며, 새 모자·가방·robe silhouette 같은 자체 캐릭터 디자인으로 보충하지 않는다.

## 2. External production base

### WITCH-P01 — Minecraft Java 26.2 runtime Witch

- source: Minecraft Java 26.2 runtime `ModelLayers.WITCH`
- texture: `minecraft:textures/entity/witch/witch.png`
- classification: **DIRECT RUNTIME PRODUCTION BASE / Mojang first-party**
- 사용:
  - Mojang Witch model geometry, hat, nose, robe/body proportions를 그대로 사용한다.
  - TURNBOUND는 모델 파트를 새로 만들거나 기존 silhouette를 교체하지 않는다.
  - exact enemy-target action에서만 기존 body/head/arms의 pose를 조정한다.

### WITCH-R01 — Fresh Animations

- source: https://modrinth.com/resourcepack/fresh-animations
- classification: **REFERENCE ONLY / custom terms**
- 사용 상태:
  - 원본 CEM/animation asset 미반입.
  - 공개 이미지를 보고 TURNBOUND 전용 hat/satchel/geometry를 다시 만드는 방식도 금지한다.

### WITCH-R02 — Minecraft Dungeons Enchanter

- behavior reference: https://minecraft.wiki/w/Dungeons:Enchanter
- classification: **REFERENCE ONLY / secondary documentation of proprietary game**
- 허용:
  - 지원 대상이 명확해야 한다는 presentation 원칙만 참고한다.
- 금지:
  - 모델, beam texture, 색/geometry 복제.

## 3. Presentation contract

### Offensive ARCANE

`Splash Hex`, `Weakening Brew`:

- 실제 actor + exact enemy-target action에서만 offensive state.
- Mojang Witch model의 기존 body/head/arms만 이용해 짧은 forward throw commitment를 추가한다.
- 새 외형 파트, 임의 potion rig, 새 robe silhouette는 만들지 않는다.

### Support ARCANE

`Restorative Draught`, `Cauldron Overflow`:

- 공격 pose를 사용하지 않는다.
- Witch 외형은 Mojang runtime base 그대로 둔다.
- caster → authoritative ally target transfer와 recipient success accent는 기존 Battle Stage presentation을 유지한다.
- transfer는 presentation-only이며 heal/buff 수치와 target을 만들지 않는다.

### Recovery / unknown action

- RECOVERY는 neutral.
- 다른 participant가 행동 중이면 neutral.
- unknown ARCANE action은 neutral.
- source entity mismatch면 Witch visual override를 적용하지 않는다.

## 4. Removed legacy design

external-only 규칙 도입 전에 존재했던 아래 TURNBOUND 자체 geometry는 production에서 제거한다.

- asymmetric alchemy hat
- side satchel
- 별도 hunched silhouette용 custom model layer
- reference를 보고 재구성한 외형 차별화

이 요소들은 외부 asset을 직접 사용한 것이 아니므로 production-final로 인정하지 않는다.

## 5. Validation state

이번 전환에서는 사용자 요청에 따라 build/CI를 돌리지 않는다.

- CODE REVIEWED: **commit diff 확인 후 판정**
- TESTED: **NO**
- BUILD VERIFIED: **NO**
- JAR PRODUCED: **NO**
- PLAYTESTED: **NO**
- MULTIPLAYER TESTED: **NO**
- PRODUCTION VISUAL PASS: **NO**

## 6. Later screenshot gate

1. Character Detail과 Battle Stage에서 실제 Minecraft Witch 정체성이 동일하게 보이는가.
2. offensive exact action만 공격 준비 pose로 읽히는가.
3. `Restorative Draught`와 `Cauldron Overflow`가 공격 pose로 오해되지 않는가.
4. support transfer가 HP/Intent/target marker를 가리지 않는가.
5. visual target과 server-authored target이 일치하는가.
6. TURNBOUND 자체 모자/가방/새 silhouette가 남아 있지 않은가.

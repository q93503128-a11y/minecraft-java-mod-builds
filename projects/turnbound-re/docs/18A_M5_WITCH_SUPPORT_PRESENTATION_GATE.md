# 18A — WITCH SUPPORT / CONTROLLER PRESENTATION GATE

최종 갱신: 2026-09-14
상태: **REFERENCE GATE PASS / CODE IMPLEMENTATION TARGET / SCREENSHOT PASS PENDING**

이 문서는 `18_CHARACTER_VFX_REFERENCE_GATE.md`를 대체하지 않는다. 대표 roster 확장에서 Witch의 외형/행동/VFX를 즉흥 설계하지 않기 위한 좁은 보조 gate다.

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

따라서 Witch를 generic ARCANE caster나 보라색 particle 캐릭터로 만드는 것은 역할 정보를 잃는다. 같은 ARCANE tag 안에서도 **공격 / 약화 / 회복 / 다중 강화**가 명확히 달라야 한다.

## 2. External reference screening

### WITCH-R01 — Fresh Animations / Witch

- source: https://modrinth.com/resourcepack/fresh-animations
- reviewed current compatible release: 1.10.5, Minecraft 26.2 지원.
- classification: **REFERENCE ONLY / custom terms**.
- 관찰:
  - 바닐라 Minecraft silhouette를 폐기하지 않고 자세와 미세 움직임으로 entity를 더 dynamic하고 believable하게 만든다.
  - Witch도 현재 지원 대상이며 최근 texture-path 갱신 대상에 포함된다.
- TURNBOUND 채택:
  - Witch라는 즉시 인지 가능한 hat/nose/robe 계열은 유지한다.
  - idle을 완전 정지 자세로 두지 않고 hunched weight shift / restrained sway로 연금술사 성격을 강화한다.
  - 원본 Fresh Animations model/animation asset은 repository에 복사하지 않는다.

### WITCH-R02 — Minecraft Dungeons Enchanter support readability

- behavior reference: https://minecraft.wiki/w/Dungeons:Enchanter
- classification: **REFERENCE ONLY / secondary documentation of proprietary game**.
- 관찰:
  - Enchanter는 직접 공격보다 아군 강화 역할이 중심이다.
  - 강화 시 caster와 recipient 사이에 명확한 enchanted link가 생겨 "누가 누구를 지원 중인지"가 전투 화면에서 즉시 읽힌다.
- TURNBOUND 채택:
  - Witch의 ally heal/buff는 caster 주변 particle spam이 아니라 **caster -> authoritative ally target transfer**로 읽히게 한다.
  - recipient에 성공 계열 impact accent를 짧게 남긴다.
  - Minecraft Dungeons 모델, beam texture, 색/geometry는 복제하지 않는다.

### WITCH-R03 — Vanilla Witch source fantasy

- runtime asset: Minecraft `textures/entity/witch/witch.png`
- classification: **RUNTIME GAME RESOURCE REFERENCE / no copied binary in repository**.
- TURNBOUND 채택:
  - gameplay source가 Witch인 의미를 잃지 않도록 기본 texture identity는 유지한다.
  - presentation model은 별도 TURNBOUND geometry/pose를 사용해 stock renderer 그대로가 되지 않게 한다.

## 3. Selected visual language

### Silhouette / idle

- 낮고 비대칭인 hat silhouette.
- 기본 villager 계열 체형보다 약간 앞으로 숙인 torso.
- side satchel을 추가해 apothecary/tool-bearing role을 읽게 한다.
- arms는 몸 앞에 안정적으로 모으되 완전 정지하지 않는다.
- head/nose/body/satchel에 작은 서로 다른 주기의 움직임만 적용한다.

과한 robe flare, giant cauldron backpack, glowing crystals 같은 새 판타지 장식은 현재 reference 근거가 부족하므로 추가하지 않는다.

### Offensive ARCANE

`Splash Hex`, `Weakening Brew`:

- 실제 actor일 때만.
- WINDUP / IMPACT에서만.
- forward body commitment + compact arm throw silhouette.
- enemy-target action임을 exact action id로 고정한다.
- ARCANE tag 하나만 보고 공격 pose를 선택하지 않는다.

### Support ARCANE

`Restorative Draught`, `Cauldron Overflow`:

- 공격 aggressive pose를 절대 사용하지 않는다.
- 캐릭터 전체는 살짝 위로 들리고 시선 각도가 열리는 non-hostile support pose를 사용한다.
- WINDUP에는 potion-shaped transfer가 caster -> authoritative ally target으로 짧은 arc를 그린다.
- multi ally Burst는 target별 transfer를 소폭 stagger하여 한 덩어리 icon spam을 피한다.
- IMPACT에는 SUCCESS semantic frame을 짧게 사용한다.
- transfer는 presentation-only이며 heal/buff 수치와 target을 만들지 않는다.

### Recovery / unknown action

- RECOVERY는 neutral로 돌아간다.
- 다른 participant가 행동 중이면 neutral.
- unknown ARCANE action은 neutral.
- source entity mismatch면 custom Witch visual override를 적용하지 않는다.

## 4. Implementation boundary

이번 pass:

- `turnbound_re:witch_visual` presentation-only entity.
- Character Detail / virtual Battle Stage shared identity.
- custom TURNBOUND Witch model layer.
- Minecraft Witch texture runtime reference.
- hunched idle + offensive action model pose.
- support action stage pose.
- single/multi ally support transfer + success impact accent.
- pure action/source regression contracts.

이번 pass에서 하지 않음:

- server gameplay entity 교체.
- heal/damage/status/target logic 변경.
- Witch 전용 새 재화/상태/skill 추가.
- 외부 model/texture binary import.
- particle cloud/glow spam.
- screenshot 없이 production visual PASS 선언.

## 5. Screenshot gate

통합 playtest 때 확인:

1. Character Detail idle만 보고 stock Witch와 다른 production posture가 읽히는가.
2. hat + satchel이 작은 GUI scale에서 형태를 망치지 않는가.
3. `Weakening Brew`가 공격 준비로, `Restorative Draught`가 아군 지원으로 서로 혼동 없이 읽히는가.
4. `Cauldron Overflow` 3-target transfer가 HUD/HP/Intent를 가리지 않는가.
5. potion transfer가 2D UI icon처럼 떠 보이지 않고 action travel로 읽히는가.
6. recipient SUCCESS accent가 기존 target marker/EXPOSED state와 충돌하지 않는가.
7. visual target과 server-authored target이 일치하는가.

실제 screenshot 확인 전 상태는 **PLAYTESTED = NO / PRODUCTION VISUAL PASS = NO**다.

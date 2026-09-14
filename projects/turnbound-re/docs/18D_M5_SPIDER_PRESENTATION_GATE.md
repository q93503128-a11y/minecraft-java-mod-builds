# 18D — SPIDER CONTROLLER / STRIKER PRESENTATION GATE

최종 갱신: 2026-09-14  
상태: **REFERENCE GATE PASS / CODE REVIEWED / AUTOMATED TEST NOT RUN / SCREENSHOT PASS PENDING**

이 문서는 `18_CHARACTER_VFX_REFERENCE_GATE.md`를 대체하지 않는다. Spider의 외형과 행동 언어를 임의 디자인하지 않고, Minecraft/Fresh Animations의 외부 시각 언어를 TURNBOUND의 Fang / Venom / Web / Pounce 규칙에 연결하기 위한 좁은 presentation gate다.

## 1. Canonical gameplay identity

현재 production data의 Spider는:

- character: `turnbound_re:spider`
- gameplay source: `minecraft:spider`
- origin: ★2
- roles: `CONTROLLER / STRIKER`
- SPD 34 — 대표 8종 중 빠른 축
- `spider_fang`: enemy single MELEE
- `spider_venom_bite`: enemy single MELEE + `venom`
- `spider_binding_web`: enemy single PROJECTILE + `webbed` + Intent Delay
- `spider_brood_pounce`: enemy single Burst MELEE + `venom`

따라서 네 행동이 모두 같은 바닐라 Spider 돌진으로 보여서는 안 된다. 특히 `Binding Web`이 공용 화살 projectile로 보이는 것은 action identity와 직접 충돌한다.

## 2. External reference screening

### SPIDER-R01 — Fresh Animations: Spiders

- source: https://modrinth.com/resourcepack/fresh-animations-spiders
- classification: **REFERENCE ONLY / ARR + custom terms**
- current compatibility: Minecraft Java 26.2 포함
- 공개된 design description:
  - wider heads
  - pedipalps
  - larger mouth parts
  - 2 segment legs
  - idle / pitch-yaw / aggressive / attack animation
- TURNBOUND 채택:
  - 낮고 넓은 Minecraft Spider silhouette 유지.
  - 머리 폭, mouth-part 가독성, pedipalp, 2절 다리 구조를 presentation model의 형태 언어로 삼는다.
  - 공격 종류는 head / pedipalp / abdomen / eight-leg brace의 조합으로 먼저 읽히게 한다.
- 사용 상태:
  - Fresh Animations `.jem` / `.jpm` / texture / animation 파일은 repository에 포함하지 않는다.
  - 공개 설명과 gallery를 reference로만 사용한다.

### SPIDER-R02 — Mojang String / Spider identity

- source: https://www.minecraft.net/en-us/article/taking-inventory--string
- classification: **REFERENCE ONLY / proprietary first-party**
- 관찰:
  - Minecraft Spider와 string/web의 연결은 기존 Minecraft 정체성 안에 이미 존재한다.
- TURNBOUND 채택:
  - `Binding Web`은 새 마법 projectile 디자인을 만들지 않고 Minecraft `COBWEB` runtime item language를 그대로 활용한다.

## 3. Selected presentation language

### Base silhouette / idle

- Minecraft Spider의 낮고 넓은 body/abdomen silhouette 유지.
- Fresh Animations reference처럼 head를 읽기 쉽게 넓히고 pedipalp / mouth-part / 2절 다리를 추가한다.
- idle은 작은 head tracking + 느린 alternating leg weight shift.
- 임의 장식, 갑옷, glow stripe, 카드식 VFX를 추가하지 않는다.

### Fang — OFFENSIVE

- exact action id + MELEE + enemy target에서만 사용.
- head-led snap + 앞다리 reach.
- Venom Bite보다 얕고 빠른 기본 공격 silhouette.

### Venom Bite — VENOM

- exact action id + MELEE + enemy target에서만 사용.
- head를 더 낮추고 mouth part / pedipalp를 더 크게 열어 committed bite로 읽힌다.
- 독 효과 자체를 녹색 particle spam으로 표현하지 않는다. 실제 `venom` status는 server battle state가 결정한다.

### Binding Web — WEB

- exact action id + PROJECTILE + enemy target에서만 사용.
- abdomen lift + front-half anchor.
- travelling projectile은 공용 `ARROW` 대신 Minecraft `COBWEB` item을 사용한다.
- sound도 `ARROW_SHOOT` 대신 가벼운 vanilla throw sound를 사용한다.
- 실제 `webbed`와 Intent Delay는 server event가 결정한다.

### Brood Pounce — POUNCE

- exact Burst action id + MELEE + enemy target에서만 사용.
- 여덟 다리를 몸 아래로 더 깊게 coil하고 stage의 existing close-motion과 결합한다.
- 일반 Fang/Venom과 다른 Burst 준비 silhouette를 확보한다.
- particle 양으로 Burst를 구분하지 않는다.

### Fail-closed

- RECOVERY는 neutral.
- 다른 actor의 행동은 neutral.
- unknown MELEE / PROJECTILE action은 neutral.
- Binding Web이 PROJECTILE 계약을 벗어나면 web pose/cobweb projectile/special throw sound를 사용하지 않는다.
- character/source가 `turnbound_re:spider` + `minecraft:spider` 정확히 일치하지 않으면 visual override를 적용하지 않는다.

## 4. Implemented boundary

이번 pass:

- `turnbound_re:spider_visual` presentation-only entity.
- Character Detail / virtual Battle Stage shared Spider identity.
- Minecraft runtime Spider texture.
- external-reference 기반 wider head / pedipalp / larger mouth area / two-segment eight-leg geometry.
- OFFENSIVE / VENOM / WEB / POUNCE exact action pose families.
- Binding Web `COBWEB` projectile presentation + non-arrow throw sound.
- exact action/actor/target/recovery fail-closed contracts.
- source override + action pose regression tests 작성.

이번 pass에서 하지 않음:

- gameplay `minecraft:spider` 교체.
- damage / venom / webbed / Intent Delay / targeting rule 변경.
- Fresh Animations binary/model/texture/animation asset import.
- 임의 particle/glow 중심 효과 추가.

## 5. Validation state

사용자 요청에 따라 이번 content pass에서는 build/CI를 돌리지 않는다.

- CODE REVIEWED: **YES**
- TESTS AUTHORED: **YES**
- TESTED: **NO**
- BUILD VERIFIED: **NO**
- JAR PRODUCED: **NO**
- PLAYTESTED: **NO**
- MULTIPLAYER TESTED: **NO**
- PRODUCTION VISUAL PASS: **NO**

## 6. Bundled representative-roster playtest gate

Spider까지 반영되면 대표 8종 presentation 구현 공백이 닫힌다. 다음 수동 테스트에서는 Creeper와 Spider만 따로 보는 대신 대표 roster를 한 번에 확인한다.

Spider 확인 항목:

1. 낮고 넓은 silhouette와 2절 다리가 작은 Battle Stage 크기에서도 읽히는가.
2. Fang과 Venom Bite가 같은 모션처럼 보이지 않는가.
3. Binding Web에서 화살 대신 cobweb이 이동하는가.
4. Binding Web에 arrow-shoot sound가 나지 않는가.
5. Brood Pounce가 기본 bite보다 깊은 coil/Burst로 보이는가.
6. 공격 visual과 실제 authoritative target이 일치하는가.

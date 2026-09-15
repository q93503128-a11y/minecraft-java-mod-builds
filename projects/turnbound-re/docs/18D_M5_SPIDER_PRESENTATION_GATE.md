# 18D — SPIDER CONTROLLER / STRIKER PRESENTATION GATE

최종 갱신: 2026-09-15  
상태: **EXTERNAL-ONLY BASE CORRECTED / CODE REVIEW PENDING COMMIT / AUTOMATED TEST NOT RUN / SCREENSHOT PASS PENDING**

이 문서는 `18_CHARACTER_VFX_REFERENCE_GATE.md`를 대체하지 않는다. Spider의 외형을 레퍼런스에서 수동 재디자인하지 않고 실제 외부 production design을 직접 사용하면서 Fang / Venom / Web / Pounce 전투 규칙만 연결하기 위한 좁은 presentation gate다.

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

특히 `Binding Web`은 공용 화살 projectile로 보여서는 안 되며, action identity는 server-authored target/status와 일치해야 한다.

## 2. External production base

### SPIDER-P01 — Mojang runtime Spider model / texture

- source: Minecraft Java 26.2 runtime
- code binding: `ModelLayers.SPIDER`
- texture binding: `minecraft:textures/entity/spider/spider.png`
- classification: **DIRECT RUNTIME USE / first-party proprietary content already supplied by Minecraft**
- 현재 사용: **YES**
- 원칙:
  - TURNBOUND가 wider head, pedipalp, two-segment leg 같은 replacement geometry를 새로 만들지 않는다.
  - vanilla Spider model을 runtime에서 직접 bake하고 사용한다.
  - TURNBOUND는 existing head/body/abdomen/eight-leg parts의 pose offset과 stage/projectile timing만 연결한다.

이 correction으로 과거 Fresh Animations 설명을 보고 수동으로 재구성했던 head/pedipalp/two-segment-leg geometry를 제거한다.

### SPIDER-C01 — Scary Spider

- source: https://modrinth.com/resourcepack/scary-spider
- pinned candidate version: `p54uoJxL` — `Scary Spider 1.20.2+.zip`
- license: MIT
- classification: **DIRECT-ASSET CANDIDATE / NOT IMPORTED**
- 공개 호환: Minecraft 1.20.2–1.21.8
- 판단:
  - external Spider model을 실제 asset으로 직접 쓰는 후보.
  - 26.2에서 format/EMF compatibility를 실제 검증하기 전 production 반입하지 않는다.
  - gallery/model을 보고 TURNBOUND geometry로 다시 만드는 것은 금지한다.

### SPIDER-C02 — Wall Climbers 1.2

- source: https://modrinth.com/resourcepack/wall-climbers/version/1.2
- license: MIT + 프로젝트 공개 사용조건 확인
- classification: **DIRECT-ASSET / ANIMATION CANDIDATE / NOT IMPORTED**
- 공개 호환: Minecraft Java 26.2 포함
- 용도 후보: Spider/Cave Spider의 leg-based wall/roof presentation 및 compatible CEM part attachment.
- 실제 파일/고지/호환을 고정한 뒤에만 반입한다. 다른 pack을 무단 복사하는 식으로 합치지 않는다.

### SPIDER-R01 — Fresh Animations: Spiders

- source: https://modrinth.com/resourcepack/fresh-animations-spiders
- classification: **REFERENCE ONLY / ARR + custom terms**
- 과거 공개 description에서 wider head / pedipalps / larger mouth parts / two-segment legs를 참고했으나, 새 external-only 규칙에서는 그것을 바탕으로 새 geometry를 만드는 행위도 금지한다.
- 원본 `.jem` / `.jpm` / texture / animation 파일은 repository에 포함하지 않는다.

### SPIDER-R02 — Mojang String / Spider identity

- source: https://www.minecraft.net/en-us/article/taking-inventory--string
- classification: **REFERENCE ONLY / proprietary first-party**
- `Binding Web`은 새 마법 projectile 디자인 대신 Minecraft `COBWEB` runtime item language를 사용한다.

## 3. Selected presentation language

### Base silhouette / idle

- Mojang `ModelLayers.SPIDER`와 runtime Spider texture를 그대로 사용한다.
- extra mouth part / pedipalp / segmented leg / armor / glow stripe를 TURNBOUND가 자체 제작하지 않는다.
- vanilla model animation을 먼저 적용하고 action-specific pose offset만 더한다.

### Fang — OFFENSIVE

- exact action id + MELEE + enemy target에서만 사용.
- existing head/body/front legs의 rotation만 조정해 빠른 bite를 읽힌다.

### Venom Bite — VENOM

- exact action id + MELEE + enemy target에서만 사용.
- existing head/body/front/middle-front leg pose로 더 낮고 committed한 bite를 구분한다.
- 독 효과를 자체 녹색 particle 디자인으로 대체하지 않는다. 실제 `venom`은 server battle state가 결정한다.

### Binding Web — WEB

- exact action id + PROJECTILE + enemy target에서만 사용.
- existing abdomen/body/front/hind leg pose만 조정한다.
- travelling projectile은 공용 `ARROW` 대신 Minecraft `COBWEB` item을 사용한다.
- sound도 `ARROW_SHOOT` 대신 vanilla throw sound를 사용한다.
- 실제 `webbed`와 Intent Delay는 server event가 결정한다.

### Brood Pounce — POUNCE

- exact Burst action id + MELEE + enemy target에서만 사용.
- Mojang model의 여덟 다리 rotation을 깊게 coil하고 existing close-motion과 결합한다.
- 새 silhouette/extra geometry로 Burst를 디자인하지 않는다.

### Fail-closed

- RECOVERY는 neutral.
- 다른 actor의 행동은 neutral.
- unknown MELEE / PROJECTILE action은 neutral.
- Binding Web이 PROJECTILE 계약을 벗어나면 web pose/cobweb projectile/special throw sound를 사용하지 않는다.
- character/source가 `turnbound_re:spider` + `minecraft:spider` 정확히 일치하지 않으면 visual override를 적용하지 않는다.

## 4. Corrected implementation boundary

이번 correction:

- hand-authored Spider replacement geometry 제거.
- wider head / pedipalp / two-segment eight-leg 자체 구현 제거.
- custom TURNBOUND model layer 제거.
- `ModelLayers.SPIDER` direct runtime base 사용.
- Minecraft runtime Spider texture 직접 사용.
- OFFENSIVE / VENOM / WEB / POUNCE는 existing vanilla part pose offset으로 한정.
- Binding Web `COBWEB` projectile presentation + non-arrow throw sound 유지.
- gameplay damage / venom / webbed / Intent Delay / targeting rule 변경 없음.

현재 하지 않음:

- Scary Spider / Wall Climbers asset import.
- Fresh Animations 파일 import 또는 디자인 재구성.
- gameplay `minecraft:spider` 교체.
- 임의 particle/glow/extra anatomy 추가.

## 5. Validation state

사용자 요청에 따라 이번 content correction에서는 build/CI를 돌리지 않는다.

- CODE REVIEWED: **PENDING FINAL DIFF REVIEW**
- TESTS AUTHORED: **기존 regression test 유지**
- TESTED: **NO**
- BUILD VERIFIED: **NO**
- JAR PRODUCED: **NO**
- PLAYTESTED: **NO**
- MULTIPLAYER TESTED: **NO**
- PRODUCTION VISUAL PASS: **NO**

## 6. Bundled representative-roster playtest gate

1. actual Mojang Spider silhouette/texture가 Character Detail과 Battle Stage에서 동일하게 보이는가.
2. Fang과 Venom Bite가 같은 pose처럼 보이지 않는가.
3. Binding Web에서 화살 대신 cobweb이 이동하는가.
4. Binding Web에 arrow-shoot sound가 나지 않는가.
5. Brood Pounce가 기본 bite보다 깊은 coil/Burst로 읽히는가.
6. 새 자체 Spider geometry가 남아 있지 않은가.
7. 공격 visual과 실제 authoritative target이 일치하는가.

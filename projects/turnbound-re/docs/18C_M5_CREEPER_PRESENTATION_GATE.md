# 18C — CREEPER BREAKER / STRIKER PRESENTATION GATE

최종 갱신: 2026-09-15  
상태: **EXTERNAL-ONLY BASE CORRECTED / CODE REVIEW PENDING COMMIT / AUTOMATED TEST NOT RUN / SCREENSHOT PASS PENDING**

이 문서는 `18_CHARACTER_VFX_REFERENCE_GATE.md`를 대체하지 않는다. Creeper를 임의의 신규 몬스터처럼 다시 디자인하지 않고, 실제 외부 production design을 직접 사용하면서 TURNBOUND 전투 규칙만 연결하기 위한 좁은 presentation gate다.

## 1. Canonical gameplay identity

현재 production data의 Creeper는:

- character: `turnbound_re:creeper`
- gameplay source: `minecraft:creeper`
- origin: ★3
- roles: `BREAKER / STRIKER`
- `creeper_fuse_bash`: enemy single MELEE
- `creeper_volatile_charge`: SELF BLAST tag, damage 없음, `atk_up` + `volatile`
- `creeper_blast_wave`: enemy multi BLAST, 높은 Poise pressure
- `creeper_catastrophe`: enemy multi Burst BLAST, 매우 높은 HP/Poise pressure

가장 중요한 계약은 `Volatile Charge`를 폭발 공격처럼 표현하지 않는 것이다. 이 행동은 실제 피해를 주지 않는 자기 강화이며, 이후 authoritative `volatile` status가 유지되는 동안 시각적으로 긴장 상태를 남길 수 있다.

## 2. External production base

### CREEPER-P01 — Mojang runtime Creeper model / texture

- source: Minecraft Java 26.2 runtime
- code binding: `ModelLayers.CREEPER`
- texture binding: `minecraft:textures/entity/creeper/creeper.png`
- classification: **DIRECT RUNTIME USE / first-party proprietary content already supplied by Minecraft**
- 현재 사용: **YES**
- 원칙:
  - TURNBOUND가 Creeper replacement geometry, UV, texture를 새로 만들지 않는다.
  - vanilla model을 runtime에서 직접 bake하고 사용한다.
  - TURNBOUND는 서버 행동을 읽게 하는 pose offset / stage motion / sound timing만 연결한다.

이 방식은 과거의 수동 `CubeListBuilder` replacement geometry를 제거한다. 외부 디자인을 참고해 비슷하게 다시 만드는 대신 실제 Mojang model design 자체를 쓴다.

### CREEPER-C01 — Moth's Creeper Redone

- source: https://modrinth.com/resourcepack/moths-creeper-redone
- license: MIT
- classification: **DIRECT-ASSET CANDIDATE / NOT IMPORTED**
- 공개 상태:
  - custom Creeper model + texture
  - 공개 호환: Minecraft 1.21.1
  - OptiFine 또는 EMF + ETF 필요
- 판단:
  - 실제 파일을 직접 쓰는 upgrade 후보로는 적합하다.
  - 26.2에서 asset format / EMF path를 실제 검증하기 전 production 반입하지 않는다.
  - gallery를 보고 TURNBOUND Java geometry로 재현하는 것은 금지한다.

### CREEPER-R01 — Mojang official Creeper identity material

- source: https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/software/Minecraft-Monstrous-Compendium-revised.pdf
- classification: **REFERENCE ONLY / proprietary first-party**
- 용도: canonical behavior/identity 확인만. 별도 asset 복사 없음.

### CREEPER-R02 — Fresh Animations: Creepers

- source: https://modrinth.com/resourcepack/fresh-animations-creepers
- classification: **REFERENCE ONLY / ARR + custom terms**
- 사용 상태: 원본 texture/model/animation asset 미반입.
- 새 규칙: 원본 디자인을 보고 비슷한 replacement geometry를 수동 제작하지 않는다.

## 3. Selected presentation language

### Base silhouette / idle

- Mojang `ModelLayers.CREEPER`와 runtime Creeper texture를 그대로 사용한다.
- 별도 장식, 갑옷, 뿔, 임의 glow ornament, 자체 silhouette를 추가하지 않는다.
- vanilla model animation을 먼저 적용한 뒤 action-specific pose offset만 더한다.

### Fuse Bash — OFFENSIVE

- exact action id + MELEE + enemy target에서만 사용.
- 기존 외부 모델의 몸/머리/다리 회전만 이용해 짧은 body shove를 읽힌다.
- fuse compression을 사용하지 않아 `Volatile Charge`와 혼동되지 않는다.

### Volatile Charge — CHARGE

- exact action id + BLAST tag + self-only target에서만 사용.
- non-aggressive.
- existing Mojang model parts의 brace/tension만 조정한다.
- `CREEPER_PRIMED` 계열 sound를 WINDUP에서 사용한다.
- generic explosion sound를 사용하지 않는다.
- server snapshot에 `turnbound_re:volatile` status가 남아 있는 동안 restrained tension도 유지한다.

### Blast Wave — BLAST

- exact action id + enemy targets에서만 사용.
- 기존 model pose만 더 낮고 대칭적으로 조정한다.
- authoritative AREA target accent와 실제 target list를 그대로 사용한다.
- impact에서 실제 BLAST sound가 난다.

### Catastrophe — CATASTROPHE

- exact Burst action id + enemy targets에서만 사용.
- Blast Wave보다 더 깊은 existing-part compression을 사용한다.
- 자체 geometry/VFX ornament로 Burst를 재디자인하지 않는다.
- 실제 damage/Poise/target은 server event를 그대로 따른다.

### Fail-closed

- RECOVERY는 neutral.
- 다른 actor의 행동은 neutral.
- unknown BLAST action은 neutral.
- Volatile Charge가 self-only target 계약을 벗어나면 neutral.
- character/source가 `turnbound_re:creeper` + `minecraft:creeper` 정확히 일치하지 않으면 visual override를 적용하지 않는다.

## 4. Corrected implementation boundary

이번 correction:

- hand-authored Creeper `CubeListBuilder` geometry 제거.
- custom TURNBOUND model layer 제거.
- `ModelLayers.CREEPER` direct runtime base 사용.
- Minecraft runtime Creeper texture 직접 사용.
- OFFENSIVE / CHARGE / BLAST / CATASTROPHE는 geometry redesign이 아니라 existing model-part pose offset으로 한정.
- server-published `volatile` status와 primed/blast sound 구분 유지.
- gameplay source / damage / status / target / turn rule은 변경하지 않음.

현재 하지 않음:

- Moth's Creeper Redone asset import.
- Fresh Animations 파일 import 또는 디자인 재구성.
- gameplay `minecraft:creeper` 교체.
- particle spam / 임의 장식 추가.

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

## 6. Later screenshot / playtest gate

1. 실제 Mojang Creeper silhouette/texture가 Character Detail과 Battle Stage에서 동일하게 보이는가.
2. Fuse Bash가 charge/explosion cast로 오해되지 않는가.
3. Volatile Charge에서 폭발음 대신 fuse priming이 들리는가.
4. `volatile` status가 남은 동안 tension이 실제 status와 같이 유지/해제되는가.
5. Blast Wave와 Catastrophe가 같은 pose로 보이지 않는가.
6. 새 자체 geometry가 남아 있지 않은가.
7. AREA accent의 대상과 server-authored target list가 일치하는가.

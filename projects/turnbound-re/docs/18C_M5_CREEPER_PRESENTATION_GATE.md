# 18C — CREEPER BREAKER / STRIKER PRESENTATION GATE

최종 갱신: 2026-09-14  
상태: **REFERENCE GATE PASS / CODE REVIEWED / AUTOMATED TEST NOT RUN / SCREENSHOT PASS PENDING**

이 문서는 `18_CHARACTER_VFX_REFERENCE_GATE.md`를 대체하지 않는다. Creeper를 임의의 신규 몬스터처럼 다시 디자인하지 않고, 외부 정본의 Creeper 정체성을 TURNBOUND 전투 규칙에 맞게 읽히도록 유지하기 위한 좁은 presentation gate다.

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

## 2. External reference screening

### CREEPER-R01 — Mojang official Creeper identity

- source: https://www.minecraft.net/content/dam/minecraftnet/games/minecraft/software/Minecraft-Monstrous-Compendium-revised.pdf
- classification: **REFERENCE ONLY / proprietary first-party**
- 관찰:
  - 초록색, 팔이 없고 네 개의 짧은 다리를 가진 canonical silhouette.
  - 목표 근처에서 멈춘 뒤 burning fuse 같은 hiss가 시작되고, 이후 폭발한다.
  - charged Creeper는 푸른 전기성 aura로 더 강한 위험을 읽힌다.
- TURNBOUND 채택:
  - 몸 비율과 texture language는 vanilla Creeper를 유지한다.
  - 공격 전 긴장/압축/brace가 행동 종류를 먼저 알려 주도록 한다.
  - charge 계열은 폭발음 대신 fuse priming sound를 사용한다.
- 금지:
  - TURNBOUND gameplay에 없는 자폭/사망을 임의 추가하지 않는다.
  - charged Creeper의 실제 lightning mechanic을 새 combat rule로 추가하지 않는다.

### CREEPER-R02 — Fresh Animations: Creepers

- source: https://modrinth.com/resourcepack/fresh-animations-creepers
- classification: **REFERENCE ONLY / ARR + custom terms**
- 관찰:
  - vanilla-like Creeper identity를 유지하면서 custom texture/model variation과 charged look을 강화한다.
  - 현재 Minecraft Java 26.2 호환 버전이 존재한다.
- TURNBOUND 채택:
  - 원형 실루엣을 갈아엎기보다 idle/brace/fuse motion을 강화하는 방향.
  - charged/volatile 상태는 자세와 지속 tension으로 구분한다.
- 사용 상태:
  - Fresh Animations 원본 texture/model/animation asset은 repository에 포함하지 않는다.

## 3. Selected presentation language

### Base silhouette / idle

- Mojang Creeper의 머리/몸/네 다리 비율과 runtime Creeper texture를 유지한다.
- idle은 빠른 bounce가 아니라 작은 head scan과 네 다리 weight shift만 사용한다.
- 외부 캐릭터 디자인을 덧붙이기 위해 장식, 갑옷, 뿔, glow ornament 등을 임의 추가하지 않는다.

### Fuse Bash — OFFENSIVE

- exact action id + MELEE + enemy target에서만 사용.
- 몸통이 먼저 앞으로 밀리는 body bash.
- fuse compression을 사용하지 않아 `Volatile Charge`와 혼동되지 않는다.

### Volatile Charge — CHARGE

- exact action id + BLAST tag + self-only target에서만 사용.
- non-aggressive.
- 네 다리를 벌려 고정하고 몸/머리가 짧고 빠르게 긴장한다.
- `CREEPER_PRIMED` 계열 sound를 WINDUP에서 사용한다.
- generic explosion sound를 사용하지 않는다.
- server snapshot에 `turnbound_re:volatile` status가 남아 있는 동안 presentation entity의 restrained fuse tension도 유지한다.

### Blast Wave — BLAST

- exact action id + enemy targets에서만 사용.
- 일반 charge보다 낮고 대칭적인 brace.
- 기존 authoritative AREA target accent와 실제 target list를 그대로 사용한다.
- impact에서 실제 BLAST sound가 난다.

### Catastrophe — CATASTROPHE

- exact Burst action id + enemy targets에서만 사용.
- Blast Wave보다 더 깊은 body/head compression과 넓은 four-leg brace를 사용한다.
- glow/particle 양으로 Burst를 구분하지 않는다.
- 실제 damage/Poise/target은 server event를 그대로 따른다.

### Fail-closed

- RECOVERY는 neutral.
- 다른 actor의 행동은 neutral.
- unknown BLAST action은 neutral.
- Volatile Charge가 self-only target 계약을 벗어나면 neutral.
- character/source가 `turnbound_re:creeper` + `minecraft:creeper` 정확히 일치하지 않으면 visual override를 적용하지 않는다.

## 4. Implemented boundary

이번 pass:

- `turnbound_re:creeper_visual` presentation-only entity.
- Character Detail / virtual Battle Stage shared Creeper identity.
- Mojang canonical Creeper silhouette + Minecraft runtime Creeper texture.
- OFFENSIVE / CHARGE / BLAST / CATASTROPHE action pose families.
- server-published `volatile` status에 연결된 지속 fuse tension.
- Volatile Charge 전용 primed hiss와 실제 BLAST impact sound 분리.
- exact action/actor/target/recovery fail-closed contracts.
- source override + action pose regression tests 작성.

이번 pass에서 하지 않음:

- gameplay `minecraft:creeper` 교체.
- damage / status / target / turn rule 변경.
- 외부 binary model/texture/animation import.
- charged blue aura를 새 gameplay mechanic으로 추가.
- particle spam, giant overlay, 임의 장식으로 캐릭터를 재설계.

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

## 6. Later screenshot / playtest gate

Spider까지 대표 8종 presentation을 묶은 뒤 한 번에 확인하는 것을 우선한다.

확인 항목:

1. 기본 idle에서 vanilla Creeper 정체성이 즉시 읽히는가.
2. Fuse Bash가 fuse/explosion cast로 오해되지 않는가.
3. Volatile Charge에서 폭발음 대신 fuse priming이 들리는가.
4. `volatile` status가 남은 동안 긴장 상태가 실제 status와 같이 유지/해제되는가.
5. Blast Wave와 Catastrophe가 같은 pose로 보이지 않는가.
6. Catastrophe가 particle/glow 남발 없이 Burst로 읽히는가.
7. AREA accent의 대상과 server-authored target list가 일치하는가.

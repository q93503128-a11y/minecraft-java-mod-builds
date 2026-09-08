# EARTH TO STARS — Third-Party Assets & Reference Ledger

이 문서는 외부 코드, 모드, 모델, UI, 사운드, 게임 레퍼런스를 추적한다.

**중요:** 이 목록에 있다고 해서 프로젝트에 직접 포함해도 된다는 뜻이 아니다. `REFERENCE`, `CODE RESEARCH`, `BASE CANDIDATE`, `DIRECT USE CANDIDATE`를 구분한다. 실제 파일을 repository에 넣기 전에는 라이선스와 현재 배포 조건을 다시 확인한다.

현재 EARTH TO STARS 저장소에는 아래 외부 자산 파일을 직접 복사해 넣지 않았다.

검토 기준일: 2026-09-08

---

## 1. VS Genesis

- Type: `REFERENCE / CODE RESEARCH`
- Project: VS Genesis
- Source: CurseForge / public source repository as linked by project
- URL: https://www.curseforge.com/minecraft/mc-mods/vs-genesis
- License observed during research: Apache License 2.0
- Minecraft generation observed: 1.20.1 Forge-era project
- Why useful:
  - ship/player transition into space layer
  - space scale compression
  - celestial body representation
  - seamless-feeling dimension transition research
- Current project policy:
  - do not make it a runtime dependency automatically
  - inspect architecture/patterns where license permits
  - do not copy assets without per-file provenance review
- Imported files: none

---

## 2. Create Cosmonautics

- Type: `REFERENCE / CODE RESEARCH`
- Project: Create Cosmonautics
- Source: GitHub / Modrinth
- URL: https://github.com/CosmonauticsTeam/Create-Cosmonautics
- License observed during research: GPL-3.0
- Minecraft generation observed during research: 1.21.1 NeoForge
- Why useful:
  - thrust/mass/spaceflight problem solving
  - orbit/travel presentation
  - spacecraft systems
  - re-entry/atmosphere ideas
  - asteroid/resource gameplay
- Current project policy:
  - research implementation patterns
  - not an approved 26.2 runtime dependency
  - GPL code incorporation would require deliberate compatibility/license decision; do not casually paste code into this ARR project
- Imported files: none

---

## 3. Robotica

- Type: `REFERENCE / CODE RESEARCH`
- Project: Robotica
- Source: Modrinth / linked public repository
- URL: https://modrinth.com/mod/robotica
- License observed during research: Apache License 2.0
- Minecraft compatibility observed during research: includes Minecraft 26.2 / NeoForge
- Why useful:
  - current-generation Minecraft robot/mecha implementation
  - SF weapons/upgrades
  - entity and gameplay structure reference
- Current project policy:
  - especially valuable for 26.2 API/reference checks
  - code reuse only after exact file/license/API fit review
  - visual assets require separate provenance check
- Imported files: none

---

## 4. Kenney — Space Station Kit

- Type: `BASE CANDIDATE / DIRECT USE CANDIDATE`
- Source: Kenney
- URL: https://kenney.nl/assets/space-station-kit
- License observed during research: CC0
- Package size observed during research: about 90 3D models
- Why useful:
  - station/interior forms
  - doors/walls/equipment
  - Blockbench/Minecraft reinterpretation base
- Required adaptation:
  - Minecraft scale
  - texel/style consistency
  - collision simplification
  - avoid direct style mismatch with other asset sources
- Imported files: none

---

## 5. Kenney — Space Kit

- Type: `BASE CANDIDATE / DIRECT USE CANDIDATE`
- Source: Kenney
- URL: https://kenney.nl/assets/space-kit
- License observed during research: CC0
- Package size observed during research: about 150 3D models
- Why useful:
  - spacecraft/space props
  - concept and silhouette base
- Required adaptation:
  - do not use as-is if visual language clashes with Minecraft or final ship system
  - rework scale/material/texture language
- Imported files: none

---

## 6. Kenney — Modular Space Kit

- Type: `BASE CANDIDATE / DIRECT USE CANDIDATE`
- Source: Kenney
- URL: https://kenney.nl/assets/modular-space-kit
- License observed during research: CC0
- Package size observed during research: about 40 models
- Why useful:
  - modular spacecraft/station form study
  - potential prototype/base pieces for module language
- Imported files: none

---

## 7. Kenney — UI Pack: Sci-Fi

- Type: `UI BASE CANDIDATE / REFERENCE`
- Source: Kenney
- URL: https://kenney.nl/assets/ui-pack-sci-fi
- License observed during research: CC0
- Package size observed during research: about 130 UI files
- Why useful:
  - buttons/panels/widgets as editable base
  - production consistency faster than AI-improvised rectangles
- Important restriction from project quality gate:
  - CC0 does not mean “paste every asset unchanged”
  - establish project design tokens, nine-slice, typography and information hierarchy first
  - adapt to Minecraft GUI scale and Korean/English text stress
- Imported files: none

---

## 8. Kenney — Sci-Fi Sounds / Interface Sounds

- Type: `AUDIO BASE CANDIDATE`
- Source: Kenney
- URLs:
  - https://kenney.nl/assets/sci-fi-sounds
  - https://kenney.nl/assets/interface-sounds
- License observed during research: CC0
- Why useful:
  - machinery/electronic/UI feedback base
  - alarms/interaction prototyping
- Required adaptation:
  - volume normalization
  - EQ/layering
  - distance behavior
  - repetition variation
  - project-specific sound language
- Imported files: none

---

## 9. Space Engineers

- Type: `DESIGN REFERENCE ONLY`
- Source: Keen Software House commercial game
- Why useful:
  - modular ship readability
  - ship power/logistics concepts
  - manual/automatic turret interaction
  - weapon arcs and ship layout consequences
  - solo vs crew operation reference
- License/reuse policy:
  - proprietary commercial game
  - do not copy code, models, textures, UI or audio
  - use only as gameplay/design reference
- Imported files: none

---

## 10. Other Minecraft Space Mods

The following categories may be researched for problem-solving, but are not approved dependencies/assets by this document:

- Ad Astra / Galacticraft lineage — planet progression, life support, travel UX reference
- Advanced Rocketry — stations, satellites, asteroid/resource progression reference
- Mekanism / Oritech — large technical-system structure and machinery presentation reference
- other seamless-space experiments — transition/presentation research

Before any code or asset reuse:

1. identify exact repository/file
2. verify current license
3. verify version
4. verify redistribution terms
5. record author/source
6. decide whether it is reference, code reuse, editable base or direct-use asset

---

# 11. Asset Import Procedure

외부 파일을 실제로 추가할 때 이 표를 채운다.

| ID | Asset | Author | Source URL | License | Use class | Modifications | Repository paths | Verified date |
|---|---|---|---|---|---|---|---|---|
| — | — | — | — | — | — | — | — | — |

Use class values:

- `REFERENCE_ONLY`
- `CODE_RESEARCH`
- `EDITABLE_BASE`
- `DIRECT_USE`

---

# 12. Rejection Rules

다음 자산은 repository에 넣지 않는다.

- license 불명확
- source/author 추적 불가
- paid asset 무단 복사
- 다른 모드 JAR에서 추출한 자산인데 재사용 허가 없음
- ripped commercial-game assets
- Minecraft 원본 배포 파일
- 출처 기록 없이 임시로 받은 모델/사운드

개인 플레이 목적이라도 이 GitHub 저장소는 공개 빌드 허브이므로 예외로 보지 않는다.

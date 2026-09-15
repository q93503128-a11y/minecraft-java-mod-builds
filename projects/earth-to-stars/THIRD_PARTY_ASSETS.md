# EARTH TO STARS — Third-Party Assets & Reference Ledger

이 문서는 외부 코드, 모드, 모델, UI, 사운드, 게임 레퍼런스를 추적한다.

**중요:** 목록에 있다는 이유만으로 직접 포함 가능한 것은 아니다. `REFERENCE_ONLY`, `CODE_RESEARCH`, `EDITABLE_BASE`, `DIRECT_USE`를 구분하고, 실제 파일/코드를 repository에 넣기 전 source / author / exact license / modification / redistribution 조건을 다시 확인한다.

현재 제품 정책은 **외부 우주/함선 모드를 플레이어에게 통째로 설치시키지 않고**, 허용되는 코드·알고리즘·리소스를 선별적으로 ETS에 포팅/통합하는 것이다. 런타임 라이브러리 의존성은 별도 승인 없이는 늘리지 않는다.

검토 기준일: 2026-09-15

---

## 1. FabricMC — fabric-example-mod 26.2

- Type: `DIRECT_USE / CODE TEMPLATE`
- Source: FabricMC GitHub
- URL: https://github.com/FabricMC/fabric-example-mod/tree/26.2
- License verified: CC0-1.0
- Exact reference branch: `26.2`
- Used for:
  - Fabric Loom project structure
  - Fabric Loader / Fabric API dependency contract
  - Java 25 compile target
  - `fabric.mod.json` dependency shape
- Current observed official 26.2 values at migration time:
  - Minecraft `26.2`
  - Fabric Loader `0.19.5`
  - Fabric API `0.160.0+26.2`
  - Java `25`
- ETS adaptation:
  - project/package/mod identifiers replaced
  - existing ETS loader-neutral kernel retained
  - repository's already-working Fabric project conventions reused where safer
  - no example gameplay/assets vendored
- Repository paths influenced:
  - `build.gradle`
  - `gradle.properties`
  - `settings.gradle`
  - `src/fabric/resources/fabric.mod.json`
- Verified date: 2026-09-15

---

## 2. VS Genesis

- Type: `REFERENCE_ONLY / CODE_RESEARCH`
- Project: VS Genesis
- Source: CurseForge / public source repository linked by project
- URL: https://www.curseforge.com/minecraft/mc-mods/vs-genesis
- License observed during research: Apache License 2.0
- Minecraft generation observed: 1.20.1 Forge-era project
- Useful for:
  - ship/player transition into a space layer
  - space scale compression
  - celestial body representation
  - seamless-feeling dimension transition research
- Current policy:
  - **not a runtime dependency**
  - exact permissively licensed implementation may be studied/ported only after file-level provenance review
  - assets are not copied without separate license/provenance verification
- Imported files: none

---

## 3. Valkyrien Skies / Zero Point ecosystem

- Type: `REFERENCE_ONLY / CODE_RESEARCH`
- Historical use: 1.20.1 Forge technical reboot used Valkyrien Skies + Genesis + ZPS + ZPL as a complete external runtime stack.
- Current policy:
  - that stack is no longer the ETS product architecture
  - retain lessons about collision, ship transforms, seating, propulsion separation and transactional deployment
  - do not require players to install the whole stack
  - any exact code reuse requires project/file/license review before import
- Imported source/assets into Fabric line: none

---

## 4. Create Cosmonautics

- Type: `REFERENCE_ONLY / CODE_RESEARCH`
- Source: GitHub / Modrinth
- URL: https://github.com/CosmonauticsTeam/Create-Cosmonautics
- License observed during research: GPL-3.0
- Minecraft generation observed during research: 1.21.1 NeoForge
- Useful for:
  - thrust/mass/spaceflight problem solving
  - orbit/travel presentation
  - spacecraft systems
  - re-entry/atmosphere ideas
  - asteroid/resource gameplay
- Policy:
  - architecture/algorithm research is welcome
  - GPL code is **not** casually pasted into this All-Rights-Reserved repository; incorporation requires an explicit project-license decision
- Imported files: none

---

## 5. Robotica

- Type: `REFERENCE_ONLY / CODE_RESEARCH`
- Source: Modrinth / linked public repository
- URL: https://modrinth.com/mod/robotica
- License observed during research: Apache License 2.0
- Minecraft compatibility observed during research: includes Minecraft 26.2 / NeoForge
- Useful for:
  - current-generation Minecraft entity/vehicle-like implementation reference
  - SF weapons/upgrades
  - modern API behavior comparison
- Policy:
  - useful code may be ported to Fabric only after exact source-file/license/API-fit review
  - visual assets require separate provenance review
- Imported files: none

---

## 6. Kenney — Space Station Kit

- Type: `EDITABLE_BASE / DIRECT_USE_CANDIDATE`
- Source: Kenney
- URL: https://kenney.nl/assets/space-station-kit
- License observed: CC0
- Useful for station/interior forms, doors/walls/equipment and Blockbench/Minecraft reinterpretation.
- Required adaptation: Minecraft scale, texel/style consistency, collision simplification, project-wide art-language fit.
- Imported files: none

---

## 7. Kenney — Space Kit

- Type: `DIRECT_USE / EDITABLE_BASE`
- Source / license authority: Kenney
- Official URL: https://kenney.nl/assets/space-kit
- License verified: CC0 1.0
- Current use:
  - upstream `craft_speederA` mesh → repository-safe `craft_speedera.obj` — starter launch craft base
  - `craft_miner` — orbital salvage base
  - `craft_racer` — first interceptor base
- Vendoring provenance:
  - source bytes obtained from pinned public mirror `melonjs/melonJS@5ab23f5a7be8fe4a1fc7abbe225428106e8a833c`
  - OBJ/MTL identify `Created by Kenney (www.kenney.nl)`
  - mirror is a byte source, not the license authority; CC0 status is verified against Kenney official asset page
- Repository path: `src/main/resources/assets/earth_to_stars/models/kenney/space_kit/`
- Historical NeoForge adapters:
  - source mesh geometry bytes preserved
  - MTL/Minecraft wrappers adapted colours/material slot for the old 26.2 NeoForge OBJ path
- Fabric policy:
  - source meshes remain approved direct-use material
  - do **not** assume the old NeoForge OBJ adapter is the production Fabric renderer
  - convert/adapt through a Fabric-compatible visual pipeline while preserving provenance

---

## 8. Kenney — Modular Space Kit

- Type: `EDITABLE_BASE / DIRECT_USE_CANDIDATE`
- Source: Kenney
- URL: https://kenney.nl/assets/modular-space-kit
- License observed: CC0
- Useful for modular spacecraft/station form language and module prototypes.
- Imported files: none

---

## 9. Kenney — UI Pack: Sci-Fi

- Type: `UI EDITABLE_BASE / REFERENCE`
- Source: Kenney
- URL: https://kenney.nl/assets/ui-pack-sci-fi
- License observed: CC0
- Useful for buttons/panels/widgets as an editable base.
- Quality restriction: establish ETS design tokens, nine-slice behavior, typography/information hierarchy and Minecraft GUI-scale stress first; do not paste the whole pack unchanged.
- Imported files: none

---

## 10. Kenney — Sci-Fi Sounds / Interface Sounds

- Type: `AUDIO EDITABLE_BASE_CANDIDATE`
- Source: Kenney
- URLs:
  - https://kenney.nl/assets/sci-fi-sounds
  - https://kenney.nl/assets/interface-sounds
- License observed: CC0
- Useful for machinery/electronic/UI feedback, alarms and interaction prototyping.
- Required adaptation: normalization, EQ/layering, distance behavior, repetition variation and ETS sound language.
- Imported files: none

---

## 11. Space Engineers

- Type: `REFERENCE_ONLY`
- Source: Keen Software House commercial game
- Useful for modular ship readability, power/logistics concepts, manual/automatic turret interaction, weapon arcs, ship layout consequences and solo/crew operation.
- Policy: proprietary; do not copy code/models/textures/UI/audio.
- Imported files: none

---

## 12. Other Minecraft space/vehicle projects

Research candidates include:

- Ad Astra / Galacticraft lineage — planet progression, life support, travel UX
- Advanced Rocketry — stations, satellites, asteroid/resource progression
- Mekanism / Oritech — technical-system structure and machinery presentation
- modern Fabric vehicle/entity projects — seat/passenger/camera, interpolation, collision and networking patterns
- seamless-space experiments — transition/presentation research

Before any code or asset reuse:

1. identify exact repository/file/commit or release
2. verify current license
3. verify target Minecraft/loader generation
4. verify modification and redistribution terms
5. record author/source
6. classify as reference, code research, editable base or direct use
7. port only the required part rather than adding the whole source mod as player installation work

---

# Asset / Code Import Records

| ID | Asset / code | Author | Source | License | Use class | Modifications | Repository paths | Verified |
|---|---|---|---|---|---|---|---|---|
| ETS-FABRIC-TEMPLATE-001 | Fabric 26.2 project/dependency structure | FabricMC contributors | `FabricMC/fabric-example-mod`, branch `26.2` | CC0-1.0 | `DIRECT_USE / CODE_TEMPLATE` | adapted identifiers/build layout; no gameplay copied | `build.gradle`, `gradle.properties`, `settings.gradle`, `src/fabric/resources/fabric.mod.json` | 2026-09-15 |
| ETS-KENNEY-SPACE-001 | `craft_speederA` / normalized `craft_speedera` OBJ/MTL | Kenney | https://kenney.nl/assets/space-kit | CC0 1.0 | `DIRECT_USE` | mesh preserved; Minecraft wrappers/transforms adapted | `src/main/resources/assets/earth_to_stars/models/kenney/space_kit/craft_speedera.*` | 2026-09-08 |
| ETS-KENNEY-SPACE-002 | `craft_miner.obj/.mtl` | Kenney | https://kenney.nl/assets/space-kit | CC0 1.0 | `DIRECT_USE` | mesh preserved; Minecraft wrappers/transforms adapted | `src/main/resources/assets/earth_to_stars/models/kenney/space_kit/craft_miner.*` | 2026-09-08 |
| ETS-KENNEY-SPACE-003 | `craft_racer.obj/.mtl` | Kenney | https://kenney.nl/assets/space-kit | CC0 1.0 | `DIRECT_USE` | mesh preserved; Minecraft wrappers/transforms adapted | `src/main/resources/assets/earth_to_stars/models/kenney/space_kit/craft_racer.*` | 2026-09-08 |

Use classes:

- `REFERENCE_ONLY`
- `CODE_RESEARCH`
- `EDITABLE_BASE`
- `DIRECT_USE`
- `CODE_TEMPLATE`

---

# Rejection Rules

Do not add:

- unclear-license resources
- untraceable source/author assets
- paid assets obtained without permission
- assets extracted from another mod JAR without reuse permission
- ripped commercial-game assets
- Minecraft distribution files
- untracked temporary models/sounds
- GPL source copied into this ARR repository without an explicit license decision

개인 플레이 목적이라도 이 GitHub 저장소는 공개 빌드 허브이므로 예외로 보지 않는다.

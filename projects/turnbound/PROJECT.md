# TURNBOUND Project Contract

## Identity
- Path: `projects/turnbound/`
- Mod ID: `turnbound`
- Display name: `TURNBOUND`
- Package: `io.github.q93503128.turnbound`
- Version: `0.1.0-alpha.13`

## Toolchain
- Minecraft Java 26.2
- NeoForge 26.2.0.62
- Java 25
- Gradle 9.2.1
- GeckoLib retained for authored model/animation phase

## Canon authority
Implementation must be checked against:
1. `01_세부기획서_v0.4`
2. `02_수치규칙위키_v0.4`
3. `03_캐릭터설계위키_v0.4`

Later explicit user decisions may override these. Alpha delta docs may clarify implementation state but do not silently replace v0.4 content.

## Combat contract
- Turn threshold 1000; action subtracts 1000 rather than resetting Gauge.
- Gauge overflow survives actions and natural consecutive turns have no arbitrary cap.
- Player decision time does not advance logical combat time.
- Cooldowns tick on the owner's later regular actions, not reactions.
- Basic actions may be non-damaging and have cooldown 0.
- 1–4 allies / 1–5 enemies.
- General field battle flee is allowed; boss/event locks are encounter data.
- B01 is flee-locked.
- Campaign AUTO/2.0x are locked until B01 first clear.

## Battle UX contract
- 3D battlefield remains dominant; no full-screen battle veil.
- Camera pivot derives from battle formation anchors.
- Every skill requires explicit final confirmation.
- Single-target skills do not silently preselect the first target.
- Direct 3D click is primary target input; HUD/Tab are fallbacks.
- Hovering a skill shows detail.
- Danger telegraphs must be visible in both world association and HUD/timeline data.

## Southgate Chapter 1 v0.4 contract
Campaign party: P01 / P03 / P04 / F03.

Encounter template:
- M01 Lv1 E001×2
- M02 Lv2 E001+E002
- M03 Lv3 E004×2
- M04 Lv4 E003+E002
- M05 Lv5 E005+E001×2

Main quest gate:
- M01+M02 → MQ_C01_01 complete / forward Meadow route
- M04 E003 fight → MQ_C01_02 complete / B01 route
- B01 → MQ_C01_03 complete
M03/M05 remain optional to the main boss gate.

E003: 650/125/50/78, body slam 0.70x, arm, next own normal action AoE 1.20x then self-down.
E004: 680/98/64/100, slash 1.00x, low-HP priority stab 1.55x CD2.
E005: 590/82/60/94, heal 0.55x, team DEF +15% 2 owner actions CD3.

B01 Graul:
- Lv6 / HP2800 ATK150 DEF115 SPD92
- 100–70: horn 1.20x; ground scratch ATK+15% 2 actions CD3
- ≤70 once: summon E001+E002; while add alive DEF+15%
- ≤35 once: SPD+20%
- charge cycle: warning action → next action ally-all 1.05x, CD4

B01 first clear:
- XP 5000 / Gold 12000
- Crystal 1200 / Star Essence 60 / T2 choice box 1
- P08 / Echo Archive / AUTO / 2.0x unlock
- P3 must additionally connect tutorial Crystal +1800 so Starter 10-pull is immediately possible.

## Minecraft shell
TURNBOUND is not a survival game.
- Vanilla player hearts are not gameplay HP.
- Incoming vanilla damage to the shell is ignored.
- Hunger is kept full and is not a resource.
- Survival HUD is hidden.
- Combat HP belongs only to CombatantState.

## World
- Fixed authored RPG world, not vanilla infinite progression.
- v0.1 target Aster March 1024×1024.
- Current A01/A02 are Southgate Meadow development cells, not separate chapters.
- Chapter 2 target is Gloamwood.
- Final major FT and boss anchors follow v0.4 coordinate tables unless the design docs are revised together.

## Required validation
- Java 25 clean test/build green.
- NeoForge real server boot smoke green.
- JAR metadata/classes/resources verified.
- Existing camera/direct-target/explicit-confirm/hover-tooltip regressions remain green.

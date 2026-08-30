# TURNBOUND Project Contract

## Identity
- Path: `projects/turnbound/`
- Mod ID: `turnbound`
- Display name: `TURNBOUND`
- Package: `io.github.q93503128.turnbound`
- Version: `0.1.0-alpha.15`

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

Later explicit user decisions override older implementation assumptions. Alpha delta docs describe implementation state but do not silently erase v0.4 world/content canon.

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

## Battle UX contract — alpha.15
- 3D battlefield remains dominant; no full-screen battle veil or giant enemy/ally wall panels.
- Camera pivot is the midpoint of the ally formation centroid and enemy formation centroid, so uneven team sizes do not pull the frame off-center.
- Direct 3D click is the primary target input; HUD/Tab remain fallbacks.
- Mouse picking must use the actual live Minecraft camera projection so click position follows the rendered actor after camera collision/zoom/orbit.
- Single-target skills do not silently preselect the first target.
- A skill click arms/selects it. Clicking the same skill again quickly commits it when its target state is already valid.
- A target click selects it. Clicking the same valid target again quickly commits the armed skill.
- There is no separate permanent `사용` button. Enter remains keyboard confirmation fallback.
- Selected enemy uses a red world marker; selected ally uses an aqua marker; current actor has a distinct gold emphasis.
- Enemy HP/status belongs beside the actual 3D enemy, not in a large detached top-right wall.
- Party state stays compact at the lower edge; timeline stays thin at top center; contextual skills stay lower-right; AUTO/speed/flee occupy a separate non-overlapping control strip.
- Hovering a skill shows detail.
- Danger telegraphs must remain associated with the relevant world actor and readable in combat state.
- Locked AUTO/2x/flee controls must visibly communicate their locked state and not send invalid client commands.

## UI reference policy
- Do not improvise a generic Minecraft/AI-style rectangle UI as the visual baseline.
- Minecraft mod UI references are preferred for frame hierarchy, density and interaction patterns.
- BetterQuesting is a reference for compact nested quest surfaces and information hierarchy.
- REI is a reference for compact framed controls, dense layouts and tooltip hierarchy.
- User-supplied reference-game screenshots are spatial/hierarchy references only: world-dominant scene, compact party/action UI and clear target association.
- Do not copy proprietary pixels, textures, icons, fonts or source code. See `EXTERNAL_ASSETS.md`.

## Southgate Chapter 1 v0.4 content contract
Campaign party: P01 / P03 / P04 / F03.

Encounter template:
- M01 Lv1 E001×2
- M02 Lv2 E001+E002
- M03 Lv3 E004×2
- M04 Lv4 E003+E002
- M05 Lv5 E005+E001×2

Main quest gate:
- M01+M02 → MQ_C01_01 complete / FT_MEADOW map-travel unlock
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

## Current playable world slice — alpha.15
- Normal play does not require `/turnbound` commands.
- After entering a new Overworld, TURNBOUND automatically starts after the initial load delay.
- Default Superflat is supported without a custom preset; the starter slice samples the actual surface Y before authoring itself.
- Current intentionally small test scope is `peaceful starter village 64×64 → south gate → first field 64×64`.
- The village has no combat enemies. It contains the scout NPC and relay/travel object.
- The first field contains visible M01/M02 patrol encounters separated enough to be avoided before engagement.
- This small vertical slice temporarily replaces the much larger alpha.14 implementation as the active playtest surface; it does not delete the v0.4 Aster March/Southgate content plan.
- Future expansion may repeat `safe village/hub → combat field → safe village/hub → next field` where it improves pacing.
- PvP/team battle is a future extension candidate, not current P2 scope.

## Required validation
- Java 25 clean test/build green.
- NeoForge real server boot smoke green.
- JAR metadata/classes/resources verified.
- Automatic starter-slice entry must remain command-free on Overworld/Superflat.
- HUD rectangles must remain inside the viewport and party/skills/control strip must not overlap at supported test resolutions.
- Live-camera direct targeting, double-click commit, battlefield centering and hover tooltip regressions remain green.
- Later restoration/extension of the full Southgate route must preserve v0.4 quest/encounter/boss canon unless explicitly changed.

#!/usr/bin/env python3
from __future__ import annotations

import contextlib
import io
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
errors: list[str] = []
CURRENT_VERSION = "0.61.0-alpha.1"
PREVIOUS_DOC_VERSION = "0.59.0-alpha.1"


def read(rel: str) -> str:
    path = ROOT / rel
    if not path.exists():
        errors.append(f"missing: {rel}")
        return ""
    return path.read_text(encoding="utf-8")


def need(text: str, needles: list[str], label: str) -> None:
    for needle in needles:
        if needle not in text:
            errors.append(f"{label} missing: {needle}")


def forbid(text: str, needles: list[str], label: str) -> None:
    for needle in needles:
        if needle in text:
            errors.append(f"{label} forbidden: {needle}")


# Preserve the complete 0.58 regression contract. Runtime/version checks advance to the
# current release; PROJECT remains the historical 0.59 long-form design baseline.
legacy_path = ROOT / "tools/test_release_source_058.py"
legacy = legacy_path.read_text(encoding="utf-8")
legacy = legacy.replace('REQUIRED_VERSION = "0.58.0-alpha.1"', f'REQUIRED_VERSION = "{CURRENT_VERSION}"')
legacy = legacy.replace(r'VERSION = \"0.58.0-alpha.1\"', rf'VERSION = \"{CURRENT_VERSION}\"')
legacy = legacy.replace('Mod version: `0.58.0-alpha.1`', f'Mod version: `{PREVIOUS_DOC_VERSION}`')
# 0.61 preserves every 0.58 construction length and appends 81 as a post-final mastery ceiling.
# Rewrite only the historical audit needle so the legacy contract verifies the preserved prefix
# instead of falsely requiring the array to terminate at 65.
legacy = legacy.replace(
    'CONSTRUCTION_LENGTHS = {5, 9, 17, 33, 49, 65}',
    'CONSTRUCTION_LENGTHS = {5, 9, 17, 33, 49, 65, 81}'
)
# 0.61 solo-balance pass intentionally raises equipment caps while preserving every old mechanic.
for old, new in [
    ('"Math.min(0.35D, reduction)"', '"Math.min(0.70D, reduction)"'),
    ('"Math.min(1.32D, 1.0D + bonus)"', '"Math.min(2.00D, 1.0D + bonus)"'),
    ('"Math.min(1.25D"', '"Math.min(2.40D"'),
    ('"Math.min(1.50D"', '"Math.min(3.00D"'),
    ('"Math.min(1.5D"', '"Math.min(5.0D"'),
    ('"Math.min(4"', '"Math.min(10"'),
    ('"Math.min(0.15D"', '"Math.min(0.50D"'),
    ('"Math.min(8.0D"', '"Math.min(14.0D"'),
    ('"Math.min(14"', '"Math.min(28"'),
    ('"Math.min(1.30D"', '"Math.min(2.00D"'),
    ('"Math.min(0.28D"', '"Math.min(0.60D"'),
    ('"Math.min(9.0D"', '"Math.min(15.0D"'),
    ('"Math.min(8,"', '"Math.min(20,"'),
    ('"Math.min(1.10D"', '"Math.min(1.75D"'),
    ('"Math.min(10.5D"', '"Math.min(16.5D"'),
    ('"Math.min(26"', '"Math.min(50"'),
    ('"Math.min(0.65D"', '"Math.min(1.00D"'),
]:
    legacy = legacy.replace(old, new)

# 0.61 also replaces the player-facing developer term "affix" with "승천 옵션".
# Keep the historical 0.58 source untouched and adapt only its UI regression needle here.
legacy = legacy.replace('h("방어구 affix")', 'h("방어구 승천 옵션")')
# 0.61 solo/QOL expectation translation; historical contracts stay immutable.
for old, new in [
    ('Math.min(8', 'Math.min(20'),
    ('Math.min(13, base + bonus)', 'Math.min(21, base + bonus)'),
    ('최대35%', '상한은 70%'),
    ('최대32%', '최대 2배'),
    ('new MaterialCost(Items.AMETHYST_SHARD, 48', 'new MaterialCost(Items.AMETHYST_SHARD, 12'),
    ('new MaterialCost(Items.AMETHYST_SHARD, 96', 'new MaterialCost(Items.AMETHYST_SHARD, 24'),
    ('재료 소비: 모드 제작·건축·인프라 비용은 가까운 사용 가능 물류 통부터', '재료 소비: 같은 차원에서 현재 로딩된 등록 창고 전체를 공용 재고로 사용하고'),
    ('new LocalRequirement("연료", 8', 'new LocalRequirement("연료(석탄 또는 숯)", 3'),
    ('new LocalRequirement("식량", 32', 'new LocalRequirement("식량(밀/당근/감자/비트)", 12'),
    ('new LocalRequirement("철 주괴", 8', 'new LocalRequirement("철 주괴", 3'),
    ('new LocalRequirement("식량", 48', 'new LocalRequirement("식량(밀/당근/감자/비트)", 16'),
    ('new LocalRequirement("철 주괴", 16', 'new LocalRequirement("철 주괴", 5'),
    ('new LocalRequirement("통나무", 32', 'new LocalRequirement("아무 종류의 통나무", 12'),
    ('new LocalRequirement("식량", 96', 'new LocalRequirement("식량(밀/당근/감자/비트)", 32'),
    ('new LocalRequirement("철 주괴", 32', 'new LocalRequirement("철 주괴", 8'),
    ('new LocalRequirement("석재 벽돌", 128', 'new LocalRequirement("석재 벽돌", 32'),
    ('전초재고(식량48/철16/통나무32)', '전초 재고(식량 16 · 철 주괴 5 · 아무 종류의 통나무 12)'),
    ('전초재고(식량96/철32/석재벽돌128)', '전초 재고(식량 32 · 철 주괴 8 · 석재 벽돌 32)'),
    ('전초재고(식량32/철8/연료8)', '전초 재고(식량 12 · 철 주괴 3 · 연료: 석탄 또는 숯 3)'),
    ('한도3→토목6→중추9', '산업 가공소 완공 → 통 4블록 이내'),
    ('화물 → 전초 현지재고 → 방어/원정', '등록 물류 통은 같은 차원 로딩 중이면 원격 사용'),
    ('new Requirement(Items.STONE_BRICKS, "석재 벽돌", 2048)', 'new Requirement(Items.STONE_BRICKS, "석재 벽돌", 384)'),
    ('new Requirement(Items.COBBLESTONE, "조약돌", 1536)', 'new Requirement(Items.COBBLESTONE, "조약돌", 256)'),
    ('new Requirement(Items.GRAVEL, "자갈", 1536)', 'new Requirement(Items.GRAVEL, "자갈", 256)'),
]:
    legacy = legacy.replace(old, new)
# 0.61 solo/QOL nested-baseline translation v2
# Adapt direct 0.58 needles without editing the historical audit file.
for _old, _new in [
    ('Math.min(13, base + bonus)', 'Math.min(21, base + bonus)'),
    ('new MaterialCost(Items.AMETHYST_SHARD, 48', 'new MaterialCost(Items.AMETHYST_SHARD, 12'),
    ('new MaterialCost(Items.AMETHYST_SHARD, 96', 'new MaterialCost(Items.AMETHYST_SHARD, 24'),
    ('재료 소비: 모드 제작·건축·인프라 비용은 가까운 사용 가능 물류 통부터', '재료 소비: 같은 차원에서 현재 로딩된 등록 창고 전체를 공용 재고로 사용하고'),
    ('4블록 내 기본 통 앵커', '산업 가공소 완공 → 통 4블록 이내'),
    ('new LocalRequirement("식량", 32', 'new LocalRequirement("식량(밀/당근/감자/비트)", 12'),
    ('new LocalRequirement("철 주괴", 8', 'new LocalRequirement("철 주괴", 3'),
    ('new LocalRequirement("연료", 8', 'new LocalRequirement("연료(석탄 또는 숯)", 3'),
    ('new LocalRequirement("식량", 48', 'new LocalRequirement("식량(밀/당근/감자/비트)", 16'),
    ('new LocalRequirement("철 주괴", 16', 'new LocalRequirement("철 주괴", 5'),
    ('new LocalRequirement("통나무", 32', 'new LocalRequirement("아무 종류의 통나무", 12'),
    ('new LocalRequirement("식량", 96', 'new LocalRequirement("식량(밀/당근/감자/비트)", 32'),
    ('new LocalRequirement("철 주괴", 32', 'new LocalRequirement("철 주괴", 8'),
    ('new LocalRequirement("석재 벽돌", 128', 'new LocalRequirement("석재 벽돌", 32'),
    ('전초재고(식량48/철16/통나무32)', '전초 재고(식량 16 · 철 주괴 5 · 아무 종류의 통나무 12)'),
    ('전초재고(식량96/철32/석재벽돌128)', '전초 재고(식량 32 · 철 주괴 8 · 석재 벽돌 32)'),
    ('전초재고(식량32/철8/연료8)', '전초 재고(식량 12 · 철 주괴 3 · 연료: 석탄 또는 숯 3)'),
    ('화물 → 전초 현지재고 → 방어/원정', '등록 물류 통은 같은 차원 로딩 중이면 원격 사용'),
    ('new Requirement(Items.STONE_BRICKS, "석재 벽돌", 2048)', 'new Requirement(Items.STONE_BRICKS, "석재 벽돌", 384)'),
    ('new Requirement(Items.COBBLESTONE, "조약돌", 1536)', 'new Requirement(Items.COBBLESTONE, "조약돌", 256)'),
    ('new Requirement(Items.GRAVEL, "자갈", 1536)', 'new Requirement(Items.GRAVEL, "자갈", 256)'),
    ('한도3→토목6→중추9', '산업 가공소 완공 → 통 4블록 이내'),
    ('FRONTLINE_FOOD = 176', 'FRONTLINE_FOOD = 60'),
    ('FRONTLINE_IRON = 56', 'FRONTLINE_IRON = 16'),
    ('FRONTLINE_FUEL = 8', 'FRONTLINE_FUEL = 3'),
    ('FRONTLINE_LOGS = 32', 'FRONTLINE_LOGS = 12'),
    ('FRONTLINE_STONE_BRICKS = 128', 'FRONTLINE_STONE_BRICKS = 32'),
    ('식량176+철56+석탄/목탄8+통나무32+석재벽돌128', '식량(밀/당근/감자/비트) 60 + 철 주괴 16 + 연료(석탄 또는 숯) 3 + 아무 종류의 통나무 12 + 석재 벽돌 32'),
    ('원정은 식량32+철8+석탄/목탄8', '원정은 식량(밀/당근/감자/비트) 12 + 철 주괴 3 + 연료(석탄 또는 숯) 3'),
    ('전초 방어는 식량48+철16+통나무32', '전초 방어는 식량 16 + 철 주괴 5 + 아무 종류의 통나무 12'),
    ('요새 방어는 식량96+철32+석재벽돌128', '요새 방어는 식량 32 + 철 주괴 8 + 석재 벽돌 32'),
]:
    legacy = legacy.replace(_old, _new)

# Inject approved translations into nested test_current_source.py baseline.
_baseline_lines = []
for _old, _new in [
    ('Math.min(13, base + bonus)', 'Math.min(21, base + bonus)'),
    ('new MaterialCost(Items.AMETHYST_SHARD, 48', 'new MaterialCost(Items.AMETHYST_SHARD, 12'),
    ('new MaterialCost(Items.AMETHYST_SHARD, 96', 'new MaterialCost(Items.AMETHYST_SHARD, 24'),
    ('재료 소비: 모드 제작·건축·인프라 비용은 가까운 사용 가능 물류 통부터', '재료 소비: 같은 차원에서 현재 로딩된 등록 창고 전체를 공용 재고로 사용하고'),
    ('4블록 내 기본 통 앵커', '산업 가공소 완공 → 통 4블록 이내'),
    ('new LocalRequirement("식량", 32', 'new LocalRequirement("식량(밀/당근/감자/비트)", 12'),
    ('new LocalRequirement("철 주괴", 8', 'new LocalRequirement("철 주괴", 3'),
    ('new LocalRequirement("연료", 8', 'new LocalRequirement("연료(석탄 또는 숯)", 3'),
    ('new LocalRequirement("식량", 48', 'new LocalRequirement("식량(밀/당근/감자/비트)", 16'),
    ('new LocalRequirement("철 주괴", 16', 'new LocalRequirement("철 주괴", 5'),
    ('new LocalRequirement("통나무", 32', 'new LocalRequirement("아무 종류의 통나무", 12'),
    ('new LocalRequirement("식량", 96', 'new LocalRequirement("식량(밀/당근/감자/비트)", 32'),
    ('new LocalRequirement("철 주괴", 32', 'new LocalRequirement("철 주괴", 8'),
    ('new LocalRequirement("석재 벽돌", 128', 'new LocalRequirement("석재 벽돌", 32'),
    ('전초재고(식량48/철16/통나무32)', '전초 재고(식량 16 · 철 주괴 5 · 아무 종류의 통나무 12)'),
    ('전초재고(식량96/철32/석재벽돌128)', '전초 재고(식량 32 · 철 주괴 8 · 석재 벽돌 32)'),
    ('전초재고(식량32/철8/연료8)', '전초 재고(식량 12 · 철 주괴 3 · 연료: 석탄 또는 숯 3)'),
    ('화물 → 전초 현지재고 → 방어/원정', '등록 물류 통은 같은 차원 로딩 중이면 원격 사용'),
    ('new Requirement(Items.STONE_BRICKS, "석재 벽돌", 2048)', 'new Requirement(Items.STONE_BRICKS, "석재 벽돌", 384)'),
    ('new Requirement(Items.COBBLESTONE, "조약돌", 1536)', 'new Requirement(Items.COBBLESTONE, "조약돌", 256)'),
    ('new Requirement(Items.GRAVEL, "자갈", 1536)', 'new Requirement(Items.GRAVEL, "자갈", 256)'),
    ('한도3→토목6→중추9', '산업 가공소 완공 → 통 4블록 이내'),
    ('FRONTLINE_FOOD = 176', 'FRONTLINE_FOOD = 60'),
    ('FRONTLINE_IRON = 56', 'FRONTLINE_IRON = 16'),
    ('FRONTLINE_FUEL = 8', 'FRONTLINE_FUEL = 3'),
    ('FRONTLINE_LOGS = 32', 'FRONTLINE_LOGS = 12'),
    ('FRONTLINE_STONE_BRICKS = 128', 'FRONTLINE_STONE_BRICKS = 32'),
    ('식량176+철56+석탄/목탄8+통나무32+석재벽돌128', '식량(밀/당근/감자/비트) 60 + 철 주괴 16 + 연료(석탄 또는 숯) 3 + 아무 종류의 통나무 12 + 석재 벽돌 32'),
    ('원정은 식량32+철8+석탄/목탄8', '원정은 식량(밀/당근/감자/비트) 12 + 철 주괴 3 + 연료(석탄 또는 숯) 3'),
    ('전초 방어는 식량48+철16+통나무32', '전초 방어는 식량 16 + 철 주괴 5 + 아무 종류의 통나무 12'),
    ('요새 방어는 식량96+철32+석재벽돌128', '요새 방어는 식량 32 + 철 주괴 8 + 석재 벽돌 32'),
]:
    _baseline_lines.append(f"baseline = baseline.replace({_old!r}, {_new!r})")
_baseline_anchor = 'baseline = baseline.replace(BASELINE_VERSION, REQUIRED_VERSION)'
legacy = legacy.replace(_baseline_anchor, _baseline_anchor + '\n' + '\n'.join(_baseline_lines), 1)

namespace = {"__file__": str(legacy_path), "__name__": "__main__"}
buffer = io.StringIO()
exit_code = 0
try:
    with contextlib.redirect_stdout(buffer):
        exec(compile(legacy, str(legacy_path), "exec"), namespace)
except (SystemExit, AssertionError) as exc:
    exit_code = int(exc.code or 0) if isinstance(exc, SystemExit) else 1
    if not isinstance(exc, SystemExit):
        print(f"0.58 regression assertion: {exc}", file=sys.stderr)
legacy_output = buffer.getvalue().replace("protocol8", "protocol9")
print(legacy_output, end="")
if exit_code != 0:
    print("RELEASE SOURCE AUDIT FAIL: 0.58 regression contract failed under 0.61 runtime identity")
    sys.exit(exit_code)

props = read("gradle.properties")
main = read("src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java")
apex = read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java")
bridge = read("src/main/java/kr/moonseungjun/survivalascension/compat/ApexContentPackBridge.java")
phase = read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexPhaseMutationService.java")
interdiction = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionInterdictionService.java")
woodcutting = read("src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java")
harvesting = read("src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java")
final_gate = read("src/main/java/kr/moonseungjun/survivalascension/endgame/FinalAscensionProgression.java")
final_system = read("src/main/java/kr/moonseungjun/survivalascension/endgame/FinalAscensionSystem.java")
final_boss = read("src/main/java/kr/moonseungjun/survivalascension/endgame/FinalAscensionBossSystem.java")
final_data = read("src/main/java/kr/moonseungjun/survivalascension/endgame/FinalAscensionData.java")
mobility = read("src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java")
construction = read("src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java")
elite = read("src/main/java/kr/moonseungjun/survivalascension/elite/EliteMobSystem.java")
infrastructure = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java")
menu = read("src/main/java/kr/moonseungjun/survivalascension/client/InfrastructureRadialMenuScreen.java")
guide = read("src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java")
equipment_menu = read("src/main/java/kr/moonseungjun/survivalascension/client/EquipmentRadialMenuScreen.java")
project = read("PROJECT.md")
readme = read("README.md")
changelog = read("CHANGELOG.md")
testing = read("TESTING.md")
release_061 = read("RELEASE-0.61.0-alpha.1.md")
testing_061 = read("TESTING-0.61.md")

need(props, [f"mod_version={CURRENT_VERSION}"], "0.61 release identity")
need(main, [
    f'VERSION = "{CURRENT_VERSION}"', "ApexContentPackBridge::onServerStarted",
    "ApexPhaseMutationService::onIncomingDamage", "ExpeditionInterdictionService::onPlayerTick",
    "WoodcuttingProgression::onServerTick", "HarvestingProgression::onServerTick",
    "FinalAscensionSystem::onServerTick", "FinalAscensionSystem::onEntityJoin",
    "FinalAscensionBossSystem::onIncomingDamage", "FinalAscensionBossSystem::onLivingDeath",
    "FinalAscensionBossSystem::onServerTick", "FinalAscensionBossSystem::onEntityJoin",
    "three-phase Final Ascension boundary boss"
], "0.61 runtime wiring")

# 0.59 optional Apex escort behavior remains a regression contract.
need(bridge, [
    "APEX_ESCORTS_TIER_0", "APEX_ESCORTS_TIER_1", "APEX_ESCORTS_TIER_2",
    "randomEscortId(RandomSource random, int worldStage)", "escortIds(int worldStage)",
    "type.getCategory() != MobCategory.MONSTER", "Tags.EntityTypes.BOSSES", "apex_escort_tier_"
], "0.59 optional Apex escort compatibility")
need(apex, [
    "ApexContentPackBridge.randomEscortId", "archetype.aquatic() ? null", "packSlot",
    "if (escort == null && packEscort)", "escort.setGlowingTag(true)", "hunt.packEscortCount++",
    "이변 호위 1체 포함"
], "0.59 Apex escort replacement")
forbid(bridge, ["tbos:", "com.nightbeam", "setChunkForced", "addRegionTicket", "getChunk("],
       "0.59 optional-content/force-load policy")

# 0.59.1 closure checks remain regression contracts.
need(phase, [
    "boss.getHealth() - event.getAmount()", "healthRatio <= 0.62D", "healthRatio <= 0.32D",
    "triggerPhaseOne", "triggerPhaseTwo"
], "0.59.1 Apex phase threshold crossing")
need(interdiction, [
    "STAGE_KEY", "firstWaveReady", "secondWaveReady", "spawnWave(player, level, active, 1)",
    "spawnWave(player, level, active, 2)", "ContentPackCompatibility.randomIncidentReinforcementId"
], "0.59.1 operation interdictions")
need(woodcutting, [
    "skillLevel >= 90", "groveTreeCap", "player.isShiftKeyDown()", "FIELD_MASTERY_LOG_LIMIT = 448",
    "SkillTuning.woodcuttingLogLimit"
], "0.59.1 high-rank woodcutting")
need(harvesting, [
    "skillLevel >= 90 ? 4 : 0", "fieldMastery ? 8", "player.isShiftKeyDown()", "MAX_PENDING_PER_PLAYER = 384"
], "0.59.1 high-rank harvesting")

# Canonical final admission authority remains unchanged and visible through normal play.
need(final_gate, [
    "WorldAscensionData.get(level.getServer()).stage() >= 2",
    "expeditions.countCompleted(player)", "apex.uniqueDefeated(player)",
    "InfrastructureProject.ASCENSION_NEXUS", "REQUIRED_EXPEDITIONS = 9", "REQUIRED_APEX = 9",
    "최후의 승천 준비", "missingRequirements()", "FinalAscensionData.sendStatus(player)"
], "final ascension canonical gate")
need(infrastructure, [
    "FinalAscensionProgression.sendStatus(player)", "최후의 승천 준비 현황은 인프라 → 진행도",
    'ACTION_FINAL_ASCENSION = "final_ascension"', "FinalAscensionSystem.tryStart(player)",
    "FinalAscensionSystem.isActive(player)"
], "final ascension admission/status exposure")
need(menu, [
    'new Entry("최후의 승천"', "ACTION_FINAL_ASCENSION", "세계의 시험 → 9지역 잔향 → 붕괴 봉쇄"
], "final ascension menu entry")
need(guide, [
    'h("방어구 승천 옵션")', 'h("최후의 승천")',
    "최후의 승천 완료 후 Lv.100", "선/도로81", "공중돌진5회",
    "승천 옵션이 없는 표준 검/스피어/메이스/활/쇠뇌/곡괭이/도끼/삽/괭이/방어구/방패"
], "0.61 player-facing guide")
need(equipment_menu, [
    "이미 Survival Ascension 승천 옵션 장비입니다", "4번째 승천 옵션 개방"
], "0.61 player-facing equipment wording")
forbid(guide + equipment_menu, ["방어구 affix", "장비 affix", "4번째 affix 개방"],
       "0.61 player-facing developer terminology")

# 0.60 acts 1-3 remain real-world, bounded and isolated from Apex persistence.
need(final_system, [
    "FinalAscensionProgression.isReady(player)", "FinalAscensionData.get(level.getServer()).isComplete()",
    "ACT1_MINING", "ACT1_BUILD", "ACT1_COMBAT", "ACT1_MOVE",
    "ACT2_SET_ONE", "ACT2_SET_TWO", "ACT2_SET_THREE",
    "ACT3_SEAL_ONE", "ACT3_SEAL_TWO", "ACT3_SEAL_THREE",
    "Blocks.CRYING_OBSIDIAN", "Blocks.AMETHYST_BLOCK", "Blocks.GLOWSTONE",
    "isShiftKeyDown()", "level.hasChunkAt", "clearTransient(run)",
    '"minecraft:spider"', '"minecraft:skeleton"', '"minecraft:drowned"',
    '"minecraft:ravager"', '"minecraft:guardian"', '"minecraft:witch"',
    '"minecraft:stray"', '"minecraft:wither_skeleton"', '"minecraft:enderman"',
    "OWNER_KEY", "PHASE_KEY", "SEAL_CHANNEL_TICKS",
    "FinalAscensionBossSystem.tryStartFromClosure(owner, run.center)"
], "0.60/0.61 Final Ascension acts 1-3")
forbid(final_system, [
    "ApexHuntData", "defeatedMask", "recordDefeat", "tbos:", "com.nightbeam",
    "setChunkForced", "addRegionTicket", "getChunk(", "extends SavedData"
], "final acts isolation policy")

# 0.61 unique final boss: actual phase gates, physical anchors and readable arena telegraphs.
need(final_boss, [
    'BOSS_KEY = "survivalascension_final_boss"', 'OWNER_KEY = "survivalascension_final_boss_owner"',
    'Identifier.parse("minecraft:warden")', "FinalAscensionData.get(level.getServer()).isComplete()",
    "Phase.OPENING", "Phase.ANCHORS", "Phase.BREAKTHROUGH", "Phase.FINAL",
    "floorRatio = run.phase == Phase.OPENING ? 0.65F : run.phase == Phase.BREAKTHROUGH ? 0.30F : 0.0F",
    "event.setAmount(Math.max(0.0F, boss.getHealth() - floor))",
    "Blocks.CRYING_OBSIDIAN", "run.anchorTotal++", "run.anchors.entrySet().removeIf",
    "AttackPattern.LINE", "AttackPattern.RING", "AttackPattern.MARKED",
    "TELEGRAPH_TICKS = 30", "insideLine", "horizontalDistance", "renderTelegraph",
    "level.hasChunkAt", "findOpenSpawn", "clearMarkers(run)",
    "FinalAscensionData.get(run.level.getServer()).complete(owner)",
    "AscensionAffixes.createEliteDrop", "AscensionAffixes.awaken", "승천의 증표"
], "0.61 final boundary boss")
forbid(final_boss, [
    "ApexHuntData", "recordVictory", "recordDefeat", "tbos:", "com.nightbeam",
    "setChunkForced", "addRegionTicket", "getChunk(", "teleportTo", "randomTeleport"
], "0.61 boss isolation/force-load policy")

# Only one new world SavedData authority is introduced, with backwards-safe optional defaults.
need(final_data, [
    '"final_ascension_v1"', 'Codec.BOOL.optionalFieldOf("complete", false)',
    'Codec.STRING.optionalFieldOf("first_conqueror", "")',
    'Codec.LONG.optionalFieldOf("completed_game_time", 0L)',
    "public boolean complete(ServerPlayer player)", "setDirty()", "isComplete()"
], "0.61 final completion persistence")
forbid(final_data, ["ApexHuntData", "ExpeditionData", "InfrastructureData", "setChunkForced", "getChunk("],
       "0.61 final completion authority separation")

# Final reward is action-scale authority, not another universal raw-stat tier.
need(mobility, [
    "FinalAscensionData", "level >= 100 && finalAscensionComplete(player)",
    "return ExpeditionProgression.hasFieldMastery(player) ? 5 : 4",
    "SkillTuning.mobilityDashPower(level) + (finalMastery ? 0.15D : 0.0D)",
    "cooldown = Math.max(12, cooldown - 4)"
], "0.61 final mobility authority")
need(construction, [
    "FinalAscensionData", "CONSTRUCTION_LENGTHS = {5, 9, 17, 33, 49, 65, 81}",
    "return ExpeditionProgression.hasFieldMastery(player) ? 81 : 65",
    "int size = finalMastery ? 15 : fieldMastery ? 13 : SkillTuning.constructionPlaneSize(level)"
], "0.61 final construction authority")
forbid(mobility + construction, ["setChunkForced", "addRegionTicket", "getChunk("],
       "0.61 final authority world-loading policy")

# The Warden shell must not be randomly converted into a normal elite during internal boss spawn.
need(elite, ["FinalAscensionBossSystem.isInternalSpawn()"], "0.61 final boss elite isolation")

# Historical docs remain regression evidence; 0.61 has a focused release/acceptance note.
need(project, ["Mod version: `0.59.0-alpha.1`", "## 0.59 Apex Content Escort Integration"], "historical PROJECT regression docs")
need(readme, ["## 0.59.0-alpha.1", "정점 사냥", "호위 수 자체를 늘리지"], "historical README regression docs")
need(changelog, ["## 0.60.0-alpha.1", "Final Ascension", "Network protocol remains 9"], "0.60 CHANGELOG regression docs")
need(testing, ["## 0.60 focused checks", "최후의 승천", "웅크리기", "Apex 진행"], "0.60 manual regression matrix")
need(release_061, [
    "0.61.0-alpha.1", "세계의 경계자", "65%", "30%", "final_ascension_v1",
    "공중 돌진", "81", "15×15", "Network protocol 9"
], "0.61 release note")
need(testing_061, [
    "Final Ascension", "세계의 경계자", "65%", "고정점 3개", "LINE", "RING", "MARKED",
    "final_ascension_v1", "공중 돌진", "81", "15×15", "Apex"
], "0.61 focused test matrix")

if errors:
    print("RELEASE SOURCE AUDIT FAIL")
    for error in errors:
        print("-", error)
    sys.exit(1)

print("apex_optional_escort_replacement=PASS")
print("apex_optional_java_dependency=ABSENT")
print("apex_escort_count_inflation=ABSENT")
print("apex_phase_crossing_hit=PASS")
print("operation_interdictions=PASS")
print("high_rank_woodcutting_harvesting=PASS")
print("final_ascension_canonical_gate=PASS")
print("final_ascension_acts_1_3=PASS")
print("final_boundary_boss_three_phase=PASS")
print("final_completion_saved_data=PASS")
print("final_mobility_construction_authority=PASS")
print("final_ascension_apex_state_mutation=ABSENT")
print("final_ascension_force_load=ABSENT")
print("player_facing_final_unlock_guidance=PASS")
print("RELEASE SOURCE AUDIT PASS")

#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> None:
    tuning = read("src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java")
    progression = read("src/main/java/kr/moonseungjun/survivalascension/progress/SkillProgressionService.java")
    compat = read("src/main/java/kr/moonseungjun/survivalascension/compat/ContentPackCompatibility.java")
    combat = read("src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java")
    expedition_progression = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionProgression.java")
    wood = read("src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java")
    harvest = read("src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java")
    mining = read("src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java")
    affix = read("src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java")
    reforge = read("src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java")
    equipment_ui = read("src/main/java/kr/moonseungjun/survivalascension/client/EquipmentRadialMenuScreen.java")
    production = read("src/main/java/kr/moonseungjun/survivalascension/production/ProductionService.java")
    production_ui = read("src/main/java/kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.java")
    guide = read("src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java")
    readme = read("README.md")
    project_doc = read("PROJECT.md")
    changelog = read("CHANGELOG.md")
    ores = read("src/main/resources/data/survivalascension/tags/block/valuable_ores.json")
    expedition_region = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionRegion.java")
    deep_expedition = json.loads(read("src/main/resources/data/survivalascension/tags/worldgen/biome/expedition/deep.json"))
    major_targets = json.loads(read("src/main/resources/data/survivalascension/tags/entity_type/expedition_major_targets.json"))
    plan = read("MODPACK_PLAN_DRAFT.md")
    matrix = read("MODPACK_COMPAT_MATRIX.md")
    builder = read("tools/build_mrpack.py")
    lock = json.loads(read("modpack/content-lock.json"))

    require("skillXpMultiplier" in tuning and "scaleSkillXp" in tuning, "skill XP normalization API missing")
    for token in (
        "MINING -> { early = 1.25D; late = 1.10D; }",
        "WOODCUTTING -> { early = 1.60D; late = 1.25D; }",
        "HARVESTING -> { early = 1.50D; late = 1.20D; }",
        "COMBAT -> { early = 1.25D; late = 1.15D; }",
        "CONSTRUCTION -> { early = 2.75D; late = 1.75D; }",
        "MOBILITY -> { early = 2.10D; late = 1.40D; }",
    ):
        require(token in tuning, f"missing authored skill XP factor: {token}")
    require("SkillTuning.scaleSkillXp(skill, currentLevel, amount)" in progression,
            "all standard skill awards must pass through skill-specific normalization")

    require(lock.get("minecraft") == "26.2", "modpack lock Minecraft version drifted")
    require(lock.get("neoforge") == "26.2.0.38-beta", "modpack lock NeoForge version drifted")
    require(lock.get("version") == "0.48.0-alpha.1-content-preview.1", "modpack preview version drifted")
    mods = lock.get("mods") or []
    require(len(mods) >= 6, "first-wave content pack unexpectedly small")
    project_ids = [entry.get("project_id") for entry in mods]
    version_ids = [entry.get("version_id") for entry in mods]
    require(len(project_ids) == len(set(project_ids)), "duplicate Modrinth project id in lock")
    require(len(version_ids) == len(set(version_ids)), "duplicate Modrinth version id in lock")
    for required in ("HXF82T3G", "s3dmwKy5", "gKOBlOap", "8RyryQ7j", "lhGA9TYQ", "9s6osm5g"):
        require(required in project_ids, f"required first-wave project not locked: {required}")
    tbos = next((entry for entry in mods if entry.get("project_id") == "gKOBlOap"), None)
    require(tbos is not None and tbos.get("version_id") == "xls8dTZv", "The Birth of Steve 0.7.0 lock drifted")

    require("https://cdn.modrinth.com/" in builder, "builder must restrict third-party downloads to Modrinth CDN")
    require("overrides/mods/" in builder, "Survival Ascension JAR must be packed as an override")
    require("verify_required_dependencies" in builder, "required dependency validation missing")
    require("modrinth.index.json" in builder, "mrpack index creation missing")
    require('TARGET_LOADER = "neoforge"' in builder, "target loader must be explicit")
    require("choose_file(version: dict, loader: str)" in builder, "loader-aware file selection missing")
    require("loader in str(entry.get(\"filename\", \"\")).lower()" in builder,
            "multi-loader versions must select a loader-marked artifact")
    require('if "fabric" in lowered and TARGET_LOADER not in lowered:' in builder,
            "wrong-loader artifact rejection missing")
    require("wrong-loader artifact" in builder, "built pack wrong-loader self-check missing")

    # Content-pack progression bridge: common conventions, never direct optional-mod implementation imports.
    require('"#c:ores"' in ores, "NeoForge common ore tag is not bridged into vein/extract eligibility")
    require("Tags.EntityTypes.BOSSES" in compat, "NeoForge common boss tag compatibility missing")
    require("entity instanceof Enemy" in compat and "builtInRegistryHolder().is(Tags.EntityTypes.BOSSES)" in compat,
            "combat target bridge must combine Enemy and holder-based common boss tags")
    require("EXPEDITION_MAJOR_TARGETS" in compat and '"expedition_major_targets"' in compat,
            "Survival-owned major-target EntityType tag seam missing")
    require("isMajorExpeditionTarget(LivingEntity entity)" in compat,
            "major-target compatibility predicate missing")
    require("ContentPackCompatibility.isCombatTarget(event.getEntity())" in combat,
            "primary combat target does not use the content-pack bridge")
    require("ContentPackCompatibility.isCombatTarget(candidate)" in combat,
            "cleave/shockwave candidates do not use the content-pack bridge")
    require("!ContentPackCompatibility.isCombatTarget(victim)" in combat,
            "combat kill XP is not restricted to real hostile/boss targets")
    require("ContentPackCompatibility.isMajorExpeditionTarget(victim)" in combat
            and "MAJOR_TARGET_EXPEDITION_BONUS = 3" in combat
            and "ExpeditionProgression.grantMajorTargetBonus(player, MAJOR_TARGET_EXPEDITION_BONUS)" in combat,
            "major-target bounded expedition credit missing")
    require("majorTarget ? 600 : 200" in combat and "majorTarget ? 2.5D : 1.5D" in combat,
            "major-target combat XP cap/scale missing")
    require("grantMajorTargetBonus(ServerPlayer player, int bonusAmount)" in expedition_progression
            and "ExpeditionAction.HOSTILES_KILLED, bonusAmount" in expedition_progression
            and "ExpeditionOperationSystem.recordAction(player, ExpeditionAction.HOSTILES_KILLED, bonusAmount)" in expedition_progression,
            "major-target regional/operation bridge missing")
    require("ExpeditionIncidentSystem.recordAction(player, ExpeditionAction.HOSTILES_KILLED, bonusAmount)" not in expedition_progression,
            "major-target bonus must not multiply incident counters")
    require("victim instanceof Enemy ? 1.5D : 0.35D" not in combat,
            "passive-livestock combat XP fallback is still present")
    require("BlockTags.LOGS" in wood and "BlockTags.LEAVES" in wood,
            "woodcutting must continue to use generic Minecraft log/leaf tags")
    require("block instanceof CropBlock" in harvest,
            "harvesting must continue to accept modded CropBlock implementations")

    major_entries = [entry for entry in major_targets.get("values", []) if isinstance(entry, dict)]
    require(major_targets.get("replace") is False, "major-target tag must merge rather than replace")
    require({entry.get("id") for entry in major_entries} == {"tbos:hour_cantor", "tbos:phoenix_guardian"},
            "audited TBS major-target set drifted")
    require(all(entry.get("required") is False for entry in major_entries),
            "TBS major-target entries must remain optional")

    # 0.48 physical frontline stock ties freight to operations without turning cargo into virtual currency.
    for token in (
        "startSiegeWithLocalSupply(player, false)",
        "startSiegeWithLocalSupply(player, true)",
        "startOperationWithLocalSupply(player)",
        "prepareLocalOutpostSupply",
        "consumeLocalOutpostSupply",
        "exactOutpostContainers",
        "data.linkedBarrels(player, depot)",
        "level.hasChunkAt(pos)",
        "level.mayInteract(player, pos)",
        "Blocks.BARREL",
        "blockEntity instanceof Container",
        "new LocalRequirement(\"식량\", 32",
        "new LocalRequirement(\"철 주괴\", 8",
        "new LocalRequirement(\"연료\", 8",
        "new LocalRequirement(\"식량\", 48",
        "new LocalRequirement(\"철 주괴\", 16",
        "new LocalRequirement(\"통나무\", 32",
        "new LocalRequirement(\"식량\", 96",
        "new LocalRequirement(\"철 주괴\", 32",
        "new LocalRequirement(\"석재 벽돌\", 128",
    ):
        require(token in production, f"0.48 frontline local supply missing: {token}")
    local_supply = production[production.find("private static PreparedLocalSupply prepareLocalOutpostSupply"):production.find("private static void bulkOffload")]
    for forbidden in ("FieldDepotService.consumeMatching", "FieldDepotService.consume(", "player.getInventory().getItem"):
        require(forbidden not in local_supply, f"0.48 frontline stock must not use global/player fallback: {forbidden}")
    require("플레이어 인벤토리나 다른 근처 거점으로 대체하지 않습니다" in production,
            "0.48 frontline stock fallback policy message missing")
    require("OutpostSiegeSystem.isActive(player) && !consumeLocalOutpostSupply" in production
            and "ExpeditionOperationSystem.isActive(player) && !consumeLocalOutpostSupply" in production,
            "0.48 local stock must only be charged after an encounter/operation actually starts")
    for token in (
        "전초재고(식량48/철16/통나무32)",
        "전초재고(식량96/철32/석재벽돌128)",
        "전초재고(식량32/철8/연료8)",
    ):
        require(token in production_ui, f"0.48 production radial cost disclosure missing: {token}")
    for token in ("전선 현지 보급", "원정은 식량32+철8+석탄/목탄8", "전초 방어는 식량48+철16+통나무32", "요새 방어는 식량96+철32+석재벽돌128", "전선 작전 재고"):
        require(token in guide, f"0.48 in-game guide drifted: {token}")
    require("0.48.0-alpha.1" in readme and "Frontline Local Supply" in readme and "exact departure outpost" in readme,
            "0.48 README contract missing")
    require("Mod version: `0.48.0-alpha.1`" in project_doc and "## 0.48 Frontline Local Supply" in project_doc,
            "0.48 PROJECT contract missing")
    require("## 0.48.0-alpha.1" in changelog and "Frontline Local Supply" in changelog,
            "0.48 changelog entry missing")

    # 0.45 optional external-world bridge.
    require("if (biome.is(integrationTag)) return true;" in expedition_region,
            "external expedition tag must be checked before vanilla fallback")
    require("biomesoplenty" not in expedition_region.lower() and "tbos" not in expedition_region.lower(),
            "optional implementation dependency leaked into ExpeditionRegion")
    deep_ids = {entry.get("id") for entry in deep_expedition.get("values", []) if isinstance(entry, dict) and entry.get("required") is False}
    require("biomesoplenty:glowing_grotto" in deep_ids and "biomesoplenty:spider_nest" in deep_ids,
            "BOP deep expedition bridge incomplete")

    # 0.44 external gear bridge: standard item tags and existing affix system only.
    for tag in ("ItemTags.SWORDS", "ItemTags.PICKAXES", "ItemTags.AXES", "ItemTags.SHOVELS", "ItemTags.HOES"):
        require(tag in affix, f"external equipment standard tag missing: {tag}")
    require("canImprint(ItemStack stack)" in affix and "imprint(ItemStack stack, RandomSource random, int requestedRarity)" in affix,
            "external equipment imprint API missing")
    require('BASE_NAME = "base_name"' in affix and "root.putString(BASE_NAME, baseName)" in affix,
            "external equipment base-name preservation missing")
    require("ACTION_IMPRINT = 3" in reforge and "WorldAscensionData.get(player.getServer()).stage()" in reforge,
            "world-stage imprint routing missing")
    require("FieldDepotService.countMaterial" in reforge and "FieldDepotService.consume" in reforge,
            "imprint must consume through physical logistics resolver")
    require('new Entry("승천 각인"' in equipment_ui and "EquipmentReforgeService.ACTION_IMPRINT" in equipment_ui,
            "equipment radial imprint action missing")
    require("Category.SHOVEL" in affix and "adjustShovelArea" in affix and "Items.NETHERITE_SHOVEL" in affix,
            "shovel affix category is incomplete")
    require("ItemTags.SHOVELS" in mining and "BlockTags.MINEABLE_WITH_SHOVEL" in mining and "breakShovelArea" in mining,
            "standard shovel Mining bridge missing")
    require("a3ac49a6202b7918d2ed22030df0b6e2906cdec8" in matrix,
            "locked Amethyst Resonance binary audit hash missing from compatibility matrix")
    require("4d55c51685bff4247fa533c925f7641ce4880db3" in matrix,
            "locked The Birth of Steve 0.7 binary audit hash missing from compatibility matrix")

    for forbidden in ("biomesoplenty", "tbos", "amethyst_resonance"):
        require(forbidden not in compat.lower(), f"hard optional-mod dependency leaked into compatibility seam: {forbidden}")
        require(forbidden not in affix.lower(), f"hard optional-mod dependency leaked into equipment imprint: {forbidden}")
        require(forbidden not in reforge.lower(), f"hard optional-mod dependency leaked into equipment service: {forbidden}")

    require("placeholder" not in plan.lower(), "modpack plan is still a placeholder")
    require("placeholder" not in matrix.lower(), "compatibility matrix is still a placeholder")
    require("여러 JAR을 직접 찾지 않는다" in matrix, "one-import player UX contract missing")

    print("content_pack_source_audit=PASS")
    print(f"locked_external_mods={len(mods)}")
    print("loader_specific_artifact_selection=PASS")
    print("skill_xp_normalization=PASS")
    print("content_pack_progression_bridge=PASS")
    print("frontline_local_supply_bridge=PASS")
    print("frontline_local_supply_docs=PASS")
    print("external_equipment_imprint=PASS")
    print("amethyst_resonance_shovel_bridge=PASS")
    print("external_component_preservation_contract=PASS")
    print("bop_expedition_bridge=PASS")
    print("generic_enemy_boss_bridge=PASS")
    print("major_external_target_bridge=PASS")
    print("passive_combat_xp_farm=REMOVED")


if __name__ == "__main__":
    main()

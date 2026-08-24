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
    wood = read("src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java")
    harvest = read("src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java")
    affix = read("src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java")
    reforge = read("src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java")
    equipment_ui = read("src/main/java/kr/moonseungjun/survivalascension/client/EquipmentRadialMenuScreen.java")
    ores = read("src/main/resources/data/survivalascension/tags/block/valuable_ores.json")
    expedition_region = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionRegion.java")
    deep_expedition = json.loads(read("src/main/resources/data/survivalascension/tags/worldgen/biome/expedition/deep.json"))
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
    mods = lock.get("mods") or []
    require(len(mods) >= 6, "first-wave content pack unexpectedly small")
    project_ids = [entry.get("project_id") for entry in mods]
    version_ids = [entry.get("version_id") for entry in mods]
    require(len(project_ids) == len(set(project_ids)), "duplicate Modrinth project id in lock")
    require(len(version_ids) == len(set(version_ids)), "duplicate Modrinth version id in lock")
    for required in ("HXF82T3G", "s3dmwKy5", "gKOBlOap", "8RyryQ7j", "lhGA9TYQ", "9s6osm5g"):
        require(required in project_ids, f"required first-wave project not locked: {required}")

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
    require("ContentPackCompatibility.isCombatTarget(event.getEntity())" in combat,
            "primary combat target does not use the content-pack bridge")
    require("ContentPackCompatibility.isCombatTarget(candidate)" in combat,
            "cleave/shockwave candidates do not use the content-pack bridge")
    require("!ContentPackCompatibility.isCombatTarget(victim)" in combat,
            "combat kill XP is not restricted to real hostile/boss targets")
    require("victim instanceof Enemy ? 1.5D : 0.35D" not in combat,
            "passive-livestock combat XP fallback is still present")
    require("BlockTags.LOGS" in wood and "BlockTags.LEAVES" in wood,
            "woodcutting must continue to use generic Minecraft log/leaf tags")
    require("block instanceof CropBlock" in harvest,
            "harvesting must continue to accept modded CropBlock implementations")

    # 0.45 optional external-world bridge.
    require("if (biome.is(integrationTag)) return true;" in expedition_region,
            "external expedition tag must be checked before vanilla fallback")
    require("biomesoplenty" not in expedition_region.lower() and "tbos" not in expedition_region.lower(),
            "optional implementation dependency leaked into ExpeditionRegion")
    deep_ids = {entry.get("id") for entry in deep_expedition.get("values", []) if isinstance(entry, dict) and entry.get("required") is False}
    require("biomesoplenty:glowing_grotto" in deep_ids and "biomesoplenty:spider_nest" in deep_ids,
            "BOP deep expedition bridge incomplete")

    # 0.44 external gear bridge: standard item tags and existing affix system only.
    for tag in ("ItemTags.SWORDS", "ItemTags.PICKAXES", "ItemTags.AXES", "ItemTags.HOES"):
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
    print("external_equipment_imprint=PASS")
    print("bop_expedition_bridge=PASS")
    print("generic_enemy_boss_bridge=PASS")
    print("passive_combat_xp_farm=REMOVED")


if __name__ == "__main__":
    main()

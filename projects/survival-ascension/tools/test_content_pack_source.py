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
    ores = read("src/main/resources/data/survivalascension/tags/block/valuable_ores.json")
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
    require("entity instanceof Enemy || entity.getType().is(Tags.EntityTypes.BOSSES)" in compat,
            "combat target bridge must combine Enemy and common boss tags")
    require("ContentPackCompatibility.isCombatTarget(event.getEntity())" in combat,
            "primary combat target does not use the content-pack bridge")
    require("ContentPackCompatibility.isCombatTarget(candidate)" in combat,
            "cleave/shockwave candidates do not use the content-pack bridge")
    require("!ContentPackCompatibility.isCombatTarget(victim)" in combat,
            "combat kill XP is not restricted to real hostile/boss targets")
    require("victim instanceof Enemy ? 1.5D : 0.35D" not in combat,
            "passive-livestock combat XP fallback is still present")
    require("state.is(BlockTags.LOGS)" in wood and "state.is(BlockTags.LEAVES)" in wood,
            "woodcutting must continue to use generic Minecraft log/leaf tags")
    require("block instanceof CropBlock" in harvest,
            "harvesting must continue to accept modded CropBlock implementations")
    for forbidden in ("biomesoplenty", "tbos", "amethyst_resonance"):
        require(forbidden not in compat.lower(), f"hard optional-mod dependency leaked into compatibility seam: {forbidden}")

    require("placeholder" not in plan.lower(), "modpack plan is still a placeholder")
    require("placeholder" not in matrix.lower(), "compatibility matrix is still a placeholder")
    require("여러 JAR을 직접 찾지 않는다" in matrix, "one-import player UX contract missing")

    print("content_pack_source_audit=PASS")
    print(f"locked_external_mods={len(mods)}")
    print("loader_specific_artifact_selection=PASS")
    print("skill_xp_normalization=PASS")
    print("content_pack_progression_bridge=PASS")
    print("passive_combat_xp_farm=REMOVED")


if __name__ == "__main__":
    main()

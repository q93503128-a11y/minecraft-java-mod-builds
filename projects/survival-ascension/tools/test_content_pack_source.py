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
    require("placeholder" not in plan.lower(), "modpack plan is still a placeholder")
    require("placeholder" not in matrix.lower(), "compatibility matrix is still a placeholder")
    require("여러 JAR을 직접 찾지 않는다" in matrix, "one-import player UX contract missing")

    print("content_pack_source_audit=PASS")
    print(f"locked_external_mods={len(mods)}")
    print("skill_xp_normalization=PASS")


if __name__ == "__main__":
    main()

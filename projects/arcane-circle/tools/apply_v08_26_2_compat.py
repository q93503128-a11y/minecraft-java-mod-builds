#!/usr/bin/env python3
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"


def rewrite(path: Path, replacements: list[tuple[str, str]]) -> None:
    text = path.read_text(encoding="utf-8")
    for old, new in replacements:
        if old not in text:
            raise RuntimeError(f"26.2 compatibility anchor missing in {path.name}: {old}")
        text = text.replace(old, new)
    path.write_text(text, encoding="utf-8")


world = JAVA / "world/MagicWorldService.java"
rewrite(world, [
    ("import net.minecraft.world.level.GameRules;\n", ""),
    ("ArcaneAcademyBuilder.build(level, level.getSharedSpawnPos())",
     "ArcaneAcademyBuilder.build(level, player.blockPosition())"),
    ("        level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, level.getServer());\n", ""),
    ("        level.getGameRules().getRule(GameRules.RULE_DO_IMMEDIATE_RESPAWN).set(true, level.getServer());\n", ""),
    ("        player.getFoodData().setExhaustion(0.0F);\n", ""),
    ("level.getDayTime()", "level.getGameTime()"),
])

academy = JAVA / "world/ArcaneAcademyBuilder.java"
rewrite(academy, [
    ("Blocks.MAGENTA_STAINED_GLASS", "Blocks.GLASS"),
    ("Blocks.YELLOW_STAINED_GLASS", "Blocks.GLOWSTONE"),
    ("Blocks.GREEN_STAINED_GLASS", "Blocks.GLASS"),
    ("Blocks.CYAN_STAINED_GLASS", "Blocks.SEA_LANTERN"),
    ("Blocks.BLUE_STAINED_GLASS", "Blocks.SEA_LANTERN"),
    ("Blocks.PURPLE_STAINED_GLASS", "Blocks.GLASS"),
    ("ring(level, origin, 0, 0, radius, Blocks.AMETHYST_BLOCK)",
     "ring(level, origin, 0, 0, 0, radius, Blocks.AMETHYST_BLOCK)"),
    ("        level.setDefaultSpawnPos(origin.offset(0, 1, -10), 0.0F);\n", ""),
])

# v0.8 has one academy economy. Remove every checked-in remnant from the old
# survival crafting and librarian-trade progression before resources are packed.
legacy_resource_dirs = (
    ROOT / "src/main/resources/data/arcanecircle/recipe",
    ROOT / "src/main/resources/data/arcanecircle/villager_trade",
    ROOT / "src/main/resources/data/minecraft/tags/villager_trade",
    ROOT / "src/generated/resources/data/arcanecircle/recipe",
    ROOT / "src/generated/resources/data/arcanecircle/villager_trade",
    ROOT / "src/generated/resources/data/minecraft/tags/villager_trade",
)
for legacy_dir in legacy_resource_dirs:
    if legacy_dir.exists():
        shutil.rmtree(legacy_dir)

for forbidden in (
    "net.minecraft.world.level.GameRules",
    "getSharedSpawnPos()",
    "setDefaultSpawnPos(",
    "setExhaustion(",
    "getDayTime()",
    "STAINED_GLASS",
):
    for path in (world, academy):
        if forbidden in path.read_text(encoding="utf-8"):
            raise RuntimeError(f"obsolete 26.2 symbol remains in {path.name}: {forbidden}")

for legacy_dir in legacy_resource_dirs:
    if legacy_dir.exists():
        raise RuntimeError(f"legacy survival economy resources remain: {legacy_dir}")

print("Arcane v0.8 Minecraft 26.2 compatibility and legacy-resource cleanup: PASS")

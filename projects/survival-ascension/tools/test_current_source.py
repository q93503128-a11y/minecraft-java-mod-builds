#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]

required = [
    "PROJECT.md",
    "README.md",
    "CHANGELOG.md",
    "build.gradle",
    "gradle.properties",
    "settings.gradle",
    "gradlew",
    "gradle/wrapper/gradle-wrapper.jar",
    "gradle/wrapper/gradle-wrapper.properties",
    "src/main/templates/META-INF/neoforge.mods.toml",
    "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java",
    "src/main/java/kr/moonseungjun/survivalascension/progress/MiningProgressData.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/command/AscensionCommands.java",
    "src/main/resources/assets/survivalascension/lang/ko_kr.json",
    "src/main/resources/data/survivalascension/tags/block/valuable_ores.json",
]

errors = []
for rel in required:
    if not (ROOT / rel).exists():
        errors.append(f"missing: {rel}")

props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
for needle in [
    "minecraft_version=26.2",
    "neo_version=26.2.0.38-beta",
    "mod_id=survivalascension",
    "mod_version=0.1.0-alpha.1",
]:
    if needle not in props:
        errors.append(f"gradle.properties missing {needle}")

build = (ROOT / "build.gradle").read_text(encoding="utf-8")
if "JavaLanguageVersion.of(25)" not in build or "options.release = 25" not in build:
    errors.append("Java 25 toolchain/release contract missing")

mining = (ROOT / "src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java").read_text(encoding="utf-8")
for needle in [
    "if (level >= 60) return 7",
    "if (level >= 30) return 5",
    "if (level >= 10) return 3",
    "player.isShiftKeyDown()",
    "player.gameMode.destroyBlock(target)",
    "Attributes.BLOCK_BREAK_SPEED",
    "level.getBlockEntity(target) != null",
]:
    if needle not in mining:
        errors.append(f"mining contract missing: {needle}")

progress = (ROOT / "src/main/java/kr/moonseungjun/survivalascension/progress/MiningProgressData.java").read_text(encoding="utf-8")
if "MAX_LEVEL = 100" not in progress or "mining_progress_v1" not in progress:
    errors.append("persistent 0-100 mining progression contract missing")

commands = (ROOT / "src/main/java/kr/moonseungjun/survivalascension/command/AscensionCommands.java").read_text(encoding="utf-8")
for needle in ["literal(\"ascension\")", "literal(\"stats\")", "literal(\"setlevel\")", "LEVEL_GAMEMASTERS"]:
    if needle not in commands:
        errors.append(f"command contract missing: {needle}")

for path in (ROOT / "src").rglob("*"):
    if not path.is_file():
        continue
    if path.suffix.lower() not in {".java", ".json", ".toml", ".mcmeta", ".txt"}:
        continue
    text = path.read_text(encoding="utf-8", errors="ignore")
    for forbidden in ["harmonised.pmmo", "Caltinor", "pmmo:"]:
        if forbidden.lower() in text.lower():
            errors.append(f"forbidden external implementation marker in {path.relative_to(ROOT)}: {forbidden}")

if re.search(r"setBlock\s*\([^\n]*AIR", mining):
    errors.append("area mining must not bypass normal player destruction with setBlock(AIR)")

if errors:
    print("SOURCE AUDIT FAILED")
    for error in errors:
        print("-", error)
    sys.exit(1)

print("SOURCE AUDIT PASS")
print("- Minecraft 26.2 / NeoForge 26.2.0.38-beta / Java 25")
print("- persistent mining progression 0-100")
print("- area unlocks 3x3 @10, 5x5 @30, 7x7 @60")
print("- precision sneak mode and normal destroyBlock pipeline")
print("- no Project MMO implementation markers")

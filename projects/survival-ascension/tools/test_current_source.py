#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
required = [
    "PROJECT.md", "README.md", "CHANGELOG.md", "THIRD_PARTY_NOTICES.md",
    "build.gradle", "gradle.properties", "settings.gradle", "gradlew",
    "gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties",
    "src/main/templates/META-INF/neoforge.mods.toml",
    "src/main/resources/META-INF/third-party/SKILL_PROFICIENCIES_MIT.txt",
    "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/SurvivalAscensionClient.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/ClientSkillState.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/SkillHudOverlay.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/SkillsScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/progress/SkillType.java",
    "src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java",
    "src/main/java/kr/moonseungjun/survivalascension/progress/SkillProgressData.java",
    "src/main/java/kr/moonseungjun/survivalascension/progress/SkillProgressionService.java",
    "src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/command/AscensionCommands.java",
    "src/main/resources/assets/survivalascension/lang/ko_kr.json",
]
errors = []
for rel in required:
    if not (ROOT / rel).exists(): errors.append(f"missing: {rel}")

props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
for needle in ["minecraft_version=26.2", "neo_version=26.2.0.38-beta", "mod_id=survivalascension", "mod_version=0.3.0-alpha.1"]:
    if needle not in props: errors.append(f"gradle.properties missing {needle}")

build = (ROOT / "build.gradle").read_text(encoding="utf-8")
if "JavaLanguageVersion.of(25)" not in build or "options.release = 25" not in build:
    errors.append("Java 25 toolchain/release contract missing")

progress = (ROOT / "src/main/java/kr/moonseungjun/survivalascension/progress/SkillProgressData.java").read_text(encoding="utf-8")
for needle in ['Codec.unboundedMap(Codec.STRING, Codec.LONG)', 'optionalFieldOf("mining_xp", 0L)', '"mining_progress_v1"', 'SkillType skill']:
    if needle not in progress: errors.append(f"shared progression/migration contract missing: {needle}")

tuning = (ROOT / "src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java").read_text(encoding="utf-8")
for needle in ["harvestingSpeedMultiplier", "harvestingAreaSize", "masteryTier", "if (level >= 90) return 9"]:
    if needle not in tuning: errors.append(f"harvesting/mastery tuning missing: {needle}")

harvest = (ROOT / "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java").read_text(encoding="utf-8")
for needle in ["CropBlock", "crop.isMaxAge(state)", "NetherWartBlock.MAX_AGE", "XP_PER_HARVEST = 15", "ItemTags.HOES", "harvestingAreaSize", "player.gameMode.destroyBlock(target)", "player.isShiftKeyDown()"]:
    if needle not in harvest: errors.append(f"harvesting contract missing: {needle}")

client = (ROOT / "src/main/java/kr/moonseungjun/survivalascension/client/SurvivalAscensionClient.java").read_text(encoding="utf-8")
for needle in ["InputConstants.KEY_K", "RegisterKeyMappingsEvent", "ClientTickEvent.Pre", "OPEN_SKILLS.consumeClick()", "minecraft.gui.setScreen(new SkillsScreen())"]:
    if needle not in client: errors.append(f"skills key contract missing: {needle}")

screen = (ROOT / "src/main/java/kr/moonseungjun/survivalascension/client/SkillsScreen.java").read_text(encoding="utf-8")
for needle in ["GuiGraphicsExtractor", "SkillType.values()", "ClientSkillState.xp", "masteryTier", "harvestingAreaSize", "graphics.fill", "graphics.text"]:
    if needle not in screen: errors.append(f"skills screen contract missing: {needle}")

commands = (ROOT / "src/main/java/kr/moonseungjun/survivalascension/command/AscensionCommands.java").read_text(encoding="utf-8")
for needle in ['skillSetLevelNode("harvesting", SkillType.HARVESTING)', "SkillTuning.harvestingAreaSize"]:
    if needle not in commands: errors.append(f"harvesting command/stats missing: {needle}")

notices = (ROOT / "THIRD_PARTY_NOTICES.md").read_text(encoding="utf-8")
for needle in ["Skill Proficiencies", "MIT License", "Copyright (c) 2026 balovich-matje", "mature-crop classification", "multi-skill overview-screen", "Project MMO 2.0", "reference-only"]:
    if needle not in notices: errors.append(f"third-party notice missing: {needle}")

for path in (ROOT / "src").rglob("*"):
    if not path.is_file() or path.suffix.lower() not in {".java", ".json", ".toml", ".mcmeta", ".txt"}: continue
    text = path.read_text(encoding="utf-8", errors="ignore")
    for forbidden in ["harmonised.pmmo", "Caltinor", "pmmo:"]:
        if forbidden.lower() in text.lower(): errors.append(f"restricted Project MMO implementation marker in {path.relative_to(ROOT)}: {forbidden}")

for rel in [
    "src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java",
]:
    text = (ROOT / rel).read_text(encoding="utf-8")
    if re.search(r"setBlock\s*\([^\n]*AIR", text): errors.append(f"scaled destruction bypasses normal destroy path: {rel}")

if errors:
    print("SOURCE AUDIT FAILED")
    for error in errors: print("-", error)
    sys.exit(1)

print("SOURCE AUDIT PASS")
print("- Minecraft 26.2 / NeoForge 26.2.0.38-beta / Java 25")
print("- mining + woodcutting regression contracts retained")
print("- harvesting: mature-only XP + hoe speed + 3x3/5x5/7x7/9x9 normal-destroy scaling")
print("- K-key six-skill overview screen + mastery tiers I-V")
print("- Skill Proficiencies MIT notice retained; Project MMO remains reference-only")

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
    "src/main/resources/META-INF/third-party/VEINMINER_PLUS_PLUS_MIT.txt",
    "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/SurvivalAscensionClient.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/SkillsScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java",
    "src/main/java/kr/moonseungjun/survivalascension/progress/SkillProgressData.java",
    "src/main/java/kr/moonseungjun/survivalascension/progress/SkillProgressionService.java",
    "src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/OreVeinMatcher.java",
    "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/command/AscensionCommands.java",
]
errors = []
for rel in required:
    if not (ROOT / rel).exists(): errors.append(f"missing: {rel}")

props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
for needle in ["minecraft_version=26.2", "neo_version=26.2.0.38-beta", "mod_id=survivalascension", "mod_version=0.5.0-alpha.1"]:
    if needle not in props: errors.append(f"gradle.properties missing {needle}")

main = (ROOT / "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java").read_text(encoding="utf-8")
for needle in ['VERSION = "0.5.0-alpha.1"', "CombatProgression::onIncomingDamage", "CombatProgression::onLivingDeath"]:
    if needle not in main: errors.append(f"main combat registration missing: {needle}")

tuning = (ROOT / "src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java").read_text(encoding="utf-8")
for needle in [
    "miningVeinLimit", "if (level >= 90) return 128", "harvestingAreaSize",
    "combatDamageMultiplier", "combatCleaveRadius", "combatCleaveTargetLimit", "combatCleaveFraction",
    "if (level >= 90) return 8", "if (level >= 60) return 4", "if (level >= 30) return 2",
]:
    if needle not in tuning: errors.append(f"progression tuning missing: {needle}")

combat = (ROOT / "src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java").read_text(encoding="utf-8")
for needle in [
    "LivingIncomingDamageEvent", "LivingDeathEvent", "event.getSource().getEntity() instanceof ServerPlayer",
    "event.getSource().getDirectEntity() != player", "candidate instanceof Enemy", "event.setAmount(scaledDamage)",
    "combatCleaveTargetLimit", "combatCleaveFraction", "CLEAVE_GUARD", "candidate.hurtServer",
    "SkillProgressionService.award(player, SkillType.COMBAT", "victim instanceof Enemy ? 1.5D : 0.35D",
]:
    if needle not in combat: errors.append(f"combat contract missing: {needle}")

mining = (ROOT / "src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java").read_text(encoding="utf-8")
for needle in ["centerState.is(VALUABLE_ORES)", "miningVeinLimit", "breakConnectedOre", "OreVeinMatcher.forOrigin", "player.gameMode.destroyBlock(next)"]:
    if needle not in mining: errors.append(f"mining regression missing: {needle}")

screen = (ROOT / "src/main/java/kr/moonseungjun/survivalascension/client/SkillsScreen.java").read_text(encoding="utf-8")
for needle in ["SkillType.COMBAT", "combatDamageMultiplier", "combatCleaveTargetLimit", "combatCleaveRadius", "SkillType.values()"]:
    if needle not in screen: errors.append(f"skills screen combat contract missing: {needle}")

commands = (ROOT / "src/main/java/kr/moonseungjun/survivalascension/command/AscensionCommands.java").read_text(encoding="utf-8")
for needle in ['skillSetLevelNode("combat", SkillType.COMBAT)', "SkillTuning.combatDamageMultiplier", "SkillTuning.combatCleaveTargetLimit"]:
    if needle not in commands: errors.append(f"combat command/stats missing: {needle}")

progress = (ROOT / "src/main/java/kr/moonseungjun/survivalascension/progress/SkillProgressData.java").read_text(encoding="utf-8")
for needle in ['Codec.unboundedMap(Codec.STRING, Codec.LONG)', 'optionalFieldOf("mining_xp", 0L)', '"mining_progress_v1"']:
    if needle not in progress: errors.append(f"save migration contract missing: {needle}")

notices = (ROOT / "THIRD_PARTY_NOTICES.md").read_text(encoding="utf-8")
for needle in ["Skill Proficiencies", "Veinminer++", "Copyright (c) 2026 Kestalkayden", "Project MMO 2.0", "reference-only"]:
    if needle not in notices: errors.append(f"third-party notice missing: {needle}")

for path in (ROOT / "src").rglob("*"):
    if not path.is_file() or path.suffix.lower() not in {".java", ".json", ".toml", ".mcmeta", ".txt"}: continue
    text = path.read_text(encoding="utf-8", errors="ignore")
    for forbidden in ["harmonised.pmmo", "Caltinor", "pmmo:"]:
        if forbidden.lower() in text.lower(): errors.append(f"restricted Project MMO marker in {path.relative_to(ROOT)}: {forbidden}")

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
print("- mining/woodcutting/harvesting regression contracts retained")
print("- combat: kill XP + smooth damage scaling + hostile-only melee cleave 2/4/8")
print("- combat cleave recursion and ranged exclusion guards present")
print("- third-party MIT notices retained; Project MMO remains reference-only")

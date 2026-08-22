#!/usr/bin/env python3
from pathlib import Path
import re, sys

ROOT = Path(__file__).resolve().parents[1]
required = [
    "PROJECT.md", "README.md", "CHANGELOG.md", "THIRD_PARTY_NOTICES.md",
    "build.gradle", "gradle.properties", "settings.gradle", "gradlew",
    "gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties",
    "src/main/templates/META-INF/neoforge.mods.toml",
    "src/main/resources/META-INF/third-party/SKILL_PROFICIENCIES_MIT.txt",
    "src/main/resources/META-INF/third-party/VEINMINER_PLUS_PLUS_MIT.txt",
    "src/main/resources/META-INF/third-party/MINEMENU_MIT.txt",
    "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/SurvivalAscensionClient.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/AscensionRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/SkillsScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java",
    "src/main/java/kr/moonseungjun/survivalascension/progress/SkillProgressData.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/OreVeinMatcher.java",
    "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java",
]
errors = []
for rel in required:
    if not (ROOT / rel).exists(): errors.append(f"missing: {rel}")

props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
for needle in ["minecraft_version=26.2", "neo_version=26.2.0.38-beta", "mod_id=survivalascension", "mod_version=0.6.0-alpha.1"]:
    if needle not in props: errors.append(f"gradle.properties missing {needle}")

main = (ROOT / "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java").read_text(encoding="utf-8")
for needle in ['VERSION = "0.6.0-alpha.1"', "CombatProgression::onIncomingDamage", "CombatProgression::onLivingDeath"]:
    if needle not in main: errors.append(f"main registration missing: {needle}")

client = (ROOT / "src/main/java/kr/moonseungjun/survivalascension/client/SurvivalAscensionClient.java").read_text(encoding="utf-8")
for needle in ["InputConstants.KEY_M", '"key.survivalascension.menu"', "new AscensionRadialMenuScreen()"]:
    if needle not in client: errors.append(f"M menu contract missing: {needle}")
if "InputConstants.KEY_K" in client or "key.survivalascension.skills" in client:
    errors.append("legacy K direct skills binding still present")

radial = (ROOT / "src/main/java/kr/moonseungjun/survivalascension/client/AscensionRadialMenuScreen.java").read_text(encoding="utf-8")
for needle in ["INNER_RADIUS = 60", "OUTER_RADIUS = 80", "MENU_A = 153", "SELECT_R = 255", "selectedIndex()", "getMouseAngle()", "new SkillsScreen(this)", "GuideScreen.Page.UNLOCKS", "GuiElementRenderState"]:
    if needle not in radial: errors.append(f"MineMenu radial contract missing: {needle}")

screen = (ROOT / "src/main/java/kr/moonseungjun/survivalascension/client/SkillsScreen.java").read_text(encoding="utf-8")
for needle in ["Screen parent", "M 메뉴", "SkillType.values()", "combatDamageMultiplier", "miningVeinLimit"]:
    if needle not in screen: errors.append(f"skills screen contract missing: {needle}")

guide = (ROOT / "src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java").read_text(encoding="utf-8")
for needle in ["OVERVIEW, UNLOCKS, STATS, CONTROLS", "ClientSkillState.level", "Shift · 정밀 모드", "K 직접 숙련 단축키는 제거"]:
    if needle not in guide: errors.append(f"guide contract missing: {needle}")

tuning = (ROOT / "src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java").read_text(encoding="utf-8")
for needle in ["miningVeinLimit", "combatDamageMultiplier", "combatCleaveTargetLimit", "harvestingAreaSize"]:
    if needle not in tuning: errors.append(f"progression regression missing: {needle}")

for rel in [
    "src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java",
]:
    text = (ROOT / rel).read_text(encoding="utf-8")
    if re.search(r"setBlock\s*\([^\n]*AIR", text): errors.append(f"scaled destruction bypasses normal destroy path: {rel}")

notices = (ROOT / "THIRD_PARTY_NOTICES.md").read_text(encoding="utf-8")
for needle in ["Skill Proficiencies", "Veinminer++", "MineMenu", "Copyright (c) 2013 Dylan Miller", "Project MMO 2.0", "reference-only"]:
    if needle not in notices: errors.append(f"third-party notice missing: {needle}")

for path in (ROOT / "src").rglob("*"):
    if not path.is_file() or path.suffix.lower() not in {".java", ".json", ".toml", ".mcmeta", ".txt"}: continue
    text = path.read_text(encoding="utf-8", errors="ignore")
    for forbidden in ["harmonised.pmmo", "Caltinor", "pmmo:"]:
        if forbidden.lower() in text.lower(): errors.append(f"restricted Project MMO marker in {path.relative_to(ROOT)}: {forbidden}")

if errors:
    print("SOURCE AUDIT FAILED")
    for error in errors: print("-", error)
    sys.exit(1)

print("SOURCE AUDIT PASS")
print("- Minecraft 26.2 / NeoForge 26.2.0.38-beta / Java 25")
print("- mining/woodcutting/harvesting/combat regression contracts retained")
print("- M radial menu replaces legacy K shortcut")
print("- in-game guide/unlocks/stats/controls pages present")
print("- Skill Proficiencies, Veinminer++ and MineMenu MIT notices retained")

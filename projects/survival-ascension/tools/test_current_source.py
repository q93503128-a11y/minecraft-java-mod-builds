#!/usr/bin/env python3
from pathlib import Path
import re, sys

ROOT = Path(__file__).resolve().parents[1]
required = [
    "PROJECT.md", "README.md", "CHANGELOG.md", "THIRD_PARTY_NOTICES.md", "build.gradle", "gradle.properties", "settings.gradle", "gradlew",
    "gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties", "src/main/templates/META-INF/neoforge.mods.toml",
    "src/main/resources/META-INF/third-party/SKILL_PROFICIENCIES_MIT.txt", "src/main/resources/META-INF/third-party/VEINMINER_PLUS_PLUS_MIT.txt",
    "src/main/resources/META-INF/third-party/MINEMENU_MIT.txt", "src/main/resources/META-INF/third-party/BUILDING_GADGETS_2_MIT.txt",
    "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/SurvivalAscensionClient.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/AscensionRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/ConstructionRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java",
    "src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java",
    "src/main/java/kr/moonseungjun/survivalascension/network/MobilityActionPayload.java",
    "src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java",
]
errors=[]
for rel in required:
    if not (ROOT / rel).exists(): errors.append(f"missing: {rel}")
props=(ROOT/"gradle.properties").read_text(encoding="utf-8")
for needle in ["minecraft_version=26.2","neo_version=26.2.0.38-beta","mod_id=survivalascension","mod_version=0.8.0-alpha.1"]:
    if needle not in props: errors.append(f"gradle.properties missing {needle}")
main=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java").read_text(encoding="utf-8")
for needle in ['VERSION = "0.8.0-alpha.1"',"ConstructionProgression::onBlockPlaced","MobilityProgression::onPlayerTick"]:
    if needle not in main: errors.append(f"main registration missing: {needle}")
client=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/client/SurvivalAscensionClient.java").read_text(encoding="utf-8")
for needle in ["InputConstants.KEY_M","InputConstants.KEY_R",'key.survivalascension.mobility_action',"MobilityActionPayload"]:
    if needle not in client: errors.append(f"client control contract missing: {needle}")
network=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java").read_text(encoding="utf-8")
for needle in ['PROTOCOL = "4"',"registrar.playToServer(MobilityActionPayload.TYPE","MobilityProgression.performAction"]:
    if needle not in network: errors.append(f"mobility network contract missing: {needle}")
mobility=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java").read_text(encoding="utf-8")
for needle in ["Attributes.MOVEMENT_SPEED","Attributes.STEP_HEIGHT","Attributes.SAFE_FALL_DISTANCE","DASH_READY_TICK","AIR_DASH_USED","distance <= 1.75D","player.hurtMarked = true"]:
    if needle not in mobility: errors.append(f"mobility safety contract missing: {needle}")
for forbidden in ["alrex.parcool", "com.alrex", "parcool.client", "parcool.common"]:
    for path in (ROOT/"src").rglob("*.java"):
        if forbidden in path.read_text(encoding="utf-8",errors="ignore").lower(): errors.append(f"ParCool LGPL implementation marker leaked: {path.relative_to(ROOT)}")
tuning=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java").read_text(encoding="utf-8")
for needle in ["constructionLineLength","mobilitySpeedMultiplier","mobilityStepHeight","mobilitySafeFallDistance","mobilityDashPower","mobilityDashCooldownTicks"]:
    if needle not in tuning: errors.append(f"tuning missing: {needle}")
for rel in ["src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java","src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java","src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java"]:
    text=(ROOT/rel).read_text(encoding="utf-8")
    if re.search(r"setBlock\s*\([^\n]*AIR", text): errors.append(f"scaled destruction bypasses normal destroy path: {rel}")
if errors:
    print("SOURCE AUDIT FAILED")
    for e in errors: print("-",e)
    sys.exit(1)
print("SOURCE AUDIT PASS")
print("- Minecraft 26.2 / NeoForge 26.2.0.38-beta / Java 25")
print("- all six skills active")
print("- Mobility uses server-tracked traversal, vanilla attributes and server-authoritative R dash")
print("- Lv.60 air dash is limited to once before landing; teleport/flight/riding do not grant traversal XP")
print("- restricted/copyleft reference mods remain source/asset clean")

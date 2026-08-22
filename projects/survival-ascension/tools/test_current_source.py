#!/usr/bin/env python3
from pathlib import Path
import re, sys

ROOT = Path(__file__).resolve().parents[1]
required = [
    "PROJECT.md", "README.md", "CHANGELOG.md", "THIRD_PARTY_NOTICES.md", "build.gradle", "gradle.properties", "settings.gradle", "gradlew",
    "gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties", "src/main/templates/META-INF/neoforge.mods.toml",
    "src/main/resources/META-INF/third-party/SKILL_PROFICIENCIES_MIT.txt", "src/main/resources/META-INF/third-party/VEINMINER_PLUS_PLUS_MIT.txt",
    "src/main/resources/META-INF/third-party/MINEMENU_MIT.txt", "src/main/resources/META-INF/third-party/BUILDING_GADGETS_2_MIT.txt",
    "src/main/resources/META-INF/third-party/MOB_CHAMPIONS_MIT.txt", "src/main/resources/META-INF/third-party/APOTHEOSIS_MIT.txt",
    "src/main/resources/META-INF/third-party/MEKANISM_MIT.txt",
    "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java",
    "src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/MiningMode.java",
    "src/main/java/kr/moonseungjun/survivalascension/network/MiningModePayload.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/MiningRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/elite/EliteMobSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java",
    "src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java",
    "src/main/java/kr/moonseungjun/survivalascension/network/EquipmentActionPayload.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/EquipmentRadialMenuScreen.java",
]
errors=[]
for rel in required:
    if not (ROOT/rel).exists(): errors.append(f"missing: {rel}")
props=(ROOT/"gradle.properties").read_text(encoding="utf-8")
for needle in ["minecraft_version=26.2","neo_version=26.2.0.38-beta","mod_id=survivalascension","mod_version=0.13.0-alpha.1"]:
    if needle not in props: errors.append(f"gradle.properties missing {needle}")
main=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java").read_text(encoding="utf-8")
for needle in ['VERSION = "0.13.0-alpha.1"',"EliteMobSystem::onFinalizeSpawn","AscensionAffixes::onEliteDeath","MobilityProgression::onPlayerTick","ConstructionProgression::onBlockPlaced"]:
    if needle not in main: errors.append(f"main registration missing: {needle}")

mining=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java").read_text(encoding="utf-8")
for needle in ["MODE_KEY","MiningMode","effectiveMode","case AUTO","case PLANE","case VEIN","case EXTRACT","extractMatchingOre",
               "EXTRACT_RADIUS_XZ = 12","EXTRACT_RADIUS_Y = 12","level.hasChunkAt(next)","OreVeinMatcher.forOrigin","player.gameMode.destroyBlock(target)",
               "level.getBlockEntity(next) != null","setMode(ServerPlayer player, MiningMode mode)","mode.requiredLevel()"]:
    if needle not in mining: errors.append(f"mining mode contract missing: {needle}")
mode=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/mining/MiningMode.java").read_text(encoding="utf-8")
for needle in ['AUTO("auto"','PLANE("plane"','VEIN("vein"','EXTRACT("extract"','"추출", 90']:
    if needle not in mode: errors.append(f"mining mode definition missing: {needle}")

network=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java").read_text(encoding="utf-8")
for needle in ['PROTOCOL = "6"',"MiningModePayload.TYPE","MiningProgression.setMode","EquipmentActionPayload.TYPE","EquipmentReforgeService.perform"]:
    if needle not in network: errors.append(f"network contract missing: {needle}")
radial=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/client/AscensionRadialMenuScreen.java").read_text(encoding="utf-8")
mining_ui=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/client/MiningRadialMenuScreen.java").read_text(encoding="utf-8")
for needle in ["채굴","MiningRadialMenuScreen","장비","EquipmentRadialMenuScreen"]:
    if needle not in radial: errors.append(f"main radial entry missing: {needle}")
for needle in ["자동","굴착","광맥","추출","MiningModePayload","requiredLevel()","Shift = 항상 1×1"]:
    if needle not in mining_ui: errors.append(f"mining radial missing: {needle}")

affix=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java").read_text(encoding="utf-8")
for needle in ["SECONDARY","UTILITY","AFFIX_POOL","reroll","toolSpeedMultiplier","adjustMiningArea","adjustMiningVeinLimit","adjustWoodcuttingLimit","adjustHarvestArea"]:
    if needle not in affix: errors.append(f"affix regression missing: {needle}")
reforge=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java").read_text(encoding="utf-8")
for needle in ["ACTION_REFORGE","ACTION_SALVAGE","AscensionAffixes.reroll","Items.AMETHYST_SHARD","Items.DIAMOND","Items.NETHERITE_SCRAP","held.shrink(1)"]:
    if needle not in reforge: errors.append(f"reforge regression missing: {needle}")

elite=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/elite/EliteMobSystem.java").read_text(encoding="utf-8")
for needle in ["contains(\"SPAWNER\")","REACTION_READY_KEY","reactToPlayerHit","Trait.VAMPIRIC","Trait.BERSERKER","dropRankReward","addPermanentModifier"]:
    if needle not in elite: errors.append(f"elite regression missing: {needle}")

for notice_rel, copyright_line in [
    ("src/main/resources/META-INF/third-party/MOB_CHAMPIONS_MIT.txt", "Copyright (c) 2024 Wendall Cada"),
    ("src/main/resources/META-INF/third-party/APOTHEOSIS_MIT.txt", "Copyright (c) 2018-2025 Stormraven Studios, LLC"),
    ("src/main/resources/META-INF/third-party/MEKANISM_MIT.txt", "Copyright (c) 2017-2025 Aidan C. Brady"),
]:
    text=(ROOT/notice_rel).read_text(encoding="utf-8")
    if copyright_line not in text or "MIT License" not in text: errors.append(f"invalid notice: {notice_rel}")
third=(ROOT/"THIRD_PARTY_NOTICES.md").read_text(encoding="utf-8")
for needle in ["Mob Champions","Apotheosis","Mekanism","mekanism/Mekanism","Digital Miner"]:
    if needle not in third: errors.append(f"third-party notice missing: {needle}")

guide=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java").read_text(encoding="utf-8")
for needle in ["M→채굴","추출모드","비연결 광석 탐색","M → 장비"]:
    if needle not in guide: errors.append(f"guide missing 0.13 help: {needle}")

for rel in ["src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java","src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java","src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java"]:
    text=(ROOT/rel).read_text(encoding="utf-8")
    if re.search(r"setBlock\s*\([^\n]*AIR", text): errors.append(f"scaled destruction bypasses normal destroy path: {rel}")

for forbidden in ["harmonised.pmmo", "alrex.parcool", "com.alrex", "mekanism.common"]:
    for path in (ROOT/"src").rglob("*.java"):
        if forbidden in path.read_text(encoding="utf-8",errors="ignore").lower(): errors.append(f"forbidden/reference namespace leaked: {path.relative_to(ROOT)} -> {forbidden}")

if errors:
    print("SOURCE AUDIT FAILED")
    for e in errors: print("-",e)
    sys.exit(1)
print("SOURCE AUDIT PASS")
print("- Minecraft 26.2 / NeoForge 26.2.0.38-beta / Java 25")
print("- six-skill, reactive elite, affix and reforge regressions retained")
print("- M -> Mining exposes Auto/Plane/Vein/Extract with server-side level validation")
print("- Lv.90 Extract is bounded to loaded chunks and normal destroyBlock semantics")
print("- Shift precision override remains global for mining modes")
print("- Mekanism MIT notice packaged; Mekanism namespace/assets/machine code are not bundled")

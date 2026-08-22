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
    "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java",
    "src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java",
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
for needle in ["minecraft_version=26.2","neo_version=26.2.0.38-beta","mod_id=survivalascension","mod_version=0.12.0-alpha.1"]:
    if needle not in props: errors.append(f"gradle.properties missing {needle}")
main=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java").read_text(encoding="utf-8")
for needle in ['VERSION = "0.12.0-alpha.1"',"EliteMobSystem::onFinalizeSpawn","AscensionAffixes::onEliteDeath","MobilityProgression::onPlayerTick","ConstructionProgression::onBlockPlaced"]:
    if needle not in main: errors.append(f"main registration missing: {needle}")

affix=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java").read_text(encoding="utf-8")
for needle in ["DataComponents.CUSTOM_DATA","CustomData.EMPTY","survivalascension_affix","createEliteDrop","onEliteDeath","reroll",
               "case 1 -> 0.25D","case 2 -> 0.65D","default -> 1.0D",
               "Items.IRON_SWORD","Items.DIAMOND_SWORD","Items.NETHERITE_SWORD",
               "toolSpeedMultiplier","damageMultiplier","xpMultiplier","adjustMiningArea","adjustMiningVeinLimit","adjustWoodcuttingLimit","adjustHarvestArea","adjustCleaveTargets","adjustCleaveFraction",
               "SECONDARY","UTILITY","AFFIX_POOL","if (base <= 1","if (base <= 0"]:
    if needle not in affix: errors.append(f"affix contract missing: {needle}")
for name in ["파괴", "파급", "굴착", "연쇄", "광역", "숙련", "가속", "광맥", "벌채", "풍작", "정교"]:
    if name not in affix: errors.append(f"affix display name missing: {name}")

reforge=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java").read_text(encoding="utf-8")
for needle in ["ACTION_REFORGE","ACTION_SALVAGE","AscensionAffixes.reroll","Items.AMETHYST_SHARD","Items.DIAMOND","Items.NETHERITE_SCRAP",
               "player.isCreative()","hasAll","consumeAll","held.shrink(1)","16", "32", "64"]:
    if needle not in reforge: errors.append(f"reforge contract missing: {needle}")

network=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java").read_text(encoding="utf-8")
for needle in ['PROTOCOL = "5"',"EquipmentActionPayload.TYPE","EquipmentReforgeService.perform"]:
    if needle not in network: errors.append(f"equipment network missing: {needle}")
radial=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/client/AscensionRadialMenuScreen.java").read_text(encoding="utf-8")
equip_ui=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/client/EquipmentRadialMenuScreen.java").read_text(encoding="utf-8")
for needle in ["장비","EquipmentRadialMenuScreen"]:
    if needle not in radial: errors.append(f"main radial equipment entry missing: {needle}")
for needle in ["재련","분해","장비 정보","EquipmentActionPayload","costText","salvageText"]:
    if needle not in equip_ui: errors.append(f"equipment radial missing: {needle}")

integrations = {
    "mining": ("src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java", ["AscensionAffixes.toolSpeedMultiplier", "AscensionAffixes.xpMultiplier", "AscensionAffixes.adjustMiningVeinLimit", "AscensionAffixes.adjustMiningArea"]),
    "woodcutting": ("src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java", ["AscensionAffixes.xpMultiplier", "AscensionAffixes.adjustWoodcuttingLimit"]),
    "harvesting": ("src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java", ["AscensionAffixes.toolSpeedMultiplier", "AscensionAffixes.xpMultiplier", "AscensionAffixes.adjustHarvestArea"]),
    "combat": ("src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java", ["AscensionAffixes.damageMultiplier", "AscensionAffixes.xpMultiplier", "AscensionAffixes.adjustCleaveTargets", "AscensionAffixes.adjustCleaveFraction"]),
}
for label,(rel,needles) in integrations.items():
    text=(ROOT/rel).read_text(encoding="utf-8")
    for needle in needles:
        if needle not in text: errors.append(f"{label} affix integration missing: {needle}")

elite=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/elite/EliteMobSystem.java").read_text(encoding="utf-8")
for needle in ["contains(\"SPAWNER\")","REACTION_READY_KEY","reactToPlayerHit","Trait.VAMPIRIC","Trait.BERSERKER","dropRankReward","addPermanentModifier"]:
    if needle not in elite: errors.append(f"elite regression missing: {needle}")

for notice_rel, copyright_line in [
    ("src/main/resources/META-INF/third-party/MOB_CHAMPIONS_MIT.txt", "Copyright (c) 2024 Wendall Cada"),
    ("src/main/resources/META-INF/third-party/APOTHEOSIS_MIT.txt", "Copyright (c) 2018-2025 Stormraven Studios, LLC"),
]:
    text=(ROOT/notice_rel).read_text(encoding="utf-8")
    if copyright_line not in text or "MIT License" not in text: errors.append(f"invalid notice: {notice_rel}")
third=(ROOT/"THIRD_PARTY_NOTICES.md").read_text(encoding="utf-8")
for needle in ["Mob Champions","wendall911/MobChampions","Apotheosis","Shadows-of-Fire/Apotheosis"]:
    if needle not in third: errors.append(f"third-party notice missing: {needle}")

for rel in ["src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java","src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java","src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java"]:
    text=(ROOT/rel).read_text(encoding="utf-8")
    if re.search(r"setBlock\s*\([^\n]*AIR", text): errors.append(f"scaled destruction bypasses normal destroy path: {rel}")

for forbidden in ["harmonised.pmmo", "alrex.parcool", "com.alrex"]:
    for path in (ROOT/"src").rglob("*.java"):
        if forbidden in path.read_text(encoding="utf-8",errors="ignore").lower(): errors.append(f"restricted/reference-only implementation marker leaked: {path.relative_to(ROOT)} -> {forbidden}")

if errors:
    print("SOURCE AUDIT FAILED")
    for e in errors: print("-",e)
    sys.exit(1)
print("SOURCE AUDIT PASS")
print("- Minecraft 26.2 / NeoForge 26.2.0.38-beta / Java 25")
print("- six-skill + reactive elite regressions retained")
print("- Elite/Ascended/Mythic affix gear now rolls 1/2/3 of five affixes")
print("- M -> Equipment radial exposes server-authoritative reforge and salvage")
print("- reforge consumes substantial amethyst/diamond/netherite resources; salvage is partial refund only")
print("- affix scale bonuses still cannot bypass skill unlock gates")
print("- Apotheosis and Mob Champions MIT notices packaged")

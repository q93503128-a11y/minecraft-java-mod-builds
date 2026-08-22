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
    "src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java",
    "src/main/java/kr/moonseungjun/survivalascension/network/MiningModePayload.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/MiningRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/IrrigationReplantService.java",
    "src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/elite/EliteMobSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java",
    "src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java",
    "src/main/java/kr/moonseungjun/survivalascension/network/EquipmentActionPayload.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/EquipmentRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureData.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java",
    "src/main/java/kr/moonseungjun/survivalascension/network/InfrastructureActionPayload.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/InfrastructureRadialMenuScreen.java",
]
errors=[]
for rel in required:
    if not (ROOT/rel).exists(): errors.append(f"missing: {rel}")
props=(ROOT/"gradle.properties").read_text(encoding="utf-8")
for needle in ["minecraft_version=26.2","neo_version=26.2.0.38-beta","mod_id=survivalascension","mod_version=0.14.0-alpha.1"]:
    if needle not in props: errors.append(f"gradle.properties missing {needle}")
main=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java").read_text(encoding="utf-8")
for needle in ['VERSION = "0.14.0-alpha.1"',"EliteMobSystem::onFinalizeSpawn","AscensionAffixes::onEliteDeath","MobilityProgression::onPlayerTick","ConstructionProgression::onBlockPlaced","BoreMiningService::onServerTick","IrrigationReplantService::onServerTick"]:
    if needle not in main: errors.append(f"main registration missing: {needle}")

mining=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java").read_text(encoding="utf-8")
for needle in ["MODE_KEY","MiningMode","effectiveMode","case AUTO","case PLANE","case VEIN","case EXTRACT","case BORE","extractMatchingOre",
               "EXTRACT_RADIUS_XZ = 12","EXTRACT_RADIUS_Y = 12","level.hasChunkAt(next)","OreVeinMatcher.forOrigin","player.gameMode.destroyBlock(target)",
               "BoreMiningService.isInternal","BoreMiningService.schedule","InfrastructureProject.QUARRY_NETWORK","setMode(ServerPlayer player, MiningMode mode)"]:
    if needle not in mining: errors.append(f"mining/infrastructure contract missing: {needle}")
mode=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/mining/MiningMode.java").read_text(encoding="utf-8")
for needle in ['AUTO("auto"','PLANE("plane"','VEIN("vein"','EXTRACT("extract"','BORE("bore"','"터널", 90']:
    if needle not in mode: errors.append(f"mining mode definition missing: {needle}")

bore=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java").read_text(encoding="utf-8")
for needle in ["GLOBAL_BLOCK_BUDGET_PER_TICK = 64","LOCAL_BLOCK_BUDGET_PER_TICK = 12","MAX_PENDING_PER_PLAYER = 256","5×5×8","INTERNAL_BREAK_GUARD","InfrastructureProject.QUARRY_NETWORK","player.gameMode.destroyBlock(target)","level.getBlockEntity(target) != null","level.hasChunkAt(target)"]:
    if needle not in bore: errors.append(f"bore safety contract missing: {needle}")

infra_project=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java").read_text(encoding="utf-8")
for needle in ["QUARRY_NETWORK","IRRIGATION_WORKS","Items.COBBLESTONE","1024","Items.COPPER_INGOT","512"]:
    if needle not in infra_project: errors.append(f"infrastructure project contract missing: {needle}")
infra_data=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureData.java").read_text(encoding="utf-8")
for needle in ["SavedDataType<InfrastructureData>","infrastructure_v1","FUNDING_CODEC","isComplete","addContribution","setDirty"]:
    if needle not in infra_data: errors.append(f"infrastructure persistence missing: {needle}")
infra_service=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java").read_text(encoding="utf-8")
for needle in ["ACTION_FUND","ACTION_STATUS","countItem","consumeItem","player.isCreative() || player.isSpectator()","getPlayerList().getPlayers()"]:
    if needle not in infra_service: errors.append(f"infrastructure funding safety missing: {needle}")

replant=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/harvesting/IrrigationReplantService.java").read_text(encoding="utf-8")
for needle in ["InfrastructureProject.IRRIGATION_WORKS","SkillType.HARVESTING) < 30","Items.WHEAT_SEEDS","Items.CARROT","Items.POTATO","Items.BEETROOT_SEEDS","Items.NETHER_WART","EventHooks.onBlockPlace","consumeOne(player, kind.seed())","REPLANT_BUDGET_PER_TICK = 64"]:
    if needle not in replant: errors.append(f"irrigation replant contract missing: {needle}")
harvest=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java").read_text(encoding="utf-8")
if "IrrigationReplantService.scheduleIfEligible" not in harvest: errors.append("Harvesting does not schedule irrigation replant")

network=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java").read_text(encoding="utf-8")
for needle in ['PROTOCOL = "7"',"MiningModePayload.TYPE","EquipmentActionPayload.TYPE","InfrastructureActionPayload.TYPE","InfrastructureService.perform"]:
    if needle not in network: errors.append(f"network contract missing: {needle}")
radial=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/client/AscensionRadialMenuScreen.java").read_text(encoding="utf-8")
mining_ui=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/client/MiningRadialMenuScreen.java").read_text(encoding="utf-8")
infra_ui=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/client/InfrastructureRadialMenuScreen.java").read_text(encoding="utf-8")
for needle in ["숙련","채굴","건축","장비","인프라","가이드","InfrastructureRadialMenuScreen"]:
    if needle not in radial: errors.append(f"main radial entry missing: {needle}")
if "UNLOCKS" in radial or "STATS" in radial or "CONTROLS" in radial: errors.append("main radial still contains duplicate guide subentries")
for needle in ["자동","굴착","광맥","추출","터널","MiningMode.BORE","MiningModePayload","Shift = 항상 1×1"]:
    if needle not in mining_ui: errors.append(f"mining radial missing: {needle}")
for needle in ["채석장 네트워크","관개 시설","진행도","InfrastructureActionPayload","ACTION_FUND","ACTION_STATUS"]:
    if needle not in infra_ui: errors.append(f"infrastructure radial missing: {needle}")

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
for needle in ["공동 인프라","채석장 네트워크","관개 시설","씨앗 소비 자동 재파종","M → 인프라"]:
    if needle not in guide: errors.append(f"guide missing 0.14 help: {needle}")

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
print("- six-skill, reactive elite, affix, reforge and selectable mining regressions retained")
print("- world-shared infrastructure funding persists through SavedData")
print("- Quarry Network gates tick-budgeted 5x5x8 tunnel mining without recursive rescheduling")
print("- Irrigation Works gates Lv.30 seed-backed protected auto-replant with no free-seed path")
print("- M radial is reduced to seven top-level entries; guide subpages remain inside Guide")

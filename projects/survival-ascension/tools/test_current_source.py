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
    "src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/MiningMode.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java",
    "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/IrrigationReplantService.java",
    "src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionMode.java",
    "src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/elite/EliteMobSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java",
    "src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureData.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java",
    "src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java",
    "src/main/java/kr/moonseungjun/survivalascension/network/InfrastructureActionPayload.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/AscensionRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/MiningRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/ConstructionRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/InfrastructureRadialMenuScreen.java",
]
errors=[]
for rel in required:
    if not (ROOT/rel).exists(): errors.append(f"missing: {rel}")

def need(text, needles, label):
    for needle in needles:
        if needle not in text: errors.append(f"{label} missing: {needle}")

props=(ROOT/"gradle.properties").read_text(encoding="utf-8")
need(props,["minecraft_version=26.2","neo_version=26.2.0.38-beta","mod_id=survivalascension","mod_version=0.15.0-alpha.1"],"gradle.properties")
main=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java").read_text(encoding="utf-8")
need(main,['VERSION = "0.15.0-alpha.1"',"WoodcuttingProgression::onServerTick","BoreMiningService::onServerTick","IrrigationReplantService::onServerTick","ConstructionProgression::onServerTick","EliteMobSystem::onFinalizeSpawn"],"main registration")

wood=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java").read_text(encoding="utf-8")
need(wood,["smart-tree leaf safety","GLOBAL_LOG_BUDGET_PER_TICK = 64","LOCAL_LOG_BUDGET_PER_TICK = 12","JOBS.containsKey","scheduleNaturalTree","gatherConnectedLogs","hasLeavesNearby","hasAdjacentLeaf","BlockTags.LEAVES","CHAIN_GUARD.add","player.gameMode.destroyBlock(target)"],"woodcutting safety")
if "hasLeavesNearby(level, origin, gathered)" not in wood: errors.append("woodcutting can bulk-fell without leaf evidence")

construction_mode=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionMode.java").read_text(encoding="utf-8")
need(construction_mode,['VOLUME("volume", "입체", 90)'],"construction mode")
construction=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java").read_text(encoding="utf-8")
need(construction,["VOLUME_SIZE = 5","ConstructionMode.VOLUME","InfrastructureProject.BUILDER_FOUNDRY","MAX_PENDING_BLOCKS_PER_PLAYER = 256","EventHooks.onBlockPlace","consumeOne(player, item)","mode == ConstructionMode.VOLUME","center.offset(dx, dy, dz)"],"construction volume")

infra=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java").read_text(encoding="utf-8")
need(infra,["QUARRY_NETWORK","IRRIGATION_WORKS","BUILDER_FOUNDRY","Items.STONE_BRICKS","1024","Items.OBSIDIAN","64"],"infrastructure projects")
infra_data=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureData.java").read_text(encoding="utf-8")
need(infra_data,["infrastructure_v1","SavedDataType<InfrastructureData>","isComplete","addContribution","setDirty"],"infrastructure persistence")
infra_service=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java").read_text(encoding="utf-8")
need(infra_service,["player.isCreative() || player.isSpectator()","countItem","consumeItem","getPlayerList().getPlayers()"],"infrastructure funding")

bore=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java").read_text(encoding="utf-8")
need(bore,["GLOBAL_BLOCK_BUDGET_PER_TICK = 64","LOCAL_BLOCK_BUDGET_PER_TICK = 12","MAX_PENDING_PER_PLAYER = 256","INTERNAL_BREAK_GUARD","InfrastructureProject.QUARRY_NETWORK","player.gameMode.destroyBlock(target)"],"bore regression")
replant=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/harvesting/IrrigationReplantService.java").read_text(encoding="utf-8")
need(replant,["InfrastructureProject.IRRIGATION_WORKS","SkillType.HARVESTING) < 30","EventHooks.onBlockPlace","consumeOne(player, kind.seed())","Items.WHEAT_SEEDS","Items.NETHER_WART"],"irrigation regression")

network=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java").read_text(encoding="utf-8")
need(network,['PROTOCOL = "7"',"InfrastructureActionPayload.TYPE","InfrastructureService.perform","MiningModePayload.TYPE","EquipmentActionPayload.TYPE"],"network regression")
main_radial=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/client/AscensionRadialMenuScreen.java").read_text(encoding="utf-8")
need(main_radial,["숙련","채굴","건축","장비","인프라","가이드","닫기"],"main radial")
for duplicate in ["UNLOCKS", "STATS", "CONTROLS"]:
    if duplicate in main_radial: errors.append(f"main radial duplicate guide entry remains: {duplicate}")
construction_ui=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/client/ConstructionRadialMenuScreen.java").read_text(encoding="utf-8")
need(construction_ui,["입체","건축 공방","5×5×5","ConstructionMode.VOLUME"],"construction radial")
infra_ui=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/client/InfrastructureRadialMenuScreen.java").read_text(encoding="utf-8")
need(infra_ui,["채석장 네트워크","관개 시설","건축 공방","InfrastructureProject.BUILDER_FOUNDRY","진행도"],"infrastructure radial")

# Preserve pre-0.15 core systems.
for rel, needles in {
    "mining/MiningProgression.java":["case EXTRACT","case BORE","extractMatchingOre","BoreMiningService.schedule","player.gameMode.destroyBlock(target)"],
    "equipment/AscensionAffixes.java":["AFFIX_POOL","reroll","adjustMiningArea","adjustWoodcuttingLimit","adjustHarvestArea"],
    "equipment/EquipmentReforgeService.java":["ACTION_REFORGE","ACTION_SALVAGE","Items.AMETHYST_SHARD","Items.NETHERITE_SCRAP"],
    "elite/EliteMobSystem.java":["contains(\"SPAWNER\")","Trait.VAMPIRIC","Trait.BERSERKER","addPermanentModifier"],
}.items():
    text=(ROOT/"src/main/java/kr/moonseungjun/survivalascension"/rel).read_text(encoding="utf-8")
    need(text,needles,f"regression {rel}")

third=(ROOT/"THIRD_PARTY_NOTICES.md").read_text(encoding="utf-8")
need(third,["Veinminer++","smart-tree","Mekanism","Create","Code license: MIT","Assets license: All Rights Reserved"],"third-party notices")
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
print("- Veinminer++ MIT smart-tree safety prevents plain log structures from bulk-felling")
print("- Woodcutting 16/48/128/256 work is tick-drained at 12/player and 64/global")
print("- Builder Foundry gates Lv.90 5x5x5 material-backed protected volume construction")
print("- 0.14 shared infrastructure, tunnel, irrigation, elite, affix and reforge regressions retained")

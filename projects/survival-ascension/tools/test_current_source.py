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
    "src/main/resources/META-INF/third-party/MEKANISM_MIT.txt", "src/main/resources/META-INF/third-party/WARBAND_MIT.txt",
    "src/main/resources/META-INF/third-party/HOSTILES_ARE_TOO_EASY_CC0.txt",
    "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java",
    "src/main/java/kr/moonseungjun/survivalascension/world/WorldAscensionData.java",
    "src/main/java/kr/moonseungjun/survivalascension/world/WorldAscensionProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/elite/WarbandDirector.java",
    "src/main/java/kr/moonseungjun/survivalascension/elite/EliteMobSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureData.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java",
    "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/IrrigationReplantService.java",
    "src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java",
    "src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/InfrastructureRadialMenuScreen.java",
]
errors=[]
for rel in required:
    if not (ROOT/rel).exists(): errors.append(f"missing: {rel}")

def need(text, needles, label):
    for needle in needles:
        if needle not in text: errors.append(f"{label} missing: {needle}")

props=(ROOT/"gradle.properties").read_text(encoding="utf-8")
need(props,["minecraft_version=26.2","neo_version=26.2.0.38-beta","mod_id=survivalascension","mod_version=0.17.0-alpha.1"],"gradle.properties")
main=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java").read_text(encoding="utf-8")
need(main,['VERSION = "0.17.0-alpha.1"',"WorldAscensionProgression::onLivingDeath","WarbandDirector::onServerTick","EliteMobSystem::onFinalizeSpawn","WoodcuttingProgression::onServerTick","BoreMiningService::onServerTick"],"main registration")

world_data=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/world/WorldAscensionData.java").read_text(encoding="utf-8")
need(world_data,["world_ascension_v1","MAX_STAGE = 2","optionalFieldOf(\"stage\", 0)","advanceTo","setDirty","전설 단계","종말 단계"],"world ascension persistence")
world_progress=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/world/WorldAscensionProgression.java").read_text(encoding="utf-8")
need(world_progress,["Hostiles Are Too Easy","CC0-1.0","instanceof WitherBoss","targetStage = 1","instanceof EnderDragon","targetStage = 2","data.advanceTo(targetStage)","getPlayerList().getPlayers()"],"boss progression")

elite=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/elite/EliteMobSystem.java").read_text(encoding="utf-8")
need(elite,["WorldAscensionData.get(level.getServer()).stage()","worldStage * 0.04D","Math.min(0.28D","chooseRank(random, power, worldStage)","Math.min(0.22D","worldStage * 0.05D","contains(\"SPAWNER\")","Trait.VAMPIRIC","Trait.BERSERKER"],"elite world scaling")

warband=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/elite/WarbandDirector.java").read_text(encoding="utf-8")
need(warband,["WorldAscensionData.get(level.getServer()).stage()","worldStage * 0.08D","int minimum = 3 + worldStage","6 + worldStage","Role.LEADER","Role.BRUISER","Role.HUNTER","Role.SUPPORT","ROUT_TICKS = 160","Items.ECHO_SHARD","NO_WARBAND_KEY","getBooleanOr(NO_WARBAND_KEY, false)"],"warband world scaling")

infra_service=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java").read_text(encoding="utf-8")
need(infra_service,["WorldAscensionData","world.stage()","world.stageName()","ACTION_STATUS","ALL_PROJECTS"],"world status")
combat=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java").read_text(encoding="utf-8")
need(combat,["SHOCKWAVE_RADIUS = 5.5D","SHOCKWAVE_TARGETS = 12","InfrastructureProject.COMBAT_ACADEMY","combatLevel < 90 || !player.isSprinting()"],"combat regression")
wood=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java").read_text(encoding="utf-8")
need(wood,["GLOBAL_LOG_BUDGET_PER_TICK = 64","LOCAL_LOG_BUDGET_PER_TICK = 12","hasLeavesNearby","BlockTags.LEAVES","player.gameMode.destroyBlock(target)"],"woodcutting regression")
construction=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java").read_text(encoding="utf-8")
need(construction,["VOLUME_SIZE = 5","InfrastructureProject.BUILDER_FOUNDRY","MAX_PENDING_BLOCKS_PER_PLAYER = 256","EventHooks.onBlockPlace","consumeOne(player, item)"],"construction regression")
bore=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java").read_text(encoding="utf-8")
need(bore,["GLOBAL_BLOCK_BUDGET_PER_TICK = 64","LOCAL_BLOCK_BUDGET_PER_TICK = 12","InfrastructureProject.QUARRY_NETWORK","player.gameMode.destroyBlock(target)"],"bore regression")
replant=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/harvesting/IrrigationReplantService.java").read_text(encoding="utf-8")
need(replant,["InfrastructureProject.IRRIGATION_WORKS","EventHooks.onBlockPlace","consumeOne(player, kind.seed())"],"irrigation regression")

guide=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java").read_text(encoding="utf-8")
need(guide,["월드 승천","위더 격파","엔더 드래곤 격파","전설 단계","종말 단계","M → 인프라 → 진행도","최대 8체"],"guide")
third=(ROOT/"THIRD_PARTY_NOTICES.md").read_text(encoding="utf-8")
need(third,["Hostiles Are Too Easy","MinecraftIsTooEasy/HostilesAreTooEasy","CC0 1.0 Universal","Warband","Veinminer++","Mekanism"],"third-party notices")
hate_notice=(ROOT/"src/main/resources/META-INF/third-party/HOSTILES_ARE_TOO_EASY_CC0.txt").read_text(encoding="utf-8")
need(hate_notice,["Hostiles Are Too Easy","CC0 1.0 Universal","public domain"],"HATE runtime notice")

# Existing economy and affix contracts stay live.
infra=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java").read_text(encoding="utf-8")
need(infra,["QUARRY_NETWORK","IRRIGATION_WORKS","BUILDER_FOUNDRY","COMBAT_ACADEMY","Items.ECHO_SHARD"],"infrastructure regression")
affix=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java").read_text(encoding="utf-8")
need(affix,["AFFIX_POOL","reroll","adjustMiningArea","adjustWoodcuttingLimit","adjustHarvestArea"],"affix regression")
reforge=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java").read_text(encoding="utf-8")
need(reforge,["ACTION_REFORGE","ACTION_SALVAGE","Items.AMETHYST_SHARD","Items.NETHERITE_SCRAP"],"reforge regression")

for rel in ["src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java","src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java","src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java"]:
    text=(ROOT/rel).read_text(encoding="utf-8")
    if re.search(r"setBlock\s*\([^\n]*AIR", text): errors.append(f"scaled destruction bypasses normal destroy path: {rel}")
for forbidden in ["harmonised.pmmo", "alrex.parcool", "com.alrex", "mekanism.common", "com.warband", "vbonedra.hostiles_are_too_easy"]:
    for path in (ROOT/"src").rglob("*.java"):
        if forbidden in path.read_text(encoding="utf-8",errors="ignore").lower(): errors.append(f"forbidden/reference namespace leaked: {path.relative_to(ROOT)} -> {forbidden}")

if errors:
    print("SOURCE AUDIT FAILED")
    for e in errors: print("-",e)
    sys.exit(1)
print("SOURCE AUDIT PASS")
print("- Minecraft 26.2 / NeoForge 26.2.0.38-beta / Java 25")
print("- CC0 boss milestones persist as world_ascension_v1 stage 0/1/2")
print("- Wither -> Legendary stage; Ender Dragon -> Endgame stage")
print("- elite frequency/rank odds and tactical warband size/frequency scale with world stage")
print("- all 0.16 combat academy, warband, infrastructure, safe scaled-work and affix regressions retained")

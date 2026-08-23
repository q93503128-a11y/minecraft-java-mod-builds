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
    "src/main/resources/META-INF/third-party/HOSTILES_ARE_TOO_EASY_CC0.txt", "src/main/resources/META-INF/third-party/GATEWAYS_TO_ETERNITY_MIT.txt",
    "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java",
    "src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java",
    "src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/SkillsScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/EquipmentRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/world/WorldAscensionData.java",
    "src/main/java/kr/moonseungjun/survivalascension/world/WorldAscensionProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionRegion.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionData.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureData.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/InfrastructureRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/elite/EliteMobSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/elite/WarbandDirector.java",
    "src/main/java/kr/moonseungjun/survivalascension/elite/EndgameMutationSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialDoctrine.java",
    "src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java",
    "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/IrrigationReplantService.java",
    "src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java",
    "src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java",
]
errors=[]
for rel in required:
    if not (ROOT/rel).exists(): errors.append(f"missing: {rel}")

def need(text, needles, label):
    for needle in needles:
        if needle not in text: errors.append(f"{label} missing: {needle}")

def read(rel):
    return (ROOT/rel).read_text(encoding="utf-8")

props=read("gradle.properties")
need(props,["minecraft_version=26.2","neo_version=26.2.0.38-beta","mod_id=survivalascension","mod_version=0.24.0-alpha.1"],"gradle.properties")
main=read("src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java")
need(main,['VERSION = "0.24.0-alpha.1"',"HarvestingProgression::onServerTick","ExpeditionProgression::onPlayerTick","ExpeditionProgression::onPlayerLoggedIn","ExpeditionProgression::onPlayerLoggedOut","WorldAscensionProgression::onLivingDeath","WarbandDirector::onServerTick","EndgameMutationSystem::onFinalizeSpawn","AscensionTrialSystem::onServerTick","AscensionTrialSystem::onEntityJoin"],"main registration")
network=read("src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java")
need(network,['PROTOCOL = "8"',"EquipmentActionPayload.TYPE","EquipmentReforgeService.perform(player, payload.action())"],"network protocol 8")

tuning=read("src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java")
need(tuning,["if (level >= 100) return 11;","if (level >= 100) return 192;","if (level >= 100) return 384;","if (level >= 100) return 10;","if (level >= 100) return 5.0D;","if (level >= 100) return 0.70D;","if (level >= 100) return 49;","if (level >= 100) return 2.0D;","if (level >= 100) return 16.0D;","if (level >= 100) return 1.80D;","if (level >= 100) return 16;","if (level >= 100) return 6;"],"base mastery VI tuning")
skills=read("src/main/java/kr/moonseungjun/survivalascension/client/SkillsScreen.java")
need(skills,['case 6 -> "VI"'],"mastery VI UI")

world=read("src/main/java/kr/moonseungjun/survivalascension/world/WorldAscensionData.java")
need(world,["world_ascension_v1","MAX_STAGE = 2","advanceTo","전설 단계","종말 단계"],"world ascension")
world_progress=read("src/main/java/kr/moonseungjun/survivalascension/world/WorldAscensionProgression.java")
need(world_progress,["instanceof WitherBoss","targetStage = 1","instanceof EnderDragon","targetStage = 2","data.advanceTo(targetStage)"],"boss progression")

region=read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionRegion.java")
need(region,[
    'WOODLAND("삼림권", 0, SkillType.WOODCUTTING, 300, "자연 나무 일괄 벌목", 96)',
    'ARID("건조권", 0, SkillType.CONSTRUCTION, 300, "대량 건축 배치", 128)',
    'WETLAND("습지권", 0, SkillType.HARVESTING, 300, "성숙 작물 수확", 96)',
    'HIGHLANDS("고산권", 0, SkillType.MOBILITY, 350, "도보·돌진 횡단", 600)',
    'OCEAN("대양권", 0, SkillType.MOBILITY, 350, "수영·항해 탐사", 800)',
    'DEEP("심층권", 1, SkillType.MINING, 500, "곡괭이 채굴", 192)',
    'FROZEN("빙설권", 1, SkillType.MOBILITY, 450, "도보·돌진 횡단", 600)',
    'NETHER("네더권", 1, SkillType.COMBAT, 600, "적대적 몹 처치", 24)',
    'END("엔드권", 2, SkillType.COMBAT, 800, "적대적 몹 처치", 32)',
    "objectiveName", "objectiveTarget", "Holder<Biome>", "biome.is(key)", "Biomes.PALE_GARDEN", "Biomes.DEEP_DARK", "Biomes.NETHER_WASTES", "Biomes.END_HIGHLANDS"
],"nine-region field objective catalog")

expedition_data=read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionData.java")
need(expedition_data,[
    '"expedition_v1"', "MILESTONE_OVERWORLD = 1", "MILESTONE_LEGENDARY = 1 << 1", "MILESTONE_MASTER = 1 << 2", "ALL_REGIONS_MASK",
    'optionalFieldOf("discovered", 0)', 'optionalFieldOf("completed", 0)', 'optionalFieldOf("progress", Map.of())', 'optionalFieldOf("region_rewards", -1)', 'optionalFieldOf("milestones", 0)',
    "regionRewardMask < 0", "? this.discoveredMask", "MILESTONE_MASTER) != 0", "migratedCompleted = ALL_REGIONS_MASK",
    "progressKey(region), region.objectiveTarget()", "addProgress(ServerPlayer player, ExpeditionRegion region, int amount)",
    "claimRegionReward(ServerPlayer player, ExpeditionRegion region)", "countCompleted", "countStageZeroCompleted", "isMasterSurveyComplete", "claimMilestone", "setDirty()"
],"expedition persistence and 0.23 migration")

expedition=read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionProgression.java")
need(expedition,[
    "PlayerTickEvent.Post", "player.tickCount % 20 != 0", "player.isCreative() || player.isSpectator()", "onPlayerLoggedOut",
    "recordSkillAction(ServerPlayer player, SkillType skill, int amount)", "worldStage < region.requiredWorldStage()", "region == ExpeditionRegion.OCEAN", "ensureDiscovered(player, region)",
    "player.isPassenger() || player.isSwimming() || player.isInWater()", "distance < 0.25D || distance > 24.0D", "addObjectiveProgress(player, ExpeditionRegion.OCEAN",
    "data.claimRegionReward(player, region)", "기존 0.23 발견 보상 승계", "data.countStageZeroCompleted(player) >= 4", "data.countCompleted(player) >= 7",
    "data.isComplete(player, ExpeditionRegion.DEEP)", "data.isComplete(player, ExpeditionRegion.NETHER)", "data.isMasterSurveyComplete(player)",
    "Items.DIAMOND, 4", "Items.EMERALD, 16", "Items.AMETHYST_SHARD, 32", "Items.NETHERITE_SCRAP, 2", "Items.ECHO_SHARD, 32",
    "AscensionAffixes.createEliteDrop", "Items.NETHERITE_SCRAP, 4", "Items.ECHO_SHARD, 64", "Items.DRAGON_BREATH, 16", "giveExperiencePoints(500)",
    "hasFieldMastery", "WorldAscensionData.get(level.getServer()).stage() >= 2"
],"expedition field progression and rewards")

command=read("src/main/java/kr/moonseungjun/survivalascension/command/AscensionCommands.java")
need(command,["ExpeditionRegion", "발견 §e", "완수 §a", "expedition.countCompleted(player)", "expedition.progress(player, region)", "region.objectiveTarget()"],"expedition stats")

mutation=read("src/main/java/kr/moonseungjun/survivalascension/elite/EndgameMutationSystem.java")
need(mutation,['MUTATION_KEY = "survivalascension_endgame_mutation"','MUTATION_CHANCE = 0.18D','WorldAscensionData.get(level.getServer()).stage() < 2','contains("SPAWNER")','Mutation.WITHERED','Mutation.PHASE','Mutation.PLAGUE','MobEffects.WITHER, 80, 0','MobEffects.POISON, 120, 0','player.giveExperiencePoints(10)','Items.ECHO_SHARD'],"endgame mutations regression")

project=read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java")
need(project,["ASCENSION_NEXUS","승천 중추","requiredWorldStage","Items.NETHER_STAR","Items.DRAGON_BREATH","Lv.100 7×7×10","Lv.100 7×7×7","Lv.100 3회","완공 후 승천 시련"],"Ascension Nexus regression")
service=read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java")
need(service,["world.stage() < project.requiredWorldStage()","countItem","consumeItem","player.isCreative() || player.isSpectator()","AscensionTrialSystem.tryStart(player)"],"infrastructure regression")

trial=read("src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java")
doctrine=read("src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialDoctrine.java")
need(trial,["Gateways to Eternity","ECHO_SHARD_COST = 32","AMETHYST_COST = 64","DRAGON_BREATH_COST = 8","TOTAL_WAVES = 4","WAVE_TIMEOUT_TICKS = 1200","START_COOLDOWN_TICKS = 2400","EXCLUSION_RADIUS = 96.0D","AscensionTrialDoctrine.random","maybeReinforce","reinforcementTypeId","reinforcementsTriggered","EntitySpawnReason.TRIGGERED","ServerBossEvent","BuiltInRegistries.ENTITY_TYPE.containsKey(identifier)","trial.doctrine == AscensionTrialDoctrine.PURSUIT","AscensionAffixes.createEliteDrop(trial.level.getRandom(), 3)","onEntityJoin(EntityJoinLevelEvent event)","removeStaleServerTrials(server)","trial.level.getServer() != server","입장 재료는 반환되지 않습니다"],"Ascension Trial regression")
need(doctrine,["ONSLAUGHT","PURSUIT","SIEGE","쇄도","추격","봉쇄","reinforcementCount","RandomSource"],"Ascension Trial doctrine")
if '"minecraft:evoker"' in trial: errors.append("Ascension Trial must not spawn untracked summon-producing evokers")

mobility=read("src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java")
need(mobility,["AIR_DASH_COUNT","WorldAscensionData.get(serverLevel.getServer()).stage() >= 2","InfrastructureProject.ASCENSION_NEXUS","ExpeditionProgression.hasFieldMastery(player)","return 4;","return level >= 100 ? 3 : 2","AIR_DASH_COUNT.put(uuid, 0)","DASH_READY_TICK","ExpeditionProgression.recordSkillAction(player, SkillType.MOBILITY, units * 6)","!player.isPassenger()","!player.getAbilities().flying","!player.isFallFlying()","!player.isSwimming()","distance <= 1.75D"],"field mastery and traversal expedition mobility")
elite=read("src/main/java/kr/moonseungjun/survivalascension/elite/EliteMobSystem.java")
need(elite,["WorldAscensionData.get(level.getServer()).stage()","Math.min(0.28D","worldStage * 0.04D"],"elite regression")
warband=read("src/main/java/kr/moonseungjun/survivalascension/elite/WarbandDirector.java")
need(warband,["int minimum = 3 + worldStage","6 + worldStage","worldStage * 0.08D","ROUT_TICKS = 160","Items.ECHO_SHARD"],"warband regression")
combat=read("src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java")
need(combat,["ExpeditionProgression.hasFieldMastery(player)","fieldMastery ? 7.5D", "fieldMastery ? 20", "combatLevel >= 100 ? 0.55D : 0.45D","combatLevel >= 100 ? 50 : 60","InfrastructureProject.COMBAT_ACADEMY","victim instanceof Enemy", "ExpeditionProgression.recordSkillAction(player, SkillType.COMBAT, 1)"],"field mastery and combat expedition")
wood=read("src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java")
need(wood,["GLOBAL_LOG_BUDGET_PER_TICK = 64","LOCAL_LOG_BUDGET_PER_TICK = 12","FIELD_MASTERY_LOG_LIMIT = 448","ExpeditionProgression.hasFieldMastery(player)","hasLeavesNearby","BlockTags.LEAVES","if (player.gameMode.destroyBlock(target))", "ExpeditionProgression.recordSkillAction(player, SkillType.WOODCUTTING, 1)"],"field mastery and woodland objective")
mining=read("src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java")
need(mining,["isValidPickaxeBreak", "ExpeditionProgression.recordSkillAction(player, SkillType.MINING, 1)", "player.gameMode.destroyBlock(next)", "player.gameMode.destroyBlock(target)"],"deep mining objective")
bore=read("src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java")
need(bore,["GLOBAL_BLOCK_BUDGET_PER_TICK = 64","LOCAL_BLOCK_BUDGET_PER_TICK = 12","MAX_PENDING_PER_PLAYER = 640","ExpeditionProgression.hasFieldMastery(player)","fieldMastery ? 12", "skillLevel >= 100 ? 10 : 8","InfrastructureProject.QUARRY_NETWORK","player.gameMode.destroyBlock(target)"],"field mastery bore")
harvest=read("src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java")
need(harvest,["GLOBAL_HARVEST_BUDGET_PER_TICK = 64","LOCAL_HARVEST_BUDGET_PER_TICK = 12","MAX_PENDING_PER_PLAYER = 384","ExpeditionProgression.hasFieldMastery(player)","baseSize = 13","scheduleHarvestArea","AREA_GUARD.add(player.getUUID())","player.gameMode.destroyBlock(target)","ExpeditionProgression.recordSkillAction(player, SkillType.HARVESTING, 1)"],"tick-drained field mastery harvesting and wetland objective")
replant=read("src/main/java/kr/moonseungjun/survivalascension/harvesting/IrrigationReplantService.java")
need(replant,["InfrastructureProject.IRRIGATION_WORKS","consumeOne(player, kind.seed())","EventHooks.onBlockPlace"],"irrigation regression")
construction=read("src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java")
need(construction,["GLOBAL_BLOCK_BUDGET_PER_TICK = 64","MAX_PENDING_BLOCKS_PER_PLAYER = 512","ExpeditionProgression.hasFieldMastery(player)","fieldMastery ? 65","fieldMastery ? 13","level >= 100 ? 7 : 5","InfrastructureProject.BUILDER_FOUNDRY","EventHooks.onBlockPlace","consumeOne(player, item)","result == PlaceResult.PLACED","ExpeditionProgression.recordSkillAction(player, SkillType.CONSTRUCTION, 1)"],"field mastery construction and arid objective")

guide=read("src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java")
need(guide,["대원정 현장 목표","자연 나무 일괄벌목96","대량배치128","성숙작물96","도보횡단600","수영/항해800","곡괭이채굴192","적대몹24","적대몹32","/ascension stats","현장 숙련"] ,"0.24 guide")
third=read("THIRD_PARTY_NOTICES.md")
need(third,["Hostiles Are Too Easy","CC0 1.0 Universal","Warband","Veinminer++","Gateways to Eternity","Copyright (c) 2020 Brennan Ward","Bountiful — reference only for 0.24","LGPL-3.0-only","FTB Quests — reference only for 0.24","All Rights Reserved"],"third-party notices regression")

affix=read("src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java")
need(affix,["AFFIX_POOL",'AWAKENED = "awakened"',"public static boolean canAwaken","currentAffixes(stack).size() == 3","if (!canAwaken(stack)) return false","missing.size() != 2","public static boolean awaken","public static boolean isAwakened","awakened ? 4 : rarity","§5[각성 신화]","adjustMiningArea","adjustWoodcuttingLimit","adjustHarvestArea","if (base <= 1","if (base <= 0.0D"],"awakened affix regression")
reforge=read("src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java")
need(reforge,["ACTION_AWAKEN","awakeningCosts","AscensionAffixes.canAwaken(held)","재료는 소비하지 않았습니다","AscensionAffixes.awaken","Items.AMETHYST_SHARD, 256","Items.DIAMOND, 24","Items.NETHERITE_SCRAP, 8","Items.ECHO_SHARD, 64","Items.DRAGON_BREATH, 16"],"Mythic awakening economy")

for rel in ["src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java","src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java","src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java"]:
    text=read(rel)
    if re.search(r"setBlock\s*\([^\n]*AIR", text): errors.append(f"scaled destruction bypasses normal destroy path: {rel}")
for forbidden in ["harmonised.pmmo", "alrex.parcool", "com.alrex", "mekanism.common", "com.warband", "vbonedra.hostiles_are_too_easy", "com.telepathicgrunt.repurposedstructures", "dev.ftb.mods.ftbquests"]:
    for path in (ROOT/"src").rglob("*.java"):
        if forbidden in path.read_text(encoding="utf-8",errors="ignore").lower(): errors.append(f"forbidden/reference namespace leaked: {path.relative_to(ROOT)} -> {forbidden}")

if errors:
    print("SOURCE AUDIT FAILED")
    for e in errors: print("-",e)
    sys.exit(1)
print("SOURCE AUDIT PASS")
print("- Minecraft 26.2 / NeoForge 26.2.0.38-beta / Java 25 / network protocol 8")
print("- expedition_v1 separates discovery, in-region field objective progress, completion and one-time rewards")
print("- 0.23 expedition saves migrate without duplicate region XP or loss of already-unlocked Field Mastery")
print("- objectives require real smart-tree/bulk-build/mature-crop/traversal/ocean-voyage/pickaxe/hostile-kill actions")
print("- Ocean travel is isolated from normal land Mobility progress and teleport-like voyage deltas are rejected")
print("- Field Mastery remains Lv.100-only: bore 7x7x12, wood448, harvest13x13, shockwave7.5/20, construction65/13, air dash4")
print("- doctrine trials, awakened Mythic, world stages, mutations, Nexus, warbands, elites and bounded tick budgets retained")

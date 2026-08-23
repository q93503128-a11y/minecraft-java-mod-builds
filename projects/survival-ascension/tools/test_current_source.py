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
    "src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/SkillsScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/EquipmentRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/world/WorldAscensionData.java",
    "src/main/java/kr/moonseungjun/survivalascension/world/WorldAscensionProgression.java",
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
    "src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java",
    "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
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

props=(ROOT/"gradle.properties").read_text(encoding="utf-8")
need(props,["minecraft_version=26.2","neo_version=26.2.0.38-beta","mod_id=survivalascension","mod_version=0.22.0-alpha.1"],"gradle.properties")
main=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java").read_text(encoding="utf-8")
need(main,['VERSION = "0.22.0-alpha.1"',"WorldAscensionProgression::onLivingDeath","WarbandDirector::onServerTick","EndgameMutationSystem::onFinalizeSpawn","EndgameMutationSystem::onDamagePost","EndgameMutationSystem::onLivingDeath","AscensionTrialSystem::onServerTick","AscensionTrialSystem::onEntityJoin"],"main registration")

tuning=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java").read_text(encoding="utf-8")
need(tuning,[
    "if (level >= 100) return 11;", "if (level >= 100) return 192;", "if (level >= 100) return 384;",
    "if (level >= 100) return 10;", "if (level >= 100) return 5.0D;", "if (level >= 100) return 0.70D;",
    "if (level >= 100) return 49;", "if (level >= 100) return 2.0D;", "if (level >= 100) return 16.0D;",
    "if (level >= 100) return 1.80D;", "if (level >= 100) return 16;", "if (level >= 100) return 6;"
],"mastery VI tuning")
skills=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/client/SkillsScreen.java").read_text(encoding="utf-8")
need(skills,['case 6 -> "VI"'],"mastery VI UI")

world=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/world/WorldAscensionData.java").read_text(encoding="utf-8")
need(world,["world_ascension_v1","MAX_STAGE = 2","advanceTo","전설 단계","종말 단계"],"world ascension")
world_progress=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/world/WorldAscensionProgression.java").read_text(encoding="utf-8")
need(world_progress,["instanceof WitherBoss","targetStage = 1","instanceof EnderDragon","targetStage = 2","data.advanceTo(targetStage)"],"boss progression")

mutation=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/elite/EndgameMutationSystem.java").read_text(encoding="utf-8")
need(mutation,[
    'MUTATION_KEY = "survivalascension_endgame_mutation"', 'MUTATION_CHANCE = 0.18D',
    'WorldAscensionData.get(level.getServer()).stage() < 2', 'contains("SPAWNER")',
    'mob instanceof AbstractSkeleton', 'mob instanceof Zombie', 'Mutation.WITHERED', 'Mutation.PHASE', 'Mutation.PLAGUE',
    'MobEffects.WITHER, 80, 0', 'MobEffects.POISON, 120, 0', 'level.getRandom().nextFloat() >= 0.55F', 'now + 45L',
    'player.giveExperiencePoints(10)', 'nextFloat() < 0.35F', 'Items.ECHO_SHARD'
],"endgame mutations regression")

project=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java").read_text(encoding="utf-8")
need(project,["ASCENSION_NEXUS","승천 중추","requiredWorldStage","Items.NETHER_STAR","Items.DRAGON_BREATH","Lv.100 7×7×10","Lv.100 7×7×7","Lv.100 3회","완공 후 승천 시련"],"Ascension Nexus regression")
service=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java").read_text(encoding="utf-8")
need(service,["world.stage() < project.requiredWorldStage()","countItem","consumeItem","player.isCreative() || player.isSpectator()","AscensionTrialSystem.tryStart(player)","메아리 조각 32 · 자수정 조각 64 · 드래곤의 숨결 8"],"infrastructure regression")

trial=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java").read_text(encoding="utf-8")
doctrine=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialDoctrine.java").read_text(encoding="utf-8")
need(trial,[
    "Gateways to Eternity", "ECHO_SHARD_COST = 32", "AMETHYST_COST = 64", "DRAGON_BREATH_COST = 8",
    "TOTAL_WAVES = 4", "WAVE_TIMEOUT_TICKS = 1200", "START_COOLDOWN_TICKS = 2400", "EXCLUSION_RADIUS = 96.0D",
    "AscensionTrialDoctrine.random", "maybeReinforce", "reinforcementTypeId", "reinforcementsTriggered", "initialWaveCount",
    "InfrastructureProject.ASCENSION_NEXUS", "WorldAscensionData.get(server).stage() < 2", "EntitySpawnReason.TRIGGERED",
    "ServerBossEvent", "BuiltInRegistries.ENTITY_TYPE.containsKey(identifier)", "BuiltInRegistries.ENTITY_TYPE.getValue(identifier)",
    '"minecraft:ravager"', '"minecraft:pillager"', '"minecraft:wither_skeleton"', '"minecraft:enderman"', '"minecraft:witch"',
    "trial.doctrine == AscensionTrialDoctrine.PURSUIT", "mob.getNavigation().moveTo(owner, 1.35D)",
    "AscensionAffixes.createEliteDrop(trial.level.getRandom(), 3)", "Items.NETHERITE_SCRAP, 2", "Items.DIAMOND, 4",
    "onEntityJoin(EntityJoinLevelEvent event)", "TRIAL_OWNER_KEY", "active.mobIds.contains(mob.getUUID())", "event.setCanceled(true)",
    "removeStaleServerTrials(server)", "trial.level.getServer() != server", "distanceToCenterSqr", "입장 재료는 반환되지 않습니다"
],"Ascension Trial doctrine regression")
need(doctrine,["ONSLAUGHT","PURSUIT","SIEGE","쇄도","추격","봉쇄","reinforcementCount","RandomSource"],"Ascension Trial doctrine")
if '"minecraft:evoker"' in trial:
    errors.append("Ascension Trial must not spawn untracked summon-producing evokers")

mobility=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java").read_text(encoding="utf-8")
need(mobility,["AIR_DASH_COUNT","maxAirDashes","WorldAscensionData.get(serverLevel.getServer()).stage() >= 2","InfrastructureProject.ASCENSION_NEXUS","return level >= 100 ? 3 : 2","AIR_DASH_COUNT.put(uuid, 0)","DASH_READY_TICK","[기동 숙련 VI]"],"mastery VI mobility")

elite=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/elite/EliteMobSystem.java").read_text(encoding="utf-8")
need(elite,["WorldAscensionData.get(level.getServer()).stage()","Math.min(0.28D","Math.min(0.22D","worldStage * 0.05D"],"elite regression")
warband=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/elite/WarbandDirector.java").read_text(encoding="utf-8")
need(warband,["int minimum = 3 + worldStage","6 + worldStage","worldStage * 0.08D","ROUT_TICKS = 160","Items.ECHO_SHARD"],"warband regression")
combat=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java").read_text(encoding="utf-8")
need(combat,["combatLevel >= 100 ? 6.5D : 5.5D","combatLevel >= 100 ? 16 : 12","combatLevel >= 100 ? 0.55D : 0.45D","combatLevel >= 100 ? 50 : 60","InfrastructureProject.COMBAT_ACADEMY","[전투 숙련 VI]"],"mastery VI combat")
wood=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java").read_text(encoding="utf-8")
need(wood,["GLOBAL_LOG_BUDGET_PER_TICK = 64","LOCAL_LOG_BUDGET_PER_TICK = 12","hasLeavesNearby","BlockTags.LEAVES"],"woodcutting regression")
bore=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java").read_text(encoding="utf-8")
need(bore,["GLOBAL_BLOCK_BUDGET_PER_TICK = 64","LOCAL_BLOCK_BUDGET_PER_TICK = 12","MAX_PENDING_PER_PLAYER = 512","skillLevel >= 100 ? 7 : 5","skillLevel >= 100 ? 10 : 8","InfrastructureProject.QUARRY_NETWORK"],"mastery VI bore")
replant=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/harvesting/IrrigationReplantService.java").read_text(encoding="utf-8")
need(replant,["InfrastructureProject.IRRIGATION_WORKS","consumeOne(player, kind.seed())","EventHooks.onBlockPlace"],"irrigation regression")
construction=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java").read_text(encoding="utf-8")
need(construction,["MAX_PENDING_BLOCKS_PER_PLAYER = 512","level >= 100 ? 7 : 5","InfrastructureProject.BUILDER_FOUNDRY","[건축 숙련 VI]"],"mastery VI construction")

guide=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java").read_text(encoding="utf-8")
need(guide,["숙련 VI","Lv.100 11×11+광맥192","Lv.100 384","Lv.100 파급10/5블록","입체7³","Lv.100은 3회","종말 변이","승천 시련","시련 교리","신화 각성","자수정256"],"guide")
third=(ROOT/"THIRD_PARTY_NOTICES.md").read_text(encoding="utf-8")
need(third,["Hostiles Are Too Easy","CC0 1.0 Universal","0.19","Warband","Veinminer++","Gateways to Eternity","Copyright (c) 2020 Brennan Ward"],"third-party notices")

affix=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java").read_text(encoding="utf-8")
need(affix,[
    "AFFIX_POOL", 'AWAKENED = "awakened"', "public static boolean awaken", "public static boolean isAwakened",
    "awakened ? 4 : rarity", "chosen.add(missing.get", "§5[각성 신화]", "adjustMiningArea","adjustWoodcuttingLimit","adjustHarvestArea",
    "if (base <= 1", "if (base <= 0.0D"
],"awakened affix regression")
reforge=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java").read_text(encoding="utf-8")
need(reforge,[
    "ACTION_REFORGE", "ACTION_SALVAGE", "ACTION_AWAKEN", "awakeningCosts", "AscensionAffixes.awaken",
    "Items.AMETHYST_SHARD, 256", "Items.DIAMOND, 24", "Items.NETHERITE_SCRAP, 8", "Items.ECHO_SHARD, 64", "Items.DRAGON_BREATH, 16",
    "Items.AMETHYST_SHARD, 128", "Items.NETHERITE_SCRAP, 4", "Items.ECHO_SHARD, 16"
],"Mythic awakening economy")
equipment_ui=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/client/EquipmentRadialMenuScreen.java").read_text(encoding="utf-8")
need(equipment_ui,["신화 각성","Items.NETHER_STAR","Action.AWAKEN","ACTION_AWAKEN","4번째 affix 개방","자수정256 · 다이아24 · 파편8","메아리64 · 드래곤숨결16"],"equipment radial awakening")

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
print("- Lv.100 mastery VI scale and existing tick/protection/material contracts retained")
print("- Stage-2 Ascension Trial now varies by tactical doctrine and adds one bounded mid-wave reinforcement without HP-only scaling")
print("- Mythic III awakening preserves unlock gates, consumes endgame resources, adds one affix, and keeps four-affix rerolls expensive")
print("- world stages, mutations, Nexus, warbands, elites and equipment economy regressions retained")

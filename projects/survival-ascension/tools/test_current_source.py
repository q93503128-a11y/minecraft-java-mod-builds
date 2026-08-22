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
    "src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/SkillsScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java",
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
need(props,["minecraft_version=26.2","neo_version=26.2.0.38-beta","mod_id=survivalascension","mod_version=0.20.0-alpha.1"],"gradle.properties")
main=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java").read_text(encoding="utf-8")
need(main,['VERSION = "0.20.0-alpha.1"',"WorldAscensionProgression::onLivingDeath","WarbandDirector::onServerTick","EndgameMutationSystem::onFinalizeSpawn","EndgameMutationSystem::onDamagePost","EndgameMutationSystem::onLivingDeath"],"main registration")

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
need(project,["ASCENSION_NEXUS","승천 중추","requiredWorldStage","Items.NETHER_STAR","Items.DRAGON_BREATH"],"Ascension Nexus regression")
service=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java").read_text(encoding="utf-8")
need(service,["world.stage() < project.requiredWorldStage()","countItem","consumeItem","player.isCreative() || player.isSpectator()"],"infrastructure regression")

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
need(guide,["숙련 VI","Lv.100 11×11+광맥192","Lv.100 384","Lv.100 파급10/5블록","입체7³","Lv.100은 3회","종말 변이"],"guide")
third=(ROOT/"THIRD_PARTY_NOTICES.md").read_text(encoding="utf-8")
need(third,["Hostiles Are Too Easy","CC0 1.0 Universal","0.19","Warband","Veinminer++"],"third-party notices")

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
print("- Lv.100 is mastery VI across all six skills with a final action-scale jump")
print("- large Mining/Construction capstones retain existing tick budgets and protection/material paths")
print("- Stage-2 mutations, Ascension Nexus, world stages, warbands, elites and equipment economy regressions retained")

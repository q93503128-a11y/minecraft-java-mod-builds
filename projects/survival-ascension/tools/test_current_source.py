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
    "src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureData.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/InfrastructureRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/elite/EliteMobSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/elite/WarbandDirector.java",
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
need(props,["minecraft_version=26.2","neo_version=26.2.0.38-beta","mod_id=survivalascension","mod_version=0.18.0-alpha.1"],"gradle.properties")
main=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java").read_text(encoding="utf-8")
need(main,['VERSION = "0.18.0-alpha.1"',"WorldAscensionProgression::onLivingDeath","MobilityProgression::onPlayerTick","WarbandDirector::onServerTick"],"main registration")

world=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/world/WorldAscensionData.java").read_text(encoding="utf-8")
need(world,["world_ascension_v1","MAX_STAGE = 2","advanceTo","전설 단계","종말 단계"],"world ascension")
world_progress=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/world/WorldAscensionProgression.java").read_text(encoding="utf-8")
need(world_progress,["instanceof WitherBoss","targetStage = 1","instanceof EnderDragon","targetStage = 2","data.advanceTo(targetStage)"],"boss progression")

project=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java").read_text(encoding="utf-8")
need(project,["ASCENSION_NEXUS","승천 중추","requiredWorldStage","Items.NETHER_STAR","4","Items.DRAGON_BREATH","64","Items.OBSIDIAN","512","Items.AMETHYST_SHARD","Items.ECHO_SHARD"],"Ascension Nexus project")
service=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java").read_text(encoding="utf-8")
need(service,["world.stage() < project.requiredWorldStage()","월드 승천","project.requiredWorldStage()","countItem","consumeItem","player.isCreative() || player.isSpectator()"],"stage-gated infrastructure funding")
ui=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/client/InfrastructureRadialMenuScreen.java").read_text(encoding="utf-8")
need(ui,["승천 중추","InfrastructureProject.ASCENSION_NEXUS","네더별4","숨결64","흑요석512","자수정512","메아리64"],"infrastructure radial")

mobility=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java").read_text(encoding="utf-8")
need(mobility,["AIR_DASH_COUNT","maxAirDashes","level < 60","level >= 90","WorldAscensionData.get(serverLevel.getServer()).stage() >= 2","InfrastructureProject.ASCENSION_NEXUS","isComplete(InfrastructureProject.ASCENSION_NEXUS)","return 2","AIR_DASH_COUNT.put(uuid, 0)","used + 1","DASH_READY_TICK"],"Nexus mobility contract")
if "AIR_DASH_USED" in mobility: errors.append("legacy boolean air-dash state still present")

elite=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/elite/EliteMobSystem.java").read_text(encoding="utf-8")
need(elite,["WorldAscensionData.get(level.getServer()).stage()","Math.min(0.28D","Math.min(0.22D","worldStage * 0.05D"],"0.17 elite regression")
warband=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/elite/WarbandDirector.java").read_text(encoding="utf-8")
need(warband,["int minimum = 3 + worldStage","6 + worldStage","worldStage * 0.08D","ROUT_TICKS = 160","Items.ECHO_SHARD"],"0.17 warband regression")
combat=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java").read_text(encoding="utf-8")
need(combat,["SHOCKWAVE_RADIUS = 5.5D","SHOCKWAVE_TARGETS = 12","InfrastructureProject.COMBAT_ACADEMY"],"combat academy regression")
wood=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java").read_text(encoding="utf-8")
need(wood,["GLOBAL_LOG_BUDGET_PER_TICK = 64","LOCAL_LOG_BUDGET_PER_TICK = 12","hasLeavesNearby","BlockTags.LEAVES"],"woodcutting regression")
bore=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java").read_text(encoding="utf-8")
need(bore,["GLOBAL_BLOCK_BUDGET_PER_TICK = 64","LOCAL_BLOCK_BUDGET_PER_TICK = 12","InfrastructureProject.QUARRY_NETWORK"],"bore regression")
replant=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/harvesting/IrrigationReplantService.java").read_text(encoding="utf-8")
need(replant,["InfrastructureProject.IRRIGATION_WORKS","consumeOne(player, kind.seed())","EventHooks.onBlockPlace"],"irrigation regression")
construction=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java").read_text(encoding="utf-8")
need(construction,["VOLUME_SIZE = 5","InfrastructureProject.BUILDER_FOUNDRY","MAX_PENDING_BLOCKS_PER_PLAYER = 256"],"construction regression")

guide=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java").read_text(encoding="utf-8")
need(guide,["승천 중추","네더의 별4","드래곤의 숨결64","공중 돌진 2회","종말 단계 전에는 자원 투입 불가"],"guide")
third=(ROOT/"THIRD_PARTY_NOTICES.md").read_text(encoding="utf-8")
need(third,["Hostiles Are Too Easy","CC0 1.0 Universal","Warband","Veinminer++"],"third-party notices")

# Affix/economy stay live.
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
print("- Ascension Nexus is Stage-2 gated and consumes boss/endgame resources")
print("- Mobility Lv.90 receives two air dashes only after Endgame stage + Nexus completion")
print("- dash uses still share the existing cooldown and reset only on landing")
print("- all 0.17 world-stage, warband, elite and earlier scaled-work/economy regressions retained")

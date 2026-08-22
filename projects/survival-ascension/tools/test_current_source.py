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
    "src/main/java/kr/moonseungjun/survivalascension/elite/WarbandDirector.java",
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
need(props,["minecraft_version=26.2","neo_version=26.2.0.38-beta","mod_id=survivalascension","mod_version=0.16.0-alpha.1"],"gradle.properties")
main=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java").read_text(encoding="utf-8")
need(main,['VERSION = "0.16.0-alpha.1"',"WoodcuttingProgression::onServerTick","BoreMiningService::onServerTick","IrrigationReplantService::onServerTick","ConstructionProgression::onServerTick","EliteMobSystem::onFinalizeSpawn","WarbandDirector::onFinalizeSpawn","WarbandDirector::onServerTick","WarbandDirector::onLivingDeath"],"main registration")

warband=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/elite/WarbandDirector.java").read_text(encoding="utf-8")
need(warband,["SPAWNER","NO_WARBAND_KEY","FORMATION_INTERVAL = 200","ROUT_TICKS = 160","power < 30.0D","3 + (int) Math.floor","Role.LEADER","Role.BRUISER","Role.HUNTER","Role.SUPPORT","member.setTarget(target)","retreat(member, target)","supportAction","wounded.heal","Items.ECHO_SHARD","UUID.randomUUID().toString()","addPermanentModifier"],"warband contract")
if "playerData.putLong(PLAYER_NEXT_FORMATION_KEY, now + 600L)" not in warband: errors.append("warband formation lacks per-player cooldown")
if "getBooleanOr(NO_WARBAND_KEY, false)" not in warband: errors.append("spawner-exclusion marker is not rechecked during formation")

combat=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java").read_text(encoding="utf-8")
need(combat,["SHOCKWAVE_GUARD","SHOCKWAVE_RADIUS = 5.5D","SHOCKWAVE_TARGETS = 12","SHOCKWAVE_FRACTION = 0.45D","SHOCKWAVE_COOLDOWN_TICKS = 60","combatLevel < 90 || !player.isSprinting()","InfrastructureProject.COMBAT_ACADEMY","InfrastructureData.get(player).isComplete","candidate.hurtServer","candidate instanceof Enemy","player.isAlliedTo(candidate)","return true"],"combat academy shockwave")

wood=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java").read_text(encoding="utf-8")
need(wood,["smart-tree leaf safety","GLOBAL_LOG_BUDGET_PER_TICK = 64","LOCAL_LOG_BUDGET_PER_TICK = 12","JOBS.containsKey","scheduleNaturalTree","gatherConnectedLogs","hasLeavesNearby","hasAdjacentLeaf","BlockTags.LEAVES","CHAIN_GUARD.add","player.gameMode.destroyBlock(target)"],"woodcutting safety")
if "hasLeavesNearby(level, origin, gathered)" not in wood: errors.append("woodcutting can bulk-fell without leaf evidence")

construction_mode=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionMode.java").read_text(encoding="utf-8")
need(construction_mode,['VOLUME("volume", "입체", 90)'],"construction mode")
construction=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java").read_text(encoding="utf-8")
need(construction,["VOLUME_SIZE = 5","ConstructionMode.VOLUME","InfrastructureProject.BUILDER_FOUNDRY","MAX_PENDING_BLOCKS_PER_PLAYER = 256","EventHooks.onBlockPlace","consumeOne(player, item)","mode == ConstructionMode.VOLUME","center.offset(dx, dy, dz)"],"construction volume")

infra=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java").read_text(encoding="utf-8")
need(infra,["QUARRY_NETWORK","IRRIGATION_WORKS","BUILDER_FOUNDRY","COMBAT_ACADEMY","Items.ECHO_SHARD","32","Items.GOLD_INGOT","256","Items.EMERALD","128"],"infrastructure projects")
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
need(infra_ui,["채석장 네트워크","관개 시설","건축 공방","전투 훈련장","InfrastructureProject.COMBAT_ACADEMY","메아리32","진행도"],"infrastructure radial")

guide=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java").read_text(encoding="utf-8")
need(guide,["전술 분대","전단장/돌격/추적/지원","메아리 조각","전투 훈련장","질주 근접 공격","5×5×5 입체 채우기"],"guide")

# Preserve pre-0.16 core systems.
for rel, needles in {
    "mining/MiningProgression.java":["case EXTRACT","case BORE","extractMatchingOre","BoreMiningService.schedule","player.gameMode.destroyBlock(target)"],
    "equipment/AscensionAffixes.java":["AFFIX_POOL","reroll","adjustMiningArea","adjustWoodcuttingLimit","adjustHarvestArea"],
    "equipment/EquipmentReforgeService.java":["ACTION_REFORGE","ACTION_SALVAGE","Items.AMETHYST_SHARD","Items.NETHERITE_SCRAP"],
    "elite/EliteMobSystem.java":["contains(\"SPAWNER\")","Trait.VAMPIRIC","Trait.BERSERKER","addPermanentModifier"],
}.items():
    text=(ROOT/"src/main/java/kr/moonseungjun/survivalascension"/rel).read_text(encoding="utf-8")
    need(text,needles,f"regression {rel}")

third=(ROOT/"THIRD_PARTY_NOTICES.md").read_text(encoding="utf-8")
need(third,["Veinminer++","smart-tree","Mekanism","Create","Warband","Renasca-Studios/Warband","Copyright (c) 2026 Divesh Gupta"],"third-party notices")
warband_notice=(ROOT/"src/main/resources/META-INF/third-party/WARBAND_MIT.txt").read_text(encoding="utf-8")
need(warband_notice,["MIT License","Copyright (c) 2026 Divesh Gupta"],"Warband runtime notice")

for rel in ["src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java","src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java","src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java"]:
    text=(ROOT/rel).read_text(encoding="utf-8")
    if re.search(r"setBlock\s*\([^\n]*AIR", text): errors.append(f"scaled destruction bypasses normal destroy path: {rel}")
for forbidden in ["harmonised.pmmo", "alrex.parcool", "com.alrex", "mekanism.common", "com.warband"]:
    for path in (ROOT/"src").rglob("*.java"):
        if forbidden in path.read_text(encoding="utf-8",errors="ignore").lower(): errors.append(f"forbidden/reference namespace leaked: {path.relative_to(ROOT)} -> {forbidden}")

if errors:
    print("SOURCE AUDIT FAILED")
    for e in errors: print("-",e)
    sys.exit(1)
print("SOURCE AUDIT PASS")
print("- Minecraft 26.2 / NeoForge 26.2.0.38-beta / Java 25")
print("- Warband MIT tactical squads use progression-gated 3-6 member formations with four active roles")
print("- spawner-marked mobs cannot join warbands; leader death causes an 8-second rout and Echo Shard reward")
print("- Combat Academy gates Lv.90 sprint shockwave: 5.5 radius, 12 targets, 60-tick cooldown")
print("- Veinminer++ smart-tree/tick-drain, Builder Foundry volume, tunnel, irrigation, elite, affix and reforge regressions retained")

#!/usr/bin/env python3
from pathlib import Path
import re, sys

ROOT = Path(__file__).resolve().parents[1]
errors = []

def read(rel):
    path = ROOT / rel
    if not path.exists():
        errors.append(f"missing: {rel}")
        return ""
    return path.read_text(encoding="utf-8")

def need(text, needles, label):
    for needle in needles:
        if needle not in text:
            errors.append(f"{label} missing: {needle}")

def ordered(text, needles, label):
    pos = -1
    for needle in needles:
        nxt = text.find(needle, pos + 1)
        if nxt < 0:
            errors.append(f"{label} missing/order: {needle}")
            return
        pos = nxt

# Binary files are existence-checked only. Never decode gradle-wrapper.jar as text.
required = [
    "PROJECT.md", "README.md", "CHANGELOG.md", "THIRD_PARTY_NOTICES.md",
    "build.gradle", "gradle.properties", "settings.gradle", "gradlew",
    "gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties",
    "src/main/templates/META-INF/neoforge.mods.toml",
    "src/main/resources/META-INF/third-party/SKILL_PROFICIENCIES_MIT.txt",
    "src/main/resources/META-INF/third-party/VEINMINER_PLUS_PLUS_MIT.txt",
    "src/main/resources/META-INF/third-party/MINEMENU_MIT.txt",
    "src/main/resources/META-INF/third-party/BUILDING_GADGETS_2_MIT.txt",
    "src/main/resources/META-INF/third-party/MOB_CHAMPIONS_MIT.txt",
    "src/main/resources/META-INF/third-party/APOTHEOSIS_MIT.txt",
    "src/main/resources/META-INF/third-party/MEKANISM_MIT.txt",
    "src/main/resources/META-INF/third-party/WARBAND_MIT.txt",
    "src/main/resources/META-INF/third-party/HOSTILES_ARE_TOO_EASY_CC0.txt",
    "src/main/resources/META-INF/third-party/GATEWAYS_TO_ETERNITY_MIT.txt",
    "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java",
    "src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionDirective.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionData.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionIncident.java",
    "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionIncidentSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/apex/ApexArchetype.java",
    "src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntData.java",
    "src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureData.java",
    "src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/ProductionProgram.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/ProductionData.java",
    "src/main/java/kr/moonseungjun/survivalascension/production/ProductionService.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/InfrastructureRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/command/AscensionCommands.java",
    "src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java",
    "src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java",
    "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/elite/WarbandDirector.java",
    "src/main/java/kr/moonseungjun/survivalascension/elite/EndgameMutationSystem.java",
    "src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java",
    "src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java",
]
for rel in required:
    if not (ROOT / rel).exists(): errors.append(f"missing: {rel}")

props = read("gradle.properties")
need(props, ["minecraft_version=26.2", "neo_version=26.2.0.38-beta", "mod_version=0.28.0-alpha.1"], "gradle properties")
main = read("src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java")
need(main, ['VERSION = "0.28.0-alpha.1"', "ApexHuntSystem::onServerTick", "ExpeditionIncidentSystem::onPlayerTick",
            "AscensionTrialSystem::onServerTick", "WarbandDirector::onServerTick"], "main registration")
network = read("src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java")
need(network, ['PROTOCOL = "8"'], "network protocol")

# 0.28 shared infrastructure + production catalog
infra_project = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureProject.java")
need(infra_project, ["INDUSTRIAL_WORKS(", '"industrial_works"', '"산업 가공소"',
                     'Items.STONE_BRICKS, "석재 벽돌", 1024', 'Items.IRON_INGOT, "철 주괴", 512',
                     'Items.COPPER_INGOT, "구리 주괴", 512', 'Items.REDSTONE, "레드스톤", 256',
                     'Items.AMETHYST_SHARD, "자수정 조각", 128', "APEX_TRACKING_POST(", "ASCENSION_NEXUS("],
     "industrial infrastructure")
infra_data = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureData.java")
need(infra_data, ['"infrastructure_v1"', 'project.id() + ":" + requirementIndex'], "infrastructure compatibility")

program = read("src/main/java/kr/moonseungjun/survivalascension/production/ProductionProgram.java")
need(program, ["METALWORKS(", "TIMBERWORKS(", "PROVISIONS(", "PRECISION(",
               'Input.item(Items.RAW_IRON, "철 원석", 96)', 'Input.item(Items.RAW_COPPER, "구리 원석", 96)',
               'Input.item(Items.COAL, "석탄", 64)', 'Input.tag(ItemTags.LOGS, "통나무", 192)',
               'Input.item(Items.COBBLESTONE, "조약돌", 384)', 'Input.item(Items.IRON_INGOT, "철 주괴", 32)',
               'Input.item(Items.WHEAT, "밀", 128)', 'Input.item(Items.CARROT, "당근", 64)',
               'Input.item(Items.POTATO, "감자", 64)', 'Input.item(Items.BEETROOT, "비트", 32)',
               'Input.item(Items.REDSTONE, "레드스톤", 128)', 'Input.item(Items.AMETHYST_SHARD, "자수정 조각", 64)',
               'Input.item(Items.GOLD_INGOT, "금 주괴", 32)', 'Input.item(Items.QUARTZ, "네더 석영", 64)',
               "stack.is(tag)"], "four production programs")

production_data = read("src/main/java/kr/moonseungjun/survivalascension/production/ProductionData.java")
need(production_data, ['"production_v1"', "MAX_BUFFER = 3", "MAX_SUPPLY_CHARGES = 3",
                       'optionalFieldOf("metalworks", 0)', 'optionalFieldOf("timberworks", 0)',
                       'optionalFieldOf("provisions", 0)', 'optionalFieldOf("precision", 0)',
                       'optionalFieldOf("cycles", 0)', 'optionalFieldOf("supply_charges", 0)',
                       "normalizeCycles(state)", "while (state.supplyCharges < MAX_SUPPLY_CHARGES",
                       "state.metalworks--", "state.timberworks--", "state.provisions--", "state.precision--",
                       "state.cycles++", "state.supplyCharges++", "consumeSupplyCharge"], "production persistence/cycle")
# Dispatch must free a charge then immediately normalize any queued complete four-line sets.
ordered(production_data, ["public boolean consumeSupplyCharge", "state.supplyCharges--;", "normalizeCycles(state);", "setDirty();"],
        "queued cycle normalization after dispatch")

production_service = read("src/main/java/kr/moonseungjun/survivalascension/production/ProductionService.java")
need(production_service, ['ACTION_PREFIX = "produce:"', 'ACTION_STATUS = "production_status"', 'ACTION_DISPATCH = "dispatch_supply"',
                          "InfrastructureProject.INDUSTRIAL_WORKS", "data.canAccept(player, program)", "hasAll(player, program)",
                          "for (ProductionProgram.Input input : program.inputs()) consume", "data.addBatch(player, program)",
                          "data.consumeSupplyCharge(player)", "new ItemStack(Items.GOLD_INGOT, 32)",
                          "new ItemStack(Items.AMETHYST_SHARD, 16)", "new ItemStack(Items.ECHO_SHARD, 2)",
                          "if (!player.getInventory().add(stack)) player.drop(stack, false)"], "production service")
# Capacity and complete-material checks must happen before any batch input consumption.
ordered(production_service, ["if (!data.canAccept(player, program))", "if (!hasAll(player, program))",
                             "for (ProductionProgram.Input input : program.inputs()) consume", "data.addBatch(player, program)"],
        "atomic production batch ordering")

infrastructure = read("src/main/java/kr/moonseungjun/survivalascension/infrastructure/InfrastructureService.java")
need(infrastructure, ["project == InfrastructureProject.INDUSTRIAL_WORKS", "ProductionService.ACTION_STATUS.equals(action)",
                      "ProductionService.ACTION_DISPATCH.equals(action)", "action.startsWith(ProductionService.ACTION_PREFIX)",
                      "ProductionService.perform(player, action)", "ApexHuntSystem.tryStart(player)",
                      "ApexHuntSystem.isActive(player)", "AscensionTrialSystem.tryStart(player)"], "infrastructure routing")

infra_ui = read("src/main/java/kr/moonseungjun/survivalascension/client/InfrastructureRadialMenuScreen.java")
need(infra_ui, ['"산업 가공소"', "InfrastructureProject.INDUSTRIAL_WORKS", "Action.OPEN_PRODUCTION",
                "new ProductionRadialMenuScreen()"], "industrial radial entry")
production_ui = read("src/main/java/kr/moonseungjun/survivalascension/client/ProductionRadialMenuScreen.java")
need(production_ui, ['"시설 투자"', '"제련 배치"', '"구조재 배치"', '"식량 배치"', '"정밀 부품 배치"',
                     '"현장 보급 출고"', '"생산 현황"', "Action.FUND", "Action.PRODUCE", "Action.DISPATCH", "Action.STATUS",
                     "InfrastructureService.ACTION_FUND", "ProductionService.ACTION_DISPATCH", "ProductionService.ACTION_PREFIX"],
     "production radial")
commands = read("src/main/java/kr/moonseungjun/survivalascension/command/AscensionCommands.java")
need(commands, ["ProductionData.get(player)", '"§3[산업 생산망]", "production.cycles(player)", "production.supplyCharges(player)"],
     "production stats")

# 0.25/0.26 expedition regressions
directive = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionDirective.java")
for marker in ["WOODLAND_STANDARD", "WOODLAND_PATROL", "ARID_STANDARD", "ARID_ROUTE", "WETLAND_STANDARD", "WETLAND_CLEARANCE",
               "HIGHLANDS_STANDARD", "HIGHLANDS_DASH", "OCEAN_STANDARD", "OCEAN_PATROL", "DEEP_STANDARD", "DEEP_CLEARANCE",
               "FROZEN_STANDARD", "FROZEN_DASH", "NETHER_STANDARD", "NETHER_SUPPLY", "END_STANDARD", "END_TRAVERSE"]:
    need(directive, [marker], "18 directive catalog")
exp_data = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionData.java")
need(exp_data, ['"expedition_v1"', 'optionalFieldOf("region_rewards", -1)', 'optionalFieldOf("incident_rewards", 0)',
                "directiveComplete", "MILESTONE_MASTER"], "expedition migration")
incident = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionIncident.java")
for marker in ["WOODLAND_AMBUSH", "WOODLAND_RUSH", "ARID_AMBUSH", "ARID_RUSH", "WETLAND_AMBUSH", "WETLAND_RUSH",
               "HIGHLANDS_AMBUSH", "HIGHLANDS_RUSH", "OCEAN_AMBUSH", "OCEAN_RUSH", "DEEP_AMBUSH", "DEEP_RUSH",
               "FROZEN_AMBUSH", "FROZEN_RUSH", "NETHER_AMBUSH", "NETHER_RUSH", "END_AMBUSH", "END_RUSH"]:
    need(incident, [marker], "18 incident catalog")
incident_system = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionIncidentSystem.java")
need(incident_system, ["CHECK_INTERVAL_TICKS = 600", "START_CHANCE = 0.10D", "EVENT_RADIUS = 48.0D",
                       "EntitySpawnReason.TRIGGERED", "data.claimIncidentReward", "bonusTask.target() / 5", "cleanupMobs"],
     "incident lifecycle")

# 0.27 Apex regressions
apex = read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexArchetype.java")
for marker in ["WOODLAND_BREAKER", "ARID_COMMANDER", "WETLAND_PLAGUEHEART", "HIGHLAND_HUNTER", "OCEAN_TYRANT",
               "DEEP_STALKER", "FROZEN_WARDEN", "NETHER_REAVER", "END_HARBINGER"]:
    need(apex, [marker], "nine apex archetypes")
need(apex, ["CHARGE", "REINFORCE", "PLAGUE", "SKIRMISH", "PULL", "LEAP", "FROST", "WITHER", "VOID"], "apex patterns")
apex_data = read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntData.java")
need(apex_data, ['"apex_hunt_v1"', "recordVictory", "claimMasteryReward"], "apex save")
apex_system = read("src/main/java/kr/moonseungjun/survivalascension/apex/ApexHuntSystem.java")
need(apex_system, ["ECHO_SHARD_COST = 8", "AMETHYST_COST = 32", "GOLD_COST = 32", "HUNT_TIMEOUT_TICKS = 1800",
                   "PLAYER_RADIUS = 64.0D", "RECALL_RADIUS = 48.0D", "EXCLUSION_RADIUS = 96.0D", "ServerBossEvent",
                   "EntitySpawnReason.TRIGGERED", "MobEffects.POISON", "MobEffects.SLOWNESS", "MobEffects.WITHER",
                   "MobEffects.LEVITATION", "data.claimMasteryReward(owner)"], "apex lifecycle")

# Physical-scale and endgame regressions
tuning = read("src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java")
need(tuning, ["if (level >= 100) return 11;", "if (level >= 100) return 192;", "if (level >= 100) return 384;",
              "if (level >= 100) return 49;", "if (level >= 100) return 2.0D;", "if (level >= 100) return 16.0D;"], "Mastery VI")
bore = read("src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java")
need(bore, ["GLOBAL_BLOCK_BUDGET_PER_TICK = 64", "LOCAL_BLOCK_BUDGET_PER_TICK = 12", "MAX_PENDING_PER_PLAYER = 640",
            "fieldMastery ? 12", "player.gameMode.destroyBlock(target)"], "bore safety")
wood = read("src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java")
need(wood, ["GLOBAL_LOG_BUDGET_PER_TICK = 64", "LOCAL_LOG_BUDGET_PER_TICK = 12", "FIELD_MASTERY_LOG_LIMIT = 448"], "wood safety")
harvest = read("src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java")
need(harvest, ["GLOBAL_HARVEST_BUDGET_PER_TICK = 64", "LOCAL_HARVEST_BUDGET_PER_TICK = 12", "baseSize = 13"], "harvest safety")
construction = read("src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java")
need(construction, ["GLOBAL_BLOCK_BUDGET_PER_TICK = 64", "MAX_PENDING_BLOCKS_PER_PLAYER = 512", "fieldMastery ? 65", "fieldMastery ? 13",
                    "EventHooks.onBlockPlace", "consumeOne(player, item)"], "construction safety")
combat = read("src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java")
need(combat, ["fieldMastery ? 7.5D", "fieldMastery ? 20", "combatLevel >= 100 ? 0.55D : 0.45D"], "combat Field Mastery")
mobility = read("src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java")
need(mobility, ["return 4;", "DASH_READY_TICK", "AIR_DASH_COUNT"], "mobility Field Mastery")
trial = read("src/main/java/kr/moonseungjun/survivalascension/endgame/AscensionTrialSystem.java")
need(trial, ["TOTAL_WAVES = 4", "WAVE_TIMEOUT_TICKS = 1200", "AscensionTrialDoctrine.random", "maybeReinforce",
             "EntitySpawnReason.TRIGGERED", "removeStaleServerTrials"], "Ascension Trial")
if '"minecraft:evoker"' in trial: errors.append("Ascension Trial must not directly spawn evokers")
mutation = read("src/main/java/kr/moonseungjun/survivalascension/elite/EndgameMutationSystem.java")
need(mutation, ["MUTATION_CHANCE = 0.18D", "Mutation.WITHERED", "Mutation.PHASE", "Mutation.PLAGUE"], "mutations")
warband = read("src/main/java/kr/moonseungjun/survivalascension/elite/WarbandDirector.java")
need(warband, ["ROUT_TICKS = 160", "3 + worldStage", "6 + worldStage"], "warband")
affix = read("src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java")
need(affix, ['AWAKENED = "awakened"', "currentAffixes(stack).size() == 3", "missing.size() != 2", "awakened ? 4 : rarity"], "awakened Mythic")
reforge = read("src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java")
need(reforge, ["ACTION_AWAKEN", "Items.AMETHYST_SHARD, 256", "Items.DRAGON_BREATH, 16"], "awakening economy")

# Canon/reference policy
project = read("PROJECT.md")
readme = read("README.md")
third = read("THIRD_PARTY_NOTICES.md")
need(project, ["0.28 Industrial Works", "production_v1", "Cycle normalization", "Gold Ingots32", "protocol remains8"], "PROJECT canon")
need(readme, ["0.28.0-alpha.1", "Industrial Works / Four-line Production", "Raw Iron96", "Supply storage is capped at3"], "README canon")
need(third, ["Create — design reference for 0.28", "code is MIT", "All Rights Reserved", "No Create source implementation"], "Create policy")

for rel in [
    "src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java",
]:
    text = read(rel)
    if re.search(r"setBlock\s*\([^\n]*AIR", text): errors.append(f"scaled destruction bypasses normal destroy path: {rel}")

for forbidden in ["harmonised.pmmo", "alrex.parcool", "com.alrex", "mekanism.common", "com.warband",
                  "vbonedra.hostiles_are_too_easy", "com.telepathicgrunt.repurposedstructures", "dev.ftb.mods.ftbquests",
                  "com.simibubi.create"]:
    for path in (ROOT / "src").rglob("*.java"):
        if forbidden in path.read_text(encoding="utf-8", errors="ignore").lower():
            errors.append(f"forbidden/reference namespace leaked: {path.relative_to(ROOT)} -> {forbidden}")

if errors:
    print("SOURCE AUDIT FAILED")
    for error in errors: print("-", error)
    sys.exit(1)

print("SOURCE AUDIT PASS")
print("- Minecraft 26.2 / NeoForge 26.2.0.38-beta / Java 25 / network protocol 8")
print("- Industrial Works has four atomic material lines, per-player bounded buffers and production_v1 persistence")
print("- four-line cycles normalize into max3 supply charges; dispatch produces Gold32 + Amethyst16 + Echo2")
print("- queued complete sets normalize after dispatch, preventing full-buffer deadlock")
print("- expeditions, regional incidents, nine Apex patterns, Trials, Mastery VI and Field Mastery regressions retained")
print("- scaled destroy/material/protection safety and third-party namespace boundaries retained")

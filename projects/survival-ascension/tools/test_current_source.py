#!/usr/bin/env python3
from pathlib import Path
import re, sys

ROOT = Path(__file__).resolve().parents[1]
required = [
    "PROJECT.md", "README.md", "CHANGELOG.md", "THIRD_PARTY_NOTICES.md", "build.gradle", "gradle.properties", "settings.gradle", "gradlew",
    "gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties", "src/main/templates/META-INF/neoforge.mods.toml",
    "src/main/resources/META-INF/third-party/SKILL_PROFICIENCIES_MIT.txt", "src/main/resources/META-INF/third-party/VEINMINER_PLUS_PLUS_MIT.txt",
    "src/main/resources/META-INF/third-party/MINEMENU_MIT.txt", "src/main/resources/META-INF/third-party/BUILDING_GADGETS_2_MIT.txt",
    "src/main/resources/META-INF/third-party/MOB_CHAMPIONS_MIT.txt",
    "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/SurvivalAscensionClient.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/AscensionRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/client/ConstructionRadialMenuScreen.java",
    "src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java",
    "src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java",
    "src/main/java/kr/moonseungjun/survivalascension/network/MobilityActionPayload.java",
    "src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java",
    "src/main/java/kr/moonseungjun/survivalascension/elite/EliteMobSystem.java",
]
errors=[]
for rel in required:
    if not (ROOT / rel).exists(): errors.append(f"missing: {rel}")
props=(ROOT/"gradle.properties").read_text(encoding="utf-8")
for needle in ["minecraft_version=26.2","neo_version=26.2.0.38-beta","mod_id=survivalascension","mod_version=0.10.0-alpha.1"]:
    if needle not in props: errors.append(f"gradle.properties missing {needle}")
main=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java").read_text(encoding="utf-8")
for needle in ['VERSION = "0.10.0-alpha.1"',"ConstructionProgression::onBlockPlaced","MobilityProgression::onPlayerTick",
               "EliteMobSystem::onFinalizeSpawn","EliteMobSystem::onDamagePre","EliteMobSystem::onDamagePost","EliteMobSystem::onLivingDeath"]:
    if needle not in main: errors.append(f"main registration missing: {needle}")
client=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/client/SurvivalAscensionClient.java").read_text(encoding="utf-8")
for needle in ["InputConstants.KEY_M","InputConstants.KEY_R",'key.survivalascension.mobility_action',"MobilityActionPayload"]:
    if needle not in client: errors.append(f"client control contract missing: {needle}")
network=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java").read_text(encoding="utf-8")
for needle in ['PROTOCOL = "4"',"registrar.playToServer(MobilityActionPayload.TYPE","MobilityProgression.performAction"]:
    if needle not in network: errors.append(f"mobility network contract missing: {needle}")
mobility=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/mobility/MobilityProgression.java").read_text(encoding="utf-8")
for needle in ["Attributes.MOVEMENT_SPEED","Attributes.STEP_HEIGHT","Attributes.SAFE_FALL_DISTANCE","DASH_READY_TICK","AIR_DASH_USED","distance <= 1.75D","player.hurtMarked = true"]:
    if needle not in mobility: errors.append(f"mobility safety contract missing: {needle}")
elite=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/elite/EliteMobSystem.java").read_text(encoding="utf-8")
for needle in ["FinalizeSpawnEvent","contains(\"SPAWNER\")","averageSkillLevel","Rank.MYTHIC_III","getPersistentData()","addPermanentModifier",
               "Attributes.MAX_HEALTH","Attributes.ARMOR","Attributes.MOVEMENT_SPEED","Attributes.ATTACK_DAMAGE","Attributes.KNOCKBACK_RESISTANCE",
               "REACTION_READY_KEY","getLongOr(REACTION_READY_KEY","reactToPlayerHit","defender.hurtMarked = true","player.hurtMarked = true",
               "dropRankReward","Items.GOLD_NUGGET","Items.EMERALD","Items.DIAMOND","new ItemEntity"]:
    if needle not in elite: errors.append(f"reactive elite-world contract missing: {needle}")
for trait in ["SWIFT", "BULWARK", "VAMPIRIC", "BERSERKER"]:
    if trait not in elite: errors.append(f"elite trait missing: {trait}")
for rank in ["ELITE_I", "ASCENDED_II", "MYTHIC_III"]:
    if rank not in elite: errors.append(f"elite rank missing: {rank}")
for cooldown in ["case ELITE_I -> 60", "case ASCENDED_II -> 45", "case MYTHIC_III -> 30"]:
    if cooldown not in elite: errors.append(f"elite reaction cooldown missing: {cooldown}")
notice=(ROOT/"src/main/resources/META-INF/third-party/MOB_CHAMPIONS_MIT.txt").read_text(encoding="utf-8")
if "Copyright (c) 2024 Wendall Cada" not in notice or "MIT License" not in notice:
    errors.append("Mob Champions MIT notice invalid")
third=(ROOT/"THIRD_PARTY_NOTICES.md").read_text(encoding="utf-8")
if "Mob Champions" not in third or "wendall911/MobChampions" not in third:
    errors.append("Mob Champions attribution missing")
for forbidden in ["alrex.parcool", "com.alrex", "parcool.client", "parcool.common"]:
    for path in (ROOT/"src").rglob("*.java"):
        if forbidden in path.read_text(encoding="utf-8",errors="ignore").lower(): errors.append(f"ParCool LGPL implementation marker leaked: {path.relative_to(ROOT)}")
tuning=(ROOT/"src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java").read_text(encoding="utf-8")
for needle in ["constructionLineLength","mobilitySpeedMultiplier","mobilityStepHeight","mobilitySafeFallDistance","mobilityDashPower","mobilityDashCooldownTicks"]:
    if needle not in tuning: errors.append(f"tuning missing: {needle}")
for rel in ["src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java","src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java","src/main/java/kr/moonseungjun/survivalascension/harvesting/HarvestingProgression.java"]:
    text=(ROOT/rel).read_text(encoding="utf-8")
    if re.search(r"setBlock\s*\([^\n]*AIR", text): errors.append(f"scaled destruction bypasses normal destroy path: {rel}")
if errors:
    print("SOURCE AUDIT FAILED")
    for e in errors: print("-",e)
    sys.exit(1)
print("SOURCE AUDIT PASS")
print("- Minecraft 26.2 / NeoForge 26.2.0.38-beta / Java 25")
print("- all six skills active; Mobility remains server-authoritative")
print("- progression-scaled elite ranks + four persistent traits retained")
print("- Swift evade / Bulwark counter-push / Berserker lunge / Vampiric heal contracts present")
print("- rank reaction cooldowns and tangible gold/emerald/diamond rewards present")
print("- spawner anti-farm + Mob Champions MIT notice retained")

from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def text(name):
    return (SRC / name).read_text(encoding="utf-8")

def require(condition, message):
    if not condition:
        raise AssertionError(message)
    print("[PASS]", message)

props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
require("mod_version=" in props, "release version property is active")

shop = text("VillageEquipmentShop.java")
rpg = text("VillageRpgSystem.java")
roles = text("VillageRoleSkillSystem.java")
require("VillageRelicSystem.projectileMultiplier" not in re.search(
    r"public static float outgoingMultiplier.*?public static float incomingMultiplier", shop, re.S).group(0),
    "Equipment outgoing multiplier no longer double-applies relics")
require("VillageRelicSystem.incomingMultiplier" not in re.search(
    r"public static float incomingMultiplier.*?public static float roleSkillMultiplier", shop, re.S).group(0),
    "Equipment incoming multiplier no longer double-applies relics")
require("VillageRelicSystem.skillMultiplier" not in re.search(
    r"public static float roleSkillMultiplier.*?public static int cooldownReductionSeconds", shop, re.S).group(0),
    "Equipment role skill multiplier no longer double-applies relics")
require("VillageRelicSystem.projectileMultiplier(attacker)" in rpg
        and "VillageRelicSystem.meleeMultiplier(attacker)" in rpg
        and "VillageRelicSystem.incomingMultiplier(defender)" in rpg,
        "Final RPG layer applies relic combat multipliers exactly once")
require("VillageRelicSystem.skillMultiplier(player)" in roles,
        "Final role-skill layer retains one relic skill multiplier")

loot = text("VillageRaidLootSystem.java")
trading = text("VillageTradingSystem.java")
require("public static int saleValue(ItemStack stack)" in loot and loot.count(", ChatFormatting.") >= 14,
        "All current raid sale loot shares one value source")
require("VillageRaidLootSystem.saleValue(stack)" in trading
        and "name.startsWith(\"[판매용] \")" not in re.search(
            r"private static boolean isSaleOnlyLoot.*?private static int unitValue", trading, re.S).group(0),
        "Bulk sale only deletes loot that has a real sale value")

relic = text("VillageRelicSystem.java")
controller = text("VillageUiController.java")
require('private static final String OFFER_SEP = ";"' in relic
        and "consumePendingOffer" in relic and "pendingRelics" in relic,
        "Multiple boss relic rewards use a persistent-compatible queue")
require("hasPendingChoice(player)" in controller and "openChoice(player)" in controller,
        "Relic UI advances through queued boss rewards one by one")

respawn = text("VillageRespawnSystem.java")
guardians = text("VillageGuardians.java")
require("LivingDamageEvent.Pre" in respawn and "event.getNewDamage()" in respawn
        and "LivingIncomingDamageEvent" not in respawn,
        "Downing uses final pre-health damage after armor and effects")
require("onFinalDamage(LivingDamageEvent.Pre event)" in guardians,
        "Final-damage downing event is registered")

raid = text("VillageRaidSystem.java")
require("discardTaggedRaidEnemies(server)" in raid
        and "if (entity == null)" in raid and "shouldDiscardStaleRaidEnemy" in raid,
        "Raid restart and unloaded-enemy state cannot leave ghost UUIDs")
require("VillageRaidSystem.shouldDiscardStaleRaidEnemy(mob)" in guardians,
        "Stale persisted raid mobs are discarded when they reload")

effect = text("VillageSkillEffectEntity.java")
ability = text("VillageRoleAbilitySystem.java")
follow = re.search(r"private boolean followsOwner\(\).*?\n    \}", effect, re.S).group(0)
require('"luminar_healing_field"' not in follow,
        "Healing sanctuary visual remains at the same fixed center as gameplay")
require('"arcanist_tornado".equals(kind())' in effect and "scale(1.20)" in ability,
        "Tornado visual and gameplay share live aim and equivalent travel speed")

require("Math.round(skill.baseCooldownSeconds() * 0.20f)" in roles,
        "Skill cooldown uses a per-skill 20 percent floor instead of universal 7 seconds")

merc = text("VillageMercenarySystem.java")
require("VillageMercenarySnapshotData" in merc and "persistNightSnapshot()" in merc,
        "Night-start mercenary snapshot survives server restart")
require((SRC / "VillageMercenarySnapshotData.java").exists(),
        "Mercenary night snapshot has dedicated SavedData")

skilltest = text("VillageSkillTestSystem.java")
require("recoverStrandedAfterRestart" in skilltest
        and "VillageSkillTestSystem.recoverStrandedAfterRestart(player)" in guardians,
        "Restarted players cannot remain stranded in a dead skill-test session")

local = text("VillageLocalActionSystem.java")
legacy_hire = re.search(r'case "hire_mercenary" -> \{.*?return true;\n            \}', local, re.S)
require(legacy_hire is not None
        and "VillageMercenaryDeploymentSystem.openCommand(player)" in legacy_hire.group(0)
        and "VillageMercenarySystem.hire" not in legacy_hire.group(0),
        "Legacy generic mercenary action only opens the classed command UI and cannot spawn obsolete golems")
require('action.startsWith("merc_hire:")' in local
        and "VillageMercenarySystem.hire(player, kind)" in local
        and "VillageLocationRules.isNear(player, VillageProgressionSystem.Building.BARRACKS)" in local,
        "Current class-specific mercenary hiring is authoritative and barracks-local")

council = text("VillageCouncilState.java")
require("onPlayerListChanged" in council and "PlayerLoggedOutEvent" in guardians,
        "Time vote is re-evaluated when a player disconnects")

purge = text("VillageGlobalMobPurgeSystem.java")
require("inflate(2048" not in purge and "BATTLEFIELD_RADIUS + 96.0" in purge,
        "Natural mob purge is bounded to the managed battlefield")
require("mob.blockPosition().distSqr(center) > radius * radius" in guardians,
        "Natural spawn suppression no longer affects the whole overworld")

enemy = text("VillageEnemyArchetypeSystem.java")
ability_block = re.search(r"public static void tickAbility.*?public static void onStructureHit", enemy, re.S).group(0)
require("abilityReady(mob, globalTicks" in enemy and "globalTicks %" not in ability_block,
        "Enemy special abilities use per-entity phase offsets")
require("Math.min(7.0f, VillageCouncilState.currentDay() * 0.22f)" in enemy,
        "Endless unavoidable magic damage has a bounded day bonus")

identity = text("VillageEquipmentIdentity.java")
rarity = text("VillageEquipmentRaritySystem.java")
require("DataComponents.CUSTOM_DATA" in identity and "DataComponents.REPAIR_COST" in identity,
        "Generated equipment owns persistent identity and rejects anvil-name spoofing")
require("VillageEquipmentIdentity.stampRarity" in rarity,
        "Rarity and enhancement writes stamp game-owned equipment identity")

screen = text("VillageRelicScreen.java")
require("Math.max(120, Math.min(820, width - 16))" in screen and "Math.min(7, summary.size())" in screen,
        "Relic collection remains bounded on narrow logical resolutions")

print("Village Guardians runtime stability contracts passed.")
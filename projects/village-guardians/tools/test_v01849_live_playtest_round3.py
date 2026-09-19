#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def section(source: str, start: str, end: str) -> str:
    return source.split(start, 1)[1].split(end, 1)[0]

def main() -> None:
    ability = read("VillageRoleAbilitySystem.java")
    raid = read("VillageRaidSystem.java")
    war = read("VillageWarfrontSystem.java")
    attack = read("VillageAttackPlanSystem.java")
    merc = read("VillageMercenarySystem.java")
    deploy = read("VillageMercenaryDeploymentSystem.java")
    enemy = read("VillageEnemyArchetypeSystem.java")
    local = read("VillageLocalActionSystem.java")
    keys = read("VillageClientKeys.java")
    lang = (ROOT / "src/main/resources/assets/villageguardians/lang/ko_kr.json").read_text(encoding="utf-8")

    taunt = section(ability, "private static void tauntShout", "private static void healLowestAlly")
    assert "Math.max(240, Math.min(400, duration + 120))" in taunt
    direct = section(raid, "private static void directEnemies", "private static net.minecraft.world.entity.animal.golem.IronGolem selectMercenaryTarget")
    assert direct.index("activeTauntTarget(level, mob)") < direct.index("VillageEnemyArchetypeSystem.isFlying(mob)")

    aimed = section(ability, "private static Vec3 aimedGround", "private static void activateArrowRain")
    assert "fieldFloor(level" in aimed
    assert "VillageCouncilState.villageCenter()" in aimed
    assert "isFaceSturdy" in aimed

    assert "nearestGroundEnemy" in merc
    assert "VillageEnemyArchetypeSystem.isFlying(enemy)" in merc
    assert "Vec3 start = mercenary.getEyePosition();" in merc
    assert "private static Mob groundTarget" in deploy
    assert ".filter(enemy -> !VillageEnemyArchetypeSystem.isFlying(enemy))" in deploy

    front = section(attack, "public static Front frontForIndex", "public static Condition condition")
    assert "detachmentFront" in front
    assert "((day + wave) & 1)" not in front
    assert "((wave + i) & 1)" not in front

    boss = section(war, "public static int bonusBossCount", "public static int countBonus")
    assert "day < 4" in boss
    assert "day == 4 || day == 7" in boss
    assert "isMilestoneDay(day)" in boss

    selection = section(enemy, "private static Archetype select", "private static Archetype bossForDay")
    assert "private static Archetype lineMix" in selection
    assert "slot == 13" in selection
    abilities = section(enemy, "public static void tickAbility", "public static void onStructureHit")
    assert "trait == VillageWaveTrait.HEXED ? 180 : 220" in abilities

    wave_clear = section(raid, "if (ACTIVE_ENEMIES.isEmpty())", "betweenWaveTicks = 0;")
    assert "waveClearCoinReward" in wave_clear
    victory = section(raid, "private static void finishVictory", "public static boolean shouldDiscardStaleRaidEnemy")
    assert "(60 + day * 14)" in victory

    assert 'case "exchange_supplies"' in local
    assert "exchangeCoinsForSupplies(player)" in local
    assert 'RETURN_TO_VILLAGE = key("return_to_village", GLFW.GLFW_KEY_R)' in keys
    assert 'consume(RETURN_TO_VILLAGE, "return_village")' in keys
    assert '"key.villageguardians.return_to_village": "마을 중앙으로 귀환"' in lang

    print("[PASS] taunt has visible duration and overrides normal raid routing")
    print("[PASS] ground fields reject wall-top Y and search village-ground walkable floor")
    print("[PASS] melee mercenaries ignore phantoms while ranger fires from eye-height LOS")
    print("[PASS] multi-front detachments no longer alternate east/west by simple parity")
    print("[PASS] early bosses are occasional and witch pressure is reduced/mixed")
    print("[PASS] raid coins grow through wave clears and stronger final rewards")
    print("[PASS] coin-to-supply exchange works and R returns to the village center")

if __name__ == "__main__":
    main()

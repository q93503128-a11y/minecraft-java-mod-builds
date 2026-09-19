#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def section(source: str, start: str, end: str) -> str:
    return source.split(start, 1)[1].split(end, 1)[0]

def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    identity = read("VillageEquipmentIdentity.java")
    rarity = read("VillageEquipmentRaritySystem.java")
    expanded = read("VillageExpandedEquipmentSystem.java")
    shop = read("VillageEquipmentShop.java")
    tooltip = read("VillageEquipmentTooltipClient.java")
    rpg = read("VillageRpgSystem.java")
    guardians = read("VillageGuardians.java")

    assert "mod_version=0.18.45-alpha.1" in props
    assert "현재 소스 버전 `0.18.45-alpha.1`" in readme
    assert "villageguardians-0.18.45-alpha.1.jar" in readme

    assert "KEY_POWER_TIER" in identity
    assert "stampPowerTier" in identity
    assert "public static int powerTier" in identity

    assert "combatTierForDay" in rarity
    assert "safe >= 15" in rarity and "safe >= 10" in rarity and "safe >= 5" in rarity
    assert "case 2 -> 1.50f" in rarity and "case 3 -> 3.00f" in rarity and "case 4 -> 5.00f" in rarity
    assert "case 2 -> 1.25f" in rarity and "case 3 -> 2.50f" in rarity and "case 4 -> 4.00f" in rarity
    assert "combatTier(second) != combatTier" in rarity
    assert "createNamed(item, rarity.next(), name, combatTier)" in rarity
    assert "combatTierForDay(safeDay)" in expanded
    assert "public int combatTier()" in shop

    assert "전장 단계:" in tooltip
    assert "기본 근접 피해" in tooltip and "기본 원거리 피해" in tooltip
    incoming = section(rpg, "public static void handleIncomingDamage", "public static void handleDeath")
    assert "flatAttackBonus(attacker, projectile)" in incoming
    assert "(event.getAmount() + flatWeaponPower) * value" in incoming

    # Raid XP is granted once to every frozen night participant, regardless of killer.
    death_bus = section(guardians, "public void onLivingDeath", "public void onArrowLoose")
    assert "for (var playerId : VillageProgressionSystem.nightParticipants(server))" in death_bus
    assert "VillageCouncilState.grantExperience(server, playerId, sharedExperience)" in death_bus
    death = section(rpg, "public static void handleDeath", "public static String useRoleSkill")
    assert "boolean raidEnemy = VillageRaidSystem.isRaidEnemy(defeated)" in death
    assert "if (!raidEnemy)" in death
    assert "result = VillageCouncilState.grantExperience(killer, reward)" in death

    print("[PASS] equipment has a separate I-IV combat tier that adds flat base weapon damage")
    print("[PASS] bows/crossbows gain real late-game base damage instead of relying only on percentage multipliers")
    print("[PASS] fusion preserves combat tier and tooltips expose tier/base damage")
    print("[PASS] raid kill XP is equal party XP with no extra player-killer XP path")

if __name__ == "__main__":
    main()

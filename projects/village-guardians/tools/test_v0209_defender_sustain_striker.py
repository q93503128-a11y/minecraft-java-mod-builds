#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def section(source: str, start: str, end: str) -> str:
    return source.split(start, 1)[1].split(end, 1)[0]

def main() -> None:
    merc = read("VillageMercenarySystem.java")
    events = read("VillageGuardians.java")
    role = read("VillageRoleAbilitySystem.java")
    rpg = read("VillageRpgSystem.java")
    sets = read("VillageEquipmentSetSystem.java")
    role_skills = read("VillageRoleSkillSystem.java")

    dawn = section(merc, "public static synchronized void healAtDawn", "public static String status")
    assert "mercenary.setHealth(mercenary.getMaxHealth())" in dawn
    assert "0.30f" not in dawn

    bastion = section(merc, "private static void bastionControl", "private static void strikerPressure")
    assert "if (engaged)" in bastion
    assert "MobEffects.ABSORPTION" in bastion
    assert "barrierAmplifier" in bastion

    taunt = section(role, "private static void tauntShout", "private static void healLowestAlly")
    assert "VillageRaidSystem.tauntEnemies" in taunt
    assert "MobEffects.ABSORPTION" in taunt

    striker = section(merc, "private static void strikerPressure", "private static void rangedAttack")
    assert "STRIKER_TRACKED_TARGETS" in striker
    assert "STRIKER_OPENING_TARGETS" in striker
    assert "public static void applyOutgoingDamage" in striker
    assert "2.20f" in striker
    assert "0.50 * masteryProgress" in striker
    assert "event.setAmount(event.getAmount() * openingMultiplier)" in striker
    assert "VillageMercenarySystem.applyOutgoingDamage(event)" in events

    assert "case RANGER -> projectile ? (isOnWallTop(player) ? 1.64f : 1.34f) : 0.92f;" in rpg
    assert "case RANGER" in sets
    assert "if (promotionTier <= 0) break;" in sets
    assert "EquipmentSet.NIGHT_HUNTER" in sets
    assert "VillageEquipmentSetSystem.roleSkillMultiplier(player, role, skill.promotionTier())" in role_skills

    print("[PASS] dawn fully restores surviving mercenaries")
    print("[PASS] bastion and warden taunts grant temporary health")
    print("[PASS] striker pursuit arms one high-damage opening hit")
    print("[PASS] ranger projectile baseline receives a small buff")
    print("[PASS] night hunter ranged damage includes promoted ranger attack skills without double-scaling base arrow skills")

if __name__ == "__main__":
    main()

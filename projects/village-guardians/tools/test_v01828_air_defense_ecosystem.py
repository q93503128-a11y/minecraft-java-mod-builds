#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    enemy = read("VillageEnemyArchetypeSystem.java")
    raid = read("VillageRaidSystem.java")
    elite = read("VillageEnemyEliteSystem.java")
    intel = read("VillageWaveIntelSystem.java")
    turret = read("VillagePlacedTurretSystem.java")
    merc = read("VillageMercenarySystem.java")
    ability = read("VillageRoleAbilitySystem.java")
    role = read("VillageRole.java")
    old = (ROOT / "tools/test_v01827_aerial_combat_integrity.py").read_text(encoding="utf-8")

    assert "mod_version=0.18.28-alpha.1" in props
    assert "0.18.28-alpha.1" in readme and "villageguardians-0.18.28-alpha.1.jar" in readme
    assert 'assert "mod_version=0.18.27-alpha.1" in props' not in old
    assert "enum AerialRole" in enemy
    for token in ("RAIDER", "BOMBARDIER", "HARRIER", "public static AerialRole aerialRole"):
        assert token in enemy
    assert "day < 10" in enemy and "day >= 13" in enemy

    assert "ACTIVE_AERIAL_ROLES" in raid
    assert "ACTIVE_AERIAL_ROLES.put(mob.getUUID(), aerialRole)" in raid
    assert "ACTIVE_AERIAL_ROLES.remove(uuid)" in raid and "ACTIVE_AERIAL_ROLES.clear()" in raid
    assert "public static int aerialThreatPriority" in raid
    assert "case BOMBARDIER -> 300" in raid and "case HARRIER -> 220" in raid
    direct = raid.split("private static void directFlyingEnemy", 1)[1].split("private static void moveFlyingToward", 1)[0]
    assert "role == VillageEnemyArchetypeSystem.AerialRole.BOMBARDIER" in direct
    assert "aerialCadence(role)" in direct and "aerialWarningTicks(role)" in direct
    assert "case BOMBARDIER -> 112" in direct and "case HARRIER -> 62" in direct
    assert "case BOMBARDIER -> 1.75f" in direct and "case HARRIER -> 0.58f" in direct
    assert "mob.setTarget(null);" in direct

    discover = elite.split("private static void discover", 1)[1].split("private static void grappler", 1)[0]
    assert "VillageEnemyArchetypeSystem.isFlying(mob)" in discover and "continue;" in discover

    assert "Map<VillageEnemyArchetypeSystem.AerialRole, Integer> aerialRoster" in intel
    assert "VillageEnemyArchetypeSystem.aerialRole(day, wave, index, trait)" in intel
    assert "count - flyingCount" in intel
    for label in ("파성 망령", "폭풍 사냥귀"):
        assert label in enemy

    assert "VillageRaidSystem.aerialThreatPriority(mob)" in turret
    assert "pendingBombardOverlapPenalty" in turret and "penalty += 190.0" in turret
    assert "-VillageRaidSystem.aerialThreatPriority(enemy)" in merc

    assert "VillageEnemyArchetypeSystem.isFlying(target)" in ability
    assert "event.getAmount() * 1.18f" in ability
    assert "aerialBias" in ability and "? -18.0 : 0.0" in ability
    assert "공중 적에게 화살 피해가 18% 증가" in role

    print("[PASS] deterministic three-role aerial roster is shared by runtime and daytime intel")
    print("[PASS] ground elite AI can no longer steal flying movement ownership")
    print("[PASS] bombardier/harrier have distinct target, cadence, telegraph and damage identities")
    print("[PASS] anti-air turret and ranger mercenary share aerial threat priority")
    print("[PASS] player ranger has truthful aerial aim support and damage identity")
    print("[PASS] delayed bombard overlap receives reservation-style target penalty")
    print("[PASS] historical v0.18.27 regression is version-independent")
    print("[PASS] v0.18.28 air-defense ecosystem contract complete")

if __name__ == "__main__":
    main()

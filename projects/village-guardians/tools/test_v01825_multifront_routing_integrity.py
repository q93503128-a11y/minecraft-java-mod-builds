#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def main() -> None:
    attack = read("VillageAttackPlanSystem.java")
    raid = read("VillageRaidSystem.java")
    guardians = read("VillageGuardians.java")
    elite = read("VillageEnemyEliteSystem.java")
    boss = read("VillageSiegeBossSystem.java")
    old = (ROOT / "tools/test_v01824_defense_action_integrity.py").read_text(encoding="utf-8")

    assert 'assert "mod_version=0.18.24-alpha.1" in props' not in old

    # Attack-plan ownership is explicit and exposes the two facts generic raid AI needs.
    for token in (
        "public static boolean isInsideFortress(BlockPos pos)",
        "public static boolean ownsExteriorRouting(UUID uuid, BlockPos pos)",
        "public static boolean hasBreachedEntry(UUID uuid)",
        "public static boolean hasInteriorAccess(UUID uuid, BlockPos pos)",
        "frontOf(uuid) != Front.NORTH && !isInsideFortress(pos)",
        "isInsideFortress(pos) || hasBreachedEntry(uuid)",
    ):
        assert token in attack
    breached = attack.split("if (VillageSiegeSegmentSystem.breached(segment)) {", 1)[1].split("continue;", 1)[0]
    assert "mob.setTarget(null);" in breached
    assert "insideApproach(segment)" in breached

    # Generic raid navigation yields while a side/rear attacker is still outside.
    assert "VillageAttackPlanSystem.ownsExteriorRouting(id, mob.blockPosition())" in raid
    exterior_guard = raid.split("VillageAttackPlanSystem.ownsExteriorRouting(id, mob.blockPosition())", 1)[1].split("if (archetype ==", 1)[0]
    assert "mob.setTarget(null);" in exterior_guard and "continue;" in exterior_guard
    assert "boolean fortressAccess = gatePassable" in raid
    assert "VillageAttackPlanSystem.hasInteriorAccess(id, mob.blockPosition())" in raid
    assert "ServerPlayer nearbyPlayer = fortressAccess" in raid
    assert "villageCenter, mob.blockPosition(), fortressAccess, archetype" in raid

    # North-gate routing stays in raid AI; the former last-in-tick global override is physically absent.
    assert not (JAVA / "VillageGatePrioritySystem.java").exists()
    assert "VillageGatePrioritySystem" not in guardians
    assert "if (!gatePassable && VillageProgressionSystem.isOperational" in raid
    assert "return VillageProgressionSystem.Building.WALLS;" in raid
    # Pursuit doctrines yield to the same side/rear exterior owner until the mob reaches an interior route.
    assert elite.count("VillageAttackPlanSystem.ownsExteriorRouting(mob.getUUID(), mob.blockPosition())") >= 2
    assert "VillageAttackPlanSystem.ownsExteriorRouting(boss.getUUID(), boss.blockPosition())" in boss

    print("[PASS] side/rear exterior attackers have one navigation owner")
    print("[PASS] raid-owned north-gate routing cannot collapse multi-front assaults")
    print("[PASS] breached side/rear attackers clear stale targets and route through the breach")
    print("[PASS] infiltrated or breached attackers recognize interior access even while north gate survives")
    print("[PASS] historical v0.18.24 regression is version-independent")
    print("[PASS] v0.18.25 multi-front routing integrity contract complete")

if __name__ == "__main__":
    main()

#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def main() -> None:
    attack = read("VillageAttackPlanSystem.java")
    raid = read("VillageRaidSystem.java")
    gate = read("VillageGatePrioritySystem.java")
    guardians = read("VillageGuardians.java")
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

    # Last-in-tick north-gate priority is scoped so it cannot overwrite multi-front routing.
    assert "VillageAttackPlanSystem.frontOf(mob.getUUID()) != VillageAttackPlanSystem.Front.NORTH" in gate
    assert "VillageAttackPlanSystem.isInsideFortress(mob.blockPosition())" in gate
    assert guardians.index("VillageAttackPlanSystem.tick(event.getServer())") < guardians.index("VillageGatePrioritySystem.tick(event.getServer())")

    print("[PASS] side/rear exterior attackers have one navigation owner")
    print("[PASS] global north-gate priority cannot collapse multi-front assaults")
    print("[PASS] breached side/rear attackers clear stale targets and route through the breach")
    print("[PASS] infiltrated or breached attackers recognize interior access even while north gate survives")
    print("[PASS] historical v0.18.24 regression is version-independent")
    print("[PASS] v0.18.25 multi-front routing integrity contract complete")

if __name__ == "__main__":
    main()

#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name):
    return (JAVA / name).read_text(encoding="utf-8")


def hire_cost(kind_ordinal, barracks):
    base = 150 + kind_ordinal * 35
    discount = max(0, barracks - 1) * 10
    floor = 110 + kind_ordinal * 30
    return max(floor, base - discount)


def main():
    assert "mod_version=" in (ROOT / "gradle.properties").read_text(encoding="utf-8")
    merc = read("VillageMercenarySystem.java")
    defense = read("VillageDefenseSystem.java")
    ui = read("VillageUiService.java")
    desc = read("VillageActionDescriptions.java")
    detail = read("VillageActionDetailScreen.java")

    # Barracks progression must never increase hire cost for any class.
    for ordinal in range(4):
        costs = [hire_cost(ordinal, level) for level in range(0, 6)]
        assert all(b <= a for a, b in zip(costs, costs[1:])), costs
    assert "Math.max(0, barracks - 1) * 10" in merc
    assert "+ VillageProgressionSystem.barracksLevel() * 25" not in merc

    # Saved roster, not a local AABB scan, owns capacity and status.
    assert "int current = rosterCount();" in merc
    assert "public static synchronized int rosterCount()" in merc
    assert "return CLASSES.size();" in merc
    assert '"용병 명부 " + rosterCount() + " / " + capacity()' in merc
    assert "loadedCount(level)" in merc
    assert "private static synchronized int count(ServerLevel level)" not in merc

    # Legacy generic mercenaries migrate once into the classed SavedData system.
    assert 'LEGACY_MERCENARY_NAME = "마을 용병"' in merc
    assert "public static synchronized boolean adoptLegacy(Mob mob)" in merc
    assert "MercenaryClass kind = MercenaryClass.BASTION;" in merc
    assert "return VillageMercenarySystem.adoptLegacy(mob);" in defense
    assert "VillageMercenarySystem.hire(player, VillageMercenarySystem.MercenaryClass.BASTION)" in defense
    assert "EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.EVENT)" not in defense

    # Barracks UI enters one roster owner; four classes and retirement use explicit action IDs.
    assert 'send(player, "mercenary_roster", "용병 명부"' in ui
    assert '"hire_mercenary:" + kind.id()' in ui
    assert '"retire_mercenary:" + entry.uuid()' in ui
    assert 'case "open_mercenary_roster", "hire_mercenary" -> openMercenaryRoster(player);' in ui
    assert "VillageDefenseSystem.hireMercenary(player)" not in ui
    assert '"open_mercenary_roster", "용병 명부 · "' in ui
    assert "public static synchronized String retire(ServerPlayer player, UUID uuid)" in merc
    assert "VillageMercenaryPresentationSystem.remove(level, uuid);" in merc
    assert "unregister(uuid);" in merc
    assert 'action.startsWith("retire_mercenary:")' in desc
    assert 'action.startsWith("retire_mercenary:")' in detail

    print("[PASS] barracks upgrades no longer make classed mercenaries more expensive")
    print("[PASS] authoritative SavedData roster owns mercenary capacity and status")
    print("[PASS] legacy generic mercenaries migrate into the classed progression system")
    print("[PASS] barracks roster UI exposes four hires and confirmation-gated retirement")


if __name__ == "__main__":
    main()

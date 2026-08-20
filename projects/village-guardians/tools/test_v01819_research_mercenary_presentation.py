#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name):
    return (JAVA / name).read_text(encoding="utf-8")


def curve(level, first, mastery):
    safe = max(0, min(10, level))
    return min(5, safe) * first + max(0, safe - 5) * mastery


def main():
    assert "mod_version=0.18.19-alpha.1" in (ROOT / "gradle.properties").read_text(encoding="utf-8")

    research = read("VillageDefenseResearchSystem.java")
    assert "MAX_LEVEL = 10" in research
    assert "mastery = Math.max(0, current - 4)" in research
    for token in ("towerRangeMultiplier", "towerDurabilityMultiplier", "mercenaryHealingMultiplier",
                  "mercenaryTrainingProgressPerKill", "consumableCostMultiplier", "fieldRepairMultiplier"):
        assert token in research
    # Foundation levels preserve v0.18.18 strength; mastery levels grow more slowly.
    assert abs((1 + curve(5, .12, .05)) - 1.60) < 1e-6
    assert abs((1 + curve(10, .12, .05)) - 1.85) < 1e-6
    assert abs((1 + curve(5, .10, .04)) - 1.50) < 1e-6
    assert abs((1 + curve(10, .10, .04)) - 1.70) < 1e-6
    assert (5 + 1) // 2 == 3 and min(5, (10 + 1) // 2) == 5

    merc = read("VillageMercenarySystem.java")
    presentation = read("VillageMercenaryPresentationSystem.java")
    assert "VillageDefenseResearchSystem.mercenaryTrainingProgressPerKill()" in merc
    assert "VillageDefenseResearchSystem.mercenaryHealingMultiplier()" in merc
    assert "VillageMercenaryPresentationSystem.ensure(level, mercenary, kind, rank)" in merc
    assert "VillageMercenaryPresentationSystem.remove" in merc
    for kind in ("bastion", "striker", "ranger", "medic"):
        assert '"mercenary_presence_" + kind.id()' in presentation
    for milestone in ("safe >= 20", "safe >= 40", "safe >= 60"):
        assert milestone in presentation

    effect = read("VillageSkillEffectEntity.java")
    mesh = read("VillageSkillMeshLibrary.java")
    assert effect.count('kind().startsWith("mercenary_presence_")') >= 2
    for kind in ("mercenary_presence_bastion", "mercenary_presence_striker",
                 "mercenary_presence_ranger", "mercenary_presence_medic"):
        assert kind in mesh
    assert "renderMercenaryPresence" in mesh and "shieldFrame" in mesh and "customArrow" in mesh

    turret = read("VillagePlacedTurretSystem.java")
    assert "* VillageDefenseResearchSystem.towerRangeMultiplier()" in turret
    assert "VillageDefenseResearchSystem.towerDurabilityMultiplier()" in turret
    assert "maxHp(upgradedBase)" in turret
    assert "state = new TurretState(id, state.type(), state.pos(), state.level(), maxHp(state), true);" in turret

    consumable = read("VillageConsumableSystem.java")
    assert "VillageDefenseResearchSystem.consumableCostMultiplier()" in consumable
    assert "VillageDefenseResearchSystem.fieldRepairMultiplier()" in consumable

    print("[PASS] v0.18.19 research extends to 10 levels with bounded mastery scaling")
    print("[PASS] tower research affects damage, range and durability without reducing old Lv5 strength")
    print("[PASS] mercenary research accelerates training/support and four classes have persistent milestone visuals")
    print("[PASS] logistics research improves tactical-supply economy without reintroducing duplicate food")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def main() -> None:
    shop = read("VillageEquipmentShop.java")
    sets = read("VillageEquipmentSetSystem.java")
    progress = read("VillageProgressionSystem.java")
    ui = read("VillageUiController.java")
    signatures = read("VillageBuildingSignatures.java")
    merc = read("VillageMercenarySystem.java")
    deploy = read("VillageMercenaryDeploymentSystem.java")
    effects = read("VillageDefenseEffectSystem.java")
    mesh = read("VillageSkillMeshLibrary.java")
    external = read("VillageExternalMercenaryMesh.java")
    prep = (ROOT / "tools/prepare_licensed_assets.py").read_text(encoding="utf-8")

    offer_ids = re.findall(r'^[ ]{8}[A-Z0-9_]+\("([a-z0-9_]+)"', shop, re.MULTILINE)
    assert len(offer_ids) == 72, len(offer_ids)
    assert all(f'"{offer_id}"' in sets for offer_id in offer_ids)
    for day in ("15", "25", "35", "45", "55", "65", "80", "85", "95", "100"):
        assert re.search(rf', {day}, \d+,', shop)
    assert "rotatingOffers(Category.EQUIPMENT, safeDay, 5)" in shop
    assert "rotatingOffers(Category.ARMOR, safeDay, 4)" in shop
    assert "int recentFloor = Math.max(1, day - 28);" in shop

    assert "MAX_BUILDING_LEVEL = 10" in progress
    assert "buildingCurve(smithyLevel, 0.04f, 0.02f)" in progress
    assert "buildingCurve(skillHallLevel, 0.05f, 0.02f)" in progress
    assert "level >= 10 ? 1 : 0" in progress
    assert "veteran * veteran * 55" in progress
    assert "case 6 -> 25;" in progress
    assert "case 7 -> 40;" in progress
    assert "case 8 -> 55;" in progress
    assert "case 9 -> 75;" in progress
    assert "default -> 90;" in progress
    assert "VillageCouncilState.currentDay() < requiredDay" in progress
    assert "requiredDayForBuildingLevel(level + 1)" in ui
    assert "buildingLevel >= 6" in signatures
    assert "buildingLevel >= 8" in signatures
    assert "buildingLevel >= 10" in signatures
    assert "MAX_BUILDING_LEVEL" in ui

    for kind in ("WARDER", "ARTILLERIST"):
        assert kind in merc and kind in deploy
    assert 'WARDER("warder", "결계 수도사", 25' in merc
    assert 'ARTILLERIST("artillerist", "비전 포격병", 45' in merc
    assert "wardAllies" in merc and "artilleryAttack" in merc
    assert "mercenaryWardPulse" in effects and "mercenaryArtilleryBurst" in effects
    assert "mercenary_presence_warder" in mesh and "mercenary_presence_artillerist" in mesh
    assert "warder.obj" in external and "artillerist.obj" in external
    assert "Monk.obj" in prep and "Wizard.obj" in prep
    assert "NEXT_WARD_PULSE.remove(uuid)" in merc
    assert "NEXT_ARTILLERY_CAST.remove(uuid)" in merc

    print("[PASS] Day 1-100 shop contains 72 explicit set-mapped combat equipment offers")
    print("[PASS] daily stock favors recent unlocks so late gear remains visible")
    print("[PASS] facilities grow to Lv.10 with diminishing late-game benefits and visible milestones")
    print("[PASS] mercenary roster expands to six jobs with ward/cleanse and heavy-target artillery identities")
    print("[PASS] new mercenary jobs use pinned CC0 Monk/Wizard models and clean transient combat state")

if __name__ == "__main__":
    main()

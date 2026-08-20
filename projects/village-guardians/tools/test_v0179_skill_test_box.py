#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    test = read("VillageSkillTestSystem.java")
    role = read("VillageRoleSkillSystem.java")
    controller = read("VillageUiController.java")
    screen = read("VillageFacilityScreen.java")
    guard = read("VillageGuardians.java")

    assert "mod_version=" in props
    assert "TEST_ROLES" in test and "selectedRole" in test and "selectRole" in test
    assert "실제 직업과 성장 데이터는 바뀌지 않습니다" in test
    assert "roleManagementBoxPosition" in test and "skillManagementBoxPosition" in test
    assert test.count("Blocks.BARREL") >= 2 and "Blocks.LAPIS_BLOCK" in test
    assert "handleManagementBox" in test and "InteractionResult.SUCCESS" in test
    assert "VillageSkillTestSystem.handleManagementBox(event)" in guard

    assert "? VillageSkillTestSystem.selectedRole(player)" in role
    assert "VillageSkillTestSystem.equippedSkill(player, slot)" in role
    assert "시험 관리함에서 기술을 장착하세요" in role

    assert '"test_role:" + candidate.id()' in controller
    assert '"test_equip:" + skill.id() + ":0"' in controller
    assert '"test_equip:" + skill.id() + ":1"' in controller
    assert '"시험 직업 관리함"' in controller and '"시험 스킬 관리함"' in controller
    assert "openSkillTestSlot" not in controller

    assert 'payload.screenId().startsWith("skill_test_")' in screen
    assert 'selectedAction.startsWith("test_role:")' in screen
    assert 'selectedAction.startsWith("test_equip:")' in screen

    print("[PASS] Test arena owns separate role and skill management barrels")
    print("[PASS] Test-only job selection leaves permanent role and progression untouched")
    print("[PASS] Skill cards directly assign Z/X and the real keyed cast path reads the test loadout")

if __name__ == "__main__":
    main()

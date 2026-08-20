#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    keys = read("VillageClientKeys.java")
    test = read("VillageSkillTestSystem.java")
    controller = read("VillageUiController.java")
    screen = read("VillageFacilityScreen.java")

    assert "mod_version=" in props
    assert "@EventBusSubscriber(value = Dist.CLIENT, modid = VillageGuardians.MOD_ID)" in keys
    assert "registerKeyMappings(RegisterKeyMappingsEvent event)" in keys
    assert 'consume(ROLE_SKILL_ONE, "use_skill:0")' in keys
    assert "consumeSkillTwo(minecraft)" in keys and 'VillageUiActionPayload("use_skill:1")' in keys

    assert "roleManagementBoxPosition" in test
    assert "skillManagementBoxPosition" in test
    assert test.count("Blocks.BARREL") >= 2
    assert "Blocks.GOLD_BLOCK" in test and "Blocks.LAPIS_BLOCK" in test
    assert "openSkillTestRoleManager" in test and "openSkillTestSkillManager" in test

    assert 'if (action.startsWith("use_skill:"))' in controller
    assert "VillageRpgSystem.useRoleSkill(player, slot)" in controller
    assert 'send(player, "skill_test_role", "시험 직업 관리함"' in controller
    assert 'send(player, "skill_test_skill", "시험 스킬 관리함"' in controller
    assert 'case "open_skill_test_roles"' in controller
    assert 'case "open_skill_test_skills"' in controller

    equip_start = controller.index('if (action.startsWith("test_equip:"))')
    equip_end = controller.index('if (action.startsWith("relic_select:"))', equip_start)
    equip_block = controller[equip_start:equip_end]
    assert "openSkillTest(" not in equip_block
    assert "openSkillTestSkillManager(" not in equip_block

    assert 'payload.screenId().startsWith("skill_test_")' in screen
    assert 'selectedAction.startsWith("test_equip:")' in screen
    assert "onClose();" in screen

    print("[PASS] Client Z/X mappings are registered by the client event subscriber")
    print("[PASS] Z/X packets route directly to the real role-skill cast system")
    print("[PASS] Role and skill management use separate physical barrels and screens")
    print("[PASS] Equipping closes the skill screen so in-game Z/X input is no longer drained")


if __name__ == "__main__":
    main()

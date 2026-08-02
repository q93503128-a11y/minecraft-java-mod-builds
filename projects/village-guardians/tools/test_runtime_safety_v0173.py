#!/usr/bin/env python3
"""Focused v0.17.3 source contracts after compact UI migration."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    controller = read("VillageUiController.java")
    service = read("VillageUiService.java")
    facility = read("VillageFacilityScreen.java")
    town = read("VillageTownHallScreen.java")
    common_tree = read("VillageSkillTreeScreen.java")
    role_tree = read("VillageRoleProgressScreen.java")
    inventory = read("VillageInventoryPanel.java")
    keys = read("VillageClientKeys.java")
    descriptions = read("VillageActionDescriptions.java")
    role_system = read("VillageRoleSkillSystem.java")
    starter = read("VillageStarterKit.java")

    assert "PANEL = 0xFFE4D8BF" in facility
    assert "CARD_HEIGHT = 30" in facility
    assert "ACTION_HEIGHT = 20" in facility
    assert "Math.min(108" in facility
    assert "PANEL = 0xFFE4D8BF" in town
    assert "CARD_HEIGHT = 30" in town
    assert "Math.min(112" in town

    assert "renderDetail" not in common_tree
    assert "Bubble" in common_tree
    assert "renderTreeFooter" not in role_tree
    assert "renderSkillFooter" not in role_tree
    assert "TreeBubble" in role_tree and "SkillBubble" in role_tree
    assert "SkillGrid" in role_tree and "CardBounds" in role_tree
    assert "font.width" in common_tree and "font.width" in role_tree

    assert '"open_skill_tree"' in inventory
    assert '"open_personal_progress"' not in inventory
    assert "B 통신 · Z/X 기술" in inventory
    assert "성장 J" in inventory
    for key in ("GLFW_KEY_Z", "GLFW_KEY_X", "GLFW_KEY_B", "GLFW_KEY_H", "GLFW_KEY_J", "GLFW_KEY_K", "GLFW_KEY_U"):
        assert key in keys
    for old_key in ("GLFW_KEY_I", "GLFW_KEY_P", "GLFW_KEY_O", "GLFW_KEY_V", "GLFW_KEY_C"):
        assert old_key not in keys

    personal = controller.split("public static void openPersonalProgress", 1)[1].split("public static void openSkillTree", 1)[0]
    assert "openSkillTree(player);" in personal
    assert "장비 강화는 대장간 단말기 근처에서만 가능합니다." in controller
    assert "VillageProgressionSystem.Building.SMITHY" in controller
    assert "기술 장착은 기술 연구소에서만 가능합니다." not in service
    assert "직업 기술 습득은 기술 연구소에서만 가능합니다." in service
    assert "기술 연구소에서 요구 레벨" in descriptions
    assert "Z 또는 X 슬롯" in descriptions
    assert 'return "Z: " + first + " | X: " + second;' in role_system
    assert "H 상태 · J 성장 · K 직업 성장 · U 호출기 · B 빠른 통신 · Z/X 기술" in starter

    assert "VillageDefenseResearchSystem.initializeServer" in read("VillageGuardians.java")
    assert "VillageMercenarySystem.initializeServer" in read("VillageGuardians.java")
    assert "VillageRelicSystem.initializeServer" in read("VillageGuardians.java")
    assert "VillageLocalActionSystem.handle" in read("VillageNetwork.java")
    assert "combineFirstPair" in read("VillageEquipmentRaritySystem.java")
    assert "재화는 소모되지 않았습니다" in read("VillageEquipmentRaritySystem.java")
    assert "RESPAWN_DELAY_TICKS = 20 * 20" in read("VillageRespawnSystem.java")
    assert "FORCED_NEXT_WAVE_TICKS = 20 * 60" in read("VillageRaidSystem.java")
    assert "MAX_ACTIVE_ENEMIES = 100" in read("VillageRaidSystem.java")

    print("[PASS] Permanent tree footers were replaced with compact anchored popovers")
    print("[PASS] Skill cards are compact squares with inline Z/X equipment choices")
    print("[PASS] Inventory Growth routes directly to the tree and old vanilla-conflicting keys are gone")
    print("[PASS] Equipment strengthening is smithy-only; learning is laboratory-only; equipping is unrestricted")
    print("[PASS] Bright parchment panels, cards, buttons and vertical chrome are reduced")


if __name__ == "__main__":
    main()

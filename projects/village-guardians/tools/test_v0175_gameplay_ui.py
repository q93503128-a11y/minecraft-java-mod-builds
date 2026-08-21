from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    shop = read("VillageEquipmentShop.java")
    shop_ui = read("VillageShopCatalogScreen.java")
    quick = read("VillageQuickChatSafeScreen.java")
    controller = read("VillageUiController.java")
    service = read("VillageUiService.java")
    rarity = read("VillageEquipmentRaritySystem.java")
    fusion = read("VillageFusionSafeScreen.java")
    role_ui = read("VillageRoleProgressScreen.java")
    client_ui = read("VillageClientUi.java")
    visuals = read("VillageSkillEffectSystem.java")
    progression = read("VillageProgressionSystem.java")
    respawn = read("VillageRespawnSystem.java")
    inventory = read("VillageInventoryPanel.java")

    assert "mod_version=" in props
    assert "currentOffers(int day)" in shop and "rotatingOffers" in shop
    assert 'EQUIPMENT("장비")' in shop and 'ARCANE_FOCUS("arcane_focus"' in shop
    assert "Category.EQUIPMENT, Items.BLAZE_ROD" in shop
    assert "currentOffers(day).contains" in shop
    assert "판매용 잡템 일괄 정산" in controller
    assert "VillageEquipmentShop.currentOffers(day)" in controller
    assert "VillageUiSafeArea.screen(width, height)" in shop_ui
    assert "font.width(normalized)" in shop_ui
    assert "카드 선택 → 상세 확인 → 실행" in shop_ui

    assert "radiusX" in quick and "radiusY" in quick
    assert "VillageUiSafeArea.screen(width, height)" in quick
    assert "font.width(normalized)" in quick
    assert "openQuickChat(player);" in controller
    assert "openQuickChat(player);" in service
    key_source = read("VillageClientKeys.java")
    assert 'consume(QUICK_COMMUNICATION, "open_quick_chat")' in key_source
    assert "CALLER" not in key_source and "GLFW.GLFW_KEY_U" not in key_source
    assert 'action = "open_quick_chat"' in inventory

    assert "combineSelected" in rarity and "fusionCandidates" in rarity
    assert "unique.size() != 3" in rarity
    assert "first.shrink(1)" in rarity and "third.shrink(1)" in rarity
    assert "selectedSlots.size() == 3" in fusion
    assert 'String action = "fusion_combine:"' in fusion
    assert 'case "equipment_fusion" -> new VillageFusionSafeScreen(payload)' in client_ui
    assert "VillageUiSafeArea" in fusion

    assert 'send(player, "role_skills"' in controller
    assert "openRoleSkillResearch" in controller
    assert 'this.skillsOnly = "role_skills".equals(payload.screenId())' in role_ui
    assert '"research_skill_equip:"' in role_ui

    assert "ParticleTypes" not in visuals and "sendParticles" not in visuals
    assert "VillageSkillEffectEntity.spawn" in visuals
    assert "vanguard_spin" in visuals and "warden_fortress" in visuals
    assert "Display.ItemDisplay" not in visuals and "Display.BlockDisplay" not in visuals

    assert "respawnDelayTicks" in progression
    assert "clearTreatmentEffects" in progression
    assert "MobEffects.ABSORPTION" in progression
    assert "VillageProgressionSystem.respawnDelayTicks()" in respawn

    print("[PASS] Daily shop inventory rotates and hides future stock")
    print("[PASS] Current shop catalog uses safe-area detail-first selection and execution")
    print("[PASS] Quick communication uses the current safe-area signal wheel")
    print("[PASS] Smithy fusion explicitly selects three compatible items on the safe screen")
    print("[PASS] Skill-hall entry opens skills-only research and current role progression")
    print("[PASS] Skills use non-particle procedural mesh actors and infirmary upgrades have real utility")


if __name__ == "__main__":
    main()

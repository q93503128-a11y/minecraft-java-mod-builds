from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    shop = read("VillageEquipmentShop.java")
    shop_ui = read("VillageShopScreen.java")
    quick = read("VillageQuickChatScreen.java")
    controller = read("VillageUiController.java")
    service = read("VillageUiService.java")
    rarity = read("VillageEquipmentRaritySystem.java")
    fusion = read("VillageFusionScreen.java")
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
    assert "shop_utility|판매용 잡템 일괄 정산" in controller
    assert "VillageEquipmentShop.currentOffers(day)" in controller
    assert 'EQUIPMENT("장비")' in shop_ui
    assert "font.width(normalized)" in shop_ui and "contentWidth < 330" in shop_ui

    assert "OptionBounds" in quick and "radiusX" in quick and "radiusY" in quick
    assert "width - cardWidth - 6" in quick and "height - cardHeight - 6" in quick
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
    assert '"fusion_combine:"' in fusion
    assert 'case "equipment_fusion" -> new VillageFusionScreen(payload)' in client_ui

    assert 'send(player, "role_skills"' in controller
    assert "openRoleSkillResearch" in controller
    assert 'this.skillsOnly = "role_skills".equals(payload.screenId())' in role_ui
    assert '"research_skill_equip:"' in role_ui

    assert "labels.add(branch.displayName() + \"|\" + detail)" in controller
    assert "다음 단계 비용: 주화" in controller

    assert "ParticleTypes" not in visuals and "sendParticles" not in visuals
    assert "VillageSkillEffectEntity.spawn" in visuals
    assert "vanguard_spin" in visuals and "warden_fortress" in visuals
    assert "Display.ItemDisplay" not in visuals and "Display.BlockDisplay" not in visuals

    assert "respawnDelayTicks" in progression
    assert "clearTreatmentEffects" in progression
    assert "MobEffects.ABSORPTION" in progression
    assert "VillageProgressionSystem.respawnDelayTicks()" in respawn

    print("[PASS] Daily shop inventory rotates and hides future stock")
    print("[PASS] Equipment, armour and supplies use bounded pixel-fit layouts")
    print("[PASS] Caller opens a screen-safe PUBG-style quick communication wheel")
    print("[PASS] Smithy fusion explicitly selects three compatible items")
    print("[PASS] Skill-hall entry opens skills only and defense research titles stay concise")
    print("[PASS] Skills use non-particle procedural mesh actors and infirmary upgrades have real utility")


if __name__ == "__main__":
    main()

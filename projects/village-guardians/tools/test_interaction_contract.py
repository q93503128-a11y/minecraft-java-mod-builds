#!/usr/bin/env python3
"""Interaction contracts for facility routing, shop safety and next-wave dossiers."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    client = read("VillageClientUi.java")
    local = read("VillageLocalActionSystem.java")
    trading = read("VillageTradingSystem.java")
    town = read("VillageTownHallGridScreen.java")
    shop = read("VillageShopCatalogScreen.java")
    action = read("VillageActionDetailScreen.java")
    wave = read("VillageWaveIntelDossierScreen.java")
    bestiary = read("VillageEnemyBestiary.java")
    relic = read("VillageRelicChoiceConfirmScreen.java")

    assert 'action.startsWith("facility:")' in local
    assert "VillageUiController.openBuilding(player, building)" in local
    assert 'case "buy_arrows"' in local and 'case "buy_food"' in local
    assert "VillageUiController.openEquipmentShop(player)" in local

    assert 'SALE_ONLY_PREFIX = "[판매용]"' in trading
    assert "name.startsWith(SALE_ONLY_PREFIX)" in trading
    sale_only = trading.split("private static boolean isSaleOnlyLoot", 1)[1].split("private static int unitValue", 1)[0]
    assert 'NAMED_PRICES.containsKey(name)' not in sale_only
    for useful in ("수호 화살", "전투 건량", "마을 배급빵"):
        assert useful in trading

    assert 'case "town_hall" -> new VillageTownHallGridScreen(payload)' in client
    assert 'case "equipment_shop" -> new VillageShopCatalogScreen(payload)' in client
    assert "new VillageActionDetailScreen(payload)" in client
    assert 'case "wave_intel" -> new VillageWaveIntelDossierScreen(payload)' in client
    assert 'case "relic_choice" -> new VillageRelicChoiceConfirmScreen(payload)' in client

    # Town hall list clicks only select. The three explicit bottom actions own facility use/repair/upgrade.
    assert "VillageConfirmScreen" in town
    assert "FacilityCard" in town and "functionAction(f)" in town
    assert '"repair:" + f.id()' in town and '"upgrade:" + f.id()' in town
    assert 'FACILITIES("시설 관리")' in town and 'ROLES("직업 배치")' in town

    for tab in ("ALL(\"전체\"", "EQUIPMENT(\"장비\"", "ARMOR(\"방어구\"",
                "CONSUMABLE(\"소모품\"", "SALE(\"판매\""):
        assert tab in shop
    assert 'action.equals("open_item_sell")' in shop
    assert 'action.equals("buy_arrows")' in shop and 'action.equals("buy_food")' in shop
    assert 'action.equals("sell_loot")' in shop
    assert "VillageConfirmScreen" in shop

    assert '"facility_info".equals(rawActions[i])' in action
    for prefix in ("sell_item:", "forge_enhance:", "hire_mercenary:", "defense_research:", "research_skill_unlock:"):
        assert prefix in action

    assert '"facility_info".equals(actions[i])' in wave
    assert "병력:" in wave and "VillageEnemyBestiary.find" in wave
    assert "개요" in wave and "능력" in wave and "위협" in wave and "대응" in wave
    for archetype in ("GRUNT", "RUSHER", "BULWARK", "SAPPER", "MARKSMAN", "SHIELDBREAKER",
                      "HEXER", "WAR_CHANTER", "NECROMANCER", "TOWER_HUNTER", "SIEGE_BEAST",
                      "IRON_WARLORD", "PLAGUE_ARCHON", "DREAD_KNIGHT"):
        assert f"case {archetype}" in bestiary

    assert '"facility_info".equals(actions[i])' in relic
    assert "VillageConfirmScreen" in relic and "영구 적용" in relic

    print("[PASS] Facility cards route to explicit function/repair/upgrade controls")
    print("[PASS] Bulk junk sale cannot sweep named combat supplies")
    print("[PASS] Shop categories and detail-first action policy are wired")
    print("[PASS] Next-wave briefing exposes bestiary dossiers and relic rewards require confirmation")


if __name__ == "__main__":
    main()

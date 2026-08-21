from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    keys = read("VillageClientKeys.java")
    controller = read("VillageUiController.java")
    service = read("VillageUiService.java")
    local = read("VillageLocalActionSystem.java")
    detail = read("VillageActionDetailScreen.java")
    common_tree = read("VillageSkillTreeScreen.java")
    role_tree = read("VillageRoleProgressScreen.java")

    assert "mod_version=" in props
    assert "minecraft.gui.screen() != null" in keys
    assert "private static void drain(KeyMapping mapping)" in keys
    assert "minecraft.getConnection() == null" in keys

    # Current production access rules: opening growth is allowed remotely, but facility mutations and
    # research/hiring/equipment operations are re-authorized at their authoritative locations.
    assert "requireTownHall(player" in controller
    assert "VillageLocationRules.isNearSkillHall(player)" in controller
    assert "장비 강화는 대장간 단말기 근처" in controller
    assert "보유품 판매는 상점 단말기 근처" in controller
    assert "VillageLocationRules.isNearTownHall(player)" in local
    assert "용병 고용은 병영 단말기 근처" in local
    assert "화살 구매는 창고 단말기 근처" in local

    # Legacy service remains only as a compatibility route; current controller/action screen own new UI.
    assert "public static void openDashboard" in service
    assert "VillageSiegeCommandUi.open(player)" in local
    assert "자동 버프 건물" in local and "전투 훈련은 패시브" in local

    # Retired monolithic town/facility screens are replaced by one safe-area detail-first surface.
    assert "VillageUiSafeArea.screen(width, height)" in detail
    assert "font.width(normalized)" in detail
    assert "panelWidth < 390" in detail
    assert "confirmationRequired" in detail
    assert "ClientPacketDistributor.sendToServer" in detail

    # Growth popovers remain clamped inside their live viewports at narrow sizes.
    assert "fitPopoverWidth(viewport.width(), 164, 246)" in common_tree
    assert "nodeX + nodeHalf < viewport.left()" in common_tree
    assert "fitPopoverWidth(view.width(), 176, 264)" in role_tree
    assert "horizontalPlacement" in role_tree
    assert "card.y() + card.size() < view.top()" in role_tree
    assert "below + bubbleHeight <= view.bottom() - 5" in role_tree

    print("Village Guardians current UI, location and shortcut safety contracts passed.")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def block(text: str, start: str, end: str) -> str:
    a = text.index(start)
    b = text.index(end, a + len(start))
    return text[a:b]


def clamp(value: int, minimum: int, maximum: int) -> int:
    return max(minimum, min(maximum, value))


def safe_area(width: int, height: int):
    side = clamp(width // 52, 7, 16)
    top_padding = clamp(height // 80, 6, 12)
    bottom_padding = clamp(height // 11, 38, 56)
    bottom = max(top_padding + 1, height - bottom_padding)
    return side, top_padding, max(side + 1, width - side), max(top_padding + 1, bottom)


def wave_layout(width: int, height: int):
    left, safe_top, safe_right, safe_bottom = safe_area(width, height)
    top = min(safe_bottom - 2, safe_top + 56)
    bottom = max(top + 1, safe_bottom - 18)
    content_height = max(1, bottom - top)
    compact = content_height < 190
    gap = 7 if compact else 9
    safe_width = safe_right - left
    wave_width = clamp(safe_width * 26 // 100, 130, 255)
    wave_width = min(wave_width, max(95, safe_width - 230))
    wave = (left + 7, top, left + 7 + wave_width, bottom)
    right_left = min(safe_right - 8, wave[2] + gap)
    right_right = max(right_left + 1, safe_right - 7)
    right_width = max(1, right_right - right_left)
    overview_height = (clamp(content_height * 30 // 100, 32, 46) if compact
                       else clamp(content_height * 39 // 100, 92, 190))
    overview = (right_left, top, right_right, min(bottom, top + overview_height))
    lower_top = min(bottom, overview[3] + gap)
    if compact or right_width >= 430:
        monster_min = min(72, right_width) if compact else min(150, right_width)
        dossier_min = (min(92, max(1, right_width - monster_min - gap)) if compact
                       else min(150, max(1, right_width - monster_min - gap)))
        preferred = right_width * 38 // 100
        maximum_monster = max(monster_min, right_width - gap - dossier_min)
        monster_width = clamp(preferred, monster_min, maximum_monster)
        monster_width = min(monster_width, max(1, right_width - gap - 1))
        monsters = (right_left, lower_top, min(right_right, right_left + monster_width), bottom)
        dossier_left = min(right_right - 1, monsters[2] + gap)
        dossier = (dossier_left, lower_top, right_right, bottom)
    else:
        lower_height = max(1, bottom - lower_top)
        minimum_dossier = min(66, max(1, lower_height // 2))
        maximum_roster = max(1, lower_height - gap - minimum_dossier)
        roster_height = clamp(lower_height * 42 // 100, min(58, maximum_roster), maximum_roster)
        monsters = (right_left, lower_top, right_right, min(bottom, lower_top + roster_height))
        dossier_top = min(bottom - 1, monsters[3] + gap)
        dossier = (right_left, dossier_top, right_right, bottom)
    return (left, safe_top, safe_right, safe_bottom), wave, overview, monsters, dossier, compact


def positive(rect):
    return rect[2] > rect[0] and rect[3] > rect[1]


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    assert "mod_version=0.18.33-alpha.1" in props

    wave = read("VillageWaveIntelDossierScreen.java")
    assert "boolean compactHeight = contentHeight < 190" in wave
    assert wave.count("graphics.enableScissor(pane.left(), pane.top(), pane.right(), pane.bottom())") >= 4
    assert "if (pane.height() < 58)" in wave and "if (pane.height() < 100)" in wave
    assert "if (y + 44 > layout.waveList().bottom() - 4) break;" in wave
    assert "if (y + 40 > layout.monsters().bottom() - 4) break;" in wave
    for dims in ((320, 240), (426, 240), (480, 270), (640, 360), (854, 480), (1280, 720)):
        safe, wave_pane, overview, monsters, dossier, compact = wave_layout(*dims)
        for pane in (wave_pane, overview, monsters, dossier):
            assert positive(pane), (dims, pane)
            assert pane[0] >= safe[0] and pane[2] <= safe[2], (dims, safe, pane)
            assert pane[1] >= safe[1] and pane[3] <= safe[3], (dims, safe, pane)
        assert overview[3] <= monsters[1]
        if compact or (dossier[1] == monsters[1]):
            assert monsters[2] <= dossier[0]
        else:
            assert monsters[3] <= dossier[1]
        if dims[1] <= 270:
            assert compact and dossier[3] - dossier[1] >= 70, (dims, dossier)

    ui_service = read("VillageUiService.java")
    restart = block(ui_service, 'case "restart_previous"', 'default ->')
    assert restart.count("VillageProgressionSystem.isGameOver()") == 2
    progression = read("VillageProgressionSystem.java")
    reset = block(progression, "public static synchronized void resetForRestart", "public static int upgradeCost")
    assert "if (!gameOver) return;" in reset
    assert reset.index("if (!gameOver) return;") < reset.index("gameOver = false;")

    local = read("VillageLocalActionSystem.java")
    deploy = block(local, 'if (action.startsWith("merc_deploy:"))', "switch (action)")
    assert "VillageMercenaryDeploymentSystem.canOpenAt(player)" in deploy

    controller = read("VillageUiController.java")
    sell = block(controller, 'if (action.startsWith("sell_item:"))', 'if (action.startsWith("research_skill_unlock:"))')
    assert "Building.STOREHOUSE" in sell and "VillageLocationRules.isNear" in sell

    role_node = block(controller, 'if (action.startsWith("role_node:"))', 'if (action.startsWith("gear:"))')
    assert "isNearSkillHall" not in role_node
    legacy_role_node = block(ui_service, 'if (action.startsWith("role_node:"))', 'if (action.startsWith("role_skill_unlock:"))')
    assert "requireSkillHall" not in legacy_role_node
    research = block(controller, 'if (action.startsWith("research_skill_unlock:"))', 'if (action.startsWith("research_skill_equip:"))')
    assert "isNearSkillHall" in research

    rarity = read("VillageEquipmentRaritySystem.java")
    enhance = block(rarity, "public static String enhanceSelected", "public static String enhancementEffectSummary")
    assert "stack.getCount() != 1" in enhance
    assert enhance.index("stack.getCount() != 1") < enhance.index("VillageProgressionSystem.spendCoins")
    assert "Items.BLAZE_ROD" in rarity

    role_screen = read("VillageRoleProgressScreen.java")
    assert '"Z 슬롯' not in role_screen and '"X 슬롯' not in role_screen
    assert role_screen.count("VillageClientKeys.skillOneKeyName()") >= 2
    assert role_screen.count("VillageClientKeys.skillTwoKeyName()") >= 2

    # The retired paid-food route is compatibility-only: it must remain a no-op if a stale action reaches it.
    buy_food = block(progression, "public static synchronized String buyFood", "public static synchronized String claimDailyBread")
    assert "유료 일반 식량은 일일 배급 식량으로 통합" in buy_food
    assert "spendCoins" not in buy_food and "giveOrDrop" not in buy_food

    print("[PASS] low-height wave briefing keeps every pane positive, clipped and non-overlapping")
    print("[PASS] restart, mercenary deployment and item sale mutations are server-authorized")
    print("[PASS] role growth matches its anywhere-access contract while skill research remains hall-gated")
    print("[PASS] stackable custom equipment cannot batch-enhance for one payment")
    print("[PASS] role skill slot labels follow remapped keys")
    print("[PASS] retired paid-food compatibility action remains mutation-free")
    print("[PASS] v0.18.33 manual source audit contract complete")


if __name__ == "__main__":
    main()

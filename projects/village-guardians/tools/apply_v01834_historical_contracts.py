#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match in {path}, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def main() -> None:
    interaction = ROOT / "tools/test_interaction_contract.py"
    replace_once(interaction,
'''    local = read("VillageLocalActionSystem.java")
    trading = read("VillageTradingSystem.java")
''',
'''    local = read("VillageLocalActionSystem.java")
    building_router = read("VillageBuildingInteractionRouter.java")
    trading = read("VillageTradingSystem.java")
''', "interaction router source")
    replace_once(interaction,
'''    assert 'action.startsWith("facility:")' in local
    assert "VillageUiController.openBuilding(player, building)" in local
''',
'''    assert 'action.startsWith("facility:")' in local
    assert "구식 시설 바로가기는 폐기되었습니다" in local
    assert "VillageUiController.openBuilding(player, building)" not in local
    assert "VillageUiController.openBuilding(player, building)" in building_router
''', "interaction facility ownership")

    runtime = ROOT / "tools/test_runtime_safety.py"
    replace_once(runtime,
'''    assert "수리·강화·포탑 건설은 회관" in controller
''',
'''    assert "성벽 지휘 레버" in controller
    assert '"siege_command"' in controller
''', "runtime wall ownership")

    ui_safety = ROOT / "tools/test_v0174_ui_safety.py"
    replace_once(ui_safety,
'''    assert "VillageLocationRules.isNearTownHall(player)" in local
''',
'''    assert "isSiegeCommandAction(action)" in local
    assert "VillageProgressionSystem.Building.WALLS" in local
''', "legacy UI safety siege location")

    defense = ROOT / "tools/test_v01824_defense_action_integrity.py"
    replace_once(defense,
'''    assert "requiresSiegeCommandAccess(action)" in local
    assert "VillageLocationRules.isNearTownHall(player)" in local
''',
'''    assert "isSiegeCommandAction(action)" in local
    assert "VillageProgressionSystem.Building.WALLS" in local
''', "defense action ownership")
    replace_once(defense,
'''    # Persistent defense maintenance is day-only and blocked after game over.
    assert 'public static String blockReason(String action)' in rules
    assert 'VillageProgressionSystem.isGameOver()' in rules
    assert 'VillageCouncilState.currentPhase() != VillageTimePhase.DAY' in rules
    for signature in (
        'public static String selectPlacement',
        'public static synchronized String repair(ServerPlayer player, int id)',
        'public static synchronized String upgrade(ServerPlayer player, int id)',
        'public static synchronized String dismantle(ServerPlayer player, int id)',
        'public static synchronized String repairAll(ServerPlayer player)',
    ):
        chunk = body(placed, signature, '\n    }')
        assert 'VillageMaintenanceRules.blockReason(' in chunk
''',
'''    # Persistent defense maintenance is day-only, game-over safe and owned by the physical wall command.
    assert 'public static String blockReason(String action)' in rules
    assert 'VillageProgressionSystem.isGameOver()' in rules
    assert 'VillageCouncilState.currentPhase() != VillageTimePhase.DAY' in rules
    method_ranges = (
        ('public static String selectPlacement', 'public static boolean handlePlacementClick'),
        ('public static synchronized String repair(ServerPlayer player, int id)', 'public static synchronized String upgrade'),
        ('public static synchronized String upgrade(ServerPlayer player, int id)', 'public static synchronized String dismantle'),
        ('public static synchronized String dismantle(ServerPlayer player, int id)', 'public static synchronized String repairAll'),
        ('public static synchronized String repairAll(ServerPlayer player)', 'public static void tick'),
    )
    for signature, end in method_ranges:
        chunk = body(placed, signature, end)
        assert 'VillageMaintenanceRules.blockReason(' in chunk
        assert 'VillageProgressionSystem.Building.WALLS' in chunk
    confirm = body(placed, 'public static boolean handlePlacementClick', 'public static String cancelPlacement')
    assert 'VillageMaintenanceRules.blockReason("포탑 배치")' in confirm
''', "defense leaf contract")
    replace_once(defense,
'''    upgrade = body(placed, 'public static synchronized String upgrade(ServerPlayer player, int id)', '\n    }')
''',
'''    upgrade = body(placed, 'public static synchronized String upgrade(ServerPlayer player, int id)',
                   'public static synchronized String dismantle')
''', "defense upgrade slice")
    replace_once(defense,
'''    print('[PASS] siege mutation packets revalidate town-hall locality server-side')
''',
'''    print('[PASS] siege mutation packets revalidate physical wall-command locality server-side')
''', "defense message")

    manual = ROOT / "tools/test_v01833_manual_audit.py"
    replace_once(manual,
'''    assert "mod_version=0.18.33-alpha.1" in props
''',
'''    assert "mod_version=" in props
''', "manual audit historical version")

    siege = ROOT / "tools/test_v0189_siege_phase2.py"
    replace_once(siege,
'''    assert "facility:walls" not in local
    assert "building == VillageProgressionSystem.Building.WALLS" in local
''',
'''    assert "facility:walls" not in local
    assert "isSiegeCommandAction(action)" in local
    assert "VillageProgressionSystem.Building.WALLS" in local
''', "historical siege entry ownership")

    print("[PATCH] v0.18.34 historical contracts migrated to current facility ownership")


if __name__ == "__main__":
    main()

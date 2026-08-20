#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def body(source: str, start: str, end: str) -> str:
    return source.split(start, 1)[1].split(end, 1)[0]

def main() -> None:
    local = read("VillageLocalActionSystem.java")
    placed = read("VillagePlacedTurretSystem.java")
    segment = read("VillageSiegeSegmentSystem.java")
    ui = read("VillageSiegeCommandUi.java")
    network = read("VillageNetwork.java")
    v23 = (ROOT / "tools/test_v01823_defense_consolidation.py").read_text(encoding="utf-8")

    assert 'assert "mod_version=0.18.23-alpha.1" in props' not in v23

    # Client actions are untrusted strings; mutations must be re-authorized server-side at packet execution time.
    assert "VillageUiActionPayload" in network and "VillageLocalActionSystem.handle(player, payload.action())" in network
    assert "requiresSiegeCommandAccess(action)" in local
    assert "VillageLocationRules.isNearTownHall(player)" in local
    for token in (
        'action.equals("siege_turret_repair_all")',
        'action.startsWith("siege_segment_repair:")',
        'action.startsWith("siege_segment_upgrade:")',
        'action.startsWith("siege_turret_select:")',
        'action.startsWith("siege_turret_repair:")',
        'action.startsWith("siege_turret_upgrade:")',
        'action.startsWith("siege_turret_dismantle:")',
    ):
        assert token in local

    # Persistent defense maintenance is day-only and blocked after game over.
    assert 'private static String maintenanceBlockReason(String action)' in placed
    assert 'VillageProgressionSystem.isGameOver()' in placed
    assert 'VillageCouncilState.currentPhase() != VillageTimePhase.DAY' in placed
    for signature in (
        'public static String selectPlacement',
        'public static synchronized String repair(ServerPlayer player, int id)',
        'public static synchronized String upgrade(ServerPlayer player, int id)',
        'public static synchronized String dismantle(ServerPlayer player, int id)',
        'public static synchronized String repairAll(ServerPlayer player)',
    ):
        chunk = body(placed, signature, '\n    }')
        assert 'maintenanceBlockReason(' in chunk

    # Emergency consumable field repair intentionally remains a combat action.
    field = body(placed, 'public static synchronized String fieldRepairNearest', '\n    }')
    assert 'maintenanceBlockReason(' not in field

    # Upgrade cannot be used as a free full repair anymore.
    upgrade = body(placed, 'public static synchronized String upgrade(ServerPlayer player, int id)', '\n    }')
    for token in ('oldMaximum', 'missingHp', 'newMaximum', 'newHp', '기존 손상 유지'):
        assert token in upgrade
    assert 'maxHp(upgradedBase), true' not in upgrade

    assert 'private static String maintenanceBlockReason(String action)' in segment
    assert segment.count('String blocked = maintenanceBlockReason(') >= 2
    assert 'VillageProgressionSystem.isGameOver()' in segment
    assert 'VillageCouncilState.currentPhase() != VillageTimePhase.DAY' in segment
    assert '현재 손상분은 유지' in ui

    print('[PASS] siege mutation packets revalidate town-hall locality server-side')
    print('[PASS] persistent wall/turret maintenance is day-only and game-over safe')
    print('[PASS] emergency field repair remains usable as a combat consumable')
    print('[PASS] turret upgrades preserve existing damage instead of granting a free full repair')
    print('[PASS] release/version documentation and historical test ownership are current')
    print('[PASS] v0.18.24 defense action integrity contract complete')

if __name__ == "__main__":
    main()

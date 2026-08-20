#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    guardians = read("VillageGuardians.java")
    defense = read("VillageDefenseSystem.java")
    tower_builder = read("VillageDefenseTowerBuilder.java")
    placed = read("VillagePlacedTurretSystem.java")
    local = read("VillageLocalActionSystem.java")
    service = read("VillageUiService.java")
    commands = read("VillageCommands.java")

    assert "mod_version=0.18.23-alpha.1" in props

    # Retired fixed-corner progression must be physically absent from production source.
    assert not (JAVA / "VillageTowerSpecializationSystem.java").exists()
    assert not (JAVA / "VillageTowerProgressData.java").exists()
    for source in (guardians, defense, tower_builder, service):
        assert "VillageTowerSpecializationSystem" not in source
        assert "VillageTowerProgressData" not in source

    # Corner towers remain architecture only; their shape must not depend on obsolete saved branches.
    for method in ("buildBallista", "buildFlame", "buildFrost", "buildArcane"):
        assert method in tower_builder
    assert "installedStage >= 1" in tower_builder and "installedStage >= 4" in tower_builder
    assert "Branch." not in tower_builder and "rank(" not in tower_builder

    # Real production defense ownership is one coherent player-placed system.
    for turret in (
        "BALLISTA", "REPEATER", "PIERCER", "FLAME", "FROST",
        "CHAIN", "BOMBARD", "NULLIFIER", "ANTI_AIR", "BEACON"
    ):
        assert turret + "(" in placed
    assert "VillageDefenseResearchSystem.towerDamageMultiplier()" in placed
    assert "VillagePlacedTurretSystem.count()" in defense
    assert "VillagePlacedTurretSystem.activeCount()" in defense
    assert "VillagePlacedTurretSystem.capacity()" in defense

    # Old clients/actions may arrive, but they can only redirect to current siege command UI.
    # The old screen-id token may remain in generic facility-layout compatibility classification;
    # what is forbidden is any callable specialization/detail or coin-mutating branch path.
    assert "action.equals(\"open_tower_control\")" in local
    assert "action.startsWith(\"tower_branch:\")" in local
    assert "VillageSiegeCommandUi.open(player);" in local
    assert "public static void openTowerControl(ServerPlayer player)" in service
    assert "VillageSiegeCommandUi.open(player);" in service
    assert "public static void openTowerDetail" not in service
    assert 'send(player, "tower_detail"' not in service
    assert "purchaseBranch" not in service and "upgradeBranch" not in service

    # Command entry points must no longer create legacy menu/status surfaces.
    assert "VillageUiController.openDashboard" in commands
    assert "VillageUiController.openStatus" in commands
    assert "VillageUiService.openDashboard(source.getPlayerOrException())" not in commands
    assert "VillageUiService.openPlayerStatus(source.getPlayerOrException())" not in commands

    print("[PASS] obsolete fixed-corner tower progression and SavedData readers are retired")
    print("[PASS] fixed corner towers remain architecture without fake combat specialization")
    print("[PASS] ten player-placed turret types remain the single production defense owner")
    print("[PASS] stale tower actions redirect safely to the current siege command UI")
    print("[PASS] slash-command menu/status entry points use the current UI controller")
    print("[PASS] v0.18.23 defense consolidation contract complete")


if __name__ == "__main__":
    main()

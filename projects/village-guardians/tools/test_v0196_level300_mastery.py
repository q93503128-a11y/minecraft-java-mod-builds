#!/usr/bin/env python3
from pathlib import Path
import math

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def target(day: int) -> int:
    safe = max(1, min(100, day))
    if safe <= 20:
        return 1 + math.floor((safe - 1) * 29.0 / 19.0 + 0.5)
    return min(300, 30 + math.floor((safe - 20) * 270.0 / 80.0 + 0.5))


def combat_level(level: int) -> int:
    safe = max(1, min(300, level))
    if safe <= 100:
        return safe
    return 100 + (safe - 100) // 4


def main() -> None:
    progress = read("RpgProgress.java")
    campaign = read("VillageCampaignProgression.java")
    rpg = read("VillageRpgSystem.java")
    ability = read("VillageRoleAbilitySystem.java")
    tree = read("VillageSkillTreeSystem.java")
    copy = read("VillageActionDescriptions.java")

    assert "MAX_LEVEL = 300" in progress
    assert "return 100 + (safe - 100) / 4;" in progress
    assert "270.0f / 80.0f" in campaign
    assert target(20) == 30
    assert target(30) == 64
    assert target(40) == 98
    assert target(60) == 165
    assert target(80) == 233
    assert target(100) == 300
    assert all(3 <= target(day) - target(day - 1) <= 4 for day in range(21, 101))

    # Lv.1-100 combat balance is exactly the established curve; visible mastery then slows 4:1.
    assert combat_level(30) == 30
    assert combat_level(60) == 60
    assert combat_level(100) == 100
    assert combat_level(200) == 125
    assert combat_level(300) == 150
    assert rpg.count("RpgProgress.combatScalingLevel(level)") == 3
    assert ability.count(
        "RpgProgress.combatScalingLevel(VillageCouncilState.levelOf(player.getUUID()))"
    ) == 2

    # 50-node tactical tree costs 110P total. Natural Lv.300 earns 96P, preserving build choice.
    foundation = 29
    mastery = (300 - 30) // 4
    assert foundation + mastery == 96
    assert "Math.max(0, level - 30) / 4" in tree
    assert "return Math.max(pacedTotal, spentPoints(player));" in tree
    assert "4레벨마다 1P" in tree
    assert "4레벨마다 얻는 전술 포인트" in copy

    print("[PASS] visible player progression now spans Lv.1-300 with 3-4 levels per late campaign day")
    print("[PASS] raw combat scaling preserves Lv.1-100 and slows to 4:1 mastery scaling afterward")
    print("[PASS] Lv.300 tactical points preserve build choice and grandfather historical allocations")


if __name__ == "__main__":
    main()

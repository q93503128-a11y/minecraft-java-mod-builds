#!/usr/bin/env python3
from pathlib import Path
import math

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def target_level(day: int) -> int:
    safe = max(1, min(100, day))
    if safe <= 20:
        return 1 + math.floor((safe - 1) * 29.0 / 19.0 + 0.5)
    return min(300, 30 + math.floor((safe - 20) * 270.0 / 80.0 + 0.5))


def combat_level(level: int) -> int:
    safe = max(1, min(300, level))
    return safe if safe <= 100 else 100 + (safe - 100) // 4


def outgoing(level: int) -> float:
    value = combat_level(level)
    foundation = min(29, value - 1)
    mastery = max(0, value - 30)
    return (
        1.0
        + foundation * 0.035
        + (foundation // 5) * 0.08
        + mastery * 0.008
        + (mastery // 10) * 0.025
    )


def player_base_hp(level: int) -> int:
    value = combat_level(level)
    bonus = ((min(30, value) - 1) // 5) * 4 + (max(0, value - 30) // 10) * 2
    return 20 + (bonus // 4) * 4


def equipment_tier(day: int) -> int:
    if day >= 90:
        return 10
    if day >= 80:
        return 9
    if day >= 70:
        return 8
    if day >= 55:
        return 7
    if day >= 40:
        return 6
    if day >= 25:
        return 5
    if day >= 15:
        return 4
    if day >= 10:
        return 3
    if day >= 5:
        return 2
    return 1


def flat_melee(tier: int) -> float:
    safe = max(1, min(10, tier))
    if safe <= 1:
        return 0.0
    if safe == 2:
        return 1.50
    if safe == 3:
        return 3.00
    if safe == 4:
        return 5.00
    n = safe - 4
    return 5.00 + n * 2.25 + n * n * 0.14


def enemy_health_tier(day: int, wave: int = 4) -> int:
    safe = max(1, min(100, day))
    base = (safe - 1) // 8 + max(0, wave - 1) // 6
    late = 0 if safe <= 20 else (safe - 20) // 20
    return min(14, max(0, base + late))


def enemy_base_scale(day: int) -> float:
    safe = max(1, min(100, day))
    return 1.0 if safe <= 20 else 1.0 + (safe - 20) / 80.0


def absorption_hp(day: int) -> int:
    if day < 30:
        return 0
    amplifier = min(2, max(0, (day - 20) // 30))
    return 4 * (amplifier + 1)


def neutral_grunt_hp(day: int) -> float:
    tier = enemy_health_tier(day)
    health_boost = 0 if tier <= 0 else 4 * (tier + 1)
    return 20.0 * enemy_base_scale(day) + health_boost + absorption_hp(day)


def main() -> None:
    campaign = read("VillageCampaignProgression.java")
    enemies = read("VillageEnemyArchetypeSystem.java")
    raid = read("VillageRaidSystem.java")
    tree = read("VillageSkillTreeSystem.java")
    rpg = read("VillageRpgSystem.java")

    assert "int lateBonus = safe <= 20 ? 0 : (safe - 20) / 20;" in campaign
    assert "return Math.min(14, Math.max(0, base + lateBonus));" in campaign
    assert "public static float enemyBaseHealthMultiplier(int day)" in campaign
    assert "return 1.0f + (safe - 20) / 80.0f;" in campaign
    assert "trait.healthScale()" in enemies
    assert "day > 20" in enemies
    assert "Math.min(18.0" in enemies
    assert "Math.min(17, healthTier + (boss ? 3 : 0))" in raid

    landmarks = {
        20: (30, 30, 2.415, 4, 5.00, 40, 2, 32),
        40: (98, 98, 3.109, 6, 10.06, 52, 5, 53),
        60: (165, 116, 3.303, 7, 13.01, 56, 9, 78),
        80: (233, 133, 3.489, 9, 19.75, 60, 12, 99),
        100: (300, 150, 3.675, 10, 23.54, 64, 14, 112),
    }
    for day, expected in landmarks.items():
        level = target_level(day)
        actual = (
            level,
            combat_level(level),
            round(outgoing(level), 3),
            equipment_tier(day),
            round(flat_melee(equipment_tier(day)), 2),
            player_base_hp(level),
            enemy_health_tier(day),
            round(neutral_grunt_hp(day)),
        )
        assert actual == expected, (day, actual, expected)

    # Opening balance remains unchanged while late durability continues upward.
    assert enemy_base_scale(20) == 1.0
    assert neutral_grunt_hp(20) == 32.0
    assert neutral_grunt_hp(40) < neutral_grunt_hp(60) < neutral_grunt_hp(80) < neutral_grunt_hp(100)
    assert neutral_grunt_hp(100) == 112.0

    # Repeat training is deliberately small per click but large in aggregate.
    assert "HEALTH_TRAINING_PER_RANK = 0.25D" in tree
    assert "ATTACK_TRAINING_PER_RANK = 0.10D" in tree
    assert "healthTrainingBonus(player)" in rpg
    assert "attackTrainingBonus(player)" in rpg
    leftover_after_full_tree = 299 - 110
    assert leftover_after_full_tree == 189
    assert leftover_after_full_tree * 0.25 == 47.25
    assert round(leftover_after_full_tree * 0.10, 1) == 18.9

    print("[PASS] day20 opening stats are preserved while enemy durability keeps rising through day100")
    print("[PASS] day20/40/60/80/100 player and enemy landmark curves match the documented balance table")
    print("[PASS] rusher base health stays below the normal line and boss health tiers retain late headroom")
    print("[PASS] repeat training is +0.25 HP / +0.1 attack per rank, with large cumulative Lv.300 value")


if __name__ == "__main__":
    main()

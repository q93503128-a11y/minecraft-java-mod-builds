#!/usr/bin/env python3
"""Deterministic balance-contract checks for Village Guardians RPG alpha."""

MAX_LEVEL = 30


def xp_to_next(level: int) -> int:
    return 0 if level >= MAX_LEVEL else 60 + level * 40


def outgoing_damage_multiplier(level: int) -> float:
    level = max(1, min(MAX_LEVEL, level))
    milestones = (level - 1) // 5
    return 1.0 + (level - 1) * 0.12 + milestones * 0.35


def incoming_damage_multiplier(level: int) -> float:
    level = max(1, min(MAX_LEVEL, level))
    milestones = (level - 1) // 5
    return max(0.28, 1.0 - (level - 1) * 0.018 - milestones * 0.06)


def bonus_health_points(level: int) -> int:
    level = max(1, min(MAX_LEVEL, level))
    return ((level - 1) // 3) * 4


def skill_cooldown_seconds(level: int) -> int:
    return max(18, 36 - level // 2)


def simulate_leveling(start_level: int, start_xp: int, awarded: int) -> tuple[int, int]:
    level = max(1, min(MAX_LEVEL, start_level))
    xp = max(0, start_xp) + max(0, awarded)
    while level < MAX_LEVEL and xp >= xp_to_next(level):
        xp -= xp_to_next(level)
        level += 1
    return (level, 0 if level >= MAX_LEVEL else xp)


def main() -> None:
    assert xp_to_next(1) == 100
    assert xp_to_next(29) == 1220
    assert xp_to_next(30) == 0

    previous_attack = 0.0
    previous_defense_multiplier = 2.0
    previous_health = -1
    for level in range(1, MAX_LEVEL + 1):
        attack = outgoing_damage_multiplier(level)
        defense = incoming_damage_multiplier(level)
        health = bonus_health_points(level)
        assert attack >= previous_attack
        assert defense <= previous_defense_multiplier
        assert health >= previous_health
        assert 0.28 <= defense <= 1.0
        previous_attack = attack
        previous_defense_multiplier = defense
        previous_health = health

    assert round(outgoing_damage_multiplier(30), 2) == 6.23
    assert round(incoming_damage_multiplier(30), 2) == 0.28
    assert bonus_health_points(30) == 36
    assert skill_cooldown_seconds(1) == 36
    assert skill_cooldown_seconds(30) == 21

    level, xp = simulate_leveling(1, 0, 3000)
    assert level == 10 and xp == 60, (level, xp)

    total_to_cap = sum(xp_to_next(level) for level in range(1, MAX_LEVEL))
    assert total_to_cap == 19140

    print("PASS: RPG balance contract")
    print(f"level30 attack=x{outgoing_damage_multiplier(30):.2f}")
    print(f"level30 incoming={incoming_damage_multiplier(30) * 100:.0f}%")
    print(f"level30 bonus_health={bonus_health_points(30)}")
    print(f"total_xp_to_level30={total_to_cap}")


if __name__ == "__main__":
    main()

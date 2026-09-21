#!/usr/bin/env python3
"""Deterministic balance-contract checks for the 100-level Village Guardians RPG curve."""

MAX_LEVEL = 100

def xp_to_next(level: int) -> int:
    return 0 if level >= MAX_LEVEL else 120 + level * 72 + level * level * 7

def outgoing_damage_multiplier(level: int) -> float:
    value = max(1, min(MAX_LEVEL, level))
    foundation = min(29, value - 1)
    mastery = max(0, value - 30)
    return (1.0 + foundation * 0.035 + (foundation // 5) * 0.08
            + mastery * 0.008 + (mastery // 10) * 0.025)

def incoming_damage_multiplier(level: int) -> float:
    value = max(1, min(MAX_LEVEL, level))
    foundation = min(29, value - 1)
    mastery = max(0, value - 30)
    result = (1.0 - foundation * 0.009 - (foundation // 5) * 0.025
              - mastery * 0.0009 - (mastery // 10) * 0.004)
    return max(0.50, result)

def bonus_health_points(level: int) -> int:
    value = max(1, min(MAX_LEVEL, level))
    foundation = ((min(30, value) - 1) // 5) * 4
    mastery = max(0, value - 30) // 10 * 2
    return foundation + mastery

def simulate_leveling(start_level: int, start_xp: int, awarded: int) -> tuple[int, int]:
    level = max(1, min(MAX_LEVEL, start_level))
    xp = max(0, start_xp) + max(0, awarded)
    while level < MAX_LEVEL and xp >= xp_to_next(level):
        xp -= xp_to_next(level)
        level += 1
    return level, 0 if level >= MAX_LEVEL else xp

def main() -> None:
    assert xp_to_next(1) == 199
    assert xp_to_next(29) == 8095
    assert xp_to_next(30) == 8580
    assert xp_to_next(99) == 75855
    assert xp_to_next(100) == 0

    previous_attack, previous_defense, previous_health = 0.0, 2.0, -1
    for level in range(1, MAX_LEVEL + 1):
        attack = outgoing_damage_multiplier(level)
        defense = incoming_damage_multiplier(level)
        health = bonus_health_points(level)
        assert attack >= previous_attack
        assert defense <= previous_defense
        assert health >= previous_health
        assert 0.50 <= defense <= 1.0
        previous_attack, previous_defense, previous_health = attack, defense, health

    assert round(outgoing_damage_multiplier(30), 3) == 2.415
    assert round(incoming_damage_multiplier(30), 3) == 0.614
    assert bonus_health_points(30) == 20
    assert round(outgoing_damage_multiplier(100), 3) == 3.150
    assert round(incoming_damage_multiplier(100), 3) == 0.523
    assert bonus_health_points(100) == 34

    level, xp = simulate_leveling(1, 0, 3000)
    assert level == 7 and xp == 131, (level, xp)
    total_to_cap = sum(xp_to_next(level) for level in range(1, MAX_LEVEL))
    assert total_to_cap == 2666730

    print("PASS: 100-level RPG curve preserves the Lv.30 anchor and tapers mastery growth")
    print(f"level100 attack=x{outgoing_damage_multiplier(100):.3f}")
    print(f"level100 incoming={incoming_damage_multiplier(100) * 100:.1f}%")
    print(f"level100 bonus_health={bonus_health_points(100)}")
    print(f"total_xp_to_level100={total_to_cap}")

if __name__ == "__main__":
    main()

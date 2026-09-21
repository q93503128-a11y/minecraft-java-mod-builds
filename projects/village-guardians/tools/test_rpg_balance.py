#!/usr/bin/env python3
"""Deterministic balance-contract checks for the 300-level Village Guardians mastery curve."""

MAX_LEVEL = 300

def xp_to_next(level: int) -> int:
    return 0 if level >= MAX_LEVEL else 120 + level * 72 + level * level * 7

def combat_scaling_level(level: int) -> int:
    safe = max(1, min(MAX_LEVEL, level))
    return safe if safe <= 100 else 100 + (safe - 100) // 4

def outgoing_damage_multiplier(level: int) -> float:
    value = combat_scaling_level(level)
    foundation = min(29, value - 1)
    mastery = max(0, value - 30)
    return (1.0 + foundation * 0.035 + (foundation // 5) * 0.08
            + mastery * 0.008 + (mastery // 10) * 0.025)

def incoming_damage_multiplier(level: int) -> float:
    value = combat_scaling_level(level)
    foundation = min(29, value - 1)
    mastery = max(0, value - 30)
    result = (1.0 - foundation * 0.009 - (foundation // 5) * 0.025
              - mastery * 0.0009 - (mastery // 10) * 0.004)
    return max(0.50, result)

def bonus_health_points(level: int) -> int:
    value = combat_scaling_level(level)
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
    assert xp_to_next(100) == 77320
    assert xp_to_next(299) == 647455
    assert xp_to_next(300) == 0

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

    # Exact legacy anchors remain intact.
    assert round(outgoing_damage_multiplier(30), 3) == 2.415
    assert round(incoming_damage_multiplier(30), 3) == 0.614
    assert bonus_health_points(30) == 20
    assert round(outgoing_damage_multiplier(100), 3) == 3.150
    assert round(incoming_damage_multiplier(100), 3) == 0.523
    assert bonus_health_points(100) == 34

    # Visible mastery continues to 300, but raw combat scaling only reaches old-equivalent Lv.150.
    assert combat_scaling_level(200) == 125
    assert combat_scaling_level(300) == 150
    assert round(outgoing_damage_multiplier(300), 3) == 3.675
    assert round(incoming_damage_multiplier(300), 3) == 0.500
    assert bonus_health_points(300) == 44

    level, xp = simulate_leveling(1, 0, 3000)
    assert level == 7 and xp == 131, (level, xp)
    total_to_cap = sum(xp_to_next(level) for level in range(1, MAX_LEVEL))
    assert total_to_cap == 65950430

    print("PASS: 300-level RPG mastery keeps the Lv.30/Lv.100 combat anchors")
    print(f"level300 combat_equivalent={combat_scaling_level(300)} attack=x{outgoing_damage_multiplier(300):.3f}")
    print(f"level300 incoming={incoming_damage_multiplier(300) * 100:.1f}%")
    print(f"level300 bonus_health={bonus_health_points(300)}")
    print(f"total_xp_to_level300={total_to_cap}")

if __name__ == "__main__":
    main()

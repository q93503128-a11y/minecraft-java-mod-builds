#!/usr/bin/env python3
"""Deterministic village territory and defense reward contract checks."""

VILLAGE_RADIUS = 64
DEFENSE_XP_MULTIPLIER = 1.5


def distance_squared(first: tuple[int, int, int], second: tuple[int, int, int]) -> int:
    return sum((a - b) ** 2 for a, b in zip(first, second, strict=True))


def is_inside_village(position: tuple[int, int, int], center: tuple[int, int, int]) -> bool:
    return distance_squared(position, center) <= VILLAGE_RADIUS**2


def rewarded_xp(base_reward: int, inside: bool) -> int:
    return round(base_reward * DEFENSE_XP_MULTIPLIER) if inside else base_reward


def main() -> None:
    center = (0, 64, 0)

    assert is_inside_village(center, center)
    assert is_inside_village((64, 64, 0), center)
    assert not is_inside_village((65, 64, 0), center)
    assert is_inside_village((32, 96, 32), center)
    assert not is_inside_village((40, 104, 40), center)

    assert rewarded_xp(100, False) == 100
    assert rewarded_xp(100, True) == 150
    assert rewarded_xp(47, True) == 70

    print("PASS: village territory contract")
    print(f"radius={VILLAGE_RADIUS}")
    print(f"defense_xp_multiplier={DEFENSE_XP_MULTIPLIER}")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3

MAX_LEVEL = 5
ROAD_X_MIN = -4
ROAD_X_MAX = 4
ROAD_Z_MIN = -76
ROAD_Z_MAX = 37

BUILDINGS = {
    "town_hall": (-18, 40, 37, 27),
    "barracks": (-71, -54, 31, 25),
    "smithy": (-71, -18, 31, 25),
    "skill_hall": (41, -18, 31, 25),
    "storehouse": (-71, 18, 31, 25),
    "infirmary": (41, 18, 31, 25),
}


def upgrade_cost(current_level: int) -> int:
    return 120 + max(0, current_level) * 140


def smithy_communal_multiplier(level: int) -> float:
    return 1.0 + level * 0.04


def personal_forge_multiplier(rank: int) -> float:
    return 1.0 + rank * 0.12


def wall_multiplier(level: int) -> float:
    return max(0.62, 0.94 - level * 0.064)


def daily_bread(storehouse_level: int) -> int:
    return 3 + storehouse_level * 2


def raid_reward_percent(storehouse: int, barracks: int) -> int:
    return 100 + storehouse * 15 + barracks * 5


def intersects_main_avenue(spec: tuple[int, int, int, int]) -> bool:
    x, z, width, depth = spec
    x2 = x + width - 1
    z2 = z + depth - 1
    return not (
        x2 < ROAD_X_MIN
        or x > ROAD_X_MAX
        or z2 < ROAD_Z_MIN
        or z > ROAD_Z_MAX
    )


def main() -> None:
    costs = [upgrade_cost(level) for level in range(MAX_LEVEL)]
    assert costs == [120, 260, 400, 540, 680]
    assert all(costs[i] < costs[i + 1] for i in range(len(costs) - 1))

    communal_attack = [smithy_communal_multiplier(level) for level in range(MAX_LEVEL + 1)]
    personal_attack = [personal_forge_multiplier(level) for level in range(MAX_LEVEL + 1)]
    defense = [wall_multiplier(level) for level in range(MAX_LEVEL + 1)]
    bread = [daily_bread(level) for level in range(MAX_LEVEL + 1)]

    assert all(communal_attack[i] < communal_attack[i + 1] for i in range(MAX_LEVEL))
    assert all(personal_attack[i] < personal_attack[i + 1] for i in range(MAX_LEVEL))
    assert all(defense[i] > defense[i + 1] for i in range(MAX_LEVEL))
    assert communal_attack[-1] == 1.20
    assert personal_attack[-1] == 1.60
    assert round(defense[-1], 2) == 0.62

    assert bread == [3, 5, 7, 9, 11, 13]
    assert raid_reward_percent(0, 0) == 100
    assert raid_reward_percent(5, 5) == 200

    blocked = [name for name, spec in BUILDINGS.items() if intersects_main_avenue(spec)]
    assert not blocked, f"buildings overlap north gate avenue: {blocked}"

    print("[PASS] Village progression upgrade costs are monotonic")
    print("[PASS] Smithy, personal forge, and wall upgrades improve combat")
    print("[PASS] Daily bread scales from 3 to 13")
    print("[PASS] Center-to-north-gate avenue contains no buildings")
    print("[PASS] Max storehouse+barracks doubles raid supplies")


if __name__ == "__main__":
    main()

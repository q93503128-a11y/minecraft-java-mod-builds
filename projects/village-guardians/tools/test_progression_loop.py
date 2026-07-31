#!/usr/bin/env python3

MAX_LEVEL = 5


def upgrade_cost(current_level: int) -> int:
    return 120 + max(0, current_level) * 140


def armory_multiplier(level: int) -> float:
    return 1.0 + level * 0.08


def wall_multiplier(level: int) -> float:
    return max(0.65, 1.0 - level * 0.06)


def raid_reward_percent(storehouse: int, barracks: int) -> int:
    return 100 + storehouse * 15 + barracks * 5


def main() -> None:
    costs = [upgrade_cost(level) for level in range(MAX_LEVEL)]
    assert costs == [120, 260, 400, 540, 680]
    assert all(costs[i] < costs[i + 1] for i in range(len(costs) - 1))

    attack = [armory_multiplier(level) for level in range(MAX_LEVEL + 1)]
    defense = [wall_multiplier(level) for level in range(MAX_LEVEL + 1)]
    assert all(attack[i] < attack[i + 1] for i in range(MAX_LEVEL))
    assert all(defense[i] > defense[i + 1] for i in range(MAX_LEVEL))
    assert round(attack[-1], 2) == 1.40
    assert round(defense[-1], 2) == 0.70

    assert raid_reward_percent(0, 0) == 100
    assert raid_reward_percent(5, 5) == 200

    print("[PASS] Village progression upgrade costs are monotonic")
    print("[PASS] Armory and wall upgrades improve combat monotonically")
    print("[PASS] Max storage+barracks doubles raid supplies")


if __name__ == "__main__":
    main()

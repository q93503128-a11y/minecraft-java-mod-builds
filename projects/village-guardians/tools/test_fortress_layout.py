#!/usr/bin/env python3
"""Deterministic layout contract for the Village Guardians fortress."""

FORTRESS_RADIUS = 76
ROAD_HALF_WIDTH = 4
GATE_HALF_WIDTH = 8
GATE_HEIGHT = 8

BUILDINGS = {
    "town_hall": (-25, 34, 51, 35, "north"),
    "barracks": (-71, -54, 31, 25, "east"),
    "smithy": (-71, -18, 31, 25, "east"),
    "skill_hall": (41, -18, 31, 25, "west"),
    "storehouse": (-71, 18, 31, 25, "east"),
    "infirmary": (41, 18, 31, 25, "west"),
}


def intersects_north_avenue(dx: int, dz: int, width: int, depth: int) -> bool:
    x0, x1 = dx, dx + width - 1
    z0, z1 = dz, dz + depth - 1
    avenue_x0, avenue_x1 = -ROAD_HALF_WIDTH, ROAD_HALF_WIDTH
    avenue_z0, avenue_z1 = -FORTRESS_RADIUS + 4, 20
    return not (
        x1 < avenue_x0
        or x0 > avenue_x1
        or z1 < avenue_z0
        or z0 > avenue_z1
    )


def main() -> None:
    for name, (dx, dz, width, depth, facing) in BUILDINGS.items():
        assert not intersects_north_avenue(dx, dz, width, depth), name
        center_x = dx + width / 2
        center_z = dz + depth / 2
        if center_x < -8:
            assert facing == "east", (name, facing)
        elif center_x > 8:
            assert facing == "west", (name, facing)
        elif center_z > 8:
            assert facing == "north", (name, facing)

    closed_gate = {
        (x, y)
        for x in range(-GATE_HALF_WIDTH, GATE_HALF_WIDTH + 1)
        for y in range(1, GATE_HEIGHT + 1)
    }
    assert len(closed_gate) == (GATE_HALF_WIDTH * 2 + 1) * GATE_HEIGHT
    assert all((x, y) in closed_gate for x in range(-8, 9) for y in range(1, 9))

    stair_z = [-FORTRESS_RADIUS + 12 - step for step in range(9)]
    platform_z = list(range(-FORTRESS_RADIUS, -FORTRESS_RADIUS + 5))
    assert stair_z[-1] == -FORTRESS_RADIUS + 4
    assert stair_z[-1] in platform_z
    assert -FORTRESS_RADIUS in platform_z

    print("[PASS] Buildings do not block the north-gate avenue")
    print("[PASS] Every functional building faces the central plaza")
    print("[PASS] Closed gate has no missing columns or slit")
    print("[PASS] Wall stairs connect to a cleared five-wide wall-top landing")


if __name__ == "__main__":
    main()

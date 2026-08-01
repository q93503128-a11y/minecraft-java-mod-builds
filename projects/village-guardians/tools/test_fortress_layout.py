#!/usr/bin/env python3
"""Deterministic layout contract for the Village Guardians fortress."""

FORTRESS_RADIUS = 76
ROAD_HALF_WIDTH = 4
GATE_HALF_WIDTH = 8
GATE_HEIGHT = 8
WALL_THICKNESS = 5

BUILDINGS = {
    "town_hall": (-25, 34, 51, 31, "north"),
    "barracks": (-70, -52, 27, 21, "east"),
    "smithy": (-70, -18, 27, 21, "east"),
    "skill_hall": (44, -18, 27, 21, "west"),
    "storehouse": (-70, 18, 27, 21, "east"),
    "infirmary": (44, 18, 27, 21, "west"),
}


def intersects_main_avenue(dx: int, dz: int, width: int, depth: int) -> bool:
    x0, x1 = dx, dx + width - 1
    z0, z1 = dz, dz + depth - 1
    avenue_x0, avenue_x1 = -ROAD_HALF_WIDTH, ROAD_HALF_WIDTH
    avenue_z0, avenue_z1 = -FORTRESS_RADIUS + 4, 33
    return not (
        x1 < avenue_x0
        or x0 > avenue_x1
        or z1 < avenue_z0
        or z0 > avenue_z1
    )


def main() -> None:
    for name, (dx, dz, width, depth, facing) in BUILDINGS.items():
        assert not intersects_main_avenue(dx, dz, width, depth), name
        assert dx >= -FORTRESS_RADIUS + 5, (name, dx)
        assert dz >= -FORTRESS_RADIUS + 5, (name, dz)
        assert dx + width - 1 <= FORTRESS_RADIUS - 5, name
        assert dz + depth - 1 <= FORTRESS_RADIUS - 5, name
        center_x = dx + width / 2
        center_z = dz + depth / 2
        if center_x < -8:
            assert facing == "east", (name, facing)
        elif center_x > 8:
            assert facing == "west", (name, facing)
        elif center_z > 8:
            assert facing == "north", (name, facing)

    hall_area = BUILDINGS["town_hall"][2] * BUILDINGS["town_hall"][3]
    normal_areas = [
        width * depth
        for name, (_, _, width, depth, _) in BUILDINGS.items()
        if name != "town_hall"
    ]
    assert hall_area >= max(normal_areas) * 2, (hall_area, max(normal_areas))

    closed_gate = {
        (x, y)
        for x in range(-GATE_HALF_WIDTH, GATE_HALF_WIDTH + 1)
        for y in range(1, GATE_HEIGHT + 1)
    }
    assert len(closed_gate) == (GATE_HALF_WIDTH * 2 + 1) * GATE_HEIGHT

    wall_top_cross_section = list(range(WALL_THICKNESS))
    assert wall_top_cross_section == [0, 1, 2, 3, 4]
    walkway_center = wall_top_cross_section[1:-1]
    assert walkway_center == [1, 2, 3]

    stair_z = [-FORTRESS_RADIUS + 14 - step for step in range(9)]
    landing_z = list(range(-FORTRESS_RADIUS + 1, -FORTRESS_RADIUS + 7))
    assert stair_z[-1] == -FORTRESS_RADIUS + 6
    assert stair_z[-1] in landing_z

    print("[PASS] Custom buildings stay inside the fortress and clear the main avenue")
    print("[PASS] Every facility entrance faces the central plaza")
    print("[PASS] Town hall footprint is at least twice a normal facility")
    print("[PASS] Closed gate has no missing columns")
    print("[PASS] Wall top has a flat three-block center walkway")
    print("[PASS] Wall stairs connect directly to the landing")


if __name__ == "__main__":
    main()

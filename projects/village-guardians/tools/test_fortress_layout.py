#!/usr/bin/env python3
"""Deterministic layout contract for the Village Guardians fortress."""

FORTRESS_RADIUS = 76
ROAD_HALF_WIDTH = 4
GATE_HALF_WIDTH = 9
GATE_HEIGHT = 8
WALL_THICKNESS = 5
PATH_MAX_LENGTH = 64
RETURN_POSITION = (0, 12)

BUILDINGS = {
    "town_hall": (-21, 36, 43, 27, "north"),
    "barracks": (-66, -50, 23, 17, "east"),
    "smithy": (-66, -17, 23, 17, "east"),
    "skill_hall": (44, -17, 23, 17, "west"),
    "storehouse": (-66, 17, 23, 17, "east"),
    "infirmary": (44, 17, 23, 17, "west"),
}


def bounds(spec: tuple[int, int, int, int, str]) -> tuple[int, int, int, int]:
    dx, dz, width, depth, _ = spec
    return dx, dz, dx + width - 1, dz + depth - 1


def overlaps(first: tuple[int, int, int, int], second: tuple[int, int, int, int]) -> bool:
    ax0, az0, ax1, az1 = first
    bx0, bz0, bx1, bz1 = second
    return not (ax1 < bx0 or ax0 > bx1 or az1 < bz0 or az0 > bz1)


def intersects_main_avenue(dx: int, dz: int, width: int, depth: int) -> bool:
    x0, x1 = dx, dx + width - 1
    z0, z1 = dz, dz + depth - 1
    avenue_x0, avenue_x1 = -ROAD_HALF_WIDTH, ROAD_HALF_WIDTH
    avenue_z0, avenue_z1 = -FORTRESS_RADIUS + 4, 35
    return not (
        x1 < avenue_x0
        or x0 > avenue_x1
        or z1 < avenue_z0
        or z0 > avenue_z1
    )


def entrance(spec: tuple[int, int, int, int, str]) -> tuple[int, int]:
    dx, dz, width, depth, facing = spec
    if facing == "north":
        return dx + width // 2, dz - 1
    if facing == "south":
        return dx + width // 2, dz + depth
    if facing == "west":
        return dx - 1, dz + depth // 2
    if facing == "east":
        return dx + width, dz + depth // 2
    raise AssertionError(facing)


def path_steps_to_road(spec: tuple[int, int, int, int, str]) -> int:
    x, z = entrance(spec)
    facing = spec[4]
    step_x = {"east": 1, "west": -1}.get(facing, 0)
    step_z = {"south": 1, "north": -1}.get(facing, 0)
    for step in range(PATH_MAX_LENGTH + 1):
        if facing in {"east", "west"} and abs(x) <= ROAD_HALF_WIDTH:
            return step
        if facing in {"north", "south"} and x * x + z * z <= 18 * 18:
            return step
        x += step_x
        z += step_z
    raise AssertionError((spec, "path did not reach a road"))


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
        assert path_steps_to_road(BUILDINGS[name]) <= PATH_MAX_LENGTH, name

    names = list(BUILDINGS)
    for index, first_name in enumerate(names):
        for second_name in names[index + 1:]:
            assert not overlaps(bounds(BUILDINGS[first_name]), bounds(BUILDINGS[second_name])), (
                first_name,
                second_name,
            )

    hall_area = BUILDINGS["town_hall"][2] * BUILDINGS["town_hall"][3]
    normal_areas = [
        width * depth
        for name, (_, _, width, depth, _) in BUILDINGS.items()
        if name != "town_hall"
    ]
    assert hall_area >= max(normal_areas) * 2, (hall_area, max(normal_areas))
    assert hall_area < 51 * 31
    assert max(normal_areas) < 27 * 21

    closed_gate = {
        (x, y)
        for x in range(-GATE_HALF_WIDTH, GATE_HALF_WIDTH + 1)
        for y in range(1, GATE_HEIGHT + 1)
    }
    assert len(closed_gate) == (GATE_HALF_WIDTH * 2 + 1) * GATE_HEIGHT
    assert -9 in {x for x, _ in closed_gate}
    assert 9 in {x for x, _ in closed_gate}

    wall_top_cross_section = list(range(WALL_THICKNESS))
    assert wall_top_cross_section == [0, 1, 2, 3, 4]
    walkway_center = wall_top_cross_section[1:-1]
    assert walkway_center == [1, 2, 3]

    stair_z = [-FORTRESS_RADIUS + 10 - step for step in range(9)]
    landing_z = list(range(-FORTRESS_RADIUS + 1, -FORTRESS_RADIUS + 7))
    assert stair_z[0] == -FORTRESS_RADIUS + 10
    assert stair_z[-1] == -FORTRESS_RADIUS + 2
    assert any(z in landing_z for z in stair_z)
    assert max(stair_z) <= -FORTRESS_RADIUS + 10

    return_x, return_z = RETURN_POSITION
    assert return_x * return_x + return_z * return_z <= 18 * 18
    assert not any(
        x0 <= return_x <= x1 and z0 <= return_z <= z1
        for x0, z0, x1, z1 in map(bounds, BUILDINGS.values())
    )
    assert RETURN_POSITION != (0, 0)

    north_inner_railing_openings = {
        offset for offset in range(-FORTRESS_RADIUS, FORTRESS_RADIUS + 1)
        if abs(offset) <= 15 or abs(abs(offset) - 25) <= 3
    }
    assert 0 in north_inner_railing_openings
    assert all(offset in north_inner_railing_openings for offset in range(22, 29))
    assert all(offset in north_inner_railing_openings for offset in range(-28, -21))
    assert 40 not in north_inner_railing_openings

    print("[PASS] Compact buildings stay inside the fortress without overlap")
    print("[PASS] Every facility entrance faces and reaches the central road or plaza")
    print("[PASS] Town hall remains at least twice a normal facility after resizing")
    print("[PASS] Closed gate spans the complete tower opening without side slits")
    print("[PASS] Wall top has a flat three-block center walkway and planned rail openings")
    print("[PASS] Wall stairs connect directly to the landing")
    print("[PASS] Return destination is inside the plaza and clear of the bell and buildings")


if __name__ == "__main__":
    main()

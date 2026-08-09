#!/usr/bin/env python3
import json
import math
from pathlib import Path

import audit_living_kingdoms_exterior_residence_overlap as legacy

ROOT = Path(__file__).resolve().parents[2]
HAMLET_DISTANCE = 96
SIDE_OFFSETS = (0, -24, 24, -48, 48)
WIDTH = 9
DEPTH = 9
ROAD_HALF_WIDTH = 2
GATES = ((-1200, 0), (1200, 0), (0, -900), (0, 900))


def households_for(role):
    workers = {
        "grain_estate": 8,
        "ranch": 7,
        "colliery": 9,
        "iron_mine": 10,
        "paper_mill": 8,
        "river_wharf": 6,
    }[role]
    return (workers + 1) // 2


def physical_anchor(node, local_index):
    if node["role"] == "paper_mill":
        outward_x = 0
        outward_z = (node["z"] > 0) - (node["z"] < 0)
        if outward_z == 0:
            outward_z = 1
    elif abs(node["x"]) >= abs(node["z"]):
        outward_x = (node["x"] > 0) - (node["x"] < 0)
        outward_z = 0
    else:
        outward_x = 0
        outward_z = (node["z"] > 0) - (node["z"] < 0)
    if outward_x == 0 and outward_z == 0:
        outward_z = 1
    side_x = -outward_z
    side_z = outward_x
    side = SIDE_OFFSETS[local_index]
    return (
        node["x"] + outward_x * HAMLET_DISTANCE + side_x * side,
        node["z"] + outward_z * HAMLET_DISTANCE + side_z * side,
    )


def footprint(node, anchor_x, anchor_z):
    chunk_x = anchor_x >> 4
    chunk_z = anchor_z >> 4
    chunk_min_x = chunk_x << 4
    chunk_min_z = chunk_z << 4
    desired_x = anchor_x - WIDTH // 2
    desired_z = anchor_z - DEPTH // 2
    min_x = max(chunk_min_x + 1, min(chunk_min_x + 6, desired_x))
    min_z = max(chunk_min_z + 1, min(chunk_min_z + 6, desired_z))
    max_x = min_x + WIDTH - 1
    max_z = min_z + DEPTH - 1
    center_x = min_x + WIDTH // 2
    center_z = min_z + DEPTH // 2
    dx = node["x"] - center_x
    dz = node["z"] - center_z
    if abs(dx) >= abs(dz):
        facing = "EAST" if dx >= 0 else "WEST"
    else:
        facing = "SOUTH" if dz >= 0 else "NORTH"
    return {
        "chunk": (chunk_x, chunk_z),
        "min_x": min_x,
        "min_z": min_z,
        "max_x": max_x,
        "max_z": max_z,
        "center_x": center_x,
        "center_z": center_z,
        "facing": facing,
    }


def residence_cells(fp):
    cells = {(x, z) for x in range(fp["min_x"], fp["max_x"] + 1)
                     for z in range(fp["min_z"], fp["max_z"] + 1)}
    cx0, cz0 = fp["chunk"]
    if fp["facing"] == "NORTH":
        door = (fp["center_x"], fp["min_z"])
        step = (0, -1)
    elif fp["facing"] == "SOUTH":
        door = (fp["center_x"], fp["max_z"])
        step = (0, 1)
    elif fp["facing"] == "WEST":
        door = (fp["min_x"], fp["center_z"])
        step = (-1, 0)
    else:
        door = (fp["max_x"], fp["center_z"])
        step = (1, 0)
    path = []
    for distance in range(1, 6):
        x = door[0] + step[0] * distance
        z = door[1] + step[1] * distance
        if (x >> 4, z >> 4) != (cx0, cz0):
            break
        path.append((x, z))
        cells.add((x, z))
    return cells, path


def structure_cells(node, template):
    turns = node["turns"] % 4
    rotated_width = template["length"] if turns in (1, 3) else template["width"]
    rotated_length = template["width"] if turns in (1, 3) else template["length"]
    origin_x = node["x"] - rotated_width // 2
    origin_z = node["z"] - rotated_length // 2
    cells = set()
    for x, _y, z in template["blocks"]:
        rx, rz = legacy.rotate(x, z, template["width"], template["length"], turns)
        cells.add((origin_x + rx, origin_z + rz))
    return cells


def site_cells(node):
    x0, z0 = node["x"], node["z"]
    role = node["role"]
    cells = set()

    def rect(x1, z1, x2, z2):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                cells.add((x, z))

    if role == "grain_estate":
        rect(x0 - 62, z0 - 27, x0 + 62, z0 + 27)
        rect(x0 - 8, z0 + 34, x0 + 8, z0 + 45)
    elif role == "ranch":
        rect(x0 - 56, z0 - 38, x0 - 8, z0 + 38)
        rect(x0 + 8, z0 - 38, x0 + 56, z0 + 38)
        rect(x0 - 10, z0 + 34, x0 + 10, z0 + 46)
    elif role in ("colliery", "iron_mine"):
        rect(x0 + 15, z0 - 7, x0 + 55, z0 + 7)
        rect(x0 - 36, z0 + 18, x0 - 24, z0 + 30)
    elif role == "paper_mill":
        rect(x0 - 46, z0 + 18, x0 + 46, z0 + 27)
        rect(x0 - 42, z0 - 8, x0 - 18, z0 + 10)
        rect(x0 + 14, z0 + 14, x0 + 30, z0 + 30)
    elif role == "river_wharf":
        rect(x0 - 11, z0 - 11, x0 + 11, z0 + 11)
        direction_x = -1 if node["turns"] == 1 else 1 if node["turns"] == 3 else 0
        direction_z = -1 if node["turns"] == 0 else 1 if node["turns"] == 2 else 0
        for length in range(0, 43):
            for width in range(-6, 7):
                x = x0 + direction_x * length + (width if direction_z else 0)
                z = z0 + direction_z * length + (width if direction_x else 0)
                cells.add((x, z))
    else:
        raise RuntimeError(f"unknown role {role}")
    return cells


def manhattan(a, b):
    return abs(a[0] - b[0]) + abs(a[1] - b[1])


def road_destination(node, all_nodes):
    if node["role"] == "paper_mill":
        wharves = [n for n in all_nodes if n["role"] == "river_wharf"]
        nearest = min(wharves, key=lambda n: manhattan((node["x"], node["z"]), (n["x"], n["z"])))
        return nearest["x"], nearest["z"]
    return min(GATES, key=lambda gate: manhattan((node["x"], node["z"]), gate))


def road_cells(node, all_nodes):
    end_x, end_z = road_destination(node, all_nodes)
    cells = set()
    for x in range(min(node["x"], end_x) - ROAD_HALF_WIDTH,
                   max(node["x"], end_x) + ROAD_HALF_WIDTH + 1):
        for z in range(node["z"] - ROAD_HALF_WIDTH, node["z"] + ROAD_HALF_WIDTH + 1):
            cells.add((x, z))
    for x in range(end_x - ROAD_HALF_WIDTH, end_x + ROAD_HALF_WIDTH + 1):
        for z in range(min(node["z"], end_z) - ROAD_HALF_WIDTH,
                       max(node["z"], end_z) + ROAD_HALF_WIDTH + 1):
            cells.add((x, z))
    return cells


def main():
    nodes = legacy.nodes()
    templates = {style: legacy.load_template(style, url) for style, url in legacy.ASSETS.items()}
    structure_by_node = {n["id"]: structure_cells(n, templates[n["style"]]) for n in nodes}
    site_by_node = {n["id"]: site_cells(n) for n in nodes}
    road_by_node = {n["id"]: road_cells(n, nodes) for n in nodes}

    reports = []
    all_home_cells = {}
    physical_chunks = set()
    failures = []
    global_index = 0
    for node in nodes:
        for local_index in range(households_for(node["role"])):
            global_index += 1
            household_id = f"erden_exterior_household_{global_index:03d}"
            px, pz = physical_anchor(node, local_index)
            fp = footprint(node, px, pz)
            cells, path = residence_cells(fp)
            physical_chunks.add(fp["chunk"])
            structure_overlap = cells & structure_by_node[node["id"]]
            site_overlap = cells & site_by_node[node["id"]]
            road_overlap = cells & road_by_node[node["id"]]
            other_overlap = set()
            for other_id, other_cells in all_home_cells.items():
                if cells & other_cells:
                    other_overlap.add(other_id)
            all_home_cells[household_id] = cells
            report = {
                "household_id": household_id,
                "node_id": node["id"],
                "role": node["role"],
                "local_index": local_index,
                "physical_anchor": [px, pz],
                "physical_chunk": list(fp["chunk"]),
                "footprint": [fp["min_x"], fp["min_z"], fp["max_x"], fp["max_z"]],
                "path_cells": [list(p) for p in path],
                "structure_overlap": len(structure_overlap),
                "site_overlap": len(site_overlap),
                "road_overlap": len(road_overlap),
                "other_home_overlap": sorted(other_overlap),
                "center_distance": math.hypot(fp["center_x"] - node["x"], fp["center_z"] - node["z"]),
            }
            reports.append(report)
            if structure_overlap or site_overlap or road_overlap or other_overlap:
                failures.append(report)

    if global_index != 74:
        raise SystemExit(f"expected 74 households, found {global_index}")
    if len(physical_chunks) != 74:
        raise SystemExit(f"expected 74 unique physical chunks, found {len(physical_chunks)}")

    summary = {
        "households": global_index,
        "unique_physical_chunks": len(physical_chunks),
        "structure_collisions": sum(1 for r in reports if r["structure_overlap"]),
        "site_collisions": sum(1 for r in reports if r["site_overlap"]),
        "road_collisions": sum(1 for r in reports if r["road_overlap"]),
        "home_collisions": sum(1 for r in reports if r["other_home_overlap"]),
        "minimum_center_distance": min(r["center_distance"] for r in reports),
        "reports": reports,
    }
    out = ROOT / "logs" / "exterior-residence-hamlets.json"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    print(json.dumps({k: v for k, v in summary.items() if k != "reports"}, ensure_ascii=False))
    if failures:
        for failure in failures:
            print("COLLISION", json.dumps(failure, ensure_ascii=False))
        raise SystemExit(f"Erden residence hamlet collision audit failed for {len(failures)} households")
    print("LK_ERDEN_RESIDENCE_HAMLET_PASS households=74 physical_chunks=74 structure_collisions=0 site_collisions=0 road_collisions=0 home_collisions=0")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
from __future__ import annotations

import collections
import gzip
import io
import json
import math
import pathlib
import statistics
import struct
import sys
import zlib

MIN_Y = -64
WORLD_HEIGHT = 384
HEIGHT_BITS = math.ceil(math.log2(WORLD_HEIGHT + 1))
HEIGHT_VALUES_PER_LONG = 64 // HEIGHT_BITS
SAMPLE_STEP = 4


class NbtReader:
    def __init__(self, data: bytes):
        if data[:2] == b"\x1f\x8b":
            data = gzip.decompress(data)
        self.fp = io.BytesIO(data)

    def read(self, fmt: str):
        size = struct.calcsize(">" + fmt)
        raw = self.fp.read(size)
        if len(raw) != size:
            raise EOFError("unexpected end of NBT")
        return struct.unpack(">" + fmt, raw)[0]

    def string(self) -> str:
        n = self.read("H")
        return self.fp.read(n).decode("utf-8", errors="replace")

    def payload(self, tag: int):
        if tag == 0:
            return None
        if tag == 1:
            return self.read("b")
        if tag == 2:
            return self.read("h")
        if tag == 3:
            return self.read("i")
        if tag == 4:
            return self.read("q")
        if tag == 5:
            return self.read("f")
        if tag == 6:
            return self.read("d")
        if tag == 7:
            n = self.read("i")
            return self.fp.read(n)
        if tag == 8:
            return self.string()
        if tag == 9:
            child = self.read("B")
            n = self.read("i")
            return [self.payload(child) for _ in range(n)]
        if tag == 10:
            out = {}
            while True:
                child = self.read("B")
                if child == 0:
                    return out
                name = self.string()
                out[name] = self.payload(child)
        if tag == 11:
            n = self.read("i")
            return [self.read("i") for _ in range(n)]
        if tag == 12:
            n = self.read("i")
            return [self.read("q") for _ in range(n)]
        raise ValueError(f"unsupported NBT tag {tag}")

    def root(self):
        tag = self.read("B")
        if tag != 10:
            raise ValueError(f"expected root compound, got tag {tag}")
        _ = self.string()
        return self.payload(tag)


def unsigned64(v: int) -> int:
    return v & ((1 << 64) - 1)


def decode_heightmap(values: list[int]) -> list[int]:
    mask = (1 << HEIGHT_BITS) - 1
    out: list[int] = []
    for raw in values:
        u = unsigned64(raw)
        for i in range(HEIGHT_VALUES_PER_LONG):
            if len(out) == 256:
                break
            stored = (u >> (i * HEIGHT_BITS)) & mask
            out.append(int(stored + MIN_Y - 1))
    if len(out) < 256:
        raise ValueError(f"short heightmap: {len(out)} values")
    return out[:256]


NON_SOLID_EXACT = {
    "minecraft:air", "minecraft:cave_air", "minecraft:void_air",
}
FLUID_EXACT = {"minecraft:water", "minecraft:lava"}
IGNORE_SURFACE_PARTS = (
    "_leaves", "_sapling", "_flower", "_tulip", "_mushroom",
    "grass", "fern", "bush", "vine", "lily_pad", "sugar_cane",
    "bamboo", "cactus", "seagrass", "kelp", "coral", "azalea",
    "snow", "torch", "lantern",
)


def should_ignore_as_surface(name: str) -> bool:
    short = name.split(":", 1)[-1]
    return any(part in short for part in IGNORE_SURFACE_PARTS)


def make_block_accessor(section: dict):
    states = section.get("block_states")
    if not isinstance(states, dict):
        return None
    palette = states.get("palette")
    if not isinstance(palette, list) or not palette:
        return None

    names: list[str] = []
    for entry in palette:
        if isinstance(entry, dict):
            names.append(entry.get("Name") or "minecraft:air")
        else:
            names.append("minecraft:air")

    if len(names) == 1:
        return lambda index: names[0]

    data = states.get("data")
    if not isinstance(data, list) or not data:
        return None

    bits = max(4, math.ceil(math.log2(len(names))))
    values_per_long = 64 // bits
    mask = (1 << bits) - 1

    def get(index: int) -> str:
        long_index = index // values_per_long
        if long_index >= len(data):
            return "minecraft:air"
        bit_offset = (index % values_per_long) * bits
        palette_index = (unsigned64(data[long_index]) >> bit_offset) & mask
        if palette_index >= len(names):
            return "minecraft:air"
        return names[palette_index]

    return get


def sampled_surface_from_sections(chunk: dict):
    usable = []
    for section in chunk.get("sections", []):
        if not isinstance(section, dict):
            continue
        sy = section.get("Y")
        if not isinstance(sy, int):
            continue
        accessor = make_block_accessor(section)
        if accessor is not None:
            usable.append((sy, accessor))
    usable.sort(key=lambda row: row[0], reverse=True)

    points = []
    for lz in range(0, 16, SAMPLE_STEP):
        for lx in range(0, 16, SAMPLE_STEP):
            water_above = False
            chosen_y = None
            is_land = False
            for sy, accessor in usable:
                for ly in range(15, -1, -1):
                    index = (ly << 8) | (lz << 4) | lx
                    name = accessor(index)
                    if name in NON_SOLID_EXACT:
                        continue
                    if name in FLUID_EXACT:
                        water_above = True
                        continue
                    if should_ignore_as_surface(name):
                        continue
                    chosen_y = sy * 16 + ly
                    is_land = not water_above
                    break
                if chosen_y is not None:
                    break
            if chosen_y is None:
                chosen_y = MIN_Y
            points.append((lx, lz, chosen_y, is_land))
    return points


def parse_region(path: pathlib.Path, diag: dict):
    parts = path.name.split(".")
    rx, rz = int(parts[1]), int(parts[2])
    data = path.read_bytes()
    if len(data) < 8192:
        return

    for idx in range(1024):
        loc = data[idx * 4: idx * 4 + 4]
        offset = int.from_bytes(loc[:3], "big")
        sectors = loc[3]
        if not offset or not sectors:
            continue

        pos = offset * 4096
        if pos + 5 > len(data):
            continue
        length = int.from_bytes(data[pos:pos + 4], "big")
        ctype = data[pos + 4]
        payload = data[pos + 5: pos + 4 + length]
        diag["compression_types"][str(ctype)] += 1

        try:
            if ctype == 1:
                raw = gzip.decompress(payload)
            elif ctype == 2:
                raw = zlib.decompress(payload)
            elif ctype == 3:
                raw = payload
            else:
                diag["skipped_compression_types"][str(ctype)] += 1
                continue
            root = NbtReader(raw).root()
            chunk = root.get("Level", root)
            diag["parsed_chunks"] += 1
        except Exception as exc:
            diag["parse_errors"][type(exc).__name__] += 1
            if len(diag["first_errors"]) < 8:
                diag["first_errors"].append(
                    f"{path.name} idx={idx} ctype={ctype}: {type(exc).__name__}: {exc}"
                )
            continue

        lx = idx % 32
        lz = idx // 32
        cx = rx * 32 + lx
        cz = rz * 32 + lz

        maps = chunk.get("Heightmaps") or {}
        chosen = None
        for key in ("OCEAN_FLOOR", "MOTION_BLOCKING_NO_LEAVES", "WORLD_SURFACE"):
            arr = maps.get(key)
            if isinstance(arr, list) and arr:
                chosen = (key, arr)
                break

        if chosen is not None:
            try:
                heights = decode_heightmap(chosen[1])
                points = [
                    (i & 15, i >> 4, y, y >= 63)
                    for i, y in enumerate(heights)
                ]
                diag["heightmap_chunks"] += 1
                yield cx, cz, points, chosen[0]
                continue
            except Exception as exc:
                diag["heightmap_decode_errors"] += 1
                if len(diag["first_errors"]) < 8:
                    diag["first_errors"].append(
                        f"{path.name} idx={idx} heightmap: {type(exc).__name__}: {exc}"
                    )

        points = sampled_surface_from_sections(chunk)
        if points:
            diag["blockstate_fallback_chunks"] += 1
            yield cx, cz, points, "BLOCK_STATES_SAMPLE"


def components(points: set[tuple[int, int]], step: int):
    remaining = set(points)
    out = []
    while remaining:
        seed = remaining.pop()
        q = collections.deque([seed])
        comp = [seed]
        while q:
            x, z = q.popleft()
            for nb in ((x + step, z), (x - step, z), (x, z + step), (x, z - step)):
                if nb in remaining:
                    remaining.remove(nb)
                    q.append(nb)
                    comp.append(nb)
        out.append(comp)
    return sorted(out, key=len, reverse=True)


def percentile(vals: list[int], p: float):
    if not vals:
        return None
    vals = sorted(vals)
    return vals[min(len(vals) - 1, max(0, int((len(vals) - 1) * p)))]


def best_window(good_tiles: set[tuple[int, int]], width_tiles: int):
    if not good_tiles:
        return {"good_tiles": 0, "fraction": 0.0, "bounds": None}
    txs = [t[0] for t in good_tiles]
    tzs = [t[1] for t in good_tiles]
    best_count, best_origin = 0, None
    for tx in range(min(txs) - width_tiles + 1, max(txs) + 1):
        for tz in range(min(tzs) - width_tiles + 1, max(tzs) + 1):
            count = sum(
                (x, z) in good_tiles
                for x in range(tx, tx + width_tiles)
                for z in range(tz, tz + width_tiles)
            )
            if count > best_count:
                best_count, best_origin = count, (tx, tz)
    bounds = None
    if best_origin:
        tx, tz = best_origin
        bounds = [tx * 16, tz * 16, (tx + width_tiles) * 16 - 1, (tz + width_tiles) * 16 - 1]
    total = width_tiles * width_tiles
    return {
        "good_tiles": best_count,
        "fraction": round(best_count / total, 4),
        "bounds": bounds,
    }


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: probe_candidate_world.py <world-root>", file=sys.stderr)
        return 2

    root = pathlib.Path(sys.argv[1])
    level = root / "level.dat"
    region_dir = root / "region"
    if not level.exists() or not region_dir.exists():
        raise SystemExit(f"invalid world root: {root}")

    level_nbt = NbtReader(level.read_bytes()).root()
    level_data = level_nbt.get("Data", level_nbt)
    summary = {
        "world_root": str(root),
        "level_name": level_data.get("LevelName"),
        "data_version": level_data.get("DataVersion"),
        "spawn": [level_data.get("SpawnX"), level_data.get("SpawnY"), level_data.get("SpawnZ")],
        "surface_sample_step_blocks": SAMPLE_STEP,
    }

    region_files = sorted(region_dir.glob("r.*.*.mca"))
    summary["region_files"] = len(region_files)

    diag = {
        "compression_types": collections.Counter(),
        "skipped_compression_types": collections.Counter(),
        "parse_errors": collections.Counter(),
        "first_errors": [],
        "parsed_chunks": 0,
        "heightmap_chunks": 0,
        "heightmap_decode_errors": 0,
        "blockstate_fallback_chunks": 0,
    }

    surface_y: dict[tuple[int, int], int] = {}
    land: set[tuple[int, int]] = set()
    kind_counts = collections.Counter()

    for region in region_files:
        for cx, cz, points, kind in parse_region(region, diag):
            kind_counts[kind] += 1
            bx, bz = cx * 16, cz * 16
            for lx, lz, y, is_land in points:
                pos = (bx + lx, bz + lz)
                surface_y[pos] = y
                if is_land:
                    land.add(pos)

    summary["surface_kind_counts"] = dict(kind_counts)
    summary["sample_points"] = len(surface_y)
    summary["land_sample_points"] = len(land)
    summary["diagnostics"] = {
        k: dict(v) if isinstance(v, collections.Counter) else v
        for k, v in diag.items()
    }

    if not surface_y:
        raise SystemExit("no usable surface samples found")

    xs = [p[0] for p in surface_y]
    zs = [p[1] for p in surface_y]
    summary["scanned_bounds"] = [min(xs), min(zs), max(xs), max(zs)]

    land_comps = components(land, SAMPLE_STEP)
    islands = []
    for comp in land_comps[:20]:
        xs2 = [p[0] for p in comp]
        zs2 = [p[1] for p in comp]
        hs = [surface_y[p] for p in comp]
        med = statistics.median(hs)
        within3 = sum(abs(h - med) <= 3 for h in hs)
        islands.append({
            "sample_points": len(comp),
            "approx_area_blocks": len(comp) * SAMPLE_STEP * SAMPLE_STEP,
            "bbox": [min(xs2), min(zs2), max(xs2), max(zs2)],
            "bbox_size_blocks": [max(xs2) - min(xs2) + SAMPLE_STEP, max(zs2) - min(zs2) + SAMPLE_STEP],
            "height_min": min(hs),
            "height_p10": percentile(hs, 0.10),
            "height_median": med,
            "height_p90": percentile(hs, 0.90),
            "height_max": max(hs),
            "within_median_plus_minus_3": round(within3 / len(hs), 4),
        })
    summary["land_components"] = islands

    # 16x16 tile quality from 4x4 sampled surface points per tile.
    tile_heights = collections.defaultdict(list)
    for pos in land:
        x, z = pos
        tile_heights[(math.floor(x / 16), math.floor(z / 16))].append(surface_y[pos])

    expected = (16 // SAMPLE_STEP) ** 2
    good_tiles = set()
    tile_detail = {}
    for tile, hs in tile_heights.items():
        if len(hs) < math.ceil(expected * 0.90):
            continue
        p10 = percentile(hs, 0.10)
        p90 = percentile(hs, 0.90)
        spread = p90 - p10
        if spread <= 4:
            good_tiles.add(tile)
            tile_detail[tile] = {"samples": len(hs), "p10": p10, "p90": p90, "spread": spread}

    good_comps = components(good_tiles, 1)
    summary["buildable_16x16_tiles"] = len(good_tiles)
    summary["largest_connected_buildable_tile_cluster"] = len(good_comps[0]) if good_comps else 0

    if good_comps:
        largest = good_comps[0]
        gx = [p[0] for p in largest]
        gz = [p[1] for p in largest]
        summary["largest_buildable_cluster_bbox_blocks"] = [
            min(gx) * 16, min(gz) * 16, (max(gx) + 1) * 16 - 1, (max(gz) + 1) * 16 - 1
        ]
        summary["largest_buildable_cluster_bbox_size_blocks"] = [
            (max(gx) - min(gx) + 1) * 16,
            (max(gz) - min(gz) + 1) * 16,
        ]

    summary["best_128x128_window"] = best_window(good_tiles, 8)
    summary["best_160x160_window"] = best_window(good_tiles, 10)
    summary["best_192x192_window"] = best_window(good_tiles, 12)

    out = root.parent / "campfire-world-probe.json"
    out.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

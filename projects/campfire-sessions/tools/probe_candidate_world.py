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
SEA_LEVEL = 63
HEIGHT_BITS = math.ceil(math.log2(WORLD_HEIGHT + 1))
VALUES_PER_LONG = 64 // HEIGHT_BITS


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
        return self.fp.read(n).decode("utf-8")

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
        raise ValueError(f"unsupported tag {tag}")

    def root(self):
        tag = self.read("B")
        if tag != 10:
            raise ValueError(f"expected root compound, got {tag}")
        _ = self.string()
        return self.payload(tag)


def unsigned64(v: int) -> int:
    return v & ((1 << 64) - 1)


def decode_heightmap(values: list[int]) -> list[int]:
    mask = (1 << HEIGHT_BITS) - 1
    out = []
    for raw in values:
        u = unsigned64(raw)
        for i in range(VALUES_PER_LONG):
            if len(out) == 256:
                break
            stored = (u >> (i * HEIGHT_BITS)) & mask
            out.append(int(stored + MIN_Y - 1))
    if len(out) < 256:
        raise ValueError(f"short heightmap: {len(out)} values")
    return out[:256]


def parse_region(path: pathlib.Path, diag: dict):
    name = path.stem.split(".")
    rx, rz = int(name[1]), int(name[2])
    data = path.read_bytes()
    if len(data) < 8192:
        return
    for idx in range(1024):
        loc = data[idx * 4 : idx * 4 + 4]
        offset = int.from_bytes(loc[:3], "big")
        sectors = loc[3]
        if not offset or not sectors:
            continue
        pos = offset * 4096
        if pos + 5 > len(data):
            continue
        length = int.from_bytes(data[pos : pos + 4], "big")
        ctype = data[pos + 4]
        diag["compression_types"][str(ctype)] += 1
        payload = data[pos + 5 : pos + 4 + length]
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
            diag["parsed_chunks"] += 1
            if not diag.get("sample_root_keys"):
                diag["sample_root_keys"] = sorted(root.keys())
                probe = root.get("Level", root)
                if isinstance(probe, dict):
                    diag["sample_chunk_keys"] = sorted(probe.keys())
        except Exception as exc:
            diag["parse_errors"][type(exc).__name__] += 1
            if len(diag["first_parse_errors"]) < 8:
                diag["first_parse_errors"].append(f"{path.name} idx={idx} ctype={ctype}: {type(exc).__name__}: {exc}")
            continue
        chunk = root.get("Level", root)
        maps = chunk.get("Heightmaps") or chunk.get("heightmaps") or {}
        if not maps:
            diag["chunks_without_heightmaps"] += 1
        chosen = None
        for key in ("OCEAN_FLOOR", "MOTION_BLOCKING_NO_LEAVES", "WORLD_SURFACE"):
            arr = maps.get(key)
            if isinstance(arr, list) and arr:
                chosen = (key, arr)
                break
        if chosen is None:
            continue
        try:
            heights = decode_heightmap(chosen[1])
        except Exception:
            continue
        lx = idx % 32
        lz = idx // 32
        cx = rx * 32 + lx
        cz = rz * 32 + lz
        yield cx, cz, heights, chosen[0]


def components(land: set[tuple[int, int]]):
    remaining = set(land)
    out = []
    while remaining:
        seed = remaining.pop()
        q = collections.deque([seed])
        comp = [seed]
        while q:
            x, z = q.popleft()
            for nb in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)):
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
    data = level_nbt.get("Data", level_nbt)
    summary = {
        "world_root": str(root),
        "level_name": data.get("LevelName"),
        "data_version": data.get("DataVersion"),
        "spawn": [data.get("SpawnX"), data.get("SpawnY"), data.get("SpawnZ")],
        "region_files": 0,
        "chunks_with_heightmaps": 0,
        "heightmap_kind_counts": {},
    }

    heights_by_pos: dict[tuple[int, int], int] = {}
    region_files = sorted(region_dir.glob("r.*.*.mca"))
    summary["region_files"] = len(region_files)
    kind_counts = collections.Counter()
    chunks = 0
    diag = {
        "compression_types": collections.Counter(),
        "skipped_compression_types": collections.Counter(),
        "parse_errors": collections.Counter(),
        "first_parse_errors": [],
        "parsed_chunks": 0,
        "chunks_without_heightmaps": 0,
    }
    for rp in region_files:
        for cx, cz, heights, kind in parse_region(rp, diag):
            chunks += 1
            kind_counts[kind] += 1
            bx, bz = cx * 16, cz * 16
            for i, y in enumerate(heights):
                x = bx + (i & 15)
                z = bz + (i >> 4)
                heights_by_pos[(x, z)] = y
    summary["chunks_with_heightmaps"] = chunks
    summary["heightmap_kind_counts"] = dict(kind_counts)
    summary["diagnostics"] = {
        k: (dict(v) if isinstance(v, collections.Counter) else v)
        for k, v in diag.items()
    }

    if not heights_by_pos:
        out = root.parent / "campfire-world-probe.json"
        out.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
        print(json.dumps(summary, ensure_ascii=False, indent=2))
        raise SystemExit("no usable heightmaps found")

    xs = [p[0] for p in heights_by_pos]
    zs = [p[1] for p in heights_by_pos]
    summary["scanned_bounds"] = [min(xs), min(zs), max(xs), max(zs)]

    land = {p for p, y in heights_by_pos.items() if y >= SEA_LEVEL}
    comps = components(land)
    comp_rows = []
    for comp in comps[:12]:
        cset = set(comp)
        cx = [p[0] for p in comp]
        cz = [p[1] for p in comp]
        hs = [heights_by_pos[p] for p in comp]
        med = statistics.median(hs)
        near = sum(1 for h in hs if abs(h - med) <= 3)
        comp_rows.append({
            "area_columns": len(comp),
            "approx_area_blocks": len(comp),
            "bbox": [min(cx), min(cz), max(cx), max(cz)],
            "bbox_size": [max(cx)-min(cx)+1, max(cz)-min(cz)+1],
            "height_median": med,
            "height_p10": percentile(hs, .10),
            "height_p90": percentile(hs, .90),
            "within_median_plus_minus_3": round(near / len(hs), 4),
        })
    summary["land_components"] = comp_rows

    # Tile-level buildability: 16x16 cells with >=90% land and p90-p10 <= 4.
    tiles = collections.defaultdict(list)
    for (x, z), y in heights_by_pos.items():
        if y >= SEA_LEVEL:
            tiles[(math.floor(x / 16), math.floor(z / 16))].append(y)
    good_tiles = set()
    tile_stats = {}
    for t, hs in tiles.items():
        if len(hs) < 230:
            continue
        p10, p90 = percentile(hs, .10), percentile(hs, .90)
        if p10 is not None and p90 is not None and p90 - p10 <= 4:
            good_tiles.add(t)
            tile_stats[t] = (len(hs), p10, p90)

    good_comps = components(good_tiles)
    summary["buildable_16x16_tiles"] = len(good_tiles)
    summary["largest_connected_buildable_tile_cluster"] = len(good_comps[0]) if good_comps else 0
    if good_comps:
        g = good_comps[0]
        gx = [p[0] for p in g]
        gz = [p[1] for p in g]
        summary["largest_buildable_cluster_bbox_blocks"] = [
            min(gx)*16, min(gz)*16, (max(gx)+1)*16-1, (max(gz)+1)*16-1
        ]
        summary["largest_buildable_cluster_bbox_size_blocks"] = [
            (max(gx)-min(gx)+1)*16, (max(gz)-min(gz)+1)*16
        ]

    # Search for a 10x10 tile (160x160) window with maximum count of good tiles.
    if tiles:
        txs = [t[0] for t in tiles]
        tzs = [t[1] for t in tiles]
        good = good_tiles
        best = (0, None)
        for tx in range(min(txs), max(txs)-9):
            for tz in range(min(tzs), max(tzs)-9):
                count = sum((x,z) in good for x in range(tx, tx+10) for z in range(tz, tz+10))
                if count > best[0]:
                    best = (count, (tx, tz))
        summary["best_160x160_window_good_tiles"] = best[0]
        summary["best_160x160_window_good_fraction"] = round(best[0]/100, 3)
        if best[1]:
            tx, tz = best[1]
            summary["best_160x160_window_bounds"] = [tx*16, tz*16, tx*16+159, tz*16+159]

    out = root.parent / "campfire-world-probe.json"
    out.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

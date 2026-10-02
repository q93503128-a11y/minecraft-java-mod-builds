#!/usr/bin/env python3
"""Inspect the pinned Chek's Mint Plains structure family before Campfire intake.

Downloads only the exact pinned MIT source commit already recorded by Campfire.
Reports the six additional residential candidates needed for the v5 housing
expansion. The broader village source contains legacy/special NBT payloads that
are irrelevant to this decision, so this probe deliberately stays narrow.
"""
from __future__ import annotations

import collections
import gzip
import io
import json
import pathlib
import struct
import urllib.parse
import urllib.request


REPOSITORY = "Chekmate90/Chek-s-Mint-Structures"
COMMIT = "6ffadba091c4a07bcdac9f1c89d3d1202f0015b9"
ROOT = f"https://raw.githubusercontent.com/{REPOSITORY}/{COMMIT}/"
PREFIX = "data/minecraft/structure/village/plains/houses/"
NAMES = [
    "plains_small_house_3",
    "plains_small_house_4",
    "plains_small_house_5",
    "plains_small_house_6",
    "plains_small_house_7",
    "plains_small_house_8",
]
USER_AGENT = "CampfireSessionsCheksProbe/1.0"


class NbtReader:
    def __init__(self, raw: bytes):
        if raw[:2] == b"\x1f\x8b":
            raw = gzip.decompress(raw)
        self.fp = io.BytesIO(raw)

    def read(self, fmt: str):
        n = struct.calcsize(">" + fmt)
        raw = self.fp.read(n)
        if len(raw) != n:
            raise EOFError("unexpected end of NBT")
        return struct.unpack(">" + fmt, raw)[0]

    def string(self) -> str:
        n = self.read("H")
        return self.fp.read(n).decode("utf-8")

    def payload(self, tag: int):
        if tag == 0: return None
        if tag == 1: return self.read("b")
        if tag == 2: return self.read("h")
        if tag == 3: return self.read("i")
        if tag == 4: return self.read("q")
        if tag == 5: return self.read("f")
        if tag == 6: return self.read("d")
        if tag == 7:
            n = self.read("i")
            return self.fp.read(n)
        if tag == 8: return self.string()
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
                out[self.string()] = self.payload(child)
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
            raise ValueError(f"expected root compound, got {tag}")
        self.string()
        return self.payload(tag)


def download(path: str) -> bytes:
    encoded = "/".join(urllib.parse.quote(part, safe="") for part in path.split("/"))
    request = urllib.request.Request(ROOT + encoded, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=90) as response:
        return response.read()


def inspect(name: str):
    path = PREFIX + name + ".nbt"
    raw = download(path)
    root = NbtReader(raw).root()
    size = root.get("size")
    palette = root.get("palette") or []
    blocks = root.get("blocks") or []

    palette_names = [
        entry.get("Name", "minecraft:air") if isinstance(entry, dict) else "minecraft:air"
        for entry in palette
    ]
    counts = collections.Counter()
    entrances = []
    foreign = collections.Counter()

    for block in blocks:
        if not isinstance(block, dict):
            continue
        state = block.get("state")
        if not isinstance(state, int) or not (0 <= state < len(palette_names)):
            continue
        block_name = palette_names[state]
        counts[block_name] += 1
        if not block_name.startswith("minecraft:"):
            foreign[block_name] += 1
        nbt = block.get("nbt")
        if block_name == "minecraft:jigsaw" and isinstance(nbt, dict):
            name_field = str(nbt.get("name", ""))
            target = str(nbt.get("target", ""))
            if "entrance" in name_field.lower() or "entrance" in target.lower():
                entrances.append(block.get("pos"))

    return {
        "name": name,
        "source": path,
        "bytes": len(raw),
        "size": size,
        "footprint": [size[0], size[2]] if isinstance(size, list) and len(size) == 3 else None,
        "blocks": len(blocks),
        "air": counts.get("minecraft:air", 0),
        "dirt_path": counts.get("minecraft:dirt_path", 0),
        "jigsaw": counts.get("minecraft:jigsaw", 0),
        "entrances": entrances,
        "foreign_palette": dict(foreign),
        "top_blocks": counts.most_common(12),
    }


def main() -> int:
    rows = [inspect(name) for name in NAMES]
    out_dir = pathlib.Path("build")
    out_dir.mkdir(parents=True, exist_ok=True)
    out = out_dir / "cheks-source-structures.json"
    out.write_text(json.dumps(rows, indent=2) + "\n", encoding="utf-8")

    print("CHEKS_PINNED_COMMIT=" + COMMIT)
    for row in rows:
        size = row["size"]
        print(
            f"{row['name']}: size={size} footprint={row['footprint']} "
            f"air={row['air']} path={row['dirt_path']} jigsaw={row['jigsaw']} "
            f"entrances={row['entrances']} foreign={row['foreign_palette']}"
        )
    print(f"CHEKS_STRUCTURES_INSPECTED={len(rows)}")
    print(f"REPORT={out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

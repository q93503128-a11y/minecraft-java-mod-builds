#!/usr/bin/env python3
"""Inspect packaged Minecraft structure NBT files and report exact structure bounds.

Uses only the Python standard library so CI can inspect third-party candidate JARs
without adding project/runtime dependencies.
"""
from __future__ import annotations

import collections
import gzip
import hashlib
import io
import pathlib
import struct
import sys
import zipfile


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
        length = self.read("H")
        return self.fp.read(length).decode("utf-8")

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
        _name = self.string()
        return self.payload(tag)


SELECTED_SUFFIXES = (
    "data/villageroles/structure/town_hall.nbt",
    "data/villageroles/structure/shop.nbt",
    "data/minecraft/structure/village/greece/village/house/small_1.nbt",
    "data/minecraft/structure/village/greece/village/house/medium_1.nbt",
    "data/minecraft/structure/village/greece/village/house/big_1.nbt",
    "data/minecraft/structure/village/greece/village/house/shop_small_1.nbt",
    "data/minecraft/structure/village/greece/village/house/shop_medium_1.nbt",
    "data/minecraft/structure/village/greece/village/house/shop_triple_1.nbt",
    "data/minecraft/structure/village/greece/village/house/triple_1.nbt",
    "data/minecraft/structure/village/greece/village/center/ratush_1.nbt",
    "data/villageroles/structure/hospital.nbt",
    "data/kaisyn/structure/village/exclusives/mediterranean/houses/regular/med_library_1.nbt",
    "data/kaisyn/structure/village/exclusives/mediterranean/houses/regular/med_leatherworker_1.nbt",
    "data/kaisyn/structure/village/exclusives/iberian/houses/iberian_temple_1.nbt",
    "data/kaisyn/structure/village/beach_lighthouse/side/beach_outdoor_shack_1.nbt",
    "data/kaisyn/structure/village/beach_lighthouse/side/beach_main_house_1.nbt",
    "data/kaisyn/structure/village/exclusives/mediterranean/houses/regular/med_small_house_1.nbt",
    "data/kaisyn/structure/village/exclusives/mediterranean/houses/regular/med_medium_house_2.nbt",
    "data/kaisyn/structure/village/exclusives/mediterranean/houses/regular/med_large_house_1.nbt",
    "data/kaisyn/structure/village/exclusives/iberian/houses/iberian_medium_house_4.nbt",
    "data/kaisyn/structure/village/exclusives/iberian/houses/iberian_large_house_2.nbt",
)

LICENSE_NAMES = (
    "license", "license.txt", "license.md", "copying", "copying.txt",
    "meta-inf/license", "meta-inf/license.txt", "meta-inf/license.md",
)


def selected_structure_report(zf: zipfile.ZipFile, name: str):
    root = NbtReader(zf.read(name)).root()
    palette = root.get("palette")
    blocks = root.get("blocks")
    counts = collections.Counter()
    if isinstance(palette, list) and isinstance(blocks, list):
        names = []
        for entry in palette:
            if isinstance(entry, dict):
                names.append(entry.get("Name", "minecraft:air"))
            else:
                names.append("minecraft:air")
        for block in blocks:
            if not isinstance(block, dict):
                continue
            state = block.get("state")
            if isinstance(state, int) and 0 <= state < len(names):
                counts[names[state]] += 1
    raw = zf.read(name)
    top = counts.most_common(16)
    return {
        "sha256": hashlib.sha256(raw).hexdigest(),
        "blocks": sum(counts.values()),
        "top": top,
    }


def inspect_license_entries(zf: zipfile.ZipFile):
    out = []
    for name in zf.namelist():
        low = name.lower().strip("/")
        base = low.rsplit("/", 1)[-1]
        if base in LICENSE_NAMES or base.startswith("license") or base.startswith("copying"):
            try:
                text = zf.read(name).decode("utf-8", errors="replace").strip()
            except Exception:
                continue
            if text:
                out.append((name, text[:3000].replace("\r", "")))
    return out


def inspect_jar(path: pathlib.Path):
    rows = []
    errors = []
    selected = {}
    licenses = []
    with zipfile.ZipFile(path) as zf:
        for name in zf.namelist():
            low = name.lower()
            if not low.endswith(".nbt"):
                continue
            if "/structure/" not in low and "/structures/" not in low:
                continue
            try:
                root = NbtReader(zf.read(name)).root()
                size = root.get("size")
                if isinstance(size, list) and len(size) == 3 and all(isinstance(v, int) for v in size):
                    x, y, z = size
                    rows.append((name, x, y, z))
                else:
                    errors.append((name, f"missing/invalid size: {size!r}"))
            except Exception as exc:
                errors.append((name, f"{type(exc).__name__}: {exc}"))

        for suffix in SELECTED_SUFFIXES:
            matches = [n for n in zf.namelist() if n.lower().endswith(suffix.lower())]
            for name in matches:
                try:
                    selected[name] = selected_structure_report(zf, name)
                except Exception as exc:
                    selected[name] = {"error": f"{type(exc).__name__}: {exc}"}

        licenses = inspect_license_entries(zf)

    return rows, errors, selected, licenses


def main() -> int:
    root = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else "build/externalStructureProbe")
    jars = sorted(root.glob("*.jar"))
    if not jars:
        print(f"NO JARS FOUND: {root}")
        return 0

    total = 0
    keywords = ("town", "hall", "shop", "hospital", "house", "clinic", "market", "store", "center", "centre")
    for jar in jars:
        rows, errors, selected, licenses = inspect_jar(jar)
        total += len(rows)
        print(f"\n## {jar.name}")
        print(f"structures with explicit size: {len(rows)}")
        if rows:
            priority = sorted(rows, key=lambda r: (not any(k in r[0].lower() for k in keywords), r[0]))
            for name, x, y, z in priority:
                flag = " *" if any(k in name.lower() for k in keywords) else ""
                print(f"{x:>3} x {y:>3} x {z:>3}  footprint={x:>3}x{z:<3}  {name}{flag}")
        if selected:
            print("\nselected candidate palettes:")
            for name, info in sorted(selected.items()):
                print(f"  {name}")
                if "error" in info:
                    print(f"    ERROR {info['error']}")
                    continue
                print(f"    sha256={info['sha256']} blocks={info['blocks']}")
                print("    top=" + ", ".join(f"{block}:{count}" for block, count in info["top"]))

        if licenses:
            print("\nlicense-like JAR entries:")
            for name, body in licenses[:8]:
                compact = " ".join(body.split())
                print(f"  {name}: {compact[:1200]}")

        if errors:
            print(f"unparsed structure entries: {len(errors)}")
            for name, err in errors[:10]:
                print(f"  WARN {name}: {err}")

    print(f"\nTOTAL STRUCTURES WITH SIZE: {total}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

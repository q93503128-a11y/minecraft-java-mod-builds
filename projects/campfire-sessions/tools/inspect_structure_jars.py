#!/usr/bin/env python3
"""Inspect packaged Minecraft structure NBT files and report exact structure bounds.

Uses only the Python standard library so CI can inspect third-party candidate JARs
without adding project/runtime dependencies.
"""
from __future__ import annotations

import gzip
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


def inspect_jar(path: pathlib.Path):
    rows = []
    errors = []
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
    return rows, errors


def main() -> int:
    root = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else "build/externalStructureProbe")
    jars = sorted(root.glob("*.jar"))
    if not jars:
        print(f"NO JARS FOUND: {root}")
        return 0

    total = 0
    keywords = ("town", "hall", "shop", "hospital", "house", "clinic", "market", "store", "center", "centre")
    for jar in jars:
        rows, errors = inspect_jar(jar)
        total += len(rows)
        print(f"\n## {jar.name}")
        print(f"structures with explicit size: {len(rows)}")
        if rows:
            priority = sorted(rows, key=lambda r: (not any(k in r[0].lower() for k in keywords), r[0]))
            for name, x, y, z in priority:
                flag = " *" if any(k in name.lower() for k in keywords) else ""
                print(f"{x:>3} x {y:>3} x {z:>3}  footprint={x:>3}x{z:<3}  {name}{flag}")
        if errors:
            print(f"unparsed structure entries: {len(errors)}")
            for name, err in errors[:10]:
                print(f"  WARN {name}: {err}")

    print(f"\nTOTAL STRUCTURES WITH SIZE: {total}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

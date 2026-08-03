#!/usr/bin/env python3
from __future__ import annotations

import math
import struct
import sys
import zlib
from pathlib import Path


def chunk(kind: bytes, data: bytes) -> bytes:
    return struct.pack(">I", len(data)) + kind + data + struct.pack(
        ">I", zlib.crc32(kind + data) & 0xFFFFFFFF
    )


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("usage: generate_skill_mesh_texture.py <resources-root>")
    root = Path(sys.argv[1])
    target = root / "assets/villageguardians/textures/effect/skill_mesh.png"
    target.parent.mkdir(parents=True, exist_ok=True)

    size = 64
    rows: list[bytes] = []
    for y in range(size):
        row = bytearray([0])
        for x in range(size):
            nx = (x + 0.5) / size * 2.0 - 1.0
            ny = (y + 0.5) / size * 2.0 - 1.0
            radius = math.sqrt(nx * nx + ny * ny)
            edge = max(0.0, min(1.0, (1.0 - radius) * 4.0))
            core = max(0.0, min(1.0, 1.25 - radius * 1.35))
            streak = max(0.0, 1.0 - abs(ny) * 3.2) * max(0.0, 1.0 - abs(nx) * 0.65)
            alpha = int(max(edge * 0.88, core * 0.46, streak * 0.72) * 255)
            value = int((0.74 + 0.26 * core) * 255)
            row.extend((value, value, value, alpha))
        rows.append(bytes(row))

    raw = b"".join(rows)
    png = (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw, 9))
        + chunk(b"IEND", b"")
    )
    target.write_bytes(png)
    print(f"Generated original procedural mesh texture: {target} ({len(png)} bytes)")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
from pathlib import Path
import runpy
import struct
import sys
import zlib


def chunk(kind: bytes, data: bytes) -> bytes:
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)


def main() -> None:
    root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("src/main/resources")
    target = root / "assets/villageguardians/textures/effect/skill_mesh.png"
    target.parent.mkdir(parents=True, exist_ok=True)
    width = 4
    height = 4
    # Fully opaque white material. Geometry and vertex alpha now define the shape;
    # repeating radial texture stamps can no longer create oval or arrowhead tiles.
    raw = b"".join(b"\x00" + bytes([255, 255, 255, 255]) * width for _ in range(height))
    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")
    target.write_bytes(png)
    print(f"Generated solid procedural mesh material: {target} ({len(png)} bytes)")

    compatibility = Path(__file__).resolve().parent / "v01715/post_apply.py"
    if compatibility.is_file():
        runpy.run_path(str(compatibility), run_name="__main__")


if __name__ == "__main__":
    main()

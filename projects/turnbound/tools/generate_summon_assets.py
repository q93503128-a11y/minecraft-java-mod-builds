#!/usr/bin/env python3
"""Copy authored summon artwork into Minecraft resources and derive portrait mattes.

The source PNGs live under assets/summon/source and are the visual masters. The only
generated images are monochrome silhouettes that reuse each portrait's authored alpha.
"""
from __future__ import annotations

import argparse
import shutil
import struct
import zlib
from pathlib import Path

ASSETS = {
    "archive-folio.png": "bg_archive.png",
    "wash-violet.png": "wash_violet.png",
    "wash-gold.png": "wash_gold.png",
    "card-common.png": "card_common.png",
    "card-rare.png": "card_rare.png",
    "card-legendary.png": "card_legendary.png",
}
PORTRAITS = ("p01", "p02", "p03", "p04", "p05", "p06", "p07", "p08",
             "f01", "f02", "f03", "f04")
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


def paeth(a: int, b: int, c: int) -> int:
    estimate = a + b - c
    da, db, dc = abs(estimate - a), abs(estimate - b), abs(estimate - c)
    return a if da <= db and da <= dc else b if db <= dc else c


def png_chunk(kind: bytes, payload: bytes) -> bytes:
    return (struct.pack(">I", len(payload)) + kind + payload
            + struct.pack(">I", zlib.crc32(kind + payload) & 0xFFFFFFFF))


def read_rgba(path: Path) -> tuple[int, int, list[bytearray]]:
    data = path.read_bytes()
    if data[:8] != PNG_SIGNATURE:
        raise ValueError(f"not a PNG: {path}")

    pos = 8
    width = height = color_type = bit_depth = None
    compressed = bytearray()
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        kind = data[pos + 4:pos + 8]
        payload = data[pos + 8:pos + 8 + length]
        pos += length + 12
        if kind == b"IHDR":
            width, height, bit_depth, color_type, compression, filtering, interlace = struct.unpack(
                ">IIBBBBB", payload)
            if bit_depth != 8 or interlace != 0 or compression != 0 or filtering != 0:
                raise ValueError(f"unsupported PNG encoding: {path}")
        elif kind == b"IDAT":
            compressed.extend(payload)
        elif kind == b"IEND":
            break

    if color_type not in (2, 6):
        raise ValueError(f"unsupported PNG color type {color_type}: {path}")
    channels = 4 if color_type == 6 else 3
    stride = width * channels
    raw = zlib.decompress(bytes(compressed))
    previous = bytearray(stride)
    rows: list[bytearray] = []
    offset = 0
    for _ in range(height):
        filter_type = raw[offset]
        offset += 1
        row = bytearray(raw[offset:offset + stride])
        offset += stride
        for i in range(stride):
            left = row[i - channels] if i >= channels else 0
            up = previous[i]
            upper_left = previous[i - channels] if i >= channels else 0
            if filter_type == 1:
                row[i] = (row[i] + left) & 255
            elif filter_type == 2:
                row[i] = (row[i] + up) & 255
            elif filter_type == 3:
                row[i] = (row[i] + ((left + up) >> 1)) & 255
            elif filter_type == 4:
                row[i] = (row[i] + paeth(left, up, upper_left)) & 255
            elif filter_type != 0:
                raise ValueError(f"unsupported PNG filter {filter_type}: {path}")
        decoded = row
        if channels == 3:
            rgba = bytearray(width * 4)
            for x in range(width):
                rgba[x * 4:x * 4 + 3] = decoded[x * 3:x * 3 + 3]
                rgba[x * 4 + 3] = 255
            row = rgba
        rows.append(row)
        previous = bytearray(decoded)
    return width, height, rows


def write_rgba(path: Path, width: int, height: int, rows: list[bytearray]) -> None:
    raw = bytearray()
    for row in rows:
        raw.append(0)
        raw.extend(row)
    png = PNG_SIGNATURE
    png += png_chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += png_chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += png_chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def write_silhouette(source: Path, destination: Path) -> None:
    width, height, rows = read_rgba(source)
    has_visible_pixel = False
    silhouette: list[bytearray] = []
    for row in rows:
        out = bytearray(width * 4)
        for x in range(width):
            alpha = row[x * 4 + 3]
            has_visible_pixel |= alpha > 0
            out[x * 4:x * 4 + 4] = bytes((25, 20, 29, alpha))
        silhouette.append(out)
    if not has_visible_pixel:
        raise ValueError(f"portrait has no visible pixels: {source}")
    write_rgba(destination, width, height, silhouette)


def generate(root: Path, output: Path) -> None:
    source = root / "assets/summon/source"
    target = output / "assets/turnbound/textures/gui/summon"
    target.mkdir(parents=True, exist_ok=True)

    for source_name, target_name in ASSETS.items():
        source_path = source / source_name
        if not source_path.is_file():
            raise FileNotFoundError(f"missing authored summon asset: {source_path}")
        read_rgba(source_path)
        shutil.copy2(source_path, target / target_name)

    portraits = source / "portraits"
    destination = target / "portraits"
    for character_id in PORTRAITS:
        source_path = portraits / f"{character_id}.png"
        if not source_path.is_file():
            raise FileNotFoundError(f"missing authored portrait: {source_path}")
        width, height, rows = read_rgba(source_path)
        if not any(rows[y][x * 4 + 3] < 255 for y in range(height) for x in range(width)):
            raise ValueError(f"portrait must include transparent background pixels: {source_path}")
        target_path = destination / f"{character_id}.png"
        target_path.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source_path, target_path)
        write_silhouette(source_path, destination / f"{character_id}_silhouette.png")

    print(f"installed authored TURNBOUND summon assets in {target}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", required=True)
    parser.add_argument("--output", required=True)
    arguments = parser.parse_args()
    generate(Path(arguments.root).resolve(), Path(arguments.output).resolve())


if __name__ == "__main__":
    main()

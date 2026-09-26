#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import io
import json
import struct
import sys
import urllib.request
import zipfile
import zlib
from pathlib import Path

GUITAR_URL = "https://opengameart.org/sites/default/files/voxelclassicalguitar.zip"
CHAIR_URL = "https://opengameart.org/sites/default/files/chair_cc0.obj"
MUSIC_URL = "https://lpc.opengameart.org/sites/default/files/Etirwer%20%28Looped%29_0.ogg"
UI_URL = "https://opengameart.org/sites/default/files/kenney_ui-pack-adventure.zip"
USER_AGENT = "CampfireSessions/0.1 (+https://github.com/q93503128-a11y/minecraft-java-mod-builds)"


def download(url: str) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=45) as response:
        data = response.read()
    if not data:
        raise RuntimeError(f"Downloaded empty asset: {url}")
    print(f"[licensed-assets] {url} -> {len(data)} bytes sha256={hashlib.sha256(data).hexdigest()}")
    return data


def write_bytes(root: Path, relative: str, data: bytes) -> None:
    path = root / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def write_text(root: Path, relative: str, text: str) -> None:
    path = root / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def png_rgba(width: int, height: int, pixels: list[tuple[int, int, int, int]]) -> bytes:
    if len(pixels) != width * height:
        raise ValueError("pixel count does not match dimensions")

    def chunk(kind: bytes, data: bytes) -> bytes:
        payload = kind + data
        return struct.pack(">I", len(data)) + payload + struct.pack(">I", zlib.crc32(payload) & 0xFFFFFFFF)

    rows = []
    for y in range(height):
        row = pixels[y * width:(y + 1) * width]
        rows.append(b"\x00" + b"".join(bytes((r, g, b, a)) for r, g, b, a in row))
    raw = b"".join(rows)
    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw, 9))
        + chunk(b"IEND", b"")
    )


def parse_vox(data: bytes):
    if len(data) < 8 or data[:4] != b"VOX ":
        raise RuntimeError("Voxel guitar archive does not contain a valid MagicaVoxel VOX file")

    pos = 8
    voxels = []
    palette = None

    while pos + 12 <= len(data):
        chunk_id = data[pos:pos + 4]
        content_size, _children_size = struct.unpack_from("<II", data, pos + 4)
        content_start = pos + 12
        content_end = content_start + content_size
        if content_end > len(data):
            raise RuntimeError("Truncated VOX chunk")

        content = data[content_start:content_end]
        if chunk_id == b"XYZI":
            if len(content) < 4:
                raise RuntimeError("Malformed XYZI chunk")
            count = struct.unpack_from("<I", content, 0)[0]
            expected = 4 + count * 4
            if len(content) < expected:
                raise RuntimeError("Malformed XYZI voxel data")
            for i in range(count):
                x, y, z, color = struct.unpack_from("<BBBB", content, 4 + i * 4)
                voxels.append((x, y, z, color))
        elif chunk_id == b"RGBA" and len(content) >= 1024:
            palette = [(0, 0, 0, 0)]
            for i in range(256):
                palette.append(struct.unpack_from("<BBBB", content, i * 4))
        pos = content_end

    if not voxels:
        raise RuntimeError("No voxels found in guitar model")
    if palette is None:
        palette = [(0, 0, 0, 0)] + [(i, i, i, 255) for i in range(1, 256)] + [(255, 255, 255, 255)]
    return voxels, palette


def guitar_to_obj(voxels, palette):
    occupied = {(x, y, z) for x, y, z, _ in voxels}
    xs = [v[0] for v in voxels]
    ys = [v[1] for v in voxels]
    zs = [v[2] for v in voxels]
    min_x, min_y, min_z = min(xs), min(ys), min(zs)
    max_x, max_y, max_z = max(xs) + 1, max(ys) + 1, max(zs) + 1
    width, depth, height = max_x - min_x, max_y - min_y, max_z - min_z
    scale = 1.0 / max(width, depth, height)
    cx = (min_x + max_x) / 2.0
    cz = (min_y + max_y) / 2.0

    faces = [
        ((1, 0, 0), ((1,0,0),(1,1,0),(1,1,1),(1,0,1))),
        ((-1, 0, 0), ((0,0,0),(0,0,1),(0,1,1),(0,1,0))),
        ((0, 1, 0), ((0,1,0),(0,1,1),(1,1,1),(1,1,0))),
        ((0, -1, 0), ((0,0,0),(1,0,0),(1,0,1),(0,0,1))),
        ((0, 0, 1), ((0,0,1),(1,0,1),(1,1,1),(0,1,1))),
        ((0, 0, -1), ((0,0,0),(0,1,0),(1,1,0),(1,0,0))),
    ]

    lines = [
        "# Derived from MonoTone's CC0 Voxel Classical Guitar",
        "mtllib acoustic_guitar.mtl",
        "o AcousticGuitar",
    ]
    for i in range(256):
        lines.append(f"vt {(i + 0.5) / 256.0:.8f} 0.50000000")
    lines.append("usemtl guitar_palette")

    vertex_index = 1
    for x, y, z, color in voxels:
        uv = max(1, min(256, int(color)))
        for (dx, dy, dz), corners in faces:
            if (x + dx, y + dy, z + dz) in occupied:
                continue
            indices = []
            for ox, oy, oz in corners:
                px = ((x + ox) - cx) * scale
                py = ((z + oz) - min_z) * scale
                pz = ((y + oy) - cz) * scale
                lines.append(f"v {px:.6f} {py:.6f} {pz:.6f}")
                indices.append(vertex_index)
                vertex_index += 1
            a, b, c, d = indices
            lines.append(f"f {a}/{uv} {b}/{uv} {c}/{uv}")
            lines.append(f"f {a}/{uv} {c}/{uv} {d}/{uv}")

    mtl = """# CC0 guitar palette mapped through a generated texture
newmtl guitar_palette
Ka 1.000000 1.000000 1.000000
Kd 1.000000 1.000000 1.000000
Ks 0.000000 0.000000 0.000000
d 1.000000
illum 1
map_Kd #palette
"""
    palette_pixels = []
    for i in range(1, 257):
        rgba = palette[i] if i < len(palette) else palette[-1]
        palette_pixels.append(tuple(int(v) for v in rgba))
    palette_pixels = palette_pixels + palette_pixels
    return "\n".join(lines) + "\n", mtl, png_rgba(256, 2, palette_pixels)


def rewrite_face_token(token: str) -> str:
    parts = token.split("/")
    vertex = parts[0]
    normal = parts[2] if len(parts) >= 3 and parts[2] else None
    return f"{vertex}/1/{normal}" if normal else f"{vertex}/1"


def normalize_chair_obj(text: str):
    lines = text.replace("\r", "").split("\n")
    vertices = []
    for line in lines:
        if line.startswith("v "):
            parts = line.split()
            if len(parts) >= 4:
                vertices.append(tuple(float(value) for value in parts[1:4]))
    if not vertices:
        raise RuntimeError("Chair OBJ contained no vertices")

    xs = [v[0] for v in vertices]
    ys = [v[1] for v in vertices]
    zs = [v[2] for v in vertices]
    min_x, min_y, min_z = min(xs), min(ys), min(zs)
    max_x, max_y, max_z = max(xs), max(ys), max(zs)
    extent = max(max_x - min_x, max_y - min_y, max_z - min_z)
    if extent <= 0:
        raise RuntimeError("Chair OBJ has zero-sized bounds")
    scale = 0.96 / extent
    cx = (min_x + max_x) / 2.0
    cz = (min_z + max_z) / 2.0

    output = ["# Geometry from Lyricsz '3D Lowpoly Chair', CC0", "mtllib wooden_chair.mtl", "o WoodenChair"]
    face_lines = []
    for line in lines:
        if not line or line.startswith(("mtllib ", "usemtl ", "vt ", "o ")):
            continue
        if line.startswith("v "):
            parts = line.split()
            x, y, z = (float(value) for value in parts[1:4])
            output.append(f"v {(x-cx)*scale:.6f} {(y-min_y)*scale:.6f} {(z-cz)*scale:.6f}")
        elif line.startswith("f "):
            face_lines.append(line)
        else:
            output.append(line)

    output.append("vt 0.500000 0.500000")
    output.append("usemtl chair")
    for line in face_lines:
        tokens = [rewrite_face_token(token) for token in line.split()[1:]]
        if len(tokens) < 3:
            continue
        for i in range(1, len(tokens) - 1):
            output.append("f " + " ".join((tokens[0], tokens[i], tokens[i + 1])))

    mtl = """# Warm wood material over the original CC0 chair geometry
newmtl chair
Ka 1.000000 1.000000 1.000000
Kd 1.000000 1.000000 1.000000
Ks 0.000000 0.000000 0.000000
d 1.000000
illum 1
map_Kd #wood
"""
    wood = [(142, 84, 42, 255)] * 4
    return "\n".join(output) + "\n", mtl, png_rgba(2, 2, wood)


def zip_asset(archive: zipfile.ZipFile, basename: str) -> bytes:
    wanted = basename.lower()
    candidates = [name for name in archive.namelist() if Path(name).name.lower() == wanted]
    if not candidates:
        raise RuntimeError(f"Missing {basename} in Kenney UI archive")
    candidates.sort(key=lambda name: ("png/default" not in name.lower().replace("\\", "/"), len(name)))
    return archive.read(candidates[0])


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("usage: prepare_licensed_assets.py <output-dir>")
    out = Path(sys.argv[1]).resolve()
    out.mkdir(parents=True, exist_ok=True)

    guitar_zip = download(GUITAR_URL)
    with zipfile.ZipFile(io.BytesIO(guitar_zip)) as archive:
        vox_names = [name for name in archive.namelist() if name.lower().endswith(".vox")]
        if not vox_names:
            raise RuntimeError("No .vox file found in voxelclassicalguitar.zip")
        voxels, palette = parse_vox(archive.read(vox_names[0]))
    guitar_obj, guitar_mtl, guitar_texture = guitar_to_obj(voxels, palette)
    write_text(out, "assets/campfiresessions/models/item/acoustic_guitar.obj", guitar_obj)
    write_text(out, "assets/campfiresessions/models/item/acoustic_guitar.mtl", guitar_mtl)
    write_bytes(out, "assets/campfiresessions/textures/item/acoustic_guitar_palette.png", guitar_texture)

    chair_bytes = download(CHAIR_URL)
    chair_obj, chair_mtl, chair_texture = normalize_chair_obj(chair_bytes.decode("utf-8", errors="replace"))
    write_text(out, "assets/campfiresessions/models/block/wooden_chair.obj", chair_obj)
    write_text(out, "assets/campfiresessions/models/block/wooden_chair.mtl", chair_mtl)
    write_bytes(out, "assets/campfiresessions/textures/block/wooden_chair.png", chair_texture)

    music = download(MUSIC_URL)
    if not music.startswith(b"OggS"):
        raise RuntimeError("Etirwer download is not an OGG/Vorbis stream")
    write_bytes(out, "assets/campfiresessions/sounds/music/etirwer.ogg", music)

    ui_zip = download(UI_URL)
    with zipfile.ZipFile(io.BytesIO(ui_zip)) as archive:
        for name in ("button_brown.png", "button_grey.png", "button_red.png"):
            png = zip_asset(archive, name)
            write_bytes(out, f"assets/campfiresessions/textures/gui/sprites/music/{name}", png)
            metadata = {
                "gui": {
                    "scaling": {
                        "type": "nine_slice",
                        "width": 48,
                        "height": 24,
                        "border": {"left": 7, "right": 7, "top": 7, "bottom": 7},
                        "stretch_inner": True,
                    }
                }
            }
            write_text(out, f"assets/campfiresessions/textures/gui/sprites/music/{name}.mcmeta", json.dumps(metadata, indent=2) + "\n")

    manifest = {
        "guitar": {"source": GUITAR_URL, "sha256": hashlib.sha256(guitar_zip).hexdigest()},
        "chair": {"source": CHAIR_URL, "sha256": hashlib.sha256(chair_bytes).hexdigest()},
        "music": {"source": MUSIC_URL, "sha256": hashlib.sha256(music).hexdigest()},
        "ui": {"source": UI_URL, "sha256": hashlib.sha256(ui_zip).hexdigest()},
    }
    write_text(out, "campfiresessions_asset_manifest.json", json.dumps(manifest, indent=2) + "\n")


if __name__ == "__main__":
    main()

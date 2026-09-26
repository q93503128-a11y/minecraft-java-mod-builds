#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import io
import json
import struct
import sys
import urllib.request
import zipfile
from pathlib import Path

GUITAR_URL = "https://opengameart.org/sites/default/files/voxelclassicalguitar.zip"
CHAIR_URL = "https://opengameart.org/sites/default/files/chair_cc0.obj"
MUSIC_URL = "https://lpc.opengameart.org/sites/default/files/Etirwer%20%28Looped%29_0.ogg"
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

    lines = ["# Derived from MonoTone's CC0 Voxel Classical Guitar", "mtllib acoustic_guitar.mtl", "o AcousticGuitar"]
    vertex_index = 1
    used_colors = set()

    for x, y, z, color in voxels:
        used_colors.add(color)
        for (dx, dy, dz), corners in faces:
            if (x + dx, y + dy, z + dz) in occupied:
                continue
            lines.append(f"usemtl c{color}")
            indices = []
            for ox, oy, oz in corners:
                px = ((x + ox) - cx) * scale
                py = ((z + oz) - min_z) * scale
                pz = ((y + oy) - cz) * scale
                lines.append(f"v {px:.6f} {py:.6f} {pz:.6f}")
                indices.append(vertex_index)
                vertex_index += 1
            lines.append("f " + " ".join(str(i) for i in indices))

    mtl = ["# CC0 source palette carried into OBJ materials"]
    for color in sorted(used_colors):
        r, g, b, a = palette[color if color < len(palette) else 0]
        mtl.extend([
            f"newmtl c{color}",
            f"Ka {r/255:.6f} {g/255:.6f} {b/255:.6f}",
            f"Kd {r/255:.6f} {g/255:.6f} {b/255:.6f}",
            "Ks 0.000000 0.000000 0.000000",
            f"d {a/255:.6f}",
            ""
        ])
    return "\n".join(lines) + "\n", "\n".join(mtl) + "\n"


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
    material_written = False
    for line in lines:
        if not line or line.startswith("mtllib ") or line.startswith("usemtl "):
            continue
        if line.startswith("v "):
            parts = line.split()
            x, y, z = (float(value) for value in parts[1:4])
            output.append(f"v {(x-cx)*scale:.6f} {(y-min_y)*scale:.6f} {(z-cz)*scale:.6f}")
        elif line.startswith("f ") and not material_written:
            output.append("usemtl chair")
            output.append(line)
            material_written = True
        else:
            output.append(line)

    mtl = """# Warm wood material applied to the CC0 chair geometry
newmtl chair
Ka 0.260000 0.150000 0.075000
Kd 0.560000 0.330000 0.160000
Ks 0.050000 0.040000 0.030000
d 1.000000
"""
    return "\n".join(output) + "\n", mtl


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
    guitar_obj, guitar_mtl = guitar_to_obj(voxels, palette)
    write_text(out, "assets/campfiresessions/models/item/acoustic_guitar.obj", guitar_obj)
    write_text(out, "assets/campfiresessions/models/item/acoustic_guitar.mtl", guitar_mtl)

    chair_bytes = download(CHAIR_URL)
    chair_obj, chair_mtl = normalize_chair_obj(chair_bytes.decode("utf-8", errors="replace"))
    write_text(out, "assets/campfiresessions/models/block/wooden_chair.obj", chair_obj)
    write_text(out, "assets/campfiresessions/models/block/wooden_chair.mtl", chair_mtl)

    music = download(MUSIC_URL)
    if not music.startswith(b"OggS"):
        raise RuntimeError("Etirwer download is not an OGG/Vorbis stream")
    write_bytes(out, "assets/campfiresessions/sounds/music/etirwer.ogg", music)

    manifest = {
        "guitar": {"source": GUITAR_URL, "sha256": hashlib.sha256(guitar_zip).hexdigest()},
        "chair": {"source": CHAIR_URL, "sha256": hashlib.sha256(chair_bytes).hexdigest()},
        "music": {"source": MUSIC_URL, "sha256": hashlib.sha256(music).hexdigest()}
    }
    write_text(out, "campfiresessions_asset_manifest.json", json.dumps(manifest, indent=2) + "\n")


if __name__ == "__main__":
    main()

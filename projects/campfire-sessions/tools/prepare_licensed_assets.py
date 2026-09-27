#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import struct
import sys
import urllib.parse
import urllib.request
from pathlib import Path

MUSIC = {
    "etirwer": {"bpm": 96, "url": "https://lpc.opengameart.org/sites/default/files/Etirwer%20%28Looped%29_0.ogg"},
    "cozy_puzzle": {"bpm": 108, "url": "https://opengameart.org/sites/default/files/cozy_puzzle_in-game_3_bpm108_0.ogg"},
    "neon_circuit": {"bpm": 145, "url": "https://opengameart.org/sites/default/files/neon_sign_circuit_bpm145_0.ogg"},
    "underwater_pad": {"bpm": 70, "url": "https://opengameart.org/sites/default/files/Underwater-Ambient-Pad-isaiah658_0.ogg"},
    "cozy_puzzle_1": {"bpm": 118, "url": "https://opengameart.org/sites/default/files/cozy_puzzle_in-game_1_bpm118_0.ogg"},
    "cozy_title": {"bpm": 95, "url": "https://opengameart.org/sites/default/files/cozy_puzzle_title_bpm95_0.ogg"},
    "beach_stage": {"bpm": 128, "url": "https://opengameart.org/sites/default/files/beach_stage_bpm128_0.ogg"},
    "space_battle": {"bpm": 130, "url": "https://opengameart.org/sites/default/files/space_battle_bpm130_0.ogg"},
    "jazzy_battle": {"bpm": 130, "url": "https://opengameart.org/sites/default/files/jazzy_battle_theme_bpm130_0.ogg"},
    "desert_pink": {"bpm": 90, "url": "https://opengameart.org/sites/default/files/desert_pink_and_navy_blue_0.ogg"},
    "nighttime_solitude": {"bpm": 110, "url": "https://opengameart.org/sites/default/files/Nighttime%20Solitude%20%5BCC0%5D.ogg"},
    "fairy_adventure": {"bpm": 140, "url": "https://opengameart.org/sites/default/files/fairy_adventure_bpm140_0.ogg"},
    "other_center": {"bpm": 134, "url": "https://opengameart.org/sites/default/files/othercenter.ogg"},
    "magic_puzzle_1": {"bpm": 110, "url": "https://opengameart.org/sites/default/files/magic_puzzle_in-game_1_bpm110_0.ogg"},
    "urban_boss_battle": {"bpm": 135, "url": "https://opengameart.org/sites/default/files/urban_boss_battle_bpm135_0.ogg"},
}

KENNEY_COMMIT = "3694c6879e487c108f55677be7dd2ca75b07cc3b"
KENNEY_RAW = f"https://raw.githubusercontent.com/shorepine/kenney/{KENNEY_COMMIT}/"

UI_ASSETS = {
    "clean_panel": "ui/UI Pack - Adventure/panel_grey.png",
    "clean_card": "ui/UI Pack/Grey/button_rectangle_depth_flat.png",
    "clean_button": "ui/UI Pack/Grey/button_rectangle_depth_gloss.png",
    "neon_panel": "ui/UI Pack - Sci-fi/glassPanel.png",
    "neon_card": "ui/UI Pack - Sci-fi/metalPanel_blue.png",
    "neon_button": "ui/UI Pack - Sci-fi/Blue/button_square_header_blade_rectangle.png",
    "ocean_panel": "ui/UI Adventure Pack/panel_blue.png",
    "ocean_card": "ui/UI Adventure Pack/panelInset_blue.png",
    "ocean_button": "ui/UI Adventure Pack/buttonLong_blue.png",
    "desert_panel": "ui/UI Adventure Pack/panel_beige.png",
    "desert_card": "ui/UI Adventure Pack/panelInset_beigeLight.png",
    "desert_button": "ui/UI Adventure Pack/buttonLong_beige.png",
    "rough_panel": "ui/UI Pack - Adventure/panel_brown_damaged_dark.png",
    "rough_card": "ui/UI Pack - Adventure/panel_brown_damaged.png",
    "rough_button": "ui/UI Pack - Adventure/button_brown.png",
}

USER_AGENT = "CampfireSessions/0.6 (+https://github.com/q93503128-a11y/minecraft-java-mod-builds)"


def download(url: str) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=90) as response:
        data = response.read()
    if not data:
        raise RuntimeError(f"Downloaded empty asset: {url}")
    digest = hashlib.sha256(data).hexdigest()
    print(f"[licensed-assets] {url} -> {len(data)} bytes sha256={digest}")
    return data


def write_bytes(root: Path, relative: str, data: bytes) -> None:
    path = root / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def write_text(root: Path, relative: str, text: str) -> None:
    path = root / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def png_size(data: bytes) -> tuple[int, int]:
    if len(data) < 24 or data[:8] != b"\x89PNG\r\n\x1a\n":
        raise RuntimeError("Expected a PNG asset")
    return struct.unpack(">II", data[16:24])


def write_ui_sprite(out: Path, key: str, data: bytes) -> None:
    width, height = png_size(data)
    write_bytes(out, f"assets/campfiresessions/textures/gui/sprites/music/theme/{key}.png", data)
    max_border = max(1, (min(width, height) - 1) // 2)
    border = min(8, max_border)
    metadata = {
        "gui": {
            "scaling": {
                "type": "nine_slice",
                "width": width,
                "height": height,
                "border": {"left": border, "right": border, "top": border, "bottom": border},
                "stretch_inner": True,
            }
        }
    }
    write_text(
        out,
        f"assets/campfiresessions/textures/gui/sprites/music/theme/{key}.png.mcmeta",
        json.dumps(metadata, indent=2) + "\n",
    )


def ogg_duration_seconds(data: bytes) -> int:
    marker = data.find(b"\x01vorbis")
    if marker < 0 or marker + 16 > len(data):
        raise RuntimeError("Vorbis identification header not found")
    sample_rate = struct.unpack_from("<I", data, marker + 12)[0]
    if sample_rate <= 0:
        raise RuntimeError("Invalid Vorbis sample rate")

    offset = 0
    max_granule = 0
    while True:
        page = data.find(b"OggS", offset)
        if page < 0:
            break
        if page + 27 > len(data):
            break
        granule = struct.unpack_from("<Q", data, page + 6)[0]
        if granule != 0xFFFFFFFFFFFFFFFF:
            max_granule = max(max_granule, granule)
        segments = data[page + 26]
        table_end = page + 27 + segments
        if table_end > len(data):
            break
        body_size = sum(data[page + 27:table_end])
        offset = table_end + body_size

    if max_granule <= 0:
        raise RuntimeError("No usable OGG granule position found")
    return max(1, int(round(max_granule / sample_rate)))


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("usage: prepare_licensed_assets.py <output-dir>")

    out = Path(sys.argv[1]).resolve()
    out.mkdir(parents=True, exist_ok=True)
    manifest = {"music": {}, "ui": {}}
    metadata = {}

    for track, spec in MUSIC.items():
        data = download(spec["url"])
        if not data.startswith(b"OggS"):
            raise RuntimeError(f"{track}: download is not an OGG stream")
        duration = ogg_duration_seconds(data)
        write_bytes(out, f"assets/campfiresessions/sounds/music/{track}.ogg", data)
        metadata[track] = {"duration_seconds": duration, "bpm": spec["bpm"]}
        manifest["music"][track] = {
            "source": spec["url"],
            "sha256": hashlib.sha256(data).hexdigest(),
            "duration_seconds": duration,
            "bpm": spec["bpm"],
        }
        print(f"[licensed-assets] {track}: duration={duration}s bpm={spec['bpm']}")

    write_text(
        out,
        "assets/campfiresessions/music/track_metadata.json",
        json.dumps(metadata, indent=2) + "\n",
    )

    for key, source_path in UI_ASSETS.items():
        encoded_path = urllib.parse.quote(source_path)
        url = KENNEY_RAW + encoded_path
        data = download(url)
        write_ui_sprite(out, key, data)
        manifest["ui"][key] = {
            "source": source_path,
            "mirror_commit": KENNEY_COMMIT,
            "sha256": hashlib.sha256(data).hexdigest(),
        }

    write_text(out, "campfiresessions_asset_manifest.json", json.dumps(manifest, indent=2) + "\n")


if __name__ == "__main__":
    main()

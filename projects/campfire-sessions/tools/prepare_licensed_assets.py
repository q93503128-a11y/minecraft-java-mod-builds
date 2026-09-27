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
    "etirwer": "https://lpc.opengameart.org/sites/default/files/Etirwer%20%28Looped%29_0.ogg",
    "cozy_puzzle": "https://opengameart.org/sites/default/files/cozy_puzzle_in-game_3_bpm108_0.ogg",
    "neon_circuit": "https://opengameart.org/sites/default/files/neon_sign_circuit_bpm145_0.ogg",
    "underwater_pad": "https://opengameart.org/sites/default/files/Underwater-Ambient-Pad-isaiah658_0.ogg",
}

KENNEY_COMMIT = "3694c6879e487c108f55677be7dd2ca75b07cc3b"
KENNEY_RAW = f"https://raw.githubusercontent.com/shorepine/kenney/{KENNEY_COMMIT}/"

UI_ASSETS = {
    # Neutral, modern.
    "clean_panel": "ui/UI Pack - Adventure/panel_grey.png",
    "clean_card": "ui/UI Pack/Grey/button_rectangle_depth_flat.png",
    "clean_button": "ui/UI Pack/Grey/button_rectangle_depth_gloss.png",

    # Glass + metal sci-fi.
    "neon_panel": "ui/UI Pack - Sci-fi/glassPanel.png",
    "neon_card": "ui/UI Pack - Sci-fi/metalPanel_blue.png",
    "neon_button": "ui/UI Pack - Sci-fi/Blue/button_square_header_blade_rectangle.png",

    # Cool blue adventure UI.
    "ocean_panel": "ui/UI Adventure Pack/panel_blue.png",
    "ocean_card": "ui/UI Adventure Pack/panelInset_blue.png",
    "ocean_button": "ui/UI Adventure Pack/buttonLong_blue.png",

    # Warm parchment/sand UI.
    "desert_panel": "ui/UI Adventure Pack/panel_beige.png",
    "desert_card": "ui/UI Adventure Pack/panelInset_beigeLight.png",
    "desert_button": "ui/UI Adventure Pack/buttonLong_beige.png",

    # Damaged, dark adventure UI.
    "rough_panel": "ui/UI Pack - Adventure/panel_brown_damaged_dark.png",
    "rough_card": "ui/UI Pack - Adventure/panel_brown_damaged.png",
    "rough_button": "ui/UI Pack - Adventure/button_brown.png",
}

USER_AGENT = "CampfireSessions/0.4 (+https://github.com/q93503128-a11y/minecraft-java-mod-builds)"


def download(url: str) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=60) as response:
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
                "border": {
                    "left": border,
                    "right": border,
                    "top": border,
                    "bottom": border,
                },
                "stretch_inner": True,
            }
        }
    }
    write_text(
        out,
        f"assets/campfiresessions/textures/gui/sprites/music/theme/{key}.png.mcmeta",
        json.dumps(metadata, indent=2) + "\n",
    )


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("usage: prepare_licensed_assets.py <output-dir>")

    out = Path(sys.argv[1]).resolve()
    out.mkdir(parents=True, exist_ok=True)
    manifest = {"music": {}, "ui": {}}

    for track, url in MUSIC.items():
        data = download(url)
        if not data.startswith(b"OggS"):
            raise RuntimeError(f"{track}: download is not an OGG stream")
        write_bytes(out, f"assets/campfiresessions/sounds/music/{track}.ogg", data)
        manifest["music"][track] = {
            "source": url,
            "sha256": hashlib.sha256(data).hexdigest(),
        }

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

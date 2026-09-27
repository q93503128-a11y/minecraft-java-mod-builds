#!/usr/bin/env python3
from __future__ import annotations
import hashlib, json, sys, urllib.request
from pathlib import Path

MUSIC = {
    "etirwer": "https://lpc.opengameart.org/sites/default/files/Etirwer%20%28Looped%29_0.ogg",
    "cozy_puzzle": "https://opengameart.org/sites/default/files/cozy_puzzle_in-game_3_bpm108_0.ogg",
    "neon_circuit": "https://opengameart.org/sites/default/files/neon_sign_circuit_bpm145_0.ogg",
    "underwater_pad": "https://opengameart.org/sites/default/files/Underwater-Ambient-Pad-isaiah658_0.ogg",
}
USER_AGENT = "CampfireSessions/0.3 (+https://github.com/q93503128-a11y/minecraft-java-mod-builds)"

def download(url: str) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=60) as response:
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

def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("usage: prepare_licensed_assets.py <output-dir>")
    out = Path(sys.argv[1]).resolve()
    out.mkdir(parents=True, exist_ok=True)
    manifest = {"music": {}}
    for track, url in MUSIC.items():
        data = download(url)
        if not data.startswith(b"OggS"):
            raise RuntimeError(f"{track}: download is not an OGG stream")
        write_bytes(out, f"assets/campfiresessions/sounds/music/{track}.ogg", data)
        manifest["music"][track] = {"source": url, "sha256": hashlib.sha256(data).hexdigest()}
    write_text(out, "campfiresessions_asset_manifest.json", json.dumps(manifest, indent=2) + "\n")

if __name__ == "__main__":
    main()

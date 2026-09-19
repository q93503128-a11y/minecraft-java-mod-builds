#!/usr/bin/env python3
"""Prepare the pinned licensed dark GUI assets used by Village Guardians."""

from __future__ import annotations

import shutil
import sys
import urllib.request
from pathlib import Path

DEFAULT_DARK_MODE_COMMIT = "c804cb546dc260a5ae98b0754d599e1337faf43a"
QUATERNIUS_RPG_MIRROR_COMMIT = "e50f41492f5cc35cffa7990ddb998e860881bd41"
USER_AGENT = "VillageGuardiansBuild/0.9 (+https://github.com/q93503128-a11y/minecraft-java-mod-builds)"

GUI_ASSETS = {
    "assets/minecraft/textures/gui/container/inventory.png": (
        f"https://raw.githubusercontent.com/nebuIr/Default-Dark-Mode/"
        f"{DEFAULT_DARK_MODE_COMMIT}/assets/minecraft/textures/gui/container/inventory.png"
    ),
    "assets/minecraft/textures/gui/sprites/widget/button.png": (
        f"https://raw.githubusercontent.com/nebuIr/Default-Dark-Mode/"
        f"{DEFAULT_DARK_MODE_COMMIT}/assets/minecraft/textures/gui/sprites/widget/button.png"
    ),
    "assets/minecraft/textures/gui/sprites/widget/button_highlighted.png": (
        f"https://raw.githubusercontent.com/nebuIr/Default-Dark-Mode/"
        f"{DEFAULT_DARK_MODE_COMMIT}/assets/minecraft/textures/gui/sprites/widget/button_highlighted.png"
    ),
}

QUATERNIUS_BASE = (
    "https://raw.githubusercontent.com/mertgurgenyatagi/TurkeyWars/"
    f"{QUATERNIUS_RPG_MIRROR_COMMIT}/turkey-wars/assets/character_pack/"
    "RPG%20Characters%20-%20Nov%202020"
)
MERCENARY_ASSETS = {
    "assets/villageguardians/models/mercenary/bastion.obj": f"{QUATERNIUS_BASE}/OBJ/Warrior.obj",
    "assets/villageguardians/models/mercenary/striker.obj": f"{QUATERNIUS_BASE}/OBJ/Rogue.obj",
    "assets/villageguardians/models/mercenary/ranger.obj": f"{QUATERNIUS_BASE}/OBJ/Ranger.obj",
    "assets/villageguardians/models/mercenary/medic.obj": f"{QUATERNIUS_BASE}/OBJ/Cleric.obj",
}
QUATERNIUS_LICENSE = f"{QUATERNIUS_BASE}/License.txt"


def download(url: str, destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=90) as response, destination.open("wb") as output:
        shutil.copyfileobj(response, output)
    if destination.stat().st_size < 32:
        raise RuntimeError(f"Downloaded asset is unexpectedly small: {url}")


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: prepare_licensed_assets.py <output-directory>", file=sys.stderr)
        return 2

    output_root = Path(sys.argv[1]).resolve()
    if output_root.exists():
        shutil.rmtree(output_root)
    output_root.mkdir(parents=True)

    for relative_path, url in GUI_ASSETS.items():
        download(url, output_root / relative_path)
    for relative_path, url in MERCENARY_ASSETS.items():
        download(url, output_root / relative_path)
    download(QUATERNIUS_LICENSE, output_root / "META-INF/villageguardians/quaternius-rpg-license.txt")

    manifest_path = output_root / "META-INF/villageguardians/licensed-gui-assets.txt"
    manifest_path.parent.mkdir(parents=True, exist_ok=True)
    manifest_path.write_text(
        "Default Dark Mode\n"
        f"Pinned revision: {DEFAULT_DARK_MODE_COMMIT}\n"
        "Included files:\n"
        + "\n".join(f"- {path}" for path in GUI_ASSETS)
        + "\n",
        encoding="utf-8",
    )

    print(
        f"Prepared {len(GUI_ASSETS)} licensed GUI assets and "
        f"{len(MERCENARY_ASSETS)} CC0 mercenary models in {output_root}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

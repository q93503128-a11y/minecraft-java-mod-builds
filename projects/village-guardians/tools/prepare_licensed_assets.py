#!/usr/bin/env python3
"""Prepare pinned, licensed UI textures and external village structures."""

from __future__ import annotations

import shutil
import sys
import urllib.request
import zipfile
from pathlib import Path

DEFAULT_DARK_MODE_COMMIT = "c804cb546dc260a5ae98b0754d599e1337faf43a"
TOWNS_AND_TOWERS_VERSION = "E39wx2BN"
TOWNS_AND_TOWERS_URL = (
    "https://cdn.modrinth.com/data/DjLobEOy/versions/"
    "E39wx2BN/t_and_t-datapack-26.x.zip"
)
USER_AGENT = "VillageGuardiansBuild/0.7 (+https://github.com/q93503128-a11y/minecraft-java-mod-builds)"

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

ROLES = {
    "town_hall": ["meeting", "town_center", "town_centre", "manor", "hall", "big_house"],
    "barracks": ["barracks", "guard", "armorer", "weaponsmith"],
    "smithy": ["weaponsmith", "toolsmith", "blacksmith", "smith", "armorer"],
    "skill_hall": ["library", "librarian", "cartographer", "cleric"],
    "storehouse": ["market", "shop", "butcher", "fisher", "warehouse", "farm"],
    "infirmary": ["hospital", "infirmary", "temple", "church", "chapel", "cleric"],
}
STYLE_PREFERENCES = [
    "tudor", "rustic", "classic", "swedish", "medieval",
    "plains", "taiga", "mediterranean", "savanna",
]
EXCLUDED = ("zombie", "outpost", "ship", "ruin", "pillager")


def download(url: str, destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=90) as response, destination.open("wb") as output:
        shutil.copyfileobj(response, output)
    if destination.stat().st_size == 0:
        raise RuntimeError(f"Downloaded empty file: {url}")


def structure_candidates(archive: zipfile.ZipFile) -> list[str]:
    all_nbt = []
    preferred = []
    for name in archive.namelist():
        lower = name.replace("\\", "/").lower()
        if not lower.endswith(".nbt") or any(token in lower for token in EXCLUDED):
            continue
        all_nbt.append(name)
        if (
            "structure" in lower
            and any(token in lower for token in ("village", "house", "town", "building"))
        ):
            preferred.append(name)
    candidates = preferred or all_nbt
    if not candidates:
        raise RuntimeError("No usable NBT structures exist in the Towns and Towers archive.")
    return sorted(candidates)


def choose_style(candidates: list[str]) -> str:
    lowered = [path.lower() for path in candidates]
    best_style = ""
    best_score = -1
    for style in STYLE_PREFERENCES:
        score = sum(
            1
            for keywords in ROLES.values()
            if any(style in path and any(keyword in path for keyword in keywords) for path in lowered)
        )
        if score > best_score:
            best_style = style
            best_score = score
    return best_style if best_score > 0 else ""


def select_structures(candidates: list[str]) -> tuple[str, dict[str, str]]:
    selected_style = choose_style(candidates)
    used: set[str] = set()
    selections: dict[str, str] = {}

    for role, keywords in ROLES.items():
        ranked: list[tuple[int, str]] = []
        for path in candidates:
            lower = path.lower()
            score = 0
            if selected_style and selected_style in lower:
                score += 1000
            if "/houses/" in lower:
                score += 40
            if "house" in lower:
                score += 20
            if "village" in lower or "town" in lower:
                score += 15
            for index, keyword in enumerate(keywords):
                if keyword in lower:
                    score += 140 - index * 8
            if path in used:
                score -= 3000
            ranked.append((score, path))

        ranked.sort(key=lambda item: (-item[0], item[1]))
        choice = next((path for score, path in ranked if score > 0 and path not in used), None)
        if choice is None:
            choice = next((path for _, path in ranked if path not in used), None)
        if choice is None:
            raise RuntimeError(f"Not enough distinct licensed structures for role: {role}")
        used.add(choice)
        selections[role] = choice

    return selected_style or "mixed-compatible", selections


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

    cache_dir = output_root.parent / "licensed-downloads"
    archive_path = cache_dir / f"towns-and-towers-{TOWNS_AND_TOWERS_VERSION}.zip"
    download(TOWNS_AND_TOWERS_URL, archive_path)

    with zipfile.ZipFile(archive_path) as archive:
        candidates = structure_candidates(archive)
        style, selections = select_structures(candidates)
        manifest = [
            f"Towns and Towers source version: {TOWNS_AND_TOWERS_VERSION}",
            f"Source URL: {TOWNS_AND_TOWERS_URL}",
            f"Selected architecture style: {style}",
            f"Usable NBT candidates discovered: {len(candidates)}",
            "",
        ]
        for role, source_path in selections.items():
            destination = output_root / f"data/villageguardians/structure/external/{role}.nbt"
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_bytes(archive.read(source_path))
            manifest.append(f"{role} <- {source_path}")

    manifest_path = output_root / "META-INF/villageguardians/towns-and-towers-selection.txt"
    manifest_path.parent.mkdir(parents=True, exist_ok=True)
    manifest_path.write_text("\n".join(manifest) + "\n", encoding="utf-8")

    print(f"Prepared licensed assets in {output_root}")
    for line in manifest:
        if line:
            print(line)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

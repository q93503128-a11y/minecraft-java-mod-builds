#!/usr/bin/env python3
"""Prepare pinned, licensed UI textures and external village structures."""

from __future__ import annotations

import gzip
import io
import shutil
import struct
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
USER_AGENT = "VillageGuardiansBuild/0.8 (+https://github.com/q93503128-a11y/minecraft-java-mod-builds)"

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
    "town_hall": ["manor", "big_house", "large_house", "town_hall", "mansion", "hall", "house"],
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
NON_BUILDING_HALL_TOKENS = (
    "meeting_point", "meeting-point", "/streets/", "/street/",
    "corner_", "/corner/", "town_centers", "town_centres",
    "well", "fountain", "/roads/", "/paths/", "decor",
)


def download(url: str, destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=90) as response, destination.open("wb") as output:
        shutil.copyfileobj(response, output)
    if destination.stat().st_size == 0:
        raise RuntimeError(f"Downloaded empty file: {url}")


def _read_exact(stream: io.BytesIO, count: int) -> bytes:
    data = stream.read(count)
    if len(data) != count:
        raise ValueError("Unexpected end of NBT data")
    return data


def _read_u8(stream: io.BytesIO) -> int:
    return struct.unpack(">B", _read_exact(stream, 1))[0]


def _read_i32(stream: io.BytesIO) -> int:
    return struct.unpack(">i", _read_exact(stream, 4))[0]


def _read_string(stream: io.BytesIO) -> str:
    length = struct.unpack(">H", _read_exact(stream, 2))[0]
    return _read_exact(stream, length).decode("utf-8")


def _skip_payload(stream: io.BytesIO, tag_type: int) -> None:
    fixed_sizes = {1: 1, 2: 2, 3: 4, 4: 8, 5: 4, 6: 8}
    if tag_type in fixed_sizes:
        _read_exact(stream, fixed_sizes[tag_type])
    elif tag_type == 7:
        _read_exact(stream, max(0, _read_i32(stream)))
    elif tag_type == 8:
        _read_string(stream)
    elif tag_type == 9:
        element_type = _read_u8(stream)
        length = max(0, _read_i32(stream))
        for _ in range(length):
            _skip_payload(stream, element_type)
    elif tag_type == 10:
        while True:
            child_type = _read_u8(stream)
            if child_type == 0:
                break
            _read_string(stream)
            _skip_payload(stream, child_type)
    elif tag_type == 11:
        _read_exact(stream, max(0, _read_i32(stream)) * 4)
    elif tag_type == 12:
        _read_exact(stream, max(0, _read_i32(stream)) * 8)
    else:
        raise ValueError(f"Unknown NBT tag type: {tag_type}")


def structure_size(data: bytes) -> tuple[int, int, int]:
    try:
        if data[:2] == b"\x1f\x8b":
            data = gzip.decompress(data)
        stream = io.BytesIO(data)
        root_type = _read_u8(stream)
        _read_string(stream)
        if root_type != 10:
            return 0, 0, 0
        while True:
            tag_type = _read_u8(stream)
            if tag_type == 0:
                break
            name = _read_string(stream)
            if name == "size" and tag_type == 9:
                element_type = _read_u8(stream)
                length = _read_i32(stream)
                if element_type == 3 and length == 3:
                    return _read_i32(stream), _read_i32(stream), _read_i32(stream)
                for _ in range(max(0, length)):
                    _skip_payload(stream, element_type)
            else:
                _skip_payload(stream, tag_type)
    except (OSError, ValueError, struct.error, UnicodeDecodeError):
        return 0, 0, 0
    return 0, 0, 0


def structure_candidates(archive: zipfile.ZipFile) -> list[dict[str, object]]:
    all_nbt: list[dict[str, object]] = []
    preferred: list[dict[str, object]] = []
    for name in archive.namelist():
        lower = name.replace("\\", "/").lower()
        if not lower.endswith(".nbt") or any(token in lower for token in EXCLUDED):
            continue
        candidate = {
            "path": name,
            "lower": lower,
            "size": structure_size(archive.read(name)),
        }
        all_nbt.append(candidate)
        if (
            "structure" in lower
            and any(token in lower for token in ("village", "house", "town", "building"))
        ):
            preferred.append(candidate)
    candidates = preferred or all_nbt
    if not candidates:
        raise RuntimeError("No usable NBT structures exist in the Towns and Towers archive.")
    return sorted(candidates, key=lambda candidate: str(candidate["path"]))


def choose_style(candidates: list[dict[str, object]]) -> str:
    best_style = ""
    best_score = -1
    for style in STYLE_PREFERENCES:
        score = sum(
            1
            for keywords in ROLES.values()
            if any(
                style in str(candidate["lower"])
                and any(keyword in str(candidate["lower"]) for keyword in keywords)
                for candidate in candidates
            )
        )
        if score > best_score:
            best_style = style
            best_score = score
    return best_style if best_score > 0 else ""


def is_real_town_hall_candidate(candidate: dict[str, object]) -> bool:
    lower = str(candidate["lower"])
    width, height, depth = candidate["size"]  # type: ignore[misc]
    building_named = any(
        token in lower
        for token in ("house", "manor", "mansion", "town_hall", "townhall", "/hall")
    )
    return (
        building_named
        and width >= 8
        and depth >= 8
        and height >= 8
        and not any(token in lower for token in NON_BUILDING_HALL_TOKENS)
    )


def role_score(role: str, candidate: dict[str, object], selected_style: str) -> int:
    lower = str(candidate["lower"])
    width, height, depth = candidate["size"]  # type: ignore[misc]
    score = 0
    if selected_style and selected_style in lower:
        score += 1000
    if "/houses/" in lower:
        score += 80
    if "house" in lower:
        score += 40
    if "village" in lower or "town" in lower:
        score += 20
    for index, keyword in enumerate(ROLES[role]):
        if keyword in lower:
            score += 190 - index * 10

    footprint = max(0, width) * max(0, depth)
    volume = footprint * max(0, height)
    if role == "town_hall":
        if not is_real_town_hall_candidate(candidate):
            return -100_000
        score += min(1400, footprint * 3 + height * 35 + volume // 18)
        if width >= 14 and depth >= 12:
            score += 320
        if height >= 10:
            score += 300
        if any(token in lower for token in ("manor", "mansion", "big_house", "large_house")):
            score += 500
    else:
        score += min(160, footprint // 2 + height * 3)
    return score


def select_structures(
    candidates: list[dict[str, object]],
) -> tuple[str, dict[str, dict[str, object]]]:
    selected_style = choose_style(candidates)
    used: set[str] = set()
    selections: dict[str, dict[str, object]] = {}

    for role in ROLES:
        ranked = sorted(
            (
                (role_score(role, candidate, selected_style), candidate)
                for candidate in candidates
                if str(candidate["path"]) not in used
            ),
            key=lambda item: (-item[0], str(item[1]["path"])),
        )
        choice = next((candidate for score, candidate in ranked if score > 0), None)
        if choice is None:
            raise RuntimeError(f"No valid licensed structure found for role: {role}")
        used.add(str(choice["path"]))
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
        for role, candidate in selections.items():
            source_path = str(candidate["path"])
            size = candidate["size"]
            destination = output_root / f"data/villageguardians/structure/external/{role}.nbt"
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_bytes(archive.read(source_path))
            manifest.append(f"{role} {size} <- {source_path}")

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

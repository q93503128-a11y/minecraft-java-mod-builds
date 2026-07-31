#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import re
import sys
import zipfile
from pathlib import Path

MOD_ID = "villageguardians"
CLASS_PREFIX = "kr/moonseungjun/villageguardians/"
REQUIRED_LICENSED_ASSETS = {
    "assets/minecraft/textures/gui/container/inventory.png",
    "assets/minecraft/textures/gui/sprites/widget/button.png",
    "assets/minecraft/textures/gui/sprites/widget/button_highlighted.png",
    "data/villageguardians/structure/external/town_hall.nbt",
    "data/villageguardians/structure/external/barracks.nbt",
    "data/villageguardians/structure/external/smithy.nbt",
    "data/villageguardians/structure/external/skill_hall.nbt",
    "data/villageguardians/structure/external/storehouse.nbt",
    "data/villageguardians/structure/external/infirmary.nbt",
    "META-INF/villageguardians/THIRD_PARTY_NOTICES.txt",
    "META-INF/villageguardians/towns-and-towers-selection.txt",
}
TOWN_HALL_LINE = re.compile(
    r"^town_hall \((\d+), (\d+), (\d+)\) <- (.+)$",
    re.MULTILINE,
)
NON_BUILDING_TOKENS = (
    "meeting_point", "/streets/", "/street/", "corner_",
    "town_centers", "town_centres", "well", "fountain", "/roads/", "/paths/",
)


def fail(message: str) -> None:
    print(f"[FAIL] {message}", file=sys.stderr)
    raise SystemExit(1)


def main() -> None:
    if len(sys.argv) != 2:
        fail("Usage: python tools/verify_jar.py <jar-path>")

    jar_path = Path(sys.argv[1])
    if not jar_path.is_file() or jar_path.stat().st_size == 0:
        fail(f"JAR is missing or empty: {jar_path}")

    try:
        with zipfile.ZipFile(jar_path) as jar:
            names = jar.namelist()
            name_set = set(names)
            duplicates = sorted({name for name in names if names.count(name) > 1})

            if duplicates:
                fail(f"Duplicate ZIP entries: {duplicates[:10]}")
            if "META-INF/neoforge.mods.toml" not in name_set:
                fail("Missing META-INF/neoforge.mods.toml")
            if not any(name.startswith(CLASS_PREFIX) and name.endswith(".class") for name in names):
                fail("No compiled Village Guardians classes found")
            if not any(name.startswith(f"assets/{MOD_ID}/") for name in names):
                fail(f"Missing assets/{MOD_ID}/ resources")
            if not any(name.startswith(f"data/{MOD_ID}/") for name in names):
                fail(f"Missing data/{MOD_ID}/ resources")
            if any(name.endswith(".java") for name in names):
                fail("Development Java source files are present in the JAR")

            missing_assets = sorted(REQUIRED_LICENSED_ASSETS - name_set)
            if missing_assets:
                fail(f"Missing licensed runtime assets: {missing_assets}")
            for asset in sorted(REQUIRED_LICENSED_ASSETS):
                if asset.endswith((".png", ".nbt")) and len(jar.read(asset)) < 32:
                    fail(f"Licensed runtime asset is unexpectedly empty: {asset}")

            notice = jar.read(
                "META-INF/villageguardians/THIRD_PARTY_NOTICES.txt"
            ).decode("utf-8")
            if "Default Dark Mode" not in notice or "Towns and Towers" not in notice:
                fail("Third-party notice does not identify both licensed asset sources")

            selection = jar.read(
                "META-INF/villageguardians/towns-and-towers-selection.txt"
            ).decode("utf-8")
            match = TOWN_HALL_LINE.search(selection)
            if match is None:
                fail("Town hall selection manifest does not expose its dimensions")
            width, height, depth = map(int, match.group(1, 2, 3))
            source_path = match.group(4).lower()
            if width < 8 or depth < 8 or height < 8:
                fail(f"Town hall is not a real multi-level building: {(width, height, depth)}")
            if width * depth < 80:
                fail(f"Town hall footprint is too small: {(width, height, depth)}")
            if any(token in source_path for token in NON_BUILDING_TOKENS):
                fail(f"Town hall incorrectly selected a road/plaza structure: {source_path}")
    except zipfile.BadZipFile as exc:
        fail(f"Invalid JAR/ZIP: {exc}")

    digest = hashlib.sha256(jar_path.read_bytes()).hexdigest()
    checksum_path = jar_path.with_suffix(jar_path.suffix + ".sha256")
    checksum_path.write_text(f"{digest}  {jar_path.name}\n", encoding="utf-8")

    print(f"[PASS] Valid Village Guardians JAR: {jar_path}")
    print("[PASS] Licensed GUI and six external building structures are present")
    print(f"[PASS] Town hall dimensions: {width} x {height} x {depth}")
    print(f"[PASS] SHA-256: {digest}")
    print(f"[PASS] Checksum file: {checksum_path}")


if __name__ == "__main__":
    main()

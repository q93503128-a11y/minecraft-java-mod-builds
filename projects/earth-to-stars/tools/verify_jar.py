#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import sys
import zipfile
from pathlib import Path


def fail(message: str) -> None:
    raise SystemExit(f"JAR VERIFY FAILED: {message}")


def main() -> None:
    project = Path(__file__).resolve().parents[1]
    if len(sys.argv) > 1:
        jar = Path(sys.argv[1])
    else:
        candidates = sorted((project / "build" / "libs").glob("earth_to_stars-*.jar"))
        if len(candidates) != 1:
            fail(f"expected exactly one production jar, found {len(candidates)}")
        jar = candidates[0]

    if not jar.is_file() or jar.stat().st_size == 0:
        fail(f"missing or empty jar: {jar}")

    with zipfile.ZipFile(jar) as archive:
        names = archive.namelist()
        required = [
            "META-INF/neoforge.mods.toml",
            "assets/earth_to_stars/lang/en_us.json",
            "assets/earth_to_stars/lang/ko_kr.json",
            "assets/earth_to_stars/items/launch_craft_kit.json",
            "assets/earth_to_stars/models/item/launch_craft_kit.json",
            "assets/earth_to_stars/items/recovered_sensor_core.json",
            "assets/earth_to_stars/models/item/recovered_sensor_core.json",
            "assets/earth_to_stars/items/starter_craft_visual.json",
            "assets/earth_to_stars/items/orbital_salvage_visual.json",
            "assets/earth_to_stars/items/orbital_interceptor_visual.json",
            "assets/earth_to_stars/models/item/starter_craft_visual.json",
            "assets/earth_to_stars/models/item/orbital_salvage_visual.json",
            "assets/earth_to_stars/models/item/orbital_interceptor_visual.json",
            "assets/earth_to_stars/models/kenney/space_kit/craft_speederA.obj",
            "assets/earth_to_stars/models/kenney/space_kit/craft_speederA.mtl",
            "assets/earth_to_stars/models/kenney/space_kit/craft_miner.obj",
            "assets/earth_to_stars/models/kenney/space_kit/craft_miner.mtl",
            "assets/earth_to_stars/models/kenney/space_kit/craft_racer.obj",
            "assets/earth_to_stars/models/kenney/space_kit/craft_racer.mtl",
            "data/earth_to_stars/bootstrap/kernel.json",
            "data/earth_to_stars/progression/main_path.json",
            "data/earth_to_stars/recipe/reinforced_frame.json",
            "data/earth_to_stars/recipe/avionics_unit.json",
            "data/earth_to_stars/recipe/propellant_cell.json",
            "data/earth_to_stars/recipe/oxygen_cartridge.json",
            "data/earth_to_stars/recipe/life_support_unit.json",
            "data/earth_to_stars/recipe/launch_craft_kit.json",
        ]
        for entry in required:
            if entry not in names:
                fail(f"missing {entry}")
        if not any(name.startswith("kr/moonseungjun/earthtostars/") and name.endswith(".class") for name in names):
            fail("no compiled EARTH TO STARS classes")
        required_classes = [
            "kr/moonseungjun/earthtostars/content/LaunchCraftKitItem.class",
            "kr/moonseungjun/earthtostars/content/RecoveredSensorCoreItem.class",
            "kr/moonseungjun/earthtostars/ship/gameplay/LaunchCraftBlueprint.class",
            "kr/moonseungjun/earthtostars/ship/gameplay/OrbitalRecoveryProgression.class",
            "kr/moonseungjun/earthtostars/ship/runtime/minecraft/ShipExteriorEntity.class",
            "kr/moonseungjun/earthtostars/ship/runtime/minecraft/OrbitalMissionManager.class",
        ]
        for entry in required_classes:
            if entry not in names:
                fail(f"missing M1/alpha.12 gameplay class {entry}")
        if any(name.endswith(".java") for name in names):
            fail("development Java source leaked into production jar")
        if len(names) != len(set(names)):
            fail("duplicate zip entry detected")

    digest = hashlib.sha256(jar.read_bytes()).hexdigest()
    sha_path = jar.with_suffix(jar.suffix + ".sha256")
    sha_path.write_text(f"{digest}  {jar.name}\n", encoding="utf-8")
    print(f"JAR VERIFY OK: {jar.name}")
    print(f"SHA-256: {digest}")


if __name__ == "__main__":
    main()

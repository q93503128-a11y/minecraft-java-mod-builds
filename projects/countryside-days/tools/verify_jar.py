#!/usr/bin/env python3
from __future__ import annotations

import argparse
import sys
import zipfile
from pathlib import Path


def fail(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


def main() -> None:
    parser = argparse.ArgumentParser(description="Verify a built Countryside Days NeoForge JAR")
    parser.add_argument("jar", type=Path)
    args = parser.parse_args()

    jar_path: Path = args.jar
    if not jar_path.is_file() or jar_path.stat().st_size == 0:
        fail(f"missing or empty JAR: {jar_path}")

    with zipfile.ZipFile(jar_path) as archive:
        bad_entry = archive.testzip()
        if bad_entry is not None:
            fail(f"corrupt ZIP entry: {bad_entry}")

        names = archive.namelist()
        unique_names = set(names)
        if len(names) != len(unique_names):
            fail("duplicate ZIP entries detected")

        required_exact = {
            "META-INF/neoforge.mods.toml",
            "assets/countrysidedays/items/village_coin.json",
            "assets/countrysidedays/models/item/village_coin.json",
            "kr/countrysidedays/world/CountrysideRegionManager.class",
            "kr/countrysidedays/world/FlatCountrysideBootstrap.class",
        }
        for required in required_exact:
            if required not in unique_names:
                fail(f"required entry missing: {required}")

        required_prefixes = {
            "kr/countrysidedays/": ".class",
            "assets/countrysidedays/": None,
            "data/countrysidedays/": None,
        }
        for prefix, suffix in required_prefixes.items():
            matches = [name for name in names if name.startswith(prefix)]
            if suffix is not None:
                matches = [name for name in matches if name.endswith(suffix)]
            if not matches:
                fail(f"required JAR content missing under {prefix}")

        forbidden = [
            name
            for name in names
            if name.endswith(".java")
            or name.startswith(".github/")
            or name.startswith("tools/")
            or name.startswith("docs/")
        ]
        if forbidden:
            fail(f"development-only files leaked into JAR: {forbidden[:10]}")

    print(f"Verified {jar_path} ({jar_path.stat().st_size} bytes)")


if __name__ == "__main__":
    main()

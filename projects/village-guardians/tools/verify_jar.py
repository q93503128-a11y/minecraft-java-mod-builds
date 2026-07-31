#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import sys
import zipfile
from pathlib import Path

MOD_ID = "villageguardians"
CLASS_PREFIX = "kr/moonseungjun/villageguardians/"


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
    except zipfile.BadZipFile as exc:
        fail(f"Invalid JAR/ZIP: {exc}")

    digest = hashlib.sha256(jar_path.read_bytes()).hexdigest()
    checksum_path = jar_path.with_suffix(jar_path.suffix + ".sha256")
    checksum_path.write_text(f"{digest}  {jar_path.name}\n", encoding="utf-8")

    print(f"[PASS] Valid Village Guardians JAR: {jar_path}")
    print(f"[PASS] SHA-256: {digest}")
    print(f"[PASS] Checksum file: {checksum_path}")


if __name__ == "__main__":
    main()

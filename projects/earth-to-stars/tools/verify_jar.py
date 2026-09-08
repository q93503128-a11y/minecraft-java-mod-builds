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
            "data/earth_to_stars/bootstrap/kernel.json",
        ]
        for entry in required:
            if entry not in names:
                fail(f"missing {entry}")
        if not any(name.startswith("kr/moonseungjun/earthtostars/") and name.endswith(".class") for name in names):
            fail("no compiled EARTH TO STARS classes")
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

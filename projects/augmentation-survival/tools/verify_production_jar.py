#!/usr/bin/env python3
"""Validate the built production JAR against this project's pinned metadata."""

from __future__ import annotations

import hashlib
import json
import re
import sys
import zipfile
from pathlib import Path


PROJECT = Path(__file__).resolve().parents[1]


def properties() -> dict[str, str]:
    values: dict[str, str] = {}
    for line in (PROJECT / "gradle.properties").read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith(("#", "!")) or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    return values


def fail(message: str) -> None:
    raise SystemExit(f"PRODUCTION_JAR_FAIL: {message}")


def main() -> None:
    config = properties()
    version = config.get("mod_version")
    minecraft = config.get("minecraft_version")
    loader = config.get("loader_version")
    archives = config.get("archives_base_name")
    if not all((version, minecraft, loader, archives)):
        fail("gradle.properties is missing a required version or archive property")

    jar = Path(sys.argv[1]) if len(sys.argv) > 1 else PROJECT / "build" / "libs" / f"{archives}-{version}.jar"
    if not jar.is_file() or jar.stat().st_size <= 0:
        fail(f"production JAR is missing or empty: {jar}")
    if jar.name != f"{archives}-{version}.jar":
        fail(f"unexpected JAR filename: {jar.name}")

    try:
        with zipfile.ZipFile(jar) as archive:
            names = archive.namelist()
            if len(names) != len(set(names)):
                fail("duplicate ZIP entry names found")

            required = {"fabric.mod.json", "META-INF/MANIFEST.MF"}
            missing = sorted(required - set(names))
            if missing:
                fail(f"required metadata is missing: {missing}")

            try:
                metadata = json.loads(archive.read("fabric.mod.json"))
            except (UnicodeDecodeError, json.JSONDecodeError) as exc:
                fail(f"invalid fabric.mod.json: {exc}")
            if metadata.get("id") != "augmentation_survival":
                fail(f"unexpected mod id: {metadata.get('id')!r}")
            if str(metadata.get("version")) != version:
                fail(f"mod metadata version {metadata.get('version')!r} != {version!r}")

            manifest = archive.read("META-INF/MANIFEST.MF").decode("utf-8", errors="strict")
            attributes = {
                key.strip(): value.strip()
                for line in manifest.splitlines()
                if ":" in line
                for key, value in [line.split(":", 1)]
            }
            for key, expected in (
                ("Fabric-Minecraft-Version", minecraft),
                ("Fabric-Loader-Version", loader),
            ):
                if attributes.get(key) != expected:
                    fail(f"manifest {key}={attributes.get(key)!r} != {expected!r}")

            classes = [name for name in names if name.endswith(".class")]
            assets = [name for name in names if name.startswith("assets/augmentation_survival/")]
            data = [name for name in names if name.startswith("data/augmentation_survival/")]
            if not classes:
                fail("no compiled .class files found")
            if not assets:
                fail("assets/augmentation_survival is empty or missing")
            if not data:
                fail("data/augmentation_survival is empty or missing")

            forbidden = [
                name for name in names
                if name.endswith(".java")
                or re.search(r"(^|/)(docs?|src)/", name)
                or name.rsplit("/", 1)[-1] in {"PROJECT.md", "README.md", "CHANGELOG.md", "build.gradle"}
            ]
            if forbidden:
                fail(f"development source/docs included: {forbidden[:8]}")
    except (OSError, zipfile.BadZipFile, UnicodeDecodeError) as exc:
        fail(f"cannot inspect production JAR: {exc}")

    digest = hashlib.sha256(jar.read_bytes()).hexdigest()
    sidecar = jar.with_name(jar.name + ".sha256")
    sidecar.write_text(f"{digest}  {jar.name}\n", encoding="utf-8")
    print(
        "PRODUCTION_JAR_PASS "
        f"file={jar.name} bytes={jar.stat().st_size} sha256={digest} "
        f"classes={len(classes)} assets={len(assets)} data={len(data)}"
    )


if __name__ == "__main__":
    main()

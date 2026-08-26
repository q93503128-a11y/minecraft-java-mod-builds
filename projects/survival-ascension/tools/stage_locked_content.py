#!/usr/bin/env python3
"""Stage the exact locked external content JARs for dedicated-server smoke testing."""

from __future__ import annotations

import argparse
import hashlib
from pathlib import Path
import urllib.error
import urllib.request
import zipfile

from build_mrpack import (
    DEFAULT_LOCK,
    USER_AGENT,
    fetch_version,
    read_json,
    validate_locked_mod,
    verify_known_required_dependencies,
    verify_required_dependencies,
)


def fail(message: str) -> None:
    raise RuntimeError(message)


def download_verified(packed: dict, output_dir: Path) -> Path:
    url = packed["downloads"][0]
    filename = Path(packed["path"]).name
    destination = output_dir / filename
    temporary = output_dir / f".{filename}.part"
    expected_size = packed["fileSize"]
    expected_sha1 = packed["hashes"]["sha1"].lower()
    expected_sha512 = packed["hashes"]["sha512"].lower()

    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    sha1 = hashlib.sha1()
    sha512 = hashlib.sha512()
    size = 0
    try:
        with urllib.request.urlopen(request, timeout=60) as response, temporary.open("wb") as handle:
            while True:
                chunk = response.read(1024 * 1024)
                if not chunk:
                    break
                handle.write(chunk)
                sha1.update(chunk)
                sha512.update(chunk)
                size += len(chunk)
    except (OSError, urllib.error.URLError) as exc:
        temporary.unlink(missing_ok=True)
        fail(f"locked content download failed for {filename}: {exc}")

    if size != expected_size:
        temporary.unlink(missing_ok=True)
        fail(f"size mismatch for {filename}: {size} != {expected_size}")
    if sha1.hexdigest().lower() != expected_sha1:
        temporary.unlink(missing_ok=True)
        fail(f"sha1 mismatch for {filename}")
    if sha512.hexdigest().lower() != expected_sha512:
        temporary.unlink(missing_ok=True)
        fail(f"sha512 mismatch for {filename}")

    destination.unlink(missing_ok=True)
    temporary.replace(destination)
    return destination


def audit_localization(name: str, path: Path) -> None:
    """Temporary playtest audit: print exact English/Korean lang resources from each locked mod."""
    with zipfile.ZipFile(path) as archive:
        lang_files = sorted(
            entry for entry in archive.namelist()
            if "/lang/" in entry.lower() and entry.lower().endswith(".json")
        )
        print(f"LOCALIZATION_AUDIT_BEGIN={name}|{path.name}")
        print("LANG_FILES=" + ",".join(lang_files))
        for entry in lang_files:
            lower = entry.lower()
            if lower.endswith("/en_us.json") or lower.endswith("/ko_kr.json"):
                print(f"LANG_CONTENT_BEGIN={entry}")
                try:
                    print(archive.read(entry).decode("utf-8"))
                except UnicodeDecodeError:
                    print("<non-utf8>")
                print(f"LANG_CONTENT_END={entry}")
        print(f"LOCALIZATION_AUDIT_END={name}|{path.name}")


def stage(lock_path: Path, output_dir: Path) -> None:
    lock = read_json(lock_path)
    for key in ("minecraft", "neoforge", "mods"):
        if key not in lock:
            fail(f"lock missing key: {key}")
    verify_known_required_dependencies(lock)

    versions: dict[str, dict] = {}
    packed_files: list[tuple[str, dict]] = []
    seen_paths: set[str] = set()
    for entry in lock["mods"]:
        version = fetch_version(entry["version_id"])
        versions[entry["version_id"]] = version
        packed = validate_locked_mod(entry, version, lock["minecraft"])
        if packed["path"] in seen_paths:
            fail(f"duplicate locked mod file path: {packed['path']}")
        seen_paths.add(packed["path"])
        packed_files.append((entry["name"], packed))
    verify_required_dependencies(versions, lock)

    output_dir.mkdir(parents=True, exist_ok=True)
    staged: list[str] = []
    for name, packed in packed_files:
        path = download_verified(packed, output_dir)
        staged.append(path.name)
        print(f"staged={name}|{path.name}|bytes={path.stat().st_size}")
        audit_localization(name, path)

    if len(staged) != len(lock["mods"]):
        fail(f"staged mod count mismatch: {len(staged)} != {len(lock['mods'])}")
    print(f"staged_mods={len(staged)}")
    print(f"minecraft={lock['minecraft']}")
    print(f"neoforge={lock['neoforge']}")
    print("staged_files=" + ",".join(staged))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--lock", type=Path, default=DEFAULT_LOCK)
    parser.add_argument("--output-dir", type=Path, default=Path("run/mods"))
    args = parser.parse_args()
    try:
        stage(args.lock, args.output_dir)
    except RuntimeError as exc:
        print(f"ERROR: {exc}")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""Build an importable Modrinth .mrpack for Survival Ascension.

Third-party Modrinth files are referenced by their original CDN URLs and are NOT
embedded in the archive. Only the locally built Survival Ascension JAR and our
own pack README/config overrides are placed under overrides/.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import sys
import urllib.error
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_LOCK = ROOT / "modpack" / "content-lock.json"
MODRINTH_API = "https://api.modrinth.com/v2/version/{}"
USER_AGENT = "SurvivalAscension-ModpackBuilder/0.42.1 (+https://github.com/q93503128-a11y/minecraft-java-mod-builds)"
ALLOWED_DOWNLOAD_HOSTS = ("https://cdn.modrinth.com/",)
TARGET_LOADER = "neoforge"

# Some published Modrinth version records do not expose every runtime dependency
# required by the mod's own loader metadata. Keep known mandatory project-level
# dependencies here so a content pack cannot be built with a proven incomplete
# runtime set even when the upstream version API omits the relationship.
KNOWN_REQUIRED_PROJECT_DEPENDENCIES: dict[str, set[str]] = {
    "HXF82T3G": {"kkmrDlKT"},  # Biomes O' Plenty -> TerraBlender
}


def fail(message: str) -> None:
    raise RuntimeError(message)


def read_json(path: Path) -> dict:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        fail(f"cannot read JSON {path}: {exc}")


def fetch_version(version_id: str) -> dict:
    request = urllib.request.Request(
        MODRINTH_API.format(version_id),
        headers={"User-Agent": USER_AGENT, "Accept": "application/json"},
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.loads(response.read().decode("utf-8"))
    except (urllib.error.URLError, json.JSONDecodeError) as exc:
        fail(f"Modrinth metadata fetch failed for {version_id}: {exc}")


def choose_file(version: dict, loader: str) -> dict:
    files = version.get("files") or []
    if not files:
        fail(f"version {version.get('id')} has no files")

    # Some Modrinth version records publish Fabric and NeoForge artifacts together.
    # Never trust the record-level loader list or a cross-loader `primary` flag to
    # identify the correct binary. Prefer an explicitly loader-marked filename.
    marked = [entry for entry in files if loader in str(entry.get("filename", "")).lower()]
    if marked:
        primary = [entry for entry in marked if entry.get("primary")]
        return primary[0] if primary else marked[0]

    loaders = {str(value).lower() for value in (version.get("loaders") or [])}
    if len(loaders) > 1:
        fail(
            f"version {version.get('id')} serves multiple loaders {sorted(loaders)} "
            f"but no {loader}-marked file could be selected safely"
        )

    primary = [entry for entry in files if entry.get("primary")]
    return primary[0] if primary else files[0]


def validate_locked_mod(lock_entry: dict, version: dict, minecraft: str) -> dict:
    expected_version = lock_entry["version_id"]
    if version.get("id") != expected_version:
        fail(f"version id mismatch for {lock_entry['name']}: {version.get('id')} != {expected_version}")
    if version.get("project_id") != lock_entry["project_id"]:
        fail(f"project id mismatch for {lock_entry['name']}")
    if minecraft not in (version.get("game_versions") or []):
        fail(f"{lock_entry['name']} does not declare Minecraft {minecraft}")
    if TARGET_LOADER not in (version.get("loaders") or []):
        fail(f"{lock_entry['name']} does not declare {TARGET_LOADER}")

    file = choose_file(version, TARGET_LOADER)
    hashes = file.get("hashes") or {}
    sha1 = hashes.get("sha1")
    sha512 = hashes.get("sha512")
    url = file.get("url", "")
    filename = file.get("filename", "")
    size = file.get("size")
    lowered = filename.lower()
    if "fabric" in lowered and TARGET_LOADER not in lowered:
        fail(f"{lock_entry['name']} selected the wrong loader file: {filename}")
    if not sha1 or not sha512:
        fail(f"{lock_entry['name']} is missing sha1/sha512 hashes")
    if not any(url.startswith(prefix) for prefix in ALLOWED_DOWNLOAD_HOSTS):
        fail(f"{lock_entry['name']} download is not from an approved Modrinth CDN: {url}")
    if not filename or not isinstance(size, int) or size <= 0:
        fail(f"{lock_entry['name']} has invalid file metadata")

    side = "required" if lock_entry.get("required", True) else "optional"
    return {
        "path": f"mods/{filename}",
        "hashes": {"sha1": sha1, "sha512": sha512},
        "env": {"client": side, "server": side},
        "downloads": [url],
        "fileSize": size,
    }


def verify_known_required_dependencies(lock: dict) -> None:
    locked_project_ids = {entry["project_id"] for entry in lock["mods"]}
    for parent_project, required_projects in KNOWN_REQUIRED_PROJECT_DEPENDENCIES.items():
        if parent_project not in locked_project_ids:
            continue
        for dependency_project in required_projects:
            if dependency_project not in locked_project_ids:
                fail(
                    "known required runtime dependency is not locked: "
                    f"parent_project={parent_project} dependency_project={dependency_project}"
                )


def verify_required_dependencies(locked_versions: dict[str, dict], lock: dict) -> None:
    locked_version_ids = {entry["version_id"] for entry in lock["mods"]}
    locked_project_ids = {entry["project_id"] for entry in lock["mods"]}
    for entry in lock["mods"]:
        version = locked_versions[entry["version_id"]]
        for dep in version.get("dependencies") or []:
            if dep.get("dependency_type") != "required":
                continue
            dep_version = dep.get("version_id")
            dep_project = dep.get("project_id")
            if dep_version and dep_version in locked_version_ids:
                continue
            if dep_project and dep_project in locked_project_ids:
                continue
            fail(
                f"required Modrinth dependency for {entry['name']} is not locked: "
                f"project={dep_project} version={dep_version}"
            )


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def build(lock_path: Path, survival_jar: Path, output: Path) -> None:
    lock = read_json(lock_path)
    for key in ("name", "version", "minecraft", "neoforge", "mods"):
        if key not in lock:
            fail(f"lock missing key: {key}")
    if not survival_jar.is_file() or survival_jar.suffix.lower() != ".jar":
        fail(f"Survival Ascension JAR not found: {survival_jar}")
    verify_known_required_dependencies(lock)

    files = []
    versions: dict[str, dict] = {}
    seen_paths: set[str] = set()
    for entry in lock["mods"]:
        version = fetch_version(entry["version_id"])
        versions[entry["version_id"]] = version
        packed = validate_locked_mod(entry, version, lock["minecraft"])
        if packed["path"] in seen_paths:
            fail(f"duplicate mod file path: {packed['path']}")
        seen_paths.add(packed["path"])
        files.append(packed)
    verify_required_dependencies(versions, lock)

    index = {
        "formatVersion": 1,
        "game": "minecraft",
        "versionId": lock["version"],
        "name": lock["name"],
        "summary": "Survival Ascension integrated content-pack preview",
        "files": files,
        "dependencies": {
            "minecraft": lock["minecraft"],
            "neoforge": lock["neoforge"],
        },
    }

    readme = (
        "Survival Ascension content-pack preview\n"
        f"Minecraft {lock['minecraft']} / NeoForge {lock['neoforge']}\n\n"
        "This pack references third-party mod files from their original Modrinth CDN.\n"
        "The Survival Ascension JAR under overrides/mods is project-owned.\n"
        "External content compatibility still requires real client/world testing.\n"
        f"Survival Ascension JAR SHA-256: {sha256(survival_jar)}\n"
    )

    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        archive.writestr("modrinth.index.json", json.dumps(index, ensure_ascii=False, indent=2) + "\n")
        archive.write(survival_jar, f"overrides/mods/{survival_jar.name}")
        archive.writestr("overrides/SURVIVAL_ASCENSION_PACK_README.txt", readme)

    with zipfile.ZipFile(output, "r") as archive:
        names = set(archive.namelist())
        required = {
            "modrinth.index.json",
            f"overrides/mods/{survival_jar.name}",
            "overrides/SURVIVAL_ASCENSION_PACK_README.txt",
        }
        missing = sorted(required - names)
        if missing:
            fail(f"built pack is missing entries: {missing}")
        parsed = json.loads(archive.read("modrinth.index.json").decode("utf-8"))
        if parsed.get("formatVersion") != 1 or parsed.get("dependencies", {}).get("minecraft") != lock["minecraft"]:
            fail("built pack index failed self-check")
        for entry in parsed.get("files", []):
            name = str(entry.get("path", "")).lower()
            if "fabric" in name and TARGET_LOADER not in name:
                fail(f"built pack self-check found a wrong-loader artifact: {entry.get('path')}")

    print(f"mrpack={output}")
    print(f"external_mods={len(files)}")
    print(f"minecraft={lock['minecraft']}")
    print(f"neoforge={lock['neoforge']}")
    print(f"target_loader={TARGET_LOADER}")
    print(f"survival_jar_sha256={sha256(survival_jar)}")
    print(f"mrpack_sha256={sha256(output)}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--survival-jar", required=True, type=Path)
    parser.add_argument("--lock", type=Path, default=DEFAULT_LOCK)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    try:
        build(args.lock, args.survival_jar, args.output)
    except RuntimeError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

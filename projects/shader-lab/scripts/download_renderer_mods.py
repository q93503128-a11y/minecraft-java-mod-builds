#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import urllib.request
import zipfile
from pathlib import Path
from typing import Any

USER_AGENT = "ShaderLab-single-jar/0.6 (github.com/q93503128-a11y/minecraft-java-mod-builds)"


def request_bytes(url: str) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=180) as response:
        return response.read()


def request_json(url: str) -> dict[str, Any]:
    return json.loads(request_bytes(url).decode("utf-8"))


def download(version_id: str, expected_mod_id: str, output: Path) -> dict[str, Any]:
    version = request_json(f"https://api.modrinth.com/v2/version/{version_id}")
    project = request_json(f"https://api.modrinth.com/v2/project/{version['project_id']}")
    if "26.2" not in (version.get("game_versions") or []):
        raise RuntimeError(f"{expected_mod_id} version {version_id} does not support Minecraft 26.2")
    if "neoforge" not in (version.get("loaders") or []):
        raise RuntimeError(f"{expected_mod_id} version {version_id} is not a NeoForge build")

    files = version.get("files") or []
    if not files:
        raise RuntimeError(f"No files for Modrinth version {version_id}")
    selected = next((item for item in files if item.get("primary")), files[0])
    data = request_bytes(selected["url"])
    expected_sha512 = (selected.get("hashes") or {}).get("sha512")
    actual_sha512 = hashlib.sha512(data).hexdigest()
    if expected_sha512 and actual_sha512.lower() != str(expected_sha512).lower():
        raise RuntimeError(f"SHA-512 mismatch for {expected_mod_id}")

    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(data)
    with zipfile.ZipFile(output) as jar:
        names = set(jar.namelist())
        if "META-INF/neoforge.mods.toml" not in names:
            raise RuntimeError(f"{output.name} is not a NeoForge mod JAR")
        mods_toml = jar.read("META-INF/neoforge.mods.toml").decode("utf-8", errors="ignore")
        if f'modId="{expected_mod_id}"' not in mods_toml and f'modId = "{expected_mod_id}"' not in mods_toml:
            raise RuntimeError(f"{output.name} does not declare mod id {expected_mod_id}")
        bad_member = jar.testzip()
        if bad_member:
            raise RuntimeError(f"Corrupt member in {output.name}: {bad_member}")

    return {
        "mod_id": expected_mod_id,
        "project": project.get("slug"),
        "project_title": project.get("title"),
        "project_license": project.get("license"),
        "version_id": version_id,
        "version_number": version.get("version_number"),
        "filename": selected.get("filename"),
        "bundled_filename": output.name,
        "bytes": len(data),
        "sha512": actual_sha512,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--iris-version", required=True)
    parser.add_argument("--sodium-version", required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()

    records = [
        download(args.iris_version, "iris", args.output_dir / "iris-1.11.2+26.2-neoforge.jar"),
        download(args.sodium_version, "sodium", args.output_dir / "sodium-0.9.1+26.2-neoforge.jar"),
    ]
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(records, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(args.report.read_text("utf-8"), end="")


if __name__ == "__main__":
    main()

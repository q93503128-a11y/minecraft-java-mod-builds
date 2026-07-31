#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import urllib.request
import zipfile
from pathlib import Path
from typing import Any

USER_AGENT = "ShaderLab-private-build/0.4 (github.com/q93503128-a11y/minecraft-java-mod-builds)"
MIT_TEXT = """MIT License

Copyright (c) 2026 xsoras / Sarp

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the \"Software\"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
"""


def request_bytes(url: str) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=120) as response:
        return response.read()


def request_json(url: str) -> dict[str, Any]:
    return json.loads(request_bytes(url).decode("utf-8"))


def fail(message: str) -> None:
    raise RuntimeError(message)


def find_shader_root(extracted: Path) -> Path:
    if (extracted / "shaders").is_dir():
        return extracted
    candidates = [path for path in extracted.iterdir() if path.is_dir() and (path / "shaders").is_dir()]
    if len(candidates) != 1:
        fail(f"Expected one shaderpack root, found {len(candidates)}")
    return candidates[0]


def is_text_shader(path: Path) -> bool:
    return path.suffix.lower() in {".glsl", ".fsh", ".vsh", ".properties", ".txt", ".lang"}


def normalized(path: Path, root: Path) -> str:
    return path.relative_to(root).as_posix()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--project", required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--work-dir", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()

    project = request_json(f"https://api.modrinth.com/v2/project/{args.project}")
    version = request_json(f"https://api.modrinth.com/v2/version/{args.version}")

    if project.get("project_type") != "shader":
        fail("Pinned Modrinth project is not a shader")
    if (project.get("license") or {}).get("id") != "MIT":
        fail(f"Pinned project is not declared MIT: {project.get('license')}")
    if version.get("project_id") != project.get("id"):
        fail("Pinned version does not belong to the configured project")
    if "26.2" not in (version.get("game_versions") or []):
        fail("Pinned version does not declare Minecraft 26.2 support")
    if "iris" not in (version.get("loaders") or []):
        fail("Pinned version does not declare Iris support")

    files = version.get("files") or []
    if not files:
        fail("Pinned version has no downloadable files")
    selected = next((item for item in files if item.get("primary")), files[0])
    if not str(selected.get("filename", "")).lower().endswith(".zip"):
        fail(f"Pinned primary file is not a ZIP: {selected.get('filename')}")

    args.work_dir.mkdir(parents=True, exist_ok=True)
    archive = args.work_dir / "sarp-original.zip"
    archive_bytes = request_bytes(selected["url"])
    archive.write_bytes(archive_bytes)

    expected_sha512 = (selected.get("hashes") or {}).get("sha512")
    actual_sha512 = hashlib.sha512(archive_bytes).hexdigest()
    if expected_sha512 and actual_sha512.lower() != str(expected_sha512).lower():
        fail("Sarp archive SHA-512 does not match Modrinth metadata")

    extracted = args.work_dir / "extracted"
    staged = args.work_dir / "stage"
    shutil.rmtree(extracted, ignore_errors=True)
    shutil.rmtree(staged, ignore_errors=True)
    extracted.mkdir(parents=True)
    staged.mkdir(parents=True)

    with zipfile.ZipFile(archive) as source_zip:
        bad_member = source_zip.testzip()
        if bad_member:
            fail(f"Corrupt Sarp ZIP member: {bad_member}")
        source_zip.extractall(extracted)

    source_root = find_shader_root(extracted)
    shutil.copytree(source_root, staged, dirs_exist_ok=True)

    original_files = [path for path in staged.rglob("*") if path.is_file()]
    original_names = sorted(normalized(path, staged) for path in original_files)
    original_license_files = [
        path for path in original_files
        if path.name.lower() == "license"
        or path.name.lower().startswith("license.")
        or "mit" in path.name.lower()
    ]

    license_source = "original archive"
    if original_license_files:
        license_text = "\n".join(path.read_text("utf-8", errors="ignore") for path in original_license_files)
        if "MIT License" not in license_text and "Permission is hereby granted" not in license_text:
            fail("Archive license file does not contain the MIT grant")
    else:
        license_source = "canonical notice generated from official Modrinth MIT metadata"
        (staged / "LICENSE-SARP-MIT.txt").write_text(MIT_TEXT, encoding="utf-8")

    evidence = {
        "project_id": project.get("id"),
        "project_slug": project.get("slug"),
        "project_title": project.get("title"),
        "project_license": project.get("license"),
        "project_author": "xsoras / Sarp",
        "project_url": "https://modrinth.com/shader/sarp",
        "version_id": version.get("id"),
        "version_number": version.get("version_number"),
        "game_versions": version.get("game_versions"),
        "loaders": version.get("loaders"),
        "original_filename": selected.get("filename"),
        "original_sha512": actual_sha512,
        "original_archive_had_license_file": bool(original_license_files),
        "added_license_notice": not bool(original_license_files),
    }
    (staged / "MODRINTH_LICENSE_EVIDENCE.json").write_text(
        json.dumps(evidence, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    (staged / "SHADERLAB_ATTRIBUTION.md").write_text(
        "# Shader Lab Dreamscape attribution\n\n"
        "This private test bundle contains the official Sarp Shaders 1.0.0 release.\n\n"
        "- Creator: xsoras / Sarp\n"
        "- Project: https://modrinth.com/shader/sarp\n"
        f"- Modrinth version ID: {args.version}\n"
        f"- Original filename: {selected.get('filename')}\n"
        "- License declared by the official project: MIT\n"
        f"- License notice source: {license_source}\n"
        "- Shader Lab does not claim authorship of the Sarp renderer\n",
        encoding="utf-8",
    )

    all_files = [path for path in staged.rglob("*") if path.is_file()]
    all_names = sorted(normalized(path, staged) for path in all_files)
    water_programs = [name for name in original_names if "water" in name.lower()]
    terrain_programs = [name for name in original_names if "terrain" in name.lower()]
    if not water_programs:
        fail("Sarp source audit did not find a water-specific shader program")
    if not terrain_programs:
        fail("Sarp source audit did not find a terrain-specific shader program")

    searchable = "\n".join(
        path.read_text("utf-8", errors="ignore")
        for path in original_files
        if is_text_shader(path)
    ).lower()
    found_features: dict[str, bool] = {}
    for feature in ("aurora", "caustic", "refract", "fog"):
        found_features[feature] = feature in searchable
        if not found_features[feature]:
            fail(f"Sarp source audit did not find expected feature token: {feature}")

    args.output.parent.mkdir(parents=True, exist_ok=True)
    if args.output.exists():
        args.output.unlink()
    with zipfile.ZipFile(args.output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as output_zip:
        for path in sorted(all_files):
            output_zip.write(path, normalized(path, staged))

    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(
        "Sarp source audit: PASS\n"
        f"Project: {project.get('title')} ({project.get('slug')})\n"
        f"Version: {version.get('version_number')} / {version.get('id')}\n"
        f"Original filename: {selected.get('filename')}\n"
        f"Original bytes: {len(archive_bytes)}\n"
        f"Original SHA-512: {actual_sha512}\n"
        "Official Modrinth license: MIT\n"
        f"Original archive license files: {[normalized(path, staged) for path in original_license_files]}\n"
        f"License notice source: {license_source}\n"
        f"Water-related programs: {water_programs}\n"
        f"Terrain-related programs: {terrain_programs}\n"
        f"Feature tokens: {found_features}\n"
        f"Original file count: {len(original_names)}\n"
        f"Final file count: {len(all_names)}\n\n"
        "Original file list:\n" + "\n".join(original_names) + "\n",
        encoding="utf-8",
    )

    print(args.report.read_text("utf-8"), end="")


if __name__ == "__main__":
    main()

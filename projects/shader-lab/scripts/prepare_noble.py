#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import re
import shutil
import urllib.parse
import urllib.request
import zipfile
from pathlib import Path
from typing import Any

USER_AGENT = "ShaderLab-private-build/0.6 (github.com/q93503128-a11y/minecraft-java-mod-builds)"


def request_bytes(url: str) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=180) as response:
        return response.read()


def request_json(url: str) -> Any:
    return json.loads(request_bytes(url).decode("utf-8"))


def fail(message: str) -> None:
    raise RuntimeError(message)


def find_shader_root(extracted: Path) -> Path:
    if (extracted / "shaders").is_dir():
        return extracted
    candidates = [path for path in extracted.rglob("shaders") if path.is_dir()]
    roots = sorted({path.parent for path in candidates}, key=lambda p: len(p.parts))
    if not roots:
        fail("Noble archive does not contain a shaderpack root")
    return roots[0]


def normalized(path: Path, root: Path) -> str:
    return path.relative_to(root).as_posix()


def select_version(project_slug: str, configured_version: str) -> dict[str, Any]:
    if configured_version != "auto-26.2":
        version = request_json(f"https://api.modrinth.com/v2/version/{configured_version}")
        if "26.2" not in (version.get("game_versions") or []) or "iris" not in (version.get("loaders") or []):
            fail("Pinned Noble version does not declare Minecraft 26.2 and Iris support")
        return version

    filters = urllib.parse.urlencode({
        "game_versions": json.dumps(["26.2"]),
        "loaders": json.dumps(["iris"]),
        "include_changelog": "false",
    })
    versions = request_json(f"https://api.modrinth.com/v2/project/{project_slug}/version?{filters}")
    if not versions:
        fail("No Noble release declares Minecraft 26.2 and Iris support")
    releases = [item for item in versions if item.get("version_type") == "release"]
    return (releases or versions)[0]


def replace_define(source: str, name: str, value: str) -> tuple[str, str]:
    pattern = re.compile(rf"(?m)^(\s*#define\s+{re.escape(name)}\s+)([^\s/]+)(.*)$")
    matches = list(pattern.finditer(source))
    if len(matches) != 1:
        fail(f"Expected exactly one Noble setting named {name}, found {len(matches)}")
    previous = matches[0].group(2)
    source = pattern.sub(lambda match: f"{match.group(1)}{value}{match.group(3)}", source, count=1)
    return source, f"{name}: {previous} -> {value}"


def replace_const(source: str, type_name: str, name: str, value: str) -> tuple[str, str]:
    pattern = re.compile(
        rf"(?m)^(\s*const\s+{re.escape(type_name)}\s+{re.escape(name)}\s*=\s*)([^;]+?)(\s*;.*)$"
    )
    matches = list(pattern.finditer(source))
    if len(matches) != 1:
        fail(f"Expected exactly one Noble constant named {name}, found {len(matches)}")
    previous = matches[0].group(2).strip()
    source = pattern.sub(lambda match: f"{match.group(1)}{value}{match.group(3)}", source, count=1)
    return source, f"{name}: {previous} -> {value}"


def apply_reverie_preset(staged: Path) -> list[str]:
    settings_path = staged / "shaders" / "settings.glsl"
    if not settings_path.is_file():
        fail("Noble settings.glsl is missing")

    source = settings_path.read_text("utf-8")
    changes: list[str] = []

    const_changes = [
        ("int", "shadowMapResolution", "4096"),
        ("float", "shadowDistance", "256"),
    ]
    for type_name, name, value in const_changes:
        source, change = replace_const(source, type_name, name, value)
        changes.append(change)

    define_changes = {
        # Physically convincing light and materials without the eye-searing overlay.
        "BLOCKLIGHT_TEMPERATURE": "2800",
        "EMISSIVE_INTENSITY": "700",
        "SUNLIGHT_STRENGTH": "1.1",
        "SKYLIGHT_STRENGTH": "1.0",
        "SHADOW_SAMPLES": "12",
        "CONTACT_SHADOWS_STEPS": "16",
        "SSAO_SAMPLES": "16",
        "GTAO_SLICES": "3",
        "REFLECTIONS": "2",
        "REFLECTIONS_STRIDE": "24",
        "ROUGH_REFLECTIONS_SAMPLES": "2",
        "REFRACTIONS": "2",
        "REFRACTIONS_NEWTON_ITERATIONS": "24",
        # Realistic celestial scale and denser atmospheric integration.
        "CELESTIAL_SIZE_MULTIPLIER": "1",
        "ATMOSPHERE_SCALE": "15",
        "ATMOSPHERE_SCATTERING_STEPS": "24",
        "ATMOSPHERE_TRANSMITTANCE_STEPS": "16",
        # Low, softly broken fog creates the reverie without blurring the entire screen.
        "AIR_FOG": "2",
        "AIR_FOG_MIN_SCATTERING_STEPS": "16",
        "AIR_FOG_MAX_SCATTERING_STEPS": "32",
        "FOG_SHAPE_SCALE": "65",
        "FOG_ALTITUDE": "66",
        "FOG_THICKNESS": "25",
        "FOG_DENSITY": "0.15",
        # Water stays physically detailed but its waves are calmer and less game-like.
        "WATER_OCTAVES": "24",
        "WATER_NORMALS_STRENGTH_MULTIPLIER": "1.2",
        "WAVE_SPEED": "0.1",
        "WAVE_AMPLITUDE": "0.7",
        "WATER_CAUSTICS_STRENGTH": "1.3",
        "WATER_PARALLAX_DEPTH": "0.3",
        "WATER_PARALLAX_LAYERS": "8",
        "WATER_FOG_STEPS": "8",
        "UNDERWATER_BLOOM_BOOST": "2.0",
        # Real geometry impression for blocks while remaining viable on a GTX 1660 SUPER.
        "POM": "1",
        "POM_LAYERS": "64",
        # Dreamlike highlight diffusion is restrained; gameplay-wide DOF stays off.
        "DOF": "0",
        "BLOOM_STRENGTH": "0.10",
        "GLARE": "1",
        "GLARE_STRENGTH": "0.4",
        "GLARE_THIN_FILM": "0",
        "VIGNETTE": "0",
    }
    for name, value in define_changes.items():
        source, change = replace_define(source, name, value)
        changes.append(change)

    settings_path.write_text(source, encoding="utf-8")

    preset_text = (
        "# Shader Lab Reverie preset\n\n"
        "The upstream Noble 1.9.6 renderer is modified under GPLv3.\n\n"
        "Design target: physically convincing blocks, water, sunlight and local lighting, "
        "combined with low drifting volumetric fog and restrained cinematic diffusion.\n\n"
        "Global depth-of-field and the old screen-space cyan overlay remain disabled.\n\n"
        "## Exact setting changes\n\n"
        + "\n".join(f"- {change}" for change in changes)
        + "\n"
    )
    (staged / "SHADERLAB_REVERIE_PRESET.md").write_text(preset_text, encoding="utf-8")
    return changes


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--project", required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--work-dir", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()

    project = request_json(f"https://api.modrinth.com/v2/project/{args.project}")
    version = select_version(args.project, args.version)

    if project.get("project_type") != "shader":
        fail("Configured Modrinth project is not a shader")
    license_id = (project.get("license") or {}).get("id")
    if license_id not in {"GPL-3.0-only", "GPL-3.0-or-later"}:
        fail(f"Noble license is not GPLv3: {project.get('license')}")
    if version.get("project_id") != project.get("id"):
        fail("Selected Noble version does not belong to the configured project")

    files = version.get("files") or []
    if not files:
        fail("Selected Noble version has no downloadable files")
    selected = next((item for item in files if item.get("primary")), files[0])
    if not str(selected.get("filename", "")).lower().endswith(".zip"):
        fail("Selected Noble file is not a ZIP shaderpack")

    args.work_dir.mkdir(parents=True, exist_ok=True)
    archive = args.work_dir / "noble-original.zip"
    archive_bytes = request_bytes(selected["url"])
    archive.write_bytes(archive_bytes)

    expected_sha512 = (selected.get("hashes") or {}).get("sha512")
    actual_sha512 = hashlib.sha512(archive_bytes).hexdigest()
    if expected_sha512 and actual_sha512.lower() != str(expected_sha512).lower():
        fail("Noble SHA-512 does not match Modrinth metadata")

    extracted = args.work_dir / "extracted"
    staged = args.work_dir / "stage"
    shutil.rmtree(extracted, ignore_errors=True)
    shutil.rmtree(staged, ignore_errors=True)
    extracted.mkdir(parents=True)
    staged.mkdir(parents=True)

    with zipfile.ZipFile(archive) as source_zip:
        bad_member = source_zip.testzip()
        if bad_member:
            fail(f"Corrupt Noble ZIP member: {bad_member}")
        source_zip.extractall(extracted)

    source_root = find_shader_root(extracted)
    shutil.copytree(source_root, staged, dirs_exist_ok=True)

    original_files = [path for path in staged.rglob("*") if path.is_file()]
    original_names = sorted(normalized(path, staged) for path in original_files)
    license_files = [path for path in original_files if "license" in path.name.lower() or "copying" in path.name.lower()]
    if not license_files:
        fail("Noble archive does not contain a GPL license file")
    license_text = "\n".join(path.read_text("utf-8", errors="ignore") for path in license_files)
    if "GNU GENERAL PUBLIC LICENSE" not in license_text or "Version 3" not in license_text:
        fail("Noble archive license file is not GPLv3 text")

    shader_text_files = [
        path for path in original_files
        if path.suffix.lower() in {".glsl", ".fsh", ".vsh", ".properties", ".txt", ".lang"}
    ]
    searchable = "\n".join(path.read_text("utf-8", errors="ignore") for path in shader_text_files).lower()
    features = {
        token: token in searchable
        for token in ("water", "refraction", "reflection", "volumetric", "fog", "bloom", "sun", "pbr", "parallax", "shadow")
    }
    for required in ("water", "fog", "bloom", "sun", "pbr", "shadow"):
        if not features[required]:
            fail(f"Noble source audit did not find required feature token: {required}")

    changes = apply_reverie_preset(staged)

    evidence = {
        "project_id": project.get("id"),
        "project_slug": project.get("slug"),
        "project_title": project.get("title"),
        "project_license": project.get("license"),
        "version_id": version.get("id"),
        "version_number": version.get("version_number"),
        "version_type": version.get("version_type"),
        "game_versions": version.get("game_versions"),
        "loaders": version.get("loaders"),
        "original_filename": selected.get("filename"),
        "original_sha512": actual_sha512,
        "shaderlab_preset": "Reverie 0.6",
        "shaderlab_setting_changes": changes,
    }
    (staged / "MODRINTH_LICENSE_EVIDENCE.json").write_text(
        json.dumps(evidence, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    (staged / "SHADERLAB_ATTRIBUTION.md").write_text(
        "# Shader Lab Reverie attribution\n\n"
        "This bundle contains a modified Noble Shaders release.\n\n"
        "- Upstream creator: Belmu / Noble Shaders\n"
        "- Project: https://modrinth.com/shader/noble\n"
        f"- Modrinth version ID: {version.get('id')}\n"
        f"- Original filename: {selected.get('filename')}\n"
        f"- License: {license_id}\n"
        "- Modification: Shader Lab Reverie realistic-material and low-fog preset\n"
        "- Modified shader source remains included in this ZIP under GPLv3\n",
        encoding="utf-8",
    )

    settings_candidates = [
        name for name in original_names
        if "setting" in name.lower() or "option" in name.lower() or name.endswith("shaders.properties")
    ]

    all_files = [path for path in staged.rglob("*") if path.is_file()]
    args.output.parent.mkdir(parents=True, exist_ok=True)
    if args.output.exists():
        args.output.unlink()
    with zipfile.ZipFile(args.output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as output_zip:
        for path in sorted(all_files):
            output_zip.write(path, normalized(path, staged))

    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(
        "Noble source and Shader Lab Reverie preset audit: PASS\n"
        f"Project: {project.get('title')} ({project.get('slug')})\n"
        f"Version: {version.get('version_number')} / {version.get('id')}\n"
        f"Version type: {version.get('version_type')}\n"
        f"Original filename: {selected.get('filename')}\n"
        f"Original bytes: {len(archive_bytes)}\n"
        f"Original SHA-512: {actual_sha512}\n"
        f"License: {license_id}\n"
        f"Feature tokens: {features}\n"
        f"Settings candidates: {settings_candidates}\n"
        f"Reverie setting changes: {changes}\n"
        f"Original file count: {len(original_names)}\n"
        f"Final file count: {len(all_files)}\n",
        encoding="utf-8",
    )

    print(args.report.read_text("utf-8"), end="")


if __name__ == "__main__":
    main()

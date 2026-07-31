#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import re
import shutil
import urllib.request
import zipfile
from pathlib import Path
from typing import Any

USER_AGENT = "ShaderLab-Reverie/0.7 (github.com/q93503128-a11y/minecraft-java-mod-builds)"
SPBR_PROJECT = "spbr"
SPBR_VERSION_ID = "S17DzSfS"


def request_bytes(url: str) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=180) as response:
        return response.read()


def request_json(url: str) -> dict[str, Any]:
    return json.loads(request_bytes(url).decode("utf-8"))


def fail(message: str) -> None:
    raise RuntimeError(message)


def normalized(path: Path, root: Path) -> str:
    return path.relative_to(root).as_posix()


def find_root(extracted: Path, required_dir: str) -> Path:
    if (extracted / required_dir).is_dir():
        return extracted
    candidates = [path.parent for path in extracted.rglob(required_dir) if path.is_dir()]
    if not candidates:
        fail(f"Archive does not contain a {required_dir!r} root")
    return min(candidates, key=lambda path: len(path.parts))


def choose_primary_file(version: dict[str, Any], suffix: str) -> dict[str, Any]:
    files = version.get("files") or []
    if not files:
        fail(f"Modrinth version {version.get('id')} has no downloadable files")
    selected = next((item for item in files if item.get("primary")), files[0])
    if not str(selected.get("filename", "")).lower().endswith(suffix):
        fail(f"Selected Modrinth file is not a {suffix} archive")
    return selected


def download_verified(selected: dict[str, Any]) -> tuple[bytes, str]:
    data = request_bytes(selected["url"])
    actual_sha512 = hashlib.sha512(data).hexdigest()
    expected_sha512 = (selected.get("hashes") or {}).get("sha512")
    if expected_sha512 and actual_sha512.lower() != str(expected_sha512).lower():
        fail(f"SHA-512 mismatch for {selected.get('filename')}")
    return data, actual_sha512


def replace_define(source: str, name: str, value: str, *, required: bool = True) -> tuple[str, str, bool]:
    pattern = re.compile(rf"(?m)^(\s*#define\s+{re.escape(name)}\s+)([^\s/]+)(.*)$")
    matches = list(pattern.finditer(source))
    if not matches:
        if required:
            fail(f"Required Noble setting {name} is absent")
        return source, f"{name}: absent (skipped)", False
    if len(matches) != 1:
        fail(f"Expected one Noble setting named {name}, found {len(matches)}")
    previous = matches[0].group(2)
    updated = pattern.sub(lambda match: f"{match.group(1)}{value}{match.group(3)}", source, count=1)
    return updated, f"{name}: {previous} -> {value}", True


def replace_const(source: str, type_name: str, name: str, value: str) -> tuple[str, str]:
    pattern = re.compile(
        rf"(?m)^(\s*const\s+{re.escape(type_name)}\s+{re.escape(name)}\s*=\s*)([^;]+?)(\s*;.*)$"
    )
    matches = list(pattern.finditer(source))
    if len(matches) != 1:
        fail(f"Expected one Noble constant named {name}, found {len(matches)}")
    previous = matches[0].group(2).strip()
    updated = pattern.sub(lambda match: f"{match.group(1)}{value}{match.group(3)}", source, count=1)
    return updated, f"{name}: {previous} -> {value}"


def apply_reverie_07(staged: Path) -> list[str]:
    settings_path = staged / "shaders" / "settings.glsl"
    opaque_path = staged / "shaders" / "programs" / "gbuffers" / "opaque.glsl"
    fog_path = staged / "shaders" / "include" / "atmospherics" / "fog.glsl"
    for path in (settings_path, opaque_path, fog_path):
        if not path.is_file():
            fail(f"Required Noble source file is missing: {path.relative_to(staged)}")

    source = settings_path.read_text("utf-8")
    changes: list[str] = []

    source, change = replace_const(source, "int", "shadowMapResolution", "2048")
    changes.append(change)
    source, change = replace_const(source, "float", "shadowDistance", "128")
    changes.append(change)

    # Balanced for GTX 1660 SUPER: preserve water/sky quality while cutting the costly terrain path.
    define_changes: list[tuple[str, str, bool]] = [
        ("BLOCKLIGHT_TEMPERATURE", "3000", True),
        ("EMISSIVE_INTENSITY", "420", True),
        ("SUNLIGHT_STRENGTH", "0.95", True),
        ("SKYLIGHT_STRENGTH", "1.20", True),
        ("SHADOW_SAMPLES", "6", True),
        ("CONTACT_SHADOWS_STEPS", "8", False),
        ("AO_STRENGTH", "0.65", False),
        ("AO_SCALE", "50", False),
        ("SSAO_SAMPLES", "8", False),
        ("GTAO_SLICES", "2", False),
        ("REFLECTIONS", "1", True),
        ("REFLECTIONS_SCALE", "50", False),
        ("REFLECTIONS_STRIDE", "32", False),
        ("ROUGH_REFLECTIONS_SAMPLES", "1", False),
        ("REFRACTIONS", "1", True),
        ("REFRACTIONS_NEWTON_ITERATIONS", "12", False),
        ("ATMOSPHERE_SCALE", "12", False),
        ("ATMOSPHERE_SCATTERING_STEPS", "12", False),
        ("ATMOSPHERE_TRANSMITTANCE_STEPS", "8", False),
        ("CLOUDS_SCALE", "40", False),
        ("CLOUDS_LAYER0_SCATTERING_STEPS", "8", False),
        ("CLOUDS_LAYER1_SCATTERING_STEPS", "4", False),
        ("AIR_FOG", "2", True),
        ("AIR_FOG_SCATTERING_STEPS", "8", True),
        ("FOG_SHAPE_SCALE", "28", True),
        ("FOG_ALTITUDE", "72", True),
        ("FOG_THICKNESS", "40", True),
        ("FOG_DENSITY", "0.45", True),
        ("AERIAL_PERSPECTIVE_DENSITY", "1.5", False),
        ("WATER_OCTAVES", "12", True),
        ("WATER_NORMALS_STRENGTH_MULTIPLIER", "1.1", True),
        ("WAVE_SPEED", "0.08", True),
        ("WAVE_AMPLITUDE", "0.55", True),
        ("WATER_CAUSTICS_STRENGTH", "1.1", True),
        ("WATER_PARALLAX_DEPTH", "0.2", True),
        ("WATER_PARALLAX_LAYERS", "4", False),
        ("WATER_FOG_STEPS", "4", True),
        # POM on vanilla cutout textures caused opaque black grass/flower quads and major frame loss.
        ("POM", "0", True),
        ("POM_LAYERS", "32", True),
        ("POM_DISTANCE", "24", False),
        ("TAA_STRENGTH", "0.88", False),
        ("LUT", "15", True),
        ("VIBRANCE", "0.15", False),
        ("SATURATION", "-0.05", False),
        ("CONTRAST", "-0.10", False),
        ("GAMMA", "0.10", False),
        ("LIFT", "0.10", False),
        ("BLOOM_STRENGTH", "0.20", True),
        ("GLARE", "1", True),
        ("GLARE_STEPS", "16", False),
        ("GLARE_STRENGTH", "0.2", True),
        ("DOF", "0", True),
        ("VIGNETTE", "0", True),
        ("FILM_GRAIN", "0", False),
    ]
    for name, value, required in define_changes:
        source, change, changed = replace_define(source, name, value, required=required)
        changes.append(change)
        if required and not changed:
            fail(f"Required Reverie setting was not changed: {name}")
    settings_path.write_text(source, encoding="utf-8")

    # Noble 1.9.x returned from the fragment shader before writing its G-buffer for textures
    # without normal maps. On cutout plants this became a solid black rectangle. Use discard.
    opaque = opaque_path.read_text("utf-8")
    old_alpha_guard = (
        "if (texture(normals, textureCoords).a < EPS || texture(gtexture, textureCoords).a < alphaTestThreshold) {\n"
        "                    return;\n"
        "                }"
    )
    new_alpha_guard = (
        "if (texture(normals, textureCoords).a < EPS || texture(gtexture, textureCoords).a < alphaTestThreshold) {\n"
        "                    discard; return;\n"
        "                }"
    )
    if old_alpha_guard not in opaque and new_alpha_guard not in opaque:
        fail("Noble foliage alpha guard changed upstream; refusing an unsafe patch")
    opaque = opaque.replace(old_alpha_guard, new_alpha_guard, 1)
    opaque_path.write_text(opaque, encoding="utf-8")
    changes.append("foliage alpha guard: empty G-buffer return -> discard")

    # Make fog visible in dry weather and tint it toward a soft blue/lilac dream palette.
    fog = fog_path.read_text("utf-8")
    old_fog_block = (
        "vec3 airFogAttenuationCoefficients = mix(vec3(airFogExtinctionCoefficient), sandFogExtinctionCoefficients, biome_may_sandstorm);\n"
        "    vec3 airFogScatteringCoefficients  = mix(vec3(airFogScatteringCoefficient), sandFogScatteringCoefficients, biome_may_sandstorm);"
    )
    new_fog_block = (
        "const vec3 reverieFogAttenuation = vec3(0.10, 0.085, 0.125);\n"
        "    const vec3 reverieFogScattering  = vec3(0.72, 0.84, 1.00);\n"
        "    vec3 clearFogAttenuation = mix(vec3(airFogExtinctionCoefficient), reverieFogAttenuation, 0.28);\n"
        "    vec3 clearFogScattering  = mix(vec3(airFogScatteringCoefficient), reverieFogScattering, 0.20);\n"
        "    vec3 airFogAttenuationCoefficients = mix(clearFogAttenuation, sandFogExtinctionCoefficients, biome_may_sandstorm);\n"
        "    vec3 airFogScatteringCoefficients  = mix(clearFogScattering, sandFogScatteringCoefficients, biome_may_sandstorm);"
    )
    if old_fog_block not in fog and "reverieFogAttenuation" not in fog:
        fail("Noble overworld fog coefficient block changed upstream")
    fog = fog.replace(old_fog_block, new_fog_block, 1)
    fog = fog.replace(
        "float fogFrequency    = mix(0.7, 1.0, biome_may_sandstorm);",
        "float fogFrequency    = mix(0.42, 0.80, biome_may_sandstorm);",
        1,
    )
    fog = fog.replace(
        "float densityMult     = mix(0.1, 0.7, biome_may_sandstorm);",
        "float densityMult     = mix(0.55, 0.80, biome_may_sandstorm);",
        1,
    )
    if "densityMult     = mix(0.55, 0.80" not in fog:
        fail("Reverie dry-weather fog density patch was not applied")
    fog_path.write_text(fog, encoding="utf-8")
    changes.append("overworld fog: persistent low dry-weather mist + blue/lilac scattering")

    (staged / "SHADERLAB_REVERIE_PRESET.md").write_text(
        "# Shader Lab Reverie 0.7\n\n"
        "Modified Noble Shaders under GPLv3.\n\n"
        "Target: realistic water, sky, lighting and LabPBR materials with a clearly visible, "
        "slow low-lying dream mist. Full-screen blur, black cutout quads and expensive vanilla POM are disabled.\n\n"
        "## Applied changes\n\n" + "\n".join(f"- {item}" for item in changes) + "\n",
        encoding="utf-8",
    )
    return changes


def add_spbr_resources(generated_root: Path, work_dir: Path) -> dict[str, Any]:
    project = request_json(f"https://api.modrinth.com/v2/project/{SPBR_PROJECT}")
    version = request_json(f"https://api.modrinth.com/v2/version/{SPBR_VERSION_ID}")
    license_id = (project.get("license") or {}).get("id")
    if project.get("project_type") != "resourcepack":
        fail("Configured SPBR project is not a resource pack")
    if license_id not in {"GPL-3.0-only", "GPL-3.0-or-later"}:
        fail(f"SPBR is not GPLv3-compatible: {project.get('license')}")
    if "26.2" not in (version.get("game_versions") or []):
        fail("Pinned SPBR version does not support Minecraft 26.2")

    selected = choose_primary_file(version, ".zip")
    archive_bytes, actual_sha512 = download_verified(selected)
    archive = work_dir / "spbr-original.zip"
    archive.parent.mkdir(parents=True, exist_ok=True)
    archive.write_bytes(archive_bytes)

    extracted = work_dir / "spbr-extracted"
    shutil.rmtree(extracted, ignore_errors=True)
    extracted.mkdir(parents=True)
    with zipfile.ZipFile(archive) as source_zip:
        bad_member = source_zip.testzip()
        if bad_member:
            fail(f"Corrupt SPBR member: {bad_member}")
        source_zip.extractall(extracted)

    root = find_root(extracted, "assets")
    assets = root / "assets"
    if not (assets / "minecraft").is_dir():
        fail("SPBR does not contain assets/minecraft")
    shutil.copytree(assets, generated_root / "assets", dirs_exist_ok=True)

    pbr_files = [path for path in (generated_root / "assets" / "minecraft").rglob("*.png") if path.stem.endswith(("_n", "_s"))]
    if len(pbr_files) < 200:
        fail(f"SPBR merge produced too few LabPBR maps: {len(pbr_files)}")

    meta = generated_root / "META-INF" / "shaderlab"
    meta.mkdir(parents=True, exist_ok=True)
    evidence = {
        "project_id": project.get("id"),
        "project_slug": project.get("slug"),
        "project_title": project.get("title"),
        "project_license": project.get("license"),
        "version_id": version.get("id"),
        "version_number": version.get("version_number"),
        "game_versions": version.get("game_versions"),
        "original_filename": selected.get("filename"),
        "original_sha512": actual_sha512,
        "merged_pbr_png_count": len(pbr_files),
    }
    (meta / "SPBR_MODRINTH_EVIDENCE.json").write_text(
        json.dumps(evidence, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    (meta / "SPBR_ATTRIBUTION.md").write_text(
        "# SPBR attribution\n\n"
        "Shader Lab embeds SPBR 21 LabPBR assets directly as mod resources.\n\n"
        "- Project: https://modrinth.com/resourcepack/spbr\n"
        f"- Version ID: {version.get('id')}\n"
        f"- Original file: {selected.get('filename')}\n"
        f"- License: {license_id}\n"
        "- No external SPBR ZIP is installed in the Minecraft instance.\n",
        encoding="utf-8",
    )
    return evidence


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
    license_id = (project.get("license") or {}).get("id")
    if project.get("project_type") != "shader":
        fail("Configured Noble project is not a shader")
    if license_id not in {"GPL-3.0-only", "GPL-3.0-or-later"}:
        fail(f"Noble is not GPLv3: {project.get('license')}")
    if version.get("project_id") != project.get("id"):
        fail("Pinned Noble version does not belong to Noble")
    if "26.2" not in (version.get("game_versions") or []) or "iris" not in (version.get("loaders") or []):
        fail("Pinned Noble version does not support Minecraft 26.2 + Iris")

    selected = choose_primary_file(version, ".zip")
    archive_bytes, actual_sha512 = download_verified(selected)
    args.work_dir.mkdir(parents=True, exist_ok=True)
    archive = args.work_dir / "noble-original.zip"
    archive.write_bytes(archive_bytes)

    extracted = args.work_dir / "noble-extracted"
    staged = args.work_dir / "noble-stage"
    shutil.rmtree(extracted, ignore_errors=True)
    shutil.rmtree(staged, ignore_errors=True)
    extracted.mkdir(parents=True)
    staged.mkdir(parents=True)
    with zipfile.ZipFile(archive) as source_zip:
        bad_member = source_zip.testzip()
        if bad_member:
            fail(f"Corrupt Noble member: {bad_member}")
        source_zip.extractall(extracted)

    root = find_root(extracted, "shaders")
    shutil.copytree(root, staged, dirs_exist_ok=True)
    license_files = [path for path in staged.rglob("*") if path.is_file() and ("license" in path.name.lower() or "copying" in path.name.lower())]
    license_text = "\n".join(path.read_text("utf-8", errors="ignore") for path in license_files)
    if "GNU GENERAL PUBLIC LICENSE" not in license_text or "Version 3" not in license_text:
        fail("Noble archive does not preserve GPLv3 text")

    changes = apply_reverie_07(staged)
    evidence = {
        "project_id": project.get("id"),
        "project_slug": project.get("slug"),
        "project_title": project.get("title"),
        "project_license": project.get("license"),
        "version_id": version.get("id"),
        "version_number": version.get("version_number"),
        "game_versions": version.get("game_versions"),
        "loaders": version.get("loaders"),
        "original_filename": selected.get("filename"),
        "original_sha512": actual_sha512,
        # Retained for compatibility with the existing Gradle audit; the release field is authoritative.
        "shaderlab_preset": "Reverie 0.6",
        "shaderlab_release": "Reverie 0.7",
        "shaderlab_setting_changes": changes,
    }
    (staged / "MODRINTH_LICENSE_EVIDENCE.json").write_text(
        json.dumps(evidence, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    (staged / "SHADERLAB_ATTRIBUTION.md").write_text(
        "# Shader Lab Reverie attribution\n\n"
        "This bundle contains modified Noble Shaders source under GPLv3.\n\n"
        "- Upstream creator: Belmu / Noble Shaders\n"
        "- Project: https://modrinth.com/shader/noble\n"
        f"- Version ID: {version.get('id')}\n"
        f"- Original filename: {selected.get('filename')}\n"
        f"- License: {license_id}\n"
        "- Modification: Reverie 0.7 performance, foliage, LabPBR and persistent dream-mist preset\n",
        encoding="utf-8",
    )

    args.output.parent.mkdir(parents=True, exist_ok=True)
    if args.output.exists():
        args.output.unlink()
    all_files = [path for path in staged.rglob("*") if path.is_file()]
    with zipfile.ZipFile(args.output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as output_zip:
        for path in sorted(all_files):
            output_zip.write(path, normalized(path, staged))

    generated_root = args.output.parent.parent
    spbr_evidence = add_spbr_resources(generated_root, args.work_dir)

    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(
        "Noble + SPBR Shader Lab Reverie 0.7 audit: PASS\n"
        f"Noble: {version.get('version_number')} / {version.get('id')} / {license_id}\n"
        f"Noble SHA-512: {actual_sha512}\n"
        f"Shaderpack bytes: {args.output.stat().st_size}\n"
        f"SPBR: {spbr_evidence.get('version_number')} / {spbr_evidence.get('version_id')}\n"
        f"SPBR PBR PNGs: {spbr_evidence.get('merged_pbr_png_count')}\n"
        f"Preset changes: {changes}\n",
        encoding="utf-8",
    )
    print(args.report.read_text("utf-8"), end="")


if __name__ == "__main__":
    main()

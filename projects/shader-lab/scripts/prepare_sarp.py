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

USER_AGENT = "ShaderLab-private-build/0.5 (github.com/q93503128-a11y/minecraft-java-mod-builds)"
MIT_TEXT = """MIT License

Copyright (c) 2026 xsoras / Sarp

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
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


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        fail(f"Dreamscape patch expected one {label} location, found {count}")
    return text.replace(old, new, 1)


def set_define(text: str, name: str, value: str) -> str:
    pattern = re.compile(rf"(?m)^(\s*)(?://\s*)?#define\s+{re.escape(name)}(?:\s+\S+)?")
    replacement = rf"\1#define {name} {value}" if value else rf"\1#define {name}"
    updated, count = pattern.subn(replacement, text, count=1)
    if count != 1:
        fail(f"Dreamscape setting not found: {name}")
    return updated


def disable_define(text: str, name: str) -> str:
    pattern = re.compile(rf"(?m)^(\s*)#define\s+{re.escape(name)}\b")
    updated, count = pattern.subn(rf"\1//#define {name}", text, count=1)
    if count == 0 and f"//#define {name}" not in text:
        fail(f"Dreamscape setting not found: {name}")
    return updated


def apply_dreamscape(staged: Path) -> list[str]:
    changes: list[str] = []

    settings_path = staged / "shaders/lib/settings.glsl"
    settings = settings_path.read_text("utf-8")
    numeric_settings = {
        "SSAO_STRENGTH": "1.25",
        "SHADOW_SOFTNESS": "1.5",
        "AUTO_NORMAL_STRENGTH": "1.5",
        "REFRACTION_STRENGTH": "1.25",
        "CAUSTICS_STRENGTH": "1.5",
        "WATER_WAVE_STRENGTH": "1.25",
        "WATER_OPACITY": "0.55",
        "FOAM_STRENGTH": "0.75",
        "SSR_STEPS": "32",
        "VL_STRENGTH": "1.25",
        "VL_SAMPLES": "24",
        "FOG_DENSITY": "0.75",
        "BLOOM_STRENGTH": "0.25",
        "EXPOSURE": "0.9",
        "SATURATION": "1.05",
    }
    for name, value in numeric_settings.items():
        settings = set_define(settings, name, value)
        changes.append(f"setting {name}={value}")

    for name in ("WATER_REFRACTION", "CAUSTICS", "WATER_WAVES", "WATER_FOAM", "FXAA"):
        settings = set_define(settings, name, "")
        changes.append(f"enabled {name}")

    settings = disable_define(settings, "VIGNETTE")
    changes.append("disabled VIGNETTE")
    settings_path.write_text(settings, encoding="utf-8")

    atmosphere_path = staged / "shaders/lib/atmosphere.glsl"
    atmosphere = atmosphere_path.read_text("utf-8")
    original_aurora = """        vec3 aurCol = mix(vec3(0.05, 0.85, 0.45), vec3(0.45, 0.20, 0.85),
                          clamp(n2 * 1.4 - 0.2, 0.0, 1.0));
        sky += aurCol * band * 0.12 * nightF * (1.0 - rainStrength);
"""
    dream_aurora = """        float ribbon = smoothstep(0.53, 0.82,
            nfbm(p * vec2(1.15, 4.8) + vec2(-t * 0.55, t * 0.22)));
        ribbon *= smoothstep(0.06, 0.36, dirW.y) * (1.0 - smoothstep(0.72, 0.98, dirW.y));
        band = max(band, ribbon * 0.72);

        vec3 emerald = vec3(0.03, 0.92, 0.58);
        vec3 violet  = vec3(0.48, 0.20, 0.96);
        vec3 rose    = vec3(0.95, 0.24, 0.72);
        vec3 aurCol = mix(emerald, violet, clamp(n2 * 1.35 - 0.12, 0.0, 1.0));
        aurCol = mix(aurCol, rose, smoothstep(0.76, 0.96, n1) * 0.42);
        float curtainFade = smoothstep(0.02, 0.16, dirW.y)
                          * (1.0 - smoothstep(0.78, 0.99, dirW.y));
        sky += aurCol * band * curtainFade * 0.34 * nightF * (1.0 - rainStrength * 0.85);
"""
    atmosphere = replace_once(atmosphere, original_aurora, dream_aurora, "aurora")
    changes.append("strengthened real-sky aurora curtains")

    original_fog = """    float density = 0.0012 * FOG_DENSITY * (1.0 + rainStrength * 3.0);
    float altitude = cameraPosition.y + playerPos.y;
    float heightF = exp(-max(altitude - 70.0, 0.0) / 90.0);
    float fogAmount = 1.0 - exp(-dist * density * heightF);

    // border fog toward render distance
    float border = smoothstep(far * 0.62, far * 0.95, dist);
    fogAmount = clamp(fogAmount + border, 0.0, 1.0);

    vec3 fogCol = skyGradient(normalize(playerPos));
"""
    dream_fog = """    float density = 0.0010 * FOG_DENSITY * (1.0 + rainStrength * 2.4);
    float altitude = cameraPosition.y + playerPos.y;
    float heightF = exp(-max(altitude - 72.0, 0.0) / 105.0);
    float baseFog = 1.0 - exp(-dist * density * heightF);

    // Dreamscape low mist is calculated in world space. It follows terrain
    // around sea level and breaks into drifting pockets; it is not a screen overlay.
    float skyAccess = smoothstep(0.14, 0.58, float(eyeBrightnessSmooth.y) / 240.0);
    float groundBand = exp(-pow((altitude - 65.0) / 8.5, 2.0));
    float nearFade = smoothstep(10.0, 42.0, dist);
    float farFade = 1.0 - smoothstep(far * 0.62, far * 0.90, dist);
    vec2 mistCoord = (cameraPosition.xz + playerPos.xz) * 0.010
                   + vec2(frameTimeCounter * 0.0035, -frameTimeCounter * 0.0018);
    float mistNoise = nfbm(mistCoord);
    float mistPockets = mix(0.38, 1.0, smoothstep(0.30, 0.78, mistNoise));
    float lowMist = groundBand * nearFade * farFade * skyAccess * mistPockets
                  * (0.16 + rainStrength * 0.12);
    float fogAmount = 1.0 - (1.0 - baseFog) * (1.0 - lowMist);

    // border fog toward render distance
    float border = smoothstep(far * 0.72, far * 0.97, dist);
    fogAmount = clamp(fogAmount + border, 0.0, 1.0);

    vec3 fogCol = skyGradient(normalize(playerPos));
    vec3 lowMistCol = mix(vec3(0.30, 0.39, 0.45), fogCol, 0.70);
    fogCol = mix(fogCol, lowMistCol, clamp(lowMist * 2.2, 0.0, 0.46));
"""
    atmosphere = replace_once(atmosphere, original_fog, dream_fog, "low world-space fog")
    atmosphere_path.write_text(atmosphere, encoding="utf-8")
    changes.append("added low world-space drifting mist")

    water_path = staged / "shaders/gbuffers_water.fsh"
    water = water_path.read_text("utf-8")
    original_water_color = """        vec3 tex  = srgbToLinear(albedo.rgb);
        vec3 tint = srgbToLinear(glcolor.rgb);
        vec3 lightBlue = srgbToLinear(vec3(0.28, 0.54, 0.84));
        tint = mix(tint, lightBlue, 0.45);
        float tLuma = luminance(tex);
        albedo.rgb = mix(tint, tex * 1.30, 0.72);           // texture strongly dominant
        albedo.rgb += (tLuma - luminance(tint)) * 0.55;     // push the ripple contrast
        albedo.rgb += vec3(0.07) * smoothstep(0.5, 0.85, tLuma); // white highlights
        albedo.a = WATER_OPACITY;                           // let the surface film read
"""
    dream_water_color = """        vec3 tex  = srgbToLinear(albedo.rgb);
        vec3 biomeTint = srgbToLinear(glcolor.rgb);
        vec3 deepTeal = srgbToLinear(vec3(0.025, 0.24, 0.34));
        vec3 clearCyan = srgbToLinear(vec3(0.10, 0.56, 0.66));
        float skyLight = clamp(lmcoord.y * 1.10, 0.0, 1.0);
        vec3 dreamTint = mix(deepTeal, clearCyan, skyLight);
        dreamTint = mix(dreamTint, biomeTint, 0.24);
        float tLuma = luminance(tex);
        albedo.rgb = mix(dreamTint, tex * dreamTint * 2.10, 0.34);
        albedo.rgb += vec3(0.025, 0.060, 0.075) * smoothstep(0.40, 0.82, tLuma);
        albedo.rgb += vec3(0.12, 0.18, 0.20) * pow(smoothstep(0.70, 0.96, tLuma), 2.0);
        albedo.a = WATER_OPACITY;
"""
    water = replace_once(water, original_water_color, dream_water_color, "water material color")
    water_path.write_text(water, encoding="utf-8")
    changes.append("reworked actual water material to teal-cyan glass water")

    preset = {
        "name": "Shader Lab Dreamscape 0.5",
        "architecture": "Iris world shader programs; no screen-space material guessing",
        "water": ["refraction", "waves", "foam", "caustics", "SSR", "teal-cyan depth tint"],
        "terrain": ["PBR mode 2", "auto normals 1.5", "SSAO 1.25", "soft shadows 1.5"],
        "atmosphere": ["real-sky aurora curtains", "world-space sea-level mist", "volumetric light 24 samples"],
        "camera": ["FXAA", "auto exposure", "low bloom", "no vignette"],
    }
    (staged / "SHADERLAB_DREAMSCAPE_PRESET.json").write_text(
        json.dumps(preset, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
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

    dreamscape_changes = apply_dreamscape(staged)

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
        "dreamscape_derivative_changes": dreamscape_changes,
    }
    (staged / "MODRINTH_LICENSE_EVIDENCE.json").write_text(
        json.dumps(evidence, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    (staged / "SHADERLAB_ATTRIBUTION.md").write_text(
        "# Shader Lab Dreamscape attribution\n\n"
        "This private test bundle is a modified derivative of Sarp Shaders 1.0.0.\n\n"
        "- Original creator: xsoras / Sarp\n"
        "- Project: https://modrinth.com/shader/sarp\n"
        f"- Modrinth version ID: {args.version}\n"
        f"- Original filename: {selected.get('filename')}\n"
        "- Original project license: MIT\n"
        f"- License notice source: {license_source}\n"
        "- Shader Lab changes: water preset, terrain material strength, real-sky aurora, world-space low mist, camera tuning\n"
        "- Shader Lab does not claim authorship of the original Sarp renderer\n",
        encoding="utf-8",
    )

    all_files = [path for path in staged.rglob("*") if path.is_file()]
    all_names = sorted(normalized(path, staged) for path in all_files)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    if args.output.exists():
        args.output.unlink()
    with zipfile.ZipFile(args.output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as output_zip:
        for path in sorted(all_files):
            output_zip.write(path, normalized(path, staged))

    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(
        "Sarp source and Dreamscape patch audit: PASS\n"
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
        f"Original feature tokens: {found_features}\n"
        "Dreamscape architecture: real Iris water/terrain/sky/fog programs; no material-guessing screen overlay\n"
        "Dreamscape changes:\n- " + "\n- ".join(dreamscape_changes) + "\n"
        f"Original file count: {len(original_names)}\n"
        f"Final file count: {len(all_names)}\n\n"
        "Original file list:\n" + "\n".join(original_names) + "\n",
        encoding="utf-8",
    )

    print(args.report.read_text("utf-8"), end="")


if __name__ == "__main__":
    main()

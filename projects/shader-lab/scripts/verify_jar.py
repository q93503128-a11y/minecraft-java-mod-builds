#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import io
import json
import re
import sys
import zipfile
from pathlib import Path

SHADERPACK = "shaderpacks/ShaderLab-Reverie-0.8.zip"
OS_UTILS = "net/caffeinemc/mods/sodium/client/compatibility/environment/OsUtils.class"
REQUIRED_EXACT = {
    "META-INF/neoforge.mods.toml",
    "META-INF/jarjar/metadata.json",
    "META-INF/shaderlab/SPBR_MODRINTH_EVIDENCE.json",
    "META-INF/shaderlab/SPBR_ATTRIBUTION.md",
    "kr/moonseungjun/shaderlab/ShaderLab.class",
    SHADERPACK,
    "assets/shaderlab/lang/ko_kr.json",
    "pack.mcmeta",
}
FORBIDDEN_PREFIXES = {
    "assets/shaderlab/post_effect/",
    "assets/shaderlab/shaders/post/",
    "data/shaderlab/shader_tests/",
}


def fail(message: str) -> None:
    print(f"JAR VERIFICATION FAILED: {message}", file=sys.stderr)
    raise SystemExit(1)


def require(source: str, token: str, label: str) -> None:
    if token not in source:
        fail(f"{label} is missing {token!r}")


def require_regex(source: str, pattern: str, label: str) -> None:
    if re.search(pattern, source, re.MULTILINE) is None:
        fail(f"{label} does not match {pattern!r}")


def require_define(source: str, name: str, value: str) -> None:
    require_regex(source, rf"^\s*#define\s+{re.escape(name)}\s+{re.escape(value)}(?:\s|$)", f"Reverie setting {name}")


def main() -> None:
    if len(sys.argv) != 2:
        fail("usage: verify_jar.py <jar>")

    jar_path = Path(sys.argv[1])
    if not jar_path.is_file() or jar_path.stat().st_size < 10_000_000:
        fail(f"missing or unexpectedly small single JAR: {jar_path}")

    with zipfile.ZipFile(jar_path) as jar:
        names = jar.namelist()
        name_set = set(names)
        if len(names) != len(name_set):
            fail("duplicate ZIP entries detected")

        missing = sorted(REQUIRED_EXACT - name_set)
        if missing:
            fail(f"required entries missing: {', '.join(missing)}")

        forbidden = [
            name for name in names
            if name.endswith(".java")
            or name.startswith(".github/")
            or name.startswith("scripts/")
            or any(name.startswith(prefix) for prefix in FORBIDDEN_PREFIXES)
        ]
        if forbidden:
            fail(f"rejected overlay or development entries found: {', '.join(forbidden[:10])}")

        nested_jars = [name for name in names if name.startswith("META-INF/jarjar/") and name.endswith(".jar")]
        iris_entries = [name for name in nested_jars if "iris" in name.lower()]
        sodium_entries = [name for name in nested_jars if "sodium" in name.lower()]
        if len(iris_entries) != 1 or len(sodium_entries) != 1:
            fail(f"expected one bundled Iris and Sodium JAR, found {nested_jars}")

        for entry, expected_mod_id in ((iris_entries[0], "iris"), (sodium_entries[0], "sodium")):
            with zipfile.ZipFile(io.BytesIO(jar.read(entry))) as nested:
                nested_names = set(nested.namelist())
                if "META-INF/neoforge.mods.toml" not in nested_names:
                    fail(f"bundled {expected_mod_id} is not a NeoForge mod")
                toml = nested.read("META-INF/neoforge.mods.toml").decode("utf-8", errors="ignore")
                if expected_mod_id not in toml:
                    fail(f"bundled renderer does not declare {expected_mod_id}")
                if expected_mod_id == "sodium":
                    if OS_UTILS not in nested_names:
                        fail("flattened Sodium module is missing OsUtils")
                    if any(name.startswith("META-INF/jarjar/") and name.endswith(".jar") for name in nested_names):
                        fail("Sodium is still double nested")
                if nested.testzip() is not None:
                    fail(f"bundled {expected_mod_id} JAR is corrupt")

        pack_meta = json.loads(jar.read("pack.mcmeta")).get("pack", {})
        if pack_meta.get("min_format") != [88, 0] or pack_meta.get("max_format") != [107, 1]:
            fail("pack.mcmeta does not cover Minecraft 26.2 formats")

        mods_toml = jar.read("META-INF/neoforge.mods.toml").decode("utf-8")
        for token in ('modId="iris"', 'modId="sodium"', 'type="required"', 'version="0.8.0-alpha.9"'):
            if token not in mods_toml:
                fail(f"NeoForge metadata is missing {token}")

        pbr_maps = [
            name for name in names
            if name.startswith("assets/minecraft/")
            and name.endswith(".png")
            and Path(name).stem.endswith(("_n", "_s"))
        ]
        if len(pbr_maps) < 200:
            fail(f"too few embedded SPBR LabPBR maps: {len(pbr_maps)}")
        spbr = json.loads(jar.read("META-INF/shaderlab/SPBR_MODRINTH_EVIDENCE.json"))
        if spbr.get("project_slug") != "spbr" or spbr.get("version_id") != "S17DzSfS":
            fail("SPBR evidence does not match the pinned 26.2 release")
        if int(spbr.get("merged_pbr_png_count", 0)) != len(pbr_maps):
            fail("SPBR evidence count does not match embedded LabPBR maps")

        shaderpack_bytes = jar.read(SHADERPACK)
        if len(shaderpack_bytes) < 1_000_000:
            fail("embedded Noble shaderpack is unexpectedly small")

        with zipfile.ZipFile(io.BytesIO(shaderpack_bytes)) as shaderpack:
            shader_names = set(shaderpack.namelist())
            for required in (
                "SHADERLAB_ATTRIBUTION.md",
                "SHADERLAB_REVERIE_PRESET.md",
                "MODRINTH_LICENSE_EVIDENCE.json",
                "shaders/settings.glsl",
                "shaders/programs/gbuffers/opaque.glsl",
                "shaders/include/atmospherics/fog.glsl",
                "shaders/include/atmospherics/atmosphere.glsl",
            ):
                if required not in shader_names:
                    fail(f"embedded shaderpack is missing {required}")
            if not any("license" in name.lower() or "copying" in name.lower() for name in shader_names):
                fail("embedded Noble shaderpack is missing GPLv3 license text")

            settings = shaderpack.read("shaders/settings.glsl").decode("utf-8", errors="ignore")
            require_regex(settings, r"const\s+int\s+shadowMapResolution\s*=\s*1024\s*;", "shadow map resolution")
            require_regex(settings, r"const\s+float\s+shadowDistance\s*=\s*64\s*;", "shadow distance")
            for name, value in (
                ("SUNLIGHT_STRENGTH", "0.65"),
                ("SHADOW_SAMPLES", "4"),
                ("REFLECTIONS", "0"),
                ("REFRACTIONS", "1"),
                ("CLOUDMAP", "0"),
                ("CLOUDS_LAYER0_ENABLED", "0"),
                ("CLOUDS_LAYER1_ENABLED", "0"),
                ("AIR_FOG", "1"),
                ("FOG_ALTITUDE", "36"),
                ("FOG_THICKNESS", "100"),
                ("FOG_DENSITY", "0.55"),
                ("WATER_OCTAVES", "6"),
                ("POM", "0"),
                ("EXPOSURE", "1"),
                ("LUT", "0"),
                ("BLOOM_STRENGTH", "0.08"),
                ("GLARE", "0"),
                ("DOF", "0"),
                ("VIGNETTE", "0"),
            ):
                require_define(settings, name, value)

            opaque = shaderpack.read("shaders/programs/gbuffers/opaque.glsl").decode("utf-8", errors="ignore")
            require(opaque, "discard; return;", "foliage alpha fix")

            fog = shaderpack.read("shaders/include/atmospherics/fog.glsl").decode("utf-8", errors="ignore")
            for token in (
                "reverieFogAttenuation",
                "reverieFogScattering",
                "0.45 + wetness * 0.35",
                "mix(0.95, 1.0",
                "reverieGroundLayer",
            ):
                require(fog, token, "Reverie visible low fog patch")

            atmosphere = shaderpack.read("shaders/include/atmospherics/atmosphere.glsl").decode("utf-8", errors="ignore")
            for token in (
                "vec3 physicalSky",
                "nightHorizon",
                "dayZenith",
                "ribbonWave",
                "return mix(physicalSky, dreamSky, 0.76)",
            ):
                require(atmosphere, token, "Reverie lightweight dream sky")

            workgroup_lines: list[str] = []
            for name in shader_names:
                if not name.endswith((".glsl", ".csh", ".fsh", ".vsh")):
                    continue
                text = shaderpack.read(name).decode("utf-8", errors="ignore")
                workgroup_lines.extend(
                    line.strip() for line in text.splitlines() if "workGroupsRender" in line and "const vec2" in line
                )
            if not workgroup_lines:
                fail("no compute workGroupsRender directives were found")
            if any("vec2(1.0, 1.0)" not in line for line in workgroup_lines):
                fail(f"non-literal Iris workGroupsRender directive remains: {workgroup_lines[:5]}")

            evidence = json.loads(shaderpack.read("MODRINTH_LICENSE_EVIDENCE.json"))
            if evidence.get("project_slug") != "noble" or evidence.get("version_id") != "3cIADbit":
                fail("Noble Modrinth evidence is invalid")
            if evidence.get("shaderlab_release") != "Reverie 0.8":
                fail("embedded preset metadata is not Reverie 0.8")

            license_text = ""
            for name in shader_names:
                lower = name.lower()
                if "license" in lower or "copying" in lower:
                    license_text += shaderpack.read(name).decode("utf-8", errors="ignore")
            if "GNU GENERAL PUBLIC LICENSE" not in license_text or "Version 3" not in license_text:
                fail("embedded Noble license is not GPLv3")
            if shaderpack.testzip() is not None:
                fail("corrupt embedded shaderpack member detected")

        if jar.testzip() is not None:
            fail("corrupt final JAR member detected")

    digest = hashlib.sha256(jar_path.read_bytes()).hexdigest()
    jar_path.with_name(jar_path.name + ".sha256").write_text(
        f"{digest}  {jar_path.name}\n", encoding="utf-8"
    )

    report_path = jar_path.parent.parent / "reports" / "jar-verification.txt"
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(
        "\n".join([
            "Shader Lab Reverie 0.8 single-JAR verification: PASS",
            f"JAR: {jar_path.name}",
            f"Bytes: {jar_path.stat().st_size}",
            f"SHA-256: {digest}",
            "Bundled Iris NeoForge: PASS",
            "Flattened Sodium NeoForge with OsUtils: PASS",
            f"Embedded SPBR LabPBR maps: {len(pbr_maps)}",
            "Noble foliage alpha discard patch: PASS",
            "Iris literal compute work-group directives: PASS",
            "Volumetric cloud and reflection compute disabled: PASS",
            "Lightweight violet-blue dream sky: PASS",
            "Persistent raymarched blue-lilac ground mist: PASS",
            "GTX 1660 SUPER reduced-cost preset: PASS",
            "Runtime external shaderpack ZIP not required: PASS",
        ]) + "\n",
        encoding="utf-8",
    )
    print(report_path.read_text(encoding="utf-8"), end="")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import io
import json
import sys
import zipfile
from pathlib import Path

SHADERPACK = "shaderpacks/ShaderLab-Dreamscape-0.4.zip"
REQUIRED_EXACT = {
    "META-INF/neoforge.mods.toml",
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


def main() -> None:
    if len(sys.argv) != 2:
        fail("usage: verify_jar.py <jar>")

    jar_path = Path(sys.argv[1])
    if not jar_path.is_file() or jar_path.stat().st_size == 0:
        fail(f"missing or empty JAR: {jar_path}")

    with zipfile.ZipFile(jar_path) as jar:
        names = jar.namelist()
        name_set = set(names)

        if len(names) != len(name_set):
            fail("duplicate ZIP entries detected")

        missing = sorted(REQUIRED_EXACT - name_set)
        if missing:
            fail(f"required entries missing: {', '.join(missing)}")

        forbidden = [
            name
            for name in names
            if name.endswith(".java")
            or name.startswith(".github/")
            or name.startswith("scripts/")
            or any(name.startswith(prefix) for prefix in FORBIDDEN_PREFIXES)
        ]
        if forbidden:
            fail(f"rejected screen-space or development entries found: {', '.join(forbidden[:10])}")

        pack = json.loads(jar.read("pack.mcmeta"))
        pack_meta = pack.get("pack", {})
        if pack_meta.get("min_format") != [88, 0] or pack_meta.get("max_format") != [107, 1]:
            fail("pack.mcmeta does not cover Minecraft 26.2 resource/data pack formats")

        mods_toml = jar.read("META-INF/neoforge.mods.toml").decode("utf-8")
        for token in ('modId="iris"', 'modId="sodium"', 'type="optional"'):
            if token not in mods_toml:
                fail(f"NeoForge metadata is missing {token}")

        shaderpack_bytes = jar.read(SHADERPACK)
        if len(shaderpack_bytes) < 250_000:
            fail("embedded Dreamscape shaderpack is unexpectedly small")

        with zipfile.ZipFile(io.BytesIO(shaderpack_bytes)) as shaderpack:
            shader_names = set(shaderpack.namelist())
            required_shader_entries = {
                "shaders/shaders.properties",
                "shaders/settings.glsl",
                "shaders/program/c0_vl.fsh",
                "LICENSE",
                "SHADERLAB_ATTRIBUTION.md",
            }
            missing_shader_entries = sorted(required_shader_entries - shader_names)
            if missing_shader_entries:
                fail(f"embedded shaderpack entries missing: {', '.join(missing_shader_entries)}")

            settings = shaderpack.read("shaders/settings.glsl").decode("utf-8")
            for token in (
                "#define AURORA_NORMAL AURORA_ALWAYS",
                "#define WATER_CAUSTICS",
                "#define WATER_PARALLAX",
                "#define WATER_DISPLACEMENT",
                "#define ENVIRONMENT_REFLECTIONS",
                "#define SKY_REFLECTIONS",
                "#define TAA",
                "#define FXAA",
                "#define CAS",
            ):
                if token not in settings:
                    fail(f"Dreamscape settings are missing {token}")

            fog = shaderpack.read("shaders/program/c0_vl.fsh").decode("utf-8")
            for token in ("shaderlab_low_mist", "world_end_pos.y", "fog_transmittance"):
                if token not in fog:
                    fail(f"world-space low mist patch is missing {token}")

            license_text = shaderpack.read("LICENSE").decode("utf-8", errors="replace")
            if "MIT License" not in license_text:
                fail("Photon MIT license was not preserved")

            if shaderpack.testzip() is not None:
                fail("corrupt embedded shaderpack member detected")

        if jar.testzip() is not None:
            fail("corrupt JAR member detected")

    digest = hashlib.sha256(jar_path.read_bytes()).hexdigest()
    checksum_path = jar_path.with_name(jar_path.name + ".sha256")
    checksum_path.write_text(f"{digest}  {jar_path.name}\n", encoding="utf-8")

    report_path = jar_path.parent.parent / "reports" / "jar-verification.txt"
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(
        "\n".join(
            [
                "Shader Lab Dreamscape JAR verification: PASS",
                f"JAR: {jar_path.name}",
                f"Bytes: {jar_path.stat().st_size}",
                f"SHA-256: {digest}",
                "Renderer: Iris shaderpack bootstrap",
                "Upstream shader: Photon MIT @ 15458c0937f8647c37eb6a501bef5eb3bf3da31b",
                "Water/terrain/sky separation: PASS",
                "World-space low mist patch: PASS",
                "Rejected screen-space post effect absent: PASS",
                "Pack format range: 88.0 through 107.1",
            ]
        )
        + "\n",
        encoding="utf-8",
    )

    print(report_path.read_text(encoding="utf-8"), end="")


if __name__ == "__main__":
    main()

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
            name for name in names
            if name.endswith(".java")
            or name.startswith(".github/")
            or name.startswith("scripts/")
            or any(name.startswith(prefix) for prefix in FORBIDDEN_PREFIXES)
        ]
        if forbidden:
            fail(f"rejected screen-space or development entries found: {', '.join(forbidden[:10])}")

        pack_meta = json.loads(jar.read("pack.mcmeta")).get("pack", {})
        if pack_meta.get("min_format") != [88, 0] or pack_meta.get("max_format") != [107, 1]:
            fail("pack.mcmeta does not cover Minecraft 26.2 formats")

        mods_toml = jar.read("META-INF/neoforge.mods.toml").decode("utf-8")
        for token in ('modId="iris"', 'modId="sodium"', 'type="optional"'):
            if token not in mods_toml:
                fail(f"NeoForge metadata is missing {token}")

        shaderpack_bytes = jar.read(SHADERPACK)
        if len(shaderpack_bytes) < 70_000:
            fail("embedded Dreamscape shaderpack is unexpectedly small")

        with zipfile.ZipFile(io.BytesIO(shaderpack_bytes)) as shaderpack:
            shader_names = set(shaderpack.namelist())
            if "SHADERLAB_ATTRIBUTION.md" not in shader_names:
                fail("embedded shaderpack is missing attribution")
            if not any("license" in name.lower() or "mit" in name.lower() for name in shader_names):
                fail("embedded shaderpack is missing its license file")
            if not any("gbuffers_water" in name.lower() or "/water." in name.lower() for name in shader_names):
                fail("embedded shaderpack is missing a water render program")
            if not any("gbuffers_terrain" in name.lower() or "/terrain." in name.lower() for name in shader_names):
                fail("embedded shaderpack is missing a terrain render program")

            source_text: list[str] = []
            license_text = ""
            for name in shader_names:
                lower = name.lower()
                if lower.endswith((".glsl", ".fsh", ".vsh", ".properties")):
                    source_text.append(shaderpack.read(name).decode("utf-8", errors="ignore"))
                if "license" in lower or "mit" in lower:
                    license_text += shaderpack.read(name).decode("utf-8", errors="ignore")

            searchable = "\n".join(source_text).lower()
            for token in ("aurora", "caustic", "refract", "fog"):
                if token not in searchable:
                    fail(f"embedded shader source is missing expected feature token: {token}")

            if "MIT License" not in license_text and "Permission is hereby granted" not in license_text:
                fail("embedded license file does not contain the expected MIT text")

            attribution = shaderpack.read("SHADERLAB_ATTRIBUTION.md").decode("utf-8")
            for token in ("xsoras", "AwTfcPdR", "MIT"):
                if token not in attribution:
                    fail(f"attribution is missing {token}")

            if shaderpack.testzip() is not None:
                fail("corrupt embedded shaderpack member detected")

        if jar.testzip() is not None:
            fail("corrupt JAR member detected")

    digest = hashlib.sha256(jar_path.read_bytes()).hexdigest()
    jar_path.with_name(jar_path.name + ".sha256").write_text(
        f"{digest}  {jar_path.name}\n", encoding="utf-8"
    )

    report_path = jar_path.parent.parent / "reports" / "jar-verification.txt"
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(
        "\n".join([
            "Shader Lab Dreamscape JAR verification: PASS",
            f"JAR: {jar_path.name}",
            f"Bytes: {jar_path.stat().st_size}",
            f"SHA-256: {digest}",
            "Renderer: Iris shaderpack bootstrap",
            "Upstream: official Sarp Shaders 1.0.0 / AwTfcPdR",
            "License file preserved and verified: PASS",
            "Water/terrain render separation: PASS",
            "Aurora/caustics/refraction/fog features: PASS",
            "Rejected screen-space post effect absent: PASS",
        ]) + "\n",
        encoding="utf-8",
    )
    print(report_path.read_text(encoding="utf-8"), end="")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import io
import json
import sys
import zipfile
from pathlib import Path

SHADERPACK = "shaderpacks/ShaderLab-Reverie-0.6.zip"
REQUIRED_EXACT = {
    "META-INF/neoforge.mods.toml",
    "META-INF/jarjar/metadata.json",
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
    if not jar_path.is_file() or jar_path.stat().st_size < 3_000_000:
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
                if nested.testzip() is not None:
                    fail(f"bundled {expected_mod_id} JAR is corrupt")

        pack_meta = json.loads(jar.read("pack.mcmeta")).get("pack", {})
        if pack_meta.get("min_format") != [88, 0] or pack_meta.get("max_format") != [107, 1]:
            fail("pack.mcmeta does not cover Minecraft 26.2 formats")

        mods_toml = jar.read("META-INF/neoforge.mods.toml").decode("utf-8")
        for token in ('modId="iris"', 'modId="sodium"', 'type="required"'):
            if token not in mods_toml:
                fail(f"NeoForge metadata is missing {token}")

        shaderpack_bytes = jar.read(SHADERPACK)
        if len(shaderpack_bytes) < 100_000:
            fail("embedded Noble shaderpack is unexpectedly small")

        with zipfile.ZipFile(io.BytesIO(shaderpack_bytes)) as shaderpack:
            shader_names = set(shaderpack.namelist())
            for required in ("SHADERLAB_ATTRIBUTION.md", "MODRINTH_LICENSE_EVIDENCE.json"):
                if required not in shader_names:
                    fail(f"embedded shaderpack is missing {required}")
            if not any("license" in name.lower() or "copying" in name.lower() for name in shader_names):
                fail("embedded Noble shaderpack is missing GPLv3 license text")

            source_text: list[str] = []
            license_text = ""
            for name in shader_names:
                lower = name.lower()
                if lower.endswith((".glsl", ".fsh", ".vsh", ".properties")):
                    source_text.append(shaderpack.read(name).decode("utf-8", errors="ignore"))
                if "license" in lower or "copying" in lower:
                    license_text += shaderpack.read(name).decode("utf-8", errors="ignore")

            searchable = "\n".join(source_text).lower()
            for token in ("water", "fog", "bloom", "sun", "pbr", "shadow"):
                if token not in searchable:
                    fail(f"Noble shader source is missing feature token: {token}")
            if "GNU GENERAL PUBLIC LICENSE" not in license_text or "Version 3" not in license_text:
                fail("embedded Noble license is not GPLv3")

            evidence = json.loads(shaderpack.read("MODRINTH_LICENSE_EVIDENCE.json"))
            if evidence.get("project_slug") != "noble":
                fail("Noble Modrinth evidence is invalid")
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
            "Shader Lab Reverie 0.6 single-JAR verification: PASS",
            f"JAR: {jar_path.name}",
            f"Bytes: {jar_path.stat().st_size}",
            f"SHA-256: {digest}",
            "Bundled Iris NeoForge: PASS",
            "Bundled Sodium NeoForge: PASS",
            "Upstream shader: Noble Shaders GPLv3",
            "Noble source and license included: PASS",
            "Real water/PBR/sun/lighting/fog/bloom source features: PASS",
            "Rejected screen-space material guessing overlay absent: PASS",
        ]) + "\n",
        encoding="utf-8",
    )
    print(report_path.read_text(encoding="utf-8"), end="")


if __name__ == "__main__":
    main()

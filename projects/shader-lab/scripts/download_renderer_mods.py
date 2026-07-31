#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import io
import json
import urllib.request
import zipfile
from pathlib import Path
from typing import Any

USER_AGENT = "ShaderLab-single-jar/0.6.1 (github.com/q93503128-a11y/minecraft-java-mod-builds)"


def request_bytes(url: str) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=180) as response:
        return response.read()


def request_json(url: str) -> dict[str, Any]:
    return json.loads(request_bytes(url).decode("utf-8"))


def validate_mod_jar(data: bytes, expected_mod_id: str, label: str) -> None:
    with zipfile.ZipFile(io.BytesIO(data)) as jar:
        names = set(jar.namelist())
        if "META-INF/neoforge.mods.toml" not in names:
            raise RuntimeError(f"{label} is not a NeoForge mod JAR")
        mods_toml = jar.read("META-INF/neoforge.mods.toml").decode("utf-8", errors="ignore")
        if f'modId="{expected_mod_id}"' not in mods_toml and f'modId = "{expected_mod_id}"' not in mods_toml:
            raise RuntimeError(f"{label} does not declare mod id {expected_mod_id}")
        bad_member = jar.testzip()
        if bad_member:
            raise RuntimeError(f"Corrupt member in {label}: {bad_member}")


def download(version_id: str, expected_mod_id: str, output: Path) -> tuple[dict[str, Any], bytes]:
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
    validate_mod_jar(data, expected_mod_id, output.name)

    return ({
        "mod_id": expected_mod_id,
        "project": project.get("slug"),
        "project_title": project.get("title"),
        "project_license": project.get("license"),
        "version_id": version_id,
        "version_number": version.get("version_number"),
        "filename": selected.get("filename"),
        "downloaded_filename": output.name,
        "bytes": len(data),
        "sha512": actual_sha512,
    }, data)


def is_signature(name: str) -> bool:
    upper = name.upper()
    return upper.startswith("META-INF/") and upper.endswith((".SF", ".RSA", ".DSA", ".EC"))


def flatten_sodium_wrapper(wrapper_data: bytes, output: Path) -> dict[str, Any]:
    """Merge Sodium's bootstrap wrapper and nested mod into one NeoForge mod JAR.

    The official 26.2 Sodium download is a bootstrap library containing the real
    Sodium mod as Jar-in-Jar. Nesting that wrapper inside Shader Lab produces a
    second Jar-in-Jar layer: NeoForge loads the inner mod, but helper classes such
    as OsUtils remain in the skipped wrapper module. Flattening keeps all official
    classes together in the same Sodium module and removes only the redundant
    nested-container layer.
    """

    with zipfile.ZipFile(io.BytesIO(wrapper_data)) as wrapper:
        wrapper_names = set(wrapper.namelist())
        inner_candidates = [
            name for name in wrapper_names
            if name.startswith("META-INF/jarjar/")
            and name.endswith(".jar")
            and "sodium-neoforge" in name.lower()
        ]
        if len(inner_candidates) != 1:
            raise RuntimeError(f"Expected one nested Sodium mod JAR, found {inner_candidates}")
        inner_name = inner_candidates[0]
        inner_data = wrapper.read(inner_name)

        with zipfile.ZipFile(io.BytesIO(inner_data)) as inner:
            if inner.testzip() is not None:
                raise RuntimeError("Official nested Sodium mod JAR is corrupt")

            entries: dict[str, bytes] = {}

            # The nested mod owns the mod metadata and mixin resources.
            for name in inner.namelist():
                if name.endswith("/") or name == "META-INF/MANIFEST.MF" or is_signature(name):
                    continue
                entries[name] = inner.read(name)

            # Add bootstrap/helper classes and the early-window service from the wrapper.
            for name in wrapper.namelist():
                if name.endswith("/") or name == "META-INF/MANIFEST.MF" or is_signature(name):
                    continue
                if name.startswith("META-INF/jarjar/"):
                    continue
                if name == "META-INF/neoforge.mods.toml":
                    continue
                entries.setdefault(name, wrapper.read(name))

    required = {
        "META-INF/neoforge.mods.toml",
        "net/caffeinemc/mods/sodium/client/compatibility/environment/OsUtils.class",
        "META-INF/services/net.neoforged.neoforgespi.earlywindow.GraphicsBootstrapper",
    }
    missing = sorted(required - set(entries))
    if missing:
        raise RuntimeError(f"Flattened Sodium is missing required entries: {missing}")
    if any(name.startswith("META-INF/jarjar/") and name.endswith(".jar") for name in entries):
        raise RuntimeError("Flattened Sodium still contains a nested mod JAR")

    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as flattened:
        flattened.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\r\n\r\n")
        for name in sorted(entries):
            flattened.writestr(name, entries[name])

    flattened_data = output.read_bytes()
    validate_mod_jar(flattened_data, "sodium", output.name)
    with zipfile.ZipFile(output) as jar:
        names = set(jar.namelist())
        if "net/caffeinemc/mods/sodium/client/compatibility/environment/OsUtils.class" not in names:
            raise RuntimeError("Flattened Sodium lost OsUtils")
        if any(name.startswith("META-INF/jarjar/") and name.endswith(".jar") for name in names):
            raise RuntimeError("Flattened Sodium contains an unexpected nested JAR")

    return {
        "flattened_filename": output.name,
        "flattened_bytes": len(flattened_data),
        "flattened_sha512": hashlib.sha512(flattened_data).hexdigest(),
        "flattened_reason": "remove unsupported double Jar-in-Jar layer while preserving official Sodium classes",
        "osutils_in_same_module": True,
        "nested_sodium_mod_removed": True,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--iris-version", required=True)
    parser.add_argument("--sodium-version", required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()

    iris_record, _ = download(
        args.iris_version,
        "iris",
        args.output_dir / "iris-1.11.2+26.2-neoforge.jar",
    )
    sodium_record, sodium_wrapper = download(
        args.sodium_version,
        "sodium",
        args.output_dir / "sodium-0.9.1+26.2-wrapper.jar",
    )
    sodium_record.update(flatten_sodium_wrapper(
        sodium_wrapper,
        args.output_dir / "sodium-0.9.1+26.2-flattened.jar",
    ))

    records = [iris_record, sodium_record]
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(records, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(args.report.read_text("utf-8"), end="")


if __name__ == "__main__":
    main()

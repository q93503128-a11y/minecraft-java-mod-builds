#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import io
import os
import sys
import zipfile
from pathlib import Path

OS_UTILS = "net/caffeinemc/mods/sodium/client/compatibility/environment/OsUtils.class"
GRAPHICS_BOOTSTRAPPER = "META-INF/services/net.neoforged.neoforgespi.earlywindow.GraphicsBootstrapper"


def fail(message: str) -> None:
    print(f"SODIUM FLATTEN FAILED: {message}", file=sys.stderr)
    raise SystemExit(1)


def is_signature(name: str) -> bool:
    upper = name.upper()
    return upper.startswith("META-INF/") and upper.endswith((".SF", ".RSA", ".DSA", ".EC"))


def flatten_sodium(wrapper_data: bytes) -> bytes:
    with zipfile.ZipFile(io.BytesIO(wrapper_data)) as wrapper:
        wrapper_names = set(wrapper.namelist())
        nested_mods = [
            name for name in wrapper_names
            if name.startswith("META-INF/jarjar/")
            and name.endswith(".jar")
            and "sodium-neoforge" in name.lower()
        ]

        if not nested_mods:
            if OS_UTILS in wrapper_names:
                return wrapper_data
            fail("Sodium JAR has neither a nested mod nor OsUtils")
        if len(nested_mods) != 1:
            fail(f"expected one nested Sodium mod, found {nested_mods}")

        inner_data = wrapper.read(nested_mods[0])
        with zipfile.ZipFile(io.BytesIO(inner_data)) as inner:
            if inner.testzip() is not None:
                fail("nested official Sodium mod is corrupt")

            entries: dict[str, bytes] = {}
            manifest = b"Manifest-Version: 1.0\r\n\r\n"
            if "META-INF/MANIFEST.MF" in inner.namelist():
                manifest = inner.read("META-INF/MANIFEST.MF")

            # Keep the actual mod metadata, mixins and implementation.
            for name in inner.namelist():
                if name.endswith("/") or name == "META-INF/MANIFEST.MF" or is_signature(name):
                    continue
                entries[name] = inner.read(name)

            # Add the bootstrap/helper classes that the official wrapper keeps outside
            # the nested mod. These include OsUtils, driver checks and the early-window
            # GraphicsBootstrapper service.
            for name in wrapper.namelist():
                if name.endswith("/") or name == "META-INF/MANIFEST.MF" or is_signature(name):
                    continue
                if name.startswith("META-INF/jarjar/"):
                    continue
                if name == "META-INF/neoforge.mods.toml":
                    continue
                entries.setdefault(name, wrapper.read(name))

    required = {"META-INF/neoforge.mods.toml", OS_UTILS, GRAPHICS_BOOTSTRAPPER}
    missing = sorted(required - set(entries))
    if missing:
        fail(f"flattened Sodium is missing required entries: {missing}")
    if any(name.startswith("META-INF/jarjar/") and name.endswith(".jar") for name in entries):
        fail("flattened Sodium still contains nested JARs")

    output = io.BytesIO()
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as flattened:
        flattened.writestr("META-INF/MANIFEST.MF", manifest)
        for name in sorted(entries):
            flattened.writestr(name, entries[name])

    result = output.getvalue()
    with zipfile.ZipFile(io.BytesIO(result)) as check:
        names = set(check.namelist())
        if OS_UTILS not in names:
            fail("OsUtils is not in the flattened Sodium module")
        if any(name.startswith("META-INF/jarjar/") and name.endswith(".jar") for name in names):
            fail("double Jar-in-Jar remains after flattening")
        if check.testzip() is not None:
            fail("flattened Sodium is corrupt")
    return result


def main() -> None:
    if len(sys.argv) != 2:
        fail("usage: flatten_sodium_in_final_jar.py <shaderlab.jar>")

    jar_path = Path(sys.argv[1])
    if not jar_path.is_file():
        fail(f"missing JAR: {jar_path}")

    original = jar_path.read_bytes()
    original_sha = hashlib.sha256(original).hexdigest()

    with zipfile.ZipFile(io.BytesIO(original)) as jar:
        infos = jar.infolist()
        sodium_entries = [
            info.filename for info in infos
            if info.filename.startswith("META-INF/jarjar/")
            and info.filename.endswith(".jar")
            and "sodium" in info.filename.lower()
        ]
        if len(sodium_entries) != 1:
            fail(f"expected one bundled Sodium entry, found {sodium_entries}")
        sodium_entry = sodium_entries[0]
        flattened_sodium = flatten_sodium(jar.read(sodium_entry))

        temp_path = jar_path.with_suffix(jar_path.suffix + ".tmp")
        with zipfile.ZipFile(temp_path, "w") as output:
            for info in infos:
                data = flattened_sodium if info.filename == sodium_entry else jar.read(info.filename)
                output.writestr(info, data)

    os.replace(temp_path, jar_path)

    with zipfile.ZipFile(jar_path) as verified:
        nested = verified.read(sodium_entry)
        with zipfile.ZipFile(io.BytesIO(nested)) as sodium:
            names = set(sodium.namelist())
            if OS_UTILS not in names:
                fail("final JAR's Sodium module is missing OsUtils")
            if any(name.startswith("META-INF/jarjar/") and name.endswith(".jar") for name in names):
                fail("final JAR still contains double-nested Sodium")
        if verified.testzip() is not None:
            fail("rewritten final JAR is corrupt")

    final_data = jar_path.read_bytes()
    final_sha = hashlib.sha256(final_data).hexdigest()
    print("Sodium single-module flatten: PASS")
    print(f"JAR: {jar_path.name}")
    print(f"Original SHA-256: {original_sha}")
    print(f"Final SHA-256: {final_sha}")
    print(f"Bundled Sodium entry: {sodium_entry}")
    print(f"Flattened Sodium bytes: {len(flattened_sodium)}")
    print("OsUtils in loaded Sodium module: PASS")
    print("Double Jar-in-Jar removed: PASS")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Build a redistribution-safe Modrinth .mrpack from the exact jars already resolved by Gradle."""

from __future__ import annotations

import hashlib
import json
import os
import re
import sys
import zipfile
from pathlib import Path

PROJECT = Path(__file__).resolve().parents[1]
BUILD = PROJECT / "build"
INVENTORY = BUILD / "reports" / "modpack" / "pack-artifact-inventory.txt"
OUT_DIR = BUILD / "distributions"
MOD_VERSION = "0.1.0-alpha.1"
MC_VERSION = "26.2"
LOADER_VERSION = "0.19.5"
FABRIC_API_VERSION = "0.160.0+26.2"
PACK_NAME = "Augmentation Survival"
PACK_VERSION = MOD_VERSION

# These are the exact repository URLs implied by the coordinates in build.gradle.
DOWNLOADS: dict[str, str] = {
    "8oi3bsk5-OxfI2n80.jar": "https://api.modrinth.com/maven/maven/modrinth/8oi3bsk5/OxfI2n80/8oi3bsk5-OxfI2n80.jar",
    "XaDC71GB-sGGUpIGT.jar": "https://api.modrinth.com/maven/maven/modrinth/XaDC71GB/sGGUpIGT/XaDC71GB-sGGUpIGT.jar",
    "muf0XoRe-7.7.6+26.2-fabric.jar": "https://api.modrinth.com/maven/maven/modrinth/muf0XoRe/7.7.6+26.2-fabric/muf0XoRe-7.7.6+26.2-fabric.jar",
    "tpehi7ww-y9AUViOD.jar": "https://api.modrinth.com/maven/maven/modrinth/tpehi7ww/y9AUViOD/tpehi7ww-y9AUViOD.jar",
    "POQ2i9zu-rJBCX3gG.jar": "https://api.modrinth.com/maven/maven/modrinth/POQ2i9zu/rJBCX3gG/POQ2i9zu-rJBCX3gG.jar",
    "jSV9w0J5-1EML9Osa.jar": "https://api.modrinth.com/maven/maven/modrinth/jSV9w0J5/1EML9Osa/jSV9w0J5-1EML9Osa.jar",
    "F8BQNPWX-zrGHo70e.jar": "https://api.modrinth.com/maven/maven/modrinth/F8BQNPWX/zrGHo70e/F8BQNPWX-zrGHo70e.jar",
    "7vxePowz-tLeFs9pK.jar": "https://api.modrinth.com/maven/maven/modrinth/7vxePowz/tLeFs9pK/7vxePowz-tLeFs9pK.jar",
    "5sy6g3kz-enlZuzkJ.jar": "https://api.modrinth.com/maven/maven/modrinth/5sy6g3kz/enlZuzkJ/5sy6g3kz-enlZuzkJ.jar",
    "yBW8D80W-XVsaQXSm.jar": "https://api.modrinth.com/maven/maven/modrinth/yBW8D80W/XVsaQXSm/yBW8D80W-XVsaQXSm.jar",
    "chrono-dawn-1414198-9145359.jar": "https://cursemaven.com/curse/maven/chrono-dawn-1414198/9145359/chrono-dawn-1414198-9145359.jar",
    "world-bosses-1545677-9254111.jar": "https://cursemaven.com/curse/maven/world-bosses-1545677/9254111/world-bosses-1545677-9254111.jar",
    "yJtKNlKW-HnEQE9aE.jar": "https://api.modrinth.com/maven/maven/modrinth/yJtKNlKW/HnEQE9aE/yJtKNlKW-HnEQE9aE.jar",
    "G1hIVOrD-SmfwDCLg.jar": "https://api.modrinth.com/maven/maven/modrinth/G1hIVOrD/SmfwDCLg/G1hIVOrD-SmfwDCLg.jar",
    "QAGBst4M-KU5rZUAR.jar": "https://api.modrinth.com/maven/maven/modrinth/QAGBst4M/KU5rZUAR/QAGBst4M-KU5rZUAR.jar",
    "ohNO6lps-rSd3GiG8.jar": "https://api.modrinth.com/maven/maven/modrinth/ohNO6lps/rSd3GiG8/ohNO6lps-rSd3GiG8.jar",
    "codAaoxh-3uBvRFE9.jar": "https://api.modrinth.com/maven/maven/modrinth/codAaoxh/3uBvRFE9/codAaoxh-3uBvRFE9.jar",
    "multiloaderdataextensions-fabric-26.2.0.jar": "https://raw.githubusercontent.com/Fuzss/modresources/main/maven/fuzs/multiloaderdataextensions/multiloaderdataextensions-fabric/26.2.0/multiloaderdataextensions-fabric-26.2.0.jar",
    "8BmcQJ2H-7gaQHok7.jar": "https://api.modrinth.com/maven/maven/modrinth/8BmcQJ2H/7gaQHok7/8BmcQJ2H-7gaQHok7.jar",
    "ha1mEyJS-Hq1CwB9w.jar": "https://api.modrinth.com/maven/maven/modrinth/ha1mEyJS/Hq1CwB9w/ha1mEyJS-Hq1CwB9w.jar",
    "cloth-config-fabric-26.2.155.jar": "https://maven.shedaniel.me/me/shedaniel/cloth/cloth-config-fabric/26.2.155/cloth-config-fabric-26.2.155.jar",
    "lhGA9TYQ-Jmfixwxe.jar": "https://api.modrinth.com/maven/maven/modrinth/lhGA9TYQ/Jmfixwxe/lhGA9TYQ-Jmfixwxe.jar",
    "Ha28R6CL-BKnpByHl.jar": "https://api.modrinth.com/maven/maven/modrinth/Ha28R6CL/BKnpByHl/Ha28R6CL-BKnpByHl.jar",
    f"fabric-api-{FABRIC_API_VERSION}.jar": f"https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/{FABRIC_API_VERSION}/fabric-api-{FABRIC_API_VERSION}.jar",
}

CLIENT_ONLY = {"yBW8D80W-XVsaQXSm.jar"}


def fail(message: str) -> None:
    raise SystemExit(f"MRPACK_VERIFY_FAIL: {message}")


def digest(path: Path, algorithm: str) -> str:
    h = hashlib.new(algorithm)
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def inventory_files() -> list[str]:
    if not INVENTORY.is_file():
        fail(f"missing inventory: {INVENTORY}")
    names: list[str] = []
    for line in INVENTORY.read_text(encoding="utf-8").splitlines():
        match = re.search(r"(?:^|\t)file=([^\t]+)", line)
        if match:
            names.append(match.group(1))
    if len(names) != 23 or len(set(names)) != 23:
        fail(f"expected 23 unique external artifacts, got {len(names)}")
    return sorted(names)


def find_cached(filename: str) -> Path:
    roots = [Path.home() / ".gradle" / "caches"]
    matches: list[Path] = []
    for root in roots:
        if root.exists():
            matches.extend(path for path in root.rglob(filename) if path.is_file())
    if not matches:
        fail(f"resolved artifact not found in Gradle cache: {filename}")
    matches.sort(key=lambda path: path.stat().st_size, reverse=True)
    if matches[0].stat().st_size <= 0:
        fail(f"empty cached artifact: {filename}")
    return matches[0]


def find_own_jar() -> Path:
    candidates = sorted((BUILD / "libs").glob(f"augmentation-survival-{MOD_VERSION}.jar"))
    if len(candidates) != 1:
        fail(f"expected one production jar, got {candidates}")
    return candidates[0]


def build() -> Path:
    external = inventory_files()
    expected_external = set(DOWNLOADS) - {f"fabric-api-{FABRIC_API_VERSION}.jar"}
    if set(external) != expected_external:
        missing = sorted(set(external) - expected_external)
        stale = sorted(expected_external - set(external))
        fail(f"download map drift; missing={missing}, stale={stale}")

    fabric_api_name = f"fabric-api-{FABRIC_API_VERSION}.jar"
    all_names = external + [fabric_api_name]
    files = []
    resolved_paths: dict[str, Path] = {}
    for filename in all_names:
        artifact = find_cached(filename)
        resolved_paths[filename] = artifact
        if not zipfile.is_zipfile(artifact):
            fail(f"artifact is not a valid jar/zip: {filename}")
        env = {"client": "required", "server": "required"}
        if filename in CLIENT_ONLY:
            env = {"client": "required", "server": "unsupported"}
        files.append({
            "path": f"mods/{filename}",
            "hashes": {
                "sha1": digest(artifact, "sha1"),
                "sha512": digest(artifact, "sha512"),
            },
            "env": env,
            "downloads": [DOWNLOADS[filename]],
            "fileSize": artifact.stat().st_size,
        })

    own_jar = find_own_jar()
    if not zipfile.is_zipfile(own_jar):
        fail("production mod jar is invalid")

    index = {
        "formatVersion": 1,
        "game": "minecraft",
        "versionId": PACK_VERSION,
        "name": PACK_NAME,
        "summary": "Pinned Fabric 26.2 Augmentation Survival pack",
        "files": files,
        "dependencies": {
            "minecraft": MC_VERSION,
            "fabric-loader": LOADER_VERSION,
        },
    }

    terralith_config = {
        "config_version": 1,
        "modules": {
            "custom_structures": True,
            "fog_tweaks": True,
            "intro_message": False,
            "recipe_changes": False,
            "skylands": True,
            "terrain_slabs": True,
            "vanilla_stone_gen": False,
        },
    }

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    output = OUT_DIR / f"augmentation-survival-{MOD_VERSION}.mrpack"
    own_arc = f"overrides/mods/{own_jar.name}"
    analytics_arc = "overrides/config/mcendgame.json"
    terralith_arc = "overrides/config/terralith.json"
    readme_arc = "overrides/AUGMENTATION_SURVIVAL_PACK.txt"
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        archive.writestr("modrinth.index.json", json.dumps(index, ensure_ascii=False, indent=2) + "\n")
        archive.write(own_jar, own_arc)
        archive.writestr(analytics_arc, json.dumps({"sendAnalytics": False}, indent=2) + "\n")
        archive.writestr(terralith_arc, json.dumps(terralith_config, indent=2) + "\n")
        archive.writestr(
            readme_arc,
            "Augmentation Survival\n"
            f"Minecraft {MC_VERSION} / Fabric Loader {LOADER_VERSION}\n"
            "External mods are downloaded from their pinned source repositories by the launcher.\n"
            "MCEndgame analytics is disabled by the included config override.\n"
            "Terralith's first-world intro message is disabled by the included config override.\n",
        )

    with zipfile.ZipFile(output) as archive:
        names = archive.namelist()
        if (names.count("modrinth.index.json") != 1 or own_arc not in names or analytics_arc not in names or terralith_arc not in names:
            fail("required mrpack entries are missing")
        third_party_overrides = [
            name for name in names
            if name.startswith("overrides/mods/") and not name.endswith(own_jar.name)
        ]
        if third_party_overrides:
            fail(f"third-party jars were embedded: {third_party_overrides}")
        parsed = json.loads(archive.read("modrinth.index.json"))
        if parsed.get("formatVersion") != 1 or len(parsed.get("files", [])) != 24:
            fail("invalid Modrinth index structure or dependency count")
        if json.loads(archive.read(analytics_arc)).get("sendAnalytics") is not False:
            fail("MCEndgame analytics override is not disabled")
        terralith = json.loads(archive.read(terralith_arc))
        if terralith.get("modules", {}).get("intro_message") is not False:
            fail("Terralith intro message override is not disabled")
        if hashlib.sha256(archive.read(own_arc)).hexdigest() != digest(own_jar, "sha256"):
            fail("embedded project jar hash mismatch")

    sha256 = digest(output, "sha256")
    (OUT_DIR / f"{output.name}.sha256").write_text(f"{sha256}  {output.name}\n", encoding="utf-8")
    report = OUT_DIR / "MRPACK_VERIFICATION.txt"
    report.write_text(
        "AUGMENTATION_SURVIVAL_MRPACK_PASS\n"
        f"file={output.name}\n"
        f"bytes={output.stat().st_size}\n"
        f"sha256={sha256}\n"
        "external_files=24\n"
        "embedded_third_party_jars=0\n"
        f"embedded_project_jar={own_jar.name}\n"
        "mcendgame_sendAnalytics=false\n"
        "terralith_intro_message=false\n",
        encoding="utf-8",
    )
    print(report.read_text(encoding="utf-8"), end="")
    return output


if __name__ == "__main__":
    try:
        build()
    except Exception as exc:
        fail(f"{type(exc).__name__}: {exc}")

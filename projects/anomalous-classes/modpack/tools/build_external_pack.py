#!/usr/bin/env python3
"""Create a rights-safe Modrinth pack and optionally inspect upstream jars.

Only retrieves exactly approved 1.21.1 NeoForge releases from Modrinth.
No mod binaries are committed or placed in the .mrpack archive.
"""
from __future__ import annotations

import argparse
import hashlib
import io
import json
import pathlib
import re
import shutil
import sys
import tempfile
import time
import tomllib
import urllib.error
import urllib.parse
import urllib.request
import zipfile

HERE = pathlib.Path(__file__).resolve().parent.parent
BASE_URL = "https://api.modrinth.com/v2"
HEADERS = {"User-Agent": "AnomalousClassesModpackValidator/0.1 (GitHub: q93503128-a11y/minecraft-java-mod-builds)", "Accept": "application/json"}
CORE_DEPS = {"minecraft", "neoforge", "forge", "java", "fabricloader", "fabric", "minecraftforge"}


def fetch_bytes(url: str) -> bytes:
    parts = urllib.parse.urlsplit(url)
    if parts.scheme != "https":
        raise ValueError("Refusing non-HTTPS resource: " + url)
    for attempt in range(4):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=HEADERS), timeout=100) as f:
                return f.read()
        except (urllib.error.URLError, TimeoutError) as exc:
            if attempt == 3:
                raise RuntimeError(f"Failed to fetch {url}: {exc}") from exc
            time.sleep(2 * (attempt + 1))
    raise AssertionError("unreachable")


def get_json(url: str):
    return json.loads(fetch_bytes(url))


def pick_version(spec: dict, minecraft: str):
    query = urllib.parse.urlencode({
        "loaders": json.dumps(["neoforge"]),
        "game_versions": json.dumps([minecraft]),
        "include_changelog": "false",
    })
    slug = urllib.parse.quote(spec["slug"], safe="")
    versions = get_json(f"{BASE_URL}/project/{slug}/version?{query}")
    matches = [v for v in versions if v.get("version_number") == spec["release"]
               and minecraft in v.get("game_versions", []) and "neoforge" in v.get("loaders", [])]
    pinned_id = spec.get("version_id")
    if pinned_id:
        matches = [v for v in matches if v["id"] == pinned_id]
    if len(matches) != 1:
        samples = ", ".join(v.get("version_number", "?") + ":" + v.get("id", "?") for v in versions[:18])
        raise ValueError(f"{spec['id']}: expected one precise NeoForge {minecraft} version "
                         f"{spec['release']}; found {len(matches)} (candidates: {samples})")
    v = matches[0]
    files = [f for f in v.get("files", []) if f.get("primary", False) and f["filename"].lower().endswith(".jar")]
    if len(files) != 1:
        files = [f for f in v.get("files", []) if f["filename"].lower().endswith(".jar")]
    if len(files) != 1:
        raise ValueError(f"{spec['id']} version {v['id']}: ambiguous/missing release JAR")
    f = files[0]
    url = f["url"]
    if not url.startswith("https://") or urllib.parse.urlsplit(url).hostname not in (
        "cdn.modrinth.com", "cdn-raw.modrinth.com"):
        raise ValueError(f"{spec['id']}: unapproved download host {url}")
    hs = f.get("hashes", {})
    if not re.fullmatch(r"[0-9a-f]{128}", hs.get("sha512", "")):
        raise ValueError(f"{spec['id']}: missing SHA512 in upstream metadata")
    return {
        "id": spec["id"], "slug": spec["slug"], "release": spec["release"], "role": spec["role"],
        "version_id": v["id"], "project_id": v["project_id"], "filename": f["filename"],
        "size": f.get("size"), "url": url, "hashes": hs,
        "dependencies": v.get("dependencies", []),
        "env": spec["env"], "primary": True,
    }


def inspect_jar_metadata(z: zipfile.ZipFile, rec_id: str, *, depth: int = 0) -> dict:
    """Inspect root NeoForge metadata AND declared embedded Jar-in-Jar mods."""
    if depth > 2:
        raise ValueError(f"{rec_id}: unsupported deeply nested Jar-in-Jar")
    if z.testzip() is not None:
        raise ValueError(f"{rec_id}: invalid JAR ZIP")
    names = set(z.namelist())
    metadata = next((p for p in ("META-INF/neoforge.mods.toml", "META-INF/mods.toml") if p in names), None)
    found = []
    if metadata is not None:
        d = tomllib.loads(z.read(metadata).decode("utf-8-sig"))
        ids = [m["modId"] for m in d.get("mods", []) if "modId" in m]
        if not ids:
            raise ValueError(f"{rec_id}: no modId declared by {metadata}")
        required = []
        for owner, deps in d.get("dependencies", {}).items():
            if owner not in ids:
                continue
            for dep in deps:
                if dep.get("type", "required" if dep.get("mandatory", False) else "optional") == "required":
                    required.append(dep.get("modId", ""))
        found.append({"mod_ids": ids, "required": sorted(set(required)), "metadata": metadata})
    jarjar_metadata = "META-INF/jarjar/metadata.json"
    if jarjar_metadata in names:
        descriptor = json.loads(z.read(jarjar_metadata))
        jars = descriptor.get("jars", [])
        if not jars or len(jars) > 8:
            raise ValueError(f"{rec_id}: empty or unreasonably large Jar-in-Jar descriptor")
        for jar in jars:
            nested_name = jar.get("path", "")
            if not (isinstance(nested_name, str) and
                    nested_name.startswith("META-INF/jarjar/") and
                    nested_name.endswith(".jar") and
                    ".." not in nested_name.split("/") and nested_name in names):
                raise ValueError(f"{rec_id}: missing or unsafe Jar-in-Jar resource {nested_name!r}")
            info = z.getinfo(nested_name)
            if info.file_size > 100 * 1024 * 1024:
                raise ValueError(f"{rec_id}: nested JAR exceeds safety limit")
            with zipfile.ZipFile(io.BytesIO(z.read(nested_name))) as nested:
                details = inspect_jar_metadata(nested, rec_id, depth=depth + 1)
                details["metadata"] = nested_name + "!" + details["metadata"]
                found.append(details)
    if not found:
        raise ValueError(f"{rec_id}: no NeoForge mods.toml or declared Jar-in-Jar metadata")
    ids = [mod for details in found for mod in details["mod_ids"]]
    required = sorted({dep for details in found for dep in details["required"]})
    if len(set(ids)) != len(ids):
        raise ValueError(f"{rec_id}: duplicate mod IDs in Jar-in-Jar dependencies")
    return {"mod_ids": ids, "required": required,
            "metadata": ", ".join(details["metadata"] for details in found)}



def download_and_validate(rec: dict, path: pathlib.Path):
    data = fetch_bytes(rec["url"])
    expected_size = rec.get("size")
    if expected_size is not None and len(data) != expected_size:
        raise ValueError(f"{rec['id']}: file length mismatch")
    h512 = hashlib.sha512(data).hexdigest()
    if h512 != rec["hashes"]["sha512"]:
        raise ValueError(f"{rec['id']}: SHA512 mismatch")
    if "sha1" in rec["hashes"] and hashlib.sha1(data).hexdigest() != rec["hashes"]["sha1"]:
        raise ValueError(f"{rec['id']}: SHA1 mismatch")
    path.write_bytes(data)
    with zipfile.ZipFile(path) as z:
        details = inspect_jar_metadata(z, rec["id"])
    return {**details, "sha256": hashlib.sha256(data).hexdigest(), "byte_size": len(data)}


def check_declared_dependencies(resolved: list[dict]):
    project_ids = {r["project_id"] for r in resolved}
    missing = {}
    for rec in resolved:
        deps = []
        for d in rec["dependencies"]:
            if d.get("dependency_type") != "required":
                continue
            if d.get("project_id") and d["project_id"] not in project_ids:
                # Upstream metadata of LambDynamicLights 4.8.9 lists Fabric API
                # even for the NeoForge release. The maintainer's NeoForge
                # installation instructions explicitly say it works by itself:
                # https://github.com/LambdAurora/LambDynamicLights
                # Do not install Fabric API into NeoForge. JAR metadata is
                # still checked in download_and_validate/check_mod_ids.
                if rec["id"] == "lambdynamiclights" and d["project_id"] == "P7dR8mSH":
                    print("UPSTREAM METADATA EXCEPTION (NeoForge): "
                          "LambDynamicLights lists Fabric API; ignoring "
                          "cross-loader Modrinth dependency only.", flush=True)
                    continue
                deps.append({"project_id": d["project_id"], "version_id": d.get("version_id")})
        if deps:
            missing[rec["id"]] = deps
    if missing:
        raise ValueError("Additional required upstream dependencies (resolve before publishing): " +
                         json.dumps(missing, ensure_ascii=False))


def check_mod_ids(inspections: dict):
    present = set(CORE_DEPS)
    for d in inspections.values():
        present.update(d["mod_ids"])
    missing = {name: [dep for dep in info["required"] if dep not in present]
               for name, info in inspections.items()}
    missing = {name: deps for name, deps in missing.items() if deps}
    if missing:
        raise ValueError("Required NeoForge modIds missing from staged pack: " +
                         json.dumps(missing, ensure_ascii=False))


def mrpack_manifest(pack: dict, resolved: list[dict]):
    return {
        "formatVersion": 1,
        "game": "minecraft",
        "versionId": pack["version_id"],
        "name": pack["name"],
        "summary": pack["summary"],
        "files": [{
            "path": "mods/" + r["filename"], "hashes": {k: r["hashes"][k] for k in ("sha1", "sha512") if k in r["hashes"]},
            "env": r["env"], "downloads": [r["url"]], "fileSize": r["size"],
        } for r in resolved],
        "dependencies": {"minecraft": pack["minecraft"], "neoforge": pack["neoforge"]},
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=pathlib.Path, default=pathlib.Path("out/anomalous-classes-pack"))
    parser.add_argument("--download-jars", action="store_true", help="Verify upstream bytes + NeoForge mod metadata")
    parser.add_argument("--server-dir", type=pathlib.Path, help="Copy only server-supported mod jars for ephemeral server test")
    parser.add_argument("--offline-check", action="store_true", help="Check local manifest structure without network")
    args = parser.parse_args()
    if args.server_dir and not args.download_jars:
        parser.error("--server-dir requires --download-jars")
    cfg = json.loads((HERE / "pack-source.json").read_text(encoding="utf-8"))
    selected = cfg["mods"] + cfg["libraries"]
    if cfg["minecraft"] != "1.21.1" or not re.fullmatch(r"21\.1\.\d+", cfg["neoforge"]):
        raise ValueError("Unsupported or unpinned project environment")
    if len(selected) != 15 or len({m["id"] for m in selected}) != 15:
        raise ValueError("Baseline requires exactly 9+6 unique entries")
    if sum(m["role"] == "mod" for m in selected) != 9:
        raise ValueError("Expected exactly 9 gameplay/utility mods")
    for mod in selected:
        if mod["env"]["client"] not in ("required", "optional", "unsupported") or mod["env"]["server"] not in (
                "required", "optional", "unsupported"):
            raise ValueError("Invalid client/server environment for " + mod["id"])
        if not mod.get("slug") or not mod.get("release"):
            raise ValueError("Incomplete pinned release " + mod["id"])
    if args.offline_check:
        print("OFFLINE MANIFEST VALID: 9 feature mods, 6 libraries; exact upstream file/version checks NOT RUN.")
        return 0
    out = args.output.resolve()
    out.mkdir(parents=True, exist_ok=True)
    resolved = []
    for mod in selected:
        rec = pick_version(mod, cfg["minecraft"])
        resolved.append(rec)
        print(f"RESOLVED {mod['id']} {rec['release']} -> {rec['version_id']} [{rec['filename']}]", flush=True)
    check_declared_dependencies(resolved)
    # Never publish any third-party JAR in the modpack archive or GitHub artifact.
    (out / "RESOLVED_RELEASES.json").write_text(json.dumps({
        "environment": {"minecraft": cfg["minecraft"], "neoforge": cfg["neoforge"], "java": "21"},
        "releases": resolved,
    }, indent=2, ensure_ascii=False), encoding="utf-8")
    inspections = {}
    if args.download_jars:
        with tempfile.TemporaryDirectory(prefix="anomalous-mod-verify-") as work:
            for rec in resolved:
                tempjar = pathlib.Path(work) / rec["filename"]
                details = download_and_validate(rec, tempjar)
                inspections[rec["id"]] = details
                print(f"ZIP+SHA512 OK {rec['id']}: {details['mod_ids']}", flush=True)
                if args.server_dir and rec["env"]["server"] == "required":
                    destination = args.server_dir / "mods"
                    destination.mkdir(parents=True, exist_ok=True)
                    shutil.copy2(tempjar, destination / rec["filename"])
            check_mod_ids(inspections)
        (out / "JAR_INSPECTION.json").write_text(json.dumps(inspections, ensure_ascii=False, indent=2),
                                                   encoding="utf-8")
    manifest = mrpack_manifest(cfg, resolved)
    zpath = out / (cfg["version_id"] + ".mrpack")
    with zipfile.ZipFile(zpath, "w", compression=zipfile.ZIP_DEFLATED) as packzip:
        packzip.writestr("modrinth.index.json", json.dumps(manifest, indent=2, ensure_ascii=False))
        packzip.writestr("overrides/README.txt", (
            "Anomalous Classes external-mod baseline (NOT the unreleased class mod).\n"
            "Minecraft 1.21.1 / NeoForge " + cfg["neoforge"] + " / Java 21.\n"
            "All mods are downloaded from their official Modrinth CDN sources.\n"
            "Class gameplay and server/co-load status require separate testing.\n"
            "JEI list overlay: Ctrl+O in-game.\n"
        ))
    print(f"PACK INDEX WRITTEN: {zpath} (no third-party JARs included)", flush=True)
    print("UPSTREAM RELEASE CHECK: PASSED", flush=True)
    if not args.download_jars:
        print("ACTUAL JAR BYTE/ZIP/LOADER INSPECTION: NOT RUN (pass --download-jars)")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        print("MODPACK VALIDATION FAILED: " + str(exc), file=sys.stderr, flush=True)
        sys.exit(1)

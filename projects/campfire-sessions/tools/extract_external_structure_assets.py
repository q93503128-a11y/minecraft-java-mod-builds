#!/usr/bin/env python3
"""Extract approved external structure bases into Campfire generated resources.

Kogtyv structures are extracted from the exact distributed JAR only when that
JAR embeds its complete MIT notice. The harbor dock is obtained from the
Currents of Trade public source repository at a pinned commit, alongside the
MIT license from that same commit.

Reference-only or license-incomplete candidates remain excluded.
"""
from __future__ import annotations

import gzip
import hashlib
import json
import pathlib
import shutil
import sys
import time
import urllib.request
import zipfile


KOGTYV = {
    "data/minecraft/structure/village/greece/village/center/ratush_1.nbt":
        "data/campfiresessions/structure/external/kogtyv_greece/center/ratush_1.nbt",
}

for i in range(1, 9):
    KOGTYV[f"data/minecraft/structure/village/greece/village/house/small_{i}.nbt"] = (
        f"data/campfiresessions/structure/external/kogtyv_greece/house/small_{i}.nbt"
    )
for i in range(1, 7):
    KOGTYV[f"data/minecraft/structure/village/greece/village/house/medium_{i}.nbt"] = (
        f"data/campfiresessions/structure/external/kogtyv_greece/house/medium_{i}.nbt"
    )
for i in range(1, 5):
    KOGTYV[f"data/minecraft/structure/village/greece/village/house/big_{i}.nbt"] = (
        f"data/campfiresessions/structure/external/kogtyv_greece/house/big_{i}.nbt"
    )
for i in range(1, 6):
    KOGTYV[f"data/minecraft/structure/village/greece/village/house/shop_small_{i}.nbt"] = (
        f"data/campfiresessions/structure/external/kogtyv_greece/house/shop_small_{i}.nbt"
    )
for i in range(1, 4):
    KOGTYV[f"data/minecraft/structure/village/greece/village/house/shop_medium_{i}.nbt"] = (
        f"data/campfiresessions/structure/external/kogtyv_greece/house/shop_medium_{i}.nbt"
    )
for i in range(1, 4):
    KOGTYV[f"data/minecraft/structure/village/greece/village/house/shop_triple_{i}.nbt"] = (
        f"data/campfiresessions/structure/external/kogtyv_greece/house/shop_triple_{i}.nbt"
    )
KOGTYV["data/minecraft/structure/village/greece/village/house/triple_1.nbt"] = (
    "data/campfiresessions/structure/external/kogtyv_greece/house/triple_1.nbt"
)

SOURCES = {"kogtyv_tav": KOGTYV}

CURRENTS_REPOSITORY = "Lexovian/Currents-of-Trade"
CURRENTS_COMMIT = "d3b769ec4cbf8d8e744785c4815b6c66107c7884"
CURRENTS_RAW = f"https://raw.githubusercontent.com/{CURRENTS_REPOSITORY}/{CURRENTS_COMMIT}/"
CURRENTS_DOCK_SOURCE = "src/main/resources/data/currents_of_trade/structure/village/dock.nbt"
CURRENTS_DOCK_TARGET = "data/campfiresessions/structure/external/currents_of_trade/dock.nbt"
CURRENTS_LICENSE_SOURCE = "LICENSE"
USER_AGENT = "CampfireSessions/0.6 (+https://github.com/q93503128-a11y/minecraft-java-mod-builds)"


def license_entries(zf: zipfile.ZipFile):
    rows = []
    for name in zf.namelist():
        base = name.lower().rstrip("/").rsplit("/", 1)[-1]
        if base.startswith("license") or base.startswith("copying"):
            try:
                raw = zf.read(name)
                text = raw.decode("utf-8", errors="replace")
            except Exception:
                continue
            if text.strip():
                rows.append((name, raw, text))
    return rows


def find_source_jar(jars: list[pathlib.Path], required_entries: set[str]):
    matches = []
    for jar in jars:
        with zipfile.ZipFile(jar) as zf:
            names = set(zf.namelist())
            hit_count = sum(entry in names for entry in required_entries)
            if hit_count:
                matches.append((hit_count, jar, names))
    exact = [row for row in matches if all(entry in row[2] for entry in required_entries)]
    if len(exact) != 1:
        detail = [(count, jar.name) for count, jar, _ in matches]
        raise RuntimeError(f"expected one JAR containing all selected entries, got {detail}")
    return exact[0][1]


def download(url: str) -> bytes:
    last_error = None
    for attempt in range(3):
        try:
            request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
            with urllib.request.urlopen(request, timeout=90) as response:
                data = response.read()
            if not data:
                raise RuntimeError(f"downloaded empty source: {url}")
            return data
        except Exception as exc:
            last_error = exc
            if attempt < 2:
                time.sleep(1.5 * (attempt + 1))
    raise RuntimeError(f"failed to download pinned source {url}: {last_error}")


def patch_compressed_nbt_identifier(raw: bytes, old: str, new: str, expected: int) -> bytes:
    payload = gzip.decompress(raw)
    old_bytes = old.encode("utf-8")
    new_bytes = new.encode("utf-8")
    old_token = len(old_bytes).to_bytes(2, "big") + old_bytes
    new_token = len(new_bytes).to_bytes(2, "big") + new_bytes
    count = payload.count(old_token)
    if count != expected:
        raise RuntimeError(
            f"unexpected {old!r} occurrence count in dock NBT: expected={expected} actual={count}"
        )
    patched = payload.replace(old_token, new_token)
    return gzip.compress(patched, compresslevel=9, mtime=0)


def add_currents_dock(out_root: pathlib.Path, manifest: dict) -> None:
    license_raw = download(CURRENTS_RAW + CURRENTS_LICENSE_SOURCE)
    license_text = license_raw.decode("utf-8", errors="strict")
    if "MIT License" not in license_text or "Permission is hereby granted" not in license_text:
        raise RuntimeError("Currents of Trade pinned source no longer contains the expected MIT license")

    source_raw = download(CURRENTS_RAW + CURRENTS_DOCK_SOURCE)
    source_sha = hashlib.sha256(source_raw).hexdigest()
    transformed = patch_compressed_nbt_identifier(
        source_raw,
        "currents_of_trade:anchor_point",
        "minecraft:barrel",
        expected=2,
    )

    # Both the palette identifier and block-entity id are rewritten. The original
    # custom HarborName/TradeLevel fields become inert unknown barrel fields and
    # are discarded by Minecraft after placement; no Currents runtime code is used.
    if b"currents_of_trade:anchor_point" in gzip.decompress(transformed):
        raise RuntimeError("custom Currents anchor identifier survived dock conversion")

    license_target = pathlib.Path("META-INF/campfiresessions/licenses/currents_of_trade_LICENSE.txt")
    license_out = out_root / license_target
    license_out.parent.mkdir(parents=True, exist_ok=True)
    license_out.write_bytes(license_raw)

    target = pathlib.Path(CURRENTS_DOCK_TARGET)
    out = out_root / target
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(transformed)

    manifest["sources"]["currents_of_trade"] = {
        "repository": CURRENTS_REPOSITORY,
        "commit": CURRENTS_COMMIT,
        "license": "MIT",
        "license_entry": CURRENTS_LICENSE_SOURCE,
        "license_target": license_target.as_posix(),
        "license_sha256": hashlib.sha256(license_raw).hexdigest(),
    }
    manifest["assets"].append({
        "source": "currents_of_trade",
        "source_repository": CURRENTS_REPOSITORY,
        "source_commit": CURRENTS_COMMIT,
        "source_entry": CURRENTS_DOCK_SOURCE,
        "source_sha256": source_sha,
        "target": target.as_posix(),
        "sha256": hashlib.sha256(transformed).hexdigest(),
        "bytes": len(transformed),
        "modifications": [
            "replace currents_of_trade:anchor_point palette/block-entity id with minecraft:barrel",
            "runtime placement filters jigsaw/structure markers",
        ],
    })


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: extract_external_structure_assets.py <probe-jar-dir> <output-dir>", file=sys.stderr)
        return 2

    jar_root = pathlib.Path(sys.argv[1])
    out_root = pathlib.Path(sys.argv[2])
    jars = sorted(jar_root.glob("*.jar"))
    if not jars:
        raise SystemExit(f"no candidate JARs in {jar_root}")

    if out_root.exists():
        shutil.rmtree(out_root)
    out_root.mkdir(parents=True, exist_ok=True)

    manifest = {
        "format": 2,
        "policy": (
            "Only explicitly selected permissive sources are packaged. Distributed JAR extraction "
            "requires an embedded complete license notice; pinned public source files are paired "
            "with the license from the exact same commit. Towns & Towers and Villageria remain excluded."
        ),
        "sources": {},
        "assets": [],
    }

    for source_id, mapping in SOURCES.items():
        source_jar = find_source_jar(jars, set(mapping))
        with zipfile.ZipFile(source_jar) as zf:
            licenses = license_entries(zf)
            mit = [
                row for row in licenses
                if "MIT License" in row[2] and "Permission is hereby granted" in row[2]
            ]
            if not mit:
                raise RuntimeError(f"{source_id}: embedded MIT license text not found in {source_jar.name}")

            license_name, license_raw, _ = mit[0]
            license_target = pathlib.Path("META-INF/campfiresessions/licenses") / f"{source_id}_LICENSE.txt"
            license_out = out_root / license_target
            license_out.parent.mkdir(parents=True, exist_ok=True)
            license_out.write_bytes(license_raw)

            manifest["sources"][source_id] = {
                "jar": source_jar.name,
                "license": "MIT",
                "license_entry": license_name,
                "license_target": license_target.as_posix(),
                "license_sha256": hashlib.sha256(license_raw).hexdigest(),
            }

            for original, target in sorted(mapping.items()):
                if "kaisyn/" in original.lower() or "villageroles/" in original.lower():
                    raise RuntimeError(f"disallowed extraction path selected: {original}")
                raw = zf.read(original)
                out = out_root / target
                out.parent.mkdir(parents=True, exist_ok=True)
                out.write_bytes(raw)
                manifest["assets"].append({
                    "source": source_id,
                    "source_jar": source_jar.name,
                    "source_entry": original,
                    "target": target,
                    "source_sha256": hashlib.sha256(raw).hexdigest(),
                    "sha256": hashlib.sha256(raw).hexdigest(),
                    "bytes": len(raw),
                    "modifications": [],
                })

    add_currents_dock(out_root, manifest)

    manifest_path = out_root / "META-INF/campfiresessions/external_structure_manifest.json"
    manifest_path.parent.mkdir(parents=True, exist_ok=True)
    manifest_path.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    print(f"external structure assets: {len(manifest['assets'])}")
    for source_id, meta in manifest["sources"].items():
        count = sum(asset["source"] == source_id for asset in manifest["assets"])
        origin = meta.get("jar") or f"{meta.get('repository')}@{meta.get('commit')}"
        print(f"  {source_id}: {count} structures from {origin} license={meta['license']}")
    print(f"manifest: {manifest_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

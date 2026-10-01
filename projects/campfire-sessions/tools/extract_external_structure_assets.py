#!/usr/bin/env python3
"""Build the approved Campfire external-structure resource set.

Only pinned, permissively licensed public source files are packaged here.
Rejected/reference-only sources (including the first Kogtyv civic set) are not
carried into the normal Campfire JAR merely because they were previously tested.
"""
from __future__ import annotations

import gzip
import hashlib
import json
import pathlib
import shutil
import sys
import time
import urllib.parse
import urllib.request


USER_AGENT = "CampfireSessions/0.6 (+https://github.com/q93503128-a11y/minecraft-java-mod-builds)"

CHEKS_REPOSITORY = "Chekmate90/Chek-s-Mint-Structures"
CHEKS_COMMIT = "6ffadba091c4a07bcdac9f1c89d3d1202f0015b9"
CHEKS_RAW = f"https://raw.githubusercontent.com/{CHEKS_REPOSITORY}/{CHEKS_COMMIT}/"
CHEKS_LICENSE_SOURCE = "LICENSE"
CHEKS_STRUCTURES = {
    "data/minecraft/structure/village/plains/houses/plains_meeting_point_4.nbt":
        "data/campfiresessions/structure/external/cheks_mint/plains_meeting_point_4.nbt",
    "data/minecraft/structure/village/plains/houses/plains_medium_house_1.nbt":
        "data/campfiresessions/structure/external/cheks_mint/plains_medium_house_1.nbt",
    "data/minecraft/structure/village/plains/houses/plains_butcher_shop_1.nbt":
        "data/campfiresessions/structure/external/cheks_mint/plains_butcher_shop_1.nbt",
    "data/minecraft/structure/village/plains/houses/plains_butcher_shop_2.nbt":
        "data/campfiresessions/structure/external/cheks_mint/plains_butcher_shop_2.nbt",
    "data/minecraft/structure/village/plains/houses/plains_shepherds_house_1.nbt":
        "data/campfiresessions/structure/external/cheks_mint/plains_shepherds_house_1.nbt",
    "data/minecraft/structure/village/plains/houses/plains_temple_3.nbt":
        "data/campfiresessions/structure/external/cheks_mint/plains_temple_3.nbt",
    "data/minecraft/structure/village/plains/houses/plains_library_1.nbt":
        "data/campfiresessions/structure/external/cheks_mint/plains_library_1.nbt",
    "data/minecraft/structure/village/plains/houses/plains_small_house_1.nbt":
        "data/campfiresessions/structure/external/cheks_mint/plains_small_house_1.nbt",
    "data/minecraft/structure/village/plains/houses/plains_small_house_2.nbt":
        "data/campfiresessions/structure/external/cheks_mint/plains_small_house_2.nbt",
    "data/minecraft/structure/village/plains/houses/plains_medium_house_2.nbt":
        "data/campfiresessions/structure/external/cheks_mint/plains_medium_house_2.nbt",
    "data/minecraft/structure/village/plains/houses/plains_big_house_1.nbt":
        "data/campfiresessions/structure/external/cheks_mint/plains_big_house_1.nbt",
    "data/minecraft/structure/village/plains/houses/plains_fisher_cottage_1.nbt":
        "data/campfiresessions/structure/external/cheks_mint/plains_fisher_cottage_1.nbt",
}

CURRENTS_REPOSITORY = "Lexovian/Currents-of-Trade"
CURRENTS_COMMIT = "d3b769ec4cbf8d8e744785c4815b6c66107c7884"
CURRENTS_RAW = f"https://raw.githubusercontent.com/{CURRENTS_REPOSITORY}/{CURRENTS_COMMIT}/"
CURRENTS_DOCK_SOURCE = "src/main/resources/data/currents_of_trade/structure/village/dock.nbt"
CURRENTS_DOCK_TARGET = "data/campfiresessions/structure/external/currents_of_trade/dock.nbt"
CURRENTS_LICENSE_SOURCE = "LICENSE"


def download(root: str, path: str) -> bytes:
    encoded = "/".join(urllib.parse.quote(part, safe="") for part in path.split("/"))
    url = root + encoded
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


def require_mit(raw: bytes, label: str) -> None:
    text = raw.decode("utf-8", errors="strict")
    if "MIT License" not in text or "Permission is hereby granted" not in text:
        raise RuntimeError(f"{label}: pinned source no longer contains the expected MIT license")


def write_license(out_root: pathlib.Path, source_id: str, raw: bytes) -> pathlib.Path:
    target = pathlib.Path("META-INF/campfiresessions/licenses") / f"{source_id}_LICENSE.txt"
    out = out_root / target
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(raw)
    return target


def add_cheks_mint(out_root: pathlib.Path, manifest: dict) -> None:
    license_raw = download(CHEKS_RAW, CHEKS_LICENSE_SOURCE)
    require_mit(license_raw, "cheks_mint")
    license_target = write_license(out_root, "cheks_mint", license_raw)

    manifest["sources"]["cheks_mint"] = {
        "repository": CHEKS_REPOSITORY,
        "commit": CHEKS_COMMIT,
        "license": "MIT",
        "license_entry": CHEKS_LICENSE_SOURCE,
        "license_target": license_target.as_posix(),
        "license_sha256": hashlib.sha256(license_raw).hexdigest(),
    }

    for source, target_text in sorted(CHEKS_STRUCTURES.items()):
        raw = download(CHEKS_RAW, source)
        target = pathlib.Path(target_text)
        out = out_root / target
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_bytes(raw)
        manifest["assets"].append({
            "source": "cheks_mint",
            "source_repository": CHEKS_REPOSITORY,
            "source_commit": CHEKS_COMMIT,
            "source_entry": source,
            "source_sha256": hashlib.sha256(raw).hexdigest(),
            "target": target.as_posix(),
            "sha256": hashlib.sha256(raw).hexdigest(),
            "bytes": len(raw),
            "modifications": [
                "runtime placement filters jigsaw/structure markers",
            ],
        })


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
    return gzip.compress(payload.replace(old_token, new_token), compresslevel=9, mtime=0)


def add_currents_dock(out_root: pathlib.Path, manifest: dict) -> None:
    license_raw = download(CURRENTS_RAW, CURRENTS_LICENSE_SOURCE)
    require_mit(license_raw, "currents_of_trade")
    license_target = write_license(out_root, "currents_of_trade", license_raw)

    source_raw = download(CURRENTS_RAW, CURRENTS_DOCK_SOURCE)
    transformed = patch_compressed_nbt_identifier(
        source_raw,
        "currents_of_trade:anchor_point",
        "minecraft:barrel",
        expected=2,
    )
    if b"currents_of_trade:anchor_point" in gzip.decompress(transformed):
        raise RuntimeError("custom Currents anchor identifier survived dock conversion")

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
        "source_sha256": hashlib.sha256(source_raw).hexdigest(),
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
        print("usage: extract_external_structure_assets.py <probe-jar-dir-unused> <output-dir>", file=sys.stderr)
        return 2

    out_root = pathlib.Path(sys.argv[2])
    if out_root.exists():
        shutil.rmtree(out_root)
    out_root.mkdir(parents=True, exist_ok=True)

    manifest = {
        "format": 3,
        "policy": (
            "Only currently accepted pinned permissive sources are packaged. "
            "The visually rejected Kogtyv Greece civic set and other reference-only candidates are excluded."
        ),
        "sources": {},
        "assets": [],
    }

    add_cheks_mint(out_root, manifest)
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
        print(
            f"  {source_id}: {count} structures from "
            f"{meta['repository']}@{meta['commit']} license={meta['license']}"
        )
    print(f"manifest: {manifest_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

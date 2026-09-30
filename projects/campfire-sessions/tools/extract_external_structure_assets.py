#!/usr/bin/env python3
"""Extract the approved third-party structure bases into Campfire generated resources.

Only permissively licensed, explicitly selected source structures are copied.
Towns & Towers/kaisyn entries are intentionally excluded because the packaged
license forbids modified/repackaged structure extraction.
"""
from __future__ import annotations

import hashlib
import json
import pathlib
import shutil
import sys
import zipfile


VILLAGERIA = {
    "data/villageroles/structure/town_hall.nbt": "data/campfiresessions/structure/external/villageria/town_hall.nbt",
    "data/villageroles/structure/shop.nbt": "data/campfiresessions/structure/external/villageria/shop.nbt",
    "data/villageroles/structure/hospital.nbt": "data/campfiresessions/structure/external/villageria/hospital.nbt",
}

KOGTYV_FIXED = {
    "data/minecraft/structure/village/greece/village/center/ratush_1.nbt":
        "data/campfiresessions/structure/external/kogtyv_greece/center/ratush_1.nbt",
}

for i in range(1, 9):
    KOGTYV_FIXED[f"data/minecraft/structure/village/greece/village/house/small_{i}.nbt"] = (
        f"data/campfiresessions/structure/external/kogtyv_greece/house/small_{i}.nbt"
    )
for i in range(1, 7):
    KOGTYV_FIXED[f"data/minecraft/structure/village/greece/village/house/medium_{i}.nbt"] = (
        f"data/campfiresessions/structure/external/kogtyv_greece/house/medium_{i}.nbt"
    )
for i in range(1, 5):
    KOGTYV_FIXED[f"data/minecraft/structure/village/greece/village/house/big_{i}.nbt"] = (
        f"data/campfiresessions/structure/external/kogtyv_greece/house/big_{i}.nbt"
    )
for i in range(1, 6):
    KOGTYV_FIXED[f"data/minecraft/structure/village/greece/village/house/shop_small_{i}.nbt"] = (
        f"data/campfiresessions/structure/external/kogtyv_greece/house/shop_small_{i}.nbt"
    )
for i in range(1, 4):
    KOGTYV_FIXED[f"data/minecraft/structure/village/greece/village/house/shop_medium_{i}.nbt"] = (
        f"data/campfiresessions/structure/external/kogtyv_greece/house/shop_medium_{i}.nbt"
    )
KOGTYV_FIXED["data/minecraft/structure/village/greece/village/house/shop_triple_1.nbt"] = (
    "data/campfiresessions/structure/external/kogtyv_greece/house/shop_triple_1.nbt"
)
KOGTYV_FIXED["data/minecraft/structure/village/greece/village/house/triple_1.nbt"] = (
    "data/campfiresessions/structure/external/kogtyv_greece/house/triple_1.nbt"
)

SOURCES = {
    "villageria": VILLAGERIA,
    "kogtyv_tav": KOGTYV_FIXED,
}


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
        "format": 1,
        "policy": "Only approved permissive sources are extracted. Towns & Towers is reference-only and excluded.",
        "sources": {},
        "assets": [],
    }

    for source_id, mapping in SOURCES.items():
        source_jar = find_source_jar(jars, set(mapping))
        with zipfile.ZipFile(source_jar) as zf:
            licenses = license_entries(zf)
            mit = [row for row in licenses if "MIT License" in row[2] and "Permission is hereby granted" in row[2]]
            if not mit:
                raise RuntimeError(f"{source_id}: MIT license text not found in {source_jar.name}")

            license_name, license_raw, _ = mit[0]
            license_target = pathlib.Path("META-INF/campfiresessions/licenses") / f"{source_id}_LICENSE.txt"
            license_out = out_root / license_target
            license_out.parent.mkdir(parents=True, exist_ok=True)
            license_out.write_bytes(license_raw)

            source_meta = {
                "jar": source_jar.name,
                "license": "MIT",
                "license_entry": license_name,
                "license_target": license_target.as_posix(),
                "license_sha256": hashlib.sha256(license_raw).hexdigest(),
            }
            manifest["sources"][source_id] = source_meta

            for original, target in sorted(mapping.items()):
                raw = zf.read(original)
                out = out_root / target
                out.parent.mkdir(parents=True, exist_ok=True)
                out.write_bytes(raw)
                manifest["assets"].append({
                    "source": source_id,
                    "source_jar": source_jar.name,
                    "source_entry": original,
                    "target": target,
                    "sha256": hashlib.sha256(raw).hexdigest(),
                    "bytes": len(raw),
                })

    manifest_path = out_root / "META-INF/campfiresessions/external_structure_manifest.json"
    manifest_path.parent.mkdir(parents=True, exist_ok=True)
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    print(f"external structure assets: {len(manifest['assets'])}")
    for source_id, meta in manifest["sources"].items():
        count = sum(a["source"] == source_id for a in manifest["assets"])
        print(f"  {source_id}: {count} structures from {meta['jar']} license={meta['license']}")
    print(f"manifest: {manifest_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

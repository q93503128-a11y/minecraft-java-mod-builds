#!/usr/bin/env python3
"""Provision the verified canonical Campfire island into the village-review run.

This task never overwrites an unrelated or unmarked save. The source archive is
verified by SHA-256 before extraction, then the exact copied save receives a
small provenance marker consumed by VillageReviewBootstrap.
"""
from __future__ import annotations

import hashlib
import json
import pathlib
import shutil
import sys
import tempfile
import time
import urllib.request
import zipfile


EXPECTED_SHA256 = "7a3d98ff75feb26913e2d4c32ca7339448d3c660c14f986ce9f5e4ff340d3d3b"
SOURCE_MARKER = ".campfiresessions-canonical-world-source"
URLS = [
    "https://mediafilez.forgecdn.net/files/6229/422/Island%20-%20No%20WorldBorder.zip",
    "https://www.curseforge.com/api/v1/mods/1206982/files/6229422/download",
    "https://www.curseforge.com/minecraft/worlds/island-map-1-19-1024x1024/download/6229422/file",
]
USER_AGENT = "CampfireSessionsVillageReview/1.0 (+https://github.com/q93503128-a11y/minecraft-java-mod-builds)"


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def download_archive() -> tuple[bytes, str]:
    errors = []
    for url in URLS:
        for attempt in range(2):
            try:
                request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
                with urllib.request.urlopen(request, timeout=180) as response:
                    data = response.read()
                if sha256(data) != EXPECTED_SHA256:
                    raise RuntimeError(
                        f"canonical world hash mismatch from {url}: "
                        f"expected={EXPECTED_SHA256} actual={sha256(data)}"
                    )
                return data, url
            except Exception as exc:
                errors.append(f"{url} attempt {attempt + 1}: {exc}")
                time.sleep(1.0)
    raise RuntimeError("unable to download canonical Campfire world:\n" + "\n".join(errors))


def select_world_root(unpacked: pathlib.Path) -> pathlib.Path:
    candidates = []
    for level_dat in unpacked.rglob("level.dat"):
        root = level_dat.parent
        region = root / "region"
        region_count = len(list(region.glob("r.*.*.mca"))) if region.is_dir() else 0
        candidates.append((region_count, -len(root.parts), root))
    if not candidates:
        raise RuntimeError("canonical archive contained no level.dat")
    candidates.sort(reverse=True)
    root = candidates[0][2]
    if candidates[0][0] < 1:
        raise RuntimeError(f"selected world root has no region files: {root}")
    return root


def marker_matches(output: pathlib.Path) -> bool:
    marker = output / SOURCE_MARKER
    if not marker.is_file():
        return False
    try:
        data = json.loads(marker.read_text(encoding="utf-8"))
    except Exception:
        return False
    return data.get("archive_sha256") == EXPECTED_SHA256


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: prepare_village_review_world.py <save-output-dir>", file=sys.stderr)
        return 2

    output = pathlib.Path(sys.argv[1]).resolve()
    if output.exists():
        if marker_matches(output):
            print(f"[village-review-world] verified existing canonical review save: {output}")
            return 0
        raise RuntimeError(
            f"refusing to overwrite existing unverified review save: {output}\n"
            f"Delete/rename it explicitly if a clean review copy is desired."
        )

    archive, source_url = download_archive()
    with tempfile.TemporaryDirectory(prefix="campfire-village-world-") as temp_dir:
        temp = pathlib.Path(temp_dir)
        archive_path = temp / "island.zip"
        archive_path.write_bytes(archive)
        unpacked = temp / "unpacked"
        unpacked.mkdir()
        with zipfile.ZipFile(archive_path) as zf:
            bad = zf.testzip()
            if bad is not None:
                raise RuntimeError(f"corrupt canonical world archive entry: {bad}")
            zf.extractall(unpacked)

        world_root = select_world_root(unpacked)
        output.parent.mkdir(parents=True, exist_ok=True)
        shutil.copytree(world_root, output)

    (output / "session.lock").unlink(missing_ok=True)
    marker = {
        "project": "Campfire Sessions",
        "source": "Geming400 Island map | 1024x1024 / Island - No WorldBorder.zip",
        "source_url": source_url,
        "curseforge_file_id": 6229422,
        "license": "MIT",
        "archive_sha256": EXPECTED_SHA256,
    }
    (output / SOURCE_MARKER).write_text(
        json.dumps(marker, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    print(f"[village-review-world] canonical save prepared: {output}")
    print(f"[village-review-world] archive sha256={EXPECTED_SHA256}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""Probe pinned MineColonies schematic candidates for Campfire village use.

This is an intake/research tool only. It downloads a pinned external commit,
parses structure NBTs with the Python standard library, and reports exact size,
palette namespaces, actual block counts, and custom/runtime block dependencies.
It does not package any candidate into Campfire.
"""
from __future__ import annotations

import collections
import gzip
import hashlib
import io
import json
import pathlib
import struct
import sys
import time
import urllib.parse
import urllib.request


REPOSITORY = "ldtteam/minecolonies-schematics"
COMMIT = "ab3d7c2ddc789de2fa2174bb14fbac19ddb7e006"
RAW_ROOT = f"https://raw.githubusercontent.com/{REPOSITORY}/{COMMIT}/"
STYLE = "Medieval oak"
ROLES = ("townhall", "cook", "baker", "warehouse", "library", "shepherd", "citizen")
LEVELS = range(1, 6)
USER_AGENT = "CampfireSessions/0.6 candidate-probe"


class NbtReader:
    def __init__(self, data: bytes):
        if data[:2] == b"\x1f\x8b":
            data = gzip.decompress(data)
        self.fp = io.BytesIO(data)

    def read(self, fmt: str):
        size = struct.calcsize(">" + fmt)
        raw = self.fp.read(size)
        if len(raw) != size:
            raise EOFError("unexpected end of NBT")
        return struct.unpack(">" + fmt, raw)[0]

    def string(self) -> str:
        length = self.read("H")
        return self.fp.read(length).decode("utf-8")

    def payload(self, tag: int):
        if tag == 0:
            return None
        if tag == 1:
            return self.read("b")
        if tag == 2:
            return self.read("h")
        if tag == 3:
            return self.read("i")
        if tag == 4:
            return self.read("q")
        if tag == 5:
            return self.read("f")
        if tag == 6:
            return self.read("d")
        if tag == 7:
            n = self.read("i")
            return self.fp.read(n)
        if tag == 8:
            return self.string()
        if tag == 9:
            child = self.read("B")
            n = self.read("i")
            return [self.payload(child) for _ in range(n)]
        if tag == 10:
            out = {}
            while True:
                child = self.read("B")
                if child == 0:
                    return out
                name = self.string()
                out[name] = self.payload(child)
        if tag == 11:
            n = self.read("i")
            return [self.read("i") for _ in range(n)]
        if tag == 12:
            n = self.read("i")
            return [self.read("q") for _ in range(n)]
        raise ValueError(f"unsupported NBT tag {tag}")

    def root(self):
        root_tag = self.read("B")
        if root_tag != 10:
            raise ValueError(f"expected root compound, got {root_tag}")
        _root_name = self.string()
        return self.payload(root_tag)


def download(path: str) -> bytes:
    encoded = "/".join(urllib.parse.quote(part, safe="") for part in path.split("/"))
    url = RAW_ROOT + encoded
    last_error = None
    for attempt in range(3):
        try:
            request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
            with urllib.request.urlopen(request, timeout=90) as response:
                data = response.read()
            if not data:
                raise RuntimeError(f"empty response for {path}")
            return data
        except Exception as exc:
            last_error = exc
            if attempt < 2:
                time.sleep(1.5 * (attempt + 1))
    raise RuntimeError(f"failed to download {path}: {last_error}")


def all_strings(value):
    if isinstance(value, str):
        yield value
    elif isinstance(value, dict):
        for child in value.values():
            yield from all_strings(child)
    elif isinstance(value, list):
        for child in value:
            yield from all_strings(child)


def analyze(role: str, level: int) -> dict:
    path = f"{STYLE}/buildings/{role}/{role}{level}.nbt"
    raw = download(path)
    root = NbtReader(raw).root()
    size = root.get("size")
    palette = root.get("palette")
    blocks = root.get("blocks")

    if not (
        isinstance(size, list)
        and len(size) == 3
        and all(isinstance(v, int) for v in size)
        and isinstance(palette, list)
        and isinstance(blocks, list)
    ):
        raise RuntimeError(f"{path}: unexpected structure NBT shape")

    palette_names = []
    for entry in palette:
        if isinstance(entry, dict):
            palette_names.append(str(entry.get("Name", "minecraft:air")))
        else:
            palette_names.append("minecraft:air")

    counts = collections.Counter()
    for block in blocks:
        if not isinstance(block, dict):
            continue
        state = block.get("state")
        if isinstance(state, int) and 0 <= state < len(palette_names):
            counts[palette_names[state]] += 1

    namespace_counts = collections.Counter()
    custom_blocks = collections.Counter()
    for name, count in counts.items():
        namespace = name.split(":", 1)[0] if ":" in name else "minecraft"
        namespace_counts[namespace] += count
        if namespace != "minecraft":
            custom_blocks[name] += count

    custom_identifiers = sorted({
        value
        for value in all_strings(root)
        if isinstance(value, str)
        and ":" in value
        and not value.startswith("minecraft:")
    })

    x, y, z = size
    non_air_blocks = sum(count for name, count in counts.items() if name != "minecraft:air")
    return {
        "role": role,
        "level": level,
        "source_path": path,
        "source_sha256": hashlib.sha256(raw).hexdigest(),
        "bytes": len(raw),
        "size": size,
        "footprint": x * z,
        "volume": x * y * z,
        "palette_entries": len(palette_names),
        "placed_blocks": sum(counts.values()),
        "non_air_blocks": non_air_blocks,
        "namespace_counts": dict(sorted(namespace_counts.items())),
        "custom_block_counts": dict(custom_blocks.most_common()),
        "custom_identifiers": custom_identifiers,
        "top_blocks": counts.most_common(20),
    }


def score(row: dict) -> tuple:
    custom = sum(row["custom_block_counts"].values())
    # This is only a mechanical intake score, not a visual quality score.
    return (
        1 if custom else 0,
        custom,
        row["footprint"],
        row["volume"],
        row["level"],
    )


def main() -> int:
    out_dir = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else "build/minecoloniesCandidateProbe")
    out_dir.mkdir(parents=True, exist_ok=True)

    rows = []
    for role in ROLES:
        for level in LEVELS:
            row = analyze(role, level)
            rows.append(row)
            print(
                f"{role:10} L{level} "
                f"size={row['size'][0]}x{row['size'][1]}x{row['size'][2]} "
                f"footprint={row['footprint']:4} "
                f"custom_blocks={sum(row['custom_block_counts'].values()):5} "
                f"namespaces={row['namespace_counts']}"
            )

    by_role = {}
    for role in ROLES:
        candidates = sorted((row for row in rows if row["role"] == role), key=score)
        by_role[role] = candidates[0]

    report = {
        "format": 1,
        "source_repository": REPOSITORY,
        "source_commit": COMMIT,
        "source_branch_observed": "carlansor-update",
        "style": STYLE,
        "license_intake_status": (
            "PROBE_ONLY: repository default branch exposes GPL-3.0 LICENSE, "
            "candidate branch itself lacks LICENSE; resolve before packaging"
        ),
        "rows": rows,
        "mechanical_best_per_role": by_role,
    }
    (out_dir / "minecolonies-candidates.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    lines = [
        "# MineColonies Campfire candidate probe",
        "",
        f"- repository: `{REPOSITORY}`",
        f"- commit: `{COMMIT}`",
        f"- style: `{STYLE}`",
        "- status: PROBE ONLY; not accepted/packaged",
        "",
        "| Role | Level | Size | Footprint | Custom block instances | Namespaces |",
        "| --- | ---: | --- | ---: | ---: | --- |",
    ]
    for row in rows:
        custom = sum(row["custom_block_counts"].values())
        namespaces = ", ".join(f"{k}:{v}" for k, v in row["namespace_counts"].items())
        lines.append(
            f"| {row['role']} | {row['level']} | "
            f"{row['size'][0]}×{row['size'][1]}×{row['size'][2]} | "
            f"{row['footprint']} | {custom} | {namespaces} |"
        )

    lines += ["", "## Mechanical lowest-dependency candidate per role", ""]
    for role, row in by_role.items():
        custom = sum(row["custom_block_counts"].values())
        lines.append(
            f"- **{role} L{row['level']}** — "
            f"{row['size'][0]}×{row['size'][1]}×{row['size'][2]}, "
            f"custom block instances={custom}"
        )
        if row["custom_block_counts"]:
            lines.append(
                "  - custom blocks: "
                + ", ".join(f"`{k}`×{v}" for k, v in row["custom_block_counts"].items())
            )

    (out_dir / "minecolonies-candidates.md").write_text(
        "\n".join(lines) + "\n",
        encoding="utf-8",
    )
    print(f"JSON={out_dir / 'minecolonies-candidates.json'}")
    print(f"MARKDOWN={out_dir / 'minecolonies-candidates.md'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

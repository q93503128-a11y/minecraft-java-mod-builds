#!/usr/bin/env python3
"""Reversible offline preview: suppress 62 hash-pinned Terminus command blocks ONLY.

Requires an unactivated world copy and the exact official coordinate-report bytes.
Never patches the original installation, unreviewed scene machinery or other commands.
"""
import argparse
import gzip
import hashlib
import json
import os
import shutil
import struct
import sys
import tempfile
import zlib
from collections import defaultdict
from pathlib import Path
from scan_drehmal_command_blocks import NBTReader, NBTError, MAX_DEPTH, MAX_LIST

LEVEL_SHA = "44e9b625f9dd8616472bbae98b3481a2fb34a22a49a98faf913d3b7148a935d1"
REPORT_SHA = "26df4bab53bb655ec6d0ca1d369162c5bc2dbf542baba9e876dccb284d71bb87"
BACKUP = "turnbound-terminus-block-backup"
RECEIPT = "turnbound-terminus-block-preview.json"

def sha(raw):
    return hashlib.sha256(raw).hexdigest()

def file_sha(path):
    h = hashlib.sha256()
    with path.open("rb") as f:
        while chunk := f.read(4 * 1024 * 1024):
            h.update(chunk)
    return h.hexdigest()

def validate_world(world, expected=LEVEL_SHA):
    world = world.resolve(strict=True)
    marker = world / "turnbound-stage.json"
    level = world / "level.dat"
    if any(not x.is_file() or x.is_symlink() for x in (marker, level)):
        raise ValueError("This is not a preserved staged world copy")
    if file_sha(level) != expected or json.loads(marker.read_text()).get("source_level_sha256") != expected:
        raise ValueError("Staged world does not match official release")
    if (world / "turnbound-world.marker").exists():
        raise ValueError("Refusing to alter an activated TURNBOUND world")
    return world

def read_targets(path, expected_hash=REPORT_SHA, expected_count=62):
    raw = path.read_bytes()
    if sha(raw) != expected_hash:
        raise ValueError("Unrecognized source coordinate report hash")
    info = json.loads(raw)
    if info.get("level_dat_sha256") != LEVEL_SHA or info.get("world_changed") is not False:
        raise ValueError("Unverified source world")
    targets = []
    for item in info.get("triggers", []):
        categories = item.get("review_categories", [])
        if categories not in (["TOWER_ARRIVAL_REPLACE"], ["TOWER_DEPARTURE_REPLACE"]):
            continue
        region = item["region_file"]
        rel = Path(region)
        prefix = ("terminus:teleport_from_tower/" if categories[0] == "TOWER_ARRIVAL_REPLACE"
                  else "terminus:teleport_to_tower/")
        coords = item["coordinates"]
        fid = item["function_ids"]
        if (rel.is_absolute() or len(rel.parts) != 2 or rel.parts[0] != "region"
                or not rel.name.endswith(".mca") or ".." in rel.parts
                or item["source_dimension"] != "minecraft:overworld"
                or item["command_actor_type"] != "minecraft:command_block"
                or len(fid) != 1 or not fid[0].startswith(prefix)
                or item["position_source"] != "NBT"
                or len(coords) != 3 or any(type(v) is not int for v in coords)
                or not isinstance(item["chunk_slot"], int) or not 0 <= item["chunk_slot"] < 1024
                or len(item["command_sha256"]) != 64):
            raise ValueError("Invalid or non-Terminus source trigger")
        targets.append((region, item["chunk_slot"], tuple(coords),
                        item["command_sha256"], item["command_actor_type"]))
    if len(targets) != expected_count or len(set(targets)) != len(targets):
        raise ValueError("Expected exactly 62 distinct verified Terminus triggers")
    return targets

class SpanReader(NBTReader):
    def __init__(self, raw):
        super().__init__(raw)
        self.blocks = []

    def value(self, kind, depth=0):
        if depth > MAX_DEPTH:
            raise NBTError("NBT nesting too deep")
        if kind == 8:
            start = self.pos
            value = self.string()
            return value, start, self.pos
        if kind == 9:
            child, count = self.numeric("B"), self.numeric("i")
            if count < 0 or count > MAX_LIST or (child == 0 and count):
                raise NBTError("Bad list count")
            for _ in range(count):
                self.value(child, depth + 1)
            return None
        if kind == 10:
            fields = {}
            while True:
                tag = self.numeric("B")
                if tag == 0:
                    break
                name = self.string()
                value = self.value(tag, depth + 1)
                if name in ("id", "Command", "x", "y", "z", "TileX", "TileY", "TileZ"):
                    fields[name] = value
            actor, command = fields.get("id"), fields.get("Command")
            if isinstance(actor, tuple) and isinstance(command, tuple):
                coords = tuple(fields.get(x, fields.get("Tile" + x.upper()))
                               for x in ("x", "y", "z"))
                self.blocks.append((actor[0], coords, command[0], command[1], command[2]))
            return None
        return super().value(kind, depth)

    def scan(self):
        if self.numeric("B") != 10:
            raise NBTError("Invalid compound root")
        self.string()
        self.value(10)
        if self.pos != len(self.data):
            raise NBTError("Unparsed NBT bytes")
        return self.blocks

def decompress(payload):
    if not payload or payload[0] & 0x80:
        raise ValueError("Unsupported external or empty Anvil chunk")
    compression = payload[0]
    if compression == 1:
        raw = gzip.decompress(payload[1:])
    elif compression == 2:
        raw = zlib.decompress(payload[1:])
    elif compression == 3:
        raw = payload[1:]
    else:
        raise ValueError("Unknown Anvil compression type")
    if len(raw) > 32 * 1024 * 1024:
        raise ValueError("Excessive decompressed chunk")
    return compression, raw

def compress(mode, raw):
    if mode == 1:
        return b"\x01" + gzip.compress(raw)
    if mode == 2:
        return b"\x02" + zlib.compress(raw)
    return b"\x03" + raw

def patch_chunk(payload, targets):
    compression, nbt = decompress(payload)
    found = SpanReader(nbt).scan()
    ranges = []
    for coord, expected_sha, actor in targets:
        candidates = [(start, end) for name, xyz, command, start, end in found
                      if name == actor and xyz == coord and sha(command.encode("utf-8")) == expected_sha]
        if len(candidates) != 1:
            raise ValueError("Target command absent or ambiguous at " + str(coord))
        ranges.append(candidates[0])
    if len(set(ranges)) != len(ranges):
        raise ValueError("Duplicate NBT command span")
    for start, end in sorted(ranges, reverse=True):
        nbt = nbt[:start] + b"\x00\x00" + nbt[end:]
    revised = SpanReader(nbt).scan()
    if (len(found) != len(revised) or
            any(a[:2] != b[:2] for a, b in zip(found, revised)) or
            sum(a[2] != b[2] for a, b in zip(found, revised)) != len(ranges)):
        raise ValueError("Unintended block command change")
    return compress(compression, nbt)

def patch_region(original, slots):
    if len(original) < 8192 or len(original) % 4096:
        raise ValueError("Unsupported Anvil region header/size")
    header = bytearray(original[:8192])
    result = bytearray(original[:8192])
    patched = 0
    for slot in range(1024):
        record = original[slot * 4:slot * 4 + 4]
        offset, sectors = int.from_bytes(record[:3], "big"), record[3]
        if offset == 0 and sectors == 0:
            continue
        if offset < 2 or sectors == 0:
            raise ValueError("Corrupt occupied chunk slot")
        a, b = offset * 4096, (offset + sectors) * 4096
        if b > len(original):
            raise ValueError("Occupied sector exceeds file")
        section = original[a:b]
        if slot in slots:
            size = struct.unpack(">I", section[:4])[0]
            if size < 2 or size > len(section) - 4:
                raise ValueError("Truncated target chunk")
            changed = patch_chunk(section[4:4 + size], slots[slot])
            payload = struct.pack(">I", len(changed)) + changed
            section = payload + bytes((-len(payload)) % 4096)
            patched += len(slots[slot])
        start = len(result) // 4096
        n = len(section) // 4096
        if start >= 2 ** 24 or n > 255 or n == 0:
            raise ValueError("Repacked Anvil sector limits exceeded")
        header[slot * 4:slot * 4 + 4] = start.to_bytes(3, "big") + bytes([n])
        result.extend(section)
    if patched != sum(map(len, slots.values())):
        raise ValueError("A selected target slot did not exist")
    result[:8192] = header
    return bytes(result), patched

def prepare(world, targets):
    groups = defaultdict(lambda: defaultdict(list))
    for region, slot, coords, command_hash, actor in targets:
        groups[region][slot].append((coords, command_hash, actor))
    prepared = {}
    for relative, slots in sorted(groups.items()):
        file = world / relative
        if not file.is_file() or file.is_symlink():
            raise ValueError("Missing region or unexpected symlink")
        before = file.read_bytes()
        after, count = patch_region(before, slots)
        if count != sum(len(v) for v in slots.values()):
            raise ValueError("Target count mismatch")
        prepared[relative] = before, after
    return prepared

def write_atomic(dest, raw):
    with tempfile.NamedTemporaryFile(dir=dest.parent, prefix=".turnbound-", delete=False) as out:
        temp = Path(out.name)
        out.write(raw)
    try:
        os.replace(temp, dest)
    finally:
        temp.unlink(missing_ok=True)

def apply(world, targets, report_sha=REPORT_SHA):
    world = validate_world(world)
    backup = world / BACKUP
    receipt = world / RECEIPT
    if backup.exists() or receipt.exists():
        raise ValueError("Existing preview backup requires restore")
    prepared = prepare(world, targets)  # Nothing written unless every original hash matches.
    summary = {
        "schema": 1,
        "mode": "OFFLINE_TERMINUS_ONLY",
        "original_level_sha256": LEVEL_SHA,
        "report_sha256": report_sha,
        "target_count": len(targets),
        "regions": {p: {"before": sha(a), "after": sha(b)}
                    for p, (a, b) in prepared.items()},
        "source_world_unchanged": True,
        "gameplay_verified": False
    }
    backup.mkdir()
    for name, (before, after) in prepared.items():
        file = backup / name
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_bytes(before)
        if file_sha(file) != sha(before):
            raise ValueError("Original region backup verification failed")
    # Durable receipt written before any staged region mutation, for crash recovery.
    receipt.write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    for name, (before, after) in prepared.items():
        write_atomic(world / name, after)
        if file_sha(world / name) != sha(after):
            raise ValueError("Rewritten region failed SHA verification")
    return summary

def restore(world, expected_level=LEVEL_SHA):
    world = validate_world(world, expected_level)
    backup, receipt = world / BACKUP, world / RECEIPT
    if not backup.is_dir() or not receipt.is_file():
        raise ValueError("No reversible preview receipt")
    info = json.loads(receipt.read_text(encoding="utf-8"))
    if info.get("mode") != "OFFLINE_TERMINUS_ONLY" or info.get("original_level_sha256") != expected_level:
        raise ValueError("Unknown region preview state")
    for name, hashes in info["regions"].items():
        p = Path(name)
        if p.is_absolute() or ".." in p.parts or len(p.parts) != 2 or p.parts[0] != "region":
            raise ValueError("Unsafe region backup record")
        if file_sha(world / p) not in (hashes["before"], hashes["after"]):
            raise ValueError("Region was modified after preview; refuse destructive restore")
        if file_sha(backup / p) != hashes["before"]:
            raise ValueError("Original region backup is corrupt")
    for name, hashes in info["regions"].items():
        write_atomic(world / name, (backup / name).read_bytes())
        if file_sha(world / name) != hashes["before"]:
            raise ValueError("Restored region SHA mismatch")
    receipt.unlink()
    shutil.rmtree(backup)
    return {"status": "EXACT_RESTORATION", "region_count": len(info["regions"]),
            "target_count": info["target_count"]}

def main():
    p = argparse.ArgumentParser()
    p.add_argument("action", choices=("inspect", "apply", "restore"))
    p.add_argument("--world-copy", type=Path, required=True)
    p.add_argument("--positions", type=Path)
    p.add_argument("--output", type=Path, required=True)
    opts = p.parse_args()
    world = validate_world(opts.world_copy)
    output = opts.output.resolve()
    if output == world or world in output.parents:
        p.error("Output cannot be written inside the world")
    if opts.action == "restore":
        result = restore(world)
    else:
        if opts.positions is None:
            p.error("--positions required")
        targets = read_targets(opts.positions)
        if opts.action == "inspect":
            prepared = prepare(world, targets)
            result = {"status": "READY_TO_PATCH_COPY", "target_count": len(targets),
                      "region_count": len(prepared), "world_changed": False}
        else:
            result = apply(world, targets)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"action": opts.action,
                      "target_count": result["target_count"],
                      "region_count": len(result["regions"]) if "regions" in result else result["region_count"],
                      "gameplay_tested": False}, ensure_ascii=False))

if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, KeyError, NBTError, zlib.error) as e:
        print("Terminus copy-only preview refused: " + str(e), file=sys.stderr)
        sys.exit(2)

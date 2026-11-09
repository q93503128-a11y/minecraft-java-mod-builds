#!/usr/bin/env python3
"""Read-only scan of Minecraft 1.20.1 region NBT for command blocks/minecarts.

No region writes, no third-party dependencies; reports unsupported and corrupt chunks.
"""
from __future__ import annotations
import gzip
import json
import re
import struct
import zlib
from pathlib import Path

MAX_DECOMPRESSED = 32 * 1024 * 1024
MAX_LIST = 2_000_000
MAX_DEPTH = 64

class NBTError(ValueError):
    pass

class NBTReader:
    def __init__(self, data: bytes):
        self.data = memoryview(data)
        self.pos = 0

    def take(self, length):
        if length < 0 or self.pos + length > len(self.data):
            raise NBTError("Unexpected NBT end or negative count")
        out = self.data[self.pos:self.pos + length]
        self.pos += length
        return out

    def numeric(self, format_):
        size = struct.calcsize(">" + format_)
        return struct.unpack(">" + format_, self.take(size))[0]

    def string(self):
        return bytes(self.take(self.numeric("H"))).decode("utf-8", errors="replace")

    def value(self, kind, depth=0):
        if depth > MAX_DEPTH:
            raise NBTError("Nested NBT exceeds limit")
        if kind == 1: return self.numeric("b")
        if kind == 2: return self.numeric("h")
        if kind == 3: return self.numeric("i")
        if kind == 4: return self.numeric("q")
        if kind == 5: return self.numeric("f")
        if kind == 6: return self.numeric("d")
        if kind == 8: return self.string()
        if kind in (7, 11, 12):
            count = self.numeric("i")
            size = {7: 1, 11: 4, 12: 8}[kind]
            if count < 0 or count > MAX_LIST: raise NBTError("Oversized NBT array")
            values = self.take(count * size)
            if kind == 7: return None
            return None
        if kind == 9:
            child = self.numeric("B")
            count = self.numeric("i")
            if count < 0 or count > MAX_LIST or (child == 0 and count):
                raise NBTError("Invalid NBT list")
            return [self.value(child, depth + 1) for _ in range(count)]
        if kind == 10:
            out = {}
            while True:
                child = self.numeric("B")
                if child == 0: break
                name = self.string()
                out[name] = self.value(child, depth + 1)
            return out
        raise NBTError(f"Unknown NBT tag type {kind}")

    def root(self):
        if self.numeric("B") != 10:
            raise NBTError("Chunk root is not a compound")
        self.string()
        return self.value(10)

def visit_command_actors(node, found):
    if isinstance(node, dict):
        entity_id = str(node.get("id", "")).lower()
        cmd = node.get("Command")
        if isinstance(cmd, str) and ("command_block" in entity_id or "commandblock_minecart" in entity_id):
            found.append({"id": entity_id, "command": cmd,
                          "x": node.get("x", node.get("TileX")),
                          "y": node.get("y", node.get("TileY")),
                          "z": node.get("z", node.get("TileZ"))})
        for child in node.values(): visit_command_actors(child, found)
    elif isinstance(node, list):
        for child in node: visit_command_actors(child, found)

def decode_chunk(payload: bytes):
    if not payload: raise NBTError("Empty chunk")
    compression = payload[0] & 0x7f
    if payload[0] & 0x80:
        raise NBTError("External .mcc chunk requires separate review")
    data = payload[1:]
    try:
        if compression == 1: expanded = gzip.decompress(data)
        elif compression == 2: expanded = zlib.decompress(data)
        elif compression == 3: expanded = data
        else: raise NBTError(f"Unsupported region compression {compression}")
    except (gzip.BadGzipFile, zlib.error, EOFError) as exc:
        raise NBTError(f"Failed to decompress chunk: {exc}") from exc
    if len(expanded) > MAX_DECOMPRESSED:
        raise NBTError("Decompressed chunk exceeds limit")
    return NBTReader(expanded).root()

def scan_region(path: Path):
    found, warnings = [], []
    with path.open("rb") as handle:
        header = handle.read(4096)
        if len(header) != 4096:
            raise NBTError("Region file missing location header")
        for idx in range(1024):
            record = header[idx * 4:idx * 4 + 4]
            sector_offset = int.from_bytes(record[:3], "big")
            sector_count = record[3]
            if not sector_offset: continue
            if sector_offset < 2 or sector_count == 0:
                warnings.append(f"chunk slot {idx}: invalid offset/sectors")
                continue
            try:
                handle.seek(sector_offset * 4096)
                length_raw = handle.read(4)
                if len(length_raw) != 4: raise NBTError("Truncated chunk length")
                length = int.from_bytes(length_raw, "big")
                if length < 2 or length > sector_count * 4096 - 4:
                    raise NBTError("Chunk size violates sector allocation")
                root = decode_chunk(handle.read(length))
                hit = []
                visit_command_actors(root, hit)
                for item in hit:
                    item["file"] = str(path)
                    item["chunk_slot"] = idx
                found.extend(hit)
            except (NBTError, OSError, IndexError, ValueError) as exc:
                warnings.append(f"chunk slot {idx}: {exc}")
    return found, warnings

def scan_world(world: Path):
    if not (world / "level.dat").is_file():
        raise ValueError("Expected Minecraft save directory with level.dat")
    files = sorted(p for p in world.rglob("*.mca")
                   if p.parent.name in ("region", "entities")
                   and not p.is_symlink())
    if not files:
        return {"status": "PARTIAL", "command_blocks": [], "warnings": ["No region files located"], "files": 0}
    records = []
    warnings = []
    for file in files:
        try:
            hits, problems = scan_region(file)
            records.extend(hits)
            warnings.extend(f"{file.relative_to(world)}: {p}" for p in problems)
        except (NBTError, OSError) as exc:
            warnings.append(f"{file.relative_to(world)}: {exc}")
    return {
        "status": "SCANNED_WITH_WARNINGS" if warnings else "SCANNED",
        "files": len(files), "command_blocks": records, "warnings": warnings,
        "note": "Region references show possible command execution, not all triggers. Functions and tags must be analyzed together."
    }

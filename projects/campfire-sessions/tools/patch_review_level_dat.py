#!/usr/bin/env python3
"""Patch the private Campfire village-review save for zero-setup client inspection.

This is deliberately review-pack-only:
- enables commands so the reviewer can use /gamemode, /tp, etc.
- moves the default world spawn onto the authored village plaza.
- gives the review world a clear display name.

It preserves all unrelated NBT tags byte-for-byte semantically through a generic
NBT parser/writer and refuses unexpected level.dat shapes.
"""

from __future__ import annotations

import gzip
import io
import pathlib
import struct
import sys
from dataclasses import dataclass
from typing import Any


TAG_END = 0
TAG_BYTE = 1
TAG_SHORT = 2
TAG_INT = 3
TAG_LONG = 4
TAG_FLOAT = 5
TAG_DOUBLE = 6
TAG_BYTE_ARRAY = 7
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10
TAG_INT_ARRAY = 11
TAG_LONG_ARRAY = 12

REVIEW_SPAWN = (-300, 72, -30)
REVIEW_YAW = 180.0
REVIEW_PITCH = 0.0
REVIEW_LEVEL_NAME = "Campfire Sessions - Village Review"


@dataclass
class Tag:
    tag_id: int
    value: Any
    list_type: int | None = None


def read_exact(stream: io.BytesIO, size: int) -> bytes:
    data = stream.read(size)
    if len(data) != size:
        raise EOFError("unexpected end of NBT stream")
    return data


def read_number(stream: io.BytesIO, fmt: str) -> Any:
    return struct.unpack(fmt, read_exact(stream, struct.calcsize(fmt)))[0]


def read_string(stream: io.BytesIO) -> str:
    size = read_number(stream, ">H")
    return read_exact(stream, size).decode("utf-8")


def read_payload(stream: io.BytesIO, tag_id: int) -> Tag:
    if tag_id == TAG_BYTE:
        return Tag(tag_id, read_number(stream, ">b"))
    if tag_id == TAG_SHORT:
        return Tag(tag_id, read_number(stream, ">h"))
    if tag_id == TAG_INT:
        return Tag(tag_id, read_number(stream, ">i"))
    if tag_id == TAG_LONG:
        return Tag(tag_id, read_number(stream, ">q"))
    if tag_id == TAG_FLOAT:
        return Tag(tag_id, read_number(stream, ">f"))
    if tag_id == TAG_DOUBLE:
        return Tag(tag_id, read_number(stream, ">d"))
    if tag_id == TAG_BYTE_ARRAY:
        size = read_number(stream, ">i")
        return Tag(tag_id, read_exact(stream, size))
    if tag_id == TAG_STRING:
        return Tag(tag_id, read_string(stream))
    if tag_id == TAG_LIST:
        element_type = read_number(stream, ">b")
        size = read_number(stream, ">i")
        return Tag(tag_id, [read_payload(stream, element_type) for _ in range(size)], element_type)
    if tag_id == TAG_COMPOUND:
        values: dict[str, Tag] = {}
        while True:
            child_type = read_number(stream, ">b")
            if child_type == TAG_END:
                break
            name = read_string(stream)
            values[name] = read_payload(stream, child_type)
        return Tag(tag_id, values)
    if tag_id == TAG_INT_ARRAY:
        size = read_number(stream, ">i")
        return Tag(tag_id, [read_number(stream, ">i") for _ in range(size)])
    if tag_id == TAG_LONG_ARRAY:
        size = read_number(stream, ">i")
        return Tag(tag_id, [read_number(stream, ">q") for _ in range(size)])
    raise ValueError(f"unsupported NBT tag id: {tag_id}")


def read_nbt(data: bytes) -> tuple[str, Tag]:
    stream = io.BytesIO(data)
    tag_id = read_number(stream, ">b")
    if tag_id == TAG_END:
        raise ValueError("root NBT tag cannot be TAG_End")
    name = read_string(stream)
    root = read_payload(stream, tag_id)
    if stream.read(1):
        raise ValueError("trailing bytes after root NBT tag")
    return name, root


def write_number(stream: io.BytesIO, fmt: str, value: Any) -> None:
    stream.write(struct.pack(fmt, value))


def write_string(stream: io.BytesIO, value: str) -> None:
    data = value.encode("utf-8")
    if len(data) > 65535:
        raise ValueError("NBT string is too long")
    write_number(stream, ">H", len(data))
    stream.write(data)


def write_payload(stream: io.BytesIO, tag: Tag) -> None:
    if tag.tag_id == TAG_BYTE:
        write_number(stream, ">b", int(tag.value))
    elif tag.tag_id == TAG_SHORT:
        write_number(stream, ">h", int(tag.value))
    elif tag.tag_id == TAG_INT:
        write_number(stream, ">i", int(tag.value))
    elif tag.tag_id == TAG_LONG:
        write_number(stream, ">q", int(tag.value))
    elif tag.tag_id == TAG_FLOAT:
        write_number(stream, ">f", float(tag.value))
    elif tag.tag_id == TAG_DOUBLE:
        write_number(stream, ">d", float(tag.value))
    elif tag.tag_id == TAG_BYTE_ARRAY:
        write_number(stream, ">i", len(tag.value))
        stream.write(bytes(tag.value))
    elif tag.tag_id == TAG_STRING:
        write_string(stream, str(tag.value))
    elif tag.tag_id == TAG_LIST:
        if tag.list_type is None:
            raise ValueError("NBT list is missing its element type")
        write_number(stream, ">b", tag.list_type)
        write_number(stream, ">i", len(tag.value))
        for item in tag.value:
            if item.tag_id != tag.list_type:
                raise ValueError("NBT list contains a mismatched element type")
            write_payload(stream, item)
    elif tag.tag_id == TAG_COMPOUND:
        for name, child in tag.value.items():
            write_number(stream, ">b", child.tag_id)
            write_string(stream, name)
            write_payload(stream, child)
        write_number(stream, ">b", TAG_END)
    elif tag.tag_id == TAG_INT_ARRAY:
        write_number(stream, ">i", len(tag.value))
        for value in tag.value:
            write_number(stream, ">i", int(value))
    elif tag.tag_id == TAG_LONG_ARRAY:
        write_number(stream, ">i", len(tag.value))
        for value in tag.value:
            write_number(stream, ">q", int(value))
    else:
        raise ValueError(f"unsupported NBT tag id: {tag.tag_id}")


def write_nbt(name: str, root: Tag) -> bytes:
    stream = io.BytesIO()
    write_number(stream, ">b", root.tag_id)
    write_string(stream, name)
    write_payload(stream, root)
    return stream.getvalue()


def require_compound(tag: Tag, label: str) -> dict[str, Tag]:
    if tag.tag_id != TAG_COMPOUND:
        raise ValueError(f"{label} is not an NBT compound")
    return tag.value


def patch(path: pathlib.Path) -> None:
    raw = gzip.decompress(path.read_bytes())
    root_name, root = read_nbt(raw)
    root_values = require_compound(root, "root")
    data = require_compound(root_values.get("Data"), "Data")

    spawn = require_compound(data.get("spawn"), "Data.spawn")
    pos = spawn.get("pos")
    if pos is None or pos.tag_id != TAG_LIST or pos.list_type != TAG_INT or len(pos.value) != 3:
        raise ValueError("Data.spawn.pos is not the expected three-int NBT list")

    data["allowCommands"] = Tag(TAG_BYTE, 1)
    data["LevelName"] = Tag(TAG_STRING, REVIEW_LEVEL_NAME)
    pos.value = [Tag(TAG_INT, value) for value in REVIEW_SPAWN]
    spawn["dimension"] = Tag(TAG_STRING, "minecraft:overworld")
    spawn["yaw"] = Tag(TAG_FLOAT, REVIEW_YAW)
    spawn["pitch"] = Tag(TAG_FLOAT, REVIEW_PITCH)

    encoded = write_nbt(root_name, root)
    buffer = io.BytesIO()
    with gzip.GzipFile(fileobj=buffer, mode="wb", mtime=0) as gz:
        gz.write(encoded)
    path.write_bytes(buffer.getvalue())

    # Read back from disk so CI catches writer mistakes before publishing a pack.
    _, verify_root = read_nbt(gzip.decompress(path.read_bytes()))
    verify_data = require_compound(require_compound(verify_root, "root")["Data"], "Data")
    verify_spawn = require_compound(verify_data["spawn"], "Data.spawn")
    verify_pos = tuple(item.value for item in verify_spawn["pos"].value)

    if verify_data["allowCommands"].value != 1:
        raise ValueError("review world commands were not enabled")
    if verify_pos != REVIEW_SPAWN:
        raise ValueError(f"review world spawn mismatch: {verify_pos}")
    if verify_data["LevelName"].value != REVIEW_LEVEL_NAME:
        raise ValueError("review world display name was not updated")

    print("REVIEW_ALLOW_COMMANDS=YES")
    print("REVIEW_SPAWN=" + ",".join(str(v) for v in REVIEW_SPAWN))
    print("REVIEW_LEVEL_NAME=" + REVIEW_LEVEL_NAME)


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: patch_review_level_dat.py <level.dat>", file=sys.stderr)
        return 2
    path = pathlib.Path(sys.argv[1])
    if not path.is_file():
        raise SystemExit(f"level.dat not found: {path}")
    patch(path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

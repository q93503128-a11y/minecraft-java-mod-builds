#!/usr/bin/env python3
from __future__ import annotations

import base64
import gzip
import hashlib
import io
import struct
import zipfile
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[3]
PROJECT = REPO_ROOT / "projects" / "countryside-days"
TOOLS = PROJECT / "tools"

PARTS = [
    ("alpha15v2_00.part", 8000, "05764a738c93db7933e60f6db85fea2d6cf168297536e79146ef28dc1eb96349"),
    ("alpha15v2_01.part", 8000, "f62307daf23ace1c04c1c4b09fdf75e3ba2a24018c550a90558753f00fc6f8b6"),
    ("alpha15v2_02.part", 8000, "647cf040b1669045f8e57f2a0eb67968e81c1753621095be6d3f4be046e00beb"),
    ("alpha15exact_03.part", 8000, "785f1b3d3b25d4482cbac7a7e11eb98436b76882d22935ce0071cfcb369dbcaf"),
    ("alpha15exact_04.part", 8000, "aa843460fb86c04dfff90515fae28237f213dce4dc43fcf6ceb7a5c7480e5a44"),
    ("alpha15v2_05_06.part", 16000, "ba4d245f3e29c0710e903a25c7fc2ada8cd306675bf373b8e001c22d88f931e9"),
    ("alpha15v2_07_08.part", 16000, "17528aa349b2a47a0192ddf71506d270487439261742b3212e8b3407e6846c3d"),
    ("alpha15v2_09_10.part", 16000, "a975b3c098d730d4077d8729118537ebd9c7f96aebd4455c8cb1f8dc1ed270c2"),
    ("alpha15exact_11.part", 8000, "8991340273b26d6601fc54df80adfb83e54b88d58a93bc02b017a3f0d104d5a1"),
    ("alpha15exact_12.part", 8000, "bf10be8d174e92ff42def406331ae70837c1a6f85901bdd4a1a6386f0d386bc1"),
    ("alpha15v2_13_14.part", 16000, "a1dd5c64d5d0524d691a228af3d07c35195a1ec76046947d647d9f080b256f05"),
    ("alpha15v2_15_16.part", 16000, "0b84bdbbb7d5a93b360911a75c608961515cea999960e8027746ef296619418f"),
    ("alpha15v2_17_18.part", 9648, "870c0a5b8faed07c9597402175e58b7659ed41ead761cb3a82f45f9bab6477d6"),
]


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def replace_required(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text("utf-8")
    if old not in text:
        if new in text:
            return
        raise RuntimeError(f"{label}: target not found in {path}")
    path.write_text(text.replace(old, new), "utf-8")


def restore_source() -> None:
    chunks: list[str] = []
    for name, expected_len, expected_sha in PARTS:
        path = TOOLS / name
        if not path.is_file():
            raise FileNotFoundError(f"missing payload part: {path}")
        text = "".join(path.read_text("ascii").split())
        actual_sha = sha256(text.encode("ascii"))
        if len(text) != expected_len or actual_sha != expected_sha:
            raise RuntimeError(
                f"bad payload part {name}: len={len(text)} sha={actual_sha}"
            )
        chunks.append(text)

    encoded = "".join(chunks)
    if len(encoded) != 145648:
        raise RuntimeError(f"bad combined payload length: {len(encoded)}")
    if sha256(encoded.encode("ascii")) != "2215f32a4fda34d490241a811051e9164eb39a4f31831894e9ce3df3361ec4ca":
        raise RuntimeError("bad combined payload text sha")

    archive = base64.b64decode(encoded, validate=True)
    if len(archive) != 109234:
        raise RuntimeError(f"bad source archive length: {len(archive)}")
    if sha256(archive) != "5499bb9fe956c9bf11a6f311105e03bcc694bc2adeed74080431cd882d80b835":
        raise RuntimeError("bad source archive sha")

    with zipfile.ZipFile(io.BytesIO(archive)) as zf:
        bad = zf.testzip()
        if bad:
            raise RuntimeError(f"bad ZIP member: {bad}")
        for name in zf.namelist():
            member = Path(name)
            if not name.startswith("projects/countryside-days/") or member.is_absolute() or ".." in member.parts:
                raise RuntimeError(f"unsafe ZIP member: {name}")
        zf.extractall(REPO_ROOT)

    props = (PROJECT / "gradle.properties").read_text("utf-8")
    if "mod_version=0.1.0-alpha.15" not in props:
        raise RuntimeError("restored project is not alpha.15")


def write_empty_structure(path: Path) -> None:
    def u16(value: str) -> bytes:
        raw = value.encode("utf-8")
        return struct.pack(">H", len(raw)) + raw

    def tag(kind: int, name: str, payload: bytes) -> bytes:
        return bytes([kind]) + u16(name) + payload

    def list_tag(kind: int, items: list[bytes]) -> bytes:
        return bytes([kind]) + struct.pack(">i", len(items)) + b"".join(items)

    def compound(items: list[bytes]) -> bytes:
        return b"".join(items) + b"\x00"

    palette = compound([tag(8, "Name", u16("minecraft:air"))])
    body = compound(
        [
            tag(3, "DataVersion", struct.pack(">i", 0)),
            tag(9, "size", list_tag(3, [struct.pack(">i", 1)] * 3)),
            tag(9, "palette", list_tag(10, [palette])),
            tag(9, "blocks", list_tag(10, [])),
            tag(9, "entities", list_tag(10, [])),
        ]
    )
    payload = bytes([10]) + u16("") + body
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("wb") as raw_file:
        with gzip.GzipFile(filename="", mode="wb", fileobj=raw_file, mtime=0) as gz:
            gz.write(payload)


def apply_fixes() -> None:
    replace_required(
        PROJECT / "src/main/java/kr/countrysidedays/world/CountrysidePropertyManager.java",
        "import net.neoforged.neoforge.event.level.block.FarmlandTrampleEvent;",
        "import net.neoforged.neoforge.event.level.BlockEvent.FarmlandTrampleEvent;",
        "farmland event import",
    )

    replace_required(
        PROJECT / "src/main/java/kr/countrysidedays/gametest/ModGameTests.java",
        """        AABB ranch = new AABB(
                absoluteOrigin.offset(PlayerEstateLayout.RANCH_MIN_X, 0, PlayerEstateLayout.RANCH_MIN_Z),
                absoluteOrigin.offset(PlayerEstateLayout.RANCH_MAX_X + 1, 5, PlayerEstateLayout.RANCH_MAX_Z + 1)
        );""",
        """        BlockPos ranchMin = absoluteOrigin.offset(PlayerEstateLayout.RANCH_MIN_X, 0, PlayerEstateLayout.RANCH_MIN_Z);
        BlockPos ranchMax = absoluteOrigin.offset(PlayerEstateLayout.RANCH_MAX_X + 1, 5, PlayerEstateLayout.RANCH_MAX_Z + 1);
        AABB ranch = new AABB(
                ranchMin.getX(), ranchMin.getY(), ranchMin.getZ(),
                ranchMax.getX(), ranchMax.getY(), ranchMax.getZ()
        );""",
        "GameTest ranch AABB",
    )

    replace_required(
        PROJECT / "src/main/java/kr/countrysidedays/world/PublicVillageExpansionBuilder.java",
        """        AABB bounds = new AABB(
                origin.offset(-58, -2, -48),
                origin.offset(58, 14, 59)
        );""",
        """        BlockPos debrisMin = origin.offset(-58, -2, -48);
        BlockPos debrisMax = origin.offset(58, 14, 59);
        AABB bounds = new AABB(
                debrisMin.getX(), debrisMin.getY(), debrisMin.getZ(),
                debrisMax.getX(), debrisMax.getY(), debrisMax.getZ()
        );""",
        "public village debris AABB",
    )

    replace_required(
        PROJECT / "src/main/java/kr/countrysidedays/gameplay/RestaurantTableManager.java",
        "        display.setSmall(true);\n",
        "",
        "armor stand small flag",
    )

    biome = PROJECT / "src/main/resources/data/countrysidedays/worldgen/biome/rural_plains.json"
    biome.write_text(
        biome.read_text("utf-8").replace(
            "minecraft:trees_birch_and_oak_leaf_litter",
            "minecraft:trees_birch_and_oak",
        ),
        "utf-8",
    )

    structure_dir = PROJECT / "src/main/resources/data/countrysidedays/structure"
    write_empty_structure(structure_dir / "homestead_layout.nbt")
    write_empty_structure(structure_dir / "world_data_round_trip.nbt")


if __name__ == "__main__":
    restore_source()
    apply_fixes()
    print("Countryside Days alpha.15 exact source prepared successfully")

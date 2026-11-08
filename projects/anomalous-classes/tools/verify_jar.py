#!/usr/bin/env python3
"""Reproducible packaging checks for the anomalous-classes runnable mod JAR."""

from __future__ import annotations

import sys
import json
import struct
import zipfile
import zlib
from collections import Counter
from pathlib import Path


def fail(message: str) -> None:
    raise SystemExit(f"JAR VERIFY FAILED: {message}")


def png_chunks(data: bytes) -> list[tuple[bytes, bytes]]:
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        fail("invalid PNG signature")
    chunks: list[tuple[bytes, bytes]] = []
    offset = 8
    while offset + 12 <= len(data):
        length = struct.unpack(">I", data[offset : offset + 4])[0]
        kind = data[offset + 4 : offset + 8]
        payload = data[offset + 8 : offset + 8 + length]
        if len(payload) != length:
            fail("truncated PNG chunk")
        chunks.append((kind, payload))
        offset += 12 + length
        if kind == b"IEND":
            break
    return chunks


def png_dimensions(data: bytes) -> tuple[int, int, int, int]:
    chunks = png_chunks(data)
    if not chunks or chunks[0][0] != b"IHDR" or len(chunks[0][1]) != 13:
        fail("PNG has no valid IHDR")
    width, height, bit_depth, color_type = struct.unpack(">IIBB", chunks[0][1][:10])
    return width, height, bit_depth, color_type


def png_has_transparent_pixel(data: bytes) -> bool:
    chunks = png_chunks(data)
    width, height, bit_depth, color_type = png_dimensions(data)
    if bit_depth != 8 or color_type != 6:
        return False
    compressed = b"".join(payload for kind, payload in chunks if kind == b"IDAT")
    raw = zlib.decompress(compressed)
    stride = width * 4
    prior = bytearray(stride)
    offset = 0
    for _ in range(height):
        filter_type = raw[offset]
        offset += 1
        row = bytearray(raw[offset : offset + stride])
        offset += stride
        for index in range(stride):
            left = row[index - 4] if index >= 4 else 0
            up = prior[index]
            upper_left = prior[index - 4] if index >= 4 else 0
            if filter_type == 1:
                row[index] = (row[index] + left) & 0xFF
            elif filter_type == 2:
                row[index] = (row[index] + up) & 0xFF
            elif filter_type == 3:
                row[index] = (row[index] + (left + up) // 2) & 0xFF
            elif filter_type == 4:
                estimate = left + up - upper_left
                distances = (abs(estimate - left), abs(estimate - up), abs(estimate - upper_left))
                predictor = left if distances[0] <= distances[1] and distances[0] <= distances[2] else (
                    up if distances[1] <= distances[2] else upper_left
                )
                row[index] = (row[index] + predictor) & 0xFF
            elif filter_type != 0:
                fail(f"unsupported PNG filter: {filter_type}")
        if any(row[index] == 0 for index in range(3, stride, 4)):
            return True
        prior = row
    return False


def main() -> None:
    if len(sys.argv) != 2:
        fail("usage: python tools/verify_jar.py <mod.jar>")

    jar_path = Path(sys.argv[1])
    if not jar_path.is_file() or jar_path.stat().st_size == 0:
        fail(f"JAR is missing or empty: {jar_path}")

    try:
        with zipfile.ZipFile(jar_path) as archive:
            names = archive.namelist()
            duplicates = [name for name, count in Counter(names).items() if count > 1]
            if duplicates:
                fail(f"duplicate ZIP entries: {duplicates}")

            required = [
                "META-INF/neoforge.mods.toml",
                "META-INF/MANIFEST.MF",
                "data/anomalousclasses/class_catalog.json",
                "assets/anomalousclasses/lang/ko_kr.json",
                "assets/anomalousclasses/lang/en_us.json",
            ]
            for entry in required:
                if entry not in names:
                    fail(f"required entry missing: {entry}")

            class_files = [name for name in names if name.startswith("kr/moonseungjun/anomalousclasses/") and name.endswith(".class")]
            if not class_files:
                fail("no compiled anomalous-classes .class files found")
            if not any(name.startswith("assets/anomalousclasses/") for name in names):
                fail("assets/anomalousclasses is missing")
            if not any(name.startswith("data/anomalousclasses/") for name in names):
                fail("data/anomalousclasses is missing")

            forbidden_suffixes = (".java", ".md", ".log", ".ps1", ".py")
            forbidden = [name for name in names if name.lower().endswith(forbidden_suffixes)]
            if forbidden:
                fail(f"development files leaked into the JAR: {forbidden}")
            if any(name.startswith((".github/", "tools/", "run/", "build/")) for name in names):
                fail("development directories leaked into the JAR")

            class_keys = (
                "mental_dominator", "puppeteer", "parasite_morpher",
                "barrier_caster", "pact_caster", "swarm_caster",
            )
            for class_key in class_keys:
                card = f"assets/anomalousclasses/textures/gui/class_cards_v3/{class_key}.png"
                icon = f"assets/anomalousclasses/textures/gui/class_icons_v2/{class_key}.png"
                for entry, expected_size in ((card, (512, 288)), (icon, (96, 96))):
                    if entry not in names:
                        fail(f"class UI texture missing: {entry}")
                    width, height, _, _ = png_dimensions(archive.read(entry))
                    if (width, height) != expected_size:
                        fail(f"unexpected texture dimensions for {entry}: {width}x{height}")
                if not png_has_transparent_pixel(archive.read(icon)):
                    fail(f"class icon has no transparent background: {icon}")

            if any("textures/gui/class_cards/" in name or "textures/gui/class_cards_v2/" in name
                   or "textures/gui/class_icons/" in name for name in names):
                fail("archived class UI artwork was incorrectly packaged")

            for language_entry in ("assets/anomalousclasses/lang/ko_kr.json", "assets/anomalousclasses/lang/en_us.json"):
                try:
                    language = json.loads(archive.read(language_entry))
                except (UnicodeDecodeError, json.JSONDecodeError) as error:
                    fail(f"invalid language JSON in {language_entry}: {error}")
                for class_key in class_keys:
                    for suffix in ("", ".description"):
                        key = f"class.anomalousclasses.{class_key}{suffix}"
                        if not language.get(key):
                            fail(f"player-facing class text missing: {language_entry} / {key}")

            metadata = archive.read("META-INF/neoforge.mods.toml").decode("utf-8")
            manifest = archive.read("META-INF/MANIFEST.MF").decode("utf-8")
            for expected in ("modId=\"anomalousclasses\"", "version=\"0.1.0-alpha.1\""):
                if expected not in metadata:
                    fail(f"mod metadata does not contain {expected}")
            for expected in ("Implementation-Version: 0.1.0-alpha.1", "Minecraft-Version: 1.21.1", "NeoForge-Version: 21.1.244"):
                if expected not in manifest:
                    fail(f"manifest does not contain {expected}")
    except zipfile.BadZipFile as error:
        fail(f"not a valid ZIP/JAR: {error}")

    print(f"JAR VERIFY PASS: {jar_path} ({jar_path.stat().st_size} bytes, {len(class_files)} classes)")


if __name__ == "__main__":
    main()

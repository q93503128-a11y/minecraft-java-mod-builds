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
                "data/anomalousclasses/skill_point_cost_model_v2.json",
                "data/anomalousclasses/skill_tree_branch_redesign_v1.json",
                "data/anomalousclasses/skill_tree_graph_layout_v1.json",
                "assets/anomalousclasses/focus_recipes.json",
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

            focus_catalog = json.loads(archive.read("assets/anomalousclasses/focus_recipes.json"))
            focus_recipes = focus_catalog.get("recipes", [])
            if len(focus_recipes) != 30:
                fail(f"expected 30 packaged focus recipe definitions, found {len(focus_recipes)}")
            if "kr/moonseungjun/anomalousclasses/state/ClassFocusService.class" not in names:
                fail("server-authoritative focus slot service is missing from the JAR")
            packaged_focus_recipes = 0
            for definition in focus_recipes:
                class_key, tier = definition.get("class"), definition.get("tier")
                item_id = f"focus_{class_key}_{tier}"
                model_path = f"assets/anomalousclasses/models/item/{item_id}.json"
                texture_path = f"assets/anomalousclasses/textures/item/{item_id}.png"
                recipe_path = f"data/anomalousclasses/recipe/focus/{class_key}/{tier}.json"
                for entry in (model_path, texture_path, recipe_path):
                    if entry not in names:
                        fail(f"focus item or shaped recipe missing from JAR: {entry}")
                if png_dimensions(archive.read(texture_path))[:2] != (16, 16):
                    fail(f"focus item texture is not 16x16: {texture_path}")
                model = json.loads(archive.read(model_path))
                if model.get("textures", {}).get("layer0") != f"anomalousclasses:item/{item_id}":
                    fail(f"focus item model points at the wrong texture: {model_path}")
                recipe = json.loads(archive.read(recipe_path))
                if recipe.get("type") != "minecraft:crafting_shaped" or recipe.get("pattern") != definition.get("pattern"):
                    fail(f"packaged focus recipe is not the canonical shaped pattern: {recipe_path}")
                if recipe.get("result", {}).get("id") != f"anomalousclasses:{item_id}":
                    fail(f"packaged focus recipe has the wrong output: {recipe_path}")
                packaged_focus_recipes += 1
            if packaged_focus_recipes != 30:
                fail("not all six classes and five focus grades were packaged")

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

                skill_icon = f"assets/anomalousclasses/textures/gui/skill_icons_v1/{class_key}.png"
                if skill_icon not in names:
                    fail(f"skill icon atlas missing: {skill_icon}")
                width, height, _, _ = png_dimensions(archive.read(skill_icon))
                if (width, height) != (1024, 1024):
                    fail(f"unexpected skill atlas dimensions for {skill_icon}: {width}x{height}")
                if not png_has_transparent_pixel(archive.read(skill_icon)):
                    fail(f"skill icon atlas has no transparent pixels: {skill_icon}")

            branch_path = Path(__file__).resolve().parents[1] / "balance/SKILL_TREE_BRANCH_REDESIGN_V1.json"
            branch_data = json.loads(branch_path.read_text(encoding="utf-8"))
            branch_keys = {
                "mental_dominator": "mental", "puppeteer": "puppet", "parasite_morpher": "parasite",
                "barrier_caster": "barrier", "pact_caster": "pact", "swarm_caster": "swarm",
            }
            tree_art_entries: set[str] = set()
            for class_key, branch_key in branch_keys.items():
                background = f"assets/anomalousclasses/textures/gui/skilltree_v1/backgrounds/{class_key}.png"
                if background not in names:
                    fail(f"class skill-tree background missing: {background}")
                width, height, _, _ = png_dimensions(archive.read(background))
                if (width, height) != (1672, 941):
                    fail(f"unexpected dimensions for {background}: {width}x{height}")
                for skill in branch_data["classes"][branch_key]["skills"]:
                    tree_art_entries.add(
                        f"assets/anomalousclasses/textures/gui/skilltree_v1/icons/{class_key}/{skill['legacy_id']}.png")
            for stat in ("max_health_hp", "armor", "movement_speed_percent", "attack_damage"):
                tree_art_entries.add(f"assets/anomalousclasses/textures/gui/skilltree_v1/icons/common/{stat}.png")
            if len(tree_art_entries) != 61:
                fail(f"expected 61 distinct skill-tree icon assets, found {len(tree_art_entries)}")
            for entry in tree_art_entries:
                if entry not in names:
                    fail(f"skill-tree node icon missing from JAR: {entry}")
                if png_dimensions(archive.read(entry)) != (256, 256, 8, 6):
                    fail(f"skill-tree node icon is not a 256x256 RGBA texture: {entry}")

            shared_ui_textures = {
                "frame_atlas": (768, 768, True),
                "title_plate": (1024, 342, True),
                "surface_tile": (256, 256, False),
                "button_states": (1024, 1024, True),
                "node_states": (1024, 1024, True),
                "connection_paths": (1024, 1024, True),
                "progress_states": (1024, 1024, True),
            }
            for name, (expected_width, expected_height, needs_alpha) in shared_ui_textures.items():
                entry = f"assets/anomalousclasses/textures/gui/menu_v1/{name}.png"
                if entry not in names:
                    fail(f"shared UI texture missing: {entry}")
                data = archive.read(entry)
                width, height, _, _ = png_dimensions(data)
                if (width, height) != (expected_width, expected_height):
                    fail(f"unexpected dimensions for {entry}: {width}x{height}")
                if needs_alpha and not png_has_transparent_pixel(data):
                    fail(f"shared UI state texture has no transparent pixels: {entry}")

            frame_parts = {
                "corner_tl": (160, 160), "corner_tr": (160, 160),
                "corner_bl": (160, 160), "corner_br": (160, 160),
                "rail_top_left": (24, 64), "rail_top_right": (24, 64),
                "rail_bottom_left": (24, 64), "rail_bottom_right": (24, 64),
                "rail_side_left": (64, 24), "rail_side_right": (64, 24),
                "jewel_side_left": (24, 64), "jewel_side_right": (24, 64),
                "jewel_top": (32, 88), "jewel_bottom": (32, 88),
            }
            for part, dimensions in frame_parts.items():
                entry = f"assets/anomalousclasses/textures/gui/menu_v1/frame_parts/{part}.png"
                if entry not in names:
                    fail(f"isolated frame texture missing from JAR: {entry}")
                data = archive.read(entry)
                if png_dimensions(data) != (*dimensions, 8, 6):
                    fail(f"unexpected dimensions or PNG format for {entry}: {png_dimensions(data)}")

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

            try:
                skill_costs = json.loads(archive.read("data/anomalousclasses/skill_point_cost_model_v2.json"))
            except (UnicodeDecodeError, json.JSONDecodeError) as error:
                fail(f"invalid skill-point data: {error}")
            expected_trees = {
                "mental": (10, 270), "puppet": (10, 290), "parasite": (10, 285),
                "barrier": (9, 300), "pact": (9, 270), "swarm": (9, 270),
            }
            for key, (node_count, rank_count) in expected_trees.items():
                nodes = skill_costs.get("classes", {}).get(key, {}).get("nodes", [])
                if len(nodes) != node_count or sum(node.get("max_rank", 0) for node in nodes) != rank_count:
                    fail(f"unexpected skill tree shape in packaged cost data: {key}")
                if sum(sum(node.get("rank_costs", [])) for node in nodes) != 900:
                    fail(f"skill tree cost does not total 900 SP: {key}")

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

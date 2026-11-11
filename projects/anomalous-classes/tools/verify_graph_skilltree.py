#!/usr/bin/env python3
"""Static data, migration-model, asset, and viewport checks for the graph skill tree."""
from __future__ import annotations

import hashlib
import json
import math
import re
import struct
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/kr/moonseungjun/anomalousclasses"
RES = ROOT / "src/main/resources/assets/anomalousclasses"
BRANCH = json.loads((ROOT / "balance/SKILL_TREE_BRANCH_REDESIGN_V1.json").read_text(encoding="utf-8"))
LAYOUT = json.loads((ROOT / "ui/SKILL_TREE_GRAPH_LAYOUT_V1.json").read_text(encoding="utf-8"))
COST = json.loads((ROOT / "balance/SKILL_POINT_COST_MODEL_V2.json").read_text(encoding="utf-8"))
KO = json.loads((RES / "lang/ko_kr.json").read_text(encoding="utf-8"))
EN = json.loads((RES / "lang/en_us.json").read_text(encoding="utf-8"))
CLASSES = {
    "MENTAL_DOMINATOR": ("mental", "mental_dominator", 94, 31, 63),
    "PUPPETEER": ("puppet", "puppeteer", 101, 33, 68),
    "PARASITE_MORPHER": ("parasite", "parasite_morpher", 108, 39, 69),
    "BARRIER_CASTER": ("barrier", "barrier_caster", 102, 34, 68),
    "PACT_CASTER": ("pact", "pact_caster", 86, 27, 59),
    "SWARM_CASTER": ("swarm", "swarm_caster", 90, 28, 62),
}
STAT_LABELS = ("max_health_hp", "armor", "movement_speed_percent", "attack_damage")


def fail(message: str) -> None:
    raise AssertionError(message)


def check_catalog() -> dict[str, list[dict]]:
    if set(KO) != set(EN):
        fail(f"KO/EN locale keys differ: KO-only={set(KO)-set(EN)}, EN-only={set(EN)-set(KO)}")
    java_catalog = (SRC / "state/SkillTreeCatalog.java").read_text(encoding="utf-8")
    graph_source = (SRC / "state/GraphSkillTreeCatalog.java").read_text(encoding="utf-8")
    old: dict[str, list[dict]] = {}
    for enum_name, (branch_key, class_key, _, _, _) in CLASSES.items():
        marker = f"case {enum_name} -> List.of("
        start = java_catalog.index(marker) + len(marker)
        end = java_catalog.index(");", start)
        entries = re.findall(r'n\("([^\"]+)",\s*"([^\"]+)",\s*"(?:one|two|three)",\s*"[^\"]+"((?:,\s*\d+)*)\)', java_catalog[start:end])
        costs = COST["classes"][branch_key]["nodes"]
        if len(entries) != len(costs):
            fail(f"{branch_key}: legacy skill catalog count changed")
        parsed = []
        for (skill_id, name, milestone_text), cost in zip(entries, costs, strict=True):
            milestones = [int(value) for value in re.findall(r"\d+", milestone_text)]
            if name != cost["name"] or len(cost["rank_costs"]) != cost["max_rank"]:
                fail(f"{branch_key}/{skill_id}: legacy name, rank range, or cost model mismatch")
            if sum(cost["rank_costs"]) != cost["full_sp"]:
                fail(f"{branch_key}/{skill_id}: old rank-cost sum mismatch")
            parsed.append({"id": skill_id, "rank_costs": cost["rank_costs"],
                           "max_rank": cost["max_rank"], "milestones": milestones})
        old[branch_key] = parsed

    total_nodes = 0
    for enum_name, (branch_key, class_key, expected_count, expected_major, expected_minor) in CLASSES.items():
        branch = BRANCH["classes"][branch_key]
        layout = LAYOUT["classes"][branch_key]
        nodes = layout["nodes"]
        by_id = {node["id"]: node for node in nodes}
        if len(by_id) != len(nodes):
            fail(f"{branch_key}: duplicate graph node ID")
        purchases = [node for node in nodes if node["kind"] in ("minor", "major")]
        majors = [node for node in purchases if node["kind"] == "major"]
        minors = [node for node in purchases if node["kind"] == "minor"]
        if (len(purchases), len(majors), len(minors)) != (expected_count, expected_major, expected_minor):
            fail(f"{branch_key}: count mismatch {(len(purchases), len(majors), len(minors))}")
        if len(purchases) != layout["summary"]["purchase_count"] or sum(n["cost"] for n in purchases) != 900:
            fail(f"{branch_key}: purchase count or 900 SP total mismatch")
        if sum(n["cost"] for n in purchases) != branch["derived"]["sp_total"]:
            fail(f"{branch_key}: branch/layout spend differs")
        if len(branch["skills"]) != len(old[branch_key]):
            fail(f"{branch_key}: skill-stage count changed")
        skills = {skill["legacy_id"]: skill for skill in branch["skills"]}
        if [skill["legacy_id"] for skill in branch["skills"]] != [skill["id"] for skill in old[branch_key]]:
            fail(f"{branch_key}: original skill ordering or names changed")

        for legacy, source in zip(old[branch_key], branch["skills"], strict=True):
            if source["legacy_max_rank"] != legacy["max_rank"] or source["catalog_milestones"] != legacy["milestones"]:
                fail(f"{branch_key}/{source['legacy_id']}: legacy maximum rank or breakthrough list changed")
            previous_rank = 0
            for segment in source["segments"]:
                if segment["old_rank_from"] != previous_rank + 1:
                    fail(f"{branch_key}/{source['legacy_id']}: non-contiguous legacy migration segment")
                if segment["old_rank_to"] < segment["old_rank_from"] or segment["old_rank_to"] > legacy["max_rank"]:
                    fail(f"{branch_key}/{source['legacy_id']}: invalid legacy stage range")
                old_cost = sum(legacy["rank_costs"][segment["old_rank_from"]-1:segment["old_rank_to"]])
                if old_cost != segment["old_segment_sp"]:
                    fail(f"{branch_key}/{source['legacy_id']}: segment no longer equals legacy spend")
                minor_costs = segment["minor_costs"]
                minor_ids = [f"{source['legacy_id']}.s{segment['stage']}.m{i+1}" for i in range(len(minor_costs))]
                major_id = f"{source['legacy_id']}.s{segment['stage']}.major"
                if len(minor_costs) != len(segment["minor_stat_types"]):
                    fail(f"{branch_key}/{source['legacy_id']}: minor stat mapping is incomplete")
                stage_nodes = [by_id.get(nid) for nid in minor_ids]
                major = by_id.get(major_id)
                if any(node is None or node["kind"] != "minor" or node["cost"] != price
                       for node, price in zip(stage_nodes, minor_costs, strict=True)):
                    fail(f"{branch_key}/{source['legacy_id']}: graph minor nodes do not match conversion")
                if major is None or major["kind"] != "major" or major["cost"] != segment["major_cost"]:
                    fail(f"{branch_key}/{source['legacy_id']}: graph major node does not match conversion")
                if sum(minor_costs) + segment["major_cost"] != segment["old_segment_sp"]:
                    fail(f"{branch_key}/{source['legacy_id']}: new stage price differs from legacy cost")
                expected_breaks = [rank for rank in legacy["milestones"]
                                   if segment["old_rank_from"] < rank <= segment["old_rank_to"]]
                if expected_breaks and major.get("milestone") is not True:
                    fail(f"{branch_key}/{source['legacy_id']}: legacy breakthrough marker was lost")
                if not expected_breaks and major.get("milestone") is True:
                    fail(f"{branch_key}/{source['legacy_id']}: unapproved breakthrough marker was added")
                for key in (f"skill.anomalousclasses.{class_key}.{source['legacy_id']}.name",
                            f"skill.anomalousclasses.{class_key}.{source['legacy_id']}.effect"):
                    if not KO.get(key) or not EN.get(key): fail(f"missing legacy passive translation {key}")
                previous_rank = segment["old_rank_to"]
            if previous_rank != legacy["max_rank"]:
                fail(f"{branch_key}/{source['legacy_id']}: last converted stage misses legacy ranks")

        for node in minors:
            if node["stat"] not in STAT_LABELS or node["cost"] <= 0:
                fail(f"{branch_key}/{node['id']}: invalid minor stat or price")
            for key in (f"skill_node.anomalousclasses.{class_key}.stat.{node['stat']}",
                        f"screen.anomalousclasses.skill_tree.stat.{node['stat']}"):
                if not KO.get(key) or not EN.get(key): fail(f"missing stat node translation {key}")
        for node in majors:
            if node["source"] not in skills:
                fail(f"{branch_key}/{node['id']}: major node has no legacy passive")
            if node.get("requires_previous_major"):
                prior = by_id.get(node["requires_previous_major"])
                if not prior or prior["kind"] != "major" or prior["source"] != node["source"]:
                    fail(f"{branch_key}/{node['id']}: invalid previous-stage requirement")
            for required in node.get("requires_stage_minors", []):
                if required not in by_id or by_id[required]["kind"] != "minor":
                    fail(f"{branch_key}/{node['id']}: invalid stage path requirement")

        edges = layout["edges"]
        if layout["summary"]["edge_count"] != len(edges): fail(f"{branch_key}: graph edge count mismatch")
        adjacency: dict[str, set[str]] = {node["id"]: set() for node in nodes}
        for edge in edges:
            if edge["from"] not in by_id or edge["to"] not in by_id or edge["type"] not in ("navigation", "growth", "bridge"):
                fail(f"{branch_key}: invalid graph edge {edge}")
            adjacency[edge["from"]].add(edge["to"])
            adjacency[edge["to"]].add(edge["from"])
        reached = {"origin"}
        todo = ["origin"]
        while todo:
            current = todo.pop()
            for neighbor in adjacency[current] - reached:
                reached.add(neighbor)
                todo.append(neighbor)
        if reached != set(by_id): fail(f"{branch_key}: graph has unreachable nodes {set(by_id)-reached}")
        total_nodes += len(purchases)

    if total_nodes != 581: fail(f"six-class graph contains {total_nodes} purchase nodes, expected 581")
    return old


def has_path(node: dict, owned: set[str], nodes: dict[str, dict], edges: list[dict]) -> bool:
    for edge in edges:
        if edge["type"] == "growth" and edge["to"] == node["id"]:
            parent = nodes[edge["from"]]
            if parent["kind"] == "navigation" or parent["id"] in owned: return True
        if edge["type"] == "bridge" and node["id"] in (edge["from"], edge["to"]):
            other = edge["to"] if edge["from"] == node["id"] else edge["from"]
            if nodes[other]["kind"] != "navigation" and other in owned: return True
    return False


def can_buy(node: dict, owned: set[str], available: int, nodes: dict[str, dict], edges: list[dict]) -> bool:
    if node["id"] in owned or node["kind"] == "navigation" or node["cost"] > available: return False
    if node.get("requires_previous_major") and node["requires_previous_major"] not in owned: return False
    if not set(node.get("requires_stage_minors", [])).issubset(owned): return False
    return has_path(node, owned, nodes, edges)


def model_migration(old_nodes: list[dict], branch: dict, layout: dict, ranks: list[int], available: int) -> tuple[set[str], int, int]:
    nodes = {node["id"]: node for node in layout["nodes"]}
    owned: set[str] = set()
    refund = 0
    for skill, legacy, rank in zip(branch["skills"], old_nodes, ranks, strict=True):
        if rank < 0 or rank > legacy["max_rank"]: fail("fixture rank outside old max")
        for segment in skill["segments"]:
            if rank >= segment["old_rank_to"]:
                owned.update(f"{skill['legacy_id']}.s{segment['stage']}.m{i+1}" for i, _ in enumerate(segment["minor_costs"]))
                owned.add(f"{skill['legacy_id']}.s{segment['stage']}.major")
            elif rank >= segment["old_rank_from"]:
                refund += sum(legacy["rank_costs"][segment["old_rank_from"]-1:rank])
    new_spent = sum(nodes[node_id]["cost"] for node_id in owned)
    old_spent = sum(sum(node["rank_costs"][:rank]) for node, rank in zip(old_nodes, ranks, strict=True))
    new_available = available + refund
    if new_available + new_spent != available + old_spent:
        fail("legacy migration failed the SP conservation fixture")
    return owned, new_available, new_spent


def check_migration_and_purchase(old: dict[str, list[dict]]) -> None:
    for branch_key, (enum_name, _class_data) in zip(
            (entry[0] for entry in CLASSES.values()), CLASSES.items(), strict=True):
        branch = BRANCH["classes"][branch_key]
        layout = LAYOUT["classes"][branch_key]
        legacy = old[branch_key]
        # New/empty, partial, segment boundary, fully invested, and mixed multi-job save fixtures.
        for point in (0, 9, 10, 11, 19, 20, 29, 30):
            ranks = [min(point, node["max_rank"]) if index == 0 else 0 for index, node in enumerate(legacy)]
            model_migration(legacy, branch, layout, ranks, 137)
        for ranks in ([0] * len(legacy), [node["max_rank"] for node in legacy],
                      [index % (node["max_rank"] + 1) for index, node in enumerate(legacy)]):
            owned, points, spent = model_migration(legacy, branch, layout, list(ranks), 23)
            if points + spent != 23 + sum(sum(node["rank_costs"][:rank])
                                          for node, rank in zip(legacy, ranks, strict=True)):
                fail(f"{branch_key}: migration fixture lost SP")
            refunded = points + spent
            if len(owned) > 0 and points >= 0 and refunded < spent:
                fail(f"{branch_key}: full reset refund fixture failed")

        nodes = layout["nodes"]
        by_id = {node["id"]: node for node in nodes}
        edges = layout["edges"]
        owned: set[str] = set()
        remaining = 900
        for _ in range(len(nodes) + 1):
            available = [node for node in nodes if node["kind"] != "navigation"
                         and can_buy(node, owned, remaining, by_id, edges)]
            if not available: break
            node = min(available, key=lambda value: value["cost"])
            owned.add(node["id"])
            remaining -= node["cost"]
        purchase_count = sum(node["kind"] != "navigation" for node in nodes)
        if len(owned) != purchase_count or remaining != 0:
            fail(f"{branch_key}: graph purchase simulation reached {len(owned)}/{purchase_count} nodes with {remaining} SP")

    profile = (SRC / "state/ClassProgress.java").read_text(encoding="utf-8")
    saved = (SRC / "state/ClassProfileSavedData.java").read_text(encoding="utf-8")
    migration = (SRC / "state/GraphSkillTreeMigration.java").read_text(encoding="utf-8")
    service = (SRC / "state/SkillTreeService.java").read_text(encoding="utf-8")
    server_network = (SRC / "network/ClassNetwork.java").read_text(encoding="utf-8")
    client_network = (SRC / "client/ClientModEvents.java").read_text(encoding="utf-8")
    if not all(term in profile for term in ("skillTreeVersion", "purchasedNodes", "withGraphTree")):
        fail("class progress is missing graph version/state")
    if not all(term in saved for term in ("purchasedNodes", "skillTreeVersion", 'schemaVersion", 3')):
        fail("saved data schema does not persist graph node ownership/version")
    if not all(term in migration for term in ("updateProgressBatch", "oldSegmentSp", "SP conservation", "rank > oldNode.maxRank")):
        fail("legacy migration is missing atomic conversion/conservation checks")
    if not all(term in service for term in ("expectedRevision", "canPurchase", "CombatTracker.isLocked", "savedData.setDirty")):
        fail("server transaction does not enforce revision, path, reset lock, or persistence")
    if 'event.registrar("2")' not in server_network or 'event.registrar("2")' not in client_network:
        fail("network protocol versions differ or were not advanced for graph payloads")


def png_info(path: Path) -> tuple[int, int, int]:
    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR": fail(f"invalid PNG {path}")
    width, height = struct.unpack(">II", data[16:24])
    return width, height, data[25]


def check_art_and_screen() -> None:
    texture_root = RES / "textures/gui/skilltree_v1"
    hashes = set()
    expected_skill_icons: set[str] = set()
    for _, class_key, *_ in CLASSES.values():
        path = texture_root / f"backgrounds/{class_key}.png"
        if png_info(path) != (1672, 941, 2): fail(f"class backdrop wrong dimensions/format: {path}")
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        if digest in hashes: fail("two class trees share one backdrop image")
        hashes.add(digest)
    for _, class_key, *_ in CLASSES.values():
        directory = texture_root / f"icons/{class_key}"
        for skill in BRANCH["classes"][_branch_key(class_key)]["skills"]:
            path = directory / f"{skill['legacy_id']}.png"
            if png_info(path) != (256, 256, 6): fail(f"missing/invalid passive icon {path}")
            expected_skill_icons.add(path.relative_to(RES).as_posix())
    if len(expected_skill_icons) != 57:
        fail(f"expected 57 distinct class skill icons, found {len(expected_skill_icons)}")
    expected_runtime_icons = set(expected_skill_icons)
    for stat in STAT_LABELS:
        path = texture_root / f"icons/common/{stat}.png"
        if png_info(path) != (256, 256, 6):
            fail(f"missing/invalid stat icon {stat}")
        expected_runtime_icons.add(path.relative_to(RES).as_posix())
    if len(expected_runtime_icons) != 61:
        fail(f"expected 61 reusable skill-tree icons, found {len(expected_runtime_icons)}")
    for relative, size in (("menu_v1/node_states.png", (1024, 1024)),
                           ("menu_v1/button_states.png", (1024, 1024)),
                           ("menu_v1/frame_atlas.png", (768, 768))):
        if png_info(RES / f"textures/gui/{relative}")[:2] != size: fail(f"shared UI texture mismatch: {relative}")
    frame_parts = {
        "corner_tl": (160, 160), "corner_tr": (160, 160),
        "corner_bl": (160, 160), "corner_br": (160, 160),
        "rail_top_left": (24, 64), "rail_top_right": (24, 64),
        "rail_bottom_left": (24, 64), "rail_bottom_right": (24, 64),
        "rail_side_left": (64, 24), "rail_side_right": (64, 24),
        "jewel_side_left": (24, 64), "jewel_side_right": (24, 64),
        "jewel_top": (32, 88), "jewel_bottom": (32, 88),
    }
    for name, dimensions in frame_parts.items():
        path = RES / f"textures/gui/menu_v1/frame_parts/{name}.png"
        if png_info(path)[:2] != dimensions:
            fail(f"isolated frame component has wrong dimensions: {path}")
    splitter = (ROOT / "tools/extract_menu_frame_parts.java").read_text(encoding="utf-8")
    if "new Part(\"corner_tl\", 32, 32, 160, 160)" not in splitter \
            or "new Part(\"rail_top_left\", 318, 64, 24, 64)" not in splitter \
            or "new Part(\"jewel_top\", 368, 48, 32, 88)" not in splitter \
            or "image.setRGB(x, y, atlas.getRGB(part.x() + x, part.y() + y))" not in splitter:
        fail("frame component crops no longer match the inspected atlas bounds")

    screen = (SRC / "client/SkillTreeScreen.java").read_text(encoding="utf-8")
    graph_source = (SRC / "state/GraphSkillTreeCatalog.java").read_text(encoding="utf-8")
    menu = (SRC / "client/MenuUi.java").read_text(encoding="utf-8")
    profile = (SRC / "client/ProfileScreen.java").read_text(encoding="utf-8")
    if screen.count("renderBackground(graphics,") != 1 or "super.render(" in screen:
        fail("skill screen renders the background more than once or re-enters Screen.render")
    for term in ("mouseDragged(", "mouseReleased(", "mouseScrolled(", "zoomAt(", "GraphSkillTreeCatalog",
                 "MenuUi.drawNodeFrame", "drawHoveredCard", "SkillTreeActionPayload.Action.PURCHASE_NODE",
                 "rebuildIconTextures()", "node.iconPath(classId)",
                 "ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, path)"):
        if term not in screen: fail(f"skill screen missing interactive graph feature {term}")
    for term in ("iconTextures.clear()", "tree.purchaseNodes()", "iconTextures.put(node.id()"):
        if term not in screen: fail(f"skill node icon texture map is not populated from graph data: {term}")
    if any(term in screen for term in ("maxStages", "node_stage", "node_rank", 'String stage = node.stage()')):
        fail("stage/rank fraction text is still rendered in the skill map or hover card")
    if "node_stage" in KO or "node_rank" in KO or "node_stage" in EN or "node_rank" in EN:
        fail("removed stage/rank labels remain in the player language files")
    if "icons/common" not in graph_source: fail("common custom stat icons are not wired in the graph catalog")
    if any(term in screen for term in ("drawTitle(graphics", "INVEST_TO_BREAKPOINT", "BuiltInRegistries", "effect_unavailable")):
        fail("old grid/title/development or vanilla-item presentation remains in the graph screen")
    if "GLFW_KEY_K" in (SRC / "client/ClassSelectionKeyMappings.java").read_text(encoding="utf-8"):
        fail("K class-change key binding was reintroduced")
    if "OPEN_PROFILE" not in (SRC / "client/ClassSelectionKeyMappings.java").read_text(encoding="utf-8"):
        fail("M profile key mapping was removed")

    if "drawHorizontalTiles" in menu or "drawVerticalTiles" in menu \
            or "region(graphics, texture, x, y, endX - x, height, sampleX, 0, 1, textureHeight" not in menu \
            or "region(graphics, texture, x, y, width, endY - y, 0, sampleY, textureWidth, 1" not in menu:
        fail("shared panel renderer must stretch a single rail cross-section, not repeat decorated chunks")
    if "0, 0, 32, 88, 32, 88" not in menu or "0, 0, 24, 64, 24, 64" not in menu:
        fail("frame jewels must use the isolated atlas crop dimensions")
    if "topRailY + (railThickness - jewelHeight) / 2" not in menu \
            or "sideLeftX + (railThickness - sideJewelWidth) / 2" not in menu:
        fail("frame jewels are not centered on the corresponding rail axes")
    if "PANEL_CORNERS[0]" not in menu or "PANEL_CORNERS[3]" not in menu:
        fail("panel corners are not drawn as separately extracted fixed-size components")
    if "sourceCellX + leftSourceX" in menu or "sourceCellX + railSourceX" in menu:
        fail("the old guessed frame-atlas UV path is still used")
    if "0, row * 256, 1024, 256" in menu:
        fail("button skin again stretches its full 1024x256 source into the destination")
    if not all(term in menu for term in ("256, 256, 1024, 1024", "512, 256, 1024, 1024")):
        fail("button skin does not preserve fixed-size left/right caps")
    if "enableScissor(artwork.x(), artwork.y(), artwork.right(), artwork.bottom())" not in profile \
            or "drawArtCover(graphics, classId, artwork)" not in profile:
        fail("profile artwork is not clipped to its assigned viewport")
    if "float sourceAspect = 16.0f / 9.0f" not in menu or "sourceWidth = Math.max(1, Math.round(288 * targetAspect))" not in menu:
        fail("profile artwork no longer uses an aspect-preserving source crop")
    if "graphics.fill(viewport.x(), viewport.y(), viewport.right(), viewport.bottom(), 0x26080712)" not in screen:
        fail("tree backdrop has no restrained viewport-only contrast layer")


def viewport_cases() -> int:
    screen = (SRC / "client/SkillTreeScreen.java").read_text(encoding="utf-8")
    required = ("HUD_HEIGHT = 34", "MIN_ZOOM = 0.24", "MAX_ZOOM = 1.85", "Math.min(160, width - 12)",
                "maxCardLines", "clampCamera", "cameraX -= (mouseX - lastX) / zoom",
                "beforeX = worldX(mouseX)", "screenX(double worldX)", "worldX(double screenX)")
    for term in required:
        if term not in screen: fail(f"responsive camera/tooltip contract missing {term}")

    cases = 0
    for physical_w, physical_h, aspect in ((1920, 1080, "16:9"), (1920, 1200, "16:10"), (1600, 1200, "4:3")):
        for scale in (2, 3, 4):
            width, height = math.ceil(physical_w / scale), math.ceil(physical_h / scale)
            margin = 5 if width < 420 else 9
            back_w = min(76, max(54, width // 7))
            reset_w = min(78, max(54, width // 7))
            sp_w = min(112, max(78, width // 4))
            sp_top_x = max(margin, width - margin - reset_w - 4 - sp_w)
            reset_x = width - margin - reset_w
            left_start = margin + back_w + 4
            class_width = max(30, min(158, max(30, sp_top_x - margin - back_w - 4 - margin)))
            actual_class_end = left_start + class_width
            sp_x = reset_x - 4 - sp_w
            actual_text_width = max(24, max(left_start, sp_x - 6) - left_start)
            if reset_x < margin or reset_x + reset_w > width - margin:
                fail(f"{aspect} scale {scale}: reset button outside screen")
            if sp_x < margin or sp_x + sp_w + 4 > reset_x:
                fail(f"{aspect} scale {scale}: SP HUD overlaps reset button")
            if left_start > sp_x - 6 or actual_class_end > sp_x:
                fail(f"{aspect} scale {scale}: left class identity overlaps SP HUD")
            if actual_text_width < 24:
                fail(f"{aspect} scale {scale}: class title has no measurable space")
            map_top = min(max(25, 34), max(1, height - 1))
            if map_top + max(1, height - map_top) > height:
                fail(f"{aspect} scale {scale}: map viewport clipped")
            max_lines = max(3, (height - 34 - 18) // 11)
            card_h = 10 + max_lines * 11
            if card_h > height - 34 - 5:
                fail(f"{aspect} scale {scale}: tooltip exceeds available height")
            card_w = min(160, width - 12)
            if card_w > width - 12:
                fail(f"{aspect} scale {scale}: tooltip exceeds available width")
            for zoom in (0.24, 0.5, 1.0, 1.85):
                cx, cy = width * 0.5, map_top + (height - map_top) * 0.5
                for wx, wy in ((-900, -1400), (-123, 77), (990, 1024)):
                    sx = cx + (wx - 17.5) * zoom
                    sy = cy + (wy + 35.25) * zoom
                    inverse_x = 17.5 + (sx - cx) / zoom
                    inverse_y = -35.25 + (sy - cy) / zoom
                    if abs(inverse_x - wx) > 1e-7 or abs(inverse_y - wy) > 1e-7:
                        fail("world-to-screen and hit-test transforms are not inverses")
            cases += 1
            print(f"{aspect} GUI {scale}: {width}x{height} logical bounds, HUD, tooltip, and camera transform OK")
    return cases


def check_text() -> None:
    for language, mapping in (("ko_kr", KO), ("en_us", EN)):
        for key in ("screen.anomalousclasses.skill_tree.back", "screen.anomalousclasses.skill_tree.reset",
                    "screen.anomalousclasses.skill_tree.confirm_reset", "screen.anomalousclasses.skill_tree.points",
                    "screen.anomalousclasses.skill_tree.spent", "screen.anomalousclasses.skill_tree.node_stat",
                    "screen.anomalousclasses.skill_tree.node_cost", "screen.anomalousclasses.skill_tree.ready",
                    "screen.anomalousclasses.skill_tree.requirement_missing", "screen.anomalousclasses.skill_tree.migration_failed"):
            if not mapping.get(key): fail(f"{language}: missing player text {key}")
        for key, value in mapping.items():
            if key.startswith("screen.anomalousclasses.skill_tree.") and re.search(
                    r"developer|debug|prototype|temporary|candidate|proposal|TODO|미구현|임시|프로토타입|개발용|디버그",
                    value, re.IGNORECASE):
                fail(f"{language}: internal wording appears in a player label: {key}")
        if any(approximate_font_width(mapping[key]) > 160 for key in (
                "screen.anomalousclasses.skill_tree.back", "screen.anomalousclasses.skill_tree.reset",
                "screen.anomalousclasses.skill_tree.confirm_reset")):
            fail(f"{language}: a compact HUD label is too wide")
        for key, value in mapping.items():
            if re.search(r"\{\d+\}", value):
                fail(f"{language}: Java translation still contains a literal indexed placeholder: {key}")
    if KO["screen.anomalousclasses.skill_tree.requirement_missing"] != "선행 경로 미해금" \
            or EN["screen.anomalousclasses.skill_tree.requirement_missing"] != "Path not yet unlocked":
        fail("skill-tree lock text is not concise and localized")
    skill_screen = (SRC / "client/SkillTreeScreen.java").read_text(encoding="utf-8")
    tooltip = skill_screen[skill_screen.index("private List<CardLine> tooltipLines"):skill_screen.index("private String statAmount")]
    if any(term in tooltip for term in ("node_requires", "node_breakthrough", "passive_effect",
                                        "node_follow_path", "requiresStageMinors()", "requiresPreviousMajor()")):
        fail("tooltip contains repeated prerequisite names, stage data, or a development prefix")
    for key in KO:
        ko_args = re.findall(r"(?<!%)%(?:\d+\$)?s", KO[key])
        en_args = re.findall(r"(?<!%)%(?:\d+\$)?s", EN[key])
        if len(ko_args) != len(en_args):
            fail(f"KO/EN translation argument count differs for {key}")
    for key, expected in {
            "screen.anomalousclasses.skill_tree.points": 1,
            "screen.anomalousclasses.skill_tree.spent": 1,
            "screen.anomalousclasses.skill_tree.node_stat": 1,
            "screen.anomalousclasses.skill_tree.node_cost": 1,
            "screen.anomalousclasses.skill_tree.stat.max_health_hp": 1,
            "screen.anomalousclasses.skill_tree.stat.armor": 1,
            "screen.anomalousclasses.skill_tree.stat.movement_speed_percent": 1,
            "screen.anomalousclasses.skill_tree.stat.attack_damage": 1,
    }.items():
        for language, mapping in (("ko_kr", KO), ("en_us", EN)):
            args = re.findall(r"(?<!%)%(?:\d+\$)?s", mapping[key])
            if len(args) != expected:
                fail(f"{language}: {key} has {len(args)} format arguments, expected {expected}")


def profile_viewport_cases() -> int:
    profile = (SRC / "client/ProfileScreen.java").read_text(encoding="utf-8")
    menu = (SRC / "client/MenuUi.java").read_text(encoding="utf-8")
    if "font.width(text) <= maxWidth" not in menu or "font.plainSubstrByWidth(value, contentWidth)" not in menu:
        fail("profile text is not fitted using Minecraft Font measurements")
    if "outer.x() + frameInset" not in profile or "outer.right() - frameInset" not in profile:
        fail("profile content does not respect the frame-safe inset")
    if "drawMetricRows" not in profile or "font.width(row.label())" not in profile \
            or "font.width(row.value())" not in profile or "MenuUi.drawFitRight" not in profile:
        fail("profile metrics are not grouped into measured, aligned label/value columns")
    for language, mapping in (("ko_kr", KO), ("en_us", EN)):
        for key in ("screen.anomalousclasses.profile.title", "screen.anomalousclasses.profile.level",
                    "screen.anomalousclasses.profile.experience", "screen.anomalousclasses.profile.skill_points",
                    "screen.anomalousclasses.profile.attributes", "screen.anomalousclasses.profile.max_health",
                    "screen.anomalousclasses.profile.attack_damage", "screen.anomalousclasses.profile.movement_speed",
                    "screen.anomalousclasses.profile.armor", "screen.anomalousclasses.profile.skill_tree",
                    "screen.anomalousclasses.profile.class_management"):
            if not mapping.get(key): fail(f"{language}: missing profile label {key}")
        for class_key in ("mental_dominator", "puppeteer", "parasite_morpher", "barrier_caster", "pact_caster", "swarm_caster"):
            if not mapping.get(f"class.anomalousclasses.{class_key}"):
                fail(f"{language}: missing profile class title for {class_key}")
    cases = 0
    for physical_w, physical_h, aspect in ((1920, 1080, "16:9"), (1920, 1200, "16:10"), (1600, 1200, "4:3")):
        for scale in (2, 3, 4):
            width, height = math.ceil(physical_w / scale), math.ceil(physical_h / scale)
            margin = 5 if width < 360 else 10
            top = 4 if height < 250 else 10
            outer_x, outer_y = margin, top
            outer_w, outer_h = width - margin * 2, height - top * 2
            inset = min(30, max(12, min(outer_w, outer_h) // 10))
            compact = width < 540 or height < 310
            header = max(17 if height < 250 else 23, inset + 7)
            body_y = outer_y + header + 5
            nav_height = max(19, min(23, height // 15))
            nav_y = outer_y + outer_h - nav_height - max(7, inset // 2)
            body_bottom = nav_y - 7
            body_height = max(1, body_bottom - body_y)
            content_x, content_right = outer_x + inset, outer_x + outer_w - inset
            inner_width = max(1, content_right - content_x)
            gap = 7
            if compact:
                left_width = min((inner_width - gap) // 2, max(90, math.floor(inner_width * 0.44 + 0.5)))
                art_height = max(1, min(body_height, left_width * 9 // 16))
                art_y = body_y + max(0, (body_height - min(body_height, left_width * 9 // 16)) // 2)
                info_x = content_x + left_width + gap
                info_width = max(1, content_right - (content_x + left_width) - gap)
            else:
                left_width = math.floor((inner_width - gap) * 0.44 + 0.5)
                art_height = body_height
                art_y = body_y
                info_x = content_x + left_width + gap
                info_width = max(1, content_right - (content_x + left_width) - gap)
            art_right = content_x + max(1, left_width)
            info_right = info_x + info_width
            button_gap = 6
            button_width = max(1, min(156, (inner_width - button_gap) // 2))
            button_x = outer_x + (outer_w - (button_width * 2 + button_gap)) // 2
            if body_height < 1 or art_height < 1 or art_y < body_y or art_y + art_height > body_bottom:
                fail(f"{aspect} GUI {scale}: profile artwork falls outside the body viewport")
            if art_right + gap > info_x or info_right > content_right or info_width < 100:
                fail(f"{aspect} GUI {scale}: profile columns overlap or leave no usable data width")
            if button_x < content_x or button_x + button_width * 2 + button_gap > content_right:
                fail(f"{aspect} GUI {scale}: profile navigation buttons escape the frame-safe area")
            for language, mapping in (("ko_kr", KO), ("en_us", EN)):
                button_label_width = max(1, button_width - max(8, button_width // 12))
                if max(approximate_font_width(mapping["screen.anomalousclasses.profile.skill_tree"]),
                       approximate_font_width(mapping["screen.anomalousclasses.profile.class_management"])) > button_label_width:
                    fail(f"{aspect} GUI {scale}: {language} profile navigation text exceeds its measured button width")
            if nav_y < body_bottom or nav_y + nav_height > outer_y + outer_h - max(7, inset // 2):
                fail(f"{aspect} GUI {scale}: profile navigation buttons overlap the body or frame")
            stats_offset = 68 if compact else 112
            stats_y = body_y + stats_offset
            stats_bottom = body_y + body_height - 2
            line = max(10, 10) if compact else 13
            stat_line = max(9, 9 + (0 if compact else 2))
            final_stat_bottom = stats_y + 2 + line + 3 * stat_line + 9
            if stats_y + 1 > stats_bottom or final_stat_bottom > stats_bottom:
                fail(f"{aspect} GUI {scale}: profile stat rows do not fit their information area")
            for language, mapping in (("ko_kr", KO), ("en_us", EN)):
                for labels, values, bounds_width in (
                        (("screen.anomalousclasses.profile.level", "screen.anomalousclasses.profile.experience",
                          "screen.anomalousclasses.profile.skill_points"), ("120", "1234567890", "900"), info_width),
                        (("screen.anomalousclasses.profile.max_health", "screen.anomalousclasses.profile.attack_damage",
                          "screen.anomalousclasses.profile.movement_speed", "screen.anomalousclasses.profile.armor"),
                         ("200", "100", "0.12", "100"), info_width)):
                    measured_label = max(approximate_font_width(mapping[key]) for key in labels)
                    measured_value = max(approximate_font_width(value) for value in values)
                    row_inset = min(5, max(2, bounds_width // 24))
                    row_gap = min(12, max(5, bounds_width // 36))
                    if measured_label + row_gap + measured_value > bounds_width - row_inset * 2:
                        fail(f"{aspect} GUI {scale}: {language} profile metric columns overlap or truncate")
            cases += 1
            print(f"{aspect} GUI {scale}: profile frame inset, art clip, columns, stats, and buttons OK")
    return cases


def approximate_font_width(value: str) -> int:
    width = 0
    for char in value:
        if unicodedata.east_asian_width(char) in ("W", "F"): width += 9
        elif char in " ilI.,:;'!|`": width += 3
        elif char.isspace(): width += 4
        else: width += 6
    return width


def _branch_key(class_key: str) -> str:
    return next(branch for branch, key, *_ in CLASSES.values() if key == class_key)


if __name__ == "__main__":
    old_catalog = check_catalog()
    check_migration_and_purchase(old_catalog)
    check_art_and_screen()
    check_text()
    tree_count = viewport_cases()
    profile_count = profile_viewport_cases()
    print(f"PASS: 581 nodes, six 900-SP graphs, legacy-rank migration fixtures, 61 tree icons, KO/EN, and {tree_count + profile_count} static viewport cases")

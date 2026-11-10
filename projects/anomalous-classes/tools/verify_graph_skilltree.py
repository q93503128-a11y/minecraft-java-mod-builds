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
    for stat in STAT_LABELS:
        if png_info(texture_root / f"icons/common/{stat}.png") != (256, 256, 6):
            fail(f"missing/invalid stat icon {stat}")
    for relative, size in (("menu_v1/node_states.png", (1024, 1024)),
                           ("menu_v1/button_states.png", (1024, 1024)),
                           ("menu_v1/frame_atlas.png", (768, 768))):
        if png_info(RES / f"textures/gui/{relative}")[:2] != size: fail(f"shared UI texture mismatch: {relative}")

    screen = (SRC / "client/SkillTreeScreen.java").read_text(encoding="utf-8")
    graph_source = (SRC / "state/GraphSkillTreeCatalog.java").read_text(encoding="utf-8")
    if screen.count("renderBackground(graphics,") != 1 or "super.render(" in screen:
        fail("skill screen renders the background more than once or re-enters Screen.render")
    for term in ("mouseDragged(", "mouseReleased(", "mouseScrolled(", "zoomAt(", "GraphSkillTreeCatalog",
                 "MenuUi.drawNodeFrame", "drawHoveredCard", "SkillTreeActionPayload.Action.PURCHASE_NODE"):
        if term not in screen: fail(f"skill screen missing interactive graph feature {term}")
    if "icons/common" not in graph_source: fail("common custom stat icons are not wired in the graph catalog")
    if any(term in screen for term in ("drawTitle(graphics", "INVEST_TO_BREAKPOINT", "BuiltInRegistries", "effect_unavailable")):
        fail("old grid/title/development or vanilla-item presentation remains in the graph screen")
    if "GLFW_KEY_K" in (SRC / "client/ClassSelectionKeyMappings.java").read_text(encoding="utf-8"):
        fail("K class-change key binding was reintroduced")
    if "OPEN_PROFILE" not in (SRC / "client/ClassSelectionKeyMappings.java").read_text(encoding="utf-8"):
        fail("M profile key mapping was removed")


def viewport_cases() -> int:
    screen = (SRC / "client/SkillTreeScreen.java").read_text(encoding="utf-8")
    required = ("HUD_HEIGHT = 34", "MIN_ZOOM = 0.24", "MAX_ZOOM = 1.85", "Math.min(230, width - 12)",
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
            card_h = 12 + max_lines * 11
            if card_h > height - 34 - 5:
                fail(f"{aspect} scale {scale}: tooltip exceeds available height")
            card_w = max(105, min(230, width - 12))
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
                    "screen.anomalousclasses.skill_tree.spent", "screen.anomalousclasses.skill_tree.node_stage",
                    "screen.anomalousclasses.skill_tree.node_stat", "screen.anomalousclasses.skill_tree.node_cost",
                    "screen.anomalousclasses.skill_tree.node_breakthrough", "screen.anomalousclasses.skill_tree.ready",
                    "screen.anomalousclasses.skill_tree.node_follow_path", "screen.anomalousclasses.skill_tree.migration_failed"):
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
    count = viewport_cases()
    print(f"PASS: 581 nodes, six 900-SP graphs, legacy-rank migration fixtures, runtime art, KO/EN, and {count} static viewport cases")

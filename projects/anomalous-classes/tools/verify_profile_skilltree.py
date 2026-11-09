#!/usr/bin/env python3
"""Static checks for canonical skill data, UI textures, and responsive screen geometry."""
from __future__ import annotations

import json
import math
import re
import struct
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "src/main/java/kr/moonseungjun/anomalousclasses"
COST_PATH = ROOT / "balance/SKILL_POINT_COST_MODEL_V2.json"
KO_PATH = ROOT / "src/main/resources/assets/anomalousclasses/lang/ko_kr.json"
EN_PATH = ROOT / "src/main/resources/assets/anomalousclasses/lang/en_us.json"
CATALOG_PATH = BASE / "state/SkillTreeCatalog.java"
RESOURCE_ROOT = ROOT / "src/main/resources/assets/anomalousclasses/textures/gui"

CLASS_MAP = {
    "MENTAL_DOMINATOR": ("mental", "mental_dominator"),
    "PUPPETEER": ("puppet", "puppeteer"),
    "PARASITE_MORPHER": ("parasite", "parasite_morpher"),
    "BARRIER_CASTER": ("barrier", "barrier_caster"),
    "PACT_CASTER": ("pact", "pact_caster"),
    "SWARM_CASTER": ("swarm", "swarm_caster"),
}


def approximate_font_width(value: str) -> int:
    """Estimate default-font width for static checks; actual game font rendering is not run here."""
    width = 0
    for char in value:
        if unicodedata.east_asian_width(char) in ("W", "F"):
            width += 9
        elif char in " ilI.,:;'!|`":
            width += 3
        elif char.isspace():
            width += 4
        else:
            width += 6
    return width


def check_costs_and_translations() -> dict[str, list[dict]]:
    costs = json.loads(COST_PATH.read_text(encoding="utf-8"))["classes"]
    ko = json.loads(KO_PATH.read_text(encoding="utf-8"))
    en = json.loads(EN_PATH.read_text(encoding="utf-8"))
    if set(ko) != set(en):
        raise AssertionError(f"Korean/English keys differ: KO-only={set(ko)-set(en)}, EN-only={set(en)-set(ko)}")
    source = CATALOG_PATH.read_text(encoding="utf-8")
    trees: dict[str, list[dict]] = {}
    for enum_name, (cost_key, class_key) in CLASS_MAP.items():
        marker = f"case {enum_name} -> List.of("
        start = source.index(marker) + len(marker)
        end = source.index(");", start)
        block = source[start:end]
        entries = re.findall(r'n\("([^\"]+)",\s*"([^\"]+)",\s*"([^\"]+)",\s*"([^\"]+)"((?:,\s*\d+)*)\)', block)
        spec = costs[cost_key]
        expected = spec["nodes"]
        source_plan = (ROOT / spec["source"]).read_text(encoding="utf-8").splitlines()
        expected_names = {node["name"] for node in expected}
        plan_nodes = []
        for line in source_plan:
            if "|" not in line:
                continue
            cells = [cell.strip().replace("**", "").replace("`", "") for cell in line.strip().strip("|").split("|")]
            if len(cells) < 3 or cells[1] not in expected_names:
                continue
            rank_match = re.fullmatch(r"\s*(\d+)\s*", cells[2])
            if rank_match:
                plan_nodes.append((cells[1], int(rank_match.group(1))))
        if plan_nodes != [(node["name"], node["max_rank"]) for node in expected]:
            raise AssertionError(f"{cost_key}: source plan name/order/max-rank mismatch: {plan_nodes}")
        if len(entries) != len(expected):
            raise AssertionError(f"{cost_key}: catalog has {len(entries)} nodes; source has {len(expected)}")
        parsed = []
        total = rank_total = 0
        for index, (entry, node) in enumerate(zip(entries, expected, strict=True)):
            node_id, display_name, group, _legacy_icon, breakpoint_text = entry
            if display_name != node["name"]:
                raise AssertionError(f"{cost_key}[{index}] name/order mismatch: {display_name!r} != {node['name']!r}")
            rank_costs = node["rank_costs"]
            if len(rank_costs) != node["max_rank"] or any(not isinstance(value, int) or value <= 0 for value in rank_costs):
                raise AssertionError(f"{cost_key}/{display_name}: malformed rank costs")
            points = sum(rank_costs)
            if points != node["full_sp"]:
                raise AssertionError(f"{cost_key}/{display_name}: {points} != declared {node['full_sp']}")
            total += points
            rank_total += len(rank_costs)
            breaks = [int(value) for value in re.findall(r"\d+", breakpoint_text)]
            if breaks != sorted(set(breaks)) or any(value < 1 or value > node["max_rank"] for value in breaks):
                raise AssertionError(f"{cost_key}/{display_name}: invalid milestones {breaks}")
            for key in (
                f"skill.anomalousclasses.{class_key}.{node_id}.name",
                f"skill.anomalousclasses.{class_key}.{node_id}.effect",
                f"skill_group.anomalousclasses.{class_key}.{group}",
            ):
                if not ko.get(key) or not en.get(key):
                    raise AssertionError(f"missing translation: {key}")
            parsed.append({"rank_costs": rank_costs, "max_rank": node["max_rank"], "milestones": breaks})
        if total != 900:
            raise AssertionError(f"{cost_key}: total spend is {total}, expected 900")
        trees[cost_key] = parsed
        print(f"{cost_key}: {len(parsed)} nodes, {rank_total} ranks, {total} SP, bilingual strings OK")

    for language, mapping in (("ko_kr", ko), ("en_us", en)):
        for key, value in mapping.items():
            if key.startswith(("screen.anomalousclasses.skill_tree.", "skill.anomalousclasses.")):
                if re.search(r"후보|제안|임시|candidate|proposal|temporary|debug|prototype|TODO", value, re.IGNORECASE):
                    raise AssertionError(f"{language}: internal planning wording in player text {key}: {value}")
    if "screen.anomalousclasses.skill_tree.effect_unavailable" in ko:
        raise AssertionError("development-only effect-status banner remains in Korean UI resources")
    return trees


def check_texture_assets() -> None:
    expected = {
        "menu_v1/frame_atlas.png": (768, 768),
        "menu_v1/title_plate.png": (1024, 342),
        "menu_v1/surface_tile.png": (256, 256),
        "menu_v1/button_states.png": (1024, 1024),
        "menu_v1/node_states.png": (1024, 1024),
        "menu_v1/connection_paths.png": (1024, 1024),
        "menu_v1/progress_states.png": (1024, 1024),
    }
    for relative, dimensions in expected.items():
        path = RESOURCE_ROOT / relative
        if not path.is_file():
            raise AssertionError(f"missing UI texture: {path}")
        data = path.read_bytes()
        if data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
            raise AssertionError(f"not a valid PNG: {path}")
        actual = struct.unpack(">II", data[16:24])
        if actual != dimensions:
            raise AssertionError(f"unexpected texture dimensions for {relative}: {actual} != {dimensions}")
    icon_dir = RESOURCE_ROOT / "skill_icons_v1"
    for _, class_key in CLASS_MAP.values():
        path = icon_dir / f"{class_key}.png"
        if not path.is_file():
            raise AssertionError(f"missing skill icon atlas: {path}")
        data = path.read_bytes()
        if data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
            raise AssertionError(f"not a valid PNG: {path}")
        actual = struct.unpack(">II", data[16:24])
        if actual != (1024, 1024):
            raise AssertionError(f"skill atlas must be 4x4 at 1024 square: {class_key} {actual}")
    screen = (BASE / "client/SkillTreeScreen.java").read_text(encoding="utf-8")
    if "graphics.renderItem" in screen or "BuiltInRegistries" in screen:
        raise AssertionError("skill-tree nodes still render vanilla item icons")
    if "effect_unavailable" in screen:
        raise AssertionError("player-facing development status text remains on the skill tree")
    if "MenuUi.drawNodeFrame" not in screen or "MenuUi.drawSkillIcon" not in screen:
        raise AssertionError("custom node states and skill atlases are not used by the skill tree")
    for screen_name in ("ProfileScreen.java", "SkillTreeScreen.java", "ClassManagementScreen.java"):
        source = (BASE / "client" / screen_name).read_text(encoding="utf-8")
        if "MenuUi.drawTitle" not in source:
            raise AssertionError(f"shared title plaque is not used by {screen_name}")
    print("textures: six 4x4 skill atlases and shared UI state atlases OK")


def rect(x: int, y: int, w: int, h: int, limit_w: int, limit_h: int, label: str) -> tuple[int, int, int, int]:
    if w < 1 or h < 1 or x < 0 or y < 0 or x + w > limit_w or y + h > limit_h:
        raise AssertionError(f"{label} outside {limit_w}x{limit_h}: {(x, y, w, h)}")
    return x, y, x + w, y + h


def overlaps(a: tuple[int, int, int, int], b: tuple[int, int, int, int]) -> bool:
    return a[0] < b[2] and a[2] > b[0] and a[1] < b[3] and a[3] > b[1]


def class_groups(source: str, enum_name: str) -> list[str]:
    marker = f"case {enum_name} -> List.of("
    start = source.index(marker) + len(marker)
    end = source.index(");", start)
    return re.findall(r'n\("[^\"]+",\s*"[^\"]+",\s*"(one|two|three)"', source[start:end])


def check_layouts(trees: dict[str, list[dict]]) -> int:
    cases = ((1920, 1080, "16:9"), (1920, 1200, "16:10"), (1600, 1200, "4:3"))
    source = CATALOG_PATH.read_text(encoding="utf-8")
    ko = json.loads(KO_PATH.read_text(encoding="utf-8"))
    en = json.loads(EN_PATH.read_text(encoding="utf-8"))
    checked = 0
    for physical_w, physical_h, aspect in cases:
        for scale in (2, 3, 4):
            width, height = math.ceil(physical_w / scale), math.ceil(physical_h / scale)
            margin, top = (5 if width < 360 else 10), (4 if height < 250 else 10)
            ox, oy, ow, oh = margin, top, width - 2 * margin, height - 2 * top
            outer = rect(ox, oy, ow, oh, width, height, "profile outer")
            profile_title_w = min(340, max(150, ow * 36 // 100))
            profile_title_w = min(profile_title_w, max(1, ow - 12))
            rect((width - profile_title_w) // 2, oy - 3, profile_title_w,
                 24 if height < 280 else 32, width, height, "profile title plaque")
            compact_profile = width < 540 or height < 310
            nav_h = max(19, min(23, height // 15))
            nav_y = oy + oh - nav_h - 5
            body_y, body_bottom = oy + (17 if height < 250 else 23) + 5, nav_y - 5
            body_h = max(1, body_bottom - body_y)
            gap = 7
            if compact_profile:
                left_w = min((ow - gap) // 2, max(90, round(ow * .44)))
                art_h = max(1, min(body_h, left_w * 9 // 16))
                art_x, art_y = ox + 7, body_y + max(0, (body_h - art_h) // 2)
                info_x, info_w = art_x + left_w + gap, ox + ow - (art_x + left_w + gap) - 7
            else:
                left_w = round((ow - 21) * .44)
                art_x, art_y, art_h = ox + 8, body_y, body_h
                info_x, info_w = art_x + left_w + gap, ox + ow - (art_x + left_w + gap) - 9
            art = rect(art_x, art_y, max(1, left_w), art_h, width, height, "profile art")
            info = rect(info_x, body_y, max(1, info_w), body_h, width, height, "profile information")
            stat_start = body_y + (68 if compact_profile else 112)
            stats = rect(info_x, stat_start, info_w, max(1, body_bottom - stat_start - 2), width, height, "profile stats")
            button_w = max(1, min(156, (ow - 20 - 6) // 2))
            button_x = ox + (ow - (button_w * 2 + 6)) // 2
            profile_buttons = [rect(button_x, nav_y, button_w, nav_h, width, height, "profile skill-tree button"),
                               rect(button_x + button_w + 6, nav_y, button_w, nav_h, width, height, "profile management button")]
            if overlaps(art, info) or overlaps(stats, profile_buttons[0]) or overlaps(stats, profile_buttons[1]):
                raise AssertionError(f"profile overlap at {aspect} GUI {scale}")

            mg_footer_h = max(19, min(27, height // 13))
            mg_footer_y = oy + oh - mg_footer_h - 5
            mg_profile_w = max(1, min(132, (ow - 28) // 2))
            mg_change_w = max(1, min(112, (ow - 28) // 2))
            mg_profile = rect(ox + 10, mg_footer_y, mg_profile_w, mg_footer_h, width, height, "management profile button")
            mg_change = rect(ox + ow - mg_change_w - 10, mg_footer_y, mg_change_w, mg_footer_h, width, height, "management change button")
            mg_central = rect(ox + 8, oy + 63, ow - 16, max(1, mg_footer_y - (oy + 72)), width, height, "management empty area")
            if overlaps(mg_profile, mg_change) or overlaps(mg_central, mg_profile) or overlaps(mg_central, mg_change):
                raise AssertionError(f"management overlap at {aspect} GUI {scale}")

            skill_margin = 4 if width < 360 else 8
            skill_top = 3 if height < 250 else 8
            sx, sy = skill_margin, skill_top
            sw, sh = width - skill_margin * 2, height - skill_top * 2
            skill_bottom = sy + sh
            skill_outer = rect(sx, sy, sw, sh, width, height, "skill-tree outer")
            skill_title_w = min(340, max(150, sw * 36 // 100))
            skill_title_w = min(skill_title_w, max(1, sw - 12))
            rect((width - skill_title_w) // 2, sy - 3, skill_title_w,
                 24 if height < 280 else 32, width, height, "skill-tree title plaque")
            compact = width < 700 or height < 400
            header = 18 if height < 280 else 26
            actions = []
            if compact:
                content_x, content_w = sx + 5, max(1, sw - 10)
                identity_h = 22 if height < 300 else 24
                identity = rect(content_x, sy + header + 3, content_w, identity_h, width, height, "compact identity")
                action_h = max(16, min(20, height // 18))
                footer_y = skill_bottom - 4 - action_h
                invest_y = footer_y - 3 - action_h
                details_h = max(52, min(62, height // 5))
                details = rect(content_x, invest_y - 4 - details_h, content_w, details_h, width, height, "compact details")
                graph_y = identity[3] + 4
                graph = rect(content_x, graph_y, content_w, max(1, details[1] - graph_y - 4), width, height, "compact graph")
                gap = 4
                action_w = max(1, (content_w - 2 * gap) // 3)
                action_x = content_x + (content_w - (action_w * 3 + 2 * gap)) // 2
                actions = [rect(action_x + i * (action_w + gap), invest_y, action_w, action_h, width, height, "skill investment button") for i in range(3)]
                footer_w = max(1, min(156, (content_w - gap) // 2))
                footer_x = content_x + (content_w - (footer_w * 2 + gap)) // 2
                actions += [rect(footer_x, footer_y, footer_w, action_h, width, height, "skill reset button"),
                            rect(footer_x + footer_w + gap, footer_y, footer_w, action_h, width, height, "skill profile button")]
                node_header = 12
            else:
                body_y = sy + header + 5
                body_h = max(1, skill_bottom - body_y - 7)
                identity_w = max(148, min(202, sw // 5))
                details_w = max(190, min(258, sw // 4))
                identity = rect(sx + 7, body_y, identity_w, body_h, width, height, "wide identity")
                details_x = sx + sw - 7 - details_w
                graph_x = identity[2] + 6
                graph = rect(graph_x, body_y, max(1, details_x - graph_x - 6), body_h, width, height, "wide graph")
                details = rect(details_x, body_y, details_w, body_h, width, height, "wide details")
                action_h, gap = 20, 4
                invest_y = body_y + body_h - 50 - action_h
                action_w = max(1, (details_w - 12 - 2 * gap) // 3)
                action_x = details_x + (details_w - (action_w * 3 + 2 * gap)) // 2
                actions = [rect(action_x + i * (action_w + gap), invest_y, action_w, action_h, width, height, "skill investment button") for i in range(3)]
                footer_w = max(1, min(132, details_w - 12))
                footer_x = details_x + (details_w - footer_w) // 2
                actions += [rect(identity[0] + 8, body_y + body_h - 27, identity_w - 16, 21, width, height, "skill reset button"),
                            rect(footer_x, body_y + body_h - 25, footer_w, 20, width, height, "skill profile button")]
                node_header = 28
            if overlaps(identity, graph) or overlaps(graph, details) or overlaps(identity, details):
                raise AssertionError(f"skill-tree panels overlap at {aspect} GUI {scale}")
            if any(overlaps(button, graph) for button in actions):
                raise AssertionError(f"skill-tree button overlaps graph at {aspect} GUI {scale}")
            if not compact and any(button[0] < details[0] or button[2] > details[2]
                                   or button[1] < details[1] or button[3] > details[3]
                                   for button in (*actions[:3], actions[4])):
                raise AssertionError(f"wide skill action leaves details panel at {aspect} GUI {scale}")
            if not compact and (actions[3][0] < identity[0] or actions[3][2] > identity[2]
                                or actions[3][1] < identity[1] or actions[3][3] > identity[3]):
                raise AssertionError(f"wide reset action leaves identity panel at {aspect} GUI {scale}")
            if compact and any(overlaps(button, details) for button in actions):
                raise AssertionError(f"compact skill action overlaps details at {aspect} GUI {scale}")

            for enum_name, (_, class_key) in CLASS_MAP.items():
                groups = class_groups(source, enum_name)
                max_rows = max(groups.count(group) for group in set(groups))
                graph_header = 12 if compact else 28
                max_size = (graph[3] - graph[1] - graph_header - 6 - (max_rows - 1) * 2) // max_rows
                node_size = max(18 if compact else 32, min(36 if compact else 48, max_size))
                row_indexes = {"one": 0, "two": 0, "three": 0}
                node_boxes = []
                for group in groups:
                    column = {"one": 0, "two": 1, "three": 2}[group]
                    row = row_indexes[group]
                    row_indexes[group] += 1
                    left = graph[0] + graph[2] * column // 3
                    right = graph[0] + graph[2] * (column + 1) // 3
                    center_x = (left + right) // 2 + (0 if row % 2 == 0 else (10 if column == 1 else (8 if column == 0 else -8)))
                    center_x = max(graph[0] + node_size // 2 + 4, min(graph[2] - node_size // 2 - 4, center_x))
                    rows = groups.count(group)
                    top_y = graph[1] + graph_header + node_size // 2 + 2
                    bottom_y = graph[3] - node_size // 2 - 4
                    step = 0 if rows <= 1 else max(1, (bottom_y - top_y) // (rows - 1))
                    center_y = top_y + row * step
                    node = rect(center_x - node_size // 2, center_y - node_size // 2,
                                node_size, node_size, width, height, "skill node hitbox")
                    if node[0] < graph[0] or node[2] > graph[2] or node[1] < graph[1] or node[3] > graph[3]:
                        raise AssertionError(f"skill node outside graph at {aspect} GUI {scale}")
                    if any(overlaps(node, prior) for prior in node_boxes):
                        raise AssertionError(f"skill node hitboxes overlap at {aspect} GUI {scale}")
                    node_boxes.append(node)
                column_width = graph[2] // 3
                for group in ("one", "two", "three"):
                    key = f"skill_group.anomalousclasses.{class_key}.{group}"
                    for language, mapping in (("ko_kr", ko), ("en_us", en)):
                        measured = approximate_font_width(mapping[key])
                        if measured > column_width - 8:
                            raise AssertionError(f"{language} group title may clip at {aspect} GUI {scale}: {mapping[key]} ({measured}>{column_width - 8})")

            checked += 1
            print(f"{aspect} GUI {scale}: logical {width}x{height} static bounds OK")
    print("font review: KO/EN group labels use conservative default-font width estimates; Minecraft font not runtime-measured")
    return checked


def check_transaction_model(trees: dict[str, list[dict]]) -> None:
    def purchase(current: int, target: int, available: int, costs: list[int]) -> tuple[int, int]:
        price = sum(costs[current:target])
        return (target, price) if target > current and price <= available else (current, 0)

    for key, nodes in trees.items():
        for node in nodes:
            costs, max_rank, milestones = node["rank_costs"], node["max_rank"], node["milestones"]
            if sum(costs) != sum(costs[:max_rank]):
                raise AssertionError(f"{key}: full refund is not equal to paid cost")
            for rank in (0, max_rank // 2, max_rank - 1):
                if rank < max_rank:
                    one_cost = costs[rank]
                    if purchase(rank, rank + 1, one_cost - 1, costs) != (rank, 0):
                        raise AssertionError(f"{key}: insufficient/one-rank boundary mismatch")
                    if purchase(rank, rank + 1, one_cost, costs) != (rank + 1, one_cost):
                        raise AssertionError(f"{key}: one-rank purchase mismatch")
                    next_break = next((milestone for milestone in milestones if milestone > rank), -1)
                    if next_break >= 0:
                        due = sum(costs[rank:next_break])
                        if purchase(rank, next_break, due - 1, costs) != (rank, 0):
                            raise AssertionError(f"{key}: insufficient breakthrough boundary mismatch")
                        if purchase(rank, next_break, due, costs) != (next_break, due):
                            raise AssertionError(f"{key}: breakthrough investment mismatch")
                for available in (0, costs[min(rank, max_rank - 1)], sum(costs[min(rank, max_rank - 1):])):
                    target, spent = rank, 0
                    while target < max_rank and spent + costs[target] <= available:
                        spent += costs[target]
                        target += 1
                    if target < rank or target > max_rank or spent > available:
                        raise AssertionError(f"{key}: max-affordable boundary invalid")
            if max_rank < 1:
                raise AssertionError(f"{key}: empty rank range")


if __name__ == "__main__":
    catalog = check_costs_and_translations()
    check_texture_assets()
    check_transaction_model(catalog)
    cases = check_layouts(catalog)
    print(f"PASS: canonical nodes/costs, transactions, custom textures, bilingual labels, and {cases} static viewport cases")

#!/usr/bin/env python3
"""Static checks for the profile/skill-tree cost source, copy, and UI geometry."""
from __future__ import annotations

import json
import math
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "src/main/java/kr/moonseungjun/anomalousclasses"
COST_PATH = ROOT / "balance/SKILL_POINT_COST_MODEL_V2.json"
KO_PATH = ROOT / "src/main/resources/assets/anomalousclasses/lang/ko_kr.json"
EN_PATH = ROOT / "src/main/resources/assets/anomalousclasses/lang/en_us.json"
CATALOG_PATH = BASE / "state/SkillTreeCatalog.java"

CLASS_MAP = {
    "MENTAL_DOMINATOR": ("mental", "mental_dominator"),
    "PUPPETEER": ("puppet", "puppeteer"),
    "PARASITE_MORPHER": ("parasite", "parasite_morpher"),
    "BARRIER_CASTER": ("barrier", "barrier_caster"),
    "PACT_CASTER": ("pact", "pact_caster"),
    "SWARM_CASTER": ("swarm", "swarm_caster"),
}


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
        class_spec = costs[cost_key]
        expected = class_spec["nodes"]
        plan_lines = (ROOT / class_spec["source"]).read_text(encoding="utf-8").splitlines()
        expected_names = {node["name"] for node in expected}
        plan_nodes = []
        for line in plan_lines:
            if "|" not in line:
                continue
            cells = [cell.strip().replace("**", "").replace("`", "") for cell in line.strip().strip("|").split("|")]
            if len(cells) < 3 or cells[1] not in expected_names:
                continue
            rank_match = re.fullmatch(r"\s*(\d+)\s*", cells[2])
            if rank_match:
                plan_nodes.append((cells[1], int(rank_match.group(1))))
        declared_nodes = [(node["name"], node["max_rank"]) for node in expected]
        if plan_nodes != declared_nodes:
            raise AssertionError(f"{cost_key}: source plan name/order/max-rank mismatch: {plan_nodes}")
        if len(entries) != len(expected):
            raise AssertionError(f"{cost_key}: catalog has {len(entries)} nodes; source has {len(expected)}")
        parsed = []
        total = 0
        rank_total = 0
        for index, (entry, node) in enumerate(zip(entries, expected, strict=True)):
            node_id, display_name, group, icon, breakpoint_text = entry
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
            name_key = f"skill.anomalousclasses.{class_key}.{node_id}.name"
            effect_key = f"skill.anomalousclasses.{class_key}.{node_id}.effect"
            group_key = f"skill_group.anomalousclasses.{class_key}.{group}"
            for key in (name_key, effect_key, group_key):
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
                if re.search(r"후보|제안|임시|candidate|proposal|temporary", value, re.IGNORECASE):
                    raise AssertionError(f"{language}: internal planning wording in player text {key}: {value}")
    return trees


def rect(x: int, y: int, w: int, h: int, limit_w: int, limit_h: int, label: str) -> tuple[int, int, int, int]:
    if w < 1 or h < 1 or x < 0 or y < 0 or x + w > limit_w or y + h > limit_h:
        raise AssertionError(f"{label} outside {limit_w}x{limit_h}: {(x, y, w, h)}")
    return x, y, x + w, y + h


def overlaps(a: tuple[int, int, int, int], b: tuple[int, int, int, int]) -> bool:
    return a[0] < b[2] and a[2] > b[0] and a[1] < b[3] and a[3] > b[1]


def check_layouts(trees: dict[str, list[dict]]) -> int:
    cases = ((1920, 1080, "16:9"), (1920, 1200, "16:10"), (1600, 1200, "4:3"))
    checked = 0
    for physical_w, physical_h, aspect in cases:
        for scale in (2, 3, 4):
            width, height = math.ceil(physical_w / scale), math.ceil(physical_h / scale)
            # Profile: both preview and information columns stay inside the frame; the two hitboxes fit the footer.
            margin, top = (5 if width < 360 else 10), (4 if height < 250 else 10)
            ox, oy, ow, oh = margin, top, width - 2 * margin, height - 2 * top
            header = 17 if height < 250 else 23
            compact = width < 540 or height < 310
            nav_h = max(19, min(26, height // 13))
            nav_y = oy + oh - nav_h - 5
            body_y, body_bottom = oy + header + 5, nav_y - 5
            body_h = max(1, body_bottom - body_y)
            if compact:
                left_w = min((ow - 7) // 2, max(90, round(ow * .44)))
                art_h = max(1, min(body_h, left_w * 9 // 16))
                art_x, art_y = ox + 7, body_y + max(0, (body_h - art_h) // 2)
                info_x, info_w = art_x + left_w + 7, ox + ow - (art_x + left_w + 7) - 7
            else:
                left_w = round((ow - 21) * .44)
                art_x, art_y, art_h = ox + 8, body_y, body_h
                info_x, info_w = art_x + left_w + 7, ox + ow - (art_x + left_w + 7) - 9
            art = rect(art_x, art_y, max(1, left_w), art_h, width, height, "profile art")
            info = rect(info_x, body_y, max(1, info_w), body_h, width, height, "profile info")
            stat_start = body_y + (68 if compact else 112)
            stat_box = rect(info_x, stat_start, info_w, max(1, body_bottom - stat_start - 2), width, height, "profile stats")
            button_gap = 6
            button_w = max(1, (ow - 20 - button_gap) // 2)
            bx = ox + (ow - (button_w * 2 + button_gap)) // 2
            button_a = rect(bx, nav_y, button_w, nav_h, width, height, "profile skill button")
            button_b = rect(bx + button_w + button_gap, nav_y, button_w, nav_h, width, height, "profile management button")
            if overlaps(art, info) or overlaps(stat_box, button_a) or overlaps(stat_box, button_b):
                raise AssertionError(f"profile overlap at {aspect} scale {scale}")

            # Management frame has an empty center and a compact class-change action in the corner.
            mg_footer_h = max(19, min(27, height // 13))
            mg_footer_y = oy + oh - mg_footer_h - 5
            profile_w = max(1, min(132, (ow - 28) // 2))
            change_w = max(1, min(112, (ow - 28) // 2))
            mg_profile = rect(ox + 10, mg_footer_y, profile_w, mg_footer_h, width, height, "management profile button")
            mg_change = rect(ox + ow - change_w - 10, mg_footer_y, change_w, mg_footer_h, width, height, "management class-change button")
            mg_central_y = oy + 63
            mg_central = rect(ox + 8, mg_central_y, ow - 16,
                              max(1, mg_footer_y - (oy + 72)), width, height, "management central frame")
            if overlaps(mg_profile, mg_change) or overlaps(mg_central, mg_profile) or overlaps(mg_central, mg_change):
                raise AssertionError(f"management frame overlap at {aspect} scale {scale}")
            if change_w > 112:
                raise AssertionError("management class-change button is not compact")

            # Skill tree: exact responsive rectangles used for renderers and AbstractButton hitboxes.
            outer_bottom = oy + oh
            skill_compact = width < 700 or height < 400
            skill_header = 17 if height < 250 else 22
            if skill_compact:
                action_top = outer_bottom - 46
                graph_y = oy + skill_header + (10 if height < 250 else 8)
                graph_h = max(56, min(114, action_top - graph_y - 52))
                gx, gy, gw, gh = ox + 4, graph_y, ow - 8, graph_h
                detail_y, detail_h = gy + gh + 4, max(1, action_top - gy - gh - 7)
                action_h, gap = max(17, min(20, height // 12)), 4
                aw = max(1, (ow - 20 - 2 * gap) // 3)
                ax = ox + (ow - (aw * 3 + 2 * gap)) // 2
                actions = [rect(ax + i * (aw + gap), action_top, aw, action_h, width, height, "skill action") for i in range(3)]
                by, bw = action_top + action_h + 3, max(1, (ow - 20 - gap) // 2)
                bx = ox + (ow - (bw * 2 + gap)) // 2
                actions += [rect(bx, by, bw, action_h, width, height, "skill footer"),
                            rect(bx + bw + gap, by, bw, action_h, width, height, "skill footer")]
                node_size = 23
                n_header = 8
            else:
                body_y = oy + skill_header + 5
                body_bottom = outer_bottom - 34 - 8
                body_h = max(1, body_bottom - body_y)
                detail_w = max(205, min(280, ow // 3))
                gx, gy, gw, gh = ox + 7, body_y, ow - detail_w - 21, body_h
                detail_y, detail_h = body_y, body_h
                gap = 5
                control_w = max(1, (detail_w - 12 - 2 * gap) // 3)
                control_x = gx + gw + 7 + (detail_w - (3 * control_w + 2 * gap)) // 2
                control_y, action_h = body_y + body_h - 57, 22
                actions = [rect(control_x + i * (control_w + gap), control_y, control_w, action_h, width, height, "skill action") for i in range(3)]
                by = control_y + action_h + 5
                bw = max(1, (detail_w - 12 - gap) // 2)
                bx = gx + gw + 7 + (detail_w - (2 * bw + gap)) // 2
                actions += [rect(bx, by, bw, action_h, width, height, "skill footer"),
                            rect(bx + bw + gap, by, bw, action_h, width, height, "skill footer")]
                node_size, n_header = 46, 30
            graph_box = rect(gx, gy, gw, gh, width, height, "skill graph")
            detail_box = rect(gx + gw + (7 if not skill_compact else 0), detail_y,
                              detail_w if not skill_compact else gw, detail_h, width, height, "skill details") if not skill_compact else rect(gx, detail_y, gw, detail_h, width, height, "skill details")
            if any(overlaps(button, graph_box) for button in actions):
                raise AssertionError(f"skill action overlaps graph at {aspect} scale {scale}")
            if skill_compact and any(overlaps(button, detail_box) for button in actions):
                raise AssertionError(f"skill action overlaps details at {aspect} scale {scale}")
            if not skill_compact and any(button[0] < detail_box[0] or button[2] > detail_box[2]
                                         or button[1] < detail_box[1] or button[3] > detail_box[3]
                                         for button in actions):
                raise AssertionError(f"skill action outside details panel at {aspect} scale {scale}")
            for tree_nodes in trees.values():
                # Re-read metadata in catalog order so the node hitboxes match the screen's positions.
                # Groups are recovered from the same source declarations.
                source = CATALOG_PATH.read_text(encoding="utf-8")
                # Node count per column is determined by each contiguous class run; each class has at most four rows.
                # The branch's canonical order occupies group columns in the class catalog.
                # Rebuild row indices per class in the exact traversal order.
                for enum_name, (cost_key, _) in CLASS_MAP.items():
                    if trees[cost_key] is not tree_nodes:
                        continue
                    marker = f"case {enum_name} -> List.of("
                    start = source.index(marker) + len(marker)
                    end = source.index(");", start)
                    block = source[start:end]
                    groups = re.findall(r'n\("[^\"]+",\s*"[^\"]+",\s*"(one|two|three)"', block)
                    rows: dict[str, int] = {"one": 0, "two": 0, "three": 0}
                    boxes = []
                    for group in groups:
                        col = {"one": 0, "two": 1, "three": 2}[group]
                        max_rows = groups.count(group)
                        column_w = gw // 3
                        center_x = gx + column_w * col + column_w // 2
                        size = node_size
                        top_y = gy + n_header + size // 2 + 2
                        bottom_y = gy + gh - size // 2 - 4
                        step = max(1, (bottom_y - top_y) // max(1, max_rows - 1))
                        center_y = top_y + rows[group] * step
                        rows[group] += 1
                        box = rect(center_x - size // 2, center_y - size // 2, size, size, width, height, "skill node hitbox")
                        if box[0] < graph_box[0] or box[2] > graph_box[2] or box[1] < graph_box[1] or box[3] > graph_box[3]:
                            raise AssertionError(f"skill node outside graph at {aspect} scale {scale}")
                        if any(overlaps(box, prior) for prior in boxes):
                            raise AssertionError(f"skill node hitboxes overlap at {aspect} scale {scale}")
                        boxes.append(box)
            checked += 1
            print(f"{aspect} GUI {scale}: logical {width}x{height} OK")
    return checked


def check_transaction_model(trees: dict[str, list[dict]]) -> None:
    def purchase(current: int, target: int, available: int, costs: list[int]) -> tuple[int, int]:
        price = sum(costs[current:target])
        return (target, price) if target > current and price <= available else (current, 0)

    for key, nodes in trees.items():
        for node in nodes:
            costs, max_rank, milestones = node["rank_costs"], node["max_rank"], node["milestones"]
            # Rank-one, exact milestone, insufficient SP, max affordable, cap, and reset/refund paths.
            if sum(costs) != sum(costs[:max_rank]):
                raise AssertionError(f"{key}: full refund is not equal to paid cost")
            for rank in (0, max_rank // 2, max_rank - 1):
                if rank < max_rank:
                    one_cost = costs[rank]
                    if one_cost <= 0:
                        raise AssertionError(f"{key}: invalid single-rank purchase boundary")
                    if purchase(rank, rank + 1, one_cost - 1, costs) != (rank, 0):
                        raise AssertionError(f"{key}: insufficient/one-rank boundary mismatch")
                    if purchase(rank, rank + 1, one_cost, costs) != (rank + 1, one_cost):
                        raise AssertionError(f"{key}: one-rank purchase mismatch")
                    next_break = next((milestone for milestone in milestones if milestone > rank), -1)
                    if next_break >= 0:
                        due = sum(costs[rank:next_break])
                        if due <= 0 or purchase(rank, next_break, due - 1, costs) != (rank, 0):
                            raise AssertionError(f"{key}: invalid milestone purchase sum")
                        if purchase(rank, next_break, due, costs) != (next_break, due):
                            raise AssertionError(f"{key}: milestone purchase mismatch")
                # Server max-affordable walks sequential rank costs and never passes the cap.
                for available in (0, costs[min(rank, max_rank - 1)], sum(costs[min(rank, max_rank - 1):])):
                    target, spent = rank, 0
                    while target < max_rank and spent + costs[target] <= available:
                        spent += costs[target]
                        target += 1
                    if target < rank or target > max_rank or spent > available:
                        raise AssertionError(f"{key}: max-affordable boundary invalid")
            rank, available = max_rank, sum(costs)
            target, spent = rank, 0
            if rank >= max_rank and target != rank or spent != 0:
                raise AssertionError(f"{key}: max-rank cap path invalid")


if __name__ == "__main__":
    catalog = check_costs_and_translations()
    check_transaction_model(catalog)
    cases = check_layouts(catalog)
    print(f"PASS: canonical costs/translations, purchase boundaries, and {cases} responsive layout cases")

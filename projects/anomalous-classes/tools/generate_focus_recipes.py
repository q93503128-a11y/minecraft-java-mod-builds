#!/usr/bin/env python3
"""Generate vanilla shaped recipe JSON from the single focus-recipe catalog."""

import json
import shutil
import sys
from collections import Counter
from pathlib import Path


def fail(message: str) -> None:
    raise SystemExit(message)


def main() -> None:
    if len(sys.argv) != 3:
        fail("usage: generate_focus_recipes.py <catalog.json> <generated-resources>")
    source = Path(sys.argv[1]).resolve()
    output = Path(sys.argv[2]).resolve()
    catalog = json.loads(source.read_text(encoding="utf-8"))
    if catalog.get("schema_version") != 1:
        fail("unsupported focus recipe catalog schema")

    recipes = catalog.get("recipes", [])
    expected = {(class_id, tier) for class_id in (
        "mental_dominator", "puppeteer", "parasite_morpher", "barrier_caster", "pact_caster", "swarm_caster"
    ) for tier in ("standard", "altered", "anomalous", "eldritch", "incomprehensible")}
    actual = set()
    for recipe in recipes:
        identity = (recipe.get("class"), recipe.get("tier"))
        if identity in actual:
            fail(f"duplicate focus recipe {identity}")
        actual.add(identity)
        if identity not in expected:
            fail(f"unexpected focus recipe {identity}")
        pattern = recipe.get("pattern")
        key = recipe.get("key")
        if not isinstance(pattern, list) or len(pattern) != 3 or any(not isinstance(row, str) or len(row) != 3 for row in pattern):
            fail(f"{identity}: shaped recipe must use exactly three rows of three cells")
        used = Counter(char for row in pattern for char in row if char != ".")
        if set(used) != set(key):
            fail(f"{identity}: pattern/key symbols differ")
        if sum(used.values()) > 9:
            fail(f"{identity}: more than nine input items")
        if any(not isinstance(items, list) or not items for items in key.values()):
            fail(f"{identity}: every ingredient needs at least one item")
        if any(not isinstance(item, str) or ":" not in item for items in key.values() for item in items):
            fail(f"{identity}: ingredient id is malformed")
        expected_result = f"anomalousclasses:focus_{identity[0]}_{identity[1]}"
        if recipe.get("result", expected_result) != expected_result:
            fail(f"{identity}: output does not match registered focus id")
        center = pattern[1][1]
        if identity[1] == "standard":
            if center != "D" and identity[0] == "mental_dominator":
                fail(f"{identity}: base focus center must use glass pane")
        else:
            predecessor = ("standard", "altered", "anomalous", "eldritch")[
                ("altered", "anomalous", "eldritch", "incomprehensible").index(identity[1])]
            if center != "O" or key.get("O") != [f"anomalousclasses:focus_{identity[0]}_{predecessor}"]:
                fail(f"{identity}: upgrade must consume exactly the previous focus in the center")

    if actual != expected:
        missing = sorted(expected - actual)
        fail(f"focus catalog requires 30 recipes; missing {missing}")

    if output.exists():
        shutil.rmtree(output)
    assets = output / "assets" / "anomalousclasses"
    recipe_dir = output / "data" / "anomalousclasses" / "recipe" / "focus"
    assets.mkdir(parents=True)
    recipe_dir.mkdir(parents=True)
    shutil.copyfile(source, assets / "focus_recipes.json")

    for recipe in recipes:
        class_id = recipe["class"]
        tier = recipe["tier"]
        vanilla_key = {}
        for symbol, ids in recipe["key"].items():
            # Minecraft/NeoForge 1.21.1 ingredients accept a single item object or
            # an array of item objects for alternatives (not {"items": [...]}).
            vanilla_key[symbol] = ({"item": ids[0]} if len(ids) == 1
                                   else [{"item": item_id} for item_id in ids])
        generated = {
            "type": "minecraft:crafting_shaped",
            "category": "misc",
            # The UI catalog uses "." as its visible empty-cell marker. Vanilla
            # shaped recipes reserve a literal SPACE for empty cells.
            "pattern": [row.replace(".", " ") for row in recipe["pattern"]],
            "key": vanilla_key,
            "result": {"id": f"anomalousclasses:focus_{class_id}_{tier}", "count": 1},
        }
        path = recipe_dir / class_id / f"{tier}.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(generated, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Generated {len(recipes)} shaped focus recipes from {source.name}.")


if __name__ == "__main__":
    main()

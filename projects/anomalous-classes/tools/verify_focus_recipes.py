#!/usr/bin/env python3
"""Static parity checks for the generated focus items and their actual shaped recipes."""

import json
import struct
import sys
from collections import Counter
from pathlib import Path


CLASSES = (
    "mental_dominator", "puppeteer", "parasite_morpher",
    "barrier_caster", "pact_caster", "swarm_caster",
)
TIERS = ("standard", "altered", "anomalous", "eldritch", "incomprehensible")
FIRST_DISCOVERY_ITEMS = {
    "mental_dominator": "minecraft:amethyst_shard",
    "puppeteer": "minecraft:string",
    "parasite_morpher": "minecraft:rotten_flesh",
    "barrier_caster": "minecraft:glass",
    "pact_caster": "minecraft:paper",
    "swarm_caster": "minecraft:slime_ball",
}


def fail(message: str) -> None:
    raise SystemExit(message)


def png_size(path: Path) -> tuple[int, int]:
    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
        fail(f"not a PNG image: {path}")
    return struct.unpack(">II", data[16:24])


def main() -> None:
    if len(sys.argv) != 4:
        fail("usage: verify_focus_recipes.py <catalog> <source-resources> <generated-resources>")
    catalog_path = Path(sys.argv[1])
    source_resources = Path(sys.argv[2])
    generated_resources = Path(sys.argv[3])
    catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
    recipes = catalog.get("recipes", [])
    expected_pairs = {(class_id, tier) for class_id in CLASSES for tier in TIERS}
    actual_pairs = {(entry.get("class"), entry.get("tier")) for entry in recipes}
    if len(recipes) != 30 or actual_pairs != expected_pairs:
        fail(f"expected exactly 30 class/tier recipes; got {len(recipes)} entries")

    source_assets = source_resources / "assets" / "anomalousclasses"
    models = source_assets / "models" / "item"
    textures = source_assets / "textures" / "item"
    for class_id, tier in sorted(expected_pairs):
        item_id = f"focus_{class_id}_{tier}"
        model_path = models / f"{item_id}.json"
        texture_path = textures / f"{item_id}.png"
        if not model_path.is_file() or not texture_path.is_file():
            fail(f"missing focus model/texture for {item_id}")
        model = json.loads(model_path.read_text(encoding="utf-8"))
        if model.get("parent") != "minecraft:item/generated" or model.get("textures", {}).get("layer0") != f"anomalousclasses:item/{item_id}":
            fail(f"incorrect item model for {item_id}")
        if png_size(texture_path) != (16, 16):
            fail(f"focus item texture must be 16x16: {texture_path}")

    for language in ("ko_kr", "en_us"):
        path = source_assets / "lang" / f"{language}.json"
        strings = json.loads(path.read_text(encoding="utf-8"))
        for class_id, tier in expected_pairs:
            key = f"item.anomalousclasses.focus_{class_id}_{tier}"
            if not strings.get(key):
                fail(f"missing {language} display name: {key}")
        required_ui = (
            "screen.anomalousclasses.class_focus.title",
            "screen.anomalousclasses.class_focus.equip",
            "screen.anomalousclasses.class_focus.unequip",
            "screen.anomalousclasses.class_focus.recipe",
            "screen.anomalousclasses.class_focus.equipped_summary",
            "screen.anomalousclasses.class_focus.required_count",
            "screen.anomalousclasses.class_focus.alternative",
        )
        missing = [key for key in required_ui if not strings.get(key)]
        if missing:
            fail(f"missing {language} focus UI translations: {missing}")
        for key in (
            "tooltip.anomalousclasses.focus.identity",
            "tooltip.anomalousclasses.focus.slot",
        ):
            if not strings.get(key):
                fail(f"missing {language} focus hover translation: {key}")
        for key in (
            "screen.anomalousclasses.class_focus.inventory",
            "screen.anomalousclasses.class_focus.required_count",
            "screen.anomalousclasses.class_focus.alternative",
            "screen.anomalousclasses.class_focus.equipped_summary",
        ):
            template = strings[key]
            if template.count("%s") != 1 or any("{" + str(index) + "}" in template for index in range(8)):
                fail(f"invalid {language} translation argument format: {key}")

    generated_catalog = generated_resources / "assets" / "anomalousclasses" / "focus_recipes.json"
    if json.loads(generated_catalog.read_text(encoding="utf-8")) != catalog:
        fail("generated UI catalog does not match canonical recipe data")

    by_pair = {(entry["class"], entry["tier"]): entry for entry in recipes}
    for class_id, tier in expected_pairs:
        definition = by_pair[(class_id, tier)]
        path = generated_resources / "data" / "anomalousclasses" / "recipe" / "focus" / class_id / f"{tier}.json"
        generated = json.loads(path.read_text(encoding="utf-8"))
        if generated.get("type") != "minecraft:crafting_shaped":
            fail(f"recipe is not shaped: {path}")
        # Canonical UI patterns use "." but the runtime recipe parser reserves
        # literal SPACE as the empty slot. Never ship undefined '.' keys.
        runtime_pattern = [row.replace(".", " ") for row in definition["pattern"]]
        if generated.get("pattern") != runtime_pattern:
            fail(f"generated shape differs from catalog: {path}")
        result_id = f"anomalousclasses:focus_{class_id}_{tier}"
        if generated.get("result", {}).get("id") != result_id or generated["result"].get("count", 1) != 1:
            fail(f"incorrect recipe output: {path}")
        keys = generated.get("key", {})
        if set(keys) != set(definition["key"]):
            fail(f"ingredient symbols differ from catalog: {path}")
        for symbol, item_ids in definition["key"].items():
            ingredient = keys[symbol]
            if isinstance(ingredient, dict) and set(ingredient) == {"item"}:
                actual = [ingredient["item"]]
            elif isinstance(ingredient, list) and ingredient and all(
                    isinstance(entry, dict) and set(entry) == {"item"} for entry in ingredient):
                actual = [entry["item"] for entry in ingredient]
            else:
                fail(f"invalid 1.21.1 ingredient JSON for {symbol} in {path}")
            if actual != item_ids:
                fail(f"ingredient alternatives differ for {symbol} in {path}")
        used = Counter(char for row in definition["pattern"] for char in row if char != ".")
        if set(used) != set(definition["key"]) or sum(used.values()) > 9:
            fail(f"invalid 3x3 shaped pattern: {path}")
        if tier != "standard":
            previous = TIERS[TIERS.index(tier) - 1]
            if definition["pattern"][1][1] != "O" or definition["key"].get("O") != [f"anomalousclasses:focus_{class_id}_{previous}"]:
                fail(f"upgrade must consume the previous grade in the center: {path}")

        advancement_path = (generated_resources / "data" / "anomalousclasses" / "advancement"
                            / "recipes" / "focus" / class_id / f"{tier}.json")
        advancement = json.loads(advancement_path.read_text(encoding="utf-8"))
        expected_recipe = f"anomalousclasses:focus/{class_id}/{tier}"
        expected_item = (FIRST_DISCOVERY_ITEMS[class_id] if tier == "standard"
                         else definition["key"]["O"][0])
        if advancement.get("parent") != "minecraft:recipes/root":
            fail(f"invalid recipe-book advancement parent: {advancement_path}")
        if advancement.get("rewards", {}).get("recipes") != [expected_recipe]:
            fail(f"missing recipe-book unlock reward: {advancement_path}")
        criteria = advancement.get("criteria", {})
        if criteria.get("has_the_recipe") != {
                "trigger": "minecraft:recipe_unlocked",
                "conditions": {"recipe": expected_recipe}}:
            fail(f"invalid recipe-unlocked criterion: {advancement_path}")
        if criteria.get("has_ingredient") != {
                "trigger": "minecraft:inventory_changed",
                "conditions": {"items": [{"items": [expected_item]}]}}:
            fail(f"invalid inventory discovery criterion: {advancement_path}")
        if advancement.get("requirements") != [["has_the_recipe", "has_ingredient"]]:
            fail(f"recipe-book discovery must have OR conditions: {advancement_path}")

    pact_highest = by_pair[("pact_caster", "incomprehensible")]["key"].get("A", [])
    if set(pact_highest) != {"minecraft:totem_of_undying", "minecraft:heart_of_the_sea"}:
        fail("Pact highest-grade recipe must accept either approved alternative")

    recipe_files = list((generated_resources / "data" / "anomalousclasses" / "recipe" / "focus").rglob("*.json"))
    if len(recipe_files) != 30:
        fail(f"expected 30 generated shaped recipe files, found {len(recipe_files)}")
    advancements = list((generated_resources / "data" / "anomalousclasses" / "advancement"
                         / "recipes" / "focus").rglob("*.json"))
    if len(advancements) != 30:
        fail(f"expected 30 generated recipe-book advancements, found {len(advancements)}")
    print("Focus registry/recipe parity: PASS (30 items, 30 shaped recipes, 30 discovery advancements).")


if __name__ == "__main__":
    main()

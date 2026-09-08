#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path


class LaunchValidationError(RuntimeError):
    pass


FORBIDDEN_EXACT = {
    "minecraft:blaze_rod",
    "minecraft:blaze_powder",
    "minecraft:quartz",
    "minecraft:netherite_ingot",
    "minecraft:netherite_scrap",
    "minecraft:ancient_debris",
    "minecraft:ender_pearl",
    "minecraft:eye_of_ender",
    "minecraft:shulker_shell",
    "minecraft:dragon_breath",
    "minecraft:elytra",
}
FORBIDDEN_PREFIXES = (
    "minecraft:nether_",
    "minecraft:warped_",
    "minecraft:crimson_",
    "minecraft:chorus_",
    "minecraft:end_",
)
TARGET = "earth_to_stars:launch_craft_kit"


def fail(message: str) -> None:
    raise LaunchValidationError(message)


def ingredient_ids(recipe: dict) -> set[str]:
    values: list[object] = []
    if "key" in recipe:
        values.extend(recipe["key"].values())
    if "ingredients" in recipe:
        values.extend(recipe["ingredients"])

    result: set[str] = set()
    for value in values:
        entries = value if isinstance(value, list) else [value]
        for entry in entries:
            if isinstance(entry, str):
                result.add(entry.removeprefix("#"))
            elif isinstance(entry, dict):
                item = entry.get("item") or entry.get("tag")
                if isinstance(item, str):
                    result.add(item.removeprefix("#"))
    return result


def load_recipes(recipe_dir: Path) -> dict[str, tuple[Path, set[str]]]:
    outputs: dict[str, tuple[Path, set[str]]] = {}
    for path in sorted(recipe_dir.glob("*.json")):
        raw = json.loads(path.read_text(encoding="utf-8"))
        result = raw.get("result")
        if not isinstance(result, dict) or not isinstance(result.get("id"), str):
            continue
        output = result["id"]
        if output in outputs:
            fail(f"multiple recipes produce {output}: {outputs[output][0].name}, {path.name}")
        outputs[output] = (path, ingredient_ids(raw))
    return outputs


def forbidden(item_id: str) -> bool:
    return item_id in FORBIDDEN_EXACT or any(item_id.startswith(prefix) for prefix in FORBIDDEN_PREFIXES)


def validate(recipe_dir: Path) -> None:
    recipes = load_recipes(recipe_dir)
    if TARGET not in recipes:
        fail(f"missing main launch recipe for {TARGET}")

    visited: set[str] = set()
    stack: list[str] = []

    def visit(item_id: str) -> None:
        if forbidden(item_id):
            chain = " -> ".join(stack + [item_id])
            fail(f"Nether/End-only ingredient entered the launch path: {chain}")
        if item_id in visited:
            return
        visited.add(item_id)
        if not item_id.startswith("earth_to_stars:"):
            return
        recipe = recipes.get(item_id)
        if recipe is None:
            fail(f"launch-path mod item has no crafting recipe: {item_id}")
        stack.append(item_id)
        for ingredient in sorted(recipe[1]):
            visit(ingredient)
        stack.pop()

    visit(TARGET)

    required = {
        "earth_to_stars:reinforced_frame",
        "earth_to_stars:avionics_unit",
        "earth_to_stars:propellant_cell",
        "earth_to_stars:oxygen_cartridge",
        "earth_to_stars:life_support_unit",
        TARGET,
    }
    missing = sorted(required - visited)
    if missing:
        fail("launch recipe closure is missing required M1 components: " + ", ".join(missing))

    print(f"M1 LAUNCH VALIDATION OK: {len(visited)} reachable ingredients/components, Nether/End independent")


def main() -> None:
    project = Path(__file__).resolve().parents[1]
    try:
        validate(project / "src/main/resources/data/earth_to_stars/recipe")
    except (OSError, json.JSONDecodeError, LaunchValidationError) as exc:
        raise SystemExit(f"M1 LAUNCH VALIDATION FAILED: {exc}") from exc


if __name__ == "__main__":
    main()

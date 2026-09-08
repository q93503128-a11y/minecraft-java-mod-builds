#!/usr/bin/env python3
from __future__ import annotations

import json
import re
from pathlib import Path


class AcceptanceError(RuntimeError):
    pass


PROJECT = Path(__file__).resolve().parents[1]
JAVA = PROJECT / "src/main/java"
RES = PROJECT / "src/main/resources"


def fail(message: str) -> None:
    raise AcceptanceError(message)


def text(path: Path) -> str:
    if not path.is_file():
        fail(f"missing required file: {path.relative_to(PROJECT)}")
    return path.read_text(encoding="utf-8")


def validate_no_runtime_proxies() -> None:
    forbidden_patterns = {
        "ArmorStand import": r"import\s+net\.minecraft\.world\.entity\.decoration\.ArmorStand",
        "ArmorStand construction": r"new\s+ArmorStand\s*\(",
        "ArmorStand type check": r"instanceof\s+ArmorStand\b",
        "fake pilot tether": r"tetherController",
    }
    for path in JAVA.rglob("*.java"):
        source = path.read_text(encoding="utf-8")
        for label, pattern in forbidden_patterns.items():
            if re.search(pattern, source):
                fail(f"{label} remains in production source: {path.relative_to(PROJECT)}")


def validate_passenger_contract() -> None:
    runtime = text(JAVA / "kr/moonseungjun/earthtostars/ship/runtime/minecraft/ShipRuntimeManager.java")
    required = [
        "player.startRiding(exterior)",
        "player.getVehicle() != entry.exterior()",
        "teleported.startRiding(nextPair.exterior())",
        "ShipSavedData.get(server).remove(shipId)",
        "ShipSystemsSavedData.get(server).remove(shipId)",
        "InteriorSavedData.get(server).release(shipId)",
    ]
    for snippet in required:
        if snippet not in runtime:
            fail(f"missing alpha.12 authoritative ship lifecycle contract: {snippet}")

    client = text(JAVA / "kr/moonseungjun/earthtostars/ship/client/ShipClientController.java")
    if "keyShift.isDown()" in client:
        fail("Shift is still bound to flight input; vanilla dismount must remain available")
    if "keySprint.isDown()" not in client:
        fail("pitch-down is not bound to the remappable sprint/Ctrl input")

    exterior = text(JAVA / "kr/moonseungjun/earthtostars/ship/runtime/minecraft/ShipExteriorEntity.java")
    hurt_body = exterior.split("public boolean hurtServer", 1)[1].split("@Override", 1)[0]
    if "retireCraft" in hurt_body:
        fail("punching the ship can still retire authoritative state")
    if "player.isShiftKeyDown()" not in exterior or "retireCraft(serverPlayer, this)" not in exterior:
        fail("explicit Shift+right-click pack-up interaction is missing")


def validate_supply_feedback() -> None:
    source = text(JAVA / "kr/moonseungjun/earthtostars/content/ShipSupplyItem.java")
    if "displayClientMessage" in source:
        fail("obsolete pre-26.2 player feedback API remains in supply UX")
    if source.count("sendSystemMessage") < 3 or source.count(", true);") < 3:
        fail("supply success/full/no-ship feedback is not consistently actionbar based")


def validate_recipe_syntax() -> None:
    recipe_dir = RES / "data/earth_to_stars/recipe"
    required = {
        "reinforced_frame.json",
        "avionics_unit.json",
        "propellant_cell.json",
        "oxygen_cartridge.json",
        "life_support_unit.json",
        "launch_craft_kit.json",
    }
    missing = sorted(name for name in required if not (recipe_dir / name).is_file())
    if missing:
        fail("missing M1 recipes: " + ", ".join(missing))

    def validate_value(value: object, path: Path) -> None:
        values = value if isinstance(value, list) else [value]
        if not values:
            fail(f"{path.name}: empty ingredient alternative list")
        for entry in values:
            if not isinstance(entry, str):
                fail(f"{path.name}: obsolete/non-26.2 vanilla ingredient syntax remains: {entry!r}")

    for path in sorted(recipe_dir.glob("*.json")):
        recipe = json.loads(path.read_text(encoding="utf-8"))
        key = recipe.get("key", {})
        if key:
            if not isinstance(key, dict):
                fail(f"{path.name}: shaped key is not an object")
            for value in key.values():
                validate_value(value, path)
        ingredients = recipe.get("ingredients", [])
        if ingredients:
            if not isinstance(ingredients, list):
                fail(f"{path.name}: ingredients is not a list")
            for value in ingredients:
                validate_value(value, path)


def validate_models() -> None:
    model_dir = RES / "assets/earth_to_stars/models/item"
    item_dir = RES / "assets/earth_to_stars/items"
    mesh_dir = RES / "assets/earth_to_stars/models/kenney/space_kit"
    mapping = {
        "starter_craft_visual": "craft_speederA.obj",
        "orbital_salvage_visual": "craft_miner.obj",
        "orbital_interceptor_visual": "craft_racer.obj",
    }
    resolved: set[str] = set()
    for item_id, mesh in mapping.items():
        definition = json.loads(text(item_dir / f"{item_id}.json"))
        expected_item_model = f"earth_to_stars:item/{item_id}"
        if definition.get("model", {}).get("model") != expected_item_model:
            fail(f"{item_id}: item definition does not point to {expected_item_model}")

        model = json.loads(text(model_dir / f"{item_id}.json"))
        expected_mesh = f"earth_to_stars:models/kenney/space_kit/{mesh}"
        if model.get("loader") != "neoforge:obj" or model.get("model") != expected_mesh:
            fail(f"{item_id}: expected NeoForge OBJ model {expected_mesh}")
        resolved.add(model["model"])

        obj = mesh_dir / mesh
        mtl = mesh_dir / mesh.replace(".obj", ".mtl")
        for asset in (obj, mtl):
            body = text(asset)
            if len(body) < 100 or "Created by Kenney" not in body:
                fail(f"invalid or unprovenanced Kenney asset: {asset.relative_to(PROJECT)}")

    if len(resolved) != 3:
        fail("starter craft, salvage and interceptor must use distinct spacecraft meshes")

    kit_model = json.loads(text(model_dir / "launch_craft_kit.json"))
    if kit_model.get("loader") != "neoforge:obj" or "craft_speederA.obj" not in kit_model.get("model", ""):
        fail("launch craft kit still uses a vanilla placeholder instead of the production craft mesh")


def main() -> None:
    try:
        validate_no_runtime_proxies()
        validate_passenger_contract()
        validate_supply_feedback()
        validate_recipe_syntax()
        validate_models()
    except (OSError, json.JSONDecodeError, AcceptanceError) as exc:
        raise SystemExit(f"ALPHA.12 ACCEPTANCE VALIDATION FAILED: {exc}") from exc
    print("ALPHA.12 ACCEPTANCE VALIDATION OK: actual passenger contract, safe retirement, actionbar supply UX, 26.2 recipes, and three distinct Kenney OBJ visuals verified")


if __name__ == "__main__":
    main()

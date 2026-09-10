#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path

import validate_alpha12_acceptance as alpha12

PROJECT = Path(__file__).resolve().parents[1]
JAVA = PROJECT / "src/main/java"
RES = PROJECT / "src/main/resources"


class AcceptanceError(RuntimeError):
    pass


def fail(message: str) -> None:
    raise AcceptanceError(message)


def text(path: Path) -> str:
    if not path.is_file():
        fail(f"missing required file: {path.relative_to(PROJECT)}")
    return path.read_text(encoding="utf-8")


def validate_target() -> None:
    props = text(PROJECT / "gradle.properties")
    for required in ("minecraft_version=26.2", "neo_version=26.2.0.76", "mod_version=0.1.0-alpha.14"):
        if required not in props:
            fail(f"target/version contract missing: {required}")


def validate_single_visible_vehicle() -> None:
    exterior = text(JAVA / "kr/moonseungjun/earthtostars/ship/runtime/minecraft/ShipExteriorEntity.java")
    if "extends Display.ItemDisplay" not in exterior:
        fail("starter craft exterior is not the visible ItemDisplay vehicle")
    for required in ("setVisualItem", "isPickable()", "boardAndControl(serverPlayer, this)", "PROPELLANT_CELL", "OXYGEN_CARTRIDGE"):
        if required not in exterior:
            fail(f"visible vehicle interaction contract missing: {required}")

    runtime = text(JAVA / "kr/moonseungjun/earthtostars/ship/runtime/minecraft/ShipRuntimeManager.java")
    create_body = runtime.split("private static VehiclePair createVehiclePair", 1)[1].split("private static void executeTransition", 1)[0]
    if "SpaceVisualFactory.create" in create_body:
        fail("starter craft still spawns a separate visual proxy")
    for required in (
        "exterior.setVisualItem(EarthToStarsItems.STARTER_CRAFT_VISUAL.get())",
        "new VehiclePair(exterior, exterior)",
        "player.startRiding(exterior)",
        "boardAndControl(player, entry.exterior(), tick)",
    ):
        if required not in runtime:
            fail(f"single visible vehicle/boarding contract missing: {required}")

    entities = text(JAVA / "kr/moonseungjun/earthtostars/content/EarthToStarsEntities.java")
    if ".sized(5.4F, 2.3F)" not in entities:
        fail("starter craft hitbox did not grow with the 5-block-class visible hull")
    if "EntityAttachment.PASSENGER, 0.0F, 1.20F, 0.35F" not in entities:
        fail("pilot seat is not aligned to the enlarged craft")


def validate_deployment_and_service_ux() -> None:
    launch = text(JAVA / "kr/moonseungjun/earthtostars/content/LaunchCraftKitItem.java")
    if "DEPLOY_RADIUS = 2" not in launch or "state.canBeReplaced()" not in launch or "level.removeBlock(pos, false)" not in launch:
        fail("deployment does not accept and clear harmless replaceable vegetation")
    if "3×3×3" in launch or "3x3x3" in launch:
        fail("obsolete tiny deployment footprint remains")

    supply = text(JAVA / "kr/moonseungjun/earthtostars/content/ShipSupplyItem.java")
    if "loadSupply(player" in supply:
        fail("supply item still magically loads a nearby ship from arbitrary block interaction")
    if "message.earth_to_stars.supply.use_on_ship" not in supply:
        fail("supply item does not direct service to the actual hull")

    runtime = text(JAVA / "kr/moonseungjun/earthtostars/ship/runtime/minecraft/ShipRuntimeManager.java")
    if "ShipSystemsManager.loadSupply(" not in runtime or "ship, type, player.level().getServer())" not in runtime:
        fail("hull service is not bound to the exact authoritative ship entry")


def obj_bounds_and_uv(path: Path) -> tuple[float, float, float, float, float, float, int]:
    xs, ys, zs = [], [], []
    uv_count = 0
    for line in text(path).splitlines():
        if line.startswith("v "):
            _, x, y, z, *_ = line.split()
            xs.append(float(x)); ys.append(float(y)); zs.append(float(z))
        elif line.startswith("vt "):
            parts = line.split()
            uv_count += 1
            if len(parts) < 3 or abs(float(parts[1]) - 0.5) > 1e-7 or abs(float(parts[2]) - 0.5) > 1e-7:
                fail(f"{path.name}: runtime UV can still bleed across the item atlas")
    if not xs or uv_count == 0:
        fail(f"{path.name}: runtime OBJ is incomplete")
    return min(xs), max(xs), min(ys), max(ys), min(zs), max(zs), uv_count


def validate_runtime_models() -> None:
    runtime_dir = RES / "assets/earth_to_stars/models/runtime"
    starter = obj_bounds_and_uv(runtime_dir / "starter_craft.obj")
    width = starter[1] - starter[0]
    height = starter[3] - starter[2]
    length = starter[5] - starter[4]
    if width < 5.0 or length < 5.0 or height < 1.8:
        fail(f"starter craft is still toy-sized: {width:.2f} x {height:.2f} x {length:.2f}")

    mapping = {
        "starter_craft_visual.json": "earth_to_stars:models/runtime/starter_craft.obj",
        "launch_craft_kit.json": "earth_to_stars:models/runtime/starter_craft.obj",
        "orbital_salvage_visual.json": "earth_to_stars:models/runtime/orbital_salvage.obj",
        "orbital_interceptor_visual.json": "earth_to_stars:models/runtime/orbital_interceptor.obj",
    }
    for name, expected in mapping.items():
        model = json.loads(text(RES / "assets/earth_to_stars/models/item" / name))
        if model.get("loader") != "neoforge:obj" or model.get("model") != expected:
            fail(f"{name}: expected adapted runtime model {expected}")
    obj_bounds_and_uv(runtime_dir / "orbital_salvage.obj")
    obj_bounds_and_uv(runtime_dir / "orbital_interceptor.obj")


def main() -> None:
    try:
        alpha12.validate_no_runtime_proxies()
        alpha12.validate_26_2_runtime_api_contract()
        alpha12.validate_recipe_syntax()
        validate_target()
        validate_single_visible_vehicle()
        validate_deployment_and_service_ux()
        validate_runtime_models()
    except (OSError, json.JSONDecodeError, AcceptanceError, alpha12.AcceptanceError) as exc:
        raise SystemExit(f"ALPHA.14 ACCEPTANCE VALIDATION FAILED: {exc}") from exc
    print("ALPHA.14 ACCEPTANCE VALIDATION OK: one visible ride entity, 5-block-class hull/hitbox, vegetation-tolerant deployment, hull-directed servicing and atlas-safe runtime OBJ meshes verified")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

import validate_alpha12_acceptance as alpha12

PROJECT = Path(__file__).resolve().parents[1]
RES = PROJECT / "src/main/resources"


class AcceptanceError(RuntimeError):
    pass


def fail(message: str) -> None:
    raise AcceptanceError(message)


def read_text(path: Path) -> str:
    if not path.is_file():
        fail(f"missing required file: {path.relative_to(PROJECT)}")
    return path.read_text(encoding="utf-8")


def validate_runtime_baseline() -> None:
    alpha12.validate_no_runtime_proxies()
    alpha12.validate_passenger_contract()
    alpha12.validate_supply_feedback()
    alpha12.validate_recipe_syntax()


def validate_target_runtime() -> None:
    props = read_text(PROJECT / "gradle.properties")
    if "minecraft_version=26.2" not in props:
        fail("Minecraft target drifted from 26.2")
    if "neo_version=26.2.0.76" not in props:
        fail("alpha.13 must compile against the live-tested NeoForge 26.2.0.76 runtime")
    if "mod_version=0.1.0-alpha.13" not in props:
        fail("alpha.13 version bump is missing")


def validate_obj_material_adapter() -> None:
    model_dir = RES / "assets/earth_to_stars/models/item"
    mesh_dir = RES / "assets/earth_to_stars/models/kenney/space_kit"
    texture = RES / "assets/earth_to_stars/textures/item/kenney_material_base.png"

    if not texture.is_file():
        fail("Kenney material adapter texture is missing")
    png = texture.read_bytes()
    if len(png) < 60 or not png.startswith(b"\x89PNG\r\n\x1a\n"):
        fail("Kenney material adapter is not a valid packaged PNG")

    for path in mesh_dir.iterdir():
        if path.name != path.name.lower():
            fail(f"uppercase resource path is illegal in Minecraft 26.2: {path.name}")

    mapping = {
        "starter_craft_visual": ("craft_speedera.obj", "craft_speedera.mtl"),
        "orbital_salvage_visual": ("craft_miner.obj", "craft_miner.mtl"),
        "orbital_interceptor_visual": ("craft_racer.obj", "craft_racer.mtl"),
    }
    model_ids = set()
    for model_id, (obj_name, mtl_name) in mapping.items():
        model = json.loads(read_text(model_dir / f"{model_id}.json"))
        expected_obj = f"earth_to_stars:models/kenney/space_kit/{obj_name}"
        expected_mtl = f"earth_to_stars:models/kenney/space_kit/{mtl_name}"
        if model.get("loader") != "neoforge:obj":
            fail(f"{model_id}: NeoForge OBJ loader missing")
        if model.get("model") != expected_obj:
            fail(f"{model_id}: expected lowercase OBJ path {expected_obj}")
        if model.get("mtl_override") != expected_mtl:
            fail(f"{model_id}: expected explicit material override {expected_mtl}")
        if model.get("textures", {}).get("particle") != "earth_to_stars:item/kenney_material_base":
            fail(f"{model_id}: material adapter particle texture missing")
        model_ids.add(model["model"])

        obj = mesh_dir / obj_name
        mtl = mesh_dir / mtl_name
        if not obj.is_file() or obj.stat().st_size < 1000:
            fail(f"{model_id}: packaged OBJ is missing or implausibly small")
        body = read_text(mtl)
        materials = len(re.findall(r"(?m)^newmtl\s+", body))
        maps = re.findall(r"(?m)^map_Kd\s+(.+)$", body)
        if materials == 0 or len(maps) != materials:
            fail(f"{model_id}: every MTL material must have a diffuse texture slot")
        if any(value.strip() != "earth_to_stars:item/kenney_material_base" for value in maps):
            fail(f"{model_id}: MTL points outside the approved Kenney adapter texture")

    if len(model_ids) != 3:
        fail("starter craft, salvage and interceptor must keep distinct spacecraft meshes")

    launch = json.loads(read_text(model_dir / "launch_craft_kit.json"))
    if launch.get("model") != "earth_to_stars:models/kenney/space_kit/craft_speedera.obj":
        fail("launch craft kit still references the invalid mixed-case OBJ path")
    if launch.get("mtl_override") != "earth_to_stars:models/kenney/space_kit/craft_speedera.mtl":
        fail("launch craft kit material override is missing")
    if launch.get("textures", {}).get("particle") != "earth_to_stars:item/kenney_material_base":
        fail("launch craft kit adapter texture is missing")


def main() -> None:
    try:
        validate_runtime_baseline()
        validate_target_runtime()
        validate_obj_material_adapter()
    except (OSError, json.JSONDecodeError, AcceptanceError, alpha12.AcceptanceError) as exc:
        raise SystemExit(f"ALPHA.13 ACCEPTANCE VALIDATION FAILED: {exc}") from exc
    print("ALPHA.13 ACCEPTANCE VALIDATION OK: authoritative ship rescue preserved; lowercase OBJ paths, explicit MTL texture slots, adapter texture and NeoForge 26.2.0.76 target verified")


if __name__ == "__main__":
    main()

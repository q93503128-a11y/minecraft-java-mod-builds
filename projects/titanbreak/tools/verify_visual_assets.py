#!/usr/bin/env python3
"""TITANBREAK visual/asset contract verifier.

Keeps GeckoLib geometry, animation references and entity texture mappings coherent.
This is intentionally asset-structure validation; gameplay/runtime behavior remains
covered by the dedicated P0/B08/B09/B10 gates.
"""
from __future__ import annotations

import json
import math
import re
import sys
from pathlib import Path

PROJECT = Path(__file__).resolve().parents[1]
ASSETS = PROJECT / "src/main/resources/assets/titanbreak"
MODELS = ASSETS / "geckolib/models/entity"
ANIMATIONS = ASSETS / "geckolib/animations/entity"
TEXTURES = ASSETS / "textures/entity"

TARGETS = {
    "gravemarch_colossus": {
        "identifier": "geometry.titanbreak.gravemarch_colossus",
        "min_cubes": 40,
        "required": {
            "root", "pelvis", "torso", "head", "left_arm", "right_arm",
            "left_leg", "right_leg", "heart_core", "dorsal_impact_keel",
            "back_armor_center", "back_armor_left", "back_armor_right",
        },
    },
    "bastion_walker": {
        "identifier": "geometry.titanbreak.bastion_walker",
        "min_cubes": 45,
        "required": {
            "root", "chassis", "deck", "turret_bank", "power_core", "defense_aperture",
            "leg_nw", "leg_ne", "leg_sw", "leg_se", "tower_left",
            "tower_right", "gun_left", "gun_right", "ram_front",
        },
    },
    "storm_leviathan": {
        "identifier": "geometry.titanbreak.storm_leviathan",
        "min_cubes": 45,
        "required": {
            "root", "body_front", "body_mid", "body_rear", "tail", "head",
            "jaw", "wing_0", "wing_1", "wing_2", "wing_3", "sensor_crest",
            "storm_organ", "tail_fin_upper", "tail_fin_lower",
        },
    },
    "ash_titan": {
        "identifier": "geometry.titanbreak.ash_titan",
        "min_cubes": 40,
        "required": {
            "root", "pelvis", "torso_lower", "torso_upper", "head", "jaw",
            "left_arm", "right_arm", "left_leg", "right_leg", "radiant_heart",
            "furnace_frame", "sensor_cowl", "heat_vent_left", "heat_vent_right",
        },
    },
    "bulwark": {
        "identifier": "geometry.titanbreak.bulwark",
        "min_cubes": 24,
        "required": {
            "root", "pelvis", "torso", "head", "left_arm", "right_arm",
            "left_leg", "right_leg", "shield", "shield_boss",
            "rampart_left", "rampart_right",
        },
    },
    "howler": {
        "identifier": "geometry.titanbreak.howler",
        "min_cubes": 30,
        "required": {
            "root", "pelvis", "torso", "resonator", "head", "jaw",
            "horn_left", "horn_right", "left_arm", "right_arm",
            "left_leg", "right_leg", "throat_bellows",
            "resonance_ring_outer", "resonance_ring_inner",
        },
    },
    "chrono_hound": {
        "identifier": "geometry.titanbreak.chrono_hound",
        "min_cubes": 36,
        "required": {"root","body","head","jaw","spine_fins","temporal_core","chrono_ring","leg_fl","leg_fr","leg_bl","leg_br","tail_base","tail_tip"},
    },
    "null_eye": {
        "identifier": "geometry.titanbreak.null_eye",
        "min_cubes": 34,
        "required": {"root","core","eye","halo_left","halo_right","antenna","tendril_left","tendril_right","tendril_back","jammer_ring_outer","jammer_ring_inner","jammer_coil"},
    },
    "iron_maw": {
        "identifier": "geometry.titanbreak.iron_maw",
        "min_cubes": 36,
        "required": {"root","body","head","jaw","left_arm","right_arm","left_leg","right_leg","clamp_left","clamp_right","impact_chest","spine_brace"},
    },
    "revenant": {
        "identifier": "geometry.titanbreak.revenant",
        "min_cubes": 32,
        "required": {"root","body","head","left_arm","right_arm","left_leg","right_leg","core_a","core_b","core_c","regen_bridge_ab","regen_bridge_c","tumor_left","tumor_right"},
    },
    "apex_stalker": {
        "identifier": "geometry.titanbreak.apex_stalker",
        "min_cubes": 33,
        "required": {"root","body","head","cloak_left","cloak_right","left_arm","right_arm","left_leg","right_leg","left_blade","right_blade","sensor_crest","optic_node_left","optic_node_right"},
    },
    "shock_choir": {
        "identifier": "geometry.titanbreak.shock_choir",
        "min_cubes": 33,
        "required": {"root","body","head","left_arm","right_arm","left_leg","right_leg","chest_coil","spire_left","spire_right","overload_ring","rear_capacitor"},
    },
    "siegeback": {
        "identifier": "geometry.titanbreak.siegeback",
        "min_cubes": 40,
        "required": {"root","body","head","left_arm","right_arm","left_leg","right_leg","dorsal_cannon","front_plate","bunker_shell","ram_keel"},
    },
    "phase_lurker": {
        "identifier": "geometry.titanbreak.phase_lurker",
        "min_cubes": 35,
        "required": {"root","body","head","left_arm","right_arm","left_leg","right_leg","phase_ring","phase_spines","distortion_core","phase_anchor_left","phase_anchor_right"},
    },
    "warden_node": {
        "identifier": "geometry.titanbreak.warden_node",
        "min_cubes": 36,
        "required": {"root","body","head","left_arm","right_arm","left_leg","right_leg","command_node","left_antenna","right_antenna","halo","signal_mast","formation_emitter_left","formation_emitter_right"},
    },
    "harvester": {
        "identifier": "geometry.titanbreak.harvester",
        "min_cubes": 37,
        "required": {"root","body","head","left_arm","right_arm","left_leg","right_leg","harvest_vat","left_claw","right_claw","feeder_tube","brood_pod","spawn_cradle"},
    },
}

# This model intentionally uses a renderer-specific material path and is not
# required to follow the ordinary stem-based texture contract.
TEXTURE_MAPPING_EXCEPTIONS = {"hollow_colossus"}


def fail(errors: list[str], message: str) -> None:
    errors.append(message)


def load_json(path: Path, errors: list[str]):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        fail(errors, f"{path.relative_to(PROJECT)}: JSON parse failed: {exc}")
        return None


def finite_vector(value, length: int) -> bool:
    return (
        isinstance(value, list)
        and len(value) == length
        and all(isinstance(v, (int, float)) and not isinstance(v, bool) and math.isfinite(v) for v in value)
    )


def validate_geometry(path: Path, errors: list[str]) -> set[str]:
    doc = load_json(path, errors)
    if not isinstance(doc, dict):
        return set()
    geometries = doc.get("minecraft:geometry")
    if not isinstance(geometries, list) or len(geometries) != 1:
        fail(errors, f"{path.name}: expected exactly one minecraft:geometry entry")
        return set()
    geometry = geometries[0]
    if not isinstance(geometry, dict):
        fail(errors, f"{path.name}: geometry entry must be an object")
        return set()

    description = geometry.get("description", {})
    if not isinstance(description, dict):
        fail(errors, f"{path.name}: missing geometry description")
    else:
        identifier = description.get("identifier")
        if not isinstance(identifier, str) or not identifier.startswith("geometry.titanbreak."):
            fail(errors, f"{path.name}: invalid identifier {identifier!r}")
        for key in ("texture_width", "texture_height"):
            value = description.get(key)
            if not isinstance(value, (int, float)) or value <= 0:
                fail(errors, f"{path.name}: invalid {key}")

    bones = geometry.get("bones")
    if not isinstance(bones, list) or not bones:
        fail(errors, f"{path.name}: bones must be a non-empty array")
        return set()

    names: list[str] = []
    by_name: dict[str, dict] = {}
    for index, bone in enumerate(bones):
        if not isinstance(bone, dict):
            fail(errors, f"{path.name}: bone #{index} is not an object")
            continue
        name = bone.get("name")
        if not isinstance(name, str) or not name:
            fail(errors, f"{path.name}: bone #{index} has invalid name")
            continue
        names.append(name)
        by_name[name] = bone

    if len(names) != len(set(names)):
        duplicates = sorted({name for name in names if names.count(name) > 1})
        fail(errors, f"{path.name}: duplicate bone names: {duplicates}")

    name_set = set(names)
    parents: dict[str, str] = {}
    cube_count = 0
    for name, bone in by_name.items():
        pivot = bone.get("pivot")
        if pivot is not None and not finite_vector(pivot, 3):
            fail(errors, f"{path.name}:{name}: invalid pivot")
        rotation = bone.get("rotation")
        if rotation is not None and not finite_vector(rotation, 3):
            fail(errors, f"{path.name}:{name}: invalid rotation")
        parent = bone.get("parent")
        if parent is not None:
            if parent == name:
                fail(errors, f"{path.name}:{name}: self-parent")
            elif parent not in name_set:
                fail(errors, f"{path.name}:{name}: missing parent {parent!r}")
            else:
                parents[name] = parent

        cubes = bone.get("cubes", [])
        if not isinstance(cubes, list):
            fail(errors, f"{path.name}:{name}: cubes must be an array")
            continue
        cube_count += len(cubes)
        for cindex, cube in enumerate(cubes):
            if not isinstance(cube, dict):
                fail(errors, f"{path.name}:{name}: cube #{cindex} is not an object")
                continue
            if not finite_vector(cube.get("origin"), 3):
                fail(errors, f"{path.name}:{name}: cube #{cindex} invalid origin")
            size = cube.get("size")
            if not finite_vector(size, 3) or any(v < 0 for v in size):
                fail(errors, f"{path.name}:{name}: cube #{cindex} invalid size")
            uv = cube.get("uv")
            if not finite_vector(uv, 2):
                fail(errors, f"{path.name}:{name}: cube #{cindex} invalid uv")
            inflate = cube.get("inflate")
            if inflate is not None and (
                not isinstance(inflate, (int, float))
                or isinstance(inflate, bool)
                or not math.isfinite(inflate)
            ):
                fail(errors, f"{path.name}:{name}: cube #{cindex} invalid inflate")

    for start in name_set:
        seen: set[str] = set()
        cursor = start
        while cursor in parents:
            cursor = parents[cursor]
            if cursor in seen:
                fail(errors, f"{path.name}: parent cycle involving {start!r}")
                break
            seen.add(cursor)

    stem = path.name.removesuffix(".geo.json")
    contract = TARGETS.get(stem)
    if contract:
        expected_identifier = contract["identifier"]
        actual_identifier = description.get("identifier") if isinstance(description, dict) else None
        if actual_identifier != expected_identifier:
            fail(errors, f"{path.name}: expected identifier {expected_identifier!r}, got {actual_identifier!r}")
        missing = sorted(contract["required"] - name_set)
        if missing:
            fail(errors, f"{path.name}: missing protected visual contract bones: {missing}")
        if cube_count < contract["min_cubes"]:
            fail(errors, f"{path.name}: protected cube count {cube_count} < {contract['min_cubes']}")

    return name_set


def animation_bone_references(doc) -> set[str]:
    refs: set[str] = set()
    if not isinstance(doc, dict):
        return refs
    animations = doc.get("animations", {})
    if not isinstance(animations, dict):
        return refs
    for animation in animations.values():
        if not isinstance(animation, dict):
            continue
        bones = animation.get("bones", {})
        if isinstance(bones, dict):
            refs.update(str(name) for name in bones)
    return refs


def main() -> int:
    errors: list[str] = []

    gradle = (PROJECT / "gradle.properties").read_text(encoding="utf-8")
    match = re.search(r"^mod_version=(\S+)$", gradle, re.MULTILINE)
    if not match:
        fail(errors, "gradle.properties: mod_version missing")

    model_bones: dict[str, set[str]] = {}
    model_files = sorted(MODELS.glob("*.geo.json"))
    if not model_files:
        fail(errors, "no GeckoLib entity models found")
    for model in model_files:
        stem = model.name.removesuffix(".geo.json")
        model_bones[stem] = validate_geometry(model, errors)

    for target in TARGETS:
        if target not in model_bones:
            fail(errors, f"protected target model missing: {target}")

    for animation_path in sorted(ANIMATIONS.glob("*.animation.json")):
        stem = animation_path.name.removesuffix(".animation.json")
        doc = load_json(animation_path, errors)
        if stem not in model_bones:
            fail(errors, f"{animation_path.name}: no matching geometry")
            continue
        missing = sorted(animation_bone_references(doc) - model_bones[stem])
        if missing:
            fail(errors, f"{animation_path.name}: animation references missing bones: {missing}")

    for stem in model_bones:
        if stem in TEXTURE_MAPPING_EXCEPTIONS:
            continue
        texture = TEXTURES / f"{stem}.png"
        if not texture.is_file():
            fail(errors, f"{stem}.geo.json: missing matching texture textures/entity/{stem}.png")

    if errors:
        print("TITANBREAK visual verifier FAILED", file=sys.stderr)
        for error in errors:
            print(f" - {error}", file=sys.stderr)
        return 1

    target_summary = ", ".join(
        f"{name}:{sum(len(b.get('cubes', [])) for b in json.loads((MODELS / f'{name}.geo.json').read_text())['minecraft:geometry'][0]['bones'])}c"
        for name in TARGETS
    )
    print(f"TITANBREAK visual verifier PASS ({len(model_files)} models; protected {target_summary})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""COPY-ONLY dual-boundary experiment for Drehmal's independent 8th and 9th layers.

This is NOT a production map patch. The nine nonweapon rewards have NOT been
approved for gameplay removal; only the primordial-root host teleport link
has a controlled two-player causal server test. All original functions stay.
"""
from __future__ import annotations

import argparse
import io
import json
import sys
import zipfile
from pathlib import Path

import prepare_drehmal_isolated_copy as six
import verify_drehmal_start_chain_isolation as seven
import preview_drehmal_nonweapon_callbacks as eight
import preview_drehmal_primordial_root_ninth as nine

PACK = nine.PACK
RECEIPT = "turnbound-eight-nine-combined-experiment-receipt.json"
BACKUP = "turnbound-eight-nine-combined-experiment-backup.zip"
STATUS = "EIGHT_NINE_COMBINED_OFFLINE_EXPERIMENT_NOT_GAMEPLAY_APPROVED"
EXPECTED_MEMBERS = frozenset(eight.TARGET_NAMES | {nine.selective.PRIMORDIAL_ENTRY})
DISALLOWED_OVERLAYS = (
    eight.RECEIPT, eight.BACKUP, nine.RECEIPT, nine.BACKUP,
    RECEIPT, BACKUP,
)


def digest(raw):
    return eight.hash_bytes(raw)


def source_guard(world):
    world = Path(world).resolve(strict=True)
    if (world / "turnbound-world.marker").exists():
        raise ValueError("Activated TURNBOUND world is forbidden")
    seven.verify(world)
    pack = world / PACK
    if not pack.is_file() or pack.is_symlink():
        raise ValueError("Source datapack is missing or symlinked")
    return world, pack


def preflight(world, targets):
    world, pack = source_guard(world)
    if any((world / name).exists() for name in DISALLOWED_OVERLAYS):
        raise ValueError("A separate overlay or interrupted combined operation exists")
    if set(targets) != eight.TARGET_NAMES or len(EXPECTED_MEMBERS) != 10:
        raise ValueError("The two reviewed reward target inventories overlap or drifted")
    before = pack.read_bytes()
    baseline = six.receipt(world, six.ROOT_RECEIPT)["patched_pack_sha256"]
    if digest(before) != baseline:
        raise ValueError("Seven-stage source pack differs from pinned baseline")
    after = combine_and_compare(before, targets)
    return before, after


def combine_and_compare(before, targets):
    """Both application orders must produce identical ZIP bytes and 10 exact edits."""
    if set(targets) != eight.TARGET_NAMES:
        raise ValueError("Invalid eighth-layer callback inventory")
    if nine.selective.PRIMORDIAL_ENTRY in targets:
        raise ValueError("Eighth and ninth layer targets overlap")
    eight_only = eight.repack(before, targets)
    ninth_only = nine.selective.repack_without_primordial_reward(before)
    eight_then_ninth = nine.selective.repack_without_primordial_reward(eight_only)
    ninth_then_eight = eight.repack(ninth_only, targets)
    if eight_then_ninth != ninth_then_eight:
        raise ValueError("Ninth and eighth selective transformations are not commutative")
    after = eight_then_ninth
    # Verify each single layer against its own original baseline.
    eight.compare(before, eight_only, targets)
    eight.compare(ninth_only, after, targets)
    with zipfile.ZipFile(io.BytesIO(before)) as origin, zipfile.ZipFile(io.BytesIO(after)) as result:
        names = origin.namelist()
        if len(names) != len(set(names)) or result.namelist() != names:
            raise ValueError("Original datapack ZIP inventory changed")
        if origin.comment != result.comment:
            raise ValueError("Original datapack ZIP comment changed")
        changed = {n for n in names if origin.read(n) != result.read(n)}
        if changed != EXPECTED_MEMBERS:
            raise ValueError("Combined preview did not change exactly the ten intended members")
        primordial = nine.selective.PRIMORDIAL_ENTRY
        if result.read(primordial) != nine.selective.filtered_advancement(origin.read(primordial)):
            raise ValueError("Original primordial reward, criteria or other fields drifted")
        for member in eight.TARGET_NAMES:
            if result.read(member) != eight.transform_entry(origin.read(member), *targets[member]):
                raise ValueError("An eighth-layer advancement changed outside its reward link")
    return after


def apply(world, targets):
    world, pack = source_guard(world)
    before, after = preflight(world, targets)
    backup = world / BACKUP
    receipt = world / RECEIPT
    backup.write_bytes(before)
    if digest(backup.read_bytes()) != digest(before):
        raise ValueError("Original seven-stage pack backup failed integrity check")
    record = {
        "schema": 1, "status": STATUS,
        "original_seven_stage_pack_sha256": digest(before),
        "combined_pack_sha256": digest(after),
        "source_eighth_review_sha256": eight.SOURCE_SHA,
        "ninth_advancement_id": nine.selective.PRIMORDIAL_ID,
        "ninth_reward_function": nine.selective.PRIMORDIAL_REWARD,
        "targets": {
            name: {"sha": sha, "callback": callback}
            for name, (sha, callback) in sorted(targets.items())
        },
        "eighth_reward_links_test_disabled": 9,
        "ninth_reward_links_test_disabled": 1,
        "changed_original_zip_members": 10,
        "application_order_independent": True,
        "original_function_bodies_unchanged": True,
        "world_activated": False,
        "eighth_layer_gameplay_removal_approved": False,
        "real_facility_playtest": False,
        "map_separation_complete": False,
    }
    receipt.write_text(json.dumps(record, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    nine.atomic_write(pack, after)
    return verify(world)


def verify(world):
    world, pack = source_guard(world)
    backup = world / BACKUP
    receipt = world / RECEIPT
    if any(not p.is_file() or p.is_symlink() for p in (backup, receipt)):
        raise ValueError("Combined receipt or backup missing or unsafe")
    record = json.loads(receipt.read_text(encoding="utf-8"))
    if (
        record.get("schema") != 1 or record.get("status") != STATUS
        or record.get("source_eighth_review_sha256") != eight.SOURCE_SHA
        or record.get("ninth_advancement_id") != nine.selective.PRIMORDIAL_ID
        or record.get("ninth_reward_function") != nine.selective.PRIMORDIAL_REWARD
        or record.get("eighth_reward_links_test_disabled") != 9
        or record.get("ninth_reward_links_test_disabled") != 1
        or record.get("changed_original_zip_members") != 10
        or record.get("application_order_independent") is not True
        or record.get("original_function_bodies_unchanged") is not True
        or record.get("world_activated") is not False
        or record.get("eighth_layer_gameplay_removal_approved") is not False
        or record.get("map_separation_complete") is not False
        or set(record.get("targets", {})) != eight.TARGET_NAMES
    ):
        raise ValueError("Unrecognized or tampered combined experiment receipt")
    before, after = backup.read_bytes(), pack.read_bytes()
    baseline = six.receipt(world, six.ROOT_RECEIPT)["patched_pack_sha256"]
    if digest(before) != baseline or digest(before) != record["original_seven_stage_pack_sha256"]:
        raise ValueError("Combined backup differs from verified seven-stage baseline")
    if digest(after) != record["combined_pack_sha256"]:
        raise ValueError("Combined pack changed after application")
    targets = {
        name: (row["sha"], row["callback"])
        for name, row in record["targets"].items()
    }
    if combine_and_compare(before, targets) != after:
        raise ValueError("Combined datapack differs from exact reviewed transformations")
    return {**record, "verified": True}


def restore(world):
    record = verify(world)
    world, pack = source_guard(world)
    nine.atomic_write(pack, (world / BACKUP).read_bytes())
    if digest(pack.read_bytes()) != record["original_seven_stage_pack_sha256"]:
        raise ValueError("Failed to restore original seven-stage pack byte-for-byte")
    (world / RECEIPT).unlink()
    (world / BACKUP).unlink()
    seven.verify(world)
    if digest(pack.read_bytes()) != six.receipt(world, six.ROOT_RECEIPT)["patched_pack_sha256"]:
        raise ValueError("Restored seven-stage baseline is not identical")
    return {
        "status": "EIGHT_NINE_EXPERIMENT_RESTORED_EXACTLY_TO_SEVEN",
        "restored_seven_stage_pack_sha256": digest(pack.read_bytes()),
        "eighth_layer_gameplay_removal_approved": False,
        "map_separation_complete": False,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("action", choices=("inspect", "apply", "verify", "restore"))
    parser.add_argument("--world-copy", required=True, type=Path)
    parser.add_argument("--source-review", type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    world = args.world_copy.resolve(strict=True)
    report = args.output.resolve()
    if report == world or world in report.parents:
        parser.error("Derived report must stay outside original world copy")
    if args.action in ("inspect", "apply"):
        if not args.source_review:
            parser.error("Source review is mandatory for inspect/apply")
        targets = eight.load_targets(args.source_review)
        if args.action == "inspect":
            before, after = preflight(world, targets)
            result = {
                "status": "EIGHT_NINE_COMBINED_PREFLIGHT_ONLY",
                "seven_stage_pack_sha256": digest(before),
                "proposed_combined_sha256": digest(after),
                "changed_original_zip_members": 10,
                "world_modified": False,
                "map_separation_complete": False,
            }
        else:
            result = apply(world, targets)
    elif args.action == "verify":
        result = verify(world)
    else:
        result = restore(world)
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps({
        "status": result["status"],
        "verified": result.get("verified", False),
        "eighth_layer_gameplay_removal_approved": False,
        "map_separation_complete": False,
    }))


if __name__ == "__main__":
    try:
        main()
    except (ValueError, KeyError, TypeError, OSError, zipfile.BadZipFile) as exc:
        print("Drehmal combined offline experiment REFUSED: " + str(exc), file=sys.stderr)
        sys.exit(2)

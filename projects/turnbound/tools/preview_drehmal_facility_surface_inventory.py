#!/usr/bin/env python3
"""Read-only geometry inventory of externally documented Drehmal facility hints.

Never upgrades approximate wiki coordinates to an actual usable control without
an in-world interaction test. No original command body, NBT, or map is emitted.
The caller must supply the SHA-verified source and separate 7+9 copied world.
"""
from __future__ import annotations
import argparse
import collections
import json
import math
import sys
from pathlib import Path

import inspect_drehmal_teleport_footprints as official
import verify_drehmal_start_chain_isolation as seven
import preview_drehmal_primordial_root_ninth as nine
from audit_drehmal_starter_mechanisms import World

# Location *hints*, not exact block IDs or production travel/START coordinates.
# Foundry: wiki.drehmal.cyou/World/Points_of_Interest/Foundry/
# Av'Sal: wiki.drehmal.cyou/Story_and_Features/Holotexts/Early-Game_Holotexts/Av%27Sal/avsal_door/
# Godmachine: wiki.drehmal.cyou/Story_and_Features/Holotexts/Post-Yav/Azimuth_Labs/gm_apotheosis/
# Terminus: wiki.drehmal.cyou/Story_and_Features/Terminus_Network/
SITES = (
    ("foundry_lever_1", (-3130, 148, 1490), 10, 8, "lever"),
    ("foundry_lever_2", (-3035, 121, 1186), 10, 8, "lever"),
    ("foundry_lever_3", (-3394, 145, 1082), 10, 8, "lever"),
    ("avsal_holodoor", (-251, 75, 1646), 10, 9, "door_and_signal"),
    ("godmachine_nearby_button", (3023, 83, -569), 13, 10, "button"),
    ("capital_valley_terminus", (559, 129, 1177), 11, 9, "button"),
)
CONTROL_CATEGORIES=("lever","button","door","trapdoor","signal")


def category(block):
    id = block.removeprefix("minecraft:")
    if id == "lever":
        return "lever"
    if id.endswith("_button"):
        return "button"
    if id.endswith("_door"):
        return "door"
    if id.endswith("_trapdoor"):
        return "trapdoor"
    if id in ("redstone_wire", "redstone_block", "repeater", "comparator",
              "observer", "target", "tripwire_hook", "redstone_torch"):
        return "signal"
    return None


def inventory(view, site, center, horizontal, vertical, expected):
    if horizontal < 1 or horizontal > 18 or vertical < 1 or vertical > 12:
        raise ValueError("Facility scan radius exceeds bounded audit")
    counts = collections.Counter()
    candidates = []
    for x in range(center[0] - horizontal, center[0] + horizontal + 1):
        for z in range(center[2] - horizontal, center[2] + horizontal + 1):
            for y in range(center[1] - vertical, center[1] + vertical + 1):
                pos=(x,y,z)
                block = view.state(pos)["id"]
                kind = category(block)
                if kind is None:
                    continue
                counts[kind]+=1
                if kind == expected or expected == "door_and_signal" and kind in ("door","button","lever","signal"):
                    distance = math.dist(center,pos)
                    candidates.append((distance,pos,kind,block))
    candidates.sort(key=lambda v:(v[0],v[1]))
    # Fail closed on insufficient evidence; nearest block is *not* an approved
    # working interaction or proof of door/puzzle/portal functionality.
    return {
        "site":site,"wiki_hint":[*center],
        "search_horizontal_radius":horizontal,
        "search_vertical_radius":vertical,
        "expected_feature":expected,
        "control_counts":{key:counts[key] for key in CONTROL_CATEGORIES},
        "nearby_candidates":[
            {"position":[*p],"block_id":block,"kind":kind,
             "distance_from_hint":round(distance,3)}
            for distance,p,kind,block in candidates[:12]],
        "candidate_total":len(candidates),
        "exact_click_target_confirmed":False,
        "real_physical_interaction_tested":False,
    }


def compare(source, copy):
    original=Path(source).resolve(strict=True)
    staged=Path(copy).resolve(strict=True)
    if original==staged or staged in original.parents or original in staged.parents:
        raise ValueError("Original and candidate map copy must be independent")
    official.check_official_source(original)
    proof=nine.verify(staged)
    if not proof.get("verified") or proof.get("modified_datapack_members")!=1:
        raise ValueError("Only official verified 7+9 candidate map allowed")
    if (staged/"turnbound-world.marker").exists():
        raise ValueError("Activated game world forbidden")
    v1,v2=World(original),World(staged)
    snapshots=[]
    for site,center,h,v,kind in SITES:
        a=inventory(v1,site,center,h,v,kind)
        b=inventory(v2,site,center,h,v,kind)
        if a!=b:
            raise ValueError("Source and copied-map physical control palettes differ")
        snapshots.append(b)
    return {
        "status":"OFFICIAL_MAP_7_PLUS_9_READ_ONLY_FACILITY_SURFACE_INVENTORY",
        "site_count":len(snapshots),
        "source_original_sha_verified":True,
        "original_and_copied_block_states_identical_at_scanned_sites":True,
        "ninth_teleport_link_isolated":True,
        "eighth_unsafe_reward_callbacks_enabled":True,
        "source_map_modified":False,
        "user_world_activated":False,
        "facility_physical_interactions_tested":False,
        "doors_portals_puzzles_tested":False,
        "coordinates_approved_for_game_start":False,
        "map_separation_complete":False,
        "sites":snapshots,
    }


def main():
    p=argparse.ArgumentParser()
    p.add_argument("--original",type=Path,required=True)
    p.add_argument("--world-copy",type=Path,required=True)
    p.add_argument("--output",type=Path,required=True)
    a=p.parse_args()
    original,world=a.original.resolve(strict=True),a.world_copy.resolve(strict=True)
    dest=a.output.resolve()
    if any(dest==r or r in dest.parents for r in (original,world)):
        p.error("Metadata output must be outside original and copied worlds")
    result=compare(original,world)
    dest.parent.mkdir(parents=True,exist_ok=True)
    dest.write_text(json.dumps(result,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    print(json.dumps({k:result[k] for k in (
        "status","site_count","original_and_copied_block_states_identical_at_scanned_sites",
        "facility_physical_interactions_tested","map_separation_complete"
    )}))


if __name__=="__main__":
    try:main()
    except (ValueError,OSError,KeyError,TypeError) as exc:
        print("Drehmal facility surface survey REFUSED: "+str(exc),file=sys.stderr)
        sys.exit(2)

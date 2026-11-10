#!/usr/bin/env python3
"""Read-only geometry audit of CI teleport footprints on hash-pinned Drehmal 2.2.2f.

NO real player start location is selected. Only well-known vanilla full cubes
are accepted as test candidate floors; non-air feet/head are not automatically
assumed colliding (partial shapes are deliberately unresolved). No world bytes
or raw NBT are published.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import sys
from pathlib import Path

from audit_drehmal_starter_mechanisms import World, palette_index
from verify_drehmal_start_chain_isolation import verify as verify_seven

AIR=frozenset({"minecraft:air","minecraft:cave_air","minecraft:void_air"})
FLUID=frozenset({"minecraft:water","minecraft:lava"})
KNOWN_FULL=frozenset({
    "minecraft:stone","minecraft:deepslate","minecraft:cobblestone",
    "minecraft:cobbled_deepslate","minecraft:dirt","minecraft:grass_block",
    "minecraft:coarse_dirt","minecraft:podzol","minecraft:gravel",
    "minecraft:sand","minecraft:red_sand","minecraft:tuff",
    "minecraft:andesite","minecraft:diorite","minecraft:granite",
    "minecraft:calcite","minecraft:bedrock","minecraft:smooth_stone",
    "minecraft:polished_andesite","minecraft:polished_diorite",
    "minecraft:polished_granite","minecraft:stone_bricks",
    "minecraft:mossy_stone_bricks","minecraft:bricks",
    "minecraft:oak_planks","minecraft:spruce_planks",
    "minecraft:birch_planks","minecraft:dark_oak_planks",
    "minecraft:warped_planks","minecraft:crimson_planks",
})
SOURCE_LEVEL_SHA="44e9b625f9dd8616472bbae98b3481a2fb34a22a49a98faf913d3b7148a935d1"
SOURCE_PACK_SHA="f074d2927ea2947f14fe0b2836332695087e5bbf2e7c3b08f94f2e4f36c6fc74"
# CI targets from prior *observations*, never player-facing starts.
SITES=(
    ("previous_ci_airborne_target",(26458.5,223.0,-285.5)),
    ("post_displacement_observation",(26520.5,177.0,-84.5)),
    ("initial_bot_join_observation",(26519.5,177.0,-82.5)),
)

def validate_level(root):
    root=Path(root).resolve(strict=True)
    return root

def check_official_source(source):
    for rel,expected in (
        ("level.dat",SOURCE_LEVEL_SHA),
        ("datapacks/hi_drehmal.zip",SOURCE_PACK_SHA),
    ):
        path=source/rel
        if path.is_symlink() or not path.is_file():
            raise ValueError("Missing/replaced official source file")
        actual=hashlib.sha256(path.read_bytes()).hexdigest()
        if actual!=expected:
            raise ValueError("Official source SHA mismatch")
    return True

def full_block(view,x,y,z):
    if y< -64 or y>319:
        return ("minecraft:void_air",{})
    chunk=view.chunk(x,z)
    section=next((v for v in chunk.get("sections",[])
                  if isinstance(v,dict) and v.get("Y")==y//16),None)
    if section is None or not section.get("block_states"):
        return ("minecraft:air",{})
    blocks=section["block_states"]
    i=((y&15)<<8)|((z&15)<<4)|(x&15)
    picked=blocks["palette"][palette_index(
        blocks["palette"],blocks.get("data"),i)]
    if not isinstance(picked,dict) or not isinstance(picked.get("Name"),str):
        raise ValueError("Invalid official block palette state")
    return (picked["Name"],picked.get("Properties",{}))

def classify_space(ground,feet,head):
    if ground in FLUID or feet in FLUID or head in FLUID:
        return "FLUID_PRESENT_NOT_SAFE"
    if feet not in AIR or head not in AIR:
        return "FEET_OR_HEAD_NOT_AIR_COLLISION_UNRESOLVED"
    if ground in AIR:
        return "AIRBORNE_NO_FLOOR"
    if ground in KNOWN_FULL:
        return "CONSERVATIVE_FULL_FLOOR_TWO_BLOCK_AIR"
    return "UNKNOWN_FLOOR_SHAPE_NO_SAFETY_CLAIM"

def footprint(view,center,ground_y=None):
    if len(center)!=3 or not all(math.isfinite(v) for v in center):
        raise ValueError("Invalid probe position")
    x,y,z=center
    if abs((x%1)-0.5)>1e-7 or abs((z%1)-0.5)>1e-7:
        raise ValueError("Footprint audit requires centered CI X/Z only")
    if abs(y-round(y))>1e-7:
        raise ValueError("Footprint audit requires integer standing Y")
    px,pz=math.floor(x),math.floor(z)
    base=round(y)
    below=full_block(view,px,base-1,pz)[0]
    feet=full_block(view,px,base,pz)[0]
    head=full_block(view,px,base+1,pz)[0]
    above=full_block(view,px,base+2,pz)[0]
    return {"ground":below,"feet":feet,"head":head,"above_head":above,
            "standing_space":classify_space(below,feet,head),
            "centered_player_width_in_one_horizontal_block":True,
            "collision_shapes_runtime_verified":False}

def conservative_candidates(view,center,radius=5,y_range=12,max_results=6):
    if radius>8 or y_range>24 or max_results>12 or min(radius,y_range,max_results)<1:
        raise ValueError("Bounded geometry inspection required")
    x,y,z=center
    # We do not change any geometry or assert actual navigability. This yields
    # possible *synthetic experiment* positions near the old CI target only.
    found=[]
    for dx in range(-radius,radius+1):
        for dz in range(-radius,radius+1):
            for dy in range(-y_range,y_range+1):
                if dx==dz==dy==0:continue
                p=(x+dx,y+dy,z+dz)
                f=footprint(view,p)
                if f["standing_space"]=="CONSERVATIVE_FULL_FLOOR_TWO_BLOCK_AIR":
                    found.append((abs(dx)+abs(dz)+abs(dy),abs(dy),abs(dx)+abs(dz),p))
    found.sort()
    return [{"ci_only_candidate":[*p],
             "relative_to_old_target":[int(p[0]-x),int(p[1]-y),int(p[2]-z)],
             "candidate_role":"SYNTHETIC_TELEPORT_CONTROL_ONLY_NOT_APPROVED_START"}
            for _,_,_,p in found[:max_results]],len(found)

def inspect(source,copy):
    source=validate_level(source)
    copy=validate_level(copy)
    if source==copy or source in copy.parents or copy in source.parents:
        raise ValueError("Source and staged copy must be independent")
    check_official_source(source)
    if (copy/"turnbound-world.marker").exists():
        raise ValueError("Activated TURNBOUND save rejected")
    seven=verify_seven(copy)
    if not seven.get("seven_stage_offline_experiment_verified"):
        raise ValueError("Expected pinned seven-stage official map copy")
    v1,v2=World(source),World(copy)
    observations=[]
    for name,position in SITES:
        a=footprint(v1,position)
        b=footprint(v2,position)
        if a!=b:
            raise ValueError("Original geometry differs from isolated-copy geometry")
        observations.append({"site":name,"ci_only_target":[*position],**b,
                             "geometry_matches_official_source":True})
    candidates,count=conservative_candidates(v2,SITES[0][1])
    return {
        "status":"OFFICIAL_DREHMAL_ORIGINAL_AND_SEVEN_STAGE_GEOMETRY_MATCH",
        "official_original_sha_verified":True,
        "seven_stage_original_geometry_preserved_at_three_sites":True,
        "footprints":observations,
        "nearby_conservative_candidates":candidates,
        "nearby_conservative_candidate_count":count,
        "actual_collision_box_and_block_shapes_tested":False,
        "actual_teleport_or_playtest_performed":False,
        "candidate_is_game_start_approved":False,
        "official_map_separation_complete":False,
        "original_world_modified":False,
    }

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--source",required=True,type=Path)
    p.add_argument("--world-copy",required=True,type=Path)
    p.add_argument("--output",required=True,type=Path)
    args=p.parse_args()
    output=args.output.resolve()
    source=args.source.resolve(strict=True)
    copy=args.world_copy.resolve(strict=True)
    if output==source or source in output.parents or output==copy or copy in output.parents:
        p.error("Never write reports into original or copied world")
    report=inspect(source,copy)
    output.parent.mkdir(exist_ok=True,parents=True)
    output.write_text(json.dumps(report,indent=2,ensure_ascii=True)+"\n")
    print(json.dumps({k:report[k] for k in (
        "status","seven_stage_original_geometry_preserved_at_three_sites",
        "nearby_conservative_candidate_count",
        "candidate_is_game_start_approved","official_map_separation_complete")}))

if __name__=="__main__":
    try:main()
    except (OSError,ValueError,TypeError,KeyError) as error:
        print("Drehmal official geometry audit BLOCKED: "+str(error),file=sys.stderr)
        sys.exit(2)

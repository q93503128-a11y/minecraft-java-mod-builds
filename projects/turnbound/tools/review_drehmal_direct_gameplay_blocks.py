#!/usr/bin/env python3
"""Inspect original Drehmal direct reward/combat command blocks WITHOUT changing world.

For approved-source review only. Emits derived item/effect/entity/advancement IDs,
source hashes, dimensions, coordinates and neighboring command operation TYPES.
Does NOT emit original commands, NBT, selector predicates, player data or map files.
Everything is a review candidate; no automatic removal is approved.
"""
from __future__ import annotations

import argparse
import collections
import hashlib
import json
import re
import sys
from pathlib import Path

from audit_drehmal_auxiliary_sources import command_shape, region_dimension
from scan_drehmal_command_blocks import scan_region

LEVEL_SHA = "44e9b625f9dd8616472bbae98b3481a2fb34a22a49a98faf913d3b7148a935d1"
AUX_SHA = "9092055106167e69e55091151acfa2af86ed468b1aa171dcb2dfe07ecd1897ef"
TARGET_ROOTS = ("give", "advancement", "effect", "kill", "summon")
EXPECTED = {"give":37, "advancement":3, "effect":127, "kill":20, "summon":81}
RESOURCE = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")
RESOURCE_HEAD = re.compile(r"^([a-z0-9_.-]+:[a-z0-9_./-]+)(?=[{\[]|$)", re.I)
SELECTORS = re.compile(r"^@([apser])(?:\[|$)", re.I)

def digest(data):
    return hashlib.sha256(data).hexdigest()

def file_digest(path):
    h=hashlib.sha256()
    with path.open("rb") as handle:
        while part := handle.read(4*1024*1024):
            h.update(part)
    return h.hexdigest()

def checked_metadata(path):
    raw=Path(path).read_bytes()
    if digest(raw)!=AUX_SHA:
        raise ValueError("Verified original 2.2.2f auxiliary artifact has changed")
    report=json.loads(raw)
    if (report.get("level_sha256")!=LEVEL_SHA
            or report.get("world_bytes_modified") is not False
            or report["command_blocks"]["scanned_command_blocks"]!=2232):
        raise ValueError("Official source or command inventory changed")
    return report

def selector_class(token):
    token=token.lstrip("/")
    match=SELECTORS.match(token)
    if match:
        return {
            "a":"ALL_PLAYERS","p":"NEAREST_PLAYER","s":"EXECUTOR",
            "e":"ENTITY_QUERY","r":"RANDOM_PLAYER"
        }[match.group(1).lower()]
    return "LITERAL_OR_UNPARSED_REVIEW" if token else "MISSING_TARGET"

def item_id(token):
    match=RESOURCE_HEAD.match(token)
    return match.group(1).lower() if match else None

def parse_direct_effect(command,root):
    # Only top-level verbs are parsed. Execution predicates and NBT are *never*
    # exported because those can encode extensive original game logic.
    parts=command.strip().lstrip("/").split()
    if not parts or parts[0].lower()!=root:
        raise ValueError("Unexpected source command grammar")
    meta={"target_class":None,"resource_id":None,
          "parameter_shape":"UNPARSED","review":"PRESERVE_UNTIL_GAMEPLAY_AND_MECHANISM_REVIEW"}
    if root=="give":
        meta["target_class"]=selector_class(parts[1]) if len(parts)>1 else "MISSING_TARGET"
        meta["resource_id"]=item_id(parts[2]) if len(parts)>2 else None
        meta["parameter_shape"]="DIRECT_ITEM_REWARD"
    elif root=="advancement":
        meta["advancement_action"]=parts[1].lower() if len(parts)>1 else "MISSING"
        meta["target_class"]=selector_class(parts[2]) if len(parts)>2 else "MISSING_TARGET"
        scope=parts[3].lower() if len(parts)>3 else "MISSING"
        meta["parameter_shape"]="ADVANCEMENT_"+(scope.upper() if scope in ("everything","only","from","through","until") else "REVIEW")
        meta["resource_id"]=item_id(parts[4]) if scope!="everything" and len(parts)>4 else None
    elif root=="effect":
        first=parts[1].lower() if len(parts)>1 else ""
        if first in ("give","clear"):
            meta["effect_action"]=first.upper()
            meta["target_class"]=selector_class(parts[2]) if len(parts)>2 else "MISSING_TARGET"
            meta["resource_id"]=item_id(parts[3]) if len(parts)>3 else None
        else:
            meta["effect_action"]="LEGACY_OR_UNPARSED"
            meta["target_class"]=selector_class(parts[1]) if len(parts)>1 else "MISSING_TARGET"
        meta["parameter_shape"]="POTION_EFFECT"
    elif root=="kill":
        meta["target_class"]=selector_class(parts[1]) if len(parts)>1 else "DEFAULT_EXECUTOR"
        meta["parameter_shape"]="ENTITY_REMOVAL"
    elif root=="summon":
        meta["resource_id"]=item_id(parts[1]) if len(parts)>1 else None
        meta["parameter_shape"]="ENTITY_SPAWN"
    return meta

def key(block):
    coords=block.get("coordinates")
    return (block["region_file"],block["chunk_slot"],
            tuple(coords) if coords is not None else None,
            block["command_actor"],block["command_sha256"])

def source_key(block,world):
    path=Path(block["file"]).resolve()
    if world not in path.parents:
        raise ValueError("Original region file is outside verified source world")
    relative=path.relative_to(world).as_posix()
    coords=[block.get("x"),block.get("y"),block.get("z")]
    if any(type(v) not in (int,float) for v in coords):
        coords=None
    return (relative,block["chunk_slot"],tuple(coords) if coords is not None else None,
            block["id"],digest(block["command"].encode("utf-8")))

def scan_and_verify(world,report):
    """Reverify all command actors in 72 targeted regions against pinned full audit.

    The earlier official scan covered 2,232 actors; rescanning the entire 5 GB
    source world a second time takes 10+ minutes. All candidate command bytes
    and surrounding actors in their *own regions* are verified here. Other
    region command bytes remain supported only by the pinned first audit.
    """
    world=Path(world).resolve(strict=True)
    level=world/"level.dat"
    if not level.is_file() or level.is_symlink() or file_digest(level)!=LEVEL_SHA:
        raise ValueError("Not official Drehmal 2.2.2f original world")
    metadata=report["command_blocks"]
    expected_rows=metadata["all_block_metadata"]
    if len(expected_rows)!=2232 or metadata["scanned_command_blocks"]!=2232:
        raise ValueError("Incomplete official full original audit")
    selected=[x for x in expected_rows if x["root_operation"] in TARGET_ROOTS]
    if len(selected)!=sum(EXPECTED.values()):
        raise ValueError("Incomplete original direct gameplay target inventory")
    paths={x["region_file"] for x in selected}
    if len(paths)!=72:
        raise ValueError("Original review target region set changed")
    grouped=collections.defaultdict(list)
    for row in expected_rows:
        if row["region_file"] in paths:
            grouped[row["region_file"]].append(row)
    actors=[]
    warnings=[]
    for relative in sorted(paths):
        rel=Path(relative)
        if rel.is_absolute() or ".." in rel.parts or not rel.name.endswith(".mca"):
            raise ValueError("Unsafe source region path")
        src=world/rel
        if not src.is_file() or src.is_symlink():
            raise ValueError("Required original region absent/symlinked")
        local,problems=scan_region(src)
        if problems:
            warnings.extend(f"{relative}: {p}" for p in problems)
            raise ValueError("Selected source region contains incomplete command-block scan coverage")
        found=collections.Counter(source_key(x,world) for x in local)
        expected=collections.Counter(key(x) for x in grouped[relative])
        if found!=expected or len(found)!=len(local):
            raise ValueError("Original target-region command actor/hash inventory differs")
        actors.extend(local)
    # No claim of re-auditing every non-target original region; they remain
    # covered by the first official SHA-pinned 2,232-actor inventory.
    return {"command_blocks":actors,"warnings":warnings,
            "target_region_count":len(paths),"verified_region_command_actor_count":len(actors)}

def summarize(world,source,metadata):
    coords_by_dimension=collections.defaultdict(list)
    for block in metadata["command_blocks"]["all_block_metadata"]:
        if isinstance(block.get("coordinates"),list):
            coords_by_dimension[block["dimension"]].append(block)
    records=[]
    roots=collections.Counter()
    for block in source["command_blocks"]:
        raw=block["command"]
        parsed=command_shape(raw)
        root=parsed["root"]
        if root not in TARGET_ROOTS:
            continue
        roots[root]+=1
        rowkey=source_key(block,world)
        relative,slot,coord,actor,hash_value=rowkey
        dimension=region_dimension(relative)
        if dimension=="UNKNOWN":
            raise ValueError("Unrecognized original command dimension")
        near=[]
        for other in coords_by_dimension[dimension]:
            pos=other.get("coordinates")
            if pos is None or coord is None:
                continue
            if (other["region_file"],other["chunk_slot"],tuple(pos),
                    other["command_actor"],other["command_sha256"])==rowkey:
                continue
            distance=sum(abs(a-b) for a,b in zip(coord,pos))
            if distance<=4:
                near.append((distance,other["root_operation"],other["effective_operation"]))
        near.sort()
        info=parse_direct_effect(raw,root)
        records.append({
            "operation":root,"dimension":dimension,"region_file":relative,
            "chunk_slot":slot,"coordinates":list(coord) if coord is not None else None,
            "physical_coordinate_review":"KNOWN" if coord is not None else "MISSING_NBT_COORDS_NO_PATCH",
            "command_sha256":hash_value,"actor":actor,
            "neighboring_command_blocks_within_4":len(near),
            "neighbor_root_operations":dict(sorted(collections.Counter(x[1] for x in near).items())),
            "neighbor_effective_operations":dict(sorted(collections.Counter(x[2] for x in near).items())),
            **info,
            "approved_for_removal":False
        })
    if dict(roots)!=EXPECTED:
        raise ValueError(f"Official gameplay direct opcode counts drifted: {dict(roots)}")
    records.sort(key=lambda x:(x["operation"],x["dimension"],x["region_file"],
                               str(x["coordinates"]),x["command_sha256"]))
    return {
        "status":"READ_ONLY_DIRECT_GAMEPLAY_MECHANISM_TRIAGE",
        "source_level_sha256":LEVEL_SHA,"source_auxiliary_sha256":AUX_SHA,
        "official_full_audit_command_blocks":2232,
        "reverified_target_regions":source["target_region_count"],
        "reverified_command_actors_in_target_regions":source["verified_region_command_actor_count"],
        "original_full_audit_region_warnings":metadata["command_blocks"]["region_warning_count"],
        "selected_region_scan_warnings":len(source["warnings"]),
        "counts_by_direct_operation":dict(sorted(roots.items())),
        "distinct_region_files":len({r["region_file"] for r in records}),
        "candidates_missing_physical_coordinates":sum(x["coordinates"] is None for x in records),
        "physical_candidates":records,
        "world_modified":False,"original_code_reused":False,
        "automatic_suppression_allowed":False,
        "limitations":[
            "Item/entity/effect IDs alone cannot prove structural independence or safe removal.",
            "Nearest-command-block adjacency is a spatial hint, not a redstone/conditional chain topology.",
            "Raw summon NBT, give item NBT, selector predicates and command text are deliberately not exported.",
            "Function indirect invocation, advancement triggers, entity behavior and unreadable region chunks remain.",
            "No candidate is authorized for removal solely by opcode; preserve puzzle and facility mechanisms.",
            "Six summon records lack physical NBT coordinates and cannot be safely patched by location.",
            "Only the 72 candidate-bearing regions are re-scanned. Full 2,232-block catalog is pinned to previous official verified audit."
        ]
    }

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--world",required=True,type=Path)
    p.add_argument("--aux",required=True,type=Path)
    p.add_argument("--output",required=True,type=Path)
    a=p.parse_args()
    world=a.world.resolve(strict=True)
    output=a.output.resolve()
    if output==world or world in output.parents:
        p.error("Report must be outside original world")
    original=checked_metadata(a.aux)
    result=summarize(world,scan_and_verify(world,original),original)
    output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(result,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    print(json.dumps({
        "official_command_blocks":result["official_full_audit_command_blocks"],
        "reverified_target_regions":result["reverified_target_regions"],
        "missing_coordinates":result["candidates_missing_physical_coordinates"],
        "gameplay_direct_candidates":len(result["physical_candidates"]),
        "operations":result["counts_by_direct_operation"],
        "world_modified":False,"removals_authorized":False
    },ensure_ascii=False))

if __name__=="__main__":
    try:main()
    except (ValueError,OSError,KeyError,TypeError) as error:
        print("Original direct gameplay command review REFUSED: "+str(error),file=sys.stderr)
        sys.exit(2)

#!/usr/bin/env python3
"""Read-only static Foundry door / six-wave entrypoint audit, official SHA pinned.

Reports IDs, literal coordinates and effect categories, not source commands,
NBT, player data, map assets, or a claim of runtime causal execution.
"""
from __future__ import annotations
import argparse
import collections
import json
import re
import sys
import zipfile
from pathlib import Path

import inspect_drehmal_teleport_footprints as official
from audit_drehmal_starter_mechanisms import World

SOURCE_PACK="datapacks/hi_drehmal.zip"
GATE_HINT=(-3276,157,1500)
LEVERS=(
 (-3128,149,1490),
 (-3035,122,1185),
 (-3394,146,1081),
)
FUNC_RX=re.compile(r"(?<![a-z_])function\s+([a-z0-9_.-]+:[a-z0-9_./-]+)")
INT=r"(-?\d+)"
SETBLOCK_RX=re.compile(r"\bsetblock\s+"+r"\s+".join([INT]*3)+r"\s+([a-z0-9_:]+)")
FILL_RX=re.compile(r"\bfill\s+"+r"\s+".join([INT]*6)+r"\s+([a-z0-9_:]+)")
SCHEDULE_RX=re.compile(r"\bschedule\s+function\s+([a-z0-9_.-]+:[a-z0-9_./-]+)")
EXEC_RX=re.compile(r"\bexecute\s+.*?\b(run|if|unless)\b")
COMMANDS=frozenset((
 "minecraft:command_block","minecraft:chain_command_block",
 "minecraft:repeating_command_block"
))


def source_id(name):
    parts=name.split("/")
    if len(parts)<4 or parts[0]!="data":
        raise ValueError("Invalid datapack member path")
    namespace=parts[1]
    if parts[2] not in ("functions","advancements"):
        raise ValueError("Unrecognized datapack member type")
    return namespace+":"+("/".join(parts[3:]).rsplit(".",1)[0])


def edit_coordinates(function_id,line):
    """Only absolute literal coordinates. Never infer relative/execution context."""
    result=[]
    for m in SETBLOCK_RX.finditer(line):
        xyz=tuple(int(v) for v in m.groups()[:3])
        result.append({"function_id":function_id,"operation":"setblock",
                       "absolute_literal_coordinates":[*xyz],
                       "new_block_id":m.group(4),
                       "execute_position_context_unresolved":"execute" in line[:m.start()]})
    for m in FILL_RX.finditer(line):
        vals=tuple(int(v) for v in m.groups()[:6])
        x1,y1,z1,x2,y2,z2=vals
        result.append({"function_id":function_id,"operation":"fill",
                       "absolute_literal_min":[min(x1,x2),min(y1,y2),min(z1,z2)],
                       "absolute_literal_max":[max(x1,x2),max(y1,y2),max(z1,z2)],
                       "new_block_id":m.group(7),
                       "execute_position_context_unresolved":"execute" in line[:m.start()]})
    return result


def near_foundry(row):
    x,y,z=GATE_HINT
    if row["operation"]=="setblock":
        a=b=row["absolute_literal_coordinates"]
    else:
        a,b=row["absolute_literal_min"],row["absolute_literal_max"]
    return all(a[i]<=([x,y,z][i]+(500 if i!=1 else 100)) and
               b[i]>=([x,y,z][i]-(500 if i!=1 else 100))
               for i in range(3))


def inspect_datapack(pack):
    with zipfile.ZipFile(pack) as archive:
        names=archive.namelist()
        if len(names)!=len(set(names)):raise ValueError("Duplicate original datapack member")
        functions={}
        advancement=[]
        callin=collections.defaultdict(set)
        edits=[]
        effects=collections.Counter()
        for name in names:
            if not name.startswith("data/"):continue
            if name.endswith(".mcfunction") and "/functions/" in name:
                fid=source_id(name)
                if not (fid.startswith("foundry:") or "valhalla" in fid.lower()
                        or "foundry" in fid.lower()):
                    continue
                raw=archive.read(name)
                if len(raw)>2_000_000:raise ValueError("Unsafe original source length")
                lines=raw.decode("utf-8").splitlines()
                types=collections.Counter()
                out=set()
                for line in lines:
                    line=line.strip().lstrip("/")
                    if not line or line.startswith("#"):continue
                    op=line.split(maxsplit=1)[0].lower()
                    types[op]+=1
                    for callee in FUNC_RX.findall(line):
                        callin[callee].add(fid)
                        out.add(callee)
                    for callee in SCHEDULE_RX.findall(line):
                        callin[callee].add(fid)
                        out.add(callee)
                    for effect in ("summon ","scoreboard ","fill ","setblock ",
                                   "clone ","advancement ","schedule "):
                        if effect in line.lower():effects[effect.rstrip()]+=1
                    edits+=edit_coordinates(fid,line)
                functions[fid]={"command_root_counts":dict(types),"callees":sorted(out)}
            elif name.endswith(".json") and "/advancements/" in name:
                # We read all original advancement reward IDs, but export only foundry-related.
                try:doc=json.loads(archive.read(name))
                except (ValueError,UnicodeDecodeError):continue
                reward=doc.get("rewards",{}) if isinstance(doc,dict) else {}
                reward_func=reward.get("function") if isinstance(reward,dict) else None
                if not ("foundry" in name.lower() or "foundry" in str(reward_func).lower()):
                    continue
                trig=sorted({v.get("trigger","UNKNOWN") for v in doc.get("criteria",{}).values()
                             if isinstance(v,dict)})
                advancement.append({"advancement_id":source_id(name),
                   "reward_function_id":reward_func,
                   "criterion_trigger_types":trig})
                if isinstance(reward_func,str):callin[reward_func].add("advancement:"+source_id(name))
    candidates=[]
    for row in edits:
        if near_foundry(row):candidates.append(row)
    roots=("foundry:arena_start","foundry:arena_victory",
           "foundry:wave_1","foundry:wave_2","foundry:wave_3",
           "foundry:wave_4","foundry:wave_5","foundry:wave_6",
           "foundry:wave_advance")
    return {
        "foundry_named_function_count":len(functions),
        "foundry_advancement_count":len(advancement),
        "foundry_advancement_reward_entries":sorted(advancement,key=lambda x:x["advancement_id"])[:120],
        "foundry_command_effect_site_counts":dict(sorted(effects.items())),
        "literal_setblock_or_fill_sites":len(edits),
        "foundry_area_literal_edit_candidates":candidates[:120],
        "foundry_area_literal_edit_candidate_count":len(candidates),
        "named_wave_entry_reverse_callers":{
            fid:sorted(callin.get(fid,[]))[:40] for fid in roots
        },
        "named_wave_entry_callers_total":{
            fid:len(callin.get(fid,[])) for fid in roots
        },
        "named_wave_entrypoint_exists":{fid:fid in functions for fid in roots},
        "wave_path_causally_verified":False,
        "original_six_wave_battle_disabled":False,
        "relative_or_macro_coordinate_edits_exhaustively_parsed":False,
        "all_data_pack_namespaces_scanned_for_calls":False,
    }


def nearby_lever_controls(world):
    view=World(world)
    out=[]
    for label,center in enumerate(LEVERS,1):
        count=0
        calls=set()
        coords=[]
        for x in range(center[0]-4,center[0]+5):
            for y in range(center[1]-4,center[1]+5):
                for z in range(center[2]-4,center[2]+5):
                    pos=(x,y,z)
                    state=view.state(pos)
                    if state["id"] not in COMMANDS:continue
                    actor=view.actor(pos)
                    raw=actor.get("Command")
                    if not isinstance(raw,str):raise ValueError("Invalid command block NBT")
                    count+=1
                    coords.append([*pos])
                    calls.update(FUNC_RX.findall(raw))
        out.append({"site":"foundry_lever_"+str(label),"command_block_count_radius4":count,
                    "command_block_positions":coords[:20],
                    "function_reference_ids":sorted(calls)[:32],
                    "original_command_text_not_exported":True})
    return out



# Follow-up: literal clone destinations and a narrow all-namespaces caller search.
# We do not infer command effects when coordinates are relative or macro-expanded.
CLONE_RX=re.compile(r"\bclone\s+"+r"\s+".join([INT]*9))
FOCUSED_IDS=(
 "foundry:levers/1_check","foundry:levers/2_check",
 "foundry:levers/3_check","foundry:open/1","foundry:open/2",
 "foundry:open/3","foundry:arena_start","foundry:wave_logic",
 "foundry:wave_1","foundry:arena_victory","foundry:reset_door",
)

def clone_coordinates(function_id,line):
    out=[]
    for match in CLONE_RX.finditer(line):
        coords=[int(v) for v in match.groups()]
        source_a=coords[:3]
        source_b=coords[3:6]
        destination=coords[6:9]
        dims=[abs(source_b[i]-source_a[i]) for i in range(3)]
        out.append({
          "function_id":function_id,"operation":"clone",
          "source_min":[min(source_a[i],source_b[i]) for i in range(3)],
          "source_max":[max(source_a[i],source_b[i]) for i in range(3)],
          "dest_min":destination,
          "dest_max":[destination[i]+dims[i] for i in range(3)],
          "execute_position_context_unresolved":"execute" in line[:match.start()],
        })
    return out

def focused_path_inventory(pack):
    reverse=collections.defaultdict(set)
    profiles={}
    clones=[]
    with zipfile.ZipFile(pack) as z:
        names=z.namelist()
        for name in names:
            if not name.startswith("data/") or not name.endswith(".mcfunction"):
                continue
            fid=source_id(name)
            data=z.read(name)
            if len(data)>2_000_000:
                raise ValueError("Too many bytes in original function")
            # Scan across all namespaces without writing original content to reports.
            if b"foundry:" not in data and fid not in FOCUSED_IDS:
                continue
            content=data.decode("utf-8")
            counts=collections.Counter()
            out=set()
            for line in content.splitlines():
                line=line.strip().lstrip("/")
                if not line or line.startswith("#"):continue
                root=line.split(maxsplit=1)[0]
                counts[root]+=1
                for callee in FUNC_RX.findall(line):
                    reverse[callee].add(fid)
                    out.add(callee)
                for callee in SCHEDULE_RX.findall(line):
                    reverse[callee].add(fid)
                    out.add(callee)
                clones.extend(clone_coordinates(fid,line))
            if fid in FOCUSED_IDS:
                profiles[fid]={"exists":True,"direct_root_operations":dict(sorted(counts.items())),
                               "literal_direct_callees":sorted(out)[:80]}
        for fid in FOCUSED_IDS:
            if fid not in profiles:
                profiles[fid]={"exists":False,"direct_root_operations":{},"literal_direct_callees":[]}
    return {
        "focused_original_functions":profiles,
        "all_namespaces_literal_callers_to_focused_functions":{
            fid:sorted(reverse.get(fid,[]))[:64] for fid in FOCUSED_IDS
        },
        "all_namespaces_literal_caller_count":{
            fid:len(reverse.get(fid,[])) for fid in FOCUSED_IDS
        },
        "literal_clone_operations_found":len(clones),
        "original_clone_coordinate_targets":clones[:100],
        "coordinate_relative_calls_exhaustively_parsed":False,
        "runtime_invocation_verified":False,
    }


def audit(world):
    world=Path(world).resolve(strict=True)
    if (world/"turnbound-world.marker").exists():
        raise ValueError("Player-facing or activated map prohibited")
    official.check_official_source(world)
    source=world/SOURCE_PACK
    if not source.is_file() or source.is_symlink():
        raise ValueError("Official source pack unavailable")
    result=inspect_datapack(source)
    result["focused_facing_execution_and_clones"]=focused_path_inventory(source)
    result.update({
        "status":"OFFICIAL_ORIGINAL_FOUNDRY_STATIC_MECHANISM_BOUNDARY",
        "nearby_lever_command_block_surfaces":nearby_lever_controls(world),
        "official_original_sha_verified":True,
        "source_world_modified":False,
        "original_code_exported":False,
        "door_location_or_traversal_confirmed":False,
        "game_map_separation_complete":False,
    })
    official.check_official_source(world)
    return result


def main():
    p=argparse.ArgumentParser()
    p.add_argument("--original",required=True,type=Path)
    p.add_argument("--output",required=True,type=Path)
    a=p.parse_args()
    world=a.original.resolve(strict=True)
    out=a.output.resolve()
    if out==world or world in out.parents:
        p.error("Never put derived report inside official map")
    result=audit(world)
    out.parent.mkdir(exist_ok=True,parents=True)
    out.write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({
        "status":result["status"],
        "nearby_gate_edit_candidates":result["foundry_area_literal_edit_candidate_count"],
        "named_functions":result["foundry_named_function_count"],
        "wave_path_causally_verified":False,
        "map_separation_complete":False,
    }))
if __name__=="__main__":
    try:main()
    except (ValueError,TypeError,KeyError,OSError,zipfile.BadZipFile) as err:
        print("Foundry original source mechanism review REFUSED: "+str(err),file=sys.stderr)
        sys.exit(2)

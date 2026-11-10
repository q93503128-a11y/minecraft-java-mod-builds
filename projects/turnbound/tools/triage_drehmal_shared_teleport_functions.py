#!/usr/bin/env python3
"""Read-only, SHA-locked original Drehmal datapack shared-teleport entrypoint triage.

The source map/datapack belongs to the original authors. Do not redistribute
their commands, files or reward JSON. Derived identifiers/counts/hashes only.
This STATIC candidate report is not proof of runtime execution or permission
to use any code in TURNBOUND.
"""
from __future__ import annotations
import argparse
import collections
import hashlib
import json
import re
import sys
import zipfile
from pathlib import Path, PurePosixPath

OFFICIAL_PACK_SHA="f074d2927ea2947f14fe0b2836332695087e5bbf2e7c3b08f94f2e4f36c6fc74"
OFFICIAL_LEVEL_SHA="44e9b625f9dd8616472bbae98b3481a2fb34a22a49a98faf913d3b7148a935d1"
FUNCTION=re.compile(r"^data/([a-z0-9_.-]+)/functions/([a-z0-9_./-]+)\.mcfunction$")
ADVANCEMENT=re.compile(r"^data/([a-z0-9_.-]+)/advancements/([a-z0-9_./-]+)\.json$")
# Numeric target observations are ONLY search hints; no code behavior inferred.
ORIGINAL_RELOCATION_HINTS={
    "reentry_26520":re.compile(r"(?<!\d)26520(?:\.5)?(?![\d.])"),
    "reentry_negative84":re.compile(r"(?<![\d.])-84(?:\.5)?(?![\d.])"),
    "test_26458":re.compile(r"(?<!\d)26458(?:\.5)?(?![\d.])"),
    "test_negative285":re.compile(r"(?<![\d.])-285(?:\.5)?(?![\d.])"),
}
COMMAND=re.compile(r"(?:^|\s)(?:tp|teleport|spreadplayers|spawnpoint|setworldspawn)\b",re.I)
DIRECT_TELEPORT=re.compile(r"(?:^|\s)(?:tp|teleport|spreadplayers)\b",re.I)
SPAWN=re.compile(r"(?:^|\s)(?:spawnpoint|setworldspawn)\b",re.I)
CALL=re.compile(r"(?:^|\s)function\s+(#?[a-z0-9_.-]+:[a-z0-9_./-]+)",re.I)
NAME_HINT=re.compile(r"(?:^|[/_:-])(spawn|start|intro|arrival|return|entry|begin|lobby|teleport|warp|initial|join|tutorial)(?:$|[/_:-])",re.I)
MAX_MEMBER=8*1024*1024
MAX_CANDIDATES=60
MAX_ADVANCEMENTS=60

def file_sha(file):
    with file.open("rb") as stream:
        digest=hashlib.sha256()
        while True:
            chunk=stream.read(1024*1024)
            if not chunk:return digest.hexdigest()
            digest.update(chunk)

def checked_zip(world,require_pinned=True):
    world=Path(world).resolve(strict=True)
    if not (world/"level.dat").is_file():
        raise ValueError("Missing source level.dat")
    if require_pinned and file_sha(world/"level.dat")!=OFFICIAL_LEVEL_SHA:
        raise ValueError("Source official world SHA mismatch")
    pack=world/"datapacks"/"hi_drehmal.zip"
    if pack.is_symlink() or not pack.is_file():
        raise ValueError("Expected original hi_drehmal.zip from verified source world")
    if require_pinned and file_sha(pack)!=OFFICIAL_PACK_SHA:
        raise ValueError("Original datapack SHA mismatch")
    return pack

def safety(info):
    p=PurePosixPath(info.filename)
    if p.is_absolute() or ".." in p.parts or "\\" in info.filename:
        raise ValueError("Unsafe datapack member")
    if info.file_size>MAX_MEMBER:
        raise ValueError("Unsafe oversized datapack member")
    return not info.is_dir()

def examine_function(name,raw):
    m=FUNCTION.fullmatch(name)
    if not m:raise ValueError("Not a datapack function")
    text=raw.decode("utf-8")
    lines=[line.strip() for line in text.splitlines()
           if line.strip() and not line.lstrip().startswith("#")]
    matched={n:False for n in ORIGINAL_RELOCATION_HINTS}
    tps=0;spawns=0;calls=set();lines_with_hints=0
    # Scan every executable line, avoid counting comments and raw content in output.
    for line in lines:
        if DIRECT_TELEPORT.search(line):tps+=1
        if SPAWN.search(line):spawns+=1
        calls.update(CALL.findall(line))
        hits=[n for n,regex in ORIGINAL_RELOCATION_HINTS.items() if regex.search(line)]
        for key in hits:matched[key]=True
        if hits:lines_with_hints+=1
    identifier=m.group(1)+":"+m.group(2)
    return {
        "function_id":identifier,
        "sha256":hashlib.sha256(raw).hexdigest(),
        "direct_teleport_lines":tps,
        "spawnpoint_lines":spawns,
        "keyword_name_hint":bool(NAME_HINT.search(identifier)),
        "location_numeric_hints":matched,
        "lines_with_location_hints":lines_with_hints,
        "calls":sorted(calls)
    }

def inspect_pack(pack):
    fns={};adv={};warnings=[]
    with zipfile.ZipFile(pack) as z:
        for info in z.infolist():
            if not safety(info):continue
            fname=info.filename
            func=FUNCTION.fullmatch(fname)
            advancement=ADVANCEMENT.fullmatch(fname)
            if not func and not advancement:continue
            raw=z.read(info)
            if func:
                rec=examine_function(fname,raw)
                key=rec["function_id"]
                if key in fns:raise ValueError("Duplicate function ID in original pack")
                fns[key]=rec
            elif advancement:
                try:data=json.loads(raw)
                except (ValueError,UnicodeDecodeError) as e:
                    raise ValueError("Invalid original advancement JSON") from e
                if not isinstance(data,dict):raise ValueError("Not a JSON object")
                identifier=advancement.group(1)+":"+advancement.group(2)
                reward=(data.get("rewards") or {}).get("function")
                criteria=data.get("criteria",{})
                if reward is not None and not isinstance(reward,str):
                    raise ValueError("Unexpected advancement reward type")
                if not isinstance(criteria,dict):
                    raise ValueError("Unexpected advancement criteria")
                triggers=sorted({
                    d.get("trigger") for d in criteria.values()
                    if isinstance(d,dict) and isinstance(d.get("trigger"),str)
                })
                adv[identifier]={
                   "advancement_id":identifier,
                   "reward_function_id":reward,
                   "triggers":triggers,
                   "name_hint":bool(NAME_HINT.search(identifier))
                }
    return fns,adv

def candidate_report(fns,adv):
    rewards=collections.defaultdict(list)
    for a in adv.values():
        if a["reward_function_id"]:
            rewards[a["reward_function_id"]].append(a["advancement_id"])
    callers=collections.defaultdict(set)
    for fid,v in fns.items():
        for called in v["calls"]:callers[called].add(fid)
    ranked=[]
    for fid,entry in fns.items():
        hints=entry["location_numeric_hints"]
        both_target=hints["reentry_26520"] and hints["reentry_negative84"]
        both_source=hints["test_26458"] and hints["test_negative285"]
        near=any(hints.values())
        reward_ids=rewards.get(fid,[])
        name=entry["keyword_name_hint"]
        if not (entry["direct_teleport_lines"] or entry["spawnpoint_lines"] or near or
                (reward_ids and name)):continue
        score=(80 if both_target else 0)+(35 if both_source else 0)+(
            20 if near else 0)+(10 if name else 0)+(
            6 if reward_ids else 0)+(3 if entry["direct_teleport_lines"] else 0)+(
            2 if entry["spawnpoint_lines"] else 0)
        ranked.append((score,fid,{
            "function_id":fid,
            "sha256":entry["sha256"],
            "candidate_score_for_investigation_only":score,
            "direct_teleport_lines":entry["direct_teleport_lines"],
            "spawnpoint_lines":entry["spawnpoint_lines"],
            "name_suggests_entry_or_warp":name,
            "both_observed_destination_numbers_somewhere_in_function":both_target,
            "both_original_ci_location_numbers_somewhere_in_function":both_source,
            "has_any_observed_numeric_location_hint":near,
            "reward_advancement_references":sorted(reward_ids)[:8],
            "function_caller_count":len(callers.get(fid,())),
            "function_call_count":len(entry["calls"]),
            "execution_proven":False
        }))
    ranked.sort(key=lambda item:(-item[0],item[1]))
    selected=[a[2] for a in ranked[:MAX_CANDIDATES]]
    award_candidates=[]
    for item in adv.values():
        target=item["reward_function_id"]
        if not target:continue
        triggers=item["triggers"]
        if ("minecraft:location" in triggers or "minecraft:tick" in triggers
            or item["name_hint"] or bool(target and NAME_HINT.search(target))):
            award_candidates.append({
                "advancement_id":item["advancement_id"],
                "reward_function_id":target,
                "trigger_types":triggers,
                "reward_function_defined_in_this_pack":target in fns,
                "called_function_is_direct_teleport":fns.get(target,{}).get("direct_teleport_lines",0)>0,
                "execution_proven":False,
            })
    award_candidates.sort(key=lambda e:(
        0 if e["called_function_is_direct_teleport"] else 1,
        0 if e["trigger_types"] and ("minecraft:location" in e["trigger_types"] or "minecraft:tick" in e["trigger_types"]) else 1,
        e["advancement_id"]))
    return {
        "status":"ORIGINAL_DREHMAL_DATAPACK_STATIC_SHARED_TELEPORT_TRIAGE",
        "source":"ORIGINAL_OFFICIAL_DREHMAL_V2_2_2F_DATAPACK_SHA_LOCKED",
        "function_count":len(fns),
        "advancement_count":len(adv),
        "functions_with_direct_teleport_lines":sum(x["direct_teleport_lines"]>0 for x in fns.values()),
        "functions_with_spawnpoint_lines":sum(x["spawnpoint_lines"]>0 for x in fns.values()),
        "functions_with_both_target_number_hints":sum(
            x["location_numeric_hints"]["reentry_26520"] and
            x["location_numeric_hints"]["reentry_negative84"] for x in fns.values()),
        "candidate_total":len(ranked),
        "candidate_function_records":selected,
        "advancement_reward_reference_total":sum(a["reward_function_id"] is not None for a in adv.values()),
        "entry_like_advancement_reward_total":len(award_candidates),
        "entry_like_advancement_reward_records":award_candidates[:MAX_ADVANCEMENTS],
        "raw_original_commands_or_advancement_json_published":False,
        "source_files_modified":False,
        "specific_function_causing_relocation_proven":False,
        "map_separation_complete":False,
    }

def main():
    cli=argparse.ArgumentParser()
    cli.add_argument("--original",type=Path,required=True)
    cli.add_argument("--output",type=Path,required=True)
    a=cli.parse_args()
    original=a.original.resolve(strict=True)
    output=a.output.resolve()
    if output==original or original in output.parents:
        cli.error("Never write derived report inside original map")
    pack=checked_zip(original)
    fns,adv=inspect_pack(pack)
    d=candidate_report(fns,adv)
    output.parent.mkdir(exist_ok=True,parents=True)
    output.write_text(json.dumps(d,indent=2,ensure_ascii=True)+"\n")
    print(json.dumps({k:d[k] for k in (
        "status","function_count","advancement_count",
        "functions_with_direct_teleport_lines","functions_with_both_target_number_hints",
        "specific_function_causing_relocation_proven")}))

if __name__=="__main__":
    try:main()
    except (OSError,ValueError,KeyError,UnicodeDecodeError,zipfile.BadZipFile) as e:
        print("Original datapack static teleport triage BLOCKED: "+str(e),file=sys.stderr)
        sys.exit(2)

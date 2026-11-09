#!/usr/bin/env python3
"""Read-only auxiliary Drehmal execution audit for verified v2.2.2f source world.

Never exports original command strings, JSON bodies, scripts, or region bytes.
This is NOT an automatic removal list or authorization to reuse Drehmal code.
"""
from __future__ import annotations
import argparse
import collections
import hashlib
import json
import re
import sys
import zipfile
from pathlib import Path
from scan_drehmal_command_blocks import scan_world

LEVEL_SHA="44e9b625f9dd8616472bbae98b3481a2fb34a22a49a98faf913d3b7148a935d1"
PACKS=("bern.zip","emis.zip","hi_drehmal.zip","mael.zip")
FUNC=re.compile(r"(?:^|\s)(?:function|schedule\s+function)\s+(#?[a-z0-9_.-]+:[a-z0-9_./-]+)",re.I)
SCHEDULE=re.compile(r"(?:^|\s)schedule\s+function\s+(#?[a-z0-9_.-]+:[a-z0-9_./-]+)",re.I)
ADV=re.compile(r"^data/([a-z0-9_.-]+)/advancements/([a-z0-9_./-]+)\.json$")
FUNCTION_PATH=re.compile(r"^data/([a-z0-9_.-]+)/functions/([a-z0-9_./-]+)\.mcfunction$")
JSON_PATHS=("advancements","predicates","loot_tables","recipes","item_modifiers","tags/functions",
            "worldgen","dimension","dimension_type")
KNOWN_OPERATIONS={"function","execute","scoreboard","tp","teleport","summon","kill","give","clear",
                  "loot","item","advancement","effect","attribute","experience","xp","data","tag",
                  "setblock","fill","clone","place","fillbiome","gamerule","schedule","trigger",
                  "title","tellraw","playsound","stopsound","particle","bossbar","ride","spreadplayers",
                  "setworldspawn","spawnpoint","time","weather","difficulty","say","team","forceload"}

def sha(raw):
    return hashlib.sha256(raw).hexdigest()

def command_shape(raw):
    command=raw.strip().lstrip("/")
    if not command:
        return {"root":"EMPTY","effective":"EMPTY","function_refs":[]}
    root=command.split(None,1)[0].lower()
    effective=root
    if root=="execute":
        # The final run subcommand is useful for triage but nested run/conditional
        # grammar must be reviewed manually. No safe-removal inference is made.
        after=re.split(r"\brun\s+",command,flags=re.I)
        if len(after)>1 and after[-1].strip():
            effective=after[-1].strip().split(None,1)[0].lower()
        else:
            effective="EXECUTE_WITHOUT_LITERAL_RUN"
    elif root=="schedule":
        effective="schedule"
    if effective not in KNOWN_OPERATIONS and effective not in ("EMPTY","EXECUTE_WITHOUT_LITERAL_RUN"):
        effective="UNRECOGNIZED_"+effective[:32]
    refs=sorted(set(FUNC.findall(command)))
    return {"root":root,"effective":effective,"function_refs":refs}

def region_dimension(rel):
    p=Path(rel).parts
    if p and p[0]=="DIM-1":return "minecraft:the_nether"
    if p and p[0]=="DIM1":return "minecraft:the_end"
    if len(p)>=3 and p[0]=="dimensions":return p[1]+":"+p[2]
    if p and p[0] in ("region","entities"):return "minecraft:overworld"
    return "UNKNOWN"

def command_inventory(raw,world):
    operations=collections.Counter()
    effective=collections.Counter()
    references=collections.Counter()
    by_dimension=collections.Counter()
    samples=[]
    root=Path(world).resolve()
    for block in raw["command_blocks"]:
        file=Path(block["file"]).resolve()
        if root not in file.parents:
            raise ValueError("Unsafe source region path")
        rel=file.relative_to(root).as_posix()
        data=command_shape(block["command"])
        operations[data["root"]]+=1
        effective[data["effective"]]+=1
        references.update(data["function_refs"])
        dim=region_dimension(rel)
        by_dimension[dim]+=1
        coord=[block.get("x"),block.get("y"),block.get("z")]
        if any(not isinstance(v,(int,float)) or isinstance(v,bool) for v in coord):
            coord=None
        samples.append({
            "dimension":dim,"region_file":rel,"chunk_slot":block["chunk_slot"],
            "coordinates":coord,"command_actor":block["id"],"command_sha256":sha(block["command"].encode("utf-8")),
            "root_operation":data["root"],"effective_operation":data["effective"],
            "literal_function_ids":data["function_refs"],"classification":"MANUAL_REVIEW_NO_REMOVAL"
        })
    return {
        "scanned_command_blocks":len(raw["command_blocks"]),
        "scanned_region_files":raw["files"],
        "region_warning_count":len(raw["warnings"]),
        "region_warning_types":dict(collections.Counter(
            "short_header" if "missing location header" in w else
            "external_chunk" if "External .mcc" in w else "other" for w in raw["warnings"]
        )),
        "by_dimension":dict(sorted(by_dimension.items())),
        "root_operation_counts":dict(sorted(operations.items())),
        "effective_operation_counts":dict(sorted(effective.items())),
        "matched_function_reference_counts":dict(sorted(references.items())),
        "all_block_metadata":samples,
    }

def pack_inventory(world):
    root=Path(world)/"datapacks"
    if not root.is_dir():
        raise ValueError("Missing original datapacks")
    names=tuple(sorted(p.name for p in root.iterdir() if p.name.endswith(".zip")))
    if names!=tuple(sorted(PACKS)):
        raise ValueError("Unexpected official datapack set")
    classes=collections.Counter()
    namespaces=collections.Counter()
    trigger_counts=collections.Counter()
    schedule_edges=[]
    advancement_records=[]
    parse_warnings=[]
    for packname in PACKS:
        with zipfile.ZipFile(root/packname) as archive:
            for info in archive.infolist():
                name=info.filename
                if name.endswith(".mcfunction") and FUNCTION_PATH.fullmatch(name):
                    body=archive.read(info)
                    if len(body)>8*1024*1024:
                        raise ValueError("Excessively large function source")
                    text=body.decode("utf-8",errors="replace")
                    paths=SCHEDULE.findall(text)
                    if paths:
                        namespace,path=FUNCTION_PATH.fullmatch(name).groups()
                        schedule_edges.append({"function_id":namespace+":"+path,
                                               "function_source_sha256":sha(body),
                                               "scheduled_target_ids":sorted(set(paths)),
                                               "literal_schedule_count":len(paths)})
                    continue
                if not name.endswith(".json") or not name.startswith("data/"):
                    continue
                parts=name.split("/")
                if len(parts)<4 or parts[2] not in JSON_PATHS:
                    continue
                kind=parts[2]
                classes[kind]+=1
                namespaces[parts[1]]+=1
                if kind!="advancements":
                    continue
                try:
                    data=json.loads(archive.read(info))
                except (ValueError,UnicodeDecodeError):
                    parse_warnings.append({"pack":packname,"file":name,"status":"BAD_JSON"})
                    continue
                if not isinstance(data,dict):
                    parse_warnings.append({"pack":packname,"file":name,"status":"NOT_AN_OBJECT"})
                    continue
                criteria=data.get("criteria",{})
                rewards=data.get("rewards",{})
                if not isinstance(criteria,dict): criteria={}
                if not isinstance(rewards,dict): rewards={}
                callback=rewards.get("function")
                for criterion in criteria.values():
                    if isinstance(criterion,dict):
                        trigger_counts[criterion.get("trigger","UNSPECIFIED")]+=1
                if callback or rewards:
                    advancement_records.append({
                        "file":name,"pack":packname,"file_sha256":sha(archive.read(info)),
                        "reward_function_id":callback if isinstance(callback,str) else None,
                        "reward_field_names":sorted(rewards.keys()),
                        "criteria_triggers":sorted(set(
                            v.get("trigger","UNSPECIFIED") for v in criteria.values() if isinstance(v,dict)
                        )),"classification":"REVIEW_REQUIRED"
                    })
    return {
        "json_content_categories":dict(sorted(classes.items())),
        "json_namespaces":dict(sorted(namespaces.items())),
        "advancement_criterion_trigger_counts":dict(sorted(trigger_counts.items())),
        "advancement_reward_records":advancement_records,
        "function_schedule_literal_sites":schedule_edges,
        "json_parse_warnings":parse_warnings,
        "warnings":"A scheduled function may persist in world state after load/tick root tags are changed.",
    }

def analyze(world):
    world=Path(world).resolve(strict=True)
    if not (world/"level.dat").is_file():
        raise ValueError("Not a Minecraft world")
    if sha((world/"level.dat").read_bytes())!=LEVEL_SHA:
        raise ValueError("Map does not match original v2.2.2f level.dat")
    blocks=command_inventory(scan_world(world),world)
    packs=pack_inventory(world)
    return {
        "status":"READ_ONLY_ORIGINAL_GAMEPLAY_ENTRYPOINT_INVENTORY",
        "map_release":"Drehmal: APOTHEOSIS v2.2.2f","level_sha256":LEVEL_SHA,
        "command_blocks":blocks,"datapack_json_and_schedule":packs,
        "world_bytes_modified":False,"original_code_copied_into_turnbound":False,
        "meaning":"Command opcodes, schedules and advancement triggers are review candidates only, not safe-delete approvals",
        "unverified":["saved scheduled functions/command storage","all corrupt region chunks","structure interactions in client",
                      "actual activation of advancement reward callbacks"]
    }

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--world",type=Path,required=True)
    p.add_argument("--report",type=Path,required=True)
    a=p.parse_args()
    world=a.world.resolve(strict=True)
    out=a.report.resolve()
    if out==world or world in out.parents:
        p.error("Report cannot be stored inside world")
    data=analyze(world)
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(data,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    summary={
        "blocks":data["command_blocks"]["scanned_command_blocks"],
        "non_function_command_blocks":sum(v for x,v in data["command_blocks"]["effective_operation_counts"].items() if x!="function"),
        "scheduled_literal_sites":len(data["datapack_json_and_schedule"]["function_schedule_literal_sites"]),
        "advancement_reward_records":len(data["datapack_json_and_schedule"]["advancement_reward_records"]),
        "map_modified":False
    }
    print(json.dumps(summary))

if __name__=="__main__":
    try:main()
    except (ValueError,OSError,zipfile.BadZipFile) as error:
        print("Official auxiliary execution audit blocked: "+str(error),file=sys.stderr)
        sys.exit(2)

#!/usr/bin/env python3
"""Official Drehmal Foundry lever advancement predicates and effect linkage.

Read-only inspection. Structured condition keypaths/resource IDs only; no raw
advancement JSON, original function commands, NBT, world map or player data.
"""
from __future__ import annotations
import argparse,collections,json,re,sys,zipfile
from pathlib import Path
import inspect_drehmal_teleport_footprints as source
from inspect_drehmal_foundry_gate_wave_paths import source_id

PACK="datapacks/hi_drehmal.zip"
IDS=("foundry:1","foundry:2","foundry:3")
FUNCTION_IDS=(
 "foundry:levers/1_check","foundry:levers/2_check","foundry:levers/3_check",
 "foundry:levers/1","foundry:levers/2","foundry:levers/3",
 "foundry:levers/door","foundry:open/0","foundry:open/1",
 "foundry:open/2","foundry:open/3",
)
ID_RX=re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")

def predicate_summary(value):
    structures=[]
    resource_ids=set()
    def visit(obj,path,depth=0):
        if depth>12:return
        if isinstance(obj,dict):
            for key,v in obj.items():
                # Keypaths reveal predicate shape but not author-defined values.
                structures.append(path+"."+key if path else key)
                visit(v,path+"."+key if path else key,depth+1)
        elif isinstance(obj,list):
            for item in obj[:32]:visit(item,path+"[]",depth+1)
        elif isinstance(obj,str) and ID_RX.fullmatch(obj):
            resource_ids.add(obj)
    visit(value,"")
    return {"condition_keypaths":sorted(set(structures))[:100],
            "literal_resource_ids":sorted(resource_ids)[:40]}

def audit(pack):
    with zipfile.ZipFile(pack) as z:
        names=z.namelist()
        if len(set(names))!=len(names):
            raise ValueError("Duplicate official pack entries")
        advancements={}
        for ident in IDS:
            ns,path=ident.split(":",1)
            name="data/"+ns+"/advancements/"+path+".json"
            if names.count(name)!=1:raise ValueError("Pinned Foundry lever advancement absent")
            doc=json.loads(z.read(name))
            if not isinstance(doc,dict) or not isinstance(doc.get("criteria"),dict):
                raise ValueError("Malformed original advancement")
            criteria=[]
            for key,details in doc["criteria"].items():
                if not isinstance(details,dict):raise ValueError("Malformed original trigger")
                row={"criterion_id":key,"trigger_type":details.get("trigger")}
                row.update(predicate_summary(details.get("conditions",{})))
                criteria.append(row)
            reward=doc.get("rewards",{})
            advancements[ident]={
                "criteria":criteria,
                "reward_function_id":reward.get("function") if isinstance(reward,dict) else None,
                "advancement_is_automatically_granted_in_live_server":False,
            }
        profiles={}
        for ident in FUNCTION_IDS:
            ns,path=ident.split(":",1)
            name="data/"+ns+"/functions/"+path+".mcfunction"
            if name not in names:raise ValueError("Pinned original Foundry function missing: "+ident)
            raw=z.read(name).decode("utf-8").splitlines()
            ops=collections.Counter()
            refs=set()
            scoreboard_categories=collections.Counter()
            for line in raw:
                v=line.strip().lstrip("/")
                if not v or v.startswith("#"):continue
                root=v.split(maxsplit=1)[0].lower()
                ops[root]+=1
                for fid in re.findall(r"\bfunction\s+([a-z0-9_.-]+:[a-z0-9_./-]+)",v):
                    refs.add(fid)
                if "scoreboard " in v:
                    scoreboard_categories["scoreboard_operation_sites"]+=1
                if " if score " in v or " unless score " in v:
                    scoreboard_categories["execute_score_condition_sites"]+=1
                if " clone " in v or v.startswith("clone "):
                    scoreboard_categories["clone_sites"]+=1
                if "schedule function" in v:
                    scoreboard_categories["scheduled_call_sites"]+=1
            profiles[ident]={"command_roots":dict(sorted(ops.items())),
                             "direct_function_callees":sorted(refs),
                             "derived_effect_categories":dict(sorted(scoreboard_categories.items()))}
    return {
      "status":"OFFICIAL_FOUNDRY_LEVER_ADVANCEMENT_TRIGGER_PREDICATES_READ_ONLY",
      "lever_advancement_conditions":advancements,
      "original_lever_and_door_function_profiles":profiles,
      "actual_live_advancement_granted_verified":False,
      "live_foundry_door_opened_verified":False,
      "original_six_wave_gameplay_disabled":False,
      "map_separation_complete":False,
    }

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--original",required=True,type=Path)
    parser.add_argument("--output",required=True,type=Path)
    a=parser.parse_args()
    original=a.original.resolve(strict=True)
    output=a.output.resolve()
    if output==original or original in output.parents or (original/"turnbound-world.marker").exists():
        parser.error("A disposable original source and external report are required")
    source.check_official_source(original)
    result=audit(original/PACK)
    source.check_official_source(original)
    output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"status":result["status"],"advancements":len(IDS),
                      "live_door_verified":False,"map_separation_complete":False}))
if __name__=="__main__":
    try:main()
    except (ValueError,TypeError,KeyError,OSError,zipfile.BadZipFile) as exc:
        print("Foundry lever advancement read-only inspector REFUSED: "+str(exc),file=sys.stderr)
        sys.exit(2)

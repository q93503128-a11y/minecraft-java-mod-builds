#!/usr/bin/env python3
"""Experimental eighth COPY-ONLY boundary: nine original non-structure reward functions.

Does not establish gameplay safety. Preserve every advancement criterion and all
other original datapack contents, and restore exact bytes before game use.
"""
import argparse
import copy
import io
import json
import sys
import zipfile
from pathlib import Path
import preview_drehmal_weapon_rewards as weapons
import prepare_drehmal_isolated_copy as six
import verify_drehmal_start_chain_isolation as seven
import patch_drehmal_terminus_commands as region

SOURCE_SHA="e66e573ce57baf44dd5b4e386cc45190a13f5fed78b45f1691866f99c48ddf2f"
RECEIPT="turnbound-nonweapon-callback-eight-preview.json"
BACKUP="turnbound-nonweapon-callback-eight-backup.zip"
PACK="datapacks/hi_drehmal.zip"
STATUS="EIGHTH_ORIGINAL_CALLBACK_OFFLINE_PREVIEW"
TARGET_NAMES=frozenset((
  "data/entities/advancements/kill_rhalon.json",
  "data/entities/advancements/hurt_by_dnzombie.json",
  "data/players/advancements/hurt_by_blooddolphin.json",
  "data/players/advancements/sleep_on_lodahr.json",
  "data/players/advancements/get_craft_replace.json",
  "data/players/advancements/virtuo_aegis.json",
  "data/players/advancements/eat_rotten_meat.json",
  "data/players/advancements/eat_gay_apple.json",
  "data/players/advancements/kill_with_bowblade.json",
))
ALLOWED_TRIGGERS=frozenset(("minecraft:player_killed_entity","minecraft:entity_hurt_player",
  "minecraft:consume_item","minecraft:slept_in_bed","minecraft:inventory_changed",
  "minecraft:using_item"))

def hash_bytes(raw): return weapons.digest(raw)

def load_targets(report):
    raw=Path(report).read_bytes()
    if hash_bytes(raw)!=SOURCE_SHA:
        raise ValueError("Original source callback-review SHA mismatch")
    doc=json.loads(raw)
    if (doc.get("schema")!=1 or doc.get("state")!="READ_ONLY_NOT_ACTIVATION_PROOF" or
        doc.get("source_aux_sha256")!=weapons.AUX_SHA or
        doc.get("source_graph_sha256")!=weapons.GRAPH_SHA or
        doc.get("nonweapon_callback_records")!=181):
        raise ValueError("Original source callback review identity changed")
    targets={}
    for row in doc["callbacks"]:
        name=row["advancement_file"]
        if name not in TARGET_NAMES: continue
        if (name in targets or
            row.get("review_group") not in ("ENTITY_COMBAT_REVIEW","PLAYER_REWARD_OR_STATE_REVIEW") or
            row.get("callback_exists") is not True or
            row.get("possible_world_edit") is not False or
            row.get("has_schedule_in_static_callgraph") is not False or
            row.get("reachable_from_original_auto_roots") is not False or
            row.get("direct_function_callers")!=[] or
            row.get("transitive_function_count")!=1 or
            row.get("reward_fields")!=["function"] or
            row.get("trigger_types") not in [[v] for v in ALLOWED_TRIGGERS] or
            row.get("decision")!="NOT_APPROVED_FOR_REMOVAL_OR_REUSE" or
            not isinstance(row.get("callback_id"),str) or
            not isinstance(row.get("advancement_source_sha256"),str) or
            len(row["advancement_source_sha256"])!=64):
            raise ValueError("Unreviewed or changed original callback candidate")
        targets[name]=(row["advancement_source_sha256"],row["callback_id"])
    if set(targets)!=TARGET_NAMES:
        raise ValueError("The pinned nine original callback choices changed")
    return targets

def transform_entry(raw,expected_sha,fid):
    if hash_bytes(raw)!=expected_sha: raise ValueError("Original advancement SHA mismatch")
    old=json.loads(raw)
    if (not isinstance(old,dict) or not isinstance(old.get("criteria"),dict) or
        old.get("rewards")!={"function":fid}):
        raise ValueError("Unexpected advancement function or other reward fields")
    new=copy.deepcopy(old)
    new["rewards"]={}
    encoded=(json.dumps(new,separators=(",",":"),ensure_ascii=False)+"\n").encode("utf-8")
    if json.loads(encoded)!=(old|{"rewards":{}}):
        raise ValueError("Advancement criteria or unrelated field changed")
    return encoded

def compare(before,after,targets):
    with zipfile.ZipFile(io.BytesIO(before)) as orig,zipfile.ZipFile(io.BytesIO(after)) as new:
        names=orig.namelist()
        if len(names)!=len(set(names)) or new.namelist()!=names:
            raise ValueError("Modified datapack ZIP member inventory changed")
        if set(targets)!=TARGET_NAMES or not set(targets).issubset(names):
            raise ValueError("Invalid nine-callback manifest")
        for name in names:
            a,b=orig.read(name),new.read(name)
            if name in targets:
                if b!=transform_entry(a,*targets[name]):
                    raise ValueError("Original callback not isolated exactly")
            elif a!=b:
                raise ValueError("Unrelated original datapack entry was changed")
    return True

def repack(original,targets):
    stream=io.BytesIO()
    with zipfile.ZipFile(io.BytesIO(original)) as source,zipfile.ZipFile(stream,"w") as dest:
        dest.comment=source.comment
        for entry in source.infolist():
            raw=source.read(entry)
            if entry.filename in targets:
                raw=transform_entry(raw,*targets[entry.filename])
            dest.writestr(entry,raw)
    changed=stream.getvalue()
    compare(original,changed,targets)
    return changed

def guard(world):
    world=seven.validate_copy(Path(world),overlay=True)
    r=six.receipt(world,seven.RECEIPT)
    if (r.get("schema")!=1 or r.get("status")!="OFFLINE_START_CHAIN_RESIDUAL_EXPERIMENT"
        or r.get("start_chain_total")!=57 or
        region.file_sha(world/seven.REGION)!=r.get("after") or
        seven.verify_empty_chain(world)["empty_start_chain_commands"]!=57):
        raise ValueError("Missing or modified seventh-stage original chain boundary")
    pack=world/PACK
    if not pack.is_file() or pack.is_symlink() or (world/"turnbound-world.marker").exists():
        raise ValueError("Missing source pack, unsafe symlink or activated world")
    return world,pack

def preflight(world,targets):
    world,pack=guard(world)
    if (world/RECEIPT).exists() or (world/BACKUP).exists():
        raise ValueError("Eighth preview exists or was interrupted")
    seven.verify(world)
    before=pack.read_bytes()
    if hash_bytes(before)!=six.receipt(world,six.ROOT_RECEIPT)["patched_pack_sha256"]:
        raise ValueError("Base original pack does not match seventh-stage source")
    return before,repack(before,targets)

def apply(world,targets):
    world,pack=guard(world)
    before,after=preflight(world,targets)
    (world/BACKUP).write_bytes(before)
    if hash_bytes((world/BACKUP).read_bytes())!=hash_bytes(before):
        raise ValueError("Backup verification failed")
    record={"schema":1,"status":STATUS,"count":9,"review_sha256":SOURCE_SHA,
        "base_pack_sha256":hash_bytes(before),"after_pack_sha256":hash_bytes(after),
        "targets":{name:{"sha":sha,"callback":fid} for name,(sha,fid) in sorted(targets.items())},
        "advancement_criteria_preserved":True,"original_code_reused":False,
        "world_activated":False,"structure_functionality_tested":False,
        "map_separation_complete":False,"playtested":False}
    (world/RECEIPT).write_text(json.dumps(record,indent=2)+"\n")
    weapons.atomic_file(pack,after)
    return verify(world)

def verify(world):
    world,pack=guard(world)
    receipt,backup=world/RECEIPT,world/BACKUP
    if any(not p.is_file() or p.is_symlink() for p in (receipt,backup)):
        raise ValueError("Missing or unsafe eighth-stage receipt/backup")
    record=json.loads(receipt.read_text(encoding="utf-8"))
    if (record.get("schema")!=1 or record.get("status")!=STATUS or
        record.get("count")!=9 or record.get("review_sha256")!=SOURCE_SHA or
        record.get("original_code_reused") is not False or
        record.get("world_activated") is not False or
        record.get("map_separation_complete") is not False or
        set(record.get("targets",{}))!=TARGET_NAMES):
        raise ValueError("Unrecognized eighth-stage receipt")
    before,after=backup.read_bytes(),pack.read_bytes()
    if (hash_bytes(before)!=record.get("base_pack_sha256") or
        hash_bytes(before)!=six.receipt(world,six.ROOT_RECEIPT)["patched_pack_sha256"] or
        hash_bytes(after)!=record.get("after_pack_sha256")):
        raise ValueError("Original source or eighth-stage pack hash chain differs")
    targets={name:(row["sha"],row["callback"]) for name,row in record["targets"].items()}
    compare(before,after,targets)
    return {**record,"verified":True}

def restore(world):
    record=verify(world)
    world,pack=guard(world)
    weapons.atomic_file(pack,(world/BACKUP).read_bytes())
    if hash_bytes(pack.read_bytes())!=record["base_pack_sha256"]:
        raise ValueError("Datapack exact restoration failed")
    (world/RECEIPT).unlink()
    (world/BACKUP).unlink()
    seven.verify(world)
    return {"status":"EIGHTH_STAGE_RESTORED_EXACTLY_TO_SEVEN",
            "restored_pack_sha256":record["base_pack_sha256"],
            "other_nonweapon_callbacks_untouched":172,
            "map_separation_complete":False,"playtested":False}

def main():
    p=argparse.ArgumentParser()
    p.add_argument("action",choices=("inspect","apply","verify","restore"))
    p.add_argument("--world-copy",required=True,type=Path)
    p.add_argument("--source-review",type=Path)
    p.add_argument("--output",required=True,type=Path)
    a=p.parse_args()
    world=a.world_copy.resolve(strict=True);out=a.output.resolve()
    if out==world or world in out.parents:
        p.error("Report must be outside copied world")
    if a.action in ("inspect","apply"):
        if not a.source_review:p.error("--source-review is required for inspect/apply")
        targets=load_targets(a.source_review)
        if a.action=="inspect":
            before,after=preflight(world,targets)
            result={"status":"EIGHTH_STAGE_PREFLIGHT_NO_WRITE",
                "count":len(targets),"before_sha256":hash_bytes(before),
                "after_sha256":hash_bytes(after),"world_modified":False,
                "playtested":False}
        else: result=apply(world,targets)
    elif a.action=="verify":result=verify(world)
    else:result=restore(world)
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(result,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    print(json.dumps({"status":result["status"],"count":result.get("count",9),
        "map_separation_complete":False,"playtested":False}))

if __name__=="__main__":
    try:main()
    except (ValueError,KeyError,TypeError,OSError,zipfile.BadZipFile) as exc:
        print("Eighth callback offline preview REFUSED: "+str(exc),file=sys.stderr)
        sys.exit(2)

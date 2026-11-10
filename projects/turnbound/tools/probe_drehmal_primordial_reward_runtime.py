#!/usr/bin/env python3
"""Selective original advancement reward isolation on official map disposable copies.

The original 7-stage world is verified before this experimental change.
Only one known original advancement's reward.function link is removed on ONE
copy, followed by /reload. World structures, all other datapack members,
and all original code remain byte-identical. No user save or release asset
is modified. This does not imply that original puzzles work or that the
candidate is causally responsible until the matched server test confirms it.
"""
from __future__ import annotations
import argparse
import copy as copy_module
import hashlib
import io
import json
import math
import zipfile
import os
import signal
import subprocess
import sys
import time
from pathlib import Path

import probe_drehmal_movement_attribution as old
import probe_drehmal_copy_player_presence as players
import probe_drehmal_start_runtime as srv
import inspect_drehmal_teleport_footprints as geo
from audit_drehmal_starter_mechanisms import World

# Fixed, inspected locations from official copied-map geometry evidence:
# explicitly CI-only. Neither is a TURNBOUND start or player checkpoint.
SITES={
    "DrehmalAliceCI": ("airborne_legacy_test",(26458.5,223.,-285.5)),
    "DrehmalBobCI": ("conservative_full_floor",(26458.5,219.,-288.5)),
}
SAMPLE_SECONDS=(0.,0.15,0.35,0.65,1.0,1.6,2.5,4.0)
MARKER=old.CI_MARKER
PRIMORDIAL_ENTRY="data/advancements/advancements/primordial/root.json"
PRIMORDIAL_ID="advancements:primordial/root"
PRIMORDIAL_REWARD="players:sethost"
EXPECTED_FUNCTION_SHA="9e850087f0ae0646d44ed54ff99d911fe0c5aba88b9ed67e9352508604792ebe"
PACK_REL="datapacks/hi_drehmal.zip"

def digest(content):
    return hashlib.sha256(content).hexdigest()

def filtered_advancement(raw):
    src=json.loads(raw)
    if not isinstance(src,dict) or not isinstance(src.get("criteria"),dict):
        raise ValueError("Pinned primordial root advancement is malformed")
    if "minecraft:location" not in {
        x.get("trigger") for x in src["criteria"].values() if isinstance(x,dict)
    }:
        raise ValueError("Original primordial root location trigger missing")
    reward=src.get("rewards")
    if not isinstance(reward,dict) or reward.get("function")!=PRIMORDIAL_REWARD:
        raise ValueError("Original target reward function is not the pinned expected function")
    # Preserve every other reward key, every criterion, title and metadata.
    after=copy_module.deepcopy(src)
    after["rewards"].pop("function")
    output=(json.dumps(after,ensure_ascii=False,separators=(",",":"))+"\n").encode("utf-8")
    if json.loads(output)!=after:
        raise ValueError("Advancement selective transform serialization drift")
    return output

def repack_without_primordial_reward(before):
    """Build changed ZIP bytes; only one original advancement member may differ."""
    buf=io.BytesIO()
    with zipfile.ZipFile(io.BytesIO(before)) as src,zipfile.ZipFile(buf,"w") as dst:
        infos=src.infolist()
        names=[v.filename for v in infos]
        if names.count(PRIMORDIAL_ENTRY)!=1:
            raise ValueError("Exact primordial root advancement not uniquely present")
        f="data/players/functions/sethost.mcfunction"
        if names.count(f)!=1 or digest(src.read(f))!=EXPECTED_FUNCTION_SHA:
            raise ValueError("Original players:sethost function bytes changed")
        dst.comment=src.comment
        for info in infos:
            raw=src.read(info)
            if info.filename==PRIMORDIAL_ENTRY:
                raw=filtered_advancement(raw)
            dst.writestr(info,raw)
    after=buf.getvalue()
    with zipfile.ZipFile(io.BytesIO(before)) as a,zipfile.ZipFile(io.BytesIO(after)) as z:
        if a.namelist()!=z.namelist():
            raise ValueError("Original world pack member inventory modified")
        changes=[name for name in a.namelist() if a.read(name)!=z.read(name)]
        if changes!=[PRIMORDIAL_ENTRY]:
            raise ValueError("Datapack changes exceeded one exact advancement")
        if z.read("data/players/functions/sethost.mcfunction")!=a.read(
                "data/players/functions/sethost.mcfunction"):
            raise ValueError("Original function source bytes must remain untouched")
        expected=filtered_advancement(a.read(PRIMORDIAL_ENTRY))
        if z.read(PRIMORDIAL_ENTRY)!=expected:
            raise ValueError("Original advancement criteria or unrelated rewards changed")
    return after

def isolate_copy_datapack(world):
    pack=world/PACK_REL
    backup=world/"turnbound-primordial-root-ci-backup.zip"
    if (world/"turnbound-world.marker").exists() or pack.is_symlink() or not pack.is_file():
        raise ValueError("Refusing to isolate reward on untrusted or activated world")
    if backup.exists():
        raise ValueError("Refusing to overwrite an existing private CI backup")
    before=pack.read_bytes()
    after=repack_without_primordial_reward(before)
    backup.write_bytes(before)
    if digest(backup.read_bytes())!=digest(before):
        raise ValueError("Disposable base pack backup failed identity check")
    replacement=pack.with_name("hi_drehmal.zip.turnbound-ci-new")
    if replacement.exists():
        raise ValueError("Refusing incomplete previous CI datapack write")
    replacement.write_bytes(after)
    if digest(replacement.read_bytes())!=digest(after):
        raise ValueError("Disposable pack transformed file changed")
    os.replace(replacement,pack)
    if digest(pack.read_bytes())!=digest(after):
        raise ValueError("Selective source advancement patch not persisted")
    return {
        "isolated_advancement_id":PRIMORDIAL_ID,
        "isolated_reward_function_id":PRIMORDIAL_REWARD,
        "modified_pack_members":1,
        "function_code_unchanged":True,
        "other_advancement_fields_unchanged":True,
        "copy_only_original_pack_backup_sha256":digest(before),
        "copy_only_modified_pack_sha256":digest(after),
        "original_world_unchanged":True,
    }


def verify_sites(source, world_copy):
    source=Path(source).resolve(strict=True)
    world_copy=old.probe_preflight(world_copy)
    if source==world_copy or world_copy in source.parents or source in world_copy.parents:
        raise ValueError("Original and copied worlds must be independent")
    geo.check_official_source(source)
    a,b=World(source),World(world_copy)
    details={}
    for name,(role,position) in SITES.items():
        first=geo.footprint(a,position)
        staged=geo.footprint(b,position)
        if first!=staged:
            raise ValueError("Seven-stage copy differs from original scene")
        required=("AIRBORNE_NO_FLOOR" if role=="airborne_legacy_test"
                  else "CONSERVATIVE_FULL_FLOOR_TWO_BLOCK_PASSABLE")
        if staged["standing_space"]!=required:
            raise ValueError("CI-only comparison no longer matches inspected geometry")
        details[role]={
            "ci_only_coordinate":[*position],
            "ground":staged["ground"],"feet":staged["feet"],
            "head":staged["head"],"geometry_class":staged["standing_space"]
        }
    return world_copy,details

def distance(a,b):
    if len(a)!=3 or len(b)!=3 or not all(math.isfinite(v) for v in (*a,*b)):
        raise ValueError("Invalid horizontal distance input")
    return round(math.hypot(a[0]-b[0],a[2]-b[2]),3)

def classify(airborne,floored):
    if not all(math.isfinite(x) and x>=0 for x in (airborne,floored)):
        raise ValueError("Invalid observed displacement")
    if airborne>80 and floored<5:
        return "UNSUPPORTED_AIRBORNE_LOCATION_SPECIFIC_ROLLBACK_IN_THIS_PROBE"
    if airborne>80 and floored>80:
        return "BOTH_LOCATIONS_ROLL_BACK_INVESTIGATE_SHARED_SERVER_OR_MAP_TRIGGER"
    if airborne<5 and floored<5:
        return "NEITHER_LOCATION_ROLLED_BACK_IN_THIS_PROBE"
    return "MIXED_RESULT_NO_CAUSE_CONFIRMED"

def probe(project,source,copy,mode):
    if mode not in ("kept","root_reward_removed"):
        raise ValueError("Only source-preserving primordial-root CI comparison allowed")
    world,geometry=verify_sites(source,copy)
    password=old.configure_server(project,False)  # command blocks OFF
    private=project/"build"/("drehmal-root-reward-"+mode+".internal.log")
    private.parent.mkdir(parents=True,exist_ok=True)
    result={
       "status":"INCOMPLETE",
       "primordial_root_reward_mode":mode,
       "original_datapack_remains_enabled":False,
       "one_advancement_function_link_removed":mode=="root_reward_removed",
       "world":"OFFICIAL_DREHMAL_V2_2_2F_COPY_SEVEN_STAGE",
       "original_sha_verified":True,
       "two_protocol_clients_connected":False,
       "command_blocks_enabled":False,
       "original_world_modified":False,
       "game_start_approved":False,
       "map_separation_complete":False,
       "interacted_with_real_facilities":False,
       "graphical_client_playtested":False,
       "verified_site_geometry":geometry,
    }
    cmd=["gradle","runServer","--no-daemon",
         "--args=--nogui --universe "+str(world.parent)+" --world "+world.name]
    with private.open("w",encoding="utf-8") as log:
        server=subprocess.Popen(cmd,cwd=project,stdout=log,
                                stderr=subprocess.STDOUT,start_new_session=True)
        rcon=None;bot=None;botlog=None
        try:
            rcon=srv.wait_rcon(server,password)
            before_list=rcon.command("datapack list enabled")
            if "file/hi_drehmal.zip" not in before_list:
                raise ValueError("Original author datapack absent at startup")
            if mode=="root_reward_removed":
                result["selective_patch"]=isolate_copy_datapack(world)
            else:
                result["selective_patch"]={
                    "modified_pack_members":0,
                    "function_code_unchanged":True,
                    "original_world_unchanged":True
                }
            # Critical controlled condition: reload BOTH variants. Otherwise a
            # reload side effect could be mistaken for the removed reward link.
            response=rcon.command("reload")
            if any(x in response.lower() for x in ("unknown","error","failed")):
                raise ValueError("Server refused controlled source datapack reload")
            time.sleep(3)
            result["both_variants_reloaded_before_clients"]=True
            enabled=rcon.command("datapack list enabled")
            if "file/hi_drehmal.zip" not in enabled:
                raise ValueError("Original author datapack became disabled during test")
            result["original_datapack_remains_enabled"]=True
            loaded=rcon.command("forceload add 26448 -304 26480 -240")
            if any(x in loaded.lower() for x in ("error","failed","unknown")):
                raise ValueError("Cannot load original START scene for CI-only test")
            botlog=(project/"build"/"grounded-compare-bots.internal.log").open("w")
            if not os.environ.get("TURNBOUND_PROBE_NODE_MODULES"):
                raise ValueError("Missing 1.20.1 synthetic bot dependencies")
            bot=subprocess.Popen(
                ["node","tools/probe_drehmal_map_players_with_movement.cjs"],
                cwd=project,stdout=botlog,stderr=subprocess.STDOUT,
                start_new_session=True)
            players.await_players(rcon,bot)
            result["two_protocol_clients_connected"]=True
            result["original_join_positions"]={
                SITES[name][0]:[*players.query_position(rcon,name)] for name in SITES
            }
            for name,(_,pos) in SITES.items():
                xyz=" ".join(str(v) for v in pos)
                answer=rcon.command(f"tp {name} {xyz}")
                if "teleported" not in answer.lower():
                    raise ValueError("CI-only physical test teleport refused")
            begun=time.monotonic()
            samples=[]
            for elapsed in SAMPLE_SECONDS:
                delta=begun+elapsed-time.monotonic()
                if delta>0:time.sleep(delta)
                readings={}
                for name,(role,location) in SITES.items():
                    actual=players.query_position(rcon,name)
                    readings[role]={
                        "position":[round(x,3) for x in actual],
                        "horizontal_offset":distance(actual,location),
                        "vertical_offset":round(actual[1]-location[1],3),
                        "dimension":old.dimension(rcon,name),
                    }
                samples.append({
                    "elapsed_ms":round((time.monotonic()-begun)*1000),
                    "game_ticks":srv.game_time(rcon.command("time query gametime")),
                    "observations":readings,
                })
            result["samples"]=samples
            last=samples[-1]["observations"]
            air=last["airborne_legacy_test"]["horizontal_offset"]
            ground=last["conservative_full_floor"]["horizontal_offset"]
            result["final_horizontal_offsets"]={
                "airborne":air,"floored":ground}
            result["comparison"]=classify(air,ground)
            result["both_players_remained_online"]=all(
                name in rcon.command("list") for name in SITES)
            if not result["both_players_remained_online"]:
                raise ValueError("One synthetic player left during comparison")
            rcon.command("save-all flush")
            rcon.command("stop")
            server.wait(timeout=80)
            if server.returncode!=0:
                raise ValueError("Disposable seven-stage Fabric server stopped abnormally")
            n,warn=srv.count_source_commands(world)
            if n!=57 or warn:
                raise ValueError("Original START chain unexpectedly changed")
            result["start_commands_still_disabled"]=True
            result["server_stopped_cleanly"]=True
            geo.check_official_source(source)
            result["original_sha_preserved_after_runtime"]=True
            result["status"]="OFFICIAL_COPIED_MAP_PRIMORDIAL_REWARD_LINK_OBSERVED"
            return result
        finally:
            if bot is not None and bot.poll() is None:
                os.killpg(bot.pid,signal.SIGTERM)
                try:bot.wait(timeout=8)
                except subprocess.TimeoutExpired:
                    os.killpg(bot.pid,signal.SIGKILL);bot.wait(timeout=10)
            if botlog is not None:botlog.close()
            if rcon is not None:rcon.close()
            if server.poll() is None:
                os.killpg(server.pid,signal.SIGTERM)
                try:server.wait(timeout=12)
                except subprocess.TimeoutExpired:
                    os.killpg(server.pid,signal.SIGKILL);server.wait(timeout=10)

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--project",type=Path,required=True)
    parser.add_argument("--original",type=Path,required=True)
    parser.add_argument("--world-copy",type=Path,required=True)
    parser.add_argument("--output",type=Path,required=True)
    parser.add_argument("--root-reward-mode",required=True,choices=("kept","root_reward_removed"))
    a=parser.parse_args()
    project=a.project.resolve(strict=True)
    source=a.original.resolve(strict=True)
    copy=a.world_copy.resolve(strict=True)
    out=a.output.resolve()
    if any(out==w or w in out.parents for w in (copy,source)):
        parser.error("Report cannot overwrite official or copied map")
    report=probe(project,source,copy,a.root_reward_mode)
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(report,indent=2,ensure_ascii=True)+"\n")
    print(json.dumps({k:report[k] for k in (
        "status","primordial_root_reward_mode","comparison","final_horizontal_offsets",
        "game_start_approved","map_separation_complete")}))

if __name__=="__main__":
    try:main()
    except (OSError,ValueError,TimeoutError,ConnectionError,
            subprocess.SubprocessError) as exc:
        print("Drehmal primordial-root selective reward isolation BLOCKED: "+str(exc),file=sys.stderr)
        sys.exit(2)

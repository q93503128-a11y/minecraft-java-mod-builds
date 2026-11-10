#!/usr/bin/env python3
"""Drehmal official copied-map landing experiment: airborne vs authored floor.

The exact original START and production checkpoint are never selected.
Only two CI-named protocol players receive RCON teleports in an expendable
seven-stage copy. The original map must pass independent SHA checks.
"""
from __future__ import annotations
import argparse
import json
import math
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

def probe(project,source,copy):
    world,geometry=verify_sites(source,copy)
    password=old.configure_server(project,False)  # command blocks OFF
    private=project/"build"/"drehmal-airborne-vs-grounded.internal.log"
    private.parent.mkdir(parents=True,exist_ok=True)
    result={
       "status":"INCOMPLETE",
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
            result["status"]="OFFICIAL_COPIED_MAP_AIRBORNE_VS_FLOOR_COMPARE_OBSERVED"
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
    a=parser.parse_args()
    project=a.project.resolve(strict=True)
    source=a.original.resolve(strict=True)
    copy=a.world_copy.resolve(strict=True)
    out=a.output.resolve()
    if any(out==w or w in out.parents for w in (copy,source)):
        parser.error("Report cannot overwrite official or copied map")
    report=probe(project,source,copy)
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(report,indent=2,ensure_ascii=True)+"\n")
    print(json.dumps({k:report[k] for k in (
        "status","comparison","final_horizontal_offsets",
        "game_start_approved","map_separation_complete")}))

if __name__=="__main__":
    try:main()
    except (OSError,ValueError,TimeoutError,ConnectionError,
            subprocess.SubprocessError) as exc:
        print("Official copied-map airborne vs floor test BLOCKED: "+str(exc),file=sys.stderr)
        sys.exit(2)

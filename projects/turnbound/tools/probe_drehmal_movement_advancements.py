#!/usr/bin/env python3
"""Trace an unexplained *horizontal* displacement on isolated official Drehmal copies.

Two separate copies of the same seven-stage official map are compared with
Minecraft's command-block execution enabled and disabled. The single world file
source is NEVER modified. This is not a game start or playable map test.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import math
import uuid
import os
import re
import secrets
import shutil
import signal
import subprocess
import sys
import time
from pathlib import Path

import probe_drehmal_copy_player_presence as prior
import probe_drehmal_start_runtime as scene
import verify_drehmal_start_chain_isolation as seven
from audit_drehmal_starter_mechanisms import World

CI_MARKER=".turnbound-disposable-ci-movement"
ACTOR=prior.PROBE_NAMES[0]
TARGET=prior.FIRST_POSITION
SAMPLE_TIMES=(0,0.15,0.35,0.6,0.95,1.35,1.9,2.7,4.1)
RCON_PORT=scene.PORT
MAX_HORIZONTAL_DIFFERENCE_FOR_STABILITY=5.0

def offline_player_id(name):
    raw=bytearray(hashlib.md5(("OfflinePlayer:"+name).encode()).digest())
    raw[6]=(raw[6]&15)|0x30
    raw[8]=(raw[8]&0x3f)|0x80
    return str(uuid.UUID(bytes=bytes(raw)))

def advancement_snapshot(world,name):
    file=Path(world)/"advancements"/(offline_player_id(name)+".json")
    if not file.exists():
        return set()
    if file.is_symlink() or not file.is_file():
        raise ValueError("Unsafe synthetic advancement file")
    data=json.loads(file.read_text(encoding="utf-8"))
    if not isinstance(data,dict):
        raise ValueError("Unexpected advancement data")
    return {key for key,val in data.items() if key!="DataVersion"
            and isinstance(val,dict) and val.get("done") is True}


def location_difference(a,b):
    if len(a)!=3 or len(b)!=3 or not all(math.isfinite(v) for v in (*a,*b)):
        raise ValueError("Invalid finite 3-axis position sample")
    return {"delta_x":round(a[0]-b[0],3),
            "delta_y":round(a[1]-b[1],3),
            "delta_z":round(a[2]-b[2],3),
            "horizontal":round(math.hypot(a[0]-b[0],a[2]-b[2]),3)}

def classify(on_distance,off_distance,threshold=30.0):
    if not all(math.isfinite(v) and v>=0 for v in (on_distance,off_distance)):
        raise ValueError("Invalid horizontal displacements")
    if on_distance>=threshold and off_distance<MAX_HORIZONTAL_DIFFERENCE_FOR_STABILITY:
        return "DISPLACEMENT_DEPENDS_ON_COMMAND_BLOCK_EXECUTION_IN_THIS_PROBE"
    if on_distance>=threshold and off_distance>=threshold:
        return "DISPLACEMENT_PERSISTS_WITH_COMMAND_BLOCKS_DISABLED"
    if on_distance<threshold and off_distance>=threshold:
        return "OFF_ONLY_DISPLACEMENT_UNEXPECTED_REQUIRES_REPRODUCTION"
    return "NO_LARGE_HORIZONTAL_DISPLACEMENT_REPRODUCED"

def probe_preflight(world):
    world=Path(world).resolve(strict=True)
    if (world/"turnbound-world.marker").exists():
        raise ValueError("Refusing activated TURNBOUND world")
    result=seven.verify(world)
    if not result.get("seven_stage_offline_experiment_verified"):
        raise ValueError("Not a pinned 7-stage reversible official copy")
    n,warnings=scene.count_source_commands(world)
    if n!=57 or warnings:
        raise ValueError("Not all 57 original START command blocks are disabled")
    return world

def configure_server(project,blocks_enabled):
    folder=project/"run"
    if folder.exists():
        if not (folder/CI_MARKER).is_file():
            raise ValueError("Refusing unknown pre-existing Minecraft server run directory")
        shutil.rmtree(folder)
    folder.mkdir()
    (folder/CI_MARKER).write_text("CI only; temporary official-map copy\n")
    password=secrets.token_hex(19)
    (folder/"eula.txt").write_text("eula=true\n")
    properties=[
        "server-ip=127.0.0.1","server-port=25580",
        "enable-rcon=true",f"rcon.port={RCON_PORT}",f"rcon.password={password}",
        "enable-command-block=false",
        "online-mode=false","white-list=false","max-players=4",
        "gamemode=creative","difficulty=normal",
        "view-distance=3","simulation-distance=3","spawn-protection=0",
        "max-tick-time=-1","motd=TURNBOUND isolated movement attribution only",
    ]
    (folder/"server.properties").write_text("\n".join(properties)+"\n")
    return password

def dimension(rcon,name):
    # The player's NBT Dimension is a server-owned field in 1.20.1. If omitted
    # in this server build, report unknown instead of fabricating a dimension.
    reply=rcon.command(f"data get entity {name} Dimension")
    m=re.search(r"minecraft:[a-z_]+",reply)
    return m.group(0) if m else "UNRESOLVED"

def trace(project,world,blocks_enabled):
    world=probe_preflight(world)
    password=configure_server(project,blocks_enabled)
    label="movement_ack" if blocks_enabled else "bare"
    private_log=project/"build"/f"movement-attribution-{label}.internal.log"
    private_log.parent.mkdir(exist_ok=True,parents=True)
    cmd=["gradle","runServer","--no-daemon",
         "--args=--nogui --universe "+str(world.parent)+" --world "+world.name]
    result={
        "status":"INCOMPLETE",
        "variant":"VANILLA_LIKE_MOVEMENT_ACK" if blocks_enabled else "BARE_TELEPORT_CONFIRM",
        "seven_stage_offline_receipts_verified":True,
        "two_synthetic_protocol_clients_connected":False,
        "original_map_unchanged":True,
        "map_separation_complete":False,
        "gameplay_or_mechanism_tested":False,
        "game_start_coordinate_approved":False,
        "graphical_client_tested":False,
        "world_sourced_from_official_sha_verified_release":True,
    }
    with private_log.open("w",encoding="utf-8") as stream:
        server=subprocess.Popen(cmd,cwd=project,stdout=stream,
                                stderr=subprocess.STDOUT,start_new_session=True)
        rcon=None; bot=None;botlog=None
        try:
            rcon=scene.wait_rcon(server,password)
            reply=rcon.command("forceload add 26448 -304 26480 -240")
            if any(z in reply.lower() for z in ("unknown","error","failed")):
                raise ValueError("Original test scene did not load")
            botlog=(project/"build"/f"movement-bot-{label}.internal.log").open("w")
            if not os.environ.get("TURNBOUND_PROBE_NODE_MODULES"):
                raise ValueError("Missing CI-only minecraft-protocol dependencies")
            script="tools/probe_drehmal_map_players_with_movement.cjs" if blocks_enabled else "tools/probe_drehmal_map_players.cjs"
            bot=subprocess.Popen(["node",script],
                                 cwd=project,stdout=botlog,stderr=subprocess.STDOUT,
                                 start_new_session=True)
            prior.await_players(rcon,bot)
            result["two_synthetic_protocol_clients_connected"]=True
            result["initial_dimension"]=dimension(rcon,ACTOR)
            result["initial_position"]=list(prior.query_position(rcon,ACTOR))
            x,y,z=TARGET
            result["ci_only_target_below_block_id"]=World(world).state(
                (math.floor(x),math.floor(y)-1,math.floor(z)))["id"]
            rcon.command("save-all flush")
            before_advancements=advancement_snapshot(world,ACTOR)
            start_tick=scene.game_time(rcon.command("time query gametime"))
            response=rcon.command(f"tp {ACTOR} {x} {y} {z}")
            if "teleported" not in response.lower():
                raise ValueError("Synthetic CI-only TP was rejected")
            started=time.monotonic()
            positions=[]
            for elapsed in SAMPLE_TIMES:
                deadline=started+elapsed
                if deadline>time.monotonic():time.sleep(deadline-time.monotonic())
                pos=prior.query_position(rcon,ACTOR)
                positions.append({
                    "observation_elapsed_ms":round((time.monotonic()-started)*1000),
                    "game_ticks_elapsed":scene.game_time(rcon.command("time query gametime"))-start_tick,
                    "position":[round(v,3) for v in pos],
                    "dimension":dimension(rcon,ACTOR),
                    "offset_from_ci_target":location_difference(pos,TARGET),
                })
            rcon.command("save-all flush")
            after_advancements=advancement_snapshot(world,ACTOR)
            result["newly_awarded_advancement_ids"]=sorted(after_advancements-before_advancements)
            result["newly_awarded_advancement_count"]=len(result["newly_awarded_advancement_ids"])
            result["timeline"]=positions
            result["max_horizontal_offset"]=max(
                p["offset_from_ci_target"]["horizontal"] for p in positions)
            result["final_horizontal_offset"]=positions[-1]["offset_from_ci_target"]["horizontal"]
            result["final_vertical_offset"]=positions[-1]["offset_from_ci_target"]["delta_y"]
            result["movement_detected_large_horizontal"]=result["max_horizontal_offset"]>=30
            result["all_samples_same_dimension"]=all(
                x["dimension"]==positions[0]["dimension"] for x in positions)
            result["other_protocol_player_still_online"]=all(
                n in rcon.command("list") for n in prior.PROBE_NAMES)
            if not result["other_protocol_player_still_online"]:
                raise ValueError("Other protocol player disconnected during observation")
            rcon.command("save-all flush")
            rcon.command("stop")
            server.wait(timeout=90)
            if server.returncode!=0:
                raise ValueError("Official copied-map Minecraft server failed shutdown")
            result["server_stopped_cleanly"]=True
            # Runtime saving on a disposable map changes its SHA. Do not restore
            # or promote it; workflow deletes the copy after each variant.
            empty,warnings=scene.count_source_commands(world)
            if empty!=57 or warnings:
                raise ValueError("Original 57 START commands changed after runtime")
            result["start_commands_still_disabled"]=True
            result["status"]="OFFICIAL_DREHMAL_MOVEMENT_TRACE_OBSERVED"
            return result
        finally:
            if bot is not None and bot.poll() is None:
                os.killpg(bot.pid,signal.SIGTERM)
                try:bot.wait(timeout=7)
                except subprocess.TimeoutExpired:
                    os.killpg(bot.pid,signal.SIGKILL)
                    bot.wait(timeout=10)
            if botlog is not None:botlog.close()
            if rcon is not None:rcon.close()
            if server.poll() is None:
                os.killpg(server.pid,signal.SIGTERM)
                try:server.wait(timeout=12)
                except subprocess.TimeoutExpired:
                    os.killpg(server.pid,signal.SIGKILL)
                    server.wait(timeout=10)

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--project",required=True,type=Path)
    parser.add_argument("--world-copy",required=True,type=Path)
    parser.add_argument("--mode",required=True,choices=("bare","ack"))
    parser.add_argument("--output",required=True,type=Path)
    args=parser.parse_args()
    project=args.project.resolve(strict=True)
    world=args.world_copy.resolve(strict=True)
    out=args.output.resolve()
    if world==out or world in out.parents:
        parser.error("Observation output must not reside in source or copied map")
    result=trace(project,world,args.mode=="ack")
    out.parent.mkdir(exist_ok=True,parents=True)
    out.write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({
        "status":result["status"],"variant":result["variant"],
        "newly_awarded_advancement_count":result["newly_awarded_advancement_count"],
        "final_horizontal_offset":result["final_horizontal_offset"],
        "max_horizontal_offset":result["max_horizontal_offset"],
        "vanilla_like_movement_packets":args.mode=="ack",
        "map_separation_complete":False,
    }))

if __name__=="__main__":
    try:main()
    except (OSError,ValueError,TimeoutError,ConnectionError,subprocess.SubprocessError) as exc:
        print("Official copied-world movement attribution BLOCKED: "+str(exc),file=sys.stderr)
        sys.exit(2)

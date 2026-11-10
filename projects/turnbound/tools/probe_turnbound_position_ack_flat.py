#!/usr/bin/env python3
"""Disposable FLAT world control for low-level 1.20.1 protocol position handling.

Nothing runs on official Drehmal or production saves. Compare a minimal bot
which only confirms teleport, against one which also sends position+rotation.
"""
from __future__ import annotations
import argparse
import json
import math
import os
import secrets
import shutil
import signal
import subprocess
import sys
import time
from pathlib import Path
import probe_drehmal_start_runtime as server
import probe_drehmal_copy_player_presence as prior

MARKER=".turnbound-position-protocol-ci-only"
TARGETS={
 "TBBareProbe":(80.5,120.0,80.5),
 "TBMoveAckProbe":(90.5,120.0,80.5),
}
SAMPLES=(0,0.35,0.75,1.25,2.15,3.5)

def summary_delta(now,requested):
    return round(math.hypot(now[0]-requested[0],now[2]-requested[2]),3)

def create_run(project):
    run=project/"run"
    if run.exists():
        if not (run/MARKER).is_file(): raise ValueError("Cannot overwrite existing game run")
        shutil.rmtree(run)
    run.mkdir()
    (run/MARKER).write_text("Disposal required; no user saves\n")
    secret=secrets.token_hex(15)
    (run/"eula.txt").write_text("eula=true\n")
    (run/"server.properties").write_text("\n".join([
        "server-ip=127.0.0.1","server-port=25580",
        "enable-rcon=true",f"rcon.port={server.PORT}",f"rcon.password={secret}",
        "level-type=minecraft:flat","level-name=world",
        "online-mode=false","white-list=false","enable-command-block=false",
        "max-players=4","gamemode=creative","difficulty=normal",
        "spawn-protection=0","view-distance=3","simulation-distance=3",
        "max-tick-time=-1","motd=TURNBOUND disposable protocol control",
    ])+"\n")
    return secret

def run_test(project):
    secret=create_run(project)
    output=project/"build"/"protocol-control.private.log"
    output.parent.mkdir(exist_ok=True,parents=True)
    record={"status":"INCOMPLETE","disposable_flat_world_only":True,
            "official_drehmal_loaded":False,"map_separation_complete":False,
            "real_minecraft_server_booted":False,
            "bot_modes":["bare_teleport_confirm","confirm_and_position_packets"],
            "samples":[]}
    with output.open("w") as log:
        process=subprocess.Popen(["gradle","runServer","--no-daemon","--console=plain"],
                cwd=project,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
        rcon=None;bots=None;botlog=None
        try:
            rcon=server.wait_rcon(process,secret)
            record["real_minecraft_server_booted"]=True
            botlog=(project/"build"/"protocol-control-bots.private.log").open("w")
            bots=subprocess.Popen(["node","tools/probe_turnbound_position_ack_flat_bots.cjs"],
                                  cwd=project,env=os.environ.copy(),stdout=botlog,
                                  stderr=subprocess.STDOUT,start_new_session=True)
            until=time.monotonic()+75
            while time.monotonic()<until:
                if bots.poll() is not None:raise ValueError("Bots exited before server join")
                listing=rcon.command("list")
                if all(name in listing for name in TARGETS):break
                time.sleep(0.5)
            else:raise TimeoutError("Synthetic protocol players did not both join")
            for name,xyz in TARGETS.items():
                x,y,z=xyz
                if "teleported" not in rcon.command(f"tp {name} {x} {y} {z}").lower():
                    raise ValueError("CI synthetic teleport refused")
            started=time.monotonic()
            timeline=[]
            for elapsed in SAMPLES:
                if started+elapsed>time.monotonic():
                    time.sleep(started+elapsed-time.monotonic())
                positions={name:[round(x,3) for x in prior.query_position(rcon,name)]
                           for name in TARGETS}
                timeline.append({
                    "elapsed_ms":round((time.monotonic()-started)*1000),
                    "horizontal_offsets":{name:summary_delta(positions[name],point)
                                          for name,point in TARGETS.items()},
                    "positions":positions,
                })
            record["samples"]=timeline
            record["max_horizontal_offsets"]={
                name:max(x["horizontal_offsets"][name] for x in timeline)
                for name in TARGETS
            }
            record["final_horizontal_offsets"]=timeline[-1]["horizontal_offsets"]
            record["two_clients_still_online"]=all(n in rcon.command("list") for n in TARGETS)
            if not record["two_clients_still_online"]:
                raise ValueError("Synthetic clients did not survive the duration")
            rcon.command("stop")
            process.wait(timeout=80)
            if process.returncode!=0:raise ValueError("Disposable Flat server failed shutdown")
            record["status"]="DISPOSABLE_FLAT_PROTOCOL_ACK_CONTROL_PASS"
            return record
        finally:
            if bots is not None and bots.poll() is None:
                os.killpg(bots.pid,signal.SIGTERM)
                try:bots.wait(timeout=8)
                except subprocess.TimeoutExpired:
                    os.killpg(bots.pid,signal.SIGKILL);bots.wait(timeout=10)
            if botlog is not None:botlog.close()
            if rcon is not None:rcon.close()
            if process.poll() is None:
                os.killpg(process.pid,signal.SIGTERM)
                try:process.wait(timeout=12)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid,signal.SIGKILL);process.wait(timeout=10)

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--project",type=Path,required=True)
    p.add_argument("--output",type=Path,required=True)
    a=p.parse_args()
    project=a.project.resolve(strict=True)
    out=a.output.resolve()
    if out==project/"run" or (project/"run") in out.parents:
        p.error("Output must be outside disposable server save")
    result=run_test(project)
    out.parent.mkdir(exist_ok=True,parents=True)
    out.write_text(json.dumps(result,indent=2)+"\n")
    print(json.dumps({
        "status":result["status"],
        "final_horizontal_offsets":result["final_horizontal_offsets"],
        "map_separation_complete":False,
    }))

if __name__=="__main__":
    try:main()
    except (OSError,ValueError,TimeoutError,ConnectionError,subprocess.SubprocessError) as e:
        print("Synthetic protocol controls BLOCKED: "+str(e),file=sys.stderr)
        sys.exit(2)

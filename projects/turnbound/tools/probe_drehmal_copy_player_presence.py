#!/usr/bin/env python3
"""Official Drehmal copied-map 7-vs-8 offline boundary **protocol player** probe.

Creates no world and never touches the source. Requires SHA-verified staged copy
with pre-existing seven/eight receipts. Only disposable CI player names are used.
Does NOT approve the copied map for actual play or original code reuse.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import secrets
import shutil
import signal
import subprocess
import sys
import time
from pathlib import Path

import probe_drehmal_start_runtime as scene
import verify_drehmal_start_chain_isolation as seven
import preview_drehmal_nonweapon_callbacks as eight

MARKER = ".turnbound-disposable-ci-player-probe"
PROBE_NAMES = ("DrehmalAliceCI", "DrehmalBobCI")
ADVANCEMENT = "players:sleep_on_lodahr"
FIRST_POSITION = (26458.5, 223.0, -285.5)
SECOND_POSITION = (26464.5, 223.0, -285.5)


def digest_text(value):
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def preflight(world, stage):
    world = Path(world).resolve(strict=True)
    if (world / "turnbound-world.marker").exists():
        raise ValueError("Never attach player probe to an activated world")
    if stage == "seven":
        result = seven.verify(world)
        if not result.get("seven_stage_offline_experiment_verified"):
            raise ValueError("Seven-layer map copy receipts not verified")
    elif stage == "eight":
        result = eight.verify(world)
        if not result.get("verified") or result.get("count") != 9:
            raise ValueError("Eight-layer map copy receipts not verified")
    else:
        raise ValueError("Only the two hash-pinned map stages are allowed")
    empty, warnings = scene.count_source_commands(world)
    if empty != 57 or warnings:
        raise ValueError("Expected all 57 START commands to be isolated")
    return world


def temporary_server(project):
    folder = project / "run"
    if folder.exists():
        if not (folder / MARKER).is_file():
            raise ValueError("Refusing to replace a non-CI run directory")
        shutil.rmtree(folder)
    folder.mkdir()
    (folder / MARKER).write_text("DISPOSABLE runner only\n", encoding="utf-8")
    secret = secrets.token_hex(18)
    (folder / "eula.txt").write_text("eula=true\n")
    config = [
        "server-ip=127.0.0.1", "server-port=25580",
        "enable-rcon=true", "rcon.port=25581", "rcon.password=" + secret,
        "online-mode=false", "white-list=false", "enable-command-block=true",
        "max-players=4", "spawn-protection=0", "max-tick-time=-1",
        "view-distance=3", "simulation-distance=3",
        "gamemode=creative", "difficulty=normal",
        "motd=TURNBOUND disposable copied-map original-function isolation",
    ]
    (folder / "server.properties").write_text("\n".join(config)+"\n")
    return secret


def query_position(rcon, name):
    reply = rcon.command("data get entity " + name + " Pos")
    found = re.search(
        r"\[\s*(-?\d+(?:\.\d+)?)[dDfF]?\s*,\s*(-?\d+(?:\.\d+)?)[dDfF]?"
        r"\s*,\s*(-?\d+(?:\.\d+)?)[dDfF]?\s*\]", reply
    )
    if not found:
        raise ValueError("Cannot read live synthetic player's position")
    return tuple(float(x) for x in found.groups())


def near(a, b, distance=1.5):
    return all(abs(x-y) <= distance for x, y in zip(a,b))


def await_players(rcon, node, limit=65):
    deadline=time.monotonic()+limit
    while time.monotonic()<deadline:
        if node.poll() is not None:
            raise ValueError("CI synthetic protocol-client process exited early")
        players=rcon.command("list")
        if all(name in players for name in PROBE_NAMES):
            return True
        time.sleep(0.7)
    raise TimeoutError("Two live protocol players did not connect to copied map")


def wait_position(rcon, name, desired, limit=25):
    deadline=time.monotonic()+limit
    while time.monotonic()<deadline:
        position=query_position(rcon,name)
        if near(position,desired):
            return position
        time.sleep(0.6)
    raise ValueError("Synthetic player did not reach CI-only test position")


def server_probe(project, world, stage):
    password=temporary_server(project)
    log=project/"build"/("drehmal-player-"+stage+".internal.log")
    log.parent.mkdir(exist_ok=True,parents=True)
    cmd=["gradle","runServer","--no-daemon",
         "--args=--nogui --universe "+str(world.parent)+" --world "+world.name]
    result={
        "status":"INCOMPLETE",
        "stage":stage,
        "world":"VERIFIED_OFFICIAL_DREHMAL_V2_2_2F_DISPOSABLE_COPY",
        "official_original_world_unchanged":True,
        "official_assets_uploaded":False,
        "turned_on_turnbound_marker":False,
        "turnbound_start_coordinates_approved":False,
        "original_code_reused":False,
        "server_started":False,
        "protocol_players_connected":0,
        "server_ticks":0,
        "start_mechanism_commands_empty":57,
        "start_scene_blocks_checked":0,
        "scene_changes_excluding_synthetic_redstone":None,
        "player_location_changed_after_ci_tp":None,
        "advancement_grant_tested":False,
        "advancement_callback_tag_side_effect_observed":None,
        "advancement_grant_response_valid":False,
        "checkpoint_gameplay_verified":False,
        "structure_mechanism_functionality_verified":False,
        "multiplayer_gameplay_verified":False,
        "human_playtested":False,
        "map_separation_complete":False,
    }
    with log.open("w",encoding="utf-8") as output:
        server=subprocess.Popen(cmd,cwd=project,stdout=output,
                               stderr=subprocess.STDOUT,start_new_session=True)
        rcon=None
        clients=None
        botlog=None
        try:
            rcon=scene.wait_rcon(server,password)
            result["server_started"]=True
            t0=scene.game_time(rcon.command("time query gametime"))
            forced=rcon.command("forceload add 26448 -304 26480 -240")
            if "error" in forced.lower() or "unknown" in forced.lower():
                raise ValueError("Cannot force load original start mechanism scene")
            botlog=(project/"build"/("drehmal-player-"+stage+".protocol.internal.log")).open("w")
            env={**os.environ}
            if not env.get("TURNBOUND_PROBE_NODE_MODULES"):
                raise ValueError("Node client dependencies missing")
            clients=subprocess.Popen(["node","tools/probe_drehmal_map_players.cjs"],
                                     cwd=project,env=env,stdout=botlog,
                                     stderr=subprocess.STDOUT,start_new_session=True)
            await_players(rcon,clients)
            result["protocol_players_connected"]=2
            for name,xyz in zip(PROBE_NAMES,(FIRST_POSITION,SECOND_POSITION)):
                x,y,z=xyz
                reply=rcon.command(f"tp {name} {x} {y} {z}")
                if "teleported" not in reply.lower():
                    raise ValueError("Failed CI-only synthetic coordinate setup")
                wait_position(rcon,name,xyz)
            time.sleep(2)
            before_pos=query_position(rcon,PROBE_NAMES[0])
            result["player_location_changed_after_ci_tp"]=not near(
                before_pos,FIRST_POSITION,2.5)
            tags_before=rcon.command("tag "+PROBE_NAMES[0]+" list")
            grant=rcon.command("advancement grant "+PROBE_NAMES[0]+" only "+ADVANCEMENT)
            if "unknown" in grant.lower() or "error" in grant.lower() or "failed" in grant.lower():
                raise ValueError("Original explicit advancement test rejected")
            result["advancement_grant_response_valid"]=True
            time.sleep(2)
            tags_after=rcon.command("tag "+PROBE_NAMES[0]+" list")
            result["advancement_grant_tested"]=True
            result["advancement_callback_tag_side_effect_observed"]=tags_before!=tags_after
            # The original callback may have other data effects, so tag equality
            # proves ONLY this particular observed tag channel.
            after_pos=query_position(rcon,PROBE_NAMES[0])
            result["position_changed_after_advancement"]=not near(after_pos,before_pos,2.5)
            if not all(n in rcon.command("list") for n in PROBE_NAMES):
                raise ValueError("Synthetic copied-map player disconnected mid-probe")
            t1=scene.game_time(rcon.command("time query gametime"))
            result["server_ticks"]=max(0,t1-t0)
            if result["server_ticks"]<10:
                raise ValueError("Map did not tick with live connected protocol clients")
            rcon.command("save-all flush")
            rcon.command("stop")
            server.wait(timeout=90)
            if server.returncode!=0:
                raise ValueError("Dedicated Fabric server did not stop normally")
            result["server_stopped_cleanly"]=True
            result["status"]="OFFICIAL_COPY_TWO_PROTOCOL_PLAYER_OBSERVATION_PASS"
            return result
        finally:
            if clients is not None and clients.poll() is None:
                os.killpg(clients.pid,signal.SIGTERM)
                try:clients.wait(timeout=7)
                except subprocess.TimeoutExpired:
                    os.killpg(clients.pid,signal.SIGKILL)
                    clients.wait(timeout=7)
            if botlog is not None:botlog.close()
            if rcon is not None:rcon.close()
            if server.poll() is None:
                os.killpg(server.pid,signal.SIGTERM)
                try:server.wait(timeout=12)
                except subprocess.TimeoutExpired:
                    os.killpg(server.pid,signal.SIGKILL)
                    server.wait(timeout=10)


def probe(project, world, stage):
    world=preflight(world,stage)
    baseline=scene.snapshot(world)
    data=server_probe(project,world,stage)
    final=scene.snapshot(world)
    data["start_scene_blocks_checked"]=len(baseline)
    sites=set()  # Probe adds no temporary redstone; any changes are real server effects.
    changes=[p for p in baseline if baseline[p]!=final[p] and p not in sites]
    data["scene_changes_excluding_synthetic_redstone"]=len(changes)
    data["initial_scene_sha256"]=scene.signature(baseline)
    data["final_scene_sha256"]=scene.signature(final)
    data["start_command_blocks_after_boot"],warn=scene.count_source_commands(world)
    if data["start_command_blocks_after_boot"]!=57 or warn:
        raise ValueError("START chain command blocks unexpectedly reactivated")
    return data


def main():
    arg=argparse.ArgumentParser()
    arg.add_argument("--project",required=True,type=Path)
    arg.add_argument("--world-copy",required=True,type=Path)
    arg.add_argument("--stage",required=True,choices=("seven","eight"))
    arg.add_argument("--output",required=True,type=Path)
    a=arg.parse_args()
    project=a.project.resolve(strict=True)
    world=a.world_copy.resolve(strict=True)
    out=a.output.resolve()
    if out==world or world in out.parents:
        arg.error("Never place a probe report inside official copied world")
    result=probe(project,world,a.stage)
    out.parent.mkdir(exist_ok=True,parents=True)
    out.write_text(json.dumps(result,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    print(json.dumps({k:result[k] for k in (
        "status","stage","protocol_players_connected",
        "advancement_grant_tested","advancement_callback_tag_side_effect_observed",
        "scene_changes_excluding_synthetic_redstone","map_separation_complete")}))
    if result["stage"]=="eight" and result["advancement_callback_tag_side_effect_observed"]:
        raise ValueError("Eighth preview still produced tag side effect from original advancement")


if __name__=="__main__":
    try:main()
    except (ValueError,OSError,TimeoutError,ConnectionError,
            subprocess.SubprocessError) as exc:
        print("Copied Drehmal protocol player inspection BLOCKED: "+str(exc),file=sys.stderr)
        sys.exit(2)

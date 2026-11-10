#!/usr/bin/env python3
"""Single disposable 7+9 server: real protocol right-click vs lever state."""
from __future__ import annotations
import argparse
import json
import os
import secrets
import shutil
import signal
import subprocess
import sys
import time
from pathlib import Path

import preview_drehmal_primordial_root_ninth as nine
import probe_drehmal_start_runtime as srv
from audit_drehmal_starter_mechanisms import World,palette_index

LEVERS={
 "foundry_1":(-3128,149,1490),
 "foundry_2":(-3035,122,1185),
 "foundry_3":(-3394,146,1081),
}
MARKER=".turnbound-disposable-foundry-protocol"

def powered(world,pos):
    x,y,z=pos
    root=World(world).chunk(x,z)
    sec=next((s for s in root["sections"] if s.get("Y")==y//16),None)
    if sec is None:raise ValueError("Lever chunk section missing")
    palette=sec["block_states"]
    idx=((y&15)<<8)|((z&15)<<4)|(x&15)
    block=palette["palette"][palette_index(palette["palette"],palette.get("data"),idx)]
    if block.get("Name")!="minecraft:lever" or block.get("Properties",{}).get("powered") not in ("true","false"):
        raise ValueError("Pinned Foundry lever no longer matches original map geometry")
    return block["Properties"]["powered"]=="true"

def stand(world,pos):
    view=World(world)
    x,y,z=pos
    candidates=[]
    for h in (y,y-1,y+1):
      for dx in (-1,0,1,-2,2):
       for dz in (-1,0,1,-2,2):
        if dx==dz==0:continue
        at=(x+dx,h,z+dz)
        air=lambda p:view.state(p)["id"] in ("minecraft:air","minecraft:cave_air")
        if air(at) and air((at[0],at[1]+1,at[2])) and not air((at[0],at[1]-1,at[2])):
            candidates.append(((dx*dx+dz*dz+(h-y)**2),at))
    if not candidates:raise ValueError("No nearby safe standing position for Foundry lever")
    candidates.sort()
    _,(sx,sy,sz)=candidates[0]
    return (sx+0.5,sy,sz+0.5)

def probe(project,world):
    if (world/"turnbound-world.marker").exists():raise ValueError("Active world forbidden")
    if not nine.verify(world)["verified"]:raise ValueError("7+9 receipts not valid")
    for pos in LEVERS.values():powered(world,pos)
    run=project/"run"
    if run.exists():
        if not (run/MARKER).is_file():raise ValueError("Refusing to overwrite non-CI run directory")
        shutil.rmtree(run)
    run.mkdir()
    (run/MARKER).write_text("disposable\n")
    (run/"eula.txt").write_text("eula=true\n")
    secret=secrets.token_hex(18)
    settings=[
      "server-ip=127.0.0.1","server-port=25580",
      "enable-rcon=true","rcon.port=25581","rcon.password="+secret,
      "online-mode=false","white-list=false","enable-command-block=true",
      "max-players=4","spawn-protection=0","max-tick-time=-1",
      "view-distance=3","simulation-distance=3","gamemode=creative",
      "difficulty=normal","motd=Disposable Drehmal lever protocol test"
    ]
    (run/"server.properties").write_text("\n".join(settings)+"\n")
    project.joinpath("build").mkdir(exist_ok=True)
    with (project/"build"/"foundry-protocol-private.log").open("w") as log:
      server=subprocess.Popen(["gradle","runServer","--no-daemon",
         "--args=--nogui --universe "+str(world.parent)+" --world "+world.name],
         cwd=project,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
      rcon=None;node=None;botlog=None
      try:
        rcon=srv.wait_rcon(server,secret)
        if "file/hi_drehmal.zip" not in rcon.command("datapack list enabled"):
            raise ValueError("Original mapped datapack inactive")
        if not os.getenv("TURNBOUND_PROBE_NODE_MODULES"):
            raise ValueError("Missing pinned protocol modules")
        botlog=(project/"build"/"foundry-bot-private.log").open("w")
        node=subprocess.Popen(["node","tools/probe_drehmal_foundry_block_use.cjs"],
           cwd=project,stdin=subprocess.PIPE,stdout=botlog,
           stderr=subprocess.STDOUT,start_new_session=True,text=True)
        limit=time.monotonic()+65
        while "DrehmalAliceCI" not in rcon.command("list"):
            if time.monotonic()>limit or node.poll() is not None:
                raise ValueError("Protocol player did not join")
            time.sleep(0.5)
        observations=[]
        for label,pos in LEVERS.items():
            x,y,z=pos
            rcon.command(f"forceload add {x-8} {z-8} {x+8} {z+8}")
            sx,sy,sz=stand(world,pos)
            rcon.command("gamemode creative DrehmalAliceCI")
            reply=rcon.command(f"tp DrehmalAliceCI {sx} {sy} {sz}")
            if "teleported" not in reply.lower():raise ValueError("Failed to place click client by real lever")
            time.sleep(0.75)
            start=powered(world,pos)
            states=[start]
            for _ in range(2):
                if node.poll() is not None:raise ValueError("Client exited during click")
                node.stdin.write(json.dumps({"action":"click","position":[*pos]})+"\n")
                node.stdin.flush()
                time.sleep(0.9)
                rcon.command("save-all flush")
                states.append(powered(world,pos))
            observations.append({"facility":label,"packet_sent":True,
                 "first_click_toggled":states[1]!=states[0],
                 "second_click_restored":states[2]==states[0],
                 "actual_original_door_opened_verified":False})
            if states[1]==states[0] or states[2]!=states[0]:
                raise ValueError("Physical block-use packet did not toggle original lever at "+label)
        rcon.command("stop")
        server.wait(timeout=90)
        if server.returncode!=0:raise ValueError("Drehmal copied server abnormal exit")
        return {"status":"OFFICIAL_7_PLUS_9_FOUNDRY_LEVER_REAL_PROTOCOL_CLICK_OBSERVED",
          "two_packets_each":True,"command_blocks_enabled":True,
          "original_datapack_enabled":True,"observations":observations,
          "lever_tests":len(observations),"rendered_client_playtested":False,
          "original_combat_gameplay_preserved":False,
          "door_opening_verified":False,"map_separation_complete":False}
      finally:
        if node and node.poll() is None:
            os.killpg(node.pid,signal.SIGTERM)
            try:node.wait(timeout=7)
            except subprocess.TimeoutExpired:os.killpg(node.pid,signal.SIGKILL);node.wait(timeout=8)
        if botlog:botlog.close()
        if rcon:rcon.close()
        if server.poll() is None:
            os.killpg(server.pid,signal.SIGTERM)
            try:server.wait(timeout=12)
            except subprocess.TimeoutExpired:os.killpg(server.pid,signal.SIGKILL);server.wait(timeout=8)

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--project",required=True,type=Path)
    p.add_argument("--world-copy",required=True,type=Path)
    p.add_argument("--output",required=True,type=Path)
    a=p.parse_args()
    world=a.world_copy.resolve(strict=True);out=a.output.resolve()
    if out==world or world in out.parents:p.error("Output cannot be inside the original copied world")
    result=probe(a.project.resolve(strict=True),world)
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(result,indent=2)+"\n")
    print(json.dumps({"status":result["status"],"lever_tests":result["lever_tests"],
                      "map_separation_complete":False}))

if __name__=="__main__":
    try:main()
    except (ValueError,KeyError,TypeError,OSError,TimeoutError,ConnectionError,
            subprocess.SubprocessError) as e:
        print("Foundry physical click probe REFUSED: "+str(e),file=sys.stderr)
        sys.exit(2)

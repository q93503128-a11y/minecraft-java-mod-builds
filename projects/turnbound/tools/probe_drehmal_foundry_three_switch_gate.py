#!/usr/bin/env python3
"""Three concurrently powered Foundry levers and entrance block deltas on disposable 7+9 copy. Not a playable door/wave approval."""
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


# The official wiki point is approximate, not the actual door coordinates.
# This envelope is a diagnostic measurement, not a gameplay navigation test.
GATE_HINT=(-3276,160,1576)
GATE_XZ_RADIUS=8
GATE_Y_RADIUS=10
BARRIER_IDS=frozenset(("minecraft:basalt","minecraft:polished_basalt",
                       "minecraft:smooth_basalt","minecraft:bedrock"))

def gate_snapshot(world):
    from audit_drehmal_starter_mechanisms import World,palette_index
    view=World(world)
    cx,cy,cz=GATE_HINT
    states={}
    for x in range(cx-GATE_XZ_RADIUS,cx+GATE_XZ_RADIUS+1):
        for z in range(cz-GATE_XZ_RADIUS,cz+GATE_XZ_RADIUS+1):
            for y in range(cy-GATE_Y_RADIUS,cy+GATE_Y_RADIUS+1):
                root=view.chunk(x,z)
                section=next((s for s in root.get("sections",[]) if s.get("Y")==y//16),None)
                if section is None or not section.get("block_states"):
                    continue
                pal=section["block_states"]
                index=((y&15)<<8)|((z&15)<<4)|(x&15)
                item=pal["palette"][palette_index(pal["palette"],pal.get("data"),index)]
                name=item["Name"]
                if name not in ("minecraft:air","minecraft:cave_air","minecraft:void_air"):
                    states[(x,y,z)]=name
    return states

def gate_diff(before,after):
    changed=[]
    for p in before.keys()|after.keys():
        a=before.get(p,"minecraft:air")
        b=after.get(p,"minecraft:air")
        if a!=b:changed.append((p,a,b))
    removed=[(p,a,b) for p,a,b in changed if a in BARRIER_IDS and b=="minecraft:air"]
    direct=[(p,a,b) for p,a,b in changed if
            -3278<=p[0]<=-3274 and 156<=p[1]<=164 and 1576<=p[2]<=1577]
    return {
        "original_clone_destination_min":[-3278,156,1576],
        "original_clone_destination_max":[-3274,164,1577],
        "original_clone_destination_changed_blocks":len(direct),
        "original_clone_destination_sample":[{"position":list(p),"before":a,"after":b}
                                             for p,a,b in sorted(direct)[:20]],
        "wiki_approximate_gate_center":list(GATE_HINT),
        "x_z_radius":GATE_XZ_RADIUS,"y_radius":GATE_Y_RADIUS,
        "block_count_compared":(2*GATE_XZ_RADIUS+1)**2*(2*GATE_Y_RADIUS+1),
        "changed_block_ids":len(changed),
        "basalt_or_bedrock_removed_to_air":len(removed),
        "removed_barrier_positions":[list(p) for p,_,_ in sorted(removed)[:30]],
        "other_changes":[
            {"position":list(p),"from":a,"to":b}
            for p,a,b in sorted(changed)[:15]
        ],
        "actual_foundry_door_traversal_verified":False,
        "causality_without_independent_control_proven":False,
    }

def foundry_function_inventory(pack):
    # Read-only metadata about original scripts. Never publish their contents.
    import zipfile,collections
    totals=collections.Counter()
    names=[]
    with zipfile.ZipFile(pack) as z:
        entries=z.namelist()
        if len(entries)!=len(set(entries)):
            raise ValueError("Source datapack ZIP duplicate members")
        for item in entries:
            if not (item.startswith("data/") and item.endswith(".mcfunction")):
                continue
            if "foundry" not in item.lower() and "valhalla" not in item.lower():
                continue
            names.append(item)
            body=z.read(item)
            if len(body)>2_000_000:raise ValueError("Overlarge original source function")
            for line in body.decode("utf-8").splitlines():
                line=line.strip().lstrip("/")
                if not line or line.startswith("#"):continue
                op=line.split(maxsplit=1)[0].lower()
                totals[op]+=1
                for category in ("summon ","schedule ","scoreboard ","advancement ",
                                 "setblock ","fill ","clone "):
                    if category in line.lower():
                        totals["contains_"+category.rstrip()]+=1
        wave=[name.replace("data/","",1).replace("/functions/",":",1).removesuffix(".mcfunction")
              for name in names if "wave" in name.lower() or "arena" in name.lower()]
    return {
        "original_foundry_named_script_count":len(names),
        "static_command_categories":dict(sorted(totals.items())),
        "wave_or_arena_named_function_ids_sample":sorted(wave)[:32],
        "wave_or_arena_named_function_ids_total":len(wave),
        "identified_actual_wave_activation":False,
        "six_wave_orig_gameplay_isolated":False,
        "original_function_content_exported":False,
    }


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
        # Load the entrance and every remote control concurrently.
        cx,cy,cz=GATE_HINT
        rcon.command(f"forceload add {cx-GATE_XZ_RADIUS} {cz-GATE_XZ_RADIUS} {cx+GATE_XZ_RADIUS} {cz+GATE_XZ_RADIUS}")
        for x,y,z in LEVERS.values():
            rcon.command(f"forceload add {x-8} {z-8} {x+8} {z+8}")
        time.sleep(1.5)
        # The original levers/door clone reads source around (-3274,134,1557)
        # and writes target (-3278..-3274,156..164,1576..1577).
        # Explicitly load BOTH source and target chunks before activation.
        rcon.command("forceload add -3274 1557 -3270 1558")
        rcon.command("save-all flush")
        before=gate_snapshot(world)
        source_inventory=foundry_function_inventory(world/nine.PACK)
        observations=[]
        for label,pos in LEVERS.items():
            first=powered(world,pos)
            if not first:
                sx,sy,sz=stand(world,pos)
                rcon.command("gamemode creative DrehmalAliceCI")
                reply=rcon.command(f"tp DrehmalAliceCI {sx} {sy} {sz}")
                if "teleported" not in reply.lower():
                    raise ValueError("Server rejected movement near physical Foundry lever")
                time.sleep(0.8)
                if node.poll() is not None:raise ValueError("Client exited during click")
                node.stdin.write(json.dumps({"action":"click","position":[*pos]})+"\n")
                node.stdin.flush()
                time.sleep(1.1)
            rcon.command("save-all flush")
            final=powered(world,pos)
            observations.append({
                "site":label,"initial_powered":first,
                "click_sent":not first,"powered_after_activation":final,
            })
            if not final:raise ValueError("Failed to power original Foundry lever "+label)
        time.sleep(5)
        rcon.command("save-all flush")
        after=gate_snapshot(world)
        delta=gate_diff(before,after)
        all_powered=all(powered(world,p) for p in LEVERS.values())
        if not all_powered:
            raise ValueError("Three original levers did not remain powered concurrently")
        rcon.command("stop")
        server.wait(timeout=90)
        if server.returncode!=0:raise ValueError("Drehmal copied server abnormal exit")
        return {
            "status":"OFFICIAL_7_PLUS_9_THREE_FOUNDRY_SWITCHES_CONCURRENT",
            "lever_tests":len(observations),
            "all_three_powered":all_powered,
            "observations":observations,
            "entry_gate_block_differential":delta,
            "foundry_static_wave_catalog":source_inventory,
            "original_datapack_enabled":True,
            "command_blocks_enabled":True,
            "rendered_client_playtested":False,
            "original_wave_gameplay_disabled":False,
            "door_opened_verified":False,
            "map_separation_complete":False,
        }

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
    print(json.dumps({"status":result["status"],"lever_tests":result["lever_tests"],"all_three_powered":result["all_three_powered"],
                      "door_area_changed":result["entry_gate_block_differential"]["changed_block_ids"],
                      "map_separation_complete":False}))

if __name__=="__main__":
    try:main()
    except (ValueError,KeyError,TypeError,OSError,TimeoutError,ConnectionError,
            subprocess.SubprocessError) as e:
        print("Foundry physical click probe REFUSED: "+str(e),file=sys.stderr)
        sys.exit(2)

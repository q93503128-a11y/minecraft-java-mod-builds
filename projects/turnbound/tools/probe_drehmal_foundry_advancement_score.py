#!/usr/bin/env python3
"""Foundry natural clicks, saved advancement progress, and exact scoreboard gate conditions on a disposable 7+9 copy."""
from __future__ import annotations
import argparse
import json
import hashlib
import re
import uuid
import zipfile
import os
import secrets
import shutil
import signal
import subprocess
import sys
import time
from pathlib import Path
from probe_drehmal_facility_callback_runtime import scoreboard_values, query_position
from inspect_drehmal_foundry_main_baseline import source_destination_comparison, function_inventory

import preview_drehmal_primordial_root_ninth as nine
import probe_drehmal_start_runtime as srv
from audit_drehmal_starter_mechanisms import World,palette_index

LEVERS={
 "foundry_1":(-3128,149,1490),
 "foundry_2":(-3035,122,1185),
 "foundry_3":(-3394,146,1081),
}
MARKER=".turnbound-disposable-foundry-protocol"
GATE_PRESENCE_CENTER=(-3276,159,1570)
GATE_PRESENCE_MAX_RADIUS=12

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



def changed_score_entries(before,after):
    # Only publish aggregate record count, never original objective/owner names.
    return sum(before.get(k)!=after.get(k) for k in before.keys() | after.keys())


ADVANCEMENTS=("foundry:1","foundry:2","foundry:3")
SCORE_MATCH=re.compile(r"\b(if|unless)\s+score\s+(\S+)\s+(\S+)\s+matches\s+(-?\d+)(?:\.\.(-?\d+)?)?")

def parse_main_clauses(text):
    clauses=[]
    for line in text.splitlines():
        line=line.strip()
        if not line or line.startswith("#"):
            continue
        for match in SCORE_MATCH.finditer(line):
            polarity,owner,objective,lower,upper=match.groups()
            clauses.append({"clause":"C"+str(len(clauses)+1),
                            "polarity":polarity,"owner":owner,"objective":objective,
                            "lower":int(lower),"upper":int(upper or lower)})
    if len(clauses)!=3 or {x["lower"] for x in clauses}!={1,2,3}:
        raise ValueError("Pinned Foundry main conditions changed; refusing ambiguous score interpretation")
    if any(x["upper"]<x["lower"] for x in clauses):
        raise ValueError("Malformed original Foundry main score range")
    return clauses

def original_conditions_and_rewards(pack):
    # Read original copy in-memory only. No original commands in public reports.
    with zipfile.ZipFile(pack) as archive:
        path="data/foundry/functions/levers/main.mcfunction"
        lines=archive.read(path).decode("utf-8")
        clauses=parse_main_clauses(lines)
        rewards={}
        for ident in ADVANCEMENTS:
            member="data/foundry/advancements/"+ident.split(":")[1]+".json"
            data=json.loads(archive.read(member))
            if not isinstance(data.get("criteria"),dict) or not data["criteria"]:
                raise ValueError("Malformed original Foundry advancement")
            reward=data.get("rewards",{})
            rewards[ident]={"reward_function":reward.get("function") if isinstance(reward,dict) else None,
                            "criterion_count":len(data["criteria"])}
    return clauses,rewards

def advancement_file(world, player="DrehmalAliceCI"):
    # Vanilla offline-mode UUID.nameUUIDFromBytes("OfflinePlayer:"+name).
    name=uuid.UUID(bytes=hashlib.md5(("OfflinePlayer:"+player).encode()).digest(),version=3)
    return world/"advancements"/(str(name)+".json")

def advancement_progress(world):
    path=advancement_file(world)
    if not path.is_file():
        return {"advancement_file_observed":False,
                "progress":{ident:{"present":False,"completed":None,"criterion_entries":None}
                            for ident in ADVANCEMENTS}}
    data=json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data,dict):
        raise ValueError("Malformed copied-world advancement data")
    progress={}
    for ident in ADVANCEMENTS:
        row=data.get(ident)
        if row is not None and not isinstance(row,dict):
            raise ValueError("Malformed original Foundry advancement progress")
        progress[ident]={"present":row is not None,
                         "completed":row.get("done") if row is not None else False,
                         "criterion_entries":len(row.get("criteria",{})) if row is not None else 0}
    return {"advancement_file_observed":True,"progress":progress}

def evaluate_main_scores(scores, clauses, player="DrehmalAliceCI"):
    values=[]
    for row in clauses:
        owner=row["owner"]
        if owner=="@s":
            resolved=player
            resolution="player_execution_context_hypothesis"
        elif owner.startswith("@"):
            resolved=None
            resolution="selector_unresolved"
        else:
            resolved=owner
            resolution="literal_owner"
        value=scores.get((resolved,row["objective"])) if resolved is not None else None
        matched=value is not None and row["lower"]<=value<=row["upper"]
        if row["polarity"]=="unless":
            matched=not matched if value is not None else None
        values.append({"clause":row["clause"],"polarity":row["polarity"],
                       "required_match":[row["lower"],row["upper"]],
                       "resolution":resolution,
                       "observed_score":value,
                       "score_record_present":value is not None,
                       "would_match_in_assumed_context":matched})
    return values

def no_click_snapshot(world, clauses):
    return {"advancements":advancement_progress(world),
            "main_scores":evaluate_main_scores(scoreboard_values(world),clauses)}



def original_door_entity_guards(pack):
    """Internal-only original selector source for positive/negative RCON predicate tests."""
    with zipfile.ZipFile(pack) as archive:
        item="data/players/functions/overworld_p.mcfunction"
        if item not in archive.namelist():
            raise ValueError("Original Foundry door controller missing")
        matches=[]
        for raw in archive.read(item).decode("utf-8").splitlines():
            line=raw.strip().lstrip("/")
            if not re.search(r"\bfunction\s+foundry:levers/door(?![a-z0-9_./-])",line):
                continue
            full=re.search(r"\bif entity\s+(@s\[[^\]]+\])",line)
            if full is None:
                raise ValueError("Original door entity check changed shape")
            matches.append(full.group(1))
        if len(matches)!=1:
            raise ValueError("Original Foundry door predicate call count drift")
        full_selector=matches[0]
        plain_selector="@s["+",".join(piece for piece in full_selector[3:-1].split(",")
                                     if not piece.startswith("predicate="))+"]"
        if plain_selector==full_selector:
            raise ValueError("No original predicate key to isolate")
        m=re.search(r"(?:\[|,)predicate=([a-z0-9_.-]+:[a-z0-9_./-]+)",full_selector)
        if not m:
            raise ValueError("Cannot classify original predicate resource ID")
        resource=m.group(1)
        ns,name=resource.split(":",1)
        member="data/"+ns+"/predicates/"+name+".json"
        predicate_shape={"resource_id":resource,"present_in_original_pack":member in archive.namelist()}
        if member in archive.namelist():
            data=json.loads(archive.read(member))
            if not isinstance(data,dict):
                raise ValueError("Malformed original predicate resource")
            predicate_shape["top_level_condition_type"]=data.get("condition")
            predicate_shape["top_level_keys"]=sorted(data.keys())
            predicate_shape["has_nested_entity_predicate"]=isinstance(data.get("predicate"),dict)
            if isinstance(data.get("predicate"),dict):
                predicate_shape["nested_predicate_keys"]=sorted(data["predicate"].keys())
        return full_selector,plain_selector,predicate_shape


UPSTREAM_EDGES=(
    ("players:main","players:overworld"),
    ("players:overworld","players:overworld_p"),
)
UPSTREAM_GUARD=re.compile(r"(if|unless)\s+(predicate|entity)\s+(\S+)")

def original_upstream_guard_clauses(pack):
    """Read 2 original function callsites, never execute/export original lines.

    Deliberately rejects new syntax, unexpected modifiers or ambiguous caller
    counts rather than silently evaluating only part of a changed condition.
    """
    result=[]
    with zipfile.ZipFile(pack) as archive:
        for caller,callee in UPSTREAM_EDGES:
            ns,rel=caller.split(":",1)
            member="data/"+ns+"/functions/"+rel+".mcfunction"
            if member not in archive.namelist():
                raise ValueError("Missing original upstream route")
            lines=[x.strip().lstrip("/") for x in archive.read(member).decode("utf-8").splitlines()]
            matching=[line for line in lines
                      if line and not line.startswith("#") and
                      re.search(r"\bfunction\s+"+re.escape(callee)+r"(?![a-z0-9_./-])",line)]
            if len(matching)!=1:
                raise ValueError("Original upstream callsite count changed")
            raw=matching[0]
            shape=re.fullmatch(
                r"execute((?:\s+(?:if|unless)\s+(?:predicate|entity)\s+\S+)+)"
                r"\s+run\s+function\s+"+re.escape(callee),raw)
            if shape is None:
                raise ValueError("Unsupported original upstream guard modifiers or syntax")
            clauses=UPSTREAM_GUARD.findall(shape.group(1))
            if not clauses:
                raise ValueError("Original upstream route has no conditions")
            result.append({"caller":caller,"callee":callee,
                           "clauses":[{"polarity":polarity,"condition_type":kind,
                                       "literal_private":arg}
                                      for polarity,kind,arg in clauses]})
    return result


def check_upstream_guard(rcon,world,polarity,kind,arg,objective):
    if polarity not in ("if","unless") or kind not in ("predicate","entity"):
        raise ValueError("Unsupported original upstream clause")
    rcon.command(f"execute as DrehmalAliceCI at @s store success score #upstream_guard {objective} run execute {polarity} {kind} {arg}")
    rcon.command("save-all flush")
    observed=scoreboard_values(world).get(("#upstream_guard",objective))
    if observed not in (0,1):
        raise ValueError("Cannot measure original upstream condition from copied-world scoreboard")
    return bool(observed)


def check_door_selector_guard(rcon,world,selector,objective):
    # The command only evaluates a vanilla entity selector and stores 0/1 in
    # a newly-created diagnostic objective; no original function is invoked.
    rcon.command(f"execute as DrehmalAliceCI at @s store success score #door_guard {objective} run execute if entity {selector}")
    rcon.command("save-all flush")
    result=scoreboard_values(world).get(("#door_guard",objective))
    if result not in (0,1):
        raise ValueError("Cannot prove original selector truth value from exact copied scoreboard")
    return bool(result)


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
        baseline=source_destination_comparison(world)
        if baseline["count"]!=90 or baseline["source_destination_fullstate_differences"]<1:
            raise ValueError("Original gate clone has no distinct closed-state target")
        before=gate_snapshot(world)
        source_inventory=foundry_function_inventory(world/nine.PACK)
        clauses,rewards=original_conditions_and_rewards(world/nine.PACK)
        rcon.command("save-all flush")
        before_control=no_click_snapshot(world,clauses)
        before_control_scores=scoreboard_values(world)
        # No block-use packets during this interval: distinguish tick-side changes.
        time.sleep(3)
        rcon.command("save-all flush")
        after_control=no_click_snapshot(world,clauses)
        background_score_changes=changed_score_entries(before_control_scores,scoreboard_values(world))
        observations=[]
        for label,pos in LEVERS.items():
            first=powered(world,pos)
            rcon.command("save-all flush")
            scores_before=scoreboard_values(world)
            advancement_before=advancement_progress(world)
            main_before=evaluate_main_scores(scores_before,clauses)
            if not first:
                sx,sy,sz=stand(world,pos)
                rcon.command("gamemode creative DrehmalAliceCI")
                equipped=rcon.command("item replace entity DrehmalAliceCI weapon.mainhand with minecraft:stick 1")
                if any(word in equipped.lower() for word in ("unknown","error","failed")):
                    raise ValueError("Cannot equip real held item in synthetic client")
                reply=rcon.command(f"tp DrehmalAliceCI {sx} {sy} {sz}")
                if "teleported" not in reply.lower():
                    raise ValueError("Server rejected movement near physical Foundry lever")
                time.sleep(0.8)
                if node.poll() is not None:raise ValueError("Client exited during click")
                node.stdin.write(json.dumps({"action":"click","position":[*pos]})+"\n")
                node.stdin.flush()
                time.sleep(1.1)
            rcon.command("save-all flush")
            scores_after=scoreboard_values(world)
            advancement_after=advancement_progress(world)
            main_after=evaluate_main_scores(scores_after,clauses)
            final=powered(world,pos)
            observations.append({
                "real_hand_item_equipped":True,
                "original_scoreboard_changed_entries":changed_score_entries(scores_before,scores_after),
                "site":label,"initial_powered":first,
                "click_sent":not first,"powered_after_activation":final,
                "advancement_id":"foundry:"+label[-1],
                "advancement_before":advancement_before["progress"]["foundry:"+label[-1]],
                "advancement_after":advancement_after["progress"]["foundry:"+label[-1]],
                "advancement_file_observed_after":advancement_after["advancement_file_observed"],
                "main_scores_before":main_before,
                "main_scores_after":main_after,
            })
            if not final:raise ValueError("Failed to power original Foundry lever "+label)
        time.sleep(5)
        rcon.command("save-all flush")
        after=gate_snapshot(world)
        delta=gate_diff(before,after)
        all_powered=all(powered(world,p) for p in LEVERS.values())
        if not all_powered:
            raise ValueError("Three original levers did not remain powered concurrently")
        natural_state=source_destination_comparison(world)
        # No explicit function calls, advancement grants/revokes, or source edits.
        # A final no-click interval detects tick-driven scoreboard/advancement changes.
        rcon.command("save-all flush")
        post_control_begin=no_click_snapshot(world,clauses)
        post_control_scores=scoreboard_values(world)
        time.sleep(3)
        rcon.command("save-all flush")
        post_control_end=no_click_snapshot(world,clauses)
        post_control_background_changes=changed_score_entries(post_control_scores,scoreboard_values(world))
        # New, separate physical hypothesis: original players:overworld_p requires
        # score == 3 AND @s near a gate-centered 12 block predicate region.
        # No original door function or advancement command is invoked here.
        position_center=GATE_PRESENCE_CENTER
        candidate=None
        for dy in (0,-1,1,-2,2,-3,3):
            try:
                candidate=stand(world,(position_center[0],position_center[1]+dy,position_center[2]))
                break
            except ValueError:
                pass
        if candidate is None:
            raise ValueError("No safe natural-player test position near original Foundry gate predicate center")
        if sum((candidate[i]-position_center[i])**2 for i in range(3))>=GATE_PRESENCE_MAX_RADIUS**2:
            raise ValueError("Cannot place natural protocol player within original gate distance predicate")
        pre_presence=source_destination_comparison(world)
        pre_presence_snapshot=gate_snapshot(world)
        sx,sy,sz=candidate
        moved=rcon.command(f"tp DrehmalAliceCI {sx} {sy} {sz}")
        if "teleported" not in moved.lower():
            raise ValueError("Natural presence probe teleport was not accepted")
        time.sleep(3)
        rcon.command("save-all flush")
        actual_pos=query_position(rcon,"DrehmalAliceCI")
        if sum((actual_pos[i]-position_center[i])**2 for i in range(3))>=GATE_PRESENCE_MAX_RADIUS**2:
            raise ValueError("Original controller displaced the test player outside the gate predicate radius")
        post_presence=source_destination_comparison(world)
        post_presence_snapshot=gate_snapshot(world)
        presence_delta=gate_diff(pre_presence_snapshot,post_presence_snapshot)
        after_proximity_all_powered=all(powered(world,p) for p in LEVERS.values())
        if not after_proximity_all_powered:
            raise ValueError("Foundry lever state regressed during natural proximity intervention")
        presence_observation={
            "test_method":"natural_player_presence_after_three_real_lever_clicks",
            "reference_center":list(position_center),
            "reference_radius":GATE_PRESENCE_MAX_RADIUS,
            "safe_player_teleport_accepted":True,
            "player_within_reference_radius":True,
            "three_levers_still_powered":True,
            "source_dest_differences_before":pre_presence["source_destination_fullstate_differences"],
            "source_dest_differences_after":post_presence["source_destination_fullstate_differences"],
            "exact_clone_target_id_changes_after_arrival":presence_delta["original_clone_destination_changed_blocks"],
            "original_function_explicitly_invoked":False,
            "predicate_resource_condition_individually_verified":False,
            "door_passage_playtested":False,
        }
        # Compare original location-only selector with the *same* selector
        # containing its original predicate. Never run the door function.
        full_selector,plain_selector,predicate_shape=original_door_entity_guards(world/nine.PACK)
        private_objective="tb_gate_"+secrets.token_hex(3)
        objective_add=rcon.command(f"scoreboard objectives add {private_objective} dummy")
        if "already exists" in objective_add.lower() or "error" in objective_add.lower():
            raise ValueError("Disposable diagnostic scoreboard could not be isolated")
        try:
            without_predicate=check_door_selector_guard(rcon,world,plain_selector,private_objective)
            with_predicate=check_door_selector_guard(rcon,world,full_selector,private_objective)
            upstream_conditions=[]
            for edge in original_upstream_guard_clauses(world/nine.PACK):
                evaluations=[]
                for clause in edge["clauses"]:
                    passed=check_upstream_guard(
                        rcon,world,clause["polarity"],clause["condition_type"],
                        clause["literal_private"],private_objective)
                    evaluations.append({
                        "condition_type":clause["condition_type"],
                        "polarity":clause["polarity"],
                        "passed_in_player_context":passed,
                        "original_literal_exported":False,
                    })
                upstream_conditions.append({
                    "caller":edge["caller"],"callee":edge["callee"],
                    "conditions":evaluations,
                    "all_guards_passed":all(x["passed_in_player_context"] for x in evaluations),
                })
        finally:
            rcon.command(f"scoreboard objectives remove {private_objective}")
        presence_observation["selector_comparison"]={
            "distance_and_coordinate_selector_passed":without_predicate,
            "full_original_predicate_selector_passed":with_predicate,
            "original_predicate_resource_metadata":predicate_shape,
            "original_door_command_invoked":False,
            "original_selector_text_exported":False,
            "diagnostic_score_objective_removed":True,
        }
        presence_observation["upstream_route_guard_comparison"]={
            "evaluated_as":"protocol_player_at_natural_foundry_gate_position",
            "callsite_evaluations":upstream_conditions,
            "original_functions_explicitly_invoked":False,
            "original_guard_literals_exported":False,
        }
        rcon.command("stop")
        server.wait(timeout=90)
        if server.returncode!=0:raise ValueError("Drehmal copied server abnormal exit")
        return {
            "status":"OFFICIAL_7_PLUS_9_FOUNDRY_NATURAL_ADVANCEMENT_AND_SCORE_TRACE",
            "lever_tests":len(observations),
            "all_three_powered":all_powered,
            "observations":observations,
            "no_click_control_before":{
                "before":before_control,"after":after_control,
                "background_score_changed_entries":background_score_changes},
            "no_click_control_after":{
                "before":post_control_begin,"after":post_control_end,
                "background_score_changed_entries":post_control_background_changes},
            "natural_gate_proximity":presence_observation,
            "original_advancement_reward_links":rewards,
            "main_score_conditions":[{"clause":r["clause"],"polarity":r["polarity"],
                                      "required_match":[r["lower"],r["upper"]],
                                      "resolution":"selector" if r["owner"].startswith("@") else "literal",
                                      "owner_or_objective_exported":False}
                                     for r in clauses],
            "original_foundry_named_script_count":source_inventory["original_foundry_named_script_count"],
            "original_lever_function_graph":function_inventory(world/nine.PACK),
            "entry_gate_block_differential":delta,
            "gate_exact_90_blocks":{
                "baseline_source_destination_differences":baseline["source_destination_fullstate_differences"],
                "after_natural_three_levers_differences":natural_state["source_destination_fullstate_differences"],
                "natural_clone_target_id_changes":delta["original_clone_destination_changed_blocks"],
                "explicit_original_door_function_invoked":False,
            },
            "original_datapack_enabled":True,"command_blocks_enabled":True,
            "advancement_file_observed":any(x["advancement_file_observed_after"] for x in observations),
            "original_commands_or_nbt_exported":False,
            "natural_advancement_to_door_callback_proven":False,
            "rendered_client_playtested":False,
            "door_traversal_verified":False,
            "six_wave_non_reentry_verified":False,
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
                      "natural_door_target_id_changes":result["gate_exact_90_blocks"]["natural_clone_target_id_changes"],
                      "map_separation_complete":False}))

if __name__=="__main__":
    try:main()
    except (ValueError,KeyError,TypeError,OSError,TimeoutError,ConnectionError,
            subprocess.SubprocessError) as e:
        print("Foundry physical click probe REFUSED: "+str(e),file=sys.stderr)
        sys.exit(2)

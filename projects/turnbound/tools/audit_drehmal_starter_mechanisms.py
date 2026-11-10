#!/usr/bin/env python3
"""Read-only command-block chain/structure audit on a SHA-pinned Drehmal COPY.
This is NOT proof of gameplay safety. Never publish world bytes or commands.
"""
from __future__ import annotations
import argparse
import collections
import hashlib
import json
import sys
from pathlib import Path
import patch_drehmal_terminus_commands as anvil
import preview_drehmal_starter_discovery_commands as starter
from scan_drehmal_command_blocks import NBTReader, NBTError

DIRECTIONS={"north":(0,0,-1),"south":(0,0,1),"west":(-1,0,0),
            "east":(1,0,0),"up":(0,1,0),"down":(0,-1,0)}
COMMANDS={"minecraft:command_block","minecraft:chain_command_block",
          "minecraft:repeating_command_block"}
SIGNALS={"redstone_wire","redstone_block","repeater","comparator","observer",
         "lever","tripwire","tripwire_hook","target","daylight_detector",
         "sculk_sensor","calibrated_sculk_sensor","redstone_torch",
         "redstone_wall_torch","detector_rail","activator_rail","powered_rail"}
DEVICES={"piston","sticky_piston","dispenser","dropper","hopper",
         "command_block","chain_command_block","repeating_command_block",
         "structure_block","jigsaw","end_gateway","end_portal","nether_portal"}
def add(a,b): return tuple(x+y for x,y in zip(a,b))
def sub(a,b): return tuple(x-y for x,y in zip(a,b))
def iscmd(s): return s["id"] in COMMANDS
def kind(name):
    base=name.removeprefix("minecraft:")
    if base in SIGNALS or base.endswith("_button") or base.endswith("_pressure_plate"): return "signal"
    if (base in DEVICES or base.endswith("_door") or
            base.endswith("_trapdoor") or base.endswith("_fence_gate")): return "device"
    return None

class PaletteNBT(NBTReader):
    def value(self, tag, depth=0):
        if tag==12:
            count=self.numeric("i")
            if count<0 or count>8192: raise NBTError("Invalid 1.20.1 long-array length")
            raw=self.take(count*8)
            return tuple(int.from_bytes(raw[i:i+8],"big") for i in range(0,len(raw),8))
        return super().value(tag,depth)

def palette_index(palette, packed, index):
    if not palette: raise ValueError("No block palette")
    if len(palette)==1:
        if packed not in (None,(),[]): raise ValueError("Unexpected singleton block data")
        return 0
    bits=max(4,(len(palette)-1).bit_length())
    each=64//bits
    word,offset=divmod(index,each)
    if not isinstance(packed,tuple) or word>=len(packed):
        raise ValueError("Truncated paletted block state array")
    value=(packed[word]>>(offset*bits))&((1<<bits)-1)
    if value>=len(palette): raise ValueError("Paletted state out of range")
    return value

class World:
    def __init__(self,root): self.root=Path(root); self.cache={}
    def chunk(self,x,z):
        cx,cz=x//16,z//16
        if (cx,cz) in self.cache: return self.cache[cx,cz]
        path=self.root/"region"/f"r.{cx//32}.{cz//32}.mca"
        if not path.is_file() or path.is_symlink(): raise ValueError("Missing adjacent region")
        slot=(cx&31)+((cz&31)<<5)
        with path.open("rb") as f:
            f.seek(slot*4)
            entry=f.read(4)
            if len(entry)!=4: raise ValueError("Corrupt region header")
            start,n=int.from_bytes(entry[:3],"big"),entry[3]
            if start<2 or not n: raise ValueError("Missing adjacent chunk")
            f.seek(start*4096)
            length=f.read(4)
            if len(length)!=4: raise ValueError("Missing chunk size")
            size=int.from_bytes(length,"big")
            if size<2 or size>n*4096-4: raise ValueError("Invalid chunk allocation")
            payload=f.read(size)
            if len(payload)!=size: raise ValueError("Truncated chunk")
        _,raw=anvil.decompress(payload)
        root=PaletteNBT(raw).root()
        if root.get("xPos")!=cx or root.get("zPos")!=cz:
            raise ValueError("Region/chunk coordinates differ")
        self.cache[cx,cz]=root
        return root
    def state(self,pos):
        x,y,z=pos
        if y < -64 or y > 319:
            return {"id":"minecraft:void_air","facing":None,"conditional":False}
        root=self.chunk(x,z)
        section=next((s for s in root.get("sections",[])
                      if isinstance(s,dict) and s.get("Y")==y//16),None)
        if section is None or not section.get("block_states"):
            return {"id":"minecraft:air","facing":None,"conditional":False}
        blocks=section["block_states"]
        i=((y&15)<<8)|((z&15)<<4)|(x&15)
        raw=blocks["palette"][palette_index(blocks["palette"],blocks.get("data"),i)]
        prop=raw.get("Properties",{})
        return {"id":raw["Name"],"facing":prop.get("facing"),
                "conditional":prop.get("conditional")=="true"}
    def actor(self,pos):
        x,_,z=pos
        records=[e for e in self.chunk(x,z).get("block_entities",[])
                 if isinstance(e,dict) and tuple(e.get(k) for k in ("x","y","z"))==pos]
        if len(records)!=1: raise ValueError("Target block entity is missing or ambiguous")
        return records[0]

def directed(view,pos):
    here=view.state(pos)
    if not iscmd(here): raise ValueError("Command chain includes non-command")
    facing=DIRECTIONS.get(here["facing"])
    if facing is None: raise ValueError("Command block facing unavailable")
    nextpos=add(pos,facing)
    outgoing=[nextpos] if view.state(nextpos)["id"]=="minecraft:chain_command_block" else []
    incoming=[]
    for delta in DIRECTIONS.values():
        near=add(pos,delta)
        nearstate=view.state(near)
        if iscmd(nearstate) and DIRECTIONS.get(nearstate["facing"])==sub(pos,near):
            incoming.append(near)
    return incoming,outgoing

def chain(view,pos):
    pending=[pos]; visited=set(); edges=set()
    while pending and len(visited)<64:
        current=pending.pop()
        if current in visited: continue
        visited.add(current)
        incoming,outgoing=directed(view,current)
        for other in incoming:
            edges.add((other,current))
            if other not in visited: pending.append(other)
        for other in outgoing:
            edges.add((current,other))
            if other not in visited: pending.append(other)
    return len(visited),len(edges),bool(pending)

def vicinity(view,pos):
    counts=collections.Counter()
    for dx in range(-2,3):
        for dy in range(-2,3):
            for dz in range(-2,3):
                if (dx,dy,dz)==(0,0,0): continue
                state=view.state(add(pos,(dx,dy,dz)))
                label=kind(state["id"])
                if label: counts[label]+=1
                if max(abs(dx),abs(dy),abs(dz))==1 and iscmd(state):
                    counts["adjacent_command"]+=1
    return counts

def audit(world,targets):
    view=World(anvil.validate_world(Path(world)))
    entries=[]
    for region,slot,pos,command_hash,actor_id in sorted(targets):
        x,y,z=pos
        if (actor_id!="minecraft:command_block" or
                region!=f"region/r.{x//512}.{z//512}.mca" or
                slot!=((x//16)&31)+(((z//16)&31)<<5)):
            raise ValueError("Official coordinate/region identity mismatch")
        state=view.state(pos); actor=view.actor(pos)
        if not iscmd(state): raise ValueError("Verified target is not a command block")
        source=actor.get("Command")
        if (actor.get("id")!=actor_id or not isinstance(source,str) or
                hashlib.sha256(source.encode()).hexdigest()!=command_hash):
            raise ValueError("Physical NBT command hash differs")
        nodes,edges,truncated=chain(view,pos)
        near=vicinity(view,pos)
        entries.append({"coordinates":list(pos),"region_file":region,
                        "command_sha256":command_hash,
                        "block_type":state["id"],"facing":state["facing"],
                        "conditional":state["conditional"],"auto":actor.get("auto"),
                        "powered":actor.get("powered"),
                        "chain_nodes":nodes,"directed_chain_edges":edges,
                        "trace_truncated":truncated,
                        "adjacent_command_blocks":near["adjacent_command"],
                        "nearby_signals":near["signal"],
                        "nearby_devices":near["device"]})
    if len(entries)!=24 or len({e["region_file"] for e in entries})!=4:
        raise ValueError("Incomplete original source physical inspection")
    return {"status":"STATIC_PHYSICAL_AUDIT_NOT_PLAYTESTED",
            "source_level_sha256":starter.LEVEL_SHA,
            "source_report_sha256":starter.DIRECT_REPORT_SHA,
            "target_count":24,"region_count":4,
            "chain_connected_targets":sum(e["chain_nodes"]>1 for e in entries),
            "conditional_targets":sum(e["conditional"] for e in entries),
            "targets_near_signals":sum(e["nearby_signals"]>0 for e in entries),
            "truncated_chains":sum(e["trace_truncated"] for e in entries),
            "integration_safe_for_play":False,
            "world_modified":False,"original_code_reused":False,"playtested":False,
            "note":"Static block states, block entities and nearby wiring; mechanisms require runtime validation.",
            "targets":entries}

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--world-copy",required=True,type=Path)
    p.add_argument("--direct-report",required=True,type=Path)
    p.add_argument("--output",required=True,type=Path)
    a=p.parse_args()
    world=a.world_copy.resolve(strict=True); out=a.output.resolve()
    if world==out or world in out.parents: p.error("Output must be outside world")
    result=audit(world,starter.load_candidates(a.direct_report))
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(result,indent=2,ensure_ascii=False)+"\n")
    print(json.dumps({k:result[k] for k in
          ("status","target_count","chain_connected_targets",
           "conditional_targets","targets_near_signals","integration_safe_for_play")}))

if __name__=="__main__":
    try: main()
    except (ValueError,OSError,KeyError,TypeError,NBTError) as exc:
        print("Physical mechanism audit REFUSED: "+str(exc),file=sys.stderr)
        sys.exit(2)

#!/usr/bin/env python3
import gzip
import io
import json
import re
import struct
import urllib.request
import zipfile
from collections import deque
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CATALOG = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomSupplyCatalog.java"

ASSETS = {
    "house": "https://www.schemcraft.com/schematics/ultimate-all-in-one-survival-house/download",
    "manor": "https://www.schemcraft.com/schematics/ultimate-medieval-manor-survival-base/download",
    "castle_house": "https://www.schemcraft.com/schematics/ultimate-fantasy-starter-castle-house/download",
}

SKIP = {
    "minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:structure_void",
    "minecraft:grass_block", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:rooted_dirt",
    "minecraft:podzol", "minecraft:mycelium", "minecraft:moss_block", "minecraft:moss_carpet",
    "minecraft:farmland", "minecraft:dirt_path", "minecraft:mud", "minecraft:packed_mud",
    "minecraft:sand", "minecraft:red_sand", "minecraft:gravel", "minecraft:clay",
    "minecraft:water", "minecraft:lava", "minecraft:snow", "minecraft:snow_block",
    "minecraft:short_grass", "minecraft:tall_grass", "minecraft:fern", "minecraft:large_fern",
    "minecraft:dead_bush", "minecraft:rose_bush", "minecraft:peony", "minecraft:lilac",
    "minecraft:sunflower", "minecraft:dandelion", "minecraft:poppy", "minecraft:blue_orchid",
    "minecraft:allium", "minecraft:azure_bluet", "minecraft:oxeye_daisy", "minecraft:cornflower",
    "minecraft:lily_of_the_valley", "minecraft:wither_rose", "minecraft:pink_petals",
    "minecraft:azalea", "minecraft:flowering_azalea", "minecraft:vine", "minecraft:lily_pad",
    "minecraft:sugar_cane", "minecraft:bamboo", "minecraft:cactus", "minecraft:wheat",
    "minecraft:carrots", "minecraft:potatoes", "minecraft:beetroots", "minecraft:pumpkin_stem",
    "minecraft:melon_stem", "minecraft:sweet_berry_bush", "minecraft:cocoa",
}

class Reader:
    def __init__(self, data):
        self.f = io.BytesIO(data)
    def read(self, n):
        b = self.f.read(n)
        if len(b) != n:
            raise EOFError("truncated NBT")
        return b
    def u8(self): return self.read(1)[0]
    def i8(self): return struct.unpack(">b", self.read(1))[0]
    def i16(self): return struct.unpack(">h", self.read(2))[0]
    def u16(self): return struct.unpack(">H", self.read(2))[0]
    def i32(self): return struct.unpack(">i", self.read(4))[0]
    def i64(self): return struct.unpack(">q", self.read(8))[0]
    def f32(self): return struct.unpack(">f", self.read(4))[0]
    def f64(self): return struct.unpack(">d", self.read(8))[0]
    def string(self): return self.read(self.u16()).decode("utf-8")

def payload(r, tag):
    if tag == 1: return r.i8()
    if tag == 2: return r.i16()
    if tag == 3: return r.i32()
    if tag == 4: return r.i64()
    if tag == 5: return r.f32()
    if tag == 6: return r.f64()
    if tag == 7:
        n = r.i32(); return r.read(n)
    if tag == 8: return r.string()
    if tag == 9:
        child = r.u8(); n = r.i32(); return [payload(r, child) for _ in range(n)]
    if tag == 10: return compound(r)
    if tag == 11:
        n = r.i32(); return [r.i32() for _ in range(n)]
    if tag == 12:
        n = r.i32(); return [r.i64() for _ in range(n)]
    raise ValueError(f"unsupported NBT tag {tag}")

def compound(r):
    out = {}
    while True:
        tag = r.u8()
        if tag == 0: return out
        name = r.string()
        out[name] = payload(r, tag)

def parse_nbt(gz_bytes):
    raw = gzip.decompress(gz_bytes)
    r = Reader(raw)
    if r.u8() != 10:
        raise ValueError("root is not compound")
    r.string()
    root = compound(r)
    if isinstance(root.get("Schematic"), dict):
        root = root["Schematic"]
    return root

def decode_varints(data, expected):
    result = []
    offset = 0
    for _ in range(expected):
        value = 0; shift = 0
        while True:
            if offset >= len(data): raise EOFError("truncated varint data")
            cur = data[offset]; offset += 1
            value |= (cur & 0x7F) << shift
            if cur & 0x80 == 0: break
            shift += 7
            if shift > 28: raise ValueError("invalid varint")
        result.append(value)
    return result

def block_id(state):
    return state.split("[", 1)[0].strip()

def skipped(state):
    bid = block_id(state)
    return (bid in SKIP or bid.endswith("_leaves") or bid.endswith("_sapling")
            or bid.endswith("_tulip") or bid.endswith("_coral") or bid.endswith("_coral_fan"))

def load_template(style, url):
    request = urllib.request.Request(url, headers={"User-Agent": "LivingKingdomsAudit/1.0"})
    with urllib.request.urlopen(request, timeout=60) as response:
        archive = response.read()
    with zipfile.ZipFile(io.BytesIO(archive)) as zf:
        names = [n for n in zf.namelist() if n.lower().endswith(".schem") and not n.endswith("/")]
        if not names: raise RuntimeError(f"no schematic in {style}")
        schem = zf.read(names[0])
    root = parse_nbt(schem)
    version = int(root.get("Version", 1))
    width = int(root["Width"]); height = int(root["Height"]); length = int(root["Length"])
    blocks = root["Blocks"] if version >= 3 else root
    palette_map = blocks["Palette"]
    max_id = max(int(v) for v in palette_map.values())
    palette = ["minecraft:air"] * (max_id + 1)
    for state, pid in palette_map.items(): palette[int(pid)] = state
    encoded = blocks["Data"] if version >= 3 else root["BlockData"]
    ids = decode_varints(encoded, width * height * length)

    candidates = set()
    layer = width * length
    for idx, pid in enumerate(ids):
        if pid < 0 or pid >= len(palette) or skipped(palette[pid]): continue
        y, rem = divmod(idx, layer)
        z, x = divmod(rem, width)
        candidates.add((x, y, z))

    remaining = set(candidates)
    retained = set()
    while remaining:
        seed = remaining.pop()
        q = deque([seed]); component = [seed]
        while q:
            x, y, z = q.popleft()
            for nxt in ((x-1,y,z),(x+1,y,z),(x,y-1,z),(x,y+1,z),(x,y,z-1),(x,y,z+1)):
                if nxt in remaining:
                    remaining.remove(nxt); q.append(nxt); component.append(nxt)
        if len(component) >= 24:
            retained.update(component)
    if not retained: raise RuntimeError(f"empty retained structure for {style}")
    minx = min(p[0] for p in retained); maxx = max(p[0] for p in retained)
    miny = min(p[1] for p in retained); maxy = max(p[1] for p in retained)
    minz = min(p[2] for p in retained); maxz = max(p[2] for p in retained)
    normalized = [(x-minx, y-miny, z-minz) for x,y,z in retained]
    return {
        "style": style,
        "width": maxx-minx+1,
        "height": maxy-miny+1,
        "length": maxz-minz+1,
        "blocks": normalized,
    }

def parse_int(token): return int(token.replace("_", ""))

def nodes():
    text = CATALOG.read_text(encoding="utf-8")
    pattern = re.compile(r'new SupplyNode\("([^"]+)",\s*([-\d_]+),\s*([-\d_]+),\s*"([^"]+)",\s*([\d_]+),\s*"([^"]+)",\s*(\d+)\)')
    out = []
    for m in pattern.finditer(text):
        out.append({"id":m.group(1),"x":parse_int(m.group(2)),"z":parse_int(m.group(3)),
                    "role":m.group(4),"style":m.group(6),"turns":int(m.group(7))})
    if len(out) != 18: raise RuntimeError(f"expected 18 supply nodes, got {len(out)}")
    return out

def residence_footprint(x, z):
    cx = x >> 4; cz = z >> 4; mincx = cx << 4; mincz = cz << 4
    desired_x = mincx + (6 if x % 16 < 8 else 1)
    desired_z = mincz + (6 if z % 16 < 8 else 1)
    minx = max(mincx+1, min(mincx+6, desired_x))
    minz = max(mincz+1, min(mincz+6, desired_z))
    return minx, minz, minx+8, minz+8

def rotate(x, z, width, length, turns):
    turns %= 4
    if turns == 0: return x, z
    if turns == 1: return length - 1 - z, x
    if turns == 2: return width - 1 - x, length - 1 - z
    return z, width - 1 - x

def audit_node(node, template):
    turns = node["turns"]
    rw = template["length"] if turns in (1,3) else template["width"]
    rl = template["width"] if turns in (1,3) else template["length"]
    ox = node["x"] - rw // 2
    oz = node["z"] - rl // 2
    fx1, fz1, fx2, fz2 = residence_footprint(node["x"], node["z"])
    overlap_blocks = 0
    overlap_low_blocks = 0
    columns = set()
    low_columns = set()
    for x,y,z in template["blocks"]:
        rx, rz = rotate(x,z,template["width"],template["length"],turns)
        wx, wz = ox + rx, oz + rz
        if fx1 <= wx <= fx2 and fz1 <= wz <= fz2:
            overlap_blocks += 1; columns.add((wx,wz))
            if y <= 12:
                overlap_low_blocks += 1; low_columns.add((wx,wz))
    return {
        **node,
        "footprint": [fx1,fz1,fx2,fz2],
        "template_dimensions": [rw,template["height"],rl],
        "overlap_blocks_all_heights": overlap_blocks,
        "overlap_columns": len(columns),
        "overlap_blocks_y0_12": overlap_low_blocks,
        "overlap_columns_y0_12": len(low_columns),
    }

def main():
    templates = {style: load_template(style,url) for style,url in ASSETS.items()}
    results = [audit_node(node, templates[node["style"]]) for node in nodes()]
    collisions = [r for r in results if r["overlap_blocks_all_heights"] > 0]
    low = [r for r in results if r["overlap_blocks_y0_12"] > 0]
    report = {
        "nodes": len(results),
        "colliding_nodes_any_height": len(collisions),
        "colliding_nodes_y0_12": len(low),
        "total_overlap_blocks": sum(r["overlap_blocks_all_heights"] for r in results),
        "total_overlap_blocks_y0_12": sum(r["overlap_blocks_y0_12"] for r in results),
        "results": results,
    }
    out = ROOT / "logs" / "exterior-residence-overlap.json"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({k:v for k,v in report.items() if k != "results"}, ensure_ascii=False))
    for r in results:
        print(f"{r['id']} style={r['style']} dims={r['template_dimensions']} footprint={r['footprint']} overlap={r['overlap_blocks_all_heights']} low_overlap={r['overlap_blocks_y0_12']} low_columns={r['overlap_columns_y0_12']}")
    if not collisions:
        print("LK_ERDEN_RESIDENCE_OVERLAP_PASS colliding_nodes=0")
    else:
        print(f"LK_ERDEN_RESIDENCE_OVERLAP_FOUND colliding_nodes={len(collisions)} low_colliding_nodes={len(low)} total_blocks={report['total_overlap_blocks']} low_blocks={report['total_overlap_blocks_y0_12']}")

if __name__ == "__main__": main()

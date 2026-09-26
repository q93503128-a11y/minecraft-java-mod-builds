#!/usr/bin/env python3
from __future__ import annotations
import hashlib, json, struct, sys, urllib.parse, urllib.request, zlib
from pathlib import Path

GUITAR_URL = "https://cdn.3dassets.dev/assets/33789/v1/model.glb"
CHAIR_URL = "https://cdn.3dassets.dev/assets/38784/v1/model.glb"
MUSIC = {
    "etirwer": "https://lpc.opengameart.org/sites/default/files/Etirwer%20%28Looped%29_0.ogg",
    "cozy_puzzle": "https://opengameart.org/sites/default/files/cozy_puzzle_in-game_3_bpm108_0.ogg",
    "neon_circuit": "https://opengameart.org/sites/default/files/neon_sign_circuit_bpm145_0.ogg",
    "underwater_pad": "https://opengameart.org/sites/default/files/Underwater-Ambient-Pad-isaiah658_0.ogg",
}
KENNEY_COMMIT = "3694c6879e487c108f55677be7dd2ca75b07cc3b"
KENNEY_FILES = {
    "glass_panel": "ui/UI Pack - Sci-fi/glassPanel.png",
    "metal_panel": "ui/UI Pack - Sci-fi/metalPanel.png",
    "metal_blue": "ui/UI Pack - Sci-fi/metalPanel_blue.png",
    "metal_green": "ui/UI Pack - Sci-fi/metalPanel_green.png",
    "metal_red": "ui/UI Pack - Sci-fi/metalPanel_red.png",
    "metal_yellow": "ui/UI Pack - Sci-fi/metalPanel_yellow.png",
}
USER_AGENT = "CampfireSessions/0.2 (+https://github.com/q93503128-a11y/minecraft-java-mod-builds)"

def download(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=60) as r:
        data = r.read()
    if not data:
        raise RuntimeError(f"Downloaded empty asset: {url}")
    print(f"[licensed-assets] {url} -> {len(data)} bytes sha256={hashlib.sha256(data).hexdigest()}")
    return data

def write_bytes(root: Path, relative: str, data: bytes):
    path = root / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)

def write_text(root: Path, relative: str, text: str):
    path = root / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")

def png_rgba(width: int, height: int, pixels):
    def chunk(kind: bytes, data: bytes):
        payload = kind + data
        return struct.pack(">I", len(data)) + payload + struct.pack(">I", zlib.crc32(payload) & 0xFFFFFFFF)
    rows = []
    for y in range(height):
        row = pixels[y * width:(y + 1) * width]
        rows.append(b"\x00" + b"".join(bytes(px) for px in row))
    raw = b"".join(rows)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")

COMPONENTS = {
    5120: ("b", 1), 5121: ("B", 1), 5122: ("h", 2),
    5123: ("H", 2), 5125: ("I", 4), 5126: ("f", 4),
}
TYPE_SIZE = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4, "MAT4": 16}

def parse_glb(data: bytes):
    if len(data) < 20 or data[:4] != b"glTF":
        raise RuntimeError("Asset is not a GLB file")
    _magic, version, total = struct.unpack_from("<4sII", data, 0)
    if version != 2 or total > len(data):
        raise RuntimeError(f"Unsupported/truncated GLB version={version}")
    pos, document, binary = 12, None, b""
    while pos + 8 <= total:
        length, kind = struct.unpack_from("<II", data, pos)
        payload = data[pos + 8:pos + 8 + length]
        pos += 8 + length
        if kind == 0x4E4F534A:
            document = json.loads(payload.rstrip(b"\x00 \t\r\n").decode("utf-8"))
        elif kind == 0x004E4942:
            binary = payload
    if document is None or not binary:
        raise RuntimeError("GLB is missing JSON or BIN chunk")
    return document, binary

def normalized_component(value, component_type: int, normalized: bool):
    if not normalized or component_type == 5126: return float(value)
    if component_type == 5120: return max(float(value) / 127.0, -1.0)
    if component_type == 5121: return float(value) / 255.0
    if component_type == 5122: return max(float(value) / 32767.0, -1.0)
    if component_type == 5123: return float(value) / 65535.0
    if component_type == 5125: return float(value) / 4294967295.0
    return float(value)

def read_accessor(doc, binary: bytes, index: int):
    acc = doc["accessors"][index]
    if "sparse" in acc:
        raise RuntimeError("Sparse GLB accessors are not supported")
    n = TYPE_SIZE[acc["type"]]
    if "bufferView" not in acc:
        return [(0.0,) * n for _ in range(acc["count"])]
    component_type = acc["componentType"]
    fmt, component_bytes = COMPONENTS[component_type]
    view = doc["bufferViews"][acc["bufferView"]]
    stride = view.get("byteStride", component_bytes * n)
    base = view.get("byteOffset", 0) + acc.get("byteOffset", 0)
    normalized = bool(acc.get("normalized", False))
    out = []
    for i in range(acc["count"]):
        values = struct.unpack_from("<" + fmt * n, binary, base + i * stride)
        out.append(tuple(normalized_component(v, component_type, normalized) for v in values))
    return out

def identity():
    return [[1.0 if r == c else 0.0 for c in range(4)] for r in range(4)]

def mul(a, b):
    return [[sum(a[r][k] * b[k][c] for k in range(4)) for c in range(4)] for r in range(4)]

def trs(node):
    if "matrix" in node:
        m = node["matrix"]
        return [[m[0],m[4],m[8],m[12]],[m[1],m[5],m[9],m[13]],[m[2],m[6],m[10],m[14]],[m[3],m[7],m[11],m[15]]]
    tx,ty,tz = node.get("translation",[0,0,0])
    sx,sy,sz = node.get("scale",[1,1,1])
    x,y,z,w = node.get("rotation",[0,0,0,1])
    xx,yy,zz,xy,xz,yz,wx,wy,wz = x*x,y*y,z*z,x*y,x*z,y*z,w*x,w*y,w*z
    r = [[1-2*(yy+zz),2*(xy-wz),2*(xz+wy),0],[2*(xy+wz),1-2*(xx+zz),2*(yz-wx),0],[2*(xz-wy),2*(yz+wx),1-2*(xx+yy),0],[0,0,0,1]]
    s = [[sx,0,0,0],[0,sy,0,0],[0,0,sz,0],[0,0,0,1]]
    t = [[1,0,0,tx],[0,1,0,ty],[0,0,1,tz],[0,0,0,1]]
    return mul(t, mul(r, s))

def transform(m, p):
    x,y,z = p[:3]
    q = [x,y,z,1.0]
    v = [sum(m[r][c] * q[c] for c in range(4)) for r in range(4)]
    w = v[3] if abs(v[3]) > 1e-9 else 1.0
    return v[0]/w, v[1]/w, v[2]/w

def skip_primitive(doc, node, primitive, mesh, terms):
    if not terms:
        return False

    # Asset-level node names can legitimately contain phrases such as
    # "Acoustic Guitar on a Stand". Only reject a node/mesh name when it
    # names stand/rack geometry without also identifying the guitar itself.
    def dedicated_fixture(name: str) -> bool:
        lowered = name.lower()
        return any(term in lowered for term in terms) and "guitar" not in lowered

    if dedicated_fixture(str(node.get("name", ""))) or dedicated_fixture(str(mesh.get("name", ""))):
        return True

    mi = primitive.get("material")
    if mi is not None and mi < len(doc.get("materials", [])):
        material_name = str(doc["materials"][mi].get("name", "")).lower()
        if any(term in material_name for term in terms):
            return True
    return False

def glb_to_obj(data: bytes, label: str, target_height: float, skip_terms=()):
    doc, binary = parse_glb(data)
    materials = doc.get("materials",[])
    primitives, skipped = [], []
    def visit(node_i, parent):
        node = doc["nodes"][node_i]
        world = mul(parent, trs(node))
        mesh_i = node.get("mesh")
        if mesh_i is not None:
            mesh = doc["meshes"][mesh_i]
            for pi, primitive in enumerate(mesh.get("primitives",[])):
                if primitive.get("mode",4) != 4: continue
                if skip_primitive(doc,node,primitive,mesh,skip_terms):
                    skipped.append(f"{node.get('name','')} / {mesh.get('name','')} / primitive {pi}")
                    continue
                attrs = primitive.get("attributes",{})
                if "POSITION" not in attrs: continue
                positions = [transform(world,p) for p in read_accessor(doc,binary,attrs["POSITION"])]
                indices = [int(v[0]) for v in read_accessor(doc,binary,primitive["indices"])] if "indices" in primitive else list(range(len(positions)))
                indices = indices[:len(indices) - (len(indices) % 3)]
                primitives.append((positions,indices,int(primitive.get("material",-1))))
        for child in node.get("children",[]): visit(child,world)

    scenes = doc.get("scenes",[])
    roots = scenes[doc.get("scene",0)].get("nodes",[]) if scenes else list(range(len(doc.get("nodes",[]))))
    for root in roots: visit(root,identity())
    if not primitives: raise RuntimeError(f"{label}: no triangle primitives")

    points = [p for ps,_i,_m in primitives for p in ps]
    xs,ys,zs = [p[0] for p in points],[p[1] for p in points],[p[2] for p in points]
    minx,maxx,miny,maxy,minz,maxz = min(xs),max(xs),min(ys),max(ys),min(zs),max(zs)
    if maxy-miny <= 1e-9: raise RuntimeError(f"{label}: zero model height")
    scale = target_height / (maxy-miny)
    cx,cz = (minx+maxx)/2.0,(minz+maxz)/2.0

    used = sorted({m for _p,_i,m in primitives})
    slots = {m:i for i,m in enumerate(used)}
    palette = []
    for m in used:
        factor = [0.72,0.50,0.30,1.0]
        if 0 <= m < len(materials):
            factor = materials[m].get("pbrMetallicRoughness",{}).get("baseColorFactor",factor)
        factor = (list(factor)+[1,1,1,1])[:4]
        palette.append(tuple(max(0,min(255,round(float(c)*255))) for c in factor))
    if not palette:
        palette=[(180,130,80,255)]; slots[-1]=0

    lines=[f"# Converted from CC0 {label} GLB",f"mtllib {label}.mtl",f"o {label}"]
    for i in range(len(palette)): lines.append(f"vt {(i+0.5)/len(palette):.8f} 0.50000000")
    base,triangles=1,0
    for positions,indices,mat in primitives:
        slot=slots.get(mat,0); lines.append(f"usemtl material_{slot}")
        for x,y,z in positions: lines.append(f"v {(x-cx)*scale:.7f} {(y-miny)*scale:.7f} {(z-cz)*scale:.7f}")
        uv=slot+1
        for i in range(0,len(indices),3):
            a,b,c=indices[i:i+3]
            lines.append(f"f {base+a}/{uv} {base+b}/{uv} {base+c}/{uv}"); triangles += 1
        base += len(positions)

    mtl=[f"# Material palette for {label}"]
    for i in range(len(palette)):
        mtl += [f"newmtl material_{i}","Ka 1.000000 1.000000 1.000000","Kd 1.000000 1.000000 1.000000",
                "Ks 0.000000 0.000000 0.000000","d 1.000000","illum 1","map_Kd #palette",""]
    print(f"[licensed-assets] {label}: triangles={triangles} materials={len(palette)} skipped={len(skipped)}")
    for name in skipped: print(f"[licensed-assets] {label}: skipped {name}")
    if skip_terms and not skipped:
        print(f"[licensed-assets] WARNING {label}: no stand/rack-named primitive was found")
    return "\n".join(lines)+"\n","\n".join(mtl)+"\n",png_rgba(len(palette),2,palette+palette)

def kenney_url(path: str):
    return f"https://raw.githubusercontent.com/shorepine/kenney/{KENNEY_COMMIT}/{urllib.parse.quote(path,safe='/')}"

def main():
    if len(sys.argv) != 2: raise SystemExit("usage: prepare_licensed_assets.py <output-dir>")
    out=Path(sys.argv[1]).resolve(); out.mkdir(parents=True,exist_ok=True)
    manifest={}

    guitar=download(GUITAR_URL)
    obj,mtl,pal=glb_to_obj(guitar,"acoustic_guitar",1.00,("stand","rack"))
    write_text(out,"assets/campfiresessions/models/item/acoustic_guitar.obj",obj)
    write_text(out,"assets/campfiresessions/models/item/acoustic_guitar.mtl",mtl)
    write_bytes(out,"assets/campfiresessions/textures/item/acoustic_guitar_palette.png",pal)
    manifest["guitar"]={"source":GUITAR_URL,"sha256":hashlib.sha256(guitar).hexdigest()}

    chair=download(CHAIR_URL)
    obj,mtl,pal=glb_to_obj(chair,"wooden_chair",0.96)
    write_text(out,"assets/campfiresessions/models/block/wooden_chair.obj",obj)
    write_text(out,"assets/campfiresessions/models/block/wooden_chair.mtl",mtl)
    write_bytes(out,"assets/campfiresessions/textures/block/wooden_chair_palette.png",pal)
    manifest["chair"]={"source":CHAIR_URL,"sha256":hashlib.sha256(chair).hexdigest()}

    manifest["music"]={}
    for track,url in MUSIC.items():
        data=download(url)
        if not data.startswith(b"OggS"): raise RuntimeError(f"{track}: not OGG")
        write_bytes(out,f"assets/campfiresessions/sounds/music/{track}.ogg",data)
        manifest["music"][track]={"source":url,"sha256":hashlib.sha256(data).hexdigest()}

    manifest["ui"]={}
    for asset,relative in KENNEY_FILES.items():
        url=kenney_url(relative); data=download(url)
        if not data.startswith(b"\x89PNG\r\n\x1a\n"): raise RuntimeError(f"{asset}: not PNG")
        write_bytes(out,f"assets/campfiresessions/textures/gui/sprites/music/{asset}.png",data)
        manifest["ui"][asset]={"source":url,"sha256":hashlib.sha256(data).hexdigest()}
    write_text(out,"campfiresessions_asset_manifest.json",json.dumps(manifest,indent=2)+"\n")

if __name__=="__main__": main()

#!/usr/bin/env python3
"""Generate TURNBOUND 2D summon assets with only Python stdlib.

The portrait baker reads the production GeckoLib geometry + texture atlas and renders
an orthographic 3/4 portrait to PNG. It intentionally does not depend on Blender,
Minecraft client rendering, Pillow, or external services so CI can reproduce assets.
"""
from __future__ import annotations

import argparse
import base64
import json
import math
import os
import struct
import zlib
from pathlib import Path

PORTRAITS = {
    "P01": ("hero/kyren.geo.json", "hero/kyren.png"),
    "P02": ("hero/lumea.geo.json", "hero/lumea.png"),
    "P03": ("hero/bram.geo.json", "hero/bram.png"),
    "P04": ("hero/elysia.geo.json", "hero/elysia.png"),
    "P05": ("hero/lynette.geo.json", "hero/lynette.png"),
    "P06": ("hero/morwen.geo.json", "hero/morwen.png"),
    "P07": ("hero/marion.geo.json", "hero/marion.png"),
    "P08": ("hero/raze.geo.json", "hero/raze.png"),
    "F01": ("filler/f01_militia_male.geo.json", "filler/f01_militia_male.png"),
    "F02": ("filler/f02_field_apprentice.geo.json", "filler/f02_field_apprentice.png"),
    "F03": ("filler/f03_border_hunter.geo.json", "filler/f03_border_hunter.png"),
    "F04": ("filler/f04_shield_mercenary.geo.json", "filler/f04_shield_mercenary.png"),
}

# Character-specific crop nudges. These are intentionally small; visual identity comes from production geometry.
PRESETS = {
    "P01": (-0.08, 0.01, -26.0, -6.0), "P02": (0.02, 0.00, -24.0, -5.0),
    "P03": (-0.02, -0.02, -28.0, -6.0), "P04": (0.03, 0.00, -24.0, -5.0),
    "P05": (0.00, 0.00, -28.0, -5.0), "P06": (0.02, -0.01, -25.0, -6.0),
    "P07": (0.04, 0.00, -24.0, -5.0), "P08": (-0.03, -0.02, -29.0, -6.0),
    "F01": (0.00, -0.02, -27.0, -6.0), "F02": (0.02, -0.01, -25.0, -5.0),
    "F03": (0.00, -0.02, -28.0, -6.0), "F04": (-0.02, -0.02, -27.0, -6.0),
}


def paeth(a: int, b: int, c: int) -> int:
    p = a + b - c
    pa, pb, pc = abs(p-a), abs(p-b), abs(p-c)
    return a if pa <= pb and pa <= pc else b if pb <= pc else c


def read_png(path: Path):
    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError(f"not PNG: {path}")
    pos = 8
    width = height = color_type = bit_depth = None
    raw = bytearray()
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos+4])[0]
        kind = data[pos+4:pos+8]
        payload = data[pos+8:pos+8+length]
        pos += 12 + length
        if kind == b"IHDR":
            width, height, bit_depth, color_type, comp, filt, interlace = struct.unpack(">IIBBBBB", payload)
            if bit_depth != 8 or interlace != 0:
                raise ValueError(f"unsupported PNG mode in {path}")
        elif kind == b"IDAT":
            raw.extend(payload)
        elif kind == b"IEND":
            break
    if color_type not in (2, 6):
        raise ValueError(f"unsupported PNG color type {color_type}: {path}")
    channels = 4 if color_type == 6 else 3
    decompressed = zlib.decompress(bytes(raw))
    stride = width * channels
    out = []
    previous = bytearray(stride)
    offset = 0
    for _ in range(height):
        f = decompressed[offset]; offset += 1
        scan = bytearray(decompressed[offset:offset+stride]); offset += stride
        for i in range(stride):
            left = scan[i-channels] if i >= channels else 0
            up = previous[i]
            ul = previous[i-channels] if i >= channels else 0
            if f == 1: scan[i] = (scan[i] + left) & 255
            elif f == 2: scan[i] = (scan[i] + up) & 255
            elif f == 3: scan[i] = (scan[i] + ((left + up) >> 1)) & 255
            elif f == 4: scan[i] = (scan[i] + paeth(left, up, ul)) & 255
            elif f != 0: raise ValueError(f"unsupported PNG filter {f}")
        row = []
        for x in range(width):
            i = x * channels
            if channels == 4: row.append(tuple(scan[i:i+4]))
            else: row.append((scan[i], scan[i+1], scan[i+2], 255))
        out.append(row)
        previous = scan
    return width, height, out


def png_chunk(kind: bytes, payload: bytes) -> bytes:
    return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload) & 0xffffffff)


def write_png(path: Path, width: int, height: int, pixels):
    rows = bytearray()
    for y in range(height):
        rows.append(0)
        for r,g,b,a in pixels[y]: rows.extend((r,g,b,a))
    data = b"\x89PNG\r\n\x1a\n"
    data += png_chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    data += png_chunk(b"IDAT", zlib.compress(bytes(rows), 9))
    data += png_chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def blank(w, h, rgba=(0,0,0,0)):
    return [[rgba for _ in range(w)] for _ in range(h)]


def blend(dst, src, alpha=1.0):
    sr,sg,sb,sa = src
    sa = max(0.0, min(1.0, sa / 255.0 * alpha))
    if sa <= 0: return dst
    dr,dg,db,da = dst
    da0 = da / 255.0
    oa = sa + da0 * (1-sa)
    if oa <= 1e-9: return (0,0,0,0)
    return (
        int((sr*sa + dr*da0*(1-sa))/oa + .5),
        int((sg*sa + dg*da0*(1-sa))/oa + .5),
        int((sb*sa + db*da0*(1-sa))/oa + .5),
        int(oa*255 + .5),
    )


def mat_mul(a, b):
    return [[sum(a[r][k]*b[k][c] for k in range(4)) for c in range(4)] for r in range(4)]


def identity():
    return [[1,0,0,0],[0,1,0,0],[0,0,1,0],[0,0,0,1]]


def trans(x,y,z):
    m=identity(); m[0][3]=x; m[1][3]=y; m[2][3]=z; return m


def rot_x(d):
    a=math.radians(d); c,s=math.cos(a),math.sin(a)
    return [[1,0,0,0],[0,c,-s,0],[0,s,c,0],[0,0,0,1]]


def rot_y(d):
    a=math.radians(d); c,s=math.cos(a),math.sin(a)
    return [[c,0,s,0],[0,1,0,0],[-s,0,c,0],[0,0,0,1]]


def rot_z(d):
    a=math.radians(d); c,s=math.cos(a),math.sin(a)
    return [[c,-s,0,0],[s,c,0,0],[0,0,1,0],[0,0,0,1]]


def transform(m, p):
    x,y,z=p
    v=(x,y,z,1)
    return tuple(sum(m[r][k]*v[k] for k in range(4)) for r in range(3))


def bone_local(b):
    pivot=b.get("pivot", [0,0,0]); rotation=b.get("rotation", [0,0,0])
    px,py,pz=pivot; rx,ry,rz=rotation
    r = mat_mul(rot_z(rz), mat_mul(rot_y(ry), rot_x(rx)))
    return mat_mul(trans(px,py,pz), mat_mul(r, trans(-px,-py,-pz)))


def cube_faces(origin, size, inflate=0.0):
    x,y,z=origin; dx,dy,dz=size
    x0,x1=x-inflate,x+dx+inflate; y0,y1=y-inflate,y+dy+inflate; z0,z1=z-inflate,z+dz+inflate
    return {
        "north": [(x1,y0,z0),(x0,y0,z0),(x0,y1,z0),(x1,y1,z0)],
        "south": [(x0,y0,z1),(x1,y0,z1),(x1,y1,z1),(x0,y1,z1)],
        "west":  [(x0,y0,z0),(x0,y0,z1),(x0,y1,z1),(x0,y1,z0)],
        "east":  [(x1,y0,z1),(x1,y0,z0),(x1,y1,z0),(x1,y1,z1)],
        "up":    [(x0,y1,z0),(x0,y1,z1),(x1,y1,z1),(x1,y1,z0)],
        "down":  [(x0,y0,z1),(x0,y0,z0),(x1,y0,z0),(x1,y0,z1)],
    }


def box_uv(uv, size):
    u,v=uv; dx,dy,dz=[int(round(n)) for n in size]
    return {
        "west":  (u, v+dz, dz, dy),
        "north": (u+dz, v+dz, dx, dy),
        "east":  (u+dz+dx, v+dz, dz, dy),
        "south": (u+2*dz+dx, v+dz, dx, dy),
        "up":    (u+dz, v, dx, dz),
        "down":  (u+dz+dx, v, dx, dz),
    }


def explicit_uv(face):
    uv=face.get("uv", [0,0]); size=face.get("uv_size", [1,1])
    return (uv[0],uv[1],size[0],size[1])


def triangle_raster(canvas, texture, tw, th, p0,p1,p2, uv0,uv1,uv2, light):
    w=len(canvas[0]); h=len(canvas)
    minx=max(0,int(math.floor(min(p0[0],p1[0],p2[0])))); maxx=min(w-1,int(math.ceil(max(p0[0],p1[0],p2[0]))))
    miny=max(0,int(math.floor(min(p0[1],p1[1],p2[1])))); maxy=min(h-1,int(math.ceil(max(p0[1],p1[1],p2[1]))))
    den=(p1[1]-p2[1])*(p0[0]-p2[0])+(p2[0]-p1[0])*(p0[1]-p2[1])
    if abs(den)<1e-8: return
    for y in range(miny,maxy+1):
        py=y+0.5
        for x in range(minx,maxx+1):
            px=x+0.5
            a=((p1[1]-p2[1])*(px-p2[0])+(p2[0]-p1[0])*(py-p2[1]))/den
            b=((p2[1]-p0[1])*(px-p2[0])+(p0[0]-p2[0])*(py-p2[1]))/den
            c=1-a-b
            if a < -1e-5 or b < -1e-5 or c < -1e-5: continue
            u=a*uv0[0]+b*uv1[0]+c*uv2[0]; v=a*uv0[1]+b*uv1[1]+c*uv2[1]
            tx=max(0,min(tw-1,int(u))); ty=max(0,min(th-1,int(v)))
            r,g,bb,aa=texture[ty][tx]
            if aa == 0: continue
            r=min(255,int(r*light)); g=min(255,int(g*light)); bb=min(255,int(bb*light))
            canvas[y][x]=blend(canvas[y][x],(r,g,bb,aa))


def render_portrait(model_path: Path, texture_path: Path, out_path: Path, silhouette_path: Path, preset):
    doc=json.loads(model_path.read_text(encoding="utf-8"))
    geo=doc["minecraft:geometry"][0]
    tw,th,texture=read_png(texture_path)
    bones=geo.get("bones",[]); by_name={b["name"]:b for b in bones}
    world_cache={}
    def world(name):
        if name in world_cache: return world_cache[name]
        b=by_name[name]; local=bone_local(b); parent=b.get("parent")
        m=mat_mul(world(parent),local) if parent and parent in by_name else local
        world_cache[name]=m; return m

    yaw,pitch=preset[2],preset[3]
    camera=mat_mul(rot_x(pitch),rot_y(yaw))
    polys=[]
    allpts=[]
    for b in bones:
        bm=world(b["name"])
        for cube in b.get("cubes",[]):
            origin=cube.get("origin",[0,0,0]); size=cube.get("size",[1,1,1]); inflate=float(cube.get("inflate",0.0))
            faces=cube_faces(origin,size,inflate)
            uvdef=cube.get("uv",[0,0])
            if isinstance(uvdef, list): uvmap=box_uv(uvdef,size)
            else: uvmap={k:explicit_uv(v) for k,v in uvdef.items() if isinstance(v,dict)}
            for face_name, verts in faces.items():
                if face_name not in uvmap: continue
                worldpts=[transform(camera,transform(bm,p)) for p in verts]
                allpts += worldpts
                depth=sum(p[2] for p in worldpts)/4.0
                u,v,uw,uh=uvmap[face_name]
                uvs=[(u,v+uh),(u+uw,v+uh),(u+uw,v),(u,v)]
                a=worldpts[0]; bb=worldpts[1]; c=worldpts[2]
                e1=(bb[0]-a[0],bb[1]-a[1],bb[2]-a[2]); e2=(c[0]-a[0],c[1]-a[1],c[2]-a[2])
                n=(e1[1]*e2[2]-e1[2]*e2[1], e1[2]*e2[0]-e1[0]*e2[2], e1[0]*e2[1]-e1[1]*e2[0])
                nl=max(1e-6,math.sqrt(sum(q*q for q in n))); n=tuple(q/nl for q in n)
                light_dir=(-0.35,0.55,-0.75); ll=math.sqrt(sum(q*q for q in light_dir)); light_dir=tuple(q/ll for q in light_dir)
                nd=abs(sum(n[i]*light_dir[i] for i in range(3)))
                light=0.72+0.34*nd
                polys.append((depth,worldpts,uvs,light))
    if not allpts: raise ValueError(f"no renderable cubes in {model_path}")
    minx,maxx=min(p[0] for p in allpts),max(p[0] for p in allpts)
    miny,maxy=min(p[1] for p in allpts),max(p[1] for p in allpts)
    outw,outh=512,768
    pad=52
    scale=min((outw-2*pad)/max(1e-6,maxx-minx),(outh-2*pad)/max(1e-6,maxy-miny))
    scale*=1.05
    cx=(minx+maxx)/2.0; cy=(miny+maxy)/2.0
    nudge_x,nudge_y=preset[0],preset[1]
    def screen(p):
        return (outw/2 + (p[0]-cx)*scale + nudge_x*outw,
                outh/2 - (p[1]-cy)*scale + nudge_y*outh)
    canvas=blank(outw,outh)
    for _,pts,uvs,light in sorted(polys,key=lambda x:x[0],reverse=True):
        s=[screen(p) for p in pts]
        triangle_raster(canvas,texture,tw,th,s[0],s[1],s[2],uvs[0],uvs[1],uvs[2],light)
        triangle_raster(canvas,texture,tw,th,s[0],s[2],s[3],uvs[0],uvs[2],uvs[3],light)
    write_png(out_path,outw,outh,canvas)
    sil=blank(outw,outh)
    for y,row in enumerate(canvas):
        for x,p in enumerate(row):
            if p[3]: sil[y][x]=(7,9,16,p[3])
    write_png(silhouette_path,outw,outh,sil)


def radial(w,h, inner, outer, radius=0.48, power=1.7):
    img=blank(w,h)
    cx,cy=(w-1)/2,(h-1)/2
    maxr=min(w,h)*radius
    for y in range(h):
        for x in range(w):
            d=math.hypot(x-cx,y-cy)/maxr
            t=max(0.0,min(1.0,d))**power
            img[y][x]=tuple(int(inner[i]*(1-t)+outer[i]*t) for i in range(4))
    return img


def draw_line(img,x0,y0,x1,y1,color,width=1):
    dx=x1-x0; dy=y1-y0; steps=max(1,int(max(abs(dx),abs(dy))))
    for i in range(steps+1):
        x=int(round(x0+dx*i/steps)); y=int(round(y0+dy*i/steps))
        for oy in range(-width,width+1):
            for ox in range(-width,width+1):
                xx,yy=x+ox,y+oy
                if 0<=yy<len(img) and 0<=xx<len(img[0]): img[yy][xx]=blend(img[yy][xx],color)


def ring_asset(size,color,tilt=0.38, thickness=2):
    img=blank(size,size); cx=cy=size/2; rx=size*0.43; ry=rx*tilt
    prev=None
    for i in range(361):
        a=math.radians(i); p=(cx+math.cos(a)*rx, cy+math.sin(a)*ry)
        if prev: draw_line(img,*prev,*p,color,thickness)
        prev=p
    return img


def rune_asset(size):
    img=blank(size,size); cx=cy=size/2
    for r,alpha in ((0.42,92),(0.33,72),(0.25,52)):
        prev=None
        for i in range(361):
            a=math.radians(i); p=(cx+math.cos(a)*size*r,cy+math.sin(a)*size*r)
            if prev: draw_line(img,*prev,*p,(80,198,255,alpha),1)
            prev=p
    for i in range(24):
        a=math.radians(i*15); r1=size*.36; r2=size*.39 if i%2==0 else size*.37
        draw_line(img,cx+math.cos(a)*r1,cy+math.sin(a)*r1,cx+math.cos(a)*r2,cy+math.sin(a)*r2,(120,225,255,105),1)
    return img


def crystal_asset(w,h):
    img=blank(w,h); cx=w/2
    points=[(cx,h*.05),(w*.83,h*.35),(w*.70,h*.83),(cx,h*.97),(w*.30,h*.83),(w*.17,h*.35)]
    for y in range(h):
        xs=[]
        for i in range(len(points)):
            x1,y1=points[i]; x2,y2=points[(i+1)%len(points)]
            if (y1<=y<y2) or (y2<=y<y1): xs.append(x1+(y-y1)*(x2-x1)/(y2-y1))
        if len(xs)>=2:
            xs.sort(); xa=max(0,int(xs[0])); xb=min(w-1,int(xs[-1]))
            for x in range(xa,xb+1):
                dx=abs(x-cx)/(w*.5); glow=max(0,1-dx)
                img[y][x]=(int(68+70*glow),int(160+70*glow),255,int(205+45*glow))
    for p in points: draw_line(img,cx,h*.50,p[0],p[1],(210,245,255,95),1)
    draw_line(img,w*.17,h*.35,w*.83,h*.35,(210,245,255,95),1)
    return img


def page_asset(w,h):
    img=blank(w,h)
    for y in range(4,h-4):
        for x in range(4,w-4): img[y][x]=(226,216,184,190)
    for i in range(5):
        yy=10+i*7; draw_line(img,12,yy,w-12,yy,(69,75,86,120),1)
    return img


def card_asset(w,h,color):
    img=blank(w,h,(8,14,25,228))
    for t in range(3):
        c=(color[0],color[1],color[2],210-int(t*45))
        draw_line(img,3+t,3+t,w-4-t,3+t,c,0); draw_line(img,3+t,h-4-t,w-4-t,h-4-t,c,0)
        draw_line(img,3+t,3+t,3+t,h-4-t,c,0); draw_line(img,w-4-t,3+t,w-4-t,h-4-t,c,0)
    return img


def generate_common(out: Path):
    out.mkdir(parents=True,exist_ok=True)
    w,h=1000,625
    bg=blank(w,h)
    for y in range(h):
        t=y/(h-1); base=(5+int(5*t),9+int(11*t),20+int(18*t),255)
        for x in range(w):
            edge=abs(x-w/2)/(w/2); shade=int(18*(1-edge)*(1-t*.4))
            bg[y][x]=(base[0]+shade//4,base[1]+shade//2,base[2]+shade,255)
    for x0 in (0,70,140,820,890,960):
        x1=min(w,x0+34)
        for y in range(40,h):
            for x in range(x0,x1): bg[y][x]=(4,7,15,255)
    seed=0x5EED1234
    for _ in range(240):
        seed=(1103515245*seed+12345)&0x7fffffff; x=seed%w
        seed=(1103515245*seed+12345)&0x7fffffff; y=seed%(h*3//4)
        a=80+(seed%140); bg[y][x]=blend(bg[y][x],(75,190,255,a))
    write_png(out/"bg_archive.png",w,h,bg)
    write_png(out/"crystal.png",300,430,crystal_asset(300,430))
    write_png(out/"rune_circle.png",512,512,rune_asset(512))
    write_png(out/"orbit_a.png",512,512,ring_asset(512,(212,177,92,185),0.42,1))
    write_png(out/"orbit_b.png",512,512,ring_asset(512,(93,190,232,145),0.62,1))
    write_png(out/"radial_glow.png",512,512,radial(512,512,(86,215,255,150),(20,60,90,0),0.50,1.8))
    write_png(out/"omen_violet.png",512,512,radial(512,512,(190,96,255,185),(28,8,58,0),0.50,1.5))
    br=radial(512,512,(255,222,116,210),(95,45,8,0),0.49,1.4)
    cx=cy=256
    for i in range(18):
        a=math.radians(i*20+7); length=110+(i%4)*32
        draw_line(br,cx,cy,cx+math.cos(a)*length,cy+math.sin(a)*length,(255,224,136,160),1 if i%3 else 2)
    write_png(out/"break_gold.png",512,512,br)
    sweep=blank(512,128)
    for x in range(512):
        d=abs(x-256)/256; a=int(max(0,1-d*3.5)*190)
        for y in range(128):
            vy=max(0,1-abs(y-64)/64); sweep[y][x]=(255,238,190,int(a*vy))
    write_png(out/"light_sweep.png",512,128,sweep)
    vig=blank(1000,625)
    for y in range(625):
        for x in range(1000):
            dx=abs(x-500)/500; dy=abs(y-312.5)/312.5; e=min(1,(max(dx,dy)-.48)/.52)
            vig[y][x]=(0,0,0,int(max(0,e)*180))
    write_png(out/"vignette.png",1000,625,vig)
    write_png(out/"page.png",80,58,page_asset(80,58))
    write_png(out/"card_common.png",180,250,card_asset(180,250,(78,144,194)))
    write_png(out/"card_rare.png",180,250,card_asset(180,250,(181,92,236)))
    write_png(out/"card_legendary.png",180,250,card_asset(180,250,(244,193,77)))
    tag=blank(96,34,(18,13,5,220))
    for i in range(2):
        draw_line(tag,2+i,2+i,93-i,2+i,(244,193,77,230),0); draw_line(tag,2+i,31-i,93-i,31-i,(244,193,77,230),0)
        draw_line(tag,2+i,2+i,2+i,31-i,(244,193,77,230),0); draw_line(tag,93-i,2+i,93-i,31-i,(244,193,77,230),0)
    write_png(out/"new_tag.png",96,34,tag)


def resolve_texture(resources: Path, rel: str, temp: Path):
    p=resources/"textures"/"entity"/rel
    if p.exists(): return p
    b64=Path(str(p)+".b64")
    if b64.exists():
        temp.parent.mkdir(parents=True,exist_ok=True); temp.write_bytes(base64.b64decode(b64.read_text(encoding="utf-8").strip())); return temp
    raise FileNotFoundError(p)


def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--root",required=True)
    ap.add_argument("--output",required=True)
    args=ap.parse_args()
    root=Path(args.root).resolve(); output=Path(args.output).resolve()
    assets=root/"src/main/resources/assets/turnbound"
    models=assets/"geckolib/models/entity"
    temp=output/".tmp"
    summon=output/"assets/turnbound/textures/gui/summon"
    portraits=summon/"portraits"
    generate_common(summon)
    for cid,(model_rel,tex_rel) in PORTRAITS.items():
        model=models/model_rel
        tex=resolve_texture(assets,tex_rel,temp/(cid.lower()+".png"))
        render_portrait(model,tex,portraits/(cid.lower()+".png"),portraits/(cid.lower()+"_silhouette.png"),PRESETS[cid])
    if temp.exists():
        for p in sorted(temp.rglob("*"),reverse=True):
            if p.is_file(): p.unlink()
            elif p.is_dir(): p.rmdir()
        temp.rmdir()
    print(f"generated TURNBOUND summon assets: {summon}")

if __name__ == "__main__": main()

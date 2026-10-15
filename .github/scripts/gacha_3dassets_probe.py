import json
import sys
import urllib.parse
import urllib.request
from pathlib import Path

import bpy
from mathutils import Vector

API = "https://3dassets.dev/api/v1/assets"
TARGETS = ["Twin leaf blast door", "Decontamination arch"]


def out_dir():
    if "--" not in sys.argv:
        raise SystemExit("Expected -- <output_dir>")
    args = sys.argv[sys.argv.index("--") + 1:]
    if len(args) != 1:
        raise SystemExit("Expected one output dir")
    p = Path(args[0]).resolve()
    p.mkdir(parents=True, exist_ok=True)
    return p


def get_json(url):
    req = urllib.request.Request(url, headers={"User-Agent": "moon-test-asset-previs/1.0"})
    with urllib.request.urlopen(req, timeout=90) as r:
        return json.loads(r.read().decode("utf-8"))


def get_bytes(url):
    req = urllib.request.Request(url, headers={"User-Agent": "moon-test-asset-previs/1.0"})
    with urllib.request.urlopen(req, timeout=180) as r:
        return r.read()


def search_exact(name):
    url = API + "?" + urllib.parse.urlencode({"q": name, "sort": "relevance", "limit": 20})
    data = get_json(url)
    results = data.get("assets") or data.get("results") or data.get("items") or []
    if isinstance(data, list):
        results = data
    exact = [x for x in results if str(x.get("title") or x.get("name") or "").strip().lower() == name.lower()]
    if not exact:
        raise RuntimeError(f"Exact 3dassets match not found for {name}: {[x.get('title') or x.get('name') for x in results[:10]]}")
    return exact[0], url


def resolve_asset(asset):
    slug = asset.get("slug") or asset.get("id")
    if not slug:
        return asset
    try:
        detail = get_json(f"https://3dassets.dev/api/v1/assets/{slug}")
        if isinstance(detail, dict):
            return detail
    except Exception:
        pass
    return asset


def validate_cc0(asset, name):
    lic = asset.get("license") or {}
    text = " ".join([
        str(lic.get("name") if isinstance(lic, dict) else lic),
        str(lic.get("spdx") if isinstance(lic, dict) else ""),
        str(asset.get("licenseName") or asset.get("license_name") or ""),
    ]).lower()
    if "cc0" not in text and "public domain" not in text:
        raise RuntimeError(f"{name} did not report CC0/public-domain license: {lic}")


def download_glb(asset, dest, name):
    url = asset.get("downloadUrl") or asset.get("download_url") or asset.get("cdnUrl") or asset.get("cdn_url")
    if not url:
        files = asset.get("files") or []
        for f in files:
            candidate = f.get("downloadUrl") or f.get("download_url") or f.get("cdnUrl") or f.get("url")
            if candidate and ("glb" in candidate.lower() or str(f.get("format", "")).lower() == "glb"):
                url = candidate
                break
    if not url:
        raise RuntimeError(f"No direct GLB URL for {name}")
    data = get_bytes(url)
    if len(data) < 1024 or data[:4] != b"glTF":
        raise RuntimeError(f"Unexpected GLB for {name}: size={len(data)} head={data[:16]!r} url={url}")
    dest.write_bytes(data)
    return url


def clear_scene():
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)


def point_at(obj, target):
    obj.rotation_euler = (Vector(target) - obj.location).to_track_quat("-Z", "Y").to_euler()


def bounds(objects):
    pts = []
    for o in objects:
        if o.type != "MESH" or o.hide_render:
            continue
        for c in o.bound_box:
            pts.append(o.matrix_world @ Vector(c))
    if not pts:
        raise RuntimeError("No mesh bounds")
    lo = Vector((min(p.x for p in pts), min(p.y for p in pts), min(p.z for p in pts)))
    hi = Vector((max(p.x for p in pts), max(p.y for p in pts), max(p.z for p in pts)))
    return lo, hi


def import_glb(path, label, target_height=5.4):
    before = set(bpy.context.scene.objects)
    bpy.ops.import_scene.gltf(filepath=str(path))
    objects = [o for o in bpy.context.scene.objects if o not in before]
    lo, hi = bounds(objects)
    center = (lo + hi) * 0.5
    size = hi - lo
    scale = target_height / max(size.z, size.y, size.x)
    root = bpy.data.objects.new(label, None)
    bpy.context.scene.collection.objects.link(root)
    for o in objects:
        if o.parent is None:
            o.parent = root
    root.scale = (scale, scale, scale)
    root.location = (-center.x * scale, -center.y * scale, -lo.z * scale)
    return root, objects


def mat(name, color, metallic=0.0, roughness=0.5):
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    bsdf = m.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value = (*color, 1)
    bsdf.inputs["Metallic"].default_value = metallic
    bsdf.inputs["Roughness"].default_value = roughness
    return m


def setup_scene():
    scene = bpy.context.scene
    try:
        scene.render.engine = "BLENDER_EEVEE_NEXT"
    except Exception:
        scene.render.engine = "BLENDER_EEVEE"
    scene.render.resolution_x = 1280
    scene.render.resolution_y = 720
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = "PNG"
    scene.view_settings.exposure = 0.55
    scene.world.use_nodes = True
    bg = scene.world.node_tree.nodes.get("Background")
    bg.inputs["Color"].default_value = (0.004, 0.006, 0.012, 1)
    bg.inputs["Strength"].default_value = 0.14

    bpy.ops.mesh.primitive_plane_add(size=32, location=(0, 0, -0.03))
    bpy.context.object.data.materials.append(mat("Floor", (0.012, 0.016, 0.025), 0.55, 0.25))

    target = (0, 0, 2.6)
    for name, loc, energy, color, size in [
        ("Key", (-5.5, -6.0, 6.5), 1200, (0.84, 0.90, 1.0), 4.0),
        ("BlueRim", (5.5, 0.6, 5.6), 1300, (0.08, 0.25, 0.85), 3.2),
        ("WarmRim", (-4.2, 2.0, 4.1), 480, (0.72, 0.17, 0.04), 2.3),
    ]:
        d = bpy.data.lights.new(name, "AREA")
        d.energy = energy
        d.color = color
        d.shape = "DISK"
        d.size = size
        o = bpy.data.objects.new(name, d)
        bpy.context.scene.collection.objects.link(o)
        o.location = loc
        point_at(o, target)

    cd = bpy.data.cameras.new("Camera")
    cam = bpy.data.objects.new("Camera", cd)
    bpy.context.scene.collection.objects.link(cam)
    bpy.context.scene.camera = cam
    return cam


def render_one(path, out, prefix):
    clear_scene()
    root, _ = import_glb(path, prefix)
    cam = setup_scene()
    views = [
        (f"{prefix}_front.png", (0, -9.5, 2.9), (0, 0, 2.55), 58),
        (f"{prefix}_three_quarter.png", (6.3, -7.2, 3.2), (0, 0, 2.55), 60),
    ]
    for filename, loc, look, lens in views:
        cam.location = loc
        cam.data.lens = lens
        point_at(cam, look)
        bpy.context.scene.render.filepath = str(out / filename)
        bpy.ops.render.render(write_still=True)


def render_compare(door_path, arch_path, out):
    clear_scene()
    door, _ = import_glb(door_path, "BlastDoor", target_height=5.2)
    arch, _ = import_glb(arch_path, "DeconArch", target_height=3.2)
    door.location.x -= 3.2
    arch.location.x += 3.2
    cam = setup_scene()
    cam.location = (0, -13.0, 3.0)
    cam.data.lens = 55
    point_at(cam, (0, 0, 2.4))
    bpy.context.scene.render.filepath = str(out / "05_scale_compare.png")
    bpy.ops.render.render(write_still=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(out / "mechanical_candidates.blend"))


def main():
    out = out_dir()
    records = []
    local = {}
    for name in TARGETS:
        found, query_url = search_exact(name)
        detail = resolve_asset(found)
        validate_cc0(detail, name)
        slug = str(detail.get("slug") or detail.get("id") or name.lower().replace(" ", "-"))
        dest = out / (slug.replace("/", "_") + ".glb")
        dl = download_glb(detail, dest, name)
        rec = {"target": name, "query": query_url, "download": dl, "asset": detail}
        records.append(rec)
        local[name] = dest
    (out / "PROVENANCE.json").write_text(json.dumps(records, indent=2, ensure_ascii=False), encoding="utf-8")
    render_one(local["Twin leaf blast door"], out, "01_blast_door")
    render_one(local["Decontamination arch"], out, "03_decon_arch")
    render_compare(local["Twin leaf blast door"], local["Decontamination arch"], out)


if __name__ == "__main__":
    main()

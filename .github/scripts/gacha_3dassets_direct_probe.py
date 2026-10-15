import json
import sys
import urllib.request
from pathlib import Path

import bpy
from mathutils import Vector

ASSETS = {
    "blast_door": {
        "name": "Twin leaf blast door",
        "url": "https://cdn.3dassets.dev/assets/17358/v1/model.glb",
        "page": "https://3dassets.dev/assets/ufo-crash-site-and-recovery-facility-blast-door-15c5e35c",
        "license": "CC0 1.0 Universal",
    },
    "decon_arch": {
        "name": "Decontamination arch",
        "url": "https://cdn.3dassets.dev/assets/17367/v1/model.glb",
        "page": "https://3dassets.dev/assets/ufo-crash-site-and-recovery-facility-decontamination-a-c32d776f",
        "license": "CC0 1.0 Universal",
    },
}


def output_dir():
    args = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    if len(args) != 1:
        raise SystemExit("Expected -- <output_dir>")
    out = Path(args[0]).resolve()
    out.mkdir(parents=True, exist_ok=True)
    return out


def download(url, dest):
    req = urllib.request.Request(url, headers={"User-Agent": "moon-test-asset-previs/1.0"})
    with urllib.request.urlopen(req, timeout=180) as response:
        data = response.read()
    if len(data) < 1024 or data[:4] != b"glTF":
        raise RuntimeError(f"Unexpected GLB: {len(data)} bytes {data[:16]!r}")
    dest.write_bytes(data)


def point_at(obj, target):
    obj.rotation_euler = (Vector(target) - obj.location).to_track_quat("-Z", "Y").to_euler()


def clear_scene():
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)


def bounds(objects):
    pts = []
    for obj in objects:
        if obj.type != "MESH" or obj.hide_render:
            continue
        pts.extend(obj.matrix_world @ Vector(c) for c in obj.bound_box)
    if not pts:
        raise RuntimeError("No renderable mesh")
    lo = Vector((min(p.x for p in pts), min(p.y for p in pts), min(p.z for p in pts)))
    hi = Vector((max(p.x for p in pts), max(p.y for p in pts), max(p.z for p in pts)))
    return lo, hi


def import_normalized(path, label, target_height):
    before = set(bpy.context.scene.objects)
    bpy.ops.import_scene.gltf(filepath=str(path))
    imported = [o for o in bpy.context.scene.objects if o not in before]
    lo, hi = bounds(imported)
    center = (lo + hi) * 0.5
    max_dim = max((hi - lo).x, (hi - lo).y, (hi - lo).z)
    scale = target_height / max_dim
    root = bpy.data.objects.new(label, None)
    bpy.context.scene.collection.objects.link(root)
    for obj in imported:
        if obj.parent is None:
            obj.parent = root
    root.scale = (scale, scale, scale)
    root.location = (-center.x * scale, -center.y * scale, -lo.z * scale)
    return root


def floor_mat():
    mat = bpy.data.materials.new("Floor")
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value = (0.012, 0.016, 0.025, 1)
    bsdf.inputs["Metallic"].default_value = 0.55
    bsdf.inputs["Roughness"].default_value = 0.25
    return mat


def setup_scene(target=(0, 0, 2.55)):
    scene = bpy.context.scene
    try:
        scene.render.engine = "BLENDER_EEVEE_NEXT"
    except Exception:
        scene.render.engine = "BLENDER_EEVEE"
    scene.render.resolution_x = 1280
    scene.render.resolution_y = 720
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = "PNG"
    scene.view_settings.exposure = 0.6
    scene.world.use_nodes = True
    bg = scene.world.node_tree.nodes.get("Background")
    bg.inputs["Color"].default_value = (0.004, 0.006, 0.012, 1)
    bg.inputs["Strength"].default_value = 0.14

    bpy.ops.mesh.primitive_plane_add(size=32, location=(0, 0, -0.03))
    bpy.context.object.data.materials.append(floor_mat())

    lights = [
        ("Key", (-5.5, -6.0, 6.5), 1200, (0.84, 0.90, 1.0), 4.0),
        ("BlueRim", (5.5, 0.6, 5.6), 1300, (0.08, 0.25, 0.85), 3.2),
        ("WarmRim", (-4.2, 2.0, 4.1), 480, (0.72, 0.17, 0.04), 2.3),
    ]
    for name, loc, energy, color, size in lights:
        data = bpy.data.lights.new(name, "AREA")
        data.energy = energy
        data.color = color
        data.shape = "DISK"
        data.size = size
        obj = bpy.data.objects.new(name, data)
        bpy.context.scene.collection.objects.link(obj)
        obj.location = loc
        point_at(obj, target)

    cdata = bpy.data.cameras.new("Camera")
    cam = bpy.data.objects.new("Camera", cdata)
    bpy.context.scene.collection.objects.link(cam)
    bpy.context.scene.camera = cam
    return cam


def render_asset(path, out, prefix, height):
    clear_scene()
    import_normalized(path, prefix, height)
    cam = setup_scene()
    for suffix, loc, lens in [
        ("front", (0, -9.5, 2.9), 58),
        ("three_quarter", (6.3, -7.2, 3.2), 60),
    ]:
        cam.location = loc
        cam.data.lens = lens
        point_at(cam, (0, 0, 2.55))
        bpy.context.scene.render.filepath = str(out / f"{prefix}_{suffix}.png")
        bpy.ops.render.render(write_still=True)


def render_compare(door, arch, out):
    clear_scene()
    droot = import_normalized(door, "BlastDoor", 5.2)
    aroot = import_normalized(arch, "DeconArch", 3.2)
    droot.location.x -= 3.3
    aroot.location.x += 3.3
    cam = setup_scene((0, 0, 2.4))
    cam.location = (0, -13.0, 3.0)
    cam.data.lens = 55
    point_at(cam, (0, 0, 2.4))
    bpy.context.scene.render.filepath = str(out / "05_scale_compare.png")
    bpy.ops.render.render(write_still=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(out / "mechanical_candidates.blend"))


def main():
    out = output_dir()
    local = {}
    provenance = []
    for key, info in ASSETS.items():
        path = out / f"{key}.glb"
        download(info["url"], path)
        local[key] = path
        provenance.append({**info, "local_file": path.name, "download_method": "documented permanent CDN URL", "visual_approval": "NOT YET GRANTED"})
    (out / "PROVENANCE.json").write_text(json.dumps(provenance, indent=2), encoding="utf-8")
    render_asset(local["blast_door"], out, "01_blast_door", 5.4)
    render_asset(local["decon_arch"], out, "03_decon_arch", 3.4)
    render_compare(local["blast_door"], local["decon_arch"], out)


if __name__ == "__main__":
    main()

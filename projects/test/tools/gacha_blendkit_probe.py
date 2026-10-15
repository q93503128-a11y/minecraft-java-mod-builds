import json
import sys
import urllib.request
from pathlib import Path

import bpy
from mathutils import Vector

ASSET_NAME = "Eldritch Portal"
AUTHOR = "phantasmaCora"
SOURCE_PAGE = "https://opengameart.org/content/eldritch-portal-model-with-pbr-textures"
SOURCE_BLEND = "https://opengameart.org/sites/default/files/eldritch_portal.blend"
LICENSE = "CC-BY 4.0"


def output_dir():
    if "--" not in sys.argv:
        raise SystemExit("Expected -- <output_dir>")
    args = sys.argv[sys.argv.index("--") + 1:]
    if len(args) != 1:
        raise SystemExit("Expected one output directory")
    out = Path(args[0]).resolve()
    out.mkdir(parents=True, exist_ok=True)
    return out


def download(url: str, path: Path):
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 moon-test-asset-previs"})
    with urllib.request.urlopen(req, timeout=120) as response:
        data = response.read()
    if len(data) < 1024 or data[:4] != b"BLEN":
        raise RuntimeError(f"Unexpected Blender payload: size={len(data)} head={data[:16]!r}")
    path.write_bytes(data)


def point_at(obj, target):
    obj.rotation_euler = (Vector(target) - obj.location).to_track_quat("-Z", "Y").to_euler()


def mesh_bounds(objects):
    points = []
    for obj in objects:
        if obj.type != "MESH" or obj.hide_render:
            continue
        for corner in obj.bound_box:
            points.append(obj.matrix_world @ Vector(corner))
    if not points:
        raise RuntimeError("No renderable mesh found")
    lo = Vector((min(p.x for p in points), min(p.y for p in points), min(p.z for p in points)))
    hi = Vector((max(p.x for p in points), max(p.y for p in points), max(p.z for p in points)))
    return lo, hi


def normalize_scene():
    for obj in list(bpy.data.objects):
        if obj.type in {"CAMERA", "LIGHT"}:
            bpy.data.objects.remove(obj, do_unlink=True)
    scene_objects = list(bpy.context.scene.objects)
    lo, hi = mesh_bounds(scene_objects)
    center = (lo + hi) * 0.5
    size = hi - lo
    max_dim = max(size.x, size.y, size.z)
    root = bpy.data.objects.new("InspectionRoot", None)
    bpy.context.scene.collection.objects.link(root)
    for obj in scene_objects:
        if obj.parent is None:
            obj.parent = root
    scale = 6.2 / max_dim
    root.scale = (scale, scale, scale)
    root.location = (-center.x * scale, -center.y * scale, -lo.z * scale)
    return root, 6.2


def simple_material(name, color, metallic=0.0, roughness=0.5):
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value = (*color, 1)
    bsdf.inputs["Metallic"].default_value = metallic
    bsdf.inputs["Roughness"].default_value = roughness
    return mat


def setup_render(out: Path, height: float):
    scene = bpy.context.scene
    try:
        scene.render.engine = "BLENDER_EEVEE_NEXT"
    except Exception:
        scene.render.engine = "BLENDER_EEVEE"
    scene.render.resolution_x = 1280
    scene.render.resolution_y = 720
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = "PNG"
    scene.view_settings.exposure = 0.65
    scene.world.use_nodes = True
    bg = scene.world.node_tree.nodes.get("Background")
    bg.inputs["Color"].default_value = (0.005, 0.006, 0.012, 1)
    bg.inputs["Strength"].default_value = 0.13

    bpy.ops.mesh.primitive_plane_add(size=32, location=(0, 0, -0.03))
    floor = bpy.context.object
    floor.data.materials.append(simple_material("InspectionFloor", (0.012, 0.016, 0.024), 0.55, 0.24))

    target = (0, 0, height * 0.45)
    def area(name, loc, energy, color, size):
        data = bpy.data.lights.new(name, "AREA")
        data.energy = energy
        data.color = color
        data.shape = "DISK"
        data.size = size
        obj = bpy.data.objects.new(name, data)
        bpy.context.scene.collection.objects.link(obj)
        obj.location = loc
        point_at(obj, target)
    area("Key", (-5.8, -6.8, 7.0), 1200, (0.85, 0.90, 1.0), 4.2)
    area("BlueRim", (6.0, 0.8, 5.8), 1250, (0.12, 0.30, 0.85), 3.2)
    area("WarmRim", (-4.0, 2.0, 4.4), 500, (0.75, 0.18, 0.04), 2.5)

    cdata = bpy.data.cameras.new("InspectionCamera")
    cam = bpy.data.objects.new("InspectionCamera", cdata)
    bpy.context.scene.collection.objects.link(cam)
    bpy.context.scene.camera = cam
    views = [
        ("01_front.png", (0, -10.5, 3.1), target, 58),
        ("02_three_quarter.png", (7.0, -8.0, 3.7), target, 60),
        ("03_close.png", (2.4, -6.3, 3.0), (0, 0, height * 0.47), 65),
    ]
    for filename, loc, look, lens in views:
        cam.location = loc
        cam.data.lens = lens
        point_at(cam, look)
        scene.render.filepath = str(out / filename)
        bpy.ops.render.render(write_still=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(out / "hero_candidate.blend"))


def main():
    out = output_dir()
    source = out / "source_asset.blend"
    download(SOURCE_BLEND, source)
    meta = {
        "name": ASSET_NAME,
        "author": AUTHOR,
        "source_page": SOURCE_PAGE,
        "source_file": SOURCE_BLEND,
        "license": LICENSE,
        "download_method": "public direct file; no authentication or bypass",
        "runtime_use": "NOT YET APPROVED",
        "visual_approval": "NOT YET GRANTED"
    }
    (out / "SEARCH_RESULT.json").write_text(json.dumps(meta, indent=2), encoding="utf-8")
    (out / "SELECTED_ASSET.json").write_text(json.dumps(meta, indent=2), encoding="utf-8")
    (out / "PROVENANCE.json").write_text(json.dumps(meta, indent=2), encoding="utf-8")

    bpy.ops.wm.open_mainfile(filepath=str(source))
    _, height = normalize_scene()
    setup_render(out, height)


if __name__ == "__main__":
    main()

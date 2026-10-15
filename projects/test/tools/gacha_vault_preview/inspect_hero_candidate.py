import math
import sys
from pathlib import Path

import bpy
from mathutils import Vector


def cli_args():
    if "--" not in sys.argv:
        raise SystemExit("Expected -- <asset_dir> <output_dir>")
    args = sys.argv[sys.argv.index("--") + 1:]
    if len(args) != 2:
        raise SystemExit("Expected <asset_dir> <output_dir>")
    return Path(args[0]).resolve(), Path(args[1]).resolve()


def clear_scene():
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)


def mat(name, color, metallic=0.85, roughness=0.24):
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    bsdf = m.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value = (*color, 1)
    bsdf.inputs["Metallic"].default_value = metallic
    bsdf.inputs["Roughness"].default_value = roughness
    return m


def override_material(objects, material):
    for obj in objects:
        if obj.type == "MESH":
            obj.data.materials.clear()
            obj.data.materials.append(material)


def bounds(objects):
    points = []
    for obj in objects:
        if obj.type != "MESH":
            continue
        for c in obj.bound_box:
            points.append(obj.matrix_world @ Vector(c))
    if not points:
        raise RuntimeError("Imported asset contains no mesh bounds")
    mins = Vector((min(p.x for p in points), min(p.y for p in points), min(p.z for p in points)))
    maxs = Vector((max(p.x for p in points), max(p.y for p in points), max(p.z for p in points)))
    return mins, maxs


def import_candidate(asset_dir):
    fbx = sorted(asset_dir.rglob("*.fbx"))
    obj = sorted(asset_dir.rglob("*.obj"))
    blends = sorted(asset_dir.rglob("*.blend"))

    def score(p):
        s = p.name.lower()
        value = 0
        if "door" in s:
            value += 100
        if "vault" in s:
            value += 80
        if "wall" in s:
            value -= 90
        if "handle" in s:
            value += 15
        return (-value, len(s), s)

    if fbx:
        candidate = sorted(fbx, key=score)[0]
        before = set(bpy.context.scene.objects)
        bpy.ops.import_scene.fbx(filepath=str(candidate))
        imported = [o for o in bpy.context.scene.objects if o not in before]
        return candidate, imported

    if obj:
        candidate = sorted(obj, key=score)[0]
        before = set(bpy.context.scene.objects)
        try:
            bpy.ops.wm.obj_import(filepath=str(candidate))
        except Exception:
            bpy.ops.import_scene.obj(filepath=str(candidate))
        imported = [o for o in bpy.context.scene.objects if o not in before]
        return candidate, imported

    if blends:
        candidate = sorted(blends, key=score)[0]
        with bpy.data.libraries.load(str(candidate), link=False) as (src, dst):
            dst.objects = list(src.objects)
        imported = []
        for obj in dst.objects:
            if obj is not None:
                bpy.context.scene.collection.objects.link(obj)
                imported.append(obj)
        return candidate, imported

    raise RuntimeError(f"No FBX/OBJ/BLEND candidate found under {asset_dir}")


def normalize(objects, target_height=5.2):
    lo, hi = bounds(objects)
    size = hi - lo
    center = (lo + hi) * 0.5
    scale = target_height / max(size.z, size.y, size.x)
    root = bpy.data.objects.new("HeroCandidateRoot", None)
    bpy.context.scene.collection.objects.link(root)
    for obj in objects:
        if obj.parent is None:
            obj.parent = root
    root.scale = (scale, scale, scale)
    root.location = (-center.x * scale, -center.y * scale, -lo.z * scale)
    return root


def point_at(obj, target):
    obj.rotation_euler = (Vector(target) - obj.location).to_track_quat("-Z", "Y").to_euler()


def light(name, loc, energy, color, size, target):
    data = bpy.data.lights.new(name, "AREA")
    data.energy = energy
    data.color = color
    data.shape = "DISK"
    data.size = size
    obj = bpy.data.objects.new(name, data)
    bpy.context.scene.collection.objects.link(obj)
    obj.location = loc
    point_at(obj, target)


def setup(output_dir):
    scene = bpy.context.scene
    try:
        scene.render.engine = "BLENDER_EEVEE_NEXT"
    except Exception:
        scene.render.engine = "BLENDER_EEVEE"
    scene.render.resolution_x = 960
    scene.render.resolution_y = 960
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = "PNG"
    scene.view_settings.exposure = 0.65
    scene.world.use_nodes = True
    bg = scene.world.node_tree.nodes.get("Background")
    bg.inputs["Color"].default_value = (0.006, 0.008, 0.014, 1)
    bg.inputs["Strength"].default_value = 0.16

    bpy.ops.mesh.primitive_plane_add(size=22, location=(0, 0, -0.04))
    floor = bpy.context.object
    floor.data.materials.append(mat("Floor", (0.014, 0.017, 0.025), 0.55, 0.18))

    light("Key", (-5, -6, 7), 1150, (0.9, 0.92, 1.0), 4.0, (0, 0, 2.4))
    light("Rim", (5, 2, 6), 900, (0.16, 0.28, 0.75), 3.3, (0, 0, 2.5))
    light("Warm", (-3, 3, 3), 550, (0.65, 0.18, 0.06), 2.4, (0, 0, 2.4))

    cam_data = bpy.data.cameras.new("Camera")
    cam_data.lens = 56
    cam = bpy.data.objects.new("Camera", cam_data)
    bpy.context.scene.collection.objects.link(cam)
    bpy.context.scene.camera = cam
    output_dir.mkdir(parents=True, exist_ok=True)
    return cam


def render_views(root, cam, output_dir):
    scene = bpy.context.scene
    views = [
        ("01_front.png", (0, -9.2, 2.7), (0, 0, 2.55), 0),
        ("02_three_quarter.png", (6.6, -7.0, 3.0), (0, 0, 2.45), 0),
        ("03_openability.png", (-5.7, -6.8, 2.6), (0, 0, 2.45), math.radians(4)),
    ]
    for filename, pos, target, root_z in views:
        root.rotation_euler[2] = root_z
        cam.location = pos
        point_at(cam, target)
        scene.render.filepath = str(output_dir / filename)
        bpy.ops.render.render(write_still=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(output_dir / "hero_candidate.blend"))


if __name__ == "__main__":
    asset_dir, output_dir = cli_args()
    clear_scene()
    candidate, imported = import_candidate(asset_dir)
    neutral = mat("HeroNeutral", (0.055, 0.065, 0.085), 0.90, 0.20)
    override_material(imported, neutral)
    root = normalize(imported)
    cam = setup(output_dir)
    render_views(root, cam, output_dir)
    (output_dir / "SELECTED_SOURCE.txt").write_text(str(candidate) + "\n", encoding="utf-8")

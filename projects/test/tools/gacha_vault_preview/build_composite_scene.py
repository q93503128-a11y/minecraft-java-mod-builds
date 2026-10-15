import math
import sys
from pathlib import Path

import bpy
from mathutils import Vector


def cli_args():
    if "--" not in sys.argv:
        raise SystemExit("Expected -- <quaternius_root> <vault_source_root> <output_dir>")
    args = sys.argv[sys.argv.index("--") + 1:]
    if len(args) != 3:
        raise SystemExit("Expected <quaternius_root> <vault_source_root> <output_dir>")
    return Path(args[0]).resolve(), Path(args[1]).resolve(), Path(args[2]).resolve()


def clear_scene():
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)


def material(name, color, metallic=0.0, roughness=0.5, emission=None, emission_strength=0.0):
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    bsdf = m.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value = (*color, 1.0)
    bsdf.inputs["Metallic"].default_value = metallic
    bsdf.inputs["Roughness"].default_value = roughness
    if emission is not None:
        key = "Emission Color" if "Emission Color" in bsdf.inputs else "Emission"
        bsdf.inputs[key].default_value = (*emission, 1.0)
        if "Emission Strength" in bsdf.inputs:
            bsdf.inputs["Emission Strength"].default_value = emission_strength
    return m


def override_material(objects, m):
    for obj in objects:
        if obj.type == "MESH":
            obj.data.materials.clear()
            obj.data.materials.append(m)


def import_gltf(path, name, m=None):
    before = set(bpy.context.scene.objects)
    bpy.ops.import_scene.gltf(filepath=str(path))
    objects = [o for o in bpy.context.scene.objects if o not in before]
    root = bpy.data.objects.new(name, None)
    bpy.context.scene.collection.objects.link(root)
    for obj in objects:
        if obj.parent is None:
            obj.parent = root
    if m is not None:
        override_material(objects, m)
    return root, objects


def mesh_bounds(objects):
    points = []
    for obj in objects:
        if obj.type != "MESH":
            continue
        for corner in obj.bound_box:
            points.append(obj.matrix_world @ Vector(corner))
    if not points:
        raise RuntimeError("No mesh bounds found")
    lo = Vector((min(p.x for p in points), min(p.y for p in points), min(p.z for p in points)))
    hi = Vector((max(p.x for p in points), max(p.y for p in points), max(p.z for p in points)))
    return lo, hi


def import_vault_fbx(source_root, m):
    candidates = sorted(source_root.rglob("*.fbx"))
    if not candidates:
        raise RuntimeError("No FBX in vault source")

    def rank(p):
        n = p.name.lower()
        score = (100 if "door" in n else 0) + (80 if "vault" in n else 0) - (100 if "wall" in n else 0)
        return (-score, len(n), n)

    chosen = sorted(candidates, key=rank)[0]
    before = set(bpy.context.scene.objects)
    bpy.ops.import_scene.fbx(filepath=str(chosen))
    objects = [o for o in bpy.context.scene.objects if o not in before]
    override_material(objects, m)

    lo, hi = mesh_bounds(objects)
    size = hi - lo
    scale = 5.25 / max(size.x, size.y, size.z)
    center = (lo + hi) * 0.5

    root = bpy.data.objects.new("PhysicalVaultDoor", None)
    offset = bpy.data.objects.new("PhysicalVaultDoorOffset", None)
    bpy.context.scene.collection.objects.link(root)
    bpy.context.scene.collection.objects.link(offset)
    offset.parent = root
    for obj in objects:
        if obj.parent is None:
            obj.parent = offset
    offset.location = (-center.x, -center.y, -lo.z)
    root.scale = (scale, scale, scale)
    # Source front normal is approximately +X. Rotate it toward the camera (-Y).
    root.rotation_euler[2] = math.radians(-90)
    root.location = (0.0, 2.70, 0.22)
    return root, objects, chosen


def point_at(obj, target):
    obj.rotation_euler = (Vector(target) - obj.location).to_track_quat("-Z", "Y").to_euler()


def add_area(name, pos, energy, color, size, target):
    data = bpy.data.lights.new(name, "AREA")
    data.energy = energy
    data.color = color
    data.shape = "DISK"
    data.size = size
    obj = bpy.data.objects.new(name, data)
    bpy.context.scene.collection.objects.link(obj)
    obj.location = pos
    point_at(obj, target)
    return obj


def add_point(name, pos, energy, color, radius):
    data = bpy.data.lights.new(name, "POINT")
    data.energy = energy
    data.color = color
    data.shadow_soft_size = radius
    obj = bpy.data.objects.new(name, data)
    bpy.context.scene.collection.objects.link(obj)
    obj.location = pos
    return obj


def add_box(name, pos, scale, m, rotation=(0, 0, 0), bevel=0.0):
    bpy.ops.mesh.primitive_cube_add(location=pos, rotation=rotation)
    obj = bpy.context.object
    obj.name = name
    obj.scale = scale
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    if bevel:
        mod = obj.modifiers.new("EdgeSoftening", "BEVEL")
        mod.width = bevel
        mod.segments = 3
    obj.data.materials.append(m)
    return obj


def add_disk(name, pos, radius, depth, m):
    bpy.ops.mesh.primitive_cylinder_add(vertices=96, radius=radius, depth=depth, location=pos, rotation=(math.radians(90), 0, 0))
    obj = bpy.context.object
    obj.name = name
    obj.data.materials.append(m)
    return obj


def add_curve_halo(columns, center, scale, m):
    roots = []
    for i in range(4):
        root, _ = import_gltf(columns / "Column_MetalSupport_Curve.gltf", f"HaloQuarter_{i+1}", m)
        root.location = center
        root.scale = (scale, scale, scale)
        root.rotation_euler = (math.radians(90), math.radians(i * 90), 0)
        roots.append(root)
    return roots


def add_clamps(props, m):
    items = []
    placements = [
        (-3.05, 2.37, 2.82, 0.0),
        (3.05, 2.37, 2.82, math.pi),
        (0.0, 2.37, 5.83, math.pi / 2),
        (0.0, 2.37, 0.18, -math.pi / 2),
    ]
    for i, (x, y, z, rz) in enumerate(placements):
        root, _ = import_gltf(props / "Prop_Clamp.gltf", f"OuterLock_{i+1}", m)
        root.location = (x, y, z)
        root.scale = (1.55, 1.55, 1.55)
        root.rotation_euler = (math.radians(90), 0, rz)
        items.append(root)
    return items


def configure_render():
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
    bg.inputs["Color"].default_value = (0.0025, 0.0035, 0.007, 1)
    bg.inputs["Strength"].default_value = 0.08

    for look in ("AgX - Medium High Contrast", "Medium High Contrast"):
        try:
            scene.view_settings.look = look
            break
        except Exception:
            pass

    scene.use_nodes = True
    tree = scene.node_tree
    tree.nodes.clear()
    rl = tree.nodes.new("CompositorNodeRLayers")
    glare = tree.nodes.new("CompositorNodeGlare")
    glare.glare_type = "FOG_GLOW"
    glare.quality = "HIGH"
    glare.threshold = 1.4
    glare.size = 6
    comp = tree.nodes.new("CompositorNodeComposite")
    tree.links.new(rl.outputs["Image"], glare.inputs["Image"])
    tree.links.new(glare.outputs["Image"], comp.inputs["Image"])


def build(quaternius_root, vault_root):
    platforms = quaternius_root / "Platforms"
    props = quaternius_root / "Props"
    columns = quaternius_root / "Columns"

    door_m = material("VaultDoorMetal", (0.070, 0.080, 0.105), 0.91, 0.18)
    frame_m = material("FrameMetal", (0.040, 0.048, 0.066), 0.86, 0.25)
    halo_m = material("HaloMetal", (0.080, 0.090, 0.115), 0.92, 0.18)
    clamp_m = material("ClampMetal", (0.115, 0.120, 0.145), 0.92, 0.17)
    stage_m = material("StageMetal", (0.030, 0.036, 0.050), 0.78, 0.24)
    violet = material("VioletStatus", (0.012, 0.003, 0.030), 0.1, 0.25, (0.34, 0.035, 0.95), 5.5)
    gold = material("GoldCore", (0.070, 0.022, 0.002), 0.05, 0.18, (1.0, 0.34, 0.035), 12.0)
    floor_m = material("Floor", (0.006, 0.008, 0.014), 0.62, 0.18)

    hero, _, chosen = import_vault_fbx(vault_root, door_m)

    frame, _ = import_gltf(platforms / "Door_Frame_SquareTall.gltf", "OuterFrame", frame_m)
    frame.location = (0, 3.58, -0.05)
    frame.scale = (1.52, 1.52, 1.52)

    halo = add_curve_halo(columns, (0, 3.18, 2.84), 0.70, halo_m)
    clamps = add_clamps(props, clamp_m)

    stage, _ = import_gltf(platforms / "Platform_Round1.gltf", "RevealStage", stage_m)
    stage.location = (0, -0.45, 0)
    stage.scale = (1.38, 1.38, 1.38)

    for x in (-4.45, 4.45):
        support, _ = import_gltf(columns / "Column_Hollow.gltf", f"SideSupport_{x:+.0f}", frame_m)
        support.location = (x, 3.72, 0)
        support.scale = (0.92, 0.92, 1.23)

    indicators = []
    for name, pos, scale in [
        ("TopStatus", (0, 2.10, 5.47), (0.25, 0.025, 0.025)),
        ("LeftStatus", (-2.64, 2.10, 2.82), (0.025, 0.025, 0.25)),
        ("RightStatus", (2.64, 2.10, 2.82), (0.025, 0.025, 0.25)),
    ]:
        indicators.append(add_box(name, pos, scale, violet, bevel=0.012))

    bpy.ops.mesh.primitive_plane_add(size=44, location=(0, 0, -0.04))
    floor = bpy.context.object
    floor.data.materials.append(floor_m)

    add_area("FrontKey", (-3.5, -6.0, 5.5), 1050, (0.16, 0.18, 0.28), 4.2, (0, 2.8, 2.7))
    add_area("RightRim", (5.8, 0.5, 6.0), 1250, (0.04, 0.18, 0.52), 3.4, (0, 3.0, 2.8))
    add_area("LeftRim", (-5.5, 1.5, 4.8), 760, (0.32, 0.08, 0.40), 3.0, (0, 3.0, 2.6))
    add_area("Top", (0, 3.8, 8.8), 1150, (0.25, 0.12, 0.42), 3.3, (0, 3.0, 2.7))
    add_area("FloorBounce", (0, -1.0, 0.8), 420, (0.08, 0.10, 0.16), 4.0, (0, 2.8, 1.8))

    cam_data = bpy.data.cameras.new("VaultCamera")
    cam_data.lens = 52
    cam = bpy.data.objects.new("VaultCamera", cam_data)
    bpy.context.scene.collection.objects.link(cam)
    bpy.context.scene.camera = cam

    return {
        "hero": hero,
        "halo": halo,
        "clamps": clamps,
        "indicators": indicators,
        "gold": gold,
        "cam": cam,
        "chosen": chosen,
    }


def set_material(obj, m):
    obj.data.materials.clear()
    obj.data.materials.append(m)


def render(parts, output_dir):
    output_dir.mkdir(parents=True, exist_ok=True)
    scene = bpy.context.scene
    cam = parts["cam"]

    cam.location = (0.75, -11.4, 2.25)
    cam.data.lens = 52
    point_at(cam, (0, 2.75, 2.70))
    scene.render.filepath = str(output_dir / "01_locked_composite.png")
    bpy.ops.render.render(write_still=True)

    cam.location = (-1.05, -8.4, 1.95)
    cam.data.lens = 56
    point_at(cam, (0, 2.72, 2.78))
    scene.render.filepath = str(output_dir / "02_approach_composite.png")
    bpy.ops.render.render(write_still=True)

    # Legendary blockout: outer locks disengage, physical vault door is forced off-axis,
    # and the gold core becomes visible behind it. This is previs, not final fracture animation.
    for i, clamp in enumerate(parts["clamps"]):
        if i == 0:
            clamp.location.x -= 0.42
        elif i == 1:
            clamp.location.x += 0.42
        elif i == 2:
            clamp.location.z += 0.35
        else:
            clamp.location.z -= 0.28
    for indicator in parts["indicators"]:
        set_material(indicator, parts["gold"])

    pivot = bpy.data.objects.new("BreachPivot", None)
    bpy.context.scene.collection.objects.link(pivot)
    pivot.location = (-2.45, 2.72, 2.65)
    hero = parts["hero"]
    world = hero.matrix_world.copy()
    hero.parent = pivot
    hero.matrix_world = world
    pivot.rotation_euler[2] = math.radians(-18)
    pivot.location.y -= 0.20

    core = add_disk("LegendaryCore", (0, 3.18, 2.82), 2.42, 0.07, parts["gold"])
    add_area("LegendaryBlast", (0, 2.95, 2.82), 3000, (1.0, 0.36, 0.045), 2.5, (0, -2.0, 2.5))
    add_point("LegendaryBounce", (0, 1.85, 2.8), 850, (1.0, 0.22, 0.025), 0.45)

    cam.location = (0.45, -9.0, 2.12)
    cam.data.lens = 49
    point_at(cam, (0, 2.78, 2.72))
    scene.render.filepath = str(output_dir / "03_legendary_breach.png")
    bpy.ops.render.render(write_still=True)

    bpy.ops.wm.save_as_mainfile(filepath=str(output_dir / "gacha_vault_composite.blend"))
    (output_dir / "HERO_SOURCE.txt").write_text(str(parts["chosen"]) + "\n", encoding="utf-8")


if __name__ == "__main__":
    qroot, vroot, out = cli_args()
    clear_scene()
    configure_render()
    render(build(qroot, vroot), out)

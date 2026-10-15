import math
import sys
from pathlib import Path

import bpy
from mathutils import Vector


def args_after_double_dash():
    if "--" not in sys.argv:
        raise SystemExit("Usage: blender -b -P build_scene.py -- <asset_root> <output_dir>")
    return sys.argv[sys.argv.index("--") + 1 :]


def clear_scene():
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)
    for datablocks in (bpy.data.meshes, bpy.data.curves, bpy.data.cameras, bpy.data.lights):
        for block in list(datablocks):
            if block.users == 0:
                datablocks.remove(block)


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
    scene.render.film_transparent = False

    if hasattr(scene, "eevee") and hasattr(scene.eevee, "taa_render_samples"):
        scene.eevee.taa_render_samples = 48

    scene.world.use_nodes = True
    background = scene.world.node_tree.nodes.get("Background")
    background.inputs["Color"].default_value = (0.003, 0.005, 0.012, 1.0)
    background.inputs["Strength"].default_value = 0.10

    view = scene.view_settings
    for look in ("AgX - Medium High Contrast", "Medium High Contrast"):
        try:
            view.look = look
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
    glare.threshold = 0.8
    glare.size = 7
    comp = tree.nodes.new("CompositorNodeComposite")
    tree.links.new(rl.outputs["Image"], glare.inputs["Image"])
    tree.links.new(glare.outputs["Image"], comp.inputs["Image"])


def make_principled(name, base_color, metallic=0.0, roughness=0.5, emission=None, emission_strength=0.0):
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value = (*base_color, 1.0)
    if "Metallic" in bsdf.inputs:
        bsdf.inputs["Metallic"].default_value = metallic
    if "Roughness" in bsdf.inputs:
        bsdf.inputs["Roughness"].default_value = roughness
    if emission is not None:
        if "Emission Color" in bsdf.inputs:
            bsdf.inputs["Emission Color"].default_value = (*emission, 1.0)
        elif "Emission" in bsdf.inputs:
            bsdf.inputs["Emission"].default_value = (*emission, 1.0)
        if "Emission Strength" in bsdf.inputs:
            bsdf.inputs["Emission Strength"].default_value = emission_strength
    return mat


def import_gltf(path: Path, label: str):
    if not path.exists():
        raise FileNotFoundError(path)
    before = {obj.name for obj in bpy.context.scene.objects}
    bpy.ops.import_scene.gltf(filepath=str(path))
    imported = [obj for obj in bpy.context.scene.objects if obj.name not in before]
    root = bpy.data.objects.new(label, None)
    bpy.context.scene.collection.objects.link(root)
    for obj in imported:
        if obj.parent is None:
            obj.parent = root
    return root, imported


def override_materials(objects, material):
    """Use stable neutral previs materials when the public transport mirror lacks kit textures."""
    for obj in objects:
        if obj.type != "MESH":
            continue
        obj.data.materials.clear()
        obj.data.materials.append(material)


def add_torus(name, location, major, minor, material, rotation=(math.radians(90), 0, 0)):
    bpy.ops.mesh.primitive_torus_add(
        align="WORLD",
        major_segments=96,
        minor_segments=12,
        location=location,
        rotation=rotation,
        major_radius=major,
        minor_radius=minor,
    )
    obj = bpy.context.object
    obj.name = name
    obj.data.materials.append(material)
    return obj


def add_box(name, location, scale, material, rotation=(0, 0, 0), bevel=0.0):
    bpy.ops.mesh.primitive_cube_add(location=location, rotation=rotation)
    obj = bpy.context.object
    obj.name = name
    obj.scale = scale
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    if bevel > 0:
        modifier = obj.modifiers.new("Bevel", "BEVEL")
        modifier.width = bevel
        modifier.segments = 3
    obj.data.materials.append(material)
    return obj


def add_area_light(name, location, energy, color, size, target):
    data = bpy.data.lights.new(name, type="AREA")
    data.energy = energy
    data.color = color
    data.shape = "DISK"
    data.size = size
    obj = bpy.data.objects.new(name, data)
    bpy.context.scene.collection.objects.link(obj)
    obj.location = location
    point_at(obj, target)
    return obj


def add_point_light(name, location, energy, color, radius=0.5):
    data = bpy.data.lights.new(name, type="POINT")
    data.energy = energy
    data.color = color
    data.shadow_soft_size = radius
    obj = bpy.data.objects.new(name, data)
    bpy.context.scene.collection.objects.link(obj)
    obj.location = location
    return obj


def point_at(obj, target):
    direction = Vector(target) - obj.location
    obj.rotation_euler = direction.to_track_quat("-Z", "Y").to_euler()


def add_camera(location, target, lens=42):
    data = bpy.data.cameras.new("VaultCamera")
    data.lens = lens
    data.sensor_width = 36
    obj = bpy.data.objects.new("VaultCamera", data)
    bpy.context.scene.collection.objects.link(obj)
    obj.location = location
    point_at(obj, target)
    bpy.context.scene.camera = obj
    return obj


def add_mechanical_ring_details(radius, y, z, material, count=12):
    for i in range(count):
        a = math.tau * i / count
        x = math.cos(a) * radius
        zz = z + math.sin(a) * radius
        block = add_box(
            f"SealBracket_{i:02d}",
            (x, y, zz),
            (0.13, 0.10, 0.055),
            material,
            rotation=(0, -a, 0),
            bevel=0.035,
        )
        block.rotation_euler[1] = -a


def build_scene(asset_root: Path):
    platforms = asset_root / "Platforms"
    props = asset_root / "Props"
    columns = asset_root / "Columns"

    frame_mat = make_principled("Frame_Neutral", (0.040, 0.050, 0.072), metallic=0.78, roughness=0.27)
    door_mat = make_principled("Door_Neutral", (0.018, 0.022, 0.032), metallic=0.86, roughness=0.18)
    stage_mat = make_principled("Stage_Neutral", (0.027, 0.032, 0.044), metallic=0.74, roughness=0.24)
    support_mat = make_principled("Support_Neutral", (0.022, 0.027, 0.040), metallic=0.82, roughness=0.31)
    clamp_mat = make_principled("Clamp_Neutral", (0.075, 0.082, 0.100), metallic=0.88, roughness=0.20)
    graphite = make_principled("Graphite_Blockout", (0.025, 0.030, 0.045), metallic=0.82, roughness=0.22)
    violet = make_principled(
        "Violet_Emission", (0.05, 0.01, 0.10), metallic=0.25, roughness=0.22,
        emission=(0.39, 0.06, 1.0), emission_strength=7.0,
    )
    gold = make_principled(
        "GoldWhite_Emission", (0.12, 0.07, 0.01), metallic=0.1, roughness=0.18,
        emission=(1.0, 0.58, 0.12), emission_strength=14.0,
    )
    floor_mat = make_principled("VoidFloor", (0.006, 0.008, 0.014), metallic=0.65, roughness=0.18)

    frame, frame_objects = import_gltf(platforms / "Door_Frame_SquareTall.gltf", "VaultFrame")
    override_materials(frame_objects, frame_mat)
    frame.location = (0, 3.15, 0)
    frame.scale = (1.16, 1.16, 1.16)

    left_door, left_objects = import_gltf(platforms / "Door_DarkMetal.gltf", "VaultDoor_Left")
    override_materials(left_objects, door_mat)
    left_door.location = (0, 3.03, 0.48)
    left_door.scale = (1.22, 1.22, 1.22)

    right_door, right_objects = import_gltf(platforms / "Door_DarkMetal.gltf", "VaultDoor_Right")
    override_materials(right_objects, door_mat)
    right_door.location = (0, 3.03, 0.48)
    right_door.scale = (-1.22, 1.22, 1.22)

    stage, stage_objects = import_gltf(platforms / "Platform_Round1.gltf", "RevealStage")
    override_materials(stage_objects, stage_mat)
    stage.location = (0, -0.15, 0)
    stage.scale = (1.18, 1.18, 1.18)

    for x in (-3.45, 3.45):
        support, support_objects = import_gltf(columns / "Column_Hollow.gltf", f"Support_{'L' if x < 0 else 'R'}")
        override_materials(support_objects, support_mat)
        support.location = (x, 3.20, 0)
        support.scale = (0.92, 0.92, 1.08)

    clamp_positions = [
        (-2.45, 2.75, 1.35, 0.0),
        (2.45, 2.75, 1.35, math.pi),
        (-2.45, 2.75, 4.10, 0.0),
        (2.45, 2.75, 4.10, math.pi),
    ]
    clamps = []
    for i, (x, y, z, rz) in enumerate(clamp_positions):
        clamp, clamp_objects = import_gltf(props / "Prop_Clamp.gltf", f"LockClamp_{i + 1}")
        override_materials(clamp_objects, clamp_mat)
        clamp.location = (x, y, z)
        clamp.scale = (1.75, 1.75, 1.75)
        clamp.rotation_euler = (math.radians(90), 0, rz)
        clamps.append(clamp)

    # Hero ring remains BLOCKOUT ONLY until this composition passes visual review.
    ring_outer = add_torus("SealRing_Outer", (0, 2.62, 2.65), 2.12, 0.105, graphite)
    ring_inner = add_torus("SealRing_Inner", (0, 2.56, 2.65), 1.70, 0.062, violet)
    add_mechanical_ring_details(2.10, 2.60, 2.65, graphite, count=12)
    seam = add_box("VaultSeam", (0, 2.48, 2.55), (0.022, 0.035, 1.83), violet, bevel=0.012)

    bpy.ops.mesh.primitive_plane_add(size=36, location=(0, 0, -0.035))
    void_floor = bpy.context.object
    void_floor.name = "ChamberFloor"
    void_floor.data.materials.append(floor_mat)

    add_area_light("Key_Left", (-5.0, -1.0, 6.5), 1050, (0.18, 0.10, 0.65), 4.2, (0, 2.5, 2.4))
    add_area_light("Key_Right", (5.0, -0.5, 5.2), 900, (0.08, 0.18, 0.55), 3.8, (0, 2.8, 2.5))
    add_area_light("Top_Rim", (0, 3.0, 8.0), 1200, (0.35, 0.12, 0.8), 3.2, (0, 3.0, 2.6))
    add_point_light("SeamGlow", (0, 2.0, 2.55), 700, (0.45, 0.08, 1.0), 0.45)
    add_area_light("Front_Fill", (0, -6.0, 4.0), 420, (0.10, 0.14, 0.22), 5.0, (0, 2.5, 2.0))

    camera = add_camera((0, -9.4, 2.25), (0, 2.65, 2.40), lens=42)

    return {
        "left_door": left_door,
        "right_door": right_door,
        "ring_inner": ring_inner,
        "seam": seam,
        "gold": gold,
        "camera": camera,
    }


def render(scene_parts, output_dir: Path):
    scene = bpy.context.scene
    output_dir.mkdir(parents=True, exist_ok=True)
    camera = scene_parts["camera"]

    camera.location = (0, -9.4, 2.25)
    camera.data.lens = 42
    point_at(camera, (0, 2.65, 2.40))
    scene.render.filepath = str(output_dir / "01_locked_hero.png")
    bpy.ops.render.render(write_still=True)

    camera.location = (0.35, -6.8, 2.05)
    camera.data.lens = 46
    point_at(camera, (0, 2.75, 2.55))
    scene.render.filepath = str(output_dir / "02_approach.png")
    bpy.ops.render.render(write_still=True)

    # Highest-rarity blocking only. Final fracture must use authored replacement geometry.
    left = scene_parts["left_door"]
    right = scene_parts["right_door"]
    left.location.x -= 1.05
    left.location.y -= 0.28
    left.rotation_euler[2] = math.radians(7)
    right.location.x += 1.05
    right.location.y -= 0.28
    right.rotation_euler[2] = math.radians(-7)
    scene_parts["ring_inner"].rotation_euler[1] += math.radians(19)
    scene_parts["seam"].data.materials.clear()
    scene_parts["seam"].data.materials.append(scene_parts["gold"])
    add_point_light("LegendaryCore", (0, 2.95, 2.55), 3200, (1.0, 0.46, 0.08), 0.35)
    add_area_light("LegendaryBacklight", (0, 3.35, 2.65), 4800, (1.0, 0.55, 0.13), 2.8, (0, -2.0, 2.5))
    camera.location = (0, -7.7, 2.20)
    camera.data.lens = 39
    point_at(camera, (0, 2.85, 2.48))
    scene.render.filepath = str(output_dir / "03_legendary_backlight_blockout.png")
    bpy.ops.render.render(write_still=True)

    bpy.ops.wm.save_as_mainfile(filepath=str(output_dir / "gacha_vault_previs.blend"))


if __name__ == "__main__":
    cli = args_after_double_dash()
    if len(cli) != 2:
        raise SystemExit("Expected <asset_root> <output_dir>")
    asset_root = Path(cli[0]).resolve()
    output_dir = Path(cli[1]).resolve()
    clear_scene()
    configure_render()
    parts = build_scene(asset_root)
    render(parts, output_dir)

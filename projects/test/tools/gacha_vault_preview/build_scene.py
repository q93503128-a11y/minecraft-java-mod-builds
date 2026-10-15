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
    scene.view_settings.exposure = 0.75
    if hasattr(scene, "eevee") and hasattr(scene.eevee, "taa_render_samples"):
        scene.eevee.taa_render_samples = 48

    scene.world.use_nodes = True
    background = scene.world.node_tree.nodes.get("Background")
    background.inputs["Color"].default_value = (0.003, 0.004, 0.009, 1.0)
    background.inputs["Strength"].default_value = 0.08

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
    glare.threshold = 1.45
    glare.size = 6
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
    for obj in objects:
        if obj.type == "MESH":
            obj.data.materials.clear()
            obj.data.materials.append(material)


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


def add_camera(location, target, lens=50):
    data = bpy.data.cameras.new("VaultCamera")
    data.lens = lens
    data.sensor_width = 36
    obj = bpy.data.objects.new("VaultCamera", data)
    bpy.context.scene.collection.objects.link(obj)
    obj.location = location
    point_at(obj, target)
    bpy.context.scene.camera = obj
    return obj


def add_quarter_ring(asset_path, prefix, center, scale, material):
    roots = []
    for i in range(4):
        root, objects = import_gltf(asset_path, f"{prefix}_{i + 1}")
        override_materials(objects, material)
        root.location = center
        root.scale = (scale, scale, scale)
        root.rotation_euler = (math.radians(90), math.radians(i * 90), 0)
        roots.append(root)
    return roots


def build_scene(asset_root: Path):
    platforms = asset_root / "Platforms"
    props = asset_root / "Props"
    columns = asset_root / "Columns"

    frame_mat = make_principled("Frame_Neutral", (0.075, 0.085, 0.115), metallic=0.82, roughness=0.25)
    door_mat = make_principled("Door_Neutral", (0.035, 0.041, 0.058), metallic=0.90, roughness=0.18)
    stage_mat = make_principled("Stage_Neutral", (0.047, 0.054, 0.070), metallic=0.78, roughness=0.25)
    support_mat = make_principled("Support_Neutral", (0.050, 0.057, 0.078), metallic=0.84, roughness=0.29)
    clamp_mat = make_principled("Clamp_Neutral", (0.12, 0.13, 0.16), metallic=0.90, roughness=0.18)
    ring_mat = make_principled("SealRing_Metal", (0.075, 0.080, 0.100), metallic=0.92, roughness=0.20)
    violet = make_principled(
        "Violet_Indicator", (0.020, 0.004, 0.045), metallic=0.15, roughness=0.22,
        emission=(0.25, 0.025, 0.72), emission_strength=4.0,
    )
    gold = make_principled(
        "GoldWhite_Emission", (0.10, 0.040, 0.003), metallic=0.08, roughness=0.18,
        emission=(1.0, 0.36, 0.04), emission_strength=10.0,
    )
    floor_mat = make_principled("VoidFloor", (0.010, 0.013, 0.022), metallic=0.70, roughness=0.21)

    frame, objects = import_gltf(platforms / "Door_Frame_SquareTall.gltf", "VaultFrame")
    override_materials(objects, frame_mat)
    frame.location = (0, 3.30, 0)
    frame.scale = (1.34, 1.34, 1.34)

    left_door, objects = import_gltf(platforms / "Door_DarkMetal.gltf", "VaultDoor_Left")
    override_materials(objects, door_mat)
    left_door.location = (0, 3.12, 0.55)
    left_door.scale = (1.36, 1.36, 1.36)

    right_door, objects = import_gltf(platforms / "Door_DarkMetal.gltf", "VaultDoor_Right")
    override_materials(objects, door_mat)
    right_door.location = (0, 3.12, 0.55)
    right_door.scale = (-1.36, 1.36, 1.36)

    stage, objects = import_gltf(platforms / "Platform_Round1.gltf", "RevealStage")
    override_materials(objects, stage_mat)
    stage.location = (0, -0.50, 0)
    stage.scale = (1.34, 1.34, 1.34)

    for x in (-4.18, 4.18):
        support, objects = import_gltf(columns / "Column_Hollow.gltf", f"Support_{'L' if x < 0 else 'R'}")
        override_materials(objects, support_mat)
        support.location = (x, 3.52, 0)
        support.scale = (0.90, 0.90, 1.18)

    # Real curved structural pieces form a dark mechanical halo around the door.
    outer_ring = add_quarter_ring(
        columns / "Column_MetalSupport_Curve.gltf",
        "SealSupportQuarter",
        (0, 2.78, 2.76),
        0.50,
        ring_mat,
    )

    # Physical locks at the four main stress points.
    clamp_positions = [
        (-2.30, 2.34, 2.76, 0.0),
        (2.30, 2.34, 2.76, math.pi),
        (0.0, 2.34, 5.05, math.pi / 2),
        (0.0, 2.34, 0.47, -math.pi / 2),
    ]
    clamps = []
    for i, (x, y, z, rz) in enumerate(clamp_positions):
        clamp, objects = import_gltf(props / "Prop_Clamp.gltf", f"LockClamp_{i + 1}")
        override_materials(objects, clamp_mat)
        clamp.location = (x, y, z)
        clamp.scale = (2.35, 2.35, 2.35)
        clamp.rotation_euler = (math.radians(90), 0, rz)
        clamps.append(clamp)

    indicators = [
        add_box("Indicator_Top", (0, 2.18, 4.47), (0.21, 0.028, 0.025), violet, bevel=0.014),
        add_box("Indicator_Bottom", (0, 2.18, 1.05), (0.21, 0.028, 0.025), violet, bevel=0.014),
        add_box("Indicator_Left", (-1.71, 2.18, 2.76), (0.025, 0.028, 0.21), violet, bevel=0.014),
        add_box("Indicator_Right", (1.71, 2.18, 2.76), (0.025, 0.028, 0.21), violet, bevel=0.014),
    ]
    seam = add_box("VaultSeam", (0, 2.14, 2.68), (0.014, 0.020, 1.64), violet, bevel=0.007)

    bpy.ops.mesh.primitive_plane_add(size=42, location=(0, 0, -0.035))
    floor = bpy.context.object
    floor.name = "ChamberFloor"
    floor.data.materials.append(floor_mat)

    # Form-revealing lighting; the machine stays dark but readable.
    add_area_light("Front_Key", (-2.2, -5.5, 5.2), 980, (0.16, 0.18, 0.28), 4.0, (0, 3.0, 2.55))
    add_area_light("Right_Rim", (5.2, 0.8, 5.8), 1250, (0.05, 0.17, 0.46), 3.5, (0, 3.1, 2.8))
    add_area_light("Left_Rim", (-5.4, 1.8, 4.6), 760, (0.22, 0.07, 0.34), 3.1, (0, 3.1, 2.6))
    add_area_light("Top_Rim", (0, 3.5, 8.6), 1100, (0.27, 0.11, 0.43), 3.0, (0, 3.0, 2.7))
    add_area_light("Floor_Fill", (0, -1.0, 0.7), 430, (0.07, 0.09, 0.14), 4.0, (0, 3.0, 1.8))

    camera = add_camera((1.05, -11.0, 2.02), (0, 3.0, 2.52), lens=50)
    return {
        "left_door": left_door,
        "right_door": right_door,
        "outer_ring": outer_ring,
        "indicators": indicators,
        "seam": seam,
        "gold": gold,
        "camera": camera,
    }


def set_material(obj, material):
    obj.data.materials.clear()
    obj.data.materials.append(material)


def render(scene_parts, output_dir: Path):
    scene = bpy.context.scene
    output_dir.mkdir(parents=True, exist_ok=True)
    camera = scene_parts["camera"]

    camera.location = (1.05, -11.0, 2.02)
    camera.data.lens = 50
    point_at(camera, (0, 3.0, 2.52))
    scene.render.filepath = str(output_dir / "01_locked_hero.png")
    bpy.ops.render.render(write_still=True)

    camera.location = (-0.65, -7.9, 1.75)
    camera.data.lens = 54
    point_at(camera, (0.05, 3.0, 2.66))
    scene.render.filepath = str(output_dir / "02_approach.png")
    bpy.ops.render.render(write_still=True)

    # Highest-rarity lighting/blocking only; fracture geometry is still not authored.
    left = scene_parts["left_door"]
    right = scene_parts["right_door"]
    left.location.x -= 1.12
    left.location.y -= 0.24
    left.rotation_euler[2] = math.radians(5)
    right.location.x += 1.12
    right.location.y -= 0.24
    right.rotation_euler[2] = math.radians(-5)
    for i, quarter in enumerate(scene_parts["outer_ring"]):
        quarter.rotation_euler[1] += math.radians(4 if i % 2 == 0 else -4)
    for indicator in scene_parts["indicators"]:
        set_material(indicator, scene_parts["gold"])
    set_material(scene_parts["seam"], scene_parts["gold"])

    add_box("Legendary_Backlight_Blockout", (0, 3.45, 2.70), (1.46, 0.020, 1.86), scene_parts["gold"], bevel=0.10)
    add_area_light("Legendary_Backlight", (0, 3.28, 2.70), 3000, (1.0, 0.40, 0.055), 2.5, (0, -2.0, 2.45))
    add_point_light("Legendary_FrontBounce", (0, 1.8, 2.7), 720, (1.0, 0.24, 0.025), 0.45)

    camera.location = (0.42, -8.85, 1.92)
    camera.data.lens = 48
    point_at(camera, (0, 3.0, 2.52))
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

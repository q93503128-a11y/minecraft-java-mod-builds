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
    background.inputs["Color"].default_value = (0.002, 0.003, 0.007, 1.0)
    background.inputs["Strength"].default_value = 0.07

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
    glare.threshold = 1.25
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
        if obj.type != "MESH":
            continue
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


def add_camera(location, target, lens=48):
    data = bpy.data.cameras.new("VaultCamera")
    data.lens = lens
    data.sensor_width = 36
    obj = bpy.data.objects.new("VaultCamera", data)
    bpy.context.scene.collection.objects.link(obj)
    obj.location = location
    point_at(obj, target)
    bpy.context.scene.camera = obj
    return obj


def add_quarter_ring(asset_path, prefix, center, scale, material, rotation_offset=0.0):
    roots = []
    for i in range(4):
        root, objects = import_gltf(asset_path, f"{prefix}_{i + 1}")
        override_materials(objects, material)
        root.location = center
        root.scale = (scale, scale, scale)
        root.rotation_euler = (
            math.radians(90),
            math.radians(rotation_offset + i * 90),
            0,
        )
        roots.append(root)
    return roots


def build_scene(asset_root: Path):
    platforms = asset_root / "Platforms"
    props = asset_root / "Props"
    columns = asset_root / "Columns"

    frame_mat = make_principled("Frame_Neutral", (0.035, 0.043, 0.060), metallic=0.82, roughness=0.25)
    door_mat = make_principled("Door_Neutral", (0.013, 0.016, 0.024), metallic=0.90, roughness=0.19)
    stage_mat = make_principled("Stage_Neutral", (0.021, 0.026, 0.036), metallic=0.78, roughness=0.26)
    support_mat = make_principled("Support_Neutral", (0.018, 0.022, 0.032), metallic=0.84, roughness=0.31)
    clamp_mat = make_principled("Clamp_Neutral", (0.070, 0.076, 0.090), metallic=0.90, roughness=0.18)
    ring_outer_mat = make_principled("SealRing_Outer_Metal", (0.030, 0.036, 0.050), metallic=0.92, roughness=0.20)
    ring_inner_mat = make_principled("SealRing_Inner_Metal", (0.055, 0.058, 0.070), metallic=0.88, roughness=0.23)
    violet = make_principled(
        "Violet_Indicator", (0.025, 0.004, 0.055), metallic=0.2, roughness=0.24,
        emission=(0.28, 0.035, 0.85), emission_strength=5.5,
    )
    gold = make_principled(
        "GoldWhite_Emission", (0.10, 0.045, 0.004), metallic=0.1, roughness=0.18,
        emission=(1.0, 0.42, 0.055), emission_strength=11.0,
    )
    floor_mat = make_principled("VoidFloor", (0.004, 0.006, 0.011), metallic=0.72, roughness=0.20)

    frame, frame_objects = import_gltf(platforms / "Door_Frame_SquareTall.gltf", "VaultFrame")
    override_materials(frame_objects, frame_mat)
    frame.location = (0, 3.30, 0)
    frame.scale = (1.28, 1.28, 1.28)

    left_door, left_objects = import_gltf(platforms / "Door_DarkMetal.gltf", "VaultDoor_Left")
    override_materials(left_objects, door_mat)
    left_door.location = (0, 3.14, 0.48)
    left_door.scale = (1.31, 1.31, 1.31)

    right_door, right_objects = import_gltf(platforms / "Door_DarkMetal.gltf", "VaultDoor_Right")
    override_materials(right_objects, door_mat)
    right_door.location = (0, 3.14, 0.48)
    right_door.scale = (-1.31, 1.31, 1.31)

    stage, stage_objects = import_gltf(platforms / "Platform_Round1.gltf", "RevealStage")
    override_materials(stage_objects, stage_mat)
    stage.location = (0, -0.35, 0)
    stage.scale = (1.30, 1.30, 1.30)

    for x in (-4.05, 4.05):
        support, support_objects = import_gltf(columns / "Column_Hollow.gltf", f"Support_{'L' if x < 0 else 'R'}")
        override_materials(support_objects, support_mat)
        support.location = (x, 3.45, 0)
        support.scale = (0.88, 0.88, 1.15)

    # Real kit geometry replaces the generic neon torus blockout.
    outer_ring = add_quarter_ring(
        columns / "Column_MetalSupport_Curve.gltf",
        "OuterSealQuarter",
        (0, 2.72, 2.72),
        0.49,
        ring_outer_mat,
        rotation_offset=0,
    )
    inner_ring = add_quarter_ring(
        platforms / "Platform_CenterPlate_Curve.gltf",
        "InnerSealQuarter",
        (0, 2.58, 2.72),
        0.80,
        ring_inner_mat,
        rotation_offset=45,
    )

    # Four physical clamps sit at the cardinal locking points and remain readable.
    clamp_positions = [
        (-2.16, 2.36, 2.72, 0.0),
        (2.16, 2.36, 2.72, math.pi),
        (0.0, 2.36, 4.88, math.pi / 2),
        (0.0, 2.36, 0.56, -math.pi / 2),
    ]
    clamps = []
    for i, (x, y, z, rz) in enumerate(clamp_positions):
        clamp, clamp_objects = import_gltf(props / "Prop_Clamp.gltf", f"LockClamp_{i + 1}")
        override_materials(clamp_objects, clamp_mat)
        clamp.location = (x, y, z)
        clamp.scale = (2.10, 2.10, 2.10)
        clamp.rotation_euler = (math.radians(90), 0, rz)
        clamps.append(clamp)

    # Sparse emissive status marks only; the machinery itself stays metal.
    indicators = [
        add_box("Indicator_Top", (0, 2.28, 4.40), (0.24, 0.035, 0.030), violet, bevel=0.018),
        add_box("Indicator_Bottom", (0, 2.28, 1.04), (0.24, 0.035, 0.030), violet, bevel=0.018),
        add_box("Indicator_Left", (-1.68, 2.28, 2.72), (0.030, 0.035, 0.24), violet, bevel=0.018),
        add_box("Indicator_Right", (1.68, 2.28, 2.72), (0.030, 0.035, 0.24), violet, bevel=0.018),
    ]
    seam = add_box("VaultSeam", (0, 2.30, 2.62), (0.018, 0.025, 1.58), violet, bevel=0.009)

    bpy.ops.mesh.primitive_plane_add(size=40, location=(0, 0, -0.035))
    floor = bpy.context.object
    floor.name = "ChamberFloor"
    floor.data.materials.append(floor_mat)

    # Keep most of the machine dark; lighting reveals form instead of painting it blue.
    add_area_light("Cool_Rim_Left", (-5.8, 0.0, 6.6), 720, (0.10, 0.13, 0.36), 3.8, (0, 3.0, 2.5))
    add_area_light("Cool_Rim_Right", (5.5, 1.0, 5.4), 540, (0.05, 0.14, 0.28), 3.6, (0, 3.0, 2.7))
    add_area_light("Top_Rim", (0, 3.3, 8.5), 850, (0.22, 0.08, 0.42), 3.4, (0, 3.0, 2.7))
    add_area_light("Front_Fill", (0, -7.5, 3.2), 260, (0.07, 0.075, 0.10), 4.5, (0, 3.0, 2.4))

    camera = add_camera((0, -10.6, 1.95), (0, 2.95, 2.45), lens=48)

    return {
        "left_door": left_door,
        "right_door": right_door,
        "inner_ring": inner_ring,
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

    camera.location = (0, -10.6, 1.95)
    camera.data.lens = 48
    point_at(camera, (0, 2.95, 2.45))
    scene.render.filepath = str(output_dir / "01_locked_hero.png")
    bpy.ops.render.render(write_still=True)

    camera.location = (0.48, -7.55, 1.72)
    camera.data.lens = 52
    point_at(camera, (-0.10, 3.0, 2.62))
    scene.render.filepath = str(output_dir / "02_approach.png")
    bpy.ops.render.render(write_still=True)

    # Highest-rarity blocking only. Authored fracture geometry still comes later.
    left = scene_parts["left_door"]
    right = scene_parts["right_door"]
    left.location.x -= 1.16
    left.location.y -= 0.26
    left.rotation_euler[2] = math.radians(6)
    right.location.x += 1.16
    right.location.y -= 0.26
    right.rotation_euler[2] = math.radians(-6)
    for i, quarter in enumerate(scene_parts["inner_ring"]):
        quarter.rotation_euler[1] += math.radians(11 if i % 2 == 0 else -11)
    for indicator in scene_parts["indicators"]:
        set_material(indicator, scene_parts["gold"])
    set_material(scene_parts["seam"], scene_parts["gold"])

    # Backlight surface is a previs stand-in for the final volumetric fracture flash.
    core = add_box("Legendary_Backlight_Blockout", (0, 3.48, 2.62), (1.42, 0.025, 1.82), scene_parts["gold"], bevel=0.12)
    add_point_light("LegendaryCore", (0, 2.95, 2.62), 1450, (1.0, 0.34, 0.045), 0.35)
    add_area_light("LegendaryBacklight", (0, 3.30, 2.62), 2200, (1.0, 0.44, 0.08), 2.4, (0, -2.0, 2.5))

    camera.location = (0, -8.65, 1.92)
    camera.data.lens = 46
    point_at(camera, (0, 3.0, 2.48))
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

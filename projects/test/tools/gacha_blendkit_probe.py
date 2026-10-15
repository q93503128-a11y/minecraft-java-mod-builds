import json
import math
import sys
import urllib.parse
import urllib.request
import uuid
from pathlib import Path

import bpy
from mathutils import Vector

QUERY_NAME = "Sci fi Titan Core Gate"
API_HOSTS = [
    "https://www.blendkit.com/api/v1/search/",
    "https://www.blenderkit.com/api/v1/search/",
]


def cli_output_dir():
    if "--" not in sys.argv:
        raise SystemExit("Expected -- <output_dir>")
    args = sys.argv[sys.argv.index("--") + 1:]
    if len(args) != 1:
        raise SystemExit("Expected exactly one output directory")
    out = Path(args[0]).resolve()
    out.mkdir(parents=True, exist_ok=True)
    return out


def http_bytes(url, timeout=60):
    req = urllib.request.Request(url, headers={"User-Agent": "moon-test-asset-previs/1.0"})
    with urllib.request.urlopen(req, timeout=timeout) as response:
        return response.read(), response.headers.get("Content-Type", "")


def get_json(url, timeout=60):
    raw, _ = http_bytes(url, timeout=timeout)
    return json.loads(raw.decode("utf-8"))


def search_asset(out_dir):
    params = {
        "query": f"{QUERY_NAME} asset_type:model is_free:true",
        "dict_parameters": "1",
        "page_size": "30",
        "addon_version": "3.21.3",
        "blender_version": bpy.app.version_string,
    }
    errors = []
    for host in API_HOSTS:
        url = host + "?" + urllib.parse.urlencode(params)
        try:
            data = get_json(url)
            (out_dir / "SEARCH_RESULT.json").write_text(
                json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8"
            )
            results = data.get("results", [])
            if not results:
                errors.append(f"{host}: no results")
                continue
            exact = [r for r in results if r.get("name", "").strip().lower() == QUERY_NAME.lower()]
            if exact:
                return exact[0]
            # Strict enough fallback: require all title tokens.
            tokens = {t.lower() for t in QUERY_NAME.split()}
            scored = []
            for r in results:
                name_tokens = set(r.get("name", "").lower().replace("-", " ").split())
                scored.append((len(tokens & name_tokens), r))
            scored.sort(key=lambda x: x[0], reverse=True)
            if scored and scored[0][0] >= 4:
                return scored[0][1]
            errors.append(f"{host}: no sufficiently close title")
        except Exception as exc:
            errors.append(f"{host}: {type(exc).__name__}: {exc}")
    raise RuntimeError("Blendkit public search failed: " + " | ".join(errors))


def asset_is_free(asset):
    # Blendkit schema changed over time. Treat any explicit paid/full marker as a hard stop.
    plan = str(asset.get("plan", asset.get("availability", ""))).lower()
    if plan in {"full", "paid", "subscription"}:
        return False
    if asset.get("isFree") is False or asset.get("is_free") is False:
        return False
    return True


def download_blend(asset, out_dir):
    if not asset_is_free(asset):
        raise RuntimeError(f"Selected asset is not free: {asset.get('name')} plan={asset.get('plan')}")

    files = asset.get("files", [])
    if not files:
        raise RuntimeError("Selected asset exposes no downloadable files")

    def priority(entry):
        ft = str(entry.get("fileType", entry.get("file_type", ""))).lower()
        order = {
            "resolution_1k": 0,
            "resolution_0_5k": 1,
            "blend": 2,
            "resolution_2k": 3,
            "resolution_4k": 4,
        }
        return order.get(ft, 50)

    scene_uuid = str(uuid.uuid4())
    attempts = []
    for entry in sorted(files, key=priority):
        endpoint = entry.get("downloadUrl") or entry.get("download_url")
        if not endpoint:
            continue
        try:
            sep = "&" if "?" in endpoint else "?"
            resolved_raw, content_type = http_bytes(
                endpoint + sep + urllib.parse.urlencode({"scene_uuid": scene_uuid}), timeout=90
            )
            resolved_url = None
            if "json" in content_type.lower() or resolved_raw[:1] in (b"{", b"["):
                payload = json.loads(resolved_raw.decode("utf-8"))
                resolved_url = (
                    payload.get("filePath")
                    or payload.get("file_path")
                    or payload.get("download_url")
                    or payload.get("url")
                )
            if resolved_url:
                data, _ = http_bytes(resolved_url, timeout=180)
            else:
                data = resolved_raw
            if len(data) < 1024 or data[:4] != b"BLEN":
                attempts.append(
                    f"{entry.get('fileType')}: unexpected payload {len(data)} bytes, head={data[:16]!r}"
                )
                continue
            dest = out_dir / "source_asset.blend"
            dest.write_bytes(data)
            return dest, entry
        except Exception as exc:
            attempts.append(f"{entry.get('fileType')}: {type(exc).__name__}: {exc}")
    raise RuntimeError("Anonymous free-asset download failed: " + " | ".join(attempts))


def point_at(obj, target):
    obj.rotation_euler = (Vector(target) - obj.location).to_track_quat("-Z", "Y").to_euler()


def mesh_bounds(objects):
    pts = []
    for obj in objects:
        if obj.type != "MESH" or obj.hide_render:
            continue
        for corner in obj.bound_box:
            pts.append(obj.matrix_world @ Vector(corner))
    if not pts:
        raise RuntimeError("Downloaded Blendkit asset contains no renderable mesh")
    lo = Vector((min(p.x for p in pts), min(p.y for p in pts), min(p.z for p in pts)))
    hi = Vector((max(p.x for p in pts), max(p.y for p in pts), max(p.z for p in pts)))
    return lo, hi


def normalize_scene():
    # Remove author cameras/lights only; preserve authored mesh/material/VFX objects.
    for obj in list(bpy.data.objects):
        if obj.type in {"CAMERA", "LIGHT"}:
            bpy.data.objects.remove(obj, do_unlink=True)

    scene_objects = list(bpy.context.scene.objects)
    lo, hi = mesh_bounds(scene_objects)
    center = (lo + hi) * 0.5
    size = hi - lo
    max_dim = max(size.x, size.y, size.z)
    if max_dim <= 0:
        raise RuntimeError("Invalid candidate bounds")

    root = bpy.data.objects.new("PrevisCandidateRoot", None)
    bpy.context.scene.collection.objects.link(root)
    top_level = [o for o in scene_objects if o.parent is None]
    for obj in top_level:
        obj.parent = root

    target_height = 5.8
    scale = target_height / max_dim
    root.scale = (scale, scale, scale)
    root.location = (-center.x * scale, -center.y * scale, -lo.z * scale)
    return root, target_height


def make_material(name, color, metallic=0.0, roughness=0.5):
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value = (*color, 1)
    bsdf.inputs["Metallic"].default_value = metallic
    bsdf.inputs["Roughness"].default_value = roughness
    return mat


def setup_render(out_dir, root, target_height):
    scene = bpy.context.scene
    try:
        scene.render.engine = "BLENDER_EEVEE_NEXT"
    except Exception:
        scene.render.engine = "BLENDER_EEVEE"
    scene.render.resolution_x = 1280
    scene.render.resolution_y = 720
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = "PNG"
    scene.view_settings.exposure = 0.45

    scene.world.use_nodes = True
    bg = scene.world.node_tree.nodes.get("Background")
    bg.inputs["Color"].default_value = (0.004, 0.005, 0.010, 1)
    bg.inputs["Strength"].default_value = 0.12

    bpy.ops.mesh.primitive_plane_add(size=30, location=(0, 0, -0.03))
    floor = bpy.context.object
    floor.name = "InspectionFloor"
    floor.data.materials.append(make_material("InspectionFloorMat", (0.012, 0.016, 0.024), 0.55, 0.24))

    def area(name, loc, energy, color, size, target):
        data = bpy.data.lights.new(name, "AREA")
        data.energy = energy
        data.color = color
        data.shape = "DISK"
        data.size = size
        obj = bpy.data.objects.new(name, data)
        bpy.context.scene.collection.objects.link(obj)
        obj.location = loc
        point_at(obj, target)

    target = (0, 0, target_height * 0.46)
    area("Key", (-5.5, -6.0, 6.5), 1050, (0.82, 0.88, 1.0), 4.0, target)
    area("Rim", (5.5, 0.8, 5.8), 1200, (0.10, 0.28, 0.75), 3.0, target)
    area("Warm", (-4.2, 2.0, 3.8), 520, (0.72, 0.20, 0.05), 2.5, target)

    data = bpy.data.cameras.new("InspectionCamera")
    data.lens = 55
    cam = bpy.data.objects.new("InspectionCamera", data)
    bpy.context.scene.collection.objects.link(cam)
    bpy.context.scene.camera = cam

    views = [
        ("01_front.png", (0, -9.5, 3.0), target, 55),
        ("02_three_quarter.png", (6.6, -7.3, 3.4), target, 58),
        ("03_close.png", (2.2, -6.1, 2.8), (0, 0, target_height * 0.48), 62),
    ]
    for filename, loc, look, lens in views:
        cam.location = loc
        cam.data.lens = lens
        point_at(cam, look)
        scene.render.filepath = str(out_dir / filename)
        bpy.ops.render.render(write_still=True)

    bpy.ops.wm.save_as_mainfile(filepath=str(out_dir / "hero_candidate.blend"))


def main():
    out_dir = cli_output_dir()
    asset = search_asset(out_dir)
    (out_dir / "SELECTED_ASSET.json").write_text(
        json.dumps(asset, indent=2, ensure_ascii=False), encoding="utf-8"
    )
    source, selected_file = download_blend(asset, out_dir)
    provenance = {
        "requested_name": QUERY_NAME,
        "selected_name": asset.get("name"),
        "asset_base_id": asset.get("assetBaseId") or asset.get("asset_base_id"),
        "asset_id": asset.get("id"),
        "author": asset.get("author"),
        "license": asset.get("license"),
        "plan": asset.get("plan"),
        "selected_file_type": selected_file.get("fileType"),
        "source": "Blendkit public API; anonymous free-asset path only",
        "minecraft_runtime_use": "NOT YET APPROVED",
        "visual_approval": "NOT YET GRANTED",
    }
    (out_dir / "PROVENANCE.json").write_text(
        json.dumps(provenance, indent=2, ensure_ascii=False), encoding="utf-8"
    )

    bpy.ops.wm.open_mainfile(filepath=str(source))
    root, target_height = normalize_scene()
    setup_render(out_dir, root, target_height)


if __name__ == "__main__":
    main()

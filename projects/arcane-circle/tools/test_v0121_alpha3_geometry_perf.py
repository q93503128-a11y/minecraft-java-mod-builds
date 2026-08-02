#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"
TRACKER = JAVA / "client/WorldMagicTracker.java"
SIGNATURE = JAVA / "client/SignatureGeometry.java"

tracker = TRACKER.read_text(encoding="utf-8")
signature = SIGNATURE.read_text(encoding="utf-8")
properties = (ROOT / "gradle.properties").read_text(encoding="utf-8")
main = (JAVA / "ArcaneCircle.java").read_text(encoding="utf-8")
index = json.loads(
    (ROOT / "src/main/resources/data/arcanecircle/spell_catalog/index.json").read_text(encoding="utf-8")
)

required_tracker = (
    "MAX_CHARGE_PRIMITIVES = 320",
    "MAX_RELEASE_PRIMITIVES = 512",
    "MAX_FRAME_PRIMITIVES = 1024",
    "MAX_RENDER_DISTANCE_SQR",
    "progressStep",
    "GeometryKey.charge",
    "for (int ring = 0; ring < spell.circle(); ring++)",
    "List<VoxelShape> geometry",
    "SignatureGeometry.append",
    "submitShapeOutline",
)
missing_tracker = [token for token in required_tracker if token not in tracker]
if missing_tracker:
    raise SystemExit(f"bounded world geometry tracker missing: {missing_tracker}")

required_signature = (
    "static void append",
    "meteor(",
    "tornado(",
    "portal(",
    "cage(",
    "wall(",
    "dome(",
    "shapes.size() < budget",
)
missing_signature = [token for token in required_signature if token not in signature]
if missing_signature:
    raise SystemExit(f"bounded signature geometry missing: {missing_signature}")

for label, source in (("WorldMagicTracker", tracker), ("SignatureGeometry", signature)):
    if "Shapes.or(" in source:
        raise SystemExit(f"{label} still performs combinatorial VoxelShape unions")
    if "joinUnoptimized" in source:
        raise SystemExit(f"{label} still performs VoxelShape boolean joins")

if tracker.count("for (int ring = 0; ring < spell.circle(); ring++)") != 1:
    raise SystemExit("1C-9C primary concentric ring loop changed unexpectedly")
if "while (RELEASES.size() >= MAX_RELEASE_VISUALS)" not in tracker:
    raise SystemExit("release visual retention cap missing")
if "if (submitted >= MAX_FRAME_PRIMITIVES)" not in tracker:
    raise SystemExit("per-frame primitive cap missing")

if "mod_version=0.12.1-alpha.5" not in properties:
    raise SystemExit("alpha.3 Gradle version missing")
if 'VERSION = "0.12.1-alpha.5"' not in main:
    raise SystemExit("alpha.3 runtime version missing")
if index.get("version") != "0.12.1-alpha.5":
    raise SystemExit("alpha.3 catalogue version missing")
if index.get("release_geometry") != "bounded_primitive_batches":
    raise SystemExit("bounded primitive catalogue metadata missing")

particle_files = []
for path in (JAVA / "magic").glob("*.java"):
    if ".sendParticles(" in path.read_text(encoding="utf-8"):
        particle_files.append(path.name)
if particle_files:
    raise SystemExit(f"particle-centered magic code remains: {particle_files}")

print("Arcane Circle v0.12.1-alpha.5 bounded geometry freeze contract: PASS")

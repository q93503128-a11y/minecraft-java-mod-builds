#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"
tracker = (JAVA / "client/WorldMagicTracker.java").read_text(encoding="utf-8")
properties = (ROOT / "gradle.properties").read_text(encoding="utf-8")
main = (JAVA / "ArcaneCircle.java").read_text(encoding="utf-8")
index = json.loads((ROOT / "src/main/resources/data/arcanecircle/spell_catalog/index.json").read_text(encoding="utf-8"))

required = (
    "RELEASE_FRAME_NS", "meteorGeometry", "tornadoGeometry", "portalGeometry",
    "forceCageGeometry", "barrierWallGeometry", "barrierDomeGeometry",
    '"meteor_swarm"', '"control_weather"', '"gate"', '"forcecage"',
    '"wall_of_force"', '"antimagic_field"',
    "for (int ring = 0; ring < spell.circle(); ring++)",
    "Math.max(0.85F, baseWidth * 0.90F)", "withAlpha"
)
missing = [token for token in required if token not in tracker]
if missing:
    signature_start = tracker.find("private static Signature signature")
    print("--- decoded signature snippet ---")
    print(tracker[signature_start:signature_start + 1100])
    print("--- decoded alpha lines ---")
    print("\n".join(line for line in tracker.splitlines() if "lpha" in line or "ALPHA" in line))
    raise SystemExit(f"signature geometry contract missing: {missing}")
if "Math.max(1.2F, baseWidth * 1.25F)" in tracker:
    raise SystemExit("legacy thick line width remains")
if "mod_version=0.12.1-alpha.2" not in properties or 'VERSION = "0.12.1-alpha.2"' not in main:
    raise SystemExit("alpha.2 version was not applied")
if index.get("version") != "0.12.1-alpha.2" or index.get("release_geometry") != "animated_signature_meshes":
    raise SystemExit("signature geometry catalogue metadata missing")
if list((ROOT / "tools").glob("v0121_alpha2_tracker.part-*")):
    raise SystemExit("staging chunks were not removed")
particle_files = []
for path in (JAVA / "magic").glob("*.java"):
    if ".sendParticles(" in path.read_text(encoding="utf-8"):
        particle_files.append(path.name)
if particle_files:
    raise SystemExit(f"particle-centered magic code remains: {particle_files}")
print("Arcane Circle v0.12.1-alpha.2 animated signature geometry contract: PASS")

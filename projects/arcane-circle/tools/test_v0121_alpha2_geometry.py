#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"
tracker = (JAVA / "client/WorldMagicTracker.java").read_text(encoding="utf-8")
signatures = (JAVA / "client/SignatureGeometry.java").read_text(encoding="utf-8")
properties = (ROOT / "gradle.properties").read_text(encoding="utf-8")
main = (JAVA / "ArcaneCircle.java").read_text(encoding="utf-8")
index = json.loads((ROOT / "src/main/resources/data/arcanecircle/spell_catalog/index.json").read_text(encoding="utf-8"))

tracker_required = (
    "SignatureGeometry.build(spell, direction, range)",
    "for (int ring = 0; ring < spell.circle(); ring++)",
    "Math.max(0.85F, baseWidth * 0.90F)",
    "int ringPoints = 48 + spell.circle() * 5;",
)
signature_required = (
    '"meteor_swarm"', '"control_weather"', '"gate"', '"forcecage"',
    '"wall_of_force"', '"antimagic_field"',
    "meteor(", "tornado(", "portal(", "cage(", "wall(", "dome(",
)
missing = [token for token in tracker_required if token not in tracker]
missing += [token for token in signature_required if token not in signatures]
if missing:
    raise SystemExit(f"signature geometry contract missing: {missing}")
if "Math.max(1.2F, baseWidth * 1.25F)" in tracker:
    raise SystemExit("legacy thick line width remains")
if "mod_version=0.12.1-alpha.2" not in properties or 'VERSION = "0.12.1-alpha.2"' not in main:
    raise SystemExit("alpha.2 version was not applied")
if index.get("version") != "0.12.1-alpha.2" or index.get("release_geometry") != "per_spell_signature_geometry":
    raise SystemExit("signature geometry catalogue metadata missing")
if list((ROOT / "tools").glob("v0121_alpha2_tracker.part-*")):
    raise SystemExit("staging chunks were not removed")
particle_files = []
for path in (JAVA / "magic").glob("*.java"):
    if ".sendParticles(" in path.read_text(encoding="utf-8"):
        particle_files.append(path.name)
if particle_files:
    raise SystemExit(f"particle-centered magic code remains: {particle_files}")
print("Arcane Circle v0.12.1-alpha.2 per-spell signature geometry contract: PASS")

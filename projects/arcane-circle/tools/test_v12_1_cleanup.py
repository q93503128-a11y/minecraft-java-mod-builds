#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"

properties = (ROOT / "gradle.properties").read_text(encoding="utf-8")
network = (JAVA / "network/ArcaneNetwork.java").read_text(encoding="utf-8")
tracker = (JAVA / "client/WorldMagicTracker.java").read_text(encoding="utf-8")
service = (JAVA / "magic/WorldMagicService.java").read_text(encoding="utf-8")

if "mod_version=0.12.1-alpha.2" not in properties:
    raise SystemExit("v0.12.1 version missing")
if "ninefold-arcana-12-1" not in network:
    raise SystemExit("v0.12.1 network protocol missing")
for token in ("SubmitCustomGeometryEvent", "submitShapeOutline", "buildCharge", "buildRelease",
              "for (int ring = 0; ring < spell.circle(); ring++)"):
    if token not in tracker:
        raise SystemExit(f"world geometry contract missing: {token}")
for token in ("PacketDistributor.sendToPlayersNear", "WorldMagicPayload", "public static void release"):
    if token not in service:
        raise SystemExit(f"multiplayer visual service missing: {token}")

particle_files = []
for path in (JAVA / "magic").glob("*.java"):
    text = path.read_text(encoding="utf-8")
    if ".sendParticles(" in text:
        particle_files.append(path.name)
if particle_files:
    raise SystemExit(f"particle-centered magic code remains: {particle_files}")

sigil = (JAVA / "magic/SpellSigilService.java").read_text(encoding="utf-8")
if "@Deprecated" not in sigil or "WorldMagicTracker" not in sigil:
    raise SystemExit("legacy particle sigil service was not retired")

print("Arcane Circle v0.12.1 all-circle non-particle core visual contract: PASS")

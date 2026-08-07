from __future__ import annotations

from hashlib import sha256
from pathlib import Path
import subprocess
import sys

ROOT = Path.cwd()
WORKSPACE = ROOT.parent.parent
MIGRATION = WORKSPACE / ".github/scripts/arcane-circle/apply_v0121_alpha17_spell_presentation_v2.py"

TRACKED = [
    ROOT / "gradle.properties",
    ROOT / "src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java",
    ROOT / "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellPresentationProfile.java",
    ROOT / "src/main/java/kr/moonseungjun/arcanecircle/magic/WorldMagicService.java",
    ROOT / "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellKineticsService.java",
    ROOT / "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java",
    ROOT / "src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java",
    ROOT / "src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneWorldMesh.java",
]


def digest() -> str:
    h = sha256()
    for path in TRACKED:
        h.update(path.as_posix().encode())
        h.update(path.read_bytes())
    return h.hexdigest()


def require(text: str, token: str, label: str) -> None:
    if token not in text:
        raise SystemExit(f"missing {label}: {token}")


def main() -> None:
    profile = (ROOT / "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellPresentationProfile.java").read_text(encoding="utf-8")
    service = (ROOT / "src/main/java/kr/moonseungjun/arcanecircle/magic/WorldMagicService.java").read_text(encoding="utf-8")
    tracker = (ROOT / "src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java").read_text(encoding="utf-8")
    mesh = (ROOT / "src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneWorldMesh.java").read_text(encoding="utf-8")
    kinetics = (ROOT / "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellKineticsService.java").read_text(encoding="utf-8")
    casting = (ROOT / "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java").read_text(encoding="utf-8")

    require(profile, 'put("meteor_swarm", SigilStyle.SKY_RITUAL, MotionStyle.SKY_DROP, 16.0', "giant meteor sky ritual")
    require(profile, 'put("power_word_kill", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 2.35', "compact 9C death seal")
    require(profile, 'put("gate", SigilStyle.PORTAL_GATE, MotionStyle.PORTAL, 10.5', "giant gate array")
    require(profile, 'put("magic_missile", SigilStyle.FRONT_COMPACT, MotionStyle.MISSILE_SWARM, 0.92', "missile launch identity")
    require(profile, 'put("void_lance", SigilStyle.FRONT_LANCE, MotionStyle.LANCE, 1.45, 6, 2, 72', "fast lance identity")
    if 'put("meteor_swarm", SKY_RITUAL' in profile:
        raise SystemExit("unqualified authored enum constants remain")

    require(service, "tx=%.5f;ty=%.5f;tz=%.5f", "explicit visual target payload")
    require(service, "case SKY_RITUAL -> target.add(0.0, profile.skyHeight(), 0.0);", "sky ritual placement")
    require(service, "aimedMob(player, range)", "entity-aware target")
    require(service, "aimGround(player", "wall-safe ground placement")

    require(tracker, "buildSkyRitualArray", "sky ritual charge geometry")
    require(tracker, "buildQuadArray", "four-node array geometry")
    require(tracker, "buildOrbitingSubArrays", "satellite circle geometry")
    require(tracker, "targetOffset(Visual visual)", "target-relative effect travel")
    require(tracker, "motionProgress(Visual visual, double age)", "authored projectile easing")
    require(tracker, "visual.impactAge", "visible impact phase")
    if "GRAND_ARRAY_RADIUS" in tracker:
        raise SystemExit("legacy circle-rank size table still active")

    require(mesh, "Builder runeGlyph", "procedural rune glyphs")
    require(mesh, "Builder runeRing", "procedural rune bands")
    require(mesh, "VIVID_SATURATION=2.05", "strong saturation")
    require(mesh, "FACE_ALPHA_BOOST=1.78", "strong opacity")

    require(kinetics, "presentationImpactDelay", "shared presentation impact delay")
    require(kinetics, "clock(player) + presentationImpactDelay", "server impact scheduling")
    require(kinetics, "interval, totalPulses, pulsePower, false", "delayed first field pulse without pulse loss")
    require(casting, "static double kineticDistance(ServerPlayer player, double range)", "actual travel distance helper")

    before = digest()
    subprocess.run([sys.executable, str(MIGRATION)], cwd=ROOT, check=True)
    after = digest()
    if before != after:
        raise SystemExit("alpha.17 migration v2 is not idempotent")

    print("Arcane Circle alpha.17 spell-presentation audit v2: PASS")


if __name__ == "__main__":
    main()

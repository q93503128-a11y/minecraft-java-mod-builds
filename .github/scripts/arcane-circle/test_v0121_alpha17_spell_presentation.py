from __future__ import annotations

from hashlib import sha256
from pathlib import Path
import subprocess
import sys

ROOT = Path.cwd()
WORKSPACE = ROOT.parent.parent
MIGRATION = WORKSPACE / ".github/scripts/arcane-circle/apply_v0121_alpha17_spell_presentation.py"

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

    require(profile, 'put("meteor_swarm", SKY_RITUAL, SKY_DROP, 16.0', "giant meteor sky ritual")
    require(profile, 'put("power_word_kill", TARGET_SEAL, TARGET_BURST, 2.35', "compact 9C death seal")
    require(profile, 'put("control_weather", SKY_RITUAL, STORM, 16.0', "weather sky array")
    require(profile, 'put("gate", PORTAL_GATE, PORTAL, 10.5', "world gate presentation")
    require(profile, 'put("magic_missile", FRONT_COMPACT, MISSILE_SWARM, 0.92', "missile authored launch")
    for speed in ("56", "44", "28", "48", "34", "23", "72"):
        require(profile, speed, f"varied projectile speed {speed}")

    require(service, "tx=%.5f;ty=%.5f;tz=%.5f", "visual target payload")
    require(service, "case SKY_RITUAL -> target.add(0.0, profile.skyHeight(), 0.0);", "sky anchor")
    require(service, "aimedMob(player, range)", "entity-aware visual target")
    require(service, "aimGround(player", "wall-safe ground target")

    require(tracker, "buildSkyRitualArray", "sky ritual charge mesh")
    require(tracker, "buildQuadArray", "four-satellite array")
    require(tracker, "runeRing", "dense rune bands")
    require(tracker, "targetOffset(Visual visual)", "target-relative release geometry")
    require(tracker, "travelAge(Visual visual, double age)", "impact synchronized visual travel")
    require(tracker, "SpellPresentationProfile.profile", "authored presentation lookup")
    if "GRAND_ARRAY_RADIUS" in tracker:
        raise SystemExit("legacy circle-rank size table still active")

    require(mesh, "Builder runeGlyph", "procedural rune glyph")
    require(mesh, "Builder runeRing", "procedural rune ring")
    require(mesh, "VIVID_SATURATION=2.05", "strong color saturation")
    require(mesh, "FACE_ALPHA_BOOST=1.78", "strong magic opacity")

    require(kinetics, "projectileImpactDelay", "projectile impact scheduling")
    require(kinetics, "SpellPresentationProfile.impactDelayTicks", "shared visual/impact timing")
    require(casting, "static double kineticDistance(ServerPlayer player, double range)", "target travel distance")

    before = digest()
    subprocess.run([sys.executable, str(MIGRATION)], cwd=ROOT, check=True)
    after = digest()
    if before != after:
        raise SystemExit("alpha.17 migration is not idempotent")

    print("Arcane Circle alpha.17 spell-presentation audit: PASS")


if __name__ == "__main__":
    main()

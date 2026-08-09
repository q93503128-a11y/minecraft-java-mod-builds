from __future__ import annotations

from pathlib import Path
import hashlib
import subprocess
import sys

ROOT = Path.cwd()
WORKSPACE = ROOT.parent.parent
MIGRATION = WORKSPACE / ".github/scripts/arcane-circle/apply_v0121_alpha18_visual_polish_v2.py"
FILES = [
    Path("gradle.properties"),
    Path("src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java"),
    Path("src/main/java/kr/moonseungjun/arcanecircle/magic/SpellPresentationProfile.java"),
    Path("src/main/java/kr/moonseungjun/arcanecircle/magic/WorldMagicService.java"),
    Path("src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java"),
]


def digest() -> str:
    h = hashlib.sha256()
    for path in FILES:
        h.update(path.as_posix().encode())
        h.update(path.read_bytes())
    return h.hexdigest()


def require(text: str, token: str, label: str) -> None:
    if token not in text:
        raise SystemExit(f"{label}: missing {token!r}")


def forbid(text: str, token: str, label: str) -> None:
    if token in text:
        raise SystemExit(f"{label}: forbidden {token!r}")


def main() -> None:
    subprocess.run([sys.executable, str(MIGRATION)], cwd=ROOT, check=True)
    first = digest()
    subprocess.run([sys.executable, str(MIGRATION)], cwd=ROOT, check=True)
    second = digest()
    if first != second:
        raise SystemExit("alpha.18 migration is not idempotent")

    gradle = Path("gradle.properties").read_text(encoding="utf-8")
    require(gradle, "mod_version=0.12.1-alpha.18", "version")

    profile = Path("src/main/java/kr/moonseungjun/arcanecircle/magic/SpellPresentationProfile.java").read_text(encoding="utf-8")
    for spell in ("wall_of_fire", "wall_of_force", "hold_person", "ice_storm", "dimension_door", "maze"):
        require(profile, f'put("{spell}"', "profile")
    require(profile, 'put("meteor_swarm", SigilStyle.SKY_RITUAL, MotionStyle.SKY_DROP, 18.0, 6, 4, 0, 30, 2.55, 30);', "meteor profile")
    require(profile, 'put("power_word_kill", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 2.35', "non-monotonic scale")

    service = Path("src/main/java/kr/moonseungjun/arcanecircle/magic/WorldMagicService.java").read_text(encoding="utf-8")
    require(service, "profile.motion() == SpellPresentationProfile.MotionStyle.PRISON", "prison anchor")
    require(service, "targetEntity.getEyePosition()", "npc target anchor")
    require(service, "target.map(mob -> groundUnder(player, mob.position()))", "player prison ground")
    forbid(service, "MotionStyle.PRISON\\n", "literal newline escape")
    forbid(service, "case TARGET_SEAL -> target.add(0.0, 1.05, 0.0);", "old target seal offset")

    tracker = Path("src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java").read_text(encoding="utf-8")
    require(tracker, "sigilPhase", "staged sigils")
    require(tracker, "There is deliberately no universal disc/ring prelude here", "distinct charge grammar")
    require(tracker, "mouthFade", "meteor exit apertures")
    require(tracker, "panelAppear", "progressive wall assembly")
    require(tracker, "sideAppear", "progressive prison assembly")
    require(tracker, "horizontal = new Vec3(visual.direction.x", "portal orientation")
    require(tracker, "mesh.runeRing(facing, Vec3.ZERO, radius * 3.35", "beam source aperture")

    start = tracker.index("    private static ArcaneWorldMesh buildCharge(Visual visual) {")
    end = tracker.index("    private static void buildQuadArray(", start)
    charge = tracker[start:end]
    forbid(charge, "mesh.disc(", "universal charge disc")

    print("Arcane Circle alpha.18 visual polish audit: PASS")


if __name__ == "__main__":
    main()

from __future__ import annotations

from hashlib import sha256
from pathlib import Path
import subprocess
import sys

ROOT = Path.cwd()
SCRIPT = Path(__file__).with_name("apply_v0121_alpha16_visual_rebuild.py")

FILES = [
    "gradle.properties",
    "src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java",
    "src/main/java/kr/moonseungjun/arcanecircle/magic/WorldMagicService.java",
    "src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneWorldMesh.java",
    "src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java",
]


def text(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def require(rel: str, *tokens: str) -> None:
    value = text(rel)
    for token in tokens:
        if token not in value:
            raise SystemExit(f"{rel}: missing required token: {token}")


def forbid(rel: str, *tokens: str) -> None:
    value = text(rel)
    for token in tokens:
        if token in value:
            raise SystemExit(f"{rel}: forbidden stale token remains: {token}")


def digest(rel: str) -> str:
    return sha256((ROOT / rel).read_bytes()).hexdigest()


def main() -> None:
    require("gradle.properties", "mod_version=0.12.1-alpha.16")
    require("src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java",
            'public static final String VERSION = "0.12.1-alpha.16";')

    service = "src/main/java/kr/moonseungjun/arcanecircle/magic/WorldMagicService.java"
    require(service,
            "visibleFrontAnchor(ServerPlayer player, SpellDefinition spell, Vec3 look)",
            "visiblePoint(ServerPlayer player, Vec3 look, double desiredDistance)",
            "getCollisionShape(level, pos).isEmpty()",
            "getCollisionShape(level, samplePos).isEmpty()",
            "bestVisibleFloor")
    forbid(service,
           "case FRONT -> player.getEyePosition().add(look.scale(1.55 + spell.circle() * 0.035))",
           "for (int step = (int) Math.max(2, Math.floor(Math.min(range, 28.0))); step >= 2; step--)")

    mesh = "src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneWorldMesh.java"
    require(mesh,
            "VIVID_SATURATION=1.72",
            "FACE_ALPHA_BOOST=1.52",
            "windowScale*4.10F",
            "windowScale*2.05F",
            "windowScale*.82F",
            "Luma-based saturation")
    forbid(mesh, "SATURATION_BOOST=1.28", "ALPHA_BOOST=1.32")

    tracker = "src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java"
    require(tracker,
            "MAX_CHARGE_GEOMETRY = 3200",
            "MAX_RELEASE_GEOMETRY = 2200",
            "MAX_FRAME = 18000",
            "GRAND_ARRAY_RADIUS",
            "0.68, 0.82, 0.98, 1.18, 1.45, 1.82, 2.30, 2.92, 3.72",
            "System.nanoTime() - visual.startedAt",
            "if (circle >= 5)",
            "if (circle >= 6)",
            "if (circle >= 8)",
            "if (circle == 9)",
            "axisA",
            "axisB",
            "axisC",
            "addReleaseCrown(mesh, visual, age)",
            "spectacleScale(circle)",
            "case 9 -> 1.62",
            "case FIRE -> 0xFFFF2A08",
            "case FROST -> 0xFF18C8FF",
            "case WIND -> 0xFF00F0A8",
            "case WARD -> 0xFF8A35FF",
            "case LIFE -> 0xFF25E85A",
            "case SPACE -> 0xFFD51CFF",
            "default -> 0xFF3E63FF")

    # The migration must be safe to leave in CI: a second application cannot mutate published source.
    before = {rel: digest(rel) for rel in FILES}
    subprocess.run([sys.executable, str(SCRIPT)], cwd=ROOT, check=True)
    after = {rel: digest(rel) for rel in FILES}
    if before != after:
        changed = [rel for rel in FILES if before[rel] != after[rel]]
        raise SystemExit(f"alpha.16 migration is not idempotent: {changed}")

    print("Arcane Circle alpha.16 high-circle visual rebuild audit: PASS")


if __name__ == "__main__":
    main()

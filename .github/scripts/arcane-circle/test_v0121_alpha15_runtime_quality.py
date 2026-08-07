from __future__ import annotations

from hashlib import sha256
from pathlib import Path
import subprocess
import sys

ROOT = Path.cwd()


def text(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def require(rel: str, *tokens: str) -> None:
    body = text(rel)
    for token in tokens:
        if token not in body:
            raise SystemExit(f"{rel}: missing required token {token!r}")


def forbid(rel: str, *tokens: str) -> None:
    body = text(rel)
    for token in tokens:
        if token in body:
            raise SystemExit(f"{rel}: forbidden legacy token remains {token!r}")


def digest(rel: str) -> str:
    return sha256((ROOT / rel).read_bytes()).hexdigest()


tracked = [
    "gradle.properties",
    "src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java",
    "src/main/java/kr/moonseungjun/arcanecircle/magic/MageGearService.java",
    "src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java",
    "src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneWorldMesh.java",
    "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneMageService.java",
    "src/main/java/kr/moonseungjun/arcanecircle/registry/ModItems.java",
    "src/main/java/kr/moonseungjun/arcanecircle/item/ArcaneTestKitItem.java",
    "src/main/resources/assets/arcanecircle/items/arcane_test_kit.json",
    "src/main/resources/assets/arcanecircle/models/item/arcane_test_kit.json",
    "src/main/resources/assets/arcanecircle/lang/ko_kr.json",
]

require("gradle.properties", "mod_version=0.12.1-alpha.15")
require("src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java",
        'VERSION = "0.12.1-alpha.15"', "MageGearService::onIncomingDamage")
forbid("src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java", "tickMovement(player)")
require("src/main/java/kr/moonseungjun/arcanecircle/magic/MageGearService.java",
        "DamageTypes.FALL", "event.setCanceled(true)", "ignoreThreshold", "bootReduction")
forbid("src/main/java/kr/moonseungjun/arcanecircle/magic/MageGearService.java",
       "stabilizeDescent(", "SLOW_FALLING", "STABLE_DESCENT_UNTIL")
require("src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneWorldMesh.java",
        "SATURATION_BOOST=1.28", "ALPHA_BOOST=1.32")
require("src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneMageService.java",
        "chooseCombatSpell(Mob caster, MageProfile profile)", "roll<55", "preferredSchool")
forbid("src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneMageService.java",
       "private static SpellDefinition visualSpell(MageProfile profile)")
require("src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java",
        "enableCreativeTestProfile(ServerPlayer player)", "state.circle=SpellCatalog.IMPLEMENTED_MAX_CIRCLE",
        "state.cooldowns.clear()", "100_000")
require("src/main/java/kr/moonseungjun/arcanecircle/item/ArcaneTestKitItem.java",
        "hasInfiniteMaterials()", "1_000_000_000L", "ArcaneNetwork.sync(serverPlayer)")
require("src/main/java/kr/moonseungjun/arcanecircle/registry/ModItems.java",
        "ARCANE_TEST_KIT", "event.accept(ARCANE_TEST_KIT.get())")
require("src/main/resources/assets/arcanecircle/lang/ko_kr.json",
        "item.arcanecircle.arcane_test_kit", "아르카나 시험핵")

before = {rel: digest(rel) for rel in tracked}
subprocess.run([sys.executable, str(Path("..") / ".." / ".github" / "scripts" / "arcane-circle" /
                                   "apply_v0121_alpha15_runtime_quality.py")], cwd=ROOT, check=True)
after = {rel: digest(rel) for rel in tracked}
if before != after:
    changed = [rel for rel in tracked if before[rel] != after[rel]]
    raise SystemExit(f"alpha.15 migration is not idempotent; second pass changed: {changed}")

print("Arcane Circle alpha.15 runtime-quality audit: PASS")

from pathlib import Path
import math

root = Path(__file__).resolve().parents[1]
magic = (root / "src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java").read_text(encoding="utf-8")
casting = (root / "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java").read_text(encoding="utf-8")
tracker = (root / "src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java").read_text(encoding="utf-8")
props = (root / "gradle.properties").read_text(encoding="utf-8")

assert "mod_version=0.12.1-alpha.14" in props
for marker in (
    "double equipmentCostMultiplier = Math.max(0.10,",
    "double progressionCostMultiplier = circleMana * masteryMana * facultyMana;",
    "double equipmentCooldownMultiplier = Math.max(0.10,",
    "double progressionCooldownMultiplier = circleCooldown * masteryCooldown * facultyCooldown;",
    "rawCooldown < 2.0",
):
    assert marker in magic, marker
for obsolete in (
    "Math.max(0.10, circleMana * masteryMana",
    "Math.max(0.10, circleCooldown * masteryCooldown",
    "double circleMana = Math.max(0.10",
    "double circleCooldown = Math.max(0.10",
):
    assert obsolete not in magic, obsolete
assert "return raw < 2.0 ? 0" in casting

for marker in (
    "METEOR_FORMS", "PORTAL_FORMS", "PRISON_FORMS", "WALL_FORMS", "STORM_FORMS",
    "buildMeteor", "buildPortal", "buildPrison", "buildWall", "buildStorm",
    "buildMissileSwarm", "buildElementalOrb", "buildLance",
    "for (int layer = 1; layer <= circle", "Exactly one complete concentric band per spell circle",
):
    assert marker in tracker, marker

# Equipment alone bottoms at ten percent. Progression remains multiplicative afterwards.
equipment = max(0.10, 0.22 * 0.30)
progression = math.pow(0.62, 7) * 0.70 * 0.90
assert equipment == 0.10
assert equipment * progression < 0.10
assert 40 * equipment * progression < 2.0
assert 10 * math.pow(0.78, 7) * 0.72 < 2.0

# Generic continuous beams remain restricted to the explicit beam family.
assert 'else if (TRUE_BEAMS.contains(id)) buildBeam' in tracker
assert 'else if ("magic_missile".equals(id)) buildMissileSwarm' in tracker
print("Arcane Circle alpha.14 visual architecture audit: PASS")

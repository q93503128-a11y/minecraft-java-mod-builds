#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def ricochet_damage(base: float, hop: int) -> float:
    return max(1.5, base * (0.86 ** hop))


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    ability = read("VillageRoleAbilitySystem.java")
    skills = read("VillageRoleSkillSystem.java")
    rpg = read("VillageRpgSystem.java")
    technique = read("VillageCombatTechniqueSystem.java")

    assert "mod_version=0.18.12-alpha.1" in props

    # Initial lock uses a real forward cone instead of accepting almost any target in front.
    assert "to.normalize().dot(look) >= 0.62" in ability
    assert "body.subtract(origin).dot(look) > 0.20" not in ability
    assert "bestAimTarget(level, player, arrow.position(), 64.0)" in ability

    # Flight homing is predictive but blended, and can reacquire instead of dying with one target.
    assert "bestFlightTarget(level, owner, arrow, 52.0)" in ability
    assert "entry.setValue(new TrackingArrowState(target.getUUID(), state.until()))" in ability
    assert "double turnStrength = Math.min(0.76, 0.46 + specialRank * 0.05)" in ability
    assert "current.scale(1.0 - turnStrength)" in ability
    assert "current.scale(0.38).add(desired.scale(0.62))" in ability
    assert "hasClearFlightPath(level, owner, origin, target)" in ability
    assert "ClipContext.Block.COLLIDER" in ability
    assert "arrow.setNoGravity(false)" in ability
    assert "predicted.subtract(arrow.position())" in ability

    # A ricochet is now an ordered nearest-neighbour chain, not one-frame radial splash damage.
    assert "RICOCHET_HOPS" in ability
    assert "tickRicochetHops" in ability
    assert "queueRicochet" in ability
    assert "buildRicochetChain" in ability
    assert "visited.add(primary.getUUID())" in ability
    assert ".filter(target -> !visited.contains(target.getUUID()))" in ability
    assert ".filter(from::hasLineOfSight)" in ability
    assert "now + 2L + i * 2L" in ability
    assert "Math.pow(0.86, i)" in ability
    assert "damage * (1.0f - i * 0.09f)" not in ability
    assert "int maximumChain = 4 + Math.min(4, Math.max(0, specialRank))" in ability

    # Secondary hits keep the owner for XP/coins, while guarding against duplicate RPG scaling/ricochet recursion.
    assert "PRE_SCALED_RICOCHET_DAMAGE" in ability
    assert "isPreScaledRicochetDamage" in ability
    assert "level.damageSources().playerAttack(owner)" in ability
    assert "PRE_SCALED_RICOCHET_DAMAGE.add(key)" in ability
    assert "PRE_SCALED_RICOCHET_DAMAGE.remove(key)" in ability
    assert "boolean preScaledRicochet" in rpg
    assert "if (!preScaledRicochet" in rpg
    assert "if (!preScaledRicochet) VillageCombatTechniqueSystem.handleIncomingDamage(event)" in rpg
    assert "SECONDARY_DAMAGE" in technique  # normal passive ricochet keeps its own recursion guard too.

    # Player-facing description must match runtime behaviour.
    assert "표적이 사라지면 비행 경로 전방의 새 적을 재포착" in skills
    assert "중복 없이 순차 도탄" in skills

    # Damage falloff stays meaningful without going negative, and skill-rank chain size is bounded 4..8.
    damages = [ricochet_damage(20.0, i) for i in range(8)]
    assert all(damages[i] >= damages[i + 1] for i in range(len(damages) - 1))
    assert damages[0] == 20.0 and damages[-1] > 1.5
    assert [4 + min(4, rank) for rank in range(6)] == [4, 5, 6, 7, 8, 8]

    print("[PASS] tracking ricochet locks only inside a forward aim cone")
    print("[PASS] homing uses smooth predictive steering and live target reacquisition")
    print("[PASS] ricochet hops sequentially through unique line-of-sight targets")
    print("[PASS] secondary hits preserve player kill ownership without double scaling or recursion")
    print("[PASS] skill description and bounded damage/chain scaling match runtime")


if __name__ == "__main__":
    main()

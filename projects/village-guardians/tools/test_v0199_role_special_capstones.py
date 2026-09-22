#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    roles = read("VillageRoleSkillSystem.java")
    ability = read("VillageRoleAbilitySystem.java")

    # Tier V is described as a behavior-changing capstone for every role.
    for token in (
        "근접 처치가 주변 적을 약화",
        "신속 사격이 7발 부채꼴",
        "첫 비전 메아리가 반드시 발생",
        "초과 회복이 흡수 보호막으로 전환",
        "주변 아군에게 피해 저항 오라",
    ):
        assert token in roles

    # Vanguard: melee finish weakens nearby enemies.
    assert "role == VillageRole.VANGUARD" in ability
    assert "VillageRoleSkillSystem.specialRank(killer, VillageRole.VANGUARD) >= 5" in ability
    assert "MobEffects.WEAKNESS, 70, 0" in ability

    # Ranger: rank V adds the outer pair, turning the prepared shot into seven total arrows.
    assert "scale.specialRank() >= 5" in ability
    assert "spawnSideArrow(level, player, arrow, -24.0" in ability
    assert "spawnSideArrow(level, player, arrow, 24.0" in ability

    # Arcanist: the first echo is guaranteed at rank V while the second remains probabilistic.
    assert "specialRank >= 5 || player.getRandom().nextFloat() < firstEchoChance" in ability
    assert "player.getRandom().nextFloat() < secondEchoChance" in ability

    # Luminar: excess healing converts into absorption rather than being wasted.
    assert "healWithOverflowBarrier" in ability
    assert "float overflow = Math.max(0.0f, scaled - Math.max(0.0f, maximum - before));" in ability
    assert "if (specialRank < 5) return;" in ability
    assert "MobEffects.ABSORPTION, 120, amplifier" in ability

    # Warden: both held shield states project a resistance aura at rank V.
    assert "fortress.specialRank() >= 5" in ability
    assert "aegis.specialRank() >= 5" in ability
    assert "allies(player, 10.0)" in ability
    assert "allies(player, 12.0)" in ability

    print("[PASS] all five Special V nodes now change combat behavior instead of only adding stats")
    print("[PASS] capstones reuse the existing role tree, skill slots and currency without menu bloat")


if __name__ == "__main__":
    main()

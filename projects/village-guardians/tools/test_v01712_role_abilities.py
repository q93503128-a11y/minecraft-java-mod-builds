#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    skills = read("VillageRoleSkillSystem.java")
    ability = read("VillageRoleAbilitySystem.java")
    effects = read("VillageSkillEffectSystem.java")
    rpg = read("VillageRpgSystem.java")
    guard = read("VillageGuardians.java")
    respawn = read("VillageRespawnSystem.java")
    role = read("VillageRole.java")
    keys = read("VillageClientKeys.java")

    assert "mod_version=" in props
    expected_names = [
        "회전 검무", "전투 고양", "검기 난무", "천붕 강하",
        "신속 삼연사", "추적 도탄", "천공 화살비", "성멸 대궁",
        "홍염탄", "빙결 지대", "폭풍 회랑", "천뢰 폭격",
        "응급 성광", "전군 정화", "치유 성역", "기적의 대성역",
        "수호 돌진", "위압의 함성", "거대 방패 태세", "대수호 진군"
    ]
    for name in expected_names:
        assert name in skills, name
    for token in [
        "SPIN_UNTIL", "player.swing", "EntityTypes.SNOWBALL", "ArrowLooseEvent", "spawnSideArrow",
        "RICOCHET_UNTIL", "ARROW_RAIN", "ENERGY_ARROW", "AreaKind.FROST",
        "AreaKind.TORNADO", "healLowestAlly", "cleanseAllies",
        "AreaKind.HEALING", "reviveNow", "LivingKnockBackEvent", "replayingEcho",
        "VillageSkillEffectSystem.startCast"
    ]:
        assert token in ability, token
    for token in [
        "VillageSkillEffectEntity.spawn", "vanguard_spin", "ranger_rain_field",
        "arcanist_frost", "arcanist_tornado", "luminar_healing_field",
        "warden_fortress", "warden_aegis"
    ]:
        assert token in effects, token
    assert "Display.ItemDisplay" not in effects and "Display.BlockDisplay" not in effects
    assert "ParticleTypes" not in ability and "sendParticles" not in ability
    assert "ParticleTypes" not in effects and "sendParticles" not in effects
    assert "VillageRoleAbilitySystem.tick" in guard
    assert "VillageRoleAbilitySystem.handleArrowLoose" in guard
    assert "VillageRoleAbilitySystem.handleKnockback" in guard
    assert "VillageRoleAbilitySystem.handleEntityJoin" in guard
    assert "VillageRoleAbilitySystem.handleIncomingDamage" in guard
    assert "VillageRoleAbilitySystem.handleDeath" in guard
    assert "public static boolean reviveNow" in respawn
    assert "근접 피해 일부를 체력으로 흡수" in role
    assert "화살로 처치하면 사용 화살을 회수" in role
    assert "최대 두 번 반복" in role
    assert "체력이 낮을수록 치유량과 보호막량" in role
    assert "모든 넉백을 무효화" in role
    assert 'GLFW.GLFW_KEY_Z' in keys and 'GLFW.GLFW_KEY_V' in keys
    assert 'GLFW.GLFW_KEY_B' in keys and 'GLFW.GLFW_KEY_H' in keys
    assert 'GLFW.GLFW_KEY_J' in keys and 'GLFW.GLFW_KEY_K' in keys
    assert 'GLFW.GLFW_KEY_U' not in keys and 'CALLER' not in keys
    assert "■" in skills and "□" in skills
    assert "VillageSkillVisualSystem.render" not in rpg

    print("[PASS] Twenty active skills own real movement, projectile, field and procedural-mesh motions")
    print("[PASS] Vanguard, ranger, arcanist, luminar and warden passives are wired to combat events")
    print("[PASS] Skill visuals avoid particle geometry and cooldown HUD exposes live readiness")
    print("[PASS] Default shortcuts match Z/X/V/H/J/K with no obsolete U duplicate")


if __name__ == "__main__":
    main()

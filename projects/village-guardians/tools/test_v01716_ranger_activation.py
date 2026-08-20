#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def block(text: str, start: str, end: str) -> str:
    return text.split(start, 1)[1].split(end, 1)[0]


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    ability = read("VillageRoleAbilitySystem.java")
    effects = read("VillageSkillEffectSystem.java")
    mesh = read("VillageSkillMeshLibrary.java")
    hud = read("VillageSkillHudOverlay.java")
    skills = read("VillageRoleSkillSystem.java")

    assert "mod_version=" in props

    # All four ranger actives are queued and consumed by a real arrow spawn.
    assert "ARROW_RAIN_READY" in ability
    assert "clearRangerReadies(player.getUUID())" in ability
    join = block(ability, "public static void handleEntityJoin", "public static void handleIncomingDamage")
    assert "MEGA_ARROW_READY.remove(id)" in join
    assert "ARROW_RAIN_READY.remove(id)" in join
    assert "RICOCHET_UNTIL.remove(id)" in join
    assert "RAPID_UNTIL.remove(id)" in join
    assert "activateArrowRain(level, player" in join
    assert "isRangerContext(player)" in join
    assert "VillageSkillTestSystem.selectedRole(player)" in ability

    # The crash path is removed; generated split arrows always receive a valid bow weapon.
    assert "spawnFallingArrow" not in ability
    assert "Invalid weapon firing an arrow" not in ability
    side = block(ability, "private static void spawnSideArrow", "private static void aimAssist")
    assert "new ItemStack(Items.BOW)" in side

    # Rain is a short mesh scene anchored by an actual downward ground raycast.
    assert "int fieldDuration = 42" in ability
    assert "i < 8" in block(ability, "private static void activateArrowRain", "private static void clearRangerReadies")
    ground = block(ability, "private static Vec3 aimedGround", "private static void activateArrowRain")
    assert "point.add(0.0, -48.0, 0.0)" in ground
    assert "ground.getLocation().add(0.0, 0.02, 0.0)" in ground
    rain_mesh = block(mesh, "private static void renderArrowRainField", "private static void renderArrowRainImpact")
    assert "radius, 0.012" in rain_mesh
    assert "int arrows = 18 + meta.rank() * 3" in rain_mesh
    assert "progress * 5.8" in rain_mesh
    assert "ranger_rain_impact" in effects and "10, 0.0f" in effects

    # Blade wave travels horizontally from waist height; slam no longer owns a giant arrow blade.
    blade = block(ability, "private static void bladeWave", "private static void groundSlam")
    assert "player.position().add(0.0, 0.82, 0.0)" in blade
    blade_mesh = block(mesh, "private static void renderBladeWave", "private static void renderSlamImpact")
    assert "3.6, 0.0" in blade_mesh
    slam_mesh = block(mesh, "private static void renderSlamCharge", "private static void renderBladeWave")
    assert "energyBlade" not in slam_mesh
    assert "customArrow" not in slam_mesh

    # Tracking cue is local to the player and the skill row is slightly higher.
    assert "player.getEyePosition().add(sight.scale(1.45))" in ability
    focus = block(mesh, "private static void renderRangerFocus", "private static void renderFireImpact")
    assert "b.local(0.0, 0.0, 0.18)" in focus
    assert "graphics.guiHeight() - 92" in hud

    for name in ("신속 삼연사", "추적 도탄", "천공 화살비", "성멸 대궁"):
        assert name in skills
    assert skills.count("다음 실제 활·석궁 발사") >= 3

    print("[PASS] 시험 궁수와 실제 궁수 모두 다음 활 발사에서 네 기술을 소비합니다")
    print("[PASS] 화살비는 위험한 실제 낙하 화살 없이 짧은 메시 장면과 사용자 피해 판정을 사용합니다")
    print("[PASS] 화살비 범위는 지면 레이캐스트에 붙고 남은 화살은 빠르게 사라집니다")
    print("[PASS] 검기는 허리 높이에서 전방으로 가로 이동하며 천붕 강하의 거대 화살표는 제거됩니다")
    print("[PASS] 추적 도탄 준비 표식과 기술 HUD 위치가 플레이 화면에 맞게 조정됩니다")


if __name__ == "__main__":
    main()

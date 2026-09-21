#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def section(source: str, start: str, end: str) -> str:
    return source.split(start, 1)[1].split(end, 1)[0]


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    turret = read("VillagePlacedTurretSystem.java")
    effects = read("VillageDefenseEffectSystem.java")
    mesh = read("VillageSkillMeshLibrary.java")
    siege = read("VillageSiegeCommandUi.java")
    enemy = read("VillageEnemyArchetypeSystem.java")
    skills = read("VillageRoleSkillSystem.java")

    assert "mod_version=" in props
    assert "현재 소스 버전 `" in readme
    assert "목표 JAR `villageguardians-" in readme

    # Placement preview transmits the authoritative level-1 range and renders a fixed circular boundary.
    placement = section(turret, "public static boolean handlePlacementClick", "public static String cancelPlacement")
    assert "effectiveRange(pending.type(), 1)" in placement
    assert "바닥의 원형 사거리선" in placement
    assert "turretPlacementPreview(level" in placement
    preview = section(effects, "public static void turretPlacementPreview(", "public static void turretDeployPulse")
    assert 'type.ordinal() + "|" +' in preview
    assert '"turret_placement_preview"' in preview
    assert ", 70, 0.0f, encoded" in preview
    assert 'case "turret_placement_preview" -> renderTurretPlacementPreview' in mesh
    ring_preview = section(mesh, "private static void renderTurretPlacementPreview", "private static void renderDefenseMaintenance")
    assert "ring(pose, out, b, range" in ring_preview
    assert "range >= 60.0 ? 128 : 96" in ring_preview
    assert "Keep its radius fixed" in ring_preview

    # Turret progression and maintenance are discoverable rather than hidden in formulas.
    assert "return 2 + VillageProgressionSystem.wallLevel()" in turret
    assert "VillageDefenseResearchSystem.level(VillageDefenseResearchSystem.Branch.TOWER)" in turret
    assert "static int dismantleRefund" in turret
    assert "설치 한도 = 기본 2 + 성벽 단계" in siege
    assert "폭파병·탑 사냥꾼·보스" in siege
    assert "VillagePlacedTurretSystem.dismantleRefund(state)" in siege
    assert "nextStats" in siege
    assert "effectiveDamage" in siege and "effectiveRange" in siege and "effectiveInterval" in siege

    # The 0.18.38 early-game sapper nerf remains, while the 100-day campaign may add only
    # gradual health/damage mastery. Objective pressure must never be replaced by sprint scaling.
    attrs = section(enemy, "private static void applyArchetypeAttributes", "private static void applyArchetypeEffects")
    assert "VillageCampaignProgression.effectiveCombatDay(day)" in attrs
    assert "health.setBaseValue(8.5 + Math.max(0.0f, pacedDay - 1.0f) * 0.24)" in attrs
    assert "attack.setBaseValue(1.25 + Math.max(0.0f, pacedDay - 1.0f) * 0.045)" in attrs
    assert "Math.min(24.0, 8.5" not in attrs
    assert "Math.min(4.0, 1.25" not in attrs
    assert "speed.setBaseValue(0.15)" in attrs
    assert "case SAPPER -> 1.72f" in enemy

    # Role-tree descriptions expose exact cumulative duration/power values and high-tier cooldown reductions.
    role_node = section(skills, "public enum RoleNode", "public enum ActiveSkill")
    for text in ("누적 +32%", "누적 +48%", "누적 +59%", "누적 +70%",
                 "누적 +28%", "누적 피해·치유량 +50%", "누적 +61%", "누적 +72%",
                 "특수 등급 ", "재사용 감소 총 -2초"):
        assert text in role_node, text

    print("[PASS] v0.18.42 turret placement shows the authoritative circular maximum-range preview")
    print("[PASS] turret capacity, damageability, upgrades and exact dismantle refund are visible in command UI")
    print("[PASS] sapper keeps fixed readable speed and only gradual 100-day health/damage mastery")
    print("[PASS] role growth descriptions expose exact cumulative power/duration and special-tier cooldown effects")


if __name__ == "__main__":
    main()

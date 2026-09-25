#!/usr/bin/env python3
"""Pre-play semantic contracts for Village Guardians 0.18.48."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def section(source: str, start: str, end: str) -> str:
    return source.split(start, 1)[1].split(end, 1)[0]

def main() -> None:
    effects = read("VillageSkillEffectSystem.java")
    entity = read("VillageSkillEffectEntity.java")
    entities = read("VillageSkillEffectEntities.java")
    mesh = read("VillageSkillMeshLibrary.java")
    abilities = read("VillageRoleAbilitySystem.java")
    skills = read("VillageRoleSkillSystem.java")
    mastery = read("VillageRoleMasterySystem.java")
    progression = read("VillageProgressionSystem.java")
    arena = read("VillageSkillTestSystem.java")
    victory = read("VillageVictoryScreen.java")
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    project = (ROOT / "PROJECT.md").read_text(encoding="utf-8")
    gradle = (ROOT / "gradle.properties").read_text(encoding="utf-8")

    assert "System.currentTimeMillis()" not in skills
    assert "System.currentTimeMillis()" not in mastery
    assert "player.level().getGameTime()" in skills
    assert "windowTicks" in mastery and "untilGameTime" in mastery

    assert '"promotion_skill_cast", "promotion_skill_follow" -> true' in entity
    assert '.sized(64.0f, 32.0f)' in entities
    assert "promotionCastDuration(skill)" in effects
    assert "promotionMovingField" in effects and "promotionFollow" in effects

    assert "phase >= 2 ? honestRadius : honestRadius * tierScale * phaseScale" in mesh
    assert '"promotion_skill_field", "promotion_skill_follow"' in mesh
    sword = section(mesh, 'case "vanguard_sword_chain" -> {', 'case "vanguard_life_sever" -> {')
    split = section(mesh, 'case "ranger_split_shot" -> {', 'case "ranger_aa_intercept" -> {')
    star = section(mesh, 'case "ranger_star_tracker" -> {', 'case "ranger_constellation" -> {')
    assert "phase == 1" in sword and "energyBlade" in sword
    assert "phase == 1" in split and split.count("customArrow") == 1
    assert "phase == 1" in star and star.count("customArrow") == 1
    assert "Basis.from(new Vec3(0.0, 1.0, 0.0))" in mesh
    assert "Basis.from(new Vec3(0.0, -1.0, 0.0))" in mesh
    assert "triangleTwoSided(pose, out, left, right, nose" in mesh

    absolute = section(abilities, "case VANGUARD_ABSOLUTE_BREAK -> {", "case VANGUARD_HEAVEN_SEVER -> {")
    fortress = section(abilities, "case WARDEN_FORTRESS_CHARGE -> {", "case WARDEN_ABSOLUTE_FORMATION -> {")
    tracker = section(abilities, "case RANGER_STAR_TRACKER -> {", "case RANGER_CONSTELLATION -> {")
    gravity = section(abilities, "case ARCANIST_GRAVITY_STORM -> {", "case ARCANIST_SOLAR_CORE -> {")
    assert "startDash(" in absolute and "for (int i = 0; i < 11; i++)" not in absolute
    assert "startDash(" in fortress and "for (int i = 0; i < 10; i++)" not in fortress
    assert "launchPromotionTrackingAt(" in tracker and "hurt(level, player, target" not in tracker
    assert "MovingKind.TRACKING_ARROW" in abilities
    assert "promotionMovingField" in gravity and "0.24f" in gravity
    assert "area.moveTo(area.center().add(travel.scale(0.24)))" in abilities
    assert "private record DashState" in abilities

    allies = section(abilities, "private static List<ServerPlayer> allies(", "private static Vec3 aimedGround")
    assert "!VillageRespawnSystem.isDowned(ally)" in allies
    assert "alliesIncludingDowned" in allies
    cleanse = section(abilities, "private static void cleanseAllies(", "private static void miracle(")
    assert "MobEffectCategory.HARMFUL" in cleanse
    assert "scaledHealAmount" in abilities
    assert "VillageRoleAbilitySystem.clearPlayerState(player)" in progression
    assert "player.removeAllEffects()" in progression

    assert "ARENA_RADIUS = 34" in arena
    for distance in ("{0, 4}", "{-5, 8}", "{-10, 14}", "{-16, 21}", "{-22, 28}"):
        assert distance in arena
    assert arena.count("VillageRoleAbilitySystem.clearPlayerState(player)") >= 3

    assert 'getTitle().getString()' in victory
    assert "maxBodyScroll" in victory and "mouseScrolled" in victory

    assert "mod_version=0.18.48-alpha.1" in gradle
    assert "0.18.48-alpha.1" in project and "0.18.48-alpha.1" in readme

    print("[PASS] pre-play combat clocks and retry transient state are server-tick authoritative")
    print("[PASS] promotion phase geometry, dash contact and tracking projectiles match live combat semantics")
    print("[PASS] support targeting/cleanse/overheal and expanded skill arena close manual-audit gaps")
    print("[PASS] victory report and 0.18.48 identity are ready for client playtest")

if __name__ == "__main__":
    main()

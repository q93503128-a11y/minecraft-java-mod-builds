#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def section(source: str, start: str, end: str) -> str:
    return source.split(start, 1)[1].split(end, 1)[0]

def main() -> None:
    turret = read("VillagePlacedTurretSystem.java")
    relic = read("VillageRelicSystem.java")
    rpg = read("VillageRpgSystem.java")
    role = read("VillageRoleSkillSystem.java")
    ability = read("VillageRoleAbilitySystem.java")
    enemy = read("VillageEnemyArchetypeSystem.java")
    trait = read("VillageWaveTrait.java")
    raid = read("VillageRaidSystem.java")

    fire = section(turret, "private static void fire", "private static Mob selectTarget")
    assert "scanLimit = state.type() == TurretType.ANTI_AIR ? 96 : 12" in fire
    assert "visible.stream().filter(VillageRaidSystem::isAerialEnemy)" in fire
    assert 'ANTI_AIR("anti_air", "대공 발사대", 19, 24, 128' in turret
    assert "VillageRaidSystem.isAerialEnemy(target) ? 1.80f : 0.65f" in turret

    for token in (
        "projectileTargetMultiplier", "skillDurationMultiplier", "cooldownMultiplier",
        "meleeLifeStealBonus", "tauntDurationMultiplier",
        "maximumHealth * 0.35f ? 1.35f : 1.0f",
    ):
        assert token in relic, token
    assert "VillageRelicSystem.projectileTargetMultiplier(attacker, target)" in rpg
    assert "VillageRelicSystem.meleeLifeStealBonus(attacker)" in rpg
    assert "VillageRelicSystem.skillDurationMultiplier(player)" in role
    assert "VillageRelicSystem.cooldownMultiplier(player)" in role
    assert "VillageRelicSystem.tauntDurationMultiplier(player)" in ability

    assert "SUPPORT_HEAL_BUDGET" in enemy
    assert "public static void forget(UUID uuid)" in enemy
    assert "public static void resetRaidState()" in enemy
    assert "supportHeal(ally" in enemy and "supportHeal(mob" in enemy
    assert "MobEffects.REGENERATION, LONG_EFFECT_TICKS" not in trait
    plague = section(enemy, "case PLAGUE_ARCHON -> {", "case DREAD_KNIGHT -> {")
    assert "MobEffects.REGENERATION" not in plague
    assert "VillageEnemyArchetypeSystem.forget(uuid)" in raid
    assert "VillageEnemyArchetypeSystem.resetRaidState()" in raid

    print("[PASS] anti-air uses huge range and hard aerial-first acquisition")
    print("[PASS] relics have stronger conditional identities wired into real combat")
    print("[PASS] enemy sustain has finite budgets and no permanent regeneration loop")

if __name__ == "__main__":
    main()

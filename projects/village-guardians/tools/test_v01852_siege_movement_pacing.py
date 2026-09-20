#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def section(source: str, start: str, end: str) -> str:
    return source.split(start, 1)[1].split(end, 1)[0]

def main() -> None:
    enemy = read("VillageEnemyArchetypeSystem.java")
    raid = read("VillageRaidSystem.java")
    aspect = read("VillageBossAspectSystem.java")
    boss = read("VillageSiegeBossSystem.java")
    elite = read("VillageEnemyEliteSystem.java")
    bestiary = read("VillageEnemyBestiary.java")

    pacing = section(enemy, "public static boolean usesSiegePacing", "public static boolean isTacticalThreat")
    assert "Archetype.SAPPER" in pacing
    assert "Archetype.SHIELDBREAKER" in pacing
    assert "isBoss(archetype)" in pacing

    attributes = section(enemy, "private static void applyArchetypeAttributes", "private static void applyArchetypeEffects")
    for token in (
        "speed.setBaseValue(0.15)",
        "speed.setBaseValue(0.19)",
        "speed.setBaseValue(0.16)",
        "speed.setBaseValue(0.18)",
        "speed.setBaseValue(0.17)",
        "speed.setBaseValue(0.20)",
    ):
        assert token in attributes

    configure = section(enemy, "public static void configure", "public static float structureDamageMultiplier")
    assert "trait.applyLongEffects(mob);" in configure
    assert "if (usesSiegePacing(archetype))" in configure
    assert "mob.removeEffect(MobEffects.SPEED);" in configure

    spawned = section(raid, "private static void spawnWave", "private static void applyScaling")
    assert spawned.index("applyScaling(") < spawned.index("VillageEnemyArchetypeSystem.configure(")

    enemy_effects = section(enemy, "private static void applyArchetypeEffects", "private static Archetype select")
    dread = enemy_effects.split("case DREAD_KNIGHT ->", 1)[1].split("default ->", 1)[0]
    assert "MobEffects.SPEED" not in dread

    abilities = section(enemy, "public static void tickAbility", "public static void onStructureHit")
    war_chant = section(abilities, "case WAR_CHANTER -> {", "case NECROMANCER -> {")
    assert "usesSiegePacing(allyType)" in war_chant

    elite_discover = section(elite, "private static void discover", "private static void grappler")
    assert "!VillageEnemyArchetypeSystem.usesSiegePacing(archetype)" in elite_discover

    aspect_config = section(aspect, "public static void configure", "public static void tick")
    assert "case BERSERKER -> mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH" in aspect_config
    assert "case STORMCALLER -> { }" in aspect_config
    aspect_tick = section(aspect, "public static void tick", "public static float structureMultiplier")
    berserker_tick = section(aspect_tick, "case BERSERKER -> {", "case BULWARK -> {")
    assert "MobEffects.SPEED, 60, 0" in berserker_tick
    assert "MobEffects.SPEED, 60, 2" not in berserker_tick
    warleader_tick = section(aspect_tick, "case WARLEADER -> {", "case WALLBREAKER -> {")
    assert "!VillageEnemyArchetypeSystem.usesSiegePacing(allyType)" in warleader_tick

    discover = section(boss, "private static void discover", "private static void enterPhaseTwo")
    assert "MobEffects.SPEED" not in discover
    phase_two = section(boss, "private static void enterPhaseTwo", "private static void tickBreach")
    assert "MobEffects.SPEED" not in phase_two
    duel = section(boss, "private static void tickDuel", "public static void forget")
    assert "? 1.28 : 1.16" in duel
    assert "? 1.58 : 1.34" not in duel

    assert "시설 공격 피해가 기본의 1.72배입니다." in bestiary
    assert "시설 특화 2.30배" not in bestiary

    print("[PASS] siege-focused enemies use authored lower base movement speeds")
    print("[PASS] wave, elite and support haste cannot turn siege units into sprinters")
    print("[PASS] bosses no longer stack permanent campaign/aspect/doctrine haste")
    print("[PASS] berserker keeps only a mild temporary chase burst")
    print("[PASS] black-marshal pursuit remains distinct without extreme navigation speed")
    print("[PASS] sapper dossier matches the live 1.72x structure-damage value")

if __name__ == "__main__":
    main()

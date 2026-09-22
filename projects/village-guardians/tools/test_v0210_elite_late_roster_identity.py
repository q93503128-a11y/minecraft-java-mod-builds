#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def section(source: str, start: str, end: str) -> str:
    return source.split(start, 1)[1].split(end, 1)[0]

def main() -> None:
    elite = read("VillageEnemyEliteSystem.java")
    comp = read("VillageEnemyCompositionSystem.java")
    effects = read("VillageEnemyEffectSystem.java")
    mesh = read("VillageSkillMeshLibrary.java")
    arche = read("VillageEnemyArchetypeSystem.java")

    discover = section(elite, "private static void discover", "private static void grappler")
    assert "VillageEnemyCompositionSystem.attachElitePresentation(level, mob, doctrine)" in discover

    assert "ASSASSIN_STRIKES" in elite
    assassin = section(elite, "private static void assassin", "private static void plague")
    assert "elite_assassin_lunge" not in assassin
    assert "VillageEnemyEffectSystem.assassinLunge" in assassin
    assert "mob.setDeltaMovement(horizontal.normalize().scale(1.55)" in assassin
    assert "target.hurtServer(level, level.damageSources().mobAttack(mob), damage)" in assassin

    shock = section(elite, "private static void shock", "private static java.util.List<ServerPlayer> nearbyPlayers")
    assert "SHOCK_CHARGES" in shock
    assert "VillageEnemyCompositionSystem.animateRiderAttack(mob)" in shock
    assert "VillageEnemyEffectSystem.shockCharge" in shock
    assert "VillageEnemyEffectSystem.shockImpact" in shock
    assert "player.hurtMarked = true" in shock

    presentation = section(comp, "public static void attachElitePresentation", "public static boolean isVisualRider")
    for entity in ("EntityTypes.SKELETON", "EntityTypes.HUSK", "EntityTypes.VINDICATOR",
                   "EntityTypes.WITCH", "EntityTypes.ZOMBIFIED_PIGLIN"):
        assert entity in presentation
    for item in ("Items.LEAD", "Items.FIRE_CHARGE", "Items.IRON_SWORD", "Items.SPIDER_EYE", "Items.GOLDEN_AXE"):
        assert item in presentation

    for kind in ("elite_assassin_lunge", "elite_assassin_impact", "elite_shock_charge", "elite_shock_impact"):
        assert kind in effects
        assert kind in mesh

    abilities = section(arche, "public static void tickAbility", "public static void onStructureHit")
    assert "case BREEZE_DISRUPTOR" in abilities
    assert "BREEZE_WIND_CHARGE_BURST" in abilities
    assert "case NETHER_REAVER" in abilities
    assert "damageAndDebuffPlayers(level, server, mob, 4.5, 2.8f, MobEffects.WEAKNESS)" in abilities

    bodies = section(arche, "private static Mob createEntity", "private static void equip")
    assert "case BOGGED_ARCHER -> EntityTypes.BOGGED.create" in bodies
    assert "case BREEZE_DISRUPTOR -> EntityTypes.BREEZE.create" in bodies
    assert "case MAGMA_BRUTE -> EntityTypes.MAGMA_CUBE.create" in bodies
    assert "case NETHER_REAVER -> EntityTypes.ZOMBIFIED_PIGLIN.create" in bodies

    print("[PASS] five elites own distinct presentation layers and authored combat actions")
    print("[PASS] assassin and shock rider use readable warning-to-impact combat, not passive stat buffs")
    print("[PASS] late normal roster keeps distinct vanilla bodies and authored disruption/pressure")

if __name__ == "__main__":
    main()

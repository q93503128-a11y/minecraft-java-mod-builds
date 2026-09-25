#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name):
    return (JAVA / name).read_text(encoding="utf-8")

def section(source, start, end):
    return source.split(start, 1)[1].split(end, 1)[0]

def main():
    world = read("VillageWorldSystem.java")
    raid = read("VillageRaidSystem.java")
    attack = read("VillageAttackPlanSystem.java")
    elite = read("VillageEnemyEliteSystem.java")
    boss = read("VillageSiegeBossSystem.java")
    merc = read("VillageMercenarySystem.java")
    deploy = read("VillageMercenaryDeploymentSystem.java")
    hud = read("VillageHudSystem.java")
    overlay = read("VillageMainHudOverlay.java")

    assert "RETURN_COOLDOWN_TICKS = 20L * 10L" in world
    assert "COMBAT_RETURN_LOCK_TICKS = 20L * 5L" in world
    assert "projectile.getOwner() instanceof ServerPlayer attacker" in world
    assert "재사용 대기시간은 10초" in world

    assert 'AERIAL_ENEMY_TAG = "villageguardians_aerial_enemy"' in raid
    assert "mob.addTag(AERIAL_ENEMY_TAG)" in raid
    assert "public static boolean isAerialEnemy(Entity entity)" in raid
    assert "public static boolean hasActiveTaunt(ServerLevel level, Mob mob)" in raid

    taunt = section(raid, "public static int tauntEnemies", "public static boolean isAerialEnemy")
    assert "double effectiveRadius = radius" in taunt and "int maximum = Math.max(1, limit)" in taunt
    direct = section(raid, "private static void directEnemies", "private static net.minecraft.world.entity.animal.golem.IronGolem selectMercenaryTarget")
    assert "directTauntedGroundEnemy" in direct
    assert direct.index("activeTauntTarget(level, mob)") < direct.index("VillageEnemyArchetypeSystem.tickAbility")
    assert direct.index("activeTauntTarget(level, mob)") < direct.index("isAerialEnemy(mob)")

    assert "VillageRaidSystem.hasActiveTaunt(level, mob)" in attack
    assert "VillageRaidSystem.hasActiveTaunt(level, mob)" in elite
    assert "VillageRaidSystem.hasActiveTaunt(server.overworld(), mob)" in boss

    assert "VillageRaidSystem.isAerialEnemy(enemy)" in merc
    assert "VillageRaidSystem.isAerialEnemy(enemy)" in deploy

    assert "public static String currentThreatHud" in attack
    assert "previewThreatLine(day, wave, count)" in attack
    for label in ("↑북문", "↖북서", "↗북동", "←서", "동→", "✦공중"):
        assert label in attack
    assert "VillageAttackPlanSystem.currentThreatHud(level)" in hud
    assert 'base + " §8│ " + threat' in hud
    assert "int maxWidth = Math.min(380" in overlay
    assert "third.isBlank() ? 35 : 49" in overlay
    assert "if (!third.isBlank()) graphics.text" in overlay

    print("[PASS] return uses 10s cooldown and five-second dealt/taken combat lock")
    print("[PASS] aerial raid actors have a dedicated authoritative tag")
    print("[PASS] taunt overrides front, elite and boss routing")
    print("[PASS] melee mercenaries ignore tagged aerial threats")
    print("[PASS] raid HUD and wave messages expose active attack directions")

if __name__ == "__main__":
    main()

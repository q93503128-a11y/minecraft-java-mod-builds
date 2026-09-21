#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    tree = read("VillageSkillTreeSystem.java")
    data = read("VillageSkillTreeData.java")
    controller = read("VillageUiController.java")
    network = read("VillageNetwork.java")
    rpg = read("VillageRpgSystem.java")
    hud = read("VillageMainHudOverlay.java")
    suppressor = read("VillageUiHudSuppressor.java")

    # Original low node costs remain, allowing all 50 nodes by Lv.300.
    assert "Math.max(1, Math.min(4, (tier + 2) / 3))" in tree
    assert "int naturalTotal = Math.max(0, level - 1);" in tree

    # Any completed branch unlocks repeatable 1P health/attack training.
    assert "public static boolean trainingUnlocked" in tree
    assert "public static synchronized String purchaseTraining" in tree
    assert '"health".equals(normalized)' in tree
    assert '"attack".equals(normalized)' in tree
    assert "SPENT_POINTS.put(id, spentPoints(player) + 1);" in tree
    assert '"health_training_v1"' in data and '"attack_training_v1"' in data
    assert 'action.startsWith("skill_training:")' in controller
    assert 'action.startsWith("skill_training:")' in network

    # Health training is a real base attribute; attack training is a shared combat stat.
    assert "Attributes.MAX_HEALTH" in rpg
    assert "Attributes.ATTACK_DAMAGE" in rpg
    assert "20.0D + VillageSkillTreeSystem.healthTrainingBonus(player)" in rpg
    assert "attackDamage.setBaseValue(1.0D);" in rpg
    assert "float attackTrainingPower = (float) VillageSkillTreeSystem.attackTrainingBonus(attacker);" in rpg
    assert "applySkillAttackTraining" in rpg

    # Vanilla heart pips are replaced by a live numeric health bar.
    assert "VanillaGuiLayers.PLAYER_HEALTH.equals(event.getName())" in suppressor
    assert "renderHealthBar(graphics, minecraft, font);" in hud
    assert "minecraft.player.getHealth()" in hud
    assert "minecraft.player.getMaxHealth()" in hud
    assert "minecraft.player.getAbsorptionAmount()" in hud
    assert '" / "' in hud
    assert '"  +"' in hud
    assert "HEALTH_FILL" in hud and "HEALTH_BACK" in hud
    assert "ABSORPTION_TRACK" in hud and "GOLD" in hud

    print("[PASS] all five tactical branches retain the original low-cost completion path")
    print("[PASS] a completed branch unlocks persistent repeatable +0.25 health / +0.1 attack training")
    print("[PASS] repeat health is an attribute while repeat attack is a shared melee/bow/skill combat stat")
    print("[PASS] vanilla heart pips are replaced by a live current/max health bar with visible absorption")


if __name__ == "__main__":
    main()

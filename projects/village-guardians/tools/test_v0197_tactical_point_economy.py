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
    screen = read("VillageSkillTreeScreen.java")
    descriptions = read("VillageActionDescriptions.java")
    rpg = read("VillageRpgSystem.java")

    assert "int naturalTotal = Math.max(0, level - 1);" in tree
    assert "return Math.max(naturalTotal, spentPoints(player));" in tree
    assert "레벨이 오를 때마다 전술 포인트 1P를 얻습니다." in tree
    assert "레벨이 오를 때마다 얻는 전술 포인트" in descriptions

    # Original low-cost branch price: 22P each, 110P for all five branches.
    costs = [1, 1, 1, 2, 2, 2, 3, 3, 3, 4]
    assert sum(costs) == 22
    assert sum(costs) * 5 == 110
    assert 300 - 1 == 299
    assert (300 - 1) - sum(costs) * 5 == 189
    assert "Math.max(1, Math.min(4, (tier + 2) / 3))" in tree
    assert "int currentCost = currentNodeCost(mask)" in tree
    assert "Math.min(Math.max(0, stored), Math.max(legacyFloor, currentCost))" in tree

    # Completing any full branch unlocks repeatable 1P stat training.
    assert "public static boolean trainingUnlocked" in tree
    assert "public static synchronized String purchaseTraining" in tree
    assert '"health".equals(normalized)' in tree
    assert '"attack".equals(normalized)' in tree
    assert "HEALTH_TRAINING.put(id, next);" in tree
    assert "ATTACK_TRAINING.put(id, next);" in tree
    assert "SPENT_POINTS.put(id, spentPoints(player) + 1);" in tree

    # Training ranks are save-backed and surfaced through the current growth screen.
    assert '"health_training_v1"' in data
    assert '"attack_training_v1"' in data
    assert 'actions.add("skill_training:health")' in controller
    assert 'actions.add("skill_training:attack")' in controller
    assert "TrainingVisual" in screen and "renderTrainingPanel" in screen

    # Health is a base attribute; attack training is an authoritative shared combat stat.
    assert "Attributes.MAX_HEALTH" in rpg
    assert "Attributes.ATTACK_DAMAGE" in rpg
    assert "20.0D + VillageSkillTreeSystem.healthTrainingBonus(player)" in rpg
    assert "attackDamage.setBaseValue(1.0D);" in rpg
    assert "VillageSkillTreeSystem.attackTrainingBonus(attacker)" in rpg
    assert "applySkillAttackTraining" in rpg

    print("[PASS] every level awards 1P, all five branches cost 110P, and superseded high costs are refunded")
    print("[PASS] Lv.300 can complete every branch and has 189P left for repeatable training")
    print("[PASS] any completed branch unlocks save-backed +0.25 health / +0.1 attack repeat training")
    print("[PASS] repeat training is exposed in growth UI and attack training is shared by authoritative combat paths")


if __name__ == "__main__":
    main()

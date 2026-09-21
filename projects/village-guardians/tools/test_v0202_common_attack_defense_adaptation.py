#!/usr/bin/env python3
from pathlib import Path
import math

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def defense_damage(day: int) -> float:
    return 1.0 if day <= 100 else 1.0 + math.sqrt(day - 100) * 0.016

def defense_utility(day: int) -> float:
    return 1.0 if day <= 100 else 1.0 + math.log1p(day - 100) * 0.018

def main() -> None:
    rpg = read("VillageRpgSystem.java")
    role = read("VillageRoleAbilitySystem.java")
    technique = read("VillageCombatTechniqueSystem.java")
    campaign = read("VillageCampaignProgression.java")
    turret = read("VillagePlacedTurretSystem.java")
    merc = read("VillageMercenarySystem.java")
    tree = read("VillageSkillTreeSystem.java")
    ui = read("VillageUiController.java")

    # Repeat attack is no longer a melee-only ATTACK_DAMAGE base patch.
    assert "attackDamage.setBaseValue(1.0D);" in rpg
    assert "1.0D + VillageSkillTreeSystem.attackTrainingBonus(player)" not in rpg
    assert "attackTrainingPower * trainingCoefficient" in rpg
    assert "projectileAttackTrainingCoefficient(attacker, arrow)" in rpg
    assert "attackTrainingPrimaryCoefficient" in rpg
    assert "SkillAttackProfile" in rpg and "PERSISTENT(0.16f)" in rpg
    assert "applySkillAttackTraining(owner, damage, profile)" in role
    assert "hurt(level, target" not in role
    assert "1.0f / arrowCount" in role
    assert "1.0 / (1.0 + 0.72 * falloffBudget)" in role
    assert "1.0f / (1.0f + limit * ratio)" in technique
    assert "공용 공격력 +0.1" in tree
    assert "공용 공격력 +0.1 · 공격 유형별 계수 적용" in ui

    # Endless defenders gain no new levels/currency: only fully mastered assets receive soft tails.
    assert "endlessDefenseDamageMultiplier" in campaign
    assert "Math.sqrt(safe - CAMPAIGN_END_DAY) * 0.016f" in campaign
    assert "endlessDefenseUtilityMultiplier" in campaign
    assert "Math.log1p(safe - CAMPAIGN_END_DAY) * 0.018f" in campaign
    assert "fullTowerMastery(safeLevel)" in turret
    assert "fullMercenaryMastery(rank)" in merc
    assert "fullMercenaryMastery(safeRank)" in merc
    assert "18 + VillageCouncilState.currentDay()" not in turret
    assert "VillageCampaignProgression.effectiveCombatDay" in turret

    # Mercenary doctrine damage now covers melee attributes as well as ranger shots.
    assert "}) * VillageDefenseResearchSystem.mercenaryDamageMultiplier()" in merc
    assert "5.2f * mercenaryPower(rank)" in merc
    assert "* endlessDamageAdaptation(rank)" in merc

    # Tail remains bounded through the requested long-war checkpoints.
    assert defense_damage(100) == 1.0
    assert round(defense_damage(120), 3) == 1.072
    assert round(defense_damage(200), 2) == 1.16
    assert round(defense_damage(500), 2) == 1.32
    assert round(defense_utility(500), 3) == 1.108
    assert defense_damage(500) < 1.5
    assert defense_utility(500) < 1.2

    print("[PASS] repeat attack training is shared across melee, bow, role skills and budgeted multi-hit paths")
    print("[PASS] ricochet/fan/technique paths cannot re-add the full flat training bonus per hit")
    print("[PASS] full turrets and mercenaries receive bounded Endless War damage/utility adaptation")
    print("[PASS] mercenary doctrine damage matches both ranged and melee implementation")

if __name__ == "__main__":
    main()

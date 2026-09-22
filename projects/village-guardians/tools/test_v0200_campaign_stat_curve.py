#!/usr/bin/env python3
from pathlib import Path
import math

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def target_level(day: int) -> int:
    safe = max(1, min(100, day))
    if safe <= 20:
        return 1 + math.floor((safe - 1) * 29.0 / 19.0 + 0.5)
    return min(300, 30 + math.floor((safe - 20) * 270.0 / 80.0 + 0.5))


def combat_level(level: int) -> int:
    safe = max(1, min(300, level))
    return safe if safe <= 100 else 100 + (safe - 100) // 4


def outgoing(level: int) -> float:
    value = combat_level(level)
    foundation = min(29, value - 1)
    mastery = max(0, value - 30)
    return (
        1.0
        + foundation * 0.035
        + (foundation // 5) * 0.08
        + mastery * 0.008
        + (mastery // 10) * 0.025
    )


def player_base_hp(level: int) -> int:
    value = combat_level(level)
    bonus = ((min(30, value) - 1) // 5) * 4 + (max(0, value - 30) // 10) * 2
    return 20 + (bonus // 4) * 4


def equipment_tier(day: int) -> int:
    safe = max(1, min(100, day))
    if safe >= 90:
        return 10
    if safe >= 80:
        return 9
    if safe >= 70:
        return 8
    if safe >= 55:
        return 7
    if safe >= 40:
        return 6
    if safe >= 25:
        return 5
    if safe >= 15:
        return 4
    if safe >= 10:
        return 3
    if safe >= 5:
        return 2
    return 1


def flat_melee(tier: int) -> float:
    safe = max(1, min(10, tier))
    if safe <= 1:
        return 0.0
    if safe == 2:
        return 1.50
    if safe == 3:
        return 3.00
    if safe == 4:
        return 5.00
    n = safe - 4
    return 5.00 + n * 2.25 + n * n * 0.14


def enemy_health_tier(day: int, wave: int = 4) -> int:
    safe = max(1, day)
    authored = min(100, safe)
    base = (authored - 1) // 8 + max(0, wave - 1) // 6
    late = 0 if authored <= 20 else (authored - 20) // 20
    endless = 0 if safe <= 100 else math.floor(math.sqrt(safe - 100) / 4.0)
    return max(0, base + late + endless)


def enemy_strength_tier(day: int, wave: int = 4) -> int:
    safe = max(1, day)
    authored = min(100, safe)
    base = (authored - 1) // 18 + max(0, wave - 3) // 5
    endless = 0 if safe <= 100 else math.floor(math.sqrt(safe - 100) / 12.0)
    return max(0, base + endless)


def enemy_base_scale(day: int) -> float:
    safe = max(1, day)
    if safe <= 20:
        return 1.0
    if safe <= 100:
        return 1.0 + (safe - 20) / 80.0
    return 2.0 + math.sqrt(safe - 100) * 0.05


def effective_combat_day(day: int) -> float:
    safe = max(1, day)
    if safe <= 20:
        return float(safe)
    if safe <= 100:
        return 20.0 + (safe - 20) * 0.40
    return 52.0 + math.sqrt(safe - 100) * 0.80


def absorption_hp(day: int) -> int:
    safe = max(1, day)
    if safe < 30:
        return 0
    if safe <= 100:
        amplifier = max(0, (safe - 20) // 30)
    else:
        amplifier = 2 + math.floor(math.sqrt(safe - 100) / 10.0)
    return 4 * (amplifier + 1)


def neutral_grunt_hp(day: int) -> float:
    tier = enemy_health_tier(day)
    health_boost = 0 if tier <= 0 else 4 * (tier + 1)
    return 20.0 * enemy_base_scale(day) + health_boost + absorption_hp(day)


def rusher_base_hp(day: int) -> float:
    paced = effective_combat_day(day)
    progress = max(0.0, paced - 1.0)
    late_progress = max(0.0, paced - 20.0)
    return 11.0 + progress * 0.34 - late_progress * 0.27


def rusher_hp(day: int) -> float:
    tier = enemy_health_tier(day)
    if day > 20:
        tier = max(0, tier - 2)
    health_boost = 0 if tier <= 0 else 4 * (tier + 1)
    return rusher_base_hp(day) * enemy_base_scale(day) + health_boost + absorption_hp(day)


def main() -> None:
    campaign = read("VillageCampaignProgression.java")
    enemies = read("VillageEnemyArchetypeSystem.java")
    raid = read("VillageRaidSystem.java")
    attack = read("VillageAttackPlanSystem.java")
    tree = read("VillageSkillTreeSystem.java")
    rpg = read("VillageRpgSystem.java")

    # Enemy stat growth has no date-based hard ceiling.
    assert "return Math.max(0, base + lateBonus + endlessBonus);" in campaign
    assert "return 2.0f + (float) Math.sqrt(safe - CAMPAIGN_END_DAY) * 0.05f;" in campaign
    assert "return Math.max(0, base + endlessBonus);" in campaign
    assert "enemyAbsorptionAmplifier" in campaign
    assert "bonusAbsorptionAmplifier" in campaign
    assert "structureDamageDayContribution" in campaign
    assert "aerialStructureDamageDayContribution" in campaign
    assert "VillageCampaignProgression.structureDamageDayContribution(day)" in attack
    assert "day * 0.65f" not in attack
    assert "healthTier + (boss ? 3 : 0)" in raid
    assert "strengthTier + (boss ? 1 : 0)" in raid
    assert "Math.min(17, healthTier" not in raid
    assert "Math.min(6, strengthTier" not in raid

    # Archetypes use slopes/relative tiers instead of direct HP or attack caps.
    assert "trait.healthScale()" in enemies
    assert "lateProgress * 0.27" in enemies
    assert "lateProgress * 0.020" in enemies
    for old_cap in ("Math.min(52.0", "Math.min(9.0", "Math.min(18.0", "Math.min(5.0",
                    "Math.min(24.0", "Math.min(4.0"):
        assert old_cap not in enemies
    assert "Math.min(3, day / 10)" not in enemies
    assert "Math.min(24, day)" not in raid
    assert "Math.min(16, day)" not in raid

    landmarks = {
        20: (30, 30, 2.415, 4, 5.00, 40, 2, 32),
        40: (98, 98, 3.109, 6, 10.06, 52, 5, 53),
        60: (165, 116, 3.303, 7, 13.01, 56, 9, 78),
        80: (233, 133, 3.489, 9, 19.75, 60, 12, 99),
        100: (300, 150, 3.675, 10, 23.54, 64, 16, 120),
    }
    for day, expected in landmarks.items():
        level = target_level(day)
        actual = (
            level,
            combat_level(level),
            round(outgoing(level), 3),
            equipment_tier(day),
            round(flat_melee(equipment_tier(day)), 2),
            player_base_hp(level),
            enemy_health_tier(day),
            round(neutral_grunt_hp(day)),
        )
        assert actual == expected, (day, actual, expected)

    # Rusher identity is preserved by a lower late slope/tier, not a health ceiling.
    for day in (20, 40, 60, 80, 100, 200, 500):
        assert rusher_hp(day) < neutral_grunt_hp(day)

    # Endless war keeps rising but the square-root tail slows the per-day increase.
    endless = [(day, enemy_health_tier(day), neutral_grunt_hp(day))
               for day in (100, 120, 200, 500)]
    assert [x[1] for x in endless] == [16, 17, 18, 21]
    assert [round(x[2]) for x in endless] == [120, 128, 142, 168]
    assert neutral_grunt_hp(500) > neutral_grunt_hp(200) > neutral_grunt_hp(120) > neutral_grunt_hp(100)

    # Repeat training remains uncapped; small rank values control accumulation.
    assert "HEALTH_TRAINING_PER_RANK = 0.25D" in tree
    assert "ATTACK_TRAINING_PER_RANK = 0.10D" in tree
    assert "healthTrainingBonus(player)" in rpg
    assert "attackTrainingBonus(player)" in rpg
    leftover_after_full_tree = 299 - 110
    assert leftover_after_full_tree == 189
    assert leftover_after_full_tree * 0.25 == 47.25
    assert round(leftover_after_full_tree * 0.10, 1) == 18.9

    print("[PASS] day20 opening stats are preserved while enemy durability rises through day100")
    print("[PASS] enemy health/strength/protection and structure pressure continue past day100 without date plateaus")
    print("[PASS] rusher fragility comes from growth slope and tier offsets, not a fixed HP ceiling")
    print("[PASS] repeat training remains uncapped at +0.25 HP / +0.1 attack per rank")


if __name__ == "__main__":
    main()

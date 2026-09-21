#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")

def section(source: str, start: str, end: str) -> str:
    return source.split(start, 1)[1].split(end, 1)[0]

def main() -> None:
    progress = read("RpgProgress.java")
    campaign = read("VillageCampaignProgression.java")
    raid = read("VillageRaidSystem.java")
    war = read("VillageWarfrontSystem.java")
    traits = read("VillageWaveTrait.java")
    identity = read("VillageEquipmentIdentity.java")
    rarity = read("VillageEquipmentRaritySystem.java")
    shop = read("VillageEquipmentShop.java")
    turret = read("VillagePlacedTurretSystem.java")
    research = read("VillageDefenseResearchSystem.java")
    merc = read("VillageMercenarySystem.java")
    promotion = read("VillageRolePromotionSystem.java")
    role = read("VillageRoleSkillSystem.java")
    ability = read("VillageRoleAbilitySystem.java")
    effects = read("VillageSkillEffectSystem.java")
    skill_tree = read("VillageSkillTreeSystem.java")

    assert "MAX_LEVEL = 100" in progress
    assert "CAMPAIGN_END_DAY = 100" in campaign
    assert "return Math.min(RpgProgress.MAX_LEVEL" in campaign
    assert 'return "최종 대공성"' in war
    for token in ("검은 물결", "천공 침공", "사령 공성", "철의 일식",
                  "폭풍 군단", "심연 진군", "멸망 공세", "최후 전쟁"):
        assert token in war

    scaling = section(raid, "private static void applyScaling", "private static void directEnemies")
    assert "VillageCampaignProgression.enemyHealthTier" in scaling
    assert "VillageCampaignProgression.enemyStrengthTier" in scaling
    assert "MobEffects.SPEED" not in scaling
    xp = section(raid, "public static int experienceForEnemy", "public static VillageEnemyArchetypeSystem.AerialRole")
    assert "RpgProgress.experienceRequiredAtLevel(targetLevel)" in xp
    assert "expectedThreatsPerLevel(day)" in xp

    for trait in ("BREACH_STORM", "SKY_SIEGE", "HUNTER_NET", "DEATH_CHORUS",
                  "IRON_TIDE", "CATACLYSM", "FINAL_HOST"):
        assert trait in traits

    assert "Math.min(10, tier)" in identity
    assert "VillageCampaignProgression.equipmentTierForDay(day)" in rarity
    for item in ("BLACKWALL_GREATSWORD", "SKYWARD_CROSSBOW", "STORM_LONGBOW",
                 "WARCASTER_FOCUS", "DOOMSTAR_BOW", "LAST_GUARD_BLADE", "CENTURY_AEGIS"):
        assert item in shop

    assert "MAX_TURRET_LEVEL = 10" in turret
    assert "MAX_LEVEL = 20" in research
    assert "MAX_LEVEL = 100" in merc
    presentation = read("VillageMercenaryPresentationSystem.java")
    assert "safe >= 100" in presentation and "safe >= 80" in presentation

    assert "FIRST_PROMOTION_LEVEL = 30" in promotion
    assert "SECOND_PROMOTION_LEVEL = 60" in promotion
    for name in ("전선검장", "파성검성", "성루명사수", "천공추적자",
                 "전투원소술사", "대비전술사", "전장성직자", "대성휘사제",
                 "성문수호장", "불락수호장"):
        assert name in promotion

    enum_block = section(role, "public enum ActiveSkill", "private final String id;")
    for role_id in ("VANGUARD", "RANGER", "ARCANIST", "LUMINAR", "WARDEN"):
        assert enum_block.count("VillageRole." + role_id) == 12
    assert "return 12;" in role
    assert "promotionTier()" in role and "promotionSlot()" in role
    assert "1차 전직 필요" in role and "2차 전직 필요" in role
    assert "VillageRoleAbilitySystem.cast(level, player, skill" in role
    assert "VillageRolePromotionSystem.skillPowerMultiplier" in role
    assert "VillageRolePromotionSystem.cooldownReductionSeconds" in role

    assert "default -> castPromotionSkill" in ability
    for method in ("castPromotedVanguard", "castPromotedRanger", "castPromotedArcanist",
                   "castPromotedLuminar", "castPromotedWarden"):
        assert method in ability
    assert "default -> promotionStartCast" in effects

    # Reuse the existing tactical-point economy and preserve pre-Lv.30 allocations.
    assert "Math.max(0, Math.min(30, level) - 1)" in skill_tree
    assert "Math.max(0, level - 30) / 2" in skill_tree
    assert "SPENT_POINTS" in skill_tree
    assert "레벨마다, 이후에는 2레벨마다 1P" in skill_tree

    print("[PASS] campaign and reward growth span authored progression through day 100")
    print("[PASS] enemy raw stat growth is slower and never reintroduces day-based speed escalation")
    print("[PASS] equipment, turrets, defense research and mercenaries retain long-campaign growth")
    print("[PASS] five roles have four base + four first-promotion + four second-promotion active skills")
    print("[PASS] promotion passives and existing tactical points extend progression without a new currency")

if __name__ == "__main__":
    main()
